// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.coyote.http11;

import org.apache.tomcat.util.buf.CharChunk;
import org.apache.tomcat.util.buf.ByteChunk;
import org.apache.tomcat.util.buf.MessageBytes;
import java.io.IOException;
import org.apache.coyote.ActionCode;
import org.apache.tomcat.jni.Socket;
import org.apache.tomcat.util.http.HttpMessages;
import java.nio.ByteBuffer;
import org.apache.tomcat.util.http.MimeHeaders;
import org.apache.coyote.Response;
import org.apache.tomcat.util.res.StringManager;
import org.apache.coyote.OutputBuffer;

public class InternalAprOutputBuffer implements OutputBuffer
{
    protected static StringManager sm;
    protected Response response;
    protected MimeHeaders headers;
    protected boolean committed;
    protected boolean finished;
    protected byte[] buf;
    protected int pos;
    protected byte[] headerBuffer;
    protected long socket;
    protected OutputBuffer outputStreamOutputBuffer;
    protected OutputFilter[] filterLibrary;
    protected OutputFilter[] activeFilters;
    protected int lastActiveFilter;
    protected ByteBuffer bbuf;
    
    public InternalAprOutputBuffer(final Response response) {
        this(response, 49152);
    }
    
    public InternalAprOutputBuffer(final Response response, final int headerBufferSize) {
        this.bbuf = null;
        this.response = response;
        this.headers = response.getMimeHeaders();
        this.headerBuffer = new byte[headerBufferSize];
        this.buf = this.headerBuffer;
        this.bbuf = ByteBuffer.allocateDirect((headerBufferSize / 1500 + 1) * 1500);
        this.outputStreamOutputBuffer = (OutputBuffer)new SocketOutputBuffer();
        this.filterLibrary = new OutputFilter[0];
        this.activeFilters = new OutputFilter[0];
        this.lastActiveFilter = -1;
        this.committed = false;
        this.finished = false;
        HttpMessages.getMessage(200);
    }
    
    public void setSocket(final long socket) {
        Socket.setsbb(this.socket = socket, this.bbuf);
    }
    
    public long getSocket() {
        return this.socket;
    }
    
    public void setSocketBuffer(final int socketBufferSize) {
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
        this.flushBuffer();
    }
    
    public void reset() {
        if (this.committed) {
            throw new IllegalStateException();
        }
        this.response.recycle();
    }
    
    public void recycle() {
        this.response.recycle();
        this.bbuf.clear();
        this.socket = 0L;
        this.buf = this.headerBuffer;
        this.pos = 0;
        this.lastActiveFilter = -1;
        this.committed = false;
        this.finished = false;
    }
    
    public void nextRequest() {
        this.response.recycle();
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
        this.flushBuffer();
        this.finished = true;
    }
    
    public void sendAck() throws IOException {
        if (!this.committed && Socket.send(this.socket, Constants.ACK_BYTES, 0, Constants.ACK_BYTES.length) < 0) {
            throw new IOException(InternalAprOutputBuffer.sm.getString("iib.failedwrite"));
        }
    }
    
    public void sendStatus() {
        this.write(Constants.HTTP_11_BYTES);
        this.buf[this.pos++] = 32;
        final int status = this.response.getStatus();
        switch (status) {
            case 200: {
                this.write(Constants._200_BYTES);
                break;
            }
            case 400: {
                this.write(Constants._400_BYTES);
                break;
            }
            case 404: {
                this.write(Constants._404_BYTES);
                break;
            }
            default: {
                this.write(status);
                break;
            }
        }
        this.buf[this.pos++] = 32;
        final String message = this.response.getMessage();
        if (message == null) {
            this.write(HttpMessages.getMessage(status));
        }
        else {
            this.write(message);
        }
        this.buf[this.pos++] = 13;
        this.buf[this.pos++] = 10;
    }
    
