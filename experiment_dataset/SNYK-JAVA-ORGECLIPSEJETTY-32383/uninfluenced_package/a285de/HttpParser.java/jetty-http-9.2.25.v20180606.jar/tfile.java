// 
// Decompiled by Procyon v0.5.36
// 

package org.eclipse.jetty.http;

import org.eclipse.jetty.util.ArrayTrie;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.TypeUtil;
import java.nio.charset.StandardCharsets;
import org.eclipse.jetty.util.HostPort;
import org.eclipse.jetty.util.ArrayTernaryTrie;
import org.eclipse.jetty.util.BufferUtil;
import java.nio.ByteBuffer;
import org.eclipse.jetty.util.Trie;
import org.eclipse.jetty.util.log.Logger;

public class HttpParser
{
    public static final Logger LOG;
    public static final boolean __STRICT;
    public static final int INITIAL_URI_LENGTH = 256;
    private static final int MAX_CHUNK_LENGTH = 134217711;
    public static final Trie<HttpField> CACHE;
    private final boolean DEBUG;
    private final HttpHandler<ByteBuffer> _handler;
    private final RequestHandler<ByteBuffer> _requestHandler;
    private final ResponseHandler<ByteBuffer> _responseHandler;
    private final int _maxHeaderBytes;
    private final boolean _strict;
    private HttpField _field;
    private HttpHeader _header;
    private String _headerString;
    private HttpHeaderValue _value;
    private String _valueString;
    private int _responseStatus;
    private int _headerBytes;
    private boolean _host;
    private volatile State _state;
    private volatile boolean _eof;
    private volatile boolean _closed;
    private HttpMethod _method;
    private String _methodString;
    private HttpVersion _version;
    private ByteBuffer _uri;
    private HttpTokens.EndOfContent _endOfContent;
    private boolean _hasContentLength;
    private long _contentLength;
    private long _contentPosition;
    private int _chunkLength;
    private int _chunkPosition;
    private boolean _headResponse;
    private boolean _cr;
    private ByteBuffer _contentChunk;
    private Trie<HttpField> _connectionFields;
    private int _length;
    private final StringBuilder _string;
    
    public HttpParser(final RequestHandler<ByteBuffer> handler) {
        this(handler, -1, HttpParser.__STRICT);
    }
    
    public HttpParser(final ResponseHandler<ByteBuffer> handler) {
        this(handler, -1, HttpParser.__STRICT);
    }
    
    public HttpParser(final RequestHandler<ByteBuffer> handler, final int maxHeaderBytes) {
        this(handler, maxHeaderBytes, HttpParser.__STRICT);
    }
    
    public HttpParser(final ResponseHandler<ByteBuffer> handler, final int maxHeaderBytes) {
        this(handler, maxHeaderBytes, HttpParser.__STRICT);
    }
    
    public HttpParser(final RequestHandler<ByteBuffer> handler, final int maxHeaderBytes, final boolean strict) {
        this.DEBUG = HttpParser.LOG.isDebugEnabled();
        this._state = State.START;
        this._uri = ByteBuffer.allocate(256);
        this._string = new StringBuilder();
        this._handler = handler;
        this._requestHandler = handler;
        this._responseHandler = null;
        this._maxHeaderBytes = maxHeaderBytes;
        this._strict = strict;
    }
    
    public HttpParser(final ResponseHandler<ByteBuffer> handler, final int maxHeaderBytes, final boolean strict) {
        this.DEBUG = HttpParser.LOG.isDebugEnabled();
        this._state = State.START;
        this._uri = ByteBuffer.allocate(256);
        this._string = new StringBuilder();
        this._handler = handler;
        this._requestHandler = null;
        this._responseHandler = handler;
        this._maxHeaderBytes = maxHeaderBytes;
        this._strict = strict;
    }
    
    public long getContentLength() {
        return this._contentLength;
    }
    
    public long getContentRead() {
        return this._contentPosition;
    }
    
    public void setHeadResponse(final boolean head) {
        this._headResponse = head;
    }
    
    protected void setResponseStatus(final int status) {
        this._responseStatus = status;
    }
    
    public State getState() {
        return this._state;
    }
    
    public boolean inContentState() {
        return this._state.ordinal() >= State.CONTENT.ordinal() && this._state.ordinal() < State.END.ordinal();
    }
    
    public boolean inHeaderState() {
        return this._state.ordinal() < State.CONTENT.ordinal();
    }
    
    public boolean isChunking() {
        return this._endOfContent == HttpTokens.EndOfContent.CHUNKED_CONTENT;
    }
    
    public boolean isStart() {
        return this.isState(State.START);
    }
    
    public boolean isClosed() {
        return this.isState(State.CLOSED);
    }
    
    public boolean isIdle() {
        return this.isState(State.START) || this.isState(State.END) || this.isState(State.CLOSED);
    }
    
    public boolean isComplete() {
        return this.isState(State.END) || this.isState(State.CLOSED);
    }
    
    public boolean isState(final State state) {
        return this._state == state;
    }
    
    private byte next(final ByteBuffer buffer) {
        byte ch = buffer.get();
        if (!this._cr) {
            if (ch >= 0 && ch < 32) {
                if (ch == 13) {
                    if (!buffer.hasRemaining()) {
                        this._cr = true;
                        return 0;
                    }
                    if (this._maxHeaderBytes > 0 && this._state.ordinal() < State.END.ordinal()) {
                        ++this._headerBytes;
                    }
                    ch = buffer.get();
                    if (ch != 10) {
                        throw new BadMessageException("Bad EOL");
                    }
                }
                else if (ch != 10 && ch != 9) {
                    throw new IllegalCharacterException(this._state, ch, buffer);
                }
            }
            return ch;
        }
        if (ch != 10) {
            throw new BadMessageException("Bad EOL");
        }
        this._cr = false;
        return ch;
    }
    
