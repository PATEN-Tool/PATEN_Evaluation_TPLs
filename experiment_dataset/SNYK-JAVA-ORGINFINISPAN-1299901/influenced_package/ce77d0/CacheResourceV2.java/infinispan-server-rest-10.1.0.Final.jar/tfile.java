// 
// Decompiled by Procyon v0.5.36
// 

package org.infinispan.rest.resources;

import com.fasterxml.jackson.annotation.JsonRawValue;
import org.infinispan.rest.RestResponseException;
import org.infinispan.AdvancedCache;
import org.infinispan.query.impl.ComponentRegistryUtils;
import org.infinispan.query.impl.InfinispanQueryStatisticsInfo;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.stats.Stats;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.infinispan.rest.framework.ContentSource;
import org.infinispan.commons.api.CacheContainerAdmin;
import java.nio.charset.StandardCharsets;
import org.infinispan.commons.configuration.ConfigurationBuilderInfo;
import org.infinispan.commons.dataconversion.StandardConversions;
import java.util.EnumSet;
import org.infinispan.manager.EmbeddedCacheManagerAdmin;
import org.infinispan.rest.cachemanager.RestCacheManager;
import org.infinispan.Cache;
import org.infinispan.CacheStream;
import org.infinispan.rest.CacheInputStream;
import java.util.List;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ParserRegistry;
import org.infinispan.commons.configuration.ConfigurationInfo;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import java.util.concurrent.CompletableFuture;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.infinispan.rest.NettyRestResponse;
import org.infinispan.rest.framework.impl.InvocationImpl;
import java.util.Objects;
import org.infinispan.rest.framework.RestResponse;
import java.util.concurrent.CompletionStage;
import org.infinispan.rest.framework.RestRequest;
import java.util.function.Function;
import org.infinispan.rest.framework.Method;
import org.infinispan.rest.framework.impl.Invocations;
import org.infinispan.rest.InvocationHelper;

public class CacheResourceV2 extends CacheResource
{
    private static final int STREAM_BATCH_SIZE = 1000;
    
    public CacheResourceV2(final InvocationHelper invocationHelper) {
        super(invocationHelper);
    }
    
    @Override
    public Invocations getInvocations() {
        final InvocationImpl.Builder withAction = new Invocations.Builder().invocation().methods(Method.PUT, Method.POST).path("/v2/caches/{cacheName}/{cacheKey}").handleWith(this::putValueToCache).invocation().methods(Method.GET, Method.HEAD).path("/v2/caches/{cacheName}/{cacheKey}").handleWith(this::getCacheValue).invocation().method(Method.DELETE).path("/v2/caches/{cacheName}/{cacheKey}").handleWith(this::deleteCacheValue).invocation().methods(Method.GET).path("/v2/caches/{cacheName}").withAction("keys").handleWith(this::streamKeys).invocation().methods(Method.GET, Method.HEAD).path("/v2/caches/{cacheName}").withAction("config").handleWith(this::getCacheConfig).invocation().methods(Method.GET).path("/v2/caches/{cacheName}").withAction("stats").handleWith(this::getCacheStats).invocation().methods(Method.GET).path("/v2/caches/").handleWith(this::getCacheNames).invocation().methods(Method.POST).path("/v2/caches/{cacheName}").handleWith((Function<RestRequest, CompletionStage<RestResponse>>)this::createCache).invocation().method(Method.DELETE).path("/v2/caches/{cacheName}").handleWith(this::removeCache).invocation().method(Method.HEAD).path("/v2/caches/{cacheName}").handleWith(this::cacheExists).invocation().methods(Method.GET).path("/v2/caches/{cacheName}").withAction("clear").handleWith(this::clearEntireCache).invocation().methods(Method.GET).path("/v2/caches/{cacheName}").withAction("size").handleWith(this::getSize).invocation().methods(Method.GET, Method.POST).path("/v2/caches/{cacheName}").withAction("search");
        final CacheResourceQueryAction queryAction = this.queryAction;
        Objects.requireNonNull(queryAction);
        return withAction.handleWith((Function<RestRequest, CompletionStage<RestResponse>>)queryAction::search).invocation().methods(Method.POST).path("/v2/caches").withAction("toJSON").handleWith(this::convertToJson).invocation().methods(Method.GET).path("/v2/caches/{cacheName}").handleWith(this::getAllDetails).create();
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
        final NettyRestResponse.Builder responseBuilder = new NettyRestResponse.Builder();
        final String cacheName = request.variables().get("cacheName");
        final List<String> values = request.parameters().get("batch");
        final int batch = (values == null || values.isEmpty()) ? 1000 : Integer.parseInt(values.iterator().next());
        final Cache<?, ?> cache = (Cache<?, ?>)this.invocationHelper.getRestCacheManager().getCache(cacheName, MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON, request);
        if (cache == null) {
            responseBuilder.status(HttpResponseStatus.NOT_FOUND);
            return (CompletionStage<RestResponse>)CompletableFuture.completedFuture(responseBuilder.build());
        }
        responseBuilder.entity((Object)new CacheInputStream((CacheStream<?>)cache.keySet().stream(), batch));
        responseBuilder.contentType("application/json");
        return (CompletionStage<RestResponse>)CompletableFuture.completedFuture(responseBuilder.build());
    }
    
