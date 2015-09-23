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
package org.sonar.api.utils.log;

import ch.qos.logback.classic.Level;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class LogbackLoggerTest {

  LogbackLogger underTest = new LogbackLogger((ch.qos.logback.classic.Logger)LoggerFactory.getLogger(getClass()));

  @Rule
  public LogTester tester = new LogTester();

  @Test
  public void log() {
    // no assertions. Simply verify that calls do not fail.
    underTest.trace("message");
    underTest.trace("message {}", "foo");
    underTest.trace("message {} {}", "foo", "bar");
    underTest.trace("message {} {} {}", "foo", "bar", "baz");

    underTest.debug("message");
    underTest.debug("message {}", "foo");
    underTest.debug("message {} {}", "foo", "bar");
    underTest.debug("message {} {} {}", "foo", "bar", "baz");

    underTest.info("message");
    underTest.info("message {}", "foo");
    underTest.info("message {} {}", "foo", "bar");
    underTest.info("message {} {} {}", "foo", "bar", "baz");

    underTest.warn("message");
    underTest.warn("message {}", "foo");
    underTest.warn("message {} {}", "foo", "bar");
    underTest.warn("message {} {} {}", "foo", "bar", "baz");

    underTest.error("message");
    underTest.error("message {}", "foo");
    underTest.error("message {} {}", "foo", "bar");
    underTest.error("message {} {} {}", "foo", "bar", "baz");
    underTest.error("message", new IllegalArgumentException(""));
  }

  @Test
  public void change_level() {
    assertThat(underTest.setLevel(LoggerLevel.INFO)).isTrue();
    assertThat(underTest.logbackLogger().getLevel()).isEqualTo(Level.INFO);
    assertThat(underTest.isDebugEnabled()).isFalse();
    assertThat(underTest.isTraceEnabled()).isFalse();

    assertThat(underTest.setLevel(LoggerLevel.DEBUG)).isTrue();
    assertThat(underTest.isDebugEnabled()).isTrue();
    assertThat(underTest.isTraceEnabled()).isFalse();
    assertThat(underTest.logbackLogger().getLevel()).isEqualTo(Level.DEBUG);

    assertThat(underTest.setLevel(LoggerLevel.TRACE)).isTrue();
    assertThat(underTest.isDebugEnabled()).isTrue();
    assertThat(underTest.isTraceEnabled()).isTrue();
    assertThat(underTest.logbackLogger().getLevel()).isEqualTo(Level.TRACE);
  }

  @Test
  public void info_level_can_not_be_disabled() {
    try {
      underTest.setLevel(LoggerLevel.ERROR);
      fail();

    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Only TRACE, DEBUG and INFO logging levels are supported. Got: ERROR");
    }


  }
}
