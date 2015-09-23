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
package org.sonar.server.computation.issue.commonrule;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.ReportComponent;
import org.sonar.server.computation.component.FileAttributes;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureRepositoryRule;
import org.sonar.server.computation.metric.MetricRepositoryRule;
import org.sonar.server.computation.qualityprofile.ActiveRule;
import org.sonar.server.computation.qualityprofile.ActiveRulesHolderRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.computation.component.ReportComponent.DUMB_PROJECT;

public abstract class CoverageRuleTest {

  static ReportComponent FILE = ReportComponent.builder(Component.Type.FILE, 1)
    .setFileAttributes(new FileAttributes(false, "java"))
    .build();

  @Rule
  public ActiveRulesHolderRule activeRuleHolder = new ActiveRulesHolderRule();

  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(CoreMetrics.LINE_COVERAGE)
    .add(CoreMetrics.LINES_TO_COVER)
    .add(CoreMetrics.UNCOVERED_LINES)
    .add(CoreMetrics.BRANCH_COVERAGE)
    .add(CoreMetrics.CONDITIONS_TO_COVER)
    .add(CoreMetrics.UNCOVERED_CONDITIONS);

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  CommonRule underTest = createRule();

  @Before
  public void setUp() {
    treeRootHolder.setRoot(DUMB_PROJECT);
  }

  protected abstract CommonRule createRule();

  protected abstract RuleKey getRuleKey();

  protected abstract String getMinPropertyKey();

  protected abstract String getCoverageMetricKey();

  protected abstract String getToCoverMetricKey();

  protected abstract String getUncoveredMetricKey();

  @Test
  public void no_issue_if_enough_coverage() {
    activeRuleHolder.put(new ActiveRule(getRuleKey(), Severity.CRITICAL, ImmutableMap.of(getMinPropertyKey(), "65")));
    measureRepository.addRawMeasure(FILE.getReportAttributes().getRef(), getCoverageMetricKey(), Measure.newMeasureBuilder().create(90.0));

    DefaultIssue issue = underTest.processFile(FILE, "java");

    assertThat(issue).isNull();
  }

  @Test
  public void issue_if_coverage_is_too_low() {
    activeRuleHolder.put(new ActiveRule(getRuleKey(), Severity.CRITICAL, ImmutableMap.of(getMinPropertyKey(), "65")));
    measureRepository.addRawMeasure(FILE.getReportAttributes().getRef(), getCoverageMetricKey(), Measure.newMeasureBuilder().create(20.0));
    measureRepository.addRawMeasure(FILE.getReportAttributes().getRef(), getUncoveredMetricKey(), Measure.newMeasureBuilder().create(40));
    measureRepository.addRawMeasure(FILE.getReportAttributes().getRef(), getToCoverMetricKey(), Measure.newMeasureBuilder().create(50));

    DefaultIssue issue = underTest.processFile(FILE, "java");

    assertThat(issue.ruleKey()).isEqualTo(getRuleKey());
    assertThat(issue.severity()).isEqualTo(Severity.CRITICAL);
    // FIXME explain
    assertThat(issue.effortToFix()).isEqualTo(23.0);
    assertThat(issue.message()).isEqualTo(getExpectedIssueMessage());
  }

  protected abstract String getExpectedIssueMessage();

  @Test
  public void no_issue_if_coverage_is_not_set() {
    activeRuleHolder.put(new ActiveRule(getRuleKey(), Severity.CRITICAL, ImmutableMap.of(getMinPropertyKey(), "65")));

    DefaultIssue issue = underTest.processFile(FILE, "java");

    assertThat(issue).isNull();
  }

  @Test
  public void ignored_if_rule_is_deactivated() {
    // coverage is too low, but rule is not activated
    measureRepository.addRawMeasure(FILE.getReportAttributes().getRef(), getCoverageMetricKey(), Measure.newMeasureBuilder().create(20.0));

    DefaultIssue issue = underTest.processFile(FILE, "java");

    assertThat(issue).isNull();
  }
}
