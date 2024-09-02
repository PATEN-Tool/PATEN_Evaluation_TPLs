// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.solr.security;

import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;
import org.apache.solr.client.solrj.impl.HttpListenerFactory;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.eclipse.jetty.client.api.Request;
import org.apache.solr.servlet.SolrDispatchFilter;
import org.apache.solr.request.SolrRequestInfo;
import org.apache.http.protocol.HttpContext;
import org.apache.http.HttpRequest;
import javax.servlet.FilterChain;
import javax.servlet.ServletResponse;
import javax.servlet.ServletRequest;
import com.google.common.annotations.VisibleForTesting;
import org.apache.solr.cloud.ZkController;
import java.util.Iterator;
import org.apache.commons.collections.iterators.IteratorEnumeration;
import java.util.Enumeration;
import javax.servlet.ServletContext;
import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.solr.client.solrj.impl.SolrHttpClientBuilder;
import java.util.HashMap;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import org.apache.solr.common.SolrException;
import java.util.Map;
import org.apache.solr.core.CoreContainer;
import javax.servlet.Filter;
import org.apache.solr.client.solrj.impl.Krb5HttpClientBuilder;
import org.slf4j.Logger;

public class KerberosPlugin extends AuthenticationPlugin implements HttpClientBuilderPlugin
{
    private static final Logger log;
    Krb5HttpClientBuilder kerberosBuilder;
    private Filter kerberosFilter;
    public static final String NAME_RULES_PARAM = "solr.kerberos.name.rules";
    public static final String COOKIE_DOMAIN_PARAM = "solr.kerberos.cookie.domain";
    public static final String COOKIE_PATH_PARAM = "solr.kerberos.cookie.path";
    public static final String PRINCIPAL_PARAM = "solr.kerberos.principal";
    public static final String KEYTAB_PARAM = "solr.kerberos.keytab";
    public static final String TOKEN_VALID_PARAM = "solr.kerberos.token.valid";
    public static final String COOKIE_PORT_AWARE_PARAM = "solr.kerberos.cookie.portaware";
    public static final String IMPERSONATOR_PREFIX = "solr.kerberos.impersonator.user.";
    public static final String DELEGATION_TOKEN_ENABLED = "solr.kerberos.delegation.token.enabled";
    public static final String DELEGATION_TOKEN_KIND = "solr.kerberos.delegation.token.kind";
    public static final String DELEGATION_TOKEN_VALIDITY = "solr.kerberos.delegation.token.validity";
    public static final String DELEGATION_TOKEN_SECRET_PROVIDER = "solr.kerberos.delegation.token.signer.secret.provider";
    public static final String DELEGATION_TOKEN_SECRET_PROVIDER_ZK_PATH = "solr.kerberos.delegation.token.signer.secret.provider.zookeper.path";
    public static final String DELEGATION_TOKEN_SECRET_MANAGER_ZNODE_WORKING_PATH = "solr.kerberos.delegation.token.secret.manager.znode.working.path";
    public static final String DELEGATION_TOKEN_TYPE_DEFAULT = "solr-dt";
    public static final String IMPERSONATOR_DO_AS_HTTP_PARAM = "doAs";
    public static final String IMPERSONATOR_USER_NAME = "solr.impersonator.user.name";
    public static final String ORIGINAL_USER_PRINCIPAL_HEADER = "originalUserPrincipal";
    static final String DELEGATION_TOKEN_ZK_CLIENT = "solr.kerberos.delegation.token.zk.client";
    private final CoreContainer coreContainer;
    
    public KerberosPlugin(final CoreContainer coreContainer) {
        this.kerberosBuilder = new Krb5HttpClientBuilder();
        this.coreContainer = coreContainer;
    }
    
    @Override
    public void init(final Map<String, Object> pluginConfig) {
        try {
            final FilterConfig conf = this.getInitFilterConfig(pluginConfig, false);
            this.kerberosFilter.init(conf);
        }
        catch (ServletException e) {
            throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Error initializing kerberos authentication plugin: " + e);
        }
    }
    
