// 
// Decompiled by Procyon v0.5.36
// 

package org.keycloak.authentication.authenticators.x509;

import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.keycloak.crypto.HashException;
import java.security.cert.CertificateEncodingException;
import org.keycloak.jose.jws.crypto.HashUtils;
import org.bouncycastle.util.encoders.Hex;
import org.bouncycastle.asn1.x500.X500Name;
import java.util.function.Function;
import org.keycloak.models.UserModel;
import org.keycloak.models.RealmModel;
import java.security.GeneralSecurityException;
import org.keycloak.services.x509.X509ClientCertificateLookup;
import java.security.cert.X509Certificate;
import org.keycloak.models.KeycloakSession;
import org.keycloak.forms.login.LoginFormsProvider;
import javax.ws.rs.core.Response;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.services.ServicesLogger;
import org.keycloak.authentication.Authenticator;

public abstract class AbstractX509ClientCertificateAuthenticator implements Authenticator
{
    public static final String DEFAULT_ATTRIBUTE_NAME = "usercertificate";
    protected static ServicesLogger logger;
    public static final String REGULAR_EXPRESSION = "x509-cert-auth.regular-expression";
    public static final String ENABLE_CRL = "x509-cert-auth.crl-checking-enabled";
    public static final String ENABLE_OCSP = "x509-cert-auth.ocsp-checking-enabled";
    public static final String ENABLE_CRLDP = "x509-cert-auth.crldp-checking-enabled";
    public static final String CANONICAL_DN = "x509-cert-auth.canonical-dn-enabled";
    public static final String SERIALNUMBER_HEX = "x509-cert-auth.serialnumber-hex-enabled";
    public static final String CRL_RELATIVE_PATH = "x509-cert-auth.crl-relative-path";
    public static final String OCSPRESPONDER_URI = "x509-cert-auth.ocsp-responder-uri";
    public static final String OCSPRESPONDER_CERTIFICATE = "x509-cert-auth.ocsp-responder-certificate";
    public static final String MAPPING_SOURCE_SELECTION = "x509-cert-auth.mapping-source-selection";
    public static final String MAPPING_SOURCE_CERT_SUBJECTDN = "Match SubjectDN using regular expression";
    public static final String MAPPING_SOURCE_CERT_SUBJECTDN_EMAIL = "Subject's e-mail";
    public static final String MAPPING_SOURCE_CERT_SUBJECTALTNAME_EMAIL = "Subject's Alternative Name E-mail";
    public static final String MAPPING_SOURCE_CERT_SUBJECTALTNAME_OTHERNAME = "Subject's Alternative Name otherName (UPN)";
    public static final String MAPPING_SOURCE_CERT_SUBJECTDN_CN = "Subject's Common Name";
    public static final String MAPPING_SOURCE_CERT_ISSUERDN = "Match IssuerDN using regular expression";
    public static final String MAPPING_SOURCE_CERT_SERIALNUMBER = "Certificate Serial Number";
    public static final String MAPPING_SOURCE_CERT_SHA256_THUMBPRINT = "SHA-256 Thumbprint";
    public static final String MAPPING_SOURCE_CERT_SERIALNUMBER_ISSUERDN = "Certificate Serial Number and IssuerDN";
    public static final String MAPPING_SOURCE_CERT_CERTIFICATE_PEM = "Full Certificate in PEM format";
    public static final String USER_MAPPER_SELECTION = "x509-cert-auth.mapper-selection";
    public static final String USER_ATTRIBUTE_MAPPER = "Custom Attribute Mapper";
    public static final String USERNAME_EMAIL_MAPPER = "Username or Email";
    public static final String CUSTOM_ATTRIBUTE_NAME = "x509-cert-auth.mapper-selection.user-attribute-name";
    public static final String CERTIFICATE_KEY_USAGE = "x509-cert-auth.keyusage";
    public static final String CERTIFICATE_EXTENDED_KEY_USAGE = "x509-cert-auth.extendedkeyusage";
    static final String DEFAULT_MATCH_ALL_EXPRESSION = "(.*?)(?:$)";
    public static final String CONFIRMATION_PAGE_DISALLOWED = "x509-cert-auth.confirmation-page-disallowed";
    
    protected Response createInfoResponse(final AuthenticationFlowContext context, final String infoMessage, final Object... parameters) {
        final LoginFormsProvider form = context.form();
        return form.setInfo(infoMessage, parameters).createInfoPage();
    }
    
    public CertificateValidator.CertificateValidatorBuilder certificateValidationParameters(final KeycloakSession session, final X509AuthenticatorConfigModel config) throws Exception {
        return CertificateValidatorConfigBuilder.fromConfig(session, config);
    }
    
    public void close() {
    }
    
