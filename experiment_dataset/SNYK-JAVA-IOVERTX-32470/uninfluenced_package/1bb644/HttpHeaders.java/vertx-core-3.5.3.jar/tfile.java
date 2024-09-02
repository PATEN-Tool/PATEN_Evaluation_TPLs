// 
// Decompiled by Procyon v0.5.36
// 

package io.vertx.core.http;

import io.netty.util.AsciiString;
import io.vertx.core.http.impl.HttpUtils;

public final class HttpHeaders
{
    public static final CharSequence ACCEPT;
    public static final CharSequence ACCEPT_CHARSET;
    public static final CharSequence ACCEPT_ENCODING;
    public static final CharSequence ACCEPT_LANGUAGE;
    public static final CharSequence ACCEPT_RANGES;
    public static final CharSequence ACCEPT_PATCH;
    public static final CharSequence ACCESS_CONTROL_ALLOW_CREDENTIALS;
    public static final CharSequence ACCESS_CONTROL_ALLOW_HEADERS;
    public static final CharSequence ACCESS_CONTROL_ALLOW_METHODS;
    public static final CharSequence ACCESS_CONTROL_ALLOW_ORIGIN;
    public static final CharSequence ACCESS_CONTROL_EXPOSE_HEADERS;
    public static final CharSequence ACCESS_CONTROL_MAX_AGE;
    public static final CharSequence ACCESS_CONTROL_REQUEST_HEADERS;
    public static final CharSequence ACCESS_CONTROL_REQUEST_METHOD;
    public static final CharSequence AGE;
    public static final CharSequence ALLOW;
    public static final CharSequence AUTHORIZATION;
    public static final CharSequence CACHE_CONTROL;
    public static final CharSequence CONNECTION;
    public static final CharSequence CONTENT_BASE;
    public static final CharSequence CONTENT_ENCODING;
    public static final CharSequence CONTENT_LANGUAGE;
    public static final CharSequence CONTENT_LENGTH;
    public static final CharSequence CONTENT_LOCATION;
    public static final CharSequence CONTENT_TRANSFER_ENCODING;
    public static final CharSequence CONTENT_MD5;
    public static final CharSequence CONTENT_RANGE;
    public static final CharSequence CONTENT_TYPE;
    public static final CharSequence COOKIE;
    public static final CharSequence DATE;
    public static final CharSequence ETAG;
    public static final CharSequence EXPECT;
    public static final CharSequence EXPIRES;
    public static final CharSequence FROM;
    public static final CharSequence HOST;
    public static final CharSequence IF_MATCH;
    public static final CharSequence IF_MODIFIED_SINCE;
    public static final CharSequence IF_NONE_MATCH;
    public static final CharSequence LAST_MODIFIED;
    public static final CharSequence LOCATION;
    public static final CharSequence ORIGIN;
    public static final CharSequence PROXY_AUTHENTICATE;
    public static final CharSequence PROXY_AUTHORIZATION;
    public static final CharSequence REFERER;
    public static final CharSequence RETRY_AFTER;
    public static final CharSequence SERVER;
    public static final CharSequence TRANSFER_ENCODING;
    public static final CharSequence USER_AGENT;
    public static final CharSequence SET_COOKIE;
    public static final CharSequence APPLICATION_X_WWW_FORM_URLENCODED;
    public static final CharSequence CHUNKED;
    public static final CharSequence CLOSE;
    public static final CharSequence CONTINUE;
    public static final CharSequence IDENTITY;
    public static final CharSequence KEEP_ALIVE;
    public static final CharSequence UPGRADE;
    public static final CharSequence WEBSOCKET;
    public static final CharSequence DEFLATE_GZIP;
    public static final CharSequence TEXT_HTML;
    public static final CharSequence GET;
    
    public static CharSequence createOptimized(final String value) {
        HttpUtils.validateHeader(value);
        return (CharSequence)new AsciiString((CharSequence)value);
    }
    
    private HttpHeaders() {
    }
    
