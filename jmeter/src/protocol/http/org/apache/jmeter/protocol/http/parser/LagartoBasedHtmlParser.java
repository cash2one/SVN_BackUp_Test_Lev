/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.jmeter.protocol.http.parser;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Iterator;
import java.util.Stack;

import jodd.lagarto.EmptyTagVisitor;
import jodd.lagarto.LagartoException;
import jodd.lagarto.LagartoParser;
import jodd.lagarto.LagartoParserConfig;
import jodd.lagarto.Tag;
import jodd.lagarto.TagType;
import jodd.lagarto.TagUtil;
import jodd.lagarto.dom.HtmlCCommentExpressionMatcher;
import jodd.log.LoggerFactory;
import jodd.log.impl.Slf4jLoggerFactory;

import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.protocol.http.util.ConversionUtils;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

/**
 * Parser based on Lagarto
 * @since 2.10
 */
public class LagartoBasedHtmlParser extends HTMLParser {
    private static final Logger log = LoggingManager.getLoggerForClass();
    static {
        LoggerFactory.setLoggerFactory(new Slf4jLoggerFactory());
    }

    /*
     * A dummy class to pass the pointer of URL.
     */
    private static class URLPointer {
        private URLPointer(URL newUrl) {
            url = newUrl;
        }
        private URL url;
    }
    
    private static final class JMeterTagVisitor extends EmptyTagVisitor {
        private HtmlCCommentExpressionMatcher htmlCCommentExpressionMatcher;
        private URLCollection urls;
        private URLPointer baseUrl;
        private Float ieVersion;
        private Stack<Boolean> enabled = new Stack<Boolean>();

        /**
         * @param baseUrl base url to add possibly missing information to urls found in <code>urls</code>
         * @param urls collection of urls to consider
         * @param ieVersion version number of IE to emulate
         */
        public JMeterTagVisitor(final URLPointer baseUrl, URLCollection urls, Float ieVersion) {
            this.urls = urls;
            this.baseUrl = baseUrl;
            this.ieVersion = ieVersion;
        }

        private final void extractAttribute(Tag tag, String attributeName) {
            CharSequence url = tag.getAttributeValue(attributeName);
            if (!StringUtils.isEmpty(url)) {
                urls.addURL(url.toString(), baseUrl.url);
            }
        }
        /*
         * (non-Javadoc)
         * 
         * @see jodd.lagarto.EmptyTagVisitor#script(jodd.lagarto.Tag,
         * java.lang.CharSequence)
         */
        @Override
        public void script(Tag tag, CharSequence body) {
            if (!enabled.peek().booleanValue()) {
                return;
            }
            extractAttribute(tag, ATT_SRC);
        }

        /*
         * (non-Javadoc)
         * 
         * @see jodd.lagarto.EmptyTagVisitor#tag(jodd.lagarto.Tag)
         */
        @Override
        public void tag(Tag tag) {
            if (!enabled.peek().booleanValue()) {
                return;
            }
            TagType tagType = tag.getType();
            switch (tagType) {
            case START:
            case SELF_CLOSING:
                if (tag.nameEquals(TAG_BODY)) {
                    extractAttribute(tag, ATT_BACKGROUND);
                } else if (tag.nameEquals(TAG_BASE)) {
                    CharSequence baseref = tag.getAttributeValue(ATT_HREF);
                    try {
                        if (!StringUtils.isEmpty(baseref))// Bugzilla 30713
                        {
                            baseUrl.url = ConversionUtils.makeRelativeURL(baseUrl.url, baseref.toString());
                        }
                    } catch (MalformedURLException e1) {
                        throw new RuntimeException(e1);
                    }
                } else if (tag.nameEquals(TAG_IMAGE)) {
                    extractAttribute(tag, ATT_SRC);
                } else if (tag.nameEquals(TAG_APPLET)) {
                    extractAttribute(tag, ATT_CODE);
                } else if (tag.nameEquals(TAG_OBJECT)) {
                    extractAttribute(tag, ATT_CODEBASE);                
                    extractAttribute(tag, ATT_DATA);                 
                } else if (tag.nameEquals(TAG_INPUT)) {
                    // we check the input tag type for image
                    CharSequence type = tag.getAttributeValue(ATT_TYPE);
                    if (type != null && TagUtil.equalsIgnoreCase(ATT_IS_IMAGE, type)) {
                        // then we need to download the binary
                        extractAttribute(tag, ATT_SRC);
                    }
                } else if (tag.nameEquals(TAG_SCRIPT)) {
                    extractAttribute(tag, ATT_SRC);
                    // Bug 51750
                } else if (tag.nameEquals(TAG_FRAME) || tag.nameEquals(TAG_IFRAME)) {
                    extractAttribute(tag, ATT_SRC);
                } else if (tag.nameEquals(TAG_EMBED)) {
                    extractAttribute(tag, ATT_SRC);
                } else if (tag.nameEquals(TAG_BGSOUND)){
                    extractAttribute(tag, ATT_SRC);
                } else if (tag.nameEquals(TAG_LINK)) {
                    CharSequence relAttribute = tag.getAttributeValue(ATT_REL);
                    // Putting the string first means it works even if the attribute is null
                    if (relAttribute != null && TagUtil.equalsIgnoreCase(STYLESHEET,relAttribute)) {
                        extractAttribute(tag, ATT_HREF);
                    }
                } else {
                    extractAttribute(tag, ATT_BACKGROUND);
                }
    
    
                // Now look for URLs in the STYLE attribute
                CharSequence styleTagStr = tag.getAttributeValue(ATT_STYLE);
                if(!StringUtils.isEmpty(styleTagStr)) {
                    HtmlParsingUtils.extractStyleURLs(baseUrl.url, urls, styleTagStr.toString());
                }
                break;
            case END:
                break;
            }
        }

