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

package org.sonar.server.metric.ws;

import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.db.DbClient;
import org.sonar.db.metric.MetricDao;
import org.sonar.server.ws.WsTester;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.metric.MetricTesting.newMetricDto;
import static org.sonar.server.metric.ws.SearchAction.PARAM_IS_CUSTOM;

@Category(DbTests.class)
public class SearchActionTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  DbClient dbClient;
  DbSession dbSession;
  WsTester ws;

  @Before
  public void setUp() {
    dbClient = new DbClient(db.database(), db.myBatis(), new MetricDao());
    dbSession = dbClient.openSession(false);
    ws = new WsTester(new MetricsWs(new SearchAction(dbClient)));
    db.truncateTables();
  }

  @After
  public void tearDown() {
    dbSession.close();
  }

  @Test
  public void search_metrics_in_database() throws Exception {
    insertNewCustomMetric("1", "2", "3");

    WsTester.Result result = newRequest().execute();

    result.assertJson(getClass(), "search_metrics.json");
  }

  @Test
  public void search_metrics_ordered_by_name_case_insensitive() throws Exception {
    insertNewCustomMetric("3", "1", "2");

    String firstResult = newRequest().setParam(Param.PAGE, "1").setParam(Param.PAGE_SIZE, "1").execute().outputAsString();
    String secondResult = newRequest().setParam(Param.PAGE, "2").setParam(Param.PAGE_SIZE, "1").execute().outputAsString();
    String thirdResult = newRequest().setParam(Param.PAGE, "3").setParam(Param.PAGE_SIZE, "1").execute().outputAsString();

    assertThat(firstResult).contains("custom-key-1").doesNotContain("custom-key-2").doesNotContain("custom-key-3");
    assertThat(secondResult).contains("custom-key-2").doesNotContain("custom-key-1").doesNotContain("custom-key-3");
    assertThat(thirdResult).contains("custom-key-3").doesNotContain("custom-key-1").doesNotContain("custom-key-2");
  }

  @Test
  public void search_metrics_with_pagination() throws Exception {
    insertNewCustomMetric("1", "2", "3", "4", "5", "6", "7", "8", "9", "10");

    WsTester.Result result = newRequest()
      .setParam(Param.PAGE, "3")
      .setParam(Param.PAGE_SIZE, "4")
      .execute();

    assertThat(StringUtils.countMatches(result.outputAsString(), "custom-key")).isEqualTo(2);
  }

  @Test
  public void list_metric_with_is_custom_true() throws Exception {
    insertNewCustomMetric("1", "2");
    insertNewNonCustomMetric("3");

    String result = newRequest()
      .setParam(PARAM_IS_CUSTOM, "true").execute().outputAsString();

    assertThat(result).contains("custom-key-1", "custom-key-2")
      .doesNotContain("non-custom-key-3");
  }

  @Test
  public void list_metric_with_is_custom_false() throws Exception {
    insertNewCustomMetric("1", "2");
    insertNewNonCustomMetric("3");

    String result = newRequest()
      .setParam(PARAM_IS_CUSTOM, "false").execute().outputAsString();

    assertThat(result).doesNotContain("custom-key-1")
      .doesNotContain("custom-key-2")
      .contains("non-custom-key-3");
  }

  @Test
  public void list_metric_with_chosen_fields() throws Exception {
    insertNewCustomMetric("1");

    String result = newRequest().setParam(Param.FIELDS, "name").execute().outputAsString();

    assertThat(result).contains("id", "key", "name", "type")
      .doesNotContain("domain")
      .doesNotContain("description");
  }

  private void insertNewNonCustomMetric(String... ids) {
    for (String id : ids) {
      dbClient.metricDao().insert(dbSession, newMetricDto()
        .setKey("non-custom-key-" + id)
        .setEnabled(true)
        .setUserManaged(false));
    }
    dbSession.commit();
  }

  private void insertNewCustomMetric(String... ids) {
    for (String id : ids) {
      dbClient.metricDao().insert(dbSession, newCustomMetric(id));
    }
    dbSession.commit();
  }

  private MetricDto newCustomMetric(String id) {
    return newMetricDto()
      .setKey("custom-key-" + id)
      .setShortName("custom-name-" + id)
      .setDomain("custom-domain-" + id)
      .setDescription("custom-description-" + id)
      .setValueType("INT")
      .setUserManaged(true)
      .setDirection(0)
      .setHidden(false)
      .setQualitative(true)
      .setEnabled(true);
  }

  private WsTester.TestRequest newRequest() {
    return ws.newGetRequest("api/metrics", "search");
  }
}
