// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.coyote.ajp;

import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.http.MimeHeaders;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.tomcat.util.buf.ByteChunk;
import java.net.InetAddress;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateFactory;
import java.io.ByteArrayInputStream;
import org.apache.coyote.ActionCode;
import org.apache.tomcat.util.net.SocketStatus;
import org.apache.coyote.RequestInfo;
import java.io.InterruptedIOException;
import java.io.IOException;
import org.apache.tomcat.util.net.AbstractEndpoint;
import org.apache.tomcat.util.http.HttpMessages;
import org.apache.tomcat.util.buf.HexUtils;
import org.apache.coyote.OutputBuffer;
import org.apache.coyote.InputBuffer;
import org.apache.tomcat.util.net.JIoEndpoint;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.Socket;
import org.apache.tomcat.util.net.SocketWrapper;
import org.apache.tomcat.util.buf.MessageBytes;
import org.apache.coyote.Response;
import org.apache.coyote.Request;
import org.apache.coyote.Adapter;
import org.apache.tomcat.util.res.StringManager;
import org.apache.juli.logging.Log;
import org.apache.coyote.ActionHook;

public class AjpProcessor implements ActionHook
{
    private static final Log log;
    protected static final StringManager sm;
    protected Adapter adapter;
    protected Request request;
    protected Response response;
    protected int packetSize;
    protected AjpMessage requestHeaderMessage;
    protected AjpMessage responseHeaderMessage;
    protected AjpMessage bodyMessage;
    protected MessageBytes bodyBytes;
    protected boolean started;
    protected boolean error;
    protected SocketWrapper<Socket> socket;
    protected InputStream input;
    protected OutputStream output;
    protected char[] hostNameC;
    protected JIoEndpoint endpoint;
    protected MessageBytes tmpMB;
    protected MessageBytes certificates;
    protected boolean endOfStream;
    protected boolean empty;
    protected boolean first;
    protected boolean replay;
    protected boolean finished;
    protected final byte[] getBodyMessageArray;
    protected static final byte[] pongMessageArray;
    protected static final byte[] endMessageArray;
    protected static final byte[] flushMessageArray;
    protected boolean async;
    protected boolean tomcatAuthentication;
    protected String requiredSecret;
    protected int keepAliveTimeout;
    
    public AjpProcessor(final int packetSize, final JIoEndpoint endpoint) {
        this.adapter = null;
        this.request = null;
        this.response = null;
        this.requestHeaderMessage = null;
        this.responseHeaderMessage = null;
        this.bodyMessage = null;
        this.bodyBytes = MessageBytes.newInstance();
        this.started = false;
        this.error = false;
        this.hostNameC = new char[0];
        this.tmpMB = MessageBytes.newInstance();
        this.certificates = MessageBytes.newInstance();
        this.endOfStream = false;
        this.empty = true;
        this.first = true;
        this.replay = false;
        this.finished = false;
        this.async = false;
        this.tomcatAuthentication = true;
        this.requiredSecret = null;
        this.keepAliveTimeout = -1;
        this.endpoint = endpoint;
        (this.request = new Request()).setInputBuffer(new SocketInputBuffer());
        (this.response = new Response()).setHook(this);
        this.response.setOutputBuffer(new SocketOutputBuffer());
        this.request.setResponse(this.response);
        this.packetSize = packetSize;
        this.requestHeaderMessage = new AjpMessage(packetSize);
        this.responseHeaderMessage = new AjpMessage(packetSize);
        this.bodyMessage = new AjpMessage(packetSize);
        final AjpMessage getBodyMessage = new AjpMessage(16);
        getBodyMessage.reset();
        getBodyMessage.appendByte(6);
        getBodyMessage.appendInt(8186 + packetSize - 8192);
        getBodyMessage.end();
        this.getBodyMessageArray = new byte[getBodyMessage.getLen()];
        System.arraycopy(getBodyMessage.getBuffer(), 0, this.getBodyMessageArray, 0, getBodyMessage.getLen());
        HexUtils.load();
        HttpMessages.getMessage(200);
    }
    
    public boolean getTomcatAuthentication() {
        return this.tomcatAuthentication;
    }
    
    public void setTomcatAuthentication(final boolean tomcatAuthentication) {
        this.tomcatAuthentication = tomcatAuthentication;
    }
    
    public void setRequiredSecret(final String requiredSecret) {
        this.requiredSecret = requiredSecret;
    }
    
    public int getKeepAliveTimeout() {
        return this.keepAliveTimeout;
    }
    
