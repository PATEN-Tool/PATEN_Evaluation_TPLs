// 
// Decompiled by Procyon v0.5.36
// 

package ratpack.server.internal;

import org.slf4j.LoggerFactory;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import java.nio.CharBuffer;
import ratpack.handling.internal.DescribingHandlers;
import ratpack.handling.internal.DescribingHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.channel.ChannelFutureListener;
import ratpack.http.internal.HttpHeaderConstants;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import io.netty.handler.codec.http.HttpVersion;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLPeerUnverifiedException;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import io.netty.handler.timeout.IdleStateEvent;
import ratpack.http.Response;
import ratpack.http.MutableHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.channel.Channel;
import ratpack.http.Headers;
import ratpack.exec.Execution;
import ratpack.http.internal.DefaultResponse;
import ratpack.file.internal.ResponseTransmitter;
import ratpack.http.Request;
import java.util.concurrent.atomic.AtomicBoolean;
import ratpack.http.internal.NettyHeadersBackedMutableHeaders;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import ratpack.http.internal.RequestIdleTimeout;
import ratpack.http.internal.DefaultRequest;
import ratpack.http.internal.ConnectionIdleTimeout;
import java.net.InetSocketAddress;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpUtil;
import ratpack.http.internal.NettyHeadersBackedHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.ReferenceCountUtil;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.channel.ChannelHandlerContext;
import ratpack.server.ServerConfig;
import ratpack.render.internal.RenderController;
import ratpack.handling.Handlers;
import ratpack.exec.ExecController;
import ratpack.render.internal.DefaultRenderController;
import ratpack.handling.internal.ChainHandler;
import java.time.Clock;
import ratpack.registry.Registry;
import ratpack.handling.internal.DefaultContext;
import ratpack.handling.Handler;
import org.slf4j.Logger;
import javax.security.cert.X509Certificate;
import ratpack.func.Action;
import io.netty.util.AttributeKey;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;

@ChannelHandler.Sharable
public class NettyHandlerAdapter extends ChannelInboundHandlerAdapter
{
    private static final AttributeKey<Action<Object>> CHANNEL_SUBSCRIBER_ATTRIBUTE_KEY;
    private static final AttributeKey<RequestBodyAccumulator> BODY_ACCUMULATOR_KEY;
    private static final AttributeKey<X509Certificate> CLIENT_CERT_KEY;
    private static final Logger LOGGER;
    private final Handler[] handlers;
    private final DefaultContext.ApplicationConstants applicationConstants;
    private final Registry serverRegistry;
    private final boolean development;
    private final Clock clock;
    
