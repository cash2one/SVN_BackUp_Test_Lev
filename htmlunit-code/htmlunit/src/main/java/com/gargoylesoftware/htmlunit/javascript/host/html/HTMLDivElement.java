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

import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.CHROME;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.FF;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.IE;

import com.gargoylesoftware.htmlunit.html.HtmlDivision;
import com.gargoylesoftware.htmlunit.html.HtmlMarquee;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxClass;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxClasses;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxConstructor;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxGetter;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxSetter;
import com.gargoylesoftware.htmlunit.javascript.configuration.WebBrowser;

import net.sourceforge.htmlunit.corejs.javascript.Context;

/**
 * The JavaScript object "HTMLDivElement".
 *
 * @version $Revision: 10780 $
 * @author Ahmed Ashour
 * @author Daniel Gredler
 * @author Ronald Brill
 */
@JsxClasses({
        @JsxClass(domClass = HtmlDivision.class,
                browsers = { @WebBrowser(FF), @WebBrowser(CHROME), @WebBrowser(value = IE, minVersion = 11) }),
        @JsxClass(isJSObject = false, domClass = HtmlDivision.class,
                browsers = { @WebBrowser(value = IE, maxVersion = 8) }),
        @JsxClass(domClass = HtmlMarquee.class, browsers = @WebBrowser(FF))
    })
public class HTMLDivElement extends HTMLElement {

    /**
     * Creates an instance.
     */
    @JsxConstructor({ @WebBrowser(CHROME), @WebBrowser(FF) })
    public HTMLDivElement() {
    }

    /**
     * Returns the value of the "align" property.
     * @return the value of the "align" property
     */
    @JsxGetter
    public String getAlign() {
        return getAlign(true);
    }

    /**
     * Sets the value of the "align" property.
     * @param align the value of the "align" property
     */
    @JsxSetter
    public void setAlign(final String align) {
        setAlign(align, false);
    }

    /**
     * Returns the {@code dataFld} attribute.
     * @return the {@code dataFld} attribute
     */
    @JsxGetter(@WebBrowser(value = IE, maxVersion = 8))
    public String getDataFld() {
        throw Context.throwAsScriptRuntimeEx(new UnsupportedOperationException());
    }

    /**
     * Sets the {@code dataFld} attribute.
     * @param dataFld {@code dataFld} attribute
     */
    @JsxSetter(@WebBrowser(value = IE, maxVersion = 8))
    public void setDataFld(final String dataFld) {
        throw Context.throwAsScriptRuntimeEx(new UnsupportedOperationException());
    }

    /**
     * Returns the {@code dataFormatAs} attribute.
     * @return the {@code dataFormatAs} attribute
     */
    @JsxGetter(@WebBrowser(value = IE, maxVersion = 8))
    public String getDataFormatAs() {
        throw Context.throwAsScriptRuntimeEx(new UnsupportedOperationException());
    }

    /**
     * Sets the {@code dataFormatAs} attribute.
     * @param dataFormatAs {@code dataFormatAs} attribute
     */
    @JsxSetter(@WebBrowser(value = IE, maxVersion = 8))
    public void setDataFormatAs(final String dataFormatAs) {
        throw Context.throwAsScriptRuntimeEx(new UnsupportedOperationException());
    }

    /**
     * Returns the {@code dataSrc} attribute.
     * @return the {@code dataSrc} attribute
     */
    @JsxGetter(@WebBrowser(value = IE, maxVersion = 8))
    public String getDataSrc() {
        throw Context.throwAsScriptRuntimeEx(new UnsupportedOperationException());
    }

    /**
     * Sets the {@code dataSrc} attribute.
     * @param dataSrc {@code dataSrc} attribute
     */
    @JsxSetter(@WebBrowser(value = IE, maxVersion = 8))
    public void setDataSrc(final String dataSrc) {
        throw Context.throwAsScriptRuntimeEx(new UnsupportedOperationException());
    }
}
