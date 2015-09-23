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
package org.sonar.batch.deprecated;

import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.component.Component;
import org.sonar.api.resources.Qualifiers;

public class InputFileComponent implements Component {

  private final DefaultInputFile inputFile;

  public InputFileComponent(DefaultInputFile inputFile) {
    this.inputFile = inputFile;
  }

  @Override
  public String key() {
    return inputFile.key();
  }

  @Override
  public String path() {
    return inputFile.relativePath();
  }

  @Override
  public String name() {
    return inputFile.file().getName();
  }

  @Override
  public String longName() {
    return inputFile.relativePath();
  }

  @Override
  public String qualifier() {
    return inputFile.type() == Type.MAIN ? Qualifiers.FILE : Qualifiers.UNIT_TEST_FILE;
  }

}
