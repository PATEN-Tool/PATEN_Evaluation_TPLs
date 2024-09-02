// 
// Decompiled by Procyon v0.5.36
// 

package org.keycloak.partialimport;

import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import java.util.List;
import org.keycloak.representations.idm.PartialImportRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;

public class IdentityProvidersPartialImport extends AbstractPartialImport<IdentityProviderRepresentation>
{
    @Override
    public List<IdentityProviderRepresentation> getRepList(final PartialImportRepresentation partialImportRep) {
        return (List<IdentityProviderRepresentation>)partialImportRep.getIdentityProviders();
    }
    
    @Override
    public String getName(final IdentityProviderRepresentation idpRep) {
        return idpRep.getAlias();
    }
    
    @Override
    public String getModelId(final RealmModel realm, final KeycloakSession session, final IdentityProviderRepresentation idpRep) {
        return realm.getIdentityProviderByAlias(this.getName(idpRep)).getInternalId();
    }
    
    @Override
    public boolean exists(final RealmModel realm, final KeycloakSession session, final IdentityProviderRepresentation idpRep) {
        return realm.getIdentityProviderByAlias(this.getName(idpRep)) != null;
    }
    
    @Override
    public String existsMessage(final RealmModel realm, final IdentityProviderRepresentation idpRep) {
        return "Identity Provider '" + this.getName(idpRep) + "' already exists.";
    }
    
    @Override
    public ResourceType getResourceType() {
        return ResourceType.IDP;
    }
    
    @Override
    public void remove(final RealmModel realm, final KeycloakSession session, final IdentityProviderRepresentation idpRep) {
        realm.removeIdentityProviderByAlias(this.getName(idpRep));
    }
    
    @Override
    public void create(final RealmModel realm, final KeycloakSession session, final IdentityProviderRepresentation idpRep) {
        idpRep.setInternalId(KeycloakModelUtils.generateId());
        final IdentityProviderModel identityProvider = RepresentationToModel.toModel(realm, idpRep);
        realm.addIdentityProvider(identityProvider);
    }
}
