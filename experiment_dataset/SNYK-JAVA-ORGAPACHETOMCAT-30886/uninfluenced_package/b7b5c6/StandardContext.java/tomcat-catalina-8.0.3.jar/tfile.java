// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.catalina.core;

import java.util.EventListener;
import javax.servlet.SessionTrackingMode;
import javax.servlet.SessionCookieConfig;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import java.util.Enumeration;
import javax.servlet.RequestDispatcher;
import java.net.MalformedURLException;
import org.apache.juli.logging.LogFactory;
import javax.management.ListenerNotFoundException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import java.io.InputStream;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequest;
import java.util.Stack;
import java.security.PrivilegedAction;
import java.security.AccessController;
import javax.naming.NamingException;
import org.apache.naming.ContextBindings;
import java.util.Collection;
import javax.servlet.ServletSecurityElement;
import java.util.List;
import org.apache.tomcat.util.descriptor.web.InjectionTarget;
import org.apache.tomcat.util.descriptor.web.ContextService;
import org.apache.tomcat.util.descriptor.web.ContextResource;
import org.apache.tomcat.util.descriptor.web.ContextResourceEnvRef;
import org.apache.tomcat.util.descriptor.web.ContextEnvironment;
import org.apache.tomcat.util.descriptor.web.ContextEjb;
import org.apache.tomcat.util.descriptor.web.Injectable;
import org.apache.tomcat.util.descriptor.web.ContextLocalEjb;
import org.apache.tomcat.util.IntrospectionUtils;
import org.apache.catalina.Realm;
import org.apache.catalina.Cluster;
import org.apache.catalina.session.StandardManager;
import org.apache.catalina.util.ExtensionValidator;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.webresources.StandardRoot;
import javax.management.Notification;
import javax.servlet.ServletException;
import java.util.TreeMap;
import org.apache.catalina.WebResource;
import javax.servlet.ServletContextEvent;
import javax.servlet.http.HttpSessionListener;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionIdListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletContextAttributeListener;
import java.util.ArrayList;
import javax.servlet.FilterConfig;
import javax.servlet.ServletRegistration;
import org.apache.catalina.ContainerListener;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.InstanceListener;
import org.apache.tomcat.util.buf.UDecoder;
import org.apache.tomcat.util.descriptor.web.MessageDestinationRef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.catalina.Wrapper;
import org.apache.catalina.Container;
import java.io.IOException;
import java.io.File;
import javax.servlet.ServletContext;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Lifecycle;
import java.util.concurrent.locks.Lock;
import java.util.Locale;
import org.apache.tomcat.util.ExceptionUtils;
import java.util.Arrays;
import org.apache.tomcat.util.scan.StandardJarScanner;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Authenticator;
import java.util.Iterator;
import org.apache.catalina.util.ContextName;
import org.apache.catalina.Valve;
import java.util.HashSet;
import org.apache.catalina.Globals;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.LinkedHashMap;
import javax.management.MBeanNotificationInfo;
import org.apache.catalina.ThreadBindingListener;
import javax.servlet.Servlet;
import javax.servlet.descriptor.JspConfigDescriptor;
import org.apache.tomcat.JarScanner;
import org.apache.catalina.WebResourceRoot;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.tomcat.util.descriptor.web.MessageDestination;
import org.apache.catalina.deploy.NamingResourcesImpl;
import org.apache.catalina.Manager;
import org.apache.tomcat.util.descriptor.web.LoginConfig;
import java.util.concurrent.locks.ReadWriteLock;
import org.apache.catalina.Loader;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.ErrorPage;
import java.util.HashMap;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import java.net.URL;
import org.apache.catalina.util.CharsetMapper;
import javax.management.NotificationBroadcasterSupport;
import org.apache.tomcat.util.descriptor.web.ApplicationParameter;
import java.util.Set;
import javax.servlet.ServletContainerInitializer;
import java.util.Map;
import org.apache.tomcat.util.descriptor.web.ApplicationListener;
import org.apache.tomcat.InstanceManager;
import org.apache.catalina.util.URLEncoder;
import org.apache.juli.logging.Log;
import javax.management.NotificationEmitter;
import org.apache.catalina.Context;

public class StandardContext extends ContainerBase implements Context, NotificationEmitter
{
    private static final Log log;
    protected static URLEncoder urlEncoder;
    protected boolean allowCasualMultipartParsing;
    private boolean swallowAbortedUploads;
    private String altDDName;
    private InstanceManager instanceManager;
    private boolean antiResourceLocking;
    private ApplicationListener[] applicationListeners;
    private final Object applicationListenersLock;
    private Object[] applicationEventListenersObjects;
    private Object[] applicationLifecycleListenersObjects;
    private Map<ServletContainerInitializer, Set<Class<?>>> initializers;
    private ApplicationParameter[] applicationParameters;
    private final Object applicationParametersLock;
    private NotificationBroadcasterSupport broadcaster;
    private CharsetMapper charsetMapper;
    private String charsetMapperClass;
    private URL configFile;
    private boolean configured;
    private volatile SecurityConstraint[] constraints;
    private final Object constraintsLock;
    protected ApplicationContext context;
    private boolean cookies;
    private boolean crossContext;
    private String encodedPath;
    private String path;
    private boolean delegate;
    private boolean denyUncoveredHttpMethods;
    private String displayName;
    private String defaultContextXml;
    private String defaultWebXml;
    private boolean distributable;
    private String docBase;
    private HashMap<String, ErrorPage> exceptionPages;
    private HashMap<String, ApplicationFilterConfig> filterConfigs;
    private HashMap<String, FilterDef> filterDefs;
    private final ContextFilterMaps filterMaps;
    private boolean ignoreAnnotations;
    private String[] instanceListeners;
    private final Object instanceListenersLock;
    private Loader loader;
    private final ReadWriteLock loaderLock;
    private LoginConfig loginConfig;
    protected Manager manager;
    private final ReadWriteLock managerLock;
    private NamingContextListener namingContextListener;
    private NamingResourcesImpl namingResources;
    private HashMap<String, MessageDestination> messageDestinations;
    private HashMap<String, String> mimeMappings;
    private HashMap<String, String> parameters;
    private boolean paused;
    private String publicId;
    private boolean reloadable;
    private boolean unpackWAR;
    private boolean copyXML;
    private boolean override;
    private String originalDocBase;
    private boolean privileged;
    private boolean replaceWelcomeFiles;
    private HashMap<String, String> roleMappings;
    private String[] securityRoles;
    private final Object securityRolesLock;
    private HashMap<String, String> servletMappings;
    private final Object servletMappingsLock;
    private int sessionTimeout;
    private AtomicLong sequenceNumber;
    private HashMap<Integer, ErrorPage> statusPages;
    private boolean swallowOutput;
    private long unloadDelay;
    private String[] watchedResources;
    private final Object watchedResourcesLock;
    private String[] welcomeFiles;
    private final Object welcomeFilesLock;
    private String[] wrapperLifecycles;
    private final Object wrapperLifecyclesLock;
    private String[] wrapperListeners;
    private final Object wrapperListenersLock;
    private String workDir;
    private String wrapperClassName;
    private Class<?> wrapperClass;
    private boolean useNaming;
    private String namingContextName;
    private WebResourceRoot resources;
    private final ReadWriteLock resourcesLock;
    private long startupTime;
    private long startTime;
    private long tldScanTime;
    private String j2EEApplication;
    private String j2EEServer;
    private boolean webXmlValidation;
    private boolean webXmlNamespaceAware;
    private boolean xmlBlockExternal;
    private boolean tldValidation;
    private String sessionCookieName;
    private boolean useHttpOnly;
    private String sessionCookieDomain;
    private String sessionCookiePath;
    private boolean sessionCookiePathUsesTrailingSlash;
    private JarScanner jarScanner;
    private boolean clearReferencesStatic;
    private boolean clearReferencesStopThreads;
    private boolean clearReferencesStopTimerThreads;
    private boolean clearReferencesHttpClientKeepAliveThread;
    private boolean renewThreadsWhenStoppingContext;
    private boolean logEffectiveWebXml;
    private int effectiveMajorVersion;
    private int effectiveMinorVersion;
    private JspConfigDescriptor jspConfigDescriptor;
    private Set<String> resourceOnlyServlets;
    private String webappVersion;
    private boolean addWebinfClassesResources;
    private boolean fireRequestListenersOnForwards;
    private Set<Servlet> createdServlets;
    private boolean preemptiveAuthentication;
    private boolean sendRedirectBody;
    private boolean jndiExceptionOnFailedWrite;
    private Map<String, String> postConstructMethods;
    private Map<String, String> preDestroyMethods;
    private String containerSciFilter;
    protected static final ThreadBindingListener DEFAULT_NAMING_LISTENER;
    protected ThreadBindingListener threadBindingListener;
    private MBeanNotificationInfo[] notificationInfo;
    private String server;
    private String[] javaVMs;
    
    public StandardContext() {
        this.allowCasualMultipartParsing = false;
        this.swallowAbortedUploads = true;
        this.altDDName = null;
        this.instanceManager = null;
        this.antiResourceLocking = false;
        this.applicationListeners = new ApplicationListener[0];
        this.applicationListenersLock = new Object();
        this.applicationEventListenersObjects = new Object[0];
        this.applicationLifecycleListenersObjects = new Object[0];
        this.initializers = new LinkedHashMap<ServletContainerInitializer, Set<Class<?>>>();
        this.applicationParameters = new ApplicationParameter[0];
        this.applicationParametersLock = new Object();
        this.broadcaster = null;
        this.charsetMapper = null;
        this.charsetMapperClass = "org.apache.catalina.util.CharsetMapper";
        this.configFile = null;
        this.configured = false;
        this.constraints = new SecurityConstraint[0];
        this.constraintsLock = new Object();
        this.context = null;
        this.cookies = true;
        this.crossContext = false;
        this.encodedPath = null;
        this.path = null;
        this.delegate = false;
        this.displayName = null;
        this.distributable = false;
        this.docBase = null;
        this.exceptionPages = new HashMap<String, ErrorPage>();
        this.filterConfigs = new HashMap<String, ApplicationFilterConfig>();
        this.filterDefs = new HashMap<String, FilterDef>();
        this.filterMaps = new ContextFilterMaps();
        this.ignoreAnnotations = false;
        this.instanceListeners = new String[0];
        this.instanceListenersLock = new Object();
        this.loader = null;
        this.loaderLock = new ReentrantReadWriteLock();
        this.loginConfig = null;
        this.manager = null;
        this.managerLock = new ReentrantReadWriteLock();
        this.namingContextListener = null;
        this.namingResources = null;
        this.messageDestinations = new HashMap<String, MessageDestination>();
        this.mimeMappings = new HashMap<String, String>();
        this.parameters = new HashMap<String, String>();
        this.paused = false;
        this.publicId = null;
        this.reloadable = false;
        this.unpackWAR = true;
        this.copyXML = false;
        this.override = false;
        this.originalDocBase = null;
        this.privileged = false;
        this.replaceWelcomeFiles = false;
        this.roleMappings = new HashMap<String, String>();
        this.securityRoles = new String[0];
        this.securityRolesLock = new Object();
        this.servletMappings = new HashMap<String, String>();
        this.servletMappingsLock = new Object();
        this.sessionTimeout = 30;
        this.sequenceNumber = new AtomicLong(0L);
        this.statusPages = new HashMap<Integer, ErrorPage>();
        this.swallowOutput = false;
        this.unloadDelay = 2000L;
        this.watchedResources = new String[0];
        this.watchedResourcesLock = new Object();
        this.welcomeFiles = new String[0];
        this.welcomeFilesLock = new Object();
        this.wrapperLifecycles = new String[0];
        this.wrapperLifecyclesLock = new Object();
        this.wrapperListeners = new String[0];
        this.wrapperListenersLock = new Object();
        this.workDir = null;
        this.wrapperClassName = StandardWrapper.class.getName();
        this.wrapperClass = null;
        this.useNaming = true;
        this.namingContextName = null;
        this.resourcesLock = new ReentrantReadWriteLock();
        this.j2EEApplication = "none";
        this.j2EEServer = "none";
        this.webXmlValidation = Globals.STRICT_SERVLET_COMPLIANCE;
        this.webXmlNamespaceAware = Globals.STRICT_SERVLET_COMPLIANCE;
        this.xmlBlockExternal = true;
        this.tldValidation = Globals.STRICT_SERVLET_COMPLIANCE;
        this.useHttpOnly = true;
        this.sessionCookiePathUsesTrailingSlash = true;
        this.jarScanner = null;
        this.clearReferencesStatic = false;
        this.clearReferencesStopThreads = false;
        this.clearReferencesStopTimerThreads = false;
        this.clearReferencesHttpClientKeepAliveThread = true;
        this.renewThreadsWhenStoppingContext = true;
        this.logEffectiveWebXml = false;
        this.effectiveMajorVersion = 3;
        this.effectiveMinorVersion = 0;
        this.jspConfigDescriptor = null;
        this.resourceOnlyServlets = new HashSet<String>();
        this.webappVersion = "";
        this.addWebinfClassesResources = false;
        this.fireRequestListenersOnForwards = false;
        this.createdServlets = new HashSet<Servlet>();
        this.preemptiveAuthentication = false;
        this.sendRedirectBody = false;
        this.jndiExceptionOnFailedWrite = true;
        this.postConstructMethods = new HashMap<String, String>();
        this.preDestroyMethods = new HashMap<String, String>();
        this.threadBindingListener = StandardContext.DEFAULT_NAMING_LISTENER;
        this.server = null;
        this.javaVMs = null;
        this.pipeline.setBasic(new StandardContextValve());
        this.broadcaster = new NotificationBroadcasterSupport();
        if (!Globals.STRICT_SERVLET_COMPLIANCE) {
            this.resourceOnlyServlets.add("jsp");
        }
    }
    
    @Override
    public void setContainerSciFilter(final String containerSciFilter) {
        this.containerSciFilter = containerSciFilter;
    }
    
    @Override
    public String getContainerSciFilter() {
        return this.containerSciFilter;
    }
    
    @Override
    public boolean getSendRedirectBody() {
        return this.sendRedirectBody;
    }
    
    @Override
    public void setSendRedirectBody(final boolean sendRedirectBody) {
        this.sendRedirectBody = sendRedirectBody;
    }
    
    @Override
    public boolean getPreemptiveAuthentication() {
        return this.preemptiveAuthentication;
    }
    
    @Override
    public void setPreemptiveAuthentication(final boolean preemptiveAuthentication) {
        this.preemptiveAuthentication = preemptiveAuthentication;
    }
    
    @Override
    public void setFireRequestListenersOnForwards(final boolean enable) {
        this.fireRequestListenersOnForwards = enable;
    }
    
    @Override
    public boolean getFireRequestListenersOnForwards() {
        return this.fireRequestListenersOnForwards;
    }
    
    @Override
    public void setAddWebinfClassesResources(final boolean addWebinfClassesResources) {
        this.addWebinfClassesResources = addWebinfClassesResources;
    }
    
    @Override
    public boolean getAddWebinfClassesResources() {
        return this.addWebinfClassesResources;
    }
    
    @Override
    public void setWebappVersion(final String webappVersion) {
        if (null == webappVersion) {
            this.webappVersion = "";
        }
        else {
            this.webappVersion = webappVersion;
        }
    }
    
    @Override
    public String getWebappVersion() {
        return this.webappVersion;
    }
    
    @Override
    public String getBaseName() {
        return new ContextName(this.path, this.webappVersion).getBaseName();
    }
    
    @Override
    public String getResourceOnlyServlets() {
        final StringBuilder result = new StringBuilder();
        final boolean first = true;
        for (final String servletName : this.resourceOnlyServlets) {
            if (!first) {
                result.append(',');
            }
            result.append(servletName);
        }
        return result.toString();
    }
    
    @Override
    public void setResourceOnlyServlets(final String resourceOnlyServlets) {
        this.resourceOnlyServlets.clear();
        if (resourceOnlyServlets == null) {
            return;
        }
        for (String servletName : resourceOnlyServlets.split(",")) {
            servletName = servletName.trim();
            if (servletName.length() > 0) {
                this.resourceOnlyServlets.add(servletName);
            }
        }
    }
    
    @Override
    public boolean isResourceOnlyServlet(final String servletName) {
        return this.resourceOnlyServlets.contains(servletName);
    }
    
    @Override
    public int getEffectiveMajorVersion() {
        return this.effectiveMajorVersion;
    }
    
    @Override
    public void setEffectiveMajorVersion(final int effectiveMajorVersion) {
        this.effectiveMajorVersion = effectiveMajorVersion;
    }
    
