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

import com.google.common.base.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.server.computation.measure.Measure;

import static java.util.Objects.requireNonNull;

@Immutable
public final class EvaluationResult {
  private final Measure.Level level;
  @CheckForNull
  private final Comparable<?> value;

  public EvaluationResult(Measure.Level level, @Nullable Comparable<?> value) {
    this.level = requireNonNull(level);
    this.value = value;
  }

  public Measure.Level getLevel() {
    return level;
  }

  @CheckForNull
  public Comparable<?> getValue() {
    return value;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("level", level)
        .add("value", value)
        .toString();
  }
}
