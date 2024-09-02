// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.coyote.http11;

import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.net.SendfileKeepAliveState;
import java.nio.ByteBuffer;
import org.apache.coyote.http11.filters.SavedRequestInputFilter;
import org.apache.tomcat.util.buf.HexUtils;
import org.apache.tomcat.util.http.FastHttpDateFormat;
import org.apache.tomcat.util.http.MimeHeaders;
import org.apache.coyote.Request;
import org.apache.coyote.http11.upgrade.InternalHttpUpgradeHandler;
import java.util.Enumeration;
import org.apache.coyote.RequestInfo;
import java.io.InterruptedIOException;
import org.apache.tomcat.InstanceManager;
import org.apache.tomcat.ContextBind;
import javax.servlet.http.HttpUpgradeHandler;
import org.apache.coyote.ActionCode;
import org.apache.tomcat.util.ExceptionUtils;
import java.io.IOException;
import org.apache.tomcat.util.net.SendfileState;
import org.apache.tomcat.util.net.SocketWrapperBase;
import org.apache.coyote.ErrorState;
import java.util.Locale;
import org.apache.tomcat.util.buf.Ascii;
import org.apache.tomcat.util.buf.ByteChunk;
import org.apache.tomcat.util.buf.MessageBytes;
import org.apache.coyote.http11.filters.GzipOutputFilter;
import org.apache.coyote.http11.filters.BufferedInputFilter;
import org.apache.coyote.http11.filters.VoidOutputFilter;
import org.apache.coyote.http11.filters.VoidInputFilter;
import org.apache.coyote.http11.filters.ChunkedOutputFilter;
import org.apache.coyote.http11.filters.ChunkedInputFilter;
import org.apache.coyote.http11.filters.IdentityOutputFilter;
import org.apache.coyote.http11.filters.IdentityInputFilter;
import org.apache.coyote.OutputBuffer;
import org.apache.coyote.InputBuffer;
import java.util.Set;
import org.apache.tomcat.util.net.AbstractEndpoint;
import org.apache.coyote.UpgradeProtocol;
import java.util.Map;
import org.apache.tomcat.util.net.SendfileDataBase;
import org.apache.coyote.UpgradeToken;
import java.util.regex.Pattern;
import org.apache.tomcat.util.log.UserDataHelper;
import org.apache.tomcat.util.res.StringManager;
import org.apache.juli.logging.Log;
import org.apache.coyote.AbstractProcessor;

public class Http11Processor extends AbstractProcessor
{
    private static final Log log;
    private static final StringManager sm;
    private final UserDataHelper userDataHelper;
    protected final Http11InputBuffer inputBuffer;
    protected final Http11OutputBuffer outputBuffer;
    private int pluggableFilterIndex;
    protected volatile boolean keepAlive;
    protected boolean openSocket;
    protected boolean readComplete;
    protected boolean http11;
    protected boolean http09;
    protected boolean contentDelimitation;
    protected Pattern restrictedUserAgents;
    protected int maxKeepAliveRequests;
    protected int connectionUploadTimeout;
    protected boolean disableUploadTimeout;
    protected int compressionLevel;
    protected int compressionMinSize;
    protected int maxSavePostSize;
    protected Pattern noCompressionUserAgents;
    protected String[] compressableMimeTypes;
    protected char[] hostNameC;
    private String server;
    private boolean serverRemoveAppProvidedValues;
    protected UpgradeToken upgradeToken;
    protected SendfileDataBase sendfileData;
    private final Map<String, UpgradeProtocol> httpUpgradeProtocols;
    private final boolean allowHostHeaderMismatch;
    
    public Http11Processor(final int maxHttpHeaderSize, final boolean allowHostHeaderMismatch, final boolean rejectIllegalHeaderName, final AbstractEndpoint<?> endpoint, final int maxTrailerSize, final Set<String> allowedTrailerHeaders, final int maxExtensionSize, final int maxSwallowSize, final Map<String, UpgradeProtocol> httpUpgradeProtocols, final boolean sendReasonPhrase) {
        super(endpoint);
        this.pluggableFilterIndex = Integer.MAX_VALUE;
        this.keepAlive = true;
        this.openSocket = false;
        this.readComplete = true;
        this.http11 = true;
        this.http09 = false;
        this.contentDelimitation = true;
        this.restrictedUserAgents = null;
        this.maxKeepAliveRequests = -1;
        this.connectionUploadTimeout = 300000;
        this.disableUploadTimeout = false;
        this.compressionLevel = 0;
        this.compressionMinSize = 2048;
        this.maxSavePostSize = 4096;
        this.noCompressionUserAgents = null;
        this.hostNameC = new char[0];
        this.server = null;
        this.serverRemoveAppProvidedValues = false;
        this.upgradeToken = null;
        this.sendfileData = null;
        this.userDataHelper = new UserDataHelper(Http11Processor.log);
        this.inputBuffer = new Http11InputBuffer(this.request, maxHttpHeaderSize, rejectIllegalHeaderName);
        this.request.setInputBuffer(this.inputBuffer);
        this.outputBuffer = new Http11OutputBuffer(this.response, maxHttpHeaderSize, sendReasonPhrase);
        this.response.setOutputBuffer(this.outputBuffer);
        this.inputBuffer.addFilter(new IdentityInputFilter(maxSwallowSize));
        this.outputBuffer.addFilter(new IdentityOutputFilter());
        this.inputBuffer.addFilter(new ChunkedInputFilter(maxTrailerSize, allowedTrailerHeaders, maxExtensionSize, maxSwallowSize));
        this.outputBuffer.addFilter(new ChunkedOutputFilter());
        this.inputBuffer.addFilter(new VoidInputFilter());
        this.outputBuffer.addFilter(new VoidOutputFilter());
        this.inputBuffer.addFilter(new BufferedInputFilter());
        this.outputBuffer.addFilter(new GzipOutputFilter());
        this.pluggableFilterIndex = this.inputBuffer.getFilters().length;
        this.httpUpgradeProtocols = httpUpgradeProtocols;
        this.allowHostHeaderMismatch = allowHostHeaderMismatch;
    }
    
