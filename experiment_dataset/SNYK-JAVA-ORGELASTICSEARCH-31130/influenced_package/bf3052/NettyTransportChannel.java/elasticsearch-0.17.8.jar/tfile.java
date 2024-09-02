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
import org.elasticsearch.common.netty.channel.ChannelFutureListener;
import org.elasticsearch.common.netty.buffer.ChannelBuffers;
import org.elasticsearch.transport.support.TransportStreams;
import org.elasticsearch.common.io.stream.CachedStreamOutput;
import java.io.IOException;
import org.elasticsearch.transport.TransportResponseOptions;
import org.elasticsearch.common.io.stream.Streamable;
import org.elasticsearch.common.netty.channel.Channel;
import org.elasticsearch.transport.TransportChannel;

public class NettyTransportChannel implements TransportChannel
{
    private static final byte[] LENGTH_PLACEHOLDER;
    private final NettyTransport transport;
    private final String action;
    private final Channel channel;
    private final long requestId;
    
    public NettyTransportChannel(final NettyTransport transport, final String action, final Channel channel, final long requestId) {
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
    public void sendResponse(final Streamable message) throws IOException {
        this.sendResponse(message, TransportResponseOptions.EMPTY);
    }
    
    @Override
    public void sendResponse(final Streamable message, final TransportResponseOptions options) throws IOException {
        if (this.transport.compress) {
            options.withCompress(true);
        }
        final CachedStreamOutput.Entry cachedEntry = CachedStreamOutput.popEntry();
        TransportStreams.buildResponse(cachedEntry, this.requestId, message, options);
        final ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(cachedEntry.bytes().unsafeByteArray(), 0, cachedEntry.bytes().size());
        final ChannelFuture future = this.channel.write(buffer);
        future.addListener(new NettyTransport.CacheFutureListener(cachedEntry));
    }
    
    @Override
    public void sendResponse(final Throwable error) throws IOException {
        final CachedStreamOutput.Entry cachedEntry = CachedStreamOutput.popEntry();
        BytesStreamOutput stream;
        try {
            stream = cachedEntry.cachedBytes();
            this.writeResponseExceptionHeader(stream);
            final RemoteTransportException tx = new RemoteTransportException(this.transport.nodeName(), this.transport.wrapAddress(this.channel.getLocalAddress()), this.action, error);
            final ThrowableObjectOutputStream too = new ThrowableObjectOutputStream(stream);
            too.writeObject(tx);
            too.close();
        }
        catch (NotSerializableException e) {
            stream = cachedEntry.cachedBytes();
            this.writeResponseExceptionHeader(stream);
            final RemoteTransportException tx2 = new RemoteTransportException(this.transport.nodeName(), this.transport.wrapAddress(this.channel.getLocalAddress()), this.action, new NotSerializableTransportException(error));
            final ThrowableObjectOutputStream too2 = new ThrowableObjectOutputStream(stream);
            too2.writeObject(tx2);
            too2.close();
        }
        final ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(stream.unsafeByteArray(), 0, stream.size());
        buffer.setInt(0, buffer.writerIndex() - 4);
        final ChannelFuture future = this.channel.write(buffer);
        future.addListener(new NettyTransport.CacheFutureListener(cachedEntry));
    }
    
    private void writeResponseExceptionHeader(final BytesStreamOutput stream) throws IOException {
        stream.writeBytes(NettyTransportChannel.LENGTH_PLACEHOLDER);
        stream.writeLong(this.requestId);
        byte status = 0;
        status = TransportStreams.statusSetResponse(status);
        status = TransportStreams.statusSetError(status);
        stream.writeByte(status);
    }
    
    static {
        LENGTH_PLACEHOLDER = new byte[4];
    }
}
