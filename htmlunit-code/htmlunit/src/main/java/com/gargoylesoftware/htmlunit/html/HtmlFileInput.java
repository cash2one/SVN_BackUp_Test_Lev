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

import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.FILEINPUT_EMPTY_DEFAULT_VALUE;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.SgmlPage;
import com.gargoylesoftware.htmlunit.util.KeyDataPair;
import com.gargoylesoftware.htmlunit.util.NameValuePair;

/**
 * Wrapper for the HTML element "input".
 *
 * @version $Revision: 10933 $
 * @author <a href="mailto:mbowler@GargoyleSoftware.com">Mike Bowler</a>
 * @author <a href="mailto:cse@dynabean.de">Christian Sell</a>
 * @author Daniel Gredler
 * @author Ahmed Ashour
 * @author Marc Guillemot
 * @author Frank Danek
 * @author Ronald Brill
 */
public class HtmlFileInput extends HtmlInput {

    private static final String FILE_SEPARATOR = "\u00A7";

    private String contentType_;
    private byte[] data_;

    /**
     * Creates an instance.
     *
     * @param qualifiedName the qualified name of the element type to instantiate
     * @param page the page that contains this element
     * @param attributes the initial attributes
     */
    HtmlFileInput(final String qualifiedName, final SgmlPage page,
            final Map<String, DomAttr> attributes) {
        super(qualifiedName, page, addValueIfNeeded(page, attributes));

        if (hasFeature(FILEINPUT_EMPTY_DEFAULT_VALUE)) {
            setDefaultValue("", false);
        }
        else {
            for (final Map.Entry<String, DomAttr> entry : attributes.entrySet()) {
                if ("value".equalsIgnoreCase(entry.getKey())) {
                    setDefaultValue(entry.getValue().getNodeValue(), false);
                }
            }
        }
    }

    /**
     * Add missing attribute if needed by fixing attribute map rather to add it afterwards as this second option
     * triggers the instantiation of the script object at a time where the DOM node has not yet been added to its
     * parent.
     */
    private static Map<String, DomAttr> addValueIfNeeded(final SgmlPage page,
            final Map<String, DomAttr> attributes) {

        // we need a copy here because we have to check attributes later again
        final Map<String, DomAttr> result = new HashMap<>(attributes);
        final DomAttr newAttr = new DomAttr(page, null, "value", "", true);
        result.put("value", newAttr);

        return result;
    }

    /**
     * Returns the in-memory data assigned to this file input element, if any.
     * @return {@code null} if {@link #setData(byte[])} hasn't be used
     */
    public final byte[] getData() {
        return data_;
    }

    /**
     * <p>Assigns in-memory data to this file input element. During submission, instead
     * of loading data from a file, the data is read from in-memory byte array.</p>
     *
     * <p>NOTE: Only use this method if you wish to upload in-memory data; if you instead
     * wish to upload the contents of an actual file, use {@link #setValueAttribute(String)},
     * passing in the path to the file.</p>
     *
     * @param data the in-memory data assigned to this file input element
     */
    public final void setData(final byte[] data) {
        data_ = data;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NameValuePair[] getSubmitKeyValuePairs() {
        final String valueAttribute = getValueAttribute();

        if (StringUtils.isEmpty(valueAttribute)) {
            return new NameValuePair[] {new KeyDataPair(getNameAttribute(), new File(""), null, null)};
        }

        final List<NameValuePair> list = new ArrayList<>();
        for (File file : splitFiles(valueAttribute)) {
            // contentType and charset are determined from browser and page
            // perhaps it could be interesting to have setters for it in this class
            // to give finer control to user
            final String contentType;
            if (contentType_ == null) {
                contentType = getPage().getWebClient().guessContentType(file);
            }
            else {
                contentType = contentType_;
            }
            final String charset = getPage().getPageEncoding();
            final KeyDataPair keyDataPair = new KeyDataPair(getNameAttribute(), file, contentType, charset);
            keyDataPair.setData(data_);
            list.add(keyDataPair);
        }
        return list.toArray(new NameValuePair[list.size()]);
    }

    /**
     * <span style="color:red">INTERNAL API - SUBJECT TO CHANGE AT ANY TIME - USE AT YOUR OWN RISK.</span><br>
     *
     * @param valueAttribute the string to split
     * @return the list of files
     */
    public static List<File> splitFiles(final String valueAttribute) {
        final List<File> files = new LinkedList<>();
        for (String value : valueAttribute.split(FILE_SEPARATOR)) {
            File file = null;
            // to tolerate file://
            if (value.startsWith("file:/")) {
                if (value.startsWith("file://") && !value.startsWith("file:///")) {
                    value = "file:///" + value.substring(7);
                }
                try {
                    file = new File(new URI(value));
                }
                catch (final URISyntaxException e) {
                    // nothing here
                }
            }

            if (file == null) {
                file = new File(value);
            }

            files.add(file);
        }
        return files;
    }

    /**
     * {@inheritDoc} This method <b>does nothing</b> for file input elements.
     * @see SubmittableElement#reset()
     */
    @Override
    public void reset() {
        // Empty.
    }

    /**
     * {@inheritDoc} Overridden so that this does not set the value attribute when emulating
     * Netscape browsers.
     * @see HtmlInput#setDefaultValue(String)
     */
    @Override
    public void setDefaultValue(final String defaultValue) {
        setDefaultValue(defaultValue, false);
    }

    /**
     * Sets the content type value that should be sent together with the uploaded file.
     * If content type is not explicitly set, HtmlUnit will try to guess it from the file content.
     * @param contentType the content type (<tt>null</tt> resets it)
     */
    public void setContentType(final String contentType) {
        contentType_ = contentType;
    }

    /**
     * Gets the content type that should be sent together with the uploaded file.
     * @return the content type, or <tt>null</tt> if this has not been explicitly set
     * and should be guessed from file content
     */
    public String getContentType() {
        return contentType_;
    }

    /**
     * Used to specify <code>multiple</code> paths to upload.
     *
     * The current implementation splits the value based on '§'.
     * We may follow WebDriver solution, once made,
     * see https://code.google.com/p/selenium/issues/detail?id=2239
     * @param paths the list of paths of the files to upload
     * @return the page contained by this element's window after the value is set
     */
    public Page setValueAttribute(final String[] paths) {
        if (getAttribute("multiple") == ATTRIBUTE_NOT_DEFINED) {
            throw new IllegalStateException("HtmlFileInput is not 'multiple'.");
        }
        final StringBuilder builder = new StringBuilder();
        for (final String p : paths) {
            if (builder.length() != 0) {
                builder.append(FILE_SEPARATOR);
            }
            builder.append(p);
        }
        return super.setValueAttribute(builder.toString());
    }

}
