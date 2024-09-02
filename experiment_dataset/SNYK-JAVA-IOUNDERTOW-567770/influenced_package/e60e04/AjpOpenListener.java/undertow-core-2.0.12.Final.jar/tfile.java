// 
// Decompiled by Procyon v0.5.36
// 

package io.undertow.server.protocol.ajp;

import java.nio.channels.Channel;
import io.undertow.server.ConnectorStatistics;
import io.undertow.UndertowMessages;
import org.xnio.channels.StreamSourceChannel;
import org.xnio.ChannelListener;
import io.undertow.conduits.BytesReceivedStreamSourceConduit;
import io.undertow.conduits.BytesSentStreamSinkConduit;
import java.io.IOException;
import java.io.Closeable;
import org.xnio.IoUtils;
import io.undertow.conduits.WriteTimeoutStreamSinkConduit;
import io.undertow.conduits.ReadTimeoutStreamSourceConduit;
import org.xnio.conduits.StreamSinkConduit;
import org.xnio.conduits.StreamSourceConduit;
import io.undertow.conduits.IdleTimeoutConduit;
import org.xnio.Options;
import io.undertow.UndertowLogger;
import org.xnio.StreamConnection;
import io.undertow.connector.PooledByteBuffer;
import org.xnio.Option;
import java.nio.charset.StandardCharsets;
import io.undertow.UndertowOptions;
import io.undertow.server.XnioByteBufferPool;
import java.nio.ByteBuffer;
import org.xnio.Pool;
import io.undertow.server.ServerConnection;
import io.undertow.server.ConnectorStatisticsImpl;
import org.xnio.OptionMap;
import io.undertow.server.HttpHandler;
import io.undertow.connector.ByteBufferPool;
import io.undertow.server.OpenListener;

public class AjpOpenListener implements OpenListener
{
    private final ByteBufferPool bufferPool;
    private final int bufferSize;
    private volatile String scheme;
    private volatile HttpHandler rootHandler;
    private volatile OptionMap undertowOptions;
    private volatile AjpRequestParser parser;
    private volatile boolean statisticsEnabled;
    private final ConnectorStatisticsImpl connectorStatistics;
    private final ServerConnection.CloseListener closeListener;
    
    public AjpOpenListener(final Pool<ByteBuffer> pool) {
        this(pool, OptionMap.EMPTY);
    }
    
    public AjpOpenListener(final Pool<ByteBuffer> pool, final OptionMap undertowOptions) {
        this(new XnioByteBufferPool(pool), undertowOptions);
    }
    
    public AjpOpenListener(final ByteBufferPool pool) {
        this(pool, OptionMap.EMPTY);
    }
    
    public AjpOpenListener(final ByteBufferPool pool, final OptionMap undertowOptions) {
        this.closeListener = new ServerConnection.CloseListener() {
            @Override
            public void closed(final ServerConnection connection) {
                AjpOpenListener.this.connectorStatistics.decrementConnectionCount();
            }
        };
        this.undertowOptions = undertowOptions;
        this.bufferPool = pool;
        final PooledByteBuffer buf = pool.allocate();
        this.bufferSize = buf.getBuffer().remaining();
        buf.close();
        this.parser = new AjpRequestParser((String)undertowOptions.get((Option)UndertowOptions.URL_CHARSET, (Object)StandardCharsets.UTF_8.name()), undertowOptions.get((Option)UndertowOptions.DECODE_URL, true), undertowOptions.get((Option)UndertowOptions.MAX_PARAMETERS, 1000), undertowOptions.get((Option)UndertowOptions.MAX_HEADERS, 200), undertowOptions.get((Option)UndertowOptions.ALLOW_ENCODED_SLASH, false), undertowOptions.get((Option)UndertowOptions.ALLOW_UNESCAPED_CHARACTERS_IN_URL, false));
        this.connectorStatistics = new ConnectorStatisticsImpl();
        this.statisticsEnabled = undertowOptions.get((Option)UndertowOptions.ENABLE_CONNECTOR_STATISTICS, false);
    }
    
