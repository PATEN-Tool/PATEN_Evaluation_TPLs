// 
// Decompiled by Procyon v0.5.36
// 

package org.eclipse.jetty.http;

import java.util.Arrays;
import java.util.Locale;
import org.eclipse.jetty.util.ArrayTrie;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.TypeUtil;
import java.nio.charset.StandardCharsets;
import org.eclipse.jetty.util.ArrayTernaryTrie;
import org.eclipse.jetty.util.BufferUtil;
import java.nio.ByteBuffer;
import org.eclipse.jetty.util.Utf8StringBuilder;
import java.util.EnumSet;
import org.eclipse.jetty.util.Trie;
import org.eclipse.jetty.util.log.Logger;

public class HttpParser
{
    public static final Logger LOG;
    @Deprecated
    public static final String __STRICT = "org.eclipse.jetty.http.HttpParser.STRICT";
    public static final int INITIAL_URI_LENGTH = 256;
    private static final int MAX_CHUNK_LENGTH = 134217711;
    public static final Trie<HttpField> CACHE;
    private static final EnumSet<State> __idleStates;
    private static final EnumSet<State> __completeStates;
    private final boolean DEBUG;
    private final HttpHandler _handler;
    private final RequestHandler _requestHandler;
    private final ResponseHandler _responseHandler;
    private final ComplianceHandler _complianceHandler;
    private final int _maxHeaderBytes;
    private final HttpCompliance _compliance;
    private HttpField _field;
    private HttpHeader _header;
    private String _headerString;
    private HttpHeaderValue _value;
    private String _valueString;
    private int _responseStatus;
    private int _headerBytes;
    private boolean _host;
    private boolean _headerComplete;
    private volatile State _state;
    private volatile boolean _eof;
    private HttpMethod _method;
    private String _methodString;
    private HttpVersion _version;
    private Utf8StringBuilder _uri;
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
    private static final CharState[] __charState;
    
    private static HttpCompliance compliance() {
        final Boolean strict = Boolean.getBoolean("org.eclipse.jetty.http.HttpParser.STRICT");
        return strict ? HttpCompliance.LEGACY : HttpCompliance.RFC7230;
    }
    
    public HttpParser(final RequestHandler handler) {
        this(handler, -1, compliance());
    }
    
    public HttpParser(final ResponseHandler handler) {
        this(handler, -1, compliance());
    }
    
    public HttpParser(final RequestHandler handler, final int maxHeaderBytes) {
        this(handler, maxHeaderBytes, compliance());
    }
    
    public HttpParser(final ResponseHandler handler, final int maxHeaderBytes) {
        this(handler, maxHeaderBytes, compliance());
    }
    
    @Deprecated
    public HttpParser(final RequestHandler handler, final int maxHeaderBytes, final boolean strict) {
        this(handler, maxHeaderBytes, strict ? HttpCompliance.LEGACY : compliance());
    }
    
    @Deprecated
    public HttpParser(final ResponseHandler handler, final int maxHeaderBytes, final boolean strict) {
        this(handler, maxHeaderBytes, strict ? HttpCompliance.LEGACY : compliance());
    }
    
    public HttpParser(final RequestHandler handler, final HttpCompliance compliance) {
        this(handler, -1, compliance);
    }
    
    public HttpParser(final RequestHandler handler, final int maxHeaderBytes, final HttpCompliance compliance) {
        this.DEBUG = HttpParser.LOG.isDebugEnabled();
        this._state = State.START;
        this._uri = new Utf8StringBuilder(256);
        this._string = new StringBuilder();
        this._handler = handler;
        this._requestHandler = handler;
        this._responseHandler = null;
        this._maxHeaderBytes = maxHeaderBytes;
        this._compliance = ((compliance == null) ? compliance() : compliance);
        this._complianceHandler = ((handler instanceof ComplianceHandler) ? handler : null);
    }
    
    public HttpParser(final ResponseHandler handler, final int maxHeaderBytes, final HttpCompliance compliance) {
        this.DEBUG = HttpParser.LOG.isDebugEnabled();
        this._state = State.START;
        this._uri = new Utf8StringBuilder(256);
        this._string = new StringBuilder();
        this._handler = handler;
        this._requestHandler = null;
        this._responseHandler = handler;
        this._maxHeaderBytes = maxHeaderBytes;
        this._compliance = ((compliance == null) ? compliance() : compliance);
        this._complianceHandler = ((handler instanceof ComplianceHandler) ? handler : null);
    }
    
