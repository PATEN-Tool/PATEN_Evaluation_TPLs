// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.catalina.authenticator;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;
import org.apache.juli.logging.LogFactory;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Valve;
import org.apache.catalina.util.StandardSessionIdGenerator;
import org.apache.catalina.Manager;
import javax.servlet.http.Cookie;
import org.apache.coyote.ActionCode;
import javax.servlet.ServletException;
import java.io.IOException;
import java.security.cert.X509Certificate;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.apache.catalina.Realm;
import org.apache.catalina.Wrapper;
import org.apache.catalina.Session;
import java.security.Principal;
import javax.servlet.http.HttpServletResponse;
import org.apache.catalina.connector.Response;
import org.apache.catalina.connector.Request;
import org.apache.catalina.Container;
import org.apache.tomcat.util.descriptor.web.LoginConfig;
import org.apache.tomcat.util.res.StringManager;
import org.apache.catalina.util.SessionIdGeneratorBase;
import org.apache.catalina.Context;
import org.apache.juli.logging.Log;
import org.apache.catalina.Authenticator;
import org.apache.catalina.valves.ValveBase;

public abstract class AuthenticatorBase extends ValveBase implements Authenticator
{
    private static final Log log;
    protected static final String AUTH_HEADER_NAME = "WWW-Authenticate";
    protected static final String REALM_NAME = "Authentication required";
    protected boolean alwaysUseSession;
    protected boolean cache;
    protected boolean changeSessionIdOnAuthentication;
    protected Context context;
    protected boolean disableProxyCaching;
    protected boolean securePagesWithPragma;
    protected String secureRandomClass;
    protected String secureRandomAlgorithm;
    protected String secureRandomProvider;
    protected SessionIdGeneratorBase sessionIdGenerator;
    protected static final StringManager sm;
    protected SingleSignOn sso;
    private static final String DATE_ONE;
    
    public AuthenticatorBase() {
        super(true);
        this.alwaysUseSession = false;
        this.cache = true;
        this.changeSessionIdOnAuthentication = true;
        this.context = null;
        this.disableProxyCaching = true;
        this.securePagesWithPragma = false;
        this.secureRandomClass = null;
        this.secureRandomAlgorithm = "SHA1PRNG";
        this.secureRandomProvider = null;
        this.sessionIdGenerator = null;
        this.sso = null;
    }
    
    protected static String getRealmName(final Context context) {
        if (context == null) {
            return "Authentication required";
        }
        final LoginConfig config = context.getLoginConfig();
        if (config == null) {
            return "Authentication required";
        }
        final String result = config.getRealmName();
        if (result == null) {
            return "Authentication required";
        }
        return result;
    }
    
    public boolean getAlwaysUseSession() {
        return this.alwaysUseSession;
    }
    
    public void setAlwaysUseSession(final boolean alwaysUseSession) {
        this.alwaysUseSession = alwaysUseSession;
    }
    
    public boolean getCache() {
        return this.cache;
    }
    
    public void setCache(final boolean cache) {
        this.cache = cache;
    }
    
    @Override
    public Container getContainer() {
        return this.context;
    }
    
    @Override
    public void setContainer(final Container container) {
        if (container != null && !(container instanceof Context)) {
            throw new IllegalArgumentException(AuthenticatorBase.sm.getString("authenticator.notContext"));
        }
        super.setContainer(container);
        this.context = (Context)container;
    }
    
    public boolean getDisableProxyCaching() {
        return this.disableProxyCaching;
    }
    
    public void setDisableProxyCaching(final boolean nocache) {
        this.disableProxyCaching = nocache;
    }
    
    public boolean getSecurePagesWithPragma() {
        return this.securePagesWithPragma;
    }
    
    public void setSecurePagesWithPragma(final boolean securePagesWithPragma) {
        this.securePagesWithPragma = securePagesWithPragma;
    }
    
    public boolean getChangeSessionIdOnAuthentication() {
        return this.changeSessionIdOnAuthentication;
    }
    
    public void setChangeSessionIdOnAuthentication(final boolean changeSessionIdOnAuthentication) {
        this.changeSessionIdOnAuthentication = changeSessionIdOnAuthentication;
    }
    
    public String getSecureRandomClass() {
        return this.secureRandomClass;
    }
    
