// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.openejb.loader;

import java.io.File;
import org.apache.openejb.loader.provisining.ProvisioningResolver;
import org.apache.openejb.loader.event.ComponentAdded;
import org.apache.openejb.loader.event.ComponentRemoved;
import java.util.Iterator;
import java.io.IOException;
import java.util.Hashtable;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.openejb.observer.ObserverManager;
import java.util.Map;
import java.util.Properties;

public final class SystemInstance
{
    private static final String PROFILE_PROP = "openejb.profile";
    private static final String DEFAULT_PROFILE = "development";
    private final long startTime;
    private final Properties internalProperties;
    private final Options options;
    private final FileUtils home;
    private final FileUtils base;
    private final ClassLoader classLoader;
    private final Map<Class, Object> components;
    private final ClassPath classPath;
    private final ObserverManager observerManager;
    private static final AtomicReference<SystemInstance> system;
    private static boolean initialized;
    
    private SystemInstance(final Properties properties) {
        this.startTime = System.currentTimeMillis();
        this.internalProperties = new Properties(System.getProperties());
        this.observerManager = new ObserverManager();
        this.components = new HashMap<Class, Object>();
        for (final String key : System.getProperties().stringPropertyNames()) {
            if (key.startsWith("sun.")) {
                continue;
            }
            if (key.startsWith("os.")) {
                continue;
            }
            if (key.startsWith("user.")) {
                continue;
            }
            if (key.startsWith("awt.")) {
                continue;
            }
            if (key.startsWith("java.")) {
                final String pkg = key.substring("java.".length());
                if (pkg.startsWith("vm.")) {
                    continue;
                }
                if (pkg.startsWith("runtime.")) {
                    continue;
                }
                if (pkg.startsWith("awt.")) {
                    continue;
                }
                if (pkg.startsWith("specification.")) {
                    continue;
                }
                if (pkg.startsWith("class.")) {
                    continue;
                }
                if (pkg.startsWith("library.")) {
                    continue;
                }
                if (pkg.startsWith("ext.")) {
                    continue;
                }
                if (pkg.startsWith("vendor.")) {
                    continue;
                }
                if (pkg.startsWith("endorsed.")) {
                    continue;
                }
            }
            final String value = System.getProperty(key);
            if (value == null) {
                continue;
            }
            ((Hashtable<String, String>)this.internalProperties).put(key, value);
        }
        this.internalProperties.putAll(properties);
        this.options = new Options(this.internalProperties, new Options(System.getProperties()));
        this.home = new FileUtils("openejb.home", "user.dir", this.internalProperties);
        this.base = new FileUtils("openejb.base", "openejb.home", this.internalProperties);
        this.classPath = ClassPathFactory.createClassPath(this.internalProperties.getProperty("openejb.loader", "context"));
        this.classLoader = this.classPath.getClassLoader();
        String homeDirCanonicalPath;
        String baseDirCanonicalPath;
        try {
            homeDirCanonicalPath = this.home.getDirectory().getCanonicalPath();
            baseDirCanonicalPath = this.base.getDirectory().getCanonicalPath();
        }
        catch (IOException e) {
            throw new LoaderRuntimeException("Failed to create default instance of SystemInstance", e);
        }
        this.internalProperties.setProperty("openejb.home", homeDirCanonicalPath);
        this.internalProperties.setProperty("openejb.base", baseDirCanonicalPath);
        System.setProperty("derby.system.home", System.getProperty("derby.system.home", baseDirCanonicalPath));
    }
    
    public <E> E fireEvent(final E event) {
        return this.observerManager.fireEvent(event);
    }
    
    public boolean addObserver(final Object observer) {
        return this.observerManager.addObserver(observer);
    }
    
    public boolean removeObserver(final Object observer) {
        return this.observerManager.removeObserver(observer);
    }
    
    public long getStartTime() {
        return this.startTime;
    }
    
    public Options getOptions() {
        return this.options;
    }
    
    public Properties getProperties() {
        return this.internalProperties;
    }
    
    public String getProperty(final String key) {
        return this.internalProperties.getProperty(key);
    }
    
    public String getProperty(final String key, final String defaultValue) {
        return this.internalProperties.getProperty(key, defaultValue);
    }
    
    public Object setProperty(final String key, final String value) {
        return this.setProperty(key, value, false);
    }
    
    public Object setProperty(final String key, final String value, final boolean isExternalProperty) {
        if (isExternalProperty) {
            System.setProperty(key, value);
        }
        return this.internalProperties.setProperty(key, value);
    }
    
    public FileUtils getHome() {
        if (!isInitialized()) {
            return new FileUtils("openejb.home", "user.dir", System.getProperties());
        }
        return this.home;
    }
    
