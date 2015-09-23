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

package org.sonar.server.properties;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.config.Settings;

import java.util.Map;

public class ProjectSettings extends Settings {
  private final Settings settings;
  private final Map<String, String> projectProperties;

  public ProjectSettings(Settings settings, Map<String, String> projectProperties) {
    this.settings = settings;
    this.projectProperties = projectProperties;
  }

  @Override
  public String getString(String key) {
    String value = get(key);
    if (value == null) {
      return settings.getString(key);
    }

    return value;
  }

  @Override
  public boolean getBoolean(String key) {
    String value = get(key);
    if (value == null) {
      return settings.getBoolean(key);
    }

    return StringUtils.isNotEmpty(value) && Boolean.parseBoolean(value);
  }

  @Override
  public int getInt(String key) {
    String value = get(key);
    if (value == null) {
      return settings.getInt(key);
    } else if (StringUtils.isNotEmpty(value)) {
      return Integer.parseInt(value);
    } else {
      return 0;
    }
  }

  private String get(String key) {
    return projectProperties.get(key);
  }
}
