// 
// Decompiled by Procyon v0.5.36
// 

package org.keycloak.services.resources.account;

import org.keycloak.models.UserSessionModel;
import javax.ws.rs.NotFoundException;
import org.keycloak.common.Profile;
import java.util.Set;
import java.util.function.Consumer;
import org.keycloak.sessions.CommonClientSessionModel;
import java.util.Collection;
import java.util.LinkedList;
import java.util.HashSet;
import java.util.stream.Stream;
import javax.ws.rs.QueryParam;
import java.util.Iterator;
import java.util.function.Function;
import org.keycloak.models.ClientScopeModel;
import javax.ws.rs.PUT;
import javax.ws.rs.DELETE;
import org.keycloak.services.managers.UserSessionManager;
import javax.ws.rs.PathParam;
import java.io.IOException;
import org.keycloak.theme.Theme;
import java.util.Properties;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.keycloak.representations.account.ConsentScopeRepresentation;
import org.keycloak.representations.account.ConsentRepresentation;
import org.keycloak.services.util.ResolveRelative;
import org.keycloak.common.util.StringPropertyReplacer;
import org.keycloak.representations.account.ClientRepresentation;
import org.keycloak.models.UserConsentModel;
import org.keycloak.services.resources.account.resources.ResourcesService;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import org.keycloak.userprofile.validation.UserProfileValidationResult;
import org.keycloak.storage.ReadOnlyException;
import org.keycloak.userprofile.utils.UserUpdateHelper;
import org.keycloak.userprofile.validation.AttributeValidationResult;
import org.keycloak.userprofile.validation.ValidationResult;
import org.keycloak.services.ErrorResponse;
import org.keycloak.userprofile.profile.UserProfileContextFactory;
import org.keycloak.events.EventType;
import javax.ws.rs.core.Response;
import org.jboss.resteasy.annotations.cache.NoCache;
import javax.ws.rs.Produces;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import org.keycloak.representations.account.UserRepresentation;
import org.keycloak.common.enums.AccountRestApiVersion;
import java.util.Locale;
import org.keycloak.models.UserModel;
import org.keycloak.models.RealmModel;
import org.keycloak.services.managers.Auth;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.common.ClientConnection;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Context;
import org.jboss.resteasy.spi.HttpRequest;

public class AccountRestService
{
    @Context
    private HttpRequest request;
    @Context
    protected HttpHeaders headers;
    @Context
    protected ClientConnection clientConnection;
    private final KeycloakSession session;
    private final ClientModel client;
    private final EventBuilder event;
    private EventStoreProvider eventStore;
    private Auth auth;
    private final RealmModel realm;
    private final UserModel user;
    private final Locale locale;
    private final AccountRestApiVersion version;
    
    public AccountRestService(final KeycloakSession session, final Auth auth, final ClientModel client, final EventBuilder event, final AccountRestApiVersion version) {
        this.session = session;
        this.auth = auth;
        this.realm = auth.getRealm();
        this.user = auth.getUser();
        this.client = client;
        this.event = event;
        this.locale = session.getContext().resolveLocale(this.user);
        this.version = version;
    }
    
    public void init() {
        this.eventStore = (EventStoreProvider)this.session.getProvider((Class)EventStoreProvider.class);
    }
    
    @Path("/")
    @GET
    @Produces({ "application/json" })
    @NoCache
    public UserRepresentation account() {
        this.auth.requireOneOf("manage-account", "view-profile");
        final UserModel user = this.auth.getUser();
        final UserRepresentation rep = new UserRepresentation();
        rep.setUsername(user.getUsername());
        rep.setFirstName(user.getFirstName());
        rep.setLastName(user.getLastName());
        rep.setEmail(user.getEmail());
        rep.setEmailVerified(user.isEmailVerified());
        rep.setEmailVerified(user.isEmailVerified());
        final Map<String, List<String>> attributes = (Map<String, List<String>>)user.getAttributes();
        final Map<String, List<String>> copiedAttributes = new HashMap<String, List<String>>(attributes);
        copiedAttributes.remove("firstName");
        copiedAttributes.remove("lastName");
        copiedAttributes.remove("email");
        copiedAttributes.remove("username");
        rep.setAttributes((Map)copiedAttributes);
        return rep;
    }
    
