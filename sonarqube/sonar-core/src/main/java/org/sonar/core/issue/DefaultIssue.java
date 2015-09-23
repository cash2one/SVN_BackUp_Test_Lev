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
package org.sonar.core.issue;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.commons.lang.time.DateUtils;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueComment;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.Duration;
import org.sonar.core.issue.tracking.Trackable;

import static java.lang.String.format;

public class DefaultIssue implements Issue, Trackable, org.sonar.api.ce.measure.Issue {

  private String key;

  private String componentUuid;
  private String componentKey;

  private String moduleUuid;
  private String moduleUuidPath;

  private String projectUuid;
  private String projectKey;

  private RuleKey ruleKey;
  private String language;
  private String severity;
  private boolean manualSeverity = false;
  private String message;
  private Integer line;
  private Double effortToFix;
  private Duration debt;
  private String status;
  private String resolution;
  private String reporter;
  private String assignee;
  private String checksum;
  private Map<String, String> attributes = null;
  private String authorLogin = null;
  private String actionPlanKey;
  private List<IssueComment> comments = null;
  private Set<String> tags = null;
  // temporarily an Object as long as DefaultIssue is used by sonar-batch
  private Object locations = null;
  // FUNCTIONAL DATES
  private Date creationDate;
  private Date updateDate;
  private Date closeDate;

  // FOLLOWING FIELDS ARE AVAILABLE ONLY DURING SCAN

  // Current changes
  private FieldDiffs currentChange = null;

  // all changes
  private List<FieldDiffs> changes = null;

  // true if the the issue did not exist in the previous scan.
  private boolean isNew = true;

  // True if the the issue did exist in the previous scan but not in the current one. That means
  // that this issue should be closed.
  private boolean beingClosed = false;

  private boolean onDisabledRule = false;

  // true if some fields have been changed since the previous scan
  private boolean isChanged = false;

  // true if notifications have to be sent
  private boolean sendNotifications = false;

  // Date when issue was loaded from db (only when isNew=false)
  private Long selectedAt;

  @Override
  public String key() {
    return key;
  }

  public DefaultIssue setKey(String key) {
    this.key = key;
    return this;
  }

  /**
   * Can be null on Views or Devs
   */
  @Override
  @CheckForNull
  public String componentUuid() {
    return componentUuid;
  }

  public DefaultIssue setComponentUuid(@CheckForNull String componentUuid) {
    this.componentUuid = componentUuid;
    return this;
  }

  @Override
  public String componentKey() {
    return componentKey;
  }

  public DefaultIssue setComponentKey(String s) {
    this.componentKey = s;
    return this;
  }

  @CheckForNull
  public String moduleUuid() {
    return moduleUuid;
  }

  public DefaultIssue setModuleUuid(@Nullable String moduleUuid) {
    this.moduleUuid = moduleUuid;
    return this;
  }

  @CheckForNull
  public String moduleUuidPath() {
    return moduleUuidPath;
  }

  public DefaultIssue setModuleUuidPath(@Nullable String moduleUuidPath) {
    this.moduleUuidPath = moduleUuidPath;
    return this;
  }

  /**
   * Can be null on Views or Devs
   */
  @Override
  @CheckForNull
  public String projectUuid() {
    return projectUuid;
  }

  public DefaultIssue setProjectUuid(@Nullable String projectUuid) {
    this.projectUuid = projectUuid;
    return this;
  }

  @Override
  public String projectKey() {
    return projectKey;
  }

  public DefaultIssue setProjectKey(String projectKey) {
    this.projectKey = projectKey;
    return this;
  }

  @Override
  public RuleKey ruleKey() {
    return ruleKey;
  }

  public DefaultIssue setRuleKey(RuleKey k) {
    this.ruleKey = k;
    return this;
  }

  @Override
  public String language() {
    return language;
  }

  public DefaultIssue setLanguage(String l) {
    this.language = l;
    return this;
  }

  @Override
  public String severity() {
    return severity;
  }

  public DefaultIssue setSeverity(@Nullable String s) {
    Preconditions.checkArgument(s == null || Severity.ALL.contains(s), "Not a valid severity: " + s);
    this.severity = s;
    return this;
  }

