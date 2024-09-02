// 
// Decompiled by Procyon v0.5.36
// 

package org.keycloak.services.managers;

import java.security.PublicKey;
import org.keycloak.common.VerificationException;
import org.keycloak.jose.jws.AlgorithmType;
import java.util.Comparator;
import java.util.ArrayList;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.authentication.AuthenticationFlowException;
import org.keycloak.authentication.ConsoleDisplayMode;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.DisplayTypeRequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.authentication.RequiredActionContextResult;
import java.util.HashSet;
import java.util.LinkedList;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.ActionTokenKeyModel;
import org.keycloak.events.EventType;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.ActionTokenStoreProvider;
import org.keycloak.authentication.actiontoken.DefaultActionTokenKey;
import javax.ws.rs.core.UriBuilder;
import org.keycloak.services.resources.LoginActionsService;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.models.ClientSessionContext;
import java.net.URI;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.models.KeyManager;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.services.util.P3PHelper;
import org.keycloak.services.util.CookieHelper;
import org.keycloak.common.util.Base64Url;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.events.EventBuilder;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.keycloak.protocol.oidc.TokenManager;
import javax.ws.rs.core.Response;
import org.keycloak.services.ServicesLogger;
import org.keycloak.protocol.LoginProtocol;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.function.Function;
import java.util.Map;
import org.keycloak.models.AuthenticatedClientSessionModel;
import java.util.Set;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.services.resources.IdentityBrokerService;
import java.util.Optional;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.models.ClientModel;
import java.util.Objects;
import org.keycloak.sessions.CommonClientSessionModel;
import org.keycloak.models.utils.SystemClientUtil;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.models.UserModel;
import javax.crypto.SecretKey;
import org.keycloak.services.Urls;
import org.keycloak.TokenVerifier;
import org.keycloak.representations.AccessToken;
import javax.ws.rs.core.Cookie;
import org.keycloak.common.ClientConnection;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
import org.keycloak.models.KeycloakSession;
import org.keycloak.common.util.Time;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.RealmModel;
import org.jboss.logging.Logger;

public class AuthenticationManager
{
    public static final String SET_REDIRECT_URI_AFTER_REQUIRED_ACTIONS = "SET_REDIRECT_URI_AFTER_REQUIRED_ACTIONS";
    public static final String END_AFTER_REQUIRED_ACTIONS = "END_AFTER_REQUIRED_ACTIONS";
    public static final String INVALIDATE_ACTION_TOKEN = "INVALIDATE_ACTION_TOKEN";
    public static final String CLIENT_LOGOUT_STATE = "logout.state.";
    public static final String AUTH_TIME = "AUTH_TIME";
    public static final String SSO_AUTH = "SSO_AUTH";
    protected static final Logger logger;
    public static final String FORM_USERNAME = "username";
    public static final String KEYCLOAK_IDENTITY_COOKIE = "KEYCLOAK_IDENTITY";
    public static final String KEYCLOAK_SESSION_COOKIE = "KEYCLOAK_SESSION";
    public static final String KEYCLOAK_REMEMBER_ME = "KEYCLOAK_REMEMBER_ME";
    public static final String KEYCLOAK_LOGOUT_PROTOCOL = "KEYCLOAK_LOGOUT_PROTOCOL";
    
    public static boolean isSessionValid(final RealmModel realm, final UserSessionModel userSession) {
        if (userSession == null) {
            AuthenticationManager.logger.debug((Object)"No user session");
            return false;
        }
        final int currentTime = Time.currentTime();
        final int max = userSession.getStarted() + realm.getSsoSessionMaxLifespan();
        final int maxIdle = realm.getSsoSessionIdleTimeout() + 120;
        return userSession.getLastSessionRefresh() + maxIdle > currentTime && max > currentTime;
    }
    
    public static boolean isOfflineSessionValid(final RealmModel realm, final UserSessionModel userSession) {
        if (userSession == null) {
            AuthenticationManager.logger.debug((Object)"No offline user session");
            return false;
        }
        final int currentTime = Time.currentTime();
        final int maxIdle = realm.getOfflineSessionIdleTimeout() + 120;
        if (realm.isOfflineSessionMaxLifespanEnabled()) {
            final int max = userSession.getStarted() + realm.getOfflineSessionMaxLifespan();
            return userSession.getLastSessionRefresh() + maxIdle > currentTime && max > currentTime;
        }
        return userSession.getLastSessionRefresh() + maxIdle > currentTime;
    }
    
    public static void expireUserSessionCookie(final KeycloakSession session, final UserSessionModel userSession, final RealmModel realm, final UriInfo uriInfo, final HttpHeaders headers, final ClientConnection connection) {
        try {
            final Cookie cookie = headers.getCookies().get("KEYCLOAK_IDENTITY");
            if (cookie == null) {
                return;
            }
            final String tokenString = cookie.getValue();
            final TokenVerifier<AccessToken> verifier = (TokenVerifier<AccessToken>)TokenVerifier.create(tokenString, (Class)AccessToken.class).realmUrl(Urls.realmIssuer(uriInfo.getBaseUri(), realm.getName())).checkActive(false).checkTokenType(false);
            final String kid = verifier.getHeader().getKeyId();
            final SecretKey secretKey = session.keys().getHmacSecretKey(realm, kid);
            final AccessToken token = (AccessToken)verifier.secretKey(secretKey).verify().getToken();
            final UserSessionModel cookieSession = session.sessions().getUserSession(realm, token.getSessionState());
            if (cookieSession == null || !cookieSession.getId().equals(userSession.getId())) {
                return;
            }
            expireIdentityCookie(realm, uriInfo, connection);
        }
        catch (Exception ex) {}
    }
    
    public static void backchannelLogout(final KeycloakSession session, final UserSessionModel userSession, final boolean logoutBroker) {
        backchannelLogout(session, session.getContext().getRealm(), userSession, (UriInfo)session.getContext().getUri(), session.getContext().getConnection(), session.getContext().getRequestHeaders(), logoutBroker);
    }
    
    public static void backchannelLogout(final KeycloakSession session, final RealmModel realm, final UserSessionModel userSession, final UriInfo uriInfo, final ClientConnection connection, final HttpHeaders headers, final boolean logoutBroker) {
        backchannelLogout(session, realm, userSession, uriInfo, connection, headers, logoutBroker, false);
    }
    
