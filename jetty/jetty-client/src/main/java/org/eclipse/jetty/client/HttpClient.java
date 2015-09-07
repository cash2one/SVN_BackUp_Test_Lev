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

package org.eclipse.jetty.client;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieStore;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URI;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.client.api.AuthenticationStore;
import org.eclipse.jetty.client.api.Connection;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Destination;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.http.HttpClientTransportOverHTTP;
import org.eclipse.jetty.client.util.FormContentProvider;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpScheme;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.io.MappedByteBufferPool;
import org.eclipse.jetty.util.Fields;
import org.eclipse.jetty.util.Jetty;
import org.eclipse.jetty.util.Promise;
import org.eclipse.jetty.util.SocketAddressResolver;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.component.ContainerLifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;
import org.eclipse.jetty.util.thread.Scheduler;

/**
 * <p>{@link HttpClient} provides an efficient, asynchronous, non-blocking implementation
 * to perform HTTP requests to a server through a simple API that offers also blocking semantic.</p>
 * <p>{@link HttpClient} provides easy-to-use methods such as {@link #GET(String)} that allow to perform HTTP
 * requests in a one-liner, but also gives the ability to fine tune the configuration of requests via
 * {@link HttpClient#newRequest(URI)}.</p>
 * <p>{@link HttpClient} acts as a central configuration point for network parameters (such as idle timeouts)
 * and HTTP parameters (such as whether to follow redirects).</p>
 * <p>{@link HttpClient} transparently pools connections to servers, but allows direct control of connections
 * for cases where this is needed.</p>
 * <p>{@link HttpClient} also acts as a central configuration point for cookies, via {@link #getCookieStore()}.</p>
 * <p>Typical usage:</p>
 * <pre>
 * HttpClient httpClient = new HttpClient();
 * httpClient.start();
 *
 * // One liner:
 * httpClient.GET("http://localhost:8080/").getStatus();
 *
 * // Building a request with a timeout
 * ContentResponse response = httpClient.newRequest("http://localhost:8080")
 *         .timeout(5, TimeUnit.SECONDS)
 *         .send();
 * int status = response.status();
 *
 * // Asynchronously
 * httpClient.newRequest("http://localhost:8080").send(new Response.CompleteListener()
 * {
 *     &#64;Override
 *     public void onComplete(Result result)
 *     {
 *         ...
 *     }
 * });
 * </pre>
 */
@ManagedObject("The HTTP client")
public class HttpClient extends ContainerLifeCycle
{
    private static final Logger LOG = Log.getLogger(HttpClient.class);

    private final ConcurrentMap<Origin, HttpDestination> destinations = new ConcurrentHashMap<>();
    private final ProtocolHandlers handlers = new ProtocolHandlers();
    private final List<Request.Listener> requestListeners = new ArrayList<>();
    private final AuthenticationStore authenticationStore = new HttpAuthenticationStore();
    private final Set<ContentDecoder.Factory> decoderFactories = new ContentDecoderFactorySet();
    private final ProxyConfiguration proxyConfig = new ProxyConfiguration();
    private final HttpClientTransport transport;
    private final SslContextFactory sslContextFactory;
    private volatile CookieManager cookieManager;
    private volatile CookieStore cookieStore;
    private volatile Executor executor;
    private volatile ByteBufferPool byteBufferPool;
    private volatile Scheduler scheduler;
    private volatile SocketAddressResolver resolver;
    private volatile HttpField agentField = new HttpField(HttpHeader.USER_AGENT, "Jetty/" + Jetty.VERSION);
    private volatile boolean followRedirects = true;
    private volatile int maxConnectionsPerDestination = 64;
    private volatile int maxRequestsQueuedPerDestination = 1024;
    private volatile int requestBufferSize = 4096;
    private volatile int responseBufferSize = 16384;
    private volatile int maxRedirects = 8;
    private volatile SocketAddress bindAddress;
    private volatile long connectTimeout = 15000;
    private volatile long addressResolutionTimeout = 15000;
    private volatile long idleTimeout;
    private volatile boolean tcpNoDelay = true;
    private volatile boolean strictEventOrdering = false;
    private volatile HttpField encodingField;
    private volatile boolean removeIdleDestinations = false;
    private volatile boolean connectBlocking = false;

