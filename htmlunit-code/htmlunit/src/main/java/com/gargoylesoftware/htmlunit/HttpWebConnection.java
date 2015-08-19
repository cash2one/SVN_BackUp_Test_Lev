/*
 * Copyright (c) 2002-2015 Gargoyle Software Inc.
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
package com.gargoylesoftware.htmlunit;

import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.HEADER_CONTENT_DISPOSITION_ABSOLUTE_PATH;
import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.URL_AUTH_CREDENTIALS;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.ConnectionClosedException;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.protocol.RequestAcceptEncoding;
import org.apache.http.client.protocol.RequestAddCookies;
import org.apache.http.client.protocol.RequestAuthCache;
import org.apache.http.client.protocol.RequestClientConnControl;
import org.apache.http.client.protocol.RequestDefaultHeaders;
import org.apache.http.client.protocol.RequestExpectContinue;
import org.apache.http.client.protocol.ResponseContentEncoding;
import org.apache.http.client.protocol.ResponseProcessCookies;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.cookie.CookieSpecProvider;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessorBuilder;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.ssl.SSLContexts;

import com.gargoylesoftware.htmlunit.httpclient.HtmlUnitCookieSpecProvider;
import com.gargoylesoftware.htmlunit.httpclient.HtmlUnitCookieStore;
import com.gargoylesoftware.htmlunit.httpclient.HtmlUnitRedirectStrategie;
import com.gargoylesoftware.htmlunit.httpclient.HtmlUnitSSLConnectionSocketFactory;
import com.gargoylesoftware.htmlunit.httpclient.SocksConnectionSocketFactory;
import com.gargoylesoftware.htmlunit.util.KeyDataPair;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.gargoylesoftware.htmlunit.util.UrlUtils;

/**
 * Default implementation of {@link WebConnection}, using the HttpClient library to perform HTTP requests.
 *
 * @version $Revision: 10818 $
 * @author <a href="mailto:mbowler@GargoyleSoftware.com">Mike Bowler</a>
 * @author Noboru Sinohara
 * @author David D. Kilzer
 * @author Marc Guillemot
 * @author Brad Clarke
 * @author Ahmed Ashour
 * @author Nicolas Belisle
 * @author Ronald Brill
 * @author John J Murdoch
 * @author Carsten Steul
 */
public class HttpWebConnection implements WebConnection {

    private static final Log LOG = LogFactory.getLog(HttpWebConnection.class);

    private static final String HACKED_COOKIE_POLICY = "mine";

    // have one per thread because this is (re)configured for every call (see configureHttpProcessor)
    private ThreadLocal<HttpClientBuilder> httpClientBuilder_ = new ThreadLocal<HttpClientBuilder>();
    private final WebClient webClient_;

    /** Use single HttpContext, so there is no need to re-send authentication for each and every request. */
    private final HttpContext httpContext_;
    private String virtualHost_;
    private final CookieSpecProvider htmlUnitCookieSpecProvider_;
    private final WebClientOptions usedOptions_;
    private PoolingHttpClientConnectionManager connectionManager_;

