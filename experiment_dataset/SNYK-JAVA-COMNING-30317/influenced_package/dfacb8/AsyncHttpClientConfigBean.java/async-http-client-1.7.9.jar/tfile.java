// 
// Decompiled by Procyon v0.5.36
// 

package com.ning.http.client;

import javax.net.ssl.SSLContext;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import javax.net.ssl.SSLSession;
import javax.net.ssl.HostnameVerifier;
import com.ning.http.util.ProxyUtils;
import com.ning.http.client.filter.IOExceptionFilter;
import com.ning.http.client.filter.ResponseFilter;
import com.ning.http.client.filter.RequestFilter;
import java.util.LinkedList;

public class AsyncHttpClientConfigBean extends AsyncHttpClientConfig
{
    public AsyncHttpClientConfigBean() {
        this.configureExecutors();
        this.configureDefaults();
        this.configureFilters();
    }
    
    void configureFilters() {
        this.requestFilters = new LinkedList<RequestFilter>();
        this.responseFilters = new LinkedList<ResponseFilter>();
        this.ioExceptionFilters = new LinkedList<IOExceptionFilter>();
    }
    
    void configureDefaults() {
        this.maxTotalConnections = Integer.getInteger(AsyncHttpClientConfigBean.ASYNC_CLIENT + "defaultMaxTotalConnections", -1);
        this.maxConnectionPerHost = Integer.getInteger(AsyncHttpClientConfigBean.ASYNC_CLIENT + "defaultMaxConnectionsPerHost", -1);
        this.connectionTimeOutInMs = Integer.getInteger(AsyncHttpClientConfigBean.ASYNC_CLIENT + "defaultConnectionTimeoutInMS", 60000);
        this.idleConnectionInPoolTimeoutInMs = Integer.getInteger(AsyncHttpClientConfigBean.ASYNC_CLIENT + "defaultIdleConnectionInPoolTimeoutInMS", 60000);
        this.idleConnectionTimeoutInMs = Integer.getInteger(AsyncHttpClientConfigBean.ASYNC_CLIENT + "defaultIdleConnectionTimeoutInMS", 60000);
        this.requestTimeoutInMs = Integer.getInteger(AsyncHttpClientConfigBean.ASYNC_CLIENT + "defaultRequestTimeoutInMS", 60000);
        this.redirectEnabled = Boolean.getBoolean(AsyncHttpClientConfigBean.ASYNC_CLIENT + "defaultRedirectsEnabled");
        this.maxDefaultRedirects = Integer.getInteger(AsyncHttpClientConfigBean.ASYNC_CLIENT + "defaultMaxRedirects", 5);
        this.compressionEnabled = Boolean.getBoolean(AsyncHttpClientConfigBean.ASYNC_CLIENT + "compressionEnabled");
        this.userAgent = System.getProperty(AsyncHttpClientConfigBean.ASYNC_CLIENT + "userAgent", "NING/1.0");
        final boolean useProxyProperties = Boolean.getBoolean(AsyncHttpClientConfigBean.ASYNC_CLIENT + "useProxyProperties");
        if (useProxyProperties) {
            this.proxyServer = ProxyUtils.createProxy(System.getProperties());
        }
        this.allowPoolingConnection = true;
        this.requestCompressionLevel = -1;
        this.maxRequestRetry = 5;
        this.allowSslConnectionPool = true;
        this.useRawUrl = false;
        this.removeQueryParamOnRedirect = true;
        this.hostnameVerifier = new HostnameVerifier() {
            public boolean verify(final String s, final SSLSession sslSession) {
                return true;
            }
        };
    }
    
