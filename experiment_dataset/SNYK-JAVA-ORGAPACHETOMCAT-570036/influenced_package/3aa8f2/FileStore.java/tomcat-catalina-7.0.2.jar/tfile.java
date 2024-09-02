// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.catalina.session;

import javax.servlet.ServletContext;
import org.apache.catalina.Context;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import org.apache.catalina.Container;
import org.apache.catalina.Loader;
import java.io.FileNotFoundException;
import java.io.ObjectInputStream;
import org.apache.catalina.util.CustomObjectInputStream;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import org.apache.catalina.Session;
import java.util.ArrayList;
import java.io.IOException;
import java.io.File;

public final class FileStore extends StoreBase
{
    private static final String FILE_EXT = ".session";
    private String directory;
    private File directoryFile;
    private static final String info = "FileStore/1.0";
    private static final String storeName = "fileStore";
    private static final String threadName = "FileStore";
    
    public FileStore() {
        this.directory = ".";
        this.directoryFile = null;
    }
    
    public String getDirectory() {
        return this.directory;
    }
    
    public void setDirectory(final String path) {
        final String oldDirectory = this.directory;
        this.directory = path;
        this.directoryFile = null;
        this.support.firePropertyChange("directory", oldDirectory, this.directory);
    }
    
    @Override
    public String getInfo() {
        return "FileStore/1.0";
    }
    
    public String getThreadName() {
        return "FileStore";
    }
    
    @Override
    public String getStoreName() {
        return "fileStore";
    }
    
    @Override
    public int getSize() throws IOException {
        final File file = this.directory();
        if (file == null) {
            return 0;
        }
        final String[] files = file.list();
        int keycount = 0;
        for (int i = 0; i < files.length; ++i) {
            if (files[i].endsWith(".session")) {
                ++keycount;
            }
        }
        return keycount;
    }
    
    @Override
    public void clear() throws IOException {
        final String[] keys = this.keys();
        for (int i = 0; i < keys.length; ++i) {
            this.remove(keys[i]);
        }
    }
    
    @Override
    public String[] keys() throws IOException {
        final File file = this.directory();
        if (file == null) {
            return new String[0];
        }
        final String[] files = file.list();
        if (files == null || files.length < 1) {
            return new String[0];
        }
        final ArrayList<String> list = new ArrayList<String>();
        final int n = ".session".length();
        for (int i = 0; i < files.length; ++i) {
            if (files[i].endsWith(".session")) {
                list.add(files[i].substring(0, files[i].length() - n));
            }
        }
        return list.toArray(new String[list.size()]);
    }
    
    @Override
    public Session load(final String id) throws ClassNotFoundException, IOException {
        final File file = this.file(id);
        if (file == null) {
            return null;
        }
        if (!file.exists()) {
            return null;
        }
        if (this.manager.getContainer().getLogger().isDebugEnabled()) {
            this.manager.getContainer().getLogger().debug((Object)FileStore.sm.getString(this.getStoreName() + ".loading", new Object[] { id, file.getAbsolutePath() }));
        }
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        ObjectInputStream ois = null;
        Loader loader = null;
        ClassLoader classLoader = null;
        try {
            fis = new FileInputStream(file.getAbsolutePath());
            bis = new BufferedInputStream(fis);
            final Container container = this.manager.getContainer();
            if (container != null) {
                loader = container.getLoader();
            }
            if (loader != null) {
                classLoader = loader.getClassLoader();
            }
            if (classLoader != null) {
                ois = new CustomObjectInputStream(bis, classLoader);
            }
            else {
                ois = new ObjectInputStream(bis);
            }
        }
        catch (FileNotFoundException e2) {
            if (this.manager.getContainer().getLogger().isDebugEnabled()) {
                this.manager.getContainer().getLogger().debug((Object)"No persisted data file found");
            }
            return null;
        }
        catch (IOException e) {
            if (bis != null) {
                try {
                    bis.close();
                }
                catch (IOException ex) {}
            }
            if (fis != null) {
                try {
                    fis.close();
                }
                catch (IOException ex2) {}
            }
            throw e;
        }
        try {
            final StandardSession session = (StandardSession)this.manager.createEmptySession();
            session.readObjectData(ois);
            session.setManager(this.manager);
            return session;
        }
        finally {
            try {
                ois.close();
            }
            catch (IOException ex3) {}
        }
    }
    
    @Override
    public void remove(final String id) throws IOException {
        final File file = this.file(id);
        if (file == null) {
            return;
        }
        if (this.manager.getContainer().getLogger().isDebugEnabled()) {
            this.manager.getContainer().getLogger().debug((Object)FileStore.sm.getString(this.getStoreName() + ".removing", new Object[] { id, file.getAbsolutePath() }));
        }
        file.delete();
    }
    
    @Override
    public void save(final Session session) throws IOException {
        final File file = this.file(session.getIdInternal());
        if (file == null) {
            return;
        }
        if (this.manager.getContainer().getLogger().isDebugEnabled()) {
            this.manager.getContainer().getLogger().debug((Object)FileStore.sm.getString(this.getStoreName() + ".saving", new Object[] { session.getIdInternal(), file.getAbsolutePath() }));
        }
        FileOutputStream fos = null;
        ObjectOutputStream oos = null;
        try {
            fos = new FileOutputStream(file.getAbsolutePath());
            oos = new ObjectOutputStream(new BufferedOutputStream(fos));
        }
        catch (IOException e) {
            if (fos != null) {
                try {
                    fos.close();
                }
                catch (IOException ex) {}
            }
            throw e;
        }
        try {
            ((StandardSession)session).writeObjectData(oos);
        }
        finally {
            oos.close();
        }
    }
    
    private File directory() {
        if (this.directory == null) {
            return null;
        }
        if (this.directoryFile != null) {
            return this.directoryFile;
        }
        File file = new File(this.directory);
        if (!file.isAbsolute()) {
            final Container container = this.manager.getContainer();
            if (!(container instanceof Context)) {
                throw new IllegalArgumentException("Parent Container is not a Context");
            }
            final ServletContext servletContext = ((Context)container).getServletContext();
            final File work = (File)servletContext.getAttribute("javax.servlet.context.tempdir");
            file = new File(work, this.directory);
        }
        if (!file.exists() || !file.isDirectory()) {
            file.delete();
            file.mkdirs();
        }
        return this.directoryFile = file;
    }
    
    private File file(final String id) {
        if (this.directory == null) {
            return null;
        }
        final String filename = id + ".session";
        final File file = new File(this.directory(), filename);
        return file;
    }
}
