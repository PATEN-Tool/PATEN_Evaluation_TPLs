// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.catalina.startup;

import java.net.JarURLConnection;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.bcel.classfile.AnnotationElementValue;
import org.apache.tomcat.util.bcel.classfile.ArrayElementValue;
import org.apache.tomcat.util.bcel.classfile.ElementValue;
import org.apache.tomcat.util.bcel.classfile.ElementValuePair;
import org.apache.tomcat.util.bcel.classfile.ClassFormatException;
import org.apache.tomcat.util.bcel.classfile.AnnotationEntry;
import org.apache.tomcat.util.bcel.classfile.JavaClass;
import org.apache.tomcat.util.bcel.classfile.ClassParser;
import java.util.Enumeration;
import java.net.URLConnection;
import org.apache.naming.resources.DirContextURLConnection;
import java.net.URISyntaxException;
import org.apache.tomcat.JarScanner;
import org.apache.tomcat.JarScannerCallback;
import javax.servlet.ServletContext;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.apache.tomcat.util.scan.Jar;
import javax.servlet.annotation.HandlesTypes;
import java.io.FileInputStream;
import org.apache.tomcat.util.scan.JarFactory;
import org.apache.catalina.deploy.ServletDef;
import java.util.Iterator;
import java.util.LinkedHashSet;
import org.apache.catalina.deploy.WebXml;
import java.util.HashSet;
import org.apache.catalina.Engine;
import org.apache.catalina.Wrapper;
import org.apache.catalina.deploy.FilterMap;
import org.apache.catalina.deploy.FilterDef;
import org.apache.catalina.deploy.ErrorPage;
import java.util.Locale;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.util.ContextName;
import org.apache.catalina.Host;
import org.xml.sax.SAXParseException;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.File;
import org.apache.catalina.Container;
import org.apache.catalina.core.StandardEngine;
import java.util.ArrayList;
import java.util.List;
import org.apache.tomcat.util.digester.RuleSet;
import org.apache.catalina.Pipeline;
import java.io.InputStream;
import org.apache.catalina.deploy.SecurityConstraint;
import org.apache.tomcat.util.ExceptionUtils;
import java.io.IOException;
import org.apache.catalina.Valve;
import org.apache.catalina.core.ContainerBase;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.LifecycleEvent;
import java.util.HashMap;
import java.util.LinkedHashMap;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.res.StringManager;
import java.util.Set;
import javax.servlet.ServletContainerInitializer;
import org.apache.catalina.Context;
import java.util.Properties;
import org.apache.catalina.Authenticator;
import java.util.Map;
import org.apache.juli.logging.Log;
import org.apache.catalina.LifecycleListener;

public class ContextConfig implements LifecycleListener
{
    private static final Log log;
    private static final String SCI_LOCATION = "META-INF/services/javax.servlet.ServletContainerInitializer";
    protected Map<String, Authenticator> customAuthenticators;
    protected static Properties authenticators;
    protected Context context;
    protected String defaultContextXml;
    protected String defaultWebXml;
    protected boolean ok;
    protected String originalDocBase;
    protected Map<ServletContainerInitializer, Set<Class<?>>> initializerClassMap;
    protected Map<Class<?>, Set<ServletContainerInitializer>> typeInitializerMap;
    protected static final StringManager sm;
    protected static Digester contextDigester;
    protected Digester webDigester;
    protected Digester webFragmentDigester;
    protected static Digester[] webDigesters;
    protected static Digester[] webFragmentDigesters;
    protected static WebRuleSet webRuleSet;
    protected static WebRuleSet webFragmentRuleSet;
    protected static long deploymentCount;
    protected static final LoginConfig DUMMY_LOGIN_CONFIG;
    
