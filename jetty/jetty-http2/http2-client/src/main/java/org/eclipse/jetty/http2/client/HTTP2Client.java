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

package org.eclipse.jetty.http2.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

import org.eclipse.jetty.alpn.client.ALPNClientConnectionFactory;
import org.eclipse.jetty.http2.ErrorCode;
import org.eclipse.jetty.http2.ISession;
import org.eclipse.jetty.http2.api.Session;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.io.ClientConnectionFactory;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.io.ManagedSelector;
import org.eclipse.jetty.io.MappedByteBufferPool;
import org.eclipse.jetty.io.SelectChannelEndPoint;
import org.eclipse.jetty.io.SelectorManager;
import org.eclipse.jetty.io.ssl.SslClientConnectionFactory;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.Promise;
import org.eclipse.jetty.util.component.ContainerLifeCycle;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;
import org.eclipse.jetty.util.thread.Scheduler;

/**
 * <p>{@link HTTP2Client} provides an asynchronous, non-blocking implementation
 * to send HTTP/2 frames to a server.</p>
 * <p>Typical usage:</p>
 * <pre>
 * // Create and start HTTP2Client.
 * HTTP2Client client = new HTTP2Client();
 * SslContextFactory sslContextFactory = new SslContextFactory();
 * client.addBean(sslContextFactory);
 * client.start();
 *
 * // Connect to host.
 * String host = "webtide.com";
 * int port = 443;
 *
 * FuturePromise&lt;Session&gt; sessionPromise = new FuturePromise&lt;&gt;();
 * client.connect(sslContextFactory, new InetSocketAddress(host, port), new ServerSessionListener.Adapter(), sessionPromise);
 *
 * // Obtain the client Session object.
 * Session session = sessionPromise.get(5, TimeUnit.SECONDS);
 *
 * // Prepare the HTTP request headers.
 * HttpFields requestFields = new HttpFields();
 * requestFields.put("User-Agent", client.getClass().getName() + "/" + Jetty.VERSION);
 * // Prepare the HTTP request object.
 * MetaData.Request request = new MetaData.Request("PUT", new HttpURI("https://" + host + ":" + port + "/"), HttpVersion.HTTP_2, requestFields);
 * // Create the HTTP/2 HEADERS frame representing the HTTP request.
 * HeadersFrame headersFrame = new HeadersFrame(0, request, null, false);
 *
 * // Prepare the listener to receive the HTTP response frames.
 * Stream.Listener responseListener = new new Stream.Listener.Adapter()
 * {
 *      &#64;Override
 *      public void onHeaders(Stream stream, HeadersFrame frame)
 *      {
 *          System.err.println(frame);
 *      }
 *
 *      &#64;Override
 *      public void onData(Stream stream, DataFrame frame, Callback callback)
 *      {
 *          System.err.println(frame);
 *          callback.succeeded();
 *      }
 * };
 *
 * // Send the HEADERS frame to create a stream.
 * FuturePromise&lt;Stream&gt; streamPromise = new FuturePromise&lt;&gt;();
 * session.newStream(headersFrame, streamPromise, responseListener);
 * Stream stream = streamPromise.get(5, TimeUnit.SECONDS);
 *
 * // Use the Stream object to send request content, if any, using a DATA frame.
 * ByteBuffer content = ...;
 * DataFrame requestContent = new DataFrame(stream.getId(), content, true);
 * stream.data(requestContent, Callback.Adapter.INSTANCE);
 *
 * // When done, stop the client.
 * client.stop();
 * </pre>
 */
public class HTTP2Client extends ContainerLifeCycle
{
    private Executor executor;
    private Scheduler scheduler;
    private ByteBufferPool bufferPool;
    private ClientConnectionFactory connectionFactory;
    private Queue<ISession> sessions;
    private SelectorManager selector;
    private int selectors = 1;
    private long idleTimeout = 30000;
    private long connectTimeout = 10000;
    private List<String> protocols = Arrays.asList("h2", "h2-17", "h2-16", "h2-15", "h2-14");