    @Override
    public int getEffectiveMinorVersion() {
        return this.effectiveMinorVersion;
    }
    
    @Override
    public void setEffectiveMinorVersion(final int effectiveMinorVersion) {
        this.effectiveMinorVersion = effectiveMinorVersion;
    }
    
    @Override
    public void setLogEffectiveWebXml(final boolean logEffectiveWebXml) {
        this.logEffectiveWebXml = logEffectiveWebXml;
    }
    
    @Override
    public boolean getLogEffectiveWebXml() {
        return this.logEffectiveWebXml;
    }
    
    @Override
    public Authenticator getAuthenticator() {
        if (this instanceof Authenticator) {
            return (Authenticator)this;
        }
        final Pipeline pipeline = this.getPipeline();
        if (pipeline != null) {
            final Valve basic = pipeline.getBasic();
            if (basic != null && basic instanceof Authenticator) {
                return (Authenticator)basic;
            }
            final Valve[] valves = pipeline.getValves();
            for (int i = 0; i < valves.length; ++i) {
                if (valves[i] instanceof Authenticator) {
                    return (Authenticator)valves[i];
                }
            }
        }
        return null;
    }
    
    @Override
    public JarScanner getJarScanner() {
        if (this.jarScanner == null) {
            this.jarScanner = (JarScanner)new StandardJarScanner();
        }
        return this.jarScanner;
    }
    
    @Override
    public void setJarScanner(final JarScanner jarScanner) {
        this.jarScanner = jarScanner;
    }
    
    @Override
    public InstanceManager getInstanceManager() {
        return this.instanceManager;
    }
    
    @Override
    public void setInstanceManager(final InstanceManager instanceManager) {
        this.instanceManager = instanceManager;
    }
    
    @Override
    public String getEncodedPath() {
        return this.encodedPath;
    }
    
    @Override
    public void setAllowCasualMultipartParsing(final boolean allowCasualMultipartParsing) {
        this.allowCasualMultipartParsing = allowCasualMultipartParsing;
    }
    
    @Override
    public boolean getAllowCasualMultipartParsing() {
        return this.allowCasualMultipartParsing;
    }
    
    @Override
    public void setSwallowAbortedUploads(final boolean swallowAbortedUploads) {
        this.swallowAbortedUploads = swallowAbortedUploads;
    }
    
    @Override
    public boolean getSwallowAbortedUploads() {
        return this.swallowAbortedUploads;
    }
    
    @Override
    public void addServletContainerInitializer(final ServletContainerInitializer sci, final Set<Class<?>> classes) {
        this.initializers.put(sci, classes);
    }
    
    public boolean getDelegate() {
        return this.delegate;
    }
    
    public void setDelegate(final boolean delegate) {
        final boolean oldDelegate = this.delegate;
        this.delegate = delegate;
        this.support.firePropertyChange("delegate", oldDelegate, this.delegate);
    }
    
    public boolean isUseNaming() {
        return this.useNaming;
    }
    
    public void setUseNaming(final boolean useNaming) {
        this.useNaming = useNaming;
    }
    
    @Override
    public Object[] getApplicationEventListeners() {
        return this.applicationEventListenersObjects;
    }
    
    @Override
    public void setApplicationEventListeners(final Object[] listeners) {
        this.applicationEventListenersObjects = listeners;
    }
    
    public void addApplicationEventListener(final Object listener) {
        final int len = this.applicationEventListenersObjects.length;
        final Object[] newListeners = Arrays.copyOf(this.applicationEventListenersObjects, len + 1);
        newListeners[len] = listener;
        this.applicationEventListenersObjects = newListeners;
    }
    
    @Override
    public Object[] getApplicationLifecycleListeners() {
        return this.applicationLifecycleListenersObjects;
    }
    
    @Override
    public void setApplicationLifecycleListeners(final Object[] listeners) {
        this.applicationLifecycleListenersObjects = listeners;
    }
    
    public void addApplicationLifecycleListener(final Object listener) {
        final int len = this.applicationLifecycleListenersObjects.length;
        final Object[] newListeners = Arrays.copyOf(this.applicationLifecycleListenersObjects, len + 1);
        newListeners[len] = listener;
        this.applicationLifecycleListenersObjects = newListeners;
    }
    
    public boolean getAntiResourceLocking() {
        return this.antiResourceLocking;
    }
    
    public void setAntiResourceLocking(final boolean antiResourceLocking) {
        final boolean oldAntiResourceLocking = this.antiResourceLocking;
        this.antiResourceLocking = antiResourceLocking;
        this.support.firePropertyChange("antiResourceLocking", oldAntiResourceLocking, this.antiResourceLocking);
    }
    
    public CharsetMapper getCharsetMapper() {
        if (this.charsetMapper == null) {
            try {
                final Class<?> clazz = Class.forName(this.charsetMapperClass);
                this.charsetMapper = (CharsetMapper)clazz.newInstance();
            }
            catch (Throwable t) {
                ExceptionUtils.handleThrowable(t);
                this.charsetMapper = new CharsetMapper();
            }
        }
        return this.charsetMapper;
    }
    
    public void setCharsetMapper(final CharsetMapper mapper) {
        final CharsetMapper oldCharsetMapper = this.charsetMapper;
        this.charsetMapper = mapper;
        if (mapper != null) {
            this.charsetMapperClass = mapper.getClass().getName();
        }
        this.support.firePropertyChange("charsetMapper", oldCharsetMapper, this.charsetMapper);
    }
    
    @Override
    public String getCharset(final Locale locale) {
        return this.getCharsetMapper().getCharset(locale);
    }
    
    @Override
    public URL getConfigFile() {
        return this.configFile;
    }
    
    @Override
    public void setConfigFile(final URL configFile) {
        this.configFile = configFile;
    }
    
    @Override
    public boolean getConfigured() {
        return this.configured;
    }
    
    @Override
    public void setConfigured(final boolean configured) {
        final boolean oldConfigured = this.configured;
        this.configured = configured;
        this.support.firePropertyChange("configured", oldConfigured, this.configured);
    }
    
    @Override
    public boolean getCookies() {
        return this.cookies;
    }
    
    @Override
    public void setCookies(final boolean cookies) {
        final boolean oldCookies = this.cookies;
        this.cookies = cookies;
        this.support.firePropertyChange("cookies", oldCookies, this.cookies);
    }
    
    @Override
    public String getSessionCookieName() {
        return this.sessionCookieName;
    }
    
    @Override
    public void setSessionCookieName(final String sessionCookieName) {
        final String oldSessionCookieName = this.sessionCookieName;
        this.sessionCookieName = sessionCookieName;
        this.support.firePropertyChange("sessionCookieName", oldSessionCookieName, sessionCookieName);
    }
    
    @Override
    public boolean getUseHttpOnly() {
        return this.useHttpOnly;
    }
    
    @Override
    public void setUseHttpOnly(final boolean useHttpOnly) {
        final boolean oldUseHttpOnly = this.useHttpOnly;
        this.useHttpOnly = useHttpOnly;
        this.support.firePropertyChange("useHttpOnly", oldUseHttpOnly, this.useHttpOnly);
    }
    
    @Override
    public String getSessionCookieDomain() {
        return this.sessionCookieDomain;
    }
    
    @Override
    public void setSessionCookieDomain(final String sessionCookieDomain) {
        final String oldSessionCookieDomain = this.sessionCookieDomain;
        this.sessionCookieDomain = sessionCookieDomain;
        this.support.firePropertyChange("sessionCookieDomain", oldSessionCookieDomain, sessionCookieDomain);
    }
    
    @Override
    public String getSessionCookiePath() {
        return this.sessionCookiePath;
    }
    
    @Override
    public void setSessionCookiePath(final String sessionCookiePath) {
        final String oldSessionCookiePath = this.sessionCookiePath;
        this.sessionCookiePath = sessionCookiePath;
        this.support.firePropertyChange("sessionCookiePath", oldSessionCookiePath, sessionCookiePath);
    }
    
    @Override
    public boolean getSessionCookiePathUsesTrailingSlash() {
        return this.sessionCookiePathUsesTrailingSlash;
    }
    
    @Override
    public void setSessionCookiePathUsesTrailingSlash(final boolean sessionCookiePathUsesTrailingSlash) {
        this.sessionCookiePathUsesTrailingSlash = sessionCookiePathUsesTrailingSlash;
    }
    
    @Override
    public boolean getCrossContext() {
        return this.crossContext;
    }
    
    @Override
    public void setCrossContext(final boolean crossContext) {
        final boolean oldCrossContext = this.crossContext;
        this.crossContext = crossContext;
        this.support.firePropertyChange("crossContext", oldCrossContext, this.crossContext);
    }
    
    public String getDefaultContextXml() {
        return this.defaultContextXml;
    }
    
    public void setDefaultContextXml(final String defaultContextXml) {
        this.defaultContextXml = defaultContextXml;
    }
    
    public String getDefaultWebXml() {
        return this.defaultWebXml;
    }
    
    public void setDefaultWebXml(final String defaultWebXml) {
        this.defaultWebXml = defaultWebXml;
    }
    
    public long getStartupTime() {
        return this.startupTime;
    }
    
    public void setStartupTime(final long startupTime) {
        this.startupTime = startupTime;
    }
    
    public long getTldScanTime() {
        return this.tldScanTime;
    }
    
    public void setTldScanTime(final long tldScanTime) {
        this.tldScanTime = tldScanTime;
    }
    
    @Override
    public boolean getDenyUncoveredHttpMethods() {
        return this.denyUncoveredHttpMethods;
    }
    
    @Override
    public void setDenyUncoveredHttpMethods(final boolean denyUncoveredHttpMethods) {
        this.denyUncoveredHttpMethods = denyUncoveredHttpMethods;
    }
    
    @Override
    public String getDisplayName() {
        return this.displayName;
    }
    
    @Override
    public String getAltDDName() {
        return this.altDDName;
    }
    
    @Override
    public void setAltDDName(final String altDDName) {
        this.altDDName = altDDName;
        if (this.context != null) {
            this.context.setAttribute("org.apache.catalina.deploy.alt_dd", altDDName);
        }
    }
    
    @Override
    public void setDisplayName(final String displayName) {
        final String oldDisplayName = this.displayName;
        this.displayName = displayName;
        this.support.firePropertyChange("displayName", oldDisplayName, this.displayName);
    }
    
    @Override
    public boolean getDistributable() {
        return this.distributable;
    }
    
    @Override
    public void setDistributable(final boolean distributable) {
        final boolean oldDistributable = this.distributable;
        this.distributable = distributable;
        this.support.firePropertyChange("distributable", oldDistributable, this.distributable);
        if (this.getManager() != null) {
            if (StandardContext.log.isDebugEnabled()) {
                StandardContext.log.debug((Object)("Propagating distributable=" + distributable + " to manager"));
            }
            this.getManager().setDistributable(distributable);
        }
    }
    
    @Override
    public String getDocBase() {
        return this.docBase;
    }
    
    @Override
    public void setDocBase(final String docBase) {
        this.docBase = docBase;
    }
    
    public String getJ2EEApplication() {
        return this.j2EEApplication;
    }
    
    public void setJ2EEApplication(final String j2EEApplication) {
        this.j2EEApplication = j2EEApplication;
    }
    
    public String getJ2EEServer() {
        return this.j2EEServer;
    }
    
    public void setJ2EEServer(final String j2EEServer) {
        this.j2EEServer = j2EEServer;
    }
    
    @Override
    public Loader getLoader() {
        final Lock readLock = this.loaderLock.readLock();
        readLock.lock();
        try {
            return this.loader;
        }
        finally {
            readLock.unlock();
        }
    }
    
    @Override
    public void setLoader(final Loader loader) {
        final Lock writeLock = this.loaderLock.writeLock();
        writeLock.lock();
        Loader oldLoader = null;
        try {
            oldLoader = this.loader;
            if (oldLoader == loader) {
                return;
            }
            this.loader = loader;
            if (this.getState().isAvailable() && oldLoader != null && oldLoader instanceof Lifecycle) {
                try {
                    ((Lifecycle)oldLoader).stop();
                }
                catch (LifecycleException e) {
                    StandardContext.log.error((Object)"StandardContext.setLoader: stop: ", (Throwable)e);
                }
            }
            if (loader != null) {
                loader.setContext(this);
            }
            if (this.getState().isAvailable() && loader != null && loader instanceof Lifecycle) {
                try {
                    ((Lifecycle)loader).start();
                }
                catch (LifecycleException e) {
                    StandardContext.log.error((Object)"StandardContext.setLoader: start: ", (Throwable)e);
                }
            }
        }
        finally {
            writeLock.unlock();
        }
        this.support.firePropertyChange("loader", oldLoader, loader);
    }
    
    @Override
    public Manager getManager() {
        final Lock readLock = this.managerLock.readLock();
        readLock.lock();
        try {
            return this.manager;
        }
        finally {
            readLock.unlock();
        }
    }
    
    @Override
    public void setManager(final Manager manager) {
        final Lock writeLock = this.managerLock.writeLock();
        writeLock.lock();
        Manager oldManager = null;
        try {
            oldManager = this.manager;
            if (oldManager == manager) {
                return;
            }
            this.manager = manager;
            if (this.getState().isAvailable() && oldManager != null && oldManager instanceof Lifecycle) {
                try {
                    ((Lifecycle)oldManager).stop();
                }
                catch (LifecycleException e) {
                    StandardContext.log.error((Object)"StandardContext.setManager: stop: ", (Throwable)e);
                }
            }
            if (manager != null) {
                manager.setContext(this);
            }
            if (this.getState().isAvailable() && manager != null && manager instanceof Lifecycle) {
                try {
                    ((Lifecycle)manager).start();
                }
                catch (LifecycleException e) {
                    StandardContext.log.error((Object)"StandardContext.setManager: start: ", (Throwable)e);
                }
            }
        }
        finally {
            writeLock.unlock();
        }
        this.support.firePropertyChange("manager", oldManager, manager);
    }
    
    @Override
    public boolean getIgnoreAnnotations() {
        return this.ignoreAnnotations;
    }
    
    @Override
    public void setIgnoreAnnotations(final boolean ignoreAnnotations) {
        final boolean oldIgnoreAnnotations = this.ignoreAnnotations;
        this.ignoreAnnotations = ignoreAnnotations;
        this.support.firePropertyChange("ignoreAnnotations", oldIgnoreAnnotations, this.ignoreAnnotations);
    }
    
    @Override
    public LoginConfig getLoginConfig() {
        return this.loginConfig;
    }
    
    @Override
    public void setLoginConfig(final LoginConfig config) {
        if (config == null) {
            throw new IllegalArgumentException(StandardContext.sm.getString("standardContext.loginConfig.required"));
        }
        final String loginPage = config.getLoginPage();
        if (loginPage != null && !loginPage.startsWith("/")) {
            if (!this.isServlet22()) {
                throw new IllegalArgumentException(StandardContext.sm.getString("standardContext.loginConfig.loginPage", new Object[] { loginPage }));
            }
            if (StandardContext.log.isDebugEnabled()) {
                StandardContext.log.debug((Object)StandardContext.sm.getString("standardContext.loginConfig.loginWarning", new Object[] { loginPage }));
            }
            config.setLoginPage("/" + loginPage);
        }
        final String errorPage = config.getErrorPage();
        if (errorPage != null && !errorPage.startsWith("/")) {
            if (!this.isServlet22()) {
                throw new IllegalArgumentException(StandardContext.sm.getString("standardContext.loginConfig.errorPage", new Object[] { errorPage }));
            }
            if (StandardContext.log.isDebugEnabled()) {
                StandardContext.log.debug((Object)StandardContext.sm.getString("standardContext.loginConfig.errorWarning", new Object[] { errorPage }));
            }
            config.setErrorPage("/" + errorPage);
        }
        final LoginConfig oldLoginConfig = this.loginConfig;
        this.loginConfig = config;
        this.support.firePropertyChange("loginConfig", oldLoginConfig, this.loginConfig);
    }
    
    @Override
    public NamingResourcesImpl getNamingResources() {
        if (this.namingResources == null) {
            this.setNamingResources(new NamingResourcesImpl());
        }
        return this.namingResources;
    }
    
