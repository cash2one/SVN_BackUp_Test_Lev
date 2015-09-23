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
package org.sonar.server.computation.batch;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.internal.JUnitTempFolder;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportWriter;
import org.sonar.core.util.CloseableIterator;

import static com.google.common.collect.ImmutableList.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;

public class BatchReportReaderImplTest {
  private static final int COMPONENT_REF = 1;
  private static final BatchReport.Changesets CHANGESETS = BatchReport.Changesets.newBuilder().setComponentRef(COMPONENT_REF).build();
  private static final BatchReport.Measure MEASURE = BatchReport.Measure.newBuilder().build();
  private static final BatchReport.Component COMPONENT = BatchReport.Component.newBuilder().setRef(COMPONENT_REF).build();
  private static final BatchReport.Issue ISSUE = BatchReport.Issue.newBuilder().build();
  private static final BatchReport.Duplication DUPLICATION = BatchReport.Duplication.newBuilder().build();
  private static final BatchReport.Symbol SYMBOL = BatchReport.Symbol.newBuilder().build();
  private static final BatchReport.SyntaxHighlighting SYNTAX_HIGHLIGHTING_1 = BatchReport.SyntaxHighlighting.newBuilder().build();
  private static final BatchReport.SyntaxHighlighting SYNTAX_HIGHLIGHTING_2 = BatchReport.SyntaxHighlighting.newBuilder().build();
  private static final BatchReport.Coverage COVERAGE_1 = BatchReport.Coverage.newBuilder().build();
  private static final BatchReport.Coverage COVERAGE_2 = BatchReport.Coverage.newBuilder().build();
  private static final BatchReport.Test TEST_1 = BatchReport.Test.newBuilder().setName("1").build();
  private static final BatchReport.Test TEST_2 = BatchReport.Test.newBuilder().setName("2").build();
  private static final BatchReport.CoverageDetail COVERAGE_DETAIL_1 = BatchReport.CoverageDetail.newBuilder().setTestName("1").build();
  private static final BatchReport.CoverageDetail COVERAGE_DETAIL_2 = BatchReport.CoverageDetail.newBuilder().setTestName("2").build();

  @Rule
  public JUnitTempFolder tempFolder = new JUnitTempFolder();

  private BatchReportWriter writer;
  private BatchReportReaderImpl underTest;

  @Before
  public void setUp() {
    BatchReportDirectoryHolder holder = new ImmutableBatchReportDirectoryHolder(tempFolder.newDir());
    underTest = new BatchReportReaderImpl(holder);
    writer = new BatchReportWriter(holder.getDirectory());
  }

  @Test(expected = IllegalStateException.class)
  public void readMetadata_throws_ISE_if_no_metadata() {
    underTest.readMetadata();
  }

  @Test
  public void readMetadata_result_is_cached() {
    BatchReport.Metadata metadata = BatchReport.Metadata.newBuilder().build();

    writer.writeMetadata(metadata);

    BatchReport.Metadata res = underTest.readMetadata();
    assertThat(res).isEqualTo(metadata);
    assertThat(underTest.readMetadata()).isSameAs(res);
  }

  @Test
  public void readComponentMeasures_returns_empty_list_if_there_is_no_measure() {
    assertThat(underTest.readComponentMeasures(COMPONENT_REF)).isEmpty();
  }

  @Test
  public void verify_readComponentMeasures_returns_measures() {
    writer.writeComponentMeasures(COMPONENT_REF, of(MEASURE));

    try (CloseableIterator<BatchReport.Measure> measures = underTest.readComponentMeasures(COMPONENT_REF)) {
      assertThat(measures.next()).isEqualTo(MEASURE);
      assertThat(measures.hasNext()).isFalse();
    }
  }

  @Test
  public void readComponentMeasures_is_not_cached() {
    writer.writeComponentMeasures(COMPONENT_REF, of(MEASURE));

    assertThat(underTest.readComponentMeasures(COMPONENT_REF)).isNotSameAs(underTest.readComponentMeasures(COMPONENT_REF));
  }

  @Test
  public void readChangesets_returns_null_if_no_changeset() {
    assertThat(underTest.readChangesets(COMPONENT_REF)).isNull();
  }

  @Test
  public void verify_readChangesets_returns_changesets() {
    writer.writeComponentChangesets(CHANGESETS);

    BatchReport.Changesets res = underTest.readChangesets(COMPONENT_REF);
    assertThat(res).isEqualTo(CHANGESETS);
  }

  @Test
  public void readChangesets_is_not_cached() {
    writer.writeComponentChangesets(CHANGESETS);

    assertThat(underTest.readChangesets(COMPONENT_REF)).isNotSameAs(underTest.readChangesets(COMPONENT_REF));
  }

  @Test(expected = IllegalStateException.class)
  public void readComponent_throws_ISE_if_file_does_not_exist() {
    underTest.readComponent(COMPONENT_REF);
  }

  @Test
  public void verify_readComponent_returns_Component() {
    writer.writeComponent(COMPONENT);

    assertThat(underTest.readComponent(COMPONENT_REF)).isEqualTo(COMPONENT);
  }

  @Test
  public void readComponent_is_not_cached() {
    writer.writeComponent(COMPONENT);

    assertThat(underTest.readComponent(COMPONENT_REF)).isNotSameAs(underTest.readComponent(COMPONENT_REF));
  }

  @Test
  public void readComponentIssues_returns_empty_list_if_file_does_not_exist() {
    assertThat(underTest.readComponentIssues(COMPONENT_REF)).isEmpty();
  }

