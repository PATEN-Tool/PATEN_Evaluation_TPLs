// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.hadoop.hive.ql.parse;

import org.apache.hadoop.hive.ql.metadata.InvalidTableException;
import org.apache.hadoop.hive.ql.metadata.Hive;
import org.apache.hadoop.hive.metastore.MetaStoreUtils;
import org.apache.hadoop.hive.ql.hooks.WriteEntity;
import org.apache.hadoop.mapred.OutputFormat;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import java.util.Collections;
import org.apache.commons.lang.ObjectUtils;
import org.apache.hadoop.hive.ql.io.HiveFileFormatUtils;
import org.apache.hadoop.hive.metastore.TableType;
import org.apache.hadoop.hive.metastore.api.Order;
import java.util.Comparator;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.hive.common.FileUtils;
import org.apache.hadoop.hive.metastore.api.Database;
import org.apache.hadoop.hive.ql.plan.DropTableDesc;
import org.apache.hadoop.hive.ql.plan.DDLWork;
import java.io.Serializable;
import org.apache.hadoop.hive.ql.exec.TaskFactory;
import org.apache.hadoop.hive.ql.plan.LoadFileDesc;
import org.apache.hadoop.hive.ql.plan.MoveWork;
import org.apache.hadoop.hive.ql.plan.LoadTableDesc;
import java.util.TreeMap;
import org.apache.hadoop.hive.ql.exec.Utilities;
import org.apache.hadoop.hive.ql.exec.ReplCopyTask;
import org.apache.hadoop.hive.ql.exec.Task;
import org.apache.hadoop.hive.metastore.api.SQLForeignKey;
import org.apache.hadoop.hive.metastore.api.SQLPrimaryKey;
import java.net.URISyntaxException;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.metastore.api.MetaException;
import org.apache.hadoop.hive.ql.metadata.Table;
import java.util.Iterator;
import java.util.List;
import org.apache.hadoop.hive.ql.plan.CreateTableDesc;
import java.net.URI;
import org.apache.hadoop.hive.metastore.Warehouse;
import org.apache.hadoop.hive.metastore.api.Partition;
import org.apache.hadoop.hive.ql.plan.AddPartitionDesc;
import java.util.ArrayList;
import org.apache.hadoop.hive.ql.session.SessionState;
import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.antlr.runtime.tree.Tree;
import org.apache.hadoop.hive.ql.ErrorMsg;
import java.util.Map;
import java.util.LinkedHashMap;
import org.apache.hadoop.hive.ql.QueryState;

public class ImportSemanticAnalyzer extends BaseSemanticAnalyzer
{
    private boolean tableExists;
    
    public ImportSemanticAnalyzer(final QueryState queryState) throws SemanticException {
        super(queryState);
        this.tableExists = false;
    }
    
    public boolean existsTable() {
        return this.tableExists;
    }
    
    @Override
    public void analyzeInternal(final ASTNode ast) throws SemanticException {
        try {
            final Tree fromTree = ast.getChild(0);
            boolean isLocationSet = false;
            boolean isExternalSet = false;
            boolean isPartSpecSet = false;
            String parsedLocation = null;
            String parsedTableName = null;
            String parsedDbName = null;
            final LinkedHashMap<String, String> parsedPartSpec = new LinkedHashMap<String, String>();
            final boolean waitOnPrecursor = false;
            for (int i = 1; i < ast.getChildCount(); ++i) {
                final ASTNode child = (ASTNode)ast.getChild(i);
                switch (child.getToken().getType()) {
                    case 115: {
                        isExternalSet = true;
                        break;
                    }
                    case 976: {
                        isLocationSet = true;
                        parsedLocation = EximUtil.relativeToAbsolutePath(this.conf, BaseSemanticAnalyzer.unescapeSQLString(child.getChild(0).getText()));
                        break;
                    }
                    case 965: {
                        final ASTNode tableNameNode = (ASTNode)child.getChild(0);
                        final Map.Entry<String, String> dbTablePair = BaseSemanticAnalyzer.getDbTableNamePair(tableNameNode);
                        parsedDbName = dbTablePair.getKey();
                        parsedTableName = dbTablePair.getValue();
                        if (child.getChildCount() == 2) {
                            final ASTNode partspec = (ASTNode)child.getChild(1);
                            isPartSpecSet = true;
                            this.parsePartitionSpec(child, parsedPartSpec);
                            break;
                        }
                        break;
                    }
                }
            }
            this.tableExists = prepareImport(isLocationSet, isExternalSet, isPartSpecSet, waitOnPrecursor, parsedLocation, parsedTableName, parsedDbName, parsedPartSpec, fromTree.getText(), new EximUtil.SemanticAnalyzerWrapperContext(this.conf, this.db, this.inputs, this.outputs, this.rootTasks, this.LOG, this.ctx), null, null);
        }
        catch (SemanticException e) {
            throw e;
        }
        catch (Exception e2) {
            throw new SemanticException(ErrorMsg.IMPORT_SEMANTIC_ERROR.getMsg(), e2);
        }
    }
    
