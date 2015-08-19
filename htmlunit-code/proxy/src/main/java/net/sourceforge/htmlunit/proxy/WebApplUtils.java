/*
 * Copyright (c) 2010 HtmlUnit team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sourceforge.htmlunit.proxy;

import org.mortbay.jetty.LocalConnector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.webapp.WebAppContext;

import sunlabs.brazil.server.Request;

/**
 * Utility for loading from the locally deployed WebApp.
 *
 * @author Ahmed Ashour
 * @version $Revision: 5517 $
 */
final class WebApplUtils {

    private static final Server server_;
    private static final LocalConnector connector_ = new LocalConnector();

    static {
        server_ = new Server();
        server_.addConnector(connector_);
        final WebAppContext context = new WebAppContext();
        context.setServer(server_);
        context.setContextPath("/");
        context.setWar("ProxyWebApp.war");
        server_.addHandler(context);

        try {
            server_.start();
        }
        catch (final Exception e) {
            e.printStackTrace();
        }
    }

    private WebApplUtils() { }

    /**
     * Respond to the request from the locally deployed WebApp.
     * @param request the request
     * @throws Exception if an error occurs
     */
    static synchronized void respond(final Request request) throws Exception {
        final HttpTester req = new HttpTester();
        final HttpTester res = new HttpTester();
        req.setMethod(request.method);
        if (request.version == 10) {
            req.setVersion("HTTP/1.0");
        }
        else {
            req.setVersion("HTTP/1.1");
            req.addHeader("Host", "localhost");
        }
        String uri = request.url;
        if (!request.url.startsWith("/")) {
            uri = uri.substring(uri.indexOf('/', 7));
        }
        req.setURI(uri);

        String content = null;
        if (request.postData != null) {
            content = new String(request.postData);
            final String contentType = request.getRequestHeader("Content-Type");
            if (contentType != null) {
                req.setHeader("Content-Type", contentType);
            }
            req.setContent(content);
        }

        connector_.reopen();

        final String generated = req.generate();
        final String response = connector_.getResponses(generated);
        res.parse(response);
        request.sendResponse(res.getContent(), res.getContentType(), res.getStatus());
    }

    /**
     * Sends a log request to the locally deployed WebApp
     * @param log the log entry
     * @throws Exception if an error occurs
     */
    static synchronized void addLog(final String log) {
        try {
            final HttpTester req = new HttpTester();
            req.setMethod("POST");
            req.setVersion("HTTP/1.1");
            req.addHeader("Host", "localhost");
            req.setURI("/proxywebapp/addLog");

            req.setHeader("Content-Length", String.valueOf(log.length()));
            req.setContent(log);

            connector_.reopen();

            final String generated = req.generate();
            connector_.getResponses(generated);
        }
        catch (final Exception e) {
            e.printStackTrace();
        }
    }
}
