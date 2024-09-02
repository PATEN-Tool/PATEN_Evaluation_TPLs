// 
// Decompiled by Procyon v0.5.36
// 

package org.keycloak.encoding;

import org.keycloak.provider.Provider;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.keycloak.common.Version;
import org.keycloak.platform.Platform;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import java.util.HashSet;
import java.io.File;
import java.util.Set;
import org.jboss.logging.Logger;

public class GzipResourceEncodingProviderFactory implements ResourceEncodingProviderFactory
{
    private static final Logger logger;
    private Set<String> excludedContentTypes;
    private File cacheDir;
    
    public GzipResourceEncodingProviderFactory() {
        this.excludedContentTypes = new HashSet<String>();
    }
    
    public ResourceEncodingProvider create(final KeycloakSession session) {
        if (this.cacheDir == null) {
            this.cacheDir = this.initCacheDir();
        }
        return new GzipResourceEncodingProvider(session, this.cacheDir);
    }
    
    @Override
    public void init(final Config.Scope config) {
        final String e = config.get("excludedContentTypes", "image/png image/jpeg");
        for (final String s : e.split(" ")) {
            this.excludedContentTypes.add(s);
        }
    }
    
    @Override
    public boolean encodeContentType(final String contentType) {
        return !this.excludedContentTypes.contains(contentType);
    }
    
    public String getId() {
        return "gzip";
    }
    
    private synchronized File initCacheDir() {
        if (this.cacheDir != null) {
            return this.cacheDir;
        }
        final File cacheRoot = new File(Platform.getPlatform().getTmpDirectory(), "kc-gzip-cache");
        final File cacheDir = new File(cacheRoot, Version.RESOURCES_VERSION);
        if (cacheRoot.isDirectory()) {
            for (final File f : cacheRoot.listFiles()) {
                if (!f.getName().equals(Version.RESOURCES_VERSION)) {
                    try {
                        FileUtils.deleteDirectory(f);
                    }
                    catch (IOException e) {
                        GzipResourceEncodingProviderFactory.logger.warn((Object)"Failed to delete old gzip cache directory", (Throwable)e);
                    }
                }
            }
        }
        if (!cacheDir.isDirectory() && !cacheDir.mkdirs()) {
            GzipResourceEncodingProviderFactory.logger.warn((Object)"Failed to create gzip cache directory");
            return null;
        }
        return cacheDir;
    }
    
    static {
        logger = Logger.getLogger((Class)GzipResourceEncodingProviderFactory.class);
    }
}
