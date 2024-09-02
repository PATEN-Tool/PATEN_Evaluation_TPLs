// 
// Decompiled by Procyon v0.5.36
// 

package org.jooby;

import com.google.common.base.Strings;
import org.jooby.internal.SessionManager;
import org.jooby.internal.BuiltinBodyConverter;
import com.google.inject.Scopes;
import org.jooby.internal.mvc.Routes;
import org.jooby.internal.RouteMetadata;
import com.google.inject.multibindings.Multibinder;
import java.util.TimeZone;
import org.jooby.internal.TypeConverters;
import com.google.inject.name.Named;
import java.lang.annotation.Annotation;
import com.google.inject.Key;
import java.lang.reflect.Type;
import com.google.inject.util.Types;
import com.google.inject.name.Names;
import com.typesafe.config.ConfigValue;
import com.google.inject.Binder;
import java.util.Map;
import com.google.common.collect.ImmutableMap;
import java.text.NumberFormat;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.io.File;
import com.google.common.collect.UnmodifiableIterator;
import java.util.Arrays;
import com.typesafe.config.ConfigMergeable;
import com.google.common.collect.ImmutableList;
import com.typesafe.config.ConfigValueFactory;
import java.util.Iterator;
import java.util.Locale;
import com.google.inject.Guice;
import com.google.inject.Module;
import com.google.inject.Stage;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import org.jooby.internal.LocaleUtils;
import java.nio.charset.Charset;
import com.typesafe.config.ConfigFactory;
import java.util.Optional;
import java.util.Collection;
import org.jooby.internal.AppManager;
import org.jooby.internal.RouteHandler;
import org.jooby.internal.Server;
import com.google.common.base.Preconditions;
import javax.inject.Singleton;
import org.jooby.internal.RoutePattern;
import org.jooby.internal.AssetHandler;
import org.jooby.internal.AssetFormatter;
import org.jooby.internal.routes.TraceHandler;
import org.jooby.internal.routes.OptionsHandler;
import org.jooby.internal.routes.HeadHandler;
import javax.annotation.Nonnull;
import java.util.Objects;
import org.jooby.internal.undertow.Undertow;
import java.util.LinkedList;
import java.util.LinkedHashSet;
import org.slf4j.LoggerFactory;
import java.util.List;
import com.google.inject.Injector;
import com.typesafe.config.Config;
import java.util.Set;
import org.slf4j.Logger;

public class Jooby
{
    private final Logger log;
    private final Set<Object> bag;
    private final Set<Module> modules;
    private final Set<Class<?>> singletonRoutes;
    private final Set<Class<?>> protoRoutes;
    private Config source;
    private Injector injector;
    private Err.Handler err;
    private List<Body.Formatter> formatters;
    private List<Body.Parser> parsers;
    private Session.Definition session;
    private boolean assetFormatter;
    private Env.Builder env;
    
    public Jooby() {
        this.log = LoggerFactory.getLogger((Class)this.getClass());
        this.bag = new LinkedHashSet<Object>();
        this.modules = new LinkedHashSet<Module>();
        this.singletonRoutes = new LinkedHashSet<Class<?>>();
        this.protoRoutes = new LinkedHashSet<Class<?>>();
        this.formatters = new LinkedList<Body.Formatter>();
        this.parsers = new LinkedList<Body.Parser>();
        this.session = new Session.Definition(new Session.MemoryStore());
        this.assetFormatter = false;
        this.env = Env.DEFAULT;
        this.use(new Undertow());
    }
    
    @Nonnull
    public Jooby env(final Env.Builder env) {
        this.env = Objects.requireNonNull(env, "Env builder is required.");
        return this;
    }
    
    @Nonnull
    public Session.Definition use(@Nonnull final Session.Store sessionStore) {
        return this.session = new Session.Definition(Objects.requireNonNull(sessionStore, "A session store is required."));
    }
    
    @Nonnull
    public Jooby use(@Nonnull final Body.Formatter formatter) {
        this.formatters.add(Objects.requireNonNull(formatter, "A body formatter is required."));
        return this;
    }
    
    @Nonnull
    public Jooby use(@Nonnull final Body.Parser parser) {
        this.parsers.add(Objects.requireNonNull(parser, "A body parser is required."));
        return this;
    }
    
