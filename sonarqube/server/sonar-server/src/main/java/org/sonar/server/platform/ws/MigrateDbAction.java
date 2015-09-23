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
package org.sonar.server.platform.ws;

import com.google.common.io.Resources;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.Database;
import org.sonar.db.version.DatabaseMigration;
import org.sonar.db.version.DatabaseVersion;

import static org.sonar.server.platform.ws.DbMigrationJsonWriter.UNSUPPORTED_DATABASE_MIGRATION_STATUS;
import static org.sonar.server.platform.ws.DbMigrationJsonWriter.statusDescription;
import static org.sonar.server.platform.ws.DbMigrationJsonWriter.write;
import static org.sonar.server.platform.ws.DbMigrationJsonWriter.writeJustStartedResponse;
import static org.sonar.server.platform.ws.DbMigrationJsonWriter.writeNotSupportedResponse;

/**
 * Implementation of the {@code migrate_db} action for the System WebService.
 */
public class MigrateDbAction implements SystemWsAction {

  private final DatabaseVersion databaseVersion;
  private final DatabaseMigration databaseMigration;
  private final Database database;

  public MigrateDbAction(DatabaseVersion databaseVersion, Database database, DatabaseMigration databaseMigration) {
    this.databaseVersion = databaseVersion;
    this.database = database;
    this.databaseMigration = databaseMigration;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("migrate_db")
      .setDescription("Migrate the database to match the current version of SonarQube." +
        "<br/>" +
        "Sending a POST request to this URL starts the DB migration. " +
        "It is strongly advised to <strong>make a database backup</strong> before invoking this WS." +
        "<br/>" +
        statusDescription())
      .setSince("5.2")
      .setPost(true)
      .setHandler(this)
      .setResponseExample(Resources.getResource(this.getClass(), "example-migrate_db.json"));
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    Integer currentVersion = databaseVersion.getVersion();
    if (currentVersion == null) {
      throw new IllegalStateException("Version can not be retrieved from Database. Database is either blank or corrupted");
    }

    JsonWriter json = response.newJsonWriter();
    try {
      if (currentVersion >= DatabaseVersion.LAST_VERSION) {
        write(json, databaseMigration);
      } else if (!database.getDialect().supportsMigration()) {
        writeNotSupportedResponse(json);
      } else {
        switch (databaseMigration.status()) {
          case RUNNING:
          case FAILED:
          case SUCCEEDED:
            write(json, databaseMigration);
            break;
          case NONE:
            databaseMigration.startIt();
            writeJustStartedResponse(json, databaseMigration);
            break;
          default:
            throw new IllegalArgumentException(UNSUPPORTED_DATABASE_MIGRATION_STATUS);
        }
      }
    } finally {
      json.close();
    }
  }
}
