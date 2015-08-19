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

import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_TEXT_AREA_GET_MAXLENGTH_MAX_INT;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_TEXT_AREA_GET_MAXLENGTH_UNDEFINED;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_TEXT_AREA_SET_COLS_NEGATIVE_THROWS_EXCEPTION;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_TEXT_AREA_SET_COLS_THROWS_EXCEPTION;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_TEXT_AREA_SET_MAXLENGTH_NEGATIVE_THROWS_EXCEPTION;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_TEXT_AREA_SET_ROWS_NEGATIVE_THROWS_EXCEPTION;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_TEXT_AREA_SET_ROWS_THROWS_EXCEPTION;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.TEXTAREA_CRNL;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.CHROME;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.FF;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.IE;

import java.util.regex.Pattern;

import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlTextArea;
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
 * The JavaScript object that represents a textarea.
 *
 * @version $Revision: 10780 $
 * @author <a href="mailto:mbowler@GargoyleSoftware.com">Mike Bowler</a>
 * @author Marc Guillemot
 * @author Chris Erskine
 * @author Ahmed Ashour
 * @author Daniel Gredler
 * @author Ronald Brill
 * @author Frank Danek
 * @author Carsten Steul
 */
@JsxClasses({
        @JsxClass(domClass = HtmlTextArea.class,
                browsers = { @WebBrowser(CHROME), @WebBrowser(FF), @WebBrowser(value = IE, minVersion = 11) }),
        @JsxClass(domClass = HtmlTextArea.class,
            isJSObject = false, browsers = @WebBrowser(value = IE, maxVersion = 8))
    })
public class HTMLTextAreaElement extends FormField {

    private static final Pattern NORMALIZE_VALUE_PATTERN = Pattern.compile("([^\\r])\\n");

    /**
     * Creates an instance.
     */
    @JsxConstructor({ @WebBrowser(CHROME), @WebBrowser(FF) })
    public HTMLTextAreaElement() {
    }

    /**
     * Returns the type of this input.
     * @return the type of this input
     */
    @Override
    public String getType() {
        return "textarea";
    }

    /**
     * Returns the value of the "value" attribute.
     * @return the value of the "value" attribute
     */
    @Override
    public String getValue() {
        String value = ((HtmlTextArea) getDomNodeOrDie()).getText();
        if (getBrowserVersion().hasFeature(TEXTAREA_CRNL)) {
            value = NORMALIZE_VALUE_PATTERN.matcher(value).replaceAll("$1\r\n");
        }
        return value;
    }

    /**
     * Sets the value of the "value" attribute.
     * @param value the new value
     */
    @Override
    public void setValue(final String value) {
        ((HtmlTextArea) getDomNodeOrDie()).setText(value);
    }

    /**
     * Returns the number of columns in this text area.
     * @return the number of columns in this text area
     */
    @JsxGetter
    public int getCols() {
        final String s = getDomNodeOrDie().getAttribute("cols");
        try {
            return Integer.parseInt(s);
        }
        catch (final NumberFormatException e) {
            return 20;
        }
    }

    /**
     * Sets the number of columns in this text area.
     * @param cols the number of columns in this text area
     */
    @JsxSetter
    public void setCols(final String cols) {
        int i;
        try {
            i = Float.valueOf(cols).intValue();
            if (i < 0) {
                if (getBrowserVersion().hasFeature(JS_TEXT_AREA_SET_COLS_NEGATIVE_THROWS_EXCEPTION)) {
                    throw new NumberFormatException("New value for cols '" + cols + "' is smaller than zero.");
                }
                getDomNodeOrDie().setAttribute("cols", null);
                return;
            }
        }
        catch (final NumberFormatException e) {
            if (getBrowserVersion().hasFeature(JS_TEXT_AREA_SET_COLS_THROWS_EXCEPTION)) {
                throw Context.throwAsScriptRuntimeEx(e);
            }
            return;
        }
        getDomNodeOrDie().setAttribute("cols", Integer.toString(i));
    }

    /**
     * Returns the number of rows in this text area.
     * @return the number of rows in this text area
     */
    @JsxGetter
    public int getRows() {
        final String s = getDomNodeOrDie().getAttribute("rows");
        try {
            return Integer.parseInt(s);
        }
        catch (final NumberFormatException e) {
            return 2;
        }
    }

