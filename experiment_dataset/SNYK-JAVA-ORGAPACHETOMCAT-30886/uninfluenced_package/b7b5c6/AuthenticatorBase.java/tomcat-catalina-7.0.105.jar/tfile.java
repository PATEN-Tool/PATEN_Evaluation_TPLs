// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.catalina.authenticator;

import org.apache.tomcat.util.http.FastHttpDateFormat;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Valve;
import org.apache.catalina.util.StandardSessionIdGenerator;
import org.apache.tomcat.util.ExceptionUtils;
import javax.servlet.http.Cookie;
import java.util.List;
import org.apache.catalina.realm.GenericPrincipal;
import org.apache.coyote.ActionCode;
import org.apache.catalina.deploy.FilterMap;
import org.apache.catalina.deploy.FilterDef;
import org.apache.catalina.filters.CorsFilter;
import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;
import org.apache.tomcat.util.http.RequestUtil;
import javax.servlet.ServletException;
import java.io.IOException;
import java.security.cert.X509Certificate;
import org.apache.catalina.deploy.SecurityConstraint;
import org.apache.catalina.Realm;
import org.apache.catalina.Session;
import java.security.Principal;
import org.apache.catalina.deploy.LoginConfig;
import javax.servlet.http.HttpServletResponse;
import org.apache.catalina.connector.Response;
import org.apache.catalina.connector.Request;
import org.apache.catalina.Container;
import java.util.Locale;
import org.apache.juli.logging.LogFactory;
import org.apache.catalina.util.SessionIdGeneratorBase;
import org.apache.catalina.Context;
import org.apache.tomcat.util.res.StringManager;
import org.apache.juli.logging.Log;
import org.apache.catalina.Authenticator;
import org.apache.catalina.valves.ValveBase;

public abstract class AuthenticatorBase extends ValveBase implements Authenticator
{
    private final Log log;
    private static final String DATE_ONE;
    protected static final StringManager sm;
    protected static final String AUTH_HEADER_NAME = "WWW-Authenticate";
    protected static final String REALM_NAME = "Authentication required";
    protected boolean alwaysUseSession;
    protected boolean cache;
    protected boolean changeSessionIdOnAuthentication;
    protected Context context;
    protected static final String info = "org.apache.catalina.authenticator.AuthenticatorBase/1.0";
    protected boolean disableProxyCaching;
    protected boolean securePagesWithPragma;
    protected String secureRandomClass;
    protected String secureRandomAlgorithm;
    protected String secureRandomProvider;
    protected boolean sendAuthInfoResponseHeaders;
    protected SessionIdGeneratorBase sessionIdGenerator;
    protected SingleSignOn sso;
    private AllowCorsPreflight allowCorsPreflight;
    
    public AuthenticatorBase() {
        super(true);
        this.log = LogFactory.getLog((Class)AuthenticatorBase.class);
        this.alwaysUseSession = false;
        this.cache = true;
        this.changeSessionIdOnAuthentication = true;
        this.context = null;
        this.disableProxyCaching = true;
        this.securePagesWithPragma = false;
        this.secureRandomClass = null;
        this.secureRandomAlgorithm = "SHA1PRNG";
        this.secureRandomProvider = null;
        this.sendAuthInfoResponseHeaders = false;
        this.sessionIdGenerator = null;
        this.sso = null;
        this.allowCorsPreflight = AllowCorsPreflight.NEVER;
    }
    
    public String getAllowCorsPreflight() {
        return this.allowCorsPreflight.name().toLowerCase(Locale.ENGLISH);
    }
    