    /**
     * Creates a {@link HttpClient} instance that can perform requests to non-TLS destinations only
     * (that is, requests with the "http" scheme only, and not "https").
     *
     * @see #HttpClient(SslContextFactory) to perform requests to TLS destinations.
     */
    public HttpClient()
    {
        this(null);
    }

    /**
     * Creates a {@link HttpClient} instance that can perform requests to non-TLS and TLS destinations
     * (that is, both requests with the "http" scheme and with the "https" scheme).
     *
     * @param sslContextFactory the {@link SslContextFactory} that manages TLS encryption
     * @see #getSslContextFactory()
     */
    public HttpClient(SslContextFactory sslContextFactory)
    {
        this(new HttpClientTransportOverHTTP(), sslContextFactory);
    }

    public HttpClient(HttpClientTransport transport, SslContextFactory sslContextFactory)
    {
        this.transport = transport;
        this.sslContextFactory = sslContextFactory;
    }

    public HttpClientTransport getTransport()
    {
        return transport;
    }

    /**
     * @return the {@link SslContextFactory} that manages TLS encryption
     * @see #HttpClient(SslContextFactory)
     */
    public SslContextFactory getSslContextFactory()
    {
        return sslContextFactory;
    }

    @Override
    protected void doStart() throws Exception
    {
        if (sslContextFactory != null)
            addBean(sslContextFactory);

        String name = HttpClient.class.getSimpleName() + "@" + hashCode();

        if (executor == null)
        {
            QueuedThreadPool threadPool = new QueuedThreadPool();
            threadPool.setName(name);
            executor = threadPool;
        }
        addBean(executor);

        if (byteBufferPool == null)
            byteBufferPool = new MappedByteBufferPool();
        addBean(byteBufferPool);

        if (scheduler == null)
            scheduler = new ScheduledExecutorScheduler(name + "-scheduler", false);
        addBean(scheduler);

        transport.setHttpClient(this);
        addBean(transport);

        if (resolver == null)
            resolver = new SocketAddressResolver.Async(executor, scheduler, getAddressResolutionTimeout());
        addBean(resolver);

        handlers.put(new ContinueProtocolHandler());
        handlers.put(new RedirectProtocolHandler(this));
        handlers.put(new WWWAuthenticationProtocolHandler(this));
        handlers.put(new ProxyAuthenticationProtocolHandler(this));

        decoderFactories.add(new GZIPContentDecoder.Factory());

        cookieManager = newCookieManager();
        cookieStore = cookieManager.getCookieStore();

        super.doStart();
    }

    private CookieManager newCookieManager()
    {
        return new CookieManager(getCookieStore(), CookiePolicy.ACCEPT_ALL);
    }

    @Override
    protected void doStop() throws Exception
    {
        cookieStore.removeAll();
        decoderFactories.clear();
        handlers.clear();

        for (HttpDestination destination : destinations.values())
            destination.close();
        destinations.clear();

        requestListeners.clear();
        authenticationStore.clearAuthentications();
        authenticationStore.clearAuthenticationResults();

        super.doStop();
    }

    /**
     * Returns a <em>non</em> thread-safe list of {@link org.eclipse.jetty.client.api.Request.Listener}s that can be modified before
     * performing requests.
     *
     * @return a list of {@link org.eclipse.jetty.client.api.Request.Listener} that can be used to add and remove listeners
     */
    public List<Request.Listener> getRequestListeners()
    {
        return requestListeners;
    }

    /**
     * @return the cookie store associated with this instance
     */
    public CookieStore getCookieStore()
    {
        return cookieStore;
    }

    /**
     * @param cookieStore the cookie store associated with this instance
     */
    public void setCookieStore(CookieStore cookieStore)
    {
        this.cookieStore = Objects.requireNonNull(cookieStore);
        this.cookieManager = newCookieManager();
    }

    /**
     * Keep this method package-private because its interface is so ugly
     * that we really don't want to expose it more than strictly needed.
     *
     * @return the cookie manager
     */
    CookieManager getCookieManager()
    {
        return cookieManager;
    }

    /**
     * @return the authentication store associated with this instance
     */
    public AuthenticationStore getAuthenticationStore()
    {
        return authenticationStore;
    }

