// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.coyote.http11.filters;

import org.apache.tomcat.util.buf.MessageBytes;
import org.apache.tomcat.util.http.MimeHeaders;
import java.io.EOFException;
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
    private Request request;
    
    public ChunkedInputFilter() {
        this.remaining = 0;
        this.pos = 0;
        this.lastValid = 0;
        this.buf = null;
        this.readChunk = new ByteChunk();
        this.endChunk = false;
        this.needCRLFParse = false;
    }
    
    @Override
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
    
    @Override
    public void setRequest(final Request request) {
        this.request = request;
    }
    
    @Override
    public long end() throws IOException {
        while (this.doRead(this.readChunk, null) >= 0) {}
        return this.lastValid - this.pos;
    }
    
    @Override
    public int available() {
        return this.lastValid - this.pos;
    }
    
    @Override
    public void setBuffer(final InputBuffer buffer) {
        this.buffer = buffer;
    }
    
    @Override
    public void recycle() {
        this.remaining = 0;
        this.pos = 0;
        this.lastValid = 0;
        this.endChunk = false;
    }
    
    @Override
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
                    if (HexUtils.getDec(this.buf[this.pos]) == -1) {
                        return false;
                    }
                    readDigit = true;
                    result *= 16;
                    result += HexUtils.getDec(this.buf[this.pos]);
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
    
    protected void parseEndChunk() throws IOException {
        while (this.parseHeader()) {}
    }
    
    private boolean parseHeader() throws IOException {
        final MimeHeaders headers = this.request.getMimeHeaders();
        byte chr = 0;
        while (this.pos < this.lastValid || this.readBytes() >= 0) {
            chr = this.buf[this.pos];
            if (chr != 13 && chr != 10) {
                int start = this.pos;
                boolean colon = false;
                MessageBytes headerValue = null;
                while (!colon) {
                    if (this.pos >= this.lastValid && this.readBytes() < 0) {
                        throw new EOFException("Unexpected end of stream whilst reading trailer headers for chunked request");
                    }
                    if (this.buf[this.pos] == 58) {
                        colon = true;
                        headerValue = headers.addValue(this.buf, start, this.pos - start);
                    }
                    chr = this.buf[this.pos];
                    if (chr >= 65 && chr <= 90) {
                        this.buf[this.pos] = (byte)(chr + 32);
                    }
                    ++this.pos;
                }
                start = this.pos;
                int realPos = this.pos;
                boolean eol = false;
                boolean validLine = true;
                while (validLine) {
                    boolean space = true;
                    while (space) {
                        if (this.pos >= this.lastValid && this.readBytes() < 0) {
                            throw new EOFException("Unexpected end of stream whilst reading trailer headers for chunked request");
                        }
                        if (this.buf[this.pos] == 32 || this.buf[this.pos] == 9) {
                            ++this.pos;
                        }
                        else {
                            space = false;
                        }
                    }
                    int lastSignificantChar = realPos;
                    while (!eol) {
                        if (this.pos >= this.lastValid && this.readBytes() < 0) {
                            throw new EOFException("Unexpected end of stream whilst reading trailer headers for chunked request");
                        }
                        if (this.buf[this.pos] != 13) {
                            if (this.buf[this.pos] == 10) {
                                eol = true;
                            }
                            else if (this.buf[this.pos] == 32) {
                                this.buf[realPos] = this.buf[this.pos];
                                ++realPos;
                            }
                            else {
                                this.buf[realPos] = this.buf[this.pos];
                                lastSignificantChar = ++realPos;
                            }
                        }
                        ++this.pos;
                    }
                    realPos = lastSignificantChar;
                    if (this.pos >= this.lastValid && this.readBytes() < 0) {
                        throw new EOFException("Unexpected end of stream whilst reading trailer headers for chunked request");
                    }
                    chr = this.buf[this.pos];
                    if (chr != 32 && chr != 9) {
                        validLine = false;
                    }
                    else {
                        eol = false;
                        this.buf[realPos] = chr;
                        ++realPos;
                    }
                }
                headerValue.setBytes(this.buf, start, realPos - start);
                return true;
            }
            if (chr == 10) {
                ++this.pos;
                return false;
            }
            ++this.pos;
        }
        throw new EOFException("Unexpected end of stream whilst reading trailer headers for chunked request");
    }
    
    static {
        (ENCODING = new ByteChunk()).setBytes("chunked".getBytes(), 0, "chunked".length());
    }
}
