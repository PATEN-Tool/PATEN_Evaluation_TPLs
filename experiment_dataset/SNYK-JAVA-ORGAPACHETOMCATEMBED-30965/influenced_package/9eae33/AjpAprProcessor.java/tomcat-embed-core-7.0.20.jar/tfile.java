// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.coyote.ajp;

import org.apache.tomcat.util.buf.ByteChunk;
import org.apache.juli.logging.LogFactory;
import org.apache.coyote.Response;
import org.apache.tomcat.util.net.SocketStatus;
import org.apache.coyote.ActionCode;
import org.apache.coyote.RequestInfo;
import java.io.InterruptedIOException;
import org.apache.tomcat.util.ExceptionUtils;
import java.io.IOException;
import org.apache.tomcat.jni.Socket;
import org.apache.coyote.OutputBuffer;
import org.apache.tomcat.util.net.AbstractEndpoint;
import org.apache.tomcat.util.net.AprEndpoint;
import java.nio.ByteBuffer;
import org.apache.tomcat.util.net.SocketWrapper;
import org.apache.juli.logging.Log;

public class AjpAprProcessor extends AbstractAjpProcessor<Long>
{
    private static final Log log;
    protected SocketWrapper<Long> socket;
    protected ByteBuffer inputBuffer;
    protected ByteBuffer outputBuffer;
    
    @Override
    protected Log getLog() {
        return AjpAprProcessor.log;
    }
    
    public AjpAprProcessor(final int packetSize, final AprEndpoint endpoint) {
        super(packetSize, endpoint);
        this.inputBuffer = null;
        this.outputBuffer = null;
        this.response.setOutputBuffer(new SocketOutputBuffer());
        (this.inputBuffer = ByteBuffer.allocateDirect(packetSize * 2)).limit(0);
        this.outputBuffer = ByteBuffer.allocateDirect(packetSize * 2);
    }
    
    @Override
    public AbstractEndpoint.Handler.SocketState process(final SocketWrapper<Long> socket) throws IOException {
        final RequestInfo rp = this.request.getRequestProcessor();
        rp.setStage(1);
        this.socket = socket;
        final long socketRef = socket.getSocket();
        Socket.setrbb(socketRef, this.inputBuffer);
        Socket.setsbb(socketRef, this.outputBuffer);
        this.error = false;
        boolean keptAlive = false;
        while (!this.error && !this.endpoint.isPaused()) {
            try {
                if (!this.readMessage(this.requestHeaderMessage, true, keptAlive)) {
                    rp.setStage(7);
                    break;
                }
                final int type = this.requestHeaderMessage.getByte();
                if (type == 10) {
                    if (Socket.send(socketRef, AjpAprProcessor.pongMessageArray, 0, AjpAprProcessor.pongMessageArray.length) >= 0) {
                        continue;
                    }
                    this.error = true;
                    continue;
                }
                if (type != 2) {
                    if (!AjpAprProcessor.log.isDebugEnabled()) {
                        continue;
                    }
                    AjpAprProcessor.log.debug((Object)("Unexpected message: " + type));
                    continue;
                }
                keptAlive = true;
                this.request.setStartTime(System.currentTimeMillis());
            }
            catch (IOException e) {
                this.error = true;
                break;
            }
            catch (Throwable t) {
                ExceptionUtils.handleThrowable(t);
                AjpAprProcessor.log.debug((Object)AjpAprProcessor.sm.getString("ajpprocessor.header.error"), t);
                this.response.setStatus(400);
                this.adapter.log(this.request, this.response, 0L);
                this.error = true;
            }
            if (!this.error) {
                rp.setStage(2);
                try {
                    this.prepareRequest();
                }
                catch (Throwable t) {
                    ExceptionUtils.handleThrowable(t);
                    AjpAprProcessor.log.debug((Object)AjpAprProcessor.sm.getString("ajpprocessor.request.prepare"), t);
                    this.response.setStatus(400);
                    this.adapter.log(this.request, this.response, 0L);
                    this.error = true;
                }
            }
            if (!this.error) {
                try {
                    rp.setStage(3);
                    this.adapter.service(this.request, this.response);
                }
                catch (InterruptedIOException e2) {
                    this.error = true;
                }
                catch (Throwable t) {
                    ExceptionUtils.handleThrowable(t);
                    AjpAprProcessor.log.error((Object)AjpAprProcessor.sm.getString("ajpprocessor.request.process"), t);
                    this.response.setStatus(500);
                    this.adapter.log(this.request, this.response, 0L);
                    this.error = true;
                }
            }
            if (this.isAsync() && !this.error) {
                break;
            }
            if (!this.finished) {
                try {
                    this.finish();
                }
                catch (Throwable t) {
                    ExceptionUtils.handleThrowable(t);
                    this.error = true;
                }
            }
            if (this.error) {
                this.response.setStatus(500);
            }
            this.request.updateCounters();
            rp.setStage(6);
            this.recycle(false);
        }
        rp.setStage(7);
        if (this.error || this.endpoint.isPaused()) {
            return AbstractEndpoint.Handler.SocketState.CLOSED;
        }
        if (this.isAsync()) {
            return AbstractEndpoint.Handler.SocketState.LONG;
        }
        return AbstractEndpoint.Handler.SocketState.OPEN;
    }
    
