// 
// Decompiled by Procyon v0.5.36
// 

package org.eclipse.jetty.util.resource;

import org.eclipse.jetty.util.log.Log;
import java.nio.ByteBuffer;
import org.eclipse.jetty.util.BufferUtil;
import java.io.OutputStream;
import org.eclipse.jetty.util.IO;
import java.nio.file.CopyOption;
import java.util.Iterator;
import java.util.List;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryIteratorException;
import java.util.ArrayList;
import java.nio.file.attribute.FileTime;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.StandardOpenOption;
import java.nio.file.OpenOption;
import java.io.InputStream;
import java.nio.file.InvalidPathException;
import org.eclipse.jetty.util.StringUtil;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.io.IOError;
import java.io.File;
import java.nio.file.Files;
import java.io.IOException;
import java.nio.file.Paths;
import org.eclipse.jetty.util.URIUtil;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.LinkOption;
import org.eclipse.jetty.util.log.Logger;

public class PathResource extends Resource
{
    private static final Logger LOG;
    private static final LinkOption[] NO_FOLLOW_LINKS;
    private static final LinkOption[] FOLLOW_LINKS;
    private final Path path;
    private final Path alias;
    private final URI uri;
    private final boolean belongsToDefaultFileSystem;
    
    private final Path checkAliasPath() {
        Path abs = this.path;
        if (!URIUtil.equalsIgnoreEncodings(this.uri, this.path.toUri())) {
            try {
                return Paths.get(this.uri).toRealPath(PathResource.FOLLOW_LINKS);
            }
            catch (IOException ignored) {
                PathResource.LOG.ignore(ignored);
            }
        }
        if (!abs.isAbsolute()) {
            abs = this.path.toAbsolutePath();
        }
        try {
            if (Files.isSymbolicLink(this.path)) {
                return this.path.getParent().resolve(Files.readSymbolicLink(this.path));
            }
            if (Files.exists(this.path, new LinkOption[0])) {
                final Path real = abs.toRealPath(PathResource.FOLLOW_LINKS);
                if (!isSameName(abs, real)) {
                    return real;
                }
            }
        }
        catch (IOException e) {
            PathResource.LOG.ignore(e);
        }
        catch (Exception e2) {
            PathResource.LOG.warn("bad alias ({} {}) for {}", e2.getClass().getName(), e2.getMessage(), this.path);
        }
        return null;
    }
    
    public static boolean isSameName(final Path pathA, final Path pathB) {
        final int aCount = pathA.getNameCount();
        final int bCount = pathB.getNameCount();
        if (aCount != bCount) {
            return false;
        }
        int i = bCount;
        while (i-- > 0) {
            if (!pathA.getName(i).toString().equals(pathB.getName(i).toString())) {
                return false;
            }
        }
        return true;
    }
    
    public PathResource(final File file) {
        this(file.toPath());
    }
    
    public PathResource(final Path path) {
        Path absPath = path;
        try {
            absPath = path.toRealPath(PathResource.NO_FOLLOW_LINKS);
        }
        catch (IOError | IOException ioError) {
            final Throwable t;
            final Throwable e = t;
            if (PathResource.LOG.isDebugEnabled()) {
                PathResource.LOG.debug("Unable to get real/canonical path for {}", path, e);
            }
        }
        this.path = absPath.normalize();
        this.assertValidPath(path);
        this.uri = this.path.toUri();
        this.alias = this.checkAliasPath();
        this.belongsToDefaultFileSystem = (this.path.getFileSystem() == FileSystems.getDefault());
    }
    
    private PathResource(final PathResource parent, String childPath) {
        this.path = parent.path.getFileSystem().getPath(parent.path.toString(), childPath);
        if (this.isDirectory() && !childPath.endsWith("/")) {
            childPath += "/";
        }
        this.uri = URIUtil.addPath(parent.uri, childPath);
        this.alias = this.checkAliasPath();
        this.belongsToDefaultFileSystem = (this.path.getFileSystem() == FileSystems.getDefault());
    }
    
