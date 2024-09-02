// 
// Decompiled by Procyon v0.5.36
// 

package org.eclipse.jetty.http;

import java.util.Locale;
import org.eclipse.jetty.util.ArrayTrie;
import org.eclipse.jetty.util.log.Log;
import java.nio.charset.StandardCharsets;
import org.eclipse.jetty.util.StringUtil;
import java.util.stream.Stream;
import java.util.List;
import java.util.function.Predicate;
import java.util.Objects;
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
    private final EnumSet<HttpComplianceSection> _compliances;
    private HttpField _field;
    private HttpHeader _header;
    private String _headerString;
    private String _valueString;
    private int _responseStatus;
    private int _headerBytes;
    private boolean _host;
    private boolean _headerComplete;
    private volatile State _state;
    private volatile FieldState _fieldState;
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
    private Trie<HttpField> _fieldCache;
    private int _length;
    private final StringBuilder _string;
    
    private static HttpCompliance compliance() {
        final Boolean strict = Boolean.getBoolean("org.eclipse.jetty.http.HttpParser.STRICT");
        if (strict) {
            HttpParser.LOG.warn("Deprecated property used: org.eclipse.jetty.http.HttpParser.STRICT", new Object[0]);
            return HttpCompliance.LEGACY;
        }
        return HttpCompliance.RFC7230;
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
        this(handler, null, maxHeaderBytes, (compliance == null) ? compliance() : compliance);
    }
    
    public HttpParser(final ResponseHandler handler, final int maxHeaderBytes, final HttpCompliance compliance) {
        this(null, handler, maxHeaderBytes, (compliance == null) ? compliance() : compliance);
    }
    
    private HttpParser(final RequestHandler requestHandler, final ResponseHandler responseHandler, final int maxHeaderBytes, final HttpCompliance compliance) {
        this.DEBUG = HttpParser.LOG.isDebugEnabled();
        this._state = State.START;
        this._fieldState = FieldState.FIELD;
        this._uri = new Utf8StringBuilder(256);
        this._contentLength = -1L;
        this._string = new StringBuilder();
        this._handler = (HttpHandler)((requestHandler != null) ? requestHandler : responseHandler);
        this._requestHandler = requestHandler;
        this._responseHandler = responseHandler;
        this._maxHeaderBytes = maxHeaderBytes;
        this._compliance = compliance;
        this._compliances = compliance.sections();
        this._complianceHandler = (ComplianceHandler)((this._handler instanceof ComplianceHandler) ? this._handler : null);
    }
    
    public HttpHandler getHandler() {
        return this._handler;
    }
    
    protected boolean complianceViolation(final HttpComplianceSection violation) {
        return this.complianceViolation(violation, null);
    }
    
    protected boolean complianceViolation(final HttpComplianceSection violation, String reason) {
        if (this._compliances.contains(violation)) {
            return true;
        }
        if (reason == null) {
            reason = violation.description;
        }
        if (this._complianceHandler != null) {
            this._complianceHandler.onComplianceViolation(this._compliance, violation, reason);
        }
        return false;
    }
    
    protected void handleViolation(final HttpComplianceSection section, final String reason) {
        if (this._complianceHandler != null) {
            this._complianceHandler.onComplianceViolation(this._compliance, section, reason);
        }
    }
    
    protected String caseInsensitiveHeader(final String orig, final String normative) {
        if (this._compliances.contains(HttpComplianceSection.FIELD_NAME_CASE_INSENSITIVE)) {
            return normative;
        }
        if (!orig.equals(normative)) {
            this.handleViolation(HttpComplianceSection.FIELD_NAME_CASE_INSENSITIVE, orig);
        }
        return orig;
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
    
    private HttpTokens.Token next(final ByteBuffer buffer) {
        final byte ch = buffer.get();
        final HttpTokens.Token t = HttpTokens.TOKENS[0xFF & ch];
        switch (t.getType()) {
            case CNTL: {
                throw new IllegalCharacterException(this._state, t, buffer);
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
                    if (this._maxHeaderBytes > 0 && (this._state == State.HEADER || this._state == State.TRAILER)) {
                        ++this._headerBytes;
                    }
                    return this.next(buffer);
                }
                return null;
            }
            case ALPHA:
            case DIGIT:
            case TCHAR:
            case VCHAR:
            case HTAB:
            case SPACE:
            case OTEXT:
            case COLON: {
                if (this._cr) {
                    throw new BadMessageException("Bad EOL");
                }
                break;
            }
        }
        return t;
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
            final HttpTokens.Token t = this.next(buffer);
            if (t == null) {
                break;
            }
            switch (t.getType()) {
                case ALPHA:
                case DIGIT:
                case TCHAR:
                case VCHAR: {
                    this._string.setLength(0);
                    this._string.append(t.getChar());
                    this.setState((this._requestHandler != null) ? State.METHOD : State.RESPONSE_VERSION);
                    return false;
                }
                case HTAB:
                case SPACE:
                case OTEXT: {
                    throw new IllegalCharacterException(this._state, t, buffer);
                }
                default: {
                    if (this._maxHeaderBytes > 0 && ++this._headerBytes > this._maxHeaderBytes) {
                        HttpParser.LOG.warn("padding is too large >" + this._maxHeaderBytes, new Object[0]);
                        throw new BadMessageException(400);
                    }
                    continue;
                }
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
        this._headerComplete = true;
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
            final HttpTokens.Token t = this.next(buffer);
            if (t == null) {
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
                        switch (t.getType()) {
                            case SPACE: {
                                this._length = this._string.length();
                                this._methodString = this.takeString();
                                if (this._compliances.contains(HttpComplianceSection.METHOD_CASE_SENSITIVE)) {
                                    final HttpMethod method = (HttpMethod)HttpMethod.CACHE.get(this._methodString);
                                    if (method != null) {
                                        this._methodString = method.asString();
                                    }
                                }
                                else {
                                    final HttpMethod method = (HttpMethod)HttpMethod.INSENSITIVE_CACHE.get(this._methodString);
                                    if (method != null) {
                                        if (!method.asString().equals(this._methodString)) {
                                            this.handleViolation(HttpComplianceSection.METHOD_CASE_SENSITIVE, this._methodString);
                                        }
                                        this._methodString = method.asString();
                                    }
                                }
                                this.setState(State.SPACE1);
                                continue;
                            }
                            case LF: {
                                throw new BadMessageException("No URI");
                            }
                            case ALPHA:
                            case DIGIT:
                            case TCHAR: {
                                this._string.append(t.getChar());
                                continue;
                            }
                            default: {
                                throw new IllegalCharacterException(this._state, t, buffer);
                            }
                        }
                        break;
                    }
                    case RESPONSE_VERSION: {
                        switch (t.getType()) {
                            case SPACE: {
                                this._length = this._string.length();
                                final String version = this.takeString();
                                this._version = (HttpVersion)HttpVersion.CACHE.get(version);
                                this.checkVersion();
                                this.setState(State.SPACE1);
                                continue;
                            }
                            case ALPHA:
                            case DIGIT:
                            case TCHAR:
                            case VCHAR:
                            case COLON: {
                                this._string.append(t.getChar());
                                continue;
                            }
                            default: {
                                throw new IllegalCharacterException(this._state, t, buffer);
                            }
                        }
                        break;
                    }
                    case SPACE1: {
                        switch (t.getType()) {
                            case SPACE: {
                                continue;
                            }
                            case ALPHA:
                            case DIGIT:
                            case TCHAR:
                            case VCHAR:
                            case COLON: {
                                if (this._responseHandler != null) {
                                    if (t.getType() != HttpTokens.Type.DIGIT) {
                                        throw new IllegalCharacterException(this._state, t, buffer);
                                    }
                                    this.setState(State.STATUS);
                                    this.setResponseStatus(t.getByte() - 48);
                                    continue;
                                }
                                else {
                                    this._uri.reset();
                                    this.setState(State.URI);
                                    if (!buffer.hasArray()) {
                                        this._uri.append(t.getByte());
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
                                break;
                            }
                            default: {
                                throw new BadMessageException(400, (this._requestHandler != null) ? "No URI" : "No Status");
                            }
                        }
                        break;
                    }
                    case STATUS: {
                        switch (t.getType()) {
                            case SPACE: {
                                this.setState(State.SPACE2);
                                continue;
                            }
                            case DIGIT: {
                                this._responseStatus = this._responseStatus * 10 + (t.getByte() - 48);
                                if (this._responseStatus >= 1000) {
                                    throw new BadMessageException("Bad status");
                                }
                                continue;
                            }
                            case LF: {
                                this.setState(State.HEADER);
                                handle |= this._responseHandler.startResponse(this._version, this._responseStatus, null);
                                continue;
                            }
                            default: {
                                throw new IllegalCharacterException(this._state, t, buffer);
                            }
                        }
                        break;
                    }
                    case URI: {
                        switch (t.getType()) {
                            case SPACE: {
                                this.setState(State.SPACE2);
                                continue;
                            }
                            case LF: {
                                if (this.complianceViolation(HttpComplianceSection.NO_HTTP_0_9, "No request version")) {
                                    throw new BadMessageException("HTTP/0.9 not supported");
                                }
                                handle = this._requestHandler.startRequest(this._methodString, this._uri.toString(), HttpVersion.HTTP_0_9);
                                this.setState(State.END);
                                BufferUtil.clear(buffer);
                                handle |= this.handleHeaderContentMessage();
                                continue;
                            }
                            case ALPHA:
                            case DIGIT:
                            case TCHAR:
                            case VCHAR:
                            case OTEXT:
                            case COLON: {
                                this._uri.append(t.getByte());
                                continue;
                            }
                            default: {
                                throw new IllegalCharacterException(this._state, t, buffer);
                            }
                        }
                        break;
                    }
                    case SPACE2: {
                        switch (t.getType()) {
                            case SPACE: {
                                continue;
                            }
                            case ALPHA:
                            case DIGIT:
                            case TCHAR:
                            case VCHAR:
                            case COLON: {
                                this._string.setLength(0);
                                this._string.append(t.getChar());
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
                            case LF: {
                                if (this._responseHandler != null) {
                                    this.setState(State.HEADER);
                                    handle |= this._responseHandler.startResponse(this._version, this._responseStatus, null);
                                    continue;
                                }
                                if (this.complianceViolation(HttpComplianceSection.NO_HTTP_0_9, "No request version")) {
                                    throw new BadMessageException("HTTP/0.9 not supported");
                                }
                                handle = this._requestHandler.startRequest(this._methodString, this._uri.toString(), HttpVersion.HTTP_0_9);
                                this.setState(State.END);
                                BufferUtil.clear(buffer);
                                handle |= this.handleHeaderContentMessage();
                                continue;
                            }
                            default: {
                                throw new IllegalCharacterException(this._state, t, buffer);
                            }
                        }
                        break;
                    }
                    case REQUEST_VERSION: {
                        switch (t.getType()) {
                            case LF: {
                                if (this._version == null) {
                                    this._length = this._string.length();
                                    this._version = (HttpVersion)HttpVersion.CACHE.get(this.takeString());
                                }
                                this.checkVersion();
                                if (this._fieldCache == null && this._version.getVersion() >= HttpVersion.HTTP_1_1.getVersion() && this._handler.getHeaderCacheSize() > 0) {
                                    final int header_cache = this._handler.getHeaderCacheSize();
                                    this._fieldCache = (Trie<HttpField>)new ArrayTernaryTrie(header_cache);
                                }
                                this.setState(State.HEADER);
                                handle |= this._requestHandler.startRequest(this._methodString, this._uri.toString(), this._version);
                                continue;
                            }
                            case ALPHA:
                            case DIGIT:
                            case TCHAR:
                            case VCHAR:
                            case COLON: {
                                this._string.append(t.getChar());
                                continue;
                            }
                            default: {
                                throw new IllegalCharacterException(this._state, t, buffer);
                            }
                        }
                        break;
                    }
                    case REASON: {
                        switch (t.getType()) {
                            case LF: {
                                final String reason = this.takeString();
                                this.setState(State.HEADER);
                                handle |= this._responseHandler.startResponse(this._version, this._responseStatus, reason);
                                continue;
                            }
                            case ALPHA:
                            case DIGIT:
                            case TCHAR:
                            case VCHAR:
                            case OTEXT:
                            case COLON: {
                                this._string.append(t.getChar());
                                this._length = this._string.length();
                                continue;
                            }
                            case HTAB:
                            case SPACE: {
                                this._string.append(t.getChar());
                                continue;
                            }
                            default: {
                                throw new IllegalCharacterException(this._state, t, buffer);
                            }
                        }
                        break;
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
                        if (this._hasContentLength) {
                            if (this.complianceViolation(HttpComplianceSection.MULTIPLE_CONTENT_LENGTHS)) {
                                throw new BadMessageException(400, HttpComplianceSection.MULTIPLE_CONTENT_LENGTHS.description);
                            }
                            if (this.convertContentLength(this._valueString) != this._contentLength) {
                                throw new BadMessageException(400, HttpComplianceSection.MULTIPLE_CONTENT_LENGTHS.description);
                            }
                        }
                        this._hasContentLength = true;
                        if (this._endOfContent == HttpTokens.EndOfContent.CHUNKED_CONTENT && this.complianceViolation(HttpComplianceSection.TRANSFER_ENCODING_WITH_CONTENT_LENGTH)) {
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
                        if (this._hasContentLength && this.complianceViolation(HttpComplianceSection.TRANSFER_ENCODING_WITH_CONTENT_LENGTH)) {
                            throw new BadMessageException(400, "Transfer-Encoding and Content-Length");
                        }
                        if (HttpHeaderValue.CHUNKED.is(this._valueString)) {
                            this._endOfContent = HttpTokens.EndOfContent.CHUNKED_CONTENT;
                            this._contentLength = -1L;
                            break;
                        }
                        final List<String> values = new QuotedCSV(new String[] { this._valueString }).getValues();
                        if (!values.isEmpty() && HttpHeaderValue.CHUNKED.is(values.get(values.size() - 1))) {
                            this._endOfContent = HttpTokens.EndOfContent.CHUNKED_CONTENT;
                            this._contentLength = -1L;
                        }
                        else {
                            final Stream<Object> stream = values.stream();
                            final HttpHeaderValue chunked = HttpHeaderValue.CHUNKED;
                            Objects.requireNonNull(chunked);
                            if (stream.anyMatch((Predicate<? super Object>)chunked::is)) {
                                throw new BadMessageException(400, "Bad chunking");
                            }
                        }
                        break;
                    }
                    case HOST: {
                        this._host = true;
                        if (!(this._field instanceof HostPortHttpField) && this._valueString != null && !this._valueString.isEmpty()) {
                            this._field = new HostPortHttpField(this._header, this._compliances.contains(HttpComplianceSection.FIELD_NAME_CASE_INSENSITIVE) ? this._header.asString() : this._headerString, this._valueString);
                            add_to_connection_trie = (this._fieldCache != null);
                            break;
                        }
                        break;
                    }
                    case CONNECTION: {
                        if (!HttpHeaderValue.CLOSE.is(this._valueString)) {
                            final Stream<Object> stream2 = new QuotedCSV(new String[] { this._valueString }).getValues().stream();
                            final HttpHeaderValue close = HttpHeaderValue.CLOSE;
                            Objects.requireNonNull(close);
                            if (!stream2.anyMatch((Predicate<? super Object>)close::is)) {
                                break;
                            }
                        }
                        this._fieldCache = null;
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
                        add_to_connection_trie = (this._fieldCache != null && this._field == null);
                        break;
                    }
                }
                if (add_to_connection_trie && !this._fieldCache.isFull() && this._header != null && this._valueString != null) {
                    if (this._field == null) {
                        this._field = new HttpField(this._header, this.caseInsensitiveHeader(this._headerString, this._header.asString()), this._valueString);
                    }
                    this._fieldCache.put((Object)this._field);
                }
            }
            this._handler.parsedHeader((this._field != null) ? this._field : new HttpField(this._header, this._headerString, this._valueString));
        }
        final String s = null;
        this._valueString = s;
        this._headerString = s;
        this._header = null;
        this._field = null;
    }
    
    private void parsedTrailer() {
        if (this._headerString != null || this._valueString != null) {
            this._handler.parsedTrailer((this._field != null) ? this._field : new HttpField(this._header, this._headerString, this._valueString));
        }
        final String s = null;
        this._valueString = s;
        this._headerString = s;
        this._header = null;
        this._field = null;
    }
    
    private long convertContentLength(final String valueString) {
        try {
            return Long.parseLong(valueString);
        }
        catch (NumberFormatException e) {
            HttpParser.LOG.ignore((Throwable)e);
            throw new BadMessageException(400, "Invalid Content-Length Value", e);
        }
    }
    
    protected boolean parseFields(final ByteBuffer buffer) {
        while ((this._state == State.HEADER || this._state == State.TRAILER) && buffer.hasRemaining()) {
            final HttpTokens.Token t = this.next(buffer);
            if (t == null) {
                break;
            }
            if (this._maxHeaderBytes > 0 && ++this._headerBytes > this._maxHeaderBytes) {
                final boolean header = this._state == State.HEADER;
                HttpParser.LOG.warn("{} is too large {}>{}", new Object[] { header ? "Header" : "Trailer", this._headerBytes, this._maxHeaderBytes });
                throw new BadMessageException(header ? 431 : 413);
            }
            switch (this._fieldState) {
                case FIELD: {
                    switch (t.getType()) {
                        case HTAB:
                        case SPACE:
                        case COLON: {
                            if (this.complianceViolation(HttpComplianceSection.NO_FIELD_FOLDING, this._headerString)) {
                                throw new BadMessageException(400, "Header Folding");
                            }
                            if (StringUtil.isEmpty(this._valueString)) {
                                this._string.setLength(0);
                                this._length = 0;
                            }
                            else {
                                this.setString(this._valueString);
                                this._string.append(' ');
                                ++this._length;
                                this._valueString = null;
                            }
                            this.setState(FieldState.VALUE);
                            continue;
                        }
                        case LF: {
                            if (this._state == State.HEADER) {
                                this.parsedHeader();
                            }
                            else {
                                this.parsedTrailer();
                            }
                            this._contentPosition = 0L;
                            if (this._state == State.TRAILER) {
                                this.setState(State.END);
                                return this._handler.messageComplete();
                            }
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
                                    final boolean handle = this._handler.headerComplete();
                                    this._headerComplete = true;
                                    return handle;
                                }
                                case CHUNKED_CONTENT: {
                                    this.setState(State.CHUNKED_CONTENT);
                                    final boolean handle = this._handler.headerComplete();
                                    this._headerComplete = true;
                                    return handle;
                                }
                                case NO_CONTENT: {
                                    this.setState(State.END);
                                    return this.handleHeaderContentMessage();
                                }
                                default: {
                                    this.setState(State.CONTENT);
                                    final boolean handle = this._handler.headerComplete();
                                    this._headerComplete = true;
                                    return handle;
                                }
                            }
                            break;
                        }
                        case ALPHA:
                        case DIGIT:
                        case TCHAR: {
                            if (this._state == State.HEADER) {
                                this.parsedHeader();
                            }
                            else {
                                this.parsedTrailer();
                            }
                            if (buffer.hasRemaining()) {
                                HttpField cached_field = (this._fieldCache == null) ? null : ((HttpField)this._fieldCache.getBest(buffer, -1, buffer.remaining()));
                                if (cached_field == null) {
                                    cached_field = (HttpField)HttpParser.CACHE.getBest(buffer, -1, buffer.remaining());
                                }
                                if (cached_field != null) {
                                    String n = cached_field.getName();
                                    String v = cached_field.getValue();
                                    if (!this._compliances.contains(HttpComplianceSection.FIELD_NAME_CASE_INSENSITIVE)) {
                                        final String en = BufferUtil.toString(buffer, buffer.position() - 1, n.length(), StandardCharsets.US_ASCII);
                                        if (!n.equals(en)) {
                                            this.handleViolation(HttpComplianceSection.FIELD_NAME_CASE_INSENSITIVE, en);
                                            n = en;
                                            cached_field = new HttpField(cached_field.getHeader(), n, v);
                                        }
                                    }
                                    if (v != null && !this._compliances.contains(HttpComplianceSection.CASE_INSENSITIVE_FIELD_VALUE_CACHE)) {
                                        final String ev = BufferUtil.toString(buffer, buffer.position() + n.length() + 1, v.length(), StandardCharsets.ISO_8859_1);
                                        if (!v.equals(ev)) {
                                            this.handleViolation(HttpComplianceSection.CASE_INSENSITIVE_FIELD_VALUE_CACHE, ev + "!=" + v);
                                            v = ev;
                                            cached_field = new HttpField(cached_field.getHeader(), n, v);
                                        }
                                    }
                                    this._header = cached_field.getHeader();
                                    this._headerString = n;
                                    if (v == null) {
                                        this.setState(FieldState.VALUE);
                                        this._string.setLength(0);
                                        this._length = 0;
                                        buffer.position(buffer.position() + n.length() + 1);
                                        continue;
                                    }
                                    final int pos = buffer.position() + n.length() + v.length() + 1;
                                    final byte peek = buffer.get(pos);
                                    if (peek != 13 && peek != 10) {
                                        this.setState(FieldState.IN_VALUE);
                                        this.setString(v);
                                        buffer.position(pos);
                                        continue;
                                    }
                                    this._field = cached_field;
                                    this._valueString = v;
                                    this.setState(FieldState.IN_VALUE);
                                    if (peek == 13) {
                                        this._cr = true;
                                        buffer.position(pos + 1);
                                        continue;
                                    }
                                    buffer.position(pos);
                                    continue;
                                }
                            }
                            this.setState(FieldState.IN_NAME);
                            this._string.setLength(0);
                            this._string.append(t.getChar());
                            this._length = 1;
                            continue;
                        }
                        default: {
                            throw new IllegalCharacterException(this._state, t, buffer);
                        }
                    }
                    break;
                }
                case IN_NAME: {
                    switch (t.getType()) {
                        case HTAB:
                        case SPACE: {
                            if (!this.complianceViolation(HttpComplianceSection.NO_WS_AFTER_FIELD_NAME, null)) {
                                this._headerString = this.takeString();
                                this._header = (HttpHeader)HttpHeader.CACHE.get(this._headerString);
                                this._length = -1;
                                this.setState(FieldState.WS_AFTER_NAME);
                                continue;
                            }
                            throw new IllegalCharacterException(this._state, t, buffer);
                        }
                        case COLON: {
                            this._headerString = this.takeString();
                            this._header = (HttpHeader)HttpHeader.CACHE.get(this._headerString);
                            this._length = -1;
                            this.setState(FieldState.VALUE);
                            continue;
                        }
                        case LF: {
                            this._headerString = this.takeString();
                            this._header = (HttpHeader)HttpHeader.CACHE.get(this._headerString);
                            this._string.setLength(0);
                            this._valueString = "";
                            this._length = -1;
                            if (!this.complianceViolation(HttpComplianceSection.FIELD_COLON, this._headerString)) {
                                this.setState(FieldState.FIELD);
                                continue;
                            }
                            throw new IllegalCharacterException(this._state, t, buffer);
                        }
                        case ALPHA:
                        case DIGIT:
                        case TCHAR: {
                            this._string.append(t.getChar());
                            this._length = this._string.length();
                            continue;
                        }
                        default: {
                            throw new IllegalCharacterException(this._state, t, buffer);
                        }
                    }
                    break;
                }
                case WS_AFTER_NAME: {
                    switch (t.getType()) {
                        case HTAB:
                        case SPACE: {
                            continue;
                        }
                        case COLON: {
                            this.setState(FieldState.VALUE);
                            continue;
                        }
                        case LF: {
                            if (!this.complianceViolation(HttpComplianceSection.FIELD_COLON, this._headerString)) {
                                this.setState(FieldState.FIELD);
                                continue;
                            }
                            throw new IllegalCharacterException(this._state, t, buffer);
                        }
                        default: {
                            throw new IllegalCharacterException(this._state, t, buffer);
                        }
                    }
                    break;
                }
                case VALUE: {
                    switch (t.getType()) {
                        case LF: {
                            this._string.setLength(0);
                            this._valueString = "";
                            this._length = -1;
                            this.setState(FieldState.FIELD);
                            continue;
                        }
                        case HTAB:
                        case SPACE: {
                            continue;
                        }
                        case ALPHA:
                        case DIGIT:
                        case TCHAR:
                        case VCHAR:
                        case OTEXT:
                        case COLON: {
                            this._string.append(t.getChar());
                            this._length = this._string.length();
                            this.setState(FieldState.IN_VALUE);
                            continue;
                        }
                        default: {
                            throw new IllegalCharacterException(this._state, t, buffer);
                        }
                    }
                    break;
                }
                case IN_VALUE: {
                    switch (t.getType()) {
                        case LF: {
                            if (this._length > 0) {
                                this._valueString = this.takeString();
                                this._length = -1;
                            }
                            this.setState(FieldState.FIELD);
                            continue;
                        }
                        case HTAB:
                        case SPACE: {
                            this._string.append(t.getChar());
                            continue;
                        }
                        case ALPHA:
                        case DIGIT:
                        case TCHAR:
                        case VCHAR:
                        case OTEXT:
                        case COLON: {
                            this._string.append(t.getChar());
                            this._length = this._string.length();
                            continue;
                        }
                        default: {
                            throw new IllegalCharacterException(this._state, t, buffer);
                        }
                    }
                    break;
                }
                default: {
                    throw new IllegalStateException(this._state.toString());
                }
            }
        }
        return false;
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
            if (this._state == State.HEADER && this.parseFields(buffer)) {
                return true;
            }
            if (this._state.ordinal() >= State.CONTENT.ordinal() && this._state.ordinal() < State.TRAILER.ordinal()) {
                if (this._responseStatus > 0 && this._headResponse) {
                    this.setState(State.END);
                    return this.handleContentMessage();
                }
                if (this.parseContent(buffer)) {
                    return true;
                }
            }
            if (this._state == State.TRAILER && this.parseFields(buffer)) {
                return true;
            }
            if (this._state == State.END) {
                while (buffer.remaining() > 0 && buffer.get(buffer.position()) <= 32) {
                    buffer.get();
                }
            }
            else if (this.isClose() || this.isClosed()) {
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
                    case TRAILER: {
                        if (this._fieldState == FieldState.FIELD) {
                            this.setState(State.CLOSED);
                            return this.handleContentMessage();
                        }
                        this.setState(State.CLOSED);
                        this._handler.earlyEOF();
                        break;
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
                        this._handler.badMessage(new BadMessageException(400));
                        break;
                    }
                }
            }
        }
        catch (BadMessageException x) {
            BufferUtil.clear(buffer);
            this.badMessage(x);
        }
        catch (Throwable x2) {
            BufferUtil.clear(buffer);
            this.badMessage(new BadMessageException(400, (this._requestHandler != null) ? "Bad Request" : "Bad Response", x2));
        }
        return false;
    }
    
    protected void badMessage(final BadMessageException x) {
        if (this.DEBUG) {
            HttpParser.LOG.debug("Parse exception: " + this + " for " + this._handler, (Throwable)x);
        }
        this.setState(State.CLOSE);
        if (this._headerComplete) {
            this._handler.earlyEOF();
        }
        else {
            this._handler.badMessage(x);
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
        while (this._state.ordinal() < State.TRAILER.ordinal() && remaining > 0) {
            Label_0847: {
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
                        final HttpTokens.Token t = this.next(buffer);
                        if (t == null) {
                            break;
                        }
                        switch (t.getType()) {
                            case LF: {
                                break Label_0847;
                            }
                            case DIGIT: {
                                this._chunkLength = t.getHexDigit();
                                this._chunkPosition = 0;
                                this.setState(State.CHUNK_SIZE);
                                break Label_0847;
                            }
                            case ALPHA: {
                                if (t.isHexDigit()) {
                                    this._chunkLength = t.getHexDigit();
                                    this._chunkPosition = 0;
                                    this.setState(State.CHUNK_SIZE);
                                    break Label_0847;
                                }
                                throw new IllegalCharacterException(this._state, t, buffer);
                            }
                            default: {
                                throw new IllegalCharacterException(this._state, t, buffer);
                            }
                        }
                        break;
                    }
                    case CHUNK_SIZE: {
                        final HttpTokens.Token t = this.next(buffer);
                        if (t == null) {
                            break;
                        }
                        switch (t.getType()) {
                            case LF: {
                                if (this._chunkLength != 0) {
                                    this.setState(State.CHUNK);
                                    break Label_0847;
                                }
                                this.setState(State.TRAILER);
                                if (this._handler.contentComplete()) {
                                    return true;
                                }
                                break Label_0847;
                            }
                            case SPACE: {
                                this.setState(State.CHUNK_PARAMS);
                                break Label_0847;
                            }
                            default: {
                                if (!t.isHexDigit()) {
                                    this.setState(State.CHUNK_PARAMS);
                                    break Label_0847;
                                }
                                if (this._chunkLength > 134217711) {
                                    throw new BadMessageException(413);
                                }
                                this._chunkLength = this._chunkLength * 16 + t.getHexDigit();
                                break Label_0847;
                            }
                        }
                        break;
                    }
                    case CHUNK_PARAMS: {
                        final HttpTokens.Token t = this.next(buffer);
                        if (t == null) {
                            break;
                        }
                        switch (t.getType()) {
                            case LF: {
                                if (this._chunkLength != 0) {
                                    this.setState(State.CHUNK);
                                    break Label_0847;
                                }
                                this.setState(State.TRAILER);
                                if (this._handler.contentComplete()) {
                                    return true;
                                }
                                break Label_0847;
                            }
                            default: {
                                break Label_0847;
                            }
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
                    case CLOSED: {
                        BufferUtil.clear(buffer);
                        return false;
                    }
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
    
    protected void setState(final FieldState state) {
        if (this.DEBUG) {
            HttpParser.LOG.debug("{}:{} --> {}", new Object[] { this._state, (this._field != null) ? this._field : ((this._headerString != null) ? this._headerString : this._string), state });
        }
        this._fieldState = state;
    }
    
    public Trie<HttpField> getFieldCache() {
        return this._fieldCache;
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
        HttpParser.CACHE.put((Object)new HttpField(HttpHeader.ACCEPT_ENCODING, "gzip, deflate, br"));
        HttpParser.CACHE.put((Object)new HttpField(HttpHeader.ACCEPT_ENCODING, "gzip,deflate,sdch"));
        HttpParser.CACHE.put((Object)new HttpField(HttpHeader.ACCEPT_LANGUAGE, "en-US,en;q=0.5"));
        HttpParser.CACHE.put((Object)new HttpField(HttpHeader.ACCEPT_LANGUAGE, "en-GB,en-US;q=0.8,en;q=0.6"));
        HttpParser.CACHE.put((Object)new HttpField(HttpHeader.ACCEPT_LANGUAGE, "en-AU,en;q=0.9,it-IT;q=0.8,it;q=0.7,en-GB;q=0.6,en-US;q=0.5"));
        HttpParser.CACHE.put((Object)new HttpField(HttpHeader.ACCEPT_CHARSET, "ISO-8859-1,utf-8;q=0.7,*;q=0.3"));
        HttpParser.CACHE.put((Object)new HttpField(HttpHeader.ACCEPT, "*/*"));
        HttpParser.CACHE.put((Object)new HttpField(HttpHeader.ACCEPT, "image/png,image/*;q=0.8,*/*;q=0.5"));
        HttpParser.CACHE.put((Object)new HttpField(HttpHeader.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"));
        HttpParser.CACHE.put((Object)new HttpField(HttpHeader.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8"));
        HttpParser.CACHE.put((Object)new HttpField(HttpHeader.ACCEPT_RANGES, HttpHeaderValue.BYTES));
        HttpParser.CACHE.put((Object)new HttpField(HttpHeader.PRAGMA, "no-cache"));
        HttpParser.CACHE.put((Object)new HttpField(HttpHeader.CACHE_CONTROL, "private, no-cache, no-cache=Set-Cookie, proxy-revalidate"));
        HttpParser.CACHE.put((Object)new HttpField(HttpHeader.CACHE_CONTROL, "no-cache"));
        HttpParser.CACHE.put((Object)new HttpField(HttpHeader.CACHE_CONTROL, "max-age=0"));
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
    }
    
    public enum FieldState
    {
        FIELD, 
        IN_NAME, 
        VALUE, 
        IN_VALUE, 
        WS_AFTER_NAME;
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
        CONTENT, 
        EOF_CONTENT, 
        CHUNKED_CONTENT, 
        CHUNK_SIZE, 
        CHUNK_PARAMS, 
        CHUNK, 
        TRAILER, 
        END, 
        CLOSE, 
        CLOSED;
    }
    
    public interface HttpHandler
    {
        boolean content(final ByteBuffer p0);
        
        boolean headerComplete();
        
        boolean contentComplete();
        
        boolean messageComplete();
        
        void parsedHeader(final HttpField p0);
        
        default void parsedTrailer(final HttpField field) {
        }
        
        void earlyEOF();
        
        default void badMessage(final BadMessageException failure) {
            this.badMessage(failure.getCode(), failure.getReason());
        }
        
        @Deprecated
        default void badMessage(final int status, final String reason) {
        }
        
        int getHeaderCacheSize();
    }
    
    public interface ComplianceHandler extends HttpHandler
    {
        @Deprecated
        default void onComplianceViolation(final HttpCompliance compliance, final HttpCompliance required, final String reason) {
        }
        
        default void onComplianceViolation(final HttpCompliance compliance, final HttpComplianceSection violation, final String details) {
            this.onComplianceViolation(compliance, HttpCompliance.requiredCompliance(violation), details);
        }
    }
    
    private static class IllegalCharacterException extends BadMessageException
    {
        private IllegalCharacterException(final State state, final HttpTokens.Token token, final ByteBuffer buffer) {
            super(400, String.format("Illegal character %s", token));
            if (HttpParser.LOG.isDebugEnabled()) {
                HttpParser.LOG.debug(String.format("Illegal character %s in state=%s for buffer %s", token, state, BufferUtil.toDetailString(buffer)), new Object[0]);
            }
        }
    }
    
    public interface ResponseHandler extends HttpHandler
    {
        boolean startResponse(final HttpVersion p0, final int p1, final String p2);
    }
    
    public interface RequestHandler extends HttpHandler
    {
        boolean startRequest(final String p0, final String p1, final HttpVersion p2);
    }
}
