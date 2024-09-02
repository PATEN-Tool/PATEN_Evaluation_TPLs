// 
// Decompiled by Procyon v0.5.36
// 

package io.vertx.core.http.impl;

import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;
import java.util.ArrayList;
import io.netty.buffer.Unpooled;
import io.netty.buffer.CompositeByteBuf;
import io.vertx.core.http.HttpHeaders;
import java.util.Objects;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.impl.Arguments;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.StreamPriority;
import io.vertx.core.http.impl.headers.VertxHttpHeaders;
import io.vertx.core.AsyncResult;
import java.util.List;
import io.netty.buffer.ByteBuf;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpConnection;
import io.vertx.core.Handler;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.logging.Logger;
import io.vertx.core.http.HttpClientRequest;

public class HttpClientRequestImpl extends HttpClientRequestBase implements HttpClientRequest
{
    static final Logger log;
    private final VertxInternal vertx;
    private boolean chunked;
    private String hostHeader;
    private String rawMethod;
    private Handler<Void> continueHandler;
    private Handler<Void> drainHandler;
    private Handler<HttpClientRequest> pushHandler;
    private Handler<HttpConnection> connectionHandler;
    private Handler<Throwable> exceptionHandler;
    private Promise<Void> endPromise;
    private Future<Void> endFuture;
    private boolean ended;
    private Throwable reset;
    private ByteBuf pendingChunks;
    private List<Handler<AsyncResult<Void>>> pendingHandlers;
    private int pendingMaxSize;
    private int followRedirects;
    private VertxHttpHeaders headers;
    private StreamPriority priority;
    private HttpClientStream stream;
    private boolean connecting;
    private Handler<HttpClientResponse> respHandler;
    private Handler<Void> endHandler;
    
    HttpClientRequestImpl(final HttpClientImpl client, final boolean ssl, final HttpMethod method, final SocketAddress server, final String host, final int port, final String relativeURI, final VertxInternal vertx) {
        super(client, ssl, method, server, host, port, relativeURI);
        this.endPromise = Promise.promise();
        this.endFuture = this.endPromise.future();
        this.pendingMaxSize = -1;
        this.chunked = false;
        this.vertx = vertx;
        this.priority = HttpUtils.DEFAULT_STREAM_PRIORITY;
    }
    
    @Override
    public synchronized int streamId() {
        return (this.stream == null) ? -1 : this.stream.id();
    }
    
    @Override
    public synchronized HttpClientRequest handler(final Handler<HttpClientResponse> handler) {
        if (handler != null) {
            this.checkEnded();
        }
        this.respHandler = handler;
        return this;
    }
    
    @Override
    public synchronized HttpClientRequest setFollowRedirects(final boolean followRedirects) {
        this.checkEnded();
        if (followRedirects) {
            this.followRedirects = this.client.getOptions().getMaxRedirects() - 1;
        }
        else {
            this.followRedirects = 0;
        }
        return this;
    }
    
    @Override
    public synchronized HttpClientRequest setMaxRedirects(final int maxRedirects) {
        Arguments.require(maxRedirects >= 0, "Max redirects must be >= 0");
        this.checkEnded();
        this.followRedirects = maxRedirects;
        return this;
    }
    
    @Override
    public synchronized HttpClientRequest endHandler(final Handler<Void> handler) {
        if (handler != null) {
            this.checkEnded();
        }
        this.endHandler = handler;
        return this;
    }
    
    @Override
    public synchronized HttpClientRequestImpl setChunked(final boolean chunked) {
        this.checkEnded();
        if (this.stream != null) {
            throw new IllegalStateException("Cannot set chunked after data has been written on request");
        }
        if (this.client.getOptions().getProtocolVersion() != HttpVersion.HTTP_1_0) {
            this.chunked = chunked;
        }
        return this;
    }
    
    @Override
    public synchronized boolean isChunked() {
        return this.chunked;
    }
    
