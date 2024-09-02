// 
// Decompiled by Procyon v0.5.36
// 

package org.keycloak.broker.oidc;

import org.keycloak.broker.provider.IdentityProvider;
import java.security.PublicKey;
import org.keycloak.jose.jwk.JSONWebKeySet;
import java.security.Key;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.protocol.oidc.utils.JWKSUtils;
import java.io.IOException;
import org.keycloak.util.JsonSerialization;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import java.util.Map;
import java.io.InputStream;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.services.ServicesLogger;
import org.keycloak.broker.provider.AbstractIdentityProviderFactory;

public class OIDCIdentityProviderFactory extends AbstractIdentityProviderFactory<OIDCIdentityProvider>
{
    private static final ServicesLogger logger;
    public static final String PROVIDER_ID = "oidc";
    
    public String getName() {
        return "OpenID Connect v1.0";
    }
    
    public OIDCIdentityProvider create(final IdentityProviderModel model) {
        return new OIDCIdentityProvider(new OIDCIdentityProviderConfig(model));
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
        final OIDCIdentityProviderConfig config = new OIDCIdentityProviderConfig(new IdentityProviderModel());
        config.setIssuer(rep.getIssuer());
        config.setLogoutUrl(rep.getLogoutEndpoint());
        config.setAuthorizationUrl(rep.getAuthorizationEndpoint());
        config.setTokenUrl(rep.getTokenEndpoint());
        config.setUserInfoUrl(rep.getUserinfoEndpoint());
        if (rep.getJwksUri() != null) {
            sendJwksRequest(session, rep, config);
        }
        return (Map<String, String>)config.getConfig();
    }
    
    protected static void sendJwksRequest(final KeycloakSession session, final OIDCConfigurationRepresentation rep, final OIDCIdentityProviderConfig config) {
        try {
            final JSONWebKeySet keySet = JWKSUtils.sendJwksRequest(session, rep.getJwksUri());
            final PublicKey key = JWKSUtils.getKeyForUse(keySet, JWK.Use.SIG);
            if (key == null) {
                OIDCIdentityProviderFactory.logger.supportedJwkNotFound(JWK.Use.SIG.asString());
            }
            else {
                config.setPublicKeySignatureVerifier(KeycloakModelUtils.getPemFromKey((Key)key));
                config.setValidateSignature(true);
            }
        }
        catch (IOException e) {
            throw new RuntimeException("Failed to query JWKSet from: " + rep.getJwksUri(), e);
        }
    }
    
    static {
        logger = ServicesLogger.ROOT_LOGGER;
    }
}