    @Override
    protected void doStart() throws Exception
    {
        if (executor == null)
            setExecutor(new QueuedThreadPool());

        if (scheduler == null)
            setScheduler(new ScheduledExecutorScheduler());

        if (bufferPool == null)
            setByteBufferPool(new MappedByteBufferPool());

        if (connectionFactory == null)
            setClientConnectionFactory(new HTTP2ClientConnectionFactory());

        if (sessions == null)
        {
            sessions = new ConcurrentLinkedQueue<>();
            addBean(sessions);
        }

        if (selector == null)
        {
            selector = newSelectorManager();
            addBean(selector);
        }
        selector.setConnectTimeout(getConnectTimeout());

        super.doStart();
    }

    protected SelectorManager newSelectorManager()
    {
        return new ClientSelectorManager(getExecutor(), getScheduler(), getSelectors());
    }

    @Override
    protected void doStop() throws Exception
    {
        closeConnections();
        super.doStop();
    }

    public Executor getExecutor()
    {
        return executor;
    }

    public void setExecutor(Executor executor)
    {
        this.updateBean(this.executor, executor);
        this.executor = executor;
    }

    public Scheduler getScheduler()
    {
        return scheduler;
    }

    public void setScheduler(Scheduler scheduler)
    {
        this.updateBean(this.scheduler, scheduler);
        this.scheduler = scheduler;
    }

    public ByteBufferPool getByteBufferPool()
    {
        return bufferPool;
    }

    public void setByteBufferPool(ByteBufferPool bufferPool)
    {
        this.updateBean(this.bufferPool, bufferPool);
        this.bufferPool = bufferPool;
    }

    public ClientConnectionFactory getClientConnectionFactory()
    {
        return connectionFactory;
    }

    public void setClientConnectionFactory(ClientConnectionFactory connectionFactory)
    {
        this.updateBean(this.connectionFactory, connectionFactory);
        this.connectionFactory = connectionFactory;
    }

    public int getSelectors()
    {
        return selectors;
    }

    public void setSelectors(int selectors)
    {
        this.selectors = selectors;
    }

    public long getIdleTimeout()
    {
        return idleTimeout;
    }

    public void setIdleTimeout(long idleTimeout)
    {
        this.idleTimeout = idleTimeout;
    }

    public long getConnectTimeout()
    {
        return connectTimeout;
    }

    public void setConnectTimeout(long connectTimeout)
    {
        this.connectTimeout = connectTimeout;
        SelectorManager selector = this.selector;
        if (selector != null)
            selector.setConnectTimeout(connectTimeout);
    }

    public List<String> getProtocols()
    {
        return protocols;
    }

    public void setProtocols(List<String> protocols)
    {
        this.protocols = protocols;
    }

    public void connect(InetSocketAddress address, Session.Listener listener, Promise<Session> promise)
    {
        connect(null, address, listener, promise);
    }

    public void connect(SslContextFactory sslContextFactory, InetSocketAddress address, Session.Listener listener, Promise<Session> promise)
    {
        connect(sslContextFactory, address, listener, promise, null);
    }

    public void connect(SslContextFactory sslContextFactory, InetSocketAddress address, Session.Listener listener, Promise<Session> promise, Map<String, Object> context)
    {
        try
        {
            SocketChannel channel = SocketChannel.open();
            configure(channel);
            channel.configureBlocking(false);
            context = contextFrom(sslContextFactory, address, listener, promise, context);
            if (channel.connect(address))
                selector.accept(channel, context);
            else
                selector.connect(channel, context);
        }
        catch (Throwable x)
        {
            promise.failed(x);
        }
    }

    public void accept(SslContextFactory sslContextFactory, SocketChannel channel, Session.Listener listener, Promise<Session> promise)
    {
        try
        {
            if (!channel.isConnected())
                throw new IllegalStateException("SocketChannel must be connected");
            channel.configureBlocking(false);
            Map<String, Object> context = contextFrom(sslContextFactory, (InetSocketAddress)channel.getRemoteAddress(), listener, promise, null);
            selector.accept(channel, context);
        }
        catch (Throwable x)
        {
            promise.failed(x);
        }
    }

