// 
// Decompiled by Procyon v0.5.36
// 

package org.keycloak.storage.jpa;

import org.keycloak.storage.client.ClientStorageProvider;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.common.util.Base64;
import org.keycloak.storage.jpa.entity.FederatedUserCredentialEntity;
import org.keycloak.credential.CredentialModel;
import org.keycloak.storage.jpa.entity.FederatedUserRoleMappingEntity;
import org.keycloak.models.RoleModel;
import java.util.function.Consumer;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.Objects;
import org.keycloak.storage.jpa.entity.FederatedUserRequiredActionEntity;
import java.util.function.Function;
import org.keycloak.storage.jpa.entity.FederatedUserGroupMembershipEntity;
import org.keycloak.models.GroupModel;
import java.util.HashSet;
import org.keycloak.models.ClientScopeModel;
import java.util.Collection;
import org.keycloak.models.ClientModel;
import org.keycloak.storage.jpa.entity.FederatedUserConsentClientScopeEntity;
import org.keycloak.models.ModelException;
import org.keycloak.common.util.Time;
import org.keycloak.storage.jpa.entity.FederatedUserConsentEntity;
import org.keycloak.models.ModelDuplicateException;
import javax.persistence.LockModeType;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.storage.jpa.entity.BrokerLinkEntity;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.utils.StreamsUtil;
import java.util.stream.Stream;
import javax.persistence.TypedQuery;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.storage.jpa.entity.FederatedUserAttributeEntity;
import java.util.Iterator;
import java.util.List;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.jpa.entity.FederatedUser;
import org.keycloak.models.RealmModel;
import javax.persistence.EntityManager;
import org.keycloak.models.KeycloakSession;
import org.jboss.logging.Logger;
import org.keycloak.credential.UserCredentialStore;
import org.keycloak.storage.federated.UserFederatedStorageProvider;

public class JpaUserFederatedStorageProvider implements UserFederatedStorageProvider.Streams, UserCredentialStore.Streams
{
    protected static final Logger logger;
    private final KeycloakSession session;
    protected EntityManager em;
    
    public JpaUserFederatedStorageProvider(final KeycloakSession session, final EntityManager em) {
        this.session = session;
        this.em = em;
    }
    
    public void close() {
    }
    
    protected void createIndex(final RealmModel realm, final String userId) {
        if (this.em.find((Class)FederatedUser.class, (Object)userId) == null) {
            final FederatedUser fedUser = new FederatedUser();
            fedUser.setId(userId);
            fedUser.setRealmId(realm.getId());
            fedUser.setStorageProviderId(new StorageId(userId).getProviderId());
            this.em.persist((Object)fedUser);
        }
    }
    
    public void setAttribute(final RealmModel realm, final String userId, final String name, final List<String> values) {
        this.createIndex(realm, userId);
        this.deleteAttribute(realm, userId, name);
        this.em.flush();
        for (final String value : values) {
            this.persistAttributeValue(realm, userId, name, value);
        }
    }
    
    private void deleteAttribute(final RealmModel realm, final String userId, final String name) {
        this.em.createNamedQuery("deleteUserFederatedAttributesByUserAndName").setParameter("userId", (Object)userId).setParameter("realmId", (Object)realm.getId()).setParameter("name", (Object)name).executeUpdate();
    }
    
    private void persistAttributeValue(final RealmModel realm, final String userId, final String name, final String value) {
        final FederatedUserAttributeEntity attr = new FederatedUserAttributeEntity();
        attr.setId(KeycloakModelUtils.generateId());
        attr.setName(name);
        attr.setValue(value);
        attr.setUserId(userId);
        attr.setRealmId(realm.getId());
        attr.setStorageProviderId(new StorageId(userId).getProviderId());
        this.em.persist((Object)attr);
    }
    
    public void setSingleAttribute(final RealmModel realm, final String userId, final String name, final String value) {
        this.createIndex(realm, userId);
        this.deleteAttribute(realm, userId, name);
        this.em.flush();
        this.persistAttributeValue(realm, userId, name, value);
    }
    
    public void removeAttribute(final RealmModel realm, final String userId, final String name) {
        this.deleteAttribute(realm, userId, name);
        this.em.flush();
    }
    
    public MultivaluedHashMap<String, String> getAttributes(final RealmModel realm, final String userId) {
        final TypedQuery<FederatedUserAttributeEntity> query = (TypedQuery<FederatedUserAttributeEntity>)this.em.createNamedQuery("getFederatedAttributesByUser", (Class)FederatedUserAttributeEntity.class);
        final List<FederatedUserAttributeEntity> list = (List<FederatedUserAttributeEntity>)query.setParameter("userId", (Object)userId).setParameter("realmId", (Object)realm.getId()).getResultList();
        final MultivaluedHashMap<String, String> result = (MultivaluedHashMap<String, String>)new MultivaluedHashMap();
        for (final FederatedUserAttributeEntity entity : list) {
            result.add((Object)entity.getName(), (Object)entity.getValue());
        }
        return result;
    }
    
