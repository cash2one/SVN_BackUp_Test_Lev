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

package examples;

import java.security.Principal;

import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

public class MyAuthedConfigurator extends ServerEndpointConfig.Configurator
{
    @Override
    public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response)
    {
        // Is Authenticated?
        Principal principal = request.getUserPrincipal();
        if (principal == null)
        { 
            throw new RuntimeException("Not authenticated");
        }

        // Is Authorized?
        if (!request.isUserInRole("websocket"))
        {
            throw new RuntimeException("Not authorized");
        }
        
        // normal operation
        super.modifyHandshake(sec,request,response);
    }
}
