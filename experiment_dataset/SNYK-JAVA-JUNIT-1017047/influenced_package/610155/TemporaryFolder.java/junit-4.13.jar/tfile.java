// 
// Decompiled by Procyon v0.5.36
// 

package org.junit.rules;

import org.junit.Assert;
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
        this.folder = this.createTemporaryFolderIn(this.parentFolder);
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
        return this.createTemporaryFolderIn(this.getRoot());
    }
    
    private File createTemporaryFolderIn(final File parentFolder) throws IOException {
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
