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

import java.util.List;
import org.assertj.core.data.Offset;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metric.ValueType;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.RowNotFoundException;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.measure.custom.CustomMeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.metric.MetricTesting;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.ServerException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.index.UserDoc;
import org.sonar.server.user.index.UserIndex;
import org.sonar.server.user.index.UserIndexDefinition;
import org.sonar.server.user.ws.UserJsonWriter;
import org.sonar.server.ws.WsTester;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;
import static org.sonar.api.measures.Metric.ValueType.BOOL;
import static org.sonar.api.measures.Metric.ValueType.FLOAT;
import static org.sonar.api.measures.Metric.ValueType.INT;
import static org.sonar.api.measures.Metric.ValueType.LEVEL;
import static org.sonar.api.measures.Metric.ValueType.STRING;
import static org.sonar.api.measures.Metric.ValueType.WORK_DUR;
import static org.sonar.server.util.TypeValidationsTesting.newFullTypeValidations;

@Category(DbTests.class)
public class CreateActionTest {

  private static final String DEFAULT_PROJECT_UUID = "project-uuid";
  private static final String DEFAULT_PROJECT_KEY = "project-key";

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  @ClassRule
  public static EsTester es = new EsTester().addDefinitions(new UserIndexDefinition(new Settings()));

  DbClient dbClient = db.getDbClient();

  DbSession dbSession = db.getSession();

