// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.thrift.transport;

import org.slf4j.LoggerFactory;
import org.apache.thrift.transport.layered.TFramedTransport;
import javax.security.sasl.SaslException;
import java.nio.charset.StandardCharsets;
import org.apache.thrift.EncodingUtils;
import org.apache.thrift.transport.sasl.NegotiationStatus;
import javax.security.sasl.SaslServer;
import javax.security.sasl.SaslClient;
import org.apache.thrift.TConfiguration;
import java.util.Objects;
import org.apache.thrift.TByteArrayOutputStream;
import org.slf4j.Logger;

abstract class TSaslTransport extends TEndpointTransport
{
    private static final Logger LOGGER;
    protected static final int DEFAULT_MAX_LENGTH = Integer.MAX_VALUE;
    protected static final int MECHANISM_NAME_BYTES = 1;
    protected static final int STATUS_BYTES = 1;
    protected static final int PAYLOAD_LENGTH_BYTES = 4;
    protected TTransport underlyingTransport;
    private SaslParticipant sasl;
    private boolean shouldWrap;
    private TMemoryInputTransport readBuffer;
    private final TByteArrayOutputStream writeBuffer;
    private final byte[] messageHeader;
    
    protected TSaslTransport(final TTransport underlyingTransport) throws TTransportException {
        super(Objects.isNull(underlyingTransport.getConfiguration()) ? new TConfiguration() : underlyingTransport.getConfiguration());
        this.shouldWrap = false;
        this.readBuffer = new TMemoryInputTransport();
        this.writeBuffer = new TByteArrayOutputStream(1024);
        this.messageHeader = new byte[5];
        this.underlyingTransport = underlyingTransport;
    }
    
    protected TSaslTransport(final SaslClient saslClient, final TTransport underlyingTransport) throws TTransportException {
        super(Objects.isNull(underlyingTransport.getConfiguration()) ? new TConfiguration() : underlyingTransport.getConfiguration());
        this.shouldWrap = false;
        this.readBuffer = new TMemoryInputTransport();
        this.writeBuffer = new TByteArrayOutputStream(1024);
        this.messageHeader = new byte[5];
        this.sasl = new SaslParticipant(saslClient);
        this.underlyingTransport = underlyingTransport;
    }
    
    protected void setSaslServer(final SaslServer saslServer) {
        this.sasl = new SaslParticipant(saslServer);
    }
    
    protected void sendSaslMessage(final NegotiationStatus status, byte[] payload) throws TTransportException {
        if (payload == null) {
            payload = new byte[0];
        }
        this.messageHeader[0] = status.getValue();
        EncodingUtils.encodeBigEndian(payload.length, this.messageHeader, 1);
        TSaslTransport.LOGGER.debug("{}: Writing message with status {} and payload length {}", new Object[] { this.getRole(), status, payload.length });
        this.underlyingTransport.write(this.messageHeader);
        this.underlyingTransport.write(payload);
        this.underlyingTransport.flush();
    }
    
    protected SaslResponse receiveSaslMessage() throws TTransportException {
        this.underlyingTransport.readAll(this.messageHeader, 0, this.messageHeader.length);
        final byte statusByte = this.messageHeader[0];
        final NegotiationStatus status = NegotiationStatus.byValue(statusByte);
        if (status == null) {
            throw this.sendAndThrowMessage(NegotiationStatus.ERROR, "Invalid status " + statusByte);
        }
        final int payloadBytes = EncodingUtils.decodeBigEndian(this.messageHeader, 1);
        if (payloadBytes < 0 || payloadBytes > this.getConfiguration().getMaxMessageSize()) {
            throw this.sendAndThrowMessage(NegotiationStatus.ERROR, "Invalid payload header length: " + payloadBytes);
        }
        final byte[] payload = new byte[payloadBytes];
        this.underlyingTransport.readAll(payload, 0, payload.length);
        if (status == NegotiationStatus.BAD || status == NegotiationStatus.ERROR) {
            final String remoteMessage = new String(payload, StandardCharsets.UTF_8);
            throw new TTransportException("Peer indicated failure: " + remoteMessage);
        }
        TSaslTransport.LOGGER.debug("{}: Received message with status {} and payload length {}", new Object[] { this.getRole(), status, payload.length });
        return new SaslResponse(status, payload);
    }
    
