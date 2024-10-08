// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.coyote.ajp;

import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.net.SocketStatus;
import org.apache.coyote.ActionCode;
import org.apache.coyote.RequestInfo;
import java.io.InterruptedIOException;
import org.apache.tomcat.util.ExceptionUtils;
import java.io.IOException;
import org.apache.coyote.ErrorState;
import org.apache.tomcat.util.net.SocketWrapper;
import org.apache.coyote.OutputBuffer;
import org.apache.tomcat.util.net.AbstractEndpoint;
import org.apache.tomcat.util.net.JIoEndpoint;
import java.io.OutputStream;
import java.io.InputStream;
import org.apache.juli.logging.Log;
import java.net.Socket;

public class AjpProcessor extends AbstractAjpProcessor<Socket>
{
    private static final Log log;
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
        this.socketWrapper = (SocketWrapper<S>)socket;
        this.input = socket.getSocket().getInputStream();
        this.output = socket.getSocket().getOutputStream();
        int soTimeout = -1;
        if (this.keepAliveTimeout > 0) {
            soTimeout = socket.getSocket().getSoTimeout();
        }
        boolean cping = false;
        while (!this.getErrorState().isError() && !this.endpoint.isPaused()) {
            try {
                if (this.keepAliveTimeout > 0) {
                    socket.getSocket().setSoTimeout(this.keepAliveTimeout);
                }
                if (!this.readMessage(this.requestHeaderMessage)) {
                    break;
                }
                if (this.keepAliveTimeout > 0) {
                    socket.getSocket().setSoTimeout(soTimeout);
                }
                final int type = this.requestHeaderMessage.getByte();
                if (type == 10) {
                    if (this.endpoint.isPaused()) {
                        this.recycle(true);
                        break;
                    }
                    cping = true;
                    try {
                        this.output.write(AjpProcessor.pongMessageArray);
                    }
                    catch (IOException e) {
                        this.setErrorState(ErrorState.CLOSE_NOW, e);
                    }
                    continue;
                }
                else {
                    if (type != 2) {
                        if (AjpProcessor.log.isDebugEnabled()) {
                            AjpProcessor.log.debug((Object)("Unexpected message: " + type));
                        }
                        this.setErrorState(ErrorState.CLOSE_NOW, null);
                        break;
                    }
                    this.request.setStartTime(System.currentTimeMillis());
                }
            }
            catch (IOException e2) {
                this.setErrorState(ErrorState.CLOSE_NOW, e2);
                break;
            }
            catch (Throwable t) {
                ExceptionUtils.handleThrowable(t);
                AjpProcessor.log.debug((Object)AjpProcessor.sm.getString("ajpprocessor.header.error"), t);
                this.response.setStatus(400);
                this.setErrorState(ErrorState.CLOSE_CLEAN, t);
                this.getAdapter().log(this.request, this.response, 0L);
            }
            if (!this.getErrorState().isError()) {
                rp.setStage(2);
                try {
                    this.prepareRequest();
                }
                catch (Throwable t) {
                    ExceptionUtils.handleThrowable(t);
                    AjpProcessor.log.debug((Object)AjpProcessor.sm.getString("ajpprocessor.request.prepare"), t);
                    this.response.setStatus(500);
                    this.setErrorState(ErrorState.CLOSE_CLEAN, t);
                    this.getAdapter().log(this.request, this.response, 0L);
                }
            }
            if (!this.getErrorState().isError() && !cping && this.endpoint.isPaused()) {
                this.response.setStatus(503);
                this.setErrorState(ErrorState.CLOSE_CLEAN, null);
                this.getAdapter().log(this.request, this.response, 0L);
            }
            cping = false;
            if (!this.getErrorState().isError()) {
                try {
                    rp.setStage(3);
                    this.adapter.service(this.request, this.response);
                }
                catch (InterruptedIOException e3) {
                    this.setErrorState(ErrorState.CLOSE_NOW, e3);
                }
                catch (Throwable t) {
                    ExceptionUtils.handleThrowable(t);
                    AjpProcessor.log.error((Object)AjpProcessor.sm.getString("ajpprocessor.request.process"), t);
                    this.response.setStatus(500);
                    this.setErrorState(ErrorState.CLOSE_CLEAN, t);
                    this.getAdapter().log(this.request, this.response, 0L);
                }
            }
            if (this.isAsync() && !this.getErrorState().isError()) {
                break;
            }
            if (!this.finished && this.getErrorState().isIoAllowed()) {
                try {
                    this.finish();
                }
                catch (Throwable t) {
                    ExceptionUtils.handleThrowable(t);
                    this.setErrorState(ErrorState.CLOSE_NOW, t);
                }
            }
            if (this.getErrorState().isError()) {
                this.response.setStatus(500);
            }
            this.request.updateCounters();
            rp.setStage(6);
            this.recycle(false);
        }
        rp.setStage(7);
        if (this.isAsync() && !this.getErrorState().isError() && !this.endpoint.isPaused()) {
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
        switch (actionCode) {
            case ASYNC_COMPLETE: {
                if (this.asyncStateMachine.asyncComplete()) {
                    ((JIoEndpoint)this.endpoint).processSocketAsync((SocketWrapper<Socket>)this.socketWrapper, SocketStatus.OPEN_READ);
                    break;
                }
                break;
            }
            case ASYNC_SETTIMEOUT: {
                if (param == null) {
                    return;
                }
                final long timeout = (long)param;
                this.socketWrapper.setTimeout(timeout);
                break;
            }
            case ASYNC_DISPATCH: {
                if (this.asyncStateMachine.asyncDispatch()) {
                    ((JIoEndpoint)this.endpoint).processSocketAsync((SocketWrapper<Socket>)this.socketWrapper, SocketStatus.OPEN_READ);
                    break;
                }
                break;
            }
        }
    }
    
    @Override
    protected void resetTimeouts() {
    }
    
    @Override
    protected void output(final byte[] src, final int offset, final int length) throws IOException {
        this.output.write(src, offset, length);
    }
    
    protected boolean read(final byte[] buf, final int pos, final int n) throws IOException {
        for (int read = 0, res = 0; read < n; read += res) {
            res = this.input.read(buf, read + pos, n - read);
            if (res <= 0) {
                throw new IOException(AjpProcessor.sm.getString("ajpprocessor.failedread"));
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
        this.bodyMessage.getBodyBytes(this.bodyBytes);
        this.empty = false;
        return true;
    }
    
    protected boolean readMessage(final AjpMessage message) throws IOException {
        final byte[] buf = message.getBuffer();
        final int headerLength = message.getHeaderLength();
        this.read(buf, 0, headerLength);
        final int messageLength = message.processHeader(true);
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
    
    static {
        log = LogFactory.getLog((Class)AjpProcessor.class);
    }
}
