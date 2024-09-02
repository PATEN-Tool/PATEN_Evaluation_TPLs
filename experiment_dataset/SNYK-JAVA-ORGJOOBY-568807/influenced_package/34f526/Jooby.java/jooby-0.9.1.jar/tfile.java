// 
// Decompiled by Procyon v0.5.36
// 

package org.jooby;

import org.jooby.internal.JvmInfo;
import javax.inject.Provider;
import org.jooby.util.Providers;
import org.jooby.internal.SessionManager;
import com.google.inject.Scope;
import org.jooby.scope.RequestScoped;
import org.jooby.internal.RequestScope;
import org.jooby.internal.HttpHandlerImpl;
import org.jooby.spi.HttpHandler;
import org.jooby.internal.DefaulErrRenderer;
import org.jooby.internal.BuiltinRenderer;
import javax.inject.Singleton;
import org.jooby.internal.reqparam.ParserExecutor;
import org.jooby.internal.reqparam.StringConstructorParser;
import org.jooby.internal.reqparam.StaticMethodParser;
import org.jooby.internal.reqparam.BeanParser;
import org.jooby.internal.reqparam.LocaleParser;
import org.jooby.internal.reqparam.LocalDateParser;
import org.jooby.internal.reqparam.DateParser;
import org.jooby.internal.BuiltinParser;
import org.jooby.internal.mvc.MvcRoutes;
import org.jooby.reflect.ParameterNameProvider;
import org.jooby.internal.RouteMetadata;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.spi.TypeListener;
import com.google.inject.matcher.Matchers;
import java.util.TimeZone;
import org.jooby.internal.TypeConverters;
import com.typesafe.config.ConfigObject;
import com.google.inject.name.Named;
import java.lang.annotation.Annotation;
import com.google.inject.Key;
import java.lang.reflect.Type;
import com.google.inject.util.Types;
import java.util.List;
import com.google.inject.name.Names;
import com.typesafe.config.ConfigValue;
import com.google.inject.Binder;
import java.text.NumberFormat;
import com.google.common.collect.UnmodifiableIterator;
import java.util.Arrays;
import com.typesafe.config.ConfigMergeable;
import com.google.common.collect.ImmutableList;
import java.nio.file.Paths;
import com.typesafe.config.ConfigValueFactory;
import org.jooby.internal.LifecycleProcessor;
import java.util.Iterator;
import java.util.Locale;
import com.google.inject.Guice;
import com.google.inject.Module;
import java.util.Collection;
import java.util.Map;
import com.google.inject.Stage;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import org.jooby.internal.LocaleUtils;
import java.nio.charset.Charset;
import com.typesafe.config.ConfigFactory;
import java.io.File;
import org.jooby.internal.js.JsJooby;
import org.jooby.internal.AppPrinter;
import org.jooby.spi.Server;
import org.jooby.internal.routes.CorsHandler;
import java.util.Optional;
import org.jooby.internal.CdnAssetHandler;
import org.jooby.internal.AssetHandler;
import com.google.common.base.Strings;
import org.jooby.internal.FwdFilter;
import org.jooby.internal.routes.TraceHandler;
import org.jooby.internal.routes.OptionsHandler;
import org.jooby.internal.routes.HeadHandler;
import com.google.common.base.Preconditions;
import java.util.Objects;
import org.jooby.internal.ServerLookup;
import com.google.common.collect.ArrayListMultimap;
import java.util.LinkedHashSet;
import org.slf4j.LoggerFactory;
import com.google.inject.Injector;
import com.typesafe.config.Config;
import java.util.function.Consumer;
import java.util.function.Predicate;
import com.google.common.collect.Multimap;
import java.util.Set;
import org.slf4j.Logger;

public class Jooby
{
    private final Logger log;
    private final Set<Object> bag;
    private final Set<Module> modules;
    private final Multimap<Predicate<String>, Consumer<Config>> envcallbacks;
    private Config source;
    private Injector injector;
    private Session.Definition session;
    private Env.Builder env;
    
    public Jooby() {
        this.log = LoggerFactory.getLogger((Class)this.getClass());
        this.bag = new LinkedHashSet<Object>();
        this.modules = new LinkedHashSet<Module>();
        this.envcallbacks = (Multimap<Predicate<String>, Consumer<Config>>)ArrayListMultimap.create();
        this.session = new Session.Definition(Session.Mem.class);
        this.env = Env.DEFAULT;
        this.use(new ServerLookup());
    }
    
    public Jooby use(final Jooby app) {
        Objects.requireNonNull(app, "App is required.");
        app.bag.forEach(s -> {
            if (s instanceof Module) {
                this.use(s);
            }
            else {
                this.bag.add(s);
            }
            return;
        });
        this.envcallbacks.putAll((Multimap)app.envcallbacks);
        return this;
    }
    
    public Route.Namespace use(final String pattern) {
        final Route.Namespace ns = new Route.Namespace(pattern);
        this.bag.add(ns);
        return ns;
    }
    
    public Jooby env(final Env.Builder env) {
        this.env = Objects.requireNonNull(env, "Env builder is required.");
        return this;
    }
    
