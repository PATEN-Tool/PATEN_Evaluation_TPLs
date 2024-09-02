// 
// Decompiled by Procyon v0.5.36
// 

package org.keycloak.social.bitbucket;

import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.broker.social.SocialIdentityProviderFactory;
import org.keycloak.broker.provider.AbstractIdentityProviderFactory;

public class BitbucketIdentityProviderFactory extends AbstractIdentityProviderFactory<BitbucketIdentityProvider> implements SocialIdentityProviderFactory<BitbucketIdentityProvider>
{
    public static final String PROVIDER_ID = "bitbucket";
    
    public String getName() {
        return "BitBucket";
    }
    
    public BitbucketIdentityProvider create(final KeycloakSession session, final IdentityProviderModel model) {
        return new BitbucketIdentityProvider(session, new OAuth2IdentityProviderConfig(model));
    }
    
    public OAuth2IdentityProviderConfig createConfig() {
        return new OAuth2IdentityProviderConfig();
    }
    
    public String getId() {
        return "bitbucket";
    }
}