    @Nonnull
    public Route.Definition use(@Nonnull final String path, @Nonnull final Route.Filter filter) {
        return this.appendDefinition(new Route.Definition("*", path, filter));
    }
    
    @Nonnull
    public Route.Definition use(@Nonnull final String verb, @Nonnull final String path, @Nonnull final Route.Filter filter) {
        return this.appendDefinition(new Route.Definition(verb, path, filter));
    }
    
    @Nonnull
    public Route.Definition use(@Nonnull final String verb, @Nonnull final String path, @Nonnull final Route.Handler handler) {
        return this.appendDefinition(new Route.Definition(verb, path, handler));
    }
    
    @Nonnull
    public Route.Definition use(@Nonnull final String path, @Nonnull final Route.Handler handler) {
        return this.appendDefinition(new Route.Definition("*", path, handler));
    }
    
    @Nonnull
    public Route.Definition get(final String path) {
        return this.appendDefinition(new Route.Definition("GET", path, this.staticFile(path)));
    }
    
    @Nonnull
    public Route.Definition get(@Nonnull final String path, @Nonnull final Route.Handler handler) {
        return this.appendDefinition(new Route.Definition("GET", path, handler));
    }
    
    @Nonnull
    public Route.Definition get(@Nonnull final String path, @Nonnull final Route.OneArgHandler handler) {
        return this.appendDefinition(new Route.Definition("GET", path, handler));
    }
    
    @Nonnull
    public Route.Definition get(@Nonnull final String path, @Nonnull final Route.ZeroArgHandler handler) {
        return this.appendDefinition(new Route.Definition("GET", path, handler));
    }
    
    @Nonnull
    public Route.Definition get(@Nonnull final String path, @Nonnull final Route.Filter filter) {
        return this.appendDefinition(new Route.Definition("GET", path, filter));
    }
    
    @Nonnull
    public Route.Definition post(@Nonnull final String path, @Nonnull final Route.Handler handler) {
        return this.appendDefinition(new Route.Definition("POST", path, handler));
    }
    
    @Nonnull
    public Route.Definition post(@Nonnull final String path, @Nonnull final Route.OneArgHandler handler) {
        return this.appendDefinition(new Route.Definition("POST", path, handler));
    }
    
    @Nonnull
    public Route.Definition post(@Nonnull final String path, @Nonnull final Route.ZeroArgHandler handler) {
        return this.appendDefinition(new Route.Definition("POST", path, handler));
    }
    
    @Nonnull
    public Route.Definition post(@Nonnull final String path, @Nonnull final Route.Filter filter) {
        return this.appendDefinition(new Route.Definition("POST", path, filter));
    }
    
    public Route.Definition head(@Nonnull final String path, @Nonnull final Route.Handler handler) {
        return this.appendDefinition(new Route.Definition("HEAD", path, handler));
    }
    
    @Nonnull
    public Route.Definition head(@Nonnull final String path, @Nonnull final Route.OneArgHandler handler) {
        return this.appendDefinition(new Route.Definition("HEAD", path, handler));
    }
    
    @Nonnull
    public Route.Definition head(@Nonnull final String path, @Nonnull final Route.ZeroArgHandler handler) {
        return this.appendDefinition(new Route.Definition("HEAD", path, handler));
    }
    
    @Nonnull
    public Route.Definition head(@Nonnull final String path, @Nonnull final Route.Filter filter) {
        return this.appendDefinition(new Route.Definition("HEAD", path, filter));
    }
    
    @Nonnull
    public Route.Definition head() {
        return this.appendDefinition(new Route.Definition("HEAD", "*", this.filter(HeadHandler.class)).name("*.head"));
    }
    
    @Nonnull
    public Route.Definition options(@Nonnull final String path, @Nonnull final Route.Handler handler) {
        return this.appendDefinition(new Route.Definition("OPTIONS", path, handler));
    }
    
    @Nonnull
    public Route.Definition options(@Nonnull final String path, @Nonnull final Route.OneArgHandler handler) {
        return this.appendDefinition(new Route.Definition("OPTIONS", path, handler));
    }
    
    @Nonnull
    public Route.Definition options(@Nonnull final String path, @Nonnull final Route.ZeroArgHandler handler) {
        return this.appendDefinition(new Route.Definition("OPTIONS", path, handler));
    }
    
