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
package com.gargoylesoftware.htmlunit.javascript.host.xml;

import static com.gargoylesoftware.htmlunit.BrowserRunner.Browser.CHROME;
import static org.junit.Assert.assertSame;

import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.BrowserRunner;
import com.gargoylesoftware.htmlunit.BrowserRunner.Alerts;
import com.gargoylesoftware.htmlunit.BrowserRunner.NotYetImplemented;
import com.gargoylesoftware.htmlunit.CollectingAlertHandler;
import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.MockWebConnection;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.WebServerTestCase;
import com.gargoylesoftware.htmlunit.html.DomChangeEvent;
import com.gargoylesoftware.htmlunit.html.DomChangeListener;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.gargoylesoftware.htmlunit.javascript.host.xml.XMLHttpRequestTest.StreamingServlet;

/**
 * Tests for {@link XMLHttpRequest}.
 *
 * @version $Revision: 10804 $
 * @author Daniel Gredler
 * @author Marc Guillemot
 * @author Ahmed Ashour
 * @author Stuart Begg
 * @author Sudhan Moghe
 * @author Frank Danek
 * @author Ronald Brill
 */
@RunWith(BrowserRunner.class)
public class XMLHttpRequest3Test extends WebServerTestCase {

    private static final String MSG_NO_CONTENT = "no Content";
    private static final String MSG_PROCESSING_ERROR = "error processing";

    /**
     * Tests asynchronous use of XMLHttpRequest, using Mozilla style object creation.
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = { "0", "1", "1", "2", "3", "4" },
                FF = { "0", "1", "2", "3", "4" })
    @NotYetImplemented(CHROME)
    public void asyncUse() throws Exception {
        final String html =
              "<html>\n"
            + "  <head>\n"
            + "    <title>XMLHttpRequest Test</title>\n"
            + "    <script>\n"
            + "      var request;\n"
            + "      function testAsync() {\n"
            + "        request = " + XMLHttpRequest2Test.XHRInstantiation_ + ";\n"
            + "        request.onreadystatechange = onReadyStateChange;\n"
            + "        alert(request.readyState);\n"
            + "        request.open('GET', '" + URL_SECOND + "', true);\n"
            + "        request.send('');\n"
            + "      }\n"
            + "      function onReadyStateChange() {\n"
            + "        alert(request.readyState);\n"
            + "        if (request.readyState == 4)\n"
            + "          alert(request.responseText);\n"
            + "      }\n"
            + "    </script>\n"
            + "  </head>\n"
            + "  <body onload='testAsync()'>\n"
            + "  </body>\n"
            + "</html>";

        final String xml =
              "<xml2>\n"
            + "<content2>sdgxsdgx</content2>\n"
            + "<content2>sdgxsdgx2</content2>\n"
            + "</xml2>";

        final WebClient client = getWebClient();
        final List<String> collectedAlerts = Collections.synchronizedList(new ArrayList<String>());
        client.setAlertHandler(new CollectingAlertHandler(collectedAlerts));
        final MockWebConnection conn = new MockWebConnection();
        conn.setResponse(URL_FIRST, html);
        conn.setResponse(URL_SECOND, xml, "text/xml");
        client.setWebConnection(conn);
        client.getPage(URL_FIRST);

        assertEquals(0, client.waitForBackgroundJavaScriptStartingBefore(1000));
        assertEquals(ArrayUtils.add(getExpectedAlerts(), xml), collectedAlerts);
    }

    /**
     * Tests asynchronous use of XMLHttpRequest, where the XHR request fails due to IOException (Connection refused).
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = { "0", "1", "1", "2", "4", MSG_NO_CONTENT, MSG_PROCESSING_ERROR },
            IE8 = { "0", "1", "1", "2", "4", MSG_NO_CONTENT },
            FF = { "0", "1", "2", "4", MSG_NO_CONTENT, MSG_PROCESSING_ERROR })
    @NotYetImplemented(CHROME)
    public void testAsyncUseWithNetworkConnectionFailure() throws Exception {
        final String html =
              "<html>\n"
            + "<head>\n"
            + "<title>XMLHttpRequest Test</title>\n"
            + "<script>\n"
            + "var request;\n"
            + "function testAsync() {\n"
            + "  request = " + XMLHttpRequest2Test.XHRInstantiation_ + ";\n"
            + "  request.onreadystatechange = onReadyStateChange;\n"
            + "  request.onerror = onError;\n"
            + "  alert(request.readyState);\n"
            + "  request.open('GET', '" + URL_SECOND + "', true);\n"
            + "  request.send('');\n"
            + "}\n"
            + "function onError() {\n"
            + "  alert('" + MSG_PROCESSING_ERROR + "');\n"
            + "}\n"
            + "function onReadyStateChange() {\n"
            + "  alert(request.readyState);\n"
            + "  if (request.readyState == 4) {\n"
            + "    if (request.responseText.length == 0)\n"
            + "      alert('" + MSG_NO_CONTENT + "');"
            + "    else\n"
            + "      throw 'Unexpected content, should be zero length but is: \"' + request.responseText + '\"';\n"
            + "  }\n"
            + "}\n"
            + "</script>\n"
            + "</head>\n"
            + "<body onload='testAsync()'>\n"
            + "</body>\n"
            + "</html>";

        final WebClient client = getWebClient();
        final List<String> collectedAlerts = Collections.synchronizedList(new ArrayList<String>());
        client.setAlertHandler(new CollectingAlertHandler(collectedAlerts));
        final MockWebConnection conn = new DisconnectedMockWebConnection();
        conn.setResponse(URL_FIRST, html);
        client.setWebConnection(conn);
        client.getPage(URL_FIRST);

        assertEquals(0, client.waitForBackgroundJavaScriptStartingBefore(1000));
        assertEquals(getExpectedAlerts(), collectedAlerts);
    }

    /**
     * Connection refused WebConnection for URL_SECOND.
     */
    private static final class DisconnectedMockWebConnection extends MockWebConnection {
        /** {@inheritDoc} */
        @Override
        public WebResponse getResponse(final WebRequest request) throws IOException {
            if (URL_SECOND.equals(request.getUrl())) {
                throw new IOException("Connection refused");
            }
            return super.getResponse(request);
        }
    }

