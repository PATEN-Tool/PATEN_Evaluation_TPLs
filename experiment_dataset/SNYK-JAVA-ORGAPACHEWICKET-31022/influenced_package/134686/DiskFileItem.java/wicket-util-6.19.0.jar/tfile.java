// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.wicket.util.upload;

import java.util.UUID;
import org.slf4j.LoggerFactory;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Closeable;
import org.apache.wicket.util.io.IOUtils;
import org.apache.wicket.util.io.Streams;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedInputStream;
import org.apache.wicket.util.lang.Checks;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import org.apache.wicket.util.file.Files;
import java.util.Map;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import org.apache.wicket.util.file.IFileCleaner;
import org.apache.wicket.util.io.DeferredFileOutputStream;
import java.io.File;
import java.util.Random;
import org.slf4j.Logger;

public class DiskFileItem implements FileItem, FileItemHeadersSupport
{
    private static final Logger log;
    private static final long serialVersionUID = 2237570099615271025L;
    public static final String DEFAULT_CHARSET = "ISO-8859-1";
    private static final String UID;
    private static final Random counter;
    private String fieldName;
    private final String contentType;
    private boolean isFormField;
    private final String fileName;
    private long size;
    private final int sizeThreshold;
    private final File repository;
    private byte[] cachedContent;
    private transient DeferredFileOutputStream dfos;
    private transient File tempFile;
    private File dfosFile;
    private FileItemHeaders headers;
    private final transient IFileCleaner fileUploadCleaner;
    
    public DiskFileItem(final String fieldName, final String contentType, final boolean isFormField, final String fileName, final int sizeThreshold, final File repository, final IFileCleaner fileUploadCleaner) {
        this.size = -1L;
        this.fieldName = fieldName;
        this.contentType = contentType;
        this.isFormField = isFormField;
        this.fileName = fileName;
        this.sizeThreshold = sizeThreshold;
        this.repository = repository;
        this.fileUploadCleaner = fileUploadCleaner;
    }
    
    @Override
    public InputStream getInputStream() throws IOException {
        if (!this.isInMemory()) {
            return new FileInputStream(this.dfos.getFile());
        }
        if (this.cachedContent == null) {
            this.cachedContent = this.dfos.getData();
        }
        return new ByteArrayInputStream(this.cachedContent);
    }
    
    @Override
    public String getContentType() {
        return this.contentType;
    }
    
    public String getCharSet() {
        final ParameterParser parser = new ParameterParser();
        parser.setLowerCaseNames(true);
        final Map<?, ?> params = parser.parse(this.getContentType(), ';');
        return (String)params.get("charset");
    }
    
    @Override
    public String getName() {
        return this.fileName;
    }
    
    @Override
    public boolean isInMemory() {
        return this.cachedContent != null || this.dfos.isInMemory();
    }
    
    @Override
    public long getSize() {
        if (this.size >= 0L) {
            return this.size;
        }
        if (this.cachedContent != null) {
            return this.cachedContent.length;
        }
        if (this.dfos.isInMemory()) {
            return this.dfos.getData().length;
        }
        return this.dfos.getFile().length();
    }
    
    @Override
    public byte[] get() {
        if (this.isInMemory()) {
            if (this.cachedContent == null) {
                this.cachedContent = this.dfos.getData();
            }
            return this.cachedContent;
        }
        final File file = this.dfos.getFile();
        try {
            return Files.readBytes(file);
        }
        catch (IOException e) {
            DiskFileItem.log.debug("failed to read content of file: " + file.getAbsolutePath(), (Throwable)e);
            return null;
        }
    }
    
    @Override
    public String getString(final String charset) throws UnsupportedEncodingException {
        return new String(this.get(), charset);
    }
    
    @Override
    public String getString() {
        final byte[] rawdata = this.get();
        String charset = this.getCharSet();
        if (charset == null) {
            charset = "ISO-8859-1";
        }
        try {
            return new String(rawdata, charset);
        }
        catch (UnsupportedEncodingException e) {
            return new String(rawdata);
        }
    }
    
    @Override
    public void write(final File file) throws IOException {
        if (this.isInMemory()) {
            final FileOutputStream fout = new FileOutputStream(file);
            try {
                fout.write(this.get());
            }
            finally {
                fout.close();
            }
        }
        else {
            final File outputFile = this.getStoreLocation();
            Checks.notNull(outputFile, "for a non-memory upload the file location must not be empty", new Object[0]);
            this.size = outputFile.length();
            if (!outputFile.renameTo(file)) {
                BufferedInputStream in = null;
                BufferedOutputStream out = null;
                try {
                    in = new BufferedInputStream(new FileInputStream(outputFile));
                    out = new BufferedOutputStream(new FileOutputStream(file));
                    Streams.copy(in, out);
                }
                finally {
                    IOUtils.closeQuietly(in);
                    IOUtils.closeQuietly(out);
                }
            }
        }
    }
    
