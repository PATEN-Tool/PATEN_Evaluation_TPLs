// 
// Decompiled by Procyon v0.5.36
// 

package io.hawt.web.proxy;

import org.apache.http.message.BasicHeader;
import org.slf4j.LoggerFactory;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Formatter;
import java.io.OutputStream;
import org.apache.http.HttpHost;
import java.util.Enumeration;
import org.apache.http.client.methods.CloseableHttpResponse;
import javax.servlet.http.HttpSession;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.util.EntityUtils;
import java.net.UnknownHostException;
import java.net.ConnectException;
import org.apache.http.client.methods.AbortableHttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.utils.URIUtils;
import org.apache.commons.codec.binary.Base64;
import io.hawt.util.Strings;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.HttpEntity;
import java.io.InputStream;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import java.net.URISyntaxException;
import java.net.URI;
import io.hawt.web.ServletHelpers;
import io.hawt.web.ForbiddenReason;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.apache.http.impl.client.HttpClientBuilder;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import javax.servlet.ServletException;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import java.security.KeyStore;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.BasicCookieStore;
import javax.servlet.ServletConfig;
import java.util.BitSet;
import org.apache.http.message.HeaderGroup;
import org.apache.http.client.CookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import io.hawt.system.ProxyWhitelist;
import org.slf4j.Logger;
import javax.servlet.http.HttpServlet;

public class ProxyServlet extends HttpServlet
{
    private static final long serialVersionUID = 7792226114533360114L;
    private static final transient Logger LOG;
    @Deprecated
    public static final String P_LOG = "log";
    public static final String P_FORWARDEDFOR = "forwardip";
    private static final String PROXY_ACCEPT_SELF_SIGNED_CERTS = "hawtio.proxyDisableCertificateValidation";
    private static final String PROXY_ACCEPT_SELF_SIGNED_CERTS_ENV = "PROXY_DISABLE_CERT_VALIDATION";
    public static final String PROXY_WHITELIST = "proxyWhitelist";
    public static final String HAWTIO_PROXY_WHITELIST = "hawtio.proxyWhitelist";
    protected boolean doLog;
    protected boolean doForwardIP;
    protected boolean acceptSelfSignedCerts;
    protected ProxyWhitelist whitelist;
    protected CloseableHttpClient proxyClient;
    private CookieStore cookieStore;
    protected static final HeaderGroup hopByHopHeaders;
    protected static final BitSet asciiQueryChars;
    
    public ProxyServlet() {
        this.doLog = false;
        this.doForwardIP = true;
        this.acceptSelfSignedCerts = false;
    }
    
    public String getServletInfo() {
        return "A proxy servlet by David Smiley, dsmiley@mitre.org";
    }
    
