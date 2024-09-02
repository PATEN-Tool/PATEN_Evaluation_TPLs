// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.shiro.web.util;

import org.slf4j.LoggerFactory;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.util.StringUtils;
import java.io.IOException;
import java.util.Map;
import org.apache.shiro.subject.support.DefaultSubjectContext;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletResponse;
import javax.servlet.ServletRequest;
import java.io.UnsupportedEncodingException;
import org.owasp.encoder.Encode;
import java.net.URLDecoder;
import org.apache.shiro.web.env.EnvironmentLoader;
import org.apache.shiro.web.env.WebEnvironment;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;

public class WebUtils
{
    private static final Logger log;
    public static final String SERVLET_REQUEST_KEY;
    public static final String SERVLET_RESPONSE_KEY;
    public static final String ALLOW_BACKSLASH = "org.apache.shiro.web.ALLOW_BACKSLASH";
    public static final String SAVED_REQUEST_KEY = "shiroSavedRequest";
    public static final String INCLUDE_REQUEST_URI_ATTRIBUTE = "javax.servlet.include.request_uri";
    public static final String INCLUDE_CONTEXT_PATH_ATTRIBUTE = "javax.servlet.include.context_path";
    public static final String INCLUDE_SERVLET_PATH_ATTRIBUTE = "javax.servlet.include.servlet_path";
    public static final String INCLUDE_PATH_INFO_ATTRIBUTE = "javax.servlet.include.path_info";
    public static final String INCLUDE_QUERY_STRING_ATTRIBUTE = "javax.servlet.include.query_string";
    public static final String FORWARD_REQUEST_URI_ATTRIBUTE = "javax.servlet.forward.request_uri";
    public static final String FORWARD_CONTEXT_PATH_ATTRIBUTE = "javax.servlet.forward.context_path";
    public static final String FORWARD_SERVLET_PATH_ATTRIBUTE = "javax.servlet.forward.servlet_path";
    public static final String FORWARD_PATH_INFO_ATTRIBUTE = "javax.servlet.forward.path_info";
    public static final String FORWARD_QUERY_STRING_ATTRIBUTE = "javax.servlet.forward.query_string";
    public static final String DEFAULT_CHARACTER_ENCODING = "ISO-8859-1";
    
    public static String getPathWithinApplication(final HttpServletRequest request) {
        return normalize(removeSemicolon(getServletPath(request) + getPathInfo(request)));
    }
    
    @Deprecated
    public static String getRequestUri(final HttpServletRequest request) {
        String uri = (String)request.getAttribute("javax.servlet.include.request_uri");
        if (uri == null) {
            uri = request.getRequestURI();
        }
        return normalize(decodeAndCleanUriString(request, uri));
    }
    
    private static String getServletPath(final HttpServletRequest request) {
        final String servletPath = (String)request.getAttribute("javax.servlet.include.servlet_path");
        return (servletPath != null) ? servletPath : valueOrEmpty(request.getServletPath());
    }
    
    private static String getPathInfo(final HttpServletRequest request) {
        final String pathInfo = (String)request.getAttribute("javax.servlet.include.path_info");
        return (pathInfo != null) ? pathInfo : valueOrEmpty(request.getPathInfo());
    }
    
    private static String valueOrEmpty(final String input) {
        if (input == null) {
            return "";
        }
        return input;
    }
    
    public static String normalize(final String path) {
        return normalize(path, Boolean.getBoolean("org.apache.shiro.web.ALLOW_BACKSLASH"));
    }
    
    private static String normalize(final String path, final boolean replaceBackSlash) {
        if (path == null) {
            return null;
        }
        String normalized = path;
        if (replaceBackSlash && normalized.indexOf(92) >= 0) {
            normalized = normalized.replace('\\', '/');
        }
        if (normalized.equals("/.")) {
            return "/";
        }
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        while (true) {
            final int index = normalized.indexOf("//");
            if (index < 0) {
                break;
            }
            normalized = normalized.substring(0, index) + normalized.substring(index + 1);
        }
        while (true) {
            final int index = normalized.indexOf("/./");
            if (index < 0) {
                break;
            }
            normalized = normalized.substring(0, index) + normalized.substring(index + 2);
        }
        while (true) {
            final int index = normalized.indexOf("/../");
            if (index < 0) {
                return normalized;
            }
            if (index == 0) {
                return null;
            }
            final int index2 = normalized.lastIndexOf(47, index - 1);
            normalized = normalized.substring(0, index2) + normalized.substring(index + 3);
        }
    }
    
