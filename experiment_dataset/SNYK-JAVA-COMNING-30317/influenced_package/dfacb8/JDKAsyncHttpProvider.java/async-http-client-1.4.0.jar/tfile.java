// 
// Decompiled by Procyon v0.5.36
// 

package com.ning.http.client.providers.jdk;

import com.ning.http.client.Body;
import java.io.OutputStream;
import java.io.File;
import org.jboss.netty.buffer.ChannelBuffer;
import com.ning.http.multipart.MultipartRequestEntity;
import java.util.Iterator;
import com.ning.http.client.FluentCaseInsensitiveStringsMap;
import com.ning.http.client.PerRequestConfig;
import java.nio.ByteBuffer;
import java.io.FileInputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import com.ning.http.util.UTF8UrlEncoder;
import java.util.List;
import java.util.Map;
import com.ning.http.util.AuthenticatorUtils;
import javax.net.ssl.SSLHandshakeException;
import java.util.concurrent.TimeoutException;
import java.net.SocketTimeoutException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import com.ning.http.client.ProgressAsyncHandler;
import java.util.zip.GZIPInputStream;
import java.net.URI;
import com.ning.http.client.RequestBuilder;
import com.ning.http.client.MaxRedirectException;
import com.ning.http.client.logging.LogManager;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.SocketAddress;
import java.net.PasswordAuthentication;
import java.net.InetSocketAddress;
import com.ning.http.client.Response;
import com.ning.http.client.HttpResponseBodyPart;
import java.util.Collection;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.HostnameVerifier;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import com.ning.http.util.SslUtils;
import javax.net.ssl.HttpsURLConnection;
import com.ning.http.util.AsyncHttpProviderUtils;
import java.net.Proxy;
import com.ning.http.client.Realm;
import com.ning.http.client.ProxyServer;
import java.util.concurrent.Callable;
import javax.naming.AuthenticationException;
import java.io.IOException;
import java.util.concurrent.Future;
import com.ning.http.client.AsyncHandler;
import com.ning.http.client.Request;
import com.ning.http.client.AsyncHttpProviderConfig;
import java.net.Authenticator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.logging.Logger;
import java.net.HttpURLConnection;
import com.ning.http.client.AsyncHttpProvider;

public class JDKAsyncHttpProvider implements AsyncHttpProvider<HttpURLConnection>
{
    private static final Logger logger;
    private static final String NTLM_DOMAIN = "http.auth.ntlm.domain";
    private final AsyncHttpClientConfig config;
    private final AtomicBoolean isClose;
    private static final int MAX_BUFFERED_BYTES = 8192;
    private final AtomicInteger maxConnections;
    private String jdkNtlmDomain;
    private Authenticator jdkAuthenticator;
    
    public JDKAsyncHttpProvider(final AsyncHttpClientConfig config) {
        this.isClose = new AtomicBoolean(false);
        this.maxConnections = new AtomicInteger();
        this.config = config;
        final AsyncHttpProviderConfig<?, ?> providerConfig = config.getAsyncHttpProviderConfig();
        if (providerConfig != null && JDKAsyncHttpProviderConfig.class.isAssignableFrom(providerConfig.getClass())) {
            this.configure(JDKAsyncHttpProviderConfig.class.cast(providerConfig));
        }
    }
    
    private void configure(final JDKAsyncHttpProviderConfig config) {
    }
    
