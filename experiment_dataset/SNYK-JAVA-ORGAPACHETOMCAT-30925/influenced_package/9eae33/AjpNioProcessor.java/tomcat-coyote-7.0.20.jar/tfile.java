// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.coyote.ajp;

import org.apache.tomcat.util.buf.ByteChunk;
import org.apache.juli.logging.LogFactory;
import org.apache.coyote.Response;
import java.io.EOFException;
import java.nio.channels.Selector;
import java.nio.ByteBuffer;
import org.apache.tomcat.util.MutableInteger;
import org.apache.tomcat.util.net.SocketStatus;
import org.apache.coyote.ActionCode;
import org.apache.coyote.RequestInfo;
import java.io.InterruptedIOException;
import org.apache.tomcat.util.ExceptionUtils;
import java.io.IOException;
import org.apache.tomcat.util.net.SocketWrapper;
import org.apache.coyote.OutputBuffer;
import org.apache.tomcat.util.net.AbstractEndpoint;
import org.apache.tomcat.util.net.NioEndpoint;
import org.apache.tomcat.util.net.NioSelectorPool;
import org.apache.juli.logging.Log;
import org.apache.tomcat.util.net.NioChannel;

public class AjpNioProcessor extends AbstractAjpProcessor<NioChannel>
{
    private static final Log log;
    protected NioChannel socket;
    protected NioSelectorPool pool;
    
    @Override
    protected Log getLog() {
        return AjpNioProcessor.log;
    }
    
    public AjpNioProcessor(final int packetSize, final NioEndpoint endpoint) {
        super(packetSize, endpoint);
        this.response.setOutputBuffer(new SocketOutputBuffer());
        this.pool = endpoint.getSelectorPool();
    }
    
    @Override
    public AbstractEndpoint.Handler.SocketState process(final SocketWrapper<NioChannel> socket) throws IOException {
        final RequestInfo rp = this.request.getRequestProcessor();
        rp.setStage(1);
        this.socket = socket.getSocket();
        final long soTimeout = this.endpoint.getSoTimeout();
        final int keepAliveTimeout = this.endpoint.getKeepAliveTimeout();
        this.error = false;
        while (!this.error && !this.endpoint.isPaused()) {
            try {
                final int bytesRead = this.readMessage(this.requestHeaderMessage, false);
                if (bytesRead == 0) {
                    rp.setStage(7);
                    break;
                }
                if (keepAliveTimeout > 0) {
                    socket.setTimeout(soTimeout);
                }
                final int type = this.requestHeaderMessage.getByte();
                if (type == 10) {
                    try {
                        this.output(AjpNioProcessor.pongMessageArray, 0, AjpNioProcessor.pongMessageArray.length);
                    }
                    catch (IOException e) {
                        this.error = true;
                    }
                    this.recycle(false);
                    continue;
                }
                if (type != 2) {
                    if (AjpNioProcessor.log.isDebugEnabled()) {
                        AjpNioProcessor.log.debug((Object)("Unexpected message: " + type));
                    }
                    this.recycle(true);
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
                AjpNioProcessor.log.debug((Object)AjpNioProcessor.sm.getString("ajpprocessor.header.error"), t);
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
                    AjpNioProcessor.log.debug((Object)AjpNioProcessor.sm.getString("ajpprocessor.request.prepare"), t);
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
                    AjpNioProcessor.log.error((Object)AjpNioProcessor.sm.getString("ajpprocessor.request.process"), t);
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
            if (keepAliveTimeout > 0) {
                socket.setTimeout(keepAliveTimeout);
            }
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
                ((NioEndpoint)this.endpoint).processSocket(this.socket, SocketStatus.OPEN, false);
            }
        }
        else if (actionCode == ActionCode.ASYNC_SETTIMEOUT) {
            if (param == null) {
                return;
            }
            final long timeout = (long)param;
            final NioEndpoint.KeyAttachment ka = (NioEndpoint.KeyAttachment)this.socket.getAttachment(false);
            if (this.keepAliveTimeout > 0) {
                ka.setTimeout(timeout);
            }
        }
        else if (actionCode == ActionCode.ASYNC_DISPATCH && this.asyncStateMachine.asyncDispatch()) {
            ((NioEndpoint)this.endpoint).processSocket(this.socket, SocketStatus.OPEN, true);
        }
    }
    
    @Override
    protected void output(final byte[] src, final int offset, final int length) throws IOException {
        final ByteBuffer writeBuffer = this.socket.getBufHandler().getWriteBuffer();
        writeBuffer.put(src, offset, length);
        writeBuffer.flip();
        final NioEndpoint.KeyAttachment att = (NioEndpoint.KeyAttachment)this.socket.getAttachment(false);
        if (att == null) {
            throw new IOException("Key must be cancelled");
        }
        final long writeTimeout = att.getTimeout();
        Selector selector = null;
        try {
            selector = this.pool.get();
        }
        catch (IOException ex) {}
        try {
            this.pool.write(writeBuffer, this.socket, selector, writeTimeout, true, null);
        }
        finally {
            if (selector != null) {
                this.pool.put(selector);
            }
        }
        writeBuffer.clear();
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
            messageArray = AjpNioProcessor.endAndCloseMessageArray;
        }
        else {
            messageArray = AjpNioProcessor.endMessageArray;
        }
        this.output(messageArray, 0, messageArray.length);
    }
    
