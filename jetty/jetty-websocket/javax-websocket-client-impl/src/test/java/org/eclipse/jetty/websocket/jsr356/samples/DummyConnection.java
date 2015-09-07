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

package org.eclipse.jetty.websocket.jsr356.samples;

import java.net.InetSocketAddress;
import java.util.concurrent.Executor;

import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.websocket.api.BatchMode;
import org.eclipse.jetty.websocket.api.SuspendToken;
import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.eclipse.jetty.websocket.api.WriteCallback;
import org.eclipse.jetty.websocket.api.extensions.Frame;
import org.eclipse.jetty.websocket.api.extensions.IncomingFrames;
import org.eclipse.jetty.websocket.common.LogicalConnection;
import org.eclipse.jetty.websocket.common.WebSocketSession;
import org.eclipse.jetty.websocket.common.io.IOState;

public class DummyConnection implements LogicalConnection
{
    private IOState iostate;

    public DummyConnection()
    {
        this.iostate = new IOState();
    }

    @Override
    public void close()
    {
    }

    @Override
    public void close(int statusCode, String reason)
    {
    }

    @Override
    public void disconnect()
    {
    }

    @Override
    public ByteBufferPool getBufferPool()
    {
        return null;
    }

    @Override
    public Executor getExecutor()
    {
        return null;
    }

    @Override
    public long getIdleTimeout()
    {
        return 0;
    }

    @Override
    public IOState getIOState()
    {
        return this.iostate;
    }

    @Override
    public InetSocketAddress getLocalAddress()
    {
        return null;
    }

    @Override
    public long getMaxIdleTimeout()
    {
        return 0;
    }

    @Override
    public WebSocketPolicy getPolicy()
    {
        return null;
    }

    @Override
    public InetSocketAddress getRemoteAddress()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WebSocketSession getSession()
    {
        return null;
    }

    @Override
    public boolean isOpen()
    {
        return false;
    }

    @Override
    public boolean isReading()
    {
        return false;
    }

    @Override
    public void outgoingFrame(Frame frame, WriteCallback callback, BatchMode batchMode)
    {
    }

    @Override
    public void resume()
    {
    }

    @Override
    public void setMaxIdleTimeout(long ms)
    {
    }

    @Override
    public void setNextIncomingFrames(IncomingFrames incoming)
    {
    }

    @Override
    public void setSession(WebSocketSession session)
    {
    }

    @Override
    public SuspendToken suspend()
    {
        return null;
    }
}
