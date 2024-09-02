// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.hadoop.http;

import java.util.regex.Matcher;
import java.util.Enumeration;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.FilterChain;
import javax.servlet.ServletResponse;
import javax.servlet.ServletRequest;
import javax.servlet.FilterConfig;
import javax.servlet.Filter;
import javax.servlet.ServletException;
import java.io.OutputStream;
import java.io.PrintStream;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.http.HttpVersion;
import org.apache.hadoop.util.StringUtils;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.ConnectionFactory;
import org.apache.hadoop.util.Shell;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.apache.hadoop.HadoopIllegalArgumentException;
import org.eclipse.jetty.server.HttpConfiguration;
import java.net.URI;
import org.slf4j.LoggerFactory;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.net.BindException;
import org.eclipse.jetty.server.Connector;
import java.io.InterruptedIOException;
import org.eclipse.jetty.util.MultiException;
import org.apache.hadoop.security.SecurityUtil;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.FileNotFoundException;
import java.util.Iterator;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.FilterMapping;
import org.apache.hadoop.security.UserGroupInformation;
import org.eclipse.jetty.util.ArrayUtil;
import org.eclipse.jetty.servlet.ServletMapping;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import org.apache.hadoop.conf.ConfServlet;
import org.apache.hadoop.jmx.JMXJsonServlet;
import org.apache.hadoop.log.LogLevel;
import javax.servlet.http.HttpServlet;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.server.HandlerContainer;
import org.apache.hadoop.util.ReflectionUtils;
import java.util.Collections;
import org.apache.hadoop.security.AuthenticationFilterInitializer;
import java.util.Properties;
import org.apache.hadoop.security.authentication.server.AuthenticationFilter;
import java.io.File;
import com.google.common.collect.ImmutableMap;
import javax.servlet.Servlet;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.SessionManager;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.session.AbstractSessionManager;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import com.google.common.base.Preconditions;
import java.io.IOException;
import javax.servlet.ServletContext;
import java.util.ArrayList;
import java.util.HashMap;
import com.google.common.collect.Lists;
import java.util.regex.Pattern;
import org.apache.hadoop.security.authentication.util.SignerSecretProvider;
import org.eclipse.jetty.servlet.ServletContextHandler;
import java.util.Map;
import org.apache.hadoop.conf.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.server.ServerConnector;
import java.util.List;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.Server;
import org.apache.hadoop.security.authorize.AccessControlList;
import org.slf4j.Logger;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.classification.InterfaceAudience;

@InterfaceAudience.Private
@InterfaceStability.Evolving
public final class HttpServer2 implements FilterContainer
{
    public static final Logger LOG;
    public static final String HTTP_SCHEME = "http";
    public static final String HTTPS_SCHEME = "https";
    public static final String HTTP_MAX_REQUEST_HEADER_SIZE_KEY = "hadoop.http.max.request.header.size";
    public static final int HTTP_MAX_REQUEST_HEADER_SIZE_DEFAULT = 65536;
    public static final String HTTP_MAX_RESPONSE_HEADER_SIZE_KEY = "hadoop.http.max.response.header.size";
    public static final int HTTP_MAX_RESPONSE_HEADER_SIZE_DEFAULT = 65536;
    public static final String HTTP_SOCKET_BACKLOG_SIZE_KEY = "hadoop.http.socket.backlog.size";
    public static final int HTTP_SOCKET_BACKLOG_SIZE_DEFAULT = 128;
    public static final String HTTP_MAX_THREADS_KEY = "hadoop.http.max.threads";
    public static final String HTTP_ACCEPTOR_COUNT_KEY = "hadoop.http.acceptor.count";
    public static final int HTTP_ACCEPTOR_COUNT_DEFAULT = -1;
    public static final String HTTP_SELECTOR_COUNT_KEY = "hadoop.http.selector.count";
    public static final int HTTP_SELECTOR_COUNT_DEFAULT = -1;
    public static final String HTTP_IDLE_TIMEOUT_MS_KEY = "hadoop.http.idle_timeout.ms";
    public static final int HTTP_IDLE_TIMEOUT_MS_DEFAULT = 10000;
    public static final String HTTP_TEMP_DIR_KEY = "hadoop.http.temp.dir";
    public static final String FILTER_INITIALIZER_PROPERTY = "hadoop.http.filter.initializers";
    public static final String CONF_CONTEXT_ATTRIBUTE = "hadoop.conf";
    public static final String ADMINS_ACL = "admins.acl";
    public static final String SPNEGO_FILTER = "SpnegoFilter";
    public static final String NO_CACHE_FILTER = "NoCacheFilter";
    public static final String BIND_ADDRESS = "bind.address";
    private final AccessControlList adminsAcl;
    protected final Server webServer;
    private final HandlerCollection handlers;
    private final List<ServerConnector> listeners;
    protected final WebAppContext webAppContext;
    protected final boolean findPort;
    protected final Configuration.IntegerRanges portRanges;
    private final Map<ServletContextHandler, Boolean> defaultContexts;
    protected final List<String> filterNames;
    static final String STATE_DESCRIPTION_ALIVE = " - alive";
    static final String STATE_DESCRIPTION_NOT_LIVE = " - not live";
    private final SignerSecretProvider secretProvider;
    private XFrameOption xFrameOption;
    private boolean xFrameOptionIsEnabled;
    public static final String HTTP_HEADER_PREFIX = "hadoop.http.header.";
    private static final String HTTP_HEADER_REGEX = "hadoop\\.http\\.header\\.([a-zA-Z\\-_]+)";
    static final String X_XSS_PROTECTION = "X-XSS-Protection:1; mode=block";
    static final String X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options:nosniff";
    private static final String X_FRAME_OPTIONS = "X-FRAME-OPTIONS";
    private static final Pattern PATTERN_HTTP_HEADER_REGEX;
    
