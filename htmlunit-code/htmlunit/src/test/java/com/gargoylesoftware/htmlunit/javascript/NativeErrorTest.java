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
package com.gargoylesoftware.htmlunit.javascript;

import static com.gargoylesoftware.htmlunit.BrowserRunner.Browser.CHROME;
import static com.gargoylesoftware.htmlunit.BrowserRunner.Browser.FF;
import static com.gargoylesoftware.htmlunit.BrowserRunner.Browser.IE11;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.BrowserRunner;
import com.gargoylesoftware.htmlunit.BrowserRunner.Alerts;
import com.gargoylesoftware.htmlunit.BrowserRunner.NotYetImplemented;
import com.gargoylesoftware.htmlunit.WebDriverTestCase;

/**
 * Tests for NativeError.
 *
 * @version $Revision: 9843 $
 * @author Ahmed Ashour
 * @author Marc Guillemot
 * @author Frank Danek
 */
@RunWith(BrowserRunner.class)
public class NativeErrorTest extends WebDriverTestCase {

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = "method (url)",
            FF = "method@url",
            IE8 = "undefined")
    @NotYetImplemented({ CHROME, FF, IE11 })
    public void stack() throws Exception {
        final String html
            = "<html><head><title>foo</title><script>\n"
            + "function test() {\n"
            + "  try {\n"
            + "    null.method();\n"
            + "  } catch (e) {\n"
            + "    if (e.stack) {\n"
            + "      var s = e.stack;\n"
            + "      if (s.indexOf('test()@') != -1) {\n"
            + "        alert('method()@url');\n"
            + "      } else if (s.indexOf('test@') != -1) {\n"
            + "        alert('method@url');\n"
            + "      } else if (s.indexOf('test (') != -1) {\n"
            + "        alert('method (url)');\n"
            + "      } else if (s.indexOf('test() (') != -1) {\n"
            + "        alert('method() (url)');\n"
            + "      }\n"
            + "    }\n"
            + "    else\n"
            + "      alert('undefined');\n"
            + "  }\n"
            + "}\n"
            + "</script></head><body onload='test()'>\n"
            + "</body></html>";

        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = "method (url)",
            FF = "method@url",
            IE8 = "undefined")
    @NotYetImplemented({ CHROME, FF, IE11 })
    public void stackNewError() throws Exception {
        final String html
            = "<html><head><title>foo</title><script>\n"
            + "function test() {\n"
            + "  try {\n"
            + "    throw new Error();\n"
            + "  } catch (e) {\n"
            + "    if (e.stack) {\n"
            + "      var s = e.stack;\n"
            + "      if (s.indexOf('test()@') != -1) {\n"
            + "        alert('method()@url');\n"
            + "      } else if (s.indexOf('test@') != -1) {\n"
            + "        alert('method@url');\n"
            + "      } else if (s.indexOf('test (') != -1) {\n"
            + "        alert('method (url)');\n"
            + "      } else if (s.indexOf('test() (') != -1) {\n"
            + "        alert('method() (url)');\n"
            + "      }\n"
            + "    }\n"
            + "    else\n"
            + "      alert('undefined');\n"
            + "  }\n"
            + "}\n"
            + "</script></head><body onload='test()'>\n"
            + "</body></html>";

        loadPageWithAlerts2(html);
    }
}