    public static void backchannelLogout(final KeycloakSession session, final RealmModel realm, final UserSessionModel userSession, final UriInfo uriInfo, final ClientConnection connection, final HttpHeaders headers, final boolean logoutBroker, final boolean offlineSession) {
        if (userSession == null) {
            return;
        }
        final UserModel user = userSession.getUser();
        if (userSession.getState() != UserSessionModel.State.LOGGING_OUT) {
            userSession.setState(UserSessionModel.State.LOGGING_OUT);
        }
        AuthenticationManager.logger.debugv("Logging out: {0} ({1}) offline: {2}", (Object)user.getUsername(), (Object)userSession.getId(), (Object)userSession.isOffline());
        expireUserSessionCookie(session, userSession, realm, uriInfo, headers, connection);
        final AuthenticationSessionManager asm = new AuthenticationSessionManager(session);
        final AuthenticationSessionModel logoutAuthSession = createOrJoinLogoutSession(session, realm, asm, userSession, false);
        try {
            backchannelLogoutAll(session, realm, userSession, logoutAuthSession, uriInfo, headers, logoutBroker);
            checkUserSessionOnlyHasLoggedOutClients(realm, userSession, logoutAuthSession);
        }
        finally {
            asm.removeAuthenticationSession(realm, logoutAuthSession, false);
        }
        userSession.setState(UserSessionModel.State.LOGGED_OUT);
        if (offlineSession) {
            new UserSessionManager(session).revokeOfflineUserSession(userSession);
            final UserSessionModel onlineUserSession = session.sessions().getUserSession(realm, userSession.getId());
            if (onlineUserSession != null) {
                session.sessions().removeUserSession(realm, onlineUserSession);
            }
        }
        else {
            session.sessions().removeUserSession(realm, userSession);
        }
    }
    
    private static AuthenticationSessionModel createOrJoinLogoutSession(final KeycloakSession session, final RealmModel realm, final AuthenticationSessionManager asm, final UserSessionModel userSession, final boolean browserCookie) {
        final ClientModel client = SystemClientUtil.getSystemClient(realm);
        RootAuthenticationSessionModel rootLogoutSession = null;
        boolean browserCookiePresent = false;
        if (browserCookie) {
            rootLogoutSession = asm.getCurrentRootAuthenticationSession(realm);
        }
        String authSessionId;
        if (rootLogoutSession != null) {
            authSessionId = rootLogoutSession.getId();
            browserCookiePresent = true;
        }
        else {
            authSessionId = userSession.getId();
            rootLogoutSession = session.authenticationSessions().getRootAuthenticationSession(realm, authSessionId);
        }
        if (rootLogoutSession == null) {
            rootLogoutSession = session.authenticationSessions().createRootAuthenticationSession(authSessionId, realm);
        }
        if (browserCookie && !browserCookiePresent) {
            asm.setAuthSessionCookie(authSessionId, realm);
        }
        final Optional<AuthenticationSessionModel> found = rootLogoutSession.getAuthenticationSessions().values().stream().filter(authSession -> client.equals(authSession.getClient()) && Objects.equals(CommonClientSessionModel.Action.LOGGING_OUT.name(), authSession.getAction())).findFirst();
        final AuthenticationSessionModel logoutAuthSession = found.isPresent() ? found.get() : rootLogoutSession.createAuthenticationSession(client);
        logoutAuthSession.setAction(CommonClientSessionModel.Action.LOGGING_OUT.name());
        return logoutAuthSession;
    }
    
    private static void backchannelLogoutAll(final KeycloakSession session, final RealmModel realm, final UserSessionModel userSession, final AuthenticationSessionModel logoutAuthSession, final UriInfo uriInfo, final HttpHeaders headers, final boolean logoutBroker) {
        userSession.getAuthenticatedClientSessions().values().forEach(clientSession -> backchannelLogoutClientSession(session, realm, clientSession, logoutAuthSession, uriInfo, headers));
        if (logoutBroker) {
            final String brokerId = userSession.getNote("identity_provider");
            if (brokerId != null) {
                final IdentityProvider identityProvider = IdentityBrokerService.getIdentityProvider(session, realm, brokerId);
                try {
                    identityProvider.backchannelLogout(session, userSession, uriInfo, realm);
                }
                catch (Exception e) {
                    AuthenticationManager.logger.warn((Object)("Exception at broker backchannel logout for broker " + brokerId), (Throwable)e);
                }
            }
        }
    }
    
    private static boolean checkUserSessionOnlyHasLoggedOutClients(final RealmModel realm, final UserSessionModel userSession, final AuthenticationSessionModel logoutAuthSession) {
        final Map<String, AuthenticatedClientSessionModel> acs = (Map<String, AuthenticatedClientSessionModel>)userSession.getAuthenticatedClientSessions();
        final Set<AuthenticatedClientSessionModel> notLoggedOutSessions = acs.entrySet().stream().filter(me -> !Objects.equals(CommonClientSessionModel.Action.LOGGED_OUT, getClientLogoutAction(logoutAuthSession, me.getKey()))).filter(me -> !Objects.equals(CommonClientSessionModel.Action.LOGGED_OUT.name(), me.getValue().getAction())).filter(me -> Objects.nonNull(me.getValue().getProtocol())).map((Function<? super Object, ?>)Map.Entry::getValue).collect((Collector<? super Object, ?, Set<AuthenticatedClientSessionModel>>)Collectors.toSet());
        final boolean allClientsLoggedOut = notLoggedOutSessions.isEmpty();
        if (!allClientsLoggedOut) {
            AuthenticationManager.logger.warnf("Some clients have been not been logged out for user %s in %s realm: %s", (Object)userSession.getUser().getUsername(), (Object)realm.getName(), (Object)notLoggedOutSessions.stream().map((Function<? super Object, ?>)CommonClientSessionModel::getClient).map((Function<? super Object, ?>)ClientModel::getClientId).sorted().collect((Collector<? super Object, ?, String>)Collectors.joining(", ")));
        }
        else if (AuthenticationManager.logger.isDebugEnabled()) {
            AuthenticationManager.logger.debugf("All clients have been logged out for user %s in %s realm, session %s", (Object)userSession.getUser().getUsername(), (Object)realm.getName(), (Object)userSession.getId());
        }
        return allClientsLoggedOut;
    }
    
