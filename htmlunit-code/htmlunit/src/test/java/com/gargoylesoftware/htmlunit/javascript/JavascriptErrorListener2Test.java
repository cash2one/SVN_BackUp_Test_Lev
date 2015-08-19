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

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.BrowserRunner;
import com.gargoylesoftware.htmlunit.InteractivePage;
import com.gargoylesoftware.htmlunit.ScriptException;
import com.gargoylesoftware.htmlunit.SimpleWebTestCase;
import com.gargoylesoftware.htmlunit.WebClient;

/**
 * Tests for {@link JavaScriptErrorListener}.
 *
 * @version $Revision: 10913 $
 * @author Ronald Brill
 */
@RunWith(BrowserRunner.class)
public class JavascriptErrorListener2Test extends SimpleWebTestCase {

    /**
     * Test for running with a JavaScript error listener.
     *
     * @throws Exception if the test fails
     */
    @Test
    public void parsingError() throws Exception {
        final StringBuilder scriptExceptions = new StringBuilder();

        final WebClient webClient = getWebClientWithMockWebConnection();
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        webClient.setJavaScriptErrorListener(new JavaScriptErrorListener() {

            @Override
            public void loadScriptError(final InteractivePage page, final URL scriptUrl, final Exception exception) {
                // nothing
            }

            @Override
            public void malformedScriptURL(final InteractivePage page, final String url,
                    final MalformedURLException malformedURLException) {
                // nothing
            }

            @Override
            public void scriptException(final InteractivePage page, final ScriptException scriptException) {
                scriptExceptions.append(scriptException.getCause() + "\n");
            }

            @Override
            public void timeoutError(final InteractivePage page, final long allowedTime, final long executionTime) {
                // nothing
            }
        });

        final String html = "<html><body><script>while (</script></body></html>";
        getMockWebConnection().setDefaultResponse(html);
        webClient.getPage(getDefaultUrl());

        assertEquals("net.sourceforge.htmlunit.corejs.javascript.EvaluatorException: "
            + "Unexpected end of file (script in " + getDefaultUrl() + " from (1, 21) to (1, 37)#1)\n",
                scriptExceptions.toString());
    }
}
