// 
// Decompiled by Procyon v0.5.36
// 

package org.keycloak.models.utils;

import org.keycloak.validation.ClientValidationContext;
import org.keycloak.Config;
import org.keycloak.storage.federated.UserFederatedStorageProvider;
import org.keycloak.authorization.store.PermissionTicketStore;
import org.keycloak.authorization.model.PermissionTicket;
import org.keycloak.representations.idm.authorization.PermissionTicketRepresentation;
import org.keycloak.authorization.store.ScopeStore;
import org.keycloak.models.UserProvider;
import org.keycloak.authorization.store.ResourceStore;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.representations.idm.authorization.AbstractPolicyRepresentation;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.store.ResourceServerStore;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceOwnerRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.PolicyEnforcementMode;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.AuthorizationProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import java.util.ListIterator;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.AuthenticationExecutionRepresentation;
import org.keycloak.migration.migrators.MigrateTo8_0_0;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.provider.IdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.ModelException;
import org.keycloak.policy.PasswordPolicyNotMetException;
import org.keycloak.credential.CredentialInput;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.UserConsentModel;
import org.keycloak.representations.idm.UserConsentRepresentation;
import java.util.ArrayList;
import org.keycloak.models.UserModel;
import org.keycloak.models.ProtocolMapperContainerModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.common.util.UriUtils;
import org.keycloak.validation.ClientValidationUtil;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;
import java.io.IOException;
import org.keycloak.models.credential.dto.OTPSecretData;
import org.keycloak.models.credential.dto.OTPCredentialData;
import org.keycloak.util.JsonSerialization;
import org.keycloak.models.credential.dto.PasswordCredentialData;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.ClientTemplateRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.ClaimRepresentation;
import org.keycloak.migration.MigrationProvider;
import org.keycloak.representations.idm.OAuthClientRepresentation;
import org.keycloak.representations.idm.ApplicationRepresentation;
import org.keycloak.representations.idm.SocialLinkRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import java.util.LinkedList;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.AuthenticationExecutionExportRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;
import org.keycloak.representations.idm.UserFederationMapperRepresentation;
import org.keycloak.representations.idm.UserFederationProviderRepresentation;
import org.keycloak.component.ComponentModel;
import java.util.Arrays;
import org.keycloak.models.GroupModel;
import org.keycloak.representations.idm.ComponentExportRepresentation;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.RoleModel;
import org.keycloak.models.ScopeContainerModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.WebAuthnPolicy;
import java.util.Iterator;
import org.keycloak.keys.KeyProvider;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.models.BrowserSecurityHeaders;
import org.keycloak.representations.idm.ScopeMappingRepresentation;
import java.util.Map;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import java.util.List;
import org.keycloak.models.ClientScopeModel;
import java.util.HashMap;
import org.keycloak.migration.migrators.MigrationUtils;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.common.enums.SslRequired;
import java.util.Set;
import java.util.Collection;
import java.util.HashSet;
import org.keycloak.models.RealmModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OTPPolicy;
import org.keycloak.representations.idm.RealmRepresentation;
import org.jboss.logging.Logger;

public class RepresentationToModel
{
    private static Logger logger;
    public static final String OIDC = "openid-connect";
    
    public static OTPPolicy toPolicy(final RealmRepresentation rep) {
        final OTPPolicy policy = new OTPPolicy();
        if (rep.getOtpPolicyType() != null) {
            policy.setType(rep.getOtpPolicyType());
        }
        if (rep.getOtpPolicyLookAheadWindow() != null) {
            policy.setLookAheadWindow((int)rep.getOtpPolicyLookAheadWindow());
        }
        if (rep.getOtpPolicyInitialCounter() != null) {
            policy.setInitialCounter((int)rep.getOtpPolicyInitialCounter());
        }
        if (rep.getOtpPolicyAlgorithm() != null) {
            policy.setAlgorithm(rep.getOtpPolicyAlgorithm());
        }
        if (rep.getOtpPolicyDigits() != null) {
            policy.setDigits((int)rep.getOtpPolicyDigits());
        }
        if (rep.getOtpPolicyPeriod() != null) {
            policy.setPeriod((int)rep.getOtpPolicyPeriod());
        }
        return policy;
    }
    
    public static void importRealm(final KeycloakSession session, final RealmRepresentation rep, final RealmModel newRealm, final boolean skipUserDependent) {
        convertDeprecatedSocialProviders(rep);
        convertDeprecatedApplications(session, rep);
        convertDeprecatedClientTemplates(rep);
        newRealm.setName(rep.getRealm());
        if (rep.getDisplayName() != null) {
            newRealm.setDisplayName(rep.getDisplayName());
        }
        if (rep.getDisplayNameHtml() != null) {
            newRealm.setDisplayNameHtml(rep.getDisplayNameHtml());
        }
        if (rep.isEnabled() != null) {
            newRealm.setEnabled((boolean)rep.isEnabled());
        }
        if (rep.isUserManagedAccessAllowed() != null) {
            newRealm.setUserManagedAccessAllowed((boolean)rep.isUserManagedAccessAllowed());
        }
        if (rep.isBruteForceProtected() != null) {
            newRealm.setBruteForceProtected((boolean)rep.isBruteForceProtected());
        }
        if (rep.isPermanentLockout() != null) {
            newRealm.setPermanentLockout((boolean)rep.isPermanentLockout());
        }
        if (rep.getMaxFailureWaitSeconds() != null) {
            newRealm.setMaxFailureWaitSeconds((int)rep.getMaxFailureWaitSeconds());
        }
        if (rep.getMinimumQuickLoginWaitSeconds() != null) {
            newRealm.setMinimumQuickLoginWaitSeconds((int)rep.getMinimumQuickLoginWaitSeconds());
        }
        if (rep.getWaitIncrementSeconds() != null) {
            newRealm.setWaitIncrementSeconds((int)rep.getWaitIncrementSeconds());
        }
        if (rep.getQuickLoginCheckMilliSeconds() != null) {
            newRealm.setQuickLoginCheckMilliSeconds((long)rep.getQuickLoginCheckMilliSeconds());
        }
        if (rep.getMaxDeltaTimeSeconds() != null) {
            newRealm.setMaxDeltaTimeSeconds((int)rep.getMaxDeltaTimeSeconds());
        }
        if (rep.getFailureFactor() != null) {
            newRealm.setFailureFactor((int)rep.getFailureFactor());
        }
        if (rep.isEventsEnabled() != null) {
            newRealm.setEventsEnabled((boolean)rep.isEventsEnabled());
        }
        if (rep.getEnabledEventTypes() != null) {
            newRealm.setEnabledEventTypes((Set)new HashSet(rep.getEnabledEventTypes()));
        }
        if (rep.getEventsExpiration() != null) {
            newRealm.setEventsExpiration((long)rep.getEventsExpiration());
        }
        if (rep.getEventsListeners() != null) {
            newRealm.setEventsListeners((Set)new HashSet(rep.getEventsListeners()));
        }
        if (rep.isAdminEventsEnabled() != null) {
            newRealm.setAdminEventsEnabled((boolean)rep.isAdminEventsEnabled());
        }
        if (rep.isAdminEventsDetailsEnabled() != null) {
            newRealm.setAdminEventsDetailsEnabled((boolean)rep.isAdminEventsDetailsEnabled());
        }
        if (rep.getNotBefore() != null) {
            newRealm.setNotBefore((int)rep.getNotBefore());
        }
        if (rep.getDefaultSignatureAlgorithm() != null) {
            newRealm.setDefaultSignatureAlgorithm(rep.getDefaultSignatureAlgorithm());
        }
        if (rep.getRevokeRefreshToken() != null) {
            newRealm.setRevokeRefreshToken((boolean)rep.getRevokeRefreshToken());
        }
        else {
            newRealm.setRevokeRefreshToken(false);
        }
        if (rep.getRefreshTokenMaxReuse() != null) {
            newRealm.setRefreshTokenMaxReuse((int)rep.getRefreshTokenMaxReuse());
        }
        else {
            newRealm.setRefreshTokenMaxReuse(0);
        }
        if (rep.getAccessTokenLifespan() != null) {
            newRealm.setAccessTokenLifespan((int)rep.getAccessTokenLifespan());
        }
        else {
            newRealm.setAccessTokenLifespan(300);
        }
        if (rep.getAccessTokenLifespanForImplicitFlow() != null) {
            newRealm.setAccessTokenLifespanForImplicitFlow((int)rep.getAccessTokenLifespanForImplicitFlow());
        }
        else {
            newRealm.setAccessTokenLifespanForImplicitFlow(900);
        }
        if (rep.getSsoSessionIdleTimeout() != null) {
            newRealm.setSsoSessionIdleTimeout((int)rep.getSsoSessionIdleTimeout());
        }
        else {
            newRealm.setSsoSessionIdleTimeout(1800);
        }
        if (rep.getSsoSessionMaxLifespan() != null) {
            newRealm.setSsoSessionMaxLifespan((int)rep.getSsoSessionMaxLifespan());
        }
        else {
            newRealm.setSsoSessionMaxLifespan(36000);
        }
        if (rep.getSsoSessionMaxLifespanRememberMe() != null) {
            newRealm.setSsoSessionMaxLifespanRememberMe((int)rep.getSsoSessionMaxLifespanRememberMe());
        }
        if (rep.getSsoSessionIdleTimeoutRememberMe() != null) {
            newRealm.setSsoSessionIdleTimeoutRememberMe((int)rep.getSsoSessionIdleTimeoutRememberMe());
        }
        if (rep.getOfflineSessionIdleTimeout() != null) {
            newRealm.setOfflineSessionIdleTimeout((int)rep.getOfflineSessionIdleTimeout());
        }
        else {
            newRealm.setOfflineSessionIdleTimeout(2592000);
        }
        if (rep.getOfflineSessionMaxLifespanEnabled() != null) {
            newRealm.setOfflineSessionMaxLifespanEnabled((boolean)rep.getOfflineSessionMaxLifespanEnabled());
        }
        else {
            newRealm.setOfflineSessionMaxLifespanEnabled(false);
        }
        if (rep.getOfflineSessionMaxLifespan() != null) {
            newRealm.setOfflineSessionMaxLifespan((int)rep.getOfflineSessionMaxLifespan());
        }
        else {
            newRealm.setOfflineSessionMaxLifespan(5184000);
        }
        if (rep.getAccessCodeLifespan() != null) {
            newRealm.setAccessCodeLifespan((int)rep.getAccessCodeLifespan());
        }
        else {
            newRealm.setAccessCodeLifespan(60);
        }
        if (rep.getAccessCodeLifespanUserAction() != null) {
            newRealm.setAccessCodeLifespanUserAction((int)rep.getAccessCodeLifespanUserAction());
        }
        else {
            newRealm.setAccessCodeLifespanUserAction(300);
        }
        if (rep.getAccessCodeLifespanLogin() != null) {
            newRealm.setAccessCodeLifespanLogin((int)rep.getAccessCodeLifespanLogin());
        }
        else {
            newRealm.setAccessCodeLifespanLogin(1800);
        }
        if (rep.getActionTokenGeneratedByAdminLifespan() != null) {
            newRealm.setActionTokenGeneratedByAdminLifespan((int)rep.getActionTokenGeneratedByAdminLifespan());
        }
        else {
            newRealm.setActionTokenGeneratedByAdminLifespan(43200);
        }
        if (rep.getActionTokenGeneratedByUserLifespan() != null) {
            newRealm.setActionTokenGeneratedByUserLifespan((int)rep.getActionTokenGeneratedByUserLifespan());
        }
        else {
            newRealm.setActionTokenGeneratedByUserLifespan(newRealm.getAccessCodeLifespanUserAction());
        }
        if (rep.getSslRequired() != null) {
            newRealm.setSslRequired(SslRequired.valueOf(rep.getSslRequired().toUpperCase()));
        }
        if (rep.isRegistrationAllowed() != null) {
            newRealm.setRegistrationAllowed((boolean)rep.isRegistrationAllowed());
        }
        if (rep.isRegistrationEmailAsUsername() != null) {
            newRealm.setRegistrationEmailAsUsername((boolean)rep.isRegistrationEmailAsUsername());
        }
        if (rep.isRememberMe() != null) {
            newRealm.setRememberMe((boolean)rep.isRememberMe());
        }
        if (rep.isVerifyEmail() != null) {
            newRealm.setVerifyEmail((boolean)rep.isVerifyEmail());
        }
        if (rep.isLoginWithEmailAllowed() != null) {
            newRealm.setLoginWithEmailAllowed((boolean)rep.isLoginWithEmailAllowed());
        }
        if (rep.isDuplicateEmailsAllowed() != null) {
            newRealm.setDuplicateEmailsAllowed((boolean)rep.isDuplicateEmailsAllowed());
        }
        if (rep.isResetPasswordAllowed() != null) {
            newRealm.setResetPasswordAllowed((boolean)rep.isResetPasswordAllowed());
        }
        if (rep.isEditUsernameAllowed() != null) {
            newRealm.setEditUsernameAllowed((boolean)rep.isEditUsernameAllowed());
        }
        if (rep.getLoginTheme() != null) {
            newRealm.setLoginTheme(rep.getLoginTheme());
        }
        if (rep.getAccountTheme() != null) {
            newRealm.setAccountTheme(rep.getAccountTheme());
        }
        if (rep.getAdminTheme() != null) {
            newRealm.setAdminTheme(rep.getAdminTheme());
        }
        if (rep.getEmailTheme() != null) {
            newRealm.setEmailTheme(rep.getEmailTheme());
        }
        if (rep.getRequiredCredentials() != null) {
            for (final String requiredCred : rep.getRequiredCredentials()) {
                newRealm.addRequiredCredential(requiredCred);
            }
        }
        else {
            newRealm.addRequiredCredential("password");
        }
        if (rep.getPasswordPolicy() != null) {
            newRealm.setPasswordPolicy(PasswordPolicy.parse(session, rep.getPasswordPolicy()));
        }
        if (rep.getOtpPolicyType() != null) {
            newRealm.setOTPPolicy(toPolicy(rep));
        }
        else {
            newRealm.setOTPPolicy(OTPPolicy.DEFAULT_POLICY);
        }
        WebAuthnPolicy webAuthnPolicy = getWebAuthnPolicyTwoFactor(rep);
        newRealm.setWebAuthnPolicy(webAuthnPolicy);
        webAuthnPolicy = getWebAuthnPolicyPasswordless(rep);
        newRealm.setWebAuthnPolicyPasswordless(webAuthnPolicy);
        final Map<String, String> mappedFlows = importAuthenticationFlows(newRealm, rep);
        if (rep.getRequiredActions() != null) {
            for (final RequiredActionProviderRepresentation action : rep.getRequiredActions()) {
                final RequiredActionProviderModel model = toModel(action);
                MigrationUtils.updateOTPRequiredAction(model);
                newRealm.addRequiredActionProvider(model);
            }
        }
        else {
            DefaultRequiredActions.addActions(newRealm);
        }
        importIdentityProviders(rep, newRealm, session);
        importIdentityProviderMappers(rep, newRealm);
        Map<String, ClientScopeModel> clientScopes = new HashMap<String, ClientScopeModel>();
        if (rep.getClientScopes() != null) {
            clientScopes = createClientScopes(session, rep.getClientScopes(), newRealm);
        }
        if (rep.getDefaultDefaultClientScopes() != null) {
            for (final String clientScopeName : rep.getDefaultDefaultClientScopes()) {
                final ClientScopeModel clientScope = clientScopes.get(clientScopeName);
                if (clientScope != null) {
                    newRealm.addDefaultClientScope(clientScope, true);
                }
                else {
                    RepresentationToModel.logger.warnf("Referenced client scope '%s' doesn't exists", (Object)clientScopeName);
                }
            }
        }
        if (rep.getDefaultOptionalClientScopes() != null) {
            for (final String clientScopeName : rep.getDefaultOptionalClientScopes()) {
                final ClientScopeModel clientScope = clientScopes.get(clientScopeName);
                if (clientScope != null) {
                    newRealm.addDefaultClientScope(clientScope, false);
                }
                else {
                    RepresentationToModel.logger.warnf("Referenced client scope '%s' doesn't exists", (Object)clientScopeName);
                }
            }
        }
        if (rep.getClients() != null) {
            createClients(session, rep, newRealm, mappedFlows);
        }
        importRoles(rep.getRoles(), newRealm);
        if (rep.getDefaultRoles() != null) {
            for (final String roleString : rep.getDefaultRoles()) {
                newRealm.addDefaultRole(roleString.trim());
            }
        }
        if (rep.getClients() != null) {
            for (final ClientRepresentation resourceRep : rep.getClients()) {
                if (resourceRep.getDefaultRoles() != null) {
                    final ClientModel clientModel = newRealm.getClientByClientId(resourceRep.getClientId());
                    clientModel.updateDefaultRoles(resourceRep.getDefaultRoles());
                }
            }
        }
        if (rep.getClientScopeMappings() != null) {
            for (final Map.Entry<String, List<ScopeMappingRepresentation>> entry : rep.getClientScopeMappings().entrySet()) {
                final ClientModel app = newRealm.getClientByClientId((String)entry.getKey());
                if (app == null) {
                    throw new RuntimeException("Unable to find client role mappings for client: " + entry.getKey());
                }
                createClientScopeMappings(newRealm, app, entry.getValue());
            }
        }
        if (rep.getScopeMappings() != null) {
            for (final ScopeMappingRepresentation scope : rep.getScopeMappings()) {
                final ScopeContainerModel scopeContainer = getScopeContainerHavingScope(newRealm, scope);
                for (final String roleString2 : scope.getRoles()) {
                    RoleModel role = newRealm.getRole(roleString2.trim());
                    if (role == null) {
                        role = newRealm.addRole(roleString2.trim());
                    }
                    scopeContainer.addScopeMapping(role);
                }
            }
        }
        if (rep.getSmtpServer() != null) {
            newRealm.setSmtpConfig((Map)new HashMap(rep.getSmtpServer()));
        }
        if (rep.getBrowserSecurityHeaders() != null) {
            newRealm.setBrowserSecurityHeaders(rep.getBrowserSecurityHeaders());
        }
        else {
            newRealm.setBrowserSecurityHeaders((Map)BrowserSecurityHeaders.defaultHeaders);
        }
        if (rep.getComponents() != null) {
            final MultivaluedHashMap<String, ComponentExportRepresentation> components = (MultivaluedHashMap<String, ComponentExportRepresentation>)rep.getComponents();
            final String parentId = newRealm.getId();
            importComponents(newRealm, components, parentId);
        }
        importUserFederationProvidersAndMappers(session, rep, newRealm);
        if (rep.getGroups() != null) {
            importGroups(newRealm, rep);
            if (rep.getDefaultGroups() != null) {
                for (final String path : rep.getDefaultGroups()) {
                    final GroupModel found = KeycloakModelUtils.findGroupByPath(newRealm, path);
                    if (found == null) {
                        throw new RuntimeException("default group in realm rep doesn't exist: " + path);
                    }
                    newRealm.addDefaultGroup(found);
                }
            }
        }
        if (rep.getUsers() != null) {
            for (final UserRepresentation userRep : rep.getUsers()) {
                createUser(session, newRealm, userRep);
            }
        }
        if (rep.getFederatedUsers() != null) {
            for (final UserRepresentation userRep : rep.getFederatedUsers()) {
                importFederatedUser(session, newRealm, userRep);
            }
        }
        if (!skipUserDependent) {
            importRealmAuthorizationSettings(rep, newRealm, session);
        }
        if (rep.isInternationalizationEnabled() != null) {
            newRealm.setInternationalizationEnabled((boolean)rep.isInternationalizationEnabled());
        }
        if (rep.getSupportedLocales() != null) {
            newRealm.setSupportedLocales((Set)new HashSet(rep.getSupportedLocales()));
        }
        if (rep.getDefaultLocale() != null) {
            newRealm.setDefaultLocale(rep.getDefaultLocale());
        }
        if (rep.getAttributes() != null) {
            for (final Map.Entry<String, String> attr : rep.getAttributes().entrySet()) {
                newRealm.setAttribute((String)attr.getKey(), (String)attr.getValue());
            }
        }
        if (newRealm.getComponents(newRealm.getId(), KeyProvider.class.getName()).isEmpty()) {
            if (rep.getPrivateKey() != null) {
                DefaultKeyProviders.createProviders(newRealm, rep.getPrivateKey(), rep.getCertificate());
            }
            else {
                DefaultKeyProviders.createProviders(newRealm);
            }
        }
    }
    
