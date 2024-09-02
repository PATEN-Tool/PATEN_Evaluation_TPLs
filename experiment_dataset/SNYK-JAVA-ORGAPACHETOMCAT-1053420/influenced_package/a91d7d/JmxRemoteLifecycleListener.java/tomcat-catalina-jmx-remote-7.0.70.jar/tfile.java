// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.catalina.mbeans;

import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.net.InetAddress;
import java.net.Socket;
import java.io.Serializable;
import org.apache.juli.logging.LogFactory;
import java.rmi.registry.Registry;
import java.rmi.AlreadyBoundException;
import java.io.IOException;
import java.rmi.Remote;
import javax.management.remote.rmi.RMIServerImpl;
import javax.management.remote.rmi.RMIConnectorServer;
import java.lang.management.ManagementFactory;
import java.util.Map;
import javax.management.remote.rmi.RMIJRMPServerImpl;
import java.net.MalformedURLException;
import javax.management.remote.JMXServiceURL;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.RMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import java.util.HashMap;
import org.apache.catalina.LifecycleEvent;
import javax.management.remote.JMXConnectorServer;
import org.apache.tomcat.util.res.StringManager;
import org.apache.juli.logging.Log;
import org.apache.catalina.LifecycleListener;

public class JmxRemoteLifecycleListener implements LifecycleListener
{
    private static final Log log;
    protected static final StringManager sm;
    protected String rmiBindAddress;
    protected int rmiRegistryPortPlatform;
    protected int rmiServerPortPlatform;
    protected boolean rmiRegistrySSL;
    protected boolean rmiServerSSL;
    protected String[] ciphers;
    protected String[] protocols;
    protected boolean clientAuth;
    protected boolean authenticate;
    protected String passwordFile;
    protected String loginModuleName;
    protected String accessFile;
    protected boolean useLocalPorts;
    protected JMXConnectorServer csPlatform;
    
    public JmxRemoteLifecycleListener() {
        this.rmiBindAddress = null;
        this.rmiRegistryPortPlatform = -1;
        this.rmiServerPortPlatform = -1;
        this.rmiRegistrySSL = true;
        this.rmiServerSSL = true;
        this.ciphers = null;
        this.protocols = null;
        this.clientAuth = true;
        this.authenticate = true;
        this.passwordFile = null;
        this.loginModuleName = null;
        this.accessFile = null;
        this.useLocalPorts = false;
        this.csPlatform = null;
    }
    
    public String getRmiBindAddress() {
        return this.rmiBindAddress;
    }
    
    public void setRmiBindAddress(final String theRmiBindAddress) {
        this.rmiBindAddress = theRmiBindAddress;
    }
    
    public int getRmiServerPortPlatform() {
        return this.rmiServerPortPlatform;
    }
    
    public void setRmiServerPortPlatform(final int theRmiServerPortPlatform) {
        this.rmiServerPortPlatform = theRmiServerPortPlatform;
    }
    
    public int getRmiRegistryPortPlatform() {
        return this.rmiRegistryPortPlatform;
    }
    
    public void setRmiRegistryPortPlatform(final int theRmiRegistryPortPlatform) {
        this.rmiRegistryPortPlatform = theRmiRegistryPortPlatform;
    }
    
    public boolean getUseLocalPorts() {
        return this.useLocalPorts;
    }
    
    public void setUseLocalPorts(final boolean useLocalPorts) {
        this.useLocalPorts = useLocalPorts;
    }
    
    private void init() {
        final String rmiRegistrySSLValue = System.getProperty("com.sun.management.jmxremote.registry.ssl", "false");
        this.rmiRegistrySSL = Boolean.parseBoolean(rmiRegistrySSLValue);
        final String rmiServerSSLValue = System.getProperty("com.sun.management.jmxremote.ssl", "true");
        this.rmiServerSSL = Boolean.parseBoolean(rmiServerSSLValue);
        final String protocolsValue = System.getProperty("com.sun.management.jmxremote.ssl.enabled.protocols");
        if (protocolsValue != null) {
            this.protocols = protocolsValue.split(",");
        }
        final String ciphersValue = System.getProperty("com.sun.management.jmxremote.ssl.enabled.cipher.suites");
        if (ciphersValue != null) {
            this.ciphers = ciphersValue.split(",");
        }
        final String clientAuthValue = System.getProperty("com.sun.management.jmxremote.ssl.need.client.auth", "true");
        this.clientAuth = Boolean.parseBoolean(clientAuthValue);
        final String authenticateValue = System.getProperty("com.sun.management.jmxremote.authenticate", "true");
        this.authenticate = Boolean.parseBoolean(authenticateValue);
        this.passwordFile = System.getProperty("com.sun.management.jmxremote.password.file", "jmxremote.password");
        this.accessFile = System.getProperty("com.sun.management.jmxremote.access.file", "jmxremote.access");
        this.loginModuleName = System.getProperty("com.sun.management.jmxremote.login.config");
    }
    
