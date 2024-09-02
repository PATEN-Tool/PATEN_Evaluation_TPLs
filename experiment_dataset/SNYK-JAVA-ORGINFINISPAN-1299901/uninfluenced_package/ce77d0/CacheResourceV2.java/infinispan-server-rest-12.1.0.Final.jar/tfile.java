// 
// Decompiled by Procyon v0.5.36
// 

package org.infinispan.rest.resources;

import org.infinispan.commons.dataconversion.internal.JsonSerialization;
import org.infinispan.rest.RestResponseException;
import java.util.Collection;
import org.infinispan.commons.dataconversion.internal.Json;
import org.infinispan.configuration.global.GlobalConfiguration;
import java.util.Collections;
import java.io.OutputStream;
import org.infinispan.commons.configuration.io.ConfigurationWriter;
import java.io.ByteArrayOutputStream;
import org.infinispan.query.core.stats.IndexStatistics;
import org.infinispan.query.core.stats.SearchStatistics;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.query.Search;
import org.infinispan.AdvancedCache;
import org.infinispan.stats.Stats;
import org.infinispan.rest.framework.ContentSource;
import org.infinispan.commons.api.CacheContainerAdmin;
import java.nio.charset.StandardCharsets;
import org.infinispan.commons.configuration.ConfigurationBuilderInfo;
import org.infinispan.commons.dataconversion.StandardConversions;
import java.util.EnumSet;
import org.infinispan.manager.EmbeddedCacheManagerAdmin;
import java.util.List;
import org.infinispan.rest.cachemanager.RestCacheManager;
import java.util.Map;
import org.infinispan.rest.CacheEntryInputStream;
import org.infinispan.CacheStream;
import org.infinispan.rest.CacheKeyInputStream;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ParserRegistry;
import org.infinispan.commons.configuration.ConfigurationInfo;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.commons.util.Util;
import org.infinispan.commons.util.ProcessorInfo;
import org.infinispan.rest.logging.Log;
import org.infinispan.Cache;
import java.util.concurrent.CompletableFuture;
import org.infinispan.upgrade.RollingUpgradeManager;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.infinispan.rest.NettyRestResponse;
import org.infinispan.rest.framework.impl.InvocationImpl;
import java.util.Objects;
import org.infinispan.security.AuthorizationPermission;
import org.infinispan.rest.framework.RestResponse;
import java.util.concurrent.CompletionStage;
import org.infinispan.rest.framework.RestRequest;
import java.util.function.Function;
import org.infinispan.rest.framework.Method;
import org.infinispan.rest.framework.impl.Invocations;
import org.infinispan.rest.InvocationHelper;
import org.infinispan.rest.framework.ResourceHandler;

public class CacheResourceV2 extends BaseCacheResource implements ResourceHandler
{
    private static final int STREAM_BATCH_SIZE = 1000;
    
    public CacheResourceV2(final InvocationHelper invocationHelper) {
        super(invocationHelper);
    }
    
