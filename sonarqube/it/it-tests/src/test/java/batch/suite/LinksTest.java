/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package batch.suite;

import util.ItUtils;
import com.google.common.collect.Lists;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.db.Database;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class LinksTest {

  @ClassRule
  public static Orchestrator orchestrator = BatchTestSuite.ORCHESTRATOR;

  private static String[] expectedLinks = new String[] {
    "homepage=http://www.simplesample.org_OVERRIDDEN",
    "ci=http://bamboo.ci.codehaus.org/browse/SIMPLESAMPLE",
    "issue=http://jira.codehaus.org/browse/SIMPLESAMPLE",
    "scm=https://github.com/SonarSource/simplesample",
    "scm_dev=scm:git:git@github.com:SonarSource/simplesample.git"
  };

  @Before
  @After
  public void cleanProjectLinksTable() {
    // TODO should not do this and find another way without using direct db connection
    orchestrator.getDatabase().truncate("project_links");
  }

  /**
   * SONAR-3676
   */
  @Test
  public void shouldUseLinkProperties() {
    SonarRunner runner = SonarRunner.create(ItUtils.projectDir("batch/links-project"))
      .setProperty("sonar.scm.disabled", "true");
    orchestrator.executeBuild(runner);

    checkLinks();
  }

  /**
   * SONAR-3676
   */
  @Test
  public void shouldUseLinkPropertiesOverPomLinksInMaven() {
    MavenBuild build = MavenBuild.create(ItUtils.projectPom("batch/links-project"))
      .setCleanPackageSonarGoals()
      .setProperty("sonar.scm.disabled", "true");
    orchestrator.executeBuild(build);

    checkLinks();
  }

  private void checkLinks() {
    Database db = orchestrator.getDatabase();
    List<Map<String, String>> links = db.executeSql("select * from project_links");

    assertThat(links.size()).isEqualTo(5);
    Collection<String> linksToCheck = Lists.newArrayList();
    for (Map<String, String> linkRow : links) {
      linksToCheck.add(linkRow.get("LINK_TYPE") + "=" + linkRow.get("HREF"));
    }
    assertThat(linksToCheck).contains(expectedLinks);
  }

}
