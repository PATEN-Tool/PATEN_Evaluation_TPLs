// 
// Decompiled by Procyon v0.5.36
// 

package com.google.api.client.auth.oauth2;

import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.Lists;
import com.google.api.client.util.Joiner;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import com.google.api.client.util.Preconditions;
import com.google.api.client.http.GenericUrl;
import java.util.Collection;
import com.google.api.client.util.Clock;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.Beta;
import com.google.api.client.http.HttpExecuteInterceptor;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.http.HttpTransport;

public class AuthorizationCodeFlow
{
    private final Credential.AccessMethod method;
    private final HttpTransport transport;
    private final JsonFactory jsonFactory;
    private final String tokenServerEncodedUrl;
    private final HttpExecuteInterceptor clientAuthentication;
    private final String clientId;
    private final String authorizationServerEncodedUrl;
    @Deprecated
    @Beta
    private final CredentialStore credentialStore;
    @Beta
    private final DataStore<StoredCredential> credentialDataStore;
    private final HttpRequestInitializer requestInitializer;
    private final Clock clock;
    private final Collection<String> scopes;
    private final CredentialCreatedListener credentialCreatedListener;
    private final Collection<CredentialRefreshListener> refreshListeners;
    
    public AuthorizationCodeFlow(final Credential.AccessMethod method, final HttpTransport transport, final JsonFactory jsonFactory, final GenericUrl tokenServerUrl, final HttpExecuteInterceptor clientAuthentication, final String clientId, final String authorizationServerEncodedUrl) {
        this(new Builder(method, transport, jsonFactory, tokenServerUrl, clientAuthentication, clientId, authorizationServerEncodedUrl));
    }
    
    protected AuthorizationCodeFlow(final Builder builder) {
        this.method = (Credential.AccessMethod)Preconditions.checkNotNull((Object)builder.method);
        this.transport = (HttpTransport)Preconditions.checkNotNull((Object)builder.transport);
        this.jsonFactory = (JsonFactory)Preconditions.checkNotNull((Object)builder.jsonFactory);
        this.tokenServerEncodedUrl = ((GenericUrl)Preconditions.checkNotNull((Object)builder.tokenServerUrl)).build();
        this.clientAuthentication = builder.clientAuthentication;
        this.clientId = (String)Preconditions.checkNotNull((Object)builder.clientId);
        this.authorizationServerEncodedUrl = (String)Preconditions.checkNotNull((Object)builder.authorizationServerEncodedUrl);
        this.requestInitializer = builder.requestInitializer;
        this.credentialStore = builder.credentialStore;
        this.credentialDataStore = builder.credentialDataStore;
        this.scopes = Collections.unmodifiableCollection((Collection<? extends String>)builder.scopes);
        this.clock = (Clock)Preconditions.checkNotNull((Object)builder.clock);
        this.credentialCreatedListener = builder.credentialCreatedListener;
        this.refreshListeners = Collections.unmodifiableCollection((Collection<? extends CredentialRefreshListener>)builder.refreshListeners);
    }
    
    public AuthorizationCodeRequestUrl newAuthorizationUrl() {
        return new AuthorizationCodeRequestUrl(this.authorizationServerEncodedUrl, this.clientId).setScopes(this.scopes);
    }
    
    public AuthorizationCodeTokenRequest newTokenRequest(final String authorizationCode) {
        return new AuthorizationCodeTokenRequest(this.transport, this.jsonFactory, new GenericUrl(this.tokenServerEncodedUrl), authorizationCode).setClientAuthentication(this.clientAuthentication).setRequestInitializer(this.requestInitializer).setScopes(this.scopes);
    }
    
    public Credential createAndStoreCredential(final TokenResponse response, final String userId) throws IOException {
        final Credential credential = this.newCredential(userId).setFromTokenResponse(response);
        if (this.credentialStore != null) {
            this.credentialStore.store(userId, credential);
        }
        if (this.credentialDataStore != null) {
            this.credentialDataStore.set(userId, (Serializable)new StoredCredential(credential));
        }
        if (this.credentialCreatedListener != null) {
            this.credentialCreatedListener.onCredentialCreated(credential, response);
        }
        return credential;
    }
    