    @Override
    public Invocations getInvocations() {
        final InvocationImpl.Builder permission = new Invocations.Builder().invocation().methods(Method.PUT, Method.POST).path("/v2/caches/{cacheName}/{cacheKey}").handleWith(this::putValueToCache).invocation().methods(Method.GET, Method.HEAD).path("/v2/caches/{cacheName}/{cacheKey}").handleWith(this::getCacheValue).invocation().method(Method.DELETE).path("/v2/caches/{cacheName}/{cacheKey}").handleWith(this::deleteCacheValue).invocation().methods(Method.GET).path("/v2/caches/{cacheName}").withAction("keys").handleWith(this::streamKeys).invocation().methods(Method.GET).path("/v2/caches/{cacheName}").withAction("entries").handleWith(this::streamEntries).invocation().methods(Method.GET, Method.HEAD).path("/v2/caches/{cacheName}").withAction("config").handleWith(this::getCacheConfig).invocation().methods(Method.GET).path("/v2/caches/{cacheName}").withAction("stats").handleWith(this::getCacheStats).invocation().methods(Method.GET).path("/v2/caches/").handleWith(this::getCacheNames).invocation().methods(Method.POST).path("/v2/caches/{cacheName}").handleWith((Function<RestRequest, CompletionStage<RestResponse>>)this::createCache).invocation().method(Method.DELETE).path("/v2/caches/{cacheName}").handleWith(this::removeCache).invocation().method(Method.HEAD).path("/v2/caches/{cacheName}").handleWith(this::cacheExists).invocation().methods(Method.POST).path("/v2/caches/{cacheName}").withAction("clear").handleWith(this::clearEntireCache).invocation().methods(Method.GET).path("/v2/caches/{cacheName}").withAction("size").handleWith(this::getSize).invocation().methods(Method.POST).path("/v2/caches/{cacheName}").withAction("sync-data").handleWith(this::syncData).invocation().methods(Method.POST).path("/v2/caches/{cacheName}").withAction("disconnect-source").handleWith(this::disconnectSource).invocation().methods(Method.GET, Method.POST).path("/v2/caches/{cacheName}").withAction("search").permission(AuthorizationPermission.BULK_READ);
        final CacheResourceQueryAction queryAction = this.queryAction;
        Objects.requireNonNull(queryAction);
        return permission.handleWith((Function<RestRequest, CompletionStage<RestResponse>>)queryAction::search).invocation().methods(Method.POST).path("/v2/caches").withAction("toJSON").handleWith(this::convertToJson).invocation().methods(Method.GET).path("/v2/caches/{cacheName}").handleWith(this::getAllDetails).create();
    }
    
    private CompletionStage<RestResponse> disconnectSource(final RestRequest request) {
        final NettyRestResponse.Builder builder = new NettyRestResponse.Builder();
        builder.status(HttpResponseStatus.NO_CONTENT);
        final String cacheName = request.variables().get("cacheName");
        final Cache<?, ?> cache = (Cache<?, ?>)this.invocationHelper.getRestCacheManager().getCache(cacheName, request);
        final RollingUpgradeManager upgradeManager = (RollingUpgradeManager)cache.getAdvancedCache().getComponentRegistry().getComponent((Class)RollingUpgradeManager.class);
        try {
            upgradeManager.disconnectSource("hotrod");
        }
        catch (Exception e) {
            builder.status(HttpResponseStatus.INTERNAL_SERVER_ERROR).entity((Object)e.getMessage());
        }
        return (CompletionStage<RestResponse>)CompletableFuture.completedFuture(builder.build());
    }
    
    private CompletionStage<RestResponse> syncData(final RestRequest request) {
        final NettyRestResponse.Builder builder = new NettyRestResponse.Builder();
        final String cacheName = request.variables().get("cacheName");
        final String readBatchReq = request.getParameter("read-batch");
        final String threadsReq = request.getParameter("threads");
        final int readBatch = (readBatchReq == null) ? 10000 : Integer.parseInt(readBatchReq);
        if (readBatch < 1) {
            return (CompletionStage<RestResponse>)CompletableFuture.completedFuture(builder.status(HttpResponseStatus.BAD_REQUEST).entity((Object)Log.REST.illegalArgument("read-batch", readBatch).getMessage()).build());
        }
        final int threads = (request.getParameter("threads") == null) ? ProcessorInfo.availableProcessors() : Integer.parseInt(threadsReq);
        if (threads < 1) {
            return (CompletionStage<RestResponse>)CompletableFuture.completedFuture(builder.status(HttpResponseStatus.BAD_REQUEST).entity((Object)Log.REST.illegalArgument("threads", threads).getMessage()).build());
        }
        final Cache<?, ?> cache = (Cache<?, ?>)this.invocationHelper.getRestCacheManager().getCache(cacheName, request);
        final RollingUpgradeManager upgradeManager = (RollingUpgradeManager)cache.getAdvancedCache().getComponentRegistry().getComponent((Class)RollingUpgradeManager.class);
        final RollingUpgradeManager rollingUpgradeManager;
        final int n;
        final int n2;
        long hotrod;
        final NettyRestResponse.Builder builder2;
        Throwable rootCause;
        return (CompletionStage<RestResponse>)CompletableFuture.supplyAsync(() -> {
            try {
                hotrod = rollingUpgradeManager.synchronizeData("hotrod", n, n2);
                builder2.entity((Object)Log.REST.synchronizedEntries(hotrod));
            }
            catch (Exception e) {
                rootCause = Util.getRootCause((Throwable)e);
                builder2.status(HttpResponseStatus.INTERNAL_SERVER_ERROR).entity((Object)rootCause.getMessage());
            }
            return builder2.build();
        }, this.invocationHelper.getExecutor());
    }
    
