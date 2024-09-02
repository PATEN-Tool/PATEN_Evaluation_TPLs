// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.catalina.session;

import org.apache.juli.logging.LogFactory;
import jakarta.servlet.ServletContext;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import org.apache.catalina.Context;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.FileInputStream;
import org.apache.catalina.Globals;
import org.apache.catalina.Session;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.io.File;
import org.apache.tomcat.util.res.StringManager;
import org.apache.juli.logging.Log;

public final class FileStore extends StoreBase
{
    private static final Log log;
    private static final StringManager sm;
    private static final String FILE_EXT = ".session";
    private String directory;
    private File directoryFile;
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
        final String[] keys2;
        final String[] keys = keys2 = this.keys();
        for (final String key : keys2) {
            this.remove(key);
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
        return list.toArray(new String[0]);
    }
    
    @Override
    public Session load(final String id) throws ClassNotFoundException, IOException {
        final File file = this.file(id);
        if (file == null || !file.exists()) {
            return null;
        }
        final Context context = this.getManager().getContext();
        final Log contextLog = context.getLogger();
        if (contextLog.isDebugEnabled()) {
            contextLog.debug(FileStore.sm.getString(this.getStoreName() + ".loading", id, file.getAbsolutePath()));
        }
        final ClassLoader oldThreadContextCL = context.bind(Globals.IS_SECURITY_ENABLED, null);
        try (final FileInputStream fis = new FileInputStream(file.getAbsolutePath());
             final ObjectInputStream ois = this.getObjectInputStream(fis)) {
            final StandardSession session = (StandardSession)this.manager.createEmptySession();
            session.readObjectData(ois);
            session.setManager(this.manager);
            return session;
        }
        catch (FileNotFoundException e) {
            if (contextLog.isDebugEnabled()) {
                contextLog.debug("No persisted data file found");
            }
            return null;
        }
        finally {
            context.unbind(Globals.IS_SECURITY_ENABLED, oldThreadContextCL);
        }
    }
    
    @Override
    public void remove(final String id) throws IOException {
        final File file = this.file(id);
        if (file == null) {
            return;
        }
        if (this.manager.getContext().getLogger().isDebugEnabled()) {
            this.manager.getContext().getLogger().debug(FileStore.sm.getString(this.getStoreName() + ".removing", id, file.getAbsolutePath()));
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
        if (this.manager.getContext().getLogger().isDebugEnabled()) {
            this.manager.getContext().getLogger().debug(FileStore.sm.getString(this.getStoreName() + ".saving", session.getIdInternal(), file.getAbsolutePath()));
        }
        try (final FileOutputStream fos = new FileOutputStream(file.getAbsolutePath());
             final ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(fos))) {
            ((StandardSession)session).writeObjectData(oos);
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
            final Context context = this.manager.getContext();
            final ServletContext servletContext = context.getServletContext();
            final File work = (File)servletContext.getAttribute("jakarta.servlet.context.tempdir");
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
        final File storageDir = this.directory();
        if (storageDir == null) {
            return null;
        }
        final String filename = id + ".session";
        final File file = new File(storageDir, filename);
        if (!file.getCanonicalPath().startsWith(storageDir.getCanonicalPath())) {
            FileStore.log.warn(FileStore.sm.getString("fileStore.invalid", file.getPath(), id));
            return null;
        }
        return file;
    }
    
    static {
        log = LogFactory.getLog(FileStore.class);
        sm = StringManager.getManager(FileStore.class);
    }
}
