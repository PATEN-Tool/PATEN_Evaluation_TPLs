// 
// Decompiled by Procyon v0.5.36
// 

package io.hawt;

import org.slf4j.LoggerFactory;
import javax.servlet.ServletContextEvent;
import java.util.Objects;
import io.hawt.jmx.RBACRegistry;
import io.hawt.jmx.JMXSecurity;
import io.hawt.system.ConfigManager;
import io.hawt.jmx.UploadManager;
import io.hawt.jmx.PluginRegistry;
import io.hawt.jmx.JmxTreeWatcher;
import io.hawt.jmx.QuartzFacade;
import io.hawt.jmx.About;
import org.slf4j.Logger;
import javax.servlet.ServletContextListener;

public class HawtioContextListener implements ServletContextListener
{
    private static final Logger LOGGER;
    private final About about;
    private final QuartzFacade quartz;
    private final JmxTreeWatcher treeWatcher;
    private final PluginRegistry registry;
    private final UploadManager uploadManager;
    private final ConfigManager configManager;
    private final JMXSecurity jmxSecurity;
    private final RBACRegistry rbacRegistry;
    
    public HawtioContextListener() {
        this(new ConfigManager());
    }
    
    public HawtioContextListener(final ConfigManager configManager) {
        this.configManager = Objects.requireNonNull(configManager);
        this.about = new About();
        this.quartz = new QuartzFacade();
        this.treeWatcher = new JmxTreeWatcher();
        this.registry = new PluginRegistry();
        this.uploadManager = new UploadManager();
        this.jmxSecurity = new JMXSecurity();
        this.rbacRegistry = new RBACRegistry();
    }
    
    public void contextInitialized(final ServletContextEvent servletContextEvent) {
        HawtioContextListener.LOGGER.info("Initialising hawtio services");
        try {
            this.about.init();
            this.quartz.init();
            this.configManager.init(servletContextEvent.getServletContext());
            this.treeWatcher.init();
            this.registry.init();
            this.uploadManager.init(this.configManager);
            this.jmxSecurity.init();
            this.rbacRegistry.init();
        }
        catch (Exception e) {
            throw this.createServletException(e);
        }
        servletContextEvent.getServletContext().setAttribute("ConfigManager", (Object)this.configManager);
    }
    
    public void contextDestroyed(final ServletContextEvent servletContextEvent) {
        HawtioContextListener.LOGGER.info("Destroying hawtio services");
        try {
            this.rbacRegistry.destroy();
            this.about.destroy();
            this.quartz.destroy();
            this.treeWatcher.destroy();
            this.registry.destroy();
            this.uploadManager.destroy();
            this.configManager.destroy();
            this.jmxSecurity.destroy();
        }
        catch (Exception e) {
            throw this.createServletException(e);
        }
    }
    
    protected RuntimeException createServletException(final Exception e) {
        return new RuntimeException(e);
    }
    
    static {
        LOGGER = LoggerFactory.getLogger((Class)HawtioContextListener.class);
    }
}