    @Nonnull
    public Route.Definition options(@Nonnull final String path, @Nonnull final Route.Filter filter) {
        return this.appendDefinition(new Route.Definition("OPTIONS", path, filter));
    }
    
    @Nonnull
    public Route.Definition options() {
        return this.appendDefinition(new Route.Definition("OPTIONS", "*", this.handler(OptionsHandler.class)).name("*.options"));
    }
    
    @Nonnull
    public Route.Definition put(@Nonnull final String path, @Nonnull final Route.Handler handler) {
        return this.appendDefinition(new Route.Definition("PUT", path, handler));
    }
    
    @Nonnull
    public Route.Definition put(@Nonnull final String path, @Nonnull final Route.OneArgHandler handler) {
        return this.appendDefinition(new Route.Definition("PUT", path, handler));
    }
    
    @Nonnull
    public Route.Definition put(@Nonnull final String path, @Nonnull final Route.ZeroArgHandler handler) {
        return this.appendDefinition(new Route.Definition("PUT", path, handler));
    }
    
    @Nonnull
    public Route.Definition put(@Nonnull final String path, @Nonnull final Route.Filter filter) {
        return this.appendDefinition(new Route.Definition("PUT", path, filter));
    }
    
    @Nonnull
    public Route.Definition patch(@Nonnull final String path, @Nonnull final Route.Handler handler) {
        return this.appendDefinition(new Route.Definition("PATCH", path, handler));
    }
    
    @Nonnull
    public Route.Definition patch(@Nonnull final String path, @Nonnull final Route.OneArgHandler handler) {
        return this.appendDefinition(new Route.Definition("PATCH", path, handler));
    }
    
    @Nonnull
    public Route.Definition patch(@Nonnull final String path, @Nonnull final Route.ZeroArgHandler handler) {
        return this.appendDefinition(new Route.Definition("PATCH", path, handler));
    }
    
    @Nonnull
    public Route.Definition patch(@Nonnull final String path, @Nonnull final Route.Filter filter) {
        return this.appendDefinition(new Route.Definition("PATCH", path, filter));
    }
    
    @Nonnull
    public Route.Definition delete(@Nonnull final String path, @Nonnull final Route.Handler handler) {
        return this.appendDefinition(new Route.Definition("DELETE", path, handler));
    }
    
    @Nonnull
    public Route.Definition delete(@Nonnull final String path, @Nonnull final Route.OneArgHandler handler) {
        return this.appendDefinition(new Route.Definition("DELETE", path, handler));
    }
    
    @Nonnull
    public Route.Definition delete(@Nonnull final String path, @Nonnull final Route.ZeroArgHandler handler) {
        return this.appendDefinition(new Route.Definition("DELETE", path, handler));
    }
    
    @Nonnull
    public Route.Definition delete(@Nonnull final String path, @Nonnull final Route.Filter filter) {
        return this.appendDefinition(new Route.Definition("DELETE", path, filter));
    }
    
    @Nonnull
    public Route.Definition trace(@Nonnull final String path, @Nonnull final Route.Handler handler) {
        return this.appendDefinition(new Route.Definition("TRACE", path, handler));
    }
    
    @Nonnull
    public Route.Definition trace(@Nonnull final String path, @Nonnull final Route.OneArgHandler handler) {
        return this.appendDefinition(new Route.Definition("TRACE", path, handler));
    }
    
    @Nonnull
    public Route.Definition trace(@Nonnull final String path, @Nonnull final Route.ZeroArgHandler handler) {
        return this.appendDefinition(new Route.Definition("TRACE", path, handler));
    }
    
    @Nonnull
    public Route.Definition trace(@Nonnull final String path, @Nonnull final Route.Filter filter) {
        return this.appendDefinition(new Route.Definition("TRACE", path, filter));
    }
    
    @Nonnull
    public Route.Definition trace() {
        return this.appendDefinition(new Route.Definition("TRACE", "*", this.handler(TraceHandler.class)).name("*.trace"));
    }
    
    @Nonnull
    public Route.Definition connect(@Nonnull final String path, @Nonnull final Route.Handler handler) {
        return this.appendDefinition(new Route.Definition("CONNECT", path, handler));
    }
    
