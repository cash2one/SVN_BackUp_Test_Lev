/*
*  Licensed to the Apache Software Foundation (ASF) under one
*  or more contributor license agreements.  See the NOTICE file
*  distributed with this work for additional information
*  regarding copyright ownership.  The ASF licenses this file
*  to you under the Apache License, Version 2.0 (the
*  "License"); you may not use this file except in compliance
*  with the License.  You may obtain a copy of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/
package org.apache.axis2.transport.base;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;

public class TransportListenerEndpointView implements TransportListenerEndpointViewMBean {
    private final AbstractTransportListener listener;
    private final String serviceName;
    
    public TransportListenerEndpointView(AbstractTransportListener listener, String serviceName) {
        this.listener = listener;
        this.serviceName = serviceName;
    }

    public String[] getAddresses() {
        String hostname;
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        }
        catch (UnknownHostException ex) {
            hostname = "localhost";
        }
        EndpointReference[] epr;
        try {
            epr = listener.getEPRsForService(serviceName, hostname);
        }
        catch (AxisFault ex) {
            return null;
        }
        if (epr == null) {
            return null;
        } else {
            String[] result = new String[epr.length];
            for (int i=0; i<epr.length; i++) {
                result[i] = epr[i].getAddress();
            }
            return result;
        }
    }
}
