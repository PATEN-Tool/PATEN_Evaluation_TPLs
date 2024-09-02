// 
// Decompiled by Procyon v0.5.36
// 

package org.elasticsearch.transport.local;

import java.io.NotSerializableException;
import org.elasticsearch.transport.NotSerializableTransportException;
import java.io.OutputStream;
import org.elasticsearch.common.io.ThrowableObjectOutputStream;
import org.elasticsearch.transport.RemoteTransportException;
import org.elasticsearch.transport.support.TransportStatus;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.HandlesStreamOutput;
import org.elasticsearch.common.io.stream.BytesStreamOutput;
import java.io.IOException;
import org.elasticsearch.transport.TransportResponseOptions;
import org.elasticsearch.transport.TransportResponse;
import org.elasticsearch.Version;
import org.elasticsearch.transport.TransportServiceAdapter;
import org.elasticsearch.transport.TransportChannel;

public class LocalTransportChannel implements TransportChannel
{
    private final LocalTransport sourceTransport;
    private final TransportServiceAdapter sourceTransportServiceAdapter;
    private final LocalTransport targetTransport;
    private final String action;
    private final long requestId;
    private final Version version;
    
    public LocalTransportChannel(final LocalTransport sourceTransport, final TransportServiceAdapter sourceTransportServiceAdapter, final LocalTransport targetTransport, final String action, final long requestId, final Version version) {
        this.sourceTransport = sourceTransport;
        this.sourceTransportServiceAdapter = sourceTransportServiceAdapter;
        this.targetTransport = targetTransport;
        this.action = action;
        this.requestId = requestId;
        this.version = version;
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
        final BytesStreamOutput bStream = new BytesStreamOutput();
        final StreamOutput stream = new HandlesStreamOutput(bStream);
        stream.setVersion(this.version);
        stream.writeLong(this.requestId);
        byte status = 0;
        status = TransportStatus.setResponse(status);
        stream.writeByte(status);
        response.writeTo(stream);
        stream.close();
        final byte[] data = bStream.bytes().toBytes();
        this.targetTransport.workers().execute(new Runnable() {
            @Override
            public void run() {
                LocalTransportChannel.this.targetTransport.messageReceived(data, LocalTransportChannel.this.action, LocalTransportChannel.this.sourceTransport, LocalTransportChannel.this.version, null);
            }
        });
        this.sourceTransportServiceAdapter.onResponseSent(this.requestId, this.action, response, options);
    }
    
    @Override
    public void sendResponse(final Throwable error) throws IOException {
        final BytesStreamOutput stream = new BytesStreamOutput();
        try {
            this.writeResponseExceptionHeader(stream);
            final RemoteTransportException tx = new RemoteTransportException(this.targetTransport.nodeName(), this.targetTransport.boundAddress().boundAddress(), this.action, error);
            final ThrowableObjectOutputStream too = new ThrowableObjectOutputStream(stream);
            too.writeObject(tx);
            too.close();
        }
        catch (NotSerializableException e) {
            stream.reset();
            this.writeResponseExceptionHeader(stream);
            final RemoteTransportException tx2 = new RemoteTransportException(this.targetTransport.nodeName(), this.targetTransport.boundAddress().boundAddress(), this.action, new NotSerializableTransportException(error));
            final ThrowableObjectOutputStream too2 = new ThrowableObjectOutputStream(stream);
            too2.writeObject(tx2);
            too2.close();
        }
        final byte[] data = stream.bytes().toBytes();
        this.targetTransport.workers().execute(new Runnable() {
            @Override
            public void run() {
                LocalTransportChannel.this.targetTransport.messageReceived(data, LocalTransportChannel.this.action, LocalTransportChannel.this.sourceTransport, LocalTransportChannel.this.version, null);
            }
        });
        this.sourceTransportServiceAdapter.onResponseSent(this.requestId, this.action, error);
    }
    
    private void writeResponseExceptionHeader(final BytesStreamOutput stream) throws IOException {
        stream.writeLong(this.requestId);
        byte status = 0;
        status = TransportStatus.setResponse(status);
        status = TransportStatus.setError(status);
        stream.writeByte(status);
    }
}
