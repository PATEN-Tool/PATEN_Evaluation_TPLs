// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.security.oauth2.client;

import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.util.StringUtils;
import org.springframework.security.oauth2.client.http.AccessTokenRequiredException;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;

@Deprecated
public class DefaultOAuth2RequestAuthenticator implements OAuth2RequestAuthenticator
{
    @Override
    public void authenticate(final OAuth2ProtectedResourceDetails resource, final OAuth2ClientContext clientContext, final ClientHttpRequest request) {
        final OAuth2AccessToken accessToken = clientContext.getAccessToken();
        if (accessToken == null) {
            throw new AccessTokenRequiredException(resource);
        }
        String tokenType = accessToken.getTokenType();
        if (!StringUtils.hasText(tokenType)) {
            tokenType = "Bearer";
        }
        else if (tokenType.equalsIgnoreCase("Bearer")) {
            tokenType = "Bearer";
        }
        request.getHeaders().set("Authorization", String.format("%s %s", tokenType, accessToken.getValue()));
    }
}
