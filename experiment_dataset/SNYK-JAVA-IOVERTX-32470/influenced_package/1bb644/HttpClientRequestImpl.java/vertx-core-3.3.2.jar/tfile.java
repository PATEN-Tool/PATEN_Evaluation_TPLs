// 
// Decompiled by Procyon v0.5.36
// 

package io.vertx.core.http.impl;

import io.vertx.core.streams.WriteStream;
import io.netty.buffer.Unpooled;
import io.netty.buffer.CompositeByteBuf;
import io.vertx.core.impl.ContextImpl;
import io.vertx.core.streams.StreamBase;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.http.HttpFrame;
import java.util.List;
import io.vertx.core.net.NetSocket;
import io.vertx.core.http.HttpHeaders;
import java.util.Objects;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.CaseInsensitiveHeaders;
import io.netty.buffer.ByteBuf;
import io.vertx.core.http.HttpConnection;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.Handler;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.http.HttpClientRequest;

public class HttpClientRequestImpl extends HttpClientRequestBase implements HttpClientRequest
{
    private final boolean ssl;
    private final VertxInternal vertx;
    private final int port;
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
    private boolean headWritten;
    private boolean completed;
    private ByteBuf pendingChunks;
    private int pendingMaxSize;
    private boolean connecting;
    private boolean writeHead;
    private long written;
    private CaseInsensitiveHeaders headers;
    
    HttpClientRequestImpl(final HttpClientImpl client, final HttpMethod method, final String host, final int port, final boolean ssl, final String relativeURI, final VertxInternal vertx) {
        super(client, method, host, relativeURI);
        this.pendingMaxSize = -1;
        this.chunked = false;
        this.vertx = vertx;
        this.ssl = ssl;
        this.port = port;
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
    public HttpClientRequestImpl write(final Buffer chunk) {
        synchronized (this.getLock()) {
            this.checkComplete();
            this.checkResponseHandler();
            final ByteBuf buf = chunk.getByteBuf();
            this.write(buf, false);
            return this;
        }
    }
    
    @Override
    public HttpClientRequestImpl write(final String chunk) {
        synchronized (this.getLock()) {
            this.checkComplete();
            this.checkResponseHandler();
            return this.write(Buffer.buffer(chunk));
        }
    }
    
    @Override
    public HttpClientRequestImpl write(final String chunk, final String enc) {
        synchronized (this.getLock()) {
            Objects.requireNonNull(enc, "no null encoding accepted");
            this.checkComplete();
            this.checkResponseHandler();
            return this.write(Buffer.buffer(chunk, enc));
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
            this.checkComplete();
            this.drainHandler = handler;
            if (this.stream != null) {
                this.stream.getContext().runOnContext(v -> this.stream.checkDrained());
            }
            return this;
        }
    }
    
    @Override
    public HttpClientRequest continueHandler(final Handler<Void> handler) {
        synchronized (this.getLock()) {
            this.checkComplete();
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
                if (!this.headWritten) {
                    this.writeHead();
                    if (completionHandler != null) {
                        completionHandler.handle(this.stream.version());
                    }
                }
            }
            else {
                this.connect(completionHandler);
                this.writeHead = true;
            }
            return this;
        }
    }
    
    @Override
    public void end(final String chunk) {
        synchronized (this.getLock()) {
            this.end(Buffer.buffer(chunk));
        }
    }
    
    @Override
    public void end(final String chunk, final String enc) {
        synchronized (this.getLock()) {
            Objects.requireNonNull(enc, "no null encoding accepted");
            this.end(Buffer.buffer(chunk, enc));
        }
    }
    
    @Override
    public void end(final Buffer chunk) {
        synchronized (this.getLock()) {
            this.checkComplete();
            this.checkResponseHandler();
            if (!this.chunked && !this.contentLengthSet()) {
                this.headers().set(HttpHeaders.CONTENT_LENGTH, String.valueOf(chunk.length()));
            }
            this.write(chunk.getByteBuf(), true);
        }
    }
    
