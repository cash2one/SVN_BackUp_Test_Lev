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

import static com.gargoylesoftware.htmlunit.BrowserRunner.Browser.IE;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;

import com.gargoylesoftware.htmlunit.BrowserRunner;
import com.gargoylesoftware.htmlunit.BrowserRunner.Alerts;
import com.gargoylesoftware.htmlunit.BrowserRunner.NotYetImplemented;
import com.gargoylesoftware.htmlunit.WebDriverTestCase;

/**
 * Tests for {@link HtmlFileInput}.
 *
 * @version $Revision: 10952 $
 * @author Ahmed Ashour
 * @author Ronald Brill
 * @author Frank Danek
 */
@RunWith(BrowserRunner.class)
public class HtmlFileInput2Test extends WebDriverTestCase {

    /**
     * @throws Exception if an error occurs
     */
    @Test
    @Alerts(DEFAULT = { "CONTENT_TYPE:application/octet-stream", "charset" },
            IE = { "CONTENT_TYPE:text/plain", "charset" })
    @NotYetImplemented(IE)
    public void contentType() throws Exception {
        final Map<String, Class<? extends Servlet>> servlets = new HashMap<>();
        servlets.put("/upload1", Upload1Servlet.class);
        servlets.put("/upload2", ContentTypeUpload2Servlet.class);
        startWebServer("./", new String[0], servlets);

        final WebDriver driver = getWebDriver();
        driver.get("http://localhost:" + PORT + "/upload1");
        String path = getClass().getClassLoader().getResource("realm.properties").toExternalForm();
        if (driver instanceof InternetExplorerDriver || driver instanceof ChromeDriver) {
            path = path.substring(path.indexOf('/') + 1).replace('/', '\\');
        }
        driver.findElement(By.name("myInput")).sendKeys(path);
        driver.findElement(By.id("mySubmit")).click();

        final String pageSource = driver.getPageSource();
        assertTrue(pageSource, pageSource.contains(getExpectedAlerts()[0]));
        assertFalse(pageSource, pageSource.contains(getExpectedAlerts()[1]));
    }

    /**
     * Servlet for '/upload1'.
     */
    public static class Upload1Servlet extends HttpServlet {

