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
package org.sonar.server.computation.source;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.utils.System2;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.server.computation.batch.BatchReportReaderRule;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.ComponentVisitor;
import org.sonar.server.computation.component.FileAttributes;
import org.sonar.server.computation.component.ReportComponent;
import org.sonar.server.computation.component.ViewsComponent;
import org.sonar.server.computation.component.VisitorsCrawler;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureRepositoryRule;
import org.sonar.server.computation.metric.MetricRepositoryRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.measures.CoreMetrics.DAYS_SINCE_LAST_COMMIT_KEY;
import static org.sonar.api.measures.CoreMetrics.LAST_COMMIT_DATE_KEY;
import static org.sonar.server.computation.component.Component.Type.DIRECTORY;
import static org.sonar.server.computation.component.Component.Type.FILE;
import static org.sonar.server.computation.component.Component.Type.MODULE;
import static org.sonar.server.computation.component.Component.Type.PROJECT;
import static org.sonar.server.computation.component.Component.Type.PROJECT_VIEW;
import static org.sonar.server.computation.component.Component.Type.SUBVIEW;
import static org.sonar.server.computation.component.Component.Type.VIEW;
import static org.sonar.server.computation.component.ViewsComponent.builder;
import static org.sonar.server.computation.measure.Measure.newMeasureBuilder;

public class LastCommitVisitorTest {

  public static final int PROJECT_REF = 1;
  public static final int MODULE_REF = 2;
  public static final int FILE_1_REF = 1_111;
  public static final int FILE_2_REF = 1_112;
  public static final int FILE_3_REF = 1_121;
  public static final int DIR_1_REF = 3;
  public static final int DIR_2_REF = 4;
  public static final long NOW = 1_800_000_000_000L;

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();

  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(CoreMetrics.LAST_COMMIT_DATE)
    .add(CoreMetrics.DAYS_SINCE_LAST_COMMIT);

  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  System2 system2 = mock(System2.class);

  @Before
  public void setUp() {
    when(system2.now()).thenReturn(NOW);
  }

  @Test
  public void aggregate_date_of_last_commit_to_directories_and_project() {
    final long FILE_1_DATE = 1_100_000_000_000L;
    // FILE_2 is the most recent file in DIR_1
    final long FILE_2_DATE = 1_200_000_000_000L;
    // FILE_3 is the most recent file in the project
    final long FILE_3_DATE = 1_300_000_000_000L;

    // simulate the output of visitFile()
    LastCommitVisitor visitor = new LastCommitVisitor(reportReader, metricRepository, measureRepository, system2) {
      @Override
      public void visitFile(Component file, Path<LastCommit> path) {
        long fileDate;
        switch (file.getReportAttributes().getRef()) {
          case FILE_1_REF:
            fileDate = FILE_1_DATE;
            break;
          case FILE_2_REF:
            fileDate = FILE_2_DATE;
            break;
          case FILE_3_REF:
            fileDate = FILE_3_DATE;
            break;
          default:
            throw new IllegalArgumentException();
        }
        path.parent().addDate(fileDate);
      }
    };

    // project with 1 module, 2 directories and 3 files
    ReportComponent project = ReportComponent.builder(PROJECT, PROJECT_REF)
      .addChildren(
        ReportComponent.builder(MODULE, MODULE_REF)
          .addChildren(
            ReportComponent.builder(DIRECTORY, DIR_1_REF)
              .addChildren(
                createFileComponent(FILE_1_REF),
                createFileComponent(FILE_2_REF))
              .build(),
            ReportComponent.builder(DIRECTORY, DIR_2_REF)
              .addChildren(
                createFileComponent(FILE_3_REF))
              .build())
          .build())
      .build();
    treeRootHolder.setRoot(project);

    VisitorsCrawler underTest = new VisitorsCrawler(Lists.<ComponentVisitor>newArrayList(visitor));
    underTest.visit(project);

    assertDate(DIR_1_REF, FILE_2_DATE);
    assertDaysSinceLastCommit(DIR_1_REF, 6944 /* number of days between FILE_2_DATE and now */);
    assertDate(DIR_2_REF, FILE_3_DATE);
    assertDaysSinceLastCommit(DIR_2_REF, 5787 /* number of days between FILE_3_DATE and now */);

    // module = most recent commit date of directories
    assertDate(MODULE_REF, FILE_3_DATE);
    assertDaysSinceLastCommit(MODULE_REF, 5787 /* number of days between FILE_3_DATE and now */);

    // project
    assertDate(PROJECT_REF, FILE_3_DATE);
    assertDaysSinceLastCommit(PROJECT_REF, 5787 /* number of days between FILE_3_DATE and now */);
  }