    private CompletionStage<RestResponse> convertToJson(final RestRequest restRequest) {
        final NettyRestResponse.Builder responseBuilder = new NettyRestResponse.Builder();
        final String contents = restRequest.contents().asString();
        if (contents == null || contents.isEmpty()) {
            responseBuilder.status(HttpResponseStatus.BAD_REQUEST);
            return (CompletionStage<RestResponse>)CompletableFuture.completedFuture(responseBuilder.build());
        }
        final ParserRegistry parserRegistry = this.invocationHelper.getParserRegistry();
        final ConfigurationBuilderHolder builderHolder = parserRegistry.parse(contents);
        final ConfigurationBuilder builder = builderHolder.getNamedConfigurationBuilders().values().iterator().next();
        final Configuration configuration = builder.build();
        responseBuilder.contentType(MediaType.APPLICATION_JSON).entity((Object)this.invocationHelper.getJsonWriter().toJSON((ConfigurationInfo)configuration));
        return (CompletionStage<RestResponse>)CompletableFuture.completedFuture(responseBuilder.build());
    }
    
    private CompletionStage<RestResponse> streamKeys(final RestRequest request) {
        final String cacheName = request.variables().get("cacheName");
        final String batchParam = request.getParameter("batch");
        final String limitParam = request.getParameter("limit");
        final int batch = (batchParam == null || batchParam.isEmpty()) ? 1000 : Integer.parseInt(batchParam);
        final int limit = (limitParam == null || limitParam.isEmpty()) ? -1 : Integer.parseInt(limitParam);
        final Cache<?, ?> cache = (Cache<?, ?>)this.invocationHelper.getRestCacheManager().getCache(cacheName, MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON, request);
        if (cache == null) {
            return ResourceUtil.notFoundResponseFuture();
        }
        final NettyRestResponse.Builder responseBuilder;
        final Cache cache2;
        CacheStream<?> stream;
        final int n;
        final int batchSize;
        return (CompletionStage<RestResponse>)CompletableFuture.supplyAsync(() -> {
            responseBuilder = new NettyRestResponse.Builder();
            stream = (CacheStream<?>)cache2.keySet().stream();
            if (n > -1) {
                stream = (CacheStream<?>)stream.limit((long)n);
            }
            responseBuilder.entity((Object)new CacheKeyInputStream(stream, batchSize));
            responseBuilder.contentType("application/json");
            return responseBuilder.build();
        }, this.invocationHelper.getExecutor());
    }
    
    private CompletionStage<RestResponse> streamEntries(final RestRequest request) {
        final String cacheName = request.variables().get("cacheName");
        final String limitParam = request.getParameter("limit");
        final String metadataParam = request.getParameter("metadata");
        final String batchParam = request.getParameter("batch");
        final int limit = (limitParam == null) ? -1 : Integer.parseInt(limitParam);
        final boolean metadata = metadataParam != null && Boolean.parseBoolean(metadataParam);
        final int batch = (batchParam == null) ? 1000 : Integer.parseInt(batchParam);
        final Cache<?, ?> cache = (Cache<?, ?>)this.invocationHelper.getRestCacheManager().getCache(cacheName, MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON, request);
        if (cache == null) {
            return ResourceUtil.notFoundResponseFuture();
        }
        final NettyRestResponse.Builder responseBuilder;
        final Cache cache2;
        CacheStream<? extends Map.Entry<?, ?>> stream;
        final int n;
        final int batchSize;
        final boolean includeMetadata;
        return (CompletionStage<RestResponse>)CompletableFuture.supplyAsync(() -> {
            responseBuilder = new NettyRestResponse.Builder();
            stream = (CacheStream<? extends Map.Entry<?, ?>>)cache2.entrySet().stream();
            if (n > -1) {
                stream = (CacheStream<? extends Map.Entry<?, ?>>)stream.limit((long)n);
            }
            responseBuilder.entity((Object)new CacheEntryInputStream(stream, batchSize, includeMetadata));
            responseBuilder.contentType("application/json");
            return responseBuilder.build();
        }, this.invocationHelper.getExecutor());
    }
    
