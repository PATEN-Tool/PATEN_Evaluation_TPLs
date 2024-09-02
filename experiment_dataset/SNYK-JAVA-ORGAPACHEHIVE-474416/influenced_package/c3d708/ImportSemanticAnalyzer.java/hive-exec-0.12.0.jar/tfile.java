// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.hadoop.hive.ql.parse;

import java.net.URISyntaxException;
import java.util.Collections;
import org.apache.commons.lang.ObjectUtils;
import org.apache.hadoop.hive.metastore.TableType;
import java.util.Comparator;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.metastore.api.MetaException;
import org.apache.hadoop.hive.ql.plan.LoadFileDesc;
import org.apache.hadoop.hive.ql.plan.MoveWork;
import org.apache.hadoop.hive.ql.plan.LoadTableDesc;
import java.util.TreeMap;
import org.apache.hadoop.hive.ql.exec.Utilities;
import org.apache.hadoop.hive.ql.plan.CopyWork;
import java.util.Iterator;
import java.net.URI;
import org.antlr.runtime.tree.Tree;
import org.apache.hadoop.hive.ql.metadata.InvalidTableException;
import org.apache.hadoop.hive.ql.exec.TaskFactory;
import org.apache.hadoop.hive.ql.plan.DDLWork;
import org.apache.hadoop.hive.ql.hooks.WriteEntity;
import java.io.Serializable;
import org.apache.hadoop.hive.ql.exec.Task;
import java.util.LinkedHashMap;
import java.io.IOException;
import org.apache.hadoop.hive.ql.ErrorMsg;
import org.apache.hadoop.hive.metastore.Warehouse;
import org.apache.hadoop.hive.metastore.api.Partition;
import java.util.Map;
import org.apache.hadoop.hive.metastore.api.Order;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import java.util.List;
import org.apache.hadoop.hive.ql.plan.CreateTableDesc;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.hadoop.hive.ql.session.SessionState;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.ql.plan.AddPartitionDesc;
import java.util.ArrayList;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.hive.conf.HiveConf;

public class ImportSemanticAnalyzer extends BaseSemanticAnalyzer
{
    public static final String METADATA_NAME = "_metadata";
    private boolean tableExists;
    
    public ImportSemanticAnalyzer(final HiveConf conf) throws SemanticException {
        super(conf);
        this.tableExists = false;
    }
    
    public boolean existsTable() {
        return this.tableExists;
    }
    
