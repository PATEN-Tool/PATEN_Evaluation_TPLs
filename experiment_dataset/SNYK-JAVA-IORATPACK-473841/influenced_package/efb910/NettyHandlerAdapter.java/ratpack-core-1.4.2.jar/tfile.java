// 
// Decompiled by Procyon v0.5.36
// 

package ratpack.server.internal;

import org.slf4j.LoggerFactory;
import ratpack.exec.Execution;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpVersion;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import io.netty.buffer.ByteBuf;
import ratpack.http.Response;
import ratpack.http.MutableHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.channel.Channel;
import ratpack.http.Headers;
import io.netty.buffer.Unpooled;
import ratpack.http.internal.HttpHeaderConstants;
import io.netty.buffer.ByteBufUtil;
import io.netty.util.CharsetUtil;
import java.nio.CharBuffer;
import ratpack.handling.internal.DescribingHandlers;
import ratpack.handling.internal.DescribingHandler;
import ratpack.http.internal.DefaultResponse;
import ratpack.file.internal.ResponseTransmitter;
import ratpack.http.Request;
import java.util.concurrent.atomic.AtomicBoolean;
import ratpack.http.internal.NettyHeadersBackedMutableHeaders;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import ratpack.http.internal.DefaultRequest;
import java.time.Instant;
import java.net.InetSocketAddress;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpUtil;
import ratpack.http.internal.NettyHeadersBackedHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
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
import ratpack.registry.Registry;
import ratpack.handling.internal.DefaultContext;
import ratpack.handling.Handler;
import org.slf4j.Logger;
import ratpack.func.Action;
import io.netty.util.AttributeKey;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;

@ChannelHandler.Sharable
public class NettyHandlerAdapter extends ChannelInboundHandlerAdapter
{
    private static final AttributeKey<Action<Object>> CHANNEL_SUBSCRIBER_ATTRIBUTE_KEY;
    private static final AttributeKey<RequestBodyAccumulator> BODY_ACCUMULATOR_KEY;
    private static final Logger LOGGER;
    private final Handler[] handlers;
    private final DefaultContext.ApplicationConstants applicationConstants;
    private final Registry serverRegistry;
    private final boolean development;
    
