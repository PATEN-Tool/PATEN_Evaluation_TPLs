// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.web.util;

import org.apache.commons.logging.LogFactory;
import java.util.Properties;
import java.util.List;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import org.springframework.util.StringUtils;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.logging.Log;

public class UrlPathHelper
{
    private static final String WEBSPHERE_URI_ATTRIBUTE = "com.ibm.websphere.servlet.uri_non_decoded";
    private static final Log logger;
    static volatile Boolean websphereComplianceFlag;
    private boolean alwaysUseFullPath;
    private boolean urlDecode;
    private boolean removeSemicolonContent;
    private String defaultEncoding;
    
    public UrlPathHelper() {
        this.alwaysUseFullPath = false;
        this.urlDecode = true;
        this.removeSemicolonContent = true;
        this.defaultEncoding = "ISO-8859-1";
    }
    
    public void setAlwaysUseFullPath(final boolean alwaysUseFullPath) {
        this.alwaysUseFullPath = alwaysUseFullPath;
    }
    
    public void setUrlDecode(final boolean urlDecode) {
        this.urlDecode = urlDecode;
    }
    
    public boolean isUrlDecode() {
        return this.urlDecode;
    }
    
    public void setRemoveSemicolonContent(final boolean removeSemicolonContent) {
        this.removeSemicolonContent = removeSemicolonContent;
    }
    
    public boolean shouldRemoveSemicolonContent() {
        return this.removeSemicolonContent;
    }
    
    public void setDefaultEncoding(final String defaultEncoding) {
        this.defaultEncoding = defaultEncoding;
    }
    
    protected String getDefaultEncoding() {
        return this.defaultEncoding;
    }
    
    public String getLookupPathForRequest(final HttpServletRequest request) {
        if (this.alwaysUseFullPath) {
            return this.getPathWithinApplication(request);
        }
        final String rest = this.getPathWithinServletMapping(request);
        if (!"".equals(rest)) {
            return rest;
        }
        return this.getPathWithinApplication(request);
    }
    
    public String getPathWithinServletMapping(final HttpServletRequest request) {
        final String pathWithinApp = this.getPathWithinApplication(request);
        final String servletPath = this.getServletPath(request);
        final String sanitizedPathWithinApp = this.getSanitizedPath(pathWithinApp);
        String path;
        if (servletPath.contains(sanitizedPathWithinApp)) {
            path = this.getRemainingPath(sanitizedPathWithinApp, servletPath, false);
        }
        else {
            path = this.getRemainingPath(pathWithinApp, servletPath, false);
        }
        if (path != null) {
            return path;
        }
        final String pathInfo = request.getPathInfo();
        if (pathInfo != null) {
            return pathInfo;
        }
        if (!this.urlDecode) {
            path = this.getRemainingPath(this.decodeInternal(request, pathWithinApp), servletPath, false);
            if (path != null) {
                return pathWithinApp;
            }
        }
        return servletPath;
    }
    
    public String getPathWithinApplication(final HttpServletRequest request) {
        final String contextPath = this.getContextPath(request);
        final String requestUri = this.getRequestUri(request);
        final String path = this.getRemainingPath(requestUri, contextPath, true);
        if (path != null) {
            return StringUtils.hasText(path) ? path : "/";
        }
        return requestUri;
    }
    
    private String getRemainingPath(final String requestUri, final String mapping, final boolean ignoreCase) {
        int index1;
        int index2;
        for (index1 = 0, index2 = 0; index1 < requestUri.length() && index2 < mapping.length(); ++index1, ++index2) {
            char c1 = requestUri.charAt(index1);
            final char c2 = mapping.charAt(index2);
            if (c1 == ';') {
                index1 = requestUri.indexOf(47, index1);
                if (index1 == -1) {
                    return null;
                }
                c1 = requestUri.charAt(index1);
            }
            if (c1 != c2) {
                if (!ignoreCase || Character.toLowerCase(c1) != Character.toLowerCase(c2)) {
                    return null;
                }
            }
        }
        if (index2 != mapping.length()) {
            return null;
        }
        if (index1 == requestUri.length()) {
            return "";
        }
        if (requestUri.charAt(index1) == ';') {
            index1 = requestUri.indexOf(47, index1);
        }
        return (index1 != -1) ? requestUri.substring(index1) : "";
    }
    
