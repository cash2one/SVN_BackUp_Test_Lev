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
package org.sonar.server.rule.ws;

import com.google.common.io.Resources;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.rule.RuleService;

import java.util.Set;

public class TagsAction implements RulesWsAction {

  private final RuleService service;

  public TagsAction(RuleService service) {
    this.service = service;
  }

  @Override
  public void define(WebService.NewController controller) {
    NewAction action = controller
      .createAction("tags")
      .setDescription("List rule tags")
      .setSince("4.4")
      .setHandler(this)
      .setResponseExample(Resources.getResource(getClass(), "example-tags.json"));

    action.createParam(Param.TEXT_QUERY)
      .setDescription("A pattern to match tags against")
      .setExampleValue("misra");
    action.createParam("ps")
      .setDescription("The size of the list to return, 0 for all tags")
      .setExampleValue("25")
      .setDefaultValue("0");
  }

  @Override
  public void handle(Request request, Response response) {
    String query = request.param(Param.TEXT_QUERY);
    int pageSize = request.mandatoryParamAsInt("ps");
    Set<String> tags = service.listTags(query, pageSize);
    JsonWriter json = response.newJsonWriter().beginObject();
    json.name("tags").beginArray();
    for (String tag : tags) {
      json.value(tag);
    }
    json.endArray().endObject().close();
  }
}
