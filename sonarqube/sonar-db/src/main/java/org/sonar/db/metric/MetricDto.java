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

package org.sonar.db.metric;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class MetricDto {

  private Integer id;

  private String kee;

  private String shortName;

  private String valueType;

  private String description;

  private String domain;

  private int direction;

  private boolean qualitative;

  private boolean userManaged;

  private Double worstValue;

  private Double bestValue;

  private boolean optimizedBestValue;

  private boolean hidden;

  private boolean deleteHistoricalData;

  private boolean enabled;

  public Integer getId() {
    return id;
  }

  public MetricDto setId(Integer id) {
    this.id = id;
    return this;
  }

  public String getKey() {
    return kee;
  }

  public MetricDto setKey(String name) {
    this.kee = name;
    return this;
  }

  public String getShortName() {
    return shortName;
  }

  public MetricDto setShortName(String shortName) {
    this.shortName = shortName;
    return this;
  }

  public String getValueType() {
    return valueType;
  }

  public MetricDto setValueType(String valueType) {
    this.valueType = valueType;
    return this;
  }

  /**
   * @return null for manual metrics
   */
  @CheckForNull
  public String getDescription() {
    return description;
  }

  public MetricDto setDescription(@Nullable String description) {
    this.description = description;
    return this;
  }

  public String getDomain() {
    return domain;
  }

  public MetricDto setDomain(String domain) {
    this.domain = domain;
    return this;
  }

  public int getDirection() {
    return direction;
  }

  public MetricDto setDirection(int direction) {
    this.direction = direction;
    return this;
  }

  public boolean isQualitative() {
    return qualitative;
  }

  public MetricDto setQualitative(boolean qualitative) {
    this.qualitative = qualitative;
    return this;
  }

  public boolean isUserManaged() {
    return userManaged;
  }

  public MetricDto setUserManaged(boolean userManaged) {
    this.userManaged = userManaged;
    return this;
  }

  @CheckForNull
  public Double getWorstValue() {
    return worstValue;
  }

  public MetricDto setWorstValue(@Nullable Double worstValue) {
    this.worstValue = worstValue;
    return this;
  }

  @CheckForNull
  public Double getBestValue() {
    return bestValue;
  }

  public MetricDto setBestValue(@Nullable Double bestValue) {
    this.bestValue = bestValue;
    return this;
  }

  public boolean isOptimizedBestValue() {
    return optimizedBestValue;
  }

  public MetricDto setOptimizedBestValue(boolean optimizedBestValue) {
    this.optimizedBestValue = optimizedBestValue;
    return this;
  }

  public boolean isHidden() {
    return hidden;
  }

  public MetricDto setHidden(boolean hidden) {
    this.hidden = hidden;
    return this;
  }

  public boolean isDeleteHistoricalData() {
    return deleteHistoricalData;
  }

  public MetricDto setDeleteHistoricalData(boolean deleteHistoricalData) {
    this.deleteHistoricalData = deleteHistoricalData;
    return this;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public MetricDto setEnabled(boolean enabled) {
    this.enabled = enabled;
    return this;
  }

}
