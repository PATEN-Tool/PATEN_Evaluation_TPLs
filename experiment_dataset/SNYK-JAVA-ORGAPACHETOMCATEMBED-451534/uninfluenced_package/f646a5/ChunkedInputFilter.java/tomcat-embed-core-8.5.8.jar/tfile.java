// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.coyote.http11.filters;

import java.io.EOFException;
import org.apache.tomcat.util.buf.MessageBytes;
import org.apache.tomcat.util.http.MimeHeaders;
import java.util.Locale;
import java.nio.charset.StandardCharsets;
import org.apache.tomcat.util.buf.HexUtils;
import java.io.IOException;
import java.util.Set;
import org.apache.coyote.Request;
import java.nio.ByteBuffer;
import org.apache.coyote.InputBuffer;
import org.apache.tomcat.util.buf.ByteChunk;
import org.apache.tomcat.util.res.StringManager;
import org.apache.tomcat.util.net.ApplicationBufferHandler;
import org.apache.coyote.http11.InputFilter;

public class ChunkedInputFilter implements InputFilter, ApplicationBufferHandler
{
    private static final StringManager sm;
    protected static final String ENCODING_NAME = "chunked";
    protected static final ByteChunk ENCODING;
    protected InputBuffer buffer;
    protected int remaining;
    protected ByteBuffer readChunk;
    protected boolean endChunk;
    protected final ByteChunk trailingHeaders;
    protected boolean needCRLFParse;
    private Request request;
    private final long maxExtensionSize;
    private final int maxTrailerSize;
    private long extensionSize;
    private final int maxSwallowSize;
    private boolean error;
    private final Set<String> allowedTrailerHeaders;
    
    public ChunkedInputFilter(final int maxTrailerSize, final Set<String> allowedTrailerHeaders, final int maxExtensionSize, final int maxSwallowSize) {
        this.remaining = 0;
        this.endChunk = false;
        this.trailingHeaders = new ByteChunk();
        this.needCRLFParse = false;
        this.trailingHeaders.setLimit(maxTrailerSize);
        this.allowedTrailerHeaders = allowedTrailerHeaders;
        this.maxExtensionSize = maxExtensionSize;
        this.maxTrailerSize = maxTrailerSize;
        this.maxSwallowSize = maxSwallowSize;
    }
    
    @Override
    @Deprecated
    public int doRead(final ByteChunk chunk) throws IOException {
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
        if ((this.readChunk == null || this.readChunk.position() >= this.readChunk.limit()) && this.readBytes() < 0) {
            this.throwIOException(ChunkedInputFilter.sm.getString("chunkedInputFilter.eos"));
        }
        if (this.remaining > this.readChunk.remaining()) {
            result = this.readChunk.remaining();
            this.remaining -= result;
            chunk.setBytes(this.readChunk.array(), this.readChunk.arrayOffset() + this.readChunk.position(), result);
            this.readChunk.position(this.readChunk.limit());
        }
        else {
            result = this.remaining;
            chunk.setBytes(this.readChunk.array(), this.readChunk.arrayOffset() + this.readChunk.position(), this.remaining);
            this.readChunk.position(this.readChunk.position() + this.remaining);
            this.remaining = 0;
            if (this.readChunk.position() + 1 >= this.readChunk.limit()) {
                this.needCRLFParse = true;
            }
            else {
                this.parseCRLF(false);
            }
        }
        return result;
    }
    
