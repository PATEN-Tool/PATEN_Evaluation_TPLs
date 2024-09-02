// 
// Decompiled by Procyon v0.5.36
// 

package io.undertow.server.protocol.ajp;

import io.undertow.util.Headers;
import io.undertow.util.Methods;
import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.util.TreeMap;
import io.undertow.security.impl.ExternalAuthenticationMechanism;
import io.undertow.server.Connectors;
import io.undertow.util.ParameterLimitException;
import io.undertow.UndertowLogger;
import io.undertow.util.URLUtils;
import io.undertow.util.BadRequestException;
import io.undertow.UndertowMessages;
import io.undertow.server.HttpServerExchange;
import java.nio.ByteBuffer;
import io.undertow.util.HttpString;

public class AjpRequestParser
{
    private final String encoding;
    private final boolean doDecode;
    private final boolean allowEncodedSlash;
    private final int maxParameters;
    private final int maxHeaders;
    private StringBuilder decodeBuffer;
    private final boolean allowUnescapedCharactersInUrl;
    private static final HttpString[] HTTP_HEADERS;
    public static final int FORWARD_REQUEST = 2;
    public static final int CPONG = 9;
    public static final int CPING = 10;
    public static final int SHUTDOWN = 7;
    private static final HttpString[] HTTP_METHODS;
    private static final String[] ATTRIBUTES;
    public static final String QUERY_STRING = "query_string";
    public static final String SSL_CERT = "ssl_cert";
    public static final String CONTEXT = "context";
    public static final String SERVLET_PATH = "servlet_path";
    public static final String REMOTE_USER = "remote_user";
    public static final String AUTH_TYPE = "auth_type";
    public static final String ROUTE = "route";
    public static final String SSL_CIPHER = "ssl_cipher";
    public static final String SSL_SESSION = "ssl_session";
    public static final String REQ_ATTRIBUTE = "req_attribute";
    public static final String SSL_KEY_SIZE = "ssl_key_size";
    public static final String SECRET = "secret";
    public static final String STORED_METHOD = "stored_method";
    public static final String AJP_REMOTE_PORT = "AJP_REMOTE_PORT";
    public static final int STRING_LENGTH_MASK = Integer.MIN_VALUE;
    
    public AjpRequestParser(final String encoding, final boolean doDecode, final int maxParameters, final int maxHeaders, final boolean allowEncodedSlash, final boolean allowUnescapedCharactersInUrl) {
        this.encoding = encoding;
        this.doDecode = doDecode;
        this.maxParameters = maxParameters;
        this.maxHeaders = maxHeaders;
        this.allowEncodedSlash = allowEncodedSlash;
        this.allowUnescapedCharactersInUrl = allowUnescapedCharactersInUrl;
    }
    