    /**
     * Creates a new HTTP web connection instance.
     * @param webClient the WebClient that is using this connection
     */
    public HttpWebConnection(final WebClient webClient) {
        webClient_ = webClient;
        htmlUnitCookieSpecProvider_ = new HtmlUnitCookieSpecProvider(webClient.getBrowserVersion());
        httpContext_ = new HttpClientContext();
        usedOptions_ = new WebClientOptions();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WebResponse getResponse(final WebRequest request) throws IOException {
        final URL url = request.getUrl();
        final HttpClientBuilder builder = reconfigureHttpClientIfNeeded(getHttpClientBuilder());

        if (connectionManager_ == null) {
            connectionManager_ = createConnectionManager(builder);
        }
        builder.setConnectionManager(connectionManager_);

        HttpUriRequest httpMethod = null;
        try {
            try {
                httpMethod = makeHttpMethod(request, builder);
            }
            catch (final URISyntaxException e) {
                throw new IOException("Unable to create URI from URL: " + url.toExternalForm()
                        + " (reason: " + e.getMessage() + ")", e);
            }
            final HttpHost hostConfiguration = getHostConfiguration(request);
            final long startTime = System.currentTimeMillis();

            HttpResponse httpResponse = null;
            try {
                httpResponse = builder.build().execute(hostConfiguration, httpMethod, httpContext_);
            }
            catch (final SSLPeerUnverifiedException s) {
                // Try to use only SSLv3 instead
                if (webClient_.getOptions().isUseInsecureSSL()) {
                    HtmlUnitSSLConnectionSocketFactory.setUseSSL3Only(httpContext_, true);
                    httpResponse = builder.build().execute(hostConfiguration, httpMethod, httpContext_);
                }
                else {
                    throw s;
                }
            }
            catch (final Error e) {
                // in case a StackOverflowError occurs while the connection is leased, it won't get released.
                // Calling code may catch the StackOverflowError, but due to the leak, the httpClient_ may
                // come out of connections and throw a ConnectionPoolTimeoutException.
                // => best solution, discard the HttpClient instance.
                httpClientBuilder_.set(null);
                throw e;
            }

            final DownloadedContent downloadedBody = downloadResponseBody(httpResponse);
            final long endTime = System.currentTimeMillis();
            return makeWebResponse(httpResponse, request, downloadedBody, endTime - startTime);
        }
        finally {
            if (httpMethod != null) {
                onResponseGenerated(httpMethod);
            }
        }
    }

    /**
     * Called when the response has been generated. Default action is to release
     * the HttpMethod's connection. Subclasses may override.
     * @param httpMethod the httpMethod used (can be null)
     */
    protected void onResponseGenerated(final HttpUriRequest httpMethod) {
    }

    /**
     * Returns a new HttpClient host configuration, initialized based on the specified request.
     * @param webRequest the request to use to initialize the returned host configuration
     * @return a new HttpClient host configuration, initialized based on the specified request
     */
    private static HttpHost getHostConfiguration(final WebRequest webRequest) {
        final URL url = webRequest.getUrl();
        return new HttpHost(url.getHost(), url.getPort(), url.getProtocol());
    }

    private void setProxy(final HttpRequestBase httpRequest, final WebRequest webRequest) {
        final RequestConfig.Builder requestBuilder = createRequestConfigBuilder(getTimeout());

        if (webRequest.getProxyHost() != null) {
            final HttpHost proxy = new HttpHost(webRequest.getProxyHost(), webRequest.getProxyPort());
            if (webRequest.isSocksProxy()) {
                SocksConnectionSocketFactory.setSocksProxy(httpContext_, proxy);
            }
            else {
                requestBuilder.setProxy(proxy);
                httpRequest.setConfig(requestBuilder.build());
            }
        }
        else {
            requestBuilder.setProxy(null);
            httpRequest.setConfig(requestBuilder.build());
        }
    }

    /**
     * Creates an <tt>HttpMethod</tt> instance according to the specified parameters.
     * @param webRequest the request
     * @param httpClientBuilder the httpClientBuilder that will be configured
     * @return the <tt>HttpMethod</tt> instance constructed according to the specified parameters
     * @throws IOException
     * @throws URISyntaxException
     */
    @SuppressWarnings("deprecation")
    private HttpUriRequest makeHttpMethod(final WebRequest webRequest, final HttpClientBuilder httpClientBuilder)
        throws IOException, URISyntaxException {

        final String charset = webRequest.getCharset();

        // Make sure that the URL is fully encoded. IE actually sends some Unicode chars in request
        // URLs; because of this we allow some Unicode chars in URLs. However, at this point we're
        // handing things over the HttpClient, and HttpClient will blow up if we leave these Unicode
        // chars in the URL.
        final URL url = UrlUtils.encodeUrl(webRequest.getUrl(), false, charset);

        // URIUtils.createURI is deprecated but as of httpclient-4.2.1, URIBuilder doesn't work here as it encodes path
        // what shouldn't happen here
        URI uri = URIUtils.createURI(url.getProtocol(), url.getHost(), url.getPort(), url.getPath(),
                escapeQuery(url.getQuery()), null);
        if (getVirtualHost() != null) {
            uri = URI.create(getVirtualHost());
        }
        final HttpRequestBase httpMethod = buildHttpMethod(webRequest.getHttpMethod(), uri);
        setProxy(httpMethod, webRequest);
        if (!(httpMethod instanceof HttpEntityEnclosingRequest)) {
            // this is the case for GET as well as TRACE, DELETE, OPTIONS and HEAD
            if (!webRequest.getRequestParameters().isEmpty()) {
                final List<NameValuePair> pairs = webRequest.getRequestParameters();
                final org.apache.http.NameValuePair[] httpClientPairs = NameValuePair.toHttpClient(pairs);
                final String query = URLEncodedUtils.format(Arrays.asList(httpClientPairs), charset);
                uri = URIUtils.createURI(url.getProtocol(), url.getHost(), url.getPort(), url.getPath(), query, null);
                httpMethod.setURI(uri);
            }
        }
        else { // POST as well as PUT and PATCH
            final HttpEntityEnclosingRequest method = (HttpEntityEnclosingRequest) httpMethod;

            if (webRequest.getEncodingType() == FormEncodingType.URL_ENCODED && method instanceof HttpPost) {
                final HttpPost postMethod = (HttpPost) method;
                if (webRequest.getRequestBody() == null) {
                    final List<NameValuePair> pairs = webRequest.getRequestParameters();
                    final org.apache.http.NameValuePair[] httpClientPairs = NameValuePair.toHttpClient(pairs);
                    final String query = URLEncodedUtils.format(Arrays.asList(httpClientPairs), charset);
                    final StringEntity urlEncodedEntity = new StringEntity(query, charset);
                    urlEncodedEntity.setContentType(URLEncodedUtils.CONTENT_TYPE);
                    postMethod.setEntity(urlEncodedEntity);
                }
                else {
                    final String body = StringUtils.defaultString(webRequest.getRequestBody());
                    final StringEntity urlEncodedEntity = new StringEntity(body, charset);
                    urlEncodedEntity.setContentType(URLEncodedUtils.CONTENT_TYPE);
                    postMethod.setEntity(urlEncodedEntity);
                }
            }
            else if (FormEncodingType.MULTIPART == webRequest.getEncodingType()) {
                final Charset c = getCharset(charset, webRequest.getRequestParameters());
                final MultipartEntityBuilder builder = MultipartEntityBuilder.create().setLaxMode();
                builder.setCharset(c);

                for (final NameValuePair pair : webRequest.getRequestParameters()) {
                    if (pair instanceof KeyDataPair) {
                        buildFilePart((KeyDataPair) pair, builder);
                    }
                    else {
                        builder.addTextBody(pair.getName(), pair.getValue(),
                                ContentType.create("text/plain", charset));
                    }
                }
                method.setEntity(builder.build());
            }
            else { // for instance a PUT or PATCH request
                final String body = webRequest.getRequestBody();
                if (body != null) {
                    method.setEntity(new StringEntity(body, charset));
                }
            }
        }

        configureHttpProcessorBuilder(httpClientBuilder, webRequest);

        // Tell the client where to get its credentials from
        // (it may have changed on the webClient since last call to getHttpClientFor(...))
        final CredentialsProvider credentialsProvider = webClient_.getCredentialsProvider();

        // if the used url contains credentials, we have to add this
        final Credentials requestUrlCredentials = webRequest.getUrlCredentials();
        if (null != requestUrlCredentials
                && webClient_.getBrowserVersion().hasFeature(URL_AUTH_CREDENTIALS)) {
            final URL requestUrl = webRequest.getUrl();
            final AuthScope authScope = new AuthScope(requestUrl.getHost(), requestUrl.getPort());
            // updating our client to keep the credentials for the next request
            credentialsProvider.setCredentials(authScope, requestUrlCredentials);
            httpContext_.removeAttribute(HttpClientContext.TARGET_AUTH_STATE);
        }

        // if someone has set credentials to this request, we have to add this
        final Credentials requestCredentials = webRequest.getCredentials();
        if (null != requestCredentials) {
            final URL requestUrl = webRequest.getUrl();
            final AuthScope authScope = new AuthScope(requestUrl.getHost(), requestUrl.getPort());
            // updating our client to keep the credentials for the next request
            credentialsProvider.setCredentials(authScope, requestCredentials);
            httpContext_.removeAttribute(HttpClientContext.TARGET_AUTH_STATE);
        }
        httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
        httpContext_.removeAttribute(HttpClientContext.CREDS_PROVIDER);

        return httpMethod;
    }

    private String escapeQuery(final String query) {
        if (query == null) {
            return null;
        }
        return query.replace("%%", "%25%25");
    }

    private Charset getCharset(final String charset, final List<NameValuePair> pairs) {
        for (final NameValuePair pair : pairs) {
            if (pair instanceof KeyDataPair) {
                final KeyDataPair pairWithFile = (KeyDataPair) pair;
                if (pairWithFile.getData() == null && pairWithFile.getFile() != null) {
                    final String fileName = pairWithFile.getFile().getName();
                    for (int i = 0; i < fileName.length(); i++) {
                        if (fileName.codePointAt(i) > 127) {
                            return Charset.forName(charset);
                        }
                    }
                }
            }
        }
        return null;
    }

    void buildFilePart(final KeyDataPair pairWithFile, final MultipartEntityBuilder builder)
        throws IOException {
        String mimeType = pairWithFile.getMimeType();
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }

        final ContentType contentType = ContentType.create(mimeType);
        final File file = pairWithFile.getFile();

        if (pairWithFile.getData() != null) {
            final String filename;
            if (file == null) {
                filename = pairWithFile.getValue();
            }
            else if (webClient_.getBrowserVersion().hasFeature(HEADER_CONTENT_DISPOSITION_ABSOLUTE_PATH)) {
                filename = file.getAbsolutePath();
            }
            else {
                filename = file.getName();
            }

            builder.addBinaryBody(pairWithFile.getName(), new ByteArrayInputStream(pairWithFile.getData()),
                    contentType, filename);
            return;
        }

        if (file == null) {
            builder.addPart(pairWithFile.getName(),
                    // Overridden in order not to have a chunked response.
                    new InputStreamBody(new ByteArrayInputStream(new byte[0]), contentType, pairWithFile.getValue()) {
                    @Override
                    public long getContentLength() {
                        return 0;
                    }
                });
            return;
        }

        String filename;
        if (pairWithFile.getFile() == null) {
            filename = pairWithFile.getValue();
        }
        else if (webClient_.getBrowserVersion().hasFeature(HEADER_CONTENT_DISPOSITION_ABSOLUTE_PATH)) {
            filename = pairWithFile.getFile().getAbsolutePath();
        }
        else {
            filename = pairWithFile.getFile().getName();
        }
        builder.addBinaryBody(pairWithFile.getName(), pairWithFile.getFile(), contentType, filename);
    }

