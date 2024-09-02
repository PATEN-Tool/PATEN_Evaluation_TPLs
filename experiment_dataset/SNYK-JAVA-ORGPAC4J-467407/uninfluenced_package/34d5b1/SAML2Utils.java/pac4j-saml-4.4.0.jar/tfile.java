// 
// Decompiled by Procyon v0.5.36
// 

package org.pac4j.saml.util;

import org.slf4j.LoggerFactory;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.resolver.ResolverException;
import org.pac4j.core.exception.TechnicalException;
import java.util.List;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import java.util.ArrayList;
import org.opensaml.saml.metadata.resolver.ChainingMetadataResolver;
import org.pac4j.saml.metadata.SAML2MetadataResolver;
import java.net.URISyntaxException;
import java.net.URI;
import org.pac4j.core.util.CommonHelper;
import org.slf4j.Logger;
import org.pac4j.core.context.HttpConstants;

public final class SAML2Utils implements HttpConstants
{
    private static final Logger logger;
    
    private SAML2Utils() {
    }
    
    public static String generateID() {
        return "_".concat(CommonHelper.randomString(39)).toLowerCase();
    }
    
    public static boolean urisEqualAfterPortNormalization(final URI uri1, final URI uri2) {
        if (uri1 == null && uri2 == null) {
            return true;
        }
        if (uri1 == null || uri2 == null) {
            return false;
        }
        try {
            final URI normalizedUri1 = normalizePortNumbersInUri(uri1);
            final URI normalizedUri2 = normalizePortNumbersInUri(uri2);
            final boolean eq = normalizedUri1.equals(normalizedUri2);
            return eq;
        }
        catch (URISyntaxException use) {
            SAML2Utils.logger.error("Cannot compare 2 URIs.", (Throwable)use);
            return false;
        }
    }
    
    private static URI normalizePortNumbersInUri(final URI uri) throws URISyntaxException {
        int port = uri.getPort();
        final String scheme = uri.getScheme();
        if ("http".equals(scheme) && port == 80) {
            port = -1;
        }
        if ("https".equals(scheme) && port == 443) {
            port = -1;
        }
        final URI result = new URI(scheme, uri.getUserInfo(), uri.getHost(), port, uri.getPath(), uri.getQuery(), uri.getFragment());
        return result;
    }
    
    public static ChainingMetadataResolver buildChainingMetadataResolver(final SAML2MetadataResolver idpMetadataProvider, final SAML2MetadataResolver spMetadataProvider) {
        final ChainingMetadataResolver metadataManager = new ChainingMetadataResolver();
        metadataManager.setId(ChainingMetadataResolver.class.getCanonicalName());
        try {
            final List<MetadataResolver> list = new ArrayList<MetadataResolver>();
            list.add(idpMetadataProvider.resolve());
            list.add(spMetadataProvider.resolve());
            metadataManager.setResolvers((List)list);
            metadataManager.initialize();
        }
        catch (ResolverException e) {
            throw new TechnicalException("Error adding idp or sp metadatas to manager", (Throwable)e);
        }
        catch (ComponentInitializationException e2) {
            throw new TechnicalException("Error initializing manager", (Throwable)e2);
        }
        return metadataManager;
    }
    
    static {
        logger = LoggerFactory.getLogger((Class)SAML2Utils.class);
    }
}
