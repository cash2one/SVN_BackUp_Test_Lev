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

package org.sonar.db.component;

import java.util.List;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

public class ComponentLinkDao implements Dao {

  public List<ComponentLinkDto> selectByComponentUuid(DbSession session, String componentUuid) {
    return session.getMapper(ComponentLinkMapper.class).selectByComponentUuid(componentUuid);
  }

  public void insert(DbSession session, ComponentLinkDto dto) {
    session.getMapper(ComponentLinkMapper.class).insert(dto);
  }

  public void update(DbSession session, ComponentLinkDto dto) {
    session.getMapper(ComponentLinkMapper.class).update(dto);
  }

  public void delete(DbSession session, long id) {
    session.getMapper(ComponentLinkMapper.class).delete(id);
  }

}
