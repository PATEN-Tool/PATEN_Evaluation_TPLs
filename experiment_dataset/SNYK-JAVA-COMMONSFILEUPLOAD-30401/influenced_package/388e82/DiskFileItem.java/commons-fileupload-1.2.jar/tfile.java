// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.commons.fileupload.disk;

import java.rmi.server.UID;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.apache.commons.io.FileCleaner;
import org.apache.commons.io.IOUtils;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedInputStream;
import org.apache.commons.fileupload.FileUploadException;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import org.apache.commons.fileupload.ParameterParser;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import org.apache.commons.io.output.DeferredFileOutputStream;
import java.io.File;
import org.apache.commons.fileupload.FileItem;

public class DiskFileItem implements FileItem
{
    public static final String DEFAULT_CHARSET = "ISO-8859-1";
    private static final String UID;
    private static int counter;
    private String fieldName;
    private String contentType;
    private boolean isFormField;
    private String fileName;
    private long size;
    private int sizeThreshold;
    private File repository;
    private byte[] cachedContent;
    private transient DeferredFileOutputStream dfos;
    private File dfosFile;
    static /* synthetic */ Class class$org$apache$commons$fileupload$disk$DiskFileItem;
    
    public DiskFileItem(final String fieldName, final String contentType, final boolean isFormField, final String fileName, final int sizeThreshold, final File repository) {
        this.size = -1L;
        this.fieldName = fieldName;
        this.contentType = contentType;
        this.isFormField = isFormField;
        this.fileName = fileName;
        this.sizeThreshold = sizeThreshold;
        this.repository = repository;
    }
    
    public InputStream getInputStream() throws IOException {
        if (!this.isInMemory()) {
            return new FileInputStream(this.dfos.getFile());
        }
        if (this.cachedContent == null) {
            this.cachedContent = this.dfos.getData();
        }
        return new ByteArrayInputStream(this.cachedContent);
    }
    
    public String getContentType() {
        return this.contentType;
    }
    
    public String getCharSet() {
        final ParameterParser parser = new ParameterParser();
        parser.setLowerCaseNames(true);
        final Map params = parser.parse(this.getContentType(), ';');
        return params.get("charset");
    }
    
    public String getName() {
        return this.fileName;
    }
    
    public boolean isInMemory() {
        return this.cachedContent != null || this.dfos.isInMemory();
    }
    
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
    
    public byte[] get() {
        if (this.isInMemory()) {
            if (this.cachedContent == null) {
                this.cachedContent = this.dfos.getData();
            }
            return this.cachedContent;
        }
        byte[] fileData = new byte[(int)this.getSize()];
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(this.dfos.getFile());
            fis.read(fileData);
        }
        catch (IOException e) {
            fileData = null;
        }
        finally {
            if (fis != null) {
                try {
                    fis.close();
                }
                catch (IOException ex) {}
            }
        }
        return fileData;
    }
    
    public String getString(final String charset) throws UnsupportedEncodingException {
        return new String(this.get(), charset);
    }
    
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
    
    public void write(final File file) throws Exception {
        if (this.isInMemory()) {
            FileOutputStream fout = null;
            try {
                fout = new FileOutputStream(file);
                fout.write(this.get());
            }
            finally {
                if (fout != null) {
                    fout.close();
                }
            }
        }
        else {
            final File outputFile = this.getStoreLocation();
            if (outputFile == null) {
                throw new FileUploadException("Cannot write uploaded file to disk!");
            }
            this.size = outputFile.length();
            if (!outputFile.renameTo(file)) {
                BufferedInputStream in = null;
                BufferedOutputStream out = null;
                try {
                    in = new BufferedInputStream(new FileInputStream(outputFile));
                    out = new BufferedOutputStream(new FileOutputStream(file));
                    IOUtils.copy((InputStream)in, (OutputStream)out);
                }
                finally {
                    if (in != null) {
                        try {
                            in.close();
                        }
                        catch (IOException ex) {}
                    }
                    if (out != null) {
                        try {
                            out.close();
                        }
                        catch (IOException ex2) {}
                    }
                }
            }
        }
    }
    
    public void delete() {
        this.cachedContent = null;
        final File outputFile = this.getStoreLocation();
        if (outputFile != null && outputFile.exists()) {
            outputFile.delete();
        }
    }
    
    public String getFieldName() {
        return this.fieldName;
    }
    
    public void setFieldName(final String fieldName) {
        this.fieldName = fieldName;
    }
    
    public boolean isFormField() {
        return this.isFormField;
    }
    
    public void setFormField(final boolean state) {
        this.isFormField = state;
    }
    
    public OutputStream getOutputStream() throws IOException {
        if (this.dfos == null) {
            final File outputFile = this.getTempFile();
            this.dfos = new DeferredFileOutputStream(this.sizeThreshold, outputFile);
        }
        return (OutputStream)this.dfos;
    }
    
    public File getStoreLocation() {
        return this.dfos.getFile();
    }
    
    protected void finalize() {
        final File outputFile = this.dfos.getFile();
        if (outputFile != null && outputFile.exists()) {
            outputFile.delete();
        }
    }
    
    protected File getTempFile() {
        File tempDir = this.repository;
        if (tempDir == null) {
            tempDir = new File(System.getProperty("java.io.tmpdir"));
        }
        final String tempFileName = "upload_" + DiskFileItem.UID + "_" + getUniqueId() + ".tmp";
        final File f = new File(tempDir, tempFileName);
        FileCleaner.track(f, (Object)this);
        return f;
    }
    
    private static String getUniqueId() {
        final int limit = 100000000;
        Class class$;
        Class class$org$apache$commons$fileupload$disk$DiskFileItem;
        if (DiskFileItem.class$org$apache$commons$fileupload$disk$DiskFileItem == null) {
            class$org$apache$commons$fileupload$disk$DiskFileItem = (DiskFileItem.class$org$apache$commons$fileupload$disk$DiskFileItem = (class$ = class$("org.apache.commons.fileupload.disk.DiskFileItem")));
        }
        else {
            class$ = (class$org$apache$commons$fileupload$disk$DiskFileItem = DiskFileItem.class$org$apache$commons$fileupload$disk$DiskFileItem);
        }
        final Class clazz = class$org$apache$commons$fileupload$disk$DiskFileItem;
        final int current;
        synchronized (class$) {
            current = DiskFileItem.counter++;
        }
        String id = Integer.toString(current);
        if (current < 100000000) {
            id = ("00000000" + id).substring(id.length());
        }
        return id;
    }
    
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
            IOUtils.copy((InputStream)input, output);
            this.dfosFile.delete();
            this.dfosFile = null;
        }
        output.close();
        this.cachedContent = null;
    }
    
    static /* synthetic */ Class class$(final String x0) {
        try {
            return Class.forName(x0);
        }
        catch (ClassNotFoundException x) {
            throw new NoClassDefFoundError(x.getMessage());
        }
    }
    
    static {
        UID = new UID().toString().replace(':', '_').replace('-', '_');
        DiskFileItem.counter = 0;
    }
}
