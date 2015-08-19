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

import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.HTMLCOLLECTION_COMMENT_IS_ELEMENT;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.HTMLCOLLECTION_EXCEPTION_FOR_NEGATIVE_INDEX;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.HTMLCOLLECTION_ITEM_SUPPORTS_DOUBLE_INDEX_ALSO;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.HTMLCOLLECTION_ITEM_SUPPORTS_ID_SEARCH_ALSO;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.HTMLCOLLECTION_OBJECT_DETECTION;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_NODE_LIST_ENUMERATE_FUNCTIONS;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.CHROME;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.FF;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.IE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.gargoylesoftware.htmlunit.html.DomComment;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.javascript.configuration.JavaScriptConfiguration;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxClass;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxClasses;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxConstructor;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxFunction;
import com.gargoylesoftware.htmlunit.javascript.configuration.WebBrowser;
import com.gargoylesoftware.htmlunit.javascript.host.Window;
import com.gargoylesoftware.htmlunit.javascript.host.dom.AbstractList;

import net.sourceforge.htmlunit.corejs.javascript.Context;
import net.sourceforge.htmlunit.corejs.javascript.ScriptRuntime;
import net.sourceforge.htmlunit.corejs.javascript.Scriptable;
import net.sourceforge.htmlunit.corejs.javascript.ScriptableObject;

/**
 * An array of elements. Used for the element arrays returned by <tt>document.all</tt>,
 * <tt>document.all.tags('x')</tt>, <tt>document.forms</tt>, <tt>window.frames</tt>, etc.
 * Note that this class must not be used for collections that can be modified, for example
 * <tt>map.areas</tt> and <tt>select.options</tt>.
 * <br>
 * This class (like all classes in this package) is specific for the JavaScript engine.
 * Users of HtmlUnit shouldn't use it directly.
 *
 * @version $Revision: 10891 $
 * @author Daniel Gredler
 * @author Marc Guillemot
 * @author Chris Erskine
 * @author Ahmed Ashour
 * @author Frank Danek
 */
@JsxClasses({
        @JsxClass(browsers = { @WebBrowser(CHROME), @WebBrowser(FF), @WebBrowser(value = IE, minVersion = 11) }),
        @JsxClass(isJSObject = false, browsers = @WebBrowser(value = IE, maxVersion = 8))
    })
public class HTMLCollection extends AbstractList {

    /**
     * IE provides a way of enumerating through some element collections; this counter supports that functionality.
     */
    private int currentIndex_ = 0;

    /**
     * Creates an instance.
     */
    @JsxConstructor({ @WebBrowser(CHROME), @WebBrowser(FF) })
    public HTMLCollection() {
    }

    /**
     * Creates an instance.
     * @param parentScope parent scope
     */
    private HTMLCollection(final ScriptableObject parentScope) {
        setParentScope(parentScope);
        setPrototype(getPrototype(getClass()));
    }

    /**
     * Creates an instance.
     * @param parentScope parent scope
     * @param attributeChangeSensitive indicates if the content of the collection may change when an attribute
     * of a descendant node of parentScope changes (attribute added, modified or removed)
     * @param description a text useful for debugging
     */
    public HTMLCollection(final DomNode parentScope, final boolean attributeChangeSensitive, final String description) {
        super(parentScope, attributeChangeSensitive, description);
    }

    /**
     * Constructs an instance with an initial cache value.
     * @param parentScope the parent scope, on which we listen for changes
     * @param initialElements the initial content for the cache
     */
    HTMLCollection(final DomNode parentScope, final List<?> initialElements) {
        super(parentScope, initialElements);
    }

    /**
     * Gets an empty collection.
     * @param window the current scope
     * @return an empty collection
     */
    public static HTMLCollection emptyCollection(final Window window) {
        final List<Object> list = Collections.emptyList();
        return new HTMLCollection(window) {
            @Override
            public List<Object> getElements() {
                return list;
            }
        };
    }

    /**
     * Returns the elements whose associated host objects are available through this collection.
     * @return the elements whose associated host objects are available through this collection
     */
    @Override
    protected List<Object> computeElements() {
        final List<Object> response = new ArrayList<>();
        final DomNode domNode = getDomNodeOrNull();
        if (domNode == null) {
            return response;
        }
        final boolean commentIsElement = getBrowserVersion().hasFeature(HTMLCOLLECTION_COMMENT_IS_ELEMENT);
        for (final DomNode node : getCandidates()) {
            if ((node instanceof DomElement
                    || (commentIsElement && node instanceof DomComment)) && isMatching(node)) {
                response.add(node);
            }
        }
        return response;
    }

    /**
     * Gets the DOM node that have to be examined to see if they are matching.
     * Default implementation looks at all descendants of reference node.
     * @return the nodes
     */
    @Override
    protected Iterable<DomNode> getCandidates() {
        final DomNode domNode = getDomNodeOrNull();
        return domNode.getDescendants();
    }

