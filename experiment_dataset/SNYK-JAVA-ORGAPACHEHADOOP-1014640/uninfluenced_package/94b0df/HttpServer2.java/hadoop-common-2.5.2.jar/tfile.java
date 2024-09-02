// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.hadoop.http;

import java.util.Enumeration;
import org.mortbay.io.Buffer;
import org.mortbay.jetty.MimeTypes;
import org.mortbay.jetty.handler.ContextHandler;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.FilterChain;
import javax.servlet.ServletResponse;
import javax.servlet.ServletRequest;
import javax.servlet.FilterConfig;
import javax.servlet.Filter;
import javax.servlet.ServletException;
import java.io.PrintWriter;
import org.mortbay.jetty.security.SslSocketConnector;
import org.apache.hadoop.security.ssl.SslSocketConnectorSecure;
import org.apache.hadoop.HadoopIllegalArgumentException;
import java.net.URI;
import org.apache.commons.logging.LogFactory;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletContext;
import java.net.BindException;
import java.io.InterruptedIOException;
import org.mortbay.util.MultiException;
import org.apache.hadoop.security.authentication.server.AuthenticationFilter;
import org.apache.hadoop.security.SecurityUtil;
import java.net.InetSocketAddress;
import java.net.URL;
import org.mortbay.jetty.servlet.FilterHolder;
import java.util.Iterator;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.FilterMapping;
import org.apache.hadoop.security.UserGroupInformation;
import org.mortbay.jetty.servlet.ServletHolder;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import org.apache.hadoop.conf.ConfServlet;
import org.apache.hadoop.jmx.JMXJsonServlet;
import org.apache.hadoop.metrics.MetricsServlet;
import org.apache.hadoop.log.LogLevel;
import javax.servlet.http.HttpServlet;
import org.mortbay.jetty.servlet.DefaultServlet;
import org.mortbay.jetty.HandlerContainer;
import org.apache.hadoop.util.ReflectionUtils;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.apache.hadoop.util.Shell;
import java.util.Collections;
import org.mortbay.jetty.Connector;
import java.io.FileNotFoundException;
import org.mortbay.jetty.RequestLog;
import org.mortbay.jetty.SessionManager;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.handler.RequestLogHandler;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.servlet.AbstractSessionManager;
import org.mortbay.thread.ThreadPool;
import org.mortbay.thread.QueuedThreadPool;
import com.google.common.base.Preconditions;
import org.apache.hadoop.conf.Configuration;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import com.google.common.collect.Lists;
import org.mortbay.jetty.servlet.Context;
import java.util.Map;
import org.mortbay.jetty.webapp.WebAppContext;
import java.util.List;
import org.mortbay.jetty.Server;
import org.apache.hadoop.security.authorize.AccessControlList;
import org.apache.commons.logging.Log;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.classification.InterfaceAudience;

@InterfaceAudience.Private
@InterfaceStability.Evolving
public final class HttpServer2 implements FilterContainer
{
    public static final Log LOG;
    static final String FILTER_INITIALIZER_PROPERTY = "hadoop.http.filter.initializers";
    static final String HTTP_MAX_THREADS = "hadoop.http.max.threads";
    public static final String CONF_CONTEXT_ATTRIBUTE = "hadoop.conf";
    public static final String ADMINS_ACL = "admins.acl";
    public static final String SPNEGO_FILTER = "SpnegoFilter";
    public static final String NO_CACHE_FILTER = "NoCacheFilter";
    public static final String BIND_ADDRESS = "bind.address";
    private final AccessControlList adminsAcl;
    protected final Server webServer;
    private final List<ListenerInfo> listeners;
    protected final WebAppContext webAppContext;
    protected final boolean findPort;
    protected final Map<Context, Boolean> defaultContexts;
    protected final List<String> filterNames;
    static final String STATE_DESCRIPTION_ALIVE = " - alive";
    static final String STATE_DESCRIPTION_NOT_LIVE = " - not live";
    
