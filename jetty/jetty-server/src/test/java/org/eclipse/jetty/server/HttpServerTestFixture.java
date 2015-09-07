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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.toolchain.test.PropertyFlag;
import org.eclipse.jetty.util.IO;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.junit.After;
import org.junit.Before;

public class HttpServerTestFixture
{    // Useful constants
    protected static final long PAUSE=10L;
    protected static final int LOOPS= PropertyFlag.isEnabled("test.stress")?250:50;

    protected QueuedThreadPool _threadPool;
    protected Server _server;
    protected URI _serverURI;
    protected HttpConfiguration _httpConfiguration;
    protected ServerConnector _connector;
    protected String _scheme="http";

    protected Socket newSocket(String host,int port) throws Exception
    {
        Socket socket = new Socket(host,port);
        socket.setSoTimeout(10000);
        socket.setTcpNoDelay(true);
        socket.setSoLinger(false,0);
        return socket;
    }

    @Before
    public void before()
    {
        _threadPool = new QueuedThreadPool();
        _server = new Server(_threadPool);
    }

    protected void startServer(ServerConnector connector) throws Exception
    {
        startServer(connector,new HandlerWrapper());
    }
    
    protected void startServer(ServerConnector connector, Handler handler) throws Exception
    {
        _connector = connector;
        _httpConfiguration=_connector.getConnectionFactory(HttpConnectionFactory.class).getHttpConfiguration();
        _httpConfiguration.setBlockingTimeout(-1);
        _httpConfiguration.setSendDateHeader(false);
        _server.addConnector(_connector);
        _server.setHandler(handler);
        _server.start();
        _serverURI = _server.getURI();
    }

    @After
    public void stopServer() throws Exception
    {
        _server.stop();
        _server.join();
        _server.setConnectors(new Connector[]{});
    }

    protected void configureServer(Handler handler) throws Exception
    {
        HandlerWrapper current = (HandlerWrapper)_server.getHandler();
        current.stop();
        current.setHandler(handler);
        current.start();
    }


    protected static class EchoHandler extends AbstractHandler
    {
        boolean _musthavecontent=true;

        public EchoHandler()
        {}

        public EchoHandler(boolean content)
        {
            _musthavecontent=false;
        }

        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
        {
            baseRequest.setHandled(true);

            if (request.getContentType()!=null)
                response.setContentType(request.getContentType());
            if (request.getParameter("charset")!=null)
                response.setCharacterEncoding(request.getParameter("charset"));
            else if (request.getCharacterEncoding()!=null)
                response.setCharacterEncoding(request.getCharacterEncoding());

            PrintWriter writer=response.getWriter();

            int count=0;
            BufferedReader reader=request.getReader();

            if (request.getContentLength()!=0)
            {
                String line=reader.readLine();
                while (line!=null)
                {
                    writer.print(line);
                    writer.print("\n");
                    count+=line.length();
                    line=reader.readLine();
                }
            }

            if (count==0)
            {
                if (_musthavecontent)
                    throw new IllegalStateException("no input recieved");

                writer.println("No content");
            }

            // just to be difficult
            reader.close();
            writer.close();

            if (reader.read()>=0)
                throw new IllegalStateException("Not closed");
        }
    }

    protected static class OptionsHandler extends AbstractHandler
    {
        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
        {
            baseRequest.setHandled(true);
            if (request.getMethod().equals("OPTIONS"))
                response.setStatus(200);
            else
                response.setStatus(500);

            response.setHeader("Allow", "GET");
        }
    }
    
    protected static class HelloWorldHandler extends AbstractHandler
    {
        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
        {
            baseRequest.setHandled(true);
            response.setStatus(200);
            response.getOutputStream().print("Hello world\r\n");
        }
    }

    protected static class DataHandler extends AbstractHandler
    {
        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
        {
            baseRequest.setHandled(true);
            response.setStatus(200);

            InputStream in = request.getInputStream();
            String input= IO.toString(in);

            String tmp = request.getParameter("writes");
            int writes=Integer.parseInt(tmp==null?"10":tmp);
            tmp = request.getParameter("block");
            int block=Integer.parseInt(tmp==null?"10":tmp);
            String encoding=request.getParameter("encoding");
            String chars=request.getParameter("chars");

            String data = "\u0a870123456789A\u0a87CDEFGHIJKLMNOPQRSTUVWXYZ\u0250bcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
            while (data.length()<block)
                data+=data;

            String chunk = (input+data).substring(0,block);
            response.setContentType("text/plain");
            if (encoding==null)
            {
                byte[] bytes=chunk.getBytes(StandardCharsets.ISO_8859_1);
                OutputStream out=response.getOutputStream();
                for (int i=0;i<writes;i++)
                {
                    out.write(bytes);
                }
            }
            else if ("true".equals(chars))
            {
                response.setCharacterEncoding(encoding);
                PrintWriter out=response.getWriter();
                char[] c=chunk.toCharArray();
                for (int i=0;i<writes;i++)
                {
                    out.write(c);
                    if (out.checkError())
                        break;
                }
            }
            else
            {
                response.setCharacterEncoding(encoding);
                PrintWriter out=response.getWriter();
                for (int i=0;i<writes;i++)
                {
                    out.write(chunk);
                    if (out.checkError())
                        break;
                }
            }

        }
    }


    public final static HostnameVerifier __hostnameverifier = new HostnameVerifier()
    {
        public boolean verify(String hostname, SSLSession session)
        {
            return true;
        }
    };
}