    public Credential loadCredential(final String userId) throws IOException {
        if (this.credentialDataStore == null && this.credentialStore == null) {
            return null;
        }
        final Credential credential = this.newCredential(userId);
        if (this.credentialDataStore != null) {
            final StoredCredential stored = (StoredCredential)this.credentialDataStore.get(userId);
            if (stored == null) {
                return null;
            }
            credential.setAccessToken(stored.getAccessToken());
            credential.setRefreshToken(stored.getRefreshToken());
            credential.setExpirationTimeMilliseconds(stored.getExpirationTimeMilliseconds());
        }
        else if (!this.credentialStore.load(userId, credential)) {
            return null;
        }
        return credential;
    }
    
    private Credential newCredential(final String userId) {
        final Credential.Builder builder = new Credential.Builder(this.method).setTransport(this.transport).setJsonFactory(this.jsonFactory).setTokenServerEncodedUrl(this.tokenServerEncodedUrl).setClientAuthentication(this.clientAuthentication).setRequestInitializer(this.requestInitializer).setClock(this.clock);
        if (this.credentialDataStore != null) {
            builder.addRefreshListener(new DataStoreCredentialRefreshListener(userId, this.credentialDataStore));
        }
        else if (this.credentialStore != null) {
            builder.addRefreshListener(new CredentialStoreRefreshListener(userId, this.credentialStore));
        }
        builder.getRefreshListeners().addAll(this.refreshListeners);
        return builder.build();
    }
    
    public final Credential.AccessMethod getMethod() {
        return this.method;
    }
    
    public final HttpTransport getTransport() {
        return this.transport;
    }
    
    public final JsonFactory getJsonFactory() {
        return this.jsonFactory;
    }
    
    public final String getTokenServerEncodedUrl() {
        return this.tokenServerEncodedUrl;
    }
    
    public final HttpExecuteInterceptor getClientAuthentication() {
        return this.clientAuthentication;
    }
    
    public final String getClientId() {
        return this.clientId;
    }
    
    public final String getAuthorizationServerEncodedUrl() {
        return this.authorizationServerEncodedUrl;
    }
    
    @Deprecated
    @Beta
    public final CredentialStore getCredentialStore() {
        return this.credentialStore;
    }
    
    @Beta
    public final DataStore<StoredCredential> getCredentialDataStore() {
        return this.credentialDataStore;
    }
    
    public final HttpRequestInitializer getRequestInitializer() {
        return this.requestInitializer;
    }
    
    public final String getScopesAsString() {
        return Joiner.on(' ').join((Iterable)this.scopes);
    }
    
    public final Collection<String> getScopes() {
        return this.scopes;
    }
    
    public final Clock getClock() {
        return this.clock;
    }
    
    public final Collection<CredentialRefreshListener> getRefreshListeners() {
        return this.refreshListeners;
    }
    
    public static class Builder
    {
        Credential.AccessMethod method;
        HttpTransport transport;
        JsonFactory jsonFactory;
        GenericUrl tokenServerUrl;
        HttpExecuteInterceptor clientAuthentication;
        String clientId;
        String authorizationServerEncodedUrl;
        @Deprecated
        @Beta
        CredentialStore credentialStore;
        @Beta
        DataStore<StoredCredential> credentialDataStore;
        HttpRequestInitializer requestInitializer;
        Collection<String> scopes;
        Clock clock;
        CredentialCreatedListener credentialCreatedListener;
        Collection<CredentialRefreshListener> refreshListeners;
        
        public Builder(final Credential.AccessMethod method, final HttpTransport transport, final JsonFactory jsonFactory, final GenericUrl tokenServerUrl, final HttpExecuteInterceptor clientAuthentication, final String clientId, final String authorizationServerEncodedUrl) {
            this.scopes = (Collection<String>)Lists.newArrayList();
            this.clock = Clock.SYSTEM;
            this.refreshListeners = (Collection<CredentialRefreshListener>)Lists.newArrayList();
            this.setMethod(method);
            this.setTransport(transport);
            this.setJsonFactory(jsonFactory);
            this.setTokenServerUrl(tokenServerUrl);
            this.setClientAuthentication(clientAuthentication);
            this.setClientId(clientId);
            this.setAuthorizationServerEncodedUrl(authorizationServerEncodedUrl);
        }
        
        public AuthorizationCodeFlow build() {
            return new AuthorizationCodeFlow(this);
        }
        
        public final Credential.AccessMethod getMethod() {
            return this.method;
        }
        
        public Builder setMethod(final Credential.AccessMethod method) {
            this.method = (Credential.AccessMethod)Preconditions.checkNotNull((Object)method);
            return this;
        }
        
