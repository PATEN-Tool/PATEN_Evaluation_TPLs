// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.cloud.config.server.resource;

import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Collection;
import java.util.Iterator;
import java.io.IOException;
import org.springframework.cloud.config.server.support.PathUtils;
import org.springframework.util.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.cloud.config.server.environment.SearchPathLocator;
import org.springframework.core.io.ResourceLoader;
import org.springframework.context.ResourceLoaderAware;

public class GenericResourceRepository implements ResourceRepository, ResourceLoaderAware
{
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
                    if (PathUtils.isInvalidEncodedLocation(location)) {
                        continue;
                    }
                    for (final String local : this.getProfilePaths(profile, path)) {
                        if (!PathUtils.isInvalidPath(local) && !PathUtils.isInvalidEncodedPath(local)) {
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
}