    private HttpServer2(final Builder b) throws IOException {
        this.listeners = (List<ListenerInfo>)Lists.newArrayList();
        this.defaultContexts = new HashMap<Context, Boolean>();
        this.filterNames = new ArrayList<String>();
        final String appDir = this.getWebAppsPath(b.name);
        this.webServer = new Server();
        this.adminsAcl = b.adminsAcl;
        this.webAppContext = createWebAppContext(b.name, b.conf, this.adminsAcl, appDir);
        this.findPort = b.findPort;
        this.initializeWebServer(b.name, b.hostName, b.conf, b.pathSpecs);
    }
    
    private void initializeWebServer(final String name, final String hostName, Configuration conf, final String[] pathSpecs) throws FileNotFoundException, IOException {
        Preconditions.checkNotNull((Object)this.webAppContext);
        final int maxThreads = conf.getInt("hadoop.http.max.threads", -1);
        final QueuedThreadPool threadPool = (maxThreads == -1) ? new QueuedThreadPool() : new QueuedThreadPool(maxThreads);
        threadPool.setDaemon(true);
        this.webServer.setThreadPool((ThreadPool)threadPool);
        final SessionManager sm = this.webAppContext.getSessionHandler().getSessionManager();
        if (sm instanceof AbstractSessionManager) {
            final AbstractSessionManager asm = (AbstractSessionManager)sm;
            asm.setHttpOnly(true);
            asm.setSecureCookies(true);
        }
        final ContextHandlerCollection contexts = new ContextHandlerCollection();
        final RequestLog requestLog = HttpRequestLog.getRequestLog(name);
        if (requestLog != null) {
            final RequestLogHandler requestLogHandler = new RequestLogHandler();
            requestLogHandler.setRequestLog(requestLog);
            final HandlerCollection handlers = new HandlerCollection();
            handlers.setHandlers(new Handler[] { (Handler)contexts, (Handler)requestLogHandler });
            this.webServer.setHandler((Handler)handlers);
        }
        else {
            this.webServer.setHandler((Handler)contexts);
        }
        final String appDir = this.getWebAppsPath(name);
        this.webServer.addHandler((Handler)this.webAppContext);
        this.addDefaultApps(contexts, appDir, conf);
        this.addGlobalFilter("safety", QuotingInputFilter.class.getName(), null);
        final FilterInitializer[] initializers = getFilterInitializers(conf);
        if (initializers != null) {
            conf = new Configuration(conf);
            conf.set("bind.address", hostName);
            for (final FilterInitializer c : initializers) {
                c.initFilter(this, conf);
            }
        }
        this.addDefaultServlets();
        if (pathSpecs != null) {
            for (final String path : pathSpecs) {
                HttpServer2.LOG.info((Object)("adding path spec: " + path));
                this.addFilterPathMapping(path, (Context)this.webAppContext);
            }
        }
    }
    
    private void addUnmanagedListener(final Connector connector) {
        this.listeners.add(new ListenerInfo(false, connector));
    }
    
    private void addManagedListener(final Connector connector) {
        this.listeners.add(new ListenerInfo(true, connector));
    }
    
    private static WebAppContext createWebAppContext(final String name, final Configuration conf, final AccessControlList adminsAcl, final String appDir) {
        final WebAppContext ctx = new WebAppContext();
        ctx.setDisplayName(name);
        ctx.setContextPath("/");
        ctx.setWar(appDir + "/" + name);
        ctx.getServletContext().setAttribute("hadoop.conf", (Object)conf);
        ctx.getServletContext().setAttribute("admins.acl", (Object)adminsAcl);
        addNoCacheFilter(ctx);
        return ctx;
    }
    
    private static void addNoCacheFilter(final WebAppContext ctxt) {
        defineFilter((Context)ctxt, "NoCacheFilter", NoCacheFilter.class.getName(), Collections.emptyMap(), new String[] { "/*" });
    }
    
    public Connector createBaseListener(final Configuration conf) {
        return createDefaultChannelConnector();
    }
    
    @InterfaceAudience.Private
    public static Connector createDefaultChannelConnector() {
        final SelectChannelConnector ret = new SelectChannelConnectorWithSafeStartup();
        ret.setLowResourceMaxIdleTime(10000);
        ret.setAcceptQueueSize(128);
        ret.setResolveNames(false);
        ret.setUseDirectBuffers(false);
        if (Shell.WINDOWS) {
            ret.setReuseAddress(false);
        }
        ret.setHeaderBufferSize(65536);
        return (Connector)ret;
    }
    