        /**
         * {@inheritDoc}
         */
        @Override
        protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
            response.setCharacterEncoding("UTF-8");
            response.setContentType("text/html");
            response.getWriter().write("<html>"
                + "<body><form action='upload2' method='post' enctype='multipart/form-data'>\n"
                + "Name: <input name='myInput' type='file'><br>\n"
                + "Name 2 (should stay empty): <input name='myInput2' type='file'><br>\n"
                + "<input type='submit' value='Upload' id='mySubmit'>\n"
                + "</form></body></html>\n");
        }
    }

    /**
     * Servlet for '/upload2'.
     */
    public static class ContentTypeUpload2Servlet extends HttpServlet {

        /**
         * {@inheritDoc}
         */
        @Override
        protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
            request.setCharacterEncoding("UTF-8");
            response.setContentType("text/html");
            final Writer writer = response.getWriter();
            if (ServletFileUpload.isMultipartContent(request)) {
                try {
                    final ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory());
                    for (final FileItem item : upload.parseRequest(request)) {
                        if ("myInput".equals(item.getFieldName())) {
                            writer.write("CONTENT_TYPE:" + item.getContentType());
                        }
                    }
                }
                catch (final FileUploadBase.SizeLimitExceededException e) {
                    writer.write("SizeLimitExceeded");
                }
                catch (final Exception e) {
                    writer.write("error");
                }
            }
            writer.close();
        }
    }

    /**
     * Prints request content to the response.
     */
    public static class PrintRequestServlet extends HttpServlet {

        /**
         * {@inheritDoc}
         */
        @Override
        protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
            request.setCharacterEncoding("UTF-8");
            response.setContentType("text/html");
            final Writer writer = response.getWriter();
            final BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line);
            }
            writer.close();
        }
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    public void contentTypeHeader() throws Exception {
        final Map<String, Class<? extends Servlet>> servlets = new HashMap<>();
        servlets.put("/upload1", Upload1Servlet.class);
        servlets.put("/upload2", ContentTypeHeaderUpload2Servlet.class);
        startWebServer("./", new String[0], servlets);

        final WebDriver driver = getWebDriver();
        driver.get("http://localhost:" + PORT + "/upload1");
        String path = getClass().getClassLoader().getResource("realm.properties").toExternalForm();
        if (driver instanceof InternetExplorerDriver || driver instanceof ChromeDriver) {
            path = path.substring(path.indexOf('/') + 1).replace('/', '\\');
        }
        driver.findElement(By.name("myInput")).sendKeys(path);
        driver.findElement(By.id("mySubmit")).click();
        final String source = driver.getPageSource();
        assertTrue(source.contains("CONTENT_TYPE:"));
        assertFalse(source.contains("charset"));
    }

    /**
     * Servlet for '/upload2'.
     */
    public static class ContentTypeHeaderUpload2Servlet extends HttpServlet {

        /**
         * {@inheritDoc}
         */
        @Override
        protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
            request.setCharacterEncoding("UTF-8");
            response.setContentType("text/html");
            final Writer writer = response.getWriter();
            writer.write("CONTENT_TYPE:" + request.getContentType());
            writer.close();
        }
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    @Alerts("Content-Disposition: form-data; name=\"myInput\"; filename=\"\"")
    public void empty() throws Exception {
        final Map<String, Class<? extends Servlet>> servlets = new HashMap<>();
        servlets.put("/upload1", Upload1Servlet.class);
        servlets.put("/upload2", PrintRequestServlet.class);
        startWebServer("./", new String[0], servlets);

        final WebDriver driver = getWebDriver();
        driver.get("http://localhost:" + PORT + "/upload1");
        driver.findElement(By.id("mySubmit")).click();

        String pageSource = driver.getPageSource();
        // hack for selenium
        int count = 0;
        while (count < 100 && StringUtils.isEmpty(pageSource)) {
            pageSource = driver.getPageSource();
            count++;
        }

        final Matcher matcher = Pattern.compile(getExpectedAlerts()[0]).matcher(pageSource);
        assertTrue(pageSource, matcher.find());
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    @Alerts(DEFAULT = "Content-Disposition: form-data; name=\"myInput\"; filename=\"realm.properties\"",
            IE = "Content-Disposition: form-data; name=\"myInput\";"
                        + " filename=\".*test-classes[\\\\/]realm\\.properties\"")
    public void realFile() throws Exception {
        final Map<String, Class<? extends Servlet>> servlets = new HashMap<>();
        servlets.put("/upload1", Upload1Servlet.class);
        servlets.put("/upload2", PrintRequestServlet.class);
        startWebServer("./", new String[0], servlets);

        final WebDriver driver = getWebDriver();
        driver.get("http://localhost:" + PORT + "/upload1");
        String path = getClass().getClassLoader().getResource("realm.properties").toExternalForm();
        if (driver instanceof InternetExplorerDriver || driver instanceof ChromeDriver) {
            path = path.substring(path.indexOf('/') + 1).replace('/', '\\');
        }
        driver.findElement(By.name("myInput")).sendKeys(path);
        driver.findElement(By.id("mySubmit")).click();

        String pageSource = driver.getPageSource();
        // hack for selenium
        int count = 0;
        while (count < 100 && StringUtils.isEmpty(pageSource)) {
            pageSource = driver.getPageSource();
            count++;
        }

        final Matcher matcher = Pattern.compile(getExpectedAlerts()[0]).matcher(pageSource);
        assertTrue(pageSource, matcher.find());
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    public void chunked() throws Exception {
        final Map<String, Class<? extends Servlet>> servlets = new HashMap<>();
        servlets.put("/upload1", Upload1Servlet.class);
        servlets.put("/upload2", ChunkedUpload2Servlet.class);
        startWebServer("./", new String[0], servlets);

        final WebDriver driver = getWebDriver();
        driver.get("http://localhost:" + PORT + "/upload1");
        driver.findElement(By.id("mySubmit")).click();
        assertFalse(driver.getPageSource().contains("chunked"));
    }

    /**
     * Servlet for '/upload2'.
     */
    public static class ChunkedUpload2Servlet extends HttpServlet {
        /**
         * {@inheritDoc}
         */
        @Override
        protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
            request.setCharacterEncoding("UTF-8");
            response.setContentType("text/html");
            final Writer writer = response.getWriter();
            writer.write("TRANSFER_ENCODING:" + request.getHeader("TRANSFER-ENCODING"));
            writer.close();
        }
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts({ "-", "-", "-" })
    public void defaultValues() throws Exception {
        final String html = "<html><head><title>foo</title>\n"
            + "<script>\n"
            + "  function test() {\n"
            + "    var input = document.getElementById('file1');\n"
            + "    alert(input.value + '-' + input.defaultValue);\n"

            + "    input = document.createElement('input');\n"
            + "    input.type = 'file';\n"
            + "    alert(input.value + '-' + input.defaultValue);\n"

            + "    var builder = document.createElement('div');\n"
            + "    builder.innerHTML = '<input type=\"file\">';\n"
            + "    input = builder.firstChild;\n"
            + "    alert(input.value + '-' + input.defaultValue);\n"
            + "  }\n"
            + "</script>\n"
            + "</head><body onload='test()'>\n"
            + "<form>\n"
            + "  <input type='file' id='file1'>\n"
            + "</form>\n"
            + "</body></html>";

        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts({ "-", "-", "-" })
    public void defaultValuesAfterClone() throws Exception {
        final String html = "<html><head><title>foo</title>\n"
            + "<script>\n"
            + "  function test() {\n"
            + "    var input = document.getElementById('file1');\n"
            + "    alert(input.value + '-' + input.defaultValue);\n"
            + "    input = input.cloneNode(false);\n"

            + "    input = document.createElement('input');\n"
            + "    input.type = 'file';\n"
            + "    input = input.cloneNode(false);\n"
            + "    alert(input.value + '-' + input.defaultValue);\n"

            + "    var builder = document.createElement('div');\n"
            + "    builder.innerHTML = '<input type=\"file\">';\n"
            + "    input = builder.firstChild;\n"
            + "    input = input.cloneNode(false);\n"
            + "    alert(input.value + '-' + input.defaultValue);\n"
            + "  }\n"
            + "</script>\n"
            + "</head><body onload='test()'>\n"
            + "<form>\n"
            + "  <input type='file' id='file1'>\n"
            + "</form>\n"
            + "</body></html>";

        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = { "-initial", "-initial", "-newDefault", "-newDefault" },
            IE8 = { "-", "-", "-newDefault", "-newDefault" })
    public void resetByClick() throws Exception {
        final String html = "<html><head><title>foo</title>\n"
            + "<script>\n"
            + "  function test() {\n"
            + "    var file = document.getElementById('testId');\n"
            + "    alert(file.value + '-' + file.defaultValue);\n"

            + "    document.getElementById('testReset').click;\n"
            + "    alert(file.value + '-' + file.defaultValue);\n"

            + "    file.defaultValue = 'newDefault';\n"
            + "    alert(file.value + '-' + file.defaultValue);\n"

            + "    document.forms[0].reset;\n"
            + "    alert(file.value + '-' + file.defaultValue);\n"
            + "  }\n"
            + "</script>\n"
            + "</head><body onload='test()'>\n"
            + "<form>\n"
            + "  <input type='file' id='testId' value='initial'>\n"
            + "  <input type='reset' id='testReset'>\n"
            + "</form>\n"
            + "</body></html>";

        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = { "-initial", "-initial", "-newDefault", "-newDefault" },
            IE8 = { "-", "-", "-newDefault", "-newDefault" })
    public void resetByJS() throws Exception {
        final String html = "<html><head><title>foo</title>\n"
            + "<script>\n"
            + "  function test() {\n"
            + "    var file = document.getElementById('testId');\n"
            + "    alert(file.value + '-' + file.defaultValue);\n"

            + "    document.forms[0].reset;\n"
            + "    alert(file.value + '-' + file.defaultValue);\n"

            + "    file.defaultValue = 'newDefault';\n"
            + "    alert(file.value + '-' + file.defaultValue);\n"

            + "    document.forms[0].reset;\n"
            + "    alert(file.value + '-' + file.defaultValue);\n"
            + "  }\n"
            + "</script>\n"
            + "</head><body onload='test()'>\n"
            + "<form>\n"
            + "  <input type='file' id='testId' value='initial'>\n"
            + "</form>\n"
            + "</body></html>";

        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = { "-initial", "-default" },
            IE8 = { "-", "-default" })
    public void defaultValue() throws Exception {
        final String html = "<!DOCTYPE HTML>\n<html><head><title>foo</title>\n"
            + "<script>\n"
            + "  function test() {\n"
            + "    var file = document.getElementById('testId');\n"
            + "    alert(file.value + '-' + file.defaultValue);\n"

            + "    file.defaultValue = 'default';\n"
            + "    alert(file.value + '-' + file.defaultValue);\n"
            + "  }\n"
            + "</script>\n"
            + "</head><body onload='test()'>\n"
            + "<form>\n"
            + "  <input type='file' id='testId' value='initial'>\n"
            + "</form>\n"
            + "</body></html>";

        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts("changed")
    public void firingOnchange() throws Exception {
        final String html = "<html><body>\n"
            + "<form onchange='alert(\"changed\")'>\n"
            + "  <input type='file' id='file1'>\n"
            + "</form>\n"
            + "</body></html>";

        final WebDriver driver = loadPage2(html);
        final File tmpFile = File.createTempFile("htmlunit-test", ".txt");
        driver.findElement(By.id("file1")).sendKeys(tmpFile.getAbsolutePath());
        tmpFile.delete();
        driver.findElement(By.tagName("body")).click();

        verifyAlerts(driver, getExpectedAlerts());
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts({ "true", "true" })
    public void nonZeroWidthHeight() throws Exception {
        final String html = "<html><head><title>foo</title>\n"
                + "<script>\n"
                + "  function test() {\n"
                + "    var file = document.getElementById('testId');\n"
                + "    alert(file.clientWidth > 2);\n"
                + "    alert(file.clientHeight > 2);\n"
                + "  }\n"
                + "</script>\n"
                + "</head><body onload='test()'>\n"
                + "<form>\n"
                + "  <input type='file' id='testId'>\n"
                + "</form>\n"
                + "</body></html>";
        loadPageWithAlerts2(html);
    }
}
