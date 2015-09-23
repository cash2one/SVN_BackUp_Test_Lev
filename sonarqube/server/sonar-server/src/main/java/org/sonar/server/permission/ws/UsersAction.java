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

package org.sonar.server.permission.ws;

import com.google.common.base.Optional;
import java.util.List;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.server.ws.WebService.SelectionMode;
import org.sonar.core.permission.UserWithPermission;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.permission.PermissionQuery;
import org.sonar.server.permission.PermissionFinder;
import org.sonar.server.permission.UserWithPermissionQueryResult;
import org.sonar.server.permission.ws.PermissionRequest.Builder;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Common.Paging;
import org.sonarqube.ws.WsPermissions.WsUsersResponse;

import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.base.Strings.nullToEmpty;
import static org.sonar.server.permission.PermissionPrivilegeChecker.checkProjectAdminUserByComponentDto;
import static org.sonar.server.permission.PermissionQueryParser.toMembership;
import static org.sonar.server.permission.ws.Parameters.createPermissionParameter;
import static org.sonar.server.permission.ws.Parameters.createProjectParameter;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class UsersAction implements PermissionsWsAction {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final PermissionFinder permissionFinder;
  private final PermissionDependenciesFinder dependenciesFinder;

  public UsersAction(DbClient dbClient, UserSession userSession, PermissionFinder permissionFinder, PermissionDependenciesFinder dependenciesFinder) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.permissionFinder = permissionFinder;
    this.dependenciesFinder = dependenciesFinder;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("users")
      .setSince("5.2")
      .setDescription(String.format("Lists the users that have been granted the specified permission as individual users rather than through group affiliation. <br />" +
        "This service defaults to global permissions, but can be limited to project permissions by providing project id or project key.<br /> " +
        "If the query parameter '%s' is specified, the '%s' parameter is forced to '%s'.<br />" +
        "It requires administration permissions to access.<br />",
        Param.TEXT_QUERY, Param.SELECTED, SelectionMode.ALL.value()))
      .addPagingParams(100)
      .addSearchQuery("stas", "names")
      .addSelectionModeParam()
      .setInternal(true)
      .setResponseExample(getClass().getResource("users-example.json"))
      .setHandler(this);

    createPermissionParameter(action);
    createProjectParameter(action);
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    DbSession dbSession = dbClient.openSession(false);
    try {
      PermissionRequest request = new Builder(wsRequest).withPagination().build();
      Optional<ComponentDto> project = dependenciesFinder.searchProject(dbSession, request);
      checkProjectAdminUserByComponentDto(userSession, project);
      PermissionQuery permissionQuery = buildPermissionQuery(request, project);
      WsUsersResponse wsUsersResponse = wsUsersResponse(permissionQuery, request.page(), request.pageSize());

      writeProtobuf(wsUsersResponse, wsRequest, wsResponse);
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private WsUsersResponse wsUsersResponse(PermissionQuery permissionQuery, int page, int pageSize) {
    UserWithPermissionQueryResult usersResult = permissionFinder.findUsersWithPermission(permissionQuery);
    List<UserWithPermission> usersWithPermission = usersResult.users();

    WsUsersResponse.Builder userResponse = WsUsersResponse.newBuilder();
    WsUsersResponse.User.Builder user = WsUsersResponse.User.newBuilder();
    for (UserWithPermission userWithPermission : usersWithPermission) {
      userResponse.addUsers(
        user
          .clear()
          .setLogin(userWithPermission.login())
          .setName(nullToEmpty(userWithPermission.name()))
          .setEmail(nullToEmpty(userWithPermission.email()))
          .setSelected(userWithPermission.hasPermission()));
    }
    userResponse.setPaging(
      Paging.newBuilder()
        .clear()
        .setPageIndex(page)
        .setPageSize(pageSize)
        .setTotal(usersResult.total())
      );

    return userResponse.build();
  }

  private static PermissionQuery buildPermissionQuery(PermissionRequest request, Optional<ComponentDto> project) {
    PermissionQuery.Builder permissionQuery = PermissionQuery.builder()
      .permission(request.permission())
      .pageIndex(request.page())
      .pageSize(request.pageSize())
      .membership(toMembership(firstNonNull(request.selected(), SelectionMode.SELECTED.value())))
      .search(request.query());
    if (project.isPresent()) {
      permissionQuery.component(project.get().getKey());
    }

    return permissionQuery.build();
  }
}