    @Path("/")
    @POST
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @NoCache
    public Response updateAccount(final UserRepresentation rep) {
        this.auth.require("manage-account");
        this.event.event(EventType.UPDATE_PROFILE).client(this.auth.getClient()).user(this.auth.getUser());
        final UserProfileValidationResult result = UserProfileContextFactory.forAccountService(this.user, rep, this.session).validate();
        if (result.hasFailureOfErrorType(new String[] { "readOnlyUsernameMessage" })) {
            return ErrorResponse.error("readOnlyUsernameMessage", Response.Status.BAD_REQUEST);
        }
        if (result.hasFailureOfErrorType(new String[] { "usernameExistsMessage" })) {
            return ErrorResponse.exists("usernameExistsMessage");
        }
        if (result.hasFailureOfErrorType(new String[] { "emailExistsMessage" })) {
            return ErrorResponse.exists("emailExistsMessage");
        }
        if (!result.getErrors().isEmpty()) {
            final String firstErrorMessage = result.getErrors().get(0).getFailedValidations().get(0).getErrorType();
            return ErrorResponse.error(firstErrorMessage, Response.Status.BAD_REQUEST);
        }
        try {
            UserUpdateHelper.updateAccount(this.realm, this.user, result.getProfile());
            this.event.success();
            return Response.noContent().build();
        }
        catch (ReadOnlyException e) {
            return ErrorResponse.error("readOnlyUserMessage", Response.Status.BAD_REQUEST);
        }
    }
    
    @Path("/sessions")
    public SessionResource sessions() {
        checkAccountApiEnabled();
        this.auth.requireOneOf("manage-account", "view-profile");
        return new SessionResource(this.session, this.auth, this.request);
    }
    
    @Path("/credentials")
    public AccountCredentialResource credentials() {
        checkAccountApiEnabled();
        return new AccountCredentialResource(this.session, this.user, this.auth);
    }
    
    @Path("/resources")
    public ResourcesService resources() {
        checkAccountApiEnabled();
        this.auth.requireOneOf("manage-account", "view-profile");
        return new ResourcesService(this.session, this.user, this.auth, this.request);
    }
    
    private ClientRepresentation modelToRepresentation(final ClientModel model, final List<String> inUseClients, final List<String> offlineClients, final Map<String, UserConsentModel> consents) {
        final ClientRepresentation representation = new ClientRepresentation();
        representation.setClientId(model.getClientId());
        representation.setClientName(StringPropertyReplacer.replaceProperties(model.getName(), this.getProperties()));
        representation.setDescription(model.getDescription());
        representation.setUserConsentRequired(model.isConsentRequired());
        representation.setInUse(inUseClients.contains(model.getClientId()));
        representation.setOfflineAccess(offlineClients.contains(model.getClientId()));
        representation.setRootUrl(model.getRootUrl());
        representation.setBaseUrl(model.getBaseUrl());
        representation.setEffectiveUrl(ResolveRelative.resolveRelativeUri(this.session, model.getRootUrl(), model.getBaseUrl()));
        final UserConsentModel consentModel = consents.get(model.getClientId());
        if (consentModel != null) {
            representation.setConsent(this.modelToRepresentation(consentModel));
        }
        return representation;
    }
    
