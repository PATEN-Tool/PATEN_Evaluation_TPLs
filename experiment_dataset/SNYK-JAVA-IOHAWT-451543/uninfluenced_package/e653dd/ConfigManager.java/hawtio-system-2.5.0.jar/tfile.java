// 
// Decompiled by Procyon v0.5.36
// 

package io.hawt.system;

import org.slf4j.LoggerFactory;
import java.util.Arrays;
import javax.naming.NamingException;
import javax.naming.InitialContext;
import javax.servlet.ServletContext;
import java.util.Objects;
import java.util.function.Function;
import javax.naming.Context;
import org.slf4j.Logger;

public class ConfigManager
{
    private static final transient Logger LOG;
    public static final String CONFIG_MANAGER = "ConfigManager";
    private Context envContext;
    private Function<String, String> propertyResolver;
    
    public ConfigManager() {
        this.envContext = null;
        this.propertyResolver = ConfigManager::getHawtioSystemProperty;
    }
    
    public ConfigManager(final Function<String, String> propertyResolver) {
        this.envContext = null;
        Objects.requireNonNull(propertyResolver);
        this.propertyResolver = (Function<String, String>)(x -> getProperty(x, ConfigManager::getHawtioSystemProperty, propertyResolver));
    }
    
    public void init(final ServletContext servletContext) {
        if (Boolean.parseBoolean(getHawtioSystemProperty("forceProperties"))) {
            ConfigManager.LOG.info("Forced using system properties");
            return;
        }
        try {
            this.envContext = (Context)new InitialContext().lookup("java:comp/env");
            ConfigManager.LOG.info("Configuration will be discovered via JNDI");
        }
        catch (NamingException e) {
            ConfigManager.LOG.debug("Failed to look up environment context: {}", (Object)e.getMessage());
            ConfigManager.LOG.info("Configuration will be discovered via system properties");
        }
    }
    
    public void destroy() {
        if (this.envContext != null) {
            try {
                this.envContext.close();
            }
            catch (NamingException ex) {}
            this.envContext = null;
        }
    }
    
    public String get(final String name, final String defaultValue) {
        String answer = null;
        if (this.envContext != null) {
            try {
                answer = (String)this.envContext.lookup("hawtio/" + name);
            }
            catch (Exception ex) {}
        }
        if (answer == null) {
            answer = this.propertyResolver.apply(name);
        }
        if (answer == null) {
            answer = defaultValue;
        }
        ConfigManager.LOG.debug("Property {} is set to value {}", (Object)name, (Object)answer);
        return answer;
    }
    
    public boolean getBoolean(final String name, final boolean defaultValue) {
        return Boolean.parseBoolean(this.get(name, Boolean.toString(defaultValue)));
    }
    
    private static String getHawtioSystemProperty(final String name) {
        return System.getProperty("hawtio." + name);
    }
    
    @SafeVarargs
    private static String getProperty(final String name, final Function<String, String>... propertyResolvers) {
        return Arrays.stream(propertyResolvers).map(resolver -> resolver.apply(name)).filter(result -> result != null).findFirst().orElse(null);
    }
    
    static {
        LOG = LoggerFactory.getLogger((Class)ConfigManager.class);
    }
}
