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
package org.sonar.batch.scan;

import org.picocontainer.injectors.ProviderAdapter;
import org.sonar.api.batch.bootstrap.ProjectReactor;

public class ImmutableProjectReactorProvider extends ProviderAdapter {

  private ImmutableProjectReactor singleton;

  public ImmutableProjectReactor provide(ProjectReactor reactor, ProjectBuildersExecutor projectBuildersExecutor, ProjectExclusions exclusions, ProjectReactorValidator validator) {
    if (singleton == null) {
      // 1 Apply project builders
      projectBuildersExecutor.execute(reactor);

      // 2 Apply project exclusions
      exclusions.apply(reactor);

      // 3 Validate final reactor
      validator.validate(reactor);

      singleton = new ImmutableProjectReactor(reactor.getRoot());
    }
    return singleton;
  }
}