    /**
     * Creates and returns a new HttpClient HTTP method based on the specified parameters.
     * @param submitMethod the submit method being used
     * @param uri the uri being used
     * @return a new HttpClient HTTP method based on the specified parameters
     */
    private static HttpRequestBase buildHttpMethod(final HttpMethod submitMethod, final URI uri) {
        final HttpRequestBase method;
        switch (submitMethod) {
            case GET:
                method = new HttpGet(uri);
                break;

            case POST:
                method = new HttpPost(uri);
                break;

            case PUT:
                method = new HttpPut(uri);
                break;

            case DELETE:
                method = new HttpDelete(uri);
                break;

            case OPTIONS:
                method = new HttpOptions(uri);
                break;

            case HEAD:
                method = new HttpHead(uri);
                break;

            case TRACE:
                method = new HttpTrace(uri);
                break;

            case PATCH:
                method = new HttpPatch(uri);
                break;

            default:
                throw new IllegalStateException("Submit method not yet supported: " + submitMethod);
        }
        return method;
    }

    /**
     * Lazily initializes the internal HTTP client.
     *
     * @return the initialized HTTP client
     */
    protected HttpClientBuilder getHttpClientBuilder() {
        HttpClientBuilder builder = httpClientBuilder_.get();
        if (builder == null) {
            builder = createHttpClient();

            // this factory is required later
            // to be sure this is done, we do it outside the createHttpClient() call
            final RegistryBuilder<CookieSpecProvider> registeryBuilder
                = RegistryBuilder.<CookieSpecProvider>create()
                            .register(HACKED_COOKIE_POLICY, htmlUnitCookieSpecProvider_);
            builder.setDefaultCookieSpecRegistry(registeryBuilder.build());

            builder.setDefaultCookieStore(new HtmlUnitCookieStore(webClient_.getCookieManager()));
            httpClientBuilder_.set(builder);
        }

        return builder;
    }

