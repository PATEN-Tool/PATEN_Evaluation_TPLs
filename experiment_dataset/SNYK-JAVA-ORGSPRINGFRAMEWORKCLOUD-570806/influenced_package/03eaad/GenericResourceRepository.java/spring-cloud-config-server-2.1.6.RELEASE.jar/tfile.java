// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.cloud.config.server.resource;

import org.apache.commons.logging.LogFactory;
import org.springframework.util.ResourceUtils;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Collection;
import java.util.Iterator;
import java.io.IOException;
import org.springframework.util.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.cloud.config.server.environment.SearchPathLocator;
import org.springframework.core.io.ResourceLoader;
import org.apache.commons.logging.Log;
import org.springframework.context.ResourceLoaderAware;

public class GenericResourceRepository implements ResourceRepository, ResourceLoaderAware
{
    private static final Log logger;
    private ResourceLoader resourceLoader;
    private SearchPathLocator service;
    
    public GenericResourceRepository(final SearchPathLocator service) {
        this.service = service;
    }
    
    public void setResourceLoader(final ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
    
    @Override
    public synchronized Resource findOne(final String application, final String profile, final String label, final String path) {
        if (StringUtils.hasText(path)) {
            final String[] locations = this.service.getLocations(application, profile, label).getLocations();
            try {
                int i = locations.length;
                while (i-- > 0) {
                    final String location = locations[i];
                    for (final String local : this.getProfilePaths(profile, path)) {
                        if (!this.isInvalidPath(local) && !this.isInvalidEncodedPath(local)) {
                            final Resource file = this.resourceLoader.getResource(location).createRelative(local);
                            if (file.exists() && file.isReadable()) {
                                return file;
                            }
                            continue;
                        }
                    }
                }
            }
            catch (IOException e) {
                throw new NoSuchResourceException("Error : " + path + ". (" + e.getMessage() + ")");
            }
        }
        throw new NoSuchResourceException("Not found: " + path);
    }
    
    private Collection<String> getProfilePaths(final String profiles, final String path) {
        final Set<String> paths = new LinkedHashSet<String>();
        for (final String profile : StringUtils.commaDelimitedListToSet(profiles)) {
            if (!StringUtils.hasText(profile) || "default".equals(profile)) {
                paths.add(path);
            }
            else {
                String ext = StringUtils.getFilenameExtension(path);
                String file = path;
                if (ext != null) {
                    ext = "." + ext;
                    file = StringUtils.stripFilenameExtension(path);
                }
                else {
                    ext = "";
                }
                paths.add(file + "-" + profile + ext);
            }
        }
        paths.add(path);
        return paths;
    }
    
    private boolean isInvalidEncodedPath(final String path) {
        if (path.contains("%")) {
            try {
                String decodedPath = URLDecoder.decode(path, "UTF-8");
                if (this.isInvalidPath(decodedPath)) {
                    return true;
                }
                decodedPath = this.processPath(decodedPath);
                if (this.isInvalidPath(decodedPath)) {
                    return true;
                }
            }
            catch (IllegalArgumentException ex) {}
            catch (UnsupportedEncodingException ex2) {}
        }
        return false;
    }
    
    protected String processPath(String path) {
        path = StringUtils.replace(path, "\\", "/");
        path = this.cleanDuplicateSlashes(path);
        return this.cleanLeadingSlash(path);
    }
    
    private String cleanDuplicateSlashes(final String path) {
        StringBuilder sb = null;
        char prev = '\0';
        for (int i = 0; i < path.length(); ++i) {
            final char curr = path.charAt(i);
            try {
                if (curr == '/' && prev == '/') {
                    if (sb == null) {
                        sb = new StringBuilder(path.substring(0, i));
                    }
                }
                else if (sb != null) {
                    sb.append(path.charAt(i));
                }
            }
            finally {
                prev = curr;
            }
        }
        return (sb != null) ? sb.toString() : path;
    }
    
    private String cleanLeadingSlash(final String path) {
        boolean slash = false;
        for (int i = 0; i < path.length(); ++i) {
            if (path.charAt(i) == '/') {
                slash = true;
            }
            else if (path.charAt(i) > ' ' && path.charAt(i) != '\u007f') {
                if (i == 0 || (i == 1 && slash)) {
                    return path;
                }
                return slash ? ("/" + path.substring(i)) : path.substring(i);
            }
        }
        return slash ? "/" : "";
    }
    
    protected boolean isInvalidPath(final String path) {
        if (path.contains("WEB-INF") || path.contains("META-INF")) {
            if (GenericResourceRepository.logger.isWarnEnabled()) {
                GenericResourceRepository.logger.warn((Object)("Path with \"WEB-INF\" or \"META-INF\": [" + path + "]"));
            }
            return true;
        }
        if (path.contains(":/")) {
            final String relativePath = (path.charAt(0) == '/') ? path.substring(1) : path;
            if (ResourceUtils.isUrl(relativePath) || relativePath.startsWith("url:")) {
                if (GenericResourceRepository.logger.isWarnEnabled()) {
                    GenericResourceRepository.logger.warn((Object)("Path represents URL or has \"url:\" prefix: [" + path + "]"));
                }
                return true;
            }
        }
        if (path.contains("..") && StringUtils.cleanPath(path).contains("../")) {
            if (GenericResourceRepository.logger.isWarnEnabled()) {
                GenericResourceRepository.logger.warn((Object)("Path contains \"../\" after call to StringUtils#cleanPath: [" + path + "]"));
            }
            return true;
        }
        return false;
    }
    
    static {
        logger = LogFactory.getLog((Class)GenericResourceRepository.class);
    }
}
