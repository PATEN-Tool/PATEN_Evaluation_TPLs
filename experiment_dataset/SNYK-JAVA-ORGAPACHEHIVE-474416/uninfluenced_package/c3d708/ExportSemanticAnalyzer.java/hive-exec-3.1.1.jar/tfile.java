// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.hadoop.hive.ql.parse;

import org.antlr.runtime.tree.Tree;
import org.apache.hadoop.hive.ql.exec.TaskFactory;
import org.apache.hadoop.hive.ql.exec.Utilities;
import java.util.Collection;
import org.apache.hadoop.hive.ql.parse.repl.dump.TableExport;
import org.apache.hadoop.hive.ql.ErrorMsg;
import org.apache.hadoop.hive.ql.metadata.Table;
import org.apache.hadoop.hive.ql.metadata.InvalidTableException;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.ql.metadata.Hive;
import javax.annotation.Nullable;
import org.apache.hadoop.hive.ql.exec.Task;
import org.apache.hadoop.hive.ql.plan.ExportWork;
import org.apache.hadoop.hive.ql.hooks.WriteEntity;
import org.apache.hadoop.hive.ql.hooks.ReadEntity;
import java.util.Set;
import org.apache.hadoop.hive.ql.QueryState;

public class ExportSemanticAnalyzer extends BaseSemanticAnalyzer
{
    private boolean isMmExport;
    
    ExportSemanticAnalyzer(final QueryState queryState) throws SemanticException {
        super(queryState);
        this.isMmExport = false;
    }
    
    @Override
    public void analyzeInternal(final ASTNode ast) throws SemanticException {
        final Task<ExportWork> task = analyzeExport(ast, null, this.db, this.conf, this.inputs, this.outputs);
        this.isMmExport = (task.getWork().getMmContext() != null);
        this.rootTasks.add(task);
    }
    
    static Task<ExportWork> analyzeExport(final ASTNode ast, @Nullable final String acidTableName, final Hive db, final HiveConf conf, final Set<ReadEntity> inputs, final Set<WriteEntity> outputs) throws SemanticException {
        final Tree tableTree = ast.getChild(0);
        final Tree toTree = ast.getChild(1);
        ReplicationSpec replicationSpec;
        if (ast.getChildCount() > 2) {
            replicationSpec = new ReplicationSpec((ASTNode)ast.getChild(2));
        }
        else {
            replicationSpec = new ReplicationSpec();
        }
        if (replicationSpec.getCurrentReplicationState() == null) {
            try {
                final long currentEventId = db.getMSC().getCurrentNotificationEventId().getEventId();
                replicationSpec.setCurrentReplicationState(String.valueOf(currentEventId));
            }
            catch (Exception e) {
                throw new SemanticException("Error when getting current notification event ID", e);
            }
        }
        TableSpec ts;
        try {
            ts = new TableSpec(db, conf, (ASTNode)tableTree, false, true);
        }
        catch (SemanticException sme) {
            if (!replicationSpec.isInReplicationScope()) {
                throw sme;
            }
            if (!(sme.getCause() instanceof InvalidTableException) && !(sme instanceof Table.ValidationFailureSemanticException)) {
                throw sme;
            }
            ts = null;
        }
        final String tmpPath = BaseSemanticAnalyzer.stripQuotes(toTree.getText());
        final TableExport.Paths exportPaths = new TableExport.Paths(ErrorMsg.INVALID_PATH.getMsg(ast), tmpPath, conf, false);
        final TableExport.AuthEntities authEntities = new TableExport(exportPaths, ts, replicationSpec, db, null, conf, null).getAuthEntities();
        inputs.addAll(authEntities.inputs);
        outputs.addAll(authEntities.outputs);
        final String exportRootDirName = tmpPath;
        final ExportWork.MmContext mmCtx = ExportWork.MmContext.createIfNeeded((ts == null) ? null : ts.tableHandle);
        Utilities.FILE_OP_LOGGER.debug("Exporting table {}: MM context {}", (Object)((ts == null) ? null : ts.tableName), (Object)mmCtx);
        final ExportWork exportWork = new ExportWork(exportRootDirName, ts, replicationSpec, ErrorMsg.INVALID_PATH.getMsg(ast), acidTableName, mmCtx);
        return TaskFactory.get(exportWork);
    }
    
    @Override
    public boolean hasTransactionalInQuery() {
        return this.isMmExport;
    }
}
