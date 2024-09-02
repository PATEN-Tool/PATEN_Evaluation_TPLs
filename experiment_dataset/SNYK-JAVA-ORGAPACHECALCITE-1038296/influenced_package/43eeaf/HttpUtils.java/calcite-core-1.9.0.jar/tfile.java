// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.calcite.runtime;

import org.slf4j.LoggerFactory;
import java.io.OutputStreamWriter;
import java.io.InputStream;
import java.io.Closeable;
import java.util.Iterator;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.io.IOException;
import java.net.URLConnection;
import javax.net.ssl.SSLSession;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import java.net.URL;
import java.net.HttpURLConnection;
import org.slf4j.Logger;

public class HttpUtils
{
    private static final Logger LOGGER;
    
    private HttpUtils() {
    }
    
    public static HttpURLConnection getURLConnection(final String url) throws IOException {
        final URLConnection conn = new URL(url).openConnection();
        final HttpURLConnection httpConn = (HttpURLConnection)conn;
        if (httpConn instanceof HttpsURLConnection) {
            final HttpsURLConnection httpsConn = (HttpsURLConnection)httpConn;
            httpsConn.setSSLSocketFactory(TrustAllSslSocketFactory.createSSLSocketFactory());
            httpsConn.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(final String arg0, final SSLSession arg1) {
                    return true;
                }
            });
        }
        return httpConn;
    }
    
    public static void appendURLEncodedArgs(final StringBuilder out, final Map<String, String> args) {
        int i = 0;
        try {
            for (final Map.Entry<String, String> me : args.entrySet()) {
                if (i++ != 0) {
                    out.append("&");
                }
                out.append(URLEncoder.encode(me.getKey(), "UTF-8"));
                out.append("=").append(URLEncoder.encode(me.getValue(), "UTF-8"));
            }
        }
        catch (UnsupportedEncodingException ex) {}
    }
    
    public static void appendURLEncodedArgs(final StringBuilder out, final CharSequence... args) {
        if (args.length % 2 != 0) {
            throw new IllegalArgumentException("args should contain an even number of items");
        }
        try {
            int appended = 0;
            for (int i = 0; i < args.length; i += 2) {
                if (args[i + 1] != null) {
                    if (appended++ > 0) {
                        out.append("&");
                    }
                    out.append(URLEncoder.encode(args[i].toString(), "UTF-8")).append("=").append(URLEncoder.encode(args[i + 1].toString(), "UTF-8"));
                }
            }
        }
        catch (UnsupportedEncodingException ex) {}
    }
    
    public static void close(final Closeable c) {
        try {
            c.close();
        }
        catch (Exception ex) {}
    }
    
    public static InputStream post(final String url, final CharSequence data, final Map<String, String> headers) throws IOException {
        return post(url, data, headers, 10000, 60000);
    }
    
    public static InputStream post(final String url, final CharSequence data, final Map<String, String> headers, final int cTimeout, final int rTimeout) throws IOException {
        return executeMethod((data == null) ? "GET" : "POST", url, data, headers, cTimeout, rTimeout);
    }
    
    public static InputStream executeMethod(final String method, final String url, final CharSequence data, final Map<String, String> headers, final int ctimeout, final int rtimeout) throws IOException {
        OutputStreamWriter wr = null;
        try {
            final HttpURLConnection conn = getURLConnection(url);
            conn.setRequestMethod(method);
            conn.setReadTimeout(rtimeout);
            conn.setConnectTimeout(ctimeout);
            if (headers != null) {
                for (final Map.Entry<String, String> me : headers.entrySet()) {
                    conn.setRequestProperty(me.getKey(), me.getValue());
                }
            }
            if (data != null) {
                conn.setDoOutput(true);
                wr = new OutputStreamWriter(conn.getOutputStream());
                wr.write(data.toString());
                wr.flush();
            }
            final InputStream in = conn.getInputStream();
            if (wr != null) {
                wr.close();
            }
            HttpUtils.LOGGER.debug("url: {}, data: {}", (Object)url, (Object)String.valueOf(data));
            return in;
        }
        finally {
            close(wr);
        }
    }
    
    static {
        LOGGER = LoggerFactory.getLogger((Class)HttpUtils.class);
    }
}
