// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.catalina.authenticator;

import org.apache.juli.logging.LogFactory;
import java.util.Enumeration;
import org.apache.tomcat.util.buf.ByteChunk;
import org.apache.tomcat.util.http.MimeHeaders;
import java.util.Iterator;
import java.io.InputStream;
import org.apache.tomcat.util.buf.MessageBytes;
import org.apache.coyote.ActionCode;
import java.util.Locale;
import javax.servlet.http.Cookie;
import javax.servlet.RequestDispatcher;
import org.apache.catalina.Manager;
import org.apache.tomcat.util.ExceptionUtils;
import javax.servlet.ServletResponse;
import javax.servlet.ServletRequest;
import org.apache.catalina.connector.Response;
import org.apache.catalina.Realm;
import org.apache.tomcat.util.descriptor.web.LoginConfig;
import org.apache.catalina.Session;
import java.io.IOException;
import java.security.Principal;
import javax.servlet.http.HttpServletResponse;
import org.apache.catalina.connector.Request;
import org.apache.juli.logging.Log;

public class FormAuthenticator extends AuthenticatorBase
{
    private static final Log log;
    protected String characterEncoding;
    protected String landingPage;
    
    public FormAuthenticator() {
        this.characterEncoding = null;
        this.landingPage = null;
    }
    
    public String getCharacterEncoding() {
        return this.characterEncoding;
    }
    
    public void setCharacterEncoding(final String encoding) {
        this.characterEncoding = encoding;
    }
    
    public String getLandingPage() {
        return this.landingPage;
    }
    
    public void setLandingPage(final String landingPage) {
        this.landingPage = landingPage;
    }
    
