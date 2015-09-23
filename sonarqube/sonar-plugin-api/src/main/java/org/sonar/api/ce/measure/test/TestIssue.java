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

package org.sonar.api.ce.measure.test;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.ce.measure.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.Duration;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Immutable
public class TestIssue implements Issue {

  private String key;
  private String status;
  private String resolution;
  private String severity;
  private RuleKey ruleKey;
  private Duration debt;

  private TestIssue(Builder builder) {
    this.key = builder.key;
    this.status = builder.status;
    this.resolution = builder.resolution;
    this.severity = builder.severity;
    this.ruleKey = builder.ruleKey;
    this.debt = builder.debt;
  }

  @Override
  public String key() {
    return key;
  }

  @Override
  public RuleKey ruleKey() {
    return ruleKey;
  }

  @Override
  public String status() {
    return status;
  }

  @Override
  @CheckForNull
  public String resolution() {
    return resolution;
  }

  @Override
  public String severity() {
    return severity;
  }

  @Override
  @CheckForNull
  public Duration debt() {
    return debt;
  }

  public static class Builder {
    private String key;
    private String status;
    private String resolution;
    private String severity;
    private RuleKey ruleKey;
    private Duration debt;

    public Builder setKey(String key) {
      this.key = validateKey(key);
      return this;
    }

    public Builder setResolution(@Nullable String resolution) {
      this.resolution = validateResolution(resolution);
      return this;
    }

    public Builder setSeverity(String severity) {
      this.severity = validateSeverity(severity);
      return this;
    }

    public Builder setStatus(String status) {
      this.status = validateStatus(status);
      return this;
    }

    public Builder setRuleKey(RuleKey ruleKey) {
      this.ruleKey = validateRuleKey(ruleKey);
      return this;
    }

    public Builder setDebt(@Nullable Duration debt) {
      this.debt = debt;
      return this;
    }

    private static String validateKey(String key){
      checkNotNull(key, "key cannot be null");
      return key;
    }

    private static RuleKey validateRuleKey(RuleKey ruleKey){
      checkNotNull(ruleKey, "ruleKey cannot be null");
      return ruleKey;
    }

    private static String validateResolution(@Nullable String resolution){
      checkArgument(resolution == null || org.sonar.api.issue.Issue.RESOLUTIONS.contains(resolution), String.format("resolution '%s' is invalid", resolution));
      return resolution;
    }

    private static String validateSeverity(String severity){
      checkNotNull(severity, "severity cannot be null");
      checkArgument(Severity.ALL.contains(severity), String.format("severity '%s' is invalid", severity));
      return severity;
    }

    private static String validateStatus(String status){
      checkNotNull(status, "status cannot be null");
      checkArgument(org.sonar.api.issue.Issue.STATUSES.contains(status), String.format("status '%s' is invalid", status));
      return status;
    }

    public Issue build(){
      validateKey(key);
      validateResolution(resolution);
      validateSeverity(severity);
      validateStatus(status);
      validateRuleKey(ruleKey);
      return new TestIssue(this);
    }
  }
}
