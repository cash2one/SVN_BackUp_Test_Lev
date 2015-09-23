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
package org.sonar.server.issue;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.issue.ActionPlan;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.server.ServerSide;
import org.sonar.api.user.User;
import org.sonar.api.user.UserFinder;
import org.sonar.api.web.UserRole;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.DefaultIssueBuilder;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.core.issue.IssueUpdater;
import org.sonar.core.issue.workflow.IssueWorkflow;
import org.sonar.core.issue.workflow.Transition;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.es.SearchResult;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.issue.actionplan.ActionPlanService;
import org.sonar.server.issue.index.IssueDoc;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.notification.IssueChangeNotification;
import org.sonar.server.notification.NotificationManager;
import org.sonar.server.source.SourceService;
import org.sonar.server.user.UserSession;
import org.sonar.server.user.index.UserDoc;
import org.sonar.server.user.index.UserIndex;

@ServerSide
public class IssueService {

  private final DbClient dbClient;
  private final IssueIndex issueIndex;

  private final IssueWorkflow workflow;
  private final IssueUpdater issueUpdater;
  private final IssueStorage issueStorage;
  private final NotificationManager notificationService;
  private final ActionPlanService actionPlanService;
  private final RuleFinder ruleFinder;
  private final UserFinder userFinder;
  private final UserIndex userIndex;
  private final SourceService sourceService;
  private final UserSession userSession;

  public IssueService(DbClient dbClient, IssueIndex issueIndex,
    IssueWorkflow workflow,
    IssueStorage issueStorage,
    IssueUpdater issueUpdater,
    NotificationManager notificationService,
    ActionPlanService actionPlanService,
    RuleFinder ruleFinder,
    UserFinder userFinder,
    UserIndex userIndex, SourceService sourceService, UserSession userSession) {
    this.dbClient = dbClient;
    this.issueIndex = issueIndex;
    this.workflow = workflow;
    this.issueStorage = issueStorage;
    this.issueUpdater = issueUpdater;
    this.actionPlanService = actionPlanService;
    this.ruleFinder = ruleFinder;
    this.notificationService = notificationService;
    this.userFinder = userFinder;
    this.userIndex = userIndex;
    this.sourceService = sourceService;
    this.userSession = userSession;
  }

  public List<String> listStatus() {
    return workflow.statusKeys();
  }

  /**
   * List of available transitions.
   * <p>
   * Never return null, but return an empty list if the issue does not exist.
   */
  public List<Transition> listTransitions(String issueKey) {
    DbSession session = dbClient.openSession(false);
    try {
      return listTransitions(getByKeyForUpdate(session, issueKey).toDefaultIssue());
    } finally {
      session.close();
    }
  }

  /**
   * Never return null, but an empty list if the issue does not exist.
   * No security check is done since it should already have been done to get the issue
   */
  public List<Transition> listTransitions(@Nullable Issue issue) {
    if (issue == null) {
      return Collections.emptyList();
    }
    List<Transition> outTransitions = workflow.outTransitions(issue);
    List<Transition> allowedTransitions = new ArrayList<>();
    for (Transition transition : outTransitions) {
      String projectUuid = issue.projectUuid();
      if (userSession.isLoggedIn() && StringUtils.isBlank(transition.requiredProjectPermission()) ||
        (projectUuid != null && userSession.hasProjectPermissionByUuid(transition.requiredProjectPermission(), projectUuid))) {
        allowedTransitions.add(transition);
      }
    }
    return allowedTransitions;
  }

  public Issue doTransition(String issueKey, String transitionKey) {
    verifyLoggedIn();

    DbSession session = dbClient.openSession(false);
    try {
      DefaultIssue defaultIssue = getByKeyForUpdate(session, issueKey).toDefaultIssue();
      IssueChangeContext context = IssueChangeContext.createUser(new Date(), userSession.getLogin());
      checkTransitionPermission(transitionKey, userSession, defaultIssue);
      if (workflow.doTransition(defaultIssue, transitionKey, context)) {
        saveIssue(session, defaultIssue, context, null);
      }
      return defaultIssue;

    } finally {
      session.close();
    }
  }

