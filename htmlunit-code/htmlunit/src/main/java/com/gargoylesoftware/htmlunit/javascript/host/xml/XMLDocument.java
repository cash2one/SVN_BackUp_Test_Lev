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

import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_DOMPARSER_EMPTY_STRING_IS_ERROR;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_DOMPARSER_EXCEPTION_ON_ERROR;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_DOMPARSER_PARSERERROR_ON_ERROR;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_XML_GET_ELEMENT_BY_ID__ANY_ELEMENT;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.CHROME;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.FF;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.IE;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.CDATASection;
import org.w3c.dom.ProcessingInstruction;

import com.gargoylesoftware.htmlunit.StringWebResponse;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.WebWindow;
import com.gargoylesoftware.htmlunit.html.DomAttr;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.javascript.SimpleScriptable;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxClass;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxConstructor;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxFunction;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxGetter;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxSetter;
import com.gargoylesoftware.htmlunit.javascript.configuration.WebBrowser;
import com.gargoylesoftware.htmlunit.javascript.host.Element;
import com.gargoylesoftware.htmlunit.javascript.host.dom.Attr;
import com.gargoylesoftware.htmlunit.javascript.host.dom.DOMException;
import com.gargoylesoftware.htmlunit.javascript.host.dom.Document;
import com.gargoylesoftware.htmlunit.javascript.host.html.HTMLCollection;
import com.gargoylesoftware.htmlunit.svg.SvgElement;
import com.gargoylesoftware.htmlunit.xml.XmlPage;

import net.sourceforge.htmlunit.corejs.javascript.Context;

/**
 * A JavaScript object for XMLDocument.
 *
 * @version $Revision: 10913 $
 * @author Ahmed Ashour
 * @author Marc Guillemot
 * @author Sudhan Moghe
 * @author Ronald Brill
 * @author Chuck Dumont
 * @author Frank Danek
 */
@JsxClass(browsers = { @WebBrowser(CHROME), @WebBrowser(FF), @WebBrowser(value = IE, minVersion = 11) })
public class XMLDocument extends Document {

    private static final Log LOG = LogFactory.getLog(XMLDocument.class);

    private boolean async_ = true;

    /**
     * Creates a new instance.
     */
    @JsxConstructor({ @WebBrowser(CHROME), @WebBrowser(FF) })
    public XMLDocument() {
        this(null);
    }

    /**
     * Creates a new instance, with associated XmlPage.
     * @param enclosingWindow the window
     */
    public XMLDocument(final WebWindow enclosingWindow) {
        if (enclosingWindow != null) {
            try {
                final XmlPage page = new XmlPage((WebResponse) null, enclosingWindow);
                setDomNode(page);
            }
            catch (final IOException e) {
                throw Context.reportRuntimeError("IOException: " + e);
            }
        }
    }

    /**
     * Sets the <tt>async</tt> attribute.
     * @param async Whether or not to send the request to the server asynchronously
     */
    @JsxSetter(@WebBrowser(FF))
    public void setAsync(final boolean async) {
        async_ = async;
    }

    /**
     * Returns Whether or not to send the request to the server asynchronously.
     * @return the <tt>async</tt> attribute
     */
    @JsxGetter(@WebBrowser(FF))
    public boolean getAsync() {
        return async_;
    }

