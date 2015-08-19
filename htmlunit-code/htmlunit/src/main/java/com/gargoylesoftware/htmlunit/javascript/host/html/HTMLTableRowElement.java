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

import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_INNER_HTML_READONLY_FOR_SOME_TAGS;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_INNER_TEXT_READONLY_FOR_TABLE;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_TABLE_ROW_DELETE_CELL_REQUIRES_INDEX;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_TABLE_ROW_SECTION_INDEX_BIG_INT_IF_UNATTACHED;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.CHROME;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.FF;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.IE;

import java.util.ArrayList;
import java.util.List;

import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTable;
import com.gargoylesoftware.htmlunit.html.HtmlTableRow;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxClass;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxClasses;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxConstructor;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxFunction;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxGetter;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxSetter;
import com.gargoylesoftware.htmlunit.javascript.configuration.WebBrowser;

import net.sourceforge.htmlunit.corejs.javascript.Context;
import net.sourceforge.htmlunit.corejs.javascript.Undefined;

/**
 * A JavaScript object representing a TR.
 *
 * @version $Revision: 10780 $
 * @author Marc Guillemot
 * @author Chris Erskine
 * @author Ahmed Ashour
 * @author Ronald Brill
 * @author Frank Danek
 */
@JsxClasses({
        @JsxClass(domClass = HtmlTableRow.class,
                browsers = { @WebBrowser(CHROME), @WebBrowser(FF), @WebBrowser(value = IE, minVersion = 11) }),
        @JsxClass(domClass = HtmlTableRow.class,
            isJSObject = false, browsers = @WebBrowser(value = IE, maxVersion = 8))
    })
public class HTMLTableRowElement extends HTMLTableComponent {

    private HTMLCollection cells_; // has to be a member to have equality (==) working

    /**
     * Creates an instance.
     */
    @JsxConstructor({ @WebBrowser(CHROME), @WebBrowser(FF) })
    public HTMLTableRowElement() {
    }

    /**
     * Returns the index of the row within the parent table.
     * @return the index of the row within the parent table
     * @see <a href="http://msdn.microsoft.com/en-us/library/ms534377.aspx">MSDN Documentation</a>
     */
    @JsxGetter
    public int getRowIndex() {
        final HtmlTableRow row = (HtmlTableRow) getDomNodeOrDie();
        final HtmlTable table = row.getEnclosingTable();
        if (table == null) { // a not attached document.createElement('TR')
            return -1;
        }
        return table.getRows().indexOf(row);
    }

    /**
     * Returns the index of the row within the enclosing thead, tbody or tfoot.
     * @return the index of the row within the enclosing thead, tbody or tfoot
     * @see <a href="http://msdn.microsoft.com/en-us/library/ms534621.aspx">MSDN Documentation</a>
     * @see <a href="http://www.w3.org/TR/2000/WD-DOM-Level-1-20000929/level-one-html.html#ID-79105901">
     * DOM Level 1</a>
     */
    @JsxGetter
    public int getSectionRowIndex() {
        DomNode row = getDomNodeOrDie();
        final HtmlTable table = ((HtmlTableRow) row).getEnclosingTable();
        if (table == null) { // a not attached document.createElement('TR')
            if (!getBrowserVersion().hasFeature(JS_TABLE_ROW_SECTION_INDEX_BIG_INT_IF_UNATTACHED)) {
                return -1;
            }
            // IE 6, 7 and 8 return really strange values: large integers that are not constants
            // as tests on different browsers give different results
            return 5461640;
        }
        int index = -1;
        while (row != null) {
            if (row instanceof HtmlTableRow) {
                ++index;
            }
            row = row.getPreviousSibling();
        }
        return index;
    }

    /**
     * Returns the cells in the row.
     * @return the cells in the row
     */
    @JsxGetter
    public Object getCells() {
        if (cells_ == null) {
            final HtmlTableRow row = (HtmlTableRow) getDomNodeOrDie();
            cells_ = new HTMLCollection(row, false, "cells") {
                @Override
                protected List<Object> computeElements() {
                    return new ArrayList<Object>(row.getCells());
                }
            };
        }
        return cells_;
    }

    /**
     * Returns the value of the <tt>bgColor</tt> attribute.
     * @return the value of the <tt>bgColor</tt> attribute
     * @see <a href="http://msdn.microsoft.com/en-us/library/ms533505.aspx">MSDN Documentation</a>
     */
    @JsxGetter
    public String getBgColor() {
        return getDomNodeOrDie().getAttribute("bgColor");
    }

    /**
     * Sets the value of the <tt>bgColor</tt> attribute.
     * @param bgColor the value of the <tt>bgColor</tt> attribute
     * @see <a href="http://msdn.microsoft.com/en-us/library/ms533505.aspx">MSDN Documentation</a>
     */
    @JsxSetter
    public void setBgColor(final String bgColor) {
        setColorAttribute("bgColor", bgColor);
    }

