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

import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.CSS_FONTFACERULE_CSSTEXT_CRLF;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.CHROME;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.FF;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.IE;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.gargoylesoftware.htmlunit.javascript.configuration.JsxClass;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxConstructor;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxGetter;
import com.gargoylesoftware.htmlunit.javascript.configuration.WebBrowser;

/**
 * A JavaScript object for a {@link org.w3c.dom.css.CSSFontFaceRule}.
 *
 * @version $Revision: 10000 $
 * @author Marc Guillemot
 * @author Ronald Brill
 * @author Frank Danek
 * @author Ahmed Ashour
 */
@JsxClass(browsers = { @WebBrowser(CHROME), @WebBrowser(FF), @WebBrowser(value = IE, minVersion = 11) })
public class CSSFontFaceRule extends CSSRule {

    private static final Pattern REPLACEMENT_1 = Pattern.compile("font-family: ([^;]*);");
    private static final Pattern REPLACEMENT_2 = Pattern.compile("src: url\\(([^;]*)\\);");

    /**
     * Creates a new instance.
     */
    @JsxConstructor(@WebBrowser(CHROME))
    public CSSFontFaceRule() {
    }

    /**
     * Creates a new instance.
     * @param stylesheet the Stylesheet of this rule.
     * @param rule the wrapped rule
     */
    protected CSSFontFaceRule(final CSSStyleSheet stylesheet, final org.w3c.dom.css.CSSFontFaceRule rule) {
        super(stylesheet, rule);
    }

    @Override
    @JsxGetter({ @WebBrowser(FF), @WebBrowser(value = IE, minVersion = 11) })
    public short getType() {
        return FONT_FACE_RULE;
    }

    @Override
    @JsxGetter({ @WebBrowser(FF), @WebBrowser(value = IE, minVersion = 11) })
    public String getCssText() {
        String cssText = super.getCssText();
        if (getBrowserVersion().hasFeature(CSS_FONTFACERULE_CSSTEXT_CRLF)) {
            cssText = StringUtils.replace(cssText, "{", "{\r\n\t");
            cssText = StringUtils.replace(cssText, "}", ";\r\n}\r\n");
            cssText = StringUtils.replace(cssText, "; ", ";\r\n\t");
        }
        else {
            cssText = StringUtils.replace(cssText, "{", "{\n  ");
            cssText = StringUtils.replace(cssText, "}", ";\n}");
            cssText = StringUtils.replace(cssText, "; ", ";\n  ");
            cssText = REPLACEMENT_1.matcher(cssText).replaceFirst("font-family: \"$1\";");
            cssText = REPLACEMENT_2.matcher(cssText).replaceFirst("src: url(\"$1\");");
        }
        return cssText;
    }
}