    /**
     * Indicates if the node should belong to the collection.
     * Belongs to the refactoring effort to improve HTMLCollection's performance.
     * @param node the node to test. Will be a child node of the reference node.
     * @return {@code false} here as subclasses for concrete collections should decide it.
     */
    @Override
    protected boolean isMatching(final DomNode node) {
        return false;
    }

    /**
     * Returns the element or elements that match the specified key. If it is the name
     * of a property, the property value is returned. If it is the id of an element in
     * the array, that element is returned. Finally, if it is the name of an element or
     * elements in the array, then all those elements are returned. Otherwise,
     * {@link #NOT_FOUND} is returned.
     * {@inheritDoc}
     */
    @Override
    protected Object getWithPreemption(final String name) {
        // Test to see if we are trying to get the length of this collection?
        // If so return NOT_FOUND here to let the property be retrieved using the prototype
        if (/*xpath_ == null || */"length".equals(name)) {
            return NOT_FOUND;
        }

        final List<Object> elements = getElements();

        // See if there is an element in the element array with the specified id.
        final List<Object> matchingElements = new ArrayList<>();

        for (final Object next : elements) {
            if (next instanceof DomElement) {
                final String id = ((DomElement) next).getAttribute("id");
                if (name.equals(id)) {
                    matchingElements.add(next);
                }
            }
        }

        if (matchingElements.size() == 1) {
            return getScriptableForElement(matchingElements.get(0));
        }
        else if (!matchingElements.isEmpty()) {
            final HTMLCollection collection = new HTMLCollection(getDomNodeOrDie(), matchingElements);
            collection.setAvoidObjectDetection(
                    !getBrowserVersion().hasFeature(HTMLCOLLECTION_OBJECT_DETECTION));
            return collection;
        }

        // no element found by id, let's search by name
        for (final Object next : elements) {
            if (next instanceof DomElement) {
                final String nodeName = ((DomElement) next).getAttribute("name");
                if (name.equals(nodeName)) {
                    matchingElements.add(next);
                }
            }
        }

        if (matchingElements.isEmpty()) {
            if (getBrowserVersion().hasFeature(HTMLCOLLECTION_ITEM_SUPPORTS_DOUBLE_INDEX_ALSO)) {
                final Double doubleValue = Context.toNumber(name);
                if (ScriptRuntime.NaN != doubleValue && !doubleValue.isNaN()) {
                    final Object object = get(doubleValue.intValue(), this);
                    if (object != NOT_FOUND) {
                        return object;
                    }
                }
            }
            return NOT_FOUND;
        }
        else if (matchingElements.size() == 1) {
            return getScriptableForElement(matchingElements.get(0));
        }

        // many elements => build a sub collection
        final DomNode domNode = getDomNodeOrNull();
        final HTMLCollection collection = new HTMLCollection(domNode, matchingElements);
        collection.setAvoidObjectDetection(!getBrowserVersion().hasFeature(HTMLCOLLECTION_OBJECT_DETECTION));
        return collection;
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
        if (index instanceof String && getBrowserVersion().hasFeature(HTMLCOLLECTION_ITEM_SUPPORTS_ID_SEARCH_ALSO)) {
            final String name = (String) index;
            final Object result = namedItem(name);
            return result;
        }

        int idx = 0;
        final Double doubleValue = Context.toNumber(index);
        if (ScriptRuntime.NaN != doubleValue && !doubleValue.isNaN()) {
            idx = doubleValue.intValue();
        }

        if (idx < 0 && getBrowserVersion().hasFeature(HTMLCOLLECTION_EXCEPTION_FOR_NEGATIVE_INDEX)) {
            throw Context.reportRuntimeError("Invalid index.");
        }

        final Object object = get(idx, this);
        if (object == NOT_FOUND) {
            return null;
        }
        return object;
    }

    /**
     * Retrieves the item or items corresponding to the specified name (checks ids, and if
     * that does not work, then names).
     * @param name the name or id the element or elements to return
     * @return the element or elements corresponding to the specified name or id
     * @see <a href="http://msdn.microsoft.com/en-us/library/ms536634.aspx">MSDN doc</a>
     */
    @JsxFunction
    public Object namedItem(final String name) {
        final List<Object> elements = getElements();
        for (final Object next : elements) {
            if (next instanceof DomElement) {
                final DomElement elem = (DomElement) next;
                final String nodeName = elem.getAttribute("name");
                if (name.equals(nodeName)) {
                    return getScriptableForElement(elem);
                }

                final String id = elem.getAttribute("id");
                if (name.equals(id)) {
                    return getScriptableForElement(elem);
                }
            }
        }
        return null;
    }

