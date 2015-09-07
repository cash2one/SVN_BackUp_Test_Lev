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

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.util.Callback;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DeferredContentProviderTest
{
    private ExecutorService executor;

    @Before
    public void prepare() throws Exception
    {
        executor = Executors.newCachedThreadPool();
    }

    @After
    public void dispose() throws Exception
    {
        executor.shutdownNow();
    }

    @Test
    public void testWhenEmptyFlushDoesNotBlock() throws Exception
    {
        final DeferredContentProvider provider = new DeferredContentProvider();

        Future<?> task = executor.submit(new Callable<Object>()
        {
            @Override
            public Object call() throws Exception
            {
                provider.flush();
                return null;
            }
        });

        Assert.assertTrue(await(task, 5, TimeUnit.SECONDS));
    }

    @Test
    public void testOfferFlushBlocksUntilSucceeded() throws Exception
    {
        final DeferredContentProvider provider = new DeferredContentProvider();
        Iterator<ByteBuffer> iterator = provider.iterator();

        provider.offer(ByteBuffer.allocate(0));

        Future<?> task = executor.submit(new Callable<Object>()
        {
            @Override
            public Object call() throws Exception
            {
                provider.flush();
                return null;
            }
        });

        // Wait until flush() blocks.
        Assert.assertFalse(await(task, 1, TimeUnit.SECONDS));

        // Consume the content and succeed the callback.
        iterator.next();
        ((Callback)iterator).succeeded();

        // Flush should return.
        Assert.assertTrue(await(task, 5, TimeUnit.SECONDS));
    }

    @Test
    public void testCloseFlushDoesNotBlock() throws Exception
    {
        final DeferredContentProvider provider = new DeferredContentProvider();

        provider.close();

        Future<?> task = executor.submit(new Callable<Object>()
        {
            @Override
            public Object call() throws Exception
            {
                provider.flush();
                return null;
            }
        });

        // Wait until flush() blocks.
        Assert.assertTrue(await(task, 5, TimeUnit.SECONDS));
    }

    @Test
    public void testCloseNextHasNextReturnsFalse() throws Exception
    {
        DeferredContentProvider provider = new DeferredContentProvider();
        Iterator<ByteBuffer> iterator = provider.iterator();

        provider.close();

        Assert.assertFalse(iterator.hasNext());

        try
        {
            iterator.next();
            Assert.fail();
        }
        catch (NoSuchElementException x)
        {
            // Expected
        }

        Assert.assertFalse(iterator.hasNext());
    }

    private boolean await(Future<?> task, long time, TimeUnit unit) throws Exception
    {
        try
        {
            task.get(time, unit);
            return true;
        }
        catch (TimeoutException x)
        {
            return false;
        }
    }
}
