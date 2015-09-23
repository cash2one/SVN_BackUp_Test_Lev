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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.measure.custom.CustomMeasureDto;
import org.sonar.db.measure.custom.CustomMeasureTesting;
import org.sonar.server.computation.batch.BatchReportReaderRule;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.ReportComponent;
import org.sonar.server.computation.component.ViewsComponent;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureRepositoryRule;
import org.sonar.server.computation.metric.Metric;
import org.sonar.server.computation.metric.MetricImpl;
import org.sonar.server.computation.metric.MetricRepositoryRule;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.sonar.server.computation.component.Component.Type.DIRECTORY;
import static org.sonar.server.computation.component.Component.Type.FILE;
import static org.sonar.server.computation.component.Component.Type.MODULE;
import static org.sonar.server.computation.component.Component.Type.PROJECT;
import static org.sonar.server.computation.component.Component.Type.PROJECT_VIEW;
import static org.sonar.server.computation.component.Component.Type.SUBVIEW;
import static org.sonar.server.computation.component.Component.Type.VIEW;
import static org.sonar.server.computation.component.ReportComponent.builder;
import static org.sonar.server.computation.measure.Measure.newMeasureBuilder;
import static org.sonar.server.computation.measure.MeasureRepoEntry.entryOf;
import static org.sonar.server.computation.measure.MeasureRepoEntry.toEntries;
import static org.sonar.server.computation.step.CustomMeasuresCopyStep.dtoToMeasure;

@Category(DbTests.class)
public class CustomMeasuresCopyStepTest {

  static final int PROJECT_REF = 1;
  static final int MODULE_REF = 11;
  static final int SUB_MODULE_REF = 111;
  static final int DIR_REF = 1111;
  static final int FILE1_REF = 11111;
  static final int FILE2_REF = 11112;

  static final String PROJECT_UUID = "PROJECT";
  static final String MODULE_UUID = "MODULE";
  static final String SUB_MODULE_UUID = "SUB_MODULE";
  static final String DIR_UUID = "DIR";
  static final String FILE1_UUID = "FILE1";
  static final String FILE2_UUID = "FILE2";

  static final String VIEW_UUID = "VIEW";
  static final String SUBVIEW_UUID = "SUBVIEW";

  static final int VIEW_REF = 10;
  static final int SUBVIEW_REF = 101;
  static final int PROJECT_VIEW_REF = 1011;

  static final Metric FLOAT_METRIC = new MetricImpl(10, "float_metric", "Float Metric", Metric.MetricType.FLOAT);
  static final Metric STRING_METRIC = new MetricImpl(11, "string_metric", "String Metric", Metric.MetricType.STRING);

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(FLOAT_METRIC)
    .add(STRING_METRIC);

  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  CustomMeasuresCopyStep underTest;

  @Before
  public void setUp() {
    DbClient dbClient = dbTester.getDbClient();
    underTest = new CustomMeasuresCopyStep(treeRootHolder, dbClient, metricRepository, measureRepository);
  }

  @Test
  public void copy_custom_measures_on_report() {
    // File2 has no custom measure
    insertCustomMeasure(FILE1_UUID, FLOAT_METRIC, 3.14);
    insertCustomMeasure(DIR_UUID, FLOAT_METRIC, 123d);
    insertCustomMeasure(SUB_MODULE_UUID, STRING_METRIC, "sub-module");
    insertCustomMeasure(MODULE_UUID, STRING_METRIC, "module");
    insertCustomMeasure(PROJECT_UUID, STRING_METRIC, "project");

    treeRootHolder.setRoot(
      builder(PROJECT, PROJECT_REF).setUuid(PROJECT_UUID)
        .addChildren(
          ReportComponent.builder(MODULE, MODULE_REF).setUuid(MODULE_UUID)
            .addChildren(
              ReportComponent.builder(MODULE, SUB_MODULE_REF).setUuid(SUB_MODULE_UUID)
                .addChildren(
                  ReportComponent.builder(DIRECTORY, DIR_REF).setUuid(DIR_UUID)
                    .addChildren(
                      ReportComponent.builder(FILE, FILE1_REF).setUuid(FILE1_UUID).build(),
                      ReportComponent.builder(FILE, FILE2_REF).setUuid(FILE2_UUID).build()
                    )
                    .build()
                )
                .build()
            )
            .build())
        .build());

    underTest.execute();

    assertRawMeasureValue(FILE1_REF, FLOAT_METRIC.getKey(), 3.1d);
    assertNoRawMeasureValue(FILE2_REF);
    assertRawMeasureValue(DIR_REF, FLOAT_METRIC.getKey(), 123d);
    assertRawMeasureValue(SUB_MODULE_REF, STRING_METRIC.getKey(), "sub-module");
    assertRawMeasureValue(MODULE_REF, STRING_METRIC.getKey(), "module");
    assertRawMeasureValue(PROJECT_REF, STRING_METRIC.getKey(), "project");
  }

