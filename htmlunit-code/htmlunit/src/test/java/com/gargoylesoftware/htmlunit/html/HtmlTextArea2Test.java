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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.gargoylesoftware.htmlunit.BrowserRunner;
import com.gargoylesoftware.htmlunit.BrowserRunner.Alerts;
import com.gargoylesoftware.htmlunit.WebDriverTestCase;

/**
 * Tests for {@link HtmlTextInput}.
 *
 * @version $Revision: 10927 $
 * @author Ronald Brill
 */
@RunWith(BrowserRunner.class)
public class HtmlTextArea2Test extends WebDriverTestCase {

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts({ "-", "-", "newValue-", "newValue-", "newValue-newDefault", "newValue-newDefault" })
    public void resetByClick() throws Exception {
        final String html = "<html><head><title>foo</title>\n"
            + "<script>\n"
            + "  function test() {\n"
            + "    var text = document.getElementById('testId');\n"
            + "    alert(text.value + '-' + text.defaultValue);\n"

            + "    document.getElementById('testReset').click;\n"
            + "    alert(text.value + '-' + text.defaultValue);\n"

            + "    text.value = 'newValue';\n"
            + "    alert(text.value + '-' + text.defaultValue);\n"

            + "    document.getElementById('testReset').click;\n"
            + "    alert(text.value + '-' + text.defaultValue);\n"

            + "    text.defaultValue = 'newDefault';\n"
            + "    alert(text.value + '-' + text.defaultValue);\n"

            + "    document.forms[0].reset;\n"
            + "    alert(text.value + '-' + text.defaultValue);\n"
            + "  }\n"
            + "</script>\n"
            + "</head><body onload='test()'>\n"
            + "<form>\n"
            + "  <textarea id='testId' value='initial'></textarea>\n"
            + "  <input type='reset' id='testReset'>\n"
            + "</form>\n"
            + "</body></html>";

        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts({ "-", "-", "newValue-", "newValue-", "newValue-newDefault", "newValue-newDefault" })
    public void resetByJS() throws Exception {
        final String html = "<html><head><title>foo</title>\n"
            + "<script>\n"
            + "  function test() {\n"
            + "    var text = document.getElementById('testId');\n"
            + "    alert(text.value + '-' + text.defaultValue);\n"

            + "    document.forms[0].reset;\n"
            + "    alert(text.value + '-' + text.defaultValue);\n"

            + "    text.value = 'newValue';\n"
            + "    alert(text.value + '-' + text.defaultValue);\n"

            + "    document.forms[0].reset;\n"
            + "    alert(text.value + '-' + text.defaultValue);\n"

            + "    text.defaultValue = 'newDefault';\n"
            + "    alert(text.value + '-' + text.defaultValue);\n"

            + "    document.forms[0].reset;\n"
            + "    alert(text.value + '-' + text.defaultValue);\n"
            + "  }\n"
            + "</script>\n"
            + "</head><body onload='test()'>\n"
            + "<form>\n"
            + "  <textarea id='testId' value='initial'></textarea>\n"
            + "</form>\n"
            + "</body></html>";

        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = { "-", "default-default", "some text-default", "some text-newdefault" },
            IE = { "-", "-default", "some text-default", "some text-newdefault" })
    public void defaultValue() throws Exception {
        final String html = "<!DOCTYPE HTML>\n<html><head><title>foo</title>\n"
            + "<script>\n"
            + "  function test() {\n"
            + "    var text = document.getElementById('testId');\n"
            + "    alert(text.value + '-' + text.defaultValue);\n"

            + "    text.defaultValue = 'default';\n"
            + "    alert(text.value + '-' + text.defaultValue);\n"

            + "    text.value = 'some text';\n"
            + "    alert(text.value + '-' + text.defaultValue);\n"
            + "    text.defaultValue = 'newdefault';\n"
            + "    alert(text.value + '-' + text.defaultValue);\n"
            + "  }\n"
            + "</script>\n"
            + "</head><body onload='test()'>\n"
            + "<form>\n"
            + "  <textarea id='testId' value='initial'></textarea>\n"
            + "</form>\n"
            + "</body></html>";

        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = { "7" },
            IE = { "textLength not available" })
    public void textLength() throws Exception {
        final String html = "<html><head><title>foo</title>\n"
            + "<script>\n"
            + "  function test() {\n"
            + "    var text = document.getElementById('testId');\n"
            + "    if(text.textLength) {\n"
            + "      alert(text.textLength);\n"
            + "    } else {\n"
            + "      alert('textLength not available');\n"
            + "    }\n"
            + "  }\n"
            + "</script>\n"
            + "</head><body onload='test()'>\n"
            + "<form>\n"
            + "  <textarea id='testId'>initial</textarea>\n"
            + "</form>\n"
            + "</body></html>";

        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    @Alerts("0")
    public void selection() throws Exception {
        final String html =
              "<html><head><script>\n"
            + "  function test() {\n"
            + "    alert(getSelection(document.getElementById('text1')).length);\n"
            + "  }\n"
            + "  function getSelection(element) {\n"
            + "    if (typeof element.selectionStart == 'number') {\n"
            + "      return element.value.substring(element.selectionStart, element.selectionEnd);\n"
            + "    } else if (document.selection && document.selection.createRange) {\n"
            + "      return document.selection.createRange().text;\n"
            + "    }\n"
            + "  }\n"
            + "</script></head>\n"
            + "<body onload='test()'>\n"
            + "  <textarea id='text1'></textarea>\n"
            + "</body></html>";
        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if test fails
     */
    @Test
    @Alerts(DEFAULT = { "0,0", "11,11", "3,11", "3,10" },
            IE11 = { "0,0", "0,0", "3,3", "3,10" },
            IE8 = { "undefined,undefined", "undefined,undefined", "3,undefined", "3,10" })
    public void selection2_1() throws Exception {
        selection2(3, 10);
    }

    /**
     * @throws Exception if test fails
     */
    @Test
    @Alerts(DEFAULT = { "0,0", "11,11", "0,11", "0,11" },
            IE11 = { "0,0", "0,0", "0,0", "0,11" },
            IE8 = { "undefined,undefined", "undefined,undefined", "-3,undefined", "-3,15" })
    public void selection2_2() throws Exception {
        selection2(-3, 15);
    }

    /**
     * @throws Exception if test fails
     */
    @Test
    @Alerts(DEFAULT = { "0,0", "11,11", "10,11", "5,5" },
            IE = { "undefined,undefined", "undefined,undefined", "10,undefined", "10,5" },
            IE11 = { "0,0", "0,0", "10,10", "5,5" })
    public void selection2_3() throws Exception {
        selection2(10, 5);
    }

    private void selection2(final int selectionStart, final int selectionEnd) throws Exception {
        final String html = "<html>\n"
            + "<body>\n"
            + "<textarea id='myTextInput'>Bonjour</textarea>\n"
            + "<script>\n"
            + "    var input = document.getElementById('myTextInput');\n"
            + "    alert(input.selectionStart + ',' + input.selectionEnd);\n"
            + "    input.value = 'Hello there';\n"
            + "    alert(input.selectionStart + ',' + input.selectionEnd);\n"
            + "    input.selectionStart = " + selectionStart + ";\n"
            + "    alert(input.selectionStart + ',' + input.selectionEnd);\n"
            + "    input.selectionEnd = " + selectionEnd + ";\n"
            + "    alert(input.selectionStart + ',' + input.selectionEnd);\n"
            + "</script>\n"
            + "</body>\n"
            + "</html>";
        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if test fails
     */
    @Test
    @Alerts(DEFAULT = { "0,0", "4,5", "10,10", "4,4", "1,1" },
            IE11 = { "0,0", "4,5", "0,0", "0,0", "0,0" },
            IE8 = { "undefined,undefined", "4,5", "4,5", "4,5", "0,4" })
    public void selectionOnUpdate() throws Exception {
        final String html = "<html>\n"
            + "<body>\n"
            + "<textarea id='myTextInput'>Hello</textarea>\n"
            + "<script>\n"
            + "    var input = document.getElementById('myTextInput');\n"
            + "    alert(input.selectionStart + ',' + input.selectionEnd);\n"

            + "    input.selectionStart = 4;\n"
            + "    input.selectionEnd = 5;\n"
            + "    alert(input.selectionStart + ',' + input.selectionEnd);\n"
            + "    input.value = 'abcdefghif';\n"
            + "    alert(input.selectionStart + ',' + input.selectionEnd);\n"

            + "    input.value = 'abcd';\n"
            + "    alert(input.selectionStart + ',' + input.selectionEnd);\n"

            + "    input.selectionStart = 0;\n"
            + "    input.selectionEnd = 4;\n"

            + "    input.value = 'a';\n"
            + "    alert(input.selectionStart + ',' + input.selectionEnd);\n"
            + "</script>\n"
            + "</body>\n"
            + "</html>";
        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = " foo \n bar\n test\n a <p>html snippet</p>",
            FF31 = "foo bar test a <p>html snippet</p>")
    public void asText() throws Exception {
        final String html
            = "<html><head><title>foo</title></head><body>\n"
            + "<form id='form1'>\n"
            + "<textarea id='textArea1'> foo \n bar\r\n test\r a "
            + "<p>html snippet</p>\n"
            + "</textarea>\n"
            + "</form></body></html>";
        final WebDriver driver = loadPage2(html);
        final WebElement textArea = driver.findElement(By.id("textArea1"));
        Assert.assertEquals(getExpectedAlerts()[0], textArea.getText());
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts("")
    public void asTextAndVisibility() throws Exception {
        final String html
            = "<html><head><title>foo</title></head><body>\n"
            + "<form id='form1'>\n"
            + "<textarea id='textArea1' style='visibility:hidden'> foo \n bar "
            + "</textarea>\n"
            + "</form></body></html>";
        final WebDriver driver = loadPage2(html);
        final WebElement textArea = driver.findElement(By.id("textArea1"));
        Assert.assertEquals(getExpectedAlerts()[0], textArea.getText());
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = " foo \n bar <p>html snippet</p>",
            FF31 = "foo bar <p>html snippet</p>")
    public void parentAsText() throws Exception {
        final String html
            = "<html><head><title>foo</title></head><body>\n"
            + "<form id='form1'>\n"
            + "<textarea name='textArea1'> foo \n bar "
            + "<p>html snippet</p>\n"
            + "</textarea>\n"
            + "</form></body></html>";

        final WebDriver driver = loadPage2(html);
        final WebElement textArea = driver.findElement(By.id("form1"));
        Assert.assertEquals(getExpectedAlerts()[0], textArea.getText());
    }
}
