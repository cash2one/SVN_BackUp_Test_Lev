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
package org.sonar.api.batch.rule.internal;

import org.sonar.api.batch.rule.Rules;
import org.sonar.api.rule.RuleKey;

import java.util.HashMap;
import java.util.Map;

/**
 * For unit testing and internal use only.
 *
 * @since 4.2
 */

public class RulesBuilder {

  private final Map<RuleKey, NewRule> map = new HashMap<>();

  public NewRule add(RuleKey key) {
    if (map.containsKey(key)) {
      throw new IllegalStateException(String.format("Rule '%s' already exists", key));
    }
    NewRule newRule = new NewRule(key);
    map.put(key, newRule);
    return newRule;
  }

  public Rules build() {
    return new DefaultRules(map.values());
  }
}
