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
package com.gargoylesoftware.htmlunit.javascript.host;

import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.CHROME;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.FF;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.IE;

import com.gargoylesoftware.htmlunit.javascript.configuration.JsxClass;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxConstructor;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxGetter;
import com.gargoylesoftware.htmlunit.javascript.configuration.WebBrowser;

/**
 * A JavaScript object for a document.navigator.plugins.
 *
 * @version $Revision: 10961 $
 * @author Marc Guillemot
 * @author Ahmed Ashour
 *
 * @see <a href="http://www.xulplanet.com/references/objref/MimeTypeArray.html">XUL Planet</a>
 */
@JsxClass(browsers = { @WebBrowser(CHROME), @WebBrowser(FF), @WebBrowser(value = IE, minVersion = 11) })
public class Plugin extends SimpleArray {
    private String description_;
    private String filename_;
    private String name_;
    private String version_;

    /**
     * Creates an instance.
     */
    @JsxConstructor({ @WebBrowser(CHROME), @WebBrowser(FF) })
    public Plugin() {
    }

    /**
     * C'tor initializing fields.
     * @param name the plugin name
     * @param description the plugin description
     * @param version the version
     * @param filename the plugin filename
     */
    public Plugin(final String name, final String description, final String version, final String filename) {
        name_ = name;
        description_ = description;
        version_ = version;
        filename_ = filename;
    }

    /**
     * Gets the name of the mime type.
     * @param element a {@link MimeType}
     * @return the name
     */
    @Override
    protected String getItemName(final Object element) {
        return ((MimeType) element).getType();
    }

    /**
     * Gets the plugin's description.
     * @return the description
     */
    @JsxGetter
    public String getDescription() {
        return description_;
    }

    /**
     * Gets the plugin's file name.
     * @return the file name
     */
    @JsxGetter
    public String getFilename() {
        return filename_;
    }

    /**
     * Gets the plugin's name.
     * @return the name
     */
    @JsxGetter
    public String getName() {
        return name_;
    }

    /**
     * Gets the plugin's version.
     * @return the name
     */
    @JsxGetter({ @WebBrowser(value = IE, minVersion = 11), @WebBrowser(FF) })
    public String getVersion() {
        return version_;
    }
}
