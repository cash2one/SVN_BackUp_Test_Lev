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
package org.sonar.api.measures;

import org.sonar.api.batch.BatchSide;
import org.sonar.api.server.ServerSide;

import javax.annotation.CheckForNull;

import java.util.Collection;
import java.util.List;

/**
 * @since 2.5
 * @deprecated since 5.1 use {@link org.sonar.api.batch.measure.MetricFinder} on batch side
 */
@Deprecated
@BatchSide
@ServerSide
public interface MetricFinder {

  @CheckForNull
  Metric findById(int id);

  @CheckForNull
  Metric findByKey(String key);

  Collection<Metric> findAll(List<String> metricKeys);

  Collection<Metric> findAll();
}
