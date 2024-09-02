// 
// Decompiled by Procyon v0.5.36
// 

package io.undertow.server.protocol.http;

import org.xnio.XnioWorker;
import org.xnio.conduits.Conduits;
import java.nio.channels.WritableByteChannel;
import java.nio.channels.ReadableByteChannel;
import org.xnio.conduits.ConduitWritableByteChannel;
import org.xnio.channels.StreamSourceChannel;
import java.nio.channels.FileChannel;
import java.nio.Buffer;
import org.xnio.Buffers;
import java.io.Closeable;
import org.xnio.IoUtils;
import org.xnio.Bits;
import io.undertow.util.HttpString;
import io.undertow.util.HeaderMap;
import java.io.IOException;
import io.undertow.UndertowMessages;
import io.undertow.util.StatusCodes;
import io.undertow.util.Protocols;
import io.undertow.server.Connectors;
import java.nio.channels.ClosedChannelException;
import java.nio.ByteBuffer;
import io.undertow.server.HttpServerExchange;
import io.undertow.connector.PooledByteBuffer;
import io.undertow.util.HeaderValues;
import io.undertow.connector.ByteBufferPool;
import org.xnio.conduits.StreamSinkConduit;
import org.xnio.conduits.AbstractStreamSinkConduit;

final class HttpResponseConduit extends AbstractStreamSinkConduit<StreamSinkConduit>
{
    private final ByteBufferPool pool;
    private final HttpServerConnection connection;
    private int state;
    private long fiCookie;
    private String string;
    private HeaderValues headerValues;
    private int valueIdx;
    private int charIndex;
    private PooledByteBuffer pooledBuffer;
    private PooledByteBuffer pooledFileTransferBuffer;
    private HttpServerExchange exchange;
    private ByteBuffer[] writevBuffer;
    private boolean done;
    private static final int STATE_BODY = 0;
    private static final int STATE_START = 1;
    private static final int STATE_HDR_NAME = 2;
    private static final int STATE_HDR_D = 3;
    private static final int STATE_HDR_DS = 4;
    private static final int STATE_HDR_VAL = 5;
    private static final int STATE_HDR_EOL_CR = 6;
    private static final int STATE_HDR_EOL_LF = 7;
    private static final int STATE_HDR_FINAL_CR = 8;
    private static final int STATE_HDR_FINAL_LF = 9;
    private static final int STATE_BUF_FLUSH = 10;
    private static final int MASK_STATE = 15;
    private static final int FLAG_SHUTDOWN = 16;
    
    HttpResponseConduit(final StreamSinkConduit next, final ByteBufferPool pool, final HttpServerConnection connection) {
        super(next);
        this.state = 1;
        this.fiCookie = -1L;
        this.done = false;
        this.pool = pool;
        this.connection = connection;
    }
    
    HttpResponseConduit(final StreamSinkConduit next, final ByteBufferPool pool, final HttpServerConnection connection, final HttpServerExchange exchange) {
        super(next);
        this.state = 1;
        this.fiCookie = -1L;
        this.done = false;
        this.pool = pool;
        this.connection = connection;
        this.exchange = exchange;
    }
    
    void reset(final HttpServerExchange exchange) {
        this.exchange = exchange;
        this.state = 1;
        this.fiCookie = -1L;
        this.string = null;
        this.headerValues = null;
        this.valueIdx = 0;
        this.charIndex = 0;
    }
    