    private CompletionStage<RestResponse> removeCache(final RestRequest request) {
        final String cacheName = request.variables().get("cacheName");
        final RestCacheManager<Object> restCacheManager = this.invocationHelper.getRestCacheManager();
        if (!restCacheManager.cacheExists(cacheName)) {
            return ResourceUtil.notFoundResponseFuture();
        }
        return (CompletionStage<RestResponse>)CompletableFuture.supplyAsync(() -> {
            restCacheManager.getCacheManagerAdmin(request).removeCache(cacheName);
            return new NettyRestResponse.Builder().status(HttpResponseStatus.OK).build();
        }, this.invocationHelper.getExecutor());
    }
    
    private CompletionStage<RestResponse> cacheExists(final RestRequest restRequest) {
        final NettyRestResponse.Builder responseBuilder = new NettyRestResponse.Builder();
        final String cacheName = restRequest.variables().get("cacheName");
        if (!this.invocationHelper.getRestCacheManager().getInstance().getCacheConfigurationNames().contains(cacheName)) {
            responseBuilder.status(HttpResponseStatus.NOT_FOUND);
        }
        else {
            responseBuilder.status(HttpResponseStatus.NO_CONTENT);
        }
        return (CompletionStage<RestResponse>)CompletableFuture.completedFuture(responseBuilder.build());
    }
    
    private CompletableFuture<RestResponse> createCache(final RestRequest request) {
        final NettyRestResponse.Builder responseBuilder = new NettyRestResponse.Builder();
        final List<String> template = request.parameters().get("template");
        final String cacheName = request.variables().get("cacheName");
        final EnumSet<CacheContainerAdmin.AdminFlag> adminFlags = request.getAdminFlags();
        final EmbeddedCacheManagerAdmin initialAdmin = this.invocationHelper.getRestCacheManager().getCacheManagerAdmin(request);
        final EmbeddedCacheManagerAdmin administration = (EmbeddedCacheManagerAdmin)((adminFlags == null) ? initialAdmin : initialAdmin.withFlags((EnumSet)adminFlags));
        if (template != null && !template.isEmpty()) {
            final String templateName = template.iterator().next();
            final EmbeddedCacheManagerAdmin embeddedCacheManagerAdmin;
            final String s;
            final String s2;
            final NettyRestResponse.Builder builder;
            return (CompletableFuture<RestResponse>)CompletableFuture.supplyAsync(() -> {
                embeddedCacheManagerAdmin.createCache(s, s2);
                builder.status(HttpResponseStatus.OK);
                return builder.build();
            }, this.invocationHelper.getExecutor());
        }
        final ContentSource contents = request.contents();
        final byte[] bytes = contents.rawContent();
        if (bytes == null || bytes.length == 0) {
            final EmbeddedCacheManagerAdmin embeddedCacheManagerAdmin2;
            final String s3;
            final NettyRestResponse.Builder builder2;
            return (CompletableFuture<RestResponse>)CompletableFuture.supplyAsync(() -> {
                embeddedCacheManagerAdmin2.createCache(s3, (String)null);
                builder2.status(HttpResponseStatus.OK);
                return builder2.build();
            }, this.invocationHelper.getExecutor());
        }
        ConfigurationBuilder cfgBuilder = new ConfigurationBuilder();
        final MediaType sourceType = (request.contentType() == null) ? MediaType.APPLICATION_JSON : request.contentType();
        if (sourceType.match(MediaType.APPLICATION_JSON)) {
            this.invocationHelper.getJsonReader().readJson((ConfigurationBuilderInfo)cfgBuilder, StandardConversions.convertTextToObject((Object)bytes, sourceType));
        }
        else {
            if (!sourceType.match(MediaType.APPLICATION_XML)) {
                responseBuilder.status(HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE);
                return (CompletableFuture<RestResponse>)CompletableFuture.completedFuture(responseBuilder.build());
            }
            final ConfigurationBuilderHolder builderHolder = this.invocationHelper.getParserRegistry().parse(new String(bytes, StandardCharsets.UTF_8));
            cfgBuilder = builderHolder.getCurrentConfigurationBuilder();
        }
        final ConfigurationBuilder finalCfgBuilder = cfgBuilder;
        final EmbeddedCacheManagerAdmin embeddedCacheManagerAdmin3;
        final String s4;
        final ConfigurationBuilder configurationBuilder;
        final NettyRestResponse.Builder builder3;
        return (CompletableFuture<RestResponse>)CompletableFuture.supplyAsync(() -> {
            try {
                embeddedCacheManagerAdmin3.createCache(s4, configurationBuilder.build());
                builder3.status(HttpResponseStatus.OK);
            }
            catch (Throwable t) {
                builder3.status(HttpResponseStatus.BAD_REQUEST).entity((Object)t.getMessage());
            }
            return builder3.build();
        }, this.invocationHelper.getExecutor());
    }
    