    private static FilterInitializer[] getFilterInitializers(final Configuration conf) {
        if (conf == null) {
            return null;
        }
        final Class<?>[] classes = conf.getClasses("hadoop.http.filter.initializers", (Class<?>[])new Class[0]);
        if (classes == null) {
            return null;
        }
        final FilterInitializer[] initializers = new FilterInitializer[classes.length];
        for (int i = 0; i < classes.length; ++i) {
            initializers[i] = ReflectionUtils.newInstance(classes[i], conf);
        }
        return initializers;
    }
    
    protected void addDefaultApps(final ContextHandlerCollection parent, final String appDir, final Configuration conf) throws IOException {
        final String logDir = System.getProperty("hadoop.log.dir");
        if (logDir != null) {
            final Context logContext = new Context((HandlerContainer)parent, "/logs");
            logContext.setResourceBase(logDir);
            logContext.addServlet((Class)AdminAuthorizedServlet.class, "/*");
            if (conf.getBoolean("hadoop.jetty.logs.serve.aliases", true)) {
                final Map<String, String> params = (Map<String, String>)logContext.getInitParams();
                params.put("org.mortbay.jetty.servlet.Default.aliases", "true");
            }
            logContext.setDisplayName("logs");
            this.setContextAttributes(logContext, conf);
            addNoCacheFilter(this.webAppContext);
            this.defaultContexts.put(logContext, true);
        }
        final Context staticContext = new Context((HandlerContainer)parent, "/static");
        staticContext.setResourceBase(appDir + "/static");
        staticContext.addServlet((Class)DefaultServlet.class, "/*");
        staticContext.setDisplayName("static");
        this.setContextAttributes(staticContext, conf);
        this.defaultContexts.put(staticContext, true);
    }
    
    private void setContextAttributes(final Context context, final Configuration conf) {
        context.getServletContext().setAttribute("hadoop.conf", (Object)conf);
        context.getServletContext().setAttribute("admins.acl", (Object)this.adminsAcl);
    }
    
    protected void addDefaultServlets() {
        this.addServlet("stacks", "/stacks", StackServlet.class);
        this.addServlet("logLevel", "/logLevel", LogLevel.Servlet.class);
        this.addServlet("metrics", "/metrics", MetricsServlet.class);
        this.addServlet("jmx", "/jmx", JMXJsonServlet.class);
        this.addServlet("conf", "/conf", ConfServlet.class);
    }
    
    public void addContext(final Context ctxt, final boolean isFiltered) {
        this.webServer.addHandler((Handler)ctxt);
        addNoCacheFilter(this.webAppContext);
        this.defaultContexts.put(ctxt, isFiltered);
    }
    
    protected void addContext(final String pathSpec, final String dir, final boolean isFiltered) throws IOException {
        if (0 == this.webServer.getHandlers().length) {
            throw new RuntimeException("Couldn't find handler");
        }
        final WebAppContext webAppCtx = new WebAppContext();
        webAppCtx.setContextPath(pathSpec);
        webAppCtx.setWar(dir);
        this.addContext((Context)webAppCtx, true);
    }
    
    public void setAttribute(final String name, final Object value) {
        this.webAppContext.setAttribute(name, value);
    }
    
    public void addJerseyResourcePackage(final String packageName, final String pathSpec) {
        HttpServer2.LOG.info((Object)("addJerseyResourcePackage: packageName=" + packageName + ", pathSpec=" + pathSpec));
        final ServletHolder sh = new ServletHolder((Class)ServletContainer.class);
        sh.setInitParameter("com.sun.jersey.config.property.resourceConfigClass", "com.sun.jersey.api.core.PackagesResourceConfig");
        sh.setInitParameter("com.sun.jersey.config.property.packages", packageName);
        this.webAppContext.addServlet(sh, pathSpec);
    }
    
    public void addServlet(final String name, final String pathSpec, final Class<? extends HttpServlet> clazz) {
        this.addInternalServlet(name, pathSpec, clazz, false);
        this.addFilterPathMapping(pathSpec, (Context)this.webAppContext);
    }
    