    private void parsePartitionSpec(final ASTNode tableNode, final LinkedHashMap<String, String> partSpec) throws SemanticException {
        if (tableNode.getChildCount() == 2) {
            final ASTNode partspec = (ASTNode)tableNode.getChild(1);
            for (int j = 0; j < partspec.getChildCount(); ++j) {
                final ASTNode partspec_val = (ASTNode)partspec.getChild(j);
                String val = null;
                final String colName = BaseSemanticAnalyzer.unescapeIdentifier(partspec_val.getChild(0).getText().toLowerCase());
                if (partspec_val.getChildCount() < 2) {
                    throw new SemanticException(ErrorMsg.INVALID_PARTITION.getMsg(" - Dynamic partitions not allowed"));
                }
                val = BaseSemanticAnalyzer.stripQuotes(partspec_val.getChild(1).getText());
                partSpec.put(colName, val);
            }
        }
    }
    
    public static boolean prepareImport(final boolean isLocationSet, final boolean isExternalSet, final boolean isPartSpecSet, final boolean waitOnPrecursor, final String parsedLocation, final String parsedTableName, final String parsedDbName, final LinkedHashMap<String, String> parsedPartSpec, final String fromLocn, final EximUtil.SemanticAnalyzerWrapperContext x, final Map<String, Long> dbsUpdated, final Map<String, Long> tablesUpdated) throws IOException, MetaException, HiveException, URISyntaxException {
        final URI fromURI = EximUtil.getValidatedURI(x.getConf(), BaseSemanticAnalyzer.stripQuotes(fromLocn));
        final Path fromPath = new Path(fromURI.getScheme(), fromURI.getAuthority(), fromURI.getPath());
        final FileSystem fs = FileSystem.get(fromURI, (Configuration)x.getConf());
        x.getInputs().add(BaseSemanticAnalyzer.toReadEntity(fromPath, x.getConf()));
        EximUtil.ReadMetaData rv = new EximUtil.ReadMetaData();
        try {
            rv = EximUtil.readMetaData(fs, new Path(fromPath, "_metadata"));
        }
        catch (IOException e) {
            throw new SemanticException(ErrorMsg.INVALID_PATH.getMsg(), e);
        }
        final ReplicationSpec replicationSpec = rv.getReplicationSpec();
        if (replicationSpec.isNoop()) {
            return false;
        }
        String dbname = SessionState.get().getCurrentDatabase();
        if (parsedDbName != null && !parsedDbName.isEmpty()) {
            dbname = parsedDbName;
        }
        if (dbsUpdated != null) {
            dbsUpdated.put(dbname, Long.valueOf(replicationSpec.get(ReplicationSpec.KEY.EVENT_ID)));
        }
        final CreateTableDesc tblDesc = getBaseCreateTableDescFromTable(dbname, rv.getTable());
        if (replicationSpec != null && replicationSpec.isInReplicationScope()) {
            tblDesc.setReplicationSpec(replicationSpec);
        }
        if (isExternalSet) {
            tblDesc.setExternal(isExternalSet);
        }
        if (isLocationSet) {
            tblDesc.setLocation(parsedLocation);
            x.getInputs().add(BaseSemanticAnalyzer.toReadEntity(new Path(parsedLocation), x.getConf()));
        }
        if (parsedTableName != null && !parsedTableName.isEmpty()) {
            tblDesc.setTableName(parsedTableName);
        }
        if (tablesUpdated != null) {
            tablesUpdated.put(dbname + "." + tblDesc.getTableName(), Long.valueOf(replicationSpec.get(ReplicationSpec.KEY.EVENT_ID)));
        }
        final List<AddPartitionDesc> partitionDescs = new ArrayList<AddPartitionDesc>();
        final Iterable<Partition> partitions = rv.getPartitions();
        for (final Partition partition : partitions) {
            final AddPartitionDesc partsDesc = getBaseAddPartitionDescFromPartition(fromPath, dbname, tblDesc, partition);
            partitionDescs.add(partsDesc);
        }
        if (isPartSpecSet) {
            boolean found = false;
            final Iterator<AddPartitionDesc> partnIter = partitionDescs.listIterator();
            while (partnIter.hasNext()) {
                final AddPartitionDesc addPartitionDesc = partnIter.next();
                if (!found && addPartitionDesc.getPartition(0).getPartSpec().equals(parsedPartSpec)) {
                    found = true;
                }
                else {
                    partnIter.remove();
                }
            }
            if (!found) {
                throw new SemanticException(ErrorMsg.INVALID_PARTITION.getMsg(" - Specified partition not found in import directory"));
            }
        }
        if (tblDesc.getTableName() == null) {
            throw new SemanticException(ErrorMsg.NEED_TABLE_SPECIFICATION.getMsg());
        }
        x.getConf().set("import.destination.table", tblDesc.getTableName());
        for (final AddPartitionDesc addPartitionDesc2 : partitionDescs) {
            addPartitionDesc2.setTableName(tblDesc.getTableName());
        }
        final Warehouse wh = new Warehouse(x.getConf());
        final Table table = tableIfExists(tblDesc, x.getHive());
        boolean tableExists = false;
        if (table != null) {
            checkTable(table, tblDesc, replicationSpec, x.getConf());
            x.getLOG().debug("table " + tblDesc.getTableName() + " exists: metadata checked");
            tableExists = true;
        }
        if (!replicationSpec.isInReplicationScope()) {
            createRegularImportTasks(tblDesc, partitionDescs, isPartSpecSet, replicationSpec, table, fromURI, fs, wh, x);
        }
        else {
            createReplImportTasks(tblDesc, partitionDescs, isPartSpecSet, replicationSpec, waitOnPrecursor, table, fromURI, fs, wh, x);
        }
        return tableExists;
    }
    
