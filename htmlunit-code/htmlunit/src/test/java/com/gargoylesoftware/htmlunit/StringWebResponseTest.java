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

import org.junit.Test;

/**
 * Unit tests for {@link StringWebResponse}.
 *
 * @version $Revision: 9855 $
 * @author Marc Guillemot
 * @author Carsten Steul
 */
public class StringWebResponseTest extends SimpleWebTestCase {

    /**
     * Regression test for bug 2998004.
     */
    @Test
    public void charset() {
        final StringWebResponse webResponse = new StringWebResponse("hello", "UTF-8", getDefaultUrl());
        assertEquals("UTF-8", webResponse.getContentCharset());
    }

    /**
     * Regression test for bug #1660.
     */
    @Test
    public void charsetInContent() {
        final String content = "<html><head>"
                + "<meta http-equiv='Content-Type' content='text/html; charset=windows-1250' />"
                + "</head><body>\u010C\u00CDSLO</body></html>";
        final StringWebResponse webResponse = new StringWebResponse(content, "UTF-8", getDefaultUrl());

        assertEquals("UTF-8", webResponse.getContentCharset());
        assertEquals(content, webResponse.getContentAsString());
    }
}
