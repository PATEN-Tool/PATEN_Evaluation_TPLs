// 
// Decompiled by Procyon v0.5.36
// 

package org.infinispan.server.configuration.rest;

import org.infinispan.util.logging.LogFactory;
import org.infinispan.server.core.configuration.SniConfigurationBuilder;
import org.infinispan.server.core.configuration.EncryptionConfigurationBuilder;
import java.util.Collection;
import org.infinispan.rest.configuration.AuthenticationConfigurationBuilder;
import org.infinispan.rest.configuration.CorsRuleConfigurationBuilder;
import org.infinispan.rest.configuration.CorsConfigurationBuilder;
import org.infinispan.server.security.ServerSecurityRealm;
import org.infinispan.server.configuration.endpoint.EndpointConfigurationBuilder;
import org.infinispan.server.Server;
import org.infinispan.server.core.configuration.ProtocolServerConfigurationBuilder;
import org.infinispan.rest.configuration.ExtendedHeaders;
import org.infinispan.commons.util.StringPropertyReplacer;
import org.infinispan.rest.configuration.RestServerConfigurationBuilder;
import javax.xml.stream.XMLStreamException;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import javax.xml.stream.XMLStreamReader;
import org.infinispan.configuration.parsing.ParseUtils;
import org.infinispan.server.configuration.ServerConfigurationBuilder;
import org.infinispan.server.configuration.ServerConfigurationParser;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.XMLExtendedStreamReader;
import org.infinispan.util.logging.Log;
import org.infinispan.configuration.parsing.Namespace;
import org.infinispan.configuration.parsing.Namespaces;
import org.infinispan.configuration.parsing.ConfigurationParser;

@Namespaces({ @Namespace(root = "rest-connector"), @Namespace(uri = "urn:infinispan:server:*", root = "rest-connector") })
public class RestServerConfigurationParser implements ConfigurationParser
{
    private static Log coreLog;
    
    public void readElement(final XMLExtendedStreamReader reader, final ConfigurationBuilderHolder holder) throws XMLStreamException {
        if (!holder.inScope(ServerConfigurationParser.ENDPOINTS_SCOPE)) {
            throw RestServerConfigurationParser.coreLog.invalidScope(ServerConfigurationParser.ENDPOINTS_SCOPE, holder.getScope());
        }
        final GlobalConfigurationBuilder builder = holder.getGlobalConfigurationBuilder();
        final Element element = Element.forName(reader.getLocalName());
        switch (element) {
            case REST_CONNECTOR: {
                final ServerConfigurationBuilder serverBuilder = (ServerConfigurationBuilder)builder.module((Class)ServerConfigurationBuilder.class);
                if (serverBuilder != null) {
                    this.parseRest(reader, serverBuilder);
                    return;
                }
                throw ParseUtils.unexpectedElement((XMLStreamReader)reader);
            }
            default: {
                throw ParseUtils.unexpectedElement((XMLStreamReader)reader);
            }
        }
    }
    
    public Namespace[] getNamespaces() {
        return ParseUtils.getNamespaceAnnotations((Class)this.getClass());
    }
    
