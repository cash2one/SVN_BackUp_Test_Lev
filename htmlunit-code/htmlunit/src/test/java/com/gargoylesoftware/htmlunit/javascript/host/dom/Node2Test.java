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
package com.gargoylesoftware.htmlunit.javascript.host.dom;

import static org.junit.Assert.assertNotNull;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.BrowserRunner;
import com.gargoylesoftware.htmlunit.BrowserRunner.Alerts;
import com.gargoylesoftware.htmlunit.SimpleWebTestCase;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

/**
 * Tests for {@link Node}.
 *
 * @version $Revision: 10304 $
 * @author Brad Clarke
 * @author <a href="mailto:george@murnock.com">George Murnock</a>
 * @author Bruce Faulkner
 * @author Marc Guillemot
 * @author Ahmed Ashour
 * @author Frank Danek
 */
@RunWith(BrowserRunner.class)
public class Node2Test extends SimpleWebTestCase {

    /**
     * @throws Exception if an error occurs
     */
    @Test
    public void testReplaceChild_WithSameNode() throws Exception {
        final String html
            = "<html><head><title>foo</title><script>\n"
            + "function doTest(){\n"
            + "    var a = document.getElementById('a');\n"
            + "    var b = document.getElementById('b');\n"
            + "    a.replaceChild(b, b);\n"
            + "}\n"
            + "</script></head><body onload='doTest()'><div id='a'><div id='b'/></div></html>";
        final HtmlPage page = loadPageWithAlerts(html);
        assertNotNull(page.getHtmlElementById("b").getParentNode());
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts({ "1", "2" })
    public void eventListener() throws Exception {
        final String html
            = "<html><head>\n"
            + "<script>\n"
            + "  function clicking1() {\n"
            + "    alert(1);\n"
            + "  }\n"
            + "  function clicking2() {\n"
            + "    alert(2);\n"
            + "  }\n"
            + "  function test() {\n"
            + "    var e = document.getElementById('myAnchor');\n"
            + "    if (e.addEventListener) {\n"
            + "      e.addEventListener('click', clicking1, false);\n"
            + "      e.addEventListener('click', clicking2, false);\n"
            + "    } else if (e.attachEvent) {\n"
            + "      e.attachEvent('onclick', clicking1);\n"
            + "      e.attachEvent('onclick', clicking2);\n"
            + "    }\n"
            + "  }\n"
            + "</script></head><body onload='test()'>\n"
            + "  <a href='" + URL_SECOND + "' id='myAnchor'>Click me</a>\n"
            + "</body></html>";

        final List<String> collectedAlerts = new ArrayList<>();
        final HtmlPage page = loadPage(getBrowserVersion(), html, collectedAlerts);
        final HtmlPage page2 = page.getHtmlElementById("myAnchor").click();
        //IE doesn't have specific order
        Collections.sort(collectedAlerts);
        assertEquals(getExpectedAlerts(), collectedAlerts);
        assertEquals(URL_SECOND.toExternalForm(), page2.getUrl().toExternalForm());
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts({ "1", "2" })
    public void eventListener_return_false() throws Exception {
        final String html
            = "<html><head>\n"
            + "<script>\n"
            + "  function clicking1() {\n"
            + "    alert(1);\n"
            + "  }\n"
            + "  function clicking2() {\n"
            + "    alert(2);\n"
            + "    return false;\n"
            + "  }\n"
            + "  function test() {\n"
            + "    var e = document.getElementById('myAnchor');\n"
            + "    if (e.addEventListener) {\n"
            + "      e.addEventListener('click', clicking1, false);\n"
            + "      e.addEventListener('click', clicking2, false);\n"
            + "    } else if (e.attachEvent) {\n"
            + "      e.attachEvent('onclick', clicking1);\n"
            + "      e.attachEvent('onclick', clicking2);\n"
            + "    }\n"
            + "  }\n"
            + "</script></head><body onload='test()'>\n"
            + "  <a href='" + URL_SECOND + "' id='myAnchor'>Click me</a>\n"
            + "</body></html>";

        final List<String> collectedAlerts = new ArrayList<>();
        final HtmlPage page = loadPage(getBrowserVersion(), html, collectedAlerts);
        final HtmlPage page2 = page.getHtmlElementById("myAnchor").click();
        //IE doesn't have specific order
        Collections.sort(collectedAlerts);
        assertEquals(getExpectedAlerts(), collectedAlerts);

        final URL expectedURL;
        if (getBrowserVersion().isIE()) {
            expectedURL = getDefaultUrl();
        }
        else {
            expectedURL = URL_SECOND;
        }
        assertEquals(expectedURL.toExternalForm(), page2.getUrl().toExternalForm());
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = { "1", "2", "§§URL§§second/" },
            IE8 = { "1", "2", "§§URL§§" })
    public void eventListener_returnValue_false() throws Exception {
        final String html
            = "<html><head>\n"
            + "<script>\n"
            + "  function clicking1() {\n"
            + "    alert(1);\n"
            + "  }\n"
            + "  function clicking2() {\n"
            + "    alert(2);\n"
            + "    if (window.event)\n"
            + "      window.event.returnValue = false;\n"
            + "  }\n"
            + "  function test() {\n"
            + "    var e = document.getElementById('myAnchor');\n"
            + "    if (e.addEventListener) {\n"
            + "      e.addEventListener('click', clicking1, false);\n"
            + "      e.addEventListener('click', clicking2, false);\n"
            + "    } else if (e.attachEvent) {\n"
            + "      e.attachEvent('onclick', clicking1);\n"
            + "      e.attachEvent('onclick', clicking2);\n"
            + "    }\n"
            + "  }\n"
            + "</script></head><body onload='test()'>\n"
            + "  <a href='" + URL_SECOND + "' id='myAnchor'>Click me</a>\n"
            + "</body></html>";

        expandExpectedAlertsVariables(URL_FIRST);

        final List<String> collectedAlerts = new ArrayList<>();
        final HtmlPage page = loadPage(getBrowserVersion(), html, collectedAlerts);
        final HtmlPage page2 = page.getHtmlElementById("myAnchor").click();
        //IE doesn't have specific order
        Collections.sort(collectedAlerts);
        assertEquals(ArrayUtils.subarray(getExpectedAlerts(), 0, 2), collectedAlerts);

        assertEquals(getExpectedAlerts()[2], page2.getUrl().toExternalForm());
    }

}
