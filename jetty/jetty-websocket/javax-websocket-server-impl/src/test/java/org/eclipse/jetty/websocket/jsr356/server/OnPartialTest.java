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

package org.eclipse.jetty.websocket.jsr356.server;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.eclipse.jetty.websocket.common.WebSocketFrame;
import org.eclipse.jetty.websocket.common.events.EventDriver;
import org.eclipse.jetty.websocket.common.events.EventDriverFactory;
import org.eclipse.jetty.websocket.common.events.EventDriverImpl;
import org.eclipse.jetty.websocket.common.frames.ContinuationFrame;
import org.eclipse.jetty.websocket.common.frames.TextFrame;
import org.eclipse.jetty.websocket.common.scopes.SimpleContainerScope;
import org.eclipse.jetty.websocket.common.scopes.WebSocketContainerScope;
import org.eclipse.jetty.websocket.jsr356.ClientContainer;
import org.eclipse.jetty.websocket.jsr356.JsrSession;
import org.eclipse.jetty.websocket.jsr356.annotations.AnnotatedEndpointScanner;
import org.eclipse.jetty.websocket.jsr356.endpoints.EndpointInstance;
import org.eclipse.jetty.websocket.jsr356.server.samples.partial.PartialTrackingSocket;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

public class OnPartialTest
{
    @Rule
    public TestName testname = new TestName();

    public EventDriver toEventDriver(Object websocket) throws Throwable
    {
        WebSocketPolicy policy = WebSocketPolicy.newServerPolicy();
        policy.setInputBufferSize(1024);
        policy.setMaxBinaryMessageBufferSize(1024);
        policy.setMaxTextMessageBufferSize(1024);

        // Event Driver Factory
        EventDriverFactory factory = new EventDriverFactory(policy);
        factory.addImplementation(new JsrServerEndpointImpl());

        // Create EventDriver
        EventDriverImpl driverImpl = new JsrServerEndpointImpl();
        Class<?> endpoint = websocket.getClass();
        ServerEndpoint anno = endpoint.getAnnotation(ServerEndpoint.class);
        Assert.assertThat("Endpoint: " + endpoint + " should be annotated with @ServerEndpoint",anno,notNullValue());
        
        WebSocketContainerScope containerScope = new SimpleContainerScope(policy);
        
        ServerEndpointConfig config = new BasicServerEndpointConfig(containerScope,endpoint,"/");
        AnnotatedServerEndpointMetadata metadata = new AnnotatedServerEndpointMetadata(containerScope,endpoint,config);
        AnnotatedEndpointScanner<ServerEndpoint, ServerEndpointConfig> scanner = new AnnotatedEndpointScanner<>(metadata);
        scanner.scan();
        EndpointInstance ei = new EndpointInstance(websocket,config,metadata);
        EventDriver driver = driverImpl.create(ei,policy);
        Assert.assertThat("EventDriver",driver,notNullValue());

        // Create Local JsrSession
        String id = testname.getMethodName();
        URI requestURI = URI.create("ws://localhost/" + id);
        DummyConnection connection = new DummyConnection();
        ClientContainer container = new ClientContainer();
        @SuppressWarnings("resource")
        JsrSession session = new JsrSession(container,id,requestURI,driver,connection);
        session.setPolicy(policy);
        session.open();
        return driver;
    }

    @Test
    public void testOnTextPartial() throws Throwable
    {
        List<WebSocketFrame> frames = new ArrayList<>();
        frames.add(new TextFrame().setPayload("Saved").setFin(false));
        frames.add(new ContinuationFrame().setPayload(" by ").setFin(false));
        frames.add(new ContinuationFrame().setPayload("zero").setFin(true));

        PartialTrackingSocket socket = new PartialTrackingSocket();

        EventDriver driver = toEventDriver(socket);
        driver.onConnect();

        for (WebSocketFrame frame : frames)
        {
            driver.incomingFrame(frame);
        }

        Assert.assertThat("Captured Event Queue size",socket.eventQueue.size(),is(3));
        Assert.assertThat("Event[0]",socket.eventQueue.poll(),is("onPartial(\"Saved\",false)"));
        Assert.assertThat("Event[1]",socket.eventQueue.poll(),is("onPartial(\" by \",false)"));
        Assert.assertThat("Event[2]",socket.eventQueue.poll(),is("onPartial(\"zero\",true)"));
    }
}