  @Test
  public void verify_readComponentIssues_returns_Issues() {
    writer.writeComponentIssues(COMPONENT_REF, of(ISSUE));

    try (CloseableIterator<BatchReport.Issue> res = underTest.readComponentIssues(COMPONENT_REF)) {
      assertThat(res.next()).isEqualTo(ISSUE);
      assertThat(res.hasNext()).isFalse();
    }
  }

  @Test
  public void readComponentIssues_it_not_cached() {
    writer.writeComponentIssues(COMPONENT_REF, of(ISSUE));

    assertThat(underTest.readComponentIssues(COMPONENT_REF)).isNotSameAs(underTest.readComponentIssues(COMPONENT_REF));
  }

  @Test
  public void readComponentDuplications_returns_empty_list_if_file_does_not_exist() {
    assertThat(underTest.readComponentDuplications(COMPONENT_REF)).isEmpty();
  }

  @Test
  public void verify_readComponentDuplications_returns_Issues() {
    writer.writeComponentDuplications(COMPONENT_REF, of(DUPLICATION));

    try (CloseableIterator<BatchReport.Duplication> res = underTest.readComponentDuplications(COMPONENT_REF)) {
      assertThat(res.next()).isEqualTo(DUPLICATION);
      assertThat(res.hasNext()).isFalse();
    }
  }

  @Test
  public void readComponentDuplications_it_not_cached() {
    writer.writeComponentDuplications(COMPONENT_REF, of(DUPLICATION));

    assertThat(underTest.readComponentDuplications(COMPONENT_REF)).isNotSameAs(underTest.readComponentDuplications(COMPONENT_REF));
  }

  @Test
  public void readComponentSymbols_returns_empty_list_if_file_does_not_exist() {
    assertThat(underTest.readComponentSymbols(COMPONENT_REF)).isEmpty();
  }

  @Test
  public void verify_readComponentSymbols_returns_Issues() {
    writer.writeComponentSymbols(COMPONENT_REF, of(SYMBOL));

    try (CloseableIterator<BatchReport.Symbol> res = underTest.readComponentSymbols(COMPONENT_REF)) {
      assertThat(res.next()).isEqualTo(SYMBOL);
      assertThat(res.hasNext()).isFalse();
    }
  }

  @Test
  public void readComponentSymbols_it_not_cached() {
    writer.writeComponentSymbols(COMPONENT_REF, of(SYMBOL));

    assertThat(underTest.readComponentSymbols(COMPONENT_REF)).isNotSameAs(underTest.readComponentSymbols(COMPONENT_REF));
  }

  @Test
  public void readComponentSyntaxHighlighting_returns_empty_CloseableIterator_when_file_does_not_exist() {
    assertThat(underTest.readComponentSyntaxHighlighting(COMPONENT_REF)).isEmpty();
  }

  @Test
  public void verify_readComponentSyntaxHighlighting() {
    writer.writeComponentSyntaxHighlighting(COMPONENT_REF, of(SYNTAX_HIGHLIGHTING_1, SYNTAX_HIGHLIGHTING_2));

    CloseableIterator<BatchReport.SyntaxHighlighting> res = underTest.readComponentSyntaxHighlighting(COMPONENT_REF);
    assertThat(res).containsExactly(SYNTAX_HIGHLIGHTING_1, SYNTAX_HIGHLIGHTING_2);
    res.close();
  }

  @Test
  public void readComponentCoverage_returns_empty_CloseableIterator_when_file_does_not_exist() {
    assertThat(underTest.readComponentCoverage(COMPONENT_REF)).isEmpty();
  }

  @Test
  public void verify_readComponentCoverage() {
    writer.writeComponentCoverage(COMPONENT_REF, of(COVERAGE_1, COVERAGE_2));

    CloseableIterator<BatchReport.Coverage> res = underTest.readComponentCoverage(COMPONENT_REF);
    assertThat(res).containsExactly(COVERAGE_1, COVERAGE_2);
    res.close();
  }

  @Test
  public void readFileSource_returns_absent_optional_when_file_does_not_exist() {
    assertThat(underTest.readFileSource(COMPONENT_REF)).isAbsent();
  }

  @Test
  public void verify_readFileSource() throws IOException {
    File file = writer.getSourceFile(COMPONENT_REF);
    FileUtils.writeLines(file, of("1", "2", "3"));

    CloseableIterator<String> res = underTest.readFileSource(COMPONENT_REF).get();
    assertThat(res).containsExactly("1", "2", "3");
    res.close();
  }

  @Test
  public void readTests_returns_empty_CloseableIterator_when_file_does_not_exist() {
    assertThat(underTest.readTests(COMPONENT_REF)).isEmpty();
  }

  @Test
  public void verify_readTests() {
    writer.writeTests(COMPONENT_REF, of(TEST_1, TEST_2));

    CloseableIterator<BatchReport.Test> res = underTest.readTests(COMPONENT_REF);
    assertThat(res).containsExactly(TEST_1, TEST_2);
    res.close();
  }

  @Test
  public void readCoverageDetails_returns_empty_CloseableIterator_when_file_does_not_exist() {
    assertThat(underTest.readCoverageDetails(COMPONENT_REF)).isEmpty();
  }

  @Test
  public void verify_readCoverageDetails() {
    writer.writeCoverageDetails(COMPONENT_REF, of(COVERAGE_DETAIL_1, COVERAGE_DETAIL_2));

    CloseableIterator<BatchReport.CoverageDetail> res = underTest.readCoverageDetails(COMPONENT_REF);
    assertThat(res).containsExactly(COVERAGE_DETAIL_1, COVERAGE_DETAIL_2);
    res.close();
  }
}