    @Override
    public synchronized String getRawMethod() {
        return this.rawMethod;
    }
    
    @Override
    public synchronized HttpClientRequest setRawMethod(final String method) {
        this.rawMethod = method;
        return this;
    }
    
    @Override
    public synchronized HttpClientRequest setHost(final String host) {
        this.hostHeader = host;
        return this;
    }
    
    @Override
    public synchronized String getHost() {
        return this.hostHeader;
    }
    
    @Override
    public synchronized MultiMap headers() {
        if (this.headers == null) {
            this.headers = new VertxHttpHeaders();
        }
        return this.headers;
    }
    
    @Override
    public synchronized HttpClientRequest putHeader(final String name, final String value) {
        this.checkEnded();
        this.headers().set(name, value);
        return this;
    }
    
    @Override
    public synchronized HttpClientRequest putHeader(final String name, final Iterable<String> values) {
        this.checkEnded();
        this.headers().set(name, values);
        return this;
    }
    
    @Override
    public synchronized HttpClientRequest setWriteQueueMaxSize(final int maxSize) {
        this.checkEnded();
        if (this.stream == null) {
            this.pendingMaxSize = maxSize;
        }
        else {
            this.stream.doSetWriteQueueMaxSize(maxSize);
        }
        return this;
    }
    
    @Override
    public synchronized boolean writeQueueFull() {
        this.checkEnded();
        synchronized (this) {
            this.checkEnded();
            if (this.stream == null) {
                return false;
            }
        }
        return this.stream.isNotWritable();
    }
    
    @Override
    public synchronized HttpClientRequest drainHandler(final Handler<Void> handler) {
        if (handler != null) {
            this.checkEnded();
            this.drainHandler = handler;
            if (this.stream == null) {
                return this;
            }
            this.stream.getContext().runOnContext(v -> {
                if (!this.stream.isNotWritable()) {
                    this.handleDrained();
                }
                return;
            });
        }
        else {
            this.drainHandler = null;
        }
        return this;
    }
    
    @Override
    public synchronized HttpClientRequest continueHandler(final Handler<Void> handler) {
        if (handler != null) {
            this.checkEnded();
        }
        this.continueHandler = handler;
        return this;
    }
    
    @Override
    public HttpClientRequest sendHead() {
        return this.sendHead(null);
    }
    
    @Override
    public synchronized HttpClientRequest sendHead(final Handler<HttpVersion> headersHandler) {
        this.checkEnded();
        this.checkResponseHandler();
        if (this.stream != null) {
            throw new IllegalStateException("Head already written");
        }
        this.connect(headersHandler);
        return this;
    }
    
    @Override
    public synchronized HttpClientRequest putHeader(final CharSequence name, final CharSequence value) {
        this.checkEnded();
        this.headers().set(name, value);
        return this;
    }
    
    @Override
    public synchronized HttpClientRequest putHeader(final CharSequence name, final Iterable<CharSequence> values) {
        this.checkEnded();
        this.headers().set(name, values);
        return this;
    }
    
    @Override
    public synchronized HttpClientRequest pushHandler(final Handler<HttpClientRequest> handler) {
        this.pushHandler = handler;
        return this;
    }
    
    @Override
    boolean reset(final Throwable cause) {
        final HttpClientStream s;
        synchronized (this) {
            if (this.reset != null) {
                return false;
            }
            this.reset = cause;
            s = this.stream;
        }
        if (s != null) {
            s.reset(cause);
        }
        else {
            this.handleException(cause);
        }
        return true;
    }
    
    private void tryComplete() {
        this.endPromise.tryComplete();
    }
    
    @Override
    public synchronized HttpConnection connection() {
        return (this.stream == null) ? null : this.stream.connection();
    }
    
    @Override
    public synchronized HttpClientRequest connectionHandler(final Handler<HttpConnection> handler) {
        this.connectionHandler = handler;
        return this;
    }
    
