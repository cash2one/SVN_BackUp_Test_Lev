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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.platform.PluginInfo;
import org.sonar.server.plugins.UpdateCenterMatrixFactory;
import org.sonar.updatecenter.common.Artifact;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.PluginUpdate;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.UpdateCenter;
import org.sonar.updatecenter.common.Version;

import static com.google.common.collect.ImmutableSortedSet.copyOf;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;
import static java.lang.String.CASE_INSENSITIVE_ORDER;
import static org.sonar.core.platform.PluginInfoFunctions.toKey;
import static org.sonar.core.platform.PluginInfoFunctions.toName;

public class PluginWSCommons {
  static final String PROPERTY_KEY = "key";
  static final String PROPERTY_NAME = "name";
  static final String PROPERTY_DESCRIPTION = "description";
  static final String PROPERTY_LICENSE = "license";
  static final String PROPERTY_VERSION = "version";
  static final String PROPERTY_CATEGORY = "category";
  static final String PROPERTY_ORGANIZATION_NAME = "organizationName";
  static final String PROPERTY_ORGANIZATION_URL = "organizationUrl";
  static final String PROPERTY_DATE = "date";
  static final String PROPERTY_STATUS = "status";
  static final String PROPERTY_HOMEPAGE_URL = "homepageUrl";
  static final String PROPERTY_ISSUE_TRACKER_URL = "issueTrackerUrl";
  static final String OBJECT_ARTIFACT = "artifact";
  static final String PROPERTY_URL = "url";
  static final String PROPERTY_TERMS_AND_CONDITIONS_URL = "termsAndConditionsUrl";
  static final String OBJECT_UPDATE = "update";
  static final String OBJECT_RELEASE = "release";
  static final String ARRAY_REQUIRES = "requires";
  static final String PROPERTY_UPDATE_CENTER_REFRESH = "updateCenterRefresh";
  static final String PROPERTY_IMPLEMENTATION_BUILD = "implementationBuild";
  static final String PROPERTY_CHANGE_LOG_URL = "changeLogUrl";

  public static final Ordering<PluginInfo> NAME_KEY_PLUGIN_METADATA_COMPARATOR = Ordering.natural()
    .onResultOf(toName())
    .compound(Ordering.natural().onResultOf(toKey()));
  public static final Comparator<Plugin> NAME_KEY_PLUGIN_ORDERING = Ordering.from(CASE_INSENSITIVE_ORDER)
    .onResultOf(PluginToName.INSTANCE)
    .compound(
      Ordering.from(CASE_INSENSITIVE_ORDER).onResultOf(PluginToKeyFunction.INSTANCE)
    );
  public static final Comparator<PluginUpdate> NAME_KEY_PLUGIN_UPDATE_ORDERING = Ordering.from(NAME_KEY_PLUGIN_ORDERING)
    .onResultOf(PluginUpdateToPlugin.INSTANCE);

  void writePluginInfo(JsonWriter json, PluginInfo pluginInfo, @Nullable String category) {
    json.beginObject();

    json.prop(PROPERTY_KEY, pluginInfo.getKey());
    json.prop(PROPERTY_NAME, pluginInfo.getName());
    json.prop(PROPERTY_DESCRIPTION, pluginInfo.getDescription());
    Version version = pluginInfo.getVersion();
    if (version != null) {
      json.prop(PROPERTY_VERSION, version.getName());
    }
    json.prop(PROPERTY_CATEGORY, category);
    json.prop(PROPERTY_LICENSE, pluginInfo.getLicense());
    json.prop(PROPERTY_ORGANIZATION_NAME, pluginInfo.getOrganizationName());
    json.prop(PROPERTY_ORGANIZATION_URL, pluginInfo.getOrganizationUrl());
    json.prop(PROPERTY_HOMEPAGE_URL, pluginInfo.getHomepageUrl());
    json.prop(PROPERTY_ISSUE_TRACKER_URL, pluginInfo.getIssueTrackerUrl());
    json.prop(PROPERTY_IMPLEMENTATION_BUILD, pluginInfo.getImplementationBuild());

    json.endObject();
  }

  public void writePluginInfoList(JsonWriter json, Iterable<PluginInfo> plugins, Map<String, Plugin> compatiblePluginsByKey, String propertyName) {
    json.name(propertyName);
    json.beginArray();
    for (PluginInfo pluginInfo : copyOf(NAME_KEY_PLUGIN_METADATA_COMPARATOR, plugins)) {
      Plugin plugin = compatiblePluginsByKey.get(pluginInfo.getKey());
      writePluginInfo(json, pluginInfo, categoryOrNull(plugin));
    }
    json.endArray();
  }

  public void writePlugin(JsonWriter jsonWriter, Plugin plugin) {
    jsonWriter.prop(PROPERTY_KEY, plugin.getKey());
    jsonWriter.prop(PROPERTY_NAME, plugin.getName());
    jsonWriter.prop(PROPERTY_CATEGORY, plugin.getCategory());
    jsonWriter.prop(PROPERTY_DESCRIPTION, plugin.getDescription());
    jsonWriter.prop(PROPERTY_LICENSE, plugin.getLicense());
    jsonWriter.prop(PROPERTY_TERMS_AND_CONDITIONS_URL, plugin.getTermsConditionsUrl());
    jsonWriter.prop(PROPERTY_ORGANIZATION_NAME, plugin.getOrganization());
    jsonWriter.prop(PROPERTY_ORGANIZATION_URL, plugin.getOrganizationUrl());
    jsonWriter.prop(PROPERTY_HOMEPAGE_URL, plugin.getHomepageUrl());
    jsonWriter.prop(PROPERTY_ISSUE_TRACKER_URL, plugin.getIssueTrackerUrl());
  }

