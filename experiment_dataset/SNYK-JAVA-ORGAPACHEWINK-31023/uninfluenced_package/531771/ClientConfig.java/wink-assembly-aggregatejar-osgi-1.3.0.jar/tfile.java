// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.wink.client;

import java.util.Hashtable;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.Collection;
import org.apache.wink.client.internal.handlers.HttpURLConnectionHandler;
import org.apache.wink.client.handlers.ConnectionHandler;
import org.apache.wink.client.internal.handlers.AcceptHeaderHandler;
import java.util.Collections;
import java.util.List;
import org.apache.wink.common.internal.i18n.Messages;
import java.security.PrivilegedActionException;
import java.security.AccessController;
import java.io.FileNotFoundException;
import java.util.Set;
import org.apache.wink.common.WinkApplication;
import org.apache.wink.common.internal.application.ApplicationFileLoader;
import java.security.PrivilegedExceptionAction;
import java.util.Properties;
import javax.ws.rs.core.Application;
import org.apache.wink.client.handlers.ClientHandler;
import java.util.LinkedList;
import org.slf4j.Logger;
import org.apache.wink.common.internal.WinkConfiguration;

public class ClientConfig implements Cloneable, WinkConfiguration
{
    private static final Logger logger;
    private String proxyHost;
    private int proxyPort;
    private boolean followRedirects;
    private LinkedList<ClientHandler> handlers;
    private LinkedList<Application> applications;
    private boolean modifiable;
    private boolean isAcceptHeaderAutoSet;
    private boolean loadWinkApplications;
    private boolean bypassHostnameVerification;
    private static final String WINK_CLIENT_CONNECTTIMEOUT = "wink.client.connectTimeout";
    private static final String WINK_CLIENT_READTIMEOUT = "wink.client.readTimeout";
    private static final String WINK_SUPPORT_DTD_EXPANSION = "wink.supportDTDEntityExpansion";
    private static int WINK_CLIENT_CONNECTTIMEOUT_DEFAULT;
    private static int WINK_CLIENT_READTIMEOUT_DEFAULT;
    private static boolean WINK_CLIENT_SUPPORT_DTD_EXPANSION_DEFAULT;
    private Properties properties;
    
    public ClientConfig() {
        this.loadWinkApplications = true;
        this.bypassHostnameVerification = false;
        this.properties = null;
        this.modifiable = true;
        this.proxyHost = null;
        this.proxyPort = 80;
        this.followRedirects = true;
        this.isAcceptHeaderAutoSet = true;
        this.handlers = new LinkedList<ClientHandler>();
    }
    
    private void initDefaultApplication() {
        if (this.applications != null) {
            return;
        }
        this.applications = new LinkedList<Application>();
        try {
            AccessController.doPrivileged((PrivilegedExceptionAction<Object>)new PrivilegedExceptionAction<Object>() {
                public Object run() throws FileNotFoundException {
                    final Set<Class<?>> classes = new ApplicationFileLoader(ClientConfig.this.loadWinkApplications).getClasses();
                    ClientConfig.this.applications(new WinkApplication() {
                        @Override
                        public Set<Class<?>> getClasses() {
                            return classes;
                        }
                        
                        @Override
                        public double getPriority() {
                            return 0.1;
                        }
                    });
                    return null;
                }
            });
        }
        catch (PrivilegedActionException e) {
            throw new ClientConfigException(e.getException());
        }
    }
    
    public final String getProxyHost() {
        return this.proxyHost;
    }
    
    public final ClientConfig proxyHost(final String proxyHost) {
        if (!this.modifiable) {
            throw new ClientConfigException(Messages.getMessage("clientConfigurationUnmodifiable"));
        }
        this.proxyHost = proxyHost;
        return this;
    }
    
    public final int getProxyPort() {
        return this.proxyPort;
    }
    
    public final ClientConfig proxyPort(int proxyPort) {
        if (!this.modifiable) {
            throw new ClientConfigException(Messages.getMessage("clientConfigurationUnmodifiable"));
        }
        if (proxyPort <= 0) {
            proxyPort = 80;
        }
        this.proxyPort = proxyPort;
        return this;
    }
    
