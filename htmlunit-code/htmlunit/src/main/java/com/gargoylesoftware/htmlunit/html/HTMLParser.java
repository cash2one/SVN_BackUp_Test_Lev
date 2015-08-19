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

import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.DOCTYPE_IS_COMMENT;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.HTMLCONDITIONAL_COMMENTS;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.HTMLIFRAME_IGNORE_SELFCLOSING;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.HTMLPARSER_REMOVE_EMPTY_CONTENT;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.HTML_ATTRIBUTE_LOWER_CASE;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.HTML_CDATA_AS_COMMENT;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.IGNORE_CONTENTS_OF_INNER_HEAD;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_DEFINE_GETTER;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.META_X_UA_COMPATIBLE;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.PAGE_WAIT_LOAD_BEFORE_BODY;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.SVG;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.xerces.parsers.AbstractSAXParser;
import org.apache.xerces.util.DefaultErrorHandler;
import org.apache.xerces.util.XMLStringBuffer;
import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.QName;
import org.apache.xerces.xni.XMLAttributes;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.apache.xerces.xni.parser.XMLParseException;
import org.apache.xerces.xni.parser.XMLParserConfiguration;
import org.cyberneko.html.HTMLConfiguration;
import org.cyberneko.html.HTMLEventInfo;
import org.cyberneko.html.HTMLScanner;
import org.cyberneko.html.HTMLTagBalancer;
import org.cyberneko.html.HTMLTagBalancingListener;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.ObjectInstantiationException;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.SgmlPage;
import com.gargoylesoftware.htmlunit.WebAssert;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.WebWindow;
import com.gargoylesoftware.htmlunit.javascript.host.html.HTMLBodyElement;
import com.gargoylesoftware.htmlunit.javascript.host.html.HTMLDocument;
import com.gargoylesoftware.htmlunit.javascript.host.html.HTMLElement;
import com.gargoylesoftware.htmlunit.svg.SvgElementFactory;

import net.sourceforge.htmlunit.corejs.javascript.Scriptable;
import net.sourceforge.htmlunit.corejs.javascript.ScriptableObject;

/**
 * <p>SAX parser implementation that uses the NekoHTML {@link org.cyberneko.html.HTMLConfiguration}
 * to parse HTML into a HtmlUnit-specific DOM (HU-DOM) tree.</p>
 *
 * @version $Revision: 10913 $
 * @author <a href="mailto:cse@dynabean.de">Christian Sell</a>
 * @author David K. Taylor
 * @author Chris Erskine
 * @author Ahmed Ashour
 * @author Marc Guillemot
 * @author Ethan Glasser-Camp
 * @author Sudhan Moghe
 * @author Ronald Brill
 * @author Frank Danek
 * @author Carsten Steul
 */
public final class HTMLParser {

    /** XHTML namespace. */
    public static final String XHTML_NAMESPACE = "http://www.w3.org/1999/xhtml";

    /** SVG namespace. */
    public static final String SVG_NAMESPACE = "http://www.w3.org/2000/svg";

    /**
     * The SVG factory.
     */
    public static final ElementFactory SVG_FACTORY = new SvgElementFactory();

    private static final Map<String, ElementFactory> ELEMENT_FACTORIES = new HashMap<>();

    static {
        ELEMENT_FACTORIES.put(HtmlInput.TAG_NAME, InputElementFactory.instance);

        final DefaultElementFactory defaultElementFactory = new DefaultElementFactory();
        for (final String tagName : DefaultElementFactory.SUPPORTED_TAGS_) {
            ELEMENT_FACTORIES.put(tagName, defaultElementFactory);
        }
    }

    /**
     * You should never need to create one of these!
     */
    private HTMLParser() {
        // Empty.
    }

    /**
     * Parses the HTML content from the given string into an object tree representation.
     *
     * @param parent the parent for the new nodes
     * @param source the (X)HTML to be parsed
     * @throws SAXException if a SAX error occurs
     * @throws IOException if an IO error occurs
     */
    public static void parseFragment(final DomNode parent, final String source) throws SAXException, IOException {
        parseFragment(parent, parent, source);
    }

    /**
     * Parses the HTML content from the given string into an object tree representation.
     *
     * @param parent where the new parsed nodes will be added to
     * @param context the context to build the fragment context stack
     * @param source the (X)HTML to be parsed
     * @throws SAXException if a SAX error occurs
     * @throws IOException if an IO error occurs
     */
    public static void parseFragment(final DomNode parent, final DomNode context, final String source)
        throws SAXException, IOException {
        final HtmlPage page = (HtmlPage) parent.getPage();
        final URL url = page.getUrl();

        final HtmlUnitDOMBuilder domBuilder = new HtmlUnitDOMBuilder(parent, url, source);
        domBuilder.setFeature("http://cyberneko.org/html/features/balance-tags/document-fragment", true);
        // build fragment context stack
        DomNode node = context;
        final List<QName> ancestors = new ArrayList<>();
        while (node != null && node.getNodeType() != Node.DOCUMENT_NODE) {
            ancestors.add(0, new QName(null, node.getNodeName(), null, null));
            node = node.getParentNode();
        }
        if (ancestors.isEmpty() || !"html".equals(ancestors.get(0).localpart)) {
            ancestors.add(0, new QName(null, "html", null, null));
        }
        if (ancestors.size() == 1 || !"body".equals(ancestors.get(1).localpart)) {
            ancestors.add(1, new QName(null, "body", null, null));
        }

        domBuilder.setFeature(HTMLScanner.ALLOW_SELFCLOSING_TAGS, true);
        domBuilder.setProperty(HTMLTagBalancer.FRAGMENT_CONTEXT_STACK, ancestors.toArray(new QName[] {}));

        final XMLInputSource in = new XMLInputSource(null, url.toString(), null, new StringReader(source), null);

        page.registerParsingStart();
        page.registerSnippetParsingStart();
        try {
            domBuilder.parse(in);
        }
        finally {
            page.registerParsingEnd();
            page.registerSnippetParsingEnd();
        }
    }

