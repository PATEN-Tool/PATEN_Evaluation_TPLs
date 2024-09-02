// 
// Decompiled by Procyon v0.5.36
// 

package org.jooby;

import java.util.function.BiConsumer;
import com.google.common.collect.ImmutableMap;
import javax.annotation.Nonnull;
import org.jooby.internal.JvmInfo;
import org.jooby.scope.Providers;
import org.jooby.internal.ServerSessionManager;
import org.jooby.internal.CookieSessionManager;
import org.jooby.internal.SessionManager;
import com.google.inject.Scope;
import org.jooby.scope.RequestScoped;
import org.jooby.internal.RequestScope;
import org.jooby.internal.HttpHandlerImpl;
import org.jooby.spi.HttpHandler;
import org.jooby.internal.DefaulErrRenderer;
import org.jooby.internal.parser.ParserExecutor;
import org.jooby.internal.parser.StringConstructorParser;
import org.jooby.internal.parser.StaticMethodParser;
import org.jooby.internal.parser.LocaleParser;
import org.jooby.internal.parser.ZonedDateTimeParser;
import org.jooby.internal.parser.LocalDateParser;
import org.jooby.internal.parser.DateParser;
import org.jooby.internal.BuiltinRenderer;
import org.jooby.internal.BuiltinParser;
import org.jooby.internal.ParameterNameProvider;
import org.jooby.internal.ssl.SslContextProvider;
import javax.net.ssl.SSLContext;
import java.util.TimeZone;
import com.google.inject.TypeLiteral;
import org.jooby.internal.TypeConverters;
import java.util.concurrent.TimeUnit;
import org.jooby.internal.SourceProvider;
import org.slf4j.LoggerFactory;
import java.net.URL;
import com.typesafe.config.ConfigObject;
import com.google.inject.name.Named;
import java.lang.reflect.Type;
import com.google.inject.util.Types;
import com.typesafe.config.ConfigValue;
import com.google.inject.internal.ProviderMethodsModule;
import java.text.NumberFormat;
import com.google.common.base.Joiner;
import java.util.Map;
import java.util.HashMap;
import com.google.common.collect.UnmodifiableIterator;
import java.nio.file.Paths;
import com.typesafe.config.ConfigValueFactory;
import com.google.inject.multibindings.Multibinder;
import com.google.common.escape.Escaper;
import com.google.common.html.HtmlEscapers;
import com.google.common.net.UrlEscapers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.jooby.internal.ServerExecutorProvider;
import com.google.common.util.concurrent.MoreExecutors;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.jooby.internal.LocaleUtils;
import org.jooby.internal.mvc.MvcRoutes;
import org.jooby.internal.RouteMetadata;
import javax.inject.Singleton;
import com.google.inject.Provider;
import java.lang.annotation.Annotation;
import com.google.inject.name.Names;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.Iterator;
import org.slf4j.Logger;
import org.jooby.internal.AppPrinter;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import org.jooby.funzy.Try;
import com.typesafe.config.ConfigMergeable;
import java.util.function.Supplier;
import java.util.function.Function;
import org.jooby.mvc.Produces;
import java.util.Arrays;
import org.jooby.mvc.Consumes;
import org.jooby.internal.mvc.MvcWebSocket;
import java.io.File;
import com.typesafe.config.ConfigFactory;
import org.jooby.handlers.AssetHandler;
import java.nio.file.Path;
import org.jooby.internal.handlers.TraceHandler;
import org.jooby.internal.handlers.OptionsHandler;
import org.jooby.internal.handlers.HeadHandler;
import org.jooby.internal.parser.BeanParser;
import com.google.inject.ProvisionException;
import com.google.common.base.Preconditions;
import com.google.inject.Key;
import java.util.function.Predicate;
import java.util.Collection;
import java.util.Objects;
import org.jooby.spi.Server;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import com.google.inject.Guice;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import com.google.inject.Module;
import com.google.inject.Stage;
import java.util.function.BiFunction;
import com.google.inject.Binder;
import java.util.function.Consumer;
import java.time.ZoneId;
import java.nio.charset.Charset;
import org.jooby.internal.ServerLookup;
import java.util.Optional;
import org.jooby.funzy.Throwing;
import java.util.List;
import com.google.inject.Injector;
import java.util.concurrent.atomic.AtomicBoolean;
import com.typesafe.config.Config;
import java.util.Set;

public class Jooby implements Router, LifeCycle, Registry
{
    private transient Set<Object> bag;
    private transient Config srcconf;
    private final transient AtomicBoolean started;
    private transient Injector injector;
    private transient Session.Definition session;
    private transient Env.Builder env;
    private transient String prefix;
    private transient List<Throwing.Consumer<Registry>> onStart;
    private transient List<Throwing.Consumer<Registry>> onStarted;
    private transient List<Throwing.Consumer<Registry>> onStop;
    private transient Route.Mapper mapper;
    private transient Set<String> mappers;
    private transient Optional<Parser> beanParser;
    private transient ServerLookup server;
    private transient String dateFormat;
    private transient Charset charset;
    private transient String[] languages;
    private transient ZoneId zoneId;
    private transient Integer port;
    private transient Integer securePort;
    private transient String numberFormat;
    private transient boolean http2;
    private transient List<Consumer<Binder>> executors;
    private transient boolean defaultExecSet;
    private boolean throwBootstrapException;
    private transient BiFunction<Stage, com.google.inject.Module, Injector> injectorFactory;
    private transient List<Jooby> apprefs;
    private transient LinkedList<String> path;
    private transient String confname;
    private transient boolean caseSensitiveRouting;
    private transient String classname;
    
    public Jooby() {
        this(null);
    }
    
    public Jooby(final String prefix) {
        this.bag = new LinkedHashSet<Object>();
        this.started = new AtomicBoolean(false);
        this.session = new Session.Definition(Session.Mem.class);
        this.env = Env.DEFAULT;
        this.onStart = new ArrayList<Throwing.Consumer<Registry>>();
        this.onStarted = new ArrayList<Throwing.Consumer<Registry>>();
        this.onStop = new ArrayList<Throwing.Consumer<Registry>>();
        this.mappers = new HashSet<String>();
        this.beanParser = Optional.empty();
        this.server = new ServerLookup();
        this.executors = new ArrayList<Consumer<Binder>>();
        this.injectorFactory = (BiFunction<Stage, com.google.inject.Module, Injector>)((x$0, xva$1) -> Guice.createInjector(x$0, new com.google.inject.Module[] { xva$1 }));
        this.path = new LinkedList<String>();
        this.caseSensitiveRouting = true;
        this.prefix = prefix;
        this.use(this.server);
        this.classname = this.classname(this.getClass().getName());
    }
    
    @Override
    public Route.Collection path(final String path, final Runnable action) {
        this.path.addLast(Route.normalize(path));
        final Route.Collection collection = this.with(action);
        this.path.removeLast();
        return collection;
    }
    
    @Override
    public Jooby use(final Jooby app) {
        return this.use(this.prefixPath(null), app);
    }
    
    private Optional<String> prefixPath(@Nullable final String tail) {
        return (this.path.size() == 0) ? ((tail == null) ? Optional.empty() : Optional.of(Route.normalize(tail))) : Optional.of((String)this.path.stream().collect((Collector<? super Object, ?, T>)Collectors.joining("", "", (tail == null) ? "" : Route.normalize(tail))));
    }
    
    @Override
    public Jooby use(final String path, final Jooby app) {
        return this.use(this.prefixPath(path), app);
    }
    
    public Jooby server(final Class<? extends Server> server) {
        Objects.requireNonNull(server, "Server required.");
        final List<Object> tmp = this.bag.stream().skip(1L).collect((Collector<? super Object, ?, List<Object>>)Collectors.toList());
        tmp.add(0, (env, conf, binder) -> binder.bind((Class)Server.class).to((Class)server).asEagerSingleton());
        this.bag.clear();
        this.bag.addAll(tmp);
        return this;
    }
    
