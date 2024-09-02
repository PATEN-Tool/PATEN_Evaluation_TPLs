// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.solr.security;

import java.util.Objects;
import java.util.Map;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.client.solrj.impl.HttpClientConfigurer;

public class ConfigurableInternodeAuthHadoopPlugin extends HadoopAuthPlugin implements HttpClientInterceptorPlugin
{
    private static final String HTTPCLIENT_BUILDER_FACTORY = "clientBuilderFactory";
    private HttpClientConfigurer factory;
    
    public ConfigurableInternodeAuthHadoopPlugin(final CoreContainer coreContainer) {
        super(coreContainer);
        this.factory = null;
    }
    
    @Override
    public void init(final Map<String, Object> pluginConfig) {
        super.init(pluginConfig);
        final String httpClientBuilderFactory = Objects.requireNonNull(pluginConfig.get("clientBuilderFactory"), "Please specify clientBuilderFactory to be used for Solr internal communication.");
        this.factory = this.coreContainer.getResourceLoader().newInstance(httpClientBuilderFactory, HttpClientConfigurer.class);
    }
    
    @Override
    public HttpClientConfigurer getClientConfigurer() {
        return this.factory;
    }
}
