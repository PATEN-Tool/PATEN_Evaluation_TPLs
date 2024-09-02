// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.coyote.http2;

import java.nio.ByteBuffer;
import org.apache.tomcat.util.res.StringManager;

public class HpackDecoder
{
    protected static final StringManager sm;
    private static final int DEFAULT_RING_BUFFER_SIZE = 10;
    private HeaderEmitter headerEmitter;
    private Hpack.HeaderField[] headerTable;
    private int firstSlotPosition;
    private int filledTableSlots;
    private int currentMemorySize;
    private int maxMemorySize;
    private final StringBuilder stringBuilder;
    
    public HpackDecoder(final int maxMemorySize) {
        this.firstSlotPosition = 0;
        this.filledTableSlots = 0;
        this.currentMemorySize = 0;
        this.stringBuilder = new StringBuilder();
        this.maxMemorySize = maxMemorySize;
        this.headerTable = new Hpack.HeaderField[10];
    }
    
    public HpackDecoder() {
        this(4096);
    }
    
    public void decode(final ByteBuffer buffer) throws HpackException {
        while (buffer.hasRemaining()) {
            final int originalPos = buffer.position();
            final byte b = buffer.get();
            if ((b & 0x80) != 0x0) {
                buffer.position(buffer.position() - 1);
                final int index = Hpack.decodeInteger(buffer, 7);
                if (index == -1) {
                    buffer.position(originalPos);
                    return;
                }
                if (index == 0) {
                    throw new HpackException(HpackDecoder.sm.getString("hpackdecoder.zeroNotValidHeaderTableIndex"));
                }
                this.handleIndex(index);
            }
            else if ((b & 0x40) != 0x0) {
                final String headerName = this.readHeaderName(buffer, 6);
                if (headerName == null) {
                    buffer.position(originalPos);
                    return;
                }
                final String headerValue = this.readHpackString(buffer);
                if (headerValue == null) {
                    buffer.position(originalPos);
                    return;
                }
                this.headerEmitter.emitHeader(headerName, headerValue, false);
                this.addEntryToHeaderTable(new Hpack.HeaderField(headerName, headerValue));
            }
            else if ((b & 0xF0) == 0x0) {
                final String headerName = this.readHeaderName(buffer, 4);
                if (headerName == null) {
                    buffer.position(originalPos);
                    return;
                }
                final String headerValue = this.readHpackString(buffer);
                if (headerValue == null) {
                    buffer.position(originalPos);
                    return;
                }
                this.headerEmitter.emitHeader(headerName, headerValue, false);
            }
            else if ((b & 0xF0) == 0x10) {
                final String headerName = this.readHeaderName(buffer, 4);
                if (headerName == null) {
                    buffer.position(originalPos);
                    return;
                }
                final String headerValue = this.readHpackString(buffer);
                if (headerValue == null) {
                    buffer.position(originalPos);
                    return;
                }
                this.headerEmitter.emitHeader(headerName, headerValue, true);
            }
            else {
                if ((b & 0xE0) != 0x20) {
                    throw new RuntimeException("Not yet implemented");
                }
                if (!this.handleMaxMemorySizeChange(buffer, originalPos)) {
                    return;
                }
                continue;
            }
        }
    }
    
    private boolean handleMaxMemorySizeChange(final ByteBuffer buffer, final int originalPos) throws HpackException {
        buffer.position(buffer.position() - 1);
        final int size = Hpack.decodeInteger(buffer, 5);
        if (size == -1) {
            buffer.position(originalPos);
            return false;
        }
        this.maxMemorySize = size;
        if (this.currentMemorySize > this.maxMemorySize) {
            int newTableSlots = this.filledTableSlots;
            final int tableLength = this.headerTable.length;
            int newSize;
            Hpack.HeaderField oldData;
            for (newSize = this.currentMemorySize; newSize > this.maxMemorySize; newSize -= oldData.size, --newTableSlots) {
                final int clearIndex = this.firstSlotPosition;
                ++this.firstSlotPosition;
                if (this.firstSlotPosition == tableLength) {
                    this.firstSlotPosition = 0;
                }
                oldData = this.headerTable[clearIndex];
                this.headerTable[clearIndex] = null;
            }
            this.filledTableSlots = newTableSlots;
            this.currentMemorySize = newSize;
        }
        return true;
    }
    
    private String readHeaderName(final ByteBuffer buffer, final int prefixLength) throws HpackException {
        buffer.position(buffer.position() - 1);
        final int index = Hpack.decodeInteger(buffer, prefixLength);
        if (index == -1) {
            return null;
        }
        if (index != 0) {
            return this.handleIndexedHeaderName(index);
        }
        return this.readHpackString(buffer);
    }
    
