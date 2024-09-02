// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.catalina.startup;

import java.net.JarURLConnection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.bcel.classfile.AnnotationElementValue;
import org.apache.tomcat.util.bcel.classfile.ArrayElementValue;
import org.apache.tomcat.util.bcel.classfile.ElementValue;
import org.apache.tomcat.util.bcel.classfile.ElementValuePair;
import java.util.Collection;
import org.apache.catalina.util.Introspection;
import org.apache.tomcat.util.bcel.classfile.AnnotationEntry;
import org.apache.tomcat.util.bcel.classfile.JavaClass;
import org.apache.tomcat.util.bcel.classfile.ClassParser;
import java.util.Enumeration;
import org.apache.naming.resources.DirContextURLConnection;
import org.apache.tomcat.util.bcel.classfile.ClassFormatException;
import org.apache.tomcat.JarScanner;
import org.apache.tomcat.JarScannerCallback;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import org.apache.tomcat.util.scan.Jar;
import java.net.URISyntaxException;
import javax.naming.directory.DirContext;
import org.apache.tomcat.util.scan.JarFactory;
import javax.servlet.annotation.HandlesTypes;
import org.apache.catalina.deploy.ServletDef;
import org.apache.tomcat.util.descriptor.InputSourceUtil;
import java.util.Iterator;
import javax.naming.NamingEnumeration;
import javax.servlet.ServletContext;
import java.util.LinkedHashSet;
import javax.naming.NamingException;
import org.apache.naming.resources.FileDirContext;
import javax.naming.Binding;
import javax.naming.NameNotFoundException;
import org.apache.catalina.deploy.WebXml;
import java.util.HashSet;
import org.apache.catalina.Wrapper;
import org.apache.catalina.Service;
import org.apache.catalina.Engine;
import org.apache.catalina.Server;
import org.apache.catalina.deploy.FilterMap;
import org.apache.catalina.deploy.FilterDef;
import org.apache.catalina.deploy.ErrorPage;
import org.apache.tomcat.util.buf.UriUtil;
import java.util.Locale;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.util.ContextName;
import java.net.URLConnection;
import java.io.InputStream;
import java.io.IOException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ErrorHandler;
import org.apache.tomcat.util.descriptor.XmlErrorHandler;
import org.xml.sax.InputSource;
import java.net.URL;
import java.net.MalformedURLException;
import org.apache.catalina.Container;
import org.apache.catalina.core.StandardEngine;
import java.util.ArrayList;
import java.util.List;
import org.apache.tomcat.util.digester.RuleSet;
import org.apache.tomcat.util.descriptor.DigesterFactory;
import org.apache.catalina.Pipeline;
import org.apache.catalina.deploy.SecurityConstraint;
import org.apache.tomcat.util.ExceptionUtils;
import org.apache.catalina.Valve;
import org.apache.catalina.core.ContainerBase;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.LifecycleEvent;
import java.util.StringTokenizer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import org.apache.tomcat.util.digester.Digester;
import java.io.File;
import org.apache.catalina.Context;
import org.apache.catalina.Authenticator;
import javax.servlet.ServletContainerInitializer;
import org.apache.catalina.Host;
import java.util.Map;
import java.util.Set;
import java.util.Properties;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.tomcat.util.res.StringManager;
import org.apache.juli.logging.Log;
import org.apache.catalina.LifecycleListener;

public class ContextConfig implements LifecycleListener
{
    private static final Log log;
    protected static final StringManager sm;
    protected static final LoginConfig DUMMY_LOGIN_CONFIG;
    protected static final Properties authenticators;
    private static final Set<String> pluggabilityJarsToSkip;
    protected static long deploymentCount;
    protected static final Map<Host, DefaultWebXmlCacheEntry> hostWebXmlCache;
    private static final Set<ServletContainerInitializer> EMPTY_SCI_SET;
    protected Map<String, Authenticator> customAuthenticators;
    protected Context context;
    @Deprecated
    protected String defaultContextXml;
    protected String defaultWebXml;
    protected boolean ok;
    protected String originalDocBase;
    private File antiLockingDocBase;
    protected final Map<ServletContainerInitializer, Set<Class<?>>> initializerClassMap;
    protected final Map<Class<?>, Set<ServletContainerInitializer>> typeInitializerMap;
    protected final Map<String, JavaClassCacheEntry> javaClassCache;
    protected boolean handlesTypesAnnotations;
    protected boolean handlesTypesNonAnnotations;
    protected Digester webDigester;
    protected WebRuleSet webRuleSet;
    protected Digester webFragmentDigester;
    protected WebRuleSet webFragmentRuleSet;
    
    public ContextConfig() {
        this.context = null;
        this.defaultContextXml = null;
        this.defaultWebXml = null;
        this.ok = false;
        this.originalDocBase = null;
        this.antiLockingDocBase = null;
        this.initializerClassMap = new LinkedHashMap<ServletContainerInitializer, Set<Class<?>>>();
        this.typeInitializerMap = new HashMap<Class<?>, Set<ServletContainerInitializer>>();
        this.javaClassCache = new HashMap<String, JavaClassCacheEntry>();
        this.handlesTypesAnnotations = false;
        this.handlesTypesNonAnnotations = false;
        this.webDigester = null;
        this.webRuleSet = null;
        this.webFragmentDigester = null;
        this.webFragmentRuleSet = null;
    }
    
    private static void addJarsToSkip(final String systemPropertyName) {
        final String jarList = System.getProperty(systemPropertyName);
        if (jarList != null) {
            final StringTokenizer tokenizer = new StringTokenizer(jarList, ",");
            while (tokenizer.hasMoreElements()) {
                final String token = tokenizer.nextToken().trim();
                if (token.length() > 0) {
                    ContextConfig.pluggabilityJarsToSkip.add(token);
                }
            }
        }
    }
    
    public String getDefaultWebXml() {
        if (this.defaultWebXml == null) {
            this.defaultWebXml = "conf/web.xml";
        }
        return this.defaultWebXml;
    }
    
    public void setDefaultWebXml(final String path) {
        this.defaultWebXml = path;
    }
    
    @Deprecated
    public String getDefaultContextXml() {
        if (this.defaultContextXml == null) {
            this.defaultContextXml = "conf/context.xml";
        }
        return this.defaultContextXml;
    }
    
    @Deprecated
    public void setDefaultContextXml(final String path) {
        this.defaultContextXml = path;
    }
    
    public void setCustomAuthenticators(final Map<String, Authenticator> customAuthenticators) {
        this.customAuthenticators = customAuthenticators;
    }
    
    @Override
    public void lifecycleEvent(final LifecycleEvent event) {
        try {
            this.context = (Context)event.getLifecycle();
        }
        catch (ClassCastException e) {
            ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.cce", event.getLifecycle()), (Throwable)e);
            return;
        }
        if (event.getType().equals("configure_start")) {
            this.configureStart();
        }
        else if (event.getType().equals("before_start")) {
            this.beforeStart();
        }
        else if (event.getType().equals("after_start")) {
            if (this.originalDocBase != null) {
                this.context.setDocBase(this.originalDocBase);
            }
        }
        else if (event.getType().equals("configure_stop")) {
            this.configureStop();
        }
        else if (event.getType().equals("after_init")) {
            this.init();
        }
        else if (event.getType().equals("after_destroy")) {
            this.destroy();
        }
    }
    
    protected void applicationAnnotationsConfig() {
        final long t1 = System.currentTimeMillis();
        WebAnnotationSet.loadApplicationAnnotations(this.context);
        final long t2 = System.currentTimeMillis();
        if (this.context instanceof StandardContext) {
            ((StandardContext)this.context).setStartupTime(t2 - t1 + ((StandardContext)this.context).getStartupTime());
        }
    }
    
    protected void authenticatorConfig() {
        LoginConfig loginConfig = this.context.getLoginConfig();
        final SecurityConstraint[] constraints = this.context.findConstraints();
        if (this.context.getIgnoreAnnotations() && (constraints == null || constraints.length == 0) && !this.context.getPreemptiveAuthentication()) {
            return;
        }
        if (loginConfig == null) {
            loginConfig = ContextConfig.DUMMY_LOGIN_CONFIG;
            this.context.setLoginConfig(loginConfig);
        }
        if (this.context.getAuthenticator() != null) {
            return;
        }
        if (!(this.context instanceof ContainerBase)) {
            return;
        }
        if (this.context.getRealm() == null) {
            ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.missingRealm"));
            this.ok = false;
            return;
        }
        Valve authenticator = null;
        if (this.customAuthenticators != null) {
            authenticator = (Valve)this.customAuthenticators.get(loginConfig.getAuthMethod());
        }
        if (authenticator == null) {
            if (ContextConfig.authenticators == null) {
                ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.authenticatorResources"));
                this.ok = false;
                return;
            }
            String authenticatorName = null;
            authenticatorName = ContextConfig.authenticators.getProperty(loginConfig.getAuthMethod());
            if (authenticatorName == null) {
                ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.authenticatorMissing", loginConfig.getAuthMethod()));
                this.ok = false;
                return;
            }
            try {
                final Class<?> authenticatorClass = Class.forName(authenticatorName);
                authenticator = (Valve)authenticatorClass.newInstance();
            }
            catch (Throwable t) {
                ExceptionUtils.handleThrowable(t);
                ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.authenticatorInstantiate", authenticatorName), t);
                this.ok = false;
            }
        }
        if (authenticator != null && this.context instanceof ContainerBase) {
            final Pipeline pipeline = ((ContainerBase)this.context).getPipeline();
            if (pipeline != null) {
                ((ContainerBase)this.context).getPipeline().addValve(authenticator);
                if (ContextConfig.log.isDebugEnabled()) {
                    ContextConfig.log.debug((Object)ContextConfig.sm.getString("contextConfig.authenticatorConfigured", loginConfig.getAuthMethod()));
                }
            }
        }
    }
    
