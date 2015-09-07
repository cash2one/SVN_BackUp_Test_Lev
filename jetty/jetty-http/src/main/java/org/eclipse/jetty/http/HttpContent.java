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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import org.eclipse.jetty.http.MimeTypes.Type;
import org.eclipse.jetty.util.resource.Resource;

/* ------------------------------------------------------------ */
/** HttpContent interface.
 * <p>This information represents all the information about a 
 * static resource that is needed to evaluate conditional headers
 * and to serve the content if need be.     It can be implemented
 * either transiently (values and fields generated on demand) or 
 * persistently (values and fields pre-generated in anticipation of
 * reuse in from a cache).
 * </p> 
 *
 */
public interface HttpContent
{
    HttpField getContentType();
    String getContentTypeValue();
    String getCharacterEncoding();
    Type getMimeType();

    HttpField getContentLength();
    long getContentLengthValue();
    
    HttpField getLastModified();
    String getLastModifiedValue();
    
    HttpField getETag();
    String getETagValue();
    
    ByteBuffer getIndirectBuffer();
    ByteBuffer getDirectBuffer();
    Resource getResource();
    InputStream getInputStream() throws IOException;
    ReadableByteChannel getReadableByteChannel() throws IOException;
    void release();

}
