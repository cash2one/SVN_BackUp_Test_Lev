package issue.suite;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import java.util.List;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueQuery;

import static issue.suite.IssueTestSuite.adminIssueClient;
import static issue.suite.IssueTestSuite.searchIssueByKey;
import static issue.suite.IssueTestSuite.searchIssues;
import static issue.suite.IssueTestSuite.searchRandomIssue;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.projectDir;

public class IssueWorkflowTest {

  @ClassRule
  public static Orchestrator orchestrator = IssueTestSuite.ORCHESTRATOR;

  Issue issue;
  SonarRunner scan;

  @Before
  public void resetData() {
    orchestrator.resetData();

    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/issue/suite/IssueWorkflowTest/xoo-one-issue-per-line-profile.xml"));
    orchestrator.getServer().provisionProject("workflow", "Workflow");
    orchestrator.getServer().associateProjectToQualityProfile("workflow", "xoo", "xoo-one-issue-per-line-profile");

    scan = SonarRunner.create(projectDir("issue/workflow"));
    orchestrator.executeBuild(scan);

    issue = searchRandomIssue();
  }

  /**
   * Issue on a disabled rule (uninstalled plugin or rule deactivated from quality profile) must 
   * be CLOSED with resolution REMOVED
   */
  @Test
  public void issue_is_closed_as_removed_when_rule_is_disabled() throws Exception {
    List<Issue> issues = searchIssues(IssueQuery.create().rules("xoo:OneIssuePerLine"));
    assertThat(issues).isNotEmpty();

    // re-analyze with profile "empty". The rule is disabled so the issues must be closed
    orchestrator.getServer().associateProjectToQualityProfile("workflow", "xoo", "empty");
    orchestrator.executeBuild(scan);
    issues = searchIssues(IssueQuery.create().rules("xoo:OneIssuePerLine"));
    assertThat(issues).isNotEmpty();
    for (Issue issue : issues) {
      assertThat(issue.status()).isEqualTo("CLOSED");
      assertThat(issue.resolution()).isEqualTo("REMOVED");
    }
  }

  /**
   * SONAR-4329
   */
  @Test
  public void user_should_confirm_issue() {
    // mark as confirmed
    adminIssueClient().doTransition(issue.key(), "confirm");

    Issue confirmed = searchIssueByKey(issue.key());
    assertThat(confirmed.status()).isEqualTo("CONFIRMED");
    assertThat(confirmed.resolution()).isNull();
    assertThat(confirmed.creationDate()).isEqualTo(issue.creationDate());

    // user unconfirm the issue
    assertThat(adminIssueClient().transitions(confirmed.key())).contains("unconfirm");
    adminIssueClient().doTransition(confirmed.key(), "unconfirm");

    Issue unconfirmed = searchIssueByKey(issue.key());
    assertThat(unconfirmed.status()).isEqualTo("REOPENED");
    assertThat(unconfirmed.resolution()).isNull();
    assertThat(unconfirmed.creationDate()).isEqualTo(confirmed.creationDate());
  }

  /**
   * SONAR-4329
   */
  @Test
  public void user_should_mark_as_false_positive_confirmed_issue() {
    // mark as confirmed
    adminIssueClient().doTransition(issue.key(), "confirm");

    Issue confirmed = searchIssueByKey(issue.key());
    assertThat(confirmed.status()).isEqualTo("CONFIRMED");
    assertThat(confirmed.resolution()).isNull();
    assertThat(confirmed.creationDate()).isEqualTo(issue.creationDate());

    // user mark the issue as false-positive
    assertThat(adminIssueClient().transitions(confirmed.key())).contains("falsepositive");
    adminIssueClient().doTransition(confirmed.key(), "falsepositive");

    Issue falsePositive = searchIssueByKey(issue.key());
    assertThat(falsePositive.status()).isEqualTo("RESOLVED");
    assertThat(falsePositive.resolution()).isEqualTo("FALSE-POSITIVE");
    assertThat(falsePositive.creationDate()).isEqualTo(confirmed.creationDate());
  }

