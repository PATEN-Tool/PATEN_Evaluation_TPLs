// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.solr.handler;

import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.util.zip.DeflaterOutputStream;
import org.apache.commons.io.output.CloseShieldOutputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.lucene.store.RateLimiter;
import org.apache.solr.common.util.FastOutputStream;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;
import java.util.regex.Matcher;
import org.apache.solr.core.SolrEventListener;
import org.apache.solr.core.CloseHook;
import org.apache.solr.common.util.StrUtils;
import org.apache.lucene.index.IndexWriter;
import org.apache.solr.util.RefCounted;
import org.apache.lucene.index.IndexDeletionPolicy;
import org.apache.solr.core.XmlConfigFile;
import org.apache.lucene.util.Version;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.core.SolrDeletionPolicy;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.Executors;
import java.io.Reader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.apache.solr.util.PropertiesInputStream;
import java.nio.file.NoSuchFileException;
import java.io.FileNotFoundException;
import org.apache.solr.util.NumberUtils;
import java.util.Properties;
import org.apache.solr.metrics.MetricsMap;
import com.codahale.metrics.Gauge;
import org.apache.solr.metrics.SolrMetricManager;
import org.apache.solr.core.SolrInfoBean;
import java.util.concurrent.TimeUnit;
import java.util.Date;
import org.apache.solr.common.util.SuppressForbidden;
import java.util.zip.Adler32;
import org.apache.solr.update.VersionInfo;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.index.DirectoryReader;
import org.apache.solr.update.CdcrUpdateLog;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.Directory;
import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.index.SegmentCommitInfo;
import org.apache.lucene.index.SegmentInfos;
import org.apache.solr.core.DirectoryFactory;
import org.apache.solr.common.util.SimpleOrderedMap;
import java.net.URI;
import org.apache.solr.core.backup.repository.BackupRepository;
import org.apache.solr.core.CoreContainer;
import java.util.concurrent.Callable;
import org.slf4j.MDC;
import org.apache.solr.core.backup.repository.LocalFileSystemRepository;
import java.util.Optional;
import org.apache.solr.common.SolrException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import java.io.FileInputStream;
import java.io.File;
import java.util.zip.Checksum;
import java.util.Iterator;
import java.io.IOException;
import java.util.Collections;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.core.IndexDeletionPolicyWrapper;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.request.SolrQueryRequest;
import java.util.HashMap;
import java.util.concurrent.ThreadFactory;
import org.apache.solr.common.util.ExecutorUtil;
import org.apache.solr.util.DefaultSolrThreadFactory;
import java.util.regex.Pattern;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.lucene.index.IndexCommit;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import org.apache.solr.common.util.NamedList;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.solr.core.SolrCore;
import org.slf4j.Logger;
import org.apache.solr.util.plugin.SolrCoreAware;

public class ReplicationHandler extends RequestHandlerBase implements SolrCoreAware
{
    public static final String PATH = "/replication";
    private static final Logger log;
    SolrCore core;
    private volatile boolean closed;
    private IndexFetcher pollingIndexFetcher;
    private ReentrantLock indexFetchLock;
    private ExecutorService restoreExecutor;
    private volatile Future<Boolean> restoreFuture;
    private volatile String currentRestoreName;
    private String includeConfFiles;
    private NamedList<String> confFileNameAlias;
    private boolean isMaster;
    private boolean isSlave;
    private boolean replicateOnOptimize;
    private boolean replicateOnCommit;
    private boolean replicateOnStart;
    private volatile ScheduledExecutorService executorService;
    private volatile long executorStartTime;
    private int numberBackupsToKeep;
    private int numTimesReplicated;
    private final Map<String, FileInfo> confFileInfoCache;
    private Long reserveCommitDuration;
    volatile IndexCommit indexCommitPoint;
    volatile NamedList<?> snapShootDetails;
    private AtomicBoolean replicationEnabled;
    private Long pollIntervalNs;
    private String pollIntervalStr;
    private PollListener pollListener;
    private AtomicBoolean pollDisabled;
    private volatile IndexFetcher currentIndexFetcher;
    private static final String SUCCESS = "success";
    private static final String FAILED = "failed";
    private static final String EXCEPTION = "exception";
    public static final String MASTER_URL = "masterUrl";
    public static final String FETCH_FROM_LEADER = "fetchFromLeader";
    public static final String SKIP_COMMIT_ON_MASTER_VERSION_ZERO = "skipCommitOnMasterVersionZero";
    public static final String STATUS = "status";
    public static final String MESSAGE = "message";
    public static final String COMMAND = "command";
    public static final String CMD_DETAILS = "details";
    public static final String CMD_BACKUP = "backup";
    public static final String CMD_RESTORE = "restore";
    public static final String CMD_RESTORE_STATUS = "restorestatus";
    public static final String CMD_FETCH_INDEX = "fetchindex";
    public static final String CMD_ABORT_FETCH = "abortfetch";
    public static final String CMD_GET_FILE_LIST = "filelist";
    public static final String CMD_GET_FILE = "filecontent";
    public static final String CMD_DISABLE_POLL = "disablepoll";
    public static final String CMD_DISABLE_REPL = "disablereplication";
    public static final String CMD_ENABLE_REPL = "enablereplication";
    public static final String CMD_ENABLE_POLL = "enablepoll";
    public static final String CMD_INDEX_VERSION = "indexversion";
    public static final String CMD_SHOW_COMMITS = "commits";
    public static final String CMD_DELETE_BACKUP = "deletebackup";
    public static final String GENERATION = "generation";
    public static final String OFFSET = "offset";
    public static final String LEN = "len";
    public static final String FILE = "file";
    public static final String SIZE = "size";
    public static final String MAX_WRITE_PER_SECOND = "maxWriteMBPerSec";
    public static final String CONF_FILE_SHORT = "cf";
    public static final String TLOG_FILE = "tlogFile";
    public static final String CHECKSUM = "checksum";
    public static final String ALIAS = "alias";
    public static final String CONF_CHECKSUM = "confchecksum";
    public static final String CONF_FILES = "confFiles";
    public static final String TLOG_FILES = "tlogFiles";
    public static final String REPLICATE_AFTER = "replicateAfter";
    public static final String FILE_STREAM = "filestream";
    public static final String POLL_INTERVAL = "pollInterval";
    public static final String INTERVAL_ERR_MSG = "The pollInterval must be in this format 'HH:mm:ss'";
    private static final Pattern INTERVAL_PATTERN;
    public static final int PACKET_SZ = 1048576;
    public static final String RESERVE = "commitReserveDuration";
    public static final String COMPRESSION = "compression";
    public static final String EXTERNAL = "external";
    public static final String INTERNAL = "internal";
    public static final String ERR_STATUS = "ERROR";
    public static final String OK_STATUS = "OK";
    public static final String NEXT_EXECUTION_AT = "nextExecutionAt";
    public static final String NUMBER_BACKUPS_TO_KEEP_REQUEST_PARAM = "numberToKeep";
    public static final String NUMBER_BACKUPS_TO_KEEP_INIT_PARAM = "maxNumberOfBackups";
    public static final String WAIT = "wait";
    
    public ReplicationHandler() {
        this.closed = false;
        this.indexFetchLock = new ReentrantLock();
        this.restoreExecutor = ExecutorUtil.newMDCAwareSingleThreadExecutor((ThreadFactory)new DefaultSolrThreadFactory("restoreExecutor"));
        this.confFileNameAlias = (NamedList<String>)new NamedList();
        this.isMaster = false;
        this.isSlave = false;
        this.replicateOnOptimize = false;
        this.replicateOnCommit = false;
        this.replicateOnStart = false;
        this.numberBackupsToKeep = 0;
        this.numTimesReplicated = 0;
        this.confFileInfoCache = new HashMap<String, FileInfo>();
        this.reserveCommitDuration = readIntervalMs("00:00:10");
        this.replicationEnabled = new AtomicBoolean(true);
        this.pollDisabled = new AtomicBoolean(false);
    }
    
    String getPollInterval() {
        return this.pollIntervalStr;
    }
    
    public void setPollListener(final PollListener pollListener) {
        this.pollListener = pollListener;
    }
    