    private int processWrite(final int state, final Object userData, final int pos, final int length) throws IOException {
        if (this.done || this.exchange == null) {
            throw new ClosedChannelException();
        }
        try {
            assert state != 0;
            Label_0249: {
                if (state != 10) {
                    break Label_0249;
                }
                final ByteBuffer byteBuffer = this.pooledBuffer.getBuffer();
                while (true) {
                    long res = 0L;
                    if (userData == null || length == 0) {
                        res = ((StreamSinkConduit)this.next).write(byteBuffer);
                    }
                    else if (userData instanceof ByteBuffer) {
                        ByteBuffer[] data = this.writevBuffer;
                        if (data == null) {
                            final ByteBuffer[] writevBuffer = new ByteBuffer[2];
                            this.writevBuffer = writevBuffer;
                            data = writevBuffer;
                        }
                        data[0] = byteBuffer;
                        data[1] = (ByteBuffer)userData;
                        res = ((StreamSinkConduit)this.next).write(data, 0, 2);
                    }
                    else {
                        ByteBuffer[] data = this.writevBuffer;
                        if (data == null || data.length < length + 1) {
                            final ByteBuffer[] writevBuffer2 = new ByteBuffer[length + 1];
                            this.writevBuffer = writevBuffer2;
                            data = writevBuffer2;
                        }
                        data[0] = byteBuffer;
                        System.arraycopy(userData, pos, data, 1, length);
                        res = ((StreamSinkConduit)this.next).write(data, 0, length + 1);
                    }
                    if (res == 0L) {
                        return 10;
                    }
                    try {
                        if (!byteBuffer.hasRemaining()) {
                            this.bufferDone();
                            return 0;
                        }
                        continue;
                        try {
                            if (state != 1) {
                                return this.processStatefulWrite(state, userData, pos, length);
                            }
                            try {
                                Connectors.flattenCookies(this.exchange);
                                if (this.pooledBuffer == null) {
                                    this.pooledBuffer = this.pool.allocate();
                                }
                                final ByteBuffer buffer = this.pooledBuffer.getBuffer();
                                assert buffer.remaining() >= 50;
                                Protocols.HTTP_1_1.appendTo(buffer);
                                buffer.put((byte)32);
                                final int code = this.exchange.getStatusCode();
                                assert 999 >= code && code >= 100;
                                buffer.put((byte)(code / 100 + 48));
                                buffer.put((byte)(code / 10 % 10 + 48));
                                buffer.put((byte)(code % 10 + 48));
                                buffer.put((byte)32);
                                String string = this.exchange.getReasonPhrase();
                                if (string == null) {
                                    string = StatusCodes.getReason(code);
                                }
                                if (string.length() > buffer.remaining()) {
                                    this.pooledBuffer.close();
                                    this.pooledBuffer = null;
                                    this.truncateWrites();
                                    throw UndertowMessages.MESSAGES.reasonPhraseToLargeForBuffer(string);
                                }
                                writeString(buffer, string);
                                buffer.put((byte)13).put((byte)10);
                                int remaining = buffer.remaining();
                                final HeaderMap headers = this.exchange.getResponseHeaders();
                                long fiCookie = headers.fastIterateNonEmpty();
                                HeaderValues headerValues = null;
                                int valueIdx = 0;
                            Label_0577_Outer:
                                while (true) {
                                    Label_0786: {
                                        if (fiCookie == -1L) {
                                            break Label_0786;
                                        }
                                        headerValues = headers.fiCurrent(fiCookie);
                                        final HttpString header = headerValues.getHeaderName();
                                        final int headerSize = header.length();
                                        valueIdx = 0;
                                        ByteBuffer[] data2;
                                        ByteBuffer[] writevBuffer3;
                                        ByteBuffer[] writevBuffer4;
                                        long res2;
                                        Block_30_Outer:Label_0917_Outer:
                                        while (true) {
                                            Label_0774: {
                                                if (valueIdx >= headerValues.size()) {
                                                    break Label_0774;
                                                }
                                                remaining -= headerSize + 2;
                                                if (remaining < 0) {
                                                    break;
                                                }
                                                try {
                                                    header.appendTo(buffer);
                                                    buffer.put((byte)58).put((byte)32);
                                                    string = headerValues.get(valueIdx++);
                                                    remaining -= string.length() + 2;
                                                    if (remaining < 2) {
                                                        this.fiCookie = fiCookie;
                                                        this.string = string;
                                                        this.headerValues = headerValues;
                                                        this.valueIdx = valueIdx;
                                                        this.charIndex = 0;
                                                        this.state = 5;
                                                        buffer.flip();
                                                        return this.processStatefulWrite(5, userData, pos, length);
                                                    }
                                                    try {
                                                        writeString(buffer, string);
                                                        buffer.put((byte)13).put((byte)10);
                                                        continue Block_30_Outer;
                                                        fiCookie = headers.fiNextNonEmpty(fiCookie);
                                                        continue Label_0577_Outer;
                                                        // iftrue(Label_0861:, data2 != null)
                                                        // iftrue(Label_0832:, userData != null)
                                                        // iftrue(Label_0978:, res2 != 0L)
                                                        // iftrue(Label_0896:, !userData instanceof ByteBuffer)
                                                        // iftrue(Label_0931:, data2 != null && data2.length >= length + 1)
                                                    Label_0861:
                                                        while (true) {
                                                            while (true) {
                                                                data2 = this.writevBuffer;
                                                                Label_0931: {
                                                                    Block_31: {
                                                                        break Block_31;
                                                                        writevBuffer3 = new ByteBuffer[length + 1];
                                                                        this.writevBuffer = writevBuffer3;
                                                                        data2 = writevBuffer3;
                                                                        break Label_0931;
                                                                    }
                                                                    writevBuffer4 = new ByteBuffer[2];
                                                                    this.writevBuffer = writevBuffer4;
                                                                    data2 = writevBuffer4;
                                                                    break Label_0861;
                                                                }
                                                                data2[0] = buffer;
                                                                System.arraycopy(userData, pos, data2, 1, length);
                                                                res2 = ((StreamSinkConduit)this.next).write(data2, 0, length + 1);
                                                                while (true) {
                                                                    Label_0968: {
                                                                        break Label_0968;
                                                                        res2 = 0L;
                                                                        res2 = ((StreamSinkConduit)this.next).write(buffer);
                                                                    }
                                                                    return 10;
                                                                    try {
                                                                        Label_0978: {
                                                                            if (!buffer.hasRemaining()) {
                                                                                this.bufferDone();
                                                                                return 0;
                                                                            }
                                                                        }
                                                                        continue;
                                                                    }
                                                                    catch (RuntimeException e) {
                                                                        if (this.pooledBuffer != null) {
                                                                            this.pooledBuffer.close();
                                                                            this.pooledBuffer = null;
                                                                        }
                                                                        throw e;
                                                                    }
                                                                    buffer.put((byte)13).put((byte)10);
                                                                    buffer.flip();
                                                                    continue;
                                                                }
                                                                Label_0832: {
                                                                    continue Label_0917_Outer;
                                                                }
                                                            }
                                                            Label_0896: {
                                                                data2 = this.writevBuffer;
                                                            }
                                                            continue;
                                                        }
                                                        data2[0] = buffer;
                                                        data2[1] = (ByteBuffer)userData;
                                                        res2 = ((StreamSinkConduit)this.next).write(data2, 0, 2);
                                                    }
                                                    catch (RuntimeException ex) {}
                                                }
                                                catch (RuntimeException ex2) {}
                                            }
                                            break;
                                        }
                                    }
                                }
                                this.fiCookie = fiCookie;
                                this.string = string;
                                this.headerValues = headerValues;
                                this.valueIdx = valueIdx;
                                this.charIndex = 0;
                                this.state = 2;
                                buffer.flip();
                                return this.processStatefulWrite(2, userData, pos, length);
                            }
                            catch (RuntimeException ex3) {}
                        }
                        catch (RuntimeException ex4) {}
                    }
                    catch (RuntimeException ex5) {}
                    break;
                }
            }
        }
        catch (IOException ex6) {}
        catch (RuntimeException ex7) {}
        catch (Error error) {}
    }
    
