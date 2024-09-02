// 
// Decompiled by Procyon v0.5.36
// 

package com.ning.http.client;

import com.ning.http.client.simple.HeaderMap;
import com.ning.http.client.resumable.ResumableAsyncHandler;
import com.ning.http.client.filter.IOExceptionFilter;
import com.ning.http.client.resumable.ResumableIOExceptionFilter;
import javax.net.ssl.SSLContext;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.Collection;
import java.util.Map;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.concurrent.Future;
import com.ning.http.client.simple.SimpleAHCTransferListener;
import org.slf4j.Logger;

public class SimpleAsyncHttpClient
{
    private static final Logger logger;
    private final AsyncHttpClientConfig config;
    private final RequestBuilder requestBuilder;
    private AsyncHttpClient asyncHttpClient;
    private final ThrowableHandler defaultThrowableHandler;
    private final boolean resumeEnabled;
    private final ErrorDocumentBehaviour errorDocumentBehaviour;
    private final SimpleAHCTransferListener listener;
    private final boolean derived;
    
    private SimpleAsyncHttpClient(final AsyncHttpClientConfig config, final RequestBuilder requestBuilder, final ThrowableHandler defaultThrowableHandler, final ErrorDocumentBehaviour errorDocumentBehaviour, final boolean resumeEnabled, final AsyncHttpClient ahc, final SimpleAHCTransferListener listener) {
        this.config = config;
        this.requestBuilder = requestBuilder;
        this.defaultThrowableHandler = defaultThrowableHandler;
        this.resumeEnabled = resumeEnabled;
        this.errorDocumentBehaviour = errorDocumentBehaviour;
        this.asyncHttpClient = ahc;
        this.listener = listener;
        this.derived = (ahc != null);
    }
    
    public Future<Response> post(final Part... parts) throws IOException {
        final RequestBuilder r = this.rebuildRequest(this.requestBuilder.build());
        r.setMethod("POST");
        for (final Part part : parts) {
            r.addBodyPart(part);
        }
        return this.execute(r, null, null);
    }
    
    public Future<Response> post(final BodyConsumer consumer, final Part... parts) throws IOException {
        final RequestBuilder r = this.rebuildRequest(this.requestBuilder.build());
        r.setMethod("POST");
        for (final Part part : parts) {
            r.addBodyPart(part);
        }
        return this.execute(r, consumer, null);
    }
    
    public Future<Response> post(final BodyGenerator bodyGenerator) throws IOException {
        final RequestBuilder r = this.rebuildRequest(this.requestBuilder.build());
        r.setMethod("POST");
        r.setBody(bodyGenerator);
        return this.execute(r, null, null);
    }
    
    public Future<Response> post(final BodyGenerator bodyGenerator, final ThrowableHandler throwableHandler) throws IOException {
        final RequestBuilder r = this.rebuildRequest(this.requestBuilder.build());
        r.setMethod("POST");
        r.setBody(bodyGenerator);
        return this.execute(r, null, throwableHandler);
    }
    
    public Future<Response> post(final BodyGenerator bodyGenerator, final BodyConsumer bodyConsumer) throws IOException {
        final RequestBuilder r = this.rebuildRequest(this.requestBuilder.build());
        r.setMethod("POST");
        r.setBody(bodyGenerator);
        return this.execute(r, bodyConsumer, null);
    }
    
    public Future<Response> post(final BodyGenerator bodyGenerator, final BodyConsumer bodyConsumer, final ThrowableHandler throwableHandler) throws IOException {
        final RequestBuilder r = this.rebuildRequest(this.requestBuilder.build());
        r.setMethod("POST");
        r.setBody(bodyGenerator);
        return this.execute(r, bodyConsumer, throwableHandler);
    }
    
    public Future<Response> put(final Part... parts) throws IOException {
        final RequestBuilder r = this.rebuildRequest(this.requestBuilder.build());
        r.setMethod("POST");
        for (final Part part : parts) {
            r.addBodyPart(part);
        }
        return this.execute(r, null, null);
    }
    
