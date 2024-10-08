// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.solr.handler;

import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;
import org.apache.solr.search.SolrIndexSearcher;
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
        if (this.commitName != null) {
            final IndexCommit indexCommit = this.getIndexCommitFromName();
            return this.createSnapshot(indexCommit);
        }
        final IndexCommit indexCommit = this.getIndexCommit();
        final IndexDeletionPolicyWrapper deletionPolicy = this.solrCore.getDeletionPolicy();
        deletionPolicy.saveCommitPoint(indexCommit.getGeneration());
        try {
            return this.createSnapshot(indexCommit);
        }
        finally {
            deletionPolicy.releaseCommitPoint(indexCommit.getGeneration());
        }
    }
    
    private IndexCommit getIndexCommit() throws IOException {
        final IndexDeletionPolicyWrapper delPolicy = this.solrCore.getDeletionPolicy();
        final IndexCommit indexCommit = delPolicy.getLatestCommit();
        if (indexCommit != null) {
            return indexCommit;
        }
        return this.solrCore.withSearcher(searcher -> searcher.getIndexReader().getIndexCommit());
    }
    
    private IndexCommit getIndexCommitFromName() throws IOException {
        assert this.commitName != null;
        final SolrSnapshotMetaDataManager snapshotMgr = this.solrCore.getSnapshotMetaDataManager();
        final Optional<IndexCommit> commit = snapshotMgr.getIndexCommitByName(this.commitName);
        if (commit.isPresent()) {
            final IndexCommit indexCommit = commit.get();
            return indexCommit;
        }
        throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Unable to find an index commit with name " + this.commitName + " for core " + this.solrCore.getName());
    }
    
    public void createSnapAsync(final int numberToKeep, final Consumer<NamedList> result) throws IOException {
        IndexCommit indexCommit;
        if (this.commitName != null) {
            indexCommit = this.getIndexCommitFromName();
        }
        else {
            indexCommit = this.getIndexCommit();
        }
        this.createSnapAsync(indexCommit, numberToKeep, result);
    }
    
    private void createSnapAsync(final IndexCommit indexCommit, final int numberToKeep, final Consumer<NamedList> result) {
        NamedList snapShootDetails;
        new Thread(() -> {
            try {
                result.accept(this.createSnapshot(indexCommit));
            }
            catch (Exception e) {
                SnapShooter.log.error("Exception while creating snapshot", (Throwable)e);
                snapShootDetails = new NamedList();
                snapShootDetails.add("exception", (Object)e.getMessage());
                result.accept(snapShootDetails);
            }
            finally {
                this.solrCore.getDeletionPolicy().releaseCommitPoint(indexCommit.getGeneration());
            }
            if (this.snapshotName == null) {
                try {
                    this.deleteOldBackups(numberToKeep);
                }
                catch (IOException e2) {
                    SnapShooter.log.warn("Unable to delete old snapshots ", (Throwable)e2);
                }
            }
        }).start();
    }
    
    protected NamedList createSnapshot(final IndexCommit indexCommit) throws Exception {
        assert indexCommit != null;
        SnapShooter.log.info("Creating backup snapshot " + ((this.snapshotName == null) ? "<not named>" : this.snapshotName) + " at " + this.baseSnapDirPath);
        boolean success = false;
        try {
            final NamedList<Object> details = (NamedList<Object>)new NamedList();
            details.add("startTime", (Object)new Date().toString());
            final Collection<String> files = (Collection<String>)indexCommit.getFileNames();
            final Directory dir = this.solrCore.getDirectoryFactory().get(this.solrCore.getIndexDir(), DirectoryFactory.DirContext.DEFAULT, this.solrCore.getSolrConfig().indexConfig.lockType);
            try {
                for (final String fileName : files) {
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
            SnapShooter.log.info("Done creating backup snapshot: " + ((this.snapshotName == null) ? "<not named>" : this.snapshotName) + " at " + this.baseSnapDirPath);
            success = true;
            return details;
        }
        finally {
            if (!success) {
                try {
                    this.backupRepo.deleteDirectory(this.snapshotDirPath);
                }
                catch (Exception excDuringDelete) {
                    SnapShooter.log.warn("Failed to delete " + this.snapshotDirPath + " after snapshot creation failed due to: " + excDuringDelete);
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
        SnapShooter.log.info("Deleting snapshot: " + this.snapshotName);
        final NamedList<Object> details = (NamedList<Object>)new NamedList();
        try {
            final URI path = this.baseSnapDirPath.resolve("snapshot." + this.snapshotName);
            this.backupRepo.deleteDirectory(path);
            details.add("status", (Object)"success");
            details.add("snapshotDeletedAt", (Object)new Date().toString());
        }
        catch (IOException e) {
            details.add("status", (Object)("Unable to delete snapshot: " + this.snapshotName));
            SnapShooter.log.warn("Unable to delete snapshot: " + this.snapshotName, (Throwable)e);
        }
        replicationHandler.snapShootDetails = details;
    }
    
    static {
        log = LoggerFactory.getLogger((Class)MethodHandles.lookup().lookupClass());
    }
}
