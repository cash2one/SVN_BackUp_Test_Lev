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

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.ServletResponseWrapper;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.QuietServletException;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.toolchain.test.AdvancedRunner;
import org.eclipse.jetty.util.IO;
import org.eclipse.jetty.util.URIUtil;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;


@RunWith(AdvancedRunner.class)
public class AsyncServletTest
{
    protected AsyncServlet _servlet=new AsyncServlet();
    protected int _port;

    protected Server _server = new Server();
    protected ServletHandler _servletHandler;
    protected ServerConnector _connector;
    protected List<String> _log;
    protected int _expectedLogs;
    protected String _expectedCode;
    protected static List<String> __history=new CopyOnWriteArrayList<>();
    protected static CountDownLatch __latch;

    @Before
    public void setUp() throws Exception
    {
        _connector = new ServerConnector(_server);
        _server.setConnectors(new Connector[]{ _connector });

        _log=new ArrayList<>();
        RequestLog log=new Log();
        RequestLogHandler logHandler = new RequestLogHandler();
        logHandler.setRequestLog(log);
        _server.setHandler(logHandler);
        _expectedLogs=1;
        _expectedCode="200 ";

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        context.setContextPath("/ctx");
        logHandler.setHandler(context);

        _servletHandler=context.getServletHandler();
        ServletHolder holder=new ServletHolder(_servlet);
        holder.setAsyncSupported(true);
        _servletHandler.addServletWithMapping(holder,"/path/*");
        _servletHandler.addServletWithMapping(holder,"/path1/*");
        _servletHandler.addServletWithMapping(holder,"/path2/*");
        _servletHandler.addServletWithMapping(holder,"/p th3/*");
        _servletHandler.addServletWithMapping(new ServletHolder(new FwdServlet()),"/fwd/*");
        _server.start();
        _port=_connector.getLocalPort();
        __history.clear();
        __latch=new CountDownLatch(1);
    }

    @After
    public void tearDown() throws Exception
    {
        _server.stop();
        assertEquals(_expectedLogs,_log.size());
        Assert.assertThat(_log.get(0), Matchers.containsString(_expectedCode));
    }

    @Test
    public void testNormal() throws Exception
    {
        String response=process(null,null);
        assertThat(response,startsWith("HTTP/1.1 200 OK"));
        assertThat(__history,contains(
                "REQUEST /ctx/path/info",
                "initial"));
        assertContains("NORMAL",response);
        assertFalse(__history.contains("onTimeout"));
        assertFalse(__history.contains("onComplete"));
    }

    @Test
    public void testSleep() throws Exception
    {
        String response=process("sleep=200",null);
        assertThat(response,startsWith("HTTP/1.1 200 OK"));
        assertThat(__history,contains(
                "REQUEST /ctx/path/info",
                "initial"));
        assertContains("SLEPT",response);
        assertFalse(__history.contains("onTimeout"));
        assertFalse(__history.contains("onComplete"));
    }

    @Test
    public void testNonAsync() throws Exception
    {
        String response=process("",null);
        Assert.assertThat(response,Matchers.startsWith("HTTP/1.1 200 OK"));
        assertThat(__history,contains(
            "REQUEST /ctx/path/info",
            "initial"));

        assertContains("NORMAL",response);
    }
    
    @Test
    public void testStart() throws Exception
    {
        _expectedCode="500 ";
        String response=process("start=200",null);
        Assert.assertThat(response,Matchers.startsWith("HTTP/1.1 500 Async Timeout"));
        assertThat(__history,contains(
            "REQUEST /ctx/path/info",
            "initial",
            "start",
            "onTimeout",
            "ERROR /ctx/path/info",
            "!initial",
            "onComplete"));

        assertContains("ERROR DISPATCH: /ctx/path/info",response);
    }

