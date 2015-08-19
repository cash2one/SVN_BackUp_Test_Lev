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

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.htmlunit.corejs.javascript.Context;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.htmlunit.HtmlUnitWebElement;
import org.openqa.selenium.ie.InternetExplorerDriver;

import com.gargoylesoftware.htmlunit.MockWebConnection.RawResponseData;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPageTest;
import com.gargoylesoftware.htmlunit.util.NameValuePair;

/**
 * Base class for tests using WebDriver.
 * <p>
 * By default, this test runs with HtmlUnit, but this behavior can be changed by having a property file named
 * "test.properties" in the HtmlUnit root directory.
 * Sample:
 * <pre>
   browsers=hu,ff38,ie11
   ie.bin=C:\\path\\to\\32bit\\IEDriverServer.exe       [Windows]
   ff38.bin=/usr/bin/firefox                            [Unix-like]
   chrome.bin=/path/to/chromedriver                     [Unix-like]
   autofix=true
 * </pre>
 * The file should contain four properties: "browsers", "ie.bin", "ff38.bin", and "chrome.bin".
 * <ul>
 *   <li>browsers: is a comma separated list contains any combination of "hu" (for HtmlUnit with all browser versions),
 *   "hu-ie8", "hu-ff31",
 *   "ff31", "ff38", "ie8", "ie11", "chrome", which will be used to driver real browsers,
 *   note that you can't define more than one IE as there is no standard way
 *   to have multiple IEs on the same machine</li>
 *   <li>ie.bin (mandatory if it does not exist in the <i>path</i>): is the location of the IEDriverServer binary (see
 *   <a href="http://selenium-release.storage.googleapis.com/index.html">IEDriverServer downloads</a>)</li>
 *   <li>ff38.bin (optional): is the location of the FF binary, in Windows use double back-slashes</li>
 *   <li>chrome.bin (mandatory if it does not exist in the <i>path</i>): is the location of the ChromeDriver binary (see
 *   <a href="http://chromedriver.storage.googleapis.com/index.html">Chrome Driver downloads</a>)</li>
 *   <li>autofix (optional): if {@code true}, try to automatically fix the real browser expectations,
 *   or add/remove {@code @NotYetImplemented} annotations, use with caution!</li>
 * </ul>
 * </p>
 *
 * @version $Revision: 10944 $
 * @author Marc Guillemot
 * @author Ahmed Ashour
 * @author Ronald Brill
 * @author Frank Danek
 */
public abstract class WebDriverTestCase extends WebTestCase {

    /**
     * The system property for automatically fixing the test case expectations.
     */
    public static final String AUTOFIX_ = "htmlunit.autofix";

    private static final Log LOG = LogFactory.getLog(WebDriverTestCase.class);

    private static Set<String> BROWSERS_PROPERTIES_;
    private static String IE_BIN_;
    private static String FF31_BIN_;
    private static String FF38_BIN_;
    private static String CHROME_BIN_;

    /** The driver cache. */
    protected static final Map<BrowserVersion, WebDriver> WEB_DRIVERS_ = new HashMap<>();
    private static Server STATIC_SERVER_;
    // second server for cross-origin tests.
    private static Server STATIC_SERVER2_;
    // third server for multi-origin cross-origin tests.
    private static Server STATIC_SERVER3_;
    private static ChromeDriverService CHROME_SERVICE_;

    private boolean useRealBrowser_;
    private Boolean useStandards_;
    private static Boolean LAST_TEST_MockWebConnection_;

    private WebClient webClient_;

    /**
     * Override this function in a test class to ask for STATIC_SERVER2_ to be set up.
     * @return true if two servers are needed.
     */
    protected boolean needThreeConnections() {
        return false;
    }