    /**
     * Returns a <em>non</em> thread-safe set of {@link ContentDecoder.Factory}s that can be modified before
     * performing requests.
     *
     * @return a set of {@link ContentDecoder.Factory} that can be used to add and remove content decoder factories
     */
    public Set<ContentDecoder.Factory> getContentDecoderFactories()
    {
        return decoderFactories;
    }

    /**
     * Performs a GET request to the specified URI.
     *
     * @param uri the URI to GET
     * @return the {@link ContentResponse} for the request
     * @throws InterruptedException if send threading has been interrupted
     * @throws ExecutionException the execution failed
     * @throws TimeoutException the send timed out
     * @see #GET(URI)
     */
    public ContentResponse GET(String uri) throws InterruptedException, ExecutionException, TimeoutException
    {
        return GET(URI.create(uri));
    }

    /**
     * Performs a GET request to the specified URI.
     *
     * @param uri the URI to GET
     * @return the {@link ContentResponse} for the request
     * @throws InterruptedException if send threading has been interrupted
     * @throws ExecutionException the execution failed
     * @throws TimeoutException the send timed out
     * @see #newRequest(URI)
     */
    public ContentResponse GET(URI uri) throws InterruptedException, ExecutionException, TimeoutException
    {
        return newRequest(uri).send();
    }

    /**
     * Performs a POST request to the specified URI with the given form parameters.
     *
     * @param uri the URI to POST
     * @param fields the fields composing the form name/value pairs
     * @return the {@link ContentResponse} for the request
     * @throws InterruptedException if send threading has been interrupted
     * @throws ExecutionException the execution failed
     * @throws TimeoutException the send timed out
     */
    public ContentResponse FORM(String uri, Fields fields) throws InterruptedException, ExecutionException, TimeoutException
    {
        return FORM(URI.create(uri), fields);
    }

    /**
     * Performs a POST request to the specified URI with the given form parameters.
     *
     * @param uri the URI to POST
     * @param fields the fields composing the form name/value pairs
     * @return the {@link ContentResponse} for the request
     * @throws InterruptedException if send threading has been interrupted
     * @throws ExecutionException the execution failed
     * @throws TimeoutException the send timed out
     */
    public ContentResponse FORM(URI uri, Fields fields) throws InterruptedException, ExecutionException, TimeoutException
    {
        return POST(uri).content(new FormContentProvider(fields)).send();
    }

    /**
     * Creates a POST request to the specified URI.
     *
     * @param uri the URI to POST to
     * @return the POST request
     * @see #POST(URI)
     */
    public Request POST(String uri)
    {
        return POST(URI.create(uri));
    }

    /**
     * Creates a POST request to the specified URI.
     *
     * @param uri the URI to POST to
     * @return the POST request
     */
    public Request POST(URI uri)
    {
        return newRequest(uri).method(HttpMethod.POST);
    }

    /**
     * Creates a new request with the "http" scheme and the specified host and port
     *
     * @param host the request host
     * @param port the request port
     * @return the request just created
     */
    public Request newRequest(String host, int port)
    {
        return newRequest(new Origin("http", host, port).asString());
    }

    /**
     * Creates a new request with the specified URI.
     *
     * @param uri the URI to request
     * @return the request just created
     */
    public Request newRequest(String uri)
    {
        return newRequest(URI.create(uri));
    }

    /**
     * Creates a new request with the specified URI.
     *
     * @param uri the URI to request
     * @return the request just created
     */
    public Request newRequest(URI uri)
    {
        return newHttpRequest(newConversation(), uri);
    }

