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

import com.google.common.collect.Maps;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import javax.annotation.Nullable;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.dbunit.DataSourceDatabaseTester;
import org.dbunit.IDatabaseTester;
import org.dbunit.dataset.datatype.IDataTypeFactory;
import org.junit.AssumptionViolatedException;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.deprecated.NullQueue;
import org.sonar.db.dialect.H2;

/**
 * This class should be call using @ClassRule in order to create the schema once (ft @Rule is used
 * the schema will be recreated before each test).
 * Data will be truncated each time you call prepareDbUnit().
 * <p/>
 * File using {@link TestDb} must be annotated with {@link org.sonar.test.DbTests} so
 * that they can be executed on all supported DBs (Oracle, MySQL, ...).
 */
class TestDb {

  private static TestDb DEFAULT;

  private static final Logger LOG = Loggers.get(TestDb.class);

  private Database db;
  private DatabaseCommands commands;
  private IDatabaseTester tester;
  private MyBatis myBatis;
  private boolean isDefault;

  static TestDb create(@Nullable String schemaPath) {
    if (schemaPath == null) {
      if (DEFAULT == null) {
        DEFAULT = new TestDb(null);
      }
      return DEFAULT;
    }
    return new TestDb(schemaPath);
  }

  private TestDb(@Nullable String schemaPath) {
    if (db == null) {
      Settings settings = new Settings().setProperties(Maps.fromProperties(System.getProperties()));
      if (settings.hasKey("orchestrator.configUrl")) {
        loadOrchestratorSettings(settings);
      }
      String login = settings.getString("sonar.jdbc.username");
      for (String key : settings.getKeysStartingWith("sonar.jdbc")) {
        LOG.info(key + ": " + settings.getString(key));
      }
      String dialect = settings.getString("sonar.jdbc.dialect");
      if (dialect != null && !"h2".equals(dialect)) {
        db = new DefaultDatabase(settings);
      } else {
        db = new H2Database("h2Tests" + DigestUtils.md5Hex(StringUtils.defaultString(schemaPath)), schemaPath == null);
      }
      db.start();
      if (schemaPath != null) {
        // will fail if not H2
        if (db.getDialect().getId().equals("h2")) {
          ((H2Database) db).executeScript(schemaPath);
        } else {
          db.stop();

        }
      }
      isDefault = (schemaPath == null);
      LOG.info("Test Database: " + db);

      commands = DatabaseCommands.forDialect(db.getDialect());
      tester = new DataSourceDatabaseTester(db.getDataSource(), commands.useLoginAsSchema() ? login : null);

      myBatis = new MyBatis(db, new NullQueue());
      myBatis.start();
    }
  }

  public void start() {
    if (!isDefault && !H2.ID.equals(db.getDialect().getId())) {
      throw new AssumptionViolatedException("Test disabled because it supports only H2");
    }
  }

  void stop() {
    if (!isDefault) {
      db.stop();
    }
  }

  void truncateTables() {
    try {
      commands.truncateDatabase(db.getDataSource());
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to truncate db tables", e);
    }
  }

  Database getDatabase() {
    return db;
  }

  DatabaseCommands getCommands() {
    return commands;
  }

  MyBatis getMyBatis() {
    return myBatis;
  }

  IDatabaseTester getDbUnitTester() {
    return tester;
  }

  IDataTypeFactory getDbUnitFactory() {
    return commands.getDbUnitFactory();
  }

  private void loadOrchestratorSettings(Settings settings) {
    String url = settings.getString("orchestrator.configUrl");
    InputStream input = null;
    try {
      URI uri = new URI(url);
      if (url.startsWith("file:")) {
        File file = new File(uri);
        input = FileUtils.openInputStream(file);
      } else {
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        int responseCode = connection.getResponseCode();
        if (responseCode >= 400) {
          throw new IllegalStateException("Fail to request: " + uri + ". Status code=" + responseCode);
        }

        input = connection.getInputStream();

      }
      Properties props = new Properties();
      props.load(input);
      settings.addProperties(props);
      for (Map.Entry<String, String> entry : settings.getProperties().entrySet()) {
        String interpolatedValue = StrSubstitutor.replace(entry.getValue(), System.getenv(), "${", "}");
        settings.setProperty(entry.getKey(), interpolatedValue);
      }
    } catch (Exception e) {
      throw new IllegalStateException(e);
    } finally {
      IOUtils.closeQuietly(input);
    }
  }


}
