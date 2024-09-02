// 
// Decompiled by Procyon v0.5.36
// 

package org.infinispan.server.configuration.endpoint;

import org.infinispan.server.hotrod.configuration.AuthenticationConfigurationBuilder;
import java.util.Collection;
import org.infinispan.server.configuration.security.KerberosSecurityFactoryConfiguration;
import org.infinispan.server.security.ServerSecurityRealm;
import java.util.Iterator;
import org.infinispan.rest.configuration.RestServerConfigurationBuilder;
import org.infinispan.server.hotrod.configuration.HotRodServerConfigurationBuilder;
import org.infinispan.server.core.configuration.ProtocolServerConfiguration;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.Map;
import org.infinispan.server.Server;
import org.infinispan.commons.configuration.attributes.AttributeDefinition;
import java.util.ArrayList;
import org.infinispan.server.core.configuration.ProtocolServerConfigurationBuilder;
import java.util.List;
import org.infinispan.server.configuration.ServerConfigurationBuilder;
import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.infinispan.commons.configuration.Builder;

public class EndpointConfigurationBuilder implements Builder<EndpointConfiguration>
{
    private final AttributeSet attributes;
    private final ServerConfigurationBuilder serverConfigurationBuilder;
    private final List<ProtocolServerConfigurationBuilder<?, ?>> connectorBuilders;
    private final SinglePortServerConfigurationBuilder singlePortBuilder;
    private boolean implicitConnectorSecurity;
    
    public EndpointConfigurationBuilder(final ServerConfigurationBuilder serverConfigurationBuilder, final String socketBindingName) {
        this.connectorBuilders = new ArrayList<ProtocolServerConfigurationBuilder<?, ?>>(2);
        this.singlePortBuilder = new SinglePortServerConfigurationBuilder();
        this.serverConfigurationBuilder = serverConfigurationBuilder;
        this.attributes = EndpointConfiguration.attributeDefinitionSet();
        this.attributes.attribute((AttributeDefinition)EndpointConfiguration.SOCKET_BINDING).set((Object)socketBindingName);
        serverConfigurationBuilder.applySocketBinding(socketBindingName, this.singlePortBuilder, this.singlePortBuilder);
    }
    
    public EndpointConfigurationBuilder securityRealm(final String name) {
        this.attributes.attribute((AttributeDefinition)EndpointConfiguration.SECURITY_REALM).set((Object)name);
        this.singlePortBuilder.securityRealm(this.serverConfigurationBuilder.getSecurityRealm(name));
        return this;
    }
    
    public EndpointConfigurationBuilder implicitConnectorSecurity(final boolean implicitConnectorSecurity) {
        this.implicitConnectorSecurity = implicitConnectorSecurity;
        return this;
    }
    
    public EndpointConfigurationBuilder admin(final boolean admin) {
        this.attributes.attribute((AttributeDefinition)EndpointConfiguration.ADMIN).set((Object)admin);
        return this;
    }
    
    public boolean admin() {
        return (boolean)this.attributes.attribute((AttributeDefinition)EndpointConfiguration.ADMIN).get();
    }
    
    public EndpointConfigurationBuilder metricsAuth(final boolean auth) {
        this.attributes.attribute((AttributeDefinition)EndpointConfiguration.METRICS_AUTH).set((Object)auth);
        return this;
    }
    
    public boolean metricsAuth() {
        return (boolean)this.attributes.attribute((AttributeDefinition)EndpointConfiguration.METRICS_AUTH).get();
    }
    
    public List<ProtocolServerConfigurationBuilder<?, ?>> connectors() {
        return this.connectorBuilders;
    }
    
    public SinglePortServerConfigurationBuilder singlePort() {
        return this.singlePortBuilder;
    }
    
    public <T extends ProtocolServerConfigurationBuilder<?, ?>> T addConnector(final Class<T> klass) {
        try {
            final T builder = klass.getConstructor((Class<?>[])new Class[0]).newInstance(new Object[0]);
            this.connectorBuilders.add(builder);
            this.singlePortBuilder.applyConfigurationToProtocol(builder);
            return builder;
        }
        catch (Exception e) {
            throw Server.log.cannotInstantiateProtocolServerConfigurationBuilder(klass, e);
        }
    }
    
    public void validate() {
        final Map<String, List<ProtocolServerConfigurationBuilder<?, ?>>> buildersPerClass = this.connectorBuilders.stream().collect((Collector<? super Object, ?, Map<String, List<ProtocolServerConfigurationBuilder<?, ?>>>>)Collectors.groupingBy(s -> s.getClass().getSimpleName() + "/" + s.host() + ":" + s.port()));
        final String names;
        buildersPerClass.values().stream().filter(c -> c.size() > 1).findFirst().ifPresent(c -> {
            names = c.stream().map((Function<? super Object, ?>)ProtocolServerConfigurationBuilder::name).collect((Collector<? super Object, ?, String>)Collectors.joining(","));
            throw Server.log.multipleEndpointsSameTypeFound(names);
        });
    }
    
    public EndpointConfiguration create() {
        final boolean implicitSecurity = this.implicitConnectorSecurity && this.singlePortBuilder.securityRealm() != null;
        final List<ProtocolServerConfiguration> connectors = new ArrayList<ProtocolServerConfiguration>(this.connectorBuilders.size());
        for (final ProtocolServerConfigurationBuilder<?, ?> builder : this.connectorBuilders) {
            if (implicitSecurity) {
                if (builder instanceof HotRodServerConfigurationBuilder) {
                    this.enableImplicitAuthentication(this.singlePortBuilder.securityRealm(), (HotRodServerConfigurationBuilder)builder);
                }
                else if (builder instanceof RestServerConfigurationBuilder) {
                    this.enableImplicitAuthentication(this.singlePortBuilder.securityRealm(), (RestServerConfigurationBuilder)builder);
                }
            }
            connectors.add((ProtocolServerConfiguration)builder.create());
        }
        return new EndpointConfiguration(this.attributes.protect(), connectors, this.singlePortBuilder.create());
    }
    
