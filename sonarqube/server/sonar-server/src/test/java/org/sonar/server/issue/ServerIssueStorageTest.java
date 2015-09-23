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

import java.util.Collection;
import java.util.Date;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RuleQuery;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.System2;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.DefaultIssueComment;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Category(DbTests.class)
public class ServerIssueStorageTest {

  System2 system = mock(System2.class);

  @org.junit.Rule
  public DbTester dbTester = DbTester.create(system);

  DbClient dbClient = dbTester.getDbClient();

  ServerIssueStorage storage = new ServerIssueStorage(new FakeRuleFinder(), dbClient, mock(IssueIndexer.class));

  @Before
  public void setupDbClient() {
    when(system.now()).thenReturn(2000000000L);
  }

  @Test
  public void load_component_id_from_db() {
    dbTester.prepareDbUnit(getClass(), "load_component_id_from_db.xml");

    long componentId = storage.component(dbTester.getSession(), new DefaultIssue().setComponentKey("struts:Action")).getId();

    assertThat(componentId).isEqualTo(100);
  }

  @Test
  public void load_project_id_from_db() {
    dbTester.prepareDbUnit(getClass(), "load_project_id_from_db.xml");

    long projectId = storage.project(dbTester.getSession(), new DefaultIssue().setProjectKey("struts")).getId();

    assertThat(projectId).isEqualTo(1);
  }

  @Test
  public void should_insert_new_issues() {
    dbTester.prepareDbUnit(getClass(), "should_insert_new_issues.xml");

    DefaultIssueComment comment = DefaultIssueComment.create("ABCDE", "emmerik", "the comment");
    // override generated key
    comment.setKey("FGHIJ");

    Date date = DateUtils.parseDateTime("2013-05-18T12:00:00+0000");
    DefaultIssue issue = new DefaultIssue()
      .setKey("ABCDE")
      .setNew(true)

      .setRuleKey(RuleKey.of("squid", "AvoidCycle"))
      .setProjectKey("struts")
      .setLine(5000)
      .setDebt(Duration.create(10L))
      .setReporter("emmerik")
      .setResolution("OPEN")
      .setStatus("OPEN")
      .setSeverity("BLOCKER")
      .setAttribute("foo", "bar")
      .addComment(comment)
      .setCreationDate(date)
      .setUpdateDate(date)
      .setCloseDate(date)

      .setComponentKey("struts:Action");

    storage.save(issue);

    dbTester.assertDbUnit(getClass(), "should_insert_new_issues-result.xml",
      new String[]{"id", "created_at", "updated_at", "issue_change_creation_date"}, "issues", "issue_changes");
  }

  @Test
  public void should_update_issues() {
    dbTester.prepareDbUnit(getClass(), "should_update_issues.xml");

    IssueChangeContext context = IssueChangeContext.createUser(new Date(), "emmerik");

    DefaultIssueComment comment = DefaultIssueComment.create("ABCDE", "emmerik", "the comment");
    // override generated key
    comment.setKey("FGHIJ");

    Date date = DateUtils.parseDateTime("2013-05-18T12:00:00+0000");
    DefaultIssue issue = new DefaultIssue()
      .setKey("ABCDE")
      .setNew(false)
      .setChanged(true)

        // updated fields
      .setLine(5000)
      .setProjectUuid("CDEF")
      .setDebt(Duration.create(10L))
      .setChecksum("FFFFF")
      .setAuthorLogin("simon")
      .setAssignee("loic")
      .setFieldChange(context, "severity", "INFO", "BLOCKER")
      .setReporter("emmerik")
      .setResolution("FIXED")
      .setStatus("RESOLVED")
      .setSeverity("BLOCKER")
      .setAttribute("foo", "bar")
      .addComment(comment)
      .setCreationDate(date)
      .setUpdateDate(date)
      .setCloseDate(date)

        // unmodifiable fields
      .setRuleKey(RuleKey.of("xxx", "unknown"))
      .setComponentKey("struts:Action")
      .setProjectKey("struts");

    storage.save(issue);

    dbTester.assertDbUnit(getClass(), "should_update_issues-result.xml",
      new String[]{"id", "created_at", "updated_at", "issue_change_creation_date"}, "issues", "issue_changes");
  }

  static class FakeRuleFinder implements RuleFinder {

    @Override
    public Rule findById(int ruleId) {
      return null;
    }

    @Override
    public Rule findByKey(String repositoryKey, String key) {
      return null;
    }

    @Override
    public Rule findByKey(RuleKey key) {
      Rule rule = Rule.create().setRepositoryKey(key.repository()).setKey(key.rule());
      rule.setId(200);
      return rule;
    }

    @Override
    public Rule find(RuleQuery query) {
      return null;
    }

    @Override
    public Collection<Rule> findAll(RuleQuery query) {
      return null;
    }
  }
}