    private boolean quickStart(final ByteBuffer buffer) {
        if (this._requestHandler != null) {
            this._method = HttpMethod.lookAheadGet(buffer);
            if (this._method != null) {
                this._methodString = this._method.asString();
                buffer.position(buffer.position() + this._methodString.length() + 1);
                this.setState(State.SPACE1);
                return false;
            }
        }
        else if (this._responseHandler != null) {
            this._version = HttpVersion.lookAheadGet(buffer);
            if (this._version != null) {
                buffer.position(buffer.position() + this._version.asString().length() + 1);
                this.setState(State.SPACE1);
                return false;
            }
        }
        while (this._state == State.START && buffer.hasRemaining()) {
            final int ch = this.next(buffer);
            if (ch > 32) {
                this._string.setLength(0);
                this._string.append((char)ch);
                this.setState((this._requestHandler != null) ? State.METHOD : State.RESPONSE_VERSION);
                return false;
            }
            if (ch == 0) {
                break;
            }
            if (ch != 10) {
                throw new BadMessageException();
            }
            if (this._maxHeaderBytes > 0 && ++this._headerBytes > this._maxHeaderBytes) {
                HttpParser.LOG.warn("padding is too large >" + this._maxHeaderBytes, new Object[0]);
                throw new BadMessageException(400);
            }
        }
        return false;
    }
    
    private void setString(final String s) {
        this._string.setLength(0);
        this._string.append(s);
        this._length = s.length();
    }
    
    private String takeString() {
        this._string.setLength(this._length);
        final String s = this._string.toString();
        this._string.setLength(0);
        this._length = -1;
        return s;
    }
    
