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
package com.gargoylesoftware.htmlunit.javascript.host.html;

import static com.gargoylesoftware.htmlunit.BrowserRunner.Browser.CHROME;
import static com.gargoylesoftware.htmlunit.BrowserRunner.Browser.FF;
import static com.gargoylesoftware.htmlunit.BrowserRunner.Browser.FF31;
import static com.gargoylesoftware.htmlunit.BrowserRunner.Browser.IE11;
import static com.gargoylesoftware.htmlunit.BrowserRunner.Browser.IE8;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.gargoylesoftware.htmlunit.BrowserRunner;
import com.gargoylesoftware.htmlunit.BrowserRunner.Alerts;
import com.gargoylesoftware.htmlunit.BrowserRunner.BuggyWebDriver;
import com.gargoylesoftware.htmlunit.BrowserRunner.NotYetImplemented;
import com.gargoylesoftware.htmlunit.WebDriverTestCase;

/**
 * Tests for {@link HTMLOptionElement}.
 *
 * @version $Revision: 10989 $
 * @author Marc Guillemot
 * @author Ahmed Ashour
 * @author Ronald Brill
 * @author Frank Danek
 */
@RunWith(BrowserRunner.class)
public class HTMLOptionElement2Test extends WebDriverTestCase {

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = { },
            FF38 = "SELECT;",
            IE = "SELECT;")
    @NotYetImplemented({ FF31, CHROME })
    public void clickSelect() throws Exception {
        final String html
            = "<html><head><title>foo</title><script>\n"
                + "  function log(x) {\n"
                + "    document.getElementById('log_').value += x + '; ';\n"
                + "  }\n"

                + "  function init() {\n"
                + "    var s = document.getElementById('s');\n"
                + "    if (s.addEventListener) {\n"
                + "      s.addEventListener('click', handle, false);\n"
                + "    } else if (s.attachEvent) {\n"
                + "      s.attachEvent('onclick', handle);\n"
                + "    }\n"
                + "  }\n"

                + "  function handle(event) {\n"
                + "    if (event.target) {\n"
                + "      log(event.target.nodeName);\n"
                + "    } else {\n"
                + "      log(event.srcElement.nodeName);\n"
                + "    }\n"
                + "  }\n"
                + "</script></head>\n"

                + "<body onload='init()'>\n"
                + "<form>\n"
                + "  <textarea id='log_' rows='4' cols='50'></textarea>\n"
                + "  <select id='s' size='7'>\n"
                + "    <option value='opt-a'>A</option>\n"
                + "    <option id='opt-b' value='b'>B</option>\n"
                + "    <option value='opt-c'>C</option>\n"
                + "  </select>\n"
                + "</form>\n"
                + "</body></html>";

        final WebDriver driver = loadPage2(html);
        driver.findElement(By.id("s")).click();

        final List<String> alerts = new LinkedList<>();
        final WebElement log = driver.findElement(By.id("log_"));
        alerts.add(log.getAttribute("value").trim());
        assertEquals(getExpectedAlerts(), alerts);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = "opt-a; opt-b",
            CHROME = "opt-b")
    @BuggyWebDriver
    @NotYetImplemented
    //TODO: Needs further investigation of clicking an option without clicking the select
    // See the first comment in http://code.google.com/p/selenium/issues/detail?id=2131#c1
    // Additionally, FF and Chrome drivers look buggy as they don't allow to capture
    // what happens when running the test manually in the browser.
    public void click2() throws Exception {
        final String html
            = "<html><head><title>foo</title><script>\n"
                + "  function log(x) {\n"
                + "    document.getElementById('log_').value += x + '; ';\n"
                + "  }\n"

                + "  function init() {\n"
                + "    s = document.getElementById('s');\n"
                + "    if (s.addEventListener) {\n"
                + "      s.addEventListener('click', handle, false);\n"
                + "    } else if (s.attachEvent) {\n"
                + "      s.attachEvent('onclick', handle);\n"
                + "    }\n"
                + "  }\n"

                + "  function handle(event) {\n"
                + "    log(s.options[s.selectedIndex].value);\n"
                + "  }\n"
                + "</script></head>\n"

                + "<body onload='init()'>\n"
                + "<form>\n"
                + "  <textarea id='log_' rows='4' cols='50'></textarea>\n"
                + "  <select id='s'>\n"
                + "    <option value='opt-a'>A</option>\n"
                + "    <option id='opt-b' value='b'>B</option>\n"
                + "    <option value='opt-c'>C</option>\n"
                + "  </select>\n"
                + "</form>\n"
                + "</body></html>";

        final WebDriver driver = loadPage2(html);
        driver.findElement(By.id("s")).click();
        driver.findElement(By.id("opt-b")).click();

        final List<String> alerts = new LinkedList<>();
        final WebElement log = driver.findElement(By.id("log_"));
        alerts.add(log.getAttribute("value").trim());
        assertEquals(getExpectedAlerts(), alerts);
    }

    /**
     * Test for the right event sequence when clicking.
     *
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = "onchange-select; onclick-select;",
            FF = "onchange-select; onclick-option; onclick-select;")
    @BuggyWebDriver(FF)
    @NotYetImplemented(CHROME)
    public void clickOptionEventSequence1() throws Exception {
        final String html = "<html><head>\n"
                + "<script>\n"
                + "  function log(x) {\n"
                + "    document.getElementById('log_').value += x + '; ';\n"
                + "  }\n"
                + "</script></head>\n"

                + "<body>\n"
                + "<form>\n"
                + "  <textarea id='log_' rows='4' cols='50'></textarea>\n"
                + "  <select id='s' size='2' onclick=\"log('onclick-select')\""
                        + " onchange=\"log('onchange-select')\">\n"
                + "    <option id='clickId' value='a' onclick=\"log('onclick-option')\""
                        + " onchange=\"log('onchange-option')\">A</option>\n"
                + "  </select>\n"
                + "</form>\n"

                + "</body></html>";

        final WebDriver driver = loadPage2(html);

        driver.findElement(By.id("clickId")).click();

        final List<String> alerts = new LinkedList<>();
        final WebElement log = driver.findElement(By.id("log_"));
        alerts.add(log.getAttribute("value").trim());
        assertEquals(getExpectedAlerts(), alerts);
    }

    /**
     * Test for the right event sequence when clicking.
     *
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = "change-SELECT; click-SELECT;",
            FF = "change-SELECT; click-OPTION; click-OPTION;")
    @BuggyWebDriver(FF)
    @NotYetImplemented(CHROME)
    public void clickOptionEventSequence2() throws Exception {
        final String html = "<html><head>\n"
                + "<script>\n"
                + "  function log(x) {\n"
                + "    document.getElementById('log_').value += x + '; ';\n"
                + "  }\n"

                + "  function init() {\n"
                + "    var s = document.getElementById('s');\n"
                + "    var o = document.getElementById('clickId');\n"
                + "    if (s.addEventListener) {\n"
                + "      s.addEventListener('click', handle, false);\n"
                + "      s.addEventListener('change', handle, false);\n"
                + "      o.addEventListener('click', handle, false);\n"
                + "      o.addEventListener('change', handle, false);\n"
                + "    } else if (s.attachEvent) {\n"
                + "      s.attachEvent('onclick', handle);\n"
                + "      s.attachEvent('onchange', handle);\n"
                + "      o.attachEvent('onclick', handle);\n"
                + "      o.attachEvent('onchange', handle);\n"
                + "    }\n"
                + "  }\n"

                + "  function handle(event) {\n"
                + "    if (event.target) {\n"
                + "      log(event.type + '-' + event.target.nodeName);\n"
                + "    } else {\n"
                + "      log(event.type + '-' + event.srcElement.nodeName);\n"
                + "    }\n"
                + "  }\n"
                + "</script></head>\n"

                + "<body onload='init()'>\n"
                + "<form>\n"
                + "  <textarea id='log_' rows='4' cols='50'></textarea>\n"
                + "  <select id='s' size='2' >\n"
                + "    <option id='clickId' value='a' >A</option>\n"
                + "  </select>\n"
                + "</form>\n"

                + "</body></html>";

        final WebDriver driver = loadPage2(html);

        driver.findElement(By.id("clickId")).click();

        final List<String> alerts = new LinkedList<>();
        final WebElement log = driver.findElement(By.id("log_"));
        alerts.add(log.getAttribute("value").trim());
        assertEquals(getExpectedAlerts(), alerts);
    }

    /**
     * Test for the right event sequence when clicking.
     *
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = "onchange-select; change-SELECT; onclick-select; click-SELECT;",
            FF = "onchange-select; change-SELECT; onclick-option; click-OPTION; onclick-select; click-OPTION;")
    @BuggyWebDriver(FF)
    @NotYetImplemented(CHROME)
    public void clickOptionEventSequence3() throws Exception {
        final String html = "<html><head>\n"
                + "<script>\n"
                + "  function log(x) {\n"
                + "    document.getElementById('log_').value += x + '; ';\n"
                + "  }\n"

                + "  function init() {\n"
                + "    var s = document.getElementById('s');\n"
                + "    var o = document.getElementById('clickId');\n"
                + "    if (s.addEventListener) {\n"
                + "      s.addEventListener('click', handle, false);\n"
                + "      s.addEventListener('change', handle, false);\n"
                + "      o.addEventListener('click', handle, false);\n"
                + "      o.addEventListener('change', handle, false);\n"
                + "    } else if (s.attachEvent) {\n"
                + "      s.attachEvent('onclick', handle);\n"
                + "      s.attachEvent('onchange', handle);\n"
                + "      o.attachEvent('onclick', handle);\n"
                + "      o.attachEvent('onchange', handle);\n"
                + "    }\n"
                + "  }\n"

                + "  function handle(event) {\n"
                + "    if (event.target) {\n"
                + "      log(event.type + '-' + event.target.nodeName);\n"
                + "    } else {\n"
                + "      log(event.type + '-' + event.srcElement.nodeName);\n"
                + "    }\n"
                + "  }\n"
                + "</script></head>\n"

                + "<body onload='init()'>\n"
                + "<form>\n"
                + "  <textarea id='log_' rows='4' cols='50'></textarea>\n"
                + "  <select id='s' size='2' onclick=\"log('onclick-select')\""
                        + " onchange=\"log('onchange-select')\">\n"
                + "    <option id='clickId' value='a' onclick=\"log('onclick-option')\""
                        + " onchange=\"log('onchange-option')\">A</option>\n"
                + "  </select>\n"
                + "</form>\n"

                + "</body></html>";

        final WebDriver driver = loadPage2(html);

        driver.findElement(By.id("clickId")).click();

        final List<String> alerts = new LinkedList<>();
        final WebElement log = driver.findElement(By.id("log_"));
        alerts.add(log.getAttribute("value").trim());
        assertEquals(getExpectedAlerts(), alerts);
    }

    /**
     * Regression test for 3171569: unselecting the selected option should select the first one (FF)
     * or have no effect (IE).
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = { "1", "option1", "0" },
            IE8 = { "1", "option2", "1" })
    public void unselectResetToFirstOption() throws Exception {
        final String html
            = "<html><head><title>foo</title><script>\n"
            + "function doTest() {\n"
            + "  var sel = document.form1.select1;\n"
            + "  alert(sel.selectedIndex);\n"
            + "  sel.options[1].selected = false;\n"
            + "  alert(sel.value);\n"
            + "  alert(sel.selectedIndex);\n"
            + "}</script></head><body onload='doTest()'>\n"
            + "<form name='form1'>\n"
            + "    <select name='select1'>\n"
            + "        <option value='option1' name='option1'>One</option>\n"
            + "        <option value='option2' name='option2' selected>Two</option>\n"
            + "    </select>\n"
            + "</form>\n"
            + "</body></html>";

        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts("1")
    public void selectFromJSTriggersNoFocusEvent() throws Exception {
        final String html
            = "<html><head><title>foo</title><script>\n"
            + "function doTest() {\n"
            + "  var sel = document.form1.select1;\n"
            + "  sel.options[1].selected = true;\n"
            + "  alert(sel.selectedIndex);\n"
            + "}\n"
            + "</script></head><body onload='doTest()'>\n"
            + "<form name='form1'>\n"
            + "    <select name='select1' onfocus='alert(\"focus\")'>\n"
            + "        <option value='option1' name='option1'>One</option>\n"
            + "        <option value='option2' name='option2'>Two</option>\n"
            + "    </select>\n"
            + "</form>\n"
            + "</body></html>";

        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts({ "false", "true", "true", "false", "true" })
    public void disabledAttribute() throws Exception {
        final String html =
            "<html>\n"
            + "  <head>\n"
            + "    <script>\n"
            + "      function test() {\n"
            + "        var test1 = document.getElementById('test1');\n"
            + "        alert(test1.disabled);\n"
            + "        test1.disabled = true;\n"
            + "        alert(test1.disabled);\n"
            + "        test1.disabled = true;\n"
            + "        alert(test1.disabled);\n"
            + "        test1.disabled = false;\n"
            + "        alert(test1.disabled);\n"

            + "        var test2 = document.getElementById('test2');\n"
            + "        alert(test2.disabled);\n"
            + "      }\n"
            + "    </script>\n"
            + "  </head>\n"
            + "  <body onload='test()'>\n"
            + "    <form name='form1'>\n"
            + "      <select>\n"
            + "        <option id='test1' value='option1'>Option1</option>\n"
            + "        <option id='test2' value='option2' disabled>Option2</option>\n"
            + "      </select>\n"
            + "  </form>\n"
            + "</body></html>";
        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts({ "some text", "some value", "false", "some other text", "some other value", "true" })
    public void readPropsBeforeAdding() throws Exception {
        final String html
            = "<html><head><title>foo</title><script>\n"
            + "function doTest() {\n"
            + "    var oOption = new Option('some text', 'some value');\n"
            + "    alert(oOption.text);\n"
            + "    alert(oOption.value);\n"
            + "    alert(oOption.selected);\n"
            + "    oOption.text = 'some other text';\n"
            + "    oOption.value = 'some other value';\n"
            + "    oOption.selected = true;\n"
            + "    alert(oOption.text);\n"
            + "    alert(oOption.value);\n"
            + "    alert(oOption.selected);\n"
            + "}</script></head><body onload='doTest()'>\n"
            + "<p>hello world</p>\n"
            + "</body></html>";

        loadPageWithAlerts2(html);
    }

    /**
     * Regression test for bug 313.
     * See http://sourceforge.net/p/htmlunit/bugs/313/.
     * @throws Exception if the test fails
     */
    @Test
    public void selectingOrphanedOptionCreatedByDocument() throws Exception {
        final String html = "<html>\n"
            + "<body>\n"
            + "<form name='myform'/>\n"
            + "<script language='javascript'>\n"
            + "var select = document.createElement('select');\n"
            + "var opt = document.createElement('option');\n"
            + "opt.value = 'x';\n"
            + "opt.selected = true;\n"
            + "select.appendChild(opt);\n"
            + "document.myform.appendChild(select);\n"
            + "</script>\n"
            + "</body></html>";
        loadPageWithAlerts2(html);
    }

    /**
     * Regression test for 1592728.
     * @throws Exception if the test fails
     */
    @Test
    @Alerts({ "2", "2" })
    public void setSelected() throws Exception {
        final String html
            = "<html><head><title>foo</title><script>\n"
            + "function doTest() {\n"
            + "  var sel = document.form1.select1;\n"
            + "  alert(sel.selectedIndex);\n"
            + "  sel.options[0].selected = false;\n"
            + "  alert(sel.selectedIndex);\n"
            + "}</script></head><body onload='doTest()'>\n"
            + "<form name='form1'>\n"
            + "    <select name='select1' onchange='this.form.submit()'>\n"
            + "        <option value='option1' name='option1'>One</option>\n"
            + "        <option value='option2' name='option2'>Two</option>\n"
            + "        <option value='option3' name='option3' selected>Three</option>\n"
            + "    </select>\n"
            + "</form>\n"
            + "</body></html>";

        loadPageWithAlerts2(html);
    }

    /**
     * Regression test for 1672048.
     * @throws Exception if the test fails
     */
    @Test
    public void setAttribute() throws Exception {
        final String html
            = "<html><head><title>foo</title><script>\n"
            + "function doTest() {\n"
            + "  document.getElementById('option1').setAttribute('class', 'bla bla');\n"
            + "  var o = new Option('some text', 'some value');\n"
            + "  o.setAttribute('class', 'myClass');\n"
            + "}</script></head><body onload='doTest()'>\n"
            + "<form name='form1'>\n"
            + "    <select name='select1'>\n"
            + "        <option value='option1' id='option1' name='option1'>One</option>\n"
            + "    </select>\n"
            + "</form>\n"
            + "</body></html>";

        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = { "undefined", "undefined" },
            IE11 = { "null", "undefined" },
            IE8 = { "null", "exception" })
    public void optionIndexOutOfBound() throws Exception {
        final String html
            = "<html>\n"
            + "<head><title>foo</title>\n"
            + "<script>\n"
            + "function doTest() {\n"
            + "  var options = document.getElementById('testSelect').options;\n"
            + "  alert(options[55]);\n"
            + "  try {\n"
            + "    alert(options[-55]);\n"
            + "  } catch (e) { alert('exception'); }\n"
            + "}\n"
            + "</script>\n"
            + "</head>\n"
            + "<body onload='doTest()'>\n"
            + "  <form name='form1'>\n"
            + "    <select name='select1' id='testSelect'>\n"
            + "        <option value='option1' name='option1'>One</option>\n"
            + "    </select>\n"
            + "  </form>\n"
            + "</body></html>";

        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = { "o2: text: Option 2, label: Option 2, value: 2, defaultSelected: false, selected: false",
            "o3: text: Option 3, label: Option 3, value: 3, defaultSelected: true, selected: false",
            "0", "1" },
        IE8 = { "o2: text: Option 2, label: , value: 2, defaultSelected: false, selected: false",
            "o3: text: Option 3, label: , value: 3, defaultSelected: true, selected: false",
            "0", "1" })
    public void constructor() throws Exception {
        final String html
            = "<html><head><title>foo</title><script>\n"
            + "function dumpOption(_o) {\n"
            + "  return 'text: ' + _o.text\n"
            + " + ', label: ' + _o.label\n"
            + " + ', value: ' + _o.value\n"
            + " + ', defaultSelected: ' + _o.defaultSelected\n"
            + " + ', selected: ' + _o.selected;\n"
            + "}\n"
            + "function doTest() {\n"
            + "   var o2 = new Option('Option 2', '2');\n"
            + "   alert('o2: ' + dumpOption(o2));\n"
            + "   var o3 = new Option('Option 3', '3', true, false);\n"
            + "   alert('o3: ' + dumpOption(o3));\n"
            + "   document.form1.select1.appendChild(o3);\n"
            + "   alert(document.form1.select1.options.selectedIndex);\n"
            + "   document.form1.reset();\n"
            + "   alert(document.form1.select1.options.selectedIndex);\n"
            + "}\n"
            + "</script></head><body onload='doTest()'>\n"
            + "<form name='form1'>\n"
            + "    <select name='select1' id='testSelect'>\n"
            + "        <option value='option1' name='option1'>One</option>\n"
            + "    </select>\n"
            + "</form>\n"
            + "</body></html>";

        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts("0")
    public void insideBold() throws Exception {
        final String html
            = "<html><head><title>foo</title><script>\n"
            + "function test() {\n"
            + "  var sel = document.form1.select1;\n"
            + "  sel.options[0] = null;\n"
            + "  alert(sel.options.length);\n"
            + "}</script></head><body onload='test()'>\n"
            + "<form name='form1'>\n"
            + "  <b>\n"
            + "    <select name='select1'>\n"
            + "        <option>One</option>\n"
            + "    </select>\n"
            + "  </b>\n"
            + "</form>\n"
            + "</body></html>";

        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = { "null", "[object Attr]", "null", "null", "null",
                "null", "null", "null", "null", "null" },
            IE8 = { "[object]", "[object]", "[object]", "[object]", "null",
                "[object]", "null", "[object]", "[object]", "null" })
    public void getAttributeNode() throws Exception {
        final String html
            = "<html><head><title>foo</title><script>\n"
            + "function doTest() {\n"
            + "  var s = document.getElementById('testSelect');\n"
            + "  var o1 = s.options[0];\n"
            + "  alert(o1.getAttributeNode('id'));\n"
            + "  alert(o1.getAttributeNode('name'));\n"
            + "  alert(o1.getAttributeNode('value'));\n"
            + "  alert(o1.getAttributeNode('selected'));\n"
            + "  alert(o1.getAttributeNode('foo'));\n"
            + "  var o2 = s.options[1];\n"
            + "  alert(o2.getAttributeNode('id'));\n"
            + "  alert(o2.getAttributeNode('name'));\n"
            + "  alert(o2.getAttributeNode('value'));\n"
            + "  alert(o2.getAttributeNode('selected'));\n"
            + "  alert(o2.getAttributeNode('foo'));\n"
            + "}\n"
            + "</script></head>\n"
            + "<body onload='doTest()'>\n"
            + "<form name='form1'>\n"
            + "    <select name='select1' id='testSelect'>\n"
            + "        <option name='option1'>One</option>\n"
            + "        <option>Two</option>\n"
            + "    </select>\n"
            + "</form>\n"
            + "</body></html>";

        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = { "false null null", "false null null", "true *selected selected",
                        "true null null", "false null null", "false *selected selected",
                        "false null null", "true null null", "false *selected selected",
                        "true null null", "false null null", "false *selected selected" },
            IE8 = { "false *false false", "false *false false", "true *true true",
                    "true *true true", "false *false false", "false *false false",
                    "false *false false", "true *true true", "false *false false",
                    "false *false false", "true *true true", "false *false false" })
    @NotYetImplemented(IE8)
    public void selectedAttribute() throws Exception {
        final String html
            = "<html><head><title>foo</title>\n"
            + "<script>\n"
            + "  function info(opt) {\n"
            + "    var attrNode = opt.getAttributeNode('selected');\n"
            + "    if (attrNode) { attrNode = '*' + attrNode.value; }\n"
            + "    alert(opt.selected + ' ' + attrNode + ' ' + opt.getAttribute('selected'));\n"
            + "  }\n"

            + "  function doTest() {\n"
            + "    var s = document.getElementById('testSelect');\n"

            + "    var o1 = s.options[0];\n"
            + "    var o2 = s.options[1];\n"
            + "    var o3 = s.options[2];\n"

            + "    info(o1);info(o2);info(o3);\n"

            + "    o1.selected = true;\n"
            + "    info(o1);info(o2);info(o3);\n"

            + "    o2.selected = true;\n"
            + "    info(o1);info(o2);info(o3);\n"

            + "    o2.selected = false;\n"
            + "    info(o1);info(o2);info(o3);\n"

            + "}\n"
            + "</script></head>\n"
            + "<body onload='doTest()'>\n"
            + "<form name='form1'>\n"
            + "    <select name='select1' id='testSelect'>\n"
            + "        <option>One</option>\n"
            + "        <option>Two</option>\n"
            + "        <option selected='selected'>Three</option>\n"
            + "    </select>\n"
            + "</form>\n"
            + "</body></html>";

        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = { "false null null", "false null null", "true *selected selected",
                        "true null null", "false null null", "true *selected selected",
                        "true null null", "true null null", "true *selected selected",
                        "true null null", "false null null", "true *selected selected" },
            IE8 = { "false *false false", "false *false false", "true *true true",
                    "true *true true", "false *false false", "true *true true",
                    "true *true true", "true *true true", "true *true true",
                    "true *true true", "false *false false", "true *true true" })
    @NotYetImplemented(IE8)
    public void selectedAttributeMultiple() throws Exception {
        final String html
            = "<html><head><title>foo</title>\n"
            + "<script>\n"
            + "  function info(opt) {\n"
            + "    var attrNode = opt.getAttributeNode('selected');\n"
            + "    if (attrNode) { attrNode = '*' + attrNode.value; }\n"
            + "    alert(opt.selected + ' ' + attrNode + ' ' + opt.getAttribute('selected'));\n"
            + "  }\n"

            + "  function doTest() {\n"
            + "    var s = document.getElementById('testSelect');\n"

            + "    var o1 = s.options[0];\n"
            + "    var o2 = s.options[1];\n"
            + "    var o3 = s.options[2];\n"

            + "    info(o1);info(o2);info(o3);\n"

            + "    o1.selected = true;\n"
            + "    info(o1);info(o2);info(o3);\n"

            + "    o2.selected = true;\n"
            + "    info(o1);info(o2);info(o3);\n"

            + "    o2.selected = false;\n"
            + "    info(o1);info(o2);info(o3);\n"

            + "}\n"
            + "</script></head>\n"
            + "<body onload='doTest()'>\n"
            + "<form name='form1'>\n"
            + "    <select name='select1' id='testSelect' multiple>\n"
            + "        <option>One</option>\n"
            + "        <option>Two</option>\n"
            + "        <option selected='selected'>Three</option>\n"
            + "    </select>\n"
            + "</form>\n"
            + "</body></html>";

        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = { "[object HTMLOptionsCollection]", "0", "1" },
            IE11 = { "[object HTMLSelectElement]", "0", "1" },
            IE8 = { "[object]", "0", "1" })
    public void with_new() throws Exception {
        final String html
            = "<html><head><title>foo</title><script>\n"
            + "function doTest() {\n"
            + "  var s = document.getElementById('testSelect');\n"
            + "  alert(s.options);\n"
            + "  alert(s.length);\n"
            + "  try {\n"
            + "    s.options[0] = new Option('one', 'two');\n"
            + "  } catch (e) { alert(e) };\n"
            + "  alert(s.length);\n"
            + "}\n"
            + "</script></head>\n"
            + "<body onload='doTest()'>\n"
            + "  <select id='testSelect'>\n"
            + "  </select>\n"
            + "</form>\n"
            + "</body></html>";

        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = { "[object HTMLOptionsCollection]", "0", "exception", "0" },
            IE11 = { "[object HTMLSelectElement]", "0", "1" },
            IE8 = { "[object]", "0", "exception", "0" })
    @NotYetImplemented(IE11)
    public void without_new() throws Exception {
        final String html
            = "<html><head><title>foo</title><script>\n"
            + "function doTest() {\n"
            + "  var s = document.getElementById('testSelect');\n"
            + "  alert(s.options);\n"
            + "  alert(s.length);\n"
            + "  try {\n"
            + "    s.options[0] = Option('one', 'two');\n"
            + "  } catch (e) { alert('exception') };\n"
            + "  alert(s.length);\n"
            + "}\n"
            + "</script></head>\n"
            + "<body onload='doTest()'>\n"
            + "  <select id='testSelect'>\n"
            + "  </select>\n"
            + "</form>\n"
            + "</body></html>";

        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts({"text1", "New Text" })
    public void text() throws Exception {
        final String html =
            "<html>\n"
            + "  <head>\n"
            + "    <title>Page Title</title>\n"
            + "    <script>\n"
            + "      function test() {\n"
            + "         var option = document.getElementsByTagName('option')[0];\n"
            + "         alert(option.text);\n"
            + "         option.text='New Text';\n"
            + "         alert(option.text);\n"
            + "      }\n"
            + "    </script>\n"
            + "  </head>\n"
            + "  <body onload='test()'>\n"
            + "    <select>\n"
            + "      <option value='value1' label='label1'>text1</option>\n"
            + "    </select>\n"
            + "  </body>\n"
            + "</html>";
        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = { "[object Text]", "[object Text]", "null" },
            CHROME = { "null", "[object Text]", "[object Text]" },
            IE8 = { "null", "[object]", "null" },
            IE11 = { "null", "[object Text]", "null" })
    @NotYetImplemented(CHROME)
    public void setText() throws Exception {
        final String html
            = "<html><head><title>foo</title><script>\n"
            + "function test() {\n"
            + "  var s = document.getElementById('testSelect');\n"
            + "  var lastIndex = s.length;\n"
            + "  s.length += 1;\n"
            + "  alert(s[lastIndex].firstChild);\n"
            + "  s[lastIndex].text  = 'text2';\n"
            + "  alert(s[lastIndex].firstChild);\n"
            + "  s[lastIndex].text  = '';\n"
            + "  alert(s[lastIndex].firstChild);\n"
            + "}\n"
            + "</script></head><body onload='test()'>\n"
            + "  <select id='testSelect'>\n"
            + "    <option value='value1' label='label1'>text1</option>\n"
            + "  </select>\n"
            + "</form>\n"
            + "</body></html>";

        loadPageWithAlerts2(html);
    }

    /**
     * Visibility of elements should not impact the determination of the value of option
     * without value attribute or the text of options. This tests for one part a regression
     * introduced in rev. 4367 as well probably as a problem that exists since a long time.
     * @throws Exception if the test fails
     */
    @Test
    @Alerts({ "text1", "text1b", "text2" })
    public void text_when_not_displayed() throws Exception {
        final String html
            = "<html><head><title>foo</title><script>\n"
            + "function test() {\n"
            + "  var s = document.getElementById('testSelect1');\n"
            + "  alert(s.options[0].text);\n"
            + "  alert(s.options[1].text);\n"
            + "  var s2 = document.getElementById('testSelect2');\n"
            + "  alert(s2.options[0].text);\n"
            + "}\n"
            + "</script></head><body onload='test()'>\n"
            + "  <div style='display: none'>\n"
            + "  <select id='testSelect1'>\n"
            + "    <option>text1</option>\n"
            + "    <option><strong>text1b</strong></option>\n"
            + "  </select>\n"
            + "  </div>\n"
            + "  <div style='visibility: hidden'>\n"
            + "  <select id='testSelect2'>\n"
            + "    <option>text2</option>\n"
            + "  </select>\n"
            + "  </div>\n"
            + "</form>\n"
            + "</body></html>";

        loadPageWithAlerts2(html);
    }

    /**
     * For IE nested nodes aren't used as default value attribute.
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = { "text0", "text1", "text1b", "text2" },
            IE8 = { "", "", "", "" })
    public void defaultValueFromNestedNodes() throws Exception {
        final String html
            = "<html><head><title>foo</title><script>\n"
            + "function test() {\n"
            + "  var s0 = document.getElementById('testSelect0');\n"
            + "  alert(s0.options[0].value);\n"
            + "  var s = document.getElementById('testSelect1');\n"
            + "  alert(s.options[0].value);\n"
            + "  alert(s.options[1].value);\n"
            + "  var s2 = document.getElementById('testSelect2');\n"
            + "  alert(s2.options[0].value);\n"
            + "}\n"
            + "</script></head><body onload='test()'>\n"
            + "  <select id='testSelect0'>\n"
            + "    <option>text0</option>\n"
            + "  </select>\n"
            + "  <div style='display: none'>\n"
            + "  <select id='testSelect1'>\n"
            + "    <option>text1</option>\n"
            + "    <option><strong>text1b</strong></option>\n"
            + "  </select>\n"
            + "  </div>\n"
            + "  <div style='visibility: hidden'>\n"
            + "  <select id='testSelect2'>\n"
            + "    <option>text2</option>\n"
            + "  </select>\n"
            + "  </div>\n"
            + "</form>\n"
            + "</body></html>";
        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = "[object HTMLFormElement]",
            IE8 = "[object]")
    public void form() throws Exception {
        final String html
            = "<html>\n"
            + "<body>\n"
            + "  <form>\n"
            + "    <select id='s'>\n"
            + "      <option>a</option>\n"
            + "    </select>\n"
            + "  </form>\n"
            + "  <script>\n"
            + "    alert(document.getElementById('s').options[0].form);\n"
            + "  </script>\n"
            + "</body></html>";
        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = {"o2", "1", "0", "o2" },
            IE8 = { "evaluate not supported", "1", "1", "evaluate not supported" },
            IE11 = { "evaluate not supported", "1", "0", "evaluate not supported" })
    public void xpathSelected() throws Exception {
        final String selectionChangeCode = "    sel.options[1].selected = false;\n";

        xpathSelected(selectionChangeCode, false);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = {"o2", "1", "1", "o2" },
            IE = { "evaluate not supported", "1", "1", "evaluate not supported" })
    public void xpathSelectedSetAttribute() throws Exception {
        final String selectionChangeCode = "    sel.options[1].setAttribute('selected', false);\n";

        xpathSelected(selectionChangeCode, false);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = {"o2", "1", "-1", "o2" },
            IE = { "evaluate not supported", "1", "-1", "evaluate not supported" })
    public void xpathSelectedMultiple() throws Exception {
        final String selectionChangeCode = "    sel.options[1].selected = false;\n";

        xpathSelected(selectionChangeCode, true);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = {"o2", "1", "1", "o2" },
            IE = { "evaluate not supported", "1", "1", "evaluate not supported" })
    public void xpathSelectedSetAttributeMultiple() throws Exception {
        final String selectionChangeCode = "    sel.options[1].setAttribute('selected', false);\n";

        xpathSelected(selectionChangeCode, false);
    }

    private void xpathSelected(final String selectionChangeCode, final boolean multiple) throws Exception {
        final String html
            = "<html>\n"
            + "<head><title>foo</title>\n"
            + "<script>\n"
            + "  function test() {\n"
            + "    xpath();\n"
            + "    var sel = document.getElementById('s1');\n"
            + "    alert(sel.selectedIndex);\n"
            + selectionChangeCode
            + "    alert(sel.selectedIndex);\n"
            + "    xpath();\n"
            + "  }\n"

            + "  function xpath() {\n"
            + "    if (document.evaluate && XPathResult) {\n"
            + "      try {\n"
            + "        var result = document.evaluate('" + "//option[@selected]" + "', document.documentElement, "
                                            + "null, XPathResult.ANY_TYPE, null);\n"

            + "        var thisNode = result.iterateNext();\n"
            + "        while (thisNode) {\n"
            + "          alert(thisNode.getAttribute('id'));\n"
            + "          thisNode = result.iterateNext();\n"
            + "        }\n"
            + "      } catch (e) { alert(e); }\n"
            + "    } else {\n"
            + "      alert('evaluate not supported');\n"
            + "    }\n"
            + "  }\n"
            + "</script>\n"
            + "</head>\n"
            + "<body onload='test()'>\n"
            + "  <form id='form1'>\n"
            + "    <select id='s1' name='select1' " + (multiple ? "multiple='multiple'" : "") + ">\n"
            + "      <option id='o1' value='option1'>Option1</option>\n"
            + "      <option id='o2' value='option2' selected='selected'>Option2</option>\n"
            + "      <option id='o3'value='option3'>Option3</option>\n"
            + "    </select>\n"
            + "  </form>\n"
            + "</body></html>";

        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts({ "text2", "label2" })
    public void setLabel() throws Exception {
        final String html
            = "<html><head><title>foo</title><script>\n"
            + "function test() {\n"
            + "  var s = document.getElementById('testSelect');\n"
            + "  var lastIndex = s.length;\n"
            + "  s.length += 1;\n"
            + "  s[lastIndex].value = 'value2';\n"
            + "  s[lastIndex].text  = 'text2';\n"
            + "  s[lastIndex].label = 'label2';\n"
            + "  alert(s[1].text);\n"
            + "  alert(s[1].label);\n"
            + "}\n"
            + "</script></head><body onload='test()'>\n"
            + "  <select id='testSelect'>\n"
            + "    <option value='value1' label='label1'>text1</option>\n"
            + "  </select>\n"
            + "</form>\n"
            + "</body></html>";

        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts({"0", "1", "2", "0" })
    public void index() throws Exception {
        final String html
            = "<html>\n"
            + "<head><title>foo</title>\n"
            + "<script>\n"
            + "  function test() {\n"
            + "    var opt = document.getElementById('o1');\n"
            + "    alert(opt.index);\n"

            + "    opt = document.getElementById('o2');\n"
            + "    alert(opt.index);\n"

            + "    opt = document.getElementById('o3');\n"
            + "    alert(opt.index);\n"

            + "    opt = document.createElement('option');\n"
            + "    alert(opt.index);\n"
            + "  }\n"
            + "</script>\n"
            + "</head>\n"
            + "<body onload='test()'>\n"
            + "  <form id='form1'>\n"
            + "    <select id='s1'>\n"
            + "      <option id='o1' value='option1'>Option1</option>\n"
            + "      <option id='o2' value='option2' selected='selected'>Option2</option>\n"
            + "      <option id='o3'value='option3'>Option3</option>\n"
            + "    </select>\n"
            + "  </form>\n"
            + "</body></html>";

        loadPageWithAlerts2(html);
    }
}
