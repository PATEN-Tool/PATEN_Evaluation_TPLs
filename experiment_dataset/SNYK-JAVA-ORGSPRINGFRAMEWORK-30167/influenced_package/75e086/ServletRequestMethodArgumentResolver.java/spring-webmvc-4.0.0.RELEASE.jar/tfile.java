// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.web.servlet.mvc.method.annotation;

import java.time.ZoneId;
import java.io.IOException;
import java.lang.reflect.Method;
import org.springframework.web.servlet.support.RequestContextUtils;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import java.io.Reader;
import java.io.InputStream;
import java.util.TimeZone;
import java.util.Locale;
import java.security.Principal;
import javax.servlet.http.HttpSession;
import org.springframework.web.multipart.MultipartRequest;
import javax.servlet.ServletRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.core.MethodParameter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

public class ServletRequestMethodArgumentResolver implements HandlerMethodArgumentResolver
{
    public boolean supportsParameter(final MethodParameter parameter) {
        final Class<?> paramType = (Class<?>)parameter.getParameterType();
        return WebRequest.class.isAssignableFrom(paramType) || ServletRequest.class.isAssignableFrom(paramType) || MultipartRequest.class.isAssignableFrom(paramType) || HttpSession.class.isAssignableFrom(paramType) || Principal.class.isAssignableFrom(paramType) || Locale.class.equals(paramType) || TimeZone.class.equals(paramType) || "java.time.ZoneId".equals(paramType.getName()) || InputStream.class.isAssignableFrom(paramType) || Reader.class.isAssignableFrom(paramType);
    }
    
    public Object resolveArgument(final MethodParameter parameter, final ModelAndViewContainer mavContainer, final NativeWebRequest webRequest, final WebDataBinderFactory binderFactory) throws IOException {
        final Class<?> paramType = (Class<?>)parameter.getParameterType();
        if (WebRequest.class.isAssignableFrom(paramType)) {
            return webRequest;
        }
        final HttpServletRequest request = (HttpServletRequest)webRequest.getNativeRequest((Class)HttpServletRequest.class);
        if (ServletRequest.class.isAssignableFrom(paramType) || MultipartRequest.class.isAssignableFrom(paramType)) {
            final Object nativeRequest = webRequest.getNativeRequest((Class)paramType);
            if (nativeRequest == null) {
                throw new IllegalStateException("Current request is not of type [" + paramType.getName() + "]: " + request);
            }
            return nativeRequest;
        }
        else {
            if (HttpSession.class.isAssignableFrom(paramType)) {
                return request.getSession();
            }
            if (Principal.class.isAssignableFrom(paramType)) {
                return request.getUserPrincipal();
            }
            if (Locale.class.equals(paramType)) {
                return RequestContextUtils.getLocale(request);
            }
            if (TimeZone.class.equals(paramType)) {
                final TimeZone timeZone = RequestContextUtils.getTimeZone(request);
                return (timeZone != null) ? timeZone : TimeZone.getDefault();
            }
            if ("java.time.ZoneId".equals(paramType.getName())) {
                return ZoneIdResolver.resolveZoneId(request);
            }
            if (InputStream.class.isAssignableFrom(paramType)) {
                return request.getInputStream();
            }
            if (Reader.class.isAssignableFrom(paramType)) {
                return request.getReader();
            }
            final Method method = parameter.getMethod();
            throw new UnsupportedOperationException("Unknown parameter type: " + paramType + " in method: " + method);
        }
    }
    
    private static class ZoneIdResolver
    {
        public static Object resolveZoneId(final HttpServletRequest request) {
            final TimeZone timeZone = RequestContextUtils.getTimeZone(request);
            return (timeZone != null) ? timeZone.toZoneId() : ZoneId.systemDefault();
        }
    }
}