    private boolean parseLine(final ByteBuffer buffer) {
        boolean handle = false;
        while (this._state.ordinal() < State.HEADER.ordinal() && buffer.hasRemaining() && !handle) {
            final byte ch = this.next(buffer);
            if (ch == 0) {
                break;
            }
            if (this._maxHeaderBytes > 0 && ++this._headerBytes > this._maxHeaderBytes) {
                if (this._state == State.URI) {
                    HttpParser.LOG.warn("URI is too large >" + this._maxHeaderBytes, new Object[0]);
                    throw new BadMessageException(414);
                }
                if (this._requestHandler != null) {
                    HttpParser.LOG.warn("request is too large >" + this._maxHeaderBytes, new Object[0]);
                }
                else {
                    HttpParser.LOG.warn("response is too large >" + this._maxHeaderBytes, new Object[0]);
                }
                throw new BadMessageException(413);
            }
            else {
                switch (this._state) {
                    case METHOD: {
                        if (ch == 32) {
                            this._length = this._string.length();
                            this._methodString = this.takeString();
                            final HttpMethod method = (HttpMethod)HttpMethod.CACHE.get(this._methodString);
                            if (method != null && !this._strict) {
                                this._methodString = method.asString();
                            }
                            this.setState(State.SPACE1);
                            continue;
                        }
                        if (ch >= 32) {
                            this._string.append((char)ch);
                            continue;
                        }
                        if (ch == 10) {
                            throw new BadMessageException("No URI");
                        }
                        throw new IllegalCharacterException(this._state, ch, buffer);
                    }
                    case RESPONSE_VERSION: {
                        if (ch == 32) {
                            this._length = this._string.length();
                            final String version = this.takeString();
                            this._version = (HttpVersion)HttpVersion.CACHE.get(version);
                            if (this._version == null) {
                                throw new BadMessageException(400, "Unknown Version");
                            }
                            this.setState(State.SPACE1);
                            continue;
                        }
                        else {
                            if (ch < 32) {
                                throw new IllegalCharacterException(this._state, ch, buffer);
                            }
                            this._string.append((char)ch);
                            continue;
                        }
                        break;
                    }
                    case SPACE1: {
                        if (ch > 32 || ch < 0) {
                            if (this._responseHandler != null) {
                                this.setState(State.STATUS);
                                this.setResponseStatus(ch - 48);
                                continue;
                            }
                            this._uri.clear();
                            this.setState(State.URI);
                            if (!buffer.hasArray()) {
                                this._uri.put(ch);
                                continue;
                            }
                            final byte[] array = buffer.array();
                            final int p = buffer.arrayOffset() + buffer.position();
                            int l;
                            int i;
                            for (l = buffer.arrayOffset() + buffer.limit(), i = p; i < l && array[i] > 32; ++i) {}
                            final int len = i - p;
                            this._headerBytes += len;
                            if (this._maxHeaderBytes > 0 && ++this._headerBytes > this._maxHeaderBytes) {
                                HttpParser.LOG.warn("URI is too large >" + this._maxHeaderBytes, new Object[0]);
                                throw new BadMessageException(414);
                            }
                            if (this._uri.remaining() <= len) {
                                final ByteBuffer uri = ByteBuffer.allocate(this._uri.capacity() + 2 * len);
                                this._uri.flip();
                                uri.put(this._uri);
                                this._uri = uri;
                            }
                            this._uri.put(array, p - 1, len + 1);
                            buffer.position(i - buffer.arrayOffset());
                            continue;
                        }
                        else {
                            if (ch < 32) {
                                throw new BadMessageException(400, (this._requestHandler != null) ? "No URI" : "No Status");
                            }
                            continue;
                        }
                        break;
                    }
                    case STATUS: {
                        if (ch == 32) {
                            this.setState(State.SPACE2);
                            continue;
                        }
                        if (ch >= 48 && ch <= 57) {
                            this._responseStatus = this._responseStatus * 10 + (ch - 48);
                            continue;
                        }
                        if (ch < 32 && ch >= 0) {
                            handle = (this._responseHandler.startResponse(this._version, this._responseStatus, null) || handle);
                            this.setState(State.HEADER);
                            continue;
                        }
                        throw new BadMessageException();
                    }
                    case URI: {
                        if (ch == 32) {
                            this.setState(State.SPACE2);
                            continue;
                        }
                        if (ch < 32 && ch >= 0) {
                            this._uri.flip();
                            handle = (this._requestHandler.startRequest(this._method, this._methodString, this._uri, null) || handle);
                            this.setState(State.END);
                            BufferUtil.clear(buffer);
                            handle = (this._handler.headerComplete() || handle);
                            handle = (this._handler.messageComplete() || handle);
                            return handle;
                        }
                        if (!this._uri.hasRemaining()) {
                            final ByteBuffer uri2 = ByteBuffer.allocate(this._uri.capacity() * 2);
                            this._uri.flip();
                            uri2.put(this._uri);
                            this._uri = uri2;
                        }
                        this._uri.put(ch);
                        continue;
                    }
                    case SPACE2: {
                        if (ch > 32) {
                            this._string.setLength(0);
                            this._string.append((char)ch);
                            if (this._responseHandler != null) {
                                this._length = 1;
                                this.setState(State.REASON);
                                continue;
                            }
                            this.setState(State.REQUEST_VERSION);
                            HttpVersion version2;
                            if (buffer.position() > 0 && buffer.hasArray()) {
                                version2 = HttpVersion.lookAheadGet(buffer.array(), buffer.arrayOffset() + buffer.position() - 1, buffer.arrayOffset() + buffer.limit());
                            }
                            else {
                                version2 = (HttpVersion)HttpVersion.CACHE.getBest(buffer, 0, buffer.remaining());
                            }
                            if (version2 == null) {
                                if (this._method != HttpMethod.PROXY) {
                                    continue;
                                }
                                if (!(this._requestHandler instanceof ProxyHandler)) {
                                    throw new BadMessageException();
                                }
                                this._uri.flip();
                                final String protocol = BufferUtil.toString(this._uri);
                                buffer.position(buffer.position() - 1);
                                final String sAddr = this.getProxyField(buffer);
                                final String dAddr = this.getProxyField(buffer);
                                final int sPort = BufferUtil.takeInt(buffer);
                                this.next(buffer);
                                final int dPort = BufferUtil.takeInt(buffer);
                                this.next(buffer);
                                this._state = State.START;
                                ((ProxyHandler)this._requestHandler).proxied(protocol, sAddr, dAddr, sPort, dPort);
                                return false;
                            }
                            else {
                                if (version2 != HttpVersion.HTTP_1_0 && version2 != HttpVersion.HTTP_1_1) {
                                    throw new BadMessageException(400, "Bad Version");
                                }
                                final int pos = buffer.position() + version2.asString().length() - 1;
                                if (pos >= buffer.limit()) {
                                    continue;
                                }
                                final byte n = buffer.get(pos);
                                if (n == 13) {
                                    this._cr = true;
                                    this._version = version2;
                                    this._string.setLength(0);
                                    buffer.position(pos + 1);
                                }
                                else {
                                    if (n != 10) {
                                        continue;
                                    }
                                    this._version = version2;
                                    this._string.setLength(0);
                                    buffer.position(pos);
                                }
                            }
                            continue;
                        }
                        else if (ch == 10) {
                            if (this._responseHandler != null) {
                                handle = (this._responseHandler.startResponse(this._version, this._responseStatus, null) || handle);
                                this.setState(State.HEADER);
                                continue;
                            }
                            this._uri.flip();
                            handle = (this._requestHandler.startRequest(this._method, this._methodString, this._uri, null) || handle);
                            this.setState(State.END);
                            BufferUtil.clear(buffer);
                            handle = (this._handler.headerComplete() || handle);
                            handle = (this._handler.messageComplete() || handle);
                            return handle;
                        }
                        else {
                            if (ch < 0) {
                                throw new BadMessageException();
                            }
                            continue;
                        }
                        break;
                    }
                    case REQUEST_VERSION: {
                        if (ch == 10) {
                            if (this._version == null) {
                                this._length = this._string.length();
                                this._version = (HttpVersion)HttpVersion.CACHE.get(this.takeString());
                            }
                            if (this._version == null) {
                                throw new BadMessageException(400, "Unknown Version");
                            }
                            if (this._version != HttpVersion.HTTP_1_0 && this._version != HttpVersion.HTTP_1_1) {
                                throw new BadMessageException(400, "Bad Version");
                            }
                            if (this._connectionFields == null && this._version.getVersion() == HttpVersion.HTTP_1_1.getVersion()) {
                                final int header_cache = this._handler.getHeaderCacheSize();
                                this._connectionFields = (Trie<HttpField>)new ArrayTernaryTrie(header_cache);
                            }
                            this.setState(State.HEADER);
                            this._uri.flip();
                            handle = (this._requestHandler.startRequest(this._method, this._methodString, this._uri, this._version) || handle);
                            continue;
                        }
                        else {
                            if (ch >= 32) {
                                this._string.append((char)ch);
                                continue;
                            }
                            throw new BadMessageException();
                        }
                        break;
                    }
                    case REASON: {
                        if (ch == 10) {
                            final String reason = this.takeString();
                            this.setState(State.HEADER);
                            handle = (this._responseHandler.startResponse(this._version, this._responseStatus, reason) || handle);
                            continue;
                        }
                        if (ch < 32) {
                            throw new BadMessageException();
                        }
                        this._string.append((char)ch);
                        if (ch != 32 && ch != 9) {
                            this._length = this._string.length();
                            continue;
                        }
                        continue;
                    }
                    default: {
                        throw new IllegalStateException(this._state.toString());
                    }
                }
            }
        }
        return handle;
    }
    