    @Nonnull
    public Route.Definition connect(@Nonnull final String path, @Nonnull final Route.OneArgHandler handler) {
        return this.appendDefinition(new Route.Definition("CONNECT", path, handler));
    }
    
    @Nonnull
    public Route.Definition connect(@Nonnull final String path, @Nonnull final Route.ZeroArgHandler handler) {
        return this.appendDefinition(new Route.Definition("CONNECT", path, handler));
    }
    
    @Nonnull
    public Route.Definition connect(@Nonnull final String path, @Nonnull final Route.Filter filter) {
        return this.appendDefinition(new Route.Definition("CONNECT", path, filter));
    }
    
    @Nonnull
    private Route.Handler handler(@Nonnull final Class<? extends Route.Handler> handler) {
        Objects.requireNonNull(handler, "Route handler is required.");
        this.registerRouteScope(handler);
        return (req, rsp) -> req.require(handler).handle(req, rsp);
    }
    
    @Nonnull
    private Route.Filter filter(@Nonnull final Class<? extends Route.Filter> filter) {
        Objects.requireNonNull(filter, "Filter is required.");
        this.registerRouteScope(filter);
        return (req, rsp, chain) -> req.require(filter).handle(req, rsp, chain);
    }
    
    @Nonnull
    public Route.Definition assets(@Nonnull final String path) {
        if (!this.assetFormatter) {
            this.formatters.add(new AssetFormatter());
            this.assetFormatter = true;
        }
        return this.get(path, new AssetHandler());
    }
    
    @Nonnull
    public Jooby use(@Nonnull final Class<?> routeClass) {
        Objects.requireNonNull(routeClass, "Route class is required.");
        this.registerRouteScope(routeClass);
        this.bag.add(routeClass);
        return this;
    }
    
    public Route.Handler redirect(final String location) {
        return this.redirect(Status.FOUND, location);
    }
    
    public Route.Filter staticFile(@Nonnull final String location) {
        Objects.requireNonNull(location, "A location is required.");
        final String path = RoutePattern.normalize(location);
        if (!this.assetFormatter) {
            this.formatters.add(new AssetFormatter());
            this.assetFormatter = true;
        }
        return filehandler(path);
    }
    
    private static Route.Filter filehandler(final String path) {
        return (req, rsp, chain) -> new AssetHandler().handle(new Request.Forwarding(req) {
            final /* synthetic */ String val$cap$0;
            
            @Override
            public String path() {
                return this.route().path();
            }
            
            @Override
            public Route route() {
                return new Route.Forwarding(super.route()) {
                    @Override
                    public String path() {
                        return Request.Forwarding.this.val$cap$0;
                    }
                };
            }
        }, rsp, chain);
    }
    
    public Route.Handler redirect(final Status status, final String location) {
        Objects.requireNonNull(location, "A location is required.");
        return (req, rsp) -> rsp.redirect(status, location);
    }
    
    private void registerRouteScope(final Class<?> route) {
        if (route.getAnnotation(Singleton.class) != null || route.getAnnotation(com.google.inject.Singleton.class) != null) {
            this.singletonRoutes.add(route);
        }
        else {
            this.protoRoutes.add(route);
        }
    }
    
    private Route.Definition appendDefinition(final Route.Definition route) {
        this.bag.add(route);
        return route;
    }
    
    @Nonnull
    public Jooby use(@Nonnull final Module module) {
        Objects.requireNonNull(module, "A module is required.");
        this.modules.add(module);
        this.bag.add(module);
        return this;
    }
    
    @Nonnull
    public Jooby use(@Nonnull final Request.Module module) {
        Objects.requireNonNull(module, "A module is required.");
        this.bag.add(module);
        return this;
    }
    
    @Nonnull
    public Jooby use(@Nonnull final Config config) {
        this.source = Objects.requireNonNull(config, "A config is required.");
        return this;
    }
    
    @Nonnull
    public Jooby err(@Nonnull final Err.Handler err) {
        this.err = Objects.requireNonNull(err, "An err handler is required.");
        return this;
    }
    