    public HttpHandler getHandler() {
        return this._handler;
    }
    
    protected boolean complianceViolation(final HttpCompliance compliance, final String reason) {
        if (this._complianceHandler == null) {
            return this._compliance.ordinal() >= compliance.ordinal();
        }
        if (this._compliance.ordinal() < compliance.ordinal()) {
            this._complianceHandler.onComplianceViolation(this._compliance, compliance, reason);
            return false;
        }
        return true;
    }
    
    protected String legacyString(final String orig, final String cached) {
        return (this._compliance != HttpCompliance.LEGACY || orig.equals(cached) || this.complianceViolation(HttpCompliance.RFC2616, "case sensitive")) ? cached : orig;
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
    
    public boolean isClose() {
        return this.isState(State.CLOSE);
    }
    
    public boolean isClosed() {
        return this.isState(State.CLOSED);
    }
    
    public boolean isIdle() {
        return HttpParser.__idleStates.contains(this._state);
    }
    
    public boolean isComplete() {
        return HttpParser.__completeStates.contains(this._state);
    }
    
    public boolean isState(final State state) {
        return this._state == state;
    }
    
    private byte next(final ByteBuffer buffer) {
        final byte ch = buffer.get();
        final CharState s = HttpParser.__charState[0xFF & ch];
        switch (s) {
            case ILLEGAL: {
                throw new IllegalCharacterException(this._state, ch, buffer);
            }
            case LF: {
                this._cr = false;
                break;
            }
            case CR: {
                if (this._cr) {
                    throw new BadMessageException("Bad EOL");
                }
                this._cr = true;
                if (buffer.hasRemaining()) {
                    if (this._maxHeaderBytes > 0 && this._state.ordinal() < State.END.ordinal()) {
                        ++this._headerBytes;
                    }
                    return this.next(buffer);
                }
                return 0;
            }
            case LEGAL: {
                if (this._cr) {
                    throw new BadMessageException("Bad EOL");
                }
                break;
            }
        }
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
                throw new BadMessageException("Bad preamble");
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
    
    private boolean handleHeaderContentMessage() {
        final boolean handle_header = this._handler.headerComplete();
        final boolean handle_content = this._handler.contentComplete();
        final boolean handle_message = this._handler.messageComplete();
        return handle_header || handle_content || handle_message;
    }
    
    private boolean handleContentMessage() {
        final boolean handle_content = this._handler.contentComplete();
        final boolean handle_message = this._handler.messageComplete();
        return handle_content || handle_message;
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
                throw new BadMessageException(431);
            }
            else {
                switch (this._state) {
                    case METHOD: {
                        if (ch == 32) {
                            this._length = this._string.length();
                            this._methodString = this.takeString();
                            final HttpMethod method = (HttpMethod)HttpMethod.CACHE.get(this._methodString);
                            if (method != null) {
                                this._methodString = this.legacyString(this._methodString, method.asString());
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
                            this.checkVersion();
                            this.setState(State.SPACE1);
                            continue;
                        }
                        if (ch < 32) {
                            throw new IllegalCharacterException(this._state, ch, buffer);
                        }
                        this._string.append((char)ch);
                        continue;
                    }
                    case SPACE1: {
                        if (ch > 32 || ch < 0) {
                            if (this._responseHandler != null) {
                                this.setState(State.STATUS);
                                this.setResponseStatus(ch - 48);
                                continue;
                            }
                            this._uri.reset();
                            this.setState(State.URI);
                            if (!buffer.hasArray()) {
                                this._uri.append(ch);
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
                            this._uri.append(array, p - 1, len + 1);
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
                            this.setState(State.HEADER);
                            handle = (this._responseHandler.startResponse(this._version, this._responseStatus, null) || handle);
                            continue;
                        }
                        throw new BadMessageException();
                    }
                    case URI: {
                        if (ch == 32) {
                            this.setState(State.SPACE2);
                            continue;
                        }
                        if (ch >= 32 || ch < 0) {
                            this._uri.append(ch);
                            continue;
                        }
                        if (this.complianceViolation(HttpCompliance.RFC7230, "HTTP/0.9")) {
                            throw new BadMessageException("HTTP/0.9 not supported");
                        }
                        handle = this._requestHandler.startRequest(this._methodString, this._uri.toString(), HttpVersion.HTTP_0_9);
                        this.setState(State.END);
                        BufferUtil.clear(buffer);
                        handle = (this.handleHeaderContentMessage() || handle);
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
                                continue;
                            }
                            final int pos = buffer.position() + version2.asString().length() - 1;
                            if (pos >= buffer.limit()) {
                                continue;
                            }
                            final byte n = buffer.get(pos);
                            if (n == 13) {
                                this._cr = true;
                                this._version = version2;
                                this.checkVersion();
                                this._string.setLength(0);
                                buffer.position(pos + 1);
                            }
                            else {
                                if (n != 10) {
                                    continue;
                                }
                                this._version = version2;
                                this.checkVersion();
                                this._string.setLength(0);
                                buffer.position(pos);
                            }
                            continue;
                        }
                        else if (ch == 10) {
                            if (this._responseHandler != null) {
                                this.setState(State.HEADER);
                                handle = (this._responseHandler.startResponse(this._version, this._responseStatus, null) || handle);
                                continue;
                            }
                            if (this.complianceViolation(HttpCompliance.RFC7230, "HTTP/0.9")) {
                                throw new BadMessageException("HTTP/0.9 not supported");
                            }
                            handle = this._requestHandler.startRequest(this._methodString, this._uri.toString(), HttpVersion.HTTP_0_9);
                            this.setState(State.END);
                            BufferUtil.clear(buffer);
                            handle = (this.handleHeaderContentMessage() || handle);
                            continue;
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
                            this.checkVersion();
                            if (this._connectionFields == null && this._version.getVersion() >= HttpVersion.HTTP_1_1.getVersion() && this._handler.getHeaderCacheSize() > 0) {
                                final int header_cache = this._handler.getHeaderCacheSize();
                                this._connectionFields = (Trie<HttpField>)new ArrayTernaryTrie(header_cache);
                            }
                            this.setState(State.HEADER);
                            handle = (this._requestHandler.startRequest(this._methodString, this._uri.toString(), this._version) || handle);
                            continue;
                        }
                        if (ch >= 32) {
                            this._string.append((char)ch);
                            continue;
                        }
                        throw new BadMessageException();
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
    
    private void checkVersion() {
        if (this._version == null) {
            throw new BadMessageException(400, "Unknown Version");
        }
        if (this._version.getVersion() < 10 || this._version.getVersion() > 20) {
            throw new BadMessageException(400, "Bad Version");
        }
    }
    
    private void parsedHeader() {
        if (this._headerString != null || this._valueString != null) {
            if (this._header != null) {
                boolean add_to_connection_trie = false;
                switch (this._header) {
                    case CONTENT_LENGTH: {
                        if (this._hasContentLength && this.complianceViolation(HttpCompliance.RFC7230, "Duplicate Content-Lengths")) {
                            throw new BadMessageException(400, "Duplicate Content-Lengths");
                        }
                        this._hasContentLength = true;
                        if (this._endOfContent == HttpTokens.EndOfContent.CHUNKED_CONTENT && this.complianceViolation(HttpCompliance.RFC7230, "Chunked and Content-Length")) {
                            throw new BadMessageException(400, "Bad Content-Length");
                        }
                        if (this._endOfContent == HttpTokens.EndOfContent.CHUNKED_CONTENT) {
                            break;
                        }
                        this._contentLength = this.convertContentLength(this._valueString);
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
                            this._contentLength = -1L;
                        }
                        else if (this._valueString.endsWith(HttpHeaderValue.CHUNKED.toString())) {
                            this._endOfContent = HttpTokens.EndOfContent.CHUNKED_CONTENT;
                        }
                        else if (this._valueString.contains(HttpHeaderValue.CHUNKED.toString())) {
                            throw new BadMessageException(400, "Bad chunking");
                        }
                        if (this._hasContentLength && this._endOfContent == HttpTokens.EndOfContent.CHUNKED_CONTENT && this.complianceViolation(HttpCompliance.RFC7230, "Chunked and Content-Length")) {
                            throw new BadMessageException(400, "Chunked and Content-Length");
                        }
                        break;
                    }
                    case HOST: {
                        this._host = true;
                        if (!(this._field instanceof HostPortHttpField) && this._valueString != null && !this._valueString.isEmpty()) {
                            this._field = new HostPortHttpField(this._header, this.legacyString(this._headerString, this._header.asString()), this._valueString);
                            add_to_connection_trie = (this._connectionFields != null);
                            break;
                        }
                        break;
                    }
                    case CONNECTION: {
                        if (this._valueString != null && this._valueString.contains("close")) {
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
                    if (this._field == null) {
                        this._field = new HttpField(this._header, this.legacyString(this._headerString, this._header.asString()), this._valueString);
                    }
                    this._connectionFields.put((Object)this._field);
                }
            }
            this._handler.parsedHeader((this._field != null) ? this._field : new HttpField(this._header, this._headerString, this._valueString));
        }
        final String s = null;
        this._valueString = s;
        this._headerString = s;
        this._header = null;
        this._value = null;
        this._field = null;
    }
    
    private long convertContentLength(final String valueString) {
        try {
            return Long.parseLong(valueString);
        }
        catch (NumberFormatException e) {
            HttpParser.LOG.ignore((Throwable)e);
            throw new BadMessageException(400, "Invalid Content-Length Value");
        }
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
                throw new BadMessageException(431);
            }
            switch (this._state) {
                case HEADER: {
                    switch (ch) {
                        case 9:
                        case 32:
                        case 58: {
                            if (this.complianceViolation(HttpCompliance.RFC7230, "header folding")) {
                                throw new BadMessageException(400, "Header Folding");
                            }
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
                        case 10: {
                            this.parsedHeader();
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
                                    this._headerComplete = true;
                                    return handle;
                                }
                                case CHUNKED_CONTENT: {
                                    this.setState(State.CHUNKED_CONTENT);
                                    handle = (this._handler.headerComplete() || handle);
                                    this._headerComplete = true;
                                    return handle;
                                }
                                case NO_CONTENT: {
                                    this.setState(State.END);
                                    return this.handleHeaderContentMessage();
                                }
                                default: {
                                    this.setState(State.CONTENT);
                                    handle = (this._handler.headerComplete() || handle);
                                    this._headerComplete = true;
                                    return handle;
                                }
                            }
                            break;
                        }
                        default: {
                            if (ch < 32) {
                                throw new BadMessageException();
                            }
                            this.parsedHeader();
                            if (buffer.hasRemaining()) {
                                HttpField field = (this._connectionFields == null) ? null : ((HttpField)this._connectionFields.getBest(buffer, -1, buffer.remaining()));
                                if (field == null) {
                                    field = (HttpField)HttpParser.CACHE.getBest(buffer, -1, buffer.remaining());
                                }
                                if (field != null) {
                                    String n;
                                    String v;
                                    if (this._compliance == HttpCompliance.LEGACY) {
                                        final String fn = field.getName();
                                        n = this.legacyString(BufferUtil.toString(buffer, buffer.position() - 1, fn.length(), StandardCharsets.US_ASCII), fn);
                                        final String fv = field.getValue();
                                        if (fv == null) {
                                            v = null;
                                        }
                                        else {
                                            v = this.legacyString(BufferUtil.toString(buffer, buffer.position() + fn.length() + 1, fv.length(), StandardCharsets.ISO_8859_1), fv);
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
                    }
                    break;
                }
                case HEADER_IN_NAME: {
                    if (ch == 58) {
                        if (this._headerString == null) {
                            this._headerString = this.takeString();
                            this._header = (HttpHeader)HttpHeader.CACHE.get(this._headerString);
                        }
                        this._length = -1;
                        this.setState(State.HEADER_VALUE);
                        continue;
                    }
                    if (ch > 32) {
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
                    else {
                        if (ch == 10 && !this.complianceViolation(HttpCompliance.RFC7230, "name only header")) {
                            if (this._headerString == null) {
                                this._headerString = this.takeString();
                                this._header = (HttpHeader)HttpHeader.CACHE.get(this._headerString);
                            }
                            this._value = null;
                            this._string.setLength(0);
                            this._valueString = "";
                            this._length = -1;
                            this.setState(State.HEADER);
                            continue;
                        }
                        throw new IllegalCharacterException(this._state, ch, buffer);
                    }
                    break;
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
                        this._value = null;
                        this._string.setLength(0);
                        this._valueString = "";
                        this._length = -1;
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
        while (true) {
            if (this.DEBUG) {
                HttpParser.LOG.debug("parseNext s={} {}", new Object[] { this._state, BufferUtil.toDetailString(buffer) });
                try {
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
                                return this.handleContentMessage();
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
                        else if (this._state == State.CLOSE) {
                            if (BufferUtil.hasContent(buffer)) {
                                this._headerBytes += buffer.remaining();
                                BufferUtil.clear(buffer);
                                if (this._maxHeaderBytes > 0 && this._headerBytes > this._maxHeaderBytes) {
                                    throw new IllegalStateException("too much data seeking EOF");
                                }
                            }
                        }
                        else if (this._state == State.CLOSED) {
                            BufferUtil.clear(buffer);
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
                                case END:
                                case CLOSE: {
                                    this.setState(State.CLOSED);
                                    break;
                                }
                                case EOF_CONTENT:
                                case CHUNK_END: {
                                    this.setState(State.CLOSED);
                                    return this.handleContentMessage();
                                }
                                case CONTENT:
                                case CHUNKED_CONTENT:
                                case CHUNK_SIZE:
                                case CHUNK_PARAMS:
                                case CHUNK:
                                case CHUNK_TRAILER: {
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
                        final Throwable cause = e.getCause();
                        final boolean stack = HttpParser.LOG.isDebugEnabled() || (!(cause instanceof NumberFormatException) && (cause instanceof RuntimeException || cause instanceof Error));
                        if (stack) {
                            HttpParser.LOG.warn("bad HTTP parsed: " + e._code + ((e.getReason() != null) ? (" " + e.getReason()) : "") + " for " + this._handler, (Throwable)e);
                        }
                        else {
                            HttpParser.LOG.warn("bad HTTP parsed: " + e._code + ((e.getReason() != null) ? (" " + e.getReason()) : "") + " for " + this._handler, new Object[0]);
                        }
                        this.setState(State.CLOSE);
                        this._handler.badMessage(e.getCode(), e.getReason());
                    }
                    catch (IllegalStateException e2) {
                        BufferUtil.clear(buffer);
                        HttpParser.LOG.warn("parse exception: {} in {} for {}", new Object[] { e2.toString(), this._state, this._handler });
                        if (this.DEBUG) {
                            HttpParser.LOG.debug((Throwable)e2);
                        }
                        this.badMessage();
                    }
                    catch (Error e3) {
                        BufferUtil.clear(buffer);
                        HttpParser.LOG.warn("parse exception: " + e3.toString() + " for " + this._handler, e3);
                        this.badMessage();
                    }
                }
                catch (NumberFormatException ex) {}
                catch (Exception ex2) {}
                return false;
            }
            continue;
        }
    }
    
    protected void badMessage() {
        if (this._headerComplete) {
            this._handler.earlyEOF();
        }
        else if (this._state != State.CLOSED) {
            this.setState(State.CLOSE);
            this._handler.badMessage(400, (this._requestHandler != null) ? "Bad Request" : "Bad Response");
        }
    }
    
    protected boolean parseContent(final ByteBuffer buffer) {
        int remaining = buffer.remaining();
        if (remaining == 0 && this._state == State.CONTENT) {
            final long content = this._contentLength - this._contentPosition;
            if (content == 0L) {
                this.setState(State.END);
                return this.handleContentMessage();
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
                        return this.handleContentMessage();
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
                        return this.handleContentMessage();
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
                        if (this._chunkLength != 0) {
                            this.setState(State.CHUNK);
                            break;
                        }
                        this.setState(State.CHUNK_END);
                        if (this._handler.contentComplete()) {
                            return true;
                        }
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
                    if (this._chunkLength != 0) {
                        this.setState(State.CHUNK);
                        break;
                    }
                    this.setState(State.CHUNK_END);
                    if (this._handler.contentComplete()) {
                        return true;
                    }
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
                    this.setState(State.CHUNK_TRAILER);
                    break;
                }
                case CHUNK_TRAILER: {
                    final byte ch = this.next(buffer);
                    if (ch == 0) {
                        break;
                    }
                    if (ch == 10) {
                        this.setState(State.CHUNK_END);
                        break;
                    }
                    break;
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
        this.setState(State.CLOSE);
    }
    
    public void reset() {
        if (this.DEBUG) {
            HttpParser.LOG.debug("reset {}", new Object[] { this });
        }
        if (this._state == State.CLOSE || this._state == State.CLOSED) {
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
        this._headerComplete = false;
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
        CACHE = (Trie)new ArrayTrie(2048);
        __idleStates = EnumSet.of(State.START, State.END, State.CLOSE, State.CLOSED);
        __completeStates = EnumSet.of(State.END, State.CLOSE, State.CLOSED);
        HttpParser.CACHE.put((Object)new HttpField(HttpHeader.CONNECTION, HttpHeaderValue.CLOSE));
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
            final HttpField field = new PreEncodedHttpField(HttpHeader.CONTENT_TYPE, type);
            HttpParser.CACHE.put((Object)field);
            for (final String charset : new String[] { "utf-8", "iso-8859-1" }) {
                HttpParser.CACHE.put((Object)new PreEncodedHttpField(HttpHeader.CONTENT_TYPE, type + ";charset=" + charset));
                HttpParser.CACHE.put((Object)new PreEncodedHttpField(HttpHeader.CONTENT_TYPE, type + "; charset=" + charset));
                HttpParser.CACHE.put((Object)new PreEncodedHttpField(HttpHeader.CONTENT_TYPE, type + ";charset=" + charset.toUpperCase(Locale.ENGLISH)));
                HttpParser.CACHE.put((Object)new PreEncodedHttpField(HttpHeader.CONTENT_TYPE, type + "; charset=" + charset.toUpperCase(Locale.ENGLISH)));
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
        Arrays.fill(__charState = new CharState[256], CharState.ILLEGAL);
        HttpParser.__charState[10] = CharState.LF;
        HttpParser.__charState[13] = CharState.CR;
        HttpParser.__charState[9] = CharState.LEGAL;
        HttpParser.__charState[32] = CharState.LEGAL;
        HttpParser.__charState[33] = CharState.LEGAL;
        HttpParser.__charState[35] = CharState.LEGAL;
        HttpParser.__charState[36] = CharState.LEGAL;
        HttpParser.__charState[37] = CharState.LEGAL;
        HttpParser.__charState[38] = CharState.LEGAL;
        HttpParser.__charState[39] = CharState.LEGAL;
        HttpParser.__charState[42] = CharState.LEGAL;
        HttpParser.__charState[43] = CharState.LEGAL;
        HttpParser.__charState[45] = CharState.LEGAL;
        HttpParser.__charState[46] = CharState.LEGAL;
        HttpParser.__charState[94] = CharState.LEGAL;
        HttpParser.__charState[95] = CharState.LEGAL;
        HttpParser.__charState[96] = CharState.LEGAL;
        HttpParser.__charState[124] = CharState.LEGAL;
        HttpParser.__charState[126] = CharState.LEGAL;
        HttpParser.__charState[34] = CharState.LEGAL;
        HttpParser.__charState[92] = CharState.LEGAL;
        HttpParser.__charState[40] = CharState.LEGAL;
        HttpParser.__charState[41] = CharState.LEGAL;
        Arrays.fill(HttpParser.__charState, 33, 40, CharState.LEGAL);
        Arrays.fill(HttpParser.__charState, 42, 92, CharState.LEGAL);
        Arrays.fill(HttpParser.__charState, 93, 127, CharState.LEGAL);
        Arrays.fill(HttpParser.__charState, 128, 256, CharState.LEGAL);
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
        CHUNK_TRAILER, 
        CHUNK_END, 
        END, 
        CLOSE, 
        CLOSED;
    }
    
    enum CharState
    {
        ILLEGAL, 
        CR, 
        LF, 
        LEGAL;
    }
    
    private static class IllegalCharacterException extends BadMessageException
    {
        private IllegalCharacterException(final State state, final byte ch, final ByteBuffer buffer) {
            super(400, String.format("Illegal character 0x%X", ch));
            HttpParser.LOG.warn(String.format("Illegal character 0x%X in state=%s for buffer %s", ch, state, BufferUtil.toDetailString(buffer)), new Object[0]);
        }
    }
    
    public interface ComplianceHandler extends HttpHandler
    {
        void onComplianceViolation(final HttpCompliance p0, final HttpCompliance p1, final String p2);
    }
    
    public interface ResponseHandler extends HttpHandler
    {
        boolean startResponse(final HttpVersion p0, final int p1, final String p2);
    }
    
    public interface HttpHandler
    {
        boolean content(final ByteBuffer p0);
        
        boolean headerComplete();
        
        boolean contentComplete();
        
        boolean messageComplete();
        
        void parsedHeader(final HttpField p0);
        
        void earlyEOF();
        
        void badMessage(final int p0, final String p1);
        
        int getHeaderCacheSize();
    }
    
    public interface RequestHandler extends HttpHandler
    {
        boolean startRequest(final String p0, final String p1, final HttpVersion p2);
    }
}
