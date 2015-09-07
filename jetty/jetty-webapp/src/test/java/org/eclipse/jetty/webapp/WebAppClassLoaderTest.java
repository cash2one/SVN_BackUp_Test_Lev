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

package org.eclipse.jetty.webapp;

import static org.eclipse.jetty.toolchain.test.ExtraMatchers.*; 
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jetty.toolchain.test.MavenTestingUtils;
import org.eclipse.jetty.util.resource.PathResource;
import org.eclipse.jetty.util.resource.Resource;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class WebAppClassLoaderTest
{
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private Path testWebappDir;
    private WebAppContext _context;
    private WebAppClassLoader _loader;

    @Before
    public void init() throws Exception
    {
        this.testWebappDir = MavenTestingUtils.getProjectDirPath("src/test/webapp");
        Resource webapp = new PathResource(testWebappDir);
        
        System.err.printf("testWebappDir = %s%n", testWebappDir);

        _context = new WebAppContext();
        _context.setBaseResource(webapp);
        _context.setContextPath("/test");

        _loader = new WebAppClassLoader(_context);
        _loader.addJars(webapp.addPath("WEB-INF/lib"));
        _loader.addClassPath(webapp.addPath("WEB-INF/classes"));
        _loader.setName("test");
    }
    
    public void assertCanLoadClass(String clazz) throws ClassNotFoundException
    {
        assertThat("Can Load Class ["+clazz+"]", _loader.loadClass(clazz), notNullValue());
    }
    
    public void assertCanLoadResource(String res) throws ClassNotFoundException
    {
        assertThat("Can Load Resource ["+res+"]", _loader.getResource(res), notNullValue());
    }
    
    public void assertCantLoadClass(String clazz)
    {
        try
        {
            assertThat("Can't Load Class ["+clazz+"]", _loader.loadClass(clazz), nullValue());
        }
        catch (ClassNotFoundException e)
        {
            // Valid path
        }
    }

    @Test
    public void testParentLoad() throws Exception
    {
        _context.setParentLoaderPriority(true);
        
        assertCanLoadClass("org.acme.webapp.ClassInJarA");
        assertCanLoadClass("org.acme.webapp.ClassInJarB");
        assertCanLoadClass("org.acme.other.ClassInClassesC");

        assertCantLoadClass("org.eclipse.jetty.webapp.Configuration");

        Class<?> clazzA = _loader.loadClass("org.acme.webapp.ClassInJarA");
        assertTrue(clazzA.getField("FROM_PARENT")!=null);
    }

    @Test
    public void testWebAppLoad() throws Exception
    {
        _context.setParentLoaderPriority(false);
        assertCanLoadClass("org.acme.webapp.ClassInJarA");
        assertCanLoadClass("org.acme.webapp.ClassInJarB");
        assertCanLoadClass("org.acme.other.ClassInClassesC");

        assertCantLoadClass("org.eclipse.jetty.webapp.Configuration");

        Class<?> clazzA = _loader.loadClass("org.acme.webapp.ClassInJarA");
        expectedException.expect(NoSuchFieldException.class);
        clazzA.getField("FROM_PARENT");
    }
    
    @Test
    public void testClassFileTranslations() throws Exception
    {
        final List<Object> results=new ArrayList<Object>();
        
        _loader.addTransformer(new ClassFileTransformer()
        {
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer)
                    throws IllegalClassFormatException
            {
                results.add(loader);
                byte[] b = new byte[classfileBuffer.length];
                for (int i=0;i<classfileBuffer.length;i++)
                    b[i]=(byte)(classfileBuffer[i]^0xff);
                return b;
            }
        });
        _loader.addTransformer(new ClassFileTransformer()
        {
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer)
                    throws IllegalClassFormatException
            {
                results.add(className);
                byte[] b = new byte[classfileBuffer.length];
                for (int i=0;i<classfileBuffer.length;i++)
                    b[i]=(byte)(classfileBuffer[i]^0xff);
                return b;
            }
        });
        
        _context.setParentLoaderPriority(false);
        
        assertCanLoadClass("org.acme.webapp.ClassInJarA");
        assertCanLoadClass("org.acme.webapp.ClassInJarB");
        assertCanLoadClass("org.acme.other.ClassInClassesC");
        assertCanLoadClass("java.lang.String");
        assertCantLoadClass("org.eclipse.jetty.webapp.Configuration");
        
        assertThat("Classname Results", results, contains(
                _loader,
                "org.acme.webapp.ClassInJarA",
                _loader,
                "org.acme.webapp.ClassInJarB",
                _loader,
                "org.acme.other.ClassInClassesC"));
    }
    
    @Test
    public void testNullClassFileTransformer () throws Exception
    {
        _loader.addTransformer(new ClassFileTransformer()
        {
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer)
                    throws IllegalClassFormatException
            {
                return null;
            }
        });
        
        assertCanLoadClass("org.acme.webapp.ClassInJarA");
    }

    @Test
    public void testExposedClass() throws Exception
    {
        String[] oldSC=_context.getServerClasses();
        String[] newSC=new String[oldSC.length+1];
        newSC[0]="-org.eclipse.jetty.webapp.Configuration";
        System.arraycopy(oldSC,0,newSC,1,oldSC.length);
        _context.setServerClasses(newSC);

        assertCanLoadClass("org.acme.webapp.ClassInJarA");
        assertCanLoadClass("org.acme.webapp.ClassInJarB");
        assertCanLoadClass("org.acme.other.ClassInClassesC");

        assertCanLoadClass("org.eclipse.jetty.webapp.Configuration");
        assertCantLoadClass("org.eclipse.jetty.webapp.JarScanner");
    }

    @Test
    public void testSystemServerClass() throws Exception
    {
        String[] oldServC=_context.getServerClasses();
        String[] newServC=new String[oldServC.length+1];
        newServC[0]="org.eclipse.jetty.webapp.Configuration";
        System.arraycopy(oldServC,0,newServC,1,oldServC.length);
        _context.setServerClasses(newServC);

        String[] oldSysC=_context.getSystemClasses();
        String[] newSysC=new String[oldSysC.length+1];
        newSysC[0]="org.eclipse.jetty.webapp.";
        System.arraycopy(oldSysC,0,newSysC,1,oldSysC.length);
        _context.setSystemClasses(newSysC);

        assertCanLoadClass("org.acme.webapp.ClassInJarA");
        assertCanLoadClass("org.acme.webapp.ClassInJarB");
        assertCanLoadClass("org.acme.other.ClassInClassesC");
        assertCantLoadClass("org.eclipse.jetty.webapp.Configuration");
        assertCantLoadClass("org.eclipse.jetty.webapp.JarScanner");

        oldSysC=_context.getSystemClasses();
        newSysC=new String[oldSysC.length+1];
        newSysC[0]="org.acme.webapp.ClassInJarA";
        System.arraycopy(oldSysC,0,newSysC,1,oldSysC.length);
        _context.setSystemClasses(newSysC);

        assertCanLoadResource("org/acme/webapp/ClassInJarA.class");
        _context.setSystemClasses(oldSysC);

        oldServC=_context.getServerClasses();
        newServC=new String[oldServC.length+1];
        newServC[0]="org.acme.webapp.ClassInJarA";
        System.arraycopy(oldServC,0,newServC,1,oldServC.length);
        _context.setServerClasses(newServC);
        assertCanLoadResource("org/acme/webapp/ClassInJarA.class");
    }

    @Test
    public void testResources() throws Exception
    {
        List<URL> expected = new ArrayList<>();
        List<URL> resources;
        
        // Expected Locations
        URL webappWebInfLibAcme = new URI("jar:" + testWebappDir.resolve("WEB-INF/lib/acme.jar").toUri().toASCIIString() + "!/org/acme/resource.txt").toURL();
        URL webappWebInfClasses = testWebappDir.resolve("WEB-INF/classes/org/acme/resource.txt").toUri().toURL();
        // (from parent classloader)
        URL targetTestClasses = this.getClass().getClassLoader().getResource("org/acme/resource.txt");

        _context.setParentLoaderPriority(false);
        dump(_context);
        resources =Collections.list(_loader.getResources("org/acme/resource.txt"));
        
        expected.clear();
        expected.add(webappWebInfLibAcme);
        expected.add(webappWebInfClasses);
        expected.add(targetTestClasses);
        
        assertThat("Resources Found (Parent Loader Priority == false)",resources,ordered(expected));
        
//        dump(resources);
//        assertEquals(3,resources.size());
//        assertEquals(0,resources.get(0).toString().indexOf("jar:file:"));
//        assertEquals(-1,resources.get(1).toString().indexOf("test-classes"));
//        assertEquals(0,resources.get(2).toString().indexOf("file:"));

        _context.setParentLoaderPriority(true);
        // dump(_context);
        resources =Collections.list(_loader.getResources("org/acme/resource.txt"));
        
        expected.clear();
        expected.add(targetTestClasses);
        expected.add(webappWebInfLibAcme);
        expected.add(webappWebInfClasses);
        
        assertThat("Resources Found (Parent Loader Priority == true)",resources,ordered(expected));
        
//        dump(resources);
//        assertEquals(3,resources.size());
//        assertEquals(0,resources.get(0).toString().indexOf("file:"));
//        assertEquals(0,resources.get(1).toString().indexOf("jar:file:"));
//        assertEquals(-1,resources.get(2).toString().indexOf("test-classes"));

        String[] oldServC=_context.getServerClasses();
        String[] newServC=new String[oldServC.length+1];
        newServC[0]="org.acme.";
        System.arraycopy(oldServC,0,newServC,1,oldServC.length);
        _context.setServerClasses(newServC);

        _context.setParentLoaderPriority(true);
        // dump(_context);
        resources =Collections.list(_loader.getResources("org/acme/resource.txt"));
        
        expected.clear();
        expected.add(webappWebInfLibAcme);
        expected.add(webappWebInfClasses);
        
        assertThat("Resources Found (Parent Loader Priority == true) (with serverClasses filtering)",resources,ordered(expected));
        
//        dump(resources);
//        assertEquals(2,resources.size());
//        assertEquals(0,resources.get(0).toString().indexOf("jar:file:"));
//        assertEquals(0,resources.get(1).toString().indexOf("file:"));

        _context.setServerClasses(oldServC);
        String[] oldSysC=_context.getSystemClasses();
        String[] newSysC=new String[oldSysC.length+1];
        newSysC[0]="org.acme.";
        System.arraycopy(oldSysC,0,newSysC,1,oldSysC.length);
        _context.setSystemClasses(newSysC);

        _context.setParentLoaderPriority(true);
        // dump(_context);
        resources =Collections.list(_loader.getResources("org/acme/resource.txt"));
        
        expected.clear();
        expected.add(targetTestClasses);
        
        assertThat("Resources Found (Parent Loader Priority == true) (with systemClasses filtering)",resources,ordered(expected));
        
//        dump(resources);
//        assertEquals(1,resources.size());
//        assertEquals(0,resources.get(0).toString().indexOf("file:"));
    }

    private void dump(WebAppContext wac)
    {
        System.err.println("--Dump WebAppContext - " + wac);
        System.err.printf("  context.getClass().getClassLoader() = %s%n",wac.getClass().getClassLoader());
        dumpClassLoaderHierarchy("  ",wac.getClass().getClassLoader());
        System.err.printf("  context.getClassLoader() = %s%n",wac.getClassLoader());
        dumpClassLoaderHierarchy("  ",wac.getClassLoader());
    }

    private void dumpClassLoaderHierarchy(String indent, ClassLoader classLoader)
    {
        if (classLoader != null)
        {
            if(classLoader instanceof URLClassLoader)
            {
                URLClassLoader urlCL = (URLClassLoader)classLoader;
                URL urls[] = urlCL.getURLs();
                for (URL url : urls)
                {
                    System.err.printf("%s url[] = %s%n",indent,url);
                }
            }
            
            ClassLoader parent = classLoader.getParent();
            if (parent != null)
            {
                System.err.printf("%s .parent = %s%n",indent,parent);
                dumpClassLoaderHierarchy(indent + "  ",parent);
            }
        }
    }

    private void dump(List<URL> resources)
    {
        System.err.println("--Dump--");
        for(URL url: resources)
        {
            System.err.printf(" \"%s\"%n",url);
        }
    }
}
