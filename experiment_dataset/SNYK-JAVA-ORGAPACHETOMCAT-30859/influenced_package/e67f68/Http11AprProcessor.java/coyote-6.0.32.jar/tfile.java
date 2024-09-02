// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.coyote.http11;

import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.buf.Ascii;
import org.apache.coyote.http11.filters.GzipOutputFilter;
import org.apache.coyote.http11.filters.VoidOutputFilter;
import org.apache.coyote.http11.filters.VoidInputFilter;
import org.apache.coyote.http11.filters.ChunkedOutputFilter;
import org.apache.coyote.http11.filters.ChunkedInputFilter;
import org.apache.coyote.http11.filters.IdentityOutputFilter;
import org.apache.coyote.http11.filters.IdentityInputFilter;
import org.apache.tomcat.util.http.FastHttpDateFormat;
import org.apache.tomcat.util.http.MimeHeaders;
import org.apache.tomcat.util.buf.MessageBytes;
import org.apache.tomcat.jni.Sockaddr;
import org.apache.coyote.http11.filters.SavedRequestInputFilter;
import org.apache.tomcat.util.buf.ByteChunk;
import org.apache.coyote.http11.filters.BufferedInputFilter;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import org.apache.tomcat.jni.SSLSocket;
import org.apache.tomcat.jni.Address;
import org.apache.coyote.ActionCode;
import org.apache.tomcat.jni.Socket;
import java.io.IOException;
import org.apache.coyote.RequestInfo;
import java.io.InterruptedIOException;
import org.apache.tomcat.util.net.SocketStatus;
import java.util.StringTokenizer;
import java.util.regex.PatternSyntaxException;
import org.apache.tomcat.util.buf.HexUtils;
import org.apache.coyote.OutputBuffer;
import org.apache.coyote.InputBuffer;
import java.util.regex.Pattern;
import org.apache.tomcat.util.net.AprEndpoint;
import org.apache.coyote.Response;
import org.apache.coyote.Request;
import org.apache.coyote.Adapter;
import org.apache.tomcat.util.res.StringManager;
import org.apache.juli.logging.Log;
import org.apache.coyote.ActionHook;

public class Http11AprProcessor implements ActionHook
{
    protected static Log log;
    protected static StringManager sm;
    private int pluggableFilterIndex;
    protected Adapter adapter;
    protected Request request;
    protected Response response;
    protected InternalAprInputBuffer inputBuffer;
    protected InternalAprOutputBuffer outputBuffer;
    protected boolean error;
    protected boolean keepAlive;
    protected boolean http11;
    protected boolean http09;
    protected AprEndpoint.SendfileData sendfileData;
    protected boolean comet;
    protected boolean contentDelimitation;
    protected boolean expectation;
    protected Pattern[] restrictedUserAgents;
    protected int maxKeepAliveRequests;
    protected boolean ssl;
    protected long socket;
    protected String remoteAddr;
    protected String remoteHost;
    protected String localName;
    protected int localPort;
    protected int remotePort;
    protected String localAddr;
    protected int timeout;
    protected boolean disableUploadTimeout;
    protected int compressionLevel;
    protected int compressionMinSize;
    protected int socketBuffer;
    protected int maxSavePostSize;
    protected Pattern[] noCompressionUserAgents;
    protected String[] compressableMimeTypes;
    protected char[] hostNameC;
    protected AprEndpoint endpoint;
    protected String server;
    