    @Override
    public int doRead(final ApplicationBufferHandler handler) throws IOException {
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
        if ((this.readChunk == null || this.readChunk.position() >= this.readChunk.limit()) && this.readBytes() < 0) {
            this.throwIOException(ChunkedInputFilter.sm.getString("chunkedInputFilter.eos"));
        }
        if (this.remaining > this.readChunk.remaining()) {
            result = this.readChunk.remaining();
            this.remaining -= result;
            if (this.readChunk != handler.getByteBuffer()) {
                handler.setByteBuffer(this.readChunk.duplicate());
            }
            this.readChunk.position(this.readChunk.limit());
        }
        else {
            result = this.remaining;
            if (this.readChunk != handler.getByteBuffer()) {
                handler.setByteBuffer(this.readChunk.duplicate());
                handler.getByteBuffer().limit(this.readChunk.position() + this.remaining);
            }
            this.readChunk.position(this.readChunk.position() + this.remaining);
            this.remaining = 0;
            if (this.readChunk.position() + 1 >= this.readChunk.limit()) {
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
        while ((read = this.doRead(this)) >= 0) {
            swallowed += read;
            if (this.maxSwallowSize > -1 && swallowed > this.maxSwallowSize) {
                this.throwIOException(ChunkedInputFilter.sm.getString("inputFilter.maxSwallow"));
            }
        }
        return this.readChunk.remaining();
    }
    
    @Override
    public int available() {
        return (this.readChunk != null) ? this.readChunk.remaining() : 0;
    }
    
    @Override
    public void setBuffer(final InputBuffer buffer) {
        this.buffer = buffer;
    }
    
    @Override
    public void recycle() {
        this.remaining = 0;
        if (this.readChunk != null) {
            this.readChunk.position(0).limit(0);
        }
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
    
    @Override
    public boolean isFinished() {
        return this.endChunk;
    }
    
    protected int readBytes() throws IOException {
        return this.buffer.doRead(this);
    }
    
    protected boolean parseChunkHeader() throws IOException {
        int result = 0;
        boolean eol = false;
        int readDigit = 0;
        boolean extension = false;
        while (!eol) {
            if ((this.readChunk == null || this.readChunk.position() >= this.readChunk.limit()) && this.readBytes() <= 0) {
                return false;
            }
            final byte chr = this.readChunk.get(this.readChunk.position());
            if (chr == 13 || chr == 10) {
                this.parseCRLF(false);
                eol = true;
            }
            else if (chr == 59 && !extension) {
                extension = true;
                ++this.extensionSize;
            }
            else if (!extension) {
                final int charValue = HexUtils.getDec(chr);
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
            this.readChunk.position(this.readChunk.position() + 1);
        }
        if (readDigit == 0 || result < 0) {
            return false;
        }
        if (result == 0) {
            this.endChunk = true;
        }
        this.remaining = result;
        return true;
    }
    
    protected void parseCRLF(final boolean tolerant) throws IOException {
        boolean eol = false;
        boolean crfound = false;
        while (!eol) {
            if ((this.readChunk == null || this.readChunk.position() >= this.readChunk.limit()) && this.readBytes() <= 0) {
                this.throwIOException(ChunkedInputFilter.sm.getString("chunkedInputFilter.invalidCrlfNoData"));
            }
            final byte chr = this.readChunk.get(this.readChunk.position());
            if (chr == 13) {
                if (crfound) {
                    this.throwIOException(ChunkedInputFilter.sm.getString("chunkedInputFilter.invalidCrlfCRCR"));
                }
                crfound = true;
            }
            else if (chr == 10) {
                if (!tolerant && !crfound) {
                    this.throwIOException(ChunkedInputFilter.sm.getString("chunkedInputFilter.invalidCrlfNoCR"));
                }
                eol = true;
            }
            else {
                this.throwIOException(ChunkedInputFilter.sm.getString("chunkedInputFilter.invalidCrlf"));
            }
            this.readChunk.position(this.readChunk.position() + 1);
        }
    }
    
    protected void parseEndChunk() throws IOException {
        while (this.parseHeader()) {}
    }
    
    private boolean parseHeader() throws IOException {
        final MimeHeaders headers = this.request.getMimeHeaders();
        byte chr = 0;
        if ((this.readChunk == null || this.readChunk.position() >= this.readChunk.limit()) && this.readBytes() < 0) {
            this.throwEOFException(ChunkedInputFilter.sm.getString("chunkedInputFilter.eosTrailer"));
        }
        chr = this.readChunk.get(this.readChunk.position());
        if (chr == 13 || chr == 10) {
            this.parseCRLF(false);
            return false;
        }
        final int startPos = this.trailingHeaders.getEnd();
        boolean colon = false;
        while (!colon) {
            if ((this.readChunk == null || this.readChunk.position() >= this.readChunk.limit()) && this.readBytes() < 0) {
                this.throwEOFException(ChunkedInputFilter.sm.getString("chunkedInputFilter.eosTrailer"));
            }
            chr = this.readChunk.get(this.readChunk.position());
            if (chr >= 65 && chr <= 90) {
                chr += 32;
            }
            if (chr == 58) {
                colon = true;
            }
            else {
                this.trailingHeaders.append(chr);
            }
            this.readChunk.position(this.readChunk.position() + 1);
        }
        final int colonPos = this.trailingHeaders.getEnd();
        boolean eol = false;
        boolean validLine = true;
        int lastSignificantChar = 0;
        while (validLine) {
            boolean space = true;
            while (space) {
                if ((this.readChunk == null || this.readChunk.position() >= this.readChunk.limit()) && this.readBytes() < 0) {
                    this.throwEOFException(ChunkedInputFilter.sm.getString("chunkedInputFilter.eosTrailer"));
                }
                chr = this.readChunk.get(this.readChunk.position());
                if (chr == 32 || chr == 9) {
                    this.readChunk.position(this.readChunk.position() + 1);
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
                if ((this.readChunk == null || this.readChunk.position() >= this.readChunk.limit()) && this.readBytes() < 0) {
                    this.throwEOFException(ChunkedInputFilter.sm.getString("chunkedInputFilter.eosTrailer"));
                }
                chr = this.readChunk.get(this.readChunk.position());
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
                    this.readChunk.position(this.readChunk.position() + 1);
                }
            }
            if ((this.readChunk == null || this.readChunk.position() >= this.readChunk.limit()) && this.readBytes() < 0) {
                this.throwEOFException(ChunkedInputFilter.sm.getString("chunkedInputFilter.eosTrailer"));
            }
            chr = this.readChunk.get(this.readChunk.position());
            if (chr != 32 && chr != 9) {
                validLine = false;
            }
            else {
                eol = false;
                this.trailingHeaders.append(chr);
            }
        }
        final String headerName = new String(this.trailingHeaders.getBytes(), startPos, colonPos - startPos, StandardCharsets.ISO_8859_1);
        if (this.allowedTrailerHeaders.contains(headerName.toLowerCase(Locale.ENGLISH))) {
            final MessageBytes headerValue = headers.addValue(headerName);
            headerValue.setBytes(this.trailingHeaders.getBytes(), colonPos, lastSignificantChar - colonPos);
        }
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
    
    @Override
    public void setByteBuffer(final ByteBuffer buffer) {
        this.readChunk = buffer;
    }
    
    @Override
    public ByteBuffer getByteBuffer() {
        return this.readChunk;
    }
    
    @Override
    public void expand(final int size) {
    }
    
    static {
        sm = StringManager.getManager(ChunkedInputFilter.class.getPackage().getName());
        (ENCODING = new ByteChunk()).setBytes("chunked".getBytes(StandardCharsets.ISO_8859_1), 0, "chunked".length());
    }
}
