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
package org.sonar.batch.bootstrap;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.BatchSide;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import static com.google.common.collect.Sets.newHashSet;

/**
 * Filters the plugins to be enabled during analysis
 */
@BatchSide
public class BatchPluginPredicate implements Predicate<String> {

  private static final Logger LOG = Loggers.get(BatchPluginPredicate.class);

  private static final String BUILDBREAKER_PLUGIN_KEY = "buildbreaker";
  private static final Joiner COMMA_JOINER = Joiner.on(", ");

  private final Set<String> whites = newHashSet();
  private final Set<String> blacks = newHashSet();
  private final GlobalMode mode;

  public BatchPluginPredicate(Settings settings, GlobalMode mode) {
    this.mode = mode;
    if (mode.isPreview() || mode.isIssues()) {
      // These default values are not supported by Settings because the class CorePlugin
      // is not loaded yet.
      whites.addAll(propertyValues(settings,
        CoreProperties.PREVIEW_INCLUDE_PLUGINS, CoreProperties.PREVIEW_INCLUDE_PLUGINS_DEFAULT_VALUE));
      blacks.addAll(propertyValues(settings,
        CoreProperties.PREVIEW_EXCLUDE_PLUGINS, CoreProperties.PREVIEW_EXCLUDE_PLUGINS_DEFAULT_VALUE));
    }
    if (!whites.isEmpty()) {
      LOG.info("Include plugins: " + COMMA_JOINER.join(whites));
    }
    if (!blacks.isEmpty()) {
      LOG.info("Exclude plugins: " + COMMA_JOINER.join(blacks));
    }
  }

  @Override
  public boolean apply(@Nonnull String pluginKey) {
    if (BUILDBREAKER_PLUGIN_KEY.equals(pluginKey) && mode.isPreview()) {
      LOG.info("Build Breaker plugin is no more supported in preview mode");
      return false;
    }

    if (whites.isEmpty()) {
      return blacks.isEmpty() || !blacks.contains(pluginKey);
    }
    return whites.contains(pluginKey);
  }

  Set<String> getWhites() {
    return whites;
  }

  Set<String> getBlacks() {
    return blacks;
  }

  private static List<String> propertyValues(Settings settings, String key, String defaultValue) {
    String s = StringUtils.defaultIfEmpty(settings.getString(key), defaultValue);
    return Lists.newArrayList(Splitter.on(",").trimResults().omitEmptyStrings().split(s));
  }
}