    private ConsentRepresentation modelToRepresentation(final UserConsentModel model) {
        final List<ConsentScopeRepresentation> grantedScopes = (List<ConsentScopeRepresentation>)model.getGrantedClientScopes().stream().map(m -> new ConsentScopeRepresentation(m.getId(), m.getName(), StringPropertyReplacer.replaceProperties(m.getConsentScreenText(), this.getProperties()))).collect(Collectors.toList());
        return new ConsentRepresentation((List)grantedScopes, model.getCreatedDate(), model.getLastUpdatedDate());
    }
    
    private Properties getProperties() {
        try {
            return this.session.theme().getTheme(Theme.Type.ACCOUNT).getMessages(this.locale);
        }
        catch (IOException e) {
            return null;
        }
    }
    
    @Path("/applications/{clientId}/consent")
    @GET
    @Produces({ "application/json" })
    public Response getConsent(@PathParam("clientId") final String clientId) {
        checkAccountApiEnabled();
        this.auth.requireOneOf("manage-account", "view-consent", "manage-consent");
        final ClientModel client = this.realm.getClientByClientId(clientId);
        if (client == null) {
            return ErrorResponse.error("No client with clientId: " + clientId + " found.", Response.Status.NOT_FOUND);
        }
        final UserConsentModel consent = this.session.users().getConsentByClient(this.realm, this.user.getId(), client.getId());
        if (consent == null) {
            return Response.noContent().build();
        }
        return Response.ok((Object)this.modelToRepresentation(consent)).build();
    }
    
    @Path("/applications/{clientId}/consent")
    @DELETE
    public Response revokeConsent(@PathParam("clientId") final String clientId) {
        checkAccountApiEnabled();
        this.auth.requireOneOf("manage-account", "manage-consent");
        this.event.event(EventType.REVOKE_GRANT);
        final ClientModel client = this.realm.getClientByClientId(clientId);
        if (client == null) {
            this.event.event(EventType.REVOKE_GRANT_ERROR);
            final String msg = String.format("No client with clientId: %s found.", clientId);
            this.event.error(msg);
            return ErrorResponse.error(msg, Response.Status.NOT_FOUND);
        }
        this.session.users().revokeConsentForClient(this.realm, this.user.getId(), client.getId());
        new UserSessionManager(this.session).revokeOfflineToken(this.user, client);
        this.event.success();
        return Response.noContent().build();
    }
    
    @Path("/applications/{clientId}/consent")
    @POST
    @Produces({ "application/json" })
    public Response grantConsent(@PathParam("clientId") final String clientId, final ConsentRepresentation consent) {
        return this.upsert(clientId, consent);
    }
    
    @Path("/applications/{clientId}/consent")
    @PUT
    @Produces({ "application/json" })
    public Response updateConsent(@PathParam("clientId") final String clientId, final ConsentRepresentation consent) {
        return this.upsert(clientId, consent);
    }
    
    private Response upsert(final String clientId, final ConsentRepresentation consent) {
        checkAccountApiEnabled();
        this.auth.requireOneOf("manage-account", "manage-consent");
        this.event.event(EventType.GRANT_CONSENT);
        final ClientModel client = this.realm.getClientByClientId(clientId);
        if (client == null) {
            this.event.event(EventType.GRANT_CONSENT_ERROR);
            final String msg = String.format("No client with clientId: %s found.", clientId);
            this.event.error(msg);
            return ErrorResponse.error(msg, Response.Status.NOT_FOUND);
        }
        try {
            UserConsentModel grantedConsent = this.createConsent(client, consent);
            if (this.session.users().getConsentByClient(this.realm, this.user.getId(), client.getId()) == null) {
                this.session.users().addConsent(this.realm, this.user.getId(), grantedConsent);
            }
            else {
                this.session.users().updateConsent(this.realm, this.user.getId(), grantedConsent);
            }
            this.event.success();
            grantedConsent = this.session.users().getConsentByClient(this.realm, this.user.getId(), client.getId());
            return Response.ok((Object)this.modelToRepresentation(grantedConsent)).build();
        }
        catch (IllegalArgumentException e) {
            return ErrorResponse.error(e.getMessage(), Response.Status.BAD_REQUEST);
        }
    }
    
