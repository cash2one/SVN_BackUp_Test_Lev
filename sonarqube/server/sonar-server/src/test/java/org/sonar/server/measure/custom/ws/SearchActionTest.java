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

package org.sonar.server.measure.custom.ws;

import java.util.Date;
import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.Metric.ValueType;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDao;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDao;
import org.sonar.db.measure.custom.CustomMeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.db.component.ComponentTesting;
import org.sonar.server.component.SnapshotTesting;
import org.sonar.server.db.DbClient;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.db.measure.custom.CustomMeasureDao;
import org.sonar.db.metric.MetricDao;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.index.UserDoc;
import org.sonar.server.user.index.UserIndex;
import org.sonar.server.user.index.UserIndexDefinition;
import org.sonar.server.user.ws.UserJsonWriter;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.measure.custom.CustomMeasureTesting.newCustomMeasureDto;
import static org.sonar.db.metric.MetricTesting.newMetricDto;

public class SearchActionTest {

  private static final String DEFAULT_PROJECT_UUID = "project-uuid";
  private static final String DEFAULT_PROJECT_KEY = "project-key";
  private static final String METRIC_KEY_PREFIX = "metric-key-";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  @ClassRule
  public static EsTester es = new EsTester().addDefinitions(new UserIndexDefinition(new Settings()));

  WsTester ws;
  DbClient dbClient;
  DbSession dbSession;
  ComponentDto defaultProject;

  @BeforeClass
  public static void setUpClass() throws Exception {
    es.putDocuments(UserIndexDefinition.INDEX, UserIndexDefinition.TYPE_USER, new UserDoc()
      .setLogin("login")
      .setName("Login")
      .setEmail("login@login.com")
      .setActive(true));
  }

  @Before
  public void setUp() {
    dbClient = new DbClient(db.database(), db.myBatis(), new CustomMeasureDao(), new ComponentDao(), new MetricDao(), new SnapshotDao());
    dbSession = dbClient.openSession(false);
    db.truncateTables();
    CustomMeasureJsonWriter customMeasureJsonWriter = new CustomMeasureJsonWriter(new UserJsonWriter(userSessionRule));
    UserIndex userIndex = new UserIndex(es.client());
    ws = new WsTester(new CustomMeasuresWs(new SearchAction(dbClient, userIndex, customMeasureJsonWriter, userSessionRule, new ComponentFinder(dbClient))));
    defaultProject = insertDefaultProject();
    userSessionRule.login("login").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
  }

  @After
  public void tearDown() {
    dbSession.close();
  }

  @Test
  public void json_well_formatted() throws Exception {
    MetricDto metric1 = insertCustomMetric(1);
    MetricDto metric2 = insertCustomMetric(2);
    MetricDto metric3 = insertCustomMetric(3);
    CustomMeasureDto customMeasure1 = insertCustomMeasure(1, metric1);
    CustomMeasureDto customMeasure2 = insertCustomMeasure(2, metric2);
    CustomMeasureDto customMeasure3 = insertCustomMeasure(3, metric3);

    WsTester.Result response = newRequest()
      .setParam(SearchAction.PARAM_PROJECT_ID, DEFAULT_PROJECT_UUID)
      .execute();

    response.assertJson(getClass(), "custom-measures.json");
    String responseAsString = response.outputAsString();
    assertThat(responseAsString).matches(nameStringValuePattern("id", metric1.getId().toString()));
    assertThat(responseAsString).matches(nameStringValuePattern("id", metric2.getId().toString()));
    assertThat(responseAsString).matches(nameStringValuePattern("id", metric3.getId().toString()));
    assertThat(responseAsString).matches(nameStringValuePattern("id", String.valueOf(customMeasure1.getId())));
    assertThat(responseAsString).matches(nameStringValuePattern("id", String.valueOf(customMeasure2.getId())));
    assertThat(responseAsString).matches(nameStringValuePattern("id", String.valueOf(customMeasure3.getId())));
    assertThat(responseAsString).contains("createdAt", "updatedAt");
  }

  @Test
  public void search_by_project_uuid() throws Exception {
    MetricDto metric1 = insertCustomMetric(1);
    MetricDto metric2 = insertCustomMetric(2);
    MetricDto metric3 = insertCustomMetric(3);
    insertCustomMeasure(1, metric1);
    insertCustomMeasure(2, metric2);
    insertCustomMeasure(3, metric3);

    String response = newRequest()
      .setParam(SearchAction.PARAM_PROJECT_ID, DEFAULT_PROJECT_UUID)
      .execute().outputAsString();

    assertThat(response).contains("text-value-1", "text-value-2", "text-value-3");
  }

  @Test
  public void search_by_project_key() throws Exception {
    MetricDto metric1 = insertCustomMetric(1);
    MetricDto metric2 = insertCustomMetric(2);
    MetricDto metric3 = insertCustomMetric(3);
    insertCustomMeasure(1, metric1);
    insertCustomMeasure(2, metric2);
    insertCustomMeasure(3, metric3);

    String response = newRequest()
      .setParam(SearchAction.PARAM_PROJECT_KEY, DEFAULT_PROJECT_KEY)
      .execute().outputAsString();

    assertThat(response).contains("text-value-1", "text-value-2", "text-value-3");
  }

  @Test
  public void search_with_pagination() throws Exception {
    for (int i = 0; i < 10; i++) {
      MetricDto metric = insertCustomMetric(i);
      insertCustomMeasure(i, metric);
    }

    String response = newRequest()
      .setParam(SearchAction.PARAM_PROJECT_KEY, DEFAULT_PROJECT_KEY)
      .setParam(WebService.Param.PAGE, "3")
      .setParam(WebService.Param.PAGE_SIZE, "4")
      .execute().outputAsString();

    assertThat(StringUtils.countMatches(response, "text-value")).isEqualTo(2);
  }

