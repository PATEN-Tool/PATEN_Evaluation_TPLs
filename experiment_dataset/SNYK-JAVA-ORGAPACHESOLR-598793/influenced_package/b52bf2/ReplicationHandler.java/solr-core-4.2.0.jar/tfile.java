// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.solr.handler;

import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.util.zip.DeflaterOutputStream;
import org.apache.solr.common.util.FastOutputStream;
import org.slf4j.LoggerFactory;
import org.apache.solr.core.SolrEventListener;
import org.apache.solr.response.QueryResponseWriter;
import java.io.Writer;
import java.io.OutputStream;
import org.apache.solr.response.BinaryQueryResponseWriter;
import org.apache.solr.core.CloseHook;
import org.apache.solr.common.util.StrUtils;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexDeletionPolicy;
import org.apache.lucene.index.DirectoryReader;
import org.apache.solr.core.SolrDeletionPolicy;
import java.util.Arrays;
import org.apache.lucene.store.IndexInput;
import org.apache.solr.util.PropertiesInputStream;
import org.apache.lucene.store.IOContext;
import org.apache.solr.common.util.SimpleOrderedMap;
import java.util.Properties;
import java.util.Date;
import org.apache.solr.util.NumberUtils;
import org.apache.solr.util.RefCounted;
import org.apache.solr.search.SolrIndexSearcher;
import java.util.zip.Adler32;
import org.apache.lucene.store.Directory;
import org.apache.solr.core.DirectoryFactory;
import java.util.HashSet;
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
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.lucene.index.IndexCommit;
import java.util.Map;
import org.apache.solr.common.util.NamedList;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.solr.core.SolrCore;
import org.slf4j.Logger;
import org.apache.solr.util.plugin.SolrCoreAware;

public class ReplicationHandler extends RequestHandlerBase implements SolrCoreAware
{
    private static final Logger LOG;
    SolrCore core;
    private SnapPuller snapPuller;
    private ReentrantLock snapPullLock;
    private String includeConfFiles;
    private NamedList<String> confFileNameAlias;
    private boolean isMaster;
    private boolean isSlave;
    private boolean replicateOnOptimize;
    private boolean replicateOnCommit;
    private boolean replicateOnStart;
    private int numberBackupsToKeep;
    private int numTimesReplicated;
    private final Map<String, FileInfo> confFileInfoCache;
    private Integer reserveCommitDuration;
    volatile IndexCommit indexCommitPoint;
    volatile NamedList<Object> snapShootDetails;
    private AtomicBoolean replicationEnabled;
    private volatile SnapPuller tempSnapPuller;
    public static final String MASTER_URL = "masterUrl";
    public static final String STATUS = "status";
    public static final String COMMAND = "command";
    public static final String CMD_DETAILS = "details";
    public static final String CMD_BACKUP = "backup";
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
    public static final String GENERATION = "generation";
    public static final String OFFSET = "offset";
    public static final String LEN = "len";
    public static final String FILE = "file";
    public static final String NAME = "name";
    public static final String SIZE = "size";
    public static final String CONF_FILE_SHORT = "cf";
    public static final String CHECKSUM = "checksum";
    public static final String ALIAS = "alias";
    public static final String CONF_CHECKSUM = "confchecksum";
    public static final String CONF_FILES = "confFiles";
    public static final String REPLICATE_AFTER = "replicateAfter";
    public static final String FILE_STREAM = "filestream";
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
    
    public ReplicationHandler() {
        this.snapPullLock = new ReentrantLock();
        this.confFileNameAlias = (NamedList<String>)new NamedList();
        this.isMaster = false;
        this.isSlave = false;
        this.replicateOnOptimize = false;
        this.replicateOnCommit = false;
        this.replicateOnStart = false;
        this.numberBackupsToKeep = 0;
        this.numTimesReplicated = 0;
        this.confFileInfoCache = new HashMap<String, FileInfo>();
        this.reserveCommitDuration = SnapPuller.readInterval("00:00:10");
        this.replicationEnabled = new AtomicBoolean(true);
    }
    
