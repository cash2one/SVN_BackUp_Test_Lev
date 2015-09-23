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

package org.sonar.db.purge.period;

import com.google.common.collect.Lists;
import java.util.Date;
import java.util.List;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.purge.PurgeableSnapshotDto;

class DeleteAllFilter implements Filter {
  private final Date before;

  public DeleteAllFilter(Date before) {
    this.before = before;
  }

  @Override
  public List<PurgeableSnapshotDto> filter(List<PurgeableSnapshotDto> history) {
    List<PurgeableSnapshotDto> result = Lists.newArrayList();
    for (PurgeableSnapshotDto snapshot : history) {
      if (snapshot.getDate().before(before)) {
        result.add(snapshot);
      }
    }
    return result;
  }

  @Override
  public void log() {
    Loggers.get(getClass()).debug("-> Delete data prior to: {}", DateUtils.formatDate(before));
  }
}
