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
package com.gargoylesoftware.htmlunit.javascript.host.html;

import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_SCRIPT_ALWAYS_REEXECUTE_ON_SET_TEXT;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_SCRIPT_APPEND_CHILD_THROWS_EXCEPTION;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_SCRIPT_INSERT_BEFORE_THROWS_EXCEPTION;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.CHROME;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.FF;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.IE;
import net.sourceforge.htmlunit.corejs.javascript.Context;

import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.DomText;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlScript;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxClass;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxClasses;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxConstructor;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxGetter;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxSetter;
import com.gargoylesoftware.htmlunit.javascript.configuration.WebBrowser;

/**
 * The JavaScript object that represents an "HTMLScriptElement".
 *
 * @version $Revision: 10984 $
 * @author Daniel Gredler
 * @author Marc Guillemot
 * @author Ahmed Ashour
 * @author Ronald Brill
 * @author Frank Danek
 */
@JsxClasses({
        @JsxClass(domClass = HtmlScript.class,
                browsers = { @WebBrowser(CHROME), @WebBrowser(FF), @WebBrowser(value = IE, minVersion = 11) }),
        @JsxClass(domClass = HtmlScript.class,
            isJSObject = false, browsers = @WebBrowser(value = IE, maxVersion = 8))
    })
public class HTMLScriptElement extends HTMLElement {

    /**
     * Creates an instance.
     */
    @JsxConstructor({ @WebBrowser(CHROME), @WebBrowser(FF) })
    public HTMLScriptElement() {
    }

    /**
     * Returns the <tt>src</tt> attribute.
     * @return the <tt>src</tt> attribute
     */
    @JsxGetter
    public String getSrc() {
        final HtmlScript tmpScript = (HtmlScript) getDomNodeOrDie();
        return tmpScript.getSrcAttribute();
    }

    /**
     * Sets the <tt>src</tt> attribute.
     * @param src the <tt>src</tt> attribute
     */
    @JsxSetter
    public void setSrc(final String src) {
        getDomNodeOrDie().setAttribute("src", src);
    }

    /**
     * Returns the <tt>text</tt> attribute.
     * @return the <tt>text</tt> attribute
     */
    @JsxGetter
    public String getText() {
        final StringBuilder scriptCode = new StringBuilder();
        for (final DomNode node : getDomNodeOrDie().getChildren()) {
            if (node instanceof DomText) {
                final DomText domText = (DomText) node;
                scriptCode.append(domText.getData());
            }
        }
        return scriptCode.toString();
    }

    /**
     * Sets the <tt>text</tt> attribute.
     * @param text the <tt>text</tt> attribute
     */
    @JsxSetter
    public void setText(final String text) {
        final HtmlElement htmlElement = getDomNodeOrDie();
        htmlElement.removeAllChildren();
        final DomNode textChild = new DomText(htmlElement.getPage(), text);
        htmlElement.appendChild(textChild);

        final HtmlScript tmpScript = (HtmlScript) htmlElement;
        if (getBrowserVersion().hasFeature(JS_SCRIPT_ALWAYS_REEXECUTE_ON_SET_TEXT)) {
            tmpScript.resetExecuted();
        }
        tmpScript.executeScriptIfNeeded();
    }

    /**
     * Returns the <tt>type</tt> attribute.
     * @return the <tt>type</tt> attribute
     */
    @JsxGetter
    public String getType() {
        return getDomNodeOrDie().getAttribute("type");
    }

    /**
     * Sets the <tt>type</tt> attribute.
     * @param type the <tt>type</tt> attribute
     */
    @JsxSetter
    public void setType(final String type) {
        getDomNodeOrDie().setAttribute("type", type);
    }

    /**
     * Returns the event handler that fires on every state change.
     * @return the event handler that fires on every state change
     */
    @JsxGetter(@WebBrowser(IE))
    public Object getOnreadystatechange() {
        return getEventHandlerProp("onreadystatechange");
    }

    /**
     * Sets the event handler that fires on every state change.
     * @param handler the event handler that fires on every state change
     */
    @JsxSetter(@WebBrowser(IE))
    public void setOnreadystatechange(final Object handler) {
        setEventHandlerProp("onreadystatechange", handler);
    }

    /**
     * Returns the event handler that fires on load.
     * @return the event handler that fires on load
     */
    @JsxGetter({ @WebBrowser(CHROME), @WebBrowser(FF), @WebBrowser(value = IE, minVersion = 11) })
    public Object getOnload() {
        return getEventHandlerProp("onload");
    }

    /**
     * Sets the event handler that fires on load.
     * @param handler the event handler that fires on load
     */
    @JsxSetter({ @WebBrowser(CHROME), @WebBrowser(FF), @WebBrowser(value = IE, minVersion = 11) })
    public void setOnload(final Object handler) {
        setEventHandlerProp("onload", handler);
    }

    /**
     * Returns the ready state of the script. This is an IE-only property.
     * @return the ready state of the script
     * @see DomNode#READY_STATE_UNINITIALIZED
     * @see DomNode#READY_STATE_LOADING
     * @see DomNode#READY_STATE_LOADED
     * @see DomNode#READY_STATE_INTERACTIVE
     * @see DomNode#READY_STATE_COMPLETE
     */
    @JsxGetter(@WebBrowser(IE))
    public String getReadyState() {
        return getDomNodeOrDie().getReadyState();
    }

    /**
     * Overwritten for special IE handling.
     *
     * @param childObject the node to add to this node
     * @return the newly added child node
     */
    @Override
    public Object appendChild(final Object childObject) {
        if (getBrowserVersion().hasFeature(JS_SCRIPT_APPEND_CHILD_THROWS_EXCEPTION)) {
            throw Context.reportRuntimeError("Unexpected call to method or property access");
        }

        final HtmlScript tmpScript = (HtmlScript) getDomNodeOrDie();
        final boolean wasEmpty = tmpScript.getFirstChild() == null;
        final Object result = super.appendChild(childObject);

        if (wasEmpty) {
            tmpScript.executeScriptIfNeeded();
        }
        return result;
    }

    /**
     * Overwritten for special IE handling.
     *
     * @param args the arguments
     * @return the newly added child node
     */
    @Override
    protected Object insertBeforeImpl(final Object[] args) {
        if (getBrowserVersion().hasFeature(JS_SCRIPT_INSERT_BEFORE_THROWS_EXCEPTION)) {
            throw Context.reportRuntimeError("Unexpected call to method or property access");
        }
        return super.insertBeforeImpl(args);
    }
}