    protected Request copyRequest(HttpRequest oldRequest, URI newURI)
    {
        Request newRequest = newHttpRequest(oldRequest.getConversation(), newURI);
        newRequest.method(oldRequest.getMethod())
                .version(oldRequest.getVersion())
                .content(oldRequest.getContent())
                .idleTimeout(oldRequest.getIdleTimeout(), TimeUnit.MILLISECONDS)
                .timeout(oldRequest.getTimeout(), TimeUnit.MILLISECONDS)
                .followRedirects(oldRequest.isFollowRedirects());
        for (HttpField field : oldRequest.getHeaders())
        {
            HttpHeader header = field.getHeader();
            // We have a new URI, so skip the host header if present.
            if (HttpHeader.HOST == header)
                continue;

            // Remove expectation headers.
            if (HttpHeader.EXPECT == header)
                continue;

            // Remove cookies.
            if (HttpHeader.COOKIE == header)
                continue;

            // Remove authorization headers.
            if (HttpHeader.AUTHORIZATION == header ||
                    HttpHeader.PROXY_AUTHORIZATION == header)
                continue;

            String value = field.getValue();
            if (!newRequest.getHeaders().contains(header, value))
                newRequest.header(field.getName(), value);
        }
        return newRequest;
    }

    protected HttpRequest newHttpRequest(HttpConversation conversation, URI uri)
    {
        return new HttpRequest(this, conversation, uri);
    }

    /**
     * Returns a {@link Destination} for the given scheme, host and port.
     * Applications may use {@link Destination}s to create {@link Connection}s
     * that will be outside {@link HttpClient}'s pooling mechanism, to explicitly
     * control the connection lifecycle (in particular their termination with
     * {@link Connection#close()}).
     *
     * @param scheme the destination scheme
     * @param host the destination host
     * @param port the destination port
     * @return the destination
     * @see #getDestinations()
     */
    public Destination getDestination(String scheme, String host, int port)
    {
        return destinationFor(scheme, host, port);
    }

    protected HttpDestination destinationFor(String scheme, String host, int port)
    {
        port = normalizePort(scheme, port);

        Origin origin = new Origin(scheme, host, port);
        HttpDestination destination = destinations.get(origin);
        if (destination == null)
        {
            destination = transport.newHttpDestination(origin);
            if (isRunning())
            {
                HttpDestination existing = destinations.putIfAbsent(origin, destination);
                if (existing != null)
                {
                    destination = existing;
                }
                else
                {
                    addManaged(destination);
                    if (LOG.isDebugEnabled())
                        LOG.debug("Created {}", destination);
                }

                if (!isRunning())
                    removeDestination(destination);
            }

        }
        return destination;
    }

    protected boolean removeDestination(HttpDestination destination)
    {
        removeBean(destination);
        return destinations.remove(destination.getOrigin()) != null;
    }

    /**
     * @return the list of destinations known to this {@link HttpClient}.
     */
    public List<Destination> getDestinations()
    {
        return new ArrayList<Destination>(destinations.values());
    }

    protected void send(final HttpRequest request, List<Response.ResponseListener> listeners)
    {
        String scheme = request.getScheme().toLowerCase(Locale.ENGLISH);
        if (!HttpScheme.HTTP.is(scheme) && !HttpScheme.HTTPS.is(scheme))
            throw new IllegalArgumentException("Invalid protocol " + scheme);

        String host = request.getHost().toLowerCase(Locale.ENGLISH);
        HttpDestination destination = destinationFor(scheme, host, request.getPort());
        destination.send(request, listeners);
    }

    protected void newConnection(final HttpDestination destination, final Promise<Connection> promise)
    {
        Origin.Address address = destination.getConnectAddress();
        resolver.resolve(address.getHost(), address.getPort(), new Promise<List<InetSocketAddress>>()
        {
            @Override
            public void succeeded(List<InetSocketAddress> socketAddresses)
            {
                Map<String, Object> context = new HashMap<>();
                context.put(HttpClientTransport.HTTP_DESTINATION_CONTEXT_KEY, destination);
                connect(socketAddresses, 0, context);
            }

            @Override
            public void failed(Throwable x)
            {
                promise.failed(x);
            }

            private void connect(List<InetSocketAddress> socketAddresses, int index, Map<String, Object> context)
            {
                context.put(HttpClientTransport.HTTP_CONNECTION_PROMISE_CONTEXT_KEY, new Promise<Connection>()
                {
                    @Override
                    public void succeeded(Connection result)
                    {
                        promise.succeeded(result);
                    }

                    @Override
                    public void failed(Throwable x)
                    {
                        int nextIndex = index + 1;
                        if (nextIndex == socketAddresses.size())
                            promise.failed(x);
                        else
                            connect(socketAddresses, nextIndex, context);
                    }
                });
                transport.connect(socketAddresses.get(index), context);
            }
        });
    }