    public Stream<String> getUsersByUserAttributeStream(final RealmModel realm, final String name, final String value) {
        final TypedQuery<String> query = (TypedQuery<String>)this.em.createNamedQuery("getFederatedAttributesByNameAndValue", (Class)String.class).setParameter("realmId", (Object)realm.getId()).setParameter("name", (Object)name).setParameter("value", (Object)value);
        return (Stream<String>)StreamsUtil.closing(query.getResultStream());
    }
    
    public String getUserByFederatedIdentity(final FederatedIdentityModel link, final RealmModel realm) {
        final TypedQuery<String> query = (TypedQuery<String>)this.em.createNamedQuery("findUserByBrokerLinkAndRealm", (Class)String.class).setParameter("realmId", (Object)realm.getId()).setParameter("identityProvider", (Object)link.getIdentityProvider()).setParameter("brokerUserId", (Object)link.getUserId());
        final List<String> results = (List<String>)query.getResultList();
        if (results.isEmpty()) {
            return null;
        }
        if (results.size() > 1) {
            throw new IllegalStateException("More results found for identityProvider=" + link.getIdentityProvider() + ", userId=" + link.getUserId() + ", results=" + results);
        }
        return results.get(0);
    }
    
    public void addFederatedIdentity(final RealmModel realm, final String userId, final FederatedIdentityModel link) {
        this.createIndex(realm, userId);
        final BrokerLinkEntity entity = new BrokerLinkEntity();
        entity.setRealmId(realm.getId());
        entity.setUserId(userId);
        entity.setBrokerUserId(link.getUserId());
        entity.setIdentityProvider(link.getIdentityProvider());
        entity.setToken(link.getToken());
        entity.setBrokerUserName(link.getUserName());
        entity.setStorageProviderId(new StorageId(userId).getProviderId());
        this.em.persist((Object)entity);
    }
    
    public boolean removeFederatedIdentity(final RealmModel realm, final String userId, final String socialProvider) {
        final BrokerLinkEntity entity = this.getBrokerLinkEntity(realm, userId, socialProvider);
        if (entity == null) {
            return false;
        }
        this.em.remove((Object)entity);
        return true;
    }
    
    public void preRemove(final RealmModel realm, final IdentityProviderModel provider) {
        this.em.createNamedQuery("deleteBrokerLinkByIdentityProvider").setParameter("realmId", (Object)realm.getId()).setParameter("providerAlias", (Object)provider.getAlias());
    }
    
    private BrokerLinkEntity getBrokerLinkEntity(final RealmModel realm, final String userId, final String socialProvider) {
        final TypedQuery<BrokerLinkEntity> query = (TypedQuery<BrokerLinkEntity>)this.em.createNamedQuery("findBrokerLinkByUserAndProvider", (Class)BrokerLinkEntity.class).setParameter("userId", (Object)userId).setParameter("realmId", (Object)realm.getId()).setParameter("identityProvider", (Object)socialProvider);
        final List<BrokerLinkEntity> results = (List<BrokerLinkEntity>)query.getResultList();
        return (results.size() > 0) ? results.get(0) : null;
    }
    
    public void updateFederatedIdentity(final RealmModel realm, final String userId, final FederatedIdentityModel model) {
        this.createIndex(realm, userId);
        final BrokerLinkEntity entity = this.getBrokerLinkEntity(realm, userId, model.getIdentityProvider());
        if (entity == null) {
            return;
        }
        entity.setBrokerUserName(model.getUserName());
        entity.setBrokerUserId(model.getUserId());
        entity.setToken(model.getToken());
        this.em.persist((Object)entity);
        this.em.flush();
    }
    
    public Stream<FederatedIdentityModel> getFederatedIdentitiesStream(final String userId, final RealmModel realm) {
        final TypedQuery<BrokerLinkEntity> query = (TypedQuery<BrokerLinkEntity>)this.em.createNamedQuery("findBrokerLinkByUser", (Class)BrokerLinkEntity.class).setParameter("userId", (Object)userId);
        return (Stream<FederatedIdentityModel>)StreamsUtil.closing((Stream)query.getResultStream().map(entity -> new FederatedIdentityModel(entity.getIdentityProvider(), entity.getBrokerUserId(), entity.getBrokerUserName(), entity.getToken())).distinct());
    }
    
    public FederatedIdentityModel getFederatedIdentity(final String userId, final String socialProvider, final RealmModel realm) {
        final BrokerLinkEntity entity = this.getBrokerLinkEntity(realm, userId, socialProvider);
        if (entity == null) {
            return null;
        }
        return new FederatedIdentityModel(entity.getIdentityProvider(), entity.getBrokerUserId(), entity.getBrokerUserName(), entity.getToken());
    }
    