    public void lifecycleEvent(final LifecycleEvent event) {
        if ("start".equals(event.getType())) {
            this.init();
            System.setProperty("java.rmi.server.randomIDs", "true");
            final HashMap<String, Object> env = new HashMap<String, Object>();
            RMIClientSocketFactory registryCsf = null;
            RMIServerSocketFactory registrySsf = null;
            RMIClientSocketFactory serverCsf = null;
            RMIServerSocketFactory serverSsf = null;
            if (this.rmiRegistrySSL) {
                registryCsf = new SslRMIClientSocketFactory();
                if (this.rmiBindAddress == null) {
                    registrySsf = new SslRMIServerSocketFactory(this.ciphers, this.protocols, this.clientAuth);
                }
                else {
                    registrySsf = new SslRmiServerBindSocketFactory(this.ciphers, this.protocols, this.clientAuth, this.rmiBindAddress);
                }
            }
            else if (this.rmiBindAddress != null) {
                registrySsf = new RmiServerBindSocketFactory(this.rmiBindAddress);
            }
            if (this.rmiServerSSL) {
                serverCsf = new SslRMIClientSocketFactory();
                if (this.rmiBindAddress == null) {
                    serverSsf = new SslRMIServerSocketFactory(this.ciphers, this.protocols, this.clientAuth);
                }
                else {
                    serverSsf = new SslRmiServerBindSocketFactory(this.ciphers, this.protocols, this.clientAuth, this.rmiBindAddress);
                }
            }
            else if (this.rmiBindAddress != null) {
                serverSsf = new RmiServerBindSocketFactory(this.rmiBindAddress);
            }
            if (this.rmiBindAddress != null) {
                System.setProperty("java.rmi.server.hostname", this.rmiBindAddress);
            }
            if (this.useLocalPorts) {
                registryCsf = new RmiClientLocalhostSocketFactory(registryCsf);
                serverCsf = new RmiClientLocalhostSocketFactory(serverCsf);
            }
            if (serverCsf != null) {
                env.put("jmx.remote.rmi.client.socket.factory", serverCsf);
                env.put("com.sun.jndi.rmi.factory.socket", registryCsf);
            }
            if (serverSsf != null) {
                env.put("jmx.remote.rmi.server.socket.factory", serverSsf);
            }
            if (this.authenticate) {
                env.put("jmx.remote.x.password.file", this.passwordFile);
                env.put("jmx.remote.x.access.file", this.accessFile);
                env.put("jmx.remote.x.login.config", this.loginModuleName);
            }
            this.csPlatform = this.createServer("Platform", this.rmiBindAddress, this.rmiRegistryPortPlatform, this.rmiServerPortPlatform, env, registryCsf, registrySsf, serverCsf, serverSsf);
        }
        else if ("stop".equals(event.getType())) {
            this.destroyServer("Platform", this.csPlatform);
        }
    }
    
    private JMXConnectorServer createServer(final String serverName, String bindAddress, final int theRmiRegistryPort, final int theRmiServerPort, final HashMap<String, Object> theEnv, final RMIClientSocketFactory registryCsf, final RMIServerSocketFactory registrySsf, final RMIClientSocketFactory serverCsf, final RMIServerSocketFactory serverSsf) {
        Registry registry;
        try {
            registry = LocateRegistry.createRegistry(theRmiRegistryPort, registryCsf, registrySsf);
        }
        catch (RemoteException e) {
            JmxRemoteLifecycleListener.log.error((Object)JmxRemoteLifecycleListener.sm.getString("jmxRemoteLifecycleListener.createRegistryFailed", new Object[] { serverName, Integer.toString(theRmiRegistryPort) }), (Throwable)e);
            return null;
        }
        if (bindAddress == null) {
            bindAddress = "localhost";
        }
        final String url = "service:jmx:rmi://" + bindAddress;
        JMXServiceURL serviceUrl;
        try {
            serviceUrl = new JMXServiceURL(url);
        }
        catch (MalformedURLException e2) {
            JmxRemoteLifecycleListener.log.error((Object)JmxRemoteLifecycleListener.sm.getString("jmxRemoteLifecycleListener.invalidURL", new Object[] { serverName, url }), (Throwable)e2);
            return null;
        }
        RMIConnectorServer cs = null;
        try {
            final RMIJRMPServerImpl server = new RMIJRMPServerImpl(this.rmiServerPortPlatform, serverCsf, serverSsf, theEnv);
            cs = new RMIConnectorServer(serviceUrl, theEnv, server, ManagementFactory.getPlatformMBeanServer());
            cs.start();
            registry.bind("jmxrmi", server);
            JmxRemoteLifecycleListener.log.info((Object)JmxRemoteLifecycleListener.sm.getString("jmxRemoteLifecycleListener.start", new Object[] { Integer.toString(theRmiRegistryPort), Integer.toString(theRmiServerPort), serverName }));
        }
        catch (IOException e3) {
            JmxRemoteLifecycleListener.log.error((Object)JmxRemoteLifecycleListener.sm.getString("jmxRemoteLifecycleListener.createServerFailed", new Object[] { serverName }), (Throwable)e3);
        }
        catch (AlreadyBoundException e4) {
            JmxRemoteLifecycleListener.log.error((Object)JmxRemoteLifecycleListener.sm.getString("jmxRemoteLifecycleListener.createServerFailed", new Object[] { serverName }), (Throwable)e4);
        }
        return cs;
    }
    
