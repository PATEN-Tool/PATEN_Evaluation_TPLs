// 
// Decompiled by Procyon v0.5.36
// 

package org.keycloak.storage.jpa;

import org.keycloak.storage.UserStorageProvider;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.UserModel;
import java.util.LinkedList;
import org.keycloak.storage.jpa.entity.FederatedUserCredentialAttributeEntity;
import org.keycloak.storage.jpa.entity.FederatedUserCredentialEntity;
import org.keycloak.credential.CredentialModel;
import org.keycloak.storage.jpa.entity.FederatedUserRoleMappingEntity;
import org.keycloak.storage.jpa.entity.FederatedUserRequiredActionEntity;
import org.keycloak.storage.jpa.entity.FederatedUserGroupMembershipEntity;
import org.keycloak.models.GroupModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RoleModel;
import java.util.Collection;
import org.keycloak.models.ClientModel;
import org.keycloak.storage.jpa.entity.FederatedUserConsentProtocolMapperEntity;
import org.keycloak.storage.jpa.entity.FederatedUserConsentRoleEntity;
import org.keycloak.models.ModelException;
import java.util.ArrayList;
import org.keycloak.common.util.Time;
import org.keycloak.storage.jpa.entity.FederatedUserConsentEntity;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.UserConsentModel;
import java.util.HashSet;
import java.util.Set;
import org.keycloak.storage.jpa.entity.BrokerLinkEntity;
import org.keycloak.models.FederatedIdentityModel;
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
import org.keycloak.credential.UserCredentialStore;
import org.keycloak.storage.federated.UserFederatedStorageProvider;

public class JpaUserFederatedStorageProvider implements UserFederatedStorageProvider, UserCredentialStore
{
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
    
