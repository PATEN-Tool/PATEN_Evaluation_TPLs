// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.solr.security;

import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;
import org.apache.solr.servlet.SolrDispatchFilter;
import org.apache.solr.request.SolrRequestInfo;
import java.util.function.BiConsumer;
import org.eclipse.jetty.client.api.Request;
import java.net.URISyntaxException;
import org.apache.http.client.utils.URIBuilder;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.protocol.HttpContext;
import org.apache.http.HttpRequest;
import java.io.IOException;
import java.util.Optional;
import org.apache.solr.client.solrj.impl.SolrHttpClientBuilder;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import java.util.Objects;
import java.util.Map;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.client.solrj.impl.HttpClientBuilderFactory;
import org.slf4j.Logger;

public class ConfigurableInternodeAuthHadoopPlugin extends HadoopAuthPlugin implements HttpClientBuilderPlugin
{
    private static final Logger log;
    private static final String HTTPCLIENT_BUILDER_FACTORY = "clientBuilderFactory";
    private static final String DO_AS = "doAs";
    private HttpClientBuilderFactory factory;
    
    public ConfigurableInternodeAuthHadoopPlugin(final CoreContainer coreContainer) {
        super(coreContainer);
        this.factory = null;
    }
    
    @Override
    public void init(final Map<String, Object> pluginConfig) {
        super.init(pluginConfig);
        final String httpClientBuilderFactory = Objects.requireNonNull(pluginConfig.get("clientBuilderFactory"), "Please specify clientBuilderFactory to be used for Solr internal communication.");
        this.factory = this.coreContainer.getResourceLoader().newInstance(httpClientBuilderFactory, HttpClientBuilderFactory.class);
    }
    
    @Override
    public void setup(final Http2SolrClient client) {
        this.factory.setup(client);
    }
    
    @Override
    public SolrHttpClientBuilder getHttpClientBuilder(final SolrHttpClientBuilder builder) {
        return this.factory.getHttpClientBuilder((Optional)Optional.ofNullable(builder));
    }
    
    @Override
    public void close() throws IOException {
        super.close();
        if (this.factory != null) {
            this.factory.close();
        }
    }
    
    public boolean interceptInternodeRequest(final HttpRequest httpRequest, final HttpContext httpContext) {
        if (!(httpRequest instanceof HttpRequestWrapper)) {
            ConfigurableInternodeAuthHadoopPlugin.log.warn("Unable to add doAs to forwarded/distributed request - unknown request type");
            return false;
        }
        final AtomicBoolean success = new AtomicBoolean(false);
        final HttpRequestWrapper request;
        final URIBuilder uriBuilder;
        final AtomicBoolean atomicBoolean;
        return this.intercept((key, value) -> {
            request = (HttpRequestWrapper)httpRequest;
            uriBuilder = new URIBuilder(request.getURI());
            uriBuilder.setParameter(key, value);
            try {
                request.setURI(uriBuilder.build());
                atomicBoolean.set(true);
            }
            catch (URISyntaxException e) {
                ConfigurableInternodeAuthHadoopPlugin.log.warn("Unable to add doAs to forwarded/distributed request - bad URI");
            }
        }) && success.get();
    }
    
    @Override
    protected boolean interceptInternodeRequest(final Request request) {
        return this.intercept(request::param);
    }
    
    private boolean intercept(final BiConsumer<String, String> setParam) {
        final SolrRequestInfo info = SolrRequestInfo.getRequestInfo();
        if (info != null && (info.getAction() == SolrDispatchFilter.Action.FORWARD || info.getAction() == SolrDispatchFilter.Action.REMOTEQUERY) && info.getUserPrincipal() != null) {
            final String name = info.getUserPrincipal().getName();
            ConfigurableInternodeAuthHadoopPlugin.log.debug("Setting doAs={} to forwarded/remote request", (Object)name);
            setParam.accept("doAs", name);
            return true;
        }
        return false;
    }
    
    static {
        log = LoggerFactory.getLogger((Class)MethodHandles.lookup().lookupClass());
    }
}
