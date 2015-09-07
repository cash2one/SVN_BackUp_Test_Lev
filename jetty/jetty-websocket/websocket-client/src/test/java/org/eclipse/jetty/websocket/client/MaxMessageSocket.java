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

package org.eclipse.jetty.websocket.client;

import static org.hamcrest.Matchers.is;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.toolchain.test.EventQueue;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.junit.Assert;

@WebSocket(maxTextMessageSize = 100*1024)
public class MaxMessageSocket
{
    private static final Logger LOG = Log.getLogger(MaxMessageSocket.class);
    private Session session;
    public CountDownLatch openLatch = new CountDownLatch(1);
    public CountDownLatch closeLatch = new CountDownLatch(1);
    public CountDownLatch dataLatch = new CountDownLatch(1);
    public EventQueue<String> messageQueue = new EventQueue<>();
    public EventQueue<Throwable> errorQueue = new EventQueue<>();
    public int closeCode = -1;
    public StringBuilder closeMessage = new StringBuilder();

    @OnWebSocketConnect
    public void onConnect(Session session)
    {
        this.session = session;
        openLatch.countDown();
    }
    
    @OnWebSocketClose
    public void onClose(int statusCode, String reason)
    {
        LOG.debug("onWebSocketClose({},{})",statusCode,reason);
        closeCode = statusCode;
        closeMessage.append(reason);
        closeLatch.countDown();
    }

    @OnWebSocketMessage
    public void onMessage(String message)
    {
        LOG.debug("onWebSocketText({})",message);
        messageQueue.offer(message);
        dataLatch.countDown();
    }
    
    @OnWebSocketError
    public void onError(Throwable cause)
    {
        LOG.debug("onWebSocketError",cause);
        Assert.assertThat("Error capture",errorQueue.offer(cause),is(true));
    }

    public Session getSession()
    {
        return this.session;
    }

    public void awaitConnect(int duration, TimeUnit unit) throws InterruptedException
    {
        Assert.assertThat("Client Socket connected",openLatch.await(duration,unit),is(true));
    }
    
    public void waitForMessage(int timeoutDuration, TimeUnit timeoutUnit) throws InterruptedException
    {
        LOG.debug("Waiting for message");
        Assert.assertThat("Message Received",dataLatch.await(timeoutDuration,timeoutUnit),is(true));
    }
    
    public void assertMessage(String expected)
    {
        String actual = messageQueue.poll();
        Assert.assertEquals("Message",expected,actual);
    }
}