    @Override
    protected boolean doAuthenticate(final Request request, final HttpServletResponse response) throws IOException {
        if (this.checkForCachedAuthentication(request, response, true)) {
            return true;
        }
        Session session = null;
        Principal principal = null;
        if (!this.cache) {
            session = request.getSessionInternal(true);
            if (FormAuthenticator.log.isDebugEnabled()) {
                FormAuthenticator.log.debug((Object)("Checking for reauthenticate in session " + session));
            }
            final String username = (String)session.getNote("org.apache.catalina.session.USERNAME");
            final String password = (String)session.getNote("org.apache.catalina.session.PASSWORD");
            if (username != null && password != null) {
                if (FormAuthenticator.log.isDebugEnabled()) {
                    FormAuthenticator.log.debug((Object)("Reauthenticating username '" + username + "'"));
                }
                principal = this.context.getRealm().authenticate(username, password);
                if (principal != null) {
                    session.setNote("org.apache.catalina.authenticator.PRINCIPAL", principal);
                    if (!this.matchRequest(request)) {
                        this.register(request, response, principal, "FORM", username, password);
                        return true;
                    }
                }
                if (FormAuthenticator.log.isDebugEnabled()) {
                    FormAuthenticator.log.debug((Object)"Reauthentication failed, proceed normally");
                }
            }
        }
        if (this.matchRequest(request)) {
            session = request.getSessionInternal(true);
            if (FormAuthenticator.log.isDebugEnabled()) {
                FormAuthenticator.log.debug((Object)("Restore request from session '" + session.getIdInternal() + "'"));
            }
            principal = (Principal)session.getNote("org.apache.catalina.authenticator.PRINCIPAL");
            this.register(request, response, principal, "FORM", (String)session.getNote("org.apache.catalina.session.USERNAME"), (String)session.getNote("org.apache.catalina.session.PASSWORD"));
            if (this.cache) {
                session.removeNote("org.apache.catalina.session.USERNAME");
                session.removeNote("org.apache.catalina.session.PASSWORD");
            }
            if (this.restoreRequest(request, session)) {
                if (FormAuthenticator.log.isDebugEnabled()) {
                    FormAuthenticator.log.debug((Object)"Proceed to restored request");
                }
                return true;
            }
            if (FormAuthenticator.log.isDebugEnabled()) {
                FormAuthenticator.log.debug((Object)"Restore of original request failed");
            }
            response.sendError(400);
            return false;
        }
        else {
            final String contextPath = request.getContextPath();
            String requestURI = request.getDecodedRequestURI();
            final boolean loginAction = requestURI.startsWith(contextPath) && requestURI.endsWith("/j_security_check");
            final LoginConfig config = this.context.getLoginConfig();
            if (!loginAction) {
                if (request.getServletPath().length() == 0 && request.getPathInfo() == null) {
                    final StringBuilder location = new StringBuilder(requestURI);
                    location.append('/');
                    if (request.getQueryString() != null) {
                        location.append('?');
                        location.append(request.getQueryString());
                    }
                    response.sendRedirect(response.encodeRedirectURL(location.toString()));
                    return false;
                }
                session = request.getSessionInternal(true);
                if (FormAuthenticator.log.isDebugEnabled()) {
                    FormAuthenticator.log.debug((Object)("Save request in session '" + session.getIdInternal() + "'"));
                }
                try {
                    this.saveRequest(request, session);
                }
                catch (IOException ioe) {
                    FormAuthenticator.log.debug((Object)"Request body too big to save during authentication");
                    response.sendError(403, FormAuthenticator.sm.getString("authenticator.requestBodyTooBig"));
                    return false;
                }
                this.forwardToLoginPage(request, response, config);
                return false;
            }
            else {
                request.getResponse().sendAcknowledgement();
                final Realm realm = this.context.getRealm();
                if (this.characterEncoding != null) {
                    request.setCharacterEncoding(this.characterEncoding);
                }
                final String username2 = request.getParameter("j_username");
                final String password2 = request.getParameter("j_password");
                if (FormAuthenticator.log.isDebugEnabled()) {
                    FormAuthenticator.log.debug((Object)("Authenticating username '" + username2 + "'"));
                }
                principal = realm.authenticate(username2, password2);
                if (principal == null) {
                    this.forwardToErrorPage(request, response, config);
                    return false;
                }
                if (FormAuthenticator.log.isDebugEnabled()) {
                    FormAuthenticator.log.debug((Object)("Authentication of '" + username2 + "' was successful"));
                }
                if (session == null) {
                    session = request.getSessionInternal(false);
                }
                if (session == null) {
                    if (this.containerLog.isDebugEnabled()) {
                        this.containerLog.debug((Object)"User took so long to log on the session expired");
                    }
                    if (this.landingPage == null) {
                        response.sendError(408, FormAuthenticator.sm.getString("authenticator.sessionExpired"));
                    }
                    else {
                        final String uri = request.getContextPath() + this.landingPage;
                        final SavedRequest saved = new SavedRequest();
                        saved.setMethod("GET");
                        saved.setRequestURI(uri);
                        saved.setDecodedRequestURI(uri);
                        request.getSessionInternal(true).setNote("org.apache.catalina.authenticator.REQUEST", saved);
                        response.sendRedirect(response.encodeRedirectURL(uri));
                    }
                    return false;
                }
                session.setNote("org.apache.catalina.authenticator.PRINCIPAL", principal);
                session.setNote("org.apache.catalina.session.USERNAME", username2);
                session.setNote("org.apache.catalina.session.PASSWORD", password2);
                requestURI = this.savedRequestURL(session);
                if (FormAuthenticator.log.isDebugEnabled()) {
                    FormAuthenticator.log.debug((Object)("Redirecting to original '" + requestURI + "'"));
                }
                if (requestURI == null) {
                    if (this.landingPage == null) {
                        response.sendError(400, FormAuthenticator.sm.getString("authenticator.formlogin"));
                    }
                    else {
                        final String uri = request.getContextPath() + this.landingPage;
                        final SavedRequest saved = new SavedRequest();
                        saved.setMethod("GET");
                        saved.setRequestURI(uri);
                        saved.setDecodedRequestURI(uri);
                        session.setNote("org.apache.catalina.authenticator.REQUEST", saved);
                        response.sendRedirect(response.encodeRedirectURL(uri));
                    }
                }
                else {
                    final Response internalResponse = request.getResponse();
                    final String location2 = response.encodeRedirectURL(requestURI);
                    if ("HTTP/1.1".equals(request.getProtocol())) {
                        internalResponse.sendRedirect(location2, 303);
                    }
                    else {
                        internalResponse.sendRedirect(location2, 302);
                    }
                }
                return false;
            }
        }
    }
    