    public EndpointConfigurationBuilder read(final EndpointConfiguration template) {
        this.attributes.read(template.attributes());
        return this;
    }
    
    private void enableImplicitAuthentication(ServerSecurityRealm securityRealm, final HotRodServerConfigurationBuilder builder) {
        final AuthenticationConfigurationBuilder authentication = builder.authentication();
        if (!authentication.hasSecurityRealm()) {
            authentication.securityRealm(securityRealm.getName());
            Server.log.debugf("Using endpoint realm \"%s\" for Hot Rod", (Object)securityRealm.getName());
        }
        else {
            securityRealm = this.serverConfigurationBuilder.getSecurityRealm(authentication.securityRealm());
        }
        if (!authentication.hasMechanisms()) {
            String serverPrincipal = null;
            for (final KerberosSecurityFactoryConfiguration identity : securityRealm.getServerIdentities().kerberosConfigurations()) {
                if (identity.getPrincipal().startsWith("hotrod/")) {
                    authentication.enable().addMechanisms(new String[] { "GS2-KRB5", "GSSAPI" });
                    serverPrincipal = identity.getPrincipal();
                    break;
                }
                Server.log.debugf("Enabled Kerberos mechanisms for Hot Rod using principal '%s'", (Object)identity.getPrincipal());
            }
            if (securityRealm.hasFeature(ServerSecurityRealm.Feature.TOKEN)) {
                authentication.enable().addMechanisms(new String[] { "OAUTHBEARER" });
                Server.log.debug((Object)"Enabled OAUTHBEARER mechanism for Hot Rod");
            }
            if (securityRealm.hasFeature(ServerSecurityRealm.Feature.TRUST)) {
                authentication.enable().addMechanisms(new String[] { "EXTERNAL" });
                Server.log.debug((Object)"Enabled EXTERNAL mechanism for Hot Rod");
            }
            if (securityRealm.hasFeature(ServerSecurityRealm.Feature.PASSWORD)) {
                authentication.enable().addMechanisms(new String[] { "SCRAM-SHA-512", "SCRAM-SHA-384", "SCRAM-SHA-256", "SCRAM-SHA-1", "DIGEST-SHA-512", "DIGEST-SHA-384", "DIGEST-SHA-256", "DIGEST-SHA", "CRAM-MD5", "DIGEST-MD5" });
                Server.log.debug((Object)"Enabled SCRAM, DIGEST and CRAM mechanisms for Hot Rod");
                if (this.singlePortBuilder.ssl().isEnabled()) {
                    authentication.enable().addMechanisms(new String[] { "PLAIN" });
                    Server.log.debug((Object)"Enabled PLAIN mechanism for Hot Rod");
                }
            }
            authentication.serverAuthenticationProvider(securityRealm.getSASLAuthenticationProvider(serverPrincipal, authentication.sasl().mechanisms()));
        }
    }
    
    private void enableImplicitAuthentication(ServerSecurityRealm securityRealm, final RestServerConfigurationBuilder builder) {
        final org.infinispan.rest.configuration.AuthenticationConfigurationBuilder authentication = builder.authentication();
        if (!authentication.hasSecurityRealm()) {
            authentication.securityRealm(securityRealm.getName());
        }
        else {
            securityRealm = this.serverConfigurationBuilder.getSecurityRealm(authentication.securityRealm());
        }
        if (!authentication.hasMechanisms()) {
            String serverPrincipal = null;
            for (final KerberosSecurityFactoryConfiguration identity : securityRealm.getServerIdentities().kerberosConfigurations()) {
                if (identity.getPrincipal().startsWith("HTTP/")) {
                    authentication.enable().addMechanisms(new String[] { "SPNEGO" });
                    serverPrincipal = identity.getPrincipal();
                }
                Server.log.debugf("Enabled SPNEGO authentication for HTTP using principal '%s'", (Object)identity.getPrincipal());
            }
            if (securityRealm.hasFeature(ServerSecurityRealm.Feature.TOKEN)) {
                authentication.enable().addMechanisms(new String[] { "BEARER_TOKEN" });
                Server.log.debug((Object)"Enabled BEARER_TOKEN for HTTP");
            }
            if (securityRealm.hasFeature(ServerSecurityRealm.Feature.TRUST)) {
                authentication.enable().addMechanisms(new String[] { "CLIENT_CERT" });
                Server.log.debug((Object)"Enabled CLIENT_CERT for HTTP");
            }
            if (securityRealm.hasFeature(ServerSecurityRealm.Feature.PASSWORD)) {
                authentication.enable().addMechanisms(new String[] { "DIGEST" });
                Server.log.debug((Object)"Enabled DIGEST for HTTP");
                if (this.singlePortBuilder.ssl().isEnabled()) {
                    authentication.enable().addMechanisms(new String[] { "BASIC" });
                    Server.log.debug((Object)"Enabled BASIC for HTTP");
                }
            }
            authentication.authenticator(securityRealm.getHTTPAuthenticationProvider(serverPrincipal, authentication.mechanisms()));
        }
    }
}
