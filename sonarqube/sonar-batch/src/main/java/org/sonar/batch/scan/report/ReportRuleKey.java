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

import org.sonar.api.batch.rule.Rule;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.sonar.api.rules.RulePriority;

/**
 * A same rule can be present with different severity if severity was manually changed so we need this special key that
 * include severity.
 *
 */
public class ReportRuleKey implements Comparable<ReportRuleKey> {
  private final Rule rule;
  private final RulePriority severity;

  public ReportRuleKey(Rule rule, RulePriority severity) {
    this.rule = rule;
    this.severity = severity;
  }

  public Rule getRule() {
    return rule;
  }

  public RulePriority getSeverity() {
    return severity;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ReportRuleKey that = (ReportRuleKey) o;
    return ObjectUtils.equals(rule, that.rule) && ObjectUtils.equals(severity, that.severity);
  }

  @Override
  public int hashCode() {
    int result = rule.hashCode();
    result = 31 * result + severity.hashCode();
    return result;
  }

  @Override
  public int compareTo(ReportRuleKey o) {
    if (severity == o.getSeverity()) {
      return getRule().key().toString().compareTo(o.getRule().key().toString());
    }
    return o.getSeverity().compareTo(severity);
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this).
      append("rule", rule).
      append("severity", severity).
      toString();
  }
}