    private static AddPartitionDesc getBaseAddPartitionDescFromPartition(final Path fromPath, final String dbname, final CreateTableDesc tblDesc, final Partition partition) throws MetaException {
        final AddPartitionDesc partsDesc = new AddPartitionDesc(dbname, tblDesc.getTableName(), EximUtil.makePartSpec(tblDesc.getPartCols(), partition.getValues()), partition.getSd().getLocation(), partition.getParameters());
        final AddPartitionDesc.OnePartitionDesc partDesc = partsDesc.getPartition(0);
        partDesc.setInputFormat(partition.getSd().getInputFormat());
        partDesc.setOutputFormat(partition.getSd().getOutputFormat());
        partDesc.setNumBuckets(partition.getSd().getNumBuckets());
        partDesc.setCols(partition.getSd().getCols());
        partDesc.setSerializationLib(partition.getSd().getSerdeInfo().getSerializationLib());
        partDesc.setSerdeParams(partition.getSd().getSerdeInfo().getParameters());
        partDesc.setBucketCols(partition.getSd().getBucketCols());
        partDesc.setSortCols(partition.getSd().getSortCols());
        partDesc.setLocation(new Path(fromPath, Warehouse.makePartName(tblDesc.getPartCols(), partition.getValues())).toString());
        return partsDesc;
    }
    
    private static CreateTableDesc getBaseCreateTableDescFromTable(final String dbName, final org.apache.hadoop.hive.metastore.api.Table table) {
        if (table.getPartitionKeys() == null || table.getPartitionKeys().size() == 0) {
            table.putToParameters("DO_NOT_UPDATE_STATS", "true");
        }
        final CreateTableDesc tblDesc = new CreateTableDesc(dbName, table.getTableName(), false, table.isTemporary(), table.getSd().getCols(), table.getPartitionKeys(), table.getSd().getBucketCols(), table.getSd().getSortCols(), table.getSd().getNumBuckets(), null, null, null, null, null, null, table.getSd().getInputFormat(), table.getSd().getOutputFormat(), null, table.getSd().getSerdeInfo().getSerializationLib(), null, table.getSd().getSerdeInfo().getParameters(), table.getParameters(), false, (null == table.getSd().getSkewedInfo()) ? null : table.getSd().getSkewedInfo().getSkewedColNames(), (null == table.getSd().getSkewedInfo()) ? null : table.getSd().getSkewedInfo().getSkewedColValues(), null, null);
        tblDesc.setStoredAsSubDirectories(table.getSd().isStoredAsSubDirectories());
        return tblDesc;
    }
    
    private static Task<?> loadTable(final URI fromURI, final Table table, final boolean replace, final Path tgtPath, final ReplicationSpec replicationSpec, final EximUtil.SemanticAnalyzerWrapperContext x) {
        final Path dataPath = new Path(fromURI.toString(), "data");
        final Path tmpPath = x.getCtx().getExternalTmpPath(tgtPath);
        final Task<?> copyTask = ReplCopyTask.getLoadCopyTask(replicationSpec, dataPath, tmpPath, x.getConf());
        final LoadTableDesc loadTableWork = new LoadTableDesc(tmpPath, Utilities.getTableDesc(table), new TreeMap<String, String>(), replace);
        final Task<?> loadTableTask = TaskFactory.get((Object)new MoveWork(x.getInputs(), x.getOutputs(), loadTableWork, null, false), x.getConf(), (Task<? extends Serializable>[])new Task[0]);
        copyTask.addDependentTask((Task<? extends Serializable>)loadTableTask);
        x.getTasks().add((Task<? extends Serializable>)copyTask);
        return loadTableTask;
    }
    
