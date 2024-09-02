// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.hadoop.hive.ql.parse;

import java.util.Iterator;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.hive.ql.exec.ReplCopyTask;
import org.apache.hadoop.hive.ql.metadata.Partition;
import java.util.Collection;
import java.util.Map;
import org.apache.hadoop.hive.ql.metadata.PartitionIterable;
import java.io.IOException;
import java.io.FileNotFoundException;
import org.apache.hadoop.hive.common.FileUtils;
import org.apache.hadoop.hive.ql.ErrorMsg;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.slf4j.Logger;
import org.apache.hadoop.hive.ql.hooks.WriteEntity;
import org.apache.hadoop.hive.ql.hooks.ReadEntity;
import java.util.HashSet;
import java.io.Serializable;
import org.apache.hadoop.hive.ql.exec.Task;
import java.util.List;
import org.apache.hadoop.hive.ql.Context;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.ql.metadata.Hive;
import java.net.URI;
import org.antlr.runtime.tree.Tree;
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
        prepareExport(ast, toURI, ts, this.replicationSpec, this.db, this.conf, this.ctx, this.rootTasks, this.inputs, this.outputs, this.LOG);
    }
    
    public static void prepareExport(final ASTNode ast, final URI toURI, TableSpec ts, final ReplicationSpec replicationSpec, final Hive db, final HiveConf conf, final Context ctx, final List<Task<? extends Serializable>> rootTasks, final HashSet<ReadEntity> inputs, final HashSet<WriteEntity> outputs, final Logger LOG) throws SemanticException {
        if (ts != null) {
            try {
                EximUtil.validateTable(ts.tableHandle);
                if (replicationSpec.isInReplicationScope() && ts.tableHandle.isTemporary()) {
                    ts = null;
                }
                else if (ts.tableHandle.isView()) {
                    replicationSpec.setIsMetadataOnly(true);
                }
            }
            catch (SemanticException e) {
                if (!replicationSpec.isInReplicationScope()) {
                    throw e;
                }
                ts = null;
            }
        }
        try {
            final FileSystem fs = FileSystem.get(toURI, (Configuration)conf);
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
            replicationSpec.setCurrentReplicationState(String.valueOf(db.getMSC().getCurrentNotificationEventId().getEventId()));
            if (ts != null && ts.tableHandle.isPartitioned()) {
                if (ts.specType == TableSpec.SpecType.TABLE_ONLY) {
                    if (replicationSpec.isMetadataOnly()) {
                        partitions = null;
                    }
                    else {
                        partitions = new PartitionIterable(db, ts.tableHandle, null, conf.getIntVar(HiveConf.ConfVars.METASTORE_BATCH_RETRIEVE_MAX));
                    }
                }
                else {
                    partitions = new PartitionIterable(ts.partitions);
                }
            }
            else {
                partitions = null;
            }
            final Path path = new Path(ctx.getLocalTmpPath(), "_metadata");
            EximUtil.createExportDump((FileSystem)FileSystem.getLocal((Configuration)conf), path, (ts != null) ? ts.tableHandle : null, partitions, replicationSpec);
            final Task<? extends Serializable> rTask = (Task<? extends Serializable>)ReplCopyTask.getDumpCopyTask(replicationSpec, path, new Path(toURI), conf);
            rootTasks.add(rTask);
            LOG.debug("_metadata file written into " + path.toString() + " and then copied to " + toURI.toString());
        }
        catch (Exception e3) {
            throw new SemanticException(ErrorMsg.IO_ERROR.getMsg("Exception while writing out the local file"), e3);
        }
        if (!replicationSpec.isMetadataOnly() && ts != null) {
            final Path parentPath = new Path(toURI);
            if (ts.tableHandle.isPartitioned()) {
                for (final Partition partition : partitions) {
                    final Path fromPath = partition.getDataLocation();
                    final Path toPartPath = new Path(parentPath, partition.getName());
                    final Task<? extends Serializable> rTask2 = (Task<? extends Serializable>)ReplCopyTask.getDumpCopyTask(replicationSpec, fromPath, toPartPath, conf);
                    rootTasks.add(rTask2);
                    inputs.add(new ReadEntity(partition));
                }
            }
            else {
                final Path fromPath2 = ts.tableHandle.getDataLocation();
                final Path toDataPath = new Path(parentPath, "data");
                final Task<? extends Serializable> rTask3 = (Task<? extends Serializable>)ReplCopyTask.getDumpCopyTask(replicationSpec, fromPath2, toDataPath, conf);
                rootTasks.add(rTask3);
                inputs.add(new ReadEntity(ts.tableHandle));
            }
            outputs.add(BaseSemanticAnalyzer.toWriteEntity(parentPath, conf));
        }
    }
}