    static Set<String> getBrowsersProperties() {
        if (BROWSERS_PROPERTIES_ == null) {
            try {
                final Properties properties = new Properties();
                final File file = new File("test.properties");
                if (file.exists()) {
                    properties.load(new FileInputStream(file));
                    String browsersValue = properties.getProperty("browsers");
                    if (browsersValue == null || browsersValue.isEmpty()) {
                        browsersValue = "hu";
                    }
                    BROWSERS_PROPERTIES_
                        = new HashSet<>(Arrays.asList(browsersValue.replaceAll(" ", "").toLowerCase().split(",")));
                    CHROME_BIN_ = properties.getProperty("chrome.bin");
                    FF31_BIN_ = properties.getProperty("ff31.bin");
                    FF38_BIN_ = properties.getProperty("ff38.bin");
                    IE_BIN_ = properties.getProperty("ie.bin");

                    final boolean autofix = Boolean.parseBoolean(properties.getProperty("autofix"));
                    System.setProperty(AUTOFIX_, Boolean.toString(autofix));
                }
            }
            catch (final Exception e) {
                LOG.error("Error reading htmlunit.properties. Ignoring!", e);
            }
            if (BROWSERS_PROPERTIES_ == null) {
                BROWSERS_PROPERTIES_ = new HashSet<>(Arrays.asList("hu"));
            }
            if (BROWSERS_PROPERTIES_.contains("hu")) {
                BROWSERS_PROPERTIES_.add("hu-chrome");
                BROWSERS_PROPERTIES_.add("hu-ff31");
                BROWSERS_PROPERTIES_.add("hu-ff38");
                BROWSERS_PROPERTIES_.add("hu-ie8");
                BROWSERS_PROPERTIES_.add("hu-ie11");
            }
        }
        return BROWSERS_PROPERTIES_;
    }

    /**
     * Configure the driver only once.
     * @return the driver
     */
    protected WebDriver getWebDriver() {
        final BrowserVersion browserVersion = getBrowserVersion();
        WebDriver driver = WEB_DRIVERS_.get(browserVersion);
        if (driver == null) {
            try {
                driver = buildWebDriver();
            }
            catch (final IOException e) {
                throw new RuntimeException(e);
            }

            if (isWebClientCached() || !(driver instanceof HtmlUnitDriver)) {
                WEB_DRIVERS_.put(browserVersion, driver);
            }
        }
        return driver;
    }

    /**
     * Closes the drivers.
     * @throws Exception If an error occurs
     */
    @AfterClass
    public static void shutDownAll() throws Exception {
        for (WebDriver driver : WEB_DRIVERS_.values()) {
            driver.quit();
        }
        WEB_DRIVERS_.clear();
        stopWebServers();
        LAST_TEST_MockWebConnection_ = null;
    }

    /**
     * Stops all WebServers.
     * @throws Exception if it fails
     */
    protected static void stopWebServers() throws Exception {
        if (STATIC_SERVER_ != null) {
            STATIC_SERVER_.stop();
        }
        if (STATIC_SERVER2_ != null) {
            STATIC_SERVER2_.stop();
        }
        if (STATIC_SERVER3_ != null) {
            STATIC_SERVER3_.stop();
        }
        STATIC_SERVER_ = null;
        STATIC_SERVER2_ = null;
        STATIC_SERVER3_ = null;
    }

    /**
     * Sets whether to use real browser or not.
     * @param useRealBrowser whether to use real browser or not
     */
    public void setUseRealBrowser(final boolean useRealBrowser) {
        useRealBrowser_ = useRealBrowser;
    }

    /**
     * Sets whether to use {@code Standards Mode} or not.
     * @param useStandards whether to use {@code Standards Mode} or not
     */
    public void setUseStandards(final boolean useStandards) {
        useStandards_ = useStandards;
    }

