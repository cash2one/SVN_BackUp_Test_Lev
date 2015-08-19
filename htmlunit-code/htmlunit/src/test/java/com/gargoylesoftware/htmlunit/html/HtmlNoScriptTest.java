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
package com.gargoylesoftware.htmlunit.html;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import com.gargoylesoftware.htmlunit.BrowserRunner;
import com.gargoylesoftware.htmlunit.BrowserRunner.Alerts;
import com.gargoylesoftware.htmlunit.WebDriverTestCase;

/**
 * Tests for elements inside {@link HtmlNoScript}.
 *
 * @version $Revision: 10675 $
 * @author Ahmed Ashour
 * @author Marc Guillemot
 * @author Frank Danek
 */
@RunWith(BrowserRunner.class)
public class HtmlNoScriptTest extends WebDriverTestCase {

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts("null")
    public void testGetElementById() throws Exception {
        final String html
            = "<html><head><title>foo</title>\n"
            + "<script>\n"
            + "  function test() {\n"
            + "    alert(document.getElementById('second'));\n"
            + "  }\n"
            + "</script>\n"
            + "</head><body onload='test()'>\n"
            + "    <input type='text' id='first' name='textfield'/>\n"
            + "    <noscript>\n"
            + "    <input type='text' id='second' name='button'/>\n"
            + "    </noscript>\n"
            + "</body></html>";

        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = { "1", "[object Text]" },
            IE8 = { "0", "null" })
    public void childNodes() throws Exception {
        final String html
            = "<html><head><title>foo</title>\n"
            + "<script>\n"
            + "  function test() {\n"
            + "    var noscript = document.getElementById('myDiv').childNodes.item(0);\n"
            + "    alert(noscript.childNodes.length);\n"
            + "    alert(noscript.firstChild);\n"
            + "  }\n"
            + "</script>\n"
            + "</head><body onload='test()'>\n"
            + "    <div id='myDiv'><noscript>\n"
            + "        <input type='text' name='button'/>\n"
            + "      </noscript></div>\n"
            + "</body></html>";

        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts("1")
    public void testJavaScript() throws Exception {
        final String html
            = "<html><head><title>foo</title>\n"
            + "<script>\n"
            + "  alert(1);\n"
            + "</script>\n"
            + "<noscript>\n"
            + "  <script>\n"
            + "    alert(2);\n"
            + "  </script>\n"
            + "</noscript>\n"
            + "</head><body>\n"
            + "</body></html>";

        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    public void testFormValues() throws Exception {
        final String html
            = "<html><body>\n"
            + "<form name='item' method='get'>\n"
            + "  <noscript>\n"
            + "    <input type=hidden name='__webpage_no_js__' value='1'>\n"
            + "  </noscript>\n"
            + "  <input type=hidden name='myParam' value='myValue'>\n"
            + "  <input type='submit' id='clickMe'>\n"
            + "</form>\n"
            + "</body></html>";

        final WebDriver webDriver = loadPage2(html);
        webDriver.findElement(By.id("clickMe")).click();

        assertFalse(webDriver.getCurrentUrl().contains("__webpage_no_js__"));
    }

}
