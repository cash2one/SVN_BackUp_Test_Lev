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

import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_ATTRIBUTES_BY_NAME_CASE_SENSITIVE;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_ATTRIBUTES_CONTAINS_EMPTY_ATTR_FOR_PROPERTIES;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.CHROME;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.FF;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.IE;

import com.gargoylesoftware.htmlunit.html.DomAttr;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxClass;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxClasses;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxConstructor;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxFunction;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxGetter;
import com.gargoylesoftware.htmlunit.javascript.configuration.WebBrowser;
import com.gargoylesoftware.htmlunit.javascript.host.dom.Attr;
import com.gargoylesoftware.htmlunit.javascript.host.dom.Node;

import net.sourceforge.htmlunit.corejs.javascript.Context;
import net.sourceforge.htmlunit.corejs.javascript.Scriptable;

/**
 * A collection of nodes that can be accessed by name. String comparisons in this class are case-insensitive when
 * used with an {@link HtmlElement},
 * but case-sensitive when used with a {@link DomElement}.
 *
 * @version $Revision: 10927 $
 * @author Daniel Gredler
 * @author Ahmed Ashour
 * @author Marc Guillemot
 * @author Ronald Brill
 * @author Frank Danek
 * @see <a href="http://www.w3.org/TR/DOM-Level-2-Core/core.html#ID-1780488922">DOM Level 2 Core Spec</a>
 * @see <a href="http://msdn2.microsoft.com/en-us/library/ms763824.aspx">IXMLDOMNamedNodeMap</a>
 */
@JsxClasses({
        @JsxClass(browsers = { @WebBrowser(CHROME), @WebBrowser(value = IE, minVersion = 11),
            @WebBrowser(value = FF, minVersion = 38) }),
        @JsxClass(isJSObject = false, isDefinedInStandardsMode = false,
            browsers = @WebBrowser(value = FF, maxVersion = 31)),
        @JsxClass(isJSObject = false, browsers = @WebBrowser(value = IE, maxVersion = 8))
    })
public class NamedNodeMap extends SimpleScriptable implements ScriptableWithFallbackGetter {

    private final org.w3c.dom.NamedNodeMap attributes_;

    /**
     * We need default constructors to build the prototype instance.
     */
    @JsxConstructor({ @WebBrowser(CHROME), @WebBrowser(value = FF, minVersion = 38) })
    public NamedNodeMap() {
        attributes_ = null;
    }

    /**
     * Creates a new named node map for the specified element.
     *
     * @param element the owning element
     */
    public NamedNodeMap(final DomElement element) {
        setParentScope(element.getScriptObject());
        setPrototype(getPrototype(getClass()));

        attributes_ = element.getAttributes();
        setDomNode(element, false);
    }

    /**
     * Returns the element at the specified index, or <tt>NOT_FOUND</tt> if the index is invalid.
     *
     * {@inheritDoc}
     */
    @Override
    public final Object get(final int index, final Scriptable start) {
        final NamedNodeMap startMap = (NamedNodeMap) start;
        final Object response = startMap.item(index);
        if (response != null) {
            return response;
        }
        return NOT_FOUND;
    }

    /**
     * Returns the element with the specified name, or <tt>NOT_FOUND</tt> if the name is invalid.
     *
     * {@inheritDoc}
     */
    @Override
    public Object getWithFallback(final String name) {
        final Object response = getNamedItem(name);
        if (response != null) {
            if (response instanceof Attr && getBrowserVersion().hasFeature(JS_ATTRIBUTES_BY_NAME_CASE_SENSITIVE)) {
                final Attr attr = (Attr) response;
                if (attr.getName().equals(name)) {
                    return response;
                }
            }
            else {
                return response;
            }
        }
        if (getDomNodeOrNull() != null && useRecursiveAttributeForIE() && isRecursiveAttribute(name)) {
            return getUnspecifiedAttributeNode(name);
        }

        return NOT_FOUND;
    }

