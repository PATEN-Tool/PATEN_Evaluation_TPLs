// 
// Decompiled by Procyon v0.5.36
// 

package io.hawt.web.auth;

import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import io.hawt.system.ConfigManager;
import javax.servlet.ServletContext;
import javax.security.auth.login.Configuration;
import org.slf4j.Logger;

public class AuthenticationConfiguration
{
    private static final transient Logger LOG;
    public static final String LOGIN_URL = "/auth/login";
    public static final String[] UNSECURED_PATHS;
    public static final String AUTHENTICATION_ENABLED = "authenticationEnabled";
    public static final String NO_CREDENTIALS_401 = "noCredentials401";
    public static final String REALM = "realm";
    public static final String ROLE = "role";
    public static final String ROLES = "roles";
    public static final String ROLE_PRINCIPAL_CLASSES = "rolePrincipalClasses";
    public static final String AUTHENTICATION_CONTAINER_DISCOVERY_CLASSES = "authenticationContainerDiscoveryClasses";
    public static final String KEYCLOAK_ENABLED = "keycloakEnabled";
    public static final String HAWTIO_AUTHENTICATION_ENABLED = "hawtio.authenticationEnabled";
    public static final String HAWTIO_NO_CREDENTIALS_401 = "hawtio.noCredentials401";
    public static final String HAWTIO_REALM = "hawtio.realm";
    public static final String HAWTIO_ROLE = "hawtio.role";
    public static final String HAWTIO_ROLES = "hawtio.roles";
    public static final String HAWTIO_ROLE_PRINCIPAL_CLASSES = "hawtio.rolePrincipalClasses";
    public static final String HAWTIO_AUTH_CONTAINER_DISCOVERY_CLASSES = "hawtio.authenticationContainerDiscoveryClasses";
    public static final String HAWTIO_KEYCLOAK_ENABLED = "hawtio.keycloakEnabled";
    public static final String AUTHENTICATION_CONFIGURATION = "authenticationConfig";
    public static final String CONFIG_MANAGER = "ConfigManager";
    public static final String DEFAULT_REALM = "karaf";
    private static final String DEFAULT_KARAF_ROLES = "admin,manager,viewer";
    public static final String DEFAULT_KARAF_ROLE_PRINCIPAL_CLASSES = "org.apache.karaf.jaas.boot.principal.RolePrincipal,org.apache.karaf.jaas.modules.RolePrincipal,org.apache.karaf.jaas.boot.principal.GroupPrincipal";
    public static final String TOMCAT_AUTH_CONTAINER_DISCOVERY = "io.hawt.web.tomcat.TomcatAuthenticationContainerDiscovery";
    private boolean enabled;
    private boolean noCredentials401;
    private String realm;
    private String role;
    private String rolePrincipalClasses;
    private Configuration configuration;
    private boolean keycloakEnabled;
    
    public AuthenticationConfiguration(final ServletContext servletContext) {
        final ConfigManager config = (ConfigManager)servletContext.getAttribute("ConfigManager");
        String defaultRolePrincipalClasses = "";
        if (System.getProperty("karaf.name") != null) {
            defaultRolePrincipalClasses = "org.apache.karaf.jaas.boot.principal.RolePrincipal,org.apache.karaf.jaas.modules.RolePrincipal,org.apache.karaf.jaas.boot.principal.GroupPrincipal";
        }
        String authDiscoveryClasses = "io.hawt.web.tomcat.TomcatAuthenticationContainerDiscovery";
        if (config != null) {
            this.realm = config.get("realm", "karaf");
            String roles = config.get("role", null);
            if (roles == null) {
                roles = config.get("roles", null);
            }
            if (roles == null) {
                roles = "admin,manager,viewer";
            }
            this.role = roles;
            this.rolePrincipalClasses = config.get("rolePrincipalClasses", defaultRolePrincipalClasses);
            this.enabled = Boolean.parseBoolean(config.get("authenticationEnabled", "true"));
            this.noCredentials401 = Boolean.parseBoolean(config.get("noCredentials401", "false"));
            this.keycloakEnabled = (this.enabled && Boolean.parseBoolean(config.get("keycloakEnabled", "false")));
            authDiscoveryClasses = config.get("authenticationContainerDiscoveryClasses", authDiscoveryClasses);
        }
        if (System.getProperty("hawtio.authenticationEnabled") != null) {
            this.enabled = Boolean.getBoolean("hawtio.authenticationEnabled");
        }
        if (System.getProperty("hawtio.noCredentials401") != null) {
            this.noCredentials401 = Boolean.getBoolean("hawtio.noCredentials401");
        }
        if (System.getProperty("hawtio.realm") != null) {
            this.realm = System.getProperty("hawtio.realm");
        }
        if (System.getProperty("hawtio.role") != null) {
            this.role = System.getProperty("hawtio.role");
        }
        if (System.getProperty("hawtio.roles") != null) {
            this.role = System.getProperty("hawtio.roles");
        }
        if (System.getProperty("hawtio.rolePrincipalClasses") != null) {
            this.rolePrincipalClasses = System.getProperty("hawtio.rolePrincipalClasses");
        }
        if (System.getProperty("hawtio.keycloakEnabled") != null) {
            this.keycloakEnabled = (this.enabled && Boolean.getBoolean("hawtio.keycloakEnabled"));
        }
        if (System.getProperty("hawtio.authenticationContainerDiscoveryClasses") != null) {
            authDiscoveryClasses = System.getProperty("hawtio.authenticationContainerDiscoveryClasses");
        }
        if (this.enabled) {
            final List<AuthenticationContainerDiscovery> discoveries = getDiscoveries(authDiscoveryClasses);
            for (final AuthenticationContainerDiscovery discovery : discoveries) {
                if (discovery.canAuthenticate(this)) {
                    AuthenticationConfiguration.LOG.info("Discovered container {} to use with hawtio authentication filter", (Object)discovery.getContainerName());
                    break;
                }
            }
        }
        if (this.enabled) {
            AuthenticationConfiguration.LOG.info("Starting hawtio authentication filter, JAAS realm: \"{}\" authorized role(s): \"{}\" role principal classes: \"{}\"", new Object[] { this.realm, this.role, this.rolePrincipalClasses });
        }
        else {
            AuthenticationConfiguration.LOG.info("Starting hawtio authentication filter, JAAS authentication disabled");
        }
    }
    