    public Jooby on(final String env, final Runnable callback) {
        Objects.requireNonNull(env, "Env is required.");
        return this.on(envpredicate(env), callback);
    }
    
    public Jooby on(final String env, final Consumer<Config> callback) {
        Objects.requireNonNull(env, "Env is required.");
        return this.on(envpredicate(env), callback);
    }
    
    public Jooby on(final Predicate<String> predicate, final Runnable callback) {
        Objects.requireNonNull(predicate, "Predicate is required.");
        Objects.requireNonNull(callback, "Callback is required.");
        return this.on(predicate, conf -> callback.run());
    }
    
    public Jooby on(final Predicate<String> predicate, final Consumer<Config> callback) {
        Objects.requireNonNull(predicate, "Predicate is required.");
        Objects.requireNonNull(callback, "Callback is required.");
        this.envcallbacks.put((Object)predicate, (Object)callback);
        return this;
    }
    
    public Jooby on(final String env1, final String env2, final String env3, final Runnable callback) {
        this.on(env1, callback);
        this.on(env2, callback);
        this.on(env3, callback);
        return this;
    }
    
    public <T> T require(final Class<T> type) {
        Preconditions.checkState(this.injector != null, (Object)"App didn't start yet");
        return (T)this.injector.getInstance((Class)type);
    }
    
    public Session.Definition session(final Class<? extends Session.Store> store) {
        return this.session = new Session.Definition(Objects.requireNonNull(store, "A session store is required."));
    }
    
    public Session.Definition session(final Session.Store store) {
        return this.session = new Session.Definition(Objects.requireNonNull(store, "A session store is required."));
    }
    
    public Jooby parser(final Parser parser) {
        this.bag.add(Objects.requireNonNull(parser, "A parser is required."));
        return this;
    }
    
    public Jooby renderer(final Renderer renderer) {
        this.bag.add(Objects.requireNonNull(renderer, "A renderer is required."));
        return this;
    }
    
    public Route.Definition use(final String path, final Route.Filter filter) {
        return this.appendDefinition(new Route.Definition("*", path, filter));
    }
    
    public Route.Definition use(final String verb, final String path, final Route.Filter filter) {
        return this.appendDefinition(new Route.Definition(verb, path, filter));
    }
    
    public Route.Definition use(final String verb, final String path, final Route.Handler handler) {
        return this.appendDefinition(new Route.Definition(verb, path, handler));
    }
    
    public Route.Definition use(final String path, final Route.Handler handler) {
        return this.appendDefinition(new Route.Definition("*", path, handler));
    }
    
    public Route.Definition get(final String path, final Route.Handler handler) {
        return this.appendDefinition(new Route.Definition("GET", path, handler));
    }
    
    public Route.Definitions get(final String path1, final String path2, final Route.Handler handler) {
        return new Route.Definitions(new Route.Definition[] { this.get(path1, handler), this.get(path2, handler) });
    }
    
    public Route.Definitions get(final String path1, final String path2, final String path3, final Route.Handler handler) {
        return new Route.Definitions(new Route.Definition[] { this.get(path1, handler), this.get(path2, handler), this.get(path3, handler) });
    }
    
    public Route.Definition get(final String path, final Route.OneArgHandler handler) {
        return this.appendDefinition(new Route.Definition("GET", path, handler));
    }
    
    public Route.Definitions get(final String path1, final String path2, final Route.OneArgHandler handler) {
        return new Route.Definitions(new Route.Definition[] { this.get(path1, handler), this.get(path2, handler) });
    }
    
    public Route.Definitions get(final String path1, final String path2, final String path3, final Route.OneArgHandler handler) {
        return new Route.Definitions(new Route.Definition[] { this.get(path1, handler), this.get(path2, handler), this.get(path3, handler) });
    }
    
    public Route.Definition get(final String path, final Route.ZeroArgHandler handler) {
        return this.appendDefinition(new Route.Definition("GET", path, handler));
    }
    
    public Route.Definitions get(final String path1, final String path2, final Route.ZeroArgHandler handler) {
        return new Route.Definitions(new Route.Definition[] { this.get(path1, handler), this.get(path2, handler) });
    }
    
    public Route.Definitions get(final String path1, final String path2, final String path3, final Route.ZeroArgHandler handler) {
        return new Route.Definitions(new Route.Definition[] { this.get(path1, handler), this.get(path2, handler), this.get(path3, handler) });
    }
    
    public Route.Definition get(final String path, final Route.Filter filter) {
        return this.appendDefinition(new Route.Definition("GET", path, filter));
    }
    
    public Route.Definitions get(final String path1, final String path2, final Route.Filter filter) {
        return new Route.Definitions(new Route.Definition[] { this.get(path1, filter), this.get(path2, filter) });
    }
    
    public Route.Definitions get(final String path1, final String path2, final String path3, final Route.Filter filter) {
        return new Route.Definitions(new Route.Definition[] { this.get(path1, filter), this.get(path2, filter), this.get(path3, filter) });
    }
    
    public Route.Definition post(final String path, final Route.Handler handler) {
        return this.appendDefinition(new Route.Definition("POST", path, handler));
    }
    
