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

import static com.gargoylesoftware.htmlunit.BrowserRunner.Browser.IE8;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.BrowserRunner;
import com.gargoylesoftware.htmlunit.BrowserRunner.Alerts;
import com.gargoylesoftware.htmlunit.BrowserRunner.NotYetImplemented;
import com.gargoylesoftware.htmlunit.WebDriverTestCase;

/**
 * Tests for HtmlUnit's support of IE conditional comments.
 * @see <a href="http://msdn.microsoft.com/en-us/library/ms537512.asp">MSDN documentation</a>
 *
 * @version $Revision: 9950 $
 * @author Marc Guillemot
 * @author Ahmed Ashour
 * @author Frank Danek
 */
@RunWith(BrowserRunner.class)
public class IEConditionalCommentsTest extends WebDriverTestCase {

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = "hello",
            IE = { "hello", "IE" },
            IE11 = "hello")
    public void ifIE() throws Exception {
        final String html = "<html><head>"
            + "<script>alert('hello')</script>\n"
            + "<!--[if IE]><script>alert('IE')</script><![endif]-->\n"
            + "</head><body></body></html>";
        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = "hello",
            IE = "hello")
    public void if_lte_IE_6() throws Exception {
        final String html = "<html><head>"
            + "<script>alert('hello')</script>\n"
            + "<!--[if lte IE 6]><script>alert('IE6')</script><![endif]-->\n"
            + "</head><body></body></html>";
        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = "hello",
            IE = "hello")
    public void if_lte_IE_7() throws Exception {
        final String html = "<html><head>"
            + "<script>alert('hello')</script>\n"
            + "<!--[if lte IE 7]><script>alert('IE up to 7')</script><![endif]-->\n"
            + "</head><body></body></html>";
        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = "hello",
            IE8 = { "hello", "IE up to 8" })
    public void if_lte_IE_8() throws Exception {
        final String html = "<html><head>"
            + "<script>alert('hello')</script>\n"
            + "<!--[if lte IE 8]><script>alert('IE up to 8')</script><![endif]-->\n"
            + "</head><body></body></html>";
        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = "hello",
            IE = { "hello", "IE up to 9" },
            IE11 = "hello")
    public void if_lte_IE_9() throws Exception {
        final String html = "<html><head>"
            + "<script>alert('hello')</script>\n"
            + "<!--[if lte IE 9]><script>alert('IE up to 9')</script><![endif]-->\n"
            + "</head><body></body></html>";
        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = "hello",
            IE = { "hello", "IE up to 10" },
            IE11 = "hello")
    public void if_lte_IE_10() throws Exception {
        final String html = "<html><head>"
            + "<script>alert('hello')</script>\n"
            + "<!--[if lte IE 10]><script>alert('IE up to 10')</script><![endif]-->\n"
            + "</head><body></body></html>";
        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = "hello",
            IE = { "hello", "lt mso 9" },
            IE11 = "hello")
    public void if_lte_mso_9() throws Exception {
        final String html = "<html><head>"
            + "<script>alert('hello')</script>\n"
            + "<!--[if gte mso 9]><script>alert('gte mso 9')</script><![endif]-->\n"
            + "<!--[if lt mso 9]><script>alert('lt mso 9')</script><![endif]-->\n"
            + "</head><body></body></html>";
        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(CHROME = { "", "" },
            FF = { "undefined", "undefined" },
            IE = { "<!--[if gte IE]>hello<![endif]-->", "world" },
            IE11 = { "", "" })
    public void incorrectExpression() throws Exception {
        final String html = "<html><head></head><body>"
            + "<div id='div1'><!--[if gte IE]>hello<![endif]--></div>\n"
            + "<div id='div2'><!--[if gte IE 5]>world<![endif]--></div>\n"
            + "<script>\n"
            + "alert(document.getElementById('div1').innerText);\n"
            + "alert(document.getElementById('div2').innerText);\n"
            + "</script>\n"
            + "</body></html>";
        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = { "hello", "><" },
            IE8 = { "hello", ">endif<" })
    @NotYetImplemented(IE8)
    public void nested() throws Exception {
        final String html = "<html><body>\n"
            + "<script>alert('hello')</script>\n"
            + "<div id='div1'>\n"
            + "<!--[if lt IE 8]>\n"
            + "ltIE8\n"
            + "<!--[if lt IE 7]>\n"
            + "ltIE7\n"
            + "<![endif]-->\n"
            + "<![endif]-->\n"
            + "</div>\n"
            + "<script>\n"
            + "var div = document.getElementById('div1');\n"
            + "alert('>' + (div.textContent || div.innerText).replace(/\\W*/g, '') + '<');\n"
            + "</script>\n"
            + "</body></html>";
        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = "8+",
            IE8 = "8+")
    public void downlevelRevealed1() throws Exception {
        final String html = "<html><head>"
            + "<![if gte IE 8]>\n"
            + "<script>alert('8+')</script>\n"
            + "<![endif]>\n"
            + "</head><body></body></html>";
        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = "8+",
            IE8 = "8+")
    public void downlevelRevealed2() throws Exception {
        final String html = "<html><head>"
            + "<!--[if gte IE 8]>-->\n"
            + "<script>alert('8+')</script>\n"
            + "<!--<![endif]-->\n"
            + "</head><body></body></html>";
        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = "8+",
            IE8 = "8+")
    public void downlevelRevealed3() throws Exception {
        final String html = "<html><head>"
            + "<!--[if gte IE 8]><!-->\n"
            + "<script>alert('8+')</script>\n"
            + "<!--<![endif]-->\n"
            + "</head><body></body></html>";
        loadPageWithAlerts2(html);
    }

}
