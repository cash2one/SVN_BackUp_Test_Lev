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
import org.sonar.api.measures.CoreMetrics;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.FileAttributes;
import org.sonar.server.computation.formula.coverage.LinesAndConditionsWithUncoveredMetricKeys;
import org.sonar.server.computation.measure.MeasureRepoEntry;
import org.sonar.server.computation.measure.MeasureRepositoryRule;
import org.sonar.server.computation.metric.MetricRepositoryRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.computation.component.Component.Type.DIRECTORY;
import static org.sonar.server.computation.component.Component.Type.FILE;
import static org.sonar.server.computation.component.Component.Type.MODULE;
import static org.sonar.server.computation.component.Component.Type.PROJECT;
import static org.sonar.server.computation.component.ReportComponent.builder;
import static org.sonar.server.computation.measure.Measure.newMeasureBuilder;
import static org.sonar.server.computation.measure.MeasureRepoEntry.entryOf;
import static org.sonar.server.computation.measure.MeasureRepoEntry.toEntries;

public class ReportCoverageMeasuresStepTest {
  private static final int ROOT_REF = 1;
  private static final int MODULE_REF = 12;
  private static final int SUB_MODULE_REF = 123;
  private static final int DIRECTORY_REF = 1234;
  private static final int FILE_1_REF = 12341;
  private static final int UNIT_TEST_FILE_REF = 12342;
  private static final int FILE_2_REF = 12343;

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(CoreMetrics.LINES_TO_COVER)
    .add(CoreMetrics.CONDITIONS_TO_COVER)
    .add(CoreMetrics.UNCOVERED_LINES)
    .add(CoreMetrics.UNCOVERED_CONDITIONS)
    .add(CoreMetrics.COVERAGE)
    .add(CoreMetrics.BRANCH_COVERAGE)
    .add(CoreMetrics.LINE_COVERAGE)

  .add(CoreMetrics.IT_LINES_TO_COVER)
    .add(CoreMetrics.IT_CONDITIONS_TO_COVER)
    .add(CoreMetrics.IT_UNCOVERED_LINES)
    .add(CoreMetrics.IT_UNCOVERED_CONDITIONS)
    .add(CoreMetrics.IT_COVERAGE)
    .add(CoreMetrics.IT_BRANCH_COVERAGE)
    .add(CoreMetrics.IT_LINE_COVERAGE)

  .add(CoreMetrics.OVERALL_LINES_TO_COVER)
    .add(CoreMetrics.OVERALL_CONDITIONS_TO_COVER)
    .add(CoreMetrics.OVERALL_UNCOVERED_LINES)
    .add(CoreMetrics.OVERALL_UNCOVERED_CONDITIONS)
    .add(CoreMetrics.OVERALL_COVERAGE)
    .add(CoreMetrics.OVERALL_BRANCH_COVERAGE)
    .add(CoreMetrics.OVERALL_LINE_COVERAGE);
  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  CoverageMeasuresStep underTest = new CoverageMeasuresStep(treeRootHolder, metricRepository, measureRepository);

  @Before
  public void setUp() throws Exception {
    treeRootHolder.setRoot(
      builder(PROJECT, ROOT_REF)
        .addChildren(
          builder(MODULE, MODULE_REF)
            .addChildren(
              builder(MODULE, SUB_MODULE_REF)
                .addChildren(
                  builder(DIRECTORY, DIRECTORY_REF)
                    .addChildren(
                      builder(FILE, FILE_1_REF).build(),
                      builder(FILE, UNIT_TEST_FILE_REF).setFileAttributes(new FileAttributes(true, "some language")).build(),
                      builder(FILE, FILE_2_REF).build())
                    .build())
                .build())
            .build())
        .build());
  }

  @Test
  public void verify_aggregates_values_for_ut_lines_and_conditions() {
    LinesAndConditionsWithUncoveredMetricKeys metricKeys = new LinesAndConditionsWithUncoveredMetricKeys(
      CoreMetrics.LINES_TO_COVER_KEY, CoreMetrics.CONDITIONS_TO_COVER_KEY,
      CoreMetrics.UNCOVERED_LINES_KEY, CoreMetrics.UNCOVERED_CONDITIONS_KEY);
    verify_lines_and_conditions_aggregates_values(metricKeys);
  }

