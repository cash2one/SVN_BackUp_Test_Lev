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

package org.eclipse.jetty.http2.server;

import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.Executor;

import org.eclipse.jetty.http.BadMessageException;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.MetaData;
import org.eclipse.jetty.http.MetaData.Request;
import org.eclipse.jetty.http2.HTTP2Connection;
import org.eclipse.jetty.http2.ISession;
import org.eclipse.jetty.http2.IStream;
import org.eclipse.jetty.http2.api.server.ServerSessionListener;
import org.eclipse.jetty.http2.frames.DataFrame;
import org.eclipse.jetty.http2.frames.HeadersFrame;
import org.eclipse.jetty.http2.frames.SettingsFrame;
import org.eclipse.jetty.http2.parser.ServerParser;
import org.eclipse.jetty.http2.parser.SettingsBodyParser;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.util.B64Code;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.ConcurrentArrayQueue;
import org.eclipse.jetty.util.TypeUtil;

public class HTTP2ServerConnection extends HTTP2Connection implements Connection.UpgradeTo
{
    private final Queue<HttpChannelOverHTTP2> channels = new ConcurrentArrayQueue<>();
    private final ServerSessionListener listener;
    private final HttpConfiguration httpConfig;
    private HeadersFrame upgradeRequest;

    public HTTP2ServerConnection(ByteBufferPool byteBufferPool, Executor executor, EndPoint endPoint, HttpConfiguration httpConfig, ServerParser parser, ISession session, int inputBufferSize, ServerSessionListener listener)
    {
        super(byteBufferPool, executor, endPoint, parser, session, inputBufferSize);
        this.listener = listener;
        this.httpConfig = httpConfig;
    }

    @Override
    protected ServerParser getParser()
    {
        return (ServerParser)super.getParser();
    }

    @Override
    public void onUpgradeTo(ByteBuffer buffer)
    {
        if (LOG.isDebugEnabled())
            LOG.debug("HTTP2 onUpgradeTo {} {}", this, BufferUtil.toDetailString(buffer));
        setInputBuffer(buffer);
    }

    @Override
    public void onOpen()
    {
        super.onOpen();
        notifyAccept(getSession());
        if (upgradeRequest != null)
            getSession().onFrame(upgradeRequest);
    }

    private void notifyAccept(ISession session)
    {
        try
        {
            listener.onAccept(session);
        }
        catch (Throwable x)
        {
            LOG.info("Failure while notifying listener " + listener, x);
        }
    }

    public void onNewStream(Connector connector, IStream stream, HeadersFrame frame)
    {
        if (LOG.isDebugEnabled())
            LOG.debug("Processing {} on {}", frame, stream);
        HttpChannelOverHTTP2 channel = provideHttpChannel(connector, stream);
        Runnable task = channel.onRequest(frame);
        if (task != null)
            offerTask(task, false);
    }

    public void onData(IStream stream, DataFrame frame, Callback callback)
    {
        if (LOG.isDebugEnabled())
            LOG.debug("Processing {} on {}", frame, stream);
        HttpChannelOverHTTP2 channel = (HttpChannelOverHTTP2)stream.getAttribute(IStream.CHANNEL_ATTRIBUTE);
        Runnable task = channel.requestContent(frame, callback);
        if (task != null)
            offerTask(task, false);
    }

    public void push(Connector connector, IStream stream, MetaData.Request request)
    {
        if (LOG.isDebugEnabled())
            LOG.debug("Processing push {} on {}", request, stream);
        HttpChannelOverHTTP2 channel = provideHttpChannel(connector, stream);
        Runnable task = channel.onPushRequest(request);
        if (task != null)
            offerTask(task, true);
    }

    private HttpChannelOverHTTP2 provideHttpChannel(Connector connector, IStream stream)
    {
        HttpChannelOverHTTP2 channel = channels.poll();
        if (channel != null)
        {
            channel.getHttpTransport().setStream(stream);
            if (LOG.isDebugEnabled())
                LOG.debug("Recycling channel {} for {}", channel, this);
        }
        else
        {
            HttpTransportOverHTTP2 transport = new HttpTransportOverHTTP2(connector, this);
            transport.setStream(stream);
            channel = new ServerHttpChannelOverHTTP2(connector, httpConfig, getEndPoint(), transport);
            if (LOG.isDebugEnabled())
                LOG.debug("Creating channel {} for {}", channel, this);
        }
        stream.setAttribute(IStream.CHANNEL_ATTRIBUTE, channel);
        return channel;
    }

    public boolean upgrade(Request request)
    {
        if (HttpMethod.PRI.is(request.getMethod()))
        {
            getParser().directUpgrade();
        }
        else
        {
            HttpField settingsField = request.getFields().getField(HttpHeader.HTTP2_SETTINGS);
            if (settingsField == null)
                throw new BadMessageException("Missing " + HttpHeader.HTTP2_SETTINGS + " header");
            String value = settingsField.getValue();
            final byte[] settings = B64Code.decodeRFC4648URL(value == null ? "" : value);

            if (LOG.isDebugEnabled())
                LOG.debug("{} settings {}",this,TypeUtil.toHexString(settings));

            SettingsFrame settingsFrame = SettingsBodyParser.parseBody(BufferUtil.toBuffer(settings));
            if (settingsFrame == null)
            {
                LOG.warn("Invalid {} header value: {}", HttpHeader.HTTP2_SETTINGS, value);
                throw new BadMessageException();
            }

            getSession().onFrame(settingsFrame);

            // Remember the request to send a response from onOpen().
            upgradeRequest = new HeadersFrame(1, request, null, true);
        }
        return true;
    }

    private class ServerHttpChannelOverHTTP2 extends HttpChannelOverHTTP2
    {
        public ServerHttpChannelOverHTTP2(Connector connector, HttpConfiguration configuration, EndPoint endPoint, HttpTransportOverHTTP2 transport)
        {
            super(connector, configuration, endPoint, transport);
        }

        @Override
        public void onCompleted()
        {
            super.onCompleted();
            recycle();
            channels.offer(this);
        }
    }
}