    private static WebAuthnPolicy getWebAuthnPolicyTwoFactor(final RealmRepresentation rep) {
        final WebAuthnPolicy webAuthnPolicy = new WebAuthnPolicy();
        String webAuthnPolicyRpEntityName = rep.getWebAuthnPolicyRpEntityName();
        if (webAuthnPolicyRpEntityName == null || webAuthnPolicyRpEntityName.isEmpty()) {
            webAuthnPolicyRpEntityName = "keycloak";
        }
        webAuthnPolicy.setRpEntityName(webAuthnPolicyRpEntityName);
        List<String> webAuthnPolicySignatureAlgorithms = (List<String>)rep.getWebAuthnPolicySignatureAlgorithms();
        if (webAuthnPolicySignatureAlgorithms == null || webAuthnPolicySignatureAlgorithms.isEmpty()) {
            webAuthnPolicySignatureAlgorithms = Arrays.asList("ES256".split(","));
        }
        webAuthnPolicy.setSignatureAlgorithm((List)webAuthnPolicySignatureAlgorithms);
        String webAuthnPolicyRpId = rep.getWebAuthnPolicyRpId();
        if (webAuthnPolicyRpId == null || webAuthnPolicyRpId.isEmpty()) {
            webAuthnPolicyRpId = "";
        }
        webAuthnPolicy.setRpId(webAuthnPolicyRpId);
        String webAuthnPolicyAttestationConveyancePreference = rep.getWebAuthnPolicyAttestationConveyancePreference();
        if (webAuthnPolicyAttestationConveyancePreference == null || webAuthnPolicyAttestationConveyancePreference.isEmpty()) {
            webAuthnPolicyAttestationConveyancePreference = "not specified";
        }
        webAuthnPolicy.setAttestationConveyancePreference(webAuthnPolicyAttestationConveyancePreference);
        String webAuthnPolicyAuthenticatorAttachment = rep.getWebAuthnPolicyAuthenticatorAttachment();
        if (webAuthnPolicyAuthenticatorAttachment == null || webAuthnPolicyAuthenticatorAttachment.isEmpty()) {
            webAuthnPolicyAuthenticatorAttachment = "not specified";
        }
        webAuthnPolicy.setAuthenticatorAttachment(webAuthnPolicyAuthenticatorAttachment);
        String webAuthnPolicyRequireResidentKey = rep.getWebAuthnPolicyRequireResidentKey();
        if (webAuthnPolicyRequireResidentKey == null || webAuthnPolicyRequireResidentKey.isEmpty()) {
            webAuthnPolicyRequireResidentKey = "not specified";
        }
        webAuthnPolicy.setRequireResidentKey(webAuthnPolicyRequireResidentKey);
        String webAuthnPolicyUserVerificationRequirement = rep.getWebAuthnPolicyUserVerificationRequirement();
        if (webAuthnPolicyUserVerificationRequirement == null || webAuthnPolicyUserVerificationRequirement.isEmpty()) {
            webAuthnPolicyUserVerificationRequirement = "not specified";
        }
        webAuthnPolicy.setUserVerificationRequirement(webAuthnPolicyUserVerificationRequirement);
        final Integer webAuthnPolicyCreateTimeout = rep.getWebAuthnPolicyCreateTimeout();
        if (webAuthnPolicyCreateTimeout != null) {
            webAuthnPolicy.setCreateTimeout((int)webAuthnPolicyCreateTimeout);
        }
        else {
            webAuthnPolicy.setCreateTimeout(0);
        }
        final Boolean webAuthnPolicyAvoidSameAuthenticatorRegister = rep.isWebAuthnPolicyAvoidSameAuthenticatorRegister();
        if (webAuthnPolicyAvoidSameAuthenticatorRegister != null) {
            webAuthnPolicy.setAvoidSameAuthenticatorRegister((boolean)webAuthnPolicyAvoidSameAuthenticatorRegister);
        }
        final List<String> webAuthnPolicyAcceptableAaguids = (List<String>)rep.getWebAuthnPolicyAcceptableAaguids();
        if (webAuthnPolicyAcceptableAaguids != null) {
            webAuthnPolicy.setAcceptableAaguids((List)webAuthnPolicyAcceptableAaguids);
        }
        return webAuthnPolicy;
    }
    
    private static WebAuthnPolicy getWebAuthnPolicyPasswordless(final RealmRepresentation rep) {
        final WebAuthnPolicy webAuthnPolicy = new WebAuthnPolicy();
        String webAuthnPolicyRpEntityName = rep.getWebAuthnPolicyPasswordlessRpEntityName();
        if (webAuthnPolicyRpEntityName == null || webAuthnPolicyRpEntityName.isEmpty()) {
            webAuthnPolicyRpEntityName = "keycloak";
        }
        webAuthnPolicy.setRpEntityName(webAuthnPolicyRpEntityName);
        List<String> webAuthnPolicySignatureAlgorithms = (List<String>)rep.getWebAuthnPolicyPasswordlessSignatureAlgorithms();
        if (webAuthnPolicySignatureAlgorithms == null || webAuthnPolicySignatureAlgorithms.isEmpty()) {
            webAuthnPolicySignatureAlgorithms = Arrays.asList("ES256".split(","));
        }
        webAuthnPolicy.setSignatureAlgorithm((List)webAuthnPolicySignatureAlgorithms);
        String webAuthnPolicyRpId = rep.getWebAuthnPolicyPasswordlessRpId();
        if (webAuthnPolicyRpId == null || webAuthnPolicyRpId.isEmpty()) {
            webAuthnPolicyRpId = "";
        }
        webAuthnPolicy.setRpId(webAuthnPolicyRpId);
        String webAuthnPolicyAttestationConveyancePreference = rep.getWebAuthnPolicyPasswordlessAttestationConveyancePreference();
        if (webAuthnPolicyAttestationConveyancePreference == null || webAuthnPolicyAttestationConveyancePreference.isEmpty()) {
            webAuthnPolicyAttestationConveyancePreference = "not specified";
        }
        webAuthnPolicy.setAttestationConveyancePreference(webAuthnPolicyAttestationConveyancePreference);
        String webAuthnPolicyAuthenticatorAttachment = rep.getWebAuthnPolicyPasswordlessAuthenticatorAttachment();
        if (webAuthnPolicyAuthenticatorAttachment == null || webAuthnPolicyAuthenticatorAttachment.isEmpty()) {
            webAuthnPolicyAuthenticatorAttachment = "not specified";
        }
        webAuthnPolicy.setAuthenticatorAttachment(webAuthnPolicyAuthenticatorAttachment);
        String webAuthnPolicyRequireResidentKey = rep.getWebAuthnPolicyPasswordlessRequireResidentKey();
        if (webAuthnPolicyRequireResidentKey == null || webAuthnPolicyRequireResidentKey.isEmpty()) {
            webAuthnPolicyRequireResidentKey = "not specified";
        }
        webAuthnPolicy.setRequireResidentKey(webAuthnPolicyRequireResidentKey);
        String webAuthnPolicyUserVerificationRequirement = rep.getWebAuthnPolicyPasswordlessUserVerificationRequirement();
        if (webAuthnPolicyUserVerificationRequirement == null || webAuthnPolicyUserVerificationRequirement.isEmpty()) {
            webAuthnPolicyUserVerificationRequirement = "not specified";
        }
        webAuthnPolicy.setUserVerificationRequirement(webAuthnPolicyUserVerificationRequirement);
        final Integer webAuthnPolicyCreateTimeout = rep.getWebAuthnPolicyPasswordlessCreateTimeout();
        if (webAuthnPolicyCreateTimeout != null) {
            webAuthnPolicy.setCreateTimeout((int)webAuthnPolicyCreateTimeout);
        }
        else {
            webAuthnPolicy.setCreateTimeout(0);
        }
        final Boolean webAuthnPolicyAvoidSameAuthenticatorRegister = rep.isWebAuthnPolicyPasswordlessAvoidSameAuthenticatorRegister();
        if (webAuthnPolicyAvoidSameAuthenticatorRegister != null) {
            webAuthnPolicy.setAvoidSameAuthenticatorRegister((boolean)webAuthnPolicyAvoidSameAuthenticatorRegister);
        }
        final List<String> webAuthnPolicyAcceptableAaguids = (List<String>)rep.getWebAuthnPolicyPasswordlessAcceptableAaguids();
        if (webAuthnPolicyAcceptableAaguids != null) {
            webAuthnPolicy.setAcceptableAaguids((List)webAuthnPolicyAcceptableAaguids);
        }
        return webAuthnPolicy;
    }
    
    public static void importUserFederationProvidersAndMappers(final KeycloakSession session, final RealmRepresentation rep, final RealmModel newRealm) {
        final Set<String> convertSet = new HashSet<String>();
        convertSet.add("ldap");
        convertSet.add("kerberos");
        final Map<String, String> mapperConvertSet = new HashMap<String, String>();
        mapperConvertSet.put("ldap", "org.keycloak.storage.ldap.mappers.LDAPStorageMapper");
        final Map<String, ComponentModel> userStorageModels = new HashMap<String, ComponentModel>();
        if (rep.getUserFederationProviders() != null) {
            for (final UserFederationProviderRepresentation fedRep : rep.getUserFederationProviders()) {
                if (convertSet.contains(fedRep.getProviderName())) {
                    final ComponentModel component = convertFedProviderToComponent(newRealm.getId(), fedRep);
                    userStorageModels.put(fedRep.getDisplayName(), newRealm.importComponentModel(component));
                }
            }
        }
        final Set<String> storageProvidersWhichShouldImportDefaultMappers = new HashSet<String>(userStorageModels.keySet());
        if (rep.getUserFederationMappers() != null) {
            for (final UserFederationMapperRepresentation representation : rep.getUserFederationMappers()) {
                if (userStorageModels.containsKey(representation.getFederationProviderDisplayName())) {
                    final ComponentModel parent = userStorageModels.get(representation.getFederationProviderDisplayName());
                    final String newMapperType = mapperConvertSet.get(parent.getProviderId());
                    final ComponentModel mapper = convertFedMapperToComponent(newRealm, parent, representation, newMapperType);
                    newRealm.importComponentModel(mapper);
                    storageProvidersWhichShouldImportDefaultMappers.remove(representation.getFederationProviderDisplayName());
                }
            }
        }
        for (final String providerDisplayName : storageProvidersWhichShouldImportDefaultMappers) {
            ComponentUtil.notifyCreated(session, newRealm, userStorageModels.get(providerDisplayName));
        }
    }
    
    protected static void importComponents(final RealmModel newRealm, final MultivaluedHashMap<String, ComponentExportRepresentation> components, final String parentId) {
        for (final Map.Entry<String, List<ComponentExportRepresentation>> entry : components.entrySet()) {
            final String providerType = entry.getKey();
            for (final ComponentExportRepresentation compRep : entry.getValue()) {
                ComponentModel component = new ComponentModel();
                component.setId(compRep.getId());
                component.setName(compRep.getName());
                component.setConfig(compRep.getConfig());
                component.setProviderType(providerType);
                component.setProviderId(compRep.getProviderId());
                component.setSubType(compRep.getSubType());
                component.setParentId(parentId);
                component = newRealm.importComponentModel(component);
                if (compRep.getSubComponents() != null) {
                    importComponents(newRealm, (MultivaluedHashMap<String, ComponentExportRepresentation>)compRep.getSubComponents(), component.getId());
                }
            }
        }
    }
    