    public void setCompression(final String compression) {
        if (compression.equals("on")) {
            this.compressionLevel = 1;
        }
        else if (compression.equals("force")) {
            this.compressionLevel = 2;
        }
        else if (compression.equals("off")) {
            this.compressionLevel = 0;
        }
        else {
            try {
                this.compressionMinSize = Integer.parseInt(compression);
                this.compressionLevel = 1;
            }
            catch (Exception e) {
                this.compressionLevel = 0;
            }
        }
    }
    
    public void setCompressionMinSize(final int compressionMinSize) {
        this.compressionMinSize = compressionMinSize;
    }
    
    public void setNoCompressionUserAgents(final String noCompressionUserAgents) {
        if (noCompressionUserAgents == null || noCompressionUserAgents.length() == 0) {
            this.noCompressionUserAgents = null;
        }
        else {
            this.noCompressionUserAgents = Pattern.compile(noCompressionUserAgents);
        }
    }
    
    @Deprecated
    public void setCompressableMimeTypes(final String[] compressibleMimeTypes) {
        this.setCompressibleMimeTypes(compressibleMimeTypes);
    }
    
    public void setCompressibleMimeTypes(final String[] compressibleMimeTypes) {
        this.compressableMimeTypes = compressibleMimeTypes;
    }
    
    public String getCompression() {
        switch (this.compressionLevel) {
            case 0: {
                return "off";
            }
            case 1: {
                return "on";
            }
            case 2: {
                return "force";
            }
            default: {
                return "off";
            }
        }
    }
    
    private static boolean startsWithStringArray(final String[] sArray, final String value) {
        if (value == null) {
            return false;
        }
        for (int i = 0; i < sArray.length; ++i) {
            if (value.startsWith(sArray[i])) {
                return true;
            }
        }
        return false;
    }
    
    public void setRestrictedUserAgents(final String restrictedUserAgents) {
        if (restrictedUserAgents == null || restrictedUserAgents.length() == 0) {
            this.restrictedUserAgents = null;
        }
        else {
            this.restrictedUserAgents = Pattern.compile(restrictedUserAgents);
        }
    }
    
    public void setMaxKeepAliveRequests(final int mkar) {
        this.maxKeepAliveRequests = mkar;
    }
    
    public int getMaxKeepAliveRequests() {
        return this.maxKeepAliveRequests;
    }
    
    public void setMaxSavePostSize(final int msps) {
        this.maxSavePostSize = msps;
    }
    
    public int getMaxSavePostSize() {
        return this.maxSavePostSize;
    }
    
    public void setDisableUploadTimeout(final boolean isDisabled) {
        this.disableUploadTimeout = isDisabled;
    }
    
    public boolean getDisableUploadTimeout() {
        return this.disableUploadTimeout;
    }
    
    public void setConnectionUploadTimeout(final int timeout) {
        this.connectionUploadTimeout = timeout;
    }
    
    public int getConnectionUploadTimeout() {
        return this.connectionUploadTimeout;
    }
    
    public void setServer(final String server) {
        if (server == null || server.equals("")) {
            this.server = null;
        }
        else {
            this.server = server;
        }
    }
    
    public void setServerRemoveAppProvidedValues(final boolean serverRemoveAppProvidedValues) {
        this.serverRemoveAppProvidedValues = serverRemoveAppProvidedValues;
    }
    
    private boolean isCompressible() {
        final MessageBytes contentEncodingMB = this.response.getMimeHeaders().getValue("Content-Encoding");
        if (contentEncodingMB != null && contentEncodingMB.indexOf("gzip") != -1) {
            return false;
        }
        if (this.compressionLevel == 2) {
            return true;
        }
        final long contentLength = this.response.getContentLengthLong();
        return (contentLength == -1L || contentLength > this.compressionMinSize) && this.compressableMimeTypes != null && startsWithStringArray(this.compressableMimeTypes, this.response.getContentType());
    }
    
    private boolean useCompression() {
        final MessageBytes acceptEncodingMB = this.request.getMimeHeaders().getValue("accept-encoding");
        if (acceptEncodingMB == null || acceptEncodingMB.indexOf("gzip") == -1) {
            return false;
        }
        if (this.compressionLevel == 2) {
            return true;
        }
        if (this.noCompressionUserAgents != null) {
            final MessageBytes userAgentValueMB = this.request.getMimeHeaders().getValue("user-agent");
            if (userAgentValueMB != null) {
                final String userAgentValue = userAgentValueMB.toString();
                if (this.noCompressionUserAgents.matcher(userAgentValue).matches()) {
                    return false;
                }
            }
        }
        return true;
    }
    
