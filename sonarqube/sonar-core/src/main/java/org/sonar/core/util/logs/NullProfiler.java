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
package org.sonar.core.util.logs;

import javax.annotation.Nullable;

class NullProfiler extends Profiler {

  static final NullProfiler NULL_INSTANCE = new NullProfiler();

  private NullProfiler() {
  }

  @Override
  public boolean isDebugEnabled() {
    return false;
  }

  @Override
  public boolean isTraceEnabled() {
    return false;
  }

  @Override
  public Profiler start() {
    return this;
  }

  @Override
  public Profiler startTrace(String message) {
    return this;
  }

  @Override
  public Profiler startDebug(String message) {
    return this;
  }

  @Override
  public Profiler startInfo(String message) {
    return this;
  }

  @Override
  public long stopTrace() {
    return 0;
  }

  @Override
  public long stopDebug() {
    return 0;
  }

  @Override
  public long stopInfo() {
    return 0;
  }

  @Override
  public long stopTrace(String message) {
    return 0;
  }

  @Override
  public long stopDebug(String message) {
    return 0;
  }

  @Override
  public long stopInfo(String message) {
    return 0;
  }

  @Override
  public Profiler addContext(String key, @Nullable Object value) {
    // nothing to do
    return this;
  }
}