    private void bufferDone() {
        if (this.exchange == null) {
            return;
        }
        final HttpServerConnection connection = (HttpServerConnection)this.exchange.getConnection();
        if (connection.getExtraBytes() != null && connection.isOpen() && this.exchange.isRequestComplete()) {
            this.pooledBuffer.getBuffer().clear();
        }
        else {
            this.pooledBuffer.close();
            this.pooledBuffer = null;
            this.exchange = null;
        }
    }
    
    public void freeContinueResponse() {
        if (this.pooledBuffer != null) {
            this.pooledBuffer.close();
            this.pooledBuffer = null;
        }
    }
    
    private static void writeString(final ByteBuffer buffer, final String string) {
        for (int length = string.length(), charIndex = 0; charIndex < length; ++charIndex) {
            final char c = string.charAt(charIndex);
            final byte b = (byte)c;
            if (b != 13 && b != 10) {
                buffer.put(b);
            }
            else {
                buffer.put((byte)32);
            }
        }
    }
    
    private int processStatefulWrite(int state, final Object userData, final int pos, final int len) throws IOException {
        final ByteBuffer buffer = this.pooledBuffer.getBuffer();
        long fiCookie = this.fiCookie;
        int valueIdx = this.valueIdx;
        int charIndex = this.charIndex;
        String string = this.string;
        HeaderValues headerValues = this.headerValues;
        if (buffer.hasRemaining()) {
            do {
                final int res = ((StreamSinkConduit)this.next).write(buffer);
                if (res == 0) {
                    return state;
                }
            } while (buffer.hasRemaining());
        }
        buffer.clear();
        final HeaderMap headers = this.exchange.getResponseHeaders();
        Label_1403: {
            Label_1158: {
            Label_1120:
                while (true) {
                    Label_0995: {
                        switch (state) {
                            case 2: {
                                final HttpString headerName = headerValues.getHeaderName();
                                final int length = headerName.length();
                                while (charIndex < length) {
                                    if (buffer.hasRemaining()) {
                                        buffer.put(headerName.byteAt(charIndex++));
                                    }
                                    else {
                                        buffer.flip();
                                        do {
                                            final int res = ((StreamSinkConduit)this.next).write(buffer);
                                            if (res == 0) {
                                                this.string = string;
                                                this.headerValues = headerValues;
                                                this.charIndex = charIndex;
                                                this.fiCookie = fiCookie;
                                                this.valueIdx = valueIdx;
                                                return 2;
                                            }
                                        } while (buffer.hasRemaining());
                                        buffer.clear();
                                    }
                                }
                            }
                            case 3: {
                                if (!buffer.hasRemaining()) {
                                    buffer.flip();
                                    do {
                                        final int res = ((StreamSinkConduit)this.next).write(buffer);
                                        if (res == 0) {
                                            this.string = string;
                                            this.headerValues = headerValues;
                                            this.charIndex = charIndex;
                                            this.fiCookie = fiCookie;
                                            this.valueIdx = valueIdx;
                                            return 3;
                                        }
                                    } while (buffer.hasRemaining());
                                    buffer.clear();
                                }
                                buffer.put((byte)58);
                            }
                            case 4: {
                                if (!buffer.hasRemaining()) {
                                    buffer.flip();
                                    do {
                                        final int res = ((StreamSinkConduit)this.next).write(buffer);
                                        if (res == 0) {
                                            this.string = string;
                                            this.headerValues = headerValues;
                                            this.charIndex = charIndex;
                                            this.fiCookie = fiCookie;
                                            this.valueIdx = valueIdx;
                                            return 4;
                                        }
                                    } while (buffer.hasRemaining());
                                    buffer.clear();
                                }
                                buffer.put((byte)32);
                                string = headerValues.get(valueIdx++);
                                charIndex = 0;
                            }
                            case 5: {
                                final int length = string.length();
                                while (charIndex < length) {
                                    if (buffer.hasRemaining()) {
                                        buffer.put((byte)string.charAt(charIndex++));
                                    }
                                    else {
                                        buffer.flip();
                                        do {
                                            final int res = ((StreamSinkConduit)this.next).write(buffer);
                                            if (res == 0) {
                                                this.string = string;
                                                this.headerValues = headerValues;
                                                this.charIndex = charIndex;
                                                this.fiCookie = fiCookie;
                                                this.valueIdx = valueIdx;
                                                return 5;
                                            }
                                        } while (buffer.hasRemaining());
                                        buffer.clear();
                                    }
                                }
                                charIndex = 0;
                                if (valueIdx != headerValues.size()) {
                                    break Label_0995;
                                }
                                if (!buffer.hasRemaining() && this.flushHeaderBuffer(buffer, string, headerValues, charIndex, fiCookie, valueIdx)) {
                                    return 6;
                                }
                                buffer.put((byte)13);
                                if (!buffer.hasRemaining() && this.flushHeaderBuffer(buffer, string, headerValues, charIndex, fiCookie, valueIdx)) {
                                    return 7;
                                }
                                buffer.put((byte)10);
                                if ((fiCookie = headers.fiNextNonEmpty(fiCookie)) != -1L) {
                                    headerValues = headers.fiCurrent(fiCookie);
                                    valueIdx = 0;
                                    state = 2;
                                    continue;
                                }
                                if (!buffer.hasRemaining() && this.flushHeaderBuffer(buffer, string, headerValues, charIndex, fiCookie, valueIdx)) {
                                    return 8;
                                }
                                buffer.put((byte)13);
                                if (!buffer.hasRemaining() && this.flushHeaderBuffer(buffer, string, headerValues, charIndex, fiCookie, valueIdx)) {
                                    return 9;
                                }
                                buffer.put((byte)10);
                                this.fiCookie = -1L;
                                this.valueIdx = 0;
                                this.string = null;
                                buffer.flip();
                                if (userData == null) {
                                    do {
                                        final int res = ((StreamSinkConduit)this.next).write(buffer);
                                        if (res == 0) {
                                            return 10;
                                        }
                                    } while (buffer.hasRemaining());
                                }
                                else if (userData instanceof ByteBuffer) {
                                    final ByteBuffer[] b = { buffer, (ByteBuffer)userData };
                                    do {
                                        final long r = ((StreamSinkConduit)this.next).write(b, 0, b.length);
                                        if (r == 0L && buffer.hasRemaining()) {
                                            return 10;
                                        }
                                    } while (buffer.hasRemaining());
                                }
                                else {
                                    final ByteBuffer[] b = new ByteBuffer[1 + len];
                                    b[0] = buffer;
                                    System.arraycopy(userData, pos, b, 1, len);
                                    do {
                                        final long r = ((StreamSinkConduit)this.next).write(b, 0, b.length);
                                        if (r == 0L && buffer.hasRemaining()) {
                                            return 10;
                                        }
                                    } while (buffer.hasRemaining());
                                }
                                this.bufferDone();
                                return 0;
                            }
                            case 6: {
                                if (!buffer.hasRemaining() && this.flushHeaderBuffer(buffer, string, headerValues, charIndex, fiCookie, valueIdx)) {
                                    return 6;
                                }
                                buffer.put((byte)13);
                            }
                            case 7: {
                                if (!buffer.hasRemaining() && this.flushHeaderBuffer(buffer, string, headerValues, charIndex, fiCookie, valueIdx)) {
                                    return 7;
                                }
                                buffer.put((byte)10);
                                if (valueIdx < headerValues.size()) {
                                    state = 2;
                                    continue;
                                }
                                if ((fiCookie = headers.fiNextNonEmpty(fiCookie)) != -1L) {
                                    headerValues = headers.fiCurrent(fiCookie);
                                    valueIdx = 0;
                                    state = 2;
                                    continue;
                                }
                                break Label_1120;
                            }
                            case 8: {
                                break Label_1120;
                            }
                            case 9: {
                                break Label_1158;
                            }
                            case 10: {
                                break Label_1403;
                            }
                            default: {
                                throw new IllegalStateException();
                            }
                        }
                    }
                }
                if (!buffer.hasRemaining() && this.flushHeaderBuffer(buffer, string, headerValues, charIndex, fiCookie, valueIdx)) {
                    return 8;
                }
                buffer.put((byte)13);
            }
            if (!buffer.hasRemaining() && this.flushHeaderBuffer(buffer, string, headerValues, charIndex, fiCookie, valueIdx)) {
                return 9;
            }
            buffer.put((byte)10);
            this.fiCookie = -1L;
            this.valueIdx = 0;
            this.string = null;
            buffer.flip();
            if (userData == null) {
                do {
                    final int res = ((StreamSinkConduit)this.next).write(buffer);
                    if (res == 0) {
                        return 10;
                    }
                } while (buffer.hasRemaining());
            }
            else if (userData instanceof ByteBuffer) {
                final ByteBuffer[] b = { buffer, (ByteBuffer)userData };
                do {
                    final long r = ((StreamSinkConduit)this.next).write(b, 0, b.length);
                    if (r == 0L && buffer.hasRemaining()) {
                        return 10;
                    }
                } while (buffer.hasRemaining());
            }
            else {
                final ByteBuffer[] b = new ByteBuffer[1 + len];
                b[0] = buffer;
                System.arraycopy(userData, pos, b, 1, len);
                do {
                    final long r = ((StreamSinkConduit)this.next).write(b, 0, b.length);
                    if (r == 0L && buffer.hasRemaining()) {
                        return 10;
                    }
                } while (buffer.hasRemaining());
            }
        }
        this.bufferDone();
        return 0;
    }
    