    @Override
    public HttpClientRequest writeCustomFrame(final int type, final int flags, final Buffer payload) {
        final HttpClientStream s;
        synchronized (this) {
            this.checkEnded();
            if ((s = this.stream) == null) {
                throw new IllegalStateException("Not yet connected");
            }
        }
        s.writeFrame(type, flags, payload.getByteBuf());
        return this;
    }
    
    void handleDrained() {
        final Handler<Void> handler;
        synchronized (this) {
            if ((handler = this.drainHandler) == null || this.endFuture.isComplete()) {
                return;
            }
        }
        try {
            handler.handle(null);
        }
        catch (Throwable t) {
            this.handleException(t);
        }
    }
    
    private void handleNextRequest(final HttpClientRequest next, final long timeoutMs) {
        next.handler(this.respHandler);
        next.exceptionHandler(this.exceptionHandler());
        this.exceptionHandler((Handler<Throwable>)null);
        next.endHandler(this.endHandler);
        next.pushHandler(this.pushHandler);
        next.setMaxRedirects(this.followRedirects - 1);
        if (next.getHost() == null) {
            next.setHost(this.hostHeader);
        }
        if (this.headers != null) {
            next.headers().addAll(this.headers);
        }
        this.endFuture.onComplete(ar -> {
            if (ar.succeeded()) {
                if (timeoutMs > 0L) {
                    next.setTimeout(timeoutMs);
                }
                next.end();
            }
            else {
                next.reset(0L);
            }
        });
    }
    
    @Override
    public void handleException(final Throwable t) {
        super.handleException(t);
        this.endPromise.tryFail(t);
    }
    
    @Override
    void handleResponse(final HttpClientResponse resp, final long timeoutMs) {
        if (this.reset == null) {
            final int statusCode = resp.statusCode();
            if (this.followRedirects > 0 && statusCode >= 300 && statusCode < 400) {
                final Future<HttpClientRequest> next = this.client.redirectHandler().apply(resp);
                if (next != null) {
                    next.onComplete(ar -> {
                        if (ar.succeeded()) {
                            this.handleNextRequest(ar.result(), timeoutMs);
                        }
                        else {
                            this.handleException(ar.cause());
                        }
                    });
                    return;
                }
            }
            if (this.respHandler != null) {
                this.respHandler.handle(resp);
            }
            if (this.endHandler != null) {
                this.endHandler.handle(null);
            }
        }
    }
    
    @Override
    protected String hostHeader() {
        return (this.hostHeader != null) ? this.hostHeader : super.hostHeader();
    }
    
    private synchronized void connect(final Handler<HttpVersion> headersHandler) {
        if (!this.connecting) {
            if (this.method == HttpMethod.OTHER && this.rawMethod == null) {
                throw new IllegalStateException("You must provide a rawMethod when using an HttpMethod.OTHER method");
            }
            SocketAddress peerAddress;
            if (this.hostHeader != null) {
                final int idx = this.hostHeader.lastIndexOf(58);
                if (idx != -1) {
                    peerAddress = SocketAddress.inetSocketAddress(Integer.parseInt(this.hostHeader.substring(idx + 1)), this.hostHeader.substring(0, idx));
                }
                else {
                    peerAddress = SocketAddress.inetSocketAddress(80, this.hostHeader);
                }
            }
            else {
                String peerHost = this.host;
                if (peerHost.endsWith(".")) {
                    peerHost = peerHost.substring(0, peerHost.length() - 1);
                }
                peerAddress = SocketAddress.inetSocketAddress(this.port, peerHost);
            }
            final Handler<HttpConnection> h1 = this.connectionHandler;
            final Handler<HttpConnection> h2 = this.client.connectionHandler();
            if (h1 != null) {
                if (h2 != null) {
                    final Handler<HttpConnection> handler;
                    final Handler<HttpConnection> handler2;
                    final Handler<HttpConnection> initializer = conn -> {
                        handler.handle(conn);
                        handler2.handle(conn);
                        return;
                    };
                }
                else {
                    final Handler<HttpConnection> initializer = h1;
                }
            }
            else {
                final Handler<HttpConnection> initializer = h2;
            }
            final ContextInternal connectCtx = this.vertx.getOrCreateContext();
            this.connecting = true;
            this.startTimeout();
            HttpClientStream stream;
            ContextInternal ctx;
            final Handler<HttpConnection> handler3;
            final ContextInternal contextInternal;
            this.client.getConnectionForRequest(connectCtx, peerAddress, this.ssl, this.server, ar1 -> {
                if (ar1.succeeded()) {
                    stream = ar1.result();
                    ctx = (ContextInternal)stream.getContext();
                    if (stream.id() == 1 && handler3 != null) {
                        ctx.executeFromIO(v -> handler3.handle(stream.connection()));
                    }
                    if (this.reset != null) {
                        stream.reset(this.reset);
                    }
                    else {
                        ctx.executeFromIO(v -> this.connected(headersHandler, stream));
                    }
                }
                else {
                    contextInternal.executeFromIO(v -> this.handleException(ar1.cause()));
                }
            });
        }
    }
    