    public Route.Definitions post(final String path1, final String path2, final Route.Handler handler) {
        return new Route.Definitions(new Route.Definition[] { this.post(path1, handler), this.post(path2, handler) });
    }
    
    public Route.Definitions post(final String path1, final String path2, final String path3, final Route.Handler handler) {
        return new Route.Definitions(new Route.Definition[] { this.post(path1, handler), this.post(path2, handler), this.post(path3, handler) });
    }
    
    public Route.Definition post(final String path, final Route.OneArgHandler handler) {
        return this.appendDefinition(new Route.Definition("POST", path, handler));
    }
    
    public Route.Definitions post(final String path1, final String path2, final Route.OneArgHandler handler) {
        return new Route.Definitions(new Route.Definition[] { this.post(path1, handler), this.post(path2, handler) });
    }
    
    public Route.Definitions post(final String path1, final String path2, final String path3, final Route.OneArgHandler handler) {
        return new Route.Definitions(new Route.Definition[] { this.post(path1, handler), this.post(path2, handler), this.post(path3, handler) });
    }
    
    public Route.Definition post(final String path, final Route.ZeroArgHandler handler) {
        return this.appendDefinition(new Route.Definition("POST", path, handler));
    }
    
    public Route.Definitions post(final String path1, final String path2, final Route.ZeroArgHandler handler) {
        return new Route.Definitions(new Route.Definition[] { this.post(path1, handler), this.post(path2, handler) });
    }
    
    public Route.Definitions post(final String path1, final String path2, final String path3, final Route.ZeroArgHandler handler) {
        return new Route.Definitions(new Route.Definition[] { this.post(path1, handler), this.post(path2, handler), this.post(path3, handler) });
    }
    
    public Route.Definition post(final String path, final Route.Filter filter) {
        return this.appendDefinition(new Route.Definition("POST", path, filter));
    }
    
    public Route.Definitions post(final String path1, final String path2, final Route.Filter filter) {
        return new Route.Definitions(new Route.Definition[] { this.post(path1, filter), this.post(path2, filter) });
    }
    
    public Route.Definitions post(final String path1, final String path2, final String path3, final Route.Filter filter) {
        return new Route.Definitions(new Route.Definition[] { this.post(path1, filter), this.post(path2, filter), this.post(path3, filter) });
    }
    
    public Route.Definition head(final String path, final Route.Handler handler) {
        return this.appendDefinition(new Route.Definition("HEAD", path, handler));
    }
    
    public Route.Definition head(final String path, final Route.OneArgHandler handler) {
        return this.appendDefinition(new Route.Definition("HEAD", path, handler));
    }
    
    public Route.Definition head(final String path, final Route.ZeroArgHandler handler) {
        return this.appendDefinition(new Route.Definition("HEAD", path, handler));
    }
    
    public Route.Definition head(final String path, final Route.Filter filter) {
        return this.appendDefinition(new Route.Definition("HEAD", path, filter));
    }
    
    public Route.Definition head() {
        return this.appendDefinition(new Route.Definition("HEAD", "*", this.filter(HeadHandler.class)).name("*.head"));
    }
    
    public Route.Definition options(final String path, final Route.Handler handler) {
        return this.appendDefinition(new Route.Definition("OPTIONS", path, handler));
    }
    
    public Route.Definition options(final String path, final Route.OneArgHandler handler) {
        return this.appendDefinition(new Route.Definition("OPTIONS", path, handler));
    }
    
    public Route.Definition options(final String path, final Route.ZeroArgHandler handler) {
        return this.appendDefinition(new Route.Definition("OPTIONS", path, handler));
    }
    
    public Route.Definition options(final String path, final Route.Filter filter) {
        return this.appendDefinition(new Route.Definition("OPTIONS", path, filter));
    }
    
    public Route.Definition options() {
        return this.appendDefinition(new Route.Definition("OPTIONS", "*", this.handler(OptionsHandler.class)).name("*.options"));
    }
    
    public Route.Definition put(final String path, final Route.Handler handler) {
        return this.appendDefinition(new Route.Definition("PUT", path, handler));
    }
    
    public Route.Definitions put(final String path1, final String path2, final Route.Handler handler) {
        return new Route.Definitions(new Route.Definition[] { this.put(path1, handler), this.put(path2, handler) });
    }
    
    public Route.Definitions put(final String path1, final String path2, final String path3, final Route.Handler handler) {
        return new Route.Definitions(new Route.Definition[] { this.put(path1, handler), this.put(path2, handler), this.put(path3, handler) });
    }
    
    public Route.Definition put(final String path, final Route.OneArgHandler handler) {
        return this.appendDefinition(new Route.Definition("PUT", path, handler));
    }
    
    public Route.Definitions put(final String path1, final String path2, final Route.OneArgHandler handler) {
        return new Route.Definitions(new Route.Definition[] { this.put(path1, handler), this.put(path2, handler) });
    }
    
    public Route.Definitions put(final String path1, final String path2, final String path3, final Route.OneArgHandler handler) {
        return new Route.Definitions(new Route.Definition[] { this.put(path1, handler), this.put(path2, handler), this.put(path3, handler) });
    }
    
