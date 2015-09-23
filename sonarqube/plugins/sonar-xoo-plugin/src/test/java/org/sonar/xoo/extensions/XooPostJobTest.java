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
package org.sonar.xoo.extensions;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.batch.postjob.PostJobContext;
import org.sonar.api.batch.postjob.internal.DefaultPostJobDescriptor;
import org.sonar.api.batch.postjob.issue.Issue;
import org.sonar.api.utils.log.LogTester;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class XooPostJobTest {

  @Rule
  public LogTester logTester = new LogTester();

  @Test
  public void increaseCoverage() {
    new XooPostJob().describe(new DefaultPostJobDescriptor());
    PostJobContext context = mock(PostJobContext.class);
    when(context.issues()).thenReturn(Arrays.<Issue>asList());
    when(context.resolvedIssues()).thenReturn(Arrays.<Issue>asList());
    new XooPostJob().execute(context);
    assertThat(logTester.logs()).contains("Resolved issues: 0", "Open issues: 0");
  }
}
