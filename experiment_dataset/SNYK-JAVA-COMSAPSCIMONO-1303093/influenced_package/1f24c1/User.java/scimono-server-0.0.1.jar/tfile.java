// 
// Decompiled by Procyon v0.5.36
// 

package com.sap.scimono.entity;

import com.sap.scimono.entity.base.Extension;
import com.sap.scimono.entity.base.MultiValuedAttribute;
import java.util.Collection;
import com.sap.scimono.exception.InvalidInputException;
import java.util.ArrayList;
import com.sap.scimono.entity.schema.validation.ValidCoreSchema;
import java.util.Map;
import com.sap.scimono.helper.Strings;
import java.util.LinkedHashMap;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Iterator;
import java.util.Optional;
import java.util.Collections;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sap.scimono.helper.Objects;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.Valid;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class User extends Resource<User>
{
    public static final String RESOURCE_TYPE_USER = "User";
    public static final String SCHEMA = "urn:ietf:params:scim:schemas:core:2.0:User";
    private static final long serialVersionUID = -4076516708797425414L;
    private final String userName;
    private final Name name;
    private final String displayName;
    private final String nickName;
    private final String profileUrl;
    private final String title;
    private final String userType;
    private final String preferredLanguage;
    private final String locale;
    private final String timezone;
    private final Boolean active;
    private final String password;
    @Valid
    private final List<Email> emails;
    @Valid
    private final List<PhoneNumber> phoneNumbers;
    @Valid
    private final List<Im> ims;
    @Valid
    private final List<Photo> photos;
    private final List<Address> addresses;
    @Valid
    private final List<GroupRef> groups;
    @Valid
    private final List<Entitlement> entitlements;
    @Valid
    private final List<Role> roles;
    @Valid
    private final List<X509Certificate> x509Certificates;
    
    @JsonCreator
    private User(@JsonProperty("id") final String id, @JsonProperty("externalId") final String externalId, @JsonProperty("meta") final Meta meta, @JsonProperty(value = "schemas", required = true) final Set<String> schemas, @JsonProperty("userName") final String userName, @JsonProperty("name") final Name name, @JsonProperty("displayName") final String displayName, @JsonProperty("nickName") final String nickName, @JsonProperty("profileUrl") final String profileUrl, @JsonProperty("title") final String title, @JsonProperty("userType") final String userType, @JsonProperty("preferredLanguage") final String preferredLanguage, @JsonProperty("locale") final String locale, @JsonProperty("timezone") final String timezone, @JsonProperty("active") final Boolean active, @JsonProperty("emails") final List<Email> emails, @JsonProperty("phoneNumbers") final List<PhoneNumber> phoneNumbers, @JsonProperty("ims") final List<Im> ims, @JsonProperty("photos") final List<Photo> photos, @JsonProperty("addresses") final List<Address> addresses, @JsonProperty("groups") final List<GroupRef> groups, @JsonProperty("entitlements") final List<Entitlement> entitlements, @JsonProperty("roles") final List<Role> roles, @JsonProperty("x509Certificates") final List<X509Certificate> x509Certificates) {
        super(id, externalId, meta, schemas);
        this.userName = ((userName != null) ? userName : "");
        this.name = name;
        this.displayName = displayName;
        this.nickName = nickName;
        this.profileUrl = profileUrl;
        this.title = title;
        this.userType = userType;
        this.preferredLanguage = preferredLanguage;
        this.locale = locale;
        this.timezone = timezone;
        this.active = active;
        this.emails = Objects.sameOrEmpty(emails);
        this.phoneNumbers = Objects.sameOrEmpty(phoneNumbers);
        this.ims = Objects.sameOrEmpty(ims);
        this.photos = Objects.sameOrEmpty(photos);
        this.addresses = Objects.sameOrEmpty(addresses);
        this.groups = Objects.sameOrEmpty(groups);
        this.entitlements = Objects.sameOrEmpty(entitlements);
        this.roles = Objects.sameOrEmpty(roles);
        this.x509Certificates = Objects.sameOrEmpty(x509Certificates);
        this.password = null;
    }
    
    private User(final Builder builder) {
        super(builder);
        this.userName = builder.userName;
        this.name = builder.name;
        this.displayName = builder.displayName;
        this.nickName = builder.nickName;
        this.profileUrl = builder.profileUrl;
        this.title = builder.title;
        this.userType = builder.userType;
        this.preferredLanguage = builder.preferredLanguage;
        this.locale = builder.locale;
        this.timezone = builder.timezone;
        this.active = builder.active;
        this.password = builder.password;
        this.emails = builder.emails;
        this.phoneNumbers = builder.phoneNumbers;
        this.ims = builder.ims;
        this.photos = builder.photos;
        this.addresses = builder.addresses;
        this.groups = builder.groups;
        this.entitlements = builder.entitlements;
        this.roles = builder.roles;
        this.x509Certificates = builder.x509Certificates;
    }
    
    public String getUserName() {
        return this.userName;
    }
    
    public Name getName() {
        return this.name;
    }
    
    public String getDisplayName() {
        return this.displayName;
    }
    
    public String getNickName() {
        return this.nickName;
    }
    
    public String getProfileUrl() {
        return this.profileUrl;
    }
    
    public String getTitle() {
        return this.title;
    }
    
    public String getUserType() {
        return this.userType;
    }
    
    public String getPreferredLanguage() {
        return this.preferredLanguage;
    }
    
    public String getLocale() {
        return this.locale;
    }
    
    public String getTimezone() {
        return this.timezone;
    }
    
    public Boolean isActive() {
        return this.active;
    }
    
    public String getPassword() {
        return this.password;
    }
    
    public List<Email> getEmails() {
        return Collections.unmodifiableList((List<? extends Email>)this.emails);
    }
    
    @JsonIgnore
    public Optional<Email> getPrimaryOrFirstEmail() {
        for (final Email email : this.emails) {
            if (email.isPrimary()) {
                return Optional.of(email);
            }
        }
        if (!this.emails.isEmpty()) {
            return Optional.of(this.emails.get(0));
        }
        return Optional.empty();
    }
    
    public List<PhoneNumber> getPhoneNumbers() {
        return Collections.unmodifiableList((List<? extends PhoneNumber>)this.phoneNumbers);
    }
    
    public List<Im> getIms() {
        return Collections.unmodifiableList((List<? extends Im>)this.ims);
    }
    
    public List<Photo> getPhotos() {
        return Collections.unmodifiableList((List<? extends Photo>)this.photos);
    }
    
    public List<Address> getAddresses() {
        return Collections.unmodifiableList((List<? extends Address>)this.addresses);
    }
    
    public List<GroupRef> getGroups() {
        return Collections.unmodifiableList((List<? extends GroupRef>)this.groups);
    }
    
    public List<Entitlement> getEntitlements() {
        return Collections.unmodifiableList((List<? extends Entitlement>)this.entitlements);
    }
    
    public List<Role> getRoles() {
        return Collections.unmodifiableList((List<? extends Role>)this.roles);
    }
    
    public List<X509Certificate> getX509Certificates() {
        return Collections.unmodifiableList((List<? extends X509Certificate>)this.x509Certificates);
    }
    
    @Override
    public String toString() {
        final Map<String, Object> valuesToDisplay = new LinkedHashMap<String, Object>();
        valuesToDisplay.put("userName", this.userName);
        valuesToDisplay.put("name", this.name);
        valuesToDisplay.put("displayName", this.displayName);
        valuesToDisplay.put("nickName", this.nickName);
        valuesToDisplay.put("profileUrl", this.profileUrl);
        valuesToDisplay.put("title", this.title);
        valuesToDisplay.put("userType", this.userType);
        valuesToDisplay.put("preferredLanguage", this.preferredLanguage);
        valuesToDisplay.put("locale", this.locale);
        valuesToDisplay.put("timezone", this.timezone);
        valuesToDisplay.put("active", this.active);
        valuesToDisplay.put("password", this.password);
        valuesToDisplay.put("emails", this.emails);
        valuesToDisplay.put("phoneNumbers", this.phoneNumbers);
        valuesToDisplay.put("ims", this.ims);
        valuesToDisplay.put("photos", this.photos);
        valuesToDisplay.put("addresses", this.addresses);
        valuesToDisplay.put("groups", this.groups);
        valuesToDisplay.put("entitlements", this.entitlements);
        valuesToDisplay.put("roles", this.roles);
        valuesToDisplay.put("x509Certificates", this.x509Certificates);
        valuesToDisplay.put("id", this.getId());
        valuesToDisplay.put("externalId", this.getExternalId());
        valuesToDisplay.put("meta", this.getMeta());
        valuesToDisplay.put("schemas", this.getSchemas());
        valuesToDisplay.put("extensions", this.getExtensions());
        return Strings.createPrettyEntityString(valuesToDisplay, this.getClass());
    }
    
    @ValidCoreSchema("urn:ietf:params:scim:schemas:core:2.0:User")
    @Override
    public Set<String> getSchemas() {
        return super.getSchemas();
    }
    
    @Override
    public Builder builder() {
        return new Builder(this);
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = 31 * result + java.util.Objects.hash(this.active);
        result = 31 * result + java.util.Objects.hash(this.addresses);
        result = 31 * result + java.util.Objects.hash(this.displayName);
        result = 31 * result + java.util.Objects.hash(this.emails);
        result = 31 * result + java.util.Objects.hash(this.entitlements);
        result = 31 * result + java.util.Objects.hash(this.groups);
        result = 31 * result + java.util.Objects.hash(this.ims);
        result = 31 * result + java.util.Objects.hash(this.locale);
        result = 31 * result + java.util.Objects.hash(this.name);
        result = 31 * result + java.util.Objects.hash(this.nickName);
        result = 31 * result + java.util.Objects.hash(this.password);
        result = 31 * result + java.util.Objects.hash(this.phoneNumbers);
        result = 31 * result + java.util.Objects.hash(this.photos);
        result = 31 * result + java.util.Objects.hash(this.preferredLanguage);
        result = 31 * result + java.util.Objects.hash(this.profileUrl);
        result = 31 * result + java.util.Objects.hash(this.roles);
        result = 31 * result + java.util.Objects.hash(this.timezone);
        result = 31 * result + java.util.Objects.hash(this.title);
        result = 31 * result + java.util.Objects.hash(this.userName);
        result = 31 * result + java.util.Objects.hash(this.userType);
        result = 31 * result + java.util.Objects.hash(this.x509Certificates);
        return result;
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof User)) {
            return false;
        }
        final User other = (User)obj;
        return java.util.Objects.equals(this.active, other.active) && java.util.Objects.equals(this.addresses, other.addresses) && java.util.Objects.equals(this.displayName, other.displayName) && java.util.Objects.equals(this.emails, other.emails) && java.util.Objects.equals(this.entitlements, other.entitlements) && java.util.Objects.equals(this.groups, other.groups) && java.util.Objects.equals(this.ims, other.ims) && java.util.Objects.equals(this.locale, other.locale) && java.util.Objects.equals(this.name, other.name) && java.util.Objects.equals(this.nickName, other.nickName) && java.util.Objects.equals(this.password, other.password) && java.util.Objects.equals(this.phoneNumbers, other.phoneNumbers) && java.util.Objects.equals(this.photos, other.photos) && java.util.Objects.equals(this.preferredLanguage, other.preferredLanguage) && java.util.Objects.equals(this.profileUrl, other.profileUrl) && java.util.Objects.equals(this.roles, other.roles) && java.util.Objects.equals(this.timezone, other.timezone) && java.util.Objects.equals(this.title, other.title) && java.util.Objects.equals(this.userName, other.userName) && java.util.Objects.equals(this.userType, other.userType) && java.util.Objects.equals(this.x509Certificates, other.x509Certificates);
    }
    
    public static final class Builder extends Resource.Builder<User>
    {
        private String userName;
        private String password;
        private Boolean active;
        private String timezone;
        private String locale;
        private String preferredLanguage;
        private String userType;
        private String title;
        private String profileUrl;
        private String nickName;
        private String displayName;
        private Name name;
        private List<Email> emails;
        private List<PhoneNumber> phoneNumbers;
        private List<Im> ims;
        private List<Photo> photos;
        private List<Address> addresses;
        private List<GroupRef> groups;
        private List<Entitlement> entitlements;
        private List<Role> roles;
        private List<X509Certificate> x509Certificates;
        
        Builder(final String userName, final User user) {
            super(user);
            this.emails = new ArrayList<Email>();
            this.phoneNumbers = new ArrayList<PhoneNumber>();
            this.ims = new ArrayList<Im>();
            this.photos = new ArrayList<Photo>();
            this.addresses = new ArrayList<Address>();
            this.groups = new ArrayList<GroupRef>();
            this.entitlements = new ArrayList<Entitlement>();
            this.roles = new ArrayList<Role>();
            this.x509Certificates = new ArrayList<X509Certificate>();
            this.addSchema("urn:ietf:params:scim:schemas:core:2.0:User");
            if (user != null) {
                this.userName = user.userName;
                this.name = user.name;
                this.displayName = user.displayName;
                this.nickName = user.nickName;
                this.profileUrl = user.profileUrl;
                this.title = user.title;
                this.userType = user.userType;
                this.preferredLanguage = user.preferredLanguage;
                this.locale = user.locale;
                this.timezone = user.timezone;
                this.active = user.active;
                this.password = user.password;
                this.emails = Objects.firstNonNull(user.emails, this.emails);
                this.phoneNumbers = Objects.firstNonNull(user.phoneNumbers, this.phoneNumbers);
                this.ims = Objects.firstNonNull(user.ims, this.ims);
                this.photos = Objects.firstNonNull(user.photos, this.photos);
                this.addresses = Objects.firstNonNull(user.addresses, this.addresses);
                this.groups = Objects.firstNonNull(user.groups, this.groups);
                this.entitlements = Objects.firstNonNull(user.entitlements, this.entitlements);
                this.roles = Objects.firstNonNull(user.roles, this.roles);
                this.x509Certificates = Objects.firstNonNull(user.x509Certificates, this.x509Certificates);
            }
            if (!Strings.isNullOrEmpty(userName)) {
                this.userName = userName;
            }
        }
        
        public Builder(final String userName) {
            this(userName, null);
            if (Strings.isNullOrEmpty(userName)) {
                throw new InvalidInputException("userName must not be null or empty.");
            }
        }
        
        public Builder() {
            this(null, null);
        }
        
        public Builder(final User user) {
            this(null, user);
            if (user == null) {
                throw new InvalidInputException("The given user must not be null");
            }
        }
        
        public Builder setUserName(final String userName) {
            this.userName = userName;
            return this;
        }
        
        public Builder setName(final Name name) {
            if (name != null && !name.isEmpty()) {
                this.name = name;
            }
            else {
                this.name = null;
            }
            return this;
        }
        
        public Builder setDisplayName(final String displayName) {
            this.displayName = displayName;
            return this;
        }
        
        public Builder setNickName(final String nickName) {
            this.nickName = nickName;
            return this;
        }
        
        public Builder setProfileUrl(final String profileUrl) {
            this.profileUrl = profileUrl;
            return this;
        }
        
        public Builder setTitle(final String title) {
            this.title = title;
            return this;
        }
        
        public Builder setUserType(final String userType) {
            this.userType = userType;
            return this;
        }
        
        public Builder setPreferredLanguage(final String preferredLanguage) {
            this.preferredLanguage = preferredLanguage;
            return this;
        }
        
        public Builder setLocale(final String locale) {
            this.locale = locale;
            return this;
        }
        
        public Builder setTimezone(final String timezone) {
            this.timezone = timezone;
            return this;
        }
        
        public Builder setActive(final boolean active) {
            this.active = active;
            return this;
        }
        
        public Builder setPassword(final String password) {
            this.password = password;
            return this;
        }
        
        public Builder addEmails(final Collection<Email> emails) {
            if (emails != null) {
                for (final Email email : emails) {
                    this.addEmail(email);
                }
            }
            return this;
        }
        
        public Builder addEmail(final Email email) {
            if (email == null || this.isMultivaluedAttributeExistInCollection(email, this.emails)) {
                return this;
            }
            if (email.isPrimary() && MultiValuedAttribute.isCollectionContainsPrimaryAttributes(this.emails)) {
                this.emails.replaceAll(e -> new Email.Builder(e).setPrimary(false).build());
            }
            this.emails.add(new Email.Builder(email).build());
            return this;
        }
        
        public Builder removeEmails() {
            this.emails.clear();
            return this;
        }
        
        public Builder removeEmail(final Email email) {
            this.emails.remove(email);
            return this;
        }
        
        public Builder addPhoneNumbers(final Collection<PhoneNumber> phoneNumbers) {
            if (phoneNumbers != null) {
                for (final PhoneNumber phoneNumber : phoneNumbers) {
                    this.addPhoneNumber(phoneNumber);
                }
            }
            return this;
        }
        
        public Builder addPhoneNumber(final PhoneNumber phoneNumber) {
            if (phoneNumber == null || this.isMultivaluedAttributeExistInCollection(phoneNumber, this.phoneNumbers)) {
                return this;
            }
            if (phoneNumber.isPrimary() && MultiValuedAttribute.isCollectionContainsPrimaryAttributes(this.phoneNumbers)) {
                this.phoneNumbers.replaceAll(ph -> new PhoneNumber.Builder(ph).setPrimary(false).build());
            }
            this.phoneNumbers.add(new PhoneNumber.Builder(phoneNumber).build());
            return this;
        }
        
        public Builder removePhoneNumbers() {
            this.phoneNumbers.clear();
            return this;
        }
        
        public Builder removePhoneNumber(final PhoneNumber phoneNumber) {
            this.phoneNumbers.remove(phoneNumber);
            return this;
        }
        
        public Builder addIms(final Collection<Im> ims) {
            if (ims != null) {
                for (final Im im : ims) {
                    this.addIm(im);
                }
            }
            return this;
        }
        
        public Builder addIm(final Im im) {
            if (im == null || this.isMultivaluedAttributeExistInCollection(im, this.ims)) {
                return this;
            }
            if (im.isPrimary() && MultiValuedAttribute.isCollectionContainsPrimaryAttributes(this.ims)) {
                this.ims.replaceAll(i -> new Im.Builder(i).setPrimary(false).build());
            }
            this.ims.add(new Im.Builder(im).build());
            return this;
        }
        
        public Builder removeIms() {
            this.ims.clear();
            return this;
        }
        
        public Builder removeIm(final Im im) {
            this.ims.remove(im);
            return this;
        }
        
        public Builder addPhotos(final Collection<Photo> photos) {
            if (photos != null) {
                for (final Photo photo : photos) {
                    this.addPhoto(photo);
                }
            }
            return this;
        }
        
        public Builder addPhoto(final Photo photo) {
            if (photo == null || this.isMultivaluedAttributeExistInCollection(photo, this.photos)) {
                return this;
            }
            if (photo.isPrimary() && MultiValuedAttribute.isCollectionContainsPrimaryAttributes(this.photos)) {
                this.photos.replaceAll(ph -> new Photo.Builder(ph).setPrimary(false).build());
            }
            this.photos.add(new Photo.Builder(photo).build());
            return this;
        }
        
        public Builder removePhotos() {
            this.photos.clear();
            return this;
        }
        
        public Builder removePhoto(final Photo photo) {
            this.photos.remove(photo);
            return this;
        }
        
        public Builder addAddresses(final Collection<Address> addresses) {
            if (addresses != null) {
                for (final Address address : addresses) {
                    this.addAddress(address);
                }
            }
            return this;
        }
        
        public Builder addAddress(final Address address) {
            if (address != null) {
                this.addresses.add(new Address.Builder(address).build());
            }
            return this;
        }
        
        public Builder removeAddresses() {
            this.addresses.clear();
            return this;
        }
        
        public Builder removeAddress(final Address address) {
            this.addresses.remove(address);
            return this;
        }
        
        public Builder addGroups(final List<GroupRef> groups) {
            this.groups.addAll(groups);
            return this;
        }
        
        public Builder addGroup(final GroupRef group) {
            this.groups.add(group);
            return this;
        }
        
        public Builder removeGroup(final GroupRef group) {
            this.groups.remove(group);
            return this;
        }
        
        public Builder removeGroups() {
            this.groups.clear();
            return this;
        }
        
        public Builder addEntitlements(final Collection<Entitlement> entitlements) {
            if (entitlements != null) {
                for (final Entitlement entitlement : entitlements) {
                    this.addEntitlement(entitlement);
                }
            }
            return this;
        }
        
        public Builder addEntitlement(final Entitlement entitlement) {
            if (entitlement == null || this.isMultivaluedAttributeExistInCollection(entitlement, this.entitlements)) {
                return this;
            }
            if (entitlement.isPrimary() && MultiValuedAttribute.isCollectionContainsPrimaryAttributes(this.entitlements)) {
                this.entitlements.replaceAll(ent -> new Entitlement.Builder(ent).setPrimary(false).build());
            }
            this.entitlements.add(new Entitlement.Builder(entitlement).build());
            return this;
        }
        
        public Builder removeEntitlements() {
            this.entitlements.clear();
            return this;
        }
        
        public Builder removeEntitlement(final Entitlement entitlement) {
            this.entitlements.remove(entitlement);
            return this;
        }
        
        public Builder addRoles(final Collection<Role> roles) {
            if (roles != null) {
                for (final Role role : roles) {
                    this.addRole(role);
                }
            }
            return this;
        }
        
        public Builder addRole(final Role role) {
            if (role == null || this.isMultivaluedAttributeExistInCollection(role, this.roles)) {
                return this;
            }
            if (role.isPrimary() && MultiValuedAttribute.isCollectionContainsPrimaryAttributes(this.roles)) {
                this.roles.replaceAll(r -> new Role.Builder(r).setPrimary(false).build());
            }
            this.roles.add(new Role.Builder(role).build());
            return this;
        }
        
        public Builder removeRoles() {
            this.roles.clear();
            return this;
        }
        
        public Builder removeRole(final Role role) {
            this.roles.remove(role);
            return this;
        }
        
        public Builder addX509Certificates(final Collection<X509Certificate> x509Certificates) {
            if (x509Certificates != null) {
                for (final X509Certificate x509Certificate : x509Certificates) {
                    this.addX509Certificate(x509Certificate);
                }
            }
            return this;
        }
        
        public Builder addX509Certificate(final X509Certificate x509Certificate) {
            if (x509Certificate == null || this.isMultivaluedAttributeExistInCollection(x509Certificate, this.x509Certificates)) {
                return this;
            }
            if (x509Certificate.isPrimary() && MultiValuedAttribute.isCollectionContainsPrimaryAttributes(this.x509Certificates)) {
                this.x509Certificates.replaceAll(current -> new X509Certificate.Builder(current).setPrimary(false).build());
            }
            this.x509Certificates.add(new X509Certificate.Builder(x509Certificate).build());
            return this;
        }
        
        public Builder removeX509Certificates() {
            this.x509Certificates.clear();
            return this;
        }
        
        public Builder removeX509Certificate(final X509Certificate x509Certificate) {
            this.x509Certificates.remove(x509Certificate);
            return this;
        }
        
        public Builder setGroups(final List<GroupRef> groups) {
            this.groups = groups;
            return this;
        }
        
        @Override
        public Builder setMeta(final Meta meta) {
            super.setMeta(meta);
            return this;
        }
        
        @Override
        public Builder setExternalId(final String externalId) {
            super.setExternalId(externalId);
            return this;
        }
        
        @Override
        public Builder setId(final String id) {
            super.setId(id);
            return this;
        }
        
        @Override
        protected void addSchema(final String schema) {
            super.addSchema(schema);
        }
        
        @Override
        public Builder addExtensions(final Collection<Extension> extensions) {
            super.addExtensions(extensions);
            return this;
        }
        
        @Override
        public Builder addExtension(final Extension extension) {
            super.addExtension(extension);
            return this;
        }
        
        @Override
        public Builder removeExtensions() {
            super.removeExtensions();
            return this;
        }
        
        @Override
        public Builder removeExtension(final String urn) {
            super.removeExtension(urn);
            return this;
        }
        
        @Override
        public User build() {
            return new User(this, null);
        }
    }
}