    /**
     * Returns the timeout to use for socket and connection timeouts for HttpConnectionManager.
     * Is overridden to 0 by StreamingWebConnection which keeps reading after a timeout and
     * must have long running connections explicitly terminated.
     * @return the WebClient's timeout
     */
    protected int getTimeout() {
        return webClient_.getOptions().getTimeout();
    }

    /**
     * Creates the <tt>HttpClientBuilder</tt> that will be used by this WebClient.
     * Extensions may override this method in order to create a customized
     * <tt>HttpClientBuilder</tt> instance (e.g. with a custom
     * {@link org.apache.http.conn.ClientConnectionManager} to perform
     * some tracking; see feature request 1438216).
     * @return the <tt>HttpClientBuilder</tt> that will be used by this WebConnection
     */
    protected HttpClientBuilder createHttpClient() {
        final HttpClientBuilder builder = HttpClientBuilder.create();
        builder.setRedirectStrategy(new HtmlUnitRedirectStrategie());
        configureTimeout(builder, getTimeout());
        configureHttpsScheme(builder);
        builder.setMaxConnPerRoute(6);
        return builder;
    }

    private void configureTimeout(final HttpClientBuilder builder, final int timeout) {
        final RequestConfig.Builder requestBuilder = createRequestConfigBuilder(timeout);
        builder.setDefaultRequestConfig(requestBuilder.build());

        builder.setDefaultSocketConfig(createSocketConfigBuilder(timeout).build());

        httpContext_.removeAttribute(HttpClientContext.REQUEST_CONFIG);
        usedOptions_.setTimeout(timeout);
    }