  /**
   * SONAR-4329
   */
  @Test
  public void scan_should_close_no_more_existing_confirmed() {
    // mark as confirmed
    adminIssueClient().doTransition(issue.key(), "confirm");
    Issue falsePositive = searchIssueByKey(issue.key());
    assertThat(falsePositive.status()).isEqualTo("CONFIRMED");
    assertThat(falsePositive.resolution()).isNull();
    assertThat(falsePositive.creationDate()).isEqualTo(issue.creationDate());

    // scan without any rules -> confirmed is closed
    orchestrator.getServer().associateProjectToQualityProfile("workflow", "xoo", "empty");
    orchestrator.executeBuild(scan);
    Issue closed = searchIssueByKey(issue.key());
    assertThat(closed.status()).isEqualTo("CLOSED");
    assertThat(closed.resolution()).isEqualTo("REMOVED");
    assertThat(closed.creationDate()).isEqualTo(issue.creationDate());
  }

  /**
   * SONAR-4288
   */
  @Test
  public void scan_should_reopen_unresolved_issue_but_marked_as_resolved() {
    // mark as resolved
    adminIssueClient().doTransition(issue.key(), "resolve");
    Issue resolvedIssue = searchIssueByKey(issue.key());
    assertThat(resolvedIssue.status()).isEqualTo("RESOLVED");
    assertThat(resolvedIssue.resolution()).isEqualTo("FIXED");
    assertThat(resolvedIssue.creationDate()).isEqualTo(issue.creationDate());
    assertThat(resolvedIssue.updateDate().before(resolvedIssue.creationDate())).isFalse();
    assertThat(resolvedIssue.updateDate().before(issue.updateDate())).isFalse();

    // re-execute scan, with the same Q profile -> the issue has not been fixed
    orchestrator.executeBuild(scan);

    // reload issue
    Issue reopenedIssue = searchIssueByKey(issue.key());

    // the issue has been reopened
    assertThat(reopenedIssue.status()).isEqualTo("REOPENED");
    assertThat(reopenedIssue.resolution()).isNull();
    assertThat(reopenedIssue.creationDate()).isEqualTo(issue.creationDate());
    assertThat(reopenedIssue.updateDate().before(issue.updateDate())).isFalse();
  }

  /**
   * SONAR-4288
   */
  @Test
  public void scan_should_close_resolved_issue() {
    // mark as resolved
    adminIssueClient().doTransition(issue.key(), "resolve");
    Issue resolvedIssue = searchIssueByKey(issue.key());
    assertThat(resolvedIssue.status()).isEqualTo("RESOLVED");
    assertThat(resolvedIssue.resolution()).isEqualTo("FIXED");
    assertThat(resolvedIssue.creationDate()).isEqualTo(issue.creationDate());
    assertThat(resolvedIssue.closeDate()).isNull();

    // re-execute scan without rules -> the issue is removed with resolution "REMOVED"
    orchestrator.getServer().associateProjectToQualityProfile("workflow", "xoo", "empty");
    orchestrator.executeBuild(scan);

    // reload issue
    Issue closedIssue = searchIssueByKey(issue.key());
    assertThat(closedIssue.status()).isEqualTo("CLOSED");
    assertThat(closedIssue.resolution()).isEqualTo("REMOVED");
    assertThat(closedIssue.creationDate()).isEqualTo(issue.creationDate());
    assertThat(closedIssue.updateDate().before(resolvedIssue.updateDate())).isFalse();
    assertThat(closedIssue.closeDate()).isNotNull();
    assertThat(closedIssue.closeDate().before(closedIssue.creationDate())).isFalse();
  }