    public void setSecureRandomClass(final String secureRandomClass) {
        this.secureRandomClass = secureRandomClass;
    }
    
    public String getSecureRandomAlgorithm() {
        return this.secureRandomAlgorithm;
    }
    
    public void setSecureRandomAlgorithm(final String secureRandomAlgorithm) {
        this.secureRandomAlgorithm = secureRandomAlgorithm;
    }
    
    public String getSecureRandomProvider() {
        return this.secureRandomProvider;
    }
    
    public void setSecureRandomProvider(final String secureRandomProvider) {
        this.secureRandomProvider = secureRandomProvider;
    }
    
    @Override
    public void invoke(final Request request, final Response response) throws IOException, ServletException {
        if (AuthenticatorBase.log.isDebugEnabled()) {
            AuthenticatorBase.log.debug((Object)("Security checking request " + request.getMethod() + " " + request.getRequestURI()));
        }
        if (this.cache) {
            Principal principal = request.getUserPrincipal();
            if (principal == null) {
                final Session session = request.getSessionInternal(false);
                if (session != null) {
                    principal = session.getPrincipal();
                    if (principal != null) {
                        if (AuthenticatorBase.log.isDebugEnabled()) {
                            AuthenticatorBase.log.debug((Object)("We have cached auth type " + session.getAuthType() + " for principal " + session.getPrincipal()));
                        }
                        request.setAuthType(session.getAuthType());
                        request.setUserPrincipal(principal);
                    }
                }
            }
        }
        final String contextPath = this.context.getPath();
        final String requestURI = request.getDecodedRequestURI();
        if (requestURI.startsWith(contextPath) && requestURI.endsWith("/j_security_check") && !this.authenticate(request, (HttpServletResponse)response)) {
            if (AuthenticatorBase.log.isDebugEnabled()) {
                AuthenticatorBase.log.debug((Object)(" Failed authenticate() test ??" + requestURI));
            }
            return;
        }
        final Session session2 = request.getSessionInternal(false);
        if (session2 != null) {
            final SavedRequest savedRequest = (SavedRequest)session2.getNote("org.apache.catalina.authenticator.REQUEST");
            if (savedRequest != null) {
                final String decodedRequestURI = request.getDecodedRequestURI();
                if (decodedRequestURI != null && decodedRequestURI.equals(savedRequest.getDecodedRequestURI()) && !this.authenticate(request, (HttpServletResponse)response)) {
                    if (AuthenticatorBase.log.isDebugEnabled()) {
                        AuthenticatorBase.log.debug((Object)" Failed authenticate() test");
                    }
                    return;
                }
            }
        }
        final Wrapper wrapper = request.getMappingData().wrapper;
        if (wrapper != null) {
            wrapper.servletSecurityAnnotationScan();
        }
        final Realm realm = this.context.getRealm();
        final SecurityConstraint[] constraints = realm.findSecurityConstraints(request, this.context);
        if (constraints == null && !this.context.getPreemptiveAuthentication()) {
            if (AuthenticatorBase.log.isDebugEnabled()) {
                AuthenticatorBase.log.debug((Object)" Not subject to any constraint");
            }
            this.getNext().invoke(request, response);
            return;
        }
        if (constraints != null && this.disableProxyCaching && !"POST".equalsIgnoreCase(request.getMethod())) {
            if (this.securePagesWithPragma) {
                response.setHeader("Pragma", "No-cache");
                response.setHeader("Cache-Control", "no-cache");
            }
            else {
                response.setHeader("Cache-Control", "private");
            }
            response.setHeader("Expires", AuthenticatorBase.DATE_ONE);
        }
        if (constraints != null) {
            if (AuthenticatorBase.log.isDebugEnabled()) {
                AuthenticatorBase.log.debug((Object)" Calling hasUserDataPermission()");
            }
            if (!realm.hasUserDataPermission(request, response, constraints)) {
                if (AuthenticatorBase.log.isDebugEnabled()) {
                    AuthenticatorBase.log.debug((Object)" Failed hasUserDataPermission() test");
                }
                return;
            }
        }
        boolean authRequired;
        if (constraints == null) {
            authRequired = false;
        }
        else {
            authRequired = true;
            for (int i = 0; i < constraints.length && authRequired; ++i) {
                if (!constraints[i].getAuthConstraint()) {
                    authRequired = false;
                    break;
                }
                if (!constraints[i].getAllRoles() && !constraints[i].getAuthenticatedUsers()) {
                    final String[] roles = constraints[i].findAuthRoles();
                    if (roles == null || roles.length == 0) {
                        authRequired = false;
                        break;
                    }
                }
            }
        }
        if (!authRequired && this.context.getPreemptiveAuthentication()) {
            authRequired = (request.getCoyoteRequest().getMimeHeaders().getValue("authorization") != null);
        }
        if (!authRequired && this.context.getPreemptiveAuthentication() && "CLIENT_CERT".equals(this.getAuthMethod())) {
            final X509Certificate[] certs = this.getRequestCertificates(request);
            authRequired = (certs != null && certs.length > 0);
        }
        if (authRequired) {
            if (AuthenticatorBase.log.isDebugEnabled()) {
                AuthenticatorBase.log.debug((Object)" Calling authenticate()");
            }
            if (!this.authenticate(request, (HttpServletResponse)response)) {
                if (AuthenticatorBase.log.isDebugEnabled()) {
                    AuthenticatorBase.log.debug((Object)" Failed authenticate() test");
                }
                return;
            }
        }
        if (constraints != null) {
            if (AuthenticatorBase.log.isDebugEnabled()) {
                AuthenticatorBase.log.debug((Object)" Calling accessControl()");
            }
            if (!realm.hasResourcePermission(request, response, constraints, this.context)) {
                if (AuthenticatorBase.log.isDebugEnabled()) {
                    AuthenticatorBase.log.debug((Object)" Failed accessControl() test");
                }
                return;
            }
        }
        if (AuthenticatorBase.log.isDebugEnabled()) {
            AuthenticatorBase.log.debug((Object)" Successfully passed all security constraints");
        }
        this.getNext().invoke(request, response);
    }
    
