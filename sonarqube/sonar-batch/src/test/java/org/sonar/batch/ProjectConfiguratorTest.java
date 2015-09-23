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
package org.sonar.batch;

import java.text.SimpleDateFormat;
import java.util.TimeZone;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.System2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProjectConfiguratorTest {

  System2 system2;

  @Before
  public void setUp() {
    system2 = mock(System2.class);
  }

  @Test
  public void analysis_is_today_by_default() {
    Long now = System.currentTimeMillis();
    when(system2.now()).thenReturn(now);

    Project project = new Project("key");
    new ProjectConfigurator(new Settings(), system2).configure(project);
    assertThat(now - project.getAnalysisDate().getTime()).isLessThan(1000);
  }

  @Test
  public void analysis_date_could_be_explicitly_set() {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.PROJECT_DATE_PROPERTY, "2005-01-30");
    Project project = new Project("key");
    new ProjectConfigurator(settings, system2).configure(project);

    assertThat(new SimpleDateFormat("ddMMyyyy").format(project.getAnalysisDate())).isEqualTo("30012005");
  }

  @Test
  public void analysis_timestamp_could_be_explicitly_set() {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.PROJECT_DATE_PROPERTY, "2005-01-30T08:45:10+0000");
    Project project = new Project("key");
    new ProjectConfigurator(settings, system2).configure(project);

    SimpleDateFormat dateFormat = new SimpleDateFormat("ddMMyyyy-mmss");
    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    assertThat(dateFormat.format(project.getAnalysisDate())).isEqualTo("30012005-4510");
  }

  @Test(expected = RuntimeException.class)
  public void fail_if_analyis_date_is_not_valid() {
    Settings configuration = new Settings();
    configuration.setProperty(CoreProperties.PROJECT_DATE_PROPERTY, "2005/30/01");
    Project project = new Project("key");
    new ProjectConfigurator(configuration, system2).configure(project);
  }

  @Test
  public void default_analysis_type_is_dynamic() {
    Project project = new Project("key");
    new ProjectConfigurator(new Settings(), system2).configure(project);
    assertThat(project.getAnalysisType()).isEqualTo(Project.AnalysisType.DYNAMIC);
  }

  @Test
  public void explicit_dynamic_analysis() {
    Settings configuration = new Settings();
    configuration.setProperty(CoreProperties.DYNAMIC_ANALYSIS_PROPERTY, "true");
    Project project = new Project("key");
    new ProjectConfigurator(configuration, system2).configure(project);
    assertThat(project.getAnalysisType()).isEqualTo(Project.AnalysisType.DYNAMIC);
  }

  @Test
  public void explicit_static_analysis() {
    Settings configuration = new Settings();
    configuration.setProperty(CoreProperties.DYNAMIC_ANALYSIS_PROPERTY, "false");
    Project project = new Project("key");
    new ProjectConfigurator(configuration, system2).configure(project);
    assertThat(project.getAnalysisType()).isEqualTo(Project.AnalysisType.STATIC);
  }

  @Test
  public void explicit_dynamic_analysis_reusing_reports() {
    Settings configuration = new Settings();
    configuration.setProperty(CoreProperties.DYNAMIC_ANALYSIS_PROPERTY, "reuseReports");
    Project project = new Project("key");
    new ProjectConfigurator(configuration, system2).configure(project);
    assertThat(project.getAnalysisType()).isEqualTo(Project.AnalysisType.REUSE_REPORTS);
  }

  @Test
  public void is_dynamic_analysis() {
    assertThat(Project.AnalysisType.DYNAMIC.isDynamic(false)).isTrue();
    assertThat(Project.AnalysisType.DYNAMIC.isDynamic(true)).isTrue();

    assertThat(Project.AnalysisType.STATIC.isDynamic(false)).isFalse();
    assertThat(Project.AnalysisType.STATIC.isDynamic(true)).isFalse();

    assertThat(Project.AnalysisType.REUSE_REPORTS.isDynamic(false)).isFalse();
    assertThat(Project.AnalysisType.REUSE_REPORTS.isDynamic(true)).isTrue();
  }

}
