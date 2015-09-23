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

package org.sonar.db.component;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public final class SnapshotQuery {

  public enum SORT_FIELD {
    BY_DATE("created_at");
    final String fieldName;

    SORT_FIELD(String fieldName) {
      this.fieldName = fieldName;
    }
  }

  public enum SORT_ORDER {
    ASC("asc"), DESC("desc");
    final String order;

    SORT_ORDER(String order) {
      this.order = order;
    }
  }

  private Long componentId;
  private Long createdAfter;
  private Long createdBefore;
  private String status;
  private String version;
  private Boolean isLast;
  private String sortField;
  private String sortOrder;
  private String scope;
  private String qualifier;

  /**
   * filter to return snapshots created at or after a given date
   */
  @CheckForNull
  public Long getCreatedAfter() {
    return createdAfter;
  }

  public SnapshotQuery setCreatedAfter(@Nullable Long createdAfter) {
    this.createdAfter = createdAfter;
    return this;
  }

  /**
   * filter to return snapshots created before a given date
   */
  @CheckForNull
  public Long getCreatedBefore() {
    return createdBefore;
  }

  public SnapshotQuery setCreatedBefore(@Nullable Long createdBefore) {
    this.createdBefore = createdBefore;
    return this;
  }

  @CheckForNull
  public Boolean getIsLast() {
    return isLast;
  }

  public SnapshotQuery setIsLast(@Nullable Boolean isLast) {
    this.isLast = isLast;
    return this;
  }

  @CheckForNull
  public Long getComponentId() {
    return componentId;
  }

  public SnapshotQuery setComponentId(@Nullable Long componentId) {
    this.componentId = componentId;
    return this;
  }

  @CheckForNull
  public String getStatus() {
    return status;
  }

  public SnapshotQuery setStatus(@Nullable String status) {
    this.status = status;
    return this;
  }

  @CheckForNull
  public String getVersion() {
    return version;
  }

  public SnapshotQuery setVersion(@Nullable String version) {
    this.version = version;
    return this;
  }

  public SnapshotQuery setSort(SORT_FIELD sortField, SORT_ORDER sortOrder) {
    this.sortField = sortField.fieldName;
    this.sortOrder = sortOrder.order;
    return this;
  }

  @CheckForNull
  public String getSortField() {
    return sortField;
  }

  @CheckForNull
  public String getSortOrder() {
    return sortOrder;
  }

  @CheckForNull
  public String getScope() {
    return scope;
  }

  public SnapshotQuery setScope(@Nullable String scope) {
    this.scope = scope;
    return this;
  }

  @CheckForNull
  public String getQualifier() {
    return qualifier;
  }

  public SnapshotQuery setQualifier(@Nullable String qualifier) {
    this.qualifier = qualifier;
    return this;
  }
}