    private String getSanitizedPath(final String path) {
        String sanitized = path;
        while (true) {
            final int index = sanitized.indexOf("//");
            if (index < 0) {
                break;
            }
            sanitized = sanitized.substring(0, index) + sanitized.substring(index + 1);
        }
        return sanitized;
    }
    
    public String getRequestUri(final HttpServletRequest request) {
        String uri = (String)request.getAttribute("javax.servlet.include.request_uri");
        if (uri == null) {
            uri = request.getRequestURI();
        }
        return this.decodeAndCleanUriString(request, uri);
    }
    
    public String getContextPath(final HttpServletRequest request) {
        String contextPath = (String)request.getAttribute("javax.servlet.include.context_path");
        if (contextPath == null) {
            contextPath = request.getContextPath();
        }
        if ("/".equals(contextPath)) {
            contextPath = "";
        }
        return this.decodeRequestString(request, contextPath);
    }
    
    public String getServletPath(final HttpServletRequest request) {
        String servletPath = (String)request.getAttribute("javax.servlet.include.servlet_path");
        if (servletPath == null) {
            servletPath = request.getServletPath();
        }
        if (servletPath.length() > 1 && servletPath.endsWith("/") && this.shouldRemoveTrailingServletPathSlash(request)) {
            servletPath = servletPath.substring(0, servletPath.length() - 1);
        }
        return servletPath;
    }
    
    public String getOriginatingRequestUri(final HttpServletRequest request) {
        String uri = (String)request.getAttribute("com.ibm.websphere.servlet.uri_non_decoded");
        if (uri == null) {
            uri = (String)request.getAttribute("javax.servlet.forward.request_uri");
            if (uri == null) {
                uri = request.getRequestURI();
            }
        }
        return this.decodeAndCleanUriString(request, uri);
    }
    
    public String getOriginatingContextPath(final HttpServletRequest request) {
        String contextPath = (String)request.getAttribute("javax.servlet.forward.context_path");
        if (contextPath == null) {
            contextPath = request.getContextPath();
        }
        return this.decodeRequestString(request, contextPath);
    }
    
    public String getOriginatingServletPath(final HttpServletRequest request) {
        String servletPath = (String)request.getAttribute("javax.servlet.forward.servlet_path");
        if (servletPath == null) {
            servletPath = request.getServletPath();
        }
        return servletPath;
    }
    
    public String getOriginatingQueryString(final HttpServletRequest request) {
        if (request.getAttribute("javax.servlet.forward.request_uri") != null || request.getAttribute("javax.servlet.error.request_uri") != null) {
            return (String)request.getAttribute("javax.servlet.forward.query_string");
        }
        return request.getQueryString();
    }
    
    private String decodeAndCleanUriString(final HttpServletRequest request, String uri) {
        uri = this.removeSemicolonContent(uri);
        uri = this.decodeRequestString(request, uri);
        uri = this.getSanitizedPath(uri);
        return uri;
    }
    
    public String decodeRequestString(final HttpServletRequest request, final String source) {
        if (this.urlDecode && source != null) {
            return this.decodeInternal(request, source);
        }
        return source;
    }
    
    private String decodeInternal(final HttpServletRequest request, final String source) {
        final String enc = this.determineEncoding(request);
        try {
            return UriUtils.decode(source, enc);
        }
        catch (UnsupportedEncodingException ex) {
            if (UrlPathHelper.logger.isWarnEnabled()) {
                UrlPathHelper.logger.warn((Object)("Could not decode request string [" + source + "] with encoding '" + enc + "': falling back to platform default encoding; exception message: " + ex.getMessage()));
            }
            return URLDecoder.decode(source);
        }
    }
    