    @Override
    public void setNamingResources(final NamingResourcesImpl namingResources) {
        final NamingResourcesImpl oldNamingResources = this.namingResources;
        this.namingResources = namingResources;
        if (namingResources != null) {
            namingResources.setContainer(this);
        }
        this.support.firePropertyChange("namingResources", oldNamingResources, this.namingResources);
        if (this.getState() == LifecycleState.NEW || this.getState() == LifecycleState.INITIALIZING || this.getState() == LifecycleState.INITIALIZED) {
            return;
        }
        if (oldNamingResources != null) {
            try {
                oldNamingResources.stop();
                oldNamingResources.destroy();
            }
            catch (LifecycleException e) {
                StandardContext.log.warn((Object)"standardContext.namingResource.destroy.fail", (Throwable)e);
            }
        }
        if (namingResources != null) {
            try {
                namingResources.init();
                namingResources.start();
            }
            catch (LifecycleException e) {
                StandardContext.log.warn((Object)"standardContext.namingResource.init.fail", (Throwable)e);
            }
        }
    }
    
    @Override
    public String getPath() {
        return this.path;
    }
    
    @Override
    public void setPath(final String path) {
        if (path == null || (!path.equals("") && !path.startsWith("/"))) {
            this.path = "/" + path;
            StandardContext.log.warn((Object)StandardContext.sm.getString("standardContext.pathInvalid", new Object[] { path, this.path }));
        }
        else {
            this.path = path;
        }
        this.encodedPath = StandardContext.urlEncoder.encode(this.path);
        if (this.getName() == null) {
            this.setName(this.path);
        }
    }
    
    @Override
    public String getPublicId() {
        return this.publicId;
    }
    
    @Override
    public void setPublicId(final String publicId) {
        if (StandardContext.log.isDebugEnabled()) {
            StandardContext.log.debug((Object)("Setting deployment descriptor public ID to '" + publicId + "'"));
        }
        final String oldPublicId = this.publicId;
        this.publicId = publicId;
        this.support.firePropertyChange("publicId", oldPublicId, publicId);
    }
    
    @Override
    public boolean getReloadable() {
        return this.reloadable;
    }
    
    @Override
    public boolean getOverride() {
        return this.override;
    }
    
    public String getOriginalDocBase() {
        return this.originalDocBase;
    }
    
    public void setOriginalDocBase(final String docBase) {
        this.originalDocBase = docBase;
    }
    
    @Override
    public ClassLoader getParentClassLoader() {
        if (this.parentClassLoader != null) {
            return this.parentClassLoader;
        }
        if (this.getPrivileged()) {
            return this.getClass().getClassLoader();
        }
        if (this.parent != null) {
            return this.parent.getParentClassLoader();
        }
        return ClassLoader.getSystemClassLoader();
    }
    
    @Override
    public boolean getPrivileged() {
        return this.privileged;
    }
    
    @Override
    public void setPrivileged(final boolean privileged) {
        final boolean oldPrivileged = this.privileged;
        this.privileged = privileged;
        this.support.firePropertyChange("privileged", oldPrivileged, this.privileged);
    }
    
    @Override
    public void setReloadable(final boolean reloadable) {
        final boolean oldReloadable = this.reloadable;
        this.reloadable = reloadable;
        this.support.firePropertyChange("reloadable", oldReloadable, this.reloadable);
    }
    
    @Override
    public void setOverride(final boolean override) {
        final boolean oldOverride = this.override;
        this.override = override;
        this.support.firePropertyChange("override", oldOverride, this.override);
    }
    
    public void setReplaceWelcomeFiles(final boolean replaceWelcomeFiles) {
        final boolean oldReplaceWelcomeFiles = this.replaceWelcomeFiles;
        this.replaceWelcomeFiles = replaceWelcomeFiles;
        this.support.firePropertyChange("replaceWelcomeFiles", oldReplaceWelcomeFiles, this.replaceWelcomeFiles);
    }
    
    @Override
    public ServletContext getServletContext() {
        if (this.context == null) {
            this.context = new ApplicationContext(this);
            if (this.altDDName != null) {
                this.context.setAttribute("org.apache.catalina.deploy.alt_dd", this.altDDName);
            }
        }
        return this.context.getFacade();
    }
    
    @Override
    public int getSessionTimeout() {
        return this.sessionTimeout;
    }
    
    @Override
    public void setSessionTimeout(final int timeout) {
        final int oldSessionTimeout = this.sessionTimeout;
        this.sessionTimeout = ((timeout == 0) ? -1 : timeout);
        this.support.firePropertyChange("sessionTimeout", oldSessionTimeout, this.sessionTimeout);
    }
    
    @Override
    public boolean getSwallowOutput() {
        return this.swallowOutput;
    }
    
    @Override
    public void setSwallowOutput(final boolean swallowOutput) {
        final boolean oldSwallowOutput = this.swallowOutput;
        this.swallowOutput = swallowOutput;
        this.support.firePropertyChange("swallowOutput", oldSwallowOutput, this.swallowOutput);
    }
    
    public long getUnloadDelay() {
        return this.unloadDelay;
    }
    
    public void setUnloadDelay(final long unloadDelay) {
        final long oldUnloadDelay = this.unloadDelay;
        this.unloadDelay = unloadDelay;
        this.support.firePropertyChange("unloadDelay", oldUnloadDelay, this.unloadDelay);
    }
    
    public boolean getUnpackWAR() {
        return this.unpackWAR;
    }
    
    public void setUnpackWAR(final boolean unpackWAR) {
        this.unpackWAR = unpackWAR;
    }
    
    public boolean getCopyXML() {
        return this.copyXML;
    }
    
    public void setCopyXML(final boolean copyXML) {
        this.copyXML = copyXML;
    }
    
    @Override
    public String getWrapperClass() {
        return this.wrapperClassName;
    }
    
    @Override
    public void setWrapperClass(final String wrapperClassName) {
        this.wrapperClassName = wrapperClassName;
        try {
            this.wrapperClass = Class.forName(wrapperClassName);
            if (!StandardWrapper.class.isAssignableFrom(this.wrapperClass)) {
                throw new IllegalArgumentException(StandardContext.sm.getString("standardContext.invalidWrapperClass", new Object[] { wrapperClassName }));
            }
        }
        catch (ClassNotFoundException cnfe) {
            throw new IllegalArgumentException(cnfe.getMessage());
        }
    }
    
    @Override
    public WebResourceRoot getResources() {
        final Lock readLock = this.resourcesLock.readLock();
        readLock.lock();
        try {
            return this.resources;
        }
        finally {
            readLock.unlock();
        }
    }
    
    @Override
    public void setResources(final WebResourceRoot resources) {
        final Lock writeLock = this.resourcesLock.writeLock();
        writeLock.lock();
        WebResourceRoot oldResources = null;
        try {
            if (this.getState().isAvailable()) {
                throw new IllegalStateException(StandardContext.sm.getString("standardContext.resources.started"));
            }
            oldResources = this.resources;
            if (oldResources == resources) {
                return;
            }
            this.resources = resources;
            if (oldResources != null) {
                oldResources.setContext(null);
            }
            if (resources != null) {
                resources.setContext(this);
            }
            this.support.firePropertyChange("resources", oldResources, resources);
        }
        finally {
            writeLock.unlock();
        }
    }
    
    @Override
    public JspConfigDescriptor getJspConfigDescriptor() {
        return this.jspConfigDescriptor;
    }
    
    @Override
    public void setJspConfigDescriptor(final JspConfigDescriptor descriptor) {
        this.jspConfigDescriptor = descriptor;
    }
    
    @Override
    public ThreadBindingListener getThreadBindingListener() {
        return this.threadBindingListener;
    }
    
    @Override
    public void setThreadBindingListener(final ThreadBindingListener threadBindingListener) {
        this.threadBindingListener = threadBindingListener;
    }
    
    public boolean getJndiExceptionOnFailedWrite() {
        return this.jndiExceptionOnFailedWrite;
    }
    
    public void setJndiExceptionOnFailedWrite(final boolean jndiExceptionOnFailedWrite) {
        this.jndiExceptionOnFailedWrite = jndiExceptionOnFailedWrite;
    }
    
    public String getCharsetMapperClass() {
        return this.charsetMapperClass;
    }
    
    public void setCharsetMapperClass(final String mapper) {
        final String oldCharsetMapperClass = this.charsetMapperClass;
        this.charsetMapperClass = mapper;
        this.support.firePropertyChange("charsetMapperClass", oldCharsetMapperClass, this.charsetMapperClass);
    }
    
    public String getWorkPath() {
        if (this.getWorkDir() == null) {
            return null;
        }
        File workDir = new File(this.getWorkDir());
        if (!workDir.isAbsolute()) {
            try {
                workDir = new File(this.getCatalinaBase().getCanonicalFile(), this.getWorkDir());
            }
            catch (IOException e) {
                StandardContext.log.warn((Object)StandardContext.sm.getString("standardContext.workPath", new Object[] { this.getName() }), (Throwable)e);
            }
        }
        return workDir.getAbsolutePath();
    }
    
    public String getWorkDir() {
        return this.workDir;
    }
    
    public void setWorkDir(final String workDir) {
        this.workDir = workDir;
        if (this.getState().isAvailable()) {
            this.postWorkDirectory();
        }
    }
    
    public boolean getClearReferencesStatic() {
        return this.clearReferencesStatic;
    }
    
    public void setClearReferencesStatic(final boolean clearReferencesStatic) {
        final boolean oldClearReferencesStatic = this.clearReferencesStatic;
        this.clearReferencesStatic = clearReferencesStatic;
        this.support.firePropertyChange("clearReferencesStatic", oldClearReferencesStatic, this.clearReferencesStatic);
    }
    
    public boolean getClearReferencesStopThreads() {
        return this.clearReferencesStopThreads;
    }
    
    public void setClearReferencesStopThreads(final boolean clearReferencesStopThreads) {
        final boolean oldClearReferencesStopThreads = this.clearReferencesStopThreads;
        this.clearReferencesStopThreads = clearReferencesStopThreads;
        this.support.firePropertyChange("clearReferencesStopThreads", oldClearReferencesStopThreads, this.clearReferencesStopThreads);
    }
    
    public boolean getClearReferencesStopTimerThreads() {
        return this.clearReferencesStopTimerThreads;
    }
    
    public void setClearReferencesStopTimerThreads(final boolean clearReferencesStopTimerThreads) {
        final boolean oldClearReferencesStopTimerThreads = this.clearReferencesStopTimerThreads;
        this.clearReferencesStopTimerThreads = clearReferencesStopTimerThreads;
        this.support.firePropertyChange("clearReferencesStopTimerThreads", oldClearReferencesStopTimerThreads, this.clearReferencesStopTimerThreads);
    }
    
    public boolean getClearReferencesHttpClientKeepAliveThread() {
        return this.clearReferencesHttpClientKeepAliveThread;
    }
    
    public void setClearReferencesHttpClientKeepAliveThread(final boolean clearReferencesHttpClientKeepAliveThread) {
        this.clearReferencesHttpClientKeepAliveThread = clearReferencesHttpClientKeepAliveThread;
    }
    
    public boolean getRenewThreadsWhenStoppingContext() {
        return this.renewThreadsWhenStoppingContext;
    }
    
    public void setRenewThreadsWhenStoppingContext(final boolean renewThreadsWhenStoppingContext) {
        final boolean oldRenewThreadsWhenStoppingContext = this.renewThreadsWhenStoppingContext;
        this.renewThreadsWhenStoppingContext = renewThreadsWhenStoppingContext;
        this.support.firePropertyChange("renewThreadsWhenStoppingContext", oldRenewThreadsWhenStoppingContext, this.renewThreadsWhenStoppingContext);
    }
    
    @Override
    public void addApplicationListener(final ApplicationListener listener) {
        synchronized (this.applicationListenersLock) {
            final ApplicationListener[] results = new ApplicationListener[this.applicationListeners.length + 1];
            for (int i = 0; i < this.applicationListeners.length; ++i) {
                if (listener.equals(this.applicationListeners[i])) {
                    StandardContext.log.info((Object)StandardContext.sm.getString("standardContext.duplicateListener", new Object[] { listener }));
                    return;
                }
                results[i] = this.applicationListeners[i];
            }
            results[this.applicationListeners.length] = listener;
            this.applicationListeners = results;
        }
        this.fireContainerEvent("addApplicationListener", listener);
    }
    
    @Override
    public void addApplicationParameter(final ApplicationParameter parameter) {
        synchronized (this.applicationParametersLock) {
            final String newName = parameter.getName();
            for (final ApplicationParameter p : this.applicationParameters) {
                if (newName.equals(p.getName()) && !p.getOverride()) {
                    return;
                }
            }
            final ApplicationParameter[] results = Arrays.copyOf(this.applicationParameters, this.applicationParameters.length + 1);
            results[this.applicationParameters.length] = parameter;
            this.applicationParameters = results;
        }
        this.fireContainerEvent("addApplicationParameter", parameter);
    }
    
    @Override
    public void addChild(final Container child) {
        Wrapper oldJspServlet = null;
        if (!(child instanceof Wrapper)) {
            throw new IllegalArgumentException(StandardContext.sm.getString("standardContext.notWrapper"));
        }
        final boolean isJspServlet = "jsp".equals(child.getName());
        if (isJspServlet) {
            oldJspServlet = (Wrapper)this.findChild("jsp");
            if (oldJspServlet != null) {
                this.removeChild(oldJspServlet);
            }
        }
        super.addChild(child);
        if (isJspServlet && oldJspServlet != null) {
            final String[] jspMappings = oldJspServlet.findMappings();
            for (int i = 0; jspMappings != null && i < jspMappings.length; ++i) {
                this.addServletMapping(jspMappings[i], child.getName());
            }
        }
    }
    
    @Override
    public void addConstraint(final SecurityConstraint constraint) {
        final SecurityCollection[] collections = constraint.findCollections();
        for (int i = 0; i < collections.length; ++i) {
            final String[] patterns = collections[i].findPatterns();
            for (int j = 0; j < patterns.length; ++j) {
                patterns[j] = this.adjustURLPattern(patterns[j]);
                if (!this.validateURLPattern(patterns[j])) {
                    throw new IllegalArgumentException(StandardContext.sm.getString("standardContext.securityConstraint.pattern", new Object[] { patterns[j] }));
                }
            }
            if (collections[i].findMethods().length > 0 && collections[i].findOmittedMethods().length > 0) {
                throw new IllegalArgumentException(StandardContext.sm.getString("standardContext.securityConstraint.mixHttpMethod"));
            }
        }
        synchronized (this.constraintsLock) {
            final SecurityConstraint[] results = new SecurityConstraint[this.constraints.length + 1];
            for (int k = 0; k < this.constraints.length; ++k) {
                results[k] = this.constraints[k];
            }
            results[this.constraints.length] = constraint;
            this.constraints = results;
        }
    }
    
    @Override
    public void addErrorPage(final ErrorPage errorPage) {
        if (errorPage == null) {
            throw new IllegalArgumentException(StandardContext.sm.getString("standardContext.errorPage.required"));
        }
        final String location = errorPage.getLocation();
        if (location != null && !location.startsWith("/")) {
            if (!this.isServlet22()) {
                throw new IllegalArgumentException(StandardContext.sm.getString("standardContext.errorPage.error", new Object[] { location }));
            }
            if (StandardContext.log.isDebugEnabled()) {
                StandardContext.log.debug((Object)StandardContext.sm.getString("standardContext.errorPage.warning", new Object[] { location }));
            }
            errorPage.setLocation("/" + location);
        }
        final String exceptionType = errorPage.getExceptionType();
        if (exceptionType != null) {
            synchronized (this.exceptionPages) {
                this.exceptionPages.put(exceptionType, errorPage);
            }
        }
        else {
            synchronized (this.statusPages) {
                this.statusPages.put(errorPage.getErrorCode(), errorPage);
            }
        }
        this.fireContainerEvent("addErrorPage", errorPage);
    }
    
    @Override
    public void addFilterDef(final FilterDef filterDef) {
        synchronized (this.filterDefs) {
            this.filterDefs.put(filterDef.getFilterName(), filterDef);
        }
        this.fireContainerEvent("addFilterDef", filterDef);
    }
    
    @Override
    public void addFilterMap(final FilterMap filterMap) {
        this.validateFilterMap(filterMap);
        this.filterMaps.add(filterMap);
        this.fireContainerEvent("addFilterMap", filterMap);
    }
    
    @Override
    public void addFilterMapBefore(final FilterMap filterMap) {
        this.validateFilterMap(filterMap);
        this.filterMaps.addBefore(filterMap);
        this.fireContainerEvent("addFilterMap", filterMap);
    }
    