    private Jooby use(final Optional<String> path, final Jooby app) {
        // 
        // This method could not be decompiled.
        // 
        // Original Bytecode:
        // 
        //     1: ldc_w           "App is required."
        //     4: invokestatic    java/util/Objects.requireNonNull:(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
        //     7: pop            
        //     8: aload_1         /* path */
        //     9: invokedynamic   BootstrapMethod #2, apply:(Ljava/util/Optional;)Ljava/util/function/Function;
        //    14: astore_3        /* rewrite */
        //    15: aload_2         /* app */
        //    16: getfield        org/jooby/Jooby.bag:Ljava/util/Set;
        //    19: aload_0         /* this */
        //    20: aload_3         /* rewrite */
        //    21: aload_1         /* path */
        //    22: invokedynamic   BootstrapMethod #3, accept:(Lorg/jooby/Jooby;Ljava/util/function/Function;Ljava/util/Optional;)Ljava/util/function/Consumer;
        //    27: invokeinterface java/util/Set.forEach:(Ljava/util/function/Consumer;)V
        //    32: aload_2         /* app */
        //    33: getfield        org/jooby/Jooby.onStart:Ljava/util/List;
        //    36: aload_0         /* this */
        //    37: getfield        org/jooby/Jooby.onStart:Ljava/util/List;
        //    40: dup            
        //    41: invokevirtual   java/lang/Object.getClass:()Ljava/lang/Class;
        //    44: pop            
        //    45: invokedynamic   BootstrapMethod #4, accept:(Ljava/util/List;)Ljava/util/function/Consumer;
        //    50: invokeinterface java/util/List.forEach:(Ljava/util/function/Consumer;)V
        //    55: aload_2         /* app */
        //    56: getfield        org/jooby/Jooby.onStarted:Ljava/util/List;
        //    59: aload_0         /* this */
        //    60: getfield        org/jooby/Jooby.onStarted:Ljava/util/List;
        //    63: dup            
        //    64: invokevirtual   java/lang/Object.getClass:()Ljava/lang/Class;
        //    67: pop            
        //    68: invokedynamic   BootstrapMethod #4, accept:(Ljava/util/List;)Ljava/util/function/Consumer;
        //    73: invokeinterface java/util/List.forEach:(Ljava/util/function/Consumer;)V
        //    78: aload_2         /* app */
        //    79: getfield        org/jooby/Jooby.onStop:Ljava/util/List;
        //    82: aload_0         /* this */
        //    83: getfield        org/jooby/Jooby.onStop:Ljava/util/List;
        //    86: dup            
        //    87: invokevirtual   java/lang/Object.getClass:()Ljava/lang/Class;
        //    90: pop            
        //    91: invokedynamic   BootstrapMethod #4, accept:(Ljava/util/List;)Ljava/util/function/Consumer;
        //    96: invokeinterface java/util/List.forEach:(Ljava/util/function/Consumer;)V
        //   101: aload_2         /* app */
        //   102: getfield        org/jooby/Jooby.mapper:Lorg/jooby/Route$Mapper;
        //   105: ifnull          117
        //   108: aload_0         /* this */
        //   109: aload_2         /* app */
        //   110: getfield        org/jooby/Jooby.mapper:Lorg/jooby/Route$Mapper;
        //   113: invokevirtual   org/jooby/Jooby.map:(Lorg/jooby/Route$Mapper;)Lorg/jooby/Jooby;
        //   116: pop            
        //   117: aload_0         /* this */
        //   118: getfield        org/jooby/Jooby.apprefs:Ljava/util/List;
        //   121: ifnonnull       135
        //   124: aload_0         /* this */
        //   125: new             Ljava/util/ArrayList;
        //   128: dup            
        //   129: invokespecial   java/util/ArrayList.<init>:()V
        //   132: putfield        org/jooby/Jooby.apprefs:Ljava/util/List;
        //   135: aload_0         /* this */
        //   136: getfield        org/jooby/Jooby.apprefs:Ljava/util/List;
        //   139: aload_2         /* app */
        //   140: invokeinterface java/util/List.add:(Ljava/lang/Object;)Z
        //   145: pop            
        //   146: aload_0         /* this */
        //   147: areturn        
        //    Signature:
        //  (Ljava/util/Optional<Ljava/lang/String;>;Lorg/jooby/Jooby;)Lorg/jooby/Jooby;
        //    MethodParameters:
        //  Name  Flags  
        //  ----  -----
        //  path  FINAL
        //  app   FINAL
        //    StackMapTable: 00 02 FC 00 75 07 01 DC 11
        // 
        // The error that occurred was:
        // 
        // java.lang.IllegalStateException: Could not infer any expression.
        //     at com.strobel.decompiler.ast.TypeAnalysis.runInference(TypeAnalysis.java:374)
        //     at com.strobel.decompiler.ast.TypeAnalysis.run(TypeAnalysis.java:96)
        //     at com.strobel.decompiler.ast.AstOptimizer.optimize(AstOptimizer.java:344)
        //     at com.strobel.decompiler.ast.AstOptimizer.optimize(AstOptimizer.java:42)
        //     at com.strobel.decompiler.languages.java.ast.AstMethodBodyBuilder.createMethodBody(AstMethodBodyBuilder.java:214)
        //     at com.strobel.decompiler.languages.java.ast.AstMethodBodyBuilder.createMethodBody(AstMethodBodyBuilder.java:99)
        //     at com.strobel.decompiler.languages.java.ast.AstBuilder.createMethodBody(AstBuilder.java:782)
        //     at com.strobel.decompiler.languages.java.ast.AstBuilder.createMethod(AstBuilder.java:675)
        //     at com.strobel.decompiler.languages.java.ast.AstBuilder.addTypeMembers(AstBuilder.java:552)
        //     at com.strobel.decompiler.languages.java.ast.AstBuilder.createTypeCore(AstBuilder.java:519)
        //     at com.strobel.decompiler.languages.java.ast.AstBuilder.createTypeNoCache(AstBuilder.java:161)
        //     at com.strobel.decompiler.languages.java.ast.AstBuilder.createType(AstBuilder.java:150)
        //     at com.strobel.decompiler.languages.java.ast.AstBuilder.addType(AstBuilder.java:125)
        //     at com.strobel.decompiler.languages.java.JavaLanguage.buildAst(JavaLanguage.java:71)
        //     at com.strobel.decompiler.languages.java.JavaLanguage.decompileType(JavaLanguage.java:59)
        //     at com.strobel.decompiler.DecompilerDriver.decompileType(DecompilerDriver.java:330)
        //     at com.strobel.decompiler.DecompilerDriver.decompileJar(DecompilerDriver.java:251)
        //     at com.strobel.decompiler.DecompilerDriver.main(DecompilerDriver.java:126)
        // 
        throw new IllegalStateException("An error occurred while decompiling this method.");
    }
    
    public Jooby env(final Env.Builder env) {
        this.env = Objects.requireNonNull(env, "Env builder is required.");
        return this;
    }
    
    @Override
    public Jooby onStart(final Throwing.Runnable callback) {
        super.onStart(callback);
        return this;
    }
    
    @Override
    public Jooby onStart(final Throwing.Consumer<Registry> callback) {
        Objects.requireNonNull(callback, "Callback is required.");
        this.onStart.add(callback);
        return this;
    }
    
    @Override
    public Jooby onStarted(final Throwing.Runnable callback) {
        super.onStarted(callback);
        return this;
    }
    
    @Override
    public Jooby onStarted(final Throwing.Consumer<Registry> callback) {
        Objects.requireNonNull(callback, "Callback is required.");
        this.onStarted.add(callback);
        return this;
    }
    
    @Override
    public Jooby onStop(final Throwing.Runnable callback) {
        super.onStop(callback);
        return this;
    }
    
    @Override
    public Jooby onStop(final Throwing.Consumer<Registry> callback) {
        Objects.requireNonNull(callback, "Callback is required.");
        this.onStop.add(callback);
        return this;
    }
    
    public EnvPredicate on(final String env, final Runnable callback) {
        Objects.requireNonNull(env, "Env is required.");
        return this.on(envpredicate(env), callback);
    }
    
    public EnvPredicate on(final String env, final Consumer<Config> callback) {
        Objects.requireNonNull(env, "Env is required.");
        return this.on(envpredicate(env), callback);
    }
    
    public EnvPredicate on(final Predicate<String> predicate, final Runnable callback) {
        Objects.requireNonNull(predicate, "Predicate is required.");
        Objects.requireNonNull(callback, "Callback is required.");
        return this.on(predicate, conf -> callback.run());
    }
    
    public EnvPredicate on(final Predicate<String> predicate, final Consumer<Config> callback) {
        Objects.requireNonNull(predicate, "Predicate is required.");
        Objects.requireNonNull(callback, "Callback is required.");
        this.bag.add(new EnvDep(predicate, callback));
        return otherwise -> this.bag.add(new EnvDep(predicate.negate(), otherwise));
    }
    
    public Jooby on(final String env1, final String env2, final String env3, final Runnable callback) {
        this.on(envpredicate(env1).or(envpredicate(env2)).or(envpredicate(env3)), callback);
        return this;
    }
    
    @Override
    public <T> T require(final Key<T> type) {
        Preconditions.checkState(this.injector != null, (Object)"Registry is not ready. Require calls are available at application startup time, see http://jooby.org/doc/#application-life-cycle");
        try {
            return (T)this.injector.getInstance((Key)type);
        }
        catch (ProvisionException x) {
            final Throwable cause = x.getCause();
            if (cause instanceof Err) {
                throw (Err)cause;
            }
            throw x;
        }
    }
    
    @Override
    public Route.OneArgHandler promise(final Deferred.Initializer initializer) {
        return req -> new Deferred(initializer);
    }
    
    @Override
    public Route.OneArgHandler promise(final String executor, final Deferred.Initializer initializer) {
        return req -> new Deferred(executor, initializer);
    }
    
    @Override
    public Route.OneArgHandler promise(final Deferred.Initializer0 initializer) {
        return req -> new Deferred(initializer);
    }
    
    @Override
    public Route.OneArgHandler promise(final String executor, final Deferred.Initializer0 initializer) {
        return req -> new Deferred(executor, initializer);
    }
    
    public Session.Definition session(final Class<? extends Session.Store> store) {
        return this.session = new Session.Definition(Objects.requireNonNull(store, "A session store is required."));
    }
    
    public Session.Definition cookieSession() {
        return this.session = new Session.Definition();
    }
    
    public Session.Definition session(final Session.Store store) {
        return this.session = new Session.Definition(Objects.requireNonNull(store, "A session store is required."));
    }
    
    public Jooby parser(final Parser parser) {
        if (parser instanceof BeanParser) {
            this.beanParser = Optional.of(parser);
        }
        else {
            this.bag.add(Objects.requireNonNull(parser, "A parser is required."));
        }
        return this;
    }
    
    public Jooby renderer(final Renderer renderer) {
        this.bag.add(Objects.requireNonNull(renderer, "A renderer is required."));
        return this;
    }
    
    @Override
    public Route.Definition before(final String method, final String pattern, final Route.Before handler) {
        return this.appendDefinition(method, pattern, handler);
    }
    
    @Override
    public Route.Definition after(final String method, final String pattern, final Route.After handler) {
        return this.appendDefinition(method, pattern, handler);
    }
    
    @Override
    public Route.Definition complete(final String method, final String pattern, final Route.Complete handler) {
        return this.appendDefinition(method, pattern, handler);
    }
    
    @Override
    public Route.Definition use(final String path, final Route.Filter filter) {
        return this.appendDefinition("*", path, filter);
    }
    
    @Override
    public Route.Definition use(final String verb, final String path, final Route.Filter filter) {
        return this.appendDefinition(verb, path, filter);
    }
    
