// 
// Decompiled by Procyon v0.5.36
// 

package com.ning.http.client.providers.jdk;

import com.ning.http.client.Body;
import java.io.OutputStream;
import java.io.File;
import com.ning.http.multipart.MultipartRequestEntity;
import com.ning.http.client.FluentCaseInsensitiveStringsMap;
import java.nio.ByteBuffer;
import java.io.FileInputStream;
import com.ning.http.util.UTF8UrlEncoder;
import java.util.Collection;
import com.ning.http.util.MiscUtil;
import com.ning.http.util.AuthenticatorUtils;
import javax.net.ssl.SSLHandshakeException;
import java.util.concurrent.TimeoutException;
import java.net.SocketTimeoutException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import com.ning.http.client.filter.IOExceptionFilter;
import com.ning.http.client.filter.FilterException;
import com.ning.http.client.ProgressAsyncHandler;
import java.util.zip.GZIPInputStream;
import java.net.URI;
import com.ning.http.client.RequestBuilder;
import com.ning.http.client.MaxRedirectException;
import com.ning.http.client.filter.ResponseFilter;
import com.ning.http.client.filter.FilterContext;
import com.ning.http.client.listener.TransferCompletionHandler;
import com.ning.http.util.AsyncHttpProviderUtils;
import org.slf4j.LoggerFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.SocketAddress;
import java.net.PasswordAuthentication;
import java.net.InetSocketAddress;
import com.ning.http.client.Response;
import com.ning.http.client.HttpResponseBodyPart;
import java.util.List;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;
import javax.net.ssl.SSLContext;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import com.ning.http.util.SslUtils;
import javax.net.ssl.HttpsURLConnection;
import com.ning.http.client.PerRequestConfig;
import java.net.HttpURLConnection;
import java.net.Proxy;
import com.ning.http.client.Realm;
import com.ning.http.client.ProxyServer;
import java.util.concurrent.Callable;
import javax.naming.AuthenticationException;
import com.ning.http.util.ProxyUtils;
import java.io.IOException;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.AsyncHandler;
import com.ning.http.client.Request;
import java.util.Iterator;
import java.util.Map;
import com.ning.http.client.AsyncHttpProviderConfig;
import java.net.Authenticator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import com.ning.http.client.AsyncHttpClientConfig;
import org.slf4j.Logger;
import com.ning.http.client.AsyncHttpProvider;

public class JDKAsyncHttpProvider implements AsyncHttpProvider
{
    private static final Logger logger;
    private static final String NTLM_DOMAIN = "http.auth.ntlm.domain";
    private final AsyncHttpClientConfig config;
    private final AtomicBoolean isClose;
    private static final int MAX_BUFFERED_BYTES = 8192;
    private final AtomicInteger maxConnections;
    private String jdkNtlmDomain;
    private Authenticator jdkAuthenticator;
    private boolean bufferResponseInMemory;
    
    public JDKAsyncHttpProvider(final AsyncHttpClientConfig config) {
        this.isClose = new AtomicBoolean(false);
        this.maxConnections = new AtomicInteger();
        this.bufferResponseInMemory = false;
        this.config = config;
        final AsyncHttpProviderConfig<?, ?> providerConfig = config.getAsyncHttpProviderConfig();
        if (providerConfig != null && JDKAsyncHttpProviderConfig.class.isAssignableFrom(providerConfig.getClass())) {
            this.configure(JDKAsyncHttpProviderConfig.class.cast(providerConfig));
        }
    }
    
    private void configure(final JDKAsyncHttpProviderConfig config) {
        for (final Map.Entry<String, String> e : config.propertiesSet()) {
            System.setProperty(e.getKey(), e.getValue());
        }
        if (config.getProperty("bufferResponseInMemory") != null) {
            this.bufferResponseInMemory = true;
        }
    }
    
    public <T> ListenableFuture<T> execute(final Request request, final AsyncHandler<T> handler) throws IOException {
        return this.execute(request, handler, null);
    }
    