  public boolean manualSeverity() {
    return manualSeverity;
  }

  public DefaultIssue setManualSeverity(boolean b) {
    this.manualSeverity = b;
    return this;
  }

  @Override
  @CheckForNull
  public String message() {
    return message;
  }

  public DefaultIssue setMessage(@Nullable String s) {
    this.message = StringUtils.abbreviate(StringUtils.trim(s), MESSAGE_MAX_SIZE);
    return this;
  }

  @Override
  @CheckForNull
  public Integer line() {
    return line;
  }

  public DefaultIssue setLine(@Nullable Integer l) {
    Preconditions.checkArgument(l == null || l > 0, format("Line must be null or greater than zero (got %d)", l));
    this.line = l;
    return this;
  }

  @Override
  @CheckForNull
  public Double effortToFix() {
    return effortToFix;
  }

  public DefaultIssue setEffortToFix(@Nullable Double d) {
    Preconditions.checkArgument(d == null || d >= 0, format("Effort to fix must be greater than or equal 0 (got %s)", d));
    this.effortToFix = d;
    return this;
  }

  /**
   * Elapsed time to fix the issue
   */
  @Override
  @CheckForNull
  public Duration debt() {
    return debt;
  }

  @CheckForNull
  public Long debtInMinutes() {
    return debt != null ? debt.toMinutes() : null;
  }

  public DefaultIssue setDebt(@Nullable Duration t) {
    this.debt = t;
    return this;
  }

  @Override
  public String status() {
    return status;
  }

