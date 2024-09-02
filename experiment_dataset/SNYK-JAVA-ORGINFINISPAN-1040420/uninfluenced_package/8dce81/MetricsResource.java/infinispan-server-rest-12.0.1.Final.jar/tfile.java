// 
// Decompiled by Procyon v0.5.36
// 

package org.infinispan.rest.resources;

import org.infinispan.util.logging.LogFactory;
import java.util.Iterator;
import java.util.Map;
import org.infinispan.rest.framework.impl.RestResponseBuilder;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import org.infinispan.rest.NettyRestResponse;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Collections;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.infinispan.rest.framework.RestResponse;
import java.util.concurrent.CompletionStage;
import org.infinispan.rest.framework.RestRequest;
import org.infinispan.rest.framework.Method;
import org.infinispan.rest.framework.impl.Invocations;
import java.io.IOException;
import io.smallrye.metrics.setup.JmxRegistrar;
import io.smallrye.metrics.MetricsRequestHandler;
import org.infinispan.rest.logging.Log;
import org.infinispan.rest.framework.ResourceHandler;

public final class MetricsResource implements ResourceHandler
{
    private static final Log log;
    private static final String METRICS_PATH = "/metrics";
    private final MetricsRequestHandler requestHandler;
    private final boolean auth;
    
    public MetricsResource(final boolean auth) {
        this.requestHandler = new MetricsRequestHandler();
        this.registerBaseMetrics();
        this.auth = auth;
    }
    
    private void registerBaseMetrics() {
        try {
            new JmxRegistrar().init();
        }
        catch (IOException | IllegalArgumentException ex2) {
            final Exception ex;
            final Exception e = ex;
            MetricsResource.log.debug((Object)"Failed to initialize base and vendor metrics from platform's JMX MBeans", (Throwable)e);
        }
    }
    
    @Override
    public Invocations getInvocations() {
        return new Invocations.Builder().invocation().methods(Method.GET, Method.OPTIONS).path("/metrics").anonymous(!this.auth).handleWith(this::metrics).invocation().methods(Method.GET, Method.OPTIONS).path("/metrics/*").anonymous(!this.auth).handleWith(this::metrics).create();
    }
    
    private CompletionStage<RestResponse> metrics(final RestRequest restRequest) {
        try {
            List<String> accept = restRequest.headers(HttpHeaderNames.ACCEPT.toString());
            if (restRequest.method() == Method.GET) {
                if (accept.isEmpty()) {
                    accept = Collections.singletonList("text/plain");
                }
                else {
                    accept = accept.stream().map(h -> h.startsWith("application/openmetrics-text") ? "text/plain" : h).collect((Collector<? super Object, ?, List<String>>)Collectors.toList());
                }
            }
            else if (restRequest.method() == Method.OPTIONS && accept.isEmpty()) {
                accept = Collections.singletonList("application/json");
            }
            final RestResponseBuilder<NettyRestResponse.Builder> builder = new NettyRestResponse.Builder();
            this.requestHandler.handleRequest(restRequest.path(), restRequest.method().name(), (Stream)accept.stream(), (status, message, headers) -> {
                builder.status(status).entity((Object)message);
                for (final String header : headers.keySet()) {
                    builder.header(header, headers.get(header));
                }
            });
            return CompletableFuture.completedFuture(builder.build());
        }
        catch (Exception e) {
            final RestResponseBuilder<NettyRestResponse.Builder> builder = new NettyRestResponse.Builder().status(HttpResponseStatus.INTERNAL_SERVER_ERROR).entity((Object)e.getMessage());
            return CompletableFuture.completedFuture(builder.build());
        }
    }
    
    static {
        log = (Log)LogFactory.getLog((Class)MetricsResource.class, (Class)Log.class);
    }
}