    public ContextConfig() {
        this.context = null;
        this.defaultContextXml = null;
        this.defaultWebXml = null;
        this.ok = false;
        this.originalDocBase = null;
        this.initializerClassMap = new LinkedHashMap<ServletContainerInitializer, Set<Class<?>>>();
        this.typeInitializerMap = new HashMap<Class<?>, Set<ServletContainerInitializer>>();
        this.webDigester = null;
        this.webFragmentDigester = null;
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
    
    public String getDefaultContextXml() {
        if (this.defaultContextXml == null) {
            this.defaultContextXml = "conf/context.xml";
        }
        return this.defaultContextXml;
    }
    
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
                final String docBase = this.context.getDocBase();
                this.context.setDocBase(this.originalDocBase);
                this.originalDocBase = docBase;
            }
        }
        else if (event.getType().equals("configure_stop")) {
            if (this.originalDocBase != null) {
                final String docBase = this.context.getDocBase();
                this.context.setDocBase(this.originalDocBase);
                this.originalDocBase = docBase;
            }
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
    
    protected synchronized void authenticatorConfig() {
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
                try {
                    final InputStream is = this.getClass().getClassLoader().getResourceAsStream("org/apache/catalina/startup/Authenticators.properties");
                    if (is == null) {
                        ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.authenticatorResources"));
                        this.ok = false;
                        return;
                    }
                    (ContextConfig.authenticators = new Properties()).load(is);
                }
                catch (IOException e) {
                    ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.authenticatorResources"), (Throwable)e);
                    this.ok = false;
                    return;
                }
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
        if (!namespaceAware && !validation) {
            if (ContextConfig.webDigesters[0] == null) {
                ContextConfig.webDigesters[0] = DigesterFactory.newDigester(validation, namespaceAware, ContextConfig.webRuleSet);
                ContextConfig.webFragmentDigesters[0] = DigesterFactory.newDigester(validation, namespaceAware, ContextConfig.webFragmentRuleSet);
            }
            this.webDigester = ContextConfig.webDigesters[0];
            this.webFragmentDigester = ContextConfig.webFragmentDigesters[0];
        }
        else if (!namespaceAware && validation) {
            if (ContextConfig.webDigesters[1] == null) {
                ContextConfig.webDigesters[1] = DigesterFactory.newDigester(validation, namespaceAware, ContextConfig.webRuleSet);
                ContextConfig.webFragmentDigesters[1] = DigesterFactory.newDigester(validation, namespaceAware, ContextConfig.webFragmentRuleSet);
            }
            this.webDigester = ContextConfig.webDigesters[1];
            this.webFragmentDigester = ContextConfig.webFragmentDigesters[1];
        }
        else if (namespaceAware && !validation) {
            if (ContextConfig.webDigesters[2] == null) {
                ContextConfig.webDigesters[2] = DigesterFactory.newDigester(validation, namespaceAware, ContextConfig.webRuleSet);
                ContextConfig.webFragmentDigesters[2] = DigesterFactory.newDigester(validation, namespaceAware, ContextConfig.webFragmentRuleSet);
            }
            this.webDigester = ContextConfig.webDigesters[2];
            this.webFragmentDigester = ContextConfig.webFragmentDigesters[2];
        }
        else {
            if (ContextConfig.webDigesters[3] == null) {
                ContextConfig.webDigesters[3] = DigesterFactory.newDigester(validation, namespaceAware, ContextConfig.webRuleSet);
                ContextConfig.webFragmentDigesters[3] = DigesterFactory.newDigester(validation, namespaceAware, ContextConfig.webFragmentRuleSet);
            }
            this.webDigester = ContextConfig.webDigesters[3];
            this.webFragmentDigester = ContextConfig.webFragmentDigesters[3];
        }
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
    
    protected void contextConfig() {
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
                    this.processContextConfig(defaultContextUrl);
                }
                catch (MalformedURLException e) {
                    ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.badUrl", defaultContextFile), (Throwable)e);
                }
            }
            final File hostContextFile = new File(this.getConfigBase(), this.getHostConfigPath("context.xml.default"));
            if (hostContextFile.exists()) {
                try {
                    final URL hostContextUrl = hostContextFile.toURI().toURL();
                    this.processContextConfig(hostContextUrl);
                }
                catch (MalformedURLException e2) {
                    ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.badUrl", hostContextFile), (Throwable)e2);
                }
            }
        }
        if (this.context.getConfigFile() != null) {
            this.processContextConfig(this.context.getConfigFile());
        }
    }
    
    protected void processContextConfig(final URL contextXml) {
        if (ContextConfig.log.isDebugEnabled()) {
            ContextConfig.log.debug((Object)("Processing context [" + this.context.getName() + "] configuration file [" + contextXml + "]"));
        }
        InputSource source = null;
        InputStream stream = null;
        try {
            source = new InputSource(contextXml.toString());
            stream = contextXml.openStream();
            if ("file".equals(contextXml.getProtocol())) {
                this.context.addWatchedResource(new File(contextXml.toURI()).getAbsolutePath());
            }
        }
        catch (Exception e) {
            ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.contextMissing", contextXml), (Throwable)e);
        }
        if (source == null) {
            return;
        }
        synchronized (ContextConfig.contextDigester) {
            try {
                source.setByteStream(stream);
                ContextConfig.contextDigester.setClassLoader(this.getClass().getClassLoader());
                ContextConfig.contextDigester.setUseContextClassLoader(false);
                ContextConfig.contextDigester.push(this.context.getParent());
                ContextConfig.contextDigester.push(this.context);
                final XmlErrorHandler errorHandler = new XmlErrorHandler();
                ContextConfig.contextDigester.setErrorHandler(errorHandler);
                ContextConfig.contextDigester.parse(source);
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
            catch (Exception e3) {
                ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.contextParse", this.context.getName()), (Throwable)e3);
                this.ok = false;
            }
            finally {
                ContextConfig.contextDigester.reset();
                try {
                    if (stream != null) {
                        stream.close();
                    }
                }
                catch (IOException e4) {
                    ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.contextClose"), (Throwable)e4);
                }
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
            canonicalAppBase = new File(System.getProperty("catalina.base"), appBase).getCanonicalFile();
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
            unpackWARs = (((StandardHost)host).isUnpackWARs() && ((StandardContext)this.context).getUnpackWAR() && docBase.startsWith(canonicalAppBase.getPath()));
        }
        if (docBase.toLowerCase(Locale.ENGLISH).endsWith(".war") && !file.isDirectory() && unpackWARs) {
            final URL war = new URL("jar:" + new File(docBase).toURI().toURL() + "!/");
            docBase = ExpandWar.expand(host, war, pathName);
            file = new File(docBase);
            docBase = file.getCanonicalPath();
            if (this.context instanceof StandardContext) {
                ((StandardContext)this.context).setOriginalDocBase(origDocBase);
            }
        }
        else if (docBase.toLowerCase(Locale.ENGLISH).endsWith(".war") && !file.isDirectory() && !unpackWARs) {
            final URL war = new URL("jar:" + new File(docBase).toURI().toURL() + "!/");
            ExpandWar.validate(host, war, pathName);
        }
        else {
            final File docDir = new File(docBase);
            if (!docDir.exists()) {
                final File warFile = new File(docBase + ".war");
                if (warFile.exists()) {
                    final URL war2 = new URL("jar:" + warFile.toURI().toURL() + "!/");
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
            if (this.originalDocBase == null) {
                this.originalDocBase = docBase;
            }
            else {
                docBase = this.originalDocBase;
            }
            File docBaseFile = new File(docBase);
            if (!docBaseFile.isAbsolute()) {
                File file = new File(appBase);
                if (!file.isAbsolute()) {
                    file = new File(System.getProperty("catalina.base"), appBase);
                }
                docBaseFile = new File(file, docBase);
            }
            final String path = this.context.getPath();
            if (path == null) {
                return;
            }
            final ContextName cn = new ContextName(path, this.context.getWebappVersion());
            docBase = cn.getBaseName();
            File file2 = null;
            if (docBase.toLowerCase(Locale.ENGLISH).endsWith(".war")) {
                file2 = new File(System.getProperty("java.io.tmpdir"), ContextConfig.deploymentCount++ + "-" + docBase + ".war");
            }
            else {
                file2 = new File(System.getProperty("java.io.tmpdir"), ContextConfig.deploymentCount++ + "-" + docBase);
            }
            if (ContextConfig.log.isDebugEnabled()) {
                ContextConfig.log.debug((Object)("Anti locking context[" + this.context.getName() + "] setting docBase to " + file2));
            }
            ExpandWar.delete(file2);
            if (ExpandWar.copy(docBaseFile, file2)) {
                this.context.setDocBase(file2.getAbsolutePath());
            }
        }
    }
    
    protected void init() {
        if (ContextConfig.contextDigester == null) {
            (ContextConfig.contextDigester = this.createContextDigester()).getParser();
        }
        if (ContextConfig.log.isDebugEnabled()) {
            ContextConfig.log.debug((Object)ContextConfig.sm.getString("contextConfig.init"));
        }
        this.context.setConfigured(false);
        this.ok = true;
        this.contextConfig();
        try {
            this.fixDocBase();
        }
        catch (IOException e) {
            ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.fixDocBase", this.context.getName()), (Throwable)e);
        }
    }
    
    protected synchronized void beforeStart() {
        this.antiLocking();
    }
    
    protected synchronized void configureStart() {
        if (ContextConfig.log.isDebugEnabled()) {
            ContextConfig.log.debug((Object)ContextConfig.sm.getString("contextConfig.start"));
        }
        if (ContextConfig.log.isDebugEnabled()) {
            ContextConfig.log.debug((Object)ContextConfig.sm.getString("contextConfig.xmlSettings", this.context.getName(), this.context.getXmlValidation(), this.context.getXmlNamespaceAware()));
        }
        this.createWebXmlDigester(this.context.getXmlNamespaceAware(), this.context.getXmlValidation());
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
        final Host host = (Host)this.context.getParent();
        final String appBase = host.getAppBase();
        final String docBase = this.context.getDocBase();
        if (docBase != null && this.originalDocBase != null) {
            File docBaseFile = new File(docBase);
            if (!docBaseFile.isAbsolute()) {
                docBaseFile = new File(appBase, docBase);
            }
            ExpandWar.delete(docBaseFile, false);
        }
        this.initializerClassMap.clear();
        this.typeInitializerMap.clear();
        this.ok = true;
    }
    
    protected synchronized void destroy() {
        if (ContextConfig.log.isDebugEnabled()) {
            ContextConfig.log.debug((Object)ContextConfig.sm.getString("contextConfig.destroy"));
        }
        final String workDir = ((StandardContext)this.context).getWorkPath();
        if (workDir != null) {
            ExpandWar.delete(new File(workDir));
        }
    }
    
    protected void validateSecurityRoles() {
        final SecurityConstraint[] constraints = this.context.findConstraints();
        for (int i = 0; i < constraints.length; ++i) {
            final String[] roles = constraints[i].findAuthRoles();
            for (int j = 0; j < roles.length; ++j) {
                if (!"*".equals(roles[j]) && !this.context.findSecurityRole(roles[j])) {
                    ContextConfig.log.info((Object)ContextConfig.sm.getString("contextConfig.role.auth", roles[j]));
                    this.context.addSecurityRole(roles[j]);
                }
            }
        }
        final Container[] wrappers = this.context.findChildren();
        for (int k = 0; k < wrappers.length; ++k) {
            final Wrapper wrapper = (Wrapper)wrappers[k];
            final String runAs = wrapper.getRunAs();
            if (runAs != null && !this.context.findSecurityRole(runAs)) {
                ContextConfig.log.info((Object)ContextConfig.sm.getString("contextConfig.role.runas", runAs));
                this.context.addSecurityRole(runAs);
            }
            final String[] names = wrapper.findSecurityReferences();
            for (int l = 0; l < names.length; ++l) {
                final String link = wrapper.findSecurityReference(names[l]);
                if (link != null && !this.context.findSecurityRole(link)) {
                    ContextConfig.log.info((Object)ContextConfig.sm.getString("contextConfig.role.link", link));
                    this.context.addSecurityRole(link);
                }
            }
        }
    }
    
    protected File getConfigBase() {
        final File configBase = new File(System.getProperty("catalina.base"), "conf");
        if (!configBase.exists()) {
            return null;
        }
        return configBase;
    }
    
    protected String getHostConfigPath(final String resourceName) {
        final StringBuilder result = new StringBuilder();
        Container container = this.context;
        Container host = null;
        Container engine = null;
        while (container != null) {
            if (container instanceof Host) {
                host = container;
            }
            if (container instanceof Engine) {
                engine = container;
            }
            container = container.getParent();
        }
        if (engine != null) {
            result.append(engine.getName()).append('/');
        }
        if (host != null) {
            result.append(host.getName()).append('/');
        }
        result.append(resourceName);
        return result.toString();
    }
    
    protected void webConfig() {
        final WebXml webXml = this.createWebXml();
        final WebXml webXmlDefaultFragment = this.createWebXml();
        webXmlDefaultFragment.setOverridable(true);
        webXmlDefaultFragment.setDistributable(true);
        webXmlDefaultFragment.setAlwaysAddWelcomeFiles(false);
        final InputSource globalWebXml = this.getGlobalWebXmlSource();
        if (globalWebXml == null) {
            ContextConfig.log.info((Object)ContextConfig.sm.getString("contextConfig.defaultMissing"));
        }
        else {
            this.parseWebXml(globalWebXml, webXmlDefaultFragment, false);
        }
        webXmlDefaultFragment.setReplaceWelcomeFiles(true);
        final InputSource hostWebXml = this.getHostWebXmlSource();
        this.parseWebXml(hostWebXml, webXmlDefaultFragment, false);
        final Set<WebXml> defaults = new HashSet<WebXml>();
        defaults.add(webXmlDefaultFragment);
        final InputSource contextWebXml = this.getContextWebXmlSource();
        this.parseWebXml(contextWebXml, webXml, false);
        if (webXml.getMajorVersion() >= 3) {
            final Map<String, WebXml> fragments = this.processJarsForWebFragments();
            Set<WebXml> orderedFragments = null;
            if (!webXml.isMetadataComplete()) {
                orderedFragments = WebXml.orderWebFragments(webXml, fragments);
                if (this.ok) {
                    this.processServletContainerInitializers(orderedFragments);
                }
                if (this.ok) {
                    try {
                        final URL webinfClasses = this.context.getServletContext().getResource("/WEB-INF/classes");
                        this.processAnnotationsUrl(webinfClasses, webXml);
                    }
                    catch (MalformedURLException e) {
                        ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.webinfClassesUrl"), (Throwable)e);
                    }
                }
                if (this.ok) {
                    this.processAnnotations(orderedFragments);
                }
                if (this.ok) {
                    this.ok = webXml.merge(orderedFragments);
                }
                webXml.merge(defaults);
                this.convertJsps(webXml);
                if (this.ok) {
                    webXml.configureContext(this.context);
                    final String mergedWebXml = webXml.toXml();
                    this.context.getServletContext().setAttribute("org.apache.tomcat.util.scan.MergedWebXml", mergedWebXml);
                    if (this.context.getLogEffectiveWebXml()) {
                        ContextConfig.log.info((Object)("web.xml:\n" + mergedWebXml));
                    }
                }
            }
            else {
                webXml.merge(defaults);
                webXml.configureContext(this.context);
            }
            if (this.ok) {
                final Set<WebXml> resourceJars = new LinkedHashSet<WebXml>();
                if (orderedFragments != null) {
                    for (final WebXml fragment : orderedFragments) {
                        resourceJars.add(fragment);
                    }
                }
                for (final WebXml fragment : fragments.values()) {
                    if (!resourceJars.contains(fragment)) {
                        resourceJars.add(fragment);
                    }
                }
                this.processResourceJARs(resourceJars);
            }
            if (!webXml.isMetadataComplete() && this.ok) {
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
        else {
            webXml.merge(defaults);
            this.convertJsps(webXml);
            webXml.configureContext(this.context);
        }
    }
    
    private void convertJsps(final WebXml webXml) {
        final ServletDef jspServlet = webXml.getServlets().get("jsp");
        for (final ServletDef servletDef : webXml.getServlets().values()) {
            if (servletDef.getJspFile() != null) {
                this.convertJsp(servletDef, jspServlet);
            }
        }
    }
    
    private void convertJsp(final ServletDef servletDef, final ServletDef jspServletDef) {
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
        for (final Map.Entry<String, String> initParam : jspServletDef.getParameterMap().entrySet()) {
            servletDef.addInitParameter(initParam.getKey(), initParam.getValue());
        }
    }
    
    protected WebXml createWebXml() {
        return new WebXml();
    }
    
    protected void processServletContainerInitializers(final Set<WebXml> fragments) {
        for (final WebXml fragment : fragments) {
            final URL url = fragment.getURL();
            Jar jar = null;
            InputStream is = null;
            ServletContainerInitializer sci = null;
            try {
                if ("jar".equals(url.getProtocol())) {
                    jar = JarFactory.newInstance(url);
                    is = jar.getInputStream("META-INF/services/javax.servlet.ServletContainerInitializer");
                }
                else if ("file".equals(url.getProtocol())) {
                    final String path = url.getPath();
                    final File file = new File(path, "META-INF/services/javax.servlet.ServletContainerInitializer");
                    if (file.exists()) {
                        is = new FileInputStream(file);
                    }
                }
                if (is != null) {
                    sci = this.getServletContainerInitializer(is);
                }
            }
            catch (IOException ioe) {
                ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.servletContainerInitializerFail", url, this.context.getName()));
                this.ok = false;
                return;
            }
            finally {
                if (is != null) {
                    try {
                        is.close();
                    }
                    catch (IOException ex) {}
                }
                if (jar != null) {
                    jar.close();
                }
            }
            if (sci == null) {
                continue;
            }
            this.initializerClassMap.put(sci, new HashSet<Class<?>>());
            final HandlesTypes ht = sci.getClass().getAnnotation(HandlesTypes.class);
            if (ht == null) {
                continue;
            }
            final Class<?>[] types = (Class<?>[])ht.value();
            if (types == null) {
                continue;
            }
            for (final Class<?> type : types) {
                Set<ServletContainerInitializer> scis = this.typeInitializerMap.get(type);
                if (scis == null) {
                    scis = new HashSet<ServletContainerInitializer>();
                    this.typeInitializerMap.put(type, scis);
                }
                scis.add(sci);
            }
        }
    }
    
    protected ServletContainerInitializer getServletContainerInitializer(final InputStream is) throws IOException {
        String className = null;
        if (is != null) {
            String line = null;
            try {
                final BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                line = br.readLine();
                if (line != null && line.trim().length() > 0) {
                    className = line.trim();
                }
            }
            catch (UnsupportedEncodingException ex) {}
        }
        ServletContainerInitializer sci = null;
        try {
            final Class<?> clazz = Class.forName(className, true, this.context.getLoader().getClassLoader());
            sci = (ServletContainerInitializer)clazz.newInstance();
        }
        catch (ClassNotFoundException e) {
            ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.invalidSci", className), (Throwable)e);
            throw new IOException(e);
        }
        catch (InstantiationException e2) {
            ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.invalidSci", className), (Throwable)e2);
            throw new IOException(e2);
        }
        catch (IllegalAccessException e3) {
            ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.invalidSci", className), (Throwable)e3);
            throw new IOException(e3);
        }
        return sci;
    }
    
    protected void processResourceJARs(final Set<WebXml> fragments) {
        for (final WebXml fragment : fragments) {
            final URL url = fragment.getURL();
            Jar jar = null;
            try {
                if (!"jar".equals(url.getProtocol())) {
                    continue;
                }
                jar = JarFactory.newInstance(url);
                if (!jar.entryExists("META-INF/resources/")) {
                    continue;
                }
                this.context.addResourceJarUrl(url);
            }
            catch (IOException ioe) {
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
        return this.getWebXmlSource(this.defaultWebXml, this.getBaseDir());
    }
    
    protected InputSource getHostWebXmlSource() {
        final String resourceName = this.getHostConfigPath("web.xml.default");
        final File configBase = this.getConfigBase();
        if (configBase == null) {
            return null;
        }
        String basePath = null;
        try {
            basePath = configBase.getCanonicalPath();
        }
        catch (IOException e) {
            ContextConfig.log.error((Object)ContextConfig.sm.getString("contectConfig.baseError"), (Throwable)e);
            return null;
        }
        return this.getWebXmlSource(resourceName, basePath);
    }
    
    protected InputSource getContextWebXmlSource() {
        InputStream stream = null;
        InputSource source = null;
        URL url = null;
        String altDDName = null;
        final ServletContext servletContext = this.context.getServletContext();
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
                    source = new InputSource(this.getClass().getClassLoader().getResource(filename).toString());
                }
            }
            else {
                source = new InputSource("file://" + file.getAbsolutePath());
                stream = new FileInputStream(file);
                this.context.addWatchedResource(file.getAbsolutePath());
            }
            if (stream != null && source != null) {
                source.setByteStream(stream);
            }
        }
        catch (Exception e) {
            ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.defaultError", filename, file), (Throwable)e);
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
            ruleSet = ContextConfig.webFragmentRuleSet;
        }
        else {
            digester = this.webDigester;
            ruleSet = ContextConfig.webRuleSet;
        }
        synchronized (ruleSet) {
            digester.push(dest);
            digester.setErrorHandler(handler);
            Label_0101: {
                if (!ContextConfig.log.isDebugEnabled()) {
                    break Label_0101;
                }
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
                }
            }
        }
    }
    
    protected Map<String, WebXml> processJarsForWebFragments() {
        final JarScanner jarScanner = this.context.getJarScanner();
        final FragmentJarScannerCallback callback = new FragmentJarScannerCallback();
        jarScanner.scan(this.context.getServletContext(), this.context.getLoader().getClassLoader(), callback, null);
        return callback.getFragments();
    }
    
    protected void processAnnotations(final Set<WebXml> fragments) {
        for (final WebXml fragment : fragments) {
            if (!fragment.isMetadataComplete()) {
                final WebXml annotations = new WebXml();
                annotations.setDistributable(true);
                final URL url = fragment.getURL();
                this.processAnnotationsUrl(url, annotations);
                final Set<WebXml> set = new HashSet<WebXml>();
                set.add(annotations);
                fragment.merge(set);
            }
        }
    }
    
    protected void processAnnotationsUrl(final URL url, final WebXml fragment) {
        if (url == null) {
            return;
        }
        if ("jar".equals(url.getProtocol())) {
            this.processAnnotationsJar(url, fragment);
        }
        else if ("jndi".equals(url.getProtocol())) {
            this.processAnnotationsJndi(url, fragment);
        }
        else if ("file".equals(url.getProtocol())) {
            try {
                this.processAnnotationsFile(new File(url.toURI()), fragment);
            }
            catch (URISyntaxException e) {
                ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.fileUrl", url), (Throwable)e);
            }
        }
        else {
            ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.unknownUrlProtocol", url.getProtocol(), url));
        }
    }
    
    protected void processAnnotationsJar(final URL url, final WebXml fragment) {
        Jar jar = null;
        try {
            jar = JarFactory.newInstance(url);
            jar.nextEntry();
            for (String entryName = jar.getEntryName(); entryName != null; entryName = jar.getEntryName()) {
                if (entryName.endsWith(".class")) {
                    InputStream is = null;
                    try {
                        is = jar.getEntryInputStream();
                        this.processAnnotationsStream(is, fragment);
                    }
                    catch (IOException e) {
                        ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.inputStreamJar", entryName, url), (Throwable)e);
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
        catch (IOException e2) {
            ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.jarFile", url), (Throwable)e2);
        }
        finally {
            if (jar != null) {
                jar.close();
            }
        }
    }
    
    protected void processAnnotationsJndi(final URL url, final WebXml fragment) {
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
                while (dirs.hasMoreElements()) {
                    final String dir = dirs.nextElement();
                    final URL dirUrl = new URL(url.toString() + '/' + dir);
                    this.processAnnotationsJndi(dirUrl, fragment);
                }
            }
            else if (url.getPath().endsWith(".class")) {
                InputStream is = null;
                try {
                    is = dcUrlConn.getInputStream();
                    this.processAnnotationsStream(is, fragment);
                }
                catch (IOException e) {
                    ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.inputStreamJndi", url), (Throwable)e);
                    if (is != null) {
                        try {
                            is.close();
                        }
                        catch (Throwable t) {
                            ExceptionUtils.handleThrowable(t);
                        }
                    }
                }
                finally {
                    if (is != null) {
                        try {
                            is.close();
                        }
                        catch (Throwable t2) {
                            ExceptionUtils.handleThrowable(t2);
                        }
                    }
                }
            }
        }
        catch (IOException e2) {
            ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.jndiUrl", url), (Throwable)e2);
        }
    }
    
    protected void processAnnotationsFile(final File file, final WebXml fragment) {
        if (file.isDirectory()) {
            final String[] arr$;
            final String[] dirs = arr$ = file.list();
            for (final String dir : arr$) {
                this.processAnnotationsFile(new File(file, dir), fragment);
            }
        }
        else if (file.canRead() && file.getName().endsWith(".class")) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
                this.processAnnotationsStream(fis, fragment);
            }
            catch (IOException e) {
                ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.inputStreamFile", file.getAbsolutePath()), (Throwable)e);
                if (fis != null) {
                    try {
                        fis.close();
                    }
                    catch (Throwable t) {
                        ExceptionUtils.handleThrowable(t);
                    }
                }
            }
            finally {
                if (fis != null) {
                    try {
                        fis.close();
                    }
                    catch (Throwable t2) {
                        ExceptionUtils.handleThrowable(t2);
                    }
                }
            }
        }
    }
    
    protected void processAnnotationsStream(final InputStream is, final WebXml fragment) throws ClassFormatException, IOException {
        final ClassParser parser = new ClassParser(is, null);
        final JavaClass clazz = parser.parse();
        this.checkHandlesTypes(clazz);
        final String className = clazz.getClassName();
        final AnnotationEntry[] arr$;
        final AnnotationEntry[] annotationsEntries = arr$ = clazz.getAnnotationEntries();
        for (final AnnotationEntry ae : arr$) {
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
    
    protected void checkHandlesTypes(final JavaClass javaClass) {
        if (this.typeInitializerMap.size() == 0) {
            return;
        }
        final String className = javaClass.getClassName();
        Class<?> clazz = null;
        try {
            clazz = this.context.getLoader().getClassLoader().loadClass(className);
        }
        catch (NoClassDefFoundError e) {
            ContextConfig.log.debug((Object)ContextConfig.sm.getString("contextConfig.invalidSciHandlesTypes", className), (Throwable)e);
            return;
        }
        catch (ClassNotFoundException e2) {
            ContextConfig.log.warn((Object)ContextConfig.sm.getString("contextConfig.invalidSciHandlesTypes", className), (Throwable)e2);
            return;
        }
        catch (ClassFormatError e3) {
            ContextConfig.log.warn((Object)ContextConfig.sm.getString("contextConfig.invalidSciHandlesTypes", className), (Throwable)e3);
            return;
        }
        if (clazz.isAnnotation()) {
            return;
        }
        boolean match = false;
        for (final Map.Entry<Class<?>, Set<ServletContainerInitializer>> entry : this.typeInitializerMap.entrySet()) {
            if (entry.getKey().isAnnotation()) {
                final AnnotationEntry[] arr$;
                final AnnotationEntry[] annotationEntries = arr$ = javaClass.getAnnotationEntries();
                for (final AnnotationEntry annotationEntry : arr$) {
                    if (entry.getKey().getName().equals(getClassName(annotationEntry.getAnnotationType()))) {
                        match = true;
                        break;
                    }
                }
            }
            else if (entry.getKey().isAssignableFrom(clazz)) {
                match = true;
            }
            if (match) {
                for (final ServletContainerInitializer sci : entry.getValue()) {
                    this.initializerClassMap.get(sci).add(clazz);
                }
            }
        }
    }
    
    private static final String getClassName(final String internalForm) {
        if (!internalForm.startsWith("L")) {
            return internalForm;
        }
        return internalForm.substring(1, internalForm.length() - 1).replace('/', '.');
    }
    
    protected void processAnnotationWebServlet(final String className, final AnnotationEntry ae, final WebXml fragment) {
        String servletName = null;
        final ElementValuePair[] arr$;
        final ElementValuePair[] evps = arr$ = ae.getElementValuePairs();
        for (final ElementValuePair evp : arr$) {
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
                    throw new IllegalArgumentException(ContextConfig.sm.getString("contextConfig.urlPatternValue", className));
                }
                urlPatternsSet = true;
                urlPatterns = this.processAnnotationsStringArray(evp2.getValue());
            }
            else if ("description".equals(name2)) {
                if (servletDef.getDescription() == null) {
                    servletDef.setDescription(evp2.getValue().stringifyValue());
                }
            }
            else if ("displayName".equals(name2)) {
                if (servletDef.getDisplayName() == null) {
                    servletDef.setDisplayName(evp2.getValue().stringifyValue());
                }
            }
            else if ("largeIcon".equals(name2)) {
                if (servletDef.getLargeIcon() == null) {
                    servletDef.setLargeIcon(evp2.getValue().stringifyValue());
                }
            }
            else if ("smallIcon".equals(name2)) {
                if (servletDef.getSmallIcon() == null) {
                    servletDef.setSmallIcon(evp2.getValue().stringifyValue());
                }
            }
            else if ("asyncSupported".equals(name2)) {
                if (servletDef.getAsyncSupported() == null) {
                    servletDef.setAsyncSupported(evp2.getValue().stringifyValue());
                }
            }
            else if ("loadOnStartup".equals(name2)) {
                if (servletDef.getLoadOnStartup() == null) {
                    servletDef.setLoadOnStartup(evp2.getValue().stringifyValue());
                }
            }
            else if ("initParams".equals(name2)) {
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
        final ElementValuePair[] arr$;
        final ElementValuePair[] evps = arr$ = ae.getElementValuePairs();
        for (final ElementValuePair evp : arr$) {
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
        boolean dispatchTypesSet = false;
        String[] urlPatterns = null;
        for (final ElementValuePair evp2 : evps) {
            final String name2 = evp2.getNameString();
            if ("value".equals(name2) || "urlPatterns".equals(name2)) {
                if (urlPatternsSet) {
                    throw new IllegalArgumentException(ContextConfig.sm.getString("contextConfig.urlPatternValue", className));
                }
                urlPatterns = this.processAnnotationsStringArray(evp2.getValue());
                urlPatternsSet = (urlPatterns.length > 0);
                for (final String urlPattern : urlPatterns) {
                    filterMap.addURLPattern(urlPattern);
                }
            }
            else if ("servletNames".equals(name2)) {
                final String[] arr$4;
                final String[] servletNames = arr$4 = this.processAnnotationsStringArray(evp2.getValue());
                for (final String servletName : arr$4) {
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
                if (filterDef.getDescription() == null) {
                    filterDef.setDescription(evp2.getValue().stringifyValue());
                }
            }
            else if ("displayName".equals(name2)) {
                if (filterDef.getDisplayName() == null) {
                    filterDef.setDisplayName(evp2.getValue().stringifyValue());
                }
            }
            else if ("largeIcon".equals(name2)) {
                if (filterDef.getLargeIcon() == null) {
                    filterDef.setLargeIcon(evp2.getValue().stringifyValue());
                }
            }
            else if ("smallIcon".equals(name2)) {
                if (filterDef.getSmallIcon() == null) {
                    filterDef.setSmallIcon(evp2.getValue().stringifyValue());
                }
            }
            else if ("asyncSupported".equals(name2)) {
                if (filterDef.getAsyncSupported() == null) {
                    filterDef.setAsyncSupported(evp2.getValue().stringifyValue());
                }
            }
            else if ("initParams".equals(name2)) {
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
            filterMap.setFilterName(filterName);
            fragment.addFilterMapping(filterMap);
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
                    for (final String urlPattern2 : filterMap.getURLPatterns()) {
                        descMap.addURLPattern(urlPattern2);
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
                    final ElementValuePair[] evps = ((AnnotationElementValue)value).getAnnotationEntry().getElementValuePairs();
                    String initParamName = null;
                    String initParamValue = null;
                    for (final ElementValuePair evp : evps) {
                        if ("name".equals(evp.getNameString())) {
                            initParamName = evp.getValue().stringifyValue();
                        }
                        else if ("value".equals(evp.getNameString())) {
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
        ContextConfig.authenticators = null;
        sm = StringManager.getManager("org.apache.catalina.startup");
        ContextConfig.contextDigester = null;
        ContextConfig.webDigesters = new Digester[4];
        ContextConfig.webFragmentDigesters = new Digester[4];
        ContextConfig.webRuleSet = new WebRuleSet(false);
        ContextConfig.webFragmentRuleSet = new WebRuleSet(true);
        ContextConfig.deploymentCount = 0L;
        DUMMY_LOGIN_CONFIG = new LoginConfig("NONE", null, null, null);
    }
    
    private class FragmentJarScannerCallback implements JarScannerCallback
    {
        private static final String FRAGMENT_LOCATION = "META-INF/web-fragment.xml";
        private Map<String, WebXml> fragments;
        
        private FragmentJarScannerCallback() {
            this.fragments = new HashMap<String, WebXml>();
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
                is = jar.getInputStream("META-INF/web-fragment.xml");
                if (is == null) {
                    fragment.setDistributable(true);
                }
                else {
                    final InputSource source = new InputSource(resourceURL.toString() + "!/" + "META-INF/web-fragment.xml");
                    source.setByteStream(is);
                    ContextConfig.this.parseWebXml(source, fragment, true);
                }
            }
            finally {
                if (is != null) {
                    try {
                        is.close();
                    }
                    catch (IOException ex) {}
                }
                if (jar != null) {
                    jar.close();
                }
                fragment.setURL(url);
                if (fragment.getName() == null) {
                    fragment.setName(fragment.getURL().toString());
                }
                this.fragments.put(fragment.getName(), fragment);
            }
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
            }
            finally {
                if (stream != null) {
                    try {
                        stream.close();
                    }
                    catch (Throwable t) {
                        ExceptionUtils.handleThrowable(t);
                    }
                }
                fragment.setURL(file.toURI().toURL());
                if (fragment.getName() == null) {
                    fragment.setName(fragment.getURL().toString());
                }
                this.fragments.put(fragment.getName(), fragment);
            }
        }
        
        public Map<String, WebXml> getFragments() {
            return this.fragments;
        }
    }
}