    public void addConsent(final RealmModel realm, final String userId, final UserConsentModel consent) {
        this.createIndex(realm, userId);
        final String clientId = consent.getClient().getId();
        FederatedUserConsentEntity consentEntity = this.getGrantedConsentEntity(userId, clientId, LockModeType.NONE);
        if (consentEntity != null) {
            throw new ModelDuplicateException("Consent already exists for client [" + clientId + "] and user [" + userId + "]");
        }
        consentEntity = new FederatedUserConsentEntity();
        consentEntity.setId(KeycloakModelUtils.generateId());
        consentEntity.setUserId(userId);
        final StorageId clientStorageId = new StorageId(clientId);
        if (clientStorageId.isLocal()) {
            consentEntity.setClientId(clientId);
        }
        else {
            consentEntity.setClientStorageProvider(clientStorageId.getProviderId());
            consentEntity.setExternalClientId(clientStorageId.getExternalId());
        }
        consentEntity.setRealmId(realm.getId());
        consentEntity.setStorageProviderId(new StorageId(userId).getProviderId());
        final long currentTime = Time.currentTimeMillis();
        consentEntity.setCreatedDate(currentTime);
        consentEntity.setLastUpdatedDate(currentTime);
        this.em.persist((Object)consentEntity);
        this.em.flush();
        this.updateGrantedConsentEntity(consentEntity, consent);
    }
    
    public UserConsentModel getConsentByClient(final RealmModel realm, final String userId, final String clientInternalId) {
        final FederatedUserConsentEntity entity = this.getGrantedConsentEntity(userId, clientInternalId, LockModeType.NONE);
        return this.toConsentModel(realm, entity);
    }
    
    public Stream<UserConsentModel> getConsentsStream(final RealmModel realm, final String userId) {
        final TypedQuery<FederatedUserConsentEntity> query = (TypedQuery<FederatedUserConsentEntity>)this.em.createNamedQuery("userFederatedConsentsByUser", (Class)FederatedUserConsentEntity.class);
        query.setParameter("userId", (Object)userId);
        return (Stream<UserConsentModel>)StreamsUtil.closing((Stream)query.getResultStream().map(entity -> this.toConsentModel(realm, entity)));
    }
    
    public void updateConsent(final RealmModel realm, final String userId, final UserConsentModel consent) {
        this.createIndex(realm, userId);
        final String clientId = consent.getClient().getId();
        final FederatedUserConsentEntity consentEntity = this.getGrantedConsentEntity(userId, clientId, LockModeType.PESSIMISTIC_WRITE);
        if (consentEntity == null) {
            throw new ModelException("Consent not found for client [" + clientId + "] and user [" + userId + "]");
        }
        this.updateGrantedConsentEntity(consentEntity, consent);
    }
    
    public boolean revokeConsentForClient(final RealmModel realm, final String userId, final String clientInternalId) {
        final FederatedUserConsentEntity consentEntity = this.getGrantedConsentEntity(userId, clientInternalId, LockModeType.PESSIMISTIC_WRITE);
        if (consentEntity == null) {
            return false;
        }
        this.em.remove((Object)consentEntity);
        this.em.flush();
        return true;
    }
    
    private FederatedUserConsentEntity getGrantedConsentEntity(final String userId, final String clientId, final LockModeType lockMode) {
        final StorageId clientStorageId = new StorageId(clientId);
        final String queryName = clientStorageId.isLocal() ? "userFederatedConsentByUserAndClient" : "userFederatedConsentByUserAndExternalClient";
        final TypedQuery<FederatedUserConsentEntity> query = (TypedQuery<FederatedUserConsentEntity>)this.em.createNamedQuery(queryName, (Class)FederatedUserConsentEntity.class);
        query.setLockMode(lockMode);
        query.setParameter("userId", (Object)userId);
        if (clientStorageId.isLocal()) {
            query.setParameter("clientId", (Object)clientId);
        }
        else {
            query.setParameter("clientStorageProvider", (Object)clientStorageId.getProviderId());
            query.setParameter("externalClientId", (Object)clientStorageId.getExternalId());
        }
        final List<FederatedUserConsentEntity> results = (List<FederatedUserConsentEntity>)query.getResultList();
        if (results.size() > 1) {
            throw new ModelException("More results found for user [" + userId + "] and client [" + clientId + "]");
        }
        if (results.size() == 1) {
            return results.get(0);
        }
        return null;
    }
    
    private UserConsentModel toConsentModel(final RealmModel realm, final FederatedUserConsentEntity entity) {
        if (entity == null) {
            return null;
        }
        StorageId clientStorageId = null;
        if (entity.getClientId() == null) {
            clientStorageId = new StorageId(entity.getClientStorageProvider(), entity.getExternalClientId());
        }
        else {
            clientStorageId = new StorageId(entity.getClientId());
        }
        final ClientModel client = realm.getClientById(clientStorageId.getId());
        final UserConsentModel model = new UserConsentModel(client);
        model.setCreatedDate(entity.getCreatedDate());
        model.setLastUpdatedDate(entity.getLastUpdatedDate());
        final Collection<FederatedUserConsentClientScopeEntity> grantedClientScopeEntities = entity.getGrantedClientScopes();
        if (grantedClientScopeEntities != null) {
            for (final FederatedUserConsentClientScopeEntity grantedClientScope : grantedClientScopeEntities) {
                ClientScopeModel grantedClientScopeModel = realm.getClientScopeById(grantedClientScope.getScopeId());
                if (grantedClientScopeModel == null) {
                    grantedClientScopeModel = (ClientScopeModel)realm.getClientById(grantedClientScope.getScopeId());
                }
                if (grantedClientScopeModel != null) {
                    model.addGrantedClientScope(grantedClientScopeModel);
                }
            }
        }
        return model;
    }
    
