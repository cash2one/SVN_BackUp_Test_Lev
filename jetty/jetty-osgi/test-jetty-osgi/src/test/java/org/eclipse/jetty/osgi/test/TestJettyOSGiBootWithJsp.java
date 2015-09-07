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

package org.eclipse.jetty.osgi.test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.osgi.boot.OSGiServerConstants;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.BundleContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

/**
 * Pax-Exam to make sure the jetty-osgi-boot can be started along with the
 * httpservice web-bundle. Then make sure we can deploy an OSGi service on the
 * top of this.
 */
@RunWith(PaxExam.class)
public class TestJettyOSGiBootWithJsp
{
    private static final String LOG_LEVEL = "WARN";

    @Inject
    BundleContext bundleContext = null;

    @Configuration
    public static Option[] configure()
    {
        ArrayList<Option> options = new ArrayList<Option>();
        options.add(CoreOptions.junitBundles());
        options.addAll(configureJettyHomeAndPort(false,"jetty-http.xml"));
        options.add(CoreOptions.bootDelegationPackages("org.xml.sax", "org.xml.*", "org.w3c.*", "javax.xml.*", "javax.activation.*"));
        options.add(CoreOptions.systemPackages("com.sun.org.apache.xalan.internal.res","com.sun.org.apache.xml.internal.utils",
                                               "com.sun.org.apache.xml.internal.utils", "com.sun.org.apache.xpath.internal",
                                               "com.sun.org.apache.xpath.internal.jaxp", "com.sun.org.apache.xpath.internal.objects"));
     
        options.addAll(TestJettyOSGiBootCore.coreJettyDependencies());
        options.addAll(Arrays.asList(options(systemProperty("pax.exam.logging").value("none"))));
        options.addAll(Arrays.asList(options(systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value(LOG_LEVEL))));
        options.addAll(Arrays.asList(options(systemProperty("org.eclipse.jetty.LEVEL").value(LOG_LEVEL))));
        options.addAll(Arrays.asList(options(systemProperty("org.eclipse.jetty.osgi.LEVEL").value(LOG_LEVEL))));
        options.addAll(jspDependencies());
        return options.toArray(new Option[options.size()]);
    }

    public static List<Option> configureJettyHomeAndPort(boolean ssl,String jettySelectorFileName)
    {
        File etcFolder = new File("src/test/config/etc");
        String etc = "file://" + etcFolder.getAbsolutePath();
        List<Option> options = new ArrayList<Option>();
        String xmlConfigs = etc     + "/jetty.xml";
        if (ssl)
            xmlConfigs += ";" 
                    + etc
                    + "/jetty-ssl.xml;"
                    + etc
                    + "/jetty-https.xml;";
        xmlConfigs+= ";"
                + etc
                + "/"
                + jettySelectorFileName
                + ";"
                + etc
                + "/jetty-deployer.xml;"
                + etc
                + "/jetty-testrealm.xml";

        options.add(systemProperty(OSGiServerConstants.MANAGED_JETTY_XML_CONFIG_URLS).value(xmlConfigs));
        options.add(systemProperty("jetty.http.port").value(String.valueOf(TestJettyOSGiBootCore.DEFAULT_HTTP_PORT)));
        options.add(systemProperty("jetty.ssl.port").value(String.valueOf(TestJettyOSGiBootCore.DEFAULT_SSL_PORT)));
        options.add(systemProperty("jetty.home").value(etcFolder.getParentFile().getAbsolutePath()));
        options.add(systemProperty("jetty.base").value(etcFolder.getParentFile().getAbsolutePath()));
        return options;
    }

    public static List<Option> jspDependencies()
    {
        List<Option> res = new ArrayList<Option>();
        res.addAll(TestJettyOSGiBootCore.jspDependencies());
        //test webapp bundle
        res.add(mavenBundle().groupId("org.eclipse.jetty").artifactId("test-jetty-webapp").classifier("webbundle").versionAsInProject());
        
        return res;
    }


    @Ignore
    @Test
    public void assertAllBundlesActiveOrResolved()
    {
        TestOSGiUtil.debugBundles(bundleContext);
        TestOSGiUtil.assertAllBundlesActiveOrResolved(bundleContext);
    }

    // at the moment can't run httpservice with jsp at the same time.
    // that is a regression in jetty-9
    @Ignore
    @Test
    public void testHttpService() throws Exception
    {
        TestOSGiUtil.testHttpServiceGreetings(bundleContext, "http", TestJettyOSGiBootCore.DEFAULT_HTTP_PORT);
    }

 
    @Test
    public void testJspDump() throws Exception
    {
        // TestOSGiUtil.debugBundles(bundleContext);
        HttpClient client = new HttpClient();
        try
        {
            client.start();
            ContentResponse response = client.GET("http://127.0.0.1:" + TestJettyOSGiBootCore.DEFAULT_HTTP_PORT + "/jsp/jstl.jsp");
            
            assertEquals(HttpStatus.OK_200, response.getStatus());
            String content = new String(response.getContent());
            assertTrue(content.contains("JSTL Example"));           
        }
        finally
        {
            client.stop();
        }
    }
}
