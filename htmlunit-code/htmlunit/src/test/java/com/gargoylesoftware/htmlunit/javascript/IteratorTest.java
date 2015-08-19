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

import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.BrowserRunner;
import com.gargoylesoftware.htmlunit.BrowserRunner.Alerts;
import com.gargoylesoftware.htmlunit.WebDriverTestCase;

/**
 * Tests for Iterator.
 *
 * @version $Revision: 9980 $
 * @author Ronald Brill
 */
@RunWith(BrowserRunner.class)
public class IteratorTest extends WebDriverTestCase {

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = "Iterator not available",
            FF = { "first,1", "second,2" })
    public void simple() throws Exception {
        final String html
            = "<html>\n"
            + "<head>\n"
            + "<script>\n"
            + "  function test() {\n"
            + "    if (!window.Iterator) { alert('Iterator not available'); return; }\n"

            + "    var data = { first: 1, second: 2 };\n"
            + "    var it = Iterator(data);\n"
            + "    alert(it.next());\n"
            + "    alert(it.next());\n"
            + "  }\n"
            + "</script>\n"
            + "</head>\n"
            + "<body onload='test()'>\n"
            + "</body></html>";

        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = "Iterator not available",
            FF = "[object StopIteration]")
    public void stopIteration() throws Exception {
        final String html
            = "<html>\n"
            + "<head>\n"
            + "<script>\n"
            + "  function test() {\n"
            + "    if (!window.Iterator) { alert('Iterator not available'); return; }\n"

            + "    var data = { };\n"
            + "    var it = Iterator(data);\n"
            + "    \n"
            + "    try {\n"
            + "      alert(it.next());\n"
            + "    } catch (e) {\n"
            + "      if (e == StopIteration) {\n"
            + "        alert(e);\n"
            + "      } else {\n"
            + "        alert('ex');\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "</script>\n"
            + "</head>\n"
            + "<body onload='test()'>\n"
            + "</body></html>";

        loadPageWithAlerts2(html);
    }
}
