// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.shiro.web.mgt;

import org.slf4j.LoggerFactory;
import org.apache.shiro.subject.SubjectContext;
import javax.servlet.ServletRequest;
import org.apache.shiro.web.servlet.ShiroHttpServletRequest;
import org.apache.shiro.web.subject.WebSubjectContext;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.apache.shiro.codec.Base64;
import org.apache.shiro.web.util.WebUtils;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.servlet.SimpleCookie;
import org.apache.shiro.web.servlet.Cookie;
import org.slf4j.Logger;
import org.apache.shiro.mgt.AbstractRememberMeManager;

public class CookieRememberMeManager extends AbstractRememberMeManager
{
    private static final transient Logger log;
    public static final String DEFAULT_REMEMBER_ME_COOKIE_NAME = "rememberMe";
    private Cookie cookie;
    
    public CookieRememberMeManager() {
        final Cookie cookie = new SimpleCookie("rememberMe");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(31536000);
        this.cookie = cookie;
    }
    
    public Cookie getCookie() {
        return this.cookie;
    }
    
    public void setCookie(final Cookie cookie) {
        this.cookie = cookie;
    }
    
    protected void rememberSerializedIdentity(final Subject subject, final byte[] serialized) {
        if (!WebUtils.isHttp(subject)) {
            if (CookieRememberMeManager.log.isDebugEnabled()) {
                final String msg = "Subject argument is not an HTTP-aware instance.  This is required to obtain a servlet request and response in order to set the rememberMe cookie. Returning immediately and ignoring rememberMe operation.";
                CookieRememberMeManager.log.debug(msg);
            }
            return;
        }
        final HttpServletRequest request = WebUtils.getHttpRequest(subject);
        final HttpServletResponse response = WebUtils.getHttpResponse(subject);
        final String base64 = Base64.encodeToString(serialized);
        final Cookie template = this.getCookie();
        final Cookie cookie = new SimpleCookie(template);
        cookie.setValue(base64);
        cookie.saveTo(request, response);
    }
    
    private boolean isIdentityRemoved(final WebSubjectContext subjectContext) {
        final ServletRequest request = subjectContext.resolveServletRequest();
        if (request != null) {
            final Boolean removed = (Boolean)request.getAttribute(ShiroHttpServletRequest.IDENTITY_REMOVED_KEY);
            return removed != null && removed;
        }
        return false;
    }
    
    protected byte[] getRememberedSerializedIdentity(final SubjectContext subjectContext) {
        if (!WebUtils.isHttp(subjectContext)) {
            if (CookieRememberMeManager.log.isDebugEnabled()) {
                final String msg = "SubjectContext argument is not an HTTP-aware instance.  This is required to obtain a servlet request and response in order to retrieve the rememberMe cookie. Returning immediately and ignoring rememberMe operation.";
                CookieRememberMeManager.log.debug(msg);
            }
            return null;
        }
        final WebSubjectContext wsc = (WebSubjectContext)subjectContext;
        if (this.isIdentityRemoved(wsc)) {
            return null;
        }
        final HttpServletRequest request = WebUtils.getHttpRequest(wsc);
        final HttpServletResponse response = WebUtils.getHttpResponse(wsc);
        String base64 = this.getCookie().readValue(request, response);
        if ("deleteMe".equals(base64)) {
            return null;
        }
        if (base64 != null) {
            base64 = this.ensurePadding(base64);
            if (CookieRememberMeManager.log.isTraceEnabled()) {
                CookieRememberMeManager.log.trace("Acquired Base64 encoded identity [" + base64 + "]");
            }
            final byte[] decoded = Base64.decode(base64);
            if (CookieRememberMeManager.log.isTraceEnabled()) {
                CookieRememberMeManager.log.trace("Base64 decoded byte array length: " + ((decoded != null) ? decoded.length : 0) + " bytes.");
            }
            return decoded;
        }
        return null;
    }
    
    private String ensurePadding(String base64) {
        final int length = base64.length();
        if (length % 4 != 0) {
            final StringBuilder sb = new StringBuilder(base64);
            for (int i = 0; i < length % 4; ++i) {
                sb.append('=');
            }
            base64 = sb.toString();
        }
        return base64;
    }
    
    protected void forgetIdentity(final Subject subject) {
        if (WebUtils.isHttp(subject)) {
            final HttpServletRequest request = WebUtils.getHttpRequest(subject);
            final HttpServletResponse response = WebUtils.getHttpResponse(subject);
            this.forgetIdentity(request, response);
        }
    }
    
    public void forgetIdentity(final SubjectContext subjectContext) {
        if (WebUtils.isHttp(subjectContext)) {
            final HttpServletRequest request = WebUtils.getHttpRequest(subjectContext);
            final HttpServletResponse response = WebUtils.getHttpResponse(subjectContext);
            this.forgetIdentity(request, response);
        }
    }
    
    private void forgetIdentity(final HttpServletRequest request, final HttpServletResponse response) {
        this.getCookie().removeFrom(request, response);
    }
    
    static {
        log = LoggerFactory.getLogger((Class)CookieRememberMeManager.class);
    }
}