        /* (non-Javadoc)
         * @see jodd.lagarto.EmptyTagVisitor#condComment(java.lang.CharSequence, boolean, boolean, boolean)
         */
        @Override
        public void condComment(CharSequence expression, boolean isStartingTag,
                boolean isHidden, boolean isHiddenEndTag) {
            // See http://css-tricks.com/how-to-create-an-ie-only-stylesheet/
            if(!isStartingTag) {
                enabled.pop();
            } else {
                if (htmlCCommentExpressionMatcher == null) {
                    htmlCCommentExpressionMatcher = new HtmlCCommentExpressionMatcher();
                }
                String expressionString = expression.toString().trim();
                enabled.push(Boolean.valueOf(htmlCCommentExpressionMatcher.match(ieVersion.floatValue(),
                        expressionString)));                
            }
        }

        /* (non-Javadoc)
         * @see jodd.lagarto.EmptyTagVisitor#start()
         */
        @Override
        public void start() {
            super.start();
            enabled.clear();
            enabled.push(Boolean.TRUE);
        }
    }

    @Override
    public Iterator<URL> getEmbeddedResourceURLs(String userAgent, byte[] html, URL baseUrl,
            URLCollection coll, String encoding) throws HTMLParseException {
        try {
            Float ieVersion = extractIEVersion(userAgent);
            
            String contents = new String(html,encoding); 
            // As per Jodd javadocs, emitStrings should be false for visitor for better performances
            LagartoParser lagartoParser = new LagartoParser(contents, false);
            LagartoParserConfig<LagartoParserConfig<?>> config = new LagartoParserConfig<LagartoParserConfig<?>>();
            config.setCaseSensitive(false);
            // Conditional comments only apply for IE < 10
            config.setEnableConditionalComments(isEnableConditionalComments(ieVersion));
            
            lagartoParser.setConfig(config);
            JMeterTagVisitor tagVisitor = new JMeterTagVisitor(new URLPointer(baseUrl), coll, ieVersion);
            lagartoParser.parse(tagVisitor);
            return coll.iterator();
        } catch (LagartoException e) {
            // TODO is it the best way ? https://bz.apache.org/bugzilla/show_bug.cgi?id=55634
            if(log.isDebugEnabled()) {
                log.debug("Error extracting embedded resource URLs from:'"+baseUrl+"', probably not text content, message:"+e.getMessage());
            }
            return Collections.<URL>emptyList().iterator();
        } catch (Exception e) {
            throw new HTMLParseException(e);
        }
    }

    



    /* (non-Javadoc)
     * @see org.apache.jmeter.protocol.http.parser.HTMLParser#isReusable()
     */
    @Override
    protected boolean isReusable() {
        return true;
    }
}