    public Route.Definition put(final String path, final Route.ZeroArgHandler handler) {
        return this.appendDefinition(new Route.Definition("PUT", path, handler));
    }
    
    public Route.Definitions put(final String path1, final String path2, final Route.ZeroArgHandler handler) {
        return new Route.Definitions(new Route.Definition[] { this.put(path1, handler), this.put(path2, handler) });
    }
    
    public Route.Definitions put(final String path1, final String path2, final String path3, final Route.ZeroArgHandler handler) {
        return new Route.Definitions(new Route.Definition[] { this.put(path1, handler), this.put(path2, handler), this.put(path3, handler) });
    }
    
    public Route.Definition put(final String path, final Route.Filter filter) {
        return this.appendDefinition(new Route.Definition("PUT", path, filter));
    }
    
    public Route.Definitions put(final String path1, final String path2, final Route.Filter filter) {
        return new Route.Definitions(new Route.Definition[] { this.put(path1, filter), this.put(path2, filter) });
    }
    
    public Route.Definitions put(final String path1, final String path2, final String path3, final Route.Filter filter) {
        return new Route.Definitions(new Route.Definition[] { this.put(path1, filter), this.put(path2, filter), this.put(path3, filter) });
    }
    
    public Route.Definition patch(final String path, final Route.Handler handler) {
        return this.appendDefinition(new Route.Definition("PATCH", path, handler));
    }
    
    public Route.Definitions patch(final String path1, final String path2, final Route.Handler handler) {
        return new Route.Definitions(new Route.Definition[] { this.patch(path1, handler), this.patch(path2, handler) });
    }
    
    public Route.Definitions patch(final String path1, final String path2, final String path3, final Route.Handler handler) {
        return new Route.Definitions(new Route.Definition[] { this.patch(path1, handler), this.patch(path2, handler), this.patch(path3, handler) });
    }
    
    public Route.Definition patch(final String path, final Route.OneArgHandler handler) {
        return this.appendDefinition(new Route.Definition("PATCH", path, handler));
    }
    
    public Route.Definitions patch(final String path1, final String path2, final Route.OneArgHandler handler) {
        return new Route.Definitions(new Route.Definition[] { this.patch(path1, handler), this.patch(path2, handler) });
    }
    
    public Route.Definitions patch(final String path1, final String path2, final String path3, final Route.OneArgHandler handler) {
        return new Route.Definitions(new Route.Definition[] { this.patch(path1, handler), this.patch(path2, handler), this.patch(path3, handler) });
    }
    
    public Route.Definition patch(final String path, final Route.ZeroArgHandler handler) {
        return this.appendDefinition(new Route.Definition("PATCH", path, handler));
    }
    
    public Route.Definitions patch(final String path1, final String path2, final Route.ZeroArgHandler handler) {
        return new Route.Definitions(new Route.Definition[] { this.patch(path1, handler), this.patch(path2, handler) });
    }
    
    public Route.Definitions patch(final String path1, final String path2, final String path3, final Route.ZeroArgHandler handler) {
        return new Route.Definitions(new Route.Definition[] { this.patch(path1, handler), this.patch(path2, handler), this.patch(path3, handler) });
    }
    
    public Route.Definition patch(final String path, final Route.Filter filter) {
        return this.appendDefinition(new Route.Definition("PATCH", path, filter));
    }
    
    public Route.Definitions patch(final String path1, final String path2, final Route.Filter filter) {
        return new Route.Definitions(new Route.Definition[] { this.patch(path1, filter), this.patch(path2, filter) });
    }
    
    public Route.Definitions patch(final String path1, final String path2, final String path3, final Route.Filter filter) {
        return new Route.Definitions(new Route.Definition[] { this.patch(path1, filter), this.patch(path2, filter), this.patch(path3, filter) });
    }
    
    public Route.Definition delete(final String path, final Route.Handler handler) {
        return this.appendDefinition(new Route.Definition("DELETE", path, handler));
    }
    
    public Route.Definitions delete(final String path1, final String path2, final Route.Handler handler) {
        return new Route.Definitions(new Route.Definition[] { this.delete(path1, handler), this.delete(path2, handler) });
    }
    
    public Route.Definitions delete(final String path1, final String path2, final String path3, final Route.Handler handler) {
        return new Route.Definitions(new Route.Definition[] { this.delete(path1, handler), this.delete(path2, handler), this.delete(path3, handler) });
    }
    
    public Route.Definition delete(final String path, final Route.OneArgHandler handler) {
        return this.appendDefinition(new Route.Definition("DELETE", path, handler));
    }
    
    public Route.Definitions delete(final String path1, final String path2, final Route.OneArgHandler handler) {
        return new Route.Definitions(new Route.Definition[] { this.delete(path1, handler), this.delete(path2, handler) });
    }
    
    public Route.Definitions delete(final String path1, final String path2, final String path3, final Route.OneArgHandler handler) {
        return new Route.Definitions(new Route.Definition[] { this.delete(path1, handler), this.delete(path2, handler), this.delete(path3, handler) });
    }
    