    private CompletionStage<RestResponse> removeCache(final RestRequest request) {
        final NettyRestResponse.Builder responseBuilder = new NettyRestResponse.Builder().status(HttpResponseStatus.NO_CONTENT);
        final String cacheName = request.variables().get("cacheName");
        final RestCacheManager<Object> restCacheManager = this.invocationHelper.getRestCacheManager();
        final Cache<?, ?> cache = (Cache<?, ?>)restCacheManager.getCache(cacheName, request);
        if (cache == null) {
            responseBuilder.status(HttpResponseStatus.NOT_FOUND);
            return (CompletionStage<RestResponse>)CompletableFuture.completedFuture(responseBuilder.build());
        }
        final RestCacheManager restCacheManager2;
        final String s;
        final NettyRestResponse.Builder builder;
        return (CompletionStage<RestResponse>)CompletableFuture.supplyAsync(() -> {
            restCacheManager2.getCacheManagerAdmin(request).removeCache(s);
            builder.status(HttpResponseStatus.OK);
            return builder.build();
        }, this.invocationHelper.getExecutor());
    }
    
    private CompletionStage<RestResponse> cacheExists(final RestRequest restRequest) {
        final NettyRestResponse.Builder responseBuilder = new NettyRestResponse.Builder();
        final String cacheName = restRequest.variables().get("cacheName");
        if (!this.invocationHelper.getRestCacheManager().getInstance().getCacheConfigurationNames().contains(cacheName)) {
            responseBuilder.status(HttpResponseStatus.NOT_FOUND).build();
        }
        return (CompletionStage<RestResponse>)CompletableFuture.completedFuture(responseBuilder.build());
    }
    