    protected int read(final byte[] buf, final int pos, final int n, final boolean blockFirstRead) throws IOException {
        int read = 0;
        int res = 0;
        boolean block = blockFirstRead;
        while (read < n) {
            res = this.readSocket(buf, read + pos, n, block);
            if (res > 0) {
                read += res;
                block = true;
            }
            else {
                if (res == 0 && !block) {
                    break;
                }
                throw new IOException(AjpNioProcessor.sm.getString("ajpprotocol.failedread"));
            }
        }
        return read;
    }
    
    private int readSocket(final byte[] buf, final int pos, final int n, final boolean block) throws IOException {
        int nRead = 0;
        this.socket.getBufHandler().getReadBuffer().clear();
        this.socket.getBufHandler().getReadBuffer().limit(n);
        if (block) {
            Selector selector = null;
            try {
                selector = this.pool.get();
            }
            catch (IOException ex) {}
            try {
                final NioEndpoint.KeyAttachment att = (NioEndpoint.KeyAttachment)this.socket.getAttachment(false);
                if (att == null) {
                    throw new IOException("Key must be cancelled.");
                }
                nRead = this.pool.read(this.socket.getBufHandler().getReadBuffer(), this.socket, selector, att.getTimeout());
            }
            catch (EOFException eof) {
                nRead = -1;
            }
            finally {
                if (selector != null) {
                    this.pool.put(selector);
                }
            }
        }
        else {
            nRead = this.socket.read(this.socket.getBufHandler().getReadBuffer());
        }
        if (nRead > 0) {
            this.socket.getBufHandler().getReadBuffer().flip();
            this.socket.getBufHandler().getReadBuffer().limit(nRead);
            this.socket.getBufHandler().getReadBuffer().get(buf, pos, nRead);
            return nRead;
        }
        if (nRead == -1) {
            throw new EOFException(AjpNioProcessor.sm.getString("iib.eof.error"));
        }
        return 0;
    }
    
    public boolean receive() throws IOException {
        this.first = false;
        this.bodyMessage.reset();
        this.readMessage(this.bodyMessage, true);
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
        this.output(this.getBodyMessageArray, 0, this.getBodyMessageArray.length);
        final boolean moreData = this.receive();
        if (!moreData) {
            this.endOfStream = true;
        }
        return moreData;
    }
    
    protected int readMessage(final AjpMessage message, final boolean blockFirstRead) throws IOException {
        final byte[] buf = message.getBuffer();
        final int headerLength = message.getHeaderLength();
        int bytesRead = this.read(buf, 0, headerLength, blockFirstRead);
        if (bytesRead == 0) {
            return 0;
        }
        final int messageLength = message.processHeader();
        if (messageLength < 0) {
            throw new IOException(AjpNioProcessor.sm.getString("ajpmessage.invalidLength", new Object[] { messageLength }));
        }
        if (messageLength == 0) {
            return bytesRead;
        }
        if (messageLength > buf.length) {
            throw new IllegalArgumentException(AjpNioProcessor.sm.getString("ajpprocessor.header.tooLong", new Object[] { messageLength, buf.length }));
        }
        bytesRead += this.read(buf, headerLength, messageLength, true);
        return bytesRead;
    }
    
    @Override
    protected void flush(final boolean explicit) throws IOException {
        if (explicit && !this.finished) {
            this.output(AjpNioProcessor.flushMessageArray, 0, AjpNioProcessor.flushMessageArray.length);
        }
    }
    
    static {
        log = LogFactory.getLog((Class)AjpNioProcessor.class);
    }
    
    protected class SocketOutputBuffer implements OutputBuffer
    {
        @Override
        public int doWrite(final ByteChunk chunk, final Response res) throws IOException {
            if (!AjpNioProcessor.this.response.isCommitted()) {
                try {
                    AjpNioProcessor.this.prepareResponse();
                }
                catch (IOException e) {
                    AjpNioProcessor.this.error = true;
                }
            }
            int len = chunk.getLength();
            final int chunkSize = 8184 + AjpNioProcessor.this.packetSize - 8192;
            int off = 0;
            while (len > 0) {
                int thisTime = len;
                if (thisTime > chunkSize) {
                    thisTime = chunkSize;
                }
                len -= thisTime;
                AjpNioProcessor.this.responseHeaderMessage.reset();
                AjpNioProcessor.this.responseHeaderMessage.appendByte(3);
                AjpNioProcessor.this.responseHeaderMessage.appendBytes(chunk.getBytes(), chunk.getOffset() + off, thisTime);
                AjpNioProcessor.this.responseHeaderMessage.end();
                AjpNioProcessor.this.output(AjpNioProcessor.this.responseHeaderMessage.getBuffer(), 0, AjpNioProcessor.this.responseHeaderMessage.getLen());
                off += thisTime;
            }
            final AjpNioProcessor this$0 = AjpNioProcessor.this;
            this$0.byteCount += chunk.getLength();
            return chunk.getLength();
        }
        
        @Override
        public long getBytesWritten() {
            return AjpNioProcessor.this.byteCount;
        }
    }
}
