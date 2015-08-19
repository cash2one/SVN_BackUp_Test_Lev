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

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link TextUtil}.
 *
 * @version $Revision: 10674 $
 * @author <a href="mailto:mbowler@GargoyleSoftware.com">Mike Bowler</a>
 */
public final class TextUtilTest extends SimpleWebTestCase {

    /**
     * @throws Exception if the test fails
     */
    @Test
    public void toInputStream_null() throws Exception {
        try {
            TextUtil.toInputStream(null);
            fail("Expected NullPointerException");
        }
        catch (final NullPointerException e) {
            // Expected path
        }
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    public void toInputStream() throws Exception {
        final String[][] data = {
            {"", null},
            {"a", "a"},
            {"abcdefABCDEF", "abcdefABCDEF"},
        };
        final String encoding = "ISO-8859-1";

        for (final String[] entry : data) {
            final String input = entry[0];
            final String expectedResult = entry[1];

            final InputStream inputStream = TextUtil.toInputStream(input, encoding);
            final String actualResult = new BufferedReader(new InputStreamReader(inputStream, encoding)).readLine();
            Assert.assertEquals(expectedResult, actualResult);
        }
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    public void stringToByteArray() throws Exception {
        byte[] result = TextUtil.stringToByteArray(null, "UTF-8");
        Assert.assertEquals(0, result.length);

        result = TextUtil.stringToByteArray("", "UTF-8");
        Assert.assertEquals(0, result.length);

        result = TextUtil.stringToByteArray("htmlunit", "UTF-8");
        Assert.assertEquals(8, result.length);
        Assert.assertEquals(104, result[0]);

        result = TextUtil.stringToByteArray("htmlunit", "Klingon");
        Assert.assertEquals(0, result.length);
    }

}
