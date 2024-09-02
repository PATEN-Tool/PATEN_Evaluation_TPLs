/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;

import javax.annotation.Nonnull;

import org.jooby.internal.AssetFormatter;
import org.jooby.internal.AssetRoute;
import org.jooby.internal.FallbackBodyConverter;
import org.jooby.internal.RoutePattern;
import org.jooby.internal.Server;
import org.jooby.internal.TypeConverters;
import org.jooby.internal.jetty.Jetty;
import org.jooby.internal.mvc.Routes;
import org.jooby.internal.routes.HeadFilter;
import org.jooby.internal.routes.OptionsRouter;
import org.jooby.internal.routes.TraceRouter;
import org.slf4j.LoggerFactory;

import ch.qos.logback.core.joran.spi.JoranException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Scopes;
import com.google.inject.Stage;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.google.inject.util.Types;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueFactory;

/**
 * <h1>Getting Started:</h1>
 * <p>
 * A new application must extends Jooby, register one ore more {@link Body.Formatter} and some
 * {@link Route routes}. It sounds like a lot of work to do, but it isn't.
 * </p>
 *
 * <pre>
 * public class MyApp extends Jooby {
 *
 *   {
 *      use(new Json()); // 1. JSON serializer.
 *
 *      // 2. Define a route
 *      get("/", (req, rsp) {@literal ->} {
 *        Map{@literal <}String, Object{@literal >} model = ...;
 *        rsp.send(model);
 *      }
 *   }
 *
 *  public static void main(String[] args) throws Exception {
 *    new MyApp().start(); // 3. Done!
 *  }
 * }
 * </pre>
 *
 * <h1>Properties files</h1>
 * <p>
 * Jooby delegate configuration management to <a
 * href="https://github.com/typesafehub/config">TypeSafe Config</a>. If you are unfamiliar with <a
 * href="https://github.com/typesafehub/config">TypeSafe Config</a> please take a few minutes to
 * discover what <a href="https://github.com/typesafehub/config">TypeSafe Config</a> can do for you.
 * </p>
 *
 * <p>
 * By default Jooby looks for an <code>application.conf</code> file at the root of the classpath. If
 * you want to specify a different file or location, you can do it with {@link #use(Config)}.
 * </p>
 *
 * <p>
 * <a href="https://github.com/typesafehub/config">TypeSafe Config</a> uses a hierarchical model to
 * define and override properties.
 * </p>
 * <p>
 * A {@link Jooby.Module} might provides his own set of properties through the
 * {@link Jooby.Module#config()} method. By default, this method returns an empty config object.
 * </p>
 * For example:
 *
 * <pre>
 *   use(new M1());
 *   use(new M2());
 *   use(new M3());
 * </pre>
 *
 * Previous example had the following order (first-listed are higher priority):
 * <ul>
 * <li>System properties</li>
 * <li>application.conf</li>
 * <li>M3 properties</li>
 * <li>M2 properties</li>
 * <li>M1 properties</li>
 * </ul>
 * <p>
 * System properties takes precedence over any application specific property.
 * </p>
 *
 * <h1>Mode</h1>
 * <p>
 * Jooby defines two modes: <strong>dev</strong> or something else. In Jooby, <strong>dev</strong>
 * is special and some modules could apply special settings while running in <strong>dev</strong>.
 * Any other mode is usually considered a <code>prod</code> like mode. But that depends on module
 * implementor.
 * </p>
 * <p>
 * A mode can be defined in your <code>application.conf</code> file using the
 * <code>application.mode</code> property. If missing, Jooby set the mode for you to
 * <strong>dev</strong>.
 * </p>
 * <p>
 * There is more at {@link Mode} so take a few minutes to discover what a {@link Mode} can do for
 * you.
 * </p>
 *
 * <h1>Modules</h1>
 * <p>
 * {@link Jooby.Module Modules} are quite similar to a Guice modules except that the configure
 * callback has been complementing with {@link Mode} and {@link Config}.
 * </p>
 *
 * <pre>
 *   public class MyModule implements Jooby.Module {
 *     public void configure(Mode mode, Config config, Binder binder) {
 *     }
 *   }
 * </pre>
 *
 * From the configure callback you can bind your services as you usually do in a Guice app.
 * <p>
 * There is more at {@link Jooby.Module} so take a few minutes to discover what a
 * {@link Jooby.Module} can do for you.
 * </p>
 *
 * <h1>Path Patterns</h1>
 * <p>
 * Jooby supports Ant-style path patterns:
 * </p>
 * <p>
 * Some examples:
 * </p>
 * <ul>
 * <li>{@code com/t?st.html} - matches {@code com/test.html} but also {@code com/tast.html} or
 * {@code com/txst.html}</li>
 * <li>{@code com/*.html} - matches all {@code .html} files in the {@code com} directory</li>
 * <li><code>com/{@literal **}/test.html</code> - matches all {@code test.html} files underneath the
 * {@code com} path</li>
 * <li>{@code **}/{@code *} - matches any path at any level.</li>
 * <li>{@code *} - matches any path at any level, shorthand for {@code **}/{@code *}.</li>
 * </ul>
 *
 * <h2>Variables</h2>
 * <p>
 * Jooby supports path parameters too:
 * </p>
 * <p>
 * Some examples:
 * </p>
 * <ul>
 * <li><code> /user/{id}</code> - /user/* and give you access to the <code>id</code> var.</li>
 * <li><code> /user/:id</code> - /user/* and give you access to the <code>id</code> var.</li>
 * <li><code> /user/{id:\\d+}</code> - /user/[digits] and give you access to the numeric
 * <code>id</code> var.</li>
 * </ul>
 *
 * <h1>Routes</h1>
 * <p>
 * Routes perform actions in response to a server HTTP request. There are two types of routes
 * callback: {@link Route.Handler} and {@link Route.Filter}.
 * </p>
 * <p>
 * Routes are executed in the order they are defined, for example:
 *
 * <pre>
 *   get("/", (req, rsp) {@literal ->} {
 *     log.info("first"); // start here and go to second
 *   });
 *
 *   get("/", (req, rsp) {@literal ->} {
 *     log.info("second"); // execute after first and go to final
 *   });
 *
 *   get("/", (req, rsp) {@literal ->} {
 *     rsp.send("final"); // done!
 *   });
 * </pre>
 *
 * Please note first and second routes are converted to a filter, so previous example is the same
 * as:
 *
 * <pre>
 *   get("/", (req, rsp, chain) {@literal ->} {
 *     log.info("first"); // start here and go to second
 *     chain.next(req, rsp);
 *   });
 *
 *   get("/", (req, rsp, chain) {@literal ->} {
 *     log.info("second"); // execute after first and go to final
 *     chain.next(req, rsp);
 *   });
 *
 *   get("/", (req, rsp) {@literal ->} {
 *     rsp.send("final"); // done!
 *   });
 * </pre>
 *
 *
 * <h2>Inline route</h2>
 * <p>
 * An inline route can be defined using Lambda expressions, like:
 * </p>
 *
 * <pre>
 *   get("/", (request, response) {@literal ->} {
 *     response.send("Hello Jooby");
 *   });
 * </pre>
 *
 * Due to the use of lambdas a route is a singleton and you should NOT use global variables.
 * For example this is a bad practice:
 *
 * <pre>
 *  List{@literal <}String{@literal >} names = new ArrayList{@literal <}{@literal >}(); // names produces side effects
 *  get("/", (req, rsp) {@literal ->} {
 *     names.add(req.param("name").stringValue();
 *     // response will be different between calls.
 *     rsp.send(names);
 *   });
 * </pre>
 *
 * <h2>External route</h2>
 * <p>
 * An external route can be defined by using a {@link Class route class}, like:
 * </p>
 *
 * <pre>
 *   get("/", route(ExternalRoute.class)); //or
 *
 *   ...
 *   // ExternalRoute.java
 *   public class ExternalRoute implements Route.Handler {
 *     public void handle(Request req, Response rsp) throws Exception {
 *       rsp.send("Hello Jooby");
 *     }
 *   }
 * </pre>
 *
 * <h2>Mvc Route</h2>
 * <p>
 * A Mvc route use annotations to define routes:
 * </p>
 *
 * <pre>
 *   route(MyRoute.class);
 *   ...
 *   // MyRoute.java
 *   {@literal @}Path("/")
 *   public class MyRoute {
 *
 *    {@literal @}GET
 *    public String hello() {
 *      return "Hello Jooby";
 *    }
 *   }
 * </pre>
 * <p>
 * Programming model is quite similar to JAX-RS/Jersey with some minor differences and/or
 * simplifications.
 * </p>
 *
 * <p>
 * To learn more about Mvc Routes, please check {@link org.jooby.mvc.Path},
 * {@link org.jooby.mvc.Produces} {@link org.jooby.mvc.Consumes}, and {@link org.jooby.mvc.Viewable}
 * .
 * </p>
 *
 * <h1>Static Files</h1>
 * <p>
 * Static files, like: *.js, *.css, ..., etc... can be served with:
 * </p>
 *
 * <pre>
 *   assets("assets/**");
 * </pre>
 * <p>
 * Classpath resources under the <code>/assets</code> folder will be accessible from client/browser.
 * </p>
 * <h1>Bootstrap</h1>
 * <p>
 * The bootstrap process is defined as follows:
 * </p>
 * <h2>1. Configuration files are loaded in this order:</h2>
 * <ol>
 * <li>System properties</li>
 * <li>Application properties: {@code application.conf} or custom, see {@link #use(Config)}</li>
 * <li>Configuration properties from {@link Jooby.Module modules}</li>
 * </ol>
 *
 * <h2>2. Dependency Injection and {@link Jooby.Module modules}</h2>
 * <ol>
 * <li>An {@link Injector Guice Injector} is created.</li>
 * <li>It configures each registered {@link Jooby.Module module}</li>
 * <li>At this point Guice is ready and all the services has been binded.</li>
 * <li>The {@link Jooby.Module#start() start method} is invoked.</li>
 * <li>Finally, Jooby starts the web server</li>
 * </ol>
 *
 * @author edgar
 * @since 0.1.0
 * @see Jooby.Module
 */
