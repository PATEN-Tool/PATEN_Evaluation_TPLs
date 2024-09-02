// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.catalina.startup;

import java.util.Collections;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.bcel.classfile.AnnotationElementValue;
import org.apache.tomcat.util.bcel.classfile.ArrayElementValue;
import org.apache.tomcat.util.bcel.classfile.ElementValue;
import org.apache.tomcat.util.bcel.classfile.ElementValuePair;
import org.apache.catalina.util.Introspection;
import org.apache.tomcat.util.bcel.classfile.AnnotationEntry;
import org.apache.tomcat.util.bcel.classfile.JavaClass;
import org.apache.tomcat.util.bcel.classfile.ClassParser;
import org.apache.tomcat.util.bcel.classfile.ClassFormatException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.apache.tomcat.JarScanner;
import org.apache.tomcat.JarScannerCallback;
import org.apache.tomcat.JarScanType;
import org.apache.tomcat.util.descriptor.web.FragmentJarScannerCallback;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import org.apache.tomcat.Jar;
import org.apache.catalina.WebResourceRoot;
import org.apache.tomcat.util.scan.JarFactory;
import javax.servlet.annotation.HandlesTypes;
import org.apache.tomcat.util.descriptor.InputSourceUtil;
import java.net.URISyntaxException;
import javax.servlet.SessionCookieConfig;
import org.apache.tomcat.util.descriptor.web.SessionConfig;
import org.apache.tomcat.util.descriptor.web.MultipartDef;
import org.apache.tomcat.util.descriptor.web.JspPropertyGroup;
import javax.servlet.MultipartConfigElement;
import org.apache.tomcat.util.descriptor.web.SecurityRoleRef;
import org.apache.tomcat.util.descriptor.web.ServletDef;
import org.apache.tomcat.util.descriptor.web.ContextService;
import org.apache.tomcat.util.descriptor.web.ContextResource;
import org.apache.tomcat.util.descriptor.web.ContextResourceEnvRef;
import org.apache.tomcat.util.descriptor.web.MessageDestinationRef;
import org.apache.tomcat.util.descriptor.web.ContextEnvironment;
import org.apache.tomcat.util.descriptor.web.ContextEjb;
import org.apache.tomcat.util.descriptor.web.ContextLocalEjb;
import org.apache.catalina.WebResource;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Iterator;
import javax.servlet.ServletContext;
import java.util.Collection;
import java.util.LinkedHashSet;
import org.apache.tomcat.util.descriptor.web.WebXml;
import java.util.HashSet;
import org.apache.tomcat.util.descriptor.web.WebXmlParser;
import org.apache.catalina.Wrapper;
import org.apache.catalina.Service;
import org.apache.catalina.Server;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.ErrorPage;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.apache.tomcat.util.buf.UriUtil;
import java.util.Locale;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.util.ContextName;
import java.net.URLConnection;
import org.xml.sax.SAXParseException;
import org.xml.sax.ErrorHandler;
import org.apache.tomcat.util.descriptor.XmlErrorHandler;
import org.xml.sax.InputSource;
import java.net.URL;
import org.apache.tomcat.util.file.ConfigurationSource;
import java.io.InputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.io.FileWriter;
import org.apache.tomcat.util.file.ConfigFileLoader;
import org.apache.catalina.Engine;
import org.apache.catalina.Container;
import org.apache.tomcat.util.digester.RuleSet;
import java.util.ArrayList;
import java.util.List;
import org.apache.tomcat.util.digester.Digester;
import org.apache.catalina.Pipeline;
import org.apache.tomcat.util.ExceptionUtils;
import org.apache.catalina.Valve;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.LifecycleEvent;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.io.File;
import org.apache.catalina.Context;
import org.apache.catalina.Authenticator;
import javax.servlet.ServletContainerInitializer;
import java.util.Set;
import org.apache.catalina.Host;
import java.util.Map;
import java.util.Properties;
import org.apache.tomcat.util.descriptor.web.LoginConfig;
import org.apache.tomcat.util.res.StringManager;
import org.apache.juli.logging.Log;
import org.apache.catalina.LifecycleListener;

public class ContextConfig implements LifecycleListener
{
    private static final Log log;
    protected static final StringManager sm;
    protected static final LoginConfig DUMMY_LOGIN_CONFIG;
    protected static final Properties authenticators;
    protected static long deploymentCount;
    protected static final Map<Host, DefaultWebXmlCacheEntry> hostWebXmlCache;
    private static final Set<ServletContainerInitializer> EMPTY_SCI_SET;
    protected Map<String, Authenticator> customAuthenticators;
    protected volatile Context context;
    protected String defaultWebXml;
    protected boolean ok;
    protected String originalDocBase;
    private File antiLockingDocBase;
    protected final Map<ServletContainerInitializer, Set<Class<?>>> initializerClassMap;
    protected final Map<Class<?>, Set<ServletContainerInitializer>> typeInitializerMap;
    protected boolean handlesTypesAnnotations;
    protected boolean handlesTypesNonAnnotations;
    