    private static int findBytes(final ByteChunk bc, final byte[] b) {
        final byte first = b[0];
        final byte[] buff = bc.getBuffer();
        final int start = bc.getStart();
        for (int end = bc.getEnd(), srcEnd = b.length, i = start; i <= end - srcEnd; ++i) {
            if (Ascii.toLower(buff[i]) == first) {
                int myPos = i + 1;
                int srcPos = 1;
                while (srcPos < srcEnd) {
                    if (Ascii.toLower(buff[myPos++]) != b[srcPos++]) {
                        break;
                    }
                    if (srcPos == srcEnd) {
                        return i - start;
                    }
                }
            }
        }
        return -1;
    }
    
    private static boolean statusDropsConnection(final int status) {
        return status == 400 || status == 408 || status == 411 || status == 413 || status == 414 || status == 500 || status == 503 || status == 501;
    }
    
    private void addInputFilter(final InputFilter[] inputFilters, String encodingName) {
        encodingName = encodingName.trim().toLowerCase(Locale.ENGLISH);
        if (!encodingName.equals("identity")) {
            if (encodingName.equals("chunked")) {
                this.inputBuffer.addActiveFilter(inputFilters[1]);
                this.contentDelimitation = true;
            }
            else {
                for (int i = this.pluggableFilterIndex; i < inputFilters.length; ++i) {
                    if (inputFilters[i].getEncodingName().toString().equals(encodingName)) {
                        this.inputBuffer.addActiveFilter(inputFilters[i]);
                        return;
                    }
                }
                this.response.setStatus(501);
                this.setErrorState(ErrorState.CLOSE_CLEAN, null);
                if (Http11Processor.log.isDebugEnabled()) {
                    Http11Processor.log.debug(Http11Processor.sm.getString("http11processor.request.prepare") + " Unsupported transfer encoding [" + encodingName + "]");
                }
            }
        }
    }
    
