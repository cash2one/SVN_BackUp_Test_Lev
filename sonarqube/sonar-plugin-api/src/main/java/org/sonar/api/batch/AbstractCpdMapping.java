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
package org.sonar.api.batch;

import org.sonar.api.resources.Resource;

import java.util.List;

/**
 * A pre-implementation of the CpdMapping extension point
 *
 * @since 1.10
 */
public abstract class AbstractCpdMapping implements CpdMapping {

  /**
   * {@inheritDoc}
   */
  @Override
  public Resource createResource(java.io.File file, List<java.io.File> sourceDirs) {
    throw new UnsupportedOperationException("Deprecated since 4.2");
  }
}
