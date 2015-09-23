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
package org.sonar.batch.mediumtest.tests;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.batch.mediumtest.BatchMediumTester;
import org.sonar.batch.mediumtest.TaskResult;
import org.sonar.batch.protocol.Constants.TestStatus;
import org.sonar.xoo.XooPlugin;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class TestExecutionMediumTest {

  @org.junit.Rule
  public TemporaryFolder temp = new TemporaryFolder();

  public BatchMediumTester tester = BatchMediumTester.builder()
    .registerPlugin("xoo", new XooPlugin())
    .addDefaultQProfile("xoo", "Sonar Way")
    .build();

  @Before
  public void prepare() {
    tester.start();
  }

  @After
  public void stop() {
    tester.stop();
  }

  @Test
  public void unitTests() throws IOException {

    File baseDir = temp.getRoot();
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();
    File testDir = new File(baseDir, "test");
    testDir.mkdir();

    File xooFile = new File(srcDir, "sample.xoo");
    FileUtils.write(xooFile, "foo");

    File xooTestFile = new File(testDir, "sampleTest.xoo");
    FileUtils.write(xooTestFile, "failure\nerror\nok\nskipped");

    File xooTestExecutionFile = new File(testDir, "sampleTest.xoo.test");
    FileUtils.write(xooTestExecutionFile, "skipped::::SKIPPED:UNIT\n" +
      "failure:2:Failure::FAILURE:UNIT\n" +
      "error:2:Error:The stack:ERROR:UNIT\n" +
      "success:4:::OK:INTEGRATION");

    TaskResult result = tester.newTask()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.task", "scan")
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.projectName", "Foo Project")
        .put("sonar.projectVersion", "1.0-SNAPSHOT")
        .put("sonar.projectDescription", "Description of Foo Project")
        .put("sonar.sources", "src")
        .put("sonar.tests", "test")
        .build())
      .start();

    InputFile file = result.inputFile("test/sampleTest.xoo");
    org.sonar.batch.protocol.output.BatchReport.Test success = result.testExecutionFor(file, "success");
    assertThat(success.getDurationInMs()).isEqualTo(4);
    assertThat(success.getStatus()).isEqualTo(TestStatus.OK);

    org.sonar.batch.protocol.output.BatchReport.Test error = result.testExecutionFor(file, "error");
    assertThat(error.getDurationInMs()).isEqualTo(2);
    assertThat(error.getStatus()).isEqualTo(TestStatus.ERROR);
    assertThat(error.getMsg()).isEqualTo("Error");
    assertThat(error.getStacktrace()).isEqualTo("The stack");
  }

}