    public void setKeepAliveTimeout(final int timeout) {
        this.keepAliveTimeout = timeout;
    }
    
    public Request getRequest() {
        return this.request;
    }
    
    public AbstractEndpoint.Handler.SocketState process(final SocketWrapper<Socket> socket) throws IOException {
        final RequestInfo rp = this.request.getRequestProcessor();
        rp.setStage(1);
        this.socket = socket;
        this.input = socket.getSocket().getInputStream();
        this.output = socket.getSocket().getOutputStream();
        int soTimeout = -1;
        if (this.keepAliveTimeout > 0) {
            soTimeout = socket.getSocket().getSoTimeout();
        }
        this.error = false;
        while (this.started && !this.error) {
            try {
                if (this.keepAliveTimeout > 0) {
                    socket.getSocket().setSoTimeout(this.keepAliveTimeout);
                }
                if (!this.readMessage(this.requestHeaderMessage)) {
                    rp.setStage(7);
                    break;
                }
                if (this.keepAliveTimeout > 0) {
                    socket.getSocket().setSoTimeout(soTimeout);
                }
                final int type = this.requestHeaderMessage.getByte();
                if (type == 10) {
                    try {
                        this.output.write(AjpProcessor.pongMessageArray);
                    }
                    catch (IOException e) {
                        this.error = true;
                    }
                    continue;
                }
                if (type != 2) {
                    if (!AjpProcessor.log.isDebugEnabled()) {
                        continue;
                    }
                    AjpProcessor.log.debug((Object)("Unexpected message: " + type));
                    continue;
                }
                this.request.setStartTime(System.currentTimeMillis());
            }
            catch (IOException e2) {
                this.error = true;
                break;
            }
            catch (Throwable t) {
                AjpProcessor.log.debug((Object)AjpProcessor.sm.getString("ajpprocessor.header.error"), t);
                this.response.setStatus(400);
                this.adapter.log(this.request, this.response, 0L);
                this.error = true;
            }
            rp.setStage(2);
            try {
                this.prepareRequest();
            }
            catch (Throwable t) {
                AjpProcessor.log.debug((Object)AjpProcessor.sm.getString("ajpprocessor.request.prepare"), t);
                this.response.setStatus(400);
                this.adapter.log(this.request, this.response, 0L);
                this.error = true;
            }
            if (!this.error) {
                try {
                    rp.setStage(3);
                    this.adapter.service(this.request, this.response);
                }
                catch (InterruptedIOException e3) {
                    this.error = true;
                }
                catch (Throwable t) {
                    AjpProcessor.log.error((Object)AjpProcessor.sm.getString("ajpprocessor.request.process"), t);
                    this.response.setStatus(500);
                    this.adapter.log(this.request, this.response, 0L);
                    this.error = true;
                }
            }
            if (this.async && !this.error) {
                break;
            }
            if (!this.finished) {
                try {
                    this.finish();
                }
                catch (Throwable t) {
                    this.error = true;
                }
            }
            if (this.error) {
                this.response.setStatus(500);
            }
            this.request.updateCounters();
            rp.setStage(6);
            this.recycle();
        }
        if (this.async && !this.error) {
            rp.setStage(7);
            return AbstractEndpoint.Handler.SocketState.LONG;
        }
        rp.setStage(7);
        this.recycle();
        this.input = null;
        this.output = null;
        return AbstractEndpoint.Handler.SocketState.CLOSED;
    }
    
    public AbstractEndpoint.Handler.SocketState asyncDispatch(final SocketStatus status) throws IOException {
        final RequestInfo rp = this.request.getRequestProcessor();
        try {
            rp.setStage(3);
            this.error = !this.adapter.asyncDispatch(this.request, this.response, status);
        }
        catch (InterruptedIOException e) {
            this.error = true;
        }
        catch (Throwable t) {
            AjpProcessor.log.error((Object)AjpProcessor.sm.getString("http11processor.request.process"), t);
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
            this.input = null;
            this.output = null;
            return AbstractEndpoint.Handler.SocketState.CLOSED;
        }
        if (this.error) {
            this.response.setStatus(500);
            this.request.updateCounters();
            this.recycle();
            this.input = null;
            this.output = null;
            return AbstractEndpoint.Handler.SocketState.CLOSED;
        }
        return AbstractEndpoint.Handler.SocketState.LONG;
    }
    
