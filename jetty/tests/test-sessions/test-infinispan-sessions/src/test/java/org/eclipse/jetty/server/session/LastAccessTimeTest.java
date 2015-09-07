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

package org.eclipse.jetty.server.session;

import java.io.File;

import org.eclipse.jetty.util.IO;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public class LastAccessTimeTest extends AbstractLastAccessTimeTest
{ 
   public static InfinispanTestSupport __testSupport;
   
    
    @BeforeClass
    public static void setup () throws Exception
    {
       __testSupport = new InfinispanTestSupport();
       __testSupport.setUseFileStore(true);
       __testSupport.setup();
    }
    
    @AfterClass
    public static void teardown () throws Exception
    {
       __testSupport.teardown();
    }
    

    @Override
    public AbstractTestServer createServer(int port, int max, int scavenge)
    {
        return new InfinispanTestSessionServer(port, max, scavenge, __testSupport.getCache());
    }

    @Override
    public void testLastAccessTime() throws Exception
    {
        super.testLastAccessTime();
    }

    
}
