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

import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.server.computation.batch.BatchReportReaderRule;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.ReportComponent;
import org.sonar.server.computation.component.FileAttributes;
import org.sonar.server.computation.formula.coverage.LinesAndConditionsWithUncoveredMetricKeys;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureRepoEntry;
import org.sonar.server.computation.measure.MeasureRepositoryRule;
import org.sonar.server.computation.measure.MeasureVariations;
import org.sonar.server.computation.metric.MetricRepositoryRule;
import org.sonar.server.computation.period.Period;
import org.sonar.server.computation.period.PeriodsHolderRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.sonar.api.measures.CoreMetrics.CONDITIONS_BY_LINE_KEY;
import static org.sonar.api.measures.CoreMetrics.COVERAGE_LINE_HITS_DATA_KEY;
import static org.sonar.api.measures.CoreMetrics.COVERED_CONDITIONS_BY_LINE_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_CONDITIONS_TO_COVER_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_LINES_TO_COVER_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_UNCOVERED_CONDITIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_UNCOVERED_LINES_KEY;
import static org.sonar.api.utils.DateUtils.parseDate;
import static org.sonar.batch.protocol.output.BatchReport.Changesets;
import static org.sonar.server.computation.component.Component.Type.DIRECTORY;
import static org.sonar.server.computation.component.Component.Type.FILE;
import static org.sonar.server.computation.component.Component.Type.MODULE;
import static org.sonar.server.computation.component.Component.Type.PROJECT;
import static org.sonar.server.computation.component.ReportComponent.builder;
import static org.sonar.server.computation.measure.Measure.newMeasureBuilder;
import static org.sonar.server.computation.measure.MeasureRepoEntry.entryOf;
import static org.sonar.server.computation.measure.MeasureRepoEntry.toEntries;
import static org.sonar.server.computation.measure.MeasureVariations.newMeasureVariationsBuilder;

public class ReportNewCoverageMeasuresStepTest {

  private static final int ROOT_REF = 1;
  private static final int MODULE_REF = 11;
  private static final int SUB_MODULE_REF = 111;
  private static final int DIRECTORY_1_REF = 1111;
  private static final int FILE_1_REF = 11111;
  private static final int DIRECTORY_2_REF = 1112;
  private static final int FILE_2_REF = 11121;
  private static final int FILE_3_REF = 11122;

  private static final ReportComponent MULTIPLE_FILES_TREE = builder(PROJECT, ROOT_REF)
    .addChildren(
      builder(MODULE, MODULE_REF)
        .addChildren(
          builder(MODULE, SUB_MODULE_REF)
            .addChildren(
              builder(DIRECTORY, DIRECTORY_1_REF)
                .addChildren(
                  builder(FILE, FILE_1_REF).build()
                ).build(),
              builder(DIRECTORY, DIRECTORY_2_REF)
                .addChildren(
                  builder(FILE, FILE_2_REF).build(),
                  builder(FILE, FILE_3_REF).build()
                ).build()
            )
            .build()
        ).build()
    ).build();

  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();
  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public PeriodsHolderRule periodsHolder = new PeriodsHolderRule();
  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(CoreMetrics.COVERAGE_LINE_HITS_DATA)
    .add(CoreMetrics.CONDITIONS_BY_LINE)
    .add(CoreMetrics.COVERED_CONDITIONS_BY_LINE)
    .add(CoreMetrics.NEW_LINES_TO_COVER)
    .add(CoreMetrics.NEW_UNCOVERED_LINES)
    .add(CoreMetrics.NEW_CONDITIONS_TO_COVER)
    .add(CoreMetrics.NEW_UNCOVERED_CONDITIONS)
    .add(CoreMetrics.NEW_COVERAGE)
    .add(CoreMetrics.NEW_BRANCH_COVERAGE)
    .add(CoreMetrics.NEW_LINE_COVERAGE)

