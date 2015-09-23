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

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.sonar.api.measures.Metric.ValueType;
import org.sonar.api.utils.System2;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.metric.MetricTesting;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.ServerException;
import org.sonar.db.measure.custom.CustomMeasureDao;
import org.sonar.db.measure.custom.CustomMeasureTesting;
import org.sonar.db.metric.MetricDao;
import org.sonar.server.ruby.RubyBridge;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.sonar.server.metric.ws.CreateAction.PARAM_DESCRIPTION;
import static org.sonar.server.metric.ws.CreateAction.PARAM_DOMAIN;
import static org.sonar.server.metric.ws.CreateAction.PARAM_KEY;
import static org.sonar.server.metric.ws.CreateAction.PARAM_NAME;
import static org.sonar.server.metric.ws.CreateAction.PARAM_TYPE;

@Category(DbTests.class)
public class CreateActionTest {

  private static final String DEFAULT_KEY = "custom-metric-key";
  private static final String DEFAULT_NAME = "custom-metric-name";
  private static final String DEFAULT_DOMAIN = "custom-metric-domain";
  private static final String DEFAULT_DESCRIPTION = "custom-metric-description";
  private static final String DEFAULT_TYPE = ValueType.INT.name();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();
  WsTester ws;
  DbClient dbClient;
  DbSession dbSession;

  @Before
  public void setUp() {
    dbClient = new DbClient(db.database(), db.myBatis(), new MetricDao(), new CustomMeasureDao());
    dbSession = dbClient.openSession(false);
    db.truncateTables();

    ws = new WsTester(new MetricsWs(new CreateAction(dbClient, userSessionRule, mock(RubyBridge.class, RETURNS_DEEP_STUBS))));
    userSessionRule.login("login").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
  }

  @After
  public void tearDown() {
    dbSession.close();
  }

  @Test
  public void insert_new_minimalist_metric() throws Exception {
    newRequest()
      .setParam(PARAM_KEY, DEFAULT_KEY)
      .setParam(PARAM_NAME, DEFAULT_NAME)
      .setParam(PARAM_TYPE, DEFAULT_TYPE)
      .execute();

    MetricDto metric = dbClient.metricDao().selectByKey(dbSession, DEFAULT_KEY);

    assertThat(metric.getKey()).isEqualTo(DEFAULT_KEY);
    assertThat(metric.getShortName()).isEqualTo(DEFAULT_NAME);
    assertThat(metric.getValueType()).isEqualTo(DEFAULT_TYPE);
    assertThat(metric.getDescription()).isNull();
    assertThat(metric.getDomain()).isNull();
    assertThat(metric.isUserManaged()).isTrue();
    assertThat(metric.isEnabled()).isTrue();
    assertThat(metric.getDirection()).isEqualTo(0);
    assertThat(metric.isQualitative()).isFalse();
  }

  @Test
  public void insert_new_full_metric() throws Exception {
    newRequest()
      .setParam(PARAM_KEY, DEFAULT_KEY)
      .setParam(PARAM_NAME, DEFAULT_NAME)
      .setParam(PARAM_TYPE, DEFAULT_TYPE)
      .setParam(PARAM_DOMAIN, DEFAULT_DOMAIN)
      .setParam(PARAM_DESCRIPTION, DEFAULT_DESCRIPTION)
      .execute();

    MetricDto metric = dbClient.metricDao().selectByKey(dbSession, DEFAULT_KEY);

    assertThat(metric.getKey()).isEqualTo(DEFAULT_KEY);
    assertThat(metric.getDescription()).isEqualTo(DEFAULT_DESCRIPTION);
    assertThat(metric.getDomain()).isEqualTo(DEFAULT_DOMAIN);
  }

  @Test
  public void return_metric_with_id() throws Exception {
    WsTester.Result result = newRequest()
      .setParam(PARAM_KEY, DEFAULT_KEY)
      .setParam(PARAM_NAME, DEFAULT_NAME)
      .setParam(PARAM_TYPE, DEFAULT_TYPE)
      .setParam(PARAM_DOMAIN, DEFAULT_DOMAIN)
      .setParam(PARAM_DESCRIPTION, DEFAULT_DESCRIPTION)
      .execute();

    result.assertJson(getClass(), "metric.json");
    assertThat(result.outputAsString()).matches(".*\"id\"\\s*:\\s*\"\\w+\".*");
  }

  @Test
  public void update_existing_metric_when_custom_and_disabled() throws Exception {
    MetricDto metricInDb = MetricTesting.newMetricDto()
      .setKey(DEFAULT_KEY)
      .setValueType(ValueType.BOOL.name())
      .setUserManaged(true)
      .setEnabled(false);
    dbClient.metricDao().insert(dbSession, metricInDb);
    dbSession.commit();

    WsTester.Result result = newRequest()
      .setParam(PARAM_KEY, DEFAULT_KEY)
      .setParam(PARAM_NAME, DEFAULT_NAME)
      .setParam(PARAM_TYPE, DEFAULT_TYPE)
      .setParam(PARAM_DESCRIPTION, DEFAULT_DESCRIPTION)
      .setParam(PARAM_DOMAIN, DEFAULT_DOMAIN)
      .execute();

    result.assertJson(getClass(), "metric.json");
    result.outputAsString().matches("\"id\"\\s*:\\s*\"" + metricInDb.getId() + "\"");
    MetricDto metricAfterWs = dbClient.metricDao().selectByKey(dbSession, DEFAULT_KEY);
    assertThat(metricAfterWs.getId()).isEqualTo(metricInDb.getId());
    assertThat(metricAfterWs.getDomain()).isEqualTo(DEFAULT_DOMAIN);
    assertThat(metricAfterWs.getDescription()).isEqualTo(DEFAULT_DESCRIPTION);
    assertThat(metricAfterWs.getValueType()).isEqualTo(DEFAULT_TYPE);
    assertThat(metricAfterWs.getShortName()).isEqualTo(DEFAULT_NAME);
  }