    private static Task<?> createTableTask(final CreateTableDesc tableDesc, final EximUtil.SemanticAnalyzerWrapperContext x) {
        return TaskFactory.get((Object)new DDLWork(x.getInputs(), x.getOutputs(), tableDesc), x.getConf(), (Task<? extends Serializable>[])new Task[0]);
    }
    
    private static Task<?> dropTableTask(final Table table, final EximUtil.SemanticAnalyzerWrapperContext x) {
        return TaskFactory.get((Object)new DDLWork(x.getInputs(), x.getOutputs(), new DropTableDesc(table.getTableName(), false, true, true, null)), x.getConf(), (Task<? extends Serializable>[])new Task[0]);
    }
    
    private static Task<? extends Serializable> alterTableTask(final CreateTableDesc tableDesc, final EximUtil.SemanticAnalyzerWrapperContext x, final ReplicationSpec replicationSpec) {
        tableDesc.setReplaceMode(true);
        if (replicationSpec != null && replicationSpec.isInReplicationScope()) {
            tableDesc.setReplicationSpec(replicationSpec);
        }
        return TaskFactory.get((Serializable)new DDLWork(x.getInputs(), x.getOutputs(), tableDesc), x.getConf(), (Task<? extends Serializable>[])new Task[0]);
    }
    
    private static Task<? extends Serializable> alterSinglePartition(final URI fromURI, final FileSystem fs, final CreateTableDesc tblDesc, final Table table, final Warehouse wh, final AddPartitionDesc addPartitionDesc, final ReplicationSpec replicationSpec, final org.apache.hadoop.hive.ql.metadata.Partition ptn, final EximUtil.SemanticAnalyzerWrapperContext x) {
        addPartitionDesc.setReplaceMode(true);
        if (replicationSpec != null && replicationSpec.isInReplicationScope()) {
            addPartitionDesc.setReplicationSpec(replicationSpec);
        }
        addPartitionDesc.getPartition(0).setLocation(ptn.getLocation());
        return TaskFactory.get((Serializable)new DDLWork(x.getInputs(), x.getOutputs(), addPartitionDesc), x.getConf(), (Task<? extends Serializable>[])new Task[0]);
    }
    
    private static Task<?> addSinglePartition(final URI fromURI, final FileSystem fs, final CreateTableDesc tblDesc, final Table table, final Warehouse wh, final AddPartitionDesc addPartitionDesc, final ReplicationSpec replicationSpec, final EximUtil.SemanticAnalyzerWrapperContext x) throws MetaException, IOException, HiveException {
        final AddPartitionDesc.OnePartitionDesc partSpec = addPartitionDesc.getPartition(0);
        if (tblDesc.isExternal() && tblDesc.getLocation() == null) {
            x.getLOG().debug("Importing in-place: adding AddPart for partition " + partSpecToString(partSpec.getPartSpec()));
            final Task<?> addPartTask = TaskFactory.get((Object)new DDLWork(x.getInputs(), x.getOutputs(), addPartitionDesc), x.getConf(), (Task<? extends Serializable>[])new Task[0]);
            return addPartTask;
        }
        final String srcLocation = partSpec.getLocation();
        fixLocationInPartSpec(fs, tblDesc, table, wh, replicationSpec, partSpec, x);
        x.getLOG().debug("adding dependent CopyWork/AddPart/MoveWork for partition " + partSpecToString(partSpec.getPartSpec()) + " with source location: " + srcLocation);
        final Path tgtLocation = new Path(partSpec.getLocation());
        final Path tmpPath = x.getCtx().getExternalTmpPath(tgtLocation);
        final Task<?> copyTask = ReplCopyTask.getLoadCopyTask(replicationSpec, new Path(srcLocation), tmpPath, x.getConf());
        final Task<?> addPartTask2 = TaskFactory.get((Object)new DDLWork(x.getInputs(), x.getOutputs(), addPartitionDesc), x.getConf(), (Task<? extends Serializable>[])new Task[0]);
        final LoadTableDesc loadTableWork = new LoadTableDesc(tmpPath, Utilities.getTableDesc(table), partSpec.getPartSpec(), true);
        loadTableWork.setInheritTableSpecs(false);
        final Task<?> loadPartTask = TaskFactory.get((Object)new MoveWork(x.getInputs(), x.getOutputs(), loadTableWork, null, false), x.getConf(), (Task<? extends Serializable>[])new Task[0]);
        copyTask.addDependentTask((Task<? extends Serializable>)loadPartTask);
        addPartTask2.addDependentTask((Task<? extends Serializable>)loadPartTask);
        x.getTasks().add((Task<? extends Serializable>)copyTask);
        return addPartTask2;
    }
    