    public void addInternalServlet(final String name, final String pathSpec, final Class<? extends HttpServlet> clazz) {
        this.addInternalServlet(name, pathSpec, clazz, false);
    }
    
    public void addInternalServlet(final String name, final String pathSpec, final Class<? extends HttpServlet> clazz, final boolean requireAuth) {
        final ServletHolder holder = new ServletHolder((Class)clazz);
        if (name != null) {
            holder.setName(name);
        }
        this.webAppContext.addServlet(holder, pathSpec);
        if (requireAuth && UserGroupInformation.isSecurityEnabled()) {
            HttpServer2.LOG.info((Object)("Adding Kerberos (SPNEGO) filter to " + name));
            final ServletHandler handler = this.webAppContext.getServletHandler();
            final FilterMapping fmap = new FilterMapping();
            fmap.setPathSpec(pathSpec);
            fmap.setFilterName("SpnegoFilter");
            fmap.setDispatches(15);
            handler.addFilterMapping(fmap);
        }
    }
    
    @Override
    public void addFilter(final String name, final String classname, final Map<String, String> parameters) {
        final String[] USER_FACING_URLS = { "*.html", "*.jsp" };
        defineFilter((Context)this.webAppContext, name, classname, parameters, USER_FACING_URLS);
        HttpServer2.LOG.info((Object)("Added filter " + name + " (class=" + classname + ") to context " + this.webAppContext.getDisplayName()));
        final String[] ALL_URLS = { "/*" };
        for (final Map.Entry<Context, Boolean> e : this.defaultContexts.entrySet()) {
            if (e.getValue()) {
                final Context ctx = e.getKey();
                defineFilter(ctx, name, classname, parameters, ALL_URLS);
                HttpServer2.LOG.info((Object)("Added filter " + name + " (class=" + classname + ") to context " + ctx.getDisplayName()));
            }
        }
        this.filterNames.add(name);
    }
    
    @Override
    public void addGlobalFilter(final String name, final String classname, final Map<String, String> parameters) {
        final String[] ALL_URLS = { "/*" };
        defineFilter((Context)this.webAppContext, name, classname, parameters, ALL_URLS);
        for (final Context ctx : this.defaultContexts.keySet()) {
            defineFilter(ctx, name, classname, parameters, ALL_URLS);
        }
        HttpServer2.LOG.info((Object)("Added global filter '" + name + "' (class=" + classname + ")"));
    }
    
    public static void defineFilter(final Context ctx, final String name, final String classname, final Map<String, String> parameters, final String[] urls) {
        final FilterHolder holder = new FilterHolder();
        holder.setName(name);
        holder.setClassName(classname);
        holder.setInitParameters((Map)parameters);
        final FilterMapping fmap = new FilterMapping();
        fmap.setPathSpecs(urls);
        fmap.setDispatches(15);
        fmap.setFilterName(name);
        final ServletHandler handler = ctx.getServletHandler();
        handler.addFilter(holder, fmap);
    }
    
    protected void addFilterPathMapping(final String pathSpec, final Context webAppCtx) {
        final ServletHandler handler = webAppCtx.getServletHandler();
        for (final String name : this.filterNames) {
            final FilterMapping fmap = new FilterMapping();
            fmap.setPathSpec(pathSpec);
            fmap.setFilterName(name);
            fmap.setDispatches(15);
            handler.addFilterMapping(fmap);
        }
    }
    
    public Object getAttribute(final String name) {
        return this.webAppContext.getAttribute(name);
    }
    
    public WebAppContext getWebAppContext() {
        return this.webAppContext;
    }
    
    protected String getWebAppsPath(final String appName) throws FileNotFoundException {
        final URL url = this.getClass().getClassLoader().getResource("webapps/" + appName);
        if (url == null) {
            throw new FileNotFoundException("webapps/" + appName + " not found in CLASSPATH");
        }
        final String urlString = url.toString();
        return urlString.substring(0, urlString.lastIndexOf(47));
    }
    
    @Deprecated
    public int getPort() {
        return this.webServer.getConnectors()[0].getLocalPort();
    }
    