    protected TTransportException sendAndThrowMessage(final NegotiationStatus status, String message) throws TTransportException {
        try {
            this.sendSaslMessage(status, message.getBytes(StandardCharsets.UTF_8));
        }
        catch (Exception e) {
            TSaslTransport.LOGGER.warn("Could not send failure response", (Throwable)e);
            message = message + "\nAlso, could not send response: " + e.toString();
        }
        throw new TTransportException(message);
    }
    
    protected abstract void handleSaslStartMessage() throws TTransportException, SaslException;
    
    protected abstract SaslRole getRole();
    
    @Override
    public void open() throws TTransportException {
        boolean readSaslHeader = false;
        TSaslTransport.LOGGER.debug("opening transport {}", (Object)this);
        if (this.sasl != null && this.sasl.isComplete()) {
            throw new TTransportException("SASL transport already open");
        }
        if (!this.underlyingTransport.isOpen()) {
            this.underlyingTransport.open();
        }
        try {
            this.handleSaslStartMessage();
            readSaslHeader = true;
            TSaslTransport.LOGGER.debug("{}: Start message handled", (Object)this.getRole());
            SaslResponse message = null;
            while (!this.sasl.isComplete()) {
                message = this.receiveSaslMessage();
                if (message.status != NegotiationStatus.COMPLETE && message.status != NegotiationStatus.OK) {
                    throw new TTransportException("Expected COMPLETE or OK, got " + message.status);
                }
                final byte[] challenge = this.sasl.evaluateChallengeOrResponse(message.payload);
                if (message.status == NegotiationStatus.COMPLETE && this.getRole() == SaslRole.CLIENT) {
                    TSaslTransport.LOGGER.debug("{}: All done!", (Object)this.getRole());
                }
                else {
                    this.sendSaslMessage(this.sasl.isComplete() ? NegotiationStatus.COMPLETE : NegotiationStatus.OK, challenge);
                }
            }
            TSaslTransport.LOGGER.debug("{}: Main negotiation loop complete", (Object)this.getRole());
            if (this.getRole() == SaslRole.CLIENT && (message == null || message.status == NegotiationStatus.OK)) {
                TSaslTransport.LOGGER.debug("{}: SASL Client receiving last message", (Object)this.getRole());
                message = this.receiveSaslMessage();
                if (message.status != NegotiationStatus.COMPLETE) {
                    throw new TTransportException("Expected SASL COMPLETE, but got " + message.status);
                }
            }
        }
        catch (SaslException e) {
            try {
                TSaslTransport.LOGGER.error("SASL negotiation failure", (Throwable)e);
                throw this.sendAndThrowMessage(NegotiationStatus.BAD, e.getMessage());
            }
            finally {
                this.underlyingTransport.close();
            }
        }
        catch (TTransportException e2) {
            if (!readSaslHeader && e2.getType() == 4) {
                this.underlyingTransport.close();
                TSaslTransport.LOGGER.debug("No data or no sasl data in the stream during negotiation");
            }
            throw e2;
        }
        final String qop = (String)this.sasl.getNegotiatedProperty("javax.security.sasl.qop");
        if (qop != null && !qop.equalsIgnoreCase("auth")) {
            this.shouldWrap = true;
        }
    }
    
    public SaslClient getSaslClient() {
        return this.sasl.saslClient;
    }
    
    public TTransport getUnderlyingTransport() {
        return this.underlyingTransport;
    }
    
    public SaslServer getSaslServer() {
        return this.sasl.saslServer;
    }
    
    protected int readLength() throws TTransportException {
        final byte[] lenBuf = new byte[4];
        this.underlyingTransport.readAll(lenBuf, 0, lenBuf.length);
        return EncodingUtils.decodeBigEndian(lenBuf);
    }
    
    protected void writeLength(final int length) throws TTransportException {
        final byte[] lenBuf = new byte[4];
        TFramedTransport.encodeFrameSize(length, lenBuf);
        this.underlyingTransport.write(lenBuf);
    }
    
    @Override
    public void close() {
        this.underlyingTransport.close();
        try {
            this.sasl.dispose();
        }
        catch (SaslException e) {
            TSaslTransport.LOGGER.warn("Failed to dispose sasl participant.", (Throwable)e);
        }
    }
    
    @Override
    public boolean isOpen() {
        return this.underlyingTransport.isOpen() && this.sasl != null && this.sasl.isComplete();
    }
    