    public void handleEvent(final StreamConnection channel) {
        if (UndertowLogger.REQUEST_LOGGER.isTraceEnabled()) {
            UndertowLogger.REQUEST_LOGGER.tracef("Opened connection with %s", (Object)channel.getPeerAddress());
        }
        try {
            final Integer readTimeout = (Integer)channel.getOption(Options.READ_TIMEOUT);
            final Integer idle = (Integer)this.undertowOptions.get((Option)UndertowOptions.IDLE_TIMEOUT);
            if (idle != null) {
                final IdleTimeoutConduit conduit = new IdleTimeoutConduit(channel);
                channel.getSourceChannel().setConduit((StreamSourceConduit)conduit);
                channel.getSinkChannel().setConduit((StreamSinkConduit)conduit);
            }
            if (readTimeout != null && readTimeout > 0) {
                channel.getSourceChannel().setConduit((StreamSourceConduit)new ReadTimeoutStreamSourceConduit(channel.getSourceChannel().getConduit(), channel, this));
            }
            final Integer writeTimeout = (Integer)channel.getOption(Options.WRITE_TIMEOUT);
            if (writeTimeout != null && writeTimeout > 0) {
                channel.getSinkChannel().setConduit((StreamSinkConduit)new WriteTimeoutStreamSinkConduit(channel.getSinkChannel().getConduit(), channel, this));
            }
        }
        catch (IOException e) {
            IoUtils.safeClose((Closeable)channel);
            UndertowLogger.REQUEST_IO_LOGGER.ioException(e);
        }
        if (this.statisticsEnabled) {
            channel.getSinkChannel().setConduit((StreamSinkConduit)new BytesSentStreamSinkConduit(channel.getSinkChannel().getConduit(), this.connectorStatistics.sentAccumulator()));
            channel.getSourceChannel().setConduit((StreamSourceConduit)new BytesReceivedStreamSourceConduit(channel.getSourceChannel().getConduit(), this.connectorStatistics.receivedAccumulator()));
            this.connectorStatistics.incrementConnectionCount();
        }
        final AjpServerConnection connection = new AjpServerConnection(channel, this.bufferPool, this.rootHandler, this.undertowOptions, this.bufferSize);
        final AjpReadListener readListener = new AjpReadListener(connection, this.scheme, this.parser, this.statisticsEnabled ? this.connectorStatistics : null);
        if (this.statisticsEnabled) {
            connection.addCloseListener(this.closeListener);
        }
        connection.setAjpReadListener(readListener);
        readListener.startRequest();
        channel.getSourceChannel().setReadListener((ChannelListener)readListener);
        readListener.handleEvent((StreamSourceChannel)channel.getSourceChannel());
    }
    
    @Override
    public HttpHandler getRootHandler() {
        return this.rootHandler;
    }
    
    @Override
    public void setRootHandler(final HttpHandler rootHandler) {
        this.rootHandler = rootHandler;
    }
    
    @Override
    public OptionMap getUndertowOptions() {
        return this.undertowOptions;
    }
    
    @Override
    public void setUndertowOptions(final OptionMap undertowOptions) {
        if (undertowOptions == null) {
            throw UndertowMessages.MESSAGES.argumentCannotBeNull("undertowOptions");
        }
        this.undertowOptions = undertowOptions;
        this.statisticsEnabled = undertowOptions.get((Option)UndertowOptions.ENABLE_CONNECTOR_STATISTICS, false);
        this.parser = new AjpRequestParser((String)undertowOptions.get((Option)UndertowOptions.URL_CHARSET, (Object)StandardCharsets.UTF_8.name()), undertowOptions.get((Option)UndertowOptions.DECODE_URL, true), undertowOptions.get((Option)UndertowOptions.MAX_PARAMETERS, 1000), undertowOptions.get((Option)UndertowOptions.MAX_HEADERS, 200), undertowOptions.get((Option)UndertowOptions.ALLOW_ENCODED_SLASH, false), undertowOptions.get((Option)UndertowOptions.ALLOW_UNESCAPED_CHARACTERS_IN_URL, false));
    }
    
    @Override
    public ByteBufferPool getBufferPool() {
        return this.bufferPool;
    }
    
    @Override
    public ConnectorStatistics getConnectorStatistics() {
        if (this.statisticsEnabled) {
            return this.connectorStatistics;
        }
        return null;
    }
    
    public String getScheme() {
        return this.scheme;
    }
    
    public void setScheme(final String scheme) {
        this.scheme = scheme;
    }
}
