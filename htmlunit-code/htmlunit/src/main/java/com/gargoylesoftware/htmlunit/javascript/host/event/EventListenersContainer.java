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
package com.gargoylesoftware.htmlunit.javascript.host.event;

import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.EVENT_FALSE_RESULT;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_EVENT_HANDLER_UNDEFINED_AS_NULL;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gargoylesoftware.htmlunit.InteractivePage;
import com.gargoylesoftware.htmlunit.ScriptResult;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.HtmlBody;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import net.sourceforge.htmlunit.corejs.javascript.Function;
import net.sourceforge.htmlunit.corejs.javascript.NativeObject;
import net.sourceforge.htmlunit.corejs.javascript.Scriptable;
import net.sourceforge.htmlunit.corejs.javascript.ScriptableObject;
import net.sourceforge.htmlunit.corejs.javascript.Undefined;

/**
 * Container for event listener.
 * @version $Revision: 10969 $
 * @author Marc Guillemot
 * @author Daniel Gredler
 * @author Ahmed Ashour
 * @author Frank Danek
 */
public class EventListenersContainer implements Serializable {

    private static final Log LOG = LogFactory.getLog(EventListenersContainer.class);

    static class Handlers implements Serializable {
        private final List<Scriptable> capturingHandlers_ = new ArrayList<>();
        private final List<Scriptable> bubblingHandlers_ = new ArrayList<>();
        private Object handler_;
        List<Scriptable> getHandlers(final boolean useCapture) {
            if (useCapture) {
                return capturingHandlers_;
            }
            return bubblingHandlers_;
        }
        @Override
        protected Handlers clone() {
            final Handlers clone = new Handlers();
            clone.handler_ = handler_;
            clone.capturingHandlers_.addAll(capturingHandlers_);
            clone.bubblingHandlers_.addAll(bubblingHandlers_);
            return clone;
        }
    }

    private final Map<String, Handlers> eventHandlers_ = new HashMap<>();
    private final EventTarget jsNode_;

    /**
     * The constructor.
     *
     * @param jsNode the node.
     */
    public EventListenersContainer(final EventTarget jsNode) {
        jsNode_ = jsNode;
    }

