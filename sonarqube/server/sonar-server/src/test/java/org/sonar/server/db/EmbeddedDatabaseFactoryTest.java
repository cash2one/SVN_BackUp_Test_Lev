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

package org.sonar.server.db;

import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.api.database.DatabaseProperties;

import static org.mockito.Mockito.*;

public class EmbeddedDatabaseFactoryTest {

  Settings settings = new Settings();

  @Test
  public void should_start_and_stop_tcp_h2_database() {
    settings.setProperty(DatabaseProperties.PROP_URL, "jdbc:h2:tcp:localhost");

    final EmbeddedDatabase embeddedDatabase = mock(EmbeddedDatabase.class);

    EmbeddedDatabaseFactory databaseFactory = new EmbeddedDatabaseFactory(settings) {
      @Override
      EmbeddedDatabase getEmbeddedDatabase(Settings settings) {
        return embeddedDatabase;
      }
    };
    databaseFactory.start();
    databaseFactory.stop();

    verify(embeddedDatabase).start();
    verify(embeddedDatabase).stop();
  }

  @Test
  public void should_not_start_mem_h2_database() {
    settings.setProperty(DatabaseProperties.PROP_URL, "jdbc:h2:mem");

    final EmbeddedDatabase embeddedDatabase = mock(EmbeddedDatabase.class);

    EmbeddedDatabaseFactory databaseFactory = new EmbeddedDatabaseFactory(settings) {
      @Override
      EmbeddedDatabase getEmbeddedDatabase(Settings settings) {
        return embeddedDatabase;
      }
    };
    databaseFactory.start();

    verify(embeddedDatabase, never()).start();
  }
}