  @Test
  public void copy_custom_measures_on_view() {
    // View and subview have custom measures, but not project_view
    insertCustomMeasure(SUBVIEW_UUID, FLOAT_METRIC, 3.14);
    insertCustomMeasure(VIEW_UUID, STRING_METRIC, "good");

    treeRootHolder.setRoot(
      ViewsComponent.builder(VIEW, VIEW_REF).setUuid("VIEW")
        .addChildren(
          ViewsComponent.builder(SUBVIEW, SUBVIEW_REF).setUuid("SUBVIEW").build(),
          ViewsComponent.builder(PROJECT_VIEW, PROJECT_VIEW_REF).setUuid("PROJECT_VIEW").build()
        )
        .build());

    underTest.execute();

    assertNoRawMeasureValue(PROJECT_VIEW_REF);
    assertRawMeasureValue(SUBVIEW_REF, FLOAT_METRIC.getKey(), 3.1d);
    assertRawMeasureValue(VIEW_REF, STRING_METRIC.getKey(), "good");
  }

  @Test
  public void test_float_value_type() throws Exception {
    CustomMeasureDto dto = new CustomMeasureDto();
    dto.setValue(10.0);
    assertThat(dtoToMeasure(dto, new MetricImpl(1, "m", "M", Metric.MetricType.FLOAT)).getDoubleValue()).isEqualTo(10.0);
  }

  @Test
  public void test_int_value_type() throws Exception {
    CustomMeasureDto dto = new CustomMeasureDto();
    dto.setValue(10.0);
    assertThat(dtoToMeasure(dto, new MetricImpl(1, "m", "M", Metric.MetricType.INT)).getIntValue()).isEqualTo(10);
  }

  @Test
  public void test_long_value_type() throws Exception {
    CustomMeasureDto dto = new CustomMeasureDto();
    dto.setValue(10.0);
    assertThat(dtoToMeasure(dto, new MetricImpl(1, "m", "M", Metric.MetricType.WORK_DUR)).getLongValue()).isEqualTo(10);
  }

  @Test
  public void test_percent_value_type() throws Exception {
    CustomMeasureDto dto = new CustomMeasureDto();
    dto.setValue(10.0);
    assertThat(dtoToMeasure(dto, new MetricImpl(1, "m", "M", Metric.MetricType.PERCENT)).getDoubleValue()).isEqualTo(10);
  }

  @Test
  public void test_string_value_type() throws Exception {
    CustomMeasureDto dto = new CustomMeasureDto();
    dto.setTextValue("foo");
    assertThat(dtoToMeasure(dto, new MetricImpl(1, "m", "M", Metric.MetricType.STRING)).getStringValue()).isEqualTo("foo");
  }

  @Test
  public void test_LEVEL_value_type() throws Exception {
    CustomMeasureDto dto = new CustomMeasureDto();
    dto.setTextValue("OK");
    assertThat(dtoToMeasure(dto, new MetricImpl(1, "m", "M", Metric.MetricType.LEVEL)).getLevelValue()).isEqualTo(Measure.Level.OK);
  }

  @Test
  public void test_boolean_value_type() throws Exception {
    MetricImpl booleanMetric = new MetricImpl(1, "m", "M", Metric.MetricType.BOOL);
    CustomMeasureDto dto = new CustomMeasureDto();
    assertThat(dtoToMeasure(dto.setValue(1.0), booleanMetric).getBooleanValue()).isTrue();
    assertThat(dtoToMeasure(dto.setValue(0.0), booleanMetric).getBooleanValue()).isFalse();
  }

  private void assertNoRawMeasureValue(int componentRef) {
    assertThat(measureRepository.getAddedRawMeasures(componentRef)).isEmpty();
  }

  private void assertRawMeasureValue(int componentRef, String metricKey, double value) {
    assertThat(toEntries(measureRepository.getAddedRawMeasures(componentRef))).containsOnly(entryOf(metricKey, newMeasureBuilder().create(value)));
  }

  private void assertRawMeasureValue(int componentRef, String metricKey, String value) {
    assertThat(toEntries(measureRepository.getAddedRawMeasures(componentRef))).containsOnly(entryOf(metricKey, newMeasureBuilder().create(value)));
  }

  private void insertCustomMeasure(String componentUuid, Metric metric, double value) {
    dbTester.getDbClient().customMeasureDao().insert(dbTester.getSession(), CustomMeasureTesting.newCustomMeasureDto()
      .setComponentUuid(componentUuid)
      .setMetricId(metric.getId())
      .setValue(value));
    dbTester.getSession().commit();
  }

  private void insertCustomMeasure(String componentUuid, Metric metric, String value) {
    dbTester.getDbClient().customMeasureDao().insert(dbTester.getSession(), CustomMeasureTesting.newCustomMeasureDto()
      .setComponentUuid(componentUuid)
      .setMetricId(metric.getId())
      .setTextValue(value));
    dbTester.getSession().commit();
  }

}
