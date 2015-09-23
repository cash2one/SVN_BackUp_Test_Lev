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
package org.sonar.api.server.rule;

import org.apache.commons.lang.StringUtils;

/**
 * @since 4.2
 */
public class RuleTagFormat {

  // Allowed characters are the same as those on StackOverflow
  // see http://meta.stackoverflow.com/questions/22624/what-symbols-characters-are-not-allowed-in-tags
  private static final String VALID_CHARACTERS_REGEX = "^[a-z0-9\\+#\\-\\.]+$";

  private RuleTagFormat() {
    // only static methods
  }

  public static boolean isValid(String tag) {
    return StringUtils.isNotBlank(tag) && tag.matches(VALID_CHARACTERS_REGEX);
  }

  public static void validate(String tag) {
    if (!isValid(tag)) {
      throw new IllegalArgumentException(String.format("Tag '%s' is invalid. Rule tags accept only the characters: a-z, 0-9, '+', '-', '#', '.'", tag));
    }
  }
}