    private static boolean backchannelLogoutClientSession(final KeycloakSession session, final RealmModel realm, final AuthenticatedClientSessionModel clientSession, final AuthenticationSessionModel logoutAuthSession, final UriInfo uriInfo, final HttpHeaders headers) {
        final UserSessionModel userSession = clientSession.getUserSession();
        final ClientModel client = clientSession.getClient();
        if (client.isFrontchannelLogout() || CommonClientSessionModel.Action.LOGGED_OUT.name().equals(clientSession.getAction())) {
            return false;
        }
        final CommonClientSessionModel.Action logoutState = getClientLogoutAction(logoutAuthSession, client.getId());
        if (logoutState == CommonClientSessionModel.Action.LOGGED_OUT || logoutState == CommonClientSessionModel.Action.LOGGING_OUT) {
            return true;
        }
        try {
            setClientLogoutAction(logoutAuthSession, client.getId(), CommonClientSessionModel.Action.LOGGING_OUT);
            final String authMethod = clientSession.getProtocol();
            if (authMethod == null) {
                return true;
            }
            AuthenticationManager.logger.debugv("backchannel logout to: {0}", (Object)client.getClientId());
            final LoginProtocol protocol = (LoginProtocol)session.getProvider((Class)LoginProtocol.class, authMethod);
            protocol.setRealm(realm).setHttpHeaders(headers).setUriInfo(uriInfo);
            protocol.backchannelLogout(userSession, clientSession);
            setClientLogoutAction(logoutAuthSession, client.getId(), CommonClientSessionModel.Action.LOGGED_OUT);
            return true;
        }
        catch (Exception ex) {
            ServicesLogger.LOGGER.failedToLogoutClient(ex);
            return false;
        }
    }
    
    private static Response frontchannelLogoutClientSession(final KeycloakSession session, final RealmModel realm, final AuthenticatedClientSessionModel clientSession, final AuthenticationSessionModel logoutAuthSession, final UriInfo uriInfo, final HttpHeaders headers) {
        final UserSessionModel userSession = clientSession.getUserSession();
        final ClientModel client = clientSession.getClient();
        if (!client.isFrontchannelLogout() || CommonClientSessionModel.Action.LOGGED_OUT.name().equals(clientSession.getAction())) {
            return null;
        }
        final CommonClientSessionModel.Action logoutState = getClientLogoutAction(logoutAuthSession, client.getId());
        if (logoutState == CommonClientSessionModel.Action.LOGGED_OUT || logoutState == CommonClientSessionModel.Action.LOGGING_OUT) {
            return null;
        }
        try {
            setClientLogoutAction(logoutAuthSession, client.getId(), CommonClientSessionModel.Action.LOGGING_OUT);
            final String authMethod = clientSession.getProtocol();
            if (authMethod == null) {
                return null;
            }
            AuthenticationManager.logger.debugv("frontchannel logout to: {0}", (Object)client.getClientId());
            final LoginProtocol protocol = (LoginProtocol)session.getProvider((Class)LoginProtocol.class, authMethod);
            protocol.setRealm(realm).setHttpHeaders(headers).setUriInfo(uriInfo);
            final Response response = protocol.frontchannelLogout(userSession, clientSession);
            if (response != null) {
                AuthenticationManager.logger.debug((Object)"returning frontchannel logout request to client");
                setClientLogoutAction(logoutAuthSession, client.getId(), CommonClientSessionModel.Action.LOGGED_OUT);
                return response;
            }
        }
        catch (Exception e) {
            ServicesLogger.LOGGER.failedToLogoutClient(e);
        }
        return null;
    }
    
    public static void setClientLogoutAction(final AuthenticationSessionModel logoutAuthSession, final String clientUuid, final CommonClientSessionModel.Action action) {
        if (logoutAuthSession != null && clientUuid != null) {
            logoutAuthSession.setAuthNote("logout.state." + clientUuid, action.name());
        }
    }
    
    public static CommonClientSessionModel.Action getClientLogoutAction(final AuthenticationSessionModel logoutAuthSession, final String clientUuid) {
        if (logoutAuthSession == null || clientUuid == null) {
            return null;
        }
        final String state = logoutAuthSession.getAuthNote("logout.state." + clientUuid);
        return (state == null) ? null : CommonClientSessionModel.Action.valueOf(state);
    }
    
    public static void backchannelLogoutUserFromClient(final KeycloakSession session, final RealmModel realm, final UserModel user, final ClientModel client, final UriInfo uriInfo, final HttpHeaders headers) {
        final List<UserSessionModel> userSessions = (List<UserSessionModel>)session.sessions().getUserSessions(realm, user);
        for (final UserSessionModel userSession : userSessions) {
            final AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessionByClient(client.getId());
            if (clientSession != null) {
                backchannelLogoutClientSession(session, realm, clientSession, null, uriInfo, headers);
                clientSession.setAction(CommonClientSessionModel.Action.LOGGED_OUT.name());
                TokenManager.dettachClientSession(session.sessions(), realm, clientSession);
            }
        }
    }
    
    public static Response browserLogout(final KeycloakSession session, final RealmModel realm, final UserSessionModel userSession, final UriInfo uriInfo, final ClientConnection connection, final HttpHeaders headers) {
        if (userSession == null) {
            return null;
        }
        if (AuthenticationManager.logger.isDebugEnabled()) {
            final UserModel user = userSession.getUser();
            AuthenticationManager.logger.debugv("Logging out: {0} ({1})", (Object)user.getUsername(), (Object)userSession.getId());
        }
        if (userSession.getState() != UserSessionModel.State.LOGGING_OUT) {
            userSession.setState(UserSessionModel.State.LOGGING_OUT);
        }
        final AuthenticationSessionManager asm = new AuthenticationSessionManager(session);
        final AuthenticationSessionModel logoutAuthSession = createOrJoinLogoutSession(session, realm, asm, userSession, true);
        Response response = browserLogoutAllClients(userSession, session, realm, headers, uriInfo, logoutAuthSession);
        if (response != null) {
            return response;
        }
        final String brokerId = userSession.getNote("identity_provider");
        if (brokerId != null) {
            final IdentityProvider identityProvider = IdentityBrokerService.getIdentityProvider(session, realm, brokerId);
            response = identityProvider.keycloakInitiatedBrowserLogout(session, userSession, uriInfo, realm);
            if (response != null) {
                return response;
            }
        }
        return finishBrowserLogout(session, realm, userSession, uriInfo, connection, headers);
    }
    
