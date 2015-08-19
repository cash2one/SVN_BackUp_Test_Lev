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
package com.gargoylesoftware.htmlunit.javascript.host.event;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.BrowserRunner;
import com.gargoylesoftware.htmlunit.BrowserRunner.Alerts;
import com.gargoylesoftware.htmlunit.WebDriverTestCase;

/**
 * Tests for {@link MessageEvent}.
 *
 * @version $Revision: 10630 $
 * @author Ahmed Ashour
 * @author Frank Danek
 * @author Ronald Brill
 */
@RunWith(BrowserRunner.class)
public class MessageEventTest extends WebDriverTestCase {

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = { "DOM2: exception", "DOM3: [object MessageEvent]" },
            IE8 = { "DOM2: exception", "DOM3: exception" })
    public void createEvent() throws Exception {
        final String html = "<html><head><title>foo</title><script>\n"
            + "  function test() {\n"
            + "    try {\n"
            + "      alert('DOM2: ' + document.createEvent('MessageEvents'));\n"
            + "    } catch(e) {alert('DOM2: exception')}\n"
            + "    try {\n"
            + "      alert('DOM3: ' + document.createEvent('MessageEvent'));\n"
            + "    } catch(e) {alert('DOM3: exception')}\n"
            + "  }\n"
            + "</script></head><body onload='test()'>\n"
            + "</body></html>";
        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    @Alerts(DEFAULT = "no initMessageEvent",
            CHROME = { "message", "true", "true", "hello", "http://localhost:", "2", "[object Window]" },
            IE11 = { "message", "true", "true", "hello", "http://localhost:", "undefined", "[object Window]" },
            IE8 = "no createEvent")
    public void initMessageEvent() throws Exception {
        final String[] expectedAlerts = getExpectedAlerts();
        if (expectedAlerts.length > 4) {
            expectedAlerts[4] += PORT;
            setExpectedAlerts(expectedAlerts);
        }
        final String origin = "http://localhost:" + PORT;
        final String html = "<html><body><script>\n"
            + "try {\n"
            + "  if (document.createEvent) {\n"
            + "    var e = document.createEvent('MessageEvent');\n"
            + "    if (e.initMessageEvent) {\n"
            + "      e.initMessageEvent('message', true, true, 'hello', '" + origin + "', 2, window, null);\n"
            + "      alert(e.type);\n"
            + "      alert(e.bubbles);\n"
            + "      alert(e.cancelable);\n"
            + "      alert(e.data);\n"
            + "      alert(e.origin);\n"
            + "      alert(e.lastEventId);\n"
            + "      alert(e.source);\n"
            + "    } else {\n"
            + "      alert('no initMessageEvent');"
            + "    }\n"
            + "  } else {\n"
            + "    alert('no createEvent');"
            + "  }\n"
            + "} catch(e) { alert(e) }\n"
            + "</script></body></html>";

        loadPageWithAlerts2(html);
    }

}