    public final int getConnectTimeout() {
        try {
            return Integer.valueOf(this.getProperties().getProperty("wink.client.connectTimeout"));
        }
        catch (NumberFormatException e) {
            ClientConfig.logger.trace("Value in properties for key {} is invalid.  Reverting to default: {}", (Object)"wink.client.connectTimeout", (Object)ClientConfig.WINK_CLIENT_CONNECTTIMEOUT_DEFAULT);
            this.getProperties().setProperty("wink.client.connectTimeout", String.valueOf(ClientConfig.WINK_CLIENT_CONNECTTIMEOUT_DEFAULT));
            return this.getReadTimeout();
        }
    }
    
    public final ClientConfig connectTimeout(final int connectTimeout) {
        if (!this.modifiable) {
            throw new ClientConfigException(Messages.getMessage("clientConfigurationUnmodifiable"));
        }
        this.getProperties().setProperty("wink.client.connectTimeout", String.valueOf(connectTimeout));
        return this;
    }
    
    public final int getReadTimeout() {
        try {
            return Integer.valueOf(this.getProperties().getProperty("wink.client.readTimeout"));
        }
        catch (NumberFormatException e) {
            ClientConfig.logger.trace("Value in properties for key {} is invalid.  Reverting to default: {}", (Object)"wink.client.readTimeout", (Object)ClientConfig.WINK_CLIENT_READTIMEOUT_DEFAULT);
            this.getProperties().setProperty("wink.client.readTimeout", String.valueOf(ClientConfig.WINK_CLIENT_READTIMEOUT_DEFAULT));
            return this.getReadTimeout();
        }
    }
    
    public final ClientConfig readTimeout(final int readTimeout) {
        if (!this.modifiable) {
            throw new ClientConfigException(Messages.getMessage("clientConfigurationUnmodifiable"));
        }
        this.getProperties().setProperty("wink.client.readTimeout", String.valueOf(readTimeout));
        return this;
    }
    
    public final boolean isSupportDTDExpansion() {
        return Boolean.valueOf(this.getProperties().getProperty("wink.supportDTDEntityExpansion"));
    }
    
    public final ClientConfig supportDTDExpansion(final boolean supportDTDExpansion) {
        if (!this.modifiable) {
            throw new ClientConfigException(Messages.getMessage("clientConfigurationUnmodifiable"));
        }
        this.getProperties().setProperty("wink.supportDTDEntityExpansion", String.valueOf(supportDTDExpansion));
        return this;
    }
    
    public final boolean isFollowRedirects() {
        return this.followRedirects;
    }
    
    public final ClientConfig followRedirects(final boolean followRedirects) {
        if (!this.modifiable) {
            throw new ClientConfigException(Messages.getMessage("clientConfigurationUnmodifiable"));
        }
        this.followRedirects = followRedirects;
        return this;
    }
    
    public final boolean isAcceptHeaderAutoSet() {
        return this.isAcceptHeaderAutoSet;
    }
    
    public final ClientConfig acceptHeaderAutoSet(final boolean isAcceptHeaderAutoSet) {
        if (!this.modifiable) {
            throw new ClientConfigException(Messages.getMessage("clientConfigurationUnmodifiable"));
        }
        this.isAcceptHeaderAutoSet = isAcceptHeaderAutoSet;
        return this;
    }
    
    public final List<ClientHandler> getHandlers() {
        return Collections.unmodifiableList((List<? extends ClientHandler>)this.handlers);
    }
    
    public final ClientConfig handlers(final ClientHandler... handlers) {
        if (!this.modifiable) {
            throw new ClientConfigException(Messages.getMessage("clientConfigurationUnmodifiable"));
        }
        for (final ClientHandler handler : handlers) {
            this.handlers.add(handler);
        }
        return this;
    }
    
    ClientConfig build() {
        if (this.isAcceptHeaderAutoSet) {
            this.handlers.add(new AcceptHeaderHandler());
        }
        this.handlers.add(this.getConnectionHandler());
        this.modifiable = false;
        return this;
    }
    
    protected ConnectionHandler getConnectionHandler() {
        return new HttpURLConnectionHandler();
    }
    
    public final List<Application> getApplications() {
        if (this.applications == null) {
            this.initDefaultApplication();
        }
        return Collections.unmodifiableList((List<? extends Application>)this.applications);
    }
    
