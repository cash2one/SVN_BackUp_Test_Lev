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

import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.BrowserRunner;
import com.gargoylesoftware.htmlunit.BrowserRunner.Alerts;
import com.gargoylesoftware.htmlunit.WebDriverTestCase;

/**
 * Tests for {@link CSSValue}.
 *
 * @version $Revision: 10049 $
 * @author Marc Guillemot
 */
@RunWith(BrowserRunner.class)
public class CSSValueTest extends WebDriverTestCase {

    /**
     * @throws Exception on test failure
     */
    @Test
    @Alerts(DEFAULT = "exception",
            FF = { "function CSSValue() {\n    [native code]\n}", "0123" })
    public void test() throws Exception {
        final String html = "<html><head><title>First</title>\n"
                + "<script>\n"
                + "function test(){\n"
                + "  try {\n"
                + "    alert(CSSValue);\n"
                + "    var props = ['CSS_INHERIT', 'CSS_PRIMITIVE_VALUE', 'CSS_VALUE_LIST', 'CSS_CUSTOM'];\n"
                + "    var str = '';\n"
                + "    for (var i=0; i<props.length; ++i)\n"
                + "      str += CSSValue[props[i]];\n"
                + "    alert(str);\n"
                + "  } catch(e) { alert('exception') }\n"
                + "}\n"
                + "</script>\n"
                + "</head><body onload='test()'>\n"
                + "</body></html>";
        loadPageWithAlerts2(html);
    }
}