    public Route.Definition delete(final String path, final Route.ZeroArgHandler handler) {
        return this.appendDefinition(new Route.Definition("DELETE", path, handler));
    }
    
    public Route.Definitions delete(final String path1, final String path2, final Route.ZeroArgHandler handler) {
        return new Route.Definitions(new Route.Definition[] { this.delete(path1, handler), this.delete(path2, handler) });
    }
    
    public Route.Definitions delete(final String path1, final String path2, final String path3, final Route.ZeroArgHandler handler) {
        return new Route.Definitions(new Route.Definition[] { this.delete(path1, handler), this.delete(path2, handler), this.delete(path3, handler) });
    }
    
    public Route.Definition delete(final String path, final Route.Filter filter) {
        return this.appendDefinition(new Route.Definition("DELETE", path, filter));
    }
    
    public Route.Definitions delete(final String path1, final String path2, final Route.Filter filter) {
        return new Route.Definitions(new Route.Definition[] { this.delete(path1, filter), this.delete(path2, filter) });
    }
    
    public Route.Definitions delete(final String path1, final String path2, final String path3, final Route.Filter filter) {
        return new Route.Definitions(new Route.Definition[] { this.delete(path1, filter), this.delete(path2, filter), this.delete(path3, filter) });
    }
    
    public Route.Definition trace(final String path, final Route.Handler handler) {
        return this.appendDefinition(new Route.Definition("TRACE", path, handler));
    }
    
    public Route.Definition trace(final String path, final Route.OneArgHandler handler) {
        return this.appendDefinition(new Route.Definition("TRACE", path, handler));
    }
    
    public Route.Definition trace(final String path, final Route.ZeroArgHandler handler) {
        return this.appendDefinition(new Route.Definition("TRACE", path, handler));
    }
    
    public Route.Definition trace(final String path, final Route.Filter filter) {
        return this.appendDefinition(new Route.Definition("TRACE", path, filter));
    }
    
    public Route.Definition trace() {
        return this.appendDefinition(new Route.Definition("TRACE", "*", this.handler(TraceHandler.class)).name("*.trace"));
    }
    
    public Route.Definition connect(final String path, final Route.Handler handler) {
        return this.appendDefinition(new Route.Definition("CONNECT", path, handler));
    }
    
    public Route.Definition connect(final String path, final Route.OneArgHandler handler) {
        return this.appendDefinition(new Route.Definition("CONNECT", path, handler));
    }
    
    public Route.Definition connect(final String path, final Route.ZeroArgHandler handler) {
        return this.appendDefinition(new Route.Definition("CONNECT", path, handler));
    }
    
    public Route.Definition connect(final String path, final Route.Filter filter) {
        return this.appendDefinition(new Route.Definition("CONNECT", path, filter));
    }
    
    private Route.Handler handler(final Class<? extends Route.Handler> handler) {
        Objects.requireNonNull(handler, "Route handler is required.");
        return (req, rsp) -> req.require(handler).handle(req, rsp);
    }
    
    private Route.Filter filter(final Class<? extends Route.Filter> filter) {
        Objects.requireNonNull(filter, "Filter is required.");
        return (req, rsp, chain) -> req.require(filter).handle(req, rsp, chain);
    }
    
    public Route.Definition assets(final String path) {
        return this.assets(path, "/");
    }
    
    public Route.Definition assets(final String path, final String location) {
        final FwdFilter router = new FwdFilter();
        final Route.Definition asset = new Route.Definition("GET", path, router);
        final String cdn;
        final FwdFilter fwdFilter;
        final Route.Definition definition;
        this.on("*", conf -> {
            cdn = conf.getString("assets.cdn");
            if (Strings.isNullOrEmpty(cdn)) {
                fwdFilter.fwd(new AssetHandler(location, this.getClass()));
            }
            else {
                fwdFilter.fwd(new CdnAssetHandler(location, this.getClass(), cdn));
                definition.name(cdn);
            }
            return;
        });
        return this.appendDefinition(asset);
    }
    
    public Jooby use(final Class<?> routeClass) {
        Objects.requireNonNull(routeClass, "Route class is required.");
        this.bag.add(routeClass);
        return this;
    }
    
    public Route.Definition cors() {
        return this.cors(Optional.empty());
    }
    
    public Route.Definition cors(final Cors cors) {
        return this.cors(Optional.of(cors));
    }
    
    private Route.Definition cors(final Optional<Cors> cors) {
        return this.appendDefinition(new Route.Definition("*", "*", new CorsHandler(cors))).name("cors");
    }
    
    private Route.Definition appendDefinition(final Route.Definition route) {
        this.bag.add(route);
        return route;
    }
    
    public Jooby use(final Module module) {
        Objects.requireNonNull(module, "A module is required.");
        this.modules.add(module);
        this.bag.add(module);
        return this;
    }
    
    public Jooby use(final Config config) {
        this.source = Objects.requireNonNull(config, "A config is required.");
        return this;
    }
    
    public Jooby err(final Err.Handler err) {
        this.bag.add(Objects.requireNonNull(err, "An err handler is required."));
        return this;
    }
    
