// 
// Decompiled by Procyon v0.5.36
// 

package org.keycloak.broker.oidc;

import org.keycloak.broker.provider.IdentityProvider;
import java.util.Map;
import java.io.InputStream;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.broker.provider.AbstractIdentityProviderFactory;

public class KeycloakOIDCIdentityProviderFactory extends AbstractIdentityProviderFactory<KeycloakOIDCIdentityProvider>
{
    public static final String PROVIDER_ID = "keycloak-oidc";
    
    public String getName() {
        return "Keycloak OpenID Connect";
    }
    
    public KeycloakOIDCIdentityProvider create(final KeycloakSession session, final IdentityProviderModel model) {
        return new KeycloakOIDCIdentityProvider(session, new OIDCIdentityProviderConfig(model));
    }
    
    public String getId() {
        return "keycloak-oidc";
    }
    
    public Map<String, String> parseConfig(final KeycloakSession session, final InputStream inputStream) {
        return OIDCIdentityProviderFactory.parseOIDCConfig(session, inputStream);
    }
    
    public OIDCIdentityProviderConfig createConfig() {
        return new OIDCIdentityProviderConfig();
    }
}
