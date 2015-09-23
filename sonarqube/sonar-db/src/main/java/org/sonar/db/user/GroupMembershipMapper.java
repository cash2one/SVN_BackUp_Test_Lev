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
package org.sonar.db.user;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;

public interface GroupMembershipMapper {

  List<GroupMembershipDto> selectGroups(GroupMembershipQuery query);

  List<GroupMembershipDto> selectGroups(Map<String, Object> params, RowBounds rowBounds);

  int countGroups(Map<String, Object> params);

  List<UserMembershipDto> selectMembers(Map<String, Object> params, RowBounds rowBounds);

  int countMembers(Map<String, Object> params);

  List<GroupUserCount> countUsersByGroup(@Param("groupIds") List<Long> groupIds);

  List<LoginGroup> selectGroupsByLogins(@Param("logins") List<String> logins);
}
