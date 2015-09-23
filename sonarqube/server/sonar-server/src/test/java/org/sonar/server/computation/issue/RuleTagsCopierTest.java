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

import com.google.common.collect.Sets;
import java.util.Collections;
import org.junit.Test;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.server.computation.component.Component;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.db.rule.RuleTesting.XOO_X1;

public class RuleTagsCopierTest {

  DumbRule rule = new DumbRule(XOO_X1);

  @org.junit.Rule
  public RuleRepositoryRule ruleRepository = new RuleRepositoryRule().add(rule);

  DefaultIssue issue = new DefaultIssue().setRuleKey(rule.getKey());
  RuleTagsCopier underTest = new RuleTagsCopier(ruleRepository);

  @Test
  public void copy_tags_if_new_rule() {
    rule.setTags(Sets.newHashSet("bug", "performance"));
    issue.setNew(true);

    underTest.onIssue(mock(Component.class), issue);

    assertThat(issue.tags()).containsExactly("bug", "performance");
  }

  @Test
  public void do_not_copy_tags_if_existing_rule() {
    rule.setTags(Sets.newHashSet("bug", "performance"));
    issue.setNew(false).setTags(asList("misra"));

    underTest.onIssue(mock(Component.class), issue);

    assertThat(issue.tags()).containsExactly("misra");
  }

  @Test
  public void do_not_copy_tags_if_existing_rule_without_tags() {
    rule.setTags(Sets.newHashSet("bug", "performance"));
    issue.setNew(false).setTags(Collections.<String>emptyList());

    underTest.onIssue(mock(Component.class), issue);

    assertThat(issue.tags()).isEmpty();
  }
}