    private static Response browserLogoutAllClients(final UserSessionModel userSession, final KeycloakSession session, final RealmModel realm, final HttpHeaders headers, final UriInfo uriInfo, final AuthenticationSessionModel logoutAuthSession) {
        final Map<Boolean, List<AuthenticatedClientSessionModel>> acss = userSession.getAuthenticatedClientSessions().values().stream().filter(clientSession -> !Objects.equals(CommonClientSessionModel.Action.LOGGED_OUT.name(), clientSession.getAction())).filter(clientSession -> clientSession.getProtocol() != null).collect((Collector<? super Object, ?, Map<Boolean, List<AuthenticatedClientSessionModel>>>)Collectors.partitioningBy(clientSession -> clientSession.getClient().isFrontchannelLogout()));
        final List<AuthenticatedClientSessionModel> backendLogoutSessions = (acss.get(false) == null) ? Collections.emptyList() : acss.get(false);
        backendLogoutSessions.forEach(acs -> backchannelLogoutClientSession(session, realm, acs, logoutAuthSession, uriInfo, headers));
        final List<AuthenticatedClientSessionModel> redirectClients = (acss.get(true) == null) ? Collections.emptyList() : acss.get(true);
        for (final AuthenticatedClientSessionModel nextRedirectClient : redirectClients) {
            final Response response = frontchannelLogoutClientSession(session, realm, nextRedirectClient, logoutAuthSession, uriInfo, headers);
            if (response != null) {
                return response;
            }
        }
        return null;
    }
    
    public static Response finishBrowserLogout(final KeycloakSession session, final RealmModel realm, final UserSessionModel userSession, final UriInfo uriInfo, final ClientConnection connection, final HttpHeaders headers) {
        final AuthenticationSessionManager asm = new AuthenticationSessionManager(session);
        final AuthenticationSessionModel logoutAuthSession = createOrJoinLogoutSession(session, realm, asm, userSession, true);
        checkUserSessionOnlyHasLoggedOutClients(realm, userSession, logoutAuthSession);
        expireIdentityCookie(realm, uriInfo, connection);
        expireRememberMeCookie(realm, uriInfo, connection);
        userSession.setState(UserSessionModel.State.LOGGED_OUT);
        final String method = userSession.getNote("KEYCLOAK_LOGOUT_PROTOCOL");
        final EventBuilder event = new EventBuilder(realm, session, connection);
        final LoginProtocol protocol = (LoginProtocol)session.getProvider((Class)LoginProtocol.class, method);
        protocol.setRealm(realm).setHttpHeaders(headers).setUriInfo(uriInfo).setEventBuilder(event);
        final Response response = protocol.finishLogout(userSession);
        session.sessions().removeUserSession(realm, userSession);
        session.authenticationSessions().removeRootAuthenticationSession(realm, logoutAuthSession.getParentSession());
        return response;
    }
    
    public static AccessToken createIdentityToken(final KeycloakSession keycloakSession, final RealmModel realm, final UserModel user, final UserSessionModel session, final String issuer) {
        final AccessToken token = new AccessToken();
        token.id(KeycloakModelUtils.generateId());
        token.issuedNow();
        token.subject(user.getId());
        token.issuer(issuer);
        if (session != null) {
            token.setSessionState(session.getId());
        }
        if (realm.getSsoSessionMaxLifespan() > 0) {
            token.expiration(Time.currentTime() + realm.getSsoSessionMaxLifespan());
        }
        String stateChecker = (String)keycloakSession.getAttribute("state_checker");
        if (stateChecker == null) {
            stateChecker = Base64Url.encode(KeycloakModelUtils.generateSecret());
            keycloakSession.setAttribute("state_checker", (Object)stateChecker);
        }
        token.getOtherClaims().put("state_checker", stateChecker);
        return token;
    }
    
    public static void createLoginCookie(final KeycloakSession keycloakSession, final RealmModel realm, final UserModel user, final UserSessionModel session, final UriInfo uriInfo, final ClientConnection connection) {
        final String cookiePath = getIdentityCookiePath(realm, uriInfo);
        final String issuer = Urls.realmIssuer(uriInfo.getBaseUri(), realm.getName());
        final AccessToken identityToken = createIdentityToken(keycloakSession, realm, user, session, issuer);
        final String encoded = encodeToken(keycloakSession, realm, identityToken);
        final boolean secureOnly = realm.getSslRequired().isRequired(connection);
        int maxAge = -1;
        if (session != null && session.isRememberMe()) {
            maxAge = realm.getSsoSessionMaxLifespan();
        }
        AuthenticationManager.logger.debugv("Create login cookie - name: {0}, path: {1}, max-age: {2}", (Object)"KEYCLOAK_IDENTITY", (Object)cookiePath, (Object)maxAge);
        CookieHelper.addCookie("KEYCLOAK_IDENTITY", encoded, cookiePath, null, null, maxAge, secureOnly, true);
        String sessionCookieValue = realm.getName() + "/" + user.getId();
        if (session != null) {
            sessionCookieValue = sessionCookieValue + "/" + session.getId();
        }
        CookieHelper.addCookie("KEYCLOAK_SESSION", sessionCookieValue, cookiePath, null, null, realm.getSsoSessionMaxLifespan(), secureOnly, false);
        P3PHelper.addP3PHeader(keycloakSession);
    }
    
    public static void createRememberMeCookie(final RealmModel realm, final String username, final UriInfo uriInfo, final ClientConnection connection) {
        final String path = getIdentityCookiePath(realm, uriInfo);
        final boolean secureOnly = realm.getSslRequired().isRequired(connection);
        CookieHelper.addCookie("KEYCLOAK_REMEMBER_ME", "username:" + username, path, null, null, 31536000, secureOnly, true);
    }
    
    public static String getRememberMeUsername(final RealmModel realm, final HttpHeaders headers) {
        if (realm.isRememberMe()) {
            final Cookie cookie = headers.getCookies().get("KEYCLOAK_REMEMBER_ME");
            if (cookie != null) {
                final String value = cookie.getValue();
                final String[] s = value.split(":");
                if (s[0].equals("username") && s.length == 2) {
                    return s[1];
                }
            }
        }
        return null;
    }
    
    protected static String encodeToken(final KeycloakSession session, final RealmModel realm, final Object token) {
        final KeyManager.ActiveHmacKey activeKey = session.keys().getActiveHmacKey(realm);
        AuthenticationManager.logger.tracef("Encoding token with kid '%s'", (Object)activeKey.getKid());
        final String encodedToken = new JWSBuilder().kid(activeKey.getKid()).jsonContent(token).hmac256(activeKey.getSecretKey());
        return encodedToken;
    }
    
    public static void expireIdentityCookie(final RealmModel realm, final UriInfo uriInfo, final ClientConnection connection) {
        AuthenticationManager.logger.debug((Object)"Expiring identity cookie");
        final String path = getIdentityCookiePath(realm, uriInfo);
        expireCookie(realm, "KEYCLOAK_IDENTITY", path, true, connection);
        expireCookie(realm, "KEYCLOAK_SESSION", path, false, connection);
        final String oldPath = getOldCookiePath(realm, uriInfo);
        expireCookie(realm, "KEYCLOAK_IDENTITY", oldPath, true, connection);
        expireCookie(realm, "KEYCLOAK_SESSION", oldPath, false, connection);
    }
    
