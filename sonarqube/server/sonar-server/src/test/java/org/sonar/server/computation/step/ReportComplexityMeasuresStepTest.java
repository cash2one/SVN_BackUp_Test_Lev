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

import org.junit.Rule;
import org.junit.Test;
import org.sonar.server.computation.batch.BatchReportReaderRule;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.measure.MeasureRepositoryRule;
import org.sonar.server.computation.metric.MetricRepositoryRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.sonar.api.measures.CoreMetrics.CLASSES;
import static org.sonar.api.measures.CoreMetrics.CLASSES_KEY;
import static org.sonar.api.measures.CoreMetrics.CLASS_COMPLEXITY;
import static org.sonar.api.measures.CoreMetrics.CLASS_COMPLEXITY_DISTRIBUTION;
import static org.sonar.api.measures.CoreMetrics.CLASS_COMPLEXITY_DISTRIBUTION_KEY;
import static org.sonar.api.measures.CoreMetrics.CLASS_COMPLEXITY_KEY;
import static org.sonar.api.measures.CoreMetrics.COMPLEXITY;
import static org.sonar.api.measures.CoreMetrics.COMPLEXITY_IN_CLASSES;
import static org.sonar.api.measures.CoreMetrics.COMPLEXITY_IN_CLASSES_KEY;
import static org.sonar.api.measures.CoreMetrics.COMPLEXITY_IN_FUNCTIONS;
import static org.sonar.api.measures.CoreMetrics.COMPLEXITY_IN_FUNCTIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.COMPLEXITY_KEY;
import static org.sonar.api.measures.CoreMetrics.FILES;
import static org.sonar.api.measures.CoreMetrics.FILES_KEY;
import static org.sonar.api.measures.CoreMetrics.FILE_COMPLEXITY;
import static org.sonar.api.measures.CoreMetrics.FILE_COMPLEXITY_DISTRIBUTION;
import static org.sonar.api.measures.CoreMetrics.FILE_COMPLEXITY_DISTRIBUTION_KEY;
import static org.sonar.api.measures.CoreMetrics.FILE_COMPLEXITY_KEY;
import static org.sonar.api.measures.CoreMetrics.FUNCTIONS;
import static org.sonar.api.measures.CoreMetrics.FUNCTIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.FUNCTION_COMPLEXITY;
import static org.sonar.api.measures.CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION;
import static org.sonar.api.measures.CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION_KEY;
import static org.sonar.api.measures.CoreMetrics.FUNCTION_COMPLEXITY_KEY;
import static org.sonar.server.computation.component.Component.Type.DIRECTORY;
import static org.sonar.server.computation.component.Component.Type.FILE;
import static org.sonar.server.computation.component.Component.Type.MODULE;
import static org.sonar.server.computation.component.Component.Type.PROJECT;
import static org.sonar.server.computation.component.ReportComponent.builder;
import static org.sonar.server.computation.measure.Measure.newMeasureBuilder;
import static org.sonar.server.computation.measure.MeasureRepoEntry.entryOf;
import static org.sonar.server.computation.measure.MeasureRepoEntry.toEntries;

public class ReportComplexityMeasuresStepTest {

  private static final int ROOT_REF = 1;
  private static final int MODULE_REF = 11;
  private static final int SUB_MODULE_REF = 111;
  private static final int DIRECTORY_REF = 1111;
  private static final int FILE_1_REF = 11111;
  private static final int FILE_2_REF = 11121;

  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();
  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule()
    .setRoot(builder(PROJECT, ROOT_REF)
      .addChildren(
        builder(MODULE, MODULE_REF)
          .addChildren(
            builder(MODULE, SUB_MODULE_REF)
              .addChildren(
                builder(DIRECTORY, DIRECTORY_REF)
                  .addChildren(
                    builder(FILE, FILE_1_REF).build(),
                    builder(FILE, FILE_2_REF).build())
                  .build())
              .build())
          .build())
      .build());
  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(COMPLEXITY)
    .add(COMPLEXITY_IN_CLASSES)
    .add(COMPLEXITY_IN_FUNCTIONS)
    .add(FUNCTION_COMPLEXITY_DISTRIBUTION)
    .add(FILE_COMPLEXITY_DISTRIBUTION)
    .add(CLASS_COMPLEXITY_DISTRIBUTION)
    .add(FILE_COMPLEXITY)
    .add(FILES)
    .add(CLASS_COMPLEXITY)
    .add(CLASSES)
    .add(FUNCTION_COMPLEXITY)
    .add(FUNCTIONS);
  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  private ComputationStep underTest = new ComplexityMeasuresStep(treeRootHolder, metricRepository, measureRepository);

  @Test
  public void aggregate_complexity() throws Exception {
    verify_sum_aggregation(COMPLEXITY_KEY);
  }

  @Test
  public void aggregate_complexity_in_classes() throws Exception {
    verify_sum_aggregation(COMPLEXITY_IN_CLASSES_KEY);
  }

  @Test
  public void aggregate_complexity_in_functions() throws Exception {
    verify_sum_aggregation(COMPLEXITY_IN_FUNCTIONS_KEY);
  }