    public static AuthenticationConfiguration getConfiguration(final ServletContext servletContext) {
        AuthenticationConfiguration authConfig = (AuthenticationConfiguration)servletContext.getAttribute("authenticationConfig");
        if (authConfig == null) {
            authConfig = new AuthenticationConfiguration(servletContext);
            servletContext.setAttribute("authenticationEnabled", (Object)authConfig.isEnabled());
            servletContext.setAttribute("authenticationConfig", (Object)authConfig);
        }
        return authConfig;
    }
    
    private static List<AuthenticationContainerDiscovery> getDiscoveries(final String authDiscoveryClasses) {
        final List<AuthenticationContainerDiscovery> discoveries = new ArrayList<AuthenticationContainerDiscovery>();
        if (authDiscoveryClasses == null || authDiscoveryClasses.trim().isEmpty()) {
            return discoveries;
        }
        final String[] split;
        final String[] discoveryClasses = split = authDiscoveryClasses.split(",");
        for (final String discoveryClass : split) {
            try {
                final Class<? extends AuthenticationContainerDiscovery> clazz = (Class<? extends AuthenticationContainerDiscovery>)AuthenticationConfiguration.class.getClassLoader().loadClass(discoveryClass.trim());
                final AuthenticationContainerDiscovery discovery = (AuthenticationContainerDiscovery)clazz.newInstance();
                discoveries.add(discovery);
            }
            catch (Exception e) {
                AuthenticationConfiguration.LOG.warn("Couldn't instantiate discovery " + discoveryClass, (Throwable)e);
            }
        }
        return discoveries;
    }
    
    public boolean isEnabled() {
        return this.enabled;
    }
    
    public boolean isNoCredentials401() {
        return this.noCredentials401;
    }
    
    public String getRealm() {
        return this.realm;
    }
    
    public String getRole() {
        return this.role;
    }
    
    public String getRolePrincipalClasses() {
        return this.rolePrincipalClasses;
    }
    
    public void setRolePrincipalClasses(final String rolePrincipalClasses) {
        this.rolePrincipalClasses = rolePrincipalClasses;
    }
    
    public Configuration getConfiguration() {
        return this.configuration;
    }
    
    public void setConfiguration(final Configuration configuration) {
        this.configuration = configuration;
    }
    
    public boolean isKeycloakEnabled() {
        return this.keycloakEnabled;
    }
    
    @Override
    public String toString() {
        return "AuthenticationConfiguration[enabled=" + this.enabled + ", noCredentials401=" + this.noCredentials401 + ", realm='" + this.realm + '\'' + ", role(s)='" + this.role + '\'' + ", rolePrincipalClasses='" + this.rolePrincipalClasses + '\'' + ", configuration=" + this.configuration + ", keycloakEnabled=" + this.keycloakEnabled + ']';
    }
    
    static {
        LOG = LoggerFactory.getLogger((Class)AuthenticationConfiguration.class);
        UNSECURED_PATHS = new String[] { "/auth", "/css", "/fonts", "/img", "/js", "/hawtconfig.json", "/jolokia", "/keycloak", "/oauth", "/user", "/login.html" };
    }
}