public class Jooby {

  /**
   * A module can publish or produces: {@link Route.Definition routes}, {@link Body.Parser},
   * {@link Body.Formatter}, {@link Request.Module request modules} and any other
   * application specific service or contract of your choice.
   * <p>
   * It is similar to {@link com.google.inject.Module} except for the callback method receives a
   * {@link Mode}, {@link Config} and {@link Binder}.
   * </p>
   *
   * <p>
   * A module can provide his own set of properties through the {@link #config()} method. By
   * default, this method returns an empty config object.
   * </p>
   * For example:
   *
   * <pre>
   *   use(new M1());
   *   use(new M2());
   *   use(new M3());
   * </pre>
   *
   * Previous example had the following order (first-listed are higher priority):
   * <ul>
   * <li>System properties</li>
   * <li>application.conf</li>
   * <li>M3 properties</li>
   * <li>M2 properties</li>
   * <li>M1 properties</li>
   * </ul>
   *
   * <p>
   * A module can provide start/stop methods in order to start or close resources.
   * </p>
   *
   * @author edgar
   * @since 0.1.0
   * @see Jooby#use(Jooby.Module)
   */
  public interface Module {

    /**
     * @return Produces a module config object (when need it). By default a module doesn't produce
     *         any configuration object.
     */
    default @Nonnull Config config() {
      return ConfigFactory.empty();
    }

    /**
     * Callback method to start a module. This method will be invoked after all the registered
     * modules has been configured.
     *
     * @throws Exception If something goes wrong.
     */
    default void start() throws Exception {
    }

    /**
     * Callback method to stop a module and clean any resources. Invoked when the application is
     * about
     * to shutdown.
     *
     * @throws Exception If something goes wrong.
     */
    default void stop() throws Exception {
    }

    /**
     * Configure and produces bindings for the underlying application. A module can optimize or
     * customize a service by checking current the {@link Mode application mode} and/or the current
     * application properties available from {@link Config}.
     *
     * @param mode The current application's mode. Not null.
     * @param config The current config object. Not null.
     * @param binder A guice binder. Not null.
     * @throws Exception If the module fails during configuration.
     */
    void configure(@Nonnull Mode mode, @Nonnull Config config,
        @Nonnull Binder binder) throws Exception;

  }

