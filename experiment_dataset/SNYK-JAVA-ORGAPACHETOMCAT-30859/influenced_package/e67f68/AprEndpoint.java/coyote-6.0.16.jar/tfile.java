// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.tomcat.util.net;

import org.apache.tomcat.jni.File;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.jni.Status;
import org.apache.tomcat.jni.Poll;
import org.apache.tomcat.jni.SSL;
import org.apache.tomcat.jni.SSLSocket;
import org.apache.tomcat.jni.SSLContext;
import org.apache.tomcat.jni.Error;
import org.apache.tomcat.jni.Socket;
import org.apache.tomcat.jni.Address;
import org.apache.tomcat.jni.OS;
import org.apache.tomcat.jni.Pool;
import org.apache.tomcat.jni.Library;
import java.net.InetAddress;
import java.util.concurrent.Executor;
import org.apache.tomcat.util.res.StringManager;
import org.apache.juli.logging.Log;

public class AprEndpoint
{
    protected static Log log;
    protected static StringManager sm;
    public static final String CIPHER_SUITE_KEY = "javax.servlet.request.cipher_suite";
    public static final String KEY_SIZE_KEY = "javax.servlet.request.key_size";
    public static final String CERTIFICATE_KEY = "javax.servlet.request.X509Certificate";
    public static final String SESSION_ID_KEY = "javax.servlet.request.ssl_session";
    protected WorkerStack workers;
    protected volatile boolean running;
    protected volatile boolean paused;
    protected boolean initialized;
    protected int curThreadsBusy;
    protected int curThreads;
    protected int sequence;
    protected long rootPool;
    protected long serverSock;
    protected long serverSockPool;
    protected long sslContext;
    protected boolean deferAccept;
    protected Executor executor;
    protected int maxThreads;
    protected int threadPriority;
    protected int pollerSize;
    protected int sendfileSize;
    protected int port;
    protected InetAddress address;
    protected Handler handler;
    protected int backlog;
    protected boolean tcpNoDelay;
    protected int soLinger;
    protected int soTimeout;
    protected int keepAliveTimeout;
    protected int pollTime;
    protected boolean daemon;
    protected String name;
    protected boolean useSendfile;
    protected boolean useComet;
    protected int acceptorThreadCount;
    protected int sendfileThreadCount;
    protected int pollerThreadCount;
    protected Poller[] pollers;
    protected int pollerRoundRobin;
    protected Poller[] cometPollers;
    protected int cometPollerRoundRobin;
    protected Sendfile[] sendfiles;
    protected int sendfileRoundRobin;
    protected boolean SSLEnabled;
    protected String SSLProtocol;
    protected String SSLPassword;
    protected String SSLCipherSuite;
    protected String SSLCertificateFile;
    protected String SSLCertificateKeyFile;
    protected String SSLCertificateChainFile;
    protected String SSLCACertificatePath;
    protected String SSLCACertificateFile;
    protected String SSLCARevocationPath;
    protected String SSLCARevocationFile;
    protected String SSLVerifyClient;
    protected int SSLVerifyDepth;
    
    public AprEndpoint() {
        this.workers = null;
        this.running = false;
        this.paused = false;
        this.initialized = false;
        this.curThreadsBusy = 0;
        this.curThreads = 0;
        this.sequence = 0;
        this.rootPool = 0L;
        this.serverSock = 0L;
        this.serverSockPool = 0L;
        this.sslContext = 0L;
        this.deferAccept = true;
        this.executor = null;
        this.maxThreads = 40;
        this.threadPriority = 5;
        this.pollerSize = 8192;
        this.sendfileSize = 1024;
        this.handler = null;
        this.backlog = 100;
        this.tcpNoDelay = false;
        this.soLinger = 100;
        this.soTimeout = -1;
        this.keepAliveTimeout = -1;
        this.pollTime = 2000;
        this.daemon = true;
        this.name = "TP";
        this.useSendfile = Library.APR_HAS_SENDFILE;
        this.useComet = true;
        this.acceptorThreadCount = 0;
        this.sendfileThreadCount = 0;
        this.pollerThreadCount = 0;
        this.pollers = null;
        this.pollerRoundRobin = 0;
        this.cometPollers = null;
        this.cometPollerRoundRobin = 0;
        this.sendfiles = null;
        this.sendfileRoundRobin = 0;
        this.SSLEnabled = false;
        this.SSLProtocol = "all";
        this.SSLPassword = null;
        this.SSLCipherSuite = "ALL";
        this.SSLCertificateFile = null;
        this.SSLCertificateKeyFile = null;
        this.SSLCertificateChainFile = null;
        this.SSLCACertificatePath = null;
        this.SSLCACertificateFile = null;
        this.SSLCARevocationPath = null;
        this.SSLCARevocationFile = null;
        this.SSLVerifyClient = "none";
        this.SSLVerifyDepth = 10;
    }
    
    public void setExecutor(final Executor executor) {
        this.executor = executor;
    }
    
    public Executor getExecutor() {
        return this.executor;
    }
    
    public void setMaxThreads(final int maxThreads) {
        this.maxThreads = maxThreads;
    }
    
    public int getMaxThreads() {
        return this.maxThreads;
    }
    
    public void setThreadPriority(final int threadPriority) {
        this.threadPriority = threadPriority;
    }
    
    public int getThreadPriority() {
        return this.threadPriority;
    }
    
    public void setPollerSize(final int pollerSize) {
        this.pollerSize = pollerSize;
    }
    