  private void verify_sum_aggregation(String metricKey) {
    measureRepository.addRawMeasure(FILE_1_REF, metricKey, newMeasureBuilder().create(10));
    measureRepository.addRawMeasure(FILE_2_REF, metricKey, newMeasureBuilder().create(40));

    underTest.execute();

    assertThat(measureRepository.getAddedRawMeasure(FILE_1_REF, metricKey)).isAbsent();
    assertThat(measureRepository.getAddedRawMeasure(FILE_2_REF, metricKey)).isAbsent();

    int expectedNonFileValue = 50;
    assertThat(toEntries(measureRepository.getAddedRawMeasures(DIRECTORY_REF))).contains(entryOf(metricKey, newMeasureBuilder().create(expectedNonFileValue)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(SUB_MODULE_REF))).contains(entryOf(metricKey, newMeasureBuilder().create(expectedNonFileValue)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(MODULE_REF))).contains(entryOf(metricKey, newMeasureBuilder().create(expectedNonFileValue)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(ROOT_REF))).contains(entryOf(metricKey, newMeasureBuilder().create(expectedNonFileValue)));
  }

  @Test
  public void aggregate_function_complexity_distribution() throws Exception {
    verify_distribution_aggregation(FUNCTION_COMPLEXITY_DISTRIBUTION_KEY);
  }

  @Test
  public void aggregate_file_complexity_distribution() throws Exception {
    verify_distribution_aggregation(FILE_COMPLEXITY_DISTRIBUTION_KEY);
  }

  @Test
  public void aggregate_class_complexity_distribution() throws Exception {
    verify_distribution_aggregation(CLASS_COMPLEXITY_DISTRIBUTION_KEY);
  }

  private void verify_distribution_aggregation(String metricKey) {
    measureRepository.addRawMeasure(FILE_1_REF, metricKey, newMeasureBuilder().create("0.5=3;3.5=5;6.5=9"));
    measureRepository.addRawMeasure(FILE_2_REF, metricKey, newMeasureBuilder().create("0.5=0;3.5=2;6.5=1"));

    underTest.execute();

    assertThat(measureRepository.getAddedRawMeasure(FILE_1_REF, metricKey)).isAbsent();
    assertThat(measureRepository.getAddedRawMeasure(FILE_2_REF, metricKey)).isAbsent();

    String expectedNonFileValue = "0.5=3;3.5=7;6.5=10";
    assertThat(toEntries(measureRepository.getAddedRawMeasures(DIRECTORY_REF))).contains(entryOf(metricKey, newMeasureBuilder().create(expectedNonFileValue)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(SUB_MODULE_REF))).contains(entryOf(metricKey, newMeasureBuilder().create(expectedNonFileValue)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(MODULE_REF))).contains(entryOf(metricKey, newMeasureBuilder().create(expectedNonFileValue)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(ROOT_REF))).contains(entryOf(metricKey, newMeasureBuilder().create(expectedNonFileValue)));
  }

  @Test
  public void compute_and_aggregate_file_complexity() throws Exception {
    verify_average_compute_and_aggregation(FILE_COMPLEXITY_KEY, COMPLEXITY_KEY, FILES_KEY);
  }

  @Test
  public void compute_and_aggregate_class_complexity() throws Exception {
    verify_average_compute_and_aggregation(CLASS_COMPLEXITY_KEY, COMPLEXITY_IN_CLASSES_KEY, CLASSES_KEY);
  }

  @Test
  public void compute_and_aggregate_class_complexity_with_fallback_metric() throws Exception {
    verify_average_compute_and_aggregation(CLASS_COMPLEXITY_KEY, COMPLEXITY_KEY, CLASSES_KEY);
  }

  @Test
  public void compute_and_aggregate_function_complexity() throws Exception {
    verify_average_compute_and_aggregation(FUNCTION_COMPLEXITY_KEY, COMPLEXITY_IN_FUNCTIONS_KEY, FUNCTIONS_KEY);
  }

  @Test
  public void compute_and_aggregate_function_complexity_with_fallback_metric() throws Exception {
    verify_average_compute_and_aggregation(FUNCTION_COMPLEXITY_KEY, COMPLEXITY_KEY, FUNCTIONS_KEY);
  }

  private void verify_average_compute_and_aggregation(String metricKey, String mainMetric, String byMetric) {
    measureRepository.addRawMeasure(FILE_1_REF, mainMetric, newMeasureBuilder().create(5));
    measureRepository.addRawMeasure(FILE_1_REF, byMetric, newMeasureBuilder().create(2));

    measureRepository.addRawMeasure(FILE_2_REF, mainMetric, newMeasureBuilder().create(1));
    measureRepository.addRawMeasure(FILE_2_REF, byMetric, newMeasureBuilder().create(1));

    underTest.execute();

    assertThat(toEntries(measureRepository.getAddedRawMeasures(FILE_1_REF))).contains(entryOf(metricKey, newMeasureBuilder().create(2.5)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(FILE_2_REF))).contains(entryOf(metricKey, newMeasureBuilder().create(1d)));

    double expectedNonFileValue = 2d;
    assertThat(toEntries(measureRepository.getAddedRawMeasures(DIRECTORY_REF))).contains(entryOf(metricKey, newMeasureBuilder().create(expectedNonFileValue)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(SUB_MODULE_REF))).contains(entryOf(metricKey, newMeasureBuilder().create(expectedNonFileValue)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(MODULE_REF))).contains(entryOf(metricKey, newMeasureBuilder().create(expectedNonFileValue)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(ROOT_REF))).contains(entryOf(metricKey, newMeasureBuilder().create(expectedNonFileValue)));
  }

}
