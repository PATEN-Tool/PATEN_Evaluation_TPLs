// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.coyote.ajp;

import org.apache.coyote.InputBuffer;
import org.apache.tomcat.util.http.HttpMessages;
import org.apache.tomcat.util.buf.HexUtils;
import org.apache.tomcat.util.http.MimeHeaders;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.coyote.AsyncContextCallback;
import org.apache.tomcat.util.buf.ByteChunk;
import java.net.InetAddress;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.apache.coyote.ActionCode;
import org.apache.juli.logging.Log;
import org.apache.coyote.AsyncStateMachine;
import org.apache.tomcat.util.buf.MessageBytes;
import org.apache.coyote.Response;
import org.apache.coyote.Request;
import org.apache.tomcat.util.net.AbstractEndpoint;
import org.apache.coyote.Adapter;
import org.apache.tomcat.util.res.StringManager;
import org.apache.coyote.Processor;
import org.apache.coyote.ActionHook;

public abstract class AbstractAjpProcessor implements ActionHook, Processor
{
    protected static final StringManager sm;
    protected Adapter adapter;
    protected AbstractEndpoint endpoint;
    protected Request request;
    protected Response response;
    protected int packetSize;
    protected AjpMessage requestHeaderMessage;
    protected AjpMessage responseHeaderMessage;
    protected AjpMessage bodyMessage;
    protected MessageBytes bodyBytes;
    protected boolean error;
    protected char[] hostNameC;
    protected MessageBytes tmpMB;
    protected MessageBytes certificates;
    protected boolean endOfStream;
    protected boolean empty;
    protected boolean first;
    protected boolean replay;
    protected boolean finished;
    protected AsyncStateMachine asyncStateMachine;
    protected long byteCount;
    protected boolean tomcatAuthentication;
    protected String requiredSecret;
    protected String clientCertProvider;
    
    public AbstractAjpProcessor() {
        this.adapter = null;
        this.request = null;
        this.response = null;
        this.requestHeaderMessage = null;
        this.responseHeaderMessage = null;
        this.bodyMessage = null;
        this.bodyBytes = MessageBytes.newInstance();
        this.error = false;
        this.hostNameC = new char[0];
        this.tmpMB = MessageBytes.newInstance();
        this.certificates = MessageBytes.newInstance();
        this.endOfStream = false;
        this.empty = true;
        this.first = true;
        this.replay = false;
        this.finished = false;
        this.asyncStateMachine = new AsyncStateMachine(this);
        this.byteCount = 0L;
        this.tomcatAuthentication = true;
        this.requiredSecret = null;
        this.clientCertProvider = null;
    }
    
    protected abstract Log getLog();
    
    public boolean getTomcatAuthentication() {
        return this.tomcatAuthentication;
    }
    
    public void setTomcatAuthentication(final boolean tomcatAuthentication) {
        this.tomcatAuthentication = tomcatAuthentication;
    }
    
    public void setRequiredSecret(final String requiredSecret) {
        this.requiredSecret = requiredSecret;
    }
    
    public String getClientCertProvider() {
        return this.clientCertProvider;
    }
    
    public void setClientCertProvider(final String s) {
        this.clientCertProvider = s;
    }
    
    public Request getRequest() {
        return this.request;
    }
    
