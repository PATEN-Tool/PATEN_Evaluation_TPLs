// 
// Decompiled by Procyon v0.5.36
// 

package org.jooby.handlers;

import com.typesafe.config.ConfigValueFactory;
import com.typesafe.config.ConfigFactory;
import java.text.MessageFormat;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.Map;
import java.util.Objects;
import java.util.Date;
import org.jooby.Status;
import java.util.function.Function;
import java.net.URL;
import org.jooby.Err;
import org.jooby.Asset;
import org.jooby.internal.URLAsset;
import org.jooby.MediaType;
import org.jooby.Response;
import java.util.function.Consumer;
import org.jooby.funzy.Try;
import java.time.Duration;
import com.google.common.base.Strings;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.jooby.Request;
import org.jooby.funzy.Throwing;
import org.jooby.Route;

public class AssetHandler implements Route.Handler
{
    private static final Throwing.Function<String, String> prefix;
    private Throwing.Function2<Request, String, String> fn;
    private Loader loader;
    private String cdn;
    private boolean etag;
    private long maxAge;
    private boolean lastModified;
    private int statusCode;
    
    public AssetHandler(final String pattern, final ClassLoader loader) {
        this.etag = true;
        this.maxAge = -1L;
        this.lastModified = true;
        this.statusCode = 404;
        this.init(Route.normalize(pattern), Paths.get("public", new String[0]), loader);
    }
    
    public AssetHandler(final Path basedir) {
        this.etag = true;
        this.maxAge = -1L;
        this.lastModified = true;
        this.statusCode = 404;
        this.init("/{0}", basedir, this.getClass().getClassLoader());
    }
    
    public AssetHandler(final String pattern) {
        this.etag = true;
        this.maxAge = -1L;
        this.lastModified = true;
        this.statusCode = 404;
        this.init(Route.normalize(pattern), Paths.get("public", new String[0]), this.getClass().getClassLoader());
    }
    
    public AssetHandler etag(final boolean etag) {
        this.etag = etag;
        return this;
    }
    
    public AssetHandler lastModified(final boolean enabled) {
        this.lastModified = enabled;
        return this;
    }
    
    public AssetHandler cdn(final String cdn) {
        this.cdn = Strings.emptyToNull(cdn);
        return this;
    }
    
    public AssetHandler maxAge(final Duration maxAge) {
        return this.maxAge(maxAge.getSeconds());
    }
    
    public AssetHandler maxAge(final long maxAge) {
        this.maxAge = maxAge;
        return this;
    }
    
    public AssetHandler maxAge(final String maxAge) {
        Try.apply(() -> Long.parseLong(maxAge)).recover(x -> ConfigFactory.empty().withValue("v", ConfigValueFactory.fromAnyRef((Object)maxAge)).getDuration("v").getSeconds()).onSuccess((Consumer)this::maxAge);
        return this;
    }
    
    public AssetHandler onMissing(final int statusCode) {
        this.statusCode = statusCode;
        return this;
    }
    
    @Override
    public void handle(final Request req, final Response rsp) throws Throwable {
        final String path = req.path();
        final URL resource = this.resolve(req, path);
        if (resource != null) {
            String localpath = resource.getPath();
            final int jarEntry = localpath.indexOf("!/");
            if (jarEntry > 0) {
                localpath = localpath.substring(jarEntry + 2);
            }
            final URLAsset asset = new URLAsset(resource, path, MediaType.byPath(localpath).orElse(MediaType.octetstream));
            if (asset.exists()) {
                if (this.cdn != null) {
                    final String absUrl = this.cdn + path;
                    rsp.redirect(absUrl);
                    rsp.end();
                }
                else {
                    this.doHandle(req, rsp, asset);
                }
            }
        }
        else if (this.statusCode > 0) {
            throw new Err(this.statusCode);
        }
    }
    
    private void doHandle(final Request req, final Response rsp, final Asset asset) throws Throwable {
        if (this.etag) {
            final String etag = asset.etag();
            final boolean ifnm = req.header("If-None-Match").toOptional().map((Function<? super String, ? extends Boolean>)etag::equals).orElse(false);
            if (ifnm) {
                rsp.header("ETag", etag).status(Status.NOT_MODIFIED).end();
                return;
            }
            rsp.header("ETag", etag);
        }
        if (this.lastModified) {
            final long lastModified = asset.lastModified();
            if (lastModified > 0L) {
                final boolean ifm = req.header("If-Modified-Since").toOptional(Long.class).map(ifModified -> lastModified / 1000L <= ifModified / 1000L).orElse(false);
                if (ifm) {
                    rsp.status(Status.NOT_MODIFIED).end();
                    return;
                }
                rsp.header("Last-Modified", new Date(lastModified));
            }
        }
        if (this.maxAge > 0L) {
            rsp.header("Cache-Control", "max-age=" + this.maxAge);
        }
        this.send(req, rsp, asset);
    }
    
    protected void send(final Request req, final Response rsp, final Asset asset) throws Throwable {
        rsp.send(asset);
    }
    
    private URL resolve(final Request req, final String path) throws Throwable {
        final String target = (String)this.fn.apply((Object)req, (Object)path);
        return this.resolve(target);
    }
    
    protected URL resolve(final String path) throws Exception {
        return this.loader.getResource(path);
    }
    
    private void init(final String pattern, final Path basedir, final ClassLoader loader) {
        Objects.requireNonNull(loader, "Resource loader is required.");
        this.fn = (Throwing.Function2<Request, String, String>)(pattern.equals("/") ? ((req, p) -> (String)AssetHandler.prefix.apply((Object)p)) : ((req, p) -> MessageFormat.format((String)AssetHandler.prefix.apply((Object)pattern), vars(req))));
        this.loader = loader(basedir, loader);
    }
    
    private static Object[] vars(final Request req) {
        final Map<Object, String> vars = req.route().vars();
        return vars.values().toArray(new Object[vars.size()]);
    }
    
    private static Loader loader(final Path basedir, final ClassLoader classloader) {
        if (Files.exists(basedir, new LinkOption[0])) {
            final Path path;
            return name -> {
                path = basedir.resolve(name).normalize();
                if (Files.exists(path, new LinkOption[0]) && path.startsWith(basedir)) {
                    try {
                        return path.toUri().toURL();
                    }
                    catch (MalformedURLException ex) {}
                }
                return classloader.getResource(name);
            };
        }
        return classloader::getResource;
    }
    
    private static Throwing.Function<String, String> prefix() {
        return (Throwing.Function<String, String>)(p -> p.substring(1));
    }
    
    static {
        prefix = prefix().memoized();
    }
    
    private interface Loader
    {
        URL getResource(final String name);
    }
}
