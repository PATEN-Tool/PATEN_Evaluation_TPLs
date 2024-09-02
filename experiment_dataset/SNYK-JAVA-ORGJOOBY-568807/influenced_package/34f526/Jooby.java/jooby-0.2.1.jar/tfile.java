// 
// Decompiled by Procyon v0.5.36
// 

package org.jooby;

import com.google.inject.name.Named;
import com.google.inject.Key;
import java.lang.reflect.Type;
import com.google.inject.util.Types;
import com.typesafe.config.ConfigValue;
import java.util.Map;
import com.google.common.collect.ImmutableMap;
import java.util.LinkedHashMap;
import com.google.common.collect.UnmodifiableIterator;
import java.util.Arrays;
import com.typesafe.config.ConfigMergeable;
import java.util.Collection;
import com.google.common.collect.ImmutableList;
import com.typesafe.config.ConfigValueFactory;
import org.slf4j.LoggerFactory;
import java.util.Iterator;
import org.jooby.internal.Server;
import com.google.inject.Guice;
import org.jooby.internal.FallbackBodyConverter;
import com.google.inject.Scopes;
import org.jooby.internal.mvc.Routes;
import java.lang.annotation.Annotation;
import com.google.inject.name.Names;
import java.io.File;
import com.google.inject.multibindings.Multibinder;
import java.text.NumberFormat;
import java.util.TimeZone;
import org.jooby.internal.TypeConverters;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Stage;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.Locale;
import java.nio.charset.Charset;
import com.typesafe.config.ConfigFactory;
import java.util.Optional;
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
import org.jooby.internal.jetty.Jetty;
import java.util.LinkedList;
import java.util.LinkedHashSet;
import java.util.List;
import com.google.inject.Injector;
import com.typesafe.config.Config;
import java.util.Set;

