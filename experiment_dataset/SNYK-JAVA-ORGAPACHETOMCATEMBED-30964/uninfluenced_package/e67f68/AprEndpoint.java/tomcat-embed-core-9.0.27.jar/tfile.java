// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.tomcat.util.net;

import java.util.concurrent.Semaphore;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.TimeUnit;
import org.apache.tomcat.util.buf.ByteBufferUtils;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.Lock;
import java.io.EOFException;
import java.nio.ByteBuffer;
import org.apache.tomcat.jni.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.juli.logging.LogFactory;
import java.util.concurrent.RejectedExecutionException;
import org.apache.tomcat.jni.Status;
import org.apache.tomcat.jni.Poll;
import org.apache.tomcat.util.ExceptionUtils;
import java.nio.charset.StandardCharsets;
import org.apache.tomcat.jni.SSL;
import org.apache.tomcat.jni.SSLSocket;
import org.apache.tomcat.util.collections.SynchronizedStack;
import javax.net.ssl.KeyManager;
import java.util.Set;
import org.apache.tomcat.util.net.openssl.OpenSSLContext;
import org.apache.tomcat.util.net.openssl.OpenSSLUtil;
import java.util.Iterator;
import org.apache.tomcat.jni.Error;
import org.apache.tomcat.jni.Socket;
import org.apache.tomcat.jni.OS;
import org.apache.tomcat.jni.Library;
import org.apache.tomcat.jni.Pool;
import org.apache.tomcat.jni.Sockaddr;
import java.io.IOException;
import org.apache.tomcat.jni.Address;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import org.apache.juli.logging.Log;
import org.apache.tomcat.jni.SSLContext;