  @Test
  public void fail_when_existing_activated_metric_with_same_key() throws Exception {
    expectedException.expect(ServerException.class);
    dbClient.metricDao().insert(dbSession, MetricTesting.newMetricDto()
      .setKey(DEFAULT_KEY)
      .setValueType(DEFAULT_TYPE)
      .setUserManaged(true)
      .setEnabled(true));
    dbSession.commit();

    newRequest()
      .setParam(PARAM_KEY, DEFAULT_KEY)
      .setParam(PARAM_NAME, "any-name")
      .setParam(PARAM_TYPE, DEFAULT_TYPE).execute();
  }

  @Test
  public void fail_when_existing_non_custom_metric_with_same_key() throws Exception {
    expectedException.expect(ServerException.class);
    dbClient.metricDao().insert(dbSession, MetricTesting.newMetricDto()
      .setKey(DEFAULT_KEY)
      .setValueType(DEFAULT_TYPE)
      .setUserManaged(false)
      .setEnabled(false));
    dbSession.commit();

    newRequest()
      .setParam(PARAM_KEY, DEFAULT_KEY)
      .setParam(PARAM_NAME, "any-name")
      .setParam(PARAM_TYPE, DEFAULT_TYPE).execute();
  }

  @Test
  public void fail_when_metric_type_is_changed_and_associated_measures_exist() throws Exception {
    expectedException.expect(ServerException.class);
    MetricDto metric = MetricTesting.newMetricDto()
      .setKey(DEFAULT_KEY)
      .setValueType(ValueType.BOOL.name())
      .setUserManaged(true)
      .setEnabled(false);
    dbClient.metricDao().insert(dbSession, metric);
    dbClient.customMeasureDao().insert(dbSession, CustomMeasureTesting.newCustomMeasureDto().setMetricId(metric.getId()));
    dbSession.commit();

    newRequest()
      .setParam(PARAM_KEY, DEFAULT_KEY)
      .setParam(PARAM_NAME, "any-name")
      .setParam(PARAM_TYPE, ValueType.INT.name())
      .execute();
  }

  @Test
  public void fail_when_missing_key() throws Exception {
    expectedException.expect(IllegalArgumentException.class);

    newRequest()
      .setParam(PARAM_NAME, DEFAULT_NAME)
      .setParam(PARAM_TYPE, DEFAULT_TYPE).execute();
  }

  @Test
  public void fail_when_missing_name() throws Exception {
    expectedException.expect(IllegalArgumentException.class);

    newRequest()
      .setParam(PARAM_KEY, DEFAULT_KEY)
      .setParam(PARAM_TYPE, DEFAULT_TYPE).execute();
  }

  @Test
  public void fail_when_missing_type() throws Exception {
    expectedException.expect(IllegalArgumentException.class);

    newRequest()
      .setParam(PARAM_NAME, DEFAULT_NAME)
      .setParam(PARAM_KEY, DEFAULT_KEY).execute();
  }

  @Test
  public void fail_when_insufficient_privileges() throws Exception {
    expectedException.expect(ForbiddenException.class);
    userSessionRule.login("login");

    newRequest()
      .setParam(PARAM_KEY, "any-key")
      .setParam(PARAM_NAME, "any-name")
      .setParam(PARAM_TYPE, DEFAULT_TYPE)
      .execute();
  }

  @Test
  public void fail_when_ill_formatted_key() throws Exception {
    expectedException.expect(IllegalArgumentException.class);

    newRequest()
      .setParam(PARAM_KEY, "123:456")
      .setParam(PARAM_NAME, DEFAULT_NAME)
      .setParam(PARAM_TYPE, DEFAULT_TYPE)
      .execute();
  }

  @Test
  public void fail_when_empty_name() throws Exception {
    expectedException.expect(IllegalArgumentException.class);

    newRequest()
      .setParam(PARAM_KEY, DEFAULT_KEY)
      .setParam(PARAM_NAME, "")
      .setParam(PARAM_TYPE, DEFAULT_TYPE)
      .execute();
  }

  @Test
  public void fail_when_empty_type() throws Exception {
    expectedException.expect(IllegalArgumentException.class);

    newRequest()
      .setParam(PARAM_KEY, DEFAULT_KEY)
      .setParam(PARAM_NAME, DEFAULT_NAME)
      .setParam(PARAM_TYPE, "")
      .execute();
  }

  private WsTester.TestRequest newRequest() {
    return ws.newPostRequest("api/metrics", "create");
  }
}