    private void connected(final Handler<HttpVersion> headersHandler, final HttpClientStream stream) {
        synchronized (this) {
            (this.stream = stream).beginRequest(this);
            if (this.pendingMaxSize != -1) {
                stream.doSetWriteQueueMaxSize(this.pendingMaxSize);
            }
            ByteBuf pending = null;
            Handler<AsyncResult<Void>> handler = null;
            if (this.pendingChunks != null) {
                final List<Handler<AsyncResult<Void>>> handlers = this.pendingHandlers;
                this.pendingHandlers = null;
                pending = this.pendingChunks;
                this.pendingChunks = null;
                if (handlers != null) {
                    handler = (ar -> handlers.forEach(h -> h.handle(ar)));
                }
            }
            stream.writeHead(this.method, this.rawMethod, this.uri, this.headers, this.hostHeader(), this.chunked, pending, this.ended, this.priority, this.continueHandler, handler);
            if (this.ended) {
                stream.endRequest();
                this.tryComplete();
            }
            this.connecting = false;
            this.stream = stream;
        }
        if (headersHandler != null) {
            headersHandler.handle(stream.version());
        }
    }
    
    @Override
    public synchronized HttpClientRequest setTimeout(final long timeoutMs) {
        super.setTimeout(timeoutMs);
        if (this.connecting || this.stream != null) {
            this.startTimeout();
        }
        return this;
    }
    
    @Override
    public void end(final String chunk) {
        this.end(chunk, (Handler<AsyncResult<Void>>)null);
    }
    
    @Override
    public void end(final String chunk, final Handler<AsyncResult<Void>> handler) {
        this.end(Buffer.buffer(chunk), handler);
    }
    
    @Override
    public void end(final String chunk, final String enc) {
        this.end(chunk, enc, null);
    }
    
    @Override
    public void end(final String chunk, final String enc, final Handler<AsyncResult<Void>> handler) {
        Objects.requireNonNull(enc, "no null encoding accepted");
        this.end(Buffer.buffer(chunk, enc), handler);
    }
    
    @Override
    public void end(final Buffer chunk) {
        this.write(chunk.getByteBuf(), true, null);
    }
    
    @Override
    public void end(final Buffer chunk, final Handler<AsyncResult<Void>> handler) {
        this.write(chunk.getByteBuf(), true, handler);
    }
    
    @Override
    public void end() {
        this.write(null, true, null);
    }
    
    @Override
    public void end(final Handler<AsyncResult<Void>> handler) {
        this.write(null, true, handler);
    }
    
    @Override
    public HttpClientRequest write(final Buffer chunk) {
        return this.write(chunk, null);
    }
    