    private CompletableFuture<RestResponse> createCache(final RestRequest request) {
        final NettyRestResponse.Builder responseBuilder = new NettyRestResponse.Builder().status(HttpResponseStatus.NO_CONTENT);
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
            embeddedCacheManagerAdmin3.createCache(s4, configurationBuilder.build());
            builder3.status(HttpResponseStatus.OK);
            return builder3.build();
        }, this.invocationHelper.getExecutor());
    }
    
    private CompletionStage<RestResponse> getCacheStats(final RestRequest request) {
        final NettyRestResponse.Builder responseBuilder = new NettyRestResponse.Builder();
        final String cacheName = request.variables().get("cacheName");
        final Cache<?, ?> cache = (Cache<?, ?>)this.invocationHelper.getRestCacheManager().getCache(cacheName, request);
        final Stats stats = cache.getAdvancedCache().getStats();
        try {
            final byte[] statsResponse = this.invocationHelper.getMapper().writeValueAsBytes((Object)stats);
            responseBuilder.contentType(MediaType.APPLICATION_JSON).entity((Object)statsResponse).status(HttpResponseStatus.OK);
        }
        catch (JsonProcessingException e) {
            responseBuilder.status(HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }
        return (CompletionStage<RestResponse>)CompletableFuture.completedFuture(responseBuilder.build());
    }
    
    private CompletionStage<RestResponse> getAllDetails(final RestRequest request) {
        final NettyRestResponse.Builder responseBuilder = new NettyRestResponse.Builder();
        final String cacheName = request.variables().get("cacheName");
        final Cache<?, ?> cache = (Cache<?, ?>)this.invocationHelper.getRestCacheManager().getCache(cacheName, request);
        if (cache == null) {
            responseBuilder.status(HttpResponseStatus.NOT_FOUND);
            return (CompletionStage<RestResponse>)CompletableFuture.completedFuture(responseBuilder.build());
        }
        return CompletableFuture.supplyAsync(() -> this.getDetailResponse(responseBuilder, cache), this.invocationHelper.getExecutor());
    }
    
    private RestResponse getDetailResponse(final NettyRestResponse.Builder responseBuilder, final Cache<?, ?> cache) {
        final Stats stats = cache.getAdvancedCache().getStats();
        final Configuration configuration = cache.getCacheConfiguration();
        final boolean statistics = configuration.jmxStatistics().enabled();
        final int size = cache.getAdvancedCache().size();
        final DistributionManager distributionManager = cache.getAdvancedCache().getDistributionManager();
        final InfinispanQueryStatisticsInfo.IndexStatistics indexStatistics = this.getIndexStatistics(cache);
        final boolean rehashInProgress = distributionManager != null && distributionManager.isRehashInProgress();
        final boolean indexingInProgress = indexStatistics != null && indexStatistics.getReindexing();
        try {
            final CacheFullDetail fullDetail = new CacheFullDetail();
            fullDetail.stats = stats;
            fullDetail.configuration = this.invocationHelper.getJsonWriter().toJSON((ConfigurationInfo)configuration);
            fullDetail.size = size;
            fullDetail.rehashInProgress = rehashInProgress;
            fullDetail.indexingInProgress = indexingInProgress;
            fullDetail.persistent = configuration.persistence().usingStores();
            fullDetail.bounded = configuration.memory().evictionStrategy().isEnabled();
            fullDetail.indexed = configuration.indexing().index().isEnabled();
            fullDetail.hasRemoteBackup = configuration.sites().hasEnabledBackups();
            fullDetail.secured = configuration.security().authorization().enabled();
            fullDetail.transactional = configuration.transaction().transactionMode().isTransactional();
            fullDetail.statistics = statistics;
            final byte[] detailsResponse = this.invocationHelper.getMapper().writeValueAsBytes((Object)fullDetail);
            responseBuilder.contentType(MediaType.APPLICATION_JSON).entity((Object)detailsResponse).status(HttpResponseStatus.OK);
        }
        catch (JsonProcessingException e) {
            responseBuilder.status(HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }
        return responseBuilder.build();
    }
    
    private InfinispanQueryStatisticsInfo.IndexStatistics getIndexStatistics(final Cache<?, ?> cache) {
        if (!cache.getCacheConfiguration().indexing().index().isEnabled()) {
            return null;
        }
        return ComponentRegistryUtils.getQueryStatistics(cache.getAdvancedCache()).getIndexStatistics();
    }
    
    private CompletionStage<RestResponse> getCacheConfig(final RestRequest request) {
        final NettyRestResponse.Builder responseBuilder = new NettyRestResponse.Builder();
        final String cacheName = request.variables().get("cacheName");
        final MediaType accept = MediaTypeUtils.negotiateMediaType(request, MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML);
        responseBuilder.contentType(accept);
        if (!this.invocationHelper.getRestCacheManager().getInstance().getCacheConfigurationNames().contains(cacheName)) {
            responseBuilder.status(HttpResponseStatus.NOT_FOUND).build();
        }
        final Cache<?, ?> cache = (Cache<?, ?>)this.invocationHelper.getRestCacheManager().getCache(cacheName, request);
        if (cache == null) {
            return (CompletionStage<RestResponse>)CompletableFuture.completedFuture(responseBuilder.status(HttpResponseStatus.NOT_FOUND.code()).build());
        }
        final Configuration cacheConfiguration = cache.getCacheConfiguration();
        String entity;
        if (accept.getTypeSubtype().equals("application/xml")) {
            entity = cacheConfiguration.toXMLString();
        }
        else {
            entity = this.invocationHelper.getJsonWriter().toJSON((ConfigurationInfo)cacheConfiguration);
        }
        return (CompletionStage<RestResponse>)CompletableFuture.completedFuture(responseBuilder.status(HttpResponseStatus.OK).entity((Object)entity).build());
    }
    
    private CompletionStage<RestResponse> getSize(final RestRequest request) {
        final String cacheName = request.variables().get("cacheName");
        final AdvancedCache<Object, Object> cache = this.invocationHelper.getRestCacheManager().getCache(cacheName, request);
        final CompletableFuture<Long> cacheSize = (CompletableFuture<Long>)cache.sizeAsync();
        final NettyRestResponse.Builder responseBuilder;
        return (CompletionStage<RestResponse>)cacheSize.thenApply(size -> {
            responseBuilder = new NettyRestResponse.Builder();
            try {
                responseBuilder.entity((Object)this.invocationHelper.getMapper().writeValueAsBytes((Object)size));
            }
            catch (JsonProcessingException e) {
                responseBuilder.status(HttpResponseStatus.INTERNAL_SERVER_ERROR);
            }
            return responseBuilder.build();
        });
    }
    
    private CompletionStage<RestResponse> getCacheNames(final RestRequest request) throws RestResponseException {
        final NettyRestResponse.Builder responseBuilder = new NettyRestResponse.Builder();
        try {
            final byte[] bytes = this.invocationHelper.getMapper().writeValueAsBytes((Object)this.invocationHelper.getRestCacheManager().getCacheNames());
            responseBuilder.contentType(MediaType.APPLICATION_JSON).entity((Object)bytes).status(HttpResponseStatus.OK);
        }
        catch (JsonProcessingException e) {
            responseBuilder.status(HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }
        return (CompletionStage<RestResponse>)CompletableFuture.completedFuture(responseBuilder.build());
    }
    
    class CacheFullDetail
    {
        public Stats stats;
        public int size;
        @JsonRawValue
        public String configuration;
        public boolean rehashInProgress;
        public boolean bounded;
        public boolean indexed;
        public boolean persistent;
        public boolean transactional;
        public boolean secured;
        public boolean hasRemoteBackup;
        public boolean indexingInProgress;
        public boolean statistics;
    }
}