    @Override
    public final void action(final ActionCode actionCode, final Object param) {
        if (actionCode == ActionCode.COMMIT) {
            if (this.response.isCommitted()) {
                return;
            }
            try {
                this.prepareResponse();
            }
            catch (IOException e3) {
                this.error = true;
            }
            try {
                this.flush(false);
            }
            catch (IOException e3) {
                this.error = true;
            }
        }
        else if (actionCode == ActionCode.CLIENT_FLUSH) {
            if (!this.response.isCommitted()) {
                try {
                    this.prepareResponse();
                }
                catch (IOException e3) {
                    this.error = true;
                    return;
                }
            }
            try {
                this.flush(true);
            }
            catch (IOException e3) {
                this.error = true;
            }
        }
        else if (actionCode == ActionCode.DISABLE_SWALLOW_INPUT) {
            this.error = true;
        }
        else if (actionCode == ActionCode.CLOSE) {
            try {
                this.finish();
            }
            catch (IOException e3) {
                this.error = true;
            }
        }
        else if (actionCode == ActionCode.REQ_SSL_ATTRIBUTE) {
            if (!this.certificates.isNull()) {
                final ByteChunk certData = this.certificates.getByteChunk();
                X509Certificate[] jsseCerts = null;
                final ByteArrayInputStream bais = new ByteArrayInputStream(certData.getBytes(), certData.getStart(), certData.getLength());
                try {
                    CertificateFactory cf;
                    if (this.clientCertProvider == null) {
                        cf = CertificateFactory.getInstance("X.509");
                    }
                    else {
                        cf = CertificateFactory.getInstance("X.509", this.clientCertProvider);
                    }
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
                    this.getLog().error((Object)AbstractAjpProcessor.sm.getString("ajpprocessor.certs.fail"), (Throwable)e);
                    return;
                }
                catch (NoSuchProviderException e2) {
                    this.getLog().error((Object)AbstractAjpProcessor.sm.getString("ajpprocessor.certs.fail"), (Throwable)e2);
                    return;
                }
                this.request.setAttribute("javax.servlet.request.X509Certificate", jsseCerts);
            }
        }
        else if (actionCode == ActionCode.REQ_HOST_ATTRIBUTE) {
            if (this.request.remoteHost().isNull()) {
                try {
                    this.request.remoteHost().setString(InetAddress.getByName(this.request.remoteAddr().toString()).getHostName());
                }
                catch (IOException iex) {}
            }
        }
        else if (actionCode == ActionCode.REQ_LOCAL_ADDR_ATTRIBUTE) {
            this.request.localAddr().setString(this.request.localName().toString());
        }
        else if (actionCode == ActionCode.REQ_SET_BODY_REPLAY) {
            final ByteChunk bc = (ByteChunk)param;
            final int length = bc.getLength();
            this.bodyBytes.setBytes(bc.getBytes(), bc.getStart(), length);
            this.request.setContentLength(length);
            this.first = false;
            this.empty = false;
            this.replay = true;
        }
        else if (actionCode == ActionCode.ASYNC_START) {
            this.asyncStateMachine.asyncStart((AsyncContextCallback)param);
        }
        else if (actionCode == ActionCode.ASYNC_DISPATCHED) {
            this.asyncStateMachine.asyncDispatched();
        }
        else if (actionCode == ActionCode.ASYNC_TIMEOUT) {
            final AtomicBoolean result = (AtomicBoolean)param;
            result.set(this.asyncStateMachine.asyncTimeout());
        }
        else if (actionCode == ActionCode.ASYNC_RUN) {
            this.asyncStateMachine.asyncRun((Runnable)param);
        }
        else if (actionCode == ActionCode.ASYNC_ERROR) {
            this.asyncStateMachine.asyncError();
        }
        else if (actionCode == ActionCode.ASYNC_IS_STARTED) {
            ((AtomicBoolean)param).set(this.asyncStateMachine.isAsyncStarted());
        }
        else if (actionCode == ActionCode.ASYNC_IS_DISPATCHING) {
            ((AtomicBoolean)param).set(this.asyncStateMachine.isAsyncDispatching());
        }
        else if (actionCode == ActionCode.ASYNC_IS_ASYNC) {
            ((AtomicBoolean)param).set(this.asyncStateMachine.isAsync());
        }
        else if (actionCode == ActionCode.ASYNC_IS_TIMINGOUT) {
            ((AtomicBoolean)param).set(this.asyncStateMachine.isAsyncTimingOut());
        }
        else {
            this.actionInternal(actionCode, param);
        }
    }
    
    protected abstract void actionInternal(final ActionCode p0, final Object p1);
    
    protected abstract void flush(final boolean p0) throws IOException;
    
    protected abstract void finish() throws IOException;
    
    @Override
    public abstract Executor getExecutor();
    
    public void recycle() {
        this.asyncStateMachine.recycle();
        this.first = true;
        this.endOfStream = false;
        this.empty = true;
        this.replay = false;
        this.finished = false;
        this.request.recycle();
        this.response.recycle();
        this.certificates.recycle();
        this.byteCount = 0L;
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
            final String methodName = Constants.getMethodForCode(methodCode - 1);
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
                hName = Constants.getHeaderForCode(hId - 1);
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
                    this.request.setAttribute("javax.servlet.request.key_size", this.requestHeaderMessage.getInt());
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
    
    protected void parseHost(final MessageBytes valueMB) {
        if (valueMB == null || valueMB.isNull()) {
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
        this.output(this.responseHeaderMessage.getBuffer(), 0, this.responseHeaderMessage.getLen());
    }
    
    protected abstract void output(final byte[] p0, final int p1, final int p2) throws IOException;
    
    protected boolean isAsync() {
        return this.asyncStateMachine.isAsync();
    }
    
    protected AbstractEndpoint.Handler.SocketState asyncPostProcess() {
        return this.asyncStateMachine.asyncPostProcess();
    }
    
    protected abstract boolean receive() throws IOException;
    
    protected abstract boolean refillReadBuffer() throws IOException;
    
    static {
        sm = StringManager.getManager("org.apache.coyote.ajp");
    }
    
    protected class SocketInputBuffer implements InputBuffer
    {
        @Override
        public int doRead(final ByteChunk chunk, final Request req) throws IOException {
            if (AbstractAjpProcessor.this.endOfStream) {
                return -1;
            }
            if (AbstractAjpProcessor.this.first && req.getContentLengthLong() > 0L) {
                if (!AbstractAjpProcessor.this.receive()) {
                    return 0;
                }
            }
            else if (AbstractAjpProcessor.this.empty && !AbstractAjpProcessor.this.refillReadBuffer()) {
                return -1;
            }
            final ByteChunk bc = AbstractAjpProcessor.this.bodyBytes.getByteChunk();
            chunk.setBytes(bc.getBuffer(), bc.getStart(), bc.getLength());
            AbstractAjpProcessor.this.empty = true;
            return chunk.getLength();
        }
    }
}