    public FileUtils getBase() {
        if (!isInitialized()) {
            return new FileUtils("openejb.base", "openejb.home", System.getProperties());
        }
        return this.base;
    }
    
    public ClassPath getClassPath() {
        return this.classPath;
    }
    
    public ClassLoader getClassLoader() {
        return this.classLoader;
    }
    
    public <T> T getComponent(final Class<T> type) {
        final T component = (T)this.components.get(type);
        if (component != null) {
            return component;
        }
        final String classname = this.getProperty(type.getName());
        if (classname != null) {
            try {
                final T instance = type.cast(Thread.currentThread().getContextClassLoader().loadClass(classname).newInstance());
                this.components.put(type, instance);
                return instance;
            }
            catch (Throwable e) {
                System.err.println("Failed to load class: " + classname);
            }
        }
        return null;
    }
    
    public <T> T removeComponent(final Class<T> type) {
        final T component = (T)this.components.remove(type);
        if (component != null) {
            this.fireEvent(new ComponentRemoved<T>(type, component));
        }
        return component;
    }
    
    public <T> T setComponent(final Class<T> type, final T value) {
        final T removed = (T)this.components.put(type, value);
        if (removed != null) {
            this.fireEvent(new ComponentRemoved<T>(type, value));
        }
        if (value != null) {
            this.fireEvent(new ComponentAdded<T>(type, value));
        }
        return removed;
    }
    
    public static boolean isInitialized() {
        return SystemInstance.initialized;
    }
    
    public static synchronized void reset() {
        try {
            System.clearProperty("openejb.loader");
            SystemInstance.system.set(new SystemInstance(new Properties()));
            SystemInstance.initialized = false;
        }
        catch (Exception e) {
            throw new LoaderRuntimeException("Failed to create default instance of SystemInstance", e);
        }
    }
    
    public static synchronized void init(final Properties properties) throws Exception {
        if (SystemInstance.initialized) {
            return;
        }
        SystemInstance.system.set(new SystemInstance(properties));
        readSystemProperties(get().currentProfile());
        readSystemProperties();
        readUserSystemProperties();
        System.getProperties().putAll(SystemInstance.system.get().getProperties());
        SystemInstance.initialized = true;
        get().setProperty("openejb.profile.custom", Boolean.toString(!get().isDefaultProfile()));
        initDefaultComponents();
    }
    
    private static void initDefaultComponents() {
        SystemInstance.system.get().components.put(ProvisioningResolver.class, new ProvisioningResolver());
    }
    
    private static void readUserSystemProperties() {
        final File file = new File(System.getProperty("user.home"), ".openejb/system.properties");
        addSystemProperties(file);
    }
    
    public File getConf(final String subPath) {
        File conf = null;
        final FileUtils base = SystemInstance.system.get().getBase();
        try {
            conf = base.getDirectory("conf");
        }
        catch (IOException ex) {}
        Label_0050: {
            if (conf != null) {
                if (conf.exists()) {
                    break Label_0050;
                }
            }
            try {
                conf = base.getDirectory("etc");
            }
            catch (IOException ex2) {}
        }
        if (conf == null || !conf.exists()) {
            return new File(base.getDirectory(), "conf");
        }
        if (subPath == null) {
            return conf;
        }
        return new File(conf, subPath);
    }
    
    private static void readSystemProperties(final String prefix) {
        String completePrefix;
        if (prefix != null && !prefix.isEmpty()) {
            completePrefix = prefix + ".";
        }
        else {
            completePrefix = "";
        }
        final File conf = SystemInstance.system.get().getConf(completePrefix + "system.properties");
        if (conf != null && conf.exists()) {
            addSystemProperties(conf);
        }
    }
    
    private static void readSystemProperties() {
        readSystemProperties(null);
    }
    
    private static void addSystemProperties(final File file) {
        if (!file.exists()) {
            return;
        }
        Properties systemProperties;
        try {
            systemProperties = IO.readProperties(file);
        }
        catch (IOException e) {
            return;
        }
        for (final String key : systemProperties.stringPropertyNames()) {
            final SystemInstance systemInstance = SystemInstance.system.get();
            if (systemInstance.getProperty(key) == null) {
                systemInstance.setProperty(key, systemProperties.getProperty(key));
            }
        }
    }
    
    public static SystemInstance get() {
        return SystemInstance.system.get();
    }
    
    public String currentProfile() {
        return this.getProperty("openejb.profile", "development");
    }
    
    public boolean isDefaultProfile() {
        return "development".equals(this.currentProfile());
    }
    
    public boolean hasProperty(final String propName) {
        return this.internalProperties.get(propName) != null;
    }
    
    public void removeObservers() {
        this.observerManager.destroy();
    }
    
    static {
        system = new AtomicReference<SystemInstance>();
        reset();
    }
}
