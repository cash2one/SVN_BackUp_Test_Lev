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

import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.HTMLALLCOLLECTION;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.HTMLALLCOLLECTION_DEFAULT_DESCRIPTION;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.HTMLALLCOLLECTION_DO_NOT_CHECK_NAME;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.HTMLALLCOLLECTION_DO_NOT_CONVERT_STRINGS_TO_NUMBER;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.HTMLALLCOLLECTION_DO_NOT_SUPPORT_PARANTHESES;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.HTMLALLCOLLECTION_NO_COLLECTION_FOR_MANY_HITS;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.HTMLALLCOLLECTION_NULL_IF_ITEM_NOT_FOUND;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.HTMLALLCOLLECTION_NULL_IF_NAMED_ITEM_NOT_FOUND;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.HTMLCOLLECTION_EXCEPTION_FOR_NEGATIVE_INDEX;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.HTMLCOLLECTION_ITEM_FUNCT_SUPPORTS_DOUBLE_INDEX_ALSO;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.HTMLCOLLECTION_OBJECT_DETECTION;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.CHROME;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.FF;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.IE;

import java.util.ArrayList;
import java.util.List;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxClass;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxConstructor;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxFunction;
import com.gargoylesoftware.htmlunit.javascript.configuration.WebBrowser;

import net.sourceforge.htmlunit.corejs.javascript.Context;
import net.sourceforge.htmlunit.corejs.javascript.ScriptRuntime;
import net.sourceforge.htmlunit.corejs.javascript.Scriptable;
import net.sourceforge.htmlunit.corejs.javascript.Undefined;

/**
 * A special {@link HTMLCollection} for <code>document.all</code>.
 *
 * @version $Revision: 10927 $
 * @author Ronald Brill
 * @author Ahmed Ashour
 */
@JsxClass(browsers = { @WebBrowser(CHROME), @WebBrowser(value = FF, minVersion = 38),
    @WebBrowser(value = IE, minVersion = 11) })
public class HTMLAllCollection extends HTMLCollection {

    /**
     * Creates an instance.
     */
    @JsxConstructor({ @WebBrowser(CHROME), @WebBrowser(value = FF, minVersion = 38) })
    public HTMLAllCollection() {
    }

    /**
     * Creates an instance.
     * @param parentScope parent scope
     * @param description a text useful for debugging
     */
    public HTMLAllCollection(final DomNode parentScope, final String description) {
        super(parentScope, false, description);
    }

    /**
     * Returns the item or items corresponding to the specified index or key.
     * @param index the index or key corresponding to the element or elements to return
     * @return the element or elements corresponding to the specified index or key
     * @see <a href="http://msdn.microsoft.com/en-us/library/ms536460.aspx">MSDN doc</a>
     */
    @Override
    @JsxFunction
    public Object item(final Object index) {
        Double numb;

        BrowserVersion browser;
        if (index instanceof String) {
            final String name = (String) index;
            final Object result = namedItem(name);
            if (null != result && Undefined.instance != result) {
                return result;
            }
            numb = Double.NaN;

            browser = getBrowserVersion();
            if (!browser.hasFeature(HTMLALLCOLLECTION_DO_NOT_CONVERT_STRINGS_TO_NUMBER)) {
                numb = ScriptRuntime.toNumber(index);
            }
            if (ScriptRuntime.NaN == numb || numb.isNaN()) {
                return itemNotFound(browser);
            }
        }
        else {
            numb = ScriptRuntime.toNumber(index);
            browser = getBrowserVersion();
        }

        if (!browser.hasFeature(HTMLCOLLECTION_ITEM_FUNCT_SUPPORTS_DOUBLE_INDEX_ALSO)
                && (Double.isInfinite(numb) || numb != Math.floor(numb))) {
            return itemNotFound(browser);
        }

        if (numb < 0 && browser.hasFeature(HTMLCOLLECTION_EXCEPTION_FOR_NEGATIVE_INDEX)) {
            throw Context.reportRuntimeError("Invalid index.");
        }

        final Object object = get(numb.intValue(), this);
        if (object == NOT_FOUND) {
            return itemNotFound(browser);
        }
        return object;
    }

    private Object itemNotFound(final BrowserVersion browser) {
        if (browser.hasFeature(HTMLALLCOLLECTION_NULL_IF_ITEM_NOT_FOUND)) {
            return null;
        }
        return Undefined.instance;
    }

    @JsxFunction
    @Override
    public final Object namedItem(final String name) {
        final List<Object> elements = getElements();

        // See if there is an element in the element array with the specified id.
        final List<DomElement> matchingByName = new ArrayList<>();
        final List<DomElement> matchingById = new ArrayList<>();

        final BrowserVersion browser = getBrowserVersion();
        final boolean byName = !browser.hasFeature(HTMLALLCOLLECTION_DO_NOT_CHECK_NAME);
        for (final Object next : elements) {
            if (next instanceof DomElement) {
                final DomElement elem = (DomElement) next;
                final String nodeName = elem.getAttribute("name");
                if (byName && name.equals(nodeName)) {
                    matchingByName.add(elem);
                }
                else {
                    final String id = elem.getAttribute("id");
                    if (name.equals(id)) {
                        matchingById.add(elem);
                    }
                }
            }
        }
        matchingByName.addAll(matchingById);

        if (matchingByName.size() == 1
                || (matchingByName.size() > 1
                        && browser.hasFeature(HTMLALLCOLLECTION_NO_COLLECTION_FOR_MANY_HITS))) {
            return getScriptableForElement(matchingByName.get(0));
        }
        if (matchingByName.isEmpty()) {
            if (browser.hasFeature(HTMLALLCOLLECTION_NULL_IF_NAMED_ITEM_NOT_FOUND)) {
                return null;
            }
            return Undefined.instance;
        }

        // many elements => build a sub collection
        final DomNode domNode = getDomNodeOrNull();
        final HTMLCollection collection = new HTMLCollection(domNode, matchingByName);
        collection.setAvoidObjectDetection(!browser.hasFeature(HTMLCOLLECTION_OBJECT_DETECTION));
        return collection;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getDefaultValue(final Class<?> hint) {
        if ((String.class.equals(hint) || hint == null)
                && getBrowserVersion().hasFeature(HTMLALLCOLLECTION_DEFAULT_DESCRIPTION)) {
            return "[object HTML document.all class]";
        }
        return super.getDefaultValue(hint);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object call(final Context cx, final Scriptable scope, final Scriptable thisObj, final Object[] args) {
        if (getBrowserVersion().hasFeature(HTMLALLCOLLECTION_DO_NOT_SUPPORT_PARANTHESES)) {
            if (args.length == 0) {
                throw Context.reportRuntimeError("Zero arguments; need an index or a key.");
            }

            if (args[0] instanceof Number) {
                return null;
            }
        }
        return super.call(cx, scope, thisObj, args);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getClassName() {
        if (getWindow().getWebWindow() != null && !getBrowserVersion().hasFeature(HTMLALLCOLLECTION)) {
            return "HTMLCollection";
        }
        return super.getClassName();
    }
}