    public InetSocketAddress getConnectorAddress(final int index) {
        Preconditions.checkArgument(index >= 0);
        if (index > this.webServer.getConnectors().length) {
            return null;
        }
        final Connector c = this.webServer.getConnectors()[index];
        if (c.getLocalPort() == -1) {
            return null;
        }
        return new InetSocketAddress(c.getHost(), c.getLocalPort());
    }
    
    public void setThreads(final int min, final int max) {
        final QueuedThreadPool pool = (QueuedThreadPool)this.webServer.getThreadPool();
        pool.setMinThreads(min);
        pool.setMaxThreads(max);
    }
    
    private void initSpnego(final Configuration conf, final String hostName, final String usernameConfKey, final String keytabConfKey) throws IOException {
        final Map<String, String> params = new HashMap<String, String>();
        final String principalInConf = conf.get(usernameConfKey);
        if (principalInConf != null && !principalInConf.isEmpty()) {
            params.put("kerberos.principal", SecurityUtil.getServerPrincipal(principalInConf, hostName));
        }
        final String httpKeytab = conf.get(keytabConfKey);
        if (httpKeytab != null && !httpKeytab.isEmpty()) {
            params.put("kerberos.keytab", httpKeytab);
        }
        params.put("type", "kerberos");
        defineFilter((Context)this.webAppContext, "SpnegoFilter", AuthenticationFilter.class.getName(), params, null);
    }
    
    public void start() throws IOException {
        try {
            try {
                this.openListeners();
                this.webServer.start();
            }
            catch (IOException ex) {
                HttpServer2.LOG.info((Object)"HttpServer.start() threw a non Bind IOException", (Throwable)ex);
                throw ex;
            }
            catch (MultiException ex2) {
                HttpServer2.LOG.info((Object)"HttpServer.start() threw a MultiException", (Throwable)ex2);
                throw ex2;
            }
            final Handler[] handlers = this.webServer.getHandlers();
            for (int i = 0; i < handlers.length; ++i) {
                if (handlers[i].isFailed()) {
                    throw new IOException("Problem in starting http server. Server handlers failed");
                }
            }
            final Throwable unavailableException = this.webAppContext.getUnavailableException();
            if (unavailableException != null) {
                this.webServer.stop();
                throw new IOException("Unable to initialize WebAppContext", unavailableException);
            }
        }
        catch (IOException e) {
            throw e;
        }
        catch (InterruptedException e2) {
            throw (IOException)new InterruptedIOException("Interrupted while starting HTTP server").initCause(e2);
        }
        catch (Exception e3) {
            throw new IOException("Problem starting http server", e3);
        }
    }
    
    private void loadListeners() {
        for (final ListenerInfo li : this.listeners) {
            this.webServer.addConnector(li.listener);
        }
    }
    
    void openListeners() throws Exception {
        for (final ListenerInfo li : this.listeners) {
            final Connector listener = li.listener;
            if (li.isManaged) {
                if (li.listener.getLocalPort() != -1) {
                    continue;
                }
                int port = listener.getPort();
                while (true) {
                    try {
                        listener.close();
                        listener.open();
                        HttpServer2.LOG.info((Object)("Jetty bound to port " + listener.getLocalPort()));
                    }
                    catch (BindException ex) {
                        if (port == 0 || !this.findPort) {
                            final BindException be = new BindException("Port in use: " + listener.getHost() + ":" + listener.getPort());
                            be.initCause(ex);
                            throw be;
                        }
                        listener.setPort(++port);
                        Thread.sleep(100L);
                        continue;
                    }
                    break;
                }
            }
        }
    }
    
    public void stop() throws Exception {
        MultiException exception = null;
        for (final ListenerInfo li : this.listeners) {
            if (!li.isManaged) {
                continue;
            }
            try {
                li.listener.close();
            }
            catch (Exception e) {
                HttpServer2.LOG.error((Object)("Error while stopping listener for webapp" + this.webAppContext.getDisplayName()), (Throwable)e);
                exception = this.addMultiException(exception, e);
            }
        }
        try {
            this.webAppContext.clearAttributes();
            this.webAppContext.stop();
        }
        catch (Exception e2) {
            HttpServer2.LOG.error((Object)("Error while stopping web app context for webapp " + this.webAppContext.getDisplayName()), (Throwable)e2);
            exception = this.addMultiException(exception, e2);
        }
        try {
            this.webServer.stop();
        }
        catch (Exception e2) {
            HttpServer2.LOG.error((Object)("Error while stopping web server for webapp " + this.webAppContext.getDisplayName()), (Throwable)e2);
            exception = this.addMultiException(exception, e2);
        }
        if (exception != null) {
            exception.ifExceptionThrow();
        }
    }
    
