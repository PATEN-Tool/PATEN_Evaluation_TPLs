// 
// Decompiled by Procyon v0.5.36
// 

package io.vertx.core.http.impl.headers;

import io.vertx.core.http.impl.HttpUtils;
import io.netty.util.CharsetUtil;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.ByteBuf;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.AbstractMap;
import java.util.function.Consumer;
import java.util.LinkedList;
import java.util.List;
import io.netty.util.HashingStrategy;
import java.util.Objects;
import java.util.Iterator;
import io.netty.util.AsciiString;
import java.util.Map;
import io.vertx.core.MultiMap;
import io.netty.handler.codec.http.HttpHeaders;

public final class VertxHttpHeaders extends HttpHeaders implements MultiMap
{
    private final MapEntry[] entries;
    private final MapEntry head;
    private static final int COLON_AND_SPACE_SHORT = 14880;
    static final int CRLF_SHORT = 3338;
    
    public MultiMap setAll(final MultiMap headers) {
        return this.set0(headers);
    }
    
    public MultiMap setAll(final Map<String, String> headers) {
        return this.set0(headers.entrySet());
    }
    
    public int size() {
        return this.names().size();
    }
    
    public VertxHttpHeaders() {
        this.entries = new MapEntry[16];
        this.head = new MapEntry();
        final MapEntry head = this.head;
        final MapEntry head2 = this.head;
        final MapEntry head3 = this.head;
        head2.after = head3;
        head.before = head3;
    }
    
    public VertxHttpHeaders add(final CharSequence name, final CharSequence value) {
        final int h = AsciiString.hashCode(name);
        final int i = h & 0xF;
        this.add0(h, i, name, value);
        return this;
    }
    
    public VertxHttpHeaders add(final CharSequence name, final Object value) {
        return this.add(name, (CharSequence)value);
    }
    
    public HttpHeaders add(final String name, final Object value) {
        return this.add((CharSequence)name, (CharSequence)value);
    }
    
    public VertxHttpHeaders add(final String name, final String strVal) {
        return this.add((CharSequence)name, strVal);
    }
    
    public VertxHttpHeaders add(final CharSequence name, final Iterable values) {
        final int h = AsciiString.hashCode(name);
        final int i = h & 0xF;
        for (final Object vstr : values) {
            this.add0(h, i, name, (CharSequence)vstr);
        }
        return this;
    }
    
    public VertxHttpHeaders add(final String name, final Iterable values) {
        return this.add((CharSequence)name, values);
    }
    
    public MultiMap addAll(final MultiMap headers) {
        return this.addAll(headers.entries());
    }
    
    public MultiMap addAll(final Map<String, String> map) {
        return this.addAll(map.entrySet());
    }
    
    private MultiMap addAll(final Iterable<Map.Entry<String, String>> headers) {
        for (final Map.Entry<String, String> entry : headers) {
            this.add(entry.getKey(), entry.getValue());
        }
        return this;
    }
    
    public VertxHttpHeaders remove(final CharSequence name) {
        Objects.requireNonNull(name, "name");
        final int h = AsciiString.hashCode(name);
        final int i = h & 0xF;
        this.remove0(h, i, name);
        return this;
    }
    
    public VertxHttpHeaders remove(final String name) {
        return this.remove((CharSequence)name);
    }
    
    public VertxHttpHeaders set(final CharSequence name, final CharSequence value) {
        return this.set0(name, value);
    }
    
    public VertxHttpHeaders set(final String name, final String value) {
        return this.set((CharSequence)name, value);
    }
    
    public VertxHttpHeaders set(final String name, final Object value) {
        return this.set((CharSequence)name, (CharSequence)value);
    }
    
    public VertxHttpHeaders set(final CharSequence name, final Object value) {
        return this.set(name, (CharSequence)value);
    }
    
    public VertxHttpHeaders set(final CharSequence name, final Iterable values) {
        Objects.requireNonNull(values, "values");
        final int h = AsciiString.hashCode(name);
        final int i = h & 0xF;
        this.remove0(h, i, name);
        for (final Object v : values) {
            if (v == null) {
                break;
            }
            this.add0(h, i, name, (CharSequence)v);
        }
        return this;
    }
    
    public VertxHttpHeaders set(final String name, final Iterable values) {
        return this.set((CharSequence)name, values);
    }
    
    public boolean contains(final CharSequence name, final CharSequence value, final boolean ignoreCase) {
        final int h = AsciiString.hashCode(name);
        final int i = h & 0xF;
        MapEntry e = this.entries[i];
        final HashingStrategy<CharSequence> strategy = (HashingStrategy<CharSequence>)(ignoreCase ? AsciiString.CASE_INSENSITIVE_HASHER : AsciiString.CASE_SENSITIVE_HASHER);
        while (e != null) {
            final CharSequence key = e.key;
            if (e.hash == h && (name == key || AsciiString.contentEqualsIgnoreCase(name, key)) && strategy.equals((Object)value, (Object)e.getValue())) {
                return true;
            }
            e = e.next;
        }
        return false;
    }
    
