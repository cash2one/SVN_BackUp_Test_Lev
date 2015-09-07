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

package org.eclipse.jetty.websocket.common.scopes;

import org.eclipse.jetty.websocket.common.WebSocketSession;

/**
 * Defined Scope for a WebSocketSession (active connection)
 */
public interface WebSocketSessionScope
{
    /**
     * Active {@link WebSocketSession} associated with this scope.
     * @return the websocket session
     */
    WebSocketSession getWebSocketSession();

    /**
     * The parent {@link WebSocketContainerScope} for this session scope.
     * @return the websocket container scope
     */
    WebSocketContainerScope getContainerScope();
}