    @VisibleForTesting
    protected FilterConfig getInitFilterConfig(final Map<String, Object> pluginConfig, final boolean skipKerberosChecking) {
        final Map<String, String> params = new HashMap<String, String>();
        params.put("type", "kerberos");
        this.putParam(params, "kerberos.name.rules", "solr.kerberos.name.rules", "DEFAULT");
        this.putParam(params, "token.valid", "solr.kerberos.token.valid", "30");
        this.putParam(params, "cookie.path", "solr.kerberos.cookie.path", "/");
        if (!skipKerberosChecking) {
            this.putParam(params, "kerberos.principal", "solr.kerberos.principal", null);
            this.putParam(params, "kerberos.keytab", "solr.kerberos.keytab", null);
        }
        else {
            this.putParamOptional(params, "kerberos.principal", "solr.kerberos.principal");
            this.putParamOptional(params, "kerberos.keytab", "solr.kerberos.keytab");
        }
        final String delegationTokenStr = System.getProperty("solr.kerberos.delegation.token.enabled", null);
        final boolean delegationTokenEnabled = delegationTokenStr != null && Boolean.parseBoolean(delegationTokenStr);
        final ZkController controller = this.coreContainer.getZkController();
        if (delegationTokenEnabled) {
            this.putParam(params, "delegation-token.token-kind", "solr.kerberos.delegation.token.kind", "solr-dt");
            if (this.coreContainer.isZooKeeperAware()) {
                this.putParam(params, "signer.secret.provider", "solr.kerberos.delegation.token.signer.secret.provider", "zookeeper");
                if ("zookeeper".equals(params.get("signer.secret.provider"))) {
                    final String zkHost = controller.getZkServerAddress();
                    this.putParam(params, "token.validity", "solr.kerberos.delegation.token.validity", "36000");
                    params.put("zk-dt-secret-manager.enable", "true");
                    final String chrootPath = zkHost.contains("/") ? zkHost.substring(zkHost.indexOf("/")) : "";
                    String znodeWorkingPath = chrootPath + "/security" + "/zkdtsm";
                    znodeWorkingPath = (znodeWorkingPath.startsWith("/") ? znodeWorkingPath.substring(1) : znodeWorkingPath);
                    this.putParam(params, "zk-dt-secret-manager.znodeWorkingPath", "solr.kerberos.delegation.token.secret.manager.znode.working.path", znodeWorkingPath);
                    this.putParam(params, "signer.secret.provider.zookeeper.path", "solr.kerberos.delegation.token.signer.secret.provider.zookeper.path", "/token");
                    this.getHttpClientBuilder(SolrHttpClientBuilder.create());
                }
            }
            else {
                KerberosPlugin.log.info("CoreContainer is not ZooKeeperAware, not setting ZK-related delegation token properties");
            }
        }
        final String usePortStr = System.getProperty("solr.kerberos.cookie.portaware", null);
        final boolean needPortAwareCookies = usePortStr != null && Boolean.parseBoolean(usePortStr);
        if (!needPortAwareCookies || !this.coreContainer.isZooKeeperAware()) {
            this.putParam(params, "cookie.domain", "solr.kerberos.cookie.domain", null);
        }
        else {
            final String host = System.getProperty("solr.kerberos.cookie.domain", null);
            if (host == null) {
                throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Missing required parameter 'solr.kerberos.cookie.domain'.");
            }
            final int port = controller.getHostPort();
            params.put("cookie.domain", host + ":" + port);
        }
        final Enumeration e = System.getProperties().propertyNames();
        while (e.hasMoreElements()) {
            final String key = e.nextElement().toString();
            if (key.startsWith("solr.kerberos.impersonator.user.")) {
                if (!delegationTokenEnabled) {
                    throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Impersonator configuration requires delegation tokens to be enabled: " + key);
                }
                params.put(key, System.getProperty(key));
            }
        }
        params.put("delegation-token.json-mapper." + JsonGenerator.Feature.AUTO_CLOSE_TARGET, "false");
        final ServletContext servletContext = (ServletContext)new AttributeOnlyServletContext();
        if (controller != null) {
            servletContext.setAttribute("solr.kerberos.delegation.token.zk.client", (Object)controller.getZkClient());
        }
        if (delegationTokenEnabled) {
            this.kerberosFilter = (Filter)new DelegationTokenKerberosFilter();
        }
        else {
            this.kerberosFilter = (Filter)new KerberosFilter(this.coreContainer);
        }
        KerberosPlugin.log.info("Params: {}", (Object)params);
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
                return "KerberosFilter";
            }
        };
        return conf;
    }
    
    private void putParam(final Map<String, String> params, final String internalParamName, final String externalParamName, final String defaultValue) {
        final String value = System.getProperty(externalParamName, defaultValue);
        if (value == null) {
            throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Missing required parameter '" + externalParamName + "'.");
        }
        params.put(internalParamName, value);
    }
    
    private void putParamOptional(final Map<String, String> params, final String internalParamName, final String externalParamName) {
        final String value = System.getProperty(externalParamName);
        if (value != null) {
            params.put(internalParamName, value);
        }
    }
    
    @Override
    public boolean doAuthenticate(final ServletRequest req, final ServletResponse rsp, final FilterChain chain) throws Exception {
        KerberosPlugin.log.debug("Request to authenticate using kerberos: {}", (Object)req);
        this.kerberosFilter.doFilter(req, rsp, chain);
        final String requestContinuesAttr = (String)req.getAttribute("org.apache.solr.security.authentication.requestcontinues");
        if (requestContinuesAttr == null) {
            KerberosPlugin.log.warn("Could not find {}", (Object)"org.apache.solr.security.authentication.requestcontinues");
            return false;
        }
        return Boolean.parseBoolean(requestContinuesAttr);
    }
    
    @Override
    protected boolean interceptInternodeRequest(final HttpRequest httpRequest, final HttpContext httpContext) {
        final SolrRequestInfo info = SolrRequestInfo.getRequestInfo();
        if (info != null && (info.getAction() == SolrDispatchFilter.Action.FORWARD || info.getAction() == SolrDispatchFilter.Action.REMOTEQUERY) && info.getUserPrincipal() != null) {
            if (KerberosPlugin.log.isInfoEnabled()) {
                KerberosPlugin.log.info("Setting original user principal: {}", (Object)info.getUserPrincipal().getName());
            }
            httpRequest.setHeader("originalUserPrincipal", info.getUserPrincipal().getName());
            return true;
        }
        return false;
    }
    
    @Override
    protected boolean interceptInternodeRequest(final Request request) {
        final SolrRequestInfo info = SolrRequestInfo.getRequestInfo();
        if (info != null && (info.getAction() == SolrDispatchFilter.Action.FORWARD || info.getAction() == SolrDispatchFilter.Action.REMOTEQUERY) && info.getUserPrincipal() != null) {
            if (KerberosPlugin.log.isInfoEnabled()) {
                KerberosPlugin.log.info("Setting original user principal: {}", (Object)info.getUserPrincipal().getName());
            }
            request.header("originalUserPrincipal", info.getUserPrincipal().getName());
            return true;
        }
        return false;
    }
    
    @Override
    public SolrHttpClientBuilder getHttpClientBuilder(final SolrHttpClientBuilder builder) {
        return this.kerberosBuilder.getBuilder(builder);
    }
    
    @Override
    public void setup(final Http2SolrClient client) {
        final HttpListenerFactory.RequestResponseListener listener = new HttpListenerFactory.RequestResponseListener() {
            public void onQueued(final Request request) {
                KerberosPlugin.this.interceptInternodeRequest(request);
            }
        };
        client.addListenerFactory(() -> listener);
        this.kerberosBuilder.setup(client);
    }
    
    @Override
    public void close() {
        this.kerberosFilter.destroy();
        this.kerberosBuilder.close();
    }
    
    protected Filter getKerberosFilter() {
        return this.kerberosFilter;
    }
    
    protected void setKerberosFilter(final Filter kerberosFilter) {
        this.kerberosFilter = kerberosFilter;
    }
    
    static {
        log = LoggerFactory.getLogger((Class)MethodHandles.lookup().lookupClass());
    }
}
