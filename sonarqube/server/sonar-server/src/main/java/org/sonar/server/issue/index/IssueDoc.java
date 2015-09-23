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
package org.sonar.server.issue.index;

import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.BooleanUtils;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueComment;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.server.search.BaseDoc;

public class IssueDoc extends BaseDoc implements Issue {

  public IssueDoc(Map<String, Object> fields) {
    super(fields);
  }

  public IssueDoc() {
    super(Maps.<String, Object>newHashMap());
  }

  @Override
  public String key() {
    return getField(IssueIndexDefinition.FIELD_ISSUE_KEY);
  }

  @Override
  public String componentKey() {
    throw new IllegalStateException("componentKey is not available on server side");
  }

  @Override
  public String componentUuid() {
    return getField(IssueIndexDefinition.FIELD_ISSUE_COMPONENT_UUID);
  }

  @CheckForNull
  public String moduleUuid() {
    return getField(IssueIndexDefinition.FIELD_ISSUE_MODULE_UUID);
  }

  public String modulePath() {
    return getField(IssueIndexDefinition.FIELD_ISSUE_MODULE_PATH);
  }

  @Override
  public String projectKey() {
    throw new IllegalStateException("projectKey is not available on server side");
  }

  @Override
  public String projectUuid() {
    return getField(IssueIndexDefinition.FIELD_ISSUE_PROJECT_UUID);
  }

  @Override
  public RuleKey ruleKey() {
    return RuleKey.parse((String) getField(IssueIndexDefinition.FIELD_ISSUE_RULE_KEY));
  }

  @Override
  public String language() {
    return getField(IssueIndexDefinition.FIELD_ISSUE_LANGUAGE);
  }

  @Override
  public String severity() {
    return getField(IssueIndexDefinition.FIELD_ISSUE_SEVERITY);
  }

  public boolean isManualSeverity() {
    return BooleanUtils.isTrue((Boolean) getField(IssueIndexDefinition.FIELD_ISSUE_MANUAL_SEVERITY));
  }

  @Nullable
  public String checksum() {
    return getNullableField(IssueIndexDefinition.FIELD_ISSUE_CHECKSUM);
  }

  @Override
  @CheckForNull
  public String message() {
    return getNullableField(IssueIndexDefinition.FIELD_ISSUE_MESSAGE);
  }

  @Override
  @CheckForNull
  public Integer line() {
    return getNullableField(IssueIndexDefinition.FIELD_ISSUE_LINE);
  }

  @Override
  @CheckForNull
  public Double effortToFix() {
    return getNullableField(IssueIndexDefinition.FIELD_ISSUE_EFFORT);
  }

  @Override
  public String status() {
    return getField(IssueIndexDefinition.FIELD_ISSUE_STATUS);
  }

  @Override
  @CheckForNull
  public String resolution() {
    return getNullableField(IssueIndexDefinition.FIELD_ISSUE_RESOLUTION);
  }

  @Override
  @CheckForNull
  public String reporter() {
    return getNullableField(IssueIndexDefinition.FIELD_ISSUE_REPORTER);
  }

  @Override
  @CheckForNull
  public String assignee() {
    return getNullableField(IssueIndexDefinition.FIELD_ISSUE_ASSIGNEE);
  }

  /**
   * Functional date
   */
  @Override
  public Date creationDate() {
    return getFieldAsDate(IssueIndexDefinition.FIELD_ISSUE_FUNC_CREATED_AT);
  }

  /**
   * Functional date
   */
  @Override
  public Date updateDate() {
    return getFieldAsDate(IssueIndexDefinition.FIELD_ISSUE_FUNC_UPDATED_AT);
  }

  @Override
  @CheckForNull
  public Date closeDate() {
    return getNullableFieldAsDate(IssueIndexDefinition.FIELD_ISSUE_FUNC_CLOSED_AT);
  }

  @Override
  @CheckForNull
  public String attribute(String key) {
    return attributes().get(key);
  }

  @Override
  public Map<String, String> attributes() {
    String data = getNullableField(IssueIndexDefinition.FIELD_ISSUE_ATTRIBUTES);
    if (data == null) {
      return Collections.emptyMap();
    } else {
      return KeyValueFormat.parse(data);
    }
  }

  @Override
  @CheckForNull
  public String authorLogin() {
    return getNullableField(IssueIndexDefinition.FIELD_ISSUE_AUTHOR_LOGIN);
  }

  @Override
  @CheckForNull
  public String actionPlanKey() {
    return getNullableField(IssueIndexDefinition.FIELD_ISSUE_ACTION_PLAN);
  }

  @Override
  public List<IssueComment> comments() {
    throw new IllegalStateException("Comments are not availables in index");
  }

  @Override
  public boolean isNew() {
    throw new IllegalStateException("isNew is only available for batch");
  }

