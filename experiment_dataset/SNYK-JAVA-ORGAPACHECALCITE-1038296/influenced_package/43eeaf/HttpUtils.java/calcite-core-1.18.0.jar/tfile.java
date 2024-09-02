// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.calcite.runtime;

import javax.net.ssl.SSLSession;
import java.io.Writer;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import java.util.Iterator;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.io.IOException;
import java.net.URLConnection;
import javax.net.ssl.HttpsURLConnection;
import java.net.URL;
import java.net.HttpURLConnection;

public class HttpUtils
{
    private HttpUtils() {
    }
    
    public static HttpURLConnection getURLConnection(final String url) throws IOException {
        final URLConnection conn = new URL(url).openConnection();
        final HttpURLConnection httpConn = (HttpURLConnection)conn;
        if (httpConn instanceof HttpsURLConnection) {
            final HttpsURLConnection httpsConn = (HttpsURLConnection)httpConn;
            httpsConn.setSSLSocketFactory(TrustAllSslSocketFactory.createSSLSocketFactory());
            httpsConn.setHostnameVerifier((arg0, arg1) -> true);
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
                out.append(URLEncoder.encode(me.getKey(), "UTF-8")).append("=").append(URLEncoder.encode(me.getValue(), "UTF-8"));
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
    
    public static InputStream post(final String url, final CharSequence data, final Map<String, String> headers) throws IOException {
        return post(url, data, headers, 10000, 60000);
    }
    
    public static InputStream post(final String url, final CharSequence data, final Map<String, String> headers, final int cTimeout, final int rTimeout) throws IOException {
        return executeMethod((data == null) ? "GET" : "POST", url, data, headers, cTimeout, rTimeout);
    }
    
    public static InputStream executeMethod(final String method, final String url, final CharSequence data, final Map<String, String> headers, final int cTimeout, final int rTimeout) throws IOException {
        final HttpURLConnection conn = getURLConnection(url);
        conn.setRequestMethod(method);
        conn.setReadTimeout(rTimeout);
        conn.setConnectTimeout(cTimeout);
        if (headers != null) {
            for (final Map.Entry<String, String> me : headers.entrySet()) {
                conn.setRequestProperty(me.getKey(), me.getValue());
            }
        }
        if (data == null) {
            return conn.getInputStream();
        }
        conn.setDoOutput(true);
        final Writer w = new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8);
        Throwable t = null;
        try {
            w.write(data.toString());
            w.flush();
            return conn.getInputStream();
        }
        catch (Throwable t2) {
            t = t2;
            throw t2;
        }
        finally {
            if (t != null) {
                try {
                    w.close();
                }
                catch (Throwable t3) {
                    t.addSuppressed(t3);
                }
            }
            else {
                w.close();
            }
        }
    }
}
