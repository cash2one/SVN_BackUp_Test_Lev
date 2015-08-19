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
package com.gargoylesoftware.htmlunit.javascript.host.css;

import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.CSS_SELECTOR_EMPTY_STRING_HITS_AS_SUBSTRING;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.CSS_SELECTOR_LANG;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.QUERYSELECTORALL_NOT_IN_QUIRKS;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.QUERYSELECTOR_CSS3_PSEUDO_REQUIRE_ATTACHED_NODE;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.STYLESHEET_HREF_EMPTY_IS_NULL;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.STYLESHEET_HREF_EXPANDURL;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.STYLESHEET_HREF_STYLE_EMPTY;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.CHROME;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.FF;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.IE;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import net.sourceforge.htmlunit.corejs.javascript.Context;
import net.sourceforge.htmlunit.corejs.javascript.ScriptableObject;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.css.sac.AttributeCondition;
import org.w3c.css.sac.CSSException;
import org.w3c.css.sac.CSSParseException;
import org.w3c.css.sac.CombinatorCondition;
import org.w3c.css.sac.Condition;
import org.w3c.css.sac.ConditionalSelector;
import org.w3c.css.sac.ContentCondition;
import org.w3c.css.sac.DescendantSelector;
import org.w3c.css.sac.ElementSelector;
import org.w3c.css.sac.ErrorHandler;
import org.w3c.css.sac.InputSource;
import org.w3c.css.sac.LangCondition;
import org.w3c.css.sac.NegativeCondition;
import org.w3c.css.sac.NegativeSelector;
import org.w3c.css.sac.SACMediaList;
import org.w3c.css.sac.Selector;
import org.w3c.css.sac.SelectorList;
import org.w3c.css.sac.SiblingSelector;
import org.w3c.css.sac.SimpleSelector;
import org.w3c.dom.DOMException;
import org.w3c.dom.css.CSSImportRule;
import org.w3c.dom.css.CSSRule;
import org.w3c.dom.css.CSSRuleList;
import org.w3c.dom.stylesheets.MediaList;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.Cache;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.DisabledElement;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.DomText;
import com.gargoylesoftware.htmlunit.html.HtmlCheckBoxInput;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlHtml;
import com.gargoylesoftware.htmlunit.html.HtmlLink;
import com.gargoylesoftware.htmlunit.html.HtmlOption;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlRadioButtonInput;
import com.gargoylesoftware.htmlunit.html.HtmlStyle;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxClass;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxClasses;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxConstructor;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxFunction;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxGetter;
import com.gargoylesoftware.htmlunit.javascript.configuration.WebBrowser;
import com.gargoylesoftware.htmlunit.javascript.host.Element;
import com.gargoylesoftware.htmlunit.javascript.host.Window;
import com.gargoylesoftware.htmlunit.javascript.host.html.HTMLDocument;
import com.gargoylesoftware.htmlunit.javascript.host.html.HTMLElement;
import com.gargoylesoftware.htmlunit.util.UrlUtils;
import com.steadystate.css.dom.CSSImportRuleImpl;
import com.steadystate.css.dom.CSSMediaRuleImpl;
import com.steadystate.css.dom.CSSStyleRuleImpl;
import com.steadystate.css.dom.CSSStyleSheetImpl;
import com.steadystate.css.dom.MediaListImpl;
import com.steadystate.css.parser.CSSOMParser;
import com.steadystate.css.parser.SACMediaListImpl;
import com.steadystate.css.parser.SACParserCSS3;
import com.steadystate.css.parser.SelectorListImpl;
import com.steadystate.css.parser.selectors.GeneralAdjacentSelectorImpl;
import com.steadystate.css.parser.selectors.PrefixAttributeConditionImpl;
import com.steadystate.css.parser.selectors.PseudoClassConditionImpl;
import com.steadystate.css.parser.selectors.SubstringAttributeConditionImpl;
import com.steadystate.css.parser.selectors.SuffixAttributeConditionImpl;

/**
 * A JavaScript object for {@code CSSStyleSheet}.
 *
 * @see <a href="http://msdn2.microsoft.com/en-us/library/ms535871.aspx">MSDN doc</a>
 * @version $Revision: 10940 $
 * @author Marc Guillemot
 * @author Daniel Gredler
 * @author Ahmed Ashour
 * @author Ronald Brill
 * @author Guy Burton
 * @author Frank Danek
 */
@JsxClasses({
        @JsxClass(browsers = { @WebBrowser(CHROME), @WebBrowser(FF), @WebBrowser(value = IE, minVersion = 11) }),
        @JsxClass(isJSObject = false, browsers = @WebBrowser(value = IE, maxVersion = 8))
    })
public class CSSStyleSheet extends StyleSheet {

    private static final Log LOG = LogFactory.getLog(CSSStyleSheet.class);
    private static final Pattern NTH_NUMERIC = Pattern.compile("\\d+");
    private static final Pattern NTH_COMPLEX = Pattern.compile("[+-]?\\d*n\\w*([+-]\\w\\d*)?");
    private static final Pattern UNESCAPE_SELECTOR = Pattern.compile("\\\\([\\[\\]\\.:])");

    /** The parsed stylesheet which this host object wraps. */
    private final org.w3c.dom.css.CSSStyleSheet wrapped_;

    /** The HTML element which owns this stylesheet. */
    private final HTMLElement ownerNode_;

    /** The collection of rules defined in this style sheet. */
    private com.gargoylesoftware.htmlunit.javascript.host.css.CSSRuleList cssRules_;

    /** The CSS import rules and their corresponding stylesheets. */
    private final Map<CSSImportRule, CSSStyleSheet> imports_ = new HashMap<>();

    /** This stylesheet's URI (used to resolved contained @import rules). */
    private String uri_;

    private static final Set<String> CSS2_PSEUDO_CLASSES = new HashSet<>(Arrays.asList(
            "link", "visited", "hover", "active",
            "focus", "lang", "first-child"));

    private static final Set<String> CSS3_PSEUDO_CLASSES = new HashSet<>(Arrays.asList(
            "checked", "disabled", "enabled", "indeterminated", "root", "target", "not()",
            "nth-child()", "nth-last-child()", "nth-of-type()", "nth-last-of-type()",
            "last-child", "first-of-type", "last-of-type", "only-child", "only-of-type", "empty"));

