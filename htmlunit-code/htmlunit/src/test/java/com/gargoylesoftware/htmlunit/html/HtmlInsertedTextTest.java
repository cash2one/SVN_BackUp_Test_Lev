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

import com.gargoylesoftware.htmlunit.BrowserRunner;
import com.gargoylesoftware.htmlunit.BrowserRunner.Alerts;
import com.gargoylesoftware.htmlunit.WebDriverTestCase;

/**
 * Tests for {@link HtmlInsertedText}.
 *
 * @version $Revision: 10103 $
 * @author Marc Guillemot
 */
@RunWith(BrowserRunner.class)
public class HtmlInsertedTextTest extends WebDriverTestCase {

    /**
     * Regression test for 3033189: &lt;ins&gt; elements are inline elements.
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = { "abcdef", "abcdef" },
            FF = { "abcdef", "undefined" },
            IE8 = { "undefined", "abcdef" })
    public void simple() throws Exception {
        final String html = "<html>\n"
            + "<body>\n"
            + "  <a href='foo' id='it'>ab<ins>cd</ins>ef</a>\n"
            + "  <script>\n"
            + "    var e = document.getElementById('it');\n"
            + "    alert(e.textContent);\n"
            + "    alert(e.innerText);\n"
            + "  </script>\n"
            + "</body></html>";

        loadPageWithAlerts2(html);
    }
}
