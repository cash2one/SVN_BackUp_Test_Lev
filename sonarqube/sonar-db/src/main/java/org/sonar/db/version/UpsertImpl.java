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
package org.sonar.db.version;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.sonar.db.BatchSession;

public class UpsertImpl extends BaseSqlStatement<Upsert> implements Upsert {

  private long batchCount = 0L;

  private UpsertImpl(PreparedStatement pstmt) {
    super(pstmt);
  }

  @Override
  public Upsert addBatch() throws SQLException {
    pstmt.addBatch();
    pstmt.clearParameters();
    batchCount++;
    if (batchCount % BatchSession.MAX_BATCH_SIZE == 0L) {
      pstmt.executeBatch();
      pstmt.getConnection().commit();
    }
    return this;
  }

  @Override
  public Upsert execute() throws SQLException {
    if (batchCount == 0L) {
      pstmt.execute();
    } else {
      pstmt.executeBatch();
    }
    return this;
  }

  public long getBatchCount() {
    return batchCount;
  }

  @Override
  public Upsert commit() throws SQLException {
    pstmt.getConnection().commit();
    return this;
  }

  static UpsertImpl create(Connection connection, String sql) throws SQLException {
    return new UpsertImpl(connection.prepareStatement(sql));
  }
}