    static {
        CSS3_PSEUDO_CLASSES.addAll(CSS2_PSEUDO_CLASSES);
    }

    /**
     * Creates a new empty stylesheet.
     */
    @JsxConstructor({ @WebBrowser(CHROME), @WebBrowser(FF) })
    public CSSStyleSheet() {
        wrapped_ = new CSSStyleSheetImpl();
        ownerNode_ = null;
    }

    /**
     * Creates a new stylesheet representing the CSS stylesheet for the specified input source.
     * @param element the owning node
     * @param source the input source which contains the CSS stylesheet which this stylesheet host object represents
     * @param uri this stylesheet's URI (used to resolved contained @import rules)
     */
    public CSSStyleSheet(final HTMLElement element, final InputSource source, final String uri) {
        setParentScope(element.getWindow());
        setPrototype(getPrototype(CSSStyleSheet.class));
        if (source != null) {
            source.setURI(uri);
        }
        wrapped_ = parseCSS(source);
        uri_ = uri;
        ownerNode_ = element;
    }

    /**
     * Creates a new stylesheet representing the specified CSS stylesheet.
     * @param element the owning node
     * @param wrapped the CSS stylesheet which this stylesheet host object represents
     * @param uri this stylesheet's URI (used to resolved contained @import rules)
     */
    public CSSStyleSheet(final HTMLElement element, final org.w3c.dom.css.CSSStyleSheet wrapped, final String uri) {
        setParentScope(element.getWindow());
        setPrototype(getPrototype(CSSStyleSheet.class));
        wrapped_ = wrapped;
        uri_ = uri;
        ownerNode_ = element;
    }

    /**
     * Returns the wrapped stylesheet.
     * @return the wrapped stylesheet
     */
    public org.w3c.dom.css.CSSStyleSheet getWrappedSheet() {
        return wrapped_;
    }

    /**
     * Modifies the specified style object by adding any style rules which apply to the specified
     * element.
     *
     * @param style the style to modify
     * @param element the element to which style rules must apply in order for them to be added to
     *        the specified style
     */
    public void modifyIfNecessary(final ComputedCSSStyleDeclaration style, final Element element) {
        final CSSRuleList rules = getWrappedSheet().getCssRules();
        modifyIfNecessary(style, element, rules, new HashSet<String>());
    }

    private void modifyIfNecessary(final ComputedCSSStyleDeclaration style, final Element element,
        final CSSRuleList rules, final Set<String> alreadyProcessing) {
        if (rules == null) {
            return;
        }

        final BrowserVersion browser = getBrowserVersion();
        final DomElement e = element.getDomNodeOrDie();
        final int rulesLength = rules.getLength();
        for (int i = 0; i < rulesLength; i++) {
            final CSSRule rule = rules.item(i);

            final short ruleType = rule.getType();
            if (CSSRule.STYLE_RULE == ruleType) {
                final CSSStyleRuleImpl styleRule = (CSSStyleRuleImpl) rule;
                final SelectorList selectors = styleRule.getSelectors();
                for (int j = 0; j < selectors.getLength(); j++) {
                    final Selector selector = selectors.item(j);
                    final boolean selected = selects(browser, selector, e);
                    if (selected) {
                        final org.w3c.dom.css.CSSStyleDeclaration dec = styleRule.getStyle();
                        style.applyStyleFromSelector(dec, selector);
                    }
                }
            }
            else if (CSSRule.IMPORT_RULE == ruleType) {
                final CSSImportRuleImpl importRule = (CSSImportRuleImpl) rule;
                final MediaList mediaList = importRule.getMedia();
                if (isActive(mediaList)) {
                    CSSStyleSheet sheet = imports_.get(importRule);
                    if (sheet == null) {
                        // TODO: surely wrong: in which case is it null and why?
                        final String uri = (uri_ != null) ? uri_ : e.getPage().getUrl().toExternalForm();
                        final String href = importRule.getHref();
                        final String url = UrlUtils.resolveUrl(uri, href);
                        sheet = loadStylesheet(getWindow(), ownerNode_, null, url);
                        imports_.put(importRule, sheet);
                    }

                    if (!alreadyProcessing.contains(sheet.getUri())) {
                        final CSSRuleList sheetRules = sheet.getWrappedSheet().getCssRules();
                        alreadyProcessing.add(getUri());
                        sheet.modifyIfNecessary(style, element, sheetRules, alreadyProcessing);
                    }
                }
            }
            else if (CSSRule.MEDIA_RULE == ruleType) {
                final CSSMediaRuleImpl mediaRule = (CSSMediaRuleImpl) rule;
                final MediaList mediaList = mediaRule.getMedia();
                if (isActive(mediaList)) {
                    final CSSRuleList internalRules = mediaRule.getCssRules();
                    modifyIfNecessary(style, element, internalRules, alreadyProcessing);
                }
            }
        }
    }

