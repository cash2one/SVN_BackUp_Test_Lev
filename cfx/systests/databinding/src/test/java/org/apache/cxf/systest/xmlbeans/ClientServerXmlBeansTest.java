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

package org.apache.cxf.systest.xmlbeans;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.soap.SOAPBinding;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.headers.Header;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.cxf.xmlbeans.docLitBare.types.InDecimalDocument;
import org.apache.cxf.xmlbeans.docLitBare.types.InDocument;
import org.apache.cxf.xmlbeans.docLitBare.types.InoutDocument;
import org.apache.cxf.xmlbeans.docLitBare.types.OutStringDocument;
import org.apache.cxf.xmlbeans.docLitBare.types.StringRespTypeDocument;
import org.apache.cxf.xmlbeans.docLitBare.types.TradePriceData;
import org.apache.cxf.xmlbeans.doc_lit_bare.PutLastTradedPricePortType;
import org.apache.helloWorldSoapHttpXmlbeans.xmlbeans.types.FaultDetailDocument;
import org.apache.helloWorldSoapHttpXmlbeans.xmlbeans.types.FaultDetailDocument.FaultDetail;
import org.apache.helloWorldSoapHttpXmlbeans.xmlbeans.types.TestEnum;
import org.apache.hello_world_soap_http_xmlbeans.xmlbeans.GreetMeFault;
import org.apache.hello_world_soap_http_xmlbeans.xmlbeans.Greeter;
import org.apache.hello_world_soap_http_xmlbeans.xmlbeans.PingMeFault;
import org.apache.hello_world_soap_http_xmlbeans.xmlbeans.SOAPService;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 
 */
public class ClientServerXmlBeansTest extends AbstractBusClientServerTestBase {
    static final String WSDL_PORT = TestUtil.getPortNumber(Server.class);
    static final String NOWSDL_PORT = TestUtil.getPortNumber(ServerNoWsdl.class);

    private static final QName SERVICE_NAME 
        = new QName("http://apache.org/hello_world_soap_http_xmlbeans/xmlbeans", "SOAPService");
    
