// 
// Decompiled by Procyon v0.5.36
// 

package org.keycloak.userprofile.utils;

import org.keycloak.models.utils.KeycloakModelUtils;
import java.util.Set;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Collection;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.keycloak.userprofile.UserProfileAttributes;
import org.keycloak.userprofile.validation.UserUpdateEvent;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.models.UserModel;
import org.keycloak.models.RealmModel;

public class UserUpdateHelper
{
    public static void updateRegistrationProfile(final RealmModel realm, final UserModel currentUser, final UserProfile updatedUser) {
        register(UserUpdateEvent.RegistrationProfile, realm, currentUser, updatedUser);
    }
    
    public static void updateRegistrationUserCreation(final RealmModel realm, final UserModel currentUser, final UserProfile updatedUser) {
        register(UserUpdateEvent.RegistrationUserCreation, realm, currentUser, updatedUser);
    }
    
    public static void updateIdpReview(final RealmModel realm, final UserModel userModelDelegate, final UserProfile updatedProfile) {
        update(UserUpdateEvent.IdpReview, realm, userModelDelegate, updatedProfile);
    }
    
    public static void updateUserProfile(final RealmModel realm, final UserModel user, final UserProfile updatedProfile) {
        update(UserUpdateEvent.UpdateProfile, realm, user, updatedProfile);
    }
    
    public static void updateAccount(final RealmModel realm, final UserModel user, final UserProfile updatedProfile) {
        update(UserUpdateEvent.Account, realm, user, updatedProfile);
    }
    
    @Deprecated
    public static void updateAccountOldConsole(final RealmModel realm, final UserModel user, final UserProfile updatedProfile) {
        update(UserUpdateEvent.Account, realm, user, updatedProfile.getAttributes(), false);
    }
    
    public static void updateUserResource(final RealmModel realm, final UserModel user, final UserProfile userRepresentationUserProfile, final boolean removeExistingAttributes) {
        update(UserUpdateEvent.UserResource, realm, user, userRepresentationUserProfile.getAttributes(), removeExistingAttributes);
    }
    
    private static void update(final UserUpdateEvent userUpdateEvent, final RealmModel realm, final UserModel currentUser, final UserProfile updatedUser) {
        update(userUpdateEvent, realm, currentUser, updatedUser.getAttributes(), true);
    }
    
    private static void register(final UserUpdateEvent userUpdateEvent, final RealmModel realm, final UserModel currentUser, final UserProfile updatedUser) {
        update(userUpdateEvent, realm, currentUser, updatedUser.getAttributes(), false);
    }
    
    private static void update(final UserUpdateEvent userUpdateEvent, final RealmModel realm, final UserModel currentUser, final UserProfileAttributes updatedUser, final boolean removeMissingAttributes) {
        if (updatedUser == null || updatedUser.size() == 0) {
            return;
        }
        filterAttributes(userUpdateEvent, realm, updatedUser);
        updateAttributes(currentUser, (Map<String, List<String>>)updatedUser, removeMissingAttributes);
    }
    
    private static void filterAttributes(final UserUpdateEvent userUpdateEvent, final RealmModel realm, final UserProfileAttributes updatedUser) {
        if (!userUpdateEvent.equals((Object)UserUpdateEvent.IdpReview) && updatedUser.getFirstAttribute("username") != null && !realm.isEditUsernameAllowed()) {
            updatedUser.removeAttribute("username");
        }
        if (updatedUser.getFirstAttribute("email") != null && updatedUser.getFirstAttribute("email").isEmpty()) {
            updatedUser.removeAttribute("email");
            updatedUser.setAttribute("email", (List)Collections.singletonList((Object)null));
        }
        if (updatedUser.getFirstAttribute("email") != null && realm.isRegistrationEmailAsUsername()) {
            updatedUser.removeAttribute("username");
            updatedUser.setAttribute("username", (List)Collections.singletonList(updatedUser.getFirstAttribute("email")));
        }
    }
    
    private static void updateAttributes(final UserModel currentUser, final Map<String, List<String>> updatedUser, final boolean removeMissingAttributes) {
        for (final Map.Entry<String, List<String>> attr : updatedUser.entrySet()) {
            final List<String> currentValue = currentUser.getAttributeStream((String)attr.getKey()).collect(Collectors.toList());
            final List<String> updatedValue = attr.getKey().equals("username") ? AttributeToLower(attr.getValue()) : attr.getValue();
            if (currentValue.size() != updatedValue.size() || !currentValue.containsAll(updatedValue)) {
                currentUser.setAttribute((String)attr.getKey(), (List)updatedValue);
            }
        }
        if (removeMissingAttributes) {
            final Set<String> attrsToRemove = new HashSet<String>(currentUser.getAttributes().keySet());
            attrsToRemove.removeAll(updatedUser.keySet());
            for (final String attr2 : attrsToRemove) {
                currentUser.removeAttribute(attr2);
            }
        }
    }
    
    private static List<String> AttributeToLower(final List<String> attr) {
        if (attr.size() == 1 && attr.get(0) != null) {
            return Collections.singletonList(KeycloakModelUtils.toLowerCaseSafe((String)attr.get(0)));
        }
        return attr;
    }
}
