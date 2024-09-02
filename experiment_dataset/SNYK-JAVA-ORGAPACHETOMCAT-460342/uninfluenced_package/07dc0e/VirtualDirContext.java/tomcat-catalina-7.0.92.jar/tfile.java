// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.naming.resources;

import org.apache.juli.logging.LogFactory;
import java.util.Set;
import java.util.HashSet;
import org.apache.naming.NamingEntry;
import java.util.Iterator;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import java.io.IOException;
import java.util.ArrayList;
import java.io.File;
import java.util.StringTokenizer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.juli.logging.Log;

public class VirtualDirContext extends FileDirContext
{
    private static final Log log;
    private String extraResourcePaths;
    private Map<String, List<String>> mappedResourcePaths;
    
    public VirtualDirContext() {
        this.extraResourcePaths = "";
    }
    
    public void setExtraResourcePaths(final String path) {
        this.extraResourcePaths = path;
    }
    
    @Override
    public void allocate() {
        super.allocate();
        this.mappedResourcePaths = new HashMap<String, List<String>>();
        final StringTokenizer tkn = new StringTokenizer(this.extraResourcePaths, ",");
        while (tkn.hasMoreTokens()) {
            String resSpec = tkn.nextToken();
            if (resSpec.length() > 0) {
                int idx = resSpec.indexOf(61);
                String path;
                if (idx <= 0) {
                    path = "";
                }
                else {
                    if (resSpec.startsWith("/=")) {
                        resSpec = resSpec.substring(1);
                        --idx;
                    }
                    path = resSpec.substring(0, idx);
                }
                final File dir = new File(resSpec.substring(idx + 1));
                List<String> resourcePaths = this.mappedResourcePaths.get(path);
                if (resourcePaths == null) {
                    resourcePaths = new ArrayList<String>();
                    this.mappedResourcePaths.put(path, resourcePaths);
                }
                try {
                    resourcePaths.add(dir.getCanonicalPath());
                }
                catch (IOException e) {
                    VirtualDirContext.log.warn((Object)VirtualDirContext.sm.getString("fileResources.canonical.fail", dir.getPath()));
                    resourcePaths.add(dir.getAbsolutePath());
                }
            }
        }
        if (this.mappedResourcePaths.isEmpty()) {
            this.mappedResourcePaths = null;
        }
    }
    
    @Override
    public void release() {
        this.mappedResourcePaths = null;
        super.release();
    }
    
    @Override
    public Attributes getAttributes(final String name) throws NamingException {
        try {
            final Attributes attributes = super.getAttributes(name);
            return attributes;
        }
        catch (NamingException exc) {
            final NamingException initialException = exc;
            if (this.mappedResourcePaths != null) {
                for (final Map.Entry<String, List<String>> mapping : this.mappedResourcePaths.entrySet()) {
                    String path = mapping.getKey();
                    final List<String> dirList = mapping.getValue();
                    final String resourcesDir = dirList.get(0);
                    if (name.equals(path)) {
                        File f = new File(resourcesDir);
                        f = this.validate(f, name, true, resourcesDir);
                        if (f != null) {
                            return new FileResourceAttributes(f);
                        }
                    }
                    path += "/";
                    if (name.startsWith(path)) {
                        final String res = name.substring(path.length());
                        File f2 = new File(resourcesDir, res);
                        f2 = this.validate(f2, res, true, resourcesDir);
                        if (f2 != null) {
                            return new FileResourceAttributes(f2);
                        }
                        continue;
                    }
                }
            }
            throw initialException;
        }
    }
    
    @Override
    protected File file(final String name) {
        return this.file(name, true);
    }
    
