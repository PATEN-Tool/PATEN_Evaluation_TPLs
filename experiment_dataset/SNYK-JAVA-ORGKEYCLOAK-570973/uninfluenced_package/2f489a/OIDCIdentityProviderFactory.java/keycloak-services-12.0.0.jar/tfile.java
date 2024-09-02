// 
// Decompiled by Procyon v0.5.36
// 

package org.keycloak.broker.oidc;

import org.keycloak.broker.provider.IdentityProvider;
import java.io.IOException;
import org.keycloak.util.JsonSerialization;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import java.util.Map;
import java.io.InputStream;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.broker.provider.AbstractIdentityProviderFactory;

public class OIDCIdentityProviderFactory extends AbstractIdentityProviderFactory<OIDCIdentityProvider>
{
    public static final String PROVIDER_ID = "oidc";
    
    public String getName() {
        return "OpenID Connect v1.0";
    }
    
    public OIDCIdentityProvider create(final KeycloakSession session, final IdentityProviderModel model) {
        return new OIDCIdentityProvider(session, new OIDCIdentityProviderConfig(model));
    }
    
    public OIDCIdentityProviderConfig createConfig() {
        return new OIDCIdentityProviderConfig();
    }
    
    public String getId() {
        return "oidc";
    }
    
    public Map<String, String> parseConfig(final KeycloakSession session, final InputStream inputStream) {
        return parseOIDCConfig(session, inputStream);
    }
    
    protected static Map<String, String> parseOIDCConfig(final KeycloakSession session, final InputStream inputStream) {
        OIDCConfigurationRepresentation rep;
        try {
            rep = (OIDCConfigurationRepresentation)JsonSerialization.readValue(inputStream, (Class)OIDCConfigurationRepresentation.class);
        }
        catch (IOException e) {
            throw new RuntimeException("failed to load openid connect metadata", e);
        }
        final OIDCIdentityProviderConfig config = new OIDCIdentityProviderConfig();
        config.setIssuer(rep.getIssuer());
        config.setLogoutUrl(rep.getLogoutEndpoint());
        config.setAuthorizationUrl(rep.getAuthorizationEndpoint());
        config.setTokenUrl(rep.getTokenEndpoint());
        config.setUserInfoUrl(rep.getUserinfoEndpoint());
        if (rep.getJwksUri() != null) {
            config.setValidateSignature(true);
            config.setUseJwksUrl(true);
            config.setJwksUrl(rep.getJwksUri());
        }
        return (Map<String, String>)config.getConfig();
    }
}
