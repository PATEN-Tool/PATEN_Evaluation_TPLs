// 
// Decompiled by Procyon v0.5.36
// 

package org.keycloak.theme;

import java.util.Locale;
import java.net.URL;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.FileInputStream;
import java.util.Properties;
import java.io.File;

public class FolderTheme implements Theme
{
    private String parentName;
    private String importName;
    private File themeDir;
    private File resourcesDir;
    private String name;
    private Theme.Type type;
    private final Properties properties;
    
    public FolderTheme(final File themeDir, final String name, final Theme.Type type) throws IOException {
        this.themeDir = themeDir;
        this.name = name;
        this.type = type;
        this.properties = new Properties();
        final File propertiesFile = new File(themeDir, "theme.properties");
        if (propertiesFile.isFile()) {
            final Charset encoding = PropertiesUtil.detectEncoding(new FileInputStream(propertiesFile));
            try (final Reader reader = new InputStreamReader(new FileInputStream(propertiesFile), encoding)) {
                this.properties.load(reader);
            }
            this.parentName = this.properties.getProperty("parent");
            this.importName = this.properties.getProperty("import");
        }
        this.resourcesDir = new File(themeDir, "resources");
    }
    
    public String getName() {
        return this.name;
    }
    
    public String getParentName() {
        return this.parentName;
    }
    
    public String getImportName() {
        return this.importName;
    }
    
    public Theme.Type getType() {
        return this.type;
    }
    
    public URL getTemplate(final String name) throws IOException {
        final File file = new File(this.themeDir, name);
        return file.isFile() ? file.toURI().toURL() : null;
    }
    
    public InputStream getTemplateAsStream(final String name) throws IOException {
        final URL url = this.getTemplate(name);
        return (url != null) ? url.openStream() : null;
    }
    
    public URL getResource(String path) throws IOException {
        if (File.separatorChar != '/') {
            path = path.replace('/', File.separatorChar);
        }
        final File file = new File(this.resourcesDir, path);
        if (!file.isFile() || !file.getCanonicalPath().startsWith(this.resourcesDir.getCanonicalPath())) {
            return null;
        }
        return file.toURI().toURL();
    }
    
    public InputStream getResourceAsStream(final String path) throws IOException {
        final URL url = this.getResource(path);
        return (url != null) ? url.openStream() : null;
    }
    
    public Properties getMessages(final Locale locale) throws IOException {
        return this.getMessages("messages", locale);
    }
    
    public Properties getMessages(final String baseBundlename, final Locale locale) throws IOException {
        if (locale == null) {
            return null;
        }
        final Properties m = new Properties();
        final File file = new File(this.themeDir, "messages" + File.separator + baseBundlename + "_" + locale.toString() + ".properties");
        if (file.isFile()) {
            final Charset encoding = PropertiesUtil.detectEncoding(new FileInputStream(file));
            try (final Reader reader = new InputStreamReader(new FileInputStream(file), encoding)) {
                m.load(reader);
            }
        }
        return m;
    }
    
    public Properties getProperties() {
        return this.properties;
    }
}