    .add(CoreMetrics.IT_COVERAGE_LINE_HITS_DATA)
    .add(CoreMetrics.IT_CONDITIONS_BY_LINE)
    .add(CoreMetrics.IT_COVERED_CONDITIONS_BY_LINE)
    .add(CoreMetrics.NEW_IT_LINES_TO_COVER)
    .add(CoreMetrics.NEW_IT_UNCOVERED_LINES)
    .add(CoreMetrics.NEW_IT_CONDITIONS_TO_COVER)
    .add(CoreMetrics.NEW_IT_UNCOVERED_CONDITIONS)
    .add(CoreMetrics.NEW_IT_COVERAGE)
    .add(CoreMetrics.NEW_IT_BRANCH_COVERAGE)
    .add(CoreMetrics.NEW_IT_LINE_COVERAGE)

    .add(CoreMetrics.OVERALL_COVERAGE_LINE_HITS_DATA)
    .add(CoreMetrics.OVERALL_CONDITIONS_BY_LINE)
    .add(CoreMetrics.OVERALL_COVERED_CONDITIONS_BY_LINE)
    .add(CoreMetrics.NEW_OVERALL_LINES_TO_COVER)
    .add(CoreMetrics.NEW_OVERALL_UNCOVERED_LINES)
    .add(CoreMetrics.NEW_OVERALL_CONDITIONS_TO_COVER)
    .add(CoreMetrics.NEW_OVERALL_UNCOVERED_CONDITIONS)
    .add(CoreMetrics.NEW_OVERALL_COVERAGE)
    .add(CoreMetrics.NEW_OVERALL_BRANCH_COVERAGE)
    .add(CoreMetrics.NEW_OVERALL_LINE_COVERAGE)
    ;
  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  private NewCoverageMeasuresStep underTest = new NewCoverageMeasuresStep(treeRootHolder, periodsHolder, reportReader,
    measureRepository, metricRepository);
  public static final ReportComponent FILE_COMPONENT = ReportComponent.builder(Component.Type.FILE, FILE_1_REF)
    .setFileAttributes(new FileAttributes(false, null)).build();

  @Before
  public void setUp() {
    periodsHolder.setPeriods(
      new Period(2, "mode_p_1", null, parseDate("2009-12-25").getTime(), 1),
      new Period(5, "mode_p_5", null, parseDate("2011-02-18").getTime(), 2));
  }

