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
package org.sonar.batch.scan;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.utils.MessageException;
import org.sonar.batch.analysis.DefaultAnalysisMode;
import org.sonar.batch.bootstrap.GlobalSettings;
import org.sonar.batch.protocol.input.FileData;
import org.sonar.batch.report.AnalysisContextReportPublisher;
import org.sonar.batch.repository.ProjectSettingsRepo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ModuleSettingsTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private DefaultAnalysisMode mode;

  @Before
  public void before() {
    mode = mock(DefaultAnalysisMode.class);
  }

  private ProjectSettingsRepo createSettings(String module, Map<String, String> settingsMap) {
    Table<String, String, FileData> fileData = ImmutableTable.of();
    Table<String, String, String> settings = HashBasedTable.create();

    for (Map.Entry<String, String> e : settingsMap.entrySet()) {
      settings.put(module, e.getKey(), e.getValue());
    }
    return new ProjectSettingsRepo(settings, fileData, null);
  }

  @Test
  public void testOrderedProjects() {
    ProjectDefinition grandParent = ProjectDefinition.create();
    ProjectDefinition parent = ProjectDefinition.create();
    ProjectDefinition child = ProjectDefinition.create();
    grandParent.addSubProject(parent);
    parent.addSubProject(child);

    List<ProjectDefinition> hierarchy = ModuleSettings.getTopDownParentProjects(child);
    assertThat(hierarchy.get(0)).isEqualTo(grandParent);
    assertThat(hierarchy.get(1)).isEqualTo(parent);
    assertThat(hierarchy.get(2)).isEqualTo(child);
  }

  @Test
  public void test_loading_of_module_settings() {
    GlobalSettings batchSettings = mock(GlobalSettings.class);
    when(batchSettings.getDefinitions()).thenReturn(new PropertyDefinitions());
    when(batchSettings.getProperties()).thenReturn(ImmutableMap.of(
      "overridding", "batch",
      "on-batch", "true"));

    ProjectSettingsRepo projSettingsRepo = createSettings("struts-core", ImmutableMap.of("on-module", "true", "overridding", "module"));

    ProjectDefinition module = ProjectDefinition.create().setKey("struts-core");

    ModuleSettings moduleSettings = new ModuleSettings(batchSettings, module, projSettingsRepo, mode, mock(AnalysisContextReportPublisher.class));

    assertThat(moduleSettings.getString("overridding")).isEqualTo("module");
    assertThat(moduleSettings.getString("on-batch")).isEqualTo("true");
    assertThat(moduleSettings.getString("on-module")).isEqualTo("true");

  }

  @Test
  public void should_not_fail_when_accessing_secured_properties() {
    GlobalSettings batchSettings = mock(GlobalSettings.class);
    when(batchSettings.getDefinitions()).thenReturn(new PropertyDefinitions());
    when(batchSettings.getProperties()).thenReturn(ImmutableMap.of(
      "sonar.foo.secured", "bar"));

    ProjectSettingsRepo projSettingsRepo = createSettings("struts-core", ImmutableMap.of("sonar.foo.license.secured", "bar2"));

    ProjectDefinition module = ProjectDefinition.create().setKey("struts-core");

    ModuleSettings moduleSettings = new ModuleSettings(batchSettings, module, projSettingsRepo, mode, mock(AnalysisContextReportPublisher.class));

    assertThat(moduleSettings.getString("sonar.foo.license.secured")).isEqualTo("bar2");
    assertThat(moduleSettings.getString("sonar.foo.secured")).isEqualTo("bar");
  }

  @Test
  public void should_fail_when_accessing_secured_properties_in_issues() {
    GlobalSettings batchSettings = mock(GlobalSettings.class);
    when(batchSettings.getDefinitions()).thenReturn(new PropertyDefinitions());
    when(batchSettings.getProperties()).thenReturn(ImmutableMap.of(
      "sonar.foo.secured", "bar"));

    ProjectSettingsRepo projSettingsRepo = createSettings("struts-core", ImmutableMap.of("sonar.foo.license.secured", "bar2"));

    when(mode.isIssues()).thenReturn(true);

    ProjectDefinition module = ProjectDefinition.create().setKey("struts-core");

    ModuleSettings moduleSettings = new ModuleSettings(batchSettings, module, projSettingsRepo, mode, mock(AnalysisContextReportPublisher.class));

    assertThat(moduleSettings.getString("sonar.foo.license.secured")).isEqualTo("bar2");

    thrown.expect(MessageException.class);
    thrown
      .expectMessage(
        "Access to the secured property 'sonar.foo.secured' is not possible in issues mode. The SonarQube plugin which requires this property must be deactivated in issues mode.");
    moduleSettings.getString("sonar.foo.secured");
  }
}
