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

import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.BrowserRunner;
import com.gargoylesoftware.htmlunit.BrowserRunner.Alerts;
import com.gargoylesoftware.htmlunit.WebDriverTestCase;

/**
 * Function is a native JavaScript object and therefore provided by Rhino but some tests are needed here
 * to be sure that we have the expected results (for instance "bind" is an EcmaScript 5 method that is not
 * available in FF2 or FF3).
 *
 * @version $Revision: 9843 $
 * @author Marc Guillemot
 * @author Ahmed Ashour
 * @author Frank Danek
 */
@RunWith(BrowserRunner.class)
public class NativeFunctionTest extends WebDriverTestCase {

    /**
     * Test for the methods with the same expectations for all browsers.
     * @throws Exception if the test fails
     */
    @Test
    @Alerts({ "apply: function", "arguments: object", "call: function", "constructor: function",
            "toString: function" })
    public void methods_common() throws Exception {
        final String[] methods = {"apply", "arguments", "call", "constructor", "toString"};
        final String html = NativeDateTest.createHTMLTestMethods("function() {}", methods);
        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = "toSource: undefined",
            FF = "toSource: function")
    public void methods_toSource() throws Exception {
        final String html = NativeDateTest.createHTMLTestMethods("function() {}", "toSource");
        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = "bind: function",
            IE8 = "bind: undefined")
    public void methods_bind() throws Exception {
        final String html = NativeDateTest.createHTMLTestMethods("function() {}", "bind");
        loadPageWithAlerts2(html);
    }

    /**
     * Ensure that "arguments" object doesn't see anything from Array's prototype.
     * This was a bug in Rhino from Head as of 06.01.2010 due to adaptation to ES5 (or to some early state
     * of the draft).
     * @throws Exception if the test fails
     */
    @Test
    @Alerts("true")
    public void arguments_prototype() throws Exception {
        final String html
            = "<html><head><title>foo</title><script>\n"
            + "var f1 = function(){};\n"
            + "var f2 = function(){};\n"
            + "Object.prototype.myFunction = f1;\n"
            + "Array.prototype.myFunction = f2;\n"
            + "var a = (function() { return arguments;})();\n"
            + "alert(a.myFunction == f1);\n"
            + "</script></head><body>\n"
            + "</body></html>";

        loadPageWithAlerts2(html);
    }

    /**
     * Regression test for bug 3076362.
     * @see <a href="https://bugzilla.mozilla.org/show_bug.cgi?id=600479">Rhino Bug 600479</a>
     * @throws Exception if the test fails
     */
    @Test
    @Alerts("true")
    public void newFunctionWithSlashSlash() throws Exception {
        final String html
            = "<html><head><title>foo</title><script>\n"
            + "var f1 = new Function('alert(true) //');\n"
            + "f1.call();\n"
            + "</script></head><body>\n"
            + "</body></html>";

        loadPageWithAlerts2(html);
    }

    /**
     * For the first test of this kind, we take a special case to have
     * correct expectations for IE as IE (at least IE6) seems to just return
     * the original string.
     * @throws Exception if the test fails
     */
    @Test
    @Alerts("function anonymous() {\n    var x = 1;\n}")
    public void newFunctionToString() throws Exception {
        final String html
            = "<html><head><title>foo</title><script>\n"
            + "var f1 = new Function('    var x = 1;');\n"
            + "alert(f1);\n"
            + "</script></head><body>\n"
            + "</body></html>";

        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts("function foo() {\n    return 1;\n}")
    public void functionToString() throws Exception {
        final String html
            = "<html><head><title>foo</title><script>\n"
            + "function foo() {\n"
            + "    return 1;\n"
            + "};\n"
            + "alert(foo);\n"
            + "</script></head><body>\n"
            + "</body></html>";

        loadPageWithAlerts2(html);
    }

    /**
     * Function properties "arguments" and "caller" were wrongly enumerated as of HtmlUnit-2.11.
     * @throws Exception if the test fails
     */
    @Test
    @Alerts("foo1 done")
    public void in() throws Exception {
        final String html = "<html><body><script>\n"
            + "function foo1() {\n"
            + "  for (var i in foo1) {\n"
            + "    alert(i);\n"
            + "  };\n"
            + "  alert('foo1 done');\n"
            + "};\n"
            + "function foo0() {\n"
            + "  foo1();\n"
            + "}\n"
            + "foo0();\n"
            + "</script></body></html>";

        loadPageWithAlerts2(html);
    }

    /**
     * Function defined in a scope should not overwrite function in top level scope.
     * @throws Exception if the test fails
     */
    @Test
    @Alerts("true")
    public void definitionInScope() throws Exception {
        final String html = "<html><body><script>\n"
            + "var $ = function() { return 1; };\n"
            + "var ori = $;\n"
            + "function foo() {\n"
            + "  var $ = function $() { return 2; };\n"
            + "};\n"
            + "foo();\n"
            + "alert(ori == $);\n"
            + "</script></body></html>";

        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts({ "2", "eat", "bananas" })
    public void apply() throws Exception {
        final String html = "<html><head><script>\n"
            + "  var myObject = {'length': 2, '0': 'eat', '1': 'bananas'};\n"
            + "  function test() {\n"
            + "    test2.apply(null, myObject);\n"
            + "  }\n"
            + "\n"
            + "  function test2() {\n"
            + "    alert(arguments.length);\n"
            + "    for (var i in arguments) {\n"
            + "      alert(arguments[i]);\n"
            + "    }\n"
            + "  }\n"
            + "</script></head><body onload='test()'>\n"
            + "</body></html>";

        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = { "t: [object Window]", "0", "t: ", "1", "a0: x",
                            "t: ab", "2", "a0: x", "a1: y" },
            IE8 = "bind not supported")
    public void bind() throws Exception {
        final String html = "<html>\n"
            + "<head>\n"
            + "  <script>\n"
            + "  function bindTest() {\n"
            + "    alert('t: ' + this);\n"
            + "    alert(arguments.length);\n"
            + "    for (var i in arguments) {\n"
            + "      alert('a' + i + ': ' + arguments[i]);\n"
            + "    }\n"
            + "  }\n"

            + "  function test() {\n"
            + "    if (!Function.prototype.bind) { alert('bind not supported'); return }"

            + "    var foo = bindTest.bind(null);\n"
            + "    foo();\n"

            + "    foo = bindTest.bind('', 'x');\n"
            + "    foo();\n"

            + "    foo = bindTest.bind('ab', 'x', 'y');\n"
            + "    foo();\n"
            + "  }\n"
            + "  </script>\n"
            + "</head>\n"
            + "<body onload='test()'>\n"
            + "</body></html>";

        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = { "t: ab", "1", "a0: x,y" },
            IE8 = "bind not supported")
    public void bindArrayParam() throws Exception {
        final String html = "<html>\n"
            + "<head>\n"
            + "  <script>\n"
            + "  function bindTest() {\n"
            + "    alert('t: ' + this);\n"
            + "    alert(arguments.length);\n"
            + "    for (var i in arguments) {\n"
            + "      alert('a' + i + ': ' + arguments[i]);\n"
            + "    }\n"
            + "  }\n"

            + "  function test() {\n"
            + "    if (!Function.prototype.bind) { alert('bind not supported'); return }"

            + "    var foo = bindTest.bind('ab', ['x', 'y']);\n"
            + "    foo();\n"
            + "  }\n"
            + "  </script>\n"
            + "</head>\n"
            + "<body onload='test()'>\n"
            + "</body></html>";

        loadPageWithAlerts2(html);
    }
}
