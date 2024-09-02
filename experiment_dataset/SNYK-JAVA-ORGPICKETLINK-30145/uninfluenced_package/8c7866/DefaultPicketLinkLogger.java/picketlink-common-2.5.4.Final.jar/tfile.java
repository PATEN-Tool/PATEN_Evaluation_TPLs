// 
// Decompiled by Procyon v0.5.36
// 

package org.picketlink.common;

import org.picketlink.common.exceptions.fed.IssueInstantMissingException;
import java.security.GeneralSecurityException;
import org.picketlink.common.exceptions.fed.SignatureValidationException;
import org.picketlink.common.exceptions.fed.IssuerNotTrustedException;
import org.picketlink.common.exceptions.fed.AssertionExpiredException;
import org.w3c.dom.Element;
import javax.security.auth.login.LoginException;
import org.picketlink.common.exceptions.fed.WSTrustException;
import javax.xml.ws.WebServiceException;
import java.io.IOException;
import org.picketlink.common.exceptions.ConfigurationException;
import org.picketlink.common.exceptions.ParsingException;
import javax.xml.stream.Location;
import org.picketlink.common.exceptions.TrustKeyProcessingException;
import org.picketlink.common.exceptions.TrustKeyConfigurationException;
import javax.xml.crypto.dsig.XMLSignatureException;
import org.picketlink.common.exceptions.ProcessingException;
import org.jboss.logging.Logger;

public class DefaultPicketLinkLogger implements PicketLinkLogger
{
    private Logger logger;
    
    DefaultPicketLinkLogger() {
        this.logger = Logger.getLogger(PicketLinkLogger.class.getPackage().getName());
    }
    
    @Override
    public void info(final String message) {
        if (this.logger.isInfoEnabled()) {
            this.logger.info((Object)message);
        }
    }
    
    @Override
    public void debug(final String message) {
        if (this.logger.isDebugEnabled()) {
            this.logger.debug((Object)message);
        }
    }
    