    @Override
    public void analyzeInternal(final ASTNode ast) throws SemanticException {
        try {
            final Tree fromTree = ast.getChild(0);
            final String tmpPath = BaseSemanticAnalyzer.stripQuotes(fromTree.getText());
            final URI fromURI = EximUtil.getValidatedURI(this.conf, tmpPath);
            final FileSystem fs = FileSystem.get(fromURI, (Configuration)this.conf);
            String dbname = null;
            CreateTableDesc tblDesc = null;
            final List<AddPartitionDesc> partitionDescs = new ArrayList<AddPartitionDesc>();
            final Path fromPath = new Path(fromURI.getScheme(), fromURI.getAuthority(), fromURI.getPath());
            try {
                final Path metadataPath = new Path(fromPath, "_metadata");
                final Map.Entry<Table, List<Partition>> rv = EximUtil.readMetaData(fs, metadataPath);
                dbname = SessionState.get().getCurrentDatabase();
                final Table table = rv.getKey();
                tblDesc = new CreateTableDesc(table.getTableName(), false, table.getSd().getCols(), table.getPartitionKeys(), table.getSd().getBucketCols(), table.getSd().getSortCols(), table.getSd().getNumBuckets(), null, null, null, null, null, null, table.getSd().getInputFormat(), table.getSd().getOutputFormat(), null, table.getSd().getSerdeInfo().getSerializationLib(), null, table.getSd().getSerdeInfo().getParameters(), table.getParameters(), false, (null == table.getSd().getSkewedInfo()) ? null : table.getSd().getSkewedInfo().getSkewedColNames(), (null == table.getSd().getSkewedInfo()) ? null : table.getSd().getSkewedInfo().getSkewedColValues());
                tblDesc.setStoredAsSubDirectories(table.getSd().isStoredAsSubDirectories());
                final List<FieldSchema> partCols = tblDesc.getPartCols();
                final List<String> partColNames = new ArrayList<String>(partCols.size());
                for (final FieldSchema fsc : partCols) {
                    partColNames.add(fsc.getName());
                }
                final List<Partition> partitions = rv.getValue();
                for (final Partition partition : partitions) {
                    final AddPartitionDesc partDesc = new AddPartitionDesc(dbname, tblDesc.getTableName(), EximUtil.makePartSpec(tblDesc.getPartCols(), partition.getValues()), partition.getSd().getLocation(), partition.getParameters());
                    partDesc.setInputFormat(partition.getSd().getInputFormat());
                    partDesc.setOutputFormat(partition.getSd().getOutputFormat());
                    partDesc.setNumBuckets(partition.getSd().getNumBuckets());
                    partDesc.setCols(partition.getSd().getCols());
                    partDesc.setSerializationLib(partition.getSd().getSerdeInfo().getSerializationLib());
                    partDesc.setSerdeParams(partition.getSd().getSerdeInfo().getParameters());
                    partDesc.setBucketCols(partition.getSd().getBucketCols());
                    partDesc.setSortCols(partition.getSd().getSortCols());
                    partDesc.setLocation(new Path(fromPath, Warehouse.makePartName((List)tblDesc.getPartCols(), partition.getValues())).toString());
                    partitionDescs.add(partDesc);
                }
            }
            catch (IOException e) {
                throw new SemanticException(ErrorMsg.INVALID_PATH.getMsg(), e);
            }
            this.LOG.debug((Object)"metadata read and parsed");
            for (int i = 1; i < ast.getChildCount(); ++i) {
                final ASTNode child = (ASTNode)ast.getChild(i);
                switch (child.getToken().getType()) {
                    case 96: {
                        tblDesc.setExternal(true);
                        break;
                    }
                    case 781: {
                        String location = BaseSemanticAnalyzer.unescapeSQLString(child.getChild(0).getText());
                        location = EximUtil.relativeToAbsolutePath(this.conf, location);
                        tblDesc.setLocation(location);
                        break;
                    }
                    case 769: {
                        final Tree tableTree = child.getChild(0);
                        final String tableName = BaseSemanticAnalyzer.getUnescapedName((ASTNode)tableTree);
                        tblDesc.setTableName(tableName);
                        final LinkedHashMap<String, String> partSpec = new LinkedHashMap<String, String>();
                        if (child.getChildCount() != 2) {
                            break;
                        }
                        final ASTNode partspec = (ASTNode)child.getChild(1);
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
                        boolean found = false;
                        final Iterator<AddPartitionDesc> partnIter = partitionDescs.listIterator();
                        while (partnIter.hasNext()) {
                            final AddPartitionDesc addPartitionDesc = partnIter.next();
                            if (!found && addPartitionDesc.getPartSpec().equals(partSpec)) {
                                found = true;
                            }
                            else {
                                partnIter.remove();
                            }
                        }
                        if (!found) {
                            throw new SemanticException(ErrorMsg.INVALID_PARTITION.getMsg(" - Specified partition not found in import directory"));
                        }
                        break;
                    }
                }
            }
            if (tblDesc.getTableName() == null) {
                throw new SemanticException(ErrorMsg.NEED_TABLE_SPECIFICATION.getMsg());
            }
            this.conf.set("import.destination.table", tblDesc.getTableName());
            for (final AddPartitionDesc addPartitionDesc2 : partitionDescs) {
                addPartitionDesc2.setTableName(tblDesc.getTableName());
            }
            final Warehouse wh = new Warehouse((Configuration)this.conf);
            try {
                final org.apache.hadoop.hive.ql.metadata.Table table2 = this.db.getTable(tblDesc.getTableName());
                checkTable(table2, tblDesc);
                this.LOG.debug((Object)("table " + tblDesc.getTableName() + " exists: metadata checked"));
                this.tableExists = true;
                this.conf.set("import.destination.dir", table2.getDataLocation().toString());
                if (table2.isPartitioned()) {
                    this.LOG.debug((Object)"table partitioned");
                    for (final AddPartitionDesc addPartitionDesc3 : partitionDescs) {
                        if (this.db.getPartition(table2, addPartitionDesc3.getPartSpec(), false) != null) {
                            throw new SemanticException(ErrorMsg.PARTITION_EXISTS.getMsg(partSpecToString(addPartitionDesc3.getPartSpec())));
                        }
                        this.rootTasks.add((Task<? extends Serializable>)this.addSinglePartition(fromURI, fs, tblDesc, table2, wh, addPartitionDesc3));
                    }
                }
                else {
                    this.LOG.debug((Object)"table non-partitioned");
                    this.checkTargetLocationEmpty(fs, new Path(table2.getDataLocation().toString()));
                    this.loadTable(fromURI, table2);
                }
                this.outputs.add(new WriteEntity(table2));
            }
            catch (InvalidTableException e4) {
                this.LOG.debug((Object)("table " + tblDesc.getTableName() + " does not exist"));
                final Task<?> t = TaskFactory.get((Object)new DDLWork(this.getInputs(), this.getOutputs(), tblDesc), this.conf, (Task<? extends Serializable>[])new Task[0]);
                final org.apache.hadoop.hive.ql.metadata.Table table3 = new org.apache.hadoop.hive.ql.metadata.Table(dbname, tblDesc.getTableName());
                final String currentDb = SessionState.get().getCurrentDatabase();
                this.conf.set("import.destination.dir", wh.getTablePath(this.db.getDatabaseCurrent(), tblDesc.getTableName()).toString());
                if (tblDesc.getPartCols() != null && tblDesc.getPartCols().size() != 0) {
                    for (final AddPartitionDesc addPartitionDesc4 : partitionDescs) {
                        t.addDependentTask((Task<? extends Serializable>)this.addSinglePartition(fromURI, fs, tblDesc, table3, wh, addPartitionDesc4));
                    }
                }
                else {
                    this.LOG.debug((Object)"adding dependent CopyWork/MoveWork for table");
                    if (tblDesc.isExternal() && tblDesc.getLocation() == null) {
                        this.LOG.debug((Object)"Importing in place, no emptiness check, no copying/loading");
                        final Path dataPath = new Path(fromURI.toString(), "data");
                        tblDesc.setLocation(dataPath.toString());
                    }
                    else {
                        Path tablePath = null;
                        if (tblDesc.getLocation() != null) {
                            tablePath = new Path(tblDesc.getLocation());
                        }
                        else {
                            tablePath = wh.getTablePath(this.db.getDatabaseCurrent(), tblDesc.getTableName());
                        }
                        this.checkTargetLocationEmpty(fs, tablePath);
                        t.addDependentTask((Task<? extends Serializable>)this.loadTable(fromURI, table3));
                    }
                }
                this.rootTasks.add((Task<? extends Serializable>)t);
            }
        }
        catch (SemanticException e2) {
            throw e2;
        }
        catch (Exception e3) {
            throw new SemanticException(ErrorMsg.GENERIC_ERROR.getMsg(), e3);
        }
    }
    
