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
package com.gargoylesoftware.htmlunit.javascript.host.css;

import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.FF;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.IE;

import com.gargoylesoftware.htmlunit.javascript.configuration.JsxClass;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxClasses;
import com.gargoylesoftware.htmlunit.javascript.configuration.WebBrowser;

/**
 * A JavaScript object for {@code CSSConditionRule}.
 *
 * @version $Revision: 10431 $
 * @author Ahmed Ashour
 */
@JsxClasses({
        @JsxClass(browsers = @WebBrowser(FF)),
        @JsxClass(isJSObject = false, isDefinedInStandardsMode = false, browsers = {@WebBrowser(IE) })
    })
public class CSSConditionRule extends CSSGroupingRule {

    /**
     * Creates a new instance.
     */
    public CSSConditionRule() {
    }

    /**
     * Creates a new instance.
     * @param stylesheet the Stylesheet of this rule.
     * @param rule the wrapped rule
     */
    protected CSSConditionRule(final CSSStyleSheet stylesheet, final org.w3c.dom.css.CSSMediaRule rule) {
        super(stylesheet, rule);
    }

}