    public int getPollerSize() {
        return this.pollerSize;
    }
    
    public void setSendfileSize(final int sendfileSize) {
        this.sendfileSize = sendfileSize;
    }
    
    public int getSendfileSize() {
        return this.sendfileSize;
    }
    
    public int getPort() {
        return this.port;
    }
    
    public void setPort(final int port) {
        this.port = port;
    }
    
    public InetAddress getAddress() {
        return this.address;
    }
    
    public void setAddress(final InetAddress address) {
        this.address = address;
    }
    
    public void setHandler(final Handler handler) {
        this.handler = handler;
    }
    
    public Handler getHandler() {
        return this.handler;
    }
    
    public void setBacklog(final int backlog) {
        if (backlog > 0) {
            this.backlog = backlog;
        }
    }
    
    public int getBacklog() {
        return this.backlog;
    }
    
    public boolean getTcpNoDelay() {
        return this.tcpNoDelay;
    }
    
    public void setTcpNoDelay(final boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
    }
    
    public int getSoLinger() {
        return this.soLinger;
    }
    
    public void setSoLinger(final int soLinger) {
        this.soLinger = soLinger;
    }
    
    public int getSoTimeout() {
        return this.soTimeout;
    }
    
    public void setSoTimeout(final int soTimeout) {
        this.soTimeout = soTimeout;
    }
    
    public int getKeepAliveTimeout() {
        return this.keepAliveTimeout;
    }
    
    public void setKeepAliveTimeout(final int keepAliveTimeout) {
        this.keepAliveTimeout = keepAliveTimeout;
    }
    
    public int getPollTime() {
        return this.pollTime;
    }
    
    public void setPollTime(final int pollTime) {
        if (pollTime > 0) {
            this.pollTime = pollTime;
        }
    }
    
    public void setDaemon(final boolean b) {
        this.daemon = b;
    }
    
    public boolean getDaemon() {
        return this.daemon;
    }
    
    public void setName(final String name) {
        this.name = name;
    }
    
    public String getName() {
        return this.name;
    }
    
    public void setUseSendfile(final boolean useSendfile) {
        this.useSendfile = useSendfile;
    }
    
    public boolean getUseSendfile() {
        return this.useSendfile;
    }
    
    public void setUseComet(final boolean useComet) {
        this.useComet = useComet;
    }
    
    public boolean getUseComet() {
        return this.useComet;
    }
    
    public void setAcceptorThreadCount(final int acceptorThreadCount) {
        this.acceptorThreadCount = acceptorThreadCount;
    }
    
    public int getAcceptorThreadCount() {
        return this.acceptorThreadCount;
    }
    
    public void setSendfileThreadCount(final int sendfileThreadCount) {
        this.sendfileThreadCount = sendfileThreadCount;
    }
    
    public int getSendfileThreadCount() {
        return this.sendfileThreadCount;
    }
    
    public void setPollerThreadCount(final int pollerThreadCount) {
        this.pollerThreadCount = pollerThreadCount;
    }
    
    public int getPollerThreadCount() {
        return this.pollerThreadCount;
    }
    
    public Poller getPoller() {
        this.pollerRoundRobin = (this.pollerRoundRobin + 1) % this.pollers.length;
        return this.pollers[this.pollerRoundRobin];
    }
    
    public Poller getCometPoller() {
        this.cometPollerRoundRobin = (this.cometPollerRoundRobin + 1) % this.cometPollers.length;
        return this.cometPollers[this.cometPollerRoundRobin];
    }
    
    public Sendfile getSendfile() {
        this.sendfileRoundRobin = (this.sendfileRoundRobin + 1) % this.sendfiles.length;
        return this.sendfiles[this.sendfileRoundRobin];
    }
    
    public int getMaxSpareThreads() {
        return 0;
    }
    
    public int getMinSpareThreads() {
        return 0;
    }
    
    public boolean isSSLEnabled() {
        return this.SSLEnabled;
    }
    
    public void setSSLEnabled(final boolean SSLEnabled) {
        this.SSLEnabled = SSLEnabled;
    }
    
    public String getSSLProtocol() {
        return this.SSLProtocol;
    }
    
    public void setSSLProtocol(final String SSLProtocol) {
        this.SSLProtocol = SSLProtocol;
    }
    
    public String getSSLPassword() {
        return this.SSLPassword;
    }
    
    public void setSSLPassword(final String SSLPassword) {
        this.SSLPassword = SSLPassword;
    }
    
    public String getSSLCipherSuite() {
        return this.SSLCipherSuite;
    }
    
    public void setSSLCipherSuite(final String SSLCipherSuite) {
        this.SSLCipherSuite = SSLCipherSuite;
    }
    
    public String getSSLCertificateFile() {
        return this.SSLCertificateFile;
    }
    
    public void setSSLCertificateFile(final String SSLCertificateFile) {
        this.SSLCertificateFile = SSLCertificateFile;
    }
    
    public String getSSLCertificateKeyFile() {
        return this.SSLCertificateKeyFile;
    }
    
    public void setSSLCertificateKeyFile(final String SSLCertificateKeyFile) {
        this.SSLCertificateKeyFile = SSLCertificateKeyFile;
    }
    
    public String getSSLCertificateChainFile() {
        return this.SSLCertificateChainFile;
    }
    
    public void setSSLCertificateChainFile(final String SSLCertificateChainFile) {
        this.SSLCertificateChainFile = SSLCertificateChainFile;
    }
    
