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

import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_STYLESHEETLIST_ACTIVE_ONLY;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_STYLESHEETLIST_EXCEPTION_FOR_NEGATIVE_INDEX;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_STYLESHEETLIST_EXCEPTION_FOR_TOO_HIGH_INDEX;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.CHROME;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.FF;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.IE;
import net.sourceforge.htmlunit.corejs.javascript.Context;
import net.sourceforge.htmlunit.corejs.javascript.Scriptable;

import org.apache.commons.lang3.StringUtils;
import org.w3c.css.sac.SACMediaList;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.HtmlAttributeChangeEvent;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlLink;
import com.gargoylesoftware.htmlunit.html.HtmlStyle;
import com.gargoylesoftware.htmlunit.javascript.SimpleScriptable;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxClass;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxClasses;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxConstructor;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxFunction;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxGetter;
import com.gargoylesoftware.htmlunit.javascript.configuration.WebBrowser;
import com.gargoylesoftware.htmlunit.javascript.host.html.HTMLCollection;
import com.gargoylesoftware.htmlunit.javascript.host.html.HTMLDocument;
import com.gargoylesoftware.htmlunit.javascript.host.html.HTMLElement;
import com.gargoylesoftware.htmlunit.javascript.host.html.HTMLLinkElement;
import com.gargoylesoftware.htmlunit.javascript.host.html.HTMLStyleElement;
import com.steadystate.css.dom.MediaListImpl;

/**
 * <p>An ordered list of stylesheets, accessible via <tt>document.styleSheets</tt>, as specified by the
 * <a href="http://www.w3.org/TR/DOM-Level-2-Style/stylesheets.html#StyleSheets-StyleSheetList">DOM
 * Level 2 Style spec</a> and the <a href="https://developer.mozilla.org/en-US/docs/DOM/document.styleSheets">Gecko
 * DOM Guide</a>.</p>
 *
 * <p>If CSS is disabled via {@link com.gargoylesoftware.htmlunit.WebClientOptions#setCssEnabled(boolean)}, instances
 * of this class will always be empty. This allows us to check for CSS enablement/disablement in a single
 * location, without having to sprinkle checks throughout the code.</p>
 *
 * @version $Revision: 10940 $
 * @author Daniel Gredler
 * @author Ahmed Ashour
 * @author Ronald Brill
 * @author Frank Danek
 */
@JsxClasses({
        @JsxClass(browsers = { @WebBrowser(CHROME), @WebBrowser(FF), @WebBrowser(value = IE, minVersion = 11) }),
        @JsxClass(isJSObject = false, browsers = @WebBrowser(value = IE, maxVersion = 8))
    })
public class StyleSheetList extends SimpleScriptable {

    /**
     * We back the stylesheet list with an {@link HTMLCollection} of styles/links because this list
     * must be "live".
     */
    private HTMLCollection nodes_;

    /**
     * <span style="color:red">INTERNAL API - SUBJECT TO CHANGE AT ANY TIME - USE AT YOUR OWN RISK.</span><br>
     *
     * Verifies if the provided node is a link node pointing to a stylesheet.
     *
     * @param domNode the mode to check
     * @return true if the provided node is a stylesheet link
     */
    public static boolean isStyleSheetLink(final DomNode domNode) {
        return domNode instanceof HtmlLink
                && "stylesheet".equalsIgnoreCase(((HtmlLink) domNode).getRelAttribute());
    }

    /**
     * <span style="color:red">INTERNAL API - SUBJECT TO CHANGE AT ANY TIME - USE AT YOUR OWN RISK.</span><br>
     *
     * Verifies if the provided node is a link node pointing to an active stylesheet.
     *
     * @param domNode the mode to check
     * @return true if the provided node is a stylesheet link
     */
    public boolean isActiveStyleSheetLink(final DomNode domNode) {
        if (domNode instanceof HtmlLink) {
            final HtmlLink link = (HtmlLink) domNode;
            if ("stylesheet".equalsIgnoreCase(link.getRelAttribute())) {
                final String media = link.getMediaAttribute();
                if (StringUtils.isBlank(media)) {
                    return true;
                }
                final WebClient webClient = getWindow().getWebWindow().getWebClient();
                final SACMediaList mediaList = CSSStyleSheet.parseMedia(webClient.getCssErrorHandler(), media);
                return CSSStyleSheet.isActive(new MediaListImpl(mediaList));
            }
        }
        return false;
    }

    /**
     * Creates an instance.
     */
    @JsxConstructor({ @WebBrowser(CHROME), @WebBrowser(FF) })
    public StyleSheetList() {
    }

    /**
     * Creates a new style sheet list owned by the specified document.
     *
     * @param document the owning document
     */
    public StyleSheetList(final HTMLDocument document) {
        setParentScope(document);
        setPrototype(getPrototype(getClass()));

        final WebClient webClient = getWindow().getWebWindow().getWebClient();
        final boolean cssEnabled = webClient.getOptions().isCssEnabled();
        final boolean onlyActive = webClient.getBrowserVersion().hasFeature(JS_STYLESHEETLIST_ACTIVE_ONLY);

        if (cssEnabled) {
            nodes_ = new HTMLCollection(document.getDomNodeOrDie(), true, "stylesheets") {
                @Override
                protected boolean isMatching(final DomNode node) {
                    if (node instanceof HtmlStyle) {
                        return true;
                    }
                    if (onlyActive) {
                        return isActiveStyleSheetLink(node);
                    }
                    return isStyleSheetLink(node);
                }

                @Override
                protected EffectOnCache getEffectOnCache(final HtmlAttributeChangeEvent event) {
                    final HtmlElement node = event.getHtmlElement();
                    if (node instanceof HtmlLink && "rel".equalsIgnoreCase(event.getName())) {
                        return EffectOnCache.RESET;
                    }
                    return EffectOnCache.NONE;
                }
            };
        }
        else {
            nodes_ = HTMLCollection.emptyCollection(getWindow());
        }
    }

    /**
     * Returns the list's length.
     *
     * @return the list's length
     */
    @JsxGetter
    public int getLength() {
        return nodes_.getLength();
    }

    /**
     * Returns the style sheet at the specified index.
     *
     * @param index the index of the style sheet to return
     * @return the style sheet at the specified index
     */
    @JsxFunction
    public Object item(final int index) {
        if (index < 0) {
            if (getWindow().getBrowserVersion().hasFeature(
                    JS_STYLESHEETLIST_EXCEPTION_FOR_NEGATIVE_INDEX)) {
                throw Context.reportRuntimeError("Invalid negative index: " + index);
            }
            return Context.getUndefinedValue();
        }
        else if (index >= nodes_.getLength()) {
            if (getWindow().getBrowserVersion().hasFeature(
                    JS_STYLESHEETLIST_EXCEPTION_FOR_TOO_HIGH_INDEX)) {
                throw Context.reportRuntimeError("Invalid index: " + index);
            }
            return Context.getUndefinedValue();
        }

        final HTMLElement element = (HTMLElement) nodes_.item(Integer.valueOf(index));

        final CSSStyleSheet sheet;
        // <style type="text/css"> ... </style>
        if (element instanceof HTMLStyleElement) {
            sheet = ((HTMLStyleElement) element).getSheet();
        }
        else {
            // <link rel="stylesheet" type="text/css" href="..." />
            sheet = ((HTMLLinkElement) element).getSheet();
        }

        return sheet;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object get(final int index, final Scriptable start) {
        if (this == start) {
            return item(index);
        }
        return super.get(index, start);
    }
}
