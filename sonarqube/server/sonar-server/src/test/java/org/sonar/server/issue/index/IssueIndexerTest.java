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

import com.google.common.collect.Iterators;
import java.util.List;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.config.Settings;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.server.db.DbClient;
import org.sonar.server.es.EsTester;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;

@Category(DbTests.class)
public class IssueIndexerTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @ClassRule
  public static EsTester esTester = new EsTester().addDefinitions(new IssueIndexDefinition(new Settings()));

  @Before
  public void setUp() {
    dbTester.truncateTables();
    esTester.truncateIndices();
  }

  @Test
  public void index_nothing() {
    IssueIndexer indexer = createIndexer();
    indexer.index(Iterators.<IssueDoc>emptyIterator());
    assertThat(esTester.countDocuments(IssueIndexDefinition.INDEX, IssueIndexDefinition.TYPE_ISSUE)).isEqualTo(0L);
  }

  @Test
  public void index_nothing_if_disabled() {
    dbTester.prepareDbUnit(getClass(), "index.xml");

    createIndexer().setEnabled(false).index();

    assertThat(esTester.countDocuments("issues", "issue")).isEqualTo(0);
  }

  @Test
  public void index() {
    dbTester.prepareDbUnit(getClass(), "index.xml");

    IssueIndexer indexer = createIndexer();
    indexer.index();

    List<IssueDoc> docs = esTester.getDocuments("issues", "issue", IssueDoc.class);
    assertThat(docs).hasSize(1);
    IssueDoc doc = docs.get(0);
    assertThat(doc.projectUuid()).isEqualTo("THE_PROJECT");
    assertThat(doc.componentUuid()).isEqualTo("THE_FILE");
    assertThat(doc.moduleUuid()).isEqualTo("THE_PROJECT");
    assertThat(doc.modulePath()).isEqualTo(".THE_PROJECT.");
    assertThat(doc.directoryPath()).isEqualTo("src/main/java");
    assertThat(doc.severity()).isEqualTo("BLOCKER");
    assertThat(doc.ruleKey()).isEqualTo(RuleKey.of("squid", "AvoidCycles"));

    // functional date
    assertThat(doc.updateDate().getTime()).isEqualTo(1368828000000L);

    // technical date
    assertThat(doc.getTechnicalUpdateDate().getTime()).isEqualTo(1550000000000L);

    // delete project
    indexer.deleteProject("THE_PROJECT", true);

    assertThat(esTester.countDocuments("issues", "issue")).isZero();
  }

  private IssueIndexer createIndexer() {
    IssueIndexer indexer = new IssueIndexer(new DbClient(dbTester.database(), dbTester.myBatis()), esTester.client());
    indexer.setEnabled(true);
    return indexer;
  }
}