    public static void expireOldIdentityCookie(final RealmModel realm, final UriInfo uriInfo, final ClientConnection connection) {
        AuthenticationManager.logger.debug((Object)"Expiring old identity cookie with wrong path");
        final String oldPath = getOldCookiePath(realm, uriInfo);
        expireCookie(realm, "KEYCLOAK_IDENTITY", oldPath, true, connection);
        expireCookie(realm, "KEYCLOAK_SESSION", oldPath, false, connection);
    }
    
    public static void expireRememberMeCookie(final RealmModel realm, final UriInfo uriInfo, final ClientConnection connection) {
        AuthenticationManager.logger.debug((Object)"Expiring remember me cookie");
        final String path = getIdentityCookiePath(realm, uriInfo);
        final String cookieName = "KEYCLOAK_REMEMBER_ME";
        expireCookie(realm, cookieName, path, true, connection);
    }
    
    public static void expireOldAuthSessionCookie(final RealmModel realm, final UriInfo uriInfo, final ClientConnection connection) {
        AuthenticationManager.logger.debugv("Expire {1} cookie .", (Object)"AUTH_SESSION_ID");
        final String oldPath = getOldCookiePath(realm, uriInfo);
        expireCookie(realm, "AUTH_SESSION_ID", oldPath, true, connection);
    }
    
    protected static String getIdentityCookiePath(final RealmModel realm, final UriInfo uriInfo) {
        return getRealmCookiePath(realm, uriInfo);
    }
    
    public static String getRealmCookiePath(final RealmModel realm, final UriInfo uriInfo) {
        final URI uri = RealmsResource.realmBaseUrl(uriInfo).build(new Object[] { realm.getName() });
        return uri.getRawPath() + "/";
    }
    
    public static String getOldCookiePath(final RealmModel realm, final UriInfo uriInfo) {
        final URI uri = RealmsResource.realmBaseUrl(uriInfo).build(new Object[] { realm.getName() });
        return uri.getRawPath();
    }
    
    public static String getAccountCookiePath(final RealmModel realm, final UriInfo uriInfo) {
        final URI uri = RealmsResource.accountUrl(uriInfo.getBaseUriBuilder()).build(new Object[] { realm.getName() });
        return uri.getRawPath();
    }
    
    public static void expireCookie(final RealmModel realm, final String cookieName, final String path, final boolean httpOnly, final ClientConnection connection) {
        AuthenticationManager.logger.debugv("Expiring cookie: {0} path: {1}", (Object)cookieName, (Object)path);
        final boolean secureOnly = realm.getSslRequired().isRequired(connection);
        CookieHelper.addCookie(cookieName, "", path, null, "Expiring cookie", 0, secureOnly, httpOnly);
    }
    
    public AuthResult authenticateIdentityCookie(final KeycloakSession session, final RealmModel realm) {
        return authenticateIdentityCookie(session, realm, true);
    }
    
    public static AuthResult authenticateIdentityCookie(final KeycloakSession session, final RealmModel realm, final boolean checkActive) {
        final Cookie cookie = session.getContext().getRequestHeaders().getCookies().get("KEYCLOAK_IDENTITY");
        if (cookie == null || "".equals(cookie.getValue())) {
            AuthenticationManager.logger.debugv("Could not find cookie: {0}", (Object)"KEYCLOAK_IDENTITY");
            return null;
        }
        final String tokenString = cookie.getValue();
        final AuthResult authResult = verifyIdentityToken(session, realm, (UriInfo)session.getContext().getUri(), session.getContext().getConnection(), checkActive, false, true, tokenString, session.getContext().getRequestHeaders());
        if (authResult == null) {
            expireIdentityCookie(realm, (UriInfo)session.getContext().getUri(), session.getContext().getConnection());
            expireOldIdentityCookie(realm, (UriInfo)session.getContext().getUri(), session.getContext().getConnection());
            return null;
        }
        authResult.getSession().setLastSessionRefresh(Time.currentTime());
        return authResult;
    }
    
    public static Response redirectAfterSuccessfulFlow(final KeycloakSession session, final RealmModel realm, final UserSessionModel userSession, final ClientSessionContext clientSessionCtx, final HttpRequest request, final UriInfo uriInfo, final ClientConnection clientConnection, final EventBuilder event, final String protocol) {
        final LoginProtocol protocolImpl = (LoginProtocol)session.getProvider((Class)LoginProtocol.class, protocol);
        protocolImpl.setRealm(realm).setHttpHeaders(request.getHttpHeaders()).setUriInfo(uriInfo).setEventBuilder(event);
        return redirectAfterSuccessfulFlow(session, realm, userSession, clientSessionCtx, request, uriInfo, clientConnection, event, protocolImpl);
    }
    
    public static Response redirectAfterSuccessfulFlow(final KeycloakSession session, final RealmModel realm, final UserSessionModel userSession, final ClientSessionContext clientSessionCtx, final HttpRequest request, final UriInfo uriInfo, final ClientConnection clientConnection, final EventBuilder event, final LoginProtocol protocol) {
        final Cookie sessionCookie = request.getHttpHeaders().getCookies().get("KEYCLOAK_SESSION");
        if (sessionCookie != null) {
            final String[] split = sessionCookie.getValue().split("/");
            if (split.length >= 3) {
                final String oldSessionId = split[2];
                if (!oldSessionId.equals(userSession.getId())) {
                    final UserSessionModel oldSession = session.sessions().getUserSession(realm, oldSessionId);
                    if (oldSession != null) {
                        AuthenticationManager.logger.debugv("Removing old user session: session: {0}", (Object)oldSessionId);
                        session.sessions().removeUserSession(realm, oldSession);
                    }
                }
            }
        }
        session.getContext().resolveLocale(userSession.getUser());
        createLoginCookie(session, realm, userSession.getUser(), userSession, uriInfo, clientConnection);
        if (userSession.getState() != UserSessionModel.State.LOGGED_IN) {
            userSession.setState(UserSessionModel.State.LOGGED_IN);
        }
        if (userSession.isRememberMe()) {
            createRememberMeCookie(realm, userSession.getLoginUsername(), uriInfo, clientConnection);
        }
        else {
            expireRememberMeCookie(realm, uriInfo, clientConnection);
        }
        final AuthenticatedClientSessionModel clientSession = clientSessionCtx.getClientSession();
        final boolean isSSOAuthentication = "true".equals(session.getAttribute("SSO_AUTH"));
        if (isSSOAuthentication) {
            clientSession.setNote("SSO_AUTH", "true");
        }
        else {
            final int authTime = Time.currentTime();
            userSession.setNote("AUTH_TIME", String.valueOf(authTime));
            clientSession.removeNote("SSO_AUTH");
        }
        return protocol.authenticated(userSession, clientSessionCtx);
    }
    
