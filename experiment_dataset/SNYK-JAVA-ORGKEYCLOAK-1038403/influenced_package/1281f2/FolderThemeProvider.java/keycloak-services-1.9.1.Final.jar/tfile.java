// 
// Decompiled by Procyon v0.5.36
// 

package org.keycloak.theme;

import java.util.Collections;
import java.util.HashSet;
import java.io.FileFilter;
import java.util.Set;
import java.io.IOException;
import java.io.File;

public class FolderThemeProvider implements ThemeProvider
{
    private File themesDir;
    
    public FolderThemeProvider(final File themesDir) {
        this.themesDir = themesDir;
    }
    
    public int getProviderPriority() {
        return 100;
    }
    
    public Theme getTheme(final String name, final Theme.Type type) throws IOException {
        final File themeDir = this.getThemeDir(name, type);
        return (Theme)(themeDir.isDirectory() ? new FolderTheme(themeDir, name, type) : null);
    }
    
    public Set<String> nameSet(final Theme.Type type) {
        final String typeName = type.name().toLowerCase();
        final File[] themeDirs = this.themesDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(final File pathname) {
                return pathname.isDirectory() && new File(pathname, typeName).isDirectory();
            }
        });
        if (themeDirs != null) {
            final Set<String> names = new HashSet<String>();
            for (final File themeDir : themeDirs) {
                names.add(themeDir.getName());
            }
            return names;
        }
        return Collections.emptySet();
    }
    
    public boolean hasTheme(final String name, final Theme.Type type) {
        return this.getThemeDir(name, type).isDirectory();
    }
    
    public void close() {
    }
    
    private File getThemeDir(final String name, final Theme.Type type) {
        return new File(this.themesDir, name + File.separator + type.name().toLowerCase());
    }
}