    @Override
    public int read(final byte[] buf, final int off, final int len) throws TTransportException {
        if (!this.isOpen()) {
            throw new TTransportException("SASL authentication not complete");
        }
        final int got = this.readBuffer.read(buf, off, len);
        if (got > 0) {
            return got;
        }
        try {
            this.readFrame();
        }
        catch (SaslException e) {
            throw new TTransportException(e);
        }
        catch (TTransportException transportException) {
            if (transportException.getType() == 4) {
                TSaslTransport.LOGGER.debug("No data or no sasl data in the stream during negotiation");
            }
            throw transportException;
        }
        return this.readBuffer.read(buf, off, len);
    }
    
    private void readFrame() throws TTransportException, SaslException {
        final int dataLength = this.readLength();
        if (dataLength < 0) {
            throw new TTransportException("Read a negative frame size (" + dataLength + ")!");
        }
        byte[] buff = new byte[dataLength];
        TSaslTransport.LOGGER.debug("{}: reading data length: {}", (Object)this.getRole(), (Object)dataLength);
        this.underlyingTransport.readAll(buff, 0, dataLength);
        if (this.shouldWrap) {
            buff = this.sasl.unwrap(buff, 0, buff.length);
            TSaslTransport.LOGGER.debug("data length after unwrap: {}", (Object)buff.length);
        }
        this.readBuffer.reset(buff);
    }
    
    @Override
    public void write(final byte[] buf, final int off, final int len) throws TTransportException {
        if (!this.isOpen()) {
            throw new TTransportException("SASL authentication not complete");
        }
        this.writeBuffer.write(buf, off, len);
    }
    
    @Override
    public void flush() throws TTransportException {
        byte[] buf = this.writeBuffer.get();
        int dataLength = this.writeBuffer.len();
        this.writeBuffer.reset();
        if (this.shouldWrap) {
            TSaslTransport.LOGGER.debug("data length before wrap: {}", (Object)dataLength);
            try {
                buf = this.sasl.wrap(buf, 0, dataLength);
            }
            catch (SaslException e) {
                throw new TTransportException(e);
            }
            dataLength = buf.length;
        }
        TSaslTransport.LOGGER.debug("writing data length: {}", (Object)dataLength);
        this.writeLength(dataLength);
        this.underlyingTransport.write(buf, 0, dataLength);
        this.underlyingTransport.flush();
    }
    
    static {
        LOGGER = LoggerFactory.getLogger((Class)TSaslTransport.class);
    }
    
    protected enum SaslRole
    {
        SERVER, 
        CLIENT;
    }
    
    protected static class SaslResponse
    {
        public NegotiationStatus status;
        public byte[] payload;
        
        public SaslResponse(final NegotiationStatus status, final byte[] payload) {
            this.status = status;
            this.payload = payload;
        }
    }
    
    private static class SaslParticipant
    {
        public SaslServer saslServer;
        public SaslClient saslClient;
        
        public SaslParticipant(final SaslServer saslServer) {
            this.saslServer = saslServer;
        }
        
        public SaslParticipant(final SaslClient saslClient) {
            this.saslClient = saslClient;
        }
        
        public byte[] evaluateChallengeOrResponse(final byte[] challengeOrResponse) throws SaslException {
            if (this.saslClient != null) {
                return this.saslClient.evaluateChallenge(challengeOrResponse);
            }
            return this.saslServer.evaluateResponse(challengeOrResponse);
        }
        
        public boolean isComplete() {
            if (this.saslClient != null) {
                return this.saslClient.isComplete();
            }
            return this.saslServer.isComplete();
        }
        
        public void dispose() throws SaslException {
            if (this.saslClient != null) {
                this.saslClient.dispose();
            }
            else {
                this.saslServer.dispose();
            }
        }
        
        public byte[] unwrap(final byte[] buf, final int off, final int len) throws SaslException {
            if (this.saslClient != null) {
                return this.saslClient.unwrap(buf, off, len);
            }
            return this.saslServer.unwrap(buf, off, len);
        }
        
        public byte[] wrap(final byte[] buf, final int off, final int len) throws SaslException {
            if (this.saslClient != null) {
                return this.saslClient.wrap(buf, off, len);
            }
            return this.saslServer.wrap(buf, off, len);
        }
        
        public Object getNegotiatedProperty(final String propName) {
            if (this.saslClient != null) {
                return this.saslClient.getNegotiatedProperty(propName);
            }
            return this.saslServer.getNegotiatedProperty(propName);
        }
    }
}