    @Override
    public void trace(final String message) {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace((Object)message);
        }
    }
    
    @Override
    public void trace(final String message, final Throwable t) {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace((Object)message, t);
        }
    }
    
    @Override
    public void trace(final Throwable t) {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace((Object)t.getMessage(), t);
        }
    }
    
    @Override
    public void error(final Throwable t) {
        this.logger.error((Object)"Unexpected error", t);
    }
    
    @Override
    public IllegalArgumentException nullArgumentError(final String argument) {
        return new IllegalArgumentException("PL00078: Null Parameter:" + argument);
    }
    
    @Override
    public IllegalArgumentException shouldNotBeTheSameError(final String string) {
        return new IllegalArgumentException("PL00016: Should not be the same:Only one of isSigningKey and isEncryptionKey should be true");
    }
    
    @Override
    public ProcessingException resourceNotFound(final String resource) {
        return new ProcessingException("PL00018: Resource not found:" + resource + " could not be loaded");
    }
    
    @Override
    public ProcessingException processingError(final Throwable t) {
        return new ProcessingException("PL00102: Processing Exception:", t);
    }
    
    @Override
    public RuntimeException unsupportedType(final String name) {
        return new RuntimeException("PL00069: Parser: Type not supported:" + name);
    }
    
    @Override
    public XMLSignatureException signatureError(final Throwable e) {
        return new XMLSignatureException("PL00100: Signing Process Failure:", e);
    }
    
    @Override
    public RuntimeException nullValueError(final String nullValue) {
        return new RuntimeException("PL00092: Null Value:" + nullValue);
    }
    
    @Override
    public RuntimeException notImplementedYet(final String feature) {
        return new RuntimeException("PL00082: Not Implemented Yet: " + feature);
    }
    
    @Override
    public IllegalStateException auditNullAuditManager() {
        return new IllegalStateException("PL00028: Audit Manager Is Not Set");
    }
    
    @Override
    public boolean isInfoEnabled() {
        return this.logger.isInfoEnabled();
    }
    
    @Override
    public void auditEvent(final String auditEvent) {
        this.info(auditEvent);
    }
    
    @Override
    public RuntimeException injectedValueMissing(final String value) {
        return new RuntimeException("PL00077: Injected Value Missing:" + value);
    }
    
    @Override
    public void keyStoreSetup() {
        this.trace("getPublicKey::Keystore is null. so setting it up");
    }
    
    @Override
    public IllegalStateException keyStoreNullStore() {
        return new IllegalStateException("PL00055: KeyStoreKeyManager : KeyStore is null");
    }
    
    @Override
    public void keyStoreNullPublicKeyForAlias(final String alias) {
        this.trace("No public key found for alias=" + alias);
    }
    
    @Override
    public TrustKeyConfigurationException keyStoreConfigurationError(final Throwable t) {
        return new TrustKeyConfigurationException(t);
    }
    
    @Override
    public TrustKeyProcessingException keyStoreProcessingError(final Throwable t) {
        return new TrustKeyProcessingException(t);
    }
    
    @Override
    public IllegalStateException keyStoreMissingDomainAlias(final String domain) {
        return new IllegalStateException("PL00058: KeyStoreKeyManager : Domain Alias missing for :" + domain);
    }
    
    @Override
    public RuntimeException keyStoreNullSigningKeyPass() {
        return new RuntimeException("PL00057: KeyStoreKeyManager :: Signing Key Pass is null");
    }
    
    @Override
    public RuntimeException keyStoreNullEncryptionKeyPass() {
        return new RuntimeException("PL00189: KeyStoreKeyManager :: Encryption Key Pass is null");
    }
    
    @Override
    public RuntimeException keyStoreNotLocated(final String keyStore) {
        return new RuntimeException("PL00056: KeyStoreKeyManager: Keystore not located:" + keyStore);
    }
    
    @Override
    public IllegalStateException keyStoreNullAlias() {
        return new IllegalStateException("PL00059: KeyStoreKeyManager : Alias is null");
    }
    
    @Override
    public RuntimeException parserUnknownEndElement(final String endElementName) {
        return new RuntimeException("PL00061: Parser: Unknown End Element:" + endElementName);
    }
    
    @Override
    public RuntimeException parserUnknownTag(final String tag, final Location location) {
        return new RuntimeException("PL00062: Parser : Unknown tag:" + tag + "::location=" + location);
    }
    
    @Override
    public ParsingException parserRequiredAttribute(final String string) {
        return new ParsingException("PL00063: Parser: Required attribute missing: " + string);
    }
    
    @Override
    public RuntimeException parserUnknownStartElement(final String elementName, final Location location) {
        return new RuntimeException("PL00064: Parser: Unknown Start Element: " + elementName + "::location=" + location);
    }
    
    @Override
    public IllegalStateException parserNullStartElement() {
        return new IllegalStateException("PL00068: Parser : Start Element is null");
    }
    
    @Override
    public ParsingException parserUnknownXSI(final String xsiTypeValue) {
        return new ParsingException("PL0065: Parser : Unknown xsi:type=" + xsiTypeValue);
    }
    
    @Override
    public ParsingException parserExpectedEndTag(final String tagName) {
        return new ParsingException("PL00066: Parser : Expected end tag:RequestAbstract or XACMLAuthzDecisionQuery");
    }
    
    @Override
    public ParsingException parserException(final Throwable t) {
        return new ParsingException(t);
    }
    
    @Override
    public ParsingException parserExpectedTextValue(final String string) {
        return new ParsingException("PL00071: Parser: Expected text value:SigningAlias");
    }
    
    @Override
    public RuntimeException parserExpectedXSI(final String expectedXsi) {
        return new RuntimeException(expectedXsi);
    }
    
    @Override
    public RuntimeException parserExpectedTag(final String tag, final String foundElementTag) {
        return new RuntimeException("PL00066: Parser : Expected start tag:" + tag + ">.  Found <" + foundElementTag + ">");
    }
    
    @Override
    public RuntimeException parserFailed(final String elementName) {
        return new RuntimeException("PL00067: Parsing has failed:" + elementName);
    }
    
    @Override
    public ParsingException parserUnableParsingNullToken() {
        return new ParsingException("PL00073: Parser: Unable to parse token request: security token is null");
    }
    
    @Override
    public ParsingException parserError(final Throwable t) {
        return new ParsingException("PL00074: Parsing Error:" + t.getMessage(), t);
    }
    
    @Override
    public RuntimeException xacmlPDPMessageProcessingError(final Throwable t) {
        return new RuntimeException(t);
    }
    
    @Override
    public IllegalStateException fileNotLocated(final String policyConfigFileName) {
        return new IllegalStateException("PL00075: File could not be located :" + policyConfigFileName);
    }
    
    @Override
    public IllegalStateException optionNotSet(final String option) {
        return new IllegalStateException("PL00076: Option not set:" + option);
    }
    
    @Override
    public void stsTokenRegistryNotSpecified() {
        this.warn("Security Token registry option not specified: Issued Tokens will not be persisted!");
    }
    
    @Override
    public void stsTokenRegistryInvalidType(final String tokenRegistryOption) {
        this.logger.warn((Object)(tokenRegistryOption + " is not an instance of SecurityTokenRegistry - using default registry"));
    }
    
    @Override
    public void stsTokenRegistryInstantiationError() {
        this.logger.warn((Object)"Error instantiating token registry class - using default registry");
    }
    
    @Override
    public void stsRevocationRegistryNotSpecified() {
        this.debug("Revocation registry option not specified: cancelled ids will not be persisted!");
    }
    
    @Override
    public void stsRevocationRegistryInvalidType(final String registryOption) {
        this.logger.warn((Object)(registryOption + " is not an instance of RevocationRegistry - using default registry"));
    }
    
    @Override
    public void stsRevocationRegistryInstantiationError() {
        this.logger.warn((Object)"Error instantiating revocation registry class - using default registry");
    }
    
    @Override
    public ProcessingException samlAssertionExpiredError() {
        return new ProcessingException("PL00079: Assertion has expired:");
    }
    
    @Override
    public ProcessingException assertionInvalidError() {
        return new ProcessingException("PL00080: Invalid Assertion:");
    }
    
    @Override
    public RuntimeException writerUnknownTypeError(final String name) {
        return new RuntimeException("PL00081: Writer: Unknown Type:" + name);
    }
    
    @Override
    public ProcessingException writerNullValueError(final String value) {
        return new ProcessingException("PL00083: Writer: Null Value:" + value);
    }
    
    @Override
    public RuntimeException writerUnsupportedAttributeValueError(final String value) {
        return new RuntimeException("PL00084: Writer: Unsupported Attribute Value:" + value);
    }
    
    @Override
    public IllegalArgumentException issuerInfoMissingStatusCodeError() {
        return new IllegalArgumentException("PL00085: IssuerInfo missing status code :");
    }
    
    @Override
    public ProcessingException classNotLoadedError(final String fqn) {
        return new ProcessingException("PL00085: Class Not Loaded:" + fqn);
    }
    
    @Override
    public ProcessingException couldNotCreateInstance(final String fqn, final Throwable t) {
        return new ProcessingException("PL00086: Cannot create instance of:" + fqn, t);
    }
    
    @Override
    public RuntimeException systemPropertyMissingError(final String property) {
        return new RuntimeException("PL00087: System Property missing:" + property);
    }
    
    @Override
    public void samlMetaDataIdentityProviderLoadingError(final Throwable t) {
        this.logger.error((Object)"Exception loading the identity providers:", t);
    }
    
    @Override
    public void samlMetaDataServiceProviderLoadingError(final Throwable t) {
        this.logger.error((Object)"Exception loading the service providers:", t);
    }
    
    @Override
    public void signatureAssertionValidationError(final Throwable t) {
        this.logger.error((Object)"Cannot validate signature of assertion", t);
    }
    
    @Override
    public void samlAssertionExpired(final String id) {
        this.info("Assertion has expired with id=" + id);
    }
    
    @Override
    public RuntimeException unknownObjectType(final Object attrValue) {
        return new RuntimeException("PL00089: Unknown Object Type:" + attrValue);
    }
    
    @Override
    public ConfigurationException configurationError(final Throwable t) {
        return new ConfigurationException(t);
    }
    
    @Override
    public RuntimeException signatureUnknownAlgo(final String algo) {
        return new RuntimeException("PL00090: Unknown Signature Algorithm:" + algo);
    }
    
    @Override
    public IllegalArgumentException invalidArgumentError(final String message) {
        return new IllegalArgumentException(message);
    }
    
    @Override
    public ProcessingException stsNoTokenProviderError(final String configuration, final String protocolContext) {
        return new ProcessingException("PL00013: No Security Token Provider found in configuration:[" + configuration + "][ProtoCtx=" + protocolContext + "]");
    }
    
    @Override
    public void stsConfigurationFileNotFoundTCL(final String fileName) {
        this.logger.warn((Object)(fileName + " configuration file not found using TCCL"));
    }
    
    @Override
    public void stsConfigurationFileNotFoundClassLoader(final String fileName) {
        this.logger.warn((Object)(fileName + " configuration file not found using class loader"));
    }
    
    @Override
    public void stsUsingDefaultConfiguration(final String fileName) {
        this.logger.warn((Object)(fileName + " configuration file not found using URL. Using default configuration values"));
    }
    
    @Override
    public void stsConfigurationFileLoaded(final String fileName) {
        this.info(fileName + " configuration file loaded");
    }
    
    @Override
    public ConfigurationException stsConfigurationFileParsingError(final Throwable t) {
        return new ConfigurationException("PL00005: Error parsing the configuration file:", t);
    }
    
    @Override
    public IOException notSerializableError(final String message) {
        return new IOException("PL00093: Not Serializable:" + message);
    }
    
    @Override
    public void trustKeyManagerCreationError(final Throwable t) {
        this.logger.error((Object)"Exception creating TrustKeyManager:", t);
    }
    
    @Override
    public void error(final String message) {
        this.logger.error((Object)message);
    }
    
    @Override
    public void xmlCouldNotGetSchema(final Throwable t) {
        this.logger.error((Object)"Cannot get schema", t);
    }
    
    @Override
    public boolean isTraceEnabled() {
        return this.logger.isTraceEnabled();
    }
    
    @Override
    public boolean isDebugEnabled() {
        return this.logger.isDebugEnabled();
    }
    
    @Override
    public void jceProviderCouldNotBeLoaded(final String name, final Throwable t) {
        this.logger.debug((Object)("The provider " + name + " could not be added: "), t);
        this.logger.debug((Object)"Check addJceProvider method of org.picketlink.identity.federation.core.util.ProvidersUtil for more info.");
    }
    
    @Override
    public ProcessingException writerInvalidKeyInfoNullContentError() {
        return new ProcessingException("PL00091: Writer: Invalid KeyInfo object: content cannot be empty");
    }
    
    @Override
    public RuntimeException notEqualError(final String first, final String second) {
        return new RuntimeException("PL00094: Not equal:" + first + " and " + second);
    }
    
    @Override
    public IllegalArgumentException wrongTypeError(final String message) {
        return new IllegalArgumentException("PL00095: Wrong type:xmlSource should be a stax source");
    }
    
    @Override
    public RuntimeException encryptUnknownAlgoError(final String certAlgo) {
        return new RuntimeException("PL00097: Unknown Encryption Algorithm:" + certAlgo);
    }
    
    @Override
    public IllegalStateException domMissingDocElementError(final String element) {
        return new IllegalStateException("PL00098: Missing Document Element:" + element);
    }
    
    @Override
    public IllegalStateException domMissingElementError(final String element) {
        return new IllegalStateException("PL00099: Missing Element:" + element);
    }
    
    @Override
    public WebServiceException stsWSInvalidTokenRequestError() {
        return new WebServiceException("PL00001: Invalid security token request");
    }
    
    @Override
    public WebServiceException stsWSError(final Throwable t) {
        return new WebServiceException("Security Token Service Exception", t);
    }
    
    @Override
    public WebServiceException stsWSConfigurationError(final Throwable t) {
        return new WebServiceException("PL00002: Encountered configuration exception:", t);
    }
    
    @Override
    public WSTrustException stsWSInvalidRequestTypeError(final String requestType) {
        return new WSTrustException("PL00001: Invalid request type: " + requestType);
    }
    
    @Override
    public WebServiceException stsWSHandlingTokenRequestError(final Throwable t) {
        return new WebServiceException("PL00003: Exception in handling token request: " + t.getMessage(), t);
    }
    
    @Override
    public WebServiceException stsWSResponseWritingError(final Throwable t) {
        return new WebServiceException("PL00004: Error writing response: " + t.getMessage(), t);
    }
    
    @Override
    public RuntimeException stsUnableToConstructKeyManagerError(final Throwable t) {
        return new RuntimeException("PL00007: Unable to construct the key manager:", t);
    }
    
    @Override
    public RuntimeException stsPublicKeyError(final String serviceName, final Throwable t) {
        return new RuntimeException("PL00010: Error obtaining public key for service: " + serviceName, t);
    }
    
    @Override
    public RuntimeException stsSigningKeyPairError(final Throwable t) {
        return new RuntimeException("PL00011: Error obtaining signing key pair:", t);
    }
    
    @Override
    public RuntimeException stsPublicKeyCertError(final Throwable t) {
        return new RuntimeException("PL00012: Error obtaining public key certificate:", t);
    }
    
    @Override
    public void stsTokenTimeoutNotSpecified() {
        this.warn("Lifetime has not been specified. Using the default timeout value.");
    }
    
    @Override
    public WSTrustException wsTrustCombinedSecretKeyError(final Throwable t) {
        return new WSTrustException("PL00006: Error generating combined secret key:", t);
    }
    
    @Override
    public WSTrustException wsTrustClientPublicKeyError() {
        return new WSTrustException("PL00008: Unable to locate client public key");
    }
    
    @Override
    public WSTrustException stsError(final Throwable t) {
        return new WSTrustException(t.getMessage(), t);
    }
    
    @Override
    public XMLSignatureException signatureInvalidError(final String message, final Throwable t) {
        return new XMLSignatureException("PL00009: Invalid Digital Signature:" + message);
    }
    
    @Override
    public void stsSecurityTokenSignatureNotVerified() {
        this.warn("Security Token digital signature has NOT been verified. Either the STS has been configurednot to sign tokens or the STS key pair has not been properly specified.");
    }
    
    @Override
    public RuntimeException encryptProcessError(final Throwable t) {
        return new RuntimeException(t);
    }
    
    @Override
    public void stsSecurityTokenShouldBeEncrypted() {
        this.logger.warn((Object)"Security token should be encrypted but no encrypting key could be found");
    }
    
    @Override
    public RuntimeException unableToDecodePasswordError(final String password) {
        return new RuntimeException("PL00102: Processing Exception:Unable to decode password:" + password);
    }
    
    @Override
    public IllegalStateException couldNotLoadProperties(final String configFile) {
        return new IllegalStateException("PL00102: Processing Exception:Could not load properties from " + configFile);
    }
    
    @Override
    public WSTrustException stsKeyInfoTypeCreationError(final Throwable t) {
        return new WSTrustException("PL00102: Processing Exception:Error creating KeyInfoType", t);
    }
    
    @Override
    public void stsSecretKeyNotEncrypted() {
        this.logger.warn((Object)"Secret key could not be encrypted because the endpoint's PKC has not been specified");
    }
    
    @Override
    public LoginException authCouldNotIssueSAMLToken() {
        return new LoginException("PL00102: Processing Exception:Could not issue a SAML Security Token");
    }
    
    @Override
    public LoginException authLoginError(final Throwable t) {
        final LoginException loginException = new LoginException("Error during login/authentication");
        loginException.initCause(t);
        return loginException;
    }
    
    @Override
    public IllegalStateException authCouldNotCreateWSTrustClient(final Throwable t) {
        return new IllegalStateException("PL00102: Processing Exception:Could not create WSTrustClient:", t);
    }
    
    @Override
    public void samlAssertionWithoutExpiration(final String id) {
        this.logger.warn((Object)("SAML Assertion has been found to have no expiration: ID = " + id));
    }
    
    @Override
    public LoginException authCouldNotValidateSAMLToken(final Element token) {
        return new LoginException("PL00102: Processing Exception:Could not validate the SAML Security Token :" + token);
    }
    
    @Override
    public LoginException authCouldNotLocateSecurityToken() {
        return new LoginException("PL00092: Null Value:Could not locate a Security Token from the callback.");
    }
    
    @Override
    public ProcessingException wsTrustNullCancelTargetError() {
        return new ProcessingException("PL00092: Null Value:Invalid cancel request: missing required CancelTarget");
    }
    
    @Override
    public ProcessingException samlAssertionMarshallError(final Throwable t) {
        return new ProcessingException("PL00102: Processing Exception:Failed to marshall assertion", t);
    }
    
    @Override
    public ProcessingException wsTrustNullRenewTargetError() {
        return new ProcessingException("PL00092: Null Value:Invalid renew request: missing required RenewTarget");
    }
    
    @Override
    public ProcessingException samlAssertionUnmarshallError(final Throwable t) {
        return new ProcessingException("PL00102: Processing Exception:Error unmarshalling assertion", t);
    }
    
    @Override
    public ProcessingException samlAssertionRevokedCouldNotRenew(final String id) {
        return new ProcessingException("PL00103:Assertion Renewal Exception:SAMLV1.1 Assertion with id " + id + " has been canceled and cannot be renewed");
    }
    
    @Override
    public ProcessingException wsTrustNullValidationTargetError() {
        return new ProcessingException("PL00092: Null Value:Bad validate request: missing required ValidateTarget");
    }
    
    @Override
    public void stsWrongAttributeProviderTypeNotInstalled(final String attributeProviderClassName) {
        this.logger.warn((Object)("Attribute provider not installed: " + attributeProviderClassName + "is not an instance of SAML20TokenAttributeProvider"));
    }
    
    @Override
    public void attributeProviderInstationError(final Throwable t) {
        this.logger.warn((Object)("Error instantiating attribute provider: " + t));
    }
    
    @Override
    public void samlAssertion(final String nodeAsString) {
        this.trace("SAML Assertion Element=" + nodeAsString);
    }
    
    @Override
    public RuntimeException wsTrustUnableToGetDataTypeFactory(final Throwable t) {
        return new RuntimeException("PL00102: Processing Exception:Unable to get DatatypeFactory instance", t);
    }
    
    @Override
    public ProcessingException wsTrustValidationStatusCodeMissing() {
        return new ProcessingException("PL00092: Null Value:Validation status code is missing");
    }
    
    @Override
    public void samlIdentityServerActiveSessionCount(final int activeSessionCount) {
        this.info("Active Session Count=" + activeSessionCount);
    }
    
    @Override
    public void samlIdentityServerSessionCreated(final String id, final int activeSessionCount) {
        this.trace("Session Created with id=" + id + "::active session count=" + activeSessionCount);
    }
    
    @Override
    public void samlIdentityServerSessionDestroyed(final String id, final int activeSessionCount) {
        this.trace("Session Destroyed with id=" + id + "::active session count=" + activeSessionCount);
    }
    
    @Override
    public RuntimeException unknowCredentialType(final String name) {
        return new RuntimeException("PL00069: Parser: Type not supported:Unknown credential type:" + name);
    }
    
    @Override
    public void samlHandlerRoleGeneratorSetupError(final Throwable t) {
        this.logger.error((Object)"Exception initializing role generator:", t);
    }
    
    @Override
    public RuntimeException samlHandlerAssertionNotFound() {
        return new RuntimeException("PL00092: Null Value:Assertion not found in the handler request");
    }
    
    @Override
    public ProcessingException samlHandlerAuthnRequestIsNull() {
        return new ProcessingException("PL00092: Null Value:AuthnRequest is null");
    }
    
    @Override
    public void samlHandlerAuthenticationError(final Throwable t) {
        this.logger.error((Object)"Exception in processing authentication:", t);
    }
    
    @Override
    public IllegalArgumentException samlHandlerNoAssertionFromIDP() {
        return new IllegalArgumentException("PL00092: Null Value:No assertions in reply from IDP");
    }
    
    @Override
    public ProcessingException samlHandlerNullEncryptedAssertion() {
        return new ProcessingException("PL00092: Null Value:Null encrypted assertion element");
    }
    
    @Override
    public SecurityException samlHandlerIDPAuthenticationFailedError() {
        return new SecurityException("PL00015: IDP Authentication Failed:IDP forbid the user");
    }
    
    @Override
    public ProcessingException assertionExpiredError(final AssertionExpiredException aee) {
        return new ProcessingException(new ProcessingException("PL00079: Assertion has expired:Assertion has expired", aee));
    }
    
    @Override
    public RuntimeException unsupportedRoleType(final Object attrValue) {
        return new RuntimeException("PL00069: Parser: Type not supported:Unknown role object type : " + attrValue);
    }
    
    @Override
    public void samlHandlerFailedInResponseToVerification(final String inResponseTo, final String authnRequestId) {
        this.trace("Verification of InResponseTo failed. InResponseTo from SAML response is " + inResponseTo + ". Value of request Id from HTTP session is " + authnRequestId);
    }
    
    @Override
    public ProcessingException samlHandlerFailedInResponseToVerificarionError() {
        return new ProcessingException("PL00104:Authn Request ID verification failed:");
    }
    
    @Override
    public IssuerNotTrustedException samlIssuerNotTrustedError(final String issuer) {
        return new IssuerNotTrustedException("Issuer not Trusted by the IDP: " + issuer);
    }
    
    @Override
    public IssuerNotTrustedException samlIssuerNotTrustedException(final Throwable t) {
        return new IssuerNotTrustedException(t);
    }
    
    @Override
    public ConfigurationException samlHandlerTrustElementMissingError() {
        return new ConfigurationException("PL00092: Null Value:trust element missing");
    }
    
    @Override
    public ProcessingException samlHandlerIdentityServerNotFoundError() {
        return new ProcessingException("PL00092: Null Value:Identity Server not found");
    }
    
    @Override
    public ProcessingException samlHandlerPrincipalNotFoundError() {
        return new ProcessingException("PL00022: Principal Not Found");
    }
    
    @Override
    public void samlHandlerKeyPairNotFound() {
        this.trace("Key Pair cannot be found");
    }
    
    @Override
    public ProcessingException samlHandlerKeyPairNotFoundError() {
        return new ProcessingException("Key Pair cannot be found");
    }
    
    @Override
    public void samlHandlerErrorSigningRedirectBindingMessage(final Throwable t) {
        this.logger.error((Object)"Error when trying to sign message for redirection", t);
    }
    
    @Override
    public RuntimeException samlHandlerSigningRedirectBindingMessageError(final Throwable t) {
        return new RuntimeException(t);
    }
    
    @Override
    public SignatureValidationException samlHandlerSignatureValidationFailed() {
        return new SignatureValidationException("PL00009: Invalid Digital Signature:Signature Validation Failed");
    }
    
    @Override
    public void samlHandlerErrorValidatingSignature(final Throwable t) {
        this.logger.error((Object)"Error validating signature:", t);
    }
    
    @Override
    public ProcessingException samlHandlerInvalidSignatureError() {
        return new ProcessingException("PL00009: Invalid Digital Signature:Error validating signature.");
    }
    
    @Override
    public ProcessingException samlHandlerSignatureNotPresentError() {
        return new ProcessingException("PL00009: Invalid Digital Signature:Signature Validation failed. Signature is not present. Check if the IDP is supporting signatures.");
    }
    
    @Override
    public ProcessingException samlHandlerSignatureValidationError(final Throwable t) {
        return new ProcessingException("PL00009: Invalid Digital Signature:Signature Validation failed", t);
    }
    
    @Override
    public RuntimeException samlHandlerChainProcessingError(final Throwable t) {
        return new RuntimeException("Error during processing the SAML Handler Chain.", t);
    }
    
    @Override
    public TrustKeyConfigurationException trustKeyManagerMissing() {
        return new TrustKeyConfigurationException("PL000023: Trust Key Manager Missing");
    }
    
    @Override
    public void samlBase64DecodingError(final Throwable t) {
        this.error("Error in base64 decoding saml message: " + t);
    }
    
    @Override
    public void samlParsingError(final Throwable t) {
        this.logger.error((Object)"Exception in parsing saml message:", t);
    }
    
    @Override
    public void mappingContextNull() {
        this.logger.error((Object)"Mapping Context returned is null");
    }
    
    @Override
    public void attributeManagerError(final Throwable t) {
        this.logger.error((Object)"Exception in attribute mapping:", t);
    }
    
    @Override
    public void couldNotObtainSecurityContext() {
        this.logger.error((Object)"Could not obtain security context.");
    }
    
    @Override
    public LoginException authFailedToCreatePrincipal(final Throwable t) {
        final LoginException loginException = new LoginException("PL00102: Processing Exception:Failed to create principal: " + t.getMessage());
        loginException.initCause(t);
        return loginException;
    }
    
    @Override
    public LoginException authSharedCredentialIsNotSAMLCredential(final String className) {
        return new LoginException("PL00095: Wrong type:SAML2STSLoginModule: Shared credential is not a SAML credential. Got " + className);
    }
    
    @Override
    public LoginException authSTSConfigFileNotFound() {
        return new LoginException("PL00039: SAML2STSLoginModule: Failed to validate assertion: STS configuration file not specified");
    }
    
    @Override
    public LoginException authErrorHandlingCallback(final Throwable t) {
        final LoginException loginException = new LoginException("Error handling callback.");
        loginException.initCause(t);
        return loginException;
    }
    
    @Override
    public LoginException authInvalidSAMLAssertionBySTS() {
        return new LoginException("PL00080: Invalid Assertion:SAML2STSLoginModule: Supplied assertion was considered invalid by the STS");
    }
    
    @Override
    public LoginException authAssertionValidationError(final Throwable t) {
        final LoginException loginException = new LoginException("Failed to validate assertion using STS");
        loginException.initCause(t);
        return loginException;
    }
    
    @Override
    public LoginException authFailedToParseSAMLAssertion(final Throwable t) {
        final LoginException exception = new LoginException("PL00044: SAML2STSLoginModule: Failed to parse assertion element:" + t.getMessage());
        exception.initCause(t);
        return exception;
    }
    
    @Override
    public void samlAssertionPasingFailed(final Throwable t) {
        this.logger.error((Object)"SAML Assertion parsing failed", t);
    }
    
    @Override
    public LoginException authNullKeyStoreFromSecurityDomainError(final String name) {
        return new LoginException("PL00092: Null Value:SAML2STSLoginModule: null truststore for " + name);
    }
    
    @Override
    public LoginException authNullKeyStoreAliasFromSecurityDomainError(final String name) {
        return new LoginException("PL00092: Null Value:SAML2STSLoginModule: null KeyStoreAlias for " + name + "; set 'KeyStoreAlias' in '" + name + "' security domain configuration");
    }
    
    @Override
    public LoginException authNoCertificateFoundForAliasError(final String alias, final String name) {
        return new LoginException("PL00092: Null Value:No certificate found for alias '" + alias + "' in the '" + name + "' security domain");
    }
    
    @Override
    public LoginException authSAMLInvalidSignatureError() {
        return new LoginException("PL00009: Invalid Digital Signature:SAML2STSLoginModule: http://docs.oasis-open.org/ws-sx/ws-trust/200512/status/invalid : invalid SAML V2.0 assertion signature");
    }
    
    @Override
    public LoginException authSAMLAssertionExpiredError() {
        return new LoginException("PL00079: Assertion has expired:SAML2STSLoginModule: http://docs.oasis-open.org/ws-sx/ws-trust/200512/status/invalid::assertion expired or used before its lifetime period");
    }
    
    @Override
    public void authSAMLAssertionIssuingFailed(final Throwable t) {
        this.logger.error((Object)"Unable to issue assertion", t);
    }
    
    @Override
    public void jbossWSUnableToCreateBinaryToken(final Throwable t) {
        this.logger.error((Object)"Unable to create binary token", t);
    }
    
    @Override
    public void jbossWSUnableToCreateSecurityToken() {
        this.logger.warn((Object)"Was not able to create security token. Just sending message without binary token");
    }
    
    @Override
    public void jbossWSUnableToWriteSOAPMessage(final Throwable t) {
        this.logger.error((Object)"Exception writing SOAP Message", t);
    }
    
    @Override
    public RuntimeException jbossWSUnableToLoadJBossWSSEConfigError() {
        return new RuntimeException("PL00018: Resource not found:unable to load jboss-wsse.xml");
    }
    
    @Override
    public RuntimeException jbossWSAuthorizationFailed() {
        return new RuntimeException("PL00102: Processing Exception:Authorization Failed");
    }
    
    @Override
    public void jbossWSErrorGettingOperationName(final Throwable t) {
        this.logger.error((Object)"Exception using backup method to get op name=", t);
    }
    
    @Override
    public LoginException authSAMLCredentialNotAvailable() {
        return new LoginException("PL00092: Null Value:SamlCredential is not available in subject");
    }
    
    @Override
    public RuntimeException authUnableToInstantiateHandler(final String token, final Throwable t) {
        return new RuntimeException("PL00086: Cannot create instance of:Unable to instantiate handler:" + token, t);
    }
    
    @Override
    public RuntimeException jbossWSUnableToCreateSSLSocketFactory(final Throwable t) {
        return new RuntimeException("PL00102: Processing Exception:Unable to create SSL Socket Factory:", t);
    }
    
    @Override
    public RuntimeException jbossWSUnableToFindSSLSocketFactory() {
        return new RuntimeException("We did not find SSL Socket Factory");
    }
    
    @Override
    public RuntimeException authUnableToGetIdentityFromSubject() {
        return new RuntimeException("PL00102: Processing Exception:Unable to get the Identity from the subject.");
    }
    
    @Override
    public RuntimeException authSAMLAssertionNullOrEmpty() {
        return new RuntimeException("SAML Assertion is null or empty");
    }
    
    @Override
    public ProcessingException jbossWSUncheckedAndRolesCannotBeTogether() {
        return new ProcessingException("PL00102: Processing Exception:unchecked and role(s) cannot be together");
    }
    
    @Override
    public void samlIDPHandlingSAML11Error(final Throwable t) {
        this.logger.error((Object)"Exception handling saml 11 use case:", t);
    }
    
    @Override
    public GeneralSecurityException samlIDPValidationCheckFailed() {
        return new GeneralSecurityException("PL00019: Validation check failed");
    }
    
    @Override
    public void samlIDPRequestProcessingError(final Throwable t) {
        this.logger.error((Object)"Exception in processing request:", t);
    }
    
    @Override
    public void samlIDPUnableToSetParticipantStackUsingDefault(final Throwable t) {
        this.logger.warn((Object)"Unable to set the Identity Participant Stack Class. Will just use the default");
    }
    
    @Override
    public void samlHandlerConfigurationError(final Throwable t) {
        this.logger.error((Object)"Exception dealing with handler configuration:", t);
    }
    
    @Override
    public void samlIDPSettingCanonicalizationMethod(final String canonicalizationMethod) {
        this.logger.debug((Object)("Setting the CanonicalizationMethod on XMLSignatureUtil::" + canonicalizationMethod));
    }
    
    @Override
    public RuntimeException samlIDPConfigurationError(final Throwable t) {
        return new RuntimeException("PL00102: Processing Exception:" + t.getMessage(), t);
    }
    
    @Override
    public RuntimeException configurationFileMissing(final String configFile) {
        return new RuntimeException("PL00017: Configuration File missing:" + configFile);
    }
    
    @Override
    public void samlIDPInstallingDefaultSTSConfig() {
        this.logger.info((Object)"Did not find picketlink-sts.xml. We will install default configuration");
    }
    
    @Override
    public void warn(final String message) {
        this.logger.warn((Object)message);
    }
    
    @Override
    public void samlSPFallingBackToLocalFormAuthentication() {
        this.logger.error((Object)"Falling back on local Form Authentication if available");
    }
    
    @Override
    public IOException unableLocalAuthentication(final Throwable t) {
        return new IOException("PL00035: Unable to fallback on local auth:", t);
    }
    
    @Override
    public void samlSPUnableToGetIDPDescriptorFromMetadata() {
        this.logger.error((Object)"Unable to obtain the IDP SSO Descriptor from metadata");
    }
    
    @Override
    public RuntimeException samlSPConfigurationError(final Throwable t) {
        return new RuntimeException(t.getMessage(), t);
    }
    
    @Override
    public void samlSPSettingCanonicalizationMethod(final String canonicalizationMethod) {
        this.logger.info((Object)("Service Provider is setting the CanonicalizationMethod on XMLSignatureUtil::" + canonicalizationMethod));
    }
    
    @Override
    public void samlSPCouldNotDispatchToLogoutPage(final String logOutPage) {
        this.logger.errorf("Cannot dispatch to the logout page: no request dispatcher" + logOutPage, new Object[0]);
    }
    
    @Override
    public void usingLoggerImplementation(final String className) {
        this.logger.debugf("Using logger implementation: " + className, new Object[0]);
    }
    
    @Override
    public void samlResponseFromIDPParsingFailed() {
        this.logger.error((Object)"Error parsing the response from the IDP. Check the strict post binding configuration on both IDP and SP side.");
    }
    
    @Override
    public ConfigurationException auditSecurityDomainNotFound(final Throwable t) {
        return new ConfigurationException("Could not find a security domain configuration. Check if it is defined in WEB-INF/jboss-web.xml or set the picketlink.audit.securitydomain system property.", t);
    }
    
    @Override
    public ConfigurationException auditAuditManagerNotFound(final String location, final Throwable t) {
        return new ConfigurationException("Could not find a audit manager configuration. Location: " + location, t);
    }
    
    @Override
    public IssueInstantMissingException samlIssueInstantMissingError() {
        return new IssueInstantMissingException("PL00088: Null IssueInstant");
    }
    
    @Override
    public RuntimeException samlSPResponseNotCatalinaResponseError(final Object response) {
        return new RuntimeException("PL00026: Response was not of type catalina response. Received: " + response);
    }
    
    @Override
    public void samlLogoutError(final Throwable t) {
        this.logger.error((Object)"Error during the logout.", t);
    }
    
    @Override
    public void samlErrorPageForwardError(final String errorPage, final Throwable t) {
        this.logger.error((Object)("Error forwarding to the error page: " + errorPage));
    }
    
    @Override
    public void samlSPHandleRequestError(final Throwable t) {
        this.logger.error((Object)"Service Provider could not handle the request.", t);
    }
    
    @Override
    public IOException samlSPProcessingExceptionError(final Throwable t) {
        return new IOException("PL00032: Service Provider :: Server Exception", t);
    }
    
    @Override
    public IllegalArgumentException samlInvalidProtocolBinding() {
        return new IllegalArgumentException("Invalid SAML Protocol Binding. Expected POST or REDIRECT.");
    }
    
    @Override
    public IllegalStateException samlHandlerServiceProviderConfigNotFound() {
        return new IllegalStateException("Service Provider configuration not found. Check if the CONFIGURATION parameter is defined in the handler chain config.");
    }
    
    @Override
    public void samlSecurityTokenAlreadyPersisted(final String id) {
        this.warn("Security Token with id=" + id + " has already been persisted.");
    }
    
    @Override
    public void samlSecurityTokenNotFoundInRegistry(final String id) {
        this.warn("Security Token with id=" + id + " was not found in the registry.");
    }
    
    @Override
    public IllegalArgumentException samlMetaDataFailedToCreateCacheDuration(final String timeValue) {
        return new IllegalArgumentException("Cache duration could not be created using '" + timeValue + "'. This value must be an ISO-8601 period or a numeric value representing the duration in milliseconds.");
    }
    
    @Override
    public ConfigurationException samlMetaDataNoIdentityProviderDefined() {
        return new ConfigurationException("No configuration provided for the Identity Provider.");
    }
    
    @Override
    public ConfigurationException samlMetaDataNoServiceProviderDefined() {
        return new ConfigurationException("No configuration provided for the Service Provider.");
    }
    
    @Override
    public ConfigurationException securityDomainNotFound() {
        return new ConfigurationException("The security domain name could not be found. Check your jboss-web.xml.");
    }
    
    @Override
    public void authenticationManagerError(final ConfigurationException e) {
        this.error("Error loading the AuthenticationManager.", e);
    }
    
    private void error(final String msg, final ConfigurationException e) {
        this.logger.error((Object)msg, (Throwable)e);
    }
    
    @Override
    public void authorizationManagerError(final ConfigurationException e) {
        this.error("Error loading AuthorizationManager.", e);
    }
    
    @Override
    public IllegalStateException jbdcInitializationError(final Throwable throwable) {
        return new IllegalStateException(throwable);
    }
    
    @Override
    public RuntimeException errorUnmarshallingToken(final Throwable e) {
        return new RuntimeException(e);
    }
    
    @Override
    public RuntimeException runtimeException(final String msg, final Throwable e) {
        return new RuntimeException(msg, e);
    }
    
    @Override
    public IllegalStateException datasourceIsNull() {
        return new IllegalStateException();
    }
    
    @Override
    public IllegalArgumentException cannotParseParameterValue(final String parameter, final Throwable e) {
        return new IllegalArgumentException("Cannot parse: " + parameter, e);
    }
    
    @Override
    public RuntimeException cannotGetFreeClientPoolKey(final String key) {
        return new RuntimeException("Cannot get free client pool key: " + key);
    }
    
    @Override
    public RuntimeException cannotGetSTSConfigByKey(final String key) {
        return new RuntimeException("Cannot get STS config by key: " + key + ". The pool for given key has to be initialized first by calling STSClientPool.initialize method.");
    }
    
    @Override
    public RuntimeException cannotGetUsedClientsByKey(final String key) {
        return new RuntimeException("Cannot get used clients by key: " + key);
    }
    
    @Override
    public RuntimeException removingNonExistingClientFromUsedClientsByKey(final String key) {
        return new RuntimeException("removing non existing client from used clients by key: " + key);
    }
    
    @Override
    public RuntimeException freePoolAlreadyContainsGivenKey(final String key) {
        return new RuntimeException("Free pool already contains given key: " + key);
    }
    
    @Override
    public RuntimeException maximumNumberOfClientsReachedforPool(final String max) {
        return new RuntimeException("Pool reached miximum number of clients within the pool (" + max + ")");
    }
    
    @Override
    public RuntimeException cannotSetMaxPoolSizeToNegative(final String max) {
        return new RuntimeException("Cannot set maximum STS client pool size to negative number (" + max + ")");
    }
    
    @Override
    public RuntimeException parserFeatureNotSupported(final String feature) {
        return new RuntimeException("Parser feature " + feature + " not supported.");
    }
}
