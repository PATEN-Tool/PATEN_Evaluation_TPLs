// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.openejb.resource.activemq;

import java.util.Hashtable;
import java.util.HashMap;
import java.lang.reflect.Method;
import java.util.Collection;
import org.apache.activemq.broker.BrokerService;
import org.apache.openejb.resource.activemq.jms2.TomEEConnectionFactory;
import javax.resource.spi.TransactionSupport;
import org.apache.activemq.ra.ActiveMQConnectionRequestInfo;
import org.apache.activemq.RedeliveryPolicy;
import org.apache.activemq.ra.ActiveMQManagedConnection;
import org.apache.openejb.resource.activemq.jms2.TomEEManagedConnectionProxy;
import org.apache.openejb.resource.AutoConnectionTracker;
import javax.jms.JMSException;
import java.lang.reflect.Field;
import javax.jms.Connection;
import javax.naming.NamingException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.openejb.loader.SystemInstance;
import org.apache.openejb.spi.ContainerSystem;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ra.MessageActivationSpec;
import javax.resource.ResourceException;
import java.util.Iterator;
import org.apache.openejb.util.reflection.Reflections;
import org.apache.openejb.core.mdb.MdbContainer;
import org.apache.activemq.ra.ActiveMQEndpointActivationKey;
import org.apache.activemq.ra.ActiveMQEndpointWorker;
import org.apache.openejb.util.LogCategory;
import javax.resource.spi.ResourceAdapterInternalException;
import java.util.Locale;
import java.util.Properties;
import java.net.URISyntaxException;
import org.apache.openejb.util.URISupport;
import org.apache.openejb.util.URLs;
import java.util.concurrent.TimeUnit;
import org.apache.openejb.util.Duration;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.openejb.util.Logger;
import javax.management.ObjectName;
import org.apache.openejb.BeanContext;
import java.util.Map;
import javax.resource.spi.BootstrapContext;

public class ActiveMQResourceAdapter extends org.apache.activemq.ra.ActiveMQResourceAdapter
{
    private String dataSource;
    private String useDatabaseLock;
    private String startupTimeout;
    private BootstrapContext bootstrapContext;
    private final Map<BeanContext, ObjectName> mbeanNames;
    private static final Map<String, String> PREVENT_CREATION_PARAMS;
    private static final Logger LOGGER;
    
