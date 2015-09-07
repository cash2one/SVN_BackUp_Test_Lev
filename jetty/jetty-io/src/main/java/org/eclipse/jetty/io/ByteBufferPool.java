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

package org.eclipse.jetty.io;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jetty.util.BufferUtil;

/**
 * <p>A {@link ByteBuffer} pool.</p>
 * <p>Acquired buffers may be {@link #release(ByteBuffer) released} but they do not need to;
 * if they are released, they may be recycled and reused, otherwise they will be garbage
 * collected as usual.</p>
 */
public interface ByteBufferPool
{
    /**
     * <p>Requests a {@link ByteBuffer} of the given size.</p>
     * <p>The returned buffer may have a bigger capacity than the size being
     * requested but it will have the limit set to the given size.</p>
     *
     * @param size   the size of the buffer
     * @param direct whether the buffer must be direct or not
     * @return the requested buffer
     * @see #release(ByteBuffer)
     */
    public ByteBuffer acquire(int size, boolean direct);

    /**
     * <p>Returns a {@link ByteBuffer}, usually obtained with {@link #acquire(int, boolean)}
     * (but not necessarily), making it available for recycling and reuse.</p>
     *
     * @param buffer the buffer to return
     * @see #acquire(int, boolean)
     */
    public void release(ByteBuffer buffer);

    public static class Lease
    {
        private final ByteBufferPool byteBufferPool;
        private final List<ByteBuffer> buffers;
        private final List<Boolean> recycles;

        public Lease(ByteBufferPool byteBufferPool)
        {
            this.byteBufferPool = byteBufferPool;
            this.buffers = new ArrayList<>();
            this.recycles = new ArrayList<>();
        }

        public ByteBuffer acquire(int capacity, boolean direct)
        {
            ByteBuffer buffer = byteBufferPool.acquire(capacity, direct);
            BufferUtil.clearToFill(buffer);
            return buffer;
        }

        public void append(ByteBuffer buffer, boolean recycle)
        {
            buffers.add(buffer);
            recycles.add(recycle);
        }

        public void insert(int index, ByteBuffer buffer, boolean recycle)
        {
            buffers.add(index, buffer);
            recycles.add(index, recycle);
        }

        public List<ByteBuffer> getByteBuffers()
        {
            return buffers;
        }

        public long getTotalLength()
        {
            long length = 0;
            for (int i = 0; i < buffers.size(); ++i)
                length += buffers.get(i).remaining();
            return length;
        }

        public int getSize()
        {
            return buffers.size();
        }

        public void recycle()
        {
            for (int i = 0; i < buffers.size(); ++i)
            {
                ByteBuffer buffer = buffers.get(i);
                if (recycles.get(i))
                    byteBufferPool.release(buffer);
            }
            buffers.clear();
            recycles.clear();
        }
    }
}
