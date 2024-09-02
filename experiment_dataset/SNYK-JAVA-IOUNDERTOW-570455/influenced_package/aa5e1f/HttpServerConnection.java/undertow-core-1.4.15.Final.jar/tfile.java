// 
// Decompiled by Procyon v0.5.36
// 

package io.undertow.server.protocol.http;

import org.xnio.conduits.ConduitStreamSinkChannel;
import io.undertow.util.Methods;
import org.xnio.conduits.StreamSourceConduit;
import javax.net.ssl.SSLSession;
import io.undertow.util.ImmediatePooledByteBuffer;
import java.nio.ByteBuffer;
import io.undertow.connector.PooledByteBuffer;
import java.util.Iterator;
import io.undertow.server.ExchangeCompletionListener;
import org.xnio.conduits.Conduit;
import io.undertow.util.ConduitFactory;
import io.undertow.server.ConduitWrapper;
import io.undertow.server.Connectors;
import io.undertow.util.Headers;
import java.util.Collection;
import io.undertow.util.HttpString;
import io.undertow.UndertowMessages;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.ServerConnection;
import org.xnio.conduits.StreamSinkConduit;
import io.undertow.server.ConnectionSSLSessionInfo;
import org.xnio.channels.SslChannel;
import io.undertow.server.ConnectorStatisticsImpl;
import org.xnio.OptionMap;
import io.undertow.server.HttpHandler;
import io.undertow.connector.ByteBufferPool;
import org.xnio.StreamConnection;
import io.undertow.server.HttpUpgradeListener;
import io.undertow.conduits.ReadDataStreamSourceConduit;
import io.undertow.server.SSLSessionInfo;
import io.undertow.server.AbstractServerConnection;

public final class HttpServerConnection extends AbstractServerConnection
{
    private SSLSessionInfo sslSessionInfo;
    private HttpReadListener readListener;
    private PipeliningBufferingStreamSinkConduit pipelineBuffer;
    private HttpResponseConduit responseConduit;
    private ServerFixedLengthStreamSinkConduit fixedLengthStreamSinkConduit;
    private ReadDataStreamSourceConduit readDataStreamSourceConduit;
    private HttpUpgradeListener upgradeListener;
    private boolean connectHandled;
    
    public HttpServerConnection(final StreamConnection channel, final ByteBufferPool bufferPool, final HttpHandler rootHandler, final OptionMap undertowOptions, final int bufferSize, final ConnectorStatisticsImpl connectorStatistics) {
        super(channel, bufferPool, rootHandler, undertowOptions, bufferSize);
        if (channel instanceof SslChannel) {
            this.sslSessionInfo = new ConnectionSSLSessionInfo((SslChannel)channel, this);
        }
        this.responseConduit = new HttpResponseConduit(channel.getSinkChannel().getConduit(), bufferPool, this);
        this.fixedLengthStreamSinkConduit = new ServerFixedLengthStreamSinkConduit((StreamSinkConduit)this.responseConduit, false, false);
        this.readDataStreamSourceConduit = new ReadDataStreamSourceConduit(channel.getSourceChannel().getConduit(), this);
        this.addCloseListener(new CloseListener() {
            @Override
            public void closed(final ServerConnection connection) {
                if (connectorStatistics != null) {
                    connectorStatistics.decrementConnectionCount();
                }
                HttpServerConnection.this.responseConduit.freeBuffers();
            }
        });
    }
    
    @Override
    public HttpServerExchange sendOutOfBandResponse(final HttpServerExchange exchange) {
        if (exchange == null || !HttpContinue.requiresContinueResponse(exchange)) {
            throw UndertowMessages.MESSAGES.outOfBandResponseOnlyAllowedFor100Continue();
        }
        final ConduitState state = this.resetChannel();
        final HttpServerExchange newExchange = new HttpServerExchange(this);
        for (final HttpString header : exchange.getRequestHeaders().getHeaderNames()) {
            newExchange.getRequestHeaders().putAll(header, exchange.getRequestHeaders().get(header));
        }
        newExchange.setProtocol(exchange.getProtocol());
        newExchange.setRequestMethod(exchange.getRequestMethod());
        exchange.setRequestURI(exchange.getRequestURI(), exchange.isHostIncludedInRequestURI());
        exchange.setRequestPath(exchange.getRequestPath());
        exchange.setRelativePath(exchange.getRelativePath());
        newExchange.getRequestHeaders().put(Headers.CONNECTION, Headers.KEEP_ALIVE.toString());
        newExchange.getRequestHeaders().put(Headers.CONTENT_LENGTH, 0L);
        newExchange.setPersistent(true);
        Connectors.terminateRequest(newExchange);
        newExchange.addResponseWrapper(new ConduitWrapper<StreamSinkConduit>() {
            @Override
            public StreamSinkConduit wrap(final ConduitFactory<StreamSinkConduit> factory, final HttpServerExchange exchange) {
                final ServerFixedLengthStreamSinkConduit fixed = new ServerFixedLengthStreamSinkConduit((StreamSinkConduit)new HttpResponseConduit(AbstractServerConnection.this.getSinkChannel().getConduit(), HttpServerConnection.this.getByteBufferPool(), HttpServerConnection.this, exchange), false, false);
                fixed.reset(0L, exchange);
                return (StreamSinkConduit)fixed;
            }
        });
        this.channel.getSourceChannel().setConduit(AbstractServerConnection.source(state));
        newExchange.addExchangeCompleteListener(new ExchangeCompletionListener() {
            @Override
            public void exchangeEvent(final HttpServerExchange exchange, final NextListener nextListener) {
                HttpServerConnection.this.restoreChannel(state);
            }
        });
        return newExchange;
    }
    
