// 
// Decompiled by Procyon v0.5.36
// 

package io.undertow.server.handlers;

import java.util.concurrent.TimeUnit;
import org.xnio.channels.StreamSinkChannel;
import java.nio.ByteBuffer;
import java.io.IOException;
import java.nio.channels.FileChannel;
import org.xnio.conduits.AbstractStreamSourceConduit;
import org.xnio.conduits.Conduit;
import io.undertow.util.ConduitFactory;
import io.undertow.server.Connectors;
import io.undertow.server.ResponseCommitListener;
import io.undertow.server.protocol.http.HttpContinue;
import io.undertow.server.HttpServerExchange;
import org.xnio.conduits.StreamSourceConduit;
import io.undertow.server.ConduitWrapper;
import io.undertow.server.HttpHandler;

public class HttpContinueReadHandler implements HttpHandler
{
    private static final ConduitWrapper<StreamSourceConduit> WRAPPER;
    private final HttpHandler handler;
    
    public HttpContinueReadHandler(final HttpHandler handler) {
        this.handler = handler;
    }
    
    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        if (HttpContinue.requiresContinueResponse(exchange)) {
            exchange.addRequestWrapper(HttpContinueReadHandler.WRAPPER);
            exchange.addResponseCommitListener(new ResponseCommitListener() {
                @Override
                public void beforeCommit(final HttpServerExchange exchange) {
                    if (!HttpContinue.isContinueResponseSent(exchange)) {
                        exchange.setPersistent(false);
                        if (!exchange.isRequestComplete()) {
                            exchange.getConnection().terminateRequestChannel(exchange);
                        }
                        else {
                            Connectors.terminateRequest(exchange);
                        }
                    }
                }
            });
        }
        this.handler.handleRequest(exchange);
    }
    
    static {
        WRAPPER = new ConduitWrapper<StreamSourceConduit>() {
            @Override
            public StreamSourceConduit wrap(final ConduitFactory<StreamSourceConduit> factory, final HttpServerExchange exchange) {
                if (exchange.isRequestChannelAvailable() && !exchange.isResponseStarted()) {
                    return (StreamSourceConduit)new ContinueConduit(factory.create(), exchange);
                }
                return factory.create();
            }
        };
    }
    
    private static final class ContinueConduit extends AbstractStreamSourceConduit<StreamSourceConduit> implements StreamSourceConduit
    {
        private boolean sent;
        private HttpContinue.ContinueResponseSender response;
        private final HttpServerExchange exchange;
        
        protected ContinueConduit(final StreamSourceConduit next, final HttpServerExchange exchange) {
            super(next);
            this.sent = false;
            this.response = null;
            this.exchange = exchange;
        }
        
        public long transferTo(final long position, final long count, final FileChannel target) throws IOException {
            if (this.exchange.getStatusCode() == 417) {
                Connectors.terminateRequest(this.exchange);
                return -1L;
            }
            if (!this.sent) {
                this.sent = true;
                this.response = HttpContinue.createResponseSender(this.exchange);
            }
            if (this.response != null) {
                if (!this.response.send()) {
                    return 0L;
                }
                this.response = null;
            }
            return super.transferTo(position, count, target);
        }
        
        public long transferTo(final long count, final ByteBuffer throughBuffer, final StreamSinkChannel target) throws IOException {
            if (this.exchange.getStatusCode() == 417) {
                Connectors.terminateRequest(this.exchange);
                return -1L;
            }
            if (!this.sent) {
                this.sent = true;
                this.response = HttpContinue.createResponseSender(this.exchange);
            }
            if (this.response != null) {
                if (!this.response.send()) {
                    return 0L;
                }
                this.response = null;
            }
            return super.transferTo(count, throughBuffer, target);
        }
        
        public int read(final ByteBuffer dst) throws IOException {
            if (this.exchange.getStatusCode() == 417) {
                Connectors.terminateRequest(this.exchange);
                return -1;
            }
            if (!this.sent) {
                this.sent = true;
                this.response = HttpContinue.createResponseSender(this.exchange);
            }
            if (this.response != null) {
                if (!this.response.send()) {
                    return 0;
                }
                this.response = null;
            }
            return super.read(dst);
        }
        
        public long read(final ByteBuffer[] dsts, final int offs, final int len) throws IOException {
            if (this.exchange.getStatusCode() == 417) {
                Connectors.terminateRequest(this.exchange);
                return -1L;
            }
            if (!this.sent) {
                this.sent = true;
                this.response = HttpContinue.createResponseSender(this.exchange);
            }
            if (this.response != null) {
                if (!this.response.send()) {
                    return 0L;
                }
                this.response = null;
            }
            return super.read(dsts, offs, len);
        }
        
        public void awaitReadable(final long time, final TimeUnit timeUnit) throws IOException {
            if (this.exchange.getStatusCode() == 417) {
                return;
            }
            if (!this.sent) {
                this.sent = true;
                this.response = HttpContinue.createResponseSender(this.exchange);
            }
            final long exitTime = System.currentTimeMillis() + timeUnit.toMillis(time);
            if (this.response != null) {
                while (!this.response.send()) {
                    final long currentTime = System.currentTimeMillis();
                    if (currentTime > exitTime) {
                        return;
                    }
                    this.response.awaitWritable(exitTime - currentTime, TimeUnit.MILLISECONDS);
                }
                this.response = null;
            }
            final long currentTime = System.currentTimeMillis();
            super.awaitReadable(exitTime - currentTime, TimeUnit.MILLISECONDS);
        }
        
        public void awaitReadable() throws IOException {
            if (this.exchange.getStatusCode() == 417) {
                return;
            }
            if (!this.sent) {
                this.sent = true;
                this.response = HttpContinue.createResponseSender(this.exchange);
            }
            if (this.response != null) {
                while (!this.response.send()) {
                    this.response.awaitWritable();
                }
                this.response = null;
            }
            super.awaitReadable();
        }
    }
}
