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
package org.sonar.batch.bootstrap;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.apache.commons.io.IOUtils.write;

public class MockHttpServer {
  private Server server;
  private String responseBody;
  private String requestBody;
  private String mockResponseData;
  private int mockResponseStatus = SC_OK;
  private int numRequests = 0;

  public void start() throws Exception {
    server = new Server(0);
    server.setHandler(getMockHandler());
    server.start();
  }

  public int getNumberRequests() {
    return numRequests;
  }

  /**
   * Creates an {@link org.mortbay.jetty.handler.AbstractHandler handler} returning an arbitrary String as a response.
   *
   * @return never <code>null</code>.
   */
  public Handler getMockHandler() {
    Handler handler = new AbstractHandler() {

      public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        numRequests++;
        setResponseBody(getMockResponseData());
        setRequestBody(IOUtils.toString(baseRequest.getInputStream()));
        response.setStatus(mockResponseStatus);
        response.setContentType("text/xml;charset=utf-8");
        write(getResponseBody(), response.getOutputStream());
        baseRequest.setHandled(true);
      }
    };
    return handler;
  }

  public void stop() {
    try {
      if (server != null) {
        server.stop();
      }
    } catch (Exception e) {
      throw new IllegalStateException("Fail to stop HTTP server", e);
    }
  }

  public String getResponseBody() {
    return responseBody;
  }

  public void setResponseBody(String responseBody) {
    this.responseBody = responseBody;
  }

  public String getRequestBody() {
    return requestBody;
  }

  public void setRequestBody(String requestBody) {
    this.requestBody = requestBody;
  }

  public void setMockResponseStatus(int status) {
    this.mockResponseStatus = status;
  }

  public String getMockResponseData() {
    return mockResponseData;
  }

  public void setMockResponseData(String mockResponseData) {
    this.mockResponseData = mockResponseData;
  }

  public int getPort() {
    return server.getConnectors()[0].getLocalPort();
  }

}