    @Override
    protected void actionInternal(final ActionCode actionCode, final Object param) {
        if (actionCode == ActionCode.ASYNC_COMPLETE) {
            if (this.asyncStateMachine.asyncComplete()) {
                ((AprEndpoint)this.endpoint).processSocketAsync(this.socket, SocketStatus.OPEN);
            }
        }
        else if (actionCode == ActionCode.ASYNC_SETTIMEOUT) {
            if (param == null) {
                return;
            }
            final long timeout = (long)param;
            this.socket.setTimeout(timeout);
        }
        else if (actionCode == ActionCode.ASYNC_DISPATCH && this.asyncStateMachine.asyncDispatch()) {
            ((AprEndpoint)this.endpoint).processSocketAsync(this.socket, SocketStatus.OPEN);
        }
    }
    
    @Override
    protected void output(final byte[] src, final int offset, final int length) throws IOException {
        this.outputBuffer.put(src, offset, length);
    }
    
    @Override
    protected void finish() throws IOException {
        if (!this.response.isCommitted()) {
            try {
                this.prepareResponse();
            }
            catch (IOException e) {
                this.error = true;
            }
        }
        if (this.finished) {
            return;
        }
        this.finished = true;
        byte[] messageArray;
        if (this.error) {
            messageArray = AjpAprProcessor.endAndCloseMessageArray;
        }
        else {
            messageArray = AjpAprProcessor.endMessageArray;
        }
        if (this.outputBuffer.position() + messageArray.length > this.outputBuffer.capacity()) {
            this.flush(false);
        }
        this.outputBuffer.put(messageArray);
        this.flush(false);
    }
    
    protected boolean read(final int n) throws IOException {
        if (this.inputBuffer.capacity() - this.inputBuffer.limit() <= n - this.inputBuffer.remaining()) {
            this.inputBuffer.compact();
            this.inputBuffer.limit(this.inputBuffer.position());
            this.inputBuffer.position(0);
        }
        while (this.inputBuffer.remaining() < n) {
            final int nRead = Socket.recvbb(this.socket.getSocket(), this.inputBuffer.limit(), this.inputBuffer.capacity() - this.inputBuffer.limit());
            if (nRead <= 0) {
                throw new IOException(AjpAprProcessor.sm.getString("ajpprotocol.failedread"));
            }
            this.inputBuffer.limit(this.inputBuffer.limit() + nRead);
        }
        return true;
    }
    
    protected boolean readt(final int n, final boolean useAvailableData) throws IOException {
        if (useAvailableData && this.inputBuffer.remaining() == 0) {
            return false;
        }
        if (this.inputBuffer.capacity() - this.inputBuffer.limit() <= n - this.inputBuffer.remaining()) {
            this.inputBuffer.compact();
            this.inputBuffer.limit(this.inputBuffer.position());
            this.inputBuffer.position(0);
        }
        while (this.inputBuffer.remaining() < n) {
            final int nRead = Socket.recvbb(this.socket.getSocket(), this.inputBuffer.limit(), this.inputBuffer.capacity() - this.inputBuffer.limit());
            if (nRead > 0) {
                this.inputBuffer.limit(this.inputBuffer.limit() + nRead);
            }
            else {
                if (-nRead == 120005 || -nRead == 120001) {
                    return false;
                }
                throw new IOException(AjpAprProcessor.sm.getString("ajpprotocol.failedread"));
            }
        }
        return true;
    }
    
