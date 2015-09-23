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

package org.sonar.db.measure;

import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class PastMeasureDto {

  private Long id;
  private Double value;
  private Integer metricId;
  private Integer ruleId;
  private Integer characteristicId;
  private Integer personId;

  public Long getId() {
    return id;
  }

  public PastMeasureDto setId(Long id) {
    this.id = id;
    return this;
  }

  public double getValue() {
    Objects.requireNonNull(value);
    return value;
  }

  public PastMeasureDto setValue(@Nullable Double value) {
    this.value = value;
    return this;
  }

  public boolean hasValue() {
    return value != null;
  }

  public Integer getMetricId() {
    return metricId;
  }

  public PastMeasureDto setMetricId(Integer metricId) {
    this.metricId = metricId;
    return this;
  }

  @CheckForNull
  public Integer getCharacteristicId() {
    return characteristicId;
  }

  public PastMeasureDto setCharacteristicId(@Nullable Integer characteristicId) {
    this.characteristicId = characteristicId;
    return this;
  }

  @CheckForNull
  public Integer getPersonId() {
    return personId;
  }

  public PastMeasureDto setPersonId(@Nullable Integer personId) {
    this.personId = personId;
    return this;
  }

  @CheckForNull
  public Integer getRuleId() {
    return ruleId;
  }

  public PastMeasureDto setRuleId(@Nullable Integer ruleId) {
    this.ruleId = ruleId;
    return this;
  }
}
