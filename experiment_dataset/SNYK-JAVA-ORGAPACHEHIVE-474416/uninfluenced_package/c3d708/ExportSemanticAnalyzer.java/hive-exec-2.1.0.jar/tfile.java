// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.hadoop.hive.ql.parse;

import java.util.Iterator;
import org.apache.hadoop.fs.FileStatus;
import java.net.URI;
import org.antlr.runtime.tree.Tree;
import org.apache.hadoop.hive.ql.hooks.ReadEntity;
import java.io.Serializable;
import org.apache.hadoop.hive.ql.exec.TaskFactory;
import org.apache.hadoop.hive.ql.exec.Task;
import org.apache.hadoop.hive.ql.plan.CopyWork;
import org.apache.hadoop.hive.ql.metadata.Partition;
import java.util.Map;
import org.apache.hadoop.hive.ql.metadata.PartitionIterable;
import org.apache.hadoop.hive.conf.HiveConf;
import java.io.IOException;
import java.io.FileNotFoundException;
import org.apache.hadoop.hive.common.FileUtils;
import org.apache.hadoop.hive.ql.ErrorMsg;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.hive.ql.metadata.Table;
import org.apache.hadoop.hive.ql.metadata.InvalidTableException;
import org.apache.hadoop.hive.ql.QueryState;

public class ExportSemanticAnalyzer extends BaseSemanticAnalyzer
{
    private ReplicationSpec replicationSpec;
    
    public ExportSemanticAnalyzer(final QueryState queryState) throws SemanticException {
        super(queryState);
    }
    
    @Override
    public void analyzeInternal(final ASTNode ast) throws SemanticException {
        final Tree tableTree = ast.getChild(0);
        final Tree toTree = ast.getChild(1);
        if (ast.getChildCount() > 2) {
            this.replicationSpec = new ReplicationSpec((ASTNode)ast.getChild(2));
        }
        else {
            this.replicationSpec = new ReplicationSpec();
        }
        final String tmpPath = BaseSemanticAnalyzer.stripQuotes(toTree.getText());
        final URI toURI = EximUtil.getValidatedURI(this.conf, tmpPath);
        TableSpec ts;
        try {
            ts = new TableSpec(this.db, this.conf, (ASTNode)tableTree, false, true);
        }
        catch (SemanticException sme) {
            if (!this.replicationSpec.isInReplicationScope() || (!(sme.getCause() instanceof InvalidTableException) && !(sme instanceof Table.ValidationFailureSemanticException))) {
                throw sme;
            }
            ts = null;
        }
        if (ts != null) {
            try {
                EximUtil.validateTable(ts.tableHandle);
                if (this.replicationSpec.isInReplicationScope() && ts.tableHandle.isTemporary()) {
                    ts = null;
                }
            }
            catch (SemanticException e) {
                if (!this.replicationSpec.isInReplicationScope()) {
                    throw e;
                }
                ts = null;
            }
        }
        try {
            final FileSystem fs = FileSystem.get(toURI, (Configuration)this.conf);
            final Path toPath = new Path(toURI.getScheme(), toURI.getAuthority(), toURI.getPath());
            try {
                final FileStatus tgt = fs.getFileStatus(toPath);
                if (!tgt.isDir()) {
                    throw new SemanticException(ErrorMsg.INVALID_PATH.getMsg(ast, "Target is not a directory : " + toURI));
                }
                final FileStatus[] files = fs.listStatus(toPath, FileUtils.HIDDEN_FILES_PATH_FILTER);
                if (files != null && files.length != 0) {
                    throw new SemanticException(ErrorMsg.INVALID_PATH.getMsg(ast, "Target is not an empty directory : " + toURI));
                }
            }
            catch (FileNotFoundException ex) {}
        }
        catch (IOException e2) {
            throw new SemanticException(ErrorMsg.INVALID_PATH.getMsg(ast), e2);
        }
        PartitionIterable partitions = null;
        try {
            this.replicationSpec.setCurrentReplicationState(String.valueOf(this.db.getMSC().getCurrentNotificationEventId().getEventId()));
            if (ts != null && ts.tableHandle.isPartitioned()) {
                if (ts.specType == TableSpec.SpecType.TABLE_ONLY) {
                    if (this.replicationSpec.isMetadataOnly()) {
                        partitions = null;
                    }
                    else {
                        partitions = new PartitionIterable(this.db, ts.tableHandle, null, this.conf.getIntVar(HiveConf.ConfVars.METASTORE_BATCH_RETRIEVE_MAX));
                    }
                }
                else {
                    partitions = new PartitionIterable(ts.partitions);
                }
            }
            else {
                partitions = null;
            }
            final Path path = new Path(this.ctx.getLocalTmpPath(), "_metadata");
            EximUtil.createExportDump((FileSystem)FileSystem.getLocal((Configuration)this.conf), path, (ts != null) ? ts.tableHandle : null, partitions, this.replicationSpec);
            final Task<? extends Serializable> rTask = TaskFactory.get((Serializable)new CopyWork(path, new Path(toURI), false), this.conf, (Task<? extends Serializable>[])new Task[0]);
            this.rootTasks.add(rTask);
            this.LOG.debug("_metadata file written into " + path.toString() + " and then copied to " + toURI.toString());
        }
        catch (Exception e3) {
            throw new SemanticException(ErrorMsg.IO_ERROR.getMsg("Exception while writing out the local file"), e3);
        }
        if (!this.replicationSpec.isMetadataOnly() && ts != null) {
            final Path parentPath = new Path(toURI);
            if (ts.tableHandle.isPartitioned()) {
                for (final Partition partition : partitions) {
                    final Path fromPath = partition.getDataLocation();
                    final Path toPartPath = new Path(parentPath, partition.getName());
                    final Task<? extends Serializable> rTask2 = TaskFactory.get((Serializable)new CopyWork(fromPath, toPartPath, false), this.conf, (Task<? extends Serializable>[])new Task[0]);
                    this.rootTasks.add(rTask2);
                    this.inputs.add(new ReadEntity(partition));
                }
            }
            else {
                final Path fromPath2 = ts.tableHandle.getDataLocation();
                final Path toDataPath = new Path(parentPath, "data");
                final Task<? extends Serializable> rTask3 = TaskFactory.get((Serializable)new CopyWork(fromPath2, toDataPath, false), this.conf, (Task<? extends Serializable>[])new Task[0]);
                this.rootTasks.add(rTask3);
                this.inputs.add(new ReadEntity(ts.tableHandle));
            }
            this.outputs.add(this.toWriteEntity(parentPath));
        }
    }
}
