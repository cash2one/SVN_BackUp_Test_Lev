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
package org.sonar.db.dashboard;

import java.util.List;
import javax.annotation.Nullable;
import org.apache.ibatis.session.SqlSession;
import org.sonar.db.Dao;
import org.sonar.db.MyBatis;

public class ActiveDashboardDao implements Dao {

  private MyBatis mybatis;

  public ActiveDashboardDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  public void insert(ActiveDashboardDto activeDashboardDto) {
    SqlSession session = mybatis.openSession(false);
    try {
      mapper(session).insert(activeDashboardDto);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public int selectMaxOrderIndexForNullUser() {
    SqlSession session = mybatis.openSession(false);
    try {
      Integer max = mapper(session).selectMaxOrderIndexForNullUser();
      return max != null ? max.intValue() : 0;
    } finally {
      session.close();
    }

  }

  public List<DashboardDto> selectGlobalDashboardsForUserLogin(@Nullable String login) {
    SqlSession session = mybatis.openSession(false);
    try {
      return mapper(session).selectGlobalDashboardsForUserLogin(login);
    } finally {
      session.close();
    }
  }

  public List<DashboardDto> selectProjectDashboardsForUserLogin(@Nullable String login) {
    SqlSession session = mybatis.openSession(false);
    try {
      return selectProjectDashboardsForUserLogin(session, login);
    } finally {
      session.close();
    }
  }

  public List<DashboardDto> selectProjectDashboardsForUserLogin(SqlSession session, @Nullable String login) {
    return mapper(session).selectProjectDashboardsForUserLogin(login);
  }

  private ActiveDashboardMapper mapper(SqlSession session) {
    return session.getMapper(ActiveDashboardMapper.class);
  }
}
