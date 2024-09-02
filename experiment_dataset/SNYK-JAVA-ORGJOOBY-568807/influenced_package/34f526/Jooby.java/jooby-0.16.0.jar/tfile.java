// 
// Decompiled by Procyon v0.5.36
// 

package org.jooby;

import org.jooby.internal.JvmInfo;
import java.util.function.Function;
import javax.inject.Provider;
import org.jooby.scope.Providers;
import org.jooby.internal.SessionManager;
import com.google.inject.Scope;
import org.jooby.scope.RequestScoped;
import org.jooby.internal.RequestScope;
import org.jooby.internal.HttpHandlerImpl;
import org.jooby.spi.HttpHandler;
import org.jooby.internal.DefaulErrRenderer;
import javax.inject.Singleton;
import org.jooby.internal.parser.ParserExecutor;
import org.jooby.internal.parser.StringConstructorParser;
import org.jooby.internal.parser.StaticMethodParser;
import org.jooby.internal.parser.BeanParser;
import org.jooby.internal.parser.LocaleParser;
import org.jooby.internal.parser.LocalDateParser;
import org.jooby.internal.parser.DateParser;
import java.util.HashSet;
import org.jooby.internal.BuiltinRenderer;
import org.jooby.internal.BuiltinParser;
import org.jooby.internal.ParameterNameProvider;
import com.google.inject.spi.TypeListener;
import com.google.inject.matcher.Matchers;
import org.jooby.internal.ssl.SslContextProvider;
import javax.net.ssl.SSLContext;
import java.util.TimeZone;
import com.google.inject.TypeLiteral;
import org.jooby.internal.TypeConverters;
import com.typesafe.config.ConfigObject;
import com.google.inject.name.Named;
import java.util.Iterator;
import java.lang.annotation.Annotation;
import com.google.inject.Key;
import java.lang.reflect.Type;
import com.google.inject.util.Types;
import com.google.inject.name.Names;
import com.typesafe.config.ConfigValue;
import java.text.NumberFormat;
import java.util.Map;
import java.util.HashMap;
import com.google.common.collect.UnmodifiableIterator;
import java.util.Arrays;
import com.google.common.collect.ImmutableList;
import java.nio.file.Paths;
import com.typesafe.config.ConfigValueFactory;
import org.jooby.internal.LifecycleProcessor;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.Binder;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Guice;
import com.google.inject.Module;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import com.google.inject.Stage;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.nio.charset.Charset;
import java.util.Locale;
import org.jooby.internal.LocaleUtils;
import java.util.Collection;
import org.jooby.internal.mvc.MvcRoutes;
import org.jooby.internal.RouteMetadata;
import com.google.common.base.Strings;
import java.io.File;
import org.jooby.internal.js.JsJooby;
import org.slf4j.Logger;
import org.jooby.internal.AppPrinter;
import org.jooby.spi.Server;
import org.slf4j.LoggerFactory;
import javaslang.control.Try;
import com.typesafe.config.ConfigMergeable;
import com.typesafe.config.ConfigFactory;
import java.util.function.Supplier;
import org.jooby.internal.AssetProxy;
import org.jooby.handlers.AssetHandler;
import org.jooby.internal.handlers.TraceHandler;
import org.jooby.internal.handlers.OptionsHandler;
import org.jooby.internal.handlers.HeadHandler;
import com.google.common.base.Preconditions;
import java.util.function.Predicate;
import java.util.function.Consumer;
import java.util.Objects;
import java.util.Optional;
import org.jooby.internal.ServerLookup;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import com.google.inject.Injector;
import com.typesafe.config.Config;
import java.util.Set;

public class Jooby implements Routes
{
    private Set<Object> bag;
    private Config srcconf;
    private Injector injector;
    private Session.Definition session;
    private Env.Builder env;
    private String prefix;
    private List<Runnable> onStart;
    private List<Runnable> onStop;
    
    public Jooby() {
        this(null);
    }
    
    public Jooby(final String prefix) {
        this.bag = new LinkedHashSet<Object>();
        this.session = new Session.Definition(Session.Mem.class);
        this.env = Env.DEFAULT;
        this.onStart = new ArrayList<Runnable>();
        this.onStop = new ArrayList<Runnable>();
        this.prefix = prefix;
        this.use(new ServerLookup());
    }
    
    public Jooby use(final Jooby app) {
        return this.use(Optional.empty(), app);
    }
    
    public Jooby use(final String path, final Jooby app) {
        return this.use(Optional.of(path), app);
    }
    