    /**
     * Loads an XML document from the specified location.
     *
     * @param xmlSource a string containing a URL that specifies the location of the XML file
     * @return true if the load succeeded; false if the load failed
     */
    @JsxFunction(@WebBrowser(FF))
    public boolean load(final String xmlSource) {
        if (async_) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("XMLDocument.load(): 'async' is true, currently treated as false.");
            }
        }
        try {
            final HtmlPage htmlPage = (HtmlPage) getWindow().getWebWindow().getEnclosedPage();
            final WebRequest request = new WebRequest(htmlPage.getFullyQualifiedUrl(xmlSource));
            final WebResponse webResponse = getWindow().getWebWindow().getWebClient().loadWebResponse(request);
            final XmlPage page = new XmlPage(webResponse, getWindow().getWebWindow(), false);
            setDomNode(page);
            return true;
        }
        catch (final IOException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Error parsing XML from '" + xmlSource + "'", e);
            }
            return false;
        }
    }

    /**
     * Loads an XML document using the supplied string.
     *
     * @param strXML A string containing the XML string to load into this XML document object
     *        This string can contain an entire XML document or a well-formed fragment.
     * @return true if the load succeeded; false if the load failed
     */
    public boolean loadXML(final String strXML) {
        final WebWindow webWindow = getWindow().getWebWindow();
        try {
            if (StringUtils.isEmpty(strXML) && getBrowserVersion().hasFeature(JS_DOMPARSER_EMPTY_STRING_IS_ERROR)) {
                throw new IOException("Error parsing XML '" + strXML + "'");
            }

            final WebResponse webResponse = new StringWebResponse(strXML, webWindow.getEnclosedPage().getUrl());

            final XmlPage page = new XmlPage(webResponse, webWindow, false);
            setDomNode(page);
            return true;
        }
        catch (final IOException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Error parsing XML\n" + strXML, e);
            }

            if (getBrowserVersion().hasFeature(JS_DOMPARSER_EXCEPTION_ON_ERROR)) {
                throw asJavaScriptException(
                        new DOMException("Syntax Error",
                            DOMException.SYNTAX_ERR));
            }
            if (getBrowserVersion().hasFeature(JS_DOMPARSER_PARSERERROR_ON_ERROR)) {
                try {
                    final XmlPage page = createParserErrorXmlPage("Syntax Error", webWindow);
                    setDomNode(page);
                }
                catch (final IOException eI) {
                    LOG.error("Could not handle ParserError", e);
                }
            }

            return false;
        }
    }

    private XmlPage createParserErrorXmlPage(final String message, final WebWindow webWindow) throws IOException {
        final String xml = "<parsererror xmlns=\"http://www.mozilla.org/newlayout/xml/parsererror.xml\">\n"
            + message + "\n"
            + "<sourcetext></sourcetext>\n"
            + "</parsererror>";

        final WebResponse webResponse = new StringWebResponse(xml, webWindow.getEnclosedPage().getUrl());

        return new XmlPage(webResponse, webWindow, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SimpleScriptable makeScriptableFor(final DomNode domNode) {
        final SimpleScriptable scriptable;

        // TODO: cleanup, getScriptObject() should be used!!!
        if (domNode instanceof DomElement && !(domNode instanceof HtmlElement)) {
            if (domNode instanceof SvgElement) {
                final Class<? extends SimpleScriptable> javaScriptClass = getWindow().getWebWindow().getWebClient()
                        .getJavaScriptEngine().getJavaScriptClass(domNode.getClass());
                try {
                    scriptable = javaScriptClass.newInstance();
                }
                catch (final Exception e) {
                    throw Context.throwAsScriptRuntimeEx(e);
                }
            }
            else {
                scriptable = new Element();
            }
        }
        else if (domNode instanceof DomAttr) {
            scriptable = new Attr();
        }
        else {
            return super.makeScriptableFor(domNode);
        }

        scriptable.setPrototype(getPrototype(scriptable.getClass()));
        scriptable.setParentScope(getParentScope());
        scriptable.setDomNode(domNode);
        return scriptable;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void initParentScope(final DomNode domNode, final SimpleScriptable scriptable) {
        scriptable.setParentScope(getParentScope());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @JsxFunction
    public HTMLCollection getElementsByTagName(final String tagName) {
        final DomNode firstChild = getDomNodeOrDie().getFirstChild();
        if (firstChild == null) {
            return HTMLCollection.emptyCollection(getWindow());
        }

        final HTMLCollection collection = new HTMLCollection(getDomNodeOrDie(), false,
                "XMLDocument.getElementsByTagName") {
            @Override
            protected boolean isMatching(final DomNode node) {
                return node.getNodeName().equals(tagName);
            }
        };

        return collection;
    }

    /**
     * Returns the element with the specified ID, as long as it is an HTML element; <tt>null</tt> otherwise.
     * @param id the ID to search for
     * @return the element with the specified ID, as long as it is an HTML element; <tt>null</tt> otherwise
     */
    @JsxFunction
    public Object getElementById(final String id) {
        final DomNode domNode = getDomNodeOrDie();
        final Object domElement = domNode.getFirstByXPath("//*[@id = \"" + id + "\"]");
        if (domElement != null) {
            if (!(domNode instanceof XmlPage) || domElement instanceof HtmlElement
                    || getBrowserVersion().hasFeature(JS_XML_GET_ELEMENT_BY_ID__ANY_ELEMENT)) {
                return ((DomElement) domElement).getScriptObject();
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("getElementById(" + id + "): no HTML DOM node found with this ID");
            }
        }
        return null;
    }

    /**
     * Creates a new ProcessingInstruction.
     * @param target the target
     * @param data the data
     * @return the new ProcessingInstruction
     */
    @JsxFunction
    public Object createProcessingInstruction(final String target, final String data) {
        final ProcessingInstruction node = getPage().createProcessingInstruction(target, data);
        return getScriptableFor(node);
    }

    /**
     * Creates a new createCDATASection.
     * @param data the data
     * @return the new CDATASection
     */
    @JsxFunction
    public Object createCDATASection(final String data) {
        final CDATASection node = getPage().createCDATASection(data);
        return getScriptableFor(node);
    }
}
