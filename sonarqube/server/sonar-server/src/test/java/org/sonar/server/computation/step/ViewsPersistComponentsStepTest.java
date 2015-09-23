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

import java.text.SimpleDateFormat;
import java.util.Date;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.MutableDbIdsRepositoryRule;
import org.sonar.server.computation.component.ProjectViewAttributes;
import org.sonar.server.computation.component.ViewsComponent;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.computation.component.Component.Type.PROJECT_VIEW;
import static org.sonar.server.computation.component.Component.Type.SUBVIEW;
import static org.sonar.server.computation.component.Component.Type.VIEW;
import static org.sonar.server.computation.component.ViewsComponent.builder;

@Category(DbTests.class)
public class ViewsPersistComponentsStepTest extends BaseStepTest {

  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

  private static final String VIEW_KEY = "VIEW_KEY";
  private static final String VIEW_NAME = "VIEW_NAME";
  private static final String VIEW_DESCRIPTION = "view description";
  private static final String VIEW_UUID = "VIEW_UUID";
  private static final String SUBVIEW_1_KEY = "SUBVIEW_1_KEY";
  private static final String SUBVIEW_1_NAME = "SUBVIEW_1_NAME";
  private static final String SUBVIEW_1_DESCRIPTION = "subview 1 description";
  private static final String SUBVIEW_1_UUID = "SUBVIEW_1_UUID";
  private static final String PROJECT_VIEW_1_KEY = "PV1_KEY";
  private static final String PROJECT_VIEW_1_NAME = "PV1_NAME";
  private static final String PROJECT_VIEW_1_UUID = "PV1_UUID";
  private static final long PROJECT_1_ID = 123;

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public MutableDbIdsRepositoryRule dbIdsRepository = MutableDbIdsRepositoryRule.create(treeRootHolder);

  System2 system2 = mock(System2.class);

  DbClient dbClient = dbTester.getDbClient();

  Date now;

  PersistComponentsStep underTest;

  @Before
  public void setup() throws Exception {
    dbTester.truncateTables();

    now = DATE_FORMAT.parse("2015-06-02");
    when(system2.now()).thenReturn(now.getTime());

    underTest = new PersistComponentsStep(dbClient, treeRootHolder, dbIdsRepository, system2);
  }

  @Override
  protected ComputationStep step() {
    return underTest;
  }

  @Test
  public void persist_empty_view() {
    treeRootHolder.setRoot(createViewBuilder().build());

    underTest.execute();

    assertRowsCountInTableProjects(1);

    ComponentDto projectDto = getComponentFromDb(VIEW_KEY);
    assertDtoIsView(projectDto);
  }

  @Test
  public void persist_existing_empty_view() {
    // most of the time view already exists since its supposed to be created when config is uploaded
    persistComponentDto(createViewDto());

    treeRootHolder.setRoot(createViewBuilder().build());

    underTest.execute();

    assertRowsCountInTableProjects(1);

    assertDtoNotUpdated(VIEW_KEY);
  }

  @Test
  public void persist_view_with_projectView() {
    treeRootHolder.setRoot(
        createViewBuilder()
            .addChildren(createProjectView1Builder().build())
            .build());

    underTest.execute();

    assertRowsCountInTableProjects(2);

    ComponentDto viewDto = getComponentFromDb(VIEW_KEY);
    assertDtoIsView(viewDto);

    ComponentDto pv1Dto = getComponentFromDb(PROJECT_VIEW_1_KEY);
    assertDtoIsProjectView1(pv1Dto, viewDto, viewDto);
  }

  @Test
  public void persist_empty_subview() {
    treeRootHolder.setRoot(
      createViewBuilder()
        .addChildren(
          createSubView1Builder().build())
        .build());

    underTest.execute();

    assertRowsCountInTableProjects(2);

    ComponentDto viewDto = getComponentFromDb(VIEW_KEY);
    assertDtoIsView(viewDto);

    ComponentDto sv1Dto = getComponentFromDb(SUBVIEW_1_KEY);
    assertDtoIsSubView1(viewDto, sv1Dto);
  }

