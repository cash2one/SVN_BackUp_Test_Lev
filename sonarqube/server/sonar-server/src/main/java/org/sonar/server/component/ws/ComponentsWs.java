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

import com.google.common.io.Resources;
import org.sonar.api.server.ws.RailsHandler;
import org.sonar.api.server.ws.WebService;

public class ComponentsWs implements WebService {

  private final AppAction appAction;
  private final SearchAction searchAction;

  public ComponentsWs(AppAction appAction, SearchAction searchAction) {
    this.appAction = appAction;
    this.searchAction = searchAction;
  }

  @Override
  public void define(Context context) {
    NewController controller = context.createController("api/components")
      .setSince("4.2")
      .setDescription("Components management");

    appAction.define(controller);
    searchAction.define(controller);
    defineSuggestionsAction(controller);

    controller.done();
  }

  private void defineSuggestionsAction(NewController controller) {
    NewAction action = controller.createAction("suggestions")
      .setDescription("Internal WS for the top-right search engine")
      .setSince("4.2")
      .setInternal(true)
      .setHandler(RailsHandler.INSTANCE)
      .setResponseExample(Resources.getResource(this.getClass(), "components-example-suggestions.json"));

    action.createParam("s")
      .setRequired(true)
      .setDescription("Substring of project key (minimum 2 characters)")
      .setExampleValue("sonar");

    RailsHandler.addJsonOnlyFormatParam(action);
  }

}
