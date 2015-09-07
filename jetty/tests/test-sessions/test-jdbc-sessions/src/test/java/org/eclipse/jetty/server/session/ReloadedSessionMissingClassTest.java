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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.net.URLClassLoader;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.toolchain.test.MavenTestingUtils;
import org.eclipse.jetty.toolchain.test.TestingDir;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.StdErrLog;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.Rule;
import org.junit.Test;

/**
 * ReloadedSessionMissingClassTest
 */
public class ReloadedSessionMissingClassTest
{
    @Rule
    public TestingDir testdir = new TestingDir();
    
    @Test
    public void testSessionReloadWithMissingClass() throws Exception
    {
        ((StdErrLog)Log.getLogger(org.eclipse.jetty.server.session.JDBCSessionManager.class)).setHideStacks(true);
        Resource.setDefaultUseCaches(false);
        String contextPath = "/foo";

        File unpackedWarDir = testdir.getDir();
        testdir.ensureEmpty();

        File webInfDir = new File (unpackedWarDir, "WEB-INF");
        webInfDir.mkdir();

        File webXml = new File(webInfDir, "web.xml");
        String xml =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<web-app xmlns=\"http://java.sun.com/xml/ns/j2ee\"\n" +
            "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "         xsi:schemaLocation=\"http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/web-app_2_4.xsd\"\n" +
            "         version=\"2.4\">\n" +
            "\n" +
            "<session-config>\n"+
            " <session-timeout>1</session-timeout>\n" +
            "</session-config>\n"+
            "</web-app>";
        FileWriter w = new FileWriter(webXml);
        w.write(xml);
        w.close();

        File foobarJar = MavenTestingUtils.getTestResourceFile("foobar.jar");
        File foobarNOfooJar = MavenTestingUtils.getTestResourceFile("foobarNOfoo.jar");
        
        URL[] foobarUrls = new URL[]{foobarJar.toURI().toURL()};
        URL[] barUrls = new URL[]{foobarNOfooJar.toURI().toURL()};
        
        URLClassLoader loaderWithFoo = new URLClassLoader(foobarUrls, Thread.currentThread().getContextClassLoader());
        URLClassLoader loaderWithoutFoo = new URLClassLoader(barUrls, Thread.currentThread().getContextClassLoader());

       
        AbstractTestServer server1 = new JdbcTestServer(0);
        WebAppContext webApp = server1.addWebAppContext(unpackedWarDir.getCanonicalPath(), contextPath);
        webApp.setClassLoader(loaderWithFoo);
        webApp.addServlet("Bar", "/bar");
        server1.start();
        int port1 = server1.getPort();
        try
        {
            HttpClient client = new HttpClient();
            client.start();
            try
            {
                // Perform one request to server1 to create a session
                ContentResponse response = client.GET("http://localhost:" + port1 + contextPath +"/bar?action=set");
                
                assertEquals( HttpServletResponse.SC_OK, response.getStatus());
                String sessionCookie = response.getHeaders().get("Set-Cookie");
                assertTrue(sessionCookie != null);
                String sessionId = (String)webApp.getServletContext().getAttribute("foo");
                assertNotNull(sessionId);
                // Mangle the cookie, replacing Path with $Path, etc.
                sessionCookie = sessionCookie.replaceFirst("(\\W)(P|p)ath=", "$1\\$Path=");
                
                //Stop the webapp
                webApp.stop();
                
                webApp.setClassLoader(loaderWithoutFoo);
                
                //restart webapp
                webApp.start();
                
                Request request = client.newRequest("http://localhost:" + port1 + contextPath + "/bar?action=get");
                request.header("Cookie", sessionCookie);
                response = request.send();
                assertEquals(HttpServletResponse.SC_OK,response.getStatus());
                String afterStopSessionId = (String)webApp.getServletContext().getAttribute("foo.session");
                
                assertNotNull(afterStopSessionId);
                assertTrue(!afterStopSessionId.equals(sessionId));  

            }
            finally
            {
                client.stop();
            }
        }
        finally
        {
            server1.stop();
        }
    }
}