    private boolean handleKnownHeaders(final ByteBuffer buffer) {
        boolean add_to_connection_trie = false;
        switch (this._header) {
            case CONTENT_LENGTH: {
                if (this._hasContentLength) {
                    throw new BadMessageException(400, "Bad Content-Lengths");
                }
                this._hasContentLength = true;
                if (this._endOfContent == HttpTokens.EndOfContent.CHUNKED_CONTENT) {
                    throw new BadMessageException(400, "Bad Content-Length");
                }
                try {
                    this._contentLength = Long.parseLong(this._valueString);
                }
                catch (NumberFormatException e) {
                    HttpParser.LOG.ignore((Throwable)e);
                    throw new BadMessageException(400, "Bad Content-Length");
                }
                if (this._contentLength <= 0L) {
                    this._endOfContent = HttpTokens.EndOfContent.NO_CONTENT;
                    break;
                }
                this._endOfContent = HttpTokens.EndOfContent.CONTENT_LENGTH;
                break;
            }
            case TRANSFER_ENCODING: {
                if (this._value == HttpHeaderValue.CHUNKED) {
                    this._endOfContent = HttpTokens.EndOfContent.CHUNKED_CONTENT;
                }
                else if (this._valueString.endsWith(HttpHeaderValue.CHUNKED.toString())) {
                    this._endOfContent = HttpTokens.EndOfContent.CHUNKED_CONTENT;
                }
                else if (this._valueString.contains(HttpHeaderValue.CHUNKED.toString())) {
                    throw new BadMessageException(400, "Bad chunking");
                }
                if (this._hasContentLength && this._endOfContent == HttpTokens.EndOfContent.CHUNKED_CONTENT) {
                    throw new BadMessageException(400, "Bad chunking");
                }
                break;
            }
            case HOST: {
                add_to_connection_trie = (this._connectionFields != null && this._field == null);
                this._host = true;
                if (this._valueString == null) {
                    break;
                }
                if (this._valueString.length() == 0) {
                    break;
                }
                try {
                    final HostPort authority = new HostPort(this._valueString);
                    if (this._requestHandler != null) {
                        this._requestHandler.parsedHostHeader(authority.getHost(), authority.getPort());
                    }
                    break;
                }
                catch (Exception e2) {
                    throw new BadMessageException(400, "Bad Host header", (Throwable)e2);
                }
            }
            case CONNECTION: {
                if (this._valueString != null && this._valueString.contains("close")) {
                    this._closed = true;
                    this._connectionFields = null;
                    break;
                }
                break;
            }
            case AUTHORIZATION:
            case ACCEPT:
            case ACCEPT_CHARSET:
            case ACCEPT_ENCODING:
            case ACCEPT_LANGUAGE:
            case COOKIE:
            case CACHE_CONTROL:
            case USER_AGENT: {
                add_to_connection_trie = (this._connectionFields != null && this._field == null);
                break;
            }
        }
        if (add_to_connection_trie && !this._connectionFields.isFull() && this._header != null && this._valueString != null) {
            this._field = new HttpField(this._header, this._valueString);
            this._connectionFields.put((Object)this._field);
        }
        return false;
    }
    
