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
package org.sonar.server.computation;

import java.util.Calendar;
import org.sonar.api.platform.Server;
import org.sonar.api.platform.ServerStartHandler;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;

@ServerSide
public class PurgeCeActivities implements ServerStartHandler {

  private static final Logger LOGGER = Loggers.get(PurgeCeActivities.class);

  private final DbClient dbClient;
  private final System2 system2;

  public PurgeCeActivities(DbClient dbClient, System2 system2) {
    this.dbClient = dbClient;
    this.system2 = system2;
  }

  @Override
  public void onServerStart(Server server) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      Calendar sixMonthsAgo = Calendar.getInstance();
      sixMonthsAgo.setTimeInMillis(system2.now());
      sixMonthsAgo.add(Calendar.DATE, -180);

      LOGGER.info("Delete the Compute Engine tasks created before " + sixMonthsAgo.getTime());
      dbClient.ceActivityDao().deleteOlderThan(dbSession, sixMonthsAgo.getTimeInMillis());
      dbSession.commit();

    } finally {
      dbClient.closeSession(dbSession);
    }
  }
}