    private void parseRest(final XMLExtendedStreamReader reader, final ServerConfigurationBuilder serverBuilder) throws XMLStreamException {
        boolean dedicatedSocketBinding = false;
        ServerSecurityRealm securityRealm = null;
        final EndpointConfigurationBuilder endpoint = serverBuilder.endpoints().current();
        final RestServerConfigurationBuilder builder = endpoint.addConnector(RestServerConfigurationBuilder.class);
        ServerConfigurationParser.configureEndpoint(reader.getProperties(), endpoint, builder);
        for (int i = 0; i < reader.getAttributeCount(); ++i) {
            ParseUtils.requireNoNamespaceAttribute((XMLStreamReader)reader, i);
            final String value = StringPropertyReplacer.replaceProperties(reader.getAttributeValue(i));
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case CONTEXT_PATH: {
                    builder.contextPath(value);
                    continue;
                }
                case EXTENDED_HEADERS: {
                    builder.extendedHeaders(ExtendedHeaders.valueOf(value));
                    continue;
                }
                case NAME: {
                    builder.name(value);
                    continue;
                }
                case MAX_CONTENT_LENGTH: {
                    builder.maxContentLength(Integer.parseInt(value));
                    continue;
                }
                case COMPRESSION_LEVEL: {
                    builder.compressionLevel(Integer.parseInt(value));
                    continue;
                }
                case SOCKET_BINDING: {
                    builder.socketBinding(value);
                    serverBuilder.applySocketBinding(value, (ProtocolServerConfigurationBuilder)builder, endpoint.singlePort());
                    builder.startTransport(true);
                    dedicatedSocketBinding = true;
                    continue;
                }
                case SECURITY_REALM: {
                    securityRealm = serverBuilder.getSecurityRealm(value);
                    break;
                }
            }
            ServerConfigurationParser.parseCommonConnectorAttributes(reader, i, serverBuilder, (ProtocolServerConfigurationBuilder<?, ?>)builder);
        }
        if (!dedicatedSocketBinding) {
            builder.socketBinding(endpoint.singlePort().socketBinding());
        }
        while (reader.hasNext() && reader.nextTag() != 2) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case AUTHENTICATION: {
                    this.parseAuthentication(reader, serverBuilder, builder.authentication().enable(), securityRealm);
                    continue;
                }
                case ENCRYPTION: {
                    if (!dedicatedSocketBinding) {
                        throw Server.log.cannotConfigureProtocolEncryptionUnderSinglePort();
                    }
                    this.parseEncryption(reader, serverBuilder, builder.encryption(), securityRealm);
                    continue;
                }
                case CORS_RULES: {
                    this.parseCorsRules(reader, builder);
                    continue;
                }
                default: {
                    throw ParseUtils.unexpectedElement((XMLStreamReader)reader);
                }
            }
        }
        if (securityRealm != null) {
            EndpointConfigurationBuilder.enableImplicitAuthentication(serverBuilder, securityRealm, builder);
        }
    }
    
    private void parseCorsRules(final XMLExtendedStreamReader reader, final RestServerConfigurationBuilder builder) throws XMLStreamException {
        ParseUtils.requireNoAttributes((XMLStreamReader)reader);
        final CorsConfigurationBuilder cors = builder.cors();
        while (reader.hasNext() && reader.nextTag() != 2) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case CORS_RULE: {
                    this.parseCorsRule(reader, cors.addNewRule());
                    continue;
                }
                default: {
                    throw ParseUtils.unexpectedElement((XMLStreamReader)reader);
                }
            }
        }
    }
    
    private void parseCorsRule(final XMLExtendedStreamReader reader, final CorsRuleConfigurationBuilder corsRule) throws XMLStreamException {
        for (int i = 0; i < reader.getAttributeCount(); ++i) {
            ParseUtils.requireNoNamespaceAttribute((XMLStreamReader)reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    corsRule.name(value);
                    break;
                }
                case ALLOW_CREDENTIALS: {
                    corsRule.allowCredentials(Boolean.parseBoolean(value));
                    break;
                }
                case MAX_AGE_SECONDS: {
                    corsRule.maxAge(Long.parseLong(value));
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute((XMLStreamReader)reader, i);
                }
            }
        }
        while (reader.hasNext() && reader.nextTag() != 2) {
            final Element element = Element.forName(reader.getLocalName());
            final String[] values = reader.getElementText().split(",");
            switch (element) {
                case ALLOWED_HEADERS: {
                    corsRule.allowHeaders(values);
                    continue;
                }
                case ALLOWED_ORIGINS: {
                    corsRule.allowOrigins(values);
                    continue;
                }
                case ALLOWED_METHODS: {
                    corsRule.allowMethods(values);
                    continue;
                }
                case EXPOSE_HEADERS: {
                    corsRule.exposeHeaders(values);
                    continue;
                }
                default: {
                    throw ParseUtils.unexpectedElement((XMLStreamReader)reader);
                }
            }
        }
    }
    
    private void parseAuthentication(final XMLExtendedStreamReader reader, final ServerConfigurationBuilder serverBuilder, final AuthenticationConfigurationBuilder builder, ServerSecurityRealm securityRealm) throws XMLStreamException {
        if (securityRealm == null) {
            securityRealm = serverBuilder.endpoints().current().singlePort().securityRealm();
        }
        String serverPrincipal = null;
        for (int i = 0; i < reader.getAttributeCount(); ++i) {
            ParseUtils.requireNoNamespaceAttribute((XMLStreamReader)reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case SECURITY_REALM: {
                    builder.securityRealm(value);
                    securityRealm = serverBuilder.getSecurityRealm(value);
                    break;
                }
                case MECHANISMS: {
                    builder.addMechanisms(reader.getListAttributeValue(i));
                    break;
                }
                case SERVER_PRINCIPAL: {
                    serverPrincipal = value;
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute((XMLStreamReader)reader, i);
                }
            }
        }
        ParseUtils.requireNoContent((XMLStreamReader)reader);
        if (securityRealm == null) {
            throw Server.log.authenticationWithoutSecurityRealm();
        }
        builder.authenticator(securityRealm.getHTTPAuthenticationProvider(serverPrincipal, builder.mechanisms()));
    }
    
    private void parseEncryption(final XMLExtendedStreamReader reader, final ServerConfigurationBuilder serverBuilder, final EncryptionConfigurationBuilder encryption, ServerSecurityRealm securityRealm) throws XMLStreamException {
        for (int i = 0; i < reader.getAttributeCount(); ++i) {
            ParseUtils.requireNoNamespaceAttribute((XMLStreamReader)reader, i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            final String value = reader.getAttributeValue(i);
            switch (attribute) {
                case REQUIRE_SSL_CLIENT_AUTH: {
                    encryption.requireClientAuth(Boolean.parseBoolean(value));
                    break;
                }
                case SECURITY_REALM: {
                    securityRealm = serverBuilder.getSecurityRealm(value);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute((XMLStreamReader)reader, i);
                }
            }
        }
        if (securityRealm == null) {
            throw Server.log.encryptionWithoutSecurityRealm();
        }
        final String name = securityRealm.getName();
        encryption.realm(name).sslContext(serverBuilder.getSSLContext(name));
        final boolean skipTagCheckAtTheEnd = reader.hasNext();
        while (reader.hasNext() && reader.nextTag() != 2) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case SNI: {
                    this.parseSni(reader, serverBuilder, encryption.addSni());
                    continue;
                }
                default: {
                    throw ParseUtils.unexpectedElement((XMLStreamReader)reader);
                }
            }
        }
        if (!skipTagCheckAtTheEnd) {
            ParseUtils.requireNoContent((XMLStreamReader)reader);
        }
    }
    
    private void parseSni(final XMLExtendedStreamReader reader, final ServerConfigurationBuilder serverBuilder, final SniConfigurationBuilder sni) throws XMLStreamException {
        for (int i = 0; i < reader.getAttributeCount(); ++i) {
            ParseUtils.requireNoNamespaceAttribute((XMLStreamReader)reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case HOST_NAME: {
                    sni.host(value);
                    break;
                }
                case SECURITY_REALM: {
                    sni.realm(value);
                    sni.sslContext(serverBuilder.getSSLContext(value));
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute((XMLStreamReader)reader, i);
                }
            }
        }
        ParseUtils.requireNoContent((XMLStreamReader)reader);
    }
    
    static {
        RestServerConfigurationParser.coreLog = LogFactory.getLog((Class)ServerConfigurationParser.class);
    }
}
