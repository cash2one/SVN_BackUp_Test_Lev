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

package org.sonar.server.i18n;

import java.text.MessageFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.i18n.I18n;

public class I18nRule implements I18n {
  private final Map<String, String> messages = new HashMap<>();

  public I18nRule put(String key, String value) {
    messages.put(key, value);
    return this;
  }

  public void setProjectPermissions() {
    put("projects_role.admin", "Administer");
    put("projects_role.admin.desc", "Ability to access project settings and perform administration tasks. " +
      "(Users will also need \"Browse\" permission)");
    put("projects_role.issueadmin", "Administer Issues");
    put("projects_role.issueadmin.desc", "Grants the permission to perform advanced editing on issues: marking an issue " +
      "False Positive / Won't Fix or changing an Issue's severity. (Users will also need \"Browse\" permission)");
    put("projects_role.user", "Browse");
    put("projects_role.user.desc", "Ability to access a project, browse its measures, and create/edit issues for it.");
    put("projects_role.codeviewer", "See Source Code");
    put("projects_role.codeviewer.desc", "Ability to view the project's source code. (Users will also need \"Browse\" permission)");
  }

  @Override
  public String message(Locale locale, String key, @Nullable String defaultValue, Object... parameters) {
    String messageInMap = messages.get(key);
    String message = messageInMap != null ? messageInMap : defaultValue;
    return formatMessage(message, parameters);
  }

  @CheckForNull
  private static String formatMessage(@Nullable String message, Object... parameters) {
    if (message == null || parameters.length == 0) {
      return message;
    }
    return MessageFormat.format(message.replaceAll("'", "''"), parameters);
  }

  @Override
  public String age(Locale locale, long durationInMillis) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String age(Locale locale, Date fromDate, Date toDate) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String ageFromNow(Locale locale, Date date) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String formatDateTime(Locale locale, Date date) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String formatDate(Locale locale, Date date) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String formatDouble(Locale locale, Double value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String formatInteger(Locale locale, Integer value) {
    throw new UnsupportedOperationException();
  }
}
