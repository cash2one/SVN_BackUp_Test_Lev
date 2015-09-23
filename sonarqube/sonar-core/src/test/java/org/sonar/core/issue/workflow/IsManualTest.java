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
package org.sonar.core.issue.workflow;

import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.issue.DefaultIssue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.core.issue.workflow.IsManual.INSTANCE;

public class IsManualTest {

  @Test
  public void should_match() {
    DefaultIssue issue = new DefaultIssue();
    assertThat(INSTANCE.matches(issue.setRuleKey(RuleKey.of(RuleKey.MANUAL_REPOSITORY_KEY, "R1")))).isTrue();
    assertThat(INSTANCE.matches(issue.setRuleKey(RuleKey.of("java", "R1")))).isFalse();
  }

}
