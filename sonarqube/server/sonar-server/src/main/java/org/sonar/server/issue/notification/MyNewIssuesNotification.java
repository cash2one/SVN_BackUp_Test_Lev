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

import org.sonar.api.utils.Durations;
import org.sonar.db.DbClient;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.user.index.UserIndex;

import static org.sonar.server.issue.notification.AbstractNewIssuesEmailTemplate.FIELD_ASSIGNEE;

public class MyNewIssuesNotification extends NewIssuesNotification {

  public static final String MY_NEW_ISSUES_NOTIF_TYPE = "my-new-issues";

  MyNewIssuesNotification(UserIndex userIndex, RuleIndex ruleIndex, DbClient dbClient, Durations durations) {
    super(MY_NEW_ISSUES_NOTIF_TYPE, userIndex, ruleIndex, dbClient, durations);
  }

  public MyNewIssuesNotification setAssignee(String assignee) {
    setFieldValue(FIELD_ASSIGNEE, assignee);

    return this;
  }

  @Override
  public boolean equals(Object obj) {
    return super.equals(obj);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
