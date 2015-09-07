//
//  ========================================================================
//  Copyright (c) 1995-2015 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class MimeTypesTest
{
    @Test
    public void testGetMimeByExtension_Gzip()
    {
        assertMimeTypeByExtension("application/gzip","test.gz");
    }

    @Test
    public void testGetMimeByExtension_Png()
    {
        assertMimeTypeByExtension("image/png","test.png");
        assertMimeTypeByExtension("image/png","TEST.PNG");
        assertMimeTypeByExtension("image/png","Test.Png");
    }

    @Test
    public void testGetMimeByExtension_Png_MultiDot()
    {
        assertMimeTypeByExtension("image/png","org.eclipse.jetty.Logo.png");
    }

    @Test
    public void testGetMimeByExtension_Png_DeepPath()
    {
        assertMimeTypeByExtension("image/png","/org/eclipse/jetty/Logo.png");
    }

    @Test
    public void testGetMimeByExtension_Text()
    {
        assertMimeTypeByExtension("text/plain","test.txt");
        assertMimeTypeByExtension("text/plain","TEST.TXT");
    }
    
    @Test
    public void testGetMimeByExtension_NoExtension()
    {
        MimeTypes mimetypes = new MimeTypes();
        String contentType = mimetypes.getMimeByExtension("README");
        assertNull(contentType);
    }

    private void assertMimeTypeByExtension(String expectedMimeType, String filename)
    {
        MimeTypes mimetypes = new MimeTypes();
        String contentType = mimetypes.getMimeByExtension(filename);
        String prefix = "MimeTypes.getMimeByExtension(" + filename + ")";
        assertNotNull(prefix,contentType);
        assertEquals(prefix,expectedMimeType,contentType);
    }

    @Test
    public void testCharsetFromContentType()
    {
        assertEquals("abc",MimeTypes.getCharsetFromContentType("foo/bar;charset=abc;some=else"));
        assertEquals("abc",MimeTypes.getCharsetFromContentType("foo/bar;charset=abc"));
        assertEquals("abc",MimeTypes.getCharsetFromContentType("foo/bar ; charset = abc"));
        assertEquals("abc",MimeTypes.getCharsetFromContentType("foo/bar ; charset = abc ; some=else"));
        assertEquals("abc",MimeTypes.getCharsetFromContentType("foo/bar;other=param;charset=abc;some=else"));
        assertEquals("abc",MimeTypes.getCharsetFromContentType("foo/bar;other=param;charset=abc"));
        assertEquals("abc",MimeTypes.getCharsetFromContentType("foo/bar other = param ; charset = abc"));
        assertEquals("abc",MimeTypes.getCharsetFromContentType("foo/bar other = param ; charset = abc ; some=else"));
        assertEquals("abc",MimeTypes.getCharsetFromContentType("foo/bar other = param ; charset = abc"));
        assertEquals("abc",MimeTypes.getCharsetFromContentType("foo/bar other = param ; charset = \"abc\" ; some=else"));
        assertEquals(null,MimeTypes.getCharsetFromContentType("foo/bar"));
        assertEquals("utf-8",MimeTypes.getCharsetFromContentType("foo/bar;charset=uTf8"));
        assertEquals("utf-8",MimeTypes.getCharsetFromContentType("foo/bar;other=\"charset=abc\";charset=uTf8"));
        assertEquals("utf-8",MimeTypes.getCharsetFromContentType("text/html;charset=utf-8"));

    }

    @Test
    public void testContentTypeWithoutCharset()
    {
        assertEquals("foo/bar;some=else",MimeTypes.getContentTypeWithoutCharset("foo/bar;charset=abc;some=else"));
        assertEquals("foo/bar",MimeTypes.getContentTypeWithoutCharset("foo/bar;charset=abc"));
        assertEquals("foo/bar",MimeTypes.getContentTypeWithoutCharset("foo/bar ; charset = abc"));
        assertEquals("foo/bar;some=else",MimeTypes.getContentTypeWithoutCharset("foo/bar ; charset = abc ; some=else"));
        assertEquals("foo/bar;other=param;some=else",MimeTypes.getContentTypeWithoutCharset("foo/bar;other=param;charset=abc;some=else"));
        assertEquals("foo/bar;other=param",MimeTypes.getContentTypeWithoutCharset("foo/bar;other=param;charset=abc"));
        assertEquals("foo/bar ; other = param",MimeTypes.getContentTypeWithoutCharset("foo/bar ; other = param ; charset = abc"));
        assertEquals("foo/bar ; other = param;some=else",MimeTypes.getContentTypeWithoutCharset("foo/bar ; other = param ; charset = abc ; some=else"));
        assertEquals("foo/bar ; other = param",MimeTypes.getContentTypeWithoutCharset("foo/bar ; other = param ; charset = abc"));
        assertEquals("foo/bar ; other = param;some=else",MimeTypes.getContentTypeWithoutCharset("foo/bar ; other = param ; charset = \"abc\" ; some=else"));
        assertEquals("foo/bar",MimeTypes.getContentTypeWithoutCharset("foo/bar"));
        assertEquals("foo/bar",MimeTypes.getContentTypeWithoutCharset("foo/bar;charset=uTf8"));
        assertEquals("foo/bar;other=\"charset=abc\"",MimeTypes.getContentTypeWithoutCharset("foo/bar;other=\"charset=abc\";charset=uTf8"));
        assertEquals("text/html",MimeTypes.getContentTypeWithoutCharset("text/html;charset=utf-8"));
    }
}
