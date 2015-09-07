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

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jetty.http.HostPortHttpField;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpScheme;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http.MetaData;
import org.eclipse.jetty.http2.ErrorCode;
import org.eclipse.jetty.http2.FlowControlStrategy;
import org.eclipse.jetty.http2.HTTP2Session;
import org.eclipse.jetty.http2.HTTP2Stream;
import org.eclipse.jetty.http2.ISession;
import org.eclipse.jetty.http2.api.Session;
import org.eclipse.jetty.http2.api.Stream;
import org.eclipse.jetty.http2.api.server.ServerSessionListener;
import org.eclipse.jetty.http2.frames.DataFrame;
import org.eclipse.jetty.http2.frames.GoAwayFrame;
import org.eclipse.jetty.http2.frames.HeadersFrame;
import org.eclipse.jetty.http2.frames.ResetFrame;
import org.eclipse.jetty.http2.frames.SettingsFrame;
import org.eclipse.jetty.http2.server.RawHTTP2ServerConnectionFactory;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.toolchain.test.TestTracker;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.FutureCallback;
import org.eclipse.jetty.util.FuturePromise;
import org.eclipse.jetty.util.Promise;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

public abstract class FlowControlStrategyTest
{
    @Rule
    public TestTracker tracker = new TestTracker();
    protected ServerConnector connector;
    protected HTTP2Client client;
    protected Server server;

    protected abstract FlowControlStrategy newFlowControlStrategy();

    protected void start(ServerSessionListener listener) throws Exception
    {
        QueuedThreadPool serverExecutor = new QueuedThreadPool();
        serverExecutor.setName("server");
        server = new Server(serverExecutor);
        connector = new ServerConnector(server, new RawHTTP2ServerConnectionFactory(new HttpConfiguration(), listener)
        {
            @Override
            protected FlowControlStrategy newFlowControlStrategy()
            {
                return FlowControlStrategyTest.this.newFlowControlStrategy();
            }
        });
        server.addConnector(connector);
        server.start();

        client = new HTTP2Client();
        QueuedThreadPool clientExecutor = new QueuedThreadPool();
        clientExecutor.setName("client");
        client.setExecutor(clientExecutor);
        client.setClientConnectionFactory(new HTTP2ClientConnectionFactory()
        {
            @Override
            protected FlowControlStrategy newFlowControlStrategy()
            {
                return FlowControlStrategyTest.this.newFlowControlStrategy();
            }
        });
        client.start();
    }

    protected Session newClient(Session.Listener listener) throws Exception
    {
        String host = "localhost";
        int port = connector.getLocalPort();
        InetSocketAddress address = new InetSocketAddress(host, port);
        FuturePromise<Session> promise = new FuturePromise<>();
        client.connect(address, listener, promise);
        return promise.get(5, TimeUnit.SECONDS);
    }

    protected MetaData.Request newRequest(String method, HttpFields fields)
    {
        String host = "localhost";
        int port = connector.getLocalPort();
        String authority = host + ":" + port;
        return new MetaData.Request(method, HttpScheme.HTTP, new HostPortHttpField(authority), "/", HttpVersion.HTTP_2, fields);
    }

    @After
    public void dispose() throws Exception
    {
        // Allow WINDOW_UPDATE frames to be sent/received to avoid exception stack traces.
        Thread.sleep(1000);
        client.stop();
        server.stop();
    }

