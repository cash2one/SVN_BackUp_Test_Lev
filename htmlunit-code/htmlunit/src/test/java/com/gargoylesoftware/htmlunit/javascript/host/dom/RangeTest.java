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
package com.gargoylesoftware.htmlunit.javascript.host.dom;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.BrowserRunner;
import com.gargoylesoftware.htmlunit.BrowserRunner.Alerts;
import com.gargoylesoftware.htmlunit.WebDriverTestCase;

/**
 * Tests for {@link Range}.
 *
 * @version $Revision: 10483 $
 * @author Marc Guillemot
 * @author Ahmed Ashour
 * @author James Phillpotts
 * @author Frank Danek
 */
@RunWith(BrowserRunner.class)
public class RangeTest extends WebDriverTestCase {

    private static final String contentStart = "<html><head><title>Range Test</title>\n"
        + "<script>\n"
        + "function safeTagName(o) {\n"
        + "  return o ? (o.tagName ? o.tagName : o) : undefined;\n"
        + "}\n"
        + "function alertRange(r) {\n"
        + "  alert(r.collapsed);\n"
        + "  alert(safeTagName(r.commonAncestorContainer));\n"
        + "  alert(safeTagName(r.startContainer));\n"
        + "  alert(r.startOffset);\n"
        + "  alert(safeTagName(r.endContainer));\n"
        + "  alert(r.endOffset);\n"
        + "}\n"
        + "function test() {\n"
        + "  if (!document.createRange) {\n"
        + "    return;\n"
        + "  }\n"
        + "  var r = document.createRange();\n";

