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

package org.sonar.batch.source;

import javax.annotation.CheckForNull;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.source.Symbolizable;
import org.sonar.batch.deprecated.perspectives.PerspectiveBuilder;
import org.sonar.batch.index.BatchComponent;
import org.sonar.batch.sensor.DefaultSensorStorage;

public class SymbolizableBuilder extends PerspectiveBuilder<Symbolizable> {

  private final DefaultSensorStorage sensorStorage;
  private final AnalysisMode analysisMode;

  public SymbolizableBuilder(DefaultSensorStorage sensorStorage, AnalysisMode analysisMode) {
    super(Symbolizable.class);
    this.sensorStorage = sensorStorage;
    this.analysisMode = analysisMode;
  }

  @CheckForNull
  @Override
  public Symbolizable loadPerspective(Class<Symbolizable> perspectiveClass, BatchComponent component) {
    if (component.isFile()) {
      InputFile path = (InputFile) component.inputComponent();
      return new DefaultSymbolizable((DefaultInputFile) path, sensorStorage, analysisMode);
    }
    return null;
  }
}