    public <T> Future<T> execute(final Request request, final AsyncHandler<T> handler) throws IOException {
        if (this.isClose.get()) {
            throw new IOException("Closed");
        }
        if (this.config.getMaxTotalConnections() > -1 && this.maxConnections.get() + 1 > this.config.getMaxTotalConnections()) {
            throw new IOException(String.format("Too many connections %s", this.config.getMaxTotalConnections()));
        }
        final ProxyServer proxyServer = (request.getProxyServer() != null) ? request.getProxyServer() : this.config.getProxyServer();
        final Realm realm = (request.getRealm() != null) ? request.getRealm() : this.config.getRealm();
        Proxy proxy = null;
        Label_0172: {
            if (proxyServer == null) {
                if (realm == null) {
                    break Label_0172;
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
        final JDKFuture f = new JDKFuture((AsyncHandler<V>)handler, this.config.getRequestTimeoutInMs());
        f.touch();
        f.setInnerFuture(this.config.executorService().submit(new AsyncHttpUrlConnection<T>(urlConnection, request, handler, f)));
        this.maxConnections.incrementAndGet();
        return (Future<T>)f;
    }
    
    private HttpURLConnection createUrlConnection(final Request request) throws IOException {
        final ProxyServer proxyServer = (request.getProxyServer() != null) ? request.getProxyServer() : this.config.getProxyServer();
        final Realm realm = (request.getRealm() != null) ? request.getRealm() : this.config.getRealm();
        Proxy proxy = null;
        Label_0089: {
            if (proxyServer == null) {
                if (realm == null) {
                    break Label_0089;
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
            urlConnection = (HttpURLConnection)AsyncHttpProviderUtils.createUri(request.getUrl()).toURL().openConnection();
        }
        else {
            urlConnection = (HttpURLConnection)AsyncHttpProviderUtils.createUri(request.getUrl()).toURL().openConnection(proxy);
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
            secure.setHostnameVerifier(new HostnameVerifier() {
                public boolean verify(final String s, final SSLSession sslSession) {
                    return true;
                }
            });
        }
        return urlConnection;
    }
    
    public void close() {
        this.isClose.set(true);
    }
    
    public Response prepareResponse(final HttpResponseStatus status, final HttpResponseHeaders headers, final Collection<HttpResponseBodyPart> bodyParts) {
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
        logger = LogManager.getLogger(JDKAsyncHttpProvider.class);
    }
    
    private final class AsyncHttpUrlConnection<T> implements Callable<T>
    {
        private HttpURLConnection urlConnection;
        private Request request;
        private final AsyncHandler<T> asyncHandler;
        private final JDKFuture future;
        private int currentRedirectCount;
        private AtomicBoolean isAuth;
        private byte[] cachedBytes;
        private int cachedBytesLenght;
        
        public AsyncHttpUrlConnection(final HttpURLConnection urlConnection, final Request request, final AsyncHandler<T> asyncHandler, final JDKFuture future) {
            this.isAuth = new AtomicBoolean(false);
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
                final int statusCode = this.urlConnection.getResponseCode();
                if (JDKAsyncHttpProvider.logger.isDebugEnabled()) {
                    JDKAsyncHttpProvider.logger.debug(String.format(AsyncHttpProviderUtils.currentThread() + "\n\nRequest %s\n\nResponse %s\n", this.request, statusCode), new Object[0]);
                }
                final boolean redirectEnabled = this.request.isRedirectEnabled() || JDKAsyncHttpProvider.this.config.isRedirectEnabled();
                if (redirectEnabled && (statusCode == 302 || statusCode == 301)) {
                    if (this.currentRedirectCount++ >= JDKAsyncHttpProvider.this.config.getMaxRedirects()) {
                        throw new MaxRedirectException("Maximum redirect reached: " + JDKAsyncHttpProvider.this.config.getMaxRedirects());
                    }
                    String location = this.urlConnection.getHeaderField("Location");
                    if (location.startsWith("/")) {
                        location = AsyncHttpProviderUtils.getBaseUrl(uri) + location;
                    }
                    if (!location.equals(uri.toString())) {
                        final URI newUri = AsyncHttpProviderUtils.createUri(location);
                        final RequestBuilder builder = new RequestBuilder(this.request);
                        final String newUrl = newUri.toString();
                        if (JDKAsyncHttpProvider.logger.isDebugEnabled()) {
                            JDKAsyncHttpProvider.logger.debug(String.format(AsyncHttpProviderUtils.currentThread() + "Redirecting to %s", newUrl), new Object[0]);
                        }
                        this.request = builder.setUrl(newUrl).build();
                        this.urlConnection = JDKAsyncHttpProvider.this.createUrlConnection(this.request);
                        return this.call();
                    }
                }
                final Realm realm = (this.request.getRealm() != null) ? this.request.getRealm() : JDKAsyncHttpProvider.this.config.getRealm();
                if (statusCode == 401 && !this.isAuth.getAndSet(true) && realm != null) {
                    final String wwwAuth = this.urlConnection.getHeaderField("WWW-Authenticate");
                    if (JDKAsyncHttpProvider.logger.isDebugEnabled()) {
                        JDKAsyncHttpProvider.logger.debug(String.format(AsyncHttpProviderUtils.currentThread() + "Sending authentication to %s", this.request.getUrl()), new Object[0]);
                    }
                    final Realm nr = new Realm.RealmBuilder().clone(realm).parseWWWAuthenticateHeader(wwwAuth).setUri(URI.create(this.request.getUrl()).getPath()).setMethodName(this.request.getReqType()).setScheme(realm.getAuthScheme()).setUsePreemptiveAuth(true).build();
                    final RequestBuilder builder2 = new RequestBuilder(this.request);
                    this.request = builder2.setRealm(nr).build();
                    this.urlConnection = JDKAsyncHttpProvider.this.createUrlConnection(this.request);
                    return this.call();
                }
                state = this.asyncHandler.onStatusReceived(new ResponseStatus(uri, this.urlConnection, JDKAsyncHttpProvider.this));
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
                    final int[] lengthWrapper = { 0 };
                    final byte[] bytes = AsyncHttpProviderUtils.readFully(is, lengthWrapper);
                    if (lengthWrapper[0] > 0) {
                        final byte[] body = new byte[lengthWrapper[0]];
                        System.arraycopy(bytes, 0, body, 0, lengthWrapper[0]);
                        this.future.touch();
                        this.asyncHandler.onBodyPartReceived(new ResponseBodyPart(uri, body, JDKAsyncHttpProvider.this));
                    }
                }
                if (ProgressAsyncHandler.class.isAssignableFrom(this.asyncHandler.getClass())) {
                    ProgressAsyncHandler.class.cast(this.asyncHandler).onHeaderWriteCompleted();
                    ProgressAsyncHandler.class.cast(this.asyncHandler).onContentWriteCompleted();
                }
                try {
                    return this.asyncHandler.onCompleted();
                }
                catch (Throwable t) {
                    final RuntimeException ex = new RuntimeException();
                    ex.initCause(t);
                    throw ex;
                }
            }
            catch (Throwable t2) {
                if (JDKAsyncHttpProvider.logger.isDebugEnabled()) {
                    JDKAsyncHttpProvider.logger.debug(t2);
                }
                try {
                    this.future.abort(this.filterException(t2));
                }
                catch (Throwable t3) {
                    JDKAsyncHttpProvider.logger.error(t3);
                }
            }
            finally {
                if (JDKAsyncHttpProvider.this.config.getMaxTotalConnections() != -1) {
                    JDKAsyncHttpProvider.this.maxConnections.decrementAndGet();
                }
                this.urlConnection.disconnect();
                if (JDKAsyncHttpProvider.this.jdkNtlmDomain != null) {
                    System.setProperty("http.auth.ntlm.domain", JDKAsyncHttpProvider.this.jdkNtlmDomain);
                }
                Authenticator.setDefault(JDKAsyncHttpProvider.this.jdkAuthenticator);
            }
            return null;
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
            final String method = request.getReqType();
            if (request.getVirtualHost() != null) {
                host = request.getVirtualHost();
            }
            if (uri.getPort() == -1) {
                urlConnection.setRequestProperty("Host", host);
            }
            else {
                urlConnection.setRequestProperty("Host", host + ":" + uri.getPort());
            }
            if (JDKAsyncHttpProvider.this.config.isCompressionEnabled()) {
                urlConnection.setRequestProperty("Accept-Encoding", "gzip");
            }
            final boolean contentTypeSet = false;
            if (!method.equalsIgnoreCase("CONNECT")) {
                final FluentCaseInsensitiveStringsMap h = request.getHeaders();
                if (h != null) {
                    for (final String name : h.keySet()) {
                        if (!"host".equalsIgnoreCase(name)) {
                            for (final String value : h.get((Object)name)) {
                                urlConnection.setRequestProperty(name, value);
                            }
                        }
                    }
                }
            }
            final String ka = JDKAsyncHttpProvider.this.config.getAllowPoolingConnection() ? "keep-alive" : "close";
            urlConnection.setRequestProperty("Connection", ka);
            final ProxyServer proxyServer = (request.getProxyServer() != null) ? request.getProxyServer() : JDKAsyncHttpProvider.this.config.getProxyServer();
            if (proxyServer != null) {
                urlConnection.setRequestProperty("Proxy-Connection", ka);
                if (proxyServer.getPrincipal() != null) {
                    urlConnection.setRequestProperty("Proxy-Authorization", AuthenticatorUtils.computeBasicAuthentication(proxyServer));
                }
            }
            final Realm realm = (request.getRealm() != null) ? request.getRealm() : JDKAsyncHttpProvider.this.config.getRealm();
            Label_0573: {
                if (realm != null && realm.getUsePreemptiveAuth()) {
                    switch (realm.getAuthScheme()) {
                        case BASIC: {
                            urlConnection.setRequestProperty("Authorization", AuthenticatorUtils.computeBasicAuthentication(realm));
                            break Label_0573;
                        }
                        case DIGEST: {
                            if (realm.getNonce() != null && !realm.getNonce().equals("")) {
                                try {
                                    urlConnection.setRequestProperty("Authorization", AuthenticatorUtils.computeDigestAuthentication(realm));
                                    break Label_0573;
                                }
                                catch (NoSuchAlgorithmException e) {
                                    throw new SecurityException(e);
                                }
                                break;
                            }
                            break Label_0573;
                        }
                    }
                    throw new IllegalStateException(String.format(AsyncHttpProviderUtils.currentThread() + "Invalid Authentication %s", realm.toString()));
                }
            }
            if (realm != null && realm.getDomain() != null && realm.getScheme() == Realm.AuthScheme.NTLM) {
                JDKAsyncHttpProvider.this.jdkNtlmDomain = System.getProperty("http.auth.ntlm.domain");
                System.setProperty("http.auth.ntlm.domain", realm.getDomain());
            }
            if (request.getHeaders().getFirstValue("Accept") == null) {
                urlConnection.setRequestProperty("Accept", "*/*");
            }
            if (request.getHeaders().getFirstValue("User-Agent") == null && JDKAsyncHttpProvider.this.config.getUserAgent() != null) {
                urlConnection.setRequestProperty("User-Agent", JDKAsyncHttpProvider.this.config.getUserAgent());
            }
            else {
                urlConnection.setRequestProperty("User-Agent", AsyncHttpProviderUtils.constructUserAgent(JDKAsyncHttpProvider.class));
            }
            if (request.getCookies() != null && !request.getCookies().isEmpty()) {
                urlConnection.setRequestProperty("Cookie", AsyncHttpProviderUtils.encodeCookies(request.getCookies()));
            }
            final String reqType = request.getReqType();
            urlConnection.setRequestMethod(reqType);
            if ("POST".equals(reqType) || "PUT".equals(reqType)) {
                urlConnection.setRequestProperty("Content-Length", "0");
                urlConnection.setDoOutput(true);
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
                    urlConnection.setRequestProperty("Content-Length", String.valueOf(request.getStringData().length()));
                    urlConnection.getOutputStream().write(request.getStringData().getBytes("UTF-8"));
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
                    urlConnection.getOutputStream().write(sb.toString().getBytes("UTF-8"));
                }
                else if (request.getParts() != null) {
                    int lenght = (int)request.getLength();
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
                    final ChannelBuffer b = ChannelBuffers.dynamicBuffer(lenght);
                    mre.writeRequest(urlConnection.getOutputStream());
                }
                else if (request.getEntityWriter() != null) {
                    final int lenght = (int)request.getLength();
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
                            length = (int)request.getLength();
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
                            JDKAsyncHttpProvider.logger.warn(e2, "Failed to close request body: %s", e2.getMessage());
                        }
                    }
                }
            }
        }
    }
}
