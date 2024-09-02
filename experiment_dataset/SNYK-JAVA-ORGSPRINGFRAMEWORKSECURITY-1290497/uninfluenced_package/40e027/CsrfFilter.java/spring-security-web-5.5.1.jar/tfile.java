// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.security.web.csrf;

import java.util.Collection;
import java.util.Arrays;
import java.util.HashSet;
import java.security.MessageDigest;
import org.springframework.security.crypto.codec.Utf8;
import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.core.log.LogMessage;
import org.springframework.security.web.util.UrlUtils;
import javax.servlet.ServletResponse;
import javax.servlet.ServletRequest;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import org.springframework.util.Assert;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.apache.commons.logging.Log;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

public final class CsrfFilter extends OncePerRequestFilter
{
    public static final RequestMatcher DEFAULT_CSRF_MATCHER;
    private static final String SHOULD_NOT_FILTER;
    private final Log logger;
    private final CsrfTokenRepository tokenRepository;
    private RequestMatcher requireCsrfProtectionMatcher;
    private AccessDeniedHandler accessDeniedHandler;
    
    public CsrfFilter(final CsrfTokenRepository csrfTokenRepository) {
        this.logger = LogFactory.getLog((Class)this.getClass());
        this.requireCsrfProtectionMatcher = CsrfFilter.DEFAULT_CSRF_MATCHER;
        this.accessDeniedHandler = new AccessDeniedHandlerImpl();
        Assert.notNull((Object)csrfTokenRepository, "csrfTokenRepository cannot be null");
        this.tokenRepository = csrfTokenRepository;
    }
    
    protected boolean shouldNotFilter(final HttpServletRequest request) throws ServletException {
        return Boolean.TRUE.equals(request.getAttribute(CsrfFilter.SHOULD_NOT_FILTER));
    }
    
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain) throws ServletException, IOException {
        request.setAttribute(HttpServletResponse.class.getName(), (Object)response);
        CsrfToken csrfToken = this.tokenRepository.loadToken(request);
        final boolean missingToken = csrfToken == null;
        if (missingToken) {
            csrfToken = this.tokenRepository.generateToken(request);
            this.tokenRepository.saveToken(csrfToken, request, response);
        }
        request.setAttribute(CsrfToken.class.getName(), (Object)csrfToken);
        request.setAttribute(csrfToken.getParameterName(), (Object)csrfToken);
        if (!this.requireCsrfProtectionMatcher.matches(request)) {
            if (this.logger.isTraceEnabled()) {
                this.logger.trace((Object)("Did not protect against CSRF since request did not match " + this.requireCsrfProtectionMatcher));
            }
            filterChain.doFilter((ServletRequest)request, (ServletResponse)response);
            return;
        }
        String actualToken = request.getHeader(csrfToken.getHeaderName());
        if (actualToken == null) {
            actualToken = request.getParameter(csrfToken.getParameterName());
        }
        if (!equalsConstantTime(csrfToken.getToken(), actualToken)) {
            this.logger.debug((Object)LogMessage.of(() -> "Invalid CSRF token found for " + UrlUtils.buildFullRequestUrl(request)));
            final AccessDeniedException exception = missingToken ? new MissingCsrfTokenException(actualToken) : new InvalidCsrfTokenException(csrfToken, actualToken);
            this.accessDeniedHandler.handle(request, response, exception);
            return;
        }
        filterChain.doFilter((ServletRequest)request, (ServletResponse)response);
    }
    
    public static void skipRequest(final HttpServletRequest request) {
        request.setAttribute(CsrfFilter.SHOULD_NOT_FILTER, (Object)Boolean.TRUE);
    }
    
    public void setRequireCsrfProtectionMatcher(final RequestMatcher requireCsrfProtectionMatcher) {
        Assert.notNull((Object)requireCsrfProtectionMatcher, "requireCsrfProtectionMatcher cannot be null");
        this.requireCsrfProtectionMatcher = requireCsrfProtectionMatcher;
    }
    
    public void setAccessDeniedHandler(final AccessDeniedHandler accessDeniedHandler) {
        Assert.notNull((Object)accessDeniedHandler, "accessDeniedHandler cannot be null");
        this.accessDeniedHandler = accessDeniedHandler;
    }
    
    private static boolean equalsConstantTime(final String expected, final String actual) {
        if (expected == actual) {
            return true;
        }
        if (expected == null || actual == null) {
            return false;
        }
        final byte[] expectedBytes = Utf8.encode((CharSequence)expected);
        final byte[] actualBytes = Utf8.encode((CharSequence)actual);
        return MessageDigest.isEqual(expectedBytes, actualBytes);
    }
    
    static {
        DEFAULT_CSRF_MATCHER = new DefaultRequiresCsrfMatcher();
        SHOULD_NOT_FILTER = "SHOULD_NOT_FILTER" + CsrfFilter.class.getName();
    }
    
    private static final class DefaultRequiresCsrfMatcher implements RequestMatcher
    {
        private final HashSet<String> allowedMethods;
        
        private DefaultRequiresCsrfMatcher() {
            this.allowedMethods = new HashSet<String>(Arrays.asList("GET", "HEAD", "TRACE", "OPTIONS"));
        }
        
        @Override
        public boolean matches(final HttpServletRequest request) {
            return !this.allowedMethods.contains(request.getMethod());
        }
        
        @Override
        public String toString() {
            return "CsrfNotRequired " + this.allowedMethods;
        }
    }
}