    private void validateFilterMap(final FilterMap filterMap) {
        final String filterName = filterMap.getFilterName();
        final String[] servletNames = filterMap.getServletNames();
        final String[] urlPatterns = filterMap.getURLPatterns();
        if (this.findFilterDef(filterName) == null) {
            throw new IllegalArgumentException(StandardContext.sm.getString("standardContext.filterMap.name", new Object[] { filterName }));
        }
        if (!filterMap.getMatchAllServletNames() && !filterMap.getMatchAllUrlPatterns() && servletNames.length == 0 && urlPatterns.length == 0) {
            throw new IllegalArgumentException(StandardContext.sm.getString("standardContext.filterMap.either"));
        }
        for (int i = 0; i < urlPatterns.length; ++i) {
            if (!this.validateURLPattern(urlPatterns[i])) {
                throw new IllegalArgumentException(StandardContext.sm.getString("standardContext.filterMap.pattern", new Object[] { urlPatterns[i] }));
            }
        }
    }
    
    @Override
    public void addInstanceListener(final String listener) {
        synchronized (this.instanceListenersLock) {
            final String[] results = new String[this.instanceListeners.length + 1];
            for (int i = 0; i < this.instanceListeners.length; ++i) {
                results[i] = this.instanceListeners[i];
            }
            results[this.instanceListeners.length] = listener;
            this.instanceListeners = results;
        }
        this.fireContainerEvent("addInstanceListener", listener);
    }
    
    @Override
    public void addLocaleEncodingMappingParameter(final String locale, final String encoding) {
        this.getCharsetMapper().addCharsetMappingFromDeploymentDescriptor(locale, encoding);
    }
    
    public void addMessageDestination(final MessageDestination md) {
        synchronized (this.messageDestinations) {
            this.messageDestinations.put(md.getName(), md);
        }
        this.fireContainerEvent("addMessageDestination", md.getName());
    }
    
    public void addMessageDestinationRef(final MessageDestinationRef mdr) {
        this.namingResources.addMessageDestinationRef(mdr);
        this.fireContainerEvent("addMessageDestinationRef", mdr.getName());
    }
    
    @Override
    public void addMimeMapping(final String extension, final String mimeType) {
        synchronized (this.mimeMappings) {
            this.mimeMappings.put(extension.toLowerCase(Locale.ENGLISH), mimeType);
        }
        this.fireContainerEvent("addMimeMapping", extension);
    }
    
    @Override
    public void addParameter(final String name, final String value) {
        if (name == null || value == null) {
            throw new IllegalArgumentException(StandardContext.sm.getString("standardContext.parameter.required"));
        }
        if (this.parameters.get(name) != null) {
            throw new IllegalArgumentException(StandardContext.sm.getString("standardContext.parameter.duplicate", new Object[] { name }));
        }
        synchronized (this.parameters) {
            this.parameters.put(name, value);
        }
        this.fireContainerEvent("addParameter", name);
    }
    
    @Override
    public void addRoleMapping(final String role, final String link) {
        synchronized (this.roleMappings) {
            this.roleMappings.put(role, link);
        }
        this.fireContainerEvent("addRoleMapping", role);
    }
    
    @Override
    public void addSecurityRole(final String role) {
        synchronized (this.securityRolesLock) {
            final String[] results = new String[this.securityRoles.length + 1];
            for (int i = 0; i < this.securityRoles.length; ++i) {
                results[i] = this.securityRoles[i];
            }
            results[this.securityRoles.length] = role;
            this.securityRoles = results;
        }
        this.fireContainerEvent("addSecurityRole", role);
    }
    
    @Override
    public void addServletMapping(final String pattern, final String name) {
        this.addServletMapping(pattern, name, false);
    }
    
    @Override
    public void addServletMapping(final String pattern, final String name, final boolean jspWildCard) {
        if (this.findChild(name) == null) {
            throw new IllegalArgumentException(StandardContext.sm.getString("standardContext.servletMap.name", new Object[] { name }));
        }
        final String decodedPattern = this.adjustURLPattern(UDecoder.URLDecode(pattern));
        if (!this.validateURLPattern(decodedPattern)) {
            throw new IllegalArgumentException(StandardContext.sm.getString("standardContext.servletMap.pattern", new Object[] { decodedPattern }));
        }
        synchronized (this.servletMappingsLock) {
            final String name2 = this.servletMappings.get(decodedPattern);
            if (name2 != null) {
                final Wrapper wrapper = (Wrapper)this.findChild(name2);
                wrapper.removeMapping(decodedPattern);
            }
            this.servletMappings.put(decodedPattern, name);
        }
        final Wrapper wrapper2 = (Wrapper)this.findChild(name);
        wrapper2.addMapping(decodedPattern);
        this.fireContainerEvent("addServletMapping", decodedPattern);
    }
    
    @Override
    public void addWatchedResource(final String name) {
        synchronized (this.watchedResourcesLock) {
            final String[] results = new String[this.watchedResources.length + 1];
            for (int i = 0; i < this.watchedResources.length; ++i) {
                results[i] = this.watchedResources[i];
            }
            results[this.watchedResources.length] = name;
            this.watchedResources = results;
        }
        this.fireContainerEvent("addWatchedResource", name);
    }
    
    @Override
    public void addWelcomeFile(final String name) {
        synchronized (this.welcomeFilesLock) {
            if (this.replaceWelcomeFiles) {
                this.fireContainerEvent("clearWelcomeFiles", null);
                this.welcomeFiles = new String[0];
                this.setReplaceWelcomeFiles(false);
            }
            final String[] results = new String[this.welcomeFiles.length + 1];
            for (int i = 0; i < this.welcomeFiles.length; ++i) {
                results[i] = this.welcomeFiles[i];
            }
            results[this.welcomeFiles.length] = name;
            this.welcomeFiles = results;
        }
        if (this.getState().equals(LifecycleState.STARTED)) {
            this.fireContainerEvent("addWelcomeFile", name);
        }
    }
    
    @Override
    public void addWrapperLifecycle(final String listener) {
        synchronized (this.wrapperLifecyclesLock) {
            final String[] results = new String[this.wrapperLifecycles.length + 1];
            for (int i = 0; i < this.wrapperLifecycles.length; ++i) {
                results[i] = this.wrapperLifecycles[i];
            }
            results[this.wrapperLifecycles.length] = listener;
            this.wrapperLifecycles = results;
        }
        this.fireContainerEvent("addWrapperLifecycle", listener);
    }
    
    @Override
    public void addWrapperListener(final String listener) {
        synchronized (this.wrapperListenersLock) {
            final String[] results = new String[this.wrapperListeners.length + 1];
            for (int i = 0; i < this.wrapperListeners.length; ++i) {
                results[i] = this.wrapperListeners[i];
            }
            results[this.wrapperListeners.length] = listener;
            this.wrapperListeners = results;
        }
        this.fireContainerEvent("addWrapperListener", listener);
    }
    
    @Override
    public Wrapper createWrapper() {
        Wrapper wrapper = null;
        Label_0050: {
            if (this.wrapperClass != null) {
                try {
                    wrapper = (Wrapper)this.wrapperClass.newInstance();
                    break Label_0050;
                }
                catch (Throwable t) {
                    ExceptionUtils.handleThrowable(t);
                    StandardContext.log.error((Object)"createWrapper", t);
                    return null;
                }
            }
            wrapper = new StandardWrapper();
        }
        synchronized (this.instanceListenersLock) {
            for (int i = 0; i < this.instanceListeners.length; ++i) {
                try {
                    final Class<?> clazz = Class.forName(this.instanceListeners[i]);
                    final InstanceListener listener = (InstanceListener)clazz.newInstance();
                    wrapper.addInstanceListener(listener);
                }
                catch (Throwable t2) {
                    ExceptionUtils.handleThrowable(t2);
                    StandardContext.log.error((Object)"createWrapper", t2);
                    return null;
                }
            }
        }
        synchronized (this.wrapperLifecyclesLock) {
            for (int i = 0; i < this.wrapperLifecycles.length; ++i) {
                try {
                    final Class<?> clazz = Class.forName(this.wrapperLifecycles[i]);
                    final LifecycleListener listener2 = (LifecycleListener)clazz.newInstance();
                    wrapper.addLifecycleListener(listener2);
                }
                catch (Throwable t2) {
                    ExceptionUtils.handleThrowable(t2);
                    StandardContext.log.error((Object)"createWrapper", t2);
                    return null;
                }
            }
        }
        synchronized (this.wrapperListenersLock) {
            for (int i = 0; i < this.wrapperListeners.length; ++i) {
                try {
                    final Class<?> clazz = Class.forName(this.wrapperListeners[i]);
                    final ContainerListener listener3 = (ContainerListener)clazz.newInstance();
                    wrapper.addContainerListener(listener3);
                }
                catch (Throwable t2) {
                    ExceptionUtils.handleThrowable(t2);
                    StandardContext.log.error((Object)"createWrapper", t2);
                    return null;
                }
            }
        }
        return wrapper;
    }
    
    @Override
    public ApplicationListener[] findApplicationListeners() {
        return this.applicationListeners;
    }
    
    @Override
    public ApplicationParameter[] findApplicationParameters() {
        synchronized (this.applicationParametersLock) {
            return this.applicationParameters;
        }
    }
    
    @Override
    public SecurityConstraint[] findConstraints() {
        return this.constraints;
    }
    
    @Override
    public ErrorPage findErrorPage(final int errorCode) {
        return this.statusPages.get(errorCode);
    }
    
    @Override
    public ErrorPage findErrorPage(final String exceptionType) {
        synchronized (this.exceptionPages) {
            return this.exceptionPages.get(exceptionType);
        }
    }
    
    @Override
    public ErrorPage[] findErrorPages() {
        synchronized (this.exceptionPages) {
            synchronized (this.statusPages) {
                ErrorPage[] results1 = new ErrorPage[this.exceptionPages.size()];
                results1 = this.exceptionPages.values().toArray(results1);
                ErrorPage[] results2 = new ErrorPage[this.statusPages.size()];
                results2 = this.statusPages.values().toArray(results2);
                final ErrorPage[] results3 = new ErrorPage[results1.length + results2.length];
                for (int i = 0; i < results1.length; ++i) {
                    results3[i] = results1[i];
                }
                for (int i = results1.length; i < results3.length; ++i) {
                    results3[i] = results2[i - results1.length];
                }
                return results3;
            }
        }
    }
    
    @Override
    public FilterDef findFilterDef(final String filterName) {
        synchronized (this.filterDefs) {
            return this.filterDefs.get(filterName);
        }
    }
    
    @Override
    public FilterDef[] findFilterDefs() {
        synchronized (this.filterDefs) {
            final FilterDef[] results = new FilterDef[this.filterDefs.size()];
            return this.filterDefs.values().toArray(results);
        }
    }
    
    @Override
    public FilterMap[] findFilterMaps() {
        return this.filterMaps.asArray();
    }
    
    @Override
    public String[] findInstanceListeners() {
        synchronized (this.instanceListenersLock) {
            return this.instanceListeners;
        }
    }
    
    public MessageDestination findMessageDestination(final String name) {
        synchronized (this.messageDestinations) {
            return this.messageDestinations.get(name);
        }
    }
    
    public MessageDestination[] findMessageDestinations() {
        synchronized (this.messageDestinations) {
            final MessageDestination[] results = new MessageDestination[this.messageDestinations.size()];
            return this.messageDestinations.values().toArray(results);
        }
    }
    
    public MessageDestinationRef findMessageDestinationRef(final String name) {
        return this.namingResources.findMessageDestinationRef(name);
    }
    
    public MessageDestinationRef[] findMessageDestinationRefs() {
        return this.namingResources.findMessageDestinationRefs();
    }
    
    @Override
    public String findMimeMapping(final String extension) {
        return this.mimeMappings.get(extension.toLowerCase(Locale.ENGLISH));
    }
    
    @Override
    public String[] findMimeMappings() {
        synchronized (this.mimeMappings) {
            final String[] results = new String[this.mimeMappings.size()];
            return this.mimeMappings.keySet().toArray(results);
        }
    }
    
    @Override
    public String findParameter(final String name) {
        synchronized (this.parameters) {
            return this.parameters.get(name);
        }
    }
    
    @Override
    public String[] findParameters() {
        synchronized (this.parameters) {
            final String[] results = new String[this.parameters.size()];
            return this.parameters.keySet().toArray(results);
        }
    }
    
    @Override
    public String findRoleMapping(final String role) {
        String realRole = null;
        synchronized (this.roleMappings) {
            realRole = this.roleMappings.get(role);
        }
        if (realRole != null) {
            return realRole;
        }
        return role;
    }
    
    @Override
    public boolean findSecurityRole(final String role) {
        synchronized (this.securityRolesLock) {
            for (int i = 0; i < this.securityRoles.length; ++i) {
                if (role.equals(this.securityRoles[i])) {
                    return true;
                }
            }
        }
        return false;
    }
    
    @Override
    public String[] findSecurityRoles() {
        synchronized (this.securityRolesLock) {
            return this.securityRoles;
        }
    }
    
    @Override
    public String findServletMapping(final String pattern) {
        synchronized (this.servletMappingsLock) {
            return this.servletMappings.get(pattern);
        }
    }
    
    @Override
    public String[] findServletMappings() {
        synchronized (this.servletMappingsLock) {
            final String[] results = new String[this.servletMappings.size()];
            return this.servletMappings.keySet().toArray(results);
        }
    }
    
    @Override
    public String findStatusPage(final int status) {
        final ErrorPage errorPage = this.statusPages.get(status);
        if (errorPage != null) {
            return errorPage.getLocation();
        }
        return null;
    }
    
    @Override
    public int[] findStatusPages() {
        synchronized (this.statusPages) {
            final int[] results = new int[this.statusPages.size()];
            final Iterator<Integer> elements = this.statusPages.keySet().iterator();
            int i = 0;
            while (elements.hasNext()) {
                results[i++] = elements.next();
            }
            return results;
        }
    }
    
    @Override
    public boolean findWelcomeFile(final String name) {
        synchronized (this.welcomeFilesLock) {
            for (int i = 0; i < this.welcomeFiles.length; ++i) {
                if (name.equals(this.welcomeFiles[i])) {
                    return true;
                }
            }
        }
        return false;
    }
    
    @Override
    public String[] findWatchedResources() {
        synchronized (this.watchedResourcesLock) {
            return this.watchedResources;
        }
    }
    
    @Override
    public String[] findWelcomeFiles() {
        synchronized (this.welcomeFilesLock) {
            return this.welcomeFiles;
        }
    }
    
    @Override
    public String[] findWrapperLifecycles() {
        synchronized (this.wrapperLifecyclesLock) {
            return this.wrapperLifecycles;
        }
    }
    
    @Override
    public String[] findWrapperListeners() {
        synchronized (this.wrapperListenersLock) {
            return this.wrapperListeners;
        }
    }
    
    @Override
    public synchronized void reload() {
        if (!this.getState().isAvailable()) {
            throw new IllegalStateException(StandardContext.sm.getString("standardContext.notStarted", new Object[] { this.getName() }));
        }
        if (StandardContext.log.isInfoEnabled()) {
            StandardContext.log.info((Object)StandardContext.sm.getString("standardContext.reloadingStarted", new Object[] { this.getName() }));
        }
        this.setPaused(true);
        try {
            this.stop();
        }
        catch (LifecycleException e) {
            StandardContext.log.error((Object)StandardContext.sm.getString("standardContext.stoppingContext", new Object[] { this.getName() }), (Throwable)e);
        }
        try {
            this.start();
        }
        catch (LifecycleException e) {
            StandardContext.log.error((Object)StandardContext.sm.getString("standardContext.startingContext", new Object[] { this.getName() }), (Throwable)e);
        }
        this.setPaused(false);
        if (StandardContext.log.isInfoEnabled()) {
            StandardContext.log.info((Object)StandardContext.sm.getString("standardContext.reloadingCompleted", new Object[] { this.getName() }));
        }
    }
    
    @Override
    public void removeApplicationListener(final String listener) {
        synchronized (this.applicationListenersLock) {
            int n = -1;
            for (int i = 0; i < this.applicationListeners.length; ++i) {
                if (this.applicationListeners[i].getClassName().equals(listener)) {
                    n = i;
                    break;
                }
            }
            if (n < 0) {
                return;
            }
            int j = 0;
            final ApplicationListener[] results = new ApplicationListener[this.applicationListeners.length - 1];
            for (int k = 0; k < this.applicationListeners.length; ++k) {
                if (k != n) {
                    results[j++] = this.applicationListeners[k];
                }
            }
            this.applicationListeners = results;
        }
        this.fireContainerEvent("removeApplicationListener", listener);
    }
    
