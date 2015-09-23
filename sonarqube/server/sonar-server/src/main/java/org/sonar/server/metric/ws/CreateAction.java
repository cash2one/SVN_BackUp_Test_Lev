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

import java.util.List;
import javax.annotation.Nullable;
import org.sonar.api.measures.Metric;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;
import org.sonar.db.measure.custom.CustomMeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.ruby.RubyBridge;
import org.sonar.server.user.UserSession;

import static org.sonar.server.util.MetricKeyValidator.checkMetricKeyFormat;

public class CreateAction implements MetricsWsAction {
  private static final String ACTION = "create";
  public static final String PARAM_NAME = "name";
  public static final String PARAM_KEY = "key";
  public static final String PARAM_TYPE = "type";
  public static final String PARAM_DESCRIPTION = "description";
  public static final String PARAM_DOMAIN = "domain";

  private static final String FIELD_ID = "id";
  private static final String FIELD_NAME = PARAM_NAME;
  private static final String FIELD_KEY = PARAM_KEY;
  private static final String FIELD_TYPE = PARAM_TYPE;
  private static final String FIELD_DESCRIPTION = PARAM_DESCRIPTION;
  private static final String FIELD_DOMAIN = PARAM_DOMAIN;

  private final DbClient dbClient;
  private final UserSession userSession;
  private final RubyBridge rubyBridge;