    protected X509Certificate[] getCertificateChain(final AuthenticationFlowContext context) {
        try {
            final X509ClientCertificateLookup provider = (X509ClientCertificateLookup)context.getSession().getProvider((Class)X509ClientCertificateLookup.class);
            if (provider == null) {
                AbstractX509ClientCertificateAuthenticator.logger.errorv("\"{0}\" Spi is not available, did you forget to update the configuration?", (Object)X509ClientCertificateLookup.class);
                return null;
            }
            final X509Certificate[] certs = provider.getCertificateChain(context.getHttpRequest());
            if (certs != null) {
                for (final X509Certificate cert : certs) {
                    AbstractX509ClientCertificateAuthenticator.logger.tracev("\"{0}\"", (Object)cert.getSubjectDN().getName());
                }
            }
            return certs;
        }
        catch (GeneralSecurityException e) {
            AbstractX509ClientCertificateAuthenticator.logger.error((Object)e.getMessage(), (Throwable)e);
            return null;
        }
    }
    
    protected void saveX509CertificateAuditDataToAuthSession(final AuthenticationFlowContext context, final X509Certificate cert) {
        context.getAuthenticationSession().setAuthNote("x509_cert_serial_number", cert.getSerialNumber().toString());
        context.getAuthenticationSession().setAuthNote("x509_cert_subject_distinguished_name", cert.getSubjectDN().toString());
        context.getAuthenticationSession().setAuthNote("x509_cert_issuer_distinguished_name", cert.getIssuerDN().toString());
    }
    
    protected void recordX509CertificateAuditDataViaContextEvent(final AuthenticationFlowContext context) {
        this.recordX509DetailFromAuthSessionToEvent(context, "x509_cert_serial_number");
        this.recordX509DetailFromAuthSessionToEvent(context, "x509_cert_subject_distinguished_name");
        this.recordX509DetailFromAuthSessionToEvent(context, "x509_cert_issuer_distinguished_name");
    }
    
    private void recordX509DetailFromAuthSessionToEvent(final AuthenticationFlowContext context, final String detailName) {
        final String detailValue = context.getAuthenticationSession().getAuthNote(detailName);
        context.getEvent().detail(detailName, detailValue);
    }
    
    public UserIdentityExtractor getUserIdentityExtractor(final X509AuthenticatorConfigModel config) {
        return UserIdentityExtractorBuilder.fromConfig(config);
    }
    
    public UserIdentityToModelMapper getUserIdentityToModelMapper(final X509AuthenticatorConfigModel config) {
        return UserIdentityToModelMapperBuilder.fromConfig(config);
    }
    
    public boolean requiresUser() {
        return false;
    }
    
    public boolean configuredFor(final KeycloakSession session, final RealmModel realm, final UserModel user) {
        return true;
    }
    
    public void setRequiredActions(final KeycloakSession session, final RealmModel realm, final UserModel user) {
    }
    
    static {
        AbstractX509ClientCertificateAuthenticator.logger = ServicesLogger.LOGGER;
    }
    
    protected static class CertificateValidatorConfigBuilder
    {
        static CertificateValidator.CertificateValidatorBuilder fromConfig(final KeycloakSession session, final X509AuthenticatorConfigModel config) throws Exception {
            final CertificateValidator.CertificateValidatorBuilder builder = new CertificateValidator.CertificateValidatorBuilder();
            return builder.session(session).keyUsage().parse(config.getKeyUsage()).extendedKeyUsage().parse(config.getExtendedKeyUsage()).revocation().cRLEnabled(config.getCRLEnabled()).cRLDPEnabled(config.getCRLDistributionPointEnabled()).cRLrelativePath(config.getCRLRelativePath()).oCSPEnabled(config.getOCSPEnabled()).oCSPResponseCertificate(config.getOCSPResponderCertificate()).oCSPResponderURI(config.getOCSPResponder());
        }
    }
    
    protected static class UserIdentityExtractorBuilder
    {
        private static final Function<X509Certificate[], X500Name> subject;
        private static final Function<X509Certificate[], X500Name> issuer;
        
        private static final Function<X509Certificate[], String> getSerialnumberFunc(final X509AuthenticatorConfigModel config) {
            return (Function<X509Certificate[], String>)(config.isSerialnumberHex() ? (certs -> Hex.toHexString(certs[0].getSerialNumber().toByteArray())) : (certs -> certs[0].getSerialNumber().toString()));
        }
        
        private static final Function<X509Certificate[], String> getIssuerDNFunc(final X509AuthenticatorConfigModel config) {
            return (Function<X509Certificate[], String>)(config.isCanonicalDnEnabled() ? (certs -> certs[0].getIssuerX500Principal().getName("CANONICAL")) : (certs -> certs[0].getIssuerDN().getName()));
        }
        
