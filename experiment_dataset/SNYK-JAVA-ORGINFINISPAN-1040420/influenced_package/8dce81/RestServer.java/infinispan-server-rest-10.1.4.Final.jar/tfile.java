// 
// Decompiled by Procyon v0.5.36
// 

package org.infinispan.rest;

import java.nio.file.Path;
import org.infinispan.rest.framework.ResourceManager;
import org.infinispan.rest.framework.impl.RestDispatcherImpl;
import org.infinispan.rest.resources.LoginResource;
import org.infinispan.rest.resources.ClusterResource;
import org.infinispan.rest.resources.ServerResource;
import org.infinispan.rest.resources.RedirectResource;
import org.infinispan.rest.resources.StaticContentResource;
import org.infinispan.rest.resources.MetricsResource;
import org.infinispan.rest.resources.TasksResource;
import org.infinispan.rest.resources.SearchAdminResource;
import org.infinispan.rest.resources.XSiteResource;
import org.infinispan.rest.resources.CacheManagerResource;
import org.infinispan.rest.resources.CounterResource;
import org.infinispan.rest.resources.CacheResourceV2;
import org.infinispan.rest.framework.ResourceHandler;
import org.infinispan.rest.resources.CacheResource;
import org.infinispan.rest.framework.impl.ResourceManagerImpl;
import java.util.concurrent.Executor;
import org.infinispan.counter.EmbeddedCounterManagerFactory;
import org.infinispan.counter.impl.manager.EmbeddedCounterManager;
import java.util.function.Predicate;
import org.infinispan.rest.configuration.AuthenticationConfiguration;
import java.io.IOException;
import org.infinispan.server.core.transport.NettyInitializers;
import org.infinispan.server.core.transport.NettyInitializer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelOutboundHandler;
import org.infinispan.rest.cachemanager.RestCacheManager;
import org.infinispan.rest.framework.RestDispatcher;
import org.infinispan.server.core.ServerManagement;
import org.infinispan.rest.configuration.RestServerConfiguration;
import org.infinispan.server.core.AbstractProtocolServer;

public class RestServer extends AbstractProtocolServer<RestServerConfiguration>
{
    private ServerManagement server;
    private RestDispatcher restDispatcher;
    private RestCacheManager<Object> restCacheManager;
    private InvocationHelper invocationHelper;
    
    public RestServer() {
        super("REST");
    }
    
    public ChannelOutboundHandler getEncoder() {
        return null;
    }
    
    public ChannelInboundHandler getDecoder() {
        return null;
    }
    
    public ChannelInitializer<Channel> getInitializer() {
        return (ChannelInitializer<Channel>)new NettyInitializers(new NettyInitializer[] { (NettyInitializer)this.getRestChannelInitializer() });
    }
    
    public RestChannelInitializer getRestChannelInitializer() {
        return new RestChannelInitializer(this, this.transport);
    }
    
    RestDispatcher getRestDispatcher() {
        return this.restDispatcher;
    }
    
    public void stop() {
        if (this.restCacheManager != null) {
            this.restCacheManager.stop();
        }
        final AuthenticationConfiguration auth = ((RestServerConfiguration)this.configuration).authentication();
        if (auth.enabled()) {
            try {
                auth.authenticator().close();
            }
            catch (IOException ex) {}
        }
        super.stop();
    }
    
    public void setServer(final ServerManagement server) {
        this.server = server;
    }
    
    protected void startInternal() {
        final AuthenticationConfiguration auth = ((RestServerConfiguration)this.configuration).authentication();
        if (auth.enabled()) {
            auth.authenticator().init(this);
        }
        super.startInternal();
        this.restCacheManager = new RestCacheManager<Object>(this.cacheManager, this::isCacheIgnored);
        this.invocationHelper = new InvocationHelper(this.restCacheManager, (EmbeddedCounterManager)EmbeddedCounterManagerFactory.asCounterManager(this.cacheManager), (RestServerConfiguration)this.configuration, this.server, this.getExecutor());
        final String restContext = ((RestServerConfiguration)this.configuration).contextPath();
        final String rootContext = "/";
        final ResourceManager resourceManager = new ResourceManagerImpl();
        resourceManager.registerResource(restContext, new CacheResource(this.invocationHelper));
        resourceManager.registerResource(restContext, new CacheResourceV2(this.invocationHelper));
        resourceManager.registerResource(restContext, new CounterResource(this.invocationHelper));
        resourceManager.registerResource(restContext, new CacheManagerResource(this.invocationHelper));
        resourceManager.registerResource(restContext, new XSiteResource(this.invocationHelper));
        resourceManager.registerResource(restContext, new SearchAdminResource(this.invocationHelper));
        resourceManager.registerResource(restContext, new TasksResource(this.invocationHelper));
        resourceManager.registerResource(rootContext, new MetricsResource());
        final Path staticResources = ((RestServerConfiguration)this.configuration).staticResources();
        if (staticResources != null) {
            final Path console = ((RestServerConfiguration)this.configuration).staticResources().resolve("console");
            resourceManager.registerResource(rootContext, new StaticContentResource(staticResources, "static"));
            resourceManager.registerResource(rootContext, new StaticContentResource(console, "console"));
            resourceManager.registerResource(rootContext, new RedirectResource(rootContext, rootContext + "console/welcome", true));
        }
        if (this.server != null) {
            resourceManager.registerResource(restContext, new ServerResource(this.invocationHelper));
            resourceManager.registerResource(restContext, new ClusterResource(this.invocationHelper));
            resourceManager.registerResource(restContext, new LoginResource(this.invocationHelper, rootContext + "console/", rootContext + "console/forbidden.html"));
        }
        this.restDispatcher = new RestDispatcherImpl(resourceManager);
    }
}