    @Override
    public boolean isContinueResponseSupported() {
        return true;
    }
    
    @Override
    public void terminateRequestChannel(final HttpServerExchange exchange) {
    }
    
    public void ungetRequestBytes(final PooledByteBuffer unget) {
        if (this.getExtraBytes() == null) {
            this.setExtraBytes(unget);
        }
        else {
            final PooledByteBuffer eb = this.getExtraBytes();
            final ByteBuffer buf = eb.getBuffer();
            final ByteBuffer ugBuffer = unget.getBuffer();
            if (ugBuffer.limit() - ugBuffer.remaining() > buf.remaining()) {
                ugBuffer.compact();
                ugBuffer.put(buf);
                ugBuffer.flip();
                eb.close();
                this.setExtraBytes(unget);
            }
            else {
                final byte[] data = new byte[ugBuffer.remaining() + buf.remaining()];
                final int first = ugBuffer.remaining();
                ugBuffer.get(data, 0, ugBuffer.remaining());
                buf.get(data, first, buf.remaining());
                eb.close();
                unget.close();
                final ByteBuffer newBuffer = ByteBuffer.wrap(data);
                this.setExtraBytes(new ImmediatePooledByteBuffer(newBuffer));
            }
        }
    }
    
    @Override
    public SSLSessionInfo getSslSessionInfo() {
        return this.sslSessionInfo;
    }
    
    @Override
    public void setSslSessionInfo(final SSLSessionInfo sessionInfo) {
        this.sslSessionInfo = sessionInfo;
    }
    
    public SSLSession getSslSession() {
        if (this.channel instanceof SslChannel) {
            return ((SslChannel)this.channel).getSslSession();
        }
        return null;
    }
    
    @Override
    protected StreamConnection upgradeChannel() {
        this.clearChannel();
        if (this.extraBytes != null) {
            this.channel.getSourceChannel().setConduit((StreamSourceConduit)new ReadDataStreamSourceConduit(this.channel.getSourceChannel().getConduit(), this));
        }
        return this.channel;
    }
    
    @Override
    protected StreamSinkConduit getSinkConduit(final HttpServerExchange exchange, final StreamSinkConduit conduit) {
        if (exchange.getRequestMethod().equals(Methods.CONNECT) && !this.connectHandled) {
            exchange.setPersistent(false);
            exchange.getResponseHeaders().put(Headers.CONNECTION, "close");
        }
        return HttpTransferEncoding.createSinkConduit(exchange);
    }
    
    @Override
    protected boolean isUpgradeSupported() {
        return true;
    }
    
    @Override
    protected boolean isConnectSupported() {
        return true;
    }
    
    void setReadListener(final HttpReadListener readListener) {
        this.readListener = readListener;
    }
    
    @Override
    protected void exchangeComplete(final HttpServerExchange exchange) {
        if (this.fixedLengthStreamSinkConduit != null) {
            this.fixedLengthStreamSinkConduit.clearExchange();
        }
        if (this.pipelineBuffer == null) {
            this.readListener.exchangeComplete(exchange);
        }
        else {
            this.pipelineBuffer.exchangeComplete(exchange);
        }
    }
    
    HttpReadListener getReadListener() {
        return this.readListener;
    }
    
    ReadDataStreamSourceConduit getReadDataStreamSourceConduit() {
        return this.readDataStreamSourceConduit;
    }
    
    public PipeliningBufferingStreamSinkConduit getPipelineBuffer() {
        return this.pipelineBuffer;
    }
    
    public HttpResponseConduit getResponseConduit() {
        return this.responseConduit;
    }
    
    ServerFixedLengthStreamSinkConduit getFixedLengthStreamSinkConduit() {
        return this.fixedLengthStreamSinkConduit;
    }
    
    protected HttpUpgradeListener getUpgradeListener() {
        return this.upgradeListener;
    }
    
    @Override
    protected void setUpgradeListener(final HttpUpgradeListener upgradeListener) {
        this.upgradeListener = upgradeListener;
    }
    
    @Override
    protected void setConnectListener(final HttpUpgradeListener connectListener) {
        this.upgradeListener = connectListener;
        this.connectHandled = true;
    }
    
    void setCurrentExchange(final HttpServerExchange exchange) {
        this.current = exchange;
    }
    
    public void setPipelineBuffer(final PipeliningBufferingStreamSinkConduit pipelineBuffer) {
        this.pipelineBuffer = pipelineBuffer;
        this.responseConduit = new HttpResponseConduit((StreamSinkConduit)pipelineBuffer, this.bufferPool, this);
        this.fixedLengthStreamSinkConduit = new ServerFixedLengthStreamSinkConduit((StreamSinkConduit)this.responseConduit, false, false);
    }
    
    @Override
    public String getTransportProtocol() {
        return "http/1.1";
    }
    
    boolean isConnectHandled() {
        return this.connectHandled;
    }
}
