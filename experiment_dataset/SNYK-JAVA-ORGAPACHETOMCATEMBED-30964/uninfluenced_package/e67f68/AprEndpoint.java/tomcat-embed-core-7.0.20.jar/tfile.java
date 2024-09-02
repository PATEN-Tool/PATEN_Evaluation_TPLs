// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.tomcat.util.net;

import org.apache.tomcat.jni.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import org.apache.juli.logging.LogFactory;
import java.security.PrivilegedAction;
import java.security.AccessController;
import java.util.concurrent.RejectedExecutionException;
import org.apache.tomcat.jni.Status;
import org.apache.tomcat.jni.Poll;
import org.apache.tomcat.util.ExceptionUtils;
import org.apache.tomcat.jni.SSLSocket;
import org.apache.tomcat.jni.SSL;
import org.apache.tomcat.jni.SSLContext;
import org.apache.tomcat.jni.Error;
import org.apache.tomcat.jni.Socket;
import org.apache.tomcat.jni.Address;
import org.apache.tomcat.jni.OS;
import org.apache.tomcat.jni.Pool;
import org.apache.tomcat.jni.Library;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.apache.juli.logging.Log;

public class AprEndpoint extends AbstractEndpoint
{
    private static final Log log;
    protected long rootPool;
    protected long serverSock;
    protected long serverSockPool;
    protected long sslContext;
    private Acceptor[] acceptors;
    protected ConcurrentLinkedQueue<SocketWrapper<Long>> waitingRequests;
    protected boolean deferAccept;
    protected int sendfileSize;
    protected Handler handler;
    protected int pollTime;
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
    protected boolean SSLInsecureRenegotiation;
    
