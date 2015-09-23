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

package org.sonar.server.computation.step;

import org.sonar.db.component.ResourceIndexDao;
import org.sonar.server.computation.component.DbIdsRepository;
import org.sonar.server.computation.component.TreeRootHolder;

/**
 * Components are currently indexed in db table RESOURCE_INDEX, not in Elasticsearch
 */
public class IndexComponentsStep implements ComputationStep {

  private final ResourceIndexDao resourceIndexDao;
  private final DbIdsRepository dbIdsRepository;
  private final TreeRootHolder treeRootHolder;

  public IndexComponentsStep(ResourceIndexDao resourceIndexDao, DbIdsRepository dbIdsRepository, TreeRootHolder treeRootHolder) {
    this.resourceIndexDao = resourceIndexDao;
    this.dbIdsRepository = dbIdsRepository;
    this.treeRootHolder = treeRootHolder;
  }

  @Override
  public void execute() {
    resourceIndexDao.indexProject(dbIdsRepository.getComponentId(treeRootHolder.getRoot()));
  }

  @Override
  public String getDescription() {
    return "Index components";
  }
}
