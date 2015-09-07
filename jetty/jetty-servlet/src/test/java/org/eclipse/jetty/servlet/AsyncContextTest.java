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

package org.eclipse.jetty.servlet;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.LocalConnector;
import org.eclipse.jetty.server.QuietServletException;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * This tests the correct functioning of the AsyncContext
 *
 * tests for #371649 and #371635
 */
public class AsyncContextTest
{

    private Server _server;
    private ServletContextHandler _contextHandler;
    private LocalConnector _connector;

    @Before
    public void setUp() throws Exception
    {
        _server = new Server();
        _contextHandler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        _connector = new LocalConnector(_server);
        _connector.setIdleTimeout(5000);
        _connector.getConnectionFactory(HttpConnectionFactory.class).getHttpConfiguration().setSendDateHeader(false);
        _server.setConnectors(new Connector[]
        { _connector });

        _contextHandler.setContextPath("/ctx");
        _contextHandler.addServlet(new ServletHolder(new TestServlet()),"/servletPath");
        _contextHandler.addServlet(new ServletHolder(new TestServlet()),"/path with spaces/servletPath");
        _contextHandler.addServlet(new ServletHolder(new TestServlet2()),"/servletPath2");
        _contextHandler.addServlet(new ServletHolder(new TestStartThrowServlet()),"/startthrow/*");
        _contextHandler.addServlet(new ServletHolder(new ForwardingServlet()),"/forward");
        _contextHandler.addServlet(new ServletHolder(new AsyncDispatchingServlet()),"/dispatchingServlet");
        _contextHandler.addServlet(new ServletHolder(new ExpireServlet()),"/expire/*");
        _contextHandler.addServlet(new ServletHolder(new BadExpireServlet()),"/badexpire/*");
        _contextHandler.addServlet(new ServletHolder(new ErrorServlet()),"/error/*");
        
        ErrorPageErrorHandler error_handler = new ErrorPageErrorHandler();
        _contextHandler.setErrorHandler(error_handler);
        error_handler.addErrorPage(500,"/error/500");
        error_handler.addErrorPage(IOException.class.getName(),"/error/IOE");

        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[]
        { _contextHandler, new DefaultHandler() });