    private void updateGrantedConsentEntity(final FederatedUserConsentEntity consentEntity, final UserConsentModel consentModel) {
        final Collection<FederatedUserConsentClientScopeEntity> grantedClientScopeEntities = consentEntity.getGrantedClientScopes();
        final Collection<FederatedUserConsentClientScopeEntity> scopesToRemove = new HashSet<FederatedUserConsentClientScopeEntity>(grantedClientScopeEntities);
        for (final ClientScopeModel clientScope : consentModel.getGrantedClientScopes()) {
            final FederatedUserConsentClientScopeEntity grantedClientScopeEntity = new FederatedUserConsentClientScopeEntity();
            grantedClientScopeEntity.setUserConsent(consentEntity);
            grantedClientScopeEntity.setScopeId(clientScope.getId());
            if (!grantedClientScopeEntities.contains(grantedClientScopeEntity)) {
                this.em.persist((Object)grantedClientScopeEntity);
                this.em.flush();
                grantedClientScopeEntities.add(grantedClientScopeEntity);
            }
            else {
                scopesToRemove.remove(grantedClientScopeEntity);
            }
        }
        for (final FederatedUserConsentClientScopeEntity toRemove : scopesToRemove) {
            grantedClientScopeEntities.remove(toRemove);
            this.em.remove((Object)toRemove);
        }
        consentEntity.setLastUpdatedDate(Time.currentTimeMillis());
        this.em.flush();
    }
    
    public void setNotBeforeForUser(final RealmModel realm, final String userId, final int notBefore) {
        final String notBeforeStr = String.valueOf(notBefore);
        this.setSingleAttribute(realm, userId, "fedNotBefore", notBeforeStr);
    }
    
    public int getNotBeforeOfUser(final RealmModel realm, final String userId) {
        final MultivaluedHashMap<String, String> attrs = this.getAttributes(realm, userId);
        final String notBeforeStr = (String)attrs.getFirst((Object)"fedNotBefore");
        return (notBeforeStr == null) ? 0 : Integer.parseInt(notBeforeStr);
    }
    
    public Stream<GroupModel> getGroupsStream(final RealmModel realm, final String userId) {
        final TypedQuery<FederatedUserGroupMembershipEntity> query = (TypedQuery<FederatedUserGroupMembershipEntity>)this.em.createNamedQuery("feduserGroupMembership", (Class)FederatedUserGroupMembershipEntity.class);
        query.setParameter("userId", (Object)userId);
        return (Stream<GroupModel>)StreamsUtil.closing((Stream)query.getResultStream().map(FederatedUserGroupMembershipEntity::getGroupId).map(realm::getGroupById));
    }
    
    public void joinGroup(final RealmModel realm, final String userId, final GroupModel group) {
        this.createIndex(realm, userId);
        final FederatedUserGroupMembershipEntity entity = new FederatedUserGroupMembershipEntity();
        entity.setUserId(userId);
        entity.setStorageProviderId(new StorageId(userId).getProviderId());
        entity.setGroupId(group.getId());
        entity.setRealmId(realm.getId());
        this.em.persist((Object)entity);
    }
    
    public void leaveGroup(final RealmModel realm, final String userId, final GroupModel group) {
        if (userId == null || group == null) {
            return;
        }
        final TypedQuery<FederatedUserGroupMembershipEntity> query1 = (TypedQuery<FederatedUserGroupMembershipEntity>)this.em.createNamedQuery("feduserMemberOf", (Class)FederatedUserGroupMembershipEntity.class);
        query1.setParameter("userId", (Object)userId);
        query1.setParameter("groupId", (Object)group.getId());
        final TypedQuery<FederatedUserGroupMembershipEntity> query2 = query1;
        query2.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        final List<FederatedUserGroupMembershipEntity> results = (List<FederatedUserGroupMembershipEntity>)query2.getResultList();
        if (results.size() == 0) {
            return;
        }
        for (final FederatedUserGroupMembershipEntity entity : results) {
            this.em.remove((Object)entity);
        }
        this.em.flush();
    }
    
    public Stream<String> getMembershipStream(final RealmModel realm, final GroupModel group, final int firstResult, final int max) {
        final TypedQuery<String> query = (TypedQuery<String>)this.em.createNamedQuery("fedgroupMembership", (Class)String.class).setParameter("realmId", (Object)realm.getId()).setParameter("groupId", (Object)group.getId());
        if (firstResult != -1) {
            query.setFirstResult(firstResult);
        }
        if (max != -1) {
            query.setMaxResults(max);
        }
        return (Stream<String>)StreamsUtil.closing(query.getResultStream());
    }
    
    public Stream<String> getRequiredActionsStream(final RealmModel realm, final String userId) {
        return this.getRequiredActionEntitiesStream(realm, userId, LockModeType.NONE).map((Function<? super FederatedUserRequiredActionEntity, ? extends String>)FederatedUserRequiredActionEntity::getAction).distinct();
    }
    
