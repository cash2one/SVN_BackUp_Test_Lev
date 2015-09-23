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

import java.util.List;
import org.sonar.core.permission.UserWithPermission;

public class UserWithPermissionQueryResult {

  private final List<UserWithPermission> users;
  private final int total;
  private final boolean hasMoreResults;

  public UserWithPermissionQueryResult(List<UserWithPermission> users, int total) {
    this.users = users;
    this.total = total;
    this.hasMoreResults = total > users.size();
  }

  public List<UserWithPermission> users() {
    return users;
  }

  public int total() {
    return total;
  }

  // called by Ruby Code
  public boolean hasMoreResults() {
    return hasMoreResults;
  }
}