  public void writePluginUpdate(JsonWriter json, PluginUpdate pluginUpdate) {
    Plugin plugin = pluginUpdate.getPlugin();

    json.beginObject();
    writePlugin(json, plugin);
    writeRelease(json, pluginUpdate.getRelease());
    writeUpdate(json, pluginUpdate);
    json.endObject();
  }

  public void writeRelease(JsonWriter jsonWriter, Release release) {
    jsonWriter.name(OBJECT_RELEASE).beginObject();

    jsonWriter.prop(PROPERTY_VERSION, release.getVersion().toString());
    jsonWriter.propDate(PROPERTY_DATE, release.getDate());
    jsonWriter.prop(PROPERTY_DESCRIPTION, release.getDescription());
    jsonWriter.prop(PROPERTY_CHANGE_LOG_URL, release.getChangelogUrl());

    jsonWriter.endObject();
  }

  public void writeArtifact(JsonWriter jsonWriter, Release release) {
    jsonWriter.name(OBJECT_ARTIFACT).beginObject();

    jsonWriter.prop(PROPERTY_NAME, release.getFilename());
    jsonWriter.prop(PROPERTY_URL, release.getDownloadUrl());

    jsonWriter.endObject();
  }

  /**
   * Write an "update" object to the specified jsonwriter.
   * <pre>
   * "update": {
   *   "status": "COMPATIBLE",
   *   "requires": [
   *     {
   *       "key": "java",
   *       "name": "Java",
   *       "description": "SonarQube rule engine."
   *     }
   *   ]
   * }
   * </pre>
   */
  public void writeUpdate(JsonWriter jsonWriter, PluginUpdate pluginUpdate) {
    jsonWriter.name(OBJECT_UPDATE).beginObject();

    writeUpdateProperties(jsonWriter, pluginUpdate);

    jsonWriter.endObject();
  }

  /**
   * Write the update properties to the specified jsonwriter.
   * <pre>
   * "status": "COMPATIBLE",
   * "requires": [
   *   {
   *     "key": "java",
   *     "name": "Java",
   *     "description": "SonarQube rule engine."
   *   }
   * ]
   * </pre>
   */
  public void writeUpdateProperties(JsonWriter jsonWriter, PluginUpdate pluginUpdate) {
    jsonWriter.prop(PROPERTY_STATUS, toJSon(pluginUpdate.getStatus()));

    jsonWriter.name(ARRAY_REQUIRES).beginArray();
    Release release = pluginUpdate.getRelease();
    for (Plugin child : filter(transform(release.getOutgoingDependencies(), ReleaseToArtifact.INSTANCE), Plugin.class)) {
      jsonWriter.beginObject();
      jsonWriter.prop(PROPERTY_KEY, child.getKey());
      jsonWriter.prop(PROPERTY_NAME, child.getName());
      jsonWriter.prop(PROPERTY_DESCRIPTION, child.getDescription());
      jsonWriter.endObject();
    }
    jsonWriter.endArray();
  }

  @VisibleForTesting
  static String toJSon(PluginUpdate.Status status) {
    switch (status) {
      case COMPATIBLE:
        return "COMPATIBLE";
      case INCOMPATIBLE:
        return "INCOMPATIBLE";
      case REQUIRE_SONAR_UPGRADE:
        return "REQUIRES_SYSTEM_UPGRADE";
      case DEPENDENCIES_REQUIRE_SONAR_UPGRADE:
        return "DEPS_REQUIRE_SYSTEM_UPGRADE";
      default:
        throw new IllegalArgumentException("Unsupported value of PluginUpdate.Status " + status);
    }
  }

  /**
   * Write properties of the specified UpdateCenter to the specified JsonWriter.
   * <pre>
   * "updateCenterRefresh": "2015-04-24T16:08:36+0200"
   * </pre>
   */
  public void writeUpdateCenterProperties(JsonWriter json, Optional<UpdateCenter> updateCenter) {
    if (updateCenter.isPresent()) {
      json.propDateTime(PROPERTY_UPDATE_CENTER_REFRESH, updateCenter.get().getDate());
    }
  }

  enum PluginToKeyFunction implements Function<Plugin, String> {
    INSTANCE;

    @Override
    public String apply(@Nonnull Plugin input) {
      return input.getKey();
    }
  }

  private enum ReleaseToArtifact implements Function<Release, Artifact> {
    INSTANCE;
    @Override
    public Artifact apply(@Nonnull Release input) {
      return input.getArtifact();
    }

  }

  private enum PluginUpdateToPlugin implements Function<PluginUpdate, Plugin> {
    INSTANCE;
    @Override
    public Plugin apply(@Nonnull PluginUpdate input) {
      return input.getPlugin();
    }
  }

  private enum PluginToName implements Function<Plugin, String> {
    INSTANCE;
    @Override
    public String apply(@Nonnull Plugin input) {
      return input.getName();
    }
  }

  static String categoryOrNull(Plugin plugin) {
    return plugin != null ? plugin.getCategory() : null;
  }

  private static List<Plugin> compatiblePlugins(UpdateCenterMatrixFactory updateCenterMatrixFactory) {
    Optional<UpdateCenter> updateCenter = updateCenterMatrixFactory.getUpdateCenter(false);
    return updateCenter.isPresent() ? updateCenter.get().findAllCompatiblePlugins() : Collections.<Plugin>emptyList();
  }

  static ImmutableMap<String, Plugin> compatiblePluginsByKey(UpdateCenterMatrixFactory updateCenterMatrixFactory) {
    List<Plugin> compatiblePlugins = compatiblePlugins(updateCenterMatrixFactory);
    return Maps.uniqueIndex(compatiblePlugins, PluginToKeyFunction.INSTANCE);
  }
}