    private Stream<FederatedUserRequiredActionEntity> getRequiredActionEntitiesStream(final RealmModel realm, final String userId, final LockModeType lockMode) {
        final TypedQuery<FederatedUserRequiredActionEntity> query = (TypedQuery<FederatedUserRequiredActionEntity>)this.em.createNamedQuery("getFederatedUserRequiredActionsByUser", (Class)FederatedUserRequiredActionEntity.class).setParameter("userId", (Object)userId).setParameter("realmId", (Object)realm.getId());
        query.setLockMode(lockMode);
        return (Stream<FederatedUserRequiredActionEntity>)StreamsUtil.closing(query.getResultStream());
    }
    
    public void addRequiredAction(final RealmModel realm, final String userId, final String action) {
        final FederatedUserRequiredActionEntity.Key key = new FederatedUserRequiredActionEntity.Key(userId, action);
        if (this.em.find((Class)FederatedUserRequiredActionEntity.class, (Object)key) == null) {
            this.createIndex(realm, userId);
            final FederatedUserRequiredActionEntity entity = new FederatedUserRequiredActionEntity();
            entity.setUserId(userId);
            entity.setRealmId(realm.getId());
            entity.setStorageProviderId(new StorageId(userId).getProviderId());
            entity.setAction(action);
            this.em.persist((Object)entity);
        }
    }
    
    public void removeRequiredAction(final RealmModel realm, final String userId, final String action) {
        this.getRequiredActionEntitiesStream(realm, userId, LockModeType.PESSIMISTIC_WRITE).filter(entity -> Objects.equals(entity.getAction(), action)).collect((Collector<? super FederatedUserRequiredActionEntity, ?, List<? super FederatedUserRequiredActionEntity>>)Collectors.toList()).forEach(this.em::remove);
        this.em.flush();
    }
    
    public void grantRole(final RealmModel realm, final String userId, final RoleModel role) {
        this.createIndex(realm, userId);
        final FederatedUserRoleMappingEntity entity = new FederatedUserRoleMappingEntity();
        entity.setUserId(userId);
        entity.setStorageProviderId(new StorageId(userId).getProviderId());
        entity.setRealmId(realm.getId());
        entity.setRoleId(role.getId());
        this.em.persist((Object)entity);
    }
    
    public Stream<RoleModel> getRoleMappingsStream(final RealmModel realm, final String userId) {
        final TypedQuery<FederatedUserRoleMappingEntity> query = (TypedQuery<FederatedUserRoleMappingEntity>)this.em.createNamedQuery("feduserRoleMappings", (Class)FederatedUserRoleMappingEntity.class);
        query.setParameter("userId", (Object)userId);
        return (Stream<RoleModel>)StreamsUtil.closing((Stream)query.getResultStream().map(FederatedUserRoleMappingEntity::getRoleId).map(realm::getRoleById));
    }
    
    public void deleteRoleMapping(final RealmModel realm, final String userId, final RoleModel role) {
        final TypedQuery<FederatedUserRoleMappingEntity> query = (TypedQuery<FederatedUserRoleMappingEntity>)this.em.createNamedQuery("feduserRoleMappings", (Class)FederatedUserRoleMappingEntity.class);
        query.setParameter("userId", (Object)userId);
        final List<FederatedUserRoleMappingEntity> results = (List<FederatedUserRoleMappingEntity>)query.getResultList();
        query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        for (final FederatedUserRoleMappingEntity entity : results) {
            if (entity.getRoleId().equals(role.getId())) {
                this.em.remove((Object)entity);
            }
        }
        this.em.flush();
    }
    
    public void updateCredential(final RealmModel realm, final String userId, final CredentialModel cred) {
        final FederatedUserCredentialEntity entity = (FederatedUserCredentialEntity)this.em.find((Class)FederatedUserCredentialEntity.class, (Object)cred.getId());
        if (!this.checkCredentialEntity(entity, userId)) {
            return;
        }
        this.createIndex(realm, userId);
        entity.setCreatedDate(cred.getCreatedDate());
        entity.setType(cred.getType());
        entity.setCredentialData(cred.getCredentialData());
        entity.setSecretData(cred.getSecretData());
        cred.setUserLabel(entity.getUserLabel());
    }
    
    public CredentialModel createCredential(final RealmModel realm, final String userId, final CredentialModel cred) {
        this.createIndex(realm, userId);
        final FederatedUserCredentialEntity entity = new FederatedUserCredentialEntity();
        final String id = (cred.getId() == null) ? KeycloakModelUtils.generateId() : cred.getId();
        entity.setId(id);
        entity.setCreatedDate(cred.getCreatedDate());
        entity.setType(cred.getType());
        entity.setCredentialData(cred.getCredentialData());
        entity.setSecretData(cred.getSecretData());
        entity.setUserLabel(cred.getUserLabel());
        entity.setUserId(userId);
        entity.setRealmId(realm.getId());
        entity.setStorageProviderId(new StorageId(userId).getProviderId());
        final List<FederatedUserCredentialEntity> credentials = this.getStoredCredentialEntitiesStream(userId).collect((Collector<? super FederatedUserCredentialEntity, ?, List<FederatedUserCredentialEntity>>)Collectors.toList());
        final int priority = credentials.isEmpty() ? 10 : (credentials.get(credentials.size() - 1).getPriority() + 10);
        entity.setPriority(priority);
        this.em.persist((Object)entity);
        return this.toModel(entity);
    }
    