    public AprEndpoint() {
        this.rootPool = 0L;
        this.serverSock = 0L;
        this.serverSockPool = 0L;
        this.sslContext = 0L;
        this.acceptors = null;
        this.waitingRequests = new ConcurrentLinkedQueue<SocketWrapper<Long>>();
        this.deferAccept = true;
        this.sendfileSize = 1024;
        this.handler = null;
        this.pollTime = 2000;
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
        this.SSLInsecureRenegotiation = false;
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
    
    public void setHandler(final Handler handler) {
        this.handler = handler;
    }
    
    public Handler getHandler() {
        return this.handler;
    }
    
    public int getPollTime() {
        return this.pollTime;
    }
    
    public void setPollTime(final int pollTime) {
        if (pollTime > 0) {
            this.pollTime = pollTime;
        }
    }
    
    public void setUseSendfile(final boolean useSendfile) {
        this.useSendfile = useSendfile;
    }
    
    @Override
    public boolean getUseSendfile() {
        return this.useSendfile;
    }
    
    public void setUseComet(final boolean useComet) {
        this.useComet = useComet;
    }
    
    @Override
    public boolean getUseComet() {
        return this.useComet;
    }
    
    @Override
    public boolean getUseCometTimeout() {
        return false;
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
    
    public void setSSLInsecureRenegotiation(final boolean SSLInsecureRenegotiation) {
        this.SSLInsecureRenegotiation = SSLInsecureRenegotiation;
    }
    
    public boolean getSSLInsecureRenegotiation() {
        return this.SSLInsecureRenegotiation;
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
        if (this.useSendfile && !Library.APR_HAS_SENDFILE) {
            this.useSendfile = false;
        }
        if (this.acceptorThreadCount == 0) {
            this.acceptorThreadCount = 1;
        }
        if (this.pollerThreadCount == 0) {
            if ((OS.IS_WIN32 || OS.IS_WIN64) && this.getMaxConnections() > 1024) {
                this.pollerThreadCount = this.getMaxConnections() / 1024;
                this.setMaxConnections(this.getMaxConnections() - this.getMaxConnections() % 1024);
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
        if (this.deferAccept && Socket.optSet(this.serverSock, 32768, 1) == 70023) {
            this.deferAccept = false;
        }
        if (this.isSSLEnabled()) {
            if (this.SSLCertificateFile == null) {
                throw new Exception(AprEndpoint.sm.getString("endpoint.apr.noSslCertFile"));
            }
            final int tcnFullVersion = Library.TCN_MAJOR_VERSION * 1000 + Library.TCN_MINOR_VERSION * 100 + Library.TCN_PATCH_VERSION;
            int value;
            if (tcnFullVersion <= 1120) {
                value = 6;
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
                else if (!"all".equalsIgnoreCase(this.SSLProtocol) && this.SSLProtocol != null) {
                    if (this.SSLProtocol.length() != 0) {
                        throw new Exception(AprEndpoint.sm.getString("endpoint.apr.invalidSslProtocol", this.SSLProtocol));
                    }
                }
            }
            else {
                value = 0;
                if (this.SSLProtocol == null || this.SSLProtocol.length() == 0) {
                    value = 6;
                }
                else {
                    for (String protocol : this.SSLProtocol.split("\\+")) {
                        protocol = protocol.trim();
                        if ("SSLv2".equalsIgnoreCase(protocol)) {
                            value |= 0x1;
                        }
                        else if ("SSLv3".equalsIgnoreCase(protocol)) {
                            value |= 0x2;
                        }
                        else if ("TLSv1".equalsIgnoreCase(protocol)) {
                            value |= 0x4;
                        }
                        else {
                            if (!"all".equalsIgnoreCase(protocol)) {
                                throw new Exception(AprEndpoint.sm.getString("endpoint.apr.invalidSslProtocol", this.SSLProtocol));
                            }
                            value |= 0x6;
                        }
                    }
                }
            }
            this.sslContext = SSLContext.make(this.rootPool, value, 1);
            if (this.SSLInsecureRenegotiation) {
                boolean legacyRenegSupported = false;
                try {
                    legacyRenegSupported = SSL.hasOp(262144);
                    if (legacyRenegSupported) {
                        SSLContext.setOptions(this.sslContext, 262144);
                    }
                }
                catch (UnsatisfiedLinkError unsatisfiedLinkError) {}
                if (!legacyRenegSupported) {
                    AprEndpoint.log.warn((Object)AprEndpoint.sm.getString("endpoint.warn.noInsecureReneg", SSL.versionString()));
                }
            }
            SSLContext.setCipherSuite(this.sslContext, this.SSLCipherSuite);
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
            this.pollers = new Poller[this.pollerThreadCount];
            for (int i = 0; i < this.pollerThreadCount; ++i) {
                (this.pollers[i] = new Poller(false)).init();
                this.pollers[i].setName(this.getName() + "-Poller-" + i);
                this.pollers[i].setPriority(this.threadPriority);
                this.pollers[i].setDaemon(true);
                this.pollers[i].start();
            }
            this.cometPollers = new Poller[this.pollerThreadCount];
            for (int i = 0; i < this.pollerThreadCount; ++i) {
                (this.cometPollers[i] = new Poller(true)).init();
                this.cometPollers[i].setName(this.getName() + "-CometPoller-" + i);
                this.cometPollers[i].setPriority(this.threadPriority);
                this.cometPollers[i].setDaemon(true);
                this.cometPollers[i].start();
            }
            if (this.useSendfile) {
                this.sendfiles = new Sendfile[this.sendfileThreadCount];
                for (int i = 0; i < this.sendfileThreadCount; ++i) {
                    (this.sendfiles[i] = new Sendfile()).init();
                    this.sendfiles[i].setName(this.getName() + "-Sendfile-" + i);
                    this.sendfiles[i].setPriority(this.threadPriority);
                    this.sendfiles[i].setDaemon(true);
                    this.sendfiles[i].start();
                }
            }
            this.acceptors = new Acceptor[this.acceptorThreadCount];
            for (int i = 0; i < this.acceptorThreadCount; ++i) {
                (this.acceptors[i] = new Acceptor()).setName(this.getName() + "-Acceptor-" + i);
                this.acceptors[i].setPriority(this.threadPriority);
                this.acceptors[i].setDaemon(this.getDaemon());
                this.acceptors[i].start();
            }
            final Thread timeoutThread = new Thread(new AsyncTimeout(), this.getName() + "-AsyncTimeout");
            timeoutThread.setPriority(this.threadPriority);
            timeoutThread.setDaemon(true);
            timeoutThread.start();
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
            this.unlockAccept();
            for (int i = 0; i < this.acceptors.length; ++i) {
                final long s = System.currentTimeMillis() + 30000L;
                while (this.acceptors[i].isAlive()) {
                    try {
                        this.acceptors[i].interrupt();
                        this.acceptors[i].join(1000L);
                    }
                    catch (InterruptedException ex) {}
                    if (System.currentTimeMillis() >= s) {
                        AprEndpoint.log.warn((Object)AprEndpoint.sm.getString("endpoint.warn.unlockAcceptorFailed", this.acceptors[i].getName()));
                        if (this.serverSock == 0L) {
                            continue;
                        }
                        Socket.shutdown(this.serverSock, 0);
                        this.serverSock = 0L;
                    }
                }
            }
            for (int i = 0; i < this.pollers.length; ++i) {
                try {
                    this.pollers[i].destroy();
                }
                catch (Exception ex2) {}
            }
            this.pollers = null;
            for (int i = 0; i < this.cometPollers.length; ++i) {
                try {
                    this.cometPollers[i].destroy();
                }
                catch (Exception ex3) {}
            }
            this.cometPollers = null;
            if (this.useSendfile) {
                for (int i = 0; i < this.sendfiles.length; ++i) {
                    try {
                        this.sendfiles[i].destroy();
                    }
                    catch (Exception ex4) {}
                }
                this.sendfiles = null;
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
        this.sslContext = 0L;
        if (this.rootPool != 0L) {
            Pool.destroy(this.rootPool);
            this.rootPool = 0L;
        }
        this.handler.recycle();
    }
    
    protected boolean setSocketOptions(final long socket) {
        int step = 1;
        try {
            if (this.socketProperties.getSoLingerOn() && this.socketProperties.getSoLingerTime() >= 0) {
                Socket.optSet(socket, 1, this.socketProperties.getSoLingerTime());
            }
            if (this.socketProperties.getTcpNoDelay()) {
                Socket.optSet(socket, 512, this.socketProperties.getTcpNoDelay() ? 1 : 0);
            }
            if (this.socketProperties.getSoTimeout() > 0) {
                Socket.timeoutSet(socket, this.socketProperties.getSoTimeout() * 1000);
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
                final SocketWrapper<Long> wrapper = new SocketWrapper<Long>(socket);
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
    
    protected boolean processSocket(final long socket) {
        try {
            final SocketWrapper<Long> wrapper = new SocketWrapper<Long>(socket);
            this.getExecutor().execute(new SocketProcessor(wrapper, null));
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
    
    protected boolean processSocket(final long socket, final SocketStatus status) {
        try {
            if (status == SocketStatus.OPEN || status == SocketStatus.STOP || status == SocketStatus.TIMEOUT) {
                final SocketWrapper<Long> wrapper = new SocketWrapper<Long>(socket);
                final SocketEventProcessor proc = new SocketEventProcessor(wrapper, status);
                final ClassLoader loader = Thread.currentThread().getContextClassLoader();
                try {
                    if (Constants.IS_SECURITY_ENABLED) {
                        final PrivilegedAction<Void> pa = new PrivilegedSetTccl(this.getClass().getClassLoader());
                        AccessController.doPrivileged(pa);
                    }
                    else {
                        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
                    }
                    this.getExecutor().execute(proc);
                }
                finally {
                    if (Constants.IS_SECURITY_ENABLED) {
                        final PrivilegedAction<Void> pa2 = new PrivilegedSetTccl(loader);
                        AccessController.doPrivileged(pa2);
                    }
                    else {
                        Thread.currentThread().setContextClassLoader(loader);
                    }
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
    
    public boolean processSocketAsync(final SocketWrapper<Long> socket, final SocketStatus status) {
        try {
            synchronized (socket) {
                if (this.waitingRequests.remove(socket)) {
                    final SocketProcessor proc = new SocketProcessor(socket, status);
                    final ClassLoader loader = Thread.currentThread().getContextClassLoader();
                    try {
                        if (Constants.IS_SECURITY_ENABLED) {
                            final PrivilegedAction<Void> pa = new PrivilegedSetTccl(this.getClass().getClassLoader());
                            AccessController.doPrivileged(pa);
                        }
                        else {
                            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
                        }
                        if (!this.running) {
                            return false;
                        }
                        this.getExecutor().execute(proc);
                    }
                    finally {
                        if (Constants.IS_SECURITY_ENABLED) {
                            final PrivilegedAction<Void> pa2 = new PrivilegedSetTccl(loader);
                            AccessController.doPrivileged(pa2);
                        }
                        else {
                            Thread.currentThread().setContextClassLoader(loader);
                        }
                    }
                }
            }
        }
        catch (RejectedExecutionException x) {
            AprEndpoint.log.warn((Object)("Socket processing request was rejected for: " + socket), (Throwable)x);
            return false;
        }
        catch (Throwable t) {
            ExceptionUtils.handleThrowable(t);
            AprEndpoint.log.error((Object)AprEndpoint.sm.getString("endpoint.process.fail"), t);
            return false;
        }
        return true;
    }
    
    private void destroySocket(final long socket) {
        if (this.running && socket != 0L) {
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
    
    protected class Acceptor extends Thread
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
                    try {
                        Thread.sleep(1000L);
                    }
                    catch (InterruptedException e3) {}
                }
                if (!AprEndpoint.this.running) {
                    break;
                }
                try {
                    AprEndpoint.this.countUpOrAwaitConnection();
                    long socket = 0L;
                    try {
                        socket = Socket.accept(AprEndpoint.this.serverSock);
                    }
                    catch (Exception e) {
                        errorDelay = AprEndpoint.this.handleExceptionWithDelay(errorDelay);
                        throw e;
                    }
                    errorDelay = 0;
                    if (AprEndpoint.this.deferAccept && (AprEndpoint.this.paused || !AprEndpoint.this.running)) {
                        AprEndpoint.this.destroySocket(socket);
                    }
                    else {
                        if (AprEndpoint.this.processSocketWithOptions(socket)) {
                            continue;
                        }
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
        }
    }
    
    protected class AsyncTimeout implements Runnable
    {
        @Override
        public void run() {
            while (AprEndpoint.this.running) {
                try {
                    Thread.sleep(1000L);
                }
                catch (InterruptedException ex) {}
                final long now = System.currentTimeMillis();
                for (final SocketWrapper<Long> socket : AprEndpoint.this.waitingRequests) {
                    if (socket.async) {
                        final long access = socket.getLastAccess();
                        if (socket.getTimeout() <= 0L || now - access <= socket.getTimeout()) {
                            continue;
                        }
                        AprEndpoint.this.processSocketAsync(socket, SocketStatus.TIMEOUT);
                    }
                }
                while (AprEndpoint.this.paused && AprEndpoint.this.running) {
                    try {
                        Thread.sleep(1000L);
                    }
                    catch (InterruptedException e) {}
                }
            }
        }
    }
    
    public class Poller extends Thread
    {
        protected long serverPollset;
        protected long pool;
        protected long[] desc;
        protected long[] addS;
        protected volatile int addCount;
        protected boolean comet;
        protected volatile int keepAliveCount;
        
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
            int size = AprEndpoint.this.getMaxConnections() / AprEndpoint.this.pollerThreadCount;
            int timeout = AprEndpoint.this.getKeepAliveTimeout();
            if (timeout <= 0) {
                timeout = AprEndpoint.this.socketProperties.getSoTimeout();
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
        
        @Override
        public void destroy() {
            for (int i = 0; i < this.addCount; ++i) {
                if (this.comet) {
                    AprEndpoint.this.processSocket(this.addS[i], SocketStatus.STOP);
                }
                else {
                    AprEndpoint.this.destroySocket(this.addS[i]);
                }
            }
            final int rv = Poll.pollset(this.serverPollset, this.desc);
            if (rv > 0) {
                for (int n = 0; n < rv; ++n) {
                    if (this.comet) {
                        AprEndpoint.this.processSocket(this.desc[n * 2 + 1], SocketStatus.STOP);
                    }
                    else {
                        AprEndpoint.this.destroySocket(this.desc[n * 2 + 1]);
                    }
                }
            }
            Pool.destroy(this.pool);
            this.keepAliveCount = 0;
            this.addCount = 0;
            try {
                while (this.isAlive()) {
                    this.interrupt();
                    this.join(1000L);
                }
            }
            catch (InterruptedException ex) {}
        }
        
        public void add(final long socket) {
            synchronized (this) {
                if (this.addCount >= this.addS.length) {
                    if (this.comet) {
                        AprEndpoint.this.processSocket(socket, SocketStatus.ERROR);
                    }
                    else {
                        AprEndpoint.this.destroySocket(socket);
                    }
                    return;
                }
                this.addS[this.addCount] = socket;
                ++this.addCount;
                this.notify();
            }
        }
        
        @Override
        public void run() {
            long maintainTime = 0L;
            while (AprEndpoint.this.running) {
                while (AprEndpoint.this.paused && AprEndpoint.this.running) {
                    try {
                        Thread.sleep(1000L);
                    }
                    catch (InterruptedException e) {}
                }
                if (!AprEndpoint.this.running) {
                    break;
                }
                if (this.keepAliveCount < 1 && this.addCount < 1) {
                    synchronized (this) {
                        while (this.keepAliveCount < 1 && this.addCount < 1 && AprEndpoint.this.running) {
                            maintainTime = 0L;
                            try {
                                this.wait();
                            }
                            catch (InterruptedException e2) {}
                        }
                    }
                }
                if (!AprEndpoint.this.running) {
                    break;
                }
                try {
                    if (this.addCount > 0) {
                        synchronized (this) {
                            int successCount = 0;
                            try {
                                for (int i = this.addCount - 1; i >= 0; --i) {
                                    final int rv = Poll.add(this.serverPollset, this.addS[i], 1);
                                    if (rv == 0) {
                                        ++successCount;
                                    }
                                    else if (this.comet) {
                                        AprEndpoint.this.processSocket(this.addS[i], SocketStatus.ERROR);
                                    }
                                    else {
                                        AprEndpoint.this.destroySocket(this.addS[i]);
                                    }
                                }
                            }
                            finally {
                                this.keepAliveCount += successCount;
                                this.addCount = 0;
                            }
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
                                    AprEndpoint.this.destroySocket(this.desc[n * 2 + 1]);
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
                            AprEndpoint.log.error((Object)AbstractEndpoint.sm.getString("endpoint.poll.fail", "" + errn, Error.strerror(errn)));
                            synchronized (this) {
                                this.destroy();
                                this.init();
                            }
                            continue;
                        }
                    }
                    if (AprEndpoint.this.socketProperties.getSoTimeout() <= 0 || maintainTime <= 1000000L || !AprEndpoint.this.running) {
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
                            AprEndpoint.this.destroySocket(this.desc[n]);
                        }
                    }
                }
                catch (Throwable t) {
                    ExceptionUtils.handleThrowable(t);
                    AprEndpoint.log.error((Object)AbstractEndpoint.sm.getString("endpoint.poll.error"), t);
                }
            }
            synchronized (this) {
                this.notifyAll();
            }
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
    
    public class Sendfile extends Thread
    {
        protected long sendfilePollset;
        protected long pool;
        protected long[] desc;
        protected HashMap<Long, SendfileData> sendfileData;
        protected volatile int sendfileCount;
        protected ArrayList<SendfileData> addS;
        protected volatile int addCount;
        
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
            this.sendfilePollset = AprEndpoint.this.allocatePoller(size, this.pool, AprEndpoint.this.socketProperties.getSoTimeout());
            if (this.sendfilePollset == 0L && size > 1024) {
                size = 1024;
                this.sendfilePollset = AprEndpoint.this.allocatePoller(size, this.pool, AprEndpoint.this.socketProperties.getSoTimeout());
            }
            if (this.sendfilePollset == 0L) {
                size = 62;
                this.sendfilePollset = AprEndpoint.this.allocatePoller(size, this.pool, AprEndpoint.this.socketProperties.getSoTimeout());
            }
            this.desc = new long[size * 2];
            this.sendfileData = new HashMap<Long, SendfileData>(size);
            this.addS = new ArrayList<SendfileData>();
            this.addCount = 0;
        }
        
        @Override
        public void destroy() {
            this.addCount = 0;
            for (int i = this.addS.size() - 1; i >= 0; --i) {
                final SendfileData data = this.addS.get(i);
                AprEndpoint.this.destroySocket(data.socket);
            }
            this.addS.clear();
            final int rv = Poll.pollset(this.sendfilePollset, this.desc);
            if (rv > 0) {
                for (int n = 0; n < rv; ++n) {
                    AprEndpoint.this.destroySocket(this.desc[n * 2 + 1]);
                }
            }
            Pool.destroy(this.pool);
            this.sendfileData.clear();
            try {
                while (this.isAlive()) {
                    this.interrupt();
                    this.join(1000L);
                }
            }
            catch (InterruptedException ex) {}
        }
        
        public boolean add(final SendfileData data) {
            try {
                data.fdpool = Socket.pool(data.socket);
            }
            catch (Exception e) {
                AprEndpoint.log.error((Object)AbstractEndpoint.sm.getString("endpoint.sendfile.error"), (Throwable)e);
                data.socket = 0L;
                return false;
            }
            try {
                data.fd = File.open(data.fileName, 4129, 0, data.fdpool);
                data.pos = data.start;
                Socket.timeoutSet(data.socket, 0L);
                while (true) {
                    final long nw = Socket.sendfilen(data.socket, data.fd, data.pos, data.end - data.pos, 0);
                    if (nw < 0L) {
                        if (-nw != 120002L) {
                            Pool.destroy(data.fdpool);
                            data.socket = 0L;
                            return false;
                        }
                        break;
                    }
                    else {
                        data.pos += nw;
                        if (data.pos >= data.end) {
                            Pool.destroy(data.fdpool);
                            Socket.timeoutSet(data.socket, AprEndpoint.this.socketProperties.getSoTimeout() * 1000);
                            return true;
                        }
                        continue;
                    }
                }
            }
            catch (Exception e) {
                AprEndpoint.log.error((Object)AbstractEndpoint.sm.getString("endpoint.sendfile.error"), (Throwable)e);
                Pool.destroy(data.fdpool);
                data.socket = 0L;
                return false;
            }
            synchronized (this) {
                this.addS.add(data);
                ++this.addCount;
                this.notify();
            }
            return false;
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
            while (AprEndpoint.this.running) {
                while (AprEndpoint.this.paused && AprEndpoint.this.running) {
                    try {
                        Thread.sleep(1000L);
                    }
                    catch (InterruptedException e) {}
                }
                if (!AprEndpoint.this.running) {
                    break;
                }
                if (this.sendfileCount < 1 && this.addCount < 1) {
                    synchronized (this) {
                        while (this.sendfileCount < 1 && this.addS.size() < 1 && AprEndpoint.this.running) {
                            maintainTime = 0L;
                            try {
                                this.wait();
                            }
                            catch (InterruptedException e2) {}
                        }
                    }
                }
                if (!AprEndpoint.this.running) {
                    break;
                }
                try {
                    if (this.addCount > 0) {
                        synchronized (this) {
                            int successCount = 0;
                            try {
                                for (int i = this.addS.size() - 1; i >= 0; --i) {
                                    final SendfileData data = this.addS.get(i);
                                    final int rv = Poll.add(this.sendfilePollset, data.socket, 4);
                                    if (rv == 0) {
                                        this.sendfileData.put(data.socket, data);
                                        ++successCount;
                                    }
                                    else {
                                        AprEndpoint.log.warn((Object)AbstractEndpoint.sm.getString("endpoint.sendfile.addfail", "" + rv, Error.strerror(rv)));
                                        AprEndpoint.this.destroySocket(data.socket);
                                    }
                                }
                            }
                            finally {
                                this.sendfileCount += successCount;
                                this.addS.clear();
                                this.addCount = 0;
                            }
                        }
                    }
                    maintainTime += AprEndpoint.this.pollTime;
                    int rv2 = Poll.poll(this.sendfilePollset, AprEndpoint.this.pollTime, this.desc, false);
                    if (rv2 > 0) {
                        for (int n = 0; n < rv2; ++n) {
                            final SendfileData state = this.sendfileData.get(this.desc[n * 2 + 1]);
                            if ((this.desc[n * 2] & 0x20L) == 0x20L || (this.desc[n * 2] & 0x10L) == 0x10L) {
                                this.remove(state);
                                AprEndpoint.this.destroySocket(state.socket);
                            }
                            else {
                                final long nw = Socket.sendfilen(state.socket, state.fd, state.pos, state.end - state.pos, 0);
                                if (nw < 0L) {
                                    this.remove(state);
                                    AprEndpoint.this.destroySocket(state.socket);
                                }
                                else {
                                    state.pos += nw;
                                    if (state.pos >= state.end) {
                                        this.remove(state);
                                        if (state.keepAlive) {
                                            Pool.destroy(state.fdpool);
                                            Socket.timeoutSet(state.socket, AprEndpoint.this.socketProperties.getSoTimeout() * 1000);
                                            AprEndpoint.this.getPoller().add(state.socket);
                                        }
                                        else {
                                            AprEndpoint.this.destroySocket(state.socket);
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
                            AprEndpoint.log.error((Object)AbstractEndpoint.sm.getString("endpoint.poll.fail", "" + errn, Error.strerror(errn)));
                            synchronized (this) {
                                this.destroy();
                                this.init();
                            }
                            continue;
                        }
                    }
                    if (AprEndpoint.this.socketProperties.getSoTimeout() <= 0 || maintainTime <= 1000000L || !AprEndpoint.this.running) {
                        continue;
                    }
                    rv2 = Poll.maintain(this.sendfilePollset, this.desc, true);
                    maintainTime = 0L;
                    if (rv2 <= 0) {
                        continue;
                    }
                    for (int n = 0; n < rv2; ++n) {
                        final SendfileData state = this.sendfileData.get(this.desc[n]);
                        this.remove(state);
                        AprEndpoint.this.destroySocket(state.socket);
                    }
                }
                catch (Throwable t) {
                    ExceptionUtils.handleThrowable(t);
                    AprEndpoint.log.error((Object)AbstractEndpoint.sm.getString("endpoint.poll.error"), t);
                }
            }
            synchronized (this) {
                this.notifyAll();
            }
        }
    }
    
    protected class SocketWithOptionsProcessor implements Runnable
    {
        protected SocketWrapper<Long> socket;
        
        public SocketWithOptionsProcessor(final SocketWrapper<Long> socket) {
            this.socket = null;
            this.socket = socket;
        }
        
        @Override
        public void run() {
            synchronized (this.socket) {
                if (!AprEndpoint.this.deferAccept) {
                    if (AprEndpoint.this.setSocketOptions(this.socket.getSocket())) {
                        AprEndpoint.this.getPoller().add(this.socket.getSocket());
                    }
                    else {
                        AprEndpoint.this.destroySocket(this.socket.getSocket());
                        this.socket = null;
                    }
                }
                else {
                    if (!AprEndpoint.this.setSocketOptions(this.socket.getSocket())) {
                        AprEndpoint.this.destroySocket(this.socket.getSocket());
                        this.socket = null;
                        return;
                    }
                    final AbstractEndpoint.Handler.SocketState state = AprEndpoint.this.handler.process(this.socket, SocketStatus.OPEN);
                    if (state == AbstractEndpoint.Handler.SocketState.CLOSED) {
                        AprEndpoint.this.destroySocket(this.socket.getSocket());
                        this.socket = null;
                    }
                    else if (state == AbstractEndpoint.Handler.SocketState.LONG) {
                        this.socket.access();
                        if (this.socket.async) {
                            AprEndpoint.this.waitingRequests.add(this.socket);
                        }
                    }
                }
            }
        }
    }
    
    protected class SocketProcessor implements Runnable
    {
        protected SocketWrapper<Long> socket;
        protected SocketStatus status;
        
        public SocketProcessor(final SocketWrapper<Long> socket, final SocketStatus status) {
            this.socket = null;
            this.status = null;
            this.socket = socket;
            this.status = status;
        }
        
        @Override
        public void run() {
            synchronized (this.socket) {
                AbstractEndpoint.Handler.SocketState state = AbstractEndpoint.Handler.SocketState.OPEN;
                if (this.status == null) {
                    state = AprEndpoint.this.handler.process(this.socket, SocketStatus.OPEN);
                }
                else {
                    state = AprEndpoint.this.handler.process(this.socket, this.status);
                }
                if (state == AbstractEndpoint.Handler.SocketState.CLOSED) {
                    AprEndpoint.this.destroySocket(this.socket.getSocket());
                    this.socket = null;
                }
                else if (state == AbstractEndpoint.Handler.SocketState.LONG) {
                    this.socket.access();
                    if (this.socket.async) {
                        AprEndpoint.this.waitingRequests.add(this.socket);
                    }
                }
                else if (state == AbstractEndpoint.Handler.SocketState.ASYNC_END) {
                    this.socket.access();
                    final SocketProcessor proc = new SocketProcessor(this.socket, SocketStatus.OPEN);
                    AprEndpoint.this.getExecutor().execute(proc);
                }
            }
        }
    }
    
    protected class SocketEventProcessor implements Runnable
    {
        protected SocketWrapper<Long> socket;
        protected SocketStatus status;
        
        public SocketEventProcessor(final SocketWrapper<Long> socket, final SocketStatus status) {
            this.socket = null;
            this.status = null;
            this.socket = socket;
            this.status = status;
        }
        
        @Override
        public void run() {
            synchronized (this.socket) {
                final AbstractEndpoint.Handler.SocketState state = AprEndpoint.this.handler.process(this.socket, this.status);
                if (state == AbstractEndpoint.Handler.SocketState.CLOSED) {
                    AprEndpoint.this.destroySocket(this.socket.getSocket());
                    this.socket = null;
                }
            }
        }
    }
    
    private static class PrivilegedSetTccl implements PrivilegedAction<Void>
    {
        private ClassLoader cl;
        
        PrivilegedSetTccl(final ClassLoader cl) {
            this.cl = cl;
        }
        
        @Override
        public Void run() {
            Thread.currentThread().setContextClassLoader(this.cl);
            return null;
        }
    }
    
    public interface Handler extends AbstractEndpoint.Handler
    {
        SocketState process(final SocketWrapper<Long> p0, final SocketStatus p1);
    }
}
