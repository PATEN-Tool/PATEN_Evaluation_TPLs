// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.coyote.http11;

import org.apache.tomcat.util.buf.MessageBytes;
import org.apache.tomcat.util.http.HttpMessages;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.io.IOException;
import org.apache.coyote.ActionCode;
import java.io.OutputStream;
import org.apache.tomcat.util.http.MimeHeaders;
import org.apache.coyote.Response;
import org.apache.tomcat.util.res.StringManager;
import org.apache.tomcat.util.buf.ByteChunk;
import org.apache.coyote.OutputBuffer;

public class InternalOutputBuffer implements OutputBuffer, ByteChunk.ByteOutputChannel
{
    protected static StringManager sm;
    protected Response response;
    protected MimeHeaders headers;
    protected boolean committed;
    protected boolean finished;
    protected byte[] buf;
    protected int pos;
    protected byte[] headerBuffer;
    protected OutputStream outputStream;
    protected OutputBuffer outputStreamOutputBuffer;
    protected OutputFilter[] filterLibrary;
    protected OutputFilter[] activeFilters;
    protected int lastActiveFilter;
    protected ByteChunk socketBuffer;
    protected boolean useSocketBuffer;
    
    public InternalOutputBuffer(final Response response) {
        this(response, 49152);
    }
    
    public InternalOutputBuffer(final Response response, final int headerBufferSize) {
        this.useSocketBuffer = false;
        this.response = response;
        this.headers = response.getMimeHeaders();
        this.headerBuffer = new byte[headerBufferSize];
        this.buf = this.headerBuffer;
        this.outputStreamOutputBuffer = (OutputBuffer)new OutputStreamOutputBuffer();
        this.filterLibrary = new OutputFilter[0];
        this.activeFilters = new OutputFilter[0];
        this.lastActiveFilter = -1;
        (this.socketBuffer = new ByteChunk()).setByteOutputChannel((ByteChunk.ByteOutputChannel)this);
        this.committed = false;
        this.finished = false;
    }
    
    public void setOutputStream(final OutputStream outputStream) {
        this.outputStream = outputStream;
    }
    
    public OutputStream getOutputStream() {
        return this.outputStream;
    }
    
    public void setSocketBuffer(final int socketBufferSize) {
        if (socketBufferSize > 500) {
            this.useSocketBuffer = true;
            this.socketBuffer.allocate(socketBufferSize, socketBufferSize);
        }
        else {
            this.useSocketBuffer = false;
        }
    }
    
    public void addFilter(final OutputFilter filter) {
        final OutputFilter[] newFilterLibrary = new OutputFilter[this.filterLibrary.length + 1];
        for (int i = 0; i < this.filterLibrary.length; ++i) {
            newFilterLibrary[i] = this.filterLibrary[i];
        }
        newFilterLibrary[this.filterLibrary.length] = filter;
        this.filterLibrary = newFilterLibrary;
        this.activeFilters = new OutputFilter[this.filterLibrary.length];
    }
    
    public OutputFilter[] getFilters() {
        return this.filterLibrary;
    }
    
    public void clearFilters() {
        this.filterLibrary = new OutputFilter[0];
        this.lastActiveFilter = -1;
    }
    
    public void addActiveFilter(final OutputFilter filter) {
        if (this.lastActiveFilter == -1) {
            filter.setBuffer(this.outputStreamOutputBuffer);
        }
        else {
            for (int i = 0; i <= this.lastActiveFilter; ++i) {
                if (this.activeFilters[i] == filter) {
                    return;
                }
            }
            filter.setBuffer((OutputBuffer)this.activeFilters[this.lastActiveFilter]);
        }
        (this.activeFilters[++this.lastActiveFilter] = filter).setResponse(this.response);
    }
    
    public void flush() throws IOException {
        if (!this.committed) {
            this.response.action(ActionCode.ACTION_COMMIT, (Object)null);
        }
        if (this.useSocketBuffer) {
            this.socketBuffer.flushBuffer();
        }
    }
    
    public void reset() {
        if (this.committed) {
            throw new IllegalStateException();
        }
        this.response.recycle();
    }
    
    public void recycle() {
        this.response.recycle();
        this.socketBuffer.recycle();
        this.outputStream = null;
        this.buf = this.headerBuffer;
        this.pos = 0;
        this.lastActiveFilter = -1;
        this.committed = false;
        this.finished = false;
    }
    
    public void nextRequest() {
        this.response.recycle();
        this.socketBuffer.recycle();
        this.buf = this.headerBuffer;
        for (int i = 0; i <= this.lastActiveFilter; ++i) {
            this.activeFilters[i].recycle();
        }
        this.pos = 0;
        this.lastActiveFilter = -1;
        this.committed = false;
        this.finished = false;
    }
    
    public void endRequest() throws IOException {
        if (!this.committed) {
            this.response.action(ActionCode.ACTION_COMMIT, (Object)null);
        }
        if (this.finished) {
            return;
        }
        if (this.lastActiveFilter != -1) {
            this.activeFilters[this.lastActiveFilter].end();
        }
        if (this.useSocketBuffer) {
            this.socketBuffer.flushBuffer();
        }
        this.finished = true;
    }
    