    public boolean contains(final String name, final String value, final boolean ignoreCase) {
        return this.contains(name, (CharSequence)value, ignoreCase);
    }
    
    public boolean contains(final CharSequence name) {
        return this.get0(name) != null;
    }
    
    public boolean contains(final String name) {
        return this.contains((CharSequence)name);
    }
    
    public String get(final CharSequence name) {
        Objects.requireNonNull(name, "name");
        final CharSequence ret = this.get0(name);
        return (ret != null) ? ret.toString() : null;
    }
    
    public String get(final String name) {
        return this.get((CharSequence)name);
    }
    
    public List<String> getAll(final CharSequence name) {
        Objects.requireNonNull(name, "name");
        final LinkedList<String> values = new LinkedList<String>();
        final int h = AsciiString.hashCode(name);
        final int i = h & 0xF;
        for (MapEntry e = this.entries[i]; e != null; e = e.next) {
            final CharSequence key = e.key;
            if (e.hash == h && (name == key || AsciiString.contentEqualsIgnoreCase(name, key))) {
                values.addFirst(e.getValue().toString());
            }
        }
        return values;
    }
    
    public List<String> getAll(final String name) {
        return this.getAll((CharSequence)name);
    }
    
    public void forEach(final Consumer<? super Map.Entry<String, String>> action) {
        for (MapEntry e = this.head.after; e != this.head; e = e.after) {
            action.accept((Object)new AbstractMap.SimpleEntry((K)e.key.toString(), (V)e.value.toString()));
        }
    }
    
    public List<Map.Entry<String, String>> entries() {
        final List<Map.Entry<String, String>> all = new ArrayList<Map.Entry<String, String>>(this.size());
        for (MapEntry e = this.head.after; e != this.head; e = e.after) {
            final MapEntry f = e;
            all.add(new Map.Entry<String, String>() {
                @Override
                public String getKey() {
                    return f.key.toString();
                }
                
                @Override
                public String getValue() {
                    return f.value.toString();
                }
                
                @Override
                public String setValue(final String value) {
                    return f.setValue((CharSequence)value).toString();
                }
                
                @Override
                public String toString() {
                    return this.getKey() + ": " + this.getValue();
                }
            });
        }
        return all;
    }
    
    public Iterator<Map.Entry<String, String>> iterator() {
        return this.entries().iterator();
    }
    
    public boolean isEmpty() {
        return this.head == this.head.after;
    }
    
    public Set<String> names() {
        final Set<String> names = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        for (MapEntry e = this.head.after; e != this.head; e = e.after) {
            names.add(e.getKey().toString());
        }
        return names;
    }
    
