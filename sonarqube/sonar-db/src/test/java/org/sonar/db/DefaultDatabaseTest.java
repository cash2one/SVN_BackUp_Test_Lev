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
package org.sonar.db;

import java.util.Properties;
import org.apache.commons.dbcp.BasicDataSource;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.db.dialect.PostgreSql;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultDatabaseTest {

  @Test
  public void shouldLoadDefaultValues() {
    DefaultDatabase db = new DefaultDatabase(new Settings());
    db.initSettings();

    Properties props = db.getProperties();
    assertThat(props.getProperty("sonar.jdbc.username")).isEqualTo("sonar");
    assertThat(props.getProperty("sonar.jdbc.password")).isEqualTo("sonar");
    assertThat(props.getProperty("sonar.jdbc.url")).isEqualTo("jdbc:h2:tcp://localhost/sonar");
    assertThat(props.getProperty("sonar.jdbc.driverClassName")).isEqualTo("org.h2.Driver");
    assertThat(db.toString()).isEqualTo("Database[jdbc:h2:tcp://localhost/sonar]");
  }

  @Test
  public void shouldSupportDeprecatedUserProperty() {
    Settings settings = new Settings();
    settings.setProperty("sonar.jdbc.user", "me");

    DefaultDatabase db = new DefaultDatabase(settings);
    db.initSettings();
    Properties props = db.getProperties();

    assertThat(props.getProperty("sonar.jdbc.username")).isEqualTo("me");
  }

  @Test
  public void shouldExtractCommonsDbcpProperties() {
    Properties props = new Properties();
    props.setProperty("sonar.jdbc.driverClassName", "my.Driver");
    props.setProperty("sonar.jdbc.username", "me");
    props.setProperty("sonar.jdbc.maxActive", "5");

    Properties commonsDbcpProps = DefaultDatabase.extractCommonsDbcpProperties(props);

    assertThat(commonsDbcpProps.getProperty("username")).isEqualTo("me");
    assertThat(commonsDbcpProps.getProperty("driverClassName")).isEqualTo("my.Driver");
    assertThat(commonsDbcpProps.getProperty("maxActive")).isEqualTo("5");
  }

  @Test
  public void shouldCompleteProperties() {
    Settings settings = new Settings();

    DefaultDatabase db = new DefaultDatabase(settings) {
      @Override
      protected void doCompleteProperties(Properties properties) {
        properties.setProperty("sonar.jdbc.maxActive", "2");
      }
    };
    db.initSettings();

    Properties props = db.getProperties();

    assertThat(props.getProperty("sonar.jdbc.maxActive")).isEqualTo("2");
  }

  @Test
  public void shouldStart() {
    Settings settings = new Settings();
    settings.setProperty("sonar.jdbc.url", "jdbc:h2:mem:sonar");
    settings.setProperty("sonar.jdbc.driverClassName", "org.h2.Driver");
    settings.setProperty("sonar.jdbc.username", "sonar");
    settings.setProperty("sonar.jdbc.password", "sonar");
    settings.setProperty("sonar.jdbc.maxActive", "1");

    DefaultDatabase db = new DefaultDatabase(settings);
    db.start();
    db.stop();

    assertThat(db.getDialect().getId()).isEqualTo("h2");
    assertThat(((BasicDataSource) db.getDataSource()).getMaxActive()).isEqualTo(1);
  }

  @Test
  public void shouldGuessDialectFromUrl() {
    Settings settings = new Settings();
    settings.setProperty("sonar.jdbc.url", "jdbc:postgresql://localhost/sonar");

    DefaultDatabase database = new DefaultDatabase(settings);
    database.initSettings();

    assertThat(database.getDialect().getId()).isEqualTo(PostgreSql.ID);
  }

  @Test
  public void shouldGuessDefaultDriver() {
    Settings settings = new Settings();
    settings.setProperty("sonar.jdbc.url", "jdbc:postgresql://localhost/sonar");

    DefaultDatabase database = new DefaultDatabase(settings);
    database.initSettings();

    assertThat(database.getProperties().getProperty("sonar.jdbc.driverClassName")).isEqualTo("org.postgresql.Driver");
  }
}