  @Test
  public void verify_aggregates_values_for_it_lines_and_conditions() {
    LinesAndConditionsWithUncoveredMetricKeys metricKeys = new LinesAndConditionsWithUncoveredMetricKeys(
      CoreMetrics.IT_LINES_TO_COVER_KEY, CoreMetrics.IT_CONDITIONS_TO_COVER_KEY,
      CoreMetrics.IT_UNCOVERED_LINES_KEY, CoreMetrics.IT_UNCOVERED_CONDITIONS_KEY);
    verify_lines_and_conditions_aggregates_values(metricKeys);
  }

  @Test
  public void verify_aggregates_values_for_overall_lines_and_conditions() {
    LinesAndConditionsWithUncoveredMetricKeys metricKeys = new LinesAndConditionsWithUncoveredMetricKeys(
      CoreMetrics.OVERALL_LINES_TO_COVER_KEY, CoreMetrics.OVERALL_CONDITIONS_TO_COVER_KEY,
      CoreMetrics.OVERALL_UNCOVERED_LINES_KEY, CoreMetrics.OVERALL_UNCOVERED_CONDITIONS_KEY);
    verify_lines_and_conditions_aggregates_values(metricKeys);
  }

  private void verify_lines_and_conditions_aggregates_values(LinesAndConditionsWithUncoveredMetricKeys metricKeys) {
    measureRepository
      .addRawMeasure(FILE_1_REF, metricKeys.getLines(), newMeasureBuilder().create(3000))
      .addRawMeasure(FILE_1_REF, metricKeys.getConditions(), newMeasureBuilder().create(300))
      .addRawMeasure(FILE_1_REF, metricKeys.getUncoveredLines(), newMeasureBuilder().create(30))
      .addRawMeasure(FILE_1_REF, metricKeys.getUncoveredConditions(), newMeasureBuilder().create(9))

    .addRawMeasure(FILE_2_REF, metricKeys.getLines(), newMeasureBuilder().create(2000))
      .addRawMeasure(FILE_2_REF, metricKeys.getConditions(), newMeasureBuilder().create(400))
      .addRawMeasure(FILE_2_REF, metricKeys.getUncoveredLines(), newMeasureBuilder().create(200))
      .addRawMeasure(FILE_2_REF, metricKeys.getUncoveredConditions(), newMeasureBuilder().create(16));

    underTest.execute();

    MeasureRepoEntry[] nonFileRepoEntries = {
      entryOf(metricKeys.getLines(), newMeasureBuilder().create(5000)),
      entryOf(metricKeys.getConditions(), newMeasureBuilder().create(700)),
      entryOf(metricKeys.getUncoveredLines(), newMeasureBuilder().create(230)),
      entryOf(metricKeys.getUncoveredConditions(), newMeasureBuilder().create(25))
    };

    assertThat(toEntries(measureRepository.getAddedRawMeasures(DIRECTORY_REF))).contains(nonFileRepoEntries);
    assertThat(toEntries(measureRepository.getAddedRawMeasures(SUB_MODULE_REF))).contains(nonFileRepoEntries);
    assertThat(toEntries(measureRepository.getAddedRawMeasures(MODULE_REF))).contains(nonFileRepoEntries);
    assertThat(toEntries(measureRepository.getAddedRawMeasures(ROOT_REF))).contains(nonFileRepoEntries);
  }

  @Test
  public void verify_aggregates_values_for_code_line_and_branch_coverage() {
    LinesAndConditionsWithUncoveredMetricKeys metricKeys = new LinesAndConditionsWithUncoveredMetricKeys(
      CoreMetrics.LINES_TO_COVER_KEY, CoreMetrics.CONDITIONS_TO_COVER_KEY,
      CoreMetrics.UNCOVERED_LINES_KEY, CoreMetrics.UNCOVERED_CONDITIONS_KEY);
    String codeCoverageKey = CoreMetrics.COVERAGE_KEY;
    String lineCoverageKey = CoreMetrics.LINE_COVERAGE_KEY;
    String branchCoverageKey = CoreMetrics.BRANCH_COVERAGE_KEY;

    verify_coverage_aggregates_values(metricKeys, codeCoverageKey, lineCoverageKey, branchCoverageKey);
  }

