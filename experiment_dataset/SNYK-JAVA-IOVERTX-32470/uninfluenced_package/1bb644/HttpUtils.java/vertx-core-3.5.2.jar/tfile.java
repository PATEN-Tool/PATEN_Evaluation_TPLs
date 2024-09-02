// 
// Decompiled by Procyon v0.5.36
// 

package io.vertx.core.http.impl;

import io.netty.handler.codec.http.HttpContentCompressor;
import java.util.function.Consumer;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.HttpMethod;
import io.netty.handler.codec.compression.ZlibWrapper;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http2.Http2Headers;
import java.nio.charset.Charset;
import io.netty.buffer.Unpooled;
import io.netty.buffer.ByteBuf;
import io.vertx.core.buffer.Buffer;
import java.util.Base64;
import io.vertx.core.http.Http2Settings;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import io.vertx.core.http.CaseInsensitiveHeaders;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import java.net.URISyntaxException;
import java.net.URI;
import io.netty.util.AsciiString;

public final class HttpUtils
{
    private static final CustomCompressor compressor;
    private static final AsciiString TIMEOUT_EQ;
    
    private HttpUtils() {
    }
    
    private static int indexOfSlash(final CharSequence str, final int start) {
        for (int i = start; i < str.length(); ++i) {
            if (str.charAt(i) == '/') {
                return i;
            }
        }
        return -1;
    }
    
    private static boolean matches(final CharSequence path, final int start, final String what) {
        return matches(path, start, what, false);
    }
    
