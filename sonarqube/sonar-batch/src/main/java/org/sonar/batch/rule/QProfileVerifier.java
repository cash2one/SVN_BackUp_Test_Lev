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
package org.sonar.batch.rule;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.BatchSide;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.MessageException;

@BatchSide
public class QProfileVerifier {

  private static final Logger LOG = LoggerFactory.getLogger(QProfileVerifier.class);

  private final Settings settings;
  private final FileSystem fs;
  private final ModuleQProfiles profiles;

  public QProfileVerifier(Settings settings, FileSystem fs, ModuleQProfiles profiles) {
    this.settings = settings;
    this.fs = fs;
    this.profiles = profiles;
  }

  public void execute() {
    execute(LOG);
  }

  @VisibleForTesting
  void execute(Logger logger) {
    String defaultName = settings.getString(ModuleQProfiles.SONAR_PROFILE_PROP);
    boolean defaultNameUsed = StringUtils.isBlank(defaultName);
    for (String lang : fs.languages()) {
      QProfile profile = profiles.findByLanguage(lang);
      if (profile == null) {
        logger.warn("No Quality profile found for language " + lang);
      } else {
        logger.info("Quality profile for {}: {}", lang, profile.getName());
        if (StringUtils.isNotBlank(defaultName) && defaultName.equals(profile.getName())) {
          defaultNameUsed = true;
        }
      }
    }
    if (!defaultNameUsed && !fs.languages().isEmpty()) {
      throw MessageException.of("sonar.profile was set to '" + defaultName + "' but didn't match any profile for any language. Please check your configuration.");
    }
  }
}