    private CompletionStage<RestResponse> getCacheStats(final RestRequest request) {
        final String cacheName = request.variables().get("cacheName");
        final Cache<?, ?> cache = (Cache<?, ?>)this.invocationHelper.getRestCacheManager().getCache(cacheName, request);
        final Stats stats = cache.getAdvancedCache().getStats();
        return ResourceUtil.asJsonResponseFuture(stats.toJson());
    }
    
    private CompletionStage<RestResponse> getAllDetails(final RestRequest request) {
        final String cacheName = request.variables().get("cacheName");
        final Cache<?, ?> cache = (Cache<?, ?>)this.invocationHelper.getRestCacheManager().getCache(cacheName, request);
        if (cache == null) {
            return ResourceUtil.notFoundResponseFuture();
        }
        return CompletableFuture.supplyAsync(() -> this.getDetailResponse(cache), this.invocationHelper.getExecutor());
    }
    
    private RestResponse getDetailResponse(final Cache<?, ?> cache) {
        final Configuration configuration = SecurityActions.getCacheConfiguration((AdvancedCache<?, ?>)cache.getAdvancedCache());
        Stats stats = null;
        Boolean rehashInProgress = null;
        Boolean indexingInProgress = null;
        Boolean queryable = null;
        try {
            stats = cache.getAdvancedCache().getStats();
            final DistributionManager distributionManager = cache.getAdvancedCache().getDistributionManager();
            rehashInProgress = (distributionManager != null && distributionManager.isRehashInProgress());
        }
        catch (SecurityException ex) {}
        Integer size = null;
        try {
            size = cache.size();
        }
        catch (SecurityException ex2) {}
        final SearchStatistics searchStatistics = Search.getSearchStatistics((Cache)cache);
        final IndexStatistics indexStatistics = searchStatistics.getIndexStatistics();
        indexingInProgress = indexStatistics.reindexing();
        queryable = this.invocationHelper.getRestCacheManager().isCacheQueryable(cache);
        final boolean statistics = configuration.statistics().enabled();
        final boolean indexed = configuration.indexing().enabled();
        final CacheFullDetail fullDetail = new CacheFullDetail();
        fullDetail.stats = stats;
        fullDetail.configuration = this.invocationHelper.getJsonWriter().toJSON((ConfigurationInfo)configuration);
        fullDetail.size = size;
        fullDetail.rehashInProgress = rehashInProgress;
        fullDetail.indexingInProgress = indexingInProgress;
        fullDetail.persistent = configuration.persistence().usingStores();
        fullDetail.bounded = configuration.memory().whenFull().isEnabled();
        fullDetail.indexed = indexed;
        fullDetail.hasRemoteBackup = configuration.sites().hasEnabledBackups();
        fullDetail.secured = configuration.security().authorization().enabled();
        fullDetail.transactional = configuration.transaction().transactionMode().isTransactional();
        fullDetail.statistics = statistics;
        fullDetail.queryable = queryable;
        return ResourceUtil.addEntityAsJson(fullDetail.toJson(), new NettyRestResponse.Builder()).build();
    }
    
