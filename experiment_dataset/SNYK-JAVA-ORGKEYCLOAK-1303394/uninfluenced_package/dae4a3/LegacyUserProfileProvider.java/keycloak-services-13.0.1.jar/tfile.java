// 
// Decompiled by Procyon v0.5.36
// 

package org.keycloak.userprofile;

import org.keycloak.userprofile.validation.StaticValidators;
import org.keycloak.models.RealmModel;
import java.util.List;
import org.keycloak.userprofile.validation.ValidationChainBuilder;
import org.keycloak.userprofile.validation.UserProfileValidationResult;
import java.util.regex.Pattern;
import org.keycloak.models.KeycloakSession;

public class LegacyUserProfileProvider implements UserProfileProvider
{
    private final KeycloakSession session;
    private final Pattern readOnlyAttributes;
    private final Pattern adminReadOnlyAttributes;
    
    public LegacyUserProfileProvider(final KeycloakSession session, final Pattern readOnlyAttributes, final Pattern adminReadOnlyAttributes) {
        this.session = session;
        this.readOnlyAttributes = readOnlyAttributes;
        this.adminReadOnlyAttributes = adminReadOnlyAttributes;
    }
    
    public void close() {
    }
    
    public UserProfileValidationResult validate(final UserProfileContext updateContext, final UserProfile updatedProfile) {
        final RealmModel realm = this.session.getContext().getRealm();
        final ValidationChainBuilder builder = ValidationChainBuilder.builder();
        switch (updateContext.getUpdateEvent()) {
            case UserResource: {
                this.addReadOnlyAttributeValidators(builder, this.adminReadOnlyAttributes, updateContext, updatedProfile);
                break;
            }
            case IdpReview: {
                this.addBasicValidators(builder, !realm.isRegistrationEmailAsUsername());
                this.addReadOnlyAttributeValidators(builder, this.readOnlyAttributes, updateContext, updatedProfile);
                break;
            }
            case Account:
            case RegistrationProfile:
            case UpdateProfile: {
                this.addBasicValidators(builder, !realm.isRegistrationEmailAsUsername() && realm.isEditUsernameAllowed());
                this.addReadOnlyAttributeValidators(builder, this.readOnlyAttributes, updateContext, updatedProfile);
                this.addSessionValidators(builder);
                break;
            }
            case RegistrationUserCreation: {
                this.addUserCreationValidators(builder);
                this.addReadOnlyAttributeValidators(builder, this.readOnlyAttributes, updateContext, updatedProfile);
                break;
            }
        }
        return new UserProfileValidationResult((List)builder.build().validate(updateContext, updatedProfile), updatedProfile);
    }
    
    public boolean isReadOnlyAttribute(final String key) {
        return this.readOnlyAttributes.matcher(key).find() || this.adminReadOnlyAttributes.matcher(key).find();
    }
    
    private void addUserCreationValidators(final ValidationChainBuilder builder) {
        final RealmModel realm = this.session.getContext().getRealm();
        if (realm.isRegistrationEmailAsUsername()) {
            builder.addAttributeValidator().forAttribute("email").addSingleAttributeValueValidationFunction("invalidEmailMessage", StaticValidators.isEmailValid()).addSingleAttributeValueValidationFunction("missingEmailMessage", StaticValidators.isBlank()).addSingleAttributeValueValidationFunction("emailExistsMessage", StaticValidators.doesEmailExist(this.session)).build().build();
        }
        else {
            builder.addAttributeValidator().forAttribute("username").addSingleAttributeValueValidationFunction("missingUsernameMessage", StaticValidators.isBlank()).addSingleAttributeValueValidationFunction("usernameExistsMessage", (value, o) -> this.session.users().getUserByUsername(realm, value) == null).build();
        }
    }
    
    private void addBasicValidators(final ValidationChainBuilder builder, final boolean userNameExistsCondition) {
        builder.addAttributeValidator().forAttribute("username").addSingleAttributeValueValidationFunction("missingUsernameMessage", StaticValidators.checkUsernameExists(userNameExistsCondition)).build().addAttributeValidator().forAttribute("firstName").addSingleAttributeValueValidationFunction("missingFirstNameMessage", StaticValidators.isBlank()).build().addAttributeValidator().forAttribute("lastName").addSingleAttributeValueValidationFunction("missingLastNameMessage", StaticValidators.isBlank()).build().addAttributeValidator().forAttribute("email").addSingleAttributeValueValidationFunction("missingEmailMessage", StaticValidators.isBlank()).addSingleAttributeValueValidationFunction("invalidEmailMessage", StaticValidators.isEmailValid()).build();
    }
    
    private void addSessionValidators(final ValidationChainBuilder builder) {
        final RealmModel realm = this.session.getContext().getRealm();
        builder.addAttributeValidator().forAttribute("username").addSingleAttributeValueValidationFunction("usernameExistsMessage", StaticValidators.userNameExists(this.session)).addSingleAttributeValueValidationFunction("readOnlyUsernameMessage", StaticValidators.isUserMutable(realm)).build().addAttributeValidator().forAttribute("email").addSingleAttributeValueValidationFunction("emailExistsMessage", StaticValidators.isEmailDuplicated(this.session)).addSingleAttributeValueValidationFunction("usernameExistsMessage", StaticValidators.doesEmailExistAsUsername(this.session)).build().build();
    }
    
    private void addReadOnlyAttributeValidators(final ValidationChainBuilder builder, final Pattern configuredReadOnlyAttrs, final UserProfileContext updateContext, final UserProfile updatedProfile) {
        this.addValidatorsForReadOnlyAttributes(builder, configuredReadOnlyAttrs, updatedProfile);
        this.addValidatorsForReadOnlyAttributes(builder, configuredReadOnlyAttrs, updateContext.getCurrentProfile());
    }
    
    private void addValidatorsForReadOnlyAttributes(final ValidationChainBuilder builder, final Pattern configuredReadOnlyAttrsPattern, final UserProfile profile) {
        if (profile == null) {
            return;
        }
        profile.getAttributes().keySet().stream().filter(currentAttrName -> configuredReadOnlyAttrsPattern.matcher(currentAttrName).find()).forEach(currentAttrName -> builder.addAttributeValidator().forAttribute(currentAttrName).addValidationFunction("updateReadOnlyAttributesRejectedMessage", StaticValidators.isReadOnlyAttributeUnchanged(currentAttrName)).build());
    }
}