    @Test
    public void testStartOnTimeoutDispatch() throws Exception
    {
        String response=process("start=200&timeout=dispatch",null);
        assertThat(response,startsWith("HTTP/1.1 200 OK"));
        assertThat(__history,contains(
            "REQUEST /ctx/path/info",
            "initial",
            "start",
            "onTimeout",
            "dispatch",
            "ASYNC /ctx/path/info",
            "!initial",
            "onComplete"));

        assertContains("DISPATCHED",response);
    }

    @Test
    public void testStartOnTimeoutError() throws Exception
    {
        _expectedCode="500 ";
        String response=process("start=200&timeout=error",null);
        assertThat(response,startsWith("HTTP/1.1 500 Server Error"));
        assertThat(__history,contains(
            "REQUEST /ctx/path/info",
            "initial",
            "start",
            "onTimeout",
            "error",
            "onError",
            "ERROR /ctx/path/info",
            "!initial",
            "onComplete"));

        assertContains("ERROR DISPATCH",response);
    }

    @Test
    public void testStartOnTimeoutErrorComplete() throws Exception
    {
        String response=process("start=200&timeout=error&error=complete",null);
        assertThat(response,startsWith("HTTP/1.1 200 OK"));
        assertThat(__history,contains(
            "REQUEST /ctx/path/info",
            "initial",
            "start",
            "onTimeout",
            "error",
            "onError",
            "complete",
            "onComplete"));

        assertContains("COMPLETED",response);
    }

    @Test
    public void testStartOnTimeoutErrorDispatch() throws Exception
    {
        String response=process("start=200&timeout=error&error=dispatch",null);
        assertThat(response,startsWith("HTTP/1.1 200 OK"));
        assertThat(__history,contains(
            "REQUEST /ctx/path/info",
            "initial",
            "start",
            "onTimeout",
            "error",
            "onError",
            "dispatch",
            "ASYNC /ctx/path/info",
            "!initial",
            "onComplete"));

        assertContains("DISPATCHED",response);
    }

    @Test
    public void testStartOnTimeoutComplete() throws Exception
    {
        String response=process("start=200&timeout=complete",null);
        assertThat(response,startsWith("HTTP/1.1 200 OK"));
        assertThat(__history,contains(
            "REQUEST /ctx/path/info",
            "initial",
            "start",
            "onTimeout",
            "complete",
            "onComplete"));

        assertContains("COMPLETED",response);
    }

    @Test
    public void testStartWaitDispatch() throws Exception
    {
        String response=process("start=200&dispatch=10",null);
        assertThat(response,startsWith("HTTP/1.1 200 OK"));
        assertThat(__history,contains(
            "REQUEST /ctx/path/info",
            "initial",
            "start",
            "dispatch",
            "ASYNC /ctx/path/info",
            "!initial",
            "onComplete"));
        assertFalse(__history.contains("onTimeout"));
    }

    @Test
    public void testStartDispatch() throws Exception
    {
        String response=process("start=200&dispatch=0",null);
        assertThat(response,startsWith("HTTP/1.1 200 OK"));
        assertThat(__history,contains(
            "REQUEST /ctx/path/info",
            "initial",
            "start",
            "dispatch",
            "ASYNC /ctx/path/info",
            "!initial",
            "onComplete"));
    }

    @Test
    public void testStartError() throws Exception
    {
        _expectedCode="500 ";
        String response=process("start=200&throw=1",null);
        assertThat(response,startsWith("HTTP/1.1 500 Server Error"));
        assertThat(__history,contains(
            "REQUEST /ctx/path/info",
            "initial",
            "start",
            "onError",
            "ERROR /ctx/path/info",
            "!initial",
            "onComplete"));
        assertContains("ERROR DISPATCH: /ctx/path/info",response);
    }

    @Test
    public void testStartWaitComplete() throws Exception
    {
        String response=process("start=200&complete=50",null);
        assertThat(response,startsWith("HTTP/1.1 200 OK"));
        assertThat(__history,contains(
            "REQUEST /ctx/path/info",
            "initial",
            "start",
            "complete",
            "onComplete"));
        assertContains("COMPLETED",response);
        assertFalse(__history.contains("onTimeout"));
        assertFalse(__history.contains("!initial"));
    }

