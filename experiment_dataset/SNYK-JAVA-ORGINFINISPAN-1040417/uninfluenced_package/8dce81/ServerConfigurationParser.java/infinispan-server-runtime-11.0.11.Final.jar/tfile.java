// 
// Decompiled by Procyon v0.5.36
// 

package org.infinispan.server.configuration;

import org.infinispan.util.logging.LogFactory;
import org.infinispan.server.configuration.endpoint.EndpointConfigurationBuilder;
import org.infinispan.server.core.configuration.ProtocolServerConfigurationBuilder;
import io.agroal.api.configuration.AgroalConnectionFactoryConfiguration;
import org.infinispan.server.configuration.security.KerberosSecurityFactoryConfigurationBuilder;
import org.infinispan.server.configuration.security.TrustStoreRealmConfigurationBuilder;
import org.infinispan.server.configuration.security.KeyStoreConfigurationBuilder;
import org.infinispan.server.configuration.security.SSLEngineConfigurationBuilder;
import org.infinispan.server.configuration.security.SSLConfigurationBuilder;
import org.infinispan.server.configuration.security.ServerIdentitiesConfigurationBuilder;
import org.infinispan.server.configuration.security.GroupsPropertiesConfigurationBuilder;
import org.infinispan.server.configuration.security.UserPropertiesConfigurationBuilder;
import org.infinispan.server.configuration.security.PropertiesRealmConfigurationBuilder;
import org.infinispan.server.configuration.security.LocalRealmConfigurationBuilder;
import org.infinispan.server.configuration.security.LdapAttributeConfigurationBuilder;
import org.infinispan.server.configuration.security.LdapAttributeMappingConfigurationBuilder;
import org.infinispan.server.configuration.security.LdapUserPasswordMapperConfigurationBuilder;
import org.infinispan.server.configuration.security.LdapIdentityMappingConfigurationBuilder;
import org.wildfly.security.auth.server.NameRewriter;
import org.wildfly.security.auth.util.RegexNameRewriter;
import java.util.regex.Pattern;
import org.wildfly.security.auth.realm.ldap.DirContextFactory;
import org.infinispan.server.configuration.security.LdapRealmConfigurationBuilder;
import org.infinispan.server.configuration.security.OAuth2ConfigurationBuilder;
import org.infinispan.server.configuration.security.JwtConfigurationBuilder;
import org.infinispan.server.configuration.security.TokenRealmConfigurationBuilder;
import org.infinispan.server.configuration.security.FileSystemRealmConfigurationBuilder;
import org.infinispan.server.configuration.security.RealmConfigurationBuilder;
import org.infinispan.server.configuration.security.RealmsConfigurationBuilder;
import org.infinispan.commons.CacheConfigurationException;
import java.io.IOException;
import org.infinispan.server.Server;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.configuration.internal.PrivateGlobalConfigurationBuilder;
import org.infinispan.configuration.parsing.ParserScope;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.XMLExtendedStreamReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.infinispan.configuration.parsing.ParseUtils;
import org.infinispan.util.logging.Log;
import org.infinispan.configuration.parsing.Namespace;
import org.infinispan.configuration.parsing.Namespaces;
import org.infinispan.configuration.parsing.ConfigurationParser;

@Namespaces({ @Namespace(root = "server"), @Namespace(uri = "urn:infinispan:server:*", root = "server") })
public class ServerConfigurationParser implements ConfigurationParser
{
    private static Log coreLog;
    public static String ENDPOINTS_SCOPE;
    
    public Namespace[] getNamespaces() {
        return ParseUtils.getNamespaceAnnotations((Class)this.getClass());
    }
    
    public static Element nextElement(final XMLStreamReader reader) throws XMLStreamException {
        if (reader.nextTag() == 2) {
            return null;
        }
        return Element.forName(reader.getLocalName());
    }
    
    public void readElement(final XMLExtendedStreamReader reader, final ConfigurationBuilderHolder holder) throws XMLStreamException {
        if (!holder.inScope((Enum)ParserScope.GLOBAL)) {
            throw ServerConfigurationParser.coreLog.invalidScope(ParserScope.GLOBAL.name(), holder.getScope());
        }
        final GlobalConfigurationBuilder builder = holder.getGlobalConfigurationBuilder();
        final Element element = Element.forName(reader.getLocalName());
        switch (element) {
            case SERVER: {
                ((PrivateGlobalConfigurationBuilder)builder.addModule((Class)PrivateGlobalConfigurationBuilder.class)).serverMode(true);
                final ServerConfigurationBuilder serverConfigurationBuilder = (ServerConfigurationBuilder)builder.addModule((Class)ServerConfigurationBuilder.class);
                this.parseServerElements(reader, holder, serverConfigurationBuilder);
            }
            default: {
                throw ParseUtils.unexpectedElement((XMLStreamReader)reader);
            }
        }
    }
    
    private void parseServerElements(final XMLExtendedStreamReader reader, final ConfigurationBuilderHolder holder, final ServerConfigurationBuilder builder) throws XMLStreamException {
        Element element = nextElement((XMLStreamReader)reader);
        if (element == Element.INTERFACES) {
            this.parseInterfaces(reader, builder);
            element = nextElement((XMLStreamReader)reader);
        }
        if (element == Element.SOCKET_BINDINGS) {
            this.parseSocketBindings(reader, builder);
            element = nextElement((XMLStreamReader)reader);
        }
        if (element == Element.SECURITY) {
            this.parseSecurity(reader, builder);
            element = nextElement((XMLStreamReader)reader);
        }
        if (element == Element.DATA_SOURCES) {
            this.parseDataSources(reader, builder);
            element = nextElement((XMLStreamReader)reader);
        }
        if (element == Element.ENDPOINTS) {
            this.parseEndpoints(reader, holder, builder);
            element = nextElement((XMLStreamReader)reader);
        }
        if (element != null) {
            throw ParseUtils.unexpectedElement((XMLStreamReader)reader, (Enum)element);
        }
    }
    
