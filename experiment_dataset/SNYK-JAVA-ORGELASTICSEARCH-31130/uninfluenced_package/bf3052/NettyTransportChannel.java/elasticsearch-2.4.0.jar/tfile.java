// 
// Decompiled by Procyon v0.5.36
// 

package org.elasticsearch.transport.netty;

import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.transport.RemoteTransportException;
import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.jboss.netty.buffer.ChannelBuffer;
import org.elasticsearch.common.bytes.ReleasablePagedBytesReference;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.lease.Releasables;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.elasticsearch.common.lease.Releasable;
import org.elasticsearch.common.netty.ReleaseChannelFutureListener;
import org.elasticsearch.common.compress.CompressorFactory;
import org.elasticsearch.common.io.stream.ReleasableBytesStreamOutput;
import org.elasticsearch.transport.support.TransportStatus;
import java.io.IOException;
import org.elasticsearch.transport.TransportResponseOptions;
import org.elasticsearch.transport.TransportResponse;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jboss.netty.channel.Channel;
import org.elasticsearch.Version;
import org.elasticsearch.transport.TransportServiceAdapter;
import org.elasticsearch.transport.TransportChannel;

public class NettyTransportChannel implements TransportChannel
{
    private final NettyTransport transport;
    private final TransportServiceAdapter transportServiceAdapter;
    private final Version version;
    private final String action;
    private final Channel channel;
    private final long requestId;
    private final String profileName;
    private final long reservedBytes;
    private final AtomicBoolean closed;
    
    public NettyTransportChannel(final NettyTransport transport, final TransportServiceAdapter transportServiceAdapter, final String action, final Channel channel, final long requestId, final Version version, final String profileName, final long reservedBytes) {
        this.closed = new AtomicBoolean();
        this.transportServiceAdapter = transportServiceAdapter;
        this.version = version;
        this.transport = transport;
        this.action = action;
        this.channel = channel;
        this.requestId = requestId;
        this.profileName = profileName;
        this.reservedBytes = reservedBytes;
    }
    
    @Override
    public String getProfileName() {
        return this.profileName;
    }
    
    @Override
    public String action() {
        return this.action;
    }
    
    @Override
    public void sendResponse(final TransportResponse response) throws IOException {
        this.sendResponse(response, TransportResponseOptions.EMPTY);
    }
    
    @Override
    public void sendResponse(final TransportResponse response, TransportResponseOptions options) throws IOException {
        this.close();
        if (this.transport.compress) {
            options = TransportResponseOptions.builder(options).withCompress(this.transport.compress).build();
        }
        byte status = 0;
        status = TransportStatus.setResponse(status);
        ReleasableBytesStreamOutput bStream = null;
        boolean addedReleaseListener = false;
        try {
            bStream = new ReleasableBytesStreamOutput(this.transport.bigArrays);
            bStream.skip(19);
            StreamOutput stream = bStream;
            if (options.compress()) {
                status = TransportStatus.setCompress(status);
                stream = CompressorFactory.defaultCompressor().streamOutput(stream);
            }
            stream.setVersion(this.version);
            response.writeTo(stream);
            stream.close();
            final ReleasablePagedBytesReference bytes = bStream.bytes();
            final ChannelBuffer buffer = bytes.toChannelBuffer();
            NettyHeader.writeHeader(buffer, this.requestId, status, this.version);
            final ChannelFuture future = this.channel.write((Object)buffer);
            final ReleaseChannelFutureListener listener = new ReleaseChannelFutureListener(bytes);
            future.addListener((ChannelFutureListener)listener);
            addedReleaseListener = true;
            final TransportResponseOptions finalOptions = options;
            final ChannelFutureListener onResponseSentListener = (ChannelFutureListener)new ChannelFutureListener() {
                public void operationComplete(final ChannelFuture future) throws Exception {
                    NettyTransportChannel.this.transportServiceAdapter.onResponseSent(NettyTransportChannel.this.requestId, NettyTransportChannel.this.action, response, finalOptions);
                }
            };
            future.addListener(onResponseSentListener);
        }
        finally {
            if (!addedReleaseListener && bStream != null) {
                Releasables.close(bStream.bytes());
            }
        }
    }
    
    @Override
    public void sendResponse(final Throwable error) throws IOException {
        this.close();
        final BytesStreamOutput stream = new BytesStreamOutput();
        stream.skip(19);
        final RemoteTransportException tx = new RemoteTransportException(this.transport.nodeName(), this.transport.wrapAddress(this.channel.getLocalAddress()), this.action, error);
        stream.writeThrowable(tx);
        byte status = 0;
        status = TransportStatus.setResponse(status);
        status = TransportStatus.setError(status);
        final BytesReference bytes = stream.bytes();
        final ChannelBuffer buffer = bytes.toChannelBuffer();
        NettyHeader.writeHeader(buffer, this.requestId, status, this.version);
        this.channel.write((Object)buffer);
        final ChannelFuture future = this.channel.write((Object)buffer);
        final ChannelFutureListener onResponseSentListener = (ChannelFutureListener)new ChannelFutureListener() {
            public void operationComplete(final ChannelFuture future) throws Exception {
                NettyTransportChannel.this.transportServiceAdapter.onResponseSent(NettyTransportChannel.this.requestId, NettyTransportChannel.this.action, error);
            }
        };
        future.addListener(onResponseSentListener);
    }
    
    private void close() {
        if (!this.closed.compareAndSet(false, true)) {
            throw new IllegalStateException("Channel is already closed");
        }
        this.transport.inFlightRequestsBreaker().addWithoutBreaking(-this.reservedBytes);
    }
    
    @Override
    public long getRequestId() {
        return this.requestId;
    }
    
    @Override
    public String getChannelType() {
        return "netty";
    }
    
    public Channel getChannel() {
        return this.channel;
    }
}