  /**
   * Keep track of routes.
   */
  private final Set<Object> bag = new LinkedHashSet<>();

  /**
   * Keep track of modules.
   */
  private final Set<Jooby.Module> modules = new LinkedHashSet<>();

  /**
   * Keep track of singleton MVC routes.
   */
  private final Set<Class<?>> singletonRoutes = new LinkedHashSet<>();

  /**
   * Keep track of prototype MVC routes.
   */
  private final Set<Class<?>> protoRoutes = new LinkedHashSet<>();

  /**
   * The override config. Optional.
   */
  private Config source;

  /** Keep the global injector instance. */
  private Injector injector;

  /** Error handler. */
  private Err.Handler err;

  /** Body formatters. */
  private List<Body.Formatter> formatters = new LinkedList<>();

  /** Body parsers. */
  private List<Body.Parser> parsers = new LinkedList<>();

  /** Session store. */
  private Session.Definition session = new Session.Definition(Session.Store.NOOP);

  {
    use(new Jetty());
    // write/format static resources
    formatters.add(new AssetFormatter());
  }

  /**
   * Setup a session store to use. Useful if you want/need to persist sessions between shutdowns.
   * Sessions are not persisted by defaults.
   *
   * @param sessionStore A session store.
   * @return A session store definition.
   */
  public @Nonnull Session.Definition use(@Nonnull final Session.Store sessionStore) {
    this.session = new Session.Definition(requireNonNull(sessionStore,
        "A session store is required."));
    return this.session;
  }

  /**
   * Append a body formatter for write HTTP messages.
   *
   * @param formatter A body formatter.
   * @return This jooby instance.
   */
  public @Nonnull Jooby use(@Nonnull final Body.Formatter formatter) {
    this.formatters.add(requireNonNull(formatter, "A body formatter is required."));
    return this;
  }

  /**
   * Append a body parser for write HTTP messages.
   *
   * @param parser A body parser.
   * @return This jooby instance.
   */
  public @Nonnull Jooby use(@Nonnull final Body.Parser parser) {
    this.parsers.add(requireNonNull(parser, "A body parser is required."));
    return this;
  }

