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
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.issue.IssueMapper;
import org.sonar.test.DbTests;

@Category(DbTests.class)
public class IssueStorageTest {

  IssueChangeContext context = IssueChangeContext.createUser(new Date(), "emmerik");

  @org.junit.Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  DbClient dbClient = dbTester.getDbClient();

  @Test
  public void batch_insert_new_issues() {
    FakeBatchSaver saver = new FakeBatchSaver(dbClient, new FakeRuleFinder());

    DefaultIssueComment comment = DefaultIssueComment.create("ABCDE", "emmerik", "the comment");
    // override generated key
    comment.setKey("FGHIJ");

    Date date = DateUtils.parseDateTime("2013-05-18T12:00:00+0000");
    DefaultIssue issue = new DefaultIssue()
      .setKey("ABCDE")
      .setNew(true)

      .setRuleKey(RuleKey.of("squid", "AvoidCycle"))
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

      .setComponentUuid("uuid-100")
      .setProjectUuid("uuid-10")
      .setComponentKey("struts:Action");

    saver.save(issue);

    dbTester.assertDbUnit(getClass(), "should_insert_new_issues-result.xml",
      new String[]{"id", "created_at", "updated_at", "issue_change_creation_date"}, "issues", "issue_changes");
  }

  @Test
  public void batch_insert_new_issues_with_session() {
    FakeBatchSaver saver = new FakeBatchSaver(dbClient, new FakeRuleFinder());

    DefaultIssueComment comment = DefaultIssueComment.create("ABCDE", "emmerik", "the comment");
    // override generated key
    comment.setKey("FGHIJ");

    Date date = DateUtils.parseDateTime("2013-05-18T12:00:00+0000");
    DefaultIssue issue = new DefaultIssue()
      .setKey("ABCDE")
      .setNew(true)

      .setRuleKey(RuleKey.of("squid", "AvoidCycle"))
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

      .setComponentUuid("uuid-100")
      .setProjectUuid("uuid-10")
      .setComponentKey("struts:Action");

    saver.save(dbTester.getSession(), issue);
    dbTester.getSession().commit();

    dbTester.assertDbUnit(getClass(), "should_insert_new_issues-result.xml",
      new String[]{"id", "created_at", "updated_at", "issue_change_creation_date"}, "issues", "issue_changes");
  }

  @Test
  public void server_insert_new_issues_with_session() {
    ComponentDto project = new ComponentDto().setId(10L).setUuid("uuid-10");
    ComponentDto component = new ComponentDto().setId(100L).setUuid("uuid-100");
    FakeServerSaver saver = new FakeServerSaver(dbClient, new FakeRuleFinder(), component, project);

    DefaultIssueComment comment = DefaultIssueComment.create("ABCDE", "emmerik", "the comment");
    // override generated key
    comment.setKey("FGHIJ");

    Date date = DateUtils.parseDateTime("2013-05-18T12:00:00+0000");
    DefaultIssue issue = new DefaultIssue()
      .setKey("ABCDE")
      .setNew(true)

      .setRuleKey(RuleKey.of("squid", "AvoidCycle"))
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

      .setComponentKey("struts:Action")
      .setComponentUuid("component-uuid")
      .setProjectUuid("project-uuid");

    saver.save(dbTester.getSession(), issue);
    dbTester.getSession().commit();

    dbTester.assertDbUnit(getClass(), "should_insert_new_issues-result.xml",
      new String[]{"id", "created_at", "updated_at", "issue_change_creation_date"}, "issues", "issue_changes");
  }

  @Test
  public void batch_update_issues() {
    dbTester.prepareDbUnit(getClass(), "should_update_issues.xml");

    FakeBatchSaver saver = new FakeBatchSaver(dbClient, new FakeRuleFinder());

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
      .setComponentUuid("uuid-100")
      .setProjectUuid("uuid-10")

        // unmodifiable fields
      .setRuleKey(RuleKey.of("xxx", "unknown"))
      .setComponentKey("not:a:component");

    saver.save(issue);

    dbTester.assertDbUnit(getClass(), "should_update_issues-result.xml", new String[]{"id", "created_at", "updated_at", "issue_change_creation_date"}, "issues", "issue_changes");
  }

  @Test
  public void server_update_issues() {
    dbTester.prepareDbUnit(getClass(), "should_update_issues.xml");

    ComponentDto project = new ComponentDto().setId(10L).setUuid("whatever-uuid");
    ComponentDto component = new ComponentDto().setId(100L).setUuid("whatever-uuid-2");
    FakeServerSaver saver = new FakeServerSaver(dbClient, new FakeRuleFinder(), component, project);

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
      .setProjectUuid("uuid-10")

        // unmodifiable fields
      .setRuleKey(RuleKey.of("xxx", "unknown"))
      .setComponentKey("not:a:component");

    saver.save(issue);

    dbTester.assertDbUnit(getClass(), "should_update_issues-result.xml", new String[]{"id", "created_at", "updated_at", "issue_change_creation_date"}, "issues", "issue_changes");
  }

  static class FakeBatchSaver extends IssueStorage {

    protected FakeBatchSaver(DbClient dbClient, RuleFinder ruleFinder) {
      super(dbClient, ruleFinder);
    }

    @Override
    protected void doInsert(DbSession session, long now, DefaultIssue issue) {
      int ruleId = rule(issue).getId();
      IssueDto dto = IssueDto.toDtoForComputationInsert(issue, ruleId, now);

      session.getMapper(IssueMapper.class).insert(dto);
    }

    @Override
    protected void doUpdate(DbSession session, long now, DefaultIssue issue) {
      IssueDto dto = IssueDto.toDtoForUpdate(issue, now);
      session.getMapper(IssueMapper.class).update(dto);
    }
  }

  static class FakeServerSaver extends IssueStorage {

    private final ComponentDto component;
    private final ComponentDto project;

    protected FakeServerSaver(DbClient dbClient, RuleFinder ruleFinder, ComponentDto component, ComponentDto project) {
      super(dbClient, ruleFinder);
      this.component = component;
      this.project = project;
    }

    @Override
    protected void doInsert(DbSession session, long now, DefaultIssue issue) {
      int ruleId = rule(issue).getId();
      IssueDto dto = IssueDto.toDtoForServerInsert(issue, component, project, ruleId, now);

      session.getMapper(IssueMapper.class).insert(dto);
    }

    @Override
    protected void doUpdate(DbSession session, long now, DefaultIssue issue) {
      IssueDto dto = IssueDto.toDtoForUpdate(issue, now);
      session.getMapper(IssueMapper.class).update(dto);
    }
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
      Rule rule = new Rule().setRepositoryKey(key.repository()).setKey(key.rule());
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
