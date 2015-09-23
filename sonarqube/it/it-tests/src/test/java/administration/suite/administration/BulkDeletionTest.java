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
package administration.suite.administration;

import administration.suite.AdministrationTestSuite;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import selenium.SeleneseTest;

import static util.ItUtils.projectDir;

public class BulkDeletionTest {

  @ClassRule
  public static Orchestrator orchestrator = AdministrationTestSuite.ORCHESTRATOR;

  @Before
  public void deleteData() {
    orchestrator.resetData();
  }

  /**
   * SONAR-2614, SONAR-3805
   */
  @Test
  public void test_bulk_deletion_on_selected_projects() throws Exception {
    // we must have several projects to test the bulk deletion
    executeBuild("cameleon-1", "Sample-Project");
    executeBuild("cameleon-2", "Foo-Application");
    executeBuild("cameleon-3", "Bar-Sonar-Plugin");

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("project-bulk-deletion-on-selected-project",
      "/administration/suite/BulkDeletionTest/project-bulk-deletion/bulk-delete-filter-projects.html"
    ).build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

  /**
   * SONAR-4560
   */
  @Test
  public void should_support_two_letters_long_project_name() throws Exception {
    executeBuild("xo", "xo");

    Selenese selenese = Selenese.builder()
      .setHtmlTestsInClasspath("bulk-delete-projects-with-short-name",
        "/administration/suite/BulkDeletionTest/project-bulk-deletion/display-two-letters-long-project.html",
        "/administration/suite/BulkDeletionTest/project-bulk-deletion/filter-two-letters-long-project.html"
      ).build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

  private void executeBuild(String projectKey, String projectName) {
    orchestrator.executeBuild(
      SonarRunner.create(projectDir("shared/xoo-sample"))
        .setProjectKey(projectKey)
        .setProjectName(projectName)
    );
  }

}