    /**
     * Asynchronous callback should be called in "main" js thread and not parallel to other js execution.
     * See http://sourceforge.net/p/htmlunit/bugs/360/.
     * @throws Exception if the test fails
     */
    @Test
    public void noParallelJSExecutionInPage() throws Exception {
        final String content = "<html><head><script>\n"
            + "var j = 0;\n"
            + "function test() {\n"
            + "  req = " + XMLHttpRequest2Test.XHRInstantiation_ + ";\n"
            + "  req.onreadystatechange = handler;\n"
            + "  req.open('post', 'foo.xml', true);\n"
            + "  req.send('');\n"
            + "  alert('before long loop');\n"
            + "  for (var i = 0; i < 5000; i++) {\n"
            + "     j = j + 1;\n"
            + "  }\n"
            + "  alert('after long loop');\n"
            + "}\n"
            + "function handler() {\n"
            + "  if (req.readyState == 4) {\n"
            + "    alert('ready state handler, content loaded: j=' + j);\n"
            + "  }\n"
            + "}\n"
            + "</script></head>\n"
            + "<body onload='test()'></body></html>";

        final WebClient client = getWebClient();
        final List<String> collectedAlerts = Collections.synchronizedList(new ArrayList<String>());
        client.setAlertHandler(new CollectingAlertHandler(collectedAlerts));
        final MockWebConnection conn = new MockWebConnection() {
            @Override
            public WebResponse getResponse(final WebRequest webRequest) throws IOException {
                collectedAlerts.add(webRequest.getUrl().toExternalForm());
                return super.getResponse(webRequest);
            }
        };
        conn.setResponse(URL_FIRST, content);
        final URL urlPage2 = new URL(URL_FIRST + "foo.xml");
        conn.setResponse(urlPage2, "<foo/>\n", "text/xml");
        client.setWebConnection(conn);
        client.getPage(URL_FIRST);

        assertEquals(0, client.waitForBackgroundJavaScriptStartingBefore(1000));

        final String[] alerts = {URL_FIRST.toExternalForm(), "before long loop", "after long loop",
            urlPage2.toExternalForm(), "ready state handler, content loaded: j=5000" };
        assertEquals(alerts, collectedAlerts);
    }

