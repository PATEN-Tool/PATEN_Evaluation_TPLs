// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.social.support;

import org.apache.http.conn.socket.ConnectionSocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.security.GeneralSecurityException;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.config.Registry;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.client.HttpClient;
import org.apache.http.HttpHost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.protocol.HttpContext;
import java.net.URI;
import org.springframework.http.HttpMethod;
import org.springframework.util.ClassUtils;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import java.util.Properties;
import java.net.SocketAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;

public class ClientHttpRequestFactorySelector
{
    private static final boolean HTTP_COMPONENTS_AVAILABLE;
    
    public static ClientHttpRequestFactory foo() {
        return (ClientHttpRequestFactory)new HttpComponentsClientHttpRequestFactory();
    }
    
    public static ClientHttpRequestFactory getRequestFactory() {
        final Properties properties = System.getProperties();
        final String proxyHost = properties.getProperty("http.proxyHost");
        final int proxyPort = properties.containsKey("http.proxyPort") ? Integer.valueOf(properties.getProperty("http.proxyPort")) : 80;
        if (ClientHttpRequestFactorySelector.HTTP_COMPONENTS_AVAILABLE) {
            return HttpComponentsClientRequestFactoryCreator.createRequestFactory(proxyHost, proxyPort);
        }
        final SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        if (proxyHost != null) {
            requestFactory.setProxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort)));
        }
        return (ClientHttpRequestFactory)requestFactory;
    }
    
    public static ClientHttpRequestFactory bufferRequests(final ClientHttpRequestFactory requestFactory) {
        return (ClientHttpRequestFactory)new BufferingClientHttpRequestFactory(requestFactory);
    }
    
    public static void setAllTrust(final boolean isAllTrust) {
        HttpComponentsClientRequestFactoryCreator.isAllTrust = isAllTrust;
    }
    
    static {
        HTTP_COMPONENTS_AVAILABLE = ClassUtils.isPresent("org.apache.http.client.HttpClient", ClientHttpRequestFactory.class.getClassLoader());
    }
    
    public static class HttpComponentsClientRequestFactoryCreator
    {
        private static boolean isAllTrust;
        
        public static ClientHttpRequestFactory createRequestFactory(final String proxyHost, final int proxyPort) {
            final HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory() {
                protected HttpContext createHttpContext(final HttpMethod httpMethod, final URI uri) {
                    final HttpClientContext context = new HttpClientContext();
                    context.setAttribute("http.protocol.expect-continue", (Object)false);
                    return (HttpContext)context;
                }
            };
            if (proxyHost != null) {
                final HttpHost proxy = new HttpHost(proxyHost, proxyPort);
                final CloseableHttpClient httpClient = HttpComponentsClientRequestFactoryCreator.isAllTrust ? getAllTrustClient(proxy) : getClient(proxy);
                requestFactory.setHttpClient((HttpClient)httpClient);
            }
            return (ClientHttpRequestFactory)requestFactory;
        }
        
        private static CloseableHttpClient getClient(final HttpHost proxy) {
            return HttpClients.custom().setProxy(proxy).build();
        }
        
        private static CloseableHttpClient getAllTrustClient(final HttpHost proxy) {
            try {
                final HttpClientBuilder clientBuilder = HttpClientBuilder.create();
                final SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial((KeyStore)null, (org.apache.http.ssl.TrustStrategy)new TrustStrategy() {
                    public boolean isTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
                        return true;
                    }
                }).build();
                clientBuilder.setSSLContext(sslContext);
                final HostnameVerifier hostnameVerifier = (HostnameVerifier)new NoopHostnameVerifier();
                final SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
                final Registry<ConnectionSocketFactory> socketFactoryRegistry = (Registry<ConnectionSocketFactory>)RegistryBuilder.create().register("http", (Object)PlainConnectionSocketFactory.getSocketFactory()).register("https", (Object)sslSocketFactory).build();
                final PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager((Registry)socketFactoryRegistry);
                clientBuilder.setConnectionManager((HttpClientConnectionManager)connMgr);
                return clientBuilder.build();
            }
            catch (GeneralSecurityException e) {
                throw new RuntimeException(e);
            }
        }
        
        static {
            HttpComponentsClientRequestFactoryCreator.isAllTrust = false;
        }
    }
}
