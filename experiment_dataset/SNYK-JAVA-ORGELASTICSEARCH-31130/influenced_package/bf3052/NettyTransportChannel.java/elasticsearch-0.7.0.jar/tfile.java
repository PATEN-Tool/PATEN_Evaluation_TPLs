// 
// Decompiled by Procyon v0.5.36
// 

package org.elasticsearch.transport.netty;

import java.io.NotSerializableException;
import org.elasticsearch.transport.NotSerializableTransportException;
import java.io.OutputStream;
import org.elasticsearch.util.io.ThrowableObjectOutputStream;
import org.elasticsearch.transport.RemoteTransportException;
import java.io.IOException;
import org.elasticsearch.util.netty.buffer.ChannelBuffer;
import org.elasticsearch.util.io.stream.HandlesStreamOutput;
import org.elasticsearch.util.netty.buffer.ChannelBuffers;
import org.elasticsearch.util.io.stream.StreamOutput;
import org.elasticsearch.transport.Transport;
import org.elasticsearch.util.io.stream.BytesStreamOutput;
import org.elasticsearch.util.io.stream.Streamable;
import org.elasticsearch.util.netty.channel.Channel;
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
        final HandlesStreamOutput stream = BytesStreamOutput.Cached.cachedHandles();
        stream.writeBytes(NettyTransportChannel.LENGTH_PLACEHOLDER);
        stream.writeLong(this.requestId);
        byte status = 0;
        status = Transport.Helper.setResponse(status);
        stream.writeByte(status);
        message.writeTo(stream);
        final byte[] data = ((BytesStreamOutput)stream.wrappedOut()).copiedByteArray();
        final ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(data);
        buffer.setInt(0, buffer.writerIndex() - 4);
        this.channel.write(buffer);
    }
    
    @Override
    public void sendResponse(final Throwable error) throws IOException {
        BytesStreamOutput stream;
        try {
            stream = BytesStreamOutput.Cached.cached();
            this.writeResponseExceptionHeader(stream);
            final RemoteTransportException tx = new RemoteTransportException(this.transport.nodeName(), this.transport.wrapAddress(this.channel.getLocalAddress()), this.action, error);
            final ThrowableObjectOutputStream too = new ThrowableObjectOutputStream(stream);
            too.writeObject(tx);
            too.close();
        }
        catch (NotSerializableException e) {
            stream = BytesStreamOutput.Cached.cached();
            this.writeResponseExceptionHeader(stream);
            final RemoteTransportException tx2 = new RemoteTransportException(this.transport.nodeName(), this.transport.wrapAddress(this.channel.getLocalAddress()), this.action, new NotSerializableTransportException(error));
            final ThrowableObjectOutputStream too2 = new ThrowableObjectOutputStream(stream);
            too2.writeObject(tx2);
            too2.close();
        }
        final ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(stream.copiedByteArray());
        buffer.setInt(0, buffer.writerIndex() - 4);
        this.channel.write(buffer);
    }
    
    private void writeResponseExceptionHeader(final BytesStreamOutput stream) throws IOException {
        stream.writeBytes(NettyTransportChannel.LENGTH_PLACEHOLDER);
        stream.writeLong(this.requestId);
        byte status = 0;
        status = Transport.Helper.setResponse(status);
        status = Transport.Helper.setError(status);
        stream.writeByte(status);
    }
    
    static {
        LENGTH_PLACEHOLDER = new byte[4];
    }
}