    protected boolean parseHeaders(final ByteBuffer buffer) {
        boolean handle = false;
        while (this._state.ordinal() < State.CONTENT.ordinal() && buffer.hasRemaining() && !handle) {
            final byte ch = this.next(buffer);
            if (ch == 0) {
                break;
            }
            if (this._maxHeaderBytes > 0 && ++this._headerBytes > this._maxHeaderBytes) {
                HttpParser.LOG.warn("Header is too large >" + this._maxHeaderBytes, new Object[0]);
                throw new BadMessageException(413);
            }
            switch (this._state) {
                case HEADER: {
                    switch (ch) {
                        case 9:
                        case 32:
                        case 58: {
                            if (this._valueString == null) {
                                this._string.setLength(0);
                                this._length = 0;
                            }
                            else {
                                this.setString(this._valueString);
                                this._string.append(' ');
                                ++this._length;
                                this._valueString = null;
                            }
                            this.setState(State.HEADER_VALUE);
                            continue;
                        }
                        default: {
                            if (this._headerString != null || this._valueString != null) {
                                if (this._header != null && this.handleKnownHeaders(buffer)) {
                                    final String s = null;
                                    this._valueString = s;
                                    this._headerString = s;
                                    this._header = null;
                                    this._value = null;
                                    this._field = null;
                                    return true;
                                }
                                handle = (this._handler.parsedHeader((this._field != null) ? this._field : new HttpField(this._header, this._headerString, this._valueString)) || handle);
                            }
                            final String s2 = null;
                            this._valueString = s2;
                            this._headerString = s2;
                            this._header = null;
                            this._value = null;
                            this._field = null;
                            if (ch == 10) {
                                this._contentPosition = 0L;
                                if (!this._host && this._version == HttpVersion.HTTP_1_1 && this._requestHandler != null) {
                                    throw new BadMessageException(400, "No Host");
                                }
                                if (this._responseHandler != null && (this._responseStatus == 304 || this._responseStatus == 204 || this._responseStatus < 200)) {
                                    this._endOfContent = HttpTokens.EndOfContent.NO_CONTENT;
                                }
                                else if (this._endOfContent == HttpTokens.EndOfContent.UNKNOWN_CONTENT) {
                                    if (this._responseStatus == 0 || this._responseStatus == 304 || this._responseStatus == 204 || this._responseStatus < 200) {
                                        this._endOfContent = HttpTokens.EndOfContent.NO_CONTENT;
                                    }
                                    else {
                                        this._endOfContent = HttpTokens.EndOfContent.EOF_CONTENT;
                                    }
                                }
                                switch (this._endOfContent) {
                                    case EOF_CONTENT: {
                                        this.setState(State.EOF_CONTENT);
                                        handle = (this._handler.headerComplete() || handle);
                                        return handle;
                                    }
                                    case CHUNKED_CONTENT: {
                                        this.setState(State.CHUNKED_CONTENT);
                                        handle = (this._handler.headerComplete() || handle);
                                        return handle;
                                    }
                                    case NO_CONTENT: {
                                        handle = (this._handler.headerComplete() || handle);
                                        this.setState(State.END);
                                        handle = (this._handler.messageComplete() || handle);
                                        return handle;
                                    }
                                    default: {
                                        this.setState(State.CONTENT);
                                        handle = (this._handler.headerComplete() || handle);
                                        return handle;
                                    }
                                }
                            }
                            else {
                                if (ch <= 32) {
                                    throw new BadMessageException();
                                }
                                if (buffer.hasRemaining()) {
                                    HttpField field = (this._connectionFields == null) ? null : ((HttpField)this._connectionFields.getBest(buffer, -1, buffer.remaining()));
                                    if (field == null) {
                                        field = (HttpField)HttpParser.CACHE.getBest(buffer, -1, buffer.remaining());
                                    }
                                    if (field != null) {
                                        String n;
                                        String v;
                                        if (this._strict) {
                                            final String fn = field.getName();
                                            final String fv = field.getValue();
                                            n = BufferUtil.toString(buffer, buffer.position() - 1, fn.length(), StandardCharsets.US_ASCII);
                                            if (fv == null) {
                                                v = null;
                                            }
                                            else {
                                                v = BufferUtil.toString(buffer, buffer.position() + fn.length() + 1, fv.length(), StandardCharsets.ISO_8859_1);
                                                field = new HttpField(field.getHeader(), n, v);
                                            }
                                        }
                                        else {
                                            n = field.getName();
                                            v = field.getValue();
                                        }
                                        this._header = field.getHeader();
                                        this._headerString = n;
                                        if (v == null) {
                                            this.setState(State.HEADER_VALUE);
                                            this._string.setLength(0);
                                            this._length = 0;
                                            buffer.position(buffer.position() + n.length() + 1);
                                            continue;
                                        }
                                        final int pos = buffer.position() + n.length() + v.length() + 1;
                                        final byte b = buffer.get(pos);
                                        if (b != 13 && b != 10) {
                                            this.setState(State.HEADER_IN_VALUE);
                                            this.setString(v);
                                            buffer.position(pos);
                                            continue;
                                        }
                                        this._field = field;
                                        this._valueString = v;
                                        this.setState(State.HEADER_IN_VALUE);
                                        if (b == 13) {
                                            this._cr = true;
                                            buffer.position(pos + 1);
                                            continue;
                                        }
                                        buffer.position(pos);
                                        continue;
                                    }
                                }
                                this.setState(State.HEADER_IN_NAME);
                                this._string.setLength(0);
                                this._string.append((char)ch);
                                this._length = 1;
                                continue;
                            }
                            break;
                        }
                    }
                    break;
                }
                case HEADER_IN_NAME: {
                    if (ch == 58 || ch == 10) {
                        if (this._headerString == null) {
                            this._headerString = this.takeString();
                            this._header = (HttpHeader)HttpHeader.CACHE.get(this._headerString);
                        }
                        this._length = -1;
                        this.setState((ch == 10) ? State.HEADER : State.HEADER_VALUE);
                        continue;
                    }
                    if (ch < 32 && ch != 9) {
                        throw new IllegalCharacterException(this._state, ch, buffer);
                    }
                    if (this._header != null) {
                        this.setString(this._header.asString());
                        this._header = null;
                        this._headerString = null;
                    }
                    this._string.append((char)ch);
                    if (ch > 32) {
                        this._length = this._string.length();
                        continue;
                    }
                    continue;
                }
                case HEADER_VALUE: {
                    if (ch > 32 || ch < 0) {
                        this._string.append((char)(0xFF & ch));
                        this._length = this._string.length();
                        this.setState(State.HEADER_IN_VALUE);
                        continue;
                    }
                    if (ch == 32) {
                        continue;
                    }
                    if (ch == 9) {
                        continue;
                    }
                    if (ch == 10) {
                        if (this._length > 0) {
                            this._value = null;
                            this._valueString = ((this._valueString == null) ? this.takeString() : (this._valueString + " " + this.takeString()));
                        }
                        this.setState(State.HEADER);
                        continue;
                    }
                    throw new IllegalCharacterException(this._state, ch, buffer);
                }
                case HEADER_IN_VALUE: {
                    if (ch >= 32 || ch < 0 || ch == 9) {
                        if (this._valueString != null) {
                            this.setString(this._valueString);
                            this._valueString = null;
                            this._field = null;
                        }
                        this._string.append((char)(0xFF & ch));
                        if (ch > 32 || ch < 0) {
                            this._length = this._string.length();
                            continue;
                        }
                        continue;
                    }
                    else {
                        if (ch == 10) {
                            if (this._length > 0) {
                                this._value = null;
                                this._valueString = this.takeString();
                                this._length = -1;
                            }
                            this.setState(State.HEADER);
                            continue;
                        }
                        throw new IllegalCharacterException(this._state, ch, buffer);
                    }
                    break;
                }
                default: {
                    throw new IllegalStateException(this._state.toString());
                }
            }
        }
        return handle;
    }
    