    private Task<?> loadTable(final URI fromURI, final org.apache.hadoop.hive.ql.metadata.Table table) {
        final Path dataPath = new Path(fromURI.toString(), "data");
        final String tmpURI = this.ctx.getExternalTmpFileURI(fromURI);
        final Task<?> copyTask = TaskFactory.get((Object)new CopyWork(dataPath.toString(), tmpURI, false), this.conf, (Task<? extends Serializable>[])new Task[0]);
        final LoadTableDesc loadTableWork = new LoadTableDesc(tmpURI.toString(), this.ctx.getExternalTmpFileURI(fromURI), Utilities.getTableDesc(table), new TreeMap<String, String>(), false);
        final Task<?> loadTableTask = TaskFactory.get((Object)new MoveWork(this.getInputs(), this.getOutputs(), loadTableWork, null, false), this.conf, (Task<? extends Serializable>[])new Task[0]);
        copyTask.addDependentTask((Task<? extends Serializable>)loadTableTask);
        this.rootTasks.add((Task<? extends Serializable>)copyTask);
        return loadTableTask;
    }
    
    private Task<?> addSinglePartition(final URI fromURI, final FileSystem fs, final CreateTableDesc tblDesc, final org.apache.hadoop.hive.ql.metadata.Table table, final Warehouse wh, final AddPartitionDesc addPartitionDesc) throws MetaException, IOException, HiveException {
        if (tblDesc.isExternal() && tblDesc.getLocation() == null) {
            this.LOG.debug((Object)("Importing in-place: adding AddPart for partition " + partSpecToString(addPartitionDesc.getPartSpec())));
            final Task<?> addPartTask = TaskFactory.get((Object)new DDLWork(this.getInputs(), this.getOutputs(), addPartitionDesc), this.conf, (Task<? extends Serializable>[])new Task[0]);
            return addPartTask;
        }
        final String srcLocation = addPartitionDesc.getLocation();
        Path tgtPath = null;
        if (tblDesc.getLocation() == null) {
            if (table.getDataLocation() != null) {
                tgtPath = new Path(table.getDataLocation().toString(), Warehouse.makePartPath((Map)addPartitionDesc.getPartSpec()));
            }
            else {
                tgtPath = new Path(wh.getTablePath(this.db.getDatabaseCurrent(), tblDesc.getTableName()), Warehouse.makePartPath((Map)addPartitionDesc.getPartSpec()));
            }
        }
        else {
            tgtPath = new Path(tblDesc.getLocation(), Warehouse.makePartPath((Map)addPartitionDesc.getPartSpec()));
        }
        this.checkTargetLocationEmpty(fs, tgtPath);
        addPartitionDesc.setLocation(tgtPath.toString());
        this.LOG.debug((Object)("adding dependent CopyWork/AddPart/MoveWork for partition " + partSpecToString(addPartitionDesc.getPartSpec()) + " with source location: " + srcLocation));
        final String tmpURI = this.ctx.getExternalTmpFileURI(fromURI);
        final Task<?> copyTask = TaskFactory.get((Object)new CopyWork(srcLocation, tmpURI, false), this.conf, (Task<? extends Serializable>[])new Task[0]);
        final Task<?> addPartTask2 = TaskFactory.get((Object)new DDLWork(this.getInputs(), this.getOutputs(), addPartitionDesc), this.conf, (Task<? extends Serializable>[])new Task[0]);
        final LoadTableDesc loadTableWork = new LoadTableDesc(tmpURI, this.ctx.getExternalTmpFileURI(fromURI), Utilities.getTableDesc(table), addPartitionDesc.getPartSpec(), true);
        loadTableWork.setInheritTableSpecs(false);
        final Task<?> loadPartTask = TaskFactory.get((Object)new MoveWork(this.getInputs(), this.getOutputs(), loadTableWork, null, false), this.conf, (Task<? extends Serializable>[])new Task[0]);
        copyTask.addDependentTask((Task<? extends Serializable>)loadPartTask);
        addPartTask2.addDependentTask((Task<? extends Serializable>)loadPartTask);
        this.rootTasks.add((Task<? extends Serializable>)copyTask);
        return addPartTask2;
    }
    
