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
package org.sonar.xoo.coverage;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.coverage.CoverageType;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class UtCoverageSensorTest {

  private UtCoverageSensor sensor;
  private SensorContextTester context;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private File baseDir;

  @Before
  public void prepare() throws IOException {
    baseDir = temp.newFolder();
    sensor = new UtCoverageSensor();
    context = SensorContextTester.create(baseDir);
  }

  @Test
  public void testDescriptor() {
    sensor.describe(new DefaultSensorDescriptor());
  }

  @Test
  public void testNoExecutionIfNoCoverageFile() {
    DefaultInputFile inputFile = new DefaultInputFile("foo", "src/foo.xoo").setLanguage("xoo");
    context.fileSystem().add(inputFile);
    sensor.execute(context);
  }

  @Test
  public void testLineHitNoConditions() throws IOException {
    File coverage = new File(baseDir, "src/foo.xoo.coverage");
    FileUtils.write(coverage, "1:3\n\n#comment");
    DefaultInputFile inputFile = new DefaultInputFile("foo", "src/foo.xoo").setLanguage("xoo");
    context.fileSystem().add(inputFile);

    sensor.execute(context);

    assertThat(context.lineHits("foo:src/foo.xoo", CoverageType.UNIT, 1)).isEqualTo(3);
    assertThat(context.lineHits("foo:src/foo.xoo", CoverageType.IT, 1)).isNull();
    assertThat(context.lineHits("foo:src/foo.xoo", CoverageType.OVERALL, 1)).isNull();
  }

  @Test
  public void testLineHitAndConditions() throws IOException {
    File coverage = new File(baseDir, "src/foo.xoo.coverage");
    FileUtils.write(coverage, "1:3:4:2");
    DefaultInputFile inputFile = new DefaultInputFile("foo", "src/foo.xoo").setLanguage("xoo");
    context.fileSystem().add(inputFile);

    sensor.execute(context);

    assertThat(context.lineHits("foo:src/foo.xoo", CoverageType.UNIT, 1)).isEqualTo(3);
    assertThat(context.conditions("foo:src/foo.xoo", CoverageType.UNIT, 1)).isEqualTo(4);
    assertThat(context.conditions("foo:src/foo.xoo", CoverageType.IT, 1)).isNull();
    assertThat(context.conditions("foo:src/foo.xoo", CoverageType.OVERALL, 1)).isNull();
    assertThat(context.coveredConditions("foo:src/foo.xoo", CoverageType.UNIT, 1)).isEqualTo(2);
    assertThat(context.coveredConditions("foo:src/foo.xoo", CoverageType.IT, 1)).isNull();
    assertThat(context.coveredConditions("foo:src/foo.xoo", CoverageType.OVERALL, 1)).isNull();
  }
}
