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

package org.eclipse.jetty.start;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.io.IOException;

import org.eclipse.jetty.start.config.CommandLineConfigSource;
import org.eclipse.jetty.start.config.ConfigSources;
import org.eclipse.jetty.start.config.JettyBaseConfigSource;
import org.eclipse.jetty.start.config.JettyHomeConfigSource;
import org.eclipse.jetty.toolchain.test.MavenTestingUtils;
import org.eclipse.jetty.toolchain.test.TestingDir;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

public class ModuleTest
{
    @Rule
    public TestingDir testdir = new TestingDir();
    
    @Test
    public void testLoadWebSocket() throws IOException
    {
        // Test Env
        File homeDir = MavenTestingUtils.getTestResourceDir("dist-home");
        File baseDir = testdir.getEmptyDir();
        String cmdLine[] = new String[] {"jetty.version=TEST"};
        
        // Configuration
        CommandLineConfigSource cmdLineSource = new CommandLineConfigSource(cmdLine);
        ConfigSources config = new ConfigSources();
        config.add(cmdLineSource);
        config.add(new JettyHomeConfigSource(homeDir.toPath()));
        config.add(new JettyBaseConfigSource(baseDir.toPath()));
        
        // Initialize
        BaseHome basehome = new BaseHome(config);
        
        File file = MavenTestingUtils.getTestResourceFile("dist-home/modules/websocket.mod");
        Module module = new Module(basehome,file.toPath());
        
        Assert.assertThat("Module Name",module.getName(),is("websocket"));
        Assert.assertThat("Module Parents Size",module.getParentNames().size(),is(1));
        Assert.assertThat("Module Parents",module.getParentNames(),containsInAnyOrder("annotations"));
        Assert.assertThat("Module Xmls Size",module.getXmls().size(),is(0));
        Assert.assertThat("Module Options Size",module.getLibs().size(),is(1));
        Assert.assertThat("Module Options",module.getLibs(),contains("lib/websocket/*.jar"));
    }
}