  private void checkTransitionPermission(String transitionKey, UserSession userSession, DefaultIssue defaultIssue) {
    List<Transition> outTransitions = workflow.outTransitions(defaultIssue);
    for (Transition transition : outTransitions) {
      String projectKey = defaultIssue.projectKey();
      if (transition.key().equals(transitionKey) && StringUtils.isNotBlank(transition.requiredProjectPermission()) && projectKey != null) {
        userSession.checkProjectPermission(transition.requiredProjectPermission(), projectKey);
      }
    }
  }

  public Issue assign(String issueKey, @Nullable String assignee) {
    verifyLoggedIn();

    DbSession session = dbClient.openSession(false);
    try {
      DefaultIssue issue = getByKeyForUpdate(session, issueKey).toDefaultIssue();
      User user = null;
      if (!Strings.isNullOrEmpty(assignee)) {
        user = userFinder.findByLogin(assignee);
        if (user == null) {
          throw new BadRequestException("Unknown user: " + assignee);
        }
      }
      IssueChangeContext context = IssueChangeContext.createUser(new Date(), userSession.getLogin());
      if (issueUpdater.assign(issue, user, context)) {
        saveIssue(session, issue, context, null);
      }
      return issue;

    } finally {
      session.close();
    }
  }

  public Issue plan(String issueKey, @Nullable String actionPlanKey) {
    verifyLoggedIn();

    DbSession session = dbClient.openSession(false);
    try {
      ActionPlan actionPlan = null;
      if (!Strings.isNullOrEmpty(actionPlanKey)) {
        actionPlan = actionPlanService.findByKey(actionPlanKey, userSession);
        if (actionPlan == null) {
          throw new BadRequestException("Unknown action plan: " + actionPlanKey);
        }
      }
      DefaultIssue issue = getByKeyForUpdate(session, issueKey).toDefaultIssue();

      IssueChangeContext context = IssueChangeContext.createUser(new Date(), userSession.getLogin());
      if (issueUpdater.plan(issue, actionPlan, context)) {
        saveIssue(session, issue, context, null);
      }
      return issue;

    } finally {
      session.close();
    }
  }

  public Issue setSeverity(String issueKey, String severity) {
    verifyLoggedIn();

    DbSession session = dbClient.openSession(false);
    try {
      DefaultIssue issue = getByKeyForUpdate(session, issueKey).toDefaultIssue();
      userSession.checkProjectPermission(UserRole.ISSUE_ADMIN, issue.projectKey());

      IssueChangeContext context = IssueChangeContext.createUser(new Date(), userSession.getLogin());
      if (issueUpdater.setManualSeverity(issue, severity, context)) {
        saveIssue(session, issue, context, null);
      }
      return issue;
    } finally {
      session.close();
    }
  }

  public DefaultIssue createManualIssue(String componentKey, RuleKey ruleKey, @Nullable Integer line, @Nullable String message, @Nullable String severity) {
    verifyLoggedIn();

    DbSession dbSession = dbClient.openSession(false);
    try {
      Optional<ComponentDto> componentOptional = dbClient.componentDao().selectByKey(dbSession, componentKey);
      if (!componentOptional.isPresent()) {
        throw new BadRequestException(String.format("Component with key '%s' not found", componentKey));
      }
      ComponentDto component = componentOptional.get();
      ComponentDto project = dbClient.componentDao().selectOrFailByUuid(dbSession, component.projectUuid());

      userSession.checkProjectPermission(UserRole.USER, project.getKey());
      if (!ruleKey.isManual()) {
        throw new IllegalArgumentException("Issues can be created only on rules marked as 'manual': " + ruleKey);
      }
      Rule rule = getNullableRuleByKey(ruleKey);
      if (rule == null) {
        throw new IllegalArgumentException("Unknown rule: " + ruleKey);
      }

      DefaultIssue issue = new DefaultIssueBuilder()
        .componentKey(component.getKey())
        .projectKey(project.getKey())
        .line(line)
        .message(!Strings.isNullOrEmpty(message) ? message : rule.getName())
        .severity(Objects.firstNonNull(severity, Severity.MAJOR))
        .ruleKey(ruleKey)
        .reporter(userSession.getLogin())
        .assignee(findSourceLineUser(dbSession, component.uuid(), line))
        .build();

      Date now = new Date();
      issue.setCreationDate(now);
      issue.setUpdateDate(now);
      issueStorage.save(issue);
      return issue;
    } finally {
      dbSession.close();
    }
  }

