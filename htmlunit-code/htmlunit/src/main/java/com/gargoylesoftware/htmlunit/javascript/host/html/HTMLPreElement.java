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

import com.gargoylesoftware.htmlunit.html.HtmlExample;
import com.gargoylesoftware.htmlunit.html.HtmlListing;
import com.gargoylesoftware.htmlunit.html.HtmlPreformattedText;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxClass;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxClasses;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxConstructor;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxGetter;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxSetter;
import com.gargoylesoftware.htmlunit.javascript.configuration.WebBrowser;

/**
 * The JavaScript object "HTMLPreElement".
 *
 * @version $Revision: 10429 $
 * @author Ahmed Ashour
 * @author Ronald Brill
 */
@JsxClasses({
        @JsxClass(domClass = HtmlExample.class, browsers = @WebBrowser(CHROME)),
        @JsxClass(domClass = HtmlPreformattedText.class,
            browsers = { @WebBrowser(FF), @WebBrowser(value = IE, minVersion = 11), @WebBrowser(CHROME) }),
        @JsxClass(domClass = HtmlListing.class, browsers = @WebBrowser(CHROME))
    })
public class HTMLPreElement extends HTMLElement {

    /**
     * Creates an instance.
     */
    @JsxConstructor({ @WebBrowser(CHROME), @WebBrowser(FF) })
    public HTMLPreElement() {
    }

    /**
     * Returns the value of the "cite" property.
     * @return the value of the "cite" property
     */
    @JsxGetter(@WebBrowser(IE))
    public String getCite() {
        final String cite = getDomNodeOrDie().getAttribute("cite");
        return cite;
    }

    /**
     * Returns the value of the "cite" property.
     * @param cite the value
     */
    @JsxSetter(@WebBrowser(IE))
    public void setCite(final String cite) {
        getDomNodeOrDie().setAttribute("cite", cite);
    }
}
