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
package com.gargoylesoftware.htmlunit.javascript.host;

import java.util.ArrayList;
import java.util.List;

import com.gargoylesoftware.htmlunit.javascript.ScriptableWithFallbackGetter;
import com.gargoylesoftware.htmlunit.javascript.SimpleScriptable;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxClass;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxFunction;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxGetter;

import net.sourceforge.htmlunit.corejs.javascript.Context;
import net.sourceforge.htmlunit.corejs.javascript.Scriptable;

/**
 * A JavaScript object for simple array allowing access per key and index (like {@link MimeTypeArray}).
 *
 * @version $Revision: 10780 $
 * @author Marc Guillemot
 *
 * @see <a href="http://www.xulplanet.com/references/objref/MimeTypeArray.html">XUL Planet</a>
 */
@JsxClass(isJSObject = false, isDefinedInStandardsMode = false)
public class SimpleArray extends SimpleScriptable implements ScriptableWithFallbackGetter {
    private final List<Object> elements_ = new ArrayList<>();

    /**
     * Creates an instance. JavaScript objects must have a default constructor.
     */
    public SimpleArray() {
        // nothing
    }

    /**
     * Returns the item at the given index.
     * @param index the index
     * @return the item at the given position
     */
    @JsxFunction
    public Object item(final int index) {
        return elements_.get(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getWithFallback(final String name) {
        final Object response = namedItem(name);
        if (response != null) {
            return response;
        }
        return Context.getUndefinedValue();
    }

    /**
     * Returns the element at the specified index, or <tt>NOT_FOUND</tt> if the
     * index is invalid.
     * {@inheritDoc}
     */
    @Override
    public final Object get(final int index, final Scriptable start) {
        final SimpleArray array = (SimpleArray) start;
        final List<Object> elements = array.elements_;

        if (index >= 0 && index < elements.size()) {
            return elements.get(index);
        }
        return null;
    }

    /**
     * Returns the item at the given index.
     * @param name the item name
     * @return the item with the given name
     */
    @JsxFunction
    public Object namedItem(final String name) {
        for (final Object element : elements_) {
            if (name.equals(getItemName(element))) {
                return element;
            }
        }
        return null;
    }

    /**
     * Gets the name of the element.
     * Should be abstract but current implementation of prototype configuration doesn't allow it.
     * @param element the array's element
     * @return the element's name
     */
    protected String getItemName(final Object element) {
        return null;
    }

    /**
     * Gets the array size.
     * @return the number elements
     */
    @JsxGetter
    public int getLength() {
        return elements_.size();
    }

    /**
     * Adds an element.
     * @param element the element to add
     */
    void add(final Object element) {
        elements_.add(element);
    }
}