  /**
   * Append a new filter that matches any method under the given path.
   *
   * @param path A path pattern.
   * @param filter A filter to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition use(final @Nonnull String path,
      final @Nonnull Route.Filter filter) {
    return handler(new Route.Definition("*", path, filter));
  }

  /**
   * Append a new filter that matches any method under the given path.
   *
   * @param verb A HTTP verb.
   * @param path A path pattern.
   * @param filter A filter to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition use(final @Nonnull String verb, final @Nonnull String path,
      final @Nonnull Route.Filter filter) {
    return handler(new Route.Definition(verb, path, filter));
  }

  /**
   * Append a new route handler that matches any method under the given path.
   *
   * @param verb A HTTP verb.
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition use(final @Nonnull String verb, final @Nonnull String path,
      final @Nonnull Route.Handler handler) {
    return handler(new Route.Definition(verb, path, handler));
  }

  /**
   * Append a new route handler that matches any method under the given path.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition use(final @Nonnull String path,
      final @Nonnull Route.Handler handler) {
    return handler(new Route.Definition("*", path, handler));
  }

  /**
   * Serve a static file from classpath:
   *
   * <pre>
   *   get("/favicon.ico");
   * </pre>
   *
   * This method is a shorcut for:
   *
   * <pre>
   *   get("/favicon.ico", file("/favicon.ico");
   * </pre>
   *
   * @param path A path pattern.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition get(final String path) {
    return handler(new Route.Definition("GET", path, file(path)));
  }

  /**
   * Define an in-line route that supports HTTP GET method:
   *
   * <pre>
   *   get("/", (req, rsp) {@literal ->} {
   *     rsp.send(something);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition get(final @Nonnull String path,
      final @Nonnull Route.Handler handler) {
    return handler(new Route.Definition("GET", path, handler));
  }

  /**
   * Append a new in-line filter that supports HTTP GET method:
   *
   * <pre>
   *   get("/", (req, rsp, chain) {@literal ->} {
   *     chain.next(req, rsp);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param filter A filter to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition get(final @Nonnull String path,
      final @Nonnull Route.Filter filter) {
    return handler(new Route.Definition("GET", path, filter));
  }

  /**
   * Append a new in-line route that supports HTTP POST method:
   *
   * <pre>
   *   post("/", (req, rsp) {@literal ->} {
   *     rsp.send(something);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition post(final @Nonnull String path,
      final @Nonnull Route.Handler handler) {
    return handler(new Route.Definition("POST", path, handler));
  }

  /**
   * Append a new in-line route that supports HTTP POST method:
   *
   * <pre>
   *   post("/", (req, rsp, chain) {@literal ->} {
   *     chain.next(req, rsp);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param filter A filter to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition post(final @Nonnull String path,
      final @Nonnull Route.Filter filter) {
    return handler(new Route.Definition("POST", path, filter));
  }

  /**
   * Append a new in-line route that supports HTTP HEAD method:
   *
   * <pre>
   *   post("/", (req, rsp) {@literal ->} {
   *     rsp.send(something);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public Route.Definition head(final @Nonnull String path, final @Nonnull Route.Handler handler) {
    return handler(new Route.Definition("HEAD", path, handler));
  }

  /**
   * Append a new in-line route that supports HTTP HEAD method:
   *
   * <pre>
   *   post("/", (req, rsp, chain) {@literal ->} {
   *     chain.next(req, rsp);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param filter A filter to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition head(final @Nonnull String path,
      final @Nonnull Route.Filter filter) {
    return handler(new Route.Definition("HEAD", path, filter));
  }

  /**
   * Append a new route that automatically handles HEAD request from existing GET routes.
   *
   * <pre>
   *   get("/", (req, rsp) {@literal ->} {
   *     rsp.send(something); // This route provides default HEAD for this GET route.
   *   });
   * </pre>
   *
   * @param path A path pattern.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition head(final @Nonnull String path) {
    return handler(new Route.Definition("HEAD", path, filter(HeadFilter.class)).name("*.head"));
  }

  /**
   * Append a new in-line route that supports HTTP OPTIONS method:
   *
   * <pre>
   *   options("/", (req, rsp) {@literal ->} {
   *     rsp.header("Allow", "GET, POST");
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition options(final @Nonnull String path,
      final @Nonnull Route.Handler handler) {
    return handler(new Route.Definition("OPTIONS", path, handler));
  }

  /**
   * Append a new in-line route that supports HTTP OPTIONS method:
   *
   * <pre>
   *   options("/", (req, rsp, chain) {@literal ->} {
   *     rsp.header("Allow", "GET, POST");
   *     chain.next(req, rsp);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param filter A callback to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition options(final @Nonnull String path,
      final @Nonnull Route.Filter filter) {
    return handler(new Route.Definition("OPTIONS", path, filter));
  }

  /**
   * Append a new route that automatically handles OPTIONS requests.
   *
   * <pre>
   *   get("/", (req, rsp) {@literal ->} {
   *     rsp.send(something);
   *   });
   *
   *   post("/", (req, rsp) {@literal ->} {
   *     rsp.send(something);
   *   });
   * </pre>
   *
   * OPTINOS / produces a response with a Allow header set to: GET, POST.
   *
   * @param path A path pattern.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition options(final @Nonnull String path) {
    return handler(new Route.Definition("OPTIONS", path, handler(OptionsRouter.class))
        .name("*.options"));
  }

  /**
   * Define an in-line route that supports HTTP PUT method:
   *
   * <pre>
   *   put("/", (req, rsp) {@literal ->} {
   *     rsp.send(something);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A route to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition put(final @Nonnull String path,
      final @Nonnull Route.Handler handler) {
    return handler(new Route.Definition("PUT", path, handler));
  }

  /**
   * Define an in-line route that supports HTTP PUT method:
   *
   * <pre>
   *   put("/", (req, rsp, chain) {@literal ->} {
   *     chain.next(req, rsp);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param filter A callback to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition put(final @Nonnull String path,
      final @Nonnull Route.Filter filter) {
    return handler(new Route.Definition("PUT", path, filter));
  }

  /**
   * Append a new in-line route that supports HTTP DELETE method:
   *
   * <pre>
   *   delete("/", (req, rsp) {@literal ->} {
   *     rsp.status(304);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition delete(final @Nonnull String path,
      final @Nonnull Route.Handler handler) {
    return handler(new Route.Definition("DELETE", path, handler));
  }

  /**
   * Append a new in-line route that supports HTTP DELETE method:
   *
   * <pre>
   *   delete("/", (req, rsp, chain) {@literal ->} {
   *     rsp.status(304);
   *     chain.next(req, rsp);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param filter A callback to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition delete(final @Nonnull String path,
      final @Nonnull Route.Filter filter) {
    return handler(new Route.Definition("DELETE", path, filter));
  }

  /**
   * Append a new in-line route that supports HTTP TRACE method:
   *
   * <pre>
   *   trace("/", (req, rsp) {@literal ->} {
   *     rsp.send(...);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A callback to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition trace(final @Nonnull String path,
      final @Nonnull Route.Handler handler) {
    return handler(new Route.Definition("TRACE", path, handler));
  }

  /**
   * Append a new in-line route that supports HTTP TRACE method:
   *
   * <pre>
   *   trace("/", (req, rsp, chain) {@literal ->} {
   *     chain.next(req, rsp);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param filter A callback to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition trace(final @Nonnull String path,
      final @Nonnull Route.Filter filter) {
    return handler(new Route.Definition("TRACE", path, filter));
  }

  /**
   * Append a default trace implementation under the given path. Default trace response, looks
   * like:
   *
   * <pre>
   *  TRACE /path
   *     header1: value
   *     header2: value
   *
   * </pre>
   *
   * @param path A path pattern.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition trace(final @Nonnull String path) {
    return handler(new Route.Definition("TRACE", path, handler(TraceRouter.class))
        .name("*.trace"));
  }

  /**
   * Append a new in-line route that supports HTTP CONNECT method:
   *
   * <pre>
   *   connect("/", (req, rsp, chain) {@literal ->} {
   *     chain.next(req, rsp);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition connect(final @Nonnull String path,
      @Nonnull final Route.Handler handler) {
    return handler(new Route.Definition("CONNECT", path, handler));
  }

  /**
   * Append a new in-line route that supports HTTP CONNECT method:
   *
   * <pre>
   *   connect("/", (req, rsp, chain) {@literal ->} {
   *     chain.next(req, rsp);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param filter A filter to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition connect(final @Nonnull String path,
      final @Nonnull Route.Filter filter) {
    return handler(new Route.Definition("CONNECT", path, filter));
  }

  /**
   * Creates a new {@link Route.Handler} that delegate the execution to the given handler. This is
   * useful when the target handler requires some dependencies.
   *
   * <pre>
   *   public class MyHandler implements Route.Handler {
   *     &#64;Inject
   *     public MyHandler(Dependency d) {
   *     }
   *
   *     public void handle(Request req, Response rsp) throws Exception {
   *      // do something
   *     }
   *   }
   *   ...
   *   // external route
   *   get("/", handler(MyHandler.class));
   *
   *   // inline version route
   *   get("/", (req, rsp) {@literal ->} {
   *     Dependency d = req.getInstance(Dependency.class);
   *     // do something
   *   });
   * </pre>
   *
   * You can access to a dependency from a in-line route too, so the use of external route it is
   * more or less a matter of taste.
   *
   * @param handler The external handler class.
   * @return A new inline route handler.
   */
  public @Nonnull Route.Handler handler(final @Nonnull Class<? extends Route.Handler> handler) {
    requireNonNull(handler, "Route handler is required.");
    registerRouteScope(handler);
    return (req, rsp) -> req.getInstance(handler).handle(req, rsp);
  }

