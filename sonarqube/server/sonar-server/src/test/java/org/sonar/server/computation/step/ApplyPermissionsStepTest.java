/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.computation.step;

import java.util.List;
import java.util.Map;
import org.elasticsearch.search.SearchHit;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.config.Settings;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.permission.PermissionRepository;
import org.sonar.db.permission.PermissionTemplateDto;
import org.sonar.db.user.GroupRoleDto;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.MutableDbIdsRepositoryRule;
import org.sonar.server.computation.component.ReportComponent;
import org.sonar.server.computation.component.ViewsComponent;
import org.sonar.server.es.EsTester;
import org.sonar.server.issue.index.IssueAuthorizationIndexer;
import org.sonar.server.issue.index.IssueIndexDefinition;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.component.ComponentTesting.newView;
import static org.sonar.db.permission.PermissionTemplateTesting.newPermissionTemplateDto;
import static org.sonar.server.computation.component.Component.Type.PROJECT;
import static org.sonar.server.computation.component.Component.Type.VIEW;

@Category(DbTests.class)
public class ApplyPermissionsStepTest extends BaseStepTest {

  private static final String ROOT_KEY = "ROOT_KEY";
  private static final String ROOT_UUID = "ROOT_UUID";
  private static final long SOME_DATE = 1000L;

  @ClassRule
  public static EsTester esTester = new EsTester().addDefinitions(new IssueIndexDefinition(new Settings()));

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  @Rule
  public MutableDbIdsRepositoryRule dbIdsRepository = MutableDbIdsRepositoryRule.create(treeRootHolder);

  DbSession dbSession;

  DbClient dbClient = dbTester.getDbClient();

  Settings settings;

  IssueAuthorizationIndexer issueAuthorizationIndexer;

  ApplyPermissionsStep step;

  @Before
  public void setUp() {
    dbSession = dbClient.openSession(false);
    settings = new Settings();
    esTester.truncateIndices();

    issueAuthorizationIndexer = new IssueAuthorizationIndexer(dbClient, esTester.client());
    issueAuthorizationIndexer.setEnabled(true);

    step = new ApplyPermissionsStep(dbClient, dbIdsRepository, issueAuthorizationIndexer, new PermissionRepository(dbClient, settings), treeRootHolder);
  }

  @After
  public void tearDown() {
    dbSession.close();
  }

  @Test
  public void grant_permission_on_new_project() {
    ComponentDto projectDto = ComponentTesting.newProjectDto(ROOT_UUID).setKey(ROOT_KEY);
    dbClient.componentDao().insert(dbSession, projectDto);

    // Create a permission template containing browse permission for anonymous group
    createDefaultPermissionTemplate(UserRole.USER);

    Component project = ReportComponent.builder(PROJECT, 1).setUuid(ROOT_UUID).setKey(ROOT_KEY).build();
    dbIdsRepository.setComponentId(project, projectDto.getId());
    treeRootHolder.setRoot(project);

    step.execute();
    dbSession.commit();

    assertThat(dbClient.componentDao().selectOrFailByKey(dbSession, ROOT_KEY).getAuthorizationUpdatedAt()).isNotNull();
    assertThat(dbClient.roleDao().selectGroupPermissions(dbSession, DefaultGroups.ANYONE, projectDto.getId())).containsOnly(UserRole.USER);
    verifyAuthorisationIndex(ROOT_UUID, DefaultGroups.ANYONE);
  }

  @Test
  public void nothing_to_do_on_existing_project() {
    ComponentDto projectDto = ComponentTesting.newProjectDto(ROOT_UUID).setKey(ROOT_KEY).setAuthorizationUpdatedAt(SOME_DATE);
    dbClient.componentDao().insert(dbSession, projectDto);
    // Permissions are already set on the project
    dbClient.roleDao().insertGroupRole(dbSession, new GroupRoleDto().setRole(UserRole.USER).setGroupId(null).setResourceId(projectDto.getId()));

    dbSession.commit();

    Component project = ReportComponent.builder(PROJECT, 1).setUuid(ROOT_UUID).setKey(ROOT_KEY).build();
    dbIdsRepository.setComponentId(project, projectDto.getId());
    treeRootHolder.setRoot(project);

    step.execute();
    dbSession.commit();

    // Check that authorization updated at has not been changed -> Nothing has been done
    assertThat(projectDto.getAuthorizationUpdatedAt()).isEqualTo(SOME_DATE);
  }

  @Test
  public void grant_permission_on_new_view() {
    ComponentDto viewDto = newView(ROOT_UUID).setKey(ROOT_KEY);
    dbClient.componentDao().insert(dbSession, viewDto);

    String permission = UserRole.USER;
    // Create a permission template containing browse permission for anonymous group
    createDefaultPermissionTemplate(permission);

    Component project = ViewsComponent.builder(VIEW, ROOT_KEY).setUuid(ROOT_UUID).build();
    dbIdsRepository.setComponentId(project, viewDto.getId());
    treeRootHolder.setRoot(project);

    step.execute();
    dbSession.commit();

    assertThat(dbClient.componentDao().selectOrFailByKey(dbSession, ROOT_KEY).getAuthorizationUpdatedAt()).isNotNull();
    assertThat(dbClient.roleDao().selectGroupPermissions(dbSession, DefaultGroups.ANYONE, viewDto.getId())).containsOnly(permission);
  }

  @Test
  public void nothing_to_do_on_existing_view() {
    ComponentDto viewDto = newView(ROOT_UUID).setKey(ROOT_KEY).setAuthorizationUpdatedAt(SOME_DATE);
    dbClient.componentDao().insert(dbSession, viewDto);
    // Permissions are already set on the view
    dbClient.roleDao().insertGroupRole(dbSession, new GroupRoleDto().setRole(UserRole.USER).setGroupId(null).setResourceId(viewDto.getId()));

    dbSession.commit();

    Component project = ReportComponent.builder(PROJECT, 1).setUuid(ROOT_UUID).setKey(ROOT_KEY).build();
    dbIdsRepository.setComponentId(project, viewDto.getId());
    treeRootHolder.setRoot(project);

    step.execute();
    dbSession.commit();

    // Check that authorization updated at has not been changed -> Nothing has been done
    assertThat(viewDto.getAuthorizationUpdatedAt()).isEqualTo(SOME_DATE);
  }

  private void createDefaultPermissionTemplate(String permission) {
    PermissionTemplateDto permissionTemplateDto = dbClient.permissionTemplateDao().insert(dbSession, newPermissionTemplateDto().setName("Default"));
    settings.setProperty("sonar.permission.template.default", permissionTemplateDto.getKee());
    dbClient.permissionTemplateDao().insertGroupPermission(permissionTemplateDto.getId(), null, permission);
    dbSession.commit();
  }

  private void verifyAuthorisationIndex(String rootUuid, String groupPermission){
    List<SearchHit> issueAuthorizationHits = esTester.getDocuments(IssueIndexDefinition.INDEX, IssueIndexDefinition.TYPE_AUTHORIZATION);
    assertThat(issueAuthorizationHits).hasSize(1);
    Map<String, Object> issueAuthorization = issueAuthorizationHits.get(0).sourceAsMap();
    assertThat(issueAuthorization.get("project")).isEqualTo(rootUuid);
    assertThat((List<String>) issueAuthorization.get("groups")).containsOnly(groupPermission);
    assertThat((List<String>) issueAuthorization.get("users")).isEmpty();
  }

  @Override
  protected ComputationStep step() {
    return step;
  }
}
