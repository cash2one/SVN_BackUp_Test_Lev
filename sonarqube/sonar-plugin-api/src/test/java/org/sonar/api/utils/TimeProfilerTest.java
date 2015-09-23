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
package org.sonar.api.utils;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import static org.mockito.Mockito.*;

public class TimeProfilerTest {

  private Logger logger;

  @Before
  public void before() {
    logger = mock(Logger.class);
  }

  @Test
  public void testBasicProfiling() {
    TimeProfiler profiler = new TimeProfiler(logger);
    profiler.start("Cycle analysis");
    verify(logger).info("Cycle analysis...");

    profiler.stop();
    verify(logger).info(eq("{} done: {} ms"), eq("Cycle analysis"), anyInt());
  }

  @Test
  public void stopOnce() {
    TimeProfiler profiler = new TimeProfiler(logger);

    profiler.start("Cycle analysis");
    profiler.stop();
    profiler.stop();
    profiler.stop();
    verify(logger, times(1)).info(anyString()); // start() executes log() with 1 parameter
    verify(logger, times(1)).info(anyString(), anyString(), anyInt()); // stop() executes log() with 3 parameters
  }

  @Test
  public void doNotLogNeverEndedTask() {
    TimeProfiler profiler = new TimeProfiler(logger);

    profiler.start("Cycle analysis");
    profiler.start("New task");
    profiler.stop();
    profiler.stop();
    verify(logger, never()).info(eq("{} done: {} ms"), eq("Cycle analysis"), anyInt());
    verify(logger, times(1)).info(eq("{} done: {} ms"), eq("New task"), anyInt());
  }
}