    public String getSSLCACertificatePath() {
        return this.SSLCACertificatePath;
    }
    
    public void setSSLCACertificatePath(final String SSLCACertificatePath) {
        this.SSLCACertificatePath = SSLCACertificatePath;
    }
    
    public String getSSLCACertificateFile() {
        return this.SSLCACertificateFile;
    }
    
    public void setSSLCACertificateFile(final String SSLCACertificateFile) {
        this.SSLCACertificateFile = SSLCACertificateFile;
    }
    
    public String getSSLCARevocationPath() {
        return this.SSLCARevocationPath;
    }
    
    public void setSSLCARevocationPath(final String SSLCARevocationPath) {
        this.SSLCARevocationPath = SSLCARevocationPath;
    }
    
    public String getSSLCARevocationFile() {
        return this.SSLCARevocationFile;
    }
    
    public void setSSLCARevocationFile(final String SSLCARevocationFile) {
        this.SSLCARevocationFile = SSLCARevocationFile;
    }
    
    public String getSSLVerifyClient() {
        return this.SSLVerifyClient;
    }
    
    public void setSSLVerifyClient(final String SSLVerifyClient) {
        this.SSLVerifyClient = SSLVerifyClient;
    }
    
    public int getSSLVerifyDepth() {
        return this.SSLVerifyDepth;
    }
    
    public void setSSLVerifyDepth(final int SSLVerifyDepth) {
        this.SSLVerifyDepth = SSLVerifyDepth;
    }
    
    public int getKeepAliveCount() {
        if (this.pollers == null) {
            return 0;
        }
        int keepAliveCount = 0;
        for (int i = 0; i < this.pollers.length; ++i) {
            keepAliveCount += this.pollers[i].getKeepAliveCount();
        }
        return keepAliveCount;
    }
    
    public int getSendfileCount() {
        if (this.sendfiles == null) {
            return 0;
        }
        int sendfileCount = 0;
        for (int i = 0; i < this.sendfiles.length; ++i) {
            sendfileCount += this.sendfiles[i].getSendfileCount();
        }
        return sendfileCount;
    }
    
    public int getCurrentThreadCount() {
        return this.curThreads;
    }
    
    public int getCurrentThreadsBusy() {
        return this.curThreadsBusy;
    }
    
    public boolean isRunning() {
        return this.running;
    }
    
    public boolean isPaused() {
        return this.paused;
    }
    