    /**
     * Loads the stylesheet at the specified link or href.
     * @param window the current window
     * @param element the parent DOM element
     * @param link the stylesheet's link (may be <tt>null</tt> if an <tt>url</tt> is specified)
     * @param url the stylesheet's url (may be <tt>null</tt> if a <tt>link</tt> is specified)
     * @return the loaded stylesheet
     */
    public static CSSStyleSheet loadStylesheet(final Window window, final HTMLElement element,
        final HtmlLink link, final String url) {
        CSSStyleSheet sheet;
        final HtmlPage page = (HtmlPage) element.getDomNodeOrDie().getPage();
        String uri = page.getUrl().toExternalForm(); // fallback uri for exceptions
        try {
            // Retrieve the associated content and respect client settings regarding failing HTTP status codes.
            final WebRequest request;
            final String accept = page.getWebClient().getBrowserVersion().getCssAcceptHeader();
            final WebClient client = page.getWebClient();
            if (link != null) {
                // Use link.
                request = link.getWebRequest();
                request.setAdditionalHeader("Accept", accept);
            }
            else {
                // Use href.
                request = new WebRequest(new URL(url), accept);
                final String referer = page.getUrl().toExternalForm();
                request.setAdditionalHeader("Referer", referer);
            }

            // our cache is a bit strange;
            // loadWebResponse check the cache for the web response
            // AND also fixes the request url for the following cache lookups
            final WebResponse response = client.loadWebResponse(request);

            // now we can look into the cache with the fixed request for
            // a cached script
            final Cache cache = client.getCache();
            final Object fromCache = cache.getCachedObject(request);
            if (fromCache != null && fromCache instanceof org.w3c.dom.css.CSSStyleSheet) {
                uri = request.getUrl().toExternalForm();
                sheet = new CSSStyleSheet(element, (org.w3c.dom.css.CSSStyleSheet) fromCache, uri);
            }
            else {
                uri = response.getWebRequest().getUrl().toExternalForm();
                client.printContentIfNecessary(response);
                client.throwFailingHttpStatusCodeExceptionIfNecessary(response);
                // CSS content must have downloaded OK; go ahead and build the corresponding stylesheet.
                final InputSource source = new InputSource();
                source.setByteStream(response.getContentAsStream());
                source.setEncoding(response.getContentCharset());
                sheet = new CSSStyleSheet(element, source, uri);
                // cache the style sheet
                cache.cacheIfPossible(request, response, sheet.getWrappedSheet());
                response.cleanUp();
            }
        }
        catch (final FailingHttpStatusCodeException e) {
            // Got a 404 response or something like that; behave nicely.
            LOG.error("Exception loading " + uri, e);
            final InputSource source = new InputSource(new StringReader(""));
            sheet = new CSSStyleSheet(element, source, uri);
        }
        catch (final IOException e) {
            // Got a basic IO error; behave nicely.
            LOG.error("IOException loading " + uri, e);
            final InputSource source = new InputSource(new StringReader(""));
            sheet = new CSSStyleSheet(element, source, uri);
        }
        catch (final RuntimeException e) {
            // Got something unexpected; we can throw an exception in this case.
            LOG.error("RuntimeException loading " + uri, e);
            throw Context.reportRuntimeError("Exception: " + e);
        }
        catch (final Exception e) {
            // Got something unexpected; we can throw an exception in this case.
            LOG.error("Exception loading " + uri, e);
            throw Context.reportRuntimeError("Exception: " + e);
        }
        return sheet;
    }

    /**
     * Returns <tt>true</tt> if the specified selector selects the specified element.
     *
     * @param selector the selector to test
     * @param element the element to test
     * @return <tt>true</tt> if it does apply, <tt>false</tt> if it doesn't apply
     */
    boolean selects(final Selector selector, final DomElement element) {
        return selects(getBrowserVersion(), selector, element);
    }

    /**
     * Returns <tt>true</tt> if the specified selector selects the specified element.
     *
     * @param browserVersion the browser version
     * @param selector the selector to test
     * @param element the element to test
     * @return <tt>true</tt> if it does apply, <tt>false</tt> if it doesn't apply
     */
    public static boolean selects(final BrowserVersion browserVersion, final Selector selector,
            final DomElement element) {
        if (selector instanceof GeneralAdjacentSelectorImpl) {
            final SiblingSelector ss = (SiblingSelector) selector;
            final Selector ssSelector = ss.getSelector();
            final SimpleSelector ssSiblingSelector = ss.getSiblingSelector();
            for (DomNode prev = element.getPreviousSibling(); prev != null; prev = prev.getPreviousSibling()) {
                if (prev instanceof HtmlElement
                    && selects(browserVersion, ssSelector, (HtmlElement) prev)
                    && selects(browserVersion, ssSiblingSelector, element)) {
                    return true;
                }
            }
            return false;
        }
        switch (selector.getSelectorType()) {
            case Selector.SAC_ANY_NODE_SELECTOR:
                return true;
            case Selector.SAC_CHILD_SELECTOR:
                final DomNode parentNode = element.getParentNode();
                if (parentNode == element.getPage()) {
                    return false;
                }
                if (!(parentNode instanceof HtmlElement)) {
                    return false; // for instance parent is a DocumentFragment
                }
                final DescendantSelector cs = (DescendantSelector) selector;
                final HtmlElement parent = (HtmlElement) parentNode;
                return selects(browserVersion, cs.getSimpleSelector(), element)
                    && selects(browserVersion, cs.getAncestorSelector(), parent);
            case Selector.SAC_DESCENDANT_SELECTOR:
                final DescendantSelector ds = (DescendantSelector) selector;
                if (selects(browserVersion, ds.getSimpleSelector(), element)) {
                    DomNode ancestor = element.getParentNode();
                    final Selector dsAncestorSelector = ds.getAncestorSelector();
                    while (ancestor instanceof HtmlElement) {
                        if (selects(browserVersion, dsAncestorSelector, (HtmlElement) ancestor)) {
                            return true;
                        }
                        ancestor = ancestor.getParentNode();
                    }
                }
                return false;
            case Selector.SAC_CONDITIONAL_SELECTOR:
                final ConditionalSelector conditional = (ConditionalSelector) selector;
                final Condition condition = conditional.getCondition();
                return selects(browserVersion, conditional.getSimpleSelector(), element)
                    && selects(browserVersion, condition, element);
            case Selector.SAC_ELEMENT_NODE_SELECTOR:
                final ElementSelector es = (ElementSelector) selector;
                final String name = es.getLocalName();
                return name == null || name.equalsIgnoreCase(element.getLocalName());
            case Selector.SAC_ROOT_NODE_SELECTOR:
                return HtmlHtml.TAG_NAME.equalsIgnoreCase(element.getTagName());
            case Selector.SAC_DIRECT_ADJACENT_SELECTOR:
                final SiblingSelector ss = (SiblingSelector) selector;
                DomNode prev = element.getPreviousSibling();
                while (prev != null && !(prev instanceof HtmlElement)) {
                    prev = prev.getPreviousSibling();
                }
                return prev != null
                    && selects(browserVersion, ss.getSelector(), (HtmlElement) prev)
                    && selects(browserVersion, ss.getSiblingSelector(), element);
            case Selector.SAC_NEGATIVE_SELECTOR:
                final NegativeSelector ns = (NegativeSelector) selector;
                return !selects(browserVersion, ns.getSimpleSelector(), element);
            case Selector.SAC_PSEUDO_ELEMENT_SELECTOR:
            case Selector.SAC_COMMENT_NODE_SELECTOR:
            case Selector.SAC_CDATA_SECTION_NODE_SELECTOR:
            case Selector.SAC_PROCESSING_INSTRUCTION_NODE_SELECTOR:
            case Selector.SAC_TEXT_NODE_SELECTOR:
                return false;
            default:
                LOG.error("Unknown CSS selector type '" + selector.getSelectorType() + "'.");
                return false;
        }
    }

