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
package org.sonar.server.computation;

import org.junit.Test;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeTaskTypes;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class CleanReportQueueListenerTest {

  ReportFiles reportFiles = mock(ReportFiles.class);
  CleanReportQueueListener underTest = new CleanReportQueueListener(reportFiles);

  @Test
  public void remove_report_file_if_success() {
    CeTask task = new CeTask.Builder().setUuid("TASK_1").setType(CeTaskTypes.REPORT).setComponentUuid("PROJECT_1").setSubmitterLogin(null).build();

    underTest.onRemoved(task, CeActivityDto.Status.SUCCESS);
    verify(reportFiles).deleteIfExists("TASK_1");
  }

  @Test
  public void remove_report_file_if_failure() {
    CeTask task = new CeTask.Builder().setUuid("TASK_1").setType(CeTaskTypes.REPORT).setComponentUuid("PROJECT_1").setSubmitterLogin(null).build();

    underTest.onRemoved(task, CeActivityDto.Status.FAILED);
    verify(reportFiles).deleteIfExists("TASK_1");
  }
}