    public Http11AprProcessor(final int headerBufferSize, final AprEndpoint endpoint) {
        this.pluggableFilterIndex = Integer.MAX_VALUE;
        this.adapter = null;
        this.request = null;
        this.response = null;
        this.inputBuffer = null;
        this.outputBuffer = null;
        this.error = false;
        this.keepAlive = true;
        this.http11 = true;
        this.http09 = false;
        this.sendfileData = null;
        this.comet = false;
        this.contentDelimitation = true;
        this.expectation = false;
        this.restrictedUserAgents = null;
        this.maxKeepAliveRequests = -1;
        this.ssl = false;
        this.socket = 0L;
        this.remoteAddr = null;
        this.remoteHost = null;
        this.localName = null;
        this.localPort = -1;
        this.remotePort = -1;
        this.localAddr = null;
        this.timeout = 300000;
        this.disableUploadTimeout = false;
        this.compressionLevel = 0;
        this.compressionMinSize = 2048;
        this.socketBuffer = -1;
        this.maxSavePostSize = 4096;
        this.noCompressionUserAgents = null;
        this.compressableMimeTypes = new String[] { "text/html", "text/xml", "text/plain" };
        this.hostNameC = new char[0];
        this.server = null;
        this.endpoint = endpoint;
        this.request = new Request();
        this.inputBuffer = new InternalAprInputBuffer(this.request, headerBufferSize);
        this.request.setInputBuffer(this.inputBuffer);
        (this.response = new Response()).setHook(this);
        this.outputBuffer = new InternalAprOutputBuffer(this.response, headerBufferSize);
        this.response.setOutputBuffer(this.outputBuffer);
        this.request.setResponse(this.response);
        this.ssl = endpoint.isSSLEnabled();
        this.initializeFilters();
        final int foo = HexUtils.DEC[0];
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
    
    public void addNoCompressionUserAgent(final String userAgent) {
        try {
            final Pattern nRule = Pattern.compile(userAgent);
            this.noCompressionUserAgents = this.addREArray(this.noCompressionUserAgents, nRule);
        }
        catch (PatternSyntaxException pse) {
            Http11AprProcessor.log.error((Object)Http11AprProcessor.sm.getString("http11processor.regexp.error", userAgent), (Throwable)pse);
        }
    }
    
    public void setNoCompressionUserAgents(final Pattern[] noCompressionUserAgents) {
        this.noCompressionUserAgents = noCompressionUserAgents;
    }
    
    public void setNoCompressionUserAgents(final String noCompressionUserAgents) {
        if (noCompressionUserAgents != null) {
            final StringTokenizer st = new StringTokenizer(noCompressionUserAgents, ",");
            while (st.hasMoreTokens()) {
                this.addNoCompressionUserAgent(st.nextToken().trim());
            }
        }
    }
    
    public void addCompressableMimeType(final String mimeType) {
        this.compressableMimeTypes = this.addStringArray(this.compressableMimeTypes, mimeType);
    }
    
    public void setCompressableMimeTypes(final String[] compressableMimeTypes) {
        this.compressableMimeTypes = compressableMimeTypes;
    }
    
    public void setCompressableMimeTypes(final String compressableMimeTypes) {
        if (compressableMimeTypes != null) {
            this.compressableMimeTypes = null;
            final StringTokenizer st = new StringTokenizer(compressableMimeTypes, ",");
            while (st.hasMoreTokens()) {
                this.addCompressableMimeType(st.nextToken().trim());
            }
        }
    }
    
    public String[] findCompressableMimeTypes() {
        return this.compressableMimeTypes;
    }
    
    protected void addFilter(final String className) {
        try {
            final Class clazz = Class.forName(className);
            final Object obj = clazz.newInstance();
            if (obj instanceof InputFilter) {
                this.inputBuffer.addFilter((InputFilter)obj);
            }
            else if (obj instanceof OutputFilter) {
                this.outputBuffer.addFilter((OutputFilter)obj);
            }
            else {
                Http11AprProcessor.log.warn((Object)Http11AprProcessor.sm.getString("http11processor.filter.unknown", className));
            }
        }
        catch (Exception e) {
            Http11AprProcessor.log.error((Object)Http11AprProcessor.sm.getString("http11processor.filter.error", className), (Throwable)e);
        }
    }
    
    private String[] addStringArray(final String[] sArray, final String value) {
        String[] result = null;
        if (sArray == null) {
            result = new String[] { value };
        }
        else {
            result = new String[sArray.length + 1];
            for (int i = 0; i < sArray.length; ++i) {
                result[i] = sArray[i];
            }
            result[sArray.length] = value;
        }
        return result;
    }
    
    private Pattern[] addREArray(final Pattern[] rArray, final Pattern value) {
        Pattern[] result = null;
        if (rArray == null) {
            result = new Pattern[] { value };
        }
        else {
            result = new Pattern[rArray.length + 1];
            for (int i = 0; i < rArray.length; ++i) {
                result[i] = rArray[i];
            }
            result[rArray.length] = value;
        }
        return result;
    }
    
    private boolean inStringArray(final String[] sArray, final String value) {
        for (int i = 0; i < sArray.length; ++i) {
            if (sArray[i].equals(value)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean startsWithStringArray(final String[] sArray, final String value) {
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
    
    public void addRestrictedUserAgent(final String userAgent) {
        try {
            final Pattern nRule = Pattern.compile(userAgent);
            this.restrictedUserAgents = this.addREArray(this.restrictedUserAgents, nRule);
        }
        catch (PatternSyntaxException pse) {
            Http11AprProcessor.log.error((Object)Http11AprProcessor.sm.getString("http11processor.regexp.error", userAgent), (Throwable)pse);
        }
    }
    
    public void setRestrictedUserAgents(final Pattern[] restrictedUserAgents) {
        this.restrictedUserAgents = restrictedUserAgents;
    }
    
    public void setRestrictedUserAgents(final String restrictedUserAgents) {
        if (restrictedUserAgents != null) {
            final StringTokenizer st = new StringTokenizer(restrictedUserAgents, ",");
            while (st.hasMoreTokens()) {
                this.addRestrictedUserAgent(st.nextToken().trim());
            }
        }
    }
    
    public String[] findRestrictedUserAgents() {
        final String[] sarr = new String[this.restrictedUserAgents.length];
        for (int i = 0; i < this.restrictedUserAgents.length; ++i) {
            sarr[i] = this.restrictedUserAgents[i].toString();
        }
        return sarr;
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
    
    public void setSocketBuffer(final int socketBuffer) {
        this.socketBuffer = socketBuffer;
        this.outputBuffer.setSocketBuffer(socketBuffer);
    }
    
    public int getSocketBuffer() {
        return this.socketBuffer;
    }
    
    public void setTimeout(final int timeouts) {
        this.timeout = timeouts;
    }
    
    public int getTimeout() {
        return this.timeout;
    }
    
    public void setServer(final String server) {
        if (server == null || server.equals("")) {
            this.server = null;
        }
        else {
            this.server = server;
        }
    }
    
    public String getServer() {
        return this.server;
    }
    
    public Request getRequest() {
        return this.request;
    }
    
    public AprEndpoint.Handler.SocketState event(final SocketStatus status) throws IOException {
        final RequestInfo rp = this.request.getRequestProcessor();
        try {
            rp.setStage(3);
            this.error = !this.adapter.event(this.request, this.response, status);
        }
        catch (InterruptedIOException e) {
            this.error = true;
        }
        catch (Throwable t) {
            Http11AprProcessor.log.error((Object)Http11AprProcessor.sm.getString("http11processor.request.process"), t);
            this.response.setStatus(500);
            this.adapter.log(this.request, this.response, 0L);
            this.error = true;
        }
        rp.setStage(7);
        if (this.error) {
            this.inputBuffer.nextRequest();
            this.outputBuffer.nextRequest();
            this.recycle();
            return AprEndpoint.Handler.SocketState.CLOSED;
        }
        if (!this.comet) {
            this.inputBuffer.nextRequest();
            this.outputBuffer.nextRequest();
            this.recycle();
            return AprEndpoint.Handler.SocketState.OPEN;
        }
        return AprEndpoint.Handler.SocketState.LONG;
    }
    
    public AprEndpoint.Handler.SocketState process(final long socket) throws IOException {
        final RequestInfo rp = this.request.getRequestProcessor();
        rp.setStage(1);
        this.remoteAddr = null;
        this.remoteHost = null;
        this.localAddr = null;
        this.localName = null;
        this.remotePort = -1;
        this.localPort = -1;
        this.socket = socket;
        this.inputBuffer.setSocket(socket);
        this.outputBuffer.setSocket(socket);
        this.error = false;
        this.comet = false;
        this.keepAlive = true;
        int keepAliveLeft = this.maxKeepAliveRequests;
        final long soTimeout = this.endpoint.getSoTimeout();
        boolean keptAlive = false;
        boolean openSocket = false;
        while (!this.error && this.keepAlive && !this.comet) {
            try {
                if (!this.disableUploadTimeout && keptAlive && soTimeout > 0L) {
                    Socket.timeoutSet(socket, soTimeout * 1000L);
                }
                if (!this.inputBuffer.parseRequestLine(keptAlive)) {
                    openSocket = true;
                    this.endpoint.getPoller().add(socket);
                    break;
                }
                this.request.setStartTime(System.currentTimeMillis());
                keptAlive = true;
                if (!this.disableUploadTimeout) {
                    Socket.timeoutSet(socket, this.timeout * 1000);
                }
                this.inputBuffer.parseHeaders();
            }
            catch (IOException e) {
                this.error = true;
                break;
            }
            catch (Throwable t) {
                if (Http11AprProcessor.log.isDebugEnabled()) {
                    Http11AprProcessor.log.debug((Object)Http11AprProcessor.sm.getString("http11processor.header.parse"), t);
                }
                this.response.setStatus(400);
                this.adapter.log(this.request, this.response, 0L);
                this.error = true;
            }
            if (!this.error) {
                rp.setStage(2);
                try {
                    this.prepareRequest();
                }
                catch (Throwable t) {
                    if (Http11AprProcessor.log.isDebugEnabled()) {
                        Http11AprProcessor.log.debug((Object)Http11AprProcessor.sm.getString("http11processor.request.prepare"), t);
                    }
                    this.response.setStatus(400);
                    this.adapter.log(this.request, this.response, 0L);
                    this.error = true;
                }
            }
            if (this.maxKeepAliveRequests > 0 && --keepAliveLeft == 0) {
                this.keepAlive = false;
            }
            if (!this.error) {
                try {
                    rp.setStage(3);
                    this.adapter.service(this.request, this.response);
                    if (this.keepAlive && !this.error) {
                        this.error = (this.response.getErrorException() != null || this.statusDropsConnection(this.response.getStatus()));
                    }
                }
                catch (InterruptedIOException e2) {
                    this.error = true;
                }
                catch (Throwable t) {
                    Http11AprProcessor.log.error((Object)Http11AprProcessor.sm.getString("http11processor.request.process"), t);
                    this.response.setStatus(500);
                    this.adapter.log(this.request, this.response, 0L);
                    this.error = true;
                }
            }
            if (!this.comet) {
                if (this.error) {
                    this.inputBuffer.setSwallowInput(false);
                }
                this.endRequest();
            }
            if (this.error) {
                this.response.setStatus(500);
            }
            this.request.updateCounters();
            if (!this.comet) {
                this.inputBuffer.nextRequest();
                this.outputBuffer.nextRequest();
            }
            if (this.sendfileData != null && !this.error) {
                this.sendfileData.socket = socket;
                this.sendfileData.keepAlive = this.keepAlive;
                if (!this.endpoint.getSendfile().add(this.sendfileData)) {
                    openSocket = true;
                    break;
                }
            }
            rp.setStage(6);
        }
        rp.setStage(7);
        if (!this.comet) {
            this.recycle();
            return openSocket ? AprEndpoint.Handler.SocketState.OPEN : AprEndpoint.Handler.SocketState.CLOSED;
        }
        if (this.error) {
            this.inputBuffer.nextRequest();
            this.outputBuffer.nextRequest();
            this.recycle();
            return AprEndpoint.Handler.SocketState.CLOSED;
        }
        return AprEndpoint.Handler.SocketState.LONG;
    }
    
    public void endRequest() {
        try {
            this.inputBuffer.endRequest();
        }
        catch (IOException e) {
            this.error = true;
        }
        catch (Throwable t) {
            Http11AprProcessor.log.error((Object)Http11AprProcessor.sm.getString("http11processor.request.finish"), t);
            this.response.setStatus(500);
            this.adapter.log(this.request, this.response, 0L);
            this.error = true;
        }
        try {
            this.outputBuffer.endRequest();
        }
        catch (IOException e) {
            this.error = true;
        }
        catch (Throwable t) {
            Http11AprProcessor.log.error((Object)Http11AprProcessor.sm.getString("http11processor.response.finish"), t);
            this.error = true;
        }
    }
    
    public void recycle() {
        this.inputBuffer.recycle();
        this.outputBuffer.recycle();
        this.socket = 0L;
    }
    
    public void action(final ActionCode actionCode, final Object param) {
        if (actionCode == ActionCode.ACTION_COMMIT) {
            if (this.response.isCommitted()) {
                return;
            }
            this.prepareResponse();
            try {
                this.outputBuffer.commit();
            }
            catch (IOException e) {
                this.error = true;
            }
        }
        else if (actionCode == ActionCode.ACTION_ACK) {
            if (this.response.isCommitted() || !this.expectation) {
                return;
            }
            this.inputBuffer.setSwallowInput(true);
            try {
                this.outputBuffer.sendAck();
            }
            catch (IOException e) {
                this.error = true;
            }
        }
        else if (actionCode == ActionCode.ACTION_CLIENT_FLUSH) {
            try {
                this.outputBuffer.flush();
            }
            catch (IOException e) {
                this.error = true;
                this.response.setErrorException(e);
            }
        }
        else if (actionCode == ActionCode.ACTION_CLOSE) {
            this.comet = false;
            try {
                this.outputBuffer.endRequest();
            }
            catch (IOException e) {
                this.error = true;
            }
        }
        else if (actionCode == ActionCode.ACTION_RESET) {
            this.outputBuffer.reset();
        }
        else if (actionCode != ActionCode.ACTION_CUSTOM) {
            if (actionCode == ActionCode.ACTION_REQ_HOST_ADDR_ATTRIBUTE) {
                if (this.remoteAddr == null && this.socket != 0L) {
                    try {
                        final long sa = Address.get(1, this.socket);
                        this.remoteAddr = Address.getip(sa);
                    }
                    catch (Exception e2) {
                        Http11AprProcessor.log.warn((Object)Http11AprProcessor.sm.getString("http11processor.socket.info"), (Throwable)e2);
                    }
                }
                this.request.remoteAddr().setString(this.remoteAddr);
            }
            else if (actionCode == ActionCode.ACTION_REQ_LOCAL_NAME_ATTRIBUTE) {
                if (this.localName == null && this.socket != 0L) {
                    try {
                        final long sa = Address.get(0, this.socket);
                        this.localName = Address.getnameinfo(sa, 0);
                    }
                    catch (Exception e2) {
                        Http11AprProcessor.log.warn((Object)Http11AprProcessor.sm.getString("http11processor.socket.info"), (Throwable)e2);
                    }
                }
                this.request.localName().setString(this.localName);
            }
            else if (actionCode == ActionCode.ACTION_REQ_HOST_ATTRIBUTE) {
                if (this.remoteHost == null && this.socket != 0L) {
                    try {
                        final long sa = Address.get(1, this.socket);
                        this.remoteHost = Address.getnameinfo(sa, 0);
                        if (this.remoteHost == null) {
                            this.remoteHost = Address.getip(sa);
                        }
                    }
                    catch (Exception e2) {
                        Http11AprProcessor.log.warn((Object)Http11AprProcessor.sm.getString("http11processor.socket.info"), (Throwable)e2);
                    }
                }
                this.request.remoteHost().setString(this.remoteHost);
            }
            else if (actionCode == ActionCode.ACTION_REQ_LOCAL_ADDR_ATTRIBUTE) {
                if (this.localAddr == null && this.socket != 0L) {
                    try {
                        final long sa = Address.get(0, this.socket);
                        this.localAddr = Address.getip(sa);
                    }
                    catch (Exception e2) {
                        Http11AprProcessor.log.warn((Object)Http11AprProcessor.sm.getString("http11processor.socket.info"), (Throwable)e2);
                    }
                }
                this.request.localAddr().setString(this.localAddr);
            }
            else if (actionCode == ActionCode.ACTION_REQ_REMOTEPORT_ATTRIBUTE) {
                if (this.remotePort == -1 && this.socket != 0L) {
                    try {
                        final long sa = Address.get(1, this.socket);
                        final Sockaddr addr = Address.getInfo(sa);
                        this.remotePort = addr.port;
                    }
                    catch (Exception e2) {
                        Http11AprProcessor.log.warn((Object)Http11AprProcessor.sm.getString("http11processor.socket.info"), (Throwable)e2);
                    }
                }
                this.request.setRemotePort(this.remotePort);
            }
            else if (actionCode == ActionCode.ACTION_REQ_LOCALPORT_ATTRIBUTE) {
                if (this.localPort == -1 && this.socket != 0L) {
                    try {
                        final long sa = Address.get(0, this.socket);
                        final Sockaddr addr = Address.getInfo(sa);
                        this.localPort = addr.port;
                    }
                    catch (Exception e2) {
                        Http11AprProcessor.log.warn((Object)Http11AprProcessor.sm.getString("http11processor.socket.info"), (Throwable)e2);
                    }
                }
                this.request.setLocalPort(this.localPort);
            }
            else if (actionCode == ActionCode.ACTION_REQ_SSL_ATTRIBUTE) {
                if (this.ssl && this.socket != 0L) {
                    try {
                        Object sslO = SSLSocket.getInfoS(this.socket, 2);
                        if (sslO != null) {
                            this.request.setAttribute("javax.servlet.request.cipher_suite", sslO);
                        }
                        final int certLength = SSLSocket.getInfoI(this.socket, 1024);
                        final byte[] clientCert = SSLSocket.getInfoB(this.socket, 263);
                        X509Certificate[] certs = null;
                        if (clientCert != null && certLength > -1) {
                            certs = new X509Certificate[certLength + 1];
                            final CertificateFactory cf = CertificateFactory.getInstance("X.509");
                            certs[0] = (X509Certificate)cf.generateCertificate(new ByteArrayInputStream(clientCert));
                            for (int i = 0; i < certLength; ++i) {
                                final byte[] data = SSLSocket.getInfoB(this.socket, 1024 + i);
                                certs[i + 1] = (X509Certificate)cf.generateCertificate(new ByteArrayInputStream(data));
                            }
                        }
                        if (certs != null) {
                            this.request.setAttribute("javax.servlet.request.X509Certificate", certs);
                        }
                        sslO = new Integer(SSLSocket.getInfoI(this.socket, 3));
                        if (sslO != null) {
                            this.request.setAttribute("javax.servlet.request.key_size", sslO);
                        }
                        sslO = SSLSocket.getInfoS(this.socket, 1);
                        if (sslO != null) {
                            this.request.setAttribute("javax.servlet.request.ssl_session", sslO);
                        }
                    }
                    catch (Exception e2) {
                        Http11AprProcessor.log.warn((Object)Http11AprProcessor.sm.getString("http11processor.socket.ssl"), (Throwable)e2);
                    }
                }
            }
            else if (actionCode == ActionCode.ACTION_REQ_SSL_CERTIFICATE) {
                if (this.ssl && this.socket != 0L) {
                    final InputFilter[] inputFilters = this.inputBuffer.getFilters();
                    ((BufferedInputFilter)inputFilters[3]).setLimit(this.maxSavePostSize);
                    this.inputBuffer.addActiveFilter(inputFilters[3]);
                    try {
                        SSLSocket.setVerify(this.socket, 2, this.endpoint.getSSLVerifyDepth());
                        if (SSLSocket.renegotiate(this.socket) == 0) {
                            final int certLength = SSLSocket.getInfoI(this.socket, 1024);
                            final byte[] clientCert = SSLSocket.getInfoB(this.socket, 263);
                            X509Certificate[] certs = null;
                            if (clientCert != null && certLength > -1) {
                                certs = new X509Certificate[certLength + 1];
                                final CertificateFactory cf = CertificateFactory.getInstance("X.509");
                                certs[0] = (X509Certificate)cf.generateCertificate(new ByteArrayInputStream(clientCert));
                                for (int i = 0; i < certLength; ++i) {
                                    final byte[] data = SSLSocket.getInfoB(this.socket, 1024 + i);
                                    certs[i + 1] = (X509Certificate)cf.generateCertificate(new ByteArrayInputStream(data));
                                }
                            }
                            if (certs != null) {
                                this.request.setAttribute("javax.servlet.request.X509Certificate", certs);
                            }
                        }
                    }
                    catch (Exception e3) {
                        Http11AprProcessor.log.warn((Object)Http11AprProcessor.sm.getString("http11processor.socket.ssl"), (Throwable)e3);
                    }
                }
            }
            else if (actionCode == ActionCode.ACTION_REQ_SET_BODY_REPLAY) {
                final ByteChunk body = (ByteChunk)param;
                final InputFilter savedBody = new SavedRequestInputFilter(body);
                savedBody.setRequest(this.request);
                final InternalAprInputBuffer internalBuffer = (InternalAprInputBuffer)this.request.getInputBuffer();
                internalBuffer.addActiveFilter(savedBody);
            }
            else if (actionCode == ActionCode.ACTION_AVAILABLE) {
                this.request.setAvailable(this.inputBuffer.available());
            }
            else if (actionCode == ActionCode.ACTION_COMET_BEGIN) {
                this.comet = true;
            }
            else if (actionCode == ActionCode.ACTION_COMET_END) {
                this.comet = false;
            }
            else if (actionCode != ActionCode.ACTION_COMET_CLOSE) {
                if (actionCode == ActionCode.ACTION_COMET_SETTIMEOUT) {}
            }
        }
    }
    
    public void setAdapter(final Adapter adapter) {
        this.adapter = adapter;
    }
    
    public Adapter getAdapter() {
        return this.adapter;
    }
    
    protected void prepareRequest() {
        this.http11 = true;
        this.http09 = false;
        this.contentDelimitation = false;
        this.expectation = false;
        this.sendfileData = null;
        if (this.ssl) {
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
            this.error = true;
            this.response.setStatus(505);
            this.adapter.log(this.request, this.response, 0L);
        }
        final MessageBytes methodMB = this.request.method();
        if (methodMB.equals("GET")) {
            methodMB.setString("GET");
        }
        else if (methodMB.equals("POST")) {
            methodMB.setString("POST");
        }
        final MimeHeaders headers = this.request.getMimeHeaders();
        final MessageBytes connectionValueMB = headers.getValue("connection");
        if (connectionValueMB != null) {
            final ByteChunk connectionValueBC = connectionValueMB.getByteChunk();
            if (this.findBytes(connectionValueBC, Constants.CLOSE_BYTES) != -1) {
                this.keepAlive = false;
            }
            else if (this.findBytes(connectionValueBC, Constants.KEEPALIVE_BYTES) != -1) {
                this.keepAlive = true;
            }
        }
        MessageBytes expectMB = null;
        if (this.http11) {
            expectMB = headers.getValue("expect");
        }
        if (expectMB != null && expectMB.indexOfIgnoreCase("100-continue", 0) != -1) {
            this.inputBuffer.setSwallowInput(false);
            this.expectation = true;
        }
        if (this.restrictedUserAgents != null && (this.http11 || this.keepAlive)) {
            final MessageBytes userAgentValueMB = headers.getValue("user-agent");
            if (userAgentValueMB != null) {
                final String userAgentValue = userAgentValueMB.toString();
                for (int i = 0; i < this.restrictedUserAgents.length; ++i) {
                    if (this.restrictedUserAgents[i].matcher(userAgentValue).matches()) {
                        this.http11 = false;
                        this.keepAlive = false;
                        break;
                    }
                }
            }
        }
        final ByteChunk uriBC = this.request.requestURI().getByteChunk();
        if (uriBC.startsWithIgnoreCase("http", 0)) {
            final int pos = uriBC.indexOf("://", 0, 3, 4);
            final int uriBCStart = uriBC.getStart();
            int slashPos = -1;
            if (pos != -1) {
                final byte[] uriB = uriBC.getBytes();
                slashPos = uriBC.indexOf('/', pos + 3);
                if (slashPos == -1) {
                    slashPos = uriBC.getLength();
                    this.request.requestURI().setBytes(uriB, uriBCStart + pos + 1, 1);
                }
                else {
                    this.request.requestURI().setBytes(uriB, uriBCStart + slashPos, uriBC.getLength() - slashPos);
                }
                final MessageBytes hostMB = headers.setValue("host");
                hostMB.setBytes(uriB, uriBCStart + pos + 3, slashPos - pos - 3);
            }
        }
        final InputFilter[] inputFilters = this.inputBuffer.getFilters();
        MessageBytes transferEncodingValueMB = null;
        if (this.http11) {
            transferEncodingValueMB = headers.getValue("transfer-encoding");
        }
        if (transferEncodingValueMB != null) {
            final String transferEncodingValue = transferEncodingValueMB.toString();
            int startPos = 0;
            int commaPos = transferEncodingValue.indexOf(44);
            String encodingName = null;
            while (commaPos != -1) {
                encodingName = transferEncodingValue.substring(startPos, commaPos).toLowerCase().trim();
                if (!this.addInputFilter(inputFilters, encodingName)) {
                    this.error = true;
                    this.response.setStatus(501);
                    this.adapter.log(this.request, this.response, 0L);
                }
                startPos = commaPos + 1;
                commaPos = transferEncodingValue.indexOf(44, startPos);
            }
            encodingName = transferEncodingValue.substring(startPos).toLowerCase().trim();
            if (!this.addInputFilter(inputFilters, encodingName)) {
                this.error = true;
                this.response.setStatus(501);
                this.adapter.log(this.request, this.response, 0L);
            }
        }
        final long contentLength = this.request.getContentLengthLong();
        if (contentLength >= 0L && !this.contentDelimitation) {
            this.inputBuffer.addActiveFilter(inputFilters[0]);
            this.contentDelimitation = true;
        }
        final MessageBytes valueMB = headers.getValue("host");
        if (this.http11 && valueMB == null) {
            this.error = true;
            this.response.setStatus(400);
            this.adapter.log(this.request, this.response, 0L);
        }
        this.parseHost(valueMB);
        if (!this.contentDelimitation) {
            this.inputBuffer.addActiveFilter(inputFilters[2]);
            this.contentDelimitation = true;
        }
        if (this.endpoint.getUseSendfile()) {
            this.request.setAttribute("org.apache.tomcat.sendfile.support", Boolean.TRUE);
        }
        this.request.setAttribute("org.apache.tomcat.comet.support", Boolean.TRUE);
    }
    
    public void parseHost(final MessageBytes valueMB) {
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
            if (!this.ssl) {
                this.request.setServerPort(80);
            }
            else {
                this.request.setServerPort(443);
            }
            this.request.serverName().setChars(this.hostNameC, 0, valueL);
        }
        else {
            this.request.serverName().setChars(this.hostNameC, 0, colonPos);
            int port = 0;
            int mult = 1;
            for (int j = valueL - 1; j > colonPos; --j) {
                final int charValue = HexUtils.DEC[valueB[j + valueS]];
                if (charValue == -1) {
                    this.error = true;
                    this.response.setStatus(400);
                    this.adapter.log(this.request, this.response, 0L);
                    break;
                }
                port += charValue * mult;
                mult *= 10;
            }
            this.request.setServerPort(port);
        }
    }
    
    private boolean isCompressable() {
        final MessageBytes contentEncodingMB = this.response.getMimeHeaders().getValue("Content-Encoding");
        if (contentEncodingMB != null && contentEncodingMB.indexOf("gzip") != -1) {
            return false;
        }
        if (this.compressionLevel == 2) {
            return true;
        }
        final long contentLength = this.response.getContentLengthLong();
        return (contentLength == -1L || contentLength > this.compressionMinSize) && this.compressableMimeTypes != null && this.startsWithStringArray(this.compressableMimeTypes, this.response.getContentType());
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
                for (int i = 0; i < this.noCompressionUserAgents.length; ++i) {
                    if (this.noCompressionUserAgents[i].matcher(userAgentValue).matches()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
    
    protected void prepareResponse() {
        boolean entityBody = true;
        this.contentDelimitation = false;
        final OutputFilter[] outputFilters = this.outputBuffer.getFilters();
        if (this.http09) {
            this.outputBuffer.addActiveFilter(outputFilters[0]);
            return;
        }
        final int statusCode = this.response.getStatus();
        if (statusCode == 204 || statusCode == 205 || statusCode == 304) {
            this.outputBuffer.addActiveFilter(outputFilters[2]);
            entityBody = false;
            this.contentDelimitation = true;
        }
        final MessageBytes methodMB = this.request.method();
        if (methodMB.equals("HEAD")) {
            this.outputBuffer.addActiveFilter(outputFilters[2]);
            this.contentDelimitation = true;
        }
        if (this.endpoint.getUseSendfile()) {
            final String fileName = (String)this.request.getAttribute("org.apache.tomcat.sendfile.filename");
            if (fileName != null) {
                this.outputBuffer.addActiveFilter(outputFilters[2]);
                this.contentDelimitation = true;
                this.sendfileData = new AprEndpoint.SendfileData();
                this.sendfileData.fileName = fileName;
                this.sendfileData.start = (long)this.request.getAttribute("org.apache.tomcat.sendfile.start");
                this.sendfileData.end = (long)this.request.getAttribute("org.apache.tomcat.sendfile.end");
            }
        }
        boolean isCompressable = false;
        boolean useCompression = false;
        if (entityBody && this.compressionLevel > 0 && this.sendfileData == null) {
            isCompressable = this.isCompressable();
            if (isCompressable) {
                useCompression = this.useCompression();
            }
            if (useCompression) {
                this.response.setContentLength(-1);
            }
        }
        final MimeHeaders headers = this.response.getMimeHeaders();
        if (!entityBody) {
            this.response.setContentLength(-1);
        }
        else {
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
        if (contentLength != -1L) {
            headers.setValue("Content-Length").setLong(contentLength);
            this.outputBuffer.addActiveFilter(outputFilters[0]);
            this.contentDelimitation = true;
        }
        else if (entityBody && this.http11) {
            this.outputBuffer.addActiveFilter(outputFilters[1]);
            this.contentDelimitation = true;
            headers.addValue("Transfer-Encoding").setString("chunked");
        }
        else {
            this.outputBuffer.addActiveFilter(outputFilters[0]);
        }
        if (useCompression) {
            this.outputBuffer.addActiveFilter(outputFilters[3]);
            headers.setValue("Content-Encoding").setString("gzip");
        }
        if (isCompressable) {
            final MessageBytes vary = headers.getValue("Vary");
            if (vary == null) {
                headers.setValue("Vary").setString("Accept-Encoding");
            }
            else if (!vary.equals("*")) {
                headers.setValue("Vary").setString(vary.getString() + ",Accept-Encoding");
            }
        }
        headers.setValue("Date").setString(FastHttpDateFormat.getCurrentDate());
        if (entityBody && !this.contentDelimitation) {
            this.keepAlive = false;
        }
        if (!(this.keepAlive = (this.keepAlive && !this.statusDropsConnection(statusCode)))) {
            headers.addValue("Connection").setString("close");
        }
        else if (!this.http11 && !this.error) {
            headers.addValue("Connection").setString("keep-alive");
        }
        this.outputBuffer.sendStatus();
        if (this.server != null) {
            headers.setValue("Server").setString(this.server);
        }
        else if (headers.getValue("Server") == null) {
            this.outputBuffer.write(Constants.SERVER_BYTES);
        }
        for (int size = headers.size(), i = 0; i < size; ++i) {
            this.outputBuffer.sendHeader(headers.getName(i), headers.getValue(i));
        }
        this.outputBuffer.endHeaders();
    }
    
    protected void initializeFilters() {
        this.inputBuffer.addFilter(new IdentityInputFilter());
        this.outputBuffer.addFilter(new IdentityOutputFilter());
        this.inputBuffer.addFilter(new ChunkedInputFilter());
        this.outputBuffer.addFilter(new ChunkedOutputFilter());
        this.inputBuffer.addFilter(new VoidInputFilter());
        this.outputBuffer.addFilter(new VoidOutputFilter());
        this.inputBuffer.addFilter(new BufferedInputFilter());
        this.outputBuffer.addFilter(new GzipOutputFilter());
        this.pluggableFilterIndex = this.inputBuffer.filterLibrary.length;
    }
    
    protected boolean addInputFilter(final InputFilter[] inputFilters, final String encodingName) {
        if (!encodingName.equals("identity")) {
            if (!encodingName.equals("chunked")) {
                for (int i = this.pluggableFilterIndex; i < inputFilters.length; ++i) {
                    if (inputFilters[i].getEncodingName().toString().equals(encodingName)) {
                        this.inputBuffer.addActiveFilter(inputFilters[i]);
                        return true;
                    }
                }
                return false;
            }
            this.inputBuffer.addActiveFilter(inputFilters[1]);
            this.contentDelimitation = true;
        }
        return true;
    }
    
    protected int findBytes(final ByteChunk bc, final byte[] b) {
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
    
    protected boolean statusDropsConnection(final int status) {
        return status == 400 || status == 408 || status == 411 || status == 413 || status == 414 || status == 500 || status == 503 || status == 501;
    }
    
    static {
        Http11AprProcessor.log = LogFactory.getLog((Class)Http11AprProcessor.class);
        Http11AprProcessor.sm = StringManager.getManager("org.apache.coyote.http11");
    }
}
