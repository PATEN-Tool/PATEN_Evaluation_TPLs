// 
// Decompiled by Procyon v0.5.36
// 

package edu.vt.middleware.ldap.ssl;

import java.security.GeneralSecurityException;
import javax.net.SocketFactory;
import java.util.StringTokenizer;
import java.util.Iterator;
import java.util.Collection;
import java.security.cert.CertificateParsingException;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import edu.vt.middleware.ldap.LdapUtil;
import javax.net.ssl.SSLPeerUnverifiedException;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLSession;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import javax.net.ssl.HostnameVerifier;

public class DefaultHostnameVerifier implements HostnameVerifier, CertificateHostnameVerifier
{
    protected final Log logger;
    
    public DefaultHostnameVerifier() {
        this.logger = LogFactory.getLog((Class)this.getClass());
    }
    
    public boolean verify(final String hostname, final SSLSession session) {
        boolean b = false;
        try {
            String name = null;
            if (hostname != null) {
                if (hostname.startsWith("[") && hostname.endsWith("]")) {
                    name = hostname.substring(1, hostname.length() - 1).trim();
                }
                else {
                    name = hostname.trim();
                }
            }
            b = this.verify(name, (X509Certificate)session.getPeerCertificates()[0]);
        }
        catch (SSLPeerUnverifiedException e) {
            if (this.logger.isWarnEnabled()) {
                this.logger.warn((Object)"Could not get certificate from the SSL session", (Throwable)e);
            }
        }
        return b;
    }
    
    public boolean verify(final String hostname, final X509Certificate cert) {
        if (this.logger.isDebugEnabled()) {
            this.logger.debug((Object)"Verify with the following parameters:");
            this.logger.debug((Object)("  hostname = " + hostname));
            this.logger.debug((Object)("  cert = " + cert.getSubjectX500Principal().toString()));
        }
        boolean b = false;
        if (LdapUtil.isIPAddress(hostname)) {
            b = this.verifyIP(hostname, cert);
        }
        else {
            b = this.verifyDNS(hostname, cert);
        }
        return b;
    }
    
    protected boolean verifyIP(final String ip, final X509Certificate cert) {
        final String[] subjAltNames = this.getSubjectAltNames(cert, SubjectAltNameType.IP_ADDRESS);
        if (this.logger.isDebugEnabled()) {
            this.logger.debug((Object)("verifyIP using subjectAltNames = " + Arrays.toString(subjAltNames)));
        }
        for (final String name : subjAltNames) {
            if (ip.equalsIgnoreCase(name)) {
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug((Object)("verifyIP found hostname match: " + name));
                }
                return true;
            }
        }
        return false;
    }
    
    protected boolean verifyDNS(final String hostname, final X509Certificate cert) {
        boolean verified = false;
        final String[] subjAltNames = this.getSubjectAltNames(cert, SubjectAltNameType.DNS_NAME);
        if (this.logger.isDebugEnabled()) {
            this.logger.debug((Object)("verifyDNS using subjectAltNames = " + Arrays.toString(subjAltNames)));
        }
        if (subjAltNames.length > 0) {
            for (final String name : subjAltNames) {
                if (this.isMatch(hostname, name)) {
                    if (this.logger.isDebugEnabled()) {
                        this.logger.debug((Object)("verifyDNS found hostname match: " + name));
                    }
                    verified = true;
                    break;
                }
            }
        }
        else {
            final String[] cns = this.getCNs(cert);
            if (this.logger.isDebugEnabled()) {
                this.logger.debug((Object)("verifyDNS using CN = " + Arrays.toString(cns)));
            }
            if (cns.length > 0 && this.isMatch(hostname, cns[0])) {
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug((Object)("verifyDNS found hostname match: " + cns[0]));
                }
                verified = true;
            }
        }
        return verified;
    }
    
    private String[] getSubjectAltNames(final X509Certificate cert, final SubjectAltNameType type) {
        final List<String> names = new ArrayList<String>();
        try {
            final Collection<List<?>> subjAltNames = cert.getSubjectAlternativeNames();
            if (subjAltNames != null) {
                for (final List<?> generalName : subjAltNames) {
                    final Integer nameType = (Integer)generalName.get(0);
                    if (nameType == type.ordinal()) {
                        names.add((String)generalName.get(1));
                    }
                }
            }
        }
        catch (CertificateParsingException e) {
            if (this.logger.isWarnEnabled()) {
                this.logger.warn((Object)"Error reading subject alt names from certificate", (Throwable)e);
            }
        }
        return names.toArray(new String[names.size()]);
    }
    
    private String[] getCNs(final X509Certificate cert) {
        final List<String> names = new ArrayList<String>();
        final String subjectPrincipal = cert.getSubjectX500Principal().toString();
        final StringTokenizer st = new StringTokenizer(subjectPrincipal, ",");
        while (st.hasMoreTokens()) {
            final String tok = st.nextToken();
            final int x = tok.indexOf("CN=");
            if (x >= 0) {
                names.add(tok.substring(x + "CN=".length()));
            }
        }
        return names.toArray(new String[names.size()]);
    }
    
    private boolean isMatch(final String hostname, final String certName) {
        final boolean isWildcard = certName.startsWith("*.") && certName.indexOf(46) < certName.lastIndexOf(46);
        boolean match = false;
        if (isWildcard) {
            final String certNameDomain = certName.substring(certName.indexOf("."));
            final int hostnameIdx = (hostname.indexOf(".") != -1) ? hostname.indexOf(".") : hostname.length();
            final String hostnameDomain = hostname.substring(hostnameIdx);
            match = certNameDomain.equalsIgnoreCase(hostnameDomain);
        }
        else {
            match = certName.equalsIgnoreCase(hostname);
        }
        return match;
    }
    
    private enum SubjectAltNameType
    {
        OTHER_NAME, 
        RFC822_NAME, 
        DNS_NAME, 
        X400_ADDRESS, 
        DIRECTORY_NAME, 
        EDI_PARTY_NAME, 
        UNIFORM_RESOURCE_IDENTIFIER, 
        IP_ADDRESS, 
        REGISTERED_ID;
    }
    
    public static class SSLSocketFactory extends TLSSocketFactory
    {
        public SSLSocketFactory() {
            this.setHostnameVerifier(new DefaultHostnameVerifier());
        }
        
        public static SocketFactory getDefault() {
            final SSLSocketFactory sf = new SSLSocketFactory();
            try {
                sf.initialize();
            }
            catch (GeneralSecurityException e) {
                final Log logger = LogFactory.getLog((Class)TLSSocketFactory.class);
                if (logger.isErrorEnabled()) {
                    logger.error((Object)"Error initializing socket factory", (Throwable)e);
                }
            }
            return sf;
        }
    }
}
