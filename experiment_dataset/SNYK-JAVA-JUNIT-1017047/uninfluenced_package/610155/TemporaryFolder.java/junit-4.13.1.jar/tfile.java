// 
// Decompiled by Procyon v0.5.36
// 

package org.junit.rules;

import org.junit.Assert;
import java.lang.reflect.Method;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.io.IOException;
import java.io.File;

public class TemporaryFolder extends ExternalResource
{
    private final File parentFolder;
    private final boolean assureDeletion;
    private File folder;
    private static final int TEMP_DIR_ATTEMPTS = 10000;
    private static final String TMP_PREFIX = "junit";
    
    public TemporaryFolder() {
        this((File)null);
    }
    
    public TemporaryFolder(final File parentFolder) {
        this.parentFolder = parentFolder;
        this.assureDeletion = false;
    }
    
    protected TemporaryFolder(final Builder builder) {
        this.parentFolder = builder.parentFolder;
        this.assureDeletion = builder.assureDeletion;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    @Override
    protected void before() throws Throwable {
        this.create();
    }
    
    @Override
    protected void after() {
        this.delete();
    }
    
    public void create() throws IOException {
        this.folder = createTemporaryFolderIn(this.parentFolder);
    }
    
    public File newFile(final String fileName) throws IOException {
        final File file = new File(this.getRoot(), fileName);
        if (!file.createNewFile()) {
            throw new IOException("a file with the name '" + fileName + "' already exists in the test folder");
        }
        return file;
    }
    
    public File newFile() throws IOException {
        return File.createTempFile("junit", null, this.getRoot());
    }
    
    public File newFolder(final String path) throws IOException {
        return this.newFolder(new String[] { path });
    }
    
    public File newFolder(final String... paths) throws IOException {
        if (paths.length == 0) {
            throw new IllegalArgumentException("must pass at least one path");
        }
        final File root = this.getRoot();
        for (final String path : paths) {
            if (new File(path).isAbsolute()) {
                throw new IOException("folder path '" + path + "' is not a relative path");
            }
        }
        File relativePath = null;
        File file = root;
        boolean lastMkdirsCallSuccessful = true;
        final String[] arr$2 = paths;
        final int len$2 = arr$2.length;
        int i$2 = 0;
        while (i$2 < len$2) {
            final String path2 = arr$2[i$2];
            relativePath = new File(relativePath, path2);
            file = new File(root, relativePath.getPath());
            lastMkdirsCallSuccessful = file.mkdirs();
            if (!lastMkdirsCallSuccessful && !file.isDirectory()) {
                if (file.exists()) {
                    throw new IOException("a file with the path '" + relativePath.getPath() + "' exists");
                }
                throw new IOException("could not create a folder with the path '" + relativePath.getPath() + "'");
            }
            else {
                ++i$2;
            }
        }
        if (!lastMkdirsCallSuccessful) {
            throw new IOException("a folder with the path '" + relativePath.getPath() + "' already exists");
        }
        return file;
    }
    
    public File newFolder() throws IOException {
        return createTemporaryFolderIn(this.getRoot());
    }
    
    private static File createTemporaryFolderIn(final File parentFolder) throws IOException {
        try {
            return createTemporaryFolderWithNioApi(parentFolder);
        }
        catch (ClassNotFoundException ignore) {
            return createTemporaryFolderWithFileApi(parentFolder);
        }
        catch (InvocationTargetException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                throw (IOException)cause;
            }
            if (cause instanceof RuntimeException) {
                throw (RuntimeException)cause;
            }
            final IOException exception = new IOException("Failed to create temporary folder in " + parentFolder);
            exception.initCause(cause);
            throw exception;
        }
        catch (Exception e2) {
            throw new RuntimeException("Failed to create temporary folder in " + parentFolder, e2);
        }
    }
    
    private static File createTemporaryFolderWithNioApi(final File parentFolder) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        final Class<?> filesClass = Class.forName("java.nio.file.Files");
        final Object fileAttributeArray = Array.newInstance(Class.forName("java.nio.file.attribute.FileAttribute"), 0);
        final Class<?> pathClass = Class.forName("java.nio.file.Path");
        Object tempDir;
        if (parentFolder != null) {
            final Method createTempDirectoryMethod = filesClass.getDeclaredMethod("createTempDirectory", pathClass, String.class, fileAttributeArray.getClass());
            final Object parentPath = File.class.getDeclaredMethod("toPath", (Class<?>[])new Class[0]).invoke(parentFolder, new Object[0]);
            tempDir = createTempDirectoryMethod.invoke(null, parentPath, "junit", fileAttributeArray);
        }
        else {
            final Method createTempDirectoryMethod = filesClass.getDeclaredMethod("createTempDirectory", String.class, fileAttributeArray.getClass());
            tempDir = createTempDirectoryMethod.invoke(null, "junit", fileAttributeArray);
        }
        return (File)pathClass.getDeclaredMethod("toFile", (Class<?>[])new Class[0]).invoke(tempDir, new Object[0]);
    }
    
    private static File createTemporaryFolderWithFileApi(final File parentFolder) throws IOException {
        File createdFolder = null;
        for (int i = 0; i < 10000; ++i) {
            final String suffix = ".tmp";
            final File tmpFile = File.createTempFile("junit", suffix, parentFolder);
            final String tmpName = tmpFile.toString();
            final String folderName = tmpName.substring(0, tmpName.length() - suffix.length());
            createdFolder = new File(folderName);
            if (createdFolder.mkdir()) {
                tmpFile.delete();
                return createdFolder;
            }
            tmpFile.delete();
        }
        throw new IOException("Unable to create temporary directory in: " + parentFolder.toString() + ". Tried " + 10000 + " times. " + "Last attempted to create: " + createdFolder.toString());
    }
    
    public File getRoot() {
        if (this.folder == null) {
            throw new IllegalStateException("the temporary folder has not yet been created");
        }
        return this.folder;
    }
    
    public void delete() {
        if (!this.tryDelete() && this.assureDeletion) {
            Assert.fail("Unable to clean up temporary folder " + this.folder);
        }
    }
    
    private boolean tryDelete() {
        return this.folder == null || this.recursiveDelete(this.folder);
    }
    
    private boolean recursiveDelete(final File file) {
        if (file.delete()) {
            return true;
        }
        final File[] files = file.listFiles();
        if (files != null) {
            for (final File each : files) {
                if (!this.recursiveDelete(each)) {
                    return false;
                }
            }
        }
        return file.delete();
    }
    
    public static class Builder
    {
        private File parentFolder;
        private boolean assureDeletion;
        
        protected Builder() {
        }
        
        public Builder parentFolder(final File parentFolder) {
            this.parentFolder = parentFolder;
            return this;
        }
        
        public Builder assureDeletion() {
            this.assureDeletion = true;
            return this;
        }
        
        public TemporaryFolder build() {
            return new TemporaryFolder(this);
        }
    }
}
