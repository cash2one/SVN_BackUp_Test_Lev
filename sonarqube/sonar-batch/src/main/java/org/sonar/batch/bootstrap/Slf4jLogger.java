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
package org.sonar.batch.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Slf4jLogger implements org.sonar.home.cache.Logger {

  private static final Logger LOG = LoggerFactory.getLogger(Slf4jLogger.class);

  @Override
  public void debug(String msg) {
    LOG.debug(msg);
  }

  @Override
  public void info(String msg) {
    LOG.info(msg);
  }

  @Override
  public void warn(String msg) {
    LOG.warn(msg);
  }

  @Override
  public void error(String msg, Throwable t) {
    LOG.error(msg, t);
  }

  @Override
  public void error(String msg) {
    LOG.error(msg);
  }

}
