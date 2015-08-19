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
package com.gargoylesoftware.htmlunit.javascript.host.xml;

import com.gargoylesoftware.htmlunit.javascript.SimpleScriptable;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxFunction;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxGetter;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxSetter;
import com.gargoylesoftware.htmlunit.javascript.host.dom.Node;

/**
 * A JavaScript object for XSLTemplate.
 * @see <a href="http://msdn2.microsoft.com/en-us/library/ms767644.aspx">MSDN documentation</a>
 *
 * @version $Revision: 10304 $
 * @author Ahmed Ashour
 */
public class XSLTemplate extends SimpleScriptable {

    private Node stylesheet_;

    /**
     * Sets the Extensible Stylesheet Language (XSL) style sheet to compile into an XSL template.
     * @param node the Extensible Stylesheet Language (XSL) style sheet to compile into an XSL template
     */
    @JsxSetter
    public void setStylesheet(final Node node) {
        stylesheet_ = node;
    }

    /**
     * Returns the Extensible Stylesheet Language (XSL) style sheet to compile into an XSL template.
     * @return the Extensible Stylesheet Language (XSL) style sheet to compile into an XSL template
     */
    @JsxGetter
    public Node getStylesheet() {
        return stylesheet_;
    }

    /**
     * Creates a rental-model XSLProcessor object that will use this template.
     * @return the XSLTProcessor
     */
    @JsxFunction
    public XSLTProcessor createProcessor() {
        final XSLTProcessor processor = new XSLTProcessor();
        processor.setPrototype(getPrototype(processor.getClass()));
        processor.setParentScope(getParentScope());
        processor.importStylesheet(stylesheet_);
        return processor;
    }
}