  @Test
  public void persist_existing_empty_subview_under_existing_view() {
    ComponentDto viewDto = createViewDto();
    persistComponentDto(viewDto);
    persistComponentDto(ComponentTesting.newSubView(viewDto, SUBVIEW_1_UUID, SUBVIEW_1_KEY).setName(SUBVIEW_1_NAME));

    treeRootHolder.setRoot(
      createViewBuilder()
        .addChildren(
          createSubView1Builder().build())
        .build());

    underTest.execute();

    assertRowsCountInTableProjects(2);

    assertDtoNotUpdated(VIEW_KEY);
    assertDtoNotUpdated(SUBVIEW_1_KEY);
  }

  @Test
  public void persist_empty_subview_under_existing_view() {
    persistComponentDto(createViewDto());

    treeRootHolder.setRoot(
      createViewBuilder()
        .addChildren(
          createSubView1Builder().build())
        .build());

    underTest.execute();

    assertRowsCountInTableProjects(2);

    assertDtoNotUpdated(VIEW_KEY);
    assertDtoIsSubView1(getComponentFromDb(VIEW_KEY), getComponentFromDb(SUBVIEW_1_KEY));
  }

  @Test
  public void persist_project_view_under_subview() {
    treeRootHolder.setRoot(
        createViewBuilder()
            .addChildren(
                createSubView1Builder()
                    .addChildren(
                        createProjectView1Builder().build())
                    .build())
            .build());

    underTest.execute();

    assertRowsCountInTableProjects(3);

    ComponentDto viewDto = getComponentFromDb(VIEW_KEY);
    assertDtoIsView(viewDto);
    ComponentDto subView1Dto = getComponentFromDb(SUBVIEW_1_KEY);
    assertDtoIsSubView1(viewDto, subView1Dto);
    ComponentDto pv1Dto = getComponentFromDb(PROJECT_VIEW_1_KEY);
    assertDtoIsProjectView1(pv1Dto, viewDto, subView1Dto);
  }

  @Test
  public void update_view_name_and_longName() {
    ComponentDto viewDto = createViewDto().setLongName("another long name").setCreatedAt(now);
    persistComponentDto(viewDto);

    treeRootHolder.setRoot(createViewBuilder().build());

    underTest.execute();

    assertRowsCountInTableProjects(1);

    ComponentDto newViewDto = getComponentFromDb(VIEW_KEY);
    assertDtoIsView(newViewDto);
  }

  private static ViewsComponent.Builder createViewBuilder() {
    return builder(VIEW, VIEW_KEY).setUuid(VIEW_UUID).setName(VIEW_NAME).setDescription(VIEW_DESCRIPTION);
  }

  private ViewsComponent.Builder createSubView1Builder() {
    return builder(SUBVIEW, SUBVIEW_1_KEY).setUuid(SUBVIEW_1_UUID).setName(SUBVIEW_1_NAME).setDescription(SUBVIEW_1_DESCRIPTION);
  }

  private static ViewsComponent.Builder createProjectView1Builder() {
    return builder(PROJECT_VIEW, PROJECT_VIEW_1_KEY)
      .setUuid(PROJECT_VIEW_1_UUID)
      .setName(PROJECT_VIEW_1_NAME)
      .setDescription("project view description is not persisted")
      .setProjectViewAttributes(new ProjectViewAttributes(PROJECT_1_ID));
  }

  private void persistComponentDto(ComponentDto componentDto) {
    dbClient.componentDao().insert(dbTester.getSession(), componentDto);
    dbTester.getSession().commit();
  }

  private ComponentDto getComponentFromDb(String componentKey) {
    return dbClient.componentDao().selectByKey(dbTester.getSession(), componentKey).get();
  }

  private void assertRowsCountInTableProjects(int rowCount) {
    assertThat(dbTester.countRowsOfTable("projects")).isEqualTo(rowCount);
  }

  private void assertDtoNotUpdated(String componentKey) {
    assertThat(getComponentFromDb(componentKey).getCreatedAt()).isNotEqualTo(now);
  }

  private ComponentDto createViewDto() {
    return ComponentTesting.newView(VIEW_UUID).setKey(VIEW_KEY).setName(VIEW_NAME);
  }

