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
package org.sonar.server.computation.issue;

import com.google.common.collect.Iterators;
import java.util.Collection;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.tracking.Input;
import org.sonar.server.computation.batch.BatchReportReaderRule;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.ReportComponent;
import org.sonar.server.computation.issue.commonrule.CommonRuleEngine;
import org.sonar.server.computation.source.SourceLinesRepositoryRule;
import org.sonar.server.rule.CommonRuleKeys;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TrackerRawInputFactoryTest {

  static ReportComponent PROJECT = ReportComponent.builder(Component.Type.PROJECT, 1).setKey("PROJECT_KEY_2").setUuid("PROJECT_UUID_1").build();
  static ReportComponent FILE = ReportComponent.builder(Component.Type.FILE, 2).setKey("FILE_KEY_2").setUuid("FILE_UUID_2").build();

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule().setRoot(PROJECT);

  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();

  @Rule
  public SourceLinesRepositoryRule fileSourceRepository = new SourceLinesRepositoryRule();

  CommonRuleEngine commonRuleEngine = mock(CommonRuleEngine.class);

  TrackerRawInputFactory underTest = new TrackerRawInputFactory(treeRootHolder, reportReader, fileSourceRepository, commonRuleEngine);

  @Test
  public void load_source_hash_sequences() throws Exception {
    fileSourceRepository.addLine("line 1;").addLine("line 2;");
    Input<DefaultIssue> input = underTest.create(FILE);

    assertThat(input.getLineHashSequence()).isNotNull();
    assertThat(input.getLineHashSequence().getHashForLine(1)).isNotEmpty();
    assertThat(input.getLineHashSequence().getHashForLine(2)).isNotEmpty();
    assertThat(input.getLineHashSequence().getHashForLine(3)).isEmpty();

    assertThat(input.getBlockHashSequence()).isNotNull();
  }

  @Test
  public void load_source_hash_sequences_only_on_files() throws Exception {
    Input<DefaultIssue> input = underTest.create(PROJECT);

    assertThat(input.getLineHashSequence()).isNotNull();
    assertThat(input.getBlockHashSequence()).isNotNull();
  }

  @Test
  public void load_issues() throws Exception {
    fileSourceRepository.addLine("line 1;").addLine("line 2;");
    BatchReport.Issue reportIssue = BatchReport.Issue.newBuilder()
      .setLine(2)
      .setMsg("the message")
      .setRuleRepository("java")
      .setRuleKey("S001")
      .setSeverity(Constants.Severity.BLOCKER)
      .setEffortToFix(3.14)
      .build();
    reportReader.putIssues(FILE.getReportAttributes().getRef(), asList(reportIssue));
    Input<DefaultIssue> input = underTest.create(FILE);

    Collection<DefaultIssue> issues = input.getIssues();
    assertThat(issues).hasSize(1);
    DefaultIssue issue = Iterators.getOnlyElement(issues.iterator());

    // fields set by analysis report
    assertThat(issue.ruleKey()).isEqualTo(RuleKey.of("java", "S001"));
    assertThat(issue.severity()).isEqualTo(Severity.BLOCKER);
    assertThat(issue.line()).isEqualTo(2);
    assertThat(issue.effortToFix()).isEqualTo(3.14);
    assertThat(issue.message()).isEqualTo("the message");

    // fields set by compute engine
    assertThat(issue.checksum()).isEqualTo(input.getLineHashSequence().getHashForLine(2));
    assertThat(issue.tags()).isEmpty();
    assertInitializedIssue(issue);
  }

  @Test
  public void ignore_report_issues_on_common_rules() throws Exception {
    fileSourceRepository.addLine("line 1;").addLine("line 2;");
    BatchReport.Issue reportIssue = BatchReport.Issue.newBuilder()
      .setMsg("the message")
      .setRuleRepository(CommonRuleKeys.commonRepositoryForLang("java"))
      .setRuleKey("S001")
      .setSeverity(Constants.Severity.BLOCKER)
      .build();
    reportReader.putIssues(FILE.getReportAttributes().getRef(), asList(reportIssue));

    Input<DefaultIssue> input = underTest.create(FILE);

    assertThat(input.getIssues()).isEmpty();
  }

  @Test
  public void load_issues_of_compute_engine_common_rules() throws Exception {
    fileSourceRepository.addLine("line 1;").addLine("line 2;");
    DefaultIssue ceIssue = new DefaultIssue()
      .setRuleKey(RuleKey.of(CommonRuleKeys.commonRepositoryForLang("java"), "InsufficientCoverage"))
      .setMessage("not enough coverage")
      .setEffortToFix(10.0);
    when(commonRuleEngine.process(FILE)).thenReturn(asList(ceIssue));

    Input<DefaultIssue> input = underTest.create(FILE);

    assertThat(input.getIssues()).containsOnly(ceIssue);
    assertInitializedIssue(input.getIssues().iterator().next());
  }

  private void assertInitializedIssue(DefaultIssue issue) {
    assertThat(issue.componentKey()).isEqualTo(FILE.getKey());
    assertThat(issue.componentUuid()).isEqualTo(FILE.getUuid());
    assertThat(issue.resolution()).isNull();
    assertThat(issue.status()).isEqualTo(Issue.STATUS_OPEN);
    assertThat(issue.key()).isNull();
    assertThat(issue.actionPlanKey()).isNull();
    assertThat(issue.authorLogin()).isNull();
    assertThat(issue.debt()).isNull();
  }
}
