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
import org.sonar.xoo.XooPlugin;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class CoveragePerTestMediumTest {

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
  public void coveragePerTestInReport() throws IOException {

    File baseDir = temp.getRoot();
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();
    File testDir = new File(baseDir, "test");
    testDir.mkdir();

    File xooFile = new File(srcDir, "sample.xoo");
    FileUtils.write(xooFile, "foo");

    File xooFile2 = new File(srcDir, "sample2.xoo");
    FileUtils.write(xooFile2, "foo");

    File xooTestFile = new File(testDir, "sampleTest.xoo");
    FileUtils.write(xooTestFile, "failure\nerror\nok\nskipped");

    File xooTestFile2 = new File(testDir, "sample2Test.xoo");
    FileUtils.write(xooTestFile2, "test file tests");

    File xooTestExecutionFile = new File(testDir, "sampleTest.xoo.test");
    FileUtils.write(xooTestExecutionFile, "some test:4:::OK:UNIT\n" +
      "another test:10:::OK:UNIT\n" +
      "test without coverage:10:::OK:UNIT\n");

    File xooCoveragePerTestFile = new File(testDir, "sampleTest.xoo.testcoverage");
    FileUtils.write(xooCoveragePerTestFile, "some test;src/sample.xoo,10,11;src/sample2.xoo,1,2\n" +
      "another test;src/sample.xoo,10,20\n");

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
    org.sonar.batch.protocol.output.BatchReport.CoverageDetail someTest = result.coveragePerTestFor(file, "some test");
    assertThat(someTest.getCoveredFileList()).hasSize(2);
    assertThat(someTest.getCoveredFile(0).getFileRef()).isGreaterThan(0);
    assertThat(someTest.getCoveredFile(0).getCoveredLineList()).containsExactly(10, 11);
    assertThat(someTest.getCoveredFile(1).getFileRef()).isGreaterThan(0);
    assertThat(someTest.getCoveredFile(1).getCoveredLineList()).containsExactly(1, 2);

    org.sonar.batch.protocol.output.BatchReport.CoverageDetail anotherTest = result.coveragePerTestFor(file, "another test");
    assertThat(anotherTest.getCoveredFileList()).hasSize(1);
    assertThat(anotherTest.getCoveredFile(0).getFileRef()).isGreaterThan(0);
    assertThat(anotherTest.getCoveredFile(0).getCoveredLineList()).containsExactly(10, 20);
  }

}