    private RequestConfig.Builder createRequestConfigBuilder(final int timeout) {
        final RequestConfig.Builder requestBuilder = RequestConfig.custom()
                .setCookieSpec(HACKED_COOKIE_POLICY)
                .setRedirectsEnabled(false)

                // timeout
                .setConnectTimeout(timeout)
                .setConnectionRequestTimeout(timeout)
                .setSocketTimeout(timeout);
        return requestBuilder;
    }

    private SocketConfig.Builder createSocketConfigBuilder(final int timeout) {
        final SocketConfig.Builder socketBuilder = SocketConfig.custom()
                // timeout
                .setSoTimeout(timeout);
        return socketBuilder;
    }

    /**
     * React on changes that may have occurred on the WebClient settings.
     * Registering as a listener would be probably better.
     */
    private HttpClientBuilder reconfigureHttpClientIfNeeded(final HttpClientBuilder httpClientBuilder) {
        final WebClientOptions options = webClient_.getOptions();

        // register new SSL factory only if settings have changed
        if (options.isUseInsecureSSL() != usedOptions_.isUseInsecureSSL()
                || options.getSSLClientCertificateStore() != usedOptions_.getSSLClientCertificateStore()
                || options.getSSLTrustStore() != usedOptions_.getSSLTrustStore()
                || options.getSSLClientCipherSuites() != usedOptions_.getSSLClientCipherSuites()
                || options.getSSLClientProtocols() != usedOptions_.getSSLClientProtocols()
                || options.getProxyConfig() != usedOptions_.getProxyConfig()) {
            configureHttpsScheme(httpClientBuilder);
            if (connectionManager_ != null) {
                connectionManager_.shutdown();
                connectionManager_ = null;
            }
        }

        final int timeout = getTimeout();
        if (timeout != usedOptions_.getTimeout()) {
            configureTimeout(httpClientBuilder, timeout);
        }
        return httpClientBuilder;
    }

    private void configureHttpsScheme(final HttpClientBuilder builder) {
        final WebClientOptions options = webClient_.getOptions();

        final SSLConnectionSocketFactory socketFactory =
                HtmlUnitSSLConnectionSocketFactory.buildSSLSocketFactory(options);

        builder.setSSLSocketFactory(socketFactory);

        usedOptions_.setUseInsecureSSL(options.isUseInsecureSSL());
        usedOptions_.setSSLClientCertificateStore(options.getSSLClientCertificateStore());
        usedOptions_.setSSLTrustStore(options.getSSLTrustStore());
        usedOptions_.setSSLClientCipherSuites(options.getSSLClientCipherSuites());
        usedOptions_.setSSLClientProtocols(options.getSSLClientProtocols());
        usedOptions_.setProxyConfig(options.getProxyConfig());
    }

    private void configureHttpProcessorBuilder(final HttpClientBuilder builder, final WebRequest webRequest)
        throws IOException {
        final HttpProcessorBuilder b = HttpProcessorBuilder.create();
        for (final HttpRequestInterceptor i : getHttpRequestInterceptors(webRequest)) {
            b.add(i);
        }

        // These are the headers used in HttpClientBuilder, excluding the already added ones
        // (RequestClientConnControl and RequestAddCookies)
        b.addAll(new RequestDefaultHeaders(null),
                new RequestContent(),
                new RequestTargetHost(),
                new RequestExpectContinue());
        b.add(new RequestAcceptEncoding());
        b.add(new RequestAuthCache());
        b.add(new ResponseProcessCookies());
        b.add(new ResponseContentEncoding());
        builder.setHttpProcessor(b.build());
    }