    public boolean parseNext(final ByteBuffer buffer) {
        if (this.DEBUG) {
            HttpParser.LOG.debug("parseNext s={} {}", new Object[] { this._state, BufferUtil.toDetailString(buffer) });
        }
        try {
            if (this._state == State.START) {
                this._version = null;
                this._method = null;
                this._methodString = null;
                this._endOfContent = HttpTokens.EndOfContent.UNKNOWN_CONTENT;
                this._header = null;
                if (this.quickStart(buffer)) {
                    return true;
                }
            }
            if (this._state.ordinal() >= State.START.ordinal() && this._state.ordinal() < State.HEADER.ordinal() && this.parseLine(buffer)) {
                return true;
            }
            if (this._state.ordinal() >= State.HEADER.ordinal() && this._state.ordinal() < State.CONTENT.ordinal() && this.parseHeaders(buffer)) {
                return true;
            }
            if (this._state.ordinal() >= State.CONTENT.ordinal() && this._state.ordinal() < State.END.ordinal()) {
                if (this._responseStatus > 0 && this._headResponse) {
                    this.setState(State.END);
                    return this._handler.messageComplete();
                }
                if (this.parseContent(buffer)) {
                    return true;
                }
            }
            if (this._state == State.END) {
                while (buffer.remaining() > 0 && buffer.get(buffer.position()) <= 32) {
                    buffer.get();
                }
            }
            else if (this._state == State.CLOSED && BufferUtil.hasContent(buffer)) {
                this._headerBytes += buffer.remaining();
                BufferUtil.clear(buffer);
                if (this._maxHeaderBytes > 0 && this._headerBytes > this._maxHeaderBytes) {
                    throw new IllegalStateException("too much data after closed");
                }
            }
            if (this._eof && !buffer.hasRemaining()) {
                switch (this._state) {
                    case CLOSED: {
                        break;
                    }
                    case START: {
                        this.setState(State.CLOSED);
                        this._handler.earlyEOF();
                        break;
                    }
                    case END: {
                        this.setState(State.CLOSED);
                        break;
                    }
                    case EOF_CONTENT: {
                        this.setState(State.CLOSED);
                        return this._handler.messageComplete();
                    }
                    case CONTENT:
                    case CHUNKED_CONTENT:
                    case CHUNK_SIZE:
                    case CHUNK_PARAMS:
                    case CHUNK: {
                        this.setState(State.CLOSED);
                        this._handler.earlyEOF();
                        break;
                    }
                    default: {
                        if (this.DEBUG) {
                            HttpParser.LOG.debug("{} EOF in {}", new Object[] { this, this._state });
                        }
                        this.setState(State.CLOSED);
                        this._handler.badMessage(400, null);
                        break;
                    }
                }
            }
            return false;
        }
        catch (BadMessageException e) {
            BufferUtil.clear(buffer);
            HttpParser.LOG.warn("badMessage: " + e._code + ((e.getMessage() != null) ? (" " + e.getMessage()) : "") + " for " + this._handler, new Object[0]);
            if (this.DEBUG) {
                HttpParser.LOG.debug((Throwable)e);
            }
            this.setState(State.CLOSED);
            this._handler.badMessage(e._code, e.getMessage());
            return false;
        }
        catch (Exception e2) {
            BufferUtil.clear(buffer);
            HttpParser.LOG.warn("badMessage: " + e2.toString() + " for " + this._handler, new Object[0]);
            if (this.DEBUG) {
                HttpParser.LOG.debug((Throwable)e2);
            }
            if (this._state.ordinal() <= State.END.ordinal()) {
                this.setState(State.CLOSED);
                this._handler.badMessage(400, null);
            }
            else {
                this._handler.earlyEOF();
                this.setState(State.CLOSED);
            }
            return false;
        }
    }
    
