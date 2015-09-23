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
package org.sonar.server.ws;

import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.utils.Paging;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.user.UserDto;
import org.sonarqube.ws.Common;

import static com.google.common.base.Strings.nullToEmpty;

public class WsResponseCommonFormat {

  private final Languages languages;

  public WsResponseCommonFormat(Languages languages) {
    this.languages = languages;
  }

  public Common.Paging.Builder formatPaging(Paging paging) {
    return Common.Paging.newBuilder()
      .setPageIndex(paging.pageIndex())
      .setPageSize(paging.pageSize())
      .setTotal(paging.total());
  }

  public Common.Rule.Builder formatRule(RuleDto rule) {
    Common.Rule.Builder builder = Common.Rule.newBuilder()
      .setKey(rule.getKey().toString())
      .setName(nullToEmpty(rule.getName()))
      .setStatus(Common.RuleStatus.valueOf(rule.getStatus().name()));

    builder.setLang(nullToEmpty(rule.getLanguage()));
    Language lang = languages.get(rule.getLanguage());
    if (lang != null) {
      builder.setLangName(lang.getName());
    }
    return builder;
  }

  public Common.User.Builder formatUser(UserDto user) {
    return Common.User.newBuilder()
      .setLogin(user.getLogin())
      .setName(nullToEmpty(user.getName()))
      .setEmail(nullToEmpty(user.getEmail()))
      .setActive(user.isActive());
  }
}
