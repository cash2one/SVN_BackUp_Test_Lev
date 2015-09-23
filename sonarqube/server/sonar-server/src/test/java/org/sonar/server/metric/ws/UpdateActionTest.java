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
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.ServerException;
import org.sonar.db.measure.custom.CustomMeasureDao;
import org.sonar.db.metric.MetricDao;
import org.sonar.server.ruby.RubyBridge;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.sonar.db.measure.custom.CustomMeasureTesting.newCustomMeasureDto;
import static org.sonar.server.metric.ws.UpdateAction.PARAM_DESCRIPTION;
import static org.sonar.server.metric.ws.UpdateAction.PARAM_DOMAIN;
import static org.sonar.server.metric.ws.UpdateAction.PARAM_ID;
import static org.sonar.server.metric.ws.UpdateAction.PARAM_KEY;
import static org.sonar.server.metric.ws.UpdateAction.PARAM_NAME;
import static org.sonar.server.metric.ws.UpdateAction.PARAM_TYPE;

@Category(DbTests.class)
public class UpdateActionTest {

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

    ws = new WsTester(new MetricsWs(new UpdateAction(dbClient, userSessionRule, mock(RubyBridge.class, RETURNS_DEEP_STUBS))));
    userSessionRule.login("login").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
  }

  @After
  public void tearDown() {
    dbSession.close();
  }

  @Test
  public void update_all_fields() throws Exception {
    int id = insertMetric(newDefaultMetric());

    newRequest()
      .setParam(PARAM_ID, String.valueOf(id))
      .setParam(PARAM_KEY, "another-key")
      .setParam(PARAM_NAME, "another-name")
      .setParam(PARAM_TYPE, ValueType.BOOL.name())
      .setParam(PARAM_DOMAIN, "another-domain")
      .setParam(PARAM_DESCRIPTION, "another-description")
      .execute();
    dbSession.commit();

    MetricDto result = dbClient.metricDao().selectById(dbSession, id);
    assertThat(result.getKey()).isEqualTo("another-key");
    assertThat(result.getShortName()).isEqualTo("another-name");
    assertThat(result.getValueType()).isEqualTo(ValueType.BOOL.name());
    assertThat(result.getDomain()).isEqualTo("another-domain");
    assertThat(result.getDescription()).isEqualTo("another-description");
  }

  @Test
  public void update_one_field() throws Exception {
    int id = insertMetric(newDefaultMetric());
    dbClient.customMeasureDao().insert(dbSession, newCustomMeasureDto().setMetricId(id));
    dbSession.commit();

    newRequest()
      .setParam(PARAM_ID, String.valueOf(id))
      .setParam(PARAM_DESCRIPTION, "another-description")
      .execute();
    dbSession.commit();

    MetricDto result = dbClient.metricDao().selectById(dbSession, id);
    assertThat(result.getKey()).isEqualTo(DEFAULT_KEY);
    assertThat(result.getShortName()).isEqualTo(DEFAULT_NAME);
    assertThat(result.getValueType()).isEqualTo(DEFAULT_TYPE);
    assertThat(result.getDomain()).isEqualTo(DEFAULT_DOMAIN);
    assertThat(result.getDescription()).isEqualTo("another-description");
  }

  @Test
  public void update_return_the_full_object_with_id() throws Exception {
    int id = insertMetric(newDefaultMetric().setDescription("another-description"));

    WsTester.Result requestResult = newRequest()
      .setParam(PARAM_ID, String.valueOf(id))
      .setParam(PARAM_DESCRIPTION, DEFAULT_DESCRIPTION)
      .execute();
    dbSession.commit();

    requestResult.assertJson(getClass(), "metric.json");
    assertThat(requestResult.outputAsString()).matches(".*\"id\"\\s*:\\s*\"" + id + "\".*");
  }

  @Test
  public void fail_when_changing_key_for_an_existing_one() throws Exception {
    expectedException.expect(ServerException.class);
    expectedException.expectMessage("The key 'metric-key' is already used by an existing metric.");
    insertMetric(newDefaultMetric().setKey("metric-key"));
    int id = insertMetric(newDefaultMetric().setKey("another-key"));

    newRequest()
      .setParam(PARAM_ID, String.valueOf(id))
      .setParam(PARAM_KEY, "metric-key")
      .execute();
  }

  @Test
  public void fail_when_metric_not_in_db() throws Exception {
    expectedException.expect(ServerException.class);

    newRequest().setParam(PARAM_ID, "42").execute();
  }

  @Test
  public void fail_when_metric_is_deactivated() throws Exception {
    expectedException.expect(ServerException.class);
    int id = insertMetric(newDefaultMetric().setEnabled(false));

    newRequest().setParam(PARAM_ID, String.valueOf(id)).execute();
  }

  @Test
  public void fail_when_metric_is_not_custom() throws Exception {
    expectedException.expect(ServerException.class);
    int id = insertMetric(newDefaultMetric().setUserManaged(false));

    newRequest().setParam(PARAM_ID, String.valueOf(id)).execute();
  }

  @Test
  public void fail_when_custom_measures_and_type_changed() throws Exception {
    expectedException.expect(ServerException.class);
    int id = insertMetric(newDefaultMetric());
    dbClient.customMeasureDao().insert(dbSession, newCustomMeasureDto().setMetricId(id));
    dbSession.commit();

    newRequest()
      .setParam(PARAM_ID, String.valueOf(id))
      .setParam(PARAM_TYPE, ValueType.BOOL.name())
      .execute();
  }

  @Test
  public void fail_when_no_id() throws Exception {
    expectedException.expect(IllegalArgumentException.class);

    newRequest().execute();
  }

  @Test
  public void fail_when_insufficient_privileges() throws Exception {
    expectedException.expect(ForbiddenException.class);
    userSessionRule.login("login");

    newRequest().execute();
  }

  @Test
  public void fail_when_metric_key_is_not_well_formatted() throws Exception {
    expectedException.expect(IllegalArgumentException.class);

    int id = insertMetric(newDefaultMetric());
    dbClient.customMeasureDao().insert(dbSession, newCustomMeasureDto().setMetricId(id));
    dbSession.commit();

    newRequest()
      .setParam(PARAM_ID, String.valueOf(id))
      .setParam(PARAM_KEY, "not well formatted key")
      .execute();
  }

  private MetricDto newDefaultMetric() {
    return new MetricDto()
      .setKey(DEFAULT_KEY)
      .setShortName(DEFAULT_NAME)
      .setValueType(DEFAULT_TYPE)
      .setDomain(DEFAULT_DOMAIN)
      .setDescription(DEFAULT_DESCRIPTION)
      .setUserManaged(true)
      .setEnabled(true);
  }

  private int insertMetric(MetricDto metricDto) {
    dbClient.metricDao().insert(dbSession, metricDto);
    dbSession.commit();
    return metricDto.getId();
  }

  private WsTester.TestRequest newRequest() {
    return ws.newPostRequest("api/metrics", "update");
  }
}
