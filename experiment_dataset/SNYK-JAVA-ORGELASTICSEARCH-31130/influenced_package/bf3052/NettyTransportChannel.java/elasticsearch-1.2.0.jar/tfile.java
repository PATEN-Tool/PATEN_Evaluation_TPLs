// 
// Decompiled by Procyon v0.5.36
// 

package org.elasticsearch.transport.netty;

import java.io.NotSerializableException;
import org.elasticsearch.transport.NotSerializableTransportException;
import java.io.OutputStream;
import org.elasticsearch.common.io.ThrowableObjectOutputStream;
import org.elasticsearch.transport.RemoteTransportException;
import org.elasticsearch.common.netty.channel.ChannelFuture;
import org.elasticsearch.common.netty.buffer.ChannelBuffer;
import org.elasticsearch.common.bytes.ReleasableBytesReference;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.lease.Releasables;
import org.elasticsearch.common.netty.channel.ChannelFutureListener;
import org.elasticsearch.common.lease.Releasable;
import org.elasticsearch.common.netty.ReleaseChannelFutureListener;
import org.elasticsearch.common.io.stream.HandlesStreamOutput;
import org.elasticsearch.common.compress.CompressorFactory;
import org.elasticsearch.common.io.stream.ReleasableBytesStreamOutput;
import org.elasticsearch.transport.support.TransportStatus;
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
        byte status = 0;
        status = TransportStatus.setResponse(status);
        final ReleasableBytesStreamOutput bStream = new ReleasableBytesStreamOutput(this.transport.bigArrays);
        boolean addedReleaseListener = false;
        try {
            bStream.skip(19);
            StreamOutput stream = bStream;
            if (options.compress()) {
                status = TransportStatus.setCompress(status);
                stream = CompressorFactory.defaultCompressor().streamOutput(stream);
            }
            stream = new HandlesStreamOutput(stream);
            stream.setVersion(this.version);
            response.writeTo(stream);
            stream.close();
            final ReleasableBytesReference bytes = bStream.bytes();
            final ChannelBuffer buffer = bytes.toChannelBuffer();
            NettyHeader.writeHeader(buffer, this.requestId, status, this.version);
            final ChannelFuture future = this.channel.write(buffer);
            final ReleaseChannelFutureListener listener = new ReleaseChannelFutureListener(bytes);
            future.addListener(listener);
            addedReleaseListener = true;
        }
        finally {
            if (!addedReleaseListener) {
                Releasables.close(bStream.bytes());
            }
        }
    }
    
    @Override
    public void sendResponse(final Throwable error) throws IOException {
        final ReleasableBytesStreamOutput stream = new ReleasableBytesStreamOutput(this.transport.bigArrays);
        boolean addedReleaseListener = false;
        try {
            try {
                stream.skip(19);
                final RemoteTransportException tx = new RemoteTransportException(this.transport.nodeName(), this.transport.wrapAddress(this.channel.getLocalAddress()), this.action, error);
                final ThrowableObjectOutputStream too = new ThrowableObjectOutputStream(stream);
                too.writeObject(tx);
                too.close();
            }
            catch (NotSerializableException e) {
                stream.reset();
                stream.skip(19);
                final RemoteTransportException tx2 = new RemoteTransportException(this.transport.nodeName(), this.transport.wrapAddress(this.channel.getLocalAddress()), this.action, new NotSerializableTransportException(error));
                final ThrowableObjectOutputStream too2 = new ThrowableObjectOutputStream(stream);
                too2.writeObject(tx2);
                too2.close();
            }
            byte status = 0;
            status = TransportStatus.setResponse(status);
            status = TransportStatus.setError(status);
            final ReleasableBytesReference bytes = stream.bytes();
            final ChannelBuffer buffer = bytes.toChannelBuffer();
            NettyHeader.writeHeader(buffer, this.requestId, status, this.version);
            final ChannelFuture future = this.channel.write(buffer);
            final ReleaseChannelFutureListener listener = new ReleaseChannelFutureListener(bytes);
            future.addListener(listener);
            addedReleaseListener = true;
        }
        finally {
            if (!addedReleaseListener) {
                Releasables.close(stream.bytes());
            }
        }
    }
}