    void configureExecutors() {
        this.reaper = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {
            public Thread newThread(final Runnable r) {
                final Thread t = new Thread(r, "AsyncHttpClient-Reaper");
                t.setDaemon(true);
                return t;
            }
        });
        this.applicationThreadPool = Executors.newCachedThreadPool(new ThreadFactory() {
            public Thread newThread(final Runnable r) {
                final Thread t = new Thread(r, "AsyncHttpClient-Callback");
                t.setDaemon(true);
                return t;
            }
        });
    }
    
    public AsyncHttpClientConfigBean setMaxTotalConnections(final int maxTotalConnections) {
        this.maxTotalConnections = maxTotalConnections;
        return this;
    }
    
    public AsyncHttpClientConfigBean setMaxConnectionPerHost(final int maxConnectionPerHost) {
        this.maxConnectionPerHost = maxConnectionPerHost;
        return this;
    }
    
    public AsyncHttpClientConfigBean setConnectionTimeOutInMs(final int connectionTimeOutInMs) {
        this.connectionTimeOutInMs = connectionTimeOutInMs;
        return this;
    }
    
    public AsyncHttpClientConfigBean setIdleConnectionInPoolTimeoutInMs(final int idleConnectionInPoolTimeoutInMs) {
        this.idleConnectionInPoolTimeoutInMs = idleConnectionInPoolTimeoutInMs;
        return this;
    }
    
    public AsyncHttpClientConfigBean setIdleConnectionTimeoutInMs(final int idleConnectionTimeoutInMs) {
        this.idleConnectionTimeoutInMs = idleConnectionTimeoutInMs;
        return this;
    }
    
    public AsyncHttpClientConfigBean setRequestTimeoutInMs(final int requestTimeoutInMs) {
        this.requestTimeoutInMs = requestTimeoutInMs;
        return this;
    }
    
    public AsyncHttpClientConfigBean setRedirectEnabled(final boolean redirectEnabled) {
        this.redirectEnabled = redirectEnabled;
        return this;
    }
    
    public AsyncHttpClientConfigBean setMaxDefaultRedirects(final int maxDefaultRedirects) {
        this.maxDefaultRedirects = maxDefaultRedirects;
        return this;
    }
    
    public AsyncHttpClientConfigBean setCompressionEnabled(final boolean compressionEnabled) {
        this.compressionEnabled = compressionEnabled;
        return this;
    }
    
    public AsyncHttpClientConfigBean setUserAgent(final String userAgent) {
        this.userAgent = userAgent;
        return this;
    }
    
    public AsyncHttpClientConfigBean setAllowPoolingConnection(final boolean allowPoolingConnection) {
        this.allowPoolingConnection = allowPoolingConnection;
        return this;
    }
    
    public AsyncHttpClientConfigBean setReaper(final ScheduledExecutorService reaper) {
        if (this.reaper != null) {
            this.reaper.shutdownNow();
        }
        this.reaper = reaper;
        return this;
    }
    
    public AsyncHttpClientConfigBean setApplicationThreadPool(final ExecutorService applicationThreadPool) {
        if (this.applicationThreadPool != null) {
            this.applicationThreadPool.shutdownNow();
        }
        this.applicationThreadPool = applicationThreadPool;
        return this;
    }
    
    public AsyncHttpClientConfigBean setProxyServer(final ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
        return this;
    }
    
    public AsyncHttpClientConfigBean setSslContext(final SSLContext sslContext) {
        this.sslContext = sslContext;
        return this;
    }
    
    public AsyncHttpClientConfigBean setSslEngineFactory(final SSLEngineFactory sslEngineFactory) {
        this.sslEngineFactory = sslEngineFactory;
        return this;
    }
    
    public AsyncHttpClientConfigBean setProviderConfig(final AsyncHttpProviderConfig<?, ?> providerConfig) {
        this.providerConfig = providerConfig;
        return this;
    }
    
    public AsyncHttpClientConfigBean setConnectionsPool(final ConnectionsPool<?, ?> connectionsPool) {
        this.connectionsPool = connectionsPool;
        return this;
    }
    
    public AsyncHttpClientConfigBean setRealm(final Realm realm) {
        this.realm = realm;
        return this;
    }
    
    public AsyncHttpClientConfigBean addRequestFilter(final RequestFilter requestFilter) {
        this.requestFilters.add(requestFilter);
        return this;
    }
    
    public AsyncHttpClientConfigBean addResponseFilters(final ResponseFilter responseFilter) {
        this.responseFilters.add(responseFilter);
        return this;
    }
    
    public AsyncHttpClientConfigBean addIoExceptionFilters(final IOExceptionFilter ioExceptionFilter) {
        this.ioExceptionFilters.add(ioExceptionFilter);
        return this;
    }
    
    public AsyncHttpClientConfigBean setRequestCompressionLevel(final int requestCompressionLevel) {
        this.requestCompressionLevel = requestCompressionLevel;
        return this;
    }
    
    public AsyncHttpClientConfigBean setMaxRequestRetry(final int maxRequestRetry) {
        this.maxRequestRetry = maxRequestRetry;
        return this;
    }
    
    public AsyncHttpClientConfigBean setAllowSslConnectionPool(final boolean allowSslConnectionPool) {
        this.allowSslConnectionPool = allowSslConnectionPool;
        return this;
    }
    
    public AsyncHttpClientConfigBean setUseRawUrl(final boolean useRawUrl) {
        this.useRawUrl = useRawUrl;
        return this;
    }
    
    public AsyncHttpClientConfigBean setRemoveQueryParamOnRedirect(final boolean removeQueryParamOnRedirect) {
        this.removeQueryParamOnRedirect = removeQueryParamOnRedirect;
        return this;
    }
    
    public AsyncHttpClientConfigBean setHostnameVerifier(final HostnameVerifier hostnameVerifier) {
        this.hostnameVerifier = hostnameVerifier;
        return this;
    }
    
    public AsyncHttpClientConfigBean setIoThreadMultiplier(final int ioThreadMultiplier) {
        this.ioThreadMultiplier = ioThreadMultiplier;
        return this;
    }
}
