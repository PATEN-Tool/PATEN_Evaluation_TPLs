// 
// Decompiled by Procyon v0.5.36
// 

package org.jivesoftware.util;

import java.net.URLConnection;
import java.io.OutputStream;
import java.io.DataOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import javax.servlet.ServletOutputStream;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletException;
import java.net.URL;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.jivesoftware.util.cache.CacheFactory;
import java.net.MalformedURLException;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import javax.servlet.ServletConfig;
import org.jivesoftware.util.cache.Cache;
import org.apache.commons.httpclient.HttpClient;
import javax.servlet.http.HttpServlet;

public class FaviconServlet extends HttpServlet
{
    private static final String CONTENT_TYPE = "image/x-icon";
    private byte[] defaultBytes;
    private HttpClient client;
    private Cache<String, Integer> missesCache;
    private Cache<String, byte[]> hitsCache;
    
    public void init(final ServletConfig config) throws ServletException {
        super.init(config);
        this.client = new HttpClient((HttpConnectionManager)new MultiThreadedHttpConnectionManager());
        final HttpConnectionManagerParams params = this.client.getHttpConnectionManager().getParams();
        params.setConnectionTimeout(2000);
        params.setSoTimeout(2000);
        try {
            final URL resource = config.getServletContext().getResource("/images/server_16x16.gif");
            this.defaultBytes = this.getImage(resource.toString());
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
        }
        this.missesCache = CacheFactory.createCache("Favicon Misses");
        this.hitsCache = CacheFactory.createCache("Favicon Hits");
    }
    
    public void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        String host = request.getParameter("host");
        host = ("gmail.com".equals(host) ? "google.com" : host);
        final byte[] bytes = this.getImage(host, this.defaultBytes);
        if (bytes != null) {
            this.writeBytesToStream(bytes, response);
        }
    }
    
    private void writeBytesToStream(final byte[] bytes, final HttpServletResponse response) {
        response.setContentType("image/x-icon");
        try (final ServletOutputStream sos = response.getOutputStream()) {
            sos.write(bytes);
            sos.flush();
        }
        catch (IOException ex) {}
    }
    
    private byte[] getImage(final String host, final byte[] defaultImage) {
        if (this.missesCache.get(host) != null && this.missesCache.get(host) > 1) {
            return defaultImage;
        }
        if (this.hitsCache.containsKey(host)) {
            return this.hitsCache.get(host);
        }
        byte[] bytes = this.getImage("http://" + host + "/favicon.ico");
        if (bytes == null) {
            if (this.missesCache.get(host) != null) {
                this.missesCache.put(host, 2);
            }
            else {
                this.missesCache.put(host, 1);
            }
            bytes = defaultImage;
        }
        else {
            this.hitsCache.put(host, bytes);
        }
        return bytes;
    }
    
    private byte[] getImage(final String url) {
        try {
            final GetMethod get = new GetMethod(url);
            get.setFollowRedirects(true);
            final int response = this.client.executeMethod((HttpMethod)get);
            if (response < 400) {
                return get.getResponseBody();
            }
            return null;
        }
        catch (IllegalStateException e) {
            try {
                final URLConnection urlConnection = new URL(url).openConnection();
                urlConnection.setReadTimeout(1000);
                urlConnection.connect();
                try (final DataInputStream di = new DataInputStream(urlConnection.getInputStream())) {
                    final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                    final DataOutputStream out = new DataOutputStream(byteStream);
                    final byte[] b = new byte[1024];
                    int len;
                    while ((len = di.read(b)) != -1) {
                        out.write(b, 0, len);
                    }
                    out.flush();
                    return byteStream.toByteArray();
                }
            }
            catch (IOException ioe) {
                return null;
            }
        }
        catch (IOException ioe2) {
            return null;
        }
    }
}
