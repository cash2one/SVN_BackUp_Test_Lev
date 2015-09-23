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
package org.sonar.duplications.internal.pmd;

import com.google.common.base.Preconditions;
import org.sonar.duplications.CodeFragment;

/**
 * Immutable code fragment, which formed from tokens of one line.
 */
public class TokensLine implements CodeFragment {

  private final String value;

  private final int startLine;
  private final int hashCode;

  private final int startUnit;
  private final int endUnit;

  public TokensLine(int startUnit, int endUnit, int startLine, String value) {
    Preconditions.checkArgument(startLine > 0);
    // TODO do we have requirements for length and hashcode ?
    this.startLine = startLine;
    this.value = value;
    this.hashCode = value.hashCode();

    this.startUnit = startUnit;
    this.endUnit = endUnit;
  }

  public String getValue() {
    return value;
  }

  @Override
  public int getStartLine() {
    return startLine;
  }

  /**
   * Same as {@link #getStartLine()}
   */
  @Override
  public int getEndLine() {
    return startLine;
  }

  public int getHashCode() {
    return hashCode;
  }

  public int getStartUnit() {
    return startUnit;
  }

  public int getEndUnit() {
    return endUnit;
  }

}
