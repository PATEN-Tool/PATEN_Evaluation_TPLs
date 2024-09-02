// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.coyote.http11.filters;

import org.apache.tomcat.util.buf.MessageBytes;
import org.apache.tomcat.util.http.MimeHeaders;
import java.io.EOFException;
import org.apache.tomcat.util.buf.HexUtils;
import java.io.IOException;
import org.apache.coyote.Constants;
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
    protected ByteChunk trailingHeaders;
    protected boolean needCRLFParse;
    private Request request;
    
    public ChunkedInputFilter() {
        this.remaining = 0;
        this.pos = 0;
        this.lastValid = 0;
        this.buf = null;
        this.readChunk = new ByteChunk();
        this.endChunk = false;
        this.trailingHeaders = new ByteChunk();
        if (Constants.MAX_TRAILER_SIZE > 0) {
            this.trailingHeaders.setLimit(Constants.MAX_TRAILER_SIZE);
        }
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
        this.request = request;
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
        this.trailingHeaders.recycle();
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
        while (this.parseHeader()) {}
        return true;
    }
    
    private boolean parseHeader() throws IOException {
        final MimeHeaders headers = this.request.getMimeHeaders();
        byte chr = 0;
        while (this.pos < this.lastValid || this.readBytes() >= 0) {
            chr = this.buf[this.pos];
            if (chr != 13 && chr != 10) {
                int start = this.trailingHeaders.getEnd();
                boolean colon = false;
                while (!colon) {
                    if (this.pos >= this.lastValid && this.readBytes() < 0) {
                        throw new EOFException("Unexpected end of stream whilst reading trailer headers for chunked request");
                    }
                    chr = this.buf[this.pos];
                    if (chr >= 65 && chr <= 90) {
                        chr += 32;
                    }
                    if (chr == 58) {
                        colon = true;
                    }
                    else {
                        this.trailingHeaders.append(chr);
                    }
                    ++this.pos;
                }
                final MessageBytes headerValue = headers.addValue(this.trailingHeaders.getBytes(), start, this.trailingHeaders.getEnd() - start);
                start = this.trailingHeaders.getEnd();
                boolean eol = false;
                boolean validLine = true;
                int lastSignificantChar = 0;
                while (validLine) {
                    boolean space = true;
                    while (space) {
                        if (this.pos >= this.lastValid && this.readBytes() < 0) {
                            throw new EOFException("Unexpected end of stream whilst reading trailer headers for chunked request");
                        }
                        chr = this.buf[this.pos];
                        if (chr == 32 || chr == 9) {
                            ++this.pos;
                        }
                        else {
                            space = false;
                        }
                    }
                    while (!eol) {
                        if (this.pos >= this.lastValid && this.readBytes() < 0) {
                            throw new EOFException("Unexpected end of stream whilst reading trailer headers for chunked request");
                        }
                        chr = this.buf[this.pos];
                        if (chr != 13) {
                            if (chr == 10) {
                                eol = true;
                            }
                            else if (chr == 32) {
                                this.trailingHeaders.append(chr);
                            }
                            else {
                                this.trailingHeaders.append(chr);
                                lastSignificantChar = this.trailingHeaders.getEnd();
                            }
                        }
                        ++this.pos;
                    }
                    if (this.pos >= this.lastValid && this.readBytes() < 0) {
                        throw new EOFException("Unexpected end of stream whilst reading trailer headers for chunked request");
                    }
                    chr = this.buf[this.pos];
                    if (chr != 32 && chr != 9) {
                        validLine = false;
                    }
                    else {
                        eol = false;
                        this.trailingHeaders.append(chr);
                    }
                }
                headerValue.setBytes(this.trailingHeaders.getBytes(), start, lastSignificantChar - start);
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
