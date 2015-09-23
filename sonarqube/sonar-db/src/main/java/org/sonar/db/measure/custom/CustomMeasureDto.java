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

package org.sonar.db.measure.custom;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class CustomMeasureDto {

  private long id;
  private int metricId;
  private String componentUuid;
  private double value;
  private String textValue;
  private String userLogin;
  private String description;
  private long createdAt;
  private long updatedAt;

  public String getDescription() {
    return description;
  }

  public CustomMeasureDto setDescription(String description) {
    this.description = description;
    return this;
  }

  public String getUserLogin() {
    return userLogin;
  }

  public CustomMeasureDto setUserLogin(String userLogin) {
    this.userLogin = userLogin;
    return this;
  }

  @CheckForNull
  public String getTextValue() {
    return textValue;
  }

  public CustomMeasureDto setTextValue(@Nullable String textValue) {
    this.textValue = textValue;
    return this;
  }

  public double getValue() {
    return value;
  }

  public CustomMeasureDto setValue(double value) {
    this.value = value;
    return this;
  }

  public int getMetricId() {
    return metricId;
  }

  public CustomMeasureDto setMetricId(int metricId) {
    this.metricId = metricId;
    return this;
  }

  public long getId() {
    return id;
  }

  public CustomMeasureDto setId(long id) {
    this.id = id;
    return this;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }

  public CustomMeasureDto setUpdatedAt(long updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public CustomMeasureDto setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public String getComponentUuid() {
    return componentUuid;
  }

  public CustomMeasureDto setComponentUuid(String componentUuid) {
    this.componentUuid = componentUuid;
    return this;
  }
}