    public void parse(final ByteBuffer buf, final AjpRequestParseState state, final HttpServerExchange exchange) throws IOException, BadRequestException {
        if (!buf.hasRemaining()) {
            return;
        }
        Label_0754: {
            switch (state.state) {
                case 0: {
                    final IntegerHolder result = this.parse16BitInteger(buf, state);
                    if (!result.readComplete) {
                        return;
                    }
                    if (result.value != 4660) {
                        throw new BadRequestException(UndertowMessages.MESSAGES.wrongMagicNumber(result.value));
                    }
                }
                case 2: {
                    final IntegerHolder result = this.parse16BitInteger(buf, state);
                    if (!result.readComplete) {
                        state.state = 2;
                        return;
                    }
                    state.dataSize = result.value;
                }
                case 3: {
                    if (!buf.hasRemaining()) {
                        state.state = 3;
                        return;
                    }
                    final byte prefix = buf.get();
                    if ((state.prefix = prefix) != 2) {
                        state.state = 15;
                        return;
                    }
                }
                case 4: {
                    if (!buf.hasRemaining()) {
                        state.state = 4;
                        return;
                    }
                    final int method = buf.get();
                    if (method > 0 && method < 28) {
                        exchange.setRequestMethod(AjpRequestParser.HTTP_METHODS[method]);
                        break Label_0754;
                    }
                    if ((method & 0xFF) != 0xFF) {
                        throw new BadRequestException("Unknown method type " + method);
                    }
                    break Label_0754;
                }
                case 5: {
                    final StringHolder result2 = this.parseString(buf, state, StringType.OTHER);
                    if (result2.readComplete) {
                        exchange.setProtocol(HttpString.tryFromString(result2.value));
                        break Label_0754;
                    }
                    state.state = 5;
                }
                case 6: {
                    final StringHolder result2 = this.parseString(buf, state, StringType.URL);
                    if (result2.readComplete) {
                        final int colon = result2.value.indexOf(59);
                        if (colon == -1) {
                            final String res = this.decode(result2.value, result2.containsUrlCharacters);
                            if (result2.containsUnencodedCharacters) {
                                exchange.setRequestURI(res);
                            }
                            else {
                                exchange.setRequestURI(result2.value);
                            }
                            exchange.setRequestPath(res);
                            exchange.setRelativePath(res);
                        }
                        else {
                            final String url = result2.value.substring(0, colon);
                            final String res2 = this.decode(url, result2.containsUrlCharacters);
                            if (result2.containsUnencodedCharacters) {
                                exchange.setRequestURI(res2);
                            }
                            else {
                                exchange.setRequestURI(result2.value);
                            }
                            exchange.setRequestPath(res2);
                            exchange.setRelativePath(res2);
                            try {
                                URLUtils.parsePathParams(result2.value.substring(colon + 1), exchange, this.encoding, this.doDecode && result2.containsUrlCharacters, this.maxParameters);
                            }
                            catch (ParameterLimitException e) {
                                UndertowLogger.REQUEST_IO_LOGGER.failedToParseRequest(e);
                                state.badRequest = true;
                            }
                        }
                        break Label_0754;
                    }
                    state.state = 6;
                }
                case 7: {
                    final StringHolder result2 = this.parseString(buf, state, StringType.OTHER);
                    if (result2.readComplete) {
                        state.remoteAddress = result2.value;
                        break Label_0754;
                    }
                    state.state = 7;
                }
                case 8: {
                    final StringHolder result2 = this.parseString(buf, state, StringType.OTHER);
                    if (result2.readComplete) {
                        break Label_0754;
                    }
                    state.state = 8;
                }
                case 9: {
                    final StringHolder result2 = this.parseString(buf, state, StringType.OTHER);
                    if (result2.readComplete) {
                        state.serverAddress = result2.value;
                        break Label_0754;
                    }
                    state.state = 9;
                }
                case 10: {
                    final IntegerHolder result = this.parse16BitInteger(buf, state);
                    if (result.readComplete) {
                        state.serverPort = result.value;
                        break Label_0754;
                    }
                    state.state = 10;
                }
                case 11: {
                    if (!buf.hasRemaining()) {
                        state.state = 11;
                        return;
                    }
                    final byte isSsl = buf.get();
                    if (isSsl != 0) {
                        exchange.setRequestScheme("https");
                        break Label_0754;
                    }
                    exchange.setRequestScheme("http");
                    break Label_0754;
                }
                case 12: {
                    final IntegerHolder result = this.parse16BitInteger(buf, state);
                    if (!result.readComplete) {
                        state.state = 12;
                        return;
                    }
                    state.numHeaders = result.value;
                    if (state.numHeaders > this.maxHeaders) {
                        UndertowLogger.REQUEST_IO_LOGGER.failedToParseRequest(new BadRequestException(UndertowMessages.MESSAGES.tooManyHeaders(this.maxHeaders)));
                        state.badRequest = true;
                    }
                }
                case 13: {
                    for (int readHeaders = state.readHeaders; readHeaders < state.numHeaders; ++readHeaders) {
                        if (state.currentHeader == null) {
                            final StringHolder result3 = this.parseString(buf, state, StringType.HEADER);
                            if (!result3.readComplete) {
                                state.state = 13;
                                state.readHeaders = readHeaders;
                                return;
                            }
                            if (result3.header != null) {
                                state.currentHeader = result3.header;
                            }
                            else {
                                Connectors.verifyToken(state.currentHeader = HttpString.tryFromString(result3.value));
                            }
                        }
                        final StringHolder result3 = this.parseString(buf, state, StringType.OTHER);
                        if (!result3.readComplete) {
                            state.state = 13;
                            state.readHeaders = readHeaders;
                            return;
                        }
                        if (!state.badRequest) {
                            exchange.getRequestHeaders().add(state.currentHeader, result3.value);
                        }
                        state.currentHeader = null;
                    }
                }
                case 14: {
                    while (true) {
                        if (state.currentAttribute == null && state.currentIntegerPart == -1) {
                            if (!buf.hasRemaining()) {
                                state.state = 14;
                                return;
                            }
                            final int val = 0xFF & buf.get();
                            if (val == 255) {
                                state.state = 15;
                                return;
                            }
                            if (val == 10) {
                                state.currentIntegerPart = 1;
                            }
                            else {
                                if (val == 0) {
                                    continue;
                                }
                                if (val >= AjpRequestParser.ATTRIBUTES.length) {
                                    continue;
                                }
                                state.currentAttribute = AjpRequestParser.ATTRIBUTES[val];
                            }
                        }
                        if (state.currentIntegerPart == 1) {
                            final StringHolder result2 = this.parseString(buf, state, StringType.OTHER);
                            if (!result2.readComplete) {
                                state.state = 14;
                                return;
                            }
                            state.currentAttribute = result2.value;
                            state.currentIntegerPart = -1;
                        }
                        boolean decodingAlreadyDone = false;
                        String result4;
                        if (state.currentAttribute.equals("ssl_key_size")) {
                            final IntegerHolder resultHolder = this.parse16BitInteger(buf, state);
                            if (!resultHolder.readComplete) {
                                state.state = 14;
                                return;
                            }
                            result4 = Integer.toString(resultHolder.value);
                        }
                        else {
                            final StringHolder resultHolder2 = this.parseString(buf, state, state.currentAttribute.equals("query_string") ? StringType.QUERY_STRING : StringType.OTHER);
                            if (!resultHolder2.readComplete) {
                                state.state = 14;
                                return;
                            }
                            if (resultHolder2.containsUnencodedCharacters) {
                                result4 = this.decode(resultHolder2.value, true);
                                decodingAlreadyDone = true;
                            }
                            else {
                                result4 = resultHolder2.value;
                            }
                        }
                        if (state.currentAttribute.equals("query_string")) {
                            final String resultAsQueryString = (result4 == null) ? "" : result4;
                            exchange.setQueryString(resultAsQueryString);
                            try {
                                URLUtils.parseQueryString(resultAsQueryString, exchange, this.encoding, this.doDecode && !decodingAlreadyDone, this.maxParameters);
                            }
                            catch (ParameterLimitException | IllegalArgumentException ex2) {
                                final Exception ex;
                                final Exception e2 = ex;
                                UndertowLogger.REQUEST_IO_LOGGER.failedToParseRequest(e2);
                                state.badRequest = true;
                            }
                        }
                        else if (state.currentAttribute.equals("remote_user")) {
                            exchange.putAttachment(ExternalAuthenticationMechanism.EXTERNAL_PRINCIPAL, result4);
                        }
                        else if (state.currentAttribute.equals("auth_type")) {
                            exchange.putAttachment(ExternalAuthenticationMechanism.EXTERNAL_AUTHENTICATION_TYPE, result4);
                        }
                        else if (state.currentAttribute.equals("stored_method")) {
                            final HttpString requestMethod = new HttpString(result4);
                            Connectors.verifyToken(requestMethod);
                            exchange.setRequestMethod(requestMethod);
                        }
                        else if (state.currentAttribute.equals("AJP_REMOTE_PORT")) {
                            state.remotePort = Integer.parseInt(result4);
                        }
                        else if (state.currentAttribute.equals("ssl_session")) {
                            state.sslSessionId = result4;
                        }
                        else if (state.currentAttribute.equals("ssl_cipher")) {
                            state.sslCipher = result4;
                        }
                        else if (state.currentAttribute.equals("ssl_cert")) {
                            state.sslCert = result4;
                        }
                        else if (state.currentAttribute.equals("ssl_key_size")) {
                            state.sslKeySize = result4;
                        }
                        else {
                            if (state.attributes == null) {
                                state.attributes = new TreeMap<String, String>();
                            }
                            state.attributes.put(state.currentAttribute, result4);
                        }
                        state.currentAttribute = null;
                    }
                    break;
                }
                default: {
                    state.state = 15;
                }
            }
        }
    }
    