    @Override
    public void handleRequestBody(final SolrQueryRequest req, final SolrQueryResponse rsp) throws Exception {
        rsp.setHttpCaching(false);
        final SolrParams solrParams = req.getParams();
        final String command = solrParams.required().get("command");
        if (command.equals("indexversion")) {
            IndexCommit commitPoint = this.indexCommitPoint;
            if (commitPoint == null) {
                commitPoint = this.core.getDeletionPolicy().getLatestCommit();
            }
            if (commitPoint != null && this.replicationEnabled.get()) {
                this.core.getDeletionPolicy().setReserveDuration(commitPoint.getGeneration(), this.reserveCommitDuration);
                rsp.add("indexversion", IndexDeletionPolicyWrapper.getCommitTimestamp(commitPoint));
                rsp.add("generation", commitPoint.getGeneration());
                rsp.add("status", "OK");
            }
            else {
                rsp.add("indexversion", 0L);
                rsp.add("generation", 0L);
                rsp.add("status", "OK");
            }
        }
        else if (command.equals("filecontent")) {
            this.getFileStream(solrParams, rsp);
        }
        else if (command.equals("filelist")) {
            this.getFileList(solrParams, rsp);
        }
        else if (command.equalsIgnoreCase("backup")) {
            this.doSnapShoot((SolrParams)new ModifiableSolrParams(solrParams), rsp, req);
        }
        else if (command.equalsIgnoreCase("restore")) {
            this.restore((SolrParams)new ModifiableSolrParams(solrParams), rsp, req);
        }
        else if (command.equalsIgnoreCase("restorestatus")) {
            this.populateRestoreStatus(rsp);
        }
        else if (command.equalsIgnoreCase("deletebackup")) {
            this.deleteSnapshot(new ModifiableSolrParams(solrParams), rsp);
        }
        else if (command.equalsIgnoreCase("fetchindex")) {
            this.fetchIndex(solrParams, rsp);
        }
        else if (command.equalsIgnoreCase("disablepoll")) {
            this.disablePoll(rsp);
        }
        else if (command.equalsIgnoreCase("enablepoll")) {
            this.enablePoll(rsp);
        }
        else if (command.equalsIgnoreCase("abortfetch")) {
            if (this.abortFetch()) {
                rsp.add("status", "OK");
            }
            else {
                this.reportErrorOnResponse(rsp, "No slave configured", null);
            }
        }
        else if (command.equals("commits")) {
            this.populateCommitInfo(rsp);
        }
        else if (command.equals("details")) {
            this.getReplicationDetails(rsp, solrParams.getBool("slave", true));
        }
        else if ("enablereplication".equalsIgnoreCase(command)) {
            this.replicationEnabled.set(true);
            rsp.add("status", "OK");
        }
        else if ("disablereplication".equalsIgnoreCase(command)) {
            this.replicationEnabled.set(false);
            rsp.add("status", "OK");
        }
    }
    
    private void reportErrorOnResponse(final SolrQueryResponse response, final String message, final Exception e) {
        response.add("status", "ERROR");
        response.add("message", message);
        if (e != null) {
            response.add("exception", e);
        }
    }
    
    public boolean abortFetch() {
        final IndexFetcher fetcher = this.currentIndexFetcher;
        if (fetcher != null) {
            fetcher.abortFetch();
            return true;
        }
        return false;
    }
    
    private void deleteSnapshot(final ModifiableSolrParams params, final SolrQueryResponse rsp) {
        final String name = params.required().get("name");
        final SnapShooter snapShooter = new SnapShooter(this.core, params.get("location"), params.get("name"));
        snapShooter.validateDeleteSnapshot();
        snapShooter.deleteSnapAsync(this);
        rsp.add("status", "OK");
    }
    
    private void fetchIndex(final SolrParams solrParams, final SolrQueryResponse rsp) throws InterruptedException {
        final String masterUrl = solrParams.get("masterUrl");
        if (!this.isSlave && masterUrl == null) {
            this.reportErrorOnResponse(rsp, "No slave configured or no 'masterUrl' specified", null);
            return;
        }
        final SolrParams paramsCopy = (SolrParams)new ModifiableSolrParams(solrParams);
        final IndexFetcher.IndexFetchResult[] results = { null };
        final SolrParams solrParams2;
        final IndexFetcher.IndexFetchResult result;
        final Object o;
        final Thread fetchThread = new Thread(() -> {
            result = this.doFetch(solrParams2, false);
            o[0] = result;
            return;
        }, "explicit-fetchindex-cmd");
        fetchThread.setDaemon(false);
        fetchThread.start();
        if (solrParams.getBool("wait", false)) {
            fetchThread.join();
            if (results[0] == null) {
                this.reportErrorOnResponse(rsp, "Unable to determine result of synchronous index fetch", null);
            }
            else if (results[0].getSuccessful()) {
                rsp.add("status", "OK");
            }
            else {
                this.reportErrorOnResponse(rsp, results[0].getMessage(), null);
            }
        }
        else {
            rsp.add("status", "OK");
        }
    }
    
    private List<NamedList<Object>> getCommits() {
        final Map<Long, IndexCommit> commits = this.core.getDeletionPolicy().getCommits();
        final List<NamedList<Object>> l = new ArrayList<NamedList<Object>>();
        for (final IndexCommit c : commits.values()) {
            try {
                final NamedList<Object> nl = (NamedList<Object>)new NamedList();
                nl.add("indexVersion", (Object)IndexDeletionPolicyWrapper.getCommitTimestamp(c));
                nl.add("generation", (Object)c.getGeneration());
                final List<String> commitList = new ArrayList<String>(c.getFileNames().size());
                commitList.addAll(c.getFileNames());
                Collections.sort(commitList);
                nl.add("filelist", (Object)commitList);
                l.add(nl);
            }
            catch (IOException e) {
                ReplicationHandler.log.warn("Exception while reading files for commit " + c, (Throwable)e);
            }
        }
        return l;
    }
    
