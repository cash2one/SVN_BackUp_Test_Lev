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
package org.sonar.application;

import org.apache.commons.lang.StringUtils;

import java.util.Map;
import java.util.Properties;

class CommandLineParser {
  /**
   * Build properties from command-line arguments and system properties
   */
  Properties parseArguments(String[] args) {
    Properties props = argumentsToProperties(args);

    // complete with only the system properties that start with "sonar."
    for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
      String key = entry.getKey().toString();
      if (key.startsWith("sonar.")) {
        props.setProperty(key, entry.getValue().toString());
      }
    }
    return props;
  }

  /**
   * Convert strings "-Dkey=value" to properties
   */
  Properties argumentsToProperties(String[] args) {
    Properties props = new Properties();
    for (String arg : args) {
      if (!arg.startsWith("-D") || !arg.contains("=")) {
        throw new IllegalArgumentException(String.format(
          "Command-line argument must start with -D, for example -Dsonar.jdbc.username=sonar. Got: %s", arg));
      }
      String key = StringUtils.substringBefore(arg, "=").substring(2);
      String value = StringUtils.substringAfter(arg, "=");
      props.setProperty(key, value);
    }
    return props;
  }
}