    public <T> ListenableFuture<T> execute(final Request request, final AsyncHandler<T> handler, final ListenableFuture<?> future) throws IOException {
        if (this.isClose.get()) {
            throw new IOException("Closed");
        }
        if (this.config.getMaxTotalConnections() > -1 && this.maxConnections.get() + 1 > this.config.getMaxTotalConnections()) {
            throw new IOException(String.format("Too many connections %s", this.config.getMaxTotalConnections()));
        }
        final ProxyServer proxyServer = ProxyUtils.getProxyServer(this.config, request);
        final Realm realm = (request.getRealm() != null) ? request.getRealm() : this.config.getRealm();
        Proxy proxy = null;
        Label_0158: {
            if (proxyServer == null) {
                if (realm == null) {
                    break Label_0158;
                }
            }
            try {
                proxy = this.configureProxyAndAuth(proxyServer, realm);
            }
            catch (AuthenticationException e) {
                throw new IOException(e.getMessage());
            }
        }
        final HttpURLConnection urlConnection = this.createUrlConnection(request);
        final PerRequestConfig conf = request.getPerRequestConfig();
        final int requestTimeout = (conf != null && conf.getRequestTimeoutInMs() != 0) ? conf.getRequestTimeoutInMs() : this.config.getRequestTimeoutInMs();
        JDKDelegateFuture delegate = null;
        if (future != null) {
            delegate = new JDKDelegateFuture((AsyncHandler<V>)handler, requestTimeout, (ListenableFuture<V>)future, urlConnection);
        }
        final JDKFuture f = (delegate == null) ? new JDKFuture((AsyncHandler<V>)handler, requestTimeout, urlConnection) : delegate;
        f.touch();
        f.setInnerFuture(this.config.executorService().submit(new AsyncHttpUrlConnection<T>(urlConnection, request, handler, f)));
        this.maxConnections.incrementAndGet();
        return (ListenableFuture<T>)f;
    }
    
    private HttpURLConnection createUrlConnection(final Request request) throws IOException {
        final ProxyServer proxyServer = ProxyUtils.getProxyServer(this.config, request);
        final Realm realm = (request.getRealm() != null) ? request.getRealm() : this.config.getRealm();
        Proxy proxy = null;
        Label_0072: {
            if (proxyServer == null) {
                if (realm == null) {
                    break Label_0072;
                }
            }
            try {
                proxy = this.configureProxyAndAuth(proxyServer, realm);
            }
            catch (AuthenticationException e) {
                throw new IOException(e.getMessage());
            }
        }
        HttpURLConnection urlConnection = null;
        if (proxy == null) {
            urlConnection = (HttpURLConnection)request.getURI().toURL().openConnection(Proxy.NO_PROXY);
        }
        else {
            urlConnection = (HttpURLConnection)proxyServer.getURI().toURL().openConnection(proxy);
        }
        if (request.getUrl().startsWith("https")) {
            final HttpsURLConnection secure = (HttpsURLConnection)urlConnection;
            SSLContext sslContext = this.config.getSSLContext();
            if (sslContext == null) {
                try {
                    sslContext = SslUtils.getSSLContext();
                }
                catch (NoSuchAlgorithmException e2) {
                    throw new IOException(e2.getMessage());
                }
                catch (GeneralSecurityException e3) {
                    throw new IOException(e3.getMessage());
                }
            }
            secure.setSSLSocketFactory(sslContext.getSocketFactory());
            secure.setHostnameVerifier(this.config.getHostnameVerifier());
        }
        return urlConnection;
    }
    
    public void close() {
        this.isClose.set(true);
    }
    
    public Response prepareResponse(final HttpResponseStatus status, final HttpResponseHeaders headers, final List<HttpResponseBodyPart> bodyParts) {
        return new JDKResponse(status, headers, bodyParts);
    }
    