public class Jooby
{
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
        this.bag = new LinkedHashSet<Object>();
        this.modules = new LinkedHashSet<Module>();
        this.singletonRoutes = new LinkedHashSet<Class<?>>();
        this.protoRoutes = new LinkedHashSet<Class<?>>();
        this.formatters = new LinkedList<Body.Formatter>();
        this.parsers = new LinkedList<Body.Parser>();
        this.session = new Session.Definition(Session.Store.NOOP);
        this.assetFormatter = false;
        this.env = Env.DEFAULT;
        this.use(new Jetty());
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
        return (req, rsp) -> req.getInstance(handler).handle(req, rsp);
    }
    
    @Nonnull
    private Route.Filter filter(@Nonnull final Class<? extends Route.Filter> filter) {
        Objects.requireNonNull(filter, "Filter is required.");
        this.registerRouteScope(filter);
        return (req, rsp, chain) -> req.getInstance(filter).handle(req, rsp, chain);
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
        final Config config = this.buildConfig(Optional.ofNullable(this.source).orElseGet(() -> ConfigFactory.parseResources("application.conf")));
        final Env env = this.env.build(config);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> this.stop()));
        final Charset charset = Charset.forName(config.getString("application.charset"));
        final String[] lang = config.getString("application.lang").split("_");
        final Locale locale = (lang.length == 1) ? new Locale(lang[0]) : new Locale(lang[0], lang[1]);
        final ZoneId zoneId = ZoneId.of(config.getString("application.tz"));
        final DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern(config.getString("application.dateFormat"), locale).withZone(zoneId);
        final DecimalFormat numberFormat = new DecimalFormat(config.getString("application.numberFormat"));
        final Stage stage = env.when("dev", Stage.DEVELOPMENT).value().orElse(Stage.PRODUCTION);
        this.injector = Guice.createInjector(stage, new com.google.inject.Module[] { (com.google.inject.Module)new com.google.inject.Module() {
                public void configure(final Binder binder) {
                    TypeConverters.configure(binder);
                    Jooby.this.bindConfig(binder, config);
                    binder.bind((Class)Env.class).toInstance((Object)env);
                    binder.bind((Class)Charset.class).toInstance((Object)charset);
                    binder.bind((Class)Locale.class).toInstance((Object)locale);
                    binder.bind((Class)ZoneId.class).toInstance((Object)zoneId);
                    binder.bind((Class)TimeZone.class).toInstance((Object)TimeZone.getTimeZone(zoneId));
                    binder.bind((Class)DateTimeFormatter.class).toInstance((Object)dateTimeFormat);
                    binder.bind((Class)NumberFormat.class).toInstance((Object)numberFormat);
                    binder.bind((Class)DecimalFormat.class).toInstance((Object)numberFormat);
                    final Multibinder<Body.Parser> parserBinder = (Multibinder<Body.Parser>)Multibinder.newSetBinder(binder, (Class)Body.Parser.class);
                    final Multibinder<Body.Formatter> formatterBinder = (Multibinder<Body.Formatter>)Multibinder.newSetBinder(binder, (Class)Body.Formatter.class);
                    binder.bind((Class)Session.Definition.class).toInstance((Object)Jooby.this.session);
                    final Multibinder<Route.Definition> definitions = (Multibinder<Route.Definition>)Multibinder.newSetBinder(binder, (Class)Route.Definition.class);
                    final Multibinder<WebSocket.Definition> sockets = (Multibinder<WebSocket.Definition>)Multibinder.newSetBinder(binder, (Class)WebSocket.Definition.class);
                    final Multibinder<Request.Module> requestModule = (Multibinder<Request.Module>)Multibinder.newSetBinder(binder, (Class)Request.Module.class);
                    if (Jooby.this.protoRoutes.size() > 0) {
                        requestModule.addBinding().toInstance(b -> Jooby.this.protoRoutes.forEach(routeClass -> b.bind(routeClass)));
                    }
                    final File tmpdir = new File(config.getString("application.tmpdir"));
                    tmpdir.mkdirs();
                    binder.bind((Class)File.class).annotatedWith((Annotation)Names.named("application.tmpdir")).toInstance((Object)tmpdir);
                    Jooby.this.parsers.forEach(it -> parserBinder.addBinding().toInstance((Object)it));
                    Jooby.this.formatters.forEach(it -> formatterBinder.addBinding().toInstance((Object)it));
                    final Env val$env;
                    final Config val$config;
                    final Multibinder multibinder;
                    final Multibinder multibinder2;
                    final Multibinder multibinder3;
                    Class routeClass2;
                    Jooby.this.bag.forEach(candidate -> {
                        val$env = env;
                        val$config = config;
                        if (candidate instanceof Module) {
                            Jooby.this.install(candidate, val$env, val$config, binder);
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
                            routeClass2 = (Class)candidate;
                            Routes.routes(val$env, routeClass2).forEach(route -> multibinder2.addBinding().toInstance((Object)route));
                        }
                        return;
                    });
                    Jooby.this.singletonRoutes.forEach(routeClass -> binder.bind(routeClass).in(Scopes.SINGLETON));
                    formatterBinder.addBinding().toInstance((Object)FallbackBodyConverter.formatReader);
                    formatterBinder.addBinding().toInstance((Object)FallbackBodyConverter.formatStream);
                    formatterBinder.addBinding().toInstance((Object)FallbackBodyConverter.formatString);
                    parserBinder.addBinding().toInstance((Object)FallbackBodyConverter.parseString);
                    if (Jooby.this.err == null) {
                        binder.bind((Class)Err.Handler.class).toInstance((Object)new Err.Default());
                    }
                    else {
                        binder.bind((Class)Err.Handler.class).toInstance((Object)Jooby.this.err);
                    }
                }
            } });
        for (final Module module : this.modules) {
            module.start();
        }
        final Server server = (Server)this.injector.getInstance((Class)Server.class);
        server.start();
    }
    
    public void stop() {
        for (final Module module : this.modules) {
            try {
                module.stop();
            }
            catch (Exception ex) {
                LoggerFactory.getLogger((Class)this.getClass()).warn("Module didn't stop normally: " + module.getClass().getName(), (Throwable)ex);
            }
        }
        try {
            if (this.injector != null) {
                final Server server = (Server)this.injector.getInstance((Class)Server.class);
                server.stop();
            }
        }
        catch (Exception ex2) {
            LoggerFactory.getLogger((Class)this.getClass()).error("Web server didn't stop normally", (Throwable)ex2);
        }
    }
    
    private Config buildConfig(final Config source) {
        final Config system = ConfigFactory.systemProperties().withValue("file.encoding", ConfigValueFactory.fromAnyRef((Object)System.getProperty("file.encoding")));
        Config moduleStack = ConfigFactory.empty();
        for (final Module module : ImmutableList.copyOf((Collection)this.modules).reverse()) {
            moduleStack = moduleStack.withFallback((ConfigMergeable)module.config());
        }
        final Config jooby = ConfigFactory.parseResources((Class)Jooby.class, "jooby.conf");
        final String env = Arrays.asList(system, source, jooby).stream().filter(it -> it.hasPath("application.env")).findFirst().get().getString("application.env");
        final String secret = Arrays.asList(system, source, jooby).stream().filter(it -> it.hasPath("application.secret")).findFirst().orElseGet(() -> ConfigFactory.empty().withValue("application.secret", ConfigValueFactory.fromAnyRef((Object)""))).getString("application.secret");
        final Config modeConfig = this.modeConfig(source, env);
        final Config config = modeConfig.withFallback((ConfigMergeable)source);
        return system.withFallback((ConfigMergeable)config).withFallback((ConfigMergeable)moduleStack).withFallback((ConfigMergeable)MediaType.types).withFallback((ConfigMergeable)this.defaultConfig(config, env, secret)).withFallback((ConfigMergeable)jooby).resolve();
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
    
    private Config defaultConfig(final Config config, final String env, final String secret) {
        final Map<String, Object> defaults = new LinkedHashMap<String, Object>();
        defaults.put("name", this.getClass().getSimpleName());
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
        if (secret.length() == 0) {
            if (!"dev".equalsIgnoreCase(env)) {
                throw new IllegalStateException("No application.secret has been defined");
            }
            final String devRandomSecret = this.getClass().getResource(this.getClass().getSimpleName() + ".class").toString();
            defaults.put("secret", devRandomSecret);
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
    
    public interface Module
    {
        @Nonnull
        default Config config() {
            return ConfigFactory.empty();
        }
        
        default void start() throws Exception {
        }
        
        default void stop() throws Exception {
        }
        
        void configure(@Nonnull final Env env, @Nonnull final Config config, @Nonnull final Binder binder) throws Exception;
    }
}
