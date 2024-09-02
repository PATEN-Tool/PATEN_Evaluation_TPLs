// 
// Decompiled by Procyon v0.5.36
// 

package org.jboss.resteasy.plugins.server.tjws;

import Acme.Serve.Serve;
import Acme.Serve.SSLAcceptor;
import org.jboss.resteasy.resteasy_jaxrs.i18n.Messages;
import java.io.File;
import java.util.Map;
import java.util.Hashtable;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import java.util.Properties;

public class TJWSServletServer
{
    protected FileMappingServe server;
    protected Properties props;
    
    public TJWSServletServer() {
        this.server = new FileMappingServe();
        this.props = new Properties();
    }
    
    public void addServlet(final String bindPath, final HttpServlet servlet) {
        this.server.addServlet(bindPath, (Servlet)servlet);
    }
    
    public void addServlet(final String bindPath, final HttpServlet servlet, final Hashtable initParams) {
        this.server.addServlet(bindPath, (Servlet)servlet, initParams);
    }
    
    public void addServlet(final String bindPath, final HttpServlet servlet, final Hashtable<String, String> initParams, final Hashtable<String, String> contextParams) {
        this.server.setInitParams(contextParams);
        this.server.addServlet(bindPath, (Servlet)servlet, (Hashtable)initParams);
    }
    
    public void setProps(final Properties props) {
        this.props.putAll(props);
    }
    
    public void setPort(final int port) {
        ((Hashtable<String, Integer>)this.props).put("port", port);
    }
    
    public void setBindAddress(final String address) {
        ((Hashtable<String, String>)this.props).put("bind-address", address);
    }
    
    public void setSessionTimeout(final long timeout) {
        ((Hashtable<String, String>)this.props).put("session-timeout", Long.toString(timeout));
    }
    
    public void setKeepAlive(final boolean keepAlive) {
        ((Hashtable<String, String>)this.props).put("keep-alive", Boolean.toString(keepAlive));
    }
    
    public void setKeepAliveTimeout(final long timeout) {
        ((Hashtable<String, String>)this.props).put("timeout-keep-alive", Long.toString(timeout));
    }
    
    public void setMaxKeepAliveConnections(final int max) {
        ((Hashtable<String, String>)this.props).put("max-alive-conn-use", Integer.toString(max));
    }
    
    public void setThreadPoolSize(final int max) {
        ((Hashtable<String, String>)this.props).put("Acme.Utils.ThreadPool.maxpooledthreads", Integer.toString(max));
    }
    
    public void setSSLAlgorithm(final String algorithm) {
        ((Hashtable<String, String>)this.props).put("algorithm", algorithm);
    }
    
    public void setSSLKeyStoreFile(final String path) {
        ((Hashtable<String, String>)this.props).put("keystoreFile", path);
    }
    
    public void setSSLKeyStorePass(final String passwd) {
        ((Hashtable<String, String>)this.props).put("keystorePass", passwd);
    }
    
    public void setSSLKeyStoreType(final String type) {
        ((Hashtable<String, String>)this.props).put("keystoreType", type);
    }
    
    public void setSSLProtocol(final String protocol) {
        ((Hashtable<String, String>)this.props).put("protocol", protocol);
    }
    
    public void setSSLPort(final int port) {
        ((Hashtable<String, String>)this.props).put("ssl-port", Integer.toString(port));
    }
    
    public void addFileMapping(final String context, final File directory) {
        this.server.addFileMapping(context, directory);
    }
    
    public void start() {
        if (this.props == null) {
            this.props = new Properties();
        }
        if (!this.props.containsKey("port") && !this.props.containsKey("ssl-port")) {
            throw new RuntimeException(Messages.MESSAGES.mustSetPort());
        }
        if (this.props.containsKey("port") && this.props.containsKey("ssl-port")) {
            throw new RuntimeException(Messages.MESSAGES.mustSetEitherPortOrSSLPort());
        }
        if (this.props.containsKey("ssl-port")) {
            ((Hashtable<String, String>)this.props).put("acceptorImpl", SSLAcceptor.class.getName());
        }
        this.props.setProperty("nohup", "nohup");
        this.server.arguments = this.props;
        this.server.initFileMappings();
        this.server.runInBackground();
    }
    
    public void stop() {
        this.server.stopBackground();
    }
    
    public static class FileMappingServe extends Serve
    {
        private static final long serialVersionUID = -5031104686755790970L;
        private Serve.PathTreeDictionary mappingTable;
        private Hashtable<String, String> initParams;
        
        public FileMappingServe() {
            this.mappingTable = null;
            this.initParams = null;
        }
        
        public void initFileMappings() {
            this.addDefaultServlets((String)null);
            if (this.mappingTable != null) {
                super.setMappingTable(this.mappingTable);
            }
        }
        
        public void addFileMapping(final String context, final File directory) {
            if (this.mappingTable == null) {
                this.mappingTable = new Serve.PathTreeDictionary();
            }
            this.mappingTable.put(context, (Object)directory);
        }
        
        public String getInitParameter(final String param) {
            if (this.initParams == null) {
                return null;
            }
            return this.initParams.get(param);
        }
        
        public Hashtable<String, String> getInitParams() {
            return this.initParams;
        }
        
        public void setInitParams(final Hashtable<String, String> initParams) {
            this.initParams = initParams;
        }
    }
}
