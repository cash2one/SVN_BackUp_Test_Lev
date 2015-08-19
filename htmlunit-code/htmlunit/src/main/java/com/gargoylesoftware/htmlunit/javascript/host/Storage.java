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

import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_STORAGE_GET_FROM_ITEMS;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_STORAGE_PRESERVED_INCLUDED;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.CHROME;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.FF;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.IE;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.gargoylesoftware.htmlunit.javascript.SimpleScriptable;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxClass;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxClasses;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxConstructor;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxFunction;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxGetter;
import com.gargoylesoftware.htmlunit.javascript.configuration.WebBrowser;

import net.sourceforge.htmlunit.corejs.javascript.Context;
import net.sourceforge.htmlunit.corejs.javascript.Scriptable;

/**
 * The JavaScript object that represents a Storage.
 *
 * @version $Revision: 10780 $
 * @author Ahmed Ashour
 * @author Marc Guillemot
 */
@JsxClasses({
        @JsxClass(browsers = { @WebBrowser(CHROME), @WebBrowser(FF), @WebBrowser(value = IE, minVersion = 11) }),
        @JsxClass(isJSObject = false, browsers = @WebBrowser(value = IE, maxVersion = 8))
    })
public class Storage extends SimpleScriptable {

    private static List<String> RESERVED_NAMES_ = Arrays.asList("clear", "key", "getItem", "length", "removeItem",
        "setItem", "constructor", "toString", "toLocaleString", "valueOf", "hasOwnProperty", "propertyIsEnumerable",
        "isPrototypeOf", "__defineGetter__", "__defineSetter__", "__lookupGetter__", "__lookupSetter__");

    private final Map<String, String> store_;

    /**
     * Public default constructor only for the prototype.
     */
    @JsxConstructor({ @WebBrowser(CHROME), @WebBrowser(value = FF, minVersion = 38) })
    public Storage() {
        store_ = null;
    }

    /**
     * Constructor.
     * @param window the parent scope
     * @param store the storage itself
     */
    public Storage(final Window window, final Map<String, String> store) {
        store_ = store;
        setParentScope(window);
        setPrototype(window.getPrototype(Storage.class));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(final String name, final Scriptable start, final Object value) {
        final boolean isReserved = RESERVED_NAMES_.contains(name);
        if (store_ == null || isReserved) {
            super.put(name, start, value);
        }
        if (store_ != null && (!isReserved || getBrowserVersion().hasFeature(JS_STORAGE_PRESERVED_INCLUDED))) {
            setItem(name, Context.toString(value));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object get(final String name, final Scriptable start) {
        if (store_ == null
                || (RESERVED_NAMES_.contains(name) && !getBrowserVersion().hasFeature(JS_STORAGE_GET_FROM_ITEMS))) {
            return super.get(name, start);
        }
        final Object value = getItem(name);
        if (value != null) {
            return value;
        }
        return super.get(name, start);
    }

    /**
     * Returns the length property.
     * @return the length property
     */
    @JsxGetter
    public int getLength() {
        return getMap().size();
    }

    /**
     * Removes the specified key.
     * @param key the item key
     */
    @JsxFunction
    public void removeItem(final String key) {
        getMap().remove(key);
    }

    /**
     * Returns the key of the specified index.
     * @param index the index
     * @return the key
     */
    @JsxFunction
    public String key(final int index) {
        int counter = 0;
        for (final String key : getMap().keySet()) {
            if (counter++ == index) {
                return key;
            }
        }
        return null;
    }

    private Map<String, String> getMap() {
        return store_;
    }

    /**
     * Returns the value of the specified key.
     * @param key the item key
     * @return the value
     */
    @JsxFunction
    public Object getItem(final String key) {
        return getMap().get(key);
    }

    /**
     * Sets the item value.
     * @param key the item key
     * @param data the value
     */
    @JsxFunction
    public void setItem(final String key, final String data) {
        getMap().put(key, data);
    }

    /**
     * Clears all items.
     */
    @JsxFunction
    public void clear() {
        getMap().clear();
    }
}