        public final HttpTransport getTransport() {
            return this.transport;
        }
        
        public Builder setTransport(final HttpTransport transport) {
            this.transport = (HttpTransport)Preconditions.checkNotNull((Object)transport);
            return this;
        }
        
        public final JsonFactory getJsonFactory() {
            return this.jsonFactory;
        }
        
        public Builder setJsonFactory(final JsonFactory jsonFactory) {
            this.jsonFactory = (JsonFactory)Preconditions.checkNotNull((Object)jsonFactory);
            return this;
        }
        
        public final GenericUrl getTokenServerUrl() {
            return this.tokenServerUrl;
        }
        
        public Builder setTokenServerUrl(final GenericUrl tokenServerUrl) {
            this.tokenServerUrl = (GenericUrl)Preconditions.checkNotNull((Object)tokenServerUrl);
            return this;
        }
        
        public final HttpExecuteInterceptor getClientAuthentication() {
            return this.clientAuthentication;
        }
        
        public Builder setClientAuthentication(final HttpExecuteInterceptor clientAuthentication) {
            this.clientAuthentication = clientAuthentication;
            return this;
        }
        
        public final String getClientId() {
            return this.clientId;
        }
        
        public Builder setClientId(final String clientId) {
            this.clientId = (String)Preconditions.checkNotNull((Object)clientId);
            return this;
        }
        
        public final String getAuthorizationServerEncodedUrl() {
            return this.authorizationServerEncodedUrl;
        }
        
        public Builder setAuthorizationServerEncodedUrl(final String authorizationServerEncodedUrl) {
            this.authorizationServerEncodedUrl = (String)Preconditions.checkNotNull((Object)authorizationServerEncodedUrl);
            return this;
        }
        
        @Deprecated
        @Beta
        public final CredentialStore getCredentialStore() {
            return this.credentialStore;
        }
        
        @Beta
        public final DataStore<StoredCredential> getCredentialDataStore() {
            return this.credentialDataStore;
        }
        
        public final Clock getClock() {
            return this.clock;
        }
        
        public Builder setClock(final Clock clock) {
            this.clock = (Clock)Preconditions.checkNotNull((Object)clock);
            return this;
        }
        
        @Deprecated
        @Beta
        public Builder setCredentialStore(final CredentialStore credentialStore) {
            Preconditions.checkArgument(this.credentialDataStore == null);
            this.credentialStore = credentialStore;
            return this;
        }
        
        @Beta
        public Builder setDataStoreFactory(final DataStoreFactory dataStoreFactory) throws IOException {
            return this.setCredentialDataStore(StoredCredential.getDefaultDataStore(dataStoreFactory));
        }
        
        @Beta
        public Builder setCredentialDataStore(final DataStore<StoredCredential> credentialDataStore) {
            Preconditions.checkArgument(this.credentialStore == null);
            this.credentialDataStore = credentialDataStore;
            return this;
        }
        
        public final HttpRequestInitializer getRequestInitializer() {
            return this.requestInitializer;
        }
        
        public Builder setRequestInitializer(final HttpRequestInitializer requestInitializer) {
            this.requestInitializer = requestInitializer;
            return this;
        }
        
        public Builder setScopes(final Collection<String> scopes) {
            this.scopes = (Collection<String>)Preconditions.checkNotNull((Object)scopes);
            return this;
        }
        
        public final Collection<String> getScopes() {
            return this.scopes;
        }
        
        public Builder setCredentialCreatedListener(final CredentialCreatedListener credentialCreatedListener) {
            this.credentialCreatedListener = credentialCreatedListener;
            return this;
        }
        
        public Builder addRefreshListener(final CredentialRefreshListener refreshListener) {
            this.refreshListeners.add((CredentialRefreshListener)Preconditions.checkNotNull((Object)refreshListener));
            return this;
        }
        
        public final Collection<CredentialRefreshListener> getRefreshListeners() {
            return this.refreshListeners;
        }
        
        public Builder setRefreshListeners(final Collection<CredentialRefreshListener> refreshListeners) {
            this.refreshListeners = (Collection<CredentialRefreshListener>)Preconditions.checkNotNull((Object)refreshListeners);
            return this;
        }
        
        public final CredentialCreatedListener getCredentialCreatedListener() {
            return this.credentialCreatedListener;
        }
    }
    
    public interface CredentialCreatedListener
    {
        void onCredentialCreated(final Credential p0, final TokenResponse p1) throws IOException;
    }
}
