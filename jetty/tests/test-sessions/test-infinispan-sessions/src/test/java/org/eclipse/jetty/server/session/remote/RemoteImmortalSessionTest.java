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


package org.eclipse.jetty.server.session.remote;

import org.eclipse.jetty.server.session.AbstractImmortalSessionTest;
import org.eclipse.jetty.server.session.AbstractTestServer;
import org.eclipse.jetty.server.session.InfinispanTestSessionServer;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * ImmortalSessionTest
 *
 *
 */
public class RemoteImmortalSessionTest extends AbstractImmortalSessionTest
{


    public static RemoteInfinispanTestSupport __testSupport;
    
    
    @BeforeClass
    public static void setup () throws Exception
    {
        __testSupport = new RemoteInfinispanTestSupport("remote-session-test");
        __testSupport.setup();
    }
    
    @AfterClass
    public static void teardown () throws Exception
    {
       __testSupport.teardown();
    }
    
    
    /** 
     * @see org.eclipse.jetty.server.session.AbstractImmortalSessionTest#createServer(int, int, int)
     */
    @Override
    public AbstractTestServer createServer(int port, int maxInactiveMs, int scavengeMs)
    {
        return new InfinispanTestSessionServer(port, maxInactiveMs, scavengeMs, __testSupport.getCache());
    }

    @Override
    public void testImmortalSession() throws Exception
    {
        super.testImmortalSession();
    }

    
}
