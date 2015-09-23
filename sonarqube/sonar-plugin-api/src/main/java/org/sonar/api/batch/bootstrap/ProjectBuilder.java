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
package org.sonar.api.batch.bootstrap;

import org.sonar.api.batch.BatchSide;
import org.sonar.api.ExtensionPoint;
import org.sonar.api.batch.InstantiationStrategy;

/**
 * This extension point allows to change project structure at runtime. It is executed once during task startup.
 * Some use-cases :
 * <ul>
 *   <li>Add sub-projects. For example the C# plugin gets the hierarchy
 *   of sub-projects from the Visual Studio metadata file. The single root pom.xml does not contain any declarations of
 *   modules</li>
 *   <li>Change project metadata like description or source directories.</li>
 * </ul>
 *
 * @since 2.9
 */
@BatchSide
@InstantiationStrategy(InstantiationStrategy.PER_BATCH)
@ExtensionPoint
public abstract class ProjectBuilder {

  /**
   * Plugins can use the implementation {@link org.sonar.api.batch.bootstrap.internal.ProjectBuilderContext}
   * for their unit tests.
   */
  public interface Context {
    ProjectReactor projectReactor();
  }

  /**
   * Don't inject ProjectReactor as it may not be available
   * @deprecated since 3.7 use {@link #ProjectBuilder()}
   */
  @Deprecated
  protected ProjectBuilder(final ProjectReactor reactor) {
  }

  /**
   * @since 3.7
   */
  protected ProjectBuilder() {
  }

  /**
   * Override this method to change project reactor structure.
   * @since 3.7
   */
  public void build(Context context) {
    // Call deprecated method for backward compatibility
    build(context.projectReactor());
  }

  /**
   * @deprecated since 3.7 override {@link #build(Context)} instead
   */
  @Deprecated
  protected void build(ProjectReactor reactor) {
  }
}