    @Override
    protected File file(String name, final boolean mustExist) {
        File file = super.file(name, true);
        if (file != null || this.mappedResourcePaths == null) {
            return file;
        }
        if (name.length() > 0 && name.charAt(0) != '/') {
            name = "/" + name;
        }
        for (final Map.Entry<String, List<String>> mapping : this.mappedResourcePaths.entrySet()) {
            String path = mapping.getKey();
            final List<String> dirList = mapping.getValue();
            if (name.equals(path)) {
                for (final String resourcesDir : dirList) {
                    file = new File(resourcesDir);
                    file = this.validate(file, name, true, resourcesDir);
                    if (file != null) {
                        return file;
                    }
                }
            }
            path += "/";
            if (name.startsWith(path)) {
                final String res = name.substring(path.length());
                for (final String resourcesDir2 : dirList) {
                    file = new File(resourcesDir2, res);
                    file = this.validate(file, res, true, resourcesDir2);
                    if (file != null) {
                        return file;
                    }
                }
            }
        }
        return null;
    }
    
    @Override
    protected List<NamingEntry> list(final File file) {
        final List<NamingEntry> entries = super.list(file);
        if (this.mappedResourcePaths != null && !this.mappedResourcePaths.isEmpty()) {
            final Set<String> entryNames = new HashSet<String>(entries.size());
            for (final NamingEntry entry : entries) {
                entryNames.add(entry.name);
            }
            final String absPath = file.getAbsolutePath();
            if (absPath.startsWith(this.getDocBase() + File.separator)) {
                final String relPath = absPath.substring(this.getDocBase().length());
                final String fsRelPath = relPath.replace(File.separatorChar, '/');
                for (final Map.Entry<String, List<String>> mapping : this.mappedResourcePaths.entrySet()) {
                    final String path = mapping.getKey();
                    final List<String> dirList = mapping.getValue();
                    String res = null;
                    if (fsRelPath.equals(path)) {
                        res = "";
                    }
                    else if (fsRelPath.startsWith(path + "/")) {
                        res = relPath.substring(path.length());
                    }
                    if (res != null) {
                        for (final String resourcesDir : dirList) {
                            File f = new File(resourcesDir, res);
                            f = this.validate(f, res, true, resourcesDir);
                            if (f != null && f.isDirectory()) {
                                final List<NamingEntry> virtEntries = super.list(f);
                                for (final NamingEntry entry2 : virtEntries) {
                                    if (!entryNames.contains(entry2.name)) {
                                        entryNames.add(entry2.name);
                                        entries.add(entry2);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return entries;
    }
    
    @Override
    protected Object doLookup(final String name) {
        final Object retSuper = super.doLookup(name);
        if (retSuper != null || this.mappedResourcePaths == null) {
            return retSuper;
        }
        for (final Map.Entry<String, List<String>> mapping : this.mappedResourcePaths.entrySet()) {
            String path = mapping.getKey();
            final List<String> dirList = mapping.getValue();
            if (name.equals(path)) {
                for (final String resourcesDir : dirList) {
                    File f = new File(resourcesDir);
                    f = this.validate(f, name, true, resourcesDir);
                    if (f != null && f.isFile()) {
                        return new FileResource(f);
                    }
                }
            }
            path += "/";
            if (name.startsWith(path)) {
                final String res = name.substring(path.length());
                for (final String resourcesDir2 : dirList) {
                    File f2 = new File(resourcesDir2, res);
                    f2 = this.validate(f2, res, true, resourcesDir2);
                    if (f2 != null && f2.isFile()) {
                        return new FileResource(f2);
                    }
                }
            }
        }
        return retSuper;
    }
    
    @Override
    protected String doGetRealPath(final String path) {
        final File file = this.file(path);
        if (null != file) {
            return file.getAbsolutePath();
        }
        return null;
    }
    
    protected File validate(final File file, final String name, final boolean mustExist, final String absoluteBase) {
        return this.validate(file, name, mustExist, this.normalize(absoluteBase), absoluteBase);
    }
    
    static {
        log = LogFactory.getLog((Class)VirtualDirContext.class);
    }
}