    @Override
    public void removeApplicationParameter(final String name) {
        synchronized (this.applicationParametersLock) {
            int n = -1;
            for (int i = 0; i < this.applicationParameters.length; ++i) {
                if (name.equals(this.applicationParameters[i].getName())) {
                    n = i;
                    break;
                }
            }
            if (n < 0) {
                return;
            }
            int j = 0;
            final ApplicationParameter[] results = new ApplicationParameter[this.applicationParameters.length - 1];
            for (int k = 0; k < this.applicationParameters.length; ++k) {
                if (k != n) {
                    results[j++] = this.applicationParameters[k];
                }
            }
            this.applicationParameters = results;
        }
        this.fireContainerEvent("removeApplicationParameter", name);
    }
    
    @Override
    public void removeChild(final Container child) {
        if (!(child instanceof Wrapper)) {
            throw new IllegalArgumentException(StandardContext.sm.getString("standardContext.notWrapper"));
        }
        super.removeChild(child);
    }
    
    @Override
    public void removeConstraint(final SecurityConstraint constraint) {
        synchronized (this.constraintsLock) {
            int n = -1;
            for (int i = 0; i < this.constraints.length; ++i) {
                if (this.constraints[i].equals(constraint)) {
                    n = i;
                    break;
                }
            }
            if (n < 0) {
                return;
            }
            int j = 0;
            final SecurityConstraint[] results = new SecurityConstraint[this.constraints.length - 1];
            for (int k = 0; k < this.constraints.length; ++k) {
                if (k != n) {
                    results[j++] = this.constraints[k];
                }
            }
            this.constraints = results;
        }
        this.fireContainerEvent("removeConstraint", constraint);
    }
    
    @Override
    public void removeErrorPage(final ErrorPage errorPage) {
        final String exceptionType = errorPage.getExceptionType();
        if (exceptionType != null) {
            synchronized (this.exceptionPages) {
                this.exceptionPages.remove(exceptionType);
            }
        }
        else {
            synchronized (this.statusPages) {
                this.statusPages.remove(errorPage.getErrorCode());
            }
        }
        this.fireContainerEvent("removeErrorPage", errorPage);
    }
    
    @Override
    public void removeFilterDef(final FilterDef filterDef) {
        synchronized (this.filterDefs) {
            this.filterDefs.remove(filterDef.getFilterName());
        }
        this.fireContainerEvent("removeFilterDef", filterDef);
    }
    
    @Override
    public void removeFilterMap(final FilterMap filterMap) {
        this.filterMaps.remove(filterMap);
        this.fireContainerEvent("removeFilterMap", filterMap);
    }
    
    @Override
    public void removeInstanceListener(final String listener) {
        synchronized (this.instanceListenersLock) {
            int n = -1;
            for (int i = 0; i < this.instanceListeners.length; ++i) {
                if (this.instanceListeners[i].equals(listener)) {
                    n = i;
                    break;
                }
            }
            if (n < 0) {
                return;
            }
            int j = 0;
            final String[] results = new String[this.instanceListeners.length - 1];
            for (int k = 0; k < this.instanceListeners.length; ++k) {
                if (k != n) {
                    results[j++] = this.instanceListeners[k];
                }
            }
            this.instanceListeners = results;
        }
        this.fireContainerEvent("removeInstanceListener", listener);
    }
    
    public void removeMessageDestination(final String name) {
        synchronized (this.messageDestinations) {
            this.messageDestinations.remove(name);
        }
        this.fireContainerEvent("removeMessageDestination", name);
    }
    
    public void removeMessageDestinationRef(final String name) {
        this.namingResources.removeMessageDestinationRef(name);
        this.fireContainerEvent("removeMessageDestinationRef", name);
    }
    
    @Override
    public void removeMimeMapping(final String extension) {
        synchronized (this.mimeMappings) {
            this.mimeMappings.remove(extension);
        }
        this.fireContainerEvent("removeMimeMapping", extension);
    }
    
    @Override
    public void removeParameter(final String name) {
        synchronized (this.parameters) {
            this.parameters.remove(name);
        }
        this.fireContainerEvent("removeParameter", name);
    }
    
    @Override
    public void removeRoleMapping(final String role) {
        synchronized (this.roleMappings) {
            this.roleMappings.remove(role);
        }
        this.fireContainerEvent("removeRoleMapping", role);
    }
    
    @Override
    public void removeSecurityRole(final String role) {
        synchronized (this.securityRolesLock) {
            int n = -1;
            for (int i = 0; i < this.securityRoles.length; ++i) {
                if (role.equals(this.securityRoles[i])) {
                    n = i;
                    break;
                }
            }
            if (n < 0) {
                return;
            }
            int j = 0;
            final String[] results = new String[this.securityRoles.length - 1];
            for (int k = 0; k < this.securityRoles.length; ++k) {
                if (k != n) {
                    results[j++] = this.securityRoles[k];
                }
            }
            this.securityRoles = results;
        }
        this.fireContainerEvent("removeSecurityRole", role);
    }
    
    @Override
    public void removeServletMapping(final String pattern) {
        String name = null;
        synchronized (this.servletMappingsLock) {
            name = this.servletMappings.remove(pattern);
        }
        final Wrapper wrapper = (Wrapper)this.findChild(name);
        if (wrapper != null) {
            wrapper.removeMapping(pattern);
        }
        this.fireContainerEvent("removeServletMapping", pattern);
    }
    
    @Override
    public void removeWatchedResource(final String name) {
        synchronized (this.watchedResourcesLock) {
            int n = -1;
            for (int i = 0; i < this.watchedResources.length; ++i) {
                if (this.watchedResources[i].equals(name)) {
                    n = i;
                    break;
                }
            }
            if (n < 0) {
                return;
            }
            int j = 0;
            final String[] results = new String[this.watchedResources.length - 1];
            for (int k = 0; k < this.watchedResources.length; ++k) {
                if (k != n) {
                    results[j++] = this.watchedResources[k];
                }
            }
            this.watchedResources = results;
        }
        this.fireContainerEvent("removeWatchedResource", name);
    }
    
    @Override
    public void removeWelcomeFile(final String name) {
        synchronized (this.welcomeFilesLock) {
            int n = -1;
            for (int i = 0; i < this.welcomeFiles.length; ++i) {
                if (this.welcomeFiles[i].equals(name)) {
                    n = i;
                    break;
                }
            }
            if (n < 0) {
                return;
            }
            int j = 0;
            final String[] results = new String[this.welcomeFiles.length - 1];
            for (int k = 0; k < this.welcomeFiles.length; ++k) {
                if (k != n) {
                    results[j++] = this.welcomeFiles[k];
                }
            }
            this.welcomeFiles = results;
        }
        if (this.getState().equals(LifecycleState.STARTED)) {
            this.fireContainerEvent("removeWelcomeFile", name);
        }
    }
    
    @Override
    public void removeWrapperLifecycle(final String listener) {
        synchronized (this.wrapperLifecyclesLock) {
            int n = -1;
            for (int i = 0; i < this.wrapperLifecycles.length; ++i) {
                if (this.wrapperLifecycles[i].equals(listener)) {
                    n = i;
                    break;
                }
            }
            if (n < 0) {
                return;
            }
            int j = 0;
            final String[] results = new String[this.wrapperLifecycles.length - 1];
            for (int k = 0; k < this.wrapperLifecycles.length; ++k) {
                if (k != n) {
                    results[j++] = this.wrapperLifecycles[k];
                }
            }
            this.wrapperLifecycles = results;
        }
        this.fireContainerEvent("removeWrapperLifecycle", listener);
    }
    
    @Override
    public void removeWrapperListener(final String listener) {
        synchronized (this.wrapperListenersLock) {
            int n = -1;
            for (int i = 0; i < this.wrapperListeners.length; ++i) {
                if (this.wrapperListeners[i].equals(listener)) {
                    n = i;
                    break;
                }
            }
            if (n < 0) {
                return;
            }
            int j = 0;
            final String[] results = new String[this.wrapperListeners.length - 1];
            for (int k = 0; k < this.wrapperListeners.length; ++k) {
                if (k != n) {
                    results[j++] = this.wrapperListeners[k];
                }
            }
            this.wrapperListeners = results;
        }
        this.fireContainerEvent("removeWrapperListener", listener);
    }
    
    public long getProcessingTime() {
        long result = 0L;
        final Container[] children = this.findChildren();
        if (children != null) {
            for (int i = 0; i < children.length; ++i) {
                result += ((StandardWrapper)children[i]).getProcessingTime();
            }
        }
        return result;
    }
    
    public long getMaxTime() {
        long result = 0L;
        final Container[] children = this.findChildren();
        if (children != null) {
            for (int i = 0; i < children.length; ++i) {
                final long time = ((StandardWrapper)children[i]).getMaxTime();
                if (time > result) {
                    result = time;
                }
            }
        }
        return result;
    }
    
    public long getMinTime() {
        long result = -1L;
        final Container[] children = this.findChildren();
        if (children != null) {
            for (int i = 0; i < children.length; ++i) {
                final long time = ((StandardWrapper)children[i]).getMinTime();
                if (result < 0L || time < result) {
                    result = time;
                }
            }
        }
        return result;
    }
    
    public int getRequestCount() {
        int result = 0;
        final Container[] children = this.findChildren();
        if (children != null) {
            for (int i = 0; i < children.length; ++i) {
                result += ((StandardWrapper)children[i]).getRequestCount();
            }
        }
        return result;
    }
    
    public int getErrorCount() {
        int result = 0;
        final Container[] children = this.findChildren();
        if (children != null) {
            for (int i = 0; i < children.length; ++i) {
                result += ((StandardWrapper)children[i]).getErrorCount();
            }
        }
        return result;
    }
    
    @Override
    public String getRealPath(String path) {
        if ("".equals(path)) {
            path = "/";
        }
        if (this.resources != null) {
            try {
                return this.resources.getResource(path).getCanonicalPath();
            }
            catch (IllegalArgumentException ex) {}
        }
        return null;
    }
    
    public ServletRegistration.Dynamic dynamicServletAdded(final Wrapper wrapper) {
        final Servlet s = wrapper.getServlet();
        if (s != null && this.createdServlets.contains(s)) {
            wrapper.setServletSecurityAnnotationScanRequired(true);
        }
        return (ServletRegistration.Dynamic)new ApplicationServletRegistration(wrapper, this);
    }
    
    public void dynamicServletCreated(final Servlet servlet) {
        this.createdServlets.add(servlet);
    }
    
    public boolean filterStart() {
        if (this.getLogger().isDebugEnabled()) {
            this.getLogger().debug((Object)"Starting filters");
        }
        boolean ok = true;
        synchronized (this.filterConfigs) {
            this.filterConfigs.clear();
            for (final Map.Entry<String, FilterDef> entry : this.filterDefs.entrySet()) {
                final String name = entry.getKey();
                if (this.getLogger().isDebugEnabled()) {
                    this.getLogger().debug((Object)(" Starting filter '" + name + "'"));
                }
                try {
                    final ApplicationFilterConfig filterConfig = new ApplicationFilterConfig(this, entry.getValue());
                    this.filterConfigs.put(name, filterConfig);
                }
                catch (Throwable t) {
                    t = ExceptionUtils.unwrapInvocationTargetException(t);
                    ExceptionUtils.handleThrowable(t);
                    this.getLogger().error((Object)StandardContext.sm.getString("standardContext.filterStart", new Object[] { name }), t);
                    ok = false;
                }
            }
        }
        return ok;
    }
    
    public boolean filterStop() {
        if (this.getLogger().isDebugEnabled()) {
            this.getLogger().debug((Object)"Stopping filters");
        }
        synchronized (this.filterConfigs) {
            for (final String name : this.filterConfigs.keySet()) {
                if (this.getLogger().isDebugEnabled()) {
                    this.getLogger().debug((Object)(" Stopping filter '" + name + "'"));
                }
                final ApplicationFilterConfig filterConfig = this.filterConfigs.get(name);
                filterConfig.release();
            }
            this.filterConfigs.clear();
        }
        return true;
    }
    
    public FilterConfig findFilterConfig(final String name) {
        return (FilterConfig)this.filterConfigs.get(name);
    }
    
    public boolean listenerStart() {
        if (StandardContext.log.isDebugEnabled()) {
            StandardContext.log.debug((Object)"Configuring application event listeners");
        }
        final ApplicationListener[] listeners = this.findApplicationListeners();
        final Object[] results = new Object[listeners.length];
        boolean ok = true;
        final Set<Object> noPluggabilityListeners = new HashSet<Object>();
        for (int i = 0; i < results.length; ++i) {
            if (this.getLogger().isDebugEnabled()) {
                this.getLogger().debug((Object)(" Configuring event listener class '" + listeners[i] + "'"));
            }
            try {
                final ApplicationListener listener = listeners[i];
                results[i] = this.instanceManager.newInstance(listener.getClassName());
                if (listener.isPluggabilityBlocked()) {
                    noPluggabilityListeners.add(results[i]);
                }
            }
            catch (Throwable t) {
                t = ExceptionUtils.unwrapInvocationTargetException(t);
                ExceptionUtils.handleThrowable(t);
                this.getLogger().error((Object)StandardContext.sm.getString("standardContext.applicationListener", new Object[] { listeners[i].getClassName() }), t);
                ok = false;
            }
        }
        if (!ok) {
            this.getLogger().error((Object)StandardContext.sm.getString("standardContext.applicationSkipped"));
            return false;
        }
        final ArrayList<Object> eventListeners = new ArrayList<Object>();
        final ArrayList<Object> lifecycleListeners = new ArrayList<Object>();
        for (int j = 0; j < results.length; ++j) {
            if (results[j] instanceof ServletContextAttributeListener || results[j] instanceof ServletRequestAttributeListener || results[j] instanceof ServletRequestListener || results[j] instanceof HttpSessionIdListener || results[j] instanceof HttpSessionAttributeListener) {
                eventListeners.add(results[j]);
            }
            if (results[j] instanceof ServletContextListener || results[j] instanceof HttpSessionListener) {
                lifecycleListeners.add(results[j]);
            }
        }
        for (final Object eventListener : this.getApplicationEventListeners()) {
            eventListeners.add(eventListener);
        }
        this.setApplicationEventListeners(eventListeners.toArray());
        for (final Object lifecycleListener : this.getApplicationLifecycleListeners()) {
            lifecycleListeners.add(lifecycleListener);
            if (lifecycleListener instanceof ServletContextListener) {
                noPluggabilityListeners.add(lifecycleListener);
            }
        }
        this.setApplicationLifecycleListeners(lifecycleListeners.toArray());
        if (this.getLogger().isDebugEnabled()) {
            this.getLogger().debug((Object)"Sending application start events");
        }
        this.getServletContext();
        this.context.setNewServletContextListenerAllowed(false);
        final Object[] instances = this.getApplicationLifecycleListeners();
        if (instances == null || instances.length == 0) {
            return ok;
        }
        final ServletContextEvent event = new ServletContextEvent(this.getServletContext());
        ServletContextEvent tldEvent = null;
        if (noPluggabilityListeners.size() > 0) {
            tldEvent = new ServletContextEvent((ServletContext)new NoPluggabilityServletContext(this.getServletContext()));
        }
        for (int k = 0; k < instances.length; ++k) {
            if (instances[k] != null) {
                if (instances[k] instanceof ServletContextListener) {
                    final ServletContextListener listener2 = (ServletContextListener)instances[k];
                    try {
                        this.fireContainerEvent("beforeContextInitialized", listener2);
                        if (noPluggabilityListeners.contains(listener2)) {
                            listener2.contextInitialized(tldEvent);
                        }
                        else {
                            listener2.contextInitialized(event);
                        }
                        this.fireContainerEvent("afterContextInitialized", listener2);
                    }
                    catch (Throwable t2) {
                        ExceptionUtils.handleThrowable(t2);
                        this.fireContainerEvent("afterContextInitialized", listener2);
                        this.getLogger().error((Object)StandardContext.sm.getString("standardContext.listenerStart", new Object[] { instances[k].getClass().getName() }), t2);
                        ok = false;
                    }
                }
            }
        }
        return ok;
    }
    
