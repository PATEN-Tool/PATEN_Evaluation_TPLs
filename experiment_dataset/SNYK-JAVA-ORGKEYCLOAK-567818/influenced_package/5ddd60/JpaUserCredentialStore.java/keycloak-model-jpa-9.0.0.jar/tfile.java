// 
// Decompiled by Procyon v0.5.36
// 

package org.keycloak.models.jpa;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Iterator;
import javax.persistence.LockModeType;
import org.keycloak.models.utils.KeycloakModelUtils;
import javax.persistence.TypedQuery;
import org.keycloak.models.jpa.entities.UserEntity;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.function.Function;
import java.util.List;
import org.keycloak.common.util.Base64;
import org.keycloak.models.jpa.entities.CredentialEntity;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.RealmModel;
import javax.persistence.EntityManager;
import org.keycloak.models.KeycloakSession;
import org.jboss.logging.Logger;
import org.keycloak.credential.UserCredentialStore;

public class JpaUserCredentialStore implements UserCredentialStore
{
    public static final int PRIORITY_DIFFERENCE = 10;
    protected static final Logger logger;
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
        entity.setCreatedDate(cred.getCreatedDate());
        entity.setUserLabel(cred.getUserLabel());
        entity.setType(cred.getType());
        entity.setSecretData(cred.getSecretData());
        entity.setCredentialData(cred.getCredentialData());
    }
    
    public CredentialModel createCredential(final RealmModel realm, final UserModel user, final CredentialModel cred) {
        final CredentialEntity entity = this.createCredentialEntity(realm, user, cred);
        return this.toModel(entity);
    }
    
    public boolean removeStoredCredential(final RealmModel realm, final UserModel user, final String id) {
        final CredentialEntity entity = this.removeCredentialEntity(realm, user, id);
        return entity != null;
    }
    
    public CredentialModel getStoredCredentialById(final RealmModel realm, final UserModel user, final String id) {
        final CredentialEntity entity = (CredentialEntity)this.em.find((Class)CredentialEntity.class, (Object)id);
        if (entity == null) {
            return null;
        }
        final CredentialModel model = this.toModel(entity);
        return model;
    }
    
    CredentialModel toModel(final CredentialEntity entity) {
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
    
    public List<CredentialModel> getStoredCredentials(final RealmModel realm, final UserModel user) {
        final List<CredentialEntity> results = this.getStoredCredentialEntities(realm, user);
        return results.stream().map((Function<? super Object, ?>)this::toModel).collect((Collector<? super Object, ?, List<CredentialModel>>)Collectors.toList());
    }
    
    private List<CredentialEntity> getStoredCredentialEntities(final RealmModel realm, final UserModel user) {
        final UserEntity userEntity = (UserEntity)this.em.getReference((Class)UserEntity.class, (Object)user.getId());
        final TypedQuery<CredentialEntity> query = (TypedQuery<CredentialEntity>)this.em.createNamedQuery("credentialByUser", (Class)CredentialEntity.class).setParameter("user", (Object)userEntity);
        return (List<CredentialEntity>)query.getResultList();
    }
    
    public List<CredentialModel> getStoredCredentialsByType(final RealmModel realm, final UserModel user, final String type) {
        return this.getStoredCredentials(realm, user).stream().filter(credential -> type.equals(credential.getType())).collect((Collector<? super Object, ?, List<CredentialModel>>)Collectors.toList());
    }
    
    public CredentialModel getStoredCredentialByNameAndType(final RealmModel realm, final UserModel user, final String name, final String type) {
        final List<CredentialModel> results = this.getStoredCredentials(realm, user).stream().filter(credential -> type.equals(credential.getType()) && name.equals(credential.getUserLabel())).collect((Collector<? super Object, ?, List<CredentialModel>>)Collectors.toList());
        if (results.isEmpty()) {
            return null;
        }
        return results.get(0);
    }
    
    public void close() {
    }
    
    CredentialEntity createCredentialEntity(final RealmModel realm, final UserModel user, final CredentialModel cred) {
        final CredentialEntity entity = new CredentialEntity();
        final String id = (cred.getId() == null) ? KeycloakModelUtils.generateId() : cred.getId();
        entity.setId(id);
        entity.setCreatedDate(cred.getCreatedDate());
        entity.setUserLabel(cred.getUserLabel());
        entity.setType(cred.getType());
        entity.setSecretData(cred.getSecretData());
        entity.setCredentialData(cred.getCredentialData());
        final UserEntity userRef = (UserEntity)this.em.getReference((Class)UserEntity.class, (Object)user.getId());
        entity.setUser(userRef);
        final List<CredentialEntity> credentials = this.getStoredCredentialEntities(realm, user);
        final int priority = credentials.isEmpty() ? 10 : (credentials.get(credentials.size() - 1).getPriority() + 10);
        entity.setPriority(priority);
        this.em.persist((Object)entity);
        return entity;
    }
    
    CredentialEntity removeCredentialEntity(final RealmModel realm, final UserModel user, final String id) {
        final CredentialEntity entity = (CredentialEntity)this.em.find((Class)CredentialEntity.class, (Object)id, LockModeType.PESSIMISTIC_WRITE);
        if (entity == null) {
            return null;
        }
        final int currentPriority = entity.getPriority();
        final List<CredentialEntity> credentials = this.getStoredCredentialEntities(realm, user);
        for (final CredentialEntity cred : credentials) {
            if (cred.getPriority() > currentPriority) {
                cred.setPriority(cred.getPriority() - 10);
            }
        }
        this.em.remove((Object)entity);
        this.em.flush();
        return entity;
    }
    
    public boolean moveCredentialTo(final RealmModel realm, final UserModel user, final String id, final String newPreviousCredentialId) {
        final List<CredentialEntity> sortedCreds = this.getStoredCredentialEntities(realm, user);
        final List<CredentialEntity> newList = new ArrayList<CredentialEntity>();
        newList.addAll(sortedCreds);
        int ourCredentialIndex = -1;
        int newPreviousCredentialIndex = -1;
        CredentialEntity ourCredential = null;
        int i = 0;
        for (final CredentialEntity credential : newList) {
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
            JpaUserCredentialStore.logger.warnf("Not found credential with id [%s] of user [%s]", (Object)id, (Object)user.getUsername());
            return false;
        }
        if (newPreviousCredentialId != null && newPreviousCredentialIndex == -1) {
            JpaUserCredentialStore.logger.warnf("Can't move up credential with id [%s] of user [%s]", (Object)id, (Object)user.getUsername());
            return false;
        }
        final int toMoveIndex = (newPreviousCredentialId == null) ? 0 : (newPreviousCredentialIndex + 1);
        newList.add(toMoveIndex, ourCredential);
        final int indexToRemove = (toMoveIndex < ourCredentialIndex) ? (ourCredentialIndex + 1) : ourCredentialIndex;
        newList.remove(indexToRemove);
        int expectedPriority = 0;
        for (final CredentialEntity credential2 : newList) {
            expectedPriority += 10;
            if (credential2.getPriority() != expectedPriority) {
                credential2.setPriority(expectedPriority);
                JpaUserCredentialStore.logger.tracef("Priority of credential [%s] of user [%s] changed to [%d]", (Object)credential2.getId(), (Object)user.getUsername(), (Object)expectedPriority);
            }
        }
        return true;
    }
    
    static {
        logger = Logger.getLogger((Class)JpaUserCredentialStore.class);
    }
}