    private static final String contentEnd = "\n}\n</script></head>\n"
        + "<body onload='test()'>\n"
        + "<div id='theDiv'>Hello, <span id='theSpan'>this is a test for"
        + "<a id='theA' href='http://htmlunit.sf.net'>HtmlUnit</a> support"
        + "</div>\n"
        + "<p id='theP'>for Range</p>\n"
        + "</body></html>";

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = { "true", "[object HTMLDocument]", "[object HTMLDocument]", "0", "[object HTMLDocument]", "0" },
            IE8 = { })
    public void emptyRange() throws Exception {
        loadPageWithAlerts2(contentStart + "alertRange(r);" + contentEnd);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = { "false", "BODY", "BODY", "1", "BODY", "2" }, IE8 = { })
    public void selectNode() throws Exception {
        final String script = "r.selectNode(document.getElementById('theDiv'));"
            + "alertRange(r);";

        loadPageWithAlerts2(contentStart + script + contentEnd);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = { "false", "DIV", "DIV", "0", "DIV", "2" }, IE8 = { })
    public void selectNodeContents() throws Exception {
        final String script = "r.selectNodeContents(document.getElementById('theDiv'));"
            + "alertRange(r);";

        loadPageWithAlerts2(contentStart + script + contentEnd);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = "<div id=\"myDiv2\"></div><div>harhar</div><div id=\"myDiv3\"></div>",
            IE8 = { })
    public void createContextualFragment() throws Exception {
        final String html = "<html><head><title>foo</title><script>\n"
            + "  function test() {\n"
            + "    if (document.createRange) {\n"
            + "      var element = document.getElementById('myDiv2');\n"
            + "      var range = element.ownerDocument.createRange();\n"
            + "      range.setStartAfter(element);\n"
            + "      var fragment = range.createContextualFragment('<div>harhar</div>');\n"
            + "      element.parentNode.insertBefore(fragment, element.nextSibling);\n"
            + "      alert(element.parentNode.innerHTML);\n"
            + "    }\n"
            + "  }\n"
            + "</script></head><body onload='test()'>\n"
            + "  <div id='myDiv'><div id='myDiv2'></div><div id='myDiv3'></div></div>\n"
            + "</body></html>";

        loadPageWithAlerts2(html);
    }

    /**
     * Same fragment should be parsed differently depending on the context.
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = { "[object Text]", "[object HTMLTableRowElement]" },
            IE8 = "exception")
    public void createContextualFragment2() throws Exception {
        final String html = "<html><body>\n"
            + "<div id ='d'></div>\n"
            + "<table><tr id='t'><td>old</td></tr></table>\n"
            + "<script>\n"
            + "function test(id) {\n"
            + "  var element = document.getElementById(id);\n"
            + "  var range = element.ownerDocument.createRange();\n"
            + "  range.selectNode(element);\n"
            + "  var str = '<tr>  <td>new</td></tr>'\n" // space between <tr> and <td> important here!
            + "  var fragment = range.createContextualFragment(str);\n"
            + "  alert(fragment.firstChild);\n"
            + "}\n"
            + "try {\n"
            + "  test('d');\n"
            + "  test('t');\n"
            + "} catch (e) { alert('exception'); }\n"
            + "</script>\n"
            + "</body></html>";

        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = { "qwerty", "tyxy", "[object DocumentFragment]", "[object HTMLSpanElement] [object Text]", "qwer",
            "[object HTMLSpanElement]" }, IE8 = { })
    public void extractContents() throws Exception {
        final String html =
              "<html><body><div id='d'>abc<span id='s'>qwerty</span>xyz</div><script>\n"
            + "  if (document.createRange) {\n"
            + "    var d = document.getElementById('d');\n"
            + "    var s = document.getElementById('s');\n"
            + "    var r = document.createRange();\n"
            + "    r.setStart(s.firstChild, 4);\n"
            + "    r.setEnd(d.childNodes[2], 2);\n"
            + "    alert(s.innerHTML);\n"
            + "    alert(r);\n"
            + "    var fragment = r.extractContents();\n"
            + "    alert(fragment);\n"
            + "    alert(fragment.childNodes[0] + ' ' + fragment.childNodes[1]);\n"
            + "    alert(s.innerHTML);\n"
            + "    alert(document.getElementById('s'));\n"
            + "  }\n"
            + "</script></body></html>";
        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = {
            "1 <p><b id=\"b\">text1<span id=\"s\">inner</span>text2</b></p>",
            "2 text1",
            "3 [object DocumentFragment]",
            "4 1: [object HTMLParagraphElement]: <b id=\"b\">text1</b>",
            "5 <p><b id=\"b\"><span id=\"s\">inner</span>text2</b></p>",
            "6 1: [object HTMLParagraphElement]: <b id=\"b\"><span id=\"s\"></span>text2</b>",
            "7 <p><b id=\"b\"><span id=\"s\">inner</span></b></p>" },
            IE8 = { })
    public void extractContents2() throws Exception {
        final String html =
              "<html><body><div id='d'><p><b id='b'>text1<span id='s'>inner</span>text2</b></p></div><script>\n"
            + "  if (document.createRange) {\n"
            + "    var d = document.getElementById('d');\n"
            + "    var b = document.getElementById('b');\n"
            + "    var s = document.getElementById('s');\n"
            + "    var r = document.createRange();\n"
            + "    r.setStart(d, 0);\n"
            + "    r.setEnd(b, 1);\n"
            + "    alert('1 ' + d.innerHTML);\n"
            + "    alert('2 ' + r);\n"
            + "    var f = r.extractContents();\n"
            + "    alert('3 ' + f);\n"
            + "    alert('4 ' + f.childNodes.length + ': ' + f.childNodes[0] + ': ' + f.childNodes[0].innerHTML);\n"
            + "    alert('5 ' + d.innerHTML);\n"
            + "    var r2 = document.createRange();\n"
            + "    r2.setStart(s, 1);\n"
            + "    r2.setEnd(d, 1);\n"
            + "    var f2 = r2.extractContents();\n"
            + "    alert('6 ' + f2.childNodes.length + ': ' + f2.childNodes[0] + ': ' + f2.childNodes[0].innerHTML);\n"
            + "    alert('7 ' + d.innerHTML);\n"
            + "  }\n"
            + "</script></body></html>";
        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = { "0", "1", "2", "3" }, IE8 = { })
    public void constants() throws Exception {
        final String html =
              "<html><body><script>\n"
            + "  if (document.createRange) {\n"
            + "    alert(Range.START_TO_START);\n"
            + "    alert(Range.START_TO_END);\n"
            + "    alert(Range.END_TO_END);\n"
            + "    alert(Range.END_TO_START);\n"
            + "  }\n"
            + "</script></body></html>";
        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = { "-1", "1", "1", "-1", "0" }, IE8 = { })
    public void compareBoundaryPoints() throws Exception {
        final String html = "<html><body>\n"
            + "<div id='d1'><div id='d2'></div></div>\n"
            + "<script>\n"
            + "  if (document.createRange) {\n"
            + "    var range = document.createRange();\n"
            + "    range.selectNode(document.getElementById('d1'));\n"
            + "    var sourceRange = document.createRange();\n"
            + "    sourceRange.selectNode(document.getElementById('d2'));\n"
            + "    alert(range.compareBoundaryPoints(Range.START_TO_START, sourceRange));\n"
            + "    alert(range.compareBoundaryPoints(Range.START_TO_END, sourceRange));\n"
            + "    alert(range.compareBoundaryPoints(Range.END_TO_END, sourceRange));\n"
            + "    alert(range.compareBoundaryPoints(Range.END_TO_START, sourceRange));\n"
            + "    alert(range.compareBoundaryPoints(Range.START_TO_START, range));\n"
            + "  }\n"
            + "</script></body></html>";
        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    @Alerts(DEFAULT = { "abcd", "bc", "null", "null", "ad", "bc" }, IE8 = { })
    public void extractContents3() throws Exception {
        final String html =
            "<html><body><div id='d'><span id='a'>a</span><span id='b'>b</span>"
            + "<span id='c'>c</span><span id='d'>d</span></div><script>\n"
            + "  if (document.createRange) {\n"
            + "    var d = document.getElementById('d');\n"
            + "    var s = document.getElementById('s');\n"
            + "    var r = document.createRange();\n"
            + "    r.setStart(d, 1);\n"
            + "    r.setEnd(d, 3);\n"
            + "    alert(d.textContent);\n"
            + "    alert(r.toString());\n"
            + "    var x = r.extractContents();\n"
            + "    alert(document.getElementById('b'));\n"
            + "    alert(document.getElementById('c'));\n"
            + "    alert(d.textContent);\n"
            + "    alert(x.textContent);\n"
            + "  }\n"
            + "</script></body></html>";
        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    @Alerts(DEFAULT = { "qwerty", "tyxy", "[object DocumentFragment]", "[object HTMLSpanElement] [object Text]",
            "qwerty", "[object HTMLSpanElement]" }, IE8 = { })
    public void cloneContents() throws Exception {
        final String html =
            "<html><body><div id='d'>abc<span id='s'>qwerty</span>xyz</div><script>\n"
            + "  if (document.createRange) {\n"
            + "    var d = document.getElementById('d');\n"
            + "    var s = document.getElementById('s');\n"
            + "    var r = document.createRange();\n"
            + "    r.setStart(s.firstChild, 4);\n"
            + "    r.setEnd(d.childNodes[2], 2);\n"
            + "    alert(s.innerHTML);\n"
            + "    alert(r);\n"
            + "    var fragment = r.cloneContents();\n"
            + "    alert(fragment);\n"
            + "    alert(fragment.childNodes[0] + ' ' + fragment.childNodes[1]);\n"
            + "    alert(s.innerHTML);\n"
            + "    alert(document.getElementById('s'));\n"
            + "  }\n"
            + "</script></body></html>";
        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    @Alerts(DEFAULT = { "qwerty", "bcqwertyxy", "null", "az" }, IE8 = { })
    public void deleteContents() throws Exception {
        final String html =
            "<html><body><div id='d'>abc<span id='s'>qwerty</span>xyz</div><script>\n"
            + "  if (document.createRange) {\n"
            + "    var d = document.getElementById('d');\n"
            + "    var s = document.getElementById('s');\n"
            + "    var r = document.createRange();\n"
            + "    r.setStart(d.firstChild, 1);\n"
            + "    r.setEnd(d.childNodes[2], 2);\n"
            + "    alert(s.innerHTML);\n"
            + "    alert(r.toString());\n"
            + "    r.deleteContents();\n"
            + "    alert(document.getElementById('s'));\n"
            + "    alert(d.textContent);\n"
            + "  }\n"
            + "</script></body></html>";
        loadPageWithAlerts2(html);
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    @Alerts(DEFAULT = { "abcd", "bc", "null", "null", "ad" }, IE8 = { })
    public void deleteContents2() throws Exception {
        final String html =
            "<html><body><div id='d'><span id='a'>a</span><span id='b'>b</span><span id='c'>c</span>"
            + "<span id='d'>d</span></div><script>\n"
            + "  if (document.createRange) {\n"
            + "    var d = document.getElementById('d');\n"
            + "    var s = document.getElementById('s');\n"
            + "    var r = document.createRange();\n"
            + "    r.setStart(d, 1);\n"
            + "    r.setEnd(d, 3);\n"
            + "    alert(d.textContent);\n"
            + "    alert(r.toString());\n"
            + "    r.deleteContents();\n"
            + "    alert(document.getElementById('b'));\n"
            + "    alert(document.getElementById('c'));\n"
            + "    alert(d.textContent);\n"
            + "  }\n"
            + "</script></body></html>";
        loadPageWithAlerts2(html);
    }
}
