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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.issue.IssueMapper;
import org.sonar.server.computation.component.TreeRootHolder;
import org.sonar.server.computation.qualityprofile.ActiveRulesHolder;

/**
 * Loads all the project open issues from database, including manual issues.
 *
 */
public class BaseIssuesLoader {

  private final TreeRootHolder treeRootHolder;
  private final DbClient dbClient;
  private final RuleRepository ruleRepository;
  private final ActiveRulesHolder activeRulesHolder;

  public BaseIssuesLoader(TreeRootHolder treeRootHolder,
    DbClient dbClient, RuleRepository ruleRepository, ActiveRulesHolder activeRulesHolder) {
    this.activeRulesHolder = activeRulesHolder;
    this.treeRootHolder = treeRootHolder;
    this.dbClient = dbClient;
    this.ruleRepository = ruleRepository;
  }

  public List<DefaultIssue> loadForComponentUuid(String componentUuid) {
    DbSession session = dbClient.openSession(false);
    final List<DefaultIssue> result = new ArrayList<>();
    try {
      session.getMapper(IssueMapper.class).selectNonClosedByComponentUuid(componentUuid, new ResultHandler() {
        @Override
        public void handleResult(ResultContext resultContext) {
          DefaultIssue issue = ((IssueDto) resultContext.getResultObject()).toDefaultIssue();

          // TODO this field should be set outside this class
          if (!isActive(issue.ruleKey()) || ruleRepository.getByKey(issue.ruleKey()).getStatus() == RuleStatus.REMOVED) {
            issue.setOnDisabledRule(true);
            // TODO to be improved, why setOnDisabledRule(true) is not enough ?
            issue.setBeingClosed(true);
          }
          // FIXME
          issue.setSelectedAt(System.currentTimeMillis());
          result.add(issue);
        }
      });
      return result;
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private boolean isActive(RuleKey ruleKey) {
    return ruleKey.isManual() || activeRulesHolder.get(ruleKey).isPresent();
  }

  /**
   * Uuids of all the components that have open issues on this project.
   */
  public Set<String> loadUuidsOfComponentsWithOpenIssues() {
    DbSession session = dbClient.openSession(false);
    try {
      return dbClient.issueDao().selectComponentUuidsOfOpenIssuesForProjectUuid(session, treeRootHolder.getRoot().getUuid());
    } finally {
      MyBatis.closeQuietly(session);
    }
  }
}