    static Long getCheckSum(final Checksum checksum, final File f) {
        FileInputStream fis = null;
        checksum.reset();
        final byte[] buffer = new byte[1048576];
        try {
            fis = new FileInputStream(f);
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) >= 0) {
                checksum.update(buffer, 0, bytesRead);
            }
            return checksum.getValue();
        }
        catch (Exception e) {
            ReplicationHandler.log.warn("Exception in finding checksum of " + f, (Throwable)e);
        }
        finally {
            IOUtils.closeQuietly((InputStream)fis);
        }
        return null;
    }
    
    public IndexFetcher.IndexFetchResult doFetch(final SolrParams solrParams, final boolean forceReplication) {
        final String masterUrl = (solrParams == null) ? null : solrParams.get("masterUrl");
        if (!this.indexFetchLock.tryLock()) {
            return IndexFetcher.IndexFetchResult.LOCK_OBTAIN_FAILED;
        }
        if (this.core.getCoreContainer().isShutDown()) {
            ReplicationHandler.log.warn("I was asked to replicate but CoreContainer is shutting down");
            return IndexFetcher.IndexFetchResult.CONTAINER_IS_SHUTTING_DOWN;
        }
        try {
            if (masterUrl != null) {
                if (this.currentIndexFetcher != null && this.currentIndexFetcher != this.pollingIndexFetcher) {
                    this.currentIndexFetcher.destroy();
                }
                this.currentIndexFetcher = new IndexFetcher(solrParams.toNamedList(), this, this.core);
            }
            else {
                this.currentIndexFetcher = this.pollingIndexFetcher;
            }
            return this.currentIndexFetcher.fetchLatestIndex(forceReplication);
        }
        catch (Exception e) {
            SolrException.log(ReplicationHandler.log, "Index fetch failed ", (Throwable)e);
            if (this.currentIndexFetcher != this.pollingIndexFetcher) {
                this.currentIndexFetcher.destroy();
            }
            return new IndexFetcher.IndexFetchResult("Fetching index failed by exception", false, e);
        }
        finally {
            if (this.pollingIndexFetcher != null) {
                if (this.currentIndexFetcher != this.pollingIndexFetcher) {
                    this.currentIndexFetcher.destroy();
                }
                this.currentIndexFetcher = this.pollingIndexFetcher;
            }
            this.indexFetchLock.unlock();
        }
    }
    
    boolean isReplicating() {
        return this.indexFetchLock.isLocked();
    }
    
    private void restore(final SolrParams params, final SolrQueryResponse rsp, final SolrQueryRequest req) throws IOException {
        if (this.restoreFuture != null && !this.restoreFuture.isDone()) {
            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Restore in progress. Cannot run multiple restore operationsfor the same core");
        }
        String name = params.get("name");
        String location = params.get("location");
        final String repoName = params.get("repository");
        final CoreContainer cc = this.core.getCoreContainer();
        BackupRepository repo = null;
        if (repoName != null) {
            repo = cc.newBackupRepository(Optional.of(repoName));
            location = repo.getBackupLocation(location);
            if (location == null) {
                throw new IllegalArgumentException("location is required");
            }
        }
        else {
            repo = new LocalFileSystemRepository();
        }
        if (location == null) {
            location = this.core.getDataDir();
        }
        final URI locationUri = repo.createURI(location);
        if (name == null) {
            final String[] filePaths = repo.listAll(locationUri);
            final List<OldBackupDirectory> dirs = new ArrayList<OldBackupDirectory>();
            for (final String f : filePaths) {
                final OldBackupDirectory obd = new OldBackupDirectory(locationUri, f);
                if (obd.getTimestamp().isPresent()) {
                    dirs.add(obd);
                }
            }
            Collections.sort(dirs);
            if (dirs.size() == 0) {
                throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "No backup name specified and none found in " + this.core.getDataDir());
            }
            name = dirs.get(0).getDirName();
        }
        else {
            name = "snapshot." + name;
        }
        final RestoreCore restoreCore = new RestoreCore(repo, this.core, locationUri, name);
        try {
            MDC.put("RestoreCore.core", this.core.getName());
            MDC.put("RestoreCore.backupLocation", location);
            MDC.put("RestoreCore.backupName", name);
            this.restoreFuture = this.restoreExecutor.submit((Callable<Boolean>)restoreCore);
            this.currentRestoreName = name;
            rsp.add("status", "OK");
        }
        finally {
            MDC.remove("RestoreCore.core");
            MDC.remove("RestoreCore.backupLocation");
            MDC.remove("RestoreCore.backupName");
        }
    }
    
    private void populateRestoreStatus(final SolrQueryResponse rsp) {
        final NamedList<Object> restoreStatus = (NamedList<Object>)new SimpleOrderedMap();
        if (this.restoreFuture == null) {
            restoreStatus.add("status", (Object)"No restore actions in progress");
            rsp.add("restorestatus", restoreStatus);
            rsp.add("status", "OK");
            return;
        }
        restoreStatus.add("snapshotName", (Object)this.currentRestoreName);
        Label_0146: {
            if (this.restoreFuture.isDone()) {
                try {
                    final boolean success = this.restoreFuture.get();
                    if (success) {
                        restoreStatus.add("status", (Object)"success");
                    }
                    else {
                        restoreStatus.add("status", (Object)"failed");
                    }
                    break Label_0146;
                }
                catch (Exception e) {
                    restoreStatus.add("status", (Object)"failed");
                    restoreStatus.add("exception", (Object)e.getMessage());
                    rsp.add("restorestatus", restoreStatus);
                    this.reportErrorOnResponse(rsp, "Unable to read restorestatus", e);
                    return;
                }
            }
            restoreStatus.add("status", (Object)"In Progress");
        }
        rsp.add("restorestatus", restoreStatus);
        rsp.add("status", "OK");
    }
    
    private void populateCommitInfo(final SolrQueryResponse rsp) {
        rsp.add("commits", this.getCommits());
        rsp.add("status", "OK");
    }
    
    private void doSnapShoot(final SolrParams params, final SolrQueryResponse rsp, final SolrQueryRequest req) {
        try {
            int numberToKeep = params.getInt("numberToKeep", 0);
            if (numberToKeep > 0 && this.numberBackupsToKeep > 0) {
                throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Cannot use numberToKeep if maxNumberOfBackups was specified in the configuration.");
            }
            numberToKeep = Math.max(numberToKeep, this.numberBackupsToKeep);
            if (numberToKeep < 1) {
                numberToKeep = Integer.MAX_VALUE;
            }
            String location = params.get("location");
            final String repoName = params.get("repository");
            final CoreContainer cc = this.core.getCoreContainer();
            BackupRepository repo = null;
            if (repoName != null) {
                repo = cc.newBackupRepository(Optional.of(repoName));
                location = repo.getBackupLocation(location);
                if (location == null) {
                    throw new IllegalArgumentException("location is required");
                }
            }
            else {
                repo = new LocalFileSystemRepository();
                if (location == null) {
                    location = this.core.getDataDir();
                }
                else {
                    location = this.core.getCoreDescriptor().getInstanceDir().resolve(location).normalize().toString();
                }
            }
            final URI locationUri = repo.createURI(location);
            final String commitName = params.get("commitName");
            final SnapShooter snapShooter = new SnapShooter(repo, this.core, locationUri, params.get("name"), commitName);
            snapShooter.validateCreateSnapshot();
            snapShooter.createSnapAsync(numberToKeep, nl -> this.snapShootDetails = nl);
            rsp.add("status", "OK");
        }
        catch (SolrException e) {
            throw e;
        }
        catch (Exception e2) {
            ReplicationHandler.log.error("Exception while creating a snapshot", (Throwable)e2);
            this.reportErrorOnResponse(rsp, "Error encountered while creating a snapshot: " + e2.getMessage(), e2);
        }
    }
    
    private void getFileStream(final SolrParams solrParams, final SolrQueryResponse rsp) {
        final ModifiableSolrParams rawParams = new ModifiableSolrParams(solrParams);
        rawParams.set("wt", new String[] { "filestream" });
        final String cfileName = solrParams.get("cf");
        final String tlogFileName = solrParams.get("tlogFile");
        if (cfileName != null) {
            rsp.add("filestream", new LocalFsConfFileStream(solrParams));
        }
        else if (tlogFileName != null) {
            rsp.add("filestream", new LocalFsTlogFileStream(solrParams));
        }
        else {
            rsp.add("filestream", new DirectoryFileStream(solrParams));
        }
        rsp.add("status", "OK");
    }
    
    private void getFileList(final SolrParams solrParams, final SolrQueryResponse rsp) {
        final String v = solrParams.required().get("generation");
        long gen = Long.parseLong(v);
        if (gen == -1L) {
            final IndexCommit commitPoint = this.core.getDeletionPolicy().getLatestCommit();
            if (commitPoint == null) {
                rsp.add("filelist", Collections.EMPTY_LIST);
                return;
            }
            gen = commitPoint.getGeneration();
        }
        final IndexCommit commit = this.core.getDeletionPolicy().getCommitPoint(gen);
        if (commit == null) {
            this.reportErrorOnResponse(rsp, "invalid index generation", null);
            return;
        }
        this.core.getDeletionPolicy().setReserveDuration(gen, this.reserveCommitDuration);
        final List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        Directory dir = null;
        try {
            dir = this.core.getDirectoryFactory().get(this.core.getNewIndexDir(), DirectoryFactory.DirContext.DEFAULT, this.core.getSolrConfig().indexConfig.lockType);
            final SegmentInfos infos = SegmentInfos.readCommit(dir, commit.getSegmentsFileName());
            for (final SegmentCommitInfo commitInfo : infos) {
                for (final String file : commitInfo.files()) {
                    final Map<String, Object> fileMeta = new HashMap<String, Object>();
                    fileMeta.put("name", file);
                    fileMeta.put("size", dir.fileLength(file));
                    try (final IndexInput in = dir.openInput(file, IOContext.READONCE)) {
                        try {
                            final long checksum = CodecUtil.retrieveChecksum(in);
                            fileMeta.put("checksum", checksum);
                        }
                        catch (Exception e) {
                            ReplicationHandler.log.warn("Could not read checksum from index file: " + file, (Throwable)e);
                        }
                    }
                    result.add(fileMeta);
                }
            }
            final Map<String, Object> fileMeta2 = new HashMap<String, Object>();
            fileMeta2.put("name", infos.getSegmentsFileName());
            fileMeta2.put("size", dir.fileLength(infos.getSegmentsFileName()));
            if (infos.getId() != null) {
                try (final IndexInput in2 = dir.openInput(infos.getSegmentsFileName(), IOContext.READONCE)) {
                    try {
                        fileMeta2.put("checksum", CodecUtil.retrieveChecksum(in2));
                    }
                    catch (Exception e2) {
                        ReplicationHandler.log.warn("Could not read checksum from index file: " + infos.getSegmentsFileName(), (Throwable)e2);
                    }
                }
            }
            result.add(fileMeta2);
        }
        catch (IOException e3) {
            ReplicationHandler.log.error("Unable to get file names for indexCommit generation: " + gen, (Throwable)e3);
            this.reportErrorOnResponse(rsp, "unable to get file names for given index generation", e3);
            return;
        }
        finally {
            if (dir != null) {
                try {
                    this.core.getDirectoryFactory().release(dir);
                }
                catch (IOException e4) {
                    SolrException.log(ReplicationHandler.log, "Could not release directory after fetching file list", (Throwable)e4);
                }
            }
        }
        rsp.add("filelist", result);
        if (solrParams.getBool("tlogFiles", false)) {
            try {
                final List<Map<String, Object>> tlogfiles = this.getTlogFileList(commit);
                ReplicationHandler.log.info("Adding tlog files to list: " + tlogfiles);
                rsp.add("tlogFiles", tlogfiles);
            }
            catch (IOException e3) {
                ReplicationHandler.log.error("Unable to get tlog file names for indexCommit generation: " + gen, (Throwable)e3);
                this.reportErrorOnResponse(rsp, "unable to get tlog file names for given index generation", e3);
                return;
            }
        }
        if (this.confFileNameAlias.size() < 1 || this.core.getCoreContainer().isZooKeeperAware()) {
            return;
        }
        ReplicationHandler.log.debug("Adding config files to list: " + this.includeConfFiles);
        rsp.add("confFiles", this.getConfFileInfoFromCache(this.confFileNameAlias, this.confFileInfoCache));
        rsp.add("status", "OK");
    }
    
    List<Map<String, Object>> getTlogFileList(final IndexCommit commit) throws IOException {
        final long maxVersion = this.getMaxVersion(commit);
        final CdcrUpdateLog ulog = (CdcrUpdateLog)this.core.getUpdateHandler().getUpdateLog();
        final String[] logList = ulog.getLogList(new File(ulog.getLogDir()));
        final List<Map<String, Object>> tlogFiles = new ArrayList<Map<String, Object>>();
        for (final String fileName : logList) {
            final long startVersion = Math.abs(Long.parseLong(fileName.substring(fileName.lastIndexOf(46) + 1)));
            if (startVersion < maxVersion) {
                final Map<String, Object> fileMeta = new HashMap<String, Object>();
                fileMeta.put("name", fileName);
                fileMeta.put("size", new File(ulog.getLogDir(), fileName).length());
                tlogFiles.add(fileMeta);
            }
        }
        return tlogFiles;
    }
    
    private long getMaxVersion(final IndexCommit commit) throws IOException {
        try (final DirectoryReader reader = DirectoryReader.open(commit)) {
            final IndexSearcher searcher = new IndexSearcher((IndexReader)reader);
            final VersionInfo vinfo = this.core.getUpdateHandler().getUpdateLog().getVersionInfo();
            return Math.abs(vinfo.getMaxVersionFromIndex(searcher));
        }
    }
    
    List<Map<String, Object>> getConfFileInfoFromCache(final NamedList<String> nameAndAlias, final Map<String, FileInfo> confFileInfoCache) {
        final List<Map<String, Object>> confFiles = new ArrayList<Map<String, Object>>();
        synchronized (confFileInfoCache) {
            final File confDir = new File(this.core.getResourceLoader().getConfigDir());
            Checksum checksum = null;
            for (int i = 0; i < nameAndAlias.size(); ++i) {
                final String cf = nameAndAlias.getName(i);
                final File f = new File(confDir, cf);
                if (f.exists()) {
                    if (!f.isDirectory()) {
                        FileInfo info = confFileInfoCache.get(cf);
                        if (info == null || info.lastmodified != f.lastModified() || info.size != f.length()) {
                            if (checksum == null) {
                                checksum = new Adler32();
                            }
                            info = new FileInfo(f.lastModified(), cf, f.length(), getCheckSum(checksum, f));
                            confFileInfoCache.put(cf, info);
                        }
                        final Map<String, Object> m = info.getAsMap();
                        if (nameAndAlias.getVal(i) != null) {
                            m.put("alias", nameAndAlias.getVal(i));
                        }
                        confFiles.add(m);
                    }
                }
            }
        }
        return confFiles;
    }
    
    private void disablePoll(final SolrQueryResponse rsp) {
        if (this.pollingIndexFetcher != null) {
            this.pollDisabled.set(true);
            ReplicationHandler.log.info("inside disable poll, value of pollDisabled = " + this.pollDisabled);
            rsp.add("status", "OK");
        }
        else {
            this.reportErrorOnResponse(rsp, "No slave configured", null);
        }
    }
    
    private void enablePoll(final SolrQueryResponse rsp) {
        if (this.pollingIndexFetcher != null) {
            this.pollDisabled.set(false);
            ReplicationHandler.log.info("inside enable poll, value of pollDisabled = " + this.pollDisabled);
            rsp.add("status", "OK");
        }
        else {
            this.reportErrorOnResponse(rsp, "No slave configured", null);
        }
    }
    
    boolean isPollingDisabled() {
        return this.pollDisabled.get();
    }
    
    @SuppressForbidden(reason = "Need currentTimeMillis, to output next execution time in replication details")
    private void markScheduledExecutionStart() {
        this.executorStartTime = System.currentTimeMillis();
    }
    
    private Date getNextScheduledExecTime() {
        Date nextTime = null;
        if (this.executorStartTime > 0L) {
            nextTime = new Date(this.executorStartTime + TimeUnit.MILLISECONDS.convert(this.pollIntervalNs, TimeUnit.NANOSECONDS));
        }
        return nextTime;
    }
    
    int getTimesReplicatedSinceStartup() {
        return this.numTimesReplicated;
    }
    
    void setTimesReplicatedSinceStartup() {
        ++this.numTimesReplicated;
    }
    
    @Override
    public SolrInfoBean.Category getCategory() {
        return SolrInfoBean.Category.REPLICATION;
    }
    
    @Override
    public String getDescription() {
        return "ReplicationHandler provides replication of index and configuration files from Master to Slaves";
    }
    
    private CommitVersionInfo getIndexVersion() {
        try {
            return this.core.withSearcher(searcher -> CommitVersionInfo.build(searcher.getIndexReader().getIndexCommit()));
        }
        catch (IOException e) {
            ReplicationHandler.log.warn("Unable to get index commit: ", (Throwable)e);
            return null;
        }
    }
    
    @Override
    public void initializeMetrics(final SolrMetricManager manager, final String registry, final String tag, final String scope) {
        super.initializeMetrics(manager, registry, tag, scope);
        manager.registerGauge(this, registry, (Gauge<?>)(() -> (this.core != null && !this.core.isClosed()) ? NumberUtils.readableSize(this.core.getIndexSize()) : ""), tag, true, "indexSize", this.getCategory().toString(), scope);
        manager.registerGauge(this, registry, (Gauge<?>)(() -> (this.core != null && !this.core.isClosed()) ? this.getIndexVersion().toString() : ""), tag, true, "indexVersion", this.getCategory().toString(), scope);
        manager.registerGauge(this, registry, (Gauge<?>)(() -> (this.core != null && !this.core.isClosed()) ? this.getIndexVersion().generation : 0L), tag, true, "generation", this.getCategory().toString(), scope);
        manager.registerGauge(this, registry, (Gauge<?>)(() -> (this.core != null && !this.core.isClosed()) ? this.core.getIndexDir() : ""), tag, true, "indexPath", this.getCategory().toString(), scope);
        manager.registerGauge(this, registry, (Gauge<?>)(() -> this.isMaster), tag, true, "isMaster", this.getCategory().toString(), scope);
        manager.registerGauge(this, registry, (Gauge<?>)(() -> this.isSlave), tag, true, "isSlave", this.getCategory().toString(), scope);
        final IndexFetcher fetcher;
        long elapsed;
        long val;
        Properties props;
        final MetricsMap fetcherMap = new MetricsMap((detailed, map) -> {
            fetcher = this.currentIndexFetcher;
            if (fetcher != null) {
                map.put("masterUrl", fetcher.getMasterUrl());
                if (this.getPollInterval() != null) {
                    map.put("pollInterval", this.getPollInterval());
                }
                map.put("isPollingDisabled", this.isPollingDisabled());
                map.put("isReplicating", this.isReplicating());
                elapsed = fetcher.getReplicationTimeElapsed();
                val = fetcher.getTotalBytesDownloaded();
                if (elapsed > 0L) {
                    map.put("timeElapsed", elapsed);
                    map.put("bytesDownloaded", val);
                    map.put("downloadSpeed", val / elapsed);
                }
                props = this.loadReplicationProperties();
                this.addVal(map, "previousCycleTimeInSeconds", props, Long.class);
                this.addVal(map, "indexReplicatedAt", props, Date.class);
                this.addVal(map, "confFilesReplicatedAt", props, Date.class);
                this.addVal(map, "replicationFailedAt", props, Date.class);
                this.addVal(map, "timesFailed", props, Integer.class);
                this.addVal(map, "timesIndexReplicated", props, Integer.class);
                this.addVal(map, "lastCycleBytesDownloaded", props, Long.class);
                this.addVal(map, "timesConfigReplicated", props, Integer.class);
                this.addVal(map, "confFilesReplicated", props, String.class);
            }
            return;
        });
        manager.registerGauge(this, registry, (Gauge<?>)fetcherMap, tag, true, "fetcher", this.getCategory().toString(), scope);
        manager.registerGauge(this, registry, (Gauge<?>)(() -> (this.isMaster && this.includeConfFiles != null) ? this.includeConfFiles : ""), tag, true, "confFilesToReplicate", this.getCategory().toString(), scope);
        manager.registerGauge(this, registry, (Gauge<?>)(() -> this.isMaster ? this.getReplicateAfterStrings() : Collections.emptyList()), tag, true, "replicateAfter", this.getCategory().toString(), scope);
        manager.registerGauge(this, registry, (Gauge<?>)(() -> this.isMaster && this.replicationEnabled.get()), tag, true, "replicationEnabled", this.getCategory().toString(), scope);
    }
    
    private NamedList<Object> getReplicationDetails(final SolrQueryResponse rsp, final boolean showSlaveDetails) {
        final NamedList<Object> details = (NamedList<Object>)new SimpleOrderedMap();
        final NamedList<Object> master = (NamedList<Object>)new SimpleOrderedMap();
        final NamedList<Object> slave = (NamedList<Object>)new SimpleOrderedMap();
        details.add("indexSize", (Object)NumberUtils.readableSize(this.core.getIndexSize()));
        details.add("indexPath", (Object)this.core.getIndexDir());
        details.add("commits", (Object)this.getCommits());
        details.add("isMaster", (Object)String.valueOf(this.isMaster));
        details.add("isSlave", (Object)String.valueOf(this.isSlave));
        final CommitVersionInfo vInfo = this.getIndexVersion();
        details.add("indexVersion", (Object)((null == vInfo) ? 0L : vInfo.version));
        details.add("generation", (Object)((null == vInfo) ? 0L : vInfo.generation));
        final IndexCommit commit = this.indexCommitPoint;
        if (this.isMaster) {
            if (this.includeConfFiles != null) {
                master.add("confFiles", (Object)this.includeConfFiles);
            }
            master.add("replicateAfter", (Object)this.getReplicateAfterStrings());
            master.add("replicationEnabled", (Object)String.valueOf(this.replicationEnabled.get()));
        }
        if (this.isMaster && commit != null) {
            final CommitVersionInfo repCommitInfo = CommitVersionInfo.build(commit);
            master.add("replicableVersion", (Object)repCommitInfo.version);
            master.add("replicableGeneration", (Object)repCommitInfo.generation);
        }
        final IndexFetcher fetcher = this.currentIndexFetcher;
        if (fetcher != null) {
            final Properties props = this.loadReplicationProperties();
            if (showSlaveDetails) {
                try {
                    final NamedList nl = fetcher.getDetails();
                    slave.add("masterDetails", nl.get("details"));
                }
                catch (Exception e) {
                    ReplicationHandler.log.warn("Exception while invoking 'details' method for replication on master ", (Throwable)e);
                    slave.add("ERROR", (Object)"invalid_master");
                }
            }
            slave.add("masterUrl", (Object)fetcher.getMasterUrl());
            if (this.getPollInterval() != null) {
                slave.add("pollInterval", (Object)this.getPollInterval());
            }
            final Date nextScheduled = this.getNextScheduledExecTime();
            if (nextScheduled != null && !this.isPollingDisabled()) {
                slave.add("nextExecutionAt", (Object)nextScheduled.toString());
            }
            else if (this.isPollingDisabled()) {
                slave.add("nextExecutionAt", (Object)"Polling disabled");
            }
            this.addVal(slave, "indexReplicatedAt", props, Date.class);
            this.addVal(slave, "indexReplicatedAtList", props, List.class);
            this.addVal(slave, "replicationFailedAtList", props, List.class);
            this.addVal(slave, "timesIndexReplicated", props, Integer.class);
            this.addVal(slave, "confFilesReplicated", props, Integer.class);
            this.addVal(slave, "timesConfigReplicated", props, Integer.class);
            this.addVal(slave, "confFilesReplicatedAt", props, Integer.class);
            this.addVal(slave, "lastCycleBytesDownloaded", props, Long.class);
            this.addVal(slave, "timesFailed", props, Integer.class);
            this.addVal(slave, "replicationFailedAt", props, Date.class);
            this.addVal(slave, "previousCycleTimeInSeconds", props, Long.class);
            this.addVal(slave, "clearedLocalIndexFirst", props, Long.class);
            slave.add("currentDate", (Object)new Date().toString());
            slave.add("isPollingDisabled", (Object)String.valueOf(this.isPollingDisabled()));
            final boolean isReplicating = this.isReplicating();
            slave.add("isReplicating", (Object)String.valueOf(isReplicating));
            if (isReplicating) {
                try {
                    long bytesToDownload = 0L;
                    final List<String> filesToDownload = new ArrayList<String>();
                    for (final Map<String, Object> file : fetcher.getFilesToDownload()) {
                        filesToDownload.add(file.get("name"));
                        bytesToDownload += file.get("size");
                    }
                    for (final Map<String, Object> file : fetcher.getConfFilesToDownload()) {
                        filesToDownload.add(file.get("name"));
                        bytesToDownload += file.get("size");
                    }
                    slave.add("filesToDownload", (Object)filesToDownload);
                    slave.add("numFilesToDownload", (Object)String.valueOf(filesToDownload.size()));
                    slave.add("bytesToDownload", (Object)NumberUtils.readableSize(bytesToDownload));
                    long bytesDownloaded = 0L;
                    final List<String> filesDownloaded = new ArrayList<String>();
                    for (final Map<String, Object> file2 : fetcher.getFilesDownloaded()) {
                        filesDownloaded.add(file2.get("name"));
                        bytesDownloaded += file2.get("size");
                    }
                    for (final Map<String, Object> file2 : fetcher.getConfFilesDownloaded()) {
                        filesDownloaded.add(file2.get("name"));
                        bytesDownloaded += file2.get("size");
                    }
                    final Map<String, Object> currentFile = fetcher.getCurrentFile();
                    String currFile = null;
                    long currFileSize = 0L;
                    long currFileSizeDownloaded = 0L;
                    float percentDownloaded = 0.0f;
                    if (currentFile != null) {
                        currFile = currentFile.get("name");
                        currFileSize = currentFile.get("size");
                        if (currentFile.containsKey("bytesDownloaded")) {
                            currFileSizeDownloaded = currentFile.get("bytesDownloaded");
                            bytesDownloaded += currFileSizeDownloaded;
                            if (currFileSize > 0L) {
                                percentDownloaded = (float)(currFileSizeDownloaded * 100L / currFileSize);
                            }
                        }
                    }
                    slave.add("filesDownloaded", (Object)filesDownloaded);
                    slave.add("numFilesDownloaded", (Object)String.valueOf(filesDownloaded.size()));
                    long estimatedTimeRemaining = 0L;
                    final Date replicationStartTimeStamp = fetcher.getReplicationStartTimeStamp();
                    if (replicationStartTimeStamp != null) {
                        slave.add("replicationStartTime", (Object)replicationStartTimeStamp.toString());
                    }
                    final long elapsed = fetcher.getReplicationTimeElapsed();
                    slave.add("timeElapsed", (Object)(String.valueOf(elapsed) + "s"));
                    if (bytesDownloaded > 0L) {
                        estimatedTimeRemaining = (bytesToDownload - bytesDownloaded) * elapsed / bytesDownloaded;
                    }
                    float totalPercent = 0.0f;
                    long downloadSpeed = 0L;
                    if (bytesToDownload > 0L) {
                        totalPercent = (float)(bytesDownloaded * 100L / bytesToDownload);
                    }
                    if (elapsed > 0L) {
                        downloadSpeed = bytesDownloaded / elapsed;
                    }
                    if (currFile != null) {
                        slave.add("currentFile", (Object)currFile);
                    }
                    slave.add("currentFileSize", (Object)NumberUtils.readableSize(currFileSize));
                    slave.add("currentFileSizeDownloaded", (Object)NumberUtils.readableSize(currFileSizeDownloaded));
                    slave.add("currentFileSizePercent", (Object)String.valueOf(percentDownloaded));
                    slave.add("bytesDownloaded", (Object)NumberUtils.readableSize(bytesDownloaded));
                    slave.add("totalPercent", (Object)String.valueOf(totalPercent));
                    slave.add("timeRemaining", (Object)(String.valueOf(estimatedTimeRemaining) + "s"));
                    slave.add("downloadSpeed", (Object)NumberUtils.readableSize(downloadSpeed));
                }
                catch (Exception e2) {
                    ReplicationHandler.log.error("Exception while writing replication details: ", (Throwable)e2);
                }
            }
        }
        if (this.isMaster) {
            details.add("master", (Object)master);
        }
        if (slave.size() > 0) {
            details.add("slave", (Object)slave);
        }
        final NamedList snapshotStats = this.snapShootDetails;
        if (snapshotStats != null) {
            details.add("backup", (Object)snapshotStats);
        }
        if (rsp.getValues().get("status") == null) {
            rsp.add("status", "OK");
        }
        rsp.add("details", details);
        return details;
    }
    
    private void addVal(final NamedList<Object> nl, final String key, final Properties props, final Class clzz) {
        final Object val = this.formatVal(key, props, clzz);
        if (val != null) {
            nl.add(key, val);
        }
    }
    
    private void addVal(final Map<String, Object> map, final String key, final Properties props, final Class clzz) {
        final Object val = this.formatVal(key, props, clzz);
        if (val != null) {
            map.put(key, val);
        }
    }
    
    private Object formatVal(final String key, final Properties props, final Class clzz) {
        final String s = props.getProperty(key);
        if (s == null || s.trim().length() == 0) {
            return null;
        }
        if (clzz == Date.class) {
            try {
                final Long l = Long.parseLong(s);
                return new Date(l).toString();
            }
            catch (NumberFormatException e) {
                return null;
            }
        }
        if (clzz == List.class) {
            final String[] ss = s.split(",");
            final List<String> i = new ArrayList<String>();
            for (final String s2 : ss) {
                i.add(new Date(Long.parseLong(s2)).toString());
            }
            return i;
        }
        return s;
    }
    
    private List<String> getReplicateAfterStrings() {
        final List<String> replicateAfter = new ArrayList<String>();
        if (this.replicateOnCommit) {
            replicateAfter.add("commit");
        }
        if (this.replicateOnOptimize) {
            replicateAfter.add("optimize");
        }
        if (this.replicateOnStart) {
            replicateAfter.add("startup");
        }
        return replicateAfter;
    }
    
    Properties loadReplicationProperties() {
        Directory dir = null;
        try {
            try {
                dir = this.core.getDirectoryFactory().get(this.core.getDataDir(), DirectoryFactory.DirContext.META_DATA, this.core.getSolrConfig().indexConfig.lockType);
                IndexInput input;
                try {
                    input = dir.openInput("replication.properties", IOContext.DEFAULT);
                }
                catch (FileNotFoundException | NoSuchFileException ex2) {
                    final IOException ex;
                    final IOException e = ex;
                    return new Properties();
                }
                try {
                    final InputStream is = new PropertiesInputStream(input);
                    final Properties props = new Properties();
                    props.load(new InputStreamReader(is, StandardCharsets.UTF_8));
                    return props;
                }
                finally {
                    input.close();
                }
            }
            finally {
                if (dir != null) {
                    this.core.getDirectoryFactory().release(dir);
                }
            }
        }
        catch (IOException e2) {
            throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, (Throwable)e2);
        }
    }
    
    private void setupPolling(final String intervalStr) {
        this.pollIntervalStr = intervalStr;
        this.pollIntervalNs = readIntervalNs(this.pollIntervalStr);
        if (this.pollIntervalNs == null || this.pollIntervalNs <= 0L) {
            ReplicationHandler.log.info(" No value set for 'pollInterval'. Timer Task not started.");
            return;
        }
        IndexFetcher.IndexFetchResult fetchResult;
        final Runnable task = () -> {
            if (this.pollDisabled.get()) {
                ReplicationHandler.log.info("Poll disabled");
                return;
            }
            else {
                try {
                    ReplicationHandler.log.debug("Polling for index modifications");
                    this.markScheduledExecutionStart();
                    fetchResult = this.doFetch(null, false);
                    if (this.pollListener != null) {
                        this.pollListener.onComplete(this.core, fetchResult);
                    }
                }
                catch (Exception e) {
                    ReplicationHandler.log.error("Exception in fetching index", (Throwable)e);
                }
                return;
            }
        };
        this.executorService = Executors.newSingleThreadScheduledExecutor(new DefaultSolrThreadFactory("indexFetcher"));
        final long initialDelayNs = new Random().nextLong() % this.pollIntervalNs + TimeUnit.NANOSECONDS.convert(1L, TimeUnit.MILLISECONDS);
        this.executorService.scheduleAtFixedRate(task, initialDelayNs, this.pollIntervalNs, TimeUnit.NANOSECONDS);
        ReplicationHandler.log.info("Poll scheduled at an interval of {}ms", (Object)TimeUnit.MILLISECONDS.convert(this.pollIntervalNs, TimeUnit.NANOSECONDS));
    }
    
    @Override
    public void inform(final SolrCore core) {
        this.core = core;
        this.registerCloseHook();
        Long deprecatedReserveCommitDuration = null;
        final Object nbtk = this.initArgs.get("maxNumberOfBackups");
        if (nbtk != null) {
            this.numberBackupsToKeep = Integer.parseInt(nbtk.toString());
        }
        else {
            this.numberBackupsToKeep = 0;
        }
        final NamedList slave = (NamedList)this.initArgs.get("slave");
        final boolean enableSlave = this.isEnabled(slave);
        if (enableSlave) {
            final IndexFetcher indexFetcher = new IndexFetcher(slave, this, core);
            this.pollingIndexFetcher = indexFetcher;
            this.currentIndexFetcher = indexFetcher;
            this.setupPolling((String)slave.get("pollInterval"));
            this.isSlave = true;
        }
        NamedList master = (NamedList)this.initArgs.get("master");
        boolean enableMaster = this.isEnabled(master);
        if ((enableMaster || (enableSlave && !this.currentIndexFetcher.fetchFromLeader)) && core.getCoreContainer().getZkController() != null) {
            ReplicationHandler.log.warn("SolrCloud is enabled for core " + core.getName() + " but so is old-style replication. Make sure you intend this behavior, it usually indicates a mis-configuration. Master setting is " + Boolean.toString(enableMaster) + " and slave setting is " + Boolean.toString(enableSlave));
        }
        if (!enableSlave && !enableMaster) {
            enableMaster = true;
            master = new NamedList();
        }
        if (enableMaster) {
            this.includeConfFiles = (String)master.get("confFiles");
            if (this.includeConfFiles != null && this.includeConfFiles.trim().length() > 0) {
                final List<String> files = Arrays.asList(this.includeConfFiles.split(","));
                for (final String file : files) {
                    if (file.trim().length() == 0) {
                        continue;
                    }
                    final String[] strs = file.trim().split(":");
                    this.confFileNameAlias.add(strs[0], (Object)((strs.length > 1) ? strs[1] : null));
                }
                ReplicationHandler.log.info("Replication enabled for following config files: " + this.includeConfFiles);
            }
            final List backup = master.getAll("backupAfter");
            final boolean backupOnCommit = backup.contains("commit");
            final boolean backupOnOptimize = !backupOnCommit && backup.contains("optimize");
            final List replicateAfter = master.getAll("replicateAfter");
            this.replicateOnCommit = replicateAfter.contains("commit");
            this.replicateOnOptimize = (!this.replicateOnCommit && replicateAfter.contains("optimize"));
            if (!this.replicateOnCommit && !this.replicateOnOptimize) {
                this.replicateOnCommit = true;
            }
            if (this.replicateOnOptimize) {
                final IndexDeletionPolicyWrapper wrapper = core.getDeletionPolicy();
                final IndexDeletionPolicy policy = (wrapper == null) ? null : wrapper.getWrappedDeletionPolicy();
                if (policy instanceof SolrDeletionPolicy) {
                    final SolrDeletionPolicy solrPolicy = (SolrDeletionPolicy)policy;
                    if (solrPolicy.getMaxOptimizedCommitsToKeep() < 1) {
                        solrPolicy.setMaxOptimizedCommitsToKeep(1);
                    }
                }
                else {
                    ReplicationHandler.log.warn("Replication can't call setMaxOptimizedCommitsToKeep on " + policy);
                }
            }
            if (this.replicateOnOptimize || backupOnOptimize) {
                core.getUpdateHandler().registerOptimizeCallback(this.getEventListener(backupOnOptimize, this.replicateOnOptimize));
            }
            if (this.replicateOnCommit || backupOnCommit) {
                this.replicateOnCommit = true;
                core.getUpdateHandler().registerCommitCallback(this.getEventListener(backupOnCommit, this.replicateOnCommit));
            }
            if (replicateAfter.contains("startup")) {
                this.replicateOnStart = true;
                final RefCounted<SolrIndexSearcher> s = core.getNewestSearcher(false);
                try {
                    final DirectoryReader reader = (s == null) ? null : s.get().getIndexReader();
                    if (reader != null && reader.getIndexCommit() != null && reader.getIndexCommit().getGeneration() != 1L) {
                        if (this.replicateOnOptimize) {
                            final Collection<IndexCommit> commits = (Collection<IndexCommit>)DirectoryReader.listCommits(reader.directory());
                            for (final IndexCommit ic : commits) {
                                if (ic.getSegmentCount() == 1 && (this.indexCommitPoint == null || this.indexCommitPoint.getGeneration() < ic.getGeneration())) {
                                    this.indexCommitPoint = ic;
                                }
                            }
                        }
                        else {
                            this.indexCommitPoint = reader.getIndexCommit();
                        }
                    }
                    final RefCounted<IndexWriter> iw = core.getUpdateHandler().getSolrCoreState().getIndexWriter(core);
                    iw.decref();
                }
                catch (IOException e) {
                    ReplicationHandler.log.warn("Unable to get IndexCommit on startup", (Throwable)e);
                }
                finally {
                    if (s != null) {
                        s.decref();
                    }
                }
            }
            final String reserve = (String)master.get("commitReserveDuration");
            if (reserve != null && !reserve.trim().equals("")) {
                this.reserveCommitDuration = readIntervalMs(reserve);
                deprecatedReserveCommitDuration = this.reserveCommitDuration;
                XmlConfigFile.assertWarnOrFail("Beginning with Solr 7.1, master.commitReserveDuration is deprecated and should now be configured directly on the ReplicationHandler.", null == reserve, core.getSolrConfig().luceneMatchVersion.onOrAfter(Version.LUCENE_7_1_0));
            }
            this.isMaster = true;
        }
        final String reserve2 = (String)this.initArgs.get("commitReserveDuration");
        if (reserve2 != null && !reserve2.trim().equals("")) {
            this.reserveCommitDuration = readIntervalMs(reserve2);
            if (deprecatedReserveCommitDuration != null) {
                throw new IllegalArgumentException("'master.commitReserveDuration' and 'commitReserveDuration' are mutually exclusive.");
            }
        }
        ReplicationHandler.log.info("Commits will be reserved for " + this.reserveCommitDuration + "ms.");
    }
    
    private boolean isEnabled(final NamedList params) {
        if (params == null) {
            return false;
        }
        final Object enable = params.get("enable");
        if (enable == null) {
            return true;
        }
        if (enable instanceof String) {
            return StrUtils.parseBool((String)enable);
        }
        return Boolean.TRUE.equals(enable);
    }
    
    private void registerCloseHook() {
        this.core.addCloseHook(new CloseHook() {
            @Override
            public void preClose(final SolrCore core) {
                if (ReplicationHandler.this.executorService != null) {
                    ReplicationHandler.this.executorService.shutdown();
                }
            }
            
            @Override
            public void postClose(final SolrCore core) {
                if (ReplicationHandler.this.pollingIndexFetcher != null) {
                    ReplicationHandler.this.pollingIndexFetcher.destroy();
                }
                if (ReplicationHandler.this.currentIndexFetcher != null && ReplicationHandler.this.currentIndexFetcher != ReplicationHandler.this.pollingIndexFetcher) {
                    ReplicationHandler.this.currentIndexFetcher.destroy();
                }
            }
        });
        this.core.addCloseHook(new CloseHook() {
            @Override
            public void preClose(final SolrCore core) {
                ExecutorUtil.shutdownAndAwaitTermination(ReplicationHandler.this.restoreExecutor);
                if (ReplicationHandler.this.restoreFuture != null) {
                    ReplicationHandler.this.restoreFuture.cancel(false);
                }
            }
            
            @Override
            public void postClose(final SolrCore core) {
            }
        });
    }
    
    public void close() {
        if (this.executorService != null) {
            this.executorService.shutdown();
        }
        if (this.pollingIndexFetcher != null) {
            this.pollingIndexFetcher.destroy();
        }
        if (this.currentIndexFetcher != null && this.currentIndexFetcher != this.pollingIndexFetcher) {
            this.currentIndexFetcher.destroy();
        }
        ExecutorUtil.shutdownAndAwaitTermination(this.restoreExecutor);
        if (this.restoreFuture != null) {
            this.restoreFuture.cancel(false);
        }
        ExecutorUtil.shutdownAndAwaitTermination((ExecutorService)this.executorService);
    }
    
    private SolrEventListener getEventListener(final boolean snapshoot, final boolean getCommit) {
        return new SolrEventListener() {
            @Override
            public void init(final NamedList args) {
            }
            
            @Override
            public void postCommit() {
                final IndexCommit currentCommitPoint = ReplicationHandler.this.core.getDeletionPolicy().getLatestCommit();
                if (getCommit) {
                    ReplicationHandler.this.indexCommitPoint = currentCommitPoint;
                }
                if (snapshoot) {
                    try {
                        int numberToKeep = ReplicationHandler.this.numberBackupsToKeep;
                        if (numberToKeep < 1) {
                            numberToKeep = Integer.MAX_VALUE;
                        }
                        final SnapShooter snapShooter = new SnapShooter(ReplicationHandler.this.core, null, null);
                        snapShooter.validateCreateSnapshot();
                        snapShooter.createSnapAsync(numberToKeep, nl -> ReplicationHandler.this.snapShootDetails = nl);
                    }
                    catch (Exception e) {
                        ReplicationHandler.log.error("Exception while snapshooting", (Throwable)e);
                    }
                }
            }
            
            @Override
            public void newSearcher(final SolrIndexSearcher newSearcher, final SolrIndexSearcher currentSearcher) {
            }
            
            @Override
            public void postSoftCommit() {
            }
        };
    }
    
    private static Long readIntervalMs(final String interval) {
        return TimeUnit.MILLISECONDS.convert(readIntervalNs(interval), TimeUnit.NANOSECONDS);
    }
    
    private static Long readIntervalNs(final String interval) {
        if (interval == null) {
            return null;
        }
        int result = 0;
        final Matcher m = ReplicationHandler.INTERVAL_PATTERN.matcher(interval.trim());
        if (m.find()) {
            final String hr = m.group(1);
            final String min = m.group(2);
            final String sec = m.group(3);
            result = 0;
            try {
                if (sec != null && sec.length() > 0) {
                    result += Integer.parseInt(sec);
                }
                if (min != null && min.length() > 0) {
                    result += 60 * Integer.parseInt(min);
                }
                if (hr != null && hr.length() > 0) {
                    result += 3600 * Integer.parseInt(hr);
                }
                return TimeUnit.NANOSECONDS.convert(result, TimeUnit.SECONDS);
            }
            catch (NumberFormatException e) {
                throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "The pollInterval must be in this format 'HH:mm:ss'");
            }
        }
        throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "The pollInterval must be in this format 'HH:mm:ss'");
    }
    
    static {
        log = LoggerFactory.getLogger((Class)MethodHandles.lookup().lookupClass());
        INTERVAL_PATTERN = Pattern.compile("(\\d*?):(\\d*?):(\\d*)");
    }
    
    private static final class CommitVersionInfo
    {
        public final long version;
        public final long generation;
        
        private CommitVersionInfo(final long g, final long v) {
            this.generation = g;
            this.version = v;
        }
        
        public static CommitVersionInfo build(final IndexCommit commit) {
            final long generation = commit.getGeneration();
            long version = 0L;
            try {
                final Map<String, String> commitData = (Map<String, String>)commit.getUserData();
                final String commitTime = commitData.get("commitTimeMSec");
                if (commitTime != null) {
                    try {
                        version = Long.parseLong(commitTime);
                    }
                    catch (NumberFormatException e) {
                        ReplicationHandler.log.warn("Version in commitData was not formatted correctly: " + commitTime, (Throwable)e);
                    }
                }
            }
            catch (IOException e2) {
                ReplicationHandler.log.warn("Unable to get version from commitData, commit: " + commit, (Throwable)e2);
            }
            return new CommitVersionInfo(generation, version);
        }
        
        @Override
        public String toString() {
            return "generation=" + this.generation + ",version=" + this.version;
        }
    }
    
    static class FileInfo
    {
        long lastmodified;
        String name;
        long size;
        long checksum;
        
        public FileInfo(final long lasmodified, final String name, final long size, final long checksum) {
            this.lastmodified = lasmodified;
            this.name = name;
            this.size = size;
            this.checksum = checksum;
        }
        
        Map<String, Object> getAsMap() {
            final Map<String, Object> map = new HashMap<String, Object>();
            map.put("name", this.name);
            map.put("size", this.size);
            map.put("checksum", this.checksum);
            return map;
        }
    }
    
    private class DirectoryFileStream implements SolrCore.RawWriter
    {
        protected SolrParams params;
        protected FastOutputStream fos;
        protected Long indexGen;
        protected IndexDeletionPolicyWrapper delPolicy;
        protected String fileName;
        protected String cfileName;
        protected String tlogFileName;
        protected String sOffset;
        protected String sLen;
        protected String compress;
        protected boolean useChecksum;
        protected long offset;
        protected int len;
        protected Checksum checksum;
        private RateLimiter rateLimiter;
        byte[] buf;
        
        public DirectoryFileStream(final SolrParams solrParams) {
            this.offset = -1L;
            this.len = -1;
            this.params = solrParams;
            this.delPolicy = ReplicationHandler.this.core.getDeletionPolicy();
            this.fileName = this.validateFilenameOrError(this.params.get("file"));
            this.cfileName = this.validateFilenameOrError(this.params.get("cf"));
            this.tlogFileName = this.validateFilenameOrError(this.params.get("tlogFile"));
            this.sOffset = this.params.get("offset");
            this.sLen = this.params.get("len");
            this.compress = this.params.get("compression");
            this.useChecksum = this.params.getBool("checksum", false);
            this.indexGen = this.params.getLong("generation");
            if (this.useChecksum) {
                this.checksum = new Adler32();
            }
            final double maxWriteMBPerSec = this.params.getDouble("maxWriteMBPerSec", Double.MAX_VALUE);
            this.rateLimiter = (RateLimiter)new RateLimiter.SimpleRateLimiter(maxWriteMBPerSec);
        }
        
        protected String validateFilenameOrError(final String filename) {
            if (filename == null) {
                return null;
            }
            final Path filePath = Paths.get(filename, new String[0]);
            filePath.forEach(subpath -> {
                if ("..".equals(subpath.toString())) {
                    throw new SolrException(SolrException.ErrorCode.FORBIDDEN, "File name cannot contain ..");
                }
                else {
                    return;
                }
            });
            if (filePath.isAbsolute()) {
                throw new SolrException(SolrException.ErrorCode.FORBIDDEN, "File name must be relative");
            }
            return filename;
        }
        
        protected void initWrite() throws IOException {
            if (this.sOffset != null) {
                this.offset = Long.parseLong(this.sOffset);
            }
            if (this.sLen != null) {
                this.len = Integer.parseInt(this.sLen);
            }
            if (this.fileName == null && this.cfileName == null && this.tlogFileName == null) {
                this.writeNothingAndFlush();
            }
            this.buf = new byte[(this.len == -1 || this.len > 1048576) ? 1048576 : this.len];
            if (this.indexGen != null) {
                this.delPolicy.saveCommitPoint(this.indexGen);
            }
        }
        
        protected void createOutputStream(OutputStream out) {
            out = (OutputStream)new CloseShieldOutputStream(out);
            if (Boolean.parseBoolean(this.compress)) {
                this.fos = new FastOutputStream((OutputStream)new DeflaterOutputStream(out));
            }
            else {
                this.fos = new FastOutputStream(out);
            }
        }
        
        protected void extendReserveAndReleaseCommitPoint() {
            if (this.indexGen != null) {
                this.delPolicy.setReserveDuration(this.indexGen, ReplicationHandler.this.reserveCommitDuration);
                this.delPolicy.releaseCommitPoint(this.indexGen);
            }
        }
        
        @Override
        public void write(final OutputStream out) throws IOException {
            this.createOutputStream(out);
            IndexInput in = null;
            try {
                this.initWrite();
                final Directory dir = ReplicationHandler.this.core.withSearcher(searcher -> searcher.getIndexReader().directory());
                in = dir.openInput(this.fileName, IOContext.READONCE);
                if (this.offset != -1L) {
                    in.seek(this.offset);
                }
                final long filelen = dir.fileLength(this.fileName);
                long maxBytesBeforePause = 0L;
                while (true) {
                    this.offset = ((this.offset == -1L) ? 0L : this.offset);
                    final int read = (int)Math.min(this.buf.length, filelen - this.offset);
                    in.readBytes(this.buf, 0, read);
                    this.fos.writeInt(read);
                    if (this.useChecksum) {
                        this.checksum.reset();
                        this.checksum.update(this.buf, 0, read);
                        this.fos.writeLong(this.checksum.getValue());
                    }
                    this.fos.write(this.buf, 0, read);
                    this.fos.flush();
                    ReplicationHandler.log.debug("Wrote {} bytes for file {}", (Object)(this.offset + read), (Object)this.fileName);
                    maxBytesBeforePause += read;
                    if (maxBytesBeforePause >= this.rateLimiter.getMinPauseCheckBytes()) {
                        this.rateLimiter.pause(maxBytesBeforePause);
                        maxBytesBeforePause = 0L;
                    }
                    if (read != this.buf.length) {
                        break;
                    }
                    in.seek(this.offset += read);
                }
                this.writeNothingAndFlush();
                this.fos.close();
            }
            catch (IOException e) {
                ReplicationHandler.log.warn("Exception while writing response for params: " + this.params, (Throwable)e);
            }
            finally {
                if (in != null) {
                    in.close();
                }
                this.extendReserveAndReleaseCommitPoint();
            }
        }
        
        protected void writeNothingAndFlush() throws IOException {
            this.fos.writeInt(0);
            this.fos.flush();
        }
    }
    
    private abstract class LocalFsFileStream extends DirectoryFileStream
    {
        private File file;
        
        public LocalFsFileStream(final SolrParams solrParams) {
            super(solrParams);
            this.file = this.initFile();
        }
        
        protected abstract File initFile();
        
        @Override
        public void write(final OutputStream out) throws IOException {
            this.createOutputStream(out);
            FileInputStream inputStream = null;
            try {
                this.initWrite();
                if (this.file.exists() && this.file.canRead()) {
                    inputStream = new FileInputStream(this.file);
                    final FileChannel channel = inputStream.getChannel();
                    if (this.offset != -1L) {
                        channel.position(this.offset);
                    }
                    final ByteBuffer bb = ByteBuffer.wrap(this.buf);
                    while (true) {
                        bb.clear();
                        final long bytesRead = channel.read(bb);
                        if (bytesRead <= 0L) {
                            break;
                        }
                        this.fos.writeInt((int)bytesRead);
                        if (this.useChecksum) {
                            this.checksum.reset();
                            this.checksum.update(this.buf, 0, (int)bytesRead);
                            this.fos.writeLong(this.checksum.getValue());
                        }
                        this.fos.write(this.buf, 0, (int)bytesRead);
                        this.fos.flush();
                    }
                    this.writeNothingAndFlush();
                    this.fos.close();
                }
                else {
                    this.writeNothingAndFlush();
                }
            }
            catch (IOException e) {
                ReplicationHandler.log.warn("Exception while writing response for params: " + this.params, (Throwable)e);
            }
            finally {
                IOUtils.closeQuietly((InputStream)inputStream);
                this.extendReserveAndReleaseCommitPoint();
            }
        }
    }
    
    private class LocalFsTlogFileStream extends LocalFsFileStream
    {
        public LocalFsTlogFileStream(final SolrParams solrParams) {
            super(solrParams);
        }
        
        @Override
        protected File initFile() {
            return new File(ReplicationHandler.this.core.getUpdateHandler().getUpdateLog().getLogDir(), this.tlogFileName);
        }
    }
    
    private class LocalFsConfFileStream extends LocalFsFileStream
    {
        public LocalFsConfFileStream(final SolrParams solrParams) {
            super(solrParams);
        }
        
        @Override
        protected File initFile() {
            return new File(ReplicationHandler.this.core.getResourceLoader().getConfigDir(), this.cfileName);
        }
    }
    
    public interface PollListener
    {
        void onComplete(final SolrCore p0, final IndexFetcher.IndexFetchResult p1) throws IOException;
    }
}
