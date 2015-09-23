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
package org.sonar.batch.scan.report;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.Matchers.any;

import org.sonar.api.rule.RuleKey;
import org.sonar.api.batch.rule.Rule;
import org.junit.Test;
import org.junit.Before;
import org.sonar.api.batch.rule.Rules;

public class RuleNameProviderTest {
  RuleNameProvider provider;
  Rules rules;
  Rule rule;
  RuleKey ruleKey;

  @Before
  public void setUp() {
    ruleKey = mock(RuleKey.class);
    rule = mock(Rule.class);
    rules = mock(Rules.class);
    provider = new RuleNameProvider(rules);

    when(ruleKey.rule()).thenReturn("ruleKey");
    when(ruleKey.repository()).thenReturn("repoKey");

    when(rule.name()).thenReturn("name");
    when(rule.key()).thenReturn(ruleKey);

    when(rules.find(any(RuleKey.class))).thenReturn(rule);
  }

  @Test
  public void testNameForHTML() {
    assertThat(provider.nameForHTML(rule)).isEqualTo(rule.name());
    assertThat(provider.nameForHTML(ruleKey)).isEqualTo(rule.name());
  }

  @Test
  public void testNameForJS() {
    assertThat(provider.nameForJS("repoKey:ruleKey")).isEqualTo(rule.name());
  }

}
