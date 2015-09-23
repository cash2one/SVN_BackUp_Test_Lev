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
package org.sonar.process.monitor;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.slf4j.Logger;

import java.io.InputStream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

public class StreamGobblerTest {

  @Test
  public void forward_stream_to_log() {
    InputStream stream = IOUtils.toInputStream("one\nsecond log\nthird log\n");
    Logger logger = mock(Logger.class);

    StreamGobbler gobbler = new StreamGobbler(stream, "WEB", logger);
    verifyZeroInteractions(logger);

    gobbler.start();
    StreamGobbler.waitUntilFinish(gobbler);

    verify(logger).info("one");
    verify(logger).info("second log");
    verify(logger).info("third log");
    verifyNoMoreInteractions(logger);
  }
}