    @Override
    public void delete() {
        this.cachedContent = null;
        final File outputFile = this.getStoreLocation();
        if (outputFile != null && outputFile.exists() && !Files.remove(outputFile)) {
            DiskFileItem.log.error("failed to delete file: " + outputFile.getAbsolutePath());
        }
    }
    
    @Override
    public String getFieldName() {
        return this.fieldName;
    }
    
    @Override
    public void setFieldName(final String fieldName) {
        this.fieldName = fieldName;
    }
    
    @Override
    public boolean isFormField() {
        return this.isFormField;
    }
    
    @Override
    public void setFormField(final boolean state) {
        this.isFormField = state;
    }
    
    @Override
    public OutputStream getOutputStream() throws IOException {
        if (this.dfos == null) {
            this.dfos = new DeferredFileOutputStream(this.sizeThreshold, new DeferredFileOutputStream.FileFactory() {
                @Override
                public File createFile() {
                    return DiskFileItem.this.getTempFile();
                }
            });
        }
        return this.dfos;
    }
    
    public File getStoreLocation() {
        return (this.dfos == null) ? null : this.dfos.getFile();
    }
    
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        final File outputFile = this.dfos.getFile();
        if (outputFile != null && outputFile.exists() && !Files.remove(outputFile)) {
            DiskFileItem.log.error("failed to delete file: " + outputFile.getAbsolutePath());
        }
    }
    
    protected File getTempFile() {
        if (this.tempFile == null) {
            File tempDir = this.repository;
            Label_0047: {
                if (tempDir != null) {
                    break Label_0047;
                }
                String systemTmp = null;
                try {
                    systemTmp = System.getProperty("java.io.tmpdir");
                }
                catch (SecurityException e2) {
                    throw new RuntimeException("Reading property java.io.tmpdir is not allowed for the current security settings. The repository location needs to be set manually, or upgrade permissions to allow reading the tmpdir property.");
                }
                tempDir = new File(systemTmp);
                try {
                    do {
                        final String tempFileName = "upload_" + DiskFileItem.UID + "_" + getUniqueId() + ".tmp";
                        this.tempFile = new File(tempDir, tempFileName);
                    } while (!this.tempFile.createNewFile());
                }
                catch (IOException e) {
                    throw new RuntimeException("Could not create the temp file for upload: " + this.tempFile.getAbsolutePath(), e);
                }
            }
            if (this.fileUploadCleaner != null) {
                this.fileUploadCleaner.track(this.tempFile, this);
            }
        }
        return this.tempFile;
    }
    
    private static String getUniqueId() {
        final int limit = 100000000;
        final int current;
        synchronized (DiskFileItem.class) {
            current = DiskFileItem.counter.nextInt();
        }
        String id = Integer.toString(current);
        if (current < 100000000) {
            id = ("00000000" + id).substring(id.length());
        }
        return id;
    }
    
    @Override
    public String toString() {
        return "name=" + this.getName() + ", StoreLocation=" + String.valueOf(this.getStoreLocation()) + ", size=" + this.getSize() + "bytes, " + "isFormField=" + this.isFormField() + ", FieldName=" + this.getFieldName();
    }
    
    private void writeObject(final ObjectOutputStream out) throws IOException {
        if (this.dfos.isInMemory()) {
            this.cachedContent = this.get();
        }
        else {
            this.cachedContent = null;
            this.dfosFile = this.dfos.getFile();
        }
        out.defaultWriteObject();
    }
    
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        final OutputStream output = this.getOutputStream();
        if (this.cachedContent != null) {
            output.write(this.cachedContent);
        }
        else {
            final FileInputStream input = new FileInputStream(this.dfosFile);
            Streams.copy(input, output);
            Files.remove(this.dfosFile);
            this.dfosFile = null;
        }
        output.close();
        this.cachedContent = null;
    }
    
    @Override
    public FileItemHeaders getHeaders() {
        return this.headers;
    }
    
    @Override
    public void setHeaders(final FileItemHeaders pHeaders) {
        this.headers = pHeaders;
    }
    
    static {
        log = LoggerFactory.getLogger((Class)DiskFileItem.class);
        UID = UUID.randomUUID().toString().replace(':', '_').replace('-', '_');
        counter = new Random();
    }
}