    public void createWebXmlDigester(final boolean namespaceAware, final boolean validation) {
        final boolean blockExternal = this.context.getXmlBlockExternal();
        this.webRuleSet = new WebRuleSet(false);
        (this.webDigester = DigesterFactory.newDigester(validation, namespaceAware, this.webRuleSet, blockExternal)).getParser();
        this.webFragmentRuleSet = new WebRuleSet(true);
        (this.webFragmentDigester = DigesterFactory.newDigester(validation, namespaceAware, this.webFragmentRuleSet, blockExternal)).getParser();
    }
    
    protected Digester createContextDigester() {
        final Digester digester = new Digester();
        digester.setValidating(false);
        digester.setRulesValidation(true);
        final HashMap<Class<?>, List<String>> fakeAttributes = new HashMap<Class<?>, List<String>>();
        final ArrayList<String> attrs = new ArrayList<String>();
        attrs.add("className");
        fakeAttributes.put(Object.class, attrs);
        digester.setFakeAttributes(fakeAttributes);
        final RuleSet contextRuleSet = new ContextRuleSet("", false);
        digester.addRuleSet(contextRuleSet);
        final RuleSet namingRuleSet = new NamingRuleSet("Context/");
        digester.addRuleSet(namingRuleSet);
        return digester;
    }
    
    protected String getBaseDir() {
        final Container engineC = this.context.getParent().getParent();
        if (engineC instanceof StandardEngine) {
            return ((StandardEngine)engineC).getBaseDir();
        }
        return System.getProperty("catalina.base");
    }
    
    protected void contextConfig(final Digester digester) {
        if (this.defaultContextXml == null && this.context instanceof StandardContext) {
            this.defaultContextXml = ((StandardContext)this.context).getDefaultContextXml();
        }
        if (this.defaultContextXml == null) {
            this.getDefaultContextXml();
        }
        if (!this.context.getOverride()) {
            File defaultContextFile = new File(this.defaultContextXml);
            if (!defaultContextFile.isAbsolute()) {
                defaultContextFile = new File(this.getBaseDir(), this.defaultContextXml);
            }
            if (defaultContextFile.exists()) {
                try {
                    final URL defaultContextUrl = defaultContextFile.toURI().toURL();
                    this.processContextConfig(digester, defaultContextUrl);
                }
                catch (MalformedURLException e) {
                    ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.badUrl", defaultContextFile), (Throwable)e);
                }
            }
            final File hostContextFile = new File(this.getHostConfigBase(), "context.xml.default");
            if (hostContextFile.exists()) {
                try {
                    final URL hostContextUrl = hostContextFile.toURI().toURL();
                    this.processContextConfig(digester, hostContextUrl);
                }
                catch (MalformedURLException e2) {
                    ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.badUrl", hostContextFile), (Throwable)e2);
                }
            }
        }
        if (this.context.getConfigFile() != null) {
            this.processContextConfig(digester, this.context.getConfigFile());
        }
    }
    
