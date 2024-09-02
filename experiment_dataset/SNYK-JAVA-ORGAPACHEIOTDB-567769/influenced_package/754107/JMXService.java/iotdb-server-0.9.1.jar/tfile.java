// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.iotdb.db.service;

import org.slf4j.LoggerFactory;
import org.apache.iotdb.db.exception.StartupException;
import java.io.IOException;
import java.util.Map;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import java.net.InetAddress;
import java.util.HashMap;
import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.MBeanRegistrationException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import javax.management.remote.JMXConnectorServer;
import org.slf4j.Logger;

public class JMXService implements IService
{
    private static final Logger logger;
    private JMXConnectorServer jmxConnectorServer;
    
    private JMXService() {
    }
    
    public static final JMXService getInstance() {
        return JMXServerHolder.INSTANCE;
    }
    
    public static void registerMBean(final Object mbean, final String name) {
        try {
            final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            final ObjectName objectName = new ObjectName(name);
            if (!mbs.isRegistered(objectName)) {
                mbs.registerMBean(mbean, objectName);
            }
        }
        catch (MalformedObjectNameException | InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException ex2) {
            final JMException ex;
            final JMException e = ex;
            JMXService.logger.error("Failed to registerMBean {}", (Object)name, (Object)e);
        }
    }
    
    public static void deregisterMBean(final String name) {
        try {
            final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            final ObjectName objectName = new ObjectName(name);
            if (mbs.isRegistered(objectName)) {
                mbs.unregisterMBean(objectName);
            }
        }
        catch (MalformedObjectNameException | MBeanRegistrationException | InstanceNotFoundException ex2) {
            final JMException ex;
            final JMException e = ex;
            JMXService.logger.error("Failed to unregisterMBean {}", (Object)name, (Object)e);
        }
    }
    
    private JMXConnectorServer createJMXServer(final boolean local) throws IOException {
        final Map<String, Object> env = new HashMap<String, Object>();
        if (local) {
            final InetAddress serverAddress = InetAddress.getLoopbackAddress();
            System.setProperty("java.rmi.server.hostname", serverAddress.getHostAddress());
        }
        final int rmiPort = Integer.getInteger("com.sun.management.jmxremote.rmi.port", 0);
        return JMXConnectorServerFactory.newJMXConnectorServer(new JMXServiceURL("rmi", null, rmiPort), env, ManagementFactory.getPlatformMBeanServer());
    }
    
    @Override
    public ServiceType getID() {
        return ServiceType.JMX_SERVICE;
    }
    
    @Override
    public void start() throws StartupException {
        if (System.getProperty("com.sun.management.jmxremote.port") != null) {
            JMXService.logger.warn("JMX settings in conf/{}.sh(Unix or OS X, if you use Windows, check conf/{}.bat) have been bypassed as the JMX connector server is already initialized. Please refer to {}.sh/bat for JMX configuration info", new Object[] { "iotdb-env", "iotdb-env", "iotdb-env" });
            return;
        }
        System.setProperty("java.rmi.server.randomIDs", "true");
        boolean localOnly = false;
        String jmxPort = System.getProperty("iotdb.jmx.remote.port");
        if (jmxPort == null) {
            localOnly = true;
            jmxPort = System.getProperty("iotdb.jmx.local.port");
        }
        if (jmxPort == null) {
            JMXService.logger.warn("Failed to start {} because JMX port is undefined", (Object)this.getID().getName());
            return;
        }
        try {
            this.jmxConnectorServer = this.createJMXServer(localOnly);
            if (this.jmxConnectorServer == null) {
                return;
            }
            this.jmxConnectorServer.start();
            JMXService.logger.info("{}: start {} successfully.", (Object)"IoTDB", (Object)this.getID().getName());
        }
        catch (IOException e) {
            throw new StartupException(this.getID().getName(), e.getMessage());
        }
    }
    
    @Override
    public void stop() {
        if (this.jmxConnectorServer != null) {
            try {
                this.jmxConnectorServer.stop();
                JMXService.logger.info("{}: close {} successfully", (Object)"IoTDB", (Object)this.getID().getName());
            }
            catch (IOException e) {
                JMXService.logger.error("Failed to stop {} because of: ", (Object)this.getID().getName(), (Object)e);
            }
        }
    }
    
    static {
        logger = LoggerFactory.getLogger((Class)JMXService.class);
    }
    
    private static class JMXServerHolder
    {
        private static final JMXService INSTANCE;
        
        static {
            INSTANCE = new JMXService(null);
        }
    }
}