    @Override
    public Route.Definition use(final String verb, final String path, final Route.Handler handler) {
        return this.appendDefinition(verb, path, handler);
    }
    
    @Override
    public Route.Definition use(final String path, final Route.Handler handler) {
        return this.appendDefinition("*", path, handler);
    }
    
    @Override
    public Route.Definition use(final String path, final Route.OneArgHandler handler) {
        return this.appendDefinition("*", path, handler);
    }
    
    @Override
    public Route.Definition get(final String path, final Route.Handler handler) {
        return this.appendDefinition("GET", path, handler);
    }
    
    @Override
    public Route.Collection get(final String path1, final String path2, final Route.Handler handler) {
        return new Route.Collection((Route.Props[])new Route.Definition[] { this.get(path1, handler), this.get(path2, handler) });
    }
    
    @Override
    public Route.Collection get(final String path1, final String path2, final String path3, final Route.Handler handler) {
        return new Route.Collection((Route.Props[])new Route.Definition[] { this.get(path1, handler), this.get(path2, handler), this.get(path3, handler) });
    }
    
    @Override
    public Route.Definition get(final String path, final Route.OneArgHandler handler) {
        return this.appendDefinition("GET", path, handler);
    }
    
    @Override
    public Route.Collection get(final String path1, final String path2, final Route.OneArgHandler handler) {
        return new Route.Collection((Route.Props[])new Route.Definition[] { this.get(path1, handler), this.get(path2, handler) });
    }
    
    @Override
    public Route.Collection get(final String path1, final String path2, final String path3, final Route.OneArgHandler handler) {
        return new Route.Collection((Route.Props[])new Route.Definition[] { this.get(path1, handler), this.get(path2, handler), this.get(path3, handler) });
    }
    
    @Override
    public Route.Definition get(final String path, final Route.ZeroArgHandler handler) {
        return this.appendDefinition("GET", path, handler);
    }
    
    @Override
    public Route.Collection get(final String path1, final String path2, final Route.ZeroArgHandler handler) {
        return new Route.Collection((Route.Props[])new Route.Definition[] { this.get(path1, handler), this.get(path2, handler) });
    }
    
    @Override
    public Route.Collection get(final String path1, final String path2, final String path3, final Route.ZeroArgHandler handler) {
        return new Route.Collection((Route.Props[])new Route.Definition[] { this.get(path1, handler), this.get(path2, handler), this.get(path3, handler) });
    }
    
    @Override
    public Route.Definition get(final String path, final Route.Filter filter) {
        return this.appendDefinition("GET", path, filter);
    }
    
    @Override
    public Route.Collection get(final String path1, final String path2, final Route.Filter filter) {
        return new Route.Collection((Route.Props[])new Route.Definition[] { this.get(path1, filter), this.get(path2, filter) });
    }
    
    @Override
    public Route.Collection get(final String path1, final String path2, final String path3, final Route.Filter filter) {
        return new Route.Collection((Route.Props[])new Route.Definition[] { this.get(path1, filter), this.get(path2, filter), this.get(path3, filter) });
    }
    
    @Override
    public Route.Definition post(final String path, final Route.Handler handler) {
        return this.appendDefinition("POST", path, handler);
    }
    
    @Override
    public Route.Collection post(final String path1, final String path2, final Route.Handler handler) {
        return new Route.Collection((Route.Props[])new Route.Definition[] { this.post(path1, handler), this.post(path2, handler) });
    }
    
    @Override
    public Route.Collection post(final String path1, final String path2, final String path3, final Route.Handler handler) {
        return new Route.Collection((Route.Props[])new Route.Definition[] { this.post(path1, handler), this.post(path2, handler), this.post(path3, handler) });
    }
    
    @Override
    public Route.Definition post(final String path, final Route.OneArgHandler handler) {
        return this.appendDefinition("POST", path, handler);
    }
    
    @Override
    public Route.Collection post(final String path1, final String path2, final Route.OneArgHandler handler) {
        return new Route.Collection((Route.Props[])new Route.Definition[] { this.post(path1, handler), this.post(path2, handler) });
    }
    
    @Override
    public Route.Collection post(final String path1, final String path2, final String path3, final Route.OneArgHandler handler) {
        return new Route.Collection((Route.Props[])new Route.Definition[] { this.post(path1, handler), this.post(path2, handler), this.post(path3, handler) });
    }
    
    @Override
    public Route.Definition post(final String path, final Route.ZeroArgHandler handler) {
        return this.appendDefinition("POST", path, handler);
    }
    
    @Override
    public Route.Collection post(final String path1, final String path2, final Route.ZeroArgHandler handler) {
        return new Route.Collection((Route.Props[])new Route.Definition[] { this.post(path1, handler), this.post(path2, handler) });
    }
    
    @Override
    public Route.Collection post(final String path1, final String path2, final String path3, final Route.ZeroArgHandler handler) {
        return new Route.Collection((Route.Props[])new Route.Definition[] { this.post(path1, handler), this.post(path2, handler), this.post(path3, handler) });
    }
    
    @Override
    public Route.Definition post(final String path, final Route.Filter filter) {
        return this.appendDefinition("POST", path, filter);
    }
    
    @Override
    public Route.Collection post(final String path1, final String path2, final Route.Filter filter) {
        return new Route.Collection((Route.Props[])new Route.Definition[] { this.post(path1, filter), this.post(path2, filter) });
    }
    
    @Override
    public Route.Collection post(final String path1, final String path2, final String path3, final Route.Filter filter) {
        return new Route.Collection((Route.Props[])new Route.Definition[] { this.post(path1, filter), this.post(path2, filter), this.post(path3, filter) });
    }
    
    @Override
    public Route.Definition head(final String path, final Route.Handler handler) {
        return this.appendDefinition("HEAD", path, handler);
    }
    
    @Override
    public Route.Definition head(final String path, final Route.OneArgHandler handler) {
        return this.appendDefinition("HEAD", path, handler);
    }
    
    @Override
    public Route.Definition head(final String path, final Route.ZeroArgHandler handler) {
        return this.appendDefinition("HEAD", path, handler);
    }
    
    @Override
    public Route.Definition head(final String path, final Route.Filter filter) {
        return this.appendDefinition("HEAD", path, filter);
    }
    
    @Override
    public Route.Definition head() {
        return this.appendDefinition("HEAD", "*", this.filter(HeadHandler.class)).name("*.head");
    }
    
    @Override
    public Route.Definition options(final String path, final Route.Handler handler) {
        return this.appendDefinition("OPTIONS", path, handler);
    }
    
    @Override
    public Route.Definition options(final String path, final Route.OneArgHandler handler) {
        return this.appendDefinition("OPTIONS", path, handler);
    }
    
    @Override
    public Route.Definition options(final String path, final Route.ZeroArgHandler handler) {
        return this.appendDefinition("OPTIONS", path, handler);
    }
    
    @Override
    public Route.Definition options(final String path, final Route.Filter filter) {
        return this.appendDefinition("OPTIONS", path, filter);
    }
    
    @Override
    public Route.Definition options() {
        return this.appendDefinition("OPTIONS", "*", this.handler(OptionsHandler.class)).name("*.options");
    }
    
    @Override
    public Route.Definition put(final String path, final Route.Handler handler) {
        return this.appendDefinition("PUT", path, handler);
    }
    
    @Override
    public Route.Collection put(final String path1, final String path2, final Route.Handler handler) {
        return new Route.Collection((Route.Props[])new Route.Definition[] { this.put(path1, handler), this.put(path2, handler) });
    }
    
    @Override
    public Route.Collection put(final String path1, final String path2, final String path3, final Route.Handler handler) {
        return new Route.Collection((Route.Props[])new Route.Definition[] { this.put(path1, handler), this.put(path2, handler), this.put(path3, handler) });
    }
    
    @Override
    public Route.Definition put(final String path, final Route.OneArgHandler handler) {
        return this.appendDefinition("PUT", path, handler);
    }
    
    @Override
    public Route.Collection put(final String path1, final String path2, final Route.OneArgHandler handler) {
        return new Route.Collection((Route.Props[])new Route.Definition[] { this.put(path1, handler), this.put(path2, handler) });
    }
    
    @Override
    public Route.Collection put(final String path1, final String path2, final String path3, final Route.OneArgHandler handler) {
        return new Route.Collection((Route.Props[])new Route.Definition[] { this.put(path1, handler), this.put(path2, handler), this.put(path3, handler) });
    }
    
    @Override
    public Route.Definition put(final String path, final Route.ZeroArgHandler handler) {
        return this.appendDefinition("PUT", path, handler);
    }
    
    @Override
    public Route.Collection put(final String path1, final String path2, final Route.ZeroArgHandler handler) {
        return new Route.Collection((Route.Props[])new Route.Definition[] { this.put(path1, handler), this.put(path2, handler) });
    }
    
    @Override
    public Route.Collection put(final String path1, final String path2, final String path3, final Route.ZeroArgHandler handler) {
        return new Route.Collection((Route.Props[])new Route.Definition[] { this.put(path1, handler), this.put(path2, handler), this.put(path3, handler) });
    }
    
    @Override
    public Route.Definition put(final String path, final Route.Filter filter) {
        return this.appendDefinition("PUT", path, filter);
    }
    
    @Override
    public Route.Collection put(final String path1, final String path2, final Route.Filter filter) {
        return new Route.Collection((Route.Props[])new Route.Definition[] { this.put(path1, filter), this.put(path2, filter) });
    }
    
    @Override
    public Route.Collection put(final String path1, final String path2, final String path3, final Route.Filter filter) {
        return new Route.Collection((Route.Props[])new Route.Definition[] { this.put(path1, filter), this.put(path2, filter), this.put(path3, filter) });
    }
    
    @Override
    public Route.Definition patch(final String path, final Route.Handler handler) {
        return this.appendDefinition("PATCH", path, handler);
    }
    
