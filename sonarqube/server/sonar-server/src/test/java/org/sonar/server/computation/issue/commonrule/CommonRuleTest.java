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
package org.sonar.server.computation.issue.commonrule;

import com.google.common.collect.ImmutableMap;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.rule.Severity;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.computation.qualityprofile.ActiveRule;

import static org.assertj.core.api.Assertions.assertThat;

public class CommonRuleTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void test_getMinDensityParam() throws Exception {
    ActiveRule activeRule = new ActiveRule(RuleTesting.XOO_X1, Severity.MAJOR, ImmutableMap.of("minDensity", "30.5"));
    double minDensity = CommonRule.getMinDensityParam(activeRule, "minDensity");

    assertThat(minDensity).isEqualTo(30.5);
  }

  @Test
  public void getMinDensityParam_fails_if_param_value_is_absent() throws Exception {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Required parameter [minDensity] is missing on rule [xoo:x1]");

    ActiveRule activeRule = new ActiveRule(RuleTesting.XOO_X1, Severity.MAJOR, ImmutableMap.<String, String>of());
    CommonRule.getMinDensityParam(activeRule, "minDensity");
  }

  @Test
  public void getMinDensityParam_fails_if_param_value_is_negative() throws Exception {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Minimum density of rule [xoo:x1] is incorrect. Got [-30.5] but must be between 0 and 100.");

    ActiveRule activeRule = new ActiveRule(RuleTesting.XOO_X1, Severity.MAJOR, ImmutableMap.of("minDensity", "-30.5"));
    CommonRule.getMinDensityParam(activeRule, "minDensity");
  }

  @Test
  public void getMinDensityParam_fails_if_param_value_is_greater_than_100() throws Exception {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Minimum density of rule [xoo:x1] is incorrect. Got [305] but must be between 0 and 100.");

    ActiveRule activeRule = new ActiveRule(RuleTesting.XOO_X1, Severity.MAJOR, ImmutableMap.of("minDensity", "305"));
    CommonRule.getMinDensityParam(activeRule, "minDensity");
  }
}