  @Override
  @CheckForNull
  public Duration debt() {
    Number debt = getNullableField(IssueIndexDefinition.FIELD_ISSUE_DEBT);
    return (debt != null) ? Duration.create(debt.longValue()) : null;
  }

  @CheckForNull
  public String filePath() {
    return getNullableField(IssueIndexDefinition.FIELD_ISSUE_FILE_PATH);
  }

  @CheckForNull
  public String directoryPath() {
    return getNullableField(IssueIndexDefinition.FIELD_ISSUE_DIRECTORY_PATH);
  }

  public IssueDoc setKey(@Nullable String s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_KEY, s);
    return this;
  }

  public IssueDoc setComponentUuid(@Nullable String s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_COMPONENT_UUID, s);
    return this;
  }

  public IssueDoc setModuleUuid(@Nullable String s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_MODULE_UUID, s);
    return this;
  }

  public IssueDoc setProjectUuid(@Nullable String s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_PROJECT_UUID, s);
    return this;
  }

  public IssueDoc setRuleKey(@Nullable String s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_RULE_KEY, s);
    return this;
  }

  public IssueDoc setLanguage(@Nullable String s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_LANGUAGE, s);
    return this;
  }

  public IssueDoc setSeverity(@Nullable String s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_SEVERITY, s);
    setField(IssueIndexDefinition.FIELD_ISSUE_SEVERITY_VALUE, Severity.ALL.indexOf(s));
    return this;
  }

  public IssueDoc setManualSeverity(boolean b) {
    setField(IssueIndexDefinition.FIELD_ISSUE_MANUAL_SEVERITY, b);
    return this;
  }

  public IssueDoc setMessage(@Nullable String s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_MESSAGE, s);
    return this;
  }

  public IssueDoc setChecksum(@Nullable String s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_CHECKSUM, s);
    return this;
  }

  public IssueDoc setLine(@Nullable Integer i) {
    setField(IssueIndexDefinition.FIELD_ISSUE_LINE, i);
    return this;
  }

  public IssueDoc setEffortToFix(@Nullable Double d) {
    setField(IssueIndexDefinition.FIELD_ISSUE_EFFORT, d);
    return this;
  }

  public IssueDoc setStatus(@Nullable String s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_STATUS, s);
    return this;
  }

  public IssueDoc setResolution(@Nullable String s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_RESOLUTION, s);
    return this;
  }

  public IssueDoc setReporter(@Nullable String s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_REPORTER, s);
    return this;
  }

  public IssueDoc setAssignee(@Nullable String s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_ASSIGNEE, s);
    return this;
  }

  public IssueDoc setFuncUpdateDate(@Nullable Date d) {
    setField(IssueIndexDefinition.FIELD_ISSUE_FUNC_UPDATED_AT, d);
    return this;
  }

  public IssueDoc setFuncCreationDate(@Nullable Date d) {
    setField(IssueIndexDefinition.FIELD_ISSUE_FUNC_CREATED_AT, d);
    return this;
  }

  public Date getTechnicalUpdateDate() {
    return getFieldAsDate(IssueIndexDefinition.FIELD_ISSUE_TECHNICAL_UPDATED_AT);
  }

  public IssueDoc setTechnicalUpdateDate(@Nullable Date d) {
    setField(IssueIndexDefinition.FIELD_ISSUE_TECHNICAL_UPDATED_AT, d);
    return this;
  }

  public IssueDoc setFuncCloseDate(@Nullable Date d) {
    setField(IssueIndexDefinition.FIELD_ISSUE_FUNC_CLOSED_AT, d);
    return this;
  }

  public IssueDoc setAttributes(@Nullable String s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_ATTRIBUTES, s);
    return this;
  }

  public IssueDoc setAuthorLogin(@Nullable String s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_AUTHOR_LOGIN, s);
    return this;
  }

  public IssueDoc setActionPlanKey(@Nullable String s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_ACTION_PLAN, s);
    return this;
  }

  public IssueDoc setDebt(@Nullable Long l) {
    setField(IssueIndexDefinition.FIELD_ISSUE_DEBT, l);
    return this;
  }

  public IssueDoc setFilePath(@Nullable String s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_FILE_PATH, s);
    return this;
  }

  public IssueDoc setDirectoryPath(@Nullable String s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_DIRECTORY_PATH, s);
    return this;
  }

  public IssueDoc setModuleUuidPath(@Nullable String s) {
    setField(IssueIndexDefinition.FIELD_ISSUE_MODULE_PATH, s);
    return this;
  }

  @Override
  @CheckForNull
  public Collection<String> tags() {
    return getNullableField(IssueIndexDefinition.FIELD_ISSUE_TAGS);
  }

  public IssueDoc setTags(@Nullable Collection<String> tags) {
    setField(IssueIndexDefinition.FIELD_ISSUE_TAGS, tags);
    return this;
  }
}