    private static boolean matches(final CharSequence path, final int start, final String what, final boolean exact) {
        if (exact && path.length() - start != what.length()) {
            return false;
        }
        if (path.length() - start >= what.length()) {
            for (int i = 0; i < what.length(); ++i) {
                if (path.charAt(start + i) != what.charAt(i)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
    
    public static String removeDots(CharSequence path) {
        if (path == null) {
            return null;
        }
        final StringBuilder obuf = new StringBuilder(path.length());
        int i = 0;
        while (i < path.length()) {
            if (matches(path, i, "./")) {
                i += 2;
            }
            else if (matches(path, i, "../")) {
                i += 3;
            }
            else if (matches(path, i, "/./")) {
                i += 2;
            }
            else if (matches(path, i, "/.", true)) {
                path = "/";
                i = 0;
            }
            else if (matches(path, i, "/../")) {
                i += 3;
                final int pos = obuf.lastIndexOf("/");
                if (pos == -1) {
                    continue;
                }
                obuf.delete(pos, obuf.length());
            }
            else if (matches(path, i, "/..", true)) {
                path = "/";
                i = 0;
                final int pos = obuf.lastIndexOf("/");
                if (pos == -1) {
                    continue;
                }
                obuf.delete(pos, obuf.length());
            }
            else {
                if (matches(path, i, ".", true)) {
                    break;
                }
                if (matches(path, i, "..", true)) {
                    break;
                }
                if (path.charAt(i) == '/') {
                    ++i;
                    if (obuf.length() == 0 || obuf.charAt(obuf.length() - 1) != '/') {
                        obuf.append('/');
                    }
                }
                final int pos = indexOfSlash(path, i);
                if (pos == -1) {
                    obuf.append(path, i, path.length());
                    break;
                }
                obuf.append(path, i, pos);
                i = pos;
            }
        }
        return obuf.toString();
    }
    
    public static URI resolveURIReference(final String base, final String ref) throws URISyntaxException {
        return resolveURIReference(URI.create(base), ref);
    }
    
    public static URI resolveURIReference(final URI base, final String ref) throws URISyntaxException {
        final URI _ref = URI.create(ref);
        String scheme;
        String authority;
        String path;
        String query;
        if (_ref.getScheme() != null) {
            scheme = _ref.getScheme();
            authority = _ref.getAuthority();
            path = removeDots(_ref.getPath());
            query = _ref.getRawQuery();
        }
        else {
            if (_ref.getAuthority() != null) {
                authority = _ref.getAuthority();
                path = _ref.getPath();
                query = _ref.getRawQuery();
            }
            else {
                if (_ref.getPath().length() == 0) {
                    path = base.getPath();
                    if (_ref.getRawQuery() != null) {
                        query = _ref.getRawQuery();
                    }
                    else {
                        query = base.getRawQuery();
                    }
                }
                else {
                    if (_ref.getPath().startsWith("/")) {
                        path = removeDots(_ref.getPath());
                    }
                    else {
                        final String basePath = base.getPath();
                        String mergedPath;
                        if (base.getAuthority() != null && basePath.length() == 0) {
                            mergedPath = "/" + _ref.getPath();
                        }
                        else {
                            final int index = basePath.lastIndexOf(47);
                            if (index > -1) {
                                mergedPath = basePath.substring(0, index + 1) + _ref.getPath();
                            }
                            else {
                                mergedPath = _ref.getPath();
                            }
                        }
                        path = removeDots(mergedPath);
                    }
                    query = _ref.getRawQuery();
                }
                authority = base.getAuthority();
            }
            scheme = base.getScheme();
        }
        return new URI(scheme, authority, path, query, _ref.getFragment());
    }
    
    static String parsePath(final String uri) {
        int i;
        if (uri.charAt(0) == '/') {
            i = 0;
        }
        else {
            i = uri.indexOf("://");
            if (i == -1) {
                i = 0;
            }
            else {
                i = uri.indexOf(47, i + 3);
                if (i == -1) {
                    return "/";
                }
            }
        }
        int queryStart = uri.indexOf(63, i);
        if (queryStart == -1) {
            queryStart = uri.length();
        }
        return uri.substring(i, queryStart);
    }
    
    static String parseQuery(final String uri) {
        final int i = uri.indexOf(63);
        if (i == -1) {
            return null;
        }
        return uri.substring(i + 1, uri.length());
    }
    
    static String absoluteURI(final String serverOrigin, final HttpServerRequest req) throws URISyntaxException {
        final URI uri = new URI(req.uri());
        final String scheme = uri.getScheme();
        String absoluteURI;
        if (scheme != null && (scheme.equals("http") || scheme.equals("https"))) {
            absoluteURI = uri.toString();
        }
        else {
            final String host = req.host();
            if (host != null) {
                absoluteURI = req.scheme() + "://" + host + uri;
            }
            else {
                absoluteURI = serverOrigin + uri;
            }
        }
        return absoluteURI;
    }
    
    static MultiMap params(final String uri) {
        final QueryStringDecoder queryStringDecoder = new QueryStringDecoder(uri);
        final Map<String, List<String>> prms = (Map<String, List<String>>)queryStringDecoder.parameters();
        final MultiMap params = new CaseInsensitiveHeaders();
        if (!prms.isEmpty()) {
            for (final Map.Entry<String, List<String>> entry : prms.entrySet()) {
                params.add(entry.getKey(), entry.getValue());
            }
        }
        return params;
    }
    
    public static void fromVertxInitialSettings(final boolean server, final Http2Settings vertxSettings, final io.netty.handler.codec.http2.Http2Settings nettySettings) {
        if (vertxSettings != null) {
            if (!server && !vertxSettings.isPushEnabled()) {
                nettySettings.pushEnabled(vertxSettings.isPushEnabled());
            }
            if (vertxSettings.getHeaderTableSize() != 4096L) {
                nettySettings.put('\u0001', Long.valueOf(vertxSettings.getHeaderTableSize()));
            }
            if (vertxSettings.getInitialWindowSize() != 65535) {
                nettySettings.initialWindowSize(vertxSettings.getInitialWindowSize());
            }
            if (vertxSettings.getMaxConcurrentStreams() != 4294967295L) {
                nettySettings.maxConcurrentStreams(vertxSettings.getMaxConcurrentStreams());
            }
            if (vertxSettings.getMaxFrameSize() != 16384) {
                nettySettings.maxFrameSize(vertxSettings.getMaxFrameSize());
            }
            if (vertxSettings.getMaxHeaderListSize() != 2147483647L) {
                nettySettings.maxHeaderListSize(vertxSettings.getMaxHeaderListSize());
            }
            final Map<Integer, Long> extraSettings = vertxSettings.getExtraSettings();
            if (extraSettings != null) {
                extraSettings.forEach((code, setting) -> nettySettings.put((char)(int)code, setting));
            }
        }
    }
    
    public static io.netty.handler.codec.http2.Http2Settings fromVertxSettings(final Http2Settings settings) {
        final io.netty.handler.codec.http2.Http2Settings converted = new io.netty.handler.codec.http2.Http2Settings();
        converted.pushEnabled(settings.isPushEnabled());
        converted.maxFrameSize(settings.getMaxFrameSize());
        converted.initialWindowSize(settings.getInitialWindowSize());
        converted.headerTableSize(settings.getHeaderTableSize());
        converted.maxConcurrentStreams(settings.getMaxConcurrentStreams());
        converted.maxHeaderListSize(settings.getMaxHeaderListSize());
        if (settings.getExtraSettings() != null) {
            settings.getExtraSettings().forEach((key, value) -> converted.put((char)(int)key, value));
        }
        return converted;
    }
    
    public static Http2Settings toVertxSettings(final io.netty.handler.codec.http2.Http2Settings settings) {
        final Http2Settings converted = new Http2Settings();
        final Boolean pushEnabled = settings.pushEnabled();
        if (pushEnabled != null) {
            converted.setPushEnabled(pushEnabled);
        }
        final Long maxConcurrentStreams = settings.maxConcurrentStreams();
        if (maxConcurrentStreams != null) {
            converted.setMaxConcurrentStreams(maxConcurrentStreams);
        }
        final Long maxHeaderListSize = settings.maxHeaderListSize();
        if (maxHeaderListSize != null) {
            converted.setMaxHeaderListSize(maxHeaderListSize);
        }
        final Integer maxFrameSize = settings.maxFrameSize();
        if (maxFrameSize != null) {
            converted.setMaxFrameSize(maxFrameSize);
        }
        final Integer initialWindowSize = settings.initialWindowSize();
        if (initialWindowSize != null) {
            converted.setInitialWindowSize(initialWindowSize);
        }
        final Long headerTableSize = settings.headerTableSize();
        if (headerTableSize != null) {
            converted.setHeaderTableSize(headerTableSize);
        }
        final Http2Settings http2Settings;
        settings.forEach((key, value) -> {
            if (key > '\u0006') {
                http2Settings.set(key, value);
            }
            return;
        });
        return converted;
    }
    
    static io.netty.handler.codec.http2.Http2Settings decodeSettings(final String base64Settings) {
        try {
            final io.netty.handler.codec.http2.Http2Settings settings = new io.netty.handler.codec.http2.Http2Settings();
            final Buffer buffer = Buffer.buffer(Base64.getUrlDecoder().decode(base64Settings));
            int pos = 0;
            final int len = buffer.length();
            while (pos < len) {
                final int i = buffer.getUnsignedShort(pos);
                pos += 2;
                final long j = buffer.getUnsignedInt(pos);
                pos += 4;
                settings.put((char)i, Long.valueOf(j));
            }
            return settings;
        }
        catch (Exception ex) {
            return null;
        }
    }
    
    public static String encodeSettings(final Http2Settings settings) {
        final Buffer buffer = Buffer.buffer();
        final Buffer buffer2;
        fromVertxSettings(settings).forEach((c, l) -> {
            buffer2.appendUnsignedShort(c);
            buffer2.appendUnsignedInt(l);
            return;
        });
        return Base64.getUrlEncoder().encodeToString(buffer.getBytes());
    }
    
    public static ByteBuf generateWSCloseFrameByteBuf(final short statusCode, final String reason) {
        if (reason != null) {
            return Unpooled.copiedBuffer(new ByteBuf[] { Unpooled.copyShort((int)statusCode), Unpooled.copiedBuffer((CharSequence)reason, Charset.forName("UTF-8")) });
        }
        return Unpooled.copyShort((int)statusCode);
    }
    
    static String determineContentEncoding(final Http2Headers headers) {
        final String acceptEncoding = (headers.get((Object)HttpHeaderNames.ACCEPT_ENCODING) != null) ? ((CharSequence)headers.get((Object)HttpHeaderNames.ACCEPT_ENCODING)).toString() : null;
        if (acceptEncoding != null) {
            final ZlibWrapper wrapper = HttpUtils.compressor.determineWrapper(acceptEncoding);
            if (wrapper != null) {
                switch (wrapper) {
                    case GZIP: {
                        return "gzip";
                    }
                    case ZLIB: {
                        return "deflate";
                    }
                }
            }
        }
        return null;
    }
    
    static io.netty.handler.codec.http.HttpMethod toNettyHttpMethod(final HttpMethod method, final String rawMethod) {
        switch (method) {
            case CONNECT: {
                return io.netty.handler.codec.http.HttpMethod.CONNECT;
            }
            case GET: {
                return io.netty.handler.codec.http.HttpMethod.GET;
            }
            case PUT: {
                return io.netty.handler.codec.http.HttpMethod.PUT;
            }
            case POST: {
                return io.netty.handler.codec.http.HttpMethod.POST;
            }
            case DELETE: {
                return io.netty.handler.codec.http.HttpMethod.DELETE;
            }
            case HEAD: {
                return io.netty.handler.codec.http.HttpMethod.HEAD;
            }
            case OPTIONS: {
                return io.netty.handler.codec.http.HttpMethod.OPTIONS;
            }
            case TRACE: {
                return io.netty.handler.codec.http.HttpMethod.TRACE;
            }
            case PATCH: {
                return io.netty.handler.codec.http.HttpMethod.PATCH;
            }
            default: {
                return io.netty.handler.codec.http.HttpMethod.valueOf(rawMethod);
            }
        }
    }
    
    static io.netty.handler.codec.http.HttpVersion toNettyHttpVersion(final HttpVersion version) {
        switch (version) {
            case HTTP_1_0: {
                return io.netty.handler.codec.http.HttpVersion.HTTP_1_0;
            }
            case HTTP_1_1: {
                return io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
            }
            default: {
                throw new IllegalArgumentException("Unsupported HTTP version: " + version);
            }
        }
    }
    
    static HttpVersion toVertxHttpVersion(final io.netty.handler.codec.http.HttpVersion version) {
        if (version == io.netty.handler.codec.http.HttpVersion.HTTP_1_0) {
            return HttpVersion.HTTP_1_0;
        }
        if (version == io.netty.handler.codec.http.HttpVersion.HTTP_1_1) {
            return HttpVersion.HTTP_1_1;
        }
        return null;
    }
    
    static HttpMethod toVertxMethod(final String method) {
        try {
            return HttpMethod.valueOf(method);
        }
        catch (IllegalArgumentException e) {
            return HttpMethod.OTHER;
        }
    }
    
    public static int parseKeepAliveHeaderTimeout(final CharSequence value) {
        int next;
        for (int len = value.length(), pos = 0; pos < len; pos = next) {
            int idx = AsciiString.indexOf(value, ',', pos);
            if (idx == -1) {
                next = (idx = len);
            }
            else {
                next = idx + 1;
            }
            while (pos < idx && value.charAt(pos) == ' ') {
                ++pos;
            }
            int to;
            for (to = idx; to > pos && value.charAt(to - 1) == ' '; --to) {}
            if (AsciiString.regionMatches(value, true, pos, (CharSequence)HttpUtils.TIMEOUT_EQ, 0, HttpUtils.TIMEOUT_EQ.length())) {
                pos += HttpUtils.TIMEOUT_EQ.length();
                if (pos < to) {
                    int ret = 0;
                    while (pos < to) {
                        final int ch = value.charAt(pos++);
                        if (ch < 48 || ch >= 57) {
                            ret = -1;
                            break;
                        }
                        ret = ret * 10 + (ch - 48);
                    }
                    if (ret > -1) {
                        return ret;
                    }
                }
            }
        }
        return -1;
    }
    
    public static void validateHeader(final CharSequence name, final CharSequence value) {
        validateHeader(name);
        validateHeader(value);
    }
    
    public static void validateHeader(final CharSequence name, final Iterable<? extends CharSequence> values) {
        validateHeader(name);
        values.forEach(HttpUtils::validateHeader);
    }
    
    public static void validateHeader(final CharSequence value) {
        for (int i = 0; i < value.length(); ++i) {
            final char c = value.charAt(i);
            if (c == '\r' || c == '\n') {
                throw new IllegalArgumentException("Illegal header character: " + (int)c);
            }
        }
    }
    
    static {
        compressor = new CustomCompressor();
        TIMEOUT_EQ = AsciiString.of((CharSequence)"timeout=");
    }
    
    private static class CustomCompressor extends HttpContentCompressor
    {
        public ZlibWrapper determineWrapper(final String acceptEncoding) {
            return super.determineWrapper(acceptEncoding);
        }
    }
}