  public CreateAction(DbClient dbClient, UserSession userSession, RubyBridge rubyBridge) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.rubyBridge = rubyBridge;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION)
      .setPost(true)
      .setDescription("Create custom metric.<br /> Requires 'Administer System' permission.")
      .setSince("5.2")
      .setHandler(this);

    action.createParam(PARAM_NAME)
      .setRequired(true)
      .setDescription("Name")
      .setExampleValue("Team Size");

    action.createParam(PARAM_KEY)
      .setRequired(true)
      .setDescription("Key")
      .setExampleValue("team_size");

    action.createParam(PARAM_TYPE)
      .setRequired(true)
      .setDescription("Metric type key")
      .setPossibleValues(Metric.ValueType.names())
      .setExampleValue(Metric.ValueType.INT.name());

    action.createParam(PARAM_DESCRIPTION)
      .setDescription("Description")
      .setExampleValue("Size of the team");

    action.createParam(PARAM_DOMAIN)
      .setDescription("Domain")
      .setExampleValue("Tests");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn().checkGlobalPermission(GlobalPermissions.SYSTEM_ADMIN);
    String key = request.mandatoryParam(PARAM_KEY);

    DbSession dbSession = dbClient.openSession(false);
    try {
      MetricDto metricTemplate = newMetricTemplate(request);
      MetricDto metricInDb = dbClient.metricDao().selectByKey(dbSession, key);
      checkMetricInDbAndTemplate(dbSession, metricInDb, metricTemplate);

      if (metricIsNotInDb(metricInDb)) {
        metricInDb = insertNewMetric(dbSession, metricTemplate);
      } else {
        updateMetric(dbSession, metricInDb, metricTemplate);
      }

      JsonWriter json = response.newJsonWriter();
      writeMetric(json, metricInDb);
      json.close();
      rubyBridge.metricCache().invalidate();
    } finally {
      MyBatis.closeQuietly(dbSession);
    }
  }

  private static MetricDto newMetricTemplate(Request request) {
    String key = checkMetricKeyFormat(request.mandatoryParam(PARAM_KEY));
    String name = request.mandatoryParam(PARAM_NAME);
    String type = Metric.ValueType.valueOf(request.mandatoryParam(PARAM_TYPE)).name();
    String domain = request.param(PARAM_DOMAIN);
    String description = request.param(PARAM_DESCRIPTION);

    MetricDto metricTemplate = new MetricDto()
      .setKey(key)
      .setShortName(name)
      .setValueType(type);
    if (domain != null) {
      metricTemplate.setDomain(domain);
    }
    if (description != null) {
      metricTemplate.setDescription(description);
    }
    return metricTemplate;
  }

  private void updateMetric(DbSession dbSession, MetricDto metricInDb, MetricDto metricTemplate) {
    metricInDb
      .setShortName(metricTemplate.getShortName())
      .setValueType(metricTemplate.getValueType())
      .setDomain(metricTemplate.getDomain())
      .setDescription(metricTemplate.getDescription())
      .setEnabled(true);
    dbClient.metricDao().update(dbSession, metricInDb);
    dbSession.commit();
  }

  private MetricDto insertNewMetric(DbSession dbSession, MetricDto metricTemplate) {
    MetricDto metric = new MetricDto()
      .setKey(metricTemplate.getKey())
      .setShortName(metricTemplate.getShortName())
      .setValueType(metricTemplate.getValueType())
      .setDomain(metricTemplate.getDomain())
      .setDescription(metricTemplate.getDescription())
      .setEnabled(true)
      .setUserManaged(true)
      .setDirection(0)
      .setQualitative(false)
      .setHidden(false)
      .setOptimizedBestValue(false)
      .setDeleteHistoricalData(false);

    dbClient.metricDao().insert(dbSession, metric);
    dbSession.commit();
    return metric;
  }

  private void checkMetricInDbAndTemplate(DbSession dbSession, @Nullable MetricDto metricInDb, MetricDto template) {
    if (areOneOfTheMandatoryArgumentsEmpty(template)) {
      throw new IllegalArgumentException(String.format("The mandatory arguments '%s','%s' and '%s' must not be empty", PARAM_KEY, PARAM_NAME, PARAM_TYPE));
    }
    if (metricIsNotInDb(metricInDb)) {
      return;
    }
    if (isMetricEnabled(metricInDb)) {
      throw new BadRequestException("An active metric already exist with key: " + metricInDb.getKey());
    }
    if (isMetricNonCustom(metricInDb)) {
      throw new BadRequestException("An non custom metric already exist with key: " + metricInDb.getKey());
    }
    if (hasMetricTypeChanged(metricInDb, template)) {
      List<CustomMeasureDto> customMeasures = dbClient.customMeasureDao().selectByMetricId(dbSession, metricInDb.getId());
      if (hasAssociatedCustomMeasures(customMeasures)) {
        throw new BadRequestException(String.format("You're trying to change the type '%s' while there are associated measures.",
          metricInDb.getValueType()));
      }
    }
  }

  private static boolean hasAssociatedCustomMeasures(List<CustomMeasureDto> customMeasures) {
    return !customMeasures.isEmpty();
  }

  private static boolean hasMetricTypeChanged(MetricDto metricInDb, MetricDto template) {
    return !metricInDb.getValueType().equals(template.getValueType());
  }

  private static boolean isMetricNonCustom(MetricDto metricInDb) {
    return !metricInDb.isUserManaged();
  }

  private static boolean isMetricEnabled(MetricDto metricInDb) {
    return metricInDb.isEnabled();
  }

  private static boolean metricIsNotInDb(@Nullable MetricDto metricInDb) {
    return metricInDb == null;
  }

  private static boolean areOneOfTheMandatoryArgumentsEmpty(MetricDto template) {
    return template.getValueType().isEmpty() || template.getShortName().isEmpty() || template.getKey().isEmpty();
  }

  private static void writeMetric(JsonWriter json, MetricDto metric) {
    json.beginObject();
    json.prop(FIELD_ID, metric.getId().toString());
    json.prop(FIELD_KEY, metric.getKey());
    json.prop(FIELD_NAME, metric.getShortName());
    json.prop(FIELD_TYPE, metric.getValueType());
    json.prop(FIELD_DOMAIN, metric.getDomain());
    json.prop(FIELD_DESCRIPTION, metric.getDescription());
    json.endObject();
  }
}