  public DefaultIssue setStatus(String s) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(s), "Status must be set");
    this.status = s;
    return this;
  }

  @Override
  @CheckForNull
  public String resolution() {
    return resolution;
  }

  public DefaultIssue setResolution(@Nullable String s) {
    this.resolution = s;
    return this;
  }

  @Override
  @CheckForNull
  public String reporter() {
    return reporter;
  }

  public DefaultIssue setReporter(@Nullable String s) {
    this.reporter = s;
    return this;
  }

  @Override
  @CheckForNull
  public String assignee() {
    return assignee;
  }

  public DefaultIssue setAssignee(@Nullable String s) {
    this.assignee = s;
    return this;
  }

  @Override
  public Date creationDate() {
    return creationDate;
  }

  public DefaultIssue setCreationDate(Date d) {
    // d is not marked as Nullable but we still allow null parameter for unit testing.
    this.creationDate = d != null ? DateUtils.truncate(d, Calendar.SECOND) : null;
    return this;
  }

  @Override
  @CheckForNull
  public Date updateDate() {
    return updateDate;
  }

  public DefaultIssue setUpdateDate(@Nullable Date d) {
    this.updateDate = d != null ? DateUtils.truncate(d, Calendar.SECOND) : null;
    return this;
  }

  @Override
  @CheckForNull
  public Date closeDate() {
    return closeDate;
  }

  public DefaultIssue setCloseDate(@Nullable Date d) {
    this.closeDate = d != null ? DateUtils.truncate(d, Calendar.SECOND) : null;
    return this;
  }

  @CheckForNull
  public String checksum() {
    return checksum;
  }

  public DefaultIssue setChecksum(@Nullable String s) {
    this.checksum = s;
    return this;
  }

  @Override
  public boolean isNew() {
    return isNew;
  }

  public DefaultIssue setNew(boolean b) {
    isNew = b;
    return this;
  }

  /**
   * True when one of the following conditions is true :
   * <ul>
   * <li>the related component has been deleted or renamed</li>
   * <li>the rule has been deleted (eg. on plugin uninstall)</li>
   * <li>the rule has been disabled in the Quality profile</li>
   * </ul>
   */
  public boolean isBeingClosed() {
    return beingClosed;
  }

  public DefaultIssue setBeingClosed(boolean b) {
    beingClosed = b;
    return this;
  }

  public boolean isOnDisabledRule() {
    return onDisabledRule;
  }

  public DefaultIssue setOnDisabledRule(boolean b) {
    onDisabledRule = b;
    return this;
  }

  public boolean isChanged() {
    return isChanged;
  }

  public DefaultIssue setChanged(boolean b) {
    isChanged = b;
    return this;
  }

  public boolean mustSendNotifications() {
    return sendNotifications;
  }

  public DefaultIssue setSendNotifications(boolean b) {
    sendNotifications = b;
    return this;
  }

  @Override
  @CheckForNull
  public String attribute(String key) {
    return attributes == null ? null : attributes.get(key);
  }

  public DefaultIssue setAttribute(String key, @Nullable String value) {
    if (attributes == null) {
      attributes = Maps.newHashMap();
    }
    if (value == null) {
      attributes.remove(key);
    } else {
      attributes.put(key, value);
    }
    return this;
  }

  @Override
  public Map<String, String> attributes() {
    return attributes == null ? Collections.<String, String>emptyMap() : ImmutableMap.copyOf(attributes);
  }

  public DefaultIssue setAttributes(@Nullable Map<String, String> map) {
    if (map != null) {
      if (attributes == null) {
        attributes = Maps.newHashMap();
      }
      attributes.putAll(map);
    }
    return this;
  }

  @Override
  @CheckForNull
  public String authorLogin() {
    return authorLogin;
  }

  public DefaultIssue setAuthorLogin(@Nullable String s) {
    this.authorLogin = s;
    return this;
  }

  @Override
  @CheckForNull
  public String actionPlanKey() {
    return actionPlanKey;
  }

  public DefaultIssue setActionPlanKey(@Nullable String actionPlanKey) {
    this.actionPlanKey = actionPlanKey;
    return this;
  }

  public DefaultIssue setFieldChange(IssueChangeContext context, String field, @Nullable Serializable oldValue, @Nullable Serializable newValue) {
    if (!Objects.equal(oldValue, newValue)) {
      if (currentChange == null) {
        currentChange = new FieldDiffs();
        currentChange.setUserLogin(context.login());
        currentChange.setCreationDate(context.date());
      }
      currentChange.setDiff(field, oldValue, newValue);
    }
    addChange(currentChange);
    return this;
  }

  public DefaultIssue setCurrentChange(FieldDiffs currentChange) {
    this.currentChange = currentChange;
    addChange(currentChange);
    return this;
  }

  @CheckForNull
  public FieldDiffs currentChange() {
    return currentChange;
  }

  public DefaultIssue addChange(FieldDiffs change) {
    if (changes == null) {
      changes = new ArrayList<>();
    }
    changes.add(change);
    return this;
  }

  public DefaultIssue setChanges(List<FieldDiffs> changes) {
    this.changes = changes;
    return this;
  }

  public List<FieldDiffs> changes() {
    if (changes == null) {
      return Collections.emptyList();
    }
    return ImmutableList.copyOf(changes);
  }

  public DefaultIssue addComment(DefaultIssueComment comment) {
    if (comments == null) {
      comments = new ArrayList<>();
    }
    comments.add(comment);
    return this;
  }

  @Override
  public List<IssueComment> comments() {
    if (comments == null) {
      return Collections.emptyList();
    }
    return ImmutableList.copyOf(comments);
  }

  @CheckForNull
  public Long selectedAt() {
    return selectedAt;
  }

  public DefaultIssue setSelectedAt(@Nullable Long d) {
    this.selectedAt = d;
    return this;
  }

  public <T> T getLocations() {
    return (T)locations;
  }

  public void setLocations(Object locations) {
    this.locations = locations;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DefaultIssue that = (DefaultIssue) o;
    return !(key != null ? !key.equals(that.key) : (that.key != null));
  }

  @Override
  public int hashCode() {
    return key != null ? key.hashCode() : 0;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

  @Override
  public Set<String> tags() {
    if (tags == null) {
      return ImmutableSet.of();
    } else {
      return ImmutableSet.copyOf(tags);
    }
  }

  public DefaultIssue setTags(Collection<String> tags) {
    this.tags = new LinkedHashSet<>(tags);
    return this;
  }

  @Override
  public Integer getLine() {
    return line;
  }

  @Override
  public String getMessage() {
    return message;
  }

  @Override
  public String getLineHash() {
    return checksum;
  }

  @Override
  public RuleKey getRuleKey() {
    return ruleKey;
  }
}