    /**
     * Adds an event listener.
     * @param type the event type to listen for (like "load")
     * @param listener the event listener
     * @param useCapture If {@code true}, indicates that the user wishes to initiate capture (not yet implemented)
     * @return {@code true} if the listener has been added
     */
    public boolean addEventListener(final String type, final Scriptable listener, final boolean useCapture) {
        if (null == listener) {
            return true;
        }

        final List<Scriptable> listeners = getHandlersOrCreateIt(type).getHandlers(useCapture);
        if (listeners.contains(listener)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(type + " listener already registered, skipping it (" + listener + ")");
            }
            return false;
        }
        listeners.add(listener);
        return true;
    }

    private Handlers getHandlersOrCreateIt(final String type) {
        final String typeLC = type.toLowerCase(Locale.ENGLISH);
        Handlers handlers = eventHandlers_.get(typeLC);
        if (handlers == null) {
            handlers = new Handlers();
            eventHandlers_.put(typeLC, handlers);
        }
        return handlers;
    }

    /**
     * Returns the relevant handlers.
     * @param eventType the event type
     * @param useCapture whether to use capture of not
     * @return the handlers list
     */
    public List<Scriptable> getHandlers(final String eventType, final boolean useCapture) {
        final Handlers handlers = eventHandlers_.get(eventType.toLowerCase(Locale.ENGLISH));
        if (handlers != null) {
            return handlers.getHandlers(useCapture);
        }
        return null;
    }

    /**
     * Removes event listener.
     * @param type the type
     * @param listener the listener
     * @param useCapture to use capture or not
     */
    public void removeEventListener(final String type, final Scriptable listener, final boolean useCapture) {
        final List<Scriptable> handlers = getHandlers(type, useCapture);
        if (handlers != null) {
            handlers.remove(listener);
        }
    }

    /**
     * Sets the handler property (with an handler or something else).
     * @param eventName the event name (like "click")
     * @param value the new property
     */
    public void setEventHandlerProp(final String eventName, final Object value) {
        Object handler = value;
        if (jsNode_.getWindow().getBrowserVersion()
            .hasFeature(JS_EVENT_HANDLER_UNDEFINED_AS_NULL)
            && Undefined.instance == value) {
            handler = null;
        }

        final Handlers handlers = getHandlersOrCreateIt(eventName);
        handlers.handler_ = handler;
    }

    /**
     * Returns event handler property.
     * @param eventName event name
     * @return the handler, or null if not found
     */
    public Object getEventHandlerProp(final String eventName) {
        final Handlers handlers = eventHandlers_.get(eventName);
        if (handlers == null) {
            return null;
        }
        return handlers.handler_;
    }

    private ScriptResult executeEventListeners(final boolean useCapture, final Event event, final Object[] args) {
        final DomNode node = jsNode_.getDomNodeOrNull();
        // some event don't apply on all kind of nodes, for instance "blur"
        if (!event.applies(node)) {
            return null;
        }
        ScriptResult allResult = null;
        final List<Scriptable> handlers = getHandlers(event.getType(), useCapture);
        if (handlers != null && !handlers.isEmpty()) {
            event.setCurrentTarget(jsNode_);
            final HtmlPage page = (HtmlPage) node.getPage();
            // make a copy of the list as execution of an handler may (de-)register handlers
            final List<Scriptable> handlersToExecute = new ArrayList<>(handlers);
            for (final Scriptable listener : handlersToExecute) {
                Function function = null;
                Scriptable thisObject = null;
                if (listener instanceof Function) {
                    function = (Function) listener;
                    thisObject = jsNode_;
                }
                else if (listener instanceof NativeObject) {
                    final Object handleEvent = ScriptableObject.getProperty(listener, "handleEvent");
                    if (handleEvent instanceof Function) {
                        function = (Function) handleEvent;
                        thisObject = listener;
                    }
                }
                if (function != null) {
                    final ScriptResult result =
                            page.executeJavaScriptFunctionIfPossible(function, thisObject, args, node);
                    if (event.isPropagationStopped()) {
                        allResult = result;
                    }
                    if (jsNode_.getBrowserVersion().hasFeature(EVENT_FALSE_RESULT)) {
                        if (ScriptResult.isFalse(result)) {
                            allResult = result;
                        }
                        else {
                            final Object eventReturnValue = event.getReturnValue();
                            if (eventReturnValue instanceof Boolean && !((Boolean) eventReturnValue).booleanValue()) {
                                allResult = new ScriptResult(Boolean.FALSE, page);
                            }
                        }
                    }
                }
            }
        }
        return allResult;
    }

    private ScriptResult executeEventHandler(final Event event, final Object[] propHandlerArgs) {
        final DomNode node = jsNode_.getDomNodeOrNull();
        // some event don't apply on all kind of nodes, for instance "blur"
        if (!event.applies(node)) {
            return null;
        }
        final Function handler = getEventHandler(event.getType());
        if (handler != null) {
            event.setCurrentTarget(jsNode_);
            final InteractivePage page = (InteractivePage) (node != null
                    ? node.getPage()
                    : jsNode_.getWindow().getWebWindow().getEnclosedPage());
            if (LOG.isDebugEnabled()) {
                LOG.debug("Executing " + event.getType() + " handler for " + node);
            }
            return page.executeJavaScriptFunctionIfPossible(handler, jsNode_,
                    propHandlerArgs, page);
        }
        return null;
    }

    /**
     * Executes bubbling listeners.
     * @param event the event
     * @param args arguments
     * @param propHandlerArgs handler arguments
     * @return the result
     */
    public ScriptResult executeBubblingListeners(final Event event, final Object[] args,
            final Object[] propHandlerArgs) {
        ScriptResult result = null;

        // the handler declared as property if any (not on body, as handler declared on body goes to the window)
        final DomNode domNode = jsNode_.getDomNodeOrNull();
        if (!(domNode instanceof HtmlBody)) {
            result = executeEventHandler(event, propHandlerArgs);
            if (event.isPropagationStopped()) {
                return result;
            }
        }

        // the registered listeners (if any)
        final ScriptResult newResult = executeEventListeners(false, event, args);
        if (newResult != null) {
            result = newResult;
        }
        return result;
    }

    /**
     * Executes capturing listeners.
     * @param event the event
     * @param args the arguments
     * @return the result
     */
    public ScriptResult executeCapturingListeners(final Event event, final Object[] args) {
        return executeEventListeners(true, event, args);
    }

    /**
     * Gets an event handler.
     * @param eventName the event name (e.g. "click")
     * @return the handler function, {@code null} if the property is null or not a function
     */
    public Function getEventHandler(final String eventName) {
        final Object handler = getEventHandlerProp(eventName.toLowerCase(Locale.ENGLISH));
        if (handler instanceof Function) {
            return (Function) handler;
        }
        return null;
    }

    /**
     * Returns <tt>true</tt> if there are any event handlers for the specified event.
     * @param eventName the event name (e.g. "click")
     * @return <tt>true</tt> if there are any event handlers for the specified event, <tt>false</tt> otherwise
     */
    public boolean hasEventHandlers(final String eventName) {
        final Handlers h = eventHandlers_.get(eventName);
        return h != null
            && (h.handler_ instanceof Function || !h.bubblingHandlers_.isEmpty() || !h.capturingHandlers_.isEmpty());
    }

    /**
     * Executes listeners.
     * @param event the event
     * @param args the arguments
     * @param propHandlerArgs handler arguments
     * @return the result
     */
    public ScriptResult executeListeners(final Event event, final Object[] args, final Object[] propHandlerArgs) {
        // the registered capturing listeners (if any)
        event.setEventPhase(Event.CAPTURING_PHASE);
        ScriptResult result = executeEventListeners(true, event, args);
        if (event.isPropagationStopped()) {
            return result;
        }

        // the handler declared as property (if any)
        event.setEventPhase(Event.AT_TARGET);
        ScriptResult newResult = executeEventHandler(event, propHandlerArgs);
        if (newResult != null) {
            result = newResult;
        }
        if (event.isPropagationStopped()) {
            return result;
        }

        // the registered bubbling listeners (if any)
        event.setEventPhase(Event.BUBBLING_PHASE);
        newResult = executeEventListeners(false, event, args);
        if (newResult != null) {
            result = newResult;
        }

        return result;
    }

    /**
     * Copies all the events from the provided container.
     * @param eventListenersContainer where to copy from
     */
    public void copyFrom(final EventListenersContainer eventListenersContainer) {
        for (final Map.Entry<String, Handlers> entry : eventListenersContainer.eventHandlers_.entrySet()) {
            final Handlers handlers = entry.getValue().clone();
            eventHandlers_.put(entry.getKey(), handlers);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[node=" + jsNode_ + " handlers=" + eventHandlers_.keySet() + "]";
    }

}