    private static void fixLocationInPartSpec(final FileSystem fs, final CreateTableDesc tblDesc, final Table table, final Warehouse wh, final ReplicationSpec replicationSpec, final AddPartitionDesc.OnePartitionDesc partSpec, final EximUtil.SemanticAnalyzerWrapperContext x) throws MetaException, HiveException, IOException {
        Path tgtPath = null;
        if (tblDesc.getLocation() == null) {
            if (table.getDataLocation() != null) {
                tgtPath = new Path(table.getDataLocation().toString(), Warehouse.makePartPath(partSpec.getPartSpec()));
            }
            else {
                final Database parentDb = x.getHive().getDatabase(tblDesc.getDatabaseName());
                tgtPath = new Path(wh.getTablePath(parentDb, tblDesc.getTableName()), Warehouse.makePartPath(partSpec.getPartSpec()));
            }
        }
        else {
            tgtPath = new Path(tblDesc.getLocation(), Warehouse.makePartPath(partSpec.getPartSpec()));
        }
        final FileSystem tgtFs = FileSystem.get(tgtPath.toUri(), (Configuration)x.getConf());
        checkTargetLocationEmpty(tgtFs, tgtPath, replicationSpec, x);
        partSpec.setLocation(tgtPath.toString());
    }
    
    private static void checkTargetLocationEmpty(final FileSystem fs, final Path targetPath, final ReplicationSpec replicationSpec, final EximUtil.SemanticAnalyzerWrapperContext x) throws IOException, SemanticException {
        if (replicationSpec.isInReplicationScope()) {
            return;
        }
        x.getLOG().debug("checking emptiness of " + targetPath.toString());
        if (fs.exists(targetPath)) {
            final FileStatus[] status = fs.listStatus(targetPath, FileUtils.HIDDEN_FILES_PATH_FILTER);
            if (status.length > 0) {
                x.getLOG().debug("Files inc. " + status[0].getPath().toString() + " found in path : " + targetPath.toString());
                throw new SemanticException(ErrorMsg.TABLE_DATA_EXISTS.getMsg());
            }
        }
    }
    
    private static String partSpecToString(final Map<String, String> partSpec) {
        final StringBuilder sb = new StringBuilder();
        boolean firstTime = true;
        for (final Map.Entry<String, String> entry : partSpec.entrySet()) {
            if (!firstTime) {
                sb.append(',');
            }
            firstTime = false;
            sb.append(entry.getKey());
            sb.append('=');
            sb.append(entry.getValue());
        }
        return sb.toString();
    }
    