  /**
   * SONAR-4288
   */
  @Test
  public void user_should_reopen_issue_marked_as_resolved() {
    // user marks issue as resolved
    adminIssueClient().doTransition(issue.key(), "resolve");
    Issue resolved = searchIssueByKey(issue.key());
    assertThat(resolved.status()).isEqualTo("RESOLVED");
    assertThat(resolved.resolution()).isEqualTo("FIXED");
    assertThat(resolved.creationDate()).isEqualTo(issue.creationDate());

    // user reopens the issue
    assertThat(adminIssueClient().transitions(resolved.key())).contains("reopen");
    adminIssueClient().doTransition(resolved.key(), "reopen");

    Issue reopened = searchIssueByKey(resolved.key());
    assertThat(reopened.status()).isEqualTo("REOPENED");
    assertThat(reopened.resolution()).isNull();
    assertThat(reopened.creationDate()).isEqualTo(resolved.creationDate());
    assertThat(reopened.updateDate().before(resolved.updateDate())).isFalse();
  }

  /**
   * SONAR-4286
   */
  @Test
  public void scan_should_not_reopen_or_close_false_positives() {
    // user marks issue as false-positive
    adminIssueClient().doTransition(issue.key(), "falsepositive");

    Issue falsePositive = searchIssueByKey(issue.key());
    assertThat(falsePositive.status()).isEqualTo("RESOLVED");
    assertThat(falsePositive.resolution()).isEqualTo("FALSE-POSITIVE");
    assertThat(falsePositive.creationDate()).isEqualTo(issue.creationDate());

    // re-execute the same scan
    orchestrator.executeBuild(scan);

    // refresh
    Issue reloaded = searchIssueByKey(falsePositive.key());
    assertThat(reloaded.status()).isEqualTo("RESOLVED");
    assertThat(reloaded.resolution()).isEqualTo("FALSE-POSITIVE");
    assertThat(reloaded.creationDate()).isEqualTo(issue.creationDate());
    // TODO check that update date has not been changed
  }

  /**
   * SONAR-4286
   */
  @Test
  public void scan_should_close_no_more_existing_false_positive() {
    // user marks as false-positive
    adminIssueClient().doTransition(issue.key(), "falsepositive");
    Issue falsePositive = searchIssueByKey(issue.key());
    assertThat(falsePositive.status()).isEqualTo("RESOLVED");
    assertThat(falsePositive.resolution()).isEqualTo("FALSE-POSITIVE");
    assertThat(falsePositive.creationDate()).isEqualTo(issue.creationDate());

    // scan without any rules -> false-positive is closed
    orchestrator.getServer().associateProjectToQualityProfile("workflow", "xoo", "empty");
    orchestrator.executeBuild(scan);
    Issue closed = searchIssueByKey(issue.key());
    assertThat(closed.status()).isEqualTo("CLOSED");
    assertThat(closed.resolution()).isEqualTo("REMOVED");
    assertThat(closed.creationDate()).isEqualTo(issue.creationDate());
  }

  /**
   * SONAR-4286
   */
  @Test
  public void user_should_reopen_false_positive() {
    // user marks as false-positive
    adminIssueClient().doTransition(issue.key(), "falsepositive");

    Issue falsePositive = searchIssueByKey(issue.key());
    assertThat(falsePositive.status()).isEqualTo("RESOLVED");
    assertThat(falsePositive.resolution()).isEqualTo("FALSE-POSITIVE");
    assertThat(falsePositive.creationDate()).isEqualTo(issue.creationDate());

    // user reopens the issue
    assertThat(adminIssueClient().transitions(falsePositive.key())).contains("reopen");
    adminIssueClient().doTransition(falsePositive.key(), "reopen");

    Issue reopened = searchIssueByKey(issue.key());
    assertThat(reopened.status()).isEqualTo("REOPENED");
    assertThat(reopened.resolution()).isNull();
    assertThat(reopened.creationDate()).isEqualTo(falsePositive.creationDate());
  }

  @Test
  public void user_should_not_reopen_closed_issue() {
    adminIssueClient().doTransition(issue.key(), "resolve");

    // re-execute scan without rules -> the issue is closed
    orchestrator.getServer().associateProjectToQualityProfile("workflow", "xoo", "empty");
    orchestrator.executeBuild(scan);

    // user try to reopen the issue
    assertThat(adminIssueClient().transitions(issue.key())).isEmpty();
  }

}