    @Override
    public void handleRequestBody(final SolrQueryRequest req, final SolrQueryResponse rsp) throws Exception {
        rsp.setHttpCaching(false);
        final SolrParams solrParams = req.getParams();
        final String command = solrParams.get("command");
        if (command == null) {
            rsp.add("status", "OK");
            rsp.add("message", "No command");
            return;
        }
        if (command.equals("indexversion")) {
            IndexCommit commitPoint = this.indexCommitPoint;
            if (commitPoint == null) {
                commitPoint = this.core.getDeletionPolicy().getLatestCommit();
            }
            if (commitPoint != null && this.replicationEnabled.get()) {
                this.core.getDeletionPolicy().setReserveDuration(commitPoint.getGeneration(), this.reserveCommitDuration);
                rsp.add("indexversion", IndexDeletionPolicyWrapper.getCommitTimestamp(commitPoint));
                rsp.add("generation", commitPoint.getGeneration());
            }
            else {
                rsp.add("indexversion", 0L);
                rsp.add("generation", 0L);
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
            rsp.add("status", "OK");
        }
        else if (command.equalsIgnoreCase("fetchindex")) {
            final String masterUrl = solrParams.get("masterUrl");
            if (!this.isSlave && masterUrl == null) {
                rsp.add("status", "ERROR");
                rsp.add("message", "No slave configured or no 'masterUrl' Specified");
                return;
            }
            final SolrParams paramsCopy = (SolrParams)new ModifiableSolrParams(solrParams);
            new Thread() {
                @Override
                public void run() {
                    ReplicationHandler.this.doFetch(paramsCopy, false);
                }
            }.start();
            rsp.add("status", "OK");
        }
        else if (command.equalsIgnoreCase("disablepoll")) {
            if (this.snapPuller != null) {
                this.snapPuller.disablePoll();
                rsp.add("status", "OK");
            }
            else {
                rsp.add("status", "ERROR");
                rsp.add("message", "No slave configured");
            }
        }
        else if (command.equalsIgnoreCase("enablepoll")) {
            if (this.snapPuller != null) {
                this.snapPuller.enablePoll();
                rsp.add("status", "OK");
            }
            else {
                rsp.add("status", "ERROR");
                rsp.add("message", "No slave configured");
            }
        }
        else if (command.equalsIgnoreCase("abortfetch")) {
            final SnapPuller temp = this.tempSnapPuller;
            if (temp != null) {
                temp.abortPull();
                rsp.add("status", "OK");
            }
            else {
                rsp.add("status", "ERROR");
                rsp.add("message", "No slave configured");
            }
        }
        else if (command.equals("commits")) {
            rsp.add("commits", this.getCommits());
        }
        else if (command.equals("details")) {
            rsp.add("details", this.getReplicationDetails(solrParams.getBool("slave", true)));
            RequestHandlerUtils.addExperimentalFormatWarning(rsp);
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
                ReplicationHandler.LOG.warn("Exception while reading files for commit " + c, (Throwable)e);
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
            ReplicationHandler.LOG.warn("Exception in finding checksum of " + f, (Throwable)e);
        }
        finally {
            IOUtils.closeQuietly((InputStream)fis);
        }
        return null;
    }
    
    public boolean doFetch(final SolrParams solrParams, final boolean forceReplication) {
        final String masterUrl = (solrParams == null) ? null : solrParams.get("masterUrl");
        if (!this.snapPullLock.tryLock()) {
            return false;
        }
        try {
            this.tempSnapPuller = this.snapPuller;
            if (masterUrl != null) {
                final NamedList<Object> nl = (NamedList<Object>)solrParams.toNamedList();
                nl.remove("pollInterval");
                this.tempSnapPuller = new SnapPuller(nl, this, this.core);
            }
            return this.tempSnapPuller.fetchLatestIndex(this.core, forceReplication);
        }
        catch (Exception e) {
            SolrException.log(ReplicationHandler.LOG, "SnapPull failed ", (Throwable)e);
        }
        finally {
            if (this.snapPuller != null) {
                this.tempSnapPuller = this.snapPuller;
            }
            this.snapPullLock.unlock();
        }
        return false;
    }
    
    boolean isReplicating() {
        return this.snapPullLock.isLocked();
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
            final IndexDeletionPolicyWrapper delPolicy = this.core.getDeletionPolicy();
            IndexCommit indexCommit = delPolicy.getLatestCommit();
            if (indexCommit == null) {
                indexCommit = req.getSearcher().getIndexReader().getIndexCommit();
            }
            new SnapShooter(this.core, params.get("location")).createSnapAsync(indexCommit, numberToKeep, this);
        }
        catch (Exception e) {
            ReplicationHandler.LOG.warn("Exception during creating a snapshot", (Throwable)e);
            rsp.add("exception", e);
        }
    }
    
