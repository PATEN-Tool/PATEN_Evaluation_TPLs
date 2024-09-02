// 
// Decompiled by Procyon v0.5.36
// 

package org.keycloak.social.facebook;

import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.broker.social.SocialIdentityProviderFactory;
import org.keycloak.broker.provider.AbstractIdentityProviderFactory;

public class FacebookIdentityProviderFactory extends AbstractIdentityProviderFactory<FacebookIdentityProvider> implements SocialIdentityProviderFactory<FacebookIdentityProvider>
{
    public static final String PROVIDER_ID = "facebook";
    
    public String getName() {
        return "Facebook";
    }
    
    public FacebookIdentityProvider create(final KeycloakSession session, final IdentityProviderModel model) {
        return new FacebookIdentityProvider(session, new OAuth2IdentityProviderConfig(model));
    }
    
    public OAuth2IdentityProviderConfig createConfig() {
        return new OAuth2IdentityProviderConfig();
    }
    
    public String getId() {
        return "facebook";
    }
}
