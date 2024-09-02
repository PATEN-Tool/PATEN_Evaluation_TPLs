// 
// Decompiled by Procyon v0.5.36
// 

package io.vertx.core.http;

import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.util.AsciiString;

public final class HttpHeaders
{
    private static final String DISABLE_HTTP_HEADERS_VALIDATION_PROP_NAME = "vertx.disableHttpHeadersValidation";
    public static final boolean DISABLE_HTTP_HEADERS_VALIDATION;
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
    public static final CharSequence CONTENT_DISPOSITION;
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
        return (CharSequence)new AsciiString((CharSequence)value);
    }
    
    private HttpHeaders() {
    }
    
    static {
        DISABLE_HTTP_HEADERS_VALIDATION = Boolean.getBoolean("vertx.disableHttpHeadersValidation");
        ACCEPT = (CharSequence)HttpHeaderNames.ACCEPT;
        ACCEPT_CHARSET = (CharSequence)HttpHeaderNames.ACCEPT_CHARSET;
        ACCEPT_ENCODING = (CharSequence)HttpHeaderNames.ACCEPT_ENCODING;
        ACCEPT_LANGUAGE = (CharSequence)HttpHeaderNames.ACCEPT_LANGUAGE;
        ACCEPT_RANGES = (CharSequence)HttpHeaderNames.ACCEPT_RANGES;
        ACCEPT_PATCH = (CharSequence)HttpHeaderNames.ACCEPT_PATCH;
        ACCESS_CONTROL_ALLOW_CREDENTIALS = (CharSequence)HttpHeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS;
        ACCESS_CONTROL_ALLOW_HEADERS = (CharSequence)HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS;
        ACCESS_CONTROL_ALLOW_METHODS = (CharSequence)HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS;
        ACCESS_CONTROL_ALLOW_ORIGIN = (CharSequence)HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN;
        ACCESS_CONTROL_EXPOSE_HEADERS = (CharSequence)HttpHeaderNames.ACCESS_CONTROL_EXPOSE_HEADERS;
        ACCESS_CONTROL_MAX_AGE = (CharSequence)HttpHeaderNames.ACCESS_CONTROL_MAX_AGE;
        ACCESS_CONTROL_REQUEST_HEADERS = (CharSequence)HttpHeaderNames.ACCESS_CONTROL_REQUEST_HEADERS;
        ACCESS_CONTROL_REQUEST_METHOD = (CharSequence)HttpHeaderNames.ACCESS_CONTROL_REQUEST_METHOD;
        AGE = (CharSequence)HttpHeaderNames.AGE;
        ALLOW = (CharSequence)HttpHeaderNames.ALLOW;
        AUTHORIZATION = (CharSequence)HttpHeaderNames.AUTHORIZATION;
        CACHE_CONTROL = (CharSequence)HttpHeaderNames.CACHE_CONTROL;
        CONNECTION = (CharSequence)HttpHeaderNames.CONNECTION;
        CONTENT_BASE = (CharSequence)HttpHeaderNames.CONTENT_BASE;
        CONTENT_DISPOSITION = (CharSequence)HttpHeaderNames.CONTENT_DISPOSITION;
        CONTENT_ENCODING = (CharSequence)HttpHeaderNames.CONTENT_ENCODING;
        CONTENT_LANGUAGE = (CharSequence)HttpHeaderNames.CONTENT_LANGUAGE;
        CONTENT_LENGTH = (CharSequence)HttpHeaderNames.CONTENT_LENGTH;
        CONTENT_LOCATION = (CharSequence)HttpHeaderNames.CONTENT_LOCATION;
        CONTENT_TRANSFER_ENCODING = (CharSequence)HttpHeaderNames.CONTENT_TRANSFER_ENCODING;
        CONTENT_MD5 = (CharSequence)HttpHeaderNames.CONTENT_MD5;
        CONTENT_RANGE = (CharSequence)HttpHeaderNames.CONTENT_RANGE;
        CONTENT_TYPE = (CharSequence)HttpHeaderNames.CONTENT_TYPE;
        COOKIE = (CharSequence)HttpHeaderNames.COOKIE;
        DATE = (CharSequence)HttpHeaderNames.DATE;
        ETAG = (CharSequence)HttpHeaderNames.ETAG;
        EXPECT = (CharSequence)HttpHeaderNames.EXPECT;
        EXPIRES = (CharSequence)HttpHeaderNames.EXPIRES;
        FROM = (CharSequence)HttpHeaderNames.FROM;
        HOST = (CharSequence)HttpHeaderNames.HOST;
        IF_MATCH = (CharSequence)HttpHeaderNames.IF_MATCH;
        IF_MODIFIED_SINCE = (CharSequence)HttpHeaderNames.IF_MODIFIED_SINCE;
        IF_NONE_MATCH = (CharSequence)HttpHeaderNames.IF_NONE_MATCH;
        LAST_MODIFIED = (CharSequence)HttpHeaderNames.LAST_MODIFIED;
        LOCATION = (CharSequence)HttpHeaderNames.LOCATION;
        ORIGIN = (CharSequence)HttpHeaderNames.ORIGIN;
        PROXY_AUTHENTICATE = (CharSequence)HttpHeaderNames.PROXY_AUTHENTICATE;
        PROXY_AUTHORIZATION = (CharSequence)HttpHeaderNames.PROXY_AUTHORIZATION;
        REFERER = (CharSequence)HttpHeaderNames.REFERER;
        RETRY_AFTER = (CharSequence)HttpHeaderNames.RETRY_AFTER;
        SERVER = (CharSequence)HttpHeaderNames.SERVER;
        TRANSFER_ENCODING = (CharSequence)HttpHeaderNames.TRANSFER_ENCODING;
        USER_AGENT = (CharSequence)HttpHeaderNames.USER_AGENT;
        SET_COOKIE = (CharSequence)HttpHeaderNames.SET_COOKIE;
        APPLICATION_X_WWW_FORM_URLENCODED = (CharSequence)HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED;
        CHUNKED = (CharSequence)HttpHeaderValues.CHUNKED;
        CLOSE = (CharSequence)HttpHeaderValues.CLOSE;
        CONTINUE = (CharSequence)HttpHeaderValues.CONTINUE;
        IDENTITY = (CharSequence)HttpHeaderValues.IDENTITY;
        KEEP_ALIVE = (CharSequence)HttpHeaderValues.KEEP_ALIVE;
        UPGRADE = (CharSequence)HttpHeaderValues.UPGRADE;
        WEBSOCKET = (CharSequence)HttpHeaderValues.WEBSOCKET;
        DEFLATE_GZIP = createOptimized("deflate, gzip");
        TEXT_HTML = createOptimized("text/html");
        GET = createOptimized("GET");
    }
}