    public WebSocket.Definition ws(final String path, final WebSocket.Handler handler) {
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
        final Config config = (Config)this.injector.getInstance((Class)Config.class);
        this.log.debug("config tree:\n{}", (Object)this.configTree(config.origin().description()));
        final Server server = (Server)this.injector.getInstance((Class)Server.class);
        final String serverName = server.getClass().getSimpleName().replace("Server", "").toLowerCase();
        server.start();
        final long end = System.currentTimeMillis();
        this.log.info("[{}@{}]: Server started in {}ms\n\n{}\n", new Object[] { config.getString("application.env"), serverName, end - start, this.injector.getInstance((Class)AppPrinter.class) });
        final boolean join = !config.hasPath("server.join") || config.getBoolean("server.join");
        if (join) {
            server.join();
        }
    }
    
    public static void main(final String[] args) throws Exception {
        final String filename = (args.length > 0) ? args[0] : "app.js";
        new JsJooby().run(new File(filename)).start();
    }
    
    private String configTree(final String description) {
        return this.configTree(description.split(":\\s+\\d+,|,"), 0);
    }
    
    private String configTree(final String[] sources, final int i) {
        if (i < sources.length) {
            return Strings.padStart("", i, ' ') + "\u2514\u2500\u2500 " + sources[i] + "\n" + this.configTree(sources, i + 1);
        }
        return "";
    }
    
