// 
// Decompiled by Procyon v0.5.36
// 

package com.ning.http.client.providers.grizzly;

import com.ning.http.client.websocket.WebSocketPongListener;
import com.ning.http.client.websocket.WebSocketPingListener;
import com.ning.http.client.websocket.WebSocketByteListener;
import com.ning.http.client.websocket.WebSocketTextListener;
import org.glassfish.grizzly.websockets.draft06.ClosingFrame;
import com.ning.http.client.websocket.WebSocketCloseCodeReasonListener;
import org.glassfish.grizzly.websockets.DataFrame;
import org.glassfish.grizzly.ssl.SSLFilter;
import java.util.concurrent.Semaphore;
import org.glassfish.grizzly.utils.Futures;
import java.net.SocketAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import org.glassfish.grizzly.nio.transport.TCPNIOConnectorHandler;
import com.ning.http.client.Body;
import com.ning.http.client.BodyGenerator;
import java.io.File;
import org.glassfish.grizzly.EmptyCompletionHandler;
import org.glassfish.grizzly.FileTransfer;
import java.io.FileInputStream;
import com.ning.http.multipart.MultipartRequestEntity;
import com.ning.http.client.Part;
import java.io.InputStream;
import java.io.OutputStream;
import org.glassfish.grizzly.utils.BufferOutputStream;
import org.glassfish.grizzly.memory.MemoryManager;
import org.glassfish.grizzly.memory.Buffers;
import org.glassfish.grizzly.utils.Charsets;
import com.ning.http.client.ConnectionsPool;
import org.glassfish.grizzly.http.util.DataChunk;
import java.security.NoSuchAlgorithmException;
import com.ning.http.client.Realm;
import com.ning.http.client.RequestBuilder;
import com.ning.http.client.websocket.WebSocketUpgradeHandler;
import org.glassfish.grizzly.websockets.WebSocketEngine;
import org.glassfish.grizzly.websockets.DefaultWebSocket;
import org.glassfish.grizzly.websockets.WebSocketListener;
import com.ning.http.client.filter.ResponseFilter;
import com.ning.http.client.filter.FilterContext;
import com.ning.http.client.MaxRedirectException;
import org.glassfish.grizzly.http.HttpResponsePacket;
import org.glassfish.grizzly.http.HttpHeader;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.util.HttpStatus;
import java.util.HashMap;
import org.glassfish.grizzly.http.HttpClientFilter;
import com.ning.http.client.FluentStringsMap;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import org.glassfish.grizzly.http.util.CookieSerializerUtils;
import org.glassfish.grizzly.http.Cookie;
import org.glassfish.grizzly.http.util.MimeHeaders;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import com.ning.http.client.UpgradeHandler;
import com.ning.http.client.ProxyServer;
import com.ning.http.client.FluentCaseInsensitiveStringsMap;
import com.ning.http.client.listener.TransferCompletionHandler;
import com.ning.http.util.AuthenticatorUtils;
import com.ning.http.util.ProxyUtils;
import java.net.URISyntaxException;
import org.glassfish.grizzly.websockets.Version;
import org.glassfish.grizzly.http.Protocol;
import com.ning.http.util.AsyncHttpProviderUtils;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.filterchain.BaseFilter;
import java.io.EOFException;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.filterchain.FilterChainEvent;
import java.util.concurrent.Callable;
import com.ning.http.client.websocket.WebSocket;
import org.glassfish.grizzly.websockets.ProtocolHandler;
import org.glassfish.grizzly.websockets.HandShake;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.LoggerFactory;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.util.Header;
import org.glassfish.grizzly.http.HttpRequestPacket;
import java.net.URI;
import java.util.concurrent.TimeoutException;
import org.glassfish.grizzly.WriteResult;
import org.glassfish.grizzly.strategies.SameThreadIOStrategy;
import org.glassfish.grizzly.IOStrategy;
import org.glassfish.grizzly.strategies.WorkerThreadIOStrategy;
import javax.net.ssl.SSLContext;
import org.glassfish.grizzly.Processor;
import org.glassfish.grizzly.websockets.WebSocketFilter;
import org.glassfish.grizzly.http.ContentEncoding;
import org.glassfish.grizzly.http.EncodingFilter;
import org.glassfish.grizzly.http.GZipContentEncoding;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import com.ning.http.util.SslUtils;
import com.ning.http.client.PerRequestConfig;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.utils.IdleTimeoutFilter;
import java.util.concurrent.TimeUnit;
import org.glassfish.grizzly.filterchain.Filter;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.attributes.AttributeStorage;
import com.ning.http.client.Response;
import com.ning.http.client.HttpResponseBodyPart;
import java.util.Collection;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;
import java.util.concurrent.ExecutorService;
import org.glassfish.grizzly.CompletionHandler;
import org.glassfish.grizzly.impl.FutureImpl;
import org.glassfish.grizzly.impl.SafeFutureImpl;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.AsyncHandler;
import com.ning.http.client.Request;
import java.io.IOException;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.utils.DelayedExecutor;
import com.ning.http.client.AsyncHttpClientConfig;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.attributes.Attribute;
import org.slf4j.Logger;
import com.ning.http.client.AsyncHttpProvider;

public class GrizzlyAsyncHttpProvider implements AsyncHttpProvider
{
    private static final Logger LOGGER;
    private static final boolean SEND_FILE_SUPPORT;
    private final Attribute<HttpTransactionContext> REQUEST_STATE_ATTR;
    private final BodyHandlerFactory bodyHandlerFactory;
    private final TCPNIOTransport clientTransport;
    private final AsyncHttpClientConfig clientConfig;
    private final ConnectionManager connectionManager;
    DelayedExecutor.Resolver<Connection> resolver;
    private DelayedExecutor timeoutExecutor;
    
    public GrizzlyAsyncHttpProvider(final AsyncHttpClientConfig clientConfig) {
        this.REQUEST_STATE_ATTR = (Attribute<HttpTransactionContext>)Grizzly.DEFAULT_ATTRIBUTE_BUILDER.createAttribute(HttpTransactionContext.class.getName());
        this.bodyHandlerFactory = new BodyHandlerFactory();
        this.clientConfig = clientConfig;
        final TCPNIOTransportBuilder builder = TCPNIOTransportBuilder.newInstance();
        this.clientTransport = builder.build();
        this.initializeTransport(clientConfig);
        this.connectionManager = new ConnectionManager(this, this.clientTransport);
        try {
            this.clientTransport.start();
        }
        catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }
    
