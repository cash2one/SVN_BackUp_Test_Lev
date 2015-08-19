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
 * Tests for {@link BaseFrameElement}.
 *
 * @version $Revision: 10283 $
 * @author Ahmed Ashour
 * @author Ronald Brill
 */
@RunWith(BrowserRunner.class)
public class BaseFrameElement2Test extends WebDriverTestCase {

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = { "posted", "called" },
            IE8 = "posted")
    public void windowEventListenersContainer() throws Exception {
        final String html
            = "<html>\n"
            + "<head>\n"
            + "<script>\n"
            + "  function test() {\n"
            + "    var iframe = document.createElement('iframe');\n"
            + "    iframe.src = '';\n"
            + "    document.documentElement.appendChild(iframe);\n"
            + "    var win = iframe.contentWindow;\n"
            + "    if (win.addEventListener) {\n"
            + "      win.addEventListener('message', handler);\n"
            + "    } else {\n"
            + "      win.attachEvent('message', handler);\n"
            + "    }\n"
            + "    win.postMessage('hello', '*');\n"
            + "    alert('posted');\n"
            + "  }\n"
            + "  function handler() {\n"
            + "      alert('called');\n"
            + "  }\n"
            + "</script>\n"
            + "</head>\n"
            + "<body onload='test()'>\n"
            + "</body></html>";

        loadPageWithAlerts2(html);
    }
}
