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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.eclipse.jetty.start.Props.Prop;
import org.junit.Test;

public class PropsTest
{
    private static final String FROM_TEST = "(test)";

    private void assertProp(String prefix, Prop prop, String expectedKey, String expectedValue, String expectedOrigin)
    {
        assertThat(prefix,prop,notNullValue());
        assertThat(prefix + ".key",prop.key,is(expectedKey));
        assertThat(prefix + ".value",prop.value,is(expectedValue));
        assertThat(prefix + ".origin",prop.origin,is(expectedOrigin));
    }

    @Test
    public void testSystemPropsOnly()
    {
        Props props = new Props();

        String expected = System.getProperty("java.io.tmpdir");
        assertThat("System Property",props.getString("java.io.tmpdir"),is(expected));

        Prop prop = props.getProp("java.io.tmpdir");
        assertProp("System Prop",prop,"java.io.tmpdir",expected,Props.ORIGIN_SYSPROP);
        assertThat("System Prop.overrides",prop.overrides,nullValue());
    }

    @Test
    public void testBasic()
    {
        Props props = new Props();
        props.setProperty("name","jetty",FROM_TEST);

        String prefix = "Basic";
        assertThat(prefix,props.getString("name"),is("jetty"));

        Prop prop = props.getProp("name");
        assertProp(prefix,prop,"name","jetty",FROM_TEST);
        assertThat(prefix + ".overrides",prop.overrides,nullValue());
    }

    @Test
    public void testOverride()
    {
        Props props = new Props();
        props.setProperty("name","jetty",FROM_TEST);
        props.setProperty("name","altjetty","(Alt-Jetty)");

        String prefix = "Overriden";
        assertThat(prefix,props.getString("name"),is("altjetty"));

        Prop prop = props.getProp("name");
        assertProp(prefix,prop,"name","altjetty","(Alt-Jetty)");
        Prop older = prop.overrides;
        assertThat(prefix + ".overrides",older,notNullValue());
        assertProp(prefix + ".overridden",older,"name","jetty",FROM_TEST);
        assertThat(prefix + ".overridden",older.overrides,nullValue());
    }

    @Test
    public void testSimpleExpand()
    {
        Props props = new Props();
        props.setProperty("name","jetty",FROM_TEST);
        props.setProperty("version","9.1",FROM_TEST);

        assertThat(props.expand("port=8080"),is("port=8080"));
        assertThat(props.expand("jdk=${java.version}"),is("jdk=" + System.getProperty("java.version")));
        assertThat(props.expand("id=${name}-${version}"),is("id=jetty-9.1"));
        assertThat(props.expand("id=${unknown}-${wibble}"),is("id=${unknown}-${wibble}"));
    }

    @Test
    public void testNoExpandDoubleDollar()
    {
        Props props = new Props();
        props.setProperty("aa","123",FROM_TEST);

        // Should NOT expand double $$ symbols
        assertThat(props.expand("zz=$${aa}"),is("zz=${aa}"));
        // Should expand
        assertThat(props.expand("zz=${aa}"),is("zz=123"));
    }

    @Test
    public void testExpandDeep()
    {
        Props props = new Props();
        props.setProperty("name","jetty",FROM_TEST);
        props.setProperty("version","9.1",FROM_TEST);
        props.setProperty("id","${name}-${version}",FROM_TEST);

        // Should expand
        assertThat(props.expand("server-id=corporate-${id}"),is("server-id=corporate-jetty-9.1"));
    }

    @Test
    public void testExpandLoop()
    {
        Props props = new Props();
        props.setProperty("aa","${bb}",FROM_TEST);
        props.setProperty("bb","${cc}",FROM_TEST);
        props.setProperty("cc","${aa}",FROM_TEST);

        try
        {
            // Should throw exception
            props.expand("val=${aa}");
            fail("Should have thrown a " + PropsException.class);
        }
        catch (PropsException e)
        {
            assertThat(e.getMessage(),is("Property expansion loop detected: aa -> bb -> cc -> aa"));
        }
    }
}
