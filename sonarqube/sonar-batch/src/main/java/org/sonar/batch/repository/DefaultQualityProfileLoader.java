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
package org.sonar.batch.repository;

import javax.annotation.Nullable;

import org.sonar.batch.protocol.input.ProjectRepositories;
import org.sonar.batch.protocol.input.QProfile;

import java.util.Collection;

public class DefaultQualityProfileLoader implements QualityProfileLoader {

  private ProjectRepositoriesFactory projectRepositoriesFactory;

  public DefaultQualityProfileLoader(ProjectRepositoriesFactory projectRepositoriesFactory) {
    this.projectRepositoriesFactory = projectRepositoriesFactory;
  }

  @Override
  public Collection<QProfile> load(@Nullable String projectKey, @Nullable String sonarProfile) {
    ProjectRepositories pr = projectRepositoriesFactory.create();
    validate(pr.qProfiles());
    return pr.qProfiles();
  }

  private static void validate(Collection<QProfile> profiles) {
    if (profiles == null || profiles.isEmpty()) {
      throw new IllegalStateException("No quality profiles has been found this project, you probably don't have any language plugin suitable for this analysis.");
    }
  }

}
