// 
// Decompiled by Procyon v0.5.36
// 

package org.keycloak.social.github;

import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.broker.social.SocialIdentityProviderFactory;
import org.keycloak.broker.provider.AbstractIdentityProviderFactory;

public class GitHubIdentityProviderFactory extends AbstractIdentityProviderFactory<GitHubIdentityProvider> implements SocialIdentityProviderFactory<GitHubIdentityProvider>
{
    public static final String PROVIDER_ID = "github";
    
    public String getName() {
        return "GitHub";
    }
    
    public GitHubIdentityProvider create(final KeycloakSession session, final IdentityProviderModel model) {
        return new GitHubIdentityProvider(session, new OAuth2IdentityProviderConfig(model));
    }
    
    public OAuth2IdentityProviderConfig createConfig() {
        return new OAuth2IdentityProviderConfig();
    }
    
    public String getId() {
        return "github";
    }
}