    public boolean removeStoredCredential(final RealmModel realm, final String userId, final String id) {
        final FederatedUserCredentialEntity entity = (FederatedUserCredentialEntity)this.em.find((Class)FederatedUserCredentialEntity.class, (Object)id, LockModeType.PESSIMISTIC_WRITE);
        if (!this.checkCredentialEntity(entity, userId)) {
            return false;
        }
        final int currentPriority = entity.getPriority();
        this.getStoredCredentialEntitiesStream(userId).filter(credentialEntity -> credentialEntity.getPriority() > currentPriority).forEach(credentialEntity -> credentialEntity.setPriority(credentialEntity.getPriority() - 10));
        this.em.remove((Object)entity);
        return true;
    }
    
    public CredentialModel getStoredCredentialById(final RealmModel realm, final String userId, final String id) {
        final FederatedUserCredentialEntity entity = (FederatedUserCredentialEntity)this.em.find((Class)FederatedUserCredentialEntity.class, (Object)id);
        if (!this.checkCredentialEntity(entity, userId)) {
            return null;
        }
        final CredentialModel model = this.toModel(entity);
        return model;
    }
    
    private boolean checkCredentialEntity(final FederatedUserCredentialEntity entity, final String userId) {
        return entity != null && entity.getUserId() != null && entity.getUserId().equals(userId);
    }
    
    protected CredentialModel toModel(final FederatedUserCredentialEntity entity) {
        final CredentialModel model = new CredentialModel();
        model.setId(entity.getId());
        model.setType(entity.getType());
        model.setCreatedDate(entity.getCreatedDate());
        model.setUserLabel(entity.getUserLabel());
        if (entity.getSalt() != null) {
            final String newSecretData = entity.getSecretData().replace("__SALT__", Base64.encodeBytes(entity.getSalt()));
            entity.setSecretData(newSecretData);
            entity.setSalt(null);
        }
        model.setSecretData(entity.getSecretData());
        model.setCredentialData(entity.getCredentialData());
        return model;
    }
    
    public Stream<CredentialModel> getStoredCredentialsStream(final RealmModel realm, final String userId) {
        return this.getStoredCredentialEntitiesStream(userId).map((Function<? super FederatedUserCredentialEntity, ? extends CredentialModel>)this::toModel);
    }
    
    private Stream<FederatedUserCredentialEntity> getStoredCredentialEntitiesStream(final String userId) {
        final TypedQuery<FederatedUserCredentialEntity> query = (TypedQuery<FederatedUserCredentialEntity>)this.em.createNamedQuery("federatedUserCredentialByUser", (Class)FederatedUserCredentialEntity.class).setParameter("userId", (Object)userId);
        return (Stream<FederatedUserCredentialEntity>)StreamsUtil.closing(query.getResultStream());
    }
    
    public Stream<CredentialModel> getStoredCredentialsByTypeStream(final RealmModel realm, final String userId, final String type) {
        final TypedQuery<FederatedUserCredentialEntity> query = (TypedQuery<FederatedUserCredentialEntity>)this.em.createNamedQuery("federatedUserCredentialByUserAndType", (Class)FederatedUserCredentialEntity.class).setParameter("type", (Object)type).setParameter("userId", (Object)userId);
        return (Stream<CredentialModel>)StreamsUtil.closing((Stream)query.getResultStream().map(this::toModel));
    }
    
    public CredentialModel getStoredCredentialByNameAndType(final RealmModel realm, final String userId, final String name, final String type) {
        final TypedQuery<FederatedUserCredentialEntity> query = (TypedQuery<FederatedUserCredentialEntity>)this.em.createNamedQuery("federatedUserCredentialByNameAndType", (Class)FederatedUserCredentialEntity.class).setParameter("type", (Object)type).setParameter("userLabel", (Object)name).setParameter("userId", (Object)userId);
        final List<FederatedUserCredentialEntity> results = (List<FederatedUserCredentialEntity>)query.getResultList();
        if (results.isEmpty()) {
            return null;
        }
        return this.toModel(results.get(0));
    }
    
    public Stream<String> getStoredUsersStream(final RealmModel realm, final int first, final int max) {
        final TypedQuery<String> query = (TypedQuery<String>)this.em.createNamedQuery("getFederatedUserIds", (Class)String.class).setParameter("realmId", (Object)realm.getId());
        if (first > 0) {
            query.setFirstResult(first);
        }
        if (max > 0) {
            query.setMaxResults(max);
        }
        return (Stream<String>)StreamsUtil.closing(query.getResultStream());
    }
    
    public void updateCredential(final RealmModel realm, final UserModel user, final CredentialModel cred) {
        this.updateCredential(realm, user.getId(), cred);
    }
    
    public CredentialModel createCredential(final RealmModel realm, final UserModel user, final CredentialModel cred) {
        return this.createCredential(realm, user.getId(), cred);
    }
    
    public boolean removeStoredCredential(final RealmModel realm, final UserModel user, final String id) {
        return this.removeStoredCredential(realm, user.getId(), id);
    }
    
    public CredentialModel getStoredCredentialById(final RealmModel realm, final UserModel user, final String id) {
        return this.getStoredCredentialById(realm, user.getId(), id);
    }
    