    public static void importRoles(final RolesRepresentation realmRoles, final RealmModel realm) {
        if (realmRoles == null) {
            return;
        }
        if (realmRoles.getRealm() != null) {
            for (final RoleRepresentation roleRep : realmRoles.getRealm()) {
                createRole(realm, roleRep);
            }
        }
        if (realmRoles.getClient() != null) {
            for (final Map.Entry<String, List<RoleRepresentation>> entry : realmRoles.getClient().entrySet()) {
                final ClientModel client = realm.getClientByClientId((String)entry.getKey());
                if (client == null) {
                    throw new RuntimeException("App doesn't exist in role definitions: " + entry.getKey());
                }
                for (final RoleRepresentation roleRep2 : entry.getValue()) {
                    final RoleModel role = (roleRep2.getId() != null) ? client.addRole(roleRep2.getId(), roleRep2.getName()) : client.addRole(roleRep2.getName());
                    role.setDescription(roleRep2.getDescription());
                    if (roleRep2.getAttributes() != null) {
                        roleRep2.getAttributes().forEach((key, value) -> role.setAttribute(key, (Collection)value));
                    }
                }
            }
        }
        if (realmRoles.getRealm() != null) {
            for (final RoleRepresentation roleRep : realmRoles.getRealm()) {
                final RoleModel role2 = realm.getRole(roleRep.getName());
                addComposites(role2, roleRep, realm);
            }
        }
        if (realmRoles.getClient() != null) {
            for (final Map.Entry<String, List<RoleRepresentation>> entry : realmRoles.getClient().entrySet()) {
                final ClientModel client = realm.getClientByClientId((String)entry.getKey());
                if (client == null) {
                    throw new RuntimeException("App doesn't exist in role definitions: " + entry.getKey());
                }
                for (final RoleRepresentation roleRep2 : entry.getValue()) {
                    final RoleModel role = client.getRole(roleRep2.getName());
                    addComposites(role, roleRep2, realm);
                }
            }
        }
    }
    
    public static void importGroups(final RealmModel realm, final RealmRepresentation rep) {
        final List<GroupRepresentation> groups = (List<GroupRepresentation>)rep.getGroups();
        if (groups == null) {
            return;
        }
        final GroupModel parent = null;
        for (final GroupRepresentation group : groups) {
            importGroup(realm, parent, group);
        }
    }
    
    public static void importGroup(final RealmModel realm, final GroupModel parent, final GroupRepresentation group) {
        final GroupModel newGroup = realm.createGroup(group.getId(), group.getName(), parent);
        if (group.getAttributes() != null) {
            for (final Map.Entry<String, List<String>> attr : group.getAttributes().entrySet()) {
                newGroup.setAttribute((String)attr.getKey(), (List)attr.getValue());
            }
        }
        if (group.getRealmRoles() != null) {
            for (final String roleString : group.getRealmRoles()) {
                RoleModel role = realm.getRole(roleString.trim());
                if (role == null) {
                    role = realm.addRole(roleString.trim());
                }
                newGroup.grantRole(role);
            }
        }
        if (group.getClientRoles() != null) {
            for (final Map.Entry<String, List<String>> entry : group.getClientRoles().entrySet()) {
                final ClientModel client = realm.getClientByClientId((String)entry.getKey());
                if (client == null) {
                    throw new RuntimeException("Unable to find client role mappings for client: " + entry.getKey());
                }
                final List<String> roleNames = entry.getValue();
                for (final String roleName : roleNames) {
                    RoleModel role2 = client.getRole(roleName.trim());
                    if (role2 == null) {
                        role2 = client.addRole(roleName.trim());
                    }
                    newGroup.grantRole(role2);
                }
            }
        }
        if (group.getSubGroups() != null) {
            for (final GroupRepresentation subGroup : group.getSubGroups()) {
                importGroup(realm, newGroup, subGroup);
            }
        }
    }
    
    public static Map<String, String> importAuthenticationFlows(final RealmModel newRealm, final RealmRepresentation rep) {
        final Map<String, String> mappedFlows = new HashMap<String, String>();
        if (rep.getAuthenticationFlows() == null) {
            DefaultAuthenticationFlows.migrateFlows(newRealm);
        }
        else {
            for (final AuthenticatorConfigRepresentation configRep : rep.getAuthenticatorConfig()) {
                final AuthenticatorConfigModel model = toModel(configRep);
                newRealm.addAuthenticatorConfig(model);
            }
            for (final AuthenticationFlowRepresentation flowRep : rep.getAuthenticationFlows()) {
                AuthenticationFlowModel model2 = toModel(flowRep);
                final String previousId = model2.getId();
                model2.setId((String)null);
                model2 = newRealm.addAuthenticationFlow(model2);
                mappedFlows.put(previousId, model2.getId());
            }
            for (final AuthenticationFlowRepresentation flowRep : rep.getAuthenticationFlows()) {
                final AuthenticationFlowModel model2 = newRealm.getFlowByAlias(flowRep.getAlias());
                for (final AuthenticationExecutionExportRepresentation exeRep : flowRep.getAuthenticationExecutions()) {
                    final AuthenticationExecutionModel execution = toModel(newRealm, model2, exeRep);
                    newRealm.addAuthenticatorExecution(execution);
                }
            }
        }
        if (rep.getBrowserFlow() == null) {
            newRealm.setBrowserFlow(newRealm.getFlowByAlias("browser"));
        }
        else {
            newRealm.setBrowserFlow(newRealm.getFlowByAlias(rep.getBrowserFlow()));
        }
        if (rep.getRegistrationFlow() == null) {
            newRealm.setRegistrationFlow(newRealm.getFlowByAlias("registration"));
        }
        else {
            newRealm.setRegistrationFlow(newRealm.getFlowByAlias(rep.getRegistrationFlow()));
        }
        if (rep.getDirectGrantFlow() == null) {
            newRealm.setDirectGrantFlow(newRealm.getFlowByAlias("direct grant"));
        }
        else {
            newRealm.setDirectGrantFlow(newRealm.getFlowByAlias(rep.getDirectGrantFlow()));
        }
        if (rep.getResetCredentialsFlow() == null) {
            final AuthenticationFlowModel resetFlow = newRealm.getFlowByAlias("reset credentials");
            if (resetFlow == null) {
                DefaultAuthenticationFlows.resetCredentialsFlow(newRealm);
            }
            else {
                newRealm.setResetCredentialsFlow(resetFlow);
            }
        }
        else {
            newRealm.setResetCredentialsFlow(newRealm.getFlowByAlias(rep.getResetCredentialsFlow()));
        }
        if (rep.getClientAuthenticationFlow() == null) {
            final AuthenticationFlowModel clientFlow = newRealm.getFlowByAlias("clients");
            if (clientFlow == null) {
                DefaultAuthenticationFlows.clientAuthFlow(newRealm);
            }
            else {
                newRealm.setClientAuthenticationFlow(clientFlow);
            }
        }
        else {
            newRealm.setClientAuthenticationFlow(newRealm.getFlowByAlias(rep.getClientAuthenticationFlow()));
        }
        if (newRealm.getFlowByAlias("first broker login") == null) {
            DefaultAuthenticationFlows.firstBrokerLoginFlow(newRealm, true);
        }
        String defaultProvider = null;
        if (rep.getIdentityProviders() != null) {
            for (final IdentityProviderRepresentation i : rep.getIdentityProviders()) {
                if (i.isEnabled() && i.isAuthenticateByDefault()) {
                    defaultProvider = i.getProviderId();
                    break;
                }
            }
        }
        if (rep.getDockerAuthenticationFlow() == null) {
            final AuthenticationFlowModel dockerAuthenticationFlow = newRealm.getFlowByAlias("docker auth");
            if (dockerAuthenticationFlow == null) {
                DefaultAuthenticationFlows.dockerAuthenticationFlow(newRealm);
            }
            else {
                newRealm.setDockerAuthenticationFlow(dockerAuthenticationFlow);
            }
        }
        else {
            newRealm.setDockerAuthenticationFlow(newRealm.getFlowByAlias(rep.getDockerAuthenticationFlow()));
        }
        DefaultAuthenticationFlows.addIdentityProviderAuthenticator(newRealm, defaultProvider);
        return mappedFlows;
    }
    
    private static void convertDeprecatedSocialProviders(final RealmRepresentation rep) {
        if (rep.isSocial() != null && rep.isSocial() && rep.getSocialProviders() != null && !rep.getSocialProviders().isEmpty() && rep.getIdentityProviders() == null) {
            final Boolean updateProfileFirstLogin = rep.isUpdateProfileOnInitialSocialLogin() != null && rep.isUpdateProfileOnInitialSocialLogin();
            if (rep.getSocialProviders() != null) {
                RepresentationToModel.logger.warn((Object)"Using deprecated 'social' configuration in JSON representation. It will be removed in future versions");
                final List<IdentityProviderRepresentation> identityProviders = new LinkedList<IdentityProviderRepresentation>();
                for (final String k : rep.getSocialProviders().keySet()) {
                    if (k.endsWith(".key")) {
                        final String providerId = k.split("\\.")[0];
                        final String key = rep.getSocialProviders().get(k);
                        final String secret = rep.getSocialProviders().get(k.replace(".key", ".secret"));
                        final IdentityProviderRepresentation identityProvider = new IdentityProviderRepresentation();
                        identityProvider.setAlias(providerId);
                        identityProvider.setProviderId(providerId);
                        identityProvider.setEnabled(true);
                        identityProvider.setLinkOnly(false);
                        identityProvider.setUpdateProfileFirstLogin((boolean)updateProfileFirstLogin);
                        final Map<String, String> config = new HashMap<String, String>();
                        config.put("clientId", key);
                        config.put("clientSecret", secret);
                        identityProvider.setConfig((Map)config);
                        identityProviders.add(identityProvider);
                    }
                }
                rep.setIdentityProviders((List)identityProviders);
            }
        }
    }
    
    private static void convertDeprecatedSocialProviders(final UserRepresentation user) {
        if (user.getSocialLinks() != null && !user.getSocialLinks().isEmpty() && user.getFederatedIdentities() == null) {
            RepresentationToModel.logger.warnf("Using deprecated 'socialLinks' configuration in JSON representation for user '%s'. It will be removed in future versions", (Object)user.getUsername());
            final List<FederatedIdentityRepresentation> federatedIdentities = new LinkedList<FederatedIdentityRepresentation>();
            for (final SocialLinkRepresentation social : user.getSocialLinks()) {
                final FederatedIdentityRepresentation federatedIdentity = new FederatedIdentityRepresentation();
                federatedIdentity.setIdentityProvider(social.getSocialProvider());
                federatedIdentity.setUserId(social.getSocialUserId());
                federatedIdentity.setUserName(social.getSocialUsername());
                federatedIdentities.add(federatedIdentity);
            }
            user.setFederatedIdentities((List)federatedIdentities);
        }
        user.setSocialLinks((List)null);
    }
    
    private static void convertDeprecatedApplications(final KeycloakSession session, final RealmRepresentation realm) {
        if (realm.getApplications() != null || realm.getOauthClients() != null) {
            if (realm.getClients() == null) {
                realm.setClients((List)new LinkedList());
            }
            final List<ApplicationRepresentation> clients = new LinkedList<ApplicationRepresentation>();
            if (realm.getApplications() != null) {
                clients.addAll(realm.getApplications());
            }
            if (realm.getOauthClients() != null) {
                clients.addAll(realm.getOauthClients());
            }
            for (final ApplicationRepresentation app : clients) {
                app.setClientId(app.getName());
                app.setName((String)null);
                if (app instanceof OAuthClientRepresentation) {
                    app.setConsentRequired(Boolean.valueOf(true));
                    app.setFullScopeAllowed(Boolean.valueOf(false));
                }
                if (app.getProtocolMappers() == null && app.getClaims() != null) {
                    final long mask = getClaimsMask(app.getClaims());
                    final List<ProtocolMapperRepresentation> convertedProtocolMappers = ((MigrationProvider)session.getProvider((Class)MigrationProvider.class)).getMappersForClaimMask(mask);
                    app.setProtocolMappers((List)convertedProtocolMappers);
                    app.setClaims((ClaimRepresentation)null);
                }
                realm.getClients().add(app);
            }
        }
        if (realm.getApplicationScopeMappings() != null && realm.getClientScopeMappings() == null) {
            realm.setClientScopeMappings(realm.getApplicationScopeMappings());
        }
        if (realm.getRoles() != null && realm.getRoles().getApplication() != null && realm.getRoles().getClient() == null) {
            realm.getRoles().setClient(realm.getRoles().getApplication());
        }
        if (realm.getUsers() != null) {
            for (final UserRepresentation user : realm.getUsers()) {
                if (user.getApplicationRoles() != null && user.getClientRoles() == null) {
                    user.setClientRoles(user.getApplicationRoles());
                }
            }
        }
        if (realm.getRoles() != null && realm.getRoles().getRealm() != null) {
            for (final RoleRepresentation role : realm.getRoles().getRealm()) {
                if (role.getComposites() != null && role.getComposites().getApplication() != null && role.getComposites().getClient() == null) {
                    role.getComposites().setClient(role.getComposites().getApplication());
                }
            }
        }
        if (realm.getRoles() != null && realm.getRoles().getClient() != null) {
            for (final Map.Entry<String, List<RoleRepresentation>> clientRoles : realm.getRoles().getClient().entrySet()) {
                for (final RoleRepresentation role2 : clientRoles.getValue()) {
                    if (role2.getComposites() != null && role2.getComposites().getApplication() != null && role2.getComposites().getClient() == null) {
                        role2.getComposites().setClient(role2.getComposites().getApplication());
                    }
                }
            }
        }
    }
    
    private static void convertDeprecatedClientTemplates(final RealmRepresentation realm) {
        if (realm.getClientTemplates() != null) {
            RepresentationToModel.logger.warnf("Using deprecated 'clientTemplates' configuration in JSON representation for realm '%s'. It will be removed in future versions", (Object)realm.getRealm());
            final List<ClientScopeRepresentation> clientScopes = new LinkedList<ClientScopeRepresentation>();
            for (final ClientTemplateRepresentation template : realm.getClientTemplates()) {
                final ClientScopeRepresentation scopeRep = new ClientScopeRepresentation();
                scopeRep.setId(template.getId());
                scopeRep.setName(template.getName());
                scopeRep.setProtocol(template.getProtocol());
                scopeRep.setDescription(template.getDescription());
                scopeRep.setAttributes(template.getAttributes());
                scopeRep.setProtocolMappers(template.getProtocolMappers());
                clientScopes.add(scopeRep);
            }
            realm.setClientScopes((List)clientScopes);
        }
    }
    
