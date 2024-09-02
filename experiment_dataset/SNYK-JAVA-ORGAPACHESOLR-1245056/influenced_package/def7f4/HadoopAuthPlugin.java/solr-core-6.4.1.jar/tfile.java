// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.solr.security;

import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;
import org.apache.solr.common.util.SuppressForbidden;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.FilterChain;
import javax.servlet.ServletResponse;
import javax.servlet.ServletRequest;
import org.apache.solr.cloud.ZkController;
import java.util.Iterator;
import org.apache.commons.collections.iterators.IteratorEnumeration;
import java.util.Enumeration;
import javax.servlet.ServletContext;
import java.util.Collections;
import java.util.Collection;
import java.util.Objects;
import java.util.HashMap;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.solr.client.solrj.impl.Krb5HttpClientConfigurer;
import java.util.Map;
import org.apache.solr.core.CoreContainer;
import org.apache.hadoop.security.authentication.server.AuthenticationFilter;
import org.slf4j.Logger;

public class HadoopAuthPlugin extends AuthenticationPlugin
{
    private static final Logger log;
    private static final String HADOOP_AUTH_TYPE = "type";
    private static final String SYSPROP_PREFIX_PROPERTY = "sysPropPrefix";
    private static final String AUTH_CONFIG_NAMES_PROPERTY = "authConfigs";
    private static final String DEFAULT_AUTH_CONFIGS_PROPERTY = "defaultConfigs";
    private static final String DELEGATION_TOKEN_ENABLED_PROPERTY = "enableDelegationToken";
    private static final String INIT_KERBEROS_ZK = "initKerberosZk";
    public static final String PROXY_USER_CONFIGS = "proxyUserConfigs";
    private AuthenticationFilter authFilter;
    protected final CoreContainer coreContainer;
    
    public HadoopAuthPlugin(final CoreContainer coreContainer) {
        this.coreContainer = coreContainer;
    }
    
    @Override
    public void init(final Map<String, Object> pluginConfig) {
        try {
            final String delegationTokenEnabled = pluginConfig.getOrDefault("enableDelegationToken", "false");
            this.authFilter = (AuthenticationFilter)(Boolean.parseBoolean(delegationTokenEnabled) ? new HadoopAuthFilter() : new AuthenticationFilter());
            final boolean initKerberosZk = Boolean.parseBoolean(pluginConfig.getOrDefault("initKerberosZk", "false"));
            if (initKerberosZk) {
                new Krb5HttpClientConfigurer().configure(new DefaultHttpClient(), (SolrParams)new ModifiableSolrParams());
            }
            final FilterConfig conf = this.getInitFilterConfig(pluginConfig);
            this.authFilter.init(conf);
        }
        catch (ServletException e) {
            throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Error initializing kerberos authentication plugin: " + e);
        }
    }
    
    protected FilterConfig getInitFilterConfig(final Map<String, Object> pluginConfig) {
        final Map<String, String> params = new HashMap<String, String>();
        final String type = Objects.requireNonNull(pluginConfig.get("type"));
        params.put("type", type);
        final String sysPropPrefix = pluginConfig.getOrDefault("sysPropPrefix", "solr.");
        final Collection<String> authConfigNames = pluginConfig.getOrDefault("authConfigs", Collections.emptyList());
        final Map<String, String> authConfigDefaults = pluginConfig.getOrDefault("defaultConfigs", Collections.emptyMap());
        final Map<String, String> proxyUserConfigs = pluginConfig.getOrDefault("proxyUserConfigs", Collections.emptyMap());
        for (final String configName : authConfigNames) {
            final String systemProperty = sysPropPrefix + configName;
            final String defaultConfigVal = authConfigDefaults.get(configName);
            final String configVal = System.getProperty(systemProperty, defaultConfigVal);
            if (configVal != null) {
                params.put(configName, configVal);
            }
        }
        params.putAll(proxyUserConfigs);
        final ServletContext servletContext = (ServletContext)new AttributeOnlyServletContext();
        HadoopAuthPlugin.log.info("Params: " + params);
        final ZkController controller = this.coreContainer.getZkController();
        if (controller != null) {
            servletContext.setAttribute("solr.kerberos.delegation.token.zk.client", (Object)controller.getZkClient());
        }
        final FilterConfig conf = (FilterConfig)new FilterConfig() {
            public ServletContext getServletContext() {
                return servletContext;
            }
            
            public Enumeration<String> getInitParameterNames() {
                return (Enumeration<String>)new IteratorEnumeration((Iterator)params.keySet().iterator());
            }
            
            public String getInitParameter(final String param) {
                return params.get(param);
            }
            
            public String getFilterName() {
                return "HadoopAuthFilter";
            }
        };
        return conf;
    }
    
    @Override
    public boolean doAuthenticate(final ServletRequest request, final ServletResponse response, final FilterChain filterChain) throws Exception {
        final HttpServletResponse frsp = (HttpServletResponse)response;
        final HttpServletResponse rspCloseShield = (HttpServletResponse)new HttpServletResponseWrapper(frsp) {
            @SuppressForbidden(reason = "Hadoop DelegationTokenAuthenticationFilter uses response writer, thisis providing a CloseShield on top of that")
            public PrintWriter getWriter() throws IOException {
                final PrintWriter pw = new PrintWriterWrapper(frsp.getWriter()) {
                    @Override
                    public void close() {
                    }
                };
                return pw;
            }
        };
        this.authFilter.doFilter(request, (ServletResponse)rspCloseShield, filterChain);
        if (!(this.authFilter instanceof HadoopAuthFilter)) {
            return true;
        }
        final String requestContinuesAttr = (String)request.getAttribute("org.apache.solr.security.authentication.requestcontinues");
        if (requestContinuesAttr == null) {
            HadoopAuthPlugin.log.warn("Could not find org.apache.solr.security.authentication.requestcontinues");
            return false;
        }
        return Boolean.parseBoolean(requestContinuesAttr);
    }
    
    @Override
    public void close() throws IOException {
        if (this.authFilter != null) {
            this.authFilter.destroy();
        }
    }
    
    static {
        log = LoggerFactory.getLogger((Class)MethodHandles.lookup().lookupClass());
    }
}
