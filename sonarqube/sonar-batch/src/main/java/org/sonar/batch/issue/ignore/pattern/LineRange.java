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

package org.sonar.batch.issue.ignore.pattern;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

import java.util.Set;

public class LineRange {
  private int from;
  private int to;

  public LineRange(int from, int to) {
    Preconditions.checkArgument(from <= to, "Line range is not valid: %s must be greater than %s", from, to);

    this.from = from;
    this.to = to;
  }

  public boolean in(int lineId) {
    return from <= lineId && lineId <= to;
  }

  public Set<Integer> toLines() {
    Set<Integer> lines = Sets.newLinkedHashSet();
    for (int index = from; index <= to; index++) {
      lines.add(index);
    }
    return lines;
  }

  @Override
  public String toString() {
    return "[" + from + "-" + to + "]";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + from;
    result = prime * result + to;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    if (fieldsDiffer((LineRange) obj)) {
      return false;
    }
    return true;
  }

  private boolean fieldsDiffer(LineRange other) {
    if (from != other.from) {
      return true;
    }
    if (to != other.to) {
      return true;
    }
    return false;
  }
}