  @Test
  public void verify_aggregates_values_for_IT_code_line_and_branch_coverage() {
    LinesAndConditionsWithUncoveredMetricKeys metricKeys = new LinesAndConditionsWithUncoveredMetricKeys(
      CoreMetrics.IT_LINES_TO_COVER_KEY, CoreMetrics.IT_CONDITIONS_TO_COVER_KEY,
      CoreMetrics.IT_UNCOVERED_LINES_KEY, CoreMetrics.IT_UNCOVERED_CONDITIONS_KEY);
    String codeCoverageKey = CoreMetrics.IT_COVERAGE_KEY;
    String lineCoverageKey = CoreMetrics.IT_LINE_COVERAGE_KEY;
    String branchCoverageKey = CoreMetrics.IT_BRANCH_COVERAGE_KEY;

    verify_coverage_aggregates_values(metricKeys, codeCoverageKey, lineCoverageKey, branchCoverageKey);
  }

  @Test
  public void verify_aggregates_values_for_Overall_code_line_and_branch_coverage() {
    LinesAndConditionsWithUncoveredMetricKeys metricKeys = new LinesAndConditionsWithUncoveredMetricKeys(
      CoreMetrics.OVERALL_LINES_TO_COVER_KEY, CoreMetrics.OVERALL_CONDITIONS_TO_COVER_KEY,
      CoreMetrics.OVERALL_UNCOVERED_LINES_KEY, CoreMetrics.OVERALL_UNCOVERED_CONDITIONS_KEY);
    String codeCoverageKey = CoreMetrics.OVERALL_COVERAGE_KEY;
    String lineCoverageKey = CoreMetrics.OVERALL_LINE_COVERAGE_KEY;
    String branchCoverageKey = CoreMetrics.OVERALL_BRANCH_COVERAGE_KEY;

    verify_coverage_aggregates_values(metricKeys, codeCoverageKey, lineCoverageKey, branchCoverageKey);
  }

  private void verify_coverage_aggregates_values(LinesAndConditionsWithUncoveredMetricKeys metricKeys, String codeCoverageKey, String lineCoverageKey, String branchCoverageKey) {
    measureRepository
      .addRawMeasure(FILE_1_REF, metricKeys.getLines(), newMeasureBuilder().create(3000))
      .addRawMeasure(FILE_1_REF, metricKeys.getConditions(), newMeasureBuilder().create(300))
      .addRawMeasure(FILE_1_REF, metricKeys.getUncoveredLines(), newMeasureBuilder().create(30))
      .addRawMeasure(FILE_1_REF, metricKeys.getUncoveredConditions(), newMeasureBuilder().create(9))

    .addRawMeasure(FILE_2_REF, metricKeys.getLines(), newMeasureBuilder().create(2000))
      .addRawMeasure(FILE_2_REF, metricKeys.getConditions(), newMeasureBuilder().create(400))
      .addRawMeasure(FILE_2_REF, metricKeys.getUncoveredLines(), newMeasureBuilder().create(200))
      .addRawMeasure(FILE_2_REF, metricKeys.getUncoveredConditions(), newMeasureBuilder().create(16));

    underTest.execute();

    assertThat(toEntries(measureRepository.getAddedRawMeasures(FILE_1_REF))).contains(
      entryOf(codeCoverageKey, newMeasureBuilder().create(98.8d)),
      entryOf(lineCoverageKey, newMeasureBuilder().create(99d)),
      entryOf(branchCoverageKey, newMeasureBuilder().create(97d)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(FILE_2_REF))).contains(
      entryOf(codeCoverageKey, newMeasureBuilder().create(91d)),
      entryOf(lineCoverageKey, newMeasureBuilder().create(90d)),
      entryOf(branchCoverageKey, newMeasureBuilder().create(96d)));
    assertThat(toEntries(measureRepository.getAddedRawMeasures(UNIT_TEST_FILE_REF))).isEmpty();

    MeasureRepoEntry[] nonFileRepoEntries = {
      entryOf(codeCoverageKey, newMeasureBuilder().create(95.5d)),
      entryOf(lineCoverageKey, newMeasureBuilder().create(95.4d)),
      entryOf(branchCoverageKey, newMeasureBuilder().create(96.4d))
    };

    assertThat(toEntries(measureRepository.getAddedRawMeasures(DIRECTORY_REF))).contains(nonFileRepoEntries);
    assertThat(toEntries(measureRepository.getAddedRawMeasures(SUB_MODULE_REF))).contains(nonFileRepoEntries);
    assertThat(toEntries(measureRepository.getAddedRawMeasures(MODULE_REF))).contains(nonFileRepoEntries);
    assertThat(toEntries(measureRepository.getAddedRawMeasures(ROOT_REF))).contains(nonFileRepoEntries);
  }

}