    private String decode(final String url, final boolean containsUrlCharacters) throws UnsupportedEncodingException {
        if (this.doDecode && containsUrlCharacters) {
            try {
                if (this.decodeBuffer == null) {
                    this.decodeBuffer = new StringBuilder();
                }
                return URLUtils.decode(url, this.encoding, this.allowEncodedSlash, false, this.decodeBuffer);
            }
            catch (Exception e) {
                throw UndertowMessages.MESSAGES.failedToDecodeURL(url, this.encoding, e);
            }
        }
        return url;
    }
    
    protected HttpString headers(final int offset) {
        return AjpRequestParser.HTTP_HEADERS[offset];
    }
    
    protected IntegerHolder parse16BitInteger(final ByteBuffer buf, final AjpRequestParseState state) {
        if (!buf.hasRemaining()) {
            return new IntegerHolder(-1, false);
        }
        int number = state.currentIntegerPart;
        if (number == -1) {
            number = (buf.get() & 0xFF);
        }
        if (buf.hasRemaining()) {
            final byte b = buf.get();
            final int result = ((0xFF & number) << 8) + (b & 0xFF);
            state.currentIntegerPart = -1;
            return new IntegerHolder(result, true);
        }
        state.currentIntegerPart = number;
        return new IntegerHolder(-1, false);
    }
    
