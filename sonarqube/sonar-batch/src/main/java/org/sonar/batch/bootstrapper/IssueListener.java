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
package org.sonar.batch.bootstrapper;

public interface IssueListener {
  void handle(Issue issue);
  
  class Issue {
    private String key;
    private String componentKey;
    private Integer line;
    private String message;
    private String ruleKey;
    private String ruleName;
    private String status;
    private String resolution;
    private boolean isNew;
    private String assigneeLogin;
    private String assigneeName;
    private String severity;

    public String getSeverity() {
      return severity;
    }

    public void setSeverity(String severity) {
      this.severity = severity;
    }

    public String getKey() {
      return key;
    }

    public void setKey(String key) {
      this.key = key;
    }

    public String getComponentKey() {
      return componentKey;
    }

    public void setComponentKey(String componentKey) {
      this.componentKey = componentKey;
    }

    public Integer getLine() {
      return line;
    }

    public void setLine(Integer line) {
      this.line = line;
    }

    public String getMessage() {
      return message;
    }

    public void setMessage(String message) {
      this.message = message;
    }

    public String getRuleKey() {
      return ruleKey;
    }

    public void setRuleKey(String ruleKey) {
      this.ruleKey = ruleKey;
    }

    public String getRuleName() {
      return ruleName;
    }

    public void setRuleName(String ruleName) {
      this.ruleName = ruleName;
    }

    public String getStatus() {
      return status;
    }

    public void setStatus(String status) {
      this.status = status;
    }

    public String getResolution() {
      return resolution;
    }

    public void setResolution(String resolution) {
      this.resolution = resolution;
    }

    public boolean isNew() {
      return isNew;
    }

    public void setNew(boolean isNew) {
      this.isNew = isNew;
    }

    public String getAssigneeLogin() {
      return assigneeLogin;
    }

    public void setAssigneeLogin(String assigneeLogin) {
      this.assigneeLogin = assigneeLogin;
    }

    public String getAssigneeName() {
      return assigneeName;
    }

    public void setAssigneeName(String assigneeName) {
      this.assigneeName = assigneeName;
    }
  }
}
