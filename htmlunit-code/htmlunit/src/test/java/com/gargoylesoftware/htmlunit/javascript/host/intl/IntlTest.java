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
package com.gargoylesoftware.htmlunit.javascript.host.intl;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.BrowserRunner;
import com.gargoylesoftware.htmlunit.BrowserRunner.Alerts;
import com.gargoylesoftware.htmlunit.WebDriverTestCase;
import com.gargoylesoftware.htmlunit.html.HtmlPageTest;

/**
 * Tests for {@link Intl}.
 *
 * @version $Revision: 10257 $
 * @author Ahmed Ashour
 */
@RunWith(BrowserRunner.class)
public class IntlTest extends WebDriverTestCase {

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = "[object Object]",
            IE8 = "exception")
    public void intl() throws Exception {
        test("Intl");
    }

    private void test(final String string) throws Exception {
        final String html = HtmlPageTest.STANDARDS_MODE_PREFIX_
            + "<html><head>\n"
            + "<script>\n"
            + "  function test() {\n"
            + "    try {\n"
            + "      alert(" + string + ");\n"
            + "    } catch(e) {alert('exception')}\n"
            + "  }\n"
            + "</script>\n"
            + "</head><body onload='test()'>\n"
            + "</body></html>";

        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(CHROME = "function () { [native code] }",
            FF = "function Collator() {\n    [native code]\n}",
            IE8 = "exception",
            IE11 = "\nfunction Collator() {\n    [native code]\n}\n")
    public void collator() throws Exception {
        test("Intl.Collator");
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(CHROME = "function () { [native code] }",
            FF = "function DateTimeFormat() {\n    [native code]\n}",
            IE8 = "exception",
            IE11 = "\nfunction DateTimeFormat() {\n    [native code]\n}\n")
    public void dateTimeFormat() throws Exception {
        test("Intl.DateTimeFormat");
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(CHROME = "function () { [native code] }",
            FF = "function NumberFormat() {\n    [native code]\n}",
            IE8 = "exception",
            IE11 = "\nfunction NumberFormat() {\n    [native code]\n}\n")
    public void numberFormat() throws Exception {
        test("Intl.NumberFormat");
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = "undefined",
            CHROME = "function () { [native code] }",
            IE8 = "exception")
    public void v8BreakIterator() throws Exception {
        test("Intl.v8BreakIterator");
    }

}
