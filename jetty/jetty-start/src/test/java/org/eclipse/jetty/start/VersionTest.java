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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import org.junit.Test;

public class VersionTest
{
    @Test
    public void testParse() 
    {
        assertParse("1.8.0_45",1,8,0,45);
        assertParse("1.8.0_45-internal",1,8,0,45);
        assertParse("1.8.0-debug",1,8,0,-1);
    }
    
    private void assertParse(String verStr, int legacyMajor, int major, int revision, int update)
    {
        Version ver = new Version(verStr);
        assertThat("Version [" + verStr + "].legacyMajor", ver.getLegacyMajor(), is(legacyMajor));
        assertThat("Version [" + verStr + "].major", ver.getMajor(), is(major));
        assertThat("Version [" + verStr + "].revision", ver.getRevision(), is(revision));
        assertThat("Version [" + verStr + "].update", ver.getUpdate(), is(update));
        
        assertThat("Version [" + verStr + "].toString", ver.toString(), is(verStr));
    }

    @Test
    public void testToShortString() 
    {
        assertToShortString("1.8","1.8");
        assertToShortString("1.8.0","1.8.0");
        assertToShortString("1.8.0_45","1.8.0_45");
        assertToShortString("1.8.0_45-internal","1.8.0_45");
        assertToShortString("1.8.0-debug","1.8.0");
    }
    
    private void assertToShortString(String verStr, String expectedShortString)
    {
        Version ver = new Version(verStr);
        assertThat("Version [" + verStr + "].toShortString", ver.toShortString(), is(expectedShortString));
    }

    @Test
    public void testNewerVersion() {
        assertIsNewer("0.0.0", "0.0.1");
        assertIsNewer("0.1.0", "0.1.1");
        assertIsNewer("1.5.0", "1.6.0");
        // assertIsNewer("1.6.0_12", "1.6.0_16"); // JDK version spec?
    }
    
    @Test
    public void testOlderVersion() {
        assertIsOlder("0.0.1", "0.0.0");
        assertIsOlder("0.1.1", "0.1.0");
        assertIsOlder("1.6.0", "1.5.0");
    }
    
    @Test
    public void testOlderOrEqualTo()
    {
        assertThat("9.2 <= 9.2",new Version("9.2").isOlderThanOrEqualTo(new Version("9.2")),is(true));
        assertThat("9.2 <= 9.3",new Version("9.2").isOlderThanOrEqualTo(new Version("9.3")),is(true));
        assertThat("9.3 <= 9.2",new Version("9.3").isOlderThanOrEqualTo(new Version("9.2")),is(false));
    }
    
    @Test
    public void testNewerOrEqualTo()
    {
        assertThat("9.2 >= 9.2",new Version("9.2").isNewerThanOrEqualTo(new Version("9.2")),is(true));
        assertThat("9.2 >= 9.3",new Version("9.2").isNewerThanOrEqualTo(new Version("9.3")),is(false));
        assertThat("9.3 >= 9.2",new Version("9.3").isNewerThanOrEqualTo(new Version("9.2")),is(true));
    }

    private void assertIsOlder(String basever, String testver)
    {
        Version vbase = new Version(basever);
        Version vtest = new Version(testver);
        assertTrue("Version [" + testver + "] should be older than [" + basever + "]", vtest.isOlderThan(vbase));
    }

    private void assertIsNewer(String basever, String testver)
    {
        Version vbase = new Version(basever);
        Version vtest = new Version(testver);
        assertTrue("Version [" + testver + "] should be newer than [" + basever + "]", vtest.isNewerThan(vbase));
    }
}