    /**
     * Returns the next node in the collection (supporting iteration in IE only).
     * @return the next node in the collection
     */
    @JsxFunction(@WebBrowser(IE))
    public Object nextNode() {
        Object nextNode;
        final List<Object> elements = getElements();
        if (currentIndex_ >= 0 && currentIndex_ < elements.size()) {
            nextNode = elements.get(currentIndex_);
        }
        else {
            nextNode = null;
        }
        currentIndex_++;
        return nextNode;
    }

    /**
     * Resets the node iterator accessed via {@link #nextNode()}.
     */
    @JsxFunction(@WebBrowser(IE))
    public void reset() {
        currentIndex_ = 0;
    }

    /**
     * Returns all the elements in this element array that have the specified tag name.
     * This method returns an empty element array if there are no elements with the
     * specified tag name.
     * @param tagName the name of the tag of the elements to return
     * @return all the elements in this element array that have the specified tag name
     * @see <a href="http://msdn.microsoft.com/en-us/library/ms536776.aspx">MSDN doc</a>
     */
    @JsxFunction(@WebBrowser(IE))
    public Object tags(final String tagName) {
        final HTMLCollection collection = new HTMLSubCollection(this, ".tags('" + tagName + "')") {
            @Override
            protected boolean isMatching(final DomNode node) {
                return tagName.equalsIgnoreCase(node.getLocalName());
            }
        };
        return collection;
    }

    /**
     * Called for the js "==".
     * {@inheritDoc}
     */
    @Override
    protected Object equivalentValues(final Object other) {
        if (other == this) {
            return Boolean.TRUE;
        }
        else if (other instanceof HTMLCollection) {
            final HTMLCollection otherArray = (HTMLCollection) other;
            final DomNode domNode = getDomNodeOrNull();
            final DomNode domNodeOther = otherArray.getDomNodeOrNull();
            if (getClass() == other.getClass()
                    && domNode == domNodeOther
                    && getElements().equals(otherArray.getElements())) {
                return Boolean.TRUE;
            }
            return NOT_FOUND;
        }

        return super.equivalentValues(other);
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public Object[] getIds() {
        // let's Rhino work normally if current instance is the prototype
        if (isPrototype()) {
            return super.getIds();
        }

        final List<String> idList = new ArrayList<>();
        final List<Object> elements = getElements();

        if (getBrowserVersion().hasFeature(JS_NODE_LIST_ENUMERATE_FUNCTIONS)) {
            final int length = elements.size();
            for (int i = 0; i < length; i++) {
                idList.add(Integer.toString(i));
            }

            idList.add("length");
            final JavaScriptConfiguration jsConfig = getWindow().getWebWindow().getWebClient()
                .getJavaScriptEngine().getJavaScriptConfiguration();
            for (final String name : jsConfig.getClassConfiguration(getClassName()).getFunctionKeys()) {
                idList.add(name);
            }
        }
        else {
            idList.add("length");
            addElementIds(idList, elements);
        }
        return idList.toArray();
    }

    private boolean isPrototype() {
        return !(getPrototype() instanceof HTMLCollection);
    }

    /**
     * Adds the ids of the collection's elements to the idList.
     * @param idList the list to add the ids to
     * @param elements the collection's elements
     */
    @Override
    protected void addElementIds(final List<String> idList, final List<Object> elements) {
        int index = 0;
        for (final Object next : elements) {
            final HtmlElement element = (HtmlElement) next;
            final String name = element.getAttribute("name");
            if (name != DomElement.ATTRIBUTE_NOT_DEFINED) {
                idList.add(name);
            }
            else {
                final String id = element.getId();
                if (id != DomElement.ATTRIBUTE_NOT_DEFINED) {
                    idList.add(id);
                }
                else {
                    idList.add(Integer.toString(index));
                }
            }
            index++;
        }
    }

    /**
     * Gets the scriptable for the provided element that may already be the right scriptable.
     * @param object the object for which to get the scriptable
     * @return the scriptable
     */
    @Override
    protected Scriptable getScriptableForElement(final Object object) {
        if (object instanceof Scriptable) {
            return (Scriptable) object;
        }
        return getScriptableFor(object);
    }
}

class HTMLSubCollection extends HTMLCollection {
    private final HTMLCollection mainCollection_;

    public HTMLSubCollection(final HTMLCollection mainCollection, final String subDescription) {
        super(mainCollection.getDomNodeOrDie(), false, mainCollection.toString() + subDescription);
        mainCollection_ = mainCollection;
    }

    @Override
    protected List<Object> computeElements() {
        final List<Object> list = new ArrayList<>();
        for (final Object o : mainCollection_.getElements()) {
            if (isMatching((DomNode) o)) {
                list.add(o);
            }
        }
        return list;
    }
}