    public static boolean isSSOAuthentication(final AuthenticatedClientSessionModel clientSession) {
        final String ssoAuth = clientSession.getNote("SSO_AUTH");
        return Boolean.parseBoolean(ssoAuth);
    }
    
    public static Response nextActionAfterAuthentication(final KeycloakSession session, final AuthenticationSessionModel authSession, final ClientConnection clientConnection, final HttpRequest request, final UriInfo uriInfo, final EventBuilder event) {
        final Response requiredAction = actionRequired(session, authSession, clientConnection, request, uriInfo, event);
        if (requiredAction != null) {
            return requiredAction;
        }
        return finishedRequiredActions(session, authSession, null, clientConnection, request, uriInfo, event);
    }
    
    public static Response redirectToRequiredActions(final KeycloakSession session, final RealmModel realm, final AuthenticationSessionModel authSession, final UriInfo uriInfo, final String requiredAction) {
        final ClientSessionCode<AuthenticationSessionModel> accessCode = new ClientSessionCode<AuthenticationSessionModel>(session, realm, authSession);
        accessCode.setAction(CommonClientSessionModel.Action.REQUIRED_ACTIONS.name());
        authSession.setAuthNote("current.flow.path", "required-action");
        authSession.setAuthNote("current.authentication.execution", requiredAction);
        final UriBuilder uriBuilder = LoginActionsService.loginActionsBaseUrl(uriInfo).path("required-action");
        if (requiredAction != null) {
            uriBuilder.queryParam("execution", new Object[] { requiredAction });
        }
        uriBuilder.queryParam("client_id", new Object[] { authSession.getClient().getClientId() });
        uriBuilder.queryParam("tab_id", new Object[] { authSession.getTabId() });
        if (uriInfo.getQueryParameters().containsKey((Object)"auth_session_id")) {
            uriBuilder.queryParam("auth_session_id", new Object[] { authSession.getParentSession().getId() });
        }
        final URI redirect = uriBuilder.build(new Object[] { realm.getName() });
        return Response.status(302).location(redirect).build();
    }
    
    public static Response finishedRequiredActions(final KeycloakSession session, final AuthenticationSessionModel authSession, UserSessionModel userSession, final ClientConnection clientConnection, final HttpRequest request, final UriInfo uriInfo, final EventBuilder event) {
        final String actionTokenKeyToInvalidate = authSession.getAuthNote("INVALIDATE_ACTION_TOKEN");
        if (actionTokenKeyToInvalidate != null) {
            final ActionTokenKeyModel actionTokenKey = (ActionTokenKeyModel)DefaultActionTokenKey.from(actionTokenKeyToInvalidate);
            if (actionTokenKey != null) {
                final ActionTokenStoreProvider actionTokenStore = (ActionTokenStoreProvider)session.getProvider((Class)ActionTokenStoreProvider.class);
                actionTokenStore.put(actionTokenKey, (Map)null);
            }
        }
        if (authSession.getAuthNote("END_AFTER_REQUIRED_ACTIONS") != null) {
            final LoginFormsProvider infoPage = ((LoginFormsProvider)session.getProvider((Class)LoginFormsProvider.class)).setAuthenticationSession(authSession).setSuccess("accountUpdatedMessage", new Object[0]);
            if (authSession.getAuthNote("SET_REDIRECT_URI_AFTER_REQUIRED_ACTIONS") != null) {
                if (authSession.getRedirectUri() != null) {
                    infoPage.setAttribute("pageRedirectUri", (Object)authSession.getRedirectUri());
                }
            }
            else {
                infoPage.setAttribute("skipLink", (Object)true);
            }
            final Response response = infoPage.createInfoPage();
            new AuthenticationSessionManager(session).removeAuthenticationSession(authSession.getRealm(), authSession, true);
            return response;
        }
        final RealmModel realm = authSession.getRealm();
        final ClientSessionContext clientSessionCtx = AuthenticationProcessor.attachSession(authSession, userSession, session, realm, clientConnection, event);
        userSession = clientSessionCtx.getClientSession().getUserSession();
        event.event(EventType.LOGIN);
        event.session(userSession);
        event.success();
        return redirectAfterSuccessfulFlow(session, realm, userSession, clientSessionCtx, request, uriInfo, clientConnection, event, authSession.getProtocol());
    }
    
    public static String nextRequiredAction(final KeycloakSession session, final AuthenticationSessionModel authSession, final ClientConnection clientConnection, final HttpRequest request, final UriInfo uriInfo, final EventBuilder event) {
        final RealmModel realm = authSession.getRealm();
        final UserModel user = authSession.getAuthenticatedUser();
        final ClientModel client = authSession.getClient();
        evaluateRequiredActionTriggers(session, authSession, clientConnection, request, uriInfo, event, realm, user);
        if (!user.getRequiredActions().isEmpty()) {
            return user.getRequiredActions().iterator().next();
        }
        if (!authSession.getRequiredActions().isEmpty()) {
            return authSession.getRequiredActions().iterator().next();
        }
        if (client.isConsentRequired()) {
            final UserConsentModel grantedConsent = session.users().getConsentByClient(realm, user.getId(), client.getId());
            final List<ClientScopeModel> clientScopesToApprove = getClientScopesToApproveOnConsentScreen(realm, grantedConsent, authSession);
            if (!clientScopesToApprove.isEmpty()) {
                return CommonClientSessionModel.Action.OAUTH_GRANT.name();
            }
            final String consentDetail = (grantedConsent != null) ? "persistent_consent" : "no_consent_required";
            event.detail("consent", consentDetail);
        }
        else {
            event.detail("consent", "no_consent_required");
        }
        return null;
    }
    