    public Future<Response> put(final BodyConsumer consumer, final Part... parts) throws IOException {
        final RequestBuilder r = this.rebuildRequest(this.requestBuilder.build());
        r.setMethod("POST");
        for (final Part part : parts) {
            r.addBodyPart(part);
        }
        return this.execute(r, consumer, null);
    }
    
    public Future<Response> put(final BodyGenerator bodyGenerator, final BodyConsumer bodyConsumer) throws IOException {
        final RequestBuilder r = this.rebuildRequest(this.requestBuilder.build());
        r.setMethod("PUT");
        r.setBody(bodyGenerator);
        return this.execute(r, bodyConsumer, null);
    }
    
    public Future<Response> put(final BodyGenerator bodyGenerator, final BodyConsumer bodyConsumer, final ThrowableHandler throwableHandler) throws IOException {
        final RequestBuilder r = this.rebuildRequest(this.requestBuilder.build());
        r.setMethod("PUT");
        r.setBody(bodyGenerator);
        return this.execute(r, bodyConsumer, throwableHandler);
    }
    
    public Future<Response> put(final BodyGenerator bodyGenerator) throws IOException {
        final RequestBuilder r = this.rebuildRequest(this.requestBuilder.build());
        r.setMethod("PUT");
        r.setBody(bodyGenerator);
        return this.execute(r, null, null);
    }
    
    public Future<Response> put(final BodyGenerator bodyGenerator, final ThrowableHandler throwableHandler) throws IOException {
        final RequestBuilder r = this.rebuildRequest(this.requestBuilder.build());
        r.setMethod("PUT");
        r.setBody(bodyGenerator);
        return this.execute(r, null, throwableHandler);
    }
    
    public Future<Response> get() throws IOException {
        final RequestBuilder r = this.rebuildRequest(this.requestBuilder.build());
        return this.execute(r, null, null);
    }
    
    public Future<Response> get(final ThrowableHandler throwableHandler) throws IOException {
        final RequestBuilder r = this.rebuildRequest(this.requestBuilder.build());
        return this.execute(r, null, throwableHandler);
    }
    
    public Future<Response> get(final BodyConsumer bodyConsumer) throws IOException {
        final RequestBuilder r = this.rebuildRequest(this.requestBuilder.build());
        return this.execute(r, bodyConsumer, null);
    }
    
    public Future<Response> get(final BodyConsumer bodyConsumer, final ThrowableHandler throwableHandler) throws IOException {
        final RequestBuilder r = this.rebuildRequest(this.requestBuilder.build());
        return this.execute(r, bodyConsumer, throwableHandler);
    }
    
    public Future<Response> delete() throws IOException {
        final RequestBuilder r = this.rebuildRequest(this.requestBuilder.build());
        r.setMethod("DELETE");
        return this.execute(r, null, null);
    }
    
    public Future<Response> delete(final ThrowableHandler throwableHandler) throws IOException {
        final RequestBuilder r = this.rebuildRequest(this.requestBuilder.build());
        r.setMethod("DELETE");
        return this.execute(r, null, throwableHandler);
    }
    
    public Future<Response> delete(final BodyConsumer bodyConsumer) throws IOException {
        final RequestBuilder r = this.rebuildRequest(this.requestBuilder.build());
        r.setMethod("DELETE");
        return this.execute(r, bodyConsumer, null);
    }
    
    public Future<Response> delete(final BodyConsumer bodyConsumer, final ThrowableHandler throwableHandler) throws IOException {
        final RequestBuilder r = this.rebuildRequest(this.requestBuilder.build());
        r.setMethod("DELETE");
        return this.execute(r, bodyConsumer, throwableHandler);
    }
    
    public Future<Response> head() throws IOException {
        final RequestBuilder r = this.rebuildRequest(this.requestBuilder.build());
        r.setMethod("HEAD");
        return this.execute(r, null, null);
    }
    
