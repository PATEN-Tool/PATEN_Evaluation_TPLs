// 
// Decompiled by Procyon v0.5.36
// 

package com.google.api.client.auth.oauth2;

import com.google.api.client.util.GenericData;
import com.google.api.client.http.GenericUrl;
import java.util.Collection;
import java.util.Collections;
import com.google.api.client.util.Key;

public class AuthorizationCodeRequestUrl extends AuthorizationRequestUrl
{
    @Key("code_challenge")
    String codeChallenge;
    @Key("code_challenge_method")
    String codeChallengeMethod;
    
    public AuthorizationCodeRequestUrl(final String authorizationServerEncodedUrl, final String clientId) {
        super(authorizationServerEncodedUrl, clientId, Collections.singleton("code"));
    }
    
    public String getCodeChallenge() {
        return this.codeChallenge;
    }
    
    public String getCodeChallengeMethod() {
        return this.codeChallengeMethod;
    }
    
    public void setCodeChallenge(final String codeChallenge) {
        this.codeChallenge = codeChallenge;
    }
    
    public void setCodeChallengeMethod(final String codeChallengeMethod) {
        this.codeChallengeMethod = codeChallengeMethod;
    }
    
    @Override
    public AuthorizationCodeRequestUrl setResponseTypes(final Collection<String> responseTypes) {
        return (AuthorizationCodeRequestUrl)super.setResponseTypes(responseTypes);
    }
    
    @Override
    public AuthorizationCodeRequestUrl setRedirectUri(final String redirectUri) {
        return (AuthorizationCodeRequestUrl)super.setRedirectUri(redirectUri);
    }
    
    @Override
    public AuthorizationCodeRequestUrl setScopes(final Collection<String> scopes) {
        return (AuthorizationCodeRequestUrl)super.setScopes(scopes);
    }
    
    @Override
    public AuthorizationCodeRequestUrl setClientId(final String clientId) {
        return (AuthorizationCodeRequestUrl)super.setClientId(clientId);
    }
    
    @Override
    public AuthorizationCodeRequestUrl setState(final String state) {
        return (AuthorizationCodeRequestUrl)super.setState(state);
    }
    
    @Override
    public AuthorizationCodeRequestUrl set(final String fieldName, final Object value) {
        return (AuthorizationCodeRequestUrl)super.set(fieldName, value);
    }
    
    @Override
    public AuthorizationCodeRequestUrl clone() {
        return (AuthorizationCodeRequestUrl)super.clone();
    }
}
