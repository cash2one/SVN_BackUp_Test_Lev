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

import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_INNER_HTML_READONLY_FOR_SOME_TAGS;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_TABLE_COLUMN_WIDTH_NO_NEGATIVE_VALUES;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_TABLE_SPAN_THROWS_EXCEPTION_IF_INVALID;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.CHROME;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.FF;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.IE;

import com.gargoylesoftware.htmlunit.html.HtmlTableColumn;
import com.gargoylesoftware.htmlunit.html.HtmlTableColumnGroup;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxClass;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxClasses;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxConstructor;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxGetter;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxSetter;
import com.gargoylesoftware.htmlunit.javascript.configuration.WebBrowser;

import net.sourceforge.htmlunit.corejs.javascript.Context;

/**
 * The JavaScript object "HTMLTableColElement".
 *
 * @version $Revision: 10780 $
 * @author Ahmed Ashour
 * @author Ronald Brill
 */
@JsxClasses({
        @JsxClass(domClass = HtmlTableColumn.class,
                browsers = { @WebBrowser(CHROME), @WebBrowser(FF), @WebBrowser(value = IE, minVersion = 11) }),
        @JsxClass(domClass = HtmlTableColumn.class,
            isJSObject = false, browsers = @WebBrowser(value = IE, maxVersion = 8)),
        @JsxClass(domClass = HtmlTableColumnGroup.class,
            browsers = { @WebBrowser(CHROME), @WebBrowser(FF), @WebBrowser(value = IE, minVersion = 11) }),
        @JsxClass(domClass = HtmlTableColumnGroup.class,
            isJSObject = false, browsers = @WebBrowser(value = IE, maxVersion = 8))
    })
public class HTMLTableColElement extends HTMLTableComponent {

    /**
     * Creates an instance.
     */
    @JsxConstructor({ @WebBrowser(CHROME), @WebBrowser(FF) })
    public HTMLTableColElement() {
    }

    /**
     * Returns the value of the "span" property.
     * @return the value of the "span" property
     */
    @JsxGetter
    public int getSpan() {
        final String span = getDomNodeOrDie().getAttribute("span");
        int i;
        try {
            i = Integer.parseInt(span);
            if (i < 1) {
                i = 1;
            }
        }
        catch (final NumberFormatException e) {
            i = 1;
        }
        return i;
    }

    /**
     * Sets the value of the "span" property.
     * @param span the value of the "span" property
     */
    @JsxSetter
    public void setSpan(final Object span) {
        final double d = Context.toNumber(span);
        int i = (int) d;
        if (i < 1) {
            if (getBrowserVersion().hasFeature(JS_TABLE_SPAN_THROWS_EXCEPTION_IF_INVALID)) {
                final Exception e = new Exception("Cannot set the span property to invalid value: " + span);
                Context.throwAsScriptRuntimeEx(e);
            }
            else {
                i = 1;
            }
        }
        getDomNodeOrDie().setAttribute("span", Integer.toString(i));
    }

    /**
     * Returns the value of the "width" property.
     * @return the value of the "width" property
     */
    @JsxGetter(propertyName = "width")
    public String getWidth_js() {
        final boolean ie = getBrowserVersion().hasFeature(JS_TABLE_COLUMN_WIDTH_NO_NEGATIVE_VALUES);
        final Boolean returnNegativeValues = ie ? Boolean.FALSE : null;
        return getWidthOrHeight("width", returnNegativeValues);
    }

    /**
     * Sets the value of the "width" property.
     * @param width the value of the "width" property
     */
    @JsxSetter
    public void setWidth(final Object width) {
        setWidthOrHeight("width", width == null ? "" : Context.toString(width), false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isEndTagForbidden() {
        return getDomNodeOrDie() instanceof HtmlTableColumn;
    }

    /**
     * Overwritten to throw an exception in IE8/9.
     * @param value the new value for replacing this node
     */
    @JsxSetter
    @Override
    public void setOuterHTML(final Object value) {
        throw Context.reportRuntimeError("outerHTML is read-only for tag '"
                            + getDomNodeOrDie().getNodeName() + "'");
    }

    /**
     * Overwritten to throw an exception in IE8/9.
     * @param value the new value for the contents of this node
     */
    @JsxSetter
    @Override
    public void setInnerHTML(final Object value) {
        if (getBrowserVersion().hasFeature(JS_INNER_HTML_READONLY_FOR_SOME_TAGS)) {
            throw Context.reportRuntimeError("innerHTML is read-only for tag '"
                            + getDomNodeOrDie().getNodeName() + "'");
        }
        super.setInnerHTML(value);
    }
}
