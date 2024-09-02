// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.security.web.csrf;

import java.util.regex.Pattern;
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
        this.requireCsrfProtectionMatcher = new DefaultRequiresCsrfMatcher();
        this.accessDeniedHandler = new AccessDeniedHandlerImpl();
        Assert.notNull((Object)csrfTokenRepository, "csrfTokenRepository cannot be null");
        this.tokenRepository = csrfTokenRepository;
    }
    
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain) throws ServletException, IOException {
        CsrfToken csrfToken = this.tokenRepository.loadToken(request);
        final boolean missingToken = csrfToken == null;
        if (missingToken) {
            final CsrfToken generatedToken = this.tokenRepository.generateToken(request);
            csrfToken = new SaveOnAccessCsrfToken(this.tokenRepository, request, response, generatedToken);
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
    
    private static final class SaveOnAccessCsrfToken implements CsrfToken
    {
        private transient CsrfTokenRepository tokenRepository;
        private transient HttpServletRequest request;
        private transient HttpServletResponse response;
        private final CsrfToken delegate;
        
        public SaveOnAccessCsrfToken(final CsrfTokenRepository tokenRepository, final HttpServletRequest request, final HttpServletResponse response, final CsrfToken delegate) {
            this.tokenRepository = tokenRepository;
            this.request = request;
            this.response = response;
            this.delegate = delegate;
        }
        
        public String getHeaderName() {
            return this.delegate.getHeaderName();
        }
        
        public String getParameterName() {
            return this.delegate.getParameterName();
        }
        
        public String getToken() {
            this.saveTokenIfNecessary();
            return this.delegate.getToken();
        }
        
        @Override
        public String toString() {
            return "SaveOnAccessCsrfToken [delegate=" + this.delegate + "]";
        }
        
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = 31 * result + ((this.delegate == null) ? 0 : this.delegate.hashCode());
            return result;
        }
        
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (this.getClass() != obj.getClass()) {
                return false;
            }
            final SaveOnAccessCsrfToken other = (SaveOnAccessCsrfToken)obj;
            if (this.delegate == null) {
                if (other.delegate != null) {
                    return false;
                }
            }
            else if (!this.delegate.equals(other.delegate)) {
                return false;
            }
            return true;
        }
        
        private void saveTokenIfNecessary() {
            if (this.tokenRepository == null) {
                return;
            }
            synchronized (this) {
                if (this.tokenRepository != null) {
                    this.tokenRepository.saveToken(this.delegate, this.request, this.response);
                    this.tokenRepository = null;
                    this.request = null;
                    this.response = null;
                }
            }
        }
    }
    
    private static final class DefaultRequiresCsrfMatcher implements RequestMatcher
    {
        private Pattern allowedMethods;
        
        private DefaultRequiresCsrfMatcher() {
            this.allowedMethods = Pattern.compile("^(GET|HEAD|TRACE|OPTIONS)$");
        }
        
        public boolean matches(final HttpServletRequest request) {
            return !this.allowedMethods.matcher(request.getMethod()).matches();
        }
    }
}
