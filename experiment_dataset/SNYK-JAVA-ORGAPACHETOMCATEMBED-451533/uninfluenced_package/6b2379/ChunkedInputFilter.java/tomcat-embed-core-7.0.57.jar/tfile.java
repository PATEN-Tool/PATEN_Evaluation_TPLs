// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.coyote.http11.filters;

import java.nio.charset.Charset;
import java.io.EOFException;
import org.apache.tomcat.util.buf.MessageBytes;
import org.apache.tomcat.util.http.MimeHeaders;
import org.apache.tomcat.util.buf.HexUtils;
import java.io.IOException;
import org.apache.coyote.Request;
import org.apache.coyote.InputBuffer;
import org.apache.tomcat.util.buf.ByteChunk;
import org.apache.tomcat.util.res.StringManager;
import org.apache.coyote.http11.InputFilter;

public class ChunkedInputFilter implements InputFilter
{
    private static final StringManager sm;
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
    private final long maxExtensionSize;
    private final int maxTrailerSize;
    private long extensionSize;
    private final int maxSwallowSize;
    private boolean error;
    
    public ChunkedInputFilter(final int maxTrailerSize, final int maxExtensionSize, final int maxSwallowSize) {
        this.remaining = 0;
        this.pos = 0;
        this.lastValid = 0;
        this.buf = null;
        this.readChunk = new ByteChunk();
        this.endChunk = false;
        this.trailingHeaders = new ByteChunk();
        this.needCRLFParse = false;
        this.trailingHeaders.setLimit(maxTrailerSize);
        this.maxExtensionSize = maxExtensionSize;
        this.maxTrailerSize = maxTrailerSize;
        this.maxSwallowSize = maxSwallowSize;
    }
    