    public AbstractEndpoint.Handler.SocketState service(final SocketWrapperBase<?> socketWrapper) throws IOException {
        final RequestInfo rp = this.request.getRequestProcessor();
        rp.setStage(1);
        this.setSocketWrapper(socketWrapper);
        this.inputBuffer.init(socketWrapper);
        this.outputBuffer.init(socketWrapper);
        this.keepAlive = true;
        this.openSocket = false;
        this.readComplete = true;
        boolean keptAlive = false;
        SendfileState sendfileState;
        for (sendfileState = SendfileState.DONE; !this.getErrorState().isError() && this.keepAlive && !this.isAsync() && this.upgradeToken == null && sendfileState == SendfileState.DONE && !this.endpoint.isPaused(); sendfileState = this.processSendfile(socketWrapper)) {
            try {
                if (!this.inputBuffer.parseRequestLine(keptAlive)) {
                    if (this.inputBuffer.getParsingRequestLinePhase() == -1) {
                        return AbstractEndpoint.Handler.SocketState.UPGRADING;
                    }
                    if (this.handleIncompleteRequestLineRead()) {
                        break;
                    }
                }
                if (this.endpoint.isPaused()) {
                    this.response.setStatus(503);
                    this.setErrorState(ErrorState.CLOSE_CLEAN, null);
                }
                else {
                    keptAlive = true;
                    this.request.getMimeHeaders().setLimit(this.endpoint.getMaxHeaderCount());
                    if (!this.inputBuffer.parseHeaders()) {
                        this.openSocket = true;
                        this.readComplete = false;
                        break;
                    }
                    if (!this.disableUploadTimeout) {
                        socketWrapper.setReadTimeout(this.connectionUploadTimeout);
                    }
                }
            }
            catch (IOException e) {
                if (Http11Processor.log.isDebugEnabled()) {
                    Http11Processor.log.debug(Http11Processor.sm.getString("http11processor.header.parse"), e);
                }
                this.setErrorState(ErrorState.CLOSE_CONNECTION_NOW, e);
                break;
            }
            catch (Throwable t) {
                ExceptionUtils.handleThrowable(t);
                final UserDataHelper.Mode logMode = this.userDataHelper.getNextMode();
                if (logMode != null) {
                    String message = Http11Processor.sm.getString("http11processor.header.parse");
                    switch (logMode) {
                        case INFO_THEN_DEBUG: {
                            message += Http11Processor.sm.getString("http11processor.fallToDebug");
                        }
                        case INFO: {
                            Http11Processor.log.info(message, t);
                            break;
                        }
                        case DEBUG: {
                            Http11Processor.log.debug(message, t);
                            break;
                        }
                    }
                }
                this.response.setStatus(400);
                this.setErrorState(ErrorState.CLOSE_CLEAN, t);
                this.getAdapter().log(this.request, this.response, 0L);
            }
            Enumeration<String> connectionValues;
            boolean foundUpgrade;
            for (connectionValues = this.request.getMimeHeaders().values("Connection"), foundUpgrade = false; connectionValues.hasMoreElements() && !foundUpgrade; foundUpgrade = connectionValues.nextElement().toLowerCase(Locale.ENGLISH).contains("upgrade")) {}
            if (foundUpgrade) {
                final String requestedProtocol = this.request.getHeader("Upgrade");
                final UpgradeProtocol upgradeProtocol = this.httpUpgradeProtocols.get(requestedProtocol);
                if (upgradeProtocol != null && upgradeProtocol.accept(this.request)) {
                    this.response.setStatus(101);
                    this.response.setHeader("Connection", "Upgrade");
                    this.response.setHeader("Upgrade", requestedProtocol);
                    this.action(ActionCode.CLOSE, null);
                    this.getAdapter().log(this.request, this.response, 0L);
                    final InternalHttpUpgradeHandler upgradeHandler = upgradeProtocol.getInternalUpgradeHandler(this.getAdapter(), this.cloneRequest(this.request));
                    final UpgradeToken upgradeToken = new UpgradeToken(upgradeHandler, null, null);
                    this.action(ActionCode.UPGRADE, upgradeToken);
                    return AbstractEndpoint.Handler.SocketState.UPGRADING;
                }
            }
            if (!this.getErrorState().isError()) {
                rp.setStage(2);
                try {
                    this.prepareRequest();
                }
                catch (Throwable t2) {
                    ExceptionUtils.handleThrowable(t2);
                    if (Http11Processor.log.isDebugEnabled()) {
                        Http11Processor.log.debug(Http11Processor.sm.getString("http11processor.request.prepare"), t2);
                    }
                    this.response.setStatus(500);
                    this.setErrorState(ErrorState.CLOSE_CLEAN, t2);
                    this.getAdapter().log(this.request, this.response, 0L);
                }
            }
            if (this.maxKeepAliveRequests == 1) {
                this.keepAlive = false;
            }
            else if (this.maxKeepAliveRequests > 0 && socketWrapper.decrementKeepAlive() <= 0) {
                this.keepAlive = false;
            }
            if (!this.getErrorState().isError()) {
                try {
                    rp.setStage(3);
                    this.getAdapter().service(this.request, this.response);
                    if (this.keepAlive && !this.getErrorState().isError() && !this.isAsync() && statusDropsConnection(this.response.getStatus())) {
                        this.setErrorState(ErrorState.CLOSE_CLEAN, null);
                    }
                }
                catch (InterruptedIOException e2) {
                    this.setErrorState(ErrorState.CLOSE_CONNECTION_NOW, e2);
                }
                catch (HeadersTooLargeException e3) {
                    Http11Processor.log.error(Http11Processor.sm.getString("http11processor.request.process"), e3);
                    if (this.response.isCommitted()) {
                        this.setErrorState(ErrorState.CLOSE_NOW, e3);
                    }
                    else {
                        this.response.reset();
                        this.response.setStatus(500);
                        this.setErrorState(ErrorState.CLOSE_CLEAN, e3);
                        this.response.setHeader("Connection", "close");
                    }
                }
                catch (Throwable t2) {
                    ExceptionUtils.handleThrowable(t2);
                    Http11Processor.log.error(Http11Processor.sm.getString("http11processor.request.process"), t2);
                    this.response.setStatus(500);
                    this.setErrorState(ErrorState.CLOSE_CLEAN, t2);
                    this.getAdapter().log(this.request, this.response, 0L);
                }
            }
            rp.setStage(4);
            if (!this.isAsync()) {
                this.endRequest();
            }
            rp.setStage(5);
            if (this.getErrorState().isError()) {
                this.response.setStatus(500);
            }
            if (!this.isAsync() || this.getErrorState().isError()) {
                this.request.updateCounters();
                if (this.getErrorState().isIoAllowed()) {
                    this.inputBuffer.nextRequest();
                    this.outputBuffer.nextRequest();
                }
            }
            if (!this.disableUploadTimeout) {
                final int soTimeout = this.endpoint.getConnectionTimeout();
                if (soTimeout > 0) {
                    socketWrapper.setReadTimeout(soTimeout);
                }
                else {
                    socketWrapper.setReadTimeout(0L);
                }
            }
            rp.setStage(6);
        }
        rp.setStage(7);
        if (this.getErrorState().isError() || this.endpoint.isPaused()) {
            return AbstractEndpoint.Handler.SocketState.CLOSED;
        }
        if (this.isAsync()) {
            return AbstractEndpoint.Handler.SocketState.LONG;
        }
        if (this.isUpgrade()) {
            return AbstractEndpoint.Handler.SocketState.UPGRADING;
        }
        if (sendfileState == SendfileState.PENDING) {
            return AbstractEndpoint.Handler.SocketState.SENDFILE;
        }
        if (!this.openSocket) {
            return AbstractEndpoint.Handler.SocketState.CLOSED;
        }
        if (this.readComplete) {
            return AbstractEndpoint.Handler.SocketState.OPEN;
        }
        return AbstractEndpoint.Handler.SocketState.LONG;
    }
    
    private Request cloneRequest(final Request source) throws IOException {
        final Request dest = new Request();
        dest.decodedURI().duplicate(source.decodedURI());
        dest.method().duplicate(source.method());
        dest.getMimeHeaders().duplicate(source.getMimeHeaders());
        dest.requestURI().duplicate(source.requestURI());
        return dest;
    }
    