    private HttpServer2(final Builder b) throws IOException {
        this.listeners = (List<ServerConnector>)Lists.newArrayList();
        this.defaultContexts = new HashMap<ServletContextHandler, Boolean>();
        this.filterNames = new ArrayList<String>();
        final String appDir = this.getWebAppsPath(b.name);
        this.webServer = new Server();
        this.adminsAcl = b.adminsAcl;
        this.handlers = new HandlerCollection();
        this.webAppContext = createWebAppContext(b, this.adminsAcl, appDir);
        this.xFrameOptionIsEnabled = b.xFrameEnabled;
        this.xFrameOption = b.xFrameOption;
        try {
            this.secretProvider = constructSecretProvider(b, (ServletContext)this.webAppContext.getServletContext());
            this.webAppContext.getServletContext().setAttribute("signer.secret.provider.object", (Object)this.secretProvider);
        }
        catch (IOException e) {
            throw e;
        }
        catch (Exception e2) {
            throw new IOException(e2);
        }
        this.findPort = b.findPort;
        this.portRanges = b.portRanges;
        this.initializeWebServer(b.name, b.hostName, b.conf, b.pathSpecs);
    }
    
    private void initializeWebServer(final String name, final String hostName, Configuration conf, final String[] pathSpecs) throws IOException {
        Preconditions.checkNotNull((Object)this.webAppContext);
        final int maxThreads = conf.getInt("hadoop.http.max.threads", -1);
        final QueuedThreadPool threadPool = (QueuedThreadPool)this.webServer.getThreadPool();
        threadPool.setDaemon(true);
        if (maxThreads != -1) {
            threadPool.setMaxThreads(maxThreads);
        }
        final SessionManager sm = this.webAppContext.getSessionHandler().getSessionManager();
        if (sm instanceof AbstractSessionManager) {
            final AbstractSessionManager asm = (AbstractSessionManager)sm;
            asm.setHttpOnly(true);
            asm.getSessionCookieConfig().setSecure(true);
        }
        final ContextHandlerCollection contexts = new ContextHandlerCollection();
        final RequestLog requestLog = HttpRequestLog.getRequestLog(name);
        this.handlers.addHandler((Handler)contexts);
        if (requestLog != null) {
            final RequestLogHandler requestLogHandler = new RequestLogHandler();
            requestLogHandler.setRequestLog(requestLog);
            this.handlers.addHandler((Handler)requestLogHandler);
        }
        this.handlers.addHandler((Handler)this.webAppContext);
        final String appDir = this.getWebAppsPath(name);
        this.addDefaultApps(contexts, appDir, conf);
        this.webServer.setHandler((Handler)this.handlers);
        final Map<String, String> xFrameParams = this.setHeaders(conf);
        this.addGlobalFilter("safety", QuotingInputFilter.class.getName(), xFrameParams);
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
                HttpServer2.LOG.info("adding path spec: " + path);
                this.addFilterPathMapping(path, (ServletContextHandler)this.webAppContext);
            }
        }
    }
    
    private void addListener(final ServerConnector connector) {
        this.listeners.add(connector);
    }
    
    private static WebAppContext createWebAppContext(final Builder b, final AccessControlList adminsAcl, final String appDir) {
        final WebAppContext ctx = new WebAppContext();
        ctx.setDefaultsDescriptor((String)null);
        final ServletHolder holder = new ServletHolder((Servlet)new DefaultServlet());
        final Map<String, String> params = (Map<String, String>)ImmutableMap.builder().put((Object)"acceptRanges", (Object)"true").put((Object)"dirAllowed", (Object)"false").put((Object)"gzip", (Object)"true").put((Object)"useFileMappedBuffer", (Object)"true").build();
        holder.setInitParameters((Map)params);
        ctx.setWelcomeFiles(new String[] { "index.html" });
        ctx.addServlet(holder, "/");
        ctx.setDisplayName(b.name);
        ctx.setContextPath("/");
        ctx.setWar(appDir + "/" + b.name);
        final String tempDirectory = b.conf.get("hadoop.http.temp.dir");
        if (tempDirectory != null && !tempDirectory.isEmpty()) {
            ctx.setTempDirectory(new File(tempDirectory));
            ctx.setAttribute("javax.servlet.context.tempdir", (Object)tempDirectory);
        }
        ctx.getServletContext().setAttribute("hadoop.conf", (Object)b.conf);
        ctx.getServletContext().setAttribute("admins.acl", (Object)adminsAcl);
        addNoCacheFilter((ServletContextHandler)ctx);
        return ctx;
    }
    
    private static SignerSecretProvider constructSecretProvider(final Builder b, final ServletContext ctx) throws Exception {
        final Configuration conf = b.conf;
        final Properties config = getFilterProperties(conf, b.authFilterConfigurationPrefix);
        return AuthenticationFilter.constructSecretProvider(ctx, config, b.disallowFallbackToRandomSignerSecretProvider);
    }
    
    private static Properties getFilterProperties(final Configuration conf, final String prefix) {
        final Properties prop = new Properties();
        final Map<String, String> filterConfig = AuthenticationFilterInitializer.getFilterConfigMap(conf, prefix);
        prop.putAll(filterConfig);
        return prop;
    }
    
    private static void addNoCacheFilter(final ServletContextHandler ctxt) {
        defineFilter(ctxt, "NoCacheFilter", NoCacheFilter.class.getName(), Collections.emptyMap(), new String[] { "/*" });
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
        final boolean logsEnabled = conf.getBoolean("hadoop.http.logs.enabled", true);
        if (logDir != null && logsEnabled) {
            final ServletContextHandler logContext = new ServletContextHandler((HandlerContainer)parent, "/logs");
            logContext.setResourceBase(logDir);
            logContext.addServlet((Class)AdminAuthorizedServlet.class, "/*");
            if (conf.getBoolean("hadoop.jetty.logs.serve.aliases", true)) {
                final Map<String, String> params = (Map<String, String>)logContext.getInitParams();
                params.put("org.eclipse.jetty.servlet.Default.aliases", "true");
            }
            logContext.setDisplayName("logs");
            final SessionHandler handler = new SessionHandler();
            final SessionManager sm = handler.getSessionManager();
            if (sm instanceof AbstractSessionManager) {
                final AbstractSessionManager asm = (AbstractSessionManager)sm;
                asm.setHttpOnly(true);
                asm.getSessionCookieConfig().setSecure(true);
            }
            logContext.setSessionHandler(handler);
            this.setContextAttributes(logContext, conf);
            addNoCacheFilter(logContext);
            this.defaultContexts.put(logContext, true);
        }
        final ServletContextHandler staticContext = new ServletContextHandler((HandlerContainer)parent, "/static");
        staticContext.setResourceBase(appDir + "/static");
        staticContext.addServlet((Class)DefaultServlet.class, "/*");
        staticContext.setDisplayName("static");
        final Map<String, String> params = (Map<String, String>)staticContext.getInitParams();
        params.put("org.eclipse.jetty.servlet.Default.dirAllowed", "false");
        params.put("org.eclipse.jetty.servlet.Default.gzip", "true");
        final SessionHandler handler2 = new SessionHandler();
        final SessionManager sm2 = handler2.getSessionManager();
        if (sm2 instanceof AbstractSessionManager) {
            final AbstractSessionManager asm2 = (AbstractSessionManager)sm2;
            asm2.setHttpOnly(true);
            asm2.getSessionCookieConfig().setSecure(true);
        }
        staticContext.setSessionHandler(handler2);
        this.setContextAttributes(staticContext, conf);
        this.defaultContexts.put(staticContext, true);
    }
    
    private void setContextAttributes(final ServletContextHandler context, final Configuration conf) {
        context.getServletContext().setAttribute("hadoop.conf", (Object)conf);
        context.getServletContext().setAttribute("admins.acl", (Object)this.adminsAcl);
    }
    
    protected void addDefaultServlets() {
        this.addServlet("stacks", "/stacks", StackServlet.class);
        this.addServlet("logLevel", "/logLevel", LogLevel.Servlet.class);
        this.addServlet("jmx", "/jmx", JMXJsonServlet.class);
        this.addServlet("conf", "/conf", ConfServlet.class);
    }
    
    public void addContext(final ServletContextHandler ctxt, final boolean isFiltered) {
        this.handlers.addHandler((Handler)ctxt);
        addNoCacheFilter(ctxt);
        this.defaultContexts.put(ctxt, isFiltered);
    }
    
    public void setAttribute(final String name, final Object value) {
        this.webAppContext.setAttribute(name, value);
    }
    
    public void addJerseyResourcePackage(final String packageName, final String pathSpec) {
        HttpServer2.LOG.info("addJerseyResourcePackage: packageName=" + packageName + ", pathSpec=" + pathSpec);
        final ServletHolder sh = new ServletHolder((Class)ServletContainer.class);
        sh.setInitParameter("com.sun.jersey.config.property.resourceConfigClass", "com.sun.jersey.api.core.PackagesResourceConfig");
        sh.setInitParameter("com.sun.jersey.config.property.packages", packageName);
        this.webAppContext.addServlet(sh, pathSpec);
    }
    
    public void addServlet(final String name, final String pathSpec, final Class<? extends HttpServlet> clazz) {
        this.addInternalServlet(name, pathSpec, clazz, false);
        this.addFilterPathMapping(pathSpec, (ServletContextHandler)this.webAppContext);
    }
    
    public void addInternalServlet(final String name, final String pathSpec, final Class<? extends HttpServlet> clazz) {
        this.addInternalServlet(name, pathSpec, clazz, false);
    }
    
    public void addInternalServlet(final String name, final String pathSpec, final Class<? extends HttpServlet> clazz, final boolean requireAuth) {
        final ServletHolder holder = new ServletHolder((Class)clazz);
        if (name != null) {
            holder.setName(name);
        }
        final ServletMapping[] servletMappings = this.webAppContext.getServletHandler().getServletMappings();
        for (int i = 0; i < servletMappings.length; ++i) {
            if (servletMappings[i].containsPathSpec(pathSpec)) {
                if (HttpServer2.LOG.isDebugEnabled()) {
                    HttpServer2.LOG.debug("Found existing " + servletMappings[i].getServletName() + " servlet at path " + pathSpec + "; will replace mapping with " + holder.getName() + " servlet");
                }
                final ServletMapping[] newServletMappings = (ServletMapping[])ArrayUtil.removeFromArray((Object[])servletMappings, (Object)servletMappings[i]);
                this.webAppContext.getServletHandler().setServletMappings(newServletMappings);
                break;
            }
        }
        this.webAppContext.addServlet(holder, pathSpec);
        if (requireAuth && UserGroupInformation.isSecurityEnabled()) {
            HttpServer2.LOG.info("Adding Kerberos (SPNEGO) filter to " + name);
            final ServletHandler handler = this.webAppContext.getServletHandler();
            final FilterMapping fmap = new FilterMapping();
            fmap.setPathSpec(pathSpec);
            fmap.setFilterName("SpnegoFilter");
            fmap.setDispatches(31);
            handler.addFilterMapping(fmap);
        }
    }
    
    public void addInternalServlet(final String name, final String pathSpec, final Class<? extends HttpServlet> clazz, final Map<String, String> params) {
        final ServletHolder sh = new ServletHolder((Class)clazz);
        sh.setName(name);
        sh.setInitParameters((Map)params);
        final ServletMapping[] servletMappings = this.webAppContext.getServletHandler().getServletMappings();
        for (int i = 0; i < servletMappings.length; ++i) {
            if (servletMappings[i].containsPathSpec(pathSpec)) {
                if (HttpServer2.LOG.isDebugEnabled()) {
                    HttpServer2.LOG.debug("Found existing " + servletMappings[i].getServletName() + " servlet at path " + pathSpec + "; will replace mapping with " + sh.getName() + " servlet");
                }
                final ServletMapping[] newServletMappings = (ServletMapping[])ArrayUtil.removeFromArray((Object[])servletMappings, (Object)servletMappings[i]);
                this.webAppContext.getServletHandler().setServletMappings(newServletMappings);
                break;
            }
        }
        this.webAppContext.addServlet(sh, pathSpec);
    }
    
    public void addHandlerAtFront(final Handler handler) {
        final Handler[] h = (Handler[])ArrayUtil.prependToArray((Object)handler, (Object[])this.handlers.getHandlers(), (Class)Handler.class);
        this.handlers.setHandlers(h);
    }
    
    public void addHandlerAtEnd(final Handler handler) {
        this.handlers.addHandler(handler);
    }
    
    @Override
    public void addFilter(final String name, final String classname, final Map<String, String> parameters) {
        final FilterHolder filterHolder = getFilterHolder(name, classname, parameters);
        final String[] USER_FACING_URLS = { "*.html", "*.jsp" };
        FilterMapping fmap = getFilterMapping(name, USER_FACING_URLS);
        defineFilter((ServletContextHandler)this.webAppContext, filterHolder, fmap);
        HttpServer2.LOG.info("Added filter " + name + " (class=" + classname + ") to context " + this.webAppContext.getDisplayName());
        final String[] ALL_URLS = { "/*" };
        fmap = getFilterMapping(name, ALL_URLS);
        for (final Map.Entry<ServletContextHandler, Boolean> e : this.defaultContexts.entrySet()) {
            if (e.getValue()) {
                final ServletContextHandler ctx = e.getKey();
                defineFilter(ctx, filterHolder, fmap);
                HttpServer2.LOG.info("Added filter " + name + " (class=" + classname + ") to context " + ctx.getDisplayName());
            }
        }
        this.filterNames.add(name);
    }
    
    @Override
    public void addGlobalFilter(final String name, final String classname, final Map<String, String> parameters) {
        final String[] ALL_URLS = { "/*" };
        final FilterHolder filterHolder = getFilterHolder(name, classname, parameters);
        final FilterMapping fmap = getFilterMapping(name, ALL_URLS);
        defineFilter((ServletContextHandler)this.webAppContext, filterHolder, fmap);
        for (final ServletContextHandler ctx : this.defaultContexts.keySet()) {
            defineFilter(ctx, filterHolder, fmap);
        }
        HttpServer2.LOG.info("Added global filter '" + name + "' (class=" + classname + ")");
    }
    
    public static void defineFilter(final ServletContextHandler ctx, final String name, final String classname, final Map<String, String> parameters, final String[] urls) {
        final FilterHolder filterHolder = getFilterHolder(name, classname, parameters);
        final FilterMapping fmap = getFilterMapping(name, urls);
        defineFilter(ctx, filterHolder, fmap);
    }
    
    private static void defineFilter(final ServletContextHandler ctx, final FilterHolder holder, final FilterMapping fmap) {
        final ServletHandler handler = ctx.getServletHandler();
        handler.addFilter(holder, fmap);
    }
    
    private static FilterMapping getFilterMapping(final String name, final String[] urls) {
        final FilterMapping fmap = new FilterMapping();
        fmap.setPathSpecs(urls);
        fmap.setDispatches(31);
        fmap.setFilterName(name);
        return fmap;
    }
    
    private static FilterHolder getFilterHolder(final String name, final String classname, final Map<String, String> parameters) {
        final FilterHolder holder = new FilterHolder();
        holder.setName(name);
        holder.setClassName(classname);
        if (parameters != null) {
            holder.setInitParameters((Map)parameters);
        }
        return holder;
    }
    
    protected void addFilterPathMapping(final String pathSpec, final ServletContextHandler webAppCtx) {
        final ServletHandler handler = webAppCtx.getServletHandler();
        for (final String name : this.filterNames) {
            final FilterMapping fmap = new FilterMapping();
            fmap.setPathSpec(pathSpec);
            fmap.setFilterName(name);
            fmap.setDispatches(31);
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
        URL resourceUrl = null;
        final File webResourceDevLocation = new File("src/main/webapps", appName);
        Label_0149: {
            if (webResourceDevLocation.exists()) {
                HttpServer2.LOG.info("Web server is in development mode. Resources will be read from the source tree.");
                try {
                    resourceUrl = webResourceDevLocation.getParentFile().toURI().toURL();
                    break Label_0149;
                }
                catch (MalformedURLException e) {
                    throw new FileNotFoundException("Mailformed URL while finding the web resource dir:" + e.getMessage());
                }
            }
            resourceUrl = this.getClass().getClassLoader().getResource("webapps/" + appName);
            if (resourceUrl == null) {
                throw new FileNotFoundException("webapps/" + appName + " not found in CLASSPATH");
            }
        }
        final String urlString = resourceUrl.toString();
        return urlString.substring(0, urlString.lastIndexOf(47));
    }
    
    @Deprecated
    public int getPort() {
        return ((ServerConnector)this.webServer.getConnectors()[0]).getLocalPort();
    }
    
    public InetSocketAddress getConnectorAddress(final int index) {
        Preconditions.checkArgument(index >= 0);
        if (index > this.webServer.getConnectors().length) {
            return null;
        }
        final ServerConnector c = (ServerConnector)this.webServer.getConnectors()[index];
        if (c.getLocalPort() == -1 || c.getLocalPort() == -2) {
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
        defineFilter((ServletContextHandler)this.webAppContext, "SpnegoFilter", AuthenticationFilter.class.getName(), params, null);
    }
    
    public void start() throws IOException {
        try {
            try {
                this.openListeners();
                this.webServer.start();
            }
            catch (IOException ex) {
                HttpServer2.LOG.info("HttpServer.start() threw a non Bind IOException", (Throwable)ex);
                throw ex;
            }
            catch (MultiException ex2) {
                HttpServer2.LOG.info("HttpServer.start() threw a MultiException", (Throwable)ex2);
                throw ex2;
            }
            final Handler[] handlers;
            final Handler[] hs = handlers = this.webServer.getHandlers();
            for (final Handler handler : handlers) {
                if (handler.isFailed()) {
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
        for (final Connector c : this.listeners) {
            this.webServer.addConnector(c);
        }
    }
    
    private static void bindListener(final ServerConnector listener) throws Exception {
        listener.close();
        listener.open();
        HttpServer2.LOG.info("Jetty bound to port " + listener.getLocalPort());
    }
    
    private static BindException constructBindException(final ServerConnector listener, final BindException ex) {
        final BindException be = new BindException("Port in use: " + listener.getHost() + ":" + listener.getPort());
        if (ex != null) {
            be.initCause(ex);
        }
        return be;
    }
    
    private void bindForSinglePort(final ServerConnector listener, int port) throws Exception {
        while (true) {
            try {
                bindListener(listener);
            }
            catch (BindException ex) {
                if (port == 0 || !this.findPort) {
                    throw constructBindException(listener, ex);
                }
                listener.setPort(++port);
                Thread.sleep(100L);
                continue;
            }
            break;
        }
    }
    
    private void bindForPortRange(final ServerConnector listener, final int startPort) throws Exception {
        BindException bindException = null;
        try {
            bindListener(listener);
        }
        catch (BindException ex) {
            bindException = ex;
            for (final Integer port : this.portRanges) {
                if (port == startPort) {
                    continue;
                }
                Thread.sleep(100L);
                listener.setPort((int)port);
                try {
                    bindListener(listener);
                    return;
                }
                catch (BindException ex2) {
                    bindException = ex2;
                    continue;
                }
                break;
            }
            throw constructBindException(listener, bindException);
        }
    }
    
    void openListeners() throws Exception {
        HttpServer2.LOG.debug("opening listeners: {}", (Object)this.listeners);
        for (final ServerConnector listener : this.listeners) {
            if (listener.getLocalPort() != -1 && listener.getLocalPort() != -2) {
                continue;
            }
            final int port = listener.getPort();
            if (this.portRanges != null && port != 0) {
                this.bindForPortRange(listener, port);
            }
            else {
                this.bindForSinglePort(listener, port);
            }
        }
    }
    
    public void stop() throws Exception {
        MultiException exception = null;
        for (final ServerConnector c : this.listeners) {
            try {
                c.close();
            }
            catch (Exception e) {
                HttpServer2.LOG.error("Error while stopping listener for webapp" + this.webAppContext.getDisplayName(), (Throwable)e);
                exception = this.addMultiException(exception, e);
            }
        }
        try {
            this.secretProvider.destroy();
            this.webAppContext.clearAttributes();
            this.webAppContext.stop();
        }
        catch (Exception e2) {
            HttpServer2.LOG.error("Error while stopping web app context for webapp " + this.webAppContext.getDisplayName(), (Throwable)e2);
            exception = this.addMultiException(exception, e2);
        }
        try {
            this.webServer.stop();
        }
        catch (Exception e2) {
            HttpServer2.LOG.error("Error while stopping web server for webapp " + this.webAppContext.getDisplayName(), (Throwable)e2);
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
        Preconditions.checkState(!this.listeners.isEmpty());
        final StringBuilder sb = new StringBuilder("HttpServer (").append(this.isAlive() ? " - alive" : " - not live").append("), listening at:");
        for (final ServerConnector l : this.listeners) {
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
            response.sendError(403, "Unauthenticated users are not authorized to access this page.");
            return false;
        }
        if (servletContext.getAttribute("admins.acl") != null && !userHasAdministratorAccess(servletContext, remoteUser)) {
            response.sendError(403, "Unauthenticated users are not authorized to access this page.");
            HttpServer2.LOG.warn("User " + remoteUser + " is unauthorized to access the page " + request.getRequestURI() + ".");
            return false;
        }
        return true;
    }
    
    public static boolean userHasAdministratorAccess(final ServletContext servletContext, final String remoteUser) {
        final AccessControlList adminsAcl = (AccessControlList)servletContext.getAttribute("admins.acl");
        final UserGroupInformation remoteUserUGI = UserGroupInformation.createRemoteUser(remoteUser);
        return adminsAcl != null && adminsAcl.isUserAllowed(remoteUserUGI);
    }
    
    private Map<String, String> setHeaders(final Configuration conf) {
        final Map<String, String> xFrameParams = new HashMap<String, String>();
        final Map<String, String> headerConfigMap = conf.getValByRegex("hadoop\\.http\\.header\\.([a-zA-Z\\-_]+)");
        xFrameParams.putAll(this.getDefaultHeaders());
        if (this.xFrameOptionIsEnabled) {
            xFrameParams.put("hadoop.http.header.X-FRAME-OPTIONS", this.xFrameOption.toString());
        }
        xFrameParams.putAll(headerConfigMap);
        return xFrameParams;
    }
    
    private Map<String, String> getDefaultHeaders() {
        final Map<String, String> headers = new HashMap<String, String>();
        String[] splitVal = "X-Content-Type-Options:nosniff".split(":");
        headers.put("hadoop.http.header." + splitVal[0], splitVal[1]);
        splitVal = "X-XSS-Protection:1; mode=block".split(":");
        headers.put("hadoop.http.header." + splitVal[0], splitVal[1]);
        return headers;
    }
    
    static {
        LOG = LoggerFactory.getLogger((Class)HttpServer2.class);
        PATTERN_HTTP_HEADER_REGEX = Pattern.compile("hadoop\\.http\\.header\\.([a-zA-Z\\-_]+)");
    }
    
    public static class Builder
    {
        private ArrayList<URI> endpoints;
        private String name;
        private Configuration conf;
        private Configuration sslConf;
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
        private Configuration.IntegerRanges portRanges;
        private String hostName;
        private boolean disallowFallbackToRandomSignerSecretProvider;
        private String authFilterConfigurationPrefix;
        private String excludeCiphers;
        private boolean xFrameEnabled;
        private XFrameOption xFrameOption;
        
        public Builder() {
            this.endpoints = (ArrayList<URI>)Lists.newArrayList();
            this.securityEnabled = false;
            this.portRanges = null;
            this.authFilterConfigurationPrefix = "hadoop.http.authentication.";
            this.xFrameOption = XFrameOption.SAMEORIGIN;
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
        
        public Builder setPortRanges(final Configuration.IntegerRanges ranges) {
            this.portRanges = ranges;
            return this;
        }
        
        public Builder setConf(final Configuration conf) {
            this.conf = conf;
            return this;
        }
        
        public Builder setSSLConf(final Configuration sslCnf) {
            this.sslConf = sslCnf;
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
        
        public Builder disallowFallbackToRandomSingerSecretProvider(final boolean value) {
            this.disallowFallbackToRandomSignerSecretProvider = value;
            return this;
        }
        
        public Builder authFilterConfigurationPrefix(final String value) {
            this.authFilterConfigurationPrefix = value;
            return this;
        }
        
        public Builder excludeCiphers(final String pExcludeCiphers) {
            this.excludeCiphers = pExcludeCiphers;
            return this;
        }
        
        public Builder configureXFrame(final boolean xFrameEnabled) {
            this.xFrameEnabled = xFrameEnabled;
            return this;
        }
        
        public Builder setXFrameOption(final String option) {
            this.xFrameOption = getEnum(option);
            return this;
        }
        
        private static String getPasswordString(final Configuration conf, final String name) throws IOException {
            final char[] passchars = conf.getPassword(name);
            if (passchars == null) {
                return null;
            }
            return new String(passchars);
        }
        
        private void loadSSLConfiguration() throws IOException {
            if (this.sslConf == null) {
                return;
            }
            this.needsClientAuth = this.sslConf.getBoolean("ssl.server.need.client.auth", false);
            this.keyStore = this.sslConf.getTrimmed("ssl.server.keystore.location");
            if (this.keyStore == null || this.keyStore.isEmpty()) {
                throw new IOException(String.format("Property %s not specified", "ssl.server.keystore.location"));
            }
            this.keyStorePassword = getPasswordString(this.sslConf, "ssl.server.keystore.password");
            if (this.keyStorePassword == null) {
                throw new IOException(String.format("Property %s not specified", "ssl.server.keystore.password"));
            }
            this.keyStoreType = this.sslConf.get("ssl.server.keystore.type", "jks");
            this.keyPassword = getPasswordString(this.sslConf, "ssl.server.keystore.keypassword");
            this.trustStore = this.sslConf.get("ssl.server.truststore.location");
            this.trustStorePassword = getPasswordString(this.sslConf, "ssl.server.truststore.password");
            this.trustStoreType = this.sslConf.get("ssl.server.truststore.type", "jks");
            this.excludeCiphers = this.sslConf.get("ssl.server.exclude.cipher.list");
        }
        
        public HttpServer2 build() throws IOException {
            Preconditions.checkNotNull((Object)this.name, (Object)"name is not set");
            Preconditions.checkState(!this.endpoints.isEmpty(), (Object)"No endpoints specified");
            if (this.hostName == null) {
                this.hostName = this.endpoints.get(0).getHost();
            }
            if (this.conf == null) {
                this.conf = new Configuration();
            }
            final HttpServer2 server = new HttpServer2(this, null);
            if (this.securityEnabled) {
                server.initSpnego(this.conf, this.hostName, this.usernameConfKey, this.keytabConfKey);
            }
            for (final URI ep : this.endpoints) {
                if ("https".equals(ep.getScheme())) {
                    this.loadSSLConfiguration();
                    break;
                }
            }
            final int requestHeaderSize = this.conf.getInt("hadoop.http.max.request.header.size", 65536);
            final int responseHeaderSize = this.conf.getInt("hadoop.http.max.response.header.size", 65536);
            final int idleTimeout = this.conf.getInt("hadoop.http.idle_timeout.ms", 10000);
            final HttpConfiguration httpConfig = new HttpConfiguration();
            httpConfig.setRequestHeaderSize(requestHeaderSize);
            httpConfig.setResponseHeaderSize(responseHeaderSize);
            httpConfig.setSendServerVersion(false);
            final int backlogSize = this.conf.getInt("hadoop.http.socket.backlog.size", 128);
            for (final URI ep2 : this.endpoints) {
                final String scheme = ep2.getScheme();
                ServerConnector connector;
                if ("http".equals(scheme)) {
                    connector = this.createHttpChannelConnector(server.webServer, httpConfig);
                }
                else {
                    if (!"https".equals(scheme)) {
                        throw new HadoopIllegalArgumentException("unknown scheme for endpoint:" + ep2);
                    }
                    connector = this.createHttpsChannelConnector(server.webServer, httpConfig);
                }
                connector.setHost(ep2.getHost());
                connector.setPort((ep2.getPort() == -1) ? 0 : ep2.getPort());
                connector.setAcceptQueueSize(backlogSize);
                connector.setIdleTimeout((long)idleTimeout);
                server.addListener(connector);
            }
            server.loadListeners();
            return server;
        }
        
        private ServerConnector createHttpChannelConnector(final Server server, final HttpConfiguration httpConfig) {
            final ServerConnector conn = new ServerConnector(server, this.conf.getInt("hadoop.http.acceptor.count", -1), this.conf.getInt("hadoop.http.selector.count", -1));
            final ConnectionFactory connFactory = (ConnectionFactory)new HttpConnectionFactory(httpConfig);
            conn.addConnectionFactory(connFactory);
            if (Shell.WINDOWS) {
                conn.setReuseAddress(false);
            }
            return conn;
        }
        
        private ServerConnector createHttpsChannelConnector(final Server server, final HttpConfiguration httpConfig) {
            httpConfig.setSecureScheme("https");
            httpConfig.addCustomizer((HttpConfiguration.Customizer)new SecureRequestCustomizer());
            final ServerConnector conn = this.createHttpChannelConnector(server, httpConfig);
            final SslContextFactory sslContextFactory = new SslContextFactory();
            sslContextFactory.setNeedClientAuth(this.needsClientAuth);
            sslContextFactory.setKeyManagerPassword(this.keyPassword);
            if (this.keyStore != null) {
                sslContextFactory.setKeyStorePath(this.keyStore);
                sslContextFactory.setKeyStoreType(this.keyStoreType);
                sslContextFactory.setKeyStorePassword(this.keyStorePassword);
            }
            if (this.trustStore != null) {
                sslContextFactory.setTrustStorePath(this.trustStore);
                sslContextFactory.setTrustStoreType(this.trustStoreType);
                sslContextFactory.setTrustStorePassword(this.trustStorePassword);
            }
            if (null != this.excludeCiphers && !this.excludeCiphers.isEmpty()) {
                sslContextFactory.setExcludeCipherSuites(StringUtils.getTrimmedStrings(this.excludeCiphers));
                HttpServer2.LOG.info("Excluded Cipher List:" + this.excludeCiphers);
            }
            conn.addFirstConnectionFactory((ConnectionFactory)new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()));
            return conn;
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
            try (final PrintStream out = new PrintStream((OutputStream)response.getOutputStream(), false, "UTF-8")) {
                ReflectionUtils.printThreadInfo(out, "");
            }
            ReflectionUtils.logThreadInfo(HttpServer2.LOG, "jsp requested", 1L);
        }
    }
    
    public static class QuotingInputFilter implements Filter
    {
        private FilterConfig config;
        private Map<String, String> headerMap;
        
        public void init(final FilterConfig config) throws ServletException {
            this.config = config;
            this.initHttpHeaderMap();
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
            this.headerMap.forEach((k, v) -> httpResponse.addHeader(k, v));
            chain.doFilter((ServletRequest)quoted, (ServletResponse)httpResponse);
        }
        
        private String inferMimeType(final ServletRequest request) {
            final String path = ((HttpServletRequest)request).getRequestURI();
            final ServletContextHandler.Context sContext = (ServletContextHandler.Context)this.config.getServletContext();
            final String mime = sContext.getMimeType(path);
            return (mime == null) ? null : mime;
        }
        
        private void initHttpHeaderMap() {
            final Enumeration<String> params = (Enumeration<String>)this.config.getInitParameterNames();
            this.headerMap = new HashMap<String, String>();
            while (params.hasMoreElements()) {
                final String key = params.nextElement();
                final Matcher m = HttpServer2.PATTERN_HTTP_HEADER_REGEX.matcher(key);
                if (m.matches()) {
                    final String headerKey = m.group(1);
                    this.headerMap.put(headerKey, this.config.getInitParameter(key));
                }
            }
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
    
    public enum XFrameOption
    {
        DENY("DENY"), 
        SAMEORIGIN("SAMEORIGIN"), 
        ALLOWFROM("ALLOW-FROM");
        
        private final String name;
        
        private XFrameOption(final String name) {
            this.name = name;
        }
        
        @Override
        public String toString() {
            return this.name;
        }
        
        private static XFrameOption getEnum(final String value) {
            Preconditions.checkState(value != null && !value.isEmpty());
            for (final XFrameOption xoption : values()) {
                if (value.equals(xoption.toString())) {
                    return xoption;
                }
            }
            throw new IllegalArgumentException("Unexpected value in xFrameOption.");
        }
    }
}