    /**
     * Inserts a new cell at the specified index in the element's cells collection. If the index
     * is -1 or there is no index specified, then the cell is appended at the end of the
     * element's cells collection.
     * @see <a href="http://msdn.microsoft.com/en-us/library/ms536455.aspx">MSDN Documentation</a>
     * @param index specifies where to insert the cell in the tr.
     *        The default value is -1, which appends the new cell to the end of the cells collection
     * @return the newly-created cell
     */
    @JsxFunction
    public Object insertCell(final Object index) {
        int position = -1;
        if (index != Undefined.instance) {
            position = (int) Context.toNumber(index);
        }
        final HtmlTableRow htmlRow = (HtmlTableRow) getDomNodeOrDie();

        final boolean indexValid = position >= -1 && position <= htmlRow.getCells().size();
        if (indexValid) {
            final DomElement newCell = ((HtmlPage) htmlRow.getPage()).createElement("td");
            if (position == -1 || position == htmlRow.getCells().size()) {
                htmlRow.appendChild(newCell);
            }
            else {
                htmlRow.getCell(position).insertBefore(newCell);
            }
            return getScriptableFor(newCell);
        }
        throw Context.reportRuntimeError("Index or size is negative or greater than the allowed amount");
    }

    /**
     * Deletes the cell at the specified index in the element's cells collection. If the index
     * is -1 (or while simulating IE, when there is no index specified), then the last cell is deleted.
     * @see <a href="http://msdn.microsoft.com/en-us/library/ms536406.aspx">MSDN Documentation</a>
     * @see <a href="http://www.w3.org/TR/2003/REC-DOM-Level-2-HTML-20030109/html.html#ID-11738598">W3C DOM Level2</a>
     * @param index specifies the cell to delete.
     */
    @JsxFunction
    public void deleteCell(final Object index) {
        int position = -1;
        if (index != Undefined.instance) {
            position = (int) Context.toNumber(index);
        }
        else if (getBrowserVersion().hasFeature(JS_TABLE_ROW_DELETE_CELL_REQUIRES_INDEX)) {
            throw Context.reportRuntimeError("No enough arguments");
        }

        final HtmlTableRow htmlRow = (HtmlTableRow) getDomNodeOrDie();

        if (position == -1) {
            position = htmlRow.getCells().size() - 1;
        }
        final boolean indexValid = position >= -1 && position <= htmlRow.getCells().size();
        if (!indexValid) {
            throw Context.reportRuntimeError("Index or size is negative or greater than the allowed amount");
        }

        htmlRow.getCell(position).remove();
    }

    /**
     * Overwritten to throw an exception in IE8/9.
     * @param value the new value for replacing this node
     */
    @JsxSetter
    @Override
    public void setOuterHTML(final Object value) {
        throw Context.reportRuntimeError("outerHTML is read-only for tag 'tr'");
    }

    /**
     * Overwritten to throw an exception in IE8/9.
     * @param value the new value for the contents of this node
     */
    @JsxSetter
    @Override
    public void setInnerHTML(final Object value) {
        if (getBrowserVersion().hasFeature(JS_INNER_HTML_READONLY_FOR_SOME_TAGS)) {
            throw Context.reportRuntimeError("innerHTML is read-only for tag 'tr'");
        }
        super.setInnerHTML(value);
    }

    /**
     * Overwritten to throw an exception because this is readonly.
     * @param value the new value for the contents of this node
     */
    @Override
    protected void setInnerTextImpl(final String value) {
        if (getBrowserVersion().hasFeature(JS_INNER_TEXT_READONLY_FOR_TABLE)) {
            throw Context.reportRuntimeError("innerText is read-only for tag 'tr'");
        }
        super.setInnerTextImpl(value);
    }

    /**
     * Gets the "borderColor" attribute.
     * @return the attribute
     */
    @JsxGetter(@WebBrowser(IE))
    public String getBorderColor() {
        return getDomNodeOrDie().getAttribute("borderColor");
    }

    /**
     * Sets the "borderColor" attribute.
     * @param borderColor the new attribute
     */
    @JsxSetter(@WebBrowser(IE))
    public void setBorderColor(final String borderColor) {
        setColorAttribute("borderColor", borderColor);
    }

    /**
     * Gets the "borderColor" attribute.
     * @return the attribute
     */
    @JsxGetter(@WebBrowser(IE))
    public String getBorderColorDark() {
        return getDomNodeOrDie().getAttribute("borderColorDark");
    }

    /**
     * Sets the "borderColor" attribute.
     * @param borderColor the new attribute
     */
    @JsxSetter(@WebBrowser(IE))
    public void setBorderColorDark(final String borderColor) {
        setColorAttribute("borderColorDark", borderColor);
    }

    /**
     * Gets the "borderColor" attribute.
     * @return the attribute
     */
    @JsxGetter(@WebBrowser(IE))
    public String getBorderColorLight() {
        return getDomNodeOrDie().getAttribute("borderColorLight");
    }

    /**
     * Sets the "borderColor" attribute.
     * @param borderColor the new attribute
     */
    @JsxSetter(@WebBrowser(IE))
    public void setBorderColorLight(final String borderColor) {
        setColorAttribute("borderColorLight", borderColor);
    }
}