    public Future<Response> head(final ThrowableHandler throwableHandler) throws IOException {
        final RequestBuilder r = this.rebuildRequest(this.requestBuilder.build());
        r.setMethod("HEAD");
        return this.execute(r, null, throwableHandler);
    }
    
    public Future<Response> options() throws IOException {
        final RequestBuilder r = this.rebuildRequest(this.requestBuilder.build());
        r.setMethod("OPTIONS");
        return this.execute(r, null, null);
    }
    
    public Future<Response> options(final ThrowableHandler throwableHandler) throws IOException {
        final RequestBuilder r = this.rebuildRequest(this.requestBuilder.build());
        r.setMethod("OPTIONS");
        return this.execute(r, null, throwableHandler);
    }
    
    public Future<Response> options(final BodyConsumer bodyConsumer) throws IOException {
        final RequestBuilder r = this.rebuildRequest(this.requestBuilder.build());
        r.setMethod("OPTIONS");
        return this.execute(r, bodyConsumer, null);
    }
    
    public Future<Response> options(final BodyConsumer bodyConsumer, final ThrowableHandler throwableHandler) throws IOException {
        final RequestBuilder r = this.rebuildRequest(this.requestBuilder.build());
        r.setMethod("OPTIONS");
        return this.execute(r, bodyConsumer, throwableHandler);
    }
    
    private RequestBuilder rebuildRequest(final Request rb) {
        return new RequestBuilder(rb);
    }
    
    private Future<Response> execute(final RequestBuilder rb, final BodyConsumer bodyConsumer, ThrowableHandler throwableHandler) throws IOException {
        if (throwableHandler == null) {
            throwableHandler = this.defaultThrowableHandler;
        }
        final Request request = rb.build();
        ProgressAsyncHandler<Response> handler = new BodyConsumerAsyncHandler(bodyConsumer, throwableHandler, this.errorDocumentBehaviour, request.getUrl(), this.listener);
        if (this.resumeEnabled && request.getMethod().equals("GET") && bodyConsumer != null && bodyConsumer instanceof ResumableBodyConsumer) {
            final ResumableBodyConsumer fileBodyConsumer = (ResumableBodyConsumer)bodyConsumer;
            final long length = fileBodyConsumer.getTransferredBytes();
            fileBodyConsumer.resume();
            handler = new ResumableBodyConsumerAsyncHandler(length, handler);
        }
        return this.asyncHttpClient().executeRequest(request, handler);
    }
    
    private AsyncHttpClient asyncHttpClient() {
        synchronized (this.config) {
            if (this.asyncHttpClient == null) {
                this.asyncHttpClient = new AsyncHttpClient(this.config);
            }
        }
        return this.asyncHttpClient;
    }
    
    public void close() {
        if (!this.derived && this.asyncHttpClient != null) {
            this.asyncHttpClient.close();
        }
    }
    
    public DerivedBuilder derive() {
        return new Builder(this);
    }
    
    static {
        logger = LoggerFactory.getLogger((Class)SimpleAsyncHttpClient.class);
    }
    
    public enum ErrorDocumentBehaviour
    {
        WRITE, 
        ACCUMULATE, 
        OMIT;
    }
    
    public static final class Builder implements DerivedBuilder
    {
        private final RequestBuilder requestBuilder;
        private final AsyncHttpClientConfig.Builder configBuilder;
        private Realm.RealmBuilder realmBuilder;
        private ProxyServer.Protocol proxyProtocol;
        private String proxyHost;
        private String proxyPrincipal;
        private String proxyPassword;
        private int proxyPort;
        private ThrowableHandler defaultThrowableHandler;
        private boolean enableResumableDownload;
        private ErrorDocumentBehaviour errorDocumentBehaviour;
        private AsyncHttpClient ahc;
        private SimpleAHCTransferListener listener;
        
