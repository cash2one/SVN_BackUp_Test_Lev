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
import com.gargoylesoftware.htmlunit.SimpleWebTestCase;
import com.gargoylesoftware.htmlunit.WebResponse;

/**
 * Tests for {@link HtmlLink}.
 *
 * @version $Revision: 9842 $
 * @author Ahmed Ashour
 * @author Marc Guillemot
 */
@RunWith(BrowserRunner.class)
public class HtmlLinkTest extends SimpleWebTestCase {

    /**
     * @throws Exception if the test fails
     */
    @Test
    public void getResponse_referer() throws Exception {
        final String html = "<html><head>\n"
            + "<link id='myId' href='file1.css'></link>\n"
            + "</head><body>\n"
            + "</body></html>";

        final HtmlPage page = loadPage(html);

        final HtmlLink link = page.getFirstByXPath("//link");
        final WebResponse respCss = link.getWebResponse(true);
        assertEquals(page.getUrl().toExternalForm(), respCss.getWebRequest().getAdditionalHeaders().get("Referer"));
    }
}