    private Map<String, Object> contextFrom(SslContextFactory sslContextFactory, InetSocketAddress address, Session.Listener listener, Promise<Session> promise, Map<String, Object> context)
    {
        if (context == null)
            context = new HashMap<>();
        context.put(HTTP2ClientConnectionFactory.CLIENT_CONTEXT_KEY, this);
        context.put(HTTP2ClientConnectionFactory.SESSION_LISTENER_CONTEXT_KEY, listener);
        context.put(HTTP2ClientConnectionFactory.SESSION_PROMISE_CONTEXT_KEY, promise);
        if (sslContextFactory != null)
            context.put(SslClientConnectionFactory.SSL_CONTEXT_FACTORY_CONTEXT_KEY, sslContextFactory);
        context.put(SslClientConnectionFactory.SSL_PEER_HOST_CONTEXT_KEY, address.getHostString());
        context.put(SslClientConnectionFactory.SSL_PEER_PORT_CONTEXT_KEY, address.getPort());
        return context;
    }

    protected void configure(SocketChannel channel) throws IOException
    {
        channel.socket().setTcpNoDelay(true);
    }

    private void closeConnections()
    {
        for (ISession session : sessions)
            session.close(ErrorCode.NO_ERROR.code, null, Callback.NOOP);
        sessions.clear();
    }

    public boolean addSession(ISession session)
    {
        return sessions.offer(session);
    }

    public boolean removeSession(ISession session)
    {
        return sessions.remove(session);
    }

    private class ClientSelectorManager extends SelectorManager
    {
        private ClientSelectorManager(Executor executor, Scheduler scheduler, int selectors)
        {
            super(executor, scheduler, selectors);
        }

        @Override
        protected EndPoint newEndPoint(SocketChannel channel, ManagedSelector selector, SelectionKey selectionKey) throws IOException
        {
            return new SelectChannelEndPoint(channel, selector, selectionKey, getScheduler(), getIdleTimeout());
        }

        @Override
        public Connection newConnection(SocketChannel channel, EndPoint endpoint, Object attachment) throws IOException
        {
            @SuppressWarnings("unchecked")
            Map<String, Object> context = (Map<String, Object>)attachment;
            context.put(HTTP2ClientConnectionFactory.BYTE_BUFFER_POOL_CONTEXT_KEY, getByteBufferPool());
            context.put(HTTP2ClientConnectionFactory.EXECUTOR_CONTEXT_KEY, getExecutor());
            context.put(HTTP2ClientConnectionFactory.SCHEDULER_CONTEXT_KEY, getScheduler());

            ClientConnectionFactory factory = getClientConnectionFactory();

            SslContextFactory sslContextFactory = (SslContextFactory)context.get(SslClientConnectionFactory.SSL_CONTEXT_FACTORY_CONTEXT_KEY);
            if (sslContextFactory != null)
            {
                ALPNClientConnectionFactory alpn = new ALPNClientConnectionFactory(getExecutor(), factory, getProtocols());
                factory = new SslClientConnectionFactory(sslContextFactory, getByteBufferPool(), getExecutor(), alpn);
            }

            return factory.newConnection(endpoint, context);
        }

        @Override
        protected void connectionFailed(SocketChannel channel, Throwable failure, Object attachment)
        {
            @SuppressWarnings("unchecked")
            Map<String, Object> context = (Map<String, Object>)attachment;
            if (LOG.isDebugEnabled())
            {
                Object host = context.get(SslClientConnectionFactory.SSL_PEER_HOST_CONTEXT_KEY);
                Object port = context.get(SslClientConnectionFactory.SSL_PEER_PORT_CONTEXT_KEY);
                LOG.debug("Could not connect to {}:{}", host, port);
            }
            @SuppressWarnings("unchecked")
            Promise<Session> promise = (Promise<Session>)context.get(HTTP2ClientConnectionFactory.SESSION_PROMISE_CONTEXT_KEY);
            promise.failed(failure);
        }
    }
}
