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

import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_ENUMERATOR_CONSTRUCTOR_THROWS;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.IE;

import com.gargoylesoftware.htmlunit.javascript.SimpleScriptable;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxClass;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxConstructor;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxFunction;
import com.gargoylesoftware.htmlunit.javascript.configuration.WebBrowser;

import net.sourceforge.htmlunit.corejs.javascript.Context;
import net.sourceforge.htmlunit.corejs.javascript.Undefined;

/**
 * A JavaScript object for Enumerator.
 *
 * @version $Revision: 10780 $
 * @author Ahmed Ashour
 * @see <a href="http://msdn.microsoft.com/en-us/library/6ch9zb09.aspx">MSDN Documentation</a>
 */
@JsxClass(browsers = @WebBrowser(IE))
public class Enumerator extends SimpleScriptable {

    private int index_;

    private HTMLCollection collection_;

    /**
     * Creates an instance. JavaScript objects must have a default constructor.
     */
    public Enumerator() {
        // Empty.
    }

    /**
     * JavaScript constructor.
     * @param o the object to enumerate over
     */
    @JsxConstructor
    public void jsConstructor(final Object o) {
        if (Undefined.instance == o) {
            collection_ = HTMLCollection.emptyCollection(getWindow());
        }
        else if (getBrowserVersion().hasFeature(JS_ENUMERATOR_CONSTRUCTOR_THROWS)) {
            throw Context.reportRuntimeError("TypeError: object is not enumerable");
        }
        else if (o instanceof HTMLCollection) {
            collection_ = (HTMLCollection) o;
        }
        else if (o instanceof HTMLFormElement) {
            collection_ = ((HTMLFormElement) o).getElements();
        }
        else {
            throw Context.reportRuntimeError("TypeError: object is not enumerable (" + String.valueOf(o) + ")");
        }
    }

    /**
     * Returns whether the enumerator is at the end of the collection or not.
     * @return whether the enumerator is at the end of the collection or not
     */
    @JsxFunction
    public boolean atEnd() {
        return index_ >= collection_.getLength();
    }

    /**
     * Returns the current item in the collection.
     * @return the current item in the collection
     */
    @JsxFunction
    public Object item() {
        if (!atEnd()) {
            SimpleScriptable scriptable = (SimpleScriptable) collection_.get(index_, collection_);
            scriptable = scriptable.clone();
            scriptable.setCaseSensitive(false);
            return scriptable;
        }
        return Undefined.instance;
    }

    /**
     * Resets the current item in the collection to the first item.
     */
    @JsxFunction
    public void moveFirst() {
        index_ = 0;
    }

    /**
     * Moves the current item to the next item in the collection.
     */
    @JsxFunction
    public void moveNext() {
        index_++;
    }
}