    /**
     * Sets the virtual host.
     * @param virtualHost the virtualHost to set
     */
    public void setVirtualHost(final String virtualHost) {
        virtualHost_ = virtualHost;
    }

    /**
     * Gets the virtual host.
     * @return virtualHost The current virtualHost
     */
    public String getVirtualHost() {
        return virtualHost_;
    }

    /**
     * Converts an HttpMethod into a WebResponse.
     */
    private WebResponse makeWebResponse(final HttpResponse httpResponse,
            final WebRequest request, final DownloadedContent responseBody, final long loadTime) {

        String statusMessage = httpResponse.getStatusLine().getReasonPhrase();
        if (statusMessage == null) {
            statusMessage = "Unknown status message";
        }
        final int statusCode = httpResponse.getStatusLine().getStatusCode();
        final List<NameValuePair> headers = new ArrayList<>();
        for (final Header header : httpResponse.getAllHeaders()) {
            headers.add(new NameValuePair(header.getName(), header.getValue()));
        }
        final WebResponseData responseData = new WebResponseData(responseBody, statusCode, statusMessage, headers);
        return newWebResponseInstance(responseData, loadTime, request);
    }

    /**
     * Downloads the response body.
     * @param httpResponse the web server's response
     * @return a wrapper for the downloaded body.
     * @throws IOException in case of problem reading/saving the body
     */
    protected DownloadedContent downloadResponseBody(final HttpResponse httpResponse) throws IOException {
        final HttpEntity httpEntity = httpResponse.getEntity();
        if (httpEntity == null) {
            return new DownloadedContent.InMemory(new byte[] {});
        }

        return downloadContent(httpEntity.getContent(), webClient_.getOptions().getMaxInMemory());
    }

    /**
     * Reads the content of the stream and saves it in memory or on the file system.
     * @param is the stream to read
     * @param maxInMemory the maximumBytes to store in memory, after which save to a local file
     * @return a wrapper around the downloaded content
     * @throws IOException in case of read issues
     */
    public static DownloadedContent downloadContent(final InputStream is, final int maxInMemory) throws IOException {
        if (is == null) {
            return new DownloadedContent.InMemory(new byte[] {});
        }
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();

        final byte[] buffer = new byte[1024];
        int nbRead;
        try {
            while ((nbRead = is.read(buffer)) != -1) {
                bos.write(buffer, 0, nbRead);
                if (bos.size() > maxInMemory) {
                    // we have exceeded the max for memory, let's write everything to a temporary file
                    final File file = File.createTempFile("htmlunit", ".tmp");
                    file.deleteOnExit();
                    final FileOutputStream fos = new FileOutputStream(file);
                    bos.writeTo(fos); // what we have already read
                    IOUtils.copyLarge(is, fos); // what remains from the server response
                    fos.close();
                    return new DownloadedContent.OnFile(file, true);
                }
            }
        }
        catch (final ConnectionClosedException e) {
            LOG.warn("Connection was closed while reading from stream.", e);
            return new DownloadedContent.InMemory(bos.toByteArray());
        }
        catch (final EOFException e) {
            // this might happen with broken gzip content
            // see com.gargoylesoftware.htmlunit.HttpWebConnection2Test.brokenGzip()
            LOG.warn("EndOfFile while reading from stream.", e);
            return new DownloadedContent.InMemory(bos.toByteArray());
        }
        finally {
            IOUtils.closeQuietly(is);
        }

        return new DownloadedContent.InMemory(bos.toByteArray());
    }

    /**
     * Constructs an appropriate WebResponse.
     * May be overridden by subclasses to return a specialized WebResponse.
     * @param responseData Data that was send back
     * @param request the request used to get this response
     * @param loadTime How long the response took to be sent
     * @return the new WebResponse
     */
    protected WebResponse newWebResponseInstance(
            final WebResponseData responseData,
            final long loadTime,
            final WebRequest request) {
        return new WebResponse(responseData, request, loadTime);
    }