    private MultiException addMultiException(MultiException exception, final Exception e) {
        if (exception == null) {
            exception = new MultiException();
        }
        exception.add((Throwable)e);
        return exception;
    }
    
    public void join() throws InterruptedException {
        this.webServer.join();
    }
    
    public boolean isAlive() {
        return this.webServer != null && this.webServer.isStarted();
    }
    
    @Override
    public String toString() {
        if (this.listeners.size() == 0) {
            return "Inactive HttpServer";
        }
        final StringBuilder sb = new StringBuilder("HttpServer (").append(this.isAlive() ? " - alive" : " - not live").append("), listening at:");
        for (final ListenerInfo li : this.listeners) {
            final Connector l = li.listener;
            sb.append(l.getHost()).append(":").append(l.getPort()).append("/,");
        }
        return sb.toString();
    }
    
    public static boolean isInstrumentationAccessAllowed(final ServletContext servletContext, final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        final Configuration conf = (Configuration)servletContext.getAttribute("hadoop.conf");
        boolean access = true;
        final boolean adminAccess = conf.getBoolean("hadoop.security.instrumentation.requires.admin", false);
        if (adminAccess) {
            access = hasAdministratorAccess(servletContext, request, response);
        }
        return access;
    }
    
    public static boolean hasAdministratorAccess(final ServletContext servletContext, final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        final Configuration conf = (Configuration)servletContext.getAttribute("hadoop.conf");
        if (!conf.getBoolean("hadoop.security.authorization", false)) {
            return true;
        }
        final String remoteUser = request.getRemoteUser();
        if (remoteUser == null) {
            response.sendError(401, "Unauthenticated users are not authorized to access this page.");
            return false;
        }
        if (servletContext.getAttribute("admins.acl") != null && !userHasAdministratorAccess(servletContext, remoteUser)) {
            response.sendError(401, "User " + remoteUser + " is unauthorized to access this page.");
            return false;
        }
        return true;
    }
    
    public static boolean userHasAdministratorAccess(final ServletContext servletContext, final String remoteUser) {
        final AccessControlList adminsAcl = (AccessControlList)servletContext.getAttribute("admins.acl");
        final UserGroupInformation remoteUserUGI = UserGroupInformation.createRemoteUser(remoteUser);
        return adminsAcl != null && adminsAcl.isUserAllowed(remoteUserUGI);
    }
    
    static {
        LOG = LogFactory.getLog((Class)HttpServer2.class);
    }
    
    private static class ListenerInfo
    {
        private final boolean isManaged;
        private final Connector listener;
        
        private ListenerInfo(final boolean isManaged, final Connector listener) {
            this.isManaged = isManaged;
            this.listener = listener;
        }
    }
    
    public static class Builder
    {
        private ArrayList<URI> endpoints;
        private Connector connector;
        private String name;
        private Configuration conf;
        private String[] pathSpecs;
        private AccessControlList adminsAcl;
        private boolean securityEnabled;
        private String usernameConfKey;
        private String keytabConfKey;
        private boolean needsClientAuth;
        private String trustStore;
        private String trustStorePassword;
        private String trustStoreType;
        private String keyStore;
        private String keyStorePassword;
        private String keyStoreType;
        private String keyPassword;
        private boolean findPort;
        private String hostName;
        
        public Builder() {
            this.endpoints = (ArrayList<URI>)Lists.newArrayList();
            this.securityEnabled = false;
        }
        
        public Builder setName(final String name) {
            this.name = name;
            return this;
        }
        
        public Builder addEndpoint(final URI endpoint) {
            this.endpoints.add(endpoint);
            return this;
        }
        
        public Builder hostName(final String hostName) {
            this.hostName = hostName;
            return this;
        }
        
        public Builder trustStore(final String location, final String password, final String type) {
            this.trustStore = location;
            this.trustStorePassword = password;
            this.trustStoreType = type;
            return this;
        }
        