    private static void checkTable(final Table table, final CreateTableDesc tableDesc, final ReplicationSpec replicationSpec, final HiveConf conf) throws SemanticException, URISyntaxException {
        if (replicationSpec.isInReplicationScope()) {
            return;
        }
        if (table.getParameters().containsKey(ReplicationSpec.KEY.CURR_STATE_ID.toString()) && conf.getBoolVar(HiveConf.ConfVars.HIVE_EXIM_RESTRICT_IMPORTS_INTO_REPLICATED_TABLES)) {
            throw new SemanticException(ErrorMsg.IMPORT_INTO_STRICT_REPL_TABLE.getMsg("Table " + table.getTableName() + " has repl.last.id parameter set."));
        }
        EximUtil.validateTable(table);
        if (tableDesc.isExternal() && (!table.isPartitioned() || !table.getTableType().equals(TableType.EXTERNAL_TABLE))) {
            throw new SemanticException(ErrorMsg.INCOMPATIBLE_SCHEMA.getMsg(" External table cannot overwrite existing table. Drop existing table first."));
        }
        if (tableDesc.getLocation() != null && !table.isPartitioned() && !table.getDataLocation().equals((Object)new Path(tableDesc.getLocation()))) {
            throw new SemanticException(ErrorMsg.INCOMPATIBLE_SCHEMA.getMsg(" Location does not match"));
        }
        final List<FieldSchema> existingTableCols = table.getCols();
        final List<FieldSchema> importedTableCols = tableDesc.getCols();
        if (!EximUtil.schemaCompare(importedTableCols, existingTableCols)) {
            throw new SemanticException(ErrorMsg.INCOMPATIBLE_SCHEMA.getMsg(" Column Schema does not match"));
        }
        final List<FieldSchema> existingTablePartCols = table.getPartCols();
        final List<FieldSchema> importedTablePartCols = tableDesc.getPartCols();
        if (!EximUtil.schemaCompare(importedTablePartCols, existingTablePartCols)) {
            throw new SemanticException(ErrorMsg.INCOMPATIBLE_SCHEMA.getMsg(" Partition Schema does not match"));
        }
        final Map<String, String> existingTableParams = table.getParameters();
        final Map<String, String> importedTableParams = tableDesc.getTblProps();
        final String error = checkParams(existingTableParams, importedTableParams, new String[] { "howl.isd", "howl.osd" });
        if (error != null) {
            throw new SemanticException(ErrorMsg.INCOMPATIBLE_SCHEMA.getMsg(" Table parameters do not match: " + error));
        }
        final String existingifc = table.getInputFormatClass().getName();
        final String importedifc = tableDesc.getInputFormat();
        final String existingofc = table.getOutputFormatClass().getName();
        String importedofc = tableDesc.getOutputFormat();
        try {
            final Class<?> origin = Class.forName(importedofc, true, Utilities.getSessionSpecifiedClassLoader());
            final Class<? extends OutputFormat> replaced = HiveFileFormatUtils.getOutputFormatSubstitute(origin);
            if (replaced == null) {
                throw new SemanticException(ErrorMsg.INVALID_OUTPUT_FORMAT_TYPE.getMsg());
            }
            importedofc = replaced.getCanonicalName();
        }
        catch (Exception e) {
            throw new SemanticException(ErrorMsg.INVALID_OUTPUT_FORMAT_TYPE.getMsg());
        }
        if (!existingifc.equals(importedifc) || !existingofc.equals(importedofc)) {
            throw new SemanticException(ErrorMsg.INCOMPATIBLE_SCHEMA.getMsg(" Table inputformat/outputformats do not match"));
        }
        final String existingSerde = table.getSerializationLib();
        final String importedSerde = tableDesc.getSerName();
        if (!existingSerde.equals(importedSerde)) {
            throw new SemanticException(ErrorMsg.INCOMPATIBLE_SCHEMA.getMsg(" Table Serde class does not match"));
        }
        final String existingSerdeFormat = table.getSerdeParam("serialization.format");
        String importedSerdeFormat = tableDesc.getSerdeProps().get("serialization.format");
        importedSerdeFormat = ((importedSerdeFormat == null) ? "1" : importedSerdeFormat);
        if (!ObjectUtils.equals(existingSerdeFormat, importedSerdeFormat)) {
            throw new SemanticException(ErrorMsg.INCOMPATIBLE_SCHEMA.getMsg(" Table Serde format does not match"));
        }
        if (!ObjectUtils.equals(table.getBucketCols(), tableDesc.getBucketCols())) {
            throw new SemanticException(ErrorMsg.INCOMPATIBLE_SCHEMA.getMsg(" Table bucketing spec does not match"));
        }
        final List<Order> existingOrder = table.getSortCols();
        final List<Order> importedOrder = tableDesc.getSortCols();
        if (existingOrder != null) {
            if (importedOrder != null) {
                final class OrderComparator implements Comparator<Order>
                {
                    @Override
                    public int compare(final Order o1, final Order o2) {
                        if (o1.getOrder() < o2.getOrder()) {
                            return -1;
                        }
                        if (o1.getOrder() == o2.getOrder()) {
                            return 0;
                        }
                        return 1;
                    }
                }
                Collections.sort(existingOrder, new OrderComparator());
                Collections.sort(importedOrder, new OrderComparator());
                if (!existingOrder.equals(importedOrder)) {
                    throw new SemanticException(ErrorMsg.INCOMPATIBLE_SCHEMA.getMsg(" Table sorting spec does not match"));
                }
            }
        }
        else if (importedOrder != null) {
            throw new SemanticException(ErrorMsg.INCOMPATIBLE_SCHEMA.getMsg(" Table sorting spec does not match"));
        }
    }
    
    private static String checkParams(final Map<String, String> map1, final Map<String, String> map2, final String[] keys) {
        if (map1 != null) {
            if (map2 != null) {
                for (final String key : keys) {
                    final String v1 = map1.get(key);
                    final String v2 = map2.get(key);
                    if (!ObjectUtils.equals(v1, v2)) {
                        return "Mismatch for " + key;
                    }
                }
            }
            else {
                for (final String key : keys) {
                    if (map1.get(key) != null) {
                        return "Mismatch for " + key;
                    }
                }
            }
        }
        else if (map2 != null) {
            for (final String key : keys) {
                if (map2.get(key) != null) {
                    return "Mismatch for " + key;
                }
            }
        }
        return null;
    }
    
