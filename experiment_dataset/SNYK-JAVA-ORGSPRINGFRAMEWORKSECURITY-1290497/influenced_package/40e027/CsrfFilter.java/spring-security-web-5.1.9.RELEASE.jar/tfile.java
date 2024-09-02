// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.security.web.csrf;

import java.util.Collection;
import java.util.Arrays;
import java.util.HashSet;
import java.io.IOException;
import javax.servlet.ServletException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.util.UrlUtils;
import javax.servlet.ServletResponse;
import javax.servlet.ServletRequest;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletResponse;
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
            filterChain.doFilter((ServletRequest)request, (ServletResponse)response);
            return;
        }
        String actualToken = request.getHeader(csrfToken.getHeaderName());
        if (actualToken == null) {
            actualToken = request.getParameter(csrfToken.getParameterName());
        }
        if (!csrfToken.getToken().equals(actualToken)) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug((Object)("Invalid CSRF token found for " + UrlUtils.buildFullRequestUrl(request)));
            }
            if (missingToken) {
                this.accessDeniedHandler.handle(request, response, new MissingCsrfTokenException(actualToken));
            }
            else {
                this.accessDeniedHandler.handle(request, response, new InvalidCsrfTokenException(csrfToken, actualToken));
            }
            return;
        }
        filterChain.doFilter((ServletRequest)request, (ServletResponse)response);
    }
    
    public void setRequireCsrfProtectionMatcher(final RequestMatcher requireCsrfProtectionMatcher) {
        Assert.notNull((Object)requireCsrfProtectionMatcher, "requireCsrfProtectionMatcher cannot be null");
        this.requireCsrfProtectionMatcher = requireCsrfProtectionMatcher;
    }
    
    public void setAccessDeniedHandler(final AccessDeniedHandler accessDeniedHandler) {
        Assert.notNull((Object)accessDeniedHandler, "accessDeniedHandler cannot be null");
        this.accessDeniedHandler = accessDeniedHandler;
    }
    
    static {
        DEFAULT_CSRF_MATCHER = new DefaultRequiresCsrfMatcher();
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
    }
}
