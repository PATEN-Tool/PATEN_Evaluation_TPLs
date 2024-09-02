// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.solr.security;

import java.io.IOException;
import java.util.Optional;
import org.apache.solr.client.solrj.impl.SolrHttpClientBuilder;
import java.util.Objects;
import java.util.Map;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.client.solrj.impl.HttpClientBuilderFactory;

public class ConfigurableInternodeAuthHadoopPlugin extends HadoopAuthPlugin implements HttpClientBuilderPlugin
{
    private static final String HTTPCLIENT_BUILDER_FACTORY = "clientBuilderFactory";
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
}
