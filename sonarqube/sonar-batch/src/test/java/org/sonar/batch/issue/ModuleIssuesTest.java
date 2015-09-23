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
package org.sonar.batch.issue;

import java.io.StringReader;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.FileMetadata;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.rule.internal.RulesBuilder;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssue;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssueLocation;
import org.sonar.api.resources.File;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.MessageException;
import org.sonar.batch.index.BatchComponentCache;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.report.ReportPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ModuleIssuesTest {

  static final RuleKey SQUID_RULE_KEY = RuleKey.of("squid", "AvoidCycle");
  static final String SQUID_RULE_NAME = "Avoid Cycle";

  @Mock
  IssueFilters filters;

  ActiveRulesBuilder activeRulesBuilder = new ActiveRulesBuilder();
  RulesBuilder ruleBuilder = new RulesBuilder();

  ModuleIssues moduleIssues;

  BatchComponentCache componentCache = new BatchComponentCache();
  InputFile file = new DefaultInputFile("foo", "src/Foo.php").initMetadata(new FileMetadata().readMetadata(new StringReader("Foo\nBar\nBiz\n")));
  ReportPublisher reportPublisher = mock(ReportPublisher.class, RETURNS_DEEP_STUBS);

  @Before
  public void prepare() {
    componentCache.add(File.create("src/Foo.php").setEffectiveKey("foo:src/Foo.php"), null).setInputComponent(file);
  }

  @Test
  public void fail_on_unknown_rule() {
    initModuleIssues();
    DefaultIssue issue = new DefaultIssue()
      .at(new DefaultIssueLocation().on(file).at(file.selectLine(3)).message("Foo"))
      .forRule(SQUID_RULE_KEY);
    try {
      moduleIssues.initAndAddIssue(issue);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(MessageException.class);
    }

    verifyZeroInteractions(reportPublisher);
  }

  @Test
  public void fail_if_rule_has_no_name_and_issue_has_no_message() {
    ruleBuilder.add(SQUID_RULE_KEY).setInternalKey(SQUID_RULE_KEY.rule());
    initModuleIssues();
    DefaultIssue issue = new DefaultIssue()
      .at(new DefaultIssueLocation().on(file).at(file.selectLine(3)).message(""))
      .forRule(SQUID_RULE_KEY);
    try {
      moduleIssues.initAndAddIssue(issue);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(MessageException.class);
    }

    verifyZeroInteractions(reportPublisher);
  }

  @Test
  public void ignore_null_active_rule() {
    ruleBuilder.add(SQUID_RULE_KEY).setName(SQUID_RULE_NAME);
    initModuleIssues();
    DefaultIssue issue = new DefaultIssue()
      .at(new DefaultIssueLocation().on(file).at(file.selectLine(3)).message("Foo"))
      .forRule(SQUID_RULE_KEY);
    boolean added = moduleIssues.initAndAddIssue(issue);

    assertThat(added).isFalse();
    verifyZeroInteractions(reportPublisher);
  }

  @Test
  public void ignore_null_rule_of_active_rule() {
    ruleBuilder.add(SQUID_RULE_KEY).setName(SQUID_RULE_NAME);
    activeRulesBuilder.create(SQUID_RULE_KEY).activate();
    initModuleIssues();

    DefaultIssue issue = new DefaultIssue()
      .at(new DefaultIssueLocation().on(file).at(file.selectLine(3)).message("Foo"))
      .forRule(SQUID_RULE_KEY);
    boolean added = moduleIssues.initAndAddIssue(issue);

    assertThat(added).isFalse();
    verifyZeroInteractions(reportPublisher);
  }

  @Test
  public void add_issue_to_cache() {
    ruleBuilder.add(SQUID_RULE_KEY).setName(SQUID_RULE_NAME);
    activeRulesBuilder.create(SQUID_RULE_KEY).setSeverity(Severity.INFO).activate();
    initModuleIssues();

    DefaultIssue issue = new DefaultIssue()
      .at(new DefaultIssueLocation().on(file).at(file.selectLine(3)).message("Foo"))
      .forRule(SQUID_RULE_KEY)
      .overrideSeverity(org.sonar.api.batch.rule.Severity.CRITICAL);

    when(filters.accept(anyString(), any(BatchReport.Issue.class))).thenReturn(true);

    boolean added = moduleIssues.initAndAddIssue(issue);

    assertThat(added).isTrue();
    ArgumentCaptor<BatchReport.Issue> argument = ArgumentCaptor.forClass(BatchReport.Issue.class);
    verify(reportPublisher.getWriter()).appendComponentIssue(eq(1), argument.capture());
    assertThat(argument.getValue().getSeverity()).isEqualTo(org.sonar.batch.protocol.Constants.Severity.CRITICAL);
  }

  @Test
  public void use_severity_from_active_rule_if_no_severity_on_issue() {
    ruleBuilder.add(SQUID_RULE_KEY).setName(SQUID_RULE_NAME);
    activeRulesBuilder.create(SQUID_RULE_KEY).setSeverity(Severity.INFO).activate();
    initModuleIssues();

    DefaultIssue issue = new DefaultIssue()
      .at(new DefaultIssueLocation().on(file).at(file.selectLine(3)).message("Foo"))
      .forRule(SQUID_RULE_KEY);
    when(filters.accept(anyString(), any(BatchReport.Issue.class))).thenReturn(true);
    moduleIssues.initAndAddIssue(issue);

    ArgumentCaptor<BatchReport.Issue> argument = ArgumentCaptor.forClass(BatchReport.Issue.class);
    verify(reportPublisher.getWriter()).appendComponentIssue(eq(1), argument.capture());
    assertThat(argument.getValue().getSeverity()).isEqualTo(org.sonar.batch.protocol.Constants.Severity.INFO);
  }

  @Test
  public void use_rule_name_if_no_message() {
    ruleBuilder.add(SQUID_RULE_KEY).setName(SQUID_RULE_NAME);
    activeRulesBuilder.create(SQUID_RULE_KEY).setSeverity(Severity.INFO).setName(SQUID_RULE_NAME).activate();
    initModuleIssues();

    DefaultIssue issue = new DefaultIssue()
      .at(new DefaultIssueLocation().on(file).at(file.selectLine(3)).message(""))
      .forRule(SQUID_RULE_KEY);
    when(filters.accept(anyString(), any(BatchReport.Issue.class))).thenReturn(true);

    boolean added = moduleIssues.initAndAddIssue(issue);

    assertThat(added).isTrue();
    ArgumentCaptor<BatchReport.Issue> argument = ArgumentCaptor.forClass(BatchReport.Issue.class);
    verify(reportPublisher.getWriter()).appendComponentIssue(eq(1), argument.capture());
    assertThat(argument.getValue().getMsg()).isEqualTo("Avoid Cycle");
  }

  @Test
  public void filter_issue() {
    ruleBuilder.add(SQUID_RULE_KEY).setName(SQUID_RULE_NAME);
    activeRulesBuilder.create(SQUID_RULE_KEY).setSeverity(Severity.INFO).activate();
    initModuleIssues();

    DefaultIssue issue = new DefaultIssue()
      .at(new DefaultIssueLocation().on(file).at(file.selectLine(3)).message(""))
      .forRule(SQUID_RULE_KEY);

    when(filters.accept(anyString(), any(BatchReport.Issue.class))).thenReturn(false);

    boolean added = moduleIssues.initAndAddIssue(issue);

    assertThat(added).isFalse();
    verifyZeroInteractions(reportPublisher);
  }

  /**
   * Every rules and active rules has to be added in builders before creating ModuleIssues
   */
  private void initModuleIssues() {
    moduleIssues = new ModuleIssues(activeRulesBuilder.build(), ruleBuilder.build(), filters, reportPublisher, componentCache);
  }

}
