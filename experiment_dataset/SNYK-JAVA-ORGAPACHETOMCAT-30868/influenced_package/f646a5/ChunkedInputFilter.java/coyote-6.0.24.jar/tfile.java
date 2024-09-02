// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.coyote.http11.filters;

import org.apache.tomcat.util.buf.HexUtils;
import java.io.IOException;
import org.apache.coyote.Request;
import org.apache.coyote.InputBuffer;
import org.apache.tomcat.util.buf.ByteChunk;
import org.apache.coyote.http11.InputFilter;

public class ChunkedInputFilter implements InputFilter
{
    protected static final String ENCODING_NAME = "chunked";
    protected static final ByteChunk ENCODING;
    protected InputBuffer buffer;
    protected int remaining;
    protected int pos;
    protected int lastValid;
    protected byte[] buf;
    protected ByteChunk readChunk;
    protected boolean endChunk;
    protected boolean needCRLFParse;
    
    public ChunkedInputFilter() {
        this.remaining = 0;
        this.pos = 0;
        this.lastValid = 0;
        this.buf = null;
        this.readChunk = new ByteChunk();
        this.endChunk = false;
        this.needCRLFParse = false;
    }
    
    public int doRead(final ByteChunk chunk, final Request req) throws IOException {
        if (this.endChunk) {
            return -1;
        }
        if (this.needCRLFParse) {
            this.needCRLFParse = false;
            this.parseCRLF();
        }
        if (this.remaining <= 0) {
            if (!this.parseChunkHeader()) {
                throw new IOException("Invalid chunk header");
            }
            if (this.endChunk) {
                this.parseEndChunk();
                return -1;
            }
        }
        int result = 0;
        if (this.pos >= this.lastValid) {
            this.readBytes();
        }
        if (this.remaining > this.lastValid - this.pos) {
            result = this.lastValid - this.pos;
            this.remaining -= result;
            chunk.setBytes(this.buf, this.pos, result);
            this.pos = this.lastValid;
        }
        else {
            result = this.remaining;
            chunk.setBytes(this.buf, this.pos, this.remaining);
            this.pos += this.remaining;
            this.remaining = 0;
            if (this.pos + 1 >= this.lastValid) {
                this.needCRLFParse = true;
            }
            else {
                this.parseCRLF();
            }
        }
        return result;
    }
    
    public void setRequest(final Request request) {
    }
    
    public long end() throws IOException {
        while (this.doRead(this.readChunk, null) >= 0) {}
        return this.lastValid - this.pos;
    }
    
    public int available() {
        return this.lastValid - this.pos;
    }
    
    public void setBuffer(final InputBuffer buffer) {
        this.buffer = buffer;
    }
    
    public void recycle() {
        this.remaining = 0;
        this.pos = 0;
        this.lastValid = 0;
        this.endChunk = false;
    }
    
    public ByteChunk getEncodingName() {
        return ChunkedInputFilter.ENCODING;
    }
    
    protected int readBytes() throws IOException {
        final int nRead = this.buffer.doRead(this.readChunk, null);
        this.pos = this.readChunk.getStart();
        this.lastValid = this.pos + nRead;
        this.buf = this.readChunk.getBytes();
        return nRead;
    }
    
    protected boolean parseChunkHeader() throws IOException {
        int result = 0;
        boolean eol = false;
        boolean readDigit = false;
        boolean trailer = false;
        while (!eol) {
            if (this.pos >= this.lastValid && this.readBytes() <= 0) {
                return false;
            }
            if (this.buf[this.pos] != 13) {
                if (this.buf[this.pos] == 10) {
                    eol = true;
                }
                else if (this.buf[this.pos] == 59) {
                    trailer = true;
                }
                else if (!trailer) {
                    if (HexUtils.DEC[this.buf[this.pos]] == -1) {
                        return false;
                    }
                    readDigit = true;
                    result *= 16;
                    result += HexUtils.DEC[this.buf[this.pos]];
                }
            }
            ++this.pos;
        }
        if (!readDigit) {
            return false;
        }
        if (result == 0) {
            this.endChunk = true;
        }
        this.remaining = result;
        return this.remaining >= 0;
    }
    
    protected boolean parseCRLF() throws IOException {
        boolean eol = false;
        boolean crfound = false;
        while (!eol) {
            if (this.pos >= this.lastValid && this.readBytes() <= 0) {
                throw new IOException("Invalid CRLF");
            }
            if (this.buf[this.pos] == 13) {
                if (crfound) {
                    throw new IOException("Invalid CRLF, two CR characters encountered.");
                }
                crfound = true;
            }
            else {
                if (this.buf[this.pos] != 10) {
                    throw new IOException("Invalid CRLF");
                }
                if (!crfound) {
                    throw new IOException("Invalid CRLF, no CR character encountered.");
                }
                eol = true;
            }
            ++this.pos;
        }
        return true;
    }
    
    protected boolean parseEndChunk() throws IOException {
        return this.parseCRLF();
    }
    
    static {
        (ENCODING = new ByteChunk()).setBytes("chunked".getBytes(), 0, "chunked".length());
    }
}
