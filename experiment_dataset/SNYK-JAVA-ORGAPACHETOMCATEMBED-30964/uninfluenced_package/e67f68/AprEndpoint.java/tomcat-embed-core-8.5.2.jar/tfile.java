// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.tomcat.util.net;

import org.apache.tomcat.util.buf.ByteBufferUtils;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.Lock;
import java.io.EOFException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import org.apache.tomcat.jni.File;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.juli.logging.LogFactory;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import org.apache.tomcat.jni.Status;
import org.apache.tomcat.jni.Poll;
import org.apache.tomcat.util.ExceptionUtils;
import java.nio.charset.StandardCharsets;
import org.apache.tomcat.jni.SSL;
import org.apache.tomcat.jni.SSLSocket;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.tomcat.util.net.openssl.OpenSSLEngine;
import org.apache.tomcat.jni.Error;
import org.apache.tomcat.jni.Socket;
import org.apache.tomcat.jni.OS;
import org.apache.tomcat.jni.Library;
import org.apache.tomcat.jni.Pool;
import org.apache.tomcat.jni.Sockaddr;
import org.apache.tomcat.jni.Address;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import org.apache.juli.logging.Log;
import org.apache.tomcat.jni.SSLContext;

public class AprEndpoint extends AbstractEndpoint<Long> implements SSLContext.SNICallBack
{
    private static final Log log;
    protected long rootPool;
    protected long serverSock;
    protected long serverSockPool;
    protected long sslContext;
    private final Map<Long, AprSocketWrapper> connections;
    protected boolean deferAccept;
    protected int sendfileSize;
    protected int pollTime;
    private boolean useSendFileSet;
    protected Poller poller;
    protected Sendfile sendfile;
    
    public AprEndpoint() {
        this.rootPool = 0L;
        this.serverSock = 0L;
        this.serverSockPool = 0L;
        this.sslContext = 0L;
        this.connections = new ConcurrentHashMap<Long, AprSocketWrapper>();
        this.deferAccept = true;
        this.sendfileSize = 1024;
        this.pollTime = 2000;
        this.useSendFileSet = false;
        this.poller = null;
        this.sendfile = null;
        this.setMaxConnections(8192);
    }
    
    public void setDeferAccept(final boolean deferAccept) {
        this.deferAccept = deferAccept;
    }
    
    public boolean getDeferAccept() {
        return this.deferAccept;
    }
    
    public void setSendfileSize(final int sendfileSize) {
        this.sendfileSize = sendfileSize;
    }
    
    public int getSendfileSize() {
        return this.sendfileSize;
    }
    
    public int getPollTime() {
        return this.pollTime;
    }
    
    public void setPollTime(final int pollTime) {
        if (pollTime > 0) {
            this.pollTime = pollTime;
        }
    }
    
    @Override
    public void setUseSendfile(final boolean useSendfile) {
        this.useSendFileSet = true;
        super.setUseSendfile(useSendfile);
    }
    
    private void setUseSendfileInternal(final boolean useSendfile) {
        super.setUseSendfile(useSendfile);
    }
    
    public Poller getPoller() {
        return this.poller;
    }
    
    public Sendfile getSendfile() {
        return this.sendfile;
    }
    
    @Override
    protected SSLHostConfig.Type getSslConfigType() {
        return SSLHostConfig.Type.OPENSSL;
    }
    
    @Override
    public int getLocalPort() {
        final long s = this.serverSock;
        if (s == 0L) {
            return -1;
        }
        try {
            final long sa = Address.get(0, s);
            final Sockaddr addr = Address.getInfo(sa);
            return addr.port;
        }
        catch (Exception e) {
            return -1;
        }
    }
    
    @Override
    public void setMaxConnections(final int maxConnections) {
        if (maxConnections == -1) {
            AprEndpoint.log.warn((Object)AprEndpoint.sm.getString("endpoint.apr.maxConnections.unlimited", this.getMaxConnections()));
            return;
        }
        if (this.running) {
            AprEndpoint.log.warn((Object)AprEndpoint.sm.getString("endpoint.apr.maxConnections.running", this.getMaxConnections()));
            return;
        }
        super.setMaxConnections(maxConnections);
    }
    
    public int getKeepAliveCount() {
        if (this.poller == null) {
            return 0;
        }
        return this.poller.getConnectionCount();
    }
    
    public int getSendfileCount() {
        if (this.sendfile == null) {
            return 0;
        }
        return this.sendfile.getSendfileCount();
    }
    
    @Override
    public void bind() throws Exception {
        try {
            this.rootPool = Pool.create(0L);
        }
        catch (UnsatisfiedLinkError e2) {
            throw new Exception(AprEndpoint.sm.getString("endpoint.init.notavail"));
        }
        this.serverSockPool = Pool.create(this.rootPool);
        String addressStr = null;
        if (this.getAddress() != null) {
            addressStr = this.getAddress().getHostAddress();
        }
        int family = 1;
        if (Library.APR_HAVE_IPV6) {
            if (addressStr == null) {
                if (!OS.IS_BSD && !OS.IS_WIN32 && !OS.IS_WIN64) {
                    family = 0;
                }
            }
            else if (addressStr.indexOf(58) >= 0) {
                family = 0;
            }
        }
        final long inetAddress = Address.info(addressStr, family, this.getPort(), 0, this.rootPool);
        this.serverSock = Socket.create(Address.getInfo(inetAddress).family, 0, 6, this.rootPool);
        if (OS.IS_UNIX) {
            Socket.optSet(this.serverSock, 16, 1);
        }
        Socket.optSet(this.serverSock, 2, 1);
        int ret = Socket.bind(this.serverSock, inetAddress);
        if (ret != 0) {
            throw new Exception(AprEndpoint.sm.getString("endpoint.init.bind", "" + ret, Error.strerror(ret)));
        }
        ret = Socket.listen(this.serverSock, this.getBacklog());
        if (ret != 0) {
            throw new Exception(AprEndpoint.sm.getString("endpoint.init.listen", "" + ret, Error.strerror(ret)));
        }
        if (OS.IS_WIN32 || OS.IS_WIN64) {
            Socket.optSet(this.serverSock, 16, 1);
        }
        if (!this.useSendFileSet) {
            this.setUseSendfileInternal(Library.APR_HAS_SENDFILE);
        }
        else if (this.getUseSendfile() && !Library.APR_HAS_SENDFILE) {
            this.setUseSendfileInternal(false);
        }
        if (this.acceptorThreadCount == 0) {
            this.acceptorThreadCount = 1;
        }
        if (this.deferAccept && Socket.optSet(this.serverSock, 32768, 1) == 70023) {
            this.deferAccept = false;
        }
        if (this.isSSLEnabled()) {
            for (final SSLHostConfig sslHostConfig : this.sslHostConfigs.values()) {
                final Set<SSLHostConfigCertificate> certificates = sslHostConfig.getCertificates(true);
                boolean firstCertificate = true;
                for (final SSLHostConfigCertificate certificate : certificates) {
                    if (SSLHostConfig.adjustRelativePath(certificate.getCertificateFile()) == null) {
                        throw new Exception(AprEndpoint.sm.getString("endpoint.apr.noSslCertFile"));
                    }
                    if (!firstCertificate) {
                        continue;
                    }
                    firstCertificate = false;
                    final List<String> enabledProtocols = SSLUtilBase.getEnabled("protocols", AprEndpoint.log, true, sslHostConfig.getProtocols(), OpenSSLEngine.IMPLEMENTED_PROTOCOLS_SET);
                    sslHostConfig.setEnabledProtocols(enabledProtocols.toArray(new String[enabledProtocols.size()]));
                    final List<String> enabledCiphers = SSLUtilBase.getEnabled("ciphers", AprEndpoint.log, false, sslHostConfig.getJsseCipherNames(), OpenSSLEngine.AVAILABLE_CIPHER_SUITES);
                    sslHostConfig.setEnabledCiphers(enabledCiphers.toArray(new String[enabledCiphers.size()]));
                }
                if (certificates.size() > 2) {
                    throw new Exception(AprEndpoint.sm.getString("endpoint.apr.tooManyCertFiles"));
                }
                int value = 0;
                if (sslHostConfig.getProtocols().size() == 0) {
                    value = 28;
                }
                else {
                    for (final String protocol : sslHostConfig.getEnabledProtocols()) {
                        if (!"SSLv2Hello".equalsIgnoreCase(protocol)) {
                            if ("SSLv2".equalsIgnoreCase(protocol)) {
                                value |= 0x1;
                            }
                            else if ("SSLv3".equalsIgnoreCase(protocol)) {
                                value |= 0x2;
                            }
                            else if ("TLSv1".equalsIgnoreCase(protocol)) {
                                value |= 0x4;
                            }
                            else if ("TLSv1.1".equalsIgnoreCase(protocol)) {
                                value |= 0x8;
                            }
                            else {
                                if (!"TLSv1.2".equalsIgnoreCase(protocol)) {
                                    throw new Exception(AprEndpoint.sm.getString("endpoint.apr.invalidSslProtocol", protocol));
                                }
                                value |= 0x10;
                            }
                        }
                    }
                }
                long ctx = 0L;
                try {
                    ctx = SSLContext.make(this.rootPool, value, 1);
                }
                catch (Exception e) {
                    throw new Exception(AprEndpoint.sm.getString("endpoint.apr.failSslContextMake"), e);
                }
                if (sslHostConfig.getInsecureRenegotiation()) {
                    SSLContext.setOptions(ctx, 262144);
                }
                else {
                    SSLContext.clearOptions(ctx, 262144);
                }
                final String honorCipherOrderStr = sslHostConfig.getHonorCipherOrder();
                if (honorCipherOrderStr != null) {
                    final boolean honorCipherOrder = Boolean.valueOf(honorCipherOrderStr);
                    if (honorCipherOrder) {
                        SSLContext.setOptions(ctx, 4194304);
                    }
                    else {
                        SSLContext.clearOptions(ctx, 4194304);
                    }
                }
                if (sslHostConfig.getDisableCompression()) {
                    SSLContext.setOptions(ctx, 131072);
                }
                else {
                    SSLContext.clearOptions(ctx, 131072);
                }
                if (sslHostConfig.getDisableSessionTickets()) {
                    SSLContext.setOptions(ctx, 16384);
                }
                else {
                    SSLContext.clearOptions(ctx, 16384);
                }
                SSLContext.setCipherSuite(ctx, sslHostConfig.getCiphers());
                int idx = 0;
                for (final SSLHostConfigCertificate certificate2 : sslHostConfig.getCertificates(true)) {
                    SSLContext.setCertificate(ctx, SSLHostConfig.adjustRelativePath(certificate2.getCertificateFile()), SSLHostConfig.adjustRelativePath(certificate2.getCertificateKeyFile()), certificate2.getCertificateKeyPassword(), idx++);
                    SSLContext.setCertificateChainFile(ctx, SSLHostConfig.adjustRelativePath(certificate2.getCertificateChainFile()), false);
                }
                SSLContext.setCACertificate(ctx, SSLHostConfig.adjustRelativePath(sslHostConfig.getCaCertificateFile()), SSLHostConfig.adjustRelativePath(sslHostConfig.getCaCertificatePath()));
                SSLContext.setCARevocation(ctx, SSLHostConfig.adjustRelativePath(sslHostConfig.getCertificateRevocationListFile()), SSLHostConfig.adjustRelativePath(sslHostConfig.getCertificateRevocationListPath()));
                switch (sslHostConfig.getCertificateVerification()) {
                    case NONE: {
                        value = 0;
                        break;
                    }
                    case OPTIONAL: {
                        value = 1;
                        break;
                    }
                    case OPTIONAL_NO_CA: {
                        value = 3;
                        break;
                    }
                    case REQUIRED: {
                        value = 2;
                        break;
                    }
                }
                SSLContext.setVerify(ctx, value, sslHostConfig.getCertificateVerificationDepth());
                if (this.getUseSendfile()) {
                    this.setUseSendfileInternal(false);
                    if (this.useSendFileSet) {
                        AprEndpoint.log.warn((Object)AprEndpoint.sm.getString("endpoint.apr.noSendfileWithSSL"));
                    }
                }
                if (this.negotiableProtocols.size() > 0) {
                    final ArrayList<String> protocols = new ArrayList<String>();
                    protocols.addAll(this.negotiableProtocols);
                    protocols.add("http/1.1");
                    final String[] protocolsArray = protocols.toArray(new String[0]);
                    SSLContext.setAlpnProtos(ctx, protocolsArray, 0);
                }
                sslHostConfig.setOpenSslContext(ctx);
            }
            final SSLHostConfig defaultSSLHostConfig = this.sslHostConfigs.get(this.getDefaultSSLHostConfigName());
            final Long defaultSSLContext = defaultSSLHostConfig.getOpenSslContext();
            this.sslContext = defaultSSLContext;
            SSLContext.registerDefault(defaultSSLContext, this);
        }
    }
    