    public Stream<CredentialModel> getStoredCredentialsStream(final RealmModel realm, final UserModel user) {
        return this.getStoredCredentialsStream(realm, user.getId());
    }
    
    public Stream<CredentialModel> getStoredCredentialsByTypeStream(final RealmModel realm, final UserModel user, final String type) {
        return this.getStoredCredentialsByTypeStream(realm, user.getId(), type);
    }
    
    public CredentialModel getStoredCredentialByNameAndType(final RealmModel realm, final UserModel user, final String name, final String type) {
        return this.getStoredCredentialByNameAndType(realm, user.getId(), name, type);
    }
    
    public boolean moveCredentialTo(final RealmModel realm, final UserModel user, final String id, final String newPreviousCredentialId) {
        final List<FederatedUserCredentialEntity> newList = this.getStoredCredentialEntitiesStream(user.getId()).collect((Collector<? super FederatedUserCredentialEntity, ?, List<FederatedUserCredentialEntity>>)Collectors.toList());
        int ourCredentialIndex = -1;
        int newPreviousCredentialIndex = -1;
        FederatedUserCredentialEntity ourCredential = null;
        int i = 0;
        for (final FederatedUserCredentialEntity credential : newList) {
            if (id.equals(credential.getId())) {
                ourCredentialIndex = i;
                ourCredential = credential;
            }
            else if (newPreviousCredentialId != null && newPreviousCredentialId.equals(credential.getId())) {
                newPreviousCredentialIndex = i;
            }
            ++i;
        }
        if (ourCredentialIndex == -1) {
            JpaUserFederatedStorageProvider.logger.warnf("Not found credential with id [%s] of user [%s]", (Object)id, (Object)user.getUsername());
            return false;
        }
        if (newPreviousCredentialId != null && newPreviousCredentialIndex == -1) {
            JpaUserFederatedStorageProvider.logger.warnf("Can't move up credential with id [%s] of user [%s]", (Object)id, (Object)user.getUsername());
            return false;
        }
        final int toMoveIndex = (newPreviousCredentialId == null) ? 0 : (newPreviousCredentialIndex + 1);
        newList.add(toMoveIndex, ourCredential);
        final int indexToRemove = (toMoveIndex < ourCredentialIndex) ? (ourCredentialIndex + 1) : ourCredentialIndex;
        newList.remove(indexToRemove);
        int expectedPriority = 0;
        for (final FederatedUserCredentialEntity credential2 : newList) {
            expectedPriority += 10;
            if (credential2.getPriority() != expectedPriority) {
                credential2.setPriority(expectedPriority);
                JpaUserFederatedStorageProvider.logger.tracef("Priority of credential [%s] of user [%s] changed to [%d]", (Object)credential2.getId(), (Object)user.getUsername(), (Object)expectedPriority);
            }
        }
        return true;
    }
    
    public int getStoredUsersCount(final RealmModel realm) {
        final Object count = this.em.createNamedQuery("getFederatedUserCount").setParameter("realmId", (Object)realm.getId()).getSingleResult();
        return ((Number)count).intValue();
    }
    
    public void preRemove(final RealmModel realm) {
        int num = this.em.createNamedQuery("deleteFederatedUserConsentClientScopesByRealm").setParameter("realmId", (Object)realm.getId()).executeUpdate();
        num = this.em.createNamedQuery("deleteFederatedUserConsentsByRealm").setParameter("realmId", (Object)realm.getId()).executeUpdate();
        num = this.em.createNamedQuery("deleteFederatedUserRoleMappingsByRealm").setParameter("realmId", (Object)realm.getId()).executeUpdate();
        num = this.em.createNamedQuery("deleteFederatedUserRequiredActionsByRealm").setParameter("realmId", (Object)realm.getId()).executeUpdate();
        num = this.em.createNamedQuery("deleteBrokerLinkByRealm").setParameter("realmId", (Object)realm.getId()).executeUpdate();
        num = this.em.createNamedQuery("deleteFederatedUserCredentialsByRealm").setParameter("realmId", (Object)realm.getId()).executeUpdate();
        num = this.em.createNamedQuery("deleteUserFederatedAttributesByRealm").setParameter("realmId", (Object)realm.getId()).executeUpdate();
        num = this.em.createNamedQuery("deleteFederatedUserGroupMembershipByRealm").setParameter("realmId", (Object)realm.getId()).executeUpdate();
        num = this.em.createNamedQuery("deleteFederatedUsersByRealm").setParameter("realmId", (Object)realm.getId()).executeUpdate();
    }
    
    public void preRemove(final RealmModel realm, final RoleModel role) {
        this.em.createNamedQuery("deleteFederatedUserRoleMappingsByRole").setParameter("roleId", (Object)role.getId()).executeUpdate();
    }
    
    public void preRemove(final RealmModel realm, final GroupModel group) {
        this.em.createNamedQuery("deleteFederatedUserGroupMembershipsByGroup").setParameter("groupId", (Object)group.getId()).executeUpdate();
    }
    
