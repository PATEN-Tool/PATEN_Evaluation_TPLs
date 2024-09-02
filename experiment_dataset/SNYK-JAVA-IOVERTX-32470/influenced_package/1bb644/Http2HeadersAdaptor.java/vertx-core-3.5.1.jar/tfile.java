// 
// Decompiled by Procyon v0.5.36
// 

package io.vertx.core.http.impl;

import java.util.function.Function;
import java.util.Iterator;
import java.util.AbstractSet;
import java.util.AbstractMap;
import java.util.Map;
import java.util.AbstractList;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import io.netty.handler.codec.http.HttpHeaderNames;
import java.util.Set;
import io.netty.handler.codec.http2.Http2Headers;
import io.vertx.core.MultiMap;

public class Http2HeadersAdaptor implements MultiMap
{
    private final Http2Headers headers;
    private Set<String> names;
    
    static CharSequence toLowerCase(final CharSequence s) {
        StringBuilder buffer = null;
        for (int len = s.length(), index = 0; index < len; ++index) {
            final char c = s.charAt(index);
            if (c >= 'A' && c <= 'Z') {
                if (buffer == null) {
                    buffer = new StringBuilder(s);
                }
                buffer.setCharAt(index, (char)(c + ' '));
            }
        }
        if (buffer != null) {
            return buffer.toString();
        }
        return s;
    }
    
    public Http2HeadersAdaptor(final Http2Headers headers) {
        final List<CharSequence> cookies = (List<CharSequence>)headers.getAll((Object)HttpHeaderNames.COOKIE);
        if (cookies != null && cookies.size() > 1) {
            final String value = cookies.stream().collect((Collector<? super Object, ?, String>)Collectors.joining("; "));
            headers.set((Object)HttpHeaderNames.COOKIE, (Object)value);
        }
        this.headers = headers;
    }
    
    @Override
    public String get(final String name) {
        final CharSequence val = (CharSequence)this.headers.get((Object)toLowerCase(name));
        return (val != null) ? val.toString() : null;
    }
    
    @Override
    public List<String> getAll(final String name) {
        final List<CharSequence> all = (List<CharSequence>)this.headers.getAll((Object)toLowerCase(name));
        if (all != null) {
            return new AbstractList<String>() {
                @Override
                public String get(final int index) {
                    return all.get(index).toString();
                }
                
                @Override
                public int size() {
                    return all.size();
                }
            };
        }
        return null;
    }
    
    @Override
    public List<Map.Entry<String, String>> entries() {
        return (List<Map.Entry<String, String>>)this.headers.names().stream().map(name -> new AbstractMap.SimpleEntry(name.toString(), ((CharSequence)this.headers.get((Object)name)).toString())).collect(Collectors.toList());
    }
    
    @Override
    public boolean contains(final String name) {
        return this.headers.contains((Object)toLowerCase(name));
    }
    
    @Override
    public boolean isEmpty() {
        return this.headers.isEmpty();
    }
    
    @Override
    public Set<String> names() {
        if (this.names == null) {
            this.names = new AbstractSet<String>() {
                @Override
                public Iterator<String> iterator() {
                    final Iterator<CharSequence> it = Http2HeadersAdaptor.this.headers.names().iterator();
                    return new Iterator<String>() {
                        @Override
                        public boolean hasNext() {
                            return it.hasNext();
                        }
                        
                        @Override
                        public String next() {
                            return it.next().toString();
                        }
                    };
                }
                
                @Override
                public int size() {
                    return Http2HeadersAdaptor.this.headers.size();
                }
            };
        }
        return this.names;
    }
    
    @Override
    public MultiMap add(final String name, final String value) {
        this.headers.add((Object)toLowerCase(name), (Object)value);
        return this;
    }
    
    @Override
    public MultiMap add(final String name, final Iterable<String> values) {
        this.headers.add((Object)toLowerCase(name), (Iterable)values);
        return this;
    }
    
    @Override
    public MultiMap addAll(final MultiMap headers) {
        for (final Map.Entry<String, String> entry : headers.entries()) {
            this.add(entry.getKey(), entry.getValue());
        }
        return this;
    }
    
    @Override
    public MultiMap addAll(final Map<String, String> map) {
        for (final Map.Entry<String, String> entry : map.entrySet()) {
            this.add(entry.getKey(), entry.getValue());
        }
        return this;
    }
    
    @Override
    public MultiMap set(final String name, final String value) {
        this.headers.set((Object)toLowerCase(name), (Object)value);
        return this;
    }
    
    @Override
    public MultiMap set(final String name, final Iterable<String> values) {
        this.headers.set((Object)toLowerCase(name), (Iterable)values);
        return this;
    }
    
    @Override
    public MultiMap setAll(final MultiMap httpHeaders) {
        this.clear();
        for (final Map.Entry<String, String> entry : httpHeaders) {
            this.add(entry.getKey(), entry.getValue());
        }
        return this;
    }
    
    @Override
    public MultiMap remove(final String name) {
        this.headers.remove((Object)toLowerCase(name));
        return this;
    }
    
    @Override
    public MultiMap clear() {
        this.headers.clear();
        return this;
    }
    
    @Override
    public Iterator<Map.Entry<String, String>> iterator() {
        return this.entries().iterator();
    }
    
    @Override
    public int size() {
        return this.names().size();
    }
    
    @Override
    public MultiMap setAll(final Map<String, String> headers) {
        for (final Map.Entry<String, String> entry : headers.entrySet()) {
            this.add(entry.getKey(), entry.getValue());
        }
        return this;
    }
    
    @Override
    public String get(final CharSequence name) {
        final CharSequence val = (CharSequence)this.headers.get((Object)toLowerCase(name));
        return (val != null) ? val.toString() : null;
    }
    
    @Override
    public List<String> getAll(final CharSequence name) {
        final List<CharSequence> all = (List<CharSequence>)this.headers.getAll((Object)toLowerCase(name));
        return (List<String>)((all != null) ? all.stream().map((Function<? super Object, ?>)CharSequence::toString).collect((Collector<? super Object, ?, List<? super Object>>)Collectors.toList()) : null);
    }
    
    @Override
    public boolean contains(final CharSequence name) {
        return this.headers.contains((Object)toLowerCase(name));
    }
    
    @Override
    public MultiMap add(final CharSequence name, final CharSequence value) {
        this.headers.add((Object)toLowerCase(name), (Object)value);
        return this;
    }
    
    @Override
    public MultiMap add(final CharSequence name, final Iterable<CharSequence> values) {
        this.headers.add((Object)toLowerCase(name), (Iterable)values);
        return this;
    }
    
    @Override
    public MultiMap set(final CharSequence name, final CharSequence value) {
        this.headers.set((Object)toLowerCase(name), (Object)value);
        return this;
    }
    
    @Override
    public MultiMap set(final CharSequence name, final Iterable<CharSequence> values) {
        this.headers.set((Object)toLowerCase(name), (Iterable)values);
        return this;
    }
    
    @Override
    public MultiMap remove(final CharSequence name) {
        this.headers.remove((Object)toLowerCase(name));
        return this;
    }
}
