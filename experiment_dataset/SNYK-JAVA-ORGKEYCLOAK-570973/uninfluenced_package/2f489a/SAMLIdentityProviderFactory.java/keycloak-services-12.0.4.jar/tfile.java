// 
// Decompiled by Procyon v0.5.36
// 

package org.keycloak.broker.saml;

import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.Config;
import org.w3c.dom.Element;
import java.util.Iterator;
import org.keycloak.dom.saml.v2.metadata.IDPSSODescriptorType;
import java.util.List;
import java.util.HashMap;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import java.util.Date;
import org.keycloak.dom.saml.v2.metadata.KeyTypes;
import org.keycloak.saml.common.util.DocumentUtil;
import javax.xml.namespace.QName;
import org.keycloak.dom.saml.v2.metadata.KeyDescriptorType;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.dom.saml.v2.metadata.EndpointType;
import org.keycloak.dom.saml.v2.metadata.EntityDescriptorType;
import org.keycloak.dom.saml.v2.metadata.EntitiesDescriptorType;
import org.keycloak.saml.processing.core.parsers.saml.SAMLParser;
import java.util.Map;
import java.io.InputStream;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.saml.validators.DestinationValidator;
import org.keycloak.broker.provider.AbstractIdentityProviderFactory;

public class SAMLIdentityProviderFactory extends AbstractIdentityProviderFactory<SAMLIdentityProvider>
{
    public static final String PROVIDER_ID = "saml";
    private static final String MACEDIR_ENTITY_CATEGORY = "http://macedir.org/entity-category";
    private static final String REFEDS_HIDE_FROM_DISCOVERY = "http://refeds.org/category/hide-from-discovery";
    private DestinationValidator destinationValidator;
    
    public String getName() {
        return "SAML v2.0";
    }
    
    public SAMLIdentityProvider create(final KeycloakSession session, final IdentityProviderModel model) {
        return new SAMLIdentityProvider(session, new SAMLIdentityProviderConfig(model), this.destinationValidator);
    }
    
    public SAMLIdentityProviderConfig createConfig() {
        return new SAMLIdentityProviderConfig();
    }
    