  @Test
  public void aggregate_date_of_last_commit_to_views() {
    final int VIEW_REF = 1;
    final int SUBVIEW_1_REF = 2;
    final int SUBVIEW_2_REF = 3;
    final int SUBVIEW_3_REF = 4;
    final int PROJECT_1_REF = 5;
    final int PROJECT_2_REF = 6;
    final int PROJECT_3_REF = 7;
    final long PROJECT_1_DATE = 1_500_000_000_000L;
    // the second project has the most recent commit date
    final long PROJECT_2_DATE = 1_700_000_000_000L;
    final long PROJECT_3_DATE = 1_600_000_000_000L;
    // view with 3 nested sub-views and 3 projects
    ViewsComponent view = ViewsComponent.builder(VIEW, VIEW_REF)
      .addChildren(
        builder(SUBVIEW, SUBVIEW_1_REF)
          .addChildren(
            builder(SUBVIEW, SUBVIEW_2_REF)
              .addChildren(
                builder(PROJECT_VIEW, PROJECT_1_REF).build(),
                builder(PROJECT_VIEW, PROJECT_2_REF).build())
              .build(),
            builder(SUBVIEW, SUBVIEW_3_REF)
              .addChildren(
                builder(PROJECT_VIEW, PROJECT_3_REF).build())
              .build())
          .build())
      .build();
    treeRootHolder.setRoot(view);

    measureRepository.addRawMeasure(PROJECT_1_REF, LAST_COMMIT_DATE_KEY, newMeasureBuilder().create(PROJECT_1_DATE));
    measureRepository.addRawMeasure(PROJECT_2_REF, LAST_COMMIT_DATE_KEY, newMeasureBuilder().create(PROJECT_2_DATE));
    measureRepository.addRawMeasure(PROJECT_3_REF, LAST_COMMIT_DATE_KEY, newMeasureBuilder().create(PROJECT_3_DATE));

    VisitorsCrawler underTest = new VisitorsCrawler(Lists.<ComponentVisitor>newArrayList(new LastCommitVisitor(reportReader, metricRepository, measureRepository, system2)));
    underTest.visit(view);

    // second level of sub-views
    assertDate(SUBVIEW_2_REF, PROJECT_2_DATE);
    assertDaysSinceLastCommit(SUBVIEW_2_REF, 1157 /* nb of days between PROJECT_2_DATE and NOW */);
    assertDate(SUBVIEW_3_REF, PROJECT_3_DATE);
    assertDaysSinceLastCommit(SUBVIEW_3_REF, 2314 /* nb of days between PROJECT_3_DATE and NOW */);

    // first level of sub-views
    assertDate(SUBVIEW_1_REF, PROJECT_2_DATE);
    assertDaysSinceLastCommit(SUBVIEW_1_REF, 1157 /* nb of days between PROJECT_2_DATE and NOW */);

    // view
    assertDate(VIEW_REF, PROJECT_2_DATE);
    assertDaysSinceLastCommit(VIEW_REF, 1157 /* nb of days between PROJECT_2_DATE and NOW */);
  }

  @Test
  public void compute_date_of_file_from_blame_info_of_report() throws Exception {
    VisitorsCrawler underTest = new VisitorsCrawler(Lists.<ComponentVisitor>newArrayList(new LastCommitVisitor(reportReader, metricRepository, measureRepository, system2)));

    BatchReport.Changesets changesets = BatchReport.Changesets.newBuilder()
      .setComponentRef(FILE_1_REF)
      .addChangeset(BatchReport.Changesets.Changeset.newBuilder()
        .setAuthor("john")
        .setDate(1_500_000_000_000L)
        .setRevision("rev-1")
        .build())
      .addChangeset(BatchReport.Changesets.Changeset.newBuilder()
        .setAuthor("tom")
        // this is the most recent change
        .setDate(1_600_000_000_000L)
        .setRevision("rev-2")
        .build())
      .addChangeset(BatchReport.Changesets.Changeset.newBuilder()
        .setAuthor("john")
        .setDate(1_500_000_000_000L)
        .setRevision("rev-1")
        .build())
      .addChangesetIndexByLine(0)
      .build();
    reportReader.putChangesets(changesets);
    ReportComponent file = createFileComponent(FILE_1_REF);
    treeRootHolder.setRoot(file);

    underTest.visit(file);

    assertDate(FILE_1_REF, 1_600_000_000_000L);
    assertDaysSinceLastCommit(FILE_1_REF, 2314);
  }

  private void assertDate(int componentRef, long expectedDate) {
    Optional<Measure> measure = measureRepository.getAddedRawMeasure(componentRef, LAST_COMMIT_DATE_KEY);
    assertThat(measure.isPresent()).isTrue();
    assertThat(measure.get().getLongValue()).isEqualTo(expectedDate);
  }

  private void assertDaysSinceLastCommit(int componentRef, int numberOfDays) {
    Optional<Measure> measure = measureRepository.getAddedRawMeasure(componentRef, DAYS_SINCE_LAST_COMMIT_KEY);
    assertThat(measure.isPresent()).isTrue();
    assertThat(measure.get().getIntValue()).isEqualTo(numberOfDays);
  }

  /**
   * When the file was not changed since previous analysis, than the report may not contain
   * the SCM blame information. In this case the date of last commit is loaded
   * from the base measure of previous analysis, directly from database
   */
  @Test
  public void reuse_date_of_previous_analysis_if_blame_info_is_not_in_report() throws Exception {
    VisitorsCrawler underTest = new VisitorsCrawler(Lists.<ComponentVisitor>newArrayList(new LastCommitVisitor(reportReader, metricRepository, measureRepository, system2)));
    ReportComponent file = createFileComponent(FILE_1_REF);
    treeRootHolder.setRoot(file);
    measureRepository.addBaseMeasure(FILE_1_REF, LAST_COMMIT_DATE_KEY, newMeasureBuilder().create(1_500_000_000L));

    underTest.visit(file);

    assertDate(FILE_1_REF, 1_500_000_000L);
  }

  @Test
  public void date_is_not_computed_on_file_if_blame_is_not_in_report_nor_in_previous_analysis() throws Exception {
    VisitorsCrawler underTest = new VisitorsCrawler(Lists.<ComponentVisitor>newArrayList(new LastCommitVisitor(reportReader, metricRepository, measureRepository, system2)));
    ReportComponent file = createFileComponent(FILE_1_REF);
    treeRootHolder.setRoot(file);

    underTest.visit(file);

    Optional<Measure> measure = measureRepository.getAddedRawMeasure(FILE_1_REF, LAST_COMMIT_DATE_KEY);
    assertThat(measure.isPresent()).isFalse();
  }

  private ReportComponent createFileComponent(int fileRef) {
    return ReportComponent.builder(FILE, fileRef).setFileAttributes(new FileAttributes(false, "js")).build();
  }
}