    private static final QName DOC_LIT_BARE_SERVICE =
        new QName("http://cxf.apache.org/xmlbeans/doc_lit_bare", "SOAPService");
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
        assertTrue("server did not launch correctly", launchServer(ServerNoWsdl.class, true));
    }
    
    @Test
    public void testCallFromDocLitBareClient() throws Exception {
        SpringBusFactory factory = new SpringBusFactory();
        Bus bus = factory.createBus("org/apache/cxf/systest/xmlbeans/cxf.xml");
        BusFactory.setDefaultBus(bus);
        URL wsdl = this.getClass().getResource("/wsdl_systest_databinding/xmlbeans/doc_lit_bare.wsdl");
        assertNotNull("We should have found the WSDL here. " , wsdl);      
        
        org.apache.cxf.xmlbeans.doc_lit_bare.SOAPService ss = 
            new org.apache.cxf.xmlbeans.doc_lit_bare.SOAPService(wsdl, DOC_LIT_BARE_SERVICE);
        PutLastTradedPricePortType port = ss.getSoapPort();
        updateAddressPort(port, WSDL_PORT);
        
         
        ClientProxy.getClient(port).getInInterceptors().add(new LoggingInInterceptor());
        ClientProxy.getClient(port).getOutInterceptors().add(new LoggingOutInterceptor());
        StringRespTypeDocument resp = port.bareNoParam();
        assertEquals("Get a wrong response", "Get the request!", resp.getStringRespType());
        
        InDecimalDocument xd = InDecimalDocument.Factory.newInstance();
        xd.setInDecimal(new BigDecimal(123));
        OutStringDocument response = port.nillableParameter(xd);
        assertEquals("Get a wrong response", "Get the request 123", response.getOutString());
        
        InDocument document = InDocument.Factory.newInstance();
        TradePriceData data = document.addNewIn();
        data.setTickerPrice(12.33F);
        data.setTickerSymbol("CXF");
        port.putLastTradedPrice(document);
        
        InoutDocument inOut = InoutDocument.Factory.newInstance();
        data = inOut.addNewInout();
        data.setTickerPrice(12.33F);
        data.setTickerSymbol("CXF");
        Holder<InoutDocument> holder = new Holder<InoutDocument>(inOut);
        port.sayHi(holder);
        assertEquals("Get a wrong response", "BAK", holder.value.getInout().getTickerSymbol());
    }
    
    @Test
    public void testCallFromClient() throws Exception {
        SpringBusFactory factory = new SpringBusFactory();
        Bus bus = factory.createBus("org/apache/cxf/systest/xmlbeans/cxf.xml");
        BusFactory.setDefaultBus(bus);
        URL wsdl = this.getClass().getResource("/wsdl_systest_databinding/xmlbeans/hello_world.wsdl");
        assertNotNull("We should have found the WSDL here. " , wsdl);      
        
        SOAPService ss = new SOAPService(wsdl, SERVICE_NAME);
        Greeter port = ss.getSoapPort();
        updateAddressPort(port, WSDL_PORT);
        
        String resp; 
        ClientProxy.getClient(port).getInInterceptors().add(new LoggingInInterceptor());
        ClientProxy.getClient(port).getOutInterceptors().add(new LoggingOutInterceptor());
        
        TestEnum.Enum response = port.sayHiEnum(TestEnum.ONE);
        assertEquals(TestEnum.ONE, response);

        
        resp = port.sayHi();
        assertEquals("We should get the right response", "Bonjour", resp);        
        
        resp = port.greetMe("Willem");
        assertEquals("We should get the right response", "Hello Willem", resp);
        
        String aresp[] = port.sayHiArray(new String[] {"Dan"});
        assertEquals("Hello", aresp[0]);
        assertEquals("Dan", aresp[1]);

        try {
            port.greetMe("fault");
            fail("Should have been a fault");
        } catch (GreetMeFault ex) {
            assertEquals("Some fault detail", ex.getFaultInfo().getGreetMeFaultDetail());
        }

        try {
            resp = port.greetMe("Invoking greetMe with invalid length string, expecting exception...");
            fail("We expect exception here");
        } catch (WebServiceException ex) {           
            assertTrue("Get a wrong exception", 
                       ex.getMessage().
                       indexOf("string length (67) is greater than maxLength facet (30)") >= 0);
        }
        
        try {
            port.pingMe();
            fail("We expect exception here");
        } catch (PingMeFault ex) {            
            FaultDetailDocument detailDocument = ex.getFaultInfo();
            FaultDetail detail = detailDocument.getFaultDetail();
            assertEquals("Wrong faultDetail major", detail.getMajor(), 2);
            assertEquals("Wrong faultDetail minor", detail.getMinor(), 1);             
        }
    }
    
    @Test
    public void testCallFromClientNoWsdlServer() throws Exception {
        SpringBusFactory factory = new SpringBusFactory();
        Bus bus = factory.createBus("org/apache/cxf/systest/xmlbeans/cxf_no_wsdl.xml");
        BusFactory.setDefaultBus(bus);
        URL wsdl = this.getClass().getResource("/wsdl_systest_databinding/xmlbeans/hello_world.wsdl");
        assertNotNull("We should have found the WSDL here. " , wsdl);      
        
        SOAPService ss = new SOAPService(wsdl, SERVICE_NAME);
        QName soapPort = new QName("http://apache.org/hello_world_soap_http_xmlbeans/xmlbeans", "SoapPort");
        ss.addPort(soapPort, SOAPBinding.SOAP11HTTP_BINDING, "http://localhost:" 
                   + NOWSDL_PORT + "/SoapContext/SoapPort");
        Greeter port = ss.getPort(soapPort, Greeter.class);
        String resp; 
        ClientProxy.getClient(port).getInInterceptors().add(new LoggingInInterceptor());
        ClientProxy.getClient(port).getOutInterceptors().add(new LoggingOutInterceptor());
        resp = port.sayHi();
        assertEquals("We should get the right response", resp, "Bonjour");        
        
        resp = port.greetMe("Willem");
        assertEquals("We should get the right response", resp, "Hello Willem");
        
        try {
            resp = port.greetMe("Invoking greetMe with invalid length string, expecting exception...");
            fail("We expect exception here");
        } catch (WebServiceException ex) {           
            assertTrue("Get a wrong exception", 
                       ex.getMessage().
                       indexOf("string length (67) is greater than maxLength facet (30)") >= 0);
        }
        
        try {
            port.pingMe();
            fail("We expect exception here");
        } catch (PingMeFault ex) {            
            FaultDetailDocument detailDocument = ex.getFaultInfo();
            FaultDetail detail = detailDocument.getFaultDetail();
            assertEquals("Wrong faultDetail major", detail.getMajor(), 2);
            assertEquals("Wrong faultDetail minor", detail.getMinor(), 1);             
        }  
        try {
            port.greetMe("fault");
            fail("Should have been a fault");
        } catch (GreetMeFault ex) {
            assertEquals("Some fault detail", ex.getFaultInfo().getGreetMeFaultDetail());
        }

    }
    @Test
    public void testXmlBeansHeader() throws Exception {
        //CXF-2955
        SpringBusFactory factory = new SpringBusFactory();
        Bus bus = factory.createBus("org/apache/cxf/systest/xmlbeans/cxf_no_wsdl.xml");
        BusFactory.setDefaultBus(bus);
        URL wsdl = this.getClass().getResource("/wsdl_systest_databinding/xmlbeans/hello_world.wsdl");
        assertNotNull("We should have found the WSDL here. " , wsdl);      
        
        SOAPService ss = new SOAPService(wsdl, SERVICE_NAME);
        QName soapPort = new QName("http://apache.org/hello_world_soap_http_xmlbeans/xmlbeans", "SoapPort");
        ss.addPort(soapPort, SOAPBinding.SOAP11HTTP_BINDING, "http://localhost:" 
                   + NOWSDL_PORT + "/SoapContext/SoapPort");
        Greeter port = ss.getPort(soapPort, Greeter.class);
        
        Client client = ClientProxy.getClient(port);
        
        List<Header> headers = new ArrayList<Header>();
        org.apache.helloWorldSoapHttpXmlbeans.xmlbeans.types.GreetMeDocument doc 
            = org.apache.helloWorldSoapHttpXmlbeans.xmlbeans.types.GreetMeDocument.Factory.newInstance();
        doc.addNewGreetMe().setRequestType("doc format header");
        Header head = new Header(new QName("", "doc"), doc,
                                 client.getEndpoint().getService().getDataBinding());
        headers.add(head);
        org.apache.helloWorldSoapHttpXmlbeans.xmlbeans.types.GreetMeDocument.GreetMe gm 
            = org.apache.helloWorldSoapHttpXmlbeans.xmlbeans.types
                .GreetMeDocument.GreetMe.Factory.newInstance();
        gm.setRequestType("non-doc format header");
        head = new Header(new QName("http://somenamespace.com", "nondocheader"), gm,
                          client.getEndpoint().getService().getDataBinding());
        headers.add(head);
        ((BindingProvider)port).getRequestContext().put(Header.HEADER_LIST, headers);
        
        String resp; 
        ClientProxy.getClient(port).getInInterceptors().add(new LoggingInInterceptor());
        
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ClientProxy.getClient(port).getOutInterceptors().add(new LoggingOutInterceptor(pw));
        resp = port.sayHi();
        assertEquals("We should get the right response", resp, "Bonjour");
        assertTrue(sw.toString().contains("doc format header"));
        assertTrue(sw.toString().contains("non-doc format header"));
        assertTrue(sw.toString().contains("nondocheader"));
        
    }
}