    @Override
    public int doRead(final ByteChunk chunk, final Request req) throws IOException {
        if (this.endChunk) {
            return -1;
        }
        this.checkError();
        if (this.needCRLFParse) {
            this.parseCRLF(this.needCRLFParse = false);
        }
        if (this.remaining <= 0) {
            if (!this.parseChunkHeader()) {
                this.throwIOException(ChunkedInputFilter.sm.getString("chunkedInputFilter.invalidHeader"));
            }
            if (this.endChunk) {
                this.parseEndChunk();
                return -1;
            }
        }
        int result = 0;
        if (this.pos >= this.lastValid && this.readBytes() < 0) {
            this.throwIOException(ChunkedInputFilter.sm.getString("chunkedInputFilter.eos"));
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
                this.parseCRLF(false);
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
        long swallowed = 0L;
        int read = 0;
        while ((read = this.doRead(this.readChunk, null)) >= 0) {
            swallowed += read;
            if (this.maxSwallowSize > -1 && swallowed > this.maxSwallowSize) {
                this.throwIOException(ChunkedInputFilter.sm.getString("inputFilter.maxSwallow"));
            }
        }
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
        this.needCRLFParse = false;
        this.trailingHeaders.recycle();
        this.trailingHeaders.setLimit(this.maxTrailerSize);
        this.extensionSize = 0L;
        this.error = false;
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
        int readDigit = 0;
        boolean extension = false;
        while (!eol) {
            if (this.pos >= this.lastValid && this.readBytes() <= 0) {
                return false;
            }
            if (this.buf[this.pos] == 13 || this.buf[this.pos] == 10) {
                this.parseCRLF(false);
                eol = true;
            }
            else if (this.buf[this.pos] == 59 && !extension) {
                extension = true;
                ++this.extensionSize;
            }
            else if (!extension) {
                final int charValue = HexUtils.getDec(this.buf[this.pos]);
                if (charValue == -1 || readDigit >= 8) {
                    return false;
                }
                ++readDigit;
                result = (result << 4 | charValue);
            }
            else {
                ++this.extensionSize;
                if (this.maxExtensionSize > -1L && this.extensionSize > this.maxExtensionSize) {
                    this.throwIOException(ChunkedInputFilter.sm.getString("chunkedInputFilter.maxExtension"));
                }
            }
            if (eol) {
                continue;
            }
            ++this.pos;
        }
        if (readDigit == 0 || result < 0) {
            return false;
        }
        if (result == 0) {
            this.endChunk = true;
        }
        this.remaining = result;
        return this.remaining >= 0;
    }
    
    @Deprecated
    protected boolean parseCRLF() throws IOException {
        this.parseCRLF(false);
        return true;
    }
    
    protected void parseCRLF(final boolean tolerant) throws IOException {
        boolean eol = false;
        boolean crfound = false;
        while (!eol) {
            if (this.pos >= this.lastValid && this.readBytes() <= 0) {
                this.throwIOException(ChunkedInputFilter.sm.getString("chunkedInputFilter.invalidCrlfNoData"));
            }
            if (this.buf[this.pos] == 13) {
                if (crfound) {
                    this.throwIOException(ChunkedInputFilter.sm.getString("chunkedInputFilter.invalidCrlfCRCR"));
                }
                crfound = true;
            }
            else if (this.buf[this.pos] == 10) {
                if (!tolerant && !crfound) {
                    this.throwIOException(ChunkedInputFilter.sm.getString("chunkedInputFilter.invalidCrlfNoCR"));
                }
                eol = true;
            }
            else {
                this.throwIOException(ChunkedInputFilter.sm.getString("chunkedInputFilter.invalidCrlf"));
            }
            ++this.pos;
        }
    }
    
    protected void parseEndChunk() throws IOException {
        while (this.parseHeader()) {}
    }
    
    private boolean parseHeader() throws IOException {
        final MimeHeaders headers = this.request.getMimeHeaders();
        byte chr = 0;
        if (this.pos >= this.lastValid && this.readBytes() < 0) {
            this.throwEOFException(ChunkedInputFilter.sm.getString("chunkedInputFilter.eosTrailer"));
        }
        chr = this.buf[this.pos];
        if (chr == 13 || chr == 10) {
            this.parseCRLF(false);
            return false;
        }
        int start = this.trailingHeaders.getEnd();
        boolean colon = false;
        while (!colon) {
            if (this.pos >= this.lastValid && this.readBytes() < 0) {
                this.throwEOFException(ChunkedInputFilter.sm.getString("chunkedInputFilter.eosTrailer"));
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
                    this.throwEOFException(ChunkedInputFilter.sm.getString("chunkedInputFilter.eosTrailer"));
                }
                chr = this.buf[this.pos];
                if (chr == 32 || chr == 9) {
                    ++this.pos;
                    final int newlimit = this.trailingHeaders.getLimit() - 1;
                    if (this.trailingHeaders.getEnd() > newlimit) {
                        this.throwIOException(ChunkedInputFilter.sm.getString("chunkedInputFilter.maxTrailer"));
                    }
                    this.trailingHeaders.setLimit(newlimit);
                }
                else {
                    space = false;
                }
            }
            while (!eol) {
                if (this.pos >= this.lastValid && this.readBytes() < 0) {
                    this.throwEOFException(ChunkedInputFilter.sm.getString("chunkedInputFilter.eosTrailer"));
                }
                chr = this.buf[this.pos];
                if (chr == 13 || chr == 10) {
                    this.parseCRLF(true);
                    eol = true;
                }
                else if (chr == 32) {
                    this.trailingHeaders.append(chr);
                }
                else {
                    this.trailingHeaders.append(chr);
                    lastSignificantChar = this.trailingHeaders.getEnd();
                }
                if (!eol) {
                    ++this.pos;
                }
            }
            if (this.pos >= this.lastValid && this.readBytes() < 0) {
                this.throwEOFException(ChunkedInputFilter.sm.getString("chunkedInputFilter.eosTrailer"));
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
    
    private void throwIOException(final String msg) throws IOException {
        this.error = true;
        throw new IOException(msg);
    }
    
    private void throwEOFException(final String msg) throws IOException {
        this.error = true;
        throw new EOFException(msg);
    }
    
    private void checkError() throws IOException {
        if (this.error) {
            throw new IOException(ChunkedInputFilter.sm.getString("chunkedInputFilter.error"));
        }
    }
    
    static {
        sm = StringManager.getManager(ChunkedInputFilter.class.getPackage().getName());
        (ENCODING = new ByteChunk()).setBytes("chunked".getBytes(Charset.defaultCharset()), 0, "chunked".length());
    }
}
