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
package org.sonar.db.qualityprofile;

import com.google.common.base.Preconditions;
import java.io.Serializable;
import org.sonar.api.rule.RuleKey;

/**
 *
 * @since 4.4
 */
public class ActiveRuleKey implements Serializable {

  private final String qualityProfileKey;
  private final RuleKey ruleKey;

  protected ActiveRuleKey(String qualityProfileKey, RuleKey ruleKey) {
    this.qualityProfileKey = qualityProfileKey;
    this.ruleKey = ruleKey;
  }

  /**
   * Create a key. Parameters are NOT null.
   */
  public static ActiveRuleKey of(String qualityProfileKey, RuleKey ruleKey) {
    Preconditions.checkNotNull(qualityProfileKey, "QProfile is missing");
    Preconditions.checkNotNull(ruleKey, "RuleKey is missing");
    return new ActiveRuleKey(qualityProfileKey, ruleKey);
  }

  /**
   * Create a key from a string representation (see {@link #toString()}. An {@link IllegalArgumentException} is raised
   * if the format is not valid.
   */
  public static ActiveRuleKey parse(String s) {
    Preconditions.checkArgument(s.split(":").length >= 3, "Bad format of activeRule key: " + s);
    int semiColonPos = s.indexOf(":");
    String key = s.substring(0, semiColonPos);
    String ruleKey = s.substring(semiColonPos + 1);
    return ActiveRuleKey.of(key, RuleKey.parse(ruleKey));
  }

  /**
   * Never null
   */
  public RuleKey ruleKey() {
    return ruleKey;
  }

  /**
   * Never null
   */
  public String qProfile() {
    return qualityProfileKey;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ActiveRuleKey activeRuleKey = (ActiveRuleKey) o;
    if (!qualityProfileKey.equals(activeRuleKey.qualityProfileKey)) {
      return false;
    }
    if (!ruleKey.equals(activeRuleKey.ruleKey)) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int result = qualityProfileKey.hashCode();
    result = 31 * result + ruleKey.hashCode();
    return result;
  }

  /**
   * Format is "qprofile:rule", for example "12345:squid:AvoidCycle"
   */
  @Override
  public String toString() {
    return String.format("%s:%s", qualityProfileKey, ruleKey.toString());
  }
}
