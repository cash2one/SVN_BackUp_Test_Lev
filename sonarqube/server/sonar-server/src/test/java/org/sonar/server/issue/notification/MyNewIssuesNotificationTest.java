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

import org.junit.Test;
import org.sonar.api.utils.Durations;
import org.sonar.db.DbClient;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.user.index.UserIndex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.server.issue.notification.AbstractNewIssuesEmailTemplate.FIELD_ASSIGNEE;

public class MyNewIssuesNotificationTest {

  MyNewIssuesNotification underTest = new MyNewIssuesNotification(mock(UserIndex.class), mock(RuleIndex.class), mock(DbClient.class), mock(Durations.class));

  @Test
  public void set_assignee() {
    underTest.setAssignee("myAssignee");

    assertThat(underTest.getFieldValue(FIELD_ASSIGNEE)).isEqualTo("myAssignee");
  }

  @Test
  public void set_with_a_specific_type() {
    assertThat(underTest.getType()).isEqualTo(MyNewIssuesNotification.MY_NEW_ISSUES_NOTIF_TYPE);

  }
}
