// 
// Decompiled by Procyon v0.5.36
// 

package org.infinispan.rest.configuration;

import org.infinispan.commons.configuration.Self;
import java.util.List;
import org.infinispan.commons.configuration.attributes.AttributeDefinition;
import org.infinispan.server.core.configuration.ProtocolServerConfigurationChildBuilder;
import org.infinispan.server.core.configuration.ProtocolServerConfigurationBuilder;
import org.infinispan.rest.authentication.Authenticator;
import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.infinispan.commons.configuration.Builder;
import org.infinispan.server.core.configuration.AbstractProtocolServerConfigurationChildBuilder;

public class AuthenticationConfigurationBuilder extends AbstractProtocolServerConfigurationChildBuilder<RestServerConfiguration, AuthenticationConfigurationBuilder> implements Builder<AuthenticationConfiguration>
{
    private final AttributeSet attributes;
    private Authenticator authenticator;
    private boolean enabled;
    
    AuthenticationConfigurationBuilder(final ProtocolServerConfigurationBuilder builder) {
        super((ProtocolServerConfigurationChildBuilder)builder);
        this.attributes = AuthenticationConfiguration.attributeDefinitionSet();
    }
    
    public AuthenticationConfigurationBuilder enable() {
        return this.enabled(true);
    }
    
    public AuthenticationConfigurationBuilder disable() {
        return this.enabled(false);
    }
    
    public AuthenticationConfigurationBuilder enabled(final boolean enabled) {
        this.enabled = enabled;
        return this;
    }
    
    public AuthenticationConfigurationBuilder securityRealm(final String realm) {
        this.attributes.attribute((AttributeDefinition)AuthenticationConfiguration.SECURITY_REALM).set((Object)realm);
        return this;
    }
    
    public String securityRealm() {
        return (String)this.attributes.attribute((AttributeDefinition)AuthenticationConfiguration.SECURITY_REALM).get();
    }
    
    public boolean hasSecurityRealm() {
        return !this.attributes.attribute((AttributeDefinition)AuthenticationConfiguration.SECURITY_REALM).isNull();
    }
    
    public AuthenticationConfigurationBuilder authenticator(final Authenticator authenticator) {
        this.authenticator = authenticator;
        return this.enable();
    }
    
    public AuthenticationConfigurationBuilder addMechanisms(final String... mechanisms) {
        final List<String> mechs = (List<String>)this.attributes.attribute((AttributeDefinition)AuthenticationConfiguration.MECHANISMS).get();
        for (int i = 0; i < mechanisms.length; ++i) {
            mechs.add(mechanisms[i]);
        }
        this.attributes.attribute((AttributeDefinition)AuthenticationConfiguration.MECHANISMS).set((Object)mechs);
        return this.enable();
    }
    
    public boolean hasMechanisms() {
        return !((List)this.attributes.attribute((AttributeDefinition)AuthenticationConfiguration.MECHANISMS).get()).isEmpty();
    }
    
    public List<String> mechanisms() {
        return (List<String>)this.attributes.attribute((AttributeDefinition)AuthenticationConfiguration.MECHANISMS).get();
    }
    
    public AuthenticationConfigurationBuilder metricsAuth(final boolean metricsAuth) {
        this.attributes.attribute((AttributeDefinition)AuthenticationConfiguration.METRICS_AUTH).set((Object)metricsAuth);
        return this;
    }
    
    public void validate() {
        if (this.enabled && this.authenticator == null) {
            throw RestServerConfigurationBuilder.logger.authenticationWithoutAuthenticator();
        }
    }
    
    public AuthenticationConfiguration create() {
        return new AuthenticationConfiguration(this.attributes.protect(), this.authenticator, this.enabled);
    }
    
    public Builder<?> read(final AuthenticationConfiguration template) {
        this.attributes.read(template.attributes());
        return (Builder<?>)this;
    }
    
    public AuthenticationConfigurationBuilder self() {
        return this;
    }
}
