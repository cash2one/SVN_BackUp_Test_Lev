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
package com.gargoylesoftware.htmlunit.html;

import java.util.Locale;

import com.gargoylesoftware.htmlunit.SgmlPage;
import com.gargoylesoftware.htmlunit.WebAssert;
import com.gargoylesoftware.htmlunit.html.xpath.XPathUtils;
import com.gargoylesoftware.htmlunit.javascript.host.dom.Document;

/**
 * Intermediate base class for DOM Nodes that have namespaces. That includes HtmlElement and HtmlAttr.
 *
 * @version $Revision: 10304 $
 * @author David K. Taylor
 * @author Ahmed Ashour
 * @author Ronald Brill
 * @author Frank Danek
 */
public abstract class DomNamespaceNode extends DomNode {

    private String namespaceURI_;
    private String qualifiedName_;
    private final String localName_;
    private String prefix_;

    /**
     * Creates an instance of a DOM node that can have a namespace.
     *
     * @param namespaceURI the URI that identifies an XML namespace
     * @param qualifiedName the qualified name of the element type to instantiate
     * @param page the page that contains this element
     */
    protected DomNamespaceNode(final String namespaceURI, final String qualifiedName, final SgmlPage page) {
        super(page);
        WebAssert.notNull("qualifiedName", qualifiedName);
        qualifiedName_ = qualifiedName;

        if (qualifiedName.indexOf(':') != -1) {
            namespaceURI_ = namespaceURI;
            final int colonPosition = qualifiedName_.indexOf(':');
            localName_ = qualifiedName_.substring(colonPosition + 1);
            prefix_ = qualifiedName_.substring(0, colonPosition);
        }
        else {
            namespaceURI_ = namespaceURI;
            localName_ = qualifiedName_;
            prefix_ = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNamespaceURI() {
        if (getPage().isHtmlPage() && !(getPage() instanceof XHtmlPage)
            && HTMLParser.XHTML_NAMESPACE.equals(namespaceURI_)
            && XPathUtils.isProcessingXPath()) {
            // for Xalan processing we have to strip the 'default' XHTML namespace for HTML pages to be able to find
            // the elements by XPath without needing to add the namespace to it
            return null;
        }
        return namespaceURI_;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLocalName() {
        final boolean caseSensitive = getPage().hasCaseSensitiveTagNames();
        if (!caseSensitive && XPathUtils.isProcessingXPath()) { // and this method was called from Xalan
            return localName_.toLowerCase(Locale.ENGLISH);
        }
        return localName_;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPrefix() {
        return prefix_;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setPrefix(final String prefix) {
        prefix_ = prefix;
        if (prefix_ != null && localName_ != null) {
            qualifiedName_ = prefix_ + ":" + localName_;
        }
    }

    /**
     * Returns this node's qualified name.
     * @return this node's qualified name
     */
    public String getQualifiedName() {
        return qualifiedName_;
    }

   /**
    * {@inheritDoc}
    */
    @Override
    public void processImportNode(final Document doc) {
        super.processImportNode(doc);

        // if we importing from an namespace aware source
        // we have to drop the XHtmlNamespace because we did this already
        // for the html document itself
        final SgmlPage page = (SgmlPage) doc.getDomNodeOrDie();
        if (page.isHtmlPage() && !(page instanceof XHtmlPage)) {
            if (HTMLParser.XHTML_NAMESPACE.equals(namespaceURI_)) {
                namespaceURI_ = null;
            }
        }
    }
}