    protected boolean parseContent(final ByteBuffer buffer) {
        int remaining = buffer.remaining();
        if (remaining == 0 && this._state == State.CONTENT) {
            final long content = this._contentLength - this._contentPosition;
            if (content == 0L) {
                this.setState(State.END);
                return this._handler.messageComplete();
            }
        }
        while (this._state.ordinal() < State.END.ordinal() && remaining > 0) {
            switch (this._state) {
                case EOF_CONTENT: {
                    this._contentChunk = buffer.asReadOnlyBuffer();
                    this._contentPosition += remaining;
                    buffer.position(buffer.position() + remaining);
                    if (this._handler.content(this._contentChunk)) {
                        return true;
                    }
                    break;
                }
                case CONTENT: {
                    final long content2 = this._contentLength - this._contentPosition;
                    if (content2 == 0L) {
                        this.setState(State.END);
                        return this._handler.messageComplete();
                    }
                    this._contentChunk = buffer.asReadOnlyBuffer();
                    if (remaining > content2) {
                        this._contentChunk.limit(this._contentChunk.position() + (int)content2);
                    }
                    this._contentPosition += this._contentChunk.remaining();
                    buffer.position(buffer.position() + this._contentChunk.remaining());
                    if (this._handler.content(this._contentChunk)) {
                        return true;
                    }
                    if (this._contentPosition == this._contentLength) {
                        this.setState(State.END);
                        return this._handler.messageComplete();
                    }
                    break;
                }
                case CHUNKED_CONTENT: {
                    final byte ch = this.next(buffer);
                    if (ch > 32) {
                        this._chunkLength = TypeUtil.convertHexDigit(ch);
                        this._chunkPosition = 0;
                        this.setState(State.CHUNK_SIZE);
                        break;
                    }
                    break;
                }
                case CHUNK_SIZE: {
                    final byte ch = this.next(buffer);
                    if (ch == 0) {
                        break;
                    }
                    if (ch == 10) {
                        if (this._chunkLength == 0) {
                            this.setState(State.CHUNK_END);
                            break;
                        }
                        this.setState(State.CHUNK);
                        break;
                    }
                    else {
                        if (ch <= 32 || ch == 59) {
                            this.setState(State.CHUNK_PARAMS);
                            break;
                        }
                        if (this._chunkLength > 134217711) {
                            throw new BadMessageException(413);
                        }
                        this._chunkLength = this._chunkLength * 16 + TypeUtil.convertHexDigit(ch);
                        break;
                    }
                    break;
                }
                case CHUNK_PARAMS: {
                    final byte ch = this.next(buffer);
                    if (ch != 10) {
                        break;
                    }
                    if (this._chunkLength == 0) {
                        this.setState(State.CHUNK_END);
                        break;
                    }
                    this.setState(State.CHUNK);
                    break;
                }
                case CHUNK: {
                    int chunk = this._chunkLength - this._chunkPosition;
                    if (chunk == 0) {
                        this.setState(State.CHUNKED_CONTENT);
                        break;
                    }
                    this._contentChunk = buffer.asReadOnlyBuffer();
                    if (remaining > chunk) {
                        this._contentChunk.limit(this._contentChunk.position() + chunk);
                    }
                    chunk = this._contentChunk.remaining();
                    this._contentPosition += chunk;
                    this._chunkPosition += chunk;
                    buffer.position(buffer.position() + chunk);
                    if (this._handler.content(this._contentChunk)) {
                        return true;
                    }
                    break;
                }
                case CHUNK_END: {
                    final byte ch = this.next(buffer);
                    if (ch == 0) {
                        break;
                    }
                    if (ch == 10) {
                        this.setState(State.END);
                        return this._handler.messageComplete();
                    }
                    throw new IllegalCharacterException(this._state, ch, buffer);
                }
                case CLOSED: {
                    BufferUtil.clear(buffer);
                    return false;
                }
            }
            remaining = buffer.remaining();
        }
        return false;
    }
    
    public boolean isAtEOF() {
        return this._eof;
    }
    
    public void atEOF() {
        if (this.DEBUG) {
            HttpParser.LOG.debug("atEOF {}", new Object[] { this });
        }
        this._eof = true;
    }
    
    public void close() {
        if (this.DEBUG) {
            HttpParser.LOG.debug("close {}", new Object[] { this });
        }
        this.setState(State.CLOSED);
    }
    
    public void reset() {
        if (this.DEBUG) {
            HttpParser.LOG.debug("reset {}", new Object[] { this });
        }
        if (this._state == State.CLOSED) {
            return;
        }
        if (this._closed) {
            this.setState(State.CLOSED);
            return;
        }
        this.setState(State.START);
        this._endOfContent = HttpTokens.EndOfContent.UNKNOWN_CONTENT;
        this._contentLength = -1L;
        this._hasContentLength = false;
        this._contentPosition = 0L;
        this._responseStatus = 0;
        this._contentChunk = null;
        this._headerBytes = 0;
        this._host = false;
    }
    
    protected void setState(final State state) {
        if (this.DEBUG) {
            HttpParser.LOG.debug("{} --> {}", new Object[] { this._state, state });
        }
        this._state = state;
    }
    
    public Trie<HttpField> getFieldCache() {
        return this._connectionFields;
    }
    
    private String getProxyField(final ByteBuffer buffer) {
        this._string.setLength(0);
        this._length = 0;
        while (buffer.hasRemaining()) {
            final byte ch = this.next(buffer);
            if (ch <= 32) {
                return this._string.toString();
            }
            this._string.append((char)ch);
        }
        throw new BadMessageException();
    }
    
    @Override
    public String toString() {
        return String.format("%s{s=%s,%d of %d}", this.getClass().getSimpleName(), this._state, this._contentPosition, this._contentLength);
    }
    