        public Builder keyStore(final String location, final String password, final String type) {
            this.keyStore = location;
            this.keyStorePassword = password;
            this.keyStoreType = type;
            return this;
        }
        
        public Builder keyPassword(final String password) {
            this.keyPassword = password;
            return this;
        }
        
        public Builder needsClientAuth(final boolean value) {
            this.needsClientAuth = value;
            return this;
        }
        
        public Builder setFindPort(final boolean findPort) {
            this.findPort = findPort;
            return this;
        }
        
        public Builder setConf(final Configuration conf) {
            this.conf = conf;
            return this;
        }
        
        public Builder setConnector(final Connector connector) {
            this.connector = connector;
            return this;
        }
        
        public Builder setPathSpec(final String[] pathSpec) {
            this.pathSpecs = pathSpec;
            return this;
        }
        
        public Builder setACL(final AccessControlList acl) {
            this.adminsAcl = acl;
            return this;
        }
        
        public Builder setSecurityEnabled(final boolean securityEnabled) {
            this.securityEnabled = securityEnabled;
            return this;
        }
        
        public Builder setUsernameConfKey(final String usernameConfKey) {
            this.usernameConfKey = usernameConfKey;
            return this;
        }
        
        public Builder setKeytabConfKey(final String keytabConfKey) {
            this.keytabConfKey = keytabConfKey;
            return this;
        }
        
        public HttpServer2 build() throws IOException {
            if (this.name == null) {
                throw new HadoopIllegalArgumentException("name is not set");
            }
            if (this.endpoints.size() == 0 && this.connector == null) {
                throw new HadoopIllegalArgumentException("No endpoints specified");
            }
            if (this.hostName == null) {
                this.hostName = ((this.endpoints.size() == 0) ? this.connector.getHost() : this.endpoints.get(0).getHost());
            }
            if (this.conf == null) {
                this.conf = new Configuration();
            }
            final HttpServer2 server = new HttpServer2(this, null);
            if (this.securityEnabled) {
                server.initSpnego(this.conf, this.hostName, this.usernameConfKey, this.keytabConfKey);
            }
            if (this.connector != null) {
                server.addUnmanagedListener(this.connector);
            }
            for (final URI ep : this.endpoints) {
                Connector listener = null;
                final String scheme = ep.getScheme();
                if ("http".equals(scheme)) {
                    listener = HttpServer2.createDefaultChannelConnector();
                }
                else {
                    if (!"https".equals(scheme)) {
                        throw new HadoopIllegalArgumentException("unknown scheme for endpoint:" + ep);
                    }
                    final SslSocketConnector c = new SslSocketConnectorSecure();
                    c.setNeedClientAuth(this.needsClientAuth);
                    c.setKeyPassword(this.keyPassword);
                    if (this.keyStore != null) {
                        c.setKeystore(this.keyStore);
                        c.setKeystoreType(this.keyStoreType);
                        c.setPassword(this.keyStorePassword);
                    }
                    if (this.trustStore != null) {
                        c.setTruststore(this.trustStore);
                        c.setTruststoreType(this.trustStoreType);
                        c.setTrustPassword(this.trustStorePassword);
                    }
                    listener = (Connector)c;
                }
                listener.setHost(ep.getHost());
                listener.setPort((ep.getPort() == -1) ? 0 : ep.getPort());
                server.addManagedListener(listener);
            }
            server.loadListeners();
            return server;
        }
    }
    
    private static class SelectChannelConnectorWithSafeStartup extends SelectChannelConnector
    {
        public SelectChannelConnectorWithSafeStartup() {
        }
        
        public boolean isRunning() {
            if (super.isRunning()) {
                return true;
            }
            HttpServer2.LOG.warn((Object)"HttpServer Acceptor: isRunning is false. Rechecking.");
            try {
                Thread.sleep(10L);
            }
            catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            final boolean runState = super.isRunning();
            HttpServer2.LOG.warn((Object)("HttpServer Acceptor: isRunning is " + runState));
            return runState;
        }
    }
    
    public static class StackServlet extends HttpServlet
    {
        private static final long serialVersionUID = -6284183679759467039L;
        