    private static String decodeAndCleanUriString(final HttpServletRequest request, String uri) {
        uri = decodeRequestString(request, uri);
        return removeSemicolon(uri);
    }
    
    private static String removeSemicolon(final String uri) {
        final int semicolonIndex = uri.indexOf(59);
        return (semicolonIndex != -1) ? uri.substring(0, semicolonIndex) : uri;
    }
    
    public static String getContextPath(final HttpServletRequest request) {
        String contextPath = (String)request.getAttribute("javax.servlet.include.context_path");
        if (contextPath == null) {
            contextPath = request.getContextPath();
        }
        contextPath = normalize(decodeRequestString(request, contextPath));
        if ("/".equals(contextPath)) {
            contextPath = "";
        }
        return contextPath;
    }
    
    public static WebEnvironment getRequiredWebEnvironment(final ServletContext sc) throws IllegalStateException {
        final WebEnvironment we = getWebEnvironment(sc);
        if (we == null) {
            throw new IllegalStateException("No WebEnvironment found: no EnvironmentLoaderListener registered?");
        }
        return we;
    }
    
    public static WebEnvironment getWebEnvironment(final ServletContext sc) {
        return getWebEnvironment(sc, EnvironmentLoader.ENVIRONMENT_ATTRIBUTE_KEY);
    }
    
    public static WebEnvironment getWebEnvironment(final ServletContext sc, final String attrName) {
        if (sc == null) {
            throw new IllegalArgumentException("ServletContext argument must not be null.");
        }
        final Object attr = sc.getAttribute(attrName);
        if (attr == null) {
            return null;
        }
        if (attr instanceof RuntimeException) {
            throw (RuntimeException)attr;
        }
        if (attr instanceof Error) {
            throw (Error)attr;
        }
        if (attr instanceof Exception) {
            throw new IllegalStateException((Throwable)attr);
        }
        if (!(attr instanceof WebEnvironment)) {
            throw new IllegalStateException("Context attribute is not of type WebEnvironment: " + attr);
        }
        return (WebEnvironment)attr;
    }
    
    public static String decodeRequestString(final HttpServletRequest request, final String source) {
        final String enc = determineEncoding(request);
        try {
            return URLDecoder.decode(source, enc);
        }
        catch (UnsupportedEncodingException ex) {
            if (WebUtils.log.isWarnEnabled()) {
                WebUtils.log.warn("Could not decode request string [" + Encode.forHtml(source) + "] with encoding '" + Encode.forHtml(enc) + "': falling back to platform default encoding; exception message: " + ex.getMessage());
            }
            return URLDecoder.decode(source);
        }
    }
    
    protected static String determineEncoding(final HttpServletRequest request) {
        String enc = request.getCharacterEncoding();
        if (enc == null) {
            enc = "ISO-8859-1";
        }
        return enc;
    }
    
    public static boolean isWeb(final Object requestPairSource) {
        return requestPairSource instanceof RequestPairSource && isWeb((RequestPairSource)requestPairSource);
    }
    
    public static boolean isHttp(final Object requestPairSource) {
        return requestPairSource instanceof RequestPairSource && isHttp((RequestPairSource)requestPairSource);
    }
    
    public static ServletRequest getRequest(final Object requestPairSource) {
        if (requestPairSource instanceof RequestPairSource) {
            return ((RequestPairSource)requestPairSource).getServletRequest();
        }
        return null;
    }
    
    public static ServletResponse getResponse(final Object requestPairSource) {
        if (requestPairSource instanceof RequestPairSource) {
            return ((RequestPairSource)requestPairSource).getServletResponse();
        }
        return null;
    }
    
    public static HttpServletRequest getHttpRequest(final Object requestPairSource) {
        final ServletRequest request = getRequest(requestPairSource);
        if (request instanceof HttpServletRequest) {
            return (HttpServletRequest)request;
        }
        return null;
    }
    
    public static HttpServletResponse getHttpResponse(final Object requestPairSource) {
        final ServletResponse response = getResponse(requestPairSource);
        if (response instanceof HttpServletResponse) {
            return (HttpServletResponse)response;
        }
        return null;
    }
    
    private static boolean isWeb(final RequestPairSource source) {
        final ServletRequest request = source.getServletRequest();
        final ServletResponse response = source.getServletResponse();
        return request != null && response != null;
    }
    
    private static boolean isHttp(final RequestPairSource source) {
        final ServletRequest request = source.getServletRequest();
        final ServletResponse response = source.getServletResponse();
        return request instanceof HttpServletRequest && response instanceof HttpServletResponse;
    }
    