    private static void createRegularImportTasks(final CreateTableDesc tblDesc, final List<AddPartitionDesc> partitionDescs, final boolean isPartSpecSet, final ReplicationSpec replicationSpec, Table table, final URI fromURI, final FileSystem fs, final Warehouse wh, final EximUtil.SemanticAnalyzerWrapperContext x) throws HiveException, URISyntaxException, IOException, MetaException {
        if (table != null) {
            if (table.isPartitioned()) {
                x.getLOG().debug("table partitioned");
                for (final AddPartitionDesc addPartitionDesc : partitionDescs) {
                    final Map<String, String> partSpec = addPartitionDesc.getPartition(0).getPartSpec();
                    org.apache.hadoop.hive.ql.metadata.Partition ptn = null;
                    if ((ptn = x.getHive().getPartition(table, partSpec, false)) != null) {
                        throw new SemanticException(ErrorMsg.PARTITION_EXISTS.getMsg(partSpecToString(partSpec)));
                    }
                    x.getTasks().add((Task<? extends Serializable>)addSinglePartition(fromURI, fs, tblDesc, table, wh, addPartitionDesc, replicationSpec, x));
                }
            }
            else {
                x.getLOG().debug("table non-partitioned");
                final Path tgtPath = new Path(table.getDataLocation().toString());
                final FileSystem tgtFs = FileSystem.get(tgtPath.toUri(), (Configuration)x.getConf());
                checkTargetLocationEmpty(tgtFs, tgtPath, replicationSpec, x);
                loadTable(fromURI, table, false, tgtPath, replicationSpec, x);
            }
            x.getOutputs().add(new WriteEntity(table, WriteEntity.WriteType.DDL_NO_LOCK));
        }
        else {
            x.getLOG().debug("table " + tblDesc.getTableName() + " does not exist");
            final Task<?> t = TaskFactory.get((Object)new DDLWork(x.getInputs(), x.getOutputs(), tblDesc), x.getConf(), (Task<? extends Serializable>[])new Task[0]);
            table = new Table(tblDesc.getDatabaseName(), tblDesc.getTableName());
            final Database parentDb = x.getHive().getDatabase(tblDesc.getDatabaseName());
            x.getOutputs().add(new WriteEntity(parentDb, WriteEntity.WriteType.DDL_SHARED));
            if (isPartitioned(tblDesc)) {
                for (final AddPartitionDesc addPartitionDesc2 : partitionDescs) {
                    t.addDependentTask((Task<? extends Serializable>)addSinglePartition(fromURI, fs, tblDesc, table, wh, addPartitionDesc2, replicationSpec, x));
                }
            }
            else {
                x.getLOG().debug("adding dependent CopyWork/MoveWork for table");
                if (tblDesc.isExternal() && tblDesc.getLocation() == null) {
                    x.getLOG().debug("Importing in place, no emptiness check, no copying/loading");
                    final Path dataPath = new Path(fromURI.toString(), "data");
                    tblDesc.setLocation(dataPath.toString());
                }
                else {
                    Path tablePath = null;
                    if (tblDesc.getLocation() != null) {
                        tablePath = new Path(tblDesc.getLocation());
                    }
                    else {
                        tablePath = wh.getTablePath(parentDb, tblDesc.getTableName());
                    }
                    final FileSystem tgtFs2 = FileSystem.get(tablePath.toUri(), (Configuration)x.getConf());
                    checkTargetLocationEmpty(tgtFs2, tablePath, replicationSpec, x);
                    t.addDependentTask((Task<? extends Serializable>)loadTable(fromURI, table, false, tablePath, replicationSpec, x));
                }
            }
            x.getTasks().add((Task<? extends Serializable>)t);
        }
    }
    