    private boolean handleIncompleteRequestLineRead() {
        this.openSocket = true;
        if (this.inputBuffer.getParsingRequestLinePhase() > 1) {
            if (this.endpoint.isPaused()) {
                this.response.setStatus(503);
                this.setErrorState(ErrorState.CLOSE_CLEAN, null);
                this.getAdapter().log(this.request, this.response, 0L);
                return false;
            }
            this.readComplete = false;
        }
        return true;
    }
    
    private void checkExpectationAndResponseStatus() {
        if (this.request.hasExpectation() && (this.response.getStatus() < 200 || this.response.getStatus() > 299)) {
            this.inputBuffer.setSwallowInput(false);
            this.keepAlive = false;
        }
    }
    
    private void prepareRequest() {
        this.http11 = true;
        this.http09 = false;
        this.contentDelimitation = false;
        if (this.endpoint.isSSLEnabled()) {
            this.request.scheme().setString("https");
        }
        final MessageBytes protocolMB = this.request.protocol();
        if (protocolMB.equals("HTTP/1.1")) {
            this.http11 = true;
            protocolMB.setString("HTTP/1.1");
        }
        else if (protocolMB.equals("HTTP/1.0")) {
            this.http11 = false;
            this.keepAlive = false;
            protocolMB.setString("HTTP/1.0");
        }
        else if (protocolMB.equals("")) {
            this.http09 = true;
            this.http11 = false;
            this.keepAlive = false;
        }
        else {
            this.http11 = false;
            this.response.setStatus(505);
            this.setErrorState(ErrorState.CLOSE_CLEAN, null);
            if (Http11Processor.log.isDebugEnabled()) {
                Http11Processor.log.debug(Http11Processor.sm.getString("http11processor.request.prepare") + " Unsupported HTTP version \"" + protocolMB + "\"");
            }
        }
        final MimeHeaders headers = this.request.getMimeHeaders();
        final MessageBytes connectionValueMB = headers.getValue("Connection");
        if (connectionValueMB != null) {
            final ByteChunk connectionValueBC = connectionValueMB.getByteChunk();
            if (findBytes(connectionValueBC, Constants.CLOSE_BYTES) != -1) {
                this.keepAlive = false;
            }
            else if (findBytes(connectionValueBC, Constants.KEEPALIVE_BYTES) != -1) {
                this.keepAlive = true;
            }
        }
        if (this.http11) {
            final MessageBytes expectMB = headers.getValue("expect");
            if (expectMB != null) {
                if (expectMB.indexOfIgnoreCase("100-continue", 0) != -1) {
                    this.inputBuffer.setSwallowInput(false);
                    this.request.setExpectation(true);
                }
                else {
                    this.response.setStatus(417);
                    this.setErrorState(ErrorState.CLOSE_CLEAN, null);
                }
            }
        }
        if (this.restrictedUserAgents != null && (this.http11 || this.keepAlive)) {
            final MessageBytes userAgentValueMB = headers.getValue("user-agent");
            if (userAgentValueMB != null) {
                final String userAgentValue = userAgentValueMB.toString();
                if (this.restrictedUserAgents != null && this.restrictedUserAgents.matcher(userAgentValue).matches()) {
                    this.http11 = false;
                    this.keepAlive = false;
                }
            }
        }
        MessageBytes hostValueMB = null;
        try {
            hostValueMB = headers.getUniqueValue("host");
        }
        catch (IllegalArgumentException iae) {
            this.response.setStatus(400);
            this.setErrorState(ErrorState.CLOSE_CLEAN, null);
            if (Http11Processor.log.isDebugEnabled()) {
                Http11Processor.log.debug(Http11Processor.sm.getString("http11processor.request.multipleHosts"));
            }
        }
        if (this.http11 && hostValueMB == null) {
            this.response.setStatus(400);
            this.setErrorState(ErrorState.CLOSE_CLEAN, null);
            if (Http11Processor.log.isDebugEnabled()) {
                Http11Processor.log.debug(Http11Processor.sm.getString("http11processor.request.prepare") + " host header missing");
            }
        }
        final ByteChunk uriBC = this.request.requestURI().getByteChunk();
        if (uriBC.startsWithIgnoreCase("http", 0)) {
            int pos = uriBC.indexOf("://", 0, 3, 4);
            final int uriBCStart = uriBC.getStart();
            int slashPos = -1;
            if (pos != -1) {
                pos += 3;
                final byte[] uriB = uriBC.getBytes();
                slashPos = uriBC.indexOf('/', pos);
                final int atPos = uriBC.indexOf('@', pos);
                if (slashPos == -1) {
                    slashPos = uriBC.getLength();
                    this.request.requestURI().setBytes(uriB, uriBCStart + pos - 2, 1);
                }
                else {
                    this.request.requestURI().setBytes(uriB, uriBCStart + slashPos, uriBC.getLength() - slashPos);
                }
                if (atPos != -1) {
                    pos = atPos + 1;
                }
                if (this.http11) {
                    if (hostValueMB != null && !hostValueMB.getByteChunk().equals(uriB, uriBCStart + pos, slashPos - pos)) {
                        if (this.allowHostHeaderMismatch) {
                            hostValueMB = headers.setValue("host");
                            hostValueMB.setBytes(uriB, uriBCStart + pos, slashPos - pos);
                        }
                        else {
                            this.response.setStatus(400);
                            this.setErrorState(ErrorState.CLOSE_CLEAN, null);
                            if (Http11Processor.log.isDebugEnabled()) {
                                Http11Processor.log.debug(Http11Processor.sm.getString("http11processor.request.inconsistentHosts"));
                            }
                        }
                    }
                }
                else {
                    hostValueMB = headers.setValue("host");
                    hostValueMB.setBytes(uriB, uriBCStart + pos, slashPos - pos);
                }
            }
        }
        final InputFilter[] inputFilters = this.inputBuffer.getFilters();
        if (this.http11) {
            final MessageBytes transferEncodingValueMB = headers.getValue("transfer-encoding");
            if (transferEncodingValueMB != null) {
                final String transferEncodingValue = transferEncodingValueMB.toString();
                int startPos = 0;
                int commaPos = transferEncodingValue.indexOf(44);
                String encodingName = null;
                while (commaPos != -1) {
                    encodingName = transferEncodingValue.substring(startPos, commaPos);
                    this.addInputFilter(inputFilters, encodingName);
                    startPos = commaPos + 1;
                    commaPos = transferEncodingValue.indexOf(44, startPos);
                }
                encodingName = transferEncodingValue.substring(startPos);
                this.addInputFilter(inputFilters, encodingName);
            }
        }
        final long contentLength = this.request.getContentLengthLong();
        if (contentLength >= 0L) {
            if (this.contentDelimitation) {
                headers.removeHeader("content-length");
                this.request.setContentLength(-1L);
            }
            else {
                this.inputBuffer.addActiveFilter(inputFilters[0]);
                this.contentDelimitation = true;
            }
        }
        this.parseHost(hostValueMB);
        if (!this.contentDelimitation) {
            this.inputBuffer.addActiveFilter(inputFilters[2]);
            this.contentDelimitation = true;
        }
        if (this.getErrorState().isError()) {
            this.getAdapter().log(this.request, this.response, 0L);
        }
    }
    
