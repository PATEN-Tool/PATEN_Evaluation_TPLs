// 
// Decompiled by Procyon v0.5.36
// 

package io.undertow.server.protocol.http;

import java.util.Collections;
import io.undertow.util.Protocols;
import java.util.HashSet;
import org.xnio.ChannelListeners;
import org.xnio.ChannelExceptionHandler;
import java.nio.channels.Channel;
import io.undertow.server.HttpHandler;
import org.xnio.ChannelListener;
import org.xnio.channels.StreamSinkChannel;
import java.util.concurrent.TimeUnit;
import java.io.IOException;
import io.undertow.io.Sender;
import io.undertow.UndertowMessages;
import io.undertow.io.IoCallback;
import java.util.Iterator;
import java.util.List;
import io.undertow.util.Headers;
import io.undertow.util.HeaderMap;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;
import io.undertow.util.HttpString;
import java.util.Set;

public class HttpContinue
{
    private static final Set<HttpString> COMPATIBLE_PROTOCOLS;
    public static final String CONTINUE = "100-continue";
    private static final AttachmentKey<Boolean> ALREADY_SENT;
    
    public static boolean requiresContinueResponse(final HttpServerExchange exchange) {
        if (!HttpContinue.COMPATIBLE_PROTOCOLS.contains(exchange.getProtocol()) || exchange.isResponseStarted() || !exchange.getConnection().isContinueResponseSupported() || exchange.getAttachment(HttpContinue.ALREADY_SENT) != null) {
            return false;
        }
        final HeaderMap requestHeaders = exchange.getRequestHeaders();
        return requiresContinueResponse(requestHeaders);
    }
    
    public static boolean requiresContinueResponse(final HeaderMap requestHeaders) {
        final List<String> expect = requestHeaders.get(Headers.EXPECT);
        if (expect != null) {
            for (final String header : expect) {
                if (header.equalsIgnoreCase("100-continue")) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public static boolean isContinueResponseSent(final HttpServerExchange exchange) {
        return exchange.getAttachment(HttpContinue.ALREADY_SENT) != null;
    }
    
    public static void sendContinueResponse(final HttpServerExchange exchange, final IoCallback callback) {
        if (!exchange.isResponseChannelAvailable()) {
            callback.onException(exchange, null, UndertowMessages.MESSAGES.cannotSendContinueResponse());
            return;
        }
        internalSendContinueResponse(exchange, callback);
    }
    
    public static ContinueResponseSender createResponseSender(final HttpServerExchange exchange) throws IOException {
        if (!exchange.isResponseChannelAvailable()) {
            throw UndertowMessages.MESSAGES.cannotSendContinueResponse();
        }
        if (exchange.getAttachment(HttpContinue.ALREADY_SENT) != null) {
            return new ContinueResponseSender() {
                @Override
                public boolean send() throws IOException {
                    return true;
                }
                
                @Override
                public void awaitWritable() throws IOException {
                }
                
                @Override
                public void awaitWritable(final long time, final TimeUnit timeUnit) throws IOException {
                }
            };
        }
        final HttpServerExchange newExchange = exchange.getConnection().sendOutOfBandResponse(exchange);
        exchange.putAttachment(HttpContinue.ALREADY_SENT, true);
        newExchange.setStatusCode(100);
        newExchange.getResponseHeaders().put(Headers.CONTENT_LENGTH, 0L);
        final StreamSinkChannel responseChannel = newExchange.getResponseChannel();
        return new ContinueResponseSender() {
            boolean shutdown = false;
            
            @Override
            public boolean send() throws IOException {
                if (!this.shutdown) {
                    this.shutdown = true;
                    responseChannel.shutdownWrites();
                }
                return responseChannel.flush();
            }
            
            @Override
            public void awaitWritable() throws IOException {
                responseChannel.awaitWritable();
            }
            
            @Override
            public void awaitWritable(final long time, final TimeUnit timeUnit) throws IOException {
                responseChannel.awaitWritable(time, timeUnit);
            }
        };
    }
    
    public static void markContinueResponseSent(final HttpServerExchange exchange) {
        exchange.putAttachment(HttpContinue.ALREADY_SENT, true);
    }
    
    public static void sendContinueResponseBlocking(final HttpServerExchange exchange) throws IOException {
        if (!exchange.isResponseChannelAvailable()) {
            throw UndertowMessages.MESSAGES.cannotSendContinueResponse();
        }
        if (exchange.getAttachment(HttpContinue.ALREADY_SENT) != null) {
            return;
        }
        final HttpServerExchange newExchange = exchange.getConnection().sendOutOfBandResponse(exchange);
        exchange.putAttachment(HttpContinue.ALREADY_SENT, true);
        newExchange.setStatusCode(100);
        newExchange.getResponseHeaders().put(Headers.CONTENT_LENGTH, 0L);
        newExchange.startBlocking();
        newExchange.getOutputStream().close();
        newExchange.getInputStream().close();
    }
    
    public static void rejectExchange(final HttpServerExchange exchange) {
        exchange.setStatusCode(417);
        exchange.setPersistent(false);
        exchange.endExchange();
    }
    
    private static void internalSendContinueResponse(final HttpServerExchange exchange, final IoCallback callback) {
        if (exchange.getAttachment(HttpContinue.ALREADY_SENT) != null) {
            callback.onComplete(exchange, null);
            return;
        }
        final HttpServerExchange newExchange = exchange.getConnection().sendOutOfBandResponse(exchange);
        exchange.putAttachment(HttpContinue.ALREADY_SENT, true);
        newExchange.setStatusCode(100);
        newExchange.getResponseHeaders().put(Headers.CONTENT_LENGTH, 0L);
        final StreamSinkChannel responseChannel = newExchange.getResponseChannel();
        try {
            responseChannel.shutdownWrites();
            if (!responseChannel.flush()) {
                responseChannel.getWriteSetter().set(ChannelListeners.flushingChannelListener((ChannelListener)new ChannelListener<StreamSinkChannel>() {
                    public void handleEvent(final StreamSinkChannel channel) {
                        channel.suspendWrites();
                        exchange.dispatch(new HttpHandler() {
                            @Override
                            public void handleRequest(final HttpServerExchange exchange) throws Exception {
                                callback.onComplete(exchange, null);
                            }
                        });
                    }
                }, (ChannelExceptionHandler)new ChannelExceptionHandler<Channel>() {
                    public void handleException(final Channel channel, final IOException e) {
                        exchange.dispatch(new HttpHandler() {
                            @Override
                            public void handleRequest(final HttpServerExchange exchange) throws Exception {
                                callback.onException(exchange, null, e);
                            }
                        });
                    }
                }));
                responseChannel.resumeWrites();
                exchange.dispatch();
            }
            else {
                callback.onComplete(exchange, null);
            }
        }
        catch (IOException e) {
            callback.onException(exchange, null, e);
        }
    }
    
    static {
        final Set<HttpString> compat = new HashSet<HttpString>();
        compat.add(Protocols.HTTP_1_1);
        compat.add(Protocols.HTTP_2_0);
        COMPATIBLE_PROTOCOLS = Collections.unmodifiableSet((Set<? extends HttpString>)compat);
        ALREADY_SENT = AttachmentKey.create((Class<? super Boolean>)Boolean.class);
    }
    
    public interface ContinueResponseSender
    {
        boolean send() throws IOException;
        
        void awaitWritable() throws IOException;
        
        void awaitWritable(final long p0, final TimeUnit p1) throws IOException;
    }
}