    private boolean flushHeaderBuffer(final ByteBuffer buffer, final String string, final HeaderValues headerValues, final int charIndex, final long fiCookie, final int valueIdx) throws IOException {
        buffer.flip();
        do {
            final int res = ((StreamSinkConduit)this.next).write(buffer);
            if (res == 0) {
                this.string = string;
                this.headerValues = headerValues;
                this.charIndex = charIndex;
                this.fiCookie = fiCookie;
                this.valueIdx = valueIdx;
                return true;
            }
        } while (buffer.hasRemaining());
        buffer.clear();
        return false;
    }
    
    public int write(final ByteBuffer src) throws IOException {
        try {
            final int oldState = this.state;
            int state = oldState & 0xF;
            int alreadyWritten = 0;
            int originalRemaining = -1;
            try {
                if (state != 0) {
                    originalRemaining = src.remaining();
                    state = this.processWrite(state, src, -1, -1);
                    if (state != 0) {
                        return 0;
                    }
                    alreadyWritten = originalRemaining - src.remaining();
                    if (Bits.allAreSet(oldState, 16)) {
                        ((StreamSinkConduit)this.next).terminateWrites();
                        throw new ClosedChannelException();
                    }
                }
                if (alreadyWritten != originalRemaining) {
                    return ((StreamSinkConduit)this.next).write(src) + alreadyWritten;
                }
                return alreadyWritten;
            }
            finally {
                this.state = ((oldState & 0xFFFFFFF0) | state);
            }
        }
        catch (IOException | RuntimeException | Error ex) {
            final Throwable t;
            final Throwable e = t;
            IoUtils.safeClose((Closeable)this.connection);
            throw e;
        }
    }
    
