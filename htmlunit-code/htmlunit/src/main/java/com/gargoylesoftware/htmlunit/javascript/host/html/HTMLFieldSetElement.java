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

import com.gargoylesoftware.htmlunit.html.HtmlFieldSet;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxClass;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxClasses;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxConstructor;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxGetter;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxSetter;
import com.gargoylesoftware.htmlunit.javascript.configuration.WebBrowser;

/**
 * The JavaScript object "HTMLFieldSetElement".
 *
 * @version $Revision: 10505 $
 * @author Ahmed Ashour
 */
@JsxClasses({
        @JsxClass(domClass = HtmlFieldSet.class,
                browsers = { @WebBrowser(CHROME), @WebBrowser(FF), @WebBrowser(value = IE, minVersion = 11) }),
        @JsxClass(domClass = HtmlFieldSet.class,
            isJSObject = false, browsers = @WebBrowser(value = IE, maxVersion = 8))
    })
public class HTMLFieldSetElement extends FormChild {

    /**
     * Creates an instance.
     */
    @JsxConstructor({ @WebBrowser(CHROME), @WebBrowser(FF) })
    public HTMLFieldSetElement() {
    }

    /**
     * Returns the value of the <tt>align</tt> property.
     * @return the value of the <tt>align</tt> property
     */
    @JsxGetter(@WebBrowser(IE))
    public String getAlign() {
        return getAlign(false);
    }

    /**
     * Sets the value of the <tt>align</tt> property.
     * @param align the value of the <tt>align</tt> property
     */
    @JsxSetter(@WebBrowser(IE))
    public void setAlign(final String align) {
        setAlign(align, false);
    }

}
