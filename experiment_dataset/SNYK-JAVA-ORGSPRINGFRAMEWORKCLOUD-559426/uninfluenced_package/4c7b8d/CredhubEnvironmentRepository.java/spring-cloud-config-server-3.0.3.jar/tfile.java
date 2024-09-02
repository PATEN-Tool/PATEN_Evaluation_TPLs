// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.cloud.config.server.environment;

import org.springframework.credhub.support.CredentialSummary;
import java.util.stream.Stream;
import org.springframework.credhub.core.credential.CredHubCredentialOperations;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.function.Function;
import org.springframework.credhub.support.CredentialDetails;
import org.springframework.credhub.support.CredentialName;
import org.springframework.credhub.support.json.JsonCredential;
import org.springframework.credhub.support.SimpleCredentialName;
import java.util.Arrays;
import java.util.Map;
import org.springframework.cloud.config.environment.PropertySource;
import org.springframework.util.StringUtils;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.credhub.core.CredHubOperations;

public class CredhubEnvironmentRepository implements EnvironmentRepository
{
    private CredHubOperations credHubOperations;
    private static final String DEFAULT_PROFILE = "default";
    private static final String DEFAULT_LABEL = "master";
    private static final String DEFAULT_APPLICATION = "application";
    
    public CredhubEnvironmentRepository(final CredHubOperations credHubOperations) {
        this.credHubOperations = credHubOperations;
    }
    
    @Override
    public Environment findOne(final String application, String profilesList, String label) {
        if (StringUtils.isEmpty((Object)profilesList)) {
            profilesList = "default";
        }
        if (StringUtils.isEmpty((Object)label)) {
            label = "master";
        }
        final String[] profiles = StringUtils.commaDelimitedListToStringArray(profilesList);
        final Environment environment = new Environment(application, profiles, label, (String)null, (String)null);
        for (final String profile : profiles) {
            environment.add(new PropertySource("credhub-" + application + "-" + profile + "-" + label, (Map)this.findProperties(application, profile, label)));
            if (!"application".equals(application)) {
                this.addDefaultPropertySource(environment, "application", profile, label);
            }
        }
        if (!Arrays.asList(profiles).contains("default")) {
            this.addDefaultPropertySource(environment, application, "default", label);
        }
        if (!Arrays.asList(profiles).contains("default") && !"application".equals(application)) {
            this.addDefaultPropertySource(environment, "application", "default", label);
        }
        return environment;
    }
    
    private void addDefaultPropertySource(final Environment environment, final String application, final String profile, final String label) {
        final Map<Object, Object> properties = this.findProperties(application, profile, label);
        if (!properties.isEmpty()) {
            final PropertySource propertySource = new PropertySource("credhub-" + application + "-" + profile + "-" + label, (Map)properties);
            environment.add(propertySource);
        }
    }
    
    private Map<Object, Object> findProperties(final String application, final String profile, final String label) {
        final String path = "/" + application + "/" + profile + "/" + label;
        final SimpleCredentialName simpleCredentialName;
        final CredHubCredentialOperations credHubCredentialOperations;
        return (Map<Object, Object>)this.credHubOperations.credentials().findByPath(path).stream().map(credentialSummary -> credentialSummary.getName().getName()).map(name -> {
            this.credHubOperations.credentials();
            new SimpleCredentialName(new String[] { name });
            return credHubCredentialOperations.getByName((CredentialName)simpleCredentialName, (Class)JsonCredential.class);
        }).map(CredentialDetails::getValue).flatMap(jsonCredential -> jsonCredential.entrySet().stream()).collect(Collectors.toMap((Function<? super Object, ?>)Map.Entry::getKey, (Function<? super Object, ?>)Map.Entry::getValue, (a, b) -> b));
    }
}
