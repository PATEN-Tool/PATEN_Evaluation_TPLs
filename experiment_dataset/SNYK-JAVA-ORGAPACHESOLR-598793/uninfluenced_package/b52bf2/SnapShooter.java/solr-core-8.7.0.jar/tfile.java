// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.solr.handler;

import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.lucene.store.Directory;
import java.util.Collection;
import org.apache.solr.core.DirectoryFactory;
import java.util.function.Consumer;
import java.util.Optional;
import org.apache.solr.core.snapshots.SolrSnapshotMetaDataManager;
import org.apache.solr.core.IndexDeletionPolicyWrapper;
import org.apache.lucene.index.IndexCommit;
import org.apache.solr.common.util.NamedList;
import java.io.IOException;
import org.apache.solr.common.SolrException;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects;
import java.nio.file.Paths;
import org.apache.solr.core.backup.repository.LocalFileSystemRepository;
import org.apache.solr.core.backup.repository.BackupRepository;
import java.net.URI;
import org.apache.solr.core.SolrCore;
import org.slf4j.Logger;

public class SnapShooter
{
    private static final Logger log;
    private SolrCore solrCore;
    private String snapshotName;
    private String directoryName;
    private URI baseSnapDirPath;
    private URI snapshotDirPath;
    private BackupRepository backupRepo;
    private String commitName;
    public static final String DATE_FMT = "yyyyMMddHHmmssSSS";
    
    @Deprecated
    public SnapShooter(final SolrCore core, final String location, final String snapshotName) {
        this.snapshotName = null;
        this.directoryName = null;
        this.baseSnapDirPath = null;
        this.snapshotDirPath = null;
        this.backupRepo = null;
        String snapDirStr = null;
        if (location == null) {
            snapDirStr = core.getDataDir();
        }
        else {
            snapDirStr = core.getCoreDescriptor().getInstanceDir().resolve(location).normalize().toString();
        }
        this.initialize(new LocalFileSystemRepository(), core, Paths.get(snapDirStr, new String[0]).toUri(), snapshotName, null);
    }
    
    public SnapShooter(final BackupRepository backupRepo, final SolrCore core, final URI location, final String snapshotName, final String commitName) {
        this.snapshotName = null;
        this.directoryName = null;
        this.baseSnapDirPath = null;
        this.snapshotDirPath = null;
        this.backupRepo = null;
        this.initialize(backupRepo, core, location, snapshotName, commitName);
    }
    
