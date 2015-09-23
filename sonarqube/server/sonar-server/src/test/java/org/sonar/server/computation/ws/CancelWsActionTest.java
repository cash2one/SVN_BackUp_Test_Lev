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
package org.sonar.server.computation.ws;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbTester;
import org.sonar.server.computation.CeQueue;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class CancelWsActionTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  CeQueue queue = mock(CeQueue.class);
  CancelWsAction underTest = new CancelWsAction(userSession, queue);
  WsActionTester tester = new WsActionTester(underTest);

  @Test
  public void cancel_all_pending_tasks() {
    userSession.setGlobalPermissions(UserRole.ADMIN);

    tester.newRequest()
      .setParam("all", "true")
      .execute();

    verify(queue).cancelAll();
  }

  @Test
  public void cancel_pending_task() {
    userSession.setGlobalPermissions(UserRole.ADMIN);

    tester.newRequest()
      .setParam("id", "T1")
      .execute();

    verify(queue).cancel("T1");
  }

  @Test(expected = BadRequestException.class)
  public void missing_parameters() {
    userSession.setGlobalPermissions(UserRole.ADMIN);

    tester.newRequest().execute();

    verifyZeroInteractions(queue);
  }

  @Test(expected = ForbiddenException.class)
  public void not_authorized() {
    tester.newRequest()
      .setParam("id", "T1")
      .execute();

    verifyZeroInteractions(queue);
  }
}
