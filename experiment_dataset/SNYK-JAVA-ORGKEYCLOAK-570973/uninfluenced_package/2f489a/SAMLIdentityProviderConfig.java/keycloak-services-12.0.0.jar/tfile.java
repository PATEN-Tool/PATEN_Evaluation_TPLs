// 
// Decompiled by Procyon v0.5.36
// 

package org.keycloak.broker.saml;

import org.keycloak.common.enums.SslRequired;
import org.keycloak.common.util.UriUtils;
import org.keycloak.models.RealmModel;
import org.keycloak.dom.saml.v2.protocol.AuthnContextComparisonType;
import org.keycloak.protocol.saml.SamlPrincipalType;
import org.keycloak.saml.common.util.XmlKeyInfoKeyNameTransformer;
import org.keycloak.models.IdentityProviderModel;

public class SAMLIdentityProviderConfig extends IdentityProviderModel
{
    public static final XmlKeyInfoKeyNameTransformer DEFAULT_XML_KEY_INFO_KEY_NAME_TRANSFORMER;
    public static final String ENTITY_ID = "entityId";
    public static final String ADD_EXTENSIONS_ELEMENT_WITH_KEY_INFO = "addExtensionsElementWithKeyInfo";
    public static final String BACKCHANNEL_SUPPORTED = "backchannelSupported";
    public static final String ENCRYPTION_PUBLIC_KEY = "encryptionPublicKey";
    public static final String FORCE_AUTHN = "forceAuthn";
    public static final String NAME_ID_POLICY_FORMAT = "nameIDPolicyFormat";
    public static final String POST_BINDING_AUTHN_REQUEST = "postBindingAuthnRequest";
    public static final String POST_BINDING_LOGOUT = "postBindingLogout";
    public static final String POST_BINDING_RESPONSE = "postBindingResponse";
    public static final String SIGNATURE_ALGORITHM = "signatureAlgorithm";
    public static final String SIGNING_CERTIFICATE_KEY = "signingCertificate";
    public static final String SINGLE_LOGOUT_SERVICE_URL = "singleLogoutServiceUrl";
    public static final String SINGLE_SIGN_ON_SERVICE_URL = "singleSignOnServiceUrl";
    public static final String VALIDATE_SIGNATURE = "validateSignature";
    public static final String PRINCIPAL_TYPE = "principalType";
    public static final String PRINCIPAL_ATTRIBUTE = "principalAttribute";
    public static final String WANT_ASSERTIONS_ENCRYPTED = "wantAssertionsEncrypted";
    public static final String WANT_ASSERTIONS_SIGNED = "wantAssertionsSigned";
    public static final String WANT_AUTHN_REQUESTS_SIGNED = "wantAuthnRequestsSigned";
    public static final String XML_SIG_KEY_INFO_KEY_NAME_TRANSFORMER = "xmlSigKeyInfoKeyNameTransformer";
    public static final String ENABLED_FROM_METADATA = "enabledFromMetadata";
    public static final String AUTHN_CONTEXT_COMPARISON_TYPE = "authnContextComparisonType";
    public static final String AUTHN_CONTEXT_CLASS_REFS = "authnContextClassRefs";
    public static final String AUTHN_CONTEXT_DECL_REFS = "authnContextDeclRefs";
    public static final String SIGN_SP_METADATA = "signSpMetadata";
    
    public SAMLIdentityProviderConfig() {
    }
    
    public SAMLIdentityProviderConfig(final IdentityProviderModel identityProviderModel) {
        super(identityProviderModel);
    }
    
    public String getEntityId() {
        return this.getConfig().get("entityId");
    }
    
    public void setEntityId(final String entityId) {
        this.getConfig().put("entityId", entityId);
    }
    
    public String getSingleSignOnServiceUrl() {
        return this.getConfig().get("singleSignOnServiceUrl");
    }
    
    public void setSingleSignOnServiceUrl(final String singleSignOnServiceUrl) {
        this.getConfig().put("singleSignOnServiceUrl", singleSignOnServiceUrl);
    }
    
    public String getSingleLogoutServiceUrl() {
        return this.getConfig().get("singleLogoutServiceUrl");
    }
    
    public void setSingleLogoutServiceUrl(final String singleLogoutServiceUrl) {
        this.getConfig().put("singleLogoutServiceUrl", singleLogoutServiceUrl);
    }
    
    public boolean isValidateSignature() {
        return Boolean.valueOf(this.getConfig().get("validateSignature"));
    }
    
    public void setValidateSignature(final boolean validateSignature) {
        this.getConfig().put("validateSignature", String.valueOf(validateSignature));
    }
    
