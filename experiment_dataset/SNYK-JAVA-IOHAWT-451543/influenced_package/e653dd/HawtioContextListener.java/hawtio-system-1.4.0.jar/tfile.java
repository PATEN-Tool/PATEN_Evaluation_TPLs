// 
// Decompiled by Procyon v0.5.36
// 

package io.hawt;

import javax.servlet.ServletContextEvent;
import io.hawt.system.ConfigManager;
import io.hawt.jmx.UploadManager;
import io.hawt.jmx.PluginRegistry;
import io.hawt.jmx.JmxTreeWatcher;
import io.hawt.jmx.QuartzFacade;
import io.hawt.jmx.About;
import javax.servlet.ServletContextListener;

public class HawtioContextListener implements ServletContextListener
{
    private About about;
    private QuartzFacade quartz;
    private JmxTreeWatcher treeWatcher;
    private PluginRegistry registry;
    private UploadManager uploadManager;
    private ConfigManager configManager;
    
    public HawtioContextListener() {
        this.about = new About();
        this.quartz = new QuartzFacade();
        this.treeWatcher = new JmxTreeWatcher();
        this.registry = new PluginRegistry();
        this.uploadManager = new UploadManager();
        this.configManager = new ConfigManager();
    }
    
    public void contextInitialized(final ServletContextEvent servletContextEvent) {
        try {
            this.about.init();
            this.quartz.init();
            this.configManager.init();
            this.treeWatcher.init();
            this.registry.init();
            this.uploadManager.init(this.configManager);
        }
        catch (Exception e) {
            throw this.createServletException(e);
        }
        servletContextEvent.getServletContext().setAttribute("ConfigManager", (Object)this.configManager);
    }
    
    public void contextDestroyed(final ServletContextEvent servletContextEvent) {
        try {
            this.about.destroy();
            this.quartz.destroy();
            this.treeWatcher.destroy();
            this.registry.destroy();
            this.uploadManager.destroy();
            this.configManager.destroy();
        }
        catch (Exception e) {
            throw this.createServletException(e);
        }
    }
    
    protected RuntimeException createServletException(final Exception e) {
        return new RuntimeException(e);
    }
}
