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
package org.sonar.server.search;

import org.elasticsearch.common.joda.time.format.ISODateTimeFormat;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Date;

public class IndexUtils {

  private IndexUtils() {
    // only static stuff
  }

  @CheckForNull
  public static Date parseDateTime(@Nullable String s) {
    if (s == null) {
      return null;
    }
    return ISODateTimeFormat.dateTime().parseDateTime(s).toDate();
  }

  public static String format(Date date) {
    return ISODateTimeFormat.dateTime().print(date.getTime());
  }
}