    private List<HttpRequestInterceptor> getHttpRequestInterceptors(final WebRequest webRequest) throws IOException {
        final List<HttpRequestInterceptor> list = new ArrayList<>();
        final Map<String, String> requestHeaders = webRequest.getAdditionalHeaders();
        final int port = webRequest.getUrl().getPort();
        final StringBuilder host = new StringBuilder(webRequest.getUrl().getHost());
        if (port != 80 && port > 0) {
            host.append(':');
            host.append(Integer.toString(port));
        }

        final String userAgent = webClient_.getBrowserVersion().getUserAgent();
        final String[] headerNames = webClient_.getBrowserVersion().getHeaderNamesOrdered();
        if (headerNames != null) {
            for (final String header : headerNames) {
                if ("Host".equals(header)) {
                    list.add(new HostHeaderHttpRequestInterceptor(host.toString()));
                }
                else if ("User-Agent".equals(header)) {
                    list.add(new UserAgentHeaderHttpRequestInterceptor(userAgent));
                }
                else if ("Accept".equals(header) && requestHeaders.get(header) != null) {
                    list.add(new AcceptHeaderHttpRequestInterceptor(requestHeaders.get(header)));
                }
                else if ("Accept-Language".equals(header) && requestHeaders.get(header) != null) {
                    list.add(new AcceptLanguageHeaderHttpRequestInterceptor(requestHeaders.get(header)));
                }
                else if ("Accept-Encoding".equals(header) && requestHeaders.get(header) != null) {
                    list.add(new AcceptEncodingHeaderHttpRequestInterceptor(requestHeaders.get(header)));
                }
                else if ("Referer".equals(header) && requestHeaders.get(header) != null) {
                    list.add(new RefererHeaderHttpRequestInterceptor(requestHeaders.get(header)));
                }
                else if ("Connection".equals(header)) {
                    list.add(new RequestClientConnControl());
                }
                else if ("Cookie".equals(header)) {
                    list.add(new RequestAddCookies());
                }
                else if ("DNT".equals(header) && webClient_.getOptions().isDoNotTrackEnabled()) {
                    list.add(new DntHeaderHttpRequestInterceptor("1"));
                }
            }
        }
        else {
            list.add(new UserAgentHeaderHttpRequestInterceptor(userAgent));
            list.add(new RequestAddCookies());
            list.add(new RequestClientConnControl());
        }

        // not all browser versions have DNT by default as part of getHeaderNamesOrdered()
        // so we add it again, in case
        if (webClient_.getOptions().isDoNotTrackEnabled()) {
            list.add(new DntHeaderHttpRequestInterceptor("1"));
        }

        synchronized (requestHeaders) {
            list.add(new MultiHttpRequestInterceptor(new HashMap<>(requestHeaders)));
        }
        return list;
    }

    /** We must have a separate class per header, because of org.apache.http.protocol.ChainBuilder. */
    private static final class HostHeaderHttpRequestInterceptor implements HttpRequestInterceptor {
        private String value_;

        HostHeaderHttpRequestInterceptor(final String value) {
            value_ = value;
        }

        @Override
        public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException {
            request.setHeader("Host", value_);
        }
    }

    private static final class UserAgentHeaderHttpRequestInterceptor implements HttpRequestInterceptor {
        private String value_;

        UserAgentHeaderHttpRequestInterceptor(final String value) {
            value_ = value;
        }

        @Override
        public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException {
            request.setHeader("User-Agent", value_);
        }
    }

    private static final class AcceptHeaderHttpRequestInterceptor implements HttpRequestInterceptor {
        private String value_;

        AcceptHeaderHttpRequestInterceptor(final String value) {
            value_ = value;
        }

        @Override
        public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException {
            request.setHeader("Accept", value_);
        }
    }

    private static final class AcceptLanguageHeaderHttpRequestInterceptor implements HttpRequestInterceptor {
        private String value_;

        AcceptLanguageHeaderHttpRequestInterceptor(final String value) {
            value_ = value;
        }

        @Override
        public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException {
            request.setHeader("Accept-Language", value_);
        }
    }

    private static final class AcceptEncodingHeaderHttpRequestInterceptor implements HttpRequestInterceptor {
        private String value_;

        AcceptEncodingHeaderHttpRequestInterceptor(final String value) {
            value_ = value;
        }

        @Override
        public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException {
            request.setHeader("Accept-Encoding", value_);
        }
    }

    private static final class RefererHeaderHttpRequestInterceptor implements HttpRequestInterceptor {
        private String value_;

        RefererHeaderHttpRequestInterceptor(final String value) {
            value_ = value;
        }

        @Override
        public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException {
            request.setHeader("Referer", value_);
        }
    }

    private static final class DntHeaderHttpRequestInterceptor implements HttpRequestInterceptor {
        private String value_;

        DntHeaderHttpRequestInterceptor(final String value) {
            value_ = value;
        }