  /**
   * Creates a new {@link Route.Filter} that delegate the execution to the given filter. This is
   * useful when the target handler requires some dependencies.
   *
   * <pre>
   *   public class MyFilter implements Filter {
   *     &#64;Inject
   *     public MyFilter(Dependency d) {
   *     }
   *
   *     public void handle(Request req, Response rsp, Route.Chain chain) throws Exception {
   *      // do something
   *     }
   *   }
   *   ...
   *   // external filter
   *   get("/", filter(MyFilter.class));
   *
   *   // inline version route
   *   get("/", (req, rsp, chain) {@literal ->} {
   *     Dependency d = req.getInstance(Dependency.class);
   *     // do something
   *   });
   * </pre>
   *
   * You can access to a dependency from a in-line route too, so the use of external filter it is
   * more or less a matter of taste.
   *
   * @param filter The external filter class.
   * @return A new inline route.
   */
  public @Nonnull Route.Filter filter(final @Nonnull Class<? extends Route.Filter> filter) {
    requireNonNull(filter, "Filter is required.");
    registerRouteScope(filter);
    return (req, rsp, chain) -> req.getInstance(filter).handle(req, rsp, chain);
  }

  /**
   * Serve or publish static files to browser.
   *
   * <pre>
   *   assets("/assets/**");
   * </pre>
   *
   * Resources are served from root of classpath, for example <code>GET /assets/file.js</code> will
   * be resolve as classpath resource at the same location.
   *
   * @param path The path to publish.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition assets(final @Nonnull String path) {
    return get(path, handler(AssetRoute.class)).name("static files");
  }

  /**
   * <p>
   * Append one or more routes defined in the given class.
   * </p>
   *
   * <pre>
   *   use(MyRoute.class);
   *   ...
   *   // MyRoute.java
   *   {@literal @}Path("/")
   *   public class MyRoute {
   *
   *    {@literal @}GET
   *    public String hello() {
   *      return "Hello Jooby";
   *    }
   *   }
   * </pre>
   * <p>
   * Programming model is quite similar to JAX-RS/Jersey with some minor differences and/or
   * simplifications.
   * </p>
   *
   * <p>
   * To learn more about Mvc Routes, please check {@link org.jooby.mvc.Path},
   * {@link org.jooby.mvc.Produces} {@link org.jooby.mvc.Consumes} and
   * {@link org.jooby.mvc.Viewable}.
   * </p>
   *
   * @param routeClass A route(s) class.
   * @return This jooby instance.
   */
  public @Nonnull Jooby use(final @Nonnull Class<?> routeClass) {
    requireNonNull(routeClass, "Route class is required.");
    registerRouteScope(routeClass);
    bag.add(routeClass);
    return this;
  }

  /**
   * Redirect to the given url with status code defaulting to {@link Status#FOUND}.
   *
   * <pre>
   *  rsp.redirect("/foo/bar");
   *  rsp.redirect("http://example.com");
   *  rsp.redirect("http://example.com");
   *  rsp.redirect("../login");
   * </pre>
   *
   * Redirects can be a fully qualified URI for redirecting to a different site:
   *
   * <pre>
   *   rsp.redirect("http://google.com");
   * </pre>
   *
   * Redirects can be relative to the root of the host name. For example, if you were
   * on <code>http://example.com/admin/post/new</code>, the following redirect to /admin would
   * land you at <code>http://example.com/admin</code>:
   *
   * <pre>
   *   rsp.redirect("/admin");
   * </pre>
   *
   * Redirects can be relative to the current URL. A redirection of post/new, from
   * <code>http://example.com/blog/admin/</code> (notice the trailing slash), would give you
   * <code>http://example.com/blog/admin/post/new.</code>
   *
   * <pre>
   *   rsp.redirect("post/new");
   * </pre>
   *
   * Redirecting to post/new from <code>http://example.com/blog/admin</code> (no trailing slash),
   * will take you to <code>http://example.com/blog/post/new</code>.
   *
   * <p>
   * If you found the above behavior confusing, think of path segments as directories (have trailing
   * slashes) and files, it will start to make sense.
   * </p>
   *
   * Pathname relative redirects are also possible. If you were on
   * <code>http://example.com/admin/post/new</code>, the following redirect would land you at
   * <code>http//example.com/admin</code>:
   *
   * <pre>
   *   rsp.redirect("..");
   * </pre>
   *
   * A back redirection will redirect the request back to the <code>Referer</code>, defaulting to
   * <code>/</code> when missing.
   *
   * <pre>
   *   rsp.redirect("back");
   * </pre>
   *
   * @param location Either a relative or absolute location.
   * @return A route handler.
   */
  public Route.Handler redirect(final String location) {
    return redirect(Status.FOUND, location);
  }

  /**
   * Serve a single file from classpath.
   * Usage:
   *
   * <pre>
   *   {
   *     // serve the welcome.html from classpath root
   *     get("/", file("welcome.html");
   *   }
   * </pre>
   *
   * @param location Absolute classpath location.
   * @return A new route handler.
   */
  public Route.Handler file(@Nonnull final String location) {
    return staticFile(location);
  }

  /**
   * Serve a single file from classpath.
   * Usage:
   *
   * <pre>
   *   {
   *     // serve the welcome.html from classpath root
   *     get("/", file(MediaType.html, "welcome.html");
   *   }
   * </pre>
   *
   * @param location Absolute classpath location.
   * @return A new route handler.
   */
  private static Route.Handler staticFile(@Nonnull final String location) {
    requireNonNull(location, "A location is required.");
    String path = RoutePattern.normalize(location);
    return (req, rsp) -> {
      req.getInstance(AssetRoute.class).handle(new Request.Forwarding(req) {

        @Override
        public String path() {
          return path;
        }

        @Override
        public Route route() {
          return new Route.Forwarding(super.route()) {
            @Override
            public String path() {
              return path;
            }
          };
        }
      }, rsp);
    };
  }

