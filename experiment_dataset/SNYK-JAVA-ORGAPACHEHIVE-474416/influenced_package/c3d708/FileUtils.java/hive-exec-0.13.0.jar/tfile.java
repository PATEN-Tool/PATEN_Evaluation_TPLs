// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.hadoop.hive.common;

import org.apache.hadoop.util.Shell;
import org.apache.commons.logging.LogFactory;
import java.net.URISyntaxException;
import org.apache.hadoop.fs.LocalFileSystem;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.PathFilter;
import org.apache.hadoop.fs.FileStatus;
import java.util.List;
import java.io.IOException;
import java.net.URI;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import java.util.BitSet;
import org.apache.commons.logging.Log;

public final class FileUtils
{
    private static final Log LOG;
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
                    code = Integer.valueOf(path.substring(i + 1, i + 3), 16);
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
            for (final FileStatus stat : fs.listStatus(fileStatus.getPath(), (PathFilter)new PathFilter() {
                public boolean accept(final Path p) {
                    final String name = p.getName();
                    return !name.startsWith("_") && !name.startsWith(".");
                }
            })) {
                listStatusRecursively(fs, stat, results);
            }
        }
        else {
            results.add(fileStatus);
        }
    }
    
    public static Path getPathOrParentThatExists(final FileSystem fs, final Path path) throws IOException {
        if (!fs.exists(path)) {
            final Path parentPath = path.getParent();
            return getPathOrParentThatExists(fs, parentPath);
        }
        return path;
    }
    
    public static boolean isActionPermittedForUser(final String userName, final FileStatus fsStatus, final FsAction action) {
        final FsPermission permissions = fsStatus.getPermission();
        if (fsStatus.getOwner().equals(userName) && permissions.getUserAction().implies(action)) {
            return true;
        }
        if (permissions.getOtherAction().implies(action)) {
            return true;
        }
        final String fileGroup = fsStatus.getGroup();
        final String[] arr$;
        final String[] userGroups = arr$ = UserGroupInformation.createRemoteUser(userName).getGroupNames();
        for (final String group : arr$) {
            if (group.equals(fileGroup)) {
                return permissions.getGroupAction().implies(action);
            }
        }
        return false;
    }
    
    public static boolean isActionPermittedForFileHierarchy(final FileSystem fs, final FileStatus fileStatus, final String userName, final FsAction action) throws IOException {
        final boolean isDir = fileStatus.isDir();
        final FsAction dirActionNeeded = action;
        if (isDir) {
            dirActionNeeded.and(FsAction.EXECUTE);
        }
        if (!isActionPermittedForUser(userName, fileStatus, dirActionNeeded)) {
            return false;
        }
        if (!isDir) {
            return true;
        }
        final FileStatus[] arr$;
        final FileStatus[] childStatuses = arr$ = fs.listStatus(fileStatus.getPath());
        for (final FileStatus childStatus : arr$) {
            if (!isActionPermittedForFileHierarchy(fs, childStatus, userName, action)) {
                return false;
            }
        }
        return true;
    }
    
    public static boolean isLocalFile(final HiveConf conf, final String fileName) {
        try {
            final FileSystem fsForFile = FileSystem.get(new URI(fileName), (Configuration)conf);
            return LocalFileSystem.class.isInstance(fsForFile);
        }
        catch (URISyntaxException e) {
            FileUtils.LOG.warn((Object)("Unable to create URI from " + fileName), (Throwable)e);
        }
        catch (IOException e2) {
            FileUtils.LOG.warn((Object)("Unable to get FileSystem for " + fileName), (Throwable)e2);
        }
        return false;
    }
    
    public static boolean isOwnerOfFileHierarchy(final FileSystem fs, final FileStatus fileStatus, final String userName) throws IOException {
        if (!fileStatus.getOwner().equals(userName)) {
            return false;
        }
        if (!fileStatus.isDir()) {
            return true;
        }
        final FileStatus[] arr$;
        final FileStatus[] childStatuses = arr$ = fs.listStatus(fileStatus.getPath());
        for (final FileStatus childStatus : arr$) {
            if (!isOwnerOfFileHierarchy(fs, childStatus, userName)) {
                return false;
            }
        }
        return true;
    }
    
    static {
        LOG = LogFactory.getLog(FileUtils.class.getName());
        FileUtils.charToEscape = new BitSet(128);
        for (char c = '\0'; c < ' '; ++c) {
            FileUtils.charToEscape.set(c);
        }
        final char[] arr$;
        final char[] clist = arr$ = new char[] { '\u0001', '\u0002', '\u0003', '\u0004', '\u0005', '\u0006', '\u0007', '\b', '\t', '\n', '\u000b', '\f', '\r', '\u000e', '\u000f', '\u0010', '\u0011', '\u0012', '\u0013', '\u0014', '\u0015', '\u0016', '\u0017', '\u0018', '\u0019', '\u001a', '\u001b', '\u001c', '\u001d', '\u001e', '\u001f', '\"', '#', '%', '\'', '*', '/', ':', '=', '?', '\\', '\u007f', '{', '[', ']', '^' };
        for (final char c2 : arr$) {
            FileUtils.charToEscape.set(c2);
        }
        if (Shell.WINDOWS) {
            final char[] arr$2;
            final char[] winClist = arr$2 = new char[] { ' ', '<', '>', '|' };
            for (final char c3 : arr$2) {
                FileUtils.charToEscape.set(c3);
            }
        }
    }
}
