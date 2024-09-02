// 
// Decompiled by Procyon v0.5.36
// 

package org.elasticsearch.transport.netty;

import org.elasticsearch.common.io.stream.BytesStreamOutput;
import java.io.NotSerializableException;
import org.elasticsearch.transport.NotSerializableTransportException;
import java.io.OutputStream;
import org.elasticsearch.common.io.ThrowableObjectOutputStream;
import org.elasticsearch.transport.RemoteTransportException;
import org.elasticsearch.common.netty.channel.ChannelFuture;
import org.elasticsearch.common.netty.buffer.ChannelBuffer;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.netty.channel.ChannelFutureListener;
import org.elasticsearch.common.compress.CompressorFactory;
import org.elasticsearch.transport.support.TransportStatus;
import org.elasticsearch.common.io.stream.CachedStreamOutput;
import java.io.IOException;
import org.elasticsearch.transport.TransportResponseOptions;
import org.elasticsearch.transport.TransportResponse;
import org.elasticsearch.common.netty.channel.Channel;
import org.elasticsearch.Version;
import org.elasticsearch.transport.TransportChannel;

public class NettyTransportChannel implements TransportChannel
{
    private final NettyTransport transport;
    private final Version version;
    private final String action;
    private final Channel channel;
    private final long requestId;
    
    public NettyTransportChannel(final NettyTransport transport, final String action, final Channel channel, final long requestId, final Version version) {
        this.version = version;
        this.transport = transport;
        this.action = action;
        this.channel = channel;
        this.requestId = requestId;
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
    public void sendResponse(final TransportResponse response, final TransportResponseOptions options) throws IOException {
        if (this.transport.compress) {
            options.withCompress(true);
        }
        final CachedStreamOutput.Entry cachedEntry = CachedStreamOutput.popEntry();
        byte status = 0;
        status = TransportStatus.setResponse(status);
        if (options.compress()) {
            status = TransportStatus.setCompress(status);
            cachedEntry.bytes().skip(19);
            final StreamOutput stream = cachedEntry.handles(CompressorFactory.defaultCompressor());
            stream.setVersion(this.version);
            response.writeTo(stream);
            stream.close();
        }
        else {
            final StreamOutput stream = cachedEntry.handles();
            stream.setVersion(this.version);
            cachedEntry.bytes().skip(19);
            response.writeTo(stream);
            stream.close();
        }
        final ChannelBuffer buffer = cachedEntry.bytes().bytes().toChannelBuffer();
        NettyHeader.writeHeader(buffer, this.requestId, status, this.version);
        final ChannelFuture future = this.channel.write(buffer);
        future.addListener(new NettyTransport.CacheFutureListener(cachedEntry));
    }
    
    @Override
    public void sendResponse(final Throwable error) throws IOException {
        final CachedStreamOutput.Entry cachedEntry = CachedStreamOutput.popEntry();
        try {
            final BytesStreamOutput stream = cachedEntry.bytes();
            cachedEntry.bytes().skip(19);
            final RemoteTransportException tx = new RemoteTransportException(this.transport.nodeName(), this.transport.wrapAddress(this.channel.getLocalAddress()), this.action, error);
            final ThrowableObjectOutputStream too = new ThrowableObjectOutputStream(stream);
            too.writeObject(tx);
            too.close();
        }
        catch (NotSerializableException e) {
            cachedEntry.reset();
            final BytesStreamOutput stream = cachedEntry.bytes();
            cachedEntry.bytes().skip(19);
            final RemoteTransportException tx2 = new RemoteTransportException(this.transport.nodeName(), this.transport.wrapAddress(this.channel.getLocalAddress()), this.action, new NotSerializableTransportException(error));
            final ThrowableObjectOutputStream too2 = new ThrowableObjectOutputStream(stream);
            too2.writeObject(tx2);
            too2.close();
        }
        byte status = 0;
        status = TransportStatus.setResponse(status);
        status = TransportStatus.setError(status);
        final ChannelBuffer buffer = cachedEntry.bytes().bytes().toChannelBuffer();
        NettyHeader.writeHeader(buffer, this.requestId, status, this.version);
        final ChannelFuture future = this.channel.write(buffer);
        future.addListener(new NettyTransport.CacheFutureListener(cachedEntry));
    }
}