    public long write(final ByteBuffer[] srcs) throws IOException {
        return this.write(srcs, 0, srcs.length);
    }
    
    public long write(final ByteBuffer[] srcs, final int offset, final int length) throws IOException {
        if (length == 0) {
            return 0L;
        }
        final int oldVal = this.state;
        int state = oldVal & 0xF;
        try {
            if (state == 0) {
                return (length == 1) ? ((StreamSinkConduit)this.next).write(srcs[offset]) : ((StreamSinkConduit)this.next).write(srcs, offset, length);
            }
            final long rem = Buffers.remaining((Buffer[])srcs, offset, length);
            state = this.processWrite(state, srcs, offset, length);
            final long ret = rem - Buffers.remaining((Buffer[])srcs, offset, length);
            if (state != 0) {
                return ret;
            }
            if (Bits.allAreSet(oldVal, 16)) {
                ((StreamSinkConduit)this.next).terminateWrites();
                throw new ClosedChannelException();
            }
            return ret;
        }
        catch (IOException ex) {}
        catch (RuntimeException ex2) {}
        catch (Error e) {
            IoUtils.safeClose((Closeable)this.connection);
            throw e;
        }
        finally {
            this.state = ((oldVal & 0xFFFFFFF0) | state);
        }
    }
    
    public long transferFrom(final FileChannel src, final long position, final long count) throws IOException {
        try {
            if (this.pooledFileTransferBuffer != null) {
                try {
                    return this.write(this.pooledFileTransferBuffer.getBuffer());
                }
                catch (IOException ex) {}
                catch (RuntimeException ex2) {}
                catch (Error e) {
                    if (this.pooledFileTransferBuffer != null) {
                        this.pooledFileTransferBuffer.close();
                        this.pooledFileTransferBuffer = null;
                    }
                    throw e;
                }
                finally {
                    if (this.pooledFileTransferBuffer != null && !this.pooledFileTransferBuffer.getBuffer().hasRemaining()) {
                        this.pooledFileTransferBuffer.close();
                        this.pooledFileTransferBuffer = null;
                    }
                }
            }
            if (this.state != 0) {
                final PooledByteBuffer pooled = this.exchange.getConnection().getByteBufferPool().allocate();
                final ByteBuffer buffer = pooled.getBuffer();
                try {
                    final int res = src.read(buffer);
                    buffer.flip();
                    if (res <= 0) {
                        return res;
                    }
                    return this.write(buffer);
                }
                finally {
                    if (buffer.hasRemaining()) {
                        this.pooledFileTransferBuffer = pooled;
                    }
                    else {
                        pooled.close();
                    }
                }
            }
            return ((StreamSinkConduit)this.next).transferFrom(src, position, count);
        }
        catch (IOException | RuntimeException | Error ex3) {
            final Throwable t;
            final Throwable e = t;
            IoUtils.safeClose((Closeable)this.connection);
            throw e;
        }
    }
    
