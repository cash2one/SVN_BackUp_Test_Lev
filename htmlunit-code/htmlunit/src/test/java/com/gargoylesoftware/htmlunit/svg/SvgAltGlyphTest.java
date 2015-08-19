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
package com.gargoylesoftware.htmlunit.svg;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import com.gargoylesoftware.htmlunit.BrowserRunner;
import com.gargoylesoftware.htmlunit.BrowserRunner.Alerts;
import com.gargoylesoftware.htmlunit.WebDriverTestCase;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlPageTest;
import com.gargoylesoftware.htmlunit.html.HtmlUnknownElement;

/**
 * Tests for {@link SvgAltGlyph}.
 *
 * @version $Revision: 9920 $
 * @author Ahmed Ashour
 * @author Frank Danek
 */
@RunWith(BrowserRunner.class)
public class SvgAltGlyphTest extends WebDriverTestCase {

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = "[object SVGAltGlyphElement]",
            CHROME = "[object SVGElement]",
            IE = "[object HTMLGenericElement]",
            IE11 = "[object SVGElement]")
    public void simpleScriptable() throws Exception {
        final String html = HtmlPageTest.STANDARDS_MODE_PREFIX_
            + "<html><head>\n"
            + "<script>\n"
            + "  function test() {\n"
            + "    alert(document.getElementById('myId'));\n"
            + "  }\n"
            + "</script>\n"
            + "</head><body onload='test()'>\n"
            + "  <svg xmlns='http://www.w3.org/2000/svg' version='1.1'>\n"
            + "    <altGlyph id='myId'/>\n"
            + "  </svg>\n"
            + "</body></html>";

        final WebDriver driver = loadPageWithAlerts2(html);
        if (driver instanceof HtmlUnitDriver) {
            final HtmlPage page = (HtmlPage) getWebWindowOf((HtmlUnitDriver) driver).getEnclosedPage();
            if ("[object SVGAltGlyphElement]".equals(getExpectedAlerts()[0])) {
                assertTrue(SvgAltGlyph.class.isInstance(page.getElementById("myId")));
            }
            else if ("[object SVGElement]".equals(getExpectedAlerts()[0])) {
                assertTrue(SvgElement.class.isInstance(page.getElementById("myId")));
            }
            else {
                assertTrue(HtmlUnknownElement.class.isInstance(page.getElementById("myId")));
            }
        }
    }
}