    private Injector bootstrap() throws Exception {
        final Config config = this.buildConfig(Optional.ofNullable(this.source).orElseGet(() -> ConfigFactory.parseResources("application.conf")));
        final Env env = this.env.build(config);
        final String envname = env.name();
        final Charset charset = Charset.forName(config.getString("application.charset"));
        final Locale locale = LocaleUtils.toLocale(config.getString("application.lang"));
        final String dateFormat = config.getString("application.dateFormat");
        final ZoneId zoneId = ZoneId.of(config.getString("application.tz"));
        final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateFormat, locale).withZone(zoneId);
        final DecimalFormat numberFormat = new DecimalFormat(config.getString("application.numberFormat"));
        final Stage stage = "dev".equals(envname) ? Stage.DEVELOPMENT : Stage.PRODUCTION;
        for (final Map.Entry<Predicate<String>, Collection<Consumer<Config>>> callback : this.envcallbacks.asMap().entrySet()) {
            if (callback.getKey().test(envname)) {
                callback.getValue().forEach(it -> it.accept(config));
            }
        }
        final Injector injector = Guice.createInjector(stage, new com.google.inject.Module[] { binder -> {
                new TypeConverters().configure(binder);
                this.bindConfig(binder, config);
                binder.bind((Class)Env.class).toInstance((Object)env);
                binder.bind((Class)Charset.class).toInstance((Object)charset);
                binder.bind((Class)Locale.class).toInstance((Object)locale);
                binder.bind((Class)ZoneId.class).toInstance((Object)zoneId);
                binder.bind((Class)TimeZone.class).toInstance((Object)TimeZone.getTimeZone(zoneId));
                binder.bind((Class)DateTimeFormatter.class).toInstance((Object)dateTimeFormatter);
                binder.bind((Class)NumberFormat.class).toInstance((Object)numberFormat);
                binder.bind((Class)DecimalFormat.class).toInstance((Object)numberFormat);
                final LifecycleProcessor lifecycleProcessor = new LifecycleProcessor();
                binder.bind((Class)LifecycleProcessor.class).toInstance((Object)lifecycleProcessor);
                binder.bindListener(Matchers.any(), (TypeListener)lifecycleProcessor);
                final Multibinder definitions = Multibinder.newSetBinder(binder, (Class)Route.Definition.class);
                final Multibinder sockets = Multibinder.newSetBinder(binder, (Class)WebSocket.Definition.class);
                final File tmpdir = new File(config.getString("application.tmpdir"));
                tmpdir.mkdirs();
                binder.bind((Class)File.class).annotatedWith((Annotation)Names.named("application.tmpdir")).toInstance((Object)tmpdir);
                final RouteMetadata classInfo = new RouteMetadata(env);
                binder.bind((Class)ParameterNameProvider.class).toInstance((Object)classInfo);
                final Multibinder ehandlers = Multibinder.newSetBinder(binder, (Class)Err.Handler.class);
                final Multibinder parsers = Multibinder.newSetBinder(binder, (Class)Parser.class);
                final Multibinder renderers = Multibinder.newSetBinder(binder, (Class)Renderer.class);
                final Multibinder multibinder;
                final Multibinder multibinder2;
                final Multibinder multibinder3;
                final Multibinder multibinder4;
                final Multibinder multibinder5;
                final RouteMetadata classInfo2;
                this.bag.forEach(candidate -> {
                    if (candidate instanceof Module) {
                        this.install((Module)candidate, env, config, binder);
                    }
                    else if (candidate instanceof Route.Definition) {
                        multibinder.addBinding().toInstance((Object)candidate);
                    }
                    else if (candidate instanceof Route.Namespace) {
                        ((Route.Namespace)candidate).routes().forEach(r -> multibinder.addBinding().toInstance((Object)r));
                    }
                    else if (candidate instanceof WebSocket.Definition) {
                        multibinder2.addBinding().toInstance((Object)candidate);
                    }
                    else if (candidate instanceof Parser) {
                        multibinder3.addBinding().toInstance((Object)candidate);
                    }
                    else if (candidate instanceof Renderer) {
                        multibinder4.addBinding().toInstance((Object)candidate);
                    }
                    else if (candidate instanceof Err.Handler) {
                        multibinder5.addBinding().toInstance((Object)candidate);
                    }
                    else {
                        binder.bind((Class)candidate);
                        MvcRoutes.routes(env, classInfo2, candidate).forEach(route -> multibinder.addBinding().toInstance((Object)route));
                    }
                    return;
                });
                parsers.addBinding().toInstance((Object)BuiltinParser.Basic);
                parsers.addBinding().toInstance((Object)BuiltinParser.Collection);
                parsers.addBinding().toInstance((Object)BuiltinParser.Optional);
                parsers.addBinding().toInstance((Object)BuiltinParser.Enum);
                parsers.addBinding().toInstance((Object)BuiltinParser.Upload);
                parsers.addBinding().toInstance((Object)BuiltinParser.Bytes);
                parsers.addBinding().toInstance((Object)new DateParser(dateFormat));
                parsers.addBinding().toInstance((Object)new LocalDateParser(dateTimeFormatter));
                parsers.addBinding().toInstance((Object)new LocaleParser());
                parsers.addBinding().toInstance((Object)new BeanParser());
                parsers.addBinding().toInstance((Object)new StaticMethodParser("valueOf"));
                parsers.addBinding().toInstance((Object)new StaticMethodParser("fromString"));
                parsers.addBinding().toInstance((Object)new StaticMethodParser("forName"));
                parsers.addBinding().toInstance((Object)new StringConstructorParser());
                binder.bind((Class)ParserExecutor.class).in((Class)Singleton.class);
                renderers.addBinding().toInstance((Object)BuiltinRenderer.Asset);
                renderers.addBinding().toInstance((Object)BuiltinRenderer.Bytes);
                renderers.addBinding().toInstance((Object)BuiltinRenderer.ByteBuffer);
                renderers.addBinding().toInstance((Object)BuiltinRenderer.File);
                renderers.addBinding().toInstance((Object)BuiltinRenderer.CharBuffer);
                renderers.addBinding().toInstance((Object)BuiltinRenderer.InputStream);
                renderers.addBinding().toInstance((Object)BuiltinRenderer.Reader);
                renderers.addBinding().toInstance((Object)BuiltinRenderer.FileChannel);
                renderers.addBinding().toInstance((Object)new DefaulErrRenderer());
                renderers.addBinding().toInstance((Object)BuiltinRenderer.ToString);
                binder.bind((Class)HttpHandler.class).to((Class)HttpHandlerImpl.class).in((Class)Singleton.class);
                final RequestScope requestScope = new RequestScope();
                binder.bind((Class)RequestScope.class).toInstance((Object)requestScope);
                binder.bindScope((Class)RequestScoped.class, (Scope)requestScope);
                binder.bind((Class)SessionManager.class).asEagerSingleton();
                binder.bind((Class)Session.Definition.class).toInstance((Object)this.session);
                final Object sstore = this.session.store();
                if (sstore instanceof Class) {
                    binder.bind((Class)Session.Store.class).to((Class)sstore).asEagerSingleton();
                }
                else {
                    binder.bind((Class)Session.Store.class).toInstance((Object)sstore);
                }
                binder.bind((Class)Request.class).toProvider((Provider)Providers.outOfScope(Request.class)).in((Class)RequestScoped.class);
                binder.bind((Class)Response.class).toProvider((Provider)Providers.outOfScope(Response.class)).in((Class)RequestScoped.class);
                binder.bind((Class)Session.class).toProvider((Provider)Providers.outOfScope(Session.class)).in((Class)RequestScoped.class);
                ehandlers.addBinding().toInstance((Object)new Err.DefHandler());
            } });
        return injector;
    }
    
    public void stop() {
        if (this.injector != null) {
            this.stopManaged();
            try {
                final Server server = (Server)this.injector.getInstance((Class)Server.class);
                final String serverName = server.getClass().getSimpleName().replace("Server", "").toLowerCase();
                server.stop();
                this.log.info("[{}] Server stopped", (Object)serverName);
            }
            catch (Exception ex) {
                this.log.error("Web server didn't stop normally", (Throwable)ex);
            }
            this.injector = null;
        }
    }
    
    private void stopManaged() {
        ((LifecycleProcessor)this.injector.getInstance((Class)LifecycleProcessor.class)).destroy();
        this.modules.clear();
    }
    
    private Config buildConfig(final Config source) {
        Config system = ConfigFactory.systemProperties();
        final Config tmpdir = source.hasPath("java.io.tmpdir") ? source : system;
        system = system.withValue("file.encoding", ConfigValueFactory.fromAnyRef((Object)System.getProperty("file.encoding"))).withValue("java.io.tmpdir", ConfigValueFactory.fromAnyRef((Object)Paths.get(tmpdir.getString("java.io.tmpdir"), new String[0]).normalize().toString()));
        Config moduleStack = ConfigFactory.empty();
        for (final Module module : ImmutableList.copyOf((Collection)this.modules).reverse()) {
            moduleStack = moduleStack.withFallback((ConfigMergeable)module.config());
        }
        final String env = Arrays.asList(system, source).stream().filter(it -> it.hasPath("application.env")).findFirst().map(c -> c.getString("application.env")).orElse("dev");
        final Config modeConfig = this.modeConfig(source, env);
        final Config config = modeConfig.withFallback((ConfigMergeable)source);
        return system.withFallback((ConfigMergeable)config).withFallback((ConfigMergeable)moduleStack).withFallback((ConfigMergeable)MediaType.types).withFallback((ConfigMergeable)this.defaultConfig(config)).resolve();
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
        final File froot = new File(fname);
        final File fconfig = new File("config", fname);
        Config config = ConfigFactory.empty();
        if (froot.exists()) {
            config = config.withFallback((ConfigMergeable)ConfigFactory.parseFile(froot));
        }
        if (fconfig.exists()) {
            config = config.withFallback((ConfigMergeable)ConfigFactory.parseFile(fconfig));
        }
        return config;
    }
    
    private Config defaultConfig(final Config config) {
        final String ns = this.getClass().getPackage().getName();
        final String[] parts = ns.split("\\.");
        final String appname = parts[parts.length - 1];
        Locale locale;
        if (!config.hasPath("application.lang")) {
            locale = Locale.getDefault();
        }
        else {
            locale = LocaleUtils.toLocale(config.getString("application.lang"));
        }
        final String lang = locale.getLanguage() + "_" + locale.getCountry();
        String tz;
        if (!config.hasPath("application.tz")) {
            tz = ZoneId.systemDefault().getId();
        }
        else {
            tz = config.getString("application.tz");
        }
        String nf;
        if (!config.hasPath("application.numberFormat")) {
            nf = ((DecimalFormat)NumberFormat.getInstance(locale)).toPattern();
        }
        else {
            nf = config.getString("application.numberFormat");
        }
        final int processors = Runtime.getRuntime().availableProcessors();
        final Config defs = ConfigFactory.parseResources((Class)Jooby.class, "jooby.conf").withValue("application.name", ConfigValueFactory.fromAnyRef((Object)appname)).withValue("application.ns", ConfigValueFactory.fromAnyRef((Object)ns)).withValue("application.lang", ConfigValueFactory.fromAnyRef((Object)lang)).withValue("application.tz", ConfigValueFactory.fromAnyRef((Object)tz)).withValue("application.numberFormat", ConfigValueFactory.fromAnyRef((Object)nf)).withValue("runtime.processors", ConfigValueFactory.fromAnyRef((Object)processors)).withValue("runtime.processors-plus1", ConfigValueFactory.fromAnyRef((Object)(processors + 1))).withValue("runtime.processors-plus2", ConfigValueFactory.fromAnyRef((Object)(processors + 2))).withValue("runtime.processors-x2", ConfigValueFactory.fromAnyRef((Object)(processors * 2)));
        return defs;
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
        this.traverse(binder, "", config.root());
        for (final Map.Entry<String, ConfigValue> entry : config.entrySet()) {
            final String name = entry.getKey();
            final Named named = Names.named(name);
            final Object value = entry.getValue().unwrapped();
            if (value instanceof List) {
                final List<Object> values = (List<Object>)value;
                final Type listType = (Type)((values.size() == 0) ? String.class : Types.listOf((Type)values.iterator().next().getClass()));
                final Key<Object> key = (Key<Object>)Key.get(listType, (Annotation)Names.named(name));
                binder.bind((Key)key).toInstance((Object)values);
            }
            else {
                binder.bindConstant().annotatedWith((Annotation)named).to(value.toString());
            }
        }
        binder.bind((Class)Config.class).toInstance((Object)config);
    }
    
    private void traverse(final Binder binder, final String p, final ConfigObject root) {
        ConfigObject child;
        String path;
        Named named;
        root.forEach((n, v) -> {
            if (v instanceof ConfigObject) {
                child = v;
                path = p + n;
                named = Names.named(path);
                binder.bind((Class)Config.class).annotatedWith((Annotation)named).toInstance((Object)child.toConfig());
                this.traverse(binder, path + ".", child);
            }
        });
    }
    
    private static Predicate<String> envpredicate(final String candidate) {
        return env -> env.equalsIgnoreCase(candidate) || candidate.equals("*");
    }
    
    static {
        System.setProperty("pid", System.getProperty("pid", JvmInfo.pid() + ""));
        final String logback = System.getProperty("logback.configurationFile");
        if (Strings.isNullOrEmpty(logback)) {
            System.setProperty("logback.configurationFile", "logback.xml");
        }
    }
    
    public interface Module
    {
        default Config config() {
            return ConfigFactory.empty();
        }
        
        void configure(final Env env, final Config conf, final Binder binder);
    }
}
