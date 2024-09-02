// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.web.util;

import java.util.StringTokenizer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import java.util.Enumeration;
import java.util.TreeMap;
import javax.servlet.http.Cookie;
import java.util.Iterator;
import java.util.Map;
import javax.servlet.ServletResponseWrapper;
import javax.servlet.ServletResponse;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import java.io.FileNotFoundException;
import java.io.File;
import org.springframework.util.StringUtils;
import org.springframework.util.Assert;
import javax.servlet.ServletContext;

public abstract class WebUtils
{
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
    public static final String ERROR_STATUS_CODE_ATTRIBUTE = "javax.servlet.error.status_code";
    public static final String ERROR_EXCEPTION_TYPE_ATTRIBUTE = "javax.servlet.error.exception_type";
    public static final String ERROR_MESSAGE_ATTRIBUTE = "javax.servlet.error.message";
    public static final String ERROR_EXCEPTION_ATTRIBUTE = "javax.servlet.error.exception";
    public static final String ERROR_REQUEST_URI_ATTRIBUTE = "javax.servlet.error.request_uri";
    public static final String ERROR_SERVLET_NAME_ATTRIBUTE = "javax.servlet.error.servlet_name";
    public static final String CONTENT_TYPE_CHARSET_PREFIX = ";charset=";
    public static final String DEFAULT_CHARACTER_ENCODING = "ISO-8859-1";
    public static final String TEMP_DIR_CONTEXT_ATTRIBUTE = "javax.servlet.context.tempdir";
    public static final String HTML_ESCAPE_CONTEXT_PARAM = "defaultHtmlEscape";
    public static final String RESPONSE_ENCODED_HTML_ESCAPE_CONTEXT_PARAM = "responseEncodedHtmlEscape";
    public static final String WEB_APP_ROOT_KEY_PARAM = "webAppRootKey";
    public static final String DEFAULT_WEB_APP_ROOT_KEY = "webapp.root";
    public static final String[] SUBMIT_IMAGE_SUFFIXES;
    public static final String SESSION_MUTEX_ATTRIBUTE;
    
    public static void setWebAppRootSystemProperty(final ServletContext servletContext) throws IllegalStateException {
        Assert.notNull((Object)servletContext, "ServletContext must not be null");
        final String root = servletContext.getRealPath("/");
        if (root == null) {
            throw new IllegalStateException("Cannot set web app root system property when WAR file is not expanded");
        }
        final String param = servletContext.getInitParameter("webAppRootKey");
        final String key = (param != null) ? param : "webapp.root";
        final String oldValue = System.getProperty(key);
        if (oldValue != null && !StringUtils.pathEquals(oldValue, root)) {
            throw new IllegalStateException("Web app root system property already set to different value: '" + key + "' = [" + oldValue + "] instead of [" + root + "] - " + "Choose unique values for the 'webAppRootKey' context-param in your web.xml files!");
        }
        System.setProperty(key, root);
        servletContext.log("Set web app root system property: '" + key + "' = [" + root + "]");
    }
    
    public static void removeWebAppRootSystemProperty(final ServletContext servletContext) {
        Assert.notNull((Object)servletContext, "ServletContext must not be null");
        final String param = servletContext.getInitParameter("webAppRootKey");
        final String key = (param != null) ? param : "webapp.root";
        System.getProperties().remove(key);
    }
    
    @Deprecated
    public static boolean isDefaultHtmlEscape(final ServletContext servletContext) {
        if (servletContext == null) {
            return false;
        }
        final String param = servletContext.getInitParameter("defaultHtmlEscape");
        return Boolean.valueOf(param);
    }
    
    public static Boolean getDefaultHtmlEscape(final ServletContext servletContext) {
        if (servletContext == null) {
            return null;
        }
        final String param = servletContext.getInitParameter("defaultHtmlEscape");
        return StringUtils.hasText(param) ? Boolean.valueOf(param) : null;
    }
    
    public static Boolean getResponseEncodedHtmlEscape(final ServletContext servletContext) {
        if (servletContext == null) {
            return null;
        }
        final String param = servletContext.getInitParameter("responseEncodedHtmlEscape");
        return StringUtils.hasText(param) ? Boolean.valueOf(param) : null;
    }
    
    public static File getTempDir(final ServletContext servletContext) {
        Assert.notNull((Object)servletContext, "ServletContext must not be null");
        return (File)servletContext.getAttribute("javax.servlet.context.tempdir");
    }
    
    public static String getRealPath(final ServletContext servletContext, String path) throws FileNotFoundException {
        Assert.notNull((Object)servletContext, "ServletContext must not be null");
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        final String realPath = servletContext.getRealPath(path);
        if (realPath == null) {
            throw new FileNotFoundException("ServletContext resource [" + path + "] cannot be resolved to absolute file path - " + "web application archive not expanded?");
        }
        return realPath;
    }
    
    public static String getSessionId(final HttpServletRequest request) {
        Assert.notNull((Object)request, "Request must not be null");
        final HttpSession session = request.getSession(false);
        return (session != null) ? session.getId() : null;
    }
    
