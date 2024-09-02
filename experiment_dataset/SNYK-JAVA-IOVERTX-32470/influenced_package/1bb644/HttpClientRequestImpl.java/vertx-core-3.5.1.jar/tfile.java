// 
// Decompiled by Procyon v0.5.36
// 

package io.vertx.core.http.impl;

import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.AsyncResult;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.streams.WriteStream;
import io.netty.buffer.Unpooled;
import io.netty.buffer.CompositeByteBuf;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.streams.StreamBase;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.http.HttpFrame;
import java.util.List;
import io.vertx.core.net.NetSocket;
import java.util.Objects;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.CaseInsensitiveHeaders;
import io.netty.buffer.ByteBuf;
import io.vertx.core.http.HttpVersion;
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
    private HttpClientStream stream;
    private volatile Object lock;
    private Handler<Void> drainHandler;
    private Handler<HttpClientRequest> pushHandler;
    private Handler<HttpConnection> connectionHandler;
    private Handler<HttpVersion> headersCompletionHandler;
    private boolean completed;
    private Handler<Void> completionHandler;
    private Long reset;
    private ByteBuf pendingChunks;
    private int pendingMaxSize;
    private int followRedirects;
    private boolean connecting;
    private long written;
    private CaseInsensitiveHeaders headers;
    
    HttpClientRequestImpl(final HttpClientImpl client, final boolean ssl, final HttpMethod method, final String host, final int port, final String relativeURI, final VertxInternal vertx) {
        super(client, ssl, method, host, port, relativeURI);
        this.pendingMaxSize = -1;
        this.chunked = false;
        this.vertx = vertx;
    }
    
    @Override
    public int streamId() {
        synchronized (this.getLock()) {
            return (this.stream != null) ? this.stream.id() : -1;
        }
    }
    
    @Override
    public HttpClientRequest handler(final Handler<HttpClientResponse> handler) {
        synchronized (this.getLock()) {
            if (handler != null) {
                this.checkComplete();
                this.respHandler = this.checkConnect(this.method, handler);
            }
            else {
                this.respHandler = null;
            }
            return this;
        }
    }
    
    @Override
    public HttpClientRequest pause() {
        return this;
    }
    
    @Override
    public HttpClientRequest resume() {
        return this;
    }
    
    @Override
    public HttpClientRequest setFollowRedirects(final boolean followRedirects) {
        synchronized (this.getLock()) {
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
    public HttpClientRequest endHandler(final Handler<Void> endHandler) {
        synchronized (this.getLock()) {
            if (endHandler != null) {
                this.checkComplete();
            }
            this.endHandler = endHandler;
            return this;
        }
    }
    
    @Override
    public HttpClientRequestImpl setChunked(final boolean chunked) {
        synchronized (this.getLock()) {
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
    public boolean isChunked() {
        synchronized (this.getLock()) {
            return this.chunked;
        }
    }
    
    @Override
    public String getRawMethod() {
        synchronized (this.getLock()) {
            return this.rawMethod;
        }
    }
    
    @Override
    public HttpClientRequest setRawMethod(final String method) {
        synchronized (this.getLock()) {
            this.rawMethod = method;
            return this;
        }
    }
    
    @Override
    public HttpClientRequest setHost(final String host) {
        synchronized (this.getLock()) {
            this.hostHeader = host;
            return this;
        }
    }
    
    @Override
    public String getHost() {
        synchronized (this.getLock()) {
            return this.hostHeader;
        }
    }
    
    @Override
    public MultiMap headers() {
        synchronized (this.getLock()) {
            if (this.headers == null) {
                this.headers = new CaseInsensitiveHeaders();
            }
            return this.headers;
        }
    }
    
    @Override
    public HttpClientRequest putHeader(final String name, final String value) {
        synchronized (this.getLock()) {
            this.checkComplete();
            this.headers().set(name, value);
            return this;
        }
    }
    
    @Override
    public HttpClientRequest putHeader(final String name, final Iterable<String> values) {
        synchronized (this.getLock()) {
            this.checkComplete();
            this.headers().set(name, values);
            return this;
        }
    }
    
    @Override
    public HttpClientRequest setWriteQueueMaxSize(final int maxSize) {
        synchronized (this.getLock()) {
            this.checkComplete();
            if (this.stream != null) {
                this.stream.doSetWriteQueueMaxSize(maxSize);
            }
            else {
                this.pendingMaxSize = maxSize;
            }
            return this;
        }
    }
    
    @Override
    public boolean writeQueueFull() {
        synchronized (this.getLock()) {
            this.checkComplete();
            return this.stream != null && this.stream.isNotWritable();
        }
    }
    
    @Override
    public HttpClientRequest drainHandler(final Handler<Void> handler) {
        synchronized (this.getLock()) {
            if (handler != null) {
                this.checkComplete();
                this.drainHandler = handler;
                if (this.stream != null) {
                    this.stream.getContext().runOnContext(v -> {
                        synchronized (this.getLock()) {
                            if (this.stream != null) {
                                this.stream.checkDrained();
                            }
                        }
                        return;
                    });
                }
            }
            else {
                this.drainHandler = null;
            }
            return this;
        }
    }
    
    @Override
    public HttpClientRequest continueHandler(final Handler<Void> handler) {
        synchronized (this.getLock()) {
            if (handler != null) {
                this.checkComplete();
            }
            this.continueHandler = handler;
            return this;
        }
    }
    
    @Override
    public HttpClientRequest sendHead() {
        return this.sendHead(null);
    }
    
    @Override
    public HttpClientRequest sendHead(final Handler<HttpVersion> completionHandler) {
        synchronized (this.getLock()) {
            this.checkComplete();
            this.checkResponseHandler();
            if (this.stream != null) {
                throw new IllegalStateException("Head already written");
            }
            this.headersCompletionHandler = completionHandler;
            this.connect();
            return this;
        }
    }
    
    @Override
    public HttpClientRequest putHeader(final CharSequence name, final CharSequence value) {
        synchronized (this.getLock()) {
            this.checkComplete();
            this.headers().set(name, value);
            return this;
        }
    }
    
    @Override
    public HttpClientRequest putHeader(final CharSequence name, final Iterable<CharSequence> values) {
        synchronized (this.getLock()) {
            this.checkComplete();
            this.headers().set(name, values);
            return this;
        }
    }
    
    @Override
    public HttpClientRequest pushHandler(final Handler<HttpClientRequest> handler) {
        synchronized (this.getLock()) {
            this.pushHandler = handler;
        }
        return this;
    }
    
    @Override
    public boolean reset(final long code) {
        synchronized (this.getLock()) {
            if (this.reset == null) {
                this.reset = code;
                if (this.tryComplete() && this.completionHandler != null) {
                    this.completionHandler.handle(null);
                }
                if (this.stream != null) {
                    this.stream.reset(code);
                }
                return true;
            }
            return false;
        }
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
        synchronized (this.getLock()) {
            return (this.stream != null) ? this.stream.connection() : null;
        }
    }
    
    @Override
    public HttpClientRequest connectionHandler(final Handler<HttpConnection> handler) {
        synchronized (this.getLock()) {
            this.connectionHandler = handler;
            return this;
        }
    }
    
    @Override
    public HttpClientRequest writeCustomFrame(final int type, final int flags, final Buffer payload) {
        synchronized (this.getLock()) {
            if (this.stream == null) {
                throw new IllegalStateException("Not yet connected");
            }
            this.stream.writeFrame(type, flags, payload.getByteBuf());
        }
        return this;
    }
    
    void handleDrained() {
        synchronized (this.getLock()) {
            if (this.drainHandler != null) {
                try {
                    this.drainHandler.handle(null);
                }
                catch (Throwable t) {
                    this.handleException(t);
                }
            }
        }
    }
    
    private void handleNextRequest(final HttpClientRequestImpl next, final long timeoutMs) {
        next.handler(this.respHandler);
        next.exceptionHandler((Handler)this.exceptionHandler());
        this.exceptionHandler((Handler)null);
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
    
    @Override
    protected Object getLock() {
        if (this.lock != null) {
            return this.lock;
        }
        synchronized (this) {
            if (this.lock != null) {
                return this.lock;
            }
            return this;
        }
    }
    
    private Handler<HttpClientResponse> checkConnect(final HttpMethod method, Handler<HttpClientResponse> handler) {
        if (method == HttpMethod.CONNECT) {
            handler = this.connectHandler(handler);
        }
        return handler;
    }
    
    private Handler<HttpClientResponse> connectHandler(final Handler<HttpClientResponse> responseHandler) {
        Objects.requireNonNull(responseHandler, "no null responseHandler accepted");
        NetSocket socket;
        HttpClientResponse response;
        return resp -> {
            if (resp.statusCode() == 200) {
                socket = resp.netSocket();
                socket.pause();
                response = new HttpClientResponse() {
                    private boolean resumed;
                    final /* synthetic */ HttpClientResponse val$resp;
                    final /* synthetic */ NetSocket val$socket;
                    
                    @Override
                    public HttpClientRequest request() {
                        return this.val$resp.request();
                    }
                    
                    @Override
                    public int statusCode() {
                        return this.val$resp.statusCode();
                    }
                    
                    @Override
                    public String statusMessage() {
                        return this.val$resp.statusMessage();
                    }
                    
                    @Override
                    public MultiMap headers() {
                        return this.val$resp.headers();
                    }
                    
                    @Override
                    public String getHeader(final String headerName) {
                        return this.val$resp.getHeader(headerName);
                    }
                    
                    @Override
                    public String getHeader(final CharSequence headerName) {
                        return this.val$resp.getHeader(headerName);
                    }
                    
                    @Override
                    public String getTrailer(final String trailerName) {
                        return this.val$resp.getTrailer(trailerName);
                    }
                    
                    @Override
                    public MultiMap trailers() {
                        return this.val$resp.trailers();
                    }
                    
                    @Override
                    public List<String> cookies() {
                        return this.val$resp.cookies();
                    }
                    
                    @Override
                    public HttpVersion version() {
                        return this.val$resp.version();
                    }
                    
                    @Override
                    public HttpClientResponse bodyHandler(final Handler<Buffer> bodyHandler) {
                        this.val$resp.bodyHandler(bodyHandler);
                        return this;
                    }
                    
                    @Override
                    public HttpClientResponse customFrameHandler(final Handler<HttpFrame> handler) {
                        this.val$resp.customFrameHandler(handler);
                        return this;
                    }
                    
                    @Override
                    public synchronized NetSocket netSocket() {
                        if (!this.resumed) {
                            this.resumed = true;
                            HttpClientRequestImpl.this.vertx.getContext().runOnContext(v -> this.val$socket.resume());
                        }
                        return this.val$socket;
                    }
                    
                    @Override
                    public HttpClientResponse endHandler(final Handler<Void> endHandler) {
                        this.val$resp.endHandler(endHandler);
                        return this;
                    }
                    
                    @Override
                    public HttpClientResponse handler(final Handler<Buffer> handler) {
                        this.val$resp.handler(handler);
                        return this;
                    }
                    
                    @Override
                    public HttpClientResponse pause() {
                        this.val$resp.pause();
                        return this;
                    }
                    
                    @Override
                    public HttpClientResponse resume() {
                        this.val$resp.resume();
                        return this;
                    }
                    
                    @Override
                    public HttpClientResponse exceptionHandler(final Handler<Throwable> handler) {
                        this.val$resp.exceptionHandler(handler);
                        return this;
                    }
                };
            }
            else {
                response = resp;
            }
            responseHandler.handle(response);
        };
    }
    
    private synchronized void connect() {
        if (!this.connecting) {
            if (this.method == HttpMethod.OTHER && this.rawMethod == null) {
                throw new IllegalStateException("You must provide a rawMethod when using an HttpMethod.OTHER method");
            }
            String peerHost;
            if (this.hostHeader != null) {
                final int idx = this.hostHeader.lastIndexOf(58);
                if (idx != -1) {
                    peerHost = this.hostHeader.substring(0, idx);
                }
                else {
                    peerHost = this.hostHeader;
                }
            }
            else {
                peerHost = this.host;
            }
            HttpClientStream stream;
            this.client.getConnectionForRequest(peerHost, this.ssl, this.port, this.host, this.connectionHandler, (ctx, conn) -> {
                if (this.exceptionOccurred != null || this.reset != null) {
                    return Boolean.valueOf(false);
                }
                else {
                    conn.createStream(this, ar -> {
                        if (ar.succeeded()) {
                            stream = ar.result();
                            ctx.executeFromIO(() -> this.connected(stream, this.headersCompletionHandler));
                            return;
                        }
                        else {
                            throw new RuntimeException(ar.cause());
                        }
                    });
                    return Boolean.valueOf(true);
                }
            }, (ctx, failure) -> ctx.executeFromIO(() -> this.handleException(failure)));
            this.connecting = true;
        }
    }
    
    synchronized void retry() {
        this.connecting = false;
        this.connect();
    }
    
    private void connected(final HttpClientStream stream, final Handler<HttpVersion> headersCompletionHandler) {
        final HttpClientConnection conn = stream.connection();
        synchronized (this) {
            (this.stream = stream).beginRequest();
            if (this.pendingMaxSize != -1) {
                this.stream.doSetWriteQueueMaxSize(this.pendingMaxSize);
            }
            if (this.pendingChunks != null) {
                final ByteBuf pending = this.pendingChunks;
                this.pendingChunks = null;
                if (this.completed) {
                    stream.writeHeadWithContent(this.method, this.rawMethod, this.uri, this.headers, this.hostHeader(), this.chunked, pending, true);
                    conn.reportBytesWritten(this.written);
                    if (this.respHandler != null) {
                        this.stream.endRequest();
                    }
                }
                else {
                    stream.writeHeadWithContent(this.method, this.rawMethod, this.uri, this.headers, this.hostHeader(), this.chunked, pending, false);
                    if (headersCompletionHandler != null) {
                        headersCompletionHandler.handle(stream.version());
                    }
                }
            }
            else if (this.completed) {
                stream.writeHeadWithContent(this.method, this.rawMethod, this.uri, this.headers, this.hostHeader(), this.chunked, null, true);
                conn.reportBytesWritten(this.written);
                if (this.respHandler != null) {
                    this.stream.endRequest();
                }
            }
            else {
                stream.writeHead(this.method, this.rawMethod, this.uri, this.headers, this.hostHeader(), this.chunked);
                if (headersCompletionHandler != null) {
                    headersCompletionHandler.handle(stream.version());
                }
            }
            this.lock = conn;
        }
    }
    
    private boolean contentLengthSet() {
        return this.headers != null && this.headers().contains(HttpHeaders.CONTENT_LENGTH);
    }
    
    @Override
    public void end(final String chunk) {
        this.end(Buffer.buffer(chunk));
    }
    
    @Override
    public void end(final String chunk, final String enc) {
        Objects.requireNonNull(enc, "no null encoding accepted");
        this.end(Buffer.buffer(chunk, enc));
    }
    
    @Override
    public void end(final Buffer chunk) {
        this.write(chunk.getByteBuf(), true);
    }
    
    @Override
    public void end() {
        this.write(null, true);
    }
    
    @Override
    public HttpClientRequestImpl write(final Buffer chunk) {
        final ByteBuf buf = chunk.getByteBuf();
        this.write(buf, false);
        return this;
    }
    
    @Override
    public HttpClientRequestImpl write(final String chunk) {
        return this.write(Buffer.buffer(chunk));
    }
    
    @Override
    public HttpClientRequestImpl write(final String chunk, final String enc) {
        Objects.requireNonNull(enc, "no null encoding accepted");
        return this.write(Buffer.buffer(chunk, enc));
    }
    
    private void write(final ByteBuf buff, final boolean end) {
        synchronized (this.getLock()) {
            this.checkComplete();
            this.checkResponseHandler();
            this._write(buff, end);
        }
    }
    
    private void _write(final ByteBuf buff, final boolean end) {
        if (buff == null && !end) {
            return;
        }
        if (end) {
            if (buff != null && !this.chunked && !this.contentLengthSet()) {
                this.headers().set(HttpHeaders.CONTENT_LENGTH, String.valueOf(buff.readableBytes()));
            }
        }
        else if (!this.chunked && !this.contentLengthSet()) {
            throw new IllegalStateException("You must set the Content-Length header to be the total size of the message body BEFORE sending any data if you are not using HTTP chunked encoding.");
        }
        if (buff != null) {
            this.written += buff.readableBytes();
        }
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
            }
            if (end) {
                this.tryComplete();
                if (this.completionHandler != null) {
                    this.completionHandler.handle(null);
                }
            }
            this.connect();
        }
        else {
            this.stream.writeBuffer(buff, end);
            if (end) {
                this.stream.connection().reportBytesWritten(this.written);
                if (this.respHandler != null) {
                    this.stream.endRequest();
                }
                this.tryComplete();
                if (this.completionHandler != null) {
                    this.completionHandler.handle(null);
                }
            }
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
    
    Handler<HttpClientRequest> pushHandler() {
        synchronized (this.getLock()) {
            return this.pushHandler;
        }
    }
    
    static {
        log = LoggerFactory.getLogger(ConnectionManager.class);
    }
}
