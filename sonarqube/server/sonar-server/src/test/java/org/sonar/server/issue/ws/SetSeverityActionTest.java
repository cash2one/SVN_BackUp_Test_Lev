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
package org.sonar.server.issue.ws;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.server.issue.IssueService;
import org.sonar.server.ws.WsAction;
import org.sonar.server.ws.WsActionTester;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SetSeverityActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  IssueService issueService = mock(IssueService.class);
  OperationResponseWriter responseWriter = mock(OperationResponseWriter.class);
  WsAction underTest = new SetSeverityAction(issueService, responseWriter);
  WsActionTester tester = new WsActionTester(underTest);

  @Test
  public void set_severity() throws Exception {
    tester.newRequest()
      .setParam("issue", "ABC")
      .setParam("severity", "BLOCKER")
      .execute();

    verify(issueService).setSeverity("ABC", "BLOCKER");
    verify(responseWriter).write(eq("ABC"), any(Request.class), any(Response.class));
  }

  @Test
  public void fail_if_bad_severity_value() {
    expectedException.expect(IllegalArgumentException.class);

    tester.newRequest()
      .setParam("issue", "ABC")
      .setParam("severity", "WAT")
      .execute();
  }
}
