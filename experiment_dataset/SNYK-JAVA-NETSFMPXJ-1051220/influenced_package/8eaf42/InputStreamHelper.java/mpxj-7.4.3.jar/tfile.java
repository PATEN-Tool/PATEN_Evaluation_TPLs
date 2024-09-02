// 
// Decompiled by Procyon v0.5.36
// 

package net.sf.mpxj.common;

import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipException;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.File;
import java.io.InputStream;

public class InputStreamHelper
{
    public static File writeStreamToTempFile(final InputStream inputStream, final String tempFileSuffix) throws IOException {
        FileOutputStream outputStream = null;
        try {
            final File file = File.createTempFile("mpxj", tempFileSuffix);
            outputStream = new FileOutputStream(file);
            final byte[] buffer = new byte[1024];
            while (true) {
                final int bytesRead = inputStream.read(buffer);
                if (bytesRead == -1) {
                    break;
                }
                outputStream.write(buffer, 0, bytesRead);
            }
            return file;
        }
        finally {
            if (outputStream != null) {
                outputStream.close();
            }
        }
    }
    
    public static File writeZipStreamToTempDir(final InputStream inputStream) throws IOException {
        final File dir = FileHelper.createTempDir();
        try {
            processZipStream(dir, inputStream);
        }
        catch (ZipException ex) {
            if (!ex.getMessage().equals("only DEFLATED entries can have EXT descriptor")) {
                throw ex;
            }
        }
        return dir;
    }
    
    private static void processZipStream(final File dir, final InputStream inputStream) throws IOException {
        final ZipInputStream zip = new ZipInputStream(inputStream);
        while (true) {
            final ZipEntry entry = zip.getNextEntry();
            if (entry == null) {
                break;
            }
            final File file = new File(dir, entry.getName());
            if (entry.isDirectory()) {
                FileHelper.mkdirsQuietly(file);
            }
            else {
                final File parent = file.getParentFile();
                if (parent != null) {
                    FileHelper.mkdirsQuietly(parent);
                }
                final FileOutputStream fos = new FileOutputStream(file);
                final byte[] bytes = new byte[1024];
                int length;
                while ((length = zip.read(bytes)) >= 0) {
                    fos.write(bytes, 0, length);
                }
                fos.close();
            }
        }
    }
}