    public void init(final ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        String whitelistStr = servletConfig.getInitParameter("proxyWhitelist");
        if (System.getProperty("hawtio.proxyWhitelist") != null) {
            whitelistStr = System.getProperty("hawtio.proxyWhitelist");
        }
        this.whitelist = new ProxyWhitelist(whitelistStr);
        final String doForwardIPString = servletConfig.getInitParameter("forwardip");
        if (doForwardIPString != null) {
            this.doForwardIP = Boolean.parseBoolean(doForwardIPString);
        }
        final String doLogStr = servletConfig.getInitParameter("log");
        if (doLogStr != null) {
            this.doLog = Boolean.parseBoolean(doLogStr);
        }
        this.cookieStore = (CookieStore)new BasicCookieStore();
        final HttpClientBuilder httpClientBuilder = HttpClients.custom().setDefaultCookieStore(this.cookieStore).useSystemProperties();
        if (System.getProperty("hawtio.proxyDisableCertificateValidation") != null) {
            this.acceptSelfSignedCerts = Boolean.parseBoolean(System.getProperty("hawtio.proxyDisableCertificateValidation"));
        }
        else if (System.getenv("PROXY_DISABLE_CERT_VALIDATION") != null) {
            this.acceptSelfSignedCerts = Boolean.parseBoolean(System.getenv("PROXY_DISABLE_CERT_VALIDATION"));
        }
        if (this.acceptSelfSignedCerts) {
            try {
                final SSLContextBuilder builder = new SSLContextBuilder();
                builder.loadTrustMaterial((KeyStore)null, (x509Certificates, s) -> true);
                final SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(builder.build(), SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
                httpClientBuilder.setSSLSocketFactory((LayeredConnectionSocketFactory)sslsf);
            }
            catch (NoSuchAlgorithmException e) {
                throw new ServletException((Throwable)e);
            }
            catch (KeyStoreException e2) {
                throw new ServletException((Throwable)e2);
            }
            catch (KeyManagementException e3) {
                throw new ServletException((Throwable)e3);
            }
        }
        this.proxyClient = httpClientBuilder.build();
    }
    
    public void destroy() {
        try {
            this.proxyClient.close();
        }
        catch (IOException e) {
            this.log("While destroying servlet, shutting down httpclient: " + e, (Throwable)e);
            ProxyServlet.LOG.error("While destroying servlet, shutting down httpclient: " + e, (Throwable)e);
        }
        super.destroy();
    }
    
    protected void service(final HttpServletRequest servletRequest, final HttpServletResponse servletResponse) throws ServletException, IOException {
        final ProxyAddress proxyAddress = this.parseProxyAddress(servletRequest);
        if (proxyAddress == null || proxyAddress.getFullProxyUrl() == null) {
            servletResponse.setStatus(404);
            return;
        }
        if (proxyAddress instanceof ProxyDetails) {
            final ProxyDetails details = (ProxyDetails)proxyAddress;
            if (!this.whitelist.isAllowed(details)) {
                ProxyServlet.LOG.debug("Rejecting {}", (Object)proxyAddress);
                ServletHelpers.doForbidden(servletResponse, ForbiddenReason.HOST_NOT_ALLOWED);
                return;
            }
        }
        final String method = servletRequest.getMethod();
        final String proxyRequestUri = proxyAddress.getFullProxyUrl();
        URI targetUriObj;
        try {
            targetUriObj = new URI(proxyRequestUri);
        }
        catch (URISyntaxException e) {
            ProxyServlet.LOG.error("URL '{}' is not valid: {}", (Object)proxyRequestUri, (Object)e.getMessage());
            servletResponse.setStatus(404);
            return;
        }
        HttpRequest proxyRequest;
        if (servletRequest.getHeader("Content-Length") != null || servletRequest.getHeader("Transfer-Encoding") != null) {
            final HttpEntityEnclosingRequest eProxyRequest = (HttpEntityEnclosingRequest)new BasicHttpEntityEnclosingRequest(method, proxyRequestUri);
            eProxyRequest.setEntity((HttpEntity)new InputStreamEntity((InputStream)servletRequest.getInputStream(), (long)servletRequest.getContentLength()));
            proxyRequest = (HttpRequest)eProxyRequest;
        }
        else {
            proxyRequest = (HttpRequest)new BasicHttpRequest(method, proxyRequestUri);
        }
        this.copyRequestHeaders(servletRequest, proxyRequest, targetUriObj);
        final String username = proxyAddress.getUserName();
        final String password = proxyAddress.getPassword();
        if (Strings.isNotBlank(username) && Strings.isNotBlank(password)) {
            final String encodedCreds = Base64.encodeBase64String((username + ":" + password).getBytes());
            proxyRequest.setHeader("Authorization", "Basic " + encodedCreds);
        }
        final Header proxyAuthHeader = proxyRequest.getFirstHeader("Authorization");
        if (proxyAuthHeader != null) {
            final String proxyAuth = proxyAuthHeader.getValue();
            final HttpSession session = servletRequest.getSession();
            if (session != null) {
                final String previousProxyCredentials = (String)session.getAttribute("proxy-credentials");
                if (previousProxyCredentials != null && !previousProxyCredentials.equals(proxyAuth)) {
                    this.cookieStore.clear();
                }
                session.setAttribute("proxy-credentials", (Object)proxyAuth);
            }
        }
        this.setXForwardedForHeader(servletRequest, proxyRequest);
        CloseableHttpResponse proxyResponse = null;
        int statusCode = 0;
        try {
            if (this.doLog) {
                this.log("proxy " + method + " uri: " + servletRequest.getRequestURI() + " -- " + proxyRequest.getRequestLine().getUri());
            }
            ProxyServlet.LOG.debug("proxy {} uri: {} -- {}", new Object[] { method, servletRequest.getRequestURI(), proxyRequest.getRequestLine().getUri() });
            proxyResponse = this.proxyClient.execute(URIUtils.extractHost(targetUriObj), proxyRequest);
            statusCode = proxyResponse.getStatusLine().getStatusCode();
            if (statusCode == 401 || statusCode == 403) {
                if (this.doLog) {
                    this.log("Authentication Failed on remote server " + proxyRequestUri);
                }
                ProxyServlet.LOG.debug("Authentication Failed on remote server {}", (Object)proxyRequestUri);
            }
            else if (this.doResponseRedirectOrNotModifiedLogic(servletRequest, servletResponse, (HttpResponse)proxyResponse, statusCode, targetUriObj)) {
                return;
            }
            servletResponse.setStatus(statusCode, proxyResponse.getStatusLine().getReasonPhrase());
            this.copyResponseHeaders((HttpResponse)proxyResponse, servletResponse);
            this.copyResponseEntity((HttpResponse)proxyResponse, servletResponse);
        }
        catch (Exception e2) {
            if (proxyRequest instanceof AbortableHttpRequest) {
                final AbortableHttpRequest abortableHttpRequest = (AbortableHttpRequest)proxyRequest;
                abortableHttpRequest.abort();
            }
            ProxyServlet.LOG.debug("Proxy to " + proxyRequestUri + " failed", (Throwable)e2);
            if (e2 instanceof ConnectException || e2 instanceof UnknownHostException) {
                servletResponse.setStatus(404);
            }
            else if (e2 instanceof ServletException) {
                servletResponse.sendError(502, e2.getMessage());
            }
            else if (e2 instanceof SecurityException) {
                servletResponse.setHeader("WWW-Authenticate", "Basic");
                servletResponse.sendError(statusCode, e2.getMessage());
            }
            else {
                servletResponse.sendError(500, e2.getMessage());
            }
            if (proxyResponse != null) {
                EntityUtils.consumeQuietly(proxyResponse.getEntity());
                try {
                    proxyResponse.close();
                }
                catch (IOException e3) {
                    ProxyServlet.LOG.error("Error closing proxy client response: {}", (Object)e3.getMessage());
                }
            }
        }
        finally {
            if (proxyResponse != null) {
                EntityUtils.consumeQuietly(proxyResponse.getEntity());
                try {
                    proxyResponse.close();
                }
                catch (IOException e4) {
                    ProxyServlet.LOG.error("Error closing proxy client response: {}", (Object)e4.getMessage());
                }
            }
        }
    }
    
    protected ProxyAddress parseProxyAddress(final HttpServletRequest servletRequest) {
        return new ProxyDetails(servletRequest);
    }
    
    protected boolean doResponseRedirectOrNotModifiedLogic(final HttpServletRequest servletRequest, final HttpServletResponse servletResponse, final HttpResponse proxyResponse, final int statusCode, final URI targetUriObj) throws ServletException, IOException {
        if (statusCode >= 300 && statusCode < 304) {
            final Header locationHeader = proxyResponse.getLastHeader("Location");
            if (locationHeader == null) {
                throw new ServletException("Received status code: " + statusCode + " but no " + "Location" + " header was found in the response");
            }
            final String locStr = this.rewriteUrlFromResponse(servletRequest, locationHeader.getValue(), targetUriObj.toString());
            servletResponse.sendRedirect(locStr);
            return true;
        }
        else {
            if (statusCode == 304) {
                servletResponse.setIntHeader("Content-Length", 0);
                servletResponse.setStatus(304);
                return true;
            }
            return false;
        }
    }
    
    protected void copyRequestHeaders(final HttpServletRequest servletRequest, final HttpRequest proxyRequest, final URI targetUriObj) {
        final Enumeration enumerationOfHeaderNames = servletRequest.getHeaderNames();
        while (enumerationOfHeaderNames.hasMoreElements()) {
            final String headerName = enumerationOfHeaderNames.nextElement();
            if (headerName.equalsIgnoreCase("Content-Length")) {
                continue;
            }
            if (ProxyServlet.hopByHopHeaders.containsHeader(headerName)) {
                continue;
            }
            final Enumeration headers = servletRequest.getHeaders(headerName);
            while (headers.hasMoreElements()) {
                String headerValue = headers.nextElement();
                if (headerName.equalsIgnoreCase("Host")) {
                    final HttpHost host = URIUtils.extractHost(targetUriObj);
                    if (host != null) {
                        headerValue = host.getHostName();
                        if (headerValue != null && host.getPort() != -1) {
                            headerValue = headerValue + ":" + host.getPort();
                        }
                    }
                }
                proxyRequest.addHeader(headerName, headerValue);
            }
        }
    }
    
    private void setXForwardedForHeader(final HttpServletRequest servletRequest, final HttpRequest proxyRequest) {
        final String headerName = "X-Forwarded-For";
        if (this.doForwardIP) {
            String newHeader = servletRequest.getRemoteAddr();
            final String existingHeader = servletRequest.getHeader(headerName);
            if (existingHeader != null) {
                newHeader = existingHeader + ", " + newHeader;
            }
            proxyRequest.setHeader(headerName, newHeader);
        }
    }
    
    protected void copyResponseHeaders(final HttpResponse proxyResponse, final HttpServletResponse servletResponse) {
        for (final Header header : proxyResponse.getAllHeaders()) {
            if (!ProxyServlet.hopByHopHeaders.containsHeader(header.getName())) {
                servletResponse.addHeader(header.getName(), header.getValue());
            }
        }
    }
    
    protected void copyResponseEntity(final HttpResponse proxyResponse, final HttpServletResponse servletResponse) throws IOException {
        final HttpEntity entity = proxyResponse.getEntity();
        if (entity != null) {
            final OutputStream servletOutputStream = (OutputStream)servletResponse.getOutputStream();
            entity.writeTo(servletOutputStream);
        }
    }
    
    protected String rewriteUrlFromResponse(final HttpServletRequest servletRequest, String theUrl, final String targetUri) {
        if (theUrl.startsWith(targetUri)) {
            String curUrl = servletRequest.getRequestURL().toString();
            final String pathInfo = servletRequest.getPathInfo();
            if (pathInfo != null) {
                assert curUrl.endsWith(pathInfo);
                curUrl = curUrl.substring(0, curUrl.length() - pathInfo.length());
            }
            theUrl = curUrl + theUrl.substring(targetUri.length());
        }
        return theUrl;
    }
    
    protected static CharSequence encodeUriQuery(final CharSequence in) {
        StringBuilder outBuf = null;
        Formatter formatter = null;
        for (int i = 0; i < in.length(); ++i) {
            final char c = in.charAt(i);
            boolean escape = true;
            if (c < '\u0080') {
                if (ProxyServlet.asciiQueryChars.get(c)) {
                    escape = false;
                }
            }
            else if (!Character.isISOControl(c) && !Character.isSpaceChar(c)) {
                escape = false;
            }
            if (!escape) {
                if (outBuf != null) {
                    outBuf.append(c);
                }
            }
            else {
                if (outBuf == null) {
                    outBuf = new StringBuilder(in.length() + 15);
                    outBuf.append(in, 0, i);
                    formatter = new Formatter(outBuf);
                }
                formatter.format("%%%02X", (int)c);
            }
        }
        return (outBuf != null) ? outBuf : in;
    }
    
    static {
        LOG = LoggerFactory.getLogger((Class)ProxyServlet.class);
        hopByHopHeaders = new HeaderGroup();
        final String[] array;
        final String[] headers = array = new String[] { "Connection", "Keep-Alive", "Proxy-Authenticate", "Proxy-Authorization", "TE", "Trailers", "Transfer-Encoding", "Upgrade", "Cookie", "Set-Cookie" };
        for (final String header : array) {
            ProxyServlet.hopByHopHeaders.addHeader((Header)new BasicHeader(header, (String)null));
        }
        final char[] c_unreserved = "_-!.~'()*".toCharArray();
        final char[] c_punct = ",;:$&+=".toCharArray();
        final char[] c_reserved = "?/[]@".toCharArray();
        asciiQueryChars = new BitSet(128);
        for (char c = 'a'; c <= 'z'; ++c) {
            ProxyServlet.asciiQueryChars.set(c);
        }
        for (char c = 'A'; c <= 'Z'; ++c) {
            ProxyServlet.asciiQueryChars.set(c);
        }
        for (char c = '0'; c <= '9'; ++c) {
            ProxyServlet.asciiQueryChars.set(c);
        }
        for (final char c2 : c_unreserved) {
            ProxyServlet.asciiQueryChars.set(c2);
        }
        for (final char c2 : c_punct) {
            ProxyServlet.asciiQueryChars.set(c2);
        }
        for (final char c2 : c_reserved) {
            ProxyServlet.asciiQueryChars.set(c2);
        }
        ProxyServlet.asciiQueryChars.set(37);
    }
}