  @Test
  public void no_measure_for_PROJECT_component() {
    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, ROOT_REF).build());

    underTest.execute();

    assertThat(measureRepository.isEmpty()).isTrue();
  }

  @Test
  public void no_measure_for_MODULE_component() {
    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.MODULE, MODULE_REF).build());

    underTest.execute();

    assertThat(measureRepository.isEmpty()).isTrue();
  }

  @Test
  public void no_measure_for_DIRECTORY_component() {
    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.DIRECTORY, DIRECTORY_1_REF).build());

    underTest.execute();

    assertThat(measureRepository.isEmpty()).isTrue();
  }

  @Test
  public void no_measure_for_unit_test_FILE_component() {
    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.FILE, FILE_1_REF).setFileAttributes(new FileAttributes(true, null)).build());

    underTest.execute();

    assertThat(measureRepository.isEmpty()).isTrue();
  }

  @Test
  public void no_measures_for_FILE_component_without_code() {
    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.FILE, FILE_1_REF).setFileAttributes(new FileAttributes(false, null)).build());

    underTest.execute();

    assertThat(measureRepository.isEmpty()).isTrue();
  }

  @Test
  public void verify_measure_of_condition_not_computed_if_there_is_none() {
    treeRootHolder.setRoot(FILE_COMPONENT);
    reportReader.putChangesets(Changesets.newBuilder()
        .setComponentRef(FILE_COMPONENT.getReportAttributes().getRef())
        .addChangeset(Changesets.Changeset.newBuilder().build())
        .addChangeset(Changesets.Changeset.newBuilder()
          .setDate(parseDate("2007-01-15").getTime())
          .build())
        .addChangeset(Changesets.Changeset.newBuilder()
          .setDate(parseDate("2011-01-01").getTime())
          .build())
        .addChangesetIndexByLine(0)
        .addChangesetIndexByLine(1)
        .addChangesetIndexByLine(2)
        .build()
    );

    underTest.execute();

    assertThat(measureRepository.isEmpty()).isTrue();
  }

  @Test
  public void verify_no_measure_when_nothing_has_changed() {
    treeRootHolder.setRoot(FILE_COMPONENT);
    reportReader.putChangesets(BatchReport.Changesets.newBuilder()
      .setComponentRef(FILE_COMPONENT.getReportAttributes().getRef())
      .addChangeset(Changesets.Changeset.newBuilder()
        .setDate(parseDate("2008-08-02").getTime())
        .build())
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .build());
    measureRepository.addRawMeasure(FILE_COMPONENT.getReportAttributes().getRef(), COVERAGE_LINE_HITS_DATA_KEY, newMeasureBuilder().create("2=1;3=1"));
    measureRepository.addRawMeasure(FILE_COMPONENT.getReportAttributes().getRef(), CONDITIONS_BY_LINE_KEY, newMeasureBuilder().create("2=1"));
    measureRepository.addRawMeasure(FILE_COMPONENT.getReportAttributes().getRef(), COVERED_CONDITIONS_BY_LINE_KEY, newMeasureBuilder().create("2=1"));

    underTest.execute();

    assertThat(measureRepository.getAddedRawMeasures(FILE_COMPONENT.getReportAttributes().getRef())).isEmpty();
  }

  @Test
  public void no_measures_for_FILE_component_without_CoverageData() {
    ReportComponent fileComponent = ReportComponent.builder(Component.Type.FILE, FILE_1_REF).setFileAttributes(new FileAttributes(false, null)).build();

    treeRootHolder.setRoot(fileComponent);
    reportReader.putChangesets(Changesets.newBuilder()
      .setComponentRef(fileComponent.getReportAttributes().getRef())
      .addChangeset(Changesets.Changeset.newBuilder()
        .setDate(parseDate("2008-05-18").getTime())
        .build())
      .addChangesetIndexByLine(0)
      .build());

    underTest.execute();

    assertThat(measureRepository.isEmpty()).isTrue();
  }

  @Test
  public void verify_computation_of_measures_for_new_lines_for_FILE() {
    String coverageLineHitsData = COVERAGE_LINE_HITS_DATA_KEY;
    String newLinesToCover = NEW_LINES_TO_COVER_KEY;
    String newUncoveredLines = NEW_UNCOVERED_LINES_KEY;
    String newConditionsToCover = NEW_CONDITIONS_TO_COVER_KEY;
    String newUncoveredConditions = NEW_UNCOVERED_CONDITIONS_KEY;

    verify_computation_of_measures_for_new_lines(coverageLineHitsData,
      newLinesToCover, newUncoveredLines, newConditionsToCover, newUncoveredConditions);
  }

  @Test
  public void verify_computation_of_measures_for_new_lines_for_IT_FILE() {
    String coverageLineHitsData = CoreMetrics.IT_COVERAGE_LINE_HITS_DATA_KEY;
    String newLinesToCover = CoreMetrics.NEW_IT_LINES_TO_COVER_KEY;
    String newUncoveredLines = CoreMetrics.NEW_IT_UNCOVERED_LINES_KEY;
    String newConditionsToCover = CoreMetrics.NEW_IT_CONDITIONS_TO_COVER_KEY;
    String newUncoveredConditions = CoreMetrics.NEW_IT_UNCOVERED_CONDITIONS_KEY;

    verify_computation_of_measures_for_new_lines(coverageLineHitsData,
      newLinesToCover, newUncoveredLines, newConditionsToCover, newUncoveredConditions);
  }

  @Test
  public void verify_computation_of_measures_for_new_lines_for_Overall() {
    String coverageLineHitsData = CoreMetrics.OVERALL_COVERAGE_LINE_HITS_DATA_KEY;
    String newLinesToCover = CoreMetrics.NEW_OVERALL_LINES_TO_COVER_KEY;
    String newUncoveredLines = CoreMetrics.NEW_OVERALL_UNCOVERED_LINES_KEY;
    String newConditionsToCover = CoreMetrics.NEW_OVERALL_CONDITIONS_TO_COVER_KEY;
    String newUncoveredConditions = CoreMetrics.NEW_OVERALL_UNCOVERED_CONDITIONS_KEY;

    verify_computation_of_measures_for_new_lines(coverageLineHitsData,
      newLinesToCover, newUncoveredLines, newConditionsToCover, newUncoveredConditions);
  }

  private void verify_computation_of_measures_for_new_lines(String coverageLineHitsData,
                                                            String newLinesToCover, String newUncoveredLines, String newConditionsToCover, String newUncoveredConditions) {
    treeRootHolder.setRoot(FILE_COMPONENT);
    reportReader.putChangesets(Changesets.newBuilder()
      .setComponentRef(FILE_COMPONENT.getReportAttributes().getRef())
      .addChangeset(Changesets.Changeset.newBuilder().build())
      .addChangeset(Changesets.Changeset.newBuilder()
        .setDate(parseDate("2007-01-15").getTime())
        .build())
      .addChangeset(Changesets.Changeset.newBuilder()
        .setDate(parseDate("2011-01-01").getTime())
        .build())
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(2) // line 2
      .addChangesetIndexByLine(1) // line 3
      .addChangesetIndexByLine(2) // line 4
      .build());
    measureRepository.addRawMeasure(FILE_COMPONENT.getReportAttributes().getRef(), coverageLineHitsData, newMeasureBuilder().create("2=0;3=2;4=3"));

    underTest.execute();

    assertThat(toEntries(measureRepository.getAddedRawMeasures(FILE_COMPONENT.getReportAttributes().getRef()))).contains(
      entryOf(newLinesToCover, createMeasure(2d, null)),
      entryOf(newUncoveredLines, createMeasure(1d, null)),
      entryOf(newConditionsToCover, createMeasure(0d, null)),
      entryOf(newUncoveredConditions, createMeasure(0d, null))
    );
  }

  @Test
  public void verify_computation_of_measures_for_new_conditions_for_FILE() {
    String coverageLineHitsData = COVERAGE_LINE_HITS_DATA_KEY;
    String conditionsByLine = CONDITIONS_BY_LINE_KEY;
    String coveredConditionsByLine = COVERED_CONDITIONS_BY_LINE_KEY;
    String newLinesToCover = NEW_LINES_TO_COVER_KEY;
    String newUncoveredLines = NEW_UNCOVERED_LINES_KEY;
    String newConditionsToCover = NEW_CONDITIONS_TO_COVER_KEY;
    String newUncoveredConditions = NEW_UNCOVERED_CONDITIONS_KEY;

    verify_computation_of_measures_for_new_conditions(new MetricKeys(coverageLineHitsData, conditionsByLine, coveredConditionsByLine,
      newLinesToCover, newUncoveredLines, newConditionsToCover, newUncoveredConditions));
  }

  @Test
  public void verify_computation_of_measures_for_new_conditions_for_IT_FILE() {
    String coverageLineHitsData = CoreMetrics.IT_COVERAGE_LINE_HITS_DATA_KEY;
    String conditionsByLine = CoreMetrics.IT_CONDITIONS_BY_LINE_KEY;
    String coveredConditionsByLine = CoreMetrics.IT_COVERED_CONDITIONS_BY_LINE_KEY;
    String newLinesToCover = CoreMetrics.NEW_IT_LINES_TO_COVER_KEY;
    String newUncoveredLines = CoreMetrics.NEW_IT_UNCOVERED_LINES_KEY;
    String newConditionsToCover = CoreMetrics.NEW_IT_CONDITIONS_TO_COVER_KEY;
    String newUncoveredConditions = CoreMetrics.NEW_IT_UNCOVERED_CONDITIONS_KEY;

    verify_computation_of_measures_for_new_conditions(new MetricKeys(coverageLineHitsData, conditionsByLine, coveredConditionsByLine,
      newLinesToCover, newUncoveredLines, newConditionsToCover, newUncoveredConditions));
  }

  @Test
  public void verify_computation_of_measures_for_new_conditions_Overall() {
    String coverageLineHitsData = CoreMetrics.OVERALL_COVERAGE_LINE_HITS_DATA_KEY;
    String conditionsByLine = CoreMetrics.OVERALL_CONDITIONS_BY_LINE_KEY;
    String coveredConditionsByLine = CoreMetrics.OVERALL_COVERED_CONDITIONS_BY_LINE_KEY;
    String newLinesToCover = CoreMetrics.NEW_OVERALL_LINES_TO_COVER_KEY;
    String newUncoveredLines = CoreMetrics.NEW_OVERALL_UNCOVERED_LINES_KEY;
    String newConditionsToCover = CoreMetrics.NEW_OVERALL_CONDITIONS_TO_COVER_KEY;
    String newUncoveredConditions = CoreMetrics.NEW_OVERALL_UNCOVERED_CONDITIONS_KEY;

    verify_computation_of_measures_for_new_conditions(new MetricKeys(coverageLineHitsData, conditionsByLine, coveredConditionsByLine,
      newLinesToCover, newUncoveredLines, newConditionsToCover, newUncoveredConditions));
  }

  @Test
  public void verify_aggregation_of_measures_for_new_conditions() {
    String coverageLineHitsData = CoreMetrics.IT_COVERAGE_LINE_HITS_DATA_KEY;
    String conditionsByLine = CoreMetrics.IT_CONDITIONS_BY_LINE_KEY;
    String coveredConditionsByLine = CoreMetrics.IT_COVERED_CONDITIONS_BY_LINE_KEY;
    String newLinesToCover = CoreMetrics.NEW_IT_LINES_TO_COVER_KEY;
    String newUncoveredLines = CoreMetrics.NEW_IT_UNCOVERED_LINES_KEY;
    String newConditionsToCover = CoreMetrics.NEW_IT_CONDITIONS_TO_COVER_KEY;
    String newUncoveredConditions = CoreMetrics.NEW_IT_UNCOVERED_CONDITIONS_KEY;

    MetricKeys metricKeys = new MetricKeys(coverageLineHitsData, conditionsByLine, coveredConditionsByLine,
      newLinesToCover, newUncoveredLines, newConditionsToCover, newUncoveredConditions);

    treeRootHolder.setRoot(MULTIPLE_FILES_TREE);
    defineChangeSetsAndMeasures(FILE_1_REF, metricKeys, new MeasureValues(3, 4, 1), new MeasureValues(0, 3, 2));
    defineChangeSetsAndMeasures(FILE_2_REF, metricKeys, new MeasureValues(0, 14, 6), new MeasureValues(0, 13, 7));
    defineChangeSetsAndMeasures(FILE_3_REF, metricKeys, new MeasureValues(3, 4, 1), new MeasureValues(1, 13, 7));

    underTest.execute();

    // files
    assertThat(toEntries(measureRepository.getAddedRawMeasures(FILE_1_REF))).contains(
      entryOf(metricKeys.newLinesToCover, createMeasure(5d, 3d)),
      entryOf(metricKeys.newUncoveredLines, createMeasure(3d, 2d)),
      entryOf(metricKeys.newConditionsToCover, createMeasure(7d, 3d)),
      entryOf(metricKeys.newUncoveredConditions, createMeasure(4d, 1d))
    );
    assertThat(toEntries(measureRepository.getAddedRawMeasures(FILE_2_REF))).contains(
      entryOf(metricKeys.newLinesToCover, createMeasure(5d, 3d)),
      entryOf(metricKeys.newUncoveredLines, createMeasure(4d, 2d)),
      entryOf(metricKeys.newConditionsToCover, createMeasure(27d, 13d)),
      entryOf(metricKeys.newUncoveredConditions, createMeasure(14d, 6d))
    );
    assertThat(toEntries(measureRepository.getAddedRawMeasures(FILE_3_REF))).contains(
      entryOf(metricKeys.newLinesToCover, createMeasure(5d, 3d)),
      entryOf(metricKeys.newUncoveredLines, createMeasure(2d, 1d)),
      entryOf(metricKeys.newConditionsToCover, createMeasure(17d, 13d)),
      entryOf(metricKeys.newUncoveredConditions, createMeasure(9d, 6d))
    );
    // directories
    assertThat(toEntries(measureRepository.getAddedRawMeasures(DIRECTORY_1_REF))).contains(
      entryOf(metricKeys.newLinesToCover, createMeasure(5d, 3d)),
      entryOf(metricKeys.newUncoveredLines, createMeasure(3d, 2d)),
      entryOf(metricKeys.newConditionsToCover, createMeasure(7d, 3d)),
      entryOf(metricKeys.newUncoveredConditions, createMeasure(4d, 1d))
    );
    assertThat(toEntries(measureRepository.getAddedRawMeasures(DIRECTORY_2_REF))).contains(
      entryOf(metricKeys.newLinesToCover, createMeasure(10d, 6d)),
      entryOf(metricKeys.newUncoveredLines, createMeasure(6d, 3d)),
      entryOf(metricKeys.newConditionsToCover, createMeasure(44d, 26d)),
      entryOf(metricKeys.newUncoveredConditions, createMeasure(23d, 12d))
    );
    // submodule
    MeasureRepoEntry[] repoEntriesFromSubModuleUp = {entryOf(metricKeys.newLinesToCover, createMeasure(15d, 9d)),
      entryOf(metricKeys.newUncoveredLines, createMeasure(9d, 5d)),
      entryOf(metricKeys.newConditionsToCover, createMeasure(51d, 29d)),
      entryOf(metricKeys.newUncoveredConditions, createMeasure(27d, 13d))};
    assertThat(toEntries(measureRepository.getAddedRawMeasures(SUB_MODULE_REF))).contains(repoEntriesFromSubModuleUp);
    // module
    assertThat(toEntries(measureRepository.getAddedRawMeasures(MODULE_REF))).contains(repoEntriesFromSubModuleUp);
    // project
    assertThat(toEntries(measureRepository.getAddedRawMeasures(ROOT_REF))).contains(repoEntriesFromSubModuleUp);
  }

  @Test
  public void verify_aggregates_variations_for_new_code_line_and_branch_Coverage() {
    LinesAndConditionsWithUncoveredMetricKeys metricKeys = new LinesAndConditionsWithUncoveredMetricKeys(
      CoreMetrics.NEW_LINES_TO_COVER_KEY, CoreMetrics.NEW_CONDITIONS_TO_COVER_KEY,
      CoreMetrics.NEW_UNCOVERED_LINES_KEY, CoreMetrics.NEW_UNCOVERED_CONDITIONS_KEY
    );
    String codeCoverageKey = CoreMetrics.NEW_COVERAGE_KEY;
    String lineCoverageKey = CoreMetrics.NEW_LINE_COVERAGE_KEY;
    String branchCoverageKey = CoreMetrics.NEW_BRANCH_COVERAGE_KEY;

    verify_aggregates_variations(metricKeys, codeCoverageKey, lineCoverageKey, branchCoverageKey);
  }

  @Test
  public void verify_aggregates_variations_for_new_IT_code_line_and_branch_Coverage() {
    LinesAndConditionsWithUncoveredMetricKeys metricKeys = new LinesAndConditionsWithUncoveredMetricKeys(
      CoreMetrics.NEW_IT_LINES_TO_COVER_KEY, CoreMetrics.NEW_IT_CONDITIONS_TO_COVER_KEY,
      CoreMetrics.NEW_IT_UNCOVERED_LINES_KEY, CoreMetrics.NEW_IT_UNCOVERED_CONDITIONS_KEY
    );
    String codeCoverageKey = CoreMetrics.NEW_IT_COVERAGE_KEY;
    String lineCoverageKey = CoreMetrics.NEW_IT_LINE_COVERAGE_KEY;
    String branchCoverageKey = CoreMetrics.NEW_IT_BRANCH_COVERAGE_KEY;

    verify_aggregates_variations(metricKeys, codeCoverageKey, lineCoverageKey, branchCoverageKey);
  }

  @Test
  public void verify_aggregates_variations_for_new_Overall_code_line_and_branch_Coverage() {
    LinesAndConditionsWithUncoveredMetricKeys metricKeys = new LinesAndConditionsWithUncoveredMetricKeys(
      CoreMetrics.NEW_OVERALL_LINES_TO_COVER_KEY, CoreMetrics.NEW_OVERALL_CONDITIONS_TO_COVER_KEY,
      CoreMetrics.NEW_OVERALL_UNCOVERED_LINES_KEY, CoreMetrics.NEW_OVERALL_UNCOVERED_CONDITIONS_KEY
    );
    String codeCoverageKey = CoreMetrics.NEW_OVERALL_COVERAGE_KEY;
    String lineCoverageKey = CoreMetrics.NEW_OVERALL_LINE_COVERAGE_KEY;
    String branchCoverageKey = CoreMetrics.NEW_OVERALL_BRANCH_COVERAGE_KEY;

    verify_aggregates_variations(metricKeys, codeCoverageKey, lineCoverageKey, branchCoverageKey);
  }

  private void verify_aggregates_variations(LinesAndConditionsWithUncoveredMetricKeys metricKeys, String codeCoverageKey, String lineCoverageKey, String branchCoverageKey) {
    treeRootHolder.setRoot(MULTIPLE_FILES_TREE);
    measureRepository
      .addRawMeasure(FILE_1_REF, metricKeys.getLines(), createMeasure(3000d, 2000d))
      .addRawMeasure(FILE_1_REF, metricKeys.getConditions(), createMeasure(300d, 400d))
      .addRawMeasure(FILE_1_REF, metricKeys.getUncoveredLines(), createMeasure(30d, 200d))
      .addRawMeasure(FILE_1_REF, metricKeys.getUncoveredConditions(), createMeasure(9d, 16d))

      .addRawMeasure(FILE_2_REF, metricKeys.getLines(), createMeasure(2000d, 3000d))
      .addRawMeasure(FILE_2_REF, metricKeys.getConditions(), createMeasure(400d, 300d))
      .addRawMeasure(FILE_2_REF, metricKeys.getUncoveredLines(), createMeasure(200d, 30d))
      .addRawMeasure(FILE_2_REF, metricKeys.getUncoveredConditions(), createMeasure(16d, 9d));

    underTest.execute();

    assertThat(toEntries(measureRepository.getAddedRawMeasures(FILE_1_REF))).containsOnly(
      entryOf(codeCoverageKey, createMeasure(98.8d, 91d)),
      entryOf(lineCoverageKey, createMeasure(99d, 90d)),
      entryOf(branchCoverageKey, createMeasure(97d, 96d))
    );
    assertThat(toEntries(measureRepository.getAddedRawMeasures(FILE_2_REF))).containsOnly(
      entryOf(codeCoverageKey, createMeasure(91d, 98.8d)),
      entryOf(lineCoverageKey, createMeasure(90d, 99d)),
      entryOf(branchCoverageKey, createMeasure(96d, 97d))
    );
    assertThat(measureRepository.getAddedRawMeasures(FILE_3_REF)).isEmpty();

    assertThat(toEntries(measureRepository.getAddedRawMeasures(DIRECTORY_1_REF))).containsOnly(
      entryOf(codeCoverageKey, createMeasure(98.8d, 91d)),
      entryOf(lineCoverageKey, createMeasure(99d, 90d)),
      entryOf(branchCoverageKey, createMeasure(97d, 96d))
    );

    assertThat(toEntries(measureRepository.getAddedRawMeasures(DIRECTORY_2_REF))).containsOnly(
      entryOf(codeCoverageKey, createMeasure(91d, 98.8d)),
      entryOf(lineCoverageKey, createMeasure(90d, 99d)),
      entryOf(branchCoverageKey, createMeasure(96d, 97d))
    );

    MeasureRepoEntry[] modulesAndProjectEntries = {
      entryOf(codeCoverageKey, createMeasure(95.5d, 95.5d)),
      entryOf(lineCoverageKey, createMeasure(95.4d, 95.4d)),
      entryOf(branchCoverageKey, createMeasure(96.4d, 96.4d))
    };

    assertThat(toEntries(measureRepository.getAddedRawMeasures(SUB_MODULE_REF))).containsOnly(modulesAndProjectEntries);
    assertThat(toEntries(measureRepository.getAddedRawMeasures(MODULE_REF))).containsOnly(modulesAndProjectEntries);
    assertThat(toEntries(measureRepository.getAddedRawMeasures(ROOT_REF))).containsOnly(modulesAndProjectEntries);
  }

  private void defineChangeSetsAndMeasures(int componentRef, MetricKeys metricKeys, MeasureValues line4, MeasureValues line6) {
    reportReader.putChangesets(Changesets.newBuilder()
      .setComponentRef(componentRef)
      .addChangeset(Changesets.Changeset.newBuilder().build())
      .addChangeset(Changesets.Changeset.newBuilder()
        .setDate(parseDate("2007-01-15").getTime())
        .build())
      .addChangeset(Changesets.Changeset.newBuilder()
        .setDate(parseDate("2011-01-01").getTime())
        .build())
      .addChangeset(Changesets.Changeset.newBuilder()
        .setDate(parseDate("2012-02-23").getTime())
        .build())
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(2) // line 2
      .addChangesetIndexByLine(1) // line 3
      .addChangesetIndexByLine(2) // line 4
      .addChangesetIndexByLine(3) // line 5
      .addChangesetIndexByLine(3) // line 6
      .addChangesetIndexByLine(3) // line 7
      .build());
    measureRepository.addRawMeasure(componentRef, metricKeys.coverageLineHitsData, newMeasureBuilder().create("2=0;3=2;4=" + line4.lineHits + ";5=1;6=" + line6.lineHits + ";7=0"));
    measureRepository.addRawMeasure(componentRef, metricKeys.conditionsByLine, newMeasureBuilder().create("4=" + line4.coveredConditions + ";6=" + line6.coveredConditions));
    measureRepository.addRawMeasure(componentRef, metricKeys.coveredConditionsByLine,
      newMeasureBuilder().create("4=" + line4.uncoveredConditions + ";6=" + line6.uncoveredConditions));
  }

  private static final class MetricKeys {
    private final String coverageLineHitsData;
    private final String conditionsByLine;
    private final String coveredConditionsByLine;
    private final String newLinesToCover;
    private final String newUncoveredLines;
    private final String newConditionsToCover;
    private final String newUncoveredConditions;

    public MetricKeys(String coverageLineHitsData, String conditionsByLine, String coveredConditionsByLine,
                      String newLinesToCover, String newUncoveredLines, String newConditionsToCover, String newUncoveredConditions) {
      this.coverageLineHitsData = coverageLineHitsData;
      this.conditionsByLine = conditionsByLine;
      this.coveredConditionsByLine = coveredConditionsByLine;
      this.newLinesToCover = newLinesToCover;
      this.newUncoveredLines = newUncoveredLines;
      this.newConditionsToCover = newConditionsToCover;
      this.newUncoveredConditions = newUncoveredConditions;
    }
  }

  private static final class MeasureValues {
    private final int lineHits;
    private final int coveredConditions;
    private final int uncoveredConditions;

    public MeasureValues(int lineHits, int coveredConditions, int uncoveredConditions) {
      this.lineHits = lineHits;
      this.coveredConditions = coveredConditions;
      this.uncoveredConditions = uncoveredConditions;
    }
  }

  private void verify_computation_of_measures_for_new_conditions(MetricKeys metricKeys) {
    treeRootHolder.setRoot(FILE_COMPONENT);
    defineChangeSetsAndMeasures(FILE_COMPONENT.getReportAttributes().getRef(), metricKeys, new MeasureValues(3, 4, 1), new MeasureValues(0, 3, 2));

    underTest.execute();

    assertThat(toEntries(measureRepository.getAddedRawMeasures(FILE_COMPONENT.getReportAttributes().getRef()))).contains(
      entryOf(metricKeys.newLinesToCover, createMeasure(5d, 3d)),
      entryOf(metricKeys.newUncoveredLines, createMeasure(3d, 2d)),
      entryOf(metricKeys.newConditionsToCover, createMeasure(7d, 3d)),
      entryOf(metricKeys.newUncoveredConditions, createMeasure(4d, 1d))
    );
  }

  private static Measure createMeasure(@Nullable Double variationPeriod2, @Nullable Double variationPeriod5) {
    MeasureVariations.Builder variationBuilder = newMeasureVariationsBuilder();
    if (variationPeriod2 != null) {
      variationBuilder.setVariation(new Period(2, "", null, 1L, 2L), variationPeriod2);
    }
    if (variationPeriod5 != null) {
      variationBuilder.setVariation(new Period(5, "", null, 1L, 2L), variationPeriod5);
    }
    return newMeasureBuilder()
      .setVariations(variationBuilder.build())
      .createNoValue();
  }

}