    public ContextConfig() {
        this.context = null;
        this.defaultWebXml = null;
        this.ok = false;
        this.originalDocBase = null;
        this.antiLockingDocBase = null;
        this.initializerClassMap = new LinkedHashMap<ServletContainerInitializer, Set<Class<?>>>();
        this.typeInitializerMap = new HashMap<Class<?>, Set<ServletContainerInitializer>>();
        this.handlesTypesAnnotations = false;
        this.handlesTypesNonAnnotations = false;
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
    
    public void setCustomAuthenticators(final Map<String, Authenticator> customAuthenticators) {
        this.customAuthenticators = customAuthenticators;
    }
    
    @Override
    public void lifecycleEvent(final LifecycleEvent event) {
        try {
            this.context = (Context)event.getLifecycle();
        }
        catch (ClassCastException e) {
            ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.cce", new Object[] { event.getLifecycle() }), (Throwable)e);
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
        if (loginConfig == null) {
            loginConfig = ContextConfig.DUMMY_LOGIN_CONFIG;
            this.context.setLoginConfig(loginConfig);
        }
        if (this.context.getAuthenticator() != null) {
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
            final String authenticatorName = ContextConfig.authenticators.getProperty(loginConfig.getAuthMethod());
            if (authenticatorName == null) {
                ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.authenticatorMissing", new Object[] { loginConfig.getAuthMethod() }));
                this.ok = false;
                return;
            }
            try {
                final Class<?> authenticatorClass = Class.forName(authenticatorName);
                authenticator = (Valve)authenticatorClass.getConstructor((Class<?>[])new Class[0]).newInstance(new Object[0]);
            }
            catch (Throwable t) {
                ExceptionUtils.handleThrowable(t);
                ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.authenticatorInstantiate", new Object[] { authenticatorName }), t);
                this.ok = false;
            }
        }
        if (authenticator != null) {
            final Pipeline pipeline = this.context.getPipeline();
            if (pipeline != null) {
                pipeline.addValve(authenticator);
                if (ContextConfig.log.isDebugEnabled()) {
                    ContextConfig.log.debug((Object)ContextConfig.sm.getString("contextConfig.authenticatorConfigured", new Object[] { loginConfig.getAuthMethod() }));
                }
            }
        }
    }
    
    protected Digester createContextDigester() {
        final Digester digester = new Digester();
        digester.setValidating(false);
        digester.setRulesValidation(true);
        final Map<Class<?>, List<String>> fakeAttributes = new HashMap<Class<?>, List<String>>();
        final List<String> objectAttrs = new ArrayList<String>();
        objectAttrs.add("className");
        fakeAttributes.put(Object.class, objectAttrs);
        final List<String> contextAttrs = new ArrayList<String>();
        contextAttrs.add("source");
        fakeAttributes.put(StandardContext.class, contextAttrs);
        digester.setFakeAttributes((Map)fakeAttributes);
        final RuleSet contextRuleSet = (RuleSet)new ContextRuleSet("", false);
        digester.addRuleSet(contextRuleSet);
        final RuleSet namingRuleSet = (RuleSet)new NamingRuleSet("Context/");
        digester.addRuleSet(namingRuleSet);
        return digester;
    }
    
    protected boolean getGenerateCode() {
        final Catalina catalina = Container.getService(this.context).getServer().getCatalina();
        return catalina != null && catalina.getGenerateCode();
    }
    
    protected boolean getUseGeneratedCode() {
        final Catalina catalina = Container.getService(this.context).getServer().getCatalina();
        return catalina != null && catalina.getUseGeneratedCode();
    }
    
    protected File getGeneratedCodeLocation() {
        final Catalina catalina = Container.getService(this.context).getServer().getCatalina();
        if (catalina != null) {
            return catalina.getGeneratedCodeLocation();
        }
        return null;
    }
    
    protected String getGeneratedCodePackage() {
        final Catalina catalina = Container.getService(this.context).getServer().getCatalina();
        if (catalina != null) {
            return catalina.getGeneratedCodePackage();
        }
        return "generatedCodePackage";
    }
    
    protected static String getContextXmlPackageName(final String generatedCodePackage, Container container) {
        final StringBuilder result = new StringBuilder();
        Container host = null;
        Container engine = null;
        while (container != null) {
            if (container instanceof Host) {
                host = container;
            }
            else if (container instanceof Engine) {
                engine = container;
            }
            container = container.getParent();
        }
        result.append(generatedCodePackage);
        if (engine != null) {
            result.append('.');
        }
        if (engine != null) {
            result.append(engine.getName());
            if (host != null) {
                result.append('.');
            }
        }
        if (host != null) {
            result.append(host.getName());
        }
        return result.toString();
    }
    
    protected File getContextXmlJavaSource(final String contextXmlPackageName, final String contextXmlSimpleClassName) {
        final File generatedSourceFolder = this.getGeneratedCodeLocation();
        final String path = contextXmlPackageName.replace('.', File.separatorChar);
        final File packageFolder = new File(generatedSourceFolder, path);
        if (packageFolder.isDirectory() || packageFolder.mkdirs()) {
            return new File(packageFolder, contextXmlSimpleClassName + ".java");
        }
        return null;
    }
    
    protected void generateClassHeader(final Digester digester, final String packageName, final String resourceName) {
        final StringBuilder code = digester.getGeneratedCode();
        code.append("package ").append(packageName).append(';').append(System.lineSeparator());
        code.append("public class ").append(resourceName).append(" implements ");
        code.append(ContextXml.class.getName().replace('$', '.'));
        code.append(" {").append(System.lineSeparator());
        code.append("public void load(");
        code.append(Context.class.getName());
        final String contextArgument = digester.toVariableName((Object)this.context);
        code.append(' ').append(contextArgument).append(") {").append(System.lineSeparator());
        digester.setKnown((Object)this.context);
        code.append(this.context.getClass().getName()).append(' ').append(digester.toVariableName((Object)this.context));
        code.append(" = (").append(this.context.getClass().getName()).append(") ").append(contextArgument);
        code.append(';').append(System.lineSeparator());
    }
    
    protected void generateClassFooter(final Digester digester) {
        final StringBuilder code = digester.getGeneratedCode();
        code.append('}').append(System.lineSeparator());
        code.append('}').append(System.lineSeparator());
    }
    
    protected void contextConfig(final Digester digester) {
        String defaultContextXml = null;
        final boolean generateCode = this.getGenerateCode();
        final boolean useGeneratedCode = this.getUseGeneratedCode();
        String contextXmlPackageName = null;
        String contextXmlSimpleClassName = null;
        String contextXmlClassName = null;
        File contextXmlJavaSource = null;
        if (this.context instanceof StandardContext) {
            defaultContextXml = ((StandardContext)this.context).getDefaultContextXml();
        }
        if (defaultContextXml == null) {
            defaultContextXml = "conf/context.xml";
        }
        ContextXml contextXml = null;
        if (!this.context.getOverride()) {
            if (useGeneratedCode || generateCode) {
                contextXmlPackageName = this.getGeneratedCodePackage();
                contextXmlSimpleClassName = "ContextXmlDefault";
                contextXmlClassName = contextXmlPackageName + "." + contextXmlSimpleClassName;
            }
            if (useGeneratedCode) {
                contextXml = (ContextXml)Digester.loadGeneratedClass(contextXmlClassName);
            }
            if (contextXml != null) {
                contextXml.load(this.context);
                contextXml = null;
            }
            else if (!useGeneratedCode) {
                try (final ConfigurationSource.Resource contextXmlResource = ConfigFileLoader.getSource().getResource(defaultContextXml)) {
                    if (generateCode) {
                        contextXmlJavaSource = this.getContextXmlJavaSource(contextXmlPackageName, contextXmlSimpleClassName);
                        digester.startGeneratingCode();
                        this.generateClassHeader(digester, contextXmlPackageName, contextXmlSimpleClassName);
                    }
                    final URL defaultContextUrl = contextXmlResource.getURI().toURL();
                    this.processContextConfig(digester, defaultContextUrl, contextXmlResource.getInputStream());
                    if (generateCode) {
                        this.generateClassFooter(digester);
                        try (final FileWriter writer = new FileWriter(contextXmlJavaSource)) {
                            writer.write(digester.getGeneratedCode().toString());
                        }
                        digester.endGeneratingCode();
                        Digester.addGeneratedClass(contextXmlClassName);
                    }
                }
                catch (MalformedURLException e) {
                    ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.badUrl", new Object[] { defaultContextXml }), (Throwable)e);
                }
                catch (IOException ex) {}
            }
            if (useGeneratedCode || generateCode) {
                contextXmlPackageName = getContextXmlPackageName(this.getGeneratedCodePackage(), this.context);
                contextXmlSimpleClassName = "ContextXmlDefault";
                contextXmlClassName = contextXmlPackageName + "." + contextXmlSimpleClassName;
            }
            if (useGeneratedCode) {
                contextXml = (ContextXml)Digester.loadGeneratedClass(contextXmlClassName);
            }
            if (contextXml != null) {
                contextXml.load(this.context);
                contextXml = null;
            }
            else if (!useGeneratedCode) {
                final String hostContextFile = Container.getConfigPath(this.context, "context.xml.default");
                try (final ConfigurationSource.Resource contextXmlResource2 = ConfigFileLoader.getSource().getResource(hostContextFile)) {
                    if (generateCode) {
                        contextXmlJavaSource = this.getContextXmlJavaSource(contextXmlPackageName, contextXmlSimpleClassName);
                        digester.startGeneratingCode();
                        this.generateClassHeader(digester, contextXmlPackageName, contextXmlSimpleClassName);
                    }
                    final URL defaultContextUrl2 = contextXmlResource2.getURI().toURL();
                    this.processContextConfig(digester, defaultContextUrl2, contextXmlResource2.getInputStream());
                    if (generateCode) {
                        this.generateClassFooter(digester);
                        try (final FileWriter writer2 = new FileWriter(contextXmlJavaSource)) {
                            writer2.write(digester.getGeneratedCode().toString());
                        }
                        digester.endGeneratingCode();
                        Digester.addGeneratedClass(contextXmlClassName);
                    }
                }
                catch (MalformedURLException e2) {
                    ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.badUrl", new Object[] { hostContextFile }), (Throwable)e2);
                }
                catch (IOException ex2) {}
            }
        }
        if (this.context.getConfigFile() != null) {
            if (useGeneratedCode || generateCode) {
                contextXmlPackageName = getContextXmlPackageName(this.getGeneratedCodePackage(), this.context);
                contextXmlSimpleClassName = "ContextXml_" + this.context.getName().replace('/', '_').replace("-", "__");
                contextXmlClassName = contextXmlPackageName + "." + contextXmlSimpleClassName;
            }
            if (useGeneratedCode) {
                contextXml = (ContextXml)Digester.loadGeneratedClass(contextXmlClassName);
            }
            if (contextXml != null) {
                contextXml.load(this.context);
                contextXml = null;
            }
            else if (!useGeneratedCode) {
                if (generateCode) {
                    contextXmlJavaSource = this.getContextXmlJavaSource(contextXmlPackageName, contextXmlSimpleClassName);
                    digester.startGeneratingCode();
                    this.generateClassHeader(digester, contextXmlPackageName, contextXmlSimpleClassName);
                }
                this.processContextConfig(digester, this.context.getConfigFile(), null);
                if (generateCode) {
                    this.generateClassFooter(digester);
                    try (final FileWriter writer3 = new FileWriter(contextXmlJavaSource)) {
                        writer3.write(digester.getGeneratedCode().toString());
                    }
                    catch (IOException ex3) {}
                    digester.endGeneratingCode();
                    Digester.addGeneratedClass(contextXmlClassName);
                }
            }
        }
    }
    
    protected void processContextConfig(final Digester digester, final URL contextXml, InputStream stream) {
        if (ContextConfig.log.isDebugEnabled()) {
            ContextConfig.log.debug((Object)("Processing context [" + this.context.getName() + "] configuration file [" + contextXml + "]"));
        }
        InputSource source = null;
        try {
            source = new InputSource(contextXml.toString());
            if (stream == null) {
                final URLConnection xmlConn = contextXml.openConnection();
                xmlConn.setUseCaches(false);
                stream = xmlConn.getInputStream();
            }
        }
        catch (Exception e) {
            ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.contextMissing", new Object[] { contextXml }), (Throwable)e);
        }
        if (source == null) {
            return;
        }
        try {
            source.setByteStream(stream);
            digester.setClassLoader(this.getClass().getClassLoader());
            digester.setUseContextClassLoader(false);
            digester.push((Object)this.context.getParent());
            digester.push((Object)this.context);
            final XmlErrorHandler errorHandler = new XmlErrorHandler();
            digester.setErrorHandler((ErrorHandler)errorHandler);
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
            ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.contextParse", new Object[] { this.context.getName() }), (Throwable)e2);
            ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.defaultPosition", new Object[] { "" + e2.getLineNumber(), "" + e2.getColumnNumber() }));
            this.ok = false;
        }
        catch (Exception e) {
            ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.contextParse", new Object[] { this.context.getName() }), (Throwable)e);
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
        final File appBase = host.getAppBaseFile();
        String docBaseConfigured = this.context.getDocBase();
        if (docBaseConfigured == null) {
            final String path = this.context.getPath();
            if (path == null) {
                return;
            }
            final ContextName cn = new ContextName(path, this.context.getWebappVersion());
            docBaseConfigured = cn.getBaseName();
        }
        final File docBaseConfiguredFile = new File(docBaseConfigured);
        String docBaseAbsolute;
        if (!docBaseConfiguredFile.isAbsolute()) {
            docBaseAbsolute = new File(appBase, docBaseConfigured).getAbsolutePath();
        }
        else {
            docBaseAbsolute = docBaseConfiguredFile.getAbsolutePath();
        }
        File docBaseAbsoluteFile = new File(docBaseAbsolute);
        final String originalDocBase = docBaseAbsolute;
        final ContextName cn2 = new ContextName(this.context.getPath(), this.context.getWebappVersion());
        final String pathName = cn2.getBaseName();
        boolean unpackWARs = true;
        if (host instanceof StandardHost) {
            unpackWARs = ((StandardHost)host).isUnpackWARs();
            if (unpackWARs && this.context instanceof StandardContext) {
                unpackWARs = ((StandardContext)this.context).getUnpackWAR();
            }
        }
        final boolean docBaseAbsoluteInAppBase = docBaseAbsolute.startsWith(appBase.getPath() + File.separatorChar);
        if (docBaseAbsolute.toLowerCase(Locale.ENGLISH).endsWith(".war") && !docBaseAbsoluteFile.isDirectory()) {
            final URL war = UriUtil.buildJarUrl(docBaseAbsoluteFile);
            if (unpackWARs) {
                docBaseAbsolute = ExpandWar.expand(host, war, pathName);
                docBaseAbsoluteFile = new File(docBaseAbsolute);
                if (this.context instanceof StandardContext) {
                    ((StandardContext)this.context).setOriginalDocBase(originalDocBase);
                }
            }
            else {
                ExpandWar.validate(host, war, pathName);
            }
        }
        else {
            final File docBaseAbsoluteFileWar = new File(docBaseAbsolute + ".war");
            URL war2 = null;
            if (docBaseAbsoluteFileWar.exists() && docBaseAbsoluteInAppBase) {
                war2 = UriUtil.buildJarUrl(docBaseAbsoluteFileWar);
            }
            if (docBaseAbsoluteFile.exists()) {
                if (war2 != null && unpackWARs) {
                    ExpandWar.expand(host, war2, pathName);
                }
            }
            else {
                if (war2 != null) {
                    if (unpackWARs) {
                        docBaseAbsolute = ExpandWar.expand(host, war2, pathName);
                        docBaseAbsoluteFile = new File(docBaseAbsolute);
                    }
                    else {
                        docBaseAbsoluteFile = docBaseAbsoluteFileWar;
                        ExpandWar.validate(host, war2, pathName);
                    }
                }
                if (this.context instanceof StandardContext) {
                    ((StandardContext)this.context).setOriginalDocBase(originalDocBase);
                }
            }
        }
        final String docBaseCanonical = docBaseAbsoluteFile.getCanonicalPath();
        final boolean docBaseCanonicalInAppBase = docBaseAbsoluteFile.getCanonicalFile().toPath().startsWith(appBase.toPath());
        String docBase;
        if (docBaseCanonicalInAppBase) {
            docBase = docBaseCanonical.substring(appBase.getPath().length());
            docBase = docBase.replace(File.separatorChar, '/');
            if (docBase.startsWith("/")) {
                docBase = docBase.substring(1);
            }
        }
        else {
            docBase = docBaseCanonical.replace(File.separatorChar, '/');
        }
        this.context.setDocBase(docBase);
    }
    
    protected void antiLocking() {
        if (this.context instanceof StandardContext && ((StandardContext)this.context).getAntiResourceLocking()) {
            final Host host = (Host)this.context.getParent();
            String docBase = this.context.getDocBase();
            if (docBase == null) {
                return;
            }
            this.originalDocBase = docBase;
            File docBaseFile = new File(docBase);
            if (!docBaseFile.isAbsolute()) {
                docBaseFile = new File(host.getAppBaseFile(), docBase);
            }
            final String path = this.context.getPath();
            if (path == null) {
                return;
            }
            final ContextName cn = new ContextName(path, this.context.getWebappVersion());
            docBase = cn.getBaseName();
            final String tmp = System.getProperty("java.io.tmpdir");
            final File tmpFile = new File(tmp);
            if (!tmpFile.isDirectory()) {
                ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.noAntiLocking", new Object[] { tmp, this.context.getName() }));
                return;
            }
            if (this.originalDocBase.toLowerCase(Locale.ENGLISH).endsWith(".war")) {
                this.antiLockingDocBase = new File(tmpFile, ContextConfig.deploymentCount++ + "-" + docBase + ".war");
            }
            else {
                this.antiLockingDocBase = new File(tmpFile, ContextConfig.deploymentCount++ + "-" + docBase);
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
    
    protected synchronized void init() {
        Digester contextDigester = null;
        if (!this.getUseGeneratedCode()) {
            contextDigester = this.createContextDigester();
            contextDigester.getParser();
        }
        if (ContextConfig.log.isDebugEnabled()) {
            ContextConfig.log.debug((Object)ContextConfig.sm.getString("contextConfig.init"));
        }
        this.context.setConfigured(false);
        this.ok = true;
        this.contextConfig(contextDigester);
    }
    
    protected synchronized void beforeStart() {
        try {
            this.fixDocBase();
        }
        catch (IOException e) {
            ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.fixDocBase", new Object[] { this.context.getName() }), (Throwable)e);
        }
        this.antiLocking();
    }
    
    protected synchronized void configureStart() {
        if (ContextConfig.log.isDebugEnabled()) {
            ContextConfig.log.debug((Object)ContextConfig.sm.getString("contextConfig.start"));
        }
        if (ContextConfig.log.isDebugEnabled()) {
            ContextConfig.log.debug((Object)ContextConfig.sm.getString("contextConfig.xmlSettings", new Object[] { this.context.getName(), this.context.getXmlValidation(), this.context.getXmlNamespaceAware() }));
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
        if (ContextConfig.log.isDebugEnabled()) {
            ContextConfig.log.debug((Object)"Pipeline Configuration:");
            final Pipeline pipeline = this.context.getPipeline();
            Valve[] valves = null;
            if (pipeline != null) {
                valves = pipeline.getValves();
            }
            if (valves != null) {
                for (final Valve valve : valves) {
                    ContextConfig.log.debug((Object)("  " + valve.getClass().getName()));
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
        final SecurityConstraint[] constraints2;
        final SecurityConstraint[] constraints = constraints2 = this.context.findConstraints();
        for (final SecurityConstraint constraint : constraints2) {
            final String[] authRoles;
            final String[] roles = authRoles = constraint.findAuthRoles();
            for (final String role : authRoles) {
                if (!"*".equals(role) && !this.context.findSecurityRole(role)) {
                    ContextConfig.log.warn((Object)ContextConfig.sm.getString("contextConfig.role.auth", new Object[] { role }));
                    this.context.addSecurityRole(role);
                }
            }
        }
        final Container[] children;
        final Container[] wrappers = children = this.context.findChildren();
        for (final Container container : children) {
            final Wrapper wrapper = (Wrapper)container;
            final String runAs = wrapper.getRunAs();
            if (runAs != null && !this.context.findSecurityRole(runAs)) {
                ContextConfig.log.warn((Object)ContextConfig.sm.getString("contextConfig.role.runas", new Object[] { runAs }));
                this.context.addSecurityRole(runAs);
            }
            final String[] securityReferences;
            final String[] names = securityReferences = wrapper.findSecurityReferences();
            for (final String name : securityReferences) {
                final String link = wrapper.findSecurityReference(name);
                if (link != null && !this.context.findSecurityRole(link)) {
                    ContextConfig.log.warn((Object)ContextConfig.sm.getString("contextConfig.role.link", new Object[] { link }));
                    this.context.addSecurityRole(link);
                }
            }
        }
    }
    
    protected File getHostConfigBase() {
        File file = null;
        if (this.context.getParent() instanceof Host) {
            file = ((Host)this.context.getParent()).getConfigBaseFile();
        }
        return file;
    }
    
    protected void webConfig() {
        final WebXmlParser webXmlParser = new WebXmlParser(this.context.getXmlNamespaceAware(), this.context.getXmlValidation(), this.context.getXmlBlockExternal());
        final Set<WebXml> defaults = new HashSet<WebXml>();
        defaults.add(this.getDefaultWebXmlFragment(webXmlParser));
        final Set<WebXml> tomcatWebXml = new HashSet<WebXml>();
        tomcatWebXml.add(this.getTomcatWebXmlFragment(webXmlParser));
        final WebXml webXml = this.createWebXml();
        final InputSource contextWebXml = this.getContextWebXmlSource();
        if (!webXmlParser.parseWebXml(contextWebXml, webXml, false)) {
            this.ok = false;
        }
        final ServletContext sContext = this.context.getServletContext();
        final Map<String, WebXml> fragments = this.processJarsForWebFragments(webXml, webXmlParser);
        Set<WebXml> orderedFragments = null;
        orderedFragments = (Set<WebXml>)WebXml.orderWebFragments(webXml, (Map)fragments, sContext);
        if (this.ok) {
            this.processServletContainerInitializers();
        }
        if (!webXml.isMetadataComplete() || this.typeInitializerMap.size() > 0) {
            this.processClasses(webXml, orderedFragments);
        }
        if (!webXml.isMetadataComplete()) {
            if (this.ok) {
                this.ok = webXml.merge((Set)orderedFragments);
            }
            webXml.merge((Set)tomcatWebXml);
            webXml.merge((Set)defaults);
            if (this.ok) {
                this.convertJsps(webXml);
            }
            if (this.ok) {
                this.configureContext(webXml);
            }
        }
        else {
            webXml.merge((Set)tomcatWebXml);
            webXml.merge((Set)defaults);
            this.convertJsps(webXml);
            this.configureContext(webXml);
        }
        if (this.context.getLogEffectiveWebXml()) {
            ContextConfig.log.info((Object)ContextConfig.sm.getString("contextConfig.effectiveWebXml", new Object[] { webXml.toXml() }));
        }
        if (this.ok) {
            final Set<WebXml> resourceJars = new LinkedHashSet<WebXml>(orderedFragments);
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
    
    protected void processClasses(final WebXml webXml, final Set<WebXml> orderedFragments) {
        Map<String, JavaClassCacheEntry> javaClassCache;
        if (this.context.isParallelAnnotationScanning()) {
            javaClassCache = new ConcurrentHashMap<String, JavaClassCacheEntry>();
        }
        else {
            javaClassCache = new HashMap<String, JavaClassCacheEntry>();
        }
        if (this.ok) {
            final WebResource[] listResources;
            final WebResource[] webResources = listResources = this.context.getResources().listResources("/WEB-INF/classes");
            for (final WebResource webResource : listResources) {
                if (!"META-INF".equals(webResource.getName())) {
                    this.processAnnotationsWebResource(webResource, webXml, webXml.isMetadataComplete(), javaClassCache);
                }
            }
        }
        if (this.ok) {
            this.processAnnotations(orderedFragments, webXml.isMetadataComplete(), javaClassCache);
        }
        javaClassCache.clear();
    }
    
    private void configureContext(final WebXml webxml) {
        this.context.setPublicId(webxml.getPublicId());
        this.context.setEffectiveMajorVersion(webxml.getMajorVersion());
        this.context.setEffectiveMinorVersion(webxml.getMinorVersion());
        for (final Map.Entry<String, String> entry : webxml.getContextParams().entrySet()) {
            this.context.addParameter(entry.getKey(), entry.getValue());
        }
        this.context.setDenyUncoveredHttpMethods(webxml.getDenyUncoveredHttpMethods());
        this.context.setDisplayName(webxml.getDisplayName());
        this.context.setDistributable(webxml.isDistributable());
        for (final ContextLocalEjb ejbLocalRef : webxml.getEjbLocalRefs().values()) {
            this.context.getNamingResources().addLocalEjb(ejbLocalRef);
        }
        for (final ContextEjb ejbRef : webxml.getEjbRefs().values()) {
            this.context.getNamingResources().addEjb(ejbRef);
        }
        for (final ContextEnvironment environment : webxml.getEnvEntries().values()) {
            this.context.getNamingResources().addEnvironment(environment);
        }
        for (final ErrorPage errorPage : webxml.getErrorPages().values()) {
            this.context.addErrorPage(errorPage);
        }
        for (final FilterDef filter : webxml.getFilters().values()) {
            if (filter.getAsyncSupported() == null) {
                filter.setAsyncSupported("false");
            }
            this.context.addFilterDef(filter);
        }
        for (final FilterMap filterMap : webxml.getFilterMappings()) {
            this.context.addFilterMap(filterMap);
        }
        this.context.setJspConfigDescriptor(webxml.getJspConfigDescriptor());
        for (final String listener : webxml.getListeners()) {
            this.context.addApplicationListener(listener);
        }
        for (final Map.Entry<String, String> entry : webxml.getLocaleEncodingMappings().entrySet()) {
            this.context.addLocaleEncodingMappingParameter(entry.getKey(), entry.getValue());
        }
        if (webxml.getLoginConfig() != null) {
            this.context.setLoginConfig(webxml.getLoginConfig());
        }
        for (final MessageDestinationRef mdr : webxml.getMessageDestinationRefs().values()) {
            this.context.getNamingResources().addMessageDestinationRef(mdr);
        }
        this.context.setIgnoreAnnotations(webxml.isMetadataComplete());
        for (final Map.Entry<String, String> entry : webxml.getMimeMappings().entrySet()) {
            this.context.addMimeMapping(entry.getKey(), entry.getValue());
        }
        this.context.setRequestCharacterEncoding(webxml.getRequestCharacterEncoding());
        for (final ContextResourceEnvRef resource : webxml.getResourceEnvRefs().values()) {
            this.context.getNamingResources().addResourceEnvRef(resource);
        }
        for (final ContextResource resource2 : webxml.getResourceRefs().values()) {
            this.context.getNamingResources().addResource(resource2);
        }
        this.context.setResponseCharacterEncoding(webxml.getResponseCharacterEncoding());
        final boolean allAuthenticatedUsersIsAppRole = webxml.getSecurityRoles().contains("**");
        for (final SecurityConstraint constraint : webxml.getSecurityConstraints()) {
            if (allAuthenticatedUsersIsAppRole) {
                constraint.treatAllAuthenticatedUsersAsApplicationRole();
            }
            this.context.addConstraint(constraint);
        }
        for (final String role : webxml.getSecurityRoles()) {
            this.context.addSecurityRole(role);
        }
        for (final ContextService service : webxml.getServiceRefs().values()) {
            this.context.getNamingResources().addService(service);
        }
        for (final ServletDef servlet : webxml.getServlets().values()) {
            final Wrapper wrapper = this.context.createWrapper();
            if (servlet.getLoadOnStartup() != null) {
                wrapper.setLoadOnStartup(servlet.getLoadOnStartup());
            }
            if (servlet.getEnabled() != null) {
                wrapper.setEnabled(servlet.getEnabled());
            }
            wrapper.setName(servlet.getServletName());
            final Map<String, String> params = (Map<String, String>)servlet.getParameterMap();
            for (final Map.Entry<String, String> entry2 : params.entrySet()) {
                wrapper.addInitParameter(entry2.getKey(), entry2.getValue());
            }
            wrapper.setRunAs(servlet.getRunAs());
            final Set<SecurityRoleRef> roleRefs = (Set<SecurityRoleRef>)servlet.getSecurityRoleRefs();
            for (final SecurityRoleRef roleRef : roleRefs) {
                wrapper.addSecurityReference(roleRef.getName(), roleRef.getLink());
            }
            wrapper.setServletClass(servlet.getServletClass());
            final MultipartDef multipartdef = servlet.getMultipartDef();
            if (multipartdef != null) {
                long maxFileSize = -1L;
                long maxRequestSize = -1L;
                int fileSizeThreshold = 0;
                if (null != multipartdef.getMaxFileSize()) {
                    maxFileSize = Long.parseLong(multipartdef.getMaxFileSize());
                }
                if (null != multipartdef.getMaxRequestSize()) {
                    maxRequestSize = Long.parseLong(multipartdef.getMaxRequestSize());
                }
                if (null != multipartdef.getFileSizeThreshold()) {
                    fileSizeThreshold = Integer.parseInt(multipartdef.getFileSizeThreshold());
                }
                wrapper.setMultipartConfigElement(new MultipartConfigElement(multipartdef.getLocation(), maxFileSize, maxRequestSize, fileSizeThreshold));
            }
            if (servlet.getAsyncSupported() != null) {
                wrapper.setAsyncSupported(servlet.getAsyncSupported());
            }
            wrapper.setOverridable(servlet.isOverridable());
            this.context.addChild(wrapper);
        }
        for (final Map.Entry<String, String> entry3 : webxml.getServletMappings().entrySet()) {
            this.context.addServletMappingDecoded(entry3.getKey(), entry3.getValue());
        }
        final SessionConfig sessionConfig = webxml.getSessionConfig();
        if (sessionConfig != null) {
            if (sessionConfig.getSessionTimeout() != null) {
                this.context.setSessionTimeout(sessionConfig.getSessionTimeout());
            }
            final SessionCookieConfig scc = this.context.getServletContext().getSessionCookieConfig();
            scc.setName(sessionConfig.getCookieName());
            scc.setDomain(sessionConfig.getCookieDomain());
            scc.setPath(sessionConfig.getCookiePath());
            scc.setComment(sessionConfig.getCookieComment());
            if (sessionConfig.getCookieHttpOnly() != null) {
                scc.setHttpOnly((boolean)sessionConfig.getCookieHttpOnly());
            }
            if (sessionConfig.getCookieSecure() != null) {
                scc.setSecure((boolean)sessionConfig.getCookieSecure());
            }
            if (sessionConfig.getCookieMaxAge() != null) {
                scc.setMaxAge((int)sessionConfig.getCookieMaxAge());
            }
            if (sessionConfig.getSessionTrackingModes().size() > 0) {
                this.context.getServletContext().setSessionTrackingModes((Set)sessionConfig.getSessionTrackingModes());
            }
        }
        for (final String welcomeFile : webxml.getWelcomeFiles()) {
            if (welcomeFile != null && welcomeFile.length() > 0) {
                this.context.addWelcomeFile(welcomeFile);
            }
        }
        for (final JspPropertyGroup jspPropertyGroup : webxml.getJspPropertyGroups()) {
            String jspServletName = this.context.findServletMapping("*.jsp");
            if (jspServletName == null) {
                jspServletName = "jsp";
            }
            if (this.context.findChild(jspServletName) != null) {
                for (final String urlPattern : jspPropertyGroup.getUrlPatterns()) {
                    this.context.addServletMappingDecoded(urlPattern, jspServletName, true);
                }
            }
            else {
                if (!ContextConfig.log.isDebugEnabled()) {
                    continue;
                }
                for (final String urlPattern : jspPropertyGroup.getUrlPatterns()) {
                    ContextConfig.log.debug((Object)("Skipping " + urlPattern + " , no servlet " + jspServletName));
                }
            }
        }
        for (final Map.Entry<String, String> entry4 : webxml.getPostConstructMethods().entrySet()) {
            this.context.addPostConstructMethod(entry4.getKey(), entry4.getValue());
        }
        for (final Map.Entry<String, String> entry4 : webxml.getPreDestroyMethods().entrySet()) {
            this.context.addPreDestroyMethod(entry4.getKey(), entry4.getValue());
        }
    }
    
    private WebXml getTomcatWebXmlFragment(final WebXmlParser webXmlParser) {
        final WebXml webXmlTomcatFragment = this.createWebXml();
        webXmlTomcatFragment.setOverridable(true);
        webXmlTomcatFragment.setDistributable(true);
        webXmlTomcatFragment.setAlwaysAddWelcomeFiles(false);
        final WebResource resource = this.context.getResources().getResource("/WEB-INF/tomcat-web.xml");
        if (resource.isFile()) {
            try {
                final InputSource source = new InputSource(resource.getURL().toURI().toString());
                source.setByteStream(resource.getInputStream());
                if (!webXmlParser.parseWebXml(source, webXmlTomcatFragment, false)) {
                    this.ok = false;
                }
            }
            catch (URISyntaxException e) {
                ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.tomcatWebXmlError"), (Throwable)e);
            }
        }
        return webXmlTomcatFragment;
    }
    
    private WebXml getDefaultWebXmlFragment(final WebXmlParser webXmlParser) {
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
                        ExceptionUtils.handleThrowable((Throwable)e);
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
                        ExceptionUtils.handleThrowable((Throwable)e2);
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
                        ExceptionUtils.handleThrowable((Throwable)e);
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
                        ExceptionUtils.handleThrowable((Throwable)e3);
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
            else if (!webXmlParser.parseWebXml(globalWebXml, webXmlDefaultFragment, false)) {
                this.ok = false;
            }
            webXmlDefaultFragment.setReplaceWelcomeFiles(true);
            if (!webXmlParser.parseWebXml(hostWebXml, webXmlDefaultFragment, false)) {
                this.ok = false;
            }
            if (globalTimeStamp != -1L && hostTimeStamp != -1L) {
                entry = new DefaultWebXmlCacheEntry(webXmlDefaultFragment, globalTimeStamp, hostTimeStamp);
                ContextConfig.hostWebXmlCache.put(host, entry);
                host.addLifecycleListener(new HostWebXmlCacheCleaner());
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
                final String[] initParameters;
                final String[] params = initParameters = w.findInitParameters();
                for (final String param : initParameters) {
                    jspInitParams.put(param, w.findInitParameter(param));
                }
            }
        }
        else {
            jspInitParams = (Map<String, String>)jspServlet.getParameterMap();
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
                throw new IllegalArgumentException(ContextConfig.sm.getString("contextConfig.jspFile.error", new Object[] { jspFile }));
            }
            if (ContextConfig.log.isDebugEnabled()) {
                ContextConfig.log.debug((Object)ContextConfig.sm.getString("contextConfig.jspFile.warning", new Object[] { jspFile }));
            }
            jspFile = "/" + jspFile;
        }
        servletDef.getParameterMap().put("jspFile", jspFile);
        servletDef.setJspFile((String)null);
        for (final Map.Entry<String, String> initParam : jspInitParams.entrySet()) {
            servletDef.addInitParameter((String)initParam.getKey(), (String)initParam.getValue());
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
            ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.servletContainerInitializerFail", new Object[] { this.context.getName() }), (Throwable)e);
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
                    ContextConfig.log.info((Object)ContextConfig.sm.getString("contextConfig.sci.debug", new Object[] { sci.getClass().getName() }), (Throwable)e2);
                }
                else {
                    ContextConfig.log.info((Object)ContextConfig.sm.getString("contextConfig.sci.info", new Object[] { sci.getClass().getName() }));
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
            try {
                if ("jar".equals(url.getProtocol()) || url.toString().endsWith(".jar")) {
                    try (final Jar jar = JarFactory.newInstance(url)) {
                        jar.nextEntry();
                        for (String entryName = jar.getEntryName(); entryName != null; entryName = jar.getEntryName()) {
                            if (entryName.startsWith("META-INF/resources/")) {
                                this.context.getResources().createWebResourceSet(WebResourceRoot.ResourceSetType.RESOURCE_JAR, "/", url, "/META-INF/resources");
                                break;
                            }
                            jar.nextEntry();
                        }
                    }
                }
                else {
                    if (!"file".equals(url.getProtocol())) {
                        continue;
                    }
                    final File file = new File(url.toURI());
                    final File resources = new File(file, "META-INF/resources/");
                    if (!resources.isDirectory()) {
                        continue;
                    }
                    this.context.getResources().createWebResourceSet(WebResourceRoot.ResourceSetType.RESOURCE_JAR, "/", resources.getAbsolutePath(), null, "/");
                }
            }
            catch (IOException | URISyntaxException ex2) {
                final Exception ex;
                final Exception e = ex;
                ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.resourceJarFail", new Object[] { url, this.context.getName() }));
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
        return this.getWebXmlSource(this.defaultWebXml, true);
    }
    
    protected InputSource getHostWebXmlSource() {
        final File hostConfigBase = this.getHostConfigBase();
        if (hostConfigBase == null) {
            return null;
        }
        return this.getWebXmlSource(hostConfigBase.getPath(), false);
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
                        ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.altDDNotFound", new Object[] { altDDName }));
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
    
    public String getConfigBasePath() {
        String path = null;
        if (this.context.getParent() instanceof Host) {
            final Host host = (Host)this.context.getParent();
            if (host.getXmlBase() != null) {
                path = host.getXmlBase();
            }
            else {
                final StringBuilder xmlDir = new StringBuilder("conf");
                final Container parent = host.getParent();
                if (parent instanceof Engine) {
                    xmlDir.append('/');
                    xmlDir.append(parent.getName());
                }
                xmlDir.append('/');
                xmlDir.append(host.getName());
                path = xmlDir.toString();
            }
        }
        return path;
    }
    
    protected InputSource getWebXmlSource(final String filename, final boolean global) {
        ConfigurationSource.Resource webXmlResource = null;
        try {
            if (global) {
                if ("conf/web.xml".equals(filename)) {
                    webXmlResource = ConfigFileLoader.getSource().getSharedWebXml();
                }
                else {
                    webXmlResource = ConfigFileLoader.getSource().getResource(filename);
                }
            }
            else {
                final String hostWebXml = Container.getConfigPath(this.context, "web.xml.default");
                webXmlResource = ConfigFileLoader.getSource().getResource(hostWebXml);
            }
        }
        catch (IOException e2) {
            return null;
        }
        InputStream stream = null;
        InputSource source = null;
        try {
            stream = webXmlResource.getInputStream();
            source = new InputSource(webXmlResource.getURI().toString());
            if (stream != null) {
                source.setByteStream(stream);
            }
        }
        catch (Exception e) {
            ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.defaultError", new Object[] { filename, webXmlResource.getURI() }), (Throwable)e);
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
    
    protected Map<String, WebXml> processJarsForWebFragments(final WebXml application, final WebXmlParser webXmlParser) {
        final JarScanner jarScanner = this.context.getJarScanner();
        boolean delegate = false;
        if (this.context instanceof StandardContext) {
            delegate = ((StandardContext)this.context).getDelegate();
        }
        boolean parseRequired = true;
        final Set<String> absoluteOrder = (Set<String>)application.getAbsoluteOrdering();
        if (absoluteOrder != null && absoluteOrder.isEmpty() && !this.context.getXmlValidation()) {
            parseRequired = false;
        }
        final FragmentJarScannerCallback callback = new FragmentJarScannerCallback(webXmlParser, delegate, parseRequired);
        jarScanner.scan(JarScanType.PLUGGABILITY, this.context.getServletContext(), (JarScannerCallback)callback);
        if (!callback.isOk()) {
            this.ok = false;
        }
        return (Map<String, WebXml>)callback.getFragments();
    }
    
    protected void processAnnotations(final Set<WebXml> fragments, final boolean handlesTypesOnly, final Map<String, JavaClassCacheEntry> javaClassCache) {
        if (this.context.isParallelAnnotationScanning()) {
            this.processAnnotationsInParallel(fragments, handlesTypesOnly, javaClassCache);
        }
        else {
            for (final WebXml fragment : fragments) {
                this.scanWebXmlFragment(handlesTypesOnly, fragment, javaClassCache);
            }
        }
    }
    
    private void scanWebXmlFragment(final boolean handlesTypesOnly, final WebXml fragment, final Map<String, JavaClassCacheEntry> javaClassCache) {
        final boolean htOnly = handlesTypesOnly || !fragment.getWebappJar() || fragment.isMetadataComplete();
        final WebXml annotations = new WebXml();
        annotations.setDistributable(true);
        final URL url = fragment.getURL();
        this.processAnnotationsUrl(url, annotations, htOnly, javaClassCache);
        final Set<WebXml> set = new HashSet<WebXml>();
        set.add(annotations);
        fragment.merge((Set)set);
    }
    
    protected void processAnnotationsInParallel(final Set<WebXml> fragments, final boolean handlesTypesOnly, final Map<String, JavaClassCacheEntry> javaClassCache) {
        final Server s = this.getServer();
        ExecutorService pool = null;
        pool = s.getUtilityExecutor();
        final List<Future<?>> futures = new ArrayList<Future<?>>(fragments.size());
        for (final WebXml fragment : fragments) {
            final Runnable task = new AnnotationScanTask(fragment, handlesTypesOnly, (Map)javaClassCache);
            futures.add(pool.submit(task));
        }
        try {
            for (final Future<?> future : futures) {
                future.get();
            }
        }
        catch (Exception e) {
            throw new RuntimeException(ContextConfig.sm.getString("contextConfig.processAnnotationsInParallelFailure"), e);
        }
    }
    
    protected void processAnnotationsWebResource(final WebResource webResource, final WebXml fragment, final boolean handlesTypesOnly, final Map<String, JavaClassCacheEntry> javaClassCache) {
        if (webResource.isDirectory()) {
            final WebResource[] webResources = webResource.getWebResourceRoot().listResources(webResource.getWebappPath());
            if (webResources.length > 0) {
                if (ContextConfig.log.isDebugEnabled()) {
                    ContextConfig.log.debug((Object)ContextConfig.sm.getString("contextConfig.processAnnotationsWebDir.debug", new Object[] { webResource.getURL() }));
                }
                for (final WebResource r : webResources) {
                    this.processAnnotationsWebResource(r, fragment, handlesTypesOnly, javaClassCache);
                }
            }
        }
        else if (webResource.isFile() && webResource.getName().endsWith(".class")) {
            try (final InputStream is = webResource.getInputStream()) {
                this.processAnnotationsStream(is, fragment, handlesTypesOnly, javaClassCache);
            }
            catch (IOException | ClassFormatException ex2) {
                final Exception ex;
                final Exception e = ex;
                ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.inputStreamWebResource", new Object[] { webResource.getWebappPath() }), (Throwable)e);
            }
        }
    }
    
    protected void processAnnotationsUrl(final URL url, final WebXml fragment, final boolean handlesTypesOnly, final Map<String, JavaClassCacheEntry> javaClassCache) {
        if (url == null) {
            return;
        }
        if ("jar".equals(url.getProtocol()) || url.toString().endsWith(".jar")) {
            this.processAnnotationsJar(url, fragment, handlesTypesOnly, javaClassCache);
        }
        else if ("file".equals(url.getProtocol())) {
            try {
                this.processAnnotationsFile(new File(url.toURI()), fragment, handlesTypesOnly, javaClassCache);
            }
            catch (URISyntaxException e) {
                ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.fileUrl", new Object[] { url }), (Throwable)e);
            }
        }
        else {
            ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.unknownUrlProtocol", new Object[] { url.getProtocol(), url }));
        }
    }
    
    protected void processAnnotationsJar(final URL url, final WebXml fragment, final boolean handlesTypesOnly, final Map<String, JavaClassCacheEntry> javaClassCache) {
        try (final Jar jar = JarFactory.newInstance(url)) {
            if (ContextConfig.log.isDebugEnabled()) {
                ContextConfig.log.debug((Object)ContextConfig.sm.getString("contextConfig.processAnnotationsJar.debug", new Object[] { url }));
            }
            jar.nextEntry();
            for (String entryName = jar.getEntryName(); entryName != null; entryName = jar.getEntryName()) {
                if (entryName.endsWith(".class")) {
                    try (final InputStream is = jar.getEntryInputStream()) {
                        this.processAnnotationsStream(is, fragment, handlesTypesOnly, javaClassCache);
                    }
                    catch (IOException | ClassFormatException ex2) {
                        final Exception ex;
                        final Exception e = ex;
                        ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.inputStreamJar", new Object[] { entryName, url }), (Throwable)e);
                    }
                }
                jar.nextEntry();
            }
        }
        catch (IOException e2) {
            ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.jarFile", new Object[] { url }), (Throwable)e2);
        }
    }
    
    protected void processAnnotationsFile(final File file, final WebXml fragment, final boolean handlesTypesOnly, final Map<String, JavaClassCacheEntry> javaClassCache) {
        if (file.isDirectory()) {
            final String[] dirs = file.list();
            if (dirs != null) {
                if (ContextConfig.log.isDebugEnabled()) {
                    ContextConfig.log.debug((Object)ContextConfig.sm.getString("contextConfig.processAnnotationsDir.debug", new Object[] { file }));
                }
                for (final String dir : dirs) {
                    this.processAnnotationsFile(new File(file, dir), fragment, handlesTypesOnly, javaClassCache);
                }
            }
        }
        else if (file.getName().endsWith(".class") && file.canRead()) {
            try (final FileInputStream fis = new FileInputStream(file)) {
                this.processAnnotationsStream(fis, fragment, handlesTypesOnly, javaClassCache);
            }
            catch (IOException | ClassFormatException ex2) {
                final Exception ex;
                final Exception e = ex;
                ContextConfig.log.error((Object)ContextConfig.sm.getString("contextConfig.inputStreamFile", new Object[] { file.getAbsolutePath() }), (Throwable)e);
            }
        }
    }
    
    protected void processAnnotationsStream(final InputStream is, final WebXml fragment, final boolean handlesTypesOnly, final Map<String, JavaClassCacheEntry> javaClassCache) throws ClassFormatException, IOException {
        final ClassParser parser = new ClassParser(is);
        final JavaClass clazz = parser.parse();
        this.checkHandlesTypes(clazz, javaClassCache);
        if (handlesTypesOnly) {
            return;
        }
        this.processClass(fragment, clazz);
    }
    
    protected void processClass(final WebXml fragment, final JavaClass clazz) {
        final AnnotationEntry[] annotationsEntries = clazz.getAnnotationEntries();
        if (annotationsEntries != null) {
            final String className = clazz.getClassName();
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
    
    protected void checkHandlesTypes(final JavaClass javaClass, final Map<String, JavaClassCacheEntry> javaClassCache) {
        if (this.typeInitializerMap.size() == 0) {
            return;
        }
        if ((javaClass.getAccessFlags() & 0x2000) != 0x0) {
            return;
        }
        final String className = javaClass.getClassName();
        Class<?> clazz = null;
        if (this.handlesTypesNonAnnotations) {
            this.populateJavaClassCache(className, javaClass, javaClassCache);
            final JavaClassCacheEntry entry = javaClassCache.get(className);
            if (entry.getSciSet() == null) {
                try {
                    this.populateSCIsForCacheEntry(entry, javaClassCache);
                }
                catch (StackOverflowError soe) {
                    throw new IllegalStateException(ContextConfig.sm.getString("contextConfig.annotationsStackOverflow", new Object[] { this.context.getName(), this.classHierarchyToString(className, entry, javaClassCache) }));
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
            final AnnotationEntry[] annotationEntries = javaClass.getAnnotationEntries();
            if (annotationEntries != null) {
                for (final Map.Entry<Class<?>, Set<ServletContainerInitializer>> entry2 : this.typeInitializerMap.entrySet()) {
                    if (entry2.getKey().isAnnotation()) {
                        final String entryClassName = entry2.getKey().getName();
                        for (final AnnotationEntry annotationEntry : annotationEntries) {
                            if (entryClassName.equals(getClassName(annotationEntry.getAnnotationType()))) {
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
    }
    
    private String classHierarchyToString(final String className, final JavaClassCacheEntry entry, final Map<String, JavaClassCacheEntry> javaClassCache) {
        final JavaClassCacheEntry start = entry;
        final StringBuilder msg = new StringBuilder(className);
        msg.append("->");
        String parentName = entry.getSuperclassName();
        JavaClassCacheEntry parent = javaClassCache.get(parentName);
        for (int count = 0; count < 100 && parent != null && parent != start; ++count, parentName = parent.getSuperclassName(), parent = javaClassCache.get(parentName)) {
            msg.append(parentName);
            msg.append("->");
        }
        msg.append(parentName);
        return msg.toString();
    }
    
    private void populateJavaClassCache(final String className, final JavaClass javaClass, final Map<String, JavaClassCacheEntry> javaClassCache) {
        if (javaClassCache.containsKey(className)) {
            return;
        }
        javaClassCache.put(className, new JavaClassCacheEntry(javaClass));
        this.populateJavaClassCache(javaClass.getSuperclassName(), javaClassCache);
        for (final String interfaceName : javaClass.getInterfaceNames()) {
            this.populateJavaClassCache(interfaceName, javaClassCache);
        }
    }
    
    private void populateJavaClassCache(final String className, final Map<String, JavaClassCacheEntry> javaClassCache) {
        if (!javaClassCache.containsKey(className)) {
            final String name = className.replace('.', '/') + ".class";
            try (final InputStream is = this.context.getLoader().getClassLoader().getResourceAsStream(name)) {
                if (is == null) {
                    return;
                }
                final ClassParser parser = new ClassParser(is);
                final JavaClass clazz = parser.parse();
                this.populateJavaClassCache(clazz.getClassName(), clazz, javaClassCache);
            }
            catch (ClassFormatException | IOException ex2) {
                final Exception ex;
                final Exception e = ex;
                ContextConfig.log.debug((Object)ContextConfig.sm.getString("contextConfig.invalidSciHandlesTypes", new Object[] { className }), (Throwable)e);
            }
        }
    }
    
    private void populateSCIsForCacheEntry(final JavaClassCacheEntry cacheEntry, final Map<String, JavaClassCacheEntry> javaClassCache) {
        final Set<ServletContainerInitializer> result = new HashSet<ServletContainerInitializer>();
        final String superClassName = cacheEntry.getSuperclassName();
        final JavaClassCacheEntry superClassCacheEntry = javaClassCache.get(superClassName);
        if (cacheEntry.equals(superClassCacheEntry)) {
            cacheEntry.setSciSet(ContextConfig.EMPTY_SCI_SET);
            return;
        }
        if (superClassCacheEntry != null) {
            if (superClassCacheEntry.getSciSet() == null) {
                this.populateSCIsForCacheEntry(superClassCacheEntry, javaClassCache);
            }
            result.addAll(superClassCacheEntry.getSciSet());
        }
        result.addAll(this.getSCIsForClass(superClassName));
        for (final String interfaceName : cacheEntry.getInterfaceNames()) {
            final JavaClassCacheEntry interfaceEntry = javaClassCache.get(interfaceName);
            if (interfaceEntry != null) {
                if (interfaceEntry.getSciSet() == null) {
                    this.populateSCIsForCacheEntry(interfaceEntry, javaClassCache);
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
        final List<ElementValuePair> evps = (List<ElementValuePair>)ae.getElementValuePairs();
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
                    throw new IllegalArgumentException(ContextConfig.sm.getString("contextConfig.urlPatternValue", new Object[] { "WebServlet", className }));
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
                    final Map<String, String> webXMLInitParams = (Map<String, String>)servletDef.getParameterMap();
                    for (final Map.Entry<String, String> entry : initParams.entrySet()) {
                        if (webXMLInitParams.get(entry.getKey()) == null) {
                            servletDef.addInitParameter((String)entry.getKey(), (String)entry.getValue());
                        }
                    }
                }
                else {
                    for (final Map.Entry<String, String> entry2 : initParams.entrySet()) {
                        servletDef.addInitParameter((String)entry2.getKey(), (String)entry2.getValue());
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
        final List<ElementValuePair> evps = (List<ElementValuePair>)ae.getElementValuePairs();
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
                    throw new IllegalArgumentException(ContextConfig.sm.getString("contextConfig.urlPatternValue", new Object[] { "WebFilter", className }));
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
                    final Map<String, String> webXMLInitParams = (Map<String, String>)filterDef.getParameterMap();
                    for (final Map.Entry<String, String> entry : initParams.entrySet()) {
                        if (webXMLInitParams.get(entry.getKey()) == null) {
                            filterDef.addInitParameter((String)entry.getKey(), (String)entry.getValue());
                        }
                    }
                }
                else {
                    for (final Map.Entry<String, String> entry2 : initParams.entrySet()) {
                        filterDef.addInitParameter((String)entry2.getKey(), (String)entry2.getValue());
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
            final Set<FilterMap> fmap = (Set<FilterMap>)fragment.getFilterMappings();
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
        final List<String> values = new ArrayList<String>();
        if (ev instanceof ArrayElementValue) {
            final ElementValue[] elementValuesArray;
            final ElementValue[] arrayValues = elementValuesArray = ((ArrayElementValue)ev).getElementValuesArray();
            for (final ElementValue value : elementValuesArray) {
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
            final ElementValue[] elementValuesArray;
            final ElementValue[] arrayValues = elementValuesArray = ((ArrayElementValue)ev).getElementValuesArray();
            for (final ElementValue value : elementValuesArray) {
                if (value instanceof AnnotationElementValue) {
                    final List<ElementValuePair> evps = (List<ElementValuePair>)((AnnotationElementValue)value).getAnnotationEntry().getElementValuePairs();
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
        DUMMY_LOGIN_CONFIG = new LoginConfig("NONE", (String)null, (String)null, (String)null);
        Properties props = new Properties();
        try (final InputStream is = ContextConfig.class.getClassLoader().getResourceAsStream("org/apache/catalina/startup/Authenticators.properties")) {
            if (is != null) {
                props.load(is);
            }
        }
        catch (IOException ioe) {
            props = null;
        }
        authenticators = props;
        ContextConfig.deploymentCount = 0L;
        hostWebXmlCache = new ConcurrentHashMap<Host, DefaultWebXmlCacheEntry>();
        EMPTY_SCI_SET = Collections.emptySet();
    }
    
    private class AnnotationScanTask implements Runnable
    {
        private final WebXml fragment;
        private final boolean handlesTypesOnly;
        private Map<String, JavaClassCacheEntry> javaClassCache;
        
        private AnnotationScanTask(final WebXml fragment, final boolean handlesTypesOnly, final Map<String, JavaClassCacheEntry> javaClassCache) {
            this.fragment = fragment;
            this.handlesTypesOnly = handlesTypesOnly;
            this.javaClassCache = javaClassCache;
        }
        
        @Override
        public void run() {
            ContextConfig.this.scanWebXmlFragment(this.handlesTypesOnly, this.fragment, this.javaClassCache);
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
    
    private static class HostWebXmlCacheCleaner implements LifecycleListener
    {
        @Override
        public void lifecycleEvent(final LifecycleEvent event) {
            if ("after_destroy".equals(event.getType())) {
                final Host host = (Host)event.getSource();
                ContextConfig.hostWebXmlCache.remove(host);
            }
        }
    }
    
    static class JavaClassCacheEntry
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
    
    public interface ContextXml
    {
        void load(final Context p0);
    }
}
