// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.naming.resources;

import java.util.Iterator;
import java.util.List;
import java.util.Collection;
import org.apache.naming.NamingEntry;
import java.util.ArrayList;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import java.util.StringTokenizer;
import java.util.Hashtable;
import java.io.File;
import java.util.Map;

public class VirtualDirContext extends FileDirContext
{
    private Map<String, File> virtualMappings;
    private Map<String, File> tagfileMappings;
    private String virtualClasspath;
    
    public void setVirtualClasspath(final String path) {
        this.virtualClasspath = path;
    }
    
    @Override
    public void allocate() {
        super.allocate();
        this.virtualMappings = new Hashtable<String, File>();
        this.tagfileMappings = new Hashtable<String, File>();
        final StringTokenizer tkn = new StringTokenizer(this.virtualClasspath, ";");
        while (tkn.hasMoreTokens()) {
            final File file = new File(tkn.nextToken(), "META-INF");
            if (file.exists()) {
                if (!file.isDirectory()) {
                    continue;
                }
                this.scanForTlds(file);
            }
        }
    }
    
    @Override
    public void release() {
        super.release();
        this.virtualMappings = null;
    }
    
    @Override
    public Attributes getAttributes(final String name) throws NamingException {
        if (name.startsWith("/WEB-INF/") && name.endsWith(".tld")) {
            final String tldName = name.substring(name.lastIndexOf("/") + 1);
            if (this.virtualMappings.containsKey(tldName)) {
                return new FileResourceAttributes(this.virtualMappings.get(tldName));
            }
        }
        else if ((name.startsWith("/META-INF/tags") && name.endsWith(".tag")) || name.endsWith(".tagx")) {
            if (this.tagfileMappings.containsKey(name)) {
                return new FileResourceAttributes(this.tagfileMappings.get(name));
            }
            final StringTokenizer tkn = new StringTokenizer(this.virtualClasspath, ";");
            while (tkn.hasMoreTokens()) {
                final File file = new File(tkn.nextToken(), name);
                if (file.exists()) {
                    this.tagfileMappings.put(name, file);
                    return new FileResourceAttributes(file);
                }
            }
        }
        return super.getAttributes(name);
    }
    
    @Override
    protected ArrayList<NamingEntry> list(final File file) {
        final ArrayList<NamingEntry> entries = super.list(file);
        if ("WEB-INF".equals(file.getName())) {
            entries.addAll(this.getVirtualNamingEntries());
        }
        return entries;
    }
    
    @Override
    protected Object doLookup(final String name) {
        if (name.startsWith("/WEB-INF/") && name.endsWith(".tld")) {
            final String tldName = name.substring(name.lastIndexOf("/") + 1);
            if (this.virtualMappings.containsKey(tldName)) {
                return new FileResource(this.virtualMappings.get(tldName));
            }
        }
        else if ((name.startsWith("/META-INF/tags") && name.endsWith(".tag")) || name.endsWith(".tagx")) {
            final File tagFile = this.tagfileMappings.get(name);
            if (tagFile != null) {
                return new FileResource(tagFile);
            }
        }
        return super.doLookup(name);
    }
    
    private void scanForTlds(final File dir) {
        final File[] files = dir.listFiles();
        for (int j = 0; j < files.length; ++j) {
            final File file = files[j];
            if (file.isDirectory()) {
                this.scanForTlds(file);
            }
            else if (file.getName().endsWith(".tld")) {
                final String virtualTldName = "~" + System.currentTimeMillis() + "~" + file.getName();
                this.virtualMappings.put(virtualTldName, file);
            }
        }
    }
    
    private List<NamingEntry> getVirtualNamingEntries() {
        final List<NamingEntry> virtual = new ArrayList<NamingEntry>();
        for (final String name : this.virtualMappings.keySet()) {
            final File file = this.virtualMappings.get(name);
            final NamingEntry entry = new NamingEntry(name, new FileResource(file), 0);
            virtual.add(entry);
        }
        return virtual;
    }
}