    public void preRemove(final RealmModel realm, final ClientModel client) {
        final StorageId clientStorageId = new StorageId(client.getId());
        if (clientStorageId.isLocal()) {
            this.em.createNamedQuery("deleteFederatedUserConsentClientScopesByClient").setParameter("clientId", (Object)client.getId()).executeUpdate();
            this.em.createNamedQuery("deleteFederatedUserConsentsByClient").setParameter("clientId", (Object)client.getId()).executeUpdate();
        }
        else {
            this.em.createNamedQuery("deleteFederatedUserConsentClientScopesByExternalClient").setParameter("clientStorageProvider", (Object)clientStorageId.getProviderId()).setParameter("externalClientId", (Object)clientStorageId.getExternalId()).executeUpdate();
            this.em.createNamedQuery("deleteFederatedUserConsentsByExternalClient").setParameter("clientStorageProvider", (Object)clientStorageId.getProviderId()).setParameter("externalClientId", (Object)clientStorageId.getExternalId()).executeUpdate();
        }
    }
    
    public void preRemove(final ProtocolMapperModel protocolMapper) {
    }
    
    public void preRemove(final ClientScopeModel clientScope) {
        this.em.createNamedQuery("deleteFederatedUserConsentClientScopesByClientScope").setParameter("scopeId", (Object)clientScope.getId()).executeUpdate();
    }
    
    public void preRemove(final RealmModel realm, final UserModel user) {
        this.em.createNamedQuery("deleteBrokerLinkByUser").setParameter("userId", (Object)user.getId()).setParameter("realmId", (Object)realm.getId()).executeUpdate();
        this.em.createNamedQuery("deleteUserFederatedAttributesByUser").setParameter("userId", (Object)user.getId()).setParameter("realmId", (Object)realm.getId()).executeUpdate();
        this.em.createNamedQuery("deleteFederatedUserConsentClientScopesByUser").setParameter("userId", (Object)user.getId()).setParameter("realmId", (Object)realm.getId()).executeUpdate();
        this.em.createNamedQuery("deleteFederatedUserConsentsByUser").setParameter("userId", (Object)user.getId()).setParameter("realmId", (Object)realm.getId()).executeUpdate();
        this.em.createNamedQuery("deleteFederatedUserCredentialByUser").setParameter("userId", (Object)user.getId()).setParameter("realmId", (Object)realm.getId()).executeUpdate();
        this.em.createNamedQuery("deleteFederatedUserGroupMembershipsByUser").setParameter("userId", (Object)user.getId()).setParameter("realmId", (Object)realm.getId()).executeUpdate();
        this.em.createNamedQuery("deleteFederatedUserRequiredActionsByUser").setParameter("userId", (Object)user.getId()).setParameter("realmId", (Object)realm.getId()).executeUpdate();
        this.em.createNamedQuery("deleteFederatedUserRoleMappingsByUser").setParameter("userId", (Object)user.getId()).setParameter("realmId", (Object)realm.getId()).executeUpdate();
        this.em.createNamedQuery("deleteFederatedUserByUser").setParameter("userId", (Object)user.getId()).setParameter("realmId", (Object)realm.getId()).executeUpdate();
    }
    
    public void preRemove(final RealmModel realm, final ComponentModel model) {
        if (model.getProviderType().equals(UserStorageProvider.class.getName())) {
            this.em.createNamedQuery("deleteBrokerLinkByStorageProvider").setParameter("storageProviderId", (Object)model.getId()).executeUpdate();
            this.em.createNamedQuery("deleteFederatedAttributesByStorageProvider").setParameter("storageProviderId", (Object)model.getId()).executeUpdate();
            this.em.createNamedQuery("deleteFederatedUserConsentClientScopesByStorageProvider").setParameter("storageProviderId", (Object)model.getId()).executeUpdate();
            this.em.createNamedQuery("deleteFederatedUserConsentsByStorageProvider").setParameter("storageProviderId", (Object)model.getId()).executeUpdate();
            this.em.createNamedQuery("deleteFederatedUserCredentialsByStorageProvider").setParameter("storageProviderId", (Object)model.getId()).executeUpdate();
            this.em.createNamedQuery("deleteFederatedUserGroupMembershipByStorageProvider").setParameter("storageProviderId", (Object)model.getId()).executeUpdate();
            this.em.createNamedQuery("deleteFederatedUserRequiredActionsByStorageProvider").setParameter("storageProviderId", (Object)model.getId()).executeUpdate();
            this.em.createNamedQuery("deleteFederatedUserRoleMappingsByStorageProvider").setParameter("storageProviderId", (Object)model.getId()).executeUpdate();
            this.em.createNamedQuery("deleteFederatedUsersByStorageProvider").setParameter("storageProviderId", (Object)model.getId()).executeUpdate();
        }
        else if (model.getProviderType().equals(ClientStorageProvider.class.getName())) {
            this.em.createNamedQuery("deleteFederatedUserConsentClientScopesByClientStorageProvider").setParameter("clientStorageProvider", (Object)model.getId()).executeUpdate();
            this.em.createNamedQuery("deleteFederatedUserConsentsByClientStorageProvider").setParameter("clientStorageProvider", (Object)model.getId()).executeUpdate();
        }
    }
    
    static {
        logger = Logger.getLogger((Class)JpaUserFederatedStorageProvider.class);
    }
}