    protected X509Certificate[] getRequestCertificates(final Request request) throws IllegalStateException {
        X509Certificate[] certs = (X509Certificate[])request.getAttribute("javax.servlet.request.X509Certificate");
        if (certs != null) {
            if (certs.length >= 1) {
                return certs;
            }
        }
        try {
            request.getCoyoteRequest().action(ActionCode.REQ_SSL_CERTIFICATE, (Object)null);
            certs = (X509Certificate[])request.getAttribute("javax.servlet.request.X509Certificate");
        }
        catch (IllegalStateException ex) {}
        return certs;
    }
    
    protected void associate(final String ssoId, final Session session) {
        if (this.sso == null) {
            return;
        }
        this.sso.associate(ssoId, session);
    }
    
    @Override
    public abstract boolean authenticate(final Request p0, final HttpServletResponse p1) throws IOException;
    
    protected boolean reauthenticateFromSSO(final String ssoId, final Request request) {
        if (this.sso == null || ssoId == null) {
            return false;
        }
        boolean reauthenticated = false;
        final Container parent = this.getContainer();
        if (parent != null) {
            final Realm realm = parent.getRealm();
            if (realm != null) {
                reauthenticated = this.sso.reauthenticate(ssoId, realm, request);
            }
        }
        if (reauthenticated) {
            this.associate(ssoId, request.getSessionInternal(true));
            if (AuthenticatorBase.log.isDebugEnabled()) {
                AuthenticatorBase.log.debug((Object)(" Reauthenticated cached principal '" + request.getUserPrincipal().getName() + "' with auth type '" + request.getAuthType() + "'"));
            }
        }
        return reauthenticated;
    }
    