    @Override
    protected final void prepareResponse() throws IOException {
        boolean entityBody = true;
        this.contentDelimitation = false;
        final OutputFilter[] outputFilters = this.outputBuffer.getFilters();
        if (this.http09) {
            this.outputBuffer.addActiveFilter(outputFilters[0]);
            this.outputBuffer.commit();
            return;
        }
        final int statusCode = this.response.getStatus();
        if (statusCode < 200 || statusCode == 204 || statusCode == 205 || statusCode == 304) {
            this.outputBuffer.addActiveFilter(outputFilters[2]);
            entityBody = false;
            this.contentDelimitation = true;
            if (statusCode == 205) {
                this.response.setContentLength(0L);
            }
            else {
                this.response.setContentLength(-1L);
            }
        }
        final MessageBytes methodMB = this.request.method();
        if (methodMB.equals("HEAD")) {
            this.outputBuffer.addActiveFilter(outputFilters[2]);
            this.contentDelimitation = true;
        }
        if (this.endpoint.getUseSendfile()) {
            this.prepareSendfile(outputFilters);
        }
        boolean isCompressible = false;
        boolean useCompression = false;
        if (entityBody && this.compressionLevel > 0 && this.sendfileData == null) {
            isCompressible = this.isCompressible();
            if (isCompressible) {
                useCompression = this.useCompression();
            }
            if (useCompression) {
                this.response.setContentLength(-1L);
            }
        }
        final MimeHeaders headers = this.response.getMimeHeaders();
        if (entityBody || statusCode == 204) {
            final String contentType = this.response.getContentType();
            if (contentType != null) {
                headers.setValue("Content-Type").setString(contentType);
            }
            final String contentLanguage = this.response.getContentLanguage();
            if (contentLanguage != null) {
                headers.setValue("Content-Language").setString(contentLanguage);
            }
        }
        final long contentLength = this.response.getContentLengthLong();
        boolean connectionClosePresent = false;
        if (contentLength != -1L) {
            headers.setValue("Content-Length").setLong(contentLength);
            this.outputBuffer.addActiveFilter(outputFilters[0]);
            this.contentDelimitation = true;
        }
        else {
            connectionClosePresent = isConnectionClose(headers);
            if (entityBody && this.http11 && !connectionClosePresent) {
                this.outputBuffer.addActiveFilter(outputFilters[1]);
                this.contentDelimitation = true;
                headers.addValue("Transfer-Encoding").setString("chunked");
            }
            else {
                this.outputBuffer.addActiveFilter(outputFilters[0]);
            }
        }
        if (useCompression) {
            this.outputBuffer.addActiveFilter(outputFilters[3]);
            headers.setValue("Content-Encoding").setString("gzip");
        }
        if (isCompressible) {
            final MessageBytes vary = headers.getValue("Vary");
            if (vary == null) {
                headers.setValue("Vary").setString("Accept-Encoding");
            }
            else if (!vary.equals("*")) {
                headers.setValue("Vary").setString(vary.getString() + ",Accept-Encoding");
            }
        }
        if (headers.getValue("Date") == null) {
            headers.addValue("Date").setString(FastHttpDateFormat.getCurrentDate());
        }
        if (entityBody && !this.contentDelimitation) {
            this.keepAlive = false;
        }
        this.checkExpectationAndResponseStatus();
        if (this.keepAlive && statusDropsConnection(statusCode)) {
            this.keepAlive = false;
        }
        if (!this.keepAlive) {
            if (!connectionClosePresent) {
                headers.addValue("Connection").setString("close");
            }
        }
        else if (!this.http11 && !this.getErrorState().isError()) {
            headers.addValue("Connection").setString("keep-alive");
        }
        if (this.server == null) {
            if (this.serverRemoveAppProvidedValues) {
                headers.removeHeader("server");
            }
        }
        else {
            headers.setValue("Server").setString(this.server);
        }
        try {
            this.outputBuffer.sendStatus();
            for (int size = headers.size(), i = 0; i < size; ++i) {
                this.outputBuffer.sendHeader(headers.getName(i), headers.getValue(i));
            }
            this.outputBuffer.endHeaders();
        }
        catch (Throwable t) {
            ExceptionUtils.handleThrowable(t);
            this.outputBuffer.resetHeaderBuffer();
            throw t;
        }
        this.outputBuffer.commit();
    }
    
