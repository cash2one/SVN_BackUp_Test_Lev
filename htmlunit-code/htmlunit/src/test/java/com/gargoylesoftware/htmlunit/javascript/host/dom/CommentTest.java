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

import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.BrowserRunner;
import com.gargoylesoftware.htmlunit.BrowserRunner.Alerts;
import com.gargoylesoftware.htmlunit.WebDriverTestCase;

/**
 * Tests for {@link Comment}.
 *
 * @version $Revision: 10304 $
 * @author Mirko Friedenhagen
 * @author Ahmed Ashour
 * @author Frank Danek
 */
@RunWith(BrowserRunner.class)
public class CommentTest extends WebDriverTestCase {

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = "[object Comment]",
            IE8 = "[object]")
    public void simpleScriptable() throws Exception {
        final String html
            = "<html><head><title>foo</title><script>\n"
            + "function test() {\n"
            + "  alert(document.body.firstChild);\n"
            + "}\n"
            + "</script></head><body onload='test()'><!-- comment --></body></html>";
        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = { "after", "comment" },
            IE8 = { "undefined", "undefined" })
    public void textContent() throws Exception {
        final String html
            = "<html><body>\n"
            + "<div id='it'><!--comment-->after</div>"
            + "<script>\n"
            + "var node = document.getElementById('it');\n"
            + "alert(node.textContent);\n"
            + "alert(node.firstChild.textContent);\n"
            + "</script></body></html>";
        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(CHROME = { "after", "undefined" },
            FF = { "undefined", "undefined" },
            IE = { "after", "" },
            IE11 = { "after", "undefined" })
    public void innerText() throws Exception {
        final String html
            = "<html><body>\n"
            + "<div id='it'><!--comment-->after</div>"
            + "<script>\n"
            + "var node = document.getElementById('it');\n"
            + "alert(node.innerText);\n"
            + "alert(node.firstChild.innerText);\n"
            + "</script></body></html>";
        loadPageWithAlerts2(html);
    }

    private void property(final String property) throws Exception {
        final String html
            = "<html><body>\n"
            + "<div id='it'><!--abcdefg-->after</div>"
            + "<script>\n"
            + "var node = document.getElementById('it');\n"
            + "alert(node.firstChild." + property + ");\n"
            + "</script></body></html>";
        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = "undefined",
            IE8 = "")
    public void id() throws Exception {
        property("id");
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = "undefined",
            IE8 = "")
    public void className() throws Exception {
        property("className");
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = "undefined",
            IE8 = "!")
    public void tagName() throws Exception {
        property("tagName");
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = "undefined",
            IE = "<!--abcdefg-->")
    public void text() throws Exception {
        property("text");
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = "undefined",
            IE8 = "[object]")
    public void document() throws Exception {
        property("document");
    }

}