  /**
   * Redirect to the given url with status code defaulting to {@link Status#FOUND}.
   *
   * <pre>
   *  rsp.redirect("/foo/bar");
   *  rsp.redirect("http://example.com");
   *  rsp.redirect("http://example.com");
   *  rsp.redirect("../login");
   * </pre>
   *
   * Redirects can be a fully qualified URI for redirecting to a different site:
   *
   * <pre>
   *   rsp.redirect("http://google.com");
   * </pre>
   *
   * Redirects can be relative to the root of the host name. For example, if you were
   * on <code>http://example.com/admin/post/new</code>, the following redirect to /admin would
   * land you at <code>http://example.com/admin</code>:
   *
   * <pre>
   *   rsp.redirect("/admin");
   * </pre>
   *
   * Redirects can be relative to the current URL. A redirection of post/new, from
   * <code>http://example.com/blog/admin/</code> (notice the trailing slash), would give you
   * <code>http://example.com/blog/admin/post/new.</code>
   *
   * <pre>
   *   rsp.redirect("post/new");
   * </pre>
   *
   * Redirecting to post/new from <code>http://example.com/blog/admin</code> (no trailing slash),
   * will take you to <code>http://example.com/blog/post/new</code>.
   *
   * <p>
   * If you found the above behavior confusing, think of path segments as directories (have trailing
   * slashes) and files, it will start to make sense.
   * </p>
   *
   * Pathname relative redirects are also possible. If you were on
   * <code>http://example.com/admin/post/new</code>, the following redirect would land you at
   * <code>http//example.com/admin</code>:
   *
   * <pre>
   *   rsp.redirect("..");
   * </pre>
   *
   * A back redirection will redirect the request back to the <code>Referer</code>, defaulting to
   * <code>/</code> when missing.
   *
   * <pre>
   *   rsp.redirect("back");
   * </pre>
   *
   * @param status A redirect status.
   * @param location Either a relative or absolute location.
   * @return A route handler.
   */
  public Route.Handler redirect(final Status status, final String location) {
    requireNonNull(location, "A location is required.");
    return (req, rsp) -> rsp.redirect(status, location);
  }

  /**
   * Check if the class had a Singleton annotation or not in order to register the route as
   * singleton or prototype.
   *
   * @param route
   */
  private void registerRouteScope(final Class<?> route) {
    if (route.getAnnotation(javax.inject.Singleton.class) == null) {
      protoRoutes.add(route);
    } else {
      singletonRoutes.add(route);
    }
  }

  /**
   * Keep track of routes in the order user define them.
   *
   * @param route A route definition to append.
   * @return The same route definition.
   */
  private Route.Definition handler(final Route.Definition route) {
    bag.add(route);
    return route;
  }

  /**
   * Register a application module.
   *
   * @param module The module to register.
   * @return This jooby instance.
   * @see Jooby.Module
   */
  public @Nonnull Jooby use(final @Nonnull Jooby.Module module) {
    requireNonNull(module, "A module is required.");
    modules.add(module);
    bag.add(module);
    return this;
  }

  /**
   * Register a request module.
   *
   * @param module The module to register.
   * @return This jooby instance.
   * @see Request.Module
   */
  public @Nonnull Jooby use(final @Nonnull Request.Module module) {
    requireNonNull(module, "A module is required.");
    bag.add(module);
    return this;
  }

  /**
   * Set the application configuration object. You must call this method when the default file
   * name: <code>application.conf</code> doesn't work for you or when you need/want to register two
   * or more files.
   *
   * @param config The application configuration object.
   * @return This jooby instance.
   * @see Config
   */
  public @Nonnull Jooby use(final @Nonnull Config config) {
    this.source = requireNonNull(config, "A config is required.");
    return this;
  }

  /**
   * Setup a route error handler. Default error handler {@link Err.Default} does content
   * negotation and this method allow to override/complement default handler.
   *
   * @param err A route error handler.
   * @return This jooby instance.
   */
  public @Nonnull Jooby err(final @Nonnull Err.Handler err) {
    this.err = requireNonNull(err, "An err handler is required.");
    return this;
  }

  /**
   * Append a new WebSocket handler under the given path.
   *
   * <pre>
   *   ws("/ws", (socket) {@literal ->} {
   *     // connected
   *     socket.onMessage(message {@literal ->} {
   *       System.out.println(message);
   *     });
   *     socket.send("Connected"):
   *   });
   * </pre>
   *
   * @param path A path pattern.
   * @param handler A connect callback.
   * @return A new WebSocket definition.
   */
  public @Nonnull WebSocket.Definition ws(final @Nonnull String path,
      final @Nonnull WebSocket.Handler handler) {
    WebSocket.Definition ws = new WebSocket.Definition(path, handler);
    checkArgument(bag.add(ws), "Path is in use: '%s'", path);
    return ws;
  }

  /**
   * <h1>Bootstrap</h1>
   * <p>
   * The bootstrap process is defined as follows:
   * </p>
   * <h2>1. Configuration files (first-listed are higher priority)</h2>
   * <ol>
   * <li>System properties</li>
   * <li>Application properties: {@code application.conf} or custom, see {@link #use(Config)}</li>
   * <li>{@link Jooby.Module Modules} properties</li>
   * </ol>
   *
   * <h2>2. Dependency Injection and {@link Jooby.Module modules}</h2>
   * <ol>
   * <li>An {@link Injector Guice Injector} is created.</li>
   * <li>It calls to {@link Jooby.Module#configure(Mode, Config, Binder)} for each module.</li>
   * <li>At this point Guice is ready and all the services has been binded.</li>
   * <li>It calls to {@link Jooby.Module#start() start method} for each module.</li>
   * <li>A web server is started</li>
   * </ol>
   *
   * @throws Exception If something fails to start.
   */
  public void start() throws Exception {
    start(new String[0]);
  }

