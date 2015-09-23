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
package org.sonar.process.test;

import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.sonar.process.Monitored;
import org.sonar.process.ProcessEntryPoint;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.IOException;

/**
 * Http server used for testing (see MonitorTest). It accepts HTTP commands /ping and /kill to hardly exit.
 * It also pushes status to temp files, so test can verify what was really done (when server went ready state and
 * if it was gracefully terminated)
 */
public class HttpProcess implements Monitored {

  private final Server server;
  private boolean ready = false;
  // temp dir is specific to this process
  private final File tempDir = new File(System.getProperty("java.io.tmpdir"));

  public HttpProcess(int httpPort) {
    server = new Server(httpPort);
  }

  @Override
  public void start() {
    writeTimeToFile("startingAt");
    ContextHandler context = new ContextHandler();
    context.setContextPath("/");
    context.setClassLoader(Thread.currentThread().getContextClassLoader());
    server.setHandler(context);
    context.setHandler(new AbstractHandler() {
      @Override
      public void handle(String target, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException, ServletException {
        if ("/ping".equals(target)) {
          request.setHandled(true);
          httpServletResponse.getWriter().print("ping");
        } else if ("/kill".equals(target)) {
          writeTimeToFile("killedAt");
          System.exit(0);
        }
      }
    });
    try {
      server.start();
    } catch (Exception e) {
      throw new IllegalStateException("Fail to start Jetty", e);
    }
  }

  @Override
  public boolean isReady() {
    if (ready) {
      return true;
    }
    if (server.isStarted()) {
      ready = true;
      writeTimeToFile("readyAt");
    }
    return ready;
  }

  @Override
  public void awaitStop() {
    try {
      server.join();
    } catch (InterruptedException ignore) {

    }
  }

  @Override
  public void stop() {
    try {
      if (!server.isStopped()) {
        server.stop();
        writeTimeToFile("terminatedAt");
      }
    } catch (Exception e) {
      throw new IllegalStateException("Fail to stop Jetty", e);
    }
  }

  private void writeTimeToFile(String filename) {
    try {
      FileUtils.write(new File(tempDir, filename), String.valueOf(System.currentTimeMillis()));
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public static void main(String[] args) {
    ProcessEntryPoint entryPoint = ProcessEntryPoint.createForArguments(args);
    entryPoint.launch(new HttpProcess(entryPoint.getProps().valueAsInt("httpPort")));
  }
}
