// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.security.web.authentication;

import org.springframework.util.Assert;
import org.springframework.security.web.util.UrlUtils;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import org.springframework.util.StringUtils;
import javax.servlet.ServletException;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.web.RedirectStrategy;
import org.apache.commons.logging.Log;

public abstract class AbstractAuthenticationTargetUrlRequestHandler
{
    public static String DEFAULT_TARGET_PARAMETER;
    protected final Log logger;
    private String targetUrlParameter;
    private String defaultTargetUrl;
    private boolean alwaysUseDefaultTargetUrl;
    private boolean useReferer;
    private RedirectStrategy redirectStrategy;
    
    protected AbstractAuthenticationTargetUrlRequestHandler() {
        this.logger = LogFactory.getLog((Class)this.getClass());
        this.targetUrlParameter = AbstractAuthenticationTargetUrlRequestHandler.DEFAULT_TARGET_PARAMETER;
        this.defaultTargetUrl = "/";
        this.alwaysUseDefaultTargetUrl = false;
        this.useReferer = false;
        this.redirectStrategy = new DefaultRedirectStrategy();
    }
    
    protected void handle(final HttpServletRequest request, final HttpServletResponse response, final Authentication authentication) throws IOException, ServletException {
        final String targetUrl = this.determineTargetUrl(request, response);
        this.redirectStrategy.sendRedirect(request, response, targetUrl);
    }
    
    protected String determineTargetUrl(final HttpServletRequest request, final HttpServletResponse response) {
        if (this.isAlwaysUseDefaultTargetUrl()) {
            return this.defaultTargetUrl;
        }
        String targetUrl = request.getParameter(this.targetUrlParameter);
        if (StringUtils.hasText(targetUrl)) {
            try {
                targetUrl = URLDecoder.decode(targetUrl, "UTF-8");
            }
            catch (UnsupportedEncodingException e) {
                throw new IllegalStateException("UTF-8 not supported. Shouldn't be possible");
            }
            this.logger.debug((Object)("Found targetUrlParameter in request: " + targetUrl));
            return targetUrl;
        }
        if (this.useReferer && !StringUtils.hasLength(targetUrl)) {
            targetUrl = request.getHeader("Referer");
            this.logger.debug((Object)("Using Referer header: " + targetUrl));
        }
        if (!StringUtils.hasText(targetUrl)) {
            targetUrl = this.defaultTargetUrl;
            this.logger.debug((Object)("Using default Url: " + targetUrl));
        }
        return targetUrl;
    }
    
    protected String getDefaultTargetUrl() {
        return this.defaultTargetUrl;
    }
    
    public void setDefaultTargetUrl(final String defaultTargetUrl) {
        Assert.isTrue(UrlUtils.isValidRedirectUrl(defaultTargetUrl), "defaultTarget must start with '/' or with 'http(s)'");
        this.defaultTargetUrl = defaultTargetUrl;
    }
    
    public void setAlwaysUseDefaultTargetUrl(final boolean alwaysUseDefaultTargetUrl) {
        this.alwaysUseDefaultTargetUrl = alwaysUseDefaultTargetUrl;
    }
    
    protected boolean isAlwaysUseDefaultTargetUrl() {
        return this.alwaysUseDefaultTargetUrl;
    }
    
    public void setTargetUrlParameter(final String targetUrlParameter) {
        Assert.hasText("targetUrlParameter canot be null or empty");
        this.targetUrlParameter = targetUrlParameter;
    }
    
    protected String getTargetUrlParameter() {
        return this.targetUrlParameter;
    }
    
    public void setRedirectStrategy(final RedirectStrategy redirectStrategy) {
        this.redirectStrategy = redirectStrategy;
    }
    
    protected RedirectStrategy getRedirectStrategy() {
        return this.redirectStrategy;
    }
    
    public void setUseReferer(final boolean useReferer) {
        this.useReferer = useReferer;
    }
    
    static {
        AbstractAuthenticationTargetUrlRequestHandler.DEFAULT_TARGET_PARAMETER = "spring-security-redirect";
    }
}
