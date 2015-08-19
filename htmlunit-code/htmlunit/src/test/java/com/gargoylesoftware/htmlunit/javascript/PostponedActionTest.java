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
package com.gargoylesoftware.htmlunit.javascript;

import java.net.URL;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.BrowserRunner;
import com.gargoylesoftware.htmlunit.BrowserRunner.Alerts;
import com.gargoylesoftware.htmlunit.BrowserRunner.NotYetImplemented;
import com.gargoylesoftware.htmlunit.MockWebConnection;
import com.gargoylesoftware.htmlunit.WebDriverTestCase;

/**
 * Tests for the {@link PostponedAction}.
 *
 * @version $Revision: 10534 $
 * @author Ahmed Ashour
 * @author Ronald Brill
 * @author Marc Guillemot
 * @author Matthias Brandt
 */
@RunWith(BrowserRunner.class)
public class PostponedActionTest extends WebDriverTestCase {

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts({ "before", "after", "second.html", "third.html" })
    public void loadingJavaScript() throws Exception {
        final String html = "<html>\n"
            + "<head><title>First Page</title>\n"
            + "<script>\n"
            + "  function test() {\n"
            + "    alert('before');\n"
            + "    var iframe2 = document.createElement('iframe');\n"
            + "    iframe2.src = 'frame2.html';\n"
            + "    document.body.appendChild(iframe2);\n"
            + "    var iframe3 = document.createElement('iframe');\n"
            + "    document.body.appendChild(iframe3);\n"
            + "    iframe3.src = 'frame3.html';\n"
            + "    alert('after');\n"
            + "}\n"
            + "</script>\n"
            + "</head>\n"
            + "<body onload='test()'>\n"
            + "</body>\n"
            + "</html>";
        final String secondContent
            = "<script>alert('second.html');</script>";
        final String thirdContent
            = "<script>alert('third.html');</script>";

        final MockWebConnection conn = getMockWebConnection();
        conn.setResponse(new URL(getDefaultUrl(), "frame2.html"), secondContent);
        conn.setResponse(new URL(getDefaultUrl(), "frame3.html"), thirdContent);

        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts({ "before", "after", "second.html" })
    public void loadingJavaScript2() throws Exception {
        final String firstContent = "<html>\n"
            + "<head><title>First Page</title>\n"
            + "<script>\n"
            + "  function test() {\n"
            + "    alert('before');\n"
            + "    var iframe = document.createElement('iframe');\n"
            + "    document.body.appendChild(iframe);\n"
            + "    iframe.contentWindow.location.replace('" + URL_SECOND + "');\n"
            + "    alert('after');\n"
            + "}\n"
            + "</script>\n"
            + "</head>\n"
            + "<body onload='test()'>\n"
            + "</body>\n"
            + "</html>";
        final String secondContent
            = "<script>alert('second.html');</script>";

        final MockWebConnection conn = getMockWebConnection();
        conn.setResponse(URL_FIRST, firstContent);
        conn.setResponse(URL_SECOND, secondContent);

        loadPageWithAlerts2(URL_FIRST);
    }

    /**
     * Test case for bug #1686.
     *
     * @throws Exception if the test fails
     */
    @Test
    @Alerts({ "setting timeout", "before", "after", "iframe.html", "simpleAlert"})
    @NotYetImplemented
    public void loadingJavaScriptWithTimeout() throws Exception {
        final String html = "<html>\n"
                + "<head><title>First Page</title>\n"
                + "<script>\n"
                + "  function test() {\n"
                + "    alert('before');\n"
                + "    var iframe = document.createElement('iframe');\n"
                + "    iframe.src = 'iframe.html';\n"
                + "    document.body.appendChild(iframe);\n"
                + "    alert('after');\n"
                + "}\n"
                + "  function timeout() {\n"
                + "    alert('setting timeout');\n"
                + "    window.setTimeout(function(){test()}, 1000);\n"
                + "    window.setTimeout(function(){alert('simpleAlert')}, 1100);\n"
                + "}\n"
                + "</script>\n"
                + "</head>\n"
                + "<body onload='timeout()'>\n"
                + "</body>\n"
                + "</html>";
        final String secondContent
                = "<script>alert('iframe.html')</script>";

        final MockWebConnection conn = getMockWebConnection();
        conn.setResponse(new URL(getDefaultUrl(), "iframe.html"), secondContent);

        loadPageWithAlerts2(html);
    }
}