    public PathResource(final URI uri) throws IOException {
        if (!uri.isAbsolute()) {
            throw new IllegalArgumentException("not an absolute uri");
        }
        if (!uri.getScheme().equalsIgnoreCase("file")) {
            throw new IllegalArgumentException("not file: scheme");
        }
        Path path;
        try {
            path = Paths.get(uri);
        }
        catch (IllegalArgumentException e) {
            throw e;
        }
        catch (Exception e2) {
            PathResource.LOG.ignore(e2);
            throw new IOException("Unable to build Path from: " + uri, e2);
        }
        this.path = path.toAbsolutePath();
        this.uri = path.toUri();
        this.alias = this.checkAliasPath();
        this.belongsToDefaultFileSystem = (this.path.getFileSystem() == FileSystems.getDefault());
    }
    
    public PathResource(final URL url) throws IOException, URISyntaxException {
        this(url.toURI());
    }
    
    @Override
    public boolean isSame(final Resource resource) {
        try {
            if (resource instanceof PathResource) {
                final Path path = ((PathResource)resource).getPath();
                return Files.isSameFile(this.getPath(), path);
            }
            if (resource instanceof FileResource) {
                final Path path = ((FileResource)resource).getFile().toPath();
                return Files.isSameFile(this.getPath(), path);
            }
        }
        catch (IOException e) {
            if (PathResource.LOG.isDebugEnabled()) {
                PathResource.LOG.debug("ignored", e);
            }
        }
        return false;
    }
    
    @Override
    public Resource addPath(final String subpath) throws IOException {
        final String cpath = URIUtil.canonicalPath(subpath);
        if (cpath == null || cpath.length() == 0) {
            throw new MalformedURLException(subpath);
        }
        if ("/".equals(cpath)) {
            return this;
        }
        return new PathResource(this, subpath);
    }
    
    private void assertValidPath(final Path path) {
        final String str = path.toString();
        final int idx = StringUtil.indexOfControlChars(str);
        if (idx >= 0) {
            throw new InvalidPathException(str, "Invalid Character at index " + idx);
        }
    }
    
    @Override
    public void close() {
    }
    
    @Override
    public boolean delete() throws SecurityException {
        try {
            return Files.deleteIfExists(this.path);
        }
        catch (IOException e) {
            PathResource.LOG.ignore(e);
            return false;
        }
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        final PathResource other = (PathResource)obj;
        if (this.path == null) {
            return other.path == null;
        }
        return this.path.equals(other.path);
    }
    
    @Override
    public boolean exists() {
        return Files.exists(this.path, PathResource.NO_FOLLOW_LINKS);
    }
    
    @Override
    public File getFile() throws IOException {
        if (!this.belongsToDefaultFileSystem) {
            return null;
        }
        return this.path.toFile();
    }
    
    public Path getPath() {
        return this.path;
    }
    
    @Override
    public InputStream getInputStream() throws IOException {
        return Files.newInputStream(this.path, StandardOpenOption.READ);
    }
    
    @Override
    public String getName() {
        return this.path.toAbsolutePath().toString();
    }
    
    @Override
    public ReadableByteChannel getReadableByteChannel() throws IOException {
        return this.newSeekableByteChannel();
    }
    
    public SeekableByteChannel newSeekableByteChannel() throws IOException {
        return Files.newByteChannel(this.path, StandardOpenOption.READ);
    }
    
    @Override
    public URI getURI() {
        return this.uri;
    }
    
    @Override
    public URL getURL() {
        try {
            return this.path.toUri().toURL();
        }
        catch (MalformedURLException e) {
            return null;
        }
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = 31 * result + ((this.path == null) ? 0 : this.path.hashCode());
        return result;
    }
    
    @Override
    public boolean isContainedIn(final Resource r) throws MalformedURLException {
        return false;
    }
    
    @Override
    public boolean isDirectory() {
        return Files.isDirectory(this.path, PathResource.FOLLOW_LINKS);
    }
    
    @Override
    public long lastModified() {
        try {
            final FileTime ft = Files.getLastModifiedTime(this.path, PathResource.FOLLOW_LINKS);
            return ft.toMillis();
        }
        catch (IOException e) {
            PathResource.LOG.ignore(e);
            return 0L;
        }
    }
    
    @Override
    public long length() {
        try {
            return Files.size(this.path);
        }
        catch (IOException e) {
            return 0L;
        }
    }
    
    @Override
    public boolean isAlias() {
        return this.alias != null;
    }
    
    public Path getAliasPath() {
        return this.alias;
    }
    
    @Override
    public URI getAlias() {
        return (this.alias == null) ? null : this.alias.toUri();
    }
    