    @Test
    public void testStartComplete() throws Exception
    {
        String response=process("start=200&complete=0",null);
        assertThat(response,startsWith("HTTP/1.1 200 OK"));
        assertThat(__history,contains(
            "REQUEST /ctx/path/info",
            "initial",
            "start",
            "complete",
            "onComplete"));
        assertContains("COMPLETED",response);
        assertFalse(__history.contains("onTimeout"));
        assertFalse(__history.contains("!initial"));
    }

    @Test
    public void testStartWaitDispatchStartWaitDispatch() throws Exception
    {
        String response=process("start=1000&dispatch=10&start2=1000&dispatch2=10",null);
        assertThat(response,startsWith("HTTP/1.1 200 OK"));
        assertThat(__history,contains(
            "REQUEST /ctx/path/info",
            "initial",
            "start",
            "dispatch",
            "ASYNC /ctx/path/info",
            "!initial",
            "onStartAsync",
            "start",
            "dispatch",
            "ASYNC /ctx/path/info",
            "!initial",
            "onComplete"));
        assertContains("DISPATCHED",response);
    }

    @Test
    public void testStartWaitDispatchStartComplete() throws Exception
    {
        String response=process("start=1000&dispatch=10&start2=1000&complete2=10",null);
        assertThat(response,startsWith("HTTP/1.1 200 OK"));
        assertThat(__history,contains(
            "REQUEST /ctx/path/info",
            "initial",
            "start",
            "dispatch",
            "ASYNC /ctx/path/info",
            "!initial",
            "onStartAsync",
            "start",
            "complete",
            "onComplete"));
        assertContains("COMPLETED",response);
    }

    @Test
    public void testStartWaitDispatchStart() throws Exception
    {
        _expectedCode="500 ";
        String response=process("start=1000&dispatch=10&start2=10",null);
        assertEquals("HTTP/1.1 500 Async Timeout",response.substring(0,26));
        assertThat(__history,contains(
            "REQUEST /ctx/path/info",
            "initial",
            "start",
            "dispatch",
            "ASYNC /ctx/path/info",
            "!initial",
            "onStartAsync",
            "start",
            "onTimeout",
            "ERROR /ctx/path/info",
            "!initial",
            "onComplete"));
        assertContains("ERROR DISPATCH: /ctx/path/info",response);
    }

    @Test
    public void testStartTimeoutStartDispatch() throws Exception
    {
        String response=process("start=10&start2=1000&dispatch2=10",null);
        assertThat(response,startsWith("HTTP/1.1 200 OK"));
        assertThat(__history,contains(
            "REQUEST /ctx/path/info",
            "initial",
            "start",
            "onTimeout",
            "ERROR /ctx/path/info",
            "!initial",
            "onStartAsync",
            "start",
            "dispatch",
            "ASYNC /ctx/path/info",
            "!initial",
            "onComplete"));
        assertContains("DISPATCHED",response);
    }

    @Test
    public void testStartTimeoutStartComplete() throws Exception
    {
        String response=process("start=10&start2=1000&complete2=10",null);
        assertThat(response,startsWith("HTTP/1.1 200 OK"));
        assertThat(__history,contains(
            "REQUEST /ctx/path/info",
            "initial",
            "start",
            "onTimeout",
            "ERROR /ctx/path/info",
            "!initial",
            "onStartAsync",
            "start",
            "complete",
            "onComplete"));
        assertContains("COMPLETED",response);
    }

    @Test
    public void testStartTimeoutStart() throws Exception
    {
        _expectedCode="500 ";
        String response=process("start=10&start2=10",null);
        assertThat(__history,contains(
            "REQUEST /ctx/path/info",
            "initial",
            "start",
            "onTimeout",
            "ERROR /ctx/path/info",
            "!initial",
            "onStartAsync",
            "start",
            "onTimeout",
            "ERROR /ctx/path/info",
            "!initial",
            "onComplete"));
        assertContains("ERROR DISPATCH: /ctx/path/info",response);
    }