    protected StringHolder parseString(final ByteBuffer buf, final AjpRequestParseState state, final StringType type) throws UnsupportedEncodingException, BadRequestException {
        boolean containsUrlCharacters = state.containsUrlCharacters;
        boolean containsUnencodedUrlCharacters = state.containsUnencodedUrlCharacters;
        if (!buf.hasRemaining()) {
            return new StringHolder((String)null, false, false, false);
        }
        int stringLength = state.stringLength;
        if (stringLength == -1) {
            final int number = buf.get() & 0xFF;
            if (!buf.hasRemaining()) {
                state.stringLength = (number | Integer.MIN_VALUE);
                return new StringHolder((String)null, false, false, false);
            }
            final byte b = buf.get();
            stringLength = ((0xFF & number) << 8) + (b & 0xFF);
        }
        else if ((stringLength & Integer.MIN_VALUE) != 0x0) {
            final int number = stringLength & Integer.MAX_VALUE;
            stringLength = ((0xFF & number) << 8) + (buf.get() & 0xFF);
        }
        if (type == StringType.HEADER && (stringLength & 0xFF00) != 0x0) {
            state.stringLength = -1;
            return new StringHolder(this.headers(stringLength & 0xFF));
        }
        if (stringLength == 65535) {
            state.stringLength = -1;
            return new StringHolder((String)null, true, false, false);
        }
        for (int length = state.getCurrentStringLength(); length < stringLength; ++length) {
            if (!buf.hasRemaining()) {
                state.stringLength = stringLength;
                state.containsUrlCharacters = containsUrlCharacters;
                state.containsUnencodedUrlCharacters = containsUnencodedUrlCharacters;
                return new StringHolder((String)null, false, false, false);
            }
            final byte c = buf.get();
            if (type == StringType.QUERY_STRING && (c == 43 || c == 37 || c < 0)) {
                if (c < 0) {
                    if (!this.allowUnescapedCharactersInUrl) {
                        throw new BadRequestException();
                    }
                    containsUnencodedUrlCharacters = true;
                }
                containsUrlCharacters = true;
            }
            else if (type == StringType.URL && (c == 37 || c < 0)) {
                if (c < 0) {
                    if (!this.allowUnescapedCharactersInUrl) {
                        throw new BadRequestException();
                    }
                    containsUnencodedUrlCharacters = true;
                }
                containsUrlCharacters = true;
            }
            state.addStringByte(c);
        }
        if (buf.hasRemaining()) {
            buf.get();
            final String value = state.getStringAndClear();
            state.stringLength = -1;
            state.containsUrlCharacters = false;
            state.containsUnencodedUrlCharacters = containsUnencodedUrlCharacters;
            return new StringHolder(value, true, containsUrlCharacters, containsUnencodedUrlCharacters);
        }
        state.stringLength = stringLength;
        state.containsUrlCharacters = containsUrlCharacters;
        state.containsUnencodedUrlCharacters = containsUnencodedUrlCharacters;
        return new StringHolder((String)null, false, false, false);
    }
    