  public Issue getByKey(String key) {
    return issueIndex.getByKey(key);
  }

  IssueDto getByKeyForUpdate(DbSession session, String key) {
    // Load from index to check permission : if the user has no permission to see the issue an exception will be generated
    Issue authorizedIssueIndex = getByKey(key);
    return dbClient.issueDao().selectOrFailByKey(session, authorizedIssueIndex.key());
  }

  void saveIssue(DbSession session, DefaultIssue issue, IssueChangeContext context, @Nullable String comment) {
    String projectKey = issue.projectKey();
    if (projectKey == null) {
      throw new IllegalStateException(String.format("Issue '%s' has no project key", issue.key()));
    }
    issueStorage.save(session, issue);
    Rule rule = getNullableRuleByKey(issue.ruleKey());
    ComponentDto project = dbClient.componentDao().selectOrFailByKey(session, projectKey);
    notificationService.scheduleForSending(new IssueChangeNotification()
      .setIssue(issue)
      .setChangeAuthorLogin(context.login())
      .setRuleName(rule != null ? rule.getName() : null)
      .setProject(project.getKey(), project.name())
      .setComponent(dbClient.componentDao().selectOrFailByKey(session, issue.componentKey()))
      .setComment(comment));
  }

  /**
   * Should use {@link org.sonar.server.rule.RuleService#getByKey(org.sonar.api.rule.RuleKey)}, but it's not possible as IssueNotifications is still used by the batch.
   * Can be null for removed rules
   */
  private Rule getNullableRuleByKey(RuleKey ruleKey) {
    return ruleFinder.findByKey(ruleKey);
  }

  public SearchResult<IssueDoc> search(IssueQuery query, SearchOptions options) {
    return issueIndex.search(query, options);
  }

  private void verifyLoggedIn() {
    userSession.checkLoggedIn();
  }

  /**
   * Search for all tags, whatever issue resolution or user access rights
   */
  public List<String> listTags(@Nullable String textQuery, int pageSize) {
    IssueQuery query = IssueQuery.builder(userSession)
      .checkAuthorization(false)
      .build();
    return issueIndex.listTags(query, textQuery, pageSize);
  }

  public List<String> listAuthors(@Nullable String textQuery, int pageSize) {
    IssueQuery query = IssueQuery.builder(userSession)
      .checkAuthorization(false)
      .build();
    return issueIndex.listAuthors(query, textQuery, pageSize);
  }

  public Collection<String> setTags(String issueKey, Collection<String> tags) {
    verifyLoggedIn();

    DbSession session = dbClient.openSession(false);
    try {
      DefaultIssue issue = getByKeyForUpdate(session, issueKey).toDefaultIssue();
      IssueChangeContext context = IssueChangeContext.createUser(new Date(), userSession.getLogin());
      if (issueUpdater.setTags(issue, tags, context)) {
        saveIssue(session, issue, context, null);
      }
      return issue.tags();

    } finally {
      session.close();
    }
  }

  public Map<String, Long> listTagsForComponent(IssueQuery query, int pageSize) {
    return issueIndex.countTags(query, pageSize);
  }

  @CheckForNull
  private String findSourceLineUser(DbSession dbSession, String fileUuid, @Nullable Integer line) {
    if (line != null) {
      Optional<DbFileSources.Line> sourceLine = sourceService.getLine(dbSession, fileUuid, line);
      if (sourceLine.isPresent() && sourceLine.get().hasScmAuthor()) {
        UserDoc userDoc = userIndex.getNullableByScmAccount(sourceLine.get().getScmAuthor());
        if (userDoc != null) {
          return userDoc.login();
        }
      }
    }
    return null;
  }
}
