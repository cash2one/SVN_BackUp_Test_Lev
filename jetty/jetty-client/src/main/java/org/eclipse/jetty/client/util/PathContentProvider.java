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

package org.eclipse.jetty.client.util;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.eclipse.jetty.client.api.ContentProvider;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

/**
 * A {@link ContentProvider} for files using JDK 7's {@code java.nio.file} APIs.
 * <p>
 * It is possible to specify, at the constructor, a buffer size used to read content from the
 * stream, by default 4096 bytes.
 */
public class PathContentProvider extends AbstractTypedContentProvider
{
    private static final Logger LOG = Log.getLogger(PathContentProvider.class);

    private final Path filePath;
    private final long fileSize;
    private final int bufferSize;

    public PathContentProvider(Path filePath) throws IOException
    {
        this(filePath, 4096);
    }

    public PathContentProvider(Path filePath, int bufferSize) throws IOException
    {
        this("application/octet-stream", filePath, bufferSize);
    }

    public PathContentProvider(String contentType, Path filePath) throws IOException
    {
        this(contentType, filePath, 4096);
    }

    public PathContentProvider(String contentType, Path filePath, int bufferSize) throws IOException
    {
        super(contentType);
        if (!Files.isRegularFile(filePath))
            throw new NoSuchFileException(filePath.toString());
        if (!Files.isReadable(filePath))
            throw new AccessDeniedException(filePath.toString());
        this.filePath = filePath;
        this.fileSize = Files.size(filePath);
        this.bufferSize = bufferSize;
    }

    @Override
    public long getLength()
    {
        return fileSize;
    }

    @Override
    public Iterator<ByteBuffer> iterator()
    {
        return new PathIterator();
    }

    private class PathIterator implements Iterator<ByteBuffer>, Closeable
    {
        private final ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);
        private SeekableByteChannel channel;
        private long position;

        @Override
        public boolean hasNext()
        {
            return position < getLength();
        }

        @Override
        public ByteBuffer next()
        {
            try
            {
                if (channel == null)
                {
                    channel = Files.newByteChannel(filePath, StandardOpenOption.READ);
                    if (LOG.isDebugEnabled())
                        LOG.debug("Opened file {}", filePath);
                }

                buffer.clear();
                int read = channel.read(buffer);
                if (read < 0)
                    throw new NoSuchElementException();

                if (LOG.isDebugEnabled())
                    LOG.debug("Read {} bytes from {}", read, filePath);

                position += read;

                if (!hasNext())
                    close();

                buffer.flip();
                return buffer;
            }
            catch (NoSuchElementException x)
            {
                close();
                throw x;
            }
            catch (Throwable x)
            {
                close();
                throw (NoSuchElementException)new NoSuchElementException().initCause(x);
            }
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close()
        {
            try
            {
                if (channel != null)
                    channel.close();
            }
            catch (Throwable x)
            {
                LOG.ignore(x);
            }
        }
    }
}
