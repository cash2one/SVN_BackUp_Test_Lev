/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.systest.jaxrs.security.jwt;

import java.net.URL;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.rs.security.jose.jaxrs.JweClientResponseFilter;
import org.apache.cxf.rs.security.jose.jaxrs.JweWriterInterceptor;
import org.apache.cxf.rs.security.jose.jaxrs.JwsJsonClientResponseFilter;
import org.apache.cxf.rs.security.jose.jaxrs.JwsJsonWriterInterceptor;
import org.apache.cxf.rs.security.jose.jwa.AlgorithmUtils;
import org.apache.cxf.systest.jaxrs.security.Book;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class JAXRSJwsJsonTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookServerJwsJson.PORT;
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", 
                   launchServer(BookServerJwsJson.class, true));
        registerBouncyCastleIfNeeded();
    }
    
    private static void registerBouncyCastleIfNeeded() throws Exception {
        try {
            // Java 8 apparently has it
            Cipher.getInstance(AlgorithmUtils.AES_GCM_ALGO_JAVA);
        } catch (Throwable t) {
            // Oracle Java 7
            Security.addProvider(new BouncyCastleProvider());    
        }
    }
    @AfterClass
    public static void unregisterBouncyCastleIfNeeded() throws Exception {
        Security.removeProvider(BouncyCastleProvider.class.getName());    
    }
    
    @Test
    public void testJwsJsonPlainTextHmac() throws Exception {
        String address = "https://localhost:" + PORT + "/jwsjsonhmac";
        BookStore bs = createBookStore(address, 
                                       "org/apache/cxf/systest/jaxrs/security/secret.jwk.properties",
                                       null);
        String text = bs.echoText("book");
        assertEquals("book", text);
    }
    @Test
    public void testJwsJsonBookBeanHmac() throws Exception {
        String address = "https://localhost:" + PORT + "/jwsjsonhmac";
        BookStore bs = createBookStore(address, 
                                       "org/apache/cxf/systest/jaxrs/security/secret.jwk.properties",
                                       Collections.singletonList(new JacksonJsonProvider()));
        Book book = bs.echoBook(new Book("book", 123L));
        assertEquals("book", book.getName());
        assertEquals(123L, book.getId());
    }
    @Test
    public void testJweCompactJwsJsonBookBeanHmac() throws Exception {
        String address = "https://localhost:" + PORT + "/jwejwsjsonhmac";
        List<?> extraProviders = Arrays.asList(new JacksonJsonProvider(),
                                               new JweWriterInterceptor(),
                                               new JweClientResponseFilter());
        String jwkStoreProperty = "org/apache/cxf/systest/jaxrs/security/secret.jwk.properties";
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("rs.security.signature.list.properties", jwkStoreProperty);
        props.put("rs.security.encryption.properties", jwkStoreProperty);
        BookStore bs = createBookStore(address, 
                                       props,
                                       extraProviders);
        Book book = bs.echoBook(new Book("book", 123L));
        assertEquals("book", book.getName());
        assertEquals(123L, book.getId());
    }
    
    @Test
    public void testJwsJsonBookDoubleHmac() throws Exception {
        String address = "https://localhost:" + PORT + "/jwsjsonhmac2";
        List<String> properties = new ArrayList<String>();
        properties.add("org/apache/cxf/systest/jaxrs/security/secret.jwk.properties");
        properties.add("org/apache/cxf/systest/jaxrs/security/secret.jwk.hmac.properties");
        BookStore bs = createBookStore(address, properties, null);
        Book book = bs.echoBook(new Book("book", 123L));
        assertEquals("book", book.getName());
        assertEquals(123L, book.getId());
    }
    
    @Test
    public void testJwsJsonBookDoubleHmacSinglePropsFile() throws Exception {
        String address = "https://localhost:" + PORT + "/jwsjsonhmac2";
        List<String> properties = new ArrayList<String>();
        properties.add("org/apache/cxf/systest/jaxrs/security/secret.jwk.hmac2.properties");
        BookStore bs = createBookStore(address, properties, null);
        Book book = bs.echoBook(new Book("book", 123L));
        assertEquals("book", book.getName());
        assertEquals(123L, book.getId());
    }
    private BookStore createBookStore(String address, Object properties,
                                      List<?> extraProviders) throws Exception {
        return createBookStore(address, 
                               Collections.singletonMap("rs.security.signature.list.properties", properties),
                               extraProviders);
    }
    private BookStore createBookStore(String address, 
                                      Map<String, Object> mapProperties,
                                      List<?> extraProviders) throws Exception {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = JAXRSJwsJsonTest.class.getResource("client.xml");
        Bus springBus = bf.createBus(busFile.toString());
        bean.setBus(springBus);
        bean.setServiceClass(BookStore.class);
        bean.setAddress(address);
        List<Object> providers = new LinkedList<Object>();
        JwsJsonWriterInterceptor writer = new JwsJsonWriterInterceptor();
        writer.setUseJwsJsonOutputStream(true);
        providers.add(writer);
        providers.add(new JwsJsonClientResponseFilter());
        if (extraProviders != null) {
            providers.addAll(extraProviders);
        }
        bean.setProviders(providers);
        bean.getProperties(true).putAll(mapProperties);
        return bean.create(BookStore.class);
    }
    
}
