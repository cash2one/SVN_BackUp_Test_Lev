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
package com.gargoylesoftware.htmlunit.javascript.host.svg;

import static com.gargoylesoftware.htmlunit.javascript.configuration.BrowserName.CHROME;

import com.gargoylesoftware.htmlunit.javascript.SimpleScriptable;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxClass;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxConstant;
import com.gargoylesoftware.htmlunit.javascript.configuration.JsxConstructor;
import com.gargoylesoftware.htmlunit.javascript.configuration.WebBrowser;

/**
 * A JavaScript object for {@code SVGRenderingIntent}.
 *
 * @version $Revision: 10726 $
 * @author Ahmed Ashour
 */
@JsxClass(browsers = @WebBrowser(CHROME))
public class SVGRenderingIntent extends SimpleScriptable {

    /** The constant {@code RENDERING_INTENT_UNKNOWN}. */
    @JsxConstant
    public static final int RENDERING_INTENT_UNKNOWN = 0;
    /** The constant {@code RENDERING_INTENT_AUTO}. */
    @JsxConstant
    public static final int RENDERING_INTENT_AUTO = 1;
    /** The constant {@code RENDERING_INTENT_PERCEPTUAL}. */
    @JsxConstant
    public static final int RENDERING_INTENT_PERCEPTUAL = 2;
    /** The constant {@code RENDERING_INTENT_RELATIVE_COLORIMETRIC}. */
    @JsxConstant
    public static final int RENDERING_INTENT_RELATIVE_COLORIMETRIC = 3;
    /** The constant {@code RENDERING_INTENT_SATURATION}. */
    @JsxConstant
    public static final int RENDERING_INTENT_SATURATION = 4;
    /** The constant {@code RENDERING_INTENT_ABSOLUTE_COLORIMETRIC}. */
    @JsxConstant
    public static final int RENDERING_INTENT_ABSOLUTE_COLORIMETRIC = 5;

    /**
     * Creates a new instance.
     */
    @JsxConstructor
    public SVGRenderingIntent() {
    }
}