    public boolean listenerStop() {
        if (StandardContext.log.isDebugEnabled()) {
            StandardContext.log.debug((Object)"Sending application stop events");
        }
        boolean ok = true;
        Object[] listeners = this.getApplicationLifecycleListeners();
        if (listeners != null && listeners.length > 0) {
            final ServletContextEvent event = new ServletContextEvent(this.getServletContext());
            for (int i = 0; i < listeners.length; ++i) {
                final int j = listeners.length - 1 - i;
                if (listeners[j] != null) {
                    if (listeners[j] instanceof ServletContextListener) {
                        final ServletContextListener listener = (ServletContextListener)listeners[j];
                        try {
                            this.fireContainerEvent("beforeContextDestroyed", listener);
                            listener.contextDestroyed(event);
                            this.fireContainerEvent("afterContextDestroyed", listener);
                        }
                        catch (Throwable t) {
                            ExceptionUtils.handleThrowable(t);
                            this.fireContainerEvent("afterContextDestroyed", listener);
                            this.getLogger().error((Object)StandardContext.sm.getString("standardContext.listenerStop", new Object[] { listeners[j].getClass().getName() }), t);
                            ok = false;
                        }
                    }
                    try {
                        this.getInstanceManager().destroyInstance(listeners[j]);
                    }
                    catch (Throwable t2) {
                        t2 = ExceptionUtils.unwrapInvocationTargetException(t2);
                        ExceptionUtils.handleThrowable(t2);
                        this.getLogger().error((Object)StandardContext.sm.getString("standardContext.listenerStop", new Object[] { listeners[j].getClass().getName() }), t2);
                        ok = false;
                    }
                }
            }
        }
        listeners = this.getApplicationEventListeners();
        if (listeners != null) {
            for (int k = 0; k < listeners.length; ++k) {
                final int l = listeners.length - 1 - k;
                if (listeners[l] != null) {
                    try {
                        this.getInstanceManager().destroyInstance(listeners[l]);
                    }
                    catch (Throwable t3) {
                        t3 = ExceptionUtils.unwrapInvocationTargetException(t3);
                        ExceptionUtils.handleThrowable(t3);
                        this.getLogger().error((Object)StandardContext.sm.getString("standardContext.listenerStop", new Object[] { listeners[l].getClass().getName() }), t3);
                        ok = false;
                    }
                }
            }
        }
        this.setApplicationEventListeners(null);
        this.setApplicationLifecycleListeners(null);
        return ok;
    }
    
    public void resourcesStart() throws LifecycleException {
        if (!this.resources.getState().isAvailable()) {
            this.resources.start();
        }
        if (this.effectiveMajorVersion >= 3 && this.addWebinfClassesResources) {
            final WebResource webinfClassesResource = this.resources.getResource("/WEB-INF/classes/META-INF/resources");
            if (webinfClassesResource.isDirectory()) {
                this.getResources().createWebResourceSet(WebResourceRoot.ResourceSetType.RESOURCE_JAR, "/", webinfClassesResource.getURL(), "/");
            }
        }
    }
    
    public boolean resourcesStop() {
        boolean ok = true;
        final Lock writeLock = this.resourcesLock.writeLock();
        writeLock.lock();
        try {
            if (this.resources != null) {
                this.resources.stop();
            }
        }
        catch (Throwable t) {
            ExceptionUtils.handleThrowable(t);
            StandardContext.log.error((Object)StandardContext.sm.getString("standardContext.resourcesStop"), t);
            ok = false;
        }
        finally {
            writeLock.unlock();
        }
        return ok;
    }
    
    public void loadOnStartup(final Container[] children) {
        final TreeMap<Integer, ArrayList<Wrapper>> map = new TreeMap<Integer, ArrayList<Wrapper>>();
        for (int i = 0; i < children.length; ++i) {
            final Wrapper wrapper = (Wrapper)children[i];
            final int loadOnStartup = wrapper.getLoadOnStartup();
            if (loadOnStartup >= 0) {
                final Integer key = loadOnStartup;
                ArrayList<Wrapper> list = map.get(key);
                if (list == null) {
                    list = new ArrayList<Wrapper>();
                    map.put(key, list);
                }
                list.add(wrapper);
            }
        }
        for (final ArrayList<Wrapper> list2 : map.values()) {
            for (final Wrapper wrapper2 : list2) {
                try {
                    wrapper2.load();
                }
                catch (ServletException e) {
                    this.getLogger().error((Object)StandardContext.sm.getString("standardWrapper.loadException", new Object[] { this.getName() }), StandardWrapper.getRootCause(e));
                }
            }
        }
    }
    
    @Override
    protected synchronized void startInternal() throws LifecycleException {
        if (StandardContext.log.isDebugEnabled()) {
            StandardContext.log.debug((Object)("Starting " + this.getBaseName()));
        }
        if (this.getObjectName() != null) {
            final Notification notification = new Notification("j2ee.state.starting", this.getObjectName(), this.sequenceNumber.getAndIncrement());
            this.broadcaster.sendNotification(notification);
        }
        this.setConfigured(false);
        boolean ok = true;
        if (this.namingResources != null) {
            this.namingResources.start();
        }
        if (this.getResources() == null) {
            if (StandardContext.log.isDebugEnabled()) {
                StandardContext.log.debug((Object)"Configuring default Resources");
            }
            try {
                this.setResources(new StandardRoot(this));
            }
            catch (IllegalArgumentException e) {
                StandardContext.log.error((Object)("Error initializing resources: " + e.getMessage()));
                ok = false;
            }
        }
        if (ok) {
            this.resourcesStart();
        }
        if (this.getLoader() == null) {
            final WebappLoader webappLoader = new WebappLoader(this.getParentClassLoader());
            webappLoader.setDelegate(this.getDelegate());
            this.setLoader(webappLoader);
        }
        this.getCharsetMapper();
        this.postWorkDirectory();
        boolean dependencyCheck = true;
        try {
            dependencyCheck = ExtensionValidator.validateApplication(this.getResources(), this);
        }
        catch (IOException ioe) {
            StandardContext.log.error((Object)"Error in dependencyCheck", (Throwable)ioe);
            dependencyCheck = false;
        }
        if (!dependencyCheck) {
            ok = false;
        }
        final String useNamingProperty = System.getProperty("catalina.useNaming");
        if (useNamingProperty != null && useNamingProperty.equals("false")) {
            this.useNaming = false;
        }
        if (ok && this.isUseNaming() && this.getNamingContextListener() == null) {
            final NamingContextListener ncl = new NamingContextListener();
            ncl.setName(this.getNamingContextName());
            ncl.setExceptionOnFailedWrite(this.getJndiExceptionOnFailedWrite());
            this.addLifecycleListener(ncl);
            this.setNamingContextListener(ncl);
        }
        if (StandardContext.log.isDebugEnabled()) {
            StandardContext.log.debug((Object)"Processing standard container startup");
        }
        ClassLoader oldCCL = this.bindThread();
        try {
            if (ok) {
                final Loader loader = this.getLoader();
                if (loader != null && loader instanceof Lifecycle) {
                    ((Lifecycle)loader).start();
                }
                this.setClassLoaderProperty("clearReferencesStatic", this.getClearReferencesStatic());
                this.setClassLoaderProperty("clearReferencesStopThreads", this.getClearReferencesStopThreads());
                this.setClassLoaderProperty("clearReferencesStopTimerThreads", this.getClearReferencesStopTimerThreads());
                this.setClassLoaderProperty("clearReferencesHttpClientKeepAliveThread", this.getClearReferencesHttpClientKeepAliveThread());
                this.unbindThread(oldCCL);
                oldCCL = this.bindThread();
                this.logger = null;
                this.getLogger();
                final Cluster cluster = this.getClusterInternal();
                if (cluster != null && cluster instanceof Lifecycle) {
                    ((Lifecycle)cluster).start();
                }
                final Realm realm = this.getRealmInternal();
                if (realm != null && realm instanceof Lifecycle) {
                    ((Lifecycle)realm).start();
                }
                this.fireLifecycleEvent("configure_start", null);
                for (final Container child : this.findChildren()) {
                    if (!child.getState().isAvailable()) {
                        child.start();
                    }
                }
                if (this.pipeline instanceof Lifecycle) {
                    ((Lifecycle)this.pipeline).start();
                }
                Manager contextManager = null;
                final Manager manager = this.getManager();
                if (manager == null) {
                    if (StandardContext.log.isDebugEnabled()) {
                        StandardContext.log.debug((Object)StandardContext.sm.getString("standardContext.cluster.noManager", new Object[] { this.getCluster() != null, this.distributable }));
                    }
                    if (this.getCluster() != null && this.distributable) {
                        try {
                            contextManager = this.getCluster().createManager(this.getName());
                        }
                        catch (Exception ex) {
                            StandardContext.log.error((Object)"standardContext.clusterFail", (Throwable)ex);
                            ok = false;
                        }
                    }
                    else {
                        contextManager = new StandardManager();
                    }
                }
                if (contextManager != null) {
                    if (StandardContext.log.isDebugEnabled()) {
                        StandardContext.log.debug((Object)StandardContext.sm.getString("standardContext.manager", new Object[] { contextManager.getClass().getName() }));
                    }
                    this.setManager(contextManager);
                }
                if (manager != null && this.getCluster() != null && this.distributable) {
                    this.getCluster().registerManager(manager);
                }
            }
            if (!this.getConfigured()) {
                StandardContext.log.error((Object)"Error getConfigured");
                ok = false;
            }
            if (ok) {
                this.getServletContext().setAttribute("org.apache.catalina.resources", (Object)this.getResources());
            }
            if (ok && this.getInstanceManager() == null) {
                javax.naming.Context context = null;
                if (this.isUseNaming() && this.getNamingContextListener() != null) {
                    context = this.getNamingContextListener().getEnvContext();
                }
                final Map<String, Map<String, String>> injectionMap = this.buildInjectionMap(this.getIgnoreAnnotations() ? new NamingResourcesImpl() : this.getNamingResources());
                this.setInstanceManager((InstanceManager)new DefaultInstanceManager(context, injectionMap, this, this.getClass().getClassLoader()));
                this.getServletContext().setAttribute(InstanceManager.class.getName(), (Object)this.getInstanceManager());
            }
            if (ok) {
                this.getServletContext().setAttribute(JarScanner.class.getName(), (Object)this.getJarScanner());
            }
            this.mergeParameters();
            for (final Map.Entry<ServletContainerInitializer, Set<Class<?>>> entry : this.initializers.entrySet()) {
                try {
                    entry.getKey().onStartup((Set)entry.getValue(), this.getServletContext());
                }
                catch (ServletException e2) {
                    StandardContext.log.error((Object)StandardContext.sm.getString("standardContext.sciFail"), (Throwable)e2);
                    ok = false;
                    break;
                }
            }
            if (ok && !this.listenerStart()) {
                StandardContext.log.error((Object)"Error listenerStart");
                ok = false;
            }
            if (ok) {
                this.checkConstraintsForUncoveredMethods(this.findConstraints());
            }
            try {
                final Manager manager2 = this.getManager();
                if (manager2 != null && manager2 instanceof Lifecycle) {
                    ((Lifecycle)this.getManager()).start();
                }
            }
            catch (Exception e3) {
                StandardContext.log.error((Object)"Error manager.start()", (Throwable)e3);
                ok = false;
            }
            if (ok && !this.filterStart()) {
                StandardContext.log.error((Object)"Error filterStart");
                ok = false;
            }
            if (ok) {
                this.loadOnStartup(this.findChildren());
            }
            super.threadStart();
        }
        finally {
            this.unbindThread(oldCCL);
        }
        if (ok) {
            if (StandardContext.log.isDebugEnabled()) {
                StandardContext.log.debug((Object)"Starting completed");
            }
        }
        else {
            StandardContext.log.error((Object)StandardContext.sm.getString("standardContext.startFailed", new Object[] { this.getName() }));
        }
        this.startTime = System.currentTimeMillis();
        if (ok && this.getObjectName() != null) {
            final Notification notification2 = new Notification("j2ee.state.running", this.getObjectName(), this.sequenceNumber.getAndIncrement());
            this.broadcaster.sendNotification(notification2);
        }
        if (!ok) {
            this.setState(LifecycleState.FAILED);
        }
        else {
            this.setState(LifecycleState.STARTING);
        }
    }
    
    private void checkConstraintsForUncoveredMethods(final SecurityConstraint[] constraints) {
        final SecurityConstraint[] arr$;
        final SecurityConstraint[] newConstraints = arr$ = SecurityConstraint.findUncoveredHttpMethods(constraints, this.getDenyUncoveredHttpMethods(), this.getLogger());
        for (final SecurityConstraint constraint : arr$) {
            this.addConstraint(constraint);
        }
    }
    
    private void setClassLoaderProperty(final String name, final boolean value) {
        final ClassLoader cl = this.getLoader().getClassLoader();
        if (!IntrospectionUtils.setProperty((Object)cl, name, Boolean.toString(value))) {
            StandardContext.log.info((Object)StandardContext.sm.getString("standardContext.webappClassLoader.missingProperty", new Object[] { name, Boolean.toString(value) }));
        }
    }
    
    private Map<String, Map<String, String>> buildInjectionMap(final NamingResourcesImpl namingResources) {
        final Map<String, Map<String, String>> injectionMap = new HashMap<String, Map<String, String>>();
        for (final Injectable resource : namingResources.findLocalEjbs()) {
            this.addInjectionTarget(resource, injectionMap);
        }
        for (final Injectable resource : namingResources.findEjbs()) {
            this.addInjectionTarget(resource, injectionMap);
        }
        for (final Injectable resource : namingResources.findEnvironments()) {
            this.addInjectionTarget(resource, injectionMap);
        }
        for (final Injectable resource : namingResources.findMessageDestinationRefs()) {
            this.addInjectionTarget(resource, injectionMap);
        }
        for (final Injectable resource : namingResources.findResourceEnvRefs()) {
            this.addInjectionTarget(resource, injectionMap);
        }
        for (final Injectable resource : namingResources.findResources()) {
            this.addInjectionTarget(resource, injectionMap);
        }
        for (final Injectable resource : namingResources.findServices()) {
            this.addInjectionTarget(resource, injectionMap);
        }
        return injectionMap;
    }
    
    private void addInjectionTarget(final Injectable resource, final Map<String, Map<String, String>> injectionMap) {
        final List<InjectionTarget> injectionTargets = (List<InjectionTarget>)resource.getInjectionTargets();
        if (injectionTargets != null && injectionTargets.size() > 0) {
            final String jndiName = resource.getName();
            for (final InjectionTarget injectionTarget : injectionTargets) {
                final String clazz = injectionTarget.getTargetClass();
                Map<String, String> injections = injectionMap.get(clazz);
                if (injections == null) {
                    injections = new HashMap<String, String>();
                    injectionMap.put(clazz, injections);
                }
                injections.put(injectionTarget.getTargetName(), jndiName);
            }
        }
    }
    
    private void mergeParameters() {
        final Map<String, String> mergedParams = new HashMap<String, String>();
        final String[] names = this.findParameters();
        for (int i = 0; i < names.length; ++i) {
            mergedParams.put(names[i], this.findParameter(names[i]));
        }
        final ApplicationParameter[] params = this.findApplicationParameters();
        for (int j = 0; j < params.length; ++j) {
            if (params[j].getOverride()) {
                if (mergedParams.get(params[j].getName()) == null) {
                    mergedParams.put(params[j].getName(), params[j].getValue());
                }
            }
            else {
                mergedParams.put(params[j].getName(), params[j].getValue());
            }
        }
        final ServletContext sc = this.getServletContext();
        for (final Map.Entry<String, String> entry : mergedParams.entrySet()) {
            sc.setInitParameter((String)entry.getKey(), (String)entry.getValue());
        }
    }
    
    @Override
    protected synchronized void stopInternal() throws LifecycleException {
        if (this.getObjectName() != null) {
            final Notification notification = new Notification("j2ee.state.stopping", this.getObjectName(), this.sequenceNumber.getAndIncrement());
            this.broadcaster.sendNotification(notification);
        }
        this.setState(LifecycleState.STOPPING);
        final ClassLoader oldCCL = this.bindThread();
        try {
            final Container[] children = this.findChildren();
            this.threadStop();
            for (int i = 0; i < children.length; ++i) {
                children[i].stop();
            }
            this.filterStop();
            final Manager manager = this.getManager();
            if (manager != null && manager instanceof Lifecycle && ((Lifecycle)manager).getState().isAvailable()) {
                ((Lifecycle)manager).stop();
            }
            this.listenerStop();
            this.setCharsetMapper(null);
            if (StandardContext.log.isDebugEnabled()) {
                StandardContext.log.debug((Object)"Processing standard container shutdown");
            }
            if (this.namingResources != null) {
                this.namingResources.stop();
            }
            this.fireLifecycleEvent("configure_stop", null);
            if (this.pipeline instanceof Lifecycle && ((Lifecycle)this.pipeline).getState().isAvailable()) {
                ((Lifecycle)this.pipeline).stop();
            }
            if (this.context != null) {
                this.context.clearAttributes();
            }
            final Realm realm = this.getRealmInternal();
            if (realm != null && realm instanceof Lifecycle) {
                ((Lifecycle)realm).stop();
            }
            final Cluster cluster = this.getClusterInternal();
            if (cluster != null && cluster instanceof Lifecycle) {
                ((Lifecycle)cluster).stop();
            }
            final Loader loader = this.getLoader();
            if (loader != null && loader instanceof Lifecycle) {
                ((Lifecycle)loader).stop();
            }
            this.resourcesStop();
        }
        finally {
            this.unbindThread(oldCCL);
        }
        if (this.getObjectName() != null) {
            final Notification notification2 = new Notification("j2ee.state.stopped", this.getObjectName(), this.sequenceNumber.getAndIncrement());
            this.broadcaster.sendNotification(notification2);
        }
        this.context = null;
        try {
            this.resetContext();
        }
        catch (Exception ex) {
            StandardContext.log.error((Object)("Error reseting context " + this + " " + ex), (Throwable)ex);
        }
        this.instanceManager = null;
        if (StandardContext.log.isDebugEnabled()) {
            StandardContext.log.debug((Object)"Stopping complete");
        }
    }
    