    public boolean receive() throws IOException {
        this.first = false;
        this.bodyMessage.reset();
        if (!this.readMessage(this.bodyMessage, false, false)) {
            return false;
        }
        if (this.bodyMessage.getLen() == 0) {
            return false;
        }
        final int blen = this.bodyMessage.peekInt();
        if (blen == 0) {
            return false;
        }
        this.bodyMessage.getBytes(this.bodyBytes);
        this.empty = false;
        return true;
    }
    
    @Override
    protected boolean refillReadBuffer() throws IOException {
        if (this.replay) {
            this.endOfStream = true;
        }
        if (this.endOfStream) {
            return false;
        }
        Socket.send(this.socket.getSocket(), this.getBodyMessageArray, 0, this.getBodyMessageArray.length);
        final boolean moreData = this.receive();
        if (!moreData) {
            this.endOfStream = true;
        }
        return moreData;
    }
    
    protected boolean readMessage(final AjpMessage message, final boolean first, final boolean useAvailableData) throws IOException {
        final int headerLength = message.getHeaderLength();
        if (first) {
            if (!this.readt(headerLength, useAvailableData)) {
                return false;
            }
        }
        else {
            this.read(headerLength);
        }
        this.inputBuffer.get(message.getBuffer(), 0, headerLength);
        final int messageLength = message.processHeader();
        if (messageLength < 0) {
            return false;
        }
        if (messageLength == 0) {
            return true;
        }
        if (messageLength > message.getBuffer().length) {
            throw new IllegalArgumentException(AjpAprProcessor.sm.getString("ajpprocessor.header.tooLong", messageLength, message.getBuffer().length));
        }
        this.read(messageLength);
        this.inputBuffer.get(message.getBuffer(), headerLength, messageLength);
        return true;
    }
    
    @Override
    public void recycle(final boolean socketClosing) {
        super.recycle(socketClosing);
        this.inputBuffer.clear();
        this.inputBuffer.limit(0);
        this.outputBuffer.clear();
    }
    
    @Override
    protected void flush(final boolean explicit) throws IOException {
        final long socketRef = this.socket.getSocket();
        if (this.outputBuffer.position() > 0) {
            if (socketRef != 0L && Socket.sendbb(socketRef, 0, this.outputBuffer.position()) < 0) {
                throw new IOException(AjpAprProcessor.sm.getString("ajpprocessor.failedsend"));
            }
            this.outputBuffer.clear();
        }
        if (explicit && !this.finished && socketRef != 0L && Socket.send(socketRef, AjpAprProcessor.flushMessageArray, 0, AjpAprProcessor.flushMessageArray.length) < 0) {
            throw new IOException(AjpAprProcessor.sm.getString("ajpprocessor.failedflush"));
        }
    }
    
    static {
        log = LogFactory.getLog((Class)AjpAprProcessor.class);
    }
    
    protected class SocketOutputBuffer implements OutputBuffer
    {
        @Override
        public int doWrite(final ByteChunk chunk, final Response res) throws IOException {
            if (!AjpAprProcessor.this.response.isCommitted()) {
                try {
                    AjpAprProcessor.this.prepareResponse();
                }
                catch (IOException e) {
                    AjpAprProcessor.this.error = true;
                }
            }
            int len = chunk.getLength();
            final int chunkSize = 8184 + AjpAprProcessor.this.packetSize - 8192;
            int off = 0;
            while (len > 0) {
                int thisTime = len;
                if (thisTime > chunkSize) {
                    thisTime = chunkSize;
                }
                len -= thisTime;
                if (AjpAprProcessor.this.outputBuffer.position() + thisTime + 4 + 4 > AjpAprProcessor.this.outputBuffer.capacity()) {
                    AjpAprProcessor.this.flush(false);
                }
                AjpAprProcessor.this.outputBuffer.put((byte)65);
                AjpAprProcessor.this.outputBuffer.put((byte)66);
                AjpAprProcessor.this.outputBuffer.putShort((short)(thisTime + 4));
                AjpAprProcessor.this.outputBuffer.put((byte)3);
                AjpAprProcessor.this.outputBuffer.putShort((short)thisTime);
                AjpAprProcessor.this.outputBuffer.put(chunk.getBytes(), chunk.getOffset() + off, thisTime);
                AjpAprProcessor.this.outputBuffer.put((byte)0);
                off += thisTime;
            }
            final AjpAprProcessor this$0 = AjpAprProcessor.this;
            this$0.byteCount += chunk.getLength();
            return chunk.getLength();
        }
        
        @Override
        public long getBytesWritten() {
            return AjpAprProcessor.this.byteCount;
        }
    }
}