    private static void convertDeprecatedCredentialsFormat(final UserRepresentation user) {
        if (user.getCredentials() != null) {
            for (final CredentialRepresentation cred : user.getCredentials()) {
                try {
                    if ((cred.getCredentialData() != null && cred.getSecretData() != null) || cred.getValue() != null) {
                        continue;
                    }
                    RepresentationToModel.logger.warnf("Using deprecated 'credentials' format in JSON representation for user '%s'. It will be removed in future versions", (Object)user.getUsername());
                    if ("password".equals(cred.getType()) || "password-history".equals(cred.getType())) {
                        final PasswordCredentialData credentialData = new PasswordCredentialData((int)cred.getHashIterations(), cred.getAlgorithm());
                        cred.setCredentialData(JsonSerialization.writeValueAsString((Object)credentialData));
                        cred.setSecretData("{\"value\":\"" + cred.getHashedSaltedValue() + "\",\"salt\":\"" + cred.getSalt() + "\"}");
                        cred.setPriority(Integer.valueOf(10));
                    }
                    else {
                        if (!"totp".equals(cred.getType()) && !"hotp".equals(cred.getType())) {
                            continue;
                        }
                        final OTPCredentialData credentialData2 = new OTPCredentialData(cred.getType(), (int)cred.getDigits(), (int)cred.getCounter(), (int)cred.getPeriod(), cred.getAlgorithm());
                        final OTPSecretData secretData = new OTPSecretData(cred.getHashedSaltedValue());
                        cred.setCredentialData(JsonSerialization.writeValueAsString((Object)credentialData2));
                        cred.setSecretData(JsonSerialization.writeValueAsString((Object)secretData));
                        cred.setPriority(Integer.valueOf(20));
                        cred.setType("otp");
                    }
                }
                catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                }
            }
        }
    }
    
    public static void renameRealm(final RealmModel realm, final String name) {
        if (name.equals(realm.getName())) {
            return;
        }
        final String oldName = realm.getName();
        final ClientModel masterApp = realm.getMasterAdminClient();
        masterApp.setClientId(KeycloakModelUtils.getMasterRealmAdminApplicationClientId(name));
        realm.setName(name);
        final ClientModel adminClient = realm.getClientByClientId("security-admin-console");
        if (adminClient != null) {
            if (adminClient.getBaseUrl() != null) {
                adminClient.setBaseUrl(adminClient.getBaseUrl().replace("/admin/" + oldName + "/", "/admin/" + name + "/"));
            }
            final Set<String> adminRedirectUris = new HashSet<String>();
            for (final String r : adminClient.getRedirectUris()) {
                adminRedirectUris.add(replace(r, "/admin/" + oldName + "/", "/admin/" + name + "/"));
            }
            adminClient.setRedirectUris((Set)adminRedirectUris);
        }
        final ClientModel accountClient = realm.getClientByClientId("account");
        if (accountClient != null) {
            if (accountClient.getBaseUrl() != null) {
                accountClient.setBaseUrl(accountClient.getBaseUrl().replace("/realms/" + oldName + "/", "/realms/" + name + "/"));
            }
            final Set<String> accountRedirectUris = new HashSet<String>();
            for (final String r2 : accountClient.getRedirectUris()) {
                accountRedirectUris.add(replace(r2, "/realms/" + oldName + "/", "/realms/" + name + "/"));
            }
            accountClient.setRedirectUris((Set)accountRedirectUris);
        }
    }
    
    private static String replace(final String url, final String target, final String replacement) {
        return (url != null) ? url.replace(target, replacement) : null;
    }
    
    public static void updateRealm(final RealmRepresentation rep, final RealmModel realm, final KeycloakSession session) {
        if (rep.getRealm() != null) {
            renameRealm(realm, rep.getRealm());
        }
        if (rep.getAttributes() != null) {
            final Set<String> attrsToRemove = new HashSet<String>(realm.getAttributes().keySet());
            attrsToRemove.removeAll(rep.getAttributes().keySet());
            for (final Map.Entry<String, String> entry : rep.getAttributes().entrySet()) {
                realm.setAttribute((String)entry.getKey(), (String)entry.getValue());
            }
            for (final String attr : attrsToRemove) {
                realm.removeAttribute(attr);
            }
        }
        if (rep.getDisplayName() != null) {
            realm.setDisplayName(rep.getDisplayName());
        }
        if (rep.getDisplayNameHtml() != null) {
            realm.setDisplayNameHtml(rep.getDisplayNameHtml());
        }
        if (rep.isEnabled() != null) {
            realm.setEnabled((boolean)rep.isEnabled());
        }
        if (rep.isUserManagedAccessAllowed() != null) {
            realm.setUserManagedAccessAllowed((boolean)rep.isUserManagedAccessAllowed());
        }
        if (rep.isBruteForceProtected() != null) {
            realm.setBruteForceProtected((boolean)rep.isBruteForceProtected());
        }
        if (rep.isPermanentLockout() != null) {
            realm.setPermanentLockout((boolean)rep.isPermanentLockout());
        }
        if (rep.getMaxFailureWaitSeconds() != null) {
            realm.setMaxFailureWaitSeconds((int)rep.getMaxFailureWaitSeconds());
        }
        if (rep.getMinimumQuickLoginWaitSeconds() != null) {
            realm.setMinimumQuickLoginWaitSeconds((int)rep.getMinimumQuickLoginWaitSeconds());
        }
        if (rep.getWaitIncrementSeconds() != null) {
            realm.setWaitIncrementSeconds((int)rep.getWaitIncrementSeconds());
        }
        if (rep.getQuickLoginCheckMilliSeconds() != null) {
            realm.setQuickLoginCheckMilliSeconds((long)rep.getQuickLoginCheckMilliSeconds());
        }
        if (rep.getMaxDeltaTimeSeconds() != null) {
            realm.setMaxDeltaTimeSeconds((int)rep.getMaxDeltaTimeSeconds());
        }
        if (rep.getFailureFactor() != null) {
            realm.setFailureFactor((int)rep.getFailureFactor());
        }
        if (rep.isRegistrationAllowed() != null) {
            realm.setRegistrationAllowed((boolean)rep.isRegistrationAllowed());
        }
        if (rep.isRegistrationEmailAsUsername() != null) {
            realm.setRegistrationEmailAsUsername((boolean)rep.isRegistrationEmailAsUsername());
        }
        if (rep.isRememberMe() != null) {
            realm.setRememberMe((boolean)rep.isRememberMe());
        }
        if (rep.isVerifyEmail() != null) {
            realm.setVerifyEmail((boolean)rep.isVerifyEmail());
        }
        if (rep.isLoginWithEmailAllowed() != null) {
            realm.setLoginWithEmailAllowed((boolean)rep.isLoginWithEmailAllowed());
        }
        if (rep.isDuplicateEmailsAllowed() != null) {
            realm.setDuplicateEmailsAllowed((boolean)rep.isDuplicateEmailsAllowed());
        }
        if (rep.isResetPasswordAllowed() != null) {
            realm.setResetPasswordAllowed((boolean)rep.isResetPasswordAllowed());
        }
        if (rep.isEditUsernameAllowed() != null) {
            realm.setEditUsernameAllowed((boolean)rep.isEditUsernameAllowed());
        }
        if (rep.getSslRequired() != null) {
            realm.setSslRequired(SslRequired.valueOf(rep.getSslRequired().toUpperCase()));
        }
        if (rep.getAccessCodeLifespan() != null) {
            realm.setAccessCodeLifespan((int)rep.getAccessCodeLifespan());
        }
        if (rep.getAccessCodeLifespanUserAction() != null) {
            realm.setAccessCodeLifespanUserAction((int)rep.getAccessCodeLifespanUserAction());
        }
        if (rep.getAccessCodeLifespanLogin() != null) {
            realm.setAccessCodeLifespanLogin((int)rep.getAccessCodeLifespanLogin());
        }
        if (rep.getActionTokenGeneratedByAdminLifespan() != null) {
            realm.setActionTokenGeneratedByAdminLifespan((int)rep.getActionTokenGeneratedByAdminLifespan());
        }
        if (rep.getActionTokenGeneratedByUserLifespan() != null) {
            realm.setActionTokenGeneratedByUserLifespan((int)rep.getActionTokenGeneratedByUserLifespan());
        }
        if (rep.getNotBefore() != null) {
            realm.setNotBefore((int)rep.getNotBefore());
        }
        if (rep.getDefaultSignatureAlgorithm() != null) {
            realm.setDefaultSignatureAlgorithm(rep.getDefaultSignatureAlgorithm());
        }
        if (rep.getRevokeRefreshToken() != null) {
            realm.setRevokeRefreshToken((boolean)rep.getRevokeRefreshToken());
        }
        if (rep.getRefreshTokenMaxReuse() != null) {
            realm.setRefreshTokenMaxReuse((int)rep.getRefreshTokenMaxReuse());
        }
        if (rep.getAccessTokenLifespan() != null) {
            realm.setAccessTokenLifespan((int)rep.getAccessTokenLifespan());
        }
        if (rep.getAccessTokenLifespanForImplicitFlow() != null) {
            realm.setAccessTokenLifespanForImplicitFlow((int)rep.getAccessTokenLifespanForImplicitFlow());
        }
        if (rep.getSsoSessionIdleTimeout() != null) {
            realm.setSsoSessionIdleTimeout((int)rep.getSsoSessionIdleTimeout());
        }
        if (rep.getSsoSessionMaxLifespan() != null) {
            realm.setSsoSessionMaxLifespan((int)rep.getSsoSessionMaxLifespan());
        }
        if (rep.getSsoSessionIdleTimeoutRememberMe() != null) {
            realm.setSsoSessionIdleTimeoutRememberMe((int)rep.getSsoSessionIdleTimeoutRememberMe());
        }
        if (rep.getSsoSessionMaxLifespanRememberMe() != null) {
            realm.setSsoSessionMaxLifespanRememberMe((int)rep.getSsoSessionMaxLifespanRememberMe());
        }
        if (rep.getOfflineSessionIdleTimeout() != null) {
            realm.setOfflineSessionIdleTimeout((int)rep.getOfflineSessionIdleTimeout());
        }
        if (rep.getOfflineSessionMaxLifespanEnabled() != null) {
            realm.setOfflineSessionMaxLifespanEnabled((boolean)rep.getOfflineSessionMaxLifespanEnabled());
        }
        if (rep.getOfflineSessionMaxLifespan() != null) {
            realm.setOfflineSessionMaxLifespan((int)rep.getOfflineSessionMaxLifespan());
        }
        if (rep.getRequiredCredentials() != null) {
            realm.updateRequiredCredentials(rep.getRequiredCredentials());
        }
        if (rep.getLoginTheme() != null) {
            realm.setLoginTheme(rep.getLoginTheme());
        }
        if (rep.getAccountTheme() != null) {
            realm.setAccountTheme(rep.getAccountTheme());
        }
        if (rep.getAdminTheme() != null) {
            realm.setAdminTheme(rep.getAdminTheme());
        }
        if (rep.getEmailTheme() != null) {
            realm.setEmailTheme(rep.getEmailTheme());
        }
        if (rep.isEventsEnabled() != null) {
            realm.setEventsEnabled((boolean)rep.isEventsEnabled());
        }
        if (rep.getEventsExpiration() != null) {
            realm.setEventsExpiration((long)rep.getEventsExpiration());
        }
        if (rep.getEventsListeners() != null) {
            realm.setEventsListeners((Set)new HashSet(rep.getEventsListeners()));
        }
        if (rep.getEnabledEventTypes() != null) {
            realm.setEnabledEventTypes((Set)new HashSet(rep.getEnabledEventTypes()));
        }
        if (rep.isAdminEventsEnabled() != null) {
            realm.setAdminEventsEnabled((boolean)rep.isAdminEventsEnabled());
        }
        if (rep.isAdminEventsDetailsEnabled() != null) {
            realm.setAdminEventsDetailsEnabled((boolean)rep.isAdminEventsDetailsEnabled());
        }
        if (rep.getPasswordPolicy() != null) {
            realm.setPasswordPolicy(PasswordPolicy.parse(session, rep.getPasswordPolicy()));
        }
        if (rep.getOtpPolicyType() != null) {
            realm.setOTPPolicy(toPolicy(rep));
        }
        if (rep.getDefaultRoles() != null) {
            realm.updateDefaultRoles((String[])rep.getDefaultRoles().toArray(new String[rep.getDefaultRoles().size()]));
        }
        WebAuthnPolicy webAuthnPolicy = getWebAuthnPolicyTwoFactor(rep);
        realm.setWebAuthnPolicy(webAuthnPolicy);
        webAuthnPolicy = getWebAuthnPolicyPasswordless(rep);
        realm.setWebAuthnPolicyPasswordless(webAuthnPolicy);
        if (rep.getSmtpServer() != null) {
            final Map<String, String> config = new HashMap<String, String>(rep.getSmtpServer());
            if (rep.getSmtpServer().containsKey("password") && "**********".equals(rep.getSmtpServer().get("password"))) {
                final String passwordValue = (realm.getSmtpConfig() != null) ? realm.getSmtpConfig().get("password") : null;
                config.put("password", passwordValue);
            }
            realm.setSmtpConfig((Map)config);
        }
        if (rep.getBrowserSecurityHeaders() != null) {
            realm.setBrowserSecurityHeaders(rep.getBrowserSecurityHeaders());
        }
        if (rep.isInternationalizationEnabled() != null) {
            realm.setInternationalizationEnabled((boolean)rep.isInternationalizationEnabled());
        }
        if (rep.getSupportedLocales() != null) {
            realm.setSupportedLocales((Set)new HashSet(rep.getSupportedLocales()));
        }
        if (rep.getDefaultLocale() != null) {
            realm.setDefaultLocale(rep.getDefaultLocale());
        }
        if (rep.getBrowserFlow() != null) {
            realm.setBrowserFlow(realm.getFlowByAlias(rep.getBrowserFlow()));
        }
        if (rep.getRegistrationFlow() != null) {
            realm.setRegistrationFlow(realm.getFlowByAlias(rep.getRegistrationFlow()));
        }
        if (rep.getDirectGrantFlow() != null) {
            realm.setDirectGrantFlow(realm.getFlowByAlias(rep.getDirectGrantFlow()));
        }
        if (rep.getResetCredentialsFlow() != null) {
            realm.setResetCredentialsFlow(realm.getFlowByAlias(rep.getResetCredentialsFlow()));
        }
        if (rep.getClientAuthenticationFlow() != null) {
            realm.setClientAuthenticationFlow(realm.getFlowByAlias(rep.getClientAuthenticationFlow()));
        }
        if (rep.getDockerAuthenticationFlow() != null) {
            realm.setDockerAuthenticationFlow(realm.getFlowByAlias(rep.getDockerAuthenticationFlow()));
        }
    }
    
    public static ComponentModel convertFedProviderToComponent(final String realmId, final UserFederationProviderRepresentation fedModel) {
        final UserStorageProviderModel model = new UserStorageProviderModel();
        model.setId(fedModel.getId());
        model.setName(fedModel.getDisplayName());
        model.setParentId(realmId);
        model.setProviderId(fedModel.getProviderName());
        model.setProviderType(UserStorageProvider.class.getName());
        model.setFullSyncPeriod(fedModel.getFullSyncPeriod());
        model.setPriority(fedModel.getPriority());
        model.setChangedSyncPeriod(fedModel.getChangedSyncPeriod());
        model.setLastSync(fedModel.getLastSync());
        if (fedModel.getConfig() != null) {
            for (final Map.Entry<String, String> entry : fedModel.getConfig().entrySet()) {
                model.getConfig().putSingle((Object)entry.getKey(), (Object)entry.getValue());
            }
        }
        return (ComponentModel)model;
    }
    
    public static ComponentModel convertFedMapperToComponent(final RealmModel realm, final ComponentModel parent, final UserFederationMapperRepresentation rep, final String newMapperType) {
        final ComponentModel mapper = new ComponentModel();
        mapper.setId(rep.getId());
        mapper.setName(rep.getName());
        mapper.setProviderId(rep.getFederationMapperType());
        mapper.setProviderType(newMapperType);
        mapper.setParentId(parent.getId());
        if (rep.getConfig() != null) {
            for (final Map.Entry<String, String> entry : rep.getConfig().entrySet()) {
                mapper.getConfig().putSingle((Object)entry.getKey(), (Object)entry.getValue());
            }
        }
        return mapper;
    }
    
    public static void createRole(final RealmModel newRealm, final RoleRepresentation roleRep) {
        final RoleModel role = (roleRep.getId() != null) ? newRealm.addRole(roleRep.getId(), roleRep.getName()) : newRealm.addRole(roleRep.getName());
        if (roleRep.getDescription() != null) {
            role.setDescription(roleRep.getDescription());
        }
        if (roleRep.getAttributes() != null) {
            for (final Map.Entry<String, List<String>> attribute : roleRep.getAttributes().entrySet()) {
                role.setAttribute((String)attribute.getKey(), (Collection)attribute.getValue());
            }
        }
    }
    
    private static void addComposites(final RoleModel role, final RoleRepresentation roleRep, final RealmModel realm) {
        if (roleRep.getComposites() == null) {
            return;
        }
        if (roleRep.getComposites().getRealm() != null) {
            for (final String roleStr : roleRep.getComposites().getRealm()) {
                final RoleModel realmRole = realm.getRole(roleStr);
                if (realmRole == null) {
                    throw new RuntimeException("Unable to find composite realm role: " + roleStr);
                }
                role.addCompositeRole(realmRole);
            }
        }
        if (roleRep.getComposites().getClient() != null) {
            for (final Map.Entry<String, List<String>> entry : roleRep.getComposites().getClient().entrySet()) {
                final ClientModel client = realm.getClientByClientId((String)entry.getKey());
                if (client == null) {
                    throw new RuntimeException("App doesn't exist in role definitions: " + roleRep.getName());
                }
                for (final String roleStr2 : entry.getValue()) {
                    final RoleModel clientRole = client.getRole(roleStr2);
                    if (clientRole == null) {
                        throw new RuntimeException("Unable to find composite client role: " + roleStr2);
                    }
                    role.addCompositeRole(clientRole);
                }
            }
        }
    }
    
    private static Map<String, ClientModel> createClients(final KeycloakSession session, final RealmRepresentation rep, final RealmModel realm, final Map<String, String> mappedFlows) {
        final Map<String, ClientModel> appMap = new HashMap<String, ClientModel>();
        for (final ClientRepresentation resourceRep : rep.getClients()) {
            final ClientModel app = createClient(session, realm, resourceRep, false, mappedFlows);
            appMap.put(app.getClientId(), app);
            final RuntimeException ex;
            final ClientModel clientModel;
            ClientValidationUtil.validate(session, app, false, c -> {
                new RuntimeException("Invalid client " + clientModel.getClientId() + ": " + c.getError());
                throw ex;
            });
        }
        return appMap;
    }
    
    public static ClientModel createClient(final KeycloakSession session, final RealmModel realm, final ClientRepresentation resourceRep, final boolean addDefaultRoles) {
        return createClient(session, realm, resourceRep, addDefaultRoles, null);
    }
    
    private static ClientModel createClient(final KeycloakSession session, final RealmModel realm, final ClientRepresentation resourceRep, final boolean addDefaultRoles, final Map<String, String> mappedFlows) {
        RepresentationToModel.logger.debugv("Create client: {0}", (Object)resourceRep.getClientId());
        final ClientModel client = (resourceRep.getId() != null) ? realm.addClient(resourceRep.getId(), resourceRep.getClientId()) : realm.addClient(resourceRep.getClientId());
        if (resourceRep.getName() != null) {
            client.setName(resourceRep.getName());
        }
        if (resourceRep.getDescription() != null) {
            client.setDescription(resourceRep.getDescription());
        }
        if (resourceRep.isEnabled() != null) {
            client.setEnabled((boolean)resourceRep.isEnabled());
        }
        if (resourceRep.isAlwaysDisplayInConsole() != null) {
            client.setAlwaysDisplayInConsole((boolean)resourceRep.isAlwaysDisplayInConsole());
        }
        client.setManagementUrl(resourceRep.getAdminUrl());
        if (resourceRep.isSurrogateAuthRequired() != null) {
            client.setSurrogateAuthRequired((boolean)resourceRep.isSurrogateAuthRequired());
        }
        if (resourceRep.getRootUrl() != null) {
            client.setRootUrl(resourceRep.getRootUrl());
        }
        if (resourceRep.getBaseUrl() != null) {
            client.setBaseUrl(resourceRep.getBaseUrl());
        }
        if (resourceRep.isBearerOnly() != null) {
            client.setBearerOnly((boolean)resourceRep.isBearerOnly());
        }
        if (resourceRep.isConsentRequired() != null) {
            client.setConsentRequired((boolean)resourceRep.isConsentRequired());
        }
        if (resourceRep.isDirectGrantsOnly() != null) {
            RepresentationToModel.logger.warn((Object)"Using deprecated 'directGrantsOnly' configuration in JSON representation. It will be removed in future versions");
            client.setStandardFlowEnabled(!resourceRep.isDirectGrantsOnly());
            client.setDirectAccessGrantsEnabled((boolean)resourceRep.isDirectGrantsOnly());
        }
        if (resourceRep.isStandardFlowEnabled() != null) {
            client.setStandardFlowEnabled((boolean)resourceRep.isStandardFlowEnabled());
        }
        if (resourceRep.isImplicitFlowEnabled() != null) {
            client.setImplicitFlowEnabled((boolean)resourceRep.isImplicitFlowEnabled());
        }
        if (resourceRep.isDirectAccessGrantsEnabled() != null) {
            client.setDirectAccessGrantsEnabled((boolean)resourceRep.isDirectAccessGrantsEnabled());
        }
        if (resourceRep.isServiceAccountsEnabled() != null) {
            client.setServiceAccountsEnabled((boolean)resourceRep.isServiceAccountsEnabled());
        }
        if (resourceRep.isPublicClient() != null) {
            client.setPublicClient((boolean)resourceRep.isPublicClient());
        }
        if (resourceRep.isFrontchannelLogout() != null) {
            client.setFrontchannelLogout((boolean)resourceRep.isFrontchannelLogout());
        }
        if (resourceRep.getProtocol() != null) {
            client.setProtocol(resourceRep.getProtocol());
        }
        else {
            client.setProtocol("openid-connect");
        }
        if (resourceRep.getNodeReRegistrationTimeout() != null) {
            client.setNodeReRegistrationTimeout((int)resourceRep.getNodeReRegistrationTimeout());
        }
        else {
            client.setNodeReRegistrationTimeout(-1);
        }
        if (resourceRep.getNotBefore() != null) {
            client.setNotBefore((int)resourceRep.getNotBefore());
        }
        if (resourceRep.getClientAuthenticatorType() != null) {
            client.setClientAuthenticatorType(resourceRep.getClientAuthenticatorType());
        }
        else {
            client.setClientAuthenticatorType(KeycloakModelUtils.getDefaultClientAuthenticatorType());
        }
        client.setSecret(resourceRep.getSecret());
        if (client.getSecret() == null) {
            KeycloakModelUtils.generateSecret(client);
        }
        if (resourceRep.getAttributes() != null) {
            for (final Map.Entry<String, String> entry : resourceRep.getAttributes().entrySet()) {
                client.setAttribute((String)entry.getKey(), (String)entry.getValue());
            }
        }
        if (resourceRep.getAuthenticationFlowBindingOverrides() != null) {
            for (final Map.Entry<String, String> entry : resourceRep.getAuthenticationFlowBindingOverrides().entrySet()) {
                if (entry.getValue() != null) {
                    if (entry.getValue().trim().equals("")) {
                        continue;
                    }
                    String flowId = entry.getValue();
                    if (mappedFlows != null && mappedFlows.containsKey(flowId)) {
                        flowId = mappedFlows.get(flowId);
                    }
                    if (client.getRealm().getAuthenticationFlowById(flowId) == null) {
                        throw new RuntimeException("Unable to resolve auth flow binding override for: " + entry.getKey());
                    }
                    client.setAuthenticationFlowBindingOverride((String)entry.getKey(), flowId);
                }
            }
        }
        if (resourceRep.getRedirectUris() != null) {
            for (final String redirectUri : resourceRep.getRedirectUris()) {
                client.addRedirectUri(redirectUri);
            }
        }
        if (resourceRep.getWebOrigins() != null) {
            for (final String webOrigin : resourceRep.getWebOrigins()) {
                RepresentationToModel.logger.debugv("Client: {0} webOrigin: {1}", (Object)resourceRep.getClientId(), (Object)webOrigin);
                client.addWebOrigin(webOrigin);
            }
        }
        else if (resourceRep.getRedirectUris() != null) {
            final Set<String> origins = new HashSet<String>();
            for (final String redirectUri2 : resourceRep.getRedirectUris()) {
                RepresentationToModel.logger.debugv("add redirect-uri to origin: {0}", (Object)redirectUri2);
                if (redirectUri2.startsWith("http")) {
                    final String origin = UriUtils.getOrigin(redirectUri2);
                    RepresentationToModel.logger.debugv("adding default client origin: {0}", (Object)origin);
                    origins.add(origin);
                }
            }
            if (origins.size() > 0) {
                client.setWebOrigins((Set)origins);
            }
        }
        if (resourceRep.getRegisteredNodes() != null) {
            for (final Map.Entry<String, Integer> entry2 : resourceRep.getRegisteredNodes().entrySet()) {
                client.registerNode((String)entry2.getKey(), (int)entry2.getValue());
            }
        }
        if (addDefaultRoles && resourceRep.getDefaultRoles() != null) {
            client.updateDefaultRoles(resourceRep.getDefaultRoles());
        }
        if (resourceRep.getProtocolMappers() != null) {
            final Set<ProtocolMapperModel> mappers = (Set<ProtocolMapperModel>)client.getProtocolMappers();
            for (final ProtocolMapperModel mapper : mappers) {
                client.removeProtocolMapper(mapper);
            }
            for (final ProtocolMapperRepresentation mapper2 : resourceRep.getProtocolMappers()) {
                client.addProtocolMapper(toModel(mapper2));
            }
            MigrationUtils.updateProtocolMappers((ProtocolMapperContainerModel)client);
        }
        if (resourceRep.getClientTemplate() != null) {
            final String clientTemplateName = KeycloakModelUtils.convertClientScopeName(resourceRep.getClientTemplate());
            addClientScopeToClient(realm, client, clientTemplateName, true);
        }
        if (resourceRep.getDefaultClientScopes() != null || resourceRep.getOptionalClientScopes() != null) {
            for (final ClientScopeModel clientScope : client.getClientScopes(true, false).values()) {
                client.removeClientScope(clientScope);
            }
            for (final ClientScopeModel clientScope : client.getClientScopes(false, false).values()) {
                client.removeClientScope(clientScope);
            }
        }
        if (resourceRep.getDefaultClientScopes() != null) {
            for (final String clientScopeName : resourceRep.getDefaultClientScopes()) {
                addClientScopeToClient(realm, client, clientScopeName, true);
            }
        }
        if (resourceRep.getOptionalClientScopes() != null) {
            for (final String clientScopeName : resourceRep.getOptionalClientScopes()) {
                addClientScopeToClient(realm, client, clientScopeName, false);
            }
        }
        if (resourceRep.isFullScopeAllowed() != null) {
            client.setFullScopeAllowed((boolean)resourceRep.isFullScopeAllowed());
        }
        else {
            client.setFullScopeAllowed(!client.isConsentRequired());
        }
        client.updateClient();
        resourceRep.setId(client.getId());
        return client;
    }
    
    private static void addClientScopeToClient(final RealmModel realm, final ClientModel client, final String clientScopeName, final boolean defaultScope) {
        final ClientScopeModel clientScope = KeycloakModelUtils.getClientScopeByName(realm, clientScopeName);
        if (clientScope != null) {
            client.addClientScope(clientScope, defaultScope);
        }
        else {
            RepresentationToModel.logger.warnf("Referenced client scope '%s' doesn't exists. Ignoring", (Object)clientScopeName);
        }
    }
    
    public static void updateClient(final ClientRepresentation rep, final ClientModel resource) {
        if (rep.getClientId() != null) {
            resource.setClientId(rep.getClientId());
        }
        if (rep.getName() != null) {
            resource.setName(rep.getName());
        }
        if (rep.getDescription() != null) {
            resource.setDescription(rep.getDescription());
        }
        if (rep.isEnabled() != null) {
            resource.setEnabled((boolean)rep.isEnabled());
        }
        if (rep.isAlwaysDisplayInConsole() != null) {
            resource.setAlwaysDisplayInConsole((boolean)rep.isAlwaysDisplayInConsole());
        }
        if (rep.isBearerOnly() != null) {
            resource.setBearerOnly((boolean)rep.isBearerOnly());
        }
        if (rep.isConsentRequired() != null) {
            resource.setConsentRequired((boolean)rep.isConsentRequired());
        }
        if (rep.isStandardFlowEnabled() != null) {
            resource.setStandardFlowEnabled((boolean)rep.isStandardFlowEnabled());
        }
        if (rep.isImplicitFlowEnabled() != null) {
            resource.setImplicitFlowEnabled((boolean)rep.isImplicitFlowEnabled());
        }
        if (rep.isDirectAccessGrantsEnabled() != null) {
            resource.setDirectAccessGrantsEnabled((boolean)rep.isDirectAccessGrantsEnabled());
        }
        if (rep.isServiceAccountsEnabled() != null) {
            resource.setServiceAccountsEnabled((boolean)rep.isServiceAccountsEnabled());
        }
        if (rep.isPublicClient() != null) {
            resource.setPublicClient((boolean)rep.isPublicClient());
        }
        if (rep.isFullScopeAllowed() != null) {
            resource.setFullScopeAllowed((boolean)rep.isFullScopeAllowed());
        }
        if (rep.isFrontchannelLogout() != null) {
            resource.setFrontchannelLogout((boolean)rep.isFrontchannelLogout());
        }
        if (rep.getRootUrl() != null) {
            resource.setRootUrl(rep.getRootUrl());
        }
        if (rep.getAdminUrl() != null) {
            resource.setManagementUrl(rep.getAdminUrl());
        }
        if (rep.getBaseUrl() != null) {
            resource.setBaseUrl(rep.getBaseUrl());
        }
        if (rep.isSurrogateAuthRequired() != null) {
            resource.setSurrogateAuthRequired((boolean)rep.isSurrogateAuthRequired());
        }
        if (rep.getNodeReRegistrationTimeout() != null) {
            resource.setNodeReRegistrationTimeout((int)rep.getNodeReRegistrationTimeout());
        }
        if (rep.getClientAuthenticatorType() != null) {
            resource.setClientAuthenticatorType(rep.getClientAuthenticatorType());
        }
        if (rep.getProtocol() != null) {
            resource.setProtocol(rep.getProtocol());
        }
        if (rep.getAttributes() != null) {
            for (final Map.Entry<String, String> entry : rep.getAttributes().entrySet()) {
                resource.setAttribute((String)entry.getKey(), (String)entry.getValue());
            }
        }
        if (rep.getAttributes() != null) {
            for (final Map.Entry<String, String> entry : removeEmptyString(rep.getAttributes()).entrySet()) {
                resource.setAttribute((String)entry.getKey(), (String)entry.getValue());
            }
        }
        if (rep.getAuthenticationFlowBindingOverrides() != null) {
            for (final Map.Entry<String, String> entry : rep.getAuthenticationFlowBindingOverrides().entrySet()) {
                if (entry.getValue() == null || entry.getValue().trim().equals("")) {
                    resource.removeAuthenticationFlowBindingOverride((String)entry.getKey());
                }
                else {
                    final String flowId = entry.getValue();
                    if (resource.getRealm().getAuthenticationFlowById(flowId) == null) {
                        throw new RuntimeException("Unable to resolve auth flow binding override for: " + entry.getKey());
                    }
                    resource.setAuthenticationFlowBindingOverride((String)entry.getKey(), (String)entry.getValue());
                }
            }
        }
        if (rep.getNotBefore() != null) {
            resource.setNotBefore((int)rep.getNotBefore());
        }
        if (rep.getDefaultRoles() != null) {
            resource.updateDefaultRoles(rep.getDefaultRoles());
        }
        final List<String> redirectUris = (List<String>)rep.getRedirectUris();
        if (redirectUris != null) {
            resource.setRedirectUris((Set)new HashSet(redirectUris));
        }
        final List<String> webOrigins = (List<String>)rep.getWebOrigins();
        if (webOrigins != null) {
            resource.setWebOrigins((Set)new HashSet(webOrigins));
        }
        if (rep.getRegisteredNodes() != null) {
            for (final Map.Entry<String, Integer> entry2 : rep.getRegisteredNodes().entrySet()) {
                resource.registerNode((String)entry2.getKey(), (int)entry2.getValue());
            }
        }
        if (rep.getSecret() != null) {
            resource.setSecret(rep.getSecret());
        }
        resource.updateClient();
    }
    
    public static void updateClientProtocolMappers(final ClientRepresentation rep, final ClientModel resource) {
        if (rep.getProtocolMappers() != null) {
            final Map<String, ProtocolMapperModel> existingProtocolMappers = new HashMap<String, ProtocolMapperModel>();
            for (final ProtocolMapperModel existingProtocolMapper : resource.getProtocolMappers()) {
                existingProtocolMappers.put(generateProtocolNameKey(existingProtocolMapper.getProtocol(), existingProtocolMapper.getName()), existingProtocolMapper);
            }
            for (final ProtocolMapperRepresentation protocolMapperRepresentation : rep.getProtocolMappers()) {
                final String protocolNameKey = generateProtocolNameKey(protocolMapperRepresentation.getProtocol(), protocolMapperRepresentation.getName());
                final ProtocolMapperModel existingMapper = existingProtocolMappers.get(protocolNameKey);
                if (existingMapper != null) {
                    final ProtocolMapperModel updatedProtocolMapperModel = toModel(protocolMapperRepresentation);
                    updatedProtocolMapperModel.setId(existingMapper.getId());
                    resource.updateProtocolMapper(updatedProtocolMapperModel);
                    existingProtocolMappers.remove(protocolNameKey);
                }
                else {
                    resource.addProtocolMapper(toModel(protocolMapperRepresentation));
                }
            }
            for (final Map.Entry<String, ProtocolMapperModel> entryToDelete : existingProtocolMappers.entrySet()) {
                resource.removeProtocolMapper((ProtocolMapperModel)entryToDelete.getValue());
            }
        }
    }
    
    private static String generateProtocolNameKey(final String protocol, final String name) {
        return String.format("%s%%%s", protocol, name);
    }
    
    private static Map<String, ClientScopeModel> createClientScopes(final KeycloakSession session, final List<ClientScopeRepresentation> clientScopes, final RealmModel realm) {
        final Map<String, ClientScopeModel> appMap = new HashMap<String, ClientScopeModel>();
        for (final ClientScopeRepresentation resourceRep : clientScopes) {
            final ClientScopeModel app = createClientScope(session, realm, resourceRep);
            appMap.put(app.getName(), app);
        }
        return appMap;
    }
    
    public static ClientScopeModel createClientScope(final KeycloakSession session, final RealmModel realm, final ClientScopeRepresentation resourceRep) {
        RepresentationToModel.logger.debug((Object)("Create client scope: {0}" + resourceRep.getName()));
        final ClientScopeModel clientScope = (resourceRep.getId() != null) ? realm.addClientScope(resourceRep.getId(), resourceRep.getName()) : realm.addClientScope(resourceRep.getName());
        if (resourceRep.getName() != null) {
            clientScope.setName(resourceRep.getName());
        }
        if (resourceRep.getDescription() != null) {
            clientScope.setDescription(resourceRep.getDescription());
        }
        if (resourceRep.getProtocol() != null) {
            clientScope.setProtocol(resourceRep.getProtocol());
        }
        if (resourceRep.getProtocolMappers() != null) {
            final Set<ProtocolMapperModel> mappers = (Set<ProtocolMapperModel>)clientScope.getProtocolMappers();
            for (final ProtocolMapperModel mapper : mappers) {
                clientScope.removeProtocolMapper(mapper);
            }
            for (final ProtocolMapperRepresentation mapper2 : resourceRep.getProtocolMappers()) {
                clientScope.addProtocolMapper(toModel(mapper2));
            }
            MigrationUtils.updateProtocolMappers((ProtocolMapperContainerModel)clientScope);
        }
        if (resourceRep.getAttributes() != null) {
            for (final Map.Entry<String, String> entry : resourceRep.getAttributes().entrySet()) {
                clientScope.setAttribute((String)entry.getKey(), (String)entry.getValue());
            }
        }
        return clientScope;
    }
    
    public static void updateClientScope(final ClientScopeRepresentation rep, final ClientScopeModel resource) {
        if (rep.getName() != null) {
            resource.setName(rep.getName());
        }
        if (rep.getDescription() != null) {
            resource.setDescription(rep.getDescription());
        }
        if (rep.getProtocol() != null) {
            resource.setProtocol(rep.getProtocol());
        }
        if (rep.getAttributes() != null) {
            for (final Map.Entry<String, String> entry : rep.getAttributes().entrySet()) {
                resource.setAttribute((String)entry.getKey(), (String)entry.getValue());
            }
        }
    }
    
    public static long getClaimsMask(final ClaimRepresentation rep) {
        long mask = 1023L;
        if (rep.getAddress()) {
            mask |= 0x100L;
        }
        else {
            mask &= 0xFFFFFFFFFFFFFEFFL;
        }
        if (rep.getEmail()) {
            mask |= 0x20L;
        }
        else {
            mask &= 0xFFFFFFFFFFFFFFDFL;
        }
        if (rep.getGender()) {
            mask |= 0x40L;
        }
        else {
            mask &= 0xFFFFFFFFFFFFFFBFL;
        }
        if (rep.getLocale()) {
            mask |= 0x80L;
        }
        else {
            mask &= 0xFFFFFFFFFFFFFF7FL;
        }
        if (rep.getName()) {
            mask |= 0x1L;
        }
        else {
            mask &= 0xFFFFFFFFFFFFFFFEL;
        }
        if (rep.getPhone()) {
            mask |= 0x200L;
        }
        else {
            mask &= 0xFFFFFFFFFFFFFDFFL;
        }
        if (rep.getPicture()) {
            mask |= 0x8L;
        }
        else {
            mask &= 0xFFFFFFFFFFFFFFF7L;
        }
        if (rep.getProfile()) {
            mask |= 0x4L;
        }
        else {
            mask &= 0xFFFFFFFFFFFFFFFBL;
        }
        if (rep.getUsername()) {
            mask |= 0x2L;
        }
        else {
            mask &= 0xFFFFFFFFFFFFFFFDL;
        }
        if (rep.getWebsite()) {
            mask |= 0x10L;
        }
        else {
            mask &= 0xFFFFFFFFFFFFFFEFL;
        }
        return mask;
    }
    
    public static void createClientScopeMappings(final RealmModel realm, final ClientModel clientModel, final List<ScopeMappingRepresentation> mappings) {
        for (final ScopeMappingRepresentation mapping : mappings) {
            final ScopeContainerModel scopeContainer = getScopeContainerHavingScope(realm, mapping);
            for (final String roleString : mapping.getRoles()) {
                RoleModel role = clientModel.getRole(roleString.trim());
                if (role == null) {
                    role = clientModel.addRole(roleString.trim());
                }
                scopeContainer.addScopeMapping(role);
            }
        }
    }
    
    private static ScopeContainerModel getScopeContainerHavingScope(final RealmModel realm, final ScopeMappingRepresentation scope) {
        if (scope.getClient() != null) {
            final ClientModel client = realm.getClientByClientId(scope.getClient());
            if (client == null) {
                throw new RuntimeException("Unknown client specification in scope mappings: " + scope.getClient());
            }
            return (ScopeContainerModel)client;
        }
        else if (scope.getClientScope() != null) {
            final ClientScopeModel clientScope = KeycloakModelUtils.getClientScopeByName(realm, scope.getClientScope());
            if (clientScope == null) {
                throw new RuntimeException("Unknown clientScope specification in scope mappings: " + scope.getClientScope());
            }
            return (ScopeContainerModel)clientScope;
        }
        else {
            if (scope.getClientTemplate() == null) {
                throw new RuntimeException("Either client or clientScope needs to be specified in scope mappings");
            }
            final String templateName = KeycloakModelUtils.convertClientScopeName(scope.getClientTemplate());
            final ClientScopeModel clientTemplate = KeycloakModelUtils.getClientScopeByName(realm, templateName);
            if (clientTemplate == null) {
                throw new RuntimeException("Unknown clientScope specification in scope mappings: " + templateName);
            }
            return (ScopeContainerModel)clientTemplate;
        }
    }
    
    public static UserModel createUser(final KeycloakSession session, final RealmModel newRealm, final UserRepresentation userRep) {
        convertDeprecatedSocialProviders(userRep);
        final UserModel user = session.userLocalStorage().addUser(newRealm, userRep.getId(), userRep.getUsername(), false, false);
        user.setEnabled(userRep.isEnabled() != null && userRep.isEnabled());
        user.setCreatedTimestamp(userRep.getCreatedTimestamp());
        user.setEmail(userRep.getEmail());
        if (userRep.isEmailVerified() != null) {
            user.setEmailVerified((boolean)userRep.isEmailVerified());
        }
        user.setFirstName(userRep.getFirstName());
        user.setLastName(userRep.getLastName());
        user.setFederationLink(userRep.getFederationLink());
        if (userRep.getAttributes() != null) {
            for (final Map.Entry<String, List<String>> entry : userRep.getAttributes().entrySet()) {
                final List<String> value = entry.getValue();
                if (value != null) {
                    user.setAttribute((String)entry.getKey(), (List)new ArrayList(value));
                }
            }
        }
        if (userRep.getRequiredActions() != null) {
            for (final String requiredAction : userRep.getRequiredActions()) {
                try {
                    user.addRequiredAction(UserModel.RequiredAction.valueOf(requiredAction.toUpperCase()));
                }
                catch (IllegalArgumentException iae) {
                    user.addRequiredAction(requiredAction);
                }
            }
        }
        createCredentials(userRep, session, newRealm, user, false);
        createFederatedIdentities(userRep, session, newRealm, user);
        createRoleMappings(userRep, user, newRealm);
        if (userRep.getClientConsents() != null) {
            for (final UserConsentRepresentation consentRep : userRep.getClientConsents()) {
                final UserConsentModel consentModel = toModel(newRealm, consentRep);
                session.users().addConsent(newRealm, user.getId(), consentModel);
            }
        }
        if (userRep.getNotBefore() != null) {
            session.users().setNotBeforeForUser(newRealm, user, (int)userRep.getNotBefore());
        }
        if (userRep.getServiceAccountClientId() != null) {
            final String clientId = userRep.getServiceAccountClientId();
            final ClientModel client = newRealm.getClientByClientId(clientId);
            if (client == null) {
                throw new RuntimeException("Unable to find client specified for service account link. Client: " + clientId);
            }
            user.setServiceAccountClientLink(client.getId());
        }
        createGroups(userRep, newRealm, user);
        return user;
    }
    
    public static void createGroups(final UserRepresentation userRep, final RealmModel newRealm, final UserModel user) {
        if (userRep.getGroups() != null) {
            for (final String path : userRep.getGroups()) {
                final GroupModel group = KeycloakModelUtils.findGroupByPath(newRealm, path);
                if (group == null) {
                    throw new RuntimeException("Unable to find group specified by path: " + path);
                }
                user.joinGroup(group);
            }
        }
    }
    
    public static void createFederatedIdentities(final UserRepresentation userRep, final KeycloakSession session, final RealmModel realm, final UserModel user) {
        if (userRep.getFederatedIdentities() != null) {
            for (final FederatedIdentityRepresentation identity : userRep.getFederatedIdentities()) {
                final FederatedIdentityModel mappingModel = new FederatedIdentityModel(identity.getIdentityProvider(), identity.getUserId(), identity.getUserName());
                session.users().addFederatedIdentity(realm, user, mappingModel);
            }
        }
    }
    
    public static void createCredentials(final UserRepresentation userRep, final KeycloakSession session, final RealmModel realm, final UserModel user, final boolean adminRequest) {
        convertDeprecatedCredentialsFormat(userRep);
        if (userRep.getCredentials() != null) {
            for (final CredentialRepresentation cred : userRep.getCredentials()) {
                if (cred.getId() != null && session.userCredentialManager().getStoredCredentialById(realm, user, cred.getId()) != null) {
                    continue;
                }
                if (cred.getValue() != null && !cred.getValue().isEmpty()) {
                    final RealmModel origRealm = session.getContext().getRealm();
                    try {
                        session.getContext().setRealm(realm);
                        session.userCredentialManager().updateCredential(realm, user, (CredentialInput)UserCredentialModel.password(cred.getValue(), false));
                    }
                    catch (ModelException ex) {
                        throw new PasswordPolicyNotMetException(ex.getMessage(), user.getUsername(), (Throwable)ex);
                    }
                    finally {
                        session.getContext().setRealm(origRealm);
                    }
                }
                else {
                    session.userCredentialManager().createCredentialThroughProvider(realm, user, toModel(cred));
                }
            }
        }
    }
    
    public static CredentialModel toModel(final CredentialRepresentation cred) {
        final CredentialModel model = new CredentialModel();
        model.setCreatedDate(cred.getCreatedDate());
        model.setType(cred.getType());
        model.setUserLabel(cred.getUserLabel());
        model.setSecretData(cred.getSecretData());
        model.setCredentialData(cred.getCredentialData());
        model.setId(cred.getId());
        return model;
    }
    
    public static void createRoleMappings(final UserRepresentation userRep, final UserModel user, final RealmModel realm) {
        if (userRep.getRealmRoles() != null) {
            for (final String roleString : userRep.getRealmRoles()) {
                RoleModel role = realm.getRole(roleString.trim());
                if (role == null) {
                    role = realm.addRole(roleString.trim());
                }
                user.grantRole(role);
            }
        }
        if (userRep.getClientRoles() != null) {
            for (final Map.Entry<String, List<String>> entry : userRep.getClientRoles().entrySet()) {
                final ClientModel client = realm.getClientByClientId((String)entry.getKey());
                if (client == null) {
                    throw new RuntimeException("Unable to find client role mappings for client: " + entry.getKey());
                }
                createClientRoleMappings(client, user, entry.getValue());
            }
        }
    }
    
    public static void createClientRoleMappings(final ClientModel clientModel, final UserModel user, final List<String> roleNames) {
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        for (final String roleName : roleNames) {
            RoleModel role = clientModel.getRole(roleName.trim());
            if (role == null) {
                role = clientModel.addRole(roleName.trim());
            }
            user.grantRole(role);
        }
    }
    
    private static void importIdentityProviders(final RealmRepresentation rep, final RealmModel newRealm, final KeycloakSession session) {
        if (rep.getIdentityProviders() != null) {
            for (final IdentityProviderRepresentation representation : rep.getIdentityProviders()) {
                newRealm.addIdentityProvider(toModel(newRealm, representation, session));
            }
        }
    }
    
    private static void importIdentityProviderMappers(final RealmRepresentation rep, final RealmModel newRealm) {
        if (rep.getIdentityProviderMappers() != null) {
            for (final IdentityProviderMapperRepresentation representation : rep.getIdentityProviderMappers()) {
                newRealm.addIdentityProviderMapper(toModel(representation));
            }
        }
    }
    
    public static IdentityProviderModel toModel(final RealmModel realm, final IdentityProviderRepresentation representation, final KeycloakSession session) {
        IdentityProviderFactory providerFactory = (IdentityProviderFactory)session.getKeycloakSessionFactory().getProviderFactory((Class)IdentityProvider.class, representation.getProviderId());
        if (providerFactory == null) {
            providerFactory = (IdentityProviderFactory)session.getKeycloakSessionFactory().getProviderFactory((Class)SocialIdentityProvider.class, representation.getProviderId());
        }
        if (providerFactory == null) {
            throw new IllegalArgumentException("Invalid identity provider id [" + representation.getProviderId() + "]");
        }
        final IdentityProviderModel identityProviderModel = providerFactory.createConfig();
        identityProviderModel.setInternalId(representation.getInternalId());
        identityProviderModel.setAlias(representation.getAlias());
        identityProviderModel.setDisplayName(representation.getDisplayName());
        identityProviderModel.setProviderId(representation.getProviderId());
        identityProviderModel.setEnabled(representation.isEnabled());
        identityProviderModel.setLinkOnly(representation.isLinkOnly());
        identityProviderModel.setTrustEmail(representation.isTrustEmail());
        identityProviderModel.setAuthenticateByDefault(representation.isAuthenticateByDefault());
        identityProviderModel.setStoreToken(representation.isStoreToken());
        identityProviderModel.setAddReadTokenRoleOnCreate(representation.isAddReadTokenRoleOnCreate());
        identityProviderModel.setConfig((Map)removeEmptyString(representation.getConfig()));
        String flowAlias = representation.getFirstBrokerLoginFlowAlias();
        if (flowAlias == null) {
            flowAlias = "first broker login";
        }
        AuthenticationFlowModel flowModel = realm.getFlowByAlias(flowAlias);
        if (flowModel == null) {
            throw new ModelException("No available authentication flow with alias: " + flowAlias);
        }
        identityProviderModel.setFirstBrokerLoginFlowId(flowModel.getId());
        flowAlias = representation.getPostBrokerLoginFlowAlias();
        if (flowAlias == null || flowAlias.trim().length() == 0) {
            identityProviderModel.setPostBrokerLoginFlowId((String)null);
        }
        else {
            flowModel = realm.getFlowByAlias(flowAlias);
            if (flowModel == null) {
                throw new ModelException("No available authentication flow with alias: " + flowAlias);
            }
            identityProviderModel.setPostBrokerLoginFlowId(flowModel.getId());
        }
        identityProviderModel.validate(realm);
        return identityProviderModel;
    }
    
    public static ProtocolMapperModel toModel(final ProtocolMapperRepresentation rep) {
        final ProtocolMapperModel model = new ProtocolMapperModel();
        model.setId(rep.getId());
        model.setName(rep.getName());
        model.setProtocol(rep.getProtocol());
        model.setProtocolMapper(rep.getProtocolMapper());
        model.setConfig((Map)removeEmptyString(rep.getConfig()));
        return model;
    }
    
    public static IdentityProviderMapperModel toModel(final IdentityProviderMapperRepresentation rep) {
        final IdentityProviderMapperModel model = new IdentityProviderMapperModel();
        model.setId(rep.getId());
        model.setName(rep.getName());
        model.setIdentityProviderAlias(rep.getIdentityProviderAlias());
        model.setIdentityProviderMapper(rep.getIdentityProviderMapper());
        model.setConfig((Map)removeEmptyString(rep.getConfig()));
        return model;
    }
    
    public static UserConsentModel toModel(final RealmModel newRealm, final UserConsentRepresentation consentRep) {
        final ClientModel client = newRealm.getClientByClientId(consentRep.getClientId());
        if (client == null) {
            throw new RuntimeException("Unable to find client consent mappings for client: " + consentRep.getClientId());
        }
        final UserConsentModel consentModel = new UserConsentModel(client);
        consentModel.setCreatedDate(consentRep.getCreatedDate());
        consentModel.setLastUpdatedDate(consentRep.getLastUpdatedDate());
        if (consentRep.getGrantedClientScopes() != null) {
            for (final String scopeName : consentRep.getGrantedClientScopes()) {
                final ClientScopeModel clientScope = KeycloakModelUtils.getClientScopeByName(newRealm, scopeName);
                if (clientScope == null) {
                    throw new RuntimeException("Unable to find client scope referenced in consent mappings of user. Client scope name: " + scopeName);
                }
                consentModel.addGrantedClientScope(clientScope);
            }
        }
        if (consentRep.getGrantedRealmRoles() != null && consentRep.getGrantedRealmRoles().contains("offline_access")) {
            final ClientScopeModel offlineScope = client.getClientScopes(false, true).get("offline_access");
            if (offlineScope == null) {
                RepresentationToModel.logger.warn((Object)"Unable to find offline_access scope referenced in grantedRoles of user");
            }
            consentModel.addGrantedClientScope(offlineScope);
        }
        return consentModel;
    }
    
    public static AuthenticationFlowModel toModel(final AuthenticationFlowRepresentation rep) {
        final AuthenticationFlowModel model = new AuthenticationFlowModel();
        model.setId(rep.getId());
        model.setBuiltIn(rep.isBuiltIn());
        model.setTopLevel(rep.isTopLevel());
        model.setProviderId(rep.getProviderId());
        model.setAlias(rep.getAlias());
        model.setDescription(rep.getDescription());
        return model;
    }
    
    private static AuthenticationExecutionModel toModel(final RealmModel realm, final AuthenticationFlowModel parentFlow, final AuthenticationExecutionExportRepresentation rep) {
        final AuthenticationExecutionModel model = new AuthenticationExecutionModel();
        if (rep.getAuthenticatorConfig() != null) {
            final AuthenticatorConfigModel config = realm.getAuthenticatorConfigByAlias(rep.getAuthenticatorConfig());
            model.setAuthenticatorConfig(config.getId());
        }
        model.setAuthenticator(rep.getAuthenticator());
        model.setAuthenticatorFlow(rep.isAutheticatorFlow());
        if (rep.getFlowAlias() != null) {
            final AuthenticationFlowModel flow = realm.getFlowByAlias(rep.getFlowAlias());
            model.setFlowId(flow.getId());
        }
        model.setPriority(rep.getPriority());
        try {
            model.setRequirement(AuthenticationExecutionModel.Requirement.valueOf(rep.getRequirement()));
            model.setParentFlow(parentFlow.getId());
        }
        catch (IllegalArgumentException iae) {
            if ("OPTIONAL".equals(rep.getRequirement())) {
                MigrateTo8_0_0.migrateOptionalAuthenticationExecution(realm, parentFlow, model, false);
            }
        }
        return model;
    }
    
    public static AuthenticationExecutionModel toModel(final RealmModel realm, final AuthenticationExecutionRepresentation rep) {
        final AuthenticationExecutionModel model = new AuthenticationExecutionModel();
        model.setId(rep.getId());
        model.setFlowId(rep.getFlowId());
        model.setAuthenticator(rep.getAuthenticator());
        model.setPriority(rep.getPriority());
        model.setParentFlow(rep.getParentFlow());
        model.setAuthenticatorFlow(rep.isAutheticatorFlow());
        model.setRequirement(AuthenticationExecutionModel.Requirement.valueOf(rep.getRequirement()));
        if (rep.getAuthenticatorConfig() != null) {
            final AuthenticatorConfigModel cfg = realm.getAuthenticatorConfigByAlias(rep.getAuthenticatorConfig());
            model.setAuthenticatorConfig(cfg.getId());
        }
        return model;
    }
    
    public static AuthenticatorConfigModel toModel(final AuthenticatorConfigRepresentation rep) {
        final AuthenticatorConfigModel model = new AuthenticatorConfigModel();
        model.setAlias(rep.getAlias());
        model.setConfig((Map)removeEmptyString(rep.getConfig()));
        return model;
    }
    
    public static RequiredActionProviderModel toModel(final RequiredActionProviderRepresentation rep) {
        final RequiredActionProviderModel model = new RequiredActionProviderModel();
        model.setConfig((Map)removeEmptyString(rep.getConfig()));
        model.setPriority(rep.getPriority());
        model.setDefaultAction(rep.isDefaultAction());
        model.setEnabled(rep.isEnabled());
        model.setProviderId(rep.getProviderId());
        model.setName(rep.getName());
        model.setAlias(rep.getAlias());
        return model;
    }
    
    public static ComponentModel toModel(final KeycloakSession session, final ComponentRepresentation rep) {
        final ComponentModel model = new ComponentModel();
        model.setId(rep.getId());
        model.setParentId(rep.getParentId());
        model.setProviderType(rep.getProviderType());
        model.setProviderId(rep.getProviderId());
        model.setConfig(new MultivaluedHashMap());
        model.setName(rep.getName());
        model.setSubType(rep.getSubType());
        if (rep.getConfig() != null) {
            final Set<String> keys = new HashSet<String>(rep.getConfig().keySet());
            for (final String k : keys) {
                final List<String> values = (List<String>)rep.getConfig().get((Object)k);
                if (values != null) {
                    final ListIterator<String> itr = values.listIterator();
                    while (itr.hasNext()) {
                        final String v = itr.next();
                        if (v == null || v.trim().isEmpty()) {
                            itr.remove();
                        }
                    }
                    if (values.isEmpty()) {
                        continue;
                    }
                    model.getConfig().put((Object)k, (Object)values);
                }
            }
        }
        return model;
    }
    
    public static void updateComponent(final KeycloakSession session, final ComponentRepresentation rep, final ComponentModel component, final boolean internal) {
        if (rep.getName() != null) {
            component.setName(rep.getName());
        }
        if (rep.getParentId() != null) {
            component.setParentId(rep.getParentId());
        }
        if (rep.getProviderType() != null) {
            component.setProviderType(rep.getProviderType());
        }
        if (rep.getProviderId() != null) {
            component.setProviderId(rep.getProviderId());
        }
        if (rep.getSubType() != null) {
            component.setSubType(rep.getSubType());
        }
        Map<String, ProviderConfigProperty> providerConfiguration = null;
        if (!internal) {
            providerConfiguration = ComponentUtil.getComponentConfigProperties(session, component);
        }
        if (rep.getConfig() != null) {
            final Set<String> keys = new HashSet<String>(rep.getConfig().keySet());
            for (final String k : keys) {
                if (!internal && !providerConfiguration.containsKey(k)) {
                    break;
                }
                final List<String> values = (List<String>)rep.getConfig().get((Object)k);
                if (values == null || values.isEmpty() || values.get(0) == null || values.get(0).trim().isEmpty()) {
                    component.getConfig().remove((Object)k);
                }
                else {
                    final ListIterator<String> itr = values.listIterator();
                    while (itr.hasNext()) {
                        final String v = itr.next();
                        if (v == null || v.trim().isEmpty() || v.equals("**********")) {
                            itr.remove();
                        }
                    }
                    if (values.isEmpty()) {
                        continue;
                    }
                    component.getConfig().put((Object)k, (Object)values);
                }
            }
        }
    }
    
    public static void importRealmAuthorizationSettings(final RealmRepresentation rep, final RealmModel newRealm, final KeycloakSession session) {
        if (rep.getClients() != null) {
            final ClientModel client;
            rep.getClients().forEach(clientRepresentation -> {
                client = newRealm.getClientByClientId(clientRepresentation.getClientId());
                importAuthorizationSettings(clientRepresentation, client, session);
            });
        }
    }
    
    public static void importAuthorizationSettings(final ClientRepresentation clientRepresentation, final ClientModel client, final KeycloakSession session) {
        if (Boolean.TRUE.equals(clientRepresentation.getAuthorizationServicesEnabled())) {
            final AuthorizationProviderFactory authorizationFactory = (AuthorizationProviderFactory)session.getKeycloakSessionFactory().getProviderFactory((Class)AuthorizationProvider.class);
            final AuthorizationProvider authorization = authorizationFactory.create(session, client.getRealm());
            client.setServiceAccountsEnabled(true);
            client.setBearerOnly(false);
            client.setPublicClient(false);
            ResourceServerRepresentation rep = clientRepresentation.getAuthorizationSettings();
            if (rep == null) {
                rep = new ResourceServerRepresentation();
            }
            rep.setClientId(client.getId());
            toModel(rep, authorization);
        }
    }
    
    public static ResourceServer toModel(final ResourceServerRepresentation rep, final AuthorizationProvider authorization) {
        final ResourceServerStore resourceServerStore = authorization.getStoreFactory().getResourceServerStore();
        final ResourceServer existing = resourceServerStore.findById(rep.getClientId());
        ResourceServer resourceServer;
        if (existing == null) {
            resourceServer = resourceServerStore.create(rep.getClientId());
            resourceServer.setAllowRemoteResourceManagement(true);
            resourceServer.setPolicyEnforcementMode(PolicyEnforcementMode.ENFORCING);
        }
        else {
            resourceServer = existing;
        }
        resourceServer.setPolicyEnforcementMode(rep.getPolicyEnforcementMode());
        resourceServer.setAllowRemoteResourceManagement(rep.isAllowRemoteResourceManagement());
        DecisionStrategy decisionStrategy = rep.getDecisionStrategy();
        if (decisionStrategy == null) {
            decisionStrategy = DecisionStrategy.UNANIMOUS;
        }
        resourceServer.setDecisionStrategy(decisionStrategy);
        for (final ScopeRepresentation scope : rep.getScopes()) {
            toModel(scope, resourceServer, authorization);
        }
        final KeycloakSession session = authorization.getKeycloakSession();
        final RealmModel realm = authorization.getRealm();
        for (final ResourceRepresentation resource : rep.getResources()) {
            ResourceOwnerRepresentation owner = resource.getOwner();
            if (owner == null) {
                owner = new ResourceOwnerRepresentation();
                owner.setId(resourceServer.getId());
                resource.setOwner(owner);
            }
            else if (owner.getName() != null) {
                final UserModel user = session.users().getUserByUsername(owner.getName(), realm);
                if (user != null) {
                    owner.setId(user.getId());
                }
            }
            toModel(resource, resourceServer, authorization);
        }
        importPolicies(authorization, resourceServer, rep.getPolicies(), null);
        return resourceServer;
    }
    
    private static Policy importPolicies(final AuthorizationProvider authorization, final ResourceServer resourceServer, final List<PolicyRepresentation> policiesToImport, final String parentPolicyName) {
        final StoreFactory storeFactory = authorization.getStoreFactory();
        for (final PolicyRepresentation policyRepresentation : policiesToImport) {
            if (parentPolicyName != null && !parentPolicyName.equals(policyRepresentation.getName())) {
                continue;
            }
            final Map<String, String> config = (Map<String, String>)policyRepresentation.getConfig();
            final String applyPolicies = config.get("applyPolicies");
            if (applyPolicies != null && !applyPolicies.isEmpty()) {
                final PolicyStore policyStore = storeFactory.getPolicyStore();
                try {
                    final List<String> policies = (List<String>)JsonSerialization.readValue(applyPolicies, (Class)List.class);
                    final Set<String> policyIds = new HashSet<String>();
                    for (final String policyName : policies) {
                        Policy policy = policyStore.findByName(policyName, resourceServer.getId());
                        if (policy == null) {
                            policy = policyStore.findById(policyName, resourceServer.getId());
                        }
                        if (policy == null) {
                            policy = importPolicies(authorization, resourceServer, policiesToImport, policyName);
                            if (policy == null) {
                                throw new RuntimeException("Policy with name [" + policyName + "] not defined.");
                            }
                        }
                        policyIds.add(policy.getId());
                    }
                    config.put("applyPolicies", JsonSerialization.writeValueAsString((Object)policyIds));
                }
                catch (Exception e) {
                    throw new RuntimeException("Error while importing policy [" + policyRepresentation.getName() + "].", e);
                }
            }
            final PolicyStore policyStore = storeFactory.getPolicyStore();
            Policy policy2 = policyStore.findById(policyRepresentation.getId(), resourceServer.getId());
            if (policy2 == null) {
                policy2 = policyStore.findByName(policyRepresentation.getName(), resourceServer.getId());
            }
            if (policy2 == null) {
                policy2 = policyStore.create((AbstractPolicyRepresentation)policyRepresentation, resourceServer);
            }
            else {
                policy2 = toModel((AbstractPolicyRepresentation)policyRepresentation, authorization, policy2);
            }
            if (parentPolicyName != null && parentPolicyName.equals(policyRepresentation.getName())) {
                return policy2;
            }
        }
        return null;
    }
    
    public static Policy toModel(final AbstractPolicyRepresentation representation, final AuthorizationProvider authorization, final Policy model) {
        model.setName(representation.getName());
        model.setDescription(representation.getDescription());
        model.setDecisionStrategy(representation.getDecisionStrategy());
        model.setLogic(representation.getLogic());
        Set resources = representation.getResources();
        Set scopes = representation.getScopes();
        Set policies = representation.getPolicies();
        if (representation instanceof PolicyRepresentation) {
            final PolicyRepresentation policy = PolicyRepresentation.class.cast(representation);
            if (resources == null) {
                final String resourcesConfig = policy.getConfig().get("resources");
                if (resourcesConfig != null) {
                    try {
                        resources = (Set)JsonSerialization.readValue(resourcesConfig, (Class)Set.class);
                    }
                    catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            if (scopes == null) {
                final String scopesConfig = policy.getConfig().get("scopes");
                if (scopesConfig != null) {
                    try {
                        scopes = (Set)JsonSerialization.readValue(scopesConfig, (Class)Set.class);
                    }
                    catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            if (policies == null) {
                final String policiesConfig = policy.getConfig().get("applyPolicies");
                if (policiesConfig != null) {
                    try {
                        policies = (Set)JsonSerialization.readValue(policiesConfig, (Class)Set.class);
                    }
                    catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            model.setConfig(policy.getConfig());
        }
        final StoreFactory storeFactory = authorization.getStoreFactory();
        updateResources(resources, model, storeFactory);
        updateScopes(scopes, model, storeFactory);
        updateAssociatedPolicies(policies, model, storeFactory);
        final PolicyProviderFactory provider = authorization.getProviderFactory(model.getType());
        if (representation instanceof PolicyRepresentation) {
            provider.onImport(model, PolicyRepresentation.class.cast(representation), authorization);
        }
        else if (representation.getId() == null) {
            provider.onCreate(model, representation, authorization);
        }
        else {
            provider.onUpdate(model, representation, authorization);
        }
        representation.setId(model.getId());
        return model;
    }
    
    private static void updateScopes(final Set<String> scopeIds, final Policy policy, final StoreFactory storeFactory) {
        if (scopeIds != null) {
            if (scopeIds.isEmpty()) {
                for (final Scope scope : new HashSet<Scope>(policy.getScopes())) {
                    policy.removeScope(scope);
                }
                return;
            }
            for (final String scopeId : scopeIds) {
                boolean hasScope = false;
                for (final Scope scopeModel : new HashSet<Scope>(policy.getScopes())) {
                    if (scopeModel.getId().equals(scopeId) || scopeModel.getName().equals(scopeId)) {
                        hasScope = true;
                    }
                }
                if (!hasScope) {
                    final ResourceServer resourceServer = policy.getResourceServer();
                    Scope scope2 = storeFactory.getScopeStore().findById(scopeId, resourceServer.getId());
                    if (scope2 == null) {
                        scope2 = storeFactory.getScopeStore().findByName(scopeId, resourceServer.getId());
                        if (scope2 == null) {
                            throw new RuntimeException("Scope with id or name [" + scopeId + "] does not exist");
                        }
                    }
                    policy.addScope(scope2);
                }
            }
            for (final Scope scopeModel2 : new HashSet<Scope>(policy.getScopes())) {
                boolean hasScope = false;
                for (final String scopeId2 : scopeIds) {
                    if (scopeModel2.getId().equals(scopeId2) || scopeModel2.getName().equals(scopeId2)) {
                        hasScope = true;
                    }
                }
                if (!hasScope) {
                    policy.removeScope(scopeModel2);
                }
            }
        }
        policy.removeConfig("scopes");
    }
    
    private static void updateAssociatedPolicies(final Set<String> policyIds, final Policy policy, final StoreFactory storeFactory) {
        final ResourceServer resourceServer = policy.getResourceServer();
        if (policyIds != null) {
            if (policyIds.isEmpty()) {
                for (final Policy associated : new HashSet<Policy>(policy.getAssociatedPolicies())) {
                    policy.removeAssociatedPolicy(associated);
                }
                return;
            }
            final PolicyStore policyStore = storeFactory.getPolicyStore();
            for (final String policyId : policyIds) {
                boolean hasPolicy = false;
                for (final Policy policyModel : new HashSet<Policy>(policy.getAssociatedPolicies())) {
                    if (policyModel.getId().equals(policyId) || policyModel.getName().equals(policyId)) {
                        hasPolicy = true;
                    }
                }
                if (!hasPolicy) {
                    Policy associatedPolicy = policyStore.findById(policyId, resourceServer.getId());
                    if (associatedPolicy == null) {
                        associatedPolicy = policyStore.findByName(policyId, resourceServer.getId());
                        if (associatedPolicy == null) {
                            throw new RuntimeException("Policy with id or name [" + policyId + "] does not exist");
                        }
                    }
                    policy.addAssociatedPolicy(associatedPolicy);
                }
            }
            for (final Policy policyModel2 : new HashSet<Policy>(policy.getAssociatedPolicies())) {
                boolean hasPolicy = false;
                for (final String policyId2 : policyIds) {
                    if (policyModel2.getId().equals(policyId2) || policyModel2.getName().equals(policyId2)) {
                        hasPolicy = true;
                    }
                }
                if (!hasPolicy) {
                    policy.removeAssociatedPolicy(policyModel2);
                }
            }
        }
        policy.removeConfig("applyPolicies");
    }
    
    private static void updateResources(final Set<String> resourceIds, final Policy policy, final StoreFactory storeFactory) {
        if (resourceIds != null) {
            if (resourceIds.isEmpty()) {
                for (final Resource resource : new HashSet<Resource>(policy.getResources())) {
                    policy.removeResource(resource);
                }
            }
            for (final String resourceId : resourceIds) {
                boolean hasResource = false;
                for (final Resource resourceModel : new HashSet<Resource>(policy.getResources())) {
                    if (resourceModel.getId().equals(resourceId) || resourceModel.getName().equals(resourceId)) {
                        hasResource = true;
                    }
                }
                if (!hasResource && !"".equals(resourceId)) {
                    Resource resource2 = storeFactory.getResourceStore().findById(resourceId, policy.getResourceServer().getId());
                    if (resource2 == null) {
                        resource2 = storeFactory.getResourceStore().findByName(resourceId, policy.getResourceServer().getId());
                        if (resource2 == null) {
                            throw new RuntimeException("Resource with id or name [" + resourceId + "] does not exist or is not owned by the resource server");
                        }
                    }
                    policy.addResource(resource2);
                }
            }
            for (final Resource resourceModel2 : new HashSet<Resource>(policy.getResources())) {
                boolean hasResource = false;
                for (final String resourceId2 : resourceIds) {
                    if (resourceModel2.getId().equals(resourceId2) || resourceModel2.getName().equals(resourceId2)) {
                        hasResource = true;
                    }
                }
                if (!hasResource) {
                    policy.removeResource(resourceModel2);
                }
            }
        }
        policy.removeConfig("resources");
    }
    
    public static Resource toModel(final ResourceRepresentation resource, final ResourceServer resourceServer, final AuthorizationProvider authorization) {
        final ResourceStore resourceStore = authorization.getStoreFactory().getResourceStore();
        ResourceOwnerRepresentation owner = resource.getOwner();
        if (owner == null) {
            owner = new ResourceOwnerRepresentation();
            owner.setId(resourceServer.getId());
        }
        String ownerId = owner.getId();
        if (ownerId == null) {
            ownerId = resourceServer.getId();
        }
        if (!resourceServer.getId().equals(ownerId)) {
            final RealmModel realm = authorization.getRealm();
            final KeycloakSession keycloakSession = authorization.getKeycloakSession();
            final UserProvider users = keycloakSession.users();
            UserModel ownerModel = users.getUserById(ownerId, realm);
            if (ownerModel == null) {
                ownerModel = users.getUserByUsername(ownerId, realm);
            }
            if (ownerModel == null) {
                throw new RuntimeException("Owner must be a valid username or user identifier. If the resource server, the client id or null.");
            }
            ownerId = ownerModel.getId();
        }
        Resource existing;
        if (resource.getId() != null) {
            existing = resourceStore.findById(resource.getId(), resourceServer.getId());
        }
        else {
            existing = resourceStore.findByName(resource.getName(), ownerId, resourceServer.getId());
        }
        if (existing != null) {
            existing.setName(resource.getName());
            existing.setDisplayName(resource.getDisplayName());
            existing.setType(resource.getType());
            existing.updateUris(resource.getUris());
            existing.setIconUri(resource.getIconUri());
            existing.setOwnerManagedAccess(Boolean.TRUE.equals(resource.getOwnerManagedAccess()));
            existing.updateScopes((Set<Scope>)resource.getScopes().stream().map(scope -> toModel(scope, resourceServer, authorization)).collect(Collectors.toSet()));
            final Map<String, List<String>> attributes = (Map<String, List<String>>)resource.getAttributes();
            if (attributes != null) {
                final Set<String> existingAttrNames = existing.getAttributes().keySet();
                for (final String name : existingAttrNames) {
                    if (attributes.containsKey(name)) {
                        existing.setAttribute(name, attributes.get(name));
                        attributes.remove(name);
                    }
                    else {
                        existing.removeAttribute(name);
                    }
                }
                for (final String name : attributes.keySet()) {
                    existing.setAttribute(name, attributes.get(name));
                }
            }
            return existing;
        }
        final Resource model = resourceStore.create(resource.getId(), resource.getName(), resourceServer, ownerId);
        model.setDisplayName(resource.getDisplayName());
        model.setType(resource.getType());
        model.updateUris(resource.getUris());
        model.setIconUri(resource.getIconUri());
        model.setOwnerManagedAccess(Boolean.TRUE.equals(resource.getOwnerManagedAccess()));
        final Set<ScopeRepresentation> scopes = (Set<ScopeRepresentation>)resource.getScopes();
        if (scopes != null) {
            model.updateScopes((Set<Scope>)scopes.stream().map(scope -> toModel(scope, resourceServer, authorization)).collect((Collector<? super Object, ?, Set<? super Object>>)Collectors.toSet()));
        }
        final Map<String, List<String>> attributes2 = (Map<String, List<String>>)resource.getAttributes();
        if (attributes2 != null) {
            for (final Map.Entry<String, List<String>> entry : attributes2.entrySet()) {
                model.setAttribute(entry.getKey(), entry.getValue());
            }
        }
        resource.setId(model.getId());
        return model;
    }
    
    public static Scope toModel(final ScopeRepresentation scope, final ResourceServer resourceServer, final AuthorizationProvider authorization) {
        final StoreFactory storeFactory = authorization.getStoreFactory();
        final ScopeStore scopeStore = storeFactory.getScopeStore();
        Scope existing;
        if (scope.getId() != null) {
            existing = scopeStore.findById(scope.getId(), resourceServer.getId());
        }
        else {
            existing = scopeStore.findByName(scope.getName(), resourceServer.getId());
        }
        if (existing != null) {
            existing.setName(scope.getName());
            existing.setDisplayName(scope.getDisplayName());
            existing.setIconUri(scope.getIconUri());
            return existing;
        }
        final Scope model = scopeStore.create(scope.getId(), scope.getName(), resourceServer);
        model.setDisplayName(scope.getDisplayName());
        model.setIconUri(scope.getIconUri());
        scope.setId(model.getId());
        return model;
    }
    
    public static PermissionTicket toModel(final PermissionTicketRepresentation representation, final String resourceServerId, final AuthorizationProvider authorization) {
        final PermissionTicketStore ticketStore = authorization.getStoreFactory().getPermissionTicketStore();
        final PermissionTicket ticket = ticketStore.findById(representation.getId(), resourceServerId);
        final boolean granted = representation.isGranted();
        if (granted && !ticket.isGranted()) {
            ticket.setGrantedTimestamp(System.currentTimeMillis());
        }
        else if (!granted) {
            ticketStore.delete(ticket.getId());
        }
        return ticket;
    }
    
    public static void importFederatedUser(final KeycloakSession session, final RealmModel newRealm, final UserRepresentation userRep) {
        final UserFederatedStorageProvider federatedStorage = session.userFederatedStorage();
        if (userRep.getAttributes() != null) {
            for (final Map.Entry<String, List<String>> entry : userRep.getAttributes().entrySet()) {
                final String key = entry.getKey();
                final List<String> value = entry.getValue();
                if (value != null) {
                    federatedStorage.setAttribute(newRealm, userRep.getId(), key, (List)new LinkedList(value));
                }
            }
        }
        if (userRep.getRequiredActions() != null) {
            for (final String action : userRep.getRequiredActions()) {
                federatedStorage.addRequiredAction(newRealm, userRep.getId(), action);
            }
        }
        if (userRep.getCredentials() != null) {
            for (final CredentialRepresentation cred : userRep.getCredentials()) {
                federatedStorage.createCredential(newRealm, userRep.getId(), toModel(cred));
            }
        }
        createFederatedRoleMappings(federatedStorage, userRep, newRealm);
        if (userRep.getGroups() != null) {
            for (final String path : userRep.getGroups()) {
                final GroupModel group = KeycloakModelUtils.findGroupByPath(newRealm, path);
                if (group == null) {
                    throw new RuntimeException("Unable to find group specified by path: " + path);
                }
                federatedStorage.joinGroup(newRealm, userRep.getId(), group);
            }
        }
        if (userRep.getFederatedIdentities() != null) {
            for (final FederatedIdentityRepresentation identity : userRep.getFederatedIdentities()) {
                final FederatedIdentityModel mappingModel = new FederatedIdentityModel(identity.getIdentityProvider(), identity.getUserId(), identity.getUserName());
                federatedStorage.addFederatedIdentity(newRealm, userRep.getId(), mappingModel);
            }
        }
        if (userRep.getClientConsents() != null) {
            for (final UserConsentRepresentation consentRep : userRep.getClientConsents()) {
                final UserConsentModel consentModel = toModel(newRealm, consentRep);
                federatedStorage.addConsent(newRealm, userRep.getId(), consentModel);
            }
        }
        if (userRep.getNotBefore() != null) {
            federatedStorage.setNotBeforeForUser(newRealm, userRep.getId(), (int)userRep.getNotBefore());
        }
    }
    
    public static void createFederatedRoleMappings(final UserFederatedStorageProvider federatedStorage, final UserRepresentation userRep, final RealmModel realm) {
        if (userRep.getRealmRoles() != null) {
            for (final String roleString : userRep.getRealmRoles()) {
                RoleModel role = realm.getRole(roleString.trim());
                if (role == null) {
                    role = realm.addRole(roleString.trim());
                }
                federatedStorage.grantRole(realm, userRep.getId(), role);
            }
        }
        if (userRep.getClientRoles() != null) {
            for (final Map.Entry<String, List<String>> entry : userRep.getClientRoles().entrySet()) {
                final ClientModel client = realm.getClientByClientId((String)entry.getKey());
                if (client == null) {
                    throw new RuntimeException("Unable to find client role mappings for client: " + entry.getKey());
                }
                createFederatedClientRoleMappings(federatedStorage, realm, client, userRep, entry.getValue());
            }
        }
    }
    
    public static void createFederatedClientRoleMappings(final UserFederatedStorageProvider federatedStorage, final RealmModel realm, final ClientModel clientModel, final UserRepresentation userRep, final List<String> roleNames) {
        if (userRep == null) {
            throw new RuntimeException("User not found");
        }
        for (final String roleName : roleNames) {
            RoleModel role = clientModel.getRole(roleName.trim());
            if (role == null) {
                role = clientModel.addRole(roleName.trim());
            }
            federatedStorage.grantRole(realm, userRep.getId(), role);
        }
    }
    
    public static Map<String, String> removeEmptyString(final Map<String, String> map) {
        if (map == null) {
            return null;
        }
        final Map<String, String> m = new HashMap<String, String>(map);
        final Iterator<Map.Entry<String, String>> itr = m.entrySet().iterator();
        while (itr.hasNext()) {
            final Map.Entry<String, String> e = itr.next();
            if (e.getValue() == null || e.getValue().equals("")) {
                itr.remove();
            }
        }
        return m;
    }
    
    public static ResourceServer createResourceServer(final ClientModel client, final KeycloakSession session, final boolean addDefaultRoles) {
        if ((client.isBearerOnly() || client.isPublicClient()) && !client.getClientId().equals(Config.getAdminRealm() + "-realm") && !client.getClientId().equals("realm-management")) {
            throw new RuntimeException("Only confidential clients are allowed to set authorization settings");
        }
        final AuthorizationProvider authorization = (AuthorizationProvider)session.getProvider((Class)AuthorizationProvider.class);
        final UserModel serviceAccount = session.users().getServiceAccount(client);
        if (serviceAccount == null) {
            client.setServiceAccountsEnabled(true);
        }
        if (addDefaultRoles) {
            RoleModel umaProtectionRole = client.getRole("uma_protection");
            if (umaProtectionRole == null) {
                umaProtectionRole = client.addRole("uma_protection");
            }
            if (serviceAccount != null) {
                serviceAccount.grantRole(umaProtectionRole);
            }
        }
        final ResourceServerRepresentation representation = new ResourceServerRepresentation();
        representation.setAllowRemoteResourceManagement(true);
        representation.setClientId(client.getId());
        return toModel(representation, authorization);
    }
    
    static {
        RepresentationToModel.logger = Logger.getLogger((Class)RepresentationToModel.class);
    }
}