    public static Response actionRequired(final KeycloakSession session, final AuthenticationSessionModel authSession, final ClientConnection clientConnection, final HttpRequest request, final UriInfo uriInfo, final EventBuilder event) {
        final RealmModel realm = authSession.getRealm();
        final UserModel user = authSession.getAuthenticatedUser();
        final ClientModel client = authSession.getClient();
        evaluateRequiredActionTriggers(session, authSession, clientConnection, request, uriInfo, event, realm, user);
        AuthenticationManager.logger.debugv("processAccessCode: go to oauth page?: {0}", (Object)client.isConsentRequired());
        event.detail("code_id", authSession.getParentSession().getId());
        Set<String> requiredActions = (Set<String>)user.getRequiredActions();
        Response action = executionActions(session, authSession, request, event, realm, user, requiredActions);
        if (action != null) {
            return action;
        }
        requiredActions = (Set<String>)authSession.getRequiredActions();
        action = executionActions(session, authSession, request, event, realm, user, requiredActions);
        if (action != null) {
            return action;
        }
        if (client.isConsentRequired()) {
            final UserConsentModel grantedConsent = session.users().getConsentByClient(realm, user.getId(), client.getId());
            final List<ClientScopeModel> clientScopesToApprove = getClientScopesToApproveOnConsentScreen(realm, grantedConsent, authSession);
            if (clientScopesToApprove.size() > 0) {
                final String execution = CommonClientSessionModel.Action.OAUTH_GRANT.name();
                final ClientSessionCode<AuthenticationSessionModel> accessCode = new ClientSessionCode<AuthenticationSessionModel>(session, realm, authSession);
                accessCode.setAction(CommonClientSessionModel.Action.REQUIRED_ACTIONS.name());
                authSession.setAuthNote("current.authentication.execution", execution);
                return ((LoginFormsProvider)session.getProvider((Class)LoginFormsProvider.class)).setAuthenticationSession(authSession).setExecution(execution).setClientSessionCode(accessCode.getOrGenerateCode()).setAccessRequest((List)clientScopesToApprove).createOAuthGrant();
            }
            final String consentDetail = (grantedConsent != null) ? "persistent_consent" : "no_consent_required";
            event.detail("consent", consentDetail);
        }
        else {
            event.detail("consent", "no_consent_required");
        }
        return null;
    }
    
    private static List<ClientScopeModel> getClientScopesToApproveOnConsentScreen(final RealmModel realm, final UserConsentModel grantedConsent, final AuthenticationSessionModel authSession) {
        final List<ClientScopeModel> clientScopesToDisplay = new LinkedList<ClientScopeModel>();
        for (final String clientScopeId : authSession.getClientScopes()) {
            final ClientScopeModel clientScope = KeycloakModelUtils.findClientScopeById(realm, clientScopeId);
            if (clientScope != null) {
                if (!clientScope.isDisplayOnConsentScreen()) {
                    continue;
                }
                if (grantedConsent != null && grantedConsent.isClientScopeGranted(clientScope)) {
                    continue;
                }
                clientScopesToDisplay.add(clientScope);
            }
        }
        return clientScopesToDisplay;
    }
    
    public static void setClientScopesInSession(final AuthenticationSessionModel authSession) {
        final ClientModel client = authSession.getClient();
        final UserModel user = authSession.getAuthenticatedUser();
        final String scopeParam = authSession.getClientNote("scope");
        final Set<String> requestedClientScopes = new HashSet<String>();
        for (final ClientScopeModel clientScope : TokenManager.getRequestedClientScopes(scopeParam, client)) {
            requestedClientScopes.add(clientScope.getId());
        }
        authSession.setClientScopes((Set)requestedClientScopes);
    }
    
    public static RequiredActionProvider createRequiredAction(final RequiredActionContextResult context) {
        final String display = context.getAuthenticationSession().getAuthNote("display");
        if (display == null) {
            return (RequiredActionProvider)context.getFactory().create(context.getSession());
        }
        if (context.getFactory() instanceof DisplayTypeRequiredActionFactory) {
            final RequiredActionProvider provider = ((DisplayTypeRequiredActionFactory)context.getFactory()).createDisplay(context.getSession(), display);
            if (provider != null) {
                return provider;
            }
        }
        if ("console".equalsIgnoreCase(display)) {
            context.getAuthenticationSession().removeAuthNote("display");
            throw new AuthenticationFlowException(AuthenticationFlowError.DISPLAY_NOT_SUPPORTED, ConsoleDisplayMode.browserContinue(context.getSession(), context.getUriInfo().getRequestUri().toString()));
        }
        return (RequiredActionProvider)context.getFactory().create(context.getSession());
    }
    
    protected static Response executionActions(final KeycloakSession session, final AuthenticationSessionModel authSession, final HttpRequest request, final EventBuilder event, final RealmModel realm, final UserModel user, final Set<String> requiredActions) {
        final List<RequiredActionProviderModel> sortedRequiredActions = sortRequiredActionsByPriority(realm, requiredActions);
        for (final RequiredActionProviderModel model : sortedRequiredActions) {
            final RequiredActionFactory factory = (RequiredActionFactory)session.getKeycloakSessionFactory().getProviderFactory((Class)RequiredActionProvider.class, model.getProviderId());
            if (factory == null) {
                throw new RuntimeException("Unable to find factory for Required Action: " + model.getProviderId() + " did you forget to declare it in a META-INF/services file?");
            }
            final RequiredActionContextResult context = new RequiredActionContextResult(authSession, realm, event, session, request, user, factory);
            RequiredActionProvider actionProvider = null;
            try {
                actionProvider = createRequiredAction(context);
            }
            catch (AuthenticationFlowException e) {
                if (e.getResponse() != null) {
                    return e.getResponse();
                }
                throw e;
            }
            actionProvider.requiredActionChallenge((RequiredActionContext)context);
            if (context.getStatus() == RequiredActionContext.Status.FAILURE) {
                final LoginProtocol protocol = (LoginProtocol)context.getSession().getProvider((Class)LoginProtocol.class, context.getAuthenticationSession().getProtocol());
                protocol.setRealm(context.getRealm()).setHttpHeaders(context.getHttpRequest().getHttpHeaders()).setUriInfo(context.getUriInfo()).setEventBuilder(event);
                final Response response = protocol.sendError(context.getAuthenticationSession(), LoginProtocol.Error.CONSENT_DENIED);
                event.error("rejected_by_user");
                return response;
            }
            if (context.getStatus() == RequiredActionContext.Status.CHALLENGE) {
                authSession.setAuthNote("current.authentication.execution", model.getProviderId());
                return context.getChallenge();
            }
            if (context.getStatus() != RequiredActionContext.Status.SUCCESS) {
                continue;
            }
            event.clone().event(EventType.CUSTOM_REQUIRED_ACTION).detail("custom_required_action", factory.getId()).success();
            authSession.getAuthenticatedUser().removeRequiredAction(factory.getId());
            authSession.removeRequiredAction(factory.getId());
        }
        return null;
    }
    