        public Builder() {
            this.configBuilder = new AsyncHttpClientConfig.Builder();
            this.realmBuilder = null;
            this.proxyProtocol = null;
            this.proxyHost = null;
            this.proxyPrincipal = null;
            this.proxyPassword = null;
            this.proxyPort = 80;
            this.defaultThrowableHandler = null;
            this.enableResumableDownload = false;
            this.errorDocumentBehaviour = ErrorDocumentBehaviour.WRITE;
            this.ahc = null;
            this.listener = null;
            this.requestBuilder = new RequestBuilder("GET");
        }
        
        private Builder(final SimpleAsyncHttpClient client) {
            this.configBuilder = new AsyncHttpClientConfig.Builder();
            this.realmBuilder = null;
            this.proxyProtocol = null;
            this.proxyHost = null;
            this.proxyPrincipal = null;
            this.proxyPassword = null;
            this.proxyPort = 80;
            this.defaultThrowableHandler = null;
            this.enableResumableDownload = false;
            this.errorDocumentBehaviour = ErrorDocumentBehaviour.WRITE;
            this.ahc = null;
            this.listener = null;
            this.requestBuilder = new RequestBuilder(client.requestBuilder.build());
            this.defaultThrowableHandler = client.defaultThrowableHandler;
            this.errorDocumentBehaviour = client.errorDocumentBehaviour;
            this.enableResumableDownload = client.resumeEnabled;
            this.ahc = client.asyncHttpClient();
            this.listener = client.listener;
        }
        
        public Builder addBodyPart(final Part part) throws IllegalArgumentException {
            this.requestBuilder.addBodyPart(part);
            return this;
        }
        
        public Builder addCookie(final Cookie cookie) {
            this.requestBuilder.addCookie(cookie);
            return this;
        }
        
        public Builder addHeader(final String name, final String value) {
            this.requestBuilder.addHeader(name, value);
            return this;
        }
        
        public Builder addParameter(final String key, final String value) throws IllegalArgumentException {
            this.requestBuilder.addParameter(key, value);
            return this;
        }
        
        public Builder addQueryParameter(final String name, final String value) {
            this.requestBuilder.addQueryParameter(name, value);
            return this;
        }
        
        public Builder setHeader(final String name, final String value) {
            this.requestBuilder.setHeader(name, value);
            return this;
        }
        
        public Builder setHeaders(final FluentCaseInsensitiveStringsMap headers) {
            this.requestBuilder.setHeaders(headers);
            return this;
        }
        
        public Builder setHeaders(final Map<String, Collection<String>> headers) {
            this.requestBuilder.setHeaders(headers);
            return this;
        }
        
        public Builder setParameters(final Map<String, Collection<String>> parameters) throws IllegalArgumentException {
            this.requestBuilder.setParameters(parameters);
            return this;
        }
        
        public Builder setParameters(final FluentStringsMap parameters) throws IllegalArgumentException {
            this.requestBuilder.setParameters(parameters);
            return this;
        }
        
        public Builder setUrl(final String url) {
            this.requestBuilder.setUrl(url);
            return this;
        }
        
        public Builder setVirtualHost(final String virtualHost) {
            this.requestBuilder.setVirtualHost(virtualHost);
            return this;
        }
        
        public Builder setFollowRedirects(final boolean followRedirects) {
            this.requestBuilder.setFollowRedirects(followRedirects);
            return this;
        }
        
        public Builder setMaximumConnectionsTotal(final int defaultMaxTotalConnections) {
            this.configBuilder.setMaximumConnectionsTotal(defaultMaxTotalConnections);
            return this;
        }
        
        public Builder setMaximumConnectionsPerHost(final int defaultMaxConnectionPerHost) {
            this.configBuilder.setMaximumConnectionsPerHost(defaultMaxConnectionPerHost);
            return this;
        }
        
        public Builder setConnectionTimeoutInMs(final int connectionTimeuot) {
            this.configBuilder.setConnectionTimeoutInMs(connectionTimeuot);
            return this;
        }
        
        public Builder setIdleConnectionInPoolTimeoutInMs(final int defaultIdleConnectionInPoolTimeoutInMs) {
            this.configBuilder.setIdleConnectionInPoolTimeoutInMs(defaultIdleConnectionInPoolTimeoutInMs);
            return this;
        }
        
