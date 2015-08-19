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
package com.gargoylesoftware.htmlunit;

/**
 * A generic page that will be returned for any text related content.
 * Specifically any content types that start with {@code text/}
 *
 * @version $Revision: 10913 $
 * @author <a href="mailto:mbowler@GargoyleSoftware.com">Mike Bowler</a>
 * @author David K. Taylor
 * @author Ronald Brill
 * @author Ahmed Ashour
 */
public class TextPage extends AbstractPage {

    /**
     * Creates an instance.
     *
     * @param webResponse the response from the server
     * @param enclosingWindow the window that holds the page
     */
    public TextPage(final WebResponse webResponse, final WebWindow enclosingWindow) {
        super(webResponse, enclosingWindow);
    }

    /**
     * Returns the content of this page.
     *
     * @return the content of this page
     */
    public String getContent() {
        return getWebResponse().getContentAsString();
    }
}
