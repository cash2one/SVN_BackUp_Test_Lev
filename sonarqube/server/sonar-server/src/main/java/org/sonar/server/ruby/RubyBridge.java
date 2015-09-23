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
package org.sonar.server.ruby;

/**
 * This component acts as a bridge between a Ruby runtime and Java. Each method it defines creates a wrapping
 * Java object which an underlying Ruby implementation.
 */
public interface RubyBridge {
  /**
   * Returns a wrapper class that allows calling the database migration in Ruby.
   *
   * @return a  {@link RubyDatabaseMigration}
   */
  RubyDatabaseMigration databaseMigration();

  /**
   * Returns a class that allows calling the (re)creation of web routes in Rails.
   *
   * @return a {@link RubyRailsRoutes}
   */
  RubyRailsRoutes railsRoutes();

  RubyMetricCache metricCache();
}
