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

package org.sonar.db.component;

import com.google.common.base.Optional;
import java.util.Collections;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.RowNotFoundException;
import org.sonar.test.DbTests;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.sonar.db.component.ComponentTesting.newDeveloper;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonar.db.component.ComponentTesting.newView;

@Category(DbTests.class)
public class ComponentDaoTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  ComponentDao underTest = new ComponentDao();

  @Test
  public void get_by_uuid() {
    db.prepareDbUnit(getClass(), "shared.xml");

    ComponentDto result = underTest.selectByUuid(db.getSession(), "KLMN").get();
    assertThat(result).isNotNull();
    assertThat(result.uuid()).isEqualTo("KLMN");
    assertThat(result.moduleUuid()).isEqualTo("EFGH");
    assertThat(result.moduleUuidPath()).isEqualTo(".ABCD.EFGH.");
    assertThat(result.parentProjectId()).isEqualTo(2);
    assertThat(result.projectUuid()).isEqualTo("ABCD");
    assertThat(result.key()).isEqualTo("org.struts:struts-core:src/org/struts/RequestContext.java");
    assertThat(result.path()).isEqualTo("src/org/struts/RequestContext.java");
    assertThat(result.name()).isEqualTo("RequestContext.java");
    assertThat(result.longName()).isEqualTo("org.struts.RequestContext");
    assertThat(result.qualifier()).isEqualTo("FIL");
    assertThat(result.scope()).isEqualTo("FIL");
    assertThat(result.language()).isEqualTo("java");
    assertThat(result.getCopyResourceId()).isNull();

    assertThat(underTest.selectByUuid(db.getSession(), "UNKNOWN")).isAbsent();
  }

  @Test
  public void get_by_uuid_on_technical_project_copy() {
    db.prepareDbUnit(getClass(), "shared.xml");

    ComponentDto result = underTest.selectByUuid(db.getSession(), "STUV").get();
    assertThat(result).isNotNull();
    assertThat(result.uuid()).isEqualTo("STUV");
    assertThat(result.moduleUuid()).isEqualTo("OPQR");
    assertThat(result.moduleUuidPath()).isEqualTo(".OPQR.");
    assertThat(result.parentProjectId()).isEqualTo(11);
    assertThat(result.projectUuid()).isEqualTo("OPQR");
    assertThat(result.key()).isEqualTo("DEV:anakin@skywalker.name:org.struts:struts");
    assertThat(result.path()).isNull();
    assertThat(result.name()).isEqualTo("Apache Struts");
    assertThat(result.longName()).isEqualTo("Apache Struts");
    assertThat(result.qualifier()).isEqualTo("DEV_PRJ");
    assertThat(result.scope()).isEqualTo("PRJ");
    assertThat(result.language()).isNull();
    assertThat(result.getCopyResourceId()).isEqualTo(1L);
  }

  @Test
  public void get_by_uuid_on_disabled_component() {
    db.prepareDbUnit(getClass(), "shared.xml");

    ComponentDto result = underTest.selectByUuid(db.getSession(), "DCBA").get();
    assertThat(result).isNotNull();
    assertThat(result.isEnabled()).isFalse();
  }

  @Test
  public void fail_to_get_by_uuid_when_component_not_found() {
    thrown.expect(RowNotFoundException.class);

    db.prepareDbUnit(getClass(), "shared.xml");

    underTest.selectOrFailByUuid(db.getSession(), "unknown");
  }

  @Test
  public void get_by_key() {
    db.prepareDbUnit(getClass(), "shared.xml");

    Optional<ComponentDto> optional = underTest.selectByKey(db.getSession(), "org.struts:struts-core:src/org/struts/RequestContext.java");
    assertThat(optional).isPresent();

    ComponentDto result = optional.get();
    assertThat(result.key()).isEqualTo("org.struts:struts-core:src/org/struts/RequestContext.java");
    assertThat(result.path()).isEqualTo("src/org/struts/RequestContext.java");
    assertThat(result.name()).isEqualTo("RequestContext.java");
    assertThat(result.longName()).isEqualTo("org.struts.RequestContext");
    assertThat(result.qualifier()).isEqualTo("FIL");
    assertThat(result.scope()).isEqualTo("FIL");
    assertThat(result.language()).isEqualTo("java");
    assertThat(result.parentProjectId()).isEqualTo(2);

    assertThat(underTest.selectByKey(db.getSession(), "unknown")).isAbsent();
  }

  @Test
  public void fail_to_get_by_key_when_component_not_found() {
    thrown.expect(RowNotFoundException.class);

    db.prepareDbUnit(getClass(), "shared.xml");

    underTest.selectOrFailByKey(db.getSession(), "unknown");
  }

  @Test
  public void get_by_key_on_disabled_component() {
    db.prepareDbUnit(getClass(), "shared.xml");

    ComponentDto result = underTest.selectOrFailByKey(db.getSession(), "org.disabled.project");
    assertThat(result.isEnabled()).isFalse();
  }

  @Test
  public void get_by_key_on_a_root_project() {
    db.prepareDbUnit(getClass(), "shared.xml");

    ComponentDto result = underTest.selectOrFailByKey(db.getSession(), "org.struts:struts");
    assertThat(result.key()).isEqualTo("org.struts:struts");
    assertThat(result.deprecatedKey()).isEqualTo("org.struts:struts");
    assertThat(result.path()).isNull();
    assertThat(result.name()).isEqualTo("Struts");
    assertThat(result.longName()).isEqualTo("Apache Struts");
    assertThat(result.description()).isEqualTo("the description");
    assertThat(result.qualifier()).isEqualTo("TRK");
    assertThat(result.scope()).isEqualTo("PRJ");
    assertThat(result.language()).isNull();
    assertThat(result.parentProjectId()).isNull();
    assertThat(result.getAuthorizationUpdatedAt()).isEqualTo(123456789L);
  }

  @Test
  public void get_by_keys() {
    db.prepareDbUnit(getClass(), "shared.xml");

    List<ComponentDto> results = underTest.selectByKeys(db.getSession(), Collections.singletonList("org.struts:struts-core:src/org/struts/RequestContext.java"));
    assertThat(results).hasSize(1);

    ComponentDto result = results.get(0);
    assertThat(result).isNotNull();
    assertThat(result.key()).isEqualTo("org.struts:struts-core:src/org/struts/RequestContext.java");
    assertThat(result.path()).isEqualTo("src/org/struts/RequestContext.java");
    assertThat(result.name()).isEqualTo("RequestContext.java");
    assertThat(result.longName()).isEqualTo("org.struts.RequestContext");
    assertThat(result.qualifier()).isEqualTo("FIL");
    assertThat(result.scope()).isEqualTo("FIL");
    assertThat(result.language()).isEqualTo("java");
    assertThat(result.parentProjectId()).isEqualTo(2);

    assertThat(underTest.selectByKeys(db.getSession(), Collections.singletonList("unknown"))).isEmpty();
  }

  @Test
  public void get_by_ids() {
    db.prepareDbUnit(getClass(), "shared.xml");

    List<ComponentDto> results = underTest.selectByIds(db.getSession(), newArrayList(4L));
    assertThat(results).hasSize(1);

    ComponentDto result = results.get(0);
    assertThat(result).isNotNull();
    assertThat(result.key()).isEqualTo("org.struts:struts-core:src/org/struts/RequestContext.java");
    assertThat(result.path()).isEqualTo("src/org/struts/RequestContext.java");
    assertThat(result.name()).isEqualTo("RequestContext.java");
    assertThat(result.longName()).isEqualTo("org.struts.RequestContext");
    assertThat(result.qualifier()).isEqualTo("FIL");
    assertThat(result.scope()).isEqualTo("FIL");
    assertThat(result.language()).isEqualTo("java");
    assertThat(result.parentProjectId()).isEqualTo(2);

    assertThat(underTest.selectByIds(db.getSession(), newArrayList(555L))).isEmpty();
  }

  @Test
  public void get_by_uuids() {
    db.prepareDbUnit(getClass(), "shared.xml");

    List<ComponentDto> results = underTest.selectByUuids(db.getSession(), newArrayList("KLMN"));
    assertThat(results).hasSize(1);

    ComponentDto result = results.get(0);
    assertThat(result).isNotNull();
    assertThat(result.uuid()).isEqualTo("KLMN");
    assertThat(result.moduleUuid()).isEqualTo("EFGH");
    assertThat(result.moduleUuidPath()).isEqualTo(".ABCD.EFGH.");
    assertThat(result.parentProjectId()).isEqualTo(2);
    assertThat(result.projectUuid()).isEqualTo("ABCD");
    assertThat(result.key()).isEqualTo("org.struts:struts-core:src/org/struts/RequestContext.java");
    assertThat(result.path()).isEqualTo("src/org/struts/RequestContext.java");
    assertThat(result.name()).isEqualTo("RequestContext.java");
    assertThat(result.longName()).isEqualTo("org.struts.RequestContext");
    assertThat(result.qualifier()).isEqualTo("FIL");
    assertThat(result.scope()).isEqualTo("FIL");
    assertThat(result.language()).isEqualTo("java");

    assertThat(underTest.selectByUuids(db.getSession(), newArrayList("unknown"))).isEmpty();
  }

  @Test
  public void get_by_uuids_on_removed_components() {
    db.prepareDbUnit(getClass(), "shared.xml");

    List<ComponentDto> results = underTest.selectByUuids(db.getSession(), newArrayList("DCBA"));
    assertThat(results).hasSize(1);

    ComponentDto result = results.get(0);
    assertThat(result).isNotNull();
    assertThat(result.isEnabled()).isFalse();
  }

  @Test
  public void select_existing_uuids() {
    db.prepareDbUnit(getClass(), "shared.xml");

    List<String> results = underTest.selectExistingUuids(db.getSession(), newArrayList("KLMN"));
    assertThat(results).containsOnly("KLMN");

    assertThat(underTest.selectExistingUuids(db.getSession(), newArrayList("KLMN", "unknown"))).hasSize(1);
    assertThat(underTest.selectExistingUuids(db.getSession(), newArrayList("unknown"))).isEmpty();
  }

  @Test
  public void get_by_id() {
    db.prepareDbUnit(getClass(), "shared.xml");

    assertThat(underTest.selectOrFailById(db.getSession(), 4L)).isNotNull();
  }

  @Test
  public void get_by_id_on_disabled_component() {
    db.prepareDbUnit(getClass(), "shared.xml");

    Optional<ComponentDto> result = underTest.selectById(db.getSession(), 10L);
    assertThat(result).isPresent();
    assertThat(result.get().isEnabled()).isFalse();
  }

  @Test(expected = RowNotFoundException.class)
  public void fail_to_get_by_id_when_project_not_found() {
    db.prepareDbUnit(getClass(), "shared.xml");

    underTest.selectOrFailById(db.getSession(), 111L);
  }

  @Test
  public void get_nullable_by_id() {
    db.prepareDbUnit(getClass(), "shared.xml");

    assertThat(underTest.selectById(db.getSession(), 4L)).isPresent();
    assertThat(underTest.selectById(db.getSession(), 111L)).isAbsent();
  }

  @Test
  public void count_by_id() {
    db.prepareDbUnit(getClass(), "shared.xml");

    assertThat(underTest.existsById(4L, db.getSession())).isTrue();
    assertThat(underTest.existsById(111L, db.getSession())).isFalse();
  }

  @Test
  public void find_sub_projects_by_component_keys() {
    db.prepareDbUnit(getClass(), "multi-modules.xml");

    // Sub project of a file
    List<ComponentDto> results = underTest.selectSubProjectsByComponentUuids(db.getSession(), newArrayList("HIJK"));
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getKey()).isEqualTo("org.struts:struts-data");

    // Sub project of a directory
    results = underTest.selectSubProjectsByComponentUuids(db.getSession(), newArrayList("GHIJ"));
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getKey()).isEqualTo("org.struts:struts-data");

    // Sub project of a sub module
    results = underTest.selectSubProjectsByComponentUuids(db.getSession(), newArrayList("FGHI"));
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getKey()).isEqualTo("org.struts:struts");

    // Sub project of a module
    results = underTest.selectSubProjectsByComponentUuids(db.getSession(), newArrayList("EFGH"));
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getKey()).isEqualTo("org.struts:struts");

    // Sub project of a project
    assertThat(underTest.selectSubProjectsByComponentUuids(db.getSession(), newArrayList("ABCD"))).isEmpty();

    // SUb projects of a component and a sub module
    assertThat(underTest.selectSubProjectsByComponentUuids(db.getSession(), newArrayList("HIJK", "FGHI"))).hasSize(2);

    assertThat(underTest.selectSubProjectsByComponentUuids(db.getSession(), newArrayList("unknown"))).isEmpty();

    assertThat(underTest.selectSubProjectsByComponentUuids(db.getSession(), Collections.<String>emptyList())).isEmpty();
  }

  @Test
  public void select_enabled_modules_tree() {
    db.prepareDbUnit(getClass(), "multi-modules.xml");

    // From root project
    List<ComponentDto> modules = underTest.selectEnabledDescendantModules(db.getSession(), "ABCD");
    assertThat(modules).extracting("uuid").containsOnly("ABCD", "EFGH", "FGHI");

    // From module
    modules = underTest.selectEnabledDescendantModules(db.getSession(), "EFGH");
    assertThat(modules).extracting("uuid").containsOnly("EFGH", "FGHI");

    // From sub module
    modules = underTest.selectEnabledDescendantModules(db.getSession(), "FGHI");
    assertThat(modules).extracting("uuid").containsOnly("FGHI");

    // Folder
    assertThat(underTest.selectEnabledDescendantModules(db.getSession(), "GHIJ")).isEmpty();
    assertThat(underTest.selectEnabledDescendantModules(db.getSession(), "unknown")).isEmpty();
  }

  @Test
  public void select_all_modules_tree() {
    db.prepareDbUnit(getClass(), "multi-modules.xml");

    // From root project, disabled sub module is returned
    List<ComponentDto> modules = underTest.selectDescendantModules(db.getSession(), "ABCD");
    assertThat(modules).extracting("uuid").containsOnly("ABCD", "EFGH", "FGHI", "IHGF");

    // From module, disabled sub module is returned
    modules = underTest.selectDescendantModules(db.getSession(), "EFGH");
    assertThat(modules).extracting("uuid").containsOnly("EFGH", "FGHI", "IHGF");

    // From removed sub module -> should not be returned
    assertThat(underTest.selectDescendantModules(db.getSession(), "IHGF")).isEmpty();
  }

  @Test
  public void select_enabled_module_files_tree_from_module() {
    db.prepareDbUnit(getClass(), "select_module_files_tree.xml");

    // From root project
    List<FilePathWithHashDto> files = underTest.selectEnabledDescendantFiles(db.getSession(), "ABCD");
    assertThat(files).extracting("uuid").containsOnly("EFGHI", "HIJK");
    assertThat(files).extracting("moduleUuid").containsOnly("EFGH", "FGHI");
    assertThat(files).extracting("srcHash").containsOnly("srcEFGHI", "srcHIJK");
    assertThat(files).extracting("path").containsOnly("src/org/struts/pom.xml", "src/org/struts/RequestContext.java");

    // From module
    files = underTest.selectEnabledDescendantFiles(db.getSession(), "EFGH");
    assertThat(files).extracting("uuid").containsOnly("EFGHI", "HIJK");
    assertThat(files).extracting("moduleUuid").containsOnly("EFGH", "FGHI");
    assertThat(files).extracting("srcHash").containsOnly("srcEFGHI", "srcHIJK");
    assertThat(files).extracting("path").containsOnly("src/org/struts/pom.xml", "src/org/struts/RequestContext.java");

    // From sub module
    files = underTest.selectEnabledDescendantFiles(db.getSession(), "FGHI");
    assertThat(files).extracting("uuid").containsOnly("HIJK");
    assertThat(files).extracting("moduleUuid").containsOnly("FGHI");
    assertThat(files).extracting("srcHash").containsOnly("srcHIJK");
    assertThat(files).extracting("path").containsOnly("src/org/struts/RequestContext.java");

    // From directory
    assertThat(underTest.selectEnabledDescendantFiles(db.getSession(), "GHIJ")).isEmpty();

    assertThat(underTest.selectEnabledDescendantFiles(db.getSession(), "unknown")).isEmpty();
  }

  @Test
  public void select_enabled_module_files_tree_from_project() {
    db.prepareDbUnit(getClass(), "select_module_files_tree.xml");

    // From root project
    List<FilePathWithHashDto> files = underTest.selectEnabledFilesFromProject(db.getSession(), "ABCD");
    assertThat(files).extracting("uuid").containsOnly("EFGHI", "HIJK");
    assertThat(files).extracting("moduleUuid").containsOnly("EFGH", "FGHI");
    assertThat(files).extracting("srcHash").containsOnly("srcEFGHI", "srcHIJK");
    assertThat(files).extracting("path").containsOnly("src/org/struts/pom.xml", "src/org/struts/RequestContext.java");

    // From module
    assertThat(underTest.selectEnabledFilesFromProject(db.getSession(), "EFGH")).isEmpty();

    // From sub module
    assertThat(underTest.selectEnabledFilesFromProject(db.getSession(), "FGHI")).isEmpty();

    // From directory
    assertThat(underTest.selectEnabledFilesFromProject(db.getSession(), "GHIJ")).isEmpty();

    assertThat(underTest.selectEnabledFilesFromProject(db.getSession(), "unknown")).isEmpty();
  }

  @Test
  public void select_all_components_from_project() {
    db.prepareDbUnit(getClass(), "multi-modules.xml");

    List<ComponentDto> components = underTest.selectAllComponentsFromProjectKey(db.getSession(), "org.struts:struts");
    // Removed components are included
    assertThat(components).hasSize(8);

    assertThat(underTest.selectAllComponentsFromProjectKey(db.getSession(), "UNKNOWN")).isEmpty();
  }

  @Test
  public void select_modules_from_project() {
    db.prepareDbUnit(getClass(), "multi-modules.xml");

    List<ComponentDto> components = underTest.selectEnabledModulesFromProjectKey(db.getSession(), "org.struts:struts");
    assertThat(components).hasSize(3);

    assertThat(underTest.selectEnabledModulesFromProjectKey(db.getSession(), "UNKNOWN")).isEmpty();
  }

  @Test
  public void select_views_and_sub_views() {
    db.prepareDbUnit(getClass(), "shared_views.xml");

    assertThat(underTest.selectAllViewsAndSubViews(db.getSession())).extracting("uuid").containsOnly("ABCD", "EFGH", "FGHI", "IJKL");
    assertThat(underTest.selectAllViewsAndSubViews(db.getSession())).extracting("projectUuid").containsOnly("ABCD", "EFGH", "IJKL");
  }

  @Test
  public void select_projects_from_view() {
    db.prepareDbUnit(getClass(), "shared_views.xml");

    assertThat(underTest.selectProjectsFromView(db.getSession(), "ABCD", "ABCD")).containsOnly("JKLM");
    assertThat(underTest.selectProjectsFromView(db.getSession(), "EFGH", "EFGH")).containsOnly("KLMN", "JKLM");
    assertThat(underTest.selectProjectsFromView(db.getSession(), "FGHI", "EFGH")).containsOnly("JKLM");
    assertThat(underTest.selectProjectsFromView(db.getSession(), "IJKL", "IJKL")).isEmpty();
    assertThat(underTest.selectProjectsFromView(db.getSession(), "Unknown", "Unknown")).isEmpty();
  }

  @Test
  public void select_projects() {
    db.prepareDbUnit(getClass(), "select_provisioned_projects.xml");

    List<ComponentDto> result = underTest.selectProjects(db.getSession());

    assertThat(result).extracting("id").containsOnly(42L, 1L);
  }

  @Test
  public void select_provisioned_projects() {
    db.prepareDbUnit(getClass(), "select_provisioned_projects.xml");

    List<ComponentDto> result = underTest.selectProvisionedProjects(db.getSession(), 0, 10, null);
    ComponentDto project = result.get(0);

    assertThat(result).hasSize(1);
    assertThat(project.getKey()).isEqualTo("org.provisioned.project");
  }

  @Test
  public void count_provisioned_projects() {
    db.prepareDbUnit(getClass(), "select_provisioned_projects.xml");

    int numberOfProjects = underTest.countProvisionedProjects(db.getSession(), null);

    assertThat(numberOfProjects).isEqualTo(1);
  }

  @Test
  public void select_ghost_projects() {
    db.prepareDbUnit(getClass(), "select_ghost_projects.xml");

    List<ComponentDto> result = underTest.selectGhostProjects(db.getSession(), 0, 10, null);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).key()).isEqualTo("org.ghost.project");
    assertThat(underTest.countGhostProjects(db.getSession(), null)).isEqualTo(1);
  }

  @Test
  public void selectResourcesByRootId() {
    db.prepareDbUnit(getClass(), "shared.xml");

    List<ComponentDto> resources = underTest.selectByProjectUuid("ABCD", db.getSession());

    assertThat(resources).extracting("id").containsOnly(1l, 2l, 3l, 4l);
  }

  @Test
  public void insert() {
    db.prepareDbUnit(getClass(), "empty.xml");

    ComponentDto componentDto = new ComponentDto()
      .setId(1L)
      .setUuid("GHIJ")
      .setProjectUuid("ABCD")
      .setModuleUuid("EFGH")
      .setModuleUuidPath(".ABCD.EFGH.")
      .setKey("org.struts:struts-core:src/org/struts/RequestContext.java")
      .setDeprecatedKey("org.struts:struts-core:src/org/struts/RequestContext.java")
      .setName("RequestContext.java")
      .setLongName("org.struts.RequestContext")
      .setQualifier("FIL")
      .setScope("FIL")
      .setLanguage("java")
      .setDescription("description")
      .setPath("src/org/struts/RequestContext.java")
      .setParentProjectId(3L)
      .setCopyResourceId(5L)
      .setEnabled(true)
      .setCreatedAt(DateUtils.parseDate("2014-06-18"))
      .setAuthorizationUpdatedAt(123456789L);

    underTest.insert(db.getSession(), componentDto);
    db.getSession().commit();

    assertThat(componentDto.getId()).isNotNull();
    db.assertDbUnit(getClass(), "insert-result.xml", "projects");
  }

  @Test
  public void insert_disabled_component() {
    db.prepareDbUnit(getClass(), "empty.xml");

    ComponentDto componentDto = new ComponentDto()
      .setId(1L)
      .setUuid("GHIJ")
      .setProjectUuid("ABCD")
      .setModuleUuid("EFGH")
      .setModuleUuidPath(".ABCD.EFGH.")
      .setKey("org.struts:struts-core:src/org/struts/RequestContext.java")
      .setName("RequestContext.java")
      .setLongName("org.struts.RequestContext")
      .setQualifier("FIL")
      .setScope("FIL")
      .setLanguage("java")
      .setPath("src/org/struts/RequestContext.java")
      .setParentProjectId(3L)
      .setEnabled(false)
      .setCreatedAt(DateUtils.parseDate("2014-06-18"))
      .setAuthorizationUpdatedAt(123456789L);

    underTest.insert(db.getSession(), componentDto);
    db.getSession().commit();

    assertThat(componentDto.getId()).isNotNull();
    db.assertDbUnit(getClass(), "insert_disabled_component-result.xml", "projects");
  }

  @Test
  public void update() {
    db.prepareDbUnit(getClass(), "update.xml");

    ComponentDto componentDto = new ComponentDto()
      .setUuid("GHIJ")
      .setProjectUuid("DCBA")
      .setModuleUuid("HGFE")
      .setModuleUuidPath(".DCBA.HGFE.")
      .setKey("org.struts:struts-core:src/org/struts/RequestContext2.java")
      .setDeprecatedKey("org.struts:struts-core:src/org/struts/RequestContext2.java")
      .setName("RequestContext2.java")
      .setLongName("org.struts.RequestContext2")
      .setQualifier("LIF")
      .setScope("LIF")
      .setLanguage("java2")
      .setDescription("description2")
      .setPath("src/org/struts/RequestContext2.java")
      .setParentProjectId(4L)
      .setCopyResourceId(6L)
      .setEnabled(false)
      .setAuthorizationUpdatedAt(12345678910L);

    underTest.update(db.getSession(), componentDto);
    db.getSession().commit();

    db.assertDbUnit(getClass(), "update-result.xml", "projects");
  }

  @Test
  public void select_components_with_paging_query_and_qualifiers() {
    DbSession session = db.getSession();
    underTest.insert(session, newProjectDto().setName("aaaa-name"));
    underTest.insert(session, newView());
    underTest.insert(session, newDeveloper("project-name"));
    for (int i = 9; i >= 1; i--) {
      underTest.insert(session, newProjectDto().setName("project-" + i));
    }

    List<ComponentDto> result = underTest.selectComponents(session, singleton(Qualifiers.PROJECT), 1, 3, "project");

    assertThat(result).hasSize(3);
    assertThat(result).extracting("name").containsExactly("project-2", "project-3", "project-4");
  }
}