    public boolean isForceAuthn() {
        return Boolean.valueOf(this.getConfig().get("forceAuthn"));
    }
    
    public void setForceAuthn(final boolean forceAuthn) {
        this.getConfig().put("forceAuthn", String.valueOf(forceAuthn));
    }
    
    @Deprecated
    public String getSigningCertificate() {
        return this.getConfig().get("signingCertificate");
    }
    
    @Deprecated
    public void setSigningCertificate(final String signingCertificate) {
        this.getConfig().put("signingCertificate", signingCertificate);
    }
    
    public void addSigningCertificate(final String signingCertificate) {
        final String crt = this.getConfig().get("signingCertificate");
        if (crt == null || crt.isEmpty()) {
            this.getConfig().put("signingCertificate", signingCertificate);
        }
        else {
            this.getConfig().put("signingCertificate", crt + "," + signingCertificate);
        }
    }
    
    public String[] getSigningCertificates() {
        final String crt = this.getConfig().get("signingCertificate");
        if (crt == null || crt.isEmpty()) {
            return new String[0];
        }
        return crt.split(",");
    }
    
    public String getNameIDPolicyFormat() {
        return this.getConfig().get("nameIDPolicyFormat");
    }
    
    public void setNameIDPolicyFormat(final String nameIDPolicyFormat) {
        this.getConfig().put("nameIDPolicyFormat", nameIDPolicyFormat);
    }
    
    public boolean isWantAuthnRequestsSigned() {
        return Boolean.valueOf(this.getConfig().get("wantAuthnRequestsSigned"));
    }
    
    public void setWantAuthnRequestsSigned(final boolean wantAuthnRequestsSigned) {
        this.getConfig().put("wantAuthnRequestsSigned", String.valueOf(wantAuthnRequestsSigned));
    }
    
    public boolean isWantAssertionsSigned() {
        return Boolean.valueOf(this.getConfig().get("wantAssertionsSigned"));
    }
    
    public void setWantAssertionsSigned(final boolean wantAssertionsSigned) {
        this.getConfig().put("wantAssertionsSigned", String.valueOf(wantAssertionsSigned));
    }
    
    public boolean isWantAssertionsEncrypted() {
        return Boolean.valueOf(this.getConfig().get("wantAssertionsEncrypted"));
    }
    
    public void setWantAssertionsEncrypted(final boolean wantAssertionsEncrypted) {
        this.getConfig().put("wantAssertionsEncrypted", String.valueOf(wantAssertionsEncrypted));
    }
    
    public boolean isAddExtensionsElementWithKeyInfo() {
        return Boolean.valueOf(this.getConfig().get("addExtensionsElementWithKeyInfo"));
    }
    
    public void setAddExtensionsElementWithKeyInfo(final boolean addExtensionsElementWithKeyInfo) {
        this.getConfig().put("addExtensionsElementWithKeyInfo", String.valueOf(addExtensionsElementWithKeyInfo));
    }
    
    public String getSignatureAlgorithm() {
        return this.getConfig().get("signatureAlgorithm");
    }
    
    public void setSignatureAlgorithm(final String signatureAlgorithm) {
        this.getConfig().put("signatureAlgorithm", signatureAlgorithm);
    }
    
    public String getEncryptionPublicKey() {
        return this.getConfig().get("encryptionPublicKey");
    }
    
    public void setEncryptionPublicKey(final String encryptionPublicKey) {
        this.getConfig().put("encryptionPublicKey", encryptionPublicKey);
    }
    
    public boolean isPostBindingAuthnRequest() {
        return Boolean.valueOf(this.getConfig().get("postBindingAuthnRequest"));
    }
    
    public void setPostBindingAuthnRequest(final boolean postBindingAuthnRequest) {
        this.getConfig().put("postBindingAuthnRequest", String.valueOf(postBindingAuthnRequest));
    }
    
    public boolean isPostBindingResponse() {
        return Boolean.valueOf(this.getConfig().get("postBindingResponse"));
    }
    
    public void setPostBindingResponse(final boolean postBindingResponse) {
        this.getConfig().put("postBindingResponse", String.valueOf(postBindingResponse));
    }
    
    public boolean isPostBindingLogout() {
        final String postBindingLogout = this.getConfig().get("postBindingLogout");
        if (postBindingLogout == null) {
            return this.isPostBindingResponse();
        }
        return Boolean.valueOf(postBindingLogout);
    }
    
    public void setPostBindingLogout(final boolean postBindingLogout) {
        this.getConfig().put("postBindingLogout", String.valueOf(postBindingLogout));
    }
    
