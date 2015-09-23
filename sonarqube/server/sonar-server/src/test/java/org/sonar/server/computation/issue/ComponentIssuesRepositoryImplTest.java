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
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.server.computation.component.Component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.computation.component.ReportComponent.builder;

public class ComponentIssuesRepositoryImplTest {

  @org.junit.Rule
  public ExpectedException thrown = ExpectedException.none();

  static final Component FILE_1 = builder(Component.Type.FILE, 1).build();
  static final Component FILE_2 = builder(Component.Type.FILE, 2).build();

  static final DefaultIssue DUMB_ISSUE = new DefaultIssue().setKey("ISSUE");

  ComponentIssuesRepositoryImpl sut = new ComponentIssuesRepositoryImpl();

  @Test
  public void get_issues() throws Exception {
    sut.setIssues(FILE_1, Arrays.asList(DUMB_ISSUE));

    assertThat(sut.getIssues(FILE_1)).containsOnly(DUMB_ISSUE);
  }

  @Test
  public void set_empty_issues() throws Exception {
    sut.setIssues(FILE_1, Collections.<DefaultIssue>emptyList());

    assertThat(sut.getIssues(FILE_1)).isEmpty();
  }

  @Test
  public void fail_with_NPE_when_setting_issues_with_null_component() throws Exception {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("component cannot be null");

    sut.setIssues(null, Arrays.asList(DUMB_ISSUE));
  }

  @Test
  public void fail_with_NPE_when_setting_issues_with_null_issues() throws Exception {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("issues cannot be null");

    sut.setIssues(FILE_1, null);
  }

  @Test
  public void fail_with_IAE_when_getting_issues_on_different_component() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Only issues from component '1' are available, but wanted component is '2'.");

    sut.setIssues(FILE_1, Arrays.asList(DUMB_ISSUE));
    sut.getIssues(FILE_2);
  }

  @Test
  public void fail_with_ISE_when_getting_issues_but_issues_are_null() throws Exception {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Issues have not been initialized");

    sut.getIssues(FILE_1);
  }
}
