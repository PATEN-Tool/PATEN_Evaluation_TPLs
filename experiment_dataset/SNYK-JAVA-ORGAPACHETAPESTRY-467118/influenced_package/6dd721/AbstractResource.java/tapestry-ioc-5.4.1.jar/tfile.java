// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.tapestry5.ioc.internal.util;

import java.net.URISyntaxException;
import java.io.File;
import java.net.URL;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.tapestry5.ioc.util.LocalizedNameGenerator;
import java.util.Locale;
import java.util.Iterator;
import java.util.List;
import org.apache.tapestry5.ioc.Resource;

public abstract class AbstractResource extends LockSupport implements Resource
{
    private final String path;
    private boolean exists;
    private boolean existsComputed;
    private Localization firstLocalization;
    
    protected AbstractResource(final String path) {
        assert path != null;
        this.path = (path.startsWith("/") ? path.substring(1) : path);
    }
    
    public final String getPath() {
        return this.path;
    }
    
    public final String getFile() {
        return extractFile(this.path);
    }
    
    private static String extractFile(final String path) {
        final int slashx = path.lastIndexOf(47);
        return path.substring(slashx + 1);
    }
    
    public final String getFolder() {
        final int slashx = this.path.lastIndexOf(47);
        return (slashx < 0) ? "" : this.path.substring(0, slashx);
    }
    
    public final Resource forFile(final String relativePath) {
        assert relativePath != null;
        final List<String> terms = (List<String>)CollectionFactory.newList();
        for (final String term : this.getFolder().split("/")) {
            terms.add(term);
        }
        for (final String term : relativePath.split("/")) {
            if (!term.equals("")) {
                if (!term.equals(".")) {
                    if (term.equals("..")) {
                        if (terms.isEmpty()) {
                            throw new IllegalStateException(String.format("Relative path '%s' for %s would go above root.", relativePath, this));
                        }
                        terms.remove(terms.size() - 1);
                    }
                    else {
                        terms.add(term);
                    }
                }
            }
        }
        final StringBuilder path = new StringBuilder(100);
        String sep = "";
        for (final String term : terms) {
            path.append(sep).append(term);
            sep = "/";
        }
        return this.createResource(path.toString());
    }
    
    public final Resource forLocale(final Locale locale) {
        try {
            this.acquireReadLock();
            for (Localization l = this.firstLocalization; l != null; l = l.next) {
                if (l.locale.equals(locale)) {
                    return l.resource;
                }
            }
            return this.populateLocalizationCache(locale);
        }
        finally {
            this.releaseReadLock();
        }
    }
    
    private Resource populateLocalizationCache(final Locale locale) {
        try {
            this.upgradeReadLockToWriteLock();
            for (Localization l = this.firstLocalization; l != null; l = l.next) {
                if (l.locale.equals(locale)) {
                    return l.resource;
                }
            }
            final Resource result = this.findLocalizedResource(locale);
            this.firstLocalization = new Localization(locale, result, this.firstLocalization);
            return result;
        }
        finally {
            this.downgradeWriteLockToReadLock();
        }
    }
    
    private Resource findLocalizedResource(final Locale locale) {
        for (final String path : new LocalizedNameGenerator(this.path, locale)) {
            final Resource potential = this.createResource(path);
            if (potential.exists()) {
                return potential;
            }
        }
        return null;
    }
    
    public final Resource withExtension(final String extension) {
        assert InternalUtils.isNonBlank(extension);
        final int dotx = this.path.lastIndexOf(46);
        if (dotx < 0) {
            return this.createResource(this.path + "." + extension);
        }
        return this.createResource(this.path.substring(0, dotx + 1) + extension);
    }
    
    private Resource createResource(final String path) {
        if (this.path.equals(path)) {
            return (Resource)this;
        }
        return this.newResource(path);
    }
    
    public boolean exists() {
        try {
            this.acquireReadLock();
            if (!this.existsComputed) {
                this.computeExists();
            }
            return this.exists;
        }
        finally {
            this.releaseReadLock();
        }
    }
    
    private void computeExists() {
        try {
            this.upgradeReadLockToWriteLock();
            if (!this.existsComputed) {
                this.exists = (this.toURL() != null);
                this.existsComputed = true;
            }
        }
        finally {
            this.downgradeWriteLockToReadLock();
        }
    }
    
    public InputStream openStream() throws IOException {
        final URL url = this.toURL();
        if (url == null) {
            return null;
        }
        if ("jar".equals(url.getProtocol())) {
            final String urlAsString = url.toString();
            final int indexOfExclamationMark = urlAsString.indexOf(33);
            final String resourceInJar = urlAsString.substring(indexOfExclamationMark + 2);
            final URL directoryResource = Thread.currentThread().getContextClassLoader().getResource(resourceInJar + "/");
            final boolean isDirectory = directoryResource != null && "jar".equals(directoryResource.getProtocol());
            if (isDirectory) {
                throw new IOException("Cannot open a stream for a resource that references a directory inside a JAR file (" + url + ").");
            }
        }
        return new BufferedInputStream(url.openStream());
    }
    
    protected abstract Resource newResource(final String p0);
    
    protected void validateURL(final URL url) {
        if (url == null) {
            return;
        }
        if (!url.getProtocol().equals("file")) {
            return;
        }
        final File file = this.toFile(url);
        String expectedFileName = null;
        try {
            final String sep = System.getProperty("file.separator");
            expectedFileName = extractFile(file.getCanonicalPath().replace(sep, "/"));
        }
        catch (IOException e) {
            return;
        }
        final String actualFileName = this.getFile();
        if (actualFileName.equals(expectedFileName)) {
            return;
        }
        throw new IllegalStateException(String.format("Resource %s does not match the case of the actual file name, '%s'.", this, expectedFileName));
    }
    
    private File toFile(final URL url) {
        try {
            return new File(url.toURI());
        }
        catch (URISyntaxException ex) {
            return new File(url.getPath());
        }
    }
    
    public boolean isVirtual() {
        return false;
    }
    
    private static class Localization
    {
        final Locale locale;
        final Resource resource;
        final Localization next;
        
        private Localization(final Locale locale, final Resource resource, final Localization next) {
            this.locale = locale;
            this.resource = resource;
            this.next = next;
        }
    }
}
