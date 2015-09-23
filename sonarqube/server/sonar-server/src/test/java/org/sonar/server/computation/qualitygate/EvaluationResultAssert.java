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
package org.sonar.server.computation.qualitygate;

import java.util.Objects;
import org.assertj.core.api.AbstractAssert;
import org.sonar.server.computation.measure.Measure;

class EvaluationResultAssert extends AbstractAssert<EvaluationResultAssert, EvaluationResult> {

  protected EvaluationResultAssert(EvaluationResult actual) {
    super(actual, EvaluationResultAssert.class);
  }

  public static EvaluationResultAssert assertThat(EvaluationResult actual) {
    return new EvaluationResultAssert(actual);
  }

  public EvaluationResultAssert hasLevel(Measure.Level expected) {
    isNotNull();

    // check condition
    if (actual.getLevel() != expected) {
      failWithMessage("Expected Level to be <%s> but was <%s>", expected, actual.getLevel());
    }

    return this;
  }

  public EvaluationResultAssert hasValue(Comparable<?> expected) {
    isNotNull();

    if (!Objects.equals(actual.getValue(), expected)) {
      failWithMessage("Expected Value to be <%s> but was <%s>", expected, actual.getValue());
    }

    return this;
  }
}