    @Override
    public Route.Collection patch(final String path1, final String path2, final Route.Handler handler) {
        return new Route.Collection((Route.Props[])new Route.Definition[] { this.patch(path1, handler), this.patch(path2, handler) });
    }
    
    @Override
    public Route.Collection patch(final String path1, final String path2, final String path3, final Route.Handler handler) {
        return new Route.Collection((Route.Props[])new Route.Definition[] { this.patch(path1, handler), this.patch(path2, handler), this.patch(path3, handler) });
    }
    
    @Override
    public Route.Definition patch(final String path, final Route.OneArgHandler handler) {
        return this.appendDefinition("PATCH", path, handler);
    }
    
    @Override
    public Route.Collection patch(final String path1, final String path2, final Route.OneArgHandler handler) {
        return new Route.Collection((Route.Props[])new Route.Definition[] { this.patch(path1, handler), this.patch(path2, handler) });
    }
    
    @Override
    public Route.Collection patch(final String path1, final String path2, final String path3, final Route.OneArgHandler handler) {
        return new Route.Collection((Route.Props[])new Route.Definition[] { this.patch(path1, handler), this.patch(path2, handler), this.patch(path3, handler) });
    }
    
    @Override
    public Route.Definition patch(final String path, final Route.ZeroArgHandler handler) {
        return this.appendDefinition("PATCH", path, handler);
    }
    
    @Override
    public Route.Collection patch(final String path1, final String path2, final Route.ZeroArgHandler handler) {
        return new Route.Collection((Route.Props[])new Route.Definition[] { this.patch(path1, handler), this.patch(path2, handler) });
    }
    
    @Override
    public Route.Collection patch(final String path1, final String path2, final String path3, final Route.ZeroArgHandler handler) {
        return new Route.Collection((Route.Props[])new Route.Definition[] { this.patch(path1, handler), this.patch(path2, handler), this.patch(path3, handler) });
    }
    
    @Override
    public Route.Definition patch(final String path, final Route.Filter filter) {
        return this.appendDefinition("PATCH", path, filter);
    }
    
    @Override
    public Route.Collection patch(final String path1, final String path2, final Route.Filter filter) {
        return new Route.Collection((Route.Props[])new Route.Definition[] { this.patch(path1, filter), this.patch(path2, filter) });
    }
    
    @Override
    public Route.Collection patch(final String path1, final String path2, final String path3, final Route.Filter filter) {
        return new Route.Collection((Route.Props[])new Route.Definition[] { this.patch(path1, filter), this.patch(path2, filter), this.patch(path3, filter) });
    }
    
    @Override
    public Route.Definition delete(final String path, final Route.Handler handler) {
        return this.appendDefinition("DELETE", path, handler);
    }
    
    @Override
    public Route.Collection delete(final String path1, final String path2, final Route.Handler handler) {
        return new Route.Collection((Route.Props[])new Route.Definition[] { this.delete(path1, handler), this.delete(path2, handler) });
    }
    
    @Override
    public Route.Collection delete(final String path1, final String path2, final String path3, final Route.Handler handler) {
        return new Route.Collection((Route.Props[])new Route.Definition[] { this.delete(path1, handler), this.delete(path2, handler), this.delete(path3, handler) });
    }
    
    @Override
    public Route.Definition delete(final String path, final Route.OneArgHandler handler) {
        return this.appendDefinition("DELETE", path, handler);
    }
    
    @Override
    public Route.Collection delete(final String path1, final String path2, final Route.OneArgHandler handler) {
        return new Route.Collection((Route.Props[])new Route.Definition[] { this.delete(path1, handler), this.delete(path2, handler) });
    }
    
    @Override
    public Route.Collection delete(final String path1, final String path2, final String path3, final Route.OneArgHandler handler) {
        return new Route.Collection((Route.Props[])new Route.Definition[] { this.delete(path1, handler), this.delete(path2, handler), this.delete(path3, handler) });
    }
    
    @Override
    public Route.Definition delete(final String path, final Route.ZeroArgHandler handler) {
        return this.appendDefinition("DELETE", path, handler);
    }
    
    @Override
    public Route.Collection delete(final String path1, final String path2, final Route.ZeroArgHandler handler) {
        return new Route.Collection((Route.Props[])new Route.Definition[] { this.delete(path1, handler), this.delete(path2, handler) });
    }
    
    @Override
    public Route.Collection delete(final String path1, final String path2, final String path3, final Route.ZeroArgHandler handler) {
        return new Route.Collection((Route.Props[])new Route.Definition[] { this.delete(path1, handler), this.delete(path2, handler), this.delete(path3, handler) });
    }
    
    @Override
    public Route.Definition delete(final String path, final Route.Filter filter) {
        return this.appendDefinition("DELETE", path, filter);
    }
    
    @Override
    public Route.Collection delete(final String path1, final String path2, final Route.Filter filter) {
        return new Route.Collection((Route.Props[])new Route.Definition[] { this.delete(path1, filter), this.delete(path2, filter) });
    }
    
    @Override
    public Route.Collection delete(final String path1, final String path2, final String path3, final Route.Filter filter) {
        return new Route.Collection((Route.Props[])new Route.Definition[] { this.delete(path1, filter), this.delete(path2, filter), this.delete(path3, filter) });
    }
    
    @Override
    public Route.Definition trace(final String path, final Route.Handler handler) {
        return this.appendDefinition("TRACE", path, handler);
    }
    
    @Override
    public Route.Definition trace(final String path, final Route.OneArgHandler handler) {
        return this.appendDefinition("TRACE", path, handler);
    }
    
    @Override
    public Route.Definition trace(final String path, final Route.ZeroArgHandler handler) {
        return this.appendDefinition("TRACE", path, handler);
    }
    
    @Override
    public Route.Definition trace(final String path, final Route.Filter filter) {
        return this.appendDefinition("TRACE", path, filter);
    }
    
    @Override
    public Route.Definition trace() {
        return this.appendDefinition("TRACE", "*", this.handler(TraceHandler.class)).name("*.trace");
    }
    
    @Override
    public Route.Definition connect(final String path, final Route.Handler handler) {
        return this.appendDefinition("CONNECT", path, handler);
    }
    
    @Override
    public Route.Definition connect(final String path, final Route.OneArgHandler handler) {
        return this.appendDefinition("CONNECT", path, handler);
    }
    
    @Override
    public Route.Definition connect(final String path, final Route.ZeroArgHandler handler) {
        return this.appendDefinition("CONNECT", path, handler);
    }
    
    @Override
    public Route.Definition connect(final String path, final Route.Filter filter) {
        return this.appendDefinition("CONNECT", path, filter);
    }
    
    private Route.Handler handler(final Class<? extends Route.Handler> handler) {
        Objects.requireNonNull(handler, "Route handler is required.");
        return (req, rsp) -> req.require(handler).handle(req, rsp);
    }
    
    private Route.Filter filter(final Class<? extends Route.Filter> filter) {
        Objects.requireNonNull(filter, "Filter is required.");
        return (req, rsp, chain) -> req.require(filter).handle(req, rsp, chain);
    }
    
    @Override
    public Route.AssetDefinition assets(final String path, final Path basedir) {
        return this.assets(path, new AssetHandler(basedir));
    }
    
    @Override
    public Route.AssetDefinition assets(final String path, final String location) {
        return this.assets(path, new AssetHandler(location));
    }
    
    @Override
    public Route.AssetDefinition assets(final String path, final AssetHandler handler) {
        final Route.AssetDefinition route = this.appendDefinition("GET", path, handler, (Throwing.Function4<String, String, Route.Filter, Boolean, Route.AssetDefinition>)Route.AssetDefinition::new);
        return this.configureAssetHandler(route);
    }
    
    @Override
    public Route.Collection use(final Class<?> routeClass) {
        return this.use("", routeClass);
    }
    
    @Override
    public Route.Collection use(final String path, final Class<?> routeClass) {
        Objects.requireNonNull(routeClass, "Route class is required.");
        Objects.requireNonNull(path, "Path is required");
        final MvcClass mvc = new MvcClass(routeClass, path, this.prefix);
        this.bag.add(mvc);
        return new Route.Collection(new Route.Props[] { mvc });
    }
    
    private Route.Definition appendDefinition(final String method, final String pattern, final Route.Filter filter) {
        return this.appendDefinition(method, pattern, filter, (Throwing.Function4<String, String, Route.Filter, Boolean, Route.Definition>)Route.Definition::new);
    }
    
    private <T extends Route.Definition> T appendDefinition(final String method, final String pattern, final Route.Filter filter, final Throwing.Function4<String, String, Route.Filter, Boolean, T> creator) {
        final String pathPattern = this.prefixPath(pattern).orElse(pattern);
        final T route = (T)creator.apply((Object)method, (Object)pathPattern, (Object)filter, (Object)this.caseSensitiveRouting);
        if (this.prefix != null) {
            route.prefix = this.prefix;
            route.name(route.name());
        }
        this.bag.add(route);
        return route;
    }
    
    public Jooby use(final Module module) {
        Objects.requireNonNull(module, "A module is required.");
        this.bag.add(module);
        return this;
    }
    
    public Jooby conf(final String path) {
        this.confname = path;
        this.use(ConfigFactory.parseResources(path));
        return this;
    }
    
    public Jooby conf(final File path) {
        this.confname = path.getName();
        this.use(ConfigFactory.parseFile(path));
        return this;
    }
    
    public Jooby use(final Config config) {
        this.srcconf = Objects.requireNonNull(config, "Config required.");
        return this;
    }
    
    @Override
    public Jooby err(final Err.Handler err) {
        this.bag.add(Objects.requireNonNull(err, "An err handler is required."));
        return this;
    }
    
    @Override
    public WebSocket.Definition ws(final String path, final WebSocket.OnOpen handler) {
        final WebSocket.Definition ws = new WebSocket.Definition(path, handler);
        Preconditions.checkArgument(this.bag.add(ws), "Duplicated path: '%s'", (Object)path);
        return ws;
    }
    