    private HttpConversation newConversation()
    {
        return new HttpConversation();
    }

    public ProtocolHandlers getProtocolHandlers()
    {
        return handlers;
    }

    protected ProtocolHandler findProtocolHandler(Request request, Response response)
    {
        return handlers.find(request, response);
    }

    /**
     * @return the {@link ByteBufferPool} of this {@link HttpClient}
     */
    public ByteBufferPool getByteBufferPool()
    {
        return byteBufferPool;
    }

    /**
     * @param byteBufferPool the {@link ByteBufferPool} of this {@link HttpClient}
     */
    public void setByteBufferPool(ByteBufferPool byteBufferPool)
    {
        this.byteBufferPool = byteBufferPool;
    }

    /**
     * @return the max time, in milliseconds, a connection can take to connect to destinations
     */
    @ManagedAttribute("The timeout, in milliseconds, for connect() operations")
    public long getConnectTimeout()
    {
        return connectTimeout;
    }

    /**
     * @param connectTimeout the max time, in milliseconds, a connection can take to connect to destinations
     * @see java.net.Socket#connect(SocketAddress, int)
     */
    public void setConnectTimeout(long connectTimeout)
    {
        this.connectTimeout = connectTimeout;
    }

    /**
     * @return the timeout, in milliseconds, for the default {@link SocketAddressResolver} created at startup
     * @see #getSocketAddressResolver()
     */
    public long getAddressResolutionTimeout()
    {
        return addressResolutionTimeout;
    }

    /**
     * <p>Sets the socket address resolution timeout used by the default {@link SocketAddressResolver}
     * created by this {@link HttpClient} at startup.</p>
     * <p>For more fine tuned configuration of socket address resolution, see
     * {@link #setSocketAddressResolver(SocketAddressResolver)}.</p>
     *
     * @param addressResolutionTimeout the timeout, in milliseconds, for the default {@link SocketAddressResolver} created at startup
     * @see #setSocketAddressResolver(SocketAddressResolver)
     */
    public void setAddressResolutionTimeout(long addressResolutionTimeout)
    {
        this.addressResolutionTimeout = addressResolutionTimeout;
    }

    /**
     * @return the max time, in milliseconds, a connection can be idle (that is, without traffic of bytes in either direction)
     */
    @ManagedAttribute("The timeout, in milliseconds, to close idle connections")
    public long getIdleTimeout()
    {
        return idleTimeout;
    }

    /**
     * @param idleTimeout the max time, in milliseconds, a connection can be idle (that is, without traffic of bytes in either direction)
     */
    public void setIdleTimeout(long idleTimeout)
    {
        this.idleTimeout = idleTimeout;
    }

    /**
     * @return the address to bind socket channels to
     * @see #setBindAddress(SocketAddress)
     */
    public SocketAddress getBindAddress()
    {
        return bindAddress;
    }

    /**
     * @param bindAddress the address to bind socket channels to
     * @see #getBindAddress()
     * @see SocketChannel#bind(SocketAddress)
     */
    public void setBindAddress(SocketAddress bindAddress)
    {
        this.bindAddress = bindAddress;
    }

    /**
     * @return the "User-Agent" HTTP field of this {@link HttpClient}
     */
    public HttpField getUserAgentField()
    {
        return agentField;
    }

    /**
     * @param agent the "User-Agent" HTTP header string of this {@link HttpClient}
     */
    public void setUserAgentField(HttpField agent)
    {
        if (agent.getHeader() != HttpHeader.USER_AGENT)
            throw new IllegalArgumentException();
        this.agentField = agent;
    }

    /**
     * @return whether this {@link HttpClient} follows HTTP redirects
     * @see Request#isFollowRedirects()
     */
    @ManagedAttribute("Whether HTTP redirects are followed")
    public boolean isFollowRedirects()
    {
        return followRedirects;
    }

    /**
     * @param follow whether this {@link HttpClient} follows HTTP redirects
     * @see #setMaxRedirects(int)
     */
    public void setFollowRedirects(boolean follow)
    {
        this.followRedirects = follow;
    }

    /**
     * @return the {@link Executor} of this {@link HttpClient}
     */
    public Executor getExecutor()
    {
        return executor;
    }

