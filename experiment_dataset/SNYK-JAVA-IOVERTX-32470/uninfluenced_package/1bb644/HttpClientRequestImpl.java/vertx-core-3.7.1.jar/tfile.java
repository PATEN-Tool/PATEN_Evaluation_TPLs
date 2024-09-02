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
import java.util.Objects;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.StreamPriority;
import io.vertx.core.http.impl.headers.VertxHttpHeaders;
import io.vertx.core.AsyncResult;
import java.util.List;
import io.netty.buffer.ByteBuf;
import io.vertx.core.http.HttpConnection;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.Handler;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.logging.Logger;
import io.vertx.core.http.HttpClientRequest;

public class HttpClientRequestImpl extends HttpClientRequestBase implements HttpClientRequest
{
    static final Logger log;
    private final VertxInternal vertx;
    private Handler<HttpClientResponse> respHandler;
    private Handler<Void> endHandler;
    private boolean chunked;
    private String hostHeader;
    private String rawMethod;
    private Handler<Void> continueHandler;
    private Handler<Void> drainHandler;
    private Handler<HttpClientRequest> pushHandler;
    private Handler<HttpConnection> connectionHandler;
    private boolean completed;
    private Handler<Void> completionHandler;
    private Long reset;
    private ByteBuf pendingChunks;
    private List<Handler<AsyncResult<Void>>> pendingHandlers;
    private int pendingMaxSize;
    private int followRedirects;
    private long written;
    private VertxHttpHeaders headers;
    private StreamPriority priority;
    private HttpClientStream stream;
    private boolean connecting;
    
    HttpClientRequestImpl(final HttpClientImpl client, final boolean ssl, final HttpMethod method, final SocketAddress server, final String host, final int port, final String relativeURI, final VertxInternal vertx) {
        super(client, ssl, method, server, host, port, relativeURI);
        this.pendingMaxSize = -1;
        this.chunked = false;
        this.vertx = vertx;
        this.priority = HttpUtils.DEFAULT_STREAM_PRIORITY;
    }
    
    @Override
    public int streamId() {
        final HttpClientStream s;
        synchronized (this) {
            if ((s = this.stream) == null) {
                return -1;
            }
        }
        return s.id();
    }
    
    @Override
    public synchronized HttpClientRequest handler(final Handler<HttpClientResponse> handler) {
        if (handler != null) {
            this.checkComplete();
        }
        this.respHandler = handler;
        return this;
    }
    
    @Override
    public HttpClientRequest setFollowRedirects(final boolean followRedirects) {
        synchronized (this) {
            this.checkComplete();
            if (followRedirects) {
                this.followRedirects = this.client.getOptions().getMaxRedirects() - 1;
            }
            else {
                this.followRedirects = 0;
            }
            return this;
        }
    }
    
    @Override
    public HttpClientRequest endHandler(final Handler<Void> handler) {
        synchronized (this) {
            if (handler != null) {
                this.checkComplete();
            }
            this.endHandler = handler;
            return this;
        }
    }
    
    @Override
    public HttpClientRequestImpl setChunked(final boolean chunked) {
        synchronized (this) {
            this.checkComplete();
            if (this.written > 0L) {
                throw new IllegalStateException("Cannot set chunked after data has been written on request");
            }
            if (this.client.getOptions().getProtocolVersion() != HttpVersion.HTTP_1_0) {
                this.chunked = chunked;
            }
            return this;
        }
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
        this.checkComplete();
        this.headers().set(name, value);
        return this;
    }
    
    @Override
    public synchronized HttpClientRequest putHeader(final String name, final Iterable<String> values) {
        this.checkComplete();
        this.headers().set(name, values);
        return this;
    }
    
    @Override
    public HttpClientRequest setWriteQueueMaxSize(final int maxSize) {
        final HttpClientStream s;
        synchronized (this) {
            this.checkComplete();
            if ((s = this.stream) == null) {
                this.pendingMaxSize = maxSize;
                return this;
            }
        }
        s.doSetWriteQueueMaxSize(maxSize);
        return this;
    }
    
    @Override
    public boolean writeQueueFull() {
        final HttpClientStream s;
        synchronized (this) {
            this.checkComplete();
            if ((s = this.stream) == null) {
                return false;
            }
        }
        return s.isNotWritable();
    }
    