        @Override
        public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException {
            request.setHeader("DNT", value_);
        }
    }

    private static class MultiHttpRequestInterceptor implements HttpRequestInterceptor {
        private final Map<String, String> map_;

        MultiHttpRequestInterceptor(final Map<String, String> map) {
            map_ = map;
        }

        @Override
        public void process(final HttpRequest request, final HttpContext context)
            throws HttpException, IOException {
            for (final String key : map_.keySet()) {
                request.setHeader(key, map_.get(key));
            }
        }
    }

    /**
     * Shutdown the connection.
     */
    public void shutdown() {
        if (httpClientBuilder_.get() != null) {
            httpClientBuilder_.set(null);
        }
        if (connectionManager_ != null) {
            connectionManager_.shutdown();
            connectionManager_ = null;
        }
    }

    /**
     * Has the exact logic in HttpClientBuilder, but with the ability to configure
     * <code>socketFactory</code>.
     */
    private PoolingHttpClientConnectionManager createConnectionManager(final HttpClientBuilder builder) {
        final ConnectionSocketFactory socketFactory = new SocksConnectionSocketFactory();

        LayeredConnectionSocketFactory sslSocketFactory;
        try {
            sslSocketFactory = (LayeredConnectionSocketFactory)
                        FieldUtils.readDeclaredField(builder, "sslSocketFactory", true);
            final SocketConfig defaultSocketConfig = (SocketConfig)
                        FieldUtils.readDeclaredField(builder, "defaultSocketConfig", true);
            final ConnectionConfig defaultConnectionConfig = (ConnectionConfig)
                        FieldUtils.readDeclaredField(builder, "defaultConnectionConfig", true);
            final boolean systemProperties = (Boolean) FieldUtils.readDeclaredField(builder, "systemProperties", true);
            final int maxConnTotal = (Integer) FieldUtils.readDeclaredField(builder, "maxConnTotal", true);
            final int maxConnPerRoute = (Integer) FieldUtils.readDeclaredField(builder, "maxConnPerRoute", true);
            HostnameVerifier hostnameVerifier = (HostnameVerifier)
                        FieldUtils.readDeclaredField(builder, "hostnameVerifier", true);
            final SSLContext sslcontext = (SSLContext) FieldUtils.readDeclaredField(builder, "sslContext", true);

            if (sslSocketFactory == null) {
                final String[] supportedProtocols = systemProperties
                        ? StringUtils.split(System.getProperty("https.protocols"), ',') : null;
                final String[] supportedCipherSuites = systemProperties
                        ? StringUtils.split(System.getProperty("https.cipherSuites"), ',') : null;
                if (hostnameVerifier == null) {
                    hostnameVerifier = SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER;
                }
                if (sslcontext != null) {
                    sslSocketFactory = new SSLConnectionSocketFactory(
                            sslcontext, supportedProtocols, supportedCipherSuites, hostnameVerifier);
                }
                else {
                    if (systemProperties) {
                        sslSocketFactory = new SSLConnectionSocketFactory(
                                (SSLSocketFactory) SSLSocketFactory.getDefault(),
                                supportedProtocols, supportedCipherSuites, hostnameVerifier);
                    }
                    else {
                        sslSocketFactory = new SSLConnectionSocketFactory(
                                SSLContexts.createDefault(),
                                hostnameVerifier);
                    }
                }
            }

            final PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(
                    RegistryBuilder.<ConnectionSocketFactory>create()
                        .register("http", socketFactory)
                        .register("https", sslSocketFactory)
                        .build());
            if (defaultSocketConfig != null) {
                connectionManager.setDefaultSocketConfig(defaultSocketConfig);
            }
            if (defaultConnectionConfig != null) {
                connectionManager.setDefaultConnectionConfig(defaultConnectionConfig);
            }
            if (systemProperties) {
                String s = System.getProperty("http.keepAlive", "true");
                if ("true".equalsIgnoreCase(s)) {
                    s = System.getProperty("http.maxConnections", "5");
                    final int max = Integer.parseInt(s);
                    connectionManager.setDefaultMaxPerRoute(max);
                    connectionManager.setMaxTotal(2 * max);
                }
            }
            if (maxConnTotal > 0) {
                connectionManager.setMaxTotal(maxConnTotal);
            }
            if (maxConnPerRoute > 0) {
                connectionManager.setDefaultMaxPerRoute(maxConnPerRoute);
            }
            return connectionManager;
        }
        catch (final IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