    private static boolean isConnectionClose(final MimeHeaders headers) {
        final MessageBytes connection = headers.getValue("Connection");
        return connection != null && connection.equals("close");
    }
    
    private void prepareSendfile(final OutputFilter[] outputFilters) {
        final String fileName = (String)this.request.getAttribute("org.apache.tomcat.sendfile.filename");
        if (fileName == null) {
            this.sendfileData = null;
        }
        else {
            this.outputBuffer.addActiveFilter(outputFilters[2]);
            this.contentDelimitation = true;
            final long pos = (long)this.request.getAttribute("org.apache.tomcat.sendfile.start");
            final long end = (long)this.request.getAttribute("org.apache.tomcat.sendfile.end");
            this.sendfileData = this.socketWrapper.createSendfileData(fileName, pos, end - pos);
        }
    }
    
    private void parseHost(final MessageBytes valueMB) {
        if (valueMB == null || valueMB.isNull()) {
            this.request.setServerPort(this.endpoint.getPort());
            return;
        }
        final ByteChunk valueBC = valueMB.getByteChunk();
        final byte[] valueB = valueBC.getBytes();
        final int valueL = valueBC.getLength();
        final int valueS = valueBC.getStart();
        int colonPos = -1;
        if (this.hostNameC.length < valueL) {
            this.hostNameC = new char[valueL];
        }
        final boolean ipv6 = valueB[valueS] == 91;
        boolean bracketClosed = false;
        for (int i = 0; i < valueL; ++i) {
            final char b = (char)valueB[i + valueS];
            if ((this.hostNameC[i] = b) == ']') {
                bracketClosed = true;
            }
            else if (b == ':' && (!ipv6 || bracketClosed)) {
                colonPos = i;
                break;
            }
        }
        if (colonPos < 0) {
            this.request.serverName().setChars(this.hostNameC, 0, valueL);
        }
        else {
            this.request.serverName().setChars(this.hostNameC, 0, colonPos);
            int port = 0;
            int mult = 1;
            for (int j = valueL - 1; j > colonPos; --j) {
                final int charValue = HexUtils.getDec(valueB[j + valueS]);
                if (charValue == -1 || charValue > 9) {
                    this.response.setStatus(400);
                    this.setErrorState(ErrorState.CLOSE_CLEAN, null);
                    break;
                }
                port += charValue * mult;
                mult *= 10;
            }
            this.request.setServerPort(port);
        }
    }
    
    @Override
    protected boolean flushBufferedWrite() throws IOException {
        if (this.outputBuffer.hasDataToWrite() && this.outputBuffer.flushBuffer(false)) {
            this.outputBuffer.registerWriteInterest();
            return true;
        }
        return false;
    }
    
    @Override
    protected AbstractEndpoint.Handler.SocketState dispatchEndRequest() {
        if (!this.keepAlive) {
            return AbstractEndpoint.Handler.SocketState.CLOSED;
        }
        this.endRequest();
        this.inputBuffer.nextRequest();
        this.outputBuffer.nextRequest();
        if (this.socketWrapper.isReadPending()) {
            return AbstractEndpoint.Handler.SocketState.LONG;
        }
        return AbstractEndpoint.Handler.SocketState.OPEN;
    }
    
    @Override
    protected Log getLog() {
        return Http11Processor.log;
    }
    
