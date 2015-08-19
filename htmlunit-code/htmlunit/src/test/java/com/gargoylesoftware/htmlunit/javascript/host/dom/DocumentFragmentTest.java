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
 * Tests for {@link DocumentFragment}.
 *
 * @version $Revision: 10848 $
 * @author Marc Guillemot
 * @author Ahmed Ashour
 * @author Ronald Brill
 * @author Frank Danek
 */
@RunWith(BrowserRunner.class)
public class DocumentFragmentTest extends WebDriverTestCase {

    /**
     * Regression test for bug 3191431 on computation from child selector
     * in a document fragment.
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = "[object CSSStyleDeclaration]",
            FF = "[object CSS2Properties]",
            IE8 = "exception")
    public void getComputedStyleOnChild() throws Exception {
        final String html = "<html><head><style>\n"
            + "  body > div { background-color: green#FF0000; }\n"
            + "</style></head>\n"
            + "<body>\n"
            + "<script>\n"
            + "  try {\n"
            + "    var frag = document.createDocumentFragment();\n"
            + "    var d = document.createElement('div');\n"
            + "    frag.appendChild(d);\n"
            + "    alert(window.getComputedStyle(d, null));\n"
            + "  } catch (e) { alert('exception'); }\n"
            + "</script>\n"
            + "</body>\n"
            + "</html>";

        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(FF = "",
            IE = "DIV",
            IE11 = "")
    public void createElement() throws Exception {
        final String html
            = "<html>\n"
            + "  <head>\n"
            + "    <script>\n"
            + "      function test() {\n"
            + "        var frag = document.createDocumentFragment();\n"
            + "        if (frag.createElement) {\n"
            + "          var d = frag.createElement('div');\n"
            + "          alert(d.tagName);\n"
            + "        }\n"
            + "      }\n"
            + "    </script>\n"
            + "  </head>\n"
            + "  <body onload='test()'>\n"
            + "  </body>\n"
            + "</html>";

        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    @Alerts({ "1", "DIV", "DIV" })
    public void querySelector() throws Exception {
        final String html = "<html><head><title>First</title>\n"
            + "<meta http-equiv='X-UA-Compatible' content='IE=edge'>\n"
            + "<script>\n"
            + "function test() {\n"
            + "  var frag = document.createDocumentFragment();\n"
            + "  var d = document.createElement('div');\n"
            + "  frag.appendChild(d);\n"

            + "  if (document.querySelectorAll) {\n"
            + "    try {\n"
            + "      alert(frag.querySelectorAll('div').length);\n"
            + "      alert(frag.querySelectorAll('div')[0].tagName);\n"
            + "      alert(frag.querySelector('div').tagName);\n"
            + "    } catch(e) {alert('exception')}\n"
            + "  }\n"
            + "}\n"
            + "</script></head>\n"
            + "<body onload='test()'>\n"
            + "<div id='root'>\n"
            + "</div>\n"
            + "</body></html>";

        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    @Alerts("0")
    public void children() throws Exception {
        final String html = "<html><head>\n"
            + "<script>\n"
            + "function test() {\n"
            + "  var fragment = document.createDocumentFragment();\n"
            + "  fragment.textContent = '';\n"
            + "  alert(fragment.childNodes.length);\n"
            + "}\n"
            + "</script></head>\n"
            + "<body onload='test()'>\n"
            + "</body></html>";

        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    @Alerts(DEFAULT = { "[object DocumentFragment]", "undefined" },
            IE8 = { "[object HTMLDocument]", "about:blank" })
    public void url() throws Exception {
        final String html = "<!DOCTYPE><html><head>\n"
            + "<script>\n"
            + "function test() {\n"
            + "  var fragment = document.createDocumentFragment();\n"
            + "  alert(fragment);\n"
            + "  alert(fragment.URL);\n"
            + "}\n"
            + "</script></head>\n"
            + "<body onload='test()'>\n"
            + "</body></html>";

        loadPageWithAlerts2(html);
    }
}
