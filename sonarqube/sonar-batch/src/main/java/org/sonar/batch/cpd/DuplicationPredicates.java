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
package org.sonar.batch.cpd;

import com.google.common.base.Predicate;
import org.sonar.duplications.index.CloneGroup;

import javax.annotation.Nullable;

public final class DuplicationPredicates {

  private DuplicationPredicates() {
  }

  public static Predicate<CloneGroup> numberOfUnitsNotLessThan(int min) {
    return new NumberOfUnitsNotLessThan(min);
  }

  private static class NumberOfUnitsNotLessThan implements Predicate<CloneGroup> {
    private final int min;

    public NumberOfUnitsNotLessThan(int min) {
      this.min = min;
    }

    @Override
    public boolean apply(@Nullable CloneGroup input) {
      return input != null && input.getLengthInUnits() >= min;
    }
  }

}