    public void setAllowCorsPreflight(final String allowCorsPreflight) {
        this.allowCorsPreflight = AllowCorsPreflight.valueOf(allowCorsPreflight.trim().toUpperCase(Locale.ENGLISH));
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
    
    @Override
    public String getInfo() {
        return "org.apache.catalina.authenticator.AuthenticatorBase/1.0";
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
    
    public boolean isSendAuthInfoResponseHeaders() {
        return this.sendAuthInfoResponseHeaders;
    }
    
    public void setSendAuthInfoResponseHeaders(final boolean sendAuthInfoResponseHeaders) {
        this.sendAuthInfoResponseHeaders = sendAuthInfoResponseHeaders;
    }
    
    @Override
    public void invoke(final Request request, final Response response) throws IOException, ServletException {
        if (this.log.isDebugEnabled()) {
            this.log.debug((Object)("Security checking request " + request.getMethod() + " " + request.getRequestURI()));
        }
        final LoginConfig config = this.context.getLoginConfig();
        if (this.cache) {
            Principal principal = request.getUserPrincipal();
            if (principal == null) {
                final Session session = request.getSessionInternal(false);
                if (session != null) {
                    principal = session.getPrincipal();
                    if (principal != null) {
                        if (this.log.isDebugEnabled()) {
                            this.log.debug((Object)("We have cached auth type " + session.getAuthType() + " for principal " + principal));
                        }
                        request.setAuthType(session.getAuthType());
                        request.setUserPrincipal(principal);
                    }
                }
            }
        }
        boolean authRequired = this.isContinuationRequired(request);
        final Realm realm = this.context.getRealm();
        final SecurityConstraint[] constraints = realm.findSecurityConstraints(request, this.context);
        if (constraints == null && !this.context.getPreemptiveAuthentication() && !authRequired) {
            if (this.log.isDebugEnabled()) {
                this.log.debug((Object)"Not subject to any constraint");
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
            if (this.log.isDebugEnabled()) {
                this.log.debug((Object)"Calling hasUserDataPermission()");
            }
            if (!realm.hasUserDataPermission(request, response, constraints)) {
                if (this.log.isDebugEnabled()) {
                    this.log.debug((Object)"Failed hasUserDataPermission() test");
                }
                return;
            }
        }
        boolean hasAuthConstraint = false;
        if (constraints != null) {
            hasAuthConstraint = true;
            for (int i = 0; i < constraints.length && hasAuthConstraint; ++i) {
                if (!constraints[i].getAuthConstraint()) {
                    hasAuthConstraint = false;
                }
                else if (!constraints[i].getAllRoles()) {
                    final String[] roles = constraints[i].findAuthRoles();
                    if (roles == null || roles.length == 0) {
                        hasAuthConstraint = false;
                    }
                }
            }
        }
        if (!authRequired && hasAuthConstraint) {
            authRequired = true;
        }
        if (!authRequired && this.context.getPreemptiveAuthentication()) {
            authRequired = (request.getCoyoteRequest().getMimeHeaders().getValue("authorization") != null);
        }
        if (!authRequired && this.context.getPreemptiveAuthentication() && "CLIENT_CERT".equals(this.getAuthMethod())) {
            final X509Certificate[] certs = this.getRequestCertificates(request);
            authRequired = (certs != null && certs.length > 0);
        }
        if ((authRequired || constraints != null) && this.allowCorsPreflightBypass(request)) {
            if (this.log.isDebugEnabled()) {
                this.log.debug((Object)"CORS Preflight request bypassing authentication");
            }
            this.getNext().invoke(request, response);
            return;
        }
        if (authRequired) {
            if (this.log.isDebugEnabled()) {
                this.log.debug((Object)"Calling authenticate()");
            }
            if (!this.authenticate(request, (HttpServletResponse)response, config)) {
                if (this.log.isDebugEnabled()) {
                    this.log.debug((Object)"Failed authenticate() test");
                }
                return;
            }
        }
        if (constraints != null) {
            if (this.log.isDebugEnabled()) {
                this.log.debug((Object)"Calling accessControl()");
            }
            if (!realm.hasResourcePermission(request, response, constraints, this.context)) {
                if (this.log.isDebugEnabled()) {
                    this.log.debug((Object)"Failed accessControl() test");
                }
                return;
            }
        }
        if (this.log.isDebugEnabled()) {
            this.log.debug((Object)"Successfully passed all security constraints");
        }
        this.getNext().invoke(request, response);
    }
    
    protected boolean allowCorsPreflightBypass(final Request request) {
        boolean allowBypass = false;
        if (this.allowCorsPreflight != AllowCorsPreflight.NEVER && "OPTIONS".equals(request.getMethod())) {
            final String originHeader = request.getHeader("Origin");
            if (originHeader != null && !originHeader.isEmpty() && RequestUtil.isValidOrigin(originHeader) && !RequestUtil.isSameOrigin((HttpServletRequest)request, originHeader)) {
                final String accessControlRequestMethodHeader = request.getHeader("Access-Control-Request-Method");
                if (accessControlRequestMethodHeader != null && !accessControlRequestMethodHeader.isEmpty()) {
                    if (this.allowCorsPreflight == AllowCorsPreflight.ALWAYS) {
                        allowBypass = true;
                    }
                    else if (this.allowCorsPreflight == AllowCorsPreflight.FILTER && DispatcherType.REQUEST == request.getDispatcherType()) {
                        for (final FilterDef filterDef : request.getContext().findFilterDefs()) {
                            if (CorsFilter.class.getName().equals(filterDef.getFilterClass())) {
                                final FilterMap[] arr$2 = this.context.findFilterMaps();
                                final int len$2 = arr$2.length;
                                int i$2 = 0;
                                while (i$2 < len$2) {
                                    final FilterMap filterMap = arr$2[i$2];
                                    if (filterMap.getFilterName().equals(filterDef.getFilterName())) {
                                        if ((filterMap.getDispatcherMapping() & 0x8) > 0) {
                                            for (final String urlPattern : filterMap.getURLPatterns()) {
                                                if ("/*".equals(urlPattern)) {
                                                    allowBypass = true;
                                                    break;
                                                }
                                            }
                                            break;
                                        }
                                        break;
                                    }
                                    else {
                                        ++i$2;
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }
        return allowBypass;
    }
    
    @Override
    public boolean authenticate(final Request request, final HttpServletResponse httpResponse) throws IOException {
        return this.context == null || this.context.getLoginConfig() == null || this.authenticate(request, httpResponse, this.context.getLoginConfig());
    }
    
    @Override
    public abstract boolean authenticate(final Request p0, final HttpServletResponse p1, final LoginConfig p2) throws IOException;
    
    protected boolean isContinuationRequired(final Request request) {
        return false;
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
    
    protected boolean checkForCachedAuthentication(final Request request, final HttpServletResponse response, final boolean useSSO) {
        final Principal principal = request.getUserPrincipal();
        final String ssoId = (String)request.getNote("org.apache.catalina.request.SSOID");
        if (principal != null) {
            if (this.log.isDebugEnabled()) {
                this.log.debug((Object)AuthenticatorBase.sm.getString("authenticator.check.found", new Object[] { principal.getName() }));
            }
            if (ssoId != null) {
                this.associate(ssoId, request.getSessionInternal(true));
            }
            return true;
        }
        if (useSSO && ssoId != null) {
            if (this.log.isDebugEnabled()) {
                this.log.debug((Object)AuthenticatorBase.sm.getString("authenticator.check.sso", new Object[] { ssoId }));
            }
            if (this.reauthenticateFromSSO(ssoId, request)) {
                return true;
            }
        }
        if (request.getCoyoteRequest().getRemoteUserNeedsAuthorization()) {
            final String username = request.getCoyoteRequest().getRemoteUser().toString();
            if (username != null) {
                if (this.log.isDebugEnabled()) {
                    this.log.debug((Object)AuthenticatorBase.sm.getString("authenticator.check.authorize", new Object[] { username }));
                }
                Principal authorized = this.context.getRealm().authenticate(username);
                if (authorized == null) {
                    if (this.log.isDebugEnabled()) {
                        this.log.debug((Object)AuthenticatorBase.sm.getString("authenticator.check.authorizeFail", new Object[] { username }));
                    }
                    authorized = new GenericPrincipal(username, null, null);
                }
                String authType = request.getAuthType();
                if (authType == null || authType.length() == 0) {
                    authType = this.getAuthMethod();
                }
                this.register(request, response, authorized, authType, username, null);
                return true;
            }
        }
        return false;
    }
    
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
            if (this.log.isDebugEnabled()) {
                this.log.debug((Object)("Reauthenticated cached principal '" + request.getUserPrincipal().getName() + "' with auth type '" + request.getAuthType() + "'"));
            }
        }
        return reauthenticated;
    }
    
    public void register(final Request request, final HttpServletResponse response, final Principal principal, final String authType, final String username, final String password) {
        if (this.log.isDebugEnabled()) {
            final String name = (principal == null) ? "none" : principal.getName();
            this.log.debug((Object)("Authenticated '" + name + "' with type '" + authType + "'"));
        }
        request.setAuthType(authType);
        request.setUserPrincipal(principal);
        if (this.sendAuthInfoResponseHeaders && Boolean.TRUE.equals(request.getAttribute("org.apache.tomcat.request.forwarded"))) {
            response.setHeader("remote-user", request.getRemoteUser());
            response.setHeader("auth-type", request.getAuthType());
        }
        Session session = request.getSessionInternal(false);
        if (session != null) {
            if (this.getChangeSessionIdOnAuthentication() && principal != null) {
                final String newSessionId = this.changeSessionID(request, session);
                if (session.getNote("org.apache.catalina.authenticator.SESSION_ID") != null) {
                    session.setNote("org.apache.catalina.authenticator.SESSION_ID", newSessionId);
                }
            }
        }
        else if (this.alwaysUseSession) {
            session = request.getSessionInternal(true);
        }
        if (session != null && this.cache) {
            session.setAuthType(authType);
            session.setPrincipal(principal);
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
    
    protected String changeSessionID(final Request request, final Session session) {
        String oldId = null;
        if (this.log.isDebugEnabled()) {
            oldId = session.getId();
        }
        final String newId = request.changeSessionId();
        if (this.log.isDebugEnabled()) {
            this.log.debug((Object)AuthenticatorBase.sm.getString("authenticator.changeSessionId", new Object[] { oldId, newId }));
        }
        return newId;
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
    public void logout(final Request request) throws ServletException {
        final Principal p = request.getPrincipal();
        if (p instanceof GenericPrincipal) {
            try {
                ((GenericPrincipal)p).logout();
            }
            catch (Throwable t) {
                ExceptionUtils.handleThrowable(t);
                this.log.debug((Object)AuthenticatorBase.sm.getString("authenticator.tomcatPrincipalLogoutFail"), t);
            }
        }
        this.register(request, (HttpServletResponse)request.getResponse(), null, null, null, null);
    }
    
    @Override
    protected synchronized void startInternal() throws LifecycleException {
        for (Container parent = this.context.getParent(); this.sso == null && parent != null; parent = parent.getParent()) {
            final Valve[] arr$;
            final Valve[] valves = arr$ = parent.getPipeline().getValves();
            for (final Valve valve : arr$) {
                if (valve instanceof SingleSignOn) {
                    this.sso = (SingleSignOn)valve;
                    break;
                }
            }
            if (this.sso == null) {}
        }
        if (this.log.isDebugEnabled()) {
            if (this.sso != null) {
                this.log.debug((Object)("Found SingleSignOn Valve at " + this.sso));
            }
            else {
                this.log.debug((Object)"No SingleSignOn Valve is present");
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
        DATE_ONE = FastHttpDateFormat.formatDate(1L);
        sm = StringManager.getManager((Class)AuthenticatorBase.class);
    }
    
    protected enum AllowCorsPreflight
    {
        NEVER, 
        FILTER, 
        ALWAYS;
    }
}