    @Override
    public long getSslContext(final String sniHostName) {
        final SSLHostConfig sslHostConfig = this.getSSLHostConfig(sniHostName);
        final Long ctx = sslHostConfig.getOpenSslContext();
        if (ctx != null) {
            return ctx;
        }
        return 0L;
    }
    
    @Override
    public void startInternal() throws Exception {
        if (!this.running) {
            this.running = true;
            this.paused = false;
            if (this.getExecutor() == null) {
                this.createExecutor();
            }
            this.initializeConnectionLatch();
            (this.poller = new Poller()).init();
            final Thread pollerThread = new Thread(this.poller, this.getName() + "-Poller");
            pollerThread.setPriority(this.threadPriority);
            pollerThread.setDaemon(true);
            pollerThread.start();
            if (this.getUseSendfile()) {
                (this.sendfile = new Sendfile()).init();
                final Thread sendfileThread = new Thread(this.sendfile, this.getName() + "-Sendfile");
                sendfileThread.setPriority(this.threadPriority);
                sendfileThread.setDaemon(true);
                sendfileThread.start();
            }
            this.startAcceptorThreads();
        }
    }
    
    @Override
    public void stopInternal() {
        this.releaseConnectionLatch();
        if (!this.paused) {
            this.pause();
        }
        if (this.running) {
            this.running = false;
            this.poller.stop();
            for (final SocketWrapperBase<Long> socketWrapper : this.connections.values()) {
                try {
                    socketWrapper.close();
                    this.getHandler().release(socketWrapper);
                }
                catch (IOException ex) {}
            }
            for (final AbstractEndpoint.Acceptor acceptor : this.acceptors) {
                long waitLeft;
                for (waitLeft = 10000L; waitLeft > 0L && acceptor.getState() != AbstractEndpoint.Acceptor.AcceptorState.ENDED && this.serverSock != 0L; waitLeft -= 50L) {
                    try {
                        Thread.sleep(50L);
                    }
                    catch (InterruptedException ex2) {}
                }
                if (waitLeft == 0L) {
                    AprEndpoint.log.warn((Object)AprEndpoint.sm.getString("endpoint.warn.unlockAcceptorFailed", acceptor.getThreadName()));
                    if (this.serverSock != 0L) {
                        Socket.shutdown(this.serverSock, 0);
                        this.serverSock = 0L;
                    }
                }
            }
            try {
                this.poller.destroy();
            }
            catch (Exception ex3) {}
            this.poller = null;
            this.connections.clear();
            if (this.getUseSendfile()) {
                try {
                    this.sendfile.destroy();
                }
                catch (Exception ex4) {}
                this.sendfile = null;
            }
        }
        this.shutdownExecutor();
    }
    
    @Override
    public void unbind() throws Exception {
        if (this.running) {
            this.stop();
        }
        if (this.serverSockPool != 0L) {
            Pool.destroy(this.serverSockPool);
            this.serverSockPool = 0L;
        }
        if (this.serverSock != 0L) {
            Socket.close(this.serverSock);
            this.serverSock = 0L;
        }
        if (this.sslContext != 0L) {
            final Long ctx = this.sslContext;
            SSLContext.unregisterDefault(ctx);
            for (final SSLHostConfig sslHostConfig : this.sslHostConfigs.values()) {
                sslHostConfig.setOpenSslContext(null);
            }
            this.sslContext = 0L;
        }
        if (this.rootPool != 0L) {
            Pool.destroy(this.rootPool);
            this.rootPool = 0L;
        }
        this.getHandler().recycle();
    }
    
    @Override
    protected AbstractEndpoint.Acceptor createAcceptor() {
        return new Acceptor();
    }
    