  /**
   * <h1>Bootstrap</h1>
   * <p>
   * The bootstrap process is defined as follows:
   * </p>
   * <h2>1. Configuration files (first-listed are higher priority)</h2>
   * <ol>
   * <li>System properties</li>
   * <li>Application properties: {@code application.conf} or custom, see {@link #use(Config)}</li>
   * <li>{@link Jooby.Module Modules} properties</li>
   * </ol>
   *
   * <h2>2. Dependency Injection and {@link Jooby.Module modules}</h2>
   * <ol>
   * <li>An {@link Injector Guice Injector} is created.</li>
   * <li>It calls to {@link Jooby.Module#configure(Mode, Config, Binder)} for each module.</li>
   * <li>At this point Guice is ready and all the services has been binded.</li>
   * <li>It calls to {@link Jooby.Module#start() start method} for each module.</li>
   * <li>A web server is started</li>
   * </ol>
   *
   * @param args Application arguments.
   * @throws Exception If something fails to start.
   */
  public void start(final String[] args) throws Exception {
    Config config = buildConfig(
        Optional.ofNullable(this.source)
            .orElseGet(
                () -> ConfigFactory.parseResources("application.conf")
            )
        );
    Mode mode = mode(config.getString("application.mode").toLowerCase());

    // logback
    logback(config);

    // shutdown hook
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        stop();
      } catch (Exception ex) {
        LoggerFactory.getLogger(getClass()).error("Shutdown with error", ex);
      }
    }));

    final Charset charset = Charset.forName(config.getString("application.charset"));

    String[] lang = config.getString("application.lang").split("_");
    final Locale locale = lang.length == 1 ? new Locale(lang[0]) : new Locale(lang[0], lang[1]);

    ZoneId zoneId = ZoneId.of(config.getString("application.tz"));
    DateTimeFormatter dateTimeFormat = DateTimeFormatter
        .ofPattern(config.getString("application.dateFormat"), locale)
        .withZone(zoneId);

    DecimalFormat numberFormat = new DecimalFormat(config.getString("application.numberFormat"));

    // Guice Stage
    Stage stage = mode.when("dev", Stage.DEVELOPMENT).value().orElse(Stage.PRODUCTION);

    // dependency injection
    injector = Guice.createInjector(stage, new com.google.inject.Module() {
      @Override
      public void configure(final Binder binder) {

        TypeConverters.configure(binder);

        // bind config
        bindConfig(binder, config);

        // bind mode
        binder.bind(Mode.class).toInstance(mode);

        // bind charset
        binder.bind(Charset.class).toInstance(charset);

        // bind locale
        binder.bind(Locale.class).toInstance(locale);

        // bind time zone
        binder.bind(ZoneId.class).toInstance(zoneId);
        binder.bind(TimeZone.class).toInstance(TimeZone.getTimeZone(zoneId));

        // bind date format
        binder.bind(DateTimeFormatter.class).toInstance(dateTimeFormat);

        // bind number format
        binder.bind(NumberFormat.class).toInstance(numberFormat);
        binder.bind(DecimalFormat.class).toInstance(numberFormat);

        // bind formatter & parser
        Multibinder<Body.Parser> parserBinder = Multibinder
            .newSetBinder(binder, Body.Parser.class);
        Multibinder<Body.Formatter> formatterBinder = Multibinder
            .newSetBinder(binder, Body.Formatter.class);

        // session definition
        binder.bind(Session.Definition.class).toInstance(session);

        // Routes
        Multibinder<Route.Definition> definitions = Multibinder
            .newSetBinder(binder, Route.Definition.class);

        // Web Sockets
        Multibinder<WebSocket.Definition> sockets = Multibinder
            .newSetBinder(binder, WebSocket.Definition.class);

        // Request Modules
        Multibinder<Request.Module> requestModule = Multibinder
            .newSetBinder(binder, Request.Module.class);

        // bind prototype routes in request module
        requestModule.addBinding().toInstance(
            b -> protoRoutes.forEach(routeClass -> b.bind(routeClass)));

        // tmp dir
        binder.bind(File.class).annotatedWith(Names.named("java.io.tmpdir"))
            .toInstance(new File(config.getString("java.io.tmpdir")));
        binder.bind(File.class).annotatedWith(Names.named("tmpdir"))
            .toInstance(new File(config.getString("java.io.tmpdir")));

        // converters
        parsers.forEach(it -> parserBinder.addBinding().toInstance(it));
        formatters.forEach(it -> formatterBinder.addBinding().toInstance(it));

        // modules, routes and websockets
        bag.forEach(candidate -> {
          if (candidate instanceof Jooby.Module) {
            install((Jooby.Module) candidate, mode, config, binder);
          } else if (candidate instanceof Request.Module) {
            requestModule.addBinding().toInstance((Request.Module) candidate);
          } else if (candidate instanceof Route.Definition) {
            definitions.addBinding().toInstance((Route.Definition) candidate);
          } else if (candidate instanceof WebSocket.Definition) {
            sockets.addBinding().toInstance((WebSocket.Definition) candidate);
          } else {
            Class<?> routeClass = (Class<?>) candidate;
            Routes.routes(mode, routeClass)
                .forEach(route -> definitions.addBinding().toInstance(route));
          }
        });

        // Singleton routes
        singletonRoutes.forEach(routeClass -> binder.bind(routeClass).in(Scopes.SINGLETON));

        formatterBinder.addBinding().toInstance(FallbackBodyConverter.formatReader);
        formatterBinder.addBinding().toInstance(FallbackBodyConverter.formatStream);
        formatterBinder.addBinding().toInstance(FallbackBodyConverter.formatString);

        parserBinder.addBinding().toInstance(FallbackBodyConverter.parseString);

        // err
        if (err == null) {
          binder.bind(Err.Handler.class).toInstance(new Err.Default());
        } else {
          binder.bind(Err.Handler.class).toInstance(err);
        }
      }

    });

    // start modules
    for (Jooby.Module module : modules) {
      module.start();
    }

    // Start server
    Server server = injector.getInstance(Server.class);

    server.start();
  }

  /**
   * Stop the application, close all the modules and stop the web server.
   */
  public void stop() {
    // stop modules
    for (Jooby.Module module : modules) {
      try {
        module.stop();
      } catch (Exception ex) {
        LoggerFactory.getLogger(getClass()).warn(
            "Module didn't stop normally: " + module.getClass().getName(), ex);
      }
    }

    try {
      if (injector != null) {
        Server server = injector.getInstance(Server.class);
        server.stop();
      }
    } catch (Exception ex) {
      LoggerFactory.getLogger(getClass()).error("Web server didn't stop normally", ex);
    }
  }

  /**
   * Build configuration properties, it configure system, app and modules properties.
   *
   * @param source Source config to use.
   * @return A configuration properties ready to use.
   */
  private Config buildConfig(final Config source) {
    // system properties
    Config system = ConfigFactory.systemProperties()
        // file encoding got corrupted sometimes so we force and override.
        .withValue("file.encoding",
            ConfigValueFactory.fromAnyRef(System.getProperty("file.encoding")));

    // set module config
    Config moduleStack = ConfigFactory.empty();
    for (Jooby.Module module : ImmutableList.copyOf(modules).reverse()) {
      moduleStack = moduleStack.withFallback(module.config());
    }

    // add default config + mime types
    Config mime = ConfigFactory.parseResources(Jooby.class, "mime.properties");
    Config jooby = ConfigFactory.parseResources(Jooby.class, "jooby.conf");

    String mode = Arrays.asList(system, source, jooby).stream()
        .filter(it -> it.hasPath("application.mode"))
        .findFirst()
        .get()
        .getString("application.mode");

    String secret = Arrays
        .asList(system, source, jooby)
        .stream()
        .filter(it -> it.hasPath("application.secret"))
        .findFirst()
        .orElseGet(() ->
            ConfigFactory.empty()
                .withValue("application.secret", ConfigValueFactory.fromAnyRef(""))
        )
        .getString("application.secret");

    Config modeConfig = modeConfig(source, mode);

    // application.[mode].conf -> application.conf
    Config config = modeConfig.withFallback(source);

    return system
        .withFallback(config)
        .withFallback(moduleStack)
        .withFallback(defaultConfig(config, mode, secret))
        .withFallback(mime)
        .withFallback(jooby)
        .resolve();
  }

  /**
   * Build a mode config: <code>[application].[mode].[conf]</code>.
   * Stack looks like
   *
   * <pre>
   *   (file://[origin].[mode].[conf])?
   *   (cp://[origin].[mode].[conf])?
   *   file://application.[mode].[conf]
   *   /application.[mode].[conf]
   * </pre>
   *
   * @param source App source to use.
   * @param mode Application mode.
   * @return A config mode.
   */
  private Config modeConfig(final Config source, final String mode) {
    String origin = source.origin().resource();
    Config result = ConfigFactory.empty();
    if (origin != null) {
      // load [resource].[mode].[ext]
      int dot = origin.lastIndexOf('.');
      String originConf = origin.substring(0, dot) + "." + mode + origin.substring(dot);

      result = fileConfig(originConf).withFallback(ConfigFactory.parseResources(originConf));
    }
    String appConfig = "application." + mode + ".conf";
    return result
        .withFallback(fileConfig(appConfig))
        .withFallback(fileConfig("application.conf"))
        .withFallback(ConfigFactory.parseResources(appConfig));
  }

  /**
   * Config from file system.
   *
   * @param fname A file name.
   * @return A config for the file name.
   */
  private Config fileConfig(final String fname) {
    File in = new File(fname);
    return in.exists() ? ConfigFactory.parseFile(in) : ConfigFactory.empty();
  }

  /**
   * Build default application.* properties.
   *
   * @param config A source config.
   * @param mode Application mode.
   * @param secret Application secret.
   * @return default properties.
   */
  private Config defaultConfig(final Config config, final String mode, final String secret) {
    Map<String, Object> defaults = new LinkedHashMap<>();

    // set app name
    defaults.put("name", getClass().getSimpleName());

    // namespacce
    defaults.put("ns", getClass().getPackage().getName());

    // locale
    final Locale locale;
    if (!config.hasPath("application.lang")) {
      locale = Locale.getDefault();
      defaults.put("lang", locale.getLanguage() + "_" + locale.getCountry());
    } else {
      locale = Locale.forLanguageTag(config.getString("application.lang").replace("_", "-"));
    }

    // time zone
    if (!config.hasPath("application.tz")) {
      defaults.put("tz", ZoneId.systemDefault().getId());
    }

    // number format
    if (!config.hasPath("application.numberFormat")) {
      String pattern = ((DecimalFormat) DecimalFormat.getInstance(locale)).toPattern();
      defaults.put("numberFormat", pattern);
    }

    // last check app secret
    if (secret.length() == 0) {
      if ("dev".equalsIgnoreCase(mode)) {
        // it will survive between restarts and allow to have different apps running for
        // development.
        String devRandomSecret = getClass().getResource(getClass().getSimpleName() + ".class")
            .toString();
        defaults.put("secret", devRandomSecret);
      } else {
        throw new IllegalStateException("No application.secret has been defined");
      }
    }

    Map<String, Object> application = ImmutableMap.of("application", defaults);
    return defaults.size() == 0
        ? ConfigFactory.empty()
        : ConfigValueFactory.fromMap(application).toConfig();
  }

  /**
   * Install a {@link JoobyModule}.
   *
   * @param module The module to install.
   * @param mode Application mode.
   * @param config The configuration object.
   * @param binder A Guice binder.
   */
  private void install(final Jooby.Module module, final Mode mode, final Config config,
      final Binder binder) {
    try {
      module.configure(mode, config, binder);
    } catch (Exception ex) {
      throw new IllegalStateException("Error found on module: " + module.getClass().getName(), ex);
    }
  }

  /**
   * Bind a {@link Config} and make it available for injection. Each property of the config is also
   * binded it and ready to be injected with {@link javax.inject.Named}.
   *
   * @param binder
   * @param config
   */
  @SuppressWarnings("unchecked")
  private void bindConfig(final Binder root, final Config config) {
    Binder binder = root.skipSources(Names.class);
    for (Entry<String, ConfigValue> entry : config.entrySet()) {
      String name = entry.getKey();
      Named named = Names.named(name);
      Object value = entry.getValue().unwrapped();
      if (value instanceof List) {
        List<Object> values = (List<Object>) value;
        Type listType = Types.listOf(values.iterator().next().getClass());
        Key<Object> key = (Key<Object>) Key.get(listType, Names.named(name));
        binder.bind(key).toInstance(values);
      } else {
        @SuppressWarnings("rawtypes")
        Class type = value.getClass();
        binder.bind(type).annotatedWith(named).toInstance(value);
      }
    }
    // bind config
    binder.bind(Config.class).toInstance(config);
  }

  /**
   * Creates the application's mode.
   *
   * @param name A mode's name.
   * @return A new mode.
   */
  private static Mode mode(final String name) {
    return new Mode() {
      @Override
      public String name() {
        return name;
      }

      @Override
      public String toString() {
        return name;
      }
    };
  }

  private static void logback(final Config config) throws JoranException {
    String confFile = config.getString("logback.configurationFile");
    System.setProperty("logback.configurationFile", confFile);
  }

}