    public boolean isBackchannelSupported() {
        return Boolean.valueOf(this.getConfig().get("backchannelSupported"));
    }
    
    public void setBackchannelSupported(final boolean backchannel) {
        this.getConfig().put("backchannelSupported", String.valueOf(backchannel));
    }
    
    public XmlKeyInfoKeyNameTransformer getXmlSigKeyInfoKeyNameTransformer() {
        return XmlKeyInfoKeyNameTransformer.from((String)this.getConfig().get("xmlSigKeyInfoKeyNameTransformer"), SAMLIdentityProviderConfig.DEFAULT_XML_KEY_INFO_KEY_NAME_TRANSFORMER);
    }
    
    public void setXmlSigKeyInfoKeyNameTransformer(final XmlKeyInfoKeyNameTransformer xmlSigKeyInfoKeyNameTransformer) {
        this.getConfig().put("xmlSigKeyInfoKeyNameTransformer", (xmlSigKeyInfoKeyNameTransformer == null) ? null : xmlSigKeyInfoKeyNameTransformer.name());
    }
    
    public int getAllowedClockSkew() {
        int result = 0;
        final String allowedClockSkew = this.getConfig().get("allowedClockSkew");
        if (allowedClockSkew != null && !allowedClockSkew.isEmpty()) {
            try {
                result = Integer.parseInt(allowedClockSkew);
                if (result < 0) {
                    result = 0;
                }
            }
            catch (NumberFormatException ex) {}
        }
        return result;
    }
    
    public void setAllowedClockSkew(final int allowedClockSkew) {
        if (allowedClockSkew < 0) {
            this.getConfig().remove("allowedClockSkew");
        }
        else {
            this.getConfig().put("allowedClockSkew", String.valueOf(allowedClockSkew));
        }
    }
    
    public SamlPrincipalType getPrincipalType() {
        return SamlPrincipalType.from(this.getConfig().get("principalType"), SamlPrincipalType.SUBJECT);
    }
    
    public void setPrincipalType(final SamlPrincipalType principalType) {
        this.getConfig().put("principalType", (principalType == null) ? null : principalType.name());
    }
    
    public String getPrincipalAttribute() {
        return this.getConfig().get("principalAttribute");
    }
    
    public void setPrincipalAttribute(final String principalAttribute) {
        this.getConfig().put("principalAttribute", principalAttribute);
    }
    
    public boolean isEnabledFromMetadata() {
        return Boolean.valueOf(this.getConfig().get("enabledFromMetadata"));
    }
    
    public void setEnabledFromMetadata(final boolean enabled) {
        this.getConfig().put("enabledFromMetadata", String.valueOf(enabled));
    }
    
    public AuthnContextComparisonType getAuthnContextComparisonType() {
        return AuthnContextComparisonType.fromValue((String)this.getConfig().getOrDefault("authnContextComparisonType", AuthnContextComparisonType.EXACT.value()));
    }
    
    public void setAuthnContextComparisonType(final AuthnContextComparisonType authnContextComparisonType) {
        this.getConfig().put("authnContextComparisonType", authnContextComparisonType.value());
    }
    
    public String getAuthnContextClassRefs() {
        return this.getConfig().get("authnContextClassRefs");
    }
    
    public void setAuthnContextClassRefs(final String authnContextClassRefs) {
        this.getConfig().put("authnContextClassRefs", authnContextClassRefs);
    }
    
    public String getAuthnContextDeclRefs() {
        return this.getConfig().get("authnContextDeclRefs");
    }
    
    public void setAuthnContextDeclRefs(final String authnContextDeclRefs) {
        this.getConfig().put("authnContextDeclRefs", authnContextDeclRefs);
    }
    
    public boolean isSignSpMetadata() {
        return Boolean.valueOf(this.getConfig().get("signSpMetadata"));
    }
    
    public void setSignSpMetadata(final boolean signSpMetadata) {
        this.getConfig().put("signSpMetadata", String.valueOf(signSpMetadata));
    }
    
    public void validate(final RealmModel realm) {
        final SslRequired sslRequired = realm.getSslRequired();
        UriUtils.checkUrl(sslRequired, this.getSingleLogoutServiceUrl(), "singleLogoutServiceUrl");
        UriUtils.checkUrl(sslRequired, this.getSingleSignOnServiceUrl(), "singleSignOnServiceUrl");
    }
    
    static {
        DEFAULT_XML_KEY_INFO_KEY_NAME_TRANSFORMER = XmlKeyInfoKeyNameTransformer.NONE;
    }
}
