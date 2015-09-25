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
package org.apache.cxf.xmlbeans;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.ws.commons.schema.constants.Constants;

import org.junit.Before;
import org.junit.Test;

public class WrappedStyleTest extends AbstractXmlBeansTest {
    private Server endpoint;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        endpoint = createService(TestService.class, new TestService(),
                                 "TestService", new QName("urn:TestService", "TestService"));

    }

    @Test
    public void testParams() throws Exception {
        String ns = "urn:TestService";
        
        OperationInfo op = endpoint.getEndpoint().getService()
            .getServiceInfos().get(0).getInterface().getOperation(new QName(ns, "GetWeatherByZipCode"));
        
        assertNotNull(op);
        MessagePartInfo info = op.getUnwrappedOperation().getInput().getMessagePart(0);

        assertEquals(new QName("http://cxf.apache.org/xmlbeans", "request"), info.getElementQName());
    }

    @Test
    public void testInvoke() throws Exception {
        Node response = invoke("TestService", "/org/apache/cxf/xmlbeans/WrappedRequest.xml");

        assertNotNull(response);

        addNamespace("t", "urn:TestService");
        addNamespace("x", "http://cxf.apache.org/xmlbeans");
        assertValid("//t:mixedRequestResponse/x:response/x:form", response);
    }

    @Test
    public void testFault() throws Exception {
        Node response = invoke("TestService", "/org/apache/cxf/xmlbeans/FaultRequest.xml");

        assertNotNull(response);

        addNamespace("t", "urn:TestService");
        addNamespace("x", "http://cxf.apache.org/xmlbeans/exception");
        assertValid("//detail/t:CustomFault[text()='extra']", response);
    }

    @Test
    public void testWSDL() throws Exception {
        Node wsdl = getWSDLDocument("TestService");

        addNamespace("xsd", Constants.URI_2001_SCHEMA_XSD);
        addNamespace("ns0", "http://cxf.apache.org/xmlbeans");
        assertValid("//xsd:schema[@targetNamespace='urn:TestService']" 
                    + "/xsd:complexType[@name='mixedRequest']"
                    + "//xsd:element[@name='string'][@type='xsd:string']", wsdl);
        
        NodeList list = assertValid("//xsd:schema[@targetNamespace='urn:TestService']" 
                    + "/xsd:complexType[@name='mixedRequest']"
                    + "//xsd:element[@ref]", wsdl);
        for (int x = 0; x < list.getLength(); x++) {
            Element el = (Element)list.item(x);
            assertTrue(el.getAttribute("ref"), el.getAttribute("ref").contains("request"));
        }
        
        assertValid("//xsd:schema[@targetNamespace='urn:TestService']"
                    + "/xsd:element[@name='CustomFault'][@type='xsd:string']", wsdl);

    }
}