    /**
     * Sets the number of rows in this text area.
     * @param rows the number of rows in this text area
     */
    @JsxSetter
    public void setRows(final String rows) {
        int i;
        try {
            i = new Float(rows).intValue();
            if (i < 0) {
                if (getBrowserVersion().hasFeature(JS_TEXT_AREA_SET_ROWS_NEGATIVE_THROWS_EXCEPTION)) {
                    throw new NumberFormatException("New value for rows '" + rows + "' is smaller than zero.");
                }
                getDomNodeOrDie().setAttribute("rows", null);
                return;
            }
        }
        catch (final NumberFormatException e) {
            if (getBrowserVersion().hasFeature(JS_TEXT_AREA_SET_ROWS_THROWS_EXCEPTION)) {
                throw Context.throwAsScriptRuntimeEx(e);
            }
            return;
        }
        getDomNodeOrDie().setAttribute("rows", Integer.toString(i));
    }

    /**
     * Returns the textarea's default value, used if the containing form gets reset.
     * @return the textarea's default value, used if the containing form gets reset
     * @see <a href="http://msdn.microsoft.com/en-us/library/ms533718.aspx">MSDN Documentation</a>
     */
    @JsxGetter
    public String getDefaultValue() {
        String value = ((HtmlTextArea) getDomNodeOrDie()).getDefaultValue();
        if (getBrowserVersion().hasFeature(TEXTAREA_CRNL)) {
            value = NORMALIZE_VALUE_PATTERN.matcher(value).replaceAll("$1\r\n");
        }
        return value;
    }

    /**
     * Sets the textarea's default value, used if the containing form gets reset.
     * @param defaultValue the textarea's default value, used if the containing form gets reset
     * @see <a href="http://msdn.microsoft.com/en-us/library/ms533718.aspx">MSDN Documentation</a>
     */
    @JsxSetter
    public void setDefaultValue(final String defaultValue) {
        ((HtmlTextArea) getDomNodeOrDie()).setDefaultValue(defaultValue);
    }

    /**
     * Gets the value of "textLength" attribute.
     * @return the text length
     */
    @JsxGetter({ @WebBrowser(CHROME), @WebBrowser(FF) })
    public int getTextLength() {
        return getValue().length();
    }

    /**
     * Gets the value of "selectionStart" attribute.
     * @return the selection start
     */
    @JsxGetter({ @WebBrowser(CHROME), @WebBrowser(FF), @WebBrowser(value = IE, minVersion = 11) })
    public int getSelectionStart() {
        return ((HtmlTextArea) getDomNodeOrDie()).getSelectionStart();
    }

    /**
     * Sets the value of "selectionStart" attribute.
     * @param start selection start
     */
    @JsxSetter({ @WebBrowser(CHROME), @WebBrowser(FF), @WebBrowser(value = IE, minVersion = 11) })
    public void setSelectionStart(final int start) {
        ((HtmlTextArea) getDomNodeOrDie()).setSelectionStart(start);
    }

    /**
     * Gets the value of "selectionEnd" attribute.
     * @return the selection end
     */
    @JsxGetter({ @WebBrowser(CHROME), @WebBrowser(FF), @WebBrowser(value = IE, minVersion = 11) })
    public int getSelectionEnd() {
        return ((HtmlTextArea) getDomNodeOrDie()).getSelectionEnd();
    }

    /**
     * Sets the value of "selectionEnd" attribute.
     * @param end selection end
     */
    @JsxSetter({ @WebBrowser(CHROME), @WebBrowser(FF), @WebBrowser(value = IE, minVersion = 11) })
    public void setSelectionEnd(final int end) {
        ((HtmlTextArea) getDomNodeOrDie()).setSelectionEnd(end);
    }

    /**
     * Sets the selected portion of this input element.
     * @param start the index of the first character to select
     * @param end the index of the character after the selection
     */
    @JsxFunction({ @WebBrowser(CHROME), @WebBrowser(FF), @WebBrowser(value = IE, minVersion = 11) })
    public void setSelectionRange(final int start, final int end) {
        setSelectionStart(start);
        setSelectionEnd(end);
    }

    /**
     * Selects this element.
     */
    @JsxFunction
    public void select() {
        ((HtmlTextArea) getDomNodeOrDie()).select();
    }

    /**
     * Gets the value of "readOnly" attribute.
     * @return the readOnly attribute
     */
    @JsxGetter
    public boolean getReadOnly() {
        return ((HtmlTextArea) getDomNodeOrDie()).isReadOnly();
    }

