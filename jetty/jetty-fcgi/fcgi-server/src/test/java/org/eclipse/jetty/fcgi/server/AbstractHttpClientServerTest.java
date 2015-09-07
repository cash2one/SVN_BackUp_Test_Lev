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

package org.eclipse.jetty.fcgi.server;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.jetty.client.ConnectionPool;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpDestination;
import org.eclipse.jetty.client.LeakTrackingConnectionPool;
import org.eclipse.jetty.client.Origin;
import org.eclipse.jetty.fcgi.client.http.HttpClientTransportOverFCGI;
import org.eclipse.jetty.fcgi.client.http.HttpDestinationOverFCGI;
import org.eclipse.jetty.http.HttpScheme;
import org.eclipse.jetty.io.LeakTrackingByteBufferPool;
import org.eclipse.jetty.io.MappedByteBufferPool;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.toolchain.test.TestTracker;
import org.eclipse.jetty.util.LeakDetector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.junit.After;
import org.junit.Rule;

public abstract class AbstractHttpClientServerTest
{
    @Rule
    public final TestTracker tracker = new TestTracker();
    private LeakTrackingByteBufferPool serverBufferPool;
    private LeakTrackingByteBufferPool clientBufferPool;
    private final AtomicLong connectionLeaks = new AtomicLong();
    protected Server server;
    protected ServerConnector connector;
    protected HttpClient client;
    protected String scheme = HttpScheme.HTTP.asString();

    public void start(Handler handler) throws Exception
    {
        server = new Server();

        ServerFCGIConnectionFactory fcgiConnectionFactory = new ServerFCGIConnectionFactory(new HttpConfiguration());
        serverBufferPool = new LeakTrackingByteBufferPool(new MappedByteBufferPool.Tagged());
        connector = new ServerConnector(server, null, null, serverBufferPool,
                1, Math.max(1, Runtime.getRuntime().availableProcessors() / 2), fcgiConnectionFactory);
//        connector.setPort(9000);

        server.addConnector(connector);
        server.setHandler(handler);
        server.start();

        QueuedThreadPool executor = new QueuedThreadPool();
        executor.setName(executor.getName() + "-client");
        
        client = new HttpClient(new HttpClientTransportOverFCGI(1, false, "")
        {
            @Override
            public HttpDestination newHttpDestination(Origin origin)
            {
                return new HttpDestinationOverFCGI(client, origin)
                {
                    @Override
                    protected ConnectionPool newConnectionPool(HttpClient client)
                    {
                        return new LeakTrackingConnectionPool(this, client.getMaxConnectionsPerDestination(), this)
                        {
                            @Override
                            protected void leaked(LeakDetector.LeakInfo leakInfo)
                            {
                                connectionLeaks.incrementAndGet();
                            }
                        };
                    }
                };
            }
        }, null);
        client.setExecutor(executor);
        clientBufferPool = new LeakTrackingByteBufferPool(new MappedByteBufferPool.Tagged());
        client.setByteBufferPool(clientBufferPool);
        client.start();
    }

    @After
    public void dispose() throws Exception
    {
        System.gc();

        assertThat("Server BufferPool - leaked acquires", serverBufferPool.getLeakedAcquires(), is(0L));
        assertThat("Server BufferPool - leaked releases", serverBufferPool.getLeakedReleases(), is(0L));
        assertThat("Server BufferPool - unreleased", serverBufferPool.getLeakedResources(), is(0L));
        
        assertThat("Client BufferPool - leaked acquires", clientBufferPool.getLeakedAcquires(), is(0L));
        assertThat("Client BufferPool - leaked releases", clientBufferPool.getLeakedReleases(), is(0L));
        assertThat("Client BufferPool - unreleased", clientBufferPool.getLeakedResources(), is(0L));
        
        assertThat("Connection Leaks", connectionLeaks.get(), is(0L));

        if (client != null)
            client.stop();
        if (server != null)
            server.stop();
    }
}