    public void sendAck() throws IOException {
        if (!this.committed) {
            this.outputStream.write(Constants.ACK_BYTES);
        }
    }
    
    public void sendStatus() {
        this.write("HTTP/1.1 ");
        final int status = this.response.getStatus();
        switch (status) {
            case 200: {
                this.write("200");
                break;
            }
            case 400: {
                this.write("400");
                break;
            }
            case 404: {
                this.write("404");
                break;
            }
            default: {
                this.write(status);
                break;
            }
        }
        this.write(" ");
        final String message = this.response.getMessage();
        if (message == null) {
            this.write(this.getMessage(status));
        }
        else {
            this.write(message);
        }
        if (System.getSecurityManager() != null) {
            AccessController.doPrivileged((PrivilegedAction<Object>)new PrivilegedAction() {
                public Object run() {
                    InternalOutputBuffer.this.write(Constants.CRLF_BYTES);
                    return null;
                }
            });
        }
        else {
            this.write(Constants.CRLF_BYTES);
        }
    }
    
    private String getMessage(final int message) {
        if (System.getSecurityManager() != null) {
            return AccessController.doPrivileged((PrivilegedAction<String>)new PrivilegedAction() {
                public Object run() {
                    return HttpMessages.getMessage(message);
                }
            });
        }
        return HttpMessages.getMessage(message);
    }
    
    public void sendHeader(final MessageBytes name, final MessageBytes value) {
        this.write(name);
        this.write(": ");
        this.write(value);
        this.write(Constants.CRLF_BYTES);
    }
    
    public void sendHeader(final ByteChunk name, final ByteChunk value) {
        this.write(name);
        this.write(": ");
        this.write(value);
        this.write(Constants.CRLF_BYTES);
    }
    
    public void sendHeader(final String name, final String value) {
        this.write(name);
        this.write(": ");
        this.write(value);
        this.write(Constants.CRLF_BYTES);
    }
    
    public void endHeaders() {
        this.write(Constants.CRLF_BYTES);
    }
    
    public int doWrite(final ByteChunk chunk, final Response res) throws IOException {
        if (!this.committed) {
            this.response.action(ActionCode.ACTION_COMMIT, (Object)null);
        }
        if (this.lastActiveFilter == -1) {
            return this.outputStreamOutputBuffer.doWrite(chunk, res);
        }
        return this.activeFilters[this.lastActiveFilter].doWrite(chunk, res);
    }
    
    protected void commit() throws IOException {
        this.committed = true;
        this.response.setCommitted(true);
        if (this.pos > 0) {
            if (this.useSocketBuffer) {
                this.socketBuffer.append(this.buf, 0, this.pos);
            }
            else {
                this.outputStream.write(this.buf, 0, this.pos);
            }
        }
    }
    
    protected void write(final MessageBytes mb) {
        mb.toBytes();
        if (mb.getType() == 2) {
            final ByteChunk bc = mb.getByteChunk();
            this.write(bc);
        }
        else {
            this.write(mb.toString());
        }
    }
    
    protected void write(final ByteChunk bc) {
        System.arraycopy(bc.getBytes(), bc.getStart(), this.buf, this.pos, bc.getLength());
        this.pos += bc.getLength();
    }
    
    protected void write(final byte[] b) {
        System.arraycopy(b, 0, this.buf, this.pos, b.length);
        this.pos += b.length;
    }
    
    protected void write(final String s) {
        if (s == null) {
            return;
        }
        for (int len = s.length(), i = 0; i < len; ++i) {
            char c = s.charAt(i);
            if ((c & '\uff00') != 0x0) {}
            if (c != '\t') {
                if (c >= '\0' && c <= '\u001f') {
                    c = ' ';
                }
                if (c == '\u007f') {
                    c = ' ';
                }
            }
            this.buf[this.pos++] = (byte)c;
        }
    }
    
    protected void write(final int i) {
        this.write(String.valueOf(i));
    }
    
    public void realWriteBytes(final byte[] cbuf, final int off, final int len) throws IOException {
        if (len > 0) {
            this.outputStream.write(cbuf, off, len);
        }
    }
    
    static {
        InternalOutputBuffer.sm = StringManager.getManager("org.apache.coyote.http11");
    }
    
    protected class OutputStreamOutputBuffer implements OutputBuffer
    {
        public int doWrite(final ByteChunk chunk, final Response res) throws IOException {
            if (InternalOutputBuffer.this.useSocketBuffer) {
                InternalOutputBuffer.this.socketBuffer.append(chunk.getBuffer(), chunk.getStart(), chunk.getLength());
            }
            else {
                InternalOutputBuffer.this.outputStream.write(chunk.getBuffer(), chunk.getStart(), chunk.getLength());
            }
            return chunk.getLength();
        }
    }
}