    public void sendHeader(final MessageBytes name, final MessageBytes value) {
        this.write(name);
        this.buf[this.pos++] = 58;
        this.buf[this.pos++] = 32;
        this.write(value);
        this.buf[this.pos++] = 13;
        this.buf[this.pos++] = 10;
    }
    
    public void sendHeader(final ByteChunk name, final ByteChunk value) {
        this.write(name);
        this.buf[this.pos++] = 58;
        this.buf[this.pos++] = 32;
        this.write(value);
        this.buf[this.pos++] = 13;
        this.buf[this.pos++] = 10;
    }
    
    public void sendHeader(final String name, final String value) {
        this.write(name);
        this.buf[this.pos++] = 58;
        this.buf[this.pos++] = 32;
        this.write(value);
        this.buf[this.pos++] = 13;
        this.buf[this.pos++] = 10;
    }
    
    public void endHeaders() {
        this.buf[this.pos++] = 13;
        this.buf[this.pos++] = 10;
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
            this.bbuf.put(this.buf, 0, this.pos);
        }
    }
    
    protected void write(final MessageBytes mb) {
        if (mb.getType() == 2) {
            final ByteChunk bc = mb.getByteChunk();
            this.write(bc);
        }
        else if (mb.getType() == 3) {
            final CharChunk cc = mb.getCharChunk();
            this.write(cc);
        }
        else {
            this.write(mb.toString());
        }
    }
    
    protected void write(final ByteChunk bc) {
        System.arraycopy(bc.getBytes(), bc.getStart(), this.buf, this.pos, bc.getLength());
        this.pos += bc.getLength();
    }
    
    protected void write(final CharChunk cc) {
        final int start = cc.getStart();
        final int end = cc.getEnd();
        final char[] cbuf = cc.getBuffer();
        for (int i = start; i < end; ++i) {
            char c = cbuf[i];
            if (c <= '\u001f' && c != '\t') {
                c = ' ';
            }
            else if (c == '\u007f') {
                c = ' ';
            }
            this.buf[this.pos++] = (byte)c;
        }
    }
    
    public void write(final byte[] b) {
        System.arraycopy(b, 0, this.buf, this.pos, b.length);
        this.pos += b.length;
    }
    
    protected void write(final String s) {
        if (s == null) {
            return;
        }
        for (int len = s.length(), i = 0; i < len; ++i) {
            char c = s.charAt(i);
            if (c <= '\u001f' && c != '\t') {
                c = ' ';
            }
            else if (c == '\u007f') {
                c = ' ';
            }
            this.buf[this.pos++] = (byte)c;
        }
    }
    
    protected void write(final int i) {
        this.write(String.valueOf(i));
    }
    
    protected void flushBuffer() throws IOException {
        if (this.bbuf.position() > 0) {
            if (Socket.sendbb(this.socket, 0, this.bbuf.position()) < 0) {
                throw new IOException();
            }
            this.bbuf.clear();
        }
    }
    
    static {
        InternalAprOutputBuffer.sm = StringManager.getManager("org.apache.coyote.http11");
    }
    
    protected class SocketOutputBuffer implements OutputBuffer
    {
        public int doWrite(final ByteChunk chunk, final Response res) throws IOException {
            int len = chunk.getLength();
            int start = chunk.getStart();
            final byte[] b = chunk.getBuffer();
            while (len > 0) {
                int thisTime = len;
                if (InternalAprOutputBuffer.this.bbuf.position() == InternalAprOutputBuffer.this.bbuf.capacity()) {
                    InternalAprOutputBuffer.this.flushBuffer();
                }
                if (thisTime > InternalAprOutputBuffer.this.bbuf.capacity() - InternalAprOutputBuffer.this.bbuf.position()) {
                    thisTime = InternalAprOutputBuffer.this.bbuf.capacity() - InternalAprOutputBuffer.this.bbuf.position();
                }
                InternalAprOutputBuffer.this.bbuf.put(b, start, thisTime);
                len -= thisTime;
                start += thisTime;
            }
            return chunk.getLength();
        }
    }
}