    protected boolean setSocketOptions(final SocketWrapperBase<Long> socketWrapper) {
        final long socket = socketWrapper.getSocket();
        int step = 1;
        try {
            if (this.socketProperties.getSoLingerOn() && this.socketProperties.getSoLingerTime() >= 0) {
                Socket.optSet(socket, 1, this.socketProperties.getSoLingerTime());
            }
            if (this.socketProperties.getTcpNoDelay()) {
                Socket.optSet(socket, 512, this.socketProperties.getTcpNoDelay() ? 1 : 0);
            }
            Socket.timeoutSet(socket, this.socketProperties.getSoTimeout() * 1000);
            step = 2;
            if (this.sslContext != 0L) {
                SSLSocket.attach(this.sslContext, socket);
                if (SSLSocket.handshake(socket) != 0) {
                    if (AprEndpoint.log.isDebugEnabled()) {
                        AprEndpoint.log.debug((Object)(AprEndpoint.sm.getString("endpoint.err.handshake") + ": " + SSL.getLastError()));
                    }
                    return false;
                }
                if (this.negotiableProtocols.size() > 0) {
                    final byte[] negotiated = new byte[256];
                    final int len = SSLSocket.getALPN(socket, negotiated);
                    final String negotiatedProtocol = new String(negotiated, 0, len, StandardCharsets.UTF_8);
                    if (negotiatedProtocol.length() > 0) {
                        socketWrapper.setNegotiatedProtocol(negotiatedProtocol);
                        if (AprEndpoint.log.isDebugEnabled()) {
                            AprEndpoint.log.debug((Object)AprEndpoint.sm.getString("endpoint.alpn.negotiated", negotiatedProtocol));
                        }
                    }
                }
            }
        }
        catch (Throwable t) {
            ExceptionUtils.handleThrowable(t);
            if (AprEndpoint.log.isDebugEnabled()) {
                if (step == 2) {
                    AprEndpoint.log.debug((Object)AprEndpoint.sm.getString("endpoint.err.handshake"), t);
                }
                else {
                    AprEndpoint.log.debug((Object)AprEndpoint.sm.getString("endpoint.err.unexpected"), t);
                }
            }
            return false;
        }
        return true;
    }
    
    protected long allocatePoller(final int size, final long pool, final int timeout) {
        try {
            return Poll.create(size, pool, 0, timeout * 1000);
        }
        catch (Error e) {
            if (Status.APR_STATUS_IS_EINVAL(e.getError())) {
                AprEndpoint.log.info((Object)AprEndpoint.sm.getString("endpoint.poll.limitedpollsize", "" + size));
                return 0L;
            }
            AprEndpoint.log.error((Object)AprEndpoint.sm.getString("endpoint.poll.initfail"), (Throwable)e);
            return -1L;
        }
    }
    
    protected boolean processSocketWithOptions(final long socket) {
        try {
            if (this.running) {
                if (AprEndpoint.log.isDebugEnabled()) {
                    AprEndpoint.log.debug((Object)AprEndpoint.sm.getString("endpoint.debug.socket", socket));
                }
                final AprSocketWrapper wrapper = new AprSocketWrapper(socket, this);
                wrapper.setKeepAliveLeft(this.getMaxKeepAliveRequests());
                wrapper.setSecure(this.isSSLEnabled());
                wrapper.setReadTimeout(this.getSoTimeout());
                wrapper.setWriteTimeout(this.getSoTimeout());
                this.connections.put(socket, wrapper);
                this.getExecutor().execute(new SocketWithOptionsProcessor(wrapper));
            }
        }
        catch (RejectedExecutionException x) {
            AprEndpoint.log.warn((Object)("Socket processing request was rejected for:" + socket), (Throwable)x);
            return false;
        }
        catch (Throwable t) {
            ExceptionUtils.handleThrowable(t);
            AprEndpoint.log.error((Object)AprEndpoint.sm.getString("endpoint.process.fail"), t);
            return false;
        }
        return true;
    }
    
    public boolean processSocket(final long socket, final SocketEvent status) {
        try {
            final Executor executor = this.getExecutor();
            if (executor == null) {
                AprEndpoint.log.warn((Object)AprEndpoint.sm.getString("endpoint.warn.noExector", socket, null));
            }
            else {
                final SocketWrapperBase<Long> wrapper = this.connections.get(socket);
                if (wrapper != null) {
                    executor.execute(new SocketProcessor(wrapper, status));
                }
            }
        }
        catch (RejectedExecutionException x) {
            AprEndpoint.log.warn((Object)("Socket processing request was rejected for:" + socket), (Throwable)x);
            return false;
        }
        catch (Throwable t) {
            ExceptionUtils.handleThrowable(t);
            AprEndpoint.log.error((Object)AprEndpoint.sm.getString("endpoint.process.fail"), t);
            return false;
        }
        return true;
    }
    
    @Override
    public void processSocket(final SocketWrapperBase<Long> socket, final SocketEvent status, final boolean dispatch) {
        try {
            synchronized (socket) {
                final SocketProcessor proc = new SocketProcessor(socket, status);
                final Executor executor = this.getExecutor();
                if (dispatch && executor != null) {
                    executor.execute(proc);
                }
                else {
                    proc.run();
                }
            }
        }
        catch (RejectedExecutionException ree) {
            AprEndpoint.log.warn((Object)AprEndpoint.sm.getString("endpoint.executor.fail", socket), (Throwable)ree);
        }
        catch (Throwable t) {
            ExceptionUtils.handleThrowable(t);
            AprEndpoint.log.error((Object)AprEndpoint.sm.getString("endpoint.process.fail"), t);
        }
    }
    
    private void closeSocket(final long socket) {
        final SocketWrapperBase<Long> wrapper = this.connections.remove(socket);
        if (wrapper != null) {
            ((AprSocketWrapper)wrapper).close();
        }
    }
    
    private void destroySocket(final long socket) {
        this.connections.remove(socket);
        if (AprEndpoint.log.isDebugEnabled()) {
            final String msg = AprEndpoint.sm.getString("endpoint.debug.destroySocket", socket);
            if (AprEndpoint.log.isTraceEnabled()) {
                AprEndpoint.log.trace((Object)msg, (Throwable)new Exception());
            }
            else {
                AprEndpoint.log.debug((Object)msg);
            }
        }
        if (socket != 0L) {
            Socket.destroy(socket);
            this.countDownConnection();
        }
    }
    
    @Override
    protected Log getLog() {
        return AprEndpoint.log;
    }
    
    static {
        log = LogFactory.getLog((Class)AprEndpoint.class);
    }
    
    protected class Acceptor extends AbstractEndpoint.Acceptor
    {
        private final Log log;
        
        protected Acceptor() {
            this.log = LogFactory.getLog((Class)Acceptor.class);
        }
        
        @Override
        public void run() {
            int errorDelay = 0;
            while (AprEndpoint.this.running) {
                while (AprEndpoint.this.paused && AprEndpoint.this.running) {
                    this.state = AcceptorState.PAUSED;
                    try {
                        Thread.sleep(50L);
                    }
                    catch (InterruptedException ex) {}
                }
                if (!AprEndpoint.this.running) {
                    break;
                }
                this.state = AcceptorState.RUNNING;
                try {
                    AprEndpoint.this.countUpOrAwaitConnection();
                    long socket = 0L;
                    try {
                        socket = Socket.accept(AprEndpoint.this.serverSock);
                        if (this.log.isDebugEnabled()) {
                            final long sa = Address.get(1, socket);
                            final Sockaddr addr = Address.getInfo(sa);
                            this.log.debug((Object)AbstractEndpoint.sm.getString("endpoint.apr.remoteport", socket, addr.port));
                        }
                    }
                    catch (Exception e) {
                        AprEndpoint.this.countDownConnection();
                        errorDelay = AprEndpoint.this.handleExceptionWithDelay(errorDelay);
                        throw e;
                    }
                    errorDelay = 0;
                    if (AprEndpoint.this.running && !AprEndpoint.this.paused) {
                        if (AprEndpoint.this.processSocketWithOptions(socket)) {
                            continue;
                        }
                        AprEndpoint.this.closeSocket(socket);
                    }
                    else {
                        AprEndpoint.this.destroySocket(socket);
                    }
                }
                catch (Throwable t) {
                    ExceptionUtils.handleThrowable(t);
                    if (!AprEndpoint.this.running) {
                        continue;
                    }
                    final String msg = AbstractEndpoint.sm.getString("endpoint.accept.fail");
                    if (t instanceof Error) {
                        final Error e2 = (Error)t;
                        if (e2.getError() == 233) {
                            this.log.warn((Object)msg, t);
                        }
                        else {
                            this.log.error((Object)msg, t);
                        }
                    }
                    else {
                        this.log.error((Object)msg, t);
                    }
                }
            }
            this.state = AcceptorState.ENDED;
        }
    }
    
    public static class SocketInfo
    {
        public long socket;
        public long timeout;
        public int flags;
        
        public boolean read() {
            return (this.flags & 0x1) == 0x1;
        }
        
        public boolean write() {
            return (this.flags & 0x4) == 0x4;
        }
        
