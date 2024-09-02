// 
// Decompiled by Procyon v0.5.36
// 

package io.vertx.core.http.impl;

import java.util.function.Function;
import io.vertx.core.http.HttpHeaders;
import java.util.Iterator;
import java.util.Map;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.Set;
import java.util.AbstractList;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http2.Http2Headers;
import io.vertx.core.MultiMap;

public class Http2HeadersAdaptor implements MultiMap
{
    private final Http2Headers headers;
    
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
    public boolean contains(final String name) {
        return this.headers.contains((Object)toLowerCase(name));
    }
    
    @Override
    public boolean contains(final String name, final String value, final boolean caseInsensitive) {
        return this.headers.contains(toLowerCase(name), (CharSequence)value, caseInsensitive);
    }
    
    @Override
    public boolean isEmpty() {
        return this.headers.isEmpty();
    }
    
    @Override
    public Set<String> names() {
        final Set<String> names = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        for (final Map.Entry<CharSequence, CharSequence> header : this.headers) {
            names.add(header.getKey().toString());
        }
        return names;
    }
    
    @Override
    public MultiMap add(final String name, final String value) {
        if (!HttpHeaders.DISABLE_HTTP_HEADERS_VALIDATION) {
            HttpUtils.validateHeader(name, value);
        }
        this.headers.add((Object)toLowerCase(name), (Object)value);
        return this;
    }
    
    @Override
    public MultiMap add(final String name, final Iterable<String> values) {
        if (!HttpHeaders.DISABLE_HTTP_HEADERS_VALIDATION) {
            HttpUtils.validateHeader(name, values);
        }
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
    public MultiMap set(String name, final String value) {
        if (!HttpHeaders.DISABLE_HTTP_HEADERS_VALIDATION) {
            HttpUtils.validateHeader(name, value);
        }
        name = (String)toLowerCase(name);
        if (value != null) {
            this.headers.set((Object)name, (Object)value);
        }
        else {
            this.headers.remove((Object)name);
        }
        return this;
    }
    
    @Override
    public MultiMap set(final String name, final Iterable<String> values) {
        if (!HttpHeaders.DISABLE_HTTP_HEADERS_VALIDATION) {
            HttpUtils.validateHeader(name, values);
        }
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
        final Iterator<Map.Entry<CharSequence, CharSequence>> i = (Iterator<Map.Entry<CharSequence, CharSequence>>)this.headers.iterator();
        return new Iterator<Map.Entry<String, String>>() {
            @Override
            public boolean hasNext() {
                return i.hasNext();
            }
            
            @Override
            public Map.Entry<String, String> next() {
                final Map.Entry<CharSequence, CharSequence> next = i.next();
                return new Map.Entry<String, String>() {
                    @Override
                    public String getKey() {
                        return next.getKey().toString();
                    }
                    
                    @Override
                    public String getValue() {
                        return next.getValue().toString();
                    }
                    
                    @Override
                    public String setValue(final String value) {
                        final String old = next.getValue().toString();
                        next.setValue(value);
                        return old;
                    }
                    
                    @Override
                    public String toString() {
                        return next.toString();
                    }
                };
            }
        };
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
    public boolean contains(final CharSequence name, final CharSequence value, final boolean caseInsensitive) {
        return this.headers.contains(toLowerCase(name), value, caseInsensitive);
    }
    
    @Override
    public MultiMap add(final CharSequence name, final CharSequence value) {
        if (!HttpHeaders.DISABLE_HTTP_HEADERS_VALIDATION) {
            HttpUtils.validateHeader(name, value);
        }
        this.headers.add((Object)toLowerCase(name), (Object)value);
        return this;
    }
    
    @Override
    public MultiMap add(final CharSequence name, final Iterable<CharSequence> values) {
        if (!HttpHeaders.DISABLE_HTTP_HEADERS_VALIDATION) {
            HttpUtils.validateHeader(name, values);
        }
        this.headers.add((Object)toLowerCase(name), (Iterable)values);
        return this;
    }
    
    @Override
    public MultiMap set(CharSequence name, final CharSequence value) {
        if (!HttpHeaders.DISABLE_HTTP_HEADERS_VALIDATION) {
            HttpUtils.validateHeader(name, value);
        }
        name = toLowerCase(name);
        if (value != null) {
            this.headers.set((Object)name, (Object)value);
        }
        else {
            this.headers.remove((Object)name);
        }
        return this;
    }
    
    @Override
    public MultiMap set(final CharSequence name, final Iterable<CharSequence> values) {
        if (!HttpHeaders.DISABLE_HTTP_HEADERS_VALIDATION) {
            HttpUtils.validateHeader(name, values);
        }
        this.headers.set((Object)toLowerCase(name), (Iterable)values);
        return this;
    }
    
    @Override
    public MultiMap remove(final CharSequence name) {
        this.headers.remove((Object)toLowerCase(name));
        return this;
    }
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        for (final Map.Entry<CharSequence, CharSequence> header : this.headers) {
            sb.append(header).append('\n');
        }
        return sb.toString();
    }
}