    private void endRequest() {
        if (this.getErrorState().isError()) {
            this.inputBuffer.setSwallowInput(false);
        }
        else {
            this.checkExpectationAndResponseStatus();
        }
        if (this.getErrorState().isIoAllowed()) {
            try {
                this.inputBuffer.endRequest();
            }
            catch (IOException e) {
                this.setErrorState(ErrorState.CLOSE_CONNECTION_NOW, e);
            }
            catch (Throwable t) {
                ExceptionUtils.handleThrowable(t);
                this.response.setStatus(500);
                this.setErrorState(ErrorState.CLOSE_NOW, t);
                Http11Processor.log.error(Http11Processor.sm.getString("http11processor.request.finish"), t);
            }
        }
        if (this.getErrorState().isIoAllowed()) {
            try {
                this.action(ActionCode.COMMIT, null);
                this.outputBuffer.finishResponse();
            }
            catch (IOException e) {
                this.setErrorState(ErrorState.CLOSE_CONNECTION_NOW, e);
            }
            catch (Throwable t) {
                ExceptionUtils.handleThrowable(t);
                this.setErrorState(ErrorState.CLOSE_NOW, t);
                Http11Processor.log.error(Http11Processor.sm.getString("http11processor.response.finish"), t);
            }
        }
    }
    
    @Override
    protected final void finishResponse() throws IOException {
        this.outputBuffer.finishResponse();
    }
    
    @Override
    protected final void ack() {
        if (!this.response.isCommitted() && this.request.hasExpectation()) {
            this.inputBuffer.setSwallowInput(true);
            try {
                this.outputBuffer.sendAck();
            }
            catch (IOException e) {
                this.setErrorState(ErrorState.CLOSE_CONNECTION_NOW, e);
            }
        }
    }
    
    @Override
    protected final void flush() throws IOException {
        this.outputBuffer.flush();
    }
    
    @Override
    protected final int available(final boolean doRead) {
        return this.inputBuffer.available(doRead);
    }
    
    @Override
    protected final void setRequestBody(final ByteChunk body) {
        final InputFilter savedBody = new SavedRequestInputFilter(body);
        savedBody.setRequest(this.request);
        final Http11InputBuffer internalBuffer = (Http11InputBuffer)this.request.getInputBuffer();
        internalBuffer.addActiveFilter(savedBody);
    }
    
    @Override
    protected final void setSwallowResponse() {
        this.outputBuffer.responseFinished = true;
    }
    
    @Override
    protected final void disableSwallowRequest() {
        this.inputBuffer.setSwallowInput(false);
    }
    
    @Override
    protected final void sslReHandShake() throws IOException {
        if (this.sslSupport != null) {
            final InputFilter[] inputFilters = this.inputBuffer.getFilters();
            ((BufferedInputFilter)inputFilters[3]).setLimit(this.maxSavePostSize);
            this.inputBuffer.addActiveFilter(inputFilters[3]);
            this.socketWrapper.doClientAuth(this.sslSupport);
            try {
                final Object sslO = this.sslSupport.getPeerCertificateChain();
                if (sslO != null) {
                    this.request.setAttribute("javax.servlet.request.X509Certificate", sslO);
                }
            }
            catch (IOException ioe) {
                Http11Processor.log.warn(Http11Processor.sm.getString("http11processor.socket.ssl"), ioe);
            }
        }
    }
    
    @Override
    protected final boolean isRequestBodyFullyRead() {
        return this.inputBuffer.isFinished();
    }
    
    @Override
    protected final void registerReadInterest() {
        this.socketWrapper.registerReadInterest();
    }
    
    @Override
    protected final boolean isReady() {
        return this.outputBuffer.isReady();
    }
    
    @Override
    public UpgradeToken getUpgradeToken() {
        return this.upgradeToken;
    }
    
    @Override
    protected final void doHttpUpgrade(final UpgradeToken upgradeToken) {
        this.upgradeToken = upgradeToken;
        this.outputBuffer.responseFinished = true;
    }
    
    @Override
    public ByteBuffer getLeftoverInput() {
        return this.inputBuffer.getLeftover();
    }
    
    @Override
    public boolean isUpgrade() {
        return this.upgradeToken != null;
    }
    
    private SendfileState processSendfile(final SocketWrapperBase<?> socketWrapper) {
        this.openSocket = this.keepAlive;
        SendfileState result = SendfileState.DONE;
        if (this.sendfileData != null && !this.getErrorState().isError()) {
            if (this.keepAlive) {
                if (this.available(false) == 0) {
                    this.sendfileData.keepAliveState = SendfileKeepAliveState.OPEN;
                }
                else {
                    this.sendfileData.keepAliveState = SendfileKeepAliveState.PIPELINED;
                }
            }
            else {
                this.sendfileData.keepAliveState = SendfileKeepAliveState.NONE;
            }
            result = socketWrapper.processSendfile(this.sendfileData);
            switch (result) {
                case ERROR: {
                    if (Http11Processor.log.isDebugEnabled()) {
                        Http11Processor.log.debug(Http11Processor.sm.getString("http11processor.sendfile.error"));
                    }
                    this.setErrorState(ErrorState.CLOSE_CONNECTION_NOW, null);
                    break;
                }
            }
            this.sendfileData = null;
        }
        return result;
    }
    
    @Override
    public final void recycle() {
        this.getAdapter().checkRecycled(this.request, this.response);
        super.recycle();
        this.inputBuffer.recycle();
        this.outputBuffer.recycle();
        this.upgradeToken = null;
        this.socketWrapper = null;
        this.sendfileData = null;
    }
    
    @Override
    public void pause() {
    }
    
    static {
        log = LogFactory.getLog(Http11Processor.class);
        sm = StringManager.getManager(Http11Processor.class);
    }
}