        public static int merge(final int flag1, final int flag2) {
            return (flag1 & 0x1) | (flag2 & 0x1) | ((flag1 & 0x4) | (flag2 & 0x4));
        }
        
        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("Socket: [");
            sb.append(this.socket);
            sb.append("], timeout: [");
            sb.append(this.timeout);
            sb.append("], flags: [");
            sb.append(this.flags);
            return sb.toString();
        }
    }
    
    public static class SocketTimeouts
    {
        protected int size;
        protected long[] sockets;
        protected long[] timeouts;
        protected int pos;
        
        public SocketTimeouts(final int size) {
            this.pos = 0;
            this.size = 0;
            this.sockets = new long[size];
            this.timeouts = new long[size];
        }
        
        public void add(final long socket, final long timeout) {
            this.sockets[this.size] = socket;
            this.timeouts[this.size] = timeout;
            ++this.size;
        }
        
        public long remove(final long socket) {
            long result = 0L;
            for (int i = 0; i < this.size; ++i) {
                if (this.sockets[i] == socket) {
                    result = this.timeouts[i];
                    this.sockets[i] = this.sockets[this.size - 1];
                    this.timeouts[i] = this.timeouts[this.size - 1];
                    --this.size;
                    break;
                }
            }
            return result;
        }
        
        public long check(final long date) {
            while (this.pos < this.size) {
                if (date >= this.timeouts[this.pos]) {
                    final long result = this.sockets[this.pos];
                    this.sockets[this.pos] = this.sockets[this.size - 1];
                    this.timeouts[this.pos] = this.timeouts[this.size - 1];
                    --this.size;
                    return result;
                }
                ++this.pos;
            }
            this.pos = 0;
            return 0L;
        }
    }
    
    public static class SocketList
    {
        protected volatile int size;
        protected int pos;
        protected long[] sockets;
        protected long[] timeouts;
        protected int[] flags;
        protected SocketInfo info;
        
        public SocketList(final int size) {
            this.info = new SocketInfo();
            this.size = 0;
            this.pos = 0;
            this.sockets = new long[size];
            this.timeouts = new long[size];
            this.flags = new int[size];
        }
        
        public int size() {
            return this.size;
        }
        
        public SocketInfo get() {
            if (this.pos == this.size) {
                return null;
            }
            this.info.socket = this.sockets[this.pos];
            this.info.timeout = this.timeouts[this.pos];
            this.info.flags = this.flags[this.pos];
            ++this.pos;
            return this.info;
        }
        
        public void clear() {
            this.size = 0;
            this.pos = 0;
        }
        
        public boolean add(final long socket, final long timeout, final int flag) {
            if (this.size == this.sockets.length) {
                return false;
            }
            for (int i = 0; i < this.size; ++i) {
                if (this.sockets[i] == socket) {
                    this.flags[i] = SocketInfo.merge(this.flags[i], flag);
                    return true;
                }
            }
            this.sockets[this.size] = socket;
            this.timeouts[this.size] = timeout;
            this.flags[this.size] = flag;
            ++this.size;
            return true;
        }
        
        public boolean remove(final long socket) {
            for (int i = 0; i < this.size; ++i) {
                if (this.sockets[i] == socket) {
                    this.sockets[i] = this.sockets[this.size - 1];
                    this.timeouts[i] = this.timeouts[this.size - 1];
                    this.flags[this.size] = this.flags[this.size - 1];
                    --this.size;
                    return true;
                }
            }
            return false;
        }
        
        public void duplicate(final SocketList copy) {
            copy.size = this.size;
            copy.pos = this.pos;
            System.arraycopy(this.sockets, 0, copy.sockets, 0, this.size);
            System.arraycopy(this.timeouts, 0, copy.timeouts, 0, this.size);
            System.arraycopy(this.flags, 0, copy.flags, 0, this.size);
        }
    }
    
    public class Poller implements Runnable
    {
        private long[] pollers;
        private int actualPollerSize;
        private int[] pollerSpace;
        private int pollerCount;
        private int pollerTime;
        private int nextPollerTime;
        private long pool;
        private long[] desc;
        private SocketList addList;
        private SocketList closeList;
        private SocketTimeouts timeouts;
        private long lastMaintain;
        private AtomicInteger connectionCount;
        private volatile boolean pollerRunning;
        
        public Poller() {
            this.pollers = null;
            this.actualPollerSize = 0;
            this.pollerSpace = null;
            this.pool = 0L;
            this.addList = null;
            this.closeList = null;
            this.timeouts = null;
            this.lastMaintain = System.currentTimeMillis();
            this.connectionCount = new AtomicInteger(0);
            this.pollerRunning = true;
        }
        
        public int getConnectionCount() {
            return this.connectionCount.get();
        }
        
        protected synchronized void init() {
            this.pool = Pool.create(AprEndpoint.this.serverSockPool);
            final int defaultPollerSize = AprEndpoint.this.getMaxConnections();
            if ((OS.IS_WIN32 || OS.IS_WIN64) && defaultPollerSize > 1024) {
                this.actualPollerSize = 1024;
            }
            else {
                this.actualPollerSize = defaultPollerSize;
            }
            this.timeouts = new SocketTimeouts(defaultPollerSize);
            long pollset = AprEndpoint.this.allocatePoller(this.actualPollerSize, this.pool, -1);
            if (pollset == 0L && this.actualPollerSize > 1024) {
                this.actualPollerSize = 1024;
                pollset = AprEndpoint.this.allocatePoller(this.actualPollerSize, this.pool, -1);
            }
            if (pollset == 0L) {
                this.actualPollerSize = 62;
                pollset = AprEndpoint.this.allocatePoller(this.actualPollerSize, this.pool, -1);
            }
            this.pollerCount = defaultPollerSize / this.actualPollerSize;
            this.pollerTime = AprEndpoint.this.pollTime / this.pollerCount;
            this.nextPollerTime = this.pollerTime;
            (this.pollers = new long[this.pollerCount])[0] = pollset;
            for (int i = 1; i < this.pollerCount; ++i) {
                this.pollers[i] = AprEndpoint.this.allocatePoller(this.actualPollerSize, this.pool, -1);
            }
            this.pollerSpace = new int[this.pollerCount];
            for (int i = 0; i < this.pollerCount; ++i) {
                this.pollerSpace[i] = this.actualPollerSize;
            }
            this.desc = new long[this.actualPollerSize * 4];
            this.connectionCount.set(0);
            this.addList = new SocketList(defaultPollerSize);
            this.closeList = new SocketList(defaultPollerSize);
        }
        
        protected synchronized void stop() {
            this.pollerRunning = false;
        }
        
        protected synchronized void destroy() {
            try {
                this.notify();
                this.wait(this.pollerCount * AprEndpoint.this.pollTime / 1000);
            }
            catch (InterruptedException ex) {}
            for (SocketInfo info = this.closeList.get(); info != null; info = this.closeList.get()) {
                this.addList.remove(info.socket);
                this.removeFromPoller(info.socket);
                AprEndpoint.this.destroySocket(info.socket);
            }
            this.closeList.clear();
            for (SocketInfo info = this.addList.get(); info != null; info = this.addList.get()) {
                this.removeFromPoller(info.socket);
                AprEndpoint.this.destroySocket(info.socket);
            }
            this.addList.clear();
            for (int i = 0; i < this.pollerCount; ++i) {
                final int rv = Poll.pollset(this.pollers[i], this.desc);
                if (rv > 0) {
                    for (int n = 0; n < rv; ++n) {
                        AprEndpoint.this.destroySocket(this.desc[n * 2 + 1]);
                    }
                }
            }
            Pool.destroy(this.pool);
            this.connectionCount.set(0);
        }
        
        private void add(final long socket, long timeout, final int flags) {
            if (AprEndpoint.log.isDebugEnabled()) {
                final String msg = AbstractEndpoint.sm.getString("endpoint.debug.pollerAdd", socket, timeout, flags);
                if (AprEndpoint.log.isTraceEnabled()) {
                    AprEndpoint.log.trace((Object)msg, (Throwable)new Exception());
                }
                else {
                    AprEndpoint.log.debug((Object)msg);
                }
            }
            if (timeout <= 0L) {
                timeout = 2147483647L;
            }
            synchronized (this) {
                if (this.addList.add(socket, timeout, flags)) {
                    this.notify();
                }
            }
        }
        
        private boolean addToPoller(final long socket, final int events) {
            int rv = -1;
            for (int i = 0; i < this.pollers.length; ++i) {
                if (this.pollerSpace[i] > 0) {
                    rv = Poll.add(this.pollers[i], socket, events);
                    if (rv == 0) {
                        final int[] pollerSpace = this.pollerSpace;
                        final int n = i;
                        --pollerSpace[n];
                        this.connectionCount.incrementAndGet();
                        return true;
                    }
                }
            }
            return false;
        }
        
        private synchronized void close(final long socket) {
            this.closeList.add(socket, 0L, 0);
            this.notify();
        }
        
        private void removeFromPoller(final long socket) {
            if (AprEndpoint.log.isDebugEnabled()) {
                AprEndpoint.log.debug((Object)AbstractEndpoint.sm.getString("endpoint.debug.pollerRemove", socket));
            }
            int rv = -1;
            for (int i = 0; i < this.pollers.length; ++i) {
                if (this.pollerSpace[i] < this.actualPollerSize) {
                    rv = Poll.remove(this.pollers[i], socket);
                    if (rv != 70015) {
                        final int[] pollerSpace = this.pollerSpace;
                        final int n = i;
                        ++pollerSpace[n];
                        this.connectionCount.decrementAndGet();
                        if (AprEndpoint.log.isDebugEnabled()) {
                            AprEndpoint.log.debug((Object)AbstractEndpoint.sm.getString("endpoint.debug.pollerRemoved", socket));
                            break;
                        }
                        break;
                    }
                }
            }
            this.timeouts.remove(socket);
        }
        
        private synchronized void maintain() {
            final long date = System.currentTimeMillis();
            if (date - this.lastMaintain < 1000L) {
                return;
            }
            this.lastMaintain = date;
            for (long socket = this.timeouts.check(date); socket != 0L; socket = this.timeouts.check(date)) {
                if (AprEndpoint.log.isDebugEnabled()) {
                    AprEndpoint.log.debug((Object)AbstractEndpoint.sm.getString("endpoint.debug.socketTimeout", socket));
                }
                this.removeFromPoller(socket);
                AprEndpoint.this.destroySocket(socket);
                this.addList.remove(socket);
                this.closeList.remove(socket);
            }
        }
        
        @Override
        public String toString() {
            final StringBuffer buf = new StringBuffer();
            buf.append("Poller");
            final long[] res = new long[this.actualPollerSize * 2];
            for (int i = 0; i < this.pollers.length; ++i) {
                final int count = Poll.pollset(this.pollers[i], res);
                buf.append(" [ ");
                for (int j = 0; j < count; ++j) {
                    buf.append(this.desc[2 * j + 1]).append(" ");
                }
                buf.append("]");
            }
            return buf.toString();
        }
        
        @Override
        public void run() {
            final SocketList localAddList = new SocketList(AprEndpoint.this.getMaxConnections());
            final SocketList localCloseList = new SocketList(AprEndpoint.this.getMaxConnections());
            while (this.pollerRunning) {
                while (this.pollerRunning && this.connectionCount.get() < 1 && this.addList.size() < 1 && this.closeList.size() < 1) {
                    try {
                        if (AprEndpoint.this.getSoTimeout() > 0 && this.pollerRunning) {
                            this.maintain();
                        }
                        synchronized (this) {
                            if (this.addList.size() >= 1 || this.closeList.size() >= 1) {
                                continue;
                            }
                            this.wait(10000L);
                        }
                    }
                    catch (InterruptedException ex) {}
                    catch (Throwable t) {
                        ExceptionUtils.handleThrowable(t);
                        AprEndpoint.this.getLog().warn((Object)AbstractEndpoint.sm.getString("endpoint.timeout.err"));
                    }
                }
                if (!this.pollerRunning) {
                    break;
                }
                try {
                    synchronized (this) {
                        if (this.closeList.size() > 0) {
                            this.closeList.duplicate(localCloseList);
                            this.closeList.clear();
                        }
                        else {
                            localCloseList.clear();
                        }
                    }
                    synchronized (this) {
                        if (this.addList.size() > 0) {
                            this.addList.duplicate(localAddList);
                            this.addList.clear();
                        }
                        else {
                            localAddList.clear();
                        }
                    }
                    if (localCloseList.size() > 0) {
                        for (SocketInfo info = localCloseList.get(); info != null; info = localCloseList.get()) {
                            localAddList.remove(info.socket);
                            this.removeFromPoller(info.socket);
                            AprEndpoint.this.destroySocket(info.socket);
                        }
                    }
                    if (localAddList.size() > 0) {
                        SocketInfo info = localAddList.get();
                        while (info != null) {
                            if (AprEndpoint.log.isDebugEnabled()) {
                                AprEndpoint.log.debug((Object)AbstractEndpoint.sm.getString("endpoint.debug.pollerAddDo", info.socket));
                            }
                            this.timeouts.remove(info.socket);
                            final AprSocketWrapper wrapper = AprEndpoint.this.connections.get(info.socket);
                            if (wrapper == null) {
                                continue;
                            }
                            if (info.read() || info.write()) {
                                wrapper.pollerFlags = (wrapper.pollerFlags | (info.read() ? 1 : 0) | (info.write() ? 4 : 0));
                                this.removeFromPoller(info.socket);
                                if (!this.addToPoller(info.socket, wrapper.pollerFlags)) {
                                    AprEndpoint.this.closeSocket(info.socket);
                                }
                                else {
                                    this.timeouts.add(info.socket, System.currentTimeMillis() + info.timeout);
                                }
                            }
                            else {
                                AprEndpoint.this.closeSocket(info.socket);
                                AprEndpoint.this.getLog().warn((Object)AbstractEndpoint.sm.getString("endpoint.apr.pollAddInvalid", info));
                            }
                            info = localAddList.get();
                        }
                    }
                    for (int i = 0; i < this.pollers.length; ++i) {
                        boolean reset = false;
                        int rv = 0;
                        if (this.pollerSpace[i] < this.actualPollerSize) {
                            rv = Poll.poll(this.pollers[i], this.nextPollerTime, this.desc, true);
                            this.nextPollerTime = this.pollerTime;
                        }
                        else {
                            this.nextPollerTime += this.pollerTime;
                        }
                        if (rv > 0) {
                            rv = this.mergeDescriptors(this.desc, rv);
                            final int[] pollerSpace = this.pollerSpace;
                            final int n2 = i;
                            pollerSpace[n2] += rv;
                            this.connectionCount.addAndGet(-rv);
                            for (int n = 0; n < rv; ++n) {
                                long timeout = this.timeouts.remove(this.desc[n * 2 + 1]);
                                final AprSocketWrapper wrapper2 = AprEndpoint.this.connections.get(this.desc[n * 2 + 1]);
                                if (AprEndpoint.this.getLog().isDebugEnabled()) {
                                    AprEndpoint.log.debug((Object)AbstractEndpoint.sm.getString("endpoint.debug.pollerProcess", this.desc[n * 2 + 1], this.desc[n * 2]));
                                }
                                wrapper2.pollerFlags &= ~(int)this.desc[n * 2];
                                if ((this.desc[n * 2] & 0x20L) == 0x20L || (this.desc[n * 2] & 0x10L) == 0x10L || (this.desc[n * 2] & 0x40L) == 0x40L) {
                                    if ((this.desc[n * 2] & 0x1L) == 0x1L) {
                                        if (!AprEndpoint.this.processSocket(this.desc[n * 2 + 1], SocketEvent.OPEN_READ)) {
                                            AprEndpoint.this.closeSocket(this.desc[n * 2 + 1]);
                                        }
                                    }
                                    else if ((this.desc[n * 2] & 0x4L) == 0x4L) {
                                        if (!AprEndpoint.this.processSocket(this.desc[n * 2 + 1], SocketEvent.OPEN_WRITE)) {
                                            AprEndpoint.this.closeSocket(this.desc[n * 2 + 1]);
                                        }
                                    }
                                    else if ((wrapper2.pollerFlags & 0x1) == 0x1) {
                                        if (!AprEndpoint.this.processSocket(this.desc[n * 2 + 1], SocketEvent.OPEN_READ)) {
                                            AprEndpoint.this.closeSocket(this.desc[n * 2 + 1]);
                                        }
                                    }
                                    else if ((wrapper2.pollerFlags & 0x4) == 0x4) {
                                        if (!AprEndpoint.this.processSocket(this.desc[n * 2 + 1], SocketEvent.OPEN_WRITE)) {
                                            AprEndpoint.this.closeSocket(this.desc[n * 2 + 1]);
                                        }
                                    }
                                    else {
                                        AprEndpoint.this.closeSocket(this.desc[n * 2 + 1]);
                                    }
                                }
                                else if ((this.desc[n * 2] & 0x1L) == 0x1L || (this.desc[n * 2] & 0x4L) == 0x4L) {
                                    boolean error = false;
                                    if ((this.desc[n * 2] & 0x1L) == 0x1L && !AprEndpoint.this.processSocket(this.desc[n * 2 + 1], SocketEvent.OPEN_READ)) {
                                        error = true;
                                        AprEndpoint.this.closeSocket(this.desc[n * 2 + 1]);
                                    }
                                    if (!error && (this.desc[n * 2] & 0x4L) == 0x4L && !AprEndpoint.this.processSocket(this.desc[n * 2 + 1], SocketEvent.OPEN_WRITE)) {
                                        error = true;
                                        AprEndpoint.this.closeSocket(this.desc[n * 2 + 1]);
                                    }
                                    if (!error && wrapper2.pollerFlags != 0) {
                                        if (timeout > 0L) {
                                            timeout -= System.currentTimeMillis();
                                        }
                                        if (timeout <= 0L) {
                                            timeout = 1L;
                                        }
                                        if (timeout > 2147483647L) {
                                            timeout = 2147483647L;
                                        }
                                        this.add(this.desc[n * 2 + 1], (int)timeout, wrapper2.pollerFlags);
                                    }
                                }
                                else {
                                    AprEndpoint.this.getLog().warn((Object)AbstractEndpoint.sm.getString("endpoint.apr.pollUnknownEvent", this.desc[n * 2]));
                                    AprEndpoint.this.closeSocket(this.desc[n * 2 + 1]);
                                }
                            }
                        }
                        else if (rv < 0) {
                            int errn = -rv;
                            if (errn != 120001 && errn != 120003) {
                                if (errn > 120000) {
                                    errn -= 120000;
                                }
                                AprEndpoint.this.getLog().error((Object)AbstractEndpoint.sm.getString("endpoint.apr.pollError", errn, Error.strerror(errn)));
                                reset = true;
                            }
                        }
                        if (reset && this.pollerRunning) {
                            final int count = Poll.pollset(this.pollers[i], this.desc);
                            final long newPoller = AprEndpoint.this.allocatePoller(this.actualPollerSize, this.pool, -1);
                            this.pollerSpace[i] = this.actualPollerSize;
                            this.connectionCount.addAndGet(-count);
                            Poll.destroy(this.pollers[i]);
                            this.pollers[i] = newPoller;
                        }
                    }
                }
                catch (Throwable t) {
                    ExceptionUtils.handleThrowable(t);
                    AprEndpoint.this.getLog().warn((Object)AbstractEndpoint.sm.getString("endpoint.poll.error"), t);
                }
                try {
                    if (AprEndpoint.this.getSoTimeout() <= 0 || !this.pollerRunning) {
                        continue;
                    }
                    this.maintain();
                }
                catch (Throwable t) {
                    ExceptionUtils.handleThrowable(t);
                    AprEndpoint.this.getLog().warn((Object)AbstractEndpoint.sm.getString("endpoint.timeout.err"), t);
                }
            }
            synchronized (this) {
                this.notifyAll();
            }
        }
        
        private int mergeDescriptors(final long[] desc, final int startCount) {
            final HashMap<Long, Long> merged = new HashMap<Long, Long>(startCount);
            for (int n = 0; n < startCount; ++n) {
                final Long old = merged.put(desc[2 * n + 1], desc[2 * n]);
                if (old != null) {
                    merged.put(desc[2 * n + 1], desc[2 * n] | (long)old);
                    if (AprEndpoint.log.isDebugEnabled()) {
                        AprEndpoint.log.debug((Object)AbstractEndpoint.sm.getString("endpoint.apr.pollMergeEvents", desc[2 * n + 1], desc[2 * n], old));
                    }
                }
            }
            int i = 0;
            for (final Map.Entry<Long, Long> entry : merged.entrySet()) {
                desc[i++] = entry.getValue();
                desc[i++] = entry.getKey();
            }
            return merged.size();
        }
    }
    
    public static class SendfileData extends SendfileDataBase
    {
        protected long fd;
        protected long fdpool;
        protected long socket;
        
        public SendfileData(final String filename, final long pos, final long length) {
            super(filename, pos, length);
        }
    }
    
    public class Sendfile implements Runnable
    {
        protected long sendfilePollset;
        protected long pool;
        protected long[] desc;
        protected HashMap<Long, SendfileData> sendfileData;
        protected int sendfileCount;
        protected ArrayList<SendfileData> addS;
        private volatile boolean sendfileRunning;
        
        public Sendfile() {
            this.sendfilePollset = 0L;
            this.pool = 0L;
            this.sendfileRunning = true;
        }
        
        public int getSendfileCount() {
            return this.sendfileCount;
        }
        
        protected void init() {
            this.pool = Pool.create(AprEndpoint.this.serverSockPool);
            int size = AprEndpoint.this.sendfileSize;
            if (size <= 0) {
                size = ((OS.IS_WIN32 || OS.IS_WIN64) ? 1024 : 16384);
            }
            this.sendfilePollset = AprEndpoint.this.allocatePoller(size, this.pool, AprEndpoint.this.getSoTimeout());
            if (this.sendfilePollset == 0L && size > 1024) {
                size = 1024;
                this.sendfilePollset = AprEndpoint.this.allocatePoller(size, this.pool, AprEndpoint.this.getSoTimeout());
            }
            if (this.sendfilePollset == 0L) {
                size = 62;
                this.sendfilePollset = AprEndpoint.this.allocatePoller(size, this.pool, AprEndpoint.this.getSoTimeout());
            }
            this.desc = new long[size * 2];
            this.sendfileData = new HashMap<Long, SendfileData>(size);
            this.addS = new ArrayList<SendfileData>();
        }
        
        protected void destroy() {
            this.sendfileRunning = false;
            try {
                synchronized (this) {
                    this.notify();
                    this.wait(AprEndpoint.this.pollTime / 1000);
                }
            }
            catch (InterruptedException ex) {}
            for (int i = this.addS.size() - 1; i >= 0; --i) {
                final SendfileData data = this.addS.get(i);
                AprEndpoint.this.closeSocket(data.socket);
            }
            final int rv = Poll.pollset(this.sendfilePollset, this.desc);
            if (rv > 0) {
                for (int n = 0; n < rv; ++n) {
                    AprEndpoint.this.closeSocket(this.desc[n * 2 + 1]);
                }
            }
            Pool.destroy(this.pool);
            this.sendfileData.clear();
        }
        
        public SendfileState add(final SendfileData data) {
            try {
                data.fdpool = Socket.pool(data.socket);
                data.fd = File.open(data.fileName, 4129, 0, data.fdpool);
                Socket.timeoutSet(data.socket, 0L);
                while (true) {
                    final long nw = Socket.sendfilen(data.socket, data.fd, data.pos, data.length, 0);
                    if (nw < 0L) {
                        if (-nw != 120002L) {
                            Pool.destroy(data.fdpool);
                            data.socket = 0L;
                            return SendfileState.ERROR;
                        }
                        break;
                    }
                    else {
                        data.pos += nw;
                        data.length -= nw;
                        if (data.length == 0L) {
                            Pool.destroy(data.fdpool);
                            Socket.timeoutSet(data.socket, AprEndpoint.this.getSoTimeout() * 1000);
                            return SendfileState.DONE;
                        }
                        continue;
                    }
                }
            }
            catch (Exception e) {
                AprEndpoint.log.warn((Object)AbstractEndpoint.sm.getString("endpoint.sendfile.error"), (Throwable)e);
                return SendfileState.ERROR;
            }
            synchronized (this) {
                this.addS.add(data);
                this.notify();
            }
            return SendfileState.PENDING;
        }
        
        protected void remove(final SendfileData data) {
            final int rv = Poll.remove(this.sendfilePollset, data.socket);
            if (rv == 0) {
                --this.sendfileCount;
            }
            this.sendfileData.remove(data.socket);
        }
        
        @Override
        public void run() {
            long maintainTime = 0L;
            while (this.sendfileRunning) {
                while (this.sendfileRunning && AprEndpoint.this.paused) {
                    try {
                        Thread.sleep(1000L);
                    }
                    catch (InterruptedException ex) {}
                }
                while (this.sendfileRunning && this.sendfileCount < 1 && this.addS.size() < 1) {
                    maintainTime = 0L;
                    try {
                        synchronized (this) {
                            this.wait();
                        }
                    }
                    catch (InterruptedException ex2) {}
                }
                if (!this.sendfileRunning) {
                    break;
                }
                try {
                    if (this.addS.size() > 0) {
                        synchronized (this) {
                            for (int i = this.addS.size() - 1; i >= 0; --i) {
                                final SendfileData data = this.addS.get(i);
                                final int rv = Poll.add(this.sendfilePollset, data.socket, 4);
                                if (rv == 0) {
                                    this.sendfileData.put(data.socket, data);
                                    ++this.sendfileCount;
                                }
                                else {
                                    AprEndpoint.this.getLog().warn((Object)AbstractEndpoint.sm.getString("endpoint.sendfile.addfail", rv, Error.strerror(rv)));
                                    AprEndpoint.this.closeSocket(data.socket);
                                }
                            }
                            this.addS.clear();
                        }
                    }
                    maintainTime += AprEndpoint.this.pollTime;
                    int rv2 = Poll.poll(this.sendfilePollset, AprEndpoint.this.pollTime, this.desc, false);
                    if (rv2 > 0) {
                        for (int n = 0; n < rv2; ++n) {
                            final SendfileData state = this.sendfileData.get(this.desc[n * 2 + 1]);
                            if ((this.desc[n * 2] & 0x20L) == 0x20L || (this.desc[n * 2] & 0x10L) == 0x10L) {
                                this.remove(state);
                                AprEndpoint.this.closeSocket(state.socket);
                            }
                            else {
                                final long nw = Socket.sendfilen(state.socket, state.fd, state.pos, state.length, 0);
                                if (nw < 0L) {
                                    this.remove(state);
                                    AprEndpoint.this.closeSocket(state.socket);
                                }
                                else {
                                    final SendfileData sendfileData = state;
                                    sendfileData.pos += nw;
                                    final SendfileData sendfileData2 = state;
                                    sendfileData2.length -= nw;
                                    if (state.length == 0L) {
                                        this.remove(state);
                                        if (state.keepAlive) {
                                            Pool.destroy(state.fdpool);
                                            Socket.timeoutSet(state.socket, AprEndpoint.this.getSoTimeout() * 1000);
                                            AprEndpoint.this.getPoller().add(state.socket, AprEndpoint.this.getKeepAliveTimeout(), 1);
                                        }
                                        else {
                                            AprEndpoint.this.closeSocket(state.socket);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else if (rv2 < 0) {
                        int errn = -rv2;
                        if (errn != 120001 && errn != 120003) {
                            if (errn > 120000) {
                                errn -= 120000;
                            }
                            AprEndpoint.this.getLog().error((Object)AbstractEndpoint.sm.getString("endpoint.apr.pollError", errn, Error.strerror(errn)));
                            synchronized (this) {
                                this.destroy();
                                this.init();
                            }
                            continue;
                        }
                    }
                    if (AprEndpoint.this.getSoTimeout() <= 0 || maintainTime <= 1000000L || !this.sendfileRunning) {
                        continue;
                    }
                    rv2 = Poll.maintain(this.sendfilePollset, this.desc, false);
                    maintainTime = 0L;
                    if (rv2 <= 0) {
                        continue;
                    }
                    for (int n = 0; n < rv2; ++n) {
                        final SendfileData state = this.sendfileData.get(this.desc[n]);
                        this.remove(state);
                        AprEndpoint.this.closeSocket(state.socket);
                    }
                }
                catch (Throwable t) {
                    ExceptionUtils.handleThrowable(t);
                    AprEndpoint.this.getLog().error((Object)AbstractEndpoint.sm.getString("endpoint.poll.error"), t);
                }
            }
            synchronized (this) {
                this.notifyAll();
            }
        }
    }
    
    protected class SocketWithOptionsProcessor implements Runnable
    {
        protected SocketWrapperBase<Long> socket;
        
        public SocketWithOptionsProcessor(final SocketWrapperBase<Long> socket) {
            this.socket = null;
            this.socket = socket;
        }
        
        @Override
        public void run() {
            synchronized (this.socket) {
                if (!AprEndpoint.this.deferAccept) {
                    if (AprEndpoint.this.setSocketOptions(this.socket)) {
                        AprEndpoint.this.getPoller().add(this.socket.getSocket(), AprEndpoint.this.getSoTimeout(), 1);
                    }
                    else {
                        AprEndpoint.this.closeSocket(this.socket.getSocket());
                        this.socket = null;
                    }
                }
                else {
                    if (!AprEndpoint.this.setSocketOptions(this.socket)) {
                        AprEndpoint.this.closeSocket(this.socket.getSocket());
                        this.socket = null;
                        return;
                    }
                    final Handler.SocketState state = AprEndpoint.this.getHandler().process(this.socket, SocketEvent.OPEN_READ);
                    if (state == Handler.SocketState.CLOSED) {
                        AprEndpoint.this.closeSocket(this.socket.getSocket());
                        this.socket = null;
                    }
                }
            }
        }
    }
    
    protected class SocketProcessor implements Runnable
    {
        private final SocketWrapperBase<Long> socket;
        private final SocketEvent status;
        
        public SocketProcessor(final SocketWrapperBase<Long> socket, final SocketEvent status) {
            this.socket = socket;
            if (status == null) {
                throw new NullPointerException();
            }
            this.status = status;
        }
        
        @Override
        public void run() {
            synchronized (this.socket) {
                if (this.socket.getSocket() == null) {
                    return;
                }
                final Handler.SocketState state = AprEndpoint.this.getHandler().process(this.socket, this.status);
                if (state == Handler.SocketState.CLOSED) {
                    AprEndpoint.this.closeSocket(this.socket.getSocket());
                }
            }
        }
    }
    
    public static class AprSocketWrapper extends SocketWrapperBase<Long>
    {
        private static final int SSL_OUTPUT_BUFFER_SIZE = 8192;
        private final ByteBuffer sslOutputBuffer;
        private final Object closedLock;
        private volatile boolean closed;
        private int pollerFlags;
        
        public AprSocketWrapper(final Long socket, final AprEndpoint endpoint) {
            super(socket, endpoint);
            this.closedLock = new Object();
            this.closed = false;
            this.pollerFlags = 0;
            if (endpoint.isSSLEnabled()) {
                (this.sslOutputBuffer = ByteBuffer.allocateDirect(8192)).position(8192);
            }
            else {
                this.sslOutputBuffer = null;
            }
            this.socketBufferHandler = new SocketBufferHandler(9000, 9000, true);
        }
        
        @Override
        public int read(final boolean block, final byte[] b, final int off, final int len) throws IOException {
            this.socketBufferHandler.configureReadBufferForRead();
            final ByteBuffer readBuffer = this.socketBufferHandler.getReadBuffer();
            final int remaining = readBuffer.remaining();
            if (remaining >= len) {
                readBuffer.get(b, off, len);
                return len;
            }
            if (remaining > 0) {
                readBuffer.get(b, off, remaining);
                return remaining;
            }
            final int nRead = this.fillReadBuffer(block);
            if (nRead <= 0) {
                return nRead;
            }
            this.socketBufferHandler.configureReadBufferForRead();
            if (nRead > len) {
                readBuffer.get(b, off, len);
                return len;
            }
            readBuffer.get(b, off, nRead);
            return nRead;
        }
        
        private int fillReadBuffer(final boolean block) throws IOException {
            if (this.closed) {
                throw new IOException(AprSocketWrapper.sm.getString("socket.apr.closed", ((SocketWrapperBase<Object>)this).getSocket()));
            }
            this.socketBufferHandler.configureReadBufferForWrite();
            final ByteBuffer socketReadBuffer = this.socketBufferHandler.getReadBuffer();
            final Lock readLock = this.getBlockingStatusReadLock();
            final ReentrantReadWriteLock.WriteLock writeLock = this.getBlockingStatusWriteLock();
            boolean readDone = false;
            int result = 0;
            readLock.lock();
            try {
                if (this.getBlockingStatus() == block) {
                    if (block) {
                        Socket.timeoutSet(this.getSocket(), this.getReadTimeout() * 1000L);
                    }
                    result = Socket.recvb(this.getSocket(), socketReadBuffer, socketReadBuffer.position(), socketReadBuffer.remaining());
                    readDone = true;
                }
            }
            finally {
                readLock.unlock();
            }
            if (!readDone) {
                writeLock.lock();
                try {
                    this.setBlockingStatus(block);
                    if (block) {
                        Socket.timeoutSet(this.getSocket(), this.getReadTimeout() * 1000L);
                    }
                    else {
                        Socket.timeoutSet(this.getSocket(), 0L);
                    }
                    readLock.lock();
                    try {
                        writeLock.unlock();
                        result = Socket.recvb(this.getSocket(), socketReadBuffer, socketReadBuffer.position(), socketReadBuffer.remaining());
                    }
                    finally {
                        readLock.unlock();
                    }
                }
                finally {
                    if (writeLock.isHeldByCurrentThread()) {
                        writeLock.unlock();
                    }
                }
            }
            if (result > 0) {
                socketReadBuffer.position(socketReadBuffer.position() + result);
                return result;
            }
            if (result == 0 || -result == 120002) {
                return 0;
            }
            if (-result == 20014 && this.isSecure()) {
                if (AprEndpoint.log.isDebugEnabled()) {
                    AprEndpoint.log.debug((Object)AprSocketWrapper.sm.getString("socket.apr.read.sslGeneralError", ((SocketWrapperBase<Object>)this).getSocket(), this));
                }
                return 0;
            }
            if (-result == 120005 || -result == 120001) {
                if (block) {
                    throw new SocketTimeoutException(AprSocketWrapper.sm.getString("iib.readtimeout"));
                }
                return 0;
            }
            else {
                if (-result == 70014) {
                    return -1;
                }
                if ((OS.IS_WIN32 || OS.IS_WIN64) && -result == 730053) {
                    throw new EOFException(AprSocketWrapper.sm.getString("socket.apr.clientAbort"));
                }
                throw new IOException(AprSocketWrapper.sm.getString("socket.apr.read.error", -result, ((SocketWrapperBase<Object>)this).getSocket(), this));
            }
        }
        
        @Override
        public boolean isReadyForRead() throws IOException {
            this.socketBufferHandler.configureReadBufferForRead();
            if (this.socketBufferHandler.getReadBuffer().remaining() > 0) {
                return true;
            }
            this.fillReadBuffer(false);
            final boolean isReady = this.socketBufferHandler.getReadBuffer().position() > 0;
            return isReady;
        }
        
        @Override
        public void close() {
            synchronized (this.closedLock) {
                if (this.closed) {
                    return;
                }
                this.closed = true;
                if (this.sslOutputBuffer != null) {
                    ByteBufferUtils.cleanDirectBuffer(this.sslOutputBuffer);
                }
                ((AprEndpoint)this.getEndpoint()).getPoller().close(this.getSocket());
            }
        }
        
        @Override
        protected void doWriteInternal(final boolean block) throws IOException {
            if (this.closed) {
                throw new IOException(AprSocketWrapper.sm.getString("socket.apr.closed", ((SocketWrapperBase<Object>)this).getSocket()));
            }
            final Lock readLock = this.getBlockingStatusReadLock();
            final ReentrantReadWriteLock.WriteLock writeLock = this.getBlockingStatusWriteLock();
            readLock.lock();
            try {
                if (this.getBlockingStatus() == block) {
                    if (block) {
                        Socket.timeoutSet(this.getSocket(), this.getWriteTimeout() * 1000L);
                    }
                    this.doWriteInternal();
                    return;
                }
            }
            finally {
                readLock.unlock();
            }
            writeLock.lock();
            try {
                this.setBlockingStatus(block);
                if (block) {
                    Socket.timeoutSet(this.getSocket(), this.getWriteTimeout() * 1000L);
                }
                else {
                    Socket.timeoutSet(this.getSocket(), 0L);
                }
                readLock.lock();
                try {
                    writeLock.unlock();
                    this.doWriteInternal();
                }
                finally {
                    readLock.unlock();
                }
            }
            finally {
                if (writeLock.isHeldByCurrentThread()) {
                    writeLock.unlock();
                }
            }
        }
        
        private void doWriteInternal() throws IOException {
            final ByteBuffer socketWriteBuffer = this.socketBufferHandler.getWriteBuffer();
            int thisTime;
            do {
                thisTime = 0;
                if (this.getEndpoint().isSSLEnabled()) {
                    if (this.sslOutputBuffer.remaining() == 0) {
                        this.sslOutputBuffer.clear();
                        this.socketBufferHandler.configureWriteBufferForRead();
                        SocketWrapperBase.transfer(socketWriteBuffer, this.sslOutputBuffer);
                        this.sslOutputBuffer.flip();
                    }
                    thisTime = Socket.sendb(this.getSocket(), this.sslOutputBuffer, this.sslOutputBuffer.position(), this.sslOutputBuffer.limit());
                    if (thisTime > 0) {
                        this.sslOutputBuffer.position(this.sslOutputBuffer.position() + thisTime);
                    }
                }
                else {
                    this.socketBufferHandler.configureWriteBufferForRead();
                    thisTime = Socket.sendb(this.getSocket(), socketWriteBuffer, socketWriteBuffer.position(), socketWriteBuffer.remaining());
                    if (thisTime > 0) {
                        socketWriteBuffer.position(socketWriteBuffer.position() + thisTime);
                    }
                }
                if (Status.APR_STATUS_IS_EAGAIN(-thisTime)) {
                    thisTime = 0;
                }
                else {
                    if (-thisTime == 70014) {
                        throw new EOFException(AprSocketWrapper.sm.getString("socket.apr.clientAbort"));
                    }
                    if ((OS.IS_WIN32 || OS.IS_WIN64) && -thisTime == 730053) {
                        throw new EOFException(AprSocketWrapper.sm.getString("socket.apr.clientAbort"));
                    }
                    if (thisTime < 0) {
                        throw new IOException(AprSocketWrapper.sm.getString("socket.apr.write.error", -thisTime, ((SocketWrapperBase<Object>)this).getSocket(), this));
                    }
                    continue;
                }
            } while ((thisTime > 0 || this.getBlockingStatus()) && socketWriteBuffer.hasRemaining());
        }
        
        @Override
        public void registerReadInterest() {
            synchronized (this.closedLock) {
                if (this.closed) {
                    return;
                }
                final Poller p = ((AprEndpoint)this.getEndpoint()).getPoller();
                if (p != null) {
                    p.add(this.getSocket(), this.getReadTimeout(), 1);
                }
            }
        }
        
        @Override
        public void registerWriteInterest() {
            synchronized (this.closedLock) {
                if (this.closed) {
                    return;
                }
                ((AprEndpoint)this.getEndpoint()).getPoller().add(this.getSocket(), this.getWriteTimeout(), 4);
            }
        }
        
        @Override
        public SendfileDataBase createSendfileData(final String filename, final long pos, final long length) {
            return new SendfileData(filename, pos, length);
        }
        
        @Override
        public SendfileState processSendfile(final SendfileDataBase sendfileData) {
            ((SendfileData)sendfileData).socket = this.getSocket();
            return ((AprEndpoint)this.getEndpoint()).getSendfile().add((SendfileData)sendfileData);
        }
        
        @Override
        protected void populateRemoteAddr() {
            if (this.closed) {
                return;
            }
            try {
                final long socket = this.getSocket();
                final long sa = Address.get(1, socket);
                this.remoteAddr = Address.getip(sa);
            }
            catch (Exception e) {
                AprEndpoint.log.warn((Object)AprSocketWrapper.sm.getString("endpoint.warn.noRemoteAddr", ((SocketWrapperBase<Object>)this).getSocket()), (Throwable)e);
            }
        }
        
        @Override
        protected void populateRemoteHost() {
            if (this.closed) {
                return;
            }
            try {
                final long socket = this.getSocket();
                final long sa = Address.get(1, socket);
                this.remoteHost = Address.getnameinfo(sa, 0);
                if (this.remoteAddr == null) {
                    this.remoteAddr = Address.getip(sa);
                }
            }
            catch (Exception e) {
                AprEndpoint.log.warn((Object)AprSocketWrapper.sm.getString("endpoint.warn.noRemoteHost", ((SocketWrapperBase<Object>)this).getSocket()), (Throwable)e);
            }
        }
        
        @Override
        protected void populateRemotePort() {
            if (this.closed) {
                return;
            }
            try {
                final long socket = this.getSocket();
                final long sa = Address.get(1, socket);
                final Sockaddr addr = Address.getInfo(sa);
                this.remotePort = addr.port;
            }
            catch (Exception e) {
                AprEndpoint.log.warn((Object)AprSocketWrapper.sm.getString("endpoint.warn.noRemotePort", ((SocketWrapperBase<Object>)this).getSocket()), (Throwable)e);
            }
        }
        
        @Override
        protected void populateLocalName() {
            if (this.closed) {
                return;
            }
            try {
                final long socket = this.getSocket();
                final long sa = Address.get(0, socket);
                this.localName = Address.getnameinfo(sa, 0);
            }
            catch (Exception e) {
                AprEndpoint.log.warn((Object)AprSocketWrapper.sm.getString("endpoint.warn.noLocalName"), (Throwable)e);
            }
        }
        
        @Override
        protected void populateLocalAddr() {
            if (this.closed) {
                return;
            }
            try {
                final long socket = this.getSocket();
                final long sa = Address.get(0, socket);
                this.localAddr = Address.getip(sa);
            }
            catch (Exception e) {
                AprEndpoint.log.warn((Object)AprSocketWrapper.sm.getString("endpoint.warn.noLocalAddr"), (Throwable)e);
            }
        }
        
        @Override
        protected void populateLocalPort() {
            if (this.closed) {
                return;
            }
            try {
                final long socket = this.getSocket();
                final long sa = Address.get(0, socket);
                final Sockaddr addr = Address.getInfo(sa);
                this.localPort = addr.port;
            }
            catch (Exception e) {
                AprEndpoint.log.warn((Object)AprSocketWrapper.sm.getString("endpoint.warn.noLocalPort"), (Throwable)e);
            }
        }
        
        @Override
        public SSLSupport getSslSupport(final String clientCertProvider) {
            if (this.getEndpoint().isSSLEnabled()) {
                return new AprSSLSupport(this, clientCertProvider);
            }
            return null;
        }
        
        @Override
        public void doClientAuth(final SSLSupport sslSupport) {
            final long socket = this.getSocket();
            SSLSocket.setVerify(socket, 2, -1);
            SSLSocket.renegotiate(socket);
        }
    }
}