    private Jooby use(final Optional<String> path, final Jooby app) {
        // 
        // This method could not be decompiled.
        // 
        // Original Bytecode:
        // 
        //     1: ldc             "App is required."
        //     3: invokestatic    java/util/Objects.requireNonNull:(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;
        //     6: pop            
        //     7: aload_1         /* path */
        //     8: invokedynamic   BootstrapMethod #0, apply:(Ljava/util/Optional;)Ljava/util/function/Function;
        //    13: astore_3        /* rewrite */
        //    14: aload_2         /* app */
        //    15: getfield        org/jooby/Jooby.bag:Ljava/util/Set;
        //    18: aload_0         /* this */
        //    19: aload_3         /* rewrite */
        //    20: aload_1         /* path */
        //    21: invokedynamic   BootstrapMethod #1, accept:(Lorg/jooby/Jooby;Ljava/util/function/Function;Ljava/util/Optional;)Ljava/util/function/Consumer;
        //    26: invokeinterface java/util/Set.forEach:(Ljava/util/function/Consumer;)V
        //    31: aload_0         /* this */
        //    32: areturn        
        //    Signature:
        //  (Ljava/util/Optional<Ljava/lang/String;>;Lorg/jooby/Jooby;)Lorg/jooby/Jooby;
        //    MethodParameters:
        //  Name  Flags  
        //  ----  -----
        //  path  FINAL
        //  app   FINAL
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
    
    @Override
    public Route.Group use(final String pattern) {
        final Route.Group group = new Route.Group(pattern, this.prefix);
        this.bag.add(group);
        return group;
    }
    
    public Jooby env(final Env.Builder env) {
        this.env = Objects.requireNonNull(env, "Env builder is required.");
        return this;
    }
    
    public Jooby onStart(final Runnable callback) {
        Objects.requireNonNull(callback, "Callback is required.");
        this.onStart.add(callback);
        return this;
    }
    
    public Jooby onStop(final Runnable callback) {
        Objects.requireNonNull(callback, "Callback is required.");
        this.onStop.add(callback);
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
        this.bag.add(new EnvDep(predicate, callback));
        return this;
    }
    
    public Jooby on(final String env1, final String env2, final String env3, final Runnable callback) {
        this.on(envpredicate(env1).or(envpredicate(env2)).or(envpredicate(env3)), callback);
        return this;
    }
    
    public <T> T require(final Class<T> type) {
        Preconditions.checkState(this.injector != null, (Object)"App didn't start yet");
        return (T)this.injector.getInstance((Class)type);
    }
    
    public Route.OneArgHandler promise(final Deferred.Initializer initializer) {
        return req -> new Deferred(req, initializer);
    }
    
    public Route.OneArgHandler promise(final Deferred.Initializer0 initializer) {
        return req -> new Deferred(initializer);
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
    
    @Override
    public Route.Definition use(final String path, final Route.Filter filter) {
        return this.appendDefinition(new Route.Definition("*", path, filter));
    }
    
    @Override
    public Route.Definition use(final String verb, final String path, final Route.Filter filter) {
        return this.appendDefinition(new Route.Definition(verb, path, filter));
    }
    
    @Override
    public Route.Definition use(final String verb, final String path, final Route.Handler handler) {
        return this.appendDefinition(new Route.Definition(verb, path, handler));
    }
    
    @Override
    public Route.Definition use(final String path, final Route.Handler handler) {
        return this.appendDefinition(new Route.Definition("*", path, handler));
    }
    
    @Override
    public Route.Definition use(final String path, final Route.OneArgHandler handler) {
        return this.appendDefinition(new Route.Definition("*", path, handler));
    }
    
    @Override
    public Route.Definition get(final String path, final Route.Handler handler) {
        return this.appendDefinition(new Route.Definition("GET", path, handler));
    }
    
    @Override
    public Route.Collection get(final String path1, final String path2, final Route.Handler handler) {
        return new Route.Collection(new Route.Definition[] { this.get(path1, handler), this.get(path2, handler) });
    }
    
    @Override
    public Route.Collection get(final String path1, final String path2, final String path3, final Route.Handler handler) {
        return new Route.Collection(new Route.Definition[] { this.get(path1, handler), this.get(path2, handler), this.get(path3, handler) });
    }
    
    @Override
    public Route.Definition get(final String path, final Route.OneArgHandler handler) {
        return this.appendDefinition(new Route.Definition("GET", path, handler));
    }
    
    @Override
    public Route.Collection get(final String path1, final String path2, final Route.OneArgHandler handler) {
        return new Route.Collection(new Route.Definition[] { this.get(path1, handler), this.get(path2, handler) });
    }
    
    @Override
    public Route.Collection get(final String path1, final String path2, final String path3, final Route.OneArgHandler handler) {
        return new Route.Collection(new Route.Definition[] { this.get(path1, handler), this.get(path2, handler), this.get(path3, handler) });
    }
    
    @Override
    public Route.Definition get(final String path, final Route.ZeroArgHandler handler) {
        return this.appendDefinition(new Route.Definition("GET", path, handler));
    }
    
    @Override
    public Route.Collection get(final String path1, final String path2, final Route.ZeroArgHandler handler) {
        return new Route.Collection(new Route.Definition[] { this.get(path1, handler), this.get(path2, handler) });
    }
    
    @Override
    public Route.Collection get(final String path1, final String path2, final String path3, final Route.ZeroArgHandler handler) {
        return new Route.Collection(new Route.Definition[] { this.get(path1, handler), this.get(path2, handler), this.get(path3, handler) });
    }
    
    @Override
    public Route.Definition get(final String path, final Route.Filter filter) {
        return this.appendDefinition(new Route.Definition("GET", path, filter));
    }
    
    @Override
    public Route.Collection get(final String path1, final String path2, final Route.Filter filter) {
        return new Route.Collection(new Route.Definition[] { this.get(path1, filter), this.get(path2, filter) });
    }
    
    @Override
    public Route.Collection get(final String path1, final String path2, final String path3, final Route.Filter filter) {
        return new Route.Collection(new Route.Definition[] { this.get(path1, filter), this.get(path2, filter), this.get(path3, filter) });
    }
    
    @Override
    public Route.Definition post(final String path, final Route.Handler handler) {
        return this.appendDefinition(new Route.Definition("POST", path, handler));
    }
    
    @Override
    public Route.Collection post(final String path1, final String path2, final Route.Handler handler) {
        return new Route.Collection(new Route.Definition[] { this.post(path1, handler), this.post(path2, handler) });
    }
    
    @Override
    public Route.Collection post(final String path1, final String path2, final String path3, final Route.Handler handler) {
        return new Route.Collection(new Route.Definition[] { this.post(path1, handler), this.post(path2, handler), this.post(path3, handler) });
    }
    
    @Override
    public Route.Definition post(final String path, final Route.OneArgHandler handler) {
        return this.appendDefinition(new Route.Definition("POST", path, handler));
    }
    
    @Override
    public Route.Collection post(final String path1, final String path2, final Route.OneArgHandler handler) {
        return new Route.Collection(new Route.Definition[] { this.post(path1, handler), this.post(path2, handler) });
    }
    
    @Override
    public Route.Collection post(final String path1, final String path2, final String path3, final Route.OneArgHandler handler) {
        return new Route.Collection(new Route.Definition[] { this.post(path1, handler), this.post(path2, handler), this.post(path3, handler) });
    }
    
    @Override
    public Route.Definition post(final String path, final Route.ZeroArgHandler handler) {
        return this.appendDefinition(new Route.Definition("POST", path, handler));
    }
    
    @Override
    public Route.Collection post(final String path1, final String path2, final Route.ZeroArgHandler handler) {
        return new Route.Collection(new Route.Definition[] { this.post(path1, handler), this.post(path2, handler) });
    }
    
    @Override
    public Route.Collection post(final String path1, final String path2, final String path3, final Route.ZeroArgHandler handler) {
        return new Route.Collection(new Route.Definition[] { this.post(path1, handler), this.post(path2, handler), this.post(path3, handler) });
    }
    
    @Override
    public Route.Definition post(final String path, final Route.Filter filter) {
        return this.appendDefinition(new Route.Definition("POST", path, filter));
    }
    
    @Override
    public Route.Collection post(final String path1, final String path2, final Route.Filter filter) {
        return new Route.Collection(new Route.Definition[] { this.post(path1, filter), this.post(path2, filter) });
    }
    
    @Override
    public Route.Collection post(final String path1, final String path2, final String path3, final Route.Filter filter) {
        return new Route.Collection(new Route.Definition[] { this.post(path1, filter), this.post(path2, filter), this.post(path3, filter) });
    }
    
    @Override
    public Route.Definition head(final String path, final Route.Handler handler) {
        return this.appendDefinition(new Route.Definition("HEAD", path, handler));
    }
    
    @Override
    public Route.Definition head(final String path, final Route.OneArgHandler handler) {
        return this.appendDefinition(new Route.Definition("HEAD", path, handler));
    }
    
    @Override
    public Route.Definition head(final String path, final Route.ZeroArgHandler handler) {
        return this.appendDefinition(new Route.Definition("HEAD", path, handler));
    }
    
    @Override
    public Route.Definition head(final String path, final Route.Filter filter) {
        return this.appendDefinition(new Route.Definition("HEAD", path, filter));
    }
    
    @Override
    public Route.Definition head() {
        return this.appendDefinition(new Route.Definition("HEAD", "*", this.filter(HeadHandler.class)).name("*.head"));
    }
    
    @Override
    public Route.Definition options(final String path, final Route.Handler handler) {
        return this.appendDefinition(new Route.Definition("OPTIONS", path, handler));
    }
    
    @Override
    public Route.Definition options(final String path, final Route.OneArgHandler handler) {
        return this.appendDefinition(new Route.Definition("OPTIONS", path, handler));
    }
    
    @Override
    public Route.Definition options(final String path, final Route.ZeroArgHandler handler) {
        return this.appendDefinition(new Route.Definition("OPTIONS", path, handler));
    }
    
    @Override
    public Route.Definition options(final String path, final Route.Filter filter) {
        return this.appendDefinition(new Route.Definition("OPTIONS", path, filter));
    }
    
    @Override
    public Route.Definition options() {
        return this.appendDefinition(new Route.Definition("OPTIONS", "*", this.handler(OptionsHandler.class)).name("*.options"));
    }
    
    @Override
    public Route.Definition put(final String path, final Route.Handler handler) {
        return this.appendDefinition(new Route.Definition("PUT", path, handler));
    }
    
    @Override
    public Route.Collection put(final String path1, final String path2, final Route.Handler handler) {
        return new Route.Collection(new Route.Definition[] { this.put(path1, handler), this.put(path2, handler) });
    }
    
    @Override
    public Route.Collection put(final String path1, final String path2, final String path3, final Route.Handler handler) {
        return new Route.Collection(new Route.Definition[] { this.put(path1, handler), this.put(path2, handler), this.put(path3, handler) });
    }
    
    @Override
    public Route.Definition put(final String path, final Route.OneArgHandler handler) {
        return this.appendDefinition(new Route.Definition("PUT", path, handler));
    }
    
    @Override
    public Route.Collection put(final String path1, final String path2, final Route.OneArgHandler handler) {
        return new Route.Collection(new Route.Definition[] { this.put(path1, handler), this.put(path2, handler) });
    }
    
    @Override
    public Route.Collection put(final String path1, final String path2, final String path3, final Route.OneArgHandler handler) {
        return new Route.Collection(new Route.Definition[] { this.put(path1, handler), this.put(path2, handler), this.put(path3, handler) });
    }
    
    @Override
    public Route.Definition put(final String path, final Route.ZeroArgHandler handler) {
        return this.appendDefinition(new Route.Definition("PUT", path, handler));
    }
    
    @Override
    public Route.Collection put(final String path1, final String path2, final Route.ZeroArgHandler handler) {
        return new Route.Collection(new Route.Definition[] { this.put(path1, handler), this.put(path2, handler) });
    }
    
    @Override
    public Route.Collection put(final String path1, final String path2, final String path3, final Route.ZeroArgHandler handler) {
        return new Route.Collection(new Route.Definition[] { this.put(path1, handler), this.put(path2, handler), this.put(path3, handler) });
    }
    
    @Override
    public Route.Definition put(final String path, final Route.Filter filter) {
        return this.appendDefinition(new Route.Definition("PUT", path, filter));
    }
    
    @Override
    public Route.Collection put(final String path1, final String path2, final Route.Filter filter) {
        return new Route.Collection(new Route.Definition[] { this.put(path1, filter), this.put(path2, filter) });
    }
    
    @Override
    public Route.Collection put(final String path1, final String path2, final String path3, final Route.Filter filter) {
        return new Route.Collection(new Route.Definition[] { this.put(path1, filter), this.put(path2, filter), this.put(path3, filter) });
    }
    
    @Override
    public Route.Definition patch(final String path, final Route.Handler handler) {
        return this.appendDefinition(new Route.Definition("PATCH", path, handler));
    }
    
    @Override
    public Route.Collection patch(final String path1, final String path2, final Route.Handler handler) {
        return new Route.Collection(new Route.Definition[] { this.patch(path1, handler), this.patch(path2, handler) });
    }
    
    @Override
    public Route.Collection patch(final String path1, final String path2, final String path3, final Route.Handler handler) {
        return new Route.Collection(new Route.Definition[] { this.patch(path1, handler), this.patch(path2, handler), this.patch(path3, handler) });
    }
    
    @Override
    public Route.Definition patch(final String path, final Route.OneArgHandler handler) {
        return this.appendDefinition(new Route.Definition("PATCH", path, handler));
    }
    
    @Override
    public Route.Collection patch(final String path1, final String path2, final Route.OneArgHandler handler) {
        return new Route.Collection(new Route.Definition[] { this.patch(path1, handler), this.patch(path2, handler) });
    }
    
    @Override
    public Route.Collection patch(final String path1, final String path2, final String path3, final Route.OneArgHandler handler) {
        return new Route.Collection(new Route.Definition[] { this.patch(path1, handler), this.patch(path2, handler), this.patch(path3, handler) });
    }
    
    @Override
    public Route.Definition patch(final String path, final Route.ZeroArgHandler handler) {
        return this.appendDefinition(new Route.Definition("PATCH", path, handler));
    }
    
    @Override
    public Route.Collection patch(final String path1, final String path2, final Route.ZeroArgHandler handler) {
        return new Route.Collection(new Route.Definition[] { this.patch(path1, handler), this.patch(path2, handler) });
    }
    
    @Override
    public Route.Collection patch(final String path1, final String path2, final String path3, final Route.ZeroArgHandler handler) {
        return new Route.Collection(new Route.Definition[] { this.patch(path1, handler), this.patch(path2, handler), this.patch(path3, handler) });
    }
    
    @Override
    public Route.Definition patch(final String path, final Route.Filter filter) {
        return this.appendDefinition(new Route.Definition("PATCH", path, filter));
    }
    
    @Override
    public Route.Collection patch(final String path1, final String path2, final Route.Filter filter) {
        return new Route.Collection(new Route.Definition[] { this.patch(path1, filter), this.patch(path2, filter) });
    }
    
    @Override
    public Route.Collection patch(final String path1, final String path2, final String path3, final Route.Filter filter) {
        return new Route.Collection(new Route.Definition[] { this.patch(path1, filter), this.patch(path2, filter), this.patch(path3, filter) });
    }
    
    @Override
    public Route.Definition delete(final String path, final Route.Handler handler) {
        return this.appendDefinition(new Route.Definition("DELETE", path, handler));
    }
    
    @Override
    public Route.Collection delete(final String path1, final String path2, final Route.Handler handler) {
        return new Route.Collection(new Route.Definition[] { this.delete(path1, handler), this.delete(path2, handler) });
    }
    
    @Override
    public Route.Collection delete(final String path1, final String path2, final String path3, final Route.Handler handler) {
        return new Route.Collection(new Route.Definition[] { this.delete(path1, handler), this.delete(path2, handler), this.delete(path3, handler) });
    }
    
    @Override
    public Route.Definition delete(final String path, final Route.OneArgHandler handler) {
        return this.appendDefinition(new Route.Definition("DELETE", path, handler));
    }
    
    @Override
    public Route.Collection delete(final String path1, final String path2, final Route.OneArgHandler handler) {
        return new Route.Collection(new Route.Definition[] { this.delete(path1, handler), this.delete(path2, handler) });
    }
    
    @Override
    public Route.Collection delete(final String path1, final String path2, final String path3, final Route.OneArgHandler handler) {
        return new Route.Collection(new Route.Definition[] { this.delete(path1, handler), this.delete(path2, handler), this.delete(path3, handler) });
    }
    
    @Override
    public Route.Definition delete(final String path, final Route.ZeroArgHandler handler) {
        return this.appendDefinition(new Route.Definition("DELETE", path, handler));
    }
    
    @Override
    public Route.Collection delete(final String path1, final String path2, final Route.ZeroArgHandler handler) {
        return new Route.Collection(new Route.Definition[] { this.delete(path1, handler), this.delete(path2, handler) });
    }
    
    @Override
    public Route.Collection delete(final String path1, final String path2, final String path3, final Route.ZeroArgHandler handler) {
        return new Route.Collection(new Route.Definition[] { this.delete(path1, handler), this.delete(path2, handler), this.delete(path3, handler) });
    }
    
    @Override
    public Route.Definition delete(final String path, final Route.Filter filter) {
        return this.appendDefinition(new Route.Definition("DELETE", path, filter));
    }
    
    @Override
    public Route.Collection delete(final String path1, final String path2, final Route.Filter filter) {
        return new Route.Collection(new Route.Definition[] { this.delete(path1, filter), this.delete(path2, filter) });
    }
    
    @Override
    public Route.Collection delete(final String path1, final String path2, final String path3, final Route.Filter filter) {
        return new Route.Collection(new Route.Definition[] { this.delete(path1, filter), this.delete(path2, filter), this.delete(path3, filter) });
    }
    
    @Override
    public Route.Definition trace(final String path, final Route.Handler handler) {
        return this.appendDefinition(new Route.Definition("TRACE", path, handler));
    }
    
    @Override
    public Route.Definition trace(final String path, final Route.OneArgHandler handler) {
        return this.appendDefinition(new Route.Definition("TRACE", path, handler));
    }
    
    @Override
    public Route.Definition trace(final String path, final Route.ZeroArgHandler handler) {
        return this.appendDefinition(new Route.Definition("TRACE", path, handler));
    }
    
    @Override
    public Route.Definition trace(final String path, final Route.Filter filter) {
        return this.appendDefinition(new Route.Definition("TRACE", path, filter));
    }
    
    @Override
    public Route.Definition trace() {
        return this.appendDefinition(new Route.Definition("TRACE", "*", this.handler(TraceHandler.class)).name("*.trace"));
    }
    
    @Override
    public Route.Definition connect(final String path, final Route.Handler handler) {
        return this.appendDefinition(new Route.Definition("CONNECT", path, handler));
    }
    
    @Override
    public Route.Definition connect(final String path, final Route.OneArgHandler handler) {
        return this.appendDefinition(new Route.Definition("CONNECT", path, handler));
    }
    
    @Override
    public Route.Definition connect(final String path, final Route.ZeroArgHandler handler) {
        return this.appendDefinition(new Route.Definition("CONNECT", path, handler));
    }
    
    @Override
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
    
    @Override
    public Route.Definition assets(final String path) {
        return this.assets(path, "/");
    }
    
    @Override
    public Route.Definition assets(final String path, final String location) {
        return this.assets(path, new AssetHandler(location));
    }
    
    @Override
    public Route.Definition assets(final String path, final AssetHandler handler) {
        final AssetProxy router = new AssetProxy();
        final Route.Definition asset = new Route.Definition("GET", path, router);
        this.on("*", conf -> router.fwd(handler.cdn(conf.getString("assets.cdn")).lastModified(conf.getBoolean("assets.lastModified")).etag(conf.getBoolean("assets.etag"))));
        return this.appendDefinition(asset);
    }
    
    @Override
    public Jooby use(final Class<?> routeClass) {
        Objects.requireNonNull(routeClass, "Route class is required.");
        this.bag.add(new RouteClass(routeClass, ""));
        return this;
    }
    
    private Route.Definition appendDefinition(final Route.Definition route) {
        if (this.prefix != null) {
            route.name(this.prefix + "/" + route.name());
        }
        this.bag.add(route);
        return route;
    }
    
    public Jooby use(final Module module) {
        Objects.requireNonNull(module, "A module is required.");
        this.bag.add(module);
        return this;
    }
    
    public Jooby use(final Config config) {
        this.srcconf = Objects.requireNonNull(config, "A config is required.");
        return this;
    }
    
    public Jooby err(final Err.Handler err) {
        this.bag.add(Objects.requireNonNull(err, "An err handler is required."));
        return this;
    }
    
    @Override
    public WebSocket.Definition ws(final String path, final WebSocket.Handler handler) {
        final WebSocket.Definition ws = new WebSocket.Definition(path, handler);
        Preconditions.checkArgument(this.bag.add(ws), "Path is in use: '%s'", new Object[] { path });
        return ws;
    }
    
    public static void run(final Supplier<? extends Jooby> app, final String... args) throws Exception {
        final Config conf = ConfigFactory.systemProperties().withFallback((ConfigMergeable)args(args));
        System.setProperty("logback.configurationFile", logback(conf));
        ((Jooby)app.get()).start(args);
    }
    
    public static void run(final Class<? extends Jooby> app, final String... args) throws Exception {
        run(() -> (Jooby)Try.of(() -> app.newInstance()).get(), args);
    }
    
    public void start() throws Exception {
        this.start(new String[0]);
    }
    
    public void start(final Consumer<List<Route.Definition>> routes) throws Exception {
        this.start(new String[0], routes);
    }
    
    public void start(final String[] args) throws Exception {
        this.start(args, null);
    }
    
    public void start(final String[] args, final Consumer<List<Route.Definition>> routes) throws Exception {
        final long start = System.currentTimeMillis();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> this.stop()));
        this.injector = this.bootstrap(args(args), routes);
        final Config config = (Config)this.injector.getInstance((Class)Config.class);
        final Logger log = LoggerFactory.getLogger((Class)this.getClass());
        log.debug("config tree:\n{}", (Object)this.configTree(config.origin().description()));
        final Server server = (Server)this.injector.getInstance((Class)Server.class);
        final String serverName = server.getClass().getSimpleName().replace("Server", "").toLowerCase();
        server.start();
        final long end = System.currentTimeMillis();
        log.info("[{}@{}]: Server started in {}ms\n\n{}\n", new Object[] { config.getString("application.env"), serverName, end - start, this.injector.getInstance((Class)AppPrinter.class) });
        this.onStart.forEach(Runnable::run);
        final boolean join = !config.hasPath("server.join") || config.getBoolean("server.join");
        if (join) {
            server.join();
        }
    }
    