    @Override
    public String[] list() {
        try {
            final DirectoryStream<Path> dir = Files.newDirectoryStream(this.path);
            try {
                final List<String> entries = new ArrayList<String>();
                for (final Path entry : dir) {
                    String name = entry.getFileName().toString();
                    if (Files.isDirectory(entry, new LinkOption[0])) {
                        name += "/";
                    }
                    entries.add(name);
                }
                final int size = entries.size();
                final String[] array = entries.toArray(new String[size]);
                if (dir != null) {
                    dir.close();
                }
                return array;
            }
            catch (Throwable t) {
                if (dir != null) {
                    try {
                        dir.close();
                    }
                    catch (Throwable t2) {
                        t.addSuppressed(t2);
                    }
                }
                throw t;
            }
        }
        catch (DirectoryIteratorException | IOException ex2) {
            final Exception ex;
            final Exception e = ex;
            PathResource.LOG.debug(e);
            return null;
        }
    }
    
    @Override
    public boolean renameTo(final Resource dest) throws SecurityException {
        if (dest instanceof PathResource) {
            final PathResource destRes = (PathResource)dest;
            try {
                final Path result = Files.move(this.path, destRes.path, new CopyOption[0]);
                return Files.exists(result, PathResource.NO_FOLLOW_LINKS);
            }
            catch (IOException e) {
                PathResource.LOG.ignore(e);
                return false;
            }
        }
        return false;
    }
    
    @Override
    public void copyTo(final File destination) throws IOException {
        if (this.isDirectory()) {
            IO.copyDir(this.path.toFile(), destination);
        }
        else {
            Files.copy(this.path, destination.toPath(), new CopyOption[0]);
        }
    }
    
    @Override
    public void writeTo(final OutputStream outputStream, final long start, final long count) throws IOException {
        long length = count;
        if (count < 0L) {
            length = Files.size(this.path) - start;
        }
        final SeekableByteChannel channel = Files.newByteChannel(this.path, StandardOpenOption.READ);
        try {
            final ByteBuffer buffer = BufferUtil.allocate(65536);
            this.skipTo(channel, buffer, start);
            int readLen;
            for (long readTotal = 0L; readTotal < length; readTotal += readLen) {
                BufferUtil.clearToFill(buffer);
                final int size = (int)Math.min(65536L, length - readTotal);
                buffer.limit(size);
                readLen = channel.read(buffer);
                BufferUtil.flipToFlush(buffer, 0);
                BufferUtil.writeTo(buffer, outputStream);
            }
            if (channel != null) {
                channel.close();
            }
        }
        catch (Throwable t) {
            if (channel != null) {
                try {
                    channel.close();
                }
                catch (Throwable t2) {
                    t.addSuppressed(t2);
                }
            }
            throw t;
        }
    }
    
    private void skipTo(final SeekableByteChannel channel, final ByteBuffer buffer, final long skipTo) throws IOException {
        try {
            if (channel.position() != skipTo) {
                channel.position(skipTo);
            }
        }
        catch (UnsupportedOperationException e) {
            final int NO_PROGRESS_LIMIT = 3;
            if (skipTo > 0L) {
                long pos = 0L;
                int noProgressLoopLimit = 3;
                while (noProgressLoopLimit > 0 && pos < skipTo) {
                    BufferUtil.clearToFill(buffer);
                    final int len = (int)Math.min(65536L, skipTo - pos);
                    buffer.limit(len);
                    final long readLen = channel.read(buffer);
                    if (readLen == 0L) {
                        --noProgressLoopLimit;
                    }
                    else {
                        if (readLen <= 0L) {
                            throw new IOException("EOF reached before SeekableByteChannel skip destination");
                        }
                        pos += readLen;
                        noProgressLoopLimit = 3;
                    }
                }
                if (noProgressLoopLimit <= 0) {
                    throw new IOException("No progress made to reach SeekableByteChannel skip position " + skipTo);
                }
            }
        }
    }
    
    @Override
    public String toString() {
        return this.uri.toASCIIString();
    }
    
    static {
        LOG = Log.getLogger(PathResource.class);
        NO_FOLLOW_LINKS = new LinkOption[] { LinkOption.NOFOLLOW_LINKS };
        FOLLOW_LINKS = new LinkOption[0];
    }
}
