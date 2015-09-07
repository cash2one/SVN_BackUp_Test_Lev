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

package org.eclipse.jetty.fcgi.parser;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jetty.fcgi.FCGI;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpParser;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class ResponseContentParser extends StreamContentParser
{
    private static final Logger LOG = Log.getLogger(ResponseContentParser.class);

    private final Map<Integer, ResponseParser> parsers = new ConcurrentHashMap<>();
    private final ClientParser.Listener listener;

    public ResponseContentParser(HeaderParser headerParser, ClientParser.Listener listener)
    {
        super(headerParser, FCGI.StreamType.STD_OUT, listener);
        this.listener = listener;
    }

    @Override
    public void noContent()
    {
        // Does nothing, since for responses the end of content is signaled via a FCGI_END_REQUEST frame
    }

    @Override
    protected boolean onContent(ByteBuffer buffer)
    {
        int request = getRequest();
        ResponseParser parser = parsers.get(request);
        if (parser == null)
        {
            parser = new ResponseParser(listener, request);
            parsers.put(request, parser);
        }
        return parser.parse(buffer);
    }

    @Override
    protected void end(int request)
    {
        super.end(request);
        parsers.remove(request);
    }

    private class ResponseParser implements HttpParser.ResponseHandler
    {
        private final HttpFields fields = new HttpFields();
        private ClientParser.Listener listener;
        private final int request;
        private final FCGIHttpParser httpParser;
        private State state = State.HEADERS;
        private boolean seenResponseCode;

        private ResponseParser(ClientParser.Listener listener, int request)
        {
            this.listener = listener;
            this.request = request;
            this.httpParser = new FCGIHttpParser(this);
        }

        public boolean parse(ByteBuffer buffer)
        {
            if (LOG.isDebugEnabled())
                LOG.debug("Response {} {} content {} {}", request, FCGI.StreamType.STD_OUT, state, buffer);

            int remaining = buffer.remaining();
            while (remaining > 0)
            {
                switch (state)
                {
                    case HEADERS:
                    {
                        if (httpParser.parseNext(buffer))
                            state = State.CONTENT_MODE;
                        remaining = buffer.remaining();
                        break;
                    }
                    case CONTENT_MODE:
                    {
                        // If we have no indication of the content, then
                        // the HTTP parser will assume there is no content
                        // and will not parse it even if it is provided,
                        // so we have to parse it raw ourselves here.
                        boolean rawContent = fields.size() == 0 ||
                                (fields.get(HttpHeader.CONTENT_LENGTH) == null &&
                                        fields.get(HttpHeader.TRANSFER_ENCODING) == null);
                        state = rawContent ? State.RAW_CONTENT : State.HTTP_CONTENT;
                        break;
                    }
                    case RAW_CONTENT:
                    {
                        if (notifyContent(buffer))
                            return true;
                        remaining = 0;
                        break;
                    }
                    case HTTP_CONTENT:
                    {
                        if (httpParser.parseNext(buffer))
                            return true;
                        remaining = buffer.remaining();
                        break;
                    }
                    default:
                    {
                        throw new IllegalStateException();
                    }
                }
            }
            return false;
        }

        @Override
        public int getHeaderCacheSize()
        {
            // TODO: configure this
            return 0;
        }

        @Override
        public boolean startResponse(HttpVersion version, int status, String reason)
        {
            // The HTTP request line does not exist in FCGI responses
            throw new IllegalStateException();
        }

        @Override
        public void parsedHeader(HttpField httpField)
        {
            try
            {
                String name = httpField.getName();
                if ("Status".equalsIgnoreCase(name))
                {
                    if (!seenResponseCode)
                    {
                        seenResponseCode = true;

                        // Need to set the response status so the
                        // HttpParser can handle the content properly.
                        String value = httpField.getValue();
                        String[] parts = value.split(" ");
                        String status = parts[0];
                        int code = Integer.parseInt(status);
                        httpParser.setResponseStatus(code);
                        String reason = parts.length > 1 ? value.substring(status.length()) : HttpStatus.getMessage(code);

                        notifyBegin(code, reason.trim());
                        notifyHeaders(fields);
                    }
                }
                else
                {
                    fields.add(httpField);
                    if (seenResponseCode)
                        notifyHeader(httpField);
                }
            }
            catch (Throwable x)
            {
                if (LOG.isDebugEnabled())
                    LOG.debug("Exception while invoking listener " + listener, x);
            }
        }

        private void notifyBegin(int code, String reason)
        {
            try
            {
                listener.onBegin(request, code, reason);
            }
            catch (Throwable x)
            {
                if (LOG.isDebugEnabled())
                    LOG.debug("Exception while invoking listener " + listener, x);
            }
        }

        private void notifyHeader(HttpField httpField)
        {
            try
            {
                listener.onHeader(request, httpField);
            }
            catch (Throwable x)
            {
                if (LOG.isDebugEnabled())
                    LOG.debug("Exception while invoking listener " + listener, x);
            }
        }

        private void notifyHeaders(HttpFields fields)
        {
            if (fields != null)
            {
                for (HttpField field : fields)
                    notifyHeader(field);
            }
        }

        private void notifyHeaders()
        {
            try
            {
                listener.onHeaders(request);
            }
            catch (Throwable x)
            {
                if (LOG.isDebugEnabled())
                    LOG.debug("Exception while invoking listener " + listener, x);
            }
        }

        @Override
        public boolean headerComplete()
        {
            if (!seenResponseCode)
            {
                // No Status header but we have other headers, assume 200 OK
                notifyBegin(200, "OK");
                notifyHeaders(fields);
            }
            notifyHeaders();
            // Return from parsing so that we can parse the content
            return true;
        }

        @Override
        public boolean content(ByteBuffer buffer)
        {
            return notifyContent(buffer);
        }

        private boolean notifyContent(ByteBuffer buffer)
        {
            try
            {
                return listener.onContent(request, FCGI.StreamType.STD_OUT, buffer);
            }
            catch (Throwable x)
            {
                if (LOG.isDebugEnabled())
                    LOG.debug("Exception while invoking listener " + listener, x);
                return false;
            }
        }

        @Override
        public boolean messageComplete()
        {
            // Return from parsing so that we can parse the next headers or the raw content.
            // No need to notify the listener because it will be done by FCGI_END_REQUEST.
            return true;
        }

        @Override
        public void earlyEOF()
        {
            // TODO
        }

        @Override
        public void badMessage(int status, String reason)
        {
            // TODO
        }
    }

    // Methods overridden to make them visible here
    private static class FCGIHttpParser extends HttpParser
    {
        private FCGIHttpParser(ResponseHandler handler)
        {
            super(handler, 65 * 1024, true);
            reset();
        }

        @Override
        public void reset()
        {
            super.reset();
            setResponseStatus(200);
            setState(State.HEADER);
        }

        @Override
        protected void setResponseStatus(int status)
        {
            super.setResponseStatus(status);
        }
    }

    private enum State
    {
        HEADERS, CONTENT_MODE, RAW_CONTENT, HTTP_CONTENT
    }
}
