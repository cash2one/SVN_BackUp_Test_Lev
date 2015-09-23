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
package org.sonar.batch.test;

import javax.annotation.CheckForNull;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.test.MutableTestable;
import org.sonar.batch.deprecated.perspectives.PerspectiveBuilder;
import org.sonar.batch.index.BatchComponent;

public class TestableBuilder extends PerspectiveBuilder<MutableTestable> {

  public TestableBuilder() {
    super(MutableTestable.class);
  }

  @CheckForNull
  @Override
  public MutableTestable loadPerspective(Class<MutableTestable> perspectiveClass, BatchComponent component) {
    if (component.isFile()) {
      InputFile inputFile = (InputFile) component.inputComponent();
      if (inputFile.type() == Type.MAIN) {
        return new DefaultTestable((DefaultInputFile) inputFile);
      }
    }
    return null;
  }
}
