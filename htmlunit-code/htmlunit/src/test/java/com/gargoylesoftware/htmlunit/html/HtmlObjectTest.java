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

import static com.gargoylesoftware.htmlunit.BrowserRunner.Browser.CHROME;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.BrowserRunner;
import com.gargoylesoftware.htmlunit.BrowserRunner.Alerts;
import com.gargoylesoftware.htmlunit.BrowserRunner.NotYetImplemented;
import com.gargoylesoftware.htmlunit.CollectingAlertHandler;
import com.gargoylesoftware.htmlunit.MockWebConnection;
import com.gargoylesoftware.htmlunit.SimpleWebTestCase;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.javascript.host.ActiveXObjectTest;

/**
 * Tests for {@link HtmlObject}.
 *
 * @version $Revision: 10156 $
 * @author Ahmed Ashour
 */
@RunWith(BrowserRunner.class)
public class HtmlObjectTest extends SimpleWebTestCase {

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(IE = "false",
            FF = "undefined")
    @NotYetImplemented(CHROME)
    public void classid() throws Exception {
        if (getBrowserVersion().isIE() && !ActiveXObjectTest.isJacobInstalled()) {
            return;
        }
        final String html = "<html><head>\n"
            // Window Media Player CLASSID
            + "<object id='wm' classid='clsid:6BF52A52-394A-11D3-B153-00C04F79FAA6'></object>\n"
            + "<script>\n"
            + "  function test() {\n"
            + "    alert(document.all.wm.fullScreen);\n"
            + "  }\n"
            + "</script>\n"
            + "</head><body onload='test()'>\n"
            + "</body></html>";

        final WebClient client = getWebClient();
        client.getOptions().setActiveXNative(true);
        final List<String> collectedAlerts = new ArrayList<>();
        client.setAlertHandler(new CollectingAlertHandler(collectedAlerts));

        final MockWebConnection webConnection = new MockWebConnection();
        webConnection.setResponse(getDefaultUrl(), html);
        client.setWebConnection(webConnection);

        client.getPage(getDefaultUrl());
        assertEquals(getExpectedAlerts(), collectedAlerts);
    }
}