    private void parseSocketBindings(final XMLExtendedStreamReader reader, final ServerConfigurationBuilder builder) throws XMLStreamException {
        final SocketBindingsConfigurationBuilder socketBindings = builder.socketBindings();
        final String[] attributes = ParseUtils.requireAttributes((XMLStreamReader)reader, new Enum[] { Attribute.DEFAULT_INTERFACE, Attribute.PORT_OFFSET });
        while (reader.hasNext() && reader.nextTag() != 2) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case SOCKET_BINDING: {
                    socketBindings.defaultInterface(attributes[0]).offset(Integer.parseInt(attributes[1]));
                    this.parseSocketBinding(reader, socketBindings);
                    continue;
                }
                default: {
                    throw ParseUtils.unexpectedElement((XMLStreamReader)reader);
                }
            }
        }
    }
    
    private void parseSocketBinding(final XMLExtendedStreamReader reader, final SocketBindingsConfigurationBuilder builder) throws XMLStreamException {
        final String[] attributes = ParseUtils.requireAttributes((XMLStreamReader)reader, new Enum[] { Attribute.NAME, Attribute.PORT });
        final String name = attributes[0];
        final int port = Integer.parseInt(attributes[1]);
        String interfaceName = builder.defaultInterface();
        for (int i = 0; i < reader.getAttributeCount(); ++i) {
            ParseUtils.requireNoNamespaceAttribute((XMLStreamReader)reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME:
                case PORT: {
                    break;
                }
                case INTERFACE: {
                    interfaceName = value;
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute((XMLStreamReader)reader, i);
                }
            }
        }
        builder.socketBinding(name, port, interfaceName);
        ParseUtils.requireNoContent((XMLStreamReader)reader);
    }
    
    private void parseInterfaces(final XMLExtendedStreamReader reader, final ServerConfigurationBuilder builder) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != 2) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case INTERFACE: {
                    this.parseInterface(reader, builder);
                    continue;
                }
                default: {
                    throw ParseUtils.unexpectedElement((XMLStreamReader)reader);
                }
            }
        }
    }
    
    private void parseInterface(final XMLExtendedStreamReader reader, final ServerConfigurationBuilder builder) throws XMLStreamException {
        final String[] attributes = ParseUtils.requireAttributes((XMLStreamReader)reader, new Enum[] { Attribute.NAME });
        final String name = attributes[0];
        final InterfaceConfigurationBuilder iface = builder.interfaces().addInterface(name);
        boolean matched = false;
        final CacheConfigurationException cce = Server.log.invalidNetworkConfiguration();
        while (reader.hasNext() && reader.nextTag() != 2) {
            final Element element = Element.forName(reader.getLocalName());
            try {
                switch (element) {
                    case INET_ADDRESS: {
                        final String value = ParseUtils.requireSingleAttribute((XMLStreamReader)reader, (Enum)Attribute.VALUE);
                        ParseUtils.requireNoContent((XMLStreamReader)reader);
                        if (!matched) {
                            iface.address(AddressType.INET_ADDRESS, value);
                            break;
                        }
                        break;
                    }
                    case LINK_LOCAL: {
                        ParseUtils.requireNoAttributes((XMLStreamReader)reader);
                        ParseUtils.requireNoContent((XMLStreamReader)reader);
                        if (!matched) {
                            iface.address(AddressType.LINK_LOCAL, null);
                            break;
                        }
                        break;
                    }
                    case GLOBAL: {
                        ParseUtils.requireNoAttributes((XMLStreamReader)reader);
                        ParseUtils.requireNoContent((XMLStreamReader)reader);
                        if (!matched) {
                            iface.address(AddressType.GLOBAL, null);
                            break;
                        }
                        break;
                    }
                    case LOOPBACK: {
                        ParseUtils.requireNoAttributes((XMLStreamReader)reader);
                        ParseUtils.requireNoContent((XMLStreamReader)reader);
                        if (!matched) {
                            iface.address(AddressType.LOOPBACK, null);
                            break;
                        }
                        break;
                    }
                    case NON_LOOPBACK: {
                        ParseUtils.requireNoAttributes((XMLStreamReader)reader);
                        ParseUtils.requireNoContent((XMLStreamReader)reader);
                        if (!matched) {
                            iface.address(AddressType.NON_LOOPBACK, null);
                            break;
                        }
                        break;
                    }
                    case SITE_LOCAL: {
                        ParseUtils.requireNoAttributes((XMLStreamReader)reader);
                        ParseUtils.requireNoContent((XMLStreamReader)reader);
                        if (!matched) {
                            iface.address(AddressType.SITE_LOCAL, null);
                            break;
                        }
                        break;
                    }
                    case MATCH_INTERFACE: {
                        final String value = ParseUtils.requireSingleAttribute((XMLStreamReader)reader, (Enum)Attribute.VALUE);
                        ParseUtils.requireNoContent((XMLStreamReader)reader);
                        if (!matched) {
                            iface.address(AddressType.MATCH_INTERFACE, value);
                            break;
                        }
                        break;
                    }
                    case MATCH_ADDRESS: {
                        final String value = ParseUtils.requireSingleAttribute((XMLStreamReader)reader, (Enum)Attribute.VALUE);
                        ParseUtils.requireNoContent((XMLStreamReader)reader);
                        if (!matched) {
                            iface.address(AddressType.MATCH_ADDRESS, value);
                            break;
                        }
                        break;
                    }
                    case MATCH_HOST: {
                        final String value = ParseUtils.requireSingleAttribute((XMLStreamReader)reader, (Enum)Attribute.VALUE);
                        ParseUtils.requireNoContent((XMLStreamReader)reader);
                        if (!matched) {
                            iface.address(AddressType.MATCH_HOST, value);
                            break;
                        }
                        break;
                    }
                    case ANY_ADDRESS: {
                        ParseUtils.requireNoAttributes((XMLStreamReader)reader);
                        ParseUtils.requireNoContent((XMLStreamReader)reader);
                        if (!matched) {
                            iface.address(AddressType.ANY_ADDRESS, null);
                            break;
                        }
                        break;
                    }
                    default: {
                        throw ParseUtils.unexpectedElement((XMLStreamReader)reader);
                    }
                }
                matched = true;
            }
            catch (IOException e) {
                cce.addSuppressed((Throwable)e);
            }
        }
        if (!matched) {
            throw cce;
        }
    }
    
    private void parseSecurity(final XMLExtendedStreamReader reader, final ServerConfigurationBuilder builder) throws XMLStreamException {
        Element element = nextElement((XMLStreamReader)reader);
        if (element == Element.SECURITY_REALMS) {
            this.parseSecurityRealms(reader, builder);
            element = nextElement((XMLStreamReader)reader);
        }
        if (element != null) {
            throw ParseUtils.unexpectedElement((XMLStreamReader)reader, (Enum)element);
        }
    }
    
    private void parseSecurityRealms(final XMLExtendedStreamReader reader, final ServerConfigurationBuilder builder) throws XMLStreamException {
        final RealmsConfigurationBuilder realms = builder.security().realms();
        while (reader.hasNext() && reader.nextTag() != 2) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case SECURITY_REALM: {
                    this.parseSecurityRealm(reader, builder, realms);
                    continue;
                }
                default: {
                    throw ParseUtils.unexpectedElement((XMLStreamReader)reader);
                }
            }
        }
    }
    
    private void parseSecurityRealm(final XMLExtendedStreamReader reader, final ServerConfigurationBuilder builder, final RealmsConfigurationBuilder realms) throws XMLStreamException {
        final String name = ParseUtils.requireAttributes((XMLStreamReader)reader, new Enum[] { Attribute.NAME })[0];
        final RealmConfigurationBuilder securityRealmBuilder = realms.addSecurityRealm(name);
        Element element = nextElement((XMLStreamReader)reader);
        if (element == Element.SERVER_IDENTITIES) {
            this.parseServerIdentities(reader, securityRealmBuilder);
            element = nextElement((XMLStreamReader)reader);
        }
        if (element == Element.FILESYSTEM_REALM) {
            this.parseFileSystemRealm(reader, securityRealmBuilder.fileSystemConfiguration());
            element = nextElement((XMLStreamReader)reader);
        }
        if (element == Element.LDAP_REALM) {
            this.parseLdapRealm(reader, securityRealmBuilder.ldapConfiguration());
            element = nextElement((XMLStreamReader)reader);
        }
        if (element == Element.LOCAL_REALM) {
            this.parseLocalRealm(reader, securityRealmBuilder.localConfiguration());
            element = nextElement((XMLStreamReader)reader);
        }
        if (element == Element.PROPERTIES_REALM) {
            this.parsePropertiesRealm(reader, securityRealmBuilder.propertiesRealm());
            element = nextElement((XMLStreamReader)reader);
        }
        if (element == Element.TOKEN_REALM) {
            this.parseTokenRealm(reader, builder, securityRealmBuilder.tokenConfiguration());
            element = nextElement((XMLStreamReader)reader);
        }
        if (element == Element.TRUSTSTORE_REALM) {
            this.parseTrustStoreRealm(reader, securityRealmBuilder.trustStoreConfiguration());
            element = nextElement((XMLStreamReader)reader);
        }
        if (element != null) {
            throw ParseUtils.unexpectedElement((XMLStreamReader)reader, (Enum)element);
        }
    }
    
    private void parseFileSystemRealm(final XMLExtendedStreamReader reader, final FileSystemRealmConfigurationBuilder fileRealmBuilder) throws XMLStreamException {
        String name = "filesystem";
        final String path = ParseUtils.requireAttributes((XMLStreamReader)reader, new Enum[] { Attribute.PATH })[0];
        fileRealmBuilder.path(path);
        String relativeTo = (String)reader.getProperty("infinispan.server.data.path");
        boolean encoded = true;
        int levels = 0;
        for (int i = 0; i < reader.getAttributeCount(); ++i) {
            ParseUtils.requireNoNamespaceAttribute((XMLStreamReader)reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    name = value;
                    fileRealmBuilder.name(name);
                    break;
                }
                case ENCODED: {
                    encoded = Boolean.parseBoolean(value);
                    fileRealmBuilder.encoded(encoded);
                    break;
                }
                case LEVELS: {
                    levels = Integer.parseInt(value);
                    fileRealmBuilder.levels(levels);
                    break;
                }
                case PATH: {
                    break;
                }
                case RELATIVE_TO: {
                    relativeTo = ParseUtils.requireAttributeProperty((XMLStreamReader)reader, i);
                    fileRealmBuilder.relativeTo(relativeTo);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute((XMLStreamReader)reader, i);
                }
            }
        }
        ParseUtils.requireNoContent((XMLStreamReader)reader);
        fileRealmBuilder.name(name).path(path).relativeTo(relativeTo).levels(levels).encoded(encoded).build();
    }
    
    private void parseTokenRealm(final XMLExtendedStreamReader reader, final ServerConfigurationBuilder serverBuilder, final TokenRealmConfigurationBuilder tokenRealmConfigBuilder) throws XMLStreamException {
        final String[] required = ParseUtils.requireAttributes((XMLStreamReader)reader, new Enum[] { Attribute.NAME, Attribute.AUTH_SERVER_URL, Attribute.CLIENT_ID });
        tokenRealmConfigBuilder.name(required[0]).authServerUrl(required[1]).clientId(required[2]);
        for (int i = 0; i < reader.getAttributeCount(); ++i) {
            ParseUtils.requireNoNamespaceAttribute((XMLStreamReader)reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME:
                case AUTH_SERVER_URL:
                case CLIENT_ID: {
                    break;
                }
                case PRINCIPAL_CLAIM: {
                    tokenRealmConfigBuilder.principalClaim(value);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute((XMLStreamReader)reader, i);
                }
            }
        }
        Element element = nextElement((XMLStreamReader)reader);
        if (element == Element.JWT) {
            this.parseJWT(reader, serverBuilder, tokenRealmConfigBuilder.jwtConfiguration());
            element = nextElement((XMLStreamReader)reader);
        }
        else if (element == Element.OAUTH2_INTROSPECTION) {
            this.parseOauth2Introspection(reader, serverBuilder, tokenRealmConfigBuilder.oauth2Configuration());
            element = nextElement((XMLStreamReader)reader);
        }
        if (element != null) {
            throw ParseUtils.unexpectedElement((XMLStreamReader)reader);
        }
        tokenRealmConfigBuilder.build();
    }
    
    private void parseJWT(final XMLExtendedStreamReader reader, final ServerConfigurationBuilder serverBuilder, final JwtConfigurationBuilder jwtBuilder) throws XMLStreamException {
        for (int i = 0; i < reader.getAttributeCount(); ++i) {
            ParseUtils.requireNoNamespaceAttribute((XMLStreamReader)reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case ISSUER: {
                    jwtBuilder.issuers(reader.getListAttributeValue(i));
                    break;
                }
                case AUDIENCE: {
                    jwtBuilder.audience(reader.getListAttributeValue(i));
                    break;
                }
                case PUBLIC_KEY: {
                    jwtBuilder.publicKey(value);
                    break;
                }
                case JKU_TIMEOUT: {
                    jwtBuilder.jkuTimeout(Long.parseLong(value));
                    break;
                }
                case CLIENT_SSL_CONTEXT: {
                    jwtBuilder.clientSSLContext(value);
                    break;
                }
                case HOST_NAME_VERIFICATION_POLICY: {
                    jwtBuilder.hostNameVerificationPolicy(value);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute((XMLStreamReader)reader, i);
                }
            }
        }
        ParseUtils.requireNoContent((XMLStreamReader)reader);
    }
    
    private void parseOauth2Introspection(final XMLExtendedStreamReader reader, final ServerConfigurationBuilder serverBuilder, final OAuth2ConfigurationBuilder oauthBuilder) throws XMLStreamException {
        final String[] required = ParseUtils.requireAttributes((XMLStreamReader)reader, new Enum[] { Attribute.CLIENT_ID, Attribute.CLIENT_SECRET, Attribute.INTROSPECTION_URL });
        oauthBuilder.clientId(required[0]).clientSecret(required[1]).introspectionUrl(required[2]);
        for (int i = 0; i < reader.getAttributeCount(); ++i) {
            ParseUtils.requireNoNamespaceAttribute((XMLStreamReader)reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case CLIENT_ID:
                case CLIENT_SECRET:
                case INTROSPECTION_URL: {
                    break;
                }
                case CLIENT_SSL_CONTEXT: {
                    oauthBuilder.clientSSLContext(value);
                    break;
                }
                case HOST_NAME_VERIFICATION_POLICY: {
                    oauthBuilder.hostVerificationPolicy(value);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute((XMLStreamReader)reader, i);
                }
            }
        }
        ParseUtils.requireNoContent((XMLStreamReader)reader);
    }
    
    private void parseLdapRealm(final XMLExtendedStreamReader reader, final LdapRealmConfigurationBuilder ldapRealmConfigBuilder) throws XMLStreamException {
        ldapRealmConfigBuilder.name("ldap");
        for (int i = 0; i < reader.getAttributeCount(); ++i) {
            ParseUtils.requireNoNamespaceAttribute((XMLStreamReader)reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    ldapRealmConfigBuilder.name(value);
                    break;
                }
                case URL: {
                    ldapRealmConfigBuilder.url(value);
                    break;
                }
                case PRINCIPAL: {
                    ldapRealmConfigBuilder.principal(value);
                    break;
                }
                case CREDENTIAL: {
                    ldapRealmConfigBuilder.credential(value);
                    break;
                }
                case DIRECT_VERIFICATION: {
                    ldapRealmConfigBuilder.directEvidenceVerification(Boolean.parseBoolean(value));
                    break;
                }
                case PAGE_SIZE: {
                    ldapRealmConfigBuilder.pageSize(Integer.parseInt(value));
                    break;
                }
                case SEARCH_DN: {
                    ldapRealmConfigBuilder.searchDn(value);
                    break;
                }
                case RDN_IDENTIFIER: {
                    ldapRealmConfigBuilder.rdnIdentifier(value);
                    break;
                }
                case CONNECTION_POOLING: {
                    ldapRealmConfigBuilder.connectionPooling(Boolean.parseBoolean(value));
                    break;
                }
                case CONNECTION_TIMEOUT: {
                    ldapRealmConfigBuilder.connectionTimeout(Integer.parseInt(value));
                    break;
                }
                case READ_TIMEOUT: {
                    ldapRealmConfigBuilder.readTimeout(Integer.parseInt(value));
                    break;
                }
                case REFERRAL_MODE: {
                    ldapRealmConfigBuilder.referralMode(DirContextFactory.ReferralMode.valueOf(value.toUpperCase()));
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute((XMLStreamReader)reader, i);
                }
            }
        }
        Element element = nextElement((XMLStreamReader)reader);
        if (element == Element.NAME_REWRITER) {
            this.parseNameRewriter(reader, ldapRealmConfigBuilder);
            element = nextElement((XMLStreamReader)reader);
        }
        while (element == Element.IDENTITY_MAPPING) {
            this.parseLdapIdentityMapping(reader, ldapRealmConfigBuilder.addIdentityMap());
            element = nextElement((XMLStreamReader)reader);
        }
        ldapRealmConfigBuilder.build();
    }
    
    private void parseNameRewriter(final XMLExtendedStreamReader reader, final LdapRealmConfigurationBuilder builder) throws XMLStreamException {
        final Element element = nextElement((XMLStreamReader)reader);
        switch (element) {
            case REGEX_PRINCIPAL_TRANSFORMER: {
                final String[] attributes = ParseUtils.requireAttributes((XMLStreamReader)reader, new Enum[] { Attribute.NAME, Attribute.PATTERN, Attribute.REPLACEMENT });
                boolean replaceAll = false;
                for (int i = 0; i < reader.getAttributeCount(); ++i) {
                    ParseUtils.requireNoNamespaceAttribute((XMLStreamReader)reader, i);
                    final String value = reader.getAttributeValue(i);
                    final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                    switch (attribute) {
                        case NAME:
                        case PATTERN:
                        case REPLACEMENT: {
                            break;
                        }
                        case REPLACE_ALL: {
                            replaceAll = Boolean.parseBoolean(value);
                            break;
                        }
                        default: {
                            throw ParseUtils.unexpectedAttribute((XMLStreamReader)reader, i);
                        }
                    }
                }
                builder.nameRewriter((NameRewriter)new RegexNameRewriter(Pattern.compile(attributes[1]), attributes[2], replaceAll));
                ParseUtils.requireNoContent((XMLStreamReader)reader);
                ParseUtils.requireNoContent((XMLStreamReader)reader);
            }
            default: {
                throw ParseUtils.unexpectedElement((XMLStreamReader)reader);
            }
        }
    }
    
    private void parseLdapIdentityMapping(final XMLExtendedStreamReader reader, final LdapIdentityMappingConfigurationBuilder identityMapBuilder) throws XMLStreamException {
        for (int i = 0; i < reader.getAttributeCount(); ++i) {
            ParseUtils.requireNoNamespaceAttribute((XMLStreamReader)reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case SEARCH_DN: {
                    identityMapBuilder.searchBaseDn(value);
                    break;
                }
                case RDN_IDENTIFIER: {
                    identityMapBuilder.rdnIdentifier(value);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute((XMLStreamReader)reader, i);
                }
            }
        }
        final LdapUserPasswordMapperConfigurationBuilder userMapperBuilder = identityMapBuilder.addUserPasswordMapper();
        final LdapAttributeMappingConfigurationBuilder attributeMapperBuilder = identityMapBuilder.addAttributeMapping();
        while (reader.hasNext() && reader.nextTag() != 2) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case ATTRIBUTE_MAPPING: {
                    this.parseLdapAttributeMapping(reader, attributeMapperBuilder);
                    continue;
                }
                case USER_PASSWORD_MAPPER: {
                    this.parseLdapUserPasswordMapper(reader, userMapperBuilder);
                    continue;
                }
                default: {
                    throw ParseUtils.unexpectedElement((XMLStreamReader)reader);
                }
            }
        }
    }
    
    private void parseLdapUserPasswordMapper(final XMLExtendedStreamReader reader, final LdapUserPasswordMapperConfigurationBuilder userMapperBuilder) throws XMLStreamException {
        for (int i = 0; i < reader.getAttributeCount(); ++i) {
            ParseUtils.requireNoNamespaceAttribute((XMLStreamReader)reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case FROM: {
                    userMapperBuilder.from(value);
                    break;
                }
                case WRITABLE: {
                    final boolean booleanVal = Boolean.parseBoolean(value);
                    userMapperBuilder.writable(booleanVal);
                    break;
                }
                case VERIFIABLE: {
                    final boolean booleanVal = Boolean.parseBoolean(value);
                    userMapperBuilder.verifiable(booleanVal);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute((XMLStreamReader)reader, i);
                }
            }
        }
        ParseUtils.requireNoContent((XMLStreamReader)reader);
        userMapperBuilder.build();
    }
    
    private void parseLdapAttributeMapping(final XMLExtendedStreamReader reader, final LdapAttributeMappingConfigurationBuilder attributeMapperBuilder) throws XMLStreamException {
        ParseUtils.requireNoAttributes((XMLStreamReader)reader);
        while (reader.hasNext() && reader.nextTag() != 2) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case ATTRIBUTE: {
                    this.parseLdapAttributeFilter(reader, attributeMapperBuilder.addAttribute());
                    continue;
                }
                case ATTRIBUTE_REFERENCE: {
                    this.parseLdapAttributeReference(reader, attributeMapperBuilder.addAttribute());
                    continue;
                }
                default: {
                    throw ParseUtils.unexpectedElement((XMLStreamReader)reader);
                }
            }
        }
    }
    
    private void parseLdapAttributeFilter(final XMLExtendedStreamReader reader, final LdapAttributeConfigurationBuilder attributeBuilder) throws XMLStreamException {
        final String filter = ParseUtils.requireAttributes((XMLStreamReader)reader, new Enum[] { Attribute.FILTER })[0];
        this.parseLdapAttribute(reader, attributeBuilder.filter(filter));
    }
    
    private void parseLdapAttributeReference(final XMLExtendedStreamReader reader, final LdapAttributeConfigurationBuilder attributeBuilder) throws XMLStreamException {
        final String reference = ParseUtils.requireAttributes((XMLStreamReader)reader, new Enum[] { Attribute.REFERENCE })[0];
        this.parseLdapAttribute(reader, attributeBuilder.reference(reference));
    }
    
    private void parseLdapAttribute(final XMLExtendedStreamReader reader, final LdapAttributeConfigurationBuilder attributeBuilder) throws XMLStreamException {
        for (int i = 0; i < reader.getAttributeCount(); ++i) {
            ParseUtils.requireNoNamespaceAttribute((XMLStreamReader)reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case FROM: {
                    attributeBuilder.from(value);
                    break;
                }
                case TO: {
                    attributeBuilder.to(value);
                    break;
                }
                case FILTER_DN: {
                    attributeBuilder.filterBaseDn(value);
                    break;
                }
                case SEARCH_RECURSIVE: {
                    attributeBuilder.searchRecursive(Boolean.parseBoolean(value));
                    break;
                }
                case ROLE_RECURSION: {
                    attributeBuilder.roleRecursion(Integer.parseInt(value));
                    break;
                }
                case ROLE_RECURSION_NAME: {
                    attributeBuilder.roleRecursionName(value);
                    break;
                }
                case EXTRACT_RDN: {
                    attributeBuilder.extractRdn(value);
                    throw ParseUtils.unexpectedAttribute((XMLStreamReader)reader, i);
                }
            }
        }
        attributeBuilder.build();
        ParseUtils.requireNoContent((XMLStreamReader)reader);
    }
    
    private void parseLocalRealm(final XMLExtendedStreamReader reader, final LocalRealmConfigurationBuilder localBuilder) throws XMLStreamException {
        final String name = ParseUtils.requireAttributes((XMLStreamReader)reader, new Enum[] { Attribute.NAME })[0];
        localBuilder.name(name);
        ParseUtils.requireNoContent((XMLStreamReader)reader);
    }
    
    private void parsePropertiesRealm(final XMLExtendedStreamReader reader, final PropertiesRealmConfigurationBuilder propertiesBuilder) throws XMLStreamException {
        final String name = "properties";
        boolean plainText = false;
        String realmName = name;
        String groupsAttribute = "groups";
        propertiesBuilder.groupAttribute(groupsAttribute);
        final UserPropertiesConfigurationBuilder userPropertiesBuilder = propertiesBuilder.userProperties();
        final GroupsPropertiesConfigurationBuilder groupsBuilder = propertiesBuilder.groupProperties();
        userPropertiesBuilder.digestRealmName(name).plainText(false);
        int i = 0;
        while (i < reader.getAttributeCount()) {
            ParseUtils.requireNoNamespaceAttribute((XMLStreamReader)reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case GROUPS_ATTRIBUTE: {
                    groupsAttribute = value;
                    ++i;
                    continue;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute((XMLStreamReader)reader, i);
                }
            }
        }
        propertiesBuilder.groupAttribute(groupsAttribute);
        Element element = nextElement((XMLStreamReader)reader);
        if (element == Element.USER_PROPERTIES) {
            final String path = ParseUtils.requireAttributes((XMLStreamReader)reader, new Enum[] { Attribute.PATH })[0];
            String relativeTo = (String)reader.getProperty("infinispan.server.config.path");
            for (int j = 0; j < reader.getAttributeCount(); ++j) {
                ParseUtils.requireNoNamespaceAttribute((XMLStreamReader)reader, j);
                final String value2 = reader.getAttributeValue(j);
                final Attribute attribute2 = Attribute.forName(reader.getAttributeLocalName(j));
                switch (attribute2) {
                    case PATH: {
                        break;
                    }
                    case RELATIVE_TO: {
                        relativeTo = ParseUtils.requireAttributeProperty((XMLStreamReader)reader, j);
                        break;
                    }
                    case DIGEST_REALM_NAME: {
                        realmName = value2;
                        break;
                    }
                    case PLAIN_TEXT: {
                        plainText = Boolean.parseBoolean(value2);
                        break;
                    }
                    default: {
                        throw ParseUtils.unexpectedAttribute((XMLStreamReader)reader, j);
                    }
                }
            }
            userPropertiesBuilder.path(path).relativeTo(relativeTo).plainText(plainText).digestRealmName(realmName);
            ParseUtils.requireNoContent((XMLStreamReader)reader);
            element = nextElement((XMLStreamReader)reader);
        }
        if (element == Element.GROUP_PROPERTIES) {
            final String path = ParseUtils.requireAttributes((XMLStreamReader)reader, new Enum[] { Attribute.PATH })[0];
            String relativeTo = (String)reader.getProperty("infinispan.server.config.path");
            for (int j = 0; j < reader.getAttributeCount(); ++j) {
                ParseUtils.requireNoNamespaceAttribute((XMLStreamReader)reader, j);
                final Attribute attribute3 = Attribute.forName(reader.getAttributeLocalName(j));
                switch (attribute3) {
                    case PATH: {
                        break;
                    }
                    case RELATIVE_TO: {
                        relativeTo = ParseUtils.requireAttributeProperty((XMLStreamReader)reader, j);
                        break;
                    }
                    default: {
                        throw ParseUtils.unexpectedAttribute((XMLStreamReader)reader, j);
                    }
                }
            }
            groupsBuilder.path(path).relativeTo(relativeTo);
            ParseUtils.requireNoContent((XMLStreamReader)reader);
            element = nextElement((XMLStreamReader)reader);
        }
        if (element != null) {
            throw ParseUtils.unexpectedElement((XMLStreamReader)reader, (Enum)element);
        }
        propertiesBuilder.build();
    }
    
    private void parseServerIdentities(final XMLExtendedStreamReader reader, final RealmConfigurationBuilder securityRealmBuilder) throws XMLStreamException {
        final ServerIdentitiesConfigurationBuilder identitiesBuilder = securityRealmBuilder.serverIdentitiesConfiguration();
        Element element = nextElement((XMLStreamReader)reader);
        if (element == Element.SSL) {
            this.parseSSL(reader, identitiesBuilder);
            element = nextElement((XMLStreamReader)reader);
        }
        while (element == Element.KERBEROS) {
            this.parseKerberos(reader, identitiesBuilder);
            element = nextElement((XMLStreamReader)reader);
        }
        if (element != null) {
            throw ParseUtils.unexpectedElement((XMLStreamReader)reader, (Enum)element);
        }
    }
    
    private void parseSSL(final XMLExtendedStreamReader reader, final ServerIdentitiesConfigurationBuilder identitiesBuilder) throws XMLStreamException {
        final SSLConfigurationBuilder serverIdentitiesBuilder = identitiesBuilder.addSslConfiguration();
        Element element = nextElement((XMLStreamReader)reader);
        if (element == Element.KEYSTORE) {
            this.parseKeyStore(reader, serverIdentitiesBuilder.keyStore());
            element = nextElement((XMLStreamReader)reader);
        }
        if (element == Element.ENGINE) {
            this.parseSSLEngine(reader, serverIdentitiesBuilder.engine());
            element = nextElement((XMLStreamReader)reader);
        }
        if (element != null) {
            throw ParseUtils.unexpectedElement((XMLStreamReader)reader, (Enum)element);
        }
    }
    
    private void parseSSLEngine(final XMLExtendedStreamReader reader, final SSLEngineConfigurationBuilder engine) throws XMLStreamException {
        for (int i = 0; i < reader.getAttributeCount(); ++i) {
            ParseUtils.requireNoNamespaceAttribute((XMLStreamReader)reader, i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case ENABLED_PROTOCOLS: {
                    engine.enabledProtocols(reader.getListAttributeValue(i));
                    break;
                }
                case ENABLED_CIPHERSUITES: {
                    engine.enabledCiphersuites(reader.getAttributeValue(i));
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute((XMLStreamReader)reader, i);
                }
            }
        }
        ParseUtils.requireNoContent((XMLStreamReader)reader);
    }
    
    private void parseKeyStore(final XMLExtendedStreamReader reader, final KeyStoreConfigurationBuilder keyStoreBuilder) throws XMLStreamException {
        final String[] attributes = ParseUtils.requireAttributes((XMLStreamReader)reader, new Enum[] { Attribute.PATH });
        keyStoreBuilder.path(attributes[0]);
        String relativeTo = (String)reader.getProperty("infinispan.server.config.path");
        keyStoreBuilder.relativeTo(relativeTo);
        for (int i = 0; i < reader.getAttributeCount(); ++i) {
            ParseUtils.requireNoNamespaceAttribute((XMLStreamReader)reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case PATH: {
                    break;
                }
                case PROVIDER: {
                    keyStoreBuilder.provider(value);
                    break;
                }
                case RELATIVE_TO: {
                    relativeTo = ParseUtils.requireAttributeProperty((XMLStreamReader)reader, i);
                    keyStoreBuilder.relativeTo(relativeTo);
                    break;
                }
                case KEYSTORE_PASSWORD: {
                    keyStoreBuilder.keyStorePassword(value.toCharArray());
                    break;
                }
                case ALIAS: {
                    keyStoreBuilder.alias(value);
                    break;
                }
                case KEY_PASSWORD: {
                    keyStoreBuilder.keyPassword(value.toCharArray());
                    break;
                }
                case GENERATE_SELF_SIGNED_CERTIFICATE_HOST: {
                    keyStoreBuilder.generateSelfSignedCertificateHost(value);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute((XMLStreamReader)reader, i);
                }
            }
        }
        ParseUtils.requireNoContent((XMLStreamReader)reader);
        keyStoreBuilder.build();
    }
    
    private void parseTrustStoreRealm(final XMLExtendedStreamReader reader, final TrustStoreRealmConfigurationBuilder trustStoreBuilder) throws XMLStreamException {
        String name = "trust";
        final String[] attributes = ParseUtils.requireAttributes((XMLStreamReader)reader, new Enum[] { Attribute.PATH });
        final String path = attributes[0];
        String relativeTo = (String)reader.getProperty("infinispan.server.config.path");
        String keyStoreProvider = null;
        char[] keyStorePassword = null;
        for (int i = 0; i < reader.getAttributeCount(); ++i) {
            ParseUtils.requireNoNamespaceAttribute((XMLStreamReader)reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    name = value;
                    break;
                }
                case PATH: {
                    break;
                }
                case PROVIDER: {
                    keyStoreProvider = value;
                    break;
                }
                case KEYSTORE_PASSWORD: {
                    keyStorePassword = value.toCharArray();
                    break;
                }
                case RELATIVE_TO: {
                    relativeTo = ParseUtils.requireAttributeProperty((XMLStreamReader)reader, i);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute((XMLStreamReader)reader, i);
                }
            }
        }
        trustStoreBuilder.name(name).path(path).relativeTo(relativeTo).keyStorePassword(keyStorePassword).provider(keyStoreProvider);
        ParseUtils.requireNoContent((XMLStreamReader)reader);
        trustStoreBuilder.build();
    }
    
    private void parseKerberos(final XMLExtendedStreamReader reader, final ServerIdentitiesConfigurationBuilder identitiesBuilder) throws XMLStreamException {
        final KerberosSecurityFactoryConfigurationBuilder builder = identitiesBuilder.addKerberosConfiguration();
        final String[] attributes = ParseUtils.requireAttributes((XMLStreamReader)reader, new Enum[] { Attribute.KEYTAB_PATH, Attribute.PRINCIPAL });
        builder.keyTabPath(attributes[0]).relativeTo((String)reader.getProperty("infinispan.server.config.path"));
        builder.principal(attributes[1]);
        for (int i = 0; i < reader.getAttributeCount(); ++i) {
            ParseUtils.requireNoNamespaceAttribute((XMLStreamReader)reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case PRINCIPAL:
                case KEYTAB_PATH: {
                    break;
                }
                case RELATIVE_TO: {
                    builder.relativeTo(ParseUtils.requireAttributeProperty((XMLStreamReader)reader, i));
                    break;
                }
                case DEBUG: {
                    builder.debug(Boolean.parseBoolean(value));
                    break;
                }
                case FAIL_CACHE: {
                    builder.failCache(Long.parseLong(value));
                    break;
                }
                case MECHANISM_NAMES: {
                    for (final String name : ParseUtils.getListAttributeValue(value)) {
                        builder.addMechanismName(name);
                    }
                    break;
                }
                case MECHANISM_OIDS: {
                    for (final String oid : ParseUtils.getListAttributeValue(value)) {
                        builder.addMechanismOid(oid);
                    }
                    break;
                }
                case MINIMUM_REMAINING_LIFETIME: {
                    builder.minimumRemainingLifetime(Integer.parseInt(value));
                    break;
                }
                case OBTAIN_KERBEROS_TICKET: {
                    builder.obtainKerberosTicket(Boolean.parseBoolean(value));
                    break;
                }
                case REQUEST_LIFETIME: {
                    builder.requestLifetime(Integer.parseInt(value));
                    break;
                }
                case REQUIRED: {
                    builder.checkKeyTab(Boolean.parseBoolean(value));
                    break;
                }
                case SERVER: {
                    builder.server(Boolean.parseBoolean(value));
                    break;
                }
                case WRAP_GSS_CREDENTIAL: {
                    builder.wrapGssCredential(Boolean.parseBoolean(value));
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute((XMLStreamReader)reader, i);
                }
            }
        }
        while (reader.hasNext() && reader.nextTag() != 2) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case OPTION: {
                    final String[] option = ParseUtils.requireAttributes((XMLStreamReader)reader, new Enum[] { Attribute.NAME, Attribute.VALUE });
                    builder.addOption(option[0], option[1]);
                    continue;
                }
                default: {
                    throw ParseUtils.unexpectedElement((XMLStreamReader)reader);
                }
            }
        }
    }
    
    private void parseDataSources(final XMLExtendedStreamReader reader, final ServerConfigurationBuilder builder) throws XMLStreamException {
        final DataSourcesConfigurationBuilder dataSources = builder.dataSources();
        while (reader.hasNext() && reader.nextTag() != 2) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case DATA_SOURCE: {
                    this.parseDataSource(reader, dataSources);
                    continue;
                }
                default: {
                    throw ParseUtils.unexpectedElement((XMLStreamReader)reader);
                }
            }
        }
    }
    
    private void parseDataSource(final XMLExtendedStreamReader reader, final DataSourcesConfigurationBuilder dataSourcesBuilder) throws XMLStreamException {
        final String[] attributes = ParseUtils.requireAttributes((XMLStreamReader)reader, new Enum[] { Attribute.NAME, Attribute.JNDI_NAME });
        final String name = attributes[0];
        final String jndiName = attributes[1];
        final DataSourceConfigurationBuilder dataSourceBuilder = dataSourcesBuilder.dataSource(name, jndiName);
        for (int i = 0; i < reader.getAttributeCount(); ++i) {
            ParseUtils.requireNoNamespaceAttribute((XMLStreamReader)reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME:
                case JNDI_NAME: {
                    break;
                }
                case STATISTICS: {
                    dataSourceBuilder.statistics(Boolean.parseBoolean(value));
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute((XMLStreamReader)reader, i);
                }
            }
        }
        Element element = nextElement((XMLStreamReader)reader);
        if (element != Element.CONNECTION_FACTORY) {
            throw ParseUtils.unexpectedElement((XMLStreamReader)reader, (Enum)element);
        }
        this.parseDataSourceConnectionFactory(reader, dataSourceBuilder);
        element = nextElement((XMLStreamReader)reader);
        if (element != Element.CONNECTION_POOL) {
            throw ParseUtils.unexpectedElement((XMLStreamReader)reader, (Enum)element);
        }
        this.parseDataSourceConnectionPool(reader, dataSourceBuilder);
        element = nextElement((XMLStreamReader)reader);
        if (element != null) {
            throw ParseUtils.unexpectedElement((XMLStreamReader)reader, (Enum)element);
        }
    }
    
    private void parseDataSourceConnectionFactory(final XMLExtendedStreamReader reader, final DataSourceConfigurationBuilder dataSourceBuilder) throws XMLStreamException {
        final String[] attributes = ParseUtils.requireAttributes((XMLStreamReader)reader, new Enum[] { Attribute.DRIVER });
        dataSourceBuilder.driver(attributes[0]);
        for (int i = 0; i < reader.getAttributeCount(); ++i) {
            ParseUtils.requireNoNamespaceAttribute((XMLStreamReader)reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case DRIVER: {
                    break;
                }
                case USERNAME: {
                    dataSourceBuilder.username(value);
                    break;
                }
                case PASSWORD: {
                    dataSourceBuilder.password(value);
                    break;
                }
                case URL: {
                    dataSourceBuilder.url(value);
                    break;
                }
                case TRANSACTION_ISOLATION: {
                    dataSourceBuilder.transactionIsolation(AgroalConnectionFactoryConfiguration.TransactionIsolation.valueOf(value));
                    break;
                }
                case NEW_CONNECTION_SQL: {
                    dataSourceBuilder.newConnectionSql(value);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute((XMLStreamReader)reader, i);
                }
            }
        }
        while (reader.hasNext() && reader.nextTag() != 2) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case CONNECTION_PROPERTY: {
                    dataSourceBuilder.addProperty(ParseUtils.requireSingleAttribute((XMLStreamReader)reader, (Enum)Attribute.NAME), reader.getElementText());
                    continue;
                }
                default: {
                    throw ParseUtils.unexpectedElement((XMLStreamReader)reader);
                }
            }
        }
    }
    
    private void parseDataSourceConnectionPool(final XMLExtendedStreamReader reader, final DataSourceConfigurationBuilder dataSourceBuilder) throws XMLStreamException {
        final String[] attributes = ParseUtils.requireAttributes((XMLStreamReader)reader, new Enum[] { Attribute.MAX_SIZE });
        dataSourceBuilder.maxSize(Integer.parseInt(attributes[0]));
        for (int i = 0; i < reader.getAttributeCount(); ++i) {
            ParseUtils.requireNoNamespaceAttribute((XMLStreamReader)reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case MAX_SIZE: {
                    break;
                }
                case MIN_SIZE: {
                    dataSourceBuilder.minSize(Integer.parseInt(value));
                    break;
                }
                case INITIAL_SIZE: {
                    dataSourceBuilder.initialSize(Integer.parseInt(value));
                    break;
                }
                case BLOCKING_TIMEOUT: {
                    dataSourceBuilder.blockingTimeout(Integer.parseInt(value));
                    break;
                }
                case BACKGROUND_VALIDATION: {
                    dataSourceBuilder.backgroundValidation(Long.parseLong(value));
                    break;
                }
                case LEAK_DETECTION: {
                    dataSourceBuilder.leakDetection(Long.parseLong(value));
                    break;
                }
                case IDLE_REMOVAL: {
                    dataSourceBuilder.idleRemoval(Integer.parseInt(value));
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute((XMLStreamReader)reader, i);
                }
            }
        }
        ParseUtils.requireNoContent((XMLStreamReader)reader);
    }
    
    private void parseEndpoints(final XMLExtendedStreamReader reader, final ConfigurationBuilderHolder holder, final ServerConfigurationBuilder builder) throws XMLStreamException {
        holder.pushScope(ServerConfigurationParser.ENDPOINTS_SCOPE);
        final String socketBinding = ParseUtils.requireAttributes((XMLStreamReader)reader, new Enum[] { Attribute.SOCKET_BINDING })[0];
        final EndpointConfigurationBuilder endpoint = builder.endpoints().addEndpoint(socketBinding);
        for (int i = 0; i < reader.getAttributeCount(); ++i) {
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            final String value = reader.getAttributeValue(i);
            switch (attribute) {
                case SOCKET_BINDING: {
                    continue;
                }
                case METRICS_AUTH: {
                    endpoint.metricsAuth(Boolean.parseBoolean(value));
                    continue;
                }
                case SECURITY_REALM: {
                    endpoint.securityRealm(value).implicitConnectorSecurity(reader.getSchema().since(11, 0));
                    break;
                }
            }
            parseCommonConnectorAttributes(reader, i, builder, endpoint.singlePort());
        }
        while (reader.hasNext() && reader.nextTag() != 2) {
            reader.handleAny(holder);
        }
        holder.popScope();
    }
    
    public static void parseCommonConnectorAttributes(final XMLExtendedStreamReader reader, final int index, final ServerConfigurationBuilder serverBuilder, final ProtocolServerConfigurationBuilder<?, ?> builder) throws XMLStreamException {
        if (ParseUtils.isNoNamespaceAttribute((XMLStreamReader)reader, index)) {
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(index));
            final String value = reader.getAttributeValue(index);
            switch (attribute) {
                case CACHE_CONTAINER: {
                    break;
                }
                case IDLE_TIMEOUT: {
                    builder.idleTimeout(Integer.parseInt(value));
                    break;
                }
                case IO_THREADS: {
                    builder.ioThreads(Integer.parseInt(value));
                    break;
                }
                case RECEIVE_BUFFER_SIZE: {
                    builder.recvBufSize(Integer.parseInt(value));
                    break;
                }
                case REQUIRE_SSL_CLIENT_AUTH: {
                    builder.ssl().requireClientAuth(Boolean.parseBoolean(value));
                    break;
                }
                case SECURITY_REALM: {
                    if (serverBuilder.hasSSLContext(value)) {
                        builder.ssl().enable().sslContext(serverBuilder.getSSLContext(value));
                        break;
                    }
                    break;
                }
                case SEND_BUFFER_SIZE: {
                    builder.sendBufSize(Integer.parseInt(value));
                    break;
                }
                case TCP_KEEPALIVE: {
                    builder.tcpKeepAlive(Boolean.parseBoolean(value));
                    break;
                }
                case TCP_NODELAY: {
                    builder.tcpNoDelay(Boolean.parseBoolean(value));
                    break;
                }
                case WORKER_THREADS: {
                    builder.workerThreads(Integer.parseInt(value));
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute((XMLStreamReader)reader, index);
                }
            }
        }
    }
    
    static {
        ServerConfigurationParser.coreLog = LogFactory.getLog((Class)ServerConfigurationParser.class);
        ServerConfigurationParser.ENDPOINTS_SCOPE = "ENDPOINTS";
    }
}