    /**
     * Parses the HTML content from the specified <tt>WebResponse</tt> into an object tree representation.
     *
     * @param webResponse the response data
     * @param webWindow the web window into which the page is to be loaded
     * @return the page object which is the root of the DOM tree
     * @throws IOException if there is an IO error
     */
    public static HtmlPage parseHtml(final WebResponse webResponse, final WebWindow webWindow) throws IOException {
        final HtmlPage page = new HtmlPage(webResponse.getWebRequest().getUrl(), webResponse, webWindow);
        parse(webResponse, webWindow, page, false);
        return page;
    }

    /**
     * Parses the XHTML content from the specified <tt>WebResponse</tt> into an object tree representation.
     *
     * @param webResponse the response data
     * @param webWindow the web window into which the page is to be loaded
     * @return the page object which is the root of the DOM tree
     * @throws IOException if there is an IO error
     */
    public static XHtmlPage parseXHtml(final WebResponse webResponse, final WebWindow webWindow) throws IOException {
        final XHtmlPage page = new XHtmlPage(webResponse.getWebRequest().getUrl(), webResponse, webWindow);
        parse(webResponse, webWindow, page, true);
        return page;
    }

    private static void parse(final WebResponse webResponse, final WebWindow webWindow, final HtmlPage page,
            final boolean xhtml)
        throws IOException {

        webWindow.setEnclosedPage(page);

        final URL url = webResponse.getWebRequest().getUrl();
        final HtmlUnitDOMBuilder domBuilder = new HtmlUnitDOMBuilder(page, url, null);

        String charset = webResponse.getContentCharsetOrNull();
        try {
            // handle charset
            if (charset != null) {
                domBuilder.setFeature(HTMLScanner.IGNORE_SPECIFIED_CHARSET, true);
            }
            else {
                final String specifiedCharset = webResponse.getWebRequest().getCharset();
                if (specifiedCharset != null) {
                    charset = specifiedCharset;
                }
            }

            // xml content is different
            if (xhtml) {
                domBuilder.setFeature(HTMLScanner.ALLOW_SELFCLOSING_TAGS, true);
            }
        }
        catch (final Exception e) {
            throw new ObjectInstantiationException("Error setting HTML parser feature", e);
        }

        final InputStream content = webResponse.getContentAsStream();
        final XMLInputSource in = new XMLInputSource(null, url.toString(), null, content, charset);

        page.registerParsingStart();
        try {
            domBuilder.parse(in);
        }
        catch (final XNIException e) {
            // extract enclosed exception
            final Throwable origin = extractNestedException(e);
            throw new RuntimeException("Failed parsing content from " + url, origin);
        }
        finally {
            IOUtils.closeQuietly(content);
            page.registerParsingEnd();
        }

        addBodyToPageIfNecessary(page, true, domBuilder.body_ != null);
    }

    /**
     * Adds a body element to the current page, if necessary. Strictly speaking, this should
     * probably be done by NekoHTML. See the bug linked below. If and when that bug is fixed,
     * we may be able to get rid of this code.
     *
     * http://sourceforge.net/p/nekohtml/bugs/15/
     * @param page
     * @param originalCall
     * @param checkInsideFrameOnly true if the original page had body that was removed by JavaScript
     */
    private static void addBodyToPageIfNecessary(
            final HtmlPage page, final boolean originalCall, final boolean checkInsideFrameOnly) {
        // IE waits for the whole page to load before initializing bodies for frames.
        final boolean waitToLoad = page.hasFeature(PAGE_WAIT_LOAD_BEFORE_BODY);
        if (page.getEnclosingWindow() instanceof FrameWindow && originalCall && waitToLoad) {
            return;
        }

        // Find out if the document already has a body element (or frameset).
        final Element doc = page.getDocumentElement();
        boolean hasBody = false;
        for (Node child = doc.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof HtmlBody || child instanceof HtmlFrameSet) {
                hasBody = true;
                break;
            }
        }

        // If the document does not have a body, add it.
        if (!hasBody && !checkInsideFrameOnly) {
            final HtmlBody body = new HtmlBody("body", page, null, false);
            doc.appendChild(body);
        }

