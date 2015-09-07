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

package org.eclipse.jetty.cdi.websocket;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;

import org.eclipse.jetty.cdi.websocket.annotation.WebSocketScope;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.websocket.api.Session;

/**
 * Producer of {@link org.eclipse.jetty.websocket.api.Session} instances
 */
public class JettyWebSocketSessionProducer
{
    private static final Logger LOG = Log.getLogger(JettyWebSocketSessionProducer.class);

    @Produces
    public Session getSession(InjectionPoint injectionPoint)
    {
        if (LOG.isDebugEnabled())
        {
            LOG.debug("getSession({})",injectionPoint);
        }
        WebSocketScopeContext ctx = WebSocketScopeContext.current();
        if (ctx == null)
        {
            throw new IllegalStateException("Not in a " + WebSocketScope.class.getName());
        }
        org.eclipse.jetty.websocket.api.Session sess = ctx.getSession();
        if (sess == null)
        {
            throw new IllegalStateException("No Session Available");
        }
        return sess;
    }
}
