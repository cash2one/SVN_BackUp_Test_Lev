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

package org.apache.cxf.binding.soap.saaj;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;

import org.apache.cxf.common.util.StringUtils;

/**
 * 
 */
public final class SAAJUtils {
    
    private SAAJUtils() {
        //not constructed
    }
    
    public static SOAPHeader getHeader(SOAPMessage m) throws SOAPException {
        try {
            return m.getSOAPHeader();
        } catch (UnsupportedOperationException ex) {
            return m.getSOAPPart().getEnvelope().getHeader();
        }
    }
    public static SOAPBody getBody(SOAPMessage m) throws SOAPException {
        try {
            return m.getSOAPBody();
        } catch (UnsupportedOperationException ex) {
            return m.getSOAPPart().getEnvelope().getBody();
        }
    }
    public static void setFaultCode(SOAPFault f, QName code) throws SOAPException {
        try {
            f.setFaultCode(code);
        } catch (Throwable t) {
            int count = 1;
            String pfx = "fc1";
            while (!StringUtils.isEmpty(f.getNamespaceURI(pfx))) {
                count++;
                pfx = "fc" + count;
            }
            f.addNamespaceDeclaration(pfx, code.getNamespaceURI());
            f.setFaultCode(pfx + ":" + code.getLocalPart());
        }
        
    }
}
