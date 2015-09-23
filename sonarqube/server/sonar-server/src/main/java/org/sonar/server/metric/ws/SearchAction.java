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
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.es.SearchOptions;

import static com.google.common.collect.Sets.newHashSet;
import static org.sonar.server.metric.ws.MetricJsonWriter.FIELD_CUSTOM;
import static org.sonar.server.metric.ws.MetricJsonWriter.FIELD_DESCRIPTION;
import static org.sonar.server.metric.ws.MetricJsonWriter.FIELD_DIRECTION;
import static org.sonar.server.metric.ws.MetricJsonWriter.FIELD_DOMAIN;
import static org.sonar.server.metric.ws.MetricJsonWriter.FIELD_HIDDEN;
import static org.sonar.server.metric.ws.MetricJsonWriter.FIELD_ID;
import static org.sonar.server.metric.ws.MetricJsonWriter.FIELD_KEY;
import static org.sonar.server.metric.ws.MetricJsonWriter.FIELD_NAME;
import static org.sonar.server.metric.ws.MetricJsonWriter.FIELD_QUALITATIVE;
import static org.sonar.server.metric.ws.MetricJsonWriter.FIELD_TYPE;

public class SearchAction implements MetricsWsAction {

  private static final String ACTION = "search";

  public static final String PARAM_IS_CUSTOM = "isCustom";

  private static final Set<String> OPTIONAL_FIELDS = newHashSet(FIELD_NAME, FIELD_DESCRIPTION, FIELD_DOMAIN, FIELD_TYPE, FIELD_DIRECTION, FIELD_QUALITATIVE, FIELD_HIDDEN,
    FIELD_CUSTOM);
  private final Set<String> allPossibleFields;

  private final DbClient dbClient;

  public SearchAction(DbClient dbClient) {
    this.dbClient = dbClient;
    Set<String> possibleFields = newHashSet(FIELD_ID, FIELD_KEY);
    possibleFields.addAll(OPTIONAL_FIELDS);
    allPossibleFields = possibleFields;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION)
      .setSince("5.2")
      .setResponseExample(getClass().getResource("example-search.json"))
      .addPagingParams(100)
      .addFieldsParam(OPTIONAL_FIELDS)
      .setHandler(this);

    action.createParam(PARAM_IS_CUSTOM)
      .setExampleValue("true")
      .setDescription("Choose custom metrics following 3 cases:" +
        "<ul>" +
        "<li>true: only custom metrics are returned</li>" +
        "<li>false: only non custom metrics are returned</li>" +
        "<li>not specified: all metrics are returned</li>" +
        "</ul>");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    SearchOptions searchOptions = new SearchOptions()
      .setPage(request.mandatoryParamAsInt(Param.PAGE),
        request.mandatoryParamAsInt(Param.PAGE_SIZE));
    Boolean isCustom = request.paramAsBoolean(PARAM_IS_CUSTOM);
    DbSession dbSession = dbClient.openSession(false);
    try {
      List<MetricDto> metrics = dbClient.metricDao().selectEnabled(dbSession, isCustom, searchOptions.getOffset(), searchOptions.getLimit());
      int nbMetrics = dbClient.metricDao().countEnabled(dbSession, isCustom);
      JsonWriter json = response.newJsonWriter();
      json.beginObject();
      Set<String> desiredFields = desiredFields(request.paramAsStrings(Param.FIELDS));
      writeMetrics(json, metrics, desiredFields);
      searchOptions.writeJson(json, nbMetrics);
      json.endObject();
      json.close();
    } finally {
      MyBatis.closeQuietly(dbSession);
    }
  }

  private Set<String> desiredFields(@Nullable List<String> fields) {
    if (fields == null || fields.isEmpty()) {
      return allPossibleFields;
    }

    return newHashSet(fields);
  }

  public static void writeMetrics(JsonWriter json, List<MetricDto> metrics, Set<String> desiredFields) {
    MetricJsonWriter.write(json, metrics, desiredFields);
  }
}
