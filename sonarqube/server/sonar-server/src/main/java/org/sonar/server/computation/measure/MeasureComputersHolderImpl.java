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

package org.sonar.server.computation.measure;

import com.google.common.base.Predicates;
import javax.annotation.CheckForNull;
import org.sonar.server.computation.measure.api.MeasureComputerWrapper;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.FluentIterable.from;
import static java.util.Objects.requireNonNull;

public class MeasureComputersHolderImpl implements MutableMeasureComputersHolder {

  @CheckForNull
  private Iterable<MeasureComputerWrapper> measureComputers;

  @Override
  public Iterable<MeasureComputerWrapper> getMeasureComputers() {
    checkState(this.measureComputers != null, "Measure computers have not been initialized yet");
    return measureComputers;
  }

  @Override
  public void setMeasureComputers(Iterable<MeasureComputerWrapper> measureComputers) {
    requireNonNull(measureComputers, "Measure computers cannot be null");
    checkState(this.measureComputers == null, "Measure computers have already been initialized");
    this.measureComputers = from(measureComputers).filter(Predicates.notNull()).toList();
  }
}