    public List<String> getUsersByUserAttribute(final RealmModel realm, final String name, final String value) {
        final TypedQuery<String> query = (TypedQuery<String>)this.em.createNamedQuery("getFederatedAttributesByNameAndValue", (Class)String.class).setParameter("realmId", (Object)realm.getId()).setParameter("name", (Object)name).setParameter("value", (Object)value);
        return (List<String>)query.getResultList();
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
    
    public Set<FederatedIdentityModel> getFederatedIdentities(final String userId, final RealmModel realm) {
        final TypedQuery<BrokerLinkEntity> query = (TypedQuery<BrokerLinkEntity>)this.em.createNamedQuery("findBrokerLinkByUser", (Class)BrokerLinkEntity.class).setParameter("userId", (Object)userId);
        final List<BrokerLinkEntity> results = (List<BrokerLinkEntity>)query.getResultList();
        final Set<FederatedIdentityModel> set = new HashSet<FederatedIdentityModel>();
        for (final BrokerLinkEntity entity : results) {
            final FederatedIdentityModel model = new FederatedIdentityModel(entity.getIdentityProvider(), entity.getBrokerUserId(), entity.getBrokerUserName(), entity.getToken());
            set.add(model);
        }
        return set;
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
        FederatedUserConsentEntity consentEntity = this.getGrantedConsentEntity(userId, clientId);
        if (consentEntity != null) {
            throw new ModelDuplicateException("Consent already exists for client [" + clientId + "] and user [" + userId + "]");
        }
        consentEntity = new FederatedUserConsentEntity();
        consentEntity.setId(KeycloakModelUtils.generateId());
        consentEntity.setUserId(userId);
        consentEntity.setClientId(clientId);
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
        final FederatedUserConsentEntity entity = this.getGrantedConsentEntity(userId, clientInternalId);
        return this.toConsentModel(realm, entity);
    }
    
    public List<UserConsentModel> getConsents(final RealmModel realm, final String userId) {
        final TypedQuery<FederatedUserConsentEntity> query = (TypedQuery<FederatedUserConsentEntity>)this.em.createNamedQuery("userFederatedConsentsByUser", (Class)FederatedUserConsentEntity.class);
        query.setParameter("userId", (Object)userId);
        final List<FederatedUserConsentEntity> results = (List<FederatedUserConsentEntity>)query.getResultList();
        final List<UserConsentModel> consents = new ArrayList<UserConsentModel>();
        for (final FederatedUserConsentEntity entity : results) {
            final UserConsentModel model = this.toConsentModel(realm, entity);
            consents.add(model);
        }
        return consents;
    }
    
    public void updateConsent(final RealmModel realm, final String userId, final UserConsentModel consent) {
        this.createIndex(realm, userId);
        final String clientId = consent.getClient().getId();
        final FederatedUserConsentEntity consentEntity = this.getGrantedConsentEntity(userId, clientId);
        if (consentEntity == null) {
            throw new ModelException("Consent not found for client [" + clientId + "] and user [" + userId + "]");
        }
        this.updateGrantedConsentEntity(consentEntity, consent);
    }
    
    public boolean revokeConsentForClient(final RealmModel realm, final String userId, final String clientInternalId) {
        final FederatedUserConsentEntity consentEntity = this.getGrantedConsentEntity(userId, clientInternalId);
        if (consentEntity == null) {
            return false;
        }
        this.em.remove((Object)consentEntity);
        this.em.flush();
        return true;
    }
    
    private FederatedUserConsentEntity getGrantedConsentEntity(final String userId, final String clientId) {
        final TypedQuery<FederatedUserConsentEntity> query = (TypedQuery<FederatedUserConsentEntity>)this.em.createNamedQuery("userFederatedConsentByUserAndClient", (Class)FederatedUserConsentEntity.class);
        query.setParameter("userId", (Object)userId);
        query.setParameter("clientId", (Object)clientId);
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
        final ClientModel client = realm.getClientById(entity.getClientId());
        if (client == null) {
            throw new ModelException("Client with id " + entity.getClientId() + " is not available");
        }
        final UserConsentModel model = new UserConsentModel(client);
        model.setCreatedDate(entity.getCreatedDate());
        model.setLastUpdatedDate(entity.getLastUpdatedDate());
        final Collection<FederatedUserConsentRoleEntity> grantedRoleEntities = entity.getGrantedRoles();
        if (grantedRoleEntities != null) {
            for (final FederatedUserConsentRoleEntity grantedRole : grantedRoleEntities) {
                final RoleModel grantedRoleModel = realm.getRoleById(grantedRole.getRoleId());
                if (grantedRoleModel != null) {
                    model.addGrantedRole(grantedRoleModel);
                }
            }
        }
        final Collection<FederatedUserConsentProtocolMapperEntity> grantedProtocolMapperEntities = entity.getGrantedProtocolMappers();
        if (grantedProtocolMapperEntities != null) {
            for (final FederatedUserConsentProtocolMapperEntity grantedProtMapper : grantedProtocolMapperEntities) {
                final ProtocolMapperModel protocolMapper = client.getProtocolMapperById(grantedProtMapper.getProtocolMapperId());
                model.addGrantedProtocolMapper(protocolMapper);
            }
        }
        return model;
    }
    
    private void updateGrantedConsentEntity(final FederatedUserConsentEntity consentEntity, final UserConsentModel consentModel) {
        final Collection<FederatedUserConsentProtocolMapperEntity> grantedProtocolMapperEntities = consentEntity.getGrantedProtocolMappers();
        final Collection<FederatedUserConsentProtocolMapperEntity> mappersToRemove = new HashSet<FederatedUserConsentProtocolMapperEntity>(grantedProtocolMapperEntities);
        for (final ProtocolMapperModel protocolMapper : consentModel.getGrantedProtocolMappers()) {
            final FederatedUserConsentProtocolMapperEntity grantedProtocolMapperEntity = new FederatedUserConsentProtocolMapperEntity();
            grantedProtocolMapperEntity.setUserConsent(consentEntity);
            grantedProtocolMapperEntity.setProtocolMapperId(protocolMapper.getId());
            if (!grantedProtocolMapperEntities.contains(grantedProtocolMapperEntity)) {
                this.em.persist((Object)grantedProtocolMapperEntity);
                this.em.flush();
                grantedProtocolMapperEntities.add(grantedProtocolMapperEntity);
            }
            else {
                mappersToRemove.remove(grantedProtocolMapperEntity);
            }
        }
        for (final FederatedUserConsentProtocolMapperEntity toRemove : mappersToRemove) {
            grantedProtocolMapperEntities.remove(toRemove);
            this.em.remove((Object)toRemove);
        }
        final Collection<FederatedUserConsentRoleEntity> grantedRoleEntities = consentEntity.getGrantedRoles();
        final Set<FederatedUserConsentRoleEntity> rolesToRemove = new HashSet<FederatedUserConsentRoleEntity>(grantedRoleEntities);
        for (final RoleModel role : consentModel.getGrantedRoles()) {
            final FederatedUserConsentRoleEntity consentRoleEntity = new FederatedUserConsentRoleEntity();
            consentRoleEntity.setUserConsent(consentEntity);
            consentRoleEntity.setRoleId(role.getId());
            if (!grantedRoleEntities.contains(consentRoleEntity)) {
                this.em.persist((Object)consentRoleEntity);
                this.em.flush();
                grantedRoleEntities.add(consentRoleEntity);
            }
            else {
                rolesToRemove.remove(consentRoleEntity);
            }
        }
        for (final FederatedUserConsentRoleEntity toRemove2 : rolesToRemove) {
            grantedRoleEntities.remove(toRemove2);
            this.em.remove((Object)toRemove2);
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
    
    public Set<GroupModel> getGroups(final RealmModel realm, final String userId) {
        final Set<GroupModel> set = new HashSet<GroupModel>();
        final TypedQuery<FederatedUserGroupMembershipEntity> query = (TypedQuery<FederatedUserGroupMembershipEntity>)this.em.createNamedQuery("feduserGroupMembership", (Class)FederatedUserGroupMembershipEntity.class);
        query.setParameter("userId", (Object)userId);
        final List<FederatedUserGroupMembershipEntity> results = (List<FederatedUserGroupMembershipEntity>)query.getResultList();
        if (results.size() == 0) {
            return set;
        }
        for (final FederatedUserGroupMembershipEntity entity : results) {
            final GroupModel group = realm.getGroupById(entity.getGroupId());
            set.add(group);
        }
        return set;
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
        final List<FederatedUserGroupMembershipEntity> results = (List<FederatedUserGroupMembershipEntity>)query2.getResultList();
        if (results.size() == 0) {
            return;
        }
        for (final FederatedUserGroupMembershipEntity entity : results) {
            this.em.remove((Object)entity);
        }
        this.em.flush();
    }
    
    public List<String> getMembership(final RealmModel realm, final GroupModel group, final int firstResult, final int max) {
        final TypedQuery<String> query = (TypedQuery<String>)this.em.createNamedQuery("fedgroupMembership", (Class)String.class).setParameter("realmId", (Object)realm.getId()).setParameter("groupId", (Object)group.getId());
        query.setFirstResult(firstResult);
        query.setMaxResults(max);
        return (List<String>)query.getResultList();
    }
    
    public Set<String> getRequiredActions(final RealmModel realm, final String userId) {
        final Set<String> set = new HashSet<String>();
        final List<FederatedUserRequiredActionEntity> values = this.getRequiredActionEntities(realm, userId);
        for (final FederatedUserRequiredActionEntity entity : values) {
            set.add(entity.getAction());
        }
        return set;
    }
    
    private List<FederatedUserRequiredActionEntity> getRequiredActionEntities(final RealmModel realm, final String userId) {
        final TypedQuery<FederatedUserRequiredActionEntity> query = (TypedQuery<FederatedUserRequiredActionEntity>)this.em.createNamedQuery("getFederatedUserRequiredActionsByUser", (Class)FederatedUserRequiredActionEntity.class).setParameter("userId", (Object)userId).setParameter("realmId", (Object)realm.getId());
        return (List<FederatedUserRequiredActionEntity>)query.getResultList();
    }
    
    public void addRequiredAction(final RealmModel realm, final String userId, final String action) {
        this.createIndex(realm, userId);
        final FederatedUserRequiredActionEntity entity = new FederatedUserRequiredActionEntity();
        entity.setUserId(userId);
        entity.setRealmId(realm.getId());
        entity.setStorageProviderId(new StorageId(userId).getProviderId());
        entity.setAction(action);
        this.em.persist((Object)entity);
    }
    
    public void removeRequiredAction(final RealmModel realm, final String userId, final String action) {
        final List<FederatedUserRequiredActionEntity> values = this.getRequiredActionEntities(realm, userId);
        for (final FederatedUserRequiredActionEntity entity : values) {
            if (action.equals(entity.getAction())) {
                this.em.remove((Object)entity);
            }
        }
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
    
    public Set<RoleModel> getRoleMappings(final RealmModel realm, final String userId) {
        final Set<RoleModel> set = new HashSet<RoleModel>();
        final TypedQuery<FederatedUserRoleMappingEntity> query = (TypedQuery<FederatedUserRoleMappingEntity>)this.em.createNamedQuery("feduserRoleMappings", (Class)FederatedUserRoleMappingEntity.class);
        query.setParameter("userId", (Object)userId);
        final List<FederatedUserRoleMappingEntity> results = (List<FederatedUserRoleMappingEntity>)query.getResultList();
        if (results.size() == 0) {
            return set;
        }
        for (final FederatedUserRoleMappingEntity entity : results) {
            final RoleModel role = realm.getRoleById(entity.getRoleId());
            set.add(role);
        }
        return set;
    }
    
    public void deleteRoleMapping(final RealmModel realm, final String userId, final RoleModel role) {
        final TypedQuery<FederatedUserRoleMappingEntity> query = (TypedQuery<FederatedUserRoleMappingEntity>)this.em.createNamedQuery("feduserRoleMappings", (Class)FederatedUserRoleMappingEntity.class);
        query.setParameter("userId", (Object)userId);
        final List<FederatedUserRoleMappingEntity> results = (List<FederatedUserRoleMappingEntity>)query.getResultList();
        for (final FederatedUserRoleMappingEntity entity : results) {
            if (entity.getRoleId().equals(role.getId())) {
                this.em.remove((Object)entity);
            }
        }
        this.em.flush();
    }
    
    public void updateCredential(final RealmModel realm, final String userId, final CredentialModel cred) {
        final FederatedUserCredentialEntity entity = (FederatedUserCredentialEntity)this.em.find((Class)FederatedUserCredentialEntity.class, (Object)cred.getId());
        if (entity == null) {
            return;
        }
        this.createIndex(realm, userId);
        entity.setAlgorithm(cred.getAlgorithm());
        entity.setCounter(cred.getCounter());
        entity.setCreatedDate(cred.getCreatedDate());
        entity.setDevice(cred.getDevice());
        entity.setDigits(cred.getDigits());
        entity.setHashIterations(cred.getHashIterations());
        entity.setPeriod(cred.getPeriod());
        entity.setSalt(cred.getSalt());
        entity.setType(cred.getType());
        entity.setValue(cred.getValue());
        if (entity.getCredentialAttributes().isEmpty()) {
            if (cred.getConfig() == null) {
                return;
            }
            if (cred.getConfig().isEmpty()) {
                return;
            }
        }
        final MultivaluedHashMap<String, String> attrs = (MultivaluedHashMap<String, String>)new MultivaluedHashMap();
        MultivaluedHashMap<String, String> config = (MultivaluedHashMap<String, String>)cred.getConfig();
        if (config == null) {
            config = (MultivaluedHashMap<String, String>)new MultivaluedHashMap();
        }
        final Iterator<FederatedUserCredentialAttributeEntity> it = entity.getCredentialAttributes().iterator();
        while (it.hasNext()) {
            final FederatedUserCredentialAttributeEntity attr = it.next();
            final List<String> values = (List<String>)config.getList((Object)attr.getName());
            if (values == null || !values.contains(attr.getValue())) {
                this.em.remove((Object)attr);
                it.remove();
            }
            else {
                attrs.add((Object)attr.getName(), (Object)attr.getValue());
            }
        }
        for (final String key : config.keySet()) {
            final List<String> values2 = (List<String>)config.getList((Object)key);
            final List<String> attrValues = (List<String>)attrs.getList((Object)key);
            for (final String val : values2) {
                if (attrValues == null || !attrValues.contains(val)) {
                    final FederatedUserCredentialAttributeEntity attr2 = new FederatedUserCredentialAttributeEntity();
                    attr2.setId(KeycloakModelUtils.generateId());
                    attr2.setValue(val);
                    attr2.setName(key);
                    attr2.setCredential(entity);
                    this.em.persist((Object)attr2);
                    entity.getCredentialAttributes().add(attr2);
                }
            }
        }
    }
    
    public CredentialModel createCredential(final RealmModel realm, final String userId, final CredentialModel cred) {
        this.createIndex(realm, userId);
        final FederatedUserCredentialEntity entity = new FederatedUserCredentialEntity();
        final String id = (cred.getId() == null) ? KeycloakModelUtils.generateId() : cred.getId();
        entity.setId(id);
        entity.setAlgorithm(cred.getAlgorithm());
        entity.setCounter(cred.getCounter());
        entity.setCreatedDate(cred.getCreatedDate());
        entity.setDevice(cred.getDevice());
        entity.setDigits(cred.getDigits());
        entity.setHashIterations(cred.getHashIterations());
        entity.setPeriod(cred.getPeriod());
        entity.setSalt(cred.getSalt());
        entity.setType(cred.getType());
        entity.setValue(cred.getValue());
        entity.setUserId(userId);
        entity.setRealmId(realm.getId());
        entity.setStorageProviderId(new StorageId(userId).getProviderId());
        this.em.persist((Object)entity);
        final MultivaluedHashMap<String, String> config = (MultivaluedHashMap<String, String>)cred.getConfig();
        if (config != null && !config.isEmpty()) {
            for (final String key : config.keySet()) {
                final List<String> values = (List<String>)config.getList((Object)key);
                for (final String val : values) {
                    final FederatedUserCredentialAttributeEntity attr = new FederatedUserCredentialAttributeEntity();
                    attr.setId(KeycloakModelUtils.generateId());
                    attr.setValue(val);
                    attr.setName(key);
                    attr.setCredential(entity);
                    this.em.persist((Object)attr);
                    entity.getCredentialAttributes().add(attr);
                }
            }
        }
        return this.toModel(entity);
    }
    
    public boolean removeStoredCredential(final RealmModel realm, final String userId, final String id) {
        final FederatedUserCredentialEntity entity = (FederatedUserCredentialEntity)this.em.find((Class)FederatedUserCredentialEntity.class, (Object)id);
        if (entity == null) {
            return false;
        }
        this.em.remove((Object)entity);
        return true;
    }
    
    public CredentialModel getStoredCredentialById(final RealmModel realm, final String userId, final String id) {
        final FederatedUserCredentialEntity entity = (FederatedUserCredentialEntity)this.em.find((Class)FederatedUserCredentialEntity.class, (Object)id);
        if (entity == null) {
            return null;
        }
        final CredentialModel model = this.toModel(entity);
        return model;
    }
    
    protected CredentialModel toModel(final FederatedUserCredentialEntity entity) {
        final CredentialModel model = new CredentialModel();
        model.setId(entity.getId());
        model.setType(entity.getType());
        model.setValue(entity.getValue());
        model.setAlgorithm(entity.getAlgorithm());
        model.setSalt(entity.getSalt());
        model.setPeriod(entity.getPeriod());
        model.setCounter(entity.getCounter());
        model.setCreatedDate(entity.getCreatedDate());
        model.setDevice(entity.getDevice());
        model.setDigits(entity.getDigits());
        model.setHashIterations(entity.getHashIterations());
        final MultivaluedHashMap<String, String> config = (MultivaluedHashMap<String, String>)new MultivaluedHashMap();
        model.setConfig((MultivaluedHashMap)config);
        for (final FederatedUserCredentialAttributeEntity attr : entity.getCredentialAttributes()) {
            config.add((Object)attr.getName(), (Object)attr.getValue());
        }
        return model;
    }
    
    public List<CredentialModel> getStoredCredentials(final RealmModel realm, final String userId) {
        final TypedQuery<FederatedUserCredentialEntity> query = (TypedQuery<FederatedUserCredentialEntity>)this.em.createNamedQuery("federatedUserCredentialByUser", (Class)FederatedUserCredentialEntity.class).setParameter("userId", (Object)userId);
        final List<FederatedUserCredentialEntity> results = (List<FederatedUserCredentialEntity>)query.getResultList();
        final List<CredentialModel> rtn = new LinkedList<CredentialModel>();
        for (final FederatedUserCredentialEntity entity : results) {
            rtn.add(this.toModel(entity));
        }
        return rtn;
    }
    
    public List<CredentialModel> getStoredCredentialsByType(final RealmModel realm, final String userId, final String type) {
        final TypedQuery<FederatedUserCredentialEntity> query = (TypedQuery<FederatedUserCredentialEntity>)this.em.createNamedQuery("federatedUserCredentialByUserAndType", (Class)FederatedUserCredentialEntity.class).setParameter("type", (Object)type).setParameter("userId", (Object)userId);
        final List<FederatedUserCredentialEntity> results = (List<FederatedUserCredentialEntity>)query.getResultList();
        final List<CredentialModel> rtn = new LinkedList<CredentialModel>();
        for (final FederatedUserCredentialEntity entity : results) {
            rtn.add(this.toModel(entity));
        }
        return rtn;
    }
    
    public CredentialModel getStoredCredentialByNameAndType(final RealmModel realm, final String userId, final String name, final String type) {
        final TypedQuery<FederatedUserCredentialEntity> query = (TypedQuery<FederatedUserCredentialEntity>)this.em.createNamedQuery("federatedUserCredentialByNameAndType", (Class)FederatedUserCredentialEntity.class).setParameter("type", (Object)type).setParameter("device", (Object)name).setParameter("userId", (Object)userId);
        final List<FederatedUserCredentialEntity> results = (List<FederatedUserCredentialEntity>)query.getResultList();
        if (results.isEmpty()) {
            return null;
        }
        return this.toModel(results.get(0));
    }
    
    public List<String> getStoredUsers(final RealmModel realm, final int first, final int max) {
        final TypedQuery<String> query = (TypedQuery<String>)this.em.createNamedQuery("getFederatedUserIds", (Class)String.class).setParameter("realmId", (Object)realm.getId()).setFirstResult(first);
        if (max > 0) {
            query.setMaxResults(max);
        }
        return (List<String>)query.getResultList();
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
    
    public List<CredentialModel> getStoredCredentials(final RealmModel realm, final UserModel user) {
        return this.getStoredCredentials(realm, user.getId());
    }
    
    public List<CredentialModel> getStoredCredentialsByType(final RealmModel realm, final UserModel user, final String type) {
        return this.getStoredCredentialsByType(realm, user.getId(), type);
    }
    
    public CredentialModel getStoredCredentialByNameAndType(final RealmModel realm, final UserModel user, final String name, final String type) {
        return this.getStoredCredentialByNameAndType(realm, user.getId(), name, type);
    }
    
    public int getStoredUsersCount(final RealmModel realm) {
        final Object count = this.em.createNamedQuery("getFederatedUserCount").setParameter("realmId", (Object)realm.getId()).getSingleResult();
        return ((Number)count).intValue();
    }
    
    public void preRemove(final RealmModel realm) {
        int num = this.em.createNamedQuery("deleteFederatedUserConsentRolesByRealm").setParameter("realmId", (Object)realm.getId()).executeUpdate();
        num = this.em.createNamedQuery("deleteFederatedUserConsentProtMappersByRealm").setParameter("realmId", (Object)realm.getId()).executeUpdate();
        num = this.em.createNamedQuery("deleteFederatedUserConsentsByRealm").setParameter("realmId", (Object)realm.getId()).executeUpdate();
        num = this.em.createNamedQuery("deleteFederatedUserRoleMappingsByRealm").setParameter("realmId", (Object)realm.getId()).executeUpdate();
        num = this.em.createNamedQuery("deleteFederatedUserRequiredActionsByRealm").setParameter("realmId", (Object)realm.getId()).executeUpdate();
        num = this.em.createNamedQuery("deleteBrokerLinkByRealm").setParameter("realmId", (Object)realm.getId()).executeUpdate();
        num = this.em.createNamedQuery("deleteFederatedCredentialAttributeByRealm").setParameter("realmId", (Object)realm.getId()).executeUpdate();
        num = this.em.createNamedQuery("deleteFederatedUserCredentialsByRealm").setParameter("realmId", (Object)realm.getId()).executeUpdate();
        num = this.em.createNamedQuery("deleteUserFederatedAttributesByRealm").setParameter("realmId", (Object)realm.getId()).executeUpdate();
        num = this.em.createNamedQuery("deleteFederatedUserGroupMembershipByRealm").setParameter("realmId", (Object)realm.getId()).executeUpdate();
        num = this.em.createNamedQuery("deleteFederatedUsersByRealm").setParameter("realmId", (Object)realm.getId()).executeUpdate();
    }
    
    public void preRemove(final RealmModel realm, final RoleModel role) {
        this.em.createNamedQuery("deleteFederatedUserRoleMappingsByRole").setParameter("roleId", (Object)role.getId()).executeUpdate();
        this.em.createNamedQuery("deleteFederatedUserRoleMappingsByRole").setParameter("roleId", (Object)role.getId()).executeUpdate();
    }
    
    public void preRemove(final RealmModel realm, final GroupModel group) {
        this.em.createNamedQuery("deleteFederatedUserGroupMembershipsByGroup").setParameter("groupId", (Object)group.getId()).executeUpdate();
    }
    
    public void preRemove(final RealmModel realm, final ClientModel client) {
        this.em.createNamedQuery("deleteFederatedUserConsentProtMappersByClient").setParameter("clientId", (Object)client.getId()).executeUpdate();
        this.em.createNamedQuery("deleteFederatedUserConsentRolesByClient").setParameter("clientId", (Object)client.getId()).executeUpdate();
        this.em.createNamedQuery("deleteFederatedUserConsentsByClient").setParameter("clientId", (Object)client.getId()).executeUpdate();
    }
    
    public void preRemove(final ProtocolMapperModel protocolMapper) {
        this.em.createNamedQuery("deleteFederatedUserConsentProtMappersByProtocolMapper").setParameter("protocolMapperId", (Object)protocolMapper.getId()).executeUpdate();
    }
    
    public void preRemove(final RealmModel realm, final UserModel user) {
        this.em.createNamedQuery("deleteBrokerLinkByUser").setParameter("userId", (Object)user.getId()).setParameter("realmId", (Object)realm.getId()).executeUpdate();
        this.em.createNamedQuery("deleteUserFederatedAttributesByUser").setParameter("userId", (Object)user.getId()).setParameter("realmId", (Object)realm.getId()).executeUpdate();
        this.em.createNamedQuery("deleteFederatedUserConsentProtMappersByUser").setParameter("userId", (Object)user.getId()).setParameter("realmId", (Object)realm.getId()).executeUpdate();
        this.em.createNamedQuery("deleteFederatedUserConsentRolesByUser").setParameter("userId", (Object)user.getId()).setParameter("realmId", (Object)realm.getId()).executeUpdate();
        this.em.createNamedQuery("deleteFederatedUserConsentsByUser").setParameter("userId", (Object)user.getId()).setParameter("realmId", (Object)realm.getId()).executeUpdate();
        this.em.createNamedQuery("deleteFederatedCredentialAttributeByUser").setParameter("userId", (Object)user.getId()).setParameter("realmId", (Object)realm.getId()).executeUpdate();
        this.em.createNamedQuery("deleteFederatedUserCredentialByUser").setParameter("userId", (Object)user.getId()).setParameter("realmId", (Object)realm.getId()).executeUpdate();
        this.em.createNamedQuery("deleteFederatedUserGroupMembershipsByUser").setParameter("userId", (Object)user.getId()).setParameter("realmId", (Object)realm.getId()).executeUpdate();
        this.em.createNamedQuery("deleteFederatedUserRequiredActionsByUser").setParameter("userId", (Object)user.getId()).setParameter("realmId", (Object)realm.getId()).executeUpdate();
        this.em.createNamedQuery("deleteFederatedUserRoleMappingsByUser").setParameter("userId", (Object)user.getId()).setParameter("realmId", (Object)realm.getId()).executeUpdate();
        this.em.createNamedQuery("deleteFederatedUserByUser").setParameter("userId", (Object)user.getId()).setParameter("realmId", (Object)realm.getId()).executeUpdate();
    }
    
    public void preRemove(final RealmModel realm, final ComponentModel model) {
        if (!model.getProviderType().equals(UserStorageProvider.class.getName())) {
            return;
        }
        this.em.createNamedQuery("deleteBrokerLinkByStorageProvider").setParameter("storageProviderId", (Object)model.getId()).executeUpdate();
        this.em.createNamedQuery("deleteFederatedAttributesByStorageProvider").setParameter("storageProviderId", (Object)model.getId()).executeUpdate();
        this.em.createNamedQuery("deleteFederatedUserConsentProtMappersByStorageProvider").setParameter("storageProviderId", (Object)model.getId()).executeUpdate();
        this.em.createNamedQuery("deleteFederatedUserRoleMappingsByStorageProvider").setParameter("storageProviderId", (Object)model.getId()).executeUpdate();
        this.em.createNamedQuery("deleteFederatedUserConsentsByStorageProvider").setParameter("storageProviderId", (Object)model.getId()).executeUpdate();
        this.em.createNamedQuery("deleteFederatedCredentialAttributeByStorageProvider").setParameter("storageProviderId", (Object)model.getId()).executeUpdate();
        this.em.createNamedQuery("deleteFederatedUserCredentialsByStorageProvider").setParameter("storageProviderId", (Object)model.getId()).executeUpdate();
        this.em.createNamedQuery("deleteFederatedUserGroupMembershipByStorageProvider").setParameter("storageProviderId", (Object)model.getId()).executeUpdate();
        this.em.createNamedQuery("deleteFederatedUserRequiredActionsByStorageProvider").setParameter("storageProviderId", (Object)model.getId()).executeUpdate();
        this.em.createNamedQuery("deleteFederatedUserRoleMappingsByStorageProvider").setParameter("storageProviderId", (Object)model.getId()).executeUpdate();
        this.em.createNamedQuery("deleteFederatedUsersByStorageProvider").setParameter("storageProviderId", (Object)model.getId()).executeUpdate();
    }
}