    private void checkTargetLocationEmpty(final FileSystem fs, final Path targetPath) throws IOException, SemanticException {
        this.LOG.debug((Object)("checking emptiness of " + targetPath.toString()));
        if (fs.exists(targetPath)) {
            final FileStatus[] status = fs.listStatus(targetPath);
            if (status.length > 0) {
                this.LOG.debug((Object)("Files inc. " + status[0].getPath().toString() + " found in path : " + targetPath.toString()));
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
    
    private static void checkTable(final org.apache.hadoop.hive.ql.metadata.Table table, final CreateTableDesc tableDesc) throws SemanticException, URISyntaxException {
        EximUtil.validateTable(table);
        if (!table.isPartitioned()) {
            if (tableDesc.isExternal()) {
                throw new SemanticException(ErrorMsg.INCOMPATIBLE_SCHEMA.getMsg(" External table cannot overwrite existing table. Drop existing table first."));
            }
        }
        else if (tableDesc.isExternal() && !table.getTableType().equals((Object)TableType.EXTERNAL_TABLE)) {
            throw new SemanticException(ErrorMsg.INCOMPATIBLE_SCHEMA.getMsg(" External table cannot overwrite existing table. Drop existing table first."));
        }
        if (!table.isPartitioned() && tableDesc.getLocation() != null && !table.getDataLocation().equals(new URI(tableDesc.getLocation()))) {
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
        final String importedofc = tableDesc.getOutputFormat();
        if (!existingifc.equals(importedifc) || !existingofc.equals(importedofc)) {
            throw new SemanticException(ErrorMsg.INCOMPATIBLE_SCHEMA.getMsg(" Table inputformat/outputformats do not match"));
        }
        final String existingSerde = table.getSerializationLib();
        final String importedSerde = tableDesc.getSerName();
        if (!existingSerde.equals(importedSerde)) {
            throw new SemanticException(ErrorMsg.INCOMPATIBLE_SCHEMA.getMsg(" Table Serde class does not match"));
        }
        final String existingSerdeFormat = table.getSerdeParam("serialization.format");
        final String importedSerdeFormat = tableDesc.getSerdeProps().get("serialization.format");
        if (!ObjectUtils.equals((Object)existingSerdeFormat, (Object)importedSerdeFormat)) {
            throw new SemanticException(ErrorMsg.INCOMPATIBLE_SCHEMA.getMsg(" Table Serde format does not match"));
        }
        if (!ObjectUtils.equals((Object)table.getBucketCols(), (Object)tableDesc.getBucketCols())) {
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
                    if (!ObjectUtils.equals((Object)v1, (Object)v2)) {
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
}
