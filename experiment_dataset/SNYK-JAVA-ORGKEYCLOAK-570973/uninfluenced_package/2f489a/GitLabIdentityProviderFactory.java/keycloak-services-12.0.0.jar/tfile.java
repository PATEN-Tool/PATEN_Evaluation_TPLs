// 
// Decompiled by Procyon v0.5.36
// 

package org.keycloak.social.gitlab;

import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.broker.social.SocialIdentityProviderFactory;
import org.keycloak.broker.provider.AbstractIdentityProviderFactory;

public class GitLabIdentityProviderFactory extends AbstractIdentityProviderFactory<GitLabIdentityProvider> implements SocialIdentityProviderFactory<GitLabIdentityProvider>
{
    public static final String PROVIDER_ID = "gitlab";
    
    public String getName() {
        return "GitLab";
    }
    
    public GitLabIdentityProvider create(final KeycloakSession session, final IdentityProviderModel model) {
        return new GitLabIdentityProvider(session, new OIDCIdentityProviderConfig(model));
    }
    
    public OIDCIdentityProviderConfig createConfig() {
        return new OIDCIdentityProviderConfig();
    }
    
    public String getId() {
        return "gitlab";
    }
}