    public static Object getSessionAttribute(final HttpServletRequest request, final String name) {
        Assert.notNull((Object)request, "Request must not be null");
        final HttpSession session = request.getSession(false);
        return (session != null) ? session.getAttribute(name) : null;
    }
    
    public static Object getRequiredSessionAttribute(final HttpServletRequest request, final String name) throws IllegalStateException {
        final Object attr = getSessionAttribute(request, name);
        if (attr == null) {
            throw new IllegalStateException("No session attribute '" + name + "' found");
        }
        return attr;
    }
    
    public static void setSessionAttribute(final HttpServletRequest request, final String name, final Object value) {
        Assert.notNull((Object)request, "Request must not be null");
        if (value != null) {
            request.getSession().setAttribute(name, value);
        }
        else {
            final HttpSession session = request.getSession(false);
            if (session != null) {
                session.removeAttribute(name);
            }
        }
    }
    
    public static Object getOrCreateSessionAttribute(final HttpSession session, final String name, final Class<?> clazz) throws IllegalArgumentException {
        Assert.notNull((Object)session, "Session must not be null");
        Object sessionObject = session.getAttribute(name);
        if (sessionObject == null) {
            try {
                sessionObject = clazz.newInstance();
            }
            catch (InstantiationException ex) {
                throw new IllegalArgumentException("Could not instantiate class [" + clazz.getName() + "] for session attribute '" + name + "': " + ex.getMessage());
            }
            catch (IllegalAccessException ex2) {
                throw new IllegalArgumentException("Could not access default constructor of class [" + clazz.getName() + "] for session attribute '" + name + "': " + ex2.getMessage());
            }
            session.setAttribute(name, sessionObject);
        }
        return sessionObject;
    }
    
    public static Object getSessionMutex(final HttpSession session) {
        Assert.notNull((Object)session, "Session must not be null");
        Object mutex = session.getAttribute(WebUtils.SESSION_MUTEX_ATTRIBUTE);
        if (mutex == null) {
            mutex = session;
        }
        return mutex;
    }
    
    public static <T> T getNativeRequest(final ServletRequest request, final Class<T> requiredType) {
        if (requiredType != null) {
            if (requiredType.isInstance(request)) {
                return (T)request;
            }
            if (request instanceof ServletRequestWrapper) {
                return (T)getNativeRequest(((ServletRequestWrapper)request).getRequest(), (Class<Object>)requiredType);
            }
        }
        return null;
    }
    
    public static <T> T getNativeResponse(final ServletResponse response, final Class<T> requiredType) {
        if (requiredType != null) {
            if (requiredType.isInstance(response)) {
                return (T)response;
            }
            if (response instanceof ServletResponseWrapper) {
                return (T)getNativeResponse(((ServletResponseWrapper)response).getResponse(), (Class<Object>)requiredType);
            }
        }
        return null;
    }
    
    public static boolean isIncludeRequest(final ServletRequest request) {
        return request.getAttribute("javax.servlet.include.request_uri") != null;
    }
    
    public static void exposeErrorRequestAttributes(final HttpServletRequest request, final Throwable ex, final String servletName) {
        exposeRequestAttributeIfNotPresent((ServletRequest)request, "javax.servlet.error.status_code", 200);
        exposeRequestAttributeIfNotPresent((ServletRequest)request, "javax.servlet.error.exception_type", ex.getClass());
        exposeRequestAttributeIfNotPresent((ServletRequest)request, "javax.servlet.error.message", ex.getMessage());
        exposeRequestAttributeIfNotPresent((ServletRequest)request, "javax.servlet.error.exception", ex);
        exposeRequestAttributeIfNotPresent((ServletRequest)request, "javax.servlet.error.request_uri", request.getRequestURI());
        exposeRequestAttributeIfNotPresent((ServletRequest)request, "javax.servlet.error.servlet_name", servletName);
    }
    
    private static void exposeRequestAttributeIfNotPresent(final ServletRequest request, final String name, final Object value) {
        if (request.getAttribute(name) == null) {
            request.setAttribute(name, value);
        }
    }
    
    public static void clearErrorRequestAttributes(final HttpServletRequest request) {
        request.removeAttribute("javax.servlet.error.status_code");
        request.removeAttribute("javax.servlet.error.exception_type");
        request.removeAttribute("javax.servlet.error.message");
        request.removeAttribute("javax.servlet.error.exception");
        request.removeAttribute("javax.servlet.error.request_uri");
        request.removeAttribute("javax.servlet.error.servlet_name");
    }
    
    public static void exposeRequestAttributes(final ServletRequest request, final Map<String, ?> attributes) {
        Assert.notNull((Object)request, "Request must not be null");
        Assert.notNull((Object)attributes, "Attributes Map must not be null");
        for (final Map.Entry<String, ?> entry : attributes.entrySet()) {
            request.setAttribute((String)entry.getKey(), (Object)entry.getValue());
        }
    }
    