        _server.setHandler(handlers);
        _server.start();
    }

    @After
    public void after() throws Exception
    {
        _server.stop();
    }

    @Test
    public void testSimpleAsyncContext() throws Exception
    {
        String request = "GET /ctx/servletPath HTTP/1.1\r\n" + "Host: localhost\r\n" + "Content-Type: application/x-www-form-urlencoded\r\n"
                + "Connection: close\r\n" + "\r\n";
        String responseString = _connector.getResponses(request);


        BufferedReader br = parseHeader(responseString);

        Assert.assertEquals("servlet gets right path", "doGet:getServletPath:/servletPath", br.readLine());
        Assert.assertEquals("async context gets right path in get","doGet:async:getServletPath:/servletPath",br.readLine());
        Assert.assertEquals("async context gets right path in async","async:run:attr:servletPath:/servletPath",br.readLine());
   
    }

    @Test
    public void testStartThrow() throws Exception
    {
        String request = 
          "GET /ctx/startthrow HTTP/1.1\r\n" + 
          "Host: localhost\r\n" + 
          "Connection: close\r\n" + 
          "\r\n";
        String responseString = _connector.getResponses(request);

        BufferedReader br = new BufferedReader(new StringReader(responseString));

        assertEquals("HTTP/1.1 500 Server Error",br.readLine());
        br.readLine();// connection close
        br.readLine();// server
        br.readLine();// empty

        Assert.assertEquals("error servlet","ERROR: /error",br.readLine());
        Assert.assertEquals("error servlet","PathInfo= /IOE",br.readLine());
        Assert.assertEquals("error servlet","EXCEPTION: org.eclipse.jetty.server.QuietServletException: java.io.IOException: Test",br.readLine());
    }

    @Test
    public void testStartDispatchThrow() throws Exception
    {
        String request = "GET /ctx/startthrow?dispatch=true HTTP/1.1\r\n" + 
           "Host: localhost\r\n" + 
           "Content-Type: application/x-www-form-urlencoded\r\n" + 
           "Connection: close\r\n" + 
           "\r\n";
        String responseString = _connector.getResponses(request);

        BufferedReader br = new BufferedReader(new StringReader(responseString));

        assertEquals("HTTP/1.1 500 Server Error",br.readLine());
        br.readLine();// connection close
        br.readLine();// server
        br.readLine();// empty
        Assert.assertEquals("error servlet","ERROR: /error",br.readLine());
        Assert.assertEquals("error servlet","PathInfo= /IOE",br.readLine());
        Assert.assertEquals("error servlet","EXCEPTION: org.eclipse.jetty.server.QuietServletException: java.io.IOException: Test",br.readLine());
    }
    
    @Test
    public void testStartCompleteThrow() throws Exception
    {
        String request = "GET /ctx/startthrow?complete=true HTTP/1.1\r\n" + 
           "Host: localhost\r\n" + 
           "Content-Type: application/x-www-form-urlencoded\r\n" + 
           "Connection: close\r\n" + 
           "\r\n";
        String responseString = _connector.getResponses(request);

        BufferedReader br = new BufferedReader(new StringReader(responseString));

        assertEquals("HTTP/1.1 500 Server Error",br.readLine());
        br.readLine();// connection close
        br.readLine();// server
        br.readLine();// empty
        Assert.assertEquals("error servlet","ERROR: /error",br.readLine());
        Assert.assertEquals("error servlet","PathInfo= /IOE",br.readLine());
        Assert.assertEquals("error servlet","EXCEPTION: org.eclipse.jetty.server.QuietServletException: java.io.IOException: Test",br.readLine());
    }
    
    @Test
    public void testStartFlushCompleteThrow() throws Exception
    {
        String request = "GET /ctx/startthrow?flush=true&complete=true HTTP/1.1\r\n" + 
           "Host: localhost\r\n" + 
           "Content-Type: application/x-www-form-urlencoded\r\n" + 
           "Connection: close\r\n" + 
           "\r\n";
        String responseString = _connector.getResponses(request);

        BufferedReader br = new BufferedReader(new StringReader(responseString));

        assertEquals("HTTP/1.1 200 OK",br.readLine());
        br.readLine();// connection close
        br.readLine();// server
        br.readLine();// empty

        Assert.assertEquals("error servlet","completeBeforeThrow",br.readLine());
    }
    
    @Test
    public void testDispatchAsyncContext() throws Exception
    {
        String request = "GET /ctx/servletPath?dispatch=true HTTP/1.1\r\n" + "Host: localhost\r\n" + "Content-Type: application/x-www-form-urlencoded\r\n"
                + "Connection: close\r\n" + "\r\n";
        String responseString = _connector.getResponses(request);

        BufferedReader br = parseHeader(responseString);

        Assert.assertEquals("servlet gets right path","doGet:getServletPath:/servletPath2",br.readLine());
        Assert.assertEquals("async context gets right path in get","doGet:async:getServletPath:/servletPath2",br.readLine());
        Assert.assertEquals("servlet path attr is original","async:run:attr:servletPath:/servletPath",br.readLine());
        Assert.assertEquals("path info attr is correct","async:run:attr:pathInfo:null",br.readLine());
        Assert.assertEquals("query string attr is correct","async:run:attr:queryString:dispatch=true",br.readLine());
        Assert.assertEquals("context path attr is correct","async:run:attr:contextPath:/ctx",br.readLine());
        Assert.assertEquals("request uri attr is correct","async:run:attr:requestURI:/ctx/servletPath",br.readLine());

        try
        {
            __asyncContext.getRequest();
            Assert.fail();
        }
        catch (IllegalStateException e)
        {
            
        }
    }

    @Test
    public void testDispatchAsyncContextEncodedPathAndQueryString() throws Exception
    {
        String request = "GET /ctx/path%20with%20spaces/servletPath?dispatch=true&queryStringWithEncoding=space%20space HTTP/1.1\r\n" + "Host: localhost\r\n" + "Content-Type: application/x-www-form-urlencoded\r\n"
                + "Connection: close\r\n" + "\r\n";
        String responseString = _connector.getResponses(request);

        BufferedReader br = parseHeader(responseString);

        assertThat("servlet gets right path",br.readLine(),equalTo("doGet:getServletPath:/servletPath2"));
        assertThat("async context gets right path in get",br.readLine(), equalTo("doGet:async:getServletPath:/servletPath2"));
        assertThat("servlet path attr is original",br.readLine(),equalTo("async:run:attr:servletPath:/path with spaces/servletPath"));
        assertThat("path info attr is correct",br.readLine(),equalTo("async:run:attr:pathInfo:null"));
        assertThat("query string attr is correct",br.readLine(),equalTo("async:run:attr:queryString:dispatch=true&queryStringWithEncoding=space%20space"));
        assertThat("context path attr is correct",br.readLine(),equalTo("async:run:attr:contextPath:/ctx"));
        assertThat("request uri attr is correct",br.readLine(),equalTo("async:run:attr:requestURI:/ctx/path%20with%20spaces/servletPath"));
    }

    @Test
    public void testSimpleWithContextAsyncContext() throws Exception
    {
        String request = "GET /ctx/servletPath HTTP/1.1\r\n" + "Host: localhost\r\n" + "Content-Type: application/x-www-form-urlencoded\r\n"
                + "Connection: close\r\n" + "\r\n";

        String responseString = _connector.getResponses(request);

        BufferedReader br = parseHeader(responseString);

        Assert.assertEquals("servlet gets right path","doGet:getServletPath:/servletPath",br.readLine());
        Assert.assertEquals("async context gets right path in get","doGet:async:getServletPath:/servletPath",br.readLine());
        Assert.assertEquals("async context gets right path in async","async:run:attr:servletPath:/servletPath",br.readLine());
    }

    @Test
    public void testDispatchWithContextAsyncContext() throws Exception
    {
        String request = "GET /ctx/servletPath?dispatch=true HTTP/1.1\r\n" + "Host: localhost\r\n" + "Content-Type: application/x-www-form-urlencoded\r\n"
                + "Connection: close\r\n" + "\r\n";

        String responseString = _connector.getResponses(request);

        BufferedReader br = parseHeader(responseString);

        Assert.assertEquals("servlet gets right path","doGet:getServletPath:/servletPath2",br.readLine());
        Assert.assertEquals("async context gets right path in get","doGet:async:getServletPath:/servletPath2",br.readLine());
        Assert.assertEquals("servlet path attr is original","async:run:attr:servletPath:/servletPath",br.readLine());
        Assert.assertEquals("path info attr is correct","async:run:attr:pathInfo:null",br.readLine());
        Assert.assertEquals("query string attr is correct","async:run:attr:queryString:dispatch=true",br.readLine());
        Assert.assertEquals("context path attr is correct","async:run:attr:contextPath:/ctx",br.readLine());
        Assert.assertEquals("request uri attr is correct","async:run:attr:requestURI:/ctx/servletPath",br.readLine());
    }

    @Test
    public void testDispatch() throws Exception
    {
        String request = "GET /ctx/forward HTTP/1.1\r\n" + "Host: localhost\r\n" + "Content-Type: application/x-www-form-urlencoded\r\n" + "Connection: close\r\n"
                + "\r\n";

        String responseString = _connector.getResponses(request);
        BufferedReader br = parseHeader(responseString);
        assertThat("!ForwardingServlet",br.readLine(),equalTo("Dispatched back to ForwardingServlet"));
    }

    @Test
    public void testDispatchRequestResponse() throws Exception
    {
        String request = "GET /ctx/forward?dispatchRequestResponse=true HTTP/1.1\r\n" + 
           "Host: localhost\r\n" + 
           "Content-Type: application/x-www-form-urlencoded\r\n" + 
           "Connection: close\r\n" + 
           "\r\n";

        String responseString = _connector.getResponses(request);

        BufferedReader br = parseHeader(responseString);

        assertThat("!AsyncDispatchingServlet",br.readLine(),equalTo("Dispatched back to AsyncDispatchingServlet"));
    }

    private BufferedReader parseHeader(String responseString) throws IOException
    {
        BufferedReader br = new BufferedReader(new StringReader(responseString));

        assertEquals("HTTP/1.1 200 OK",br.readLine());

        br.readLine();// connection close
        br.readLine();// server
        br.readLine();// empty
        return br;
    }

    private class ForwardingServlet extends HttpServlet
    {
        private static final long serialVersionUID = 1L;

        @Override
        protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
        {
            if (request.getDispatcherType() == DispatcherType.ASYNC)
            {
                response.getOutputStream().print("Dispatched back to ForwardingServlet");
            }
            else
            {
                request.getRequestDispatcher("/dispatchingServlet").forward(request,response);
            }
        }
    }

    public static volatile AsyncContext __asyncContext; 
    
    private class AsyncDispatchingServlet extends HttpServlet
    {
        private static final long serialVersionUID = 1L;
        
        @Override
        protected void doGet(HttpServletRequest req, final HttpServletResponse response) throws ServletException, IOException
        {
            Request request = (Request)req;
            if (request.getDispatcherType() == DispatcherType.ASYNC)
            {
                response.getOutputStream().print("Dispatched back to AsyncDispatchingServlet");
            }
            else
            {
                boolean wrapped = false;
                final AsyncContext asyncContext;
                if (request.getParameter("dispatchRequestResponse") != null)
                {
                    wrapped = true;
                    asyncContext = request.startAsync(request, new Wrapper(response));
                    __asyncContext=asyncContext;
                }
                else
                {
                    asyncContext = request.startAsync();
                    __asyncContext=asyncContext;
                }

                new Thread(new DispatchingRunnable(asyncContext, wrapped)).start();
            }
        }
    }

    @Test
    public void testExpire() throws Exception
    {
        String request = "GET /ctx/expire HTTP/1.1\r\n" + 
                "Host: localhost\r\n" + 
                "Content-Type: application/x-www-form-urlencoded\r\n" +
                "Connection: close\r\n" + 
                "\r\n";
        String responseString = _connector.getResponses(request);
               
        BufferedReader br = new BufferedReader(new StringReader(responseString));

        assertEquals("HTTP/1.1 500 Async Timeout",br.readLine());

        br.readLine();// connection close
        br.readLine();// server
        br.readLine();// empty

        Assert.assertEquals("error servlet","ERROR: /error",br.readLine());
    }

    @Test
    public void testBadExpire() throws Exception
    {
        String request = "GET /ctx/badexpire HTTP/1.1\r\n" + 
          "Host: localhost\r\n" + 
          "Content-Type: application/x-www-form-urlencoded\r\n" +
          "Connection: close\r\n" + 
          "\r\n";
        String responseString = _connector.getResponses(request);
        
        BufferedReader br = new BufferedReader(new StringReader(responseString));

        assertEquals("HTTP/1.1 500 Server Error",br.readLine());
        br.readLine();// connection close
        br.readLine();// server
        br.readLine();// empty

        Assert.assertEquals("error servlet","ERROR: /error",br.readLine());
        Assert.assertEquals("error servlet","PathInfo= /500",br.readLine());
        Assert.assertEquals("error servlet","EXCEPTION: java.lang.RuntimeException: TEST",br.readLine());
    }

    private class DispatchingRunnable implements Runnable
    {
        private AsyncContext asyncContext;
        private boolean wrapped;

        public DispatchingRunnable(AsyncContext asyncContext, boolean wrapped)
        {
            this.asyncContext = asyncContext;
            this.wrapped = wrapped;
        }

        public void run()
        {
            if (wrapped)
                assertTrue(asyncContext.getResponse() instanceof Wrapper);
            asyncContext.dispatch();
        }
    }

    @After
    public void tearDown() throws Exception
    {
        _server.stop();
        _server.join();
    }


    private class ErrorServlet extends HttpServlet
    {
        private static final long serialVersionUID = 1L;

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
        {
            response.getOutputStream().print("ERROR: " + request.getServletPath() + "\n");
            response.getOutputStream().print("PathInfo= " + request.getPathInfo() + "\n");
            if (request.getAttribute(RequestDispatcher.ERROR_EXCEPTION)!=null)
                response.getOutputStream().print("EXCEPTION: " + request.getAttribute(RequestDispatcher.ERROR_EXCEPTION) + "\n");
        }
    }
    
    private class ExpireServlet extends HttpServlet
    {
        private static final long serialVersionUID = 1L;

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
        {
            if (request.getDispatcherType()==DispatcherType.REQUEST)
            {
                AsyncContext asyncContext = request.startAsync();
                asyncContext.setTimeout(100);
            }
        }
    }
    
    private class BadExpireServlet extends HttpServlet
    {
        private static final long serialVersionUID = 1L;

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
        {
            if (request.getDispatcherType()==DispatcherType.REQUEST)
            {
                AsyncContext asyncContext = request.startAsync();
                asyncContext.addListener(new AsyncListener()
                {
                    @Override
                    public void onTimeout(AsyncEvent event) throws IOException
                    {
                        throw new RuntimeException("TEST");
                    }
                    
                    @Override
                    public void onStartAsync(AsyncEvent event) throws IOException
                    {                      
                    }
                    
                    @Override
                    public void onError(AsyncEvent event) throws IOException
                    {                        
                    }
                    
                    @Override
                    public void onComplete(AsyncEvent event) throws IOException
                    {                        
                    }
                });
                asyncContext.setTimeout(100);
            }
        }
    }
    
    private class TestServlet extends HttpServlet
    {
        private static final long serialVersionUID = 1L;

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
        {
            if (request.getParameter("dispatch") != null)
            {
                AsyncContext asyncContext = request.startAsync(request,response);
                __asyncContext=asyncContext;
                asyncContext.dispatch("/servletPath2");
            }
            else
            {
                response.getOutputStream().print("doGet:getServletPath:" + request.getServletPath() + "\n");
                AsyncContext asyncContext = request.startAsync(request,response);
                __asyncContext=asyncContext;
                response.getOutputStream().print("doGet:async:getServletPath:" + ((HttpServletRequest)asyncContext.getRequest()).getServletPath() + "\n");
                asyncContext.start(new AsyncRunnable(asyncContext));

            }
        }
    }

    private class TestServlet2 extends HttpServlet
    {
        private static final long serialVersionUID = 1L;

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
        {
            response.getOutputStream().print("doGet:getServletPath:" + request.getServletPath() + "\n");
            AsyncContext asyncContext = request.startAsync(request, response);
            __asyncContext=asyncContext;
            response.getOutputStream().print("doGet:async:getServletPath:" + ((HttpServletRequest)asyncContext.getRequest()).getServletPath() + "\n");
            asyncContext.start(new AsyncRunnable(asyncContext));
        }
    }
    
    private class TestStartThrowServlet extends HttpServlet
    {
        private static final long serialVersionUID = 1L;

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
        {
            if (request.getDispatcherType()==DispatcherType.REQUEST)
            {
                request.startAsync(request, response);
                
                if (Boolean.valueOf(request.getParameter("dispatch")))
                {
                    request.getAsyncContext().dispatch();
                }

                if (Boolean.valueOf(request.getParameter("complete")))
                {
                    response.getOutputStream().write("completeBeforeThrow".getBytes());
                    if (Boolean.valueOf(request.getParameter("flush")))
                        response.flushBuffer();
                    request.getAsyncContext().complete();
                }
                    
                throw new QuietServletException(new IOException("Test"));
            }
        }
    }

    private class AsyncRunnable implements Runnable
    {
        private AsyncContext _context;

        public AsyncRunnable(AsyncContext context)
        {
            _context = context;
        }

        @Override
        public void run()
        {
            HttpServletRequest req = (HttpServletRequest)_context.getRequest();

            try
            {
                _context.getResponse().getOutputStream().print("async:run:attr:servletPath:" + req.getAttribute(AsyncContext.ASYNC_SERVLET_PATH) + "\n");
                _context.getResponse().getOutputStream().print("async:run:attr:pathInfo:" + req.getAttribute(AsyncContext.ASYNC_PATH_INFO) + "\n");
                _context.getResponse().getOutputStream().print("async:run:attr:queryString:" + req.getAttribute(AsyncContext.ASYNC_QUERY_STRING) + "\n");
                _context.getResponse().getOutputStream().print("async:run:attr:contextPath:" + req.getAttribute(AsyncContext.ASYNC_CONTEXT_PATH) + "\n");
                _context.getResponse().getOutputStream().print("async:run:attr:requestURI:" + req.getAttribute(AsyncContext.ASYNC_REQUEST_URI) + "\n");
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            _context.complete();
        }
    }

    private class Wrapper extends HttpServletResponseWrapper
    {
        public Wrapper (HttpServletResponse response)
        {
            super(response);
        }
    }


}
