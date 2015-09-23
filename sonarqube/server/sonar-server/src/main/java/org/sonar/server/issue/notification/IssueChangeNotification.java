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
package org.sonar.server.issue.notification;

import com.google.common.base.Strings;
import org.sonar.api.component.Component;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.api.notifications.Notification;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.io.Serializable;
import java.util.Map;

public class IssueChangeNotification extends Notification {

  public static final String TYPE = "issue-changes";

  public IssueChangeNotification() {
    super(TYPE);
  }

  public IssueChangeNotification setIssue(DefaultIssue issue) {
    setFieldValue("key", issue.key());
    setFieldValue("reporter", issue.reporter());
    setFieldValue("assignee", issue.assignee());
    setFieldValue("message", issue.message());
    setFieldValue("componentKey", issue.componentKey());
    FieldDiffs currentChange = issue.currentChange();
    if (currentChange != null) {
      for (Map.Entry<String, FieldDiffs.Diff> entry : currentChange.diffs().entrySet()) {
        String type = entry.getKey();
        FieldDiffs.Diff diff = entry.getValue();
        setFieldValue("old." + type, neverEmptySerializableToString(diff.oldValue()));
        setFieldValue("new." + type, neverEmptySerializableToString(diff.newValue()));
      }
    }
    return this;
  }

  public IssueChangeNotification setProject(Component project) {
    setFieldValue("projectName", project.longName());
    setFieldValue("projectKey", project.key());
    return this;
  }

  public IssueChangeNotification setProject(String projectKey, String projectName) {
    setFieldValue("projectName", projectName);
    setFieldValue("projectKey", projectKey);
    return this;
  }

  public IssueChangeNotification setComponent(Component component) {
    setFieldValue("componentName", component.longName());
    return this;
  }

  public IssueChangeNotification setChangeAuthorLogin(@Nullable String s) {
    if (s != null) {
      setFieldValue("changeAuthor", s);
    }
    return this;
  }

  public IssueChangeNotification setRuleName(@Nullable String s) {
    if (s != null) {
      setFieldValue("ruleName", s);
    }
    return this;
  }

  public IssueChangeNotification setComment(@Nullable String s) {
    if (s != null) {
      setFieldValue("comment", s);
    }
    return this;
  }

  @CheckForNull
  private static String neverEmptySerializableToString(@Nullable Serializable s) {
    return s != null ? Strings.emptyToNull(s.toString()) : null;
  }
}