    @Override
    protected boolean isContinuationRequired(final Request request) {
        final String contextPath = this.context.getPath();
        final String decodedRequestURI = request.getDecodedRequestURI();
        if (decodedRequestURI.startsWith(contextPath) && decodedRequestURI.endsWith("/j_security_check")) {
            return true;
        }
        final Session session = request.getSessionInternal(false);
        if (session != null) {
            final SavedRequest savedRequest = (SavedRequest)session.getNote("org.apache.catalina.authenticator.REQUEST");
            if (savedRequest != null && decodedRequestURI.equals(savedRequest.getDecodedRequestURI())) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    protected String getAuthMethod() {
        return "FORM";
    }
    
    protected void forwardToLoginPage(final Request request, final HttpServletResponse response, final LoginConfig config) throws IOException {
        if (FormAuthenticator.log.isDebugEnabled()) {
            FormAuthenticator.log.debug((Object)FormAuthenticator.sm.getString("formAuthenticator.forwardLogin", new Object[] { request.getRequestURI(), request.getMethod(), config.getLoginPage(), this.context.getName() }));
        }
        final String loginPage = config.getLoginPage();
        if (loginPage == null || loginPage.length() == 0) {
            final String msg = FormAuthenticator.sm.getString("formAuthenticator.noLoginPage", new Object[] { this.context.getName() });
            FormAuthenticator.log.warn((Object)msg);
            response.sendError(500, msg);
            return;
        }
        if (this.getChangeSessionIdOnAuthentication()) {
            final Session session = request.getSessionInternal(false);
            if (session != null) {
                final Manager manager = request.getContext().getManager();
                manager.changeSessionId(session);
                request.changeSessionId(session.getId());
            }
        }
        final String oldMethod = request.getMethod();
        request.getCoyoteRequest().method().setString("GET");
        final RequestDispatcher disp = this.context.getServletContext().getRequestDispatcher(loginPage);
        try {
            if (this.context.fireRequestInitEvent((ServletRequest)request.getRequest())) {
                disp.forward((ServletRequest)request.getRequest(), (ServletResponse)response);
                this.context.fireRequestDestroyEvent((ServletRequest)request.getRequest());
            }
        }
        catch (Throwable t) {
            ExceptionUtils.handleThrowable(t);
            final String msg2 = FormAuthenticator.sm.getString("formAuthenticator.forwardLoginFail");
            FormAuthenticator.log.warn((Object)msg2, t);
            request.setAttribute("javax.servlet.error.exception", t);
            response.sendError(500, msg2);
        }
        finally {
            request.getCoyoteRequest().method().setString(oldMethod);
        }
    }
    
    protected void forwardToErrorPage(final Request request, final HttpServletResponse response, final LoginConfig config) throws IOException {
        final String errorPage = config.getErrorPage();
        if (errorPage == null || errorPage.length() == 0) {
            final String msg = FormAuthenticator.sm.getString("formAuthenticator.noErrorPage", new Object[] { this.context.getName() });
            FormAuthenticator.log.warn((Object)msg);
            response.sendError(500, msg);
            return;
        }
        final RequestDispatcher disp = this.context.getServletContext().getRequestDispatcher(config.getErrorPage());
        try {
            if (this.context.fireRequestInitEvent((ServletRequest)request.getRequest())) {
                disp.forward((ServletRequest)request.getRequest(), (ServletResponse)response);
                this.context.fireRequestDestroyEvent((ServletRequest)request.getRequest());
            }
        }
        catch (Throwable t) {
            ExceptionUtils.handleThrowable(t);
            final String msg2 = FormAuthenticator.sm.getString("formAuthenticator.forwardErrorFail");
            FormAuthenticator.log.warn((Object)msg2, t);
            request.setAttribute("javax.servlet.error.exception", t);
            response.sendError(500, msg2);
        }
    }
    
    protected boolean matchRequest(final Request request) {
        final Session session = request.getSessionInternal(false);
        if (session == null) {
            return false;
        }
        final SavedRequest sreq = (SavedRequest)session.getNote("org.apache.catalina.authenticator.REQUEST");
        if (sreq == null) {
            return false;
        }
        if (session.getNote("org.apache.catalina.authenticator.PRINCIPAL") == null) {
            return false;
        }
        final String decodedRequestURI = request.getDecodedRequestURI();
        return decodedRequestURI != null && decodedRequestURI.equals(sreq.getDecodedRequestURI());
    }
    
    protected boolean restoreRequest(final Request request, final Session session) throws IOException {
        final SavedRequest saved = (SavedRequest)session.getNote("org.apache.catalina.authenticator.REQUEST");
        session.removeNote("org.apache.catalina.authenticator.REQUEST");
        session.removeNote("org.apache.catalina.authenticator.PRINCIPAL");
        if (saved == null) {
            return false;
        }
        final byte[] buffer = new byte[4096];
        final InputStream is = (InputStream)request.createInputStream();
        while (is.read(buffer) >= 0) {}
        request.clearCookies();
        final Iterator<Cookie> cookies = saved.getCookies();
        while (cookies.hasNext()) {
            request.addCookie(cookies.next());
        }
        final String method = saved.getMethod();
        final MimeHeaders rmh = request.getCoyoteRequest().getMimeHeaders();
        rmh.recycle();
        final boolean cacheable = "GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method);
        final Iterator<String> names = saved.getHeaderNames();
        while (names.hasNext()) {
            final String name = names.next();
            if (!"If-Modified-Since".equalsIgnoreCase(name) && (!cacheable || !"If-None-Match".equalsIgnoreCase(name))) {
                final Iterator<String> values = saved.getHeaderValues(name);
                while (values.hasNext()) {
                    rmh.addValue(name).setString((String)values.next());
                }
            }
        }
        request.clearLocales();
        final Iterator<Locale> locales = saved.getLocales();
        while (locales.hasNext()) {
            request.addLocale(locales.next());
        }
        request.getCoyoteRequest().getParameters().recycle();
        request.getCoyoteRequest().getParameters().setQueryStringEncoding(request.getConnector().getURIEncoding());
        final ByteChunk body = saved.getBody();
        if (body != null) {
            request.getCoyoteRequest().action(ActionCode.REQ_SET_BODY_REPLAY, (Object)body);
            final MessageBytes contentType = MessageBytes.newInstance();
            String savedContentType = saved.getContentType();
            if (savedContentType == null && "POST".equalsIgnoreCase(method)) {
                savedContentType = "application/x-www-form-urlencoded";
            }
            contentType.setString(savedContentType);
            request.getCoyoteRequest().setContentType(contentType);
        }
        request.getCoyoteRequest().method().setString(method);
        return true;
    }
    
    protected void saveRequest(final Request request, final Session session) throws IOException {
        final SavedRequest saved = new SavedRequest();
        final Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (int i = 0; i < cookies.length; ++i) {
                saved.addCookie(cookies[i]);
            }
        }
        final Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            final String name = names.nextElement();
            final Enumeration<String> values = request.getHeaders(name);
            while (values.hasMoreElements()) {
                final String value = values.nextElement();
                saved.addHeader(name, value);
            }
        }
        final Enumeration<Locale> locales = request.getLocales();
        while (locales.hasMoreElements()) {
            final Locale locale = locales.nextElement();
            saved.addLocale(locale);
        }
        request.getResponse().sendAcknowledgement();
        final ByteChunk body = new ByteChunk();
        body.setLimit(request.getConnector().getMaxSavePostSize());
        final byte[] buffer = new byte[4096];
        final InputStream is = (InputStream)request.getInputStream();
        int bytesRead;
        while ((bytesRead = is.read(buffer)) >= 0) {
            body.append(buffer, 0, bytesRead);
        }
        if (body.getLength() > 0) {
            saved.setContentType(request.getContentType());
            saved.setBody(body);
        }
        saved.setMethod(request.getMethod());
        saved.setQueryString(request.getQueryString());
        saved.setRequestURI(request.getRequestURI());
        saved.setDecodedRequestURI(request.getDecodedRequestURI());
        session.setNote("org.apache.catalina.authenticator.REQUEST", saved);
    }
    
    protected String savedRequestURL(final Session session) {
        final SavedRequest saved = (SavedRequest)session.getNote("org.apache.catalina.authenticator.REQUEST");
        if (saved == null) {
            return null;
        }
        final StringBuilder sb = new StringBuilder(saved.getRequestURI());
        if (saved.getQueryString() != null) {
            sb.append('?');
            sb.append(saved.getQueryString());
        }
        return sb.toString();
    }
    
    static {
        log = LogFactory.getLog((Class)FormAuthenticator.class);
    }
}