    static {
        ACCEPT = createOptimized("Accept");
        ACCEPT_CHARSET = createOptimized("Accept-Charset");
        ACCEPT_ENCODING = createOptimized("Accept-Encoding");
        ACCEPT_LANGUAGE = createOptimized("Accept-Language");
        ACCEPT_RANGES = createOptimized("Accept-Ranges");
        ACCEPT_PATCH = createOptimized("Accept-Patch");
        ACCESS_CONTROL_ALLOW_CREDENTIALS = createOptimized("Access-Control-Allow-Credentials");
        ACCESS_CONTROL_ALLOW_HEADERS = createOptimized("Access-Control-Allow-Headers");
        ACCESS_CONTROL_ALLOW_METHODS = createOptimized("Access-Control-Allow-Methods");
        ACCESS_CONTROL_ALLOW_ORIGIN = createOptimized("Access-Control-Allow-Origin");
        ACCESS_CONTROL_EXPOSE_HEADERS = createOptimized("Access-Control-Expose-Headers");
        ACCESS_CONTROL_MAX_AGE = createOptimized("Access-Control-Max-Age");
        ACCESS_CONTROL_REQUEST_HEADERS = createOptimized("Access-Control-Request-Headers");
        ACCESS_CONTROL_REQUEST_METHOD = createOptimized("Access-Control-Request-Method");
        AGE = createOptimized("Age");
        ALLOW = createOptimized("Allow");
        AUTHORIZATION = createOptimized("Authorization");
        CACHE_CONTROL = createOptimized("Cache-Control");
        CONNECTION = createOptimized("Connection");
        CONTENT_BASE = createOptimized("Content-Base");
        CONTENT_ENCODING = createOptimized("Content-Encoding");
        CONTENT_LANGUAGE = createOptimized("Content-Language");
        CONTENT_LENGTH = createOptimized("Content-Length");
        CONTENT_LOCATION = createOptimized("Content-Location");
        CONTENT_TRANSFER_ENCODING = createOptimized("Content-Transfer-Encoding");
        CONTENT_MD5 = createOptimized("Content-MD5");
        CONTENT_RANGE = createOptimized("Content-Range");
        CONTENT_TYPE = createOptimized("Content-Type");
        COOKIE = createOptimized("Cookie");
        DATE = createOptimized("Date");
        ETAG = createOptimized("ETag");
        EXPECT = createOptimized("Expect");
        EXPIRES = createOptimized("Expires");
        FROM = createOptimized("From");
        HOST = createOptimized("Host");
        IF_MATCH = createOptimized("If-Match");
        IF_MODIFIED_SINCE = createOptimized("If-Modified-Since");
        IF_NONE_MATCH = createOptimized("If-None-Match");
        LAST_MODIFIED = createOptimized("Last-Modified");
        LOCATION = createOptimized("Location");
        ORIGIN = createOptimized("Origin");
        PROXY_AUTHENTICATE = createOptimized("Proxy-Authenticate");
        PROXY_AUTHORIZATION = createOptimized("Proxy-Authorization");
        REFERER = createOptimized("Referer");
        RETRY_AFTER = createOptimized("Retry-After");
        SERVER = createOptimized("Server");
        TRANSFER_ENCODING = createOptimized("Transfer-Encoding");
        USER_AGENT = createOptimized("User-Agent");
        SET_COOKIE = createOptimized("Set-Cookie");
        APPLICATION_X_WWW_FORM_URLENCODED = createOptimized("application/x-www-form-urlencoded");
        CHUNKED = createOptimized("chunked");
        CLOSE = createOptimized("close");
        CONTINUE = createOptimized("100-continue");
        IDENTITY = createOptimized("identity");
        KEEP_ALIVE = createOptimized("keep-alive");
        UPGRADE = createOptimized("Upgrade");
        WEBSOCKET = createOptimized("WebSocket");
        DEFLATE_GZIP = createOptimized("deflate, gzip");
        TEXT_HTML = createOptimized("text/html");
        GET = createOptimized("GET");
    }
}