    /**
     * @param executor the {@link Executor} of this {@link HttpClient}
     */
    public void setExecutor(Executor executor)
    {
        this.executor = executor;
    }

    /**
     * @return the {@link Scheduler} of this {@link HttpClient}
     */
    public Scheduler getScheduler()
    {
        return scheduler;
    }

    /**
     * @param scheduler the {@link Scheduler} of this {@link HttpClient}
     */
    public void setScheduler(Scheduler scheduler)
    {
        this.scheduler = scheduler;
    }

    /**
     * @return the {@link SocketAddressResolver} of this {@link HttpClient}
     */
    public SocketAddressResolver getSocketAddressResolver()
    {
        return resolver;
    }

    /**
     * @param resolver the {@link SocketAddressResolver} of this {@link HttpClient}
     */
    public void setSocketAddressResolver(SocketAddressResolver resolver)
    {
        this.resolver = resolver;
    }

    /**
     * @return the max number of connections that this {@link HttpClient} opens to {@link Destination}s
     */
    @ManagedAttribute("The max number of connections per each destination")
    public int getMaxConnectionsPerDestination()
    {
        return maxConnectionsPerDestination;
    }

    /**
     * Sets the max number of connections to open to each destinations.
     * <p>
     * RFC 2616 suggests that 2 connections should be opened per each destination,
     * but browsers commonly open 6.
     * If this {@link HttpClient} is used for load testing, it is common to have only one destination
     * (the server to load test), and it is recommended to set this value to a high value (at least as
     * much as the threads present in the {@link #getExecutor() executor}).
     *
     * @param maxConnectionsPerDestination the max number of connections that this {@link HttpClient} opens to {@link Destination}s
     */
    public void setMaxConnectionsPerDestination(int maxConnectionsPerDestination)
    {
        this.maxConnectionsPerDestination = maxConnectionsPerDestination;
    }

    /**
     * @return the max number of requests that may be queued to a {@link Destination}.
     */
    @ManagedAttribute("The max number of requests queued per each destination")
    public int getMaxRequestsQueuedPerDestination()
    {
        return maxRequestsQueuedPerDestination;
    }

    /**
     * Sets the max number of requests that may be queued to a destination.
     * <p>
     * If this {@link HttpClient} performs a high rate of requests to a destination,
     * and all the connections managed by that destination are busy with other requests,
     * then new requests will be queued up in the destination.
     * This parameter controls how many requests can be queued before starting to reject them.
     * If this {@link HttpClient} is used for load testing, it is common to have this parameter
     * set to a high value, although this may impact latency (requests sit in the queue for a long
     * time before being sent).
     *
     * @param maxRequestsQueuedPerDestination the max number of requests that may be queued to a {@link Destination}.
     */
    public void setMaxRequestsQueuedPerDestination(int maxRequestsQueuedPerDestination)
    {
        this.maxRequestsQueuedPerDestination = maxRequestsQueuedPerDestination;
    }

    /**
     * @return the size of the buffer used to write requests
     */
    @ManagedAttribute("The request buffer size")
    public int getRequestBufferSize()
    {
        return requestBufferSize;
    }

    /**
     * @param requestBufferSize the size of the buffer used to write requests
     */
    public void setRequestBufferSize(int requestBufferSize)
    {
        this.requestBufferSize = requestBufferSize;
    }

    /**
     * @return the size of the buffer used to read responses
     */
    @ManagedAttribute("The response buffer size")
    public int getResponseBufferSize()
    {
        return responseBufferSize;
    }

    /**
     * @param responseBufferSize the size of the buffer used to read responses
     */
    public void setResponseBufferSize(int responseBufferSize)
    {
        this.responseBufferSize = responseBufferSize;
    }

    /**
     * @return the max number of HTTP redirects that are followed
     * @see #setMaxRedirects(int)
     */
    public int getMaxRedirects()
    {
        return maxRedirects;
    }

    /**
     * @param maxRedirects the max number of HTTP redirects that are followed
     * @see #setFollowRedirects(boolean)
     */
    public void setMaxRedirects(int maxRedirects)
    {
        this.maxRedirects = maxRedirects;
    }

