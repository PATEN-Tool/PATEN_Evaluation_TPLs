// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.catalina.session;

import javax.servlet.ServletContext;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import org.apache.catalina.Loader;
import java.io.ObjectInputStream;
import org.apache.juli.logging.Log;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.FileInputStream;
import org.apache.catalina.Context;
import org.apache.catalina.Session;
import java.util.List;
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
        final File dir = this.directory();
        if (dir == null) {
            return 0;
        }
        final String[] files = dir.list();
        int keycount = 0;
        if (files != null) {
            for (final String file : files) {
                if (file.endsWith(".session")) {
                    ++keycount;
                }
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
        final File dir = this.directory();
        if (dir == null) {
            return new String[0];
        }
        final String[] files = dir.list();
        if (files == null || files.length < 1) {
            return new String[0];
        }
        final List<String> list = new ArrayList<String>();
        final int n = ".session".length();
        for (final String file : files) {
            if (file.endsWith(".session")) {
                list.add(file.substring(0, file.length() - n));
            }
        }
        return list.toArray(new String[list.size()]);
    }
    
    @Override
    public Session load(final String id) throws ClassNotFoundException, IOException {
        final File file = this.file(id);
        if (file == null || !file.exists()) {
            return null;
        }
        final Context context = (Context)this.getManager().getContainer();
        final Log containerLog = context.getLogger();
        if (containerLog.isDebugEnabled()) {
            containerLog.debug((Object)FileStore.sm.getString(this.getStoreName() + ".loading", id, file.getAbsolutePath()));
        }
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        Loader loader = null;
        ClassLoader classLoader = null;
        final ClassLoader oldThreadContextCL = Thread.currentThread().getContextClassLoader();
        try {
            fis = new FileInputStream(file.getAbsolutePath());
            loader = context.getLoader();
            if (loader != null) {
                classLoader = loader.getClassLoader();
            }
            if (classLoader != null) {
                Thread.currentThread().setContextClassLoader(classLoader);
            }
            ois = this.getObjectInputStream(fis);
            final StandardSession session = (StandardSession)this.manager.createEmptySession();
            session.readObjectData(ois);
            session.setManager(this.manager);
            return session;
        }
        catch (FileNotFoundException e) {
            if (containerLog.isDebugEnabled()) {
                containerLog.debug((Object)"No persisted data file found");
            }
            return null;
        }
        finally {
            if (fis != null) {
                try {
                    fis.close();
                }
                catch (IOException ex) {}
            }
            if (ois != null) {
                try {
                    ois.close();
                }
                catch (IOException ex2) {}
            }
            Thread.currentThread().setContextClassLoader(oldThreadContextCL);
        }
    }
    
    @Override
    public void remove(final String id) throws IOException {
        final File file = this.file(id);
        if (file == null) {
            return;
        }
        if (this.manager.getContainer().getLogger().isDebugEnabled()) {
            this.manager.getContainer().getLogger().debug((Object)FileStore.sm.getString(this.getStoreName() + ".removing", id, file.getAbsolutePath()));
        }
        if (file.exists() && !file.delete()) {
            throw new IOException(FileStore.sm.getString("fileStore.deleteSessionFailed", file));
        }
    }
    
    @Override
    public void save(final Session session) throws IOException {
        final File file = this.file(session.getIdInternal());
        if (file == null) {
            return;
        }
        if (this.manager.getContainer().getLogger().isDebugEnabled()) {
            this.manager.getContainer().getLogger().debug((Object)FileStore.sm.getString(this.getStoreName() + ".saving", session.getIdInternal(), file.getAbsolutePath()));
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
    
    private File directory() throws IOException {
        if (this.directory == null) {
            return null;
        }
        if (this.directoryFile != null) {
            return this.directoryFile;
        }
        File file = new File(this.directory);
        if (!file.isAbsolute()) {
            final Context context = (Context)this.manager.getContainer();
            final ServletContext servletContext = context.getServletContext();
            final File work = (File)servletContext.getAttribute("javax.servlet.context.tempdir");
            file = new File(work, this.directory);
        }
        if (!file.exists() || !file.isDirectory()) {
            if (!file.delete() && file.exists()) {
                throw new IOException(FileStore.sm.getString("fileStore.deleteFailed", file));
            }
            if (!file.mkdirs() && !file.isDirectory()) {
                throw new IOException(FileStore.sm.getString("fileStore.createFailed", file));
            }
        }
        return this.directoryFile = file;
    }
    
    private File file(final String id) throws IOException {
        if (this.directory == null) {
            return null;
        }
        final String filename = id + ".session";
        final File file = new File(this.directory(), filename);
        return file;
    }
}
