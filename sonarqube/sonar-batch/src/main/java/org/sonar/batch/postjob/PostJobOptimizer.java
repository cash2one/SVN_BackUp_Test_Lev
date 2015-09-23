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
package org.sonar.batch.postjob;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.BatchSide;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.batch.postjob.internal.DefaultPostJobDescriptor;
import org.sonar.api.config.Settings;

@BatchSide
public class PostJobOptimizer {

  private static final Logger LOG = LoggerFactory.getLogger(PostJobOptimizer.class);

  private final Settings settings;
  private final AnalysisMode analysisMode;

  public PostJobOptimizer(Settings settings, AnalysisMode analysisMode) {
    this.settings = settings;
    this.analysisMode = analysisMode;
  }

  /**
   * Decide if the given PostJob should be executed.
   */
  public boolean shouldExecute(DefaultPostJobDescriptor descriptor) {
    if (!settingsCondition(descriptor)) {
      LOG.debug("'{}' skipped because one of the required properties is missing", descriptor.name());
      return false;
    }
    if (descriptor.isDisabledInIssues() && analysisMode.isIssues()) {
      LOG.debug("'{}' skipped in issues mode", descriptor.name());
      return false;
    }
    return true;
  }

  private boolean settingsCondition(DefaultPostJobDescriptor descriptor) {
    if (!descriptor.properties().isEmpty()) {
      for (String propertyKey : descriptor.properties()) {
        if (!settings.hasKey(propertyKey)) {
          return false;
        }
      }
    }
    return true;
  }

}
