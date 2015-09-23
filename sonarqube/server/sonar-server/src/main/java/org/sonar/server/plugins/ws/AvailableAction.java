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
package org.sonar.server.plugins.ws;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.io.Resources;
import java.util.Collection;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.plugins.UpdateCenterMatrixFactory;
import org.sonar.updatecenter.common.PluginUpdate;
import org.sonar.updatecenter.common.UpdateCenter;

import static org.sonar.server.plugins.ws.PluginWSCommons.NAME_KEY_PLUGIN_UPDATE_ORDERING;

public class AvailableAction implements PluginsWsAction {

  private static final boolean DO_NOT_FORCE_REFRESH = false;
  private static final String ARRAY_PLUGINS = "plugins";

  private final UpdateCenterMatrixFactory updateCenterFactory;
  private final PluginWSCommons pluginWSCommons;

  public AvailableAction(UpdateCenterMatrixFactory updateCenterFactory, PluginWSCommons pluginWSCommons) {
    this.updateCenterFactory = updateCenterFactory;
    this.pluginWSCommons = pluginWSCommons;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("available")
      .setDescription("Get the list of all the plugins available for installation on the SonarQube instance, sorted by plugin name." +
        "<br/>" +
        "Plugin information is retrieved from Update Center. Date and time at which Update Center was last refreshed is provided in the response." +
        "<br/>" +
        "Update status values are: " +
        "<ul>" +
        "<li>COMPATIBLE: plugin is compatible with current SonarQube instance.</li>" +
        "<li>INCOMPATIBLE: plugin is not compatible with current SonarQube instance.</li>" +
        "<li>REQUIRES_SYSTEM_UPGRADE: plugin requires SonarQube to be upgraded before being installed.</li>" +
        "<li>DEPS_REQUIRE_SYSTEM_UPGRADE: at least one plugin on which the plugin is dependent requires SonarQube to be upgraded.</li>" +
        "</ul>")
      .setSince("5.2")
      .setHandler(this)
      .setResponseExample(Resources.getResource(this.getClass(), "example-available_plugins.json"));
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    JsonWriter jsonWriter = response.newJsonWriter();
    jsonWriter.beginObject();

    Optional<UpdateCenter> updateCenter = updateCenterFactory.getUpdateCenter(DO_NOT_FORCE_REFRESH);

    writePlugins(jsonWriter, updateCenter);
    pluginWSCommons.writeUpdateCenterProperties(jsonWriter, updateCenter);

    jsonWriter.endObject();
    jsonWriter.close();
  }

  private void writePlugins(JsonWriter jsonWriter, Optional<UpdateCenter> updateCenter) {
    jsonWriter.name(ARRAY_PLUGINS).beginArray();
    if (updateCenter.isPresent()) {
      for (PluginUpdate pluginUpdate : retrieveAvailablePlugins(updateCenter.get())) {
        pluginWSCommons.writePluginUpdate(jsonWriter, pluginUpdate);
      }
    }
    jsonWriter.endArray();
  }

  private static Collection<PluginUpdate> retrieveAvailablePlugins(UpdateCenter updateCenter) {
    return ImmutableSortedSet.copyOf(
      NAME_KEY_PLUGIN_UPDATE_ORDERING,
      updateCenter.findAvailablePlugins()
      );
  }

}
