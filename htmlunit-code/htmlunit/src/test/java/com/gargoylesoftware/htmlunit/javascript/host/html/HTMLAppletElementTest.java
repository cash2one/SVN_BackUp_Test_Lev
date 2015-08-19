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

import static com.gargoylesoftware.htmlunit.BrowserRunner.Browser.IE11;
import static com.gargoylesoftware.htmlunit.BrowserRunner.Browser.IE8;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.BrowserRunner;
import com.gargoylesoftware.htmlunit.BrowserRunner.Alerts;
import com.gargoylesoftware.htmlunit.BrowserRunner.NotYetImplemented;
import com.gargoylesoftware.htmlunit.WebDriverTestCase;

/**
 * Tests for {@link HTMLAppletElement}.
 * @version $Revision: 9935 $
 * @author Ronald Brill
 * @author Frank Danek
 * @author Ahmed Ashour
 */
@RunWith(BrowserRunner.class)
public class HTMLAppletElementTest extends WebDriverTestCase {

    /**
     * @throws Exception if an error occurs
     */
    @Test
    @Alerts(DEFAULT = { "left", "right", "bottom", "middle", "top", "wrong", "" },
            IE = { "left", "right", "bottom", "middle", "top", "", "" })
    @NotYetImplemented({ IE8, IE11 })
    public void getAlign() throws Exception {
        final String html
            = "<html><body>\n"
            + "  <applet id='a1' align='left' ></applet>\n"
            + "  <applet id='a2' align='right' ></applet>\n"
            + "  <applet id='a3' align='bottom' ></applet>\n"
            + "  <applet id='a4' align='middle' ></applet>\n"
            + "  <applet id='a5' align='top' ></applet>\n"
            + "  <applet id='a6' align='wrong' ></applet>\n"
            + "  <applet id='a7' ></applet>\n"

            + "<script>\n"
            + "  for (i=1; i<=7; i++) {\n"
            + "    alert(document.getElementById('a'+i).align);\n"
            + "  };\n"
            + "</script>\n"
            + "</body></html>";
        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    @Alerts(DEFAULT = { "CenTer", "8", "foo", "left", "right", "bottom", "middle", "top" },
            IE = { "center", "error", "center", "error", "center", "left", "right", "bottom", "middle", "top" })
    @NotYetImplemented({ IE8, IE11 })
    public void setAlign() throws Exception {
        final String html
            = "<html><body>\n"
            + "  <applet id='a1' align='left' ></applet>\n"

            + "<script>\n"
            + "  function setAlign(elem, value) {\n"
            + "    try {\n"
            + "      elem.align = value;\n"
            + "    } catch (e) { alert('error'); }\n"
            + "    alert(elem.align);\n"
            + "  }\n"

            + "  var elem = document.getElementById('a1');\n"
            + "  setAlign(elem, 'CenTer');\n"

            + "  setAlign(elem, '8');\n"
            + "  setAlign(elem, 'foo');\n"

            + "  setAlign(elem, 'left');\n"
            + "  setAlign(elem, 'right');\n"
            + "  setAlign(elem, 'bottom');\n"
            + "  setAlign(elem, 'middle');\n"
            + "  setAlign(elem, 'top');\n"
            + "</script>\n"
            + "</body></html>";
        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(IE = { "[object HTMLAppletElement]", "[object HTMLAppletElement]" },
            IE8 = { "[object]", "exception" },
            CHROME = { "[object HTMLAppletElement]", "function HTMLAppletElement() { [native code] }" },
            FF = { "[object HTMLAppletElement]", "function HTMLAppletElement() {\n    [native code]\n}" })
    public void type() throws Exception {
        final String html = ""
            + "<html><head><title>foo</title>\n"
            + "<script>\n"
            + "  function test() {\n"
            + "  var elem = document.getElementById('a1');\n"
            + "    try {\n"
            + "      alert(elem);\n"
            + "      alert(HTMLAppletElement);\n"
            + "    } catch(e) { alert('exception'); }\n"
            + "  }\n"
            + "</script>\n"
            + "</head>\n"
            + "<body onload='test()'>\n"
            + "  <applet id='a1' align='left' ></applet>\n"
            + "</body></html>";

        loadPageWithAlerts2(html);
    }
}
