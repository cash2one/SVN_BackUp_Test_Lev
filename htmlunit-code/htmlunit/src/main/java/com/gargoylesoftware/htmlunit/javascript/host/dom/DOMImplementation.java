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
package com.gargoylesoftware.htmlunit.javascript.host.dom;

import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_DOMIMPLEMENTATION_FEATURE_CORE_1;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_DOMIMPLEMENTATION_FEATURE_CORE_3;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_DOMIMPLEMENTATION_FEATURE_CSS2_1;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_DOMIMPLEMENTATION_FEATURE_CSS2_2;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_DOMIMPLEMENTATION_FEATURE_CSS2_3;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_DOMIMPLEMENTATION_FEATURE_CSS3_1;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_DOMIMPLEMENTATION_FEATURE_CSS3_2;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_DOMIMPLEMENTATION_FEATURE_CSS3_3;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_DOMIMPLEMENTATION_FEATURE_CSS_1;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_DOMIMPLEMENTATION_FEATURE_CSS_2;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_DOMIMPLEMENTATION_FEATURE_CSS_3;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_DOMIMPLEMENTATION_FEATURE_EVENTS_1;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_DOMIMPLEMENTATION_FEATURE_EVENTS_3;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_DOMIMPLEMENTATION_FEATURE_HTML_3;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_DOMIMPLEMENTATION_FEATURE_KEYBOARDEVENTS;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_DOMIMPLEMENTATION_FEATURE_LS;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_DOMIMPLEMENTATION_FEATURE_MOUSEEVENTS_1;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_DOMIMPLEMENTATION_FEATURE_MOUSEEVENTS_2;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_DOMIMPLEMENTATION_FEATURE_MUTATIONEVENTS_1;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_DOMIMPLEMENTATION_FEATURE_MUTATIONEVENTS_2;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_DOMIMPLEMENTATION_FEATURE_MUTATIONNAMEEVENTS;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_DOMIMPLEMENTATION_FEATURE_ONLY_HTML;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_DOMIMPLEMENTATION_FEATURE_RANGE_1;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_DOMIMPLEMENTATION_FEATURE_RANGE_2;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_DOMIMPLEMENTATION_FEATURE_RANGE_3;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_DOMIMPLEMENTATION_FEATURE_STYLESHEETS;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_DOMIMPLEMENTATION_FEATURE_SVG_BASICSTRUCTURE_1_0;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_DOMIMPLEMENTATION_FEATURE_SVG_SHAPE_1_0;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_DOMIMPLEMENTATION_FEATURE_TEXTEVENTS;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_DOMIMPLEMENTATION_FEATURE_TRAVERSAL_1;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_DOMIMPLEMENTATION_FEATURE_TRAVERSAL_2;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_DOMIMPLEMENTATION_FEATURE_TRAVERSAL_3;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_DOMIMPLEMENTATION_FEATURE_UIEVENTS_2;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_DOMIMPLEMENTATION_FEATURE_UIEVENTS_3;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_DOMIMPLEMENTATION_FEATURE_VALIDATION;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_DOMIMPLEMENTATION_FEATURE_VIEWS_1;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_DOMIMPLEMENTATION_FEATURE_VIEWS_2;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_DOMIMPLEMENTATION_FEATURE_VIEWS_3;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_DOMIMPLEMENTATION_FEATURE_XHTML_1;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_DOMIMPLEMENTATION_FEATURE_XHTML_3;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_DOMIMPLEMENTATION_FEATURE_XML_3;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_DOMIMPLEMENTATION_FEATURE_XPATH;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.CHROME;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.FF;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.IE;

import com.gargoylesoftware.htmlunit.javascript.SimpleScriptable;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxClass;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxClasses;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxConstructor;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxFunction;
import com.gargoylesoftware.htmlunit.javascript.configuration.WebBrowser;
import com.gargoylesoftware.htmlunit.javascript.host.xml.XMLDocument;
import com.gargoylesoftware.htmlunit.xml.XmlPage;