    public long transferFrom(final StreamSourceChannel source, final long count, final ByteBuffer throughBuffer) throws IOException {
        try {
            if (this.state != 0) {
                return IoUtils.transfer((ReadableByteChannel)source, count, throughBuffer, (WritableByteChannel)new ConduitWritableByteChannel((StreamSinkConduit)this));
            }
            try {
                return ((StreamSinkConduit)this.next).transferFrom(source, count, throughBuffer);
            }
            catch (RuntimeException e) {
                IoUtils.safeClose((Closeable)this.connection);
                throw e;
            }
        }
        catch (IOException ex) {}
        catch (RuntimeException ex2) {}
        catch (Error error) {}
    }
    
    public int writeFinal(final ByteBuffer src) throws IOException {
        try {
            return Conduits.writeFinalBasic((StreamSinkConduit)this, src);
        }
        catch (IOException | RuntimeException | Error ex) {
            final Throwable t;
            final Throwable e = t;
            IoUtils.safeClose((Closeable)this.connection);
            throw e;
        }
    }
    
    public long writeFinal(final ByteBuffer[] srcs, final int offset, final int length) throws IOException {
        try {
            return Conduits.writeFinalBasic((StreamSinkConduit)this, srcs, offset, length);
        }
        catch (IOException | RuntimeException | Error ex) {
            final Throwable t;
            final Throwable e = t;
            IoUtils.safeClose((Closeable)this.connection);
            throw e;
        }
    }
    