    @Override
    public HttpClientRequest write(final Buffer chunk, final Handler<AsyncResult<Void>> handler) {
        final ByteBuf buf = chunk.getByteBuf();
        this.write(buf, false, handler);
        return this;
    }
    
    @Override
    public HttpClientRequest write(final String chunk) {
        return this.write(chunk, (Handler<AsyncResult<Void>>)null);
    }
    
    @Override
    public HttpClientRequest write(final String chunk, final Handler<AsyncResult<Void>> handler) {
        this.write(Buffer.buffer(chunk).getByteBuf(), false, handler);
        return this;
    }
    
    @Override
    public HttpClientRequest write(final String chunk, final String enc) {
        return this.write(chunk, enc, null);
    }
    
    @Override
    public HttpClientRequest write(final String chunk, final String enc, final Handler<AsyncResult<Void>> handler) {
        Objects.requireNonNull(enc, "no null encoding accepted");
        this.write(Buffer.buffer(chunk, enc).getByteBuf(), false, handler);
        return this;
    }
    
    private boolean requiresContentLength() {
        return !this.chunked && (this.headers == null || !this.headers.contains(HttpHeaders.CONTENT_LENGTH));
    }
    
    private void write(final ByteBuf buff, final boolean end, final Handler<AsyncResult<Void>> completionHandler) {
        if (buff == null && !end) {
            return;
        }
        final HttpClientStream s;
        synchronized (this) {
            this.checkEnded();
            this.checkResponseHandler();
            if (end) {
                if (buff != null && this.requiresContentLength()) {
                    this.headers().set(HttpHeaders.CONTENT_LENGTH, String.valueOf(buff.readableBytes()));
                }
            }
            else if (this.requiresContentLength()) {
                throw new IllegalStateException("You must set the Content-Length header to be the total size of the message body BEFORE sending any data if you are not using HTTP chunked encoding.");
            }
            this.ended |= end;
            if (this.stream == null) {
                if (buff != null) {
                    if (this.pendingChunks == null) {
                        this.pendingChunks = buff;
                    }
                    else {
                        CompositeByteBuf pending;
                        if (this.pendingChunks instanceof CompositeByteBuf) {
                            pending = (CompositeByteBuf)this.pendingChunks;
                        }
                        else {
                            pending = Unpooled.compositeBuffer();
                            pending.addComponent(true, this.pendingChunks);
                            this.pendingChunks = (ByteBuf)pending;
                        }
                        pending.addComponent(true, buff);
                    }
                    if (completionHandler != null) {
                        if (this.pendingHandlers == null) {
                            this.pendingHandlers = new ArrayList<Handler<AsyncResult<Void>>>();
                        }
                        this.pendingHandlers.add(completionHandler);
                    }
                }
                this.connect(null);
                return;
            }
            s = this.stream;
        }
        s.writeBuffer(buff, end, completionHandler);
        if (end) {
            s.endRequest();
            this.tryComplete();
        }
    }
    
    @Override
    protected void checkEnded() {
        if (this.ended) {
            throw new IllegalStateException("Request already complete");
        }
    }
    
    private void checkResponseHandler() {
        if (this.respHandler == null) {
            throw new IllegalStateException("You must set an handler for the HttpClientResponse before connecting");
        }
    }
    
    synchronized Handler<HttpClientRequest> pushHandler() {
        return this.pushHandler;
    }
    
    @Override
    public synchronized HttpClientRequest setStreamPriority(final StreamPriority priority) {
        if (this.stream != null) {
            this.stream.updatePriority(priority);
        }
        else {
            this.priority = priority;
        }
        return this;
    }
    
    @Override
    public synchronized StreamPriority getStreamPriority() {
        final HttpClientStream s = this.stream;
        return (s != null) ? s.priority() : this.priority;
    }
    
    static {
        log = LoggerFactory.getLogger(HttpClientRequestImpl.class);
    }
}