    /**
     * @return whether TCP_NODELAY is enabled
     */
    @ManagedAttribute(value = "Whether the TCP_NODELAY option is enabled", name = "tcpNoDelay")
    public boolean isTCPNoDelay()
    {
        return tcpNoDelay;
    }

    /**
     * @param tcpNoDelay whether TCP_NODELAY is enabled
     * @see java.net.Socket#setTcpNoDelay(boolean)
     */
    public void setTCPNoDelay(boolean tcpNoDelay)
    {
        this.tcpNoDelay = tcpNoDelay;
    }

    /**
     * @return true to dispatch I/O operations in a different thread, false to execute them in the selector thread
     * @see #setDispatchIO(boolean)
     */
    @Deprecated
    public boolean isDispatchIO()
    {
        // TODO this did default to true, so usage needs to be evaluated.
        return false;
    }

    /**
     * Whether to dispatch I/O operations from the selector thread to a different thread.
     * <p>
     * This implementation never blocks on I/O operation, but invokes application callbacks that may
     * take time to execute or block on other I/O.
     * If application callbacks are known to take time or block on I/O, then parameter {@code dispatchIO}
     * should be set to true.
     * If application callbacks are known to be quick and never block on I/O, then parameter {@code dispatchIO}
     * may be set to false.
     *
     * @param dispatchIO true to dispatch I/O operations in a different thread,
     *                   false to execute them in the selector thread
     */
    @Deprecated
    public void setDispatchIO(boolean dispatchIO)
    {
    }

    /**
     * @return whether request events must be strictly ordered
     * @see #setStrictEventOrdering(boolean)
     */
    @ManagedAttribute("Whether request/response events must be strictly ordered")
    public boolean isStrictEventOrdering()
    {
        return strictEventOrdering;
    }

    /**
     * Whether request/response events must be strictly ordered with respect to connection usage.
     * <p>
     * From the point of view of connection usage, the connection can be reused just before the
     * "complete" event notified to {@link org.eclipse.jetty.client.api.Response.CompleteListener}s
     * (but after the "success" event).
     * <p>
     * When a request/response exchange is completing, the destination may have another request
     * queued to be sent to the server.
     * If the connection for that destination is reused for the second request before the "complete"
     * event of the first exchange, it may happen that the "begin" event of the second request
     * happens before the "complete" event of the first exchange.
     * <p>
     * Enforcing strict ordering of events so that a "begin" event of a request can never happen
     * before the "complete" event of the previous exchange comes with the cost of increased
     * connection usage.
     * In case of HTTP redirects and strict event ordering, for example, the redirect request will
     * be forced to open a new connection because it is typically sent from the complete listener
     * when the connection cannot yet be reused.
     * When strict event ordering is not enforced, the redirect request will reuse the already
     * open connection making the system more efficient.
     * <p>
     * The default value for this property is {@code false}.
     *
     * @param strictEventOrdering whether request/response events must be strictly ordered
     */
    public void setStrictEventOrdering(boolean strictEventOrdering)
    {
        this.strictEventOrdering = strictEventOrdering;
    }

    /**
     * @return whether destinations that have no connections should be removed
     * @see #setRemoveIdleDestinations(boolean)
     */
    @ManagedAttribute("Whether idle destinations are removed")
    public boolean isRemoveIdleDestinations()
    {
        return removeIdleDestinations;
    }

    /**
     * Whether destinations that have no connections (nor active nor idle) should be removed.
     * <p>
     * Applications typically make request to a limited number of destinations so keeping
     * destinations around is not a problem for the memory or the GC.
     * However, for applications that hit millions of different destinations (e.g. a spider
     * bot) it would be useful to be able to remove the old destinations that won't be visited
     * anymore and leave space for new destinations.
     *
     * @param removeIdleDestinations whether destinations that have no connections should be removed
     * @see org.eclipse.jetty.client.ConnectionPool
     */
    public void setRemoveIdleDestinations(boolean removeIdleDestinations)
    {
        this.removeIdleDestinations = removeIdleDestinations;
    }

    /**
     * @return whether {@code connect()} operations are performed in blocking mode
     */
    @ManagedAttribute("Whether the connect() operation is blocking")
    public boolean isConnectBlocking()
    {
        return connectBlocking;
    }

