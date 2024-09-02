// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.security.oauth2.client;

import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.util.StringUtils;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;

public class DefaultOAuth2RequestAuthenticator implements OAuth2RequestAuthenticator
{
    @Override
    public void authenticate(final OAuth2ProtectedResourceDetails resource, final OAuth2ClientContext clientContext, final ClientHttpRequest request) {
        final OAuth2AccessToken accessToken = clientContext.getAccessToken();
        String tokenType = accessToken.getTokenType();
        if (!StringUtils.hasText(tokenType)) {
            tokenType = "Bearer";
        }
        request.getHeaders().set("Authorization", String.format("%s %s", tokenType, accessToken.getValue()));
    }
}
