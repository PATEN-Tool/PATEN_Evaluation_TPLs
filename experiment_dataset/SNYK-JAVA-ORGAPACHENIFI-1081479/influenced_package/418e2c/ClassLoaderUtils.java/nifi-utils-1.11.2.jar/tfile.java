// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.nifi.util.file.classloader;

import org.slf4j.LoggerFactory;
import java.util.stream.Stream;
import java.net.URLClassLoader;
import java.net.URISyntaxException;
import java.net.URI;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import javax.xml.bind.DatatypeConverter;
import java.security.MessageDigest;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.Iterator;
import java.util.List;
import java.io.File;
import java.util.LinkedList;
import java.util.function.Function;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Collections;
import java.net.MalformedURLException;
import java.net.URL;
import java.io.FilenameFilter;
import org.slf4j.Logger;

public class ClassLoaderUtils
{
    static final Logger LOGGER;
    
    public static ClassLoader getCustomClassLoader(final String modulePath, final ClassLoader parentClassLoader, final FilenameFilter filenameFilter) throws MalformedURLException {
        final URL[] classpaths = getURLsForClasspath(modulePath, filenameFilter, false);
        return createModuleClassLoader(classpaths, parentClassLoader);
    }
    
    public static URL[] getURLsForClasspath(final String modulePath, final FilenameFilter filenameFilter, final boolean suppressExceptions) throws MalformedURLException {
        return getURLsForClasspath((modulePath == null) ? Collections.emptySet() : Collections.singleton(modulePath), filenameFilter, suppressExceptions);
    }
    
    public static URL[] getURLsForClasspath(final Set<String> modulePaths, final FilenameFilter filenameFilter, final boolean suppressExceptions) throws MalformedURLException {
        final Set<String> modules = new LinkedHashSet<String>();
        if (modulePaths != null) {
            modulePaths.stream().flatMap(path -> Arrays.stream(path.split(","))).filter(path -> isNotBlank(path)).map((Function<? super Object, ?>)String::trim).forEach(m -> modules.add(m));
        }
        return toURLs(modules, filenameFilter, suppressExceptions);
    }
    
    private static boolean isNotBlank(final String value) {
        return value != null && !value.trim().isEmpty();
    }
    
    protected static URL[] toURLs(final Set<String> modulePaths, final FilenameFilter filenameFilter, final boolean suppressExceptions) throws MalformedURLException {
        final List<URL> additionalClasspath = new LinkedList<URL>();
        if (modulePaths != null) {
            for (final String modulePathString : modulePaths) {
                boolean isUrl = true;
                try {
                    additionalClasspath.add(new URL(modulePathString));
                }
                catch (MalformedURLException mue) {
                    isUrl = false;
                }
                if (!isUrl) {
                    try {
                        final File modulePath = new File(modulePathString);
                        if (!modulePath.exists()) {
                            throw new MalformedURLException("Path specified does not exist");
                        }
                        additionalClasspath.add(modulePath.toURI().toURL());
                        if (!modulePath.isDirectory()) {
                            continue;
                        }
                        final File[] files = modulePath.listFiles(filenameFilter);
                        if (files == null) {
                            continue;
                        }
                        for (final File classpathResource : files) {
                            if (classpathResource.isDirectory()) {
                                ClassLoaderUtils.LOGGER.warn("Recursive directories are not supported, skipping " + classpathResource.getAbsolutePath());
                            }
                            else {
                                additionalClasspath.add(classpathResource.toURI().toURL());
                            }
                        }
                    }
                    catch (MalformedURLException e) {
                        if (!suppressExceptions) {
                            throw e;
                        }
                        continue;
                    }
                }
            }
        }
        return additionalClasspath.toArray(new URL[additionalClasspath.size()]);
    }
    
    public static String generateAdditionalUrlsFingerprint(final Set<URL> urls) {
        final List<String> listOfUrls = urls.stream().map((Function<? super Object, ?>)Object::toString).collect((Collector<? super Object, ?, List<String>>)Collectors.toList());
        final StringBuffer urlBuffer = new StringBuffer();
        Collections.sort(listOfUrls);
        try {
            final MessageDigest md = MessageDigest.getInstance("MD5");
            listOfUrls.forEach(url -> urlBuffer.append(url).append("-").append(getLastModified(url)).append(";"));
            final byte[] bytesOfAdditionalUrls = urlBuffer.toString().getBytes("UTF-8");
            final byte[] bytesOfDigest = md.digest(bytesOfAdditionalUrls);
            return DatatypeConverter.printHexBinary(bytesOfDigest);
        }
        catch (NoSuchAlgorithmException | UnsupportedEncodingException ex2) {
            final Exception ex;
            final Exception e = ex;
            ClassLoaderUtils.LOGGER.error("Unable to generate fingerprint for the provided additional resources {}", new Object[] { urls, e });
            return null;
        }
    }
    
    private static long getLastModified(final String url) {
        File file = null;
        try {
            file = new File(new URI(url));
        }
        catch (URISyntaxException e) {
            ClassLoaderUtils.LOGGER.error("Error getting last modified date for " + url);
        }
        return (file != null) ? file.lastModified() : 0L;
    }
    
    protected static ClassLoader createModuleClassLoader(final URL[] modules, final ClassLoader parentClassLoader) {
        return new URLClassLoader(modules, parentClassLoader);
    }
    
    static {
        LOGGER = LoggerFactory.getLogger((Class)ClassLoaderUtils.class);
    }
}
