// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.openejb.resource.activemq;

import java.util.Hashtable;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Collection;
import org.apache.activemq.broker.BrokerService;
import org.apache.openejb.resource.activemq.jms2.TomEEConnectionFactory;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ra.MessageActivationSpec;
import org.apache.activemq.ra.ActiveMQConnectionRequestInfo;
import org.apache.openejb.util.Logger;
import org.apache.openejb.util.LogCategory;
import java.net.URISyntaxException;
import javax.resource.spi.ResourceAdapterInternalException;
import org.apache.openejb.util.URISupport;
import org.apache.openejb.util.URLs;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.apache.openejb.util.Duration;
import javax.resource.spi.BootstrapContext;

public class ActiveMQResourceAdapter extends org.apache.activemq.ra.ActiveMQResourceAdapter
{
    private String dataSource;
    private String useDatabaseLock;
    private String startupTimeout;
    private BootstrapContext bootstrapContext;
    
    public ActiveMQResourceAdapter() {
        this.startupTimeout = "60000";
    }
    
    public String getDataSource() {
        return this.dataSource;
    }
    
    public void setDataSource(final String dataSource) {
        this.dataSource = dataSource;
    }
    
    public void setUseDatabaseLock(final String useDatabaseLock) {
        this.useDatabaseLock = useDatabaseLock;
    }
    
    public int getStartupTimeout() {
        return Integer.parseInt(this.startupTimeout);
    }
    
    public void setStartupTimeout(final Duration startupTimeout) {
        if (startupTimeout.getUnit() == null) {
            startupTimeout.setUnit(TimeUnit.MILLISECONDS);
        }
        this.startupTimeout = String.valueOf(TimeUnit.MILLISECONDS.convert(startupTimeout.getTime(), startupTimeout.getUnit()));
    }
    
    public void setServerUrl(final String url) {
        super.setServerUrl(url);
    }
    
    public void start(final BootstrapContext bootstrapContext) throws ResourceAdapterInternalException {
        this.bootstrapContext = bootstrapContext;
        final String brokerXmlConfig = this.getBrokerXmlConfig();
        super.setBrokerXmlConfig((String)null);
        super.start(bootstrapContext);
        final Properties properties = new Properties();
        if (null != this.dataSource) {
            ((Hashtable<String, String>)properties).put("DataSource", this.dataSource);
        }
        if (null != this.useDatabaseLock) {
            ((Hashtable<String, String>)properties).put("UseDatabaseLock", this.useDatabaseLock);
        }
        if (null != this.startupTimeout) {
            ((Hashtable<String, String>)properties).put("StartupTimeout", this.startupTimeout);
        }
        if (brokerXmlConfig != null && !brokerXmlConfig.trim().isEmpty()) {
            try {
                if (brokerXmlConfig.startsWith("broker:")) {
                    final URISupport.CompositeData compositeData = URISupport.parseComposite(URLs.uri(brokerXmlConfig));
                    if (!compositeData.getParameters().containsKey("persistent")) {
                        compositeData.getParameters().put("persistent", "false");
                    }
                    if ("false".equalsIgnoreCase(compositeData.getParameters().get("persistent").toString())) {
                        properties.remove("DataSource");
                    }
                    this.setBrokerXmlConfig(ActiveMQFactory.getBrokerMetaFile() + compositeData.toURI());
                }
                else if (brokerXmlConfig.toLowerCase().startsWith("xbean:")) {
                    this.setBrokerXmlConfig(ActiveMQFactory.getBrokerMetaFile() + brokerXmlConfig);
                }
            }
            catch (URISyntaxException e) {
                throw new ResourceAdapterInternalException("Invalid BrokerXmlConfig", (Throwable)e);
            }
            this.createInternalBroker(brokerXmlConfig, properties);
        }
    }
    
    private void createInternalBroker(final String brokerXmlConfig, final Properties properties) {
        ActiveMQFactory.setThreadProperties(properties);
        try {
            ActiveMQFactory.createBroker(URLs.uri(this.getBrokerXmlConfig())).start();
        }
        catch (Exception e) {
            Logger.getInstance(LogCategory.OPENEJB_STARTUP, ActiveMQResourceAdapter.class).getChildLogger("service").fatal("Failed to start ActiveMQ", e);
        }
        finally {
            ActiveMQFactory.setThreadProperties(null);
            if (brokerXmlConfig != null) {
                this.setBrokerXmlConfig(brokerXmlConfig);
            }
        }
    }
    
    public BootstrapContext getBootstrapContext() {
        return this.bootstrapContext;
    }
    
    public void stop() {
        Logger.getInstance(LogCategory.OPENEJB_STARTUP, ActiveMQResourceAdapter.class).getChildLogger("service").info("Stopping ActiveMQ");
        final Thread stopThread = new Thread("ActiveMQResourceAdapter stop") {
            @Override
            public void run() {
                try {
                    ActiveMQResourceAdapter.this.stopImpl();
                }
                catch (Throwable t) {
                    Logger.getInstance(LogCategory.OPENEJB_STARTUP, ActiveMQResourceAdapter.class).getChildLogger("service").error("ActiveMQ shutdown failed", t);
                }
            }
        };
        stopThread.setDaemon(true);
        stopThread.start();
        int timeout = 60000;
        try {
            timeout = Integer.parseInt(this.startupTimeout);
        }
        catch (Throwable t) {}
        try {
            stopThread.join(timeout);
        }
        catch (InterruptedException ex) {
            Logger.getInstance(LogCategory.OPENEJB_STARTUP, ActiveMQResourceAdapter.class).getChildLogger("service").warning("Gave up on ActiveMQ shutdown after " + timeout + "ms", ex);
        }
    }
    
    protected ActiveMQConnectionFactory createConnectionFactory(final ActiveMQConnectionRequestInfo connectionRequestInfo, final MessageActivationSpec activationSpec) {
        final ActiveMQConnectionFactory factory = (ActiveMQConnectionFactory)new TomEEConnectionFactory();
        connectionRequestInfo.configure(factory, activationSpec);
        return factory;
    }
    
    private void stopImpl() throws Exception {
        super.stop();
        final Collection<BrokerService> brokers = ActiveMQFactory.getBrokers();
        final Iterator<BrokerService> it = brokers.iterator();
        while (it.hasNext()) {
            final BrokerService bs = it.next();
            try {
                bs.stop();
                bs.waitUntilStopped();
            }
            catch (Throwable t) {}
            it.remove();
        }
        stopScheduler();
        Logger.getInstance(LogCategory.OPENEJB_STARTUP, ActiveMQResourceAdapter.class).getChildLogger("service").info("Stopped ActiveMQ broker");
    }
    
    private static void stopScheduler() {
        try {
            final Class<?> clazz = Class.forName("org.apache.kahadb.util.Scheduler");
            final Method method = clazz.getMethod("shutdown", (Class<?>[])new Class[0]);
            method.invoke(null, new Object[0]);
        }
        catch (Throwable t) {}
    }
}
