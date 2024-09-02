// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.social.support;

import java.security.NoSuchAlgorithmException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import org.apache.http.conn.ssl.SSLContexts;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import org.apache.http.conn.ssl.TrustStrategy;
import java.security.KeyStore;
import javax.net.ssl.SSLContext;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
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
            return HttpClients.custom().setProxy(proxy).setSslcontext(getSSLContext()).setHostnameVerifier(SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER).build();
        }
        
        private static SSLContext getSSLContext() {
            try {
                final KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                final TrustStrategy allTrust = (TrustStrategy)new TrustStrategy() {
                    public boolean isTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
                        return true;
                    }
                };
                return SSLContexts.custom().useSSL().loadTrustMaterial(trustStore, allTrust).build();
            }
            catch (KeyStoreException e) {
                e.printStackTrace();
            }
            catch (KeyManagementException e2) {
                e2.printStackTrace();
            }
            catch (NoSuchAlgorithmException e3) {
                e3.printStackTrace();
            }
            return null;
        }
        
        static {
            HttpComponentsClientRequestFactoryCreator.isAllTrust = false;
        }
    }
}