    private void initialize(final BackupRepository backupRepo, final SolrCore core, final URI location, final String snapshotName, final String commitName) {
        this.solrCore = Objects.requireNonNull(core);
        this.backupRepo = Objects.requireNonNull(backupRepo);
        this.baseSnapDirPath = location;
        this.snapshotName = snapshotName;
        if ("file".equals(location.getScheme())) {
            this.solrCore.getCoreContainer().assertPathAllowed(Paths.get(location));
        }
        if (snapshotName != null) {
            this.directoryName = "snapshot." + snapshotName;
        }
        else {
            final SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.ROOT);
            this.directoryName = "snapshot." + fmt.format(new Date());
        }
        this.snapshotDirPath = backupRepo.resolve(location, this.directoryName);
        this.commitName = commitName;
    }
    
    public BackupRepository getBackupRepository() {
        return this.backupRepo;
    }
    
    public URI getLocation() {
        return this.baseSnapDirPath;
    }
    
    public void validateDeleteSnapshot() {
        Objects.requireNonNull(this.snapshotName);
        boolean dirFound = false;
        try {
            final String[] listAll;
            final String[] paths = listAll = this.backupRepo.listAll(this.baseSnapDirPath);
            for (final String path : listAll) {
                if (path.equals(this.directoryName) && this.backupRepo.getPathType(this.baseSnapDirPath.resolve(path)) == BackupRepository.PathType.DIRECTORY) {
                    dirFound = true;
                    break;
                }
            }
            if (!dirFound) {
                throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Snapshot " + this.snapshotName + " cannot be found in directory: " + this.baseSnapDirPath);
            }
        }
        catch (IOException e) {
            throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Unable to find snapshot " + this.snapshotName + " in directory: " + this.baseSnapDirPath, (Throwable)e);
        }
    }
    
    protected void deleteSnapAsync(final ReplicationHandler replicationHandler) {
        new Thread(() -> this.deleteNamedSnapshot(replicationHandler)).start();
    }
    
    public void validateCreateSnapshot() throws IOException {
        if (!this.backupRepo.exists(this.baseSnapDirPath)) {
            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, " Directory does not exist: " + this.snapshotDirPath);
        }
        if (this.backupRepo.exists(this.snapshotDirPath)) {
            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Snapshot directory already exists: " + this.snapshotDirPath);
        }
    }
    
    public NamedList createSnapshot() throws Exception {
        final IndexCommit indexCommit = this.getAndSaveIndexCommit();
        try {
            return this.createSnapshot(indexCommit);
        }
        finally {
            this.solrCore.getDeletionPolicy().releaseCommitPoint(indexCommit.getGeneration());
        }
    }
    
    private IndexCommit getAndSaveIndexCommit() throws IOException {
        final IndexDeletionPolicyWrapper delPolicy = this.solrCore.getDeletionPolicy();
        if (null != this.commitName) {
            final SolrSnapshotMetaDataManager snapshotMgr = this.solrCore.getSnapshotMetaDataManager();
            synchronized (delPolicy) {
                final Optional<IndexCommit> namedCommit = snapshotMgr.getIndexCommitByName(this.commitName);
                if (namedCommit.isPresent()) {
                    final IndexCommit commit = namedCommit.get();
                    if (SnapShooter.log.isDebugEnabled()) {
                        SnapShooter.log.debug("Using named commit: name={}, generation={}", (Object)this.commitName, (Object)commit.getGeneration());
                    }
                    delPolicy.saveCommitPoint(commit.getGeneration());
                    return commit;
                }
            }
            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Unable to find an index commit with name " + this.commitName + " for core " + this.solrCore.getName());
        }
        final IndexCommit commit2 = delPolicy.getAndSaveLatestCommit();
        if (null == commit2) {
            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Index does not yet have any commits for core " + this.solrCore.getName());
        }
        if (SnapShooter.log.isDebugEnabled()) {
            SnapShooter.log.debug("Using latest commit: generation={}", (Object)commit2.getGeneration());
        }
        return commit2;
    }
    
    public void createSnapAsync(final int numberToKeep, final Consumer<NamedList> result) throws IOException {
        NamedList snapShootDetails;
        new Thread(() -> {
            try {
                snapShootDetails = this.createSnapshot();
            }
            catch (Exception e) {
                SnapShooter.log.error("Exception while creating snapshot", (Throwable)e);
                snapShootDetails = new NamedList();
                snapShootDetails.add("exception", (Object)e.getMessage());
            }
            if (this.snapshotName == null) {
                try {
                    this.deleteOldBackups(numberToKeep);
                }
                catch (IOException e2) {
                    SnapShooter.log.warn("Unable to delete old snapshots ", (Throwable)e2);
                }
            }
            if (null != snapShootDetails) {
                result.accept(snapShootDetails);
            }
        }).start();
    }
    
    protected NamedList createSnapshot(final IndexCommit indexCommit) throws Exception {
        assert indexCommit != null;
        if (SnapShooter.log.isInfoEnabled()) {
            SnapShooter.log.info("Creating backup snapshot {} at {}", (Object)((this.snapshotName == null) ? "<not named>" : this.snapshotName), (Object)this.baseSnapDirPath);
        }
        boolean success = false;
        try {
            final NamedList<Object> details = (NamedList<Object>)new NamedList();
            details.add("startTime", (Object)new Date().toString());
            final Collection<String> files = (Collection<String>)indexCommit.getFileNames();
            final Directory dir = this.solrCore.getDirectoryFactory().get(this.solrCore.getIndexDir(), DirectoryFactory.DirContext.DEFAULT, this.solrCore.getSolrConfig().indexConfig.lockType);
            try {
                for (final String fileName : files) {
                    SnapShooter.log.debug("Copying fileName={} from dir={} to snapshot={}", new Object[] { fileName, dir, this.snapshotDirPath });
                    this.backupRepo.copyFileFrom(dir, fileName, this.snapshotDirPath);
                }
            }
            finally {
                this.solrCore.getDirectoryFactory().release(dir);
            }
            details.add("fileCount", (Object)files.size());
            details.add("status", (Object)"success");
            details.add("snapshotCompletedAt", (Object)new Date().toString());
            details.add("snapshotName", (Object)this.snapshotName);
            details.add("directoryName", (Object)this.directoryName);
            if (SnapShooter.log.isInfoEnabled()) {
                SnapShooter.log.info("Done creating backup snapshot: {} into {}", (Object)((this.snapshotName == null) ? "<not named>" : this.snapshotName), (Object)this.snapshotDirPath);
            }
            success = true;
            return details;
        }
        finally {
            if (!success) {
                try {
                    this.backupRepo.deleteDirectory(this.snapshotDirPath);
                }
                catch (Exception excDuringDelete) {
                    SnapShooter.log.warn("Failed to delete {} after snapshot creation failed due to: {}", (Object)this.snapshotDirPath, (Object)excDuringDelete);
                }
            }
        }
    }
    
    private void deleteOldBackups(final int numberToKeep) throws IOException {
        final String[] paths = this.backupRepo.listAll(this.baseSnapDirPath);
        final List<OldBackupDirectory> dirs = new ArrayList<OldBackupDirectory>();
        for (final String f : paths) {
            if (this.backupRepo.getPathType(this.baseSnapDirPath.resolve(f)) == BackupRepository.PathType.DIRECTORY) {
                final OldBackupDirectory obd = new OldBackupDirectory(this.baseSnapDirPath, f);
                if (obd.getTimestamp().isPresent()) {
                    dirs.add(obd);
                }
            }
        }
        if (numberToKeep > dirs.size() - 1) {
            return;
        }
        Collections.sort(dirs);
        int i = 1;
        for (final OldBackupDirectory dir : dirs) {
            if (i++ > numberToKeep) {
                this.backupRepo.deleteDirectory(dir.getPath());
            }
        }
    }
    
    protected void deleteNamedSnapshot(final ReplicationHandler replicationHandler) {
        SnapShooter.log.info("Deleting snapshot: {}", (Object)this.snapshotName);
        final NamedList<Object> details = (NamedList<Object>)new NamedList();
        details.add("snapshotName", (Object)this.snapshotName);
        try {
            final URI path = this.baseSnapDirPath.resolve("snapshot." + this.snapshotName);
            this.backupRepo.deleteDirectory(path);
            details.add("status", (Object)"success");
            details.add("snapshotDeletedAt", (Object)new Date().toString());
        }
        catch (IOException e) {
            details.add("status", (Object)("Unable to delete snapshot: " + this.snapshotName));
            SnapShooter.log.warn("Unable to delete snapshot: {}", (Object)this.snapshotName, (Object)e);
        }
        replicationHandler.snapShootDetails = details;
    }
    
    static {
        log = LoggerFactory.getLogger((Class)MethodHandles.lookup().lookupClass());
    }
}