    /**
     * <span style="color:red">INTERNAL API - SUBJECT TO CHANGE AT ANY TIME - USE AT YOUR OWN RISK.</span><br>
     *
     * Gets the specified attribute but does not handle the synthetic class attribute for IE.
     * @see #getNamedItem(String)
     *
     * @param name attribute name
     * @return the attribute node, {@code null} if the attribute is not defined
     */
    public Object getNamedItemWithoutSytheticClassAttr(final String name) {
        if (attributes_ != null) {
            final DomNode attr = (DomNode) attributes_.getNamedItem(name);
            if (attr != null) {
                return attr.getScriptObject();
            }
            if (!"className".equals(name) && useRecursiveAttributeForIE() && isRecursiveAttribute(name)) {
                return getUnspecifiedAttributeNode(name);
            }
        }

        return null;
    }

    /**
     * Gets the specified attribute.
     * @param name attribute name
     * @return the attribute node, {@code null} if the attribute is not defined
     */
    @JsxFunction
    public Object getNamedItem(final String name) {
        final Object attr = getNamedItemWithoutSytheticClassAttr(name);
        if (null != attr) {
            return attr;
        }

        // for IE we have to add the synthetic class attribute
        if ("class".equals(name) && useRecursiveAttributeForIE()) {
            return getUnspecifiedAttributeNode(name);
        }

        return null;
    }

    /**
     * Sets the specified attribute.
     * @param node the attribute
     */
    @JsxFunction
    public void setNamedItem(final Node node) {
        attributes_.setNamedItem(node.getDomNodeOrDie());
    }

    /**
     * Removes the specified attribute.
     * @param name the name of the item to remove
     */
    @JsxFunction
    public void removeNamedItem(final String name) {
        attributes_.removeNamedItem(name);
    }

    /**
     * Returns the item at the specified index.
     * @param index the index
     * @return the item at the specified index
     */
    @JsxFunction
    public Object item(int index) {
        final DomNode attr = (DomNode) attributes_.item(index);
        if (attr != null) {
            return attr.getScriptObject();
        }
        if (useRecursiveAttributeForIE()) {
            index -= attributes_.getLength();
            final String name = getRecusiveAttributeNameAt(index);
            if (name != null) {
                return getUnspecifiedAttributeNode(name);
            }
        }
        return null;
    }

    private boolean useRecursiveAttributeForIE() {
        return getBrowserVersion().hasFeature(JS_ATTRIBUTES_CONTAINS_EMPTY_ATTR_FOR_PROPERTIES)
            && getDomNodeOrDie() instanceof HtmlElement;
    }

    /**
     * Creates a new unspecified attribute node.
     * @return a new unspecified attribute node
     */
    private Attr getUnspecifiedAttributeNode(final String attrName) {
        final HtmlElement domNode = (HtmlElement) getDomNodeOrDie();

        final DomAttr attr = domNode.getPage().createAttribute(attrName);
        domNode.setAttributeNode(attr);
        return (Attr) attr.getScriptObject();
    }

    /**
     * Returns the number of attributes in this named node map.
     * @return the number of attributes in this named node map
     */
    @JsxGetter
    public int getLength() {
        int length = attributes_.getLength();
        if (useRecursiveAttributeForIE()) {
            length += getRecursiveAttributesLength();
        }
        return length;
    }

    private boolean isRecursiveAttribute(final String name) {
        for (Scriptable object = getDomNodeOrDie().getScriptObject(); object != null;
            object = object.getPrototype()) {
            for (final Object id : object.getIds()) {
                if (name.equals(Context.toString(id))) {
                    return true;
                }
            }
        }
        return false;
    }

    private int getRecursiveAttributesLength() {
        int length = 0;
        for (Scriptable object = getDomNodeOrDie().getScriptObject(); object != null;
            object = object.getPrototype()) {
            length += object.getIds().length;
        }
        return length;
    }

    private String getRecusiveAttributeNameAt(final int index) {
        int i = 0;
        for (Scriptable object = getDomNodeOrDie().getScriptObject(); object != null;
            object = object.getPrototype()) {
            for (final Object id : object.getIds()) {
                if (i == index) {
                    return Context.toString(id);
                }
                i++;
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean has(final int index, final Scriptable start) {
        return index >= 0 && index < getLength();
    }
}
