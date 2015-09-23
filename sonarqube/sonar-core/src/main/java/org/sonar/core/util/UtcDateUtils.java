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
package org.sonar.core.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import org.sonar.api.utils.DateUtils;

public class UtcDateUtils {

  private UtcDateUtils() {
    // only static stuff
  }

  public static String formatDateTime(Date date) {
    SimpleDateFormat format = newFormat();
    return format.format(date);
  }

  public static Date parseDateTime(String s) {
    try {
      SimpleDateFormat format = newFormat();
      return format.parse(s);
    } catch (ParseException e) {
      throw new IllegalArgumentException("Fail to parse date: " + s, e);
    }
  }

  private static SimpleDateFormat newFormat() {
    SimpleDateFormat format = new SimpleDateFormat(DateUtils.DATETIME_FORMAT);
    format.setTimeZone(TimeZone.getTimeZone("UTC"));
    return format;
  }
}