    private static void createReplImportTasks(final CreateTableDesc tblDesc, final List<AddPartitionDesc> partitionDescs, final boolean isPartSpecSet, final ReplicationSpec replicationSpec, final boolean waitOnPrecursor, Table table, final URI fromURI, final FileSystem fs, final Warehouse wh, final EximUtil.SemanticAnalyzerWrapperContext x) throws HiveException, URISyntaxException, IOException, MetaException {
        Task dr = null;
        WriteEntity.WriteType lockType = WriteEntity.WriteType.DDL_NO_LOCK;
        if (table != null && isPartitioned(tblDesc) != table.isPartitioned()) {
            if (!replicationSpec.allowReplacementInto(table)) {
                return;
            }
            dr = dropTableTask(table, x);
            lockType = WriteEntity.WriteType.DDL_EXCLUSIVE;
            table = null;
        }
        final Database parentDb = x.getHive().getDatabase(tblDesc.getDatabaseName());
        if (parentDb == null && !waitOnPrecursor) {
            throw new SemanticException(ErrorMsg.DATABASE_NOT_EXISTS.getMsg(tblDesc.getDatabaseName()));
        }
        if (tblDesc.getLocation() == null) {
            if (!waitOnPrecursor) {
                tblDesc.setLocation(wh.getTablePath(parentDb, tblDesc.getTableName()).toString());
            }
            else {
                tblDesc.setLocation(wh.getDnsPath(new Path(wh.getDefaultDatabasePath(tblDesc.getDatabaseName()), MetaStoreUtils.encodeTableName(tblDesc.getTableName().toLowerCase()))).toString());
            }
        }
        if (table == null) {
            if (lockType == WriteEntity.WriteType.DDL_NO_LOCK) {
                lockType = WriteEntity.WriteType.DDL_SHARED;
            }
            final Task t = createTableTask(tblDesc, x);
            table = new Table(tblDesc.getDatabaseName(), tblDesc.getTableName());
            if (!replicationSpec.isMetadataOnly()) {
                if (isPartitioned(tblDesc)) {
                    for (final AddPartitionDesc addPartitionDesc : partitionDescs) {
                        addPartitionDesc.setReplicationSpec(replicationSpec);
                        t.addDependentTask(addSinglePartition(fromURI, fs, tblDesc, table, wh, addPartitionDesc, replicationSpec, x));
                    }
                }
                else {
                    x.getLOG().debug("adding dependent CopyWork/MoveWork for table");
                    t.addDependentTask(loadTable(fromURI, table, true, new Path(tblDesc.getLocation()), replicationSpec, x));
                }
            }
            if (dr == null) {
                x.getTasks().add(t);
            }
            else {
                dr.addDependentTask(t);
                x.getTasks().add(dr);
            }
        }
        else if (table.isPartitioned()) {
            x.getLOG().debug("table partitioned");
            for (final AddPartitionDesc addPartitionDesc2 : partitionDescs) {
                addPartitionDesc2.setReplicationSpec(replicationSpec);
                final Map<String, String> partSpec = addPartitionDesc2.getPartition(0).getPartSpec();
                org.apache.hadoop.hive.ql.metadata.Partition ptn = null;
                if ((ptn = x.getHive().getPartition(table, partSpec, false)) == null) {
                    if (replicationSpec.isMetadataOnly()) {
                        continue;
                    }
                    x.getTasks().add((Task<? extends Serializable>)addSinglePartition(fromURI, fs, tblDesc, table, wh, addPartitionDesc2, replicationSpec, x));
                }
                else {
                    if (!replicationSpec.allowReplacementInto(ptn)) {
                        continue;
                    }
                    if (!replicationSpec.isMetadataOnly()) {
                        x.getTasks().add((Task<? extends Serializable>)addSinglePartition(fromURI, fs, tblDesc, table, wh, addPartitionDesc2, replicationSpec, x));
                    }
                    else {
                        x.getTasks().add(alterSinglePartition(fromURI, fs, tblDesc, table, wh, addPartitionDesc2, replicationSpec, ptn, x));
                    }
                    if (lockType != WriteEntity.WriteType.DDL_NO_LOCK) {
                        continue;
                    }
                    lockType = WriteEntity.WriteType.DDL_SHARED;
                }
            }
            if (replicationSpec.isMetadataOnly() && partitionDescs.isEmpty()) {
                x.getTasks().add(alterTableTask(tblDesc, x, replicationSpec));
                if (lockType == WriteEntity.WriteType.DDL_NO_LOCK) {
                    lockType = WriteEntity.WriteType.DDL_SHARED;
                }
            }
        }
        else {
            x.getLOG().debug("table non-partitioned");
            if (!replicationSpec.allowReplacementInto(table)) {
                return;
            }
            if (!replicationSpec.isMetadataOnly()) {
                loadTable(fromURI, table, true, new Path(fromURI), replicationSpec, x);
            }
            else {
                x.getTasks().add(alterTableTask(tblDesc, x, replicationSpec));
            }
            if (lockType == WriteEntity.WriteType.DDL_NO_LOCK) {
                lockType = WriteEntity.WriteType.DDL_SHARED;
            }
        }
        x.getOutputs().add(new WriteEntity(table, lockType));
    }
    
    private static boolean isPartitioned(final CreateTableDesc tblDesc) {
        return tblDesc.getPartCols() != null && !tblDesc.getPartCols().isEmpty();
    }
    
    private static Table tableIfExists(final CreateTableDesc tblDesc, final Hive db) throws HiveException {
        try {
            return db.getTable(tblDesc.getDatabaseName(), tblDesc.getTableName());
        }
        catch (InvalidTableException e) {
            return null;
        }
    }
}
