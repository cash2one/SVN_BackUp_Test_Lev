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

package org.sonar.server.component.ws;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbTester;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;
import org.sonar.test.DbTests;

import static org.mockito.Mockito.mock;

@Category(DbTests.class)
public class SearchActionTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  WsTester tester;

  @Before
  public void setUp() {
    dbTester.truncateTables();
    tester = new WsTester(new ComponentsWs(mock(AppAction.class), new SearchAction(dbTester.getDbClient(), userSessionRule, new ComponentFinder(dbTester.getDbClient()))));
  }

  @Test
  public void return_projects_from_view() throws Exception {
    dbTester.prepareDbUnit(getClass(), "shared.xml");
    userSessionRule.login("john").addProjectUuidPermissions(UserRole.USER, "EFGH");

    WsTester.TestRequest request = tester.newGetRequest("api/components", "search").setParam("componentUuid", "EFGH").setParam("q", "st");
    request.execute().assertJson(getClass(), "return_projects_from_view.json");
  }

  @Test
  public void return_projects_from_subview() throws Exception {
    dbTester.prepareDbUnit(getClass(), "shared.xml");
    userSessionRule.login("john").addComponentUuidPermission(UserRole.USER, "EFGH", "FGHI");

    WsTester.TestRequest request = tester.newGetRequest("api/components", "search").setParam("componentUuid", "FGHI").setParam("q", "st");
    request.execute().assertJson(getClass(), "return_projects_from_subview.json");
  }

  @Test
  public void return_only_authorized_projects_from_view() throws Exception {
    dbTester.prepareDbUnit(getClass(), "return_only_authorized_projects_from_view.xml");
    userSessionRule.login("john").addProjectUuidPermissions(UserRole.USER, "EFGH");

    WsTester.TestRequest request = tester.newGetRequest("api/components", "search").setParam("componentUuid", "EFGH").setParam("q", "st");
    request.execute().assertJson(getClass(), "return_only_authorized_projects_from_view.json");
  }

  @Test
  public void return_paged_result() throws Exception {
    dbTester.prepareDbUnit(getClass(), "shared.xml");
    userSessionRule.login("john").addProjectUuidPermissions(UserRole.USER, "EFGH");

    WsTester.TestRequest request = tester.newGetRequest("api/components", "search").setParam("componentUuid", "EFGH").setParam("q", "st").setParam(Param.PAGE, "2")
      .setParam(Param.PAGE_SIZE, "1");
    request.execute().assertJson(getClass(), "return_paged_result.json");
  }

  @Test
  public void return_only_first_page() throws Exception {
    dbTester.prepareDbUnit(getClass(), "shared.xml");
    userSessionRule.login("john").addProjectUuidPermissions(UserRole.USER, "EFGH");

    WsTester.TestRequest request = tester.newGetRequest("api/components", "search").setParam("componentUuid", "EFGH").setParam("q", "st").setParam(Param.PAGE, "1")
      .setParam(Param.PAGE_SIZE, "1");
    request.execute().assertJson(getClass(), "return_only_first_page.json");
  }

  @Test
  public void fail_when_search_param_is_too_short() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Minimum search is 2 characters");


    dbTester.prepareDbUnit(getClass(), "shared.xml");
    userSessionRule.login("john").addProjectUuidPermissions(UserRole.USER, "EFGH");

    WsTester.TestRequest request = tester.newGetRequest("api/components", "search").setParam("componentUuid", "EFGH").setParam("q", "s");
    request.execute();
  }

  @Test
  public void fail_when_project_uuid_does_not_exists() throws Exception {
    thrown.expect(NotFoundException.class);
    thrown.expectMessage("Component id 'UNKNOWN' not found");

    dbTester.prepareDbUnit(getClass(), "shared.xml");
    userSessionRule.login("john").addProjectUuidPermissions(UserRole.USER, "EFGH");

    WsTester.TestRequest request = tester.newGetRequest("api/components", "search").setParam("componentUuid", "UNKNOWN").setParam("q", "st");
    request.execute();
  }
}
