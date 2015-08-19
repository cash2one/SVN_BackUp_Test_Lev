/*
 * Copyright (c) 2002-2015 Gargoyle Software Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gargoylesoftware.htmlunit.javascript.host;

import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.CHROME;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.FF;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.IE;

import java.net.URI;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.javascript.JavaScriptEngine;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxClass;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxConstant;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxConstructor;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxFunction;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxGetter;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxSetter;
import com.gargoylesoftware.htmlunit.javascript.configuration.WebBrowser;
import com.gargoylesoftware.htmlunit.javascript.host.event.EventTarget;
import com.gargoylesoftware.htmlunit.javascript.host.event.MessageEvent;

import net.sourceforge.htmlunit.corejs.javascript.Context;
import net.sourceforge.htmlunit.corejs.javascript.Function;
import net.sourceforge.htmlunit.corejs.javascript.Scriptable;

/**
 * A JavaScript object for a WebSocket.
 *
 * @version $Revision: 10780 $
 * @author Ahmed Ashour
 *
 * @see <a href="https://developer.mozilla.org/en/WebSockets/WebSockets_reference/WebSocket">Mozilla documentation</a>
 */
@JsxClass(browsers = { @WebBrowser(CHROME), @WebBrowser(FF), @WebBrowser(value = IE, minVersion = 11) })
public class WebSocket extends EventTarget {

    private static final Log LOG = LogFactory.getLog(WebSocket.class);

    /** The connection has not yet been established. */
    @JsxConstant
    public static final int CONNECTING = 0;
    /** The WebSocket connection is established and communication is possible. */
    @JsxConstant
    public static final int OPEN = 1;
    /** The connection is going through the closing handshake. */
    @JsxConstant
    public static final int CLOSING = 2;
    /** The connection has been closed or could not be opened. */
    @JsxConstant
    public static final int CLOSED = 3;

    private Function closeHandler_;
    private Function errorHandler_;
    private Function messageHandler_;
    private Function openHandler_;
    private int readyState_ = CLOSED;

    private HtmlPage containingPage_;
    private Session incomingSession_;
    private Session outgoingSession_;

    /**
     * Creates a new instance. JavaScript objects must have a default constructor.
     */
    public WebSocket() {
    }

    /**
     * Creates a new instance.
     * @param url the URL to which to connect
     * @param protocols if present, is either a string or an array of strings
     * @param window the top level window
     */
    private WebSocket(final String url, final Object protocols, final Window window) {
        try {
            final WebSocketClient client = new WebSocketClient();
            client.start();
            incomingSession_ = client.connect(new WebSocketImpl(), new URI(url)).get();
            containingPage_ = (HtmlPage) window.getWebWindow().getEnclosedPage();
            readyState_ = OPEN;
        }
        catch (final Exception e) {
            LOG.error(e);
        }
    }

    /**
     * JavaScript constructor.
     * @param cx the current context
     * @param args the arguments to the WebSocket constructor
     * @param ctorObj the function object
     * @param inNewExpr Is new or not
     * @return the java object to allow JavaScript to access
     */
    @JsxConstructor
    public static Scriptable jsConstructor(
            final Context cx, final Object[] args, final Function ctorObj,
            final boolean inNewExpr) {
        if (args.length < 1 || args.length > 2) {
            throw Context.reportRuntimeError(
                    "WebSocket Error: constructor must have one or two String parameters.");
        }
        if (args[0] == Context.getUndefinedValue()) {
            throw Context.reportRuntimeError("WebSocket Error: 'url' parameter is undefined.");
        }
        if (!(args[0] instanceof String)) {
            throw Context.reportRuntimeError("WebSocket Error: 'url' parameter must be a String.");
        }
        return new WebSocket((String) args[0], null, getWindow(ctorObj));
    }

    /**
     * Returns the event handler that fires on close.
     * @return the event handler that fires on close
     */
    @JsxGetter
    public Function getOnclose() {
        return closeHandler_;
    }

    /**
     * Sets the event handler that fires on close.
     * @param closeHandler the event handler that fires on close
     */
    @JsxSetter
    public void setOnclose(final Function closeHandler) {
        closeHandler_ = closeHandler;
    }

    /**
     * Returns the event handler that fires on error.
     * @return the event handler that fires on error
     */
    @JsxGetter
    public Function getOnerror() {
        return errorHandler_;
    }

