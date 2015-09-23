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
package org.sonar.server.platform.platformlevel;

import org.sonar.server.platform.ws.DbMigrationStatusAction;
import org.sonar.server.platform.ws.MigrateDbAction;
import org.sonar.server.platform.ws.StatusAction;
import org.sonar.server.platform.ws.SystemWs;
import org.sonar.server.ws.ListingWs;
import org.sonar.server.ws.WebServiceEngine;

public class PlatformLevelSafeMode extends PlatformLevel {
  public PlatformLevelSafeMode(PlatformLevel parent) {
    super("Safemode", parent);
  }

  @Override
  protected void configureLevel() {
    add(
      // Server WS
      StatusAction.class,
      MigrateDbAction.class,
      DbMigrationStatusAction.class,
      SystemWs.class,

      // Listing WS
      ListingWs.class,

      // WS engine
      WebServiceEngine.class);
  }
}
