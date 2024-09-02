// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.coyote.ajp;

import org.apache.tomcat.util.buf.ByteChunk;
import org.apache.juli.logging.LogFactory;
import org.apache.coyote.ActionCode;
import java.util.concurrent.Executor;
import org.apache.tomcat.util.net.SocketStatus;
import org.apache.coyote.RequestInfo;
import java.io.InterruptedIOException;
import org.apache.tomcat.util.ExceptionUtils;
import java.io.IOException;
import org.apache.tomcat.jni.Socket;
import org.apache.tomcat.util.net.AbstractEndpoint;
import org.apache.tomcat.util.http.HttpMessages;
import org.apache.tomcat.util.buf.HexUtils;
import org.apache.coyote.OutputBuffer;
import org.apache.coyote.ActionHook;
import org.apache.coyote.Response;
import org.apache.coyote.InputBuffer;
import org.apache.coyote.Request;
import org.apache.tomcat.util.net.AprEndpoint;
import java.nio.ByteBuffer;
import org.apache.tomcat.util.net.SocketWrapper;
import org.apache.juli.logging.Log;

public class AjpAprProcessor extends AbstractAjpProcessor
{
    private static final Log log;
    protected SocketWrapper<Long> socket;
    protected ByteBuffer inputBuffer;
    protected ByteBuffer outputBuffer;
    protected final ByteBuffer getBodyMessageBuffer;
    protected static final ByteBuffer pongMessageBuffer;
    protected static final byte[] endMessageArray;
    protected static final ByteBuffer flushMessageBuffer;
    
    @Override
    protected Log getLog() {
        return AjpAprProcessor.log;
    }
    
    public AjpAprProcessor(final int packetSize, final AprEndpoint endpoint) {
        this.inputBuffer = null;
        this.outputBuffer = null;
        this.endpoint = endpoint;
        (this.request = new Request()).setInputBuffer(new SocketInputBuffer());
        (this.response = new Response()).setHook(this);
        this.response.setOutputBuffer(new SocketOutputBuffer());
        this.request.setResponse(this.response);
        this.packetSize = packetSize;
        this.requestHeaderMessage = new AjpMessage(packetSize);
        this.responseHeaderMessage = new AjpMessage(packetSize);
        this.bodyMessage = new AjpMessage(packetSize);
        final AjpMessage getBodyMessage = new AjpMessage(16);
        getBodyMessage.reset();
        getBodyMessage.appendByte(6);
        getBodyMessage.appendInt(8186 + packetSize - 8192);
        getBodyMessage.end();
        (this.getBodyMessageBuffer = ByteBuffer.allocateDirect(getBodyMessage.getLen())).put(getBodyMessage.getBuffer(), 0, getBodyMessage.getLen());
        (this.inputBuffer = ByteBuffer.allocateDirect(packetSize * 2)).limit(0);
        this.outputBuffer = ByteBuffer.allocateDirect(packetSize * 2);
        HexUtils.load();
        HttpMessages.getMessage(200);
    }
    
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
                    if (Socket.sendb(socketRef, AjpAprProcessor.pongMessageBuffer, 0, AjpAprProcessor.pongMessageBuffer.position()) >= 0) {
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
            this.recycle();
        }
        if (!this.error && !this.endpoint.isPaused() && !this.isAsync()) {
            ((AprEndpoint)this.endpoint).getPoller().add(socketRef);
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
    
    public AbstractEndpoint.Handler.SocketState asyncDispatch(final SocketWrapper<Long> socket, final SocketStatus status) {
        this.socket = socket;
        final RequestInfo rp = this.request.getRequestProcessor();
        try {
            rp.setStage(3);
            this.error = !this.adapter.asyncDispatch(this.request, this.response, status);
        }
        catch (InterruptedIOException e) {
            this.error = true;
        }
        catch (Throwable t) {
            ExceptionUtils.handleThrowable(t);
            AjpAprProcessor.log.error((Object)AjpAprProcessor.sm.getString("http11processor.request.process"), t);
            this.response.setStatus(500);
            this.adapter.log(this.request, this.response, 0L);
            this.error = true;
        }
        rp.setStage(7);
        if (this.error) {
            this.response.setStatus(500);
        }
        if (this.isAsync()) {
            if (this.error) {
                this.request.updateCounters();
                return AbstractEndpoint.Handler.SocketState.CLOSED;
            }
            return AbstractEndpoint.Handler.SocketState.LONG;
        }
        else {
            this.request.updateCounters();
            if (this.error) {
                return AbstractEndpoint.Handler.SocketState.CLOSED;
            }
            return AbstractEndpoint.Handler.SocketState.OPEN;
        }
    }
    
    @Override
    public Executor getExecutor() {
        return this.endpoint.getExecutor();
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
        if (this.outputBuffer.position() + AjpAprProcessor.endMessageArray.length > this.outputBuffer.capacity()) {
            this.flush(false);
        }
        this.outputBuffer.put(AjpAprProcessor.endMessageArray);
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
        Socket.sendb(this.socket.getSocket(), this.getBodyMessageBuffer, 0, this.getBodyMessageBuffer.position());
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
        this.read(messageLength);
        this.inputBuffer.get(message.getBuffer(), headerLength, messageLength);
        return true;
    }
    
    @Override
    public void recycle() {
        super.recycle();
        this.inputBuffer.clear();
        this.inputBuffer.limit(0);
        this.outputBuffer.clear();
    }
    
    @Override
    protected void flush(final boolean explicit) throws IOException {
        final long socketRef = this.socket.getSocket();
        if (this.outputBuffer.position() > 0) {
            if (Socket.sendbb(socketRef, 0, this.outputBuffer.position()) < 0) {
                throw new IOException(AjpAprProcessor.sm.getString("ajpprocessor.failedsend"));
            }
            this.outputBuffer.clear();
        }
        if (explicit && !this.finished && Socket.sendb(socketRef, AjpAprProcessor.flushMessageBuffer, 0, AjpAprProcessor.flushMessageBuffer.position()) < 0) {
            throw new IOException(AjpAprProcessor.sm.getString("ajpprocessor.failedflush"));
        }
    }
    
    static {
        log = LogFactory.getLog((Class)AjpAprProcessor.class);
        final AjpMessage pongMessage = new AjpMessage(16);
        pongMessage.reset();
        pongMessage.appendByte(9);
        pongMessage.end();
        (pongMessageBuffer = ByteBuffer.allocateDirect(pongMessage.getLen())).put(pongMessage.getBuffer(), 0, pongMessage.getLen());
        final AjpMessage endMessage = new AjpMessage(16);
        endMessage.reset();
        endMessage.appendByte(5);
        endMessage.appendByte(1);
        endMessage.end();
        endMessageArray = new byte[endMessage.getLen()];
        System.arraycopy(endMessage.getBuffer(), 0, AjpAprProcessor.endMessageArray, 0, endMessage.getLen());
        final AjpMessage flushMessage = new AjpMessage(16);
        flushMessage.reset();
        flushMessage.appendByte(3);
        flushMessage.appendInt(0);
        flushMessage.appendByte(0);
        flushMessage.end();
        (flushMessageBuffer = ByteBuffer.allocateDirect(flushMessage.getLen())).put(flushMessage.getBuffer(), 0, flushMessage.getLen());
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
