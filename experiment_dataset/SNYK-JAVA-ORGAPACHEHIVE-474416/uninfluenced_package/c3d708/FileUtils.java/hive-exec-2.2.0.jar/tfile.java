// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.hadoop.hive.common;

import org.apache.hadoop.util.Shell;
import org.slf4j.LoggerFactory;
import org.apache.hadoop.util.StringUtils;
import org.apache.hive.common.util.ShutdownHookManager;
import java.io.File;
import org.apache.hadoop.fs.Trash;
import org.apache.hadoop.hive.shims.HadoopShims;
import org.apache.hadoop.fs.FileUtil;
import java.io.FileNotFoundException;
import org.apache.hadoop.hive.io.HdfsUtils;
import org.apache.hadoop.fs.LocalFileSystem;
import java.net.URISyntaxException;
import org.apache.hadoop.hive.conf.HiveConf;
import java.security.AccessControlException;
import java.security.PrivilegedExceptionAction;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.hive.shims.ShimLoader;
import org.apache.hadoop.hive.shims.Utils;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.FileStatus;
import java.util.List;
import java.io.IOException;
import java.net.URI;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import java.util.BitSet;
import org.apache.hadoop.fs.PathFilter;
import java.util.Random;
import org.slf4j.Logger;

public final class FileUtils
{
    private static final Logger LOG;
    private static final Random random;
    public static final PathFilter HIDDEN_FILES_PATH_FILTER;
    public static final PathFilter STAGING_DIR_PATH_FILTER;
    static BitSet charToEscape;
    
    public static Path makeQualified(final Path path, final Configuration conf) throws IOException {
        if (!path.isAbsolute()) {
            return path.makeQualified(FileSystem.get(conf));
        }
        final URI fsUri = FileSystem.getDefaultUri(conf);
        final URI pathUri = path.toUri();
        String scheme = pathUri.getScheme();
        String authority = pathUri.getAuthority();
        if (scheme == null) {
            scheme = fsUri.getScheme();
            authority = fsUri.getAuthority();
            if (authority == null) {
                authority = "";
            }
        }
        else if (authority == null) {
            if (scheme.equals(fsUri.getScheme()) && fsUri.getAuthority() != null) {
                authority = fsUri.getAuthority();
            }
            else {
                authority = "";
            }
        }
        return new Path(scheme, authority, pathUri.getPath());
    }
    
    private FileUtils() {
    }
    
    public static String makePartName(final List<String> partCols, final List<String> vals) {
        return makePartName(partCols, vals, null);
    }
    
    public static String makePartName(final List<String> partCols, final List<String> vals, final String defaultStr) {
        final StringBuilder name = new StringBuilder();
        for (int i = 0; i < partCols.size(); ++i) {
            if (i > 0) {
                name.append("/");
            }
            name.append(escapePathName(partCols.get(i).toLowerCase(), defaultStr));
            name.append('=');
            name.append(escapePathName(vals.get(i), defaultStr));
        }
        return name.toString();
    }
    
    public static String makeDefaultListBucketingDirName(final List<String> skewedCols, final String name) {
        final String defaultDir = escapePathName(name);
        final StringBuilder defaultDirPath = new StringBuilder();
        for (int i = 0; i < skewedCols.size(); ++i) {
            if (i > 0) {
                defaultDirPath.append("/");
            }
            defaultDirPath.append(defaultDir);
        }
        final String lbDirName = defaultDirPath.toString();
        return lbDirName;
    }
    
    public static String makeListBucketingDirName(final List<String> lbCols, final List<String> vals) {
        final StringBuilder name = new StringBuilder();
        for (int i = 0; i < lbCols.size(); ++i) {
            if (i > 0) {
                name.append("/");
            }
            name.append(escapePathName(lbCols.get(i).toLowerCase()));
            name.append('=');
            name.append(escapePathName(vals.get(i)));
        }
        return name.toString();
    }
    
    static boolean needsEscaping(final char c) {
        return c >= '\0' && c < FileUtils.charToEscape.size() && FileUtils.charToEscape.get(c);
    }
    
    public static String escapePathName(final String path) {
        return escapePathName(path, null);
    }
    