    /**
     * Builds a new WebDriver instance.
     * @return the instance
     * @throws IOException in case of exception
     */
    protected WebDriver buildWebDriver() throws IOException {
        if (useRealBrowser_) {
            if (getBrowserVersion().isIE()) {
                if (null != IE_BIN_) {
                    System.setProperty("webdriver.ie.driver", IE_BIN_);
                }
                return new InternetExplorerDriver();
            }
            if (BrowserVersion.CHROME == getBrowserVersion()) {
                if (CHROME_SERVICE_ == null) {
                    final ChromeDriverService.Builder builder = new ChromeDriverService.Builder();
                    if (CHROME_BIN_ != null) {
                        builder.usingDriverExecutable(new File(CHROME_BIN_));
                    }
                    CHROME_SERVICE_ = builder
                        .usingAnyFreePort()
                        .build();

                    CHROME_SERVICE_.start();
                }
                return new ChromeDriver(CHROME_SERVICE_);
            }
            if (!getBrowserVersion().isFirefox()) {
                throw new RuntimeException("Unexpected BrowserVersion: " + getBrowserVersion());
            }

            String ffBinary = null;
            if (BrowserVersion.FIREFOX_31 == getBrowserVersion()) {
                ffBinary = FF31_BIN_;
            }
            if (BrowserVersion.FIREFOX_38 == getBrowserVersion()) {
                ffBinary = FF38_BIN_;
            }
            if (ffBinary != null) {
                return new FirefoxDriver(new FirefoxBinary(new File(ffBinary)), new FirefoxProfile());
            }
            return new FirefoxDriver();
        }
        if (webClient_ == null) {
            webClient_ = new WebClient(getBrowserVersion());
        }
        return new HtmlUnitDriver(true) {
            @Override
            protected WebClient newWebClient(final BrowserVersion browserVersion) {
                return webClient_;
            }

            @Override
            protected WebElement newHtmlUnitWebElement(final DomElement element) {
                return new FixedWebDriverHtmlUnitWebElement(this, element);
            }
        };
    }