    @Test
    public void testWrapStartDispatch() throws Exception
    {
        String response=process("wrap=true&start=200&dispatch=20",null);
        assertThat(response,startsWith("HTTP/1.1 200 OK"));
        assertThat(__history,contains(
            "REQUEST /ctx/path/info",
            "initial",
            "start",
            "dispatch",
            "ASYNC /ctx/path/info",
            "wrapped REQ RSP",
            "!initial",
            "onComplete"));
        assertContains("DISPATCHED",response);
    }


    @Test
    public void testStartDispatchEncodedPath() throws Exception
    {
        String response=process("start=200&dispatch=20&path=/p%20th3",null);
        assertThat(response,startsWith("HTTP/1.1 200 OK"));
        assertThat(__history,contains(
            "REQUEST /ctx/path/info",
            "initial",
            "start",
            "dispatch",
            "ASYNC /ctx/p%20th3",
            "!initial",
            "onComplete"));
        assertContains("DISPATCHED",response);
    }


    @Test
    public void testFwdStartDispatch() throws Exception
    {
        String response=process("fwd","start=200&dispatch=20",null);
        assertThat(response,startsWith("HTTP/1.1 200 OK"));
        assertThat(__history,contains(
            "FWD REQUEST /ctx/fwd/info",
            "FORWARD /ctx/path1",
            "initial",
            "start",
            "dispatch",
            "FWD ASYNC /ctx/fwd/info",
            "FORWARD /ctx/path1",
            "!initial",
            "onComplete"));
        assertContains("DISPATCHED",response);
    }

    @Test
    public void testFwdStartDispatchPath() throws Exception
    {
        String response=process("fwd","start=200&dispatch=20&path=/path2",null);
        assertThat(response,startsWith("HTTP/1.1 200 OK"));
        assertThat(__history,contains(
            "FWD REQUEST /ctx/fwd/info",
            "FORWARD /ctx/path1",
            "initial",
            "start",
            "dispatch",
            "ASYNC /ctx/path2",
            "!initial",
            "onComplete"));
        assertContains("DISPATCHED",response);
    }

    @Test
    public void testFwdWrapStartDispatch() throws Exception
    {
        String response=process("fwd","wrap=true&start=200&dispatch=20",null);
        assertThat(response,startsWith("HTTP/1.1 200 OK"));
        assertThat(__history,contains(
            "FWD REQUEST /ctx/fwd/info",
            "FORWARD /ctx/path1",
            "initial",
            "start",
            "dispatch",
            "ASYNC /ctx/path1",
            "wrapped REQ RSP",
            "!initial",
            "onComplete"));
        assertContains("DISPATCHED",response);
    }

    @Test
    public void testFwdWrapStartDispatchPath() throws Exception
    {
        String response=process("fwd","wrap=true&start=200&dispatch=20&path=/path2",null);
        assertThat(response,startsWith("HTTP/1.1 200 OK"));
        assertThat(__history,contains(
            "FWD REQUEST /ctx/fwd/info",
            "FORWARD /ctx/path1",
            "initial",
            "start",
            "dispatch",
            "ASYNC /ctx/path2",
            "wrapped REQ RSP",
            "!initial",
            "onComplete"));
        assertContains("DISPATCHED",response);
    }