    static {
        (HTTP_METHODS = new HttpString[28])[1] = Methods.OPTIONS;
        AjpRequestParser.HTTP_METHODS[2] = Methods.GET;
        AjpRequestParser.HTTP_METHODS[3] = Methods.HEAD;
        AjpRequestParser.HTTP_METHODS[4] = Methods.POST;
        AjpRequestParser.HTTP_METHODS[5] = Methods.PUT;
        AjpRequestParser.HTTP_METHODS[6] = Methods.DELETE;
        AjpRequestParser.HTTP_METHODS[7] = Methods.TRACE;
        AjpRequestParser.HTTP_METHODS[8] = Methods.PROPFIND;
        AjpRequestParser.HTTP_METHODS[9] = Methods.PROPPATCH;
        AjpRequestParser.HTTP_METHODS[10] = Methods.MKCOL;
        AjpRequestParser.HTTP_METHODS[11] = Methods.COPY;
        AjpRequestParser.HTTP_METHODS[12] = Methods.MOVE;
        AjpRequestParser.HTTP_METHODS[13] = Methods.LOCK;
        AjpRequestParser.HTTP_METHODS[14] = Methods.UNLOCK;
        AjpRequestParser.HTTP_METHODS[15] = Methods.ACL;
        AjpRequestParser.HTTP_METHODS[16] = Methods.REPORT;
        AjpRequestParser.HTTP_METHODS[17] = Methods.VERSION_CONTROL;
        AjpRequestParser.HTTP_METHODS[18] = Methods.CHECKIN;
        AjpRequestParser.HTTP_METHODS[19] = Methods.CHECKOUT;
        AjpRequestParser.HTTP_METHODS[20] = Methods.UNCHECKOUT;
        AjpRequestParser.HTTP_METHODS[21] = Methods.SEARCH;
        AjpRequestParser.HTTP_METHODS[22] = Methods.MKWORKSPACE;
        AjpRequestParser.HTTP_METHODS[23] = Methods.UPDATE;
        AjpRequestParser.HTTP_METHODS[24] = Methods.LABEL;
        AjpRequestParser.HTTP_METHODS[25] = Methods.MERGE;
        AjpRequestParser.HTTP_METHODS[26] = Methods.BASELINE_CONTROL;
        AjpRequestParser.HTTP_METHODS[27] = Methods.MKACTIVITY;
        (HTTP_HEADERS = new HttpString[15])[1] = Headers.ACCEPT;
        AjpRequestParser.HTTP_HEADERS[2] = Headers.ACCEPT_CHARSET;
        AjpRequestParser.HTTP_HEADERS[3] = Headers.ACCEPT_ENCODING;
        AjpRequestParser.HTTP_HEADERS[4] = Headers.ACCEPT_LANGUAGE;
        AjpRequestParser.HTTP_HEADERS[5] = Headers.AUTHORIZATION;
        AjpRequestParser.HTTP_HEADERS[6] = Headers.CONNECTION;
        AjpRequestParser.HTTP_HEADERS[7] = Headers.CONTENT_TYPE;
        AjpRequestParser.HTTP_HEADERS[8] = Headers.CONTENT_LENGTH;
        AjpRequestParser.HTTP_HEADERS[9] = Headers.COOKIE;
        AjpRequestParser.HTTP_HEADERS[10] = Headers.COOKIE2;
        AjpRequestParser.HTTP_HEADERS[11] = Headers.HOST;
        AjpRequestParser.HTTP_HEADERS[12] = Headers.PRAGMA;
        AjpRequestParser.HTTP_HEADERS[13] = Headers.REFERER;
        AjpRequestParser.HTTP_HEADERS[14] = Headers.USER_AGENT;
        (ATTRIBUTES = new String[14])[1] = "context";
        AjpRequestParser.ATTRIBUTES[2] = "servlet_path";
        AjpRequestParser.ATTRIBUTES[3] = "remote_user";
        AjpRequestParser.ATTRIBUTES[4] = "auth_type";
        AjpRequestParser.ATTRIBUTES[5] = "query_string";
        AjpRequestParser.ATTRIBUTES[6] = "route";
        AjpRequestParser.ATTRIBUTES[7] = "ssl_cert";
        AjpRequestParser.ATTRIBUTES[8] = "ssl_cipher";
        AjpRequestParser.ATTRIBUTES[9] = "ssl_session";
        AjpRequestParser.ATTRIBUTES[10] = "req_attribute";
        AjpRequestParser.ATTRIBUTES[11] = "ssl_key_size";
        AjpRequestParser.ATTRIBUTES[12] = "secret";
        AjpRequestParser.ATTRIBUTES[13] = "stored_method";
    }
    
    protected static class IntegerHolder
    {
        public final int value;
        public final boolean readComplete;
        
        private IntegerHolder(final int value, final boolean readComplete) {
            this.value = value;
            this.readComplete = readComplete;
        }
    }
    
    protected static class StringHolder
    {
        public final String value;
        public final HttpString header;
        final boolean readComplete;
        final boolean containsUrlCharacters;
        final boolean containsUnencodedCharacters;
        
        private StringHolder(final String value, final boolean readComplete, final boolean containsUrlCharacters, final boolean containsUnencodedCharacters) {
            this.value = value;
            this.readComplete = readComplete;
            this.containsUrlCharacters = containsUrlCharacters;
            this.containsUnencodedCharacters = containsUnencodedCharacters;
            this.header = null;
        }
        
        private StringHolder(final HttpString value) {
            this.value = null;
            this.readComplete = true;
            this.header = value;
            this.containsUrlCharacters = false;
            this.containsUnencodedCharacters = false;
        }
    }
    
    enum StringType
    {
        HEADER, 
        URL, 
        QUERY_STRING, 
        OTHER;
    }
}
