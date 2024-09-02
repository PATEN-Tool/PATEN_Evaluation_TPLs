// 
// Decompiled by Procyon v0.5.36
// 

package org.netbeans.html.presenters.webkit;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.nio.file.Files;
import java.nio.file.CopyOption;
import java.util.zip.ZipEntry;
import java.util.jar.JarEntry;
import java.io.File;
import java.net.JarURLConnection;
import java.net.URL;

final class UnJarResources
{
    static URL extract(final URL url) throws IOException {
        if (!"jar".equals(url.getProtocol())) {
            return url;
        }
        final JarURLConnection jarConnection = (JarURLConnection)url.openConnection();
        final JarFile jar = jarConnection.getJarFile();
        if (jar == null) {
            return url;
        }
        final File dir = File.createTempFile(jar.getName(), ".dir");
        dir.delete();
        dir.mkdirs();
        final Enumeration<JarEntry> en = jar.entries();
        while (en.hasMoreElements()) {
            final JarEntry entry = en.nextElement();
            final String entryName = entry.getName();
            if (!entryName.endsWith(".class")) {
                if (entryName.endsWith("/")) {
                    continue;
                }
                final File file = new File(dir, entryName.replace('/', File.separatorChar));
                file.getParentFile().mkdirs();
                try (final InputStream is = jar.getInputStream(entry)) {
                    Files.copy(is, file.toPath(), new CopyOption[0]);
                }
            }
        }
        final File file2 = new File(dir, jarConnection.getEntryName().replace('/', File.separatorChar));
        if (file2.exists()) {
            return file2.toURI().toURL();
        }
        return url;
    }
}