    protected void processContextConfig(final Digester digester, final URL contextXml) {
        if (ContextConfig.log.isDebugEnabled()) {
            ContextConfig.log.debug((Object)("Processing context [" + this.context.getName() + "] configuration file [" + contextXml + "]"));
        }
        InputSource source = null;
        InputStream stream = null;
        try {
            source = new InputSource(contextXml.toString());
            final URLConnection xmlConn = contextXml.openConnection();
            xmlConn.setUseCaches(false);
            stream = xmlConn.getInputStream();
        }
        catch (Exception e) {
            ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.contextMissing", contextXml), (Throwable)e);
        }
        if (source == null) {
            return;
        }
        try {
            source.setByteStream(stream);
            digester.setClassLoader(this.getClass().getClassLoader());
            digester.setUseContextClassLoader(false);
            digester.push(this.context.getParent());
            digester.push(this.context);
            final XmlErrorHandler errorHandler = new XmlErrorHandler();
            digester.setErrorHandler(errorHandler);
            digester.parse(source);
            if (errorHandler.getWarnings().size() > 0 || errorHandler.getErrors().size() > 0) {
                errorHandler.logFindings(ContextConfig.log, contextXml.toString());
                this.ok = false;
            }
            if (ContextConfig.log.isDebugEnabled()) {
                ContextConfig.log.debug((Object)("Successfully processed context [" + this.context.getName() + "] configuration file [" + contextXml + "]"));
            }
        }
        catch (SAXParseException e2) {
            ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.contextParse", this.context.getName()), (Throwable)e2);
            ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.defaultPosition", "" + e2.getLineNumber(), "" + e2.getColumnNumber()));
            this.ok = false;
        }
        catch (Exception e) {
            ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.contextParse", this.context.getName()), (Throwable)e);
            this.ok = false;
        }
        finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            }
            catch (IOException e3) {
                ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.contextClose"), (Throwable)e3);
            }
        }
    }
    
    protected void fixDocBase() throws IOException {
        final Host host = (Host)this.context.getParent();
        final String appBase = host.getAppBase();
        File canonicalAppBase = new File(appBase);
        if (canonicalAppBase.isAbsolute()) {
            canonicalAppBase = canonicalAppBase.getCanonicalFile();
        }
        else {
            canonicalAppBase = new File(this.getBaseDir(), appBase).getCanonicalFile();
        }
        String docBase = this.context.getDocBase();
        if (docBase == null) {
            final String path = this.context.getPath();
            if (path == null) {
                return;
            }
            final ContextName cn = new ContextName(path, this.context.getWebappVersion());
            docBase = cn.getBaseName();
        }
        File file = new File(docBase);
        if (!file.isAbsolute()) {
            docBase = new File(canonicalAppBase, docBase).getPath();
        }
        else {
            docBase = file.getCanonicalPath();
        }
        file = new File(docBase);
        final String origDocBase = docBase;
        final ContextName cn2 = new ContextName(this.context.getPath(), this.context.getWebappVersion());
        final String pathName = cn2.getBaseName();
        boolean unpackWARs = true;
        if (host instanceof StandardHost) {
            unpackWARs = ((StandardHost)host).isUnpackWARs();
            if (unpackWARs && this.context instanceof StandardContext) {
                unpackWARs = ((StandardContext)this.context).getUnpackWAR();
            }
        }
        if (docBase.toLowerCase(Locale.ENGLISH).endsWith(".war") && !file.isDirectory()) {
            final URL war = UriUtil.buildJarUrl(new File(docBase));
            if (unpackWARs) {
                docBase = ExpandWar.expand(host, war, pathName);
                file = new File(docBase);
                docBase = file.getCanonicalPath();
                if (this.context instanceof StandardContext) {
                    ((StandardContext)this.context).setOriginalDocBase(origDocBase);
                }
            }
            else {
                ExpandWar.validate(host, war, pathName);
            }
        }
        else {
            final File docDir = new File(docBase);
            if (!docDir.exists()) {
                final File warFile = new File(docBase + ".war");
                if (warFile.exists()) {
                    final URL war2 = UriUtil.buildJarUrl(warFile);
                    if (unpackWARs) {
                        docBase = ExpandWar.expand(host, war2, pathName);
                        file = new File(docBase);
                        docBase = file.getCanonicalPath();
                    }
                    else {
                        docBase = warFile.getCanonicalPath();
                        ExpandWar.validate(host, war2, pathName);
                    }
                }
                if (this.context instanceof StandardContext) {
                    ((StandardContext)this.context).setOriginalDocBase(origDocBase);
                }
            }
        }
        if (docBase.startsWith(canonicalAppBase.getPath() + File.separatorChar)) {
            docBase = docBase.substring(canonicalAppBase.getPath().length());
            docBase = docBase.replace(File.separatorChar, '/');
            if (docBase.startsWith("/")) {
                docBase = docBase.substring(1);
            }
        }
        else {
            docBase = docBase.replace(File.separatorChar, '/');
        }
        this.context.setDocBase(docBase);
    }
    
    protected void antiLocking() {
        if (this.context instanceof StandardContext && ((StandardContext)this.context).getAntiResourceLocking()) {
            final Host host = (Host)this.context.getParent();
            final String appBase = host.getAppBase();
            String docBase = this.context.getDocBase();
            if (docBase == null) {
                return;
            }
            this.originalDocBase = docBase;
            File docBaseFile = new File(docBase);
            if (!docBaseFile.isAbsolute()) {
                File file = new File(appBase);
                if (!file.isAbsolute()) {
                    file = new File(this.getBaseDir(), appBase);
                }
                docBaseFile = new File(file, docBase);
            }
            final String path = this.context.getPath();
            if (path == null) {
                return;
            }
            final ContextName cn = new ContextName(path, this.context.getWebappVersion());
            docBase = cn.getBaseName();
            if (this.originalDocBase.toLowerCase(Locale.ENGLISH).endsWith(".war")) {
                this.antiLockingDocBase = new File(System.getProperty("java.io.tmpdir"), ContextConfig.deploymentCount++ + "-" + docBase + ".war");
            }
            else {
                this.antiLockingDocBase = new File(System.getProperty("java.io.tmpdir"), ContextConfig.deploymentCount++ + "-" + docBase);
            }
            this.antiLockingDocBase = this.antiLockingDocBase.getAbsoluteFile();
            if (ContextConfig.log.isDebugEnabled()) {
                ContextConfig.log.debug((Object)("Anti locking context[" + this.context.getName() + "] setting docBase to " + this.antiLockingDocBase.getPath()));
            }
            ExpandWar.delete(this.antiLockingDocBase);
            if (ExpandWar.copy(docBaseFile, this.antiLockingDocBase)) {
                this.context.setDocBase(this.antiLockingDocBase.getPath());
            }
        }
    }
    
    protected void init() {
        final Digester contextDigester = this.createContextDigester();
        contextDigester.getParser();
        if (ContextConfig.log.isDebugEnabled()) {
            ContextConfig.log.debug((Object)ContextConfig.sm.getString("contextConfig.init"));
        }
        this.context.setConfigured(false);
        this.ok = true;
        this.contextConfig(contextDigester);
        this.createWebXmlDigester(this.context.getXmlNamespaceAware(), this.context.getXmlValidation());
    }
    
    protected synchronized void beforeStart() {
        try {
            this.fixDocBase();
        }
        catch (IOException e) {
            ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.fixDocBase", this.context.getName()), (Throwable)e);
        }
        this.antiLocking();
    }
    
    protected synchronized void configureStart() {
        if (ContextConfig.log.isDebugEnabled()) {
            ContextConfig.log.debug((Object)ContextConfig.sm.getString("contextConfig.start"));
        }
        if (ContextConfig.log.isDebugEnabled()) {
            ContextConfig.log.debug((Object)ContextConfig.sm.getString("contextConfig.xmlSettings", this.context.getName(), this.context.getXmlValidation(), this.context.getXmlNamespaceAware()));
        }
        this.webConfig();
        if (!this.context.getIgnoreAnnotations()) {
            this.applicationAnnotationsConfig();
        }
        if (this.ok) {
            this.validateSecurityRoles();
        }
        if (this.ok) {
            this.authenticatorConfig();
        }
        if (ContextConfig.log.isDebugEnabled() && this.context instanceof ContainerBase) {
            ContextConfig.log.debug((Object)"Pipeline Configuration:");
            final Pipeline pipeline = ((ContainerBase)this.context).getPipeline();
            Valve[] valves = null;
            if (pipeline != null) {
                valves = pipeline.getValves();
            }
            if (valves != null) {
                for (int i = 0; i < valves.length; ++i) {
                    ContextConfig.log.debug((Object)("  " + valves[i].getInfo()));
                }
            }
            ContextConfig.log.debug((Object)"======================");
        }
        if (this.ok) {
            this.context.setConfigured(true);
        }
        else {
            ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.unavailable"));
            this.context.setConfigured(false);
        }
    }
    
    protected synchronized void configureStop() {
        if (ContextConfig.log.isDebugEnabled()) {
            ContextConfig.log.debug((Object)ContextConfig.sm.getString("contextConfig.stop"));
        }
        final Container[] children = this.context.findChildren();
        for (int i = 0; i < children.length; ++i) {
            this.context.removeChild(children[i]);
        }
        final SecurityConstraint[] securityConstraints = this.context.findConstraints();
        for (int i = 0; i < securityConstraints.length; ++i) {
            this.context.removeConstraint(securityConstraints[i]);
        }
        final ErrorPage[] errorPages = this.context.findErrorPages();
        for (int i = 0; i < errorPages.length; ++i) {
            this.context.removeErrorPage(errorPages[i]);
        }
        final FilterDef[] filterDefs = this.context.findFilterDefs();
        for (int i = 0; i < filterDefs.length; ++i) {
            this.context.removeFilterDef(filterDefs[i]);
        }
        final FilterMap[] filterMaps = this.context.findFilterMaps();
        for (int i = 0; i < filterMaps.length; ++i) {
            this.context.removeFilterMap(filterMaps[i]);
        }
        final String[] mimeMappings = this.context.findMimeMappings();
        for (int i = 0; i < mimeMappings.length; ++i) {
            this.context.removeMimeMapping(mimeMappings[i]);
        }
        final String[] parameters = this.context.findParameters();
        for (int i = 0; i < parameters.length; ++i) {
            this.context.removeParameter(parameters[i]);
        }
        final String[] securityRoles = this.context.findSecurityRoles();
        for (int i = 0; i < securityRoles.length; ++i) {
            this.context.removeSecurityRole(securityRoles[i]);
        }
        final String[] servletMappings = this.context.findServletMappings();
        for (int i = 0; i < servletMappings.length; ++i) {
            this.context.removeServletMapping(servletMappings[i]);
        }
        final String[] welcomeFiles = this.context.findWelcomeFiles();
        for (int i = 0; i < welcomeFiles.length; ++i) {
            this.context.removeWelcomeFile(welcomeFiles[i]);
        }
        final String[] wrapperLifecycles = this.context.findWrapperLifecycles();
        for (int i = 0; i < wrapperLifecycles.length; ++i) {
            this.context.removeWrapperLifecycle(wrapperLifecycles[i]);
        }
        final String[] wrapperListeners = this.context.findWrapperListeners();
        for (int i = 0; i < wrapperListeners.length; ++i) {
            this.context.removeWrapperListener(wrapperListeners[i]);
        }
        if (this.antiLockingDocBase != null) {
            ExpandWar.delete(this.antiLockingDocBase, false);
        }
        this.initializerClassMap.clear();
        this.typeInitializerMap.clear();
        this.ok = true;
    }
    
    protected synchronized void destroy() {
        if (ContextConfig.log.isDebugEnabled()) {
            ContextConfig.log.debug((Object)ContextConfig.sm.getString("contextConfig.destroy"));
        }
        final Server s = this.getServer();
        if (s != null && !s.getState().isAvailable()) {
            return;
        }
        if (this.context instanceof StandardContext) {
            final String workDir = ((StandardContext)this.context).getWorkPath();
            if (workDir != null) {
                ExpandWar.delete(new File(workDir));
            }
        }
    }
    
    private Server getServer() {
        Container c;
        for (c = this.context; c != null && !(c instanceof Engine); c = c.getParent()) {}
        if (c == null) {
            return null;
        }
        final Service s = ((Engine)c).getService();
        if (s == null) {
            return null;
        }
        return s.getServer();
    }
    
    protected void validateSecurityRoles() {
        final SecurityConstraint[] constraints = this.context.findConstraints();
        for (int i = 0; i < constraints.length; ++i) {
            final String[] roles = constraints[i].findAuthRoles();
            for (int j = 0; j < roles.length; ++j) {
                if (!"*".equals(roles[j]) && !this.context.findSecurityRole(roles[j])) {
                    ContextConfig.log.warn((Object)ContextConfig.sm.getString("contextConfig.role.auth", roles[j]));
                    this.context.addSecurityRole(roles[j]);
                }
            }
        }
        final Container[] wrappers = this.context.findChildren();
        for (int k = 0; k < wrappers.length; ++k) {
            final Wrapper wrapper = (Wrapper)wrappers[k];
            final String runAs = wrapper.getRunAs();
            if (runAs != null && !this.context.findSecurityRole(runAs)) {
                ContextConfig.log.warn((Object)ContextConfig.sm.getString("contextConfig.role.runas", runAs));
                this.context.addSecurityRole(runAs);
            }
            final String[] names = wrapper.findSecurityReferences();
            for (int l = 0; l < names.length; ++l) {
                final String link = wrapper.findSecurityReference(names[l]);
                if (link != null && !this.context.findSecurityRole(link)) {
                    ContextConfig.log.warn((Object)ContextConfig.sm.getString("contextConfig.role.link", link));
                    this.context.addSecurityRole(link);
                }
            }
        }
    }
    
    @Deprecated
    protected File getConfigBase() {
        final File configBase = new File(this.getBaseDir(), "conf");
        if (!configBase.exists()) {
            return null;
        }
        return configBase;
    }
    
    protected File getHostConfigBase() {
        File file = null;
        Container container = this.context;
        Host host = null;
        Engine engine = null;
        while (container != null) {
            if (container instanceof Host) {
                host = (Host)container;
            }
            if (container instanceof Engine) {
                engine = (Engine)container;
            }
            container = container.getParent();
        }
        if (host != null && host.getXmlBase() != null) {
            final String xmlBase = host.getXmlBase();
            file = new File(xmlBase);
            if (!file.isAbsolute()) {
                file = new File(this.getBaseDir(), xmlBase);
            }
        }
        else {
            final StringBuilder result = new StringBuilder();
            if (engine != null) {
                result.append(engine.getName()).append('/');
            }
            if (host != null) {
                result.append(host.getName()).append('/');
            }
            file = new File(this.getConfigBase(), result.toString());
        }
        try {
            return file.getCanonicalFile();
        }
        catch (IOException e) {
            return file;
        }
    }
    
    protected void webConfig() {
        final Set<WebXml> defaults = new HashSet<WebXml>();
        defaults.add(this.getDefaultWebXmlFragment());
        final WebXml webXml = this.createWebXml();
        final InputSource contextWebXml = this.getContextWebXmlSource();
        this.parseWebXml(contextWebXml, webXml, false);
        final ServletContext sContext = this.context.getServletContext();
        final Map<String, WebXml> fragments = this.processJarsForWebFragments(webXml);
        Set<WebXml> orderedFragments = null;
        orderedFragments = WebXml.orderWebFragments(webXml, fragments, sContext);
        if (this.ok) {
            this.processServletContainerInitializers();
        }
        if (!webXml.isMetadataComplete() || this.typeInitializerMap.size() > 0) {
            if (this.ok) {
                NamingEnumeration<Binding> listBindings = null;
                try {
                    try {
                        listBindings = this.context.getResources().listBindings("/WEB-INF/classes");
                    }
                    catch (NameNotFoundException ex) {}
                    while (listBindings != null && listBindings.hasMoreElements()) {
                        final Binding binding = listBindings.nextElement();
                        if (binding.getObject() instanceof FileDirContext) {
                            final File webInfClassDir = new File(((FileDirContext)binding.getObject()).getDocBase());
                            this.processAnnotationsFile(webInfClassDir, webXml, webXml.isMetadataComplete());
                        }
                        else {
                            if ("META-INF".equals(binding.getName())) {
                                continue;
                            }
                            final String resource = "/WEB-INF/classes/" + binding.getName();
                            try {
                                final URL url = sContext.getResource(resource);
                                this.processAnnotationsUrl(url, webXml, webXml.isMetadataComplete());
                            }
                            catch (MalformedURLException e) {
                                ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.webinfClassesUrl", resource), (Throwable)e);
                            }
                        }
                    }
                }
                catch (NamingException e2) {
                    ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.webinfClassesUrl", "/WEB-INF/classes"), (Throwable)e2);
                }
            }
            if (this.ok) {
                this.processAnnotations(orderedFragments, webXml.isMetadataComplete());
            }
            this.javaClassCache.clear();
        }
        if (!webXml.isMetadataComplete()) {
            if (this.ok) {
                this.ok = webXml.merge(orderedFragments);
            }
            webXml.merge(defaults);
            if (this.ok) {
                this.convertJsps(webXml);
            }
            if (this.ok) {
                webXml.configureContext(this.context);
            }
        }
        else {
            webXml.merge(defaults);
            this.convertJsps(webXml);
            webXml.configureContext(this.context);
        }
        final String mergedWebXml = webXml.toXml();
        sContext.setAttribute("org.apache.tomcat.util.scan.MergedWebXml", mergedWebXml);
        if (this.context.getLogEffectiveWebXml()) {
            ContextConfig.log.info((Object)("web.xml:\n" + mergedWebXml));
        }
        if (this.ok) {
            final Set<WebXml> resourceJars = new LinkedHashSet<WebXml>();
            for (final WebXml fragment : orderedFragments) {
                resourceJars.add(fragment);
            }
            for (final WebXml fragment : fragments.values()) {
                if (!resourceJars.contains(fragment)) {
                    resourceJars.add(fragment);
                }
            }
            this.processResourceJARs(resourceJars);
        }
        if (this.ok) {
            for (final Map.Entry<ServletContainerInitializer, Set<Class<?>>> entry : this.initializerClassMap.entrySet()) {
                if (entry.getValue().isEmpty()) {
                    this.context.addServletContainerInitializer(entry.getKey(), null);
                }
                else {
                    this.context.addServletContainerInitializer(entry.getKey(), entry.getValue());
                }
            }
        }
    }
    
    private WebXml getDefaultWebXmlFragment() {
        final Host host = (Host)this.context.getParent();
        DefaultWebXmlCacheEntry entry = ContextConfig.hostWebXmlCache.get(host);
        final InputSource globalWebXml = this.getGlobalWebXmlSource();
        final InputSource hostWebXml = this.getHostWebXmlSource();
        long globalTimeStamp = 0L;
        long hostTimeStamp = 0L;
        if (globalWebXml != null) {
            URLConnection uc = null;
            try {
                final URL url = new URL(globalWebXml.getSystemId());
                uc = url.openConnection();
                globalTimeStamp = uc.getLastModified();
            }
            catch (IOException e) {
                globalTimeStamp = -1L;
                if (uc != null) {
                    try {
                        uc.getInputStream().close();
                    }
                    catch (IOException e) {
                        ExceptionUtils.handleThrowable(e);
                        globalTimeStamp = -1L;
                    }
                }
            }
            finally {
                if (uc != null) {
                    try {
                        uc.getInputStream().close();
                    }
                    catch (IOException e2) {
                        ExceptionUtils.handleThrowable(e2);
                        globalTimeStamp = -1L;
                    }
                }
            }
        }
        if (hostWebXml != null) {
            URLConnection uc = null;
            try {
                final URL url = new URL(hostWebXml.getSystemId());
                uc = url.openConnection();
                hostTimeStamp = uc.getLastModified();
            }
            catch (IOException e) {
                hostTimeStamp = -1L;
                if (uc != null) {
                    try {
                        uc.getInputStream().close();
                    }
                    catch (IOException e) {
                        ExceptionUtils.handleThrowable(e);
                        hostTimeStamp = -1L;
                    }
                }
            }
            finally {
                if (uc != null) {
                    try {
                        uc.getInputStream().close();
                    }
                    catch (IOException e3) {
                        ExceptionUtils.handleThrowable(e3);
                        hostTimeStamp = -1L;
                    }
                }
            }
        }
        if (entry != null && entry.getGlobalTimeStamp() == globalTimeStamp && entry.getHostTimeStamp() == hostTimeStamp) {
            InputSourceUtil.close(globalWebXml);
            InputSourceUtil.close(hostWebXml);
            return entry.getWebXml();
        }
        synchronized (host.getPipeline()) {
            entry = ContextConfig.hostWebXmlCache.get(host);
            if (entry != null && entry.getGlobalTimeStamp() == globalTimeStamp && entry.getHostTimeStamp() == hostTimeStamp) {
                return entry.getWebXml();
            }
            final WebXml webXmlDefaultFragment = this.createWebXml();
            webXmlDefaultFragment.setOverridable(true);
            webXmlDefaultFragment.setDistributable(true);
            webXmlDefaultFragment.setAlwaysAddWelcomeFiles(false);
            if (globalWebXml == null) {
                ContextConfig.log.info((Object)ContextConfig.sm.getString("contextConfig.defaultMissing"));
            }
            else {
                this.parseWebXml(globalWebXml, webXmlDefaultFragment, false);
            }
            webXmlDefaultFragment.setReplaceWelcomeFiles(true);
            this.parseWebXml(hostWebXml, webXmlDefaultFragment, false);
            if (globalTimeStamp != -1L && hostTimeStamp != -1L) {
                entry = new DefaultWebXmlCacheEntry(webXmlDefaultFragment, globalTimeStamp, hostTimeStamp);
                ContextConfig.hostWebXmlCache.put(host, entry);
            }
            return webXmlDefaultFragment;
        }
    }
    
    private void convertJsps(final WebXml webXml) {
        final ServletDef jspServlet = webXml.getServlets().get("jsp");
        Map<String, String> jspInitParams;
        if (jspServlet == null) {
            jspInitParams = new HashMap<String, String>();
            final Wrapper w = (Wrapper)this.context.findChild("jsp");
            if (w != null) {
                final String[] arr$;
                final String[] params = arr$ = w.findInitParameters();
                for (final String param : arr$) {
                    jspInitParams.put(param, w.findInitParameter(param));
                }
            }
        }
        else {
            jspInitParams = jspServlet.getParameterMap();
        }
        for (final ServletDef servletDef : webXml.getServlets().values()) {
            if (servletDef.getJspFile() != null) {
                this.convertJsp(servletDef, jspInitParams);
            }
        }
    }
    
    private void convertJsp(final ServletDef servletDef, final Map<String, String> jspInitParams) {
        servletDef.setServletClass("org.apache.jasper.servlet.JspServlet");
        String jspFile = servletDef.getJspFile();
        if (jspFile != null && !jspFile.startsWith("/")) {
            if (!this.context.isServlet22()) {
                throw new IllegalArgumentException(ContextConfig.sm.getString("contextConfig.jspFile.error", jspFile));
            }
            if (ContextConfig.log.isDebugEnabled()) {
                ContextConfig.log.debug((Object)ContextConfig.sm.getString("contextConfig.jspFile.warning", jspFile));
            }
            jspFile = "/" + jspFile;
        }
        servletDef.getParameterMap().put("jspFile", jspFile);
        servletDef.setJspFile(null);
        for (final Map.Entry<String, String> initParam : jspInitParams.entrySet()) {
            servletDef.addInitParameter(initParam.getKey(), initParam.getValue());
        }
    }
    
    protected WebXml createWebXml() {
        return new WebXml();
    }
    
    protected void processServletContainerInitializers() {
        List<ServletContainerInitializer> detectedScis;
        try {
            final WebappServiceLoader<ServletContainerInitializer> loader = new WebappServiceLoader<ServletContainerInitializer>(this.context);
            detectedScis = loader.load(ServletContainerInitializer.class);
        }
        catch (IOException e) {
            ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.servletContainerInitializerFail", this.context.getName()), (Throwable)e);
            this.ok = false;
            return;
        }
        for (final ServletContainerInitializer sci : detectedScis) {
            this.initializerClassMap.put(sci, new HashSet<Class<?>>());
            HandlesTypes ht;
            try {
                ht = sci.getClass().getAnnotation(HandlesTypes.class);
            }
            catch (Exception e2) {
                if (ContextConfig.log.isDebugEnabled()) {
                    ContextConfig.log.info((Object)ContextConfig.sm.getString("contextConfig.sci.debug", sci.getClass().getName()), (Throwable)e2);
                }
                else {
                    ContextConfig.log.info((Object)ContextConfig.sm.getString("contextConfig.sci.info", sci.getClass().getName()));
                }
                continue;
            }
            if (ht == null) {
                continue;
            }
            final Class<?>[] types = (Class<?>[])ht.value();
            if (types == null) {
                continue;
            }
            for (final Class<?> type : types) {
                if (type.isAnnotation()) {
                    this.handlesTypesAnnotations = true;
                }
                else {
                    this.handlesTypesNonAnnotations = true;
                }
                Set<ServletContainerInitializer> scis = this.typeInitializerMap.get(type);
                if (scis == null) {
                    scis = new HashSet<ServletContainerInitializer>();
                    this.typeInitializerMap.put(type, scis);
                }
                scis.add(sci);
            }
        }
    }
    
    protected void processResourceJARs(final Set<WebXml> fragments) {
        for (final WebXml fragment : fragments) {
            final URL url = fragment.getURL();
            Jar jar = null;
            try {
                if ("jar".equals(url.getProtocol())) {
                    jar = JarFactory.newInstance(url);
                    jar.nextEntry();
                    for (String entryName = jar.getEntryName(); entryName != null; entryName = jar.getEntryName()) {
                        if (entryName.startsWith("META-INF/resources/")) {
                            this.context.addResourceJarUrl(url);
                            break;
                        }
                        jar.nextEntry();
                    }
                }
                else if ("file".equals(url.getProtocol())) {
                    final FileDirContext fileDirContext = new FileDirContext();
                    fileDirContext.setDocBase(new File(url.toURI()).getAbsolutePath());
                    try {
                        fileDirContext.lookup("META-INF/resources/");
                        if (this.context instanceof StandardContext) {
                            ((StandardContext)this.context).addResourcesDirContext(fileDirContext);
                        }
                    }
                    catch (NamingException ex) {}
                }
            }
            catch (IOException ioe) {
                ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.resourceJarFail", url, this.context.getName()));
            }
            catch (URISyntaxException e) {
                ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.resourceJarFail", url, this.context.getName()));
            }
            finally {
                if (jar != null) {
                    jar.close();
                }
            }
        }
    }
    
    protected InputSource getGlobalWebXmlSource() {
        if (this.defaultWebXml == null && this.context instanceof StandardContext) {
            this.defaultWebXml = ((StandardContext)this.context).getDefaultWebXml();
        }
        if (this.defaultWebXml == null) {
            this.getDefaultWebXml();
        }
        if ("org/apache/catalina/startup/NO_DEFAULT_XML".equals(this.defaultWebXml)) {
            return null;
        }
        return this.getWebXmlSource(this.defaultWebXml, this.getBaseDir());
    }
    
    protected InputSource getHostWebXmlSource() {
        final File hostConfigBase = this.getHostConfigBase();
        if (!hostConfigBase.exists()) {
            return null;
        }
        return this.getWebXmlSource("web.xml.default", hostConfigBase.getPath());
    }
    
    protected InputSource getContextWebXmlSource() {
        InputStream stream = null;
        InputSource source = null;
        URL url = null;
        String altDDName = null;
        final ServletContext servletContext = this.context.getServletContext();
        try {
            if (servletContext != null) {
                altDDName = (String)servletContext.getAttribute("org.apache.catalina.deploy.alt_dd");
                if (altDDName != null) {
                    try {
                        stream = new FileInputStream(altDDName);
                        url = new File(altDDName).toURI().toURL();
                    }
                    catch (FileNotFoundException e) {
                        ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.altDDNotFound", altDDName));
                    }
                    catch (MalformedURLException e2) {
                        ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.applicationUrl"));
                    }
                }
                else {
                    stream = servletContext.getResourceAsStream("/WEB-INF/web.xml");
                    try {
                        url = servletContext.getResource("/WEB-INF/web.xml");
                    }
                    catch (MalformedURLException e2) {
                        ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.applicationUrl"));
                    }
                }
            }
            if (stream == null || url == null) {
                if (ContextConfig.log.isDebugEnabled()) {
                    ContextConfig.log.debug((Object)(ContextConfig.sm.getString("contextConfig.applicationMissing") + " " + this.context));
                }
            }
            else {
                source = new InputSource(url.toExternalForm());
                source.setByteStream(stream);
            }
        }
        finally {
            if (source == null && stream != null) {
                try {
                    stream.close();
                }
                catch (IOException ex) {}
            }
        }
        return source;
    }
    
    protected InputSource getWebXmlSource(final String filename, final String path) {
        File file = new File(filename);
        if (!file.isAbsolute()) {
            file = new File(path, filename);
        }
        InputStream stream = null;
        InputSource source = null;
        try {
            if (!file.exists()) {
                stream = this.getClass().getClassLoader().getResourceAsStream(filename);
                if (stream != null) {
                    source = new InputSource(this.getClass().getClassLoader().getResource(filename).toURI().toString());
                }
            }
            else {
                source = new InputSource(file.getAbsoluteFile().toURI().toString());
                stream = new FileInputStream(file);
            }
            if (stream != null && source != null) {
                source.setByteStream(stream);
            }
        }
        catch (Exception e) {
            ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.defaultError", filename, file), (Throwable)e);
        }
        finally {
            if (source == null && stream != null) {
                try {
                    stream.close();
                }
                catch (IOException ex) {}
            }
        }
        return source;
    }
    
    protected void parseWebXml(final InputSource source, final WebXml dest, final boolean fragment) {
        if (source == null) {
            return;
        }
        final XmlErrorHandler handler = new XmlErrorHandler();
        Digester digester;
        WebRuleSet ruleSet;
        if (fragment) {
            digester = this.webFragmentDigester;
            ruleSet = this.webFragmentRuleSet;
        }
        else {
            digester = this.webDigester;
            ruleSet = this.webRuleSet;
        }
        digester.push(dest);
        digester.setErrorHandler(handler);
        while (true) {
            if (ContextConfig.log.isDebugEnabled()) {
                ContextConfig.log.debug((Object)ContextConfig.sm.getString("contextConfig.applicationStart", source.getSystemId()));
                try {
                    digester.parse(source);
                    if (handler.getWarnings().size() > 0 || handler.getErrors().size() > 0) {
                        this.ok = false;
                        handler.logFindings(ContextConfig.log, source.getSystemId());
                    }
                }
                catch (SAXParseException e) {
                    ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.applicationParse", source.getSystemId()), (Throwable)e);
                    ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.applicationPosition", "" + e.getLineNumber(), "" + e.getColumnNumber()));
                    this.ok = false;
                }
                catch (Exception e2) {
                    ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.applicationParse", source.getSystemId()), (Throwable)e2);
                    this.ok = false;
                }
                finally {
                    digester.reset();
                    ruleSet.recycle();
                    InputSourceUtil.close(source);
                }
                return;
            }
            continue;
        }
    }
    
    protected Map<String, WebXml> processJarsForWebFragments(final WebXml application) {
        final JarScanner jarScanner = this.context.getJarScanner();
        boolean parseRequired = true;
        final Set<String> absoluteOrder = application.getAbsoluteOrdering();
        if (absoluteOrder != null && absoluteOrder.isEmpty() && !this.context.getXmlValidation()) {
            parseRequired = false;
        }
        final FragmentJarScannerCallback callback = new FragmentJarScannerCallback(parseRequired);
        jarScanner.scan(this.context.getServletContext(), this.context.getLoader().getClassLoader(), callback, ContextConfig.pluggabilityJarsToSkip);
        return callback.getFragments();
    }
    
    protected void processAnnotations(final Set<WebXml> fragments, final boolean handlesTypesOnly) {
        for (final WebXml fragment : fragments) {
            final WebXml annotations = new WebXml();
            annotations.setDistributable(true);
            final URL url = fragment.getURL();
            this.processAnnotationsUrl(url, annotations, handlesTypesOnly || fragment.isMetadataComplete());
            final Set<WebXml> set = new HashSet<WebXml>();
            set.add(annotations);
            fragment.merge(set);
        }
    }
    
    protected void processAnnotationsUrl(final URL url, final WebXml fragment, final boolean handlesTypesOnly) {
        if (url == null) {
            return;
        }
        if ("jar".equals(url.getProtocol())) {
            this.processAnnotationsJar(url, fragment, handlesTypesOnly);
        }
        else if ("jndi".equals(url.getProtocol())) {
            this.processAnnotationsJndi(url, fragment, handlesTypesOnly);
        }
        else if ("file".equals(url.getProtocol())) {
            try {
                this.processAnnotationsFile(new File(url.toURI()), fragment, handlesTypesOnly);
            }
            catch (URISyntaxException e) {
                ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.fileUrl", url), (Throwable)e);
            }
        }
        else {
            ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.unknownUrlProtocol", url.getProtocol(), url));
        }
    }
    
    protected void processAnnotationsJar(final URL url, final WebXml fragment, final boolean handlesTypesOnly) {
        Jar jar = null;
        try {
            jar = JarFactory.newInstance(url);
            if (ContextConfig.log.isDebugEnabled()) {
                ContextConfig.log.debug((Object)ContextConfig.sm.getString("contextConfig.processAnnotationsJar.debug", url));
            }
            jar.nextEntry();
            for (String entryName = jar.getEntryName(); entryName != null; entryName = jar.getEntryName()) {
                if (entryName.endsWith(".class")) {
                    InputStream is = null;
                    try {
                        is = jar.getEntryInputStream();
                        this.processAnnotationsStream(is, fragment, handlesTypesOnly);
                    }
                    catch (IOException e) {
                        ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.inputStreamJar", entryName, url), (Throwable)e);
                    }
                    catch (ClassFormatException e2) {
                        ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.inputStreamJar", entryName, url), (Throwable)e2);
                    }
                    finally {
                        if (is != null) {
                            try {
                                is.close();
                            }
                            catch (IOException ex) {}
                        }
                    }
                }
                jar.nextEntry();
            }
        }
        catch (IOException e3) {
            ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.jarFile", url), (Throwable)e3);
        }
        finally {
            if (jar != null) {
                jar.close();
            }
        }
    }
    
    protected void processAnnotationsJndi(final URL url, final WebXml fragment, final boolean handlesTypesOnly) {
        try {
            final URLConnection urlConn = url.openConnection();
            if (!(urlConn instanceof DirContextURLConnection)) {
                ContextConfig.sm.getString("contextConfig.jndiUrlNotDirContextConn", url);
                return;
            }
            final DirContextURLConnection dcUrlConn = (DirContextURLConnection)urlConn;
            dcUrlConn.setUseCaches(false);
            final String type = dcUrlConn.getHeaderField("resourcetype");
            if ("<collection/>".equals(type)) {
                final Enumeration<String> dirs = dcUrlConn.list();
                if (ContextConfig.log.isDebugEnabled() && dirs.hasMoreElements()) {
                    ContextConfig.log.debug((Object)ContextConfig.sm.getString("contextConfig.processAnnotationsWebDir.debug", url));
                }
                while (dirs.hasMoreElements()) {
                    final String dir = dirs.nextElement();
                    final URL dirUrl = new URL(url.toString() + '/' + dir);
                    this.processAnnotationsJndi(dirUrl, fragment, handlesTypesOnly);
                }
            }
            else if (url.getPath().endsWith(".class")) {
                InputStream is = null;
                try {
                    is = dcUrlConn.getInputStream();
                    this.processAnnotationsStream(is, fragment, handlesTypesOnly);
                }
                catch (IOException e) {
                    ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.inputStreamJndi", url), (Throwable)e);
                }
                catch (ClassFormatException e2) {
                    ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.inputStreamJndi", url), (Throwable)e2);
                }
                finally {
                    if (is != null) {
                        try {
                            is.close();
                        }
                        catch (Throwable t) {
                            ExceptionUtils.handleThrowable(t);
                        }
                    }
                }
            }
        }
        catch (IOException e3) {
            ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.jndiUrl", url), (Throwable)e3);
        }
    }
    
    protected void processAnnotationsFile(final File file, final WebXml fragment, final boolean handlesTypesOnly) {
        if (file.isDirectory()) {
            final String[] dirs = file.list();
            if (dirs != null) {
                if (ContextConfig.log.isDebugEnabled()) {
                    ContextConfig.log.debug((Object)ContextConfig.sm.getString("contextConfig.processAnnotationsDir.debug", file));
                }
                for (final String dir : dirs) {
                    this.processAnnotationsFile(new File(file, dir), fragment, handlesTypesOnly);
                }
            }
        }
        else if (file.getName().endsWith(".class") && file.canRead()) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
                this.processAnnotationsStream(fis, fragment, handlesTypesOnly);
            }
            catch (IOException e) {
                ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.inputStreamFile", file.getAbsolutePath()), (Throwable)e);
            }
            catch (ClassFormatException e2) {
                ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.inputStreamFile", file.getAbsolutePath()), (Throwable)e2);
            }
            finally {
                if (fis != null) {
                    try {
                        fis.close();
                    }
                    catch (Throwable t) {
                        ExceptionUtils.handleThrowable(t);
                    }
                }
            }
        }
    }
    
    protected void processAnnotationsStream(final InputStream is, final WebXml fragment, final boolean handlesTypesOnly) throws ClassFormatException, IOException {
        final ClassParser parser = new ClassParser(is);
        final JavaClass clazz = parser.parse();
        this.checkHandlesTypes(clazz);
        if (handlesTypesOnly) {
            return;
        }
        final String className = clazz.getClassName();
        final AnnotationEntry[] annotationsEntries = clazz.getAnnotationEntries();
        if (annotationsEntries != null) {
            for (final AnnotationEntry ae : annotationsEntries) {
                final String type = ae.getAnnotationType();
                if ("Ljavax/servlet/annotation/WebServlet;".equals(type)) {
                    this.processAnnotationWebServlet(className, ae, fragment);
                }
                else if ("Ljavax/servlet/annotation/WebFilter;".equals(type)) {
                    this.processAnnotationWebFilter(className, ae, fragment);
                }
                else if ("Ljavax/servlet/annotation/WebListener;".equals(type)) {
                    fragment.addListener(className);
                }
            }
        }
    }
    
    protected void checkHandlesTypes(final JavaClass javaClass) {
        if (this.typeInitializerMap.size() == 0) {
            return;
        }
        if ((javaClass.getAccessFlags() & 0x2000) != 0x0) {
            return;
        }
        final String className = javaClass.getClassName();
        Class<?> clazz = null;
        if (this.handlesTypesNonAnnotations) {
            this.populateJavaClassCache(className, javaClass);
            final JavaClassCacheEntry entry = this.javaClassCache.get(className);
            if (entry.getSciSet() == null) {
                try {
                    this.populateSCIsForCacheEntry(entry);
                }
                catch (StackOverflowError soe) {
                    throw new IllegalStateException(ContextConfig.sm.getString("contextConfig.annotationsStackOverflow", this.context.getName(), this.classHierarchyToString(className, entry)));
                }
            }
            if (!entry.getSciSet().isEmpty()) {
                clazz = Introspection.loadClass(this.context, className);
                if (clazz == null) {
                    return;
                }
                for (final ServletContainerInitializer sci : entry.getSciSet()) {
                    Set<Class<?>> classes = this.initializerClassMap.get(sci);
                    if (classes == null) {
                        classes = new HashSet<Class<?>>();
                        this.initializerClassMap.put(sci, classes);
                    }
                    classes.add(clazz);
                }
            }
        }
        if (this.handlesTypesAnnotations) {
            for (final Map.Entry<Class<?>, Set<ServletContainerInitializer>> entry2 : this.typeInitializerMap.entrySet()) {
                if (entry2.getKey().isAnnotation()) {
                    final AnnotationEntry[] annotationEntries = javaClass.getAnnotationEntries();
                    if (annotationEntries == null) {
                        continue;
                    }
                    for (final AnnotationEntry annotationEntry : annotationEntries) {
                        if (entry2.getKey().getName().equals(getClassName(annotationEntry.getAnnotationType()))) {
                            if (clazz == null) {
                                clazz = Introspection.loadClass(this.context, className);
                                if (clazz == null) {
                                    return;
                                }
                            }
                            for (final ServletContainerInitializer sci2 : entry2.getValue()) {
                                this.initializerClassMap.get(sci2).add(clazz);
                            }
                            break;
                        }
                    }
                }
            }
        }
    }
    
    private String classHierarchyToString(final String className, final JavaClassCacheEntry entry) {
        final JavaClassCacheEntry start = entry;
        final StringBuilder msg = new StringBuilder(className);
        msg.append("->");
        String parentName = entry.getSuperclassName();
        JavaClassCacheEntry parent = this.javaClassCache.get(parentName);
        for (int count = 0; count < 100 && parent != null && parent != start; ++count, parentName = parent.getSuperclassName(), parent = this.javaClassCache.get(parentName)) {
            msg.append(parentName);
            msg.append("->");
        }
        msg.append(parentName);
        return msg.toString();
    }
    
    private void populateJavaClassCache(final String className, final JavaClass javaClass) {
        if (this.javaClassCache.containsKey(className)) {
            return;
        }
        this.javaClassCache.put(className, new JavaClassCacheEntry(javaClass));
        this.populateJavaClassCache(javaClass.getSuperclassName());
        for (final String interfaceName : javaClass.getInterfaceNames()) {
            this.populateJavaClassCache(interfaceName);
        }
    }
    
    private void populateJavaClassCache(final String className) {
        if (!this.javaClassCache.containsKey(className)) {
            final String name = className.replace('.', '/') + ".class";
            final InputStream is = this.context.getLoader().getClassLoader().getResourceAsStream(name);
            if (is == null) {
                return;
            }
            final ClassParser parser = new ClassParser(is);
            try {
                final JavaClass clazz = parser.parse();
                this.populateJavaClassCache(clazz.getClassName(), clazz);
            }
            catch (ClassFormatException e) {
                ContextConfig.log.debug((Object)ContextConfig.sm.getString("contextConfig.invalidSciHandlesTypes", className), (Throwable)e);
            }
            catch (IOException e2) {
                ContextConfig.log.debug((Object)ContextConfig.sm.getString("contextConfig.invalidSciHandlesTypes", className), (Throwable)e2);
            }
            finally {
                try {
                    is.close();
                }
                catch (IOException ex) {}
            }
        }
    }
    
    private void populateSCIsForCacheEntry(final JavaClassCacheEntry cacheEntry) {
        final Set<ServletContainerInitializer> result = new HashSet<ServletContainerInitializer>();
        final String superClassName = cacheEntry.getSuperclassName();
        final JavaClassCacheEntry superClassCacheEntry = this.javaClassCache.get(superClassName);
        if (cacheEntry.equals(superClassCacheEntry)) {
            cacheEntry.setSciSet(ContextConfig.EMPTY_SCI_SET);
            return;
        }
        if (superClassCacheEntry != null) {
            if (superClassCacheEntry.getSciSet() == null) {
                this.populateSCIsForCacheEntry(superClassCacheEntry);
            }
            result.addAll(superClassCacheEntry.getSciSet());
        }
        result.addAll(this.getSCIsForClass(superClassName));
        for (final String interfaceName : cacheEntry.getInterfaceNames()) {
            final JavaClassCacheEntry interfaceEntry = this.javaClassCache.get(interfaceName);
            if (interfaceEntry != null) {
                if (interfaceEntry.getSciSet() == null) {
                    this.populateSCIsForCacheEntry(interfaceEntry);
                }
                result.addAll(interfaceEntry.getSciSet());
            }
            result.addAll(this.getSCIsForClass(interfaceName));
        }
        cacheEntry.setSciSet(result.isEmpty() ? ContextConfig.EMPTY_SCI_SET : result);
    }
    
    private Set<ServletContainerInitializer> getSCIsForClass(final String className) {
        for (final Map.Entry<Class<?>, Set<ServletContainerInitializer>> entry : this.typeInitializerMap.entrySet()) {
            final Class<?> clazz = entry.getKey();
            if (!clazz.isAnnotation() && clazz.getName().equals(className)) {
                return entry.getValue();
            }
        }
        return ContextConfig.EMPTY_SCI_SET;
    }
    
    private static final String getClassName(final String internalForm) {
        if (!internalForm.startsWith("L")) {
            return internalForm;
        }
        return internalForm.substring(1, internalForm.length() - 1).replace('/', '.');
    }
    
    protected void processAnnotationWebServlet(final String className, final AnnotationEntry ae, final WebXml fragment) {
        String servletName = null;
        final List<ElementValuePair> evps = ae.getElementValuePairs();
        for (final ElementValuePair evp : evps) {
            final String name = evp.getNameString();
            if ("name".equals(name)) {
                servletName = evp.getValue().stringifyValue();
                break;
            }
        }
        if (servletName == null) {
            servletName = className;
        }
        ServletDef servletDef = fragment.getServlets().get(servletName);
        boolean isWebXMLservletDef;
        if (servletDef == null) {
            servletDef = new ServletDef();
            servletDef.setServletName(servletName);
            servletDef.setServletClass(className);
            isWebXMLservletDef = false;
        }
        else {
            isWebXMLservletDef = true;
        }
        boolean urlPatternsSet = false;
        String[] urlPatterns = null;
        for (final ElementValuePair evp2 : evps) {
            final String name2 = evp2.getNameString();
            if ("value".equals(name2) || "urlPatterns".equals(name2)) {
                if (urlPatternsSet) {
                    throw new IllegalArgumentException(ContextConfig.sm.getString("contextConfig.urlPatternValue", "WebServlet", className));
                }
                urlPatternsSet = true;
                urlPatterns = this.processAnnotationsStringArray(evp2.getValue());
            }
            else if ("description".equals(name2)) {
                if (servletDef.getDescription() != null) {
                    continue;
                }
                servletDef.setDescription(evp2.getValue().stringifyValue());
            }
            else if ("displayName".equals(name2)) {
                if (servletDef.getDisplayName() != null) {
                    continue;
                }
                servletDef.setDisplayName(evp2.getValue().stringifyValue());
            }
            else if ("largeIcon".equals(name2)) {
                if (servletDef.getLargeIcon() != null) {
                    continue;
                }
                servletDef.setLargeIcon(evp2.getValue().stringifyValue());
            }
            else if ("smallIcon".equals(name2)) {
                if (servletDef.getSmallIcon() != null) {
                    continue;
                }
                servletDef.setSmallIcon(evp2.getValue().stringifyValue());
            }
            else if ("asyncSupported".equals(name2)) {
                if (servletDef.getAsyncSupported() != null) {
                    continue;
                }
                servletDef.setAsyncSupported(evp2.getValue().stringifyValue());
            }
            else if ("loadOnStartup".equals(name2)) {
                if (servletDef.getLoadOnStartup() != null) {
                    continue;
                }
                servletDef.setLoadOnStartup(evp2.getValue().stringifyValue());
            }
            else {
                if (!"initParams".equals(name2)) {
                    continue;
                }
                final Map<String, String> initParams = this.processAnnotationWebInitParams(evp2.getValue());
                if (isWebXMLservletDef) {
                    final Map<String, String> webXMLInitParams = servletDef.getParameterMap();
                    for (final Map.Entry<String, String> entry : initParams.entrySet()) {
                        if (webXMLInitParams.get(entry.getKey()) == null) {
                            servletDef.addInitParameter(entry.getKey(), entry.getValue());
                        }
                    }
                }
                else {
                    for (final Map.Entry<String, String> entry2 : initParams.entrySet()) {
                        servletDef.addInitParameter(entry2.getKey(), entry2.getValue());
                    }
                }
            }
        }
        if (!isWebXMLservletDef && urlPatterns != null) {
            fragment.addServlet(servletDef);
        }
        if (urlPatterns != null && !fragment.getServletMappings().containsValue(servletName)) {
            for (final String urlPattern : urlPatterns) {
                fragment.addServletMapping(urlPattern, servletName);
            }
        }
    }
    
    protected void processAnnotationWebFilter(final String className, final AnnotationEntry ae, final WebXml fragment) {
        String filterName = null;
        final List<ElementValuePair> evps = ae.getElementValuePairs();
        for (final ElementValuePair evp : evps) {
            final String name = evp.getNameString();
            if ("filterName".equals(name)) {
                filterName = evp.getValue().stringifyValue();
                break;
            }
        }
        if (filterName == null) {
            filterName = className;
        }
        FilterDef filterDef = fragment.getFilters().get(filterName);
        final FilterMap filterMap = new FilterMap();
        boolean isWebXMLfilterDef;
        if (filterDef == null) {
            filterDef = new FilterDef();
            filterDef.setFilterName(filterName);
            filterDef.setFilterClass(className);
            isWebXMLfilterDef = false;
        }
        else {
            isWebXMLfilterDef = true;
        }
        boolean urlPatternsSet = false;
        boolean servletNamesSet = false;
        boolean dispatchTypesSet = false;
        String[] urlPatterns = null;
        for (final ElementValuePair evp2 : evps) {
            final String name2 = evp2.getNameString();
            if ("value".equals(name2) || "urlPatterns".equals(name2)) {
                if (urlPatternsSet) {
                    throw new IllegalArgumentException(ContextConfig.sm.getString("contextConfig.urlPatternValue", "WebFilter", className));
                }
                urlPatterns = this.processAnnotationsStringArray(evp2.getValue());
                urlPatternsSet = (urlPatterns.length > 0);
                for (final String urlPattern : urlPatterns) {
                    filterMap.addURLPattern(urlPattern);
                }
            }
            else if ("servletNames".equals(name2)) {
                final String[] servletNames = this.processAnnotationsStringArray(evp2.getValue());
                servletNamesSet = (servletNames.length > 0);
                for (final String servletName : servletNames) {
                    filterMap.addServletName(servletName);
                }
            }
            else if ("dispatcherTypes".equals(name2)) {
                final String[] dispatcherTypes = this.processAnnotationsStringArray(evp2.getValue());
                dispatchTypesSet = (dispatcherTypes.length > 0);
                for (final String dispatcherType : dispatcherTypes) {
                    filterMap.setDispatcher(dispatcherType);
                }
            }
            else if ("description".equals(name2)) {
                if (filterDef.getDescription() != null) {
                    continue;
                }
                filterDef.setDescription(evp2.getValue().stringifyValue());
            }
            else if ("displayName".equals(name2)) {
                if (filterDef.getDisplayName() != null) {
                    continue;
                }
                filterDef.setDisplayName(evp2.getValue().stringifyValue());
            }
            else if ("largeIcon".equals(name2)) {
                if (filterDef.getLargeIcon() != null) {
                    continue;
                }
                filterDef.setLargeIcon(evp2.getValue().stringifyValue());
            }
            else if ("smallIcon".equals(name2)) {
                if (filterDef.getSmallIcon() != null) {
                    continue;
                }
                filterDef.setSmallIcon(evp2.getValue().stringifyValue());
            }
            else if ("asyncSupported".equals(name2)) {
                if (filterDef.getAsyncSupported() != null) {
                    continue;
                }
                filterDef.setAsyncSupported(evp2.getValue().stringifyValue());
            }
            else {
                if (!"initParams".equals(name2)) {
                    continue;
                }
                final Map<String, String> initParams = this.processAnnotationWebInitParams(evp2.getValue());
                if (isWebXMLfilterDef) {
                    final Map<String, String> webXMLInitParams = filterDef.getParameterMap();
                    for (final Map.Entry<String, String> entry : initParams.entrySet()) {
                        if (webXMLInitParams.get(entry.getKey()) == null) {
                            filterDef.addInitParameter(entry.getKey(), entry.getValue());
                        }
                    }
                }
                else {
                    for (final Map.Entry<String, String> entry2 : initParams.entrySet()) {
                        filterDef.addInitParameter(entry2.getKey(), entry2.getValue());
                    }
                }
            }
        }
        if (!isWebXMLfilterDef) {
            fragment.addFilter(filterDef);
            if (urlPatternsSet || servletNamesSet) {
                filterMap.setFilterName(filterName);
                fragment.addFilterMapping(filterMap);
            }
        }
        if (urlPatternsSet || dispatchTypesSet) {
            final Set<FilterMap> fmap = fragment.getFilterMappings();
            FilterMap descMap = null;
            for (final FilterMap map : fmap) {
                if (filterName.equals(map.getFilterName())) {
                    descMap = map;
                    break;
                }
            }
            if (descMap != null) {
                final String[] urlsPatterns = descMap.getURLPatterns();
                if (urlPatternsSet && (urlsPatterns == null || urlsPatterns.length == 0)) {
                    for (final String urlPattern : filterMap.getURLPatterns()) {
                        descMap.addURLPattern(urlPattern);
                    }
                }
                final String[] dispatcherNames = descMap.getDispatcherNames();
                if (dispatchTypesSet && (dispatcherNames == null || dispatcherNames.length == 0)) {
                    for (final String dis : filterMap.getDispatcherNames()) {
                        descMap.setDispatcher(dis);
                    }
                }
            }
        }
    }
    
    protected String[] processAnnotationsStringArray(final ElementValue ev) {
        final ArrayList<String> values = new ArrayList<String>();
        if (ev instanceof ArrayElementValue) {
            final ElementValue[] arr$;
            final ElementValue[] arrayValues = arr$ = ((ArrayElementValue)ev).getElementValuesArray();
            for (final ElementValue value : arr$) {
                values.add(value.stringifyValue());
            }
        }
        else {
            values.add(ev.stringifyValue());
        }
        final String[] result = new String[values.size()];
        return values.toArray(result);
    }
    
    protected Map<String, String> processAnnotationWebInitParams(final ElementValue ev) {
        final Map<String, String> result = new HashMap<String, String>();
        if (ev instanceof ArrayElementValue) {
            final ElementValue[] arr$;
            final ElementValue[] arrayValues = arr$ = ((ArrayElementValue)ev).getElementValuesArray();
            for (final ElementValue value : arr$) {
                if (value instanceof AnnotationElementValue) {
                    final List<ElementValuePair> evps = ((AnnotationElementValue)value).getAnnotationEntry().getElementValuePairs();
                    String initParamName = null;
                    String initParamValue = null;
                    for (final ElementValuePair evp : evps) {
                        if ("name".equals(evp.getNameString())) {
                            initParamName = evp.getValue().stringifyValue();
                        }
                        else {
                            if (!"value".equals(evp.getNameString())) {
                                continue;
                            }
                            initParamValue = evp.getValue().stringifyValue();
                        }
                    }
                    result.put(initParamName, initParamValue);
                }
            }
        }
        return result;
    }
    
    static {
        log = LogFactory.getLog((Class)ContextConfig.class);
        sm = StringManager.getManager("org.apache.catalina.startup");
        DUMMY_LOGIN_CONFIG = new LoginConfig("NONE", null, null, null);
        pluggabilityJarsToSkip = new HashSet<String>();
        Properties props = new Properties();
        InputStream is = null;
        try {
            is = ContextConfig.class.getClassLoader().getResourceAsStream("org/apache/catalina/startup/Authenticators.properties");
            if (is != null) {
                props.load(is);
            }
        }
        catch (IOException ioe) {
            props = null;
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                }
                catch (IOException ex) {}
            }
        }
        authenticators = props;
        addJarsToSkip("tomcat.util.scan.DefaultJarScanner.jarsToSkip");
        addJarsToSkip("org.apache.catalina.startup.ContextConfig.jarsToSkip");
        ContextConfig.deploymentCount = 0L;
        hostWebXmlCache = new ConcurrentHashMap<Host, DefaultWebXmlCacheEntry>();
        EMPTY_SCI_SET = Collections.emptySet();
    }
    
    private class FragmentJarScannerCallback implements JarScannerCallback
    {
        private static final String FRAGMENT_LOCATION = "META-INF/web-fragment.xml";
        private Map<String, WebXml> fragments;
        private final boolean parseRequired;
        
        public FragmentJarScannerCallback(final boolean parseRequired) {
            this.fragments = new HashMap<String, WebXml>();
            this.parseRequired = parseRequired;
        }
        
        @Override
        public void scan(final JarURLConnection jarConn) throws IOException {
            final URL url = jarConn.getURL();
            final URL resourceURL = jarConn.getJarFileURL();
            Jar jar = null;
            InputStream is = null;
            final WebXml fragment = new WebXml();
            try {
                jar = JarFactory.newInstance(url);
                if (this.parseRequired || ContextConfig.this.context.getXmlValidation()) {
                    is = jar.getInputStream("META-INF/web-fragment.xml");
                }
                if (is == null) {
                    fragment.setDistributable(true);
                }
                else {
                    final InputSource source = new InputSource("jar:" + resourceURL.toString() + "!/" + "META-INF/web-fragment.xml");
                    source.setByteStream(is);
                    ContextConfig.this.parseWebXml(source, fragment, true);
                }
            }
            finally {
                if (jar != null) {
                    jar.close();
                }
                fragment.setURL(url);
                if (fragment.getName() == null) {
                    fragment.setName(fragment.getURL().toString());
                }
                fragment.setJarName(this.extractJarFileName(url));
                this.fragments.put(fragment.getName(), fragment);
            }
        }
        
        private String extractJarFileName(final URL input) {
            String url = input.toString();
            if (url.endsWith("!/")) {
                url = url.substring(0, url.length() - 2);
            }
            return url.substring(url.lastIndexOf(47) + 1);
        }
        
        @Override
        public void scan(final File file) throws IOException {
            InputStream stream = null;
            final WebXml fragment = new WebXml();
            try {
                final File fragmentFile = new File(file, "META-INF/web-fragment.xml");
                if (fragmentFile.isFile()) {
                    stream = new FileInputStream(fragmentFile);
                    final InputSource source = new InputSource(fragmentFile.toURI().toURL().toString());
                    source.setByteStream(stream);
                    ContextConfig.this.parseWebXml(source, fragment, true);
                }
                else {
                    fragment.setDistributable(true);
                }
            }
            finally {
                if (stream != null) {
                    try {
                        stream.close();
                    }
                    catch (IOException ex) {}
                }
                fragment.setURL(file.toURI().toURL());
                if (fragment.getName() == null) {
                    fragment.setName(fragment.getURL().toString());
                }
                fragment.setJarName(file.getName());
                this.fragments.put(fragment.getName(), fragment);
            }
        }
        
        public Map<String, WebXml> getFragments() {
            return this.fragments;
        }
    }
    
    private static class DefaultWebXmlCacheEntry
    {
        private final WebXml webXml;
        private final long globalTimeStamp;
        private final long hostTimeStamp;
        
        public DefaultWebXmlCacheEntry(final WebXml webXml, final long globalTimeStamp, final long hostTimeStamp) {
            this.webXml = webXml;
            this.globalTimeStamp = globalTimeStamp;
            this.hostTimeStamp = hostTimeStamp;
        }
        
        public WebXml getWebXml() {
            return this.webXml;
        }
        
        public long getGlobalTimeStamp() {
            return this.globalTimeStamp;
        }
        
        public long getHostTimeStamp() {
            return this.hostTimeStamp;
        }
    }
    
    private static class JavaClassCacheEntry
    {
        public final String superclassName;
        public final String[] interfaceNames;
        private Set<ServletContainerInitializer> sciSet;
        
        public JavaClassCacheEntry(final JavaClass javaClass) {
            this.sciSet = null;
            this.superclassName = javaClass.getSuperclassName();
            this.interfaceNames = javaClass.getInterfaceNames();
        }
        
        public String getSuperclassName() {
            return this.superclassName;
        }
        
        public String[] getInterfaceNames() {
            return this.interfaceNames;
        }
        
        public Set<ServletContainerInitializer> getSciSet() {
            return this.sciSet;
        }
        
        public void setSciSet(final Set<ServletContainerInitializer> sciSet) {
            this.sciSet = sciSet;
        }
    }
}
