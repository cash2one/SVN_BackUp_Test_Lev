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

package org.sonar.server.permission;

import com.google.common.base.Optional;
import javax.annotation.Nullable;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.user.UserSession;

public class PermissionPrivilegeChecker {
  private PermissionPrivilegeChecker() {
    // static methods only
  }

  public static void checkGlobalAdminUser(UserSession userSession) {
    userSession
      .checkLoggedIn()
      .checkGlobalPermission(GlobalPermissions.SYSTEM_ADMIN);
  }

  public static void checkProjectAdminUserByComponentKey(UserSession userSession, @Nullable String componentKey) {
    userSession.checkLoggedIn();
    if (componentKey == null || !userSession.hasProjectPermission(UserRole.ADMIN, componentKey)) {
      userSession.checkGlobalPermission(GlobalPermissions.SYSTEM_ADMIN);
    }
  }

  public static void checkProjectAdminUserByComponentUuid(UserSession userSession, @Nullable String componentUuid) {
    userSession.checkLoggedIn();
    if (componentUuid == null || !userSession.hasProjectPermissionByUuid(UserRole.ADMIN, componentUuid)) {
      userSession.checkGlobalPermission(GlobalPermissions.SYSTEM_ADMIN);
    }
  }

  public static void checkProjectAdminUserByComponentDto(UserSession userSession, Optional<ComponentDto> project) {
    if (project.isPresent()) {
      checkProjectAdminUserByComponentUuid(userSession, project.get().uuid());
    } else {
      checkGlobalAdminUser(userSession);
    }
  }
}