    @Nonnull
    public WebSocket.Definition ws(@Nonnull final String path, @Nonnull final WebSocket.Handler handler) {
        final WebSocket.Definition ws = new WebSocket.Definition(path, handler);
        Preconditions.checkArgument(this.bag.add(ws), "Path is in use: '%s'", new Object[] { path });
        return ws;
    }
    
    public void start() throws Exception {
        this.start(new String[0]);
    }
    
    public void start(final String[] args) throws Exception {
        final long start = System.currentTimeMillis();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> this.stop()));
        this.injector = this.bootstrap();
        final Server server = (Server)this.injector.getInstance((Class)Server.class);
        server.start();
        final long end = System.currentTimeMillis();
        final Config config = (Config)this.injector.getInstance((Class)Config.class);
        this.log.info("Server started in {}ms\n{}\nlistening on:\n  http://localhost:{}\n", new Object[] { end - start, this.injector.getInstance((Class)RouteHandler.class), config.getInt("application.port") });
    }
    
    private static AppManager appManager(final Jooby app, final Env env, final Logger log) {
        long start;
        Jooby newApp;
        Injector injector;
        return action -> {
            if (action == -1) {
                app.stop();
                return app.injector;
            }
            else {
                try {
                    start = System.currentTimeMillis();
                    newApp = (Jooby)app.getClass().newInstance();
                    app.stopModules();
                    injector = newApp.bootstrap();
                    app.modules.addAll(newApp.modules);
                    log.info("reloading of {} took {}ms", (Object)app.getClass().getName(), (Object)(System.currentTimeMillis() - start));
                    return injector;
                }
                catch (ReflectiveOperationException ex) {
                    log.debug("Can't create app", (Throwable)ex);
                    return app.injector;
                }
            }
        };
    }
    
    private Injector bootstrap() {
        final Config config = this.buildConfig(Optional.ofNullable(this.source).orElseGet(() -> ConfigFactory.parseResources("application.conf")));
        final Env env = this.env.build(config);
        final Charset charset = Charset.forName(config.getString("application.charset"));
        final Locale locale = LocaleUtils.toLocale(config.getString("application.lang"), "_");
        final ZoneId zoneId = ZoneId.of(config.getString("application.tz"));
        final DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern(config.getString("application.dateFormat"), locale).withZone(zoneId);
        final DecimalFormat numberFormat = new DecimalFormat(config.getString("application.numberFormat"));
        final Stage stage = "dev".equals(env.name()) ? Stage.DEVELOPMENT : Stage.PRODUCTION;
        final Injector injector = Guice.createInjector(stage, new com.google.inject.Module[] { binder -> {
                TypeConverters.configure(binder);
                if ("dev".equals(env.name())) {
                    binder.bind(Key.get((Class)String.class, (Annotation)Names.named("internal.appClass"))).toInstance((Object)this.getClass().getName());
                    binder.bind((Class)AppManager.class).toInstance((Object)appManager(this, env, this.log));
                }
                this.bindConfig(binder, config);
                binder.bind((Class)Env.class).toInstance((Object)env);
                binder.bind((Class)Charset.class).toInstance((Object)charset);
                binder.bind((Class)Locale.class).toInstance((Object)locale);
                binder.bind((Class)ZoneId.class).toInstance((Object)zoneId);
                binder.bind((Class)TimeZone.class).toInstance((Object)TimeZone.getTimeZone(zoneId));
                binder.bind((Class)DateTimeFormatter.class).toInstance((Object)dateTimeFormat);
                binder.bind((Class)NumberFormat.class).toInstance((Object)numberFormat);
                binder.bind((Class)DecimalFormat.class).toInstance((Object)numberFormat);
                final Multibinder parserBinder = Multibinder.newSetBinder(binder, (Class)Body.Parser.class);
                final Multibinder formatterBinder = Multibinder.newSetBinder(binder, (Class)Body.Formatter.class);
                binder.bind((Class)Session.Definition.class).toInstance((Object)this.session);
                final Multibinder definitions = Multibinder.newSetBinder(binder, (Class)Route.Definition.class);
                final Multibinder sockets = Multibinder.newSetBinder(binder, (Class)WebSocket.Definition.class);
                final Multibinder requestModule = Multibinder.newSetBinder(binder, (Class)Request.Module.class);
                if (this.protoRoutes.size() > 0) {
                    requestModule.addBinding().toInstance(b -> this.protoRoutes.forEach(routeClass1 -> b.bind(routeClass1)));
                }
                final File tmpdir = new File(config.getString("application.tmpdir"));
                tmpdir.mkdirs();
                binder.bind((Class)File.class).annotatedWith((Annotation)Names.named("application.tmpdir")).toInstance((Object)tmpdir);
                this.parsers.forEach(it -> parserBinder.addBinding().toInstance((Object)it));
                this.formatters.forEach(it -> formatterBinder.addBinding().toInstance((Object)it));
                final RouteMetadata classInfo = new RouteMetadata(env);
                binder.bind((Class)RouteMetadata.class).toInstance((Object)classInfo);
                final Multibinder multibinder;
                final Multibinder multibinder2;
                final Multibinder multibinder3;
                final RouteMetadata classInfo2;
                this.bag.forEach(candidate -> {
                    if (candidate instanceof Module) {
                        this.install((Module)candidate, env, config, binder);
                    }
                    else if (candidate instanceof Request.Module) {
                        multibinder.addBinding().toInstance((Object)candidate);
                    }
                    else if (candidate instanceof Route.Definition) {
                        multibinder2.addBinding().toInstance((Object)candidate);
                    }
                    else if (candidate instanceof WebSocket.Definition) {
                        multibinder3.addBinding().toInstance((Object)candidate);
                    }
                    else {
                        Routes.routes(env, classInfo2, candidate).forEach(route -> multibinder2.addBinding().toInstance((Object)route));
                    }
                    return;
                });
                this.singletonRoutes.forEach(routeClass3 -> binder.bind(routeClass3).in(Scopes.SINGLETON));
                formatterBinder.addBinding().toInstance((Object)BuiltinBodyConverter.formatReader);
                formatterBinder.addBinding().toInstance((Object)BuiltinBodyConverter.formatStream);
                formatterBinder.addBinding().toInstance((Object)BuiltinBodyConverter.formatByteArray);
                formatterBinder.addBinding().toInstance((Object)BuiltinBodyConverter.formatByteBuffer);
                formatterBinder.addBinding().toInstance((Object)BuiltinBodyConverter.formatAny);
                parserBinder.addBinding().toInstance((Object)BuiltinBodyConverter.parseString);
                binder.bind((Class)SessionManager.class).toInstance((Object)new SessionManager(config, this.session));
                if (this.err == null) {
                    binder.bind((Class)Err.Handler.class).toInstance((Object)new Err.Default());
                }
                else {
                    binder.bind((Class)Err.Handler.class).toInstance((Object)this.err);
                }
            } });
        for (final Module module : this.modules) {
            module.start();
        }
        return injector;
    }
    
    public void stop() {
        this.stopModules();
        if (this.injector != null) {
            try {
                final Server server = (Server)this.injector.getInstance((Class)Server.class);
                server.stop();
            }
            catch (Exception ex) {
                this.log.error("Web server didn't stop normally", (Throwable)ex);
            }
            this.log.info("Server stopped");
            this.injector = null;
        }
    }
    
    private void stopModules() {
        for (final Module module : this.modules) {
            try {
                module.stop();
            }
            catch (Exception ex) {
                this.log.warn("Module didn't stop normally: " + module.getClass().getName(), (Throwable)ex);
            }
        }
        this.modules.clear();
    }
    
    private Config buildConfig(final Config source) {
        final Config system = ConfigFactory.systemProperties().withValue("file.encoding", ConfigValueFactory.fromAnyRef((Object)System.getProperty("file.encoding")));
        Config moduleStack = ConfigFactory.empty();
        for (final Module module : ImmutableList.copyOf((Collection)this.modules).reverse()) {
            moduleStack = moduleStack.withFallback((ConfigMergeable)module.config());
        }
        final Config jooby = ConfigFactory.parseResources((Class)Jooby.class, "jooby.conf");
        final String env = Arrays.asList(system, source, jooby).stream().filter(it -> it.hasPath("application.env")).findFirst().get().getString("application.env");
        final Config modeConfig = this.modeConfig(source, env);
        final Config config = modeConfig.withFallback((ConfigMergeable)source);
        return system.withFallback((ConfigMergeable)config).withFallback((ConfigMergeable)moduleStack).withFallback((ConfigMergeable)MediaType.types).withFallback((ConfigMergeable)this.defaultConfig(config, env)).withFallback((ConfigMergeable)jooby).resolve();
    }
    
    private Config modeConfig(final Config source, final String env) {
        final String origin = source.origin().resource();
        Config result = ConfigFactory.empty();
        if (origin != null) {
            final int dot = origin.lastIndexOf(46);
            final String originConf = origin.substring(0, dot) + "." + env + origin.substring(dot);
            result = this.fileConfig(originConf).withFallback((ConfigMergeable)ConfigFactory.parseResources(originConf));
        }
        final String appConfig = "application." + env + ".conf";
        return result.withFallback((ConfigMergeable)this.fileConfig(appConfig)).withFallback((ConfigMergeable)this.fileConfig("application.conf")).withFallback((ConfigMergeable)ConfigFactory.parseResources(appConfig));
    }
    
    private Config fileConfig(final String fname) {
        final File in = new File(fname);
        return in.exists() ? ConfigFactory.parseFile(in) : ConfigFactory.empty();
    }
    
    private Config defaultConfig(final Config config, final String env) {
        final Map<String, Object> defaults = new LinkedHashMap<String, Object>();
        defaults.put("name", this.getClass().getSimpleName());
        defaults.put("builddir", Paths.get(System.getProperty("user.dir"), "target", "classes").toString());
        final String deftmpdir = "java.io.tmpdir";
        String tmpdir = config.hasPath(deftmpdir) ? config.getString(deftmpdir) : System.getProperty(deftmpdir);
        if (tmpdir.endsWith(File.separator)) {
            tmpdir = tmpdir.substring(0, tmpdir.length() - File.separator.length());
        }
        defaults.put("tmpdir", tmpdir + File.separator + defaults.get("name"));
        defaults.put("ns", this.getClass().getPackage().getName());
        Locale locale;
        if (!config.hasPath("application.lang")) {
            locale = Locale.getDefault();
            defaults.put("lang", locale.getLanguage() + "_" + locale.getCountry());
        }
        else {
            locale = Locale.forLanguageTag(config.getString("application.lang").replace("_", "-"));
        }
        if (!config.hasPath("application.tz")) {
            defaults.put("tz", ZoneId.systemDefault().getId());
        }
        if (!config.hasPath("application.numberFormat")) {
            final String pattern = ((DecimalFormat)NumberFormat.getInstance(locale)).toPattern();
            defaults.put("numberFormat", pattern);
        }
        final Map<String, Object> application = (Map<String, Object>)ImmutableMap.of((Object)"application", (Object)defaults);
        return ConfigValueFactory.fromMap((Map)application).toConfig();
    }
    
    private void install(final Module module, final Env env, final Config config, final Binder binder) {
        try {
            module.configure(env, config, binder);
        }
        catch (Exception ex) {
            throw new IllegalStateException("Error found on module: " + module.getClass().getName(), ex);
        }
    }
    
    private void bindConfig(final Binder binder, final Config config) {
        for (final Map.Entry<String, ConfigValue> entry : config.entrySet()) {
            final String name = entry.getKey();
            final Named named = Names.named(name);
            final Object value = entry.getValue().unwrapped();
            if (value instanceof List) {
                final List<Object> values = (List<Object>)value;
                final Type listType = Types.listOf((Type)values.iterator().next().getClass());
                final Key<Object> key = (Key<Object>)Key.get(listType, (Annotation)Names.named(name));
                binder.bind((Key)key).toInstance((Object)values);
            }
            else {
                final Class type = value.getClass();
                binder.bind(type).annotatedWith((Annotation)named).toInstance(value);
            }
        }
        binder.bind((Class)Config.class).toInstance((Object)config);
    }
    
    static {
        final String logback = System.getProperty("logback.configurationFile");
        if (Strings.isNullOrEmpty(logback)) {
            System.setProperty("logback.configurationFile", "logback.xml");
        }
    }
    
    public interface Module
    {
        @Nonnull
        default Config config() {
            return ConfigFactory.empty();
        }
        
        default void start() {
        }
        
        default void stop() {
        }
        
        void configure(@Nonnull final Env env, @Nonnull final Config config, @Nonnull final Binder binder);
    }
}