  /**
   * Assertions to verify the DTO created from {@link #createViewBuilder()}
   */
  private void assertDtoIsView(ComponentDto projectDto) {
    assertThat(projectDto.name()).isEqualTo(VIEW_NAME);
    assertThat(projectDto.longName()).isEqualTo(VIEW_NAME);
    assertThat(projectDto.description()).isEqualTo(VIEW_DESCRIPTION);
    assertThat(projectDto.path()).isNull();
    assertThat(projectDto.uuid()).isEqualTo(VIEW_UUID);
    assertThat(projectDto.projectUuid()).isEqualTo(projectDto.uuid());
    assertThat(projectDto.parentProjectId()).isNull();
    assertThat(projectDto.moduleUuid()).isNull();
    assertThat(projectDto.moduleUuidPath()).isEqualTo("." + projectDto.uuid() + ".");
    assertThat(projectDto.qualifier()).isEqualTo(Qualifiers.VIEW);
    assertThat(projectDto.scope()).isEqualTo(Scopes.PROJECT);
    assertThat(projectDto.getCopyResourceId()).isNull();
    assertThat(projectDto.getCreatedAt()).isEqualTo(now);
  }

  /**
   * Assertions to verify the DTO created from {@link #createProjectView1Builder()}
   */
  private void assertDtoIsSubView1(ComponentDto viewDto, ComponentDto sv1Dto) {
    assertThat(sv1Dto.name()).isEqualTo(SUBVIEW_1_NAME);
    assertThat(sv1Dto.longName()).isEqualTo(SUBVIEW_1_NAME);
    assertThat(sv1Dto.description()).isEqualTo(SUBVIEW_1_DESCRIPTION);
    assertThat(sv1Dto.path()).isNull();
    assertThat(sv1Dto.uuid()).isEqualTo(SUBVIEW_1_UUID);
    assertThat(sv1Dto.projectUuid()).isEqualTo(viewDto.uuid());
    assertThat(sv1Dto.parentProjectId()).isEqualTo(viewDto.getId());
    assertThat(sv1Dto.moduleUuid()).isEqualTo(viewDto.uuid());
    assertThat(sv1Dto.moduleUuidPath()).isEqualTo(viewDto.moduleUuidPath() + sv1Dto.uuid() + ".");
    assertThat(sv1Dto.qualifier()).isEqualTo(Qualifiers.SUBVIEW);
    assertThat(sv1Dto.scope()).isEqualTo(Scopes.PROJECT);
    assertThat(sv1Dto.getCopyResourceId()).isNull();
    assertThat(sv1Dto.getCreatedAt()).isEqualTo(now);
  }

  private void assertDtoIsProjectView1(ComponentDto pv1Dto, ComponentDto viewDto, ComponentDto parentViewDto) {
    assertThat(pv1Dto.name()).isEqualTo(PROJECT_VIEW_1_NAME);
    assertThat(pv1Dto.longName()).isEqualTo(PROJECT_VIEW_1_NAME);
    assertThat(pv1Dto.description()).isNull();
    assertThat(pv1Dto.path()).isNull();
    assertThat(pv1Dto.uuid()).isEqualTo(PROJECT_VIEW_1_UUID);
    assertThat(pv1Dto.projectUuid()).isEqualTo(viewDto.uuid());
    assertThat(pv1Dto.parentProjectId()).isEqualTo(viewDto.getId());
    assertThat(pv1Dto.moduleUuid()).isEqualTo(parentViewDto.uuid());
    assertThat(pv1Dto.moduleUuidPath()).isEqualTo(parentViewDto.moduleUuidPath() + pv1Dto.uuid() + ".");
    assertThat(pv1Dto.qualifier()).isEqualTo(Qualifiers.PROJECT);
    assertThat(pv1Dto.scope()).isEqualTo(Scopes.FILE);
    assertThat(pv1Dto.getCopyResourceId()).isEqualTo(PROJECT_1_ID);
    assertThat(pv1Dto.getCreatedAt()).isEqualTo(now);
  }

}