    @Override
    public void action(final ActionCode actionCode, final Object param) {
        if (actionCode == ActionCode.ACTION_COMMIT) {
            if (this.response.isCommitted()) {
                return;
            }
            try {
                this.prepareResponse();
            }
            catch (IOException e2) {
                this.error = true;
            }
        }
        else if (actionCode == ActionCode.ACTION_CLIENT_FLUSH) {
            if (!this.response.isCommitted()) {
                try {
                    this.prepareResponse();
                }
                catch (IOException e2) {
                    this.error = true;
                    return;
                }
            }
            try {
                this.flush();
            }
            catch (IOException e2) {
                this.error = true;
            }
        }
        else if (actionCode == ActionCode.ACTION_CLOSE) {
            this.async = false;
            try {
                this.finish();
            }
            catch (IOException e2) {
                this.error = true;
            }
        }
        else if (actionCode == ActionCode.ACTION_START) {
            this.started = true;
        }
        else if (actionCode == ActionCode.ACTION_STOP) {
            this.started = false;
        }
        else if (actionCode == ActionCode.ACTION_REQ_SSL_ATTRIBUTE) {
            if (!this.certificates.isNull()) {
                final ByteChunk certData = this.certificates.getByteChunk();
                X509Certificate[] jsseCerts = null;
                final ByteArrayInputStream bais = new ByteArrayInputStream(certData.getBytes(), certData.getStart(), certData.getLength());
                try {
                    final CertificateFactory cf = CertificateFactory.getInstance("X.509");
                    while (bais.available() > 0) {
                        final X509Certificate cert = (X509Certificate)cf.generateCertificate(bais);
                        if (jsseCerts == null) {
                            jsseCerts = new X509Certificate[] { cert };
                        }
                        else {
                            final X509Certificate[] temp = new X509Certificate[jsseCerts.length + 1];
                            System.arraycopy(jsseCerts, 0, temp, 0, jsseCerts.length);
                            temp[jsseCerts.length] = cert;
                            jsseCerts = temp;
                        }
                    }
                }
                catch (CertificateException e) {
                    AjpProcessor.log.error((Object)AjpProcessor.sm.getString("ajpprocessor.certs.fail"), (Throwable)e);
                    return;
                }
                this.request.setAttribute("javax.servlet.request.X509Certificate", jsseCerts);
            }
        }
        else if (actionCode == ActionCode.ACTION_REQ_HOST_ATTRIBUTE) {
            if (this.request.remoteHost().isNull()) {
                try {
                    this.request.remoteHost().setString(InetAddress.getByName(this.request.remoteAddr().toString()).getHostName());
                }
                catch (IOException iex) {}
            }
        }
        else if (actionCode == ActionCode.ACTION_REQ_LOCAL_ADDR_ATTRIBUTE) {
            this.request.localAddr().setString(this.request.localName().toString());
        }
        else if (actionCode == ActionCode.ACTION_REQ_SET_BODY_REPLAY) {
            final ByteChunk bc = (ByteChunk)param;
            final int length = bc.getLength();
            this.bodyBytes.setBytes(bc.getBytes(), bc.getStart(), length);
            this.request.setContentLength(length);
            this.first = false;
            this.empty = false;
            this.replay = true;
        }
        else if (actionCode == ActionCode.ACTION_ASYNC_START) {
            this.async = true;
        }
        else if (actionCode == ActionCode.ACTION_ASYNC_COMPLETE) {
            final AtomicBoolean dispatch = (AtomicBoolean)param;
            final RequestInfo rp = this.request.getRequestProcessor();
            if (rp.getStage() != 3) {
                dispatch.set(true);
                this.endpoint.processSocket(this.socket, SocketStatus.STOP);
            }
            else {
                dispatch.set(false);
            }
        }
        else if (actionCode == ActionCode.ACTION_ASYNC_SETTIMEOUT) {
            if (param == null) {
                return;
            }
            final long timeout = (long)param;
            this.socket.setTimeout(timeout);
        }
        else if (actionCode == ActionCode.ACTION_ASYNC_DISPATCH) {
            final RequestInfo rp2 = this.request.getRequestProcessor();
            final AtomicBoolean dispatch2 = (AtomicBoolean)param;
            if (rp2.getStage() != 3) {
                this.endpoint.processSocket(this.socket, SocketStatus.OPEN);
                dispatch2.set(true);
            }
            else {
                dispatch2.set(true);
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
        final byte methodCode = this.requestHeaderMessage.getByte();
        if (methodCode != -1) {
            final String methodName = Constants.methodTransArray[methodCode - 1];
            this.request.method().setString(methodName);
        }
        this.requestHeaderMessage.getBytes(this.request.protocol());
        this.requestHeaderMessage.getBytes(this.request.requestURI());
        this.requestHeaderMessage.getBytes(this.request.remoteAddr());
        this.requestHeaderMessage.getBytes(this.request.remoteHost());
        this.requestHeaderMessage.getBytes(this.request.localName());
        this.request.setLocalPort(this.requestHeaderMessage.getInt());
        final boolean isSSL = this.requestHeaderMessage.getByte() != 0;
        if (isSSL) {
            this.request.scheme().setString("https");
        }
        final MimeHeaders headers = this.request.getMimeHeaders();
        for (int hCount = this.requestHeaderMessage.getInt(), i = 0; i < hCount; ++i) {
            String hName = null;
            int isc = this.requestHeaderMessage.peekInt();
            int hId = isc & 0xFF;
            MessageBytes vMB = null;
            isc &= 0xFF00;
            if (40960 == isc) {
                this.requestHeaderMessage.getInt();
                hName = Constants.headerTransArray[hId - 1];
                vMB = headers.addValue(hName);
            }
            else {
                hId = -1;
                this.requestHeaderMessage.getBytes(this.tmpMB);
                final ByteChunk bc = this.tmpMB.getByteChunk();
                vMB = headers.addValue(bc.getBuffer(), bc.getStart(), bc.getLength());
            }
            this.requestHeaderMessage.getBytes(vMB);
            if (hId == 8 || (hId == -1 && this.tmpMB.equalsIgnoreCase("Content-Length"))) {
                final long cl = vMB.getLong();
                if (cl < 2147483647L) {
                    this.request.setContentLength((int)cl);
                }
            }
            else if (hId == 7 || (hId == -1 && this.tmpMB.equalsIgnoreCase("Content-Type"))) {
                final ByteChunk bchunk = vMB.getByteChunk();
                this.request.contentType().setBytes(bchunk.getBytes(), bchunk.getOffset(), bchunk.getLength());
            }
        }
        boolean secret = false;
        byte attributeCode;
        while ((attributeCode = this.requestHeaderMessage.getByte()) != -1) {
            switch (attributeCode) {
                case 10: {
                    this.requestHeaderMessage.getBytes(this.tmpMB);
                    final String n = this.tmpMB.toString();
                    this.requestHeaderMessage.getBytes(this.tmpMB);
                    final String v = this.tmpMB.toString();
                    if (n.equals("AJP_REMOTE_PORT")) {
                        try {
                            this.request.setRemotePort(Integer.parseInt(v));
                        }
                        catch (NumberFormatException nfe) {}
                        continue;
                    }
                    this.request.setAttribute(n, v);
                    continue;
                }
                case 1: {
                    this.requestHeaderMessage.getBytes(this.tmpMB);
                    continue;
                }
                case 2: {
                    this.requestHeaderMessage.getBytes(this.tmpMB);
                    continue;
                }
                case 3: {
                    if (this.tomcatAuthentication) {
                        this.requestHeaderMessage.getBytes(this.tmpMB);
                        continue;
                    }
                    this.requestHeaderMessage.getBytes(this.request.getRemoteUser());
                    continue;
                }
                case 4: {
                    if (this.tomcatAuthentication) {
                        this.requestHeaderMessage.getBytes(this.tmpMB);
                        continue;
                    }
                    this.requestHeaderMessage.getBytes(this.request.getAuthType());
                    continue;
                }
                case 5: {
                    this.requestHeaderMessage.getBytes(this.request.queryString());
                    continue;
                }
                case 6: {
                    this.requestHeaderMessage.getBytes(this.request.instanceId());
                    continue;
                }
                case 7: {
                    this.request.scheme().setString("https");
                    this.requestHeaderMessage.getBytes(this.certificates);
                    continue;
                }
                case 8: {
                    this.request.scheme().setString("https");
                    this.requestHeaderMessage.getBytes(this.tmpMB);
                    this.request.setAttribute("javax.servlet.request.cipher_suite", this.tmpMB.toString());
                    continue;
                }
                case 9: {
                    this.request.scheme().setString("https");
                    this.requestHeaderMessage.getBytes(this.tmpMB);
                    this.request.setAttribute("javax.servlet.request.ssl_session", this.tmpMB.toString());
                    continue;
                }
                case 11: {
                    this.request.setAttribute("javax.servlet.request.key_size", new Integer(this.requestHeaderMessage.getInt()));
                    continue;
                }
                case 13: {
                    this.requestHeaderMessage.getBytes(this.request.method());
                    continue;
                }
                case 12: {
                    this.requestHeaderMessage.getBytes(this.tmpMB);
                    if (this.requiredSecret == null) {
                        continue;
                    }
                    secret = true;
                    if (!this.tmpMB.equals(this.requiredSecret)) {
                        this.response.setStatus(403);
                        this.adapter.log(this.request, this.response, 0L);
                        this.error = true;
                        continue;
                    }
                    continue;
                }
            }
        }
        if (this.requiredSecret != null && !secret) {
            this.response.setStatus(403);
            this.adapter.log(this.request, this.response, 0L);
            this.error = true;
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
        final MessageBytes valueMB = this.request.getMimeHeaders().getValue("host");
        this.parseHost(valueMB);
    }
    
    public void parseHost(final MessageBytes valueMB) {
        if (valueMB == null || (valueMB != null && valueMB.isNull())) {
            this.request.setServerPort(this.request.getLocalPort());
            try {
                this.request.serverName().duplicate(this.request.localName());
            }
            catch (IOException e) {
                this.response.setStatus(400);
                this.adapter.log(this.request, this.response, 0L);
                this.error = true;
            }
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
            if (this.request.scheme().equalsIgnoreCase("https")) {
                this.request.setServerPort(443);
            }
            else {
                this.request.setServerPort(80);
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
    
    protected void prepareResponse() throws IOException {
        this.response.setCommitted(true);
        this.responseHeaderMessage.reset();
        this.responseHeaderMessage.appendByte(4);
        this.responseHeaderMessage.appendInt(this.response.getStatus());
        String message = null;
        if (org.apache.coyote.Constants.USE_CUSTOM_STATUS_MSG_IN_HEADER && HttpMessages.isSafeInHttpHeader(this.response.getMessage())) {
            message = this.response.getMessage();
        }
        if (message == null) {
            message = HttpMessages.getMessage(this.response.getStatus());
        }
        if (message == null) {
            message = Integer.toString(this.response.getStatus());
        }
        this.tmpMB.setString(message);
        this.responseHeaderMessage.appendBytes(this.tmpMB);
        final MimeHeaders headers = this.response.getMimeHeaders();
        final String contentType = this.response.getContentType();
        if (contentType != null) {
            headers.setValue("Content-Type").setString(contentType);
        }
        final String contentLanguage = this.response.getContentLanguage();
        if (contentLanguage != null) {
            headers.setValue("Content-Language").setString(contentLanguage);
        }
        final long contentLength = this.response.getContentLengthLong();
        if (contentLength >= 0L) {
            headers.setValue("Content-Length").setLong(contentLength);
        }
        final int numHeaders = headers.size();
        this.responseHeaderMessage.appendInt(numHeaders);
        for (int i = 0; i < numHeaders; ++i) {
            final MessageBytes hN = headers.getName(i);
            final int hC = Constants.getResponseAjpIndex(hN.toString());
            if (hC > 0) {
                this.responseHeaderMessage.appendInt(hC);
            }
            else {
                this.responseHeaderMessage.appendBytes(hN);
            }
            final MessageBytes hV = headers.getValue(i);
            this.responseHeaderMessage.appendBytes(hV);
        }
        this.responseHeaderMessage.end();
        this.output.write(this.responseHeaderMessage.getBuffer(), 0, this.responseHeaderMessage.getLen());
    }
    
    protected void finish() throws IOException {
        if (!this.response.isCommitted()) {
            try {
                this.prepareResponse();
            }
            catch (IOException e) {
                this.error = true;
            }
        }
        if (this.finished) {
            return;
        }
        this.finished = true;
        this.output.write(AjpProcessor.endMessageArray);
    }
    
    protected boolean read(final byte[] buf, final int pos, final int n) throws IOException {
        for (int read = 0, res = 0; read < n; read += res) {
            res = this.input.read(buf, read + pos, n - read);
            if (res <= 0) {
                throw new IOException(AjpProcessor.sm.getString("ajpprotocol.failedread"));
            }
        }
        return true;
    }
    
    public boolean receive() throws IOException {
        this.first = false;
        this.bodyMessage.reset();
        if (!this.readMessage(this.bodyMessage)) {
            return false;
        }
        if (this.bodyMessage.getLen() == 0) {
            return false;
        }
        final int blen = this.bodyMessage.peekInt();
        if (blen == 0) {
            return false;
        }
        this.bodyMessage.getBytes(this.bodyBytes);
        this.empty = false;
        return true;
    }
    
    private boolean refillReadBuffer() throws IOException {
        if (this.replay) {
            this.endOfStream = true;
        }
        if (this.endOfStream) {
            return false;
        }
        this.output.write(this.getBodyMessageArray);
        final boolean moreData = this.receive();
        if (!moreData) {
            this.endOfStream = true;
        }
        return moreData;
    }
    
    protected boolean readMessage(final AjpMessage message) throws IOException {
        final byte[] buf = message.getBuffer();
        final int headerLength = message.getHeaderLength();
        this.read(buf, 0, headerLength);
        final int messageLength = message.processHeader();
        if (messageLength < 0) {
            return false;
        }
        if (messageLength == 0) {
            return true;
        }
        this.read(buf, headerLength, messageLength);
        return true;
    }
    
    public void recycle() {
        this.first = true;
        this.endOfStream = false;
        this.empty = true;
        this.replay = false;
        this.finished = false;
        this.request.recycle();
        this.response.recycle();
        this.certificates.recycle();
        this.async = false;
    }
    
    protected void flush() throws IOException {
        this.output.write(AjpProcessor.flushMessageArray);
    }
    
    static {
        log = LogFactory.getLog((Class)AjpProcessor.class);
        sm = StringManager.getManager("org.apache.coyote.ajp");
        final AjpMessage pongMessage = new AjpMessage(16);
        pongMessage.reset();
        pongMessage.appendByte(9);
        pongMessage.end();
        pongMessageArray = new byte[pongMessage.getLen()];
        System.arraycopy(pongMessage.getBuffer(), 0, AjpProcessor.pongMessageArray, 0, pongMessage.getLen());
        final AjpMessage endMessage = new AjpMessage(16);
        endMessage.reset();
        endMessage.appendByte(5);
        endMessage.appendByte(1);
        endMessage.end();
        endMessageArray = new byte[endMessage.getLen()];
        System.arraycopy(endMessage.getBuffer(), 0, AjpProcessor.endMessageArray, 0, endMessage.getLen());
        final AjpMessage flushMessage = new AjpMessage(16);
        flushMessage.reset();
        flushMessage.appendByte(3);
        flushMessage.appendInt(0);
        flushMessage.appendByte(0);
        flushMessage.end();
        flushMessageArray = new byte[flushMessage.getLen()];
        System.arraycopy(flushMessage.getBuffer(), 0, AjpProcessor.flushMessageArray, 0, flushMessage.getLen());
    }
    
    protected class SocketInputBuffer implements InputBuffer
    {
        @Override
        public int doRead(final ByteChunk chunk, final Request req) throws IOException {
            if (AjpProcessor.this.endOfStream) {
                return -1;
            }
            if (AjpProcessor.this.first && req.getContentLengthLong() > 0L) {
                if (!AjpProcessor.this.receive()) {
                    return 0;
                }
            }
            else if (AjpProcessor.this.empty && !AjpProcessor.this.refillReadBuffer()) {
                return -1;
            }
            final ByteChunk bc = AjpProcessor.this.bodyBytes.getByteChunk();
            chunk.setBytes(bc.getBuffer(), bc.getStart(), bc.getLength());
            AjpProcessor.this.empty = true;
            return chunk.getLength();
        }
    }
    
    protected class SocketOutputBuffer implements OutputBuffer
    {
        @Override
        public int doWrite(final ByteChunk chunk, final Response res) throws IOException {
            if (!AjpProcessor.this.response.isCommitted()) {
                try {
                    AjpProcessor.this.prepareResponse();
                }
                catch (IOException e) {
                    AjpProcessor.this.error = true;
                }
            }
            int len = chunk.getLength();
            final int chunkSize = 8184 + AjpProcessor.this.packetSize - 8192;
            int off = 0;
            while (len > 0) {
                int thisTime = len;
                if (thisTime > chunkSize) {
                    thisTime = chunkSize;
                }
                len -= thisTime;
                AjpProcessor.this.responseHeaderMessage.reset();
                AjpProcessor.this.responseHeaderMessage.appendByte(3);
                AjpProcessor.this.responseHeaderMessage.appendBytes(chunk.getBytes(), chunk.getOffset() + off, thisTime);
                AjpProcessor.this.responseHeaderMessage.end();
                AjpProcessor.this.output.write(AjpProcessor.this.responseHeaderMessage.getBuffer(), 0, AjpProcessor.this.responseHeaderMessage.getLen());
                off += thisTime;
            }
            return chunk.getLength();
        }
    }
}
