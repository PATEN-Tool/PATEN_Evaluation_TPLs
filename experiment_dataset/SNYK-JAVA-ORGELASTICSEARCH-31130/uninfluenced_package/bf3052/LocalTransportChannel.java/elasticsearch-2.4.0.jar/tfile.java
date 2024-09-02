// 
// Decompiled by Procyon v0.5.36
// 

package org.elasticsearch.transport.local;

import org.elasticsearch.transport.RemoteTransportException;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.transport.support.TransportStatus;
import org.elasticsearch.common.io.stream.BytesStreamOutput;
import java.io.IOException;
import org.elasticsearch.transport.TransportResponseOptions;
import org.elasticsearch.transport.TransportResponse;
import java.util.concurrent.atomic.AtomicBoolean;
import org.elasticsearch.Version;
import org.elasticsearch.transport.TransportServiceAdapter;
import org.elasticsearch.transport.TransportChannel;

public class LocalTransportChannel implements TransportChannel
{
    private static final String LOCAL_TRANSPORT_PROFILE = "default";
    private final LocalTransport sourceTransport;
    private final TransportServiceAdapter sourceTransportServiceAdapter;
    private final LocalTransport targetTransport;
    private final String action;
    private final long requestId;
    private final Version version;
    private final long reservedBytes;
    private final AtomicBoolean closed;
    
    public LocalTransportChannel(final LocalTransport sourceTransport, final TransportServiceAdapter sourceTransportServiceAdapter, final LocalTransport targetTransport, final String action, final long requestId, final Version version, final long reservedBytes) {
        this.closed = new AtomicBoolean();
        this.sourceTransport = sourceTransport;
        this.sourceTransportServiceAdapter = sourceTransportServiceAdapter;
        this.targetTransport = targetTransport;
        this.action = action;
        this.requestId = requestId;
        this.version = version;
        this.reservedBytes = reservedBytes;
    }
    
    @Override
    public String action() {
        return this.action;
    }
    
    @Override
    public String getProfileName() {
        return "default";
    }
    
    @Override
    public void sendResponse(final TransportResponse response) throws IOException {
        this.sendResponse(response, TransportResponseOptions.EMPTY);
    }
    
    @Override
    public void sendResponse(final TransportResponse response, final TransportResponseOptions options) throws IOException {
        try (final BytesStreamOutput stream = new BytesStreamOutput()) {
            stream.setVersion(this.version);
            stream.writeLong(this.requestId);
            byte status = 0;
            status = TransportStatus.setResponse(status);
            stream.writeByte(status);
            response.writeTo(stream);
            this.sendResponseData(stream.bytes().toBytes());
            this.sourceTransportServiceAdapter.onResponseSent(this.requestId, this.action, response, options);
        }
    }
    
    @Override
    public void sendResponse(final Throwable error) throws IOException {
        final BytesStreamOutput stream = new BytesStreamOutput();
        this.writeResponseExceptionHeader(stream);
        final RemoteTransportException tx = new RemoteTransportException(this.targetTransport.nodeName(), this.targetTransport.boundAddress().boundAddresses()[0], this.action, error);
        stream.writeThrowable(tx);
        this.sendResponseData(stream.bytes().toBytes());
        this.sourceTransportServiceAdapter.onResponseSent(this.requestId, this.action, error);
    }
    
    private void sendResponseData(final byte[] data) {
        this.close();
        this.targetTransport.workers().execute(new Runnable() {
            @Override
            public void run() {
                LocalTransportChannel.this.targetTransport.messageReceived(data, LocalTransportChannel.this.action, LocalTransportChannel.this.sourceTransport, LocalTransportChannel.this.version, null);
            }
        });
    }
    
    private void close() {
        if (!this.closed.compareAndSet(false, true)) {
            throw new IllegalStateException("Channel is already closed");
        }
        this.sourceTransport.inFlightRequestsBreaker().addWithoutBreaking(-this.reservedBytes);
    }
    
    @Override
    public long getRequestId() {
        return this.requestId;
    }
    
    @Override
    public String getChannelType() {
        return "local";
    }
    
    private void writeResponseExceptionHeader(final BytesStreamOutput stream) throws IOException {
        stream.writeLong(this.requestId);
        byte status = 0;
        status = TransportStatus.setResponse(status);
        status = TransportStatus.setError(status);
        stream.writeByte(status);
    }
}
