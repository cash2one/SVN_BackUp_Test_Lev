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

import java.util.Arrays;
import java.util.Collections;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.utils.System2;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.util.CloseableIterator;
import org.sonar.server.computation.component.ComponentVisitor;
import org.sonar.server.computation.component.ReportComponent;
import org.sonar.server.computation.component.VisitorsCrawler;

import static com.google.common.collect.Sets.newHashSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.server.computation.component.Component.Type.DIRECTORY;
import static org.sonar.server.computation.component.Component.Type.FILE;
import static org.sonar.server.computation.component.Component.Type.MODULE;
import static org.sonar.server.computation.component.Component.Type.PROJECT;

public class CloseIssuesOnRemovedComponentsVisitorTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  BaseIssuesLoader baseIssuesLoader = mock(BaseIssuesLoader.class);
  ComponentsWithUnprocessedIssues componentsWithUnprocessedIssues = mock(ComponentsWithUnprocessedIssues.class);
  IssueLifecycle issueLifecycle = mock(IssueLifecycle.class);
  IssueCache issueCache;
  VisitorsCrawler underTest;

  @Before
  public void setUp() throws Exception {
    issueCache = new IssueCache(temp.newFile(), System2.INSTANCE);
    underTest = new VisitorsCrawler(Arrays.<ComponentVisitor>asList(new CloseIssuesOnRemovedComponentsVisitor(baseIssuesLoader, componentsWithUnprocessedIssues, issueCache, issueLifecycle)));
  }

  @Test
  public void close_issue() throws Exception {
    String fileUuid = "FILE1";
    String issueUuid = "ABCD";

    when(componentsWithUnprocessedIssues.getUuids()).thenReturn(newHashSet(fileUuid));
    DefaultIssue issue = new DefaultIssue().setKey(issueUuid);
    when(baseIssuesLoader.loadForComponentUuid(fileUuid)).thenReturn(Collections.singletonList(issue));

    underTest.visit(ReportComponent.builder(PROJECT, 1).build());

    verify(issueLifecycle).doAutomaticTransition(issue);
    CloseableIterator<DefaultIssue> issues = issueCache.traverse();
    assertThat(issues.hasNext()).isTrue();

    DefaultIssue result = issues.next();
    assertThat(result.key()).isEqualTo(issueUuid);
    assertThat(result.isBeingClosed()).isTrue();
    assertThat(result.isOnDisabledRule()).isFalse();
  }

  @Test
  public void nothing_to_do_when_no_uuid_in_queue() throws Exception {
    when(componentsWithUnprocessedIssues.getUuids()).thenReturn(Collections.<String>emptySet());

    underTest.visit(ReportComponent.builder(PROJECT, 1).build());

    verifyZeroInteractions(issueLifecycle);
    CloseableIterator<DefaultIssue> issues = issueCache.traverse();
    assertThat(issues.hasNext()).isFalse();
  }

  @Test
  public void do_nothing_on_module() throws Exception {
    underTest.visit(ReportComponent.builder(MODULE, 1).build());

    verifyZeroInteractions(issueLifecycle);
    CloseableIterator<DefaultIssue> issues = issueCache.traverse();
    assertThat(issues.hasNext()).isFalse();
  }

  @Test
  public void do_nothing_on_directory() throws Exception {
    underTest.visit(ReportComponent.builder(DIRECTORY, 1).build());

    verifyZeroInteractions(issueLifecycle);
    CloseableIterator<DefaultIssue> issues = issueCache.traverse();
    assertThat(issues.hasNext()).isFalse();
  }

  @Test
  public void do_nothing_on_file() throws Exception {
    underTest.visit(ReportComponent.builder(FILE, 1).build());

    verifyZeroInteractions(issueLifecycle);
    CloseableIterator<DefaultIssue> issues = issueCache.traverse();
    assertThat(issues.hasNext()).isFalse();
  }
}