    public void init() throws Exception {
        if (this.initialized) {
            return;
        }
        this.rootPool = Pool.create(0L);
        this.serverSockPool = Pool.create(this.rootPool);
        String addressStr = null;
        if (this.address == null) {
            addressStr = null;
        }
        else {
            addressStr = this.address.getHostAddress();
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
        final long inetAddress = Address.info(addressStr, family, this.port, 0, this.rootPool);
        this.serverSock = Socket.create(family, 0, 6, this.rootPool);
        if (OS.IS_UNIX) {
            Socket.optSet(this.serverSock, 16, 1);
        }
        Socket.optSet(this.serverSock, 2, 1);
        int ret = Socket.bind(this.serverSock, inetAddress);
        if (ret != 0) {
            throw new Exception(AprEndpoint.sm.getString("endpoint.init.bind", "" + ret, Error.strerror(ret)));
        }
        ret = Socket.listen(this.serverSock, this.backlog);
        if (ret != 0) {
            throw new Exception(AprEndpoint.sm.getString("endpoint.init.listen", "" + ret, Error.strerror(ret)));
        }
        if (OS.IS_WIN32 || OS.IS_WIN64) {
            Socket.optSet(this.serverSock, 16, 1);
        }
        if (this.useSendfile && !Library.APR_HAS_SENDFILE) {
            this.useSendfile = false;
        }
        if (this.acceptorThreadCount == 0) {
            this.acceptorThreadCount = 1;
        }
        if (this.pollerThreadCount == 0) {
            if ((OS.IS_WIN32 || OS.IS_WIN64) && this.pollerSize > 1024) {
                this.pollerThreadCount = this.pollerSize / 1024;
                this.pollerSize -= this.pollerSize % 1024;
            }
            else {
                this.pollerThreadCount = 1;
            }
        }
        if (this.sendfileThreadCount == 0) {
            if ((OS.IS_WIN32 || OS.IS_WIN64) && this.sendfileSize > 1024) {
                this.sendfileThreadCount = this.sendfileSize / 1024;
                this.sendfileSize -= this.sendfileSize % 1024;
            }
            else {
                this.sendfileThreadCount = 1;
            }
        }
        if (Socket.optSet(this.serverSock, 32768, 1) == 70023) {
            this.deferAccept = false;
        }
        if (this.SSLEnabled) {
            int value = 7;
            if ("SSLv2".equalsIgnoreCase(this.SSLProtocol)) {
                value = 1;
            }
            else if ("SSLv3".equalsIgnoreCase(this.SSLProtocol)) {
                value = 2;
            }
            else if ("TLSv1".equalsIgnoreCase(this.SSLProtocol)) {
                value = 4;
            }
            else if ("SSLv2+SSLv3".equalsIgnoreCase(this.SSLProtocol)) {
                value = 3;
            }
            SSLContext.setCipherSuite(this.sslContext = SSLContext.make(this.rootPool, value, 1), this.SSLCipherSuite);
            SSLContext.setCertificate(this.sslContext, this.SSLCertificateFile, this.SSLCertificateKeyFile, this.SSLPassword, 0);
            SSLContext.setCertificateChainFile(this.sslContext, this.SSLCertificateChainFile, false);
            SSLContext.setCACertificate(this.sslContext, this.SSLCACertificateFile, this.SSLCACertificatePath);
            SSLContext.setCARevocation(this.sslContext, this.SSLCARevocationFile, this.SSLCARevocationPath);
            value = 0;
            if ("optional".equalsIgnoreCase(this.SSLVerifyClient)) {
                value = 1;
            }
            else if ("require".equalsIgnoreCase(this.SSLVerifyClient)) {
                value = 2;
            }
            else if ("optionalNoCA".equalsIgnoreCase(this.SSLVerifyClient)) {
                value = 3;
            }
            SSLContext.setVerify(this.sslContext, value, this.SSLVerifyDepth);
            this.useSendfile = false;
        }
        this.initialized = true;
    }
    
    public void start() throws Exception {
        if (!this.initialized) {
            this.init();
        }
        if (!this.running) {
            this.running = true;
            this.paused = false;
            if (this.executor == null) {
                this.workers = new WorkerStack(this.maxThreads);
            }
            this.pollers = new Poller[this.pollerThreadCount];
            for (int i = 0; i < this.pollerThreadCount; ++i) {
                (this.pollers[i] = new Poller(false)).init();
                final Thread pollerThread = new Thread(this.pollers[i], this.getName() + "-Poller-" + i);
                pollerThread.setPriority(this.threadPriority);
                pollerThread.setDaemon(true);
                pollerThread.start();
            }
            this.cometPollers = new Poller[this.pollerThreadCount];
            for (int i = 0; i < this.pollerThreadCount; ++i) {
                (this.cometPollers[i] = new Poller(true)).init();
                final Thread pollerThread = new Thread(this.cometPollers[i], this.getName() + "-CometPoller-" + i);
                pollerThread.setPriority(this.threadPriority);
                pollerThread.setDaemon(true);
                pollerThread.start();
            }
            if (this.useSendfile) {
                this.sendfiles = new Sendfile[this.sendfileThreadCount];
                for (int i = 0; i < this.sendfileThreadCount; ++i) {
                    (this.sendfiles[i] = new Sendfile()).init();
                    final Thread sendfileThread = new Thread(this.sendfiles[i], this.getName() + "-Sendfile-" + i);
                    sendfileThread.setPriority(this.threadPriority);
                    sendfileThread.setDaemon(true);
                    sendfileThread.start();
                }
            }
            for (int i = 0; i < this.acceptorThreadCount; ++i) {
                final Thread acceptorThread = new Thread(new Acceptor(), this.getName() + "-Acceptor-" + i);
                acceptorThread.setPriority(this.threadPriority);
                acceptorThread.setDaemon(this.daemon);
                acceptorThread.start();
            }
        }
    }
    
    public void pause() {
        if (this.running && !this.paused) {
            this.paused = true;
            this.unlockAccept();
        }
    }
    
    public void resume() {
        if (this.running) {
            this.paused = false;
        }
    }
    
    public void stop() {
        if (this.running) {
            this.running = false;
            this.unlockAccept();
            for (int i = 0; i < this.pollers.length; ++i) {
                this.pollers[i].destroy();
            }
            this.pollers = null;
            for (int i = 0; i < this.cometPollers.length; ++i) {
                this.cometPollers[i].destroy();
            }
            this.cometPollers = null;
            if (this.useSendfile) {
                for (int i = 0; i < this.sendfiles.length; ++i) {
                    this.sendfiles[i].destroy();
                }
                this.sendfiles = null;
            }
        }
    }
    
    public void destroy() throws Exception {
        if (this.running) {
            this.stop();
        }
        Pool.destroy(this.serverSockPool);
        this.serverSockPool = 0L;
        Socket.close(this.serverSock);
        this.serverSock = 0L;
        this.sslContext = 0L;
        Pool.destroy(this.rootPool);
        this.rootPool = 0L;
        this.initialized = false;
    }
    
    protected int getSequence() {
        return this.sequence++;
    }
    
    protected void unlockAccept() {
        java.net.Socket s = null;
        try {
            if (this.address == null) {
                s = new java.net.Socket(InetAddress.getByName("localhost").getHostAddress(), this.port);
            }
            else {
                s = new java.net.Socket(this.address, this.port);
                s.setSoLinger(true, 0);
            }
        }
        catch (Exception e) {
            if (AprEndpoint.log.isDebugEnabled()) {
                AprEndpoint.log.debug((Object)AprEndpoint.sm.getString("endpoint.debug.unlock", "" + this.port), (Throwable)e);
            }
        }
        finally {
            if (s != null) {
                try {
                    s.close();
                }
                catch (Exception ex) {}
            }
        }
    }
    
    protected boolean setSocketOptions(final long socket) {
        int step = 1;
        try {
            if (this.soLinger >= 0) {
                Socket.optSet(socket, 1, this.soLinger);
            }
            if (this.tcpNoDelay) {
                Socket.optSet(socket, 512, this.tcpNoDelay ? 1 : 0);
            }
            if (this.soTimeout > 0) {
                Socket.timeoutSet(socket, this.soTimeout * 1000);
            }
            step = 2;
            if (this.sslContext != 0L) {
                SSLSocket.attach(this.sslContext, socket);
                if (SSLSocket.handshake(socket) != 0) {
                    if (AprEndpoint.log.isDebugEnabled()) {
                        AprEndpoint.log.debug((Object)(AprEndpoint.sm.getString("endpoint.err.handshake") + ": " + SSL.getLastError()));
                    }
                    return false;
                }
            }
        }
        catch (Throwable t) {
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
    
    protected Worker createWorkerThread() {
        synchronized (this.workers) {
            if (this.workers.size() > 0) {
                ++this.curThreadsBusy;
                return this.workers.pop();
            }
            if (this.maxThreads > 0 && this.curThreads < this.maxThreads) {
                ++this.curThreadsBusy;
                return this.newWorkerThread();
            }
            if (this.maxThreads < 0) {
                ++this.curThreadsBusy;
                return this.newWorkerThread();
            }
            return null;
        }
    }
    
    protected Worker newWorkerThread() {
        final Worker workerThread = new Worker();
        workerThread.start();
        return workerThread;
    }
    
    protected Worker getWorkerThread() {
        Worker workerThread;
        for (workerThread = this.createWorkerThread(); workerThread == null; workerThread = this.createWorkerThread()) {
            try {
                synchronized (this.workers) {
                    this.workers.wait();
                }
            }
            catch (InterruptedException ex) {}
        }
        return workerThread;
    }
    
    protected void recycleWorkerThread(final Worker workerThread) {
        synchronized (this.workers) {
            this.workers.push(workerThread);
            --this.curThreadsBusy;
            this.workers.notify();
        }
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
            if (this.executor == null) {
                this.getWorkerThread().assignWithOptions(socket);
            }
            else {
                this.executor.execute(new SocketWithOptionsProcessor(socket));
            }
        }
        catch (Throwable t) {
            AprEndpoint.log.error((Object)AprEndpoint.sm.getString("endpoint.process.fail"), t);
            return false;
        }
        return true;
    }
    
    protected boolean processSocket(final long socket) {
        try {
            if (this.executor == null) {
                this.getWorkerThread().assign(socket);
            }
            else {
                this.executor.execute(new SocketProcessor(socket));
            }
        }
        catch (Throwable t) {
            AprEndpoint.log.error((Object)AprEndpoint.sm.getString("endpoint.process.fail"), t);
            return false;
        }
        return true;
    }
    
    protected boolean processSocket(final long socket, final SocketStatus status) {
        try {
            if (this.executor == null) {
                this.getWorkerThread().assign(socket, status);
            }
            else {
                this.executor.execute(new SocketEventProcessor(socket, status));
            }
        }
        catch (Throwable t) {
            AprEndpoint.log.error((Object)AprEndpoint.sm.getString("endpoint.process.fail"), t);
            return false;
        }
        return true;
    }
    
    static {
        AprEndpoint.log = LogFactory.getLog((Class)AprEndpoint.class);
        AprEndpoint.sm = StringManager.getManager("org.apache.tomcat.util.net.res");
    }
    
    protected class Acceptor implements Runnable
    {
        public void run() {
            while (AprEndpoint.this.running) {
                while (AprEndpoint.this.paused) {
                    try {
                        Thread.sleep(1000L);
                    }
                    catch (InterruptedException e) {}
                }
                try {
                    final long socket = Socket.accept(AprEndpoint.this.serverSock);
                    if (AprEndpoint.this.processSocketWithOptions(socket)) {
                        continue;
                    }
                    Socket.destroy(socket);
                }
                catch (Throwable t) {
                    AprEndpoint.log.error((Object)AprEndpoint.sm.getString("endpoint.accept.fail"), t);
                }
            }
        }
    }
    
    public class Poller implements Runnable
    {
        protected long serverPollset;
        protected long pool;
        protected long[] desc;
        protected long[] addS;
        protected int addCount;
        protected boolean comet;
        protected int keepAliveCount;
        
        public int getKeepAliveCount() {
            return this.keepAliveCount;
        }
        
        public Poller(final boolean comet) {
            this.serverPollset = 0L;
            this.pool = 0L;
            this.addCount = 0;
            this.comet = true;
            this.keepAliveCount = 0;
            this.comet = comet;
        }
        
        protected void init() {
            this.pool = Pool.create(AprEndpoint.this.serverSockPool);
            int size = AprEndpoint.this.pollerSize / AprEndpoint.this.pollerThreadCount;
            int timeout = AprEndpoint.this.keepAliveTimeout;
            if (timeout < 0) {
                timeout = AprEndpoint.this.soTimeout;
            }
            this.serverPollset = AprEndpoint.this.allocatePoller(size, this.pool, timeout);
            if (this.serverPollset == 0L && size > 1024) {
                size = 1024;
                this.serverPollset = AprEndpoint.this.allocatePoller(size, this.pool, timeout);
            }
            if (this.serverPollset == 0L) {
                size = 62;
                this.serverPollset = AprEndpoint.this.allocatePoller(size, this.pool, timeout);
            }
            this.desc = new long[size * 2];
            this.keepAliveCount = 0;
            this.addS = new long[size];
            this.addCount = 0;
        }
        
        protected void destroy() {
            try {
                synchronized (this) {
                    this.wait(AprEndpoint.this.pollTime / 1000);
                }
            }
            catch (InterruptedException ex) {}
            for (int i = 0; i < this.addCount; ++i) {
                if (this.comet) {
                    AprEndpoint.this.processSocket(this.addS[i], SocketStatus.STOP);
                }
                else {
                    Socket.destroy(this.addS[i]);
                }
            }
            final int rv = Poll.pollset(this.serverPollset, this.desc);
            if (rv > 0) {
                for (int n = 0; n < rv; ++n) {
                    if (this.comet) {
                        AprEndpoint.this.processSocket(this.desc[n * 2 + 1], SocketStatus.STOP);
                    }
                    else {
                        Socket.destroy(this.desc[n * 2 + 1]);
                    }
                }
            }
            Pool.destroy(this.pool);
            this.keepAliveCount = 0;
            this.addCount = 0;
        }
        
        public void add(final long socket) {
            synchronized (this) {
                if (this.addCount >= this.addS.length) {
                    if (this.comet) {
                        AprEndpoint.this.processSocket(socket, SocketStatus.ERROR);
                    }
                    else {
                        Socket.destroy(socket);
                    }
                    return;
                }
                this.addS[this.addCount] = socket;
                ++this.addCount;
                this.notify();
            }
        }
        
        public void run() {
            long maintainTime = 0L;
            while (AprEndpoint.this.running) {
                while (AprEndpoint.this.paused) {
                    try {
                        Thread.sleep(1000L);
                    }
                    catch (InterruptedException e) {}
                }
                while (this.keepAliveCount < 1 && this.addCount < 1) {
                    maintainTime = 0L;
                    try {
                        synchronized (this) {
                            this.wait();
                        }
                    }
                    catch (InterruptedException e) {}
                }
                try {
                    if (this.addCount > 0) {
                        synchronized (this) {
                            for (int i = this.addCount - 1; i >= 0; --i) {
                                final int rv = Poll.add(this.serverPollset, this.addS[i], 1);
                                if (rv == 0) {
                                    ++this.keepAliveCount;
                                }
                                else if (this.comet) {
                                    AprEndpoint.this.processSocket(this.addS[i], SocketStatus.ERROR);
                                }
                                else {
                                    Socket.destroy(this.addS[i]);
                                }
                            }
                            this.addCount = 0;
                        }
                    }
                    maintainTime += AprEndpoint.this.pollTime;
                    int rv2 = Poll.poll(this.serverPollset, AprEndpoint.this.pollTime, this.desc, true);
                    if (rv2 > 0) {
                        this.keepAliveCount -= rv2;
                        for (int n = 0; n < rv2; ++n) {
                            if ((this.desc[n * 2] & 0x20L) == 0x20L || (this.desc[n * 2] & 0x10L) == 0x10L || (this.comet && !AprEndpoint.this.processSocket(this.desc[n * 2 + 1], SocketStatus.OPEN)) || (!this.comet && !AprEndpoint.this.processSocket(this.desc[n * 2 + 1]))) {
                                if (this.comet) {
                                    AprEndpoint.this.processSocket(this.desc[n * 2 + 1], SocketStatus.DISCONNECT);
                                }
                                else {
                                    Socket.destroy(this.desc[n * 2 + 1]);
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
                            AprEndpoint.log.error((Object)AprEndpoint.sm.getString("endpoint.poll.fail", "" + errn, Error.strerror(errn)));
                            synchronized (this) {
                                this.destroy();
                                this.init();
                            }
                            continue;
                        }
                    }
                    if (AprEndpoint.this.soTimeout <= 0 || maintainTime <= 1000000L || !AprEndpoint.this.running) {
                        continue;
                    }
                    rv2 = Poll.maintain(this.serverPollset, this.desc, true);
                    maintainTime = 0L;
                    if (rv2 <= 0) {
                        continue;
                    }
                    this.keepAliveCount -= rv2;
                    for (int n = 0; n < rv2; ++n) {
                        if (this.comet) {
                            AprEndpoint.this.processSocket(this.desc[n], SocketStatus.TIMEOUT);
                        }
                        else {
                            Socket.destroy(this.desc[n]);
                        }
                    }
                }
                catch (Throwable t) {
                    AprEndpoint.log.error((Object)AprEndpoint.sm.getString("endpoint.poll.error"), t);
                }
            }
            synchronized (this) {
                this.notifyAll();
            }
        }
    }
    
    protected class Worker implements Runnable
    {
        protected Thread thread;
        protected boolean available;
        protected long socket;
        protected SocketStatus status;
        protected boolean options;
        
        protected Worker() {
            this.thread = null;
            this.available = false;
            this.socket = 0L;
            this.status = null;
            this.options = false;
        }
        
        protected synchronized void assignWithOptions(final long socket) {
            while (this.available) {
                try {
                    this.wait();
                }
                catch (InterruptedException e) {}
            }
            this.socket = socket;
            this.status = null;
            this.options = true;
            this.available = true;
            this.notifyAll();
        }
        
        protected synchronized void assign(final long socket) {
            while (this.available) {
                try {
                    this.wait();
                }
                catch (InterruptedException e) {}
            }
            this.socket = socket;
            this.status = null;
            this.options = false;
            this.available = true;
            this.notifyAll();
        }
        
        protected synchronized void assign(final long socket, final SocketStatus status) {
            while (this.available) {
                try {
                    this.wait();
                }
                catch (InterruptedException e) {}
            }
            this.socket = socket;
            this.status = status;
            this.options = false;
            this.available = true;
            this.notifyAll();
        }
        
        protected synchronized long await() {
            while (!this.available) {
                try {
                    this.wait();
                }
                catch (InterruptedException e) {}
            }
            final long socket = this.socket;
            this.available = false;
            this.notifyAll();
            return socket;
        }
        
        public void run() {
            while (AprEndpoint.this.running) {
                long socket = this.await();
                if (socket == 0L) {
                    continue;
                }
                if (!AprEndpoint.this.deferAccept && this.options) {
                    if (AprEndpoint.this.setSocketOptions(socket)) {
                        AprEndpoint.this.getPoller().add(socket);
                    }
                    else {
                        Socket.destroy(socket);
                        socket = 0L;
                    }
                }
                else if (this.status != null && AprEndpoint.this.handler.event(socket, this.status) == Handler.SocketState.CLOSED) {
                    Socket.destroy(socket);
                    socket = 0L;
                }
                else if (this.status == null && ((this.options && !AprEndpoint.this.setSocketOptions(socket)) || AprEndpoint.this.handler.process(socket) == Handler.SocketState.CLOSED)) {
                    Socket.destroy(socket);
                    socket = 0L;
                }
                AprEndpoint.this.recycleWorkerThread(this);
            }
        }
        
        public void start() {
            (this.thread = new Thread(this)).setName(AprEndpoint.this.getName() + "-" + ++AprEndpoint.this.curThreads);
            this.thread.setDaemon(true);
            this.thread.start();
        }
    }
    
    public static class SendfileData
    {
        public String fileName;
        public long fd;
        public long fdpool;
        public long start;
        public long end;
        public long socket;
        public long pos;
        public boolean keepAlive;
    }
    
    public class Sendfile implements Runnable
    {
        protected long sendfilePollset;
        protected long pool;
        protected long[] desc;
        protected HashMap<Long, SendfileData> sendfileData;
        protected int sendfileCount;
        protected ArrayList<SendfileData> addS;
        
        public Sendfile() {
            this.sendfilePollset = 0L;
            this.pool = 0L;
        }
        
        public int getSendfileCount() {
            return this.sendfileCount;
        }
        
        protected void init() {
            this.pool = Pool.create(AprEndpoint.this.serverSockPool);
            int size = AprEndpoint.this.sendfileSize / AprEndpoint.this.sendfileThreadCount;
            this.sendfilePollset = AprEndpoint.this.allocatePoller(size, this.pool, AprEndpoint.this.soTimeout);
            if (this.sendfilePollset == 0L && size > 1024) {
                size = 1024;
                this.sendfilePollset = AprEndpoint.this.allocatePoller(size, this.pool, AprEndpoint.this.soTimeout);
            }
            if (this.sendfilePollset == 0L) {
                size = 62;
                this.sendfilePollset = AprEndpoint.this.allocatePoller(size, this.pool, AprEndpoint.this.soTimeout);
            }
            this.desc = new long[size * 2];
            this.sendfileData = new HashMap<Long, SendfileData>(size);
            this.addS = new ArrayList<SendfileData>();
        }
        
        protected void destroy() {
            try {
                synchronized (this) {
                    this.wait(AprEndpoint.this.pollTime / 1000);
                }
            }
            catch (InterruptedException ex) {}
            for (int i = this.addS.size() - 1; i >= 0; --i) {
                final SendfileData data = this.addS.get(i);
                Socket.destroy(data.socket);
            }
            final int rv = Poll.pollset(this.sendfilePollset, this.desc);
            if (rv > 0) {
                for (int n = 0; n < rv; ++n) {
                    Socket.destroy(this.desc[n * 2 + 1]);
                }
            }
            Pool.destroy(this.pool);
            this.sendfileData.clear();
        }
        
        public boolean add(final SendfileData data) {
            try {
                data.fdpool = Socket.pool(data.socket);
                data.fd = File.open(data.fileName, 4129, 0, data.fdpool);
                data.pos = data.start;
                Socket.timeoutSet(data.socket, 0L);
                while (true) {
                    final long nw = Socket.sendfilen(data.socket, data.fd, data.pos, data.end - data.pos, 0);
                    if (nw < 0L) {
                        if (-nw != 120002L) {
                            Socket.destroy(data.socket);
                            data.socket = 0L;
                            return false;
                        }
                        break;
                    }
                    else {
                        data.pos += nw;
                        if (data.pos >= data.end) {
                            Pool.destroy(data.fdpool);
                            Socket.timeoutSet(data.socket, AprEndpoint.this.soTimeout * 1000);
                            return true;
                        }
                        continue;
                    }
                }
            }
            catch (Exception e) {
                AprEndpoint.log.error((Object)AprEndpoint.sm.getString("endpoint.sendfile.error"), (Throwable)e);
                return false;
            }
            synchronized (this) {
                this.addS.add(data);
                this.notify();
            }
            return false;
        }
        
        protected void remove(final SendfileData data) {
            final int rv = Poll.remove(this.sendfilePollset, data.socket);
            if (rv == 0) {
                --this.sendfileCount;
            }
            this.sendfileData.remove(new Long(data.socket));
        }
        
        public void run() {
            long maintainTime = 0L;
            while (AprEndpoint.this.running) {
                while (AprEndpoint.this.paused) {
                    try {
                        Thread.sleep(1000L);
                    }
                    catch (InterruptedException e) {}
                }
                while (this.sendfileCount < 1 && this.addS.size() < 1) {
                    maintainTime = 0L;
                    try {
                        synchronized (this) {
                            this.wait();
                        }
                    }
                    catch (InterruptedException e) {}
                }
                try {
                    if (this.addS.size() > 0) {
                        synchronized (this) {
                            for (int i = this.addS.size() - 1; i >= 0; --i) {
                                final SendfileData data = this.addS.get(i);
                                final int rv = Poll.add(this.sendfilePollset, data.socket, 4);
                                if (rv == 0) {
                                    this.sendfileData.put(new Long(data.socket), data);
                                    ++this.sendfileCount;
                                }
                                else {
                                    AprEndpoint.log.warn((Object)AprEndpoint.sm.getString("endpoint.sendfile.addfail", "" + rv, Error.strerror(rv)));
                                    Socket.destroy(data.socket);
                                }
                            }
                            this.addS.clear();
                        }
                    }
                    maintainTime += AprEndpoint.this.pollTime;
                    int rv2 = Poll.poll(this.sendfilePollset, AprEndpoint.this.pollTime, this.desc, false);
                    if (rv2 > 0) {
                        for (int n = 0; n < rv2; ++n) {
                            final SendfileData state = this.sendfileData.get(new Long(this.desc[n * 2 + 1]));
                            if ((this.desc[n * 2] & 0x20L) == 0x20L || (this.desc[n * 2] & 0x10L) == 0x10L) {
                                this.remove(state);
                                Socket.destroy(state.socket);
                            }
                            else {
                                final long nw = Socket.sendfilen(state.socket, state.fd, state.pos, state.end - state.pos, 0);
                                if (nw < 0L) {
                                    this.remove(state);
                                    Socket.destroy(state.socket);
                                }
                                else {
                                    state.pos += nw;
                                    if (state.pos >= state.end) {
                                        this.remove(state);
                                        if (state.keepAlive) {
                                            Pool.destroy(state.fdpool);
                                            Socket.timeoutSet(state.socket, AprEndpoint.this.soTimeout * 1000);
                                            if (!AprEndpoint.this.processSocket(state.socket)) {
                                                Socket.destroy(state.socket);
                                            }
                                        }
                                        else {
                                            Socket.destroy(state.socket);
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
                            AprEndpoint.log.error((Object)AprEndpoint.sm.getString("endpoint.poll.fail", "" + errn, Error.strerror(errn)));
                            synchronized (this) {
                                this.destroy();
                                this.init();
                            }
                            continue;
                        }
                    }
                    if (AprEndpoint.this.soTimeout <= 0 || maintainTime <= 1000000L || !AprEndpoint.this.running) {
                        continue;
                    }
                    rv2 = Poll.maintain(this.sendfilePollset, this.desc, true);
                    maintainTime = 0L;
                    if (rv2 <= 0) {
                        continue;
                    }
                    for (int n = 0; n < rv2; ++n) {
                        final SendfileData state = this.sendfileData.get(new Long(this.desc[n]));
                        this.remove(state);
                        Socket.destroy(state.socket);
                    }
                }
                catch (Throwable t) {
                    AprEndpoint.log.error((Object)AprEndpoint.sm.getString("endpoint.poll.error"), t);
                }
            }
            synchronized (this) {
                this.notifyAll();
            }
        }
    }
    
    public class WorkerStack
    {
        protected Worker[] workers;
        protected int end;
        
        public WorkerStack(final int size) {
            this.workers = null;
            this.end = 0;
            this.workers = new Worker[size];
        }
        
        public void push(final Worker worker) {
            this.workers[this.end++] = worker;
        }
        
        public Worker pop() {
            if (this.end > 0) {
                final Worker[] workers = this.workers;
                final int end = this.end - 1;
                this.end = end;
                return workers[end];
            }
            return null;
        }
        
        public Worker peek() {
            return this.workers[this.end];
        }
        
        public boolean isEmpty() {
            return this.end == 0;
        }
        
        public int size() {
            return this.end;
        }
    }
    
    protected class SocketWithOptionsProcessor implements Runnable
    {
        protected long socket;
        
        public SocketWithOptionsProcessor(final long socket) {
            this.socket = 0L;
            this.socket = socket;
        }
        
        public void run() {
            if (!AprEndpoint.this.deferAccept) {
                if (AprEndpoint.this.setSocketOptions(this.socket)) {
                    AprEndpoint.this.getPoller().add(this.socket);
                }
                else {
                    Socket.destroy(this.socket);
                    this.socket = 0L;
                }
            }
            else if (!AprEndpoint.this.setSocketOptions(this.socket) || AprEndpoint.this.handler.process(this.socket) == Handler.SocketState.CLOSED) {
                Socket.destroy(this.socket);
                this.socket = 0L;
            }
        }
    }
    
    protected class SocketProcessor implements Runnable
    {
        protected long socket;
        
        public SocketProcessor(final long socket) {
            this.socket = 0L;
            this.socket = socket;
        }
        
        public void run() {
            if (AprEndpoint.this.handler.process(this.socket) == Handler.SocketState.CLOSED) {
                Socket.destroy(this.socket);
                this.socket = 0L;
            }
        }
    }
    
    protected class SocketEventProcessor implements Runnable
    {
        protected long socket;
        protected SocketStatus status;
        
        public SocketEventProcessor(final long socket, final SocketStatus status) {
            this.socket = 0L;
            this.status = null;
            this.socket = socket;
            this.status = status;
        }
        
        public void run() {
            if (AprEndpoint.this.handler.event(this.socket, this.status) == Handler.SocketState.CLOSED) {
                Socket.destroy(this.socket);
                this.socket = 0L;
            }
        }
    }
    
    public interface Handler
    {
        SocketState process(final long p0);
        
        SocketState event(final long p0, final SocketStatus p1);
        
        public enum SocketState
        {
            OPEN, 
            CLOSED, 
            LONG;
        }
    }
}
