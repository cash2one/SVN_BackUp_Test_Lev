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
package org.sonar.server.computation.monitoring;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CEQueueStatusImplConcurrentTest {
  private ExecutorService executorService = Executors.newFixedThreadPool(10, new ThreadFactory() {
    private int cnt = 0;

    @Override
    public Thread newThread(Runnable r) {
      return new Thread(r, CEQueueStatusImplConcurrentTest.class.getSimpleName() + cnt++);
    }
  });
  private CEQueueStatusImpl underTest = new CEQueueStatusImpl();

  @After
  public void tearDown() throws Exception {
    executorService.shutdownNow();
  }

  @Test
  public void test_concurrent_modifications_in_any_order() throws InterruptedException {
    for (Runnable runnable : buildShuffleCallsToUnderTest()) {
      executorService.submit(runnable);
    }

    executorService.awaitTermination(1, TimeUnit.SECONDS);

    assertThat(underTest.getReceivedCount()).isEqualTo(100);
    assertThat(underTest.getPendingCount()).isEqualTo(2);
    assertThat(underTest.getInProgressCount()).isEqualTo(1);
    assertThat(underTest.getErrorCount()).isEqualTo(17);
    assertThat(underTest.getSuccessCount()).isEqualTo(80);
    assertThat(underTest.getProcessingTime()).isEqualTo(177);
  }

  private List<Runnable> buildShuffleCallsToUnderTest() {
    List<Runnable> res = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      res.add(new AddReceivedRunnable());
    }
    for (int i = 0; i < 98; i++) {
      res.add(new AddInProgressRunnable());
    }
    for (int i = 0; i < 80; i++) {
      res.add(new AddSuccessRunnable());
    }
    for (int i = 0; i < 17; i++) {
      res.add(new AddErrorRunnable());
    }
    Collections.shuffle(res);
    return res;
  }

  private class AddReceivedRunnable implements Runnable {
    @Override
    public void run() {
      underTest.addReceived();
    }
  }

  private class AddInProgressRunnable implements Runnable {
    @Override
    public void run() {
      underTest.addInProgress();
    }
  }

  private class AddErrorRunnable implements Runnable {
    @Override
    public void run() {
      underTest.addError(1);
    }
  }

  private class AddSuccessRunnable implements Runnable {
    @Override
    public void run() {
      underTest.addSuccess(2);
    }
  }
}