    /**
     * Returns <tt>true</tt> if the specified condition selects the specified element.
     *
     * @param browserVersion the browser version
     * @param condition the condition to test
     * @param element the element to test
     * @return <tt>true</tt> if it does apply, <tt>false</tt> if it doesn't apply
     */
    static boolean selects(final BrowserVersion browserVersion, final Condition condition, final DomElement element) {
        if (condition instanceof PrefixAttributeConditionImpl) {
            final AttributeCondition ac = (AttributeCondition) condition;
            final String value = ac.getValue();
            if (browserVersion.hasFeature(CSS_SELECTOR_EMPTY_STRING_HITS_AS_SUBSTRING)) {
                final String attrib = element.getAttribute(ac.getLocalName());
                return attrib != DomElement.ATTRIBUTE_NOT_DEFINED && attrib.startsWith(value);
            }
            return !"".equals(value) && element.getAttribute(ac.getLocalName()).startsWith(value);
        }
        if (condition instanceof SuffixAttributeConditionImpl) {
            final AttributeCondition ac = (AttributeCondition) condition;
            final String value = ac.getValue();
            if (browserVersion.hasFeature(CSS_SELECTOR_EMPTY_STRING_HITS_AS_SUBSTRING)) {
                final String attrib = element.getAttribute(ac.getLocalName());
                return attrib != DomElement.ATTRIBUTE_NOT_DEFINED && attrib.endsWith(value);
            }
            return !"".equals(value) && element.getAttribute(ac.getLocalName()).endsWith(value);
        }
        if (condition instanceof SubstringAttributeConditionImpl) {
            final AttributeCondition ac = (AttributeCondition) condition;
            final String value = ac.getValue();
            if (browserVersion.hasFeature(CSS_SELECTOR_EMPTY_STRING_HITS_AS_SUBSTRING)) {
                final String attrib = element.getAttribute(ac.getLocalName());
                return attrib != DomElement.ATTRIBUTE_NOT_DEFINED && attrib.contains(value);
            }
            return !"".equals(value) && element.getAttribute(ac.getLocalName()).contains(value);
        }
        switch (condition.getConditionType()) {
            case Condition.SAC_ID_CONDITION:
                final AttributeCondition ac4 = (AttributeCondition) condition;
                return ac4.getValue().equals(element.getId());
            case Condition.SAC_CLASS_CONDITION:
                final AttributeCondition ac3 = (AttributeCondition) condition;
                String v3 = ac3.getValue();
                if (v3.indexOf('\\') > -1) {
                    v3 = UNESCAPE_SELECTOR.matcher(v3).replaceAll("$1");
                }
                final String a3 = element.getAttribute("class");
                return selectsWhitespaceSeparated(v3, a3);
            case Condition.SAC_AND_CONDITION:
                final CombinatorCondition cc1 = (CombinatorCondition) condition;
                return selects(browserVersion, cc1.getFirstCondition(), element)
                    && selects(browserVersion, cc1.getSecondCondition(), element);
            case Condition.SAC_ATTRIBUTE_CONDITION:
                final AttributeCondition ac1 = (AttributeCondition) condition;
                if (ac1.getSpecified()) {
                    String value = ac1.getValue();
                    if (value.indexOf('\\') > -1) {
                        value = UNESCAPE_SELECTOR.matcher(value).replaceAll("$1");
                    }
                    final String attrValue = element.getAttribute(ac1.getLocalName());
                    return DomElement.ATTRIBUTE_NOT_DEFINED != attrValue && attrValue.equals(value);
                }
                return element.hasAttribute(ac1.getLocalName());
            case Condition.SAC_BEGIN_HYPHEN_ATTRIBUTE_CONDITION:
                final AttributeCondition ac2 = (AttributeCondition) condition;
                final String v = ac2.getValue();
                final String a = element.getAttribute(ac2.getLocalName());
                return selects(v, a, '-');
            case Condition.SAC_ONE_OF_ATTRIBUTE_CONDITION:
                final AttributeCondition ac5 = (AttributeCondition) condition;
                final String v2 = ac5.getValue();
                final String a2 = element.getAttribute(ac5.getLocalName());
                return selects(v2, a2, ' ');
            case Condition.SAC_OR_CONDITION:
                final CombinatorCondition cc2 = (CombinatorCondition) condition;
                return selects(browserVersion, cc2.getFirstCondition(), element)
                    || selects(browserVersion, cc2.getSecondCondition(), element);
            case Condition.SAC_NEGATIVE_CONDITION:
                final NegativeCondition nc = (NegativeCondition) condition;
                return !selects(browserVersion, nc.getCondition(), element);
            case Condition.SAC_ONLY_CHILD_CONDITION:
                return element.getParentNode().getChildNodes().getLength() == 1;
            case Condition.SAC_CONTENT_CONDITION:
                final ContentCondition cc = (ContentCondition) condition;
                return element.asText().contains(cc.getData());
            case Condition.SAC_LANG_CONDITION:
                if (!browserVersion.hasFeature(CSS_SELECTOR_LANG)) {
                    return false;
                }
                final String lcLang = ((LangCondition) condition).getLang();
                final int lcLangLength = lcLang.length();
                for (DomNode node = element; node instanceof HtmlElement; node = node.getParentNode()) {
                    final String nodeLang = ((HtmlElement) node).getAttribute("lang");
                    // "en", "en-GB" should be matched by "en" but not "english"
                    if (nodeLang.startsWith(lcLang)
                        && (nodeLang.length() == lcLangLength || '-' == nodeLang.charAt(lcLangLength))) {
                        return true;
                    }
                }
                return false;
            case Condition.SAC_ONLY_TYPE_CONDITION:
                final String tagName = element.getTagName();
                return ((HtmlPage) element.getPage()).getElementsByTagName(tagName).getLength() == 1;
            case Condition.SAC_PSEUDO_CLASS_CONDITION:
                return selectsPseudoClass(browserVersion, (AttributeCondition) condition, element);
            case Condition.SAC_POSITIONAL_CONDITION:
                return false;
            default:
                LOG.error("Unknown CSS condition type '" + condition.getConditionType() + "'.");
                return false;
        }
    }