    public ActiveMQResourceAdapter() {
        this.startupTimeout = "60000";
        this.mbeanNames = new ConcurrentHashMap<BeanContext, ObjectName>();
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
        try {
            final URISupport.CompositeData compositeData = URISupport.parseComposite(URLs.uri(url));
            if ("vm".equals(compositeData.getScheme())) {
                super.setServerUrl(URISupport.addParameters(URLs.uri(url), ActiveMQResourceAdapter.PREVENT_CREATION_PARAMS).toString());
                return;
            }
        }
        catch (URISyntaxException e) {
            ActiveMQResourceAdapter.LOGGER.error("Error occurred while processing ActiveMQ ServerUrl: " + url, e);
        }
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
                else if (brokerXmlConfig.toLowerCase(Locale.ENGLISH).startsWith("xbean:")) {
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
    
    private ActiveMQEndpointWorker getWorker(final BeanContext beanContext) throws ResourceException {
        final Map<ActiveMQEndpointActivationKey, ActiveMQEndpointWorker> workers = Map.class.cast(Reflections.get(MdbContainer.class.cast(beanContext.getContainer()).getResourceAdapter(), "endpointWorkers"));
        for (final Map.Entry<ActiveMQEndpointActivationKey, ActiveMQEndpointWorker> entry : workers.entrySet()) {
            if (entry.getKey().getMessageEndpointFactory() == beanContext.getContainerData()) {
                return entry.getValue();
            }
        }
        throw new IllegalStateException("No worker for " + beanContext.getDeploymentID());
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
    
    public ActiveMQConnection makeConnection(final MessageActivationSpec activationSpec) throws JMSException {
        if (TomEEMessageActivationSpec.class.isInstance(activationSpec)) {
            final TomEEMessageActivationSpec s = TomEEMessageActivationSpec.class.cast(activationSpec);
            if (s.getConnectionFactoryLookup() != null) {
                try {
                    final Object lookup = ((ContainerSystem)SystemInstance.get().getComponent((Class)ContainerSystem.class)).getJNDIContext().lookup("openejb:Resource/" + s.getConnectionFactoryLookup());
                    if (!ActiveMQConnectionFactory.class.isInstance(lookup)) {
                        final org.apache.activemq.ra.ActiveMQConnectionFactory connectionFactory = org.apache.activemq.ra.ActiveMQConnectionFactory.class.cast(lookup);
                        final Connection connection = connectionFactory.createConnection();
                        if (Proxy.isProxyClass(connection.getClass())) {
                            final InvocationHandler invocationHandler = Proxy.getInvocationHandler(connection);
                            final ActiveMQConnection physicalConnection = this.getActiveMQConnection(activationSpec, invocationHandler);
                            if (physicalConnection != null) {
                                return physicalConnection;
                            }
                        }
                        try {
                            final Field handler = connection.getClass().getDeclaredField("this$handler");
                            handler.setAccessible(true);
                            final Object o = handler.get(connection);
                            if (InvocationHandler.class.isInstance(o)) {
                                final InvocationHandler invocationHandler2 = InvocationHandler.class.cast(o);
                                final ActiveMQConnection physicalConnection2 = this.getActiveMQConnection(activationSpec, invocationHandler2);
                                if (physicalConnection2 != null) {
                                    return physicalConnection2;
                                }
                            }
                        }
                        catch (NoSuchFieldException ex) {}
                        catch (IllegalAccessException ex2) {}
                        return null;
                    }
                }
                catch (ClassCastException cce) {
                    throw new IllegalStateException(cce);
                }
                catch (NamingException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        }
        return super.makeConnection(activationSpec);
    }
    
    private ActiveMQConnection getActiveMQConnection(final MessageActivationSpec activationSpec, final InvocationHandler invocationHandler) {
        if (AutoConnectionTracker.ConnectionInvocationHandler.class.isInstance(invocationHandler)) {
            final Object handle = Reflections.get(invocationHandler, "handle");
            if (TomEEManagedConnectionProxy.class.isInstance(handle)) {
                final ActiveMQManagedConnection c = ActiveMQManagedConnection.class.cast(Reflections.get(handle, "connection"));
                final ActiveMQConnection physicalConnection = ActiveMQConnection.class.cast(Reflections.get(c, "physicalConnection"));
                final RedeliveryPolicy redeliveryPolicy = activationSpec.redeliveryPolicy();
                if (redeliveryPolicy != null) {
                    physicalConnection.setRedeliveryPolicy(redeliveryPolicy);
                }
                return physicalConnection;
            }
        }
        return null;
    }
    
    protected ActiveMQConnectionFactory createConnectionFactory(final ActiveMQConnectionRequestInfo connectionRequestInfo, final MessageActivationSpec activationSpec) {
        if (TomEEMessageActivationSpec.class.isInstance(activationSpec)) {
            final TomEEMessageActivationSpec s = TomEEMessageActivationSpec.class.cast(activationSpec);
            if (s.getConnectionFactoryLookup() != null) {
                try {
                    final Object lookup = ((ContainerSystem)SystemInstance.get().getComponent((Class)ContainerSystem.class)).getJNDIContext().lookup("openejb:Resource/" + s.getConnectionFactoryLookup());
                    if (ActiveMQConnectionFactory.class.isInstance(lookup)) {
                        return ActiveMQConnectionFactory.class.cast(lookup);
                    }
                    return ActiveMQConnectionFactory.class.cast(lookup);
                }
                catch (NamingException e) {
                    throw new IllegalArgumentException("");
                }
            }
        }
        final ActiveMQConnectionFactory factory = (ActiveMQConnectionFactory)new TomEEConnectionFactory(TransactionSupport.TransactionSupportLevel.XATransaction);
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
    
    static {
        PREVENT_CREATION_PARAMS = new HashMap<String, String>() {
            {
                this.put("create", "false");
            }
        };
        LOGGER = Logger.getInstance(LogCategory.ACTIVEMQ, ActiveMQ5Factory.class);
    }
}