    @Test
    public void testWindowSizeUpdates() throws Exception
    {
        final CountDownLatch prefaceLatch = new CountDownLatch(1);
        final CountDownLatch stream1Latch = new CountDownLatch(1);
        final CountDownLatch stream2Latch = new CountDownLatch(1);
        final CountDownLatch settingsLatch = new CountDownLatch(1);
        start(new ServerSessionListener.Adapter()
        {
            @Override
            public Map<Integer, Integer> onPreface(Session session)
            {
                HTTP2Session serverSession = (HTTP2Session)session;
                Assert.assertEquals(FlowControlStrategy.DEFAULT_WINDOW_SIZE, serverSession.getSendWindow());
                Assert.assertEquals(FlowControlStrategy.DEFAULT_WINDOW_SIZE, serverSession.getRecvWindow());
                prefaceLatch.countDown();
                return null;
            }

            @Override
            public void onSettings(Session session, SettingsFrame frame)
            {
                for (Stream stream : session.getStreams())
                {
                    HTTP2Stream serverStream = (HTTP2Stream)stream;
                    Assert.assertEquals(0, serverStream.getSendWindow());
                    Assert.assertEquals(FlowControlStrategy.DEFAULT_WINDOW_SIZE, serverStream.getRecvWindow());
                }
                settingsLatch.countDown();
            }

            @Override
            public Stream.Listener onNewStream(Stream stream, HeadersFrame frame)
            {
                HTTP2Stream serverStream = (HTTP2Stream)stream;
                MetaData.Request request = (MetaData.Request)frame.getMetaData();
                if ("GET".equalsIgnoreCase(request.getMethod()))
                {
                    Assert.assertEquals(FlowControlStrategy.DEFAULT_WINDOW_SIZE, serverStream.getSendWindow());
                    Assert.assertEquals(FlowControlStrategy.DEFAULT_WINDOW_SIZE, serverStream.getRecvWindow());
                    stream1Latch.countDown();
                }
                else
                {
                    Assert.assertEquals(0, serverStream.getSendWindow());
                    Assert.assertEquals(FlowControlStrategy.DEFAULT_WINDOW_SIZE, serverStream.getRecvWindow());
                    stream2Latch.countDown();
                }
                return null;
            }
        });

        HTTP2Session clientSession = (HTTP2Session)newClient(new Session.Listener.Adapter());

        Assert.assertEquals(FlowControlStrategy.DEFAULT_WINDOW_SIZE, clientSession.getSendWindow());
        Assert.assertEquals(FlowControlStrategy.DEFAULT_WINDOW_SIZE, clientSession.getRecvWindow());
        Assert.assertTrue(prefaceLatch.await(5, TimeUnit.SECONDS));

        MetaData.Request request1 = newRequest("GET", new HttpFields());
        FuturePromise<Stream> promise1 = new FuturePromise<>();
        clientSession.newStream(new HeadersFrame(0, request1, null, true), promise1, new Stream.Listener.Adapter());
        HTTP2Stream clientStream1 = (HTTP2Stream)promise1.get(5, TimeUnit.SECONDS);

        Assert.assertEquals(FlowControlStrategy.DEFAULT_WINDOW_SIZE, clientStream1.getSendWindow());
        Assert.assertEquals(FlowControlStrategy.DEFAULT_WINDOW_SIZE, clientStream1.getRecvWindow());
        Assert.assertTrue(stream1Latch.await(5, TimeUnit.SECONDS));

        // Send a SETTINGS frame that changes the window size.
        // This tells the server that its stream send window must be updated,
        // so on the client it's the receive window that must be updated.
        Map<Integer, Integer> settings = new HashMap<>();
        settings.put(SettingsFrame.INITIAL_WINDOW_SIZE, 0);
        SettingsFrame frame = new SettingsFrame(settings, false);
        FutureCallback callback = new FutureCallback();
        clientSession.settings(frame, callback);
        callback.get(5, TimeUnit.SECONDS);

        Assert.assertEquals(FlowControlStrategy.DEFAULT_WINDOW_SIZE, clientStream1.getSendWindow());
        Assert.assertEquals(0, clientStream1.getRecvWindow());
        settingsLatch.await(5, TimeUnit.SECONDS);

        // Now create a new stream, it must pick up the new value.
        MetaData.Request request2 = newRequest("POST", new HttpFields());
        FuturePromise<Stream> promise2 = new FuturePromise<>();
        clientSession.newStream(new HeadersFrame(0, request2, null, true), promise2, new Stream.Listener.Adapter());
        HTTP2Stream clientStream2 = (HTTP2Stream)promise2.get(5, TimeUnit.SECONDS);

        Assert.assertEquals(FlowControlStrategy.DEFAULT_WINDOW_SIZE, clientStream2.getSendWindow());
        Assert.assertEquals(0, clientStream2.getRecvWindow());
        Assert.assertTrue(stream2Latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void testFlowControlWithConcurrentSettings() throws Exception
    {
        // Initial window is 64 KiB. We allow the client to send 1024 B
        // then we change the window to 512 B. At this point, the client
        // must stop sending data (although the initial window allows it).

        final int size = 512;
        // We get 3 data frames: the first of 1024 and 2 of 512 each
        // after the flow control window has been reduced.
        final CountDownLatch dataLatch = new CountDownLatch(3);
        final AtomicReference<Callback> callbackRef = new AtomicReference<>();
        start(new ServerSessionListener.Adapter()
        {
            @Override
            public Stream.Listener onNewStream(Stream stream, HeadersFrame requestFrame)
            {
                HttpFields fields = new HttpFields();
                MetaData.Response response = new MetaData.Response(HttpVersion.HTTP_2, 200, fields);
                HeadersFrame responseFrame = new HeadersFrame(stream.getId(), response, null, true);
                stream.headers(responseFrame, Callback.NOOP);

                return new Stream.Listener.Adapter()
                {
                    private final AtomicInteger dataFrames = new AtomicInteger();

                    @Override
                    public void onData(Stream stream, DataFrame frame, Callback callback)
                    {
                        dataLatch.countDown();
                        int dataFrameCount = dataFrames.incrementAndGet();
                        if (dataFrameCount == 1)
                        {
                            callbackRef.set(callback);
                            Map<Integer, Integer> settings = new HashMap<>();
                            settings.put(SettingsFrame.INITIAL_WINDOW_SIZE, size);
                            stream.getSession().settings(new SettingsFrame(settings, false), Callback.NOOP);
                            // Do not succeed the callback here.
                        }
                        else if (dataFrameCount > 1)
                        {
                            // Consume the data.
                            callback.succeeded();
                        }
                    }
                };
            }
        });

        // Two SETTINGS frames, the initial one and the one we send from the server.
        final CountDownLatch settingsLatch = new CountDownLatch(2);
        Session session = newClient(new Session.Listener.Adapter()
        {
            @Override
            public void onSettings(Session session, SettingsFrame frame)
            {
                settingsLatch.countDown();
            }
        });

        MetaData.Request request = newRequest("POST", new HttpFields());
        FuturePromise<Stream> promise = new FuturePromise<>();
        session.newStream(new HeadersFrame(0, request, null, false), promise, new Stream.Listener.Adapter());
        Stream stream = promise.get(5, TimeUnit.SECONDS);

        // Send first chunk that exceeds the window.
        stream.data(new DataFrame(stream.getId(), ByteBuffer.allocate(size * 2), false), Callback.NOOP);
        settingsLatch.await(5, TimeUnit.SECONDS);

        // Send the second chunk of data, must not arrive since we're flow control stalled on the client.
        stream.data(new DataFrame(stream.getId(), ByteBuffer.allocate(size * 2), true), Callback.NOOP);
        Assert.assertFalse(dataLatch.await(1, TimeUnit.SECONDS));

        // Consume the data arrived to server, this will resume flow control on the client.
        callbackRef.get().succeeded();

        Assert.assertTrue(dataLatch.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void testServerFlowControlOneBigWrite() throws Exception
    {
        final int windowSize = 1536;
        final int length = 5 * windowSize;
        final CountDownLatch settingsLatch = new CountDownLatch(1);
        start(new ServerSessionListener.Adapter()
        {
            @Override
            public void onSettings(Session session, SettingsFrame frame)
            {
                settingsLatch.countDown();
            }

            @Override
            public Stream.Listener onNewStream(Stream stream, HeadersFrame requestFrame)
            {
                MetaData.Response metaData = new MetaData.Response(HttpVersion.HTTP_2, 200, new HttpFields());
                HeadersFrame responseFrame = new HeadersFrame(stream.getId(), metaData, null, false);
                stream.headers(responseFrame, Callback.NOOP);

                DataFrame dataFrame = new DataFrame(stream.getId(), ByteBuffer.allocate(length), true);
                stream.data(dataFrame, Callback.NOOP);
                return null;
            }
        });

        Session session = newClient(new Session.Listener.Adapter());

        Map<Integer, Integer> settings = new HashMap<>();
        settings.put(SettingsFrame.INITIAL_WINDOW_SIZE, windowSize);
        session.settings(new SettingsFrame(settings, false), Callback.NOOP);

        Assert.assertTrue(settingsLatch.await(5, TimeUnit.SECONDS));

        final CountDownLatch dataLatch = new CountDownLatch(1);
        final Exchanger<Callback> exchanger = new Exchanger<>();
        MetaData.Request metaData = newRequest("GET", new HttpFields());
        HeadersFrame requestFrame = new HeadersFrame(0, metaData, null, true);
        session.newStream(requestFrame, new Promise.Adapter<Stream>(), new Stream.Listener.Adapter()
        {
            private AtomicInteger dataFrames = new AtomicInteger();

            @Override
            public void onData(Stream stream, DataFrame frame, Callback callback)
            {
                try
                {
                    int dataFrames = this.dataFrames.incrementAndGet();
                    if (dataFrames == 1 || dataFrames == 2)
                    {
                        // Do not consume the data frame.
                        // We should then be flow-control stalled.
                        exchanger.exchange(callback);
                    }
                    else if (dataFrames == 3 || dataFrames == 4 || dataFrames == 5)
                    {
                        // Consume totally.
                        callback.succeeded();
                        if (frame.isEndStream())
                            dataLatch.countDown();
                    }
                    else
                    {
                        Assert.fail();
                    }
                }
                catch (InterruptedException x)
                {
                    callback.failed(x);
                }
            }
        });

        Callback callback = exchanger.exchange(null, 5, TimeUnit.SECONDS);
        checkThatWeAreFlowControlStalled(exchanger);

        // Consume the first chunk.
        callback.succeeded();

        callback = exchanger.exchange(null, 5, TimeUnit.SECONDS);
        checkThatWeAreFlowControlStalled(exchanger);

        // Consume the second chunk.
        callback.succeeded();

        Assert.assertTrue(dataLatch.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void testClientFlowControlOneBigWrite() throws Exception
    {
        final int windowSize = 1536;
        final Exchanger<Callback> exchanger = new Exchanger<>();
        final CountDownLatch settingsLatch = new CountDownLatch(1);
        final CountDownLatch dataLatch = new CountDownLatch(1);
        start(new ServerSessionListener.Adapter()
        {
            @Override
            public Map<Integer, Integer> onPreface(Session session)
            {
                Map<Integer, Integer> settings = new HashMap<>();
                settings.put(SettingsFrame.INITIAL_WINDOW_SIZE, windowSize);
                return settings;
            }

            @Override
            public Stream.Listener onNewStream(Stream stream, HeadersFrame requestFrame)
            {
                MetaData.Response metaData = new MetaData.Response(HttpVersion.HTTP_2, 200, new HttpFields());
                HeadersFrame responseFrame = new HeadersFrame(stream.getId(), metaData, null, false);
                stream.headers(responseFrame, Callback.NOOP);
                return new Stream.Listener.Adapter()
                {
                    private AtomicInteger dataFrames = new AtomicInteger();

                    @Override
                    public void onData(Stream stream, DataFrame frame, Callback callback)
                    {
                        try
                        {
                            int dataFrames = this.dataFrames.incrementAndGet();
                            if (dataFrames == 1 || dataFrames == 2)
                            {
                                // Do not consume the data frame.
                                // We should then be flow-control stalled.
                                exchanger.exchange(callback);
                            }
                            else if (dataFrames == 3 || dataFrames == 4 || dataFrames == 5)
                            {
                                // Consume totally.
                                callback.succeeded();
                                if (frame.isEndStream())
                                    dataLatch.countDown();
                            }
                            else
                            {
                                Assert.fail();
                            }
                        }
                        catch (InterruptedException x)
                        {
                            callback.failed(x);
                        }
                    }
                };
            }
        });

        Session session = newClient(new Session.Listener.Adapter()
        {
            @Override
            public void onSettings(Session session, SettingsFrame frame)
            {
                settingsLatch.countDown();
            }
        });

        Assert.assertTrue(settingsLatch.await(5, TimeUnit.SECONDS));

        MetaData.Request metaData = newRequest("GET", new HttpFields());
        HeadersFrame requestFrame = new HeadersFrame(0, metaData, null, false);
        FuturePromise<Stream> streamPromise = new FuturePromise<>();
        session.newStream(requestFrame, streamPromise, null);
        Stream stream = streamPromise.get(5, TimeUnit.SECONDS);

        final int length = 5 * windowSize;
        DataFrame dataFrame = new DataFrame(stream.getId(), ByteBuffer.allocate(length), true);
        stream.data(dataFrame, Callback.NOOP);

        Callback callback = exchanger.exchange(null, 5, TimeUnit.SECONDS);
        checkThatWeAreFlowControlStalled(exchanger);

        // Consume the first chunk.
        callback.succeeded();

        callback = exchanger.exchange(null, 5, TimeUnit.SECONDS);
        checkThatWeAreFlowControlStalled(exchanger);

        // Consume the second chunk.
        callback.succeeded();

        Assert.assertTrue(dataLatch.await(5, TimeUnit.SECONDS));
    }

    private void checkThatWeAreFlowControlStalled(Exchanger<Callback> exchanger) throws Exception
    {
        try
        {
            exchanger.exchange(null, 1, TimeUnit.SECONDS);
            Assert.fail();
        }
        catch (TimeoutException x)
        {
            // Expected.
        }
    }

    @Test
    public void testSessionStalledStallsNewStreams() throws Exception
    {
        final int windowSize = 1024;
        start(new ServerSessionListener.Adapter()
        {
            @Override
            public Stream.Listener onNewStream(Stream stream, HeadersFrame requestFrame)
            {
                MetaData.Request request = (MetaData.Request)requestFrame.getMetaData();
                if ("POST".equalsIgnoreCase(request.getMethod()))
                {
                    // Send data to consume most of the session window.
                    ByteBuffer data = ByteBuffer.allocate(FlowControlStrategy.DEFAULT_WINDOW_SIZE - windowSize);
                    DataFrame dataFrame = new DataFrame(stream.getId(), data, true);
                    stream.data(dataFrame, Callback.NOOP);
                    return null;
                }
                else
                {
                    // For every stream, send down half the window size of data.
                    MetaData.Response metaData = new MetaData.Response(HttpVersion.HTTP_2, 200, new HttpFields());
                    HeadersFrame responseFrame = new HeadersFrame(stream.getId(), metaData, null, false);
                    stream.headers(responseFrame, Callback.NOOP);
                    DataFrame dataFrame = new DataFrame(stream.getId(), ByteBuffer.allocate(windowSize / 2), true);
                    stream.data(dataFrame, Callback.NOOP);
                    return null;
                }
            }
        });

        Session session = newClient(new Session.Listener.Adapter());

        // First request is just to consume most of the session window.
        final List<Callback> callbacks1 = new ArrayList<>();
        final CountDownLatch prepareLatch = new CountDownLatch(1);
        MetaData.Request request1 = newRequest("POST", new HttpFields());
        session.newStream(new HeadersFrame(0, request1, null, true), new Promise.Adapter<Stream>(), new Stream.Listener.Adapter()
        {
            @Override
            public void onData(Stream stream, DataFrame frame, Callback callback)
            {
                // Do not consume the data to reduce the session window.
                callbacks1.add(callback);
                if (frame.isEndStream())
                    prepareLatch.countDown();
            }
        });
        Assert.assertTrue(prepareLatch.await(5, TimeUnit.SECONDS));

        // Second request will consume half of the remaining the session window.
        MetaData.Request request2 = newRequest("GET", new HttpFields());
        session.newStream(new HeadersFrame(0, request2, null, true), new Promise.Adapter<Stream>(), new Stream.Listener.Adapter()
        {
            @Override
            public void onData(Stream stream, DataFrame frame, Callback callback)
            {
                // Do not consume it to stall flow control.
            }
        });

        // Third request will consume the whole session window, which is now stalled.
        // A fourth request will not be able to receive data.
        MetaData.Request request3 = newRequest("GET", new HttpFields());
        session.newStream(new HeadersFrame(0, request3, null, true), new Promise.Adapter<Stream>(), new Stream.Listener.Adapter()
        {
            @Override
            public void onData(Stream stream, DataFrame frame, Callback callback)
            {
                // Do not consume it to stall flow control.
            }
        });

        // Fourth request is now stalled.
        final CountDownLatch latch = new CountDownLatch(1);
        MetaData.Request request4 = newRequest("GET", new HttpFields());
        session.newStream(new HeadersFrame(0, request4, null, true), new Promise.Adapter<Stream>(), new Stream.Listener.Adapter()
        {
            @Override
            public void onData(Stream stream, DataFrame frame, Callback callback)
            {
                callback.succeeded();
                if (frame.isEndStream())
                    latch.countDown();
            }
        });

        // Verify that the data does not arrive because the server session is stalled.
        Assert.assertFalse(latch.await(1, TimeUnit.SECONDS));

        // Consume the data of the first response.
        // This will open up the session window, allowing the fourth stream to send data.
        for (Callback callback : callbacks1)
            callback.succeeded();

        Assert.assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void testServerSendsBigContent() throws Exception
    {
        final byte[] data = new byte[1024 * 1024];
        new Random().nextBytes(data);

        start(new ServerSessionListener.Adapter()
        {
            @Override
            public Stream.Listener onNewStream(Stream stream, HeadersFrame requestFrame)
            {
                MetaData.Response metaData = new MetaData.Response(HttpVersion.HTTP_2, 200, new HttpFields());
                HeadersFrame responseFrame = new HeadersFrame(stream.getId(), metaData, null, false);
                stream.headers(responseFrame, Callback.NOOP);
                DataFrame dataFrame = new DataFrame(stream.getId(), ByteBuffer.wrap(data), true);
                stream.data(dataFrame, Callback.NOOP);
                return null;
            }
        });

        Session session = newClient(new Session.Listener.Adapter());
        MetaData.Request metaData = newRequest("GET", new HttpFields());
        HeadersFrame requestFrame = new HeadersFrame(0, metaData, null, true);
        final byte[] bytes = new byte[data.length];
        final CountDownLatch latch = new CountDownLatch(1);
        session.newStream(requestFrame, new Promise.Adapter<Stream>(), new Stream.Listener.Adapter()
        {
            private int received;

            @Override
            public void onData(Stream stream, DataFrame frame, Callback callback)
            {
                int remaining = frame.remaining();
                frame.getData().get(bytes, received, remaining);
                this.received += remaining;
                callback.succeeded();
                if (frame.isEndStream())
                    latch.countDown();
            }
        });

        Assert.assertTrue(latch.await(15, TimeUnit.SECONDS));
        Assert.assertArrayEquals(data, bytes);
    }

    @Test
    public void testServerTwoDataFramesWithStalledStream() throws Exception
    {
        // Frames in queue = DATA1, DATA2.
        // Server writes part of DATA1, then stalls.
        // A window update unstalls the session, verify that the data is correctly sent.

        Random random = new Random();
        final byte[] chunk1 = new byte[1024];
        random.nextBytes(chunk1);
        final byte[] chunk2 = new byte[2048];
        random.nextBytes(chunk2);

        // Two SETTINGS frames: the initial after the preface,
        // and the explicit where we set the stream window size to zero.
        final AtomicReference<CountDownLatch> settingsLatch = new AtomicReference<>(new CountDownLatch(2));
        final CountDownLatch dataLatch = new CountDownLatch(1);
        start(new ServerSessionListener.Adapter()
        {
            @Override
            public void onSettings(Session session, SettingsFrame frame)
            {
                settingsLatch.get().countDown();
            }

            @Override
            public Stream.Listener onNewStream(Stream stream, HeadersFrame frame)
            {
                stream.data(new DataFrame(stream.getId(), ByteBuffer.wrap(chunk1), false), Callback.NOOP);
                stream.data(new DataFrame(stream.getId(), ByteBuffer.wrap(chunk2), true), Callback.NOOP);
                dataLatch.countDown();
                return null;
            }
        });

        Session session = newClient(new Session.Listener.Adapter());
        Map<Integer, Integer> settings = new HashMap<>();
        settings.put(SettingsFrame.INITIAL_WINDOW_SIZE, 0);
        session.settings(new SettingsFrame(settings, false), Callback.NOOP);
        Assert.assertTrue(settingsLatch.get().await(5, TimeUnit.SECONDS));

        byte[] content = new byte[chunk1.length + chunk2.length];
        final ByteBuffer buffer = ByteBuffer.wrap(content);
        MetaData.Request metaData = newRequest("GET", new HttpFields());
        HeadersFrame requestFrame = new HeadersFrame(0, metaData, null, true);
        final CountDownLatch responseLatch = new CountDownLatch(1);
        session.newStream(requestFrame, new Promise.Adapter<Stream>(), new Stream.Listener.Adapter()
        {
            @Override
            public void onData(Stream stream, DataFrame frame, Callback callback)
            {
                buffer.put(frame.getData());
                callback.succeeded();
                if (frame.isEndStream())
                    responseLatch.countDown();
            }
        });
        Assert.assertTrue(dataLatch.await(5, TimeUnit.SECONDS));

        // Now we have the 2 DATA frames queued in the server.

        // Unstall the stream window.
        settingsLatch.set(new CountDownLatch(1));
        settings.clear();
        settings.put(SettingsFrame.INITIAL_WINDOW_SIZE, chunk1.length / 2);
        session.settings(new SettingsFrame(settings, false), Callback.NOOP);
        Assert.assertTrue(settingsLatch.get().await(5, TimeUnit.SECONDS));

        Assert.assertTrue(responseLatch.await(5, TimeUnit.SECONDS));

        // Check that the data is sent correctly.
        byte[] expected = new byte[content.length];
        System.arraycopy(chunk1, 0, expected, 0, chunk1.length);
        System.arraycopy(chunk2, 0, expected, chunk1.length, chunk2.length);
        Assert.assertArrayEquals(expected, content);
    }

    @Test
    public void testClientSendingInitialSmallWindow() throws Exception
    {
        start(new ServerSessionListener.Adapter()
        {
            @Override
            public Stream.Listener onNewStream(Stream stream, HeadersFrame frame)
            {
                MetaData metaData = new MetaData.Response(HttpVersion.HTTP_2, 200, new HttpFields());
                HeadersFrame responseFrame = new HeadersFrame(stream.getId(), metaData, null, false);
                stream.headers(responseFrame, Callback.NOOP);
                return new Stream.Listener.Adapter()
                {
                    @Override
                    public void onData(Stream stream, DataFrame frame, Callback callback)
                    {
                        // Since we echo back the data
                        // asynchronously we must copy it.
                        ByteBuffer data = frame.getData();
                        ByteBuffer copy = ByteBuffer.allocateDirect(data.remaining());
                        copy.put(data).flip();
                        stream.data(new DataFrame(stream.getId(), copy, frame.isEndStream()), callback);
                    }
                };
            }
        });

        final int initialWindow = 16;
        Session session = newClient(new Session.Listener.Adapter()
        {
            @Override
            public Map<Integer, Integer> onPreface(Session session)
            {
                Map<Integer, Integer> settings = new HashMap<>();
                settings.put(SettingsFrame.INITIAL_WINDOW_SIZE, initialWindow);
                return settings;
            }
        });

        byte[] requestData = new byte[initialWindow * 4];
        new Random().nextBytes(requestData);

        byte[] responseData = new byte[requestData.length];
        final ByteBuffer responseContent = ByteBuffer.wrap(responseData);
        MetaData.Request metaData = newRequest("GET", new HttpFields());
        HeadersFrame requestFrame = new HeadersFrame(0, metaData, null, false);
        FuturePromise<Stream> streamPromise = new FuturePromise<>();
        final CountDownLatch latch = new CountDownLatch(1);
        session.newStream(requestFrame, streamPromise, new Stream.Listener.Adapter()
        {
            @Override
            public void onData(Stream stream, DataFrame frame, Callback callback)
            {
                responseContent.put(frame.getData());
                callback.succeeded();
                if (frame.isEndStream())
                    latch.countDown();
            }
        });
        Stream stream = streamPromise.get(5, TimeUnit.SECONDS);

        ByteBuffer requestContent = ByteBuffer.wrap(requestData);
        DataFrame dataFrame = new DataFrame(stream.getId(), requestContent, true);
        stream.data(dataFrame, Callback.NOOP);

        Assert.assertTrue(latch.await(5, TimeUnit.SECONDS));

        responseContent.flip();
        Assert.assertArrayEquals(requestData, responseData);
    }

    @Test
    public void testClientExceedingSessionWindow() throws Exception
    {
        // On server, we don't consume the data.
        start(new ServerSessionListener.Adapter());

        final CountDownLatch closeLatch = new CountDownLatch(1);
        Session session = newClient(new Session.Listener.Adapter()
        {
            @Override
            public void onClose(Session session, GoAwayFrame frame)
            {
                if (frame.getError() == ErrorCode.FLOW_CONTROL_ERROR.code)
                    closeLatch.countDown();
            }
        });

        // Consume the whole session and stream window.
        MetaData.Request metaData = newRequest("POST", new HttpFields());
        HeadersFrame requestFrame = new HeadersFrame(0, metaData, null, false);
        FuturePromise<Stream> streamPromise = new FuturePromise<>();
        session.newStream(requestFrame, streamPromise, new Stream.Listener.Adapter());
        Stream stream = streamPromise.get(5, TimeUnit.SECONDS);
        ByteBuffer data = ByteBuffer.allocate(FlowControlStrategy.DEFAULT_WINDOW_SIZE);
        final CountDownLatch dataLatch = new CountDownLatch(1);
        stream.data(new DataFrame(stream.getId(), data, false), new Callback.NonBlocking()
        {
            @Override
            public void succeeded()
            {
                dataLatch.countDown();
            }
        });
        Assert.assertTrue(dataLatch.await(5, TimeUnit.SECONDS));

        // The following "sneaky" write may clash with the write
        // of the reply SETTINGS frame sent by the client in
        // response to the server SETTINGS frame.
        // It is not enough to use a latch on the server to
        // wait for the reply frame, since the client may have
        // sent the bytes, but not yet be ready to write again.
        Thread.sleep(1000);

        // Now the client is supposed to not send more frames.
        // If it does, the connection must be closed.
        HTTP2Session http2Session = (HTTP2Session)session;
        ByteBufferPool.Lease lease = new ByteBufferPool.Lease(connector.getByteBufferPool());
        ByteBuffer extraData = ByteBuffer.allocate(1024);
        http2Session.getGenerator().data(lease, new DataFrame(stream.getId(), extraData, true), extraData.remaining());
        List<ByteBuffer> buffers = lease.getByteBuffers();
        http2Session.getEndPoint().write(Callback.NOOP, buffers.toArray(new ByteBuffer[buffers.size()]));

        // Expect the connection to be closed.
        Assert.assertTrue(closeLatch.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void testClientExceedingStreamWindow() throws Exception
    {
        // On server, we don't consume the data.
        start(new ServerSessionListener.Adapter()
        {
            @Override
            public Map<Integer, Integer> onPreface(Session session)
            {
                // Enlarge the session window.
                ((ISession)session).updateRecvWindow(FlowControlStrategy.DEFAULT_WINDOW_SIZE);
                return super.onPreface(session);
            }
        });

        final CountDownLatch closeLatch = new CountDownLatch(1);
        Session session = newClient(new Session.Listener.Adapter()
        {
            @Override
            public void onClose(Session session, GoAwayFrame frame)
            {
                if (frame.getError() == ErrorCode.FLOW_CONTROL_ERROR.code)
                    closeLatch.countDown();
            }
        });

        // Consume the whole stream window.
        MetaData.Request metaData = newRequest("POST", new HttpFields());
        HeadersFrame requestFrame = new HeadersFrame(0, metaData, null, false);
        FuturePromise<Stream> streamPromise = new FuturePromise<>();
        session.newStream(requestFrame, streamPromise, new Stream.Listener.Adapter());
        Stream stream = streamPromise.get(5, TimeUnit.SECONDS);
        ByteBuffer data = ByteBuffer.allocate(FlowControlStrategy.DEFAULT_WINDOW_SIZE);
        final CountDownLatch dataLatch = new CountDownLatch(1);
        stream.data(new DataFrame(stream.getId(), data, false), new Callback.NonBlocking()
        {
            @Override
            public void succeeded()
            {
                dataLatch.countDown();
            }
        });
        Assert.assertTrue(dataLatch.await(5, TimeUnit.SECONDS));

        // Wait for a while before doing the "sneaky" write
        // below, see comments in the previous test case.
        Thread.sleep(1000);

        // Now the client is supposed to not send more frames.
        // If it does, the connection must be closed.
        HTTP2Session http2Session = (HTTP2Session)session;
        ByteBufferPool.Lease lease = new ByteBufferPool.Lease(connector.getByteBufferPool());
        ByteBuffer extraData = ByteBuffer.allocate(1024);
        http2Session.getGenerator().data(lease, new DataFrame(stream.getId(), extraData, true), extraData.remaining());
        List<ByteBuffer> buffers = lease.getByteBuffers();
        http2Session.getEndPoint().write(Callback.NOOP, buffers.toArray(new ByteBuffer[buffers.size()]));

        // Expect the connection to be closed.
        Assert.assertTrue(closeLatch.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void testFlowControlWhenServerResetsStream() throws Exception
    {
        // On server, don't consume the data and immediately reset.
        start(new ServerSessionListener.Adapter()
        {
            @Override
            public Stream.Listener onNewStream(Stream stream, HeadersFrame frame)
            {
                MetaData.Request request = (MetaData.Request)frame.getMetaData();

                if (HttpMethod.GET.is(request.getMethod()))
                    return new Stream.Listener.Adapter();

                return new Stream.Listener.Adapter()
                {
                    @Override
                    public void onData(Stream stream, DataFrame frame, Callback callback)
                    {
                        // Fail the callback to enlarge the session window.
                        // More data frames will be discarded because the
                        // stream is reset, and automatically consumed to
                        // keep the session window large for other streams.
                        callback.failed(new Throwable());
                        stream.reset(new ResetFrame(stream.getId(), ErrorCode.CANCEL_STREAM_ERROR.code), Callback.NOOP);
                    }
                };
            }
        });

        Session session = newClient(new Session.Listener.Adapter());
        MetaData.Request metaData = newRequest("POST", new HttpFields());
        HeadersFrame frame = new HeadersFrame(0, metaData, null, false);
        FuturePromise<Stream> streamPromise = new FuturePromise<>();
        final CountDownLatch resetLatch = new CountDownLatch(1);
        session.newStream(frame, streamPromise, new Stream.Listener.Adapter()
        {
            @Override
            public void onReset(Stream stream, ResetFrame frame)
            {
                resetLatch.countDown();
            }
        });
        Stream stream = streamPromise.get(5, TimeUnit.SECONDS);

        // Perform a big upload that will stall the flow control windows.
        ByteBuffer data = ByteBuffer.allocate(5 * FlowControlStrategy.DEFAULT_WINDOW_SIZE);
        final CountDownLatch dataLatch = new CountDownLatch(1);
        stream.data(new DataFrame(stream.getId(), data, true), new Callback.NonBlocking()
        {
            @Override
            public void failed(Throwable x)
            {
                dataLatch.countDown();
            }
        });

        Assert.assertTrue(resetLatch.await(5, TimeUnit.SECONDS));
        Assert.assertTrue(dataLatch.await(5, TimeUnit.SECONDS));
    }
}