public class AprEndpoint extends AbstractEndpoint<Long, Long> implements SSLContext.SNICallBack
{
    private static final Log log;
    protected long rootPool;
    protected volatile long serverSock;
    protected long serverSockPool;
    protected long sslContext;
    private final Map<Long, AprSocketWrapper> connections;
    protected boolean deferAccept;
    private boolean ipv6v6only;
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
        this.ipv6v6only = false;
        this.sendfileSize = 1024;
        this.pollTime = 2000;
        this.useSendFileSet = false;
        this.poller = null;
        this.sendfile = null;
        this.setMaxConnections(8192);
        this.setUseAsyncIO(false);
    }
    
    public void setDeferAccept(final boolean deferAccept) {
        this.deferAccept = deferAccept;
    }
    
    public boolean getDeferAccept() {
        return this.deferAccept;
    }
    
    public void setIpv6v6only(final boolean ipv6v6only) {
        this.ipv6v6only = ipv6v6only;
    }
    
    public boolean getIpv6v6only() {
        return this.ipv6v6only;
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
    
    public InetSocketAddress getLocalAddress() throws IOException {
        final long s = this.serverSock;
        if (s == 0L) {
            return null;
        }
        long sa;
        try {
            sa = Address.get(0, s);
        }
        catch (IOException ioe) {
            throw ioe;
        }
        catch (Exception e) {
            throw new IOException(e);
        }
        final Sockaddr addr = Address.getInfo(sa);
        if (addr.hostname != null) {
            return new InetSocketAddress(addr.hostname, addr.port);
        }
        if (addr.family == 2) {
            return new InetSocketAddress("::", addr.port);
        }
        return new InetSocketAddress("0.0.0.0", addr.port);
    }
    
    @Override
    public void setMaxConnections(final int maxConnections) {
        if (maxConnections == -1) {
            AprEndpoint.log.warn(AprEndpoint.sm.getString("endpoint.apr.maxConnections.unlimited", this.getMaxConnections()));
            return;
        }
        if (this.running) {
            AprEndpoint.log.warn(AprEndpoint.sm.getString("endpoint.apr.maxConnections.running", this.getMaxConnections()));
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
        catch (UnsatisfiedLinkError e) {
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
                if (!OS.IS_BSD) {
                    family = 0;
                }
            }
            else if (addressStr.indexOf(58) >= 0) {
                family = 0;
            }
        }
        final long inetAddress = Address.info(addressStr, family, this.getPortWithOffset(), 0, this.rootPool);
        this.serverSock = Socket.create(Address.getInfo(inetAddress).family, 0, 6, this.rootPool);
        if (OS.IS_UNIX) {
            Socket.optSet(this.serverSock, 16, 1);
        }
        if (Library.APR_HAVE_IPV6) {
            if (this.getIpv6v6only()) {
                Socket.optSet(this.serverSock, 16384, 1);
            }
            else {
                Socket.optSet(this.serverSock, 16384, 0);
            }
        }
        Socket.optSet(this.serverSock, 2, 1);
        int ret = Socket.bind(this.serverSock, inetAddress);
        if (ret != 0) {
            throw new Exception(AprEndpoint.sm.getString("endpoint.init.bind", "" + ret, Error.strerror(ret)));
        }
        ret = Socket.listen(this.serverSock, this.getAcceptCount());
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
        if (this.deferAccept && Socket.optSet(this.serverSock, 32768, 1) == 70023) {
            this.deferAccept = false;
        }
        if (this.isSSLEnabled()) {
            for (final SSLHostConfig sslHostConfig : this.sslHostConfigs.values()) {
                this.createSSLContext(sslHostConfig);
            }
            final SSLHostConfig defaultSSLHostConfig = this.sslHostConfigs.get(this.getDefaultSSLHostConfigName());
            if (defaultSSLHostConfig == null) {
                throw new IllegalArgumentException(AprEndpoint.sm.getString("endpoint.noSslHostConfig", this.getDefaultSSLHostConfigName(), this.getName()));
            }
            final Long defaultSSLContext = defaultSSLHostConfig.getOpenSslContext();
            this.sslContext = defaultSSLContext;
            SSLContext.registerDefault(defaultSSLContext, this);
            if (this.getUseSendfile()) {
                this.setUseSendfileInternal(false);
                if (this.useSendFileSet) {
                    AprEndpoint.log.warn(AprEndpoint.sm.getString("endpoint.apr.noSendfileWithSSL"));
                }
            }
        }
    }
    
    @Override
    protected void createSSLContext(final SSLHostConfig sslHostConfig) throws Exception {
        OpenSSLContext sslContext = null;
        final Set<SSLHostConfigCertificate> certificates = sslHostConfig.getCertificates(true);
        for (final SSLHostConfigCertificate certificate : certificates) {
            if (sslContext == null) {
                final SSLUtil sslUtil = new OpenSSLUtil(certificate);
                sslHostConfig.setEnabledProtocols(sslUtil.getEnabledProtocols());
                sslHostConfig.setEnabledCiphers(sslUtil.getEnabledCiphers());
                try {
                    sslContext = (OpenSSLContext)sslUtil.createSSLContext(this.negotiableProtocols);
                }
                catch (Exception e) {
                    throw new IllegalArgumentException(e.getMessage(), e);
                }
            }
            else {
                final SSLUtil sslUtil = new OpenSSLUtil(certificate);
                final KeyManager[] kms = sslUtil.getKeyManagers();
                certificate.setCertificateKeyManager(OpenSSLUtil.chooseKeyManager(kms));
                sslContext.addCertificate(certificate);
            }
            certificate.setSslContext(sslContext);
        }
        if (certificates.size() > 2) {
            throw new Exception(AprEndpoint.sm.getString("endpoint.apr.tooManyCertFiles"));
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
    public boolean isAlpnSupported() {
        return this.isSSLEnabled();
    }
    
    @Override
    public void startInternal() throws Exception {
        if (!this.running) {
            this.running = true;
            this.paused = false;
            if (this.socketProperties.getProcessorCache() != 0) {
                this.processorCache = new SynchronizedStack<SocketProcessorBase<S>>(128, this.socketProperties.getProcessorCache());
            }
            if (this.getExecutor() == null) {
                this.createExecutor();
            }
            this.initializeConnectionLatch();
            (this.poller = new Poller()).init();
            this.poller.start();
            if (this.getUseSendfile()) {
                (this.sendfile = new Sendfile()).init();
                this.sendfile.start();
            }
            this.startAcceptorThread();
        }
    }
    
    @Override
    public void stopInternal() {
        if (!this.paused) {
            this.pause();
        }
        if (this.running) {
            this.running = false;
            this.poller.stop();
            for (final SocketWrapperBase<Long> socketWrapper : this.connections.values()) {
                socketWrapper.close();
            }
            long waitLeft;
            for (waitLeft = 10000L; waitLeft > 0L && this.acceptor.getState() != Acceptor.AcceptorState.ENDED && this.serverSock != 0L; waitLeft -= 50L) {
                try {
                    Thread.sleep(50L);
                }
                catch (InterruptedException ex) {}
            }
            if (waitLeft == 0L) {
                AprEndpoint.log.warn(AprEndpoint.sm.getString("endpoint.warn.unlockAcceptorFailed", this.acceptor.getThreadName()));
                if (this.serverSock != 0L) {
                    Socket.shutdown(this.serverSock, 0);
                    this.serverSock = 0L;
                }
            }
            for (final Long s : this.connections.keySet()) {
                Socket.shutdown(s, 2);
            }
            try {
                this.poller.destroy();
            }
            catch (Exception ex2) {}
            this.poller = null;
            this.connections.clear();
            if (this.getUseSendfile()) {
                try {
                    this.sendfile.stop();
                    this.sendfile.destroy();
                }
                catch (Exception ex3) {}
                this.sendfile = null;
            }
            if (this.processorCache != null) {
                this.processorCache.clear();
                this.processorCache = null;
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
        this.doCloseServerSocket();
        this.destroySsl();
        if (this.rootPool != 0L) {
            Pool.destroy(this.rootPool);
            this.rootPool = 0L;
        }
        ((AbstractEndpoint<Long, U>)this).getHandler().recycle();
    }
    
    @Override
    protected void doCloseServerSocket() {
        if (this.serverSock != 0L) {
            Socket.close(this.serverSock);
            this.serverSock = 0L;
        }
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
                        AprEndpoint.log.debug(AprEndpoint.sm.getString("endpoint.err.handshake") + ": " + SSL.getLastError());
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
                            AprEndpoint.log.debug(AprEndpoint.sm.getString("endpoint.alpn.negotiated", negotiatedProtocol));
                        }
                    }
                }
            }
        }
        catch (Throwable t) {
            ExceptionUtils.handleThrowable(t);
            if (AprEndpoint.log.isDebugEnabled()) {
                if (step == 2) {
                    AprEndpoint.log.debug(AprEndpoint.sm.getString("endpoint.err.handshake"), t);
                }
                else {
                    AprEndpoint.log.debug(AprEndpoint.sm.getString("endpoint.err.unexpected"), t);
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
                AprEndpoint.log.info(AprEndpoint.sm.getString("endpoint.poll.limitedpollsize", "" + size));
                return 0L;
            }
            AprEndpoint.log.error(AprEndpoint.sm.getString("endpoint.poll.initfail"), e);
            return -1L;
        }
    }
    
    @Override
    protected boolean setSocketOptions(final Long socket) {
        try {
            if (AprEndpoint.log.isDebugEnabled()) {
                AprEndpoint.log.debug(AprEndpoint.sm.getString("endpoint.debug.socket", socket));
            }
            final AprSocketWrapper wrapper = new AprSocketWrapper(socket, this);
            wrapper.setKeepAliveLeft(this.getMaxKeepAliveRequests());
            wrapper.setSecure(this.isSSLEnabled());
            wrapper.setReadTimeout(this.getConnectionTimeout());
            wrapper.setWriteTimeout(this.getConnectionTimeout());
            this.connections.put(socket, wrapper);
            this.getExecutor().execute(new SocketWithOptionsProcessor(wrapper));
            return true;
        }
        catch (RejectedExecutionException x) {
            AprEndpoint.log.warn(AprEndpoint.sm.getString("endpoint.rejectedExecution", socket), x);
        }
        catch (Throwable t) {
            ExceptionUtils.handleThrowable(t);
            AprEndpoint.log.error(AprEndpoint.sm.getString("endpoint.process.fail"), t);
        }
        return false;
    }
    
    @Override
    protected Long serverSocketAccept() throws Exception {
        final long socket = Socket.accept(this.serverSock);
        if (AprEndpoint.log.isDebugEnabled()) {
            final long sa = Address.get(1, socket);
            final Sockaddr addr = Address.getInfo(sa);
            AprEndpoint.log.debug(AprEndpoint.sm.getString("endpoint.apr.remoteport", socket, addr.port));
        }
        return socket;
    }
    
    protected boolean processSocket(final long socket, final SocketEvent event) {
        final AprSocketWrapper socketWrapper = this.connections.get(socket);
        if (event == SocketEvent.OPEN_READ && socketWrapper.readOperation != null) {
            return socketWrapper.readOperation.process();
        }
        if (event == SocketEvent.OPEN_WRITE && socketWrapper.writeOperation != null) {
            return socketWrapper.writeOperation.process();
        }
        return ((AbstractEndpoint<Long, U>)this).processSocket(socketWrapper, event, true);
    }
    
    @Override
    protected SocketProcessorBase<Long> createSocketProcessor(final SocketWrapperBase<Long> socketWrapper, final SocketEvent event) {
        return new SocketProcessor(socketWrapper, event);
    }
    
    @Override
    protected void closeSocket(final Long socket) {
        this.closeSocket((long)socket);
    }
    
    private void closeSocket(final long socket) {
        final SocketWrapperBase<Long> wrapper = this.connections.remove(socket);
        if (wrapper != null) {
            ((AprSocketWrapper)wrapper).close();
        }
    }
    
    @Override
    protected void destroySocket(final Long socket) {
        this.destroySocket((long)socket);
    }
    
    private void destroySocket(final long socket) {
        this.connections.remove(socket);
        if (AprEndpoint.log.isDebugEnabled()) {
            final String msg = AprEndpoint.sm.getString("endpoint.debug.destroySocket", socket);
            if (AprEndpoint.log.isTraceEnabled()) {
                AprEndpoint.log.trace(msg, new Exception());
            }
            else {
                AprEndpoint.log.debug(msg);
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
        log = LogFactory.getLog(AprEndpoint.class);
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
        private long aprPoller;
        private int pollerSize;
        private long pool;
        private long[] desc;
        private SocketList addList;
        private SocketList closeList;
        private SocketTimeouts timeouts;
        private long lastMaintain;
        private AtomicInteger connectionCount;
        private volatile Thread pollerThread;
        private volatile boolean pollerRunning;
        
        public Poller() {
            this.pollerSize = 0;
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
            this.pollerSize = AprEndpoint.this.getMaxConnections();
            this.timeouts = new SocketTimeouts(this.pollerSize);
            this.aprPoller = AprEndpoint.this.allocatePoller(this.pollerSize, this.pool, -1);
            this.desc = new long[this.pollerSize * 4];
            this.connectionCount.set(0);
            this.addList = new SocketList(this.pollerSize);
            this.closeList = new SocketList(this.pollerSize);
        }
        
        protected void start() {
            (this.pollerThread = new Thread(AprEndpoint.this.poller, AprEndpoint.this.getName() + "-Poller")).setPriority(AprEndpoint.this.threadPriority);
            this.pollerThread.setDaemon(true);
            this.pollerThread.start();
        }
        
        protected synchronized void stop() {
            this.pollerRunning = false;
            this.notify();
        }
        
        protected synchronized void destroy() {
            for (int loops = 50; loops > 0 && this.pollerThread.isAlive(); --loops) {
                try {
                    this.wait(AprEndpoint.this.pollTime / 1000);
                }
                catch (InterruptedException ex) {}
            }
            if (this.pollerThread.isAlive()) {
                AprEndpoint.log.warn(AbstractEndpoint.sm.getString("endpoint.pollerThreadStop"));
            }
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
            final int rv = Poll.pollset(this.aprPoller, this.desc);
            if (rv > 0) {
                for (int n = 0; n < rv; ++n) {
                    AprEndpoint.this.destroySocket(this.desc[n * 2 + 1]);
                }
            }
            Pool.destroy(this.pool);
            this.connectionCount.set(0);
        }
        
        private void add(final long socket, long timeout, final int flags) {
            if (AprEndpoint.log.isDebugEnabled()) {
                final String msg = AbstractEndpoint.sm.getString("endpoint.debug.pollerAdd", socket, timeout, flags);
                if (AprEndpoint.log.isTraceEnabled()) {
                    AprEndpoint.log.trace(msg, new Exception());
                }
                else {
                    AprEndpoint.log.debug(msg);
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
            final int rv = Poll.add(this.aprPoller, socket, events);
            if (rv == 0) {
                this.connectionCount.incrementAndGet();
                return true;
            }
            return false;
        }
        
        private synchronized void close(final long socket) {
            this.closeList.add(socket, 0L, 0);
            this.notify();
        }
        
        private void removeFromPoller(final long socket) {
            if (AprEndpoint.log.isDebugEnabled()) {
                AprEndpoint.log.debug(AbstractEndpoint.sm.getString("endpoint.debug.pollerRemove", socket));
            }
            final int rv = Poll.remove(this.aprPoller, socket);
            if (rv != 70015) {
                this.connectionCount.decrementAndGet();
                if (AprEndpoint.log.isDebugEnabled()) {
                    AprEndpoint.log.debug(AbstractEndpoint.sm.getString("endpoint.debug.pollerRemoved", socket));
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
                    AprEndpoint.log.debug(AbstractEndpoint.sm.getString("endpoint.debug.socketTimeout", socket));
                }
                final AprSocketWrapper socketWrapper = AprEndpoint.this.connections.get(socket);
                if (socketWrapper != null) {
                    socketWrapper.setError(new SocketTimeoutException());
                    if (socketWrapper.readOperation != null || socketWrapper.writeOperation != null) {
                        if (socketWrapper.readOperation != null) {
                            socketWrapper.readOperation.process();
                        }
                        else {
                            socketWrapper.writeOperation.process();
                        }
                    }
                    else {
                        ((AbstractEndpoint<Long, U>)AprEndpoint.this).processSocket(socketWrapper, SocketEvent.ERROR, true);
                    }
                }
            }
        }
        
        @Override
        public String toString() {
            final StringBuffer buf = new StringBuffer();
            buf.append("Poller");
            final long[] res = new long[this.pollerSize * 2];
            final int count = Poll.pollset(this.aprPoller, res);
            buf.append(" [ ");
            for (int j = 0; j < count; ++j) {
                buf.append(this.desc[2 * j + 1]).append(" ");
            }
            buf.append("]");
            return buf.toString();
        }
        
        @Override
        public void run() {
            final SocketList localAddList = new SocketList(AprEndpoint.this.getMaxConnections());
            final SocketList localCloseList = new SocketList(AprEndpoint.this.getMaxConnections());
            while (this.pollerRunning) {
                while (this.pollerRunning && this.connectionCount.get() < 1 && this.addList.size() < 1 && this.closeList.size() < 1) {
                    try {
                        if (AprEndpoint.this.getConnectionTimeout() > 0 && this.pollerRunning) {
                            this.maintain();
                        }
                        synchronized (this) {
                            if (!this.pollerRunning || this.addList.size() >= 1 || this.closeList.size() >= 1) {
                                continue;
                            }
                            this.wait(10000L);
                        }
                    }
                    catch (InterruptedException ex) {}
                    catch (Throwable t) {
                        ExceptionUtils.handleThrowable(t);
                        AprEndpoint.this.getLog().warn(AbstractEndpoint.sm.getString("endpoint.timeout.err"));
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
                        for (SocketInfo info = localAddList.get(); info != null; info = localAddList.get()) {
                            if (AprEndpoint.log.isDebugEnabled()) {
                                AprEndpoint.log.debug(AbstractEndpoint.sm.getString("endpoint.debug.pollerAddDo", info.socket));
                            }
                            this.timeouts.remove(info.socket);
                            final AprSocketWrapper wrapper = AprEndpoint.this.connections.get(info.socket);
                            if (wrapper != null) {
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
                                    AprEndpoint.this.getLog().warn(AbstractEndpoint.sm.getString("endpoint.apr.pollAddInvalid", info));
                                }
                            }
                        }
                    }
                    boolean reset = false;
                    int rv = Poll.poll(this.aprPoller, AprEndpoint.this.pollTime, this.desc, true);
                    if (rv > 0) {
                        rv = this.mergeDescriptors(this.desc, rv);
                        this.connectionCount.addAndGet(-rv);
                        for (int n = 0; n < rv; ++n) {
                            if (AprEndpoint.this.getLog().isDebugEnabled()) {
                                AprEndpoint.log.debug(AbstractEndpoint.sm.getString("endpoint.debug.pollerProcess", this.desc[n * 2 + 1], this.desc[n * 2]));
                            }
                            long timeout = this.timeouts.remove(this.desc[n * 2 + 1]);
                            final AprSocketWrapper wrapper2 = AprEndpoint.this.connections.get(this.desc[n * 2 + 1]);
                            if (wrapper2 != null) {
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
                                    AprEndpoint.this.getLog().warn(AbstractEndpoint.sm.getString("endpoint.apr.pollUnknownEvent", this.desc[n * 2]));
                                    AprEndpoint.this.closeSocket(this.desc[n * 2 + 1]);
                                }
                            }
                        }
                    }
                    else if (rv < 0) {
                        int errn = -rv;
                        if (errn != 120001 && errn != 120003) {
                            if (errn > 120000) {
                                errn -= 120000;
                            }
                            AprEndpoint.this.getLog().error(AbstractEndpoint.sm.getString("endpoint.apr.pollError", errn, Error.strerror(errn)));
                            reset = true;
                        }
                    }
                    if (reset && this.pollerRunning) {
                        final int count = Poll.pollset(this.aprPoller, this.desc);
                        final long newPoller = AprEndpoint.this.allocatePoller(this.pollerSize, this.pool, -1);
                        this.connectionCount.addAndGet(-count);
                        Poll.destroy(this.aprPoller);
                        this.aprPoller = newPoller;
                    }
                }
                catch (Throwable t) {
                    ExceptionUtils.handleThrowable(t);
                    AprEndpoint.this.getLog().warn(AbstractEndpoint.sm.getString("endpoint.poll.error"), t);
                }
                try {
                    if (AprEndpoint.this.getConnectionTimeout() <= 0 || !this.pollerRunning) {
                        continue;
                    }
                    this.maintain();
                }
                catch (Throwable t) {
                    ExceptionUtils.handleThrowable(t);
                    AprEndpoint.this.getLog().warn(AbstractEndpoint.sm.getString("endpoint.timeout.err"), t);
                }
            }
            synchronized (this) {
                this.notifyAll();
            }
        }
        
        private int mergeDescriptors(final long[] desc, final int startCount) {
            final Map<Long, Long> merged = new HashMap<Long, Long>(startCount);
            for (int n = 0; n < startCount; ++n) {
                final Long newValue = merged.merge(desc[2 * n + 1], desc[2 * n], (v1, v2) -> (long)v1 | (long)v2);
                if (AprEndpoint.log.isDebugEnabled() && newValue != desc[2 * n]) {
                    AprEndpoint.log.debug(AbstractEndpoint.sm.getString("endpoint.apr.pollMergeEvents", desc[2 * n + 1], desc[2 * n], newValue));
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
        private volatile Thread sendfileThread;
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
                size = 16384;
            }
            this.sendfilePollset = AprEndpoint.this.allocatePoller(size, this.pool, AprEndpoint.this.getConnectionTimeout());
            this.desc = new long[size * 2];
            this.sendfileData = new HashMap<Long, SendfileData>(size);
            this.addS = new ArrayList<SendfileData>();
        }
        
        protected void start() {
            (this.sendfileThread = new Thread(AprEndpoint.this.sendfile, AprEndpoint.this.getName() + "-Sendfile")).setPriority(AprEndpoint.this.threadPriority);
            this.sendfileThread.setDaemon(true);
            this.sendfileThread.start();
        }
        
        protected synchronized void stop() {
            this.sendfileRunning = false;
            this.notify();
            for (int loops = 50; loops > 0 && this.sendfileThread.isAlive(); --loops) {
                try {
                    this.wait(AprEndpoint.this.pollTime / 1000);
                }
                catch (InterruptedException ex) {}
            }
            if (this.sendfileThread.isAlive()) {
                AprEndpoint.log.warn(AbstractEndpoint.sm.getString("endpoint.sendfileThreadStop"));
            }
        }
        
        protected void destroy() {
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
                while (this.sendfileRunning) {
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
                            Socket.timeoutSet(data.socket, AprEndpoint.this.getConnectionTimeout() * 1000);
                            return SendfileState.DONE;
                        }
                        continue;
                    }
                }
            }
            catch (Exception e) {
                AprEndpoint.log.warn(AbstractEndpoint.sm.getString("endpoint.sendfile.error"), e);
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
                        Thread.sleep(AprEndpoint.this.pollTime / 1000);
                    }
                    catch (InterruptedException ex) {}
                }
                while (this.sendfileRunning && this.sendfileCount < 1 && this.addS.size() < 1) {
                    maintainTime = 0L;
                    try {
                        synchronized (this) {
                            if (!this.sendfileRunning || this.sendfileCount >= 1 || this.addS.size() >= 1) {
                                continue;
                            }
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
                                    AprEndpoint.this.getLog().warn(AbstractEndpoint.sm.getString("endpoint.sendfile.addfail", rv, Error.strerror(rv)));
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
                                        switch (state.keepAliveState) {
                                            case NONE: {
                                                AprEndpoint.this.closeSocket(state.socket);
                                                break;
                                            }
                                            case PIPELINED: {
                                                Pool.destroy(state.fdpool);
                                                Socket.timeoutSet(state.socket, AprEndpoint.this.getConnectionTimeout() * 1000);
                                                if (!AprEndpoint.this.processSocket(state.socket, SocketEvent.OPEN_READ)) {
                                                    AprEndpoint.this.closeSocket(state.socket);
                                                    break;
                                                }
                                                break;
                                            }
                                            case OPEN: {
                                                Pool.destroy(state.fdpool);
                                                Socket.timeoutSet(state.socket, AprEndpoint.this.getConnectionTimeout() * 1000);
                                                AprEndpoint.this.getPoller().add(state.socket, AprEndpoint.this.getKeepAliveTimeout(), 1);
                                                break;
                                            }
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
                            AprEndpoint.this.getLog().error(AbstractEndpoint.sm.getString("endpoint.apr.pollError", errn, Error.strerror(errn)));
                            synchronized (this) {
                                this.destroy();
                                this.init();
                            }
                            continue;
                        }
                    }
                    if (AprEndpoint.this.getConnectionTimeout() <= 0 || maintainTime <= 1000000L || !this.sendfileRunning) {
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
                    AprEndpoint.this.getLog().error(AbstractEndpoint.sm.getString("endpoint.poll.error"), t);
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
                        AprEndpoint.this.getPoller().add(this.socket.getSocket(), AprEndpoint.this.getConnectionTimeout(), 1);
                    }
                    else {
                        ((AbstractEndpoint<Long, U>)AprEndpoint.this).getHandler().process(this.socket, SocketEvent.CONNECT_FAIL);
                        AprEndpoint.this.closeSocket(this.socket.getSocket());
                        this.socket = null;
                    }
                }
                else {
                    if (!AprEndpoint.this.setSocketOptions(this.socket)) {
                        ((AbstractEndpoint<Long, U>)AprEndpoint.this).getHandler().process(this.socket, SocketEvent.CONNECT_FAIL);
                        AprEndpoint.this.closeSocket(this.socket.getSocket());
                        this.socket = null;
                        return;
                    }
                    final Handler.SocketState state = ((AbstractEndpoint<Long, U>)AprEndpoint.this).getHandler().process(this.socket, SocketEvent.OPEN_READ);
                    if (state == Handler.SocketState.CLOSED) {
                        AprEndpoint.this.closeSocket(this.socket.getSocket());
                        this.socket = null;
                    }
                }
            }
        }
    }
    
    protected class SocketProcessor extends SocketProcessorBase<Long>
    {
        public SocketProcessor(final SocketWrapperBase<Long> socketWrapper, final SocketEvent event) {
            super(socketWrapper, event);
        }
        
        @Override
        protected void doRun() {
            try {
                final Handler.SocketState state = ((AbstractEndpoint<Long, U>)AprEndpoint.this).getHandler().process((SocketWrapperBase<Long>)this.socketWrapper, this.event);
                if (state == Handler.SocketState.CLOSED) {
                    AprEndpoint.this.closeSocket((long)this.socketWrapper.getSocket());
                }
            }
            finally {
                this.socketWrapper = null;
                this.event = null;
                if (AprEndpoint.this.running && !AprEndpoint.this.paused && AprEndpoint.this.processorCache != null) {
                    AprEndpoint.this.processorCache.push((SocketProcessorBase<S>)this);
                }
            }
        }
    }
    
    public static class AprSocketWrapper extends SocketWrapperBase<Long>
    {
        private static final int SSL_OUTPUT_BUFFER_SIZE = 8192;
        private final ByteBuffer sslOutputBuffer;
        private int pollerFlags;
        
        public AprSocketWrapper(final Long socket, final AprEndpoint endpoint) {
            super(socket, endpoint);
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
            int nRead = this.populateReadBuffer(b, off, len);
            if (nRead > 0) {
                return nRead;
            }
            nRead = this.fillReadBuffer(block);
            if (nRead > 0) {
                this.socketBufferHandler.configureReadBufferForRead();
                nRead = Math.min(nRead, len);
                this.socketBufferHandler.getReadBuffer().get(b, off, nRead);
            }
            return nRead;
        }
        
        @Override
        public int read(final boolean block, final ByteBuffer to) throws IOException {
            int nRead = this.populateReadBuffer(to);
            if (nRead > 0) {
                return nRead;
            }
            final int limit = this.socketBufferHandler.getReadBuffer().capacity();
            if (to.isDirect() && to.remaining() >= limit) {
                to.limit(to.position() + limit);
                nRead = this.fillReadBuffer(block, to);
                if (AprEndpoint.log.isDebugEnabled()) {
                    AprEndpoint.log.debug("Socket: [" + this + "], Read direct from socket: [" + nRead + "]");
                }
            }
            else {
                nRead = this.fillReadBuffer(block);
                if (AprEndpoint.log.isDebugEnabled()) {
                    AprEndpoint.log.debug("Socket: [" + this + "], Read into buffer: [" + nRead + "]");
                }
                if (nRead > 0) {
                    nRead = this.populateReadBuffer(to);
                }
            }
            return nRead;
        }
        
        private int fillReadBuffer(final boolean block) throws IOException {
            this.socketBufferHandler.configureReadBufferForWrite();
            return this.fillReadBuffer(block, this.socketBufferHandler.getReadBuffer());
        }
        
        private int fillReadBuffer(final boolean block, final ByteBuffer to) throws IOException {
            final Lock readLock = this.getBlockingStatusReadLock();
            final ReentrantReadWriteLock.WriteLock writeLock = this.getBlockingStatusWriteLock();
            boolean readDone = false;
            int result = 0;
            readLock.lock();
            try {
                this.checkClosed();
                if (this.getBlockingStatus() == block) {
                    if (block) {
                        Socket.timeoutSet(this.getSocket(), this.getReadTimeout() * 1000L);
                    }
                    result = Socket.recvb(this.getSocket(), to, to.position(), to.remaining());
                    readDone = true;
                }
            }
            finally {
                readLock.unlock();
            }
            if (!readDone) {
                writeLock.lock();
                try {
                    this.checkClosed();
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
                        result = Socket.recvb(this.getSocket(), to, to.position(), to.remaining());
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
                to.position(to.position() + result);
                return result;
            }
            if (result == 0 || -result == 120002) {
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
            final int read = this.fillReadBuffer(false);
            final boolean isReady = this.socketBufferHandler.getReadBuffer().position() > 0 || read == -1;
            return isReady;
        }
        
        private void checkClosed() throws IOException {
            if (this.isClosed()) {
                throw new IOException(AprSocketWrapper.sm.getString("socket.apr.closed", ((SocketWrapperBase<Object>)this).getSocket()));
            }
        }
        
        @Override
        protected void doClose() {
            if (AprEndpoint.log.isDebugEnabled()) {
                AprEndpoint.log.debug("Calling [" + this.getEndpoint() + "].closeSocket([" + this + "])", new Exception());
            }
            this.socketBufferHandler = SocketBufferHandler.EMPTY;
            this.nonBlockingWriteBuffer.clear();
            synchronized (this.closed) {
                if (this.sslOutputBuffer != null) {
                    ByteBufferUtils.cleanDirectBuffer(this.sslOutputBuffer);
                }
                ((AprEndpoint)this.getEndpoint()).getPoller().close(this.getSocket());
            }
        }
        
        @Override
        protected void writeBlockingDirect(final ByteBuffer from) throws IOException {
            if (from.isDirect()) {
                super.writeBlockingDirect(from);
            }
            else {
                final ByteBuffer writeBuffer = this.socketBufferHandler.getWriteBuffer();
                final int limit = writeBuffer.capacity();
                while (from.remaining() >= limit) {
                    this.socketBufferHandler.configureWriteBufferForWrite();
                    SocketWrapperBase.transfer(from, writeBuffer);
                    this.doWrite(true);
                }
                if (from.remaining() > 0) {
                    this.socketBufferHandler.configureWriteBufferForWrite();
                    SocketWrapperBase.transfer(from, writeBuffer);
                }
            }
        }
        
        @Override
        protected void writeNonBlockingDirect(final ByteBuffer from) throws IOException {
            if (from.isDirect()) {
                super.writeNonBlockingDirect(from);
            }
            else {
                final ByteBuffer writeBuffer = this.socketBufferHandler.getWriteBuffer();
                final int limit = writeBuffer.capacity();
                while (from.remaining() >= limit) {
                    this.socketBufferHandler.configureWriteBufferForWrite();
                    SocketWrapperBase.transfer(from, writeBuffer);
                    final int newPosition = writeBuffer.position() + limit;
                    this.doWrite(false);
                    if (writeBuffer.position() != newPosition) {
                        return;
                    }
                }
                if (from.remaining() > 0) {
                    this.socketBufferHandler.configureWriteBufferForWrite();
                    SocketWrapperBase.transfer(from, writeBuffer);
                }
            }
        }
        
        @Override
        protected void doWrite(final boolean block, final ByteBuffer from) throws IOException {
            final Lock readLock = this.getBlockingStatusReadLock();
            final ReentrantReadWriteLock.WriteLock writeLock = this.getBlockingStatusWriteLock();
            readLock.lock();
            try {
                this.checkClosed();
                if (this.getBlockingStatus() == block) {
                    if (block) {
                        Socket.timeoutSet(this.getSocket(), this.getWriteTimeout() * 1000L);
                    }
                    this.doWriteInternal(from);
                    return;
                }
            }
            finally {
                readLock.unlock();
            }
            writeLock.lock();
            try {
                this.checkClosed();
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
                    this.doWriteInternal(from);
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
        
        private void doWriteInternal(final ByteBuffer from) throws IOException {
            int thisTime;
            do {
                thisTime = 0;
                if (this.getEndpoint().isSSLEnabled()) {
                    if (this.sslOutputBuffer.remaining() == 0) {
                        this.sslOutputBuffer.clear();
                        SocketWrapperBase.transfer(from, this.sslOutputBuffer);
                        this.sslOutputBuffer.flip();
                    }
                    thisTime = Socket.sendb(this.getSocket(), this.sslOutputBuffer, this.sslOutputBuffer.position(), this.sslOutputBuffer.limit());
                    if (thisTime > 0) {
                        this.sslOutputBuffer.position(this.sslOutputBuffer.position() + thisTime);
                    }
                }
                else {
                    thisTime = Socket.sendb(this.getSocket(), from, from.position(), from.remaining());
                    if (thisTime > 0) {
                        from.position(from.position() + thisTime);
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
            } while ((thisTime > 0 || this.getBlockingStatus()) && from.hasRemaining());
        }
        
        @Override
        public void registerReadInterest() {
            synchronized (this.closed) {
                if (this.isClosed()) {
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
            synchronized (this.closed) {
                if (this.isClosed()) {
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
            if (this.isClosed()) {
                return;
            }
            try {
                final long socket = this.getSocket();
                final long sa = Address.get(1, socket);
                this.remoteAddr = Address.getip(sa);
            }
            catch (Exception e) {
                AprEndpoint.log.warn(AprSocketWrapper.sm.getString("endpoint.warn.noRemoteAddr", ((SocketWrapperBase<Object>)this).getSocket()), e);
            }
        }
        
        @Override
        protected void populateRemoteHost() {
            if (this.isClosed()) {
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
                AprEndpoint.log.warn(AprSocketWrapper.sm.getString("endpoint.warn.noRemoteHost", ((SocketWrapperBase<Object>)this).getSocket()), e);
            }
        }
        
        @Override
        protected void populateRemotePort() {
            if (this.isClosed()) {
                return;
            }
            try {
                final long socket = this.getSocket();
                final long sa = Address.get(1, socket);
                final Sockaddr addr = Address.getInfo(sa);
                this.remotePort = addr.port;
            }
            catch (Exception e) {
                AprEndpoint.log.warn(AprSocketWrapper.sm.getString("endpoint.warn.noRemotePort", ((SocketWrapperBase<Object>)this).getSocket()), e);
            }
        }
        
        @Override
        protected void populateLocalName() {
            if (this.isClosed()) {
                return;
            }
            try {
                final long socket = this.getSocket();
                final long sa = Address.get(0, socket);
                this.localName = Address.getnameinfo(sa, 0);
            }
            catch (Exception e) {
                AprEndpoint.log.warn(AprSocketWrapper.sm.getString("endpoint.warn.noLocalName"), e);
            }
        }
        
        @Override
        protected void populateLocalAddr() {
            if (this.isClosed()) {
                return;
            }
            try {
                final long socket = this.getSocket();
                final long sa = Address.get(0, socket);
                this.localAddr = Address.getip(sa);
            }
            catch (Exception e) {
                AprEndpoint.log.warn(AprSocketWrapper.sm.getString("endpoint.warn.noLocalAddr"), e);
            }
        }
        
        @Override
        protected void populateLocalPort() {
            if (this.isClosed()) {
                return;
            }
            try {
                final long socket = this.getSocket();
                final long sa = Address.get(0, socket);
                final Sockaddr addr = Address.getInfo(sa);
                this.localPort = addr.port;
            }
            catch (Exception e) {
                AprEndpoint.log.warn(AprSocketWrapper.sm.getString("endpoint.warn.noLocalPort"), e);
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
        public void doClientAuth(final SSLSupport sslSupport) throws IOException {
            final long socket = this.getSocket();
            final Lock readLock = this.getBlockingStatusReadLock();
            final ReentrantReadWriteLock.WriteLock writeLock = this.getBlockingStatusWriteLock();
            boolean renegotiateDone = false;
            try {
                readLock.lock();
                try {
                    if (this.getBlockingStatus()) {
                        Socket.timeoutSet(this.getSocket(), this.getReadTimeout() * 1000L);
                        SSLSocket.setVerify(socket, 2, -1);
                        SSLSocket.renegotiate(socket);
                        renegotiateDone = true;
                    }
                }
                finally {
                    readLock.unlock();
                }
                if (!renegotiateDone) {
                    writeLock.lock();
                    try {
                        this.setBlockingStatus(true);
                        Socket.timeoutSet(this.getSocket(), this.getReadTimeout() * 1000L);
                        readLock.lock();
                        try {
                            writeLock.unlock();
                            SSLSocket.setVerify(socket, 2, -1);
                            SSLSocket.renegotiate(socket);
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
            }
            catch (Throwable t) {
                ExceptionUtils.handleThrowable(t);
                throw new IOException(AprSocketWrapper.sm.getString("socket.sslreneg"), t);
            }
        }
        
        @Override
        public void setAppReadBufHandler(final ApplicationBufferHandler handler) {
        }
        
        String getSSLInfoS(final int id) {
            synchronized (this.closed) {
                if (this.isClosed()) {
                    return null;
                }
                try {
                    return SSLSocket.getInfoS(this.getSocket(), id);
                }
                catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        
        int getSSLInfoI(final int id) {
            synchronized (this.closed) {
                if (this.isClosed()) {
                    return 0;
                }
                try {
                    return SSLSocket.getInfoI(this.getSocket(), id);
                }
                catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        
        byte[] getSSLInfoB(final int id) {
            synchronized (this.closed) {
                if (this.isClosed()) {
                    return null;
                }
                try {
                    return SSLSocket.getInfoB(this.getSocket(), id);
                }
                catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        
        @Override
        protected <A> OperationState<A> newOperationState(final boolean read, final ByteBuffer[] buffers, final int offset, final int length, final BlockingMode block, final long timeout, final TimeUnit unit, final A attachment, final CompletionCheck check, final CompletionHandler<Long, ? super A> handler, final Semaphore semaphore, final VectoredIOCompletionHandler<A> completion) {
            return new AprOperationState<A>(read, buffers, offset, length, block, timeout, unit, (Object)attachment, check, (CompletionHandler)handler, semaphore, (VectoredIOCompletionHandler)completion);
        }
        
        private class AprOperationState<A> extends OperationState<A>
        {
            private volatile boolean inline;
            private volatile long flushBytes;
            
            private AprOperationState(final boolean read, final ByteBuffer[] buffers, final int offset, final int length, final BlockingMode block, final long timeout, final TimeUnit unit, final A attachment, final CompletionCheck check, final CompletionHandler<Long, ? super A> handler, final Semaphore semaphore, final VectoredIOCompletionHandler<A> completion) {
                super(read, buffers, offset, length, block, timeout, unit, attachment, check, handler, semaphore, completion);
                this.inline = true;
                this.flushBytes = 0L;
            }
            
            @Override
            protected boolean isInline() {
                return this.inline;
            }
            
            @Override
            public void run() {
                long nBytes = 0L;
                if (AprSocketWrapper.this.getError() == null) {
                    try {
                        synchronized (this) {
                            if (!this.completionDone) {
                                if (AprEndpoint.log.isDebugEnabled()) {
                                    AprEndpoint.log.debug("Skip concurrent " + (this.read ? "read" : "write") + " notification");
                                }
                                return;
                            }
                            ByteBuffer buffer = null;
                            for (int i = 0; i < this.length; ++i) {
                                if (this.buffers[i + this.offset].hasRemaining()) {
                                    buffer = this.buffers[i + this.offset];
                                    break;
                                }
                            }
                            if (buffer == null && this.flushBytes == 0L) {
                                return;
                            }
                            if (this.read) {
                                nBytes = AprSocketWrapper.this.read(false, buffer);
                            }
                            else {
                                if (AprSocketWrapper.this.flush(this.block == BlockingMode.BLOCK)) {
                                    this.inline = false;
                                    AprSocketWrapper.this.registerWriteInterest();
                                    return;
                                }
                                if (this.flushBytes > 0L) {
                                    nBytes = this.flushBytes;
                                    this.flushBytes = 0L;
                                }
                                else {
                                    final int remaining = buffer.remaining();
                                    AprSocketWrapper.this.write(this.block == BlockingMode.BLOCK, buffer);
                                    nBytes = remaining - buffer.remaining();
                                    if (nBytes > 0L && AprSocketWrapper.this.flush(this.block == BlockingMode.BLOCK)) {
                                        this.inline = false;
                                        AprSocketWrapper.this.registerWriteInterest();
                                        this.flushBytes = nBytes;
                                        return;
                                    }
                                }
                            }
                            if (nBytes != 0L) {
                                this.completionDone = false;
                            }
                        }
                    }
                    catch (IOException e) {
                        AprSocketWrapper.this.setError(e);
                    }
                }
                if (nBytes > 0L) {
                    this.completion.completed(Long.valueOf(nBytes), (OperationState<A>)this);
                }
                else if (nBytes < 0L || AprSocketWrapper.this.getError() != null) {
                    IOException error = AprSocketWrapper.this.getError();
                    if (error == null) {
                        error = new EOFException();
                    }
                    this.completion.failed((Throwable)error, (OperationState<A>)this);
                }
                else {
                    this.inline = false;
                    if (this.read) {
                        AprSocketWrapper.this.registerReadInterest();
                    }
                    else {
                        AprSocketWrapper.this.registerWriteInterest();
                    }
                }
            }
        }
    }
}
