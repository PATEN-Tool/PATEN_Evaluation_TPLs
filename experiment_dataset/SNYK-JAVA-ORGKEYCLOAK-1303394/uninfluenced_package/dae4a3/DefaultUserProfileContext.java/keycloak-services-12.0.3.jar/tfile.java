// 
// Decompiled by Procyon v0.5.36
// 

package org.keycloak.userprofile.profile;

import org.keycloak.userprofile.profile.representations.UserModelUserProfile;
import org.keycloak.models.UserModel;
import org.keycloak.userprofile.profile.representations.IdpUserProfile;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.userprofile.validation.UserUpdateEvent;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileContext;

public class DefaultUserProfileContext implements UserProfileContext
{
    private UserProfile currentUserProfile;
    private UserUpdateEvent userUpdateEvent;
    
    private DefaultUserProfileContext(final UserUpdateEvent userUpdateEvent, final UserProfile currentUserProfile) {
        this.userUpdateEvent = userUpdateEvent;
        this.currentUserProfile = currentUserProfile;
    }
    
    public static DefaultUserProfileContext forIdpReview(final SerializedBrokeredIdentityContext currentUser) {
        return new DefaultUserProfileContext(UserUpdateEvent.IdpReview, (UserProfile)new IdpUserProfile(currentUser));
    }
    
    public static DefaultUserProfileContext forUpdateProfile(final UserModel currentUser) {
        return new DefaultUserProfileContext(UserUpdateEvent.UpdateProfile, (UserProfile)new UserModelUserProfile(currentUser));
    }
    
    public static DefaultUserProfileContext forAccountService(final UserModel currentUser) {
        return new DefaultUserProfileContext(UserUpdateEvent.Account, (UserProfile)new UserModelUserProfile(currentUser));
    }
    
    public static DefaultUserProfileContext forRegistrationUserCreation() {
        return new DefaultUserProfileContext(UserUpdateEvent.RegistrationUserCreation, null);
    }
    
    public static DefaultUserProfileContext forRegistrationProfile() {
        return new DefaultUserProfileContext(UserUpdateEvent.RegistrationProfile, null);
    }
    
    public static DefaultUserProfileContext forUserResource(final UserModel currentUser) {
        final UserProfile currentUserProfile = (UserProfile)((currentUser == null) ? null : new UserModelUserProfile(currentUser));
        return new DefaultUserProfileContext(UserUpdateEvent.UserResource, currentUserProfile);
    }
    
    public UserProfile getCurrentProfile() {
        return this.currentUserProfile;
    }
    
    public UserUpdateEvent getUpdateEvent() {
        return this.userUpdateEvent;
    }
}
