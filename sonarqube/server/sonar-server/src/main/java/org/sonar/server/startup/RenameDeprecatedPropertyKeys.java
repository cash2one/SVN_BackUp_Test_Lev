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
package org.sonar.server.startup;

import com.google.common.base.Strings;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.db.property.PropertiesDao;

/**
 * @since 3.4
 */
public class RenameDeprecatedPropertyKeys {

  private PropertiesDao dao;
  private PropertyDefinitions definitions;

  public RenameDeprecatedPropertyKeys(PropertiesDao dao, PropertyDefinitions definitions) {
    this.dao = dao;
    this.definitions = definitions;
  }

  public void start() {
    Loggers.get(RenameDeprecatedPropertyKeys.class).info("Rename deprecated property keys");
    for (PropertyDefinition definition : definitions.getAll()) {
      if (!Strings.isNullOrEmpty(definition.deprecatedKey())) {
        dao.renamePropertyKey(definition.deprecatedKey(), definition.key());
      }
    }
  }
}
