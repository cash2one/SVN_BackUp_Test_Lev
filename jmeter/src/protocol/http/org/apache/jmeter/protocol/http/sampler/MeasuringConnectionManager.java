/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.jmeter.protocol.http.sampler;

import org.apache.http.HttpConnectionMetrics;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpRequest;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.conn.ClientConnectionRequest;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.conn.DnsResolver;
import org.apache.http.conn.ManagedClientConnection;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.apache.jmeter.samplers.SampleResult;

import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

/**
 * Adapter for {@link PoolingClientConnectionManager}
 * that wraps all connection requests into time-measured implementation a private
 * MeasuringConnectionRequest
 */
public class MeasuringConnectionManager extends PoolingClientConnectionManager {

    private MeasuringConnectionRequest measuredConnection;
    private SampleResult sample;

    public MeasuringConnectionManager(SchemeRegistry schemeRegistry, DnsResolver resolver) {
        super(schemeRegistry, resolver);
    }

    @Override
    public ClientConnectionRequest requestConnection(final HttpRoute route, final Object state) {
        ClientConnectionRequest res = super.requestConnection(route, state);
        this.measuredConnection = new MeasuringConnectionRequest(res, this.sample);
        return this.measuredConnection;
    }

    public void setSample(SampleResult sample) {
        this.sample = sample;
    }

    /**
     * An adapter class to pass {@link SampleResult} into {@link MeasuredConnection}
     */
    private static class MeasuringConnectionRequest implements ClientConnectionRequest {
        private final ClientConnectionRequest handler;
        private final SampleResult sample;

        public MeasuringConnectionRequest(ClientConnectionRequest res, SampleResult sample) {
            handler = res;
            this.sample = sample;
        }

        @Override
        public ManagedClientConnection getConnection(long timeout, TimeUnit tunit) throws InterruptedException, ConnectionPoolTimeoutException {
            ManagedClientConnection res = handler.getConnection(timeout, tunit);
            return new MeasuredConnection(res, this.sample);
        }

        @Override
        public void abortRequest() {
            handler.abortRequest();
        }
    }

    /**
     * An adapter for {@link ManagedClientConnection}
     * that calls SampleResult.connectEnd after calling ManagedClientConnection.open
     */
    private static class MeasuredConnection implements ManagedClientConnection {
        private final ManagedClientConnection handler;
        private final SampleResult sample;

        public MeasuredConnection(ManagedClientConnection res, SampleResult sample) {
            handler = res;
            this.sample = sample;
        }

        @Override
        public void open(HttpRoute route, HttpContext context, HttpParams params) throws IOException {
            handler.open(route, context, params);
            if (sample != null) {
                sample.connectEnd();
            }
        }

        // ================= all following methods just wraps handler's =================
        @Override
        public boolean isSecure() {
            return handler.isSecure();
        }

        @Override
        public HttpRoute getRoute() {
            return handler.getRoute();
        }

        @Override
        public SSLSession getSSLSession() {
            return handler.getSSLSession();
        }

        @Override
        public void tunnelTarget(boolean secure, HttpParams params) throws IOException {
            handler.tunnelTarget(secure, params);
        }

        @Override
        public void tunnelProxy(HttpHost next, boolean secure, HttpParams params) throws IOException {
            handler.tunnelProxy(next, secure, params);
        }

        @Override
        public void layerProtocol(HttpContext context, HttpParams params) throws IOException {
            handler.layerProtocol(context, params);
        }

        @Override
        public void markReusable() {
            handler.markReusable();
        }

        @Override
        public void unmarkReusable() {
            handler.unmarkReusable();
        }

        @Override
        public boolean isMarkedReusable() {
            return handler.isMarkedReusable();
        }

        @Override
        public void setState(Object state) {
            handler.setState(state);
        }

        @Override
        public Object getState() {
            return handler.getState();
        }

        @Override
        public void setIdleDuration(long duration, TimeUnit unit) {
            handler.setIdleDuration(duration, unit);
        }

        @Override
        public void releaseConnection() throws IOException {
            handler.releaseConnection();
        }

        @Override
        public void abortConnection() throws IOException {
            handler.abortConnection();
        }

        @Override
        public boolean isResponseAvailable(int timeout) throws IOException {
            return handler.isResponseAvailable(timeout);
        }

        @Override
        public void sendRequestHeader(HttpRequest request) throws HttpException, IOException {
            handler.sendRequestHeader(request);
        }

        @Override
        public void sendRequestEntity(HttpEntityEnclosingRequest request) throws HttpException, IOException {
            handler.sendRequestEntity(request);
        }

        @Override
        public HttpResponse receiveResponseHeader() throws HttpException, IOException {
            return handler.receiveResponseHeader();
        }

        @Override
        public void receiveResponseEntity(HttpResponse response) throws HttpException, IOException {
            handler.receiveResponseEntity(response);
        }

        @Override
        public void flush() throws IOException {
            handler.flush();
        }

        @Override
        public InetAddress getLocalAddress() {
            return handler.getLocalAddress();
        }

        @Override
        public int getLocalPort() {
            return handler.getLocalPort();
        }

        @Override
        public InetAddress getRemoteAddress() {
            return handler.getRemoteAddress();
        }

        @Override
        public int getRemotePort() {
            return handler.getRemotePort();
        }

        @Override
        public void close() throws IOException {
            handler.close();
        }

        @Override
        public boolean isOpen() {
            return handler.isOpen();
        }

        @Override
        public boolean isStale() {
            return handler.isStale();
        }

        @Override
        public void setSocketTimeout(int timeout) {
            handler.setSocketTimeout(timeout);
        }

        @Override
        public int getSocketTimeout() {
            return handler.getSocketTimeout();
        }

        @Override
        public void shutdown() throws IOException {
            handler.shutdown();
        }

        @Override
        public HttpConnectionMetrics getMetrics() {
            return handler.getMetrics();
        }
    }
}
