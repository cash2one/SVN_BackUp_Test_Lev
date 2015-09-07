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

package org.eclipse.jetty.websocket.jsr356.endpoints.samples.close;

import javax.websocket.ClientEndpoint;
import javax.websocket.OnClose;
import javax.websocket.Session;

import org.eclipse.jetty.websocket.jsr356.endpoints.TrackingSocket;

@ClientEndpoint
public class CloseSessionSocket extends TrackingSocket
{
    @OnClose
    public void onClose(Session session)
    {
        addEvent("onClose(Session)");
        closeLatch.countDown();
    }
}
