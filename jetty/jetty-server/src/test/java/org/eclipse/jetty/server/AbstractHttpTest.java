//
//  ========================================================================
//  Copyright (c) 1995-2015 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.io.ArrayByteBufferPool;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.toolchain.test.TestTracker;
import org.eclipse.jetty.toolchain.test.http.SimpleHttpParser;
import org.eclipse.jetty.toolchain.test.http.SimpleHttpResponse;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.StdErrLog;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public abstract class AbstractHttpTest
{
    @Rule
    public TestTracker tracker = new TestTracker();

    protected static Server server;
    protected static ServerConnector connector;
    protected String httpVersion;
    protected SimpleHttpParser httpParser;

    public AbstractHttpTest(String httpVersion)
    {
        this.httpVersion = httpVersion;
    }

    @Before
    public void setUp() throws Exception
    {
        server = new Server();
        connector = new ServerConnector(server,null,null,new ArrayByteBufferPool(64,2048,64*1024),1,1,new HttpConnectionFactory());
        connector.setIdleTimeout(10000);
        
        server.addConnector(connector);
        httpParser = new SimpleHttpParser();
        ((StdErrLog)Log.getLogger(HttpChannel.class)).setHideStacks(true);
    }

    @After
    public void tearDown() throws Exception
    {
        server.stop();
        ((StdErrLog)Log.getLogger(HttpChannel.class)).setHideStacks(false);
    }

    protected SimpleHttpResponse executeRequest() throws URISyntaxException, IOException
    {
        Socket socket = new Socket("localhost", connector.getLocalPort());
        socket.setSoTimeout((int)connector.getIdleTimeout());
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

        writer.write("GET / " + httpVersion + "\r\n");
        writer.write("Host: localhost\r\n");
        writer.write("\r\n");
        writer.flush();

        SimpleHttpResponse response = httpParser.readResponse(reader);
        if ("HTTP/1.1".equals(httpVersion) && response.getHeaders().get("content-length") == null && response
                .getHeaders().get("transfer-encoding") == null)
            assertThat("If HTTP/1.1 response doesn't contain transfer-encoding or content-length headers, " +
                    "it should contain connection:close", response.getHeaders().get("connection"), is("close"));
        return response;
    }

    protected void assertResponseBody(SimpleHttpResponse response, String expectedResponseBody)
    {
        assertThat("response body is" + expectedResponseBody, response.getBody(), is(expectedResponseBody));
    }

    protected void assertHeader(SimpleHttpResponse response, String headerName, String expectedValue)
    {
        assertThat(headerName + "=" + expectedValue, response.getHeaders().get(headerName), is(expectedValue));
    }

    protected static class TestCommitException extends IllegalStateException
    {
        public TestCommitException()
        {
            super("Thrown by test");
        }
    }

    protected class ThrowExceptionOnDemandHandler extends AbstractHandler
    {
        private final boolean throwException;
        private volatile Throwable failure;

        protected ThrowExceptionOnDemandHandler(boolean throwException)
        {
            this.throwException = throwException;
        }

        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
        {
            if (throwException)
                throw new TestCommitException();
        }

        protected void markFailed(Throwable x)
        {
            this.failure = x;
        }

        public Throwable failure()
        {
            return failure;
        }
    }

}