    public <T> ListenableFuture<T> execute(final Request request, final AsyncHandler<T> handler) throws IOException {
        final GrizzlyResponseFuture<T> future = new GrizzlyResponseFuture<T>(this, request, handler);
        future.setDelegate((org.glassfish.grizzly.impl.FutureImpl<T>)SafeFutureImpl.create());
        final CompletionHandler<Connection> connectHandler = (CompletionHandler<Connection>)new CompletionHandler<Connection>() {
            public void cancelled() {
                future.cancel(true);
            }
            
            public void failed(final Throwable throwable) {
                future.abort(throwable);
            }
            
            public void completed(final Connection c) {
                try {
                    GrizzlyAsyncHttpProvider.this.execute(c, request, handler, (GrizzlyResponseFuture<Object>)future);
                }
                catch (Exception e) {
                    if (e instanceof RuntimeException) {
                        this.failed(e);
                    }
                    else if (e instanceof IOException) {
                        this.failed(e);
                    }
                    if (GrizzlyAsyncHttpProvider.LOGGER.isWarnEnabled()) {
                        GrizzlyAsyncHttpProvider.LOGGER.warn(e.toString(), (Throwable)e);
                    }
                }
            }
            
            public void updated(final Connection c) {
            }
        };
        try {
            this.connectionManager.doAsyncTrackedConnection(request, future, connectHandler);
        }
        catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException)e;
            }
            if (e instanceof IOException) {
                throw (IOException)e;
            }
            if (GrizzlyAsyncHttpProvider.LOGGER.isWarnEnabled()) {
                GrizzlyAsyncHttpProvider.LOGGER.warn(e.toString(), (Throwable)e);
            }
        }
        return future;
    }
    
    public void close() {
        try {
            this.connectionManager.destroy();
            this.clientTransport.stop();
            final ExecutorService service = this.clientConfig.executorService();
            if (service != null) {
                service.shutdown();
            }
            if (this.timeoutExecutor != null) {
                this.timeoutExecutor.stop();
            }
        }
        catch (IOException ex) {}
    }
    
    public Response prepareResponse(final HttpResponseStatus status, final HttpResponseHeaders headers, final Collection<HttpResponseBodyPart> bodyParts) {
        return new GrizzlyResponse(status, headers, bodyParts);
    }
    
    protected <T> ListenableFuture<T> execute(final Connection c, final Request request, final AsyncHandler<T> handler, final GrizzlyResponseFuture<T> future) throws IOException {
        try {
            if (this.getHttpTransactionContext((AttributeStorage)c) == null) {
                this.setHttpTransactionContext((AttributeStorage)c, new HttpTransactionContext(future, request, handler));
            }
            c.write((Object)request, (CompletionHandler)this.createWriteCompletionHandler(future));
        }
        catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException)e;
            }
            if (e instanceof IOException) {
                throw (IOException)e;
            }
            if (GrizzlyAsyncHttpProvider.LOGGER.isWarnEnabled()) {
                GrizzlyAsyncHttpProvider.LOGGER.warn(e.toString(), (Throwable)e);
            }
        }
        return future;
    }
    
    protected void initializeTransport(final AsyncHttpClientConfig clientConfig) {
        final FilterChainBuilder fcb = FilterChainBuilder.stateless();
        fcb.add((Filter)new AsyncHttpClientTransportFilter());
        final int timeout = clientConfig.getRequestTimeoutInMs();
        if (timeout > 0) {
            int delay = 500;
            if (timeout < delay) {
                delay = timeout - 10;
            }
            (this.timeoutExecutor = IdleTimeoutFilter.createDefaultIdleDelayedExecutor((long)delay, TimeUnit.MILLISECONDS)).start();
            final IdleTimeoutFilter.TimeoutResolver timeoutResolver = (IdleTimeoutFilter.TimeoutResolver)new IdleTimeoutFilter.TimeoutResolver() {
                public long getTimeout(final FilterChainContext ctx) {
                    final HttpTransactionContext context = GrizzlyAsyncHttpProvider.this.getHttpTransactionContext((AttributeStorage)ctx.getConnection());
                    if (context != null) {
                        if (context.isWSRequest) {
                            return clientConfig.getWebSocketIdleTimeoutInMs();
                        }
                        final PerRequestConfig config = context.request.getPerRequestConfig();
                        if (config != null) {
                            final long timeout = config.getRequestTimeoutInMs();
                            if (timeout > 0L) {
                                return timeout;
                            }
                        }
                    }
                    return timeout;
                }
            };
            final IdleTimeoutFilter timeoutFilter = new IdleTimeoutFilter(this.timeoutExecutor, timeoutResolver, (IdleTimeoutFilter.TimeoutHandler)new IdleTimeoutFilter.TimeoutHandler() {
                public void onTimeout(final Connection connection) {
                    GrizzlyAsyncHttpProvider.this.timeout(connection);
                }
            });
            fcb.add((Filter)timeoutFilter);
            this.resolver = (DelayedExecutor.Resolver<Connection>)timeoutFilter.getResolver();
        }
        SSLContext context = clientConfig.getSSLContext();
        final boolean defaultSecState = context != null;
        if (context == null) {
            try {
                context = SslUtils.getSSLContext();
            }
            catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        final SSLEngineConfigurator configurator = new SSLEngineConfigurator(context, true, false, false);
        final SwitchingSSLFilter filter = new SwitchingSSLFilter(configurator, defaultSecState);
        fcb.add((Filter)filter);
        final AsyncHttpClientEventFilter eventFilter = new AsyncHttpClientEventFilter(this);
        final AsyncHttpClientFilter clientFilter = new AsyncHttpClientFilter(clientConfig);
        final ContentEncoding[] encodings = eventFilter.getContentEncodings();
        if (encodings.length > 0) {
            for (final ContentEncoding encoding : encodings) {
                eventFilter.removeContentEncoding(encoding);
            }
        }
        if (clientConfig.isCompressionEnabled()) {
            eventFilter.addContentEncoding((ContentEncoding)new GZipContentEncoding(512, 512, (EncodingFilter)new ClientEncodingFilter()));
        }
        fcb.add((Filter)eventFilter);
        fcb.add((Filter)clientFilter);
        final GrizzlyAsyncHttpProviderConfig providerConfig = (GrizzlyAsyncHttpProviderConfig)clientConfig.getAsyncHttpProviderConfig();
        if (providerConfig != null) {
            final TransportCustomizer customizer = (TransportCustomizer)providerConfig.getProperty(GrizzlyAsyncHttpProviderConfig.Property.TRANSPORT_CUSTOMIZER);
            if (customizer != null) {
                customizer.customize(this.clientTransport, fcb);
            }
            else {
                this.doDefaultTransportConfig();
            }
        }
        else {
            this.doDefaultTransportConfig();
        }
        fcb.add((Filter)new WebSocketFilter());
        this.clientTransport.getAsyncQueueIO().getWriter().setMaxPendingBytesPerConnection(-1);
        this.clientTransport.setProcessor((Processor)fcb.build());
    }
    
    void touchConnection(final Connection c, final Request request) {
        final PerRequestConfig config = request.getPerRequestConfig();
        if (config != null) {
            final long timeout = config.getRequestTimeoutInMs();
            if (timeout > 0L) {
                final long newTimeout = System.currentTimeMillis() + timeout;
                if (this.resolver != null) {
                    this.resolver.setTimeoutMillis((Object)c, newTimeout);
                }
            }
        }
        else {
            final long timeout = this.clientConfig.getRequestTimeoutInMs();
            if (timeout > 0L && this.resolver != null) {
                this.resolver.setTimeoutMillis((Object)c, System.currentTimeMillis() + timeout);
            }
        }
    }
    
    private static boolean configSendFileSupport() {
        return (!System.getProperty("os.name").equalsIgnoreCase("linux") || linuxSendFileSupported()) && !System.getProperty("os.name").equalsIgnoreCase("HP-UX");
    }
    
    private static boolean linuxSendFileSupported() {
        final String version = System.getProperty("java.version");
        if (!version.startsWith("1.6")) {
            return version.startsWith("1.7") || version.startsWith("1.8");
        }
        final int idx = version.indexOf(95);
        if (idx == -1) {
            return false;
        }
        final int patchRev = Integer.parseInt(version.substring(idx + 1));
        return patchRev >= 18;
    }
    
    private void doDefaultTransportConfig() {
        final ExecutorService service = this.clientConfig.executorService();
        if (service != null) {
            this.clientTransport.setIOStrategy((IOStrategy)WorkerThreadIOStrategy.getInstance());
            this.clientTransport.setWorkerThreadPool(service);
        }
        else {
            this.clientTransport.setIOStrategy((IOStrategy)SameThreadIOStrategy.getInstance());
        }
    }
    
    private <T> CompletionHandler<WriteResult> createWriteCompletionHandler(final GrizzlyResponseFuture<T> future) {
        return (CompletionHandler<WriteResult>)new CompletionHandler<WriteResult>() {
            public void cancelled() {
                future.cancel(true);
            }
            
            public void failed(final Throwable throwable) {
                future.abort(throwable);
            }
            
            public void completed(final WriteResult result) {
            }
            
            public void updated(final WriteResult result) {
            }
        };
    }
    
    void setHttpTransactionContext(final AttributeStorage storage, final HttpTransactionContext httpTransactionState) {
        if (httpTransactionState == null) {
            this.REQUEST_STATE_ATTR.remove(storage);
        }
        else {
            this.REQUEST_STATE_ATTR.set(storage, (Object)httpTransactionState);
        }
    }
    
    HttpTransactionContext getHttpTransactionContext(final AttributeStorage storage) {
        return (HttpTransactionContext)this.REQUEST_STATE_ATTR.get(storage);
    }
    
    void timeout(final Connection c) {
        final HttpTransactionContext context = this.getHttpTransactionContext((AttributeStorage)c);
        this.setHttpTransactionContext((AttributeStorage)c, null);
        context.abort(new TimeoutException("Timeout exceeded"));
    }
    
    static int getPort(final URI uri, final int p) {
        int port = p;
        if (port == -1) {
            final String protocol = uri.getScheme().toLowerCase();
            if ("http".equals(protocol)) {
                port = 80;
            }
            else {
                if (!"https".equals(protocol)) {
                    throw new IllegalArgumentException("Unknown protocol: " + protocol);
                }
                port = 443;
            }
        }
        return port;
    }
    
    boolean sendRequest(final FilterChainContext ctx, final Request request, final HttpRequestPacket requestPacket) throws IOException {
        boolean isWriteComplete = true;
        if (requestHasEntityBody(request)) {
            final HttpTransactionContext context = this.getHttpTransactionContext((AttributeStorage)ctx.getConnection());
            BodyHandler handler = this.bodyHandlerFactory.getBodyHandler(request);
            if (requestPacket.getHeaders().contains(Header.Expect) && requestPacket.getHeaders().getValue(1).equalsIgnoreCase("100-Continue")) {
                handler = new ExpectHandler(handler);
            }
            context.bodyHandler = handler;
            isWriteComplete = handler.doHandle(ctx, request, requestPacket);
        }
        else {
            ctx.write((Object)requestPacket, ctx.getTransportContext().getCompletionHandler());
        }
        if (GrizzlyAsyncHttpProvider.LOGGER.isDebugEnabled()) {
            GrizzlyAsyncHttpProvider.LOGGER.debug("REQUEST: " + requestPacket.toString());
        }
        return isWriteComplete;
    }
    
    private static boolean requestHasEntityBody(final Request request) {
        final String method = request.getMethod();
        return Method.POST.matchesMethod(method) || Method.PUT.matchesMethod(method) || Method.PATCH.matchesMethod(method) || Method.DELETE.matchesMethod(method);
    }
    
    static {
        LOGGER = LoggerFactory.getLogger((Class)GrizzlyAsyncHttpProvider.class);
        SEND_FILE_SUPPORT = configSendFileSupport();
    }
    
    final class HttpTransactionContext
    {
        final AtomicInteger redirectCount;
        final int maxRedirectCount;
        final boolean redirectsAllowed;
        final GrizzlyAsyncHttpProvider provider;
        Request request;
        String requestUrl;
        AsyncHandler handler;
        BodyHandler bodyHandler;
        StatusHandler statusHandler;
        StatusHandler.InvocationStatus invocationStatus;
        GrizzlyResponseStatus responseStatus;
        GrizzlyResponseFuture future;
        String lastRedirectURI;
        AtomicLong totalBodyWritten;
        AsyncHandler.STATE currentState;
        String wsRequestURI;
        boolean isWSRequest;
        HandShake handshake;
        ProtocolHandler protocolHandler;
        WebSocket webSocket;
        
        HttpTransactionContext(final GrizzlyResponseFuture future, final Request request, final AsyncHandler handler) {
            this.redirectCount = new AtomicInteger(0);
            this.provider = GrizzlyAsyncHttpProvider.this;
            this.invocationStatus = StatusHandler.InvocationStatus.CONTINUE;
            this.totalBodyWritten = new AtomicLong();
            this.future = future;
            this.request = request;
            this.handler = handler;
            this.redirectsAllowed = this.provider.clientConfig.isRedirectEnabled();
            this.maxRedirectCount = this.provider.clientConfig.getMaxRedirects();
            this.requestUrl = request.getUrl();
        }
        
        HttpTransactionContext copy() {
            final HttpTransactionContext newContext = new HttpTransactionContext(this.future, this.request, this.handler);
            newContext.invocationStatus = this.invocationStatus;
            newContext.bodyHandler = this.bodyHandler;
            newContext.currentState = this.currentState;
            newContext.statusHandler = this.statusHandler;
            newContext.lastRedirectURI = this.lastRedirectURI;
            newContext.redirectCount.set(this.redirectCount.get());
            return newContext;
        }
        
        void abort(final Throwable t) {
            if (this.future != null) {
                this.future.abort(t);
            }
        }
        
        void done(final Callable c) {
            if (this.future != null) {
                this.future.done(c);
            }
        }
        
        void result(final Object result) {
            if (this.future != null) {
                this.future.delegate.result(result);
                this.future.done(null);
            }
        }
    }
    
    private static final class ContinueEvent implements FilterChainEvent
    {
        private final HttpTransactionContext context;
        
        ContinueEvent(final HttpTransactionContext context) {
            this.context = context;
        }
        
        public Object type() {
            return ContinueEvent.class;
        }
    }
    
    private final class AsyncHttpClientTransportFilter extends TransportFilter
    {
        public NextAction handleRead(final FilterChainContext ctx) throws IOException {
            final HttpTransactionContext context = GrizzlyAsyncHttpProvider.this.getHttpTransactionContext((AttributeStorage)ctx.getConnection());
            if (context == null) {
                return super.handleRead(ctx);
            }
            ctx.getTransportContext().setCompletionHandler((CompletionHandler)new CompletionHandler() {
                public void cancelled() {
                }
                
                public void failed(final Throwable throwable) {
                    if (throwable instanceof EOFException) {
                        context.abort(new IOException("Remotely Closed"));
                    }
                    context.abort(throwable);
                }
                
                public void completed(final Object result) {
                }
                
                public void updated(final Object result) {
                }
            });
            return super.handleRead(ctx);
        }
    }
    
    private final class AsyncHttpClientFilter extends BaseFilter
    {
        private final AsyncHttpClientConfig config;
        
        AsyncHttpClientFilter(final AsyncHttpClientConfig config) {
            this.config = config;
        }
        
        public NextAction handleWrite(final FilterChainContext ctx) throws IOException {
            final Object message = ctx.getMessage();
            if (message instanceof Request) {
                ctx.setMessage((Object)null);
                if (!this.sendAsGrizzlyRequest((Request)message, ctx)) {
                    return ctx.getSuspendAction();
                }
            }
            else if (message instanceof Buffer) {
                return ctx.getInvokeAction();
            }
            return ctx.getStopAction();
        }
        
        public NextAction handleEvent(final FilterChainContext ctx, final FilterChainEvent event) throws IOException {
            final Object type = event.type();
            if (type == ContinueEvent.class) {
                final ContinueEvent continueEvent = (ContinueEvent)event;
                ((ExpectHandler)continueEvent.context.bodyHandler).finish(ctx);
            }
            return ctx.getStopAction();
        }
        
        private boolean sendAsGrizzlyRequest(final Request request, final FilterChainContext ctx) throws IOException {
            final HttpTransactionContext httpCtx = GrizzlyAsyncHttpProvider.this.getHttpTransactionContext((AttributeStorage)ctx.getConnection());
            if (this.isUpgradeRequest(httpCtx.handler) && this.isWSRequest(httpCtx.requestUrl)) {
                httpCtx.isWSRequest = true;
                this.convertToUpgradeRequest(httpCtx);
            }
            final URI uri = AsyncHttpProviderUtils.createUri(httpCtx.requestUrl);
            final HttpRequestPacket.Builder builder = HttpRequestPacket.builder();
            final boolean secure = "https".equals(uri.getScheme());
            builder.method(request.getMethod());
            builder.protocol(Protocol.HTTP_1_1);
            final String host = request.getVirtualHost();
            if (host != null) {
                builder.header(Header.Host, host);
            }
            else if (uri.getPort() == -1) {
                builder.header(Header.Host, uri.getHost());
            }
            else {
                builder.header(Header.Host, uri.getHost() + ':' + uri.getPort());
            }
            final ProxyServer proxy = this.getProxyServer(request);
            final boolean useProxy = proxy != null;
            if (useProxy) {
                if (secure) {
                    builder.method(Method.CONNECT);
                    builder.uri(AsyncHttpProviderUtils.getAuthority(uri));
                }
                else {
                    builder.uri(uri.toString());
                }
            }
            else {
                builder.uri(uri.getPath());
            }
            if (requestHasEntityBody(request)) {
                final long contentLength = request.getContentLength();
                if (contentLength > 0L) {
                    builder.contentLength(contentLength);
                    builder.chunked(false);
                }
                else {
                    builder.chunked(true);
                }
            }
            HttpRequestPacket requestPacket = null;
            Label_0409: {
                if (httpCtx.isWSRequest) {
                    try {
                        final URI wsURI = new URI(httpCtx.wsRequestURI);
                        httpCtx.protocolHandler = Version.DRAFT17.createHandler(true);
                        httpCtx.handshake = httpCtx.protocolHandler.createHandShake(wsURI);
                        requestPacket = (HttpRequestPacket)httpCtx.handshake.composeHeaders().getHttpHeader();
                        break Label_0409;
                    }
                    catch (URISyntaxException e) {
                        throw new IllegalArgumentException("Invalid WS URI: " + httpCtx.wsRequestURI);
                    }
                }
                requestPacket = builder.build();
            }
            requestPacket.setSecure(true);
            if (!useProxy && !httpCtx.isWSRequest) {
                this.addQueryString(request, requestPacket);
            }
            this.addHeaders(request, requestPacket);
            this.addCookies(request, requestPacket);
            if (useProxy) {
                final boolean avoidProxy = ProxyUtils.avoidProxy(proxy, request);
                if (!avoidProxy) {
                    if (!requestPacket.getHeaders().contains(Header.ProxyConnection)) {
                        requestPacket.setHeader(Header.ProxyConnection, "keep-alive");
                    }
                    if (proxy.getPrincipal() != null) {
                        requestPacket.setHeader(Header.ProxyAuthorization, AuthenticatorUtils.computeBasicAuthentication(proxy));
                    }
                }
            }
            final AsyncHandler h = httpCtx.handler;
            if (h != null && TransferCompletionHandler.class.isAssignableFrom(h.getClass())) {
                final FluentCaseInsensitiveStringsMap map = new FluentCaseInsensitiveStringsMap(request.getHeaders());
                TransferCompletionHandler.class.cast(h).transferAdapter(new GrizzlyTransferAdapter(map));
            }
            return GrizzlyAsyncHttpProvider.this.sendRequest(ctx, request, requestPacket);
        }
        
        private boolean isUpgradeRequest(final AsyncHandler handler) {
            return handler instanceof UpgradeHandler;
        }
        
        private boolean isWSRequest(final String requestUri) {
            return requestUri.charAt(0) == 'w' && requestUri.charAt(1) == 's';
        }
        
        private void convertToUpgradeRequest(final HttpTransactionContext ctx) {
            final int colonIdx = ctx.requestUrl.indexOf(58);
            if (colonIdx < 2 || colonIdx > 3) {
                throw new IllegalArgumentException("Invalid websocket URL: " + ctx.requestUrl);
            }
            final StringBuilder sb = new StringBuilder(ctx.requestUrl);
            sb.replace(0, colonIdx, (colonIdx == 2) ? "http" : "https");
            ctx.wsRequestURI = ctx.requestUrl;
            ctx.requestUrl = sb.toString();
        }
        
        private ProxyServer getProxyServer(final Request request) {
            ProxyServer proxyServer = request.getProxyServer();
            if (proxyServer == null) {
                proxyServer = this.config.getProxyServer();
            }
            return proxyServer;
        }
        
        private void addHeaders(final Request request, final HttpRequestPacket requestPacket) {
            final FluentCaseInsensitiveStringsMap map = request.getHeaders();
            if (map != null && !map.isEmpty()) {
                for (final Map.Entry<String, List<String>> entry : map.entrySet()) {
                    final String headerName = entry.getKey();
                    final List<String> headerValues = entry.getValue();
                    if (headerValues != null && !headerValues.isEmpty()) {
                        for (final String headerValue : headerValues) {
                            requestPacket.addHeader(headerName, headerValue);
                        }
                    }
                }
            }
            final MimeHeaders headers = requestPacket.getHeaders();
            if (!headers.contains(Header.Connection)) {
                requestPacket.addHeader(Header.Connection, "keep-alive");
            }
            if (!headers.contains(Header.Accept)) {
                requestPacket.addHeader(Header.Accept, "*/*");
            }
            if (!headers.contains(Header.UserAgent)) {
                requestPacket.addHeader(Header.UserAgent, this.config.getUserAgent());
            }
        }
        
        private void addCookies(final Request request, final HttpRequestPacket requestPacket) {
            final Collection<com.ning.http.client.Cookie> cookies = request.getCookies();
            if (cookies != null && !cookies.isEmpty()) {
                final StringBuilder sb = new StringBuilder(128);
                final Cookie[] gCookies = new Cookie[cookies.size()];
                this.convertCookies(cookies, gCookies);
                CookieSerializerUtils.serializeClientCookies(sb, gCookies);
                requestPacket.addHeader(Header.Cookie, sb.toString());
            }
        }
        
        private void convertCookies(final Collection<com.ning.http.client.Cookie> cookies, final Cookie[] gCookies) {
            int idx = 0;
            for (final com.ning.http.client.Cookie cookie : cookies) {
                final Cookie gCookie = new Cookie(cookie.getName(), cookie.getValue());
                gCookie.setDomain(cookie.getDomain());
                gCookie.setPath(cookie.getPath());
                gCookie.setVersion(cookie.getVersion());
                gCookie.setMaxAge(cookie.getMaxAge());
                gCookie.setSecure(cookie.isSecure());
                gCookies[idx] = gCookie;
                ++idx;
            }
        }
        
        private void addQueryString(final Request request, final HttpRequestPacket requestPacket) {
            final FluentStringsMap map = request.getQueryParams();
            if (map != null && !map.isEmpty()) {
                final StringBuilder sb = new StringBuilder(128);
                for (final Map.Entry<String, List<String>> entry : map.entrySet()) {
                    final String name = entry.getKey();
                    final List<String> values = entry.getValue();
                    if (values != null && !values.isEmpty()) {
                        try {
                            for (int i = 0, len = values.size(); i < len; ++i) {
                                final String value = values.get(i);
                                if (value != null && value.length() > 0) {
                                    sb.append(URLEncoder.encode(name, "UTF-8")).append('=').append(URLEncoder.encode(values.get(i), "UTF-8")).append('&');
                                }
                                else {
                                    sb.append(URLEncoder.encode(name, "UTF-8")).append('&');
                                }
                            }
                        }
                        catch (UnsupportedEncodingException ex) {}
                    }
                }
                final String queryString = sb.deleteCharAt(sb.length() - 1).toString();
                requestPacket.setQueryString(queryString);
            }
        }
    }
    
    private static final class AsyncHttpClientEventFilter extends HttpClientFilter
    {
        private final Map<Integer, StatusHandler> HANDLER_MAP;
        private final GrizzlyAsyncHttpProvider provider;
        
        AsyncHttpClientEventFilter(final GrizzlyAsyncHttpProvider provider) {
            this.HANDLER_MAP = new HashMap<Integer, StatusHandler>();
            this.provider = provider;
            this.HANDLER_MAP.put(HttpStatus.UNAUTHORIZED_401.getStatusCode(), AuthorizationHandler.INSTANCE);
            this.HANDLER_MAP.put(HttpStatus.MOVED_PERMANENTLY_301.getStatusCode(), RedirectHandler.INSTANCE);
            this.HANDLER_MAP.put(HttpStatus.FOUND_302.getStatusCode(), RedirectHandler.INSTANCE);
            this.HANDLER_MAP.put(HttpStatus.TEMPORARY_REDIRECT_307.getStatusCode(), RedirectHandler.INSTANCE);
        }
        
        public void exceptionOccurred(final FilterChainContext ctx, final Throwable error) {
            this.provider.getHttpTransactionContext((AttributeStorage)ctx.getConnection()).abort(error);
        }
        
        protected void onHttpContentParsed(final HttpContent content, final FilterChainContext ctx) {
            final HttpTransactionContext context = this.provider.getHttpTransactionContext((AttributeStorage)ctx.getConnection());
            final AsyncHandler handler = context.handler;
            if (handler != null && context.currentState != AsyncHandler.STATE.ABORT) {
                try {
                    context.currentState = handler.onBodyPartReceived(new GrizzlyResponseBodyPart(content, null, ctx.getConnection(), this.provider));
                }
                catch (Exception e) {
                    handler.onThrowable(e);
                }
            }
        }
        
        protected void onHttpHeadersEncoded(final HttpHeader httpHeader, final FilterChainContext ctx) {
            final HttpTransactionContext context = this.provider.getHttpTransactionContext((AttributeStorage)ctx.getConnection());
            final AsyncHandler handler = context.handler;
            if (handler != null && TransferCompletionHandler.class.isAssignableFrom(handler.getClass())) {
                ((TransferCompletionHandler)handler).onHeaderWriteCompleted();
            }
        }
        
        protected void onHttpContentEncoded(final HttpContent content, final FilterChainContext ctx) {
            final HttpTransactionContext context = this.provider.getHttpTransactionContext((AttributeStorage)ctx.getConnection());
            final AsyncHandler handler = context.handler;
            if (handler != null && TransferCompletionHandler.class.isAssignableFrom(handler.getClass())) {
                final int written = content.getContent().remaining();
                final long total = context.totalBodyWritten.addAndGet(written);
                ((TransferCompletionHandler)handler).onContentWriteProgress(written, total, content.getHttpHeader().getContentLength());
            }
        }
        
        protected void onInitialLineParsed(final HttpHeader httpHeader, final FilterChainContext ctx) {
            super.onInitialLineParsed(httpHeader, ctx);
            if (httpHeader.isSkipRemainder()) {
                return;
            }
            final HttpTransactionContext context = this.provider.getHttpTransactionContext((AttributeStorage)ctx.getConnection());
            final int status = ((HttpResponsePacket)httpHeader).getStatus();
            if (HttpStatus.CONINTUE_100.statusMatches(status)) {
                ctx.notifyUpstream((FilterChainEvent)new ContinueEvent(context));
                return;
            }
            if (context.statusHandler != null && !context.statusHandler.handlesStatus(status)) {
                context.statusHandler = null;
                context.invocationStatus = StatusHandler.InvocationStatus.CONTINUE;
            }
            else {
                context.statusHandler = null;
            }
            if (context.invocationStatus == StatusHandler.InvocationStatus.CONTINUE) {
                if (this.HANDLER_MAP.containsKey(status)) {
                    context.statusHandler = this.HANDLER_MAP.get(status);
                }
                if (context.statusHandler instanceof RedirectHandler && !isRedirectAllowed(context)) {
                    context.statusHandler = null;
                }
            }
            if (isRedirectAllowed(context)) {
                if (isRedirect(status)) {
                    if (context.statusHandler == null) {
                        context.statusHandler = RedirectHandler.INSTANCE;
                    }
                    context.redirectCount.incrementAndGet();
                    if (redirectCountExceeded(context)) {
                        httpHeader.setSkipRemainder(true);
                        context.abort(new MaxRedirectException());
                    }
                }
                else if (context.redirectCount.get() > 0) {
                    context.redirectCount.set(0);
                }
            }
            final GrizzlyResponseStatus responseStatus = new GrizzlyResponseStatus((HttpResponsePacket)httpHeader, getURI(context.requestUrl), this.provider);
            context.responseStatus = responseStatus;
            if (context.statusHandler != null) {
                return;
            }
            if (context.currentState != AsyncHandler.STATE.ABORT) {
                try {
                    final AsyncHandler handler = context.handler;
                    if (handler != null) {
                        context.currentState = handler.onStatusReceived(responseStatus);
                    }
                }
                catch (Exception e) {
                    httpHeader.setSkipRemainder(true);
                    context.abort(e);
                }
            }
        }
        
        protected void onHttpHeaderError(final HttpHeader httpHeader, final FilterChainContext ctx, final Throwable t) throws IOException {
            t.printStackTrace();
            httpHeader.setSkipRemainder(true);
            final HttpTransactionContext context = this.provider.getHttpTransactionContext((AttributeStorage)ctx.getConnection());
            context.abort(t);
        }
        
        protected void onHttpHeadersParsed(final HttpHeader httpHeader, final FilterChainContext ctx) {
            super.onHttpHeadersParsed(httpHeader, ctx);
            if (GrizzlyAsyncHttpProvider.LOGGER.isDebugEnabled()) {
                GrizzlyAsyncHttpProvider.LOGGER.debug("RESPONSE: " + httpHeader.toString());
            }
            if (httpHeader.containsHeader(Header.Connection) && "close".equals(httpHeader.getHeader(Header.Connection))) {
                ConnectionManager.markConnectionAsDoNotCache(ctx.getConnection());
            }
            if (httpHeader.isSkipRemainder()) {
                return;
            }
            final HttpTransactionContext context = this.provider.getHttpTransactionContext((AttributeStorage)ctx.getConnection());
            final AsyncHandler handler = context.handler;
            final List<ResponseFilter> filters = context.provider.clientConfig.getResponseFilters();
            final GrizzlyResponseHeaders responseHeaders = new GrizzlyResponseHeaders((HttpResponsePacket)httpHeader, null, this.provider);
            if (!filters.isEmpty()) {
                FilterContext fc = new FilterContext.FilterContextBuilder().asyncHandler(handler).request(context.request).responseHeaders(responseHeaders).responseStatus(context.responseStatus).build();
                try {
                    for (final ResponseFilter f : filters) {
                        fc = f.filter(fc);
                    }
                }
                catch (Exception e) {
                    context.abort(e);
                }
                if (fc.replayRequest()) {
                    httpHeader.setSkipRemainder(true);
                    final Request newRequest = fc.getRequest();
                    final AsyncHandler newHandler = fc.getAsyncHandler();
                    try {
                        final ConnectionManager m = context.provider.connectionManager;
                        final Connection c = m.obtainConnection(newRequest, context.future);
                        final HttpTransactionContext newContext = context.copy();
                        context.future = null;
                        this.provider.setHttpTransactionContext((AttributeStorage)c, newContext);
                        try {
                            context.provider.execute(c, newRequest, newHandler, (GrizzlyResponseFuture<Object>)context.future);
                        }
                        catch (IOException ioe) {
                            newContext.abort(ioe);
                        }
                    }
                    catch (Exception e2) {
                        context.abort(e2);
                    }
                    return;
                }
            }
            if (context.statusHandler != null && context.invocationStatus == StatusHandler.InvocationStatus.CONTINUE) {
                final boolean result = context.statusHandler.handleStatus((HttpResponsePacket)httpHeader, context, ctx);
                if (!result) {
                    httpHeader.setSkipRemainder(true);
                    return;
                }
            }
            if (context.currentState != AsyncHandler.STATE.ABORT) {
                final boolean upgrade = context.currentState == AsyncHandler.STATE.UPGRADE;
                try {
                    context.currentState = handler.onHeadersReceived(responseHeaders);
                }
                catch (Exception e) {
                    httpHeader.setSkipRemainder(true);
                    context.abort(e);
                    return;
                }
                if (upgrade) {
                    try {
                        httpHeader.setChunked(false);
                        context.protocolHandler.setConnection(ctx.getConnection());
                        final DefaultWebSocket ws = new DefaultWebSocket(context.protocolHandler, new WebSocketListener[0]);
                        ws.onConnect();
                        context.webSocket = new GrizzlyWebSocketAdapter((org.glassfish.grizzly.websockets.WebSocket)ws);
                        WebSocketEngine.getEngine().setWebSocketHolder(ctx.getConnection(), context.protocolHandler, (org.glassfish.grizzly.websockets.WebSocket)ws);
                        ((WebSocketUpgradeHandler)context.handler).onSuccess(context.webSocket);
                        final int wsTimeout = context.provider.clientConfig.getWebSocketIdleTimeoutInMs();
                        IdleTimeoutFilter.setCustomTimeout(ctx.getConnection(), (wsTimeout <= 0) ? ((long)IdleTimeoutFilter.FOREVER) : ((long)wsTimeout), TimeUnit.MILLISECONDS);
                        context.result(handler.onCompleted());
                    }
                    catch (Exception e) {
                        httpHeader.setSkipRemainder(true);
                        context.abort(e);
                    }
                }
            }
        }
        
        protected boolean onHttpPacketParsed(final HttpHeader httpHeader, final FilterChainContext ctx) {
            if (httpHeader.isSkipRemainder()) {
                this.clearResponse(ctx.getConnection());
                cleanup(ctx, this.provider);
                return false;
            }
            final boolean result = super.onHttpPacketParsed(httpHeader, ctx);
            final HttpTransactionContext context = cleanup(ctx, this.provider);
            final AsyncHandler handler = context.handler;
            if (handler != null) {
                try {
                    context.result(handler.onCompleted());
                }
                catch (Exception e) {
                    context.abort(e);
                }
            }
            else {
                context.done(null);
            }
            return result;
        }
        
        private static boolean isRedirectAllowed(final HttpTransactionContext ctx) {
            boolean allowed = ctx.request.isRedirectEnabled();
            if (ctx.request.isRedirectOverrideSet()) {
                return allowed;
            }
            if (!allowed) {
                allowed = ctx.redirectsAllowed;
            }
            return allowed;
        }
        
        private static HttpTransactionContext cleanup(final FilterChainContext ctx, final GrizzlyAsyncHttpProvider provider) {
            final Connection c = ctx.getConnection();
            final HttpTransactionContext context = provider.getHttpTransactionContext((AttributeStorage)c);
            context.provider.setHttpTransactionContext((AttributeStorage)c, null);
            if (!context.provider.connectionManager.canReturnConnection(c)) {
                context.abort(new IOException("Maximum pooled connections exceeded"));
            }
            else if (!context.provider.connectionManager.returnConnection(context.requestUrl, c)) {
                ctx.getConnection().close().markForRecycle(true);
            }
            return context;
        }
        
        private static URI getURI(final String url) {
            return AsyncHttpProviderUtils.createUri(url);
        }
        
        private static boolean redirectCountExceeded(final HttpTransactionContext context) {
            return context.redirectCount.get() > context.maxRedirectCount;
        }
        
        private static boolean isRedirect(final int status) {
            return HttpStatus.MOVED_PERMANENTLY_301.statusMatches(status) || HttpStatus.FOUND_302.statusMatches(status) || HttpStatus.SEE_OTHER_303.statusMatches(status) || HttpStatus.TEMPORARY_REDIRECT_307.statusMatches(status);
        }
        
        private static Request newRequest(final URI uri, final HttpResponsePacket response, final HttpTransactionContext ctx, final boolean asGet) {
            final RequestBuilder builder = new RequestBuilder(ctx.request);
            if (asGet) {
                builder.setMethod("GET");
            }
            builder.setUrl(uri.toString());
            if (ctx.provider.clientConfig.isRemoveQueryParamOnRedirect()) {
                builder.setQueryParameters(null);
            }
            for (final String cookieStr : response.getHeaders().values(Header.Cookie)) {
                final com.ning.http.client.Cookie c = AsyncHttpProviderUtils.parseCookie(cookieStr);
                builder.addOrReplaceCookie(c);
            }
            return builder.build();
        }
        
        private static final class AuthorizationHandler implements StatusHandler
        {
            private static final AuthorizationHandler INSTANCE;
            
            public boolean handlesStatus(final int statusCode) {
                return HttpStatus.UNAUTHORIZED_401.statusMatches(statusCode);
            }
            
            public boolean handleStatus(final HttpResponsePacket responsePacket, final HttpTransactionContext httpTransactionContext, final FilterChainContext ctx) {
                final String auth = responsePacket.getHeader(Header.WWWAuthenticate);
                if (auth == null) {
                    throw new IllegalStateException("401 response received, but no WWW-Authenticate header was present");
                }
                Realm realm = httpTransactionContext.request.getRealm();
                if (realm == null) {
                    realm = httpTransactionContext.provider.clientConfig.getRealm();
                }
                if (realm == null) {
                    httpTransactionContext.invocationStatus = InvocationStatus.STOP;
                    return true;
                }
                responsePacket.setSkipRemainder(true);
                final Request req = httpTransactionContext.request;
                realm = new Realm.RealmBuilder().clone(realm).setScheme(realm.getAuthScheme()).setUri(URI.create(httpTransactionContext.requestUrl).getPath()).setMethodName(req.getMethod()).setUsePreemptiveAuth(true).parseWWWAuthenticateHeader(auth).build();
                Label_0319: {
                    if (!auth.toLowerCase().startsWith("basic")) {
                        if (auth.toLowerCase().startsWith("digest")) {
                            req.getHeaders().remove((Object)Header.Authorization.toString());
                            try {
                                req.getHeaders().add(Header.Authorization.toString(), AuthenticatorUtils.computeDigestAuthentication(realm));
                                break Label_0319;
                            }
                            catch (NoSuchAlgorithmException e) {
                                throw new IllegalStateException("Digest authentication not supported", e);
                            }
                            catch (UnsupportedEncodingException e2) {
                                throw new IllegalStateException("Unsupported encoding.", e2);
                            }
                        }
                        throw new IllegalStateException("Unsupported authorization method: " + auth);
                    }
                    req.getHeaders().remove((Object)Header.Authorization.toString());
                    try {
                        req.getHeaders().add(Header.Authorization.toString(), AuthenticatorUtils.computeBasicAuthentication(realm));
                    }
                    catch (UnsupportedEncodingException ignored) {}
                }
                final ConnectionManager m = httpTransactionContext.provider.connectionManager;
                try {
                    final Connection c = m.obtainConnection(req, httpTransactionContext.future);
                    final HttpTransactionContext newContext = httpTransactionContext.copy();
                    httpTransactionContext.future = null;
                    httpTransactionContext.provider.setHttpTransactionContext((AttributeStorage)c, newContext);
                    newContext.invocationStatus = InvocationStatus.STOP;
                    try {
                        httpTransactionContext.provider.execute(c, req, httpTransactionContext.handler, (GrizzlyResponseFuture<Object>)httpTransactionContext.future);
                        return false;
                    }
                    catch (IOException ioe) {
                        newContext.abort(ioe);
                        return false;
                    }
                }
                catch (Exception e3) {
                    httpTransactionContext.abort(e3);
                    httpTransactionContext.invocationStatus = InvocationStatus.STOP;
                    return false;
                }
            }
            
            static {
                INSTANCE = new AuthorizationHandler();
            }
        }
        
        private static final class RedirectHandler implements StatusHandler
        {
            private static final RedirectHandler INSTANCE;
            
            public boolean handlesStatus(final int statusCode) {
                return isRedirect(statusCode);
            }
            
            public boolean handleStatus(final HttpResponsePacket responsePacket, final HttpTransactionContext httpTransactionContext, final FilterChainContext ctx) {
                final String redirectURL = responsePacket.getHeader(Header.Location);
                if (redirectURL == null) {
                    throw new IllegalStateException("redirect received, but no location header was present");
                }
                URI orig;
                if (httpTransactionContext.lastRedirectURI == null) {
                    orig = AsyncHttpProviderUtils.createUri(httpTransactionContext.requestUrl);
                }
                else {
                    orig = AsyncHttpProviderUtils.getRedirectUri(AsyncHttpProviderUtils.createUri(httpTransactionContext.requestUrl), httpTransactionContext.lastRedirectURI);
                }
                httpTransactionContext.lastRedirectURI = redirectURL;
                final URI uri = AsyncHttpProviderUtils.getRedirectUri(orig, redirectURL);
                if (uri.toString().equalsIgnoreCase(orig.toString())) {
                    httpTransactionContext.statusHandler = null;
                    httpTransactionContext.invocationStatus = InvocationStatus.CONTINUE;
                    try {
                        httpTransactionContext.handler.onStatusReceived(httpTransactionContext.responseStatus);
                    }
                    catch (Exception e) {
                        httpTransactionContext.abort(e);
                    }
                    return true;
                }
                final Request requestToSend = newRequest(uri, responsePacket, httpTransactionContext, this.sendAsGet(responsePacket, httpTransactionContext));
                final ConnectionManager m = httpTransactionContext.provider.connectionManager;
                try {
                    final Connection c = m.obtainConnection(requestToSend, httpTransactionContext.future);
                    if (this.switchingSchemes(orig, uri)) {
                        try {
                            this.notifySchemeSwitch(ctx, c, uri);
                        }
                        catch (IOException ioe) {
                            httpTransactionContext.abort(ioe);
                        }
                    }
                    final HttpTransactionContext newContext = httpTransactionContext.copy();
                    httpTransactionContext.future = null;
                    newContext.invocationStatus = InvocationStatus.CONTINUE;
                    newContext.request = requestToSend;
                    newContext.requestUrl = requestToSend.getUrl();
                    httpTransactionContext.provider.setHttpTransactionContext((AttributeStorage)c, newContext);
                    httpTransactionContext.provider.execute(c, requestToSend, newContext.handler, (GrizzlyResponseFuture<Object>)newContext.future);
                    return false;
                }
                catch (Exception e2) {
                    httpTransactionContext.abort(e2);
                    httpTransactionContext.invocationStatus = InvocationStatus.CONTINUE;
                    return true;
                }
            }
            
            private boolean sendAsGet(final HttpResponsePacket response, final HttpTransactionContext ctx) {
                final int statusCode = response.getStatus();
                return statusCode >= 302 && statusCode <= 303 && (statusCode != 302 || !ctx.provider.clientConfig.isStrict302Handling());
            }
            
            private boolean switchingSchemes(final URI oldUri, final URI newUri) {
                return !oldUri.getScheme().equals(newUri.getScheme());
            }
            
            private void notifySchemeSwitch(final FilterChainContext ctx, final Connection c, final URI uri) throws IOException {
                ctx.notifyDownstream((FilterChainEvent)new SwitchingSSLFilter.SSLSwitchingEvent("https".equals(uri.getScheme()), c));
            }
            
            static {
                INSTANCE = new RedirectHandler();
            }
        }
    }
    
    private static final class ClientEncodingFilter implements EncodingFilter
    {
        public boolean applyEncoding(final HttpHeader httpPacket) {
            httpPacket.addHeader(Header.AcceptEncoding, "gzip");
            return true;
        }
        
        public boolean applyDecoding(final HttpHeader httpPacket) {
            final HttpResponsePacket httpResponse = (HttpResponsePacket)httpPacket;
            final DataChunk bc = httpResponse.getHeaders().getValue(Header.ContentEncoding);
            return bc != null && bc.indexOf("gzip", 0) != -1;
        }
    }
    
    private static final class NonCachingPool implements ConnectionsPool<String, Connection>
    {
        public boolean offer(final String uri, final Connection connection) {
            return false;
        }
        
        public Connection poll(final String uri) {
            return null;
        }
        
        public boolean removeAll(final Connection connection) {
            return false;
        }
        
        public boolean canCacheConnection() {
            return true;
        }
        
        public void destroy() {
        }
    }
    
    private final class BodyHandlerFactory
    {
        private final BodyHandler[] HANDLERS;
        
        private BodyHandlerFactory() {
            this.HANDLERS = new BodyHandler[] { new StringBodyHandler(), new ByteArrayBodyHandler(), new ParamsBodyHandler(), new EntityWriterBodyHandler(), new StreamDataBodyHandler(), new PartsBodyHandler(), new FileBodyHandler(), new BodyGeneratorBodyHandler() };
        }
        
        public BodyHandler getBodyHandler(final Request request) {
            for (final BodyHandler h : this.HANDLERS) {
                if (h.handlesBodyType(request)) {
                    return h;
                }
            }
            return new NoBodyHandler();
        }
    }
    
    private static final class ExpectHandler implements BodyHandler
    {
        private final BodyHandler delegate;
        private Request request;
        private HttpRequestPacket requestPacket;
        
        private ExpectHandler(final BodyHandler delegate) {
            this.delegate = delegate;
        }
        
        public boolean handlesBodyType(final Request request) {
            return this.delegate.handlesBodyType(request);
        }
        
        public boolean doHandle(final FilterChainContext ctx, final Request request, final HttpRequestPacket requestPacket) throws IOException {
            this.request = request;
            ctx.write((Object)(this.requestPacket = requestPacket), requestPacket.isCommitted() ? null : ctx.getTransportContext().getCompletionHandler());
            return true;
        }
        
        public void finish(final FilterChainContext ctx) throws IOException {
            this.delegate.doHandle(ctx, this.request, this.requestPacket);
        }
    }
    
    private final class ByteArrayBodyHandler implements BodyHandler
    {
        public boolean handlesBodyType(final Request request) {
            return request.getByteData() != null;
        }
        
        public boolean doHandle(final FilterChainContext ctx, final Request request, final HttpRequestPacket requestPacket) throws IOException {
            String charset = request.getBodyEncoding();
            if (charset == null) {
                charset = Charsets.DEFAULT_CHARACTER_ENCODING;
            }
            final byte[] data = new String(request.getByteData(), charset).getBytes(charset);
            final MemoryManager mm = ctx.getMemoryManager();
            final Buffer gBuffer = Buffers.wrap(mm, data);
            if (requestPacket.getContentLength() == -1L && !GrizzlyAsyncHttpProvider.this.clientConfig.isCompressionEnabled()) {
                requestPacket.setContentLengthLong((long)data.length);
            }
            final HttpContent content = requestPacket.httpContentBuilder().content(gBuffer).build();
            content.setLast(true);
            ctx.write((Object)content, requestPacket.isCommitted() ? null : ctx.getTransportContext().getCompletionHandler());
            return true;
        }
    }
    
    private final class StringBodyHandler implements BodyHandler
    {
        public boolean handlesBodyType(final Request request) {
            return request.getStringData() != null;
        }
        
        public boolean doHandle(final FilterChainContext ctx, final Request request, final HttpRequestPacket requestPacket) throws IOException {
            String charset = request.getBodyEncoding();
            if (charset == null) {
                charset = Charsets.DEFAULT_CHARACTER_ENCODING;
            }
            final byte[] data = request.getStringData().getBytes(charset);
            final MemoryManager mm = ctx.getMemoryManager();
            final Buffer gBuffer = Buffers.wrap(mm, data);
            if (requestPacket.getContentLength() == -1L && !GrizzlyAsyncHttpProvider.this.clientConfig.isCompressionEnabled()) {
                requestPacket.setContentLengthLong((long)data.length);
            }
            final HttpContent content = requestPacket.httpContentBuilder().content(gBuffer).build();
            content.setLast(true);
            ctx.write((Object)content, requestPacket.isCommitted() ? null : ctx.getTransportContext().getCompletionHandler());
            return true;
        }
    }
    
    private static final class NoBodyHandler implements BodyHandler
    {
        public boolean handlesBodyType(final Request request) {
            return false;
        }
        
        public boolean doHandle(final FilterChainContext ctx, final Request request, final HttpRequestPacket requestPacket) throws IOException {
            final HttpContent content = requestPacket.httpContentBuilder().content(Buffers.EMPTY_BUFFER).build();
            content.setLast(true);
            ctx.write((Object)content, requestPacket.isCommitted() ? null : ctx.getTransportContext().getCompletionHandler());
            return true;
        }
    }
    
    private final class ParamsBodyHandler implements BodyHandler
    {
        public boolean handlesBodyType(final Request request) {
            final FluentStringsMap params = request.getParams();
            return params != null && !params.isEmpty();
        }
        
        public boolean doHandle(final FilterChainContext ctx, final Request request, final HttpRequestPacket requestPacket) throws IOException {
            if (requestPacket.getContentType() == null) {
                requestPacket.setContentType("application/x-www-form-urlencoded");
            }
            StringBuilder sb = null;
            String charset = request.getBodyEncoding();
            if (charset == null) {
                charset = Charsets.DEFAULT_CHARACTER_ENCODING;
            }
            final FluentStringsMap params = request.getParams();
            if (!params.isEmpty()) {
                for (final Map.Entry<String, List<String>> entry : params.entrySet()) {
                    final String name = entry.getKey();
                    final List<String> values = entry.getValue();
                    if (values != null && !values.isEmpty()) {
                        if (sb == null) {
                            sb = new StringBuilder(128);
                        }
                        for (final String value : values) {
                            if (sb.length() > 0) {
                                sb.append('&');
                            }
                            sb.append(URLEncoder.encode(name, charset)).append('=').append(URLEncoder.encode(value, charset));
                        }
                    }
                }
            }
            if (sb != null) {
                final byte[] data = sb.toString().getBytes(charset);
                final MemoryManager mm = ctx.getMemoryManager();
                final Buffer gBuffer = Buffers.wrap(mm, data);
                final HttpContent content = requestPacket.httpContentBuilder().content(gBuffer).build();
                if (requestPacket.getContentLength() == -1L && !GrizzlyAsyncHttpProvider.this.clientConfig.isCompressionEnabled()) {
                    requestPacket.setContentLengthLong((long)data.length);
                }
                content.setLast(true);
                ctx.write((Object)content, requestPacket.isCommitted() ? null : ctx.getTransportContext().getCompletionHandler());
            }
            return true;
        }
    }
    
    private static final class EntityWriterBodyHandler implements BodyHandler
    {
        public boolean handlesBodyType(final Request request) {
            return request.getEntityWriter() != null;
        }
        
        public boolean doHandle(final FilterChainContext ctx, final Request request, final HttpRequestPacket requestPacket) throws IOException {
            final MemoryManager mm = ctx.getMemoryManager();
            Buffer b = mm.allocate(512);
            final BufferOutputStream o = new BufferOutputStream(mm, b, true);
            final Request.EntityWriter writer = request.getEntityWriter();
            writer.writeEntity((OutputStream)o);
            b = o.getBuffer();
            b.trim();
            if (b.hasRemaining()) {
                final HttpContent content = requestPacket.httpContentBuilder().content(b).build();
                content.setLast(true);
                ctx.write((Object)content, requestPacket.isCommitted() ? null : ctx.getTransportContext().getCompletionHandler());
            }
            return true;
        }
    }
    
    private static final class StreamDataBodyHandler implements BodyHandler
    {
        public boolean handlesBodyType(final Request request) {
            return request.getStreamData() != null;
        }
        
        public boolean doHandle(final FilterChainContext ctx, final Request request, final HttpRequestPacket requestPacket) throws IOException {
            final MemoryManager mm = ctx.getMemoryManager();
            Buffer buffer = mm.allocate(512);
            final byte[] b = new byte[512];
            final InputStream in = request.getStreamData();
            try {
                in.reset();
            }
            catch (IOException ioe) {
                if (GrizzlyAsyncHttpProvider.LOGGER.isDebugEnabled()) {
                    GrizzlyAsyncHttpProvider.LOGGER.debug(ioe.toString(), (Throwable)ioe);
                }
            }
            if (in.markSupported()) {
                in.mark(0);
            }
            int read;
            while ((read = in.read(b)) != -1) {
                if (read > buffer.remaining()) {
                    buffer = mm.reallocate(buffer, buffer.capacity() + 512);
                }
                buffer.put(b, 0, read);
            }
            buffer.trim();
            if (buffer.hasRemaining()) {
                final HttpContent content = requestPacket.httpContentBuilder().content(buffer).build();
                buffer.allowBufferDispose(false);
                content.setLast(true);
                ctx.write((Object)content, requestPacket.isCommitted() ? null : ctx.getTransportContext().getCompletionHandler());
            }
            return true;
        }
    }
    
    private static final class PartsBodyHandler implements BodyHandler
    {
        public boolean handlesBodyType(final Request request) {
            final List<Part> parts = request.getParts();
            return parts != null && !parts.isEmpty();
        }
        
        public boolean doHandle(final FilterChainContext ctx, final Request request, final HttpRequestPacket requestPacket) throws IOException {
            final MultipartRequestEntity mre = AsyncHttpProviderUtils.createMultipartRequestEntity(request.getParts(), request.getParams());
            requestPacket.setContentLengthLong(mre.getContentLength());
            requestPacket.setContentType(mre.getContentType());
            final MemoryManager mm = ctx.getMemoryManager();
            Buffer b = mm.allocate(512);
            final BufferOutputStream o = new BufferOutputStream(mm, b, true);
            mre.writeRequest((OutputStream)o);
            b = o.getBuffer();
            b.trim();
            if (b.hasRemaining()) {
                final HttpContent content = requestPacket.httpContentBuilder().content(b).build();
                content.setLast(true);
                ctx.write((Object)content, requestPacket.isCommitted() ? null : ctx.getTransportContext().getCompletionHandler());
            }
            return true;
        }
    }
    
    private final class FileBodyHandler implements BodyHandler
    {
        public boolean handlesBodyType(final Request request) {
            return request.getFile() != null;
        }
        
        public boolean doHandle(final FilterChainContext ctx, final Request request, final HttpRequestPacket requestPacket) throws IOException {
            final File f = request.getFile();
            requestPacket.setContentLengthLong(f.length());
            final HttpTransactionContext context = GrizzlyAsyncHttpProvider.this.getHttpTransactionContext((AttributeStorage)ctx.getConnection());
            if (!GrizzlyAsyncHttpProvider.SEND_FILE_SUPPORT || requestPacket.isSecure()) {
                final FileInputStream fis = new FileInputStream(request.getFile());
                final MemoryManager mm = ctx.getMemoryManager();
                final AtomicInteger written = new AtomicInteger();
                boolean last = false;
                try {
                    final byte[] buf = new byte[8192];
                    while (!last) {
                        Buffer b = null;
                        final int read;
                        if ((read = fis.read(buf)) < 0) {
                            last = true;
                            b = Buffers.EMPTY_BUFFER;
                        }
                        if (b != Buffers.EMPTY_BUFFER) {
                            written.addAndGet(read);
                            b = Buffers.wrap(mm, buf, 0, read);
                        }
                        final HttpContent content = requestPacket.httpContentBuilder().content(b).last(last).build();
                        ctx.write((Object)content, requestPacket.isCommitted() ? null : ctx.getTransportContext().getCompletionHandler());
                    }
                }
                finally {
                    try {
                        fis.close();
                    }
                    catch (IOException ex) {}
                }
            }
            else {
                ctx.write((Object)requestPacket, requestPacket.isCommitted() ? null : ctx.getTransportContext().getCompletionHandler());
                ctx.write((Object)new FileTransfer(f), (CompletionHandler)new EmptyCompletionHandler<WriteResult>() {
                    public void updated(final WriteResult result) {
                        final AsyncHandler handler = context.handler;
                        if (handler != null && TransferCompletionHandler.class.isAssignableFrom(handler.getClass())) {
                            final long written = result.getWrittenSize();
                            final long total = context.totalBodyWritten.addAndGet(written);
                            ((TransferCompletionHandler)handler).onContentWriteProgress(written, total, requestPacket.getContentLength());
                        }
                    }
                });
            }
            return true;
        }
    }
    
    private static final class BodyGeneratorBodyHandler implements BodyHandler
    {
        public boolean handlesBodyType(final Request request) {
            return request.getBodyGenerator() != null;
        }
        
        public boolean doHandle(final FilterChainContext ctx, final Request request, final HttpRequestPacket requestPacket) throws IOException {
            final BodyGenerator generator = request.getBodyGenerator();
            final Body bodyLocal = generator.createBody();
            final long len = bodyLocal.getContentLength();
            if (len > 0L) {
                requestPacket.setContentLengthLong(len);
            }
            else {
                requestPacket.setChunked(true);
            }
            final MemoryManager mm = ctx.getMemoryManager();
            boolean last = false;
            while (!last) {
                Buffer buffer = mm.allocate(8192);
                buffer.allowBufferDispose(true);
                final long readBytes = bodyLocal.read(buffer.toByteBuffer());
                if (readBytes > 0L) {
                    buffer.position((int)readBytes);
                    buffer.trim();
                }
                else {
                    buffer.dispose();
                    if (readBytes < 0L) {
                        last = true;
                        buffer = Buffers.EMPTY_BUFFER;
                    }
                    else {
                        if (generator instanceof FeedableBodyGenerator) {
                            ((FeedableBodyGenerator)generator).initializeAsynchronousTransfer(ctx, requestPacket);
                            return false;
                        }
                        throw new IllegalStateException("BodyGenerator unexpectedly returned 0 bytes available");
                    }
                }
                final HttpContent content = requestPacket.httpContentBuilder().content(buffer).last(last).build();
                ctx.write((Object)content, requestPacket.isCommitted() ? null : ctx.getTransportContext().getCompletionHandler());
            }
            return true;
        }
    }
    
    static class ConnectionManager
    {
        private static final Attribute<Boolean> DO_NOT_CACHE;
        private final ConnectionsPool<String, Connection> pool;
        private final TCPNIOConnectorHandler connectionHandler;
        private final ConnectionMonitor connectionMonitor;
        private final GrizzlyAsyncHttpProvider provider;
        
        ConnectionManager(final GrizzlyAsyncHttpProvider provider, final TCPNIOTransport transport) {
            this.provider = provider;
            final AsyncHttpClientConfig config = provider.clientConfig;
            ConnectionsPool<String, Connection> connectionPool;
            if (config.getAllowPoolingConnection()) {
                final ConnectionsPool pool = config.getConnectionsPool();
                if (pool != null) {
                    connectionPool = (ConnectionsPool<String, Connection>)pool;
                }
                else {
                    connectionPool = new GrizzlyConnectionsPool(config);
                }
            }
            else {
                connectionPool = new NonCachingPool();
            }
            this.pool = connectionPool;
            this.connectionHandler = TCPNIOConnectorHandler.builder(transport).build();
            final int maxConns = provider.clientConfig.getMaxTotalConnections();
            this.connectionMonitor = new ConnectionMonitor(maxConns);
        }
        
        static void markConnectionAsDoNotCache(final Connection c) {
            ConnectionManager.DO_NOT_CACHE.set((AttributeStorage)c, (Object)Boolean.TRUE);
        }
        
        static boolean isConnectionCacheable(final Connection c) {
            final Boolean canCache = (Boolean)ConnectionManager.DO_NOT_CACHE.get((AttributeStorage)c);
            return canCache != null && canCache;
        }
        
        void doAsyncTrackedConnection(final Request request, final GrizzlyResponseFuture requestFuture, final CompletionHandler<Connection> connectHandler) throws IOException, ExecutionException, InterruptedException {
            final String url = request.getUrl();
            final Connection c = this.pool.poll(AsyncHttpProviderUtils.getBaseUrl(url));
            if (c == null) {
                if (!this.connectionMonitor.acquire()) {
                    throw new IOException("Max connections exceeded");
                }
                this.doAsyncConnect(url, request, requestFuture, connectHandler);
            }
            else {
                this.provider.touchConnection(c, request);
                connectHandler.completed((Object)c);
            }
        }
        
        Connection obtainConnection(final Request request, final GrizzlyResponseFuture requestFuture) throws IOException, ExecutionException, InterruptedException, TimeoutException {
            final Connection c = this.obtainConnection0(request.getUrl(), request, requestFuture);
            ConnectionManager.DO_NOT_CACHE.set((AttributeStorage)c, (Object)Boolean.TRUE);
            return c;
        }
        
        void doAsyncConnect(final String url, final Request request, final GrizzlyResponseFuture requestFuture, final CompletionHandler<Connection> connectHandler) throws IOException, ExecutionException, InterruptedException {
            final URI uri = AsyncHttpProviderUtils.createUri(url);
            ProxyServer proxy = this.getProxyServer(request);
            if (ProxyUtils.avoidProxy(proxy, request)) {
                proxy = null;
            }
            final String host = (proxy != null) ? proxy.getHost() : uri.getHost();
            final int port = (proxy != null) ? proxy.getPort() : uri.getPort();
            if (request.getLocalAddress() != null) {
                this.connectionHandler.connect((SocketAddress)new InetSocketAddress(host, GrizzlyAsyncHttpProvider.getPort(uri, port)), (SocketAddress)new InetSocketAddress(request.getLocalAddress(), 0), (CompletionHandler)this.createConnectionCompletionHandler(request, requestFuture, connectHandler));
            }
            else {
                this.connectionHandler.connect((SocketAddress)new InetSocketAddress(host, GrizzlyAsyncHttpProvider.getPort(uri, port)), (CompletionHandler)this.createConnectionCompletionHandler(request, requestFuture, connectHandler));
            }
        }
        
        private Connection obtainConnection0(final String url, final Request request, final GrizzlyResponseFuture requestFuture) throws IOException, ExecutionException, InterruptedException, TimeoutException {
            final URI uri = AsyncHttpProviderUtils.createUri(url);
            ProxyServer proxy = this.getProxyServer(request);
            if (ProxyUtils.avoidProxy(proxy, request)) {
                proxy = null;
            }
            final String host = (proxy != null) ? proxy.getHost() : uri.getHost();
            final int port = (proxy != null) ? proxy.getPort() : uri.getPort();
            final int cTimeout = this.provider.clientConfig.getConnectionTimeoutInMs();
            final FutureImpl<Connection> future = (FutureImpl<Connection>)Futures.createSafeFuture();
            final CompletionHandler<Connection> ch = (CompletionHandler<Connection>)Futures.toCompletionHandler((FutureImpl)future, (CompletionHandler)this.createConnectionCompletionHandler(request, requestFuture, null));
            if (cTimeout > 0) {
                this.connectionHandler.connect((SocketAddress)new InetSocketAddress(host, GrizzlyAsyncHttpProvider.getPort(uri, port)), (CompletionHandler)ch);
                return (Connection)future.get((long)cTimeout, TimeUnit.MILLISECONDS);
            }
            this.connectionHandler.connect((SocketAddress)new InetSocketAddress(host, GrizzlyAsyncHttpProvider.getPort(uri, port)), (CompletionHandler)ch);
            return (Connection)future.get();
        }
        
        private ProxyServer getProxyServer(final Request request) {
            ProxyServer proxyServer = request.getProxyServer();
            if (proxyServer == null) {
                proxyServer = this.provider.clientConfig.getProxyServer();
            }
            return proxyServer;
        }
        
        boolean returnConnection(final String url, final Connection c) {
            final boolean result = ConnectionManager.DO_NOT_CACHE.get((AttributeStorage)c) == null && this.pool.offer(AsyncHttpProviderUtils.getBaseUrl(url), c);
            if (result && this.provider.resolver != null) {
                this.provider.resolver.setTimeoutMillis((Object)c, (long)IdleTimeoutFilter.FOREVER);
            }
            return result;
        }
        
        boolean canReturnConnection(final Connection c) {
            return ConnectionManager.DO_NOT_CACHE.get((AttributeStorage)c) != null || this.pool.canCacheConnection();
        }
        
        void destroy() {
            this.pool.destroy();
        }
        
        CompletionHandler<Connection> createConnectionCompletionHandler(final Request request, final GrizzlyResponseFuture future, final CompletionHandler<Connection> wrappedHandler) {
            return (CompletionHandler<Connection>)new CompletionHandler<Connection>() {
                public void cancelled() {
                    if (wrappedHandler != null) {
                        wrappedHandler.cancelled();
                    }
                    else {
                        future.cancel(true);
                    }
                }
                
                public void failed(final Throwable throwable) {
                    if (wrappedHandler != null) {
                        wrappedHandler.failed(throwable);
                    }
                    else {
                        future.abort(throwable);
                    }
                }
                
                public void completed(final Connection connection) {
                    future.setConnection(connection);
                    ConnectionManager.this.provider.touchConnection(connection, request);
                    if (wrappedHandler != null) {
                        connection.addCloseListener((Connection.CloseListener)ConnectionManager.this.connectionMonitor);
                        wrappedHandler.completed((Object)connection);
                    }
                }
                
                public void updated(final Connection result) {
                    if (wrappedHandler != null) {
                        wrappedHandler.updated((Object)result);
                    }
                }
            };
        }
        
        static {
            DO_NOT_CACHE = Grizzly.DEFAULT_ATTRIBUTE_BUILDER.createAttribute(ConnectionManager.class.getName());
        }
        
        private static class ConnectionMonitor implements Connection.CloseListener
        {
            private final Semaphore connections;
            
            ConnectionMonitor(final int maxConnections) {
                if (maxConnections != -1) {
                    this.connections = new Semaphore(maxConnections);
                }
                else {
                    this.connections = null;
                }
            }
            
            public boolean acquire() {
                return this.connections == null || this.connections.tryAcquire();
            }
            
            public void onClosed(final Connection connection, final Connection.CloseType closeType) throws IOException {
                if (this.connections != null) {
                    this.connections.release();
                }
            }
        }
    }
    
    static final class SwitchingSSLFilter extends SSLFilter
    {
        private final boolean secureByDefault;
        final Attribute<Boolean> CONNECTION_IS_SECURE;
        
        SwitchingSSLFilter(final SSLEngineConfigurator clientConfig, final boolean secureByDefault) {
            super((SSLEngineConfigurator)null, clientConfig);
            this.CONNECTION_IS_SECURE = (Attribute<Boolean>)Grizzly.DEFAULT_ATTRIBUTE_BUILDER.createAttribute(SwitchingSSLFilter.class.getName());
            this.secureByDefault = secureByDefault;
        }
        
        public NextAction handleEvent(final FilterChainContext ctx, final FilterChainEvent event) throws IOException {
            if (event.type() == SSLSwitchingEvent.class) {
                final SSLSwitchingEvent se = (SSLSwitchingEvent)event;
                this.CONNECTION_IS_SECURE.set((AttributeStorage)se.connection, (Object)se.secure);
                return ctx.getStopAction();
            }
            return ctx.getInvokeAction();
        }
        
        public NextAction handleRead(final FilterChainContext ctx) throws IOException {
            if (this.isSecure(ctx.getConnection())) {
                return super.handleRead(ctx);
            }
            return ctx.getInvokeAction();
        }
        
        public NextAction handleWrite(final FilterChainContext ctx) throws IOException {
            if (this.isSecure(ctx.getConnection())) {
                return super.handleWrite(ctx);
            }
            return ctx.getInvokeAction();
        }
        
        private boolean isSecure(final Connection c) {
            Boolean secStatus = (Boolean)this.CONNECTION_IS_SECURE.get((AttributeStorage)c);
            if (secStatus == null) {
                secStatus = this.secureByDefault;
            }
            return secStatus;
        }
        
        static final class SSLSwitchingEvent implements FilterChainEvent
        {
            final boolean secure;
            final Connection connection;
            
            SSLSwitchingEvent(final boolean secure, final Connection c) {
                this.secure = secure;
                this.connection = c;
            }
            
            public Object type() {
                return SSLSwitchingEvent.class;
            }
        }
    }
    
    private static final class GrizzlyTransferAdapter extends TransferCompletionHandler.TransferAdapter
    {
        public GrizzlyTransferAdapter(final FluentCaseInsensitiveStringsMap headers) throws IOException {
            super(headers);
        }
        
        @Override
        public void getBytes(final byte[] bytes) {
        }
    }
    
    private static final class GrizzlyWebSocketAdapter implements WebSocket
    {
        private final org.glassfish.grizzly.websockets.WebSocket gWebSocket;
        
        GrizzlyWebSocketAdapter(final org.glassfish.grizzly.websockets.WebSocket gWebSocket) {
            this.gWebSocket = gWebSocket;
        }
        
        public WebSocket sendMessage(final byte[] message) {
            this.gWebSocket.send(message);
            return this;
        }
        
        public WebSocket stream(final byte[] fragment, final boolean last) {
            if (fragment != null && fragment.length > 0) {
                this.gWebSocket.stream(last, fragment, 0, fragment.length);
            }
            return this;
        }
        
        public WebSocket stream(final byte[] fragment, final int offset, final int len, final boolean last) {
            if (fragment != null && fragment.length > 0) {
                this.gWebSocket.stream(last, fragment, offset, len);
            }
            return this;
        }
        
        public WebSocket sendTextMessage(final String message) {
            this.gWebSocket.send(message);
            return this;
        }
        
        public WebSocket streamText(final String fragment, final boolean last) {
            this.gWebSocket.stream(last, fragment);
            return this;
        }
        
        public WebSocket sendPing(final byte[] payload) {
            this.gWebSocket.sendPing(payload);
            return this;
        }
        
        public WebSocket sendPong(final byte[] payload) {
            this.gWebSocket.sendPong(payload);
            return this;
        }
        
        public WebSocket addWebSocketListener(final com.ning.http.client.websocket.WebSocketListener l) {
            this.gWebSocket.add((WebSocketListener)new AHCWebSocketListenerAdapter(l, this));
            return this;
        }
        
        public WebSocket removeWebSocketListener(final com.ning.http.client.websocket.WebSocketListener l) {
            this.gWebSocket.remove((WebSocketListener)new AHCWebSocketListenerAdapter(l, this));
            return this;
        }
        
        public boolean isOpen() {
            return this.gWebSocket.isConnected();
        }
        
        public void close() {
            this.gWebSocket.close();
        }
    }
    
    private static final class AHCWebSocketListenerAdapter implements WebSocketListener
    {
        private final com.ning.http.client.websocket.WebSocketListener ahcListener;
        private final WebSocket webSocket;
        
        AHCWebSocketListenerAdapter(final com.ning.http.client.websocket.WebSocketListener ahcListener, final WebSocket webSocket) {
            this.ahcListener = ahcListener;
            this.webSocket = webSocket;
        }
        
        public void onClose(final org.glassfish.grizzly.websockets.WebSocket gWebSocket, final DataFrame dataFrame) {
            try {
                if (WebSocketCloseCodeReasonListener.class.isAssignableFrom(this.ahcListener.getClass())) {
                    final ClosingFrame cf = ClosingFrame.class.cast(dataFrame);
                    WebSocketCloseCodeReasonListener.class.cast(this.ahcListener).onClose(this.webSocket, cf.getCode(), cf.getReason());
                }
                else {
                    this.ahcListener.onClose(this.webSocket);
                }
            }
            catch (Throwable e) {
                this.ahcListener.onError(e);
            }
        }
        
        public void onConnect(final org.glassfish.grizzly.websockets.WebSocket gWebSocket) {
            try {
                this.ahcListener.onOpen(this.webSocket);
            }
            catch (Throwable e) {
                this.ahcListener.onError(e);
            }
        }
        
        public void onMessage(final org.glassfish.grizzly.websockets.WebSocket webSocket, final String s) {
            try {
                if (WebSocketTextListener.class.isAssignableFrom(this.ahcListener.getClass())) {
                    WebSocketTextListener.class.cast(this.ahcListener).onMessage(s);
                }
            }
            catch (Throwable e) {
                this.ahcListener.onError(e);
            }
        }
        
        public void onMessage(final org.glassfish.grizzly.websockets.WebSocket webSocket, final byte[] bytes) {
            try {
                if (WebSocketByteListener.class.isAssignableFrom(this.ahcListener.getClass())) {
                    WebSocketByteListener.class.cast(this.ahcListener).onMessage(bytes);
                }
            }
            catch (Throwable e) {
                this.ahcListener.onError(e);
            }
        }
        
        public void onPing(final org.glassfish.grizzly.websockets.WebSocket webSocket, final byte[] bytes) {
            try {
                if (WebSocketPingListener.class.isAssignableFrom(this.ahcListener.getClass())) {
                    WebSocketPingListener.class.cast(this.ahcListener).onPing(bytes);
                }
            }
            catch (Throwable e) {
                this.ahcListener.onError(e);
            }
        }
        
        public void onPong(final org.glassfish.grizzly.websockets.WebSocket webSocket, final byte[] bytes) {
            try {
                if (WebSocketPongListener.class.isAssignableFrom(this.ahcListener.getClass())) {
                    WebSocketPongListener.class.cast(this.ahcListener).onPong(bytes);
                }
            }
            catch (Throwable e) {
                this.ahcListener.onError(e);
            }
        }
        
        public void onFragment(final org.glassfish.grizzly.websockets.WebSocket webSocket, final String s, final boolean b) {
            try {
                if (WebSocketTextListener.class.isAssignableFrom(this.ahcListener.getClass())) {
                    WebSocketTextListener.class.cast(this.ahcListener).onFragment(s, b);
                }
            }
            catch (Throwable e) {
                this.ahcListener.onError(e);
            }
        }
        
        public void onFragment(final org.glassfish.grizzly.websockets.WebSocket webSocket, final byte[] bytes, final boolean b) {
            try {
                if (WebSocketByteListener.class.isAssignableFrom(this.ahcListener.getClass())) {
                    WebSocketByteListener.class.cast(this.ahcListener).onFragment(bytes, b);
                }
            }
            catch (Throwable e) {
                this.ahcListener.onError(e);
            }
        }
        
        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || this.getClass() != o.getClass()) {
                return false;
            }
            final AHCWebSocketListenerAdapter that = (AHCWebSocketListenerAdapter)o;
            Label_0062: {
                if (this.ahcListener != null) {
                    if (this.ahcListener.equals(that.ahcListener)) {
                        break Label_0062;
                    }
                }
                else if (that.ahcListener == null) {
                    break Label_0062;
                }
                return false;
            }
            if (this.webSocket != null) {
                if (this.webSocket.equals(that.webSocket)) {
                    return true;
                }
            }
            else if (that.webSocket == null) {
                return true;
            }
            return false;
        }
        
        @Override
        public int hashCode() {
            int result = (this.ahcListener != null) ? this.ahcListener.hashCode() : 0;
            result = 31 * result + ((this.webSocket != null) ? this.webSocket.hashCode() : 0);
            return result;
        }
    }
    
    private interface BodyHandler
    {
        public static final int MAX_CHUNK_SIZE = 8192;
        
        boolean handlesBodyType(final Request p0);
        
        boolean doHandle(final FilterChainContext p0, final Request p1, final HttpRequestPacket p2) throws IOException;
    }
    
    private interface StatusHandler
    {
        boolean handleStatus(final HttpResponsePacket p0, final HttpTransactionContext p1, final FilterChainContext p2);
        
        boolean handlesStatus(final int p0);
        
        public enum InvocationStatus
        {
            CONTINUE, 
            STOP;
        }
    }
}
