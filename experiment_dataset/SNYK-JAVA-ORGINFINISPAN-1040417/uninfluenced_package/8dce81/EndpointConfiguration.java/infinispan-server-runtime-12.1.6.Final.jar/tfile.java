// 
// Decompiled by Procyon v0.5.36
// 

package org.infinispan.server.configuration.endpoint;

import org.infinispan.commons.configuration.elements.DefaultElementDefinition;
import org.infinispan.server.configuration.Element;
import java.util.Collection;
import java.util.ArrayList;
import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.infinispan.commons.configuration.elements.ElementDefinition;
import org.infinispan.server.router.configuration.SinglePortRouterConfiguration;
import org.infinispan.server.core.configuration.ProtocolServerConfiguration;
import java.util.List;
import org.infinispan.commons.configuration.attributes.AttributeDefinition;
import org.infinispan.commons.configuration.ConfigurationInfo;

public class EndpointConfiguration implements ConfigurationInfo
{
    static final AttributeDefinition<String> SOCKET_BINDING;
    static final AttributeDefinition<String> SECURITY_REALM;
    static final AttributeDefinition<Boolean> ADMIN;
    static final AttributeDefinition<Boolean> METRICS_AUTH;
    private final List<ProtocolServerConfiguration> connectors;
    private final SinglePortRouterConfiguration singlePort;
    private static final ElementDefinition ELEMENT_DEFINITION;
    private final AttributeSet attributes;
    private final List<ConfigurationInfo> configs;
    
    static AttributeSet attributeDefinitionSet() {
        return new AttributeSet((Class)EndpointConfiguration.class, new AttributeDefinition[] { EndpointConfiguration.SOCKET_BINDING, EndpointConfiguration.SECURITY_REALM, EndpointConfiguration.ADMIN, EndpointConfiguration.METRICS_AUTH });
    }
    
    EndpointConfiguration(final AttributeSet attributes, final List<ProtocolServerConfiguration> connectors, final SinglePortRouterConfiguration singlePort) {
        this.configs = new ArrayList<ConfigurationInfo>();
        this.attributes = attributes.checkProtection();
        this.connectors = connectors;
        this.singlePort = singlePort;
        this.configs.addAll((Collection<? extends ConfigurationInfo>)connectors);
    }
    
    public List<ConfigurationInfo> subElements() {
        return this.configs;
    }
    
    public SinglePortRouterConfiguration singlePortRouter() {
        return this.singlePort;
    }
    
    public List<ProtocolServerConfiguration> connectors() {
        return this.connectors;
    }
    
    public boolean admin() {
        return (boolean)this.attributes.attribute((AttributeDefinition)EndpointConfiguration.ADMIN).get();
    }
    
    public boolean metricsAuth() {
        return (boolean)this.attributes.attribute((AttributeDefinition)EndpointConfiguration.METRICS_AUTH).get();
    }
    
    public ElementDefinition getElementDefinition() {
        return EndpointConfiguration.ELEMENT_DEFINITION;
    }
    
    public AttributeSet attributes() {
        return this.attributes;
    }
    
    public String socketBinding() {
        return (String)this.attributes.attribute((AttributeDefinition)EndpointConfiguration.SOCKET_BINDING).get();
    }
    
    static {
        SOCKET_BINDING = AttributeDefinition.builder("socket-binding", (Object)null, (Class)String.class).build();
        SECURITY_REALM = AttributeDefinition.builder("security-realm", (Object)null, (Class)String.class).build();
        ADMIN = AttributeDefinition.builder("admin", (Object)true, (Class)Boolean.class).build();
        METRICS_AUTH = AttributeDefinition.builder("metrics-auth", (Object)true, (Class)Boolean.class).build();
        ELEMENT_DEFINITION = (ElementDefinition)new DefaultElementDefinition(Element.ENDPOINTS.toString());
    }
}
