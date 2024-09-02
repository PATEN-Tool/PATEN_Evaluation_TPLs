// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.integration.zip.transformer;

import org.apache.commons.logging.LogFactory;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessagingException;
import org.zeroturnaround.zip.ZipUtil;
import org.zeroturnaround.zip.ZipException;
import java.io.IOException;
import org.apache.commons.io.IOUtils;
import java.util.zip.ZipEntry;
import java.util.SortedMap;
import org.zeroturnaround.zip.ZipEntryCallback;
import java.util.TreeMap;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.File;
import org.springframework.messaging.Message;
import org.apache.commons.logging.Log;

public class UnZipTransformer extends AbstractZipTransformer
{
    private static final Log logger;
    private volatile boolean expectSingleResult;
    
    public UnZipTransformer() {
        this.expectSingleResult = false;
    }
    
    public void setExpectSingleResult(final boolean expectSingleResult) {
        this.expectSingleResult = expectSingleResult;
    }
    
    @Override
    protected Object doZipTransform(final Message<?> message) {
        try {
            final Object payload = message.getPayload();
            InputStream inputStream = null;
            Object unzippedData;
            try {
                if (payload instanceof File) {
                    final File filePayload = (File)payload;
                    if (filePayload.isDirectory()) {
                        throw new UnsupportedOperationException(String.format("Cannot unzip a directory: '%s'", filePayload.getAbsolutePath()));
                    }
                    if (!SpringZipUtils.isValid(filePayload)) {
                        throw new IllegalStateException(String.format("Not a zip file: '%s'.", filePayload.getAbsolutePath()));
                    }
                    inputStream = new FileInputStream(filePayload);
                }
                else if (payload instanceof InputStream) {
                    inputStream = (InputStream)payload;
                }
                else {
                    if (!(payload instanceof byte[])) {
                        throw new IllegalArgumentException(String.format("Unsupported payload type '%s'. The only supported payload types are java.io.File, byte[] and java.io.InputStream", payload.getClass().getSimpleName()));
                    }
                    inputStream = new ByteArrayInputStream((byte[])payload);
                }
                final SortedMap<String, Object> uncompressedData = new TreeMap<String, Object>();
                ZipUtil.iterate(inputStream, (ZipEntryCallback)new ZipEntryCallback() {
                    public void process(final InputStream zipEntryInputStream, final ZipEntry zipEntry) throws IOException {
                        final String zipEntryName = zipEntry.getName();
                        final long zipEntryTime = zipEntry.getTime();
                        final long zipEntryCompressedSize = zipEntry.getCompressedSize();
                        final String type = zipEntry.isDirectory() ? "directory" : "file";
                        if (UnZipTransformer.logger.isInfoEnabled()) {
                            UnZipTransformer.logger.info((Object)String.format("Unpacking Zip Entry - Name: '%s',Time: '%s', Compressed Size: '%s', Type: '%s'", zipEntryName, zipEntryTime, zipEntryCompressedSize, type));
                        }
                        if (ZipResultType.FILE.equals(UnZipTransformer.this.zipResultType)) {
                            final File destinationFile = this.checkPath(message, zipEntryName);
                            if (zipEntry.isDirectory()) {
                                destinationFile.mkdirs();
                            }
                            else {
                                mkDirOfAncestorDirectories(destinationFile);
                                SpringZipUtils.copy(zipEntryInputStream, destinationFile);
                                uncompressedData.put(zipEntryName, destinationFile);
                            }
                        }
                        else {
                            if (!ZipResultType.BYTE_ARRAY.equals(UnZipTransformer.this.zipResultType)) {
                                throw new IllegalStateException("Unsupported zipResultType " + UnZipTransformer.this.zipResultType);
                            }
                            if (!zipEntry.isDirectory()) {
                                this.checkPath(message, zipEntryName);
                                final byte[] data = IOUtils.toByteArray(zipEntryInputStream);
                                uncompressedData.put(zipEntryName, data);
                            }
                        }
                    }
                    
                    public File checkPath(final Message<?> message, final String zipEntryName) throws IOException {
                        final File tempDir = new File(UnZipTransformer.this.workDirectory, message.getHeaders().getId().toString());
                        tempDir.mkdirs();
                        final File destinationFile = new File(tempDir, zipEntryName);
                        if (!destinationFile.getCanonicalPath().startsWith(UnZipTransformer.this.workDirectory.getCanonicalPath())) {
                            throw new ZipException("The file " + zipEntryName + " is trying to leave the target output directory of " + UnZipTransformer.this.workDirectory);
                        }
                        return destinationFile;
                    }
                });
                if (uncompressedData.isEmpty()) {
                    if (UnZipTransformer.logger.isWarnEnabled()) {
                        UnZipTransformer.logger.warn((Object)("No data unzipped from payload with message Id " + message.getHeaders().getId()));
                    }
                    unzippedData = null;
                }
                else if (this.expectSingleResult) {
                    if (uncompressedData.size() != 1) {
                        throw new MessagingException((Message)message, String.format("The UnZip operation extracted %s result objects but expectSingleResult was 'true'.", uncompressedData.size()));
                    }
                    unzippedData = uncompressedData.values().iterator().next();
                }
                else {
                    unzippedData = uncompressedData;
                }
            }
            finally {
                IOUtils.closeQuietly(inputStream);
                if (payload instanceof File && this.deleteFiles) {
                    final File filePayload2 = (File)payload;
                    if (!filePayload2.delete() && UnZipTransformer.logger.isWarnEnabled() && UnZipTransformer.logger.isWarnEnabled()) {
                        UnZipTransformer.logger.warn((Object)("failed to delete File '" + filePayload2 + "'"));
                    }
                }
            }
            return unzippedData;
        }
        catch (Exception e) {
            throw new MessageHandlingException((Message)message, "Failed to apply Zip transformation.", (Throwable)e);
        }
    }
    
    private static void mkDirOfAncestorDirectories(final File destinationFile) {
        final File parentDirectory = destinationFile.getParentFile();
        if (parentDirectory != null) {
            parentDirectory.mkdirs();
        }
    }
    
    static {
        logger = LogFactory.getLog((Class)UnZipTransformer.class);
    }
}