        public Builder setRequestTimeoutInMs(final int defaultRequestTimeoutInMs) {
            this.configBuilder.setRequestTimeoutInMs(defaultRequestTimeoutInMs);
            return this;
        }
        
        public Builder setMaximumNumberOfRedirects(final int maxDefaultRedirects) {
            this.configBuilder.setMaximumNumberOfRedirects(maxDefaultRedirects);
            return this;
        }
        
        public Builder setCompressionEnabled(final boolean compressionEnabled) {
            this.configBuilder.setCompressionEnabled(compressionEnabled);
            return this;
        }
        
        public Builder setUserAgent(final String userAgent) {
            this.configBuilder.setUserAgent(userAgent);
            return this;
        }
        
        public Builder setAllowPoolingConnection(final boolean allowPoolingConnection) {
            this.configBuilder.setAllowPoolingConnection(allowPoolingConnection);
            return this;
        }
        
        public Builder setScheduledExecutorService(final ScheduledExecutorService reaper) {
            this.configBuilder.setScheduledExecutorService(reaper);
            return this;
        }
        
        public Builder setExecutorService(final ExecutorService applicationThreadPool) {
            this.configBuilder.setExecutorService(applicationThreadPool);
            return this;
        }
        
        public Builder setSSLEngineFactory(final SSLEngineFactory sslEngineFactory) {
            this.configBuilder.setSSLEngineFactory(sslEngineFactory);
            return this;
        }
        
        public Builder setSSLContext(final SSLContext sslContext) {
            this.configBuilder.setSSLContext(sslContext);
            return this;
        }
        
        public Builder setRequestCompressionLevel(final int requestCompressionLevel) {
            this.configBuilder.setRequestCompressionLevel(requestCompressionLevel);
            return this;
        }
        
        public Builder setRealmDomain(final String domain) {
            this.realm().setDomain(domain);
            return this;
        }
        
        public Builder setRealmPrincipal(final String principal) {
            this.realm().setPrincipal(principal);
            return this;
        }
        
        public Builder setRealmPassword(final String password) {
            this.realm().setPassword(password);
            return this;
        }
        
        public Builder setRealmScheme(final Realm.AuthScheme scheme) {
            this.realm().setScheme(scheme);
            return this;
        }
        
        public Builder setRealmName(final String realmName) {
            this.realm().setRealmName(realmName);
            return this;
        }
        
        public Builder setRealmUsePreemptiveAuth(final boolean usePreemptiveAuth) {
            this.realm().setUsePreemptiveAuth(usePreemptiveAuth);
            return this;
        }
        
        public Builder setRealmEnconding(final String enc) {
            this.realm().setEnconding(enc);
            return this;
        }
        
        public Builder setProxyProtocol(final ProxyServer.Protocol protocol) {
            this.proxyProtocol = protocol;
            return this;
        }
        
        public Builder setProxyHost(final String host) {
            this.proxyHost = host;
            return this;
        }
        
        public Builder setProxyPrincipal(final String principal) {
            this.proxyPrincipal = principal;
            return this;
        }
        
        public Builder setProxyPassword(final String password) {
            this.proxyPassword = password;
            return this;
        }
        
        public Builder setProxyPort(final int port) {
            this.proxyPort = port;
            return this;
        }
        
        public Builder setDefaultThrowableHandler(final ThrowableHandler throwableHandler) {
            this.defaultThrowableHandler = throwableHandler;
            return this;
        }
        
        public Builder setErrorDocumentBehaviour(final ErrorDocumentBehaviour behaviour) {
            this.errorDocumentBehaviour = behaviour;
            return this;
        }
        
        public Builder setResumableDownload(final boolean enableResumableDownload) {
            this.enableResumableDownload = enableResumableDownload;
            return this;
        }
        
        private Realm.RealmBuilder realm() {
            if (this.realmBuilder == null) {
                this.realmBuilder = new Realm.RealmBuilder();
            }
            return this.realmBuilder;
        }
        