/**
 * A JavaScript object for DOMImplementation.
 *
 * @version $Revision: 10653 $
 * @author Ahmed Ashour
 * @author Frank Danek
 * @author Ronald Brill
 *
 * @see <a href="http://www.w3.org/TR/2000/WD-DOM-Level-1-20000929/level-one-core.html#ID-102161490">
 * W3C Dom Level 1</a>
 */
@JsxClasses({
        @JsxClass(browsers = { @WebBrowser(CHROME), @WebBrowser(FF), @WebBrowser(value = IE, minVersion = 11) }),
        @JsxClass(isJSObject = false, browsers = @WebBrowser(value = IE, maxVersion = 8))
    })
public class DOMImplementation extends SimpleScriptable {

    /**
     * Creates an instance.
     */
    @JsxConstructor({ @WebBrowser(CHROME), @WebBrowser(FF) })
    public DOMImplementation() {
    }

    /**
     * Test if the DOM implementation implements a specific feature.
     * @param feature the name of the feature to test (case-insensitive)
     * @param version the version number of the feature to test
     * @return true if the feature is implemented in the specified version, false otherwise
     */
    @JsxFunction
    public boolean hasFeature(final String feature, final String version) {
        if (getBrowserVersion().hasFeature(JS_DOMIMPLEMENTATION_FEATURE_ONLY_HTML)) {
            if ("HTML".equals(feature) && "1.0".equals(version)) {
                return true;
            }
        }
        else {
            if ("Core".equals(feature)) {
                if ("1.0".equals(version)) {
                    return getBrowserVersion().hasFeature(JS_DOMIMPLEMENTATION_FEATURE_CORE_1);
                }
                if ("2.0".equals(version)) {
                    return true;
                }
                if ("3.0".equals(version)) {
                    return getBrowserVersion().hasFeature(JS_DOMIMPLEMENTATION_FEATURE_CORE_3);
                }
            }
            else if ("HTML".equals(feature)) {
                if ("1.0".equals(version) || "2.0".equals(version)) {
                    return true;
                }
                if ("3.0".equals(version)) {
                    return getBrowserVersion().hasFeature(JS_DOMIMPLEMENTATION_FEATURE_HTML_3);
                }
            }
            else if ("XHTML".equals(feature)) {
                if ("1.0".equals(version) || "2.0".equals(version)) {
                    return getBrowserVersion().hasFeature(JS_DOMIMPLEMENTATION_FEATURE_XHTML_1);
                }
                if ("3.0".equals(version)) {
                    return getBrowserVersion().hasFeature(JS_DOMIMPLEMENTATION_FEATURE_XHTML_3);
                }
            }
            else if ("XML".equals(feature)) {
                if ("1.0".equals(version) || "2.0".equals(version)) {
                    return true;
                }
                if ("3.0".equals(version)) {
                    return getBrowserVersion().hasFeature(JS_DOMIMPLEMENTATION_FEATURE_XML_3);
                }
            }
            else if ("Views".equals(feature)) {
                if ("1.0".equals(version)) {
                    return getBrowserVersion().hasFeature(JS_DOMIMPLEMENTATION_FEATURE_VIEWS_1);
                }
                if ("2.0".equals(version)) {
                    return getBrowserVersion().hasFeature(JS_DOMIMPLEMENTATION_FEATURE_VIEWS_2);
                }
                if ("3.0".equals(version)) {
                    return getBrowserVersion().hasFeature(JS_DOMIMPLEMENTATION_FEATURE_VIEWS_3);
                }
            }
            else if ("StyleSheets".equals(feature)) {
                return getBrowserVersion().hasFeature(JS_DOMIMPLEMENTATION_FEATURE_STYLESHEETS);
            }
            else if ("CSS".equals(feature)) {
                if ("1.0".equals(version)) {
                    return getBrowserVersion().hasFeature(JS_DOMIMPLEMENTATION_FEATURE_CSS_1);
                }
                if ("2.0".equals(version)) {
                    return getBrowserVersion().hasFeature(JS_DOMIMPLEMENTATION_FEATURE_CSS_2);
                }
                if ("3.0".equals(version)) {
                    return getBrowserVersion().hasFeature(JS_DOMIMPLEMENTATION_FEATURE_CSS_3);
                }
            }
            else if ("CSS2".equals(feature)) {
                if ("1.0".equals(version)) {
                    return getBrowserVersion().hasFeature(JS_DOMIMPLEMENTATION_FEATURE_CSS2_1);
                }
                if ("2.0".equals(version)) {
                    return getBrowserVersion().hasFeature(JS_DOMIMPLEMENTATION_FEATURE_CSS2_2);
                }
                if ("3.0".equals(version)) {
                    return getBrowserVersion().hasFeature(JS_DOMIMPLEMENTATION_FEATURE_CSS2_3);
                }
            }
            else if ("CSS3".equals(feature)) {
                if ("1.0".equals(version)) {
                    return getBrowserVersion().hasFeature(JS_DOMIMPLEMENTATION_FEATURE_CSS3_1);
                }
                if ("2.0".equals(version)) {
                    return getBrowserVersion().hasFeature(JS_DOMIMPLEMENTATION_FEATURE_CSS3_2);
                }
                if ("3.0".equals(version)) {
                    return getBrowserVersion().hasFeature(JS_DOMIMPLEMENTATION_FEATURE_CSS3_3);
                }
            }
            else if ("Events".equals(feature)) {
                if ("1.0".equals(version)) {
                    return getBrowserVersion().hasFeature(JS_DOMIMPLEMENTATION_FEATURE_EVENTS_1);
                }
                if ("2.0".equals(version) || "3.0".equals(version)) {
                    return getBrowserVersion().hasFeature(JS_DOMIMPLEMENTATION_FEATURE_EVENTS_3);
                }
            }
            else if ("HTMLEvents".equals(feature)) {
                if ("1.0".equals(version)) {
                    return getBrowserVersion().hasFeature(JS_DOMIMPLEMENTATION_FEATURE_EVENTS_1);
                }
                if ("2.0".equals(version) || "3.0".equals(version)) {
                    return getBrowserVersion().hasFeature(JS_DOMIMPLEMENTATION_FEATURE_EVENTS_3);
                }
            }
            else if ("UIEvents".equals(feature)) {
                if ("1.0".equals(version) || "2.0".equals(version)) {
                    return getBrowserVersion().hasFeature(JS_DOMIMPLEMENTATION_FEATURE_UIEVENTS_2);
                }
                if ("3.0".equals(version)) {
                    return getBrowserVersion().hasFeature(JS_DOMIMPLEMENTATION_FEATURE_UIEVENTS_3);
                }
            }
            else if ("KeyboardEvents".equals(feature)) {
                return getBrowserVersion().hasFeature(JS_DOMIMPLEMENTATION_FEATURE_KEYBOARDEVENTS);
            }
            else if ("MouseEvents".equals(feature)) {
                if ("1.0".equals(version)) {
                    return getBrowserVersion().hasFeature(JS_DOMIMPLEMENTATION_FEATURE_MOUSEEVENTS_1);
                }
                if ("2.0".equals(version) || "3.0".equals(version)) {
                    return getBrowserVersion().hasFeature(JS_DOMIMPLEMENTATION_FEATURE_MOUSEEVENTS_2);
                }
            }
            else if ("MutationEvents".equals(feature)) {
                if ("1.0".equals(version)) {
                    return getBrowserVersion().hasFeature(JS_DOMIMPLEMENTATION_FEATURE_MUTATIONEVENTS_1);
                }
                if ("2.0".equals(version) || "3.0".equals(version)) {
                    return getBrowserVersion().hasFeature(JS_DOMIMPLEMENTATION_FEATURE_MUTATIONEVENTS_2);
                }
            }
            else if ("MutationNameEvents".equals(feature)) {
                return getBrowserVersion().hasFeature(JS_DOMIMPLEMENTATION_FEATURE_MUTATIONNAMEEVENTS);
            }
            else if ("TextEvents".equals(feature)) {
                return getBrowserVersion().hasFeature(JS_DOMIMPLEMENTATION_FEATURE_TEXTEVENTS);
            }
            else if ("LS".equals(feature) || "LS-Async".equals(feature)) {
                return getBrowserVersion().hasFeature(JS_DOMIMPLEMENTATION_FEATURE_LS);
            }
            else if ("Range".equals(feature)) {
                if ("1.0".equals(version)) {
                    return getBrowserVersion().hasFeature(JS_DOMIMPLEMENTATION_FEATURE_RANGE_1);
                }
                if ("2.0".equals(version)) {
                    return getBrowserVersion().hasFeature(JS_DOMIMPLEMENTATION_FEATURE_RANGE_2);
                }
                if ("3.0".equals(version)) {
                    return getBrowserVersion().hasFeature(JS_DOMIMPLEMENTATION_FEATURE_RANGE_3);
                }
            }
            else if ("Traversal".equals(feature)) {
                if ("1.0".equals(version)) {
                    return getBrowserVersion().hasFeature(JS_DOMIMPLEMENTATION_FEATURE_TRAVERSAL_1);
                }
                if ("2.0".equals(version)) {
                    return getBrowserVersion().hasFeature(JS_DOMIMPLEMENTATION_FEATURE_TRAVERSAL_2);
                }
                if ("3.0".equals(version)) {
                    return getBrowserVersion().hasFeature(JS_DOMIMPLEMENTATION_FEATURE_TRAVERSAL_3);
                }
            }
            else if ("Validation".equals(feature)) {
                return getBrowserVersion().hasFeature(JS_DOMIMPLEMENTATION_FEATURE_VALIDATION);
            }
            else if ("XPath".equals(feature)) {
                return getBrowserVersion().hasFeature(JS_DOMIMPLEMENTATION_FEATURE_XPATH);
            }
            else if ("http://www.w3.org/TR/SVG11/feature#BasicStructure".equals(feature)) {
                if ("1.0".equals(version)
                        && getBrowserVersion().hasFeature(JS_DOMIMPLEMENTATION_FEATURE_SVG_BASICSTRUCTURE_1_0)) {
                    return true;
                }
                if ("1.1".equals(version)) {
                    return true;
                }
            }
            else if ("http://www.w3.org/TR/SVG11/feature#Shape".equals(feature)) {
                if ("1.0".equals(version)
                        && getBrowserVersion().hasFeature(JS_DOMIMPLEMENTATION_FEATURE_SVG_SHAPE_1_0)) {
                    return true;
                }
                if ("1.1".equals(version)) {
                    return true;
                }
            }
            //TODO: other features.
        }
        return false;
    }

    /**
     * Creates an {@link XMLDocument}.
     *
     * @param namespaceURI the URI that identifies an XML namespace
     * @param qualifiedName the qualified name of the document to instantiate
     * @param doctype the document types of the document
     * @return the newly created {@link XMLDocument}
     */
    //TODO: change doctype type to "DocType"
    @JsxFunction({ @WebBrowser(FF), @WebBrowser(CHROME), @WebBrowser(value = IE, minVersion = 11) })
    public XMLDocument createDocument(final String namespaceURI, final String qualifiedName,
            final Object doctype) {
        final XMLDocument document = new XMLDocument(getWindow().getWebWindow());
        document.setParentScope(getParentScope());
        document.setPrototype(getPrototype(document.getClass()));
        if (qualifiedName != null && !qualifiedName.isEmpty()) {
            final XmlPage page = (XmlPage) document.getDomNodeOrDie();
            page.appendChild(page.createXmlElementNS("".equals(namespaceURI) ? null : namespaceURI, qualifiedName));
        }
        return document;
    }
}