    @Override
    protected void destroyInternal() throws LifecycleException {
        if (this.getObjectName() != null) {
            final Notification notification = new Notification("j2ee.object.deleted", this.getObjectName(), this.sequenceNumber.getAndIncrement());
            this.broadcaster.sendNotification(notification);
        }
        if (this.namingResources != null) {
            this.namingResources.destroy();
        }
        synchronized (this.instanceListenersLock) {
            this.instanceListeners = new String[0];
        }
        final Loader loader = this.getLoader();
        if (loader != null && loader instanceof Lifecycle) {
            ((Lifecycle)loader).destroy();
        }
        final Manager manager = this.getManager();
        if (manager != null && manager instanceof Lifecycle) {
            ((Lifecycle)manager).destroy();
        }
        if (this.resources != null) {
            this.resources.destroy();
        }
        super.destroyInternal();
    }
    
    @Override
    public void backgroundProcess() {
        if (!this.getState().isAvailable()) {
            return;
        }
        final Loader loader = this.getLoader();
        if (loader != null) {
            try {
                loader.backgroundProcess();
            }
            catch (Exception e) {
                StandardContext.log.warn((Object)StandardContext.sm.getString("standardContext.backgroundProcess.loader", new Object[] { loader }), (Throwable)e);
            }
        }
        final Manager manager = this.getManager();
        if (manager != null) {
            try {
                manager.backgroundProcess();
            }
            catch (Exception e2) {
                StandardContext.log.warn((Object)StandardContext.sm.getString("standardContext.backgroundProcess.manager", new Object[] { manager }), (Throwable)e2);
            }
        }
        final WebResourceRoot resources = this.getResources();
        if (resources != null) {
            try {
                resources.backgroundProcess();
            }
            catch (Exception e3) {
                StandardContext.log.warn((Object)StandardContext.sm.getString("standardContext.backgroundProcess.resources", new Object[] { resources }), (Throwable)e3);
            }
        }
        super.backgroundProcess();
    }
    