    @Test
    public void testAsyncRead() throws Exception
    {
        String header="GET /ctx/path/info?start=2000&dispatch=1500 HTTP/1.1\r\n"+
            "Host: localhost\r\n"+
            "Content-Length: 10\r\n"+
            "Connection: close\r\n"+
            "\r\n";
        String body="12345678\r\n";

        try (Socket socket = new Socket("localhost",_port))
        {
            socket.setSoTimeout(10000);
            socket.getOutputStream().write(header.getBytes(StandardCharsets.ISO_8859_1));
            socket.getOutputStream().write(body.getBytes(StandardCharsets.ISO_8859_1),0,2);
            Thread.sleep(500);
            socket.getOutputStream().write(body.getBytes(StandardCharsets.ISO_8859_1),2,8);

            String response = IO.toString(socket.getInputStream());
            __latch.await(1,TimeUnit.SECONDS);
            assertThat(response,startsWith("HTTP/1.1 200 OK"));
            assertThat(__history,contains(
                "REQUEST /ctx/path/info",
                "initial",
                "start",
                "async-read=10",
                "dispatch",
                "ASYNC /ctx/path/info",
                "!initial",
                "onComplete"));
        }
    }

    public synchronized String process(String query,String content) throws Exception
    {
        return process("path",query,content);
    }

    public synchronized String process(String path,String query,String content) throws Exception
    {
        String request = "GET /ctx/"+path+"/info";

        if (query!=null)
            request+="?"+query;
        request+=" HTTP/1.1\r\n"+
        "Host: localhost\r\n"+
        "Connection: close\r\n";
        if (content==null)
            request+="\r\n";
        else
        {
            request+="Content-Length: "+content.length()+"\r\n";
            request+="\r\n" + content;
        }

        int port=_port;
        try (Socket socket = new Socket("localhost",port))
        {
            socket.setSoTimeout(1000000);
            socket.getOutputStream().write(request.getBytes(StandardCharsets.UTF_8));
            socket.getOutputStream().flush();
            String response = IO.toString(socket.getInputStream());
            __latch.await(1,TimeUnit.SECONDS);
            return response;
        }
        catch(Exception e)
        {
            System.err.println("failed on port "+port);
            e.printStackTrace();
            throw e;
        }
        
    }

    protected void assertContains(String content,String response)
    {
        Assert.assertThat(response, Matchers.containsString(content));
    }

    protected void assertNotContains(String content,String response)
    {
        Assert.assertThat(response,Matchers.not(Matchers.containsString(content)));
    }

    private static class FwdServlet extends HttpServlet
    {
        @Override
        public void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
        {
            __history.add("FWD "+request.getDispatcherType()+" "+request.getRequestURI());
            if (request instanceof ServletRequestWrapper || response instanceof ServletResponseWrapper)
                __history.add("wrapped"+((request instanceof ServletRequestWrapper)?" REQ":"")+((response instanceof ServletResponseWrapper)?" RSP":""));
            request.getServletContext().getRequestDispatcher("/path1").forward(request,response);
        }
    }

    private static class AsyncServlet extends HttpServlet
    {
        private static final long serialVersionUID = -8161977157098646562L;
        private final Timer _timer=new Timer();

