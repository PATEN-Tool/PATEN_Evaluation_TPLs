// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.beam.sdk.io.mongodb;

import java.security.cert.X509Certificate;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.KeyManagerFactory;
import java.security.KeyStore;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import java.security.SecureRandom;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

public class SSLUtils
{
    static TrustManager[] trustAllCerts;
    
    public static SSLContext ignoreSSLCertificate() {
        try {
            final SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, SSLUtils.trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            final KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(SSLUtils.class.getClassLoader().getResourceAsStream("resources/.keystore"), "changeit".toCharArray());
            final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, "changeit".toCharArray());
            final SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(kmf.getKeyManagers(), SSLUtils.trustAllCerts, null);
            SSLContext.setDefault(ctx);
            return ctx;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    static {
        SSLUtils.trustAllCerts = new TrustManager[] { new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
                
                @Override
                public void checkClientTrusted(final X509Certificate[] certs, final String authType) {
                }
                
                @Override
                public void checkServerTrusted(final X509Certificate[] certs, final String authType) {
                }
            } };
    }
}
