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

import java.util.Arrays;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.measure.custom.CustomMeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.ForbiddenException;
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
import static org.sonar.db.metric.MetricTesting.newMetricDto;

@Category(DbTests.class)
public class DeleteActionTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  DbClient dbClient;
  DbSession dbSession;
  WsTester ws;
  MetricDao metricDao;

  @Before
  public void setUp() {
    dbClient = new DbClient(db.database(), db.myBatis(), new MetricDao(), new CustomMeasureDao());
    dbSession = dbClient.openSession(false);
    db.truncateTables();
    userSessionRule.login("login").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    ws = new WsTester(new MetricsWs(new DeleteAction(dbClient, userSessionRule, mock(RubyBridge.class, RETURNS_DEEP_STUBS))));
    metricDao = dbClient.metricDao();
  }

  @After
  public void tearDown() {
    dbSession.close();
  }

  @Test
  public void delete_by_keys() throws Exception {
    insertCustomEnabledMetrics(1, 2, 3);
    dbSession.commit();

    newRequest().setParam("keys", "key-1, key-3").execute();
    dbSession.commit();

    List<MetricDto> disabledMetrics = metricDao.selectByKeys(dbSession, Arrays.asList("key-1", "key-3"));
    assertThat(disabledMetrics).extracting("enabled").containsOnly(false);
    assertThat(metricDao.selectByKey(dbSession, "key-2").isEnabled()).isTrue();
  }

  @Test
  public void delete_by_id() throws Exception {
    MetricDto metric = newCustomEnabledMetric(1);
    metricDao.insert(dbSession, metric);
    dbSession.commit();

    WsTester.Result result = newRequest().setParam("ids", String.valueOf(metric.getId())).execute();
    dbSession.commit();

    assertThat(metricDao.selectEnabled(dbSession)).isEmpty();
    result.assertNoContent();
  }

  @Test
  public void do_not_delete_non_custom_metric() throws Exception {
    metricDao.insert(dbSession, newCustomEnabledMetric(1).setUserManaged(false));
    dbSession.commit();

    newRequest().setParam("keys", "key-1").execute();
    dbSession.commit();

    MetricDto metric = metricDao.selectByKey(dbSession, "key-1");
    assertThat(metric.isEnabled()).isTrue();
  }

  @Test
  public void delete_associated_measures() throws Exception {
    MetricDto metric = newCustomEnabledMetric(1);
    metricDao.insert(dbSession, metric);
    CustomMeasureDto customMeasure = CustomMeasureTesting.newCustomMeasureDto().setMetricId(metric.getId());
    CustomMeasureDto undeletedCustomMeasure = CustomMeasureTesting.newCustomMeasureDto().setMetricId(metric.getId() + 1);
    dbClient.customMeasureDao().insert(dbSession, customMeasure);
    dbClient.customMeasureDao().insert(dbSession, undeletedCustomMeasure);
    dbSession.commit();

    newRequest().setParam("keys", "key-1").execute();

    assertThat(dbClient.customMeasureDao().selectById(dbSession, customMeasure.getId())).isNull();
    assertThat(dbClient.customMeasureDao().selectById(dbSession, undeletedCustomMeasure.getId())).isNotNull();
  }

  @Test
  public void fail_when_no_argument() throws Exception {
    expectedException.expect(IllegalArgumentException.class);

    newRequest().execute();
  }

  @Test
  public void fail_when_insufficient_privileges() throws Exception {
    expectedException.expect(ForbiddenException.class);

    userSessionRule.setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);
    insertCustomEnabledMetrics(1);

    newRequest().setParam("keys", "key-1").execute();
  }

  private MetricDto newCustomEnabledMetric(int id) {
    return newMetricDto().setEnabled(true).setUserManaged(true).setKey("key-" + id);
  }

  private void insertCustomEnabledMetrics(int... ids) {
    for (int id : ids) {
      metricDao.insert(dbSession, newCustomEnabledMetric(id));
    }

    dbSession.commit();
  }

  private WsTester.TestRequest newRequest() {
    return ws.newPostRequest(MetricsWs.ENDPOINT, "delete");
  }
}