        @Override
        public void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
        {
            // this should always fail at this point
            try
            {
                request.getAsyncContext();
                throw new IllegalStateException();
            }
            catch(IllegalStateException e)
            {
                // ignored
            }

            // System.err.println(request.getDispatcherType()+" "+request.getRequestURI());
            __history.add(request.getDispatcherType()+" "+request.getRequestURI());
            if (request instanceof ServletRequestWrapper || response instanceof ServletResponseWrapper)
                __history.add("wrapped"+((request instanceof ServletRequestWrapper)?" REQ":"")+((response instanceof ServletResponseWrapper)?" RSP":""));

            boolean wrap="true".equals(request.getParameter("wrap"));
            int read_before=0;
            long sleep_for=-1;
            long start_for=-1;
            long start2_for=-1;
            long dispatch_after=-1;
            long dispatch2_after=-1;
            long complete_after=-1;
            long complete2_after=-1;


            if (request.getParameter("read")!=null)
                read_before=Integer.parseInt(request.getParameter("read"));
            if (request.getParameter("sleep")!=null)
                sleep_for=Integer.parseInt(request.getParameter("sleep"));
            if (request.getParameter("start")!=null)
                start_for=Integer.parseInt(request.getParameter("start"));
            if (request.getParameter("start2")!=null)
                start2_for=Integer.parseInt(request.getParameter("start2"));
            if (request.getParameter("dispatch")!=null)
                dispatch_after=Integer.parseInt(request.getParameter("dispatch"));
            final String path=request.getParameter("path");
            if (request.getParameter("dispatch2")!=null)
                dispatch2_after=Integer.parseInt(request.getParameter("dispatch2"));
            if (request.getParameter("complete")!=null)
                complete_after=Integer.parseInt(request.getParameter("complete"));
            if (request.getParameter("complete2")!=null)
                complete2_after=Integer.parseInt(request.getParameter("complete2"));

            if (request.getAttribute("State")==null)
            {
                request.setAttribute("State",new Integer(1));
                __history.add("initial");
                if (read_before>0)
                {
                    byte[] buf=new byte[read_before];
                    request.getInputStream().read(buf);
                }
                else if (read_before<0)
                {
                    InputStream in = request.getInputStream();
                    int b=in.read();
                    while(b!=-1)
                        b=in.read();
                }
                else if (request.getContentLength()>0)
                {
                    new Thread()
                    {
                        @Override
                        public void run()
                        {
                            int c=0;
                            try
                            {
                                InputStream in=request.getInputStream();
                                int b=0;
                                while(b!=-1)
                                    if((b=in.read())>=0)
                                        c++;
                                __history.add("async-read="+c);
                            }
                            catch(Exception e)
                            {
                                e.printStackTrace();
                            }
                        }
                    }.start();
                }

                if (start_for>=0)
                {
                    final AsyncContext async=wrap?request.startAsync(new HttpServletRequestWrapper(request),new HttpServletResponseWrapper(response)):request.startAsync();
                    if (start_for>0)
                        async.setTimeout(start_for);
                    async.addListener(__listener);
                    __history.add("start");

                    if ("1".equals(request.getParameter("throw")))
                        throw new QuietServletException(new Exception("test throw in async 1"));

                    if (complete_after>0)
                    {
                        TimerTask complete = new TimerTask()
                        {
                            @Override
                            public void run()
                            {
                                try
                                {
                                    response.setStatus(200);
                                    response.getOutputStream().println("COMPLETED\n");
                                    __history.add("complete");
                                    async.complete();
                                }
                                catch(Exception e)
                                {
                                    e.printStackTrace();
                                }
                            }
                        };
                        synchronized (_timer)
                        {
                            _timer.schedule(complete,complete_after);
                        }
                    }
                    else if (complete_after==0)
                    {
                        response.setStatus(200);
                        response.getOutputStream().println("COMPLETED\n");
                        __history.add("complete");
                        async.complete();
                    }
                    else if (dispatch_after>0)
                    {
                        TimerTask dispatch = new TimerTask()
                        {
                            @Override
                            public void run()
                            {
                                __history.add("dispatch");
                                if (path!=null)
                                {
                                    int q=path.indexOf('?');
                                    String uriInContext=(q>=0)
                                        ?URIUtil.encodePath(path.substring(0,q))+path.substring(q)
                                        :URIUtil.encodePath(path);
                                    async.dispatch(uriInContext);
                                }
                                else
                                    async.dispatch();
                            }
                        };
                        synchronized (_timer)
                        {
                            _timer.schedule(dispatch,dispatch_after);
                        }
                    }
                    else if (dispatch_after==0)
                    {
                        __history.add("dispatch");
                        if (path!=null)
                            async.dispatch(path);
                        else
                            async.dispatch();
                    }

                }
                else if (sleep_for>=0)
                {
                    try
                    {
                        Thread.sleep(sleep_for);
                    }
                    catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                    response.setStatus(200);
                    response.getOutputStream().println("SLEPT\n");
                }
                else
                {
                    response.setStatus(200);
                    response.getOutputStream().println("NORMAL\n");
                }
            }
            else
            {
                __history.add("!initial");

                if (start2_for>=0 && request.getAttribute("2nd")==null)
                {
                    final AsyncContext async=wrap?request.startAsync(new HttpServletRequestWrapper(request),new HttpServletResponseWrapper(response)):request.startAsync();
                    async.addListener(__listener);
                    request.setAttribute("2nd","cycle");

                    if (start2_for>0)
                    {
                        async.setTimeout(start2_for);
                    }
                    __history.add("start");

                    if ("2".equals(request.getParameter("throw")))
                        throw new QuietServletException(new Exception("test throw in async 2"));

                    if (complete2_after>0)
                    {
                        TimerTask complete = new TimerTask()
                        {
                            @Override
                            public void run()
                            {
                                try
                                {
                                    response.setStatus(200);
                                    response.getOutputStream().println("COMPLETED\n");
                                    __history.add("complete");
                                    async.complete();
                                }
                                catch(Exception e)
                                {
                                    e.printStackTrace();
                                }
                            }
                        };
                        synchronized (_timer)
                        {
                            _timer.schedule(complete,complete2_after);
                        }
                    }
                    else if (complete2_after==0)
                    {
                        response.setStatus(200);
                        response.getOutputStream().println("COMPLETED\n");
                        __history.add("complete");
                        async.complete();
                    }
                    else if (dispatch2_after>0)
                    {
                        TimerTask dispatch = new TimerTask()
                        {
                            @Override
                            public void run()
                            {
                                __history.add("dispatch");
                                async.dispatch();
                            }
                        };
                        synchronized (_timer)
                        {
                            _timer.schedule(dispatch,dispatch2_after);
                        }
                    }
                    else if (dispatch2_after==0)
                    {
                        __history.add("dispatch");
                        async.dispatch();
                    }
                }
                else if(request.getDispatcherType()==DispatcherType.ERROR)
                {
                    response.getOutputStream().println("ERROR DISPATCH: "+request.getContextPath()+request.getServletPath()+request.getPathInfo());
                }
                else
                {
                    response.setStatus(200);
                    response.getOutputStream().println("DISPATCHED");
                }
            }
        }
    }