    private UserConsentModel createConsent(final ClientModel client, final ConsentRepresentation requested) throws IllegalArgumentException {
        final UserConsentModel consent = new UserConsentModel(client);
        final Map<String, ClientScopeModel> availableGrants = this.realm.getClientScopesStream().collect(Collectors.toMap((Function<? super Object, ? extends String>)ClientScopeModel::getId, (Function<? super Object, ? extends ClientScopeModel>)Function.identity()));
        if (client.isConsentRequired()) {
            availableGrants.put(client.getId(), (ClientScopeModel)client);
        }
        for (final ConsentScopeRepresentation scopeRepresentation : requested.getGrantedScopes()) {
            final ClientScopeModel scopeModel = availableGrants.get(scopeRepresentation.getId());
            if (scopeModel == null) {
                final String msg = String.format("Scope id %s does not exist for client %s.", scopeRepresentation, consent.getClient().getName());
                this.event.error(msg);
                throw new IllegalArgumentException(msg);
            }
            consent.addGrantedClientScope(scopeModel);
        }
        return consent;
    }
    
    @Path("/linked-accounts")
    public LinkedAccountsResource linkedAccounts() {
        return new LinkedAccountsResource(this.session, this.request, this.client, this.auth, this.event, this.user);
    }
    
    @Path("/applications")
    @GET
    @Produces({ "application/json" })
    @NoCache
    public Stream<ClientRepresentation> applications(@QueryParam("name") final String name) {
        checkAccountApiEnabled();
        this.auth.requireOneOf("manage-account", "view-applications");
        final Set<ClientModel> clients = new HashSet<ClientModel>();
        final List<String> inUseClients = new LinkedList<String>();
        clients.addAll(this.session.sessions().getUserSessionsStream(this.realm, this.user).flatMap(s -> s.getAuthenticatedClientSessions().values().stream()).map((Function<? super Object, ?>)CommonClientSessionModel::getClient).peek(client -> inUseClients.add(client.getClientId())).collect((Collector<? super Object, ?, Collection<? extends ClientModel>>)Collectors.toSet()));
        final List<String> offlineClients = new LinkedList<String>();
        clients.addAll(this.session.sessions().getOfflineUserSessionsStream(this.realm, this.user).flatMap(s -> s.getAuthenticatedClientSessions().values().stream()).map((Function<? super Object, ?>)CommonClientSessionModel::getClient).peek(client -> offlineClients.add(client.getClientId())).collect((Collector<? super Object, ?, Collection<? extends ClientModel>>)Collectors.toSet()));
        final Map<String, UserConsentModel> consentModels = new HashMap<String, UserConsentModel>();
        final UserConsentModel userConsentModel;
        clients.addAll(this.session.users().getConsentsStream(this.realm, this.user.getId()).peek(consent -> userConsentModel = consentModels.put(consent.getClient().getClientId(), consent)).map((Function<? super Object, ?>)UserConsentModel::getClient).collect((Collector<? super Object, ?, Collection<? extends ClientModel>>)Collectors.toSet()));
        this.realm.getAlwaysDisplayInConsoleClientsStream().forEach(clients::add);
        return clients.stream().filter(client -> !client.isBearerOnly() && client.getBaseUrl() != null && !client.getClientId().isEmpty()).filter(client -> this.matches(client, name)).map(client -> this.modelToRepresentation(client, inUseClients, offlineClients, consentModels));
    }
    
    private boolean matches(final ClientModel client, final String name) {
        return name == null || (client.getName() != null && client.getName().toLowerCase().contains(name.toLowerCase()));
    }
    
    private static void checkAccountApiEnabled() {
        if (!Profile.isFeatureEnabled(Profile.Feature.ACCOUNT_API)) {
            throw new NotFoundException();
        }
    }
}
