// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.coyote.http11;

import org.apache.juli.logging.LogFactory;
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
import java.util.Locale;
import org.apache.tomcat.jni.Sockaddr;
import java.util.concurrent.atomic.AtomicBoolean;
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
import org.apache.tomcat.util.net.AbstractEndpoint;
import org.apache.tomcat.util.net.SocketStatus;
import org.apache.tomcat.util.buf.HexUtils;
import org.apache.coyote.OutputBuffer;
import org.apache.coyote.Response;
import org.apache.coyote.InputBuffer;
import org.apache.coyote.Request;
import org.apache.tomcat.util.net.AprEndpoint;
import org.apache.juli.logging.Log;
import org.apache.coyote.ActionHook;

public class Http11AprProcessor extends AbstractHttp11Processor implements ActionHook
{
    private static final Log log;
    protected InternalAprInputBuffer inputBuffer;
    protected InternalAprOutputBuffer outputBuffer;
    protected AprEndpoint.SendfileData sendfileData;
    protected boolean comet;
    protected boolean async;
    protected boolean ssl;
    protected long socket;
    protected AprEndpoint endpoint;
    
    public Http11AprProcessor(final int headerBufferSize, final AprEndpoint endpoint) {
        this.inputBuffer = null;
        this.outputBuffer = null;
        this.sendfileData = null;
        this.comet = false;
        this.async = false;
        this.ssl = false;
        this.socket = 0L;
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
        HexUtils.load();
    }
    
    protected void addFilter(final String className) {
        try {
            final Class<?> clazz = Class.forName(className);
            final Object obj = clazz.newInstance();
            if (obj instanceof InputFilter) {
                this.inputBuffer.addFilter((InputFilter)obj);
            }
            else if (obj instanceof OutputFilter) {
                this.outputBuffer.addFilter((OutputFilter)obj);
            }
            else {
                Http11AprProcessor.log.warn((Object)Http11AprProcessor.sm.getString("http11processor.filter.unknown", new Object[] { className }));
            }
        }
        catch (Exception e) {
            Http11AprProcessor.log.error((Object)Http11AprProcessor.sm.getString("http11processor.filter.error", new Object[] { className }), (Throwable)e);
        }
    }
    
    public AbstractEndpoint.Handler.SocketState event(final SocketStatus status) throws IOException {
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
            return AbstractEndpoint.Handler.SocketState.CLOSED;
        }
        if (!this.comet) {
            this.inputBuffer.nextRequest();
            this.outputBuffer.nextRequest();
            this.recycle();
            return AbstractEndpoint.Handler.SocketState.OPEN;
        }
        return AbstractEndpoint.Handler.SocketState.LONG;
    }
    
    public AbstractEndpoint.Handler.SocketState process(final long socket) throws IOException {
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
        this.async = false;
        this.keepAlive = true;
        int keepAliveLeft = this.maxKeepAliveRequests;
        final long soTimeout = this.endpoint.getSoTimeout();
        boolean keptAlive = false;
        boolean openSocket = false;
        while (!this.error && this.keepAlive && !this.comet && !this.async) {
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
            if (!this.comet && !this.async) {
                if (this.error) {
                    this.inputBuffer.setSwallowInput(false);
                }
                this.endRequest();
            }
            if (this.error) {
                this.response.setStatus(500);
            }
            this.request.updateCounters();
            if (!this.comet && !this.async) {
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
        if (!this.comet && !this.async) {
            this.recycle();
            return openSocket ? AbstractEndpoint.Handler.SocketState.OPEN : AbstractEndpoint.Handler.SocketState.CLOSED;
        }
        if (this.error) {
            this.inputBuffer.nextRequest();
            this.outputBuffer.nextRequest();
            this.recycle();
            return AbstractEndpoint.Handler.SocketState.CLOSED;
        }
        return AbstractEndpoint.Handler.SocketState.LONG;
    }
    
    public AbstractEndpoint.Handler.SocketState asyncDispatch(final long socket, final SocketStatus status) throws IOException {
        this.socket = socket;
        this.inputBuffer.setSocket(socket);
        this.outputBuffer.setSocket(socket);
        final RequestInfo rp = this.request.getRequestProcessor();
        try {
            rp.setStage(3);
            this.error = !this.adapter.asyncDispatch(this.request, this.response, status);
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
        if (!this.async) {
            if (this.error) {
                this.response.setStatus(500);
            }
            this.request.updateCounters();
            this.recycle();
            return AbstractEndpoint.Handler.SocketState.CLOSED;
        }
        if (this.error) {
            this.response.setStatus(500);
            this.request.updateCounters();
            this.recycle();
            return AbstractEndpoint.Handler.SocketState.CLOSED;
        }
        return AbstractEndpoint.Handler.SocketState.LONG;
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
    
    @Override
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
            this.async = false;
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
                        this.request.setAttribute("javax.servlet.request.key_size", sslO);
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
                if (actionCode != ActionCode.ACTION_COMET_SETTIMEOUT) {
                    if (actionCode == ActionCode.ACTION_ASYNC_START) {
                        this.async = true;
                    }
                    else if (actionCode == ActionCode.ACTION_ASYNC_COMPLETE) {
                        final AtomicBoolean dispatch = (AtomicBoolean)param;
                        final RequestInfo rp = this.request.getRequestProcessor();
                        if (rp.getStage() != 3) {
                            dispatch.set(true);
                            this.endpoint.getHandler().asyncDispatch(this.socket, SocketStatus.STOP);
                        }
                        else {
                            dispatch.set(false);
                        }
                    }
                    else if (actionCode == ActionCode.ACTION_ASYNC_SETTIMEOUT) {
                        if (param == null) {
                            return;
                        }
                        if (this.socket == 0L) {
                            return;
                        }
                        final long timeout = (long)param;
                        Socket.timeoutSet(this.socket, timeout * 1000L);
                    }
                    else if (actionCode == ActionCode.ACTION_ASYNC_DISPATCH) {
                        final RequestInfo rp2 = this.request.getRequestProcessor();
                        final AtomicBoolean dispatch2 = (AtomicBoolean)param;
                        if (rp2.getStage() != 3) {
                            this.endpoint.getPoller().add(this.socket);
                            dispatch2.set(true);
                        }
                        else {
                            dispatch2.set(true);
                        }
                    }
                }
            }
        }
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
                encodingName = transferEncodingValue.substring(startPos, commaPos).toLowerCase(Locale.ENGLISH).trim();
                if (!this.addInputFilter(inputFilters, encodingName)) {
                    this.error = true;
                    this.response.setStatus(501);
                    this.adapter.log(this.request, this.response, 0L);
                }
                startPos = commaPos + 1;
                commaPos = transferEncodingValue.indexOf(44, startPos);
            }
            encodingName = transferEncodingValue.substring(startPos).toLowerCase(Locale.ENGLISH).trim();
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
                final int charValue = HexUtils.getDec(valueB[j + valueS]);
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
        boolean useCompression = false;
        if (entityBody && this.compressionLevel > 0 && this.sendfileData == null) {
            useCompression = this.isCompressable();
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
    }
    
    protected boolean addInputFilter(final InputFilter[] inputFilters, final String encodingName) {
        if (!encodingName.equals("identity")) {
            if (!encodingName.equals("chunked")) {
                for (int i = 2; i < inputFilters.length; ++i) {
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
    
    static {
        log = LogFactory.getLog((Class)Http11AprProcessor.class);
    }
}