    /**
     * Sets the event handler that fires on error.
     * @param errorHandler the event handler that fires on error
     */
    @JsxSetter
    public void setOnerror(final Function errorHandler) {
        errorHandler_ = errorHandler;
    }

    /**
     * Returns the event handler that fires on message.
     * @return the event handler that fires on message
     */
    @JsxGetter
    public Function getOnmessage() {
        return messageHandler_;
    }

    /**
     * Sets the event handler that fires on message.
     * @param messageHandler the event handler that fires on message
     */
    @JsxSetter
    public void setOnmessage(final Function messageHandler) {
        messageHandler_ = messageHandler;
    }

    /**
     * Returns the event handler that fires on open.
     * @return the event handler that fires on open
     */
    @JsxGetter
    public Function getOnopen() {
        return openHandler_;
    }

    /**
     * Sets the event handler that fires on open.
     * @param openHandler the event handler that fires on open
     */
    @JsxSetter
    public void setOnopen(final Function openHandler) {
        openHandler_ = openHandler;
        fireOnOpen();
    }

    /**
     * Returns The current state of the connection. The possible values are: {@link #CONNECTING}, {@link #OPEN},
     * {@link #CLOSING} or {@link #CLOSED}.
     * @return the current state of the connection
     */
    @JsxGetter
    public int getReadyState() {
        return readyState_;
    }

    /**
     * Closes the WebSocket connection or connection attempt, if any.
     * If the connection is already {@link #CLOSED}, this method does nothing.
     * @param code A numeric value indicating the status code explaining why the connection is being closed
     * @param reason A human-readable string explaining why the connection is closing
     */
    @JsxFunction
    public void close(final Object code, final Object reason) {
        if (readyState_ != CLOSED) {
            if (incomingSession_ != null) {
                incomingSession_.close();
            }
            if (outgoingSession_ != null) {
                outgoingSession_.close();
            }
            readyState_ = CLOSED;
        }
    }

    /**
     * Transmits data to the server over the WebSocket connection.
     * @param content the body of the message being sent with the request
     */
    @JsxFunction
    public void send(final Object content) {
        try {
            if (content instanceof String) {
                outgoingSession_.getRemote().sendString(content.toString());
            }
            else {
                throw new IllegalStateException(
                        "Not Yet Implemented: WebSocket.send() was used to send non-string value");
            }
        }
        catch (final Exception e) {
            LOG.error(e);
        }
    }

    private void fireOnOpen() {
        if (openHandler_ == null) {
            return;
        }
        final Scriptable scope = openHandler_.getParentScope();
        final JavaScriptEngine jsEngine = containingPage_.getWebClient().getJavaScriptEngine();
        jsEngine.callFunction(containingPage_, openHandler_, scope, WebSocket.this,
                ArrayUtils.EMPTY_OBJECT_ARRAY);
    }

    private class WebSocketImpl extends WebSocketAdapter {
        @Override
        public void onWebSocketConnect(final Session session) {
            outgoingSession_ = session;
        }

        @Override
        public void onWebSocketClose(final int closeCode, final String message) {
            if (closeHandler_ == null) {
                return;
            }
            final Scriptable scope = closeHandler_.getParentScope();
            final JavaScriptEngine jsEngine = containingPage_.getWebClient().getJavaScriptEngine();
            jsEngine.callFunction(containingPage_, closeHandler_, scope, WebSocket.this,
                    new Object[] {closeCode, message});
        }

        @Override
        public void onWebSocketText(final String data) {
            if (messageHandler_ == null) {
                return;
            }
            final Scriptable scope = messageHandler_.getParentScope();
            final JavaScriptEngine jsEngine = containingPage_.getWebClient().getJavaScriptEngine();
            final MessageEvent event = new MessageEvent(data);
            event.setParentScope(getParentScope());
            event.setPrototype(getPrototype(event.getClass()));
            jsEngine.callFunction(containingPage_, messageHandler_, scope, WebSocket.this, new Object[] {event});
        }

        @Override
        public void onWebSocketBinary(final byte[] data, final int offset, final int length) {
            if (messageHandler_ == null) {
                return;
            }
            final Scriptable scope = messageHandler_.getParentScope();
            final JavaScriptEngine jsEngine = containingPage_.getWebClient().getJavaScriptEngine();
            jsEngine.callFunction(containingPage_, messageHandler_, scope, WebSocket.this,
                    new Object[] {data, offset, length});
        }
    }
}