    /**
     * Tests that the different HTTP methods are supported.
     * @throws Exception if an error occurs
     */
    @Test
    public void methods() throws Exception {
        testMethod(HttpMethod.GET);
        testMethod(HttpMethod.HEAD);
        testMethod(HttpMethod.DELETE);
        testMethod(HttpMethod.POST);
        testMethod(HttpMethod.PUT);
        testMethod(HttpMethod.OPTIONS);
        testMethod(HttpMethod.TRACE);
        testMethod(HttpMethod.PATCH);
    }

    /**
     * @throws Exception if the test fails
     */
    private void testMethod(final HttpMethod method) throws Exception {
        final String content = "<html><head><script>\n"
            + "function test() {\n"
            + "  var req = " + XMLHttpRequest2Test.XHRInstantiation_ + ";\n"
            + "  req.open('" + method.name().toLowerCase() + "', 'foo.xml', false);\n"
            + "  req.send('');\n"
            + "}\n"
            + "</script></head>\n"
            + "<body onload='test()'></body></html>";

        final WebClient client = getWebClient();
        final MockWebConnection conn = new MockWebConnection();
        conn.setResponse(URL_FIRST, content);
        final URL urlPage2 = new URL(URL_FIRST + "foo.xml");
        conn.setResponse(urlPage2, "<foo/>\n", "text/xml");
        client.setWebConnection(conn);
        client.getPage(URL_FIRST);

        final WebRequest request = conn.getLastWebRequest();
        assertEquals(urlPage2, request.getUrl());
        assertSame(method, request.getHttpMethod());
    }