        public Builder setListener(final SimpleAHCTransferListener listener) {
            this.listener = listener;
            return this;
        }
        
        public Builder setMaxRequestRetry(final int maxRequestRetry) {
            this.configBuilder.setMaxRequestRetry(maxRequestRetry);
            return this;
        }
        
        public SimpleAsyncHttpClient build() {
            if (this.realmBuilder != null) {
                this.configBuilder.setRealm(this.realmBuilder.build());
            }
            if (this.proxyHost != null) {
                this.configBuilder.setProxyServer(new ProxyServer(this.proxyProtocol, this.proxyHost, this.proxyPort, this.proxyPrincipal, this.proxyPassword));
            }
            this.configBuilder.addIOExceptionFilter(new ResumableIOExceptionFilter());
            final SimpleAsyncHttpClient sc = new SimpleAsyncHttpClient(this.configBuilder.build(), this.requestBuilder, this.defaultThrowableHandler, this.errorDocumentBehaviour, this.enableResumableDownload, this.ahc, this.listener, null);
            return sc;
        }
    }
    
    private static final class ResumableBodyConsumerAsyncHandler extends ResumableAsyncHandler<Response> implements ProgressAsyncHandler<Response>
    {
        private final ProgressAsyncHandler<Response> delegate;
        
        public ResumableBodyConsumerAsyncHandler(final long byteTransferred, final ProgressAsyncHandler<Response> delegate) {
            super(byteTransferred, delegate);
            this.delegate = delegate;
        }
        
        public AsyncHandler.STATE onHeaderWriteCompleted() {
            return this.delegate.onHeaderWriteCompleted();
        }
        
        public AsyncHandler.STATE onContentWriteCompleted() {
            return this.delegate.onContentWriteCompleted();
        }
        
        public AsyncHandler.STATE onContentWriteProgress(final long amount, final long current, final long total) {
            return this.delegate.onContentWriteProgress(amount, current, total);
        }
    }
    
    private static final class BodyConsumerAsyncHandler extends AsyncCompletionHandlerBase
    {
        private final BodyConsumer bodyConsumer;
        private final ThrowableHandler exceptionHandler;
        private final ErrorDocumentBehaviour errorDocumentBehaviour;
        private final String url;
        private final SimpleAHCTransferListener listener;
        private boolean accumulateBody;
        private boolean omitBody;
        private int amount;
        private long total;
        
        public BodyConsumerAsyncHandler(final BodyConsumer bodyConsumer, final ThrowableHandler exceptionHandler, final ErrorDocumentBehaviour errorDocumentBehaviour, final String url, final SimpleAHCTransferListener listener) {
            this.accumulateBody = false;
            this.omitBody = false;
            this.amount = 0;
            this.total = -1L;
            this.bodyConsumer = bodyConsumer;
            this.exceptionHandler = exceptionHandler;
            this.errorDocumentBehaviour = errorDocumentBehaviour;
            this.url = url;
            this.listener = listener;
        }
        
        @Override
        public void onThrowable(final Throwable t) {
            try {
                if (this.exceptionHandler != null) {
                    this.exceptionHandler.onThrowable(t);
                }
                else {
                    super.onThrowable(t);
                }
            }
            finally {
                this.closeConsumer();
            }
        }
        
        @Override
        public AsyncHandler.STATE onBodyPartReceived(final HttpResponseBodyPart content) throws Exception {
            this.fireReceived(content);
            if (this.omitBody) {
                return AsyncHandler.STATE.CONTINUE;
            }
            if (!this.accumulateBody && this.bodyConsumer != null) {
                this.bodyConsumer.consume(content.getBodyByteBuffer());
                return AsyncHandler.STATE.CONTINUE;
            }
            return super.onBodyPartReceived(content);
        }
        
        @Override
        public Response onCompleted(final Response response) throws Exception {
            this.fireCompleted(response);
            this.closeConsumer();
            return super.onCompleted(response);
        }
        