    public final ClientConfig applications(final Application... applications) {
        if (!this.modifiable) {
            throw new ClientConfigException(Messages.getMessage("clientConfigurationUnmodifiable"));
        }
        if (this.applications == null) {
            this.initDefaultApplication();
        }
        for (final Application application : applications) {
            this.applications.add(application);
        }
        return this;
    }
    
    @Override
    protected ClientConfig clone() {
        if (this.applications == null) {
            this.initDefaultApplication();
        }
        try {
            final ClientConfig clone = (ClientConfig)super.clone();
            clone.handlers = new LinkedList<ClientHandler>(this.handlers);
            clone.applications = new LinkedList<Application>(this.applications);
            final Properties props = new Properties();
            props.putAll(this.getProperties());
            clone.setProperties(props);
            return clone;
        }
        catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
    
    public void setLoadWinkApplications(final boolean loadWinkApplications) {
        this.loadWinkApplications = loadWinkApplications;
    }
    
    public boolean isLoadWinkApplications() {
        return this.loadWinkApplications;
    }
    
    public Properties getProperties() {
        if (this.properties == null) {
            this.properties = new Properties();
            try {
                final String connectTimeoutString = System.getProperty("wink.client.connectTimeout", String.valueOf(ClientConfig.WINK_CLIENT_CONNECTTIMEOUT_DEFAULT));
                final int toSet = Integer.parseInt(connectTimeoutString);
                ((Hashtable<String, String>)this.properties).put("wink.client.connectTimeout", String.valueOf(toSet));
                ClientConfig.logger.trace("Wink client connectTimeout default value is {}.", (Object)toSet);
            }
            catch (Exception e) {
                ClientConfig.logger.trace("Error processing {} system property: {}", (Object)"wink.client.connectTimeout", (Object)e);
            }
            try {
                final String readTimeoutString = System.getProperty("wink.client.readTimeout", String.valueOf(ClientConfig.WINK_CLIENT_READTIMEOUT_DEFAULT));
                final int toSet = Integer.parseInt(readTimeoutString);
                ((Hashtable<String, String>)this.properties).put("wink.client.readTimeout", String.valueOf(toSet));
                ClientConfig.logger.trace("Wink client readTimeout default value is {}.", (Object)toSet);
            }
            catch (Exception e) {
                ClientConfig.logger.trace("Error processing {} system property: {}", (Object)"wink.client.readTimeout", (Object)e);
            }
            try {
                final String supportDTD = System.getProperty("wink.supportDTDEntityExpansion", String.valueOf(ClientConfig.WINK_CLIENT_SUPPORT_DTD_EXPANSION_DEFAULT));
                final boolean toSet2 = Boolean.valueOf(supportDTD);
                ((Hashtable<String, String>)this.properties).put("wink.supportDTDEntityExpansion", String.valueOf(toSet2));
                if (ClientConfig.logger.isTraceEnabled()) {
                    ClientConfig.logger.trace("Wink client readTimeout default value is {}.", (Object)String.valueOf(toSet2));
                }
            }
            catch (Exception e) {
                ClientConfig.logger.trace("Error processing {} system property: {}", (Object)"wink.supportDTDEntityExpansion", (Object)e);
            }
        }
        return this.properties;
    }
    
    public void setProperties(final Properties properties) {
        if (!this.modifiable) {
            throw new ClientConfigException(Messages.getMessage("clientConfigurationUnmodifiable"));
        }
        if (properties == null) {
            this.properties.clear();
            return;
        }
        this.properties = properties;
    }
    
    public boolean getBypassHostnameVerification() {
        return this.bypassHostnameVerification;
    }
    
    public void setBypassHostnameVerification(final boolean bypassHostnameVerification) {
        this.bypassHostnameVerification = bypassHostnameVerification;
    }
    
    static {
        logger = LoggerFactory.getLogger((Class)ClientConfig.class);
        ClientConfig.WINK_CLIENT_CONNECTTIMEOUT_DEFAULT = 60000;
        ClientConfig.WINK_CLIENT_READTIMEOUT_DEFAULT = 60000;
        ClientConfig.WINK_CLIENT_SUPPORT_DTD_EXPANSION_DEFAULT = false;
    }
}
