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

package org.sonar.server.source.ws;

import com.google.common.base.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDao;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.source.SourceService;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IndexActionTest {

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  @Mock
  DbClient dbClient;

  @Mock
  DbSession session;

  @Mock
  ComponentDao componentDao;

  @Mock
  SourceService sourceService;

  WsTester tester;

  ComponentDto project = ComponentTesting.newProjectDto();
  ComponentDto file = ComponentTesting.newFileDto(project);

  @Before
  public void setUp() {
    when(dbClient.componentDao()).thenReturn(componentDao);
    when(dbClient.openSession(false)).thenReturn(session);
    tester = new WsTester(new SourcesWs(new IndexAction(dbClient, sourceService, userSessionRule, new ComponentFinder(dbClient))));
  }

  @Test
  public void get_json() throws Exception {
    String fileKey = "src/Foo.java";
    userSessionRule.addComponentPermission(UserRole.CODEVIEWER, "polop", fileKey);
    when(componentDao.selectByKey(session, fileKey)).thenReturn(Optional.of(file));

    when(sourceService.getLinesAsRawText(session, file.uuid(), 1, Integer.MAX_VALUE)).thenReturn(Optional.of((Iterable<String>) newArrayList(
      "public class HelloWorld {",
      "}")));

    WsTester.TestRequest request = tester.newGetRequest("api/sources", "index").setParam("resource", fileKey);
    request.execute().assertJson(this.getClass(), "index-result.json");
  }

  @Test
  public void limit_range() throws Exception {
    String fileKey = "src/Foo.java";
    userSessionRule.addComponentPermission(UserRole.CODEVIEWER, "polop", fileKey);
    when(componentDao.selectByKey(session, fileKey)).thenReturn(Optional.of(file));

    when(sourceService.getLinesAsRawText(session, file.uuid(), 1, 2)).thenReturn(Optional.of((Iterable<String>) newArrayList(
      "public class HelloWorld {",
      "}")));

    WsTester.TestRequest request = tester.newGetRequest("api/sources", "index")
      .setParam("resource", fileKey).setParam("from", "1").setParam("to", "3");
    request.execute().assertJson(this.getClass(), "index-result.json");
  }

  @Test(expected = ForbiddenException.class)
  public void requires_code_viewer_permission() throws Exception {
    tester.newGetRequest("api/sources", "index").setParam("resource", "any").execute();
  }

  @Test
  public void close_db_session() throws Exception {
    String fileKey = "src/Foo.java";
    userSessionRule.addComponentPermission(UserRole.CODEVIEWER, "polop", fileKey);
    when(componentDao.selectByKey(session, fileKey)).thenReturn(Optional.<ComponentDto>absent());

    WsTester.TestRequest request = tester.newGetRequest("api/sources", "index").setParam("resource", fileKey);
    try {
      request.execute();
    } catch (NotFoundException nfe) {
      verify(session).close();
    }
  }
}
