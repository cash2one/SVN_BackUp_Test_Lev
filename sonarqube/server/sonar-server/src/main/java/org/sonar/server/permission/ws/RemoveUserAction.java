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

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.permission.PermissionChange;
import org.sonar.server.permission.PermissionUpdater;
import org.sonar.server.permission.ws.PermissionRequest.Builder;

import static org.sonar.server.permission.ws.Parameters.createPermissionParameter;
import static org.sonar.server.permission.ws.Parameters.createProjectParameter;
import static org.sonar.server.permission.ws.Parameters.createUserLoginParameter;

public class RemoveUserAction implements PermissionsWsAction {

  public static final String ACTION = "remove_user";

  private final DbClient dbClient;
  private final PermissionUpdater permissionUpdater;
  private final PermissionChangeBuilder permissionChangeBuilder;

  public RemoveUserAction(DbClient dbClient, PermissionUpdater permissionUpdater, PermissionChangeBuilder permissionChangeBuilder) {
    this.dbClient = dbClient;
    this.permissionUpdater = permissionUpdater;
    this.permissionChangeBuilder = permissionChangeBuilder;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION)
      .setDescription("Remove permission from a user.<br /> " +
        "This service defaults to global permissions, but can be limited to project permissions by providing project id or project key.<br /> " +
        "It requires administration permissions to access.")
      .setSince("5.2")
      .setPost(true)
      .setHandler(this);

    createPermissionParameter(action);
    createUserLoginParameter(action);
    createProjectParameter(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    DbSession dbSession = dbClient.openSession(false);
    try {
      PermissionRequest permissionRequest = new Builder(request).withUser().build();
      PermissionChange permissionChange = permissionChangeBuilder.buildUserPermissionChange(dbSession, permissionRequest);
      permissionUpdater.removePermission(permissionChange);

      response.noContent();
    } finally {
      dbClient.closeSession(dbSession);
    }
  }
}