    private void destroyServer(final String serverName, final JMXConnectorServer theConnectorServer) {
        if (theConnectorServer != null) {
            try {
                theConnectorServer.stop();
            }
            catch (IOException e) {
                JmxRemoteLifecycleListener.log.error((Object)JmxRemoteLifecycleListener.sm.getString("jmxRemoteLifecycleListener.destroyServerFailed", new Object[] { serverName }), (Throwable)e);
            }
        }
    }
    
    static {
        log = LogFactory.getLog((Class)JmxRemoteLifecycleListener.class);
        sm = StringManager.getManager("org.apache.catalina.mbeans");
    }
    
    public static class RmiClientLocalhostSocketFactory implements RMIClientSocketFactory, Serializable
    {
        private static final long serialVersionUID = 1L;
        private static final String FORCED_HOST = "localhost";
        private RMIClientSocketFactory factory;
        
        public RmiClientLocalhostSocketFactory(final RMIClientSocketFactory theFactory) {
            this.factory = null;
            this.factory = theFactory;
        }
        
        @Override
        public Socket createSocket(final String host, final int port) throws IOException {
            if (this.factory == null) {
                return new Socket("localhost", port);
            }
            return this.factory.createSocket("localhost", port);
        }
    }
    
    public static class RmiServerBindSocketFactory implements RMIServerSocketFactory
    {
        private final InetAddress bindAddress;
        
        public RmiServerBindSocketFactory(final String address) {
            InetAddress bindAddress = null;
            try {
                bindAddress = InetAddress.getByName(address);
            }
            catch (UnknownHostException e) {
                JmxRemoteLifecycleListener.log.error((Object)JmxRemoteLifecycleListener.sm.getString("jmxRemoteLifecycleListener.invalidRmiBindAddress", new Object[] { address }), (Throwable)e);
            }
            this.bindAddress = bindAddress;
        }
        
        @Override
        public ServerSocket createServerSocket(final int port) throws IOException {
            return new ServerSocket(port, 0, this.bindAddress);
        }
    }
    
    public static class SslRmiServerBindSocketFactory extends SslRMIServerSocketFactory
    {
        private static final SSLServerSocketFactory sslServerSocketFactory;
        private static final String[] defaultProtocols;
        private final InetAddress bindAddress;
        
        public SslRmiServerBindSocketFactory(final String[] enabledCipherSuites, final String[] enabledProtocols, final boolean needClientAuth, final String address) {
            super(enabledCipherSuites, enabledProtocols, needClientAuth);
            InetAddress bindAddress = null;
            try {
                bindAddress = InetAddress.getByName(address);
            }
            catch (UnknownHostException e) {
                JmxRemoteLifecycleListener.log.error((Object)JmxRemoteLifecycleListener.sm.getString("jmxRemoteLifecycleListener.invalidRmiBindAddress", new Object[] { address }), (Throwable)e);
            }
            this.bindAddress = bindAddress;
        }
        
        @Override
        public ServerSocket createServerSocket(final int port) throws IOException {
            final SSLServerSocket sslServerSocket = (SSLServerSocket)SslRmiServerBindSocketFactory.sslServerSocketFactory.createServerSocket(port, 0, this.bindAddress);
            if (this.getEnabledCipherSuites() != null) {
                sslServerSocket.setEnabledCipherSuites(this.getEnabledCipherSuites());
            }
            if (this.getEnabledProtocols() == null) {
                sslServerSocket.setEnabledProtocols(SslRmiServerBindSocketFactory.defaultProtocols);
            }
            else {
                sslServerSocket.setEnabledProtocols(this.getEnabledProtocols());
            }
            sslServerSocket.setNeedClientAuth(this.getNeedClientAuth());
            return sslServerSocket;
        }
        
        static {
            SSLContext sslContext;
            try {
                sslContext = SSLContext.getDefault();
            }
            catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            }
            sslServerSocketFactory = sslContext.getServerSocketFactory();
            final String[] protocols = sslContext.getDefaultSSLParameters().getProtocols();
            final List<String> filteredProtocols = new ArrayList<String>(protocols.length);
            for (final String protocol : protocols) {
                if (!protocol.toUpperCase(Locale.ENGLISH).contains("SSL")) {
                    filteredProtocols.add(protocol);
                }
            }
            defaultProtocols = filteredProtocols.toArray(new String[filteredProtocols.size()]);
        }
    }
}
