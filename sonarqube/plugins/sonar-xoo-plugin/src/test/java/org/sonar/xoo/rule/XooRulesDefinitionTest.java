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
package org.sonar.xoo.rule;

import org.junit.Before;

import org.junit.Test;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.rule.RulesDefinition;
import static org.assertj.core.api.Assertions.assertThat;

public class XooRulesDefinitionTest {
  RulesDefinition.Context context;

  @Before
  public void setUp() {
    XooRulesDefinition def = new XooRulesDefinition();
    context = new RulesDefinition.Context();
    def.define(context);
  }

  @Test
  public void define_xoo_rules() {
    RulesDefinition.Repository repo = context.repository("xoo");
    assertThat(repo).isNotNull();
    assertThat(repo.name()).isEqualTo("Xoo");
    assertThat(repo.language()).isEqualTo("xoo");
    assertThat(repo.rules()).hasSize(11);

    RulesDefinition.Rule rule = repo.rule(OneIssuePerLineSensor.RULE_KEY);
    assertThat(rule.name()).isNotEmpty();
    assertThat(rule.debtSubCharacteristic()).isEqualTo(RulesDefinition.SubCharacteristics.MEMORY_EFFICIENCY);
    assertThat(rule.debtRemediationFunction().type()).isEqualTo(DebtRemediationFunction.Type.LINEAR);
    assertThat(rule.debtRemediationFunction().coefficient()).isEqualTo("1min");
    assertThat(rule.debtRemediationFunction().offset()).isNull();
    assertThat(rule.effortToFixDescription()).isNotEmpty();
  }

  @Test
  public void define_xoo2_rules() {
    RulesDefinition.Repository repo = context.repository("xoo2");
    assertThat(repo).isNotNull();
    assertThat(repo.name()).isEqualTo("Xoo2");
    assertThat(repo.language()).isEqualTo("xoo2");
    assertThat(repo.rules()).hasSize(2);
  }
}
