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
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_INNER_TEXT_READONLY_FOR_TABLE;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_TABLE_VALIGN_SUPPORTS_IE_VALUES;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.CHROME;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.FF;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.IE;

import com.gargoylesoftware.htmlunit.html.HtmlTableBody;
import com.gargoylesoftware.htmlunit.html.HtmlTableFooter;
import com.gargoylesoftware.htmlunit.html.HtmlTableHeader;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxClass;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxClasses;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxConstructor;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxGetter;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxSetter;
import com.gargoylesoftware.htmlunit.javascript.configuration.WebBrowser;

import net.sourceforge.htmlunit.corejs.javascript.Context;

/**
 * A JavaScript object representing "HTMLTableSectionElement", it is used by
 * {@link HtmlTableBody}, {@link HtmlTableHeader}, and {@link HtmlTableFooter}.
 *
 * @version $Revision: 10780 $
 * @author Daniel Gredler
 * @author Ahmed Ashour
 * @author Ronald Brill
 */
@JsxClasses({
        @JsxClass(domClass = HtmlTableBody.class,
                browsers = { @WebBrowser(CHROME), @WebBrowser(FF), @WebBrowser(value = IE, minVersion = 11) }),
        @JsxClass(domClass = HtmlTableBody.class,
            isJSObject = false, browsers = @WebBrowser(value = IE, maxVersion = 8)),
        @JsxClass(domClass = HtmlTableHeader.class,
            browsers = { @WebBrowser(CHROME), @WebBrowser(FF), @WebBrowser(value = IE, minVersion = 11) }),
        @JsxClass(domClass = HtmlTableHeader.class,
            isJSObject = false, browsers = @WebBrowser(value = IE, maxVersion = 8)),
            @JsxClass(domClass = HtmlTableFooter.class,
            browsers = { @WebBrowser(CHROME), @WebBrowser(FF), @WebBrowser(value = IE, minVersion = 11) }),
        @JsxClass(domClass = HtmlTableFooter.class,
            isJSObject = false, browsers = @WebBrowser(value = IE, maxVersion = 8))
    })
public class HTMLTableSectionElement extends RowContainer {

    /** The valid "vAlign" values for this element, when emulating IE. */
    private static final String[] VALIGN_VALID_VALUES_IE = {"top", "bottom", "middle", "baseline"};

    /** The default value of the "vAlign" property. */
    private static final String VALIGN_DEFAULT_VALUE = "top";

    /**
     * Creates an instance.
     */
    @JsxConstructor({ @WebBrowser(CHROME), @WebBrowser(FF) })
    public HTMLTableSectionElement() {
    }

    /**
     * Returns the value of the "vAlign" property.
     * @return the value of the "vAlign" property
     */
    @JsxGetter
    public String getVAlign() {
        return getVAlign(getValidVAlignValues(), VALIGN_DEFAULT_VALUE);
    }

    /**
     * Sets the value of the "vAlign" property.
     * @param vAlign the value of the "vAlign" property
     */
    @JsxSetter
    public void setVAlign(final Object vAlign) {
        setVAlign(vAlign, getValidVAlignValues());
    }

    /**
     * Returns the valid "vAlign" values for this element, depending on the browser being emulated.
     * @return the valid "vAlign" values for this element, depending on the browser being emulated
     */
    private String[] getValidVAlignValues() {
        String[] valid;
        if (getBrowserVersion().hasFeature(JS_TABLE_VALIGN_SUPPORTS_IE_VALUES)) {
            valid = VALIGN_VALID_VALUES_IE;
        }
        else {
            valid = null;
        }
        return valid;
    }

    /**
     * Returns the value of the "ch" property.
     * @return the value of the "ch" property
     */
    @Override
    @JsxGetter
    public String getCh() {
        return super.getCh();
    }

    /**
     * Sets the value of the "ch" property.
     * @param ch the value of the "ch" property
     */
    @Override
    @JsxSetter
    public void setCh(final String ch) {
        super.setCh(ch);
    }

    /**
     * Returns the value of the "chOff" property.
     * @return the value of the "chOff" property
     */
    @Override
    @JsxGetter
    public String getChOff() {
        return super.getChOff();
    }

    /**
     * Sets the value of the "chOff" property.
     * @param chOff the value of the "chOff" property
     */
    @Override
    @JsxSetter
    public void setChOff(final String chOff) {
        super.setChOff(chOff);
    }

    /**
     * Returns the value of the <tt>bgColor</tt> attribute.
     * @return the value of the <tt>bgColor</tt> attribute
     * @see <a href="http://msdn.microsoft.com/en-us/library/ms533505.aspx">MSDN Documentation</a>
     */
    @JsxGetter(@WebBrowser(IE))
    public String getBgColor() {
        return getDomNodeOrDie().getAttribute("bgColor");
    }

    /**
     * Sets the value of the <tt>bgColor</tt> attribute.
     * @param bgColor the value of the <tt>bgColor</tt> attribute
     * @see <a href="http://msdn.microsoft.com/en-us/library/ms533505.aspx">MSDN Documentation</a>
     */
    @JsxSetter(@WebBrowser(IE))
    public void setBgColor(final String bgColor) {
        setColorAttribute("bgColor", bgColor);
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

    /**
     * Overwritten to throw an exception because this is readonly.
     * @param value the new value for the contents of this node
     */
    @Override
    protected void setInnerTextImpl(final String value) {
        if (getBrowserVersion().hasFeature(JS_INNER_TEXT_READONLY_FOR_TABLE)) {
            throw Context.reportRuntimeError("innerText is read-only for tag '"
                    + getDomNodeOrDie().getNodeName() + "'");
        }
        super.setInnerTextImpl(value);
    }
}
