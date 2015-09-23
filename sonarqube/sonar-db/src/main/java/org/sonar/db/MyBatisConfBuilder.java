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

import ch.qos.logback.classic.Level;
import com.google.common.io.Closeables;
import java.io.InputStream;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.type.JdbcType;
import org.slf4j.LoggerFactory;
import org.sonar.db.dialect.Dialect;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;

public final class MyBatisConfBuilder {
  private final Configuration conf;

  public MyBatisConfBuilder(Database database) {
    this.conf = new Configuration();
    this.conf.setEnvironment(new Environment("production", createTransactionFactory(), database.getDataSource()));
    this.conf.setUseGeneratedKeys(true);
    this.conf.setLazyLoadingEnabled(false);
    this.conf.setJdbcTypeForNull(JdbcType.NULL);
    Dialect dialect = database.getDialect();
    this.conf.setDatabaseId(dialect.getId());
    this.conf.getVariables().setProperty("_true", dialect.getTrueSqlValue());
    this.conf.getVariables().setProperty("_false", dialect.getFalseSqlValue());
    this.conf.getVariables().setProperty("_scrollFetchSize", String.valueOf(dialect.getScrollDefaultFetchSize()));
  }

  public void loadAlias(String alias, Class dtoClass) {
    conf.getTypeAliasRegistry().registerAlias(alias, dtoClass);
  }

  public void loadMapper(String mapperName) {
    String configFile = configFilePath(mapperName);
    InputStream input = null;
    try {
      input = getClass().getResourceAsStream(configFile);
      checkArgument(input != null, format("Can not find mapper XML file %s", configFile));
      new XMLMapperBuilder(input, conf, mapperName, conf.getSqlFragments()).parse();
      loadAndConfigureLogger(mapperName);
    } catch (Exception e) {
      throw new IllegalArgumentException("Unable to load mapper " + mapperName, e);
    } finally {
      Closeables.closeQuietly(input);
    }
  }

  public void loadMapper(Class mapperClass) {
    String configFile = configFilePath(mapperClass);
    InputStream input = null;
    try {
      input = mapperClass.getResourceAsStream(configFile);
      checkArgument(input != null, format("Can not find mapper XML file %s", configFile));
      new SQXMLMapperBuilder(mapperClass, input, conf, conf.getSqlFragments()).parse();
      loadAndConfigureLogger(mapperClass.getName());
    } catch (Exception e) {
      throw new IllegalArgumentException("Unable to load mapper " + mapperClass, e);
    } finally {
      Closeables.closeQuietly(input);
    }
  }

  private static String configFilePath(Class mapperClass) {
    return configFilePath(mapperClass.getName());
  }

  private static String configFilePath(String mapperName) {
    return "/" + mapperName.replace('.', '/') + ".xml";
  }

  private void loadAndConfigureLogger(String mapperName) {
    conf.addLoadedResource(mapperName);
    ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(mapperName)).setLevel(Level.INFO);
  }

  public void loadMappers(Class<?>... mapperClasses) {
    for (Class mapperClass : mapperClasses) {
      loadMapper(mapperClass);
    }
  }

  public Configuration build() {
    return conf;
  }

  private static JdbcTransactionFactory createTransactionFactory() {
    return new JdbcTransactionFactory();
  }
}