    @Override
    public HttpClientRequest drainHandler(final Handler<Void> handler) {
        synchronized (this) {
            if (handler != null) {
                this.checkComplete();
                this.drainHandler = handler;
                final HttpClientStream s;
                if ((s = this.stream) == null) {
                    return this;
                }
                s.getContext().runOnContext(v -> {
                    synchronized (this) {
                        if (!this.stream.isNotWritable()) {
                            this.handleDrained();
                        }
                    }
                    return;
                });
            }
            else {
                this.drainHandler = null;
            }
            return this;
        }
    }
    
    @Override
    public synchronized HttpClientRequest continueHandler(final Handler<Void> handler) {
        if (handler != null) {
            this.checkComplete();
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
        this.checkComplete();
        this.checkResponseHandler();
        if (this.stream != null) {
            throw new IllegalStateException("Head already written");
        }
        this.connect(headersHandler);
        return this;
    }
    
    @Override
    public synchronized HttpClientRequest putHeader(final CharSequence name, final CharSequence value) {
        this.checkComplete();
        this.headers().set(name, value);
        return this;
    }
    
    @Override
    public synchronized HttpClientRequest putHeader(final CharSequence name, final Iterable<CharSequence> values) {
        this.checkComplete();
        this.headers().set(name, values);
        return this;
    }
    
    @Override
    public synchronized HttpClientRequest pushHandler(final Handler<HttpClientRequest> handler) {
        this.pushHandler = handler;
        return this;
    }
    
    @Override
    public boolean reset(final long code) {
        final HttpClientStream s;
        synchronized (this) {
            if (this.reset != null) {
                return false;
            }
            this.reset = code;
            if (this.tryComplete() && this.completionHandler != null) {
                this.completionHandler.handle(null);
            }
            s = this.stream;
        }
        if (s != null) {
            s.reset(code);
        }
        return true;
    }
    
    private boolean tryComplete() {
        if (!this.completed) {
            this.completed = true;
            this.drainHandler = null;
            return true;
        }
        return false;
    }
    
    @Override
    public HttpConnection connection() {
        final HttpClientStream s;
        synchronized (this) {
            if ((s = this.stream) == null) {
                return null;
            }
        }
        return s.connection();
    }
    
    @Override
    public synchronized HttpClientRequest connectionHandler(final Handler<HttpConnection> handler) {
        this.connectionHandler = handler;
        return this;
    }
    
    @Override
    public synchronized HttpClientRequest writeCustomFrame(final int type, final int flags, final Buffer payload) {
        final HttpClientStream s;
        synchronized (this) {
            this.checkComplete();
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
            if ((handler = this.drainHandler) == null) {
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
    
    private void handleNextRequest(final HttpClientRequestImpl next, final long timeoutMs) {
        next.handler(this.respHandler);
        next.exceptionHandler(this.exceptionHandler());
        this.exceptionHandler((Handler<Throwable>)null);
        next.endHandler(this.endHandler);
        next.pushHandler = this.pushHandler;
        next.followRedirects = this.followRedirects - 1;
        next.written = this.written;
        if (next.hostHeader == null) {
            next.hostHeader = this.hostHeader;
        }
        if (this.headers != null && next.headers == null) {
            next.headers().addAll(this.headers);
        }
        final Future<Void> fut = Future.future();
        fut.setHandler(ar -> {
            if (ar.succeeded()) {
                if (timeoutMs > 0L) {
                    next.setTimeout(timeoutMs);
                }
                next.end();
            }
            else {
                next.handleException(ar.cause());
            }
            return;
        });
        if (this.exceptionOccurred != null) {
            fut.fail(this.exceptionOccurred);
        }
        else if (this.completed) {
            fut.complete();
        }
        else {
            final Future future;
            this.exceptionHandler(err -> {
                if (!future.isComplete()) {
                    future.fail(err);
                }
                return;
            });
            final Future future2;
            this.completionHandler = (v -> {
                if (!future2.isComplete()) {
                    future2.complete();
                }
            });
        }
    }
    
    @Override
    protected void doHandleResponse(final HttpClientResponseImpl resp, final long timeoutMs) {
        if (this.reset == null) {
            final int statusCode = resp.statusCode();
            if (this.followRedirects > 0 && statusCode >= 300 && statusCode < 400) {
                final Future<HttpClientRequest> next = this.client.redirectHandler().apply(resp);
                if (next != null) {
                    next.setHandler(ar -> {
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
            if (statusCode == 100) {
                if (this.continueHandler != null) {
                    this.continueHandler.handle(null);
                }
            }
            else {
                if (this.respHandler != null) {
                    this.respHandler.handle(resp);
                }
                if (this.endHandler != null) {
                    this.endHandler.handle(null);
                }
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
                peerAddress = SocketAddress.inetSocketAddress(this.port, this.host);
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
                    if (this.exceptionOccurred != null || this.reset != null) {
                        stream.reset(0L);
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
            if (this.pendingChunks != null) {
                final List<Handler<AsyncResult<Void>>> handlers = this.pendingHandlers;
                final ByteBuf pending = this.pendingChunks;
                this.pendingChunks = null;
                this.pendingHandlers = null;
                Handler<AsyncResult<Void>> handler;
                if (handlers != null) {
                    handler = (ar -> handlers.forEach(h -> h.handle(ar)));
                }
                else {
                    handler = null;
                }
                if (this.completed) {
                    stream.writeHead(this.method, this.rawMethod, this.uri, this.headers, this.hostHeader(), this.chunked, pending, true, this.priority, handler);
                    stream.reportBytesWritten(this.written);
                    stream.endRequest();
                }
                else {
                    stream.writeHead(this.method, this.rawMethod, this.uri, this.headers, this.hostHeader(), this.chunked, pending, false, this.priority, handler);
                }
            }
            else if (this.completed) {
                stream.writeHead(this.method, this.rawMethod, this.uri, this.headers, this.hostHeader(), this.chunked, null, true, this.priority, null);
                stream.reportBytesWritten(this.written);
                stream.endRequest();
            }
            else {
                stream.writeHead(this.method, this.rawMethod, this.uri, this.headers, this.hostHeader(), this.chunked, null, false, this.priority, null);
            }
            this.connecting = false;
            this.stream = stream;
        }
        if (headersHandler != null) {
            headersHandler.handle(stream.version());
        }
    }
    
    private boolean contentLengthSet() {
        return this.headers != null && this.headers().contains(HttpHeaders.CONTENT_LENGTH);
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
    
    private void write(final ByteBuf buff, final boolean end, final Handler<AsyncResult<Void>> h) {
        final HttpClientStream s;
        synchronized (this) {
            this.checkComplete();
            this.checkResponseHandler();
            if (end) {
                if (buff != null && !this.chunked && !this.contentLengthSet()) {
                    this.headers().set(HttpHeaders.CONTENT_LENGTH, String.valueOf(buff.readableBytes()));
                }
            }
            else if (!this.chunked && !this.contentLengthSet()) {
                throw new IllegalStateException("You must set the Content-Length header to be the total size of the message body BEFORE sending any data if you are not using HTTP chunked encoding.");
            }
            if (buff == null && !end) {
                return;
            }
            if (buff != null) {
                this.written += buff.readableBytes();
            }
            if ((s = this.stream) == null) {
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
                    if (h != null) {
                        if (this.pendingHandlers == null) {
                            this.pendingHandlers = new ArrayList<Handler<AsyncResult<Void>>>();
                        }
                        this.pendingHandlers.add(h);
                    }
                }
                if (end) {
                    this.tryComplete();
                    if (this.completionHandler != null) {
                        this.completionHandler.handle(null);
                    }
                }
                this.connect(null);
                return;
            }
        }
        s.writeBuffer(buff, end, h);
        if (end) {
            s.reportBytesWritten(this.written);
        }
        if (end) {
            final Handler<Void> handler;
            synchronized (this) {
                this.tryComplete();
                s.endRequest();
                if ((handler = this.completionHandler) == null) {
                    return;
                }
            }
            handler.handle(null);
        }
    }
    
    @Override
    protected void checkComplete() {
        if (this.completed) {
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
        synchronized (this) {
            if (this.stream != null) {
                this.stream.updatePriority(priority);
            }
            else {
                this.priority = priority;
            }
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