    private void resetContext() throws Exception {
        for (final Container child : this.findChildren()) {
            this.removeChild(child);
        }
        this.startupTime = 0L;
        this.startTime = 0L;
        this.tldScanTime = 0L;
        this.distributable = false;
        this.applicationListeners = new ApplicationListener[0];
        this.applicationEventListenersObjects = new Object[0];
        this.applicationLifecycleListenersObjects = new Object[0];
        this.jspConfigDescriptor = null;
        this.initializers.clear();
        this.createdServlets.clear();
        this.postConstructMethods.clear();
        this.preDestroyMethods.clear();
        if (StandardContext.log.isDebugEnabled()) {
            StandardContext.log.debug((Object)("resetContext " + this.getObjectName()));
        }
    }
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        if (this.getParent() != null) {
            sb.append(this.getParent().toString());
            sb.append(".");
        }
        sb.append("StandardContext[");
        sb.append(this.getName());
        sb.append("]");
        return sb.toString();
    }
    
    protected String adjustURLPattern(final String urlPattern) {
        if (urlPattern == null) {
            return urlPattern;
        }
        if (urlPattern.startsWith("/") || urlPattern.startsWith("*.")) {
            return urlPattern;
        }
        if (!this.isServlet22()) {
            return urlPattern;
        }
        if (StandardContext.log.isDebugEnabled()) {
            StandardContext.log.debug((Object)StandardContext.sm.getString("standardContext.urlPattern.patternWarning", new Object[] { urlPattern }));
        }
        return "/" + urlPattern;
    }
    
    @Override
    public boolean isServlet22() {
        return "-//Sun Microsystems, Inc.//DTD Web Application 2.2//EN".equals(this.publicId);
    }
    
    @Override
    public Set<String> addServletSecurity(final ServletRegistration.Dynamic registration, final ServletSecurityElement servletSecurityElement) {
        final Set<String> conflicts = new HashSet<String>();
        final Collection<String> urlPatterns = (Collection<String>)registration.getMappings();
        for (final String urlPattern : urlPatterns) {
            boolean foundConflict = false;
            final SecurityConstraint[] arr$;
            final SecurityConstraint[] securityConstraints = arr$ = this.findConstraints();
            for (final SecurityConstraint securityConstraint : arr$) {
                final SecurityCollection[] arr$2;
                final SecurityCollection[] collections = arr$2 = securityConstraint.findCollections();
                for (final SecurityCollection collection : arr$2) {
                    if (collection.findPattern(urlPattern)) {
                        if (collection.isFromDescriptor()) {
                            foundConflict = true;
                            conflicts.add(urlPattern);
                            break;
                        }
                        collection.removePattern(urlPattern);
                        if (collection.findPatterns().length == 0) {
                            securityConstraint.removeCollection(collection);
                        }
                    }
                }
                if (securityConstraint.findCollections().length == 0) {
                    this.removeConstraint(securityConstraint);
                }
                if (foundConflict) {
                    break;
                }
            }
            if (!foundConflict) {
                final SecurityConstraint[] arr$3;
                final SecurityConstraint[] newSecurityConstraints = arr$3 = SecurityConstraint.createConstraints(servletSecurityElement, urlPattern);
                for (final SecurityConstraint securityConstraint2 : arr$3) {
                    this.addConstraint(securityConstraint2);
                }
                this.checkConstraintsForUncoveredMethods(newSecurityConstraints);
            }
        }
        return conflicts;
    }
    
    protected ClassLoader bindThread() {
        final ClassLoader oldContextClassLoader = this.bind(false, null);
        if (this.isUseNaming()) {
            try {
                ContextBindings.bindThread(this, this);
            }
            catch (NamingException ex) {}
        }
        return oldContextClassLoader;
    }
    
    protected void unbindThread(final ClassLoader oldContextClassLoader) {
        if (this.isUseNaming()) {
            ContextBindings.unbindThread(this, this);
        }
        this.unbind(false, oldContextClassLoader);
    }
    
    @Override
    public ClassLoader bind(final boolean usePrivilegedAction, ClassLoader originalClassLoader) {
        final Loader loader = this.getLoader();
        ClassLoader webApplicationClassLoader = null;
        if (loader != null) {
            webApplicationClassLoader = loader.getClassLoader();
        }
        if (originalClassLoader == null) {
            if (usePrivilegedAction) {
                final PrivilegedAction<ClassLoader> pa = new PrivilegedGetTccl();
                originalClassLoader = AccessController.doPrivileged(pa);
            }
            else {
                originalClassLoader = Thread.currentThread().getContextClassLoader();
            }
        }
        if (webApplicationClassLoader == null || webApplicationClassLoader == originalClassLoader) {
            return null;
        }
        final ThreadBindingListener threadBindingListener = this.getThreadBindingListener();
        if (usePrivilegedAction) {
            final PrivilegedAction<Void> pa2 = new PrivilegedSetTccl(webApplicationClassLoader);
            AccessController.doPrivileged(pa2);
        }
        else {
            Thread.currentThread().setContextClassLoader(webApplicationClassLoader);
        }
        if (threadBindingListener != null) {
            try {
                threadBindingListener.bind();
            }
            catch (Throwable t) {
                ExceptionUtils.handleThrowable(t);
                StandardContext.log.error((Object)StandardContext.sm.getString("standardContext.threadBindingListenerError", new Object[] { this.getName() }), t);
            }
        }
        return originalClassLoader;
    }
    
    @Override
    public void unbind(final boolean usePrivilegedAction, final ClassLoader originalClassLoader) {
        if (originalClassLoader == null) {
            return;
        }
        if (this.threadBindingListener != null) {
            try {
                this.threadBindingListener.unbind();
            }
            catch (Throwable t) {
                ExceptionUtils.handleThrowable(t);
                StandardContext.log.error((Object)StandardContext.sm.getString("standardContext.threadBindingListenerError", new Object[] { this.getName() }), t);
            }
        }
        if (usePrivilegedAction) {
            final PrivilegedAction<Void> pa = new PrivilegedSetTccl(originalClassLoader);
            AccessController.doPrivileged(pa);
        }
        else {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }
    
    private String getNamingContextName() {
        if (this.namingContextName == null) {
            Container parent = this.getParent();
            if (parent == null) {
                this.namingContextName = this.getName();
            }
            else {
                final Stack<String> stk = new Stack<String>();
                final StringBuilder buff = new StringBuilder();
                while (parent != null) {
                    stk.push(parent.getName());
                    parent = parent.getParent();
                }
                while (!stk.empty()) {
                    buff.append("/" + stk.pop());
                }
                buff.append(this.getName());
                this.namingContextName = buff.toString();
            }
        }
        return this.namingContextName;
    }
    
    public NamingContextListener getNamingContextListener() {
        return this.namingContextListener;
    }
    
    public void setNamingContextListener(final NamingContextListener namingContextListener) {
        this.namingContextListener = namingContextListener;
    }
    
    @Override
    public boolean getPaused() {
        return this.paused;
    }
    
    @Override
    public boolean fireRequestInitEvent(final ServletRequest request) {
        final Object[] instances = this.getApplicationEventListeners();
        if (instances != null && instances.length > 0) {
            final ServletRequestEvent event = new ServletRequestEvent(this.getServletContext(), request);
            for (int i = 0; i < instances.length; ++i) {
                if (instances[i] != null) {
                    if (instances[i] instanceof ServletRequestListener) {
                        final ServletRequestListener listener = (ServletRequestListener)instances[i];
                        try {
                            listener.requestInitialized(event);
                        }
                        catch (Throwable t) {
                            ExceptionUtils.handleThrowable(t);
                            this.getLogger().error((Object)StandardContext.sm.getString("standardContext.requestListener.requestInit", new Object[] { instances[i].getClass().getName() }), t);
                            request.setAttribute("javax.servlet.error.exception", (Object)t);
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }
    
    @Override
    public boolean fireRequestDestroyEvent(final ServletRequest request) {
        final Object[] instances = this.getApplicationEventListeners();
        if (instances != null && instances.length > 0) {
            final ServletRequestEvent event = new ServletRequestEvent(this.getServletContext(), request);
            for (int i = 0; i < instances.length; ++i) {
                final int j = instances.length - 1 - i;
                if (instances[j] != null) {
                    if (instances[j] instanceof ServletRequestListener) {
                        final ServletRequestListener listener = (ServletRequestListener)instances[j];
                        try {
                            listener.requestDestroyed(event);
                        }
                        catch (Throwable t) {
                            ExceptionUtils.handleThrowable(t);
                            this.getLogger().error((Object)StandardContext.sm.getString("standardContext.requestListener.requestInit", new Object[] { instances[j].getClass().getName() }), t);
                            request.setAttribute("javax.servlet.error.exception", (Object)t);
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }
    
    @Override
    public void addPostConstructMethod(final String clazz, final String method) {
        if (clazz == null || method == null) {
            throw new IllegalArgumentException(StandardContext.sm.getString("standardContext.postconstruct.required"));
        }
        if (this.postConstructMethods.get(clazz) != null) {
            throw new IllegalArgumentException(StandardContext.sm.getString("standardContext.postconstruct.duplicate", new Object[] { clazz }));
        }
        this.postConstructMethods.put(clazz, method);
        this.fireContainerEvent("addPostConstructMethod", clazz);
    }
    
    @Override
    public void removePostConstructMethod(final String clazz) {
        this.postConstructMethods.remove(clazz);
        this.fireContainerEvent("removePostConstructMethod", clazz);
    }
    
    @Override
    public void addPreDestroyMethod(final String clazz, final String method) {
        if (clazz == null || method == null) {
            throw new IllegalArgumentException(StandardContext.sm.getString("standardContext.predestroy.required"));
        }
        if (this.preDestroyMethods.get(clazz) != null) {
            throw new IllegalArgumentException(StandardContext.sm.getString("standardContext.predestroy.duplicate", new Object[] { clazz }));
        }
        this.preDestroyMethods.put(clazz, method);
        this.fireContainerEvent("addPreDestroyMethod", clazz);
    }
    
    @Override
    public void removePreDestroyMethod(final String clazz) {
        this.preDestroyMethods.remove(clazz);
        this.fireContainerEvent("removePreDestroyMethod", clazz);
    }
    
    @Override
    public String findPostConstructMethod(final String clazz) {
        return this.postConstructMethods.get(clazz);
    }
    
    @Override
    public String findPreDestroyMethod(final String clazz) {
        return this.preDestroyMethods.get(clazz);
    }
    
    @Override
    public Map<String, String> findPostConstructMethods() {
        return this.postConstructMethods;
    }
    
    @Override
    public Map<String, String> findPreDestroyMethods() {
        return this.preDestroyMethods;
    }
    
    private void postWorkDirectory() {
        String workDir = this.getWorkDir();
        if (workDir == null || workDir.length() == 0) {
            String hostName = null;
            String engineName = null;
            String hostWorkDir = null;
            final Container parentHost = this.getParent();
            if (parentHost != null) {
                hostName = parentHost.getName();
                if (parentHost instanceof StandardHost) {
                    hostWorkDir = ((StandardHost)parentHost).getWorkDir();
                }
                final Container parentEngine = parentHost.getParent();
                if (parentEngine != null) {
                    engineName = parentEngine.getName();
                }
            }
            if (hostName == null || hostName.length() < 1) {
                hostName = "_";
            }
            if (engineName == null || engineName.length() < 1) {
                engineName = "_";
            }
            String temp = this.getBaseName();
            if (temp.startsWith("/")) {
                temp = temp.substring(1);
            }
            temp = temp.replace('/', '_');
            temp = temp.replace('\\', '_');
            if (temp.length() < 1) {
                temp = "ROOT";
            }
            if (hostWorkDir != null) {
                workDir = hostWorkDir + File.separator + temp;
            }
            else {
                workDir = "work" + File.separator + engineName + File.separator + hostName + File.separator + temp;
            }
            this.setWorkDir(workDir);
        }
        File dir = new File(workDir);
        if (!dir.isAbsolute()) {
            String catalinaHomePath = null;
            try {
                catalinaHomePath = this.getCatalinaBase().getCanonicalPath();
                dir = new File(catalinaHomePath, workDir);
            }
            catch (IOException e) {
                StandardContext.log.warn((Object)StandardContext.sm.getString("standardContext.workCreateException", new Object[] { workDir, catalinaHomePath, this.getName() }), (Throwable)e);
            }
        }
        if (!dir.mkdirs() && !dir.isDirectory()) {
            StandardContext.log.warn((Object)StandardContext.sm.getString("standardContext.workCreateFail", new Object[] { dir, this.getName() }));
        }
        if (this.context == null) {
            this.getServletContext();
        }
        this.context.setAttribute("javax.servlet.context.tempdir", dir);
        this.context.setAttributeReadOnly("javax.servlet.context.tempdir");
    }
    
    private void setPaused(final boolean paused) {
        this.paused = paused;
    }
    
    private boolean validateURLPattern(final String urlPattern) {
        if (urlPattern == null) {
            return false;
        }
        if (urlPattern.indexOf(10) >= 0 || urlPattern.indexOf(13) >= 0) {
            return false;
        }
        if (urlPattern.equals("")) {
            return true;
        }
        if (urlPattern.startsWith("*.")) {
            if (urlPattern.indexOf(47) < 0) {
                this.checkUnusualURLPattern(urlPattern);
                return true;
            }
            return false;
        }
        else {
            if (urlPattern.startsWith("/") && urlPattern.indexOf("*.") < 0) {
                this.checkUnusualURLPattern(urlPattern);
                return true;
            }
            return false;
        }
    }
    
    private void checkUnusualURLPattern(final String urlPattern) {
        if (StandardContext.log.isInfoEnabled() && urlPattern.endsWith("*") && (urlPattern.length() < 2 || urlPattern.charAt(urlPattern.length() - 2) != '/')) {
            StandardContext.log.info((Object)("Suspicious url pattern: \"" + urlPattern + "\"" + " in context [" + this.getName() + "] - see" + " section SRV.11.2 of the Servlet specification"));
        }
    }
    
    public String getDeploymentDescriptor() {
        InputStream stream = null;
        final ServletContext servletContext = this.getServletContext();
        if (servletContext != null) {
            stream = servletContext.getResourceAsStream("/WEB-INF/web.xml");
        }
        if (stream == null) {
            return "";
        }
        final StringBuilder sb = new StringBuilder();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(stream));
            for (String strRead = ""; strRead != null; strRead = br.readLine()) {
                sb.append(strRead);
            }
        }
        catch (IOException e) {
            return "";
        }
        finally {
            if (br != null) {
                try {
                    br.close();
                }
                catch (IOException ex) {}
            }
        }
        return sb.toString();
    }
    
    public String[] getServlets() {
        String[] result = null;
        final Container[] children = this.findChildren();
        if (children != null) {
            result = new String[children.length];
            for (int i = 0; i < children.length; ++i) {
                result[i] = children[i].getObjectName().toString();
            }
        }
        return result;
    }
    
    @Override
    protected String getObjectNameKeyProperties() {
        final StringBuilder keyProperties = new StringBuilder("j2eeType=WebModule,");
        keyProperties.append(this.getObjectKeyPropertiesNameOnly());
        keyProperties.append(",J2EEApplication=");
        keyProperties.append(this.getJ2EEApplication());
        keyProperties.append(",J2EEServer=");
        keyProperties.append(this.getJ2EEServer());
        return keyProperties.toString();
    }
    
    private String getObjectKeyPropertiesNameOnly() {
        final StringBuilder result = new StringBuilder("name=//");
        final String hostname = this.getParent().getName();
        if (hostname == null) {
            result.append("DEFAULT");
        }
        else {
            result.append(hostname);
        }
        final String contextName = this.getName();
        if (!contextName.startsWith("/")) {
            result.append('/');
        }
        result.append(contextName);
        return result.toString();
    }
    
    @Override
    protected void initInternal() throws LifecycleException {
        super.initInternal();
        if (this.namingResources != null) {
            this.namingResources.init();
        }
        if (this.resources != null) {
            this.resources.start();
        }
        if (this.getObjectName() != null) {
            final Notification notification = new Notification("j2ee.object.created", this.getObjectName(), this.sequenceNumber.getAndIncrement());
            this.broadcaster.sendNotification(notification);
        }
    }
    
    @Override
    public void removeNotificationListener(final NotificationListener listener, final NotificationFilter filter, final Object object) throws ListenerNotFoundException {
        this.broadcaster.removeNotificationListener(listener, filter, object);
    }
    
    @Override
    public MBeanNotificationInfo[] getNotificationInfo() {
        if (this.notificationInfo == null) {
            this.notificationInfo = new MBeanNotificationInfo[] { new MBeanNotificationInfo(new String[] { "j2ee.object.created" }, Notification.class.getName(), "web application is created"), new MBeanNotificationInfo(new String[] { "j2ee.state.starting" }, Notification.class.getName(), "change web application is starting"), new MBeanNotificationInfo(new String[] { "j2ee.state.running" }, Notification.class.getName(), "web application is running"), new MBeanNotificationInfo(new String[] { "j2ee.state.stopping" }, Notification.class.getName(), "web application start to stopped"), new MBeanNotificationInfo(new String[] { "j2ee.object.stopped" }, Notification.class.getName(), "web application is stopped"), new MBeanNotificationInfo(new String[] { "j2ee.object.deleted" }, Notification.class.getName(), "web application is deleted") };
        }
        return this.notificationInfo;
    }
    
    @Override
    public void addNotificationListener(final NotificationListener listener, final NotificationFilter filter, final Object object) throws IllegalArgumentException {
        this.broadcaster.addNotificationListener(listener, filter, object);
    }
    
    @Override
    public void removeNotificationListener(final NotificationListener listener) throws ListenerNotFoundException {
        this.broadcaster.removeNotificationListener(listener);
    }
    
    public String[] getWelcomeFiles() {
        return this.findWelcomeFiles();
    }
    
    @Override
    public boolean getXmlNamespaceAware() {
        return this.webXmlNamespaceAware;
    }
    
    @Override
    public void setXmlNamespaceAware(final boolean webXmlNamespaceAware) {
        this.webXmlNamespaceAware = webXmlNamespaceAware;
    }
    
    @Override
    public void setXmlValidation(final boolean webXmlValidation) {
        this.webXmlValidation = webXmlValidation;
    }
    
    @Override
    public boolean getXmlValidation() {
        return this.webXmlValidation;
    }
    
    @Override
    public void setXmlBlockExternal(final boolean xmlBlockExternal) {
        this.xmlBlockExternal = xmlBlockExternal;
    }
    
    @Override
    public boolean getXmlBlockExternal() {
        return this.xmlBlockExternal;
    }
    
    @Override
    public void setTldValidation(final boolean tldValidation) {
        this.tldValidation = tldValidation;
    }
    
    @Override
    public boolean getTldValidation() {
        return this.tldValidation;
    }
    
    public boolean isStateManageable() {
        return true;
    }
    
    public String getServer() {
        return this.server;
    }
    
    public String setServer(final String server) {
        return this.server = server;
    }
    
    public String[] getJavaVMs() {
        return this.javaVMs;
    }
    
    public String[] setJavaVMs(final String[] javaVMs) {
        return this.javaVMs = javaVMs;
    }
    
    public long getStartTime() {
        return this.startTime;
    }
    
    static {
        log = LogFactory.getLog((Class)StandardContext.class);
        (StandardContext.urlEncoder = new URLEncoder()).addSafeCharacter('~');
        StandardContext.urlEncoder.addSafeCharacter('-');
        StandardContext.urlEncoder.addSafeCharacter('_');
        StandardContext.urlEncoder.addSafeCharacter('.');
        StandardContext.urlEncoder.addSafeCharacter('*');
        StandardContext.urlEncoder.addSafeCharacter('/');
        DEFAULT_NAMING_LISTENER = new ThreadBindingListener() {
            @Override
            public void bind() {
            }
            
            @Override
            public void unbind() {
            }
        };
    }
    
    private static final class ContextFilterMaps
    {
        private final Object lock;
        private FilterMap[] array;
        private int insertPoint;
        
        private ContextFilterMaps() {
            this.lock = new Object();
            this.array = new FilterMap[0];
            this.insertPoint = 0;
        }
        
        public FilterMap[] asArray() {
            synchronized (this.lock) {
                return this.array;
            }
        }
        
        public void add(final FilterMap filterMap) {
            synchronized (this.lock) {
                final FilterMap[] results = Arrays.copyOf(this.array, this.array.length + 1);
                results[this.array.length] = filterMap;
                this.array = results;
            }
        }
        
        public void addBefore(final FilterMap filterMap) {
            synchronized (this.lock) {
                final FilterMap[] results = new FilterMap[this.array.length + 1];
                System.arraycopy(this.array, 0, results, 0, this.insertPoint);
                System.arraycopy(this.array, this.insertPoint, results, this.insertPoint + 1, this.array.length - this.insertPoint);
                results[this.insertPoint] = filterMap;
                this.array = results;
                ++this.insertPoint;
            }
        }
        
        public void remove(final FilterMap filterMap) {
            synchronized (this.lock) {
                int n = -1;
                for (int i = 0; i < this.array.length; ++i) {
                    if (this.array[i] == filterMap) {
                        n = i;
                        break;
                    }
                }
                if (n < 0) {
                    return;
                }
                final FilterMap[] results = new FilterMap[this.array.length - 1];
                System.arraycopy(this.array, 0, results, 0, n);
                System.arraycopy(this.array, n + 1, results, n, this.array.length - 1 - n);
                this.array = results;
                if (n < this.insertPoint) {
                    --this.insertPoint;
                }
            }
        }
    }
    
    private static class PrivilegedSetTccl implements PrivilegedAction<Void>
    {
        private ClassLoader cl;
        
        PrivilegedSetTccl(final ClassLoader cl) {
            this.cl = cl;
        }
        
        @Override
        public Void run() {
            Thread.currentThread().setContextClassLoader(this.cl);
            return null;
        }
    }
    
    private static class PrivilegedGetTccl implements PrivilegedAction<ClassLoader>
    {
        @Override
        public ClassLoader run() {
            return Thread.currentThread().getContextClassLoader();
        }
    }
    
    private static class NoPluggabilityServletContext implements ServletContext
    {
        private final ServletContext sc;
        
        public NoPluggabilityServletContext(final ServletContext sc) {
            this.sc = sc;
        }
        
        public String getContextPath() {
            return this.sc.getContextPath();
        }
        
        public ServletContext getContext(final String uripath) {
            return this.sc.getContext(uripath);
        }
        
        public int getMajorVersion() {
            return this.sc.getMajorVersion();
        }
        
        public int getMinorVersion() {
            return this.sc.getMinorVersion();
        }
        
        public int getEffectiveMajorVersion() {
            throw new UnsupportedOperationException(ContainerBase.sm.getString("noPluggabilityServletContext.notAllowed"));
        }
        
        public int getEffectiveMinorVersion() {
            throw new UnsupportedOperationException(ContainerBase.sm.getString("noPluggabilityServletContext.notAllowed"));
        }
        
        public String getMimeType(final String file) {
            return this.sc.getMimeType(file);
        }
        
        public Set<String> getResourcePaths(final String path) {
            return (Set<String>)this.sc.getResourcePaths(path);
        }
        
        public URL getResource(final String path) throws MalformedURLException {
            return this.sc.getResource(path);
        }
        
        public InputStream getResourceAsStream(final String path) {
            return this.sc.getResourceAsStream(path);
        }
        
        public RequestDispatcher getRequestDispatcher(final String path) {
            return this.sc.getRequestDispatcher(path);
        }
        
        public RequestDispatcher getNamedDispatcher(final String name) {
            return this.sc.getNamedDispatcher(name);
        }
        
        @Deprecated
        public Servlet getServlet(final String name) throws ServletException {
            return this.sc.getServlet(name);
        }
        
        @Deprecated
        public Enumeration<Servlet> getServlets() {
            return (Enumeration<Servlet>)this.sc.getServlets();
        }
        
        @Deprecated
        public Enumeration<String> getServletNames() {
            return (Enumeration<String>)this.sc.getServletNames();
        }
        
        public void log(final String msg) {
            this.sc.log(msg);
        }
        
        @Deprecated
        public void log(final Exception exception, final String msg) {
            this.sc.log(exception, msg);
        }
        
        public void log(final String message, final Throwable throwable) {
            this.sc.log(message, throwable);
        }
        
        public String getRealPath(final String path) {
            return this.sc.getRealPath(path);
        }
        
        public String getServerInfo() {
            return this.sc.getServerInfo();
        }
        
        public String getInitParameter(final String name) {
            return this.sc.getInitParameter(name);
        }
        
        public Enumeration<String> getInitParameterNames() {
            return (Enumeration<String>)this.sc.getInitParameterNames();
        }
        
        public boolean setInitParameter(final String name, final String value) {
            throw new UnsupportedOperationException(ContainerBase.sm.getString("noPluggabilityServletContext.notAllowed"));
        }
        
        public Object getAttribute(final String name) {
            return this.sc.getAttribute(name);
        }
        
        public Enumeration<String> getAttributeNames() {
            return (Enumeration<String>)this.sc.getAttributeNames();
        }
        
        public void setAttribute(final String name, final Object object) {
            this.sc.setAttribute(name, object);
        }
        
        public void removeAttribute(final String name) {
            this.sc.removeAttribute(name);
        }
        
        public String getServletContextName() {
            return this.sc.getServletContextName();
        }
        
        public ServletRegistration.Dynamic addServlet(final String servletName, final String className) {
            throw new UnsupportedOperationException(ContainerBase.sm.getString("noPluggabilityServletContext.notAllowed"));
        }
        
        public ServletRegistration.Dynamic addServlet(final String servletName, final Servlet servlet) {
            throw new UnsupportedOperationException(ContainerBase.sm.getString("noPluggabilityServletContext.notAllowed"));
        }
        
        public ServletRegistration.Dynamic addServlet(final String servletName, final Class<? extends Servlet> servletClass) {
            throw new UnsupportedOperationException(ContainerBase.sm.getString("noPluggabilityServletContext.notAllowed"));
        }
        
        public <T extends Servlet> T createServlet(final Class<T> c) throws ServletException {
            throw new UnsupportedOperationException(ContainerBase.sm.getString("noPluggabilityServletContext.notAllowed"));
        }
        
        public ServletRegistration getServletRegistration(final String servletName) {
            throw new UnsupportedOperationException(ContainerBase.sm.getString("noPluggabilityServletContext.notAllowed"));
        }
        
        public Map<String, ? extends ServletRegistration> getServletRegistrations() {
            throw new UnsupportedOperationException(ContainerBase.sm.getString("noPluggabilityServletContext.notAllowed"));
        }
        
        public FilterRegistration.Dynamic addFilter(final String filterName, final String className) {
            throw new UnsupportedOperationException(ContainerBase.sm.getString("noPluggabilityServletContext.notAllowed"));
        }
        
        public FilterRegistration.Dynamic addFilter(final String filterName, final Filter filter) {
            throw new UnsupportedOperationException(ContainerBase.sm.getString("noPluggabilityServletContext.notAllowed"));
        }
        
        public FilterRegistration.Dynamic addFilter(final String filterName, final Class<? extends Filter> filterClass) {
            throw new UnsupportedOperationException(ContainerBase.sm.getString("noPluggabilityServletContext.notAllowed"));
        }
        
        public <T extends Filter> T createFilter(final Class<T> c) throws ServletException {
            throw new UnsupportedOperationException(ContainerBase.sm.getString("noPluggabilityServletContext.notAllowed"));
        }
        
        public FilterRegistration getFilterRegistration(final String filterName) {
            throw new UnsupportedOperationException(ContainerBase.sm.getString("noPluggabilityServletContext.notAllowed"));
        }
        
        public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
            throw new UnsupportedOperationException(ContainerBase.sm.getString("noPluggabilityServletContext.notAllowed"));
        }
        
        public SessionCookieConfig getSessionCookieConfig() {
            throw new UnsupportedOperationException(ContainerBase.sm.getString("noPluggabilityServletContext.notAllowed"));
        }
        
        public void setSessionTrackingModes(final Set<SessionTrackingMode> sessionTrackingModes) {
            throw new UnsupportedOperationException(ContainerBase.sm.getString("noPluggabilityServletContext.notAllowed"));
        }
        
        public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
            throw new UnsupportedOperationException(ContainerBase.sm.getString("noPluggabilityServletContext.notAllowed"));
        }
        
        public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
            throw new UnsupportedOperationException(ContainerBase.sm.getString("noPluggabilityServletContext.notAllowed"));
        }
        
        public void addListener(final String className) {
            throw new UnsupportedOperationException(ContainerBase.sm.getString("noPluggabilityServletContext.notAllowed"));
        }
        
        public <T extends EventListener> void addListener(final T t) {
            throw new UnsupportedOperationException(ContainerBase.sm.getString("noPluggabilityServletContext.notAllowed"));
        }
        
        public void addListener(final Class<? extends EventListener> listenerClass) {
            throw new UnsupportedOperationException(ContainerBase.sm.getString("noPluggabilityServletContext.notAllowed"));
        }
        
        public <T extends EventListener> T createListener(final Class<T> c) throws ServletException {
            throw new UnsupportedOperationException(ContainerBase.sm.getString("noPluggabilityServletContext.notAllowed"));
        }
        
        public JspConfigDescriptor getJspConfigDescriptor() {
            throw new UnsupportedOperationException(ContainerBase.sm.getString("noPluggabilityServletContext.notAllowed"));
        }
        
        public ClassLoader getClassLoader() {
            throw new UnsupportedOperationException(ContainerBase.sm.getString("noPluggabilityServletContext.notAllowed"));
        }
        
        public void declareRoles(final String... roleNames) {
            throw new UnsupportedOperationException(ContainerBase.sm.getString("noPluggabilityServletContext.notAllowed"));
        }
        
        public String getVirtualServerName() {
            return this.sc.getVirtualServerName();
        }
    }
}
