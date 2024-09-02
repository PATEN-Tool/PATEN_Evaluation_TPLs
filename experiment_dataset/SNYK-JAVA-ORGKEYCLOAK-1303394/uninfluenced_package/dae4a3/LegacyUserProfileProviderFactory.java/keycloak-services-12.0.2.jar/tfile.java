// 
// Decompiled by Procyon v0.5.36
// 

package org.keycloak.userprofile;

import org.keycloak.provider.Provider;
import org.keycloak.models.KeycloakSessionFactory;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Arrays;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import java.util.regex.Pattern;
import org.jboss.logging.Logger;

public class LegacyUserProfileProviderFactory implements UserProfileProviderFactory
{
    private static final Logger logger;
    UserProfileProvider provider;
    private Pattern readOnlyAttributesPattern;
    private Pattern adminReadOnlyAttributesPattern;
    private String[] DEFAULT_READ_ONLY_ATTRIBUTES;
    private String[] DEFAULT_ADMIN_READ_ONLY_ATTRIBUTES;
    public static final String PROVIDER_ID = "legacy-user-profile";
    
    public LegacyUserProfileProviderFactory() {
        this.DEFAULT_READ_ONLY_ATTRIBUTES = new String[] { "KERBEROS_PRINCIPAL", "LDAP_ID", "LDAP_ENTRY_DN", "CREATED_TIMESTAMP", "createTimestamp", "modifyTimestamp", "userCertificate", "saml.persistent.name.id.for.*", "ENABLED", "EMAIL_VERIFIED" };
        this.DEFAULT_ADMIN_READ_ONLY_ATTRIBUTES = new String[] { "KERBEROS_PRINCIPAL", "LDAP_ID", "LDAP_ENTRY_DN", "CREATED_TIMESTAMP", "createTimestamp", "modifyTimestamp" };
    }
    
    public UserProfileProvider create(final KeycloakSession session) {
        return this.provider = (UserProfileProvider)new LegacyUserProfileProvider(session, this.readOnlyAttributesPattern, this.adminReadOnlyAttributesPattern);
    }
    
    public void init(final Config.Scope config) {
        this.readOnlyAttributesPattern = this.getRegexPatternString(config, "read-only-attributes", this.DEFAULT_READ_ONLY_ATTRIBUTES);
        this.adminReadOnlyAttributesPattern = this.getRegexPatternString(config, "admin-read-only-attributes", this.DEFAULT_ADMIN_READ_ONLY_ATTRIBUTES);
    }
    
    private Pattern getRegexPatternString(final Config.Scope config, final String configKey, final String[] builtinReadOnlyAttributes) {
        final String[] readOnlyAttributesCfg = config.getArray(configKey);
        final List<String> readOnlyAttributes = new ArrayList<String>(Arrays.asList(builtinReadOnlyAttributes));
        if (readOnlyAttributesCfg != null) {
            final List<String> configured = Arrays.asList(readOnlyAttributesCfg);
            LegacyUserProfileProviderFactory.logger.infof("Configured %s: %s", (Object)configKey, (Object)configured);
            readOnlyAttributes.addAll(configured);
        }
        String s;
        String regexStr = readOnlyAttributes.stream().map(configAttrName -> {
            if (configAttrName.endsWith("*")) {
                s = "^" + Pattern.quote(configAttrName.substring(0, configAttrName.length() - 1)) + ".*$";
            }
            else {
                s = "^" + Pattern.quote(configAttrName) + "$";
            }
            return s;
        }).collect((Collector<? super Object, ?, String>)Collectors.joining("|"));
        regexStr = "(?i:" + regexStr + ")";
        LegacyUserProfileProviderFactory.logger.debugf("Regex used for %s: %s", (Object)configKey, (Object)regexStr);
        return Pattern.compile(regexStr);
    }
    
    public void postInit(final KeycloakSessionFactory factory) {
    }
    
    public void close() {
    }
    
    public String getId() {
        return "legacy-user-profile";
    }
    
    static {
        logger = Logger.getLogger((Class)LegacyUserProfileProviderFactory.class);
    }
}