        private void closeConsumer() {
            try {
                if (this.bodyConsumer != null) {
                    this.bodyConsumer.close();
                }
            }
            catch (IOException ex) {
                SimpleAsyncHttpClient.logger.warn("Unable to close a BodyConsumer {}", (Object)this.bodyConsumer);
            }
        }
        
        @Override
        public AsyncHandler.STATE onStatusReceived(final HttpResponseStatus status) throws Exception {
            this.fireStatus(status);
            if (this.isErrorStatus(status)) {
                switch (this.errorDocumentBehaviour) {
                    case ACCUMULATE: {
                        this.accumulateBody = true;
                        break;
                    }
                    case OMIT: {
                        this.omitBody = true;
                        break;
                    }
                }
            }
            return super.onStatusReceived(status);
        }
        
        private boolean isErrorStatus(final HttpResponseStatus status) {
            return status.getStatusCode() >= 400;
        }
        
        @Override
        public AsyncHandler.STATE onHeadersReceived(final HttpResponseHeaders headers) throws Exception {
            this.calculateTotal(headers);
            this.fireHeaders(headers);
            return super.onHeadersReceived(headers);
        }
        
        private void calculateTotal(final HttpResponseHeaders headers) {
            final String length = headers.getHeaders().getFirstValue("Content-Length");
            try {
                this.total = Integer.valueOf(length);
            }
            catch (Exception e) {
                this.total = -1L;
            }
        }
        
        @Override
        public AsyncHandler.STATE onContentWriteProgress(final long amount, final long current, final long total) {
            this.fireSent(this.url, amount, current, total);
            return super.onContentWriteProgress(amount, current, total);
        }
        
        private void fireStatus(final HttpResponseStatus status) {
            if (this.listener != null) {
                this.listener.onStatus(this.url, status.getStatusCode(), status.getStatusText());
            }
        }
        
        private void fireReceived(final HttpResponseBodyPart content) {
            final int remaining = content.getBodyByteBuffer().remaining();
            this.amount += remaining;
            if (this.listener != null) {
                this.listener.onBytesReceived(this.url, this.amount, remaining, this.total);
            }
        }
        
        private void fireHeaders(final HttpResponseHeaders headers) {
            if (this.listener != null) {
                this.listener.onHeaders(this.url, new HeaderMap(headers.getHeaders()));
            }
        }
        
        private void fireSent(final String url, final long amount, final long current, final long total) {
            if (this.listener != null) {
                this.listener.onBytesSent(url, amount, current, total);
            }
        }
        
        private void fireCompleted(final Response response) {
            if (this.listener != null) {
                this.listener.onCompleted(this.url, response.getStatusCode(), response.getStatusText());
            }
        }
    }
    
    public interface DerivedBuilder
    {
        DerivedBuilder setFollowRedirects(final boolean p0);
        
        DerivedBuilder setVirtualHost(final String p0);
        
        DerivedBuilder setUrl(final String p0);
        
        DerivedBuilder setParameters(final FluentStringsMap p0) throws IllegalArgumentException;
        
        DerivedBuilder setParameters(final Map<String, Collection<String>> p0) throws IllegalArgumentException;
        
        DerivedBuilder setHeaders(final Map<String, Collection<String>> p0);
        
        DerivedBuilder setHeaders(final FluentCaseInsensitiveStringsMap p0);
        
        DerivedBuilder setHeader(final String p0, final String p1);
        
        DerivedBuilder addQueryParameter(final String p0, final String p1);
        
        DerivedBuilder addParameter(final String p0, final String p1) throws IllegalArgumentException;
        
        DerivedBuilder addHeader(final String p0, final String p1);
        
        DerivedBuilder addCookie(final Cookie p0);
        
        DerivedBuilder addBodyPart(final Part p0) throws IllegalArgumentException;
        
        DerivedBuilder setResumableDownload(final boolean p0);
        
        SimpleAsyncHttpClient build();
    }
}