    /**
     * Sets the value of "readOnly" attribute.
     * @param readOnly the new value
     */
    @JsxSetter
    public void setReadOnly(final boolean readOnly) {
        ((HtmlTextArea) getDomNodeOrDie()).setReadOnly(readOnly);
    }

    /**
     * Returns the {@code dataFld} attribute.
     * @return the {@code dataFld} attribute
     */
    @JsxGetter(@WebBrowser(value = IE, maxVersion = 8))
    public String getDataFld() {
        throw Context.throwAsScriptRuntimeEx(new UnsupportedOperationException());
    }

    /**
     * Sets the {@code dataFld} attribute.
     * @param dataFld {@code dataFld} attribute
     */
    @JsxSetter(@WebBrowser(value = IE, maxVersion = 8))
    public void setDataFld(final String dataFld) {
        throw Context.throwAsScriptRuntimeEx(new UnsupportedOperationException());
    }

    /**
     * Returns the {@code dataFormatAs} attribute.
     * @return the {@code dataFormatAs} attribute
     */
    @JsxGetter(@WebBrowser(value = IE, maxVersion = 8))
    public String getDataFormatAs() {
        throw Context.throwAsScriptRuntimeEx(new UnsupportedOperationException());
    }

    /**
     * Sets the {@code dataFormatAs} attribute.
     * @param dataFormatAs {@code dataFormatAs} attribute
     */
    @JsxSetter(@WebBrowser(value = IE, maxVersion = 8))
    public void setDataFormatAs(final String dataFormatAs) {
        throw Context.throwAsScriptRuntimeEx(new UnsupportedOperationException());
    }

    /**
     * Returns the {@code dataSrc} attribute.
     * @return the {@code dataSrc} attribute
     */
    @JsxGetter(@WebBrowser(value = IE, maxVersion = 8))
    public String getDataSrc() {
        throw Context.throwAsScriptRuntimeEx(new UnsupportedOperationException());
    }

    /**
     * Sets the {@code dataSrc} attribute.
     * @param dataSrc {@code dataSrc} attribute
     */
    @JsxSetter(@WebBrowser(value = IE, maxVersion = 8))
    public void setDataSrc(final String dataSrc) {
        throw Context.throwAsScriptRuntimeEx(new UnsupportedOperationException());
    }

    /**
     * Returns the maximum number of characters in this text area.
     * @return the maximum number of characters in this text area
     */
    @JsxGetter
    public Object getMaxLength() {
        final String maxLength = getDomNodeOrDie().getAttribute("maxLength");
        if (DomElement.ATTRIBUTE_NOT_DEFINED == maxLength
                && getBrowserVersion().hasFeature(JS_TEXT_AREA_GET_MAXLENGTH_UNDEFINED)) {
            return Undefined.instance;
        }

        try {
            return Integer.parseInt(maxLength);
        }
        catch (final NumberFormatException e) {
            if (getBrowserVersion().hasFeature(JS_TEXT_AREA_GET_MAXLENGTH_MAX_INT)) {
                return Integer.MAX_VALUE;
            }
            if (getBrowserVersion().hasFeature(JS_TEXT_AREA_GET_MAXLENGTH_UNDEFINED)) {
                return maxLength;
            }
            return -1;
        }
    }

    /**
     * Sets maximum number of characters in this text area.
     * @param maxLength maximum number of characters in this text area.
     */
    @JsxSetter
    public void setMaxLength(final String maxLength) {
        try {
            final int i = Integer.parseInt(maxLength);

            if (i < 0 && getBrowserVersion().hasFeature(JS_TEXT_AREA_SET_MAXLENGTH_NEGATIVE_THROWS_EXCEPTION)) {
                throw Context.throwAsScriptRuntimeEx(
                    new NumberFormatException("New value for maxLength '" + maxLength + "' is smaller than zero."));
            }
            getDomNodeOrDie().setAttribute("maxLength", maxLength);
        }
        catch (final NumberFormatException e) {
            if (getBrowserVersion().hasFeature(JS_TEXT_AREA_GET_MAXLENGTH_UNDEFINED)) {
                getDomNodeOrDie().setAttribute("maxLength", maxLength);
                return;
            }

            getDomNodeOrDie().setAttribute("maxLength", "0");
            return;
        }
    }
}
