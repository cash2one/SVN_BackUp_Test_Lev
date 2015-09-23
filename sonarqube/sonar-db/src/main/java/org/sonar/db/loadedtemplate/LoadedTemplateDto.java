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
package org.sonar.db.loadedtemplate;

import com.google.common.base.Objects;

public final class LoadedTemplateDto {

  public static final String DASHBOARD_TYPE = "DASHBOARD";
  public static final String FILTER_TYPE = "FILTER";
  public static final String QUALITY_PROFILE_TYPE = "QUALITY_PROFILE";
  public static final String PERMISSION_TEMPLATE_TYPE = "PERM_TEMPLATE";
  public static final String QUALITY_GATE_TYPE = "QUALITY_GATE";
  public static final String ONE_SHOT_TASK_TYPE = "ONE_SHOT_TASK";
  public static final String ISSUE_FILTER_TYPE = "ISSUE_FILTER";

  private Long id;
  private String key;
  private String type;

  public LoadedTemplateDto() {
  }

  public LoadedTemplateDto(String key, String type) {
    this.key = key;
    this.type = type;
  }

  public Long getId() {
    return id;
  }

  public LoadedTemplateDto setId(Long l) {
    this.id = l;
    return this;
  }

  public String getKey() {
    return key;
  }

  public LoadedTemplateDto setKey(String key) {
    this.key = key;
    return this;
  }

  public String getType() {
    return type;
  }

  public LoadedTemplateDto setType(String type) {
    this.type = type;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LoadedTemplateDto other = (LoadedTemplateDto) o;
    return Objects.equal(id, other.id) && Objects.equal(key, other.key) && Objects.equal(type, other.type);
  }

  @Override
  public int hashCode() {
    return id != null ? id.hashCode() : 0;
  }
}
