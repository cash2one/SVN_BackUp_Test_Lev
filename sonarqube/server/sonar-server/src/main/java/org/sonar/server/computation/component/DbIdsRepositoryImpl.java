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

package org.sonar.server.computation.component;

import static org.sonar.server.computation.component.ComponentFunctions.toReportRef;

/**
 * Cache of persisted component (component id and snapshot id) that can be used in the persistence steps
 */
public class DbIdsRepositoryImpl implements MutableDbIdsRepository {

  private final MapBasedDbIdsRepository<Integer> delegate = new MapBasedDbIdsRepository<>(toReportRef());

  @Override
  public DbIdsRepository setComponentId(Component component, long componentId) {
    return delegate.setComponentId(component, componentId);
  }

  @Override
  public long getComponentId(Component component) {
    return delegate.getComponentId(component);
  }

  @Override
  public DbIdsRepository setSnapshotId(Component component, long snapshotId) {
    return delegate.setSnapshotId(component, snapshotId);
  }

  @Override
  public long getSnapshotId(Component component) {
    return delegate.getSnapshotId(component);
  }
}
