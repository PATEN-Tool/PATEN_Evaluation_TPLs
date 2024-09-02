// 
// Decompiled by Procyon v0.5.36
// 

package org.infinispan.rest;

import org.infinispan.util.logging.LogFactory;
import org.infinispan.rest.framework.RestResponse;
import io.netty.channel.unix.Errors;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.TooLongFrameException;
import org.infinispan.rest.framework.Invocation;
import org.infinispan.rest.framework.LookupResult;
import java.util.Objects;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.infinispan.rest.framework.RestRequest;
import java.net.InetSocketAddress;
import org.infinispan.rest.framework.Method;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.channel.ChannelHandlerContext;
import org.infinispan.rest.authentication.Authenticator;
import javax.security.auth.Subject;
import org.infinispan.rest.configuration.RestServerConfiguration;
import org.infinispan.rest.logging.Log;

public class RestRequestHandler extends BaseHttpRequestHandler
{
    protected static final Log logger;
    protected final RestServer restServer;
    protected final RestServerConfiguration configuration;
    private final String context;
    private Subject subject;
    private String authorization;
    private final Authenticator authenticator;
    
    RestRequestHandler(final RestServer restServer) {
        this.restServer = restServer;
        this.configuration = (RestServerConfiguration)restServer.getConfiguration();
        this.authenticator = (this.configuration.authentication().enabled() ? this.configuration.authentication().authenticator() : null);
        this.context = this.configuration.contextPath();
    }
    
    public void channelRead0(final ChannelHandlerContext ctx, final FullHttpRequest request) {
        this.restAccessLoggingHandler.preLog(request);
        if (HttpUtil.is100ContinueExpected((HttpMessage)request)) {
            ctx.write((Object)new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE));
        }
        if (!Method.contains(request.method().name())) {
            final NettyRestResponse restResponse = new NettyRestResponse.Builder().status(HttpResponseStatus.FORBIDDEN).build();
            this.sendResponse(ctx, request, restResponse);
            return;
        }
        NettyRestRequest restRequest;
        LookupResult invocationLookup;
        try {
            restRequest = new NettyRestRequest(request, (InetSocketAddress)ctx.channel().remoteAddress());
            invocationLookup = this.restServer.getRestDispatcher().lookupInvocation(restRequest);
            final Invocation invocation = invocationLookup.getInvocation();
            if (invocation != null && invocation.deprecated()) {
                RestRequestHandler.logger.warnDeprecatedCall(restRequest.toString());
            }
        }
        catch (Exception e) {
            if (RestRequestHandler.logger.isDebugEnabled()) {
                RestRequestHandler.logger.debug((Object)"Error during REST dispatch", (Throwable)e);
            }
            final NettyRestResponse restResponse2 = new NettyRestResponse.Builder().status(HttpResponseStatus.BAD_REQUEST).build();
            this.sendResponse(ctx, request, restResponse2);
            return;
        }
        if (this.authenticator == null || this.isAnon(invocationLookup)) {
            this.handleRestRequest(ctx, restRequest, invocationLookup);
            return;
        }
        if (this.subject != null) {
            final String authz = request.headers().get((CharSequence)HttpHeaderNames.AUTHORIZATION);
            if (Objects.equals(authz, this.authorization)) {
                if (RestRequestHandler.logger.isTraceEnabled()) {
                    RestRequestHandler.logger.tracef("Authorization header match, skipping authentication for %s", (Object)request);
                }
                restRequest.setSubject(this.subject);
                this.handleRestRequest(ctx, restRequest, invocationLookup);
                return;
            }
            if (RestRequestHandler.logger.isTraceEnabled()) {
                RestRequestHandler.logger.tracef("Authorization header mismatch:\n%s\n%s", (Object)authz, (Object)this.authorization);
            }
            this.subject = null;
            this.authorization = null;
        }
        final boolean hasError;
        final boolean authorized;
        final NettyRestRequest restRequest2;
        final LookupResult invocationLookup2;
        this.authenticator.challenge(restRequest, ctx).whenComplete((authResponse, authThrowable) -> {
            hasError = (authThrowable != null);
            authorized = (!hasError && authResponse.getStatus() < HttpResponseStatus.BAD_REQUEST.code());
            if (authorized) {
                this.authorization = restRequest2.getAuthorizationHeader();
                this.subject = restRequest2.getSubject();
                this.handleRestRequest(ctx, restRequest2, invocationLookup2);
            }
            else {
                try {
                    if (hasError) {
                        this.handleError(ctx, request, authThrowable);
                    }
                    else {
                        this.sendResponse(ctx, request, authResponse);
                    }
                }
                finally {
                    request.release();
                }
            }
        });
    }
    
    private boolean isAnon(final LookupResult lookupResult) {
        return lookupResult == null || lookupResult.getInvocation() == null || lookupResult.getInvocation().anonymous();
    }
    
    private void handleRestRequest(final ChannelHandlerContext ctx, final NettyRestRequest restRequest, final LookupResult invocationLookup) {
        final FullHttpRequest request;
        NettyRestResponse nettyRestResponse;
        this.restServer.getRestDispatcher().dispatch(restRequest, invocationLookup).whenComplete((restResponse, throwable) -> {
            request = restRequest.getFullHttpRequest();
            try {
                if (throwable == null) {
                    nettyRestResponse = restResponse;
                    this.sendResponse(ctx, request, nettyRestResponse);
                }
                else {
                    this.handleError(ctx, request, throwable);
                }
            }
            finally {
                request.release();
            }
        });
    }
    
    @Override
    protected Log getLogger() {
        return RestRequestHandler.logger;
    }
    
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable e) {
        if (e.getCause() instanceof TooLongFrameException) {
            final DefaultHttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE);
            ctx.write((Object)response).addListener((GenericFutureListener)ChannelFutureListener.CLOSE);
        }
        else if (e instanceof Errors.NativeIoException) {
            RestRequestHandler.logger.debug((Object)"Native IO Exception", e);
            ctx.close();
        }
        else if (!(e instanceof IllegalStateException) || !e.getMessage().equals("ssl is null")) {
            RestRequestHandler.logger.uncaughtExceptionInThePipeline(e);
            ctx.close();
        }
    }
    
    static {
        logger = (Log)LogFactory.getLog((Class)RestRequestHandler.class, (Class)Log.class);
    }
}
