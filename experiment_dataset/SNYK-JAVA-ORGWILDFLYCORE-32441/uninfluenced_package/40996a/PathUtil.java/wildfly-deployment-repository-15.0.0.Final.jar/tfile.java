// 
// Decompiled by Procyon v0.5.36
// 

package org.jboss.as.repository;

import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileAttribute;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.io.FileNotFoundException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipInputStream;
import java.io.InputStream;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.nio.file.LinkOption;
import org.jboss.as.repository.logging.DeploymentRepositoryLogger;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.FileVisitResult;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.FileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.CopyOption;
import java.nio.file.Path;

public class PathUtil
{
    public static void copyRecursively(final Path source, final Path target, final boolean overwrite) throws IOException {
        CopyOption[] options;
        if (overwrite) {
            options = new CopyOption[] { StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING };
        }
        else {
            options = new CopyOption[] { StandardCopyOption.COPY_ATTRIBUTES };
        }
        Files.walkFileTree(source, new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
                Files.copy(dir, target.resolve(source.relativize(dir)), options);
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                Files.copy(file, target.resolve(source.relativize(file)), options);
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult visitFileFailed(final Path file, final IOException exc) throws IOException {
                DeploymentRepositoryLogger.ROOT_LOGGER.cannotCopyFile(exc, file);
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }
        });
    }
    
    public static void deleteSilentlyRecursively(final Path path) {
        if (path != null) {
            try {
                deleteRecursively(path);
            }
            catch (IOException ioex) {
                DeploymentRepositoryLogger.ROOT_LOGGER.cannotDeleteFile(ioex, path);
            }
        }
    }
    
    public static void deleteRecursively(final Path path) throws IOException {
        DeploymentRepositoryLogger.ROOT_LOGGER.debugf("Deleting %s recursively", (Object)path);
        if (Files.exists(path, new LinkOption[0])) {
            Files.walkFileTree(path, new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult visitFileFailed(final Path file, final IOException exc) throws IOException {
                    DeploymentRepositoryLogger.ROOT_LOGGER.cannotDeleteFile(exc, path);
                    throw exc;
                }
                
                @Override
                public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }
    
    public static final Path resolveSecurely(final Path rootPath, final String path) {
        Path resolvedPath;
        if (path == null || path.isEmpty()) {
            resolvedPath = rootPath.normalize();
        }
        else {
            final String relativePath = removeSuperflousSlashes(path);
            resolvedPath = rootPath.resolve(relativePath).normalize();
        }
        if (!resolvedPath.startsWith(rootPath)) {
            throw DeploymentRepositoryLogger.ROOT_LOGGER.forbiddenPath(path);
        }
        return resolvedPath;
    }
    
    private static String removeSuperflousSlashes(final String path) {
        if (path.startsWith("/")) {
            return removeSuperflousSlashes(path.substring(1));
        }
        return path;
    }
    
    public static final boolean isArchive(final Path path) throws IOException {
        if (Files.exists(path, new LinkOption[0]) && Files.isRegularFile(path, new LinkOption[0])) {
            try (final ZipFile zip = new ZipFile(path.toFile())) {
                return true;
            }
            catch (ZipException e) {
                return false;
            }
        }
        return false;
    }
    
    public static final boolean isArchive(final InputStream in) throws IOException {
        if (in != null) {
            try (final ZipInputStream zip = new ZipInputStream(in)) {
                return zip.getNextEntry() != null;
            }
            catch (ZipException e) {
                return false;
            }
        }
        return false;
    }
    
    public static List<ContentRepositoryElement> listFiles(final Path rootPath, final Path tempDir, final ContentFilter filter) throws IOException {
        final List<ContentRepositoryElement> result = new ArrayList<ContentRepositoryElement>();
        if (Files.exists(rootPath, new LinkOption[0])) {
            if (isArchive(rootPath)) {
                return listZipContent(rootPath, filter);
            }
            Files.walkFileTree(rootPath, new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                    if (filter.acceptFile(rootPath, file)) {
                        result.add(ContentRepositoryElement.createFile(this.formatPath(rootPath.relativize(file)), Files.size(file)));
                    }
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult visitFileFailed(final Path file, final IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
                    if (filter.acceptDirectory(rootPath, dir)) {
                        final String directoryPath = this.formatDirectoryPath(rootPath.relativize(dir));
                        if (!"/".equals(directoryPath)) {
                            result.add(ContentRepositoryElement.createFolder(directoryPath));
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
                
                private String formatDirectoryPath(final Path path) {
                    return this.formatPath(path) + '/';
                }
                
                private String formatPath(final Path path) {
                    return path.toString().replace(File.separatorChar, '/');
                }
            });
            return result;
        }
        else {
            final Path file = getFile(rootPath);
            if (isArchive(file)) {
                final Path relativePath = file.relativize(rootPath);
                final Path target = createTempDirectory(tempDir, "unarchive");
                unzip(file, target);
                return listFiles(target.resolve(relativePath), tempDir, filter);
            }
            throw new FileNotFoundException(rootPath.toString());
        }
    }
    
    private static List<ContentRepositoryElement> listZipContent(final Path zipFilePath, final ContentFilter filter) throws IOException {
        final List<ContentRepositoryElement> result = new ArrayList<ContentRepositoryElement>();
        try (final ZipFile zip = new ZipFile(zipFilePath.toFile())) {
            final Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                final ZipEntry entry = (ZipEntry)entries.nextElement();
                final String name = entry.getName();
                final Path entryPath = zipFilePath.resolve(name);
                if (entry.isDirectory()) {
                    if (!filter.acceptDirectory(zipFilePath, entryPath)) {
                        continue;
                    }
                    result.add(ContentRepositoryElement.createFolder(name));
                }
                else {
                    try (final InputStream in = zip.getInputStream(entry)) {
                        if (filter.acceptFile(zipFilePath, entryPath, in)) {
                            result.add(ContentRepositoryElement.createFile(name, entry.getSize()));
                        }
                    }
                }
            }
        }
        return result;
    }
    
    public static Path createTempDirectory(final Path dir, final String prefix) throws IOException {
        try {
            return Files.createTempDirectory(dir, prefix, (FileAttribute<?>[])new FileAttribute[0]);
        }
        catch (UnsupportedOperationException ex) {
            return Files.createTempDirectory(dir, prefix, (FileAttribute<?>[])new FileAttribute[0]);
        }
    }
    
    public static void unzip(final Path zip, final Path target) throws IOException {
        try (final ZipFile zipFile = new ZipFile(zip.toFile())) {
            unzip(zipFile, target);
        }
    }
    
    private static void unzip(final ZipFile zip, final Path targetDir) throws IOException {
        final Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            final ZipEntry entry = (ZipEntry)entries.nextElement();
            final String name = entry.getName();
            final Path current = resolveSecurely(targetDir, name);
            if (entry.isDirectory()) {
                if (!Files.exists(current, new LinkOption[0])) {
                    Files.createDirectories(current, (FileAttribute<?>[])new FileAttribute[0]);
                }
            }
            else {
                if (Files.notExists(current.getParent(), new LinkOption[0])) {
                    Files.createDirectories(current.getParent(), (FileAttribute<?>[])new FileAttribute[0]);
                }
                try (final InputStream eis = zip.getInputStream(entry)) {
                    Files.copy(eis, current, new CopyOption[0]);
                }
            }
            try {
                Files.getFileAttributeView(current, BasicFileAttributeView.class, new LinkOption[0]).setTimes(entry.getLastModifiedTime(), entry.getLastAccessTime(), entry.getCreationTime());
            }
            catch (IOException ex) {}
        }
    }
    
    public static String getFileExtension(final Path path) {
        final String fileName = path.getFileName().toString();
        final int separator = fileName.lastIndexOf(46);
        if (separator > 0) {
            return fileName.substring(separator);
        }
        return "";
    }
    
    public static Path readFile(final Path src, final Path tempDir) throws IOException {
        if (isFile(src)) {
            return src;
        }
        final Path file = getFile(src);
        if (isArchive(file)) {
            final Path relativePath = file.relativize(src);
            final Path target = createTempDirectory(tempDir, "unarchive");
            unzip(file, target);
            return readFile(target.resolve(relativePath), tempDir);
        }
        throw new FileNotFoundException(src.toString());
    }
    
    private static Path getFile(final Path src) throws FileNotFoundException {
        if (src.getNameCount() <= 1) {
            throw new FileNotFoundException(src.toString());
        }
        final Path parent = src.getParent();
        if (isFile(parent)) {
            return parent;
        }
        return getFile(parent);
    }
    
    private static boolean isFile(final Path src) {
        return Files.exists(src, new LinkOption[0]) && Files.isRegularFile(src, new LinkOption[0]);
    }
}
