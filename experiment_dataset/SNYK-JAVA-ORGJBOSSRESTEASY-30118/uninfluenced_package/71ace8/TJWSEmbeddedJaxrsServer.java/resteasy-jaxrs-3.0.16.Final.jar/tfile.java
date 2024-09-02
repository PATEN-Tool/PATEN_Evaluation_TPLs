// 
// Decompiled by Procyon v0.5.36
// 

package org.jboss.resteasy.plugins.server.tjws;

import org.jboss.resteasy.plugins.server.embedded.SecurityDomain;
import javax.servlet.http.HttpServlet;
import java.util.Hashtable;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.resteasy.plugins.server.embedded.EmbeddedJaxrsServer;

public class TJWSEmbeddedJaxrsServer extends TJWSServletServer implements EmbeddedJaxrsServer
{
    protected ResteasyDeployment deployment;
    protected TJWSServletDispatcher servlet;
    protected String rootResourcePath;
    protected Hashtable<String, String> initParameters;
    protected Hashtable<String, String> contextParameters;
    
    @Override
    public void setRootResourcePath(final String rootResourcePath) {
        this.rootResourcePath = rootResourcePath;
    }
    
    public TJWSEmbeddedJaxrsServer() {
        this.deployment = new ResteasyDeployment();
        this.servlet = new TJWSServletDispatcher();
        this.rootResourcePath = "";
    }
    
    @Override
    public ResteasyDeployment getDeployment() {
        return this.deployment;
    }
    
    @Override
    public void setDeployment(final ResteasyDeployment deployment) {
        this.deployment = deployment;
    }
    
    public void setInitParameters(final Hashtable<String, String> initParameters) {
        this.initParameters = initParameters;
    }
    
    public void setContextParameters(final Hashtable<String, String> contextParameters) {
        this.contextParameters = contextParameters;
    }
    
    @Override
    public void start() {
        this.server.setAttribute(ResteasyDeployment.class.getName(), (Object)this.deployment);
        this.addServlet(this.rootResourcePath, this.servlet, this.initParameters, this.contextParameters);
        this.servlet.setContextPath(this.rootResourcePath);
        super.start();
    }
    
    @Override
    public void setSecurityDomain(final SecurityDomain sc) {
        this.servlet.setSecurityDomain(sc);
    }
    
    public String getProperty(final String key) {
        return this.props.getProperty(key);
    }
    
    public String getPort() {
        return ((Hashtable<K, Object>)this.props).get("port").toString();
    }
}
