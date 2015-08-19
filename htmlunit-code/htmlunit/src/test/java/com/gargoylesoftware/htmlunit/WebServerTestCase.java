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
package com.gargoylesoftware.htmlunit;

import java.io.StringReader;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.Servlet;

import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.webapp.WebAppClassLoader;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebDescriptor;
import org.eclipse.jetty.xml.XmlParser;
import org.junit.After;
import org.xml.sax.InputSource;

import com.gargoylesoftware.htmlunit.WebDriverTestCase.MockWebConnectionServlet;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import net.sourceforge.htmlunit.corejs.javascript.NativeArray;
import net.sourceforge.htmlunit.corejs.javascript.Undefined;

/**
 * A WebTestCase which starts a local server, and doens't use WebDriver.
 *
 * <b>Note that {@link WebDriverTestCase} should be used unless HtmlUnit-specific feature
 * is needed and Selenium does not support it.</b>
 *
 * @version $Revision: 10780 $
 * @author Ahmed Ashour
 * @author Marc Guillemot
 */
public abstract class WebServerTestCase extends WebTestCase {

    private Server server_;
    private static Boolean LAST_TEST_MockWebConnection_;
    private static Server STATIC_SERVER_;
    private WebClient webClient_;

    /**
     * Starts the web server on the default {@link #PORT}.
     * The given resourceBase is used to be the ROOT directory that serves the default context.
     * <p><b>Don't forget to stop the returned HttpServer after the test</b>
     *
     * @param resourceBase the base of resources for the default context
     * @throws Exception if the test fails
     */
    protected void startWebServer(final String resourceBase) throws Exception {
        if (server_ != null) {
            throw new IllegalStateException("startWebServer() can not be called twice");
        }
        server_ = new Server(PORT);

        final WebAppContext context = new WebAppContext();
        context.setContextPath("/");
        context.setResourceBase(resourceBase);

        final ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setResourceBase(resourceBase);
        resourceHandler.getMimeTypes().addMimeMapping("js", "application/javascript");

        final HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[]{resourceHandler, context});
        server_.setHandler(handlers);
        server_.setHandler(resourceHandler);
        server_.start();
    }

    /**
     * Starts the web server on the default {@link #PORT}.
     * The given resourceBase is used to be the ROOT directory that serves the default context.
     * <p><b>Don't forget to stop the returned HttpServer after the test</b>
     *
     * @param resourceBase the base of resources for the default context
     * @param classpath additional classpath entries to add (may be null)
     * @throws Exception if the test fails
     */
    protected void startWebServer(final String resourceBase, final String[] classpath) throws Exception {
        if (server_ != null) {
            throw new IllegalStateException("startWebServer() can not be called twice");
        }
        server_ = createWebServer(resourceBase, classpath);
    }

    /**
     * This is usually needed if you want to have a running server during many tests invocation.
     *
     * Creates and starts a web server on the default {@link #PORT}.
     * The given resourceBase is used to be the ROOT directory that serves the default context.
     * <p><b>Don't forget to stop the returned Server after the test</b>
     *
     * @param resourceBase the base of resources for the default context
     * @param classpath additional classpath entries to add (may be null)
     * @return the newly created server
     * @throws Exception if an error occurs
     */
    public static Server createWebServer(final String resourceBase, final String[] classpath) throws Exception {
        return createWebServer(PORT, resourceBase, classpath, null, null);
    }

    /**
     * This is usually needed if you want to have a running server during many tests invocation.
     *
     * Creates and starts a web server on the default {@link #PORT}.
     * The given resourceBase is used to be the ROOT directory that serves the default context.
     * <p><b>Don't forget to stop the returned Server after the test</b>
     *
     * @param port the port to which the server is bound
     * @param resourceBase the base of resources for the default context
     * @param classpath additional classpath entries to add (may be null)
     * @param servlets map of {String, Class} pairs: String is the path spec, while class is the class
     * @param handler wrapper for handler (can be null)
     * @return the newly created server
     * @throws Exception if an error occurs
     */
    public static Server createWebServer(final int port, final String resourceBase, final String[] classpath,
            final Map<String, Class<? extends Servlet>> servlets, final HandlerWrapper handler) throws Exception {

        disableEntityResolution();

        final Server server = new Server(port);

        final WebAppContext context = new WebAppContext();
        context.setContextPath("/");
        context.setResourceBase(resourceBase);

        if (servlets != null) {
            for (final Map.Entry<String, Class<? extends Servlet>> entry : servlets.entrySet()) {
                final String pathSpec = entry.getKey();
                final Class<? extends Servlet> servlet = entry.getValue();
                context.addServlet(servlet, pathSpec);

                // disable defaults if someone likes to register his own root servlet
                if ("/".equals(pathSpec)) {
                    context.setDefaultsDescriptor(null);
                    context.addServlet(DefaultServlet.class, "/favicon.ico");
                }
            }
        }

        final WebAppClassLoader loader = new WebAppClassLoader(context);
        if (classpath != null) {
            for (final String path : classpath) {
                loader.addClassPath(path);
            }
        }
        context.setClassLoader(loader);
        if (handler != null) {
            handler.setHandler(context);
            server.setHandler(handler);
        }
        else {
            server.setHandler(context);
        }
        server.start();
        return server;
    }

    /**
     * Disabled external entity resolution.
     *
     * <p>This happens if there is no internet connection or we are behind a proxy server, and the web.xml contains
     * <pre>&lt;!DOCTYPE web-app PUBLIC "-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN"
     *  "http://java.sun.com/dtd/web-app_2_3.dtd"&gt;</pre>
     *
     * @throws Exception if an error occurs
     */
    private static void disableEntityResolution() throws Exception {
        final Field field = WebDescriptor.class.getDeclaredField("_parserSingleton");
        field.setAccessible(true);
        field.set(null, new XmlParser(false) {
            @Override
            protected InputSource resolveEntity(final String pid, final String sid) {
                return new InputSource(new StringReader(""));
            }
        });
    }

    /**
     * Starts the web server on the default {@link #PORT}.
     * The given resourceBase is used to be the ROOT directory that serves the default context.
     * <p><b>Don't forget to stop the returned HttpServer after the test</b>
     *
     * @param resourceBase the base of resources for the default context
     * @param classpath additional classpath entries to add (may be null)
     * @param servlets map of {String, Class} pairs: String is the path spec, while class is the class
     * @throws Exception if the test fails
     */
    protected void startWebServer(final String resourceBase, final String[] classpath,
            final Map<String, Class<? extends Servlet>> servlets) throws Exception {
        if (server_ != null) {
            throw new IllegalStateException("startWebServer() can not be called twice");
        }
        server_ = new Server(PORT);

        final WebAppContext context = new WebAppContext();
        context.setContextPath("/");
        context.setResourceBase(resourceBase);

        for (final Map.Entry<String, Class<? extends Servlet>> entry : servlets.entrySet()) {
            final String pathSpec = entry.getKey();
            final Class<? extends Servlet> servlet = entry.getValue();
            context.addServlet(servlet, pathSpec);
        }
        final WebAppClassLoader loader = new WebAppClassLoader(context);
        if (classpath != null) {
            for (final String path : classpath) {
                loader.addClassPath(path);
            }
        }
        context.setClassLoader(loader);
        server_.setHandler(context);
        server_.start();
    }

    /**
     * Performs post-test deconstruction.
     * @throws Exception if an error occurs
     */
    @After
    public void tearDown() throws Exception {
        if (server_ != null) {
            server_.stop();
        }
        server_ = null;
        stopWebServer();
        LAST_TEST_MockWebConnection_ = null;
    }

    /**
     * Defines the provided string as response for the provided URL and loads it using the currently
     * configured browser version. Finally it extracts the captured alerts and verifies them.
     * @param html the HTML to use
     * @param url the URL to use to load the page
     * @return the page
     * @throws Exception if something goes wrong
     */
    protected final HtmlPage loadPageWithAlerts(final String html, final URL url)
        throws Exception {
        return loadPageWithAlerts(html, url, 0);
    }

    /**
     * Same as {@link #loadPageWithAlerts(String, URL)}, but configuring the max wait time.
     * @param html the HTML to use
     * @param url the URL to use to load the page
     * @param maxWaitTime to wait to get the alerts (in ms)
     * @return the page
     * @throws Exception if something goes wrong
     */
    protected final HtmlPage loadPageWithAlerts(final String html, final URL url, final int maxWaitTime)
        throws Exception {
        expandExpectedAlertsVariables(URL_FIRST);

        final String[] expectedAlerts = getExpectedAlerts();
        final HtmlPage page = loadPage(html, url);

        List<String> actualAlerts = getCollectedAlerts(page);
        final long maxWait = System.currentTimeMillis() + maxWaitTime;
        while (actualAlerts.size() < expectedAlerts.length && System.currentTimeMillis() < maxWait) {
            Thread.sleep(30);
            actualAlerts = getCollectedAlerts(page);
        }

        assertEquals(expectedAlerts, getCollectedAlerts(page));
        return page;
    }

    /**
     * Defines the provided string as response for the default URL and loads it using the currently
     * configured browser version.
     * @param html the HTML to use
     * @return the page
     * @throws Exception if something goes wrong
     */
    protected final HtmlPage loadPage(final String html) throws Exception {
        return loadPage(html, getDefaultUrl());
    }

    /**
     * Same as {@link #loadPage(String)}... but defining the default URL.
     * @param html the HTML to use
     * @param url the url to use to load the page
     * @return the page
     * @throws Exception if something goes wrong
     */
    protected final HtmlPage loadPage(final String html, final URL url) throws Exception {
        return loadPage(html, url, "text/html", TextUtil.DEFAULT_CHARSET);
    }

    /**
     * Same as {@link #loadPage(String, URL)}... but defining content type and charset as well.
     * @param html the HTML to use
     * @param url the url to use to load the page
     * @param contentType the content type to return
     * @param charset the name of a supported charset
     * @return the page
     * @throws Exception if something goes wrong
     */
    private HtmlPage loadPage(final String html, final URL url,
            final String contentType, final String charset) throws Exception {
        final MockWebConnection mockWebConnection = getMockWebConnection();
        mockWebConnection.setResponse(url, html, contentType, charset);
        startWebServer(mockWebConnection);

        return getWebClient().getPage(url);
    }

    /**
     * Starts the web server delivering response from the provided connection.
     * @param mockConnection the sources for responses
     * @throws Exception if a problem occurs
     */
    protected void startWebServer(final MockWebConnection mockConnection) throws Exception {
        if (Boolean.FALSE.equals(LAST_TEST_MockWebConnection_)) {
            stopWebServer();
        }
        LAST_TEST_MockWebConnection_ = Boolean.TRUE;
        if (STATIC_SERVER_ == null) {
            STATIC_SERVER_ = new Server(PORT);

            final WebAppContext context = new WebAppContext();
            context.setContextPath("/");
            context.setResourceBase("./");

            if (isBasicAuthentication()) {
                final Constraint constraint = new Constraint();
                constraint.setName(Constraint.__BASIC_AUTH);
                constraint.setRoles(new String[]{"user"});
                constraint.setAuthenticate(true);

                final ConstraintMapping constraintMapping = new ConstraintMapping();
                constraintMapping.setConstraint(constraint);
                constraintMapping.setPathSpec("/*");

                final ConstraintSecurityHandler handler = (ConstraintSecurityHandler) context.getSecurityHandler();
                handler.setLoginService(new HashLoginService("MyRealm", "./src/test/resources/realm.properties"));
                handler.setConstraintMappings(new ConstraintMapping[]{constraintMapping});
            }

            context.addServlet(MockWebConnectionServlet.class, "/*");
            STATIC_SERVER_.setHandler(context);
            STATIC_SERVER_.start();
        }
        MockWebConnectionServlet.setMockconnection(mockConnection);
    }

    /**
     * Stops the WebServer.
     * @throws Exception if it fails
     */
    protected static void stopWebServer() throws Exception {
        if (STATIC_SERVER_ != null) {
            STATIC_SERVER_.stop();
        }
        STATIC_SERVER_ = null;
    }

    /**
     * Loads the provided URL serving responses from {@link #getMockWebConnection()}
     * and verifies that the captured alerts are correct.
     * @param url the URL to use to load the page
     * @return the web driver
     * @throws Exception if something goes wrong
     */
    protected final HtmlPage loadPageWithAlerts(final URL url) throws Exception {
        expandExpectedAlertsVariables(url);
        final String[] expectedAlerts = getExpectedAlerts();

        startWebServer(getMockWebConnection());

        final HtmlPage page = getWebClient().getPage(url);

        assertEquals(expectedAlerts, getCollectedAlerts(page));
        return page;
    }

    /**
     * Returns the collected alerts.
     * @param page the page
     * @return the alerts
     */
    protected List<String> getCollectedAlerts(final HtmlPage page) {
        final ScriptResult result = page.executeJavaScript("top.__huCatchedAlerts;");
        final List<String> list = new ArrayList<>();
        final Object object = result.getJavaScriptResult();
        if (object != Undefined.instance) {
            final NativeArray arr = (NativeArray) object;
            for (int i = 0; i < arr.getLength(); i++) {
                list.add(arr.get(i).toString());
            }
        }
        return list;
    }

    /**
     * Returns whether to use basic authentication for all resources or not.
     * The default implementation returns false.
     * @return whether to use basic authentication or not
     */
    protected boolean isBasicAuthentication() {
        return false;
    }

    /**
     * Returns the WebClient instance for the current test with the current {@link BrowserVersion}.
     * @return a WebClient with the current {@link BrowserVersion}
     */
    protected final WebClient getWebClient() {
        if (webClient_ == null) {
            webClient_ = new WebClient(getBrowserVersion());
        }
        return webClient_;
    }

    /**
     * Cleanup after a test.
     */
    @Override
    @After
    public void releaseResources() {
        super.releaseResources();
        if (webClient_ != null) {
            webClient_.close();
            webClient_.getCookieManager().clearCookies();
        }
        webClient_ = null;
    }
}