    @Override
    public <T> WebSocket.Definition ws(final String path, final Class<? extends WebSocket.OnMessage<T>> handler) {
        final String fpath = Optional.ofNullable((Object)handler.getAnnotation((Class<T>)org.jooby.mvc.Path.class)).map(it -> path + "/" + it.value()[0]).orElse(path);
        final WebSocket.Definition ws = this.ws(fpath, MvcWebSocket.newWebSocket(handler));
        Optional.ofNullable((Object)handler.getAnnotation((Class<T>)Consumes.class)).ifPresent(consumes -> Arrays.asList(consumes.value()).forEach(ws::consumes));
        Optional.ofNullable((Object)handler.getAnnotation((Class<T>)Produces.class)).ifPresent(produces -> Arrays.asList(produces.value()).forEach(ws::produces));
        return ws;
    }
    
    @Override
    public Route.Definition sse(final String path, final Sse.Handler handler) {
        return this.appendDefinition("GET", path, handler).consumes(MediaType.sse);
    }
    
    @Override
    public Route.Definition sse(final String path, final Sse.Handler1 handler) {
        return this.appendDefinition("GET", path, handler).consumes(MediaType.sse);
    }
    
    @Override
    public Route.Collection with(final Runnable callback) {
        final int size = this.bag.size();
        callback.run();
        final List<Route.Props> local = this.bag.stream().skip(size).filter(Route.Props.class::isInstance).map((Function<? super Object, ?>)Route.Props.class::cast).collect((Collector<? super Object, ?, List<Route.Props>>)Collectors.toList());
        return new Route.Collection((Route.Props[])local.toArray(new Route.Props[local.size()]));
    }
    
    public static void run(final Supplier<? extends Jooby> app, final String... args) {
        final Config conf = ConfigFactory.systemProperties().withFallback((ConfigMergeable)args(args));
        System.setProperty("logback.configurationFile", logback(conf));
        ((Jooby)app.get()).start(args);
    }
    
    public static void run(final Class<? extends Jooby> app, final String... args) {
        run(() -> (Jooby)Try.apply(() -> app.newInstance()).get(), args);
    }
    
    public static Config exportConf(final Jooby app) {
        final AtomicReference<Config> conf = new AtomicReference<Config>(ConfigFactory.empty());
        app.on("*", c -> conf.set(c));
        exportRoutes(app);
        return conf.get();
    }
    
    public static List<Route.Definition> exportRoutes(final Jooby app) {
        List<Route.Definition> routes = Collections.emptyList();
        class Success extends RuntimeException
        {
            List<Route.Definition> routes = r;
            
            Success(final List<Route.Definition> routes) {
            }
        }
        try {
            app.start(new String[0], r -> {
                throw new Success();
            });
        }
        catch (Success success) {
            routes = success.routes;
        }
        catch (Throwable x) {
            logger(app).debug("Failed bootstrap: {}", (Object)app, (Object)x);
        }
        return routes;
    }
    
    public void start() {
        this.start(new String[0]);
    }
    
    public void start(final String... args) {
        try {
            this.start(args, null);
        }
        catch (Throwable x) {
            this.stop();
            final String msg = "An error occurred while starting the application:";
            if (this.throwBootstrapException) {
                throw new Err(Status.SERVICE_UNAVAILABLE, msg, x);
            }
            logger(this).error(msg, x);
        }
    }
    
    private void start(final String[] args, final Consumer<List<Route.Definition>> routes) throws Throwable {
        final long start = System.currentTimeMillis();
        this.started.set(true);
        this.injector = this.bootstrap(args(args), routes);
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
        final Config conf = (Config)this.injector.getInstance((Class)Config.class);
        final Logger log = logger(this);
        this.injector.injectMembers((Object)this);
        if (conf.hasPath("jooby.internal.onStart")) {
            final ClassLoader loader = this.getClass().getClassLoader();
            final Object internalOnStart = loader.loadClass(conf.getString("jooby.internal.onStart")).newInstance();
            this.onStart.add((Throwing.Consumer<Registry>)internalOnStart);
        }
        for (final Throwing.Consumer<Registry> onStart : this.onStart) {
            onStart.accept((Object)this);
        }
        final Set<Route.Definition> routeDefs = (Set<Route.Definition>)this.injector.getInstance((Key)Route.KEY);
        final Set<WebSocket.Definition> sockets = (Set<WebSocket.Definition>)this.injector.getInstance((Key)WebSocket.KEY);
        if (this.mapper != null) {
            routeDefs.forEach(it -> it.map((Route.Mapper<?>)this.mapper));
        }
        final AppPrinter printer = new AppPrinter(routeDefs, sockets, conf);
        printer.printConf(log, conf);
        final Server server = (Server)this.injector.getInstance((Class)Server.class);
        final String serverName = server.getClass().getSimpleName().replace("Server", "").toLowerCase();
        server.start();
        final long end = System.currentTimeMillis();
        log.info("[{}@{}]: Server started in {}ms\n\n{}\n", new Object[] { conf.getString("application.env"), serverName, end - start, printer });
        for (final Throwing.Consumer<Registry> onStarted : this.onStarted) {
            onStarted.accept((Object)this);
        }
        final boolean join = !conf.hasPath("server.join") || conf.getBoolean("server.join");
        if (join) {
            server.join();
        }
    }
    
    @Override
    public Jooby map(final Route.Mapper<?> mapper) {
        Objects.requireNonNull(mapper, "Mapper is required.");
        if (this.mappers.add(mapper.name())) {
            this.mapper = Optional.ofNullable(this.mapper).map(next -> Route.Mapper.chain((Route.Mapper<Object>)mapper, next)).orElse(mapper);
        }
        return this;
    }
    
    public Jooby injector(final BiFunction<Stage, com.google.inject.Module, Injector> injectorFactory) {
        this.injectorFactory = injectorFactory;
        return this;
    }
    
    public <T> Jooby bind(final Class<T> type, final Class<? extends T> implementation) {
        this.use((env, conf, binder) -> binder.bind((Class)type).to((Class)implementation));
        return this;
    }
    
    public <T> Jooby bind(final Class<T> type, final Supplier<T> implementation) {
        this.use((env, conf, binder) -> binder.bind((Class)type).toInstance((Object)implementation.get()));
        return this;
    }
    
    public <T> Jooby bind(final Class<T> type) {
        this.use((env, conf, binder) -> binder.bind((Class)type));
        return this;
    }
    
    public Jooby bind(final Object service) {
        final Class type;
        this.use((env, conf, binder) -> {
            type = service.getClass();
            binder.bind(type).toInstance(service);
            return;
        });
        return this;
    }
    
    public <T> Jooby bind(final Class<T> type, final Function<Config, ? extends T> provider) {
        final Object service;
        this.use((env, conf, binder) -> {
            service = provider.apply(conf);
            binder.bind((Class)type).toInstance(service);
            return;
        });
        return this;
    }
    
    public <T> Jooby bind(final Function<Config, T> provider) {
        final Object service;
        final Class type;
        this.use((env, conf, binder) -> {
            service = provider.apply(conf);
            type = service.getClass();
            binder.bind(type).toInstance(service);
            return;
        });
        return this;
    }
    
    public Jooby dateFormat(final String dateFormat) {
        this.dateFormat = Objects.requireNonNull(dateFormat, "DateFormat required.");
        return this;
    }
    
    public Jooby numberFormat(final String numberFormat) {
        this.numberFormat = Objects.requireNonNull(numberFormat, "NumberFormat required.");
        return this;
    }
    
    public Jooby charset(final Charset charset) {
        this.charset = Objects.requireNonNull(charset, "Charset required.");
        return this;
    }
    
    public Jooby lang(final String... languages) {
        this.languages = languages;
        return this;
    }
    
    public Jooby timezone(final ZoneId zoneId) {
        this.zoneId = Objects.requireNonNull(zoneId, "ZoneId required.");
        return this;
    }
    
    public Jooby port(final int port) {
        this.port = port;
        return this;
    }
    
    public Jooby securePort(final int port) {
        this.securePort = port;
        return this;
    }
    
    public Jooby http2() {
        this.http2 = true;
        return this;
    }
    
    public Jooby executor(final ExecutorService executor) {
        this.executor((Executor)executor);
        this.onStop((Throwing.Consumer<Registry>)(r -> executor.shutdown()));
        return this;
    }
    
    public Jooby executor(final Executor executor) {
        this.defaultExecSet = true;
        this.executors.add(binder -> {
            binder.bind(Key.get((Class)String.class, (Annotation)Names.named("deferred"))).toInstance((Object)"deferred");
            binder.bind(Key.get((Class)Executor.class, (Annotation)Names.named("deferred"))).toInstance((Object)executor);
            return;
        });
        return this;
    }
    
    public Jooby executor(final String name, final ExecutorService executor) {
        this.executor(name, (Executor)executor);
        this.onStop((Throwing.Consumer<Registry>)(r -> executor.shutdown()));
        return this;
    }
    
    public Jooby executor(final String name, final Executor executor) {
        this.executors.add(binder -> binder.bind(Key.get((Class)Executor.class, (Annotation)Names.named(name))).toInstance((Object)executor));
        return this;
    }
    
    public Jooby executor(final String name) {
        this.defaultExecSet = true;
        this.executors.add(binder -> binder.bind(Key.get((Class)String.class, (Annotation)Names.named("deferred"))).toInstance((Object)name));
        return this;
    }
    
    private Jooby executor(final String name, final Class<? extends Provider<Executor>> provider) {
        this.executors.add(binder -> binder.bind(Key.get((Class)Executor.class, (Annotation)Names.named(name))).toProvider((Class)provider).in((Class)Singleton.class));
        return this;
    }
    
