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
package org.sonar.server.issue;

import java.util.Arrays;
import java.util.Locale;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.api.user.User;
import org.sonar.api.user.UserFinder;
import org.sonar.db.issue.IssueChangeDao;
import org.sonar.core.user.DefaultUser;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IssueChangelogServiceTest {

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  @Mock
  IssueChangeDao changeDao;

  @Mock
  UserFinder userFinder;

  @Mock
  IssueService issueService;

  @Mock
  IssueChangelogFormatter formatter;

  IssueChangelogService service;

  @Before
  public void setUp() {
    service = new IssueChangelogService(changeDao, userFinder, issueService, formatter, userSessionRule);
  }

  @Test
  public void load_changelog_and_related_users() {
    FieldDiffs userChange = new FieldDiffs().setUserLogin("arthur").setDiff("severity", "MAJOR", "BLOCKER");
    FieldDiffs scanChange = new FieldDiffs().setDiff("status", "RESOLVED", "CLOSED");
    when(changeDao.selectChangelogByIssue("ABCDE")).thenReturn(Arrays.asList(userChange, scanChange));
    User arthur = new DefaultUser().setLogin("arthur").setName("Arthur");
    when(userFinder.findByLogins(Arrays.asList("arthur"))).thenReturn(Arrays.asList(arthur));

    when(issueService.getByKey("ABCDE")).thenReturn(new DefaultIssue().setKey("ABCDE"));

    IssueChangelog changelog = service.changelog("ABCDE");

    assertThat(changelog).isNotNull();
    assertThat(changelog.changes()).containsOnly(userChange, scanChange);
    assertThat(changelog.user(scanChange)).isNull();
    assertThat(changelog.user(userChange)).isSameAs(arthur);
  }

  @Test
  public void format_diffs() {
    FieldDiffs diffs = new FieldDiffs().setUserLogin("arthur").setDiff("severity", "MAJOR", "BLOCKER");
    userSessionRule.login();
    service.formatDiffs(diffs);
    verify(formatter).format(any(Locale.class), eq(diffs));
  }
}
