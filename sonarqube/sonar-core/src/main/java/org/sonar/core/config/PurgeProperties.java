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

package org.sonar.core.config;

import java.util.Arrays;
import java.util.List;
import org.sonar.api.CoreProperties;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;

public final class PurgeProperties {

  private PurgeProperties() {
  }

  public static List<PropertyDefinition> all() {
    return Arrays.asList(
      PropertyDefinition.builder(PurgeConstants.PROPERTY_CLEAN_DIRECTORY)
        .defaultValue("true")
        .name("Clean directory/package history")
        .description("If set to true, no history is kept at directory/package level. Setting this to false can cause database bloat.")
        .type(PropertyType.BOOLEAN)
        .onQualifiers(Qualifiers.PROJECT)
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_DATABASE_CLEANER)
        .index(1)
        .build(),

      PropertyDefinition.builder(PurgeConstants.DAYS_BEFORE_DELETING_CLOSED_ISSUES)
        .defaultValue("30")
        .name("Delete closed issues after")
        .description("Issues that have been closed for more than this number of days will be deleted.")
        .type(PropertyType.INTEGER)
        .onQualifiers(Qualifiers.PROJECT)
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_DATABASE_CLEANER)
        .index(2)
        .build(),

      PropertyDefinition.builder(PurgeConstants.HOURS_BEFORE_KEEPING_ONLY_ONE_SNAPSHOT_BY_DAY)
        .defaultValue("24")
        .name("Keep only one snapshot a day after")
        .description("After this number of hours, if there are several snapshots during the same day, "
          + "the DbCleaner keeps the most recent one and fully deletes the other ones.")
        .type(PropertyType.INTEGER)
        .onQualifiers(Qualifiers.PROJECT)
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_DATABASE_CLEANER)
        .index(3)
        .build(),

      PropertyDefinition.builder(PurgeConstants.WEEKS_BEFORE_KEEPING_ONLY_ONE_SNAPSHOT_BY_WEEK)
        .defaultValue("4")
        .name("Keep only one snapshot a week after")
        .description("After this number of weeks, if there are several snapshots during the same week, "
          + "the DbCleaner keeps the most recent one and fully deletes the other ones")
        .type(PropertyType.INTEGER)
        .onQualifiers(Qualifiers.PROJECT)
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_DATABASE_CLEANER)
        .index(4)
        .build(),

      PropertyDefinition.builder(PurgeConstants.WEEKS_BEFORE_KEEPING_ONLY_ONE_SNAPSHOT_BY_MONTH)
        .defaultValue("52")
        .name("Keep only one snapshot a month after")
        .description("After this number of weeks, if there are several snapshots during the same month, "
          + "the DbCleaner keeps the most recent one and fully deletes the other ones.")
        .type(PropertyType.INTEGER)
        .onQualifiers(Qualifiers.PROJECT)
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_DATABASE_CLEANER)
        .index(5)
        .build(),

      PropertyDefinition.builder(PurgeConstants.WEEKS_BEFORE_DELETING_ALL_SNAPSHOTS)
        .defaultValue("260")
        .name("Delete all snapshots after")
        .description("After this number of weeks, all snapshots are fully deleted.")
        .type(PropertyType.INTEGER)
        .onQualifiers(Qualifiers.PROJECT)
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_DATABASE_CLEANER)
        .index(6)
        .build()
      );
  }
}