    public Jooby throwBootstrapException() {
        this.throwBootstrapException = true;
        return this;
    }
    
    public Jooby caseSensitiveRouting(final boolean enabled) {
        this.caseSensitiveRouting = enabled;
        return this;
    }
    
    private static List<Object> normalize(final List<Object> services, final Env env, final RouteMetadata classInfo, final boolean caseSensitiveRouting) {
        final List<Object> result = new ArrayList<Object>();
        final List<Object> snapshot = services;
        final List<MvcClass> list;
        MvcClass mvcRoute;
        Class<?> mvcClass;
        String path;
        snapshot.forEach(candidate -> {
            if (candidate instanceof Route.Definition) {
                list.add(candidate);
            }
            else if (candidate instanceof MvcClass) {
                mvcRoute = candidate;
                mvcClass = mvcRoute.routeClass;
                path = candidate.path;
                MvcRoutes.routes(env, classInfo, path, caseSensitiveRouting, mvcClass).forEach(route -> list.add((MvcClass)mvcRoute.apply(route)));
            }
            else {
                list.add(candidate);
            }
            return;
        });
        return result;
    }
    
    private static List<Object> processEnvDep(final Set<Object> src, final Env env) {
        final List<Object> result = new ArrayList<Object>();
        final List<Object> bag = new ArrayList<Object>(src);
        EnvDep envdep;
        int from;
        int to;
        final List<EnvDep> list;
        bag.forEach(it -> {
            if (it instanceof EnvDep) {
                envdep = it;
                if (envdep.predicate.test(env.name())) {
                    from = src.size();
                    envdep.callback.accept(env.config());
                    to = src.size();
                    list.addAll(new ArrayList(src).subList(from, to));
                }
            }
            else {
                list.add(it);
            }
            return;
        });
        return result;
    }
    
