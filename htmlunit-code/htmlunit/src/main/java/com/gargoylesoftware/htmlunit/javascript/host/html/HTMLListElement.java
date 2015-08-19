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

import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.HTMLLIST_LIMIT_COMPACT_TO_BOOLEAN;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_TYPE_ACCEPTS_ARBITRARY_VALUES;

import com.gargoylesoftware.htmlunit.javascript.configuration.JsxClass;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxGetter;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxSetter;

import net.sourceforge.htmlunit.corejs.javascript.Context;

/**
 * Base class for list-type elements (<tt>ul</tt>, <tt>ol</tt>, <tt>dir</tt>, etc).
 *
 * @version $Revision: 10780 $
 * @author Daniel Gredler
 * @author Frank Danek
 * @author Ronald Brill
 */
@JsxClass(isJSObject = false, isDefinedInStandardsMode = false)
public class HTMLListElement extends HTMLElement {

    /**
     * Returns the value of the <tt>compact</tt> attribute.
     * @return the value of the <tt>compact</tt> attribute
     */
    @JsxGetter
    public boolean getCompact() {
        return getDomNodeOrDie().hasAttribute("compact");
    }

    /**
     * Sets the value of the <tt>compact</tt> attribute.
     * @param compact the value of the <tt>compact</tt> attribute
     */
    @JsxSetter
    public void setCompact(final Object compact) {
        if (Context.toBoolean(compact)) {
            getDomNodeOrDie().setAttribute("compact", "");
        }
        else {
            getDomNodeOrDie().removeAttribute("compact");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getAttribute(final String attributeName, final Integer flags) {
        if ("compact".equals(attributeName) && getBrowserVersion().hasFeature(HTMLLIST_LIMIT_COMPACT_TO_BOOLEAN)) {
            return getCompact();
        }
        return super.getAttribute(attributeName, flags);
    }

    /**
     * Returns the value of the "type" property.
     * @return the value of the "type" property
     */
    protected String getType() {
        final boolean acceptArbitraryValues = getBrowserVersion().hasFeature(JS_TYPE_ACCEPTS_ARBITRARY_VALUES);

        final String type = getDomNodeOrDie().getAttribute("type");
        if (acceptArbitraryValues
            || "1".equals(type)
            || "a".equals(type)
            || "A".equals(type)
            || "i".equals(type)
            || "I".equals(type)) {
            return type;
        }
        return "";
    }

    /**
     * Sets the value of the "type" property.
     * @param type the value of the "type" property
     */
    protected void setType(final String type) {
        final boolean acceptArbitraryValues = getBrowserVersion().hasFeature(JS_TYPE_ACCEPTS_ARBITRARY_VALUES);
        if (acceptArbitraryValues
                || "1".equals(type)
                || "a".equals(type)
                || "A".equals(type)
                || "i".equals(type)
                || "I".equals(type)) {
            getDomNodeOrDie().setAttribute("type", type);
            return;
        }

        throw Context.reportRuntimeError("Cannot set the type property to invalid value: '" + type + "'");
    }
}