        // If this is IE, we need to initialize the bodies of any frames, as well.
        // This will already have been done when emulating FF (see above).
        if (waitToLoad) {
            for (final FrameWindow frame : page.getFrames()) {
                final Page containedPage = frame.getEnclosedPage();
                if (containedPage != null && containedPage.isHtmlPage()) {
                    addBodyToPageIfNecessary((HtmlPage) containedPage, false, false);
                }
            }
        }
    }

    /**
     * Extract nested exception within an XNIException (Nekohtml uses reflection and generated
     * exceptions are wrapped many times within XNIException and InvocationTargetException)
     *
     * @param e the original XNIException
     * @return the cause exception
     */
    static Throwable extractNestedException(final Throwable e) {
        Throwable originalException = e;
        Throwable cause = ((XNIException) e).getException();
        while (cause != null) {
            originalException = cause;
            if (cause instanceof XNIException) {
                cause = ((XNIException) cause).getException();
            }
            else if (cause instanceof InvocationTargetException) {
                cause = cause.getCause();
            }
            else {
                cause = null;
            }
        }
        return originalException;
    }

    /**
     * @param tagName an HTML element tag name
     * @return a factory for creating HtmlElements representing the given tag
     */
    public static ElementFactory getFactory(final String tagName) {
        final ElementFactory result = ELEMENT_FACTORIES.get(tagName);

        if (result != null) {
            return result;
        }
        return UnknownElementFactory.instance;
    }

    /**
     * Returns the pre-registered element factory corresponding to the specified tag, or an UnknownElementFactory.
     * @param page the page
     * @param namespaceURI the namespace URI
     * @param qualifiedName the qualified name
     * @return the pre-registered element factory corresponding to the specified tag, or an UnknownElementFactory
     */
    static ElementFactory getElementFactory(final SgmlPage page, final String namespaceURI,
            final String qualifiedName) {
        if (SVG_NAMESPACE.equals(namespaceURI) && page.hasFeature(SVG)) {
            return SVG_FACTORY;
        }
        if (namespaceURI == null || namespaceURI.isEmpty()
            || !qualifiedName.contains(":") || namespaceURI.equals(XHTML_NAMESPACE)) {

            String tagName = qualifiedName;
            final int index = tagName.indexOf(':');
            if (index != -1) {
                tagName = tagName.substring(index + 1);
            }
            else {
                tagName = tagName.toLowerCase(Locale.ENGLISH);
            }
            final ElementFactory factory = ELEMENT_FACTORIES.get(tagName);

            if (factory != null) {
                return factory;
            }
        }
        return UnknownElementFactory.instance;
    }

    /**
     * The parser and DOM builder. This class subclasses Xerces's AbstractSAXParser and implements
     * the ContentHandler interface. Thus all parser APIs are kept private. The ContentHandler methods
     * consume SAX events to build the page DOM
     */
    static final class HtmlUnitDOMBuilder extends AbstractSAXParser
            implements ContentHandler, LexicalHandler, HTMLTagBalancingListener {

        private enum HeadParsed { YES, SYNTHESIZED, NO };

        private final HtmlPage page_;

        private Locator locator_;
        private final Deque<DomNode> stack_ = new ArrayDeque<DomNode>();

        private DomNode currentNode_;
        private StringBuilder characters_;
        private HeadParsed headParsed_ = HeadParsed.NO;
        private boolean parsingInnerHead_ = false;
        private HtmlElement head_;
        private HtmlElement body_;
        private boolean lastTagWasSynthesized_;
        private HtmlForm formWaitingForLostChildren_;
        private static final String FEATURE_AUGMENTATIONS = "http://cyberneko.org/html/features/augmentations";
        private static final String FEATURE_PARSE_NOSCRIPT
            = "http://cyberneko.org/html/features/parse-noscript-content";

        /**
         * Parses and then inserts the specified HTML content into the HTML content currently being parsed.
         * @param html the HTML content to push
         */
        public void pushInputString(final String html) {
            page_.registerParsingStart();
            page_.registerInlineSnippetParsingStart();
            try {
                final WebResponse webResponse = page_.getWebResponse();
                final String charset = webResponse.getContentCharset();
                final String url = webResponse.getWebRequest().getUrl().toString();
                final XMLInputSource in = new XMLInputSource(null, url, null, new StringReader(html), charset);
                ((HTMLConfiguration) fConfiguration).evaluateInputSource(in);
            }
            finally {
                page_.registerParsingEnd();
                page_.registerInlineSnippetParsingEnd();
            }
        }

        /**
         * Creates a new builder for parsing the specified response contents.
         * @param node the location at which to insert the new content
         * @param url the page's URL
         */
        private HtmlUnitDOMBuilder(final DomNode node, final URL url, final String htmlContent) {
            super(createConfiguration(node.getPage().getWebClient()));
            page_ = (HtmlPage) node.getPage();

            currentNode_ = node;
            for (final Node ancestor : currentNode_.getAncestors(true)) {
                stack_.push((DomNode) ancestor);
            }

            final WebClient webClient = page_.getWebClient();
            final HTMLParserListener listener = webClient.getHTMLParserListener();
            final boolean reportErrors;
            if (listener != null) {
                reportErrors = true;
                fConfiguration.setErrorHandler(new HTMLErrorHandler(listener, url, htmlContent));
            }
            else {
                reportErrors = false;
            }

            try {
                setFeature(FEATURE_AUGMENTATIONS, true);
                setProperty("http://cyberneko.org/html/properties/names/elems", "default");
                if (!webClient.getBrowserVersion().hasFeature(HTML_ATTRIBUTE_LOWER_CASE)) {
                    setProperty("http://cyberneko.org/html/properties/names/attrs", "no-change");
                }
                setFeature("http://cyberneko.org/html/features/report-errors", reportErrors);
                setFeature(FEATURE_PARSE_NOSCRIPT, !webClient.getOptions().isJavaScriptEnabled());
                setFeature(HTMLScanner.ALLOW_SELFCLOSING_IFRAME,
                    !webClient.getBrowserVersion().hasFeature(HTMLIFRAME_IGNORE_SELFCLOSING));

                setContentHandler(this);
                setLexicalHandler(this); //comments and CDATA
            }
            catch (final SAXException e) {
                throw new ObjectInstantiationException("unable to create HTML parser", e);
            }
        }

        /**
         * Create the configuration depending on the simulated browser
         * @param webClient the current WebClient
         * @return the configuration
         */
        private static XMLParserConfiguration createConfiguration(final WebClient webClient) {
            final BrowserVersion browserVersion = webClient.getBrowserVersion();
            // for IE we need a special scanner that will be able to understand conditional comments
            if (browserVersion.hasFeature(HTMLCONDITIONAL_COMMENTS)) {
                return new HTMLConfiguration() {
                    @Override
                    protected HTMLScanner createDocumentScanner() {
                        return new HTMLScannerForIE(browserVersion);
                    }
                };
            }
            return new HTMLConfiguration();
        }

        /**
         * @return the document locator
         */
        public Locator getLocator() {
            return locator_;
        }

        /** {@inheritDoc ContentHandler#setDocumentLocator} */
        @Override
        public void setDocumentLocator(final Locator locator) {
            locator_ = locator;
        }

        /** {@inheritDoc ContentHandler#startDocument()} */
        @Override
        public void startDocument() throws SAXException {
        }

        /** {@inheritDoc} */
        @Override
        public void startElement(final QName element, final XMLAttributes attributes, final Augmentations augs)
            throws XNIException {
            // augs might change so we store only the interesting part
            lastTagWasSynthesized_ = isSynthesized(augs);
            super.startElement(element, attributes, augs);
        }

        /** {@inheritDoc ContentHandler#startElement(String,String,String,Attributes)} */
        @Override
        public void startElement(
                String namespaceURI, final String localName,
                final String qName, final Attributes atts)
            throws SAXException {

            handleCharacters();

            final String tagLower = localName.toLowerCase(Locale.ENGLISH);
            if (page_.isParsingHtmlSnippet() && ("html".equals(tagLower) || "body".equals(tagLower))) {
                return;
            }

            if (parsingInnerHead_ && page_.hasFeature(IGNORE_CONTENTS_OF_INNER_HEAD)) {
                return;
            }

            if (namespaceURI != null) {
                namespaceURI = namespaceURI.trim();
            }
            if ("head".equals(tagLower)) {
                if (headParsed_ == HeadParsed.YES || page_.isParsingHtmlSnippet()) {
                    parsingInnerHead_ = true;
                    return;
                }

                headParsed_ = lastTagWasSynthesized_ ? HeadParsed.SYNTHESIZED : HeadParsed.YES;
            }
            // add a head if none was there
            else if (headParsed_ == HeadParsed.NO && ("body".equals(tagLower) || "frameset".equals(tagLower))) {
                final ElementFactory factory = getElementFactory(page_, namespaceURI, "head");
                final DomElement newElement = factory.createElement(page_, "head", null);
                currentNode_.appendChild(newElement);
                headParsed_ = HeadParsed.SYNTHESIZED;
            }

            // If we're adding a body element, keep track of any temporary synthetic ones
            // that we may have had to create earlier (for document.write(), for example).
            HtmlBody oldBody = null;
            if ("body".equals(qName) && page_.getBody() instanceof HtmlBody) {
                oldBody = (HtmlBody) page_.getBody();
            }

            // Need to reset this at each starting form tag because it could be set from a synthesized
            // end tag.
            if ("form".equals(tagLower)) {
                formWaitingForLostChildren_ = null;
            }

            // Add the new node.
            if (!(page_ instanceof XHtmlPage) && XHTML_NAMESPACE.equals(namespaceURI)) {
                namespaceURI = null;
            }
            final ElementFactory factory = getElementFactory(page_, namespaceURI, qName);
            final DomElement newElement = factory.createElementNS(page_, namespaceURI, qName, atts, true);
            newElement.setStartLocation(locator_.getLineNumber(), locator_.getColumnNumber());

            // parse can't replace everything as it does not buffer elements while parsing
            addNodeToRightParent(currentNode_, newElement);

            // If we had an old synthetic body and we just added a real body element, quietly
            // remove the old body and move its children to the real body element we just added.
            if (oldBody != null) {
                oldBody.quietlyRemoveAndMoveChildrenTo(newElement);
            }

            if ("body".equals(tagLower)) {
                body_ = (HtmlElement) newElement;
            }
            else if ("head".equals(tagLower)) {
                head_ = (HtmlElement) newElement;
            }
            else if ("html".equals(tagLower)) {
                if (!page_.hasFeature(JS_DEFINE_GETTER) && page_.isQuirksMode()) {
                    // this is not really correct; a following meta tag may disable the quirks
                    // mode; but at the moment i have no idea for a better place for this
                    removePrototypeProperties((Scriptable) page_.getEnclosingWindow().getScriptObject(), "Array",
                        "every", "filter", "forEach", "indexOf", "lastIndexOf", "map", "reduce",
                        "reduceRight", "some");
                }
            }
            else if ("meta".equals(tagLower)) {
                // i like the IE
                if (page_.hasFeature(META_X_UA_COMPATIBLE)) {
                    final HtmlMeta meta = (HtmlMeta) newElement;
                    if ("X-UA-Compatible".equals(meta.getHttpEquivAttribute())) {
                        final String content = meta.getContentAttribute();
                        if (content.startsWith("IE=")) {
                            final String mode = content.substring(3).trim();
                            final int version = (int) page_.getWebClient().getBrowserVersion().
                                                                getBrowserVersionNumeric();
                            if ("edge".equals(mode)) {
                                ((HTMLDocument) page_.getScriptObject()).forceDocumentMode(version);
                            }
                            else {
                                try {
                                    int value = Integer.parseInt(mode);
                                    if (value > version) {
                                        value = version;
                                    }
                                    ((HTMLDocument) page_.getScriptObject()).forceDocumentMode(value);
                                }
                                catch (final Exception e) {
                                    // ignore
                                }
                            }
                        }
                    }
                }
            }
            currentNode_ = newElement;
            stack_.push(currentNode_);
        }

        /**
         * Removes prototype properties.
         * @param scope the scope
         * @param className the class for which properties should be removed
         * @param properties the properties to remove
         */
        private void removePrototypeProperties(final Scriptable scope, final String className,
                final String... properties) {
            final ScriptableObject prototype = (ScriptableObject) ScriptableObject.getClassPrototype(scope, className);
            for (final String property : properties) {
                prototype.delete(property);
            }
        }

        /**
         * Adds the new node to the right parent that is not necessary the currentNode in case of
         * malformed HTML code. The method tries to emulate the behaviour of Firefox.
         */
        private void addNodeToRightParent(final DomNode currentNode, final DomElement newElement) {
            final String currentNodeName = currentNode.getNodeName();
            final String newNodeName = newElement.getNodeName();

            DomNode parent = currentNode;

            // If the new node is a table element and the current node isn't one search the stack for the
            // correct parent.
            if ("tr".equals(newNodeName) && !isTableChild(currentNodeName)) {
                parent = findElementOnStack("tbody", "thead", "tfoot");
            }
            else if (isTableChild(newNodeName) && !"table".equals(currentNodeName)) {
                parent = findElementOnStack("table");
            }
            else if (isTableCell(newNodeName) && !"tr".equals(currentNodeName)) {
                parent = findElementOnStack("tr");
            }

            // If the parent changed and the old parent was a form it is now waiting for lost children.
            if (parent != currentNode && "form".equals(currentNodeName)) {
                formWaitingForLostChildren_ = (HtmlForm) currentNode;
            }

            final String parentNodeName = parent.getNodeName();

            if (("table".equals(parentNodeName) && !isTableChild(newNodeName))
                    || (isTableChild(parentNodeName) && !"caption".equals(parentNodeName)
                            && !"colgroup".equals(parentNodeName) && !"tr".equals(newNodeName))
                    || ("colgroup".equals(parentNodeName) && !"col".equals(newNodeName))
                    || ("tr".equals(parentNodeName) && !isTableCell(newNodeName))) {
                // If its a form or submittable just add it even though the resulting DOM is incorrect.
                // Otherwise insert the element before the table.
                if ("form".equals(newNodeName)) {
                    formWaitingForLostChildren_ = (HtmlForm) newElement;
                    parent.appendChild(newElement);
                }
                else if (newElement instanceof SubmittableElement) {
                    if (formWaitingForLostChildren_ != null) {
                        formWaitingForLostChildren_.addLostChild((HtmlElement) newElement);
                    }
                    parent.appendChild(newElement);
                }
                else {
                    parent = findElementOnStack("table");
                    parent.insertBefore(newElement);
                }
            }
            else if (head_ != null && "title".equals(newNodeName) && !parsingInnerHead_) {
                head_.appendChild(newElement);
            }
            else if (formWaitingForLostChildren_ != null && "form".equals(parentNodeName)) {
                // Do not append any children to invalid form. Submittable are inserted after the form,
                // everything else before the table.
                if (newElement instanceof SubmittableElement) {
                    formWaitingForLostChildren_.addLostChild((HtmlElement) newElement);
                    parent.getParentNode().appendChild(newElement);
                }
                else {
                    parent = findElementOnStack("table");
                    parent.insertBefore(newElement);
                }
            }
            else if (formWaitingForLostChildren_ != null && newElement instanceof SubmittableElement) {
                formWaitingForLostChildren_.addLostChild((HtmlElement) newElement);
                parent.appendChild(newElement);
            }
            else {
                parent.appendChild(newElement);
            }
        }

        private DomNode findElementOnStack(final String... searchedElementNames) {
            DomNode searchedNode = null;
            for (final DomNode node : stack_) {
                if (ArrayUtils.contains(searchedElementNames, node.getNodeName())) {
                    searchedNode = node;
                    break;
                }
            }

            if (searchedNode == null) {
                searchedNode = stack_.peek(); // this is surely wrong but at least it won't throw a NPE
            }

            return searchedNode;
        }

        private boolean isTableChild(final String nodeName) {
            return "thead".equals(nodeName) || "tbody".equals(nodeName)
                    || "tfoot".equals(nodeName) || "caption".equals(nodeName)
                    || "colgroup".equals(nodeName);
        }

        private boolean isTableCell(final String nodeName) {
            return "td".equals(nodeName) || "th".equals(nodeName);
        }

        /** {@inheritDoc} */
        @Override
        public void endElement(final QName element, final Augmentations augs)
            throws XNIException {
            // augs might change so we store only the interesting part
            lastTagWasSynthesized_ = isSynthesized(augs);
            super.endElement(element, augs);
        }

        /** {@inheritDoc ContentHandler@endElement(String,String,String)} */
        @Override
        public void endElement(final String namespaceURI, final String localName, final String qName)
            throws SAXException {

            handleCharacters();

            final String tagLower = localName.toLowerCase(Locale.ENGLISH);

            if (page_.isParsingHtmlSnippet() && ("html".equals(tagLower) || "body".equals(tagLower))) {
                return;
            }

            if (parsingInnerHead_) {
                if ("head".equals(tagLower)) {
                    parsingInnerHead_ = false;
                }
                if ("head".equals(tagLower) || page_.hasFeature(IGNORE_CONTENTS_OF_INNER_HEAD)) {
                    return;
                }
            }

            // Need to reset this at each closing form tag because a valid form could start afterwards.
            if ("form".equals(tagLower)) {
                formWaitingForLostChildren_ = null;
            }

            final DomNode previousNode = stack_.pop(); //remove currentElement from stack
            previousNode.setEndLocation(locator_.getLineNumber(), locator_.getColumnNumber());

            // special handling for form lost children (malformed HTML code where </form> is synthesized)
            if (previousNode instanceof HtmlForm && lastTagWasSynthesized_) {
                formWaitingForLostChildren_ = (HtmlForm) previousNode;
            }

            if (!stack_.isEmpty()) {
                currentNode_ = stack_.peek();
            }

            final boolean postponed = page_.isParsingInlineHtmlSnippet();
            previousNode.onAllChildrenAddedToPage(postponed);
        }

        /** {@inheritDoc} */
        @Override
        public void characters(final char[] ch, final int start, final int length) throws SAXException {
            if ((characters_ == null || characters_.length() == 0)
                    && page_.hasFeature(HTMLPARSER_REMOVE_EMPTY_CONTENT)
                    && StringUtils.isBlank(new String(ch, start, length))) {

                DomNode node = currentNode_.getLastChild();
                if (currentNode_ instanceof HTMLElement.ProxyDomNode) {
                    final HTMLElement.ProxyDomNode proxyNode = (HTMLElement.ProxyDomNode) currentNode_;
                    node = proxyNode.getDomNode();
                    if (!proxyNode.isAppend()) {
                        node = node.getPreviousSibling();
                        if (node == null) {
                            node = proxyNode.getDomNode().getParentNode();
                        }
                    }
                }
                if (removeEmptyCharacters(node)) {
                    return;
                }
            }
            if (characters_ == null) {
                characters_ = new StringBuilder();
            }
            characters_.append(ch, start, length);
        }

        private boolean removeEmptyCharacters(final DomNode node) {
            if (node != null) {
                if (node instanceof HtmlInput) {
                    return false;
                }
                if (node.getFirstChild() != null
                    && (node instanceof HtmlAnchor || node instanceof HtmlSpan
                        || node instanceof HtmlFont
                        || node instanceof HtmlStrong || node instanceof HtmlBold
                        || node instanceof HtmlItalic || node instanceof HtmlUnderlined
                        || node instanceof HtmlEmphasis
                        || node instanceof HtmlAbbreviated || node instanceof HtmlAcronym
                        || node instanceof HtmlBaseFont || node instanceof HtmlBidirectionalOverride
                        || node instanceof HtmlBig || node instanceof HtmlBlink
                        || node instanceof HtmlCitation || node instanceof HtmlCode
                        || node instanceof HtmlDeletedText || node instanceof HtmlDefinition
                        || node instanceof HtmlInsertedText || node instanceof HtmlKeyboard
                        || node instanceof HtmlLabel || node instanceof HtmlMap
                        || node instanceof HtmlNoBreak || node instanceof HtmlInlineQuotation
                        || node instanceof HtmlS || node instanceof HtmlSample
                        || node instanceof HtmlSmall || node instanceof HtmlStrike
                        || node instanceof HtmlSubscript || node instanceof HtmlSuperscript
                        || node instanceof HtmlTeletype || node instanceof HtmlVariable
                        )) {
                    return false;
                }
            }
            else {
                if (currentNode_ instanceof HtmlFont) {
                    return false;
                }
            }
            return true;
        }

        /** {@inheritDoc} */
        @Override
        public void ignorableWhitespace(final char[] ch, final int start, final int length) throws SAXException {
            if (characters_ == null) {
                characters_ = new StringBuilder();
            }
            characters_.append(ch, start, length);
        }

        /**
         * Picks up the character data accumulated so far and add it to the current element as a text node.
         */
        private void handleCharacters() {
            if (characters_ != null && characters_.length() != 0) {
                if (currentNode_ instanceof HtmlHtml) {
                    // In HTML, the <html> node only has two possible children:
                    // the <head> and the <body>; any text is ignored.
                    characters_.setLength(0);
                }
                else {
                    // Use the normal behavior: append a text node for the accumulated text.
                    final String textValue = characters_.toString();
                    final DomText text = new DomText(page_, textValue);
                    characters_.setLength(0);

                    // malformed HTML: </td>some text</tr> => text comes before the table
                    if (currentNode_ instanceof HtmlTableRow && StringUtils.isNotBlank(textValue)) {
                        final HtmlTableRow row = (HtmlTableRow) currentNode_;
                        final HtmlTable enclosingTable = row.getEnclosingTable();
                        if (enclosingTable != null) { // may be null when called from Range.createContextualFragment
                            enclosingTable.insertBefore(text);
                        }
                    }
                    else {
                        currentNode_.appendChild(text);
                    }
                }
            }
        }

        /** {@inheritDoc} */
        @Override
        public void endDocument() throws SAXException {
            handleCharacters();
            final DomNode currentPage = page_;
            currentPage.setEndLocation(locator_.getLineNumber(), locator_.getColumnNumber());
        }

        /** {@inheritDoc} */
        @Override
        public void startPrefixMapping(final String prefix, final String uri) throws SAXException {
        }

        /** {@inheritDoc} */
        @Override
        public void endPrefixMapping(final String prefix) throws SAXException {
        }

        /** {@inheritDoc} */
        @Override
        public void processingInstruction(final String target, final String data) throws SAXException {
        }

        /** {@inheritDoc} */
        @Override
        public void skippedEntity(final String name) throws SAXException {
        }

        // LexicalHandler methods

        /** {@inheritDoc} */
        @Override
        public void comment(final char[] ch, final int start, final int length) {
            handleCharacters();
            final String data = new String(ch, start, length);
            if (!data.startsWith("[CDATA")
                    || page_.hasFeature(HTML_CDATA_AS_COMMENT)) {
                final DomComment comment = new DomComment(page_, data);
                currentNode_.appendChild(comment);
            }
        }

        /** {@inheritDoc} */
        @Override
        public void endCDATA() {
        }

        /** {@inheritDoc} */
        @Override
        public void endDTD() {
        }

        /** {@inheritDoc} */
        @Override
        public void endEntity(final String name) {
        }

        /** {@inheritDoc} */
        @Override
        public void startCDATA() {
        }

        /** {@inheritDoc} */
        @Override
        public void startDTD(final String name, final String publicId, final String systemId) {
            final DomDocumentType type = new DomDocumentType(page_, name, publicId, systemId);
            page_.setDocumentType(type);

            final Node child;
            if (page_.hasFeature(DOCTYPE_IS_COMMENT)) {
                child = new DomComment(page_, "DOCTYPE " + name + " PUBLIC \""
                        + publicId + "\"      \"" + systemId + '"');
            }
            else {
                child = type;
            }
            page_.appendChild(child);
        }

        /** {@inheritDoc} */
        @Override
        public void startEntity(final String name) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void ignoredEndElement(final QName element, final Augmentations augs) {
            // if real </form> is reached, don't accept fields anymore as lost children
            if ("form".equals(element.localpart)) {
                formWaitingForLostChildren_ = null;
            }

            if (parsingInnerHead_ && "head".equalsIgnoreCase(element.localpart)) {
                parsingInnerHead_ = false;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void ignoredStartElement(final QName elem, final XMLAttributes attrs, final Augmentations augs) {
            // when multiple body elements are encountered, the attributes of the discarded
            // elements are used when not previously defined
            if (body_ != null && "body".equalsIgnoreCase(elem.localpart) && attrs != null) {
                // add the attributes that don't already exist
                final int length = attrs.getLength();
                for (int i = 0; i < length; ++i) {
                    final String attrName = attrs.getLocalName(i).toLowerCase(Locale.ENGLISH);
                    if (body_.getAttributes().getNamedItem(attrName) == null) {
                        body_.setAttribute(attrName, attrs.getValue(i));
                        if (attrName.startsWith("on") && body_.getScriptObject() != null) {
                            final HTMLBodyElement jsBody = (HTMLBodyElement) body_.getScriptObject();
                            jsBody.createEventHandlerFromAttribute(attrName, attrs.getValue(i));
                        }
                    }
                }
            }

            if (headParsed_ == HeadParsed.YES && "head".equalsIgnoreCase(elem.localpart)) {
                parsingInnerHead_ = true;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void parse(final XMLInputSource inputSource) throws XNIException, IOException {
            final HtmlUnitDOMBuilder oldBuilder = page_.getBuilder();
            page_.setBuilder(this);
            try {
                super.parse(inputSource);
            }
            finally {
                page_.setBuilder(oldBuilder);
            }
        }

        private boolean isSynthesized(final Augmentations augs) {
            final HTMLEventInfo info = (augs == null) ? null
                    : (HTMLEventInfo) augs.getItem(FEATURE_AUGMENTATIONS);
            return info != null ? info.isSynthesized() : false;
        }
    }
}

/**
 * Utility to transmit parsing errors to a {@link HTMLParserListener}.
 */
class HTMLErrorHandler extends DefaultErrorHandler {
    private final HTMLParserListener listener_;
    private final URL url_;
    private String html_;

    HTMLErrorHandler(final HTMLParserListener listener, final URL url, final String htmlContent) {
        WebAssert.notNull("listener", listener);
        WebAssert.notNull("url", url);
        listener_ = listener;
        url_ = url;
        html_ = htmlContent;
    }

    /** @see DefaultErrorHandler#error(String,String,XMLParseException) */
    @Override
    public void error(final String domain, final String key,
            final XMLParseException exception) throws XNIException {
        listener_.error(exception.getMessage(),
                url_,
                html_,
                exception.getLineNumber(),
                exception.getColumnNumber(),
                key);
    }

    /** @see DefaultErrorHandler#warning(String,String,XMLParseException) */
    @Override
    public void warning(final String domain, final String key,
            final XMLParseException exception) throws XNIException {
        listener_.warning(exception.getMessage(),
                url_,
                html_,
                exception.getLineNumber(),
                exception.getColumnNumber(),
                key);
    }
}

class HTMLScannerForIE extends org.cyberneko.html.HTMLScanner {
    HTMLScannerForIE(final BrowserVersion browserVersion) {
        fContentScanner = new ContentScannerForIE(browserVersion);
    }

    class ContentScannerForIE extends HTMLScanner.ContentScanner {
        private final BrowserVersion browserVersion_;

        ContentScannerForIE(final BrowserVersion browserVersion) {
            browserVersion_ = browserVersion;
        }

        @Override
        protected void scanComment() throws IOException {
            final String s = nextContent(30); // [if ...
            if (s.startsWith("[if ") && s.contains("]>")) {
                final String condition = StringUtils.substringBefore(s.substring(4), "]>");
                try {
                    if (IEConditionalCommentExpressionEvaluator.evaluate(condition, browserVersion_)) {
                        // skip until ">"
                        for (int i = 0; i < condition.length() + 6; ++i) {
                            read();
                        }
                        if (s.contains("]><!-->")) {
                            skip("<!-->", false);
                        }
                        else if (s.contains("]>-->")) {
                            skip("-->", false);
                        }
                    }
                    else {
                        final StringBuilder builder = new StringBuilder();
                        while (!builder.toString().endsWith("-->")) {
                            builder.append((char) read());
                        }
                    }
                    return;
                }
                catch (final Exception e) { // incorrect expression => handle it as plain text
                    // TODO: report it!
                    final XMLStringBuffer buffer = new XMLStringBuffer("<!--");
                    scanMarkupContent(buffer, '-');
                    buffer.append("-->");
                    fDocumentHandler.characters(buffer, locationAugs());
                    return;
                }
            }
            // this is a normal comment, not a conditional comment for IE
            super.scanComment();
        }

        @Override
        public String nextContent(final int len) throws IOException {
            return super.nextContent(len);
        }

        @Override
        public boolean scanMarkupContent(final XMLStringBuffer buffer, final char cend) throws IOException {
            return super.scanMarkupContent(buffer, cend);
        }
    }

    @Override
    protected boolean skipMarkup(final boolean balance) throws IOException {
        final ContentScannerForIE contentScanner = (ContentScannerForIE) fContentScanner;
        final String s = contentScanner.nextContent(30);
        if (s.startsWith("[if ") && s.contains("]>")) {
            final String condition = StringUtils.substringBefore(s.substring(4), "]>");
            try {
                if (IEConditionalCommentExpressionEvaluator.evaluate(condition, contentScanner.browserVersion_)) {
                    // skip until ">"
                    for (int i = 0; i < condition.length() + 6; ++i) {
                        read();
                    }
                    return true;
                }

                final XMLStringBuffer buffer = new XMLStringBuffer();
                int ch;
                while ((ch = read()) != -1) {
                    buffer.append((char) ch);
                    if (buffer.toString().endsWith("<![endif]>")) {
                        final XMLStringBuffer trimmedBuffer
                            = new XMLStringBuffer(buffer.ch, 0, buffer.length - 3);
                        fDocumentHandler.comment(trimmedBuffer, locationAugs());
                        return true;
                    }
                }
            }
            catch (final Exception e) { // incorrect expression => handle it as plain text
                // TODO: report it!
                final XMLStringBuffer buffer = new XMLStringBuffer("<!--");
                contentScanner.scanMarkupContent(buffer, '-');
                buffer.append("-->");
                fDocumentHandler.characters(buffer, locationAugs());
                return true;
            }

        }
        return super.skipMarkup(balance);
    }
}