    @Override
    public void end() {
        synchronized (this.getLock()) {
            this.checkComplete();
            this.checkResponseHandler();
            this.write(null, true);
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
    public void reset(final long code) {
        synchronized (this.getLock()) {
            if (this.stream == null) {
                throw new IllegalStateException("Cannot reset the request that is not yet connected");
            }
            this.completed = true;
            this.stream.reset(code);
        }
    }
    
    @Override
    public HttpConnection connection() {
        synchronized (this.getLock()) {
            if (this.stream == null) {
                throw new IllegalStateException("Not yet connected");
            }
            return this.stream.connection();
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
            if (!this.completed && this.drainHandler != null) {
                try {
                    this.drainHandler.handle(null);
                }
                catch (Throwable t) {
                    this.handleException(t);
                }
            }
        }
    }
    
    @Override
    protected void doHandleResponse(final HttpClientResponseImpl resp) {
        if (resp.statusCode() == 100) {
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
    
    private synchronized void connect(final Handler<HttpVersion> headersCompletionHandler) {
        if (!this.connecting) {
            if (this.method == HttpMethod.OTHER && this.rawMethod == null) {
                throw new IllegalStateException("You must provide a rawMethod when using an HttpMethod.OTHER method");
            }
            final Waiter waiter = new Waiter(this, this.vertx.getContext()) {
                @Override
                void handleFailure(final Throwable failure) {
                    HttpClientRequestImpl.this.handleException(failure);
                }
                
                @Override
                void handleConnection(final HttpClientConnection conn) {
                    synchronized (HttpClientRequestImpl.this) {
                        if (HttpClientRequestImpl.this.connectionHandler != null) {
                            HttpClientRequestImpl.this.connectionHandler.handle(conn);
                        }
                    }
                }
                
                @Override
                void handleStream(final HttpClientStream stream) {
                    HttpClientRequestImpl.this.connected(stream, headersCompletionHandler);
                }
                
                @Override
                boolean isCancelled() {
                    return HttpClientRequestImpl.this.exceptionOccurred;
                }
            };
            this.client.getConnectionForRequest(this.port, this.host, waiter);
            this.connecting = true;
        }
    }
    
    private void connected(final HttpClientStream stream, final Handler<HttpVersion> headersCompletionHandler) {
        final HttpClientConnection conn = stream.connection();
        synchronized (this) {
            (this.stream = stream).beginRequest(this);
            if (this.pendingMaxSize != -1) {
                this.stream.doSetWriteQueueMaxSize(this.pendingMaxSize);
            }
            if (this.pendingChunks != null) {
                final ByteBuf pending = this.pendingChunks;
                this.pendingChunks = null;
                if (this.completed) {
                    this.writeHeadWithContent(pending, true);
                    conn.reportBytesWritten(this.written);
                    if (this.respHandler != null) {
                        this.stream.endRequest();
                    }
                }
                else {
                    this.writeHeadWithContent(pending, false);
                    if (headersCompletionHandler != null) {
                        headersCompletionHandler.handle(stream.version());
                    }
                }
            }
            else if (this.completed) {
                this.writeHeadWithContent(null, true);
                conn.reportBytesWritten(this.written);
                if (this.respHandler != null) {
                    this.stream.endRequest();
                }
            }
            else if (this.writeHead) {
                this.writeHead();
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
    
    private String hostHeader() {
        if (this.hostHeader != null) {
            return this.hostHeader;
        }
        if ((this.port == 80 && !this.ssl) || (this.port == 443 && this.ssl)) {
            return this.host;
        }
        return this.host + ':' + this.port;
    }
    
    private void writeHead() {
        this.stream.writeHead(this.method, this.rawMethod, this.uri, this.headers, this.hostHeader(), this.chunked);
        this.headWritten = true;
    }
    
    private void writeHeadWithContent(final ByteBuf buf, final boolean end) {
        this.stream.writeHeadWithContent(this.method, this.rawMethod, this.uri, this.headers, this.hostHeader(), this.chunked, buf, end);
        this.headWritten = true;
    }
    
    private void write(final ByteBuf buff, final boolean end) {
        if (buff == null && !end) {
            return;
        }
        if (end) {
            this.completed = true;
        }
        if (!end && !this.chunked && !this.contentLengthSet()) {
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
                        pending.addComponent(this.pendingChunks).writerIndex(this.pendingChunks.writerIndex());
                        this.pendingChunks = (ByteBuf)pending;
                    }
                    pending.addComponent(buff).writerIndex(pending.writerIndex() + buff.writerIndex());
                }
            }
            this.connect(null);
        }
        else {
            if (!this.headWritten) {
                this.writeHeadWithContent(buff, end);
            }
            else {
                this.stream.writeBuffer(buff, end);
            }
            if (end) {
                this.stream.connection().reportBytesWritten(this.written);
                if (this.respHandler != null) {
                    this.stream.endRequest();
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
}
