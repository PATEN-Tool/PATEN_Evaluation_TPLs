// 
// Decompiled by Procyon v0.5.36
// 

package org.keycloak.models.jpa;

import javax.persistence.TypedQuery;
import java.util.LinkedList;
import org.keycloak.models.jpa.entities.UserEntity;
import java.util.List;
import java.util.Iterator;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.jpa.entities.CredentialAttributeEntity;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.jpa.entities.CredentialEntity;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.RealmModel;
import javax.persistence.EntityManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.credential.UserCredentialStore;

public class JpaUserCredentialStore implements UserCredentialStore
{
    private final KeycloakSession session;
    protected final EntityManager em;
    
    public JpaUserCredentialStore(final KeycloakSession session, final EntityManager em) {
        this.session = session;
        this.em = em;
    }
    
    public void updateCredential(final RealmModel realm, final UserModel user, final CredentialModel cred) {
        final CredentialEntity entity = (CredentialEntity)this.em.find((Class)CredentialEntity.class, (Object)cred.getId());
        if (entity == null) {
            return;
        }
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
        final MultivaluedHashMap<String, String> attrs = (MultivaluedHashMap<String, String>)cred.getConfig();
        MultivaluedHashMap<String, String> config = (MultivaluedHashMap<String, String>)cred.getConfig();
        if (config == null) {
            config = (MultivaluedHashMap<String, String>)new MultivaluedHashMap();
        }
        final Iterator<CredentialAttributeEntity> it = entity.getCredentialAttributes().iterator();
        while (it.hasNext()) {
            final CredentialAttributeEntity attr = it.next();
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
                    final CredentialAttributeEntity attr2 = new CredentialAttributeEntity();
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
    
    public CredentialModel createCredential(final RealmModel realm, final UserModel user, final CredentialModel cred) {
        final CredentialEntity entity = new CredentialEntity();
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
        final UserEntity userRef = (UserEntity)this.em.getReference((Class)UserEntity.class, (Object)user.getId());
        entity.setUser(userRef);
        this.em.persist((Object)entity);
        final MultivaluedHashMap<String, String> config = (MultivaluedHashMap<String, String>)cred.getConfig();
        if (config != null && !config.isEmpty()) {
            for (final String key : config.keySet()) {
                final List<String> values = (List<String>)config.getList((Object)key);
                for (final String val : values) {
                    final CredentialAttributeEntity attr = new CredentialAttributeEntity();
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
    
    public boolean removeStoredCredential(final RealmModel realm, final UserModel user, final String id) {
        final CredentialEntity entity = (CredentialEntity)this.em.find((Class)CredentialEntity.class, (Object)id);
        if (entity == null) {
            return false;
        }
        this.em.remove((Object)entity);
        return true;
    }
    
    public CredentialModel getStoredCredentialById(final RealmModel realm, final UserModel user, final String id) {
        final CredentialEntity entity = (CredentialEntity)this.em.find((Class)CredentialEntity.class, (Object)id);
        if (entity == null) {
            return null;
        }
        final CredentialModel model = this.toModel(entity);
        return model;
    }
    
    protected CredentialModel toModel(final CredentialEntity entity) {
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
        final MultivaluedHashMap<String, String> config = (MultivaluedHashMap<String, String>)new MultivaluedHashMap();
        model.setConfig((MultivaluedHashMap)config);
        for (final CredentialAttributeEntity attr : entity.getCredentialAttributes()) {
            config.add((Object)attr.getName(), (Object)attr.getValue());
        }
        return model;
    }
    
    public List<CredentialModel> getStoredCredentials(final RealmModel realm, final UserModel user) {
        final UserEntity userEntity = (UserEntity)this.em.getReference((Class)UserEntity.class, (Object)user.getId());
        final TypedQuery<CredentialEntity> query = (TypedQuery<CredentialEntity>)this.em.createNamedQuery("credentialByUser", (Class)CredentialEntity.class).setParameter("user", (Object)userEntity);
        final List<CredentialEntity> results = (List<CredentialEntity>)query.getResultList();
        final List<CredentialModel> rtn = new LinkedList<CredentialModel>();
        for (final CredentialEntity entity : results) {
            rtn.add(this.toModel(entity));
        }
        return rtn;
    }
    
    public List<CredentialModel> getStoredCredentialsByType(final RealmModel realm, final UserModel user, final String type) {
        final UserEntity userEntity = (UserEntity)this.em.getReference((Class)UserEntity.class, (Object)user.getId());
        final TypedQuery<CredentialEntity> query = (TypedQuery<CredentialEntity>)this.em.createNamedQuery("credentialByUserAndType", (Class)CredentialEntity.class).setParameter("type", (Object)type).setParameter("user", (Object)userEntity);
        final List<CredentialEntity> results = (List<CredentialEntity>)query.getResultList();
        final List<CredentialModel> rtn = new LinkedList<CredentialModel>();
        for (final CredentialEntity entity : results) {
            rtn.add(this.toModel(entity));
        }
        return rtn;
    }
    
    public CredentialModel getStoredCredentialByNameAndType(final RealmModel realm, final UserModel user, final String name, final String type) {
        final UserEntity userEntity = (UserEntity)this.em.getReference((Class)UserEntity.class, (Object)user.getId());
        final TypedQuery<CredentialEntity> query = (TypedQuery<CredentialEntity>)this.em.createNamedQuery("credentialByNameAndType", (Class)CredentialEntity.class).setParameter("type", (Object)type).setParameter("device", (Object)name).setParameter("user", (Object)userEntity);
        final List<CredentialEntity> results = (List<CredentialEntity>)query.getResultList();
        if (results.isEmpty()) {
            return null;
        }
        return this.toModel(results.get(0));
    }
    
    public void close() {
    }
}
