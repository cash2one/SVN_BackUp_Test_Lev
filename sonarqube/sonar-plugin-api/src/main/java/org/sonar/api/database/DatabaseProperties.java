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
package org.sonar.api.database;

public interface DatabaseProperties {

  int MAX_TEXT_SIZE = 16777215;

  String PROP_URL = "sonar.jdbc.url";
  String PROP_DRIVER = "sonar.jdbc.driverClassName";
  String PROP_USER = "sonar.jdbc.username";
  String PROP_USER_DEPRECATED = "sonar.jdbc.user";
  String PROP_USER_DEFAULT_VALUE = "sonar";
  String PROP_PASSWORD = "sonar.jdbc.password";
  String PROP_PASSWORD_DEFAULT_VALUE = "sonar";
  String PROP_DIALECT = "sonar.jdbc.dialect";

  /**
   * @since 3.2
   */
  String PROP_EMBEDDED_PORT = "sonar.embeddedDatabase.port";

  /**
   * @since 3.2
   */
  String PROP_EMBEDDED_PORT_DEFAULT_VALUE = "9092";
}