        static UserIdentityExtractor fromConfig(final X509AuthenticatorConfigModel config) {
            final X509AuthenticatorConfigModel.MappingSourceType userIdentitySource = config.getMappingSourceType();
            final String pattern = config.getRegularExpression();
            UserIdentityExtractor extractor = null;
            Function<X509Certificate[], String> func = null;
            switch (userIdentitySource) {
                case SUBJECTDN: {
                    func = (Function<X509Certificate[], String>)(config.isCanonicalDnEnabled() ? (certs -> certs[0].getSubjectX500Principal().getName("CANONICAL")) : (certs -> certs[0].getSubjectDN().getName()));
                    extractor = UserIdentityExtractor.getPatternIdentityExtractor(pattern, func);
                    break;
                }
                case ISSUERDN: {
                    extractor = UserIdentityExtractor.getPatternIdentityExtractor(pattern, getIssuerDNFunc(config));
                    break;
                }
                case SERIALNUMBER: {
                    extractor = UserIdentityExtractor.getPatternIdentityExtractor("(.*?)(?:$)", getSerialnumberFunc(config));
                    break;
                }
                case SHA256_THUMBPRINT: {
                    final Exception ex;
                    Exception e;
                    extractor = UserIdentityExtractor.getPatternIdentityExtractor("(.*?)(?:$)", certs -> {
                        try {
                            return Hex.toHexString(HashUtils.hash("SHA-256", certs[0].getEncoded()));
                        }
                        catch (CertificateEncodingException | HashException ex2) {
                            e = ex;
                            AbstractX509ClientCertificateAuthenticator.logger.warn((Object)"Unable to get certificate's thumbprint", (Throwable)e);
                            return null;
                        }
                    });
                    break;
                }
                case SERIALNUMBER_ISSUERDN: {
                    func = (Function<X509Certificate[], String>)(certs -> getSerialnumberFunc(config).apply(certs) + "##" + getIssuerDNFunc(config).apply(certs));
                    extractor = UserIdentityExtractor.getPatternIdentityExtractor("(.*?)(?:$)", func);
                    break;
                }
                case SUBJECTDN_CN: {
                    extractor = UserIdentityExtractor.getX500NameExtractor(BCStyle.CN, UserIdentityExtractorBuilder.subject);
                    break;
                }
                case SUBJECTDN_EMAIL: {
                    extractor = UserIdentityExtractor.either(UserIdentityExtractor.getX500NameExtractor(BCStyle.EmailAddress, UserIdentityExtractorBuilder.subject)).or(UserIdentityExtractor.getX500NameExtractor(BCStyle.E, UserIdentityExtractorBuilder.subject));
                    break;
                }
                case SUBJECTALTNAME_EMAIL: {
                    extractor = UserIdentityExtractor.getSubjectAltNameExtractor(1);
                    break;
                }
                case SUBJECTALTNAME_OTHERNAME: {
                    extractor = UserIdentityExtractor.getSubjectAltNameExtractor(0);
                    break;
                }
                case CERTIFICATE_PEM: {
                    extractor = UserIdentityExtractor.getCertificatePemIdentityExtractor(config);
                    break;
                }
                default: {
                    AbstractX509ClientCertificateAuthenticator.logger.warnf("[UserIdentityExtractorBuilder:fromConfig] Unknown or unsupported user identity source: \"%s\"", (Object)userIdentitySource.getName());
                    break;
                }
            }
            return extractor;
        }
        
        static {
            subject = (certs -> {
                try {
                    return new JcaX509CertificateHolder(certs[0]).getSubject();
                }
                catch (CertificateEncodingException e) {
                    AbstractX509ClientCertificateAuthenticator.logger.warn((Object)"Unable to get certificate Subject", (Throwable)e);
                    return null;
                }
            });
            issuer = (certs -> {
                try {
                    return new JcaX509CertificateHolder(certs[0]).getIssuer();
                }
                catch (CertificateEncodingException e2) {
                    AbstractX509ClientCertificateAuthenticator.logger.warn((Object)"Unable to get certificate Issuer", (Throwable)e2);
                    return null;
                }
            });
        }
    }
    
    protected static class UserIdentityToModelMapperBuilder
    {
        static UserIdentityToModelMapper fromConfig(final X509AuthenticatorConfigModel config) {
            final X509AuthenticatorConfigModel.IdentityMapperType mapperType = config.getUserIdentityMapperType();
            final String attributeName = config.getCustomAttributeName();
            UserIdentityToModelMapper mapper = null;
            switch (mapperType) {
                case USER_ATTRIBUTE: {
                    mapper = UserIdentityToModelMapper.getUserIdentityToCustomAttributeMapper(attributeName);
                    break;
                }
                case USERNAME_EMAIL: {
                    mapper = UserIdentityToModelMapper.getUsernameOrEmailMapper();
                    break;
                }
                default: {
                    AbstractX509ClientCertificateAuthenticator.logger.warnf("[UserIdentityToModelMapperBuilder:fromConfig] Unknown or unsupported user identity mapper: \"%s\"", (Object)mapperType.getName());
                    break;
                }
            }
            return mapper;
        }
    }
}