    private Injector bootstrap(final Config args, final Consumer<List<Route.Definition>> rcallback) throws Throwable {
        final Config initconf = Optional.ofNullable(this.srcconf).orElseGet(() -> ConfigFactory.parseResources("application.conf"));
        final List<Config> modconf = modconf(this.bag);
        final Config conf = this.buildConfig(initconf, args, modconf);
        final List<Locale> locales = LocaleUtils.parse(conf.getString("application.lang"));
        final Env env = this.env.build(conf, this, locales.get(0));
        final String envname = env.name();
        final Charset charset = Charset.forName(conf.getString("application.charset"));
        final String dateFormat = conf.getString("application.dateFormat");
        final ZoneId zoneId = ZoneId.of(conf.getString("application.tz"));
        final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateFormat, locales.get(0)).withZone(zoneId);
        final DateTimeFormatter zonedDateTimeFormat = DateTimeFormatter.ofPattern(conf.getString("application.zonedDateTimeFormat"));
        final DecimalFormat numberFormat = new DecimalFormat(conf.getString("application.numberFormat"));
        final Stage stage = "dev".equals(envname) ? Stage.DEVELOPMENT : Stage.PRODUCTION;
        final RouteMetadata rm = new RouteMetadata(env);
        final List<Object> realbag = processEnvDep(this.bag, env);
        final List<Config> realmodconf = modconf(realbag);
        final List<Object> bag = normalize(realbag, env, rm, this.caseSensitiveRouting);
        if (rcallback != null) {
            final List<Route.Definition> routes = bag.stream().filter(it -> it instanceof Route.Definition).map(it -> it).collect((Collector<? super Object, ?, List<Route.Definition>>)Collectors.toList());
            rcallback.accept(routes);
        }
        Config finalConfig;
        Env finalEnv;
        if (modconf.size() != realmodconf.size()) {
            finalConfig = this.buildConfig(initconf, args, realmodconf);
            finalEnv = this.env.build(finalConfig, this, locales.get(0));
        }
        else {
            finalConfig = conf;
            finalEnv = env;
        }
        final boolean cookieSession = this.session.store() == null;
        if (cookieSession && !finalConfig.hasPath("application.secret")) {
            throw new IllegalStateException("Required property 'application.secret' is missing");
        }
        if (!this.defaultExecSet) {
            this.executor(MoreExecutors.directExecutor());
        }
        this.executor("direct", MoreExecutors.directExecutor());
        this.executor("server", (Class<? extends Provider<Executor>>)ServerExecutorProvider.class);
        this.xss(finalEnv);
        final com.google.inject.Module joobyModule = binder -> {
            new TypeConverters().configure(binder);
            this.bindConfig(binder, finalConfig);
            binder.bind((Class)Env.class).toInstance((Object)finalEnv);
            binder.bind((Class)Charset.class).toInstance((Object)charset);
            binder.bind((Class)Locale.class).toInstance(locales.get(0));
            final TypeLiteral<List<Locale>> localeType = (TypeLiteral<List<Locale>>)TypeLiteral.get((Type)Types.listOf((Type)Locale.class));
            binder.bind((TypeLiteral)localeType).toInstance((Object)locales);
            binder.bind((Class)ZoneId.class).toInstance((Object)zoneId);
            binder.bind((Class)TimeZone.class).toInstance((Object)TimeZone.getTimeZone(zoneId));
            binder.bind((Class)DateTimeFormatter.class).toInstance((Object)dateTimeFormatter);
            binder.bind((Class)NumberFormat.class).toInstance((Object)numberFormat);
            binder.bind((Class)DecimalFormat.class).toInstance((Object)numberFormat);
            binder.bind((Class)SSLContext.class).toProvider((Class)SslContextProvider.class);
            final Multibinder<Route.Definition> definitions = (Multibinder<Route.Definition>)Multibinder.newSetBinder(binder, (Class)Route.Definition.class);
            final Multibinder<WebSocket.Definition> sockets = (Multibinder<WebSocket.Definition>)Multibinder.newSetBinder(binder, (Class)WebSocket.Definition.class);
            final File tmpdir = new File(finalConfig.getString("application.tmpdir"));
            tmpdir.mkdirs();
            binder.bind((Class)File.class).annotatedWith((Annotation)Names.named("application.tmpdir")).toInstance((Object)tmpdir);
            binder.bind((Class)ParameterNameProvider.class).toInstance((Object)rm);
            final Multibinder<Err.Handler> ehandlers = (Multibinder<Err.Handler>)Multibinder.newSetBinder(binder, (Class)Err.Handler.class);
            final Multibinder<Parser> parsers = (Multibinder<Parser>)Multibinder.newSetBinder(binder, (Class)Parser.class);
            final Multibinder<Renderer> renderers = (Multibinder<Renderer>)Multibinder.newSetBinder(binder, (Class)Renderer.class);
            parsers.addBinding().toInstance((Object)BuiltinParser.Basic);
            parsers.addBinding().toInstance((Object)BuiltinParser.Collection);
            parsers.addBinding().toInstance((Object)BuiltinParser.Optional);
            parsers.addBinding().toInstance((Object)BuiltinParser.Enum);
            parsers.addBinding().toInstance((Object)BuiltinParser.Bytes);
            renderers.addBinding().toInstance((Object)BuiltinRenderer.asset);
            renderers.addBinding().toInstance((Object)BuiltinRenderer.bytes);
            renderers.addBinding().toInstance((Object)BuiltinRenderer.byteBuffer);
            renderers.addBinding().toInstance((Object)BuiltinRenderer.file);
            renderers.addBinding().toInstance((Object)BuiltinRenderer.charBuffer);
            renderers.addBinding().toInstance((Object)BuiltinRenderer.stream);
            renderers.addBinding().toInstance((Object)BuiltinRenderer.reader);
            renderers.addBinding().toInstance((Object)BuiltinRenderer.fileChannel);
            final Set<Object> routeClasses = new HashSet<Object>();
            for (final Object it2 : bag) {
                Try.run(() -> bindService(logger(this), this.bag, finalConfig, finalEnv, rm, binder, definitions, sockets, ehandlers, parsers, renderers, routeClasses, this.caseSensitiveRouting).accept(it2)).throwException();
            }
            parsers.addBinding().toInstance((Object)new DateParser(dateFormat));
            parsers.addBinding().toInstance((Object)new LocalDateParser(dateTimeFormatter));
            parsers.addBinding().toInstance((Object)new ZonedDateTimeParser(zonedDateTimeFormat));
            parsers.addBinding().toInstance((Object)new LocaleParser());
            parsers.addBinding().toInstance((Object)new StaticMethodParser("valueOf"));
            parsers.addBinding().toInstance((Object)new StaticMethodParser("fromString"));
            parsers.addBinding().toInstance((Object)new StaticMethodParser("forName"));
            parsers.addBinding().toInstance((Object)new StringConstructorParser());
            parsers.addBinding().toInstance((Object)this.beanParser.orElseGet(() -> new BeanParser(false)));
            binder.bind((Class)ParserExecutor.class).in((Class)Singleton.class);
            renderers.addBinding().toInstance((Object)new DefaulErrRenderer());
            renderers.addBinding().toInstance((Object)BuiltinRenderer.text);
            binder.bind((Class)HttpHandler.class).to((Class)HttpHandlerImpl.class).in((Class)Singleton.class);
            final RequestScope requestScope = new RequestScope();
            binder.bind((Class)RequestScope.class).toInstance((Object)requestScope);
            binder.bindScope((Class)RequestScoped.class, (Scope)requestScope);
            binder.bind((Class)Session.Definition.class).toProvider((Provider)session(finalConfig.getConfig("session"), this.session)).asEagerSingleton();
            final Object sstore = this.session.store();
            if (cookieSession) {
                binder.bind((Class)SessionManager.class).to((Class)CookieSessionManager.class).asEagerSingleton();
            }
            else {
                binder.bind((Class)SessionManager.class).to((Class)ServerSessionManager.class).asEagerSingleton();
                if (sstore instanceof Class) {
                    binder.bind((Class)Session.Store.class).to((Class)sstore).asEagerSingleton();
                }
                else {
                    binder.bind((Class)Session.Store.class).toInstance((Object)sstore);
                }
            }
            binder.bind((Class)Request.class).toProvider((javax.inject.Provider)Providers.outOfScope(Request.class)).in((Class)RequestScoped.class);
            binder.bind((Class)Route.Chain.class).toProvider((javax.inject.Provider)Providers.outOfScope(Route.Chain.class)).in((Class)RequestScoped.class);
            binder.bind((Class)Response.class).toProvider((javax.inject.Provider)Providers.outOfScope(Response.class)).in((Class)RequestScoped.class);
            binder.bind((Class)Sse.class).toProvider((javax.inject.Provider)Providers.outOfScope(Sse.class)).in((Class)RequestScoped.class);
            binder.bind((Class)Session.class).toProvider((javax.inject.Provider)Providers.outOfScope(Session.class)).in((Class)RequestScoped.class);
            ehandlers.addBinding().toInstance((Object)new Err.DefHandler());
            this.executors.forEach(it -> it.accept(binder));
        };
        final Injector injector = this.injectorFactory.apply(stage, joobyModule);
        if (this.apprefs != null) {
            this.apprefs.forEach(app -> app.injector = injector);
            this.apprefs.clear();
            this.apprefs = null;
        }
        this.onStart.addAll(0, finalEnv.startTasks());
        this.onStarted.addAll(0, finalEnv.startedTasks());
        this.onStop.addAll(finalEnv.stopTasks());
        this.bag.clear();
        this.bag = (Set<Object>)ImmutableSet.of();
        this.executors.clear();
        this.executors = (List<Consumer<Binder>>)ImmutableList.of();
        return injector;
    }
    
    private void xss(final Env env) {
        final Escaper ufe = UrlEscapers.urlFragmentEscaper();
        final Escaper fpe = UrlEscapers.urlFormParameterEscaper();
        final Escaper pse = UrlEscapers.urlPathSegmentEscaper();
        final Escaper html = HtmlEscapers.htmlEscaper();
        env.xss("urlFragment", ufe::escape).xss("formParam", fpe::escape).xss("pathSegment", pse::escape).xss("html", html::escape);
    }
    
    private static Provider<Session.Definition> session(final Config $session, final Session.Definition session) {
        return (Provider<Session.Definition>)(() -> {
            session.saveInterval(session.saveInterval().orElse($session.getDuration("saveInterval", TimeUnit.MILLISECONDS)));
            final Cookie.Definition source = session.cookie();
            source.name(source.name().orElse($session.getString("cookie.name")));
            if (!source.comment().isPresent() && $session.hasPath("cookie.comment")) {
                source.comment($session.getString("cookie.comment"));
            }
            if (!source.domain().isPresent() && $session.hasPath("cookie.domain")) {
                source.domain($session.getString("cookie.domain"));
            }
            source.httpOnly(source.httpOnly().orElse($session.getBoolean("cookie.httpOnly")));
            Object maxAge = $session.getAnyRef("cookie.maxAge");
            if (maxAge instanceof String) {
                maxAge = $session.getDuration("cookie.maxAge", TimeUnit.SECONDS);
            }
            source.maxAge(source.maxAge().orElse(((Number)maxAge).intValue()));
            source.path(source.path().orElse($session.getString("cookie.path")));
            source.secure(source.secure().orElse($session.getBoolean("cookie.secure")));
            return session;
        });
    }
    
    private static Throwing.Consumer<? super Object> bindService(final Logger log, final Set<Object> src, final Config conf, final Env env, final RouteMetadata rm, final Binder binder, final Multibinder<Route.Definition> definitions, final Multibinder<WebSocket.Definition> sockets, final Multibinder<Err.Handler> ehandlers, final Multibinder<Parser> parsers, final Multibinder<Renderer> renderers, final Set<Object> routeClasses, final boolean caseSensitiveRouting) {
        return (Throwing.Consumer<? super Object>)(it -> {
            if (it instanceof Module) {
                final int from = src.size();
                install(log, (Module)it, env, conf, binder);
                final int to = src.size();
                if (to > from) {
                    final List<Object> elements = normalize(new ArrayList<Object>(src).subList(from, to), env, rm, caseSensitiveRouting);
                    for (final Object e : elements) {
                        bindService(log, src, conf, env, rm, binder, definitions, sockets, ehandlers, parsers, renderers, routeClasses, caseSensitiveRouting).accept(e);
                    }
                }
            }
            else if (it instanceof Route.Definition) {
                final Route.Definition rdef = (Route.Definition)it;
                final Route.Filter h = rdef.filter();
                if (h instanceof Route.MethodHandler) {
                    final Class<?> routeClass = ((Route.MethodHandler)h).implementingClass();
                    if (routeClasses.add(routeClass)) {
                        binder.bind((Class)routeClass);
                    }
                    definitions.addBinding().toInstance((Object)rdef);
                }
                else {
                    definitions.addBinding().toInstance((Object)rdef);
                }
            }
            else if (it instanceof WebSocket.Definition) {
                sockets.addBinding().toInstance((Object)it);
            }
            else if (it instanceof Parser) {
                parsers.addBinding().toInstance((Object)it);
            }
            else if (it instanceof Renderer) {
                renderers.addBinding().toInstance((Object)it);
            }
            else {
                ehandlers.addBinding().toInstance((Object)it);
            }
        });
    }
    
    private static List<Config> modconf(final Collection<Object> bag) {
        return bag.stream().filter(it -> it instanceof Module).map(it -> it.config()).filter(c -> !c.isEmpty()).collect((Collector<? super Object, ?, List<Config>>)Collectors.toList());
    }
    
    public boolean isStarted() {
        return this.started.get();
    }
    
    public void stop() {
        if (this.started.compareAndSet(true, false)) {
            final Logger log = logger(this);
            fireStop(this, log, this.onStop);
            if (this.injector != null) {
                try {
                    ((Server)this.injector.getInstance((Class)Server.class)).stop();
                }
                catch (Throwable ex) {
                    log.debug("server.stop() resulted in exception", ex);
                }
            }
            this.injector = null;
            log.info("Stopped");
        }
    }
    
    private static void fireStop(final Jooby app, final Logger log, final List<Throwing.Consumer<Registry>> onStop) {
        onStop.forEach(c -> Try.run(() -> c.accept((Object)app)).onFailure(x -> log.error("shutdown of {} resulted in error", (Object)c, (Object)x)));
    }
    
    private Config buildConfig(final Config source, final Config args, final List<Config> modules) {
        Config system = ConfigFactory.systemProperties();
        final Config tmpdir = source.hasPath("java.io.tmpdir") ? source : system;
        system = system.withValue("file.encoding", ConfigValueFactory.fromAnyRef((Object)System.getProperty("file.encoding"))).withValue("java.io.tmpdir", ConfigValueFactory.fromAnyRef((Object)Paths.get(tmpdir.getString("java.io.tmpdir"), new String[0]).normalize().toString()));
        Config moduleStack = ConfigFactory.empty();
        for (final Config module : ImmutableList.copyOf((Collection)modules).reverse()) {
            moduleStack = moduleStack.withFallback((ConfigMergeable)module);
        }
        final String env = Arrays.asList(system, args, source).stream().filter(it -> it.hasPath("application.env")).findFirst().map(c -> c.getString("application.env")).orElse("dev");
        final String cpath = Arrays.asList(system, args, source).stream().filter(it -> it.hasPath("application.path")).findFirst().map(c -> c.getString("application.path")).orElse("/");
        final Config envconf = this.envConf(source, env);
        final Config conf = envconf.withFallback((ConfigMergeable)source);
        return system.withFallback((ConfigMergeable)args).withFallback((ConfigMergeable)conf).withFallback((ConfigMergeable)moduleStack).withFallback((ConfigMergeable)MediaType.types).withFallback((ConfigMergeable)this.defaultConfig(conf, Route.normalize(cpath))).resolve();
    }
    
    static Config args(final String[] args) {
        if (args == null || args.length == 0) {
            return ConfigFactory.empty();
        }
        final Map<String, String> conf = new HashMap<String, String>();
        for (final String arg : args) {
            final String[] values = arg.split("=");
            String name;
            String value;
            if (values.length == 2) {
                name = values[0];
                value = values[1];
            }
            else {
                name = "application.env";
                value = values[0];
            }
            if (name.indexOf(".") == -1) {
                conf.put("application." + name, value);
            }
            conf.put(name, value);
        }
        return ConfigFactory.parseMap((Map)conf, "args");
    }
    
    private Config envConf(final Config source, final String env) {
        String name = Optional.ofNullable(this.confname).orElse(source.origin().resource());
        final Config result = ConfigFactory.empty();
        if (name != null) {
            final int dot = name.lastIndexOf(46);
            name = name.substring(0, dot);
        }
        else {
            name = "application";
        }
        final String envconfname = name + "." + env + ".conf";
        final Config envconf = fileConfig(envconfname);
        final Config appconf = fileConfig(name + ".conf");
        return result.withFallback((ConfigMergeable)envconf).withFallback((ConfigMergeable)appconf).withFallback((ConfigMergeable)ConfigFactory.parseResources(envconfname));
    }
    
    static Config fileConfig(final String fname) {
        final File dir = new File(System.getProperty("user.dir"));
        final File froot = new File(dir, fname);
        if (froot.exists()) {
            return ConfigFactory.parseFile(froot);
        }
        final File fconfig = new File(new File(dir, "conf"), fname);
        if (fconfig.exists()) {
            return ConfigFactory.parseFile(fconfig);
        }
        return ConfigFactory.empty();
    }
    
    private Config defaultConfig(final Config conf, final String cpath) {
        final String ns = Optional.ofNullable(this.getClass().getPackage()).map((Function<? super Package, ? extends String>)Package::getName).orElse("default." + this.getClass().getName());
        final String[] parts = ns.split("\\.");
        final String appname = parts[parts.length - 1];
        List<Locale> locales;
        if (!conf.hasPath("application.lang")) {
            locales = Optional.ofNullable(this.languages).map(langs -> LocaleUtils.parse(Joiner.on(",").join((Object[])langs))).orElse((List<Locale>)ImmutableList.of((Object)Locale.getDefault()));
        }
        else {
            locales = LocaleUtils.parse(conf.getString("application.lang"));
        }
        final Locale locale = locales.iterator().next();
        final String lang = locale.toLanguageTag();
        String tz;
        if (!conf.hasPath("application.tz")) {
            tz = Optional.ofNullable(this.zoneId).orElse(ZoneId.systemDefault()).getId();
        }
        else {
            tz = conf.getString("application.tz");
        }
        String nf;
        if (!conf.hasPath("application.numberFormat")) {
            nf = Optional.ofNullable(this.numberFormat).orElseGet(() -> ((DecimalFormat)NumberFormat.getInstance(locale)).toPattern());
        }
        else {
            nf = conf.getString("application.numberFormat");
        }
        final int processors = Runtime.getRuntime().availableProcessors();
        final String version = Optional.ofNullable(this.getClass().getPackage()).map((Function<? super Package, ? extends String>)Package::getImplementationVersion).filter(Objects::nonNull).orElse("0.0.0");
        Config defs = ConfigFactory.parseResources((Class)Jooby.class, "jooby.conf").withValue("contextPath", ConfigValueFactory.fromAnyRef((Object)(cpath.equals("/") ? "" : cpath))).withValue("application.name", ConfigValueFactory.fromAnyRef((Object)appname)).withValue("application.version", ConfigValueFactory.fromAnyRef((Object)version)).withValue("application.class", ConfigValueFactory.fromAnyRef((Object)this.classname)).withValue("application.ns", ConfigValueFactory.fromAnyRef((Object)ns)).withValue("application.lang", ConfigValueFactory.fromAnyRef((Object)lang)).withValue("application.tz", ConfigValueFactory.fromAnyRef((Object)tz)).withValue("application.numberFormat", ConfigValueFactory.fromAnyRef((Object)nf)).withValue("server.http2.enabled", ConfigValueFactory.fromAnyRef((Object)this.http2)).withValue("runtime.processors", ConfigValueFactory.fromAnyRef((Object)processors)).withValue("runtime.processors-plus1", ConfigValueFactory.fromAnyRef((Object)(processors + 1))).withValue("runtime.processors-plus2", ConfigValueFactory.fromAnyRef((Object)(processors + 2))).withValue("runtime.processors-x2", ConfigValueFactory.fromAnyRef((Object)(processors * 2))).withValue("runtime.processors-x4", ConfigValueFactory.fromAnyRef((Object)(processors * 4))).withValue("runtime.processors-x8", ConfigValueFactory.fromAnyRef((Object)(processors * 8))).withValue("runtime.concurrencyLevel", ConfigValueFactory.fromAnyRef((Object)Math.max(4, processors))).withValue("server.threads.Min", ConfigValueFactory.fromAnyRef((Object)Math.max(4, processors))).withValue("server.threads.Max", ConfigValueFactory.fromAnyRef((Object)Math.max(32, processors * 8)));
        if (this.charset != null) {
            defs = defs.withValue("application.charset", ConfigValueFactory.fromAnyRef((Object)this.charset.name()));
        }
        if (this.port != null) {
            defs = defs.withValue("application.port", ConfigValueFactory.fromAnyRef((Object)this.port));
        }
        if (this.securePort != null) {
            defs = defs.withValue("application.securePort", ConfigValueFactory.fromAnyRef((Object)this.securePort));
        }
        if (this.dateFormat != null) {
            defs = defs.withValue("application.dateFormat", ConfigValueFactory.fromAnyRef((Object)this.dateFormat));
        }
        return defs;
    }
    
    private static void install(final Logger log, final Module module, final Env env, final Config config, final Binder binder) throws Throwable {
        module.configure(env, config, binder);
        try {
            binder.install(ProviderMethodsModule.forObject((Object)module));
        }
        catch (NoClassDefFoundError x) {
            log.debug("ignoring class not found from guice provider method", (Throwable)x);
        }
    }
    
    private void bindConfig(final Binder binder, final Config config) {
        traverse(binder, "", config.root());
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
    
    private static void traverse(final Binder binder, final String p, final ConfigObject root) {
        ConfigObject child;
        String path;
        Named named;
        root.forEach((n, v) -> {
            if (v instanceof ConfigObject) {
                child = v;
                path = p + n;
                named = Names.named(path);
                binder.bind((Class)Config.class).annotatedWith((Annotation)named).toInstance((Object)child.toConfig());
                traverse(binder, path + ".", child);
            }
        });
    }
    
    private static Predicate<String> envpredicate(final String candidate) {
        return env -> env.equalsIgnoreCase(candidate) || candidate.equals("*");
    }
    
    static String logback(final Config conf) {
        String logback;
        if (conf.hasPath("logback.configurationFile")) {
            logback = conf.getString("logback.configurationFile");
        }
        else {
            final String env = conf.hasPath("application.env") ? conf.getString("application.env") : null;
            final ImmutableList.Builder<File> files = (ImmutableList.Builder<File>)ImmutableList.builder();
            final File userdir = new File(System.getProperty("user.dir"));
            final File confdir = new File(userdir, "conf");
            if (env != null) {
                files.add((Object)new File(userdir, "logback." + env + ".xml"));
                files.add((Object)new File(confdir, "logback." + env + ".xml"));
            }
            files.add((Object)new File(userdir, "logback.xml"));
            files.add((Object)new File(confdir, "logback.xml"));
            logback = files.build().stream().filter(File::exists).map((Function<? super Object, ? extends String>)File::getAbsolutePath).findFirst().orElseGet(() -> Optional.ofNullable(Jooby.class.getResource("/logback." + env + ".xml")).map((Function<? super URL, ? extends String>)Objects::toString).orElse("logback.xml"));
        }
        return logback;
    }
    
    private static Logger logger(final Jooby app) {
        return LoggerFactory.getLogger((Class)app.getClass());
    }
    
    private Route.AssetDefinition configureAssetHandler(final Route.AssetDefinition handler) {
        this.onStart((Throwing.Consumer<Registry>)(r -> {
            final Config conf = r.require(Config.class);
            handler.cdn(conf.getString("assets.cdn")).lastModified(conf.getBoolean("assets.lastModified")).etag(conf.getBoolean("assets.etag")).maxAge(conf.getString("assets.cache.maxAge"));
        }));
        return handler;
    }
    
    private String classname(final String name) {
        if (name.equals(Jooby.class.getName()) || name.equals("org.jooby.Kooby")) {
            return SourceProvider.INSTANCE.get().map((Function<? super StackTraceElement, ? extends String>)StackTraceElement::getClassName).orElse(name);
        }
        return name;
    }
    
    static {
        final String pid = System.getProperty("pid", JvmInfo.pid() + "");
        System.setProperty("pid", pid);
    }
    
    public interface EnvPredicate
    {
        default void orElse(final Runnable callback) {
            this.orElse(conf -> callback.run());
        }
        
        void orElse(final Consumer<Config> callback);
    }
    
    public interface Module
    {
        @Nonnull
        default Config config() {
            return ConfigFactory.empty();
        }
        
        void configure(final Env env, final Config conf, final Binder binder) throws Throwable;
    }
    
    static class MvcClass implements Route.Props<MvcClass>
    {
        Class<?> routeClass;
        String path;
        ImmutableMap.Builder<String, Object> attrs;
        private List<MediaType> consumes;
        private String name;
        private List<MediaType> produces;
        private List<String> excludes;
        private Route.Mapper<?> mapper;
        private String prefix;
        private String renderer;
        
        public MvcClass(final Class<?> routeClass, final String path, final String prefix) {
            this.attrs = (ImmutableMap.Builder<String, Object>)ImmutableMap.builder();
            this.routeClass = routeClass;
            this.path = path;
            this.prefix = prefix;
        }
        
        @Override
        public MvcClass attr(final String name, final Object value) {
            this.attrs.put((Object)name, value);
            return this;
        }
        
        @Override
        public MvcClass name(final String name) {
            this.name = name;
            return this;
        }
        
        @Override
        public MvcClass consumes(final List<MediaType> consumes) {
            this.consumes = consumes;
            return this;
        }
        
        @Override
        public MvcClass produces(final List<MediaType> produces) {
            this.produces = produces;
            return this;
        }
        
        @Override
        public MvcClass excludes(final List<String> excludes) {
            this.excludes = excludes;
            return this;
        }
        
        @Override
        public MvcClass map(final Route.Mapper<?> mapper) {
            this.mapper = mapper;
            return this;
        }
        
        @Override
        public String renderer() {
            return this.renderer;
        }
        
        @Override
        public MvcClass renderer(final String name) {
            this.renderer = name;
            return this;
        }
        
        public Route.Definition apply(final Route.Definition route) {
            this.attrs.build().forEach((BiConsumer)route::attr);
            if (this.name != null) {
                route.name(this.name);
            }
            if (this.prefix != null) {
                route.name(this.prefix + "/" + route.name());
            }
            if (this.consumes != null) {
                route.consumes(this.consumes);
            }
            if (this.produces != null) {
                route.produces(this.produces);
            }
            if (this.excludes != null) {
                route.excludes(this.excludes);
            }
            if (this.mapper != null) {
                route.map(this.mapper);
            }
            if (this.renderer != null) {
                route.renderer(this.renderer);
            }
            return route;
        }
    }
    
    private static class EnvDep
    {
        Predicate<String> predicate;
        Consumer<Config> callback;
        
        public EnvDep(final Predicate<String> predicate, final Consumer<Config> callback) {
            this.predicate = predicate;
            this.callback = callback;
        }
    }
}