    private static AsyncListener __listener = new AsyncListener()
    {
        @Override
        public void onTimeout(AsyncEvent event) throws IOException
        {
            __history.add("onTimeout");
            String action=event.getSuppliedRequest().getParameter("timeout");
            if (action!=null)
            {
                __history.add(action);

                switch(action)
                {
                    case "dispatch":
                        event.getAsyncContext().dispatch();
                        break;

                    case "complete":
                        event.getSuppliedResponse().getOutputStream().println("COMPLETED\n");
                        event.getAsyncContext().complete();
                        break;

                    case "error":
                        throw new RuntimeException("error in onTimeout");
                }
            }
        }

        @Override
        public void onStartAsync(AsyncEvent event) throws IOException
        {
            __history.add("onStartAsync");
        }

        @Override
        public void onError(AsyncEvent event) throws IOException
        {
            __history.add("onError");
            String action=event.getSuppliedRequest().getParameter("error");
            if (action!=null)
            {
                __history.add(action);

                switch(action)
                {
                    case "dispatch":
                        event.getAsyncContext().dispatch();
                        break;

                    case "complete":
                        event.getSuppliedResponse().getOutputStream().println("COMPLETED\n");
                        event.getAsyncContext().complete();
                        break;
                }
            }
        }

        @Override
        public void onComplete(AsyncEvent event) throws IOException
        {
            __history.add("onComplete");
            __latch.countDown();
        }
    };

    class Log extends AbstractLifeCycle implements RequestLog
    {
        @Override
        public void log(Request request, Response response)
        {
            int status = response.getCommittedMetaData().getStatus();
            long written = response.getHttpChannel().getBytesWritten();
            _log.add(status+" "+written+" "+request.getRequestURI());
        }
    }
}