    public static boolean _isSessionCreationEnabled(final Object requestPairSource) {
        if (requestPairSource instanceof RequestPairSource) {
            final RequestPairSource source = (RequestPairSource)requestPairSource;
            return _isSessionCreationEnabled(source.getServletRequest());
        }
        return true;
    }
    
    public static boolean _isSessionCreationEnabled(final ServletRequest request) {
        if (request != null) {
            final Object val = request.getAttribute(DefaultSubjectContext.SESSION_CREATION_ENABLED);
            if (val != null && val instanceof Boolean) {
                return (boolean)val;
            }
        }
        return true;
    }
    
    public static HttpServletRequest toHttp(final ServletRequest request) {
        return (HttpServletRequest)request;
    }
    
    public static HttpServletResponse toHttp(final ServletResponse response) {
        return (HttpServletResponse)response;
    }
    
    public static void issueRedirect(final ServletRequest request, final ServletResponse response, final String url, final Map queryParams, final boolean contextRelative, final boolean http10Compatible) throws IOException {
        final RedirectView view = new RedirectView(url, contextRelative, http10Compatible);
        view.renderMergedOutputModel(queryParams, toHttp(request), toHttp(response));
    }
    
    public static void issueRedirect(final ServletRequest request, final ServletResponse response, final String url) throws IOException {
        issueRedirect(request, response, url, null, true, true);
    }
    
    public static void issueRedirect(final ServletRequest request, final ServletResponse response, final String url, final Map queryParams) throws IOException {
        issueRedirect(request, response, url, queryParams, true, true);
    }
    
    public static void issueRedirect(final ServletRequest request, final ServletResponse response, final String url, final Map queryParams, final boolean contextRelative) throws IOException {
        issueRedirect(request, response, url, queryParams, contextRelative, true);
    }
    
    public static boolean isTrue(final ServletRequest request, final String paramName) {
        final String value = getCleanParam(request, paramName);
        return value != null && (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("t") || value.equalsIgnoreCase("1") || value.equalsIgnoreCase("enabled") || value.equalsIgnoreCase("y") || value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("on"));
    }
    
    public static String getCleanParam(final ServletRequest request, final String paramName) {
        return StringUtils.clean(request.getParameter(paramName));
    }
    
    public static void saveRequest(final ServletRequest request) {
        final Subject subject = SecurityUtils.getSubject();
        final Session session = subject.getSession();
        final HttpServletRequest httpRequest = toHttp(request);
        final SavedRequest savedRequest = new SavedRequest(httpRequest);
        session.setAttribute((Object)"shiroSavedRequest", (Object)savedRequest);
    }
    
    public static SavedRequest getAndClearSavedRequest(final ServletRequest request) {
        final SavedRequest savedRequest = getSavedRequest(request);
        if (savedRequest != null) {
            final Subject subject = SecurityUtils.getSubject();
            final Session session = subject.getSession();
            session.removeAttribute((Object)"shiroSavedRequest");
        }
        return savedRequest;
    }
    
    public static SavedRequest getSavedRequest(final ServletRequest request) {
        SavedRequest savedRequest = null;
        final Subject subject = SecurityUtils.getSubject();
        final Session session = subject.getSession(false);
        if (session != null) {
            savedRequest = (SavedRequest)session.getAttribute((Object)"shiroSavedRequest");
        }
        return savedRequest;
    }
    
    public static void redirectToSavedRequest(final ServletRequest request, final ServletResponse response, final String fallbackUrl) throws IOException {
        String successUrl = null;
        boolean contextRelative = true;
        final SavedRequest savedRequest = getAndClearSavedRequest(request);
        if (savedRequest != null && savedRequest.getMethod().equalsIgnoreCase("GET")) {
            successUrl = savedRequest.getRequestUrl();
            contextRelative = false;
        }
        if (successUrl == null) {
            successUrl = fallbackUrl;
        }
        if (successUrl == null) {
            throw new IllegalStateException("Success URL not available via saved request or via the successUrlFallback method parameter. One of these must be non-null for issueSuccessRedirect() to work.");
        }
        issueRedirect(request, response, successUrl, null, contextRelative);
    }
    
    static {
        log = LoggerFactory.getLogger((Class)WebUtils.class);
        SERVLET_REQUEST_KEY = ServletRequest.class.getName() + "_SHIRO_THREAD_CONTEXT_KEY";
        SERVLET_RESPONSE_KEY = ServletResponse.class.getName() + "_SHIRO_THREAD_CONTEXT_KEY";
    }
}
