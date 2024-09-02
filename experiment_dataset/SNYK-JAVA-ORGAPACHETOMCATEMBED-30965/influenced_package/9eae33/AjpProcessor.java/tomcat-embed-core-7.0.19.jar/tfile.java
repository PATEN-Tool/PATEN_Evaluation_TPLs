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
import org.apache.coyote.OutputBuffer;
import org.apache.tomcat.util.net.AbstractEndpoint;
import org.apache.tomcat.util.net.JIoEndpoint;
import java.io.OutputStream;
import java.io.InputStream;
import org.apache.tomcat.util.net.SocketWrapper;
import org.apache.juli.logging.Log;
import java.net.Socket;

public class AjpProcessor extends AbstractAjpProcessor<Socket>
{
    private static final Log log;
    protected SocketWrapper<Socket> socket;
    protected InputStream input;
    protected OutputStream output;
    
    @Override
    protected Log getLog() {
        return AjpProcessor.log;
    }
    
    public AjpProcessor(final int packetSize, final JIoEndpoint endpoint) {
        super(packetSize, endpoint);
        this.response.setOutputBuffer(new SocketOutputBuffer());
    }
    
    @Override
    public AbstractEndpoint.Handler.SocketState process(final SocketWrapper<Socket> socket) throws IOException {
        final RequestInfo rp = this.request.getRequestProcessor();
        rp.setStage(1);
        this.socket = socket;
        this.input = socket.getSocket().getInputStream();
        this.output = socket.getSocket().getOutputStream();
        int soTimeout = -1;
        if (this.keepAliveTimeout > 0) {
            soTimeout = socket.getSocket().getSoTimeout();
        }
        this.error = false;
        while (!this.error && !this.endpoint.isPaused()) {
            try {
                if (this.keepAliveTimeout > 0) {
                    socket.getSocket().setSoTimeout(this.keepAliveTimeout);
                }
                if (!this.readMessage(this.requestHeaderMessage)) {
                    rp.setStage(7);
                    break;
                }
                if (this.keepAliveTimeout > 0) {
                    socket.getSocket().setSoTimeout(soTimeout);
                }
                final int type = this.requestHeaderMessage.getByte();
                if (type == 10) {
                    try {
                        this.output.write(AjpProcessor.pongMessageArray);
                    }
                    catch (IOException e) {
                        this.error = true;
                    }
                    continue;
                }
                if (type != 2) {
                    if (!AjpProcessor.log.isDebugEnabled()) {
                        continue;
                    }
                    AjpProcessor.log.debug((Object)("Unexpected message: " + type));
                    continue;
                }
                this.request.setStartTime(System.currentTimeMillis());
            }
            catch (IOException e2) {
                this.error = true;
                break;
            }
            catch (Throwable t) {
                ExceptionUtils.handleThrowable(t);
                AjpProcessor.log.debug((Object)AjpProcessor.sm.getString("ajpprocessor.header.error"), t);
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
                    AjpProcessor.log.debug((Object)AjpProcessor.sm.getString("ajpprocessor.request.prepare"), t);
                    this.response.setStatus(400);
                    this.adapter.log(this.request, this.response, 0L);
                    this.error = true;
                }
            }
            if (this.endpoint.isPaused()) {
                this.response.setStatus(503);
                this.adapter.log(this.request, this.response, 0L);
                this.error = true;
            }
            if (!this.error) {
                try {
                    rp.setStage(3);
                    this.adapter.service(this.request, this.response);
                }
                catch (InterruptedIOException e3) {
                    this.error = true;
                }
                catch (Throwable t) {
                    ExceptionUtils.handleThrowable(t);
                    AjpProcessor.log.error((Object)AjpProcessor.sm.getString("ajpprocessor.request.process"), t);
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
        if (this.isAsync() && !this.error && !this.endpoint.isPaused()) {
            return AbstractEndpoint.Handler.SocketState.LONG;
        }
        this.input = null;
        this.output = null;
        return AbstractEndpoint.Handler.SocketState.CLOSED;
    }
    
    @Override
    public void recycle(final boolean socketClosing) {
        super.recycle(socketClosing);
        if (socketClosing) {
            this.input = null;
            this.output = null;
        }
    }
    
    @Override
    protected void actionInternal(final ActionCode actionCode, final Object param) {
        if (actionCode == ActionCode.ASYNC_COMPLETE) {
            if (this.asyncStateMachine.asyncComplete()) {
                ((JIoEndpoint)this.endpoint).processSocketAsync(this.socket, SocketStatus.OPEN);
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
            ((JIoEndpoint)this.endpoint).processSocketAsync(this.socket, SocketStatus.OPEN);
        }
    }
    
    @Override
    protected void output(final byte[] src, final int offset, final int length) throws IOException {
        this.output.write(src, offset, length);
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
        this.output.write(AjpProcessor.endMessageArray);
    }
    
    protected boolean read(final byte[] buf, final int pos, final int n) throws IOException {
        for (int read = 0, res = 0; read < n; read += res) {
            res = this.input.read(buf, read + pos, n - read);
            if (res <= 0) {
                throw new IOException(AjpProcessor.sm.getString("ajpprotocol.failedread"));
            }
        }
        return true;
    }
    
    public boolean receive() throws IOException {
        this.first = false;
        this.bodyMessage.reset();
        if (!this.readMessage(this.bodyMessage)) {
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
        this.output.write(this.getBodyMessageArray);
        final boolean moreData = this.receive();
        if (!moreData) {
            this.endOfStream = true;
        }
        return moreData;
    }
    
    protected boolean readMessage(final AjpMessage message) throws IOException {
        final byte[] buf = message.getBuffer();
        final int headerLength = message.getHeaderLength();
        this.read(buf, 0, headerLength);
        final int messageLength = message.processHeader();
        if (messageLength < 0) {
            return false;
        }
        if (messageLength == 0) {
            return true;
        }
        if (messageLength > buf.length) {
            throw new IllegalArgumentException(AjpProcessor.sm.getString("ajpprocessor.header.tooLong", messageLength, buf.length));
        }
        this.read(buf, headerLength, messageLength);
        return true;
    }
    
    @Override
    protected void flush(final boolean explicit) throws IOException {
        if (explicit && !this.finished) {
            this.output.write(AjpProcessor.flushMessageArray);
        }
    }
    
    static {
        log = LogFactory.getLog((Class)AjpProcessor.class);
    }
    
    protected class SocketOutputBuffer implements OutputBuffer
    {
        @Override
        public int doWrite(final ByteChunk chunk, final Response res) throws IOException {
            if (!AjpProcessor.this.response.isCommitted()) {
                try {
                    AjpProcessor.this.prepareResponse();
                }
                catch (IOException e) {
                    AjpProcessor.this.error = true;
                }
            }
            int len = chunk.getLength();
            final int chunkSize = 8184 + AjpProcessor.this.packetSize - 8192;
            int off = 0;
            while (len > 0) {
                int thisTime = len;
                if (thisTime > chunkSize) {
                    thisTime = chunkSize;
                }
                len -= thisTime;
                AjpProcessor.this.responseHeaderMessage.reset();
                AjpProcessor.this.responseHeaderMessage.appendByte(3);
                AjpProcessor.this.responseHeaderMessage.appendBytes(chunk.getBytes(), chunk.getOffset() + off, thisTime);
                AjpProcessor.this.responseHeaderMessage.end();
                AjpProcessor.this.output.write(AjpProcessor.this.responseHeaderMessage.getBuffer(), 0, AjpProcessor.this.responseHeaderMessage.getLen());
                off += thisTime;
            }
            final AjpProcessor this$0 = AjpProcessor.this;
            this$0.byteCount += chunk.getLength();
            return chunk.getLength();
        }
        
        @Override
        public long getBytesWritten() {
            return AjpProcessor.this.byteCount;
        }
    }
}
