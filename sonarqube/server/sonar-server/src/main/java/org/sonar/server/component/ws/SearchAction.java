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

package org.sonar.server.component.ws;

import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.user.UserSession;

import static com.google.common.collect.Sets.newLinkedHashSet;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;

public class SearchAction implements RequestHandler {

  private static final short MINIMUM_SEARCH_CHARACTERS = 2;

  private static final String PARAM_COMPONENT_UUID = "componentUuid";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final ComponentFinder componentFinder;

  public SearchAction(DbClient dbClient, UserSession userSession, ComponentFinder componentFinder) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.componentFinder = componentFinder;
  }

  void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("search")
      .setDescription("Search for components. Currently limited to projects in a view or a sub-view")
      .setSince("5.1")
      .setInternal(true)
      .setHandler(this);

    action
      .createParam(PARAM_COMPONENT_UUID)
      .setRequired(true)
      .setDescription("View or sub view UUID")
      .setExampleValue("d6d9e1e5-5e13-44fa-ab82-3ec29efa8935");

    action
      .createParam(Param.TEXT_QUERY)
      .setRequired(true)
      .setDescription("UTF-8 search query")
      .setExampleValue("sonar");

    action.addPagingParams(10);
  }

  @Override
  public void handle(Request request, Response response) {
    String query = request.mandatoryParam(Param.TEXT_QUERY);
    if (query.length() < MINIMUM_SEARCH_CHARACTERS) {
      throw new IllegalArgumentException(String.format("Minimum search is %s characters", MINIMUM_SEARCH_CHARACTERS));
    }
    String componentUuid = request.mandatoryParam(PARAM_COMPONENT_UUID);

    JsonWriter json = response.newJsonWriter();
    json.beginObject();

    DbSession session = dbClient.openSession(false);
    try {
      ComponentDto componentDto = componentFinder.getByUuid(session, componentUuid);
      userSession.checkProjectUuidPermission(UserRole.USER, componentDto.projectUuid());

      Set<Long> projectIds = newLinkedHashSet(dbClient.componentIndexDao().selectProjectIdsFromQueryAndViewOrSubViewUuid(session, query, componentDto.uuid()));
      Collection<Long> authorizedProjectIds = dbClient.authorizationDao().keepAuthorizedProjectIds(session, projectIds, userSession.getUserId(), UserRole.USER);

      SearchOptions options = new SearchOptions();
      options.setPage(request.mandatoryParamAsInt(PAGE), request.mandatoryParamAsInt(PAGE_SIZE));
      Set<Long> pagedProjectIds = pagedProjectIds(authorizedProjectIds, options);

      List<ComponentDto> projects = dbClient.componentDao().selectByIds(session, pagedProjectIds);

      options.writeJson(json, authorizedProjectIds.size());
      json.name("components").beginArray();
      for (ComponentDto project : projects) {
        json.beginObject();
        json.prop("uuid", project.uuid());
        json.prop("name", project.name());
        json.endObject();
      }
      json.endArray();
    } finally {
      MyBatis.closeQuietly(session);
    }

    json.endObject();
    json.close();
  }

  private static Set<Long> pagedProjectIds(Collection<Long> projectIds, SearchOptions options) {
    Set<Long> results = Sets.newLinkedHashSet();
    int index = 0;
    for (Long projectId : projectIds) {
      if (index >= options.getOffset() && results.size() < options.getLimit()) {
        results.add(projectId);
      } else if (results.size() >= options.getLimit()) {
        break;
      }
      index++;
    }
    return results;
  }
}