    /**
     * Starts the web server delivering response from the provided connection.
     * @param mockConnection the sources for responses
     * @throws Exception if a problem occurs
     */
    protected void startWebServer(final MockWebConnection mockConnection) throws Exception {
        if (Boolean.FALSE.equals(LAST_TEST_MockWebConnection_)) {
            stopWebServers();
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
        MockWebConnectionServlet.MockConnection_ = mockConnection;

        if (STATIC_SERVER2_ == null && needThreeConnections()) {
            STATIC_SERVER2_ = new Server(PORT2);
            final WebAppContext context2 = new WebAppContext();
            context2.setContextPath("/");
            context2.setResourceBase("./");
            context2.addServlet(MockWebConnectionServlet.class, "/*");
            STATIC_SERVER2_.setHandler(context2);
            STATIC_SERVER2_.start();

            STATIC_SERVER3_ = new Server(PORT3);
            final WebAppContext context3 = new WebAppContext();
            context3.setContextPath("/");
            context3.setResourceBase("./");
            context3.addServlet(MockWebConnectionServlet.class, "/*");
            STATIC_SERVER3_.setHandler(context3);
            STATIC_SERVER3_.start();
            /*
             * The mock connection servlet call sit under both servers, so long as tests
             * keep the URLs distinct.
             */
        }
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
        startWebServer(resourceBase, classpath, servlets, null);
    }

    /**
     * Starts the <b>second</b> web server on the default {@link #PORT2}.
     * The given resourceBase is used to be the ROOT directory that serves the default context.
     * <p><b>Don't forget to stop the returned HttpServer after the test</b>
     *
     * @param resourceBase the base of resources for the default context
     * @param classpath additional classpath entries to add (may be null)
     * @param servlets map of {String, Class} pairs: String is the path spec, while class is the class
     * @throws Exception if the test fails
     */
    protected void startWebServer2(final String resourceBase, final String[] classpath,
            final Map<String, Class<? extends Servlet>> servlets) throws Exception {

        if (STATIC_SERVER2_ != null) {
            STATIC_SERVER2_.stop();
        }
        STATIC_SERVER2_ = WebServerTestCase.createWebServer(PORT2, resourceBase, classpath, servlets, null);
    }

    /**
     * Starts the web server on the default {@link #PORT}.
     * The given resourceBase is used to be the ROOT directory that serves the default context.
     * <p><b>Don't forget to stop the returned Server after the test</b>
     *
     * @param resourceBase the base of resources for the default context
     * @param classpath additional classpath entries to add (may be null)
     * @param servlets map of {String, Class} pairs: String is the path spec, while class is the class
     * @param handler wrapper for handler (can be null)
     * @throws Exception if the test fails
     */
    protected void startWebServer(final String resourceBase, final String[] classpath,
            final Map<String, Class<? extends Servlet>> servlets, final HandlerWrapper handler) throws Exception {
        stopWebServers();
        LAST_TEST_MockWebConnection_ = Boolean.FALSE;

        STATIC_SERVER_ = WebServerTestCase.createWebServer(PORT, resourceBase, classpath, servlets, handler);
    }

    /**
     * Servlet delivering content from a MockWebConnection.
     */
    public static class MockWebConnectionServlet extends HttpServlet {
        private static MockWebConnection MockConnection_;

        static void setMockconnection(final MockWebConnection connection) {
            MockConnection_ = connection;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void service(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {

            try {
                doService(request, response);
            }
            catch (final ServletException e) {
                throw e;
            }
            catch (final IOException e) {
                throw e;
            }
            catch (final Exception e) {
                throw new ServletException(e);
            }
        }

        private void doService(final HttpServletRequest request, final HttpServletResponse response)
            throws Exception {
            String url = request.getRequestURL().toString();
            if (LOG.isDebugEnabled()) {
                LOG.debug(request.getMethod() + " " + url);
            }

            if (url.endsWith("/favicon.ico")) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            if (url.contains("/delay")) {
                final String delay = StringUtils.substringBetween(url, "/delay", "/");
                final int ms = Integer.parseInt(delay);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Sleeping for " + ms + " before to deliver " + url);
                }
                Thread.sleep(ms);
            }

            // copy parameters
            final List<NameValuePair> requestParameters = new ArrayList<>();
            try {
                for (final Enumeration<String> paramNames = request.getParameterNames();
                        paramNames.hasMoreElements();) {
                    final String name = paramNames.nextElement();
                    final String[] values = request.getParameterValues(name);
                    for (final String value : values) {
                        requestParameters.add(new NameValuePair(name, value));
                    }
                }
            }
            catch (final IllegalArgumentException e) {
                // Jetty 8.1.7 throws it in getParameterNames for a query like "cb=%%RANDOM_NUMBER%%"
                // => we should use a more low level test server
                requestParameters.clear();
                final String query = request.getQueryString();
                if (query != null) {
                    url += "?" + query;
                }
            }

            final URL requestedUrl = new URL(url);
            final WebRequest webRequest = new WebRequest(requestedUrl);
            webRequest.setHttpMethod(HttpMethod.valueOf(request.getMethod()));

            // copy headers
            for (final Enumeration<String> en = request.getHeaderNames(); en.hasMoreElements();) {
                final String headerName = en.nextElement();
                final String headerValue = request.getHeader(headerName);
                webRequest.setAdditionalHeader(headerName, headerValue);
            }

            if (requestParameters.isEmpty() && request.getContentLength() > 0) {
                final byte[] buffer = new byte[request.getContentLength()];
                request.getInputStream().read(buffer, 0, buffer.length);
                webRequest.setRequestBody(new String(buffer, webRequest.getCharset()));
            }
            else {
                webRequest.setRequestParameters(requestParameters);
            }

            final RawResponseData resp = MockConnection_.getRawResponse(webRequest);

            // write WebResponse to HttpServletResponse
            response.setStatus(resp.getStatusCode());

            boolean charsetInContentType = false;
            for (final NameValuePair responseHeader : resp.getHeaders()) {
                final String headerName = responseHeader.getName();
                if ("Content-Type".equals(headerName) && responseHeader.getValue().contains("charset=")) {
                    charsetInContentType = true;
                }
                response.addHeader(headerName, responseHeader.getValue());
            }

            if (resp.getByteContent() != null) {
                response.getOutputStream().write(resp.getByteContent());
            }
            else {
                final String newContent = getModifiedContent(resp.getStringContent());
                if (!charsetInContentType) {
                    response.setCharacterEncoding(resp.getCharset());
                }
                response.getWriter().print(newContent);
            }
            response.flushBuffer();
        }
    }

    /**
     * Same as {@link #loadPageWithAlerts2(String)}... but doesn't verify the alerts.
     * @param html the HTML to use
     * @return the web driver
     * @throws Exception if something goes wrong
     */
    protected final WebDriver loadPage2(final String html) throws Exception {
        return loadPage2(html, URL_FIRST);
    }

    /**
     * Same as {@link #loadPageWithAlerts2(String)}... but doesn't verify the alerts.
     * @param html the HTML to use
     * @param url the url to use to load the page
     * @return the web driver
     * @throws Exception if something goes wrong
     */
    protected final WebDriver loadPage2(final String html, final URL url) throws Exception {
        return loadPage2(html, url, "text/html;charset=ISO-8859-1", TextUtil.DEFAULT_CHARSET);
    }

    /**
     * Same as {@link #loadPageWithAlerts2(String)}... but doesn't verify the alerts.
     * @param html the HTML to use
     * @param url the url to use to load the page
     * @param contentType the content type to return
     * @param charset the name of a supported charset
     * @return the web driver
     * @throws Exception if something goes wrong
     */
    protected final WebDriver loadPage2(String html, final URL url,
            final String contentType, final String charset) throws Exception {
        if (useStandards_ != null) {
            if (html.startsWith(HtmlPageTest.STANDARDS_MODE_PREFIX_)) {
                fail("HTML must not be prefixed with Standards Mode.");
            }
            if (useStandards_) {
                html = HtmlPageTest.STANDARDS_MODE_PREFIX_ + html;
            }
        }
        final MockWebConnection mockWebConnection = getMockWebConnection();
        mockWebConnection.setResponse(url, html, contentType, charset);
        startWebServer(mockWebConnection);

        final WebDriver driver = getWebDriver();
        if (!(driver instanceof HtmlUnitDriver)) {
            try {
                driver.manage().window().setSize(new Dimension(1272, 768));
            }
            catch (final WebDriverException e) {
                // ChromeDriver version 0.5 (Mar 26, 2013) does not support the setSize command
                LOG.warn(e.getMessage(), e);
            }
        }
        driver.get(url.toExternalForm());

        return driver;
    }

    /**
     * Same as {@link #loadPage2(String)} with additional servlet configuration.
     * @param html the HTML to use for the default response
     * @param servlets the additional servlets to configure with their mapping
     * @return the web driver
     * @throws Exception if something goes wrong
     */
    protected final WebDriver loadPage2(final String html,
            final Map<String, Class<? extends Servlet>> servlets) throws Exception {
        return loadPage2(html, getDefaultUrl(), servlets);
    }

    /**
     * Same as {@link #loadPage2(String, URL)}, but with additional servlet configuration.
     * @param html the HTML to use for the default page
     * @param url the URL to use to load the page
     * @param servlets the additional servlets to configure with their mapping
     * @return the web driver
     * @throws Exception if something goes wrong
     */
    protected final WebDriver loadPage2(final String html, final URL url,
            final Map<String, Class<? extends Servlet>> servlets) throws Exception {

        servlets.put("/*", MockWebConnectionServlet.class);
        getMockWebConnection().setResponse(url, html);
        MockWebConnectionServlet.MockConnection_ = getMockWebConnection();

        startWebServer("./", null, servlets);

        final WebDriver driver = getWebDriver();
        driver.get(url.toExternalForm());

        return driver;
    }

    /**
     * Defines the provided HTML as the response for {@link #getDefaultUrl()}
     * and loads the page with this URL using the current WebDriver version; finally, asserts that the
     * alerts equal the expected alerts (in which "§§URL§§" has been expanded to the default URL).
     * @param html the HTML to use
     * @return the web driver
     * @throws Exception if something goes wrong
     */
    protected final WebDriver loadPageWithAlerts2(final String html) throws Exception {
        return loadPageWithAlerts2(html, URL_FIRST, DEFAULT_WAIT_TIME);
    }

    /**
     * Defines the provided HTML as the response for {@link #getDefaultUrl()}
     * and loads the page with this URL using the current WebDriver version; finally, asserts that the
     * alerts equal the expected alerts (in which "§§URL§§" has been expanded to the default URL).
     * @param html the HTML to use
     * @param maxWaitTime the maximum time to wait to get the alerts (in millis)
     * @return the web driver
     * @throws Exception if something goes wrong
     */
    protected final WebDriver loadPageWithAlerts2(final String html, final long maxWaitTime) throws Exception {
        return loadPageWithAlerts2(html, URL_FIRST, maxWaitTime);
    }

    /**
     * Same as {@link #loadPageWithAlerts2(String)}, but specifying the default URL.
     * @param html the HTML to use
     * @param url the URL to use to load the page
     * @return the web driver
     * @throws Exception if something goes wrong
     */
    protected final WebDriver loadPageWithAlerts2(final String html, final URL url) throws Exception {
        return loadPageWithAlerts2(html, url, DEFAULT_WAIT_TIME);
    }

    /**
     * Same as {@link #loadPageWithAlerts2(String, long)}, but specifying the default URL.
     * @param html the HTML to use
     * @param url the URL to use to load the page
     * @param maxWaitTime the maximum time to wait to get the alerts (in millis)
     * @return the web driver
     * @throws Exception if something goes wrong
     */
    protected final WebDriver loadPageWithAlerts2(final String html, final URL url, final long maxWaitTime)
        throws Exception {
        expandExpectedAlertsVariables(URL_FIRST);
        final String[] expectedAlerts = getExpectedAlerts();

        final WebDriver driver = loadPage2(html, url);

        verifyAlerts(maxWaitTime, driver, expectedAlerts);
        return driver;
    }

    /**
     * Verifies the captured alerts.
     * @param driver the driver instance
     * @param expectedAlerts the expected alerts
     * @throws Exception in case of failure
     */
    protected void verifyAlerts(final WebDriver driver, final String... expectedAlerts) throws Exception {
        verifyAlerts(DEFAULT_WAIT_TIME, driver, expectedAlerts);
    }

    /**
     * Verifies the captured alerts.
     * @param maxWaitTime the maximum time to wait for the expected alert to be found
     * @param driver the driver instance
     * @param expectedAlerts the expected alerts
     * @throws Exception in case of failure
     */
    protected void verifyAlerts(final long maxWaitTime, final WebDriver driver, final String... expectedAlerts)
        throws Exception {
        // gets the collected alerts, waiting a bit if necessary
        List<String> actualAlerts = getCollectedAlerts(driver);
        final long maxWait = System.currentTimeMillis() + maxWaitTime;
        while (actualAlerts.size() < expectedAlerts.length && System.currentTimeMillis() < maxWait) {
            Thread.sleep(30);
            actualAlerts = getCollectedAlerts(driver);
        }

        assertEquals(expectedAlerts, actualAlerts);
    }

    /**
     * Same as {@link #loadPageWithAlerts2(String)} with additional servlet configuration.
     * @param html the HTML to use for the default response
     * @param servlets the additional servlets to configure with their mapping
     * @return the web driver
     * @throws Exception if something goes wrong
     */
    protected final WebDriver loadPageWithAlerts2(final String html,
            final Map<String, Class<? extends Servlet>> servlets) throws Exception {
        return loadPageWithAlerts2(html, getDefaultUrl(), DEFAULT_WAIT_TIME, servlets);
    }

    /**
     * Same as {@link #loadPageWithAlerts2(String, URL, long)}, but with additional servlet configuration.
     * @param html the HTML to use for the default page
     * @param url the URL to use to load the page
     * @param maxWaitTime the maximum time to wait to get the alerts (in millis)
     * @param servlets the additional servlets to configure with their mapping
     * @return the web driver
     * @throws Exception if something goes wrong
     */
    protected final WebDriver loadPageWithAlerts2(final String html, final URL url, final long maxWaitTime,
            final Map<String, Class<? extends Servlet>> servlets) throws Exception {

        expandExpectedAlertsVariables(getDefaultUrl());
        final String[] expectedAlerts = getExpectedAlerts();

        final WebDriver driver = loadPage2(html, url, servlets);
        verifyAlerts(maxWaitTime, driver, expectedAlerts);

        return driver;
    }

    /**
     * Loads the provided URL serving responses from {@link #getMockWebConnection()}
     * and verifies that the captured alerts are correct.
     * @param url the URL to use to load the page
     * @return the web driver
     * @throws Exception if something goes wrong
     */
    protected final WebDriver loadPageWithAlerts2(final URL url) throws Exception {
        return loadPageWithAlerts2(url, 0);
    }

    /**
     * Same as {@link #loadPageWithAlerts2(URL)}, but using with timeout.
     * @param url the URL to use to load the page
     * @param maxWaitTime the maximum time to wait to get the alerts (in millis)
     * @return the web driver
     * @throws Exception if something goes wrong
     */
    protected final WebDriver loadPageWithAlerts2(final URL url, final long maxWaitTime) throws Exception {
        expandExpectedAlertsVariables(url);
        final String[] expectedAlerts = getExpectedAlerts();

        startWebServer(getMockWebConnection());

        final WebDriver driver = getWebDriver();
        driver.get(url.toExternalForm());

        verifyAlerts(maxWaitTime, driver, expectedAlerts);
        return driver;
    }

    /**
     * Gets the alerts collected by the driver.
     * Note: it currently works only if no new page has been loaded in the window
     * @param driver the driver
     * @return the collected alerts
     * @throws Exception in case of problem
     */
    @SuppressWarnings("unchecked")
    protected List<String> getCollectedAlerts(final WebDriver driver) throws Exception {
        final List<String> collectedAlerts = new ArrayList<>();
        final JavascriptExecutor jsExecutor = (JavascriptExecutor) driver;

        final Object result = jsExecutor.executeScript("return top.__huCatchedAlerts");

        if (result != null) {
            if (driver instanceof HtmlUnitDriver) {
                return (List<String>) result;
            }
            for (final Object alert : (List<Object>) result) {
                collectedAlerts.add(Context.toString(alert));
            }
        }
        return collectedAlerts;
    }

    /**
     * Returns the HtmlElement of the specified WebElement.
     * @param webElement the webElement
     * @return the HtmlElement
     * @see #getWebWindowOf(HtmlUnitDriver)
     */
    protected HtmlElement toHtmlElement(final WebElement webElement) {
        try {
            final Field field = HtmlUnitWebElement.class.getDeclaredField("element");
            field.setAccessible(true);
            return (HtmlElement) field.get(webElement);
        }
        catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Reads the number of JS threads remaining from unit tests run before.
     * This should be always 0, if {@link #isWebClientCached()} is {@code false}.
     */
    @Before
    public void before() {
        if (!isWebClientCached()) {
            assertEquals(0, getJavaScriptThreads().size());
        }
    }

    /**
     * Release resources but DON'T close the browser if we are running with a real browser.
     * Note that HtmlUnitDriver is not cached by default, but that can be configured by {@link #isWebClientCached()}.
     */
    @After
    @Override
    public void releaseResources() {
        super.releaseResources();

        if (!isWebClientCached()) {
            if (webClient_ != null) {
                webClient_.close();
                webClient_.getCookieManager().clearCookies();
            }
            webClient_ = null;
            assertEquals(0, getJavaScriptThreads().size());
        }

        if (useRealBrowser_) {
            final WebDriver driver = getWebDriver();
            final String currentWindow = driver.getWindowHandle();

            // close all windows except the current one
            for (final String handle : driver.getWindowHandles()) {
                if (!currentWindow.equals(handle)) {
                    driver.switchTo().window(handle).close();
                }
            }

            driver.switchTo().window(currentWindow);

            driver.manage().deleteAllCookies();

            // in the remaining window, load a blank page
            driver.get("about:blank");
        }
    }

    /**
     * Returns the underlying WebWindow of the specified driver.
     *
     * <b>Your test shouldn't depend primarily on WebClient</b>
     *
     * @param driver the driver
     * @return the current web window
     * @throws Exception if an error occurs
     * @see #toHtmlElement(WebElement)
     */
    protected WebWindow getWebWindowOf(final HtmlUnitDriver driver) throws Exception {
        final Field field = HtmlUnitDriver.class.getDeclaredField("currentWindow");
        field.setAccessible(true);
        return (WebWindow) field.get(driver);
    }

    /**
     * Whether {@link WebClient} is cached or not, defaults to {@code false}.
     *
     * <p>This is needed to be {@code true} for huge test class, as we could run out of sockets.
     *
     * @return whether {@link WebClient} is cached or not
     */
    protected boolean isWebClientCached() {
        return false;
    }
}

/**
 * As HtmlUnit didn't generate the right events, WebDriver did it for us, but now that we do it correctly,
 * WebDriver shouldn't do it anymore
 * http://code.google.com/p/webdriver/issues/detail?id=93
 */
class FixedWebDriverHtmlUnitWebElement extends HtmlUnitWebElement {

    public FixedWebDriverHtmlUnitWebElement(final HtmlUnitDriver parent, final DomElement element) {
        super(parent, element);
    }

    @Override
    public void click() {
        try {
            getElement().click();
        }
        catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getText() {
        String text = getElement().asText();
        text = text.replace('\t', ' ');
        text = text.replace("\r", "");
        return text;
    }
}