    private static List<RequiredActionProviderModel> sortRequiredActionsByPriority(final RealmModel realm, final Set<String> requiredActions) {
        final List<RequiredActionProviderModel> actions = new ArrayList<RequiredActionProviderModel>();
        for (final String action : requiredActions) {
            final RequiredActionProviderModel model = realm.getRequiredActionProviderByAlias(action);
            if (model == null) {
                AuthenticationManager.logger.warnv("Could not find configuration for Required Action {0}, did you forget to register it?", (Object)action);
            }
            else {
                if (!model.isEnabled()) {
                    continue;
                }
                actions.add(model);
            }
        }
        Collections.sort(actions, (Comparator<? super RequiredActionProviderModel>)RequiredActionProviderModel.RequiredActionComparator.SINGLETON);
        return actions;
    }
    
    public static void evaluateRequiredActionTriggers(final KeycloakSession session, final AuthenticationSessionModel authSession, final ClientConnection clientConnection, final HttpRequest request, final UriInfo uriInfo, final EventBuilder event, final RealmModel realm, final UserModel user) {
        for (final RequiredActionProviderModel model : realm.getRequiredActionProviders()) {
            if (!model.isEnabled()) {
                continue;
            }
            final RequiredActionFactory factory = (RequiredActionFactory)session.getKeycloakSessionFactory().getProviderFactory((Class)RequiredActionProvider.class, model.getProviderId());
            if (factory == null) {
                throw new RuntimeException("Unable to find factory for Required Action: " + model.getProviderId() + " did you forget to declare it in a META-INF/services file?");
            }
            final RequiredActionProvider provider = (RequiredActionProvider)factory.create(session);
            final RequiredActionContextResult result = new RequiredActionContextResult(authSession, realm, event, session, request, user, factory) {
                @Override
                public void challenge(final Response response) {
                    throw new RuntimeException("Not allowed to call challenge() within evaluateTriggers()");
                }
                
                @Override
                public void failure() {
                    throw new RuntimeException("Not allowed to call failure() within evaluateTriggers()");
                }
                
                @Override
                public void success() {
                    throw new RuntimeException("Not allowed to call success() within evaluateTriggers()");
                }
                
                @Override
                public void ignore() {
                    throw new RuntimeException("Not allowed to call ignore() within evaluateTriggers()");
                }
            };
            provider.evaluateTriggers((RequiredActionContext)result);
        }
    }
    
    public static AuthResult verifyIdentityToken(final KeycloakSession session, final RealmModel realm, final UriInfo uriInfo, final ClientConnection connection, final boolean checkActive, final boolean checkTokenType, final boolean isCookie, final String tokenString, final HttpHeaders headers) {
        try {
            final TokenVerifier<AccessToken> verifier = (TokenVerifier<AccessToken>)TokenVerifier.create(tokenString, (Class)AccessToken.class).withDefaultChecks().realmUrl(Urls.realmIssuer(uriInfo.getBaseUri(), realm.getName())).checkActive(checkActive).checkTokenType(checkTokenType);
            final String kid = verifier.getHeader().getKeyId();
            final AlgorithmType algorithmType = verifier.getHeader().getAlgorithm().getType();
            if (AlgorithmType.RSA.equals((Object)algorithmType)) {
                final PublicKey publicKey = session.keys().getRsaPublicKey(realm, kid);
                if (publicKey == null) {
                    AuthenticationManager.logger.debugf("Identity cookie signed with unknown kid '%s'", (Object)kid);
                    return null;
                }
                verifier.publicKey(publicKey);
            }
            else if (AlgorithmType.HMAC.equals((Object)algorithmType)) {
                final SecretKey secretKey = session.keys().getHmacSecretKey(realm, kid);
                if (secretKey == null) {
                    AuthenticationManager.logger.debugf("Identity cookie signed with unknown kid '%s'", (Object)kid);
                    return null;
                }
                verifier.secretKey(secretKey);
            }
            final AccessToken token = (AccessToken)verifier.verify().getToken();
            if (checkActive && (!token.isActive() || token.getIssuedAt() < realm.getNotBefore())) {
                AuthenticationManager.logger.debug((Object)"Identity cookie expired");
                return null;
            }
            final UserSessionModel userSession = session.sessions().getUserSession(realm, token.getSessionState());
            UserModel user = null;
            if (userSession != null) {
                user = userSession.getUser();
                if (user == null || !user.isEnabled()) {
                    AuthenticationManager.logger.debug((Object)"Unknown user in identity token");
                    return null;
                }
                final int userNotBefore = session.users().getNotBeforeOfUser(realm, user);
                if (token.getIssuedAt() < userNotBefore) {
                    AuthenticationManager.logger.debug((Object)"User notBefore newer than token");
                    return null;
                }
            }
            if (!isSessionValid(realm, userSession)) {
                if (!isCookie) {
                    final UserSessionModel offlineUserSession = session.sessions().getOfflineUserSession(realm, token.getSessionState());
                    if (isOfflineSessionValid(realm, offlineUserSession)) {
                        user = offlineUserSession.getUser();
                        return new AuthResult(user, offlineUserSession, token);
                    }
                }
                if (userSession != null) {
                    backchannelLogout(session, realm, userSession, uriInfo, connection, headers, true);
                }
                AuthenticationManager.logger.debug((Object)"User session not active");
                return null;
            }
            session.setAttribute("state_checker", token.getOtherClaims().get("state_checker"));
            return new AuthResult(user, userSession, token);
        }
        catch (VerificationException e) {
            AuthenticationManager.logger.debugf("Failed to verify identity token: %s", (Object)e.getMessage());
            return null;
        }
    }
    
    static {
        logger = Logger.getLogger((Class)AuthenticationManager.class);
    }
    
    public enum AuthenticationStatus
    {
        SUCCESS, 
        ACCOUNT_TEMPORARILY_DISABLED, 
        ACCOUNT_DISABLED, 
        ACTIONS_REQUIRED, 
        INVALID_USER, 
        INVALID_CREDENTIALS, 
        MISSING_PASSWORD, 
        MISSING_TOTP, 
        FAILED;
    }
    
    public static class AuthResult
    {
        private final UserModel user;
        private final UserSessionModel session;
        private final AccessToken token;
        
        public AuthResult(final UserModel user, final UserSessionModel session, final AccessToken token) {
            this.user = user;
            this.session = session;
            this.token = token;
        }
        
        public UserSessionModel getSession() {
            return this.session;
        }
        
        public UserModel getUser() {
            return this.user;
        }
        
        public AccessToken getToken() {
            return this.token;
        }
    }
}