    private static boolean selects(final String condition, final String attribute, final char separator) {
        // attribute.equals(condition)
        // || attribute.startsWith(condition + " ") || attriubte.endsWith(" " + condition)
        // || attribute.contains(" " + condition + " ");

        final int conditionLength = condition.length();
        if (conditionLength < 1) {
            return false;
        }

        final int attribLength = attribute.length();
        if (attribLength < conditionLength) {
            return false;
        }
        if (attribLength > conditionLength) {
            if (separator == attribute.charAt(conditionLength)
                    && attribute.startsWith(condition)) {
                return true;
            }
            if (separator == attribute.charAt(attribLength - conditionLength - 1)
                    && attribute.endsWith(condition)) {
                return true;
            }
            if (attribLength + 1 > conditionLength) {
                final StringBuilder tmp = new StringBuilder(conditionLength + 2);
                tmp.append(separator).append(condition).append(separator);
                return attribute.contains(tmp);
            }
            return false;
        }
        return attribute.equals(condition);
    }

    private static boolean selectsWhitespaceSeparated(final String condition, final String attribute) {
        final int conditionLength = condition.length();
        if (conditionLength < 1) {
            return false;
        }

        final int attribLength = attribute.length();
        if (attribLength < conditionLength) {
            return false;
        }

        int pos = attribute.indexOf(condition);
        while (pos != -1) {
            if (pos > 0 && !Character.isWhitespace(attribute.charAt(pos - 1))) {
                pos = attribute.indexOf(condition, pos + 1);
                continue;
            }

            final int lastPos = pos + condition.length();
            if (lastPos < attribLength && !Character.isWhitespace(attribute.charAt(lastPos))) {
                pos = attribute.indexOf(condition, pos + 1);
                continue;
            }

            return true;
        }

        return false;
    }

    private static boolean selectsPseudoClass(final BrowserVersion browserVersion,
            final AttributeCondition condition, final DomElement element) {
        if (browserVersion.hasFeature(QUERYSELECTORALL_NOT_IN_QUIRKS)) {
            final ScriptableObject sobj = element.getPage().getScriptObject();
            if (sobj instanceof HTMLDocument && ((HTMLDocument) sobj).getDocumentMode() < 8) {
                return false;
            }
        }

        final String value = condition.getValue();
        if ("root".equals(value)) {
            return element == element.getPage().getDocumentElement();
        }
        else if ("enabled".equals(value)) {
            return element instanceof DisabledElement && !((DisabledElement) element).isDisabled();
        }
        if ("disabled".equals(value)) {
            return element instanceof DisabledElement && ((DisabledElement) element).isDisabled();
        }
        if ("focus".equals(value)) {
            final HtmlPage htmlPage = element.getHtmlPageOrNull();
            if (htmlPage != null) {
                final DomElement focus = htmlPage.getFocusedElement();
                return element == focus;
            }
        }
        else if ("checked".equals(value)) {
            return (element instanceof HtmlCheckBoxInput && ((HtmlCheckBoxInput) element).isChecked())
                || (element instanceof HtmlRadioButtonInput && ((HtmlRadioButtonInput) element).isChecked()
                || (element instanceof HtmlOption && ((HtmlOption) element).isSelected()));
        }
        else if ("first-child".equals(value)) {
            for (DomNode n = element.getPreviousSibling(); n != null; n = n.getPreviousSibling()) {
                if (n instanceof DomElement) {
                    return false;
                }
            }
            return true;
        }
        else if ("last-child".equals(value)) {
            for (DomNode n = element.getNextSibling(); n != null; n = n.getNextSibling()) {
                if (n instanceof DomElement) {
                    return false;
                }
            }
            return true;
        }
        else if ("first-of-type".equals(value)) {
            final String type = element.getNodeName();
            for (DomNode n = element.getPreviousSibling(); n != null; n = n.getPreviousSibling()) {
                if (n instanceof DomElement && n.getNodeName().equals(type)) {
                    return false;
                }
            }
            return true;
        }
        else if ("last-of-type".equals(value)) {
            final String type = element.getNodeName();
            for (DomNode n = element.getNextSibling(); n != null; n = n.getNextSibling()) {
                if (n instanceof DomElement && n.getNodeName().equals(type)) {
                    return false;
                }
            }
            return true;
        }
        else if (value.startsWith("nth-child(")) {
            final String nth = value.substring(value.indexOf('(') + 1, value.length() - 1);
            int index = 0;
            for (DomNode n = element; n != null; n = n.getPreviousSibling()) {
                if (n instanceof DomElement) {
                    index++;
                }
            }
            return getNth(nth, index);
        }
        else if (value.startsWith("nth-last-child(")) {
            final String nth = value.substring(value.indexOf('(') + 1, value.length() - 1);
            int index = 0;
            for (DomNode n = element; n != null; n = n.getNextSibling()) {
                if (n instanceof DomElement) {
                    index++;
                }
            }
            return getNth(nth, index);
        }
        else if (value.startsWith("nth-of-type(")) {
            final String type = element.getNodeName();
            final String nth = value.substring(value.indexOf('(') + 1, value.length() - 1);
            int index = 0;
            for (DomNode n = element; n != null; n = n.getPreviousSibling()) {
                if (n instanceof DomElement && n.getNodeName().equals(type)) {
                    index++;
                }
            }
            return getNth(nth, index);
        }
        else if (value.startsWith("nth-last-of-type(")) {
            final String type = element.getNodeName();
            final String nth = value.substring(value.indexOf('(') + 1, value.length() - 1);
            int index = 0;
            for (DomNode n = element; n != null; n = n.getNextSibling()) {
                if (n instanceof DomElement && n.getNodeName().equals(type)) {
                    index++;
                }
            }
            return getNth(nth, index);
        }
        else if ("only-child".equals(value)) {
            for (DomNode n = element.getPreviousSibling(); n != null; n = n.getPreviousSibling()) {
                if (n instanceof DomElement) {
                    return false;
                }
            }
            for (DomNode n = element.getNextSibling(); n != null; n = n.getNextSibling()) {
                if (n instanceof DomElement) {
                    return false;
                }
            }
            return true;
        }
        else if ("only-of-type".equals(value)) {
            final String type = element.getNodeName();
            for (DomNode n = element.getPreviousSibling(); n != null; n = n.getPreviousSibling()) {
                if (n instanceof DomElement && n.getNodeName().equals(type)) {
                    return false;
                }
            }
            for (DomNode n = element.getNextSibling(); n != null; n = n.getNextSibling()) {
                if (n instanceof DomElement && n.getNodeName().equals(type)) {
                    return false;
                }
            }
            return true;
        }
        else if ("empty".equals(value)) {
            return isEmpty(element);
        }
        else if ("target".equals(value)) {
            final String ref = element.getPage().getUrl().getRef();
            return StringUtils.isNotBlank(ref) && ref.equals(element.getId());
        }
        else if (value.startsWith("not(")) {
            final String selectors = value.substring(value.indexOf('(') + 1, value.length() - 1);
            final AtomicBoolean errorOccured = new AtomicBoolean(false);
            final ErrorHandler errorHandler = new ErrorHandler() {
                @Override
                public void warning(final CSSParseException exception) throws CSSException {
                    // ignore
                }

                @Override
                public void fatalError(final CSSParseException exception) throws CSSException {
                    errorOccured.set(true);
                }

                @Override
                public void error(final CSSParseException exception) throws CSSException {
                    errorOccured.set(true);
                }
            };
            final CSSOMParser parser = new CSSOMParser(new SACParserCSS3());
            parser.setErrorHandler(errorHandler);
            try {
                final SelectorList selectorList = parser.parseSelectors(new InputSource(new StringReader(selectors)));
                if (errorOccured.get() || selectorList == null || selectorList.getLength() != 1) {
                    throw new CSSException("Invalid selectors: " + selectors);
                }

                validateSelectors(selectorList, 9, element);

                return !CSSStyleSheet.selects(browserVersion, selectorList.item(0), element);
            }
            catch (final IOException e) {
                throw new CSSException("Error parsing CSS selectors from '" + selectors + "': " + e.getMessage());
            }
        }
        return false;
    }