    public VertxHttpHeaders clear() {
        for (int i = 0; i < this.entries.length; ++i) {
            this.entries[i] = null;
        }
        final MapEntry head = this.head;
        final MapEntry head2 = this.head;
        final MapEntry head3 = this.head;
        head2.after = head3;
        head.before = head3;
        return this;
    }
    
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        for (final Map.Entry<String, String> entry : this) {
            sb.append(entry).append('\n');
        }
        return sb.toString();
    }
    
    public Integer getInt(final CharSequence name) {
        throw new UnsupportedOperationException();
    }
    
    public int getInt(final CharSequence name, final int defaultValue) {
        throw new UnsupportedOperationException();
    }
    
    public Short getShort(final CharSequence name) {
        throw new UnsupportedOperationException();
    }
    
    public short getShort(final CharSequence name, final short defaultValue) {
        throw new UnsupportedOperationException();
    }
    
    public Long getTimeMillis(final CharSequence name) {
        throw new UnsupportedOperationException();
    }
    
    public long getTimeMillis(final CharSequence name, final long defaultValue) {
        throw new UnsupportedOperationException();
    }
    
    public Iterator<Map.Entry<CharSequence, CharSequence>> iteratorCharSequence() {
        return new Iterator<Map.Entry<CharSequence, CharSequence>>() {
            MapEntry current = VertxHttpHeaders.this.head.after;
            
            @Override
            public boolean hasNext() {
                return this.current != VertxHttpHeaders.this.head;
            }
            
            @Override
            public Map.Entry<CharSequence, CharSequence> next() {
                final Map.Entry<CharSequence, CharSequence> next = this.current;
                this.current = this.current.after;
                return next;
            }
        };
    }
    
    public HttpHeaders addInt(final CharSequence name, final int value) {
        throw new UnsupportedOperationException();
    }
    
    public HttpHeaders addShort(final CharSequence name, final short value) {
        throw new UnsupportedOperationException();
    }
    
    public HttpHeaders setInt(final CharSequence name, final int value) {
        return this.set(name, Integer.toString(value));
    }
    
    public HttpHeaders setShort(final CharSequence name, final short value) {
        throw new UnsupportedOperationException();
    }
    
    public void encode(final ByteBuf buf) {
        for (MapEntry current = this.head.after; current != this.head; current = current.after) {
            encoderHeader(current.key, current.value, buf);
        }
    }
    
    static void encoderHeader(final CharSequence name, final CharSequence value, final ByteBuf buf) {
        final int nameLen = name.length();
        final int valueLen = value.length();
        final int entryLen = nameLen + valueLen + 4;
        buf.ensureWritable(entryLen);
        int offset = buf.writerIndex();
        writeAscii(buf, offset, name);
        offset += nameLen;
        ByteBufUtil.setShortBE(buf, offset, 14880);
        offset += 2;
        writeAscii(buf, offset, value);
        offset += valueLen;
        ByteBufUtil.setShortBE(buf, offset, 3338);
        offset += 2;
        buf.writerIndex(offset);
    }
    
    private static void writeAscii(final ByteBuf buf, final int offset, final CharSequence value) {
        if (value instanceof AsciiString) {
            ByteBufUtil.copy((AsciiString)value, 0, buf, offset, value.length());
        }
        else {
            buf.setCharSequence(offset, value, CharsetUtil.US_ASCII);
        }
    }
    
    private void remove0(final int h, final int i, final CharSequence name) {
        MapEntry e = this.entries[i];
        if (e == null) {
            return;
        }
        while (true) {
            final CharSequence key = e.key;
            if (e.hash != h || (name != key && !AsciiString.contentEqualsIgnoreCase(name, key))) {
                break;
            }
            e.remove();
            final MapEntry next = e.next;
            if (next == null) {
                this.entries[i] = null;
                return;
            }
            this.entries[i] = next;
            e = next;
        }
        while (true) {
            final MapEntry next2 = e.next;
            if (next2 == null) {
                break;
            }
            final CharSequence key2 = next2.key;
            if (next2.hash == h && (name == key2 || AsciiString.contentEqualsIgnoreCase(name, key2))) {
                e.next = next2.next;
                next2.remove();
            }
            else {
                e = next2;
            }
        }
    }
    
    private void add0(final int h, final int i, final CharSequence name, final CharSequence value) {
        if (!io.vertx.core.http.HttpHeaders.DISABLE_HTTP_HEADERS_VALIDATION) {
            HttpUtils.validateHeader(name, value);
        }
        final MapEntry e = this.entries[i];
        final MapEntry newEntry = this.entries[i] = new MapEntry(h, name, value);
        newEntry.next = e;
        newEntry.addBefore(this.head);
    }
    
    private VertxHttpHeaders set0(final CharSequence name, final CharSequence strVal) {
        final int h = AsciiString.hashCode(name);
        final int i = h & 0xF;
        this.remove0(h, i, name);
        if (strVal != null) {
            this.add0(h, i, name, strVal);
        }
        return this;
    }
    
    private CharSequence get0(final CharSequence name) {
        final int h = AsciiString.hashCode(name);
        final int i = h & 0xF;
        MapEntry e = this.entries[i];
        CharSequence value = null;
        while (e != null) {
            final CharSequence key = e.key;
            if (e.hash == h && (name == key || AsciiString.contentEqualsIgnoreCase(name, key))) {
                value = e.getValue();
            }
            e = e.next;
        }
        return value;
    }
    
    private MultiMap set0(final Iterable<Map.Entry<String, String>> map) {
        this.clear();
        for (final Map.Entry<String, String> entry : map) {
            this.add(entry.getKey(), entry.getValue());
        }
        return this;
    }
    
    private static final class MapEntry implements Map.Entry<CharSequence, CharSequence>
    {
        final int hash;
        final CharSequence key;
        CharSequence value;
        MapEntry next;
        MapEntry before;
        MapEntry after;
        
        MapEntry() {
            this.hash = -1;
            this.key = null;
            this.value = null;
        }
        
        MapEntry(final int hash, final CharSequence key, final CharSequence value) {
            this.hash = hash;
            this.key = key;
            this.value = value;
        }
        
        void remove() {
            this.before.after = this.after;
            this.after.before = this.before;
        }
        
        void addBefore(final MapEntry e) {
            this.after = e;
            this.before = e.before;
            this.before.after = this;
            this.after.before = this;
        }
        
        @Override
        public CharSequence getKey() {
            return this.key;
        }
        
        @Override
        public CharSequence getValue() {
            return this.value;
        }
        
        @Override
        public CharSequence setValue(final CharSequence value) {
            Objects.requireNonNull(value, "value");
            if (!io.vertx.core.http.HttpHeaders.DISABLE_HTTP_HEADERS_VALIDATION) {
                HttpUtils.validateHeaderValue(value);
            }
            final CharSequence oldValue = this.value;
            this.value = value;
            return oldValue;
        }
        
        @Override
        public String toString() {
            return (Object)this.getKey() + ": " + (Object)this.getValue();
        }
    }
}
