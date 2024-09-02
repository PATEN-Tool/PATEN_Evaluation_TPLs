// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.hadoop.hive.ql.parse;

import java.util.Iterator;
import java.util.List;
import org.apache.hadoop.fs.FileStatus;
import java.net.URI;
import org.antlr.runtime.tree.Tree;
import org.apache.hadoop.hive.ql.hooks.WriteEntity;
import org.apache.hadoop.hive.ql.hooks.ReadEntity;
import org.apache.hadoop.hive.ql.metadata.Partition;
import java.io.Serializable;
import org.apache.hadoop.hive.ql.exec.TaskFactory;
import org.apache.hadoop.hive.ql.exec.Task;
import org.apache.hadoop.hive.ql.plan.CopyWork;
import java.io.IOException;
import java.io.FileNotFoundException;
import org.apache.hadoop.hive.ql.ErrorMsg;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.hive.conf.HiveConf;

public class ExportSemanticAnalyzer extends BaseSemanticAnalyzer
{
    public ExportSemanticAnalyzer(final HiveConf conf) throws SemanticException {
        super(conf);
    }
    
    @Override
    public void analyzeInternal(final ASTNode ast) throws SemanticException {
        final Tree tableTree = ast.getChild(0);
        final Tree toTree = ast.getChild(1);
        final String tmpPath = BaseSemanticAnalyzer.stripQuotes(toTree.getText());
        final URI toURI = EximUtil.getValidatedURI(this.conf, tmpPath);
        final tableSpec ts = new tableSpec(this.db, this.conf, (ASTNode)tableTree, false, true);
        EximUtil.validateTable(ts.tableHandle);
        try {
            final FileSystem fs = FileSystem.get(toURI, (Configuration)this.conf);
            final Path toPath = new Path(toURI.getScheme(), toURI.getAuthority(), toURI.getPath());
            try {
                final FileStatus tgt = fs.getFileStatus(toPath);
                if (!tgt.isDir()) {
                    throw new SemanticException(ErrorMsg.INVALID_PATH.getMsg(ast, "Target is not a directory : " + toURI));
                }
                final FileStatus[] files = fs.listStatus(toPath);
                if (files != null) {
                    throw new SemanticException(ErrorMsg.INVALID_PATH.getMsg(ast, "Target is not an empty directory : " + toURI));
                }
            }
            catch (FileNotFoundException ex) {}
        }
        catch (IOException e) {
            throw new SemanticException(ErrorMsg.INVALID_PATH.getMsg(ast), e);
        }
        List<Partition> partitions = null;
        try {
            partitions = null;
            if (ts.tableHandle.isPartitioned()) {
                partitions = ((ts.partitions != null) ? ts.partitions : this.db.getPartitions(ts.tableHandle));
            }
            final String tmpfile = this.ctx.getLocalTmpFileURI();
            final Path path = new Path(tmpfile, "_metadata");
            EximUtil.createExportDump((FileSystem)FileSystem.getLocal((Configuration)this.conf), path, ts.tableHandle, partitions);
            final Task<? extends Serializable> rTask = TaskFactory.get((Serializable)new CopyWork(path.toString(), toURI.toString(), false), this.conf, (Task<? extends Serializable>[])new Task[0]);
            this.rootTasks.add(rTask);
            this.LOG.debug((Object)("_metadata file written into " + path.toString() + " and then copied to " + toURI.toString()));
        }
        catch (Exception e2) {
            throw new SemanticException(ErrorMsg.GENERIC_ERROR.getMsg("Exception while writing out the local file"), e2);
        }
        if (ts.tableHandle.isPartitioned()) {
            for (final Partition partition : partitions) {
                final URI fromURI = partition.getDataLocation();
                final Path toPartPath = new Path(toURI.toString(), partition.getName());
                final Task<? extends Serializable> rTask2 = TaskFactory.get((Serializable)new CopyWork(fromURI.toString(), toPartPath.toString(), false), this.conf, (Task<? extends Serializable>[])new Task[0]);
                this.rootTasks.add(rTask2);
                this.inputs.add(new ReadEntity(partition));
            }
        }
        else {
            final URI fromURI2 = ts.tableHandle.getDataLocation();
            final Path toDataPath = new Path(toURI.toString(), "data");
            final Task<? extends Serializable> rTask = TaskFactory.get((Serializable)new CopyWork(fromURI2.toString(), toDataPath.toString(), false), this.conf, (Task<? extends Serializable>[])new Task[0]);
            this.rootTasks.add(rTask);
            this.inputs.add(new ReadEntity(ts.tableHandle));
        }
        this.outputs.add(new WriteEntity(toURI.toString(), toURI.getScheme().equals("hdfs")));
    }
}