  @Test
  public void search_with_selectable_fields() throws Exception {
    MetricDto metric = insertCustomMetric(1);
    insertCustomMeasure(1, metric);

    String response = newRequest()
      .setParam(SearchAction.PARAM_PROJECT_KEY, DEFAULT_PROJECT_KEY)
      .setParam(WebService.Param.FIELDS, "value, description")
      .execute().outputAsString();

    assertThat(response).contains("id", "value", "description")
      .doesNotContain("createdAt")
      .doesNotContain("updatedAt")
      .doesNotContain("user")
      .doesNotContain("metric");
  }

  @Test
  public void search_with_more_recent_analysis() throws Exception {
    long yesterday = DateUtils.addDays(new Date(), -1).getTime();
    MetricDto metric = insertCustomMetric(1);
    dbClient.customMeasureDao().insert(dbSession, newCustomMeasure(1, metric)
      .setCreatedAt(yesterday)
      .setUpdatedAt(yesterday));
    dbClient.snapshotDao().insert(dbSession, SnapshotTesting.createForProject(defaultProject));
    dbSession.commit();

    String response = newRequest()
      .setParam(SearchAction.PARAM_PROJECT_ID, DEFAULT_PROJECT_UUID)
      .execute().outputAsString();

    assertThat(response).matches(nameValuePattern("pending", "false"));
  }

  @Test
  public void search_as_project_admin() throws Exception {
    userSessionRule.login("login").addProjectUuidPermissions(UserRole.ADMIN, DEFAULT_PROJECT_UUID);
    MetricDto metric1 = insertCustomMetric(1);
    insertCustomMeasure(1, metric1);

    String response = newRequest()
      .setParam(SearchAction.PARAM_PROJECT_ID, DEFAULT_PROJECT_UUID)
      .execute().outputAsString();

    assertThat(response).contains("text-value-1");
  }

  @Test
  public void empty_json_when_no_measure() throws Exception {
    WsTester.Result response = newRequest()
      .setParam(SearchAction.PARAM_PROJECT_KEY, DEFAULT_PROJECT_KEY)
      .execute();

    response.assertJson(getClass(), "empty.json");
  }

  @Test
  public void fail_when_project_id_and_project_key_provided() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The component key or the component id must be provided, not both.");

    newRequest()
      .setParam(SearchAction.PARAM_PROJECT_ID, DEFAULT_PROJECT_UUID)
      .setParam(SearchAction.PARAM_PROJECT_KEY, DEFAULT_PROJECT_KEY)
      .execute();
  }

  @Test
  public void fail_when_project_id_nor_project_key_provided() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The component key or the component id must be provided, not both.");
    newRequest().execute();
  }

  @Test
  public void fail_when_project_not_found_in_db() throws Exception {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Component id 'wrong-project-uuid' not found");

    newRequest().setParam(SearchAction.PARAM_PROJECT_ID, "wrong-project-uuid").execute();
  }

  @Test
  public void fail_when_not_enough_privileges() throws Exception {
    expectedException.expect(ForbiddenException.class);
    userSessionRule.login("login");
    MetricDto metric1 = insertCustomMetric(1);
    insertCustomMeasure(1, metric1);

    String response = newRequest()
      .setParam(SearchAction.PARAM_PROJECT_ID, DEFAULT_PROJECT_UUID)
      .execute().outputAsString();

    assertThat(response).contains("text-value-1");
  }

  private ComponentDto insertDefaultProject() {
    return insertProject(DEFAULT_PROJECT_UUID, DEFAULT_PROJECT_KEY);
  }

  private ComponentDto insertProject(String projectUuid, String projectKey) {
    ComponentDto project = ComponentTesting.newProjectDto(projectUuid)
      .setKey(projectKey);
    dbClient.componentDao().insert(dbSession, project);
    dbSession.commit();

    return project;
  }

  private MetricDto insertCustomMetric(int id) {
    MetricDto metric = newCustomMetric(METRIC_KEY_PREFIX + id);
    dbClient.metricDao().insert(dbSession, metric);
    dbSession.commit();

    return metric;
  }

  private static MetricDto newCustomMetric(String metricKey) {
    return newMetricDto().setEnabled(true).setUserManaged(true).setKey(metricKey).setDomain(metricKey + "-domain").setShortName(metricKey + "-name").setValueType(ValueType.STRING.name());
  }

  private CustomMeasureDto insertCustomMeasure(int id, MetricDto metric) {
    CustomMeasureDto customMeasure = newCustomMeasure(id, metric);
    dbClient.customMeasureDao().insert(dbSession, customMeasure);
    dbSession.commit();

    return customMeasure;
  }

  private CustomMeasureDto newCustomMeasure(int id, MetricDto metric) {
    return newCustomMeasureDto()
      .setUserLogin("login")
      .setValue(id)
      .setTextValue("text-value-" + id)
      .setDescription("description-" + id)
      .setMetricId(metric.getId())
      .setComponentUuid(defaultProject.uuid());
  }

  private WsTester.TestRequest newRequest() {
    return ws.newGetRequest(CustomMeasuresWs.ENDPOINT, SearchAction.ACTION);
  }

  private static String nameStringValuePattern(String name, String value) {
    return String.format(".*\"%s\"\\s*:\\s*\"%s\".*", name, value);
  }

  private static String nameValuePattern(String name, String value) {
    return String.format(".*\"%s\"\\s*:\\s*%s.*", name, value);
  }
}