    private void getFileStream(final SolrParams solrParams, final SolrQueryResponse rsp) {
        final ModifiableSolrParams rawParams = new ModifiableSolrParams(solrParams);
        rawParams.set("wt", new String[] { "filestream" });
        final String cfileName = solrParams.get("cf");
        if (cfileName != null) {
            rsp.add("filestream", new LocalFsFileStream(solrParams));
        }
        else {
            rsp.add("filestream", new DirectoryFileStream(solrParams));
        }
    }
    
    private void getFileList(final SolrParams solrParams, final SolrQueryResponse rsp) {
        final String v = solrParams.get("generation");
        if (v == null) {
            rsp.add("status", "no index generation specified");
            return;
        }
        final long gen = Long.parseLong(v);
        final IndexCommit commit = this.core.getDeletionPolicy().getCommitPoint(gen);
        if (commit == null) {
            rsp.add("status", "invalid index generation");
            return;
        }
        this.core.getDeletionPolicy().setReserveDuration(gen, this.reserveCommitDuration);
        final List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        Directory dir = null;
        try {
            final Collection<String> files = new HashSet<String>(commit.getFileNames());
            dir = this.core.getDirectoryFactory().get(this.core.getNewIndexDir(), DirectoryFactory.DirContext.DEFAULT, this.core.getSolrConfig().indexConfig.lockType);
            try {
                for (final String fileName : files) {
                    if (fileName.endsWith(".lock")) {
                        continue;
                    }
                    final Map<String, Object> fileMeta = new HashMap<String, Object>();
                    fileMeta.put("name", fileName);
                    fileMeta.put("size", dir.fileLength(fileName));
                    result.add(fileMeta);
                }
            }
            finally {
                this.core.getDirectoryFactory().release(dir);
            }
        }
        catch (IOException e) {
            rsp.add("status", "unable to get file names for given index generation");
            rsp.add("exception", e);
            ReplicationHandler.LOG.error("Unable to get file names for indexCommit generation: " + gen, (Throwable)e);
        }
        rsp.add("filelist", result);
        if (this.confFileNameAlias.size() < 1 || this.core.getCoreDescriptor().getCoreContainer().isZooKeeperAware()) {
            return;
        }
        ReplicationHandler.LOG.debug("Adding config files to list: " + this.includeConfFiles);
        rsp.add("confFiles", this.getConfFileInfoFromCache(this.confFileNameAlias, this.confFileInfoCache));
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
    
    void disablePoll() {
        if (this.isSlave) {
            this.snapPuller.disablePoll();
        }
    }
    
    void enablePoll() {
        if (this.isSlave) {
            this.snapPuller.enablePoll();
        }
    }
    
    boolean isPollingDisabled() {
        return this.snapPuller == null || this.snapPuller.isPollingDisabled();
    }
    
    int getTimesReplicatedSinceStartup() {
        return this.numTimesReplicated;
    }
    
    void setTimesReplicatedSinceStartup() {
        ++this.numTimesReplicated;
    }
    
    long getIndexSize() {
        long size = 0L;
        try {
            final Directory dir = this.core.getDirectoryFactory().get(this.core.getIndexDir(), DirectoryFactory.DirContext.DEFAULT, this.core.getSolrConfig().indexConfig.lockType);
            try {
                size = DirectoryFactory.sizeOfDirectory(dir);
            }
            finally {
                this.core.getDirectoryFactory().release(dir);
            }
        }
        catch (IOException e) {
            SolrException.log(ReplicationHandler.LOG, "IO error while trying to get the size of the Directory", (Throwable)e);
        }
        return size;
    }
    
    @Override
    public String getDescription() {
        return "ReplicationHandler provides replication of index and configuration files from Master to Slaves";
    }
    
    @Override
    public String getSource() {
        return "$URL: https://svn.apache.org/repos/asf/lucene/dev/branches/branch_4x/solr/core/src/java/org/apache/solr/handler/ReplicationHandler.java $";
    }
    
    private long[] getIndexVersion() {
        final long[] version = new long[2];
        final RefCounted<SolrIndexSearcher> searcher = this.core.getSearcher();
        try {
            final IndexCommit commit = searcher.get().getIndexReader().getIndexCommit();
            final Map<String, String> commitData = (Map<String, String>)commit.getUserData();
            final String commitTime = commitData.get("commitTimeMSec");
            if (commitTime != null) {
                version[0] = Long.parseLong(commitTime);
            }
            version[1] = commit.getGeneration();
        }
        catch (IOException e) {
            ReplicationHandler.LOG.warn("Unable to get index version : ", (Throwable)e);
        }
        finally {
            searcher.decref();
        }
        return version;
    }
    
    @Override
    public NamedList getStatistics() {
        final NamedList list = super.getStatistics();
        if (this.core != null) {
            list.add("indexSize", (Object)NumberUtils.readableSize(this.getIndexSize()));
            final long[] versionGen = this.getIndexVersion();
            list.add("indexVersion", (Object)versionGen[0]);
            list.add("generation", (Object)versionGen[1]);
            list.add("indexPath", (Object)this.core.getIndexDir());
            list.add("isMaster", (Object)String.valueOf(this.isMaster));
            list.add("isSlave", (Object)String.valueOf(this.isSlave));
            final SnapPuller snapPuller = this.tempSnapPuller;
            if (snapPuller != null) {
                list.add("masterUrl", (Object)snapPuller.getMasterUrl());
                if (snapPuller.getPollInterval() != null) {
                    list.add("pollInterval", (Object)snapPuller.getPollInterval());
                }
                list.add("isPollingDisabled", (Object)String.valueOf(this.isPollingDisabled()));
                list.add("isReplicating", (Object)String.valueOf(this.isReplicating()));
                final long elapsed = this.getTimeElapsed(snapPuller);
                final long val = SnapPuller.getTotalBytesDownloaded(snapPuller);
                if (elapsed > 0L) {
                    list.add("timeElapsed", (Object)elapsed);
                    list.add("bytesDownloaded", (Object)val);
                    list.add("downloadSpeed", (Object)(val / elapsed));
                }
                final Properties props = this.loadReplicationProperties();
                this.addVal((NamedList<Object>)list, "previousCycleTimeInSeconds", props, Long.class);
                this.addVal((NamedList<Object>)list, "indexReplicatedAt", props, Date.class);
                this.addVal((NamedList<Object>)list, "confFilesReplicatedAt", props, Date.class);
                this.addVal((NamedList<Object>)list, "replicationFailedAt", props, Date.class);
                this.addVal((NamedList<Object>)list, "timesFailed", props, Integer.class);
                this.addVal((NamedList<Object>)list, "timesIndexReplicated", props, Integer.class);
                this.addVal((NamedList<Object>)list, "lastCycleBytesDownloaded", props, Long.class);
                this.addVal((NamedList<Object>)list, "timesConfigReplicated", props, Integer.class);
                this.addVal((NamedList<Object>)list, "confFilesReplicated", props, String.class);
            }
            if (this.isMaster) {
                if (this.includeConfFiles != null) {
                    list.add("confFilesToReplicate", (Object)this.includeConfFiles);
                }
                list.add("replicateAfter", (Object)this.getReplicateAfterStrings());
                list.add("replicationEnabled", (Object)String.valueOf(this.replicationEnabled.get()));
            }
        }
        return list;
    }
    
    private NamedList<Object> getReplicationDetails(final boolean showSlaveDetails) {
        final NamedList<Object> details = (NamedList<Object>)new SimpleOrderedMap();
        final NamedList<Object> master = (NamedList<Object>)new SimpleOrderedMap();
        final NamedList<Object> slave = (NamedList<Object>)new SimpleOrderedMap();
        details.add("indexSize", (Object)NumberUtils.readableSize(this.getIndexSize()));
        details.add("indexPath", (Object)this.core.getIndexDir());
        details.add("commits", (Object)this.getCommits());
        details.add("isMaster", (Object)String.valueOf(this.isMaster));
        details.add("isSlave", (Object)String.valueOf(this.isSlave));
        final long[] versionAndGeneration = this.getIndexVersion();
        details.add("indexVersion", (Object)versionAndGeneration[0]);
        details.add("generation", (Object)versionAndGeneration[1]);
        final IndexCommit commit = this.indexCommitPoint;
        if (this.isMaster) {
            if (this.includeConfFiles != null) {
                master.add("confFiles", (Object)this.includeConfFiles);
            }
            master.add("replicateAfter", (Object)this.getReplicateAfterStrings());
            master.add("replicationEnabled", (Object)String.valueOf(this.replicationEnabled.get()));
        }
        if (this.isMaster && commit != null) {
            master.add("replicatableGeneration", (Object)commit.getGeneration());
        }
        final SnapPuller snapPuller = this.tempSnapPuller;
        if (snapPuller != null) {
            final Properties props = this.loadReplicationProperties();
            if (showSlaveDetails) {
                try {
                    final NamedList nl = snapPuller.getDetails();
                    slave.add("masterDetails", nl.get("details"));
                }
                catch (Exception e) {
                    ReplicationHandler.LOG.warn("Exception while invoking 'details' method for replication on master ", (Throwable)e);
                    slave.add("ERROR", (Object)"invalid_master");
                }
            }
            slave.add("masterUrl", (Object)snapPuller.getMasterUrl());
            if (snapPuller.getPollInterval() != null) {
                slave.add("pollInterval", (Object)snapPuller.getPollInterval());
            }
            if (snapPuller.getNextScheduledExecTime() != null && !this.isPollingDisabled()) {
                slave.add("nextExecutionAt", (Object)new Date(snapPuller.getNextScheduledExecTime()).toString());
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
            slave.add("currentDate", (Object)new Date().toString());
            slave.add("isPollingDisabled", (Object)String.valueOf(this.isPollingDisabled()));
            final boolean isReplicating = this.isReplicating();
            slave.add("isReplicating", (Object)String.valueOf(isReplicating));
            if (isReplicating) {
                try {
                    long bytesToDownload = 0L;
                    final List<String> filesToDownload = new ArrayList<String>();
                    for (final Map<String, Object> file : snapPuller.getFilesToDownload()) {
                        filesToDownload.add(file.get("name"));
                        bytesToDownload += file.get("size");
                    }
                    for (final Map<String, Object> file : snapPuller.getConfFilesToDownload()) {
                        filesToDownload.add(file.get("name"));
                        bytesToDownload += file.get("size");
                    }
                    slave.add("filesToDownload", (Object)filesToDownload);
                    slave.add("numFilesToDownload", (Object)String.valueOf(filesToDownload.size()));
                    slave.add("bytesToDownload", (Object)NumberUtils.readableSize(bytesToDownload));
                    long bytesDownloaded = 0L;
                    final List<String> filesDownloaded = new ArrayList<String>();
                    for (final Map<String, Object> file2 : snapPuller.getFilesDownloaded()) {
                        filesDownloaded.add(file2.get("name"));
                        bytesDownloaded += file2.get("size");
                    }
                    for (final Map<String, Object> file2 : snapPuller.getConfFilesDownloaded()) {
                        filesDownloaded.add(file2.get("name"));
                        bytesDownloaded += file2.get("size");
                    }
                    final Map<String, Object> currentFile = snapPuller.getCurrentFile();
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
                    if (snapPuller.getReplicationStartTime() > 0L) {
                        slave.add("replicationStartTime", (Object)new Date(snapPuller.getReplicationStartTime()).toString());
                    }
                    final long elapsed = this.getTimeElapsed(snapPuller);
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
                    ReplicationHandler.LOG.error("Exception while writing replication details: ", (Throwable)e2);
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
        return details;
    }
    
    private void addVal(final NamedList<Object> nl, final String key, final Properties props, final Class clzz) {
        final String s = props.getProperty(key);
        if (s == null || s.trim().length() == 0) {
            return;
        }
        if (clzz == Date.class) {
            try {
                final Long l = Long.parseLong(s);
                nl.add(key, (Object)new Date(l).toString());
            }
            catch (NumberFormatException e) {}
        }
        else if (clzz == List.class) {
            final String[] ss = s.split(",");
            final List<String> i = new ArrayList<String>();
            for (int j = 0; j < ss.length; ++j) {
                i.add(new Date(Long.valueOf(ss[j])).toString());
            }
            nl.add(key, (Object)i);
        }
        else {
            nl.add(key, (Object)s);
        }
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
    
    private long getTimeElapsed(final SnapPuller snapPuller) {
        long timeElapsed = 0L;
        if (snapPuller.getReplicationStartTime() > 0L) {
            timeElapsed = (System.currentTimeMillis() - snapPuller.getReplicationStartTime()) / 1000L;
        }
        return timeElapsed;
    }
    
    Properties loadReplicationProperties() {
        Directory dir = null;
        try {
            try {
                dir = this.core.getDirectoryFactory().get(this.core.getDataDir(), DirectoryFactory.DirContext.META_DATA, this.core.getSolrConfig().indexConfig.lockType);
                if (!dir.fileExists("replication.properties")) {
                    return new Properties();
                }
                final IndexInput input = dir.openInput("replication.properties", IOContext.DEFAULT);
                try {
                    final InputStream is = new PropertiesInputStream(input);
                    final Properties props = new Properties();
                    props.load(is);
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
        catch (IOException e) {
            throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, (Throwable)e);
        }
    }
    
    @Override
    public void inform(final SolrCore core) {
        this.core = core;
        this.registerFileStreamResponseWriter();
        this.registerCloseHook();
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
            final SnapPuller snapPuller = new SnapPuller(slave, this, core);
            this.snapPuller = snapPuller;
            this.tempSnapPuller = snapPuller;
            this.isSlave = true;
        }
        NamedList master = (NamedList)this.initArgs.get("master");
        boolean enableMaster = this.isEnabled(master);
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
                ReplicationHandler.LOG.info("Replication enabled for following config files: " + this.includeConfFiles);
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
                    ReplicationHandler.LOG.warn("Replication can't call setMaxOptimizedCommitsToKeep on " + policy);
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
                        try {
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
                        finally {}
                    }
                    final RefCounted<IndexWriter> iw = core.getUpdateHandler().getSolrCoreState().getIndexWriter(core);
                    iw.decref();
                }
                catch (IOException e) {
                    ReplicationHandler.LOG.warn("Unable to get IndexCommit on startup", (Throwable)e);
                }
                finally {
                    if (s != null) {
                        s.decref();
                    }
                }
            }
            final String reserve = (String)master.get("commitReserveDuration");
            if (reserve != null && !reserve.trim().equals("")) {
                this.reserveCommitDuration = SnapPuller.readInterval(reserve);
            }
            ReplicationHandler.LOG.info("Commits will be reserved for  " + this.reserveCommitDuration);
            this.isMaster = true;
        }
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
                if (ReplicationHandler.this.snapPuller != null) {
                    ReplicationHandler.this.snapPuller.destroy();
                }
            }
            
            @Override
            public void postClose(final SolrCore core) {
            }
        });
    }
    
    private void registerFileStreamResponseWriter() {
        this.core.registerResponseWriter("filestream", new BinaryQueryResponseWriter() {
            @Override
            public void write(final OutputStream out, final SolrQueryRequest request, final SolrQueryResponse resp) throws IOException {
                final DirectoryFileStream stream = (DirectoryFileStream)resp.getValues().get("filestream");
                stream.write(out);
            }
            
            @Override
            public void write(final Writer writer, final SolrQueryRequest request, final SolrQueryResponse response) {
                throw new RuntimeException("This is a binary writer , Cannot write to a characterstream");
            }
            
            @Override
            public String getContentType(final SolrQueryRequest request, final SolrQueryResponse response) {
                return "application/octet-stream";
            }
            
            @Override
            public void init(final NamedList args) {
            }
        });
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
                        final SnapShooter snapShooter = new SnapShooter(ReplicationHandler.this.core, null);
                        snapShooter.createSnapAsync(currentCommitPoint, numberToKeep, ReplicationHandler.this);
                    }
                    catch (Exception e) {
                        ReplicationHandler.LOG.error("Exception while snapshooting", (Throwable)e);
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
    
    static {
        LOG = LoggerFactory.getLogger(ReplicationHandler.class.getName());
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
    
    private class DirectoryFileStream
    {
        protected SolrParams params;
        protected FastOutputStream fos;
        protected Long indexGen;
        protected IndexDeletionPolicyWrapper delPolicy;
        
        public DirectoryFileStream(final SolrParams solrParams) {
            this.params = solrParams;
            this.delPolicy = ReplicationHandler.this.core.getDeletionPolicy();
        }
        
        public void write(final OutputStream out) throws IOException {
            final String fileName = this.params.get("file");
            final String cfileName = this.params.get("cf");
            final String sOffset = this.params.get("offset");
            final String sLen = this.params.get("len");
            final String compress = this.params.get("compression");
            final String sChecksum = this.params.get("checksum");
            final String sGen = this.params.get("generation");
            if (sGen != null) {
                this.indexGen = Long.parseLong(sGen);
            }
            if (Boolean.parseBoolean(compress)) {
                this.fos = new FastOutputStream((OutputStream)new DeflaterOutputStream(out));
            }
            else {
                this.fos = new FastOutputStream(out);
            }
            int packetsWritten = 0;
            IndexInput in = null;
            try {
                long offset = -1L;
                int len = -1;
                final boolean useChecksum = Boolean.parseBoolean(sChecksum);
                if (sOffset != null) {
                    offset = Long.parseLong(sOffset);
                }
                if (sLen != null) {
                    len = Integer.parseInt(sLen);
                }
                if (fileName == null && cfileName == null) {
                    this.writeNothing();
                }
                final RefCounted<SolrIndexSearcher> sref = ReplicationHandler.this.core.getSearcher();
                Directory dir;
                try {
                    final SolrIndexSearcher searcher = sref.get();
                    dir = searcher.getIndexReader().directory();
                }
                finally {
                    sref.decref();
                }
                in = dir.openInput(fileName, IOContext.READONCE);
                if (offset != -1L) {
                    in.seek(offset);
                }
                final byte[] buf = new byte[(len == -1 || len > 1048576) ? 1048576 : len];
                Checksum checksum = null;
                if (useChecksum) {
                    checksum = new Adler32();
                }
                final long filelen = dir.fileLength(fileName);
                while (true) {
                    offset = ((offset == -1L) ? 0L : offset);
                    final int read = (int)Math.min(buf.length, filelen - offset);
                    in.readBytes(buf, 0, read);
                    this.fos.writeInt(read);
                    if (useChecksum) {
                        checksum.reset();
                        checksum.update(buf, 0, read);
                        this.fos.writeLong(checksum.getValue());
                    }
                    this.fos.write(buf, 0, read);
                    this.fos.flush();
                    if (this.indexGen != null && packetsWritten % 5 == 0) {
                        this.delPolicy.setReserveDuration(this.indexGen, ReplicationHandler.this.reserveCommitDuration);
                    }
                    ++packetsWritten;
                    if (read != buf.length) {
                        break;
                    }
                    offset += read;
                    in.seek(offset);
                }
                this.writeNothing();
                this.fos.close();
            }
            catch (IOException e) {
                ReplicationHandler.LOG.warn("Exception while writing response for params: " + this.params, (Throwable)e);
            }
            finally {
                if (in != null) {
                    in.close();
                }
            }
        }
        
        protected void writeNothing() throws IOException {
            this.fos.writeInt(0);
            this.fos.flush();
        }
    }
    
    private class LocalFsFileStream extends DirectoryFileStream
    {
        public LocalFsFileStream(final SolrParams solrParams) {
            super(solrParams);
        }
        
        @Override
        public void write(final OutputStream out) throws IOException {
            final String fileName = this.params.get("file");
            final String cfileName = this.params.get("cf");
            final String sOffset = this.params.get("offset");
            final String sLen = this.params.get("len");
            final String compress = this.params.get("compression");
            final String sChecksum = this.params.get("checksum");
            final String sGen = this.params.get("generation");
            if (sGen != null) {
                this.indexGen = Long.parseLong(sGen);
            }
            if (Boolean.parseBoolean(compress)) {
                this.fos = new FastOutputStream((OutputStream)new DeflaterOutputStream(out));
            }
            else {
                this.fos = new FastOutputStream(out);
            }
            FileInputStream inputStream = null;
            int packetsWritten = 0;
            try {
                long offset = -1L;
                int len = -1;
                final boolean useChecksum = Boolean.parseBoolean(sChecksum);
                if (sOffset != null) {
                    offset = Long.parseLong(sOffset);
                }
                if (sLen != null) {
                    len = Integer.parseInt(sLen);
                }
                if (fileName == null && cfileName == null) {
                    this.writeNothing();
                }
                File file = null;
                file = new File(ReplicationHandler.this.core.getResourceLoader().getConfigDir(), cfileName);
                if (file.exists() && file.canRead()) {
                    inputStream = new FileInputStream(file);
                    final FileChannel channel = inputStream.getChannel();
                    if (offset != -1L) {
                        channel.position(offset);
                    }
                    final byte[] buf = new byte[(len == -1 || len > 1048576) ? 1048576 : len];
                    Checksum checksum = null;
                    if (useChecksum) {
                        checksum = new Adler32();
                    }
                    final ByteBuffer bb = ByteBuffer.wrap(buf);
                    while (true) {
                        bb.clear();
                        final long bytesRead = channel.read(bb);
                        if (bytesRead <= 0L) {
                            break;
                        }
                        this.fos.writeInt((int)bytesRead);
                        if (useChecksum) {
                            checksum.reset();
                            checksum.update(buf, 0, (int)bytesRead);
                            this.fos.writeLong(checksum.getValue());
                        }
                        this.fos.write(buf, 0, (int)bytesRead);
                        this.fos.flush();
                        if (this.indexGen != null && packetsWritten % 5 == 0) {
                            this.delPolicy.setReserveDuration(this.indexGen, ReplicationHandler.this.reserveCommitDuration);
                        }
                        ++packetsWritten;
                    }
                    this.writeNothing();
                    this.fos.close();
                }
                else {
                    this.writeNothing();
                }
            }
            catch (IOException e) {
                ReplicationHandler.LOG.warn("Exception while writing response for params: " + this.params, (Throwable)e);
            }
            finally {
                IOUtils.closeQuietly((InputStream)inputStream);
            }
        }
    }
}