        public void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
            if (!HttpServer2.isInstrumentationAccessAllowed(this.getServletContext(), request, response)) {
                return;
            }
            response.setContentType("text/plain; charset=UTF-8");
            final PrintWriter out = response.getWriter();
            ReflectionUtils.printThreadInfo(out, "");
            out.close();
            ReflectionUtils.logThreadInfo(HttpServer2.LOG, "jsp requested", 1L);
        }
    }
    
    public static class QuotingInputFilter implements Filter
    {
        private FilterConfig config;
        
        public void init(final FilterConfig config) throws ServletException {
            this.config = config;
        }
        
        public void destroy() {
        }
        
        public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
            final HttpServletRequestWrapper quoted = new RequestQuoter((HttpServletRequest)request);
            final HttpServletResponse httpResponse = (HttpServletResponse)response;
            final String mime = this.inferMimeType(request);
            if (mime == null) {
                httpResponse.setContentType("text/plain; charset=utf-8");
            }
            else if (mime.startsWith("text/html")) {
                httpResponse.setContentType("text/html; charset=utf-8");
            }
            else if (mime.startsWith("application/xml")) {
                httpResponse.setContentType("text/xml; charset=utf-8");
            }
            chain.doFilter((ServletRequest)quoted, (ServletResponse)httpResponse);
        }
        
        private String inferMimeType(final ServletRequest request) {
            final String path = ((HttpServletRequest)request).getRequestURI();
            final ContextHandler.SContext sContext = (ContextHandler.SContext)this.config.getServletContext();
            final MimeTypes mimes = sContext.getContextHandler().getMimeTypes();
            final Buffer mimeBuffer = mimes.getMimeByExtension(path);
            return (mimeBuffer == null) ? null : mimeBuffer.toString();
        }
        
        public static class RequestQuoter extends HttpServletRequestWrapper
        {
            private final HttpServletRequest rawRequest;
            
            public RequestQuoter(final HttpServletRequest rawRequest) {
                super(rawRequest);
                this.rawRequest = rawRequest;
            }
            
            public Enumeration<String> getParameterNames() {
                return new Enumeration<String>() {
                    private Enumeration<String> rawIterator = RequestQuoter.this.rawRequest.getParameterNames();
                    
                    @Override
                    public boolean hasMoreElements() {
                        return this.rawIterator.hasMoreElements();
                    }
                    
                    @Override
                    public String nextElement() {
                        return HtmlQuoting.quoteHtmlChars(this.rawIterator.nextElement());
                    }
                };
            }
            
            public String getParameter(final String name) {
                return HtmlQuoting.quoteHtmlChars(this.rawRequest.getParameter(HtmlQuoting.unquoteHtmlChars(name)));
            }
            
            public String[] getParameterValues(final String name) {
                final String unquoteName = HtmlQuoting.unquoteHtmlChars(name);
                final String[] unquoteValue = this.rawRequest.getParameterValues(unquoteName);
                if (unquoteValue == null) {
                    return null;
                }
                final String[] result = new String[unquoteValue.length];
                for (int i = 0; i < result.length; ++i) {
                    result[i] = HtmlQuoting.quoteHtmlChars(unquoteValue[i]);
                }
                return result;
            }
            
            public Map<String, String[]> getParameterMap() {
                final Map<String, String[]> result = new HashMap<String, String[]>();
                final Map<String, String[]> raw = (Map<String, String[]>)this.rawRequest.getParameterMap();
                for (final Map.Entry<String, String[]> item : raw.entrySet()) {
                    final String[] rawValue = item.getValue();
                    final String[] cookedValue = new String[rawValue.length];
                    for (int i = 0; i < rawValue.length; ++i) {
                        cookedValue[i] = HtmlQuoting.quoteHtmlChars(rawValue[i]);
                    }
                    result.put(HtmlQuoting.quoteHtmlChars(item.getKey()), cookedValue);
                }
                return result;
            }
            
            public StringBuffer getRequestURL() {
                final String url = this.rawRequest.getRequestURL().toString();
                return new StringBuffer(HtmlQuoting.quoteHtmlChars(url));
            }
            
            public String getServerName() {
                return HtmlQuoting.quoteHtmlChars(this.rawRequest.getServerName());
            }
        }
    }
}