    public Map<String, String> parseConfig(final KeycloakSession session, final InputStream inputStream) {
        try {
            final Object parsedObject = SAMLParser.getInstance().parse(inputStream);
            EntityDescriptorType entityType;
            if (EntitiesDescriptorType.class.isInstance(parsedObject)) {
                entityType = ((EntitiesDescriptorType)parsedObject).getEntityDescriptor().get(0);
            }
            else {
                entityType = (EntityDescriptorType)parsedObject;
            }
            final List<EntityDescriptorType.EDTChoiceType> choiceType = (List<EntityDescriptorType.EDTChoiceType>)entityType.getChoiceType();
            if (!choiceType.isEmpty()) {
                IDPSSODescriptorType idpDescriptor = null;
                for (final EntityDescriptorType.EDTChoiceType edtChoiceType : entityType.getChoiceType()) {
                    final List<EntityDescriptorType.EDTDescriptorChoiceType> descriptors = (List<EntityDescriptorType.EDTDescriptorChoiceType>)edtChoiceType.getDescriptors();
                    if (!descriptors.isEmpty() && descriptors.get(0).getIdpDescriptor() != null) {
                        idpDescriptor = descriptors.get(0).getIdpDescriptor();
                    }
                }
                if (idpDescriptor != null) {
                    final SAMLIdentityProviderConfig samlIdentityProviderConfig = new SAMLIdentityProviderConfig();
                    String singleSignOnServiceUrl = null;
                    boolean postBindingResponse = false;
                    boolean postBindingLogout = false;
                    for (final EndpointType endpoint : idpDescriptor.getSingleSignOnService()) {
                        if (endpoint.getBinding().toString().equals(JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.get())) {
                            singleSignOnServiceUrl = endpoint.getLocation().toString();
                            postBindingResponse = true;
                            break;
                        }
                        if (!endpoint.getBinding().toString().equals(JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.get())) {
                            continue;
                        }
                        singleSignOnServiceUrl = endpoint.getLocation().toString();
                    }
                    String singleLogoutServiceUrl = null;
                    for (final EndpointType endpoint2 : idpDescriptor.getSingleLogoutService()) {
                        if (postBindingResponse && endpoint2.getBinding().toString().equals(JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.get())) {
                            singleLogoutServiceUrl = endpoint2.getLocation().toString();
                            postBindingLogout = true;
                            break;
                        }
                        if (!postBindingResponse && endpoint2.getBinding().toString().equals(JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.get())) {
                            singleLogoutServiceUrl = endpoint2.getLocation().toString();
                            break;
                        }
                    }
                    samlIdentityProviderConfig.setSingleLogoutServiceUrl(singleLogoutServiceUrl);
                    samlIdentityProviderConfig.setSingleSignOnServiceUrl(singleSignOnServiceUrl);
                    samlIdentityProviderConfig.setWantAuthnRequestsSigned(idpDescriptor.isWantAuthnRequestsSigned());
                    samlIdentityProviderConfig.setAddExtensionsElementWithKeyInfo(false);
                    samlIdentityProviderConfig.setValidateSignature(idpDescriptor.isWantAuthnRequestsSigned());
                    samlIdentityProviderConfig.setPostBindingResponse(postBindingResponse);
                    samlIdentityProviderConfig.setPostBindingAuthnRequest(postBindingResponse);
                    samlIdentityProviderConfig.setPostBindingLogout(postBindingLogout);
                    samlIdentityProviderConfig.setLoginHint(false);
                    final List<String> nameIdFormatList = (List<String>)idpDescriptor.getNameIDFormat();
                    if (nameIdFormatList != null && !nameIdFormatList.isEmpty()) {
                        samlIdentityProviderConfig.setNameIDPolicyFormat(nameIdFormatList.get(0));
                    }
                    final List<KeyDescriptorType> keyDescriptor = (List<KeyDescriptorType>)idpDescriptor.getKeyDescriptor();
                    String defaultCertificate = null;
                    if (keyDescriptor != null) {
                        for (final KeyDescriptorType keyDescriptorType : keyDescriptor) {
                            final Element keyInfo = keyDescriptorType.getKeyInfo();
                            final Element x509KeyInfo = DocumentUtil.getChildElement(keyInfo, new QName("dsig", "X509Certificate"));
                            if (KeyTypes.SIGNING.equals((Object)keyDescriptorType.getUse())) {
                                samlIdentityProviderConfig.addSigningCertificate(x509KeyInfo.getTextContent());
                            }
                            else if (KeyTypes.ENCRYPTION.equals((Object)keyDescriptorType.getUse())) {
                                samlIdentityProviderConfig.setEncryptionPublicKey(x509KeyInfo.getTextContent());
                            }
                            else {
                                if (keyDescriptorType.getUse() != null) {
                                    continue;
                                }
                                defaultCertificate = x509KeyInfo.getTextContent();
                            }
                        }
                    }
                    if (defaultCertificate != null) {
                        if (samlIdentityProviderConfig.getSigningCertificates().length == 0) {
                            samlIdentityProviderConfig.addSigningCertificate(defaultCertificate);
                        }
                        if (samlIdentityProviderConfig.getEncryptionPublicKey() == null) {
                            samlIdentityProviderConfig.setEncryptionPublicKey(defaultCertificate);
                        }
                    }
                    samlIdentityProviderConfig.setEnabledFromMetadata(entityType.getValidUntil() == null || entityType.getValidUntil().toGregorianCalendar().getTime().after(new Date(System.currentTimeMillis())));
                    if (entityType.getExtensions() != null && entityType.getExtensions().getEntityAttributes() != null) {
                        for (final AttributeType attribute : entityType.getExtensions().getEntityAttributes().getAttribute()) {
                            if ("http://macedir.org/entity-category".equals(attribute.getName()) && attribute.getAttributeValue().contains("http://refeds.org/category/hide-from-discovery")) {
                                samlIdentityProviderConfig.setHideOnLogin(true);
                            }
                        }
                    }
                    return (Map<String, String>)samlIdentityProviderConfig.getConfig();
                }
            }
        }
        catch (ParsingException pe) {
            throw new RuntimeException("Could not parse IdP SAML Metadata", (Throwable)pe);
        }
        return new HashMap<String, String>();
    }
    
    public String getId() {
        return "saml";
    }
    
    public void init(final Config.Scope config) {
        super.init(config);
        this.destinationValidator = DestinationValidator.forProtocolMap(config.getArray("knownProtocols"));
    }
}