  WsTester ws;

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
    ws = new WsTester(new CustomMeasuresWs(new CreateAction(dbClient, userSession, System2.INSTANCE, new CustomMeasureValidator(newFullTypeValidations()),
      new CustomMeasureJsonWriter(new UserJsonWriter(userSession)), new UserIndex(es.client()), new ComponentFinder(dbClient))));
    db.truncateTables();
    userSession.login("login").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
  }

  @After
  public void tearDown() {
    dbSession.close();
  }

  @Test
  public void create_boolean_custom_measure_in_db() throws Exception {
    insertProject(DEFAULT_PROJECT_UUID);
    MetricDto metric = insertMetric(BOOL);

    newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, DEFAULT_PROJECT_UUID)
      .setParam(CreateAction.PARAM_METRIC_ID, metric.getId().toString())
      .setParam(CreateAction.PARAM_DESCRIPTION, "custom-measure-description")
      .setParam(CreateAction.PARAM_VALUE, "true")
      .execute();

    List<CustomMeasureDto> customMeasures = dbClient.customMeasureDao().selectByMetricId(dbSession, metric.getId());
    CustomMeasureDto customMeasure = customMeasures.get(0);
    assertThat(customMeasures).hasSize(1);
    assertThat(customMeasure.getDescription()).isEqualTo("custom-measure-description");
    assertThat(customMeasure.getTextValue()).isNullOrEmpty();
    assertThat(customMeasure.getValue()).isCloseTo(1.0d, offset(0.01d));
    assertThat(customMeasure.getComponentUuid()).isEqualTo(DEFAULT_PROJECT_UUID);
  }

  @Test
  public void create_int_custom_measure_in_db() throws Exception {
    insertProject(DEFAULT_PROJECT_UUID);
    MetricDto metric = insertMetric(INT);

    newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, DEFAULT_PROJECT_UUID)
      .setParam(CreateAction.PARAM_METRIC_ID, metric.getId().toString())
      .setParam(CreateAction.PARAM_VALUE, "42")
      .execute();

    CustomMeasureDto customMeasure = dbClient.customMeasureDao().selectByMetricId(dbSession, metric.getId()).get(0);
    assertThat(customMeasure.getTextValue()).isNullOrEmpty();
    assertThat(customMeasure.getValue()).isCloseTo(42.0d, offset(0.01d));
  }

  @Test
  public void create_text_custom_measure_in_db() throws Exception {
    insertProject(DEFAULT_PROJECT_UUID);
    MetricDto metric = insertMetric(STRING);

    newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, DEFAULT_PROJECT_UUID)
      .setParam(CreateAction.PARAM_METRIC_ID, metric.getId().toString())
      .setParam(CreateAction.PARAM_VALUE, "custom-measure-free-text")
      .execute();

    CustomMeasureDto customMeasure = dbClient.customMeasureDao().selectByMetricId(dbSession, metric.getId()).get(0);
    assertThat(customMeasure.getTextValue()).isEqualTo("custom-measure-free-text");
  }

  @Test
  public void create_text_custom_measure_as_project_admin() throws Exception {
    insertProject(DEFAULT_PROJECT_UUID);
    MetricDto metric = insertMetric(STRING);
    userSession.login("login").addProjectUuidPermissions(UserRole.ADMIN, DEFAULT_PROJECT_UUID);

    newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, DEFAULT_PROJECT_UUID)
      .setParam(CreateAction.PARAM_METRIC_ID, metric.getId().toString())
      .setParam(CreateAction.PARAM_VALUE, "custom-measure-free-text")
      .execute();

    CustomMeasureDto customMeasure = dbClient.customMeasureDao().selectByMetricId(dbSession, metric.getId()).get(0);
    assertThat(customMeasure).isNotNull();
  }

  @Test
  public void create_text_custom_measure_with_metric_key() throws Exception {
    insertProject(DEFAULT_PROJECT_UUID);
    MetricDto metric = insertMetric(STRING);

    newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, DEFAULT_PROJECT_UUID)
      .setParam(CreateAction.PARAM_METRIC_KEY, metric.getKey())
      .setParam(CreateAction.PARAM_VALUE, "whatever-value")
      .execute();

    CustomMeasureDto customMeasure = dbClient.customMeasureDao().selectByMetricId(dbSession, metric.getId()).get(0);
    assertThat(customMeasure).isNotNull();
  }

  @Test
  public void create_text_custom_measure_with_project_key() throws Exception {
    insertProject(DEFAULT_PROJECT_UUID);
    MetricDto metric = insertMetric(STRING);

    newRequest()
      .setParam(CreateAction.PARAM_PROJECT_KEY, DEFAULT_PROJECT_KEY)
      .setParam(CreateAction.PARAM_METRIC_ID, metric.getId().toString())
      .setParam(CreateAction.PARAM_VALUE, "whatever-value")
      .execute();

    CustomMeasureDto customMeasure = dbClient.customMeasureDao().selectByMetricId(dbSession, metric.getId()).get(0);
    assertThat(customMeasure).isNotNull();
  }

  @Test
  public void create_float_custom_measure_in_db() throws Exception {
    insertProject(DEFAULT_PROJECT_UUID);
    MetricDto metric = insertMetric(FLOAT);

    newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, DEFAULT_PROJECT_UUID)
      .setParam(CreateAction.PARAM_METRIC_ID, metric.getId().toString())
      .setParam(CreateAction.PARAM_VALUE, "4.2")
      .execute();

    CustomMeasureDto customMeasure = dbClient.customMeasureDao().selectByMetricId(dbSession, metric.getId()).get(0);
    assertThat(customMeasure.getValue()).isCloseTo(4.2d, Offset.offset(0.01d));
    assertThat(customMeasure.getTextValue()).isNullOrEmpty();
  }

  @Test
  public void create_work_duration_custom_measure_in_db() throws Exception {
    insertProject(DEFAULT_PROJECT_UUID);
    MetricDto metric = insertMetric(WORK_DUR);

    newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, DEFAULT_PROJECT_UUID)
      .setParam(CreateAction.PARAM_METRIC_ID, metric.getId().toString())
      .setParam(CreateAction.PARAM_VALUE, "253")
      .execute();

    CustomMeasureDto customMeasure = dbClient.customMeasureDao().selectByMetricId(dbSession, metric.getId()).get(0);
    assertThat(customMeasure.getTextValue()).isNullOrEmpty();
    assertThat(customMeasure.getValue()).isCloseTo(253, offset(0.01d));
  }

  @Test
  public void create_level_type_custom_measure_in_db() throws Exception {
    insertProject(DEFAULT_PROJECT_UUID);
    MetricDto metric = insertMetric(LEVEL);

    newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, DEFAULT_PROJECT_UUID)
      .setParam(CreateAction.PARAM_METRIC_ID, metric.getId().toString())
      .setParam(CreateAction.PARAM_VALUE, Metric.Level.WARN.name())
      .execute();

    CustomMeasureDto customMeasure = dbClient.customMeasureDao().selectByMetricId(dbSession, metric.getId()).get(0);
    assertThat(customMeasure.getTextValue()).isEqualTo(Metric.Level.WARN.name());
  }

  @Test
  public void response_with_object_and_id() throws Exception {
    insertProject(DEFAULT_PROJECT_UUID);
    MetricDto metric = insertMetric(STRING);

    WsTester.Result response = newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, DEFAULT_PROJECT_UUID)
      .setParam(CreateAction.PARAM_METRIC_ID, metric.getId().toString())
      .setParam(CreateAction.PARAM_DESCRIPTION, "custom-measure-description")
      .setParam(CreateAction.PARAM_VALUE, "custom-measure-free-text")
      .execute();

    CustomMeasureDto customMeasure = dbClient.customMeasureDao().selectByMetricId(dbSession, metric.getId()).get(0);
    response.assertJson(getClass(), "custom-measure.json");
    assertThat(response.outputAsString()).matches(String.format(".*\"id\"\\s*:\\s*\"%d\".*", customMeasure.getId()));
    assertThat(response.outputAsString()).matches(String.format(".*\"id\"\\s*:\\s*\"%d\".*", metric.getId()));
  }

  @Test
  public void create_custom_measure_on_a_view() throws Exception {
    String viewUuid = "VIEW_UUID";
    dbClient.componentDao().insert(dbSession, ComponentTesting.newView(viewUuid));
    dbSession.commit();
    MetricDto metric = insertMetric(BOOL);

    newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, viewUuid)
      .setParam(CreateAction.PARAM_METRIC_ID, metric.getId().toString())
      .setParam(CreateAction.PARAM_DESCRIPTION, "custom-measure-description")
      .setParam(CreateAction.PARAM_VALUE, "true")
      .execute();

    List<CustomMeasureDto> customMeasures = dbClient.customMeasureDao().selectByMetricId(dbSession, metric.getId());
    CustomMeasureDto customMeasure = customMeasures.get(0);
    assertThat(customMeasures).hasSize(1);
    assertThat(customMeasure.getComponentUuid()).isEqualTo(viewUuid);
  }

  @Test
  public void create_custom_measure_on_a_sub_view() throws Exception {
    String subViewUuid = "SUB_VIEW_UUID";

    ComponentDto view = ComponentTesting.newView();
    dbClient.componentDao().insert(dbSession, view);
    dbClient.componentDao().insert(dbSession, ComponentTesting.newSubView(view, subViewUuid, "SUB_VIEW_KEY"));
    dbSession.commit();
    MetricDto metric = insertMetric(BOOL);

    newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, subViewUuid)
      .setParam(CreateAction.PARAM_METRIC_ID, metric.getId().toString())
      .setParam(CreateAction.PARAM_DESCRIPTION, "custom-measure-description")
      .setParam(CreateAction.PARAM_VALUE, "true")
      .execute();

    List<CustomMeasureDto> customMeasures = dbClient.customMeasureDao().selectByMetricId(dbSession, metric.getId());
    CustomMeasureDto customMeasure = customMeasures.get(0);
    assertThat(customMeasures).hasSize(1);
    assertThat(customMeasure.getComponentUuid()).isEqualTo(subViewUuid);
  }

  @Test
  public void fail_when_get_request() throws Exception {
    expectedException.expect(ServerException.class);

    ws.newGetRequest(CustomMeasuresWs.ENDPOINT, CreateAction.ACTION)
      .setParam(CreateAction.PARAM_PROJECT_ID, DEFAULT_PROJECT_UUID)
      .setParam(CreateAction.PARAM_METRIC_ID, "whatever-id")
      .setParam(CreateAction.PARAM_VALUE, "custom-measure-free-text")
      .execute();
  }

  @Test
  public void fail_when_project_id_nor_project_key_provided() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The component key or the component id must be provided, not both.");
    insertProject(DEFAULT_PROJECT_UUID);
    MetricDto metric = insertMetric(STRING);

    newRequest()
      .setParam(CreateAction.PARAM_METRIC_ID, "whatever-id")
      .setParam(CreateAction.PARAM_VALUE, metric.getId().toString())
      .execute();
  }

  @Test
  public void fail_when_project_id_and_project_key_are_provided() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The component key or the component id must be provided, not both.");
    insertProject(DEFAULT_PROJECT_UUID);
    MetricDto metric = insertMetric(STRING);

    newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, DEFAULT_PROJECT_UUID)
      .setParam(CreateAction.PARAM_PROJECT_KEY, DEFAULT_PROJECT_KEY)
      .setParam(CreateAction.PARAM_METRIC_ID, metric.getId().toString())
      .setParam(CreateAction.PARAM_VALUE, "whatever-value")
      .execute();
  }

  @Test
  public void fail_when_project_key_does_not_exist_in_db() throws Exception {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Component key 'another-project-key' not found");
    insertProject(DEFAULT_PROJECT_UUID);
    MetricDto metric = insertMetric(STRING);

    newRequest()
      .setParam(CreateAction.PARAM_PROJECT_KEY, "another-project-key")
      .setParam(CreateAction.PARAM_METRIC_ID, "whatever-id")
      .setParam(CreateAction.PARAM_VALUE, "whatever-value")
      .execute();
  }

  @Test
  public void fail_when_project_id_does_not_exist_in_db() throws Exception {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Component id 'another-project-uuid' not found");
    insertProject(DEFAULT_PROJECT_UUID);
    MetricDto metric = insertMetric(STRING);

    newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, "another-project-uuid")
      .setParam(CreateAction.PARAM_METRIC_ID, metric.getId().toString())
      .setParam(CreateAction.PARAM_VALUE, "whatever-value")
      .execute();
  }

  @Test
  public void fail_when_metric_id_nor_metric_key_is_provided() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The metric id or the metric key must be provided, not both.");
    insertProject(DEFAULT_PROJECT_UUID);
    MetricDto metric = insertMetric(STRING);

    newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, DEFAULT_PROJECT_UUID)
      .setParam(CreateAction.PARAM_VALUE, "whatever-value")
      .execute();
  }

  @Test
  public void fail_when_metric_id_and_metric_key_are_provided() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The metric id or the metric key must be provided, not both.");
    insertProject(DEFAULT_PROJECT_UUID);
    MetricDto metric = insertMetric(STRING);

    newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, DEFAULT_PROJECT_UUID)
      .setParam(CreateAction.PARAM_METRIC_ID, metric.getId().toString())
      .setParam(CreateAction.PARAM_METRIC_KEY, metric.getKey())
      .setParam(CreateAction.PARAM_VALUE, "whatever-value")
      .execute();
  }

  @Test
  public void fail_when_metric_is_not_found_in_db() throws Exception {
    dbClient.componentDao().insert(dbSession, ComponentTesting.newProjectDto(DEFAULT_PROJECT_UUID));
    dbSession.commit();

    expectedException.expect(RowNotFoundException.class);
    expectedException.expectMessage("Metric id '42' not found");

    newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, DEFAULT_PROJECT_UUID)
      .setParam(CreateAction.PARAM_METRIC_ID, "42")
      .setParam(CreateAction.PARAM_VALUE, "whatever-value")
      .execute();
  }

  @Test
  public void fail_when_measure_already_exists_on_same_project_and_same_metric() throws Exception {
    insertProject(DEFAULT_PROJECT_UUID);
    MetricDto metric = insertMetric(STRING);

    expectedException.expect(ServerException.class);
    expectedException.expectMessage(String.format("A measure already exists for project 'project-key' (id: project-uuid) and metric 'metric-key' (id: '%d')", metric.getId()));

    newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, DEFAULT_PROJECT_UUID)
      .setParam(CreateAction.PARAM_METRIC_ID, metric.getId().toString())
      .setParam(CreateAction.PARAM_VALUE, "whatever-value")
      .execute();
    newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, DEFAULT_PROJECT_UUID)
      .setParam(CreateAction.PARAM_METRIC_ID, metric.getId().toString())
      .setParam(CreateAction.PARAM_VALUE, "whatever-value")
      .execute();
  }

  @Test
  public void fail_when_value_is_not_well_formatted() throws Exception {
    insertProject(DEFAULT_PROJECT_UUID);
    MetricDto metric = insertMetric(BOOL);

    expectedException.expect(BadRequestException.class);

    newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, DEFAULT_PROJECT_UUID)
      .setParam(CreateAction.PARAM_METRIC_ID, metric.getId().toString())
      .setParam(CreateAction.PARAM_VALUE, "non-correct-boolean-value")
      .execute();
  }

  @Test
  public void fail_when_not_enough_permission() throws Exception {
    expectedException.expect(ForbiddenException.class);
    userSession.login("login");
    insertProject(DEFAULT_PROJECT_UUID);
    MetricDto metric = insertMetric(STRING);

    newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, DEFAULT_PROJECT_UUID)
      .setParam(CreateAction.PARAM_METRIC_ID, metric.getId().toString())
      .setParam(CreateAction.PARAM_VALUE, "whatever-value")
      .execute();
  }

  @Test
  public void fail_when_not_a_project() throws Exception {
    MetricDto metric = MetricTesting.newMetricDto().setEnabled(true).setValueType(STRING.name()).setKey("metric-key");
    dbClient.metricDao().insert(dbSession, metric);
    ComponentDto project = ComponentTesting.newProjectDto(DEFAULT_PROJECT_UUID).setKey(DEFAULT_PROJECT_KEY);
    dbClient.componentDao().insert(dbSession, project);
    dbClient.componentDao().insert(dbSession, ComponentTesting.newDirectory(project, "directory-uuid", "path/to/directory").setKey("directory-key"));
    dbSession.commit();

    expectedException.expect(ServerException.class);
    expectedException.expectMessage("Component 'directory-key' (id: directory-uuid) must be a project or a module.");

    newRequest()
      .setParam(CreateAction.PARAM_PROJECT_ID, "directory-uuid")
      .setParam(CreateAction.PARAM_METRIC_ID, metric.getId().toString())
      .setParam(CreateAction.PARAM_VALUE, "whatever-value")
      .execute();
  }

  private WsTester.TestRequest newRequest() {
    return ws.newPostRequest(CustomMeasuresWs.ENDPOINT, CreateAction.ACTION);
  }

  private MetricDto insertMetric(ValueType metricType){
    MetricDto metric = MetricTesting.newMetricDto().setEnabled(true).setValueType(metricType.name()).setKey("metric-key");
    dbClient.metricDao().insert(dbSession, metric);
    dbSession.commit();
    return metric;
  }

  private void insertProject(String projectUuid){
    dbClient.componentDao().insert(dbSession, ComponentTesting.newProjectDto(projectUuid).setKey(DEFAULT_PROJECT_KEY));
    dbSession.commit();
  }
}