    static {
        LOG = Log.getLogger((Class)HttpParser.class);
        __STRICT = Boolean.getBoolean("org.eclipse.jetty.http.HttpParser.STRICT");
        (CACHE = (Trie)new ArrayTrie(2048)).put((Object)new HttpField(HttpHeader.CONNECTION, HttpHeaderValue.CLOSE));
        HttpParser.CACHE.put((Object)new HttpField(HttpHeader.CONNECTION, HttpHeaderValue.KEEP_ALIVE));
        HttpParser.CACHE.put((Object)new HttpField(HttpHeader.CONNECTION, HttpHeaderValue.UPGRADE));
        HttpParser.CACHE.put((Object)new HttpField(HttpHeader.ACCEPT_ENCODING, "gzip"));
        HttpParser.CACHE.put((Object)new HttpField(HttpHeader.ACCEPT_ENCODING, "gzip, deflate"));
        HttpParser.CACHE.put((Object)new HttpField(HttpHeader.ACCEPT_ENCODING, "gzip,deflate,sdch"));
        HttpParser.CACHE.put((Object)new HttpField(HttpHeader.ACCEPT_LANGUAGE, "en-US,en;q=0.5"));
        HttpParser.CACHE.put((Object)new HttpField(HttpHeader.ACCEPT_LANGUAGE, "en-GB,en-US;q=0.8,en;q=0.6"));
        HttpParser.CACHE.put((Object)new HttpField(HttpHeader.ACCEPT_CHARSET, "ISO-8859-1,utf-8;q=0.7,*;q=0.3"));
        HttpParser.CACHE.put((Object)new HttpField(HttpHeader.ACCEPT, "*/*"));
        HttpParser.CACHE.put((Object)new HttpField(HttpHeader.ACCEPT, "image/png,image/*;q=0.8,*/*;q=0.5"));
        HttpParser.CACHE.put((Object)new HttpField(HttpHeader.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"));
        HttpParser.CACHE.put((Object)new HttpField(HttpHeader.PRAGMA, "no-cache"));
        HttpParser.CACHE.put((Object)new HttpField(HttpHeader.CACHE_CONTROL, "private, no-cache, no-cache=Set-Cookie, proxy-revalidate"));
        HttpParser.CACHE.put((Object)new HttpField(HttpHeader.CACHE_CONTROL, "no-cache"));
        HttpParser.CACHE.put((Object)new HttpField(HttpHeader.CONTENT_LENGTH, "0"));
        HttpParser.CACHE.put((Object)new HttpField(HttpHeader.CONTENT_ENCODING, "gzip"));
        HttpParser.CACHE.put((Object)new HttpField(HttpHeader.CONTENT_ENCODING, "deflate"));
        HttpParser.CACHE.put((Object)new HttpField(HttpHeader.TRANSFER_ENCODING, "chunked"));
        HttpParser.CACHE.put((Object)new HttpField(HttpHeader.EXPIRES, "Fri, 01 Jan 1990 00:00:00 GMT"));
        for (final String type : new String[] { "text/plain", "text/html", "text/xml", "text/json", "application/json", "application/x-www-form-urlencoded" }) {
            final HttpField field = new HttpGenerator.CachedHttpField(HttpHeader.CONTENT_TYPE, type);
            HttpParser.CACHE.put((Object)field);
            for (final String charset : new String[] { "UTF-8", "ISO-8859-1" }) {
                HttpParser.CACHE.put((Object)new HttpGenerator.CachedHttpField(HttpHeader.CONTENT_TYPE, type + ";charset=" + charset));
                HttpParser.CACHE.put((Object)new HttpGenerator.CachedHttpField(HttpHeader.CONTENT_TYPE, type + "; charset=" + charset));
            }
        }
        for (final HttpHeader h : HttpHeader.values()) {
            if (!HttpParser.CACHE.put((Object)new HttpField(h, (String)null))) {
                throw new IllegalStateException("CACHE FULL");
            }
        }
        HttpParser.CACHE.put((Object)new HttpField(HttpHeader.REFERER, (String)null));
        HttpParser.CACHE.put((Object)new HttpField(HttpHeader.IF_MODIFIED_SINCE, (String)null));
        HttpParser.CACHE.put((Object)new HttpField(HttpHeader.IF_NONE_MATCH, (String)null));
        HttpParser.CACHE.put((Object)new HttpField(HttpHeader.AUTHORIZATION, (String)null));
        HttpParser.CACHE.put((Object)new HttpField(HttpHeader.COOKIE, (String)null));
    }
    
    public enum State
    {
        START, 
        METHOD, 
        RESPONSE_VERSION, 
        SPACE1, 
        STATUS, 
        URI, 
        SPACE2, 
        REQUEST_VERSION, 
        REASON, 
        PROXY, 
        HEADER, 
        HEADER_IN_NAME, 
        HEADER_VALUE, 
        HEADER_IN_VALUE, 
        CONTENT, 
        EOF_CONTENT, 
        CHUNKED_CONTENT, 
        CHUNK_SIZE, 
        CHUNK_PARAMS, 
        CHUNK, 
        CHUNK_END, 
        END, 
        CLOSED;
    }
    
    private static class BadMessageException extends RuntimeException
    {
        private final int _code;
        
        private BadMessageException() {
            this(400, (String)null);
        }
        
        private BadMessageException(final int code) {
            this(code, (String)null);
        }
        
        private BadMessageException(final String message) {
            this(400, message);
        }
        
        private BadMessageException(final int code, final String message) {
            this(code, message, (Throwable)null);
        }
        
        private BadMessageException(final int code, final String message, final Throwable cause) {
            super(message, cause);
            this._code = code;
        }
    }
    
    private static class IllegalCharacterException extends BadMessageException
    {
        private IllegalCharacterException(final State state, final byte ch, final ByteBuffer buffer) {
            super(400, String.format("Illegal character 0x%X", ch));
            HttpParser.LOG.warn(String.format("Illegal character 0x%X in state=%s for buffer %s", ch, state, BufferUtil.toDetailString(buffer)), new Object[0]);
        }
    }
    
    public interface ResponseHandler<T> extends HttpHandler<T>
    {
        boolean startResponse(final HttpVersion p0, final int p1, final String p2);
    }
    
    public interface HttpHandler<T>
    {
        boolean content(final T p0);
        
        boolean headerComplete();
        
        boolean messageComplete();
        
        boolean parsedHeader(final HttpField p0);
        
        void earlyEOF();
        
        void badMessage(final int p0, final String p1);
        
        int getHeaderCacheSize();
    }
    
    public interface RequestHandler<T> extends HttpHandler<T>
    {
        boolean startRequest(final HttpMethod p0, final String p1, final ByteBuffer p2, final HttpVersion p3);
        
        boolean parsedHostHeader(final String p0, final int p1);
    }
    
    public interface ProxyHandler
    {
        void proxied(final String p0, final String p1, final String p2, final int p3, final int p4);
    }
}