    public static void main(final String[] jsargs) throws Exception {
        final String[] args = new String[Math.max(0, jsargs.length - 1)];
        if (args.length > 0) {
            System.arraycopy(jsargs, 1, args, 0, args.length);
        }
        final String filename = (jsargs.length > 0) ? jsargs[0] : "app.js";
        run(new JsJooby().run(new File(filename)), args);
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
    
    private static List<Object> normalize(final List<Object> services, final Env env, final RouteMetadata classInfo, final String prefix) {
        final List<Object> result = new ArrayList<Object>();
        final List<Object> snapshot = services;
        final List<RouteClass> list;
        Class<?> routeClass;
        String path;
        final List<Route.Definition> list2;
        snapshot.forEach(candidate -> {
            if (candidate instanceof Route.Definition) {
                list.add(candidate);
            }
            else if (candidate instanceof Route.Group) {
                ((Route.Group)candidate).routes().forEach(r -> list.add(r));
            }
            else if (candidate instanceof RouteClass) {
                routeClass = candidate.routeClass;
                path = candidate.path;
                MvcRoutes.routes(env, classInfo, path, routeClass).forEach(route -> {
                    if (prefix != null) {
                        route.name(prefix + "/" + route.name());
                    }
                    list2.add(route);
                });
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
    
    private Injector bootstrap(final Config args, final Consumer<List<Route.Definition>> rcallback) throws Exception {
        final Config appconf = ConfigFactory.parseResources("application.conf");
        final Config initconf = (this.srcconf == null) ? appconf : this.srcconf.withFallback((ConfigMergeable)appconf);
        final List<Config> modconf = modconf(this.bag);
        final Config conf = this.buildConfig(initconf, args, modconf);
        final List<Locale> locales = LocaleUtils.parse(conf.getString("application.lang"));
        final Env env = this.env.build(conf, this, locales.get(0));
        final String envname = env.name();
        final Charset charset = Charset.forName(conf.getString("application.charset"));
        final String dateFormat = conf.getString("application.dateFormat");
        final ZoneId zoneId = ZoneId.of(conf.getString("application.tz"));
        final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateFormat, locales.get(0)).withZone(zoneId);
        final DecimalFormat numberFormat = new DecimalFormat(conf.getString("application.numberFormat"));
        final Stage stage = "dev".equals(envname) ? Stage.DEVELOPMENT : Stage.PRODUCTION;
        final RouteMetadata rm = new RouteMetadata(env);
        final List<Object> realbag = processEnvDep(this.bag, env);
        final List<Config> realmodconf = modconf(realbag);
        final List<Object> bag = normalize(realbag, env, rm, this.prefix);
        final List<Route.Definition> routes = bag.stream().filter(it -> it instanceof Route.Definition).map(it -> it).collect((Collector<? super Object, ?, List<Route.Definition>>)Collectors.toList());
        if (rcallback != null) {
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
        final Injector injector = Guice.createInjector(stage, new com.google.inject.Module[] { binder -> {
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
                final LifecycleProcessor lifecycleProcessor = new LifecycleProcessor();
                binder.bind((Class)LifecycleProcessor.class).toInstance((Object)lifecycleProcessor);
                binder.bindListener(Matchers.any(), (TypeListener)lifecycleProcessor);
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
                parsers.addBinding().toInstance((Object)BuiltinParser.Upload);
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
                bag.forEach(it -> bindService(this.bag, finalConfig, finalEnv, rm, binder, definitions, sockets, ehandlers, parsers, renderers, routeClasses).accept(it));
                parsers.addBinding().toInstance((Object)new DateParser(dateFormat));
                parsers.addBinding().toInstance((Object)new LocalDateParser(dateTimeFormatter));
                parsers.addBinding().toInstance((Object)new LocaleParser());
                parsers.addBinding().toInstance((Object)new BeanParser());
                parsers.addBinding().toInstance((Object)new StaticMethodParser("valueOf"));
                parsers.addBinding().toInstance((Object)new StaticMethodParser("fromString"));
                parsers.addBinding().toInstance((Object)new StaticMethodParser("forName"));
                parsers.addBinding().toInstance((Object)new StringConstructorParser());
                binder.bind((Class)ParserExecutor.class).in((Class)Singleton.class);
                final boolean stacktrace = finalConfig.hasPath("err.stacktrace") ? finalConfig.getBoolean("err.stacktrace") : "dev".equals(envname);
                renderers.addBinding().toInstance((Object)new DefaulErrRenderer(stacktrace));
                renderers.addBinding().toInstance((Object)BuiltinRenderer.text);
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
        this.onStart.addAll(0, finalEnv.startTasks());
        this.onStop.addAll(finalEnv.stopTasks());
        this.bag.clear();
        this.bag = (Set<Object>)ImmutableSet.of();
        return injector;
    }
    
    private static Consumer<? super Object> bindService(final Set<Object> src, final Config conf, final Env env, final RouteMetadata rm, final Binder binder, final Multibinder<Route.Definition> definitions, final Multibinder<WebSocket.Definition> sockets, final Multibinder<Err.Handler> ehandlers, final Multibinder<Parser> parsers, final Multibinder<Renderer> renderers, final Set<Object> routeClasses) {
        int from;
        int to;
        Route.Definition rdef;
        Route.Filter h;
        Class<?> routeClass;
        return it -> {
            if (it instanceof Module) {
                from = src.size();
                install((Module)it, env, conf, binder);
                to = src.size();
                if (to > from) {
                    normalize(new ArrayList<Object>(src).subList(from, to), env, rm, null).forEach(e -> bindService(src, conf, env, rm, binder, definitions, sockets, ehandlers, parsers, renderers, routeClasses).accept(e));
                }
            }
            else if (it instanceof Route.Definition) {
                rdef = (Route.Definition)it;
                h = rdef.filter();
                if (h instanceof Route.MethodHandler) {
                    routeClass = ((Route.MethodHandler)h).method().getDeclaringClass();
                    if (routeClasses.add(routeClass)) {
                        binder.bind((Class)routeClass);
                    }
                }
                definitions.addBinding().toInstance((Object)rdef);
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
        };
    }
    
    private static List<Config> modconf(final Collection<Object> bag) {
        return bag.stream().filter(it -> it instanceof Module).map(it -> it.config()).filter(c -> !c.isEmpty()).collect((Collector<? super Object, ?, List<Config>>)Collectors.toList());
    }
    
    public void stop() {
        if (this.injector != null) {
            stopManaged(this.injector, this.onStop);
            final Logger log = LoggerFactory.getLogger((Class)this.getClass());
            try {
                final Server server = (Server)this.injector.getInstance((Class)Server.class);
                final String serverName = server.getClass().getSimpleName().replace("Server", "").toLowerCase();
                server.stop();
                log.info("[{}] Server stopped", (Object)serverName);
            }
            catch (Exception ex) {
                log.error("Web server didn't stop normally", (Throwable)ex);
            }
            this.injector = null;
        }
    }
    
    private static void stopManaged(final Injector injector, final List<Runnable> onStop) {
        ((LifecycleProcessor)injector.getInstance((Class)LifecycleProcessor.class)).destroy();
        onStop.forEach(Runnable::run);
    }
    
    private Config buildConfig(final Config source, final Config args, final List<Config> modules) {
        Config system = ConfigFactory.systemProperties();
        final Config tmpdir = source.hasPath("java.io.tmpdir") ? source : system;
        system = system.withValue("file.encoding", ConfigValueFactory.fromAnyRef((Object)System.getProperty("file.encoding"))).withValue("java.io.tmpdir", ConfigValueFactory.fromAnyRef((Object)Paths.get(tmpdir.getString("java.io.tmpdir"), new String[0]).normalize().toString()));
        Config moduleStack = ConfigFactory.empty();
        for (final Config module : ImmutableList.copyOf((Collection)modules).reverse()) {
            moduleStack = moduleStack.withFallback((ConfigMergeable)module);
        }
        final String env = Arrays.asList(system, source).stream().filter(it -> it.hasPath("application.env")).findFirst().map(c -> c.getString("application.env")).orElse("dev");
        final Config modeConfig = this.modeConfig(source, env);
        final Config config = modeConfig.withFallback((ConfigMergeable)source);
        return system.withFallback((ConfigMergeable)args).withFallback((ConfigMergeable)config).withFallback((ConfigMergeable)moduleStack).withFallback((ConfigMergeable)MediaType.types).withFallback((ConfigMergeable)this.defaultConfig(config)).resolve();
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
    
    private Config modeConfig(final Config source, final String env) {
        final String origin = source.origin().resource();
        Config result = ConfigFactory.empty();
        if (origin != null) {
            final int dot = origin.lastIndexOf(46);
            final String originConf = origin.substring(0, dot) + "." + env + origin.substring(dot);
            result = fileConfig(originConf).withFallback((ConfigMergeable)ConfigFactory.parseResources(originConf));
        }
        final String appConfig = "application." + env + ".conf";
        return result.withFallback((ConfigMergeable)fileConfig(appConfig)).withFallback((ConfigMergeable)fileConfig("application.conf")).withFallback((ConfigMergeable)ConfigFactory.parseResources(appConfig));
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
    
    private Config defaultConfig(final Config config) {
        final String ns = this.getClass().getPackage().getName();
        final String[] parts = ns.split("\\.");
        final String appname = parts[parts.length - 1];
        List<Locale> locales;
        if (!config.hasPath("application.lang")) {
            locales = (List<Locale>)ImmutableList.of((Object)Locale.getDefault());
        }
        else {
            locales = LocaleUtils.parse(config.getString("application.lang"));
        }
        final Locale locale = locales.iterator().next();
        final String lang = locale.toLanguageTag();
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
        final String version = Optional.ofNullable(this.getClass().getPackage().getImplementationVersion()).orElse("0.0.0");
        final Config defs = ConfigFactory.parseResources((Class)Jooby.class, "jooby.conf").withValue("application.name", ConfigValueFactory.fromAnyRef((Object)appname)).withValue("application.version", ConfigValueFactory.fromAnyRef((Object)version)).withValue("application.class", ConfigValueFactory.fromAnyRef((Object)this.getClass().getName())).withValue("application.ns", ConfigValueFactory.fromAnyRef((Object)ns)).withValue("application.lang", ConfigValueFactory.fromAnyRef((Object)lang)).withValue("application.tz", ConfigValueFactory.fromAnyRef((Object)tz)).withValue("application.numberFormat", ConfigValueFactory.fromAnyRef((Object)nf)).withValue("runtime.processors", ConfigValueFactory.fromAnyRef((Object)processors)).withValue("runtime.processors-plus1", ConfigValueFactory.fromAnyRef((Object)(processors + 1))).withValue("runtime.processors-plus2", ConfigValueFactory.fromAnyRef((Object)(processors + 2))).withValue("runtime.processors-x2", ConfigValueFactory.fromAnyRef((Object)(processors * 2))).withValue("runtime.concurrencyLevel", ConfigValueFactory.fromAnyRef((Object)Math.max(4, processors)));
        return defs;
    }
    
    private static void install(final Module module, final Env env, final Config config, final Binder binder) {
        try {
            module.configure(env, config, binder);
        }
        catch (Exception ex) {
            throw new IllegalStateException("Error found on module: " + module.getClass().getName(), ex);
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
            final ImmutableList.Builder<File> files = (ImmutableList.Builder<File>)ImmutableList.builder();
            final File userdir = new File(System.getProperty("user.dir"));
            final File confdir = new File(userdir, "conf");
            if (conf.hasPath("application.env")) {
                final String env = conf.getString("application.env");
                files.add((Object)new File(userdir, "logback." + env + ".xml"));
                files.add((Object)new File(confdir, "logback." + env + ".xml"));
            }
            files.add((Object)new File(userdir, "logback.xml"));
            files.add((Object)new File(confdir, "logback.xml"));
            logback = files.build().stream().filter(f -> f.exists()).map(f -> f.getAbsolutePath()).findFirst().orElse("logback.xml");
        }
        return logback;
    }
    
    static {
        final String pid = System.getProperty("pid", JvmInfo.pid() + "");
        System.setProperty("pid", pid);
    }
    
    public interface Module
    {
        default Config config() {
            return ConfigFactory.empty();
        }
        
        void configure(final Env env, final Config conf, final Binder binder);
    }
    
    private static class RouteClass
    {
        Class<?> routeClass;
        String path;
        
        public RouteClass(final Class<?> routeClass, final String path) {
            this.routeClass = routeClass;
            this.path = path;
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
