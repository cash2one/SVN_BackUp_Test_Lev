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
package com.gargoylesoftware.htmlunit.javascript.host.html;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.BrowserRunner;
import com.gargoylesoftware.htmlunit.BrowserRunner.Alerts;
import com.gargoylesoftware.htmlunit.WebDriverTestCase;

/**
 * Unit tests for {@link HTMLTextElement}.
 * @version $Revision: 10985 $
 * @author Ronald Brill
 */
@RunWith(BrowserRunner.class)
public class HTMLTextElementTest extends WebDriverTestCase {

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts({"undefined", "New Text" })
    public void text() throws Exception {
        final String html =
            "<html>\n"
            + "  <head>\n"
            + "    <title>Page Title</title>\n"
            + "    <script>\n"
            + "      function test() {\n"
            + "         var wbr = document.getElementsByTagName('wbr')[0];\n"
            + "         alert(wbr.text);\n"
            + "         wbr.text='New Text';\n"
            + "         alert(wbr.text);\n"
            + "      }\n"
            + "    </script>\n"
            + "  </head>\n"
            + "  <body onload='test()'>\n"
            + "    <p>wbr test <wbr>wbr<wbr></p>\n"
            + "  </body>\n"
            + "</html>";
        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts({"undefined", "New Text" })
    public void textCreateElement() throws Exception {
        final String html =
            "<html>\n"
            + "  <head>\n"
            + "    <script>\n"
            + "      function test() {\n"
            + "         var wbr = document.createElement('wbr');\n"
            + "         alert(wbr.text);\n"
            + "         wbr.text='New Text';\n"
            + "         alert(wbr.text);\n"
            + "      }\n"
            + "    </script>\n"
            + "  </head>\n"
            + "  <body onload='test()'>\n"
            + "  </body>\n"
            + "</html>";
        loadPageWithAlerts2(html);
    }
}