    public boolean flush() throws IOException {
        final int oldVal = this.state;
        int state = oldVal & 0xF;
        try {
            if (state != 0) {
                state = this.processWrite(state, null, -1, -1);
                if (state != 0) {
                    return false;
                }
                if (Bits.allAreSet(oldVal, 16)) {
                    ((StreamSinkConduit)this.next).terminateWrites();
                }
            }
            return ((StreamSinkConduit)this.next).flush();
        }
        catch (IOException ex) {}
        catch (RuntimeException ex2) {}
        catch (Error e) {
            IoUtils.safeClose((Closeable)this.connection);
            throw e;
        }
        finally {
            this.state = ((oldVal & 0xFFFFFFF0) | state);
        }
    }
    
    public void terminateWrites() throws IOException {
        try {
            final int oldVal = this.state;
            if (Bits.allAreClear(oldVal, 15)) {
                ((StreamSinkConduit)this.next).terminateWrites();
                return;
            }
            try {
                this.state = (oldVal | 0x10);
            }
            catch (RuntimeException e) {
                IoUtils.safeClose((Closeable)this.connection);
                throw e;
            }
        }
        catch (IOException ex) {}
        catch (RuntimeException ex2) {}
        catch (Error error) {}
    }
    
    public void truncateWrites() throws IOException {
        try {
            ((StreamSinkConduit)this.next).truncateWrites();
        }
        catch (IOException ex) {}
        catch (RuntimeException ex2) {}
        catch (Error e) {
            IoUtils.safeClose((Closeable)this.connection);
            throw e;
        }
        finally {
            if (this.pooledBuffer != null) {
                this.bufferDone();
            }
            if (this.pooledFileTransferBuffer != null) {
                this.pooledFileTransferBuffer.close();
                this.pooledFileTransferBuffer = null;
            }
        }
    }
    
    public XnioWorker getWorker() {
        return ((StreamSinkConduit)this.next).getWorker();
    }
    
    void freeBuffers() {
        this.done = true;
        if (this.pooledBuffer != null) {
            this.bufferDone();
        }
        if (this.pooledFileTransferBuffer != null) {
            this.pooledFileTransferBuffer.close();
            this.pooledFileTransferBuffer = null;
        }
    }
}