    private static boolean isEmpty(final DomElement element) {
        for (DomNode n = element.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof DomElement || n instanceof DomText) {
                return false;
            }
        }
        return true;
    }

    private static boolean getNth(final String nth, final int index) {
        if ("odd".equalsIgnoreCase(nth)) {
            return index % 2 != 0;
        }

        if ("even".equalsIgnoreCase(nth)) {
            return index % 2 == 0;
        }

        // an+b
        final int nIndex = nth.indexOf('n');
        int a = 0;
        if (nIndex != -1) {
            String value = nth.substring(0, nIndex).trim();
            if ("-".equals(value)) {
                a = -1;
            }
            else {
                if (value.startsWith("+")) {
                    value = value.substring(1);
                }
                a = NumberUtils.toInt(value, 1);
            }
        }

        String value = nth.substring(nIndex + 1).trim();
        if (value.startsWith("+")) {
            value = value.substring(1);
        }
        final int b = NumberUtils.toInt(value, 0);
        if (a == 0) {
            return index == b && b > 0;
        }

        final double n = (index - b) / (double) a;
        return (n >= 0) && (n % 1 == 0);
    }

    /**
     * Parses the CSS at the specified input source. If anything at all goes wrong, this method
     * returns an empty stylesheet.
     *
     * @param source the source from which to retrieve the CSS to be parsed
     * @return the stylesheet parsed from the specified input source
     */
    private org.w3c.dom.css.CSSStyleSheet parseCSS(final InputSource source) {
        org.w3c.dom.css.CSSStyleSheet ss;
        try {
            final ErrorHandler errorHandler = getWindow().getWebWindow().getWebClient().getCssErrorHandler();
            final CSSOMParser parser = new CSSOMParser(new SACParserCSS3());
            parser.setErrorHandler(errorHandler);
            ss = parser.parseStyleSheet(source, null, null);
        }
        catch (final Throwable t) {
            LOG.error("Error parsing CSS from '" + toString(source) + "': " + t.getMessage(), t);
            ss = new CSSStyleSheetImpl();
        }
        return ss;
    }

    /**
     * Parses the selectors at the specified input source. If anything at all goes wrong, this
     * method returns an empty selector list.
     *
     * @param source the source from which to retrieve the selectors to be parsed
     * @return the selectors parsed from the specified input source
     */
    public SelectorList parseSelectors(final InputSource source) {
        SelectorList selectors;
        try {
            final ErrorHandler errorHandler = getWindow().getWebWindow().getWebClient().getCssErrorHandler();
            final CSSOMParser parser = new CSSOMParser(new SACParserCSS3());
            parser.setErrorHandler(errorHandler);
            selectors = parser.parseSelectors(source);
            // in case of error parseSelectors returns null
            if (null == selectors) {
                selectors = new SelectorListImpl();
            }
        }
        catch (final Throwable t) {
            LOG.error("Error parsing CSS selectors from '" + toString(source) + "': " + t.getMessage(), t);
            selectors = new SelectorListImpl();
        }
        return selectors;
    }

    /**
     * Parses the given media string. If anything at all goes wrong, this
     * method returns an empty SACMediaList list.
     *
     * @param source the source from which to retrieve the media to be parsed
     * @return the media parsed from the specified input source
     */
    static SACMediaList parseMedia(final ErrorHandler errorHandler, final String mediaString) {
        try {
            final CSSOMParser parser = new CSSOMParser(new SACParserCSS3());
            parser.setErrorHandler(errorHandler);

            final InputSource source = new InputSource(new StringReader(mediaString));
            final SACMediaList media = parser.parseMedia(source);
            if (media != null) {
                return media;
            }
        }
        catch (final Exception e) {
            LOG.error("Error parsing CSS media from '" + mediaString + "': " + e.getMessage(), e);
        }
        return new SACMediaListImpl();
    }

    /**
     * Returns the contents of the specified input source, ignoring any {@link IOException}s.
     * @param source the input source from which to read
     * @return the contents of the specified input source, or an empty string if an {@link IOException} occurs
     */
    private static String toString(final InputSource source) {
        try {
            final Reader reader = source.getCharacterStream();
            if (null != reader) {
                // try to reset to produce some output
                if (reader instanceof StringReader) {
                    final StringReader sr = (StringReader) reader;
                    sr.reset();
                }
                return IOUtils.toString(reader);
            }
            final InputStream is = source.getByteStream();
            if (null != is) {
                // try to reset to produce some output
                if (is instanceof ByteArrayInputStream) {
                    final ByteArrayInputStream bis = (ByteArrayInputStream) is;
                    bis.reset();
                }
                return IOUtils.toString(is);
            }
            return "";
        }
        catch (final IOException e) {
            return "";
        }
    }

    /**
     * For Firefox.
     * @return the owner
     */
    @JsxGetter({ @WebBrowser(FF), @WebBrowser(value = IE, minVersion = 11), @WebBrowser(CHROME) })
    public HTMLElement getOwnerNode() {
        return ownerNode_;
    }

    /**
     * For Internet Explorer.
     * @return the owner
     */
    @JsxGetter(@WebBrowser(IE))
    public HTMLElement getOwningElement() {
        return ownerNode_;
    }

    /**
     * Retrieves the collection of rules defined in this style sheet.
     * @return the collection of rules defined in this style sheet
     */
    @JsxGetter({ @WebBrowser(IE), @WebBrowser(CHROME) })
    public com.gargoylesoftware.htmlunit.javascript.host.css.CSSRuleList getRules() {
        return getCssRules();
    }

    /**
     * Returns the collection of rules defined in this style sheet.
     * @return the collection of rules defined in this style sheet
     */
    @JsxGetter({ @WebBrowser(FF), @WebBrowser(CHROME), @WebBrowser(value = IE, minVersion = 11) })
    public com.gargoylesoftware.htmlunit.javascript.host.css.CSSRuleList getCssRules() {
        if (cssRules_ == null) {
            cssRules_ = new com.gargoylesoftware.htmlunit.javascript.host.css.CSSRuleList(this);
        }
        return cssRules_;
    }

    /**
     * Returns the URL of the stylesheet.
     * @return the URL of the stylesheet
     */
    @JsxGetter
    public String getHref() {
        final BrowserVersion version = getBrowserVersion();

        if (ownerNode_ != null) {
            final DomNode node = ownerNode_.getDomNodeOrDie();
            if (node instanceof HtmlLink) {
                // <link rel="stylesheet" type="text/css" href="..." />
                final HtmlLink link = (HtmlLink) node;
                final HtmlPage page = (HtmlPage) link.getPage();
                final String href = link.getHrefAttribute();
                if ("".equals(href) && version.hasFeature(STYLESHEET_HREF_EMPTY_IS_NULL)) {
                    return null;
                }
                if (!version.hasFeature(STYLESHEET_HREF_EXPANDURL)) {
                    // Don't expand relative URLs.
                    return href;
                }
                // Expand relative URLs.
                try {
                    final URL url = page.getFullyQualifiedUrl(href);
                    return url.toExternalForm();
                }
                catch (final MalformedURLException e) {
                    // Log the error and fall through to the return values below.
                    LOG.warn(e.getMessage(), e);
                }
            }
        }

        // <style type="text/css"> ... </style>
        if (version.hasFeature(STYLESHEET_HREF_STYLE_EMPTY)) {
            return "";
        }
        return null;
    }

    /**
     * Inserts a new rule.
     * @param rule the CSS rule
     * @param position the position at which to insert the rule
     * @see <a href="http://www.w3.org/TR/DOM-Level-2-Style/css.html#CSS-CSSStyleSheet">DOM level 2</a>
     * @return the position of the inserted rule
     */
    @JsxFunction({ @WebBrowser(FF), @WebBrowser(CHROME), @WebBrowser(value = IE, minVersion = 11) })
    public int insertRule(final String rule, final int position) {
        try {
            return wrapped_.insertRule(rule, position);
        }
        catch (final DOMException e) {
            throw Context.throwAsScriptRuntimeEx(e);
        }
    }

    /**
     * Deletes an existing rule.
     * @param position the position of the rule to be deleted
     * @see <a href="http://www.w3.org/TR/DOM-Level-2-Style/css.html#CSS-CSSStyleSheet">DOM level 2</a>
     */
    @JsxFunction({ @WebBrowser(FF), @WebBrowser(value = IE, minVersion = 11), @WebBrowser(CHROME) })
    public void deleteRule(final int position) {
        try {
            wrapped_.deleteRule(position);
        }
        catch (final DOMException e) {
            throw Context.throwAsScriptRuntimeEx(e);
        }
    }

    /**
     * Adds a new rule.
     * @see <a href="http://msdn.microsoft.com/en-us/library/aa358796.aspx">MSDN</a>
     * @param selector the selector name
     * @param rule the rule
     * @return always return -1 as of MSDN documentation
     */
    @JsxFunction({ @WebBrowser(IE), @WebBrowser(CHROME) })
    public int addRule(final String selector, final String rule) {
        final String completeRule = selector + " {" + rule + "}";
        try {
            wrapped_.insertRule(completeRule, wrapped_.getCssRules().getLength());
        }
        catch (final DOMException e) {
            throw Context.throwAsScriptRuntimeEx(e);
        }
        return -1;
    }

    /**
     * Deletes an existing rule.
     * @param position the position of the rule to be deleted
     * @see <a href="http://msdn.microsoft.com/en-us/library/ms531195(v=VS.85).aspx">MSDN</a>
     */
    @JsxFunction({ @WebBrowser(IE), @WebBrowser(CHROME) })
    public void removeRule(final int position) {
        try {
            wrapped_.deleteRule(position);
        }
        catch (final DOMException e) {
            throw Context.throwAsScriptRuntimeEx(e);
        }
    }

    /**
     * Returns this stylesheet's URI (used to resolved contained @import rules).
     * @return this stylesheet's URI (used to resolved contained @import rules)
     */
    public String getUri() {
        return uri_;
    }

    /**
     * Returns <tt>true</tt> if this stylesheet is active, based on the media types it is associated with (if any).
     * @return <tt>true</tt> if this stylesheet is active, based on the media types it is associated with (if any)
     */
    public boolean isActive() {
        final String media;
        final HtmlElement e = ownerNode_.getDomNodeOrNull();
        if (e instanceof HtmlStyle) {
            final HtmlStyle style = (HtmlStyle) e;
            media = style.getMediaAttribute();
        }
        else if (e instanceof HtmlLink) {
            final HtmlLink link = (HtmlLink) e;
            media = link.getMediaAttribute();
        }
        else {
            return true;
        }

        if (StringUtils.isBlank(media)) {
            return true;
        }

        final WebClient webClient = getWindow().getWebWindow().getWebClient();
        final SACMediaList mediaList = parseMedia(webClient.getCssErrorHandler(), media);
        return isActive(new MediaListImpl(mediaList));
    }

    /**
     * Returns whether the specified {@link MediaList} is active or not.
     * @param mediaList the media list
     * @return whether the specified {@link MediaList} is active or not
     */
    static boolean isActive(final MediaList mediaList) {
        if (mediaList.getLength() == 0) {
            return true;
        }

        for (int i = 0; i < mediaList.getLength(); i++) {
            final String mediaType = mediaList.item(i);
            if ("screen".equalsIgnoreCase(mediaType) || "all".equalsIgnoreCase(mediaType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Validates the list of selectors.
     * @param selectorList the selectors
     * @param documentMode see {@link HTMLDocument#getDocumentMode()}
     * @param domNode the dom node the query should work on
     * @throws CSSException if a selector is invalid
     */
    public static void validateSelectors(final SelectorList selectorList, final int documentMode,
                final DomNode domNode) throws CSSException {
        for (int i = 0; i < selectorList.getLength(); ++i) {
            final Selector item = selectorList.item(i);
            if (!isValidSelector(item, documentMode, domNode)) {
                throw new CSSException("Invalid selector: " + item);
            }
        }
    }

    /**
     * @param documentMode see {@link HTMLDocument#getDocumentMode()}
     */
    private static boolean isValidSelector(final Selector selector, final int documentMode, final DomNode domNode) {
        switch (selector.getSelectorType()) {
            case Selector.SAC_ELEMENT_NODE_SELECTOR:
                return true;
            case Selector.SAC_CONDITIONAL_SELECTOR:
                final ConditionalSelector conditional = (ConditionalSelector) selector;
                return isValidSelector(conditional.getSimpleSelector(), documentMode, domNode)
                        && isValidSelector(conditional.getCondition(), documentMode, domNode);
            case Selector.SAC_DESCENDANT_SELECTOR:
            case Selector.SAC_CHILD_SELECTOR:
                final DescendantSelector ds = (DescendantSelector) selector;
                return isValidSelector(ds.getAncestorSelector(), documentMode, domNode)
                        && isValidSelector(ds.getSimpleSelector(), documentMode, domNode);
            case Selector.SAC_DIRECT_ADJACENT_SELECTOR:
                final SiblingSelector ss = (SiblingSelector) selector;
                return isValidSelector(ss.getSelector(), documentMode, domNode)
                        && isValidSelector(ss.getSiblingSelector(), documentMode, domNode);
            case Selector.SAC_ANY_NODE_SELECTOR:
                if (selector instanceof SiblingSelector) {
                    final SiblingSelector sibling = (SiblingSelector) selector;
                    return isValidSelector(sibling.getSelector(), documentMode, domNode)
                            && isValidSelector(sibling.getSiblingSelector(), documentMode, domNode);
                }
            default:
                LOG.warn("Unhandled CSS selector type '" + selector.getSelectorType() + "'. Accepting it silently.");
                return true; // at least in a first time to break less stuff
        }
    }

    /**
     * @param documentMode see {@link HTMLDocument#getDocumentMode()}
     */
    private static boolean isValidSelector(final Condition condition, final int documentMode, final DomNode domNode) {
        switch (condition.getConditionType()) {
            case Condition.SAC_AND_CONDITION:
                final CombinatorCondition cc1 = (CombinatorCondition) condition;
                return isValidSelector(cc1.getFirstCondition(), documentMode, domNode)
                        && isValidSelector(cc1.getSecondCondition(), documentMode, domNode);
            case Condition.SAC_ATTRIBUTE_CONDITION:
            case Condition.SAC_ID_CONDITION:
            case Condition.SAC_CLASS_CONDITION:
                return true;
            case Condition.SAC_PSEUDO_CLASS_CONDITION:
                final PseudoClassConditionImpl pcc = (PseudoClassConditionImpl) condition;
                String value = pcc.getValue();
                if (value.endsWith(")")) {
                    if (value.endsWith("()")) {
                        return false;
                    }
                    value = value.substring(0, value.indexOf('(') + 1) + ')';
                }
                if (documentMode < 9) {
                    return CSS2_PSEUDO_CLASSES.contains(value);
                }

                if (!CSS2_PSEUDO_CLASSES.contains(value)
                        && domNode.hasFeature(QUERYSELECTOR_CSS3_PSEUDO_REQUIRE_ATTACHED_NODE)
                        && !domNode.isDirectlyAttachedToPage()) {
                    throw new CSSException("Syntax Error");
                }

                if ("nth-child()".equals(value)) {
                    final String arg = StringUtils.substringBetween(pcc.getValue(), "(", ")").trim();
                    return "even".equalsIgnoreCase(arg) || "odd".equalsIgnoreCase(arg)
                            || NTH_NUMERIC.matcher(arg).matches()
                            || NTH_COMPLEX.matcher(arg).matches();
                }
                return CSS3_PSEUDO_CLASSES.contains(value);
            default:
                LOG.warn("Unhandled CSS condition type '" + condition.getConditionType() + "'. Accepting it silently.");
                return true;
        }
    }
}