    /**
     * Was causing a deadlock on 03.11.2007 (and probably with release 1.13 too).
     * @throws Exception if the test fails
     */
    @Test
    public void xmlHttpRequestWithDomChangeListenerDeadlock() throws Exception {
        final String content
            = "<html><head><title>foo</title>\n"
            + "<script>\n"
            + "  function test() {\n"
            + "    frames[0].test('foo1.txt', true);\n"
            + "    frames[0].test('foo2.txt', false);\n"
            + "  }\n"
            + "</script>\n"
            + "</head>\n"
            + "<body>\n"
            + "<p id='p1' title='myTitle' onclick='test()'></p>\n"
            + "<iframe src='page2.html'></iframe>\n"
            + "</body></html>";

        final String content2
            = "<html><head><title>foo</title>\n"
            + "<script>\n"
            + "function test(_src, _async)\n"
            + "{\n"
            + "  var request = " + XMLHttpRequest2Test.XHRInstantiation_ + ";\n"
            + "  request.onreadystatechange = onReadyStateChange;\n"
            + "  request.open('GET', _src, _async);\n"
            + "  request.send('');\n"
            + "}\n"
            + "function onReadyStateChange() {\n"
            + "  parent.document.getElementById('p1').title = 'new title';\n"
            + "}\n"
            + "</script>\n"
            + "</head>\n"
            + "<body>\n"
            + "<p id='p1' title='myTitle'></p>\n"
            + "</body></html>";

        final MockWebConnection connection = new MockWebConnection() {
            private boolean gotFoo1_ = false;

            @Override
            public WebResponse getResponse(final WebRequest webRequest) throws IOException {
                final String url = webRequest.getUrl().toExternalForm();

                synchronized (this) {
                    while (!gotFoo1_ && url.endsWith("foo2.txt")) {
                        try {
                            wait(100);
                        }
                        catch (final InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                if (url.endsWith("foo1.txt")) {
                    gotFoo1_ = true;
                }
                return super.getResponse(webRequest);
            }
        };
        connection.setDefaultResponse("");
        connection.setResponse(URL_FIRST, content);
        connection.setResponse(new URL(URL_FIRST, "page2.html"), content2);

        final WebClient webClient = getWebClient();
        webClient.setWebConnection(connection);

        final HtmlPage page = webClient.getPage(URL_FIRST);
        final DomChangeListener listener = new DomChangeListener() {
            @Override
            public void nodeAdded(final DomChangeEvent event) {
                // Empty.
            }
            @Override
            public void nodeDeleted(final DomChangeEvent event) {
                // Empty.
            }
        };
        page.addDomChangeListener(listener);
        page.getHtmlElementById("p1").click();
    }

    /**
     * Regression test for bug 1209686 (onreadystatechange not called with partial data when emulating FF).
     * @throws Exception if an error occurs
     */
    @Test
    @NotYetImplemented
    public void streaming() throws Exception {
        final Map<String, Class<? extends Servlet>> servlets = new HashMap<>();
        servlets.put("/test", StreamingServlet.class);

        final String resourceBase = "./src/test/resources/com/gargoylesoftware/htmlunit/javascript/host";
        startWebServer(resourceBase, null, servlets);
        final WebClient client = getWebClient();
        final HtmlPage page = client.getPage("http://localhost:" + PORT + "/XMLHttpRequestTest_streaming.html");
        assertEquals(0, client.waitForBackgroundJavaScriptStartingBefore(1000));
        final HtmlElement body = page.getBody();
        assertEquals(10, body.asText().split("\n").length);
    }

    /**
     * Tests the value of "this" in handler.
     * @throws Exception if the test fails
     */
    @Test
    @Alerts("this == request")
    public void thisValueInHandler() throws Exception {
        final String html =
              "<html>\n"
            + "  <head>\n"
            + "    <title>XMLHttpRequest Test</title>\n"
            + "    <script>\n"
            + "      var request;\n"
            + "      function testAsync() {\n"
            + "        request = " + XMLHttpRequest2Test.XHRInstantiation_ + ";\n"
            + "        request.onreadystatechange = onReadyStateChange;\n"
            + "        request.open('GET', 'foo.xml', true);\n"
            + "        request.send('');\n"
            + "      }\n"
            + "      function onReadyStateChange() {\n"
            + "        if (request.readyState == 4) {\n"
            + "          if (this == request)\n"
            + "            alert('this == request');\n"
            + "          else if (this == onReadyStateChange)\n"
            + "            alert('this == handler');\n"
            + "          else alert('not expected: ' + this)\n"
            + "        }\n"
            + "      }\n"
            + "    </script>\n"
            + "  </head>\n"
            + "  <body onload='testAsync()'>\n"
            + "  </body>\n"
            + "</html>";

        final WebClient client = getWebClient();
        final List<String> collectedAlerts = Collections.synchronizedList(new ArrayList<String>());
        client.setAlertHandler(new CollectingAlertHandler(collectedAlerts));
        final MockWebConnection conn = new MockWebConnection();
        conn.setResponse(URL_FIRST, html);
        conn.setDefaultResponse("");
        client.setWebConnection(conn);
        client.getPage(URL_FIRST);

        assertEquals(0, client.waitForBackgroundJavaScriptStartingBefore(1000));
        assertEquals(getExpectedAlerts(), collectedAlerts);
    }

    /**
     * Test for a strange error we found: An ajax running
     * in parallel shares the additional headers with a form
     * submit.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void ajaxInfluencesSubmitHeaders() throws Exception {
        final Map<String, Class<? extends Servlet>> servlets = new HashMap<>();
        servlets.put("/content.html", ContentServlet.class);
        servlets.put("/ajax_headers.html", AjaxHeaderServlet.class);
        servlets.put("/form_headers.html", FormHeaderServlet.class);
        startWebServer("./", null, servlets);

        collectedHeaders_.clear();
        final WebClient client = getWebClient();

        final List<String> collectedAlerts = Collections.synchronizedList(new ArrayList<String>());
        client.setAlertHandler(new CollectingAlertHandler(collectedAlerts));

        assertEquals(0, client.waitForBackgroundJavaScriptStartingBefore(100));

        final HtmlPage page = client.getPage("http://localhost:" + PORT + "/content.html");
        final DomElement elem = page.getElementById("doIt");
        ((HtmlSubmitInput) elem).click();

        Thread.sleep(400); // wait a bit to be sure, both request are out
        assertEquals(0, client.waitForBackgroundJavaScriptStartingBefore(1000));

        String headers = collectedHeaders_.get(0);
        assertTrue(headers, headers.startsWith("Form: "));
        assertFalse(headers, headers.contains("Html-Unit=is great,;"));

        headers = collectedHeaders_.get(1);
        assertTrue(headers, headers.startsWith("Ajax: "));
        assertTrue(headers, headers.contains("Html-Unit=is great,;"));
    }

    static final List<String> collectedHeaders_ = Collections.synchronizedList(new ArrayList<String>());

    /**
     * First servlet for {@link #testNoContent()}.
     */
    public static class ContentServlet extends HttpServlet {
        /**
         * {@inheritDoc}
         */
        @Override
        protected void doGet(final HttpServletRequest req, final HttpServletResponse res) throws IOException {
            final String html = "<html><head><script>\n"
                    + "  function test() {\n"
                    + "    xhr = " + XMLHttpRequest2Test.XHRInstantiation_ + ";\n"
                    + "    xhr.open('POST', 'ajax_headers.html', true);\n"
                    + "    xhr.setRequestHeader('Html-Unit', 'is great');\n"
                    + "    xhr.send('');\n"
                    + "  }\n"
                    + "</script></head>\n"
                    + "<body onload='test()'>\n"
                    + "  <form action='form_headers.html' name='myForm'>\n"
                    + "    <input name='myField' value='some value'>\n"
                    + "    <input type='submit' id='doIt' value='Do It'>\n"
                    + "  </form>\n"
                    + "</body></html>";

            res.setContentType("text/html");
            final Writer writer = res.getWriter();
            writer.write(html);
            writer.close();
        }
    }

    /**
     * Servlet for {@link #setRequestHeader()}.
     */
    public static class AjaxHeaderServlet extends HttpServlet {
        /**
         * {@inheritDoc}
         */
        @Override
        protected void doPost(final HttpServletRequest req, final HttpServletResponse res) throws IOException {
            doGet(req, res);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
            final String header = headers(request);
            try {
                // do not return before the form request is also sent
                Thread.sleep(666);
            }
            catch (final InterruptedException e) {
                e.printStackTrace();
            }

            collectedHeaders_.add("Ajax: " + header);
            response.setContentType("text/plain");
            final Writer writer = response.getWriter();
            writer.write(header);
            writer.close();
        }
    }

    /**
     * Servlet for {@link #setRequestHeader()}.
     */
    public static class FormHeaderServlet extends HttpServlet {
        /**
         * {@inheritDoc}
         */
        @Override
        protected void doPost(final HttpServletRequest req, final HttpServletResponse res) throws IOException {
            doGet(req, res);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
            final String header = headers(request);

            final String html = "<html><head></head>\n"
                    + "<body>\n"
                    + "<p>Form: " + header + "</p<\n"
                    + "</body></html>";

            collectedHeaders_.add("Form: " + header);
            response.setContentType("text/html");
            final Writer writer = response.getWriter();
            writer.write(html);
            writer.close();
        }
    }

    static String headers(final HttpServletRequest request) {
        final StringBuilder text = new StringBuilder();
        text.append("Headers: ");
        final Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            final String name = headerNames.nextElement();
            text.append(name);
            text.append('=');
            final Enumeration<String> headers = request.getHeaders(name);
            while (headers.hasMoreElements()) {
                final String header = headers.nextElement();
                text.append(header);
                text.append(',');
            }
            text.append(';');
        }
        return text.toString();
    }
}
