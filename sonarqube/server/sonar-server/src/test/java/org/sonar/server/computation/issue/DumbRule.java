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
package org.sonar.server.computation.issue;

import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.server.debt.DebtRemediationFunction;

import static java.util.Objects.requireNonNull;

public class DumbRule implements Rule {
  private Integer id;
  private RuleKey key;
  private String name;
  private RuleStatus status = RuleStatus.READY;
  private Set<String> tags = new HashSet<>();
  private Integer subCharacteristicId;
  private DebtRemediationFunction function;

  public DumbRule(RuleKey key) {
    this.key = key;
  }

  @Override
  public int getId() {
    return requireNonNull(id);
  }

  @Override
  public RuleKey getKey() {
    return requireNonNull(key);
  }

  @Override
  public String getName() {
    return requireNonNull(name);
  }

  @Override
  public RuleStatus getStatus() {
    return requireNonNull(status);
  }

  @Override
  public Set<String> getTags() {
    return requireNonNull(tags);
  }

  @Override
  public Integer getSubCharacteristicId() {
    return subCharacteristicId;
  }

  @Override
  public DebtRemediationFunction getRemediationFunction() {
    return function;
  }

  public DumbRule setId(Integer id) {
    this.id = id;
    return this;
  }

  public DumbRule setName(String name) {
    this.name = name;
    return this;
  }

  public DumbRule setStatus(RuleStatus status) {
    this.status = status;
    return this;
  }

  public DumbRule setSubCharacteristicId(@Nullable Integer subCharacteristicId) {
    this.subCharacteristicId = subCharacteristicId;
    return this;
  }

  public DumbRule setFunction(@Nullable DebtRemediationFunction function) {
    this.function = function;
    return this;
  }

  public void setTags(Set<String> tags) {
    this.tags = tags;
  }
}