    public NettyHandlerAdapter(final Registry serverRegistry, final Handler handler) throws Exception {
        this.handlers = ChainHandler.unpack(handler);
        this.serverRegistry = serverRegistry;
        this.applicationConstants = new DefaultContext.ApplicationConstants(this.serverRegistry, new DefaultRenderController(), serverRegistry.get(ExecController.class), Handlers.notFound());
        this.development = serverRegistry.get(ServerConfig.class).isDevelopment();
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
            final RequestBodyAccumulator bodyAccumulator = (RequestBodyAccumulator)ctx.channel().attr((AttributeKey)NettyHandlerAdapter.BODY_ACCUMULATOR_KEY).get();
            if (bodyAccumulator != null) {
                bodyAccumulator.add((HttpContent)msg);
            }
            if (msg instanceof LastHttpContent) {
                ctx.read();
            }
        }
        else {
            final Action<Object> subscriber = (Action<Object>)ctx.channel().attr((AttributeKey)NettyHandlerAdapter.CHANNEL_SUBSCRIBER_ATTRIBUTE_KEY).get();
            if (subscriber == null) {
                super.channelRead(ctx, msg);
            }
            else {
                subscriber.execute(msg);
            }
        }
    }
    
    private void newRequest(final ChannelHandlerContext ctx, final HttpRequest nettyRequest) throws Exception {
        if (!nettyRequest.decoderResult().isSuccess()) {
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
        final DefaultRequest request = new DefaultRequest(Instant.now(), requestHeaders, nettyRequest.method(), nettyRequest.protocolVersion(), nettyRequest.uri(), remoteAddress, socketAddress, this.serverRegistry.get(ServerConfig.class), requestBody);
        final HttpHeaders nettyHeaders = (HttpHeaders)new DefaultHttpHeaders(false);
        final MutableHeaders responseHeaders = new NettyHeadersBackedMutableHeaders(nettyHeaders);
        final AtomicBoolean transmitted = new AtomicBoolean(false);
        final DefaultResponseTransmitter responseTransmitter = new DefaultResponseTransmitter(transmitted, channel, nettyRequest, request, nettyHeaders, requestBody);
        ctx.channel().attr((AttributeKey)DefaultResponseTransmitter.ATTRIBUTE_KEY).set((Object)responseTransmitter);
        final Action<Action<Object>> subscribeHandler = thing -> {
            transmitted.set(true);
            ctx.channel().attr((AttributeKey)NettyHandlerAdapter.CHANNEL_SUBSCRIBER_ATTRIBUTE_KEY).set((Object)thing);
            return;
        };
        final DefaultContext.RequestConstants requestConstants = new DefaultContext.RequestConstants(this.applicationConstants, request, channel, responseTransmitter, subscribeHandler);
        final Response response = new DefaultResponse(responseHeaders, ctx.alloc(), responseTransmitter);
        requestConstants.response = response;
        final RequestBody requestBody2;
        final Channel channel2;
        final AtomicBoolean atomicBoolean;
        final DefaultContext.RequestConstants requestConstants2;
        Handler lastHandler;
        StringBuilder description;
        final DefaultRequest defaultRequest;
        String message;
        final Response response2;
        CharBuffer charBuffer;
        ByteBuf body;
        final DefaultResponseTransmitter defaultResponseTransmitter;
        DefaultContext.start(channel.eventLoop(), requestConstants, this.serverRegistry, this.handlers, execution -> {
            if (requestBody2 != null) {
                requestBody2.close();
                channel2.attr((AttributeKey)NettyHandlerAdapter.BODY_ACCUMULATOR_KEY).set((Object)null);
            }
            if (!atomicBoolean.get()) {
                lastHandler = requestConstants2.handler;
                description = new StringBuilder();
                description.append("No response sent for ").append(defaultRequest.getMethod().getName()).append(" request to ").append(defaultRequest.getUri());
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
                message = description.toString();
                NettyHandlerAdapter.LOGGER.warn(message);
                response2.getHeaders().clear();
                if (this.development) {
                    charBuffer = CharBuffer.wrap(message);
                    body = ByteBufUtil.encodeString(ctx.alloc(), charBuffer, CharsetUtil.UTF_8);
                    response2.contentType(HttpHeaderConstants.PLAIN_TEXT_UTF8);
                }
                else {
                    body = Unpooled.EMPTY_BUFFER;
                }
                response2.getHeaders().set(HttpHeaderConstants.CONTENT_LENGTH, body.readableBytes());
                defaultResponseTransmitter.transmit(HttpResponseStatus.INTERNAL_SERVER_ERROR, body);
            }
        });
    }
    
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
        if (!this.isIgnorableException(cause)) {
            NettyHandlerAdapter.LOGGER.error("", cause);
            if (ctx.channel().isActive()) {
                sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
            }
        }
    }
    
    public void channelWritabilityChanged(final ChannelHandlerContext ctx) throws Exception {
        ((DefaultResponseTransmitter)ctx.channel().attr((AttributeKey)DefaultResponseTransmitter.ATTRIBUTE_KEY).get()).writabilityChanged();
    }
    
    private boolean isIgnorableException(final Throwable throwable) {
        if (throwable instanceof ClosedChannelException) {
            return true;
        }
        if (throwable instanceof IOException) {
            final String message = throwable.getMessage();
            return message != null && message.endsWith("Connection reset by peer");
        }
        return false;
    }
    
    public static void sendError(final ChannelHandlerContext ctx, final HttpResponseStatus status) {
        final FullHttpResponse response = (FullHttpResponse)new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer((CharSequence)("Failure: " + status.toString() + "\r\n"), CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderConstants.CONTENT_TYPE, (Object)HttpHeaderConstants.PLAIN_TEXT_UTF8);
        ctx.writeAndFlush((Object)response).addListener((GenericFutureListener)ChannelFutureListener.CLOSE);
    }
    
    static {
        CHANNEL_SUBSCRIBER_ATTRIBUTE_KEY = AttributeKey.valueOf("ratpack.subscriber");
        BODY_ACCUMULATOR_KEY = AttributeKey.valueOf(RequestBodyAccumulator.class.getName());
        LOGGER = LoggerFactory.getLogger((Class)NettyHandlerAdapter.class);
    }
}