    /**
     * <p>Whether {@code connect()} operations are performed in blocking mode.</p>
     * <p>If {@code connect()} are performed in blocking mode, then {@link Socket#connect(SocketAddress, int)}
     * will be used to connect to servers.</p>
     * <p>Otherwise, {@link SocketChannel#connect(SocketAddress)} will be used in non-blocking mode,
     * therefore registering for {@link SelectionKey#OP_CONNECT} and finishing the connect operation
     * when the NIO system emits that event.</p>
     *
     * @param connectBlocking whether {@code connect()} operations are performed in blocking mode
     */
    public void setConnectBlocking(boolean connectBlocking)
    {
        this.connectBlocking = connectBlocking;
    }

    /**
     * @return the forward proxy configuration
     */
    public ProxyConfiguration getProxyConfiguration()
    {
        return proxyConfig;
    }

    protected HttpField getAcceptEncodingField()
    {
        return encodingField;
    }

    protected String normalizeHost(String host)
    {
        if (host != null && host.matches("\\[.*\\]"))
            return host.substring(1, host.length() - 1);
        return host;
    }

    protected int normalizePort(String scheme, int port)
    {
        return port > 0 ? port : HttpScheme.HTTPS.is(scheme) ? 443 : 80;
    }

    public boolean isDefaultPort(String scheme, int port)
    {
        return HttpScheme.HTTPS.is(scheme) ? port == 443 : port == 80;
    }

    @Override
    public void dump(Appendable out, String indent) throws IOException
    {
        dumpThis(out);
        dump(out, indent, getBeans(), destinations.values());
    }

    private class ContentDecoderFactorySet implements Set<ContentDecoder.Factory>
    {
        private final Set<ContentDecoder.Factory> set = new HashSet<>();

        @Override
        public boolean add(ContentDecoder.Factory e)
        {
            boolean result = set.add(e);
            invalidate();
            return result;
        }

        @Override
        public boolean addAll(Collection<? extends ContentDecoder.Factory> c)
        {
            boolean result = set.addAll(c);
            invalidate();
            return result;
        }

        @Override
        public boolean remove(Object o)
        {
            boolean result = set.remove(o);
            invalidate();
            return result;
        }

        @Override
        public boolean removeAll(Collection<?> c)
        {
            boolean result = set.removeAll(c);
            invalidate();
            return result;
        }

        @Override
        public boolean retainAll(Collection<?> c)
        {
            boolean result = set.retainAll(c);
            invalidate();
            return result;
        }

        @Override
        public void clear()
        {
            set.clear();
            invalidate();
        }

        @Override
        public int size()
        {
            return set.size();
        }

        @Override
        public boolean isEmpty()
        {
            return set.isEmpty();
        }

        @Override
        public boolean contains(Object o)
        {
            return set.contains(o);
        }

        @Override
        public boolean containsAll(Collection<?> c)
        {
            return set.containsAll(c);
        }

        @Override
        public Iterator<ContentDecoder.Factory> iterator()
        {
            final Iterator<ContentDecoder.Factory> iterator = set.iterator();
            return new Iterator<ContentDecoder.Factory>()
            {
                @Override
                public boolean hasNext()
                {
                    return iterator.hasNext();
                }

                @Override
                public ContentDecoder.Factory next()
                {
                    return iterator.next();
                }

                @Override
                public void remove()
                {
                    iterator.remove();
                    invalidate();
                }
            };
        }

        @Override
        public Object[] toArray()
        {
            return set.toArray();
        }

        @Override
        public <T> T[] toArray(T[] a)
        {
            return set.toArray(a);
        }

        private void invalidate()
        {
            if (set.isEmpty())
            {
                encodingField = null;
            }
            else
            {
                StringBuilder value = new StringBuilder();
                for (Iterator<ContentDecoder.Factory> iterator = set.iterator(); iterator.hasNext();)
                {
                    ContentDecoder.Factory decoderFactory = iterator.next();
                    value.append(decoderFactory.getEncoding());
                    if (iterator.hasNext())
                        value.append(",");
                }
                encodingField = new HttpField(HttpHeader.ACCEPT_ENCODING, value.toString());
            }
        }
    }
}