    public NettyHandlerAdapter(final Registry serverRegistry, final Handler handler) throws Exception {
        this.handlers = ChainHandler.unpack(handler);
        this.serverRegistry = serverRegistry;
        this.applicationConstants = new DefaultContext.ApplicationConstants(this.serverRegistry, new DefaultRenderController(), (ExecController)serverRegistry.get((Class)ExecController.class), Handlers.notFound());
        this.development = ((ServerConfig)serverRegistry.get((Class)ServerConfig.class)).isDevelopment();
        this.clock = (Clock)serverRegistry.get((Class)Clock.class);
    }
    
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        ctx.read();
        super.channelActive(ctx);
    }
    
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            this.newRequest(ctx, (HttpRequest)msg);
        }
        else if (msg instanceof HttpContent) {
            ((HttpContent)msg).touch();
            final RequestBodyAccumulator bodyAccumulator = (RequestBodyAccumulator)ctx.channel().attr((AttributeKey)NettyHandlerAdapter.BODY_ACCUMULATOR_KEY).get();
            if (bodyAccumulator == null) {
                ((HttpContent)msg).release();
            }
            else {
                bodyAccumulator.add((HttpContent)msg);
            }
            if (msg instanceof LastHttpContent) {
                ctx.channel().read();
            }
        }
        else {
            final Action<Object> subscriber = (Action<Object>)ctx.channel().attr((AttributeKey)NettyHandlerAdapter.CHANNEL_SUBSCRIBER_ATTRIBUTE_KEY).get();
            if (subscriber == null) {
                super.channelRead(ctx, ReferenceCountUtil.touch(msg));
            }
            else {
                subscriber.execute(ReferenceCountUtil.touch(msg));
            }
        }
    }
    
    private void newRequest(final ChannelHandlerContext ctx, final HttpRequest nettyRequest) throws Exception {
        if (!nettyRequest.decoderResult().isSuccess()) {
            NettyHandlerAdapter.LOGGER.debug("Failed to decode HTTP request.", nettyRequest.decoderResult().cause());
            sendError(ctx, HttpResponseStatus.BAD_REQUEST);
            return;
        }
        final Headers requestHeaders = new NettyHeadersBackedHeaders(nettyRequest.headers());
        final Long contentLength = HttpUtil.getContentLength((HttpMessage)nettyRequest, -1L);
        final String transferEncoding = requestHeaders.get((CharSequence)HttpHeaderNames.TRANSFER_ENCODING);
        final boolean hasBody = contentLength > 0L || transferEncoding != null;
        final RequestBody requestBody = hasBody ? new RequestBody(contentLength, nettyRequest, ctx) : null;
        final Channel channel = ctx.channel();
        if (requestBody != null) {
            channel.attr((AttributeKey)NettyHandlerAdapter.BODY_ACCUMULATOR_KEY).set((Object)requestBody);
        }
        final InetSocketAddress remoteAddress = (InetSocketAddress)channel.remoteAddress();
        final InetSocketAddress socketAddress = (InetSocketAddress)channel.localAddress();
        final ConnectionIdleTimeout connectionIdleTimeout = ConnectionIdleTimeout.of(channel);
        final DefaultRequest request = new DefaultRequest(this.clock.instant(), requestHeaders, nettyRequest.method(), nettyRequest.protocolVersion(), nettyRequest.uri(), remoteAddress, socketAddress, (ServerConfig)this.serverRegistry.get((Class)ServerConfig.class), requestBody, connectionIdleTimeout, (X509Certificate)channel.attr((AttributeKey)NettyHandlerAdapter.CLIENT_CERT_KEY).get());
        final HttpHeaders nettyHeaders = (HttpHeaders)new DefaultHttpHeaders();
        final MutableHeaders responseHeaders = new NettyHeadersBackedMutableHeaders(nettyHeaders);
        final AtomicBoolean transmitted = new AtomicBoolean(false);
        final DefaultResponseTransmitter responseTransmitter = new DefaultResponseTransmitter(transmitted, channel, this.clock, nettyRequest, request, nettyHeaders, requestBody);
        ctx.channel().attr((AttributeKey)DefaultResponseTransmitter.ATTRIBUTE_KEY).set((Object)responseTransmitter);
        final Action<Action<Object>> subscribeHandler = (Action<Action<Object>>)(thing -> {
            transmitted.set(true);
            ctx.channel().attr((AttributeKey)NettyHandlerAdapter.CHANNEL_SUBSCRIBER_ATTRIBUTE_KEY).set((Object)thing);
        });
        final DefaultContext.RequestConstants requestConstants = new DefaultContext.RequestConstants(this.applicationConstants, request, channel, responseTransmitter, subscribeHandler);
        final Response response = new DefaultResponse(responseHeaders, ctx.alloc(), responseTransmitter);
        requestConstants.response = response;
        DefaultContext.start(channel.eventLoop(), requestConstants, this.serverRegistry, this.handlers, (Action<? super Execution>)(execution -> {
            if (!transmitted.get()) {
                final Handler lastHandler = requestConstants.handler;
                final StringBuilder description = new StringBuilder();
                description.append("No response sent for ").append(request.getMethod().getName()).append(" request to ").append(request.getUri());
                if (lastHandler != null) {
                    description.append(" (last handler: ");
                    if (lastHandler instanceof DescribingHandler) {
                        ((DescribingHandler)lastHandler).describeTo(description);
                    }
                    else {
                        DescribingHandlers.describeTo(lastHandler, description);
                    }
                    description.append(")");
                }
                final String message = description.toString();
                NettyHandlerAdapter.LOGGER.warn(message);
                response.getHeaders().clear();
                ByteBuf body;
                if (this.development) {
                    final CharBuffer charBuffer = CharBuffer.wrap(message);
                    body = ByteBufUtil.encodeString(ctx.alloc(), charBuffer, CharsetUtil.UTF_8);
                    response.contentType(HttpHeaderConstants.PLAIN_TEXT_UTF8);
                }
                else {
                    body = Unpooled.EMPTY_BUFFER;
                }
                response.getHeaders().set(HttpHeaderConstants.CONTENT_LENGTH, body.readableBytes());
                responseTransmitter.transmit(HttpResponseStatus.INTERNAL_SERVER_ERROR, body);
            }
        }));
    }
    
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
        if (!isIgnorableException(cause)) {
            NettyHandlerAdapter.LOGGER.error("", cause);
            if (ctx.channel().isActive()) {
                sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
            }
        }
    }
    
    public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            ConnectionClosureReason.setIdle(ctx.channel());
            ctx.close();
        }
        Label_0113: {
            if (evt instanceof SslHandshakeCompletionEvent && ((SslHandshakeCompletionEvent)evt).isSuccess()) {
                final SSLEngine engine = ((SslHandler)ctx.pipeline().get((Class)SslHandler.class)).engine();
                if (!engine.getWantClientAuth()) {
                    if (!engine.getNeedClientAuth()) {
                        break Label_0113;
                    }
                }
                try {
                    final X509Certificate clientCert = engine.getSession().getPeerCertificateChain()[0];
                    ctx.channel().attr((AttributeKey)NettyHandlerAdapter.CLIENT_CERT_KEY).set((Object)clientCert);
                }
                catch (SSLPeerUnverifiedException ex) {}
            }
        }
        super.userEventTriggered(ctx, evt);
    }
    
    public void channelWritabilityChanged(final ChannelHandlerContext ctx) {
        final DefaultResponseTransmitter responseTransmitter = (DefaultResponseTransmitter)ctx.channel().attr((AttributeKey)DefaultResponseTransmitter.ATTRIBUTE_KEY).get();
        if (responseTransmitter != null) {
            responseTransmitter.writabilityChanged();
        }
    }
    
    private static boolean isIgnorableException(final Throwable throwable) {
        if (throwable instanceof ClosedChannelException) {
            return true;
        }
        if (throwable instanceof IOException) {
            final String message = throwable.getMessage();
            return message != null && message.endsWith("Connection reset by peer");
        }
        return false;
    }
    
    private static void sendError(final ChannelHandlerContext ctx, final HttpResponseStatus status) {
        final FullHttpResponse response = (FullHttpResponse)new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer((CharSequence)("Failure: " + status.toString() + "\r\n"), CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderConstants.CONTENT_TYPE, (Object)HttpHeaderConstants.PLAIN_TEXT_UTF8);
        ctx.writeAndFlush((Object)response).addListener((GenericFutureListener)ChannelFutureListener.CLOSE);
    }
    
    static {
        CHANNEL_SUBSCRIBER_ATTRIBUTE_KEY = AttributeKey.valueOf((Class)NettyHandlerAdapter.class, "subscriber");
        BODY_ACCUMULATOR_KEY = AttributeKey.valueOf((Class)NettyHandlerAdapter.class, "requestBody");
        CLIENT_CERT_KEY = AttributeKey.valueOf((Class)NettyHandlerAdapter.class, "principal");
        LOGGER = LoggerFactory.getLogger((Class)NettyHandlerAdapter.class);
    }
}