    private String readHpackString(final ByteBuffer buffer) throws HpackException {
        if (!buffer.hasRemaining()) {
            return null;
        }
        final byte data = buffer.get(buffer.position());
        final int length = Hpack.decodeInteger(buffer, 7);
        if (buffer.remaining() < length) {
            return null;
        }
        final boolean huffman = (data & 0x80) != 0x0;
        if (huffman) {
            return this.readHuffmanString(length, buffer);
        }
        for (int i = 0; i < length; ++i) {
            this.stringBuilder.append((char)buffer.get());
        }
        final String ret = this.stringBuilder.toString();
        this.stringBuilder.setLength(0);
        return ret;
    }
    
    private String readHuffmanString(final int length, final ByteBuffer buffer) throws HpackException {
        HPackHuffman.decode(buffer, length, this.stringBuilder);
        final String ret = this.stringBuilder.toString();
        this.stringBuilder.setLength(0);
        return ret;
    }
    
    private String handleIndexedHeaderName(final int index) throws HpackException {
        if (index <= Hpack.STATIC_TABLE_LENGTH) {
            return Hpack.STATIC_TABLE[index].name;
        }
        if (index >= Hpack.STATIC_TABLE_LENGTH + this.filledTableSlots) {
            throw new HpackException();
        }
        final int adjustedIndex = this.getRealIndex(index - Hpack.STATIC_TABLE_LENGTH);
        final Hpack.HeaderField res = this.headerTable[adjustedIndex];
        if (res == null) {
            throw new HpackException();
        }
        return res.name;
    }
    
    private void handleIndex(final int index) throws HpackException {
        if (index <= Hpack.STATIC_TABLE_LENGTH) {
            this.addStaticTableEntry(index);
        }
        else {
            final int adjustedIndex = this.getRealIndex(index - Hpack.STATIC_TABLE_LENGTH);
            final Hpack.HeaderField headerField = this.headerTable[adjustedIndex];
            this.headerEmitter.emitHeader(headerField.name, headerField.value, false);
        }
    }
    
    int getRealIndex(final int index) {
        return (this.firstSlotPosition + (this.filledTableSlots - index)) % this.headerTable.length;
    }
    
    private void addStaticTableEntry(final int index) throws HpackException {
        final Hpack.HeaderField entry = Hpack.STATIC_TABLE[index];
        if (entry.value == null) {
            throw new HpackException();
        }
        this.headerEmitter.emitHeader(entry.name, entry.value, false);
    }
    
    private void addEntryToHeaderTable(final Hpack.HeaderField entry) {
        if (entry.size > this.maxMemorySize) {
            while (this.filledTableSlots > 0) {
                this.headerTable[this.firstSlotPosition] = null;
                ++this.firstSlotPosition;
                if (this.firstSlotPosition == this.headerTable.length) {
                    this.firstSlotPosition = 0;
                }
                --this.filledTableSlots;
            }
            this.currentMemorySize = 0;
            return;
        }
        this.resizeIfRequired();
        int newTableSlots = this.filledTableSlots + 1;
        final int tableLength = this.headerTable.length;
        final int index = (this.firstSlotPosition + this.filledTableSlots) % tableLength;
        this.headerTable[index] = entry;
        int newSize;
        Hpack.HeaderField oldData;
        for (newSize = this.currentMemorySize + entry.size; newSize > this.maxMemorySize; newSize -= oldData.size, --newTableSlots) {
            final int clearIndex = this.firstSlotPosition;
            ++this.firstSlotPosition;
            if (this.firstSlotPosition == tableLength) {
                this.firstSlotPosition = 0;
            }
            oldData = this.headerTable[clearIndex];
            this.headerTable[clearIndex] = null;
        }
        this.filledTableSlots = newTableSlots;
        this.currentMemorySize = newSize;
    }
    
    private void resizeIfRequired() {
        if (this.filledTableSlots == this.headerTable.length) {
            final Hpack.HeaderField[] newArray = new Hpack.HeaderField[this.headerTable.length + 10];
            for (int i = 0; i < this.headerTable.length; ++i) {
                newArray[i] = this.headerTable[(this.firstSlotPosition + i) % this.headerTable.length];
            }
            this.firstSlotPosition = 0;
            this.headerTable = newArray;
        }
    }
    
    public HeaderEmitter getHeaderEmitter() {
        return this.headerEmitter;
    }
    
    public void setHeaderEmitter(final HeaderEmitter headerEmitter) {
        this.headerEmitter = headerEmitter;
    }
    
    int getFirstSlotPosition() {
        return this.firstSlotPosition;
    }
    
    Hpack.HeaderField[] getHeaderTable() {
        return this.headerTable;
    }
    
    int getFilledTableSlots() {
        return this.filledTableSlots;
    }
    
    int getCurrentMemorySize() {
        return this.currentMemorySize;
    }
    
    int getMaxMemorySize() {
        return this.maxMemorySize;
    }
    
    static {
        sm = StringManager.getManager(HpackDecoder.class);
    }
    
    public interface HeaderEmitter
    {
        void emitHeader(final String p0, final String p1, final boolean p2);
    }
}