    public static Cookie getCookie(final HttpServletRequest request, final String name) {
        Assert.notNull((Object)request, "Request must not be null");
        final Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (final Cookie cookie : cookies) {
                if (name.equals(cookie.getName())) {
                    return cookie;
                }
            }
        }
        return null;
    }
    
    public static boolean hasSubmitParameter(final ServletRequest request, final String name) {
        Assert.notNull((Object)request, "Request must not be null");
        if (request.getParameter(name) != null) {
            return true;
        }
        for (final String suffix : WebUtils.SUBMIT_IMAGE_SUFFIXES) {
            if (request.getParameter(name + suffix) != null) {
                return true;
            }
        }
        return false;
    }
    
    public static String findParameterValue(final ServletRequest request, final String name) {
        return findParameterValue(request.getParameterMap(), name);
    }
    
    public static String findParameterValue(final Map<String, ?> parameters, final String name) {
        final Object value = parameters.get(name);
        if (value instanceof String[]) {
            final String[] values = (String[])value;
            return (values.length > 0) ? values[0] : null;
        }
        if (value != null) {
            return value.toString();
        }
        final String prefix = name + "_";
        for (final String paramName : parameters.keySet()) {
            if (paramName.startsWith(prefix)) {
                for (final String suffix : WebUtils.SUBMIT_IMAGE_SUFFIXES) {
                    if (paramName.endsWith(suffix)) {
                        return paramName.substring(prefix.length(), paramName.length() - suffix.length());
                    }
                }
                return paramName.substring(prefix.length());
            }
        }
        return null;
    }
    
    public static Map<String, Object> getParametersStartingWith(final ServletRequest request, String prefix) {
        Assert.notNull((Object)request, "Request must not be null");
        final Enumeration<String> paramNames = (Enumeration<String>)request.getParameterNames();
        final Map<String, Object> params = new TreeMap<String, Object>();
        if (prefix == null) {
            prefix = "";
        }
        while (paramNames != null && paramNames.hasMoreElements()) {
            final String paramName = paramNames.nextElement();
            if ("".equals(prefix) || paramName.startsWith(prefix)) {
                final String unprefixed = paramName.substring(prefix.length());
                final String[] values = request.getParameterValues(paramName);
                if (values == null) {
                    continue;
                }
                if (values.length == 0) {
                    continue;
                }
                if (values.length > 1) {
                    params.put(unprefixed, values);
                }
                else {
                    params.put(unprefixed, values[0]);
                }
            }
        }
        return params;
    }
    
    public static int getTargetPage(final ServletRequest request, final String paramPrefix, final int currentPage) {
        final Enumeration<String> paramNames = (Enumeration<String>)request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            if (paramName.startsWith(paramPrefix)) {
                for (int i = 0; i < WebUtils.SUBMIT_IMAGE_SUFFIXES.length; ++i) {
                    final String suffix = WebUtils.SUBMIT_IMAGE_SUFFIXES[i];
                    if (paramName.endsWith(suffix)) {
                        paramName = paramName.substring(0, paramName.length() - suffix.length());
                    }
                }
                return Integer.parseInt(paramName.substring(paramPrefix.length()));
            }
        }
        return currentPage;
    }
    
    public static String extractFilenameFromUrlPath(final String urlPath) {
        String filename = extractFullFilenameFromUrlPath(urlPath);
        final int dotIndex = filename.lastIndexOf(46);
        if (dotIndex != -1) {
            filename = filename.substring(0, dotIndex);
        }
        return filename;
    }
    
    public static String extractFullFilenameFromUrlPath(final String urlPath) {
        int end = urlPath.indexOf(59);
        if (end == -1) {
            end = urlPath.indexOf(63);
            if (end == -1) {
                end = urlPath.length();
            }
        }
        final int begin = urlPath.lastIndexOf(47, end) + 1;
        return urlPath.substring(begin, end);
    }
    
    public static MultiValueMap<String, String> parseMatrixVariables(final String matrixVariables) {
        final MultiValueMap<String, String> result = (MultiValueMap<String, String>)new LinkedMultiValueMap();
        if (!StringUtils.hasText(matrixVariables)) {
            return result;
        }
        final StringTokenizer pairs = new StringTokenizer(matrixVariables, ";");
        while (pairs.hasMoreTokens()) {
            final String pair = pairs.nextToken();
            final int index = pair.indexOf(61);
            if (index != -1) {
                final String name = pair.substring(0, index);
                final String rawValue = pair.substring(index + 1);
                for (final String value : StringUtils.commaDelimitedListToStringArray(rawValue)) {
                    result.add((Object)name, (Object)value);
                }
            }
            else {
                result.add((Object)pair, (Object)"");
            }
        }
        return result;
    }
    
    static {
        SUBMIT_IMAGE_SUFFIXES = new String[] { ".x", ".y" };
        SESSION_MUTEX_ATTRIBUTE = WebUtils.class.getName() + ".MUTEX";
    }
}
