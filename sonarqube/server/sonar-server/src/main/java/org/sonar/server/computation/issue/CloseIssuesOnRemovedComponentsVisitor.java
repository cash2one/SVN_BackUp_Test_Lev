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

import java.util.List;
import java.util.Set;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.CrawlerDepthLimit;
import org.sonar.server.computation.component.TypeAwareVisitorAdapter;
import org.sonar.server.util.cache.DiskCache;

import static org.sonar.server.computation.component.ComponentVisitor.Order.POST_ORDER;

/**
 * Close issues on removed components
 */
public class CloseIssuesOnRemovedComponentsVisitor extends TypeAwareVisitorAdapter {

  private final BaseIssuesLoader baseIssuesLoader;
  private final ComponentsWithUnprocessedIssues componentsWithUnprocessedIssues;
  private final IssueCache issueCache;
  private final IssueLifecycle issueLifecycle;

  public CloseIssuesOnRemovedComponentsVisitor(BaseIssuesLoader baseIssuesLoader, ComponentsWithUnprocessedIssues componentsWithUnprocessedIssues, IssueCache issueCache,
    IssueLifecycle issueLifecycle) {
    super(CrawlerDepthLimit.PROJECT, POST_ORDER);
    this.baseIssuesLoader = baseIssuesLoader;
    this.componentsWithUnprocessedIssues = componentsWithUnprocessedIssues;
    this.issueCache = issueCache;
    this.issueLifecycle = issueLifecycle;
  }

  @Override
  public void visitProject(Component project) {
    closeIssuesForDeletedComponentUuids(componentsWithUnprocessedIssues.getUuids());
  }

  private void closeIssuesForDeletedComponentUuids(Set<String> deletedComponentUuids) {
    DiskCache<DefaultIssue>.DiskAppender cacheAppender = issueCache.newAppender();
    try {
      for (String deletedComponentUuid : deletedComponentUuids) {
        List<DefaultIssue> issues = baseIssuesLoader.loadForComponentUuid(deletedComponentUuid);
        for (DefaultIssue issue : issues) {
          issue.setBeingClosed(true);
          // TODO should be renamed
          issue.setOnDisabledRule(false);
          issueLifecycle.doAutomaticTransition(issue);
          cacheAppender.append(issue);
        }
      }
    } finally {
      cacheAppender.close();
    }
  }
}