    private CompletionStage<RestResponse> getCacheConfig(final RestRequest request) {
        final NettyRestResponse.Builder responseBuilder = new NettyRestResponse.Builder();
        final String cacheName = request.variables().get("cacheName");
        final MediaType accept = MediaTypeUtils.negotiateMediaType(request, MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.APPLICATION_YAML);
        responseBuilder.contentType(accept);
        if (!this.invocationHelper.getRestCacheManager().getInstance().getCacheConfigurationNames().contains(cacheName)) {
            responseBuilder.status(HttpResponseStatus.NOT_FOUND).build();
        }
        final Cache<?, ?> cache = (Cache<?, ?>)this.invocationHelper.getRestCacheManager().getCache(cacheName, request);
        if (cache == null) {
            return ResourceUtil.notFoundResponseFuture();
        }
        final Configuration cacheConfiguration = SecurityActions.getCacheConfiguration((AdvancedCache<?, ?>)cache.getAdvancedCache());
        final ParserRegistry registry = new ParserRegistry();
        final String typeSubtype = accept.getTypeSubtype();
        switch (typeSubtype) {
            case "application/json": {
                responseBuilder.entity((Object)this.invocationHelper.getJsonWriter().toJSON((ConfigurationInfo)cacheConfiguration));
                break;
            }
            default: {
                final ByteArrayOutputStream entity = new ByteArrayOutputStream();
                try {
                    final ConfigurationWriter writer = ConfigurationWriter.to((OutputStream)entity).withType(accept).build();
                    try {
                        registry.serialize(writer, (GlobalConfiguration)null, (Map)Collections.singletonMap(cacheName, cacheConfiguration));
                        if (writer != null) {
                            writer.close();
                        }
                    }
                    catch (Throwable t) {
                        if (writer != null) {
                            try {
                                writer.close();
                            }
                            catch (Throwable t2) {
                                t.addSuppressed(t2);
                            }
                        }
                        throw t;
                    }
                }
                catch (Exception e) {
                    return (CompletionStage<RestResponse>)CompletableFuture.completedFuture(responseBuilder.status(HttpResponseStatus.INTERNAL_SERVER_ERROR).entity((Object)Util.getRootCause((Throwable)e)).build());
                }
                responseBuilder.entity((Object)entity);
                break;
            }
        }
        return (CompletionStage<RestResponse>)CompletableFuture.completedFuture(responseBuilder.status(HttpResponseStatus.OK).build());
    }
    
    private CompletionStage<RestResponse> getSize(final RestRequest request) {
        final String cacheName = request.variables().get("cacheName");
        final AdvancedCache<Object, Object> cache = this.invocationHelper.getRestCacheManager().getCache(cacheName, request);
        return (CompletionStage<RestResponse>)cache.sizeAsync().thenApply(size -> ResourceUtil.asJsonResponse(Json.make((Object)size)));
    }
    
    private CompletionStage<RestResponse> getCacheNames(final RestRequest request) throws RestResponseException {
        final Collection<String> cacheNames = this.invocationHelper.getRestCacheManager().getCacheNames();
        return ResourceUtil.asJsonResponseFuture(Json.make((Object)cacheNames));
    }
    
    private static class CacheFullDetail implements JsonSerialization
    {
        public Stats stats;
        public Integer size;
        public String configuration;
        public Boolean rehashInProgress;
        public boolean bounded;
        public boolean indexed;
        public boolean persistent;
        public boolean transactional;
        public boolean secured;
        public boolean hasRemoteBackup;
        public Boolean indexingInProgress;
        public boolean statistics;
        public Boolean queryable;
        
        public Json toJson() {
            final Json json = Json.object();
            if (this.stats != null) {
                json.set("stats", this.stats.toJson());
            }
            if (this.size != null) {
                json.set("size", (Object)this.size);
            }
            if (this.rehashInProgress != null) {
                json.set("rehash_in_progress", (Object)this.rehashInProgress);
            }
            if (this.indexingInProgress != null) {
                json.set("indexing_in_progress", (Object)this.indexingInProgress);
            }
            if (this.queryable != null) {
                json.set("queryable", (Object)this.queryable);
            }
            return json.set("configuration", Json.factory().raw(this.configuration)).set("bounded", (Object)this.bounded).set("indexed", (Object)this.indexed).set("persistent", (Object)this.persistent).set("transactional", (Object)this.transactional).set("secured", (Object)this.secured).set("has_remote_backup", (Object)this.hasRemoteBackup).set("statistics", (Object)this.statistics);
        }
    }
}