    public void register(final Request request, final HttpServletResponse response, final Principal principal, final String authType, final String username, final String password) {
        if (AuthenticatorBase.log.isDebugEnabled()) {
            final String name = (principal == null) ? "none" : principal.getName();
            AuthenticatorBase.log.debug((Object)("Authenticated '" + name + "' with type '" + authType + "'"));
        }
        request.setAuthType(authType);
        request.setUserPrincipal(principal);
        Session session = request.getSessionInternal(false);
        if (session != null) {
            if (this.changeSessionIdOnAuthentication) {
                String oldId = null;
                if (AuthenticatorBase.log.isDebugEnabled()) {
                    oldId = session.getId();
                }
                final Manager manager = request.getContext().getManager();
                manager.changeSessionId(session);
                request.changeSessionId(session.getId());
                if (AuthenticatorBase.log.isDebugEnabled()) {
                    AuthenticatorBase.log.debug((Object)AuthenticatorBase.sm.getString("authenticator.changeSessionId", new Object[] { oldId, session.getId() }));
                }
            }
        }
        else if (this.alwaysUseSession) {
            session = request.getSessionInternal(true);
        }
        if (this.cache && session != null) {
            session.setAuthType(authType);
            session.setPrincipal(principal);
            if (username != null) {
                session.setNote("org.apache.catalina.session.USERNAME", username);
            }
            else {
                session.removeNote("org.apache.catalina.session.USERNAME");
            }
            if (password != null) {
                session.setNote("org.apache.catalina.session.PASSWORD", password);
            }
            else {
                session.removeNote("org.apache.catalina.session.PASSWORD");
            }
        }
        if (this.sso == null) {
            return;
        }
        String ssoId = (String)request.getNote("org.apache.catalina.request.SSOID");
        if (ssoId == null) {
            ssoId = this.sessionIdGenerator.generateSessionId();
            final Cookie cookie = new Cookie(Constants.SINGLE_SIGN_ON_COOKIE, ssoId);
            cookie.setMaxAge(-1);
            cookie.setPath("/");
            cookie.setSecure(request.isSecure());
            final String ssoDomain = this.sso.getCookieDomain();
            if (ssoDomain != null) {
                cookie.setDomain(ssoDomain);
            }
            if (request.getServletContext().getSessionCookieConfig().isHttpOnly() || request.getContext().getUseHttpOnly()) {
                cookie.setHttpOnly(true);
            }
            response.addCookie(cookie);
            this.sso.register(ssoId, principal, authType, username, password);
            request.setNote("org.apache.catalina.request.SSOID", ssoId);
        }
        else {
            if (principal == null) {
                this.sso.deregister(ssoId);
                request.removeNote("org.apache.catalina.request.SSOID");
                return;
            }
            this.sso.update(ssoId, principal, authType, username, password);
        }
        if (session == null) {
            session = request.getSessionInternal(true);
        }
        this.sso.associate(ssoId, session);
    }
    
    @Override
    public void login(final String username, final String password, final Request request) throws ServletException {
        final Principal principal = this.doLogin(request, username, password);
        this.register(request, (HttpServletResponse)request.getResponse(), principal, this.getAuthMethod(), username, password);
    }
    
    protected abstract String getAuthMethod();
    
    protected Principal doLogin(final Request request, final String username, final String password) throws ServletException {
        final Principal p = this.context.getRealm().authenticate(username, password);
        if (p == null) {
            throw new ServletException(AuthenticatorBase.sm.getString("authenticator.loginFail"));
        }
        return p;
    }
    
    @Override
    public void logout(final Request request) {
        this.register(request, (HttpServletResponse)request.getResponse(), null, null, null, null);
    }
    
    @Override
    protected synchronized void startInternal() throws LifecycleException {
        for (Container parent = this.context.getParent(); this.sso == null && parent != null; parent = parent.getParent()) {
            final Valve[] valves = parent.getPipeline().getValves();
            for (int i = 0; i < valves.length; ++i) {
                if (valves[i] instanceof SingleSignOn) {
                    this.sso = (SingleSignOn)valves[i];
                    break;
                }
            }
            if (this.sso == null) {}
        }
        if (AuthenticatorBase.log.isDebugEnabled()) {
            if (this.sso != null) {
                AuthenticatorBase.log.debug((Object)("Found SingleSignOn Valve at " + this.sso));
            }
            else {
                AuthenticatorBase.log.debug((Object)"No SingleSignOn Valve is present");
            }
        }
        (this.sessionIdGenerator = new StandardSessionIdGenerator()).setSecureRandomAlgorithm(this.getSecureRandomAlgorithm());
        this.sessionIdGenerator.setSecureRandomClass(this.getSecureRandomClass());
        this.sessionIdGenerator.setSecureRandomProvider(this.getSecureRandomProvider());
        super.startInternal();
    }
    
    @Override
    protected synchronized void stopInternal() throws LifecycleException {
        super.stopInternal();
        this.sso = null;
    }
    
    static {
        log = LogFactory.getLog((Class)AuthenticatorBase.class);
        sm = StringManager.getManager("org.apache.catalina.authenticator");
        DATE_ONE = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US).format(new Date(1L));
    }
}