    public static String escapePathName(final String path, final String defaultPath) {
        if (path != null && path.length() != 0) {
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < path.length(); ++i) {
                final char c = path.charAt(i);
                if (needsEscaping(c)) {
                    sb.append('%');
                    sb.append(String.format("%1$02X", (int)c));
                }
                else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }
        if (defaultPath == null) {
            return "__HIVE_DEFAULT_PARTITION__";
        }
        return defaultPath;
    }
    
    public static String unescapePathName(final String path) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < path.length(); ++i) {
            final char c = path.charAt(i);
            if (c == '%' && i + 2 < path.length()) {
                int code = -1;
                try {
                    code = Integer.parseInt(path.substring(i + 1, i + 3), 16);
                }
                catch (Exception e) {
                    code = -1;
                }
                if (code >= 0) {
                    sb.append((char)code);
                    i += 2;
                    continue;
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }
    
    public static void listStatusRecursively(final FileSystem fs, final FileStatus fileStatus, final List<FileStatus> results) throws IOException {
        if (fileStatus.isDir()) {
            for (final FileStatus stat : fs.listStatus(fileStatus.getPath(), FileUtils.HIDDEN_FILES_PATH_FILTER)) {
                listStatusRecursively(fs, stat, results);
            }
        }
        else {
            results.add(fileStatus);
        }
    }
    
    public static FileStatus getPathOrParentThatExists(final FileSystem fs, final Path path) throws IOException {
        final FileStatus stat = getFileStatusOrNull(fs, path);
        if (stat != null) {
            return stat;
        }
        final Path parentPath = path.getParent();
        return getPathOrParentThatExists(fs, parentPath);
    }
    
    public static void checkFileAccessWithImpersonation(final FileSystem fs, final FileStatus stat, final FsAction action, final String user) throws IOException, AccessControlException, InterruptedException, Exception {
        final UserGroupInformation ugi = Utils.getUGI();
        final String currentUser = ugi.getShortUserName();
        if (user == null || currentUser.equals(user)) {
            ShimLoader.getHadoopShims().checkFileAccess(fs, stat, action);
            return;
        }
        final UserGroupInformation proxyUser = UserGroupInformation.createProxyUser(user, UserGroupInformation.getLoginUser());
        try {
            proxyUser.doAs((PrivilegedExceptionAction)new PrivilegedExceptionAction<Object>() {
                @Override
                public Object run() throws Exception {
                    final FileSystem fsAsUser = FileSystem.get(fs.getUri(), fs.getConf());
                    ShimLoader.getHadoopShims().checkFileAccess(fsAsUser, stat, action);
                    return null;
                }
            });
        }
        finally {
            FileSystem.closeAllForUGI(proxyUser);
        }
    }
    
    public static boolean isActionPermittedForFileHierarchy(final FileSystem fs, final FileStatus fileStatus, final String userName, final FsAction action) throws Exception {
        return isActionPermittedForFileHierarchy(fs, fileStatus, userName, action, true);
    }
    
    public static boolean isActionPermittedForFileHierarchy(final FileSystem fs, final FileStatus fileStatus, final String userName, final FsAction action, final boolean recurse) throws Exception {
        final boolean isDir = fileStatus.isDir();
        final FsAction dirActionNeeded = action;
        if (isDir) {
            dirActionNeeded.and(FsAction.EXECUTE);
        }
        try {
            checkFileAccessWithImpersonation(fs, fileStatus, action, userName);
        }
        catch (AccessControlException err) {
            return false;
        }
        if (!isDir || !recurse) {
            return true;
        }
        final FileStatus[] listStatus;
        final FileStatus[] childStatuses = listStatus = fs.listStatus(fileStatus.getPath());
        for (final FileStatus childStatus : listStatus) {
            if (!isActionPermittedForFileHierarchy(fs, childStatus, userName, action, true)) {
                return false;
            }
        }
        return true;
    }
    
    public static boolean isLocalFile(final HiveConf conf, final String fileName) {
        try {
            return isLocalFile(conf, new URI(fileName));
        }
        catch (URISyntaxException e) {
            FileUtils.LOG.warn("Unable to create URI from " + fileName, (Throwable)e);
            return false;
        }
    }
    
    public static boolean isLocalFile(final HiveConf conf, final URI fileUri) {
        try {
            final FileSystem fsForFile = FileSystem.get(fileUri, (Configuration)conf);
            return LocalFileSystem.class.isInstance(fsForFile);
        }
        catch (IOException e) {
            FileUtils.LOG.warn("Unable to get FileSystem for " + fileUri, (Throwable)e);
            return false;
        }
    }
    
    public static boolean isOwnerOfFileHierarchy(final FileSystem fs, final FileStatus fileStatus, final String userName) throws IOException {
        return isOwnerOfFileHierarchy(fs, fileStatus, userName, true);
    }
    
    public static boolean isOwnerOfFileHierarchy(final FileSystem fs, final FileStatus fileStatus, final String userName, final boolean recurse) throws IOException {
        if (!fileStatus.getOwner().equals(userName)) {
            return false;
        }
        if (!fileStatus.isDir() || !recurse) {
            return true;
        }
        final FileStatus[] listStatus;
        final FileStatus[] childStatuses = listStatus = fs.listStatus(fileStatus.getPath());
        for (final FileStatus childStatus : listStatus) {
            if (!isOwnerOfFileHierarchy(fs, childStatus, userName, true)) {
                return false;
            }
        }
        return true;
    }
    
    public static boolean mkdir(final FileSystem fs, final Path f, final boolean inheritPerms, final Configuration conf) throws IOException {
        FileUtils.LOG.info("Creating directory if it doesn't exist: " + f);
        if (!inheritPerms) {
            return fs.mkdirs(f);
        }
        try {
            return fs.getFileStatus(f).isDir();
        }
        catch (FileNotFoundException ex) {
            Path lastExistingParent = f;
            Path firstNonExistentParent = null;
            while (!fs.exists(lastExistingParent)) {
                firstNonExistentParent = lastExistingParent;
                lastExistingParent = lastExistingParent.getParent();
            }
            final boolean success = fs.mkdirs(f);
            if (!success) {
                return false;
            }
            try {
                if (inheritPerms) {
                    HdfsUtils.setFullFileStatus(conf, new HdfsUtils.HadoopFileStatus(conf, fs, lastExistingParent), fs, firstNonExistentParent, true);
                }
            }
            catch (Exception e) {
                FileUtils.LOG.warn("Error setting permissions of " + firstNonExistentParent, (Throwable)e);
            }
            return true;
        }
    }
    
    public static boolean copy(final FileSystem srcFS, final Path src, final FileSystem dstFS, final Path dst, final boolean deleteSource, final boolean overwrite, final HiveConf conf) throws IOException {
        final HadoopShims shims = ShimLoader.getHadoopShims();
        boolean copied;
        if (srcFS.getUri().getScheme().equals("hdfs") && srcFS.getFileStatus(src).getLen() > conf.getLongVar(HiveConf.ConfVars.HIVE_EXEC_COPYFILE_MAXSIZE)) {
            FileUtils.LOG.info("Source is " + srcFS.getFileStatus(src).getLen() + " bytes. (MAX: " + conf.getLongVar(HiveConf.ConfVars.HIVE_EXEC_COPYFILE_MAXSIZE) + ")");
            FileUtils.LOG.info("Launch distributed copy (distcp) job.");
            copied = shims.runDistCp(src, dst, conf);
            if (copied && deleteSource) {
                srcFS.delete(src, true);
            }
        }
        else {
            copied = FileUtil.copy(srcFS, src, dstFS, dst, deleteSource, overwrite, (Configuration)conf);
        }
        final boolean inheritPerms = conf.getBoolVar(HiveConf.ConfVars.HIVE_WAREHOUSE_SUBDIR_INHERIT_PERMS);
        if (copied && inheritPerms) {
            try {
                HdfsUtils.setFullFileStatus(conf, new HdfsUtils.HadoopFileStatus(conf, dstFS, dst.getParent()), dstFS, dst, true);
            }
            catch (Exception e) {
                FileUtils.LOG.warn("Error setting permissions or group of " + dst, (Throwable)e);
            }
        }
        return copied;
    }
    
    public static boolean moveToTrash(final FileSystem fs, final Path f, final Configuration conf) throws IOException {
        FileUtils.LOG.debug("deleting  " + f);
        boolean result = false;
        try {
            result = Trash.moveToAppropriateTrash(fs, f, conf);
            if (result) {
                FileUtils.LOG.trace("Moved to trash: " + f);
                return true;
            }
        }
        catch (IOException ioe) {
            FileUtils.LOG.warn(ioe.getMessage() + "; Force to delete it.");
        }
        result = fs.delete(f, true);
        if (!result) {
            FileUtils.LOG.error("Failed to delete " + f);
        }
        return result;
    }
    
    public static boolean isSubDir(final Path p1, final Path p2, final FileSystem fs) {
        final String path1 = fs.makeQualified(p1).toString();
        final String path2 = fs.makeQualified(p2).toString();
        return path1.startsWith(path2);
    }
    
    public static boolean renameWithPerms(final FileSystem fs, final Path sourcePath, final Path destPath, final boolean inheritPerms, final Configuration conf) throws IOException {
        FileUtils.LOG.info("Renaming " + sourcePath + " to " + destPath);
        if (fs.exists(destPath)) {
            throw new IOException("Cannot rename the source path. The destination path already exists.");
        }
        if (!inheritPerms) {
            return fs.rename(sourcePath, destPath);
        }
        if (fs.rename(sourcePath, destPath)) {
            try {
                HdfsUtils.setFullFileStatus(conf, new HdfsUtils.HadoopFileStatus(conf, fs, destPath.getParent()), fs, destPath, true);
            }
            catch (Exception e) {
                FileUtils.LOG.warn("Error setting permissions or group of " + destPath, (Throwable)e);
            }
            return true;
        }
        return false;
    }
    
    public static boolean equalsFileSystem(final FileSystem fs1, final FileSystem fs2) {
        return fs1.getUri().equals(fs2.getUri());
    }
    
    public static void checkDeletePermission(final Path path, final Configuration conf, final String user) throws AccessControlException, InterruptedException, Exception {
        if (path == null) {
            return;
        }
        final FileSystem fs = path.getFileSystem(conf);
        FileStatus stat = null;
        try {
            stat = fs.getFileStatus(path);
        }
        catch (FileNotFoundException ex) {}
        if (stat == null) {
            return;
        }
        checkFileAccessWithImpersonation(fs, stat, FsAction.WRITE, user);
        final HadoopShims shims = ShimLoader.getHadoopShims();
        if (!shims.supportStickyBit()) {
            return;
        }
        final FileStatus parStatus = fs.getFileStatus(path.getParent());
        if (!shims.hasStickyBit(parStatus.getPermission())) {
            return;
        }
        if (parStatus.getOwner().equals(user)) {
            return;
        }
        final FileStatus childStatus = fs.getFileStatus(path);
        if (childStatus.getOwner().equals(user)) {
            return;
        }
        final String msg = String.format("Permission Denied: User %s can't delete %s because sticky bit is set on the parent dir and user does not own this file or its parent", user, path);
        throw new IOException(msg);
    }
    
    public static FileStatus getFileStatusOrNull(final FileSystem fs, final Path path) throws IOException {
        try {
            return fs.getFileStatus(path);
        }
        catch (FileNotFoundException e) {
            return null;
        }
    }
    
    public static void deleteDirectory(final File directory) throws IOException {
        org.apache.commons.io.FileUtils.deleteDirectory(directory);
    }
    
    public static File createTempFile(final String lScratchDir, final String prefix, final String suffix) throws IOException {
        final File tmpDir = (lScratchDir == null) ? null : new File(lScratchDir);
        if (tmpDir != null && !tmpDir.exists() && !tmpDir.mkdirs() && !tmpDir.exists()) {
            throw new RuntimeException("Unable to create temp directory " + lScratchDir);
        }
        final File tmpFile = File.createTempFile(prefix, suffix, tmpDir);
        ShutdownHookManager.deleteOnExit(tmpFile);
        return tmpFile;
    }
    
    public static File createLocalDirsTempFile(final String localDirList, final String prefix, final String suffix, final boolean isDirectory) throws IOException {
        if (localDirList == null || localDirList.isEmpty()) {
            return createFileInTmp(prefix, suffix, "Local directories not specified", isDirectory);
        }
        final String[] localDirs = StringUtils.getTrimmedStrings(localDirList);
        if (localDirs.length == 0) {
            return createFileInTmp(prefix, suffix, "Local directories not specified", isDirectory);
        }
        final String path = localDirs[FileUtils.random.nextInt(localDirs.length)];
        if (path == null || path.isEmpty()) {
            return createFileInTmp(prefix, suffix, "Empty path for one of the local dirs", isDirectory);
        }
        final File targetDir = new File(path);
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            return createFileInTmp(prefix, suffix, "Cannot access or create " + targetDir, isDirectory);
        }
        try {
            final File file = File.createTempFile(prefix, suffix, targetDir);
            if (isDirectory && (!file.delete() || !file.mkdirs())) {
                return createFileInTmp(prefix, suffix, "Cannot recreate " + file + " as directory", isDirectory);
            }
            file.deleteOnExit();
            return file;
        }
        catch (IOException ex) {
            FileUtils.LOG.error("Error creating a file in " + targetDir, (Throwable)ex);
            return createFileInTmp(prefix, suffix, "Cannot create a file in " + targetDir, isDirectory);
        }
    }
    
    private static File createFileInTmp(final String prefix, final String suffix, final String reason, final boolean isDirectory) throws IOException {
        final File file = File.createTempFile(prefix, suffix);
        if (isDirectory && (!file.delete() || !file.mkdirs())) {
            throw new IOException("Cannot recreate " + file + " as directory");
        }
        file.deleteOnExit();
        FileUtils.LOG.info(reason + "; created a tmp file: " + file.getAbsolutePath());
        return file;
    }
    
    public static File createLocalDirsTempFile(final Configuration conf, final String prefix, final String suffix, final boolean isDirectory) throws IOException {
        return createLocalDirsTempFile(conf.get("yarn.nodemanager.local-dirs"), prefix, suffix, isDirectory);
    }
    
    public static boolean deleteTmpFile(final File tempFile) {
        if (tempFile != null) {
            tempFile.delete();
            ShutdownHookManager.cancelDeleteOnExit(tempFile);
            return true;
        }
        return false;
    }
    
    public static boolean isPathWithinSubtree(final Path path, final Path subtree) {
        return isPathWithinSubtree(path, subtree, subtree.depth());
    }
    
    private static boolean isPathWithinSubtree(Path path, final Path subtree, final int subtreeDepth) {
        while (path != null) {
            if (subtreeDepth > path.depth()) {
                return false;
            }
            if (subtree.equals((Object)path)) {
                return true;
            }
            path = path.getParent();
        }
        return false;
    }
    
    static {
        LOG = LoggerFactory.getLogger(FileUtils.class.getName());
        random = new Random();
        HIDDEN_FILES_PATH_FILTER = (PathFilter)new PathFilter() {
            public boolean accept(final Path p) {
                final String name = p.getName();
                return !name.startsWith("_") && !name.startsWith(".");
            }
        };
        STAGING_DIR_PATH_FILTER = (PathFilter)new PathFilter() {
            public boolean accept(final Path p) {
                final String name = p.getName();
                return !name.startsWith(".");
            }
        };
        FileUtils.charToEscape = new BitSet(128);
        for (char c = '\0'; c < ' '; ++c) {
            FileUtils.charToEscape.set(c);
        }
        final char[] array;
        final char[] clist = array = new char[] { '\u0001', '\u0002', '\u0003', '\u0004', '\u0005', '\u0006', '\u0007', '\b', '\t', '\n', '\u000b', '\f', '\r', '\u000e', '\u000f', '\u0010', '\u0011', '\u0012', '\u0013', '\u0014', '\u0015', '\u0016', '\u0017', '\u0018', '\u0019', '\u001a', '\u001b', '\u001c', '\u001d', '\u001e', '\u001f', '\"', '#', '%', '\'', '*', '/', ':', '=', '?', '\\', '\u007f', '{', '[', ']', '^' };
        for (final char c2 : array) {
            FileUtils.charToEscape.set(c2);
        }
        if (Shell.WINDOWS) {
            final char[] array2;
            final char[] winClist = array2 = new char[] { ' ', '<', '>', '|' };
            for (final char c3 : array2) {
                FileUtils.charToEscape.set(c3);
            }
        }
    }
}
