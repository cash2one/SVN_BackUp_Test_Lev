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

package org.sonar.server.qualityprofile;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.db.qualityprofile.QualityProfileDto;

public class QProfile {

  private int id;
  private String key;
  private String name;
  private String language;
  private String parent;
  private boolean isDefault;
  private String rulesUpdatedAt;

  /**
   * @deprecated in 4.4
   */
  @Deprecated
  public int id() {
    return id;
  }

  /**
   * @deprecated in 4.4
   */
  @Deprecated
  QProfile setId(int id) {
    this.id = id;
    return this;
  }

  public String name() {
    return name;
  }

  public QProfile setName(String name) {
    this.name = name;
    return this;
  }

  public String language() {
    return language;
  }

  public QProfile setLanguage(String language) {
    this.language = language;
    return this;
  }

  public String key() {
    return key;
  }

  public QProfile setKey(String s) {
    this.key = s;
    return this;
  }

  @CheckForNull
  public String parent() {
    return parent;
  }

  public QProfile setParent(@Nullable String parent) {
    this.parent = parent;
    return this;
  }

  public boolean isInherited() {
    return parent != null;
  }

  public boolean isDefault() {
    return isDefault;
  }

  public QProfile setDefault(boolean isDefault) {
    this.isDefault = isDefault;
    return this;
  }

  public String getRulesUpdatedAt() {
    return rulesUpdatedAt;
  }

  public QProfile setRulesUpdatedAt(String rulesUpdatedAt) {
    this.rulesUpdatedAt = rulesUpdatedAt;
    return this;
  }

  public static QProfile from(QualityProfileDto dto) {
    return new QProfile()
      .setId(dto.getId())
      .setKey(dto.getKey())
      .setName(dto.getName())
      .setLanguage(dto.getLanguage())
      .setParent(dto.getParentKee())
      .setDefault(dto.isDefault())
      .setRulesUpdatedAt(dto.getRulesUpdatedAt());
  }

  @Override
  public String toString() {
    return new ReflectionToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).toString();
  }
}