    protected String determineEncoding(final HttpServletRequest request) {
        String enc = request.getCharacterEncoding();
        if (enc == null) {
            enc = this.getDefaultEncoding();
        }
        return enc;
    }
    
    public String removeSemicolonContent(final String requestUri) {
        return this.removeSemicolonContent ? this.removeSemicolonContentInternal(requestUri) : this.removeJsessionid(requestUri);
    }
    
    private String removeSemicolonContentInternal(String requestUri) {
        for (int semicolonIndex = requestUri.indexOf(59); semicolonIndex != -1; semicolonIndex = requestUri.indexOf(59, semicolonIndex)) {
            final int slashIndex = requestUri.indexOf(47, semicolonIndex);
            final String start = requestUri.substring(0, semicolonIndex);
            requestUri = ((slashIndex != -1) ? (start + requestUri.substring(slashIndex)) : start);
        }
        return requestUri;
    }
    
    private String removeJsessionid(String requestUri) {
        final int startIndex = requestUri.toLowerCase().indexOf(";jsessionid=");
        if (startIndex != -1) {
            final int endIndex = requestUri.indexOf(59, startIndex + 12);
            final String start = requestUri.substring(0, startIndex);
            requestUri = ((endIndex != -1) ? (start + requestUri.substring(endIndex)) : start);
        }
        return requestUri;
    }
    
    public Map<String, String> decodePathVariables(final HttpServletRequest request, final Map<String, String> vars) {
        if (this.urlDecode) {
            return vars;
        }
        final Map<String, String> decodedVars = new LinkedHashMap<String, String>(vars.size());
        for (final Map.Entry<String, String> entry : vars.entrySet()) {
            decodedVars.put(entry.getKey(), this.decodeInternal(request, entry.getValue()));
        }
        return decodedVars;
    }
    
    public MultiValueMap<String, String> decodeMatrixVariables(final HttpServletRequest request, final MultiValueMap<String, String> vars) {
        if (this.urlDecode) {
            return vars;
        }
        final MultiValueMap<String, String> decodedVars = (MultiValueMap<String, String>)new LinkedMultiValueMap(vars.size());
        for (final String key : vars.keySet()) {
            for (final String value : (List)vars.get((Object)key)) {
                decodedVars.add((Object)key, (Object)this.decodeInternal(request, value));
            }
        }
        return decodedVars;
    }
    
    private boolean shouldRemoveTrailingServletPathSlash(final HttpServletRequest request) {
        if (request.getAttribute("com.ibm.websphere.servlet.uri_non_decoded") == null) {
            return false;
        }
        if (UrlPathHelper.websphereComplianceFlag == null) {
            final ClassLoader classLoader = UrlPathHelper.class.getClassLoader();
            final String className = "com.ibm.ws.webcontainer.WebContainer";
            final String methodName = "getWebContainerProperties";
            final String propName = "com.ibm.ws.webcontainer.removetrailingservletpathslash";
            boolean flag = false;
            try {
                final Class<?> cl = classLoader.loadClass(className);
                final Properties prop = (Properties)cl.getMethod(methodName, (Class<?>[])new Class[0]).invoke(null, new Object[0]);
                flag = Boolean.parseBoolean(prop.getProperty(propName));
            }
            catch (Throwable ex) {
                if (UrlPathHelper.logger.isDebugEnabled()) {
                    UrlPathHelper.logger.debug((Object)("Could not introspect WebSphere web container properties: " + ex));
                }
            }
            UrlPathHelper.websphereComplianceFlag = flag;
        }
        return !UrlPathHelper.websphereComplianceFlag;
    }
    
    static {
        logger = LogFactory.getLog((Class)UrlPathHelper.class);
    }
}
