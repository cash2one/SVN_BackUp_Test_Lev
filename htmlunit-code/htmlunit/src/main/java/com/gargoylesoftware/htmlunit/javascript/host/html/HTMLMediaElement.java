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
package com.gargoylesoftware.htmlunit.javascript.host.html;

import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.CHROME;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.FF;
import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.IE;

import com.gargoylesoftware.htmlunit.html.HtmlMedia;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxClass;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxConstant;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxConstructor;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxFunction;
import com.gargoylesoftware.htmlunit.javascript.configuration.WebBrowser;

/**
 * The JavaScript object "HTMLMediaElement".
 *
 * @version $Revision: 10234 $
 * @author Ahmed Ashour
 */
@JsxClass(browsers = { @WebBrowser(CHROME), @WebBrowser(FF), @WebBrowser(value = IE, minVersion = 11) })
public class HTMLMediaElement extends HTMLElement {

    /**
     * No information is available about the media resource.
     */
    @JsxConstant
    public static final short HAVE_NOTHING = 0;

    /**
     * Enough of the media resource has been retrieved that the metadata attributes are initialized.
     * Seeking will no longer raise an exception.
     */
    @JsxConstant
    public static final short HAVE_METADATA = 1;

    /**
     * Data is available for the current playback position, but not enough to actually play more than one frame.
     */
    @JsxConstant
    public static final short HAVE_CURRENT_DATA = 2;

    /**
     * Data for the current playback position as well as for at least a little bit of time
     * into the future is available (in other words, at least two frames of video, for example).
     */
    @JsxConstant
    public static final short HAVE_FUTURE_DATA = 3;

    /**
     * Enough data is available—and the download rate is high enough—that the media
     * can be played through to the end without interruption.
     */
    @JsxConstant
    public static final short HAVE_ENOUGH_DATA = 4;

    /** There is no data yet.  The {@link #getReadyState} is also {@link #HAVE_NOTHING}. */
    @JsxConstant
    public static final short NETWORK_EMPTY = 0;

    /** Network is idle. */
    @JsxConstant
    public static final short NETWORK_IDLE = 1;

    /** The media is loading. */
    @JsxConstant
    public static final short NETWORK_LOADING = 2;

    /** There is no source. */
    @JsxConstant
    public static final short NETWORK_NO_SOURCE = 3;

    /**
     * Creates an instance.
     */
    @JsxConstructor({ @WebBrowser(CHROME), @WebBrowser(FF) })
    public HTMLMediaElement() {
    }

    /**
     * Determines whether the specified media type can be played back.
     * @param type the type
     * @return "probably", "maybe", or ""
     */
    @JsxFunction
    public String canPlayType(final String type) {
        return ((HtmlMedia) getDomNodeOrDie()).canPlayType(type);
    }
}