    private Proxy configureProxyAndAuth(final ProxyServer proxyServer, final Realm realm) throws AuthenticationException {
        Proxy proxy = null;
        if (proxyServer != null) {
            final String proxyHost = proxyServer.getHost().startsWith("http://") ? proxyServer.getHost().substring("http://".length()) : proxyServer.getHost();
            final SocketAddress addr = new InetSocketAddress(proxyHost, proxyServer.getPort());
            proxy = new Proxy(Proxy.Type.HTTP, addr);
        }
        final boolean hasProxy = proxyServer != null && proxyServer.getPrincipal() != null;
        final boolean hasAuthentication = realm != null && realm.getPrincipal() != null;
        if (hasProxy || hasAuthentication) {
            Field f = null;
            try {
                f = Authenticator.class.getDeclaredField("theAuthenticator");
                f.setAccessible(true);
                this.jdkAuthenticator = (Authenticator)f.get(Authenticator.class);
            }
            catch (NoSuchFieldException e) {}
            catch (IllegalAccessException ex) {}
            Authenticator.setDefault(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    if (hasProxy && this.getRequestingHost().equals(proxyServer.getHost()) && this.getRequestingPort() == proxyServer.getPort()) {
                        String password = "";
                        if (proxyServer.getPassword() != null) {
                            password = proxyServer.getPassword();
                        }
                        return new PasswordAuthentication(proxyServer.getPrincipal(), password.toCharArray());
                    }
                    if (hasAuthentication) {
                        return new PasswordAuthentication(realm.getPrincipal(), realm.getPassword().toCharArray());
                    }
                    return super.getPasswordAuthentication();
                }
            });
        }
        else {
            Authenticator.setDefault(null);
        }
        return proxy;
    }
    
    private InputStream getInputStream(final HttpURLConnection urlConnection) throws IOException {
        if (urlConnection.getResponseCode() < 400) {
            return urlConnection.getInputStream();
        }
        final InputStream ein = urlConnection.getErrorStream();
        return (ein != null) ? ein : new ByteArrayInputStream(new byte[0]);
    }
    
    static {
        logger = LoggerFactory.getLogger((Class)JDKAsyncHttpProvider.class);
    }
    
    private final class AsyncHttpUrlConnection<T> implements Callable<T>
    {
        private HttpURLConnection urlConnection;
        private Request request;
        private final AsyncHandler<T> asyncHandler;
        private final ListenableFuture<T> future;
        private int currentRedirectCount;
        private AtomicBoolean isAuth;
        private byte[] cachedBytes;
        private int cachedBytesLenght;
        private boolean terminate;
        
        public AsyncHttpUrlConnection(final HttpURLConnection urlConnection, final Request request, final AsyncHandler<T> asyncHandler, final ListenableFuture<T> future) {
            this.isAuth = new AtomicBoolean(false);
            this.terminate = true;
            this.urlConnection = urlConnection;
            this.request = request;
            this.asyncHandler = asyncHandler;
            this.future = future;
            this.request = request;
        }
        
        public T call() throws Exception {
            AsyncHandler.STATE state = AsyncHandler.STATE.ABORT;
            try {
                URI uri = null;
                try {
                    uri = AsyncHttpProviderUtils.createUri(this.request.getRawUrl());
                }
                catch (IllegalArgumentException u) {
                    uri = AsyncHttpProviderUtils.createUri(this.request.getUrl());
                }
                this.configure(uri, this.urlConnection, this.request);
                this.urlConnection.connect();
                if (TransferCompletionHandler.class.isAssignableFrom(this.asyncHandler.getClass())) {
                    throw new IllegalStateException(TransferCompletionHandler.class.getName() + "not supported by this provider");
                }
                final int statusCode = this.urlConnection.getResponseCode();
                JDKAsyncHttpProvider.logger.debug("\n\nRequest {}\n\nResponse {}\n", (Object)this.request, (Object)statusCode);
                final ResponseStatus status = new ResponseStatus(uri, this.urlConnection, JDKAsyncHttpProvider.this);
                FilterContext fc = new FilterContext.FilterContextBuilder<T>().asyncHandler(this.asyncHandler).request(this.request).responseStatus(status).build();
                for (final ResponseFilter asyncFilter : JDKAsyncHttpProvider.this.config.getResponseFilters()) {
                    fc = asyncFilter.filter(fc);
                    if (fc == null) {
                        throw new NullPointerException("FilterContext is null");
                    }
                }
                if (fc.replayRequest()) {
                    this.request = fc.getRequest();
                    this.urlConnection = JDKAsyncHttpProvider.this.createUrlConnection(this.request);
                    this.terminate = false;
                    return this.call();
                }
                final boolean redirectEnabled = this.request.isRedirectEnabled() || JDKAsyncHttpProvider.this.config.isRedirectEnabled();
                if (redirectEnabled && (statusCode == 302 || statusCode == 301)) {
                    if (this.currentRedirectCount++ >= JDKAsyncHttpProvider.this.config.getMaxRedirects()) {
                        throw new MaxRedirectException("Maximum redirect reached: " + JDKAsyncHttpProvider.this.config.getMaxRedirects());
                    }
                    final String location = this.urlConnection.getHeaderField("Location");
                    final URI redirUri = AsyncHttpProviderUtils.getRedirectUri(uri, location);
                    final String newUrl = redirUri.toString();
                    if (!newUrl.equals(uri.toString())) {
                        final RequestBuilder builder = new RequestBuilder(this.request);
                        JDKAsyncHttpProvider.logger.debug("Redirecting to {}", (Object)newUrl);
                        this.request = builder.setUrl(newUrl).build();
                        this.urlConnection = JDKAsyncHttpProvider.this.createUrlConnection(this.request);
                        this.terminate = false;
                        return this.call();
                    }
                }
                final Realm realm = (this.request.getRealm() != null) ? this.request.getRealm() : JDKAsyncHttpProvider.this.config.getRealm();
                if (statusCode == 401 && !this.isAuth.getAndSet(true) && realm != null) {
                    final String wwwAuth = this.urlConnection.getHeaderField("WWW-Authenticate");
                    JDKAsyncHttpProvider.logger.debug("Sending authentication to {}", (Object)this.request.getUrl());
                    final Realm nr = new Realm.RealmBuilder().clone(realm).parseWWWAuthenticateHeader(wwwAuth).setUri(URI.create(this.request.getUrl()).getPath()).setMethodName(this.request.getMethod()).setUsePreemptiveAuth(true).build();
                    final RequestBuilder builder = new RequestBuilder(this.request);
                    this.request = builder.setRealm(nr).build();
                    this.urlConnection = JDKAsyncHttpProvider.this.createUrlConnection(this.request);
                    this.terminate = false;
                    return this.call();
                }
                state = this.asyncHandler.onStatusReceived(status);
                if (state == AsyncHandler.STATE.CONTINUE) {
                    state = this.asyncHandler.onHeadersReceived(new ResponseHeaders(uri, this.urlConnection, JDKAsyncHttpProvider.this));
                }
                if (state == AsyncHandler.STATE.CONTINUE) {
                    InputStream is = JDKAsyncHttpProvider.this.getInputStream(this.urlConnection);
                    final String contentEncoding = this.urlConnection.getHeaderField("Content-Encoding");
                    final boolean isGZipped = contentEncoding != null && "gzip".equalsIgnoreCase(contentEncoding);
                    if (isGZipped) {
                        is = new GZIPInputStream(is);
                    }
                    int byteToRead = this.urlConnection.getContentLength();
                    InputStream stream = is;
                    if (JDKAsyncHttpProvider.this.bufferResponseInMemory || byteToRead <= 0) {
                        final int[] lengthWrapper = { 0 };
                        final byte[] bytes = AsyncHttpProviderUtils.readFully(is, lengthWrapper);
                        stream = new ByteArrayInputStream(bytes, 0, lengthWrapper[0]);
                        byteToRead = lengthWrapper[0];
                    }
                    if (byteToRead > 0) {
                        final int minBytes = Math.min(8192, byteToRead);
                        final byte[] bytes = new byte[minBytes];
                        int leftBytes = (minBytes < 8192) ? minBytes : byteToRead;
                        int read = 0;
                        while (leftBytes > -1) {
                            read = stream.read(bytes);
                            if (read == -1) {
                                break;
                            }
                            this.future.touch();
                            final byte[] b = new byte[read];
                            System.arraycopy(bytes, 0, b, 0, read);
                            leftBytes -= read;
                            this.asyncHandler.onBodyPartReceived(new ResponseBodyPart(uri, b, JDKAsyncHttpProvider.this, leftBytes > -1));
                        }
                    }
                    if (this.request.getMethod().equalsIgnoreCase("HEAD")) {
                        this.asyncHandler.onBodyPartReceived(new ResponseBodyPart(uri, "".getBytes(), JDKAsyncHttpProvider.this, true));
                    }
                }
                if (ProgressAsyncHandler.class.isAssignableFrom(this.asyncHandler.getClass())) {
                    ProgressAsyncHandler.class.cast(this.asyncHandler).onHeaderWriteCompleted();
                    ProgressAsyncHandler.class.cast(this.asyncHandler).onContentWriteCompleted();
                }
                try {
                    final T t = this.asyncHandler.onCompleted();
                    this.future.content(t);
                    this.future.done(null);
                    return t;
                }
                catch (Throwable t2) {
                    final RuntimeException ex = new RuntimeException();
                    ex.initCause(t2);
                    throw ex;
                }
            }
            catch (Throwable t3) {
                JDKAsyncHttpProvider.logger.debug(t3.getMessage(), t3);
                if (IOException.class.isAssignableFrom(t3.getClass()) && JDKAsyncHttpProvider.this.config.getIOExceptionFilters().size() > 0) {
                    FilterContext fc2 = new FilterContext.FilterContextBuilder<T>().asyncHandler(this.asyncHandler).request(this.request).ioException(IOException.class.cast(t3)).build();
                    try {
                        fc2 = this.handleIoException(fc2);
                    }
                    catch (FilterException e) {
                        if (JDKAsyncHttpProvider.this.config.getMaxTotalConnections() != -1) {
                            JDKAsyncHttpProvider.this.maxConnections.decrementAndGet();
                        }
                        this.future.done(null);
                    }
                    if (fc2.replayRequest()) {
                        this.request = fc2.getRequest();
                        this.urlConnection = JDKAsyncHttpProvider.this.createUrlConnection(this.request);
                        return this.call();
                    }
                }
                try {
                    this.future.abort(this.filterException(t3));
                }
                catch (Throwable t4) {
                    JDKAsyncHttpProvider.logger.error(t4.getMessage(), t4);
                }
            }
            finally {
                if (this.terminate) {
                    if (JDKAsyncHttpProvider.this.config.getMaxTotalConnections() != -1) {
                        JDKAsyncHttpProvider.this.maxConnections.decrementAndGet();
                    }
                    this.urlConnection.disconnect();
                    if (JDKAsyncHttpProvider.this.jdkNtlmDomain != null) {
                        System.setProperty("http.auth.ntlm.domain", JDKAsyncHttpProvider.this.jdkNtlmDomain);
                    }
                    Authenticator.setDefault(JDKAsyncHttpProvider.this.jdkAuthenticator);
                }
            }
            return null;
        }
        
        private FilterContext handleIoException(FilterContext fc) throws FilterException {
            for (final IOExceptionFilter asyncFilter : JDKAsyncHttpProvider.this.config.getIOExceptionFilters()) {
                fc = asyncFilter.filter(fc);
                if (fc == null) {
                    throw new NullPointerException("FilterContext is null");
                }
            }
            return fc;
        }
        
        private Throwable filterException(Throwable t) {
            if (UnknownHostException.class.isAssignableFrom(t.getClass())) {
                t = new ConnectException(t.getMessage());
            }
            if (SocketTimeoutException.class.isAssignableFrom(t.getClass())) {
                int responseTimeoutInMs = JDKAsyncHttpProvider.this.config.getRequestTimeoutInMs();
                if (this.request.getPerRequestConfig() != null && this.request.getPerRequestConfig().getRequestTimeoutInMs() != -1) {
                    responseTimeoutInMs = this.request.getPerRequestConfig().getRequestTimeoutInMs();
                }
                t = new TimeoutException(String.format("No response received after %s", responseTimeoutInMs));
            }
            if (SSLHandshakeException.class.isAssignableFrom(t.getClass())) {
                final Throwable t2 = new ConnectException();
                t2.initCause(t);
                t = t2;
            }
            return t;
        }
        
        private void configure(final URI uri, final HttpURLConnection urlConnection, final Request request) throws IOException, AuthenticationException {
            final PerRequestConfig conf = request.getPerRequestConfig();
            final int requestTimeout = (conf != null && conf.getRequestTimeoutInMs() != 0) ? conf.getRequestTimeoutInMs() : JDKAsyncHttpProvider.this.config.getRequestTimeoutInMs();
            urlConnection.setConnectTimeout(JDKAsyncHttpProvider.this.config.getConnectionTimeoutInMs());
            if (requestTimeout != -1) {
                urlConnection.setReadTimeout(requestTimeout);
            }
            urlConnection.setInstanceFollowRedirects(false);
            String host = uri.getHost();
            final String method = request.getMethod();
            if (request.getVirtualHost() != null) {
                host = request.getVirtualHost();
            }
            if (uri.getPort() == -1 || request.getVirtualHost() != null) {
                urlConnection.setRequestProperty("Host", host);
            }
            else {
                urlConnection.setRequestProperty("Host", host + ":" + uri.getPort());
            }
            if (JDKAsyncHttpProvider.this.config.isCompressionEnabled()) {
                urlConnection.setRequestProperty("Accept-Encoding", "gzip");
            }
            if (!method.equalsIgnoreCase("CONNECT")) {
                final FluentCaseInsensitiveStringsMap h = request.getHeaders();
                if (h != null) {
                    for (final String name : h.keySet()) {
                        if (!"host".equalsIgnoreCase(name)) {
                            for (final String value : h.get((Object)name)) {
                                urlConnection.setRequestProperty(name, value);
                                if (name.equalsIgnoreCase("Expect")) {
                                    throw new IllegalStateException("Expect: 100-Continue not supported");
                                }
                            }
                        }
                    }
                }
            }
            final String ka = AsyncHttpProviderUtils.keepAliveHeaderValue(JDKAsyncHttpProvider.this.config);
            urlConnection.setRequestProperty("Connection", ka);
            final ProxyServer proxyServer = ProxyUtils.getProxyServer(JDKAsyncHttpProvider.this.config, request);
            final boolean avoidProxy = ProxyUtils.avoidProxy(proxyServer, uri.getHost());
            if (!avoidProxy) {
                urlConnection.setRequestProperty("Proxy-Connection", ka);
                if (proxyServer.getPrincipal() != null) {
                    urlConnection.setRequestProperty("Proxy-Authorization", AuthenticatorUtils.computeBasicAuthentication(proxyServer));
                }
                if (proxyServer.getProtocol().equals(ProxyServer.Protocol.NTLM)) {
                    JDKAsyncHttpProvider.this.jdkNtlmDomain = System.getProperty("http.auth.ntlm.domain");
                    System.setProperty("http.auth.ntlm.domain", proxyServer.getNtlmDomain());
                }
            }
            final Realm realm = (request.getRealm() != null) ? request.getRealm() : JDKAsyncHttpProvider.this.config.getRealm();
            if (realm != null && realm.getUsePreemptiveAuth()) {
                Label_0571: {
                    switch (realm.getAuthScheme()) {
                        case BASIC: {
                            urlConnection.setRequestProperty("Authorization", AuthenticatorUtils.computeBasicAuthentication(realm));
                            break;
                        }
                        case DIGEST: {
                            if (MiscUtil.isNonEmpty(realm.getNonce())) {
                                try {
                                    urlConnection.setRequestProperty("Authorization", AuthenticatorUtils.computeDigestAuthentication(realm));
                                    break;
                                }
                                catch (NoSuchAlgorithmException e) {
                                    throw new SecurityException(e);
                                }
                                break Label_0571;
                            }
                            break;
                        }
                        case NTLM: {
                            JDKAsyncHttpProvider.this.jdkNtlmDomain = System.getProperty("http.auth.ntlm.domain");
                            System.setProperty("http.auth.ntlm.domain", realm.getDomain());
                            break;
                        }
                        case NONE: {
                            break;
                        }
                        default: {
                            throw new IllegalStateException(String.format("Invalid Authentication %s", realm.toString()));
                        }
                    }
                }
            }
            if (request.getHeaders().getFirstValue("Accept") == null) {
                urlConnection.setRequestProperty("Accept", "*/*");
            }
            if (request.getHeaders().getFirstValue("User-Agent") != null) {
                urlConnection.setRequestProperty("User-Agent", request.getHeaders().getFirstValue("User-Agent"));
            }
            else if (JDKAsyncHttpProvider.this.config.getUserAgent() != null) {
                urlConnection.setRequestProperty("User-Agent", JDKAsyncHttpProvider.this.config.getUserAgent());
            }
            else {
                urlConnection.setRequestProperty("User-Agent", AsyncHttpProviderUtils.constructUserAgent(JDKAsyncHttpProvider.class));
            }
            if (MiscUtil.isNonEmpty(request.getCookies())) {
                urlConnection.setRequestProperty("Cookie", AsyncHttpProviderUtils.encodeCookies(request.getCookies()));
            }
            final String reqType = request.getMethod();
            urlConnection.setRequestMethod(reqType);
            if ("POST".equals(reqType) || "PUT".equals(reqType)) {
                urlConnection.setRequestProperty("Content-Length", "0");
                urlConnection.setDoOutput(true);
                final String bodyCharset = (request.getBodyEncoding() == null) ? "ISO-8859-1" : request.getBodyEncoding();
                if (this.cachedBytes != null) {
                    urlConnection.setRequestProperty("Content-Length", String.valueOf(this.cachedBytesLenght));
                    urlConnection.setFixedLengthStreamingMode(this.cachedBytesLenght);
                    urlConnection.getOutputStream().write(this.cachedBytes, 0, this.cachedBytesLenght);
                }
                else if (request.getByteData() != null) {
                    urlConnection.setRequestProperty("Content-Length", String.valueOf(request.getByteData().length));
                    urlConnection.setFixedLengthStreamingMode(request.getByteData().length);
                    urlConnection.getOutputStream().write(request.getByteData());
                }
                else if (request.getStringData() != null) {
                    if (!request.getHeaders().containsKey("Content-Type")) {
                        urlConnection.setRequestProperty("Content-Type", "text/html;" + bodyCharset);
                    }
                    final byte[] b = request.getStringData().getBytes(bodyCharset);
                    urlConnection.setRequestProperty("Content-Length", String.valueOf(b.length));
                    urlConnection.getOutputStream().write(b);
                }
                else if (request.getStreamData() != null) {
                    final int[] lengthWrapper = { 0 };
                    this.cachedBytes = AsyncHttpProviderUtils.readFully(request.getStreamData(), lengthWrapper);
                    this.cachedBytesLenght = lengthWrapper[0];
                    urlConnection.setRequestProperty("Content-Length", String.valueOf(this.cachedBytesLenght));
                    urlConnection.setFixedLengthStreamingMode(this.cachedBytesLenght);
                    urlConnection.getOutputStream().write(this.cachedBytes, 0, this.cachedBytesLenght);
                }
                else if (request.getParams() != null) {
                    final StringBuilder sb = new StringBuilder();
                    for (final Map.Entry<String, List<String>> paramEntry : request.getParams()) {
                        final String key = paramEntry.getKey();
                        for (final String value2 : paramEntry.getValue()) {
                            if (sb.length() > 0) {
                                sb.append("&");
                            }
                            UTF8UrlEncoder.appendEncoded(sb, key);
                            sb.append("=");
                            UTF8UrlEncoder.appendEncoded(sb, value2);
                        }
                    }
                    urlConnection.setRequestProperty("Content-Length", String.valueOf(sb.length()));
                    urlConnection.setFixedLengthStreamingMode(sb.length());
                    if (!request.getHeaders().containsKey("Content-Type")) {
                        urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    }
                    urlConnection.getOutputStream().write(sb.toString().getBytes(bodyCharset));
                }
                else if (request.getParts() != null) {
                    int lenght = (int)request.getContentLength();
                    if (lenght != -1) {
                        urlConnection.setRequestProperty("Content-Length", String.valueOf(lenght));
                        urlConnection.setFixedLengthStreamingMode(lenght);
                    }
                    if (lenght == -1) {
                        lenght = 8192;
                    }
                    final MultipartRequestEntity mre = AsyncHttpProviderUtils.createMultipartRequestEntity(request.getParts(), request.getParams());
                    urlConnection.setRequestProperty("Content-Type", mre.getContentType());
                    urlConnection.setRequestProperty("Content-Length", String.valueOf(mre.getContentLength()));
                    mre.writeRequest(urlConnection.getOutputStream());
                }
                else if (request.getEntityWriter() != null) {
                    final int lenght = (int)request.getContentLength();
                    if (lenght != -1) {
                        urlConnection.setRequestProperty("Content-Length", String.valueOf(lenght));
                        urlConnection.setFixedLengthStreamingMode(lenght);
                    }
                    request.getEntityWriter().writeEntity(urlConnection.getOutputStream());
                }
                else if (request.getFile() != null) {
                    final File file = request.getFile();
                    if (!file.isFile()) {
                        throw new IOException(String.format(Thread.currentThread() + "File %s is not a file or doesn't exist", file.getAbsolutePath()));
                    }
                    urlConnection.setRequestProperty("Content-Length", String.valueOf(file.length()));
                    urlConnection.setFixedLengthStreamingMode((int)file.length());
                    final FileInputStream fis = new FileInputStream(file);
                    try {
                        final OutputStream os = urlConnection.getOutputStream();
                        final byte[] buffer = new byte[16384];
                        while (true) {
                            final int read = fis.read(buffer);
                            if (read < 0) {
                                break;
                            }
                            os.write(buffer, 0, read);
                        }
                    }
                    finally {
                        fis.close();
                    }
                }
                else if (request.getBodyGenerator() != null) {
                    final Body body = request.getBodyGenerator().createBody();
                    try {
                        int length = (int)body.getContentLength();
                        if (length < 0) {
                            length = (int)request.getContentLength();
                        }
                        if (length >= 0) {
                            urlConnection.setRequestProperty("Content-Length", String.valueOf(length));
                            urlConnection.setFixedLengthStreamingMode(length);
                        }
                        final OutputStream os = urlConnection.getOutputStream();
                        final ByteBuffer buffer2 = ByteBuffer.allocate(8192);
                        while (true) {
                            buffer2.clear();
                            if (body.read(buffer2) < 0L) {
                                break;
                            }
                            os.write(buffer2.array(), buffer2.arrayOffset(), buffer2.position());
                        }
                    }
                    finally {
                        try {
                            body.close();
                        }
                        catch (IOException e2) {
                            JDKAsyncHttpProvider.logger.warn("Failed to close request body: {}", (Object)e2.getMessage(), (Object)e2);
                        }
                    }
                }
            }
        }
    }
}
