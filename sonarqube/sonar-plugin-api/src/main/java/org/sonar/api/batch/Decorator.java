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

import org.sonar.api.ExtensionPoint;
import org.sonar.api.resources.Resource;

/**
 * @since 1.10
 * @deprecated since 5.2 there's no more decorator on batch side. Use {@link org.sonar.api.ce.measure.MeasureComputer} instead
 */
@BatchSide
@ExtensionPoint
@Deprecated
public interface Decorator extends CheckProject {

  /**
   * @param resource resource that is currently decorated, equivalent of <code>context.getResource()</code>
   */
  void decorate(Resource resource, DecoratorContext context);

}
