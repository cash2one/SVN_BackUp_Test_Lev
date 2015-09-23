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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

public class DateUtilsTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void parseDate_valid_format() {
    Date date = DateUtils.parseDate("2010-05-18");
    assertThat(date.getDate()).isEqualTo(18);
  }

  @Test
  public void parseDate_not_valid_format() {
    thrown.expect(SonarException.class);
    DateUtils.parseDate("2010/05/18");
  }

  @Test
  public void parseDate_not_lenient() {
    thrown.expect(SonarException.class);
    DateUtils.parseDate("2010-13-18");
  }

  @Test
  public void parseDateQuietly() {
    assertThat(DateUtils.parseDateQuietly("2010/05/18")).isNull();
    Date date = DateUtils.parseDateQuietly("2010-05-18");
    assertThat(date.getDate()).isEqualTo(18);
  }

  @Test
  public void parseDate_fail_if_additional_characters() {
    thrown.expect(SonarException.class);
    DateUtils.parseDate("1986-12-04foo");
  }

  @Test
  public void parseDateTime_valid_format() {
    Date date = DateUtils.parseDateTime("2010-05-18T15:50:45+0100");
    assertThat(date.getMinutes()).isEqualTo(50);
  }

  @Test
  public void parseDateTime_not_valid_format() {
    thrown.expect(SonarException.class);
    DateUtils.parseDate("2010/05/18 10:55");
  }

  @Test
  public void parseDateTime_fail_if_additional_characters() {
    thrown.expect(SonarException.class);
    DateUtils.parseDateTime("1986-12-04T01:02:03+0300foo");
  }

  @Test
  public void parseDateTimeQuietly() {
    assertThat(DateUtils.parseDateTimeQuietly("2010/05/18 10:55")).isNull();
    Date date = DateUtils.parseDateTimeQuietly("2010-05-18T15:50:45+0100");
    assertThat(date.getMinutes()).isEqualTo(50);
  }

  @Test
  public void shouldFormatDate() {
    assertThat(DateUtils.formatDate(new Date())).startsWith("20");
    assertThat(DateUtils.formatDate(new Date()).length()).isEqualTo(10);
  }

  @Test
  public void shouldFormatDateTime() {
    assertThat(DateUtils.formatDateTime(new Date())).startsWith("20");
    assertThat(DateUtils.formatDateTime(new Date()).length()).isGreaterThan(20);
  }

  @Test
  public void format_date_time_null_safe() {
    assertThat(DateUtils.formatDateTimeNullSafe(new Date())).startsWith("20");
    assertThat(DateUtils.formatDateTimeNullSafe(new Date()).length()).isGreaterThan(20);
    assertThat(DateUtils.formatDateTimeNullSafe(null)).isEmpty();
  }

  @Test
  public void long_to_date() {
    Date date = new Date();
    assertThat(DateUtils.longToDate(date.getTime())).isEqualTo(date);
    assertThat(DateUtils.longToDate(null)).isNull();
  }

  @Test
  public void date_to_long() {
    Date date = new Date();
    assertThat(DateUtils.dateToLong(date)).isEqualTo(date.getTime());
    assertThat(DateUtils.dateToLong(null)).isEqualTo(null);
  }

  /**
   * Cordially copied from XStream unit test
   * See http://koders.com/java/fid8A231D75F2C6E6909FB26BCA11C12D08AD05FB50.aspx?s=ThreadSafeDateFormatTest
   */
  @Test
  public void shouldBeThreadSafe() throws Exception {
    final DateUtils.ThreadSafeDateFormat format = new DateUtils.ThreadSafeDateFormat("yyyy-MM-dd'T'HH:mm:ss,S z");
    final Date now = new Date();
    final List<Throwable> throwables = new ArrayList<>();

    final ThreadGroup tg = new ThreadGroup("shouldBeThreadSafe") {
      @Override
      public void uncaughtException(Thread t, Throwable e) {
        throwables.add(e);
        super.uncaughtException(t, e);
      }
    };

    final int[] counter = new int[1];
    counter[0] = 0;
    final Thread[] threads = new Thread[10];
    for (int i = 0; i < threads.length; ++i) {
      threads[i] = new Thread(tg, "JUnit Thread " + i) {

        @Override
        public void run() {
          int i = 0;
          try {
            synchronized (this) {
              notifyAll();
              wait();
            }
            while (i < 1000 && !interrupted()) {
              String formatted = format.format(now);
              Thread.yield();
              assertThat(now).isEqualTo(format.parse(formatted));
              ++i;
            }
          } catch (Exception e) {
            fail("Unexpected exception: " + e);
          }
          synchronized (counter) {
            counter[0] += i;
          }
        }

      };
    }

    for (int i = 0; i < threads.length; ++i) {
      synchronized (threads[i]) {
        threads[i].start();
        threads[i].wait();
      }
    }

    for (int i = 0; i < threads.length; ++i) {
      synchronized (threads[i]) {
        threads[i].notifyAll();
      }
    }

    Thread.sleep(1000);

    for (int i = 0; i < threads.length; ++i) {
      threads[i].interrupt();
    }
    for (int i = 0; i < threads.length; ++i) {
      synchronized (threads[i]) {
        threads[i].join();
      }
    }

    assertThat(throwables).isEmpty();
    assertThat(counter[0]).isGreaterThanOrEqualTo(threads.length);
  }
}
