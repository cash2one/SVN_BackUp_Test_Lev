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

import static com.gargoylesoftware.htmlunit.BrowserRunner.Browser.IE8;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.BrowserRunner;
import com.gargoylesoftware.htmlunit.BrowserRunner.Alerts;
import com.gargoylesoftware.htmlunit.BrowserRunner.NotYetImplemented;
import com.gargoylesoftware.htmlunit.WebDriverTestCase;

/**
 * Unit tests for {@link HTMLLinkElement}.
 *
 * @version $Revision: 9952 $
 * @author Daniel Gredler
 * @author Marc Guillemot
 * @author Ahmed Ashour
 */
@RunWith(BrowserRunner.class)
public class HTMLLinkElementTest extends WebDriverTestCase {

    /**
     * @throws Exception if an error occurs
     */
    @Test
    @Alerts(DEFAULT = { "", "", "", "", "§§URL§§test.css", "text/css", "stylesheet", "stylesheet1" },
            IE8 = { "", "", "", "", "test.css", "text/css", "stylesheet", "stylesheet1" })
    @NotYetImplemented(IE8)
    public void basicLinkAttributes() throws Exception {
        final String html =
              "<html>\n"
            + "    <body onload='test()'>\n"
            + "        <script>\n"
            + "            function test() {\n"
            + "                var s = document.createElement('link');\n"
            + "                alert(s.href);\n"
            + "                alert(s.type);\n"
            + "                alert(s.rel);\n"
            + "                alert(s.rev);\n"
            + "                s.href = 'test.css';\n"
            + "                s.type = 'text/css';\n"
            + "                s.rel  = 'stylesheet';\n"
            + "                s.rev  = 'stylesheet1';\n"
            + "                alert(s.href);\n"
            + "                alert(s.type);\n"
            + "                alert(s.rel);\n"
            + "                alert(s.rev);\n"
            + "            }\n"
            + "        </script>\n"
            + "    </body>\n"
            + "</html>";

        loadPageWithAlerts2(html);
    }

}
