// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.solr.handler.dataimport;

import java.util.Hashtable;
import org.apache.solr.common.util.StrUtils;
import java.util.ArrayList;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.ContentStream;
import org.slf4j.LoggerFactory;
import java.util.LinkedHashMap;
import org.apache.solr.common.SolrException;
import java.util.List;
import java.util.Collections;
import org.w3c.dom.NodeList;
import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilder;
import org.apache.solr.core.SolrConfig;
import org.w3c.dom.Element;
import org.apache.commons.io.IOUtils;
import org.xml.sax.ErrorHandler;
import org.xml.sax.EntityResolver;
import org.apache.solr.common.ResourceLoader;
import org.apache.solr.common.util.SystemIdResolver;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.Reader;
import java.io.StringReader;
import java.util.Iterator;
import java.util.Locale;
import org.apache.solr.schema.SchemaField;
import org.xml.sax.InputSource;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import java.text.SimpleDateFormat;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.solr.core.SolrCore;
import org.apache.solr.schema.IndexSchema;
import java.util.Map;
import java.util.Properties;
import java.util.Date;
import org.apache.solr.common.util.XMLErrorLogger;
import org.slf4j.Logger;

public class DataImporter
{
    private static final Logger LOG;
    private static final XMLErrorLogger XMLLOG;
    private Status status;
    private DataConfig config;
    private Date indexStartTime;
    private Properties store;
    private Map<String, Properties> dataSourceProps;
    private IndexSchema schema;
    public DocBuilder docBuilder;
    public DocBuilder.Statistics cumulativeStatistics;
    private SolrCore core;
    private DIHPropertiesWriter propWriter;
    private ReentrantLock importLock;
    private final Map<String, Object> coreScopeSession;
    private boolean isDeltaImportSupported;
    private final String handlerName;
    static final ThreadLocal<AtomicLong> QUERY_COUNT;
    static final ThreadLocal<SimpleDateFormat> DATE_TIME_FORMAT;
    public static final String COLUMN = "column";
    public static final String TYPE = "type";
    public static final String DATA_SRC = "dataSource";
    public static final String MULTI_VALUED = "multiValued";
    public static final String NAME = "name";
    public static final String STATUS_MSGS = "status-messages";
    public static final String FULL_IMPORT_CMD = "full-import";
    public static final String IMPORT_CMD = "import";
    public static final String DELTA_IMPORT_CMD = "delta-import";
    public static final String ABORT_CMD = "abort";
    public static final String DEBUG_MODE = "debug";
    public static final String RELOAD_CONF_CMD = "reload-config";
    public static final String SHOW_CONF_CMD = "show-config";
    
    DataImporter() {
        this.status = Status.IDLE;
        this.store = new Properties();
        this.dataSourceProps = new HashMap<String, Properties>();
        this.cumulativeStatistics = new DocBuilder.Statistics();
        this.importLock = new ReentrantLock();
        this.isDeltaImportSupported = false;
        this.coreScopeSession = new ConcurrentHashMap<String, Object>();
        (this.propWriter = new SimplePropertiesWriter()).init(this);
        this.handlerName = "dataimport";
    }
    
    DataImporter(final InputSource dataConfig, final SolrCore core, final Map<String, Properties> ds, Map<String, Object> session, final String handlerName) {
        this.status = Status.IDLE;
        this.store = new Properties();
        this.dataSourceProps = new HashMap<String, Properties>();
        this.cumulativeStatistics = new DocBuilder.Statistics();
        this.importLock = new ReentrantLock();
        this.isDeltaImportSupported = false;
        this.handlerName = handlerName;
        if (dataConfig == null) {
            throw new DataImportHandlerException(500, "Configuration not found");
        }
        this.core = core;
        this.schema = core.getSchema();
        (this.propWriter = new SimplePropertiesWriter()).init(this);
        this.dataSourceProps = ds;
        if (session == null) {
            session = new HashMap<String, Object>();
        }
        this.coreScopeSession = session;
        this.loadDataConfig(dataConfig);
        for (final Map.Entry<String, SchemaField> entry : this.schema.getFields().entrySet()) {
            this.config.lowerNameVsSchemaField.put(entry.getKey().toLowerCase(Locale.ENGLISH), entry.getValue());
        }
        for (final DataConfig.Entity e : this.config.document.entities) {
            final Map<String, DataConfig.Field> fields = new HashMap<String, DataConfig.Field>();
            this.initEntity(e, fields, false);
            this.verifyWithSchema(fields);
            this.identifyPk(e);
            if (e.allAttributes.containsKey("deltaQuery")) {
                this.isDeltaImportSupported = true;
            }
        }
    }
    
    public String getHandlerName() {
        return this.handlerName;
    }
    
    private void verifyWithSchema(final Map<String, DataConfig.Field> fields) {
        final Map<String, SchemaField> schemaFields = (Map<String, SchemaField>)this.schema.getFields();
        for (final Map.Entry<String, SchemaField> entry : schemaFields.entrySet()) {
            final SchemaField sf = entry.getValue();
            if (!fields.containsKey(sf.getName()) && sf.isRequired()) {
                DataImporter.LOG.info(sf.getName() + " is a required field in SolrSchema . But not found in DataConfig");
            }
        }
        for (final Map.Entry<String, DataConfig.Field> entry2 : fields.entrySet()) {
            final DataConfig.Field fld = entry2.getValue();
            SchemaField field = this.schema.getFieldOrNull(fld.getName());
            if (field == null) {
                field = this.config.lowerNameVsSchemaField.get(fld.getName().toLowerCase(Locale.ENGLISH));
                if (field != null) {
                    continue;
                }
                DataImporter.LOG.info("The field :" + fld.getName() + " present in DataConfig does not have a counterpart in Solr Schema");
            }
        }
    }
    
    void loadAndInit(final String configStr) {
        this.loadDataConfig(new InputSource(new StringReader(configStr)));
        final Map<String, DataConfig.Field> fields = new HashMap<String, DataConfig.Field>();
        for (final DataConfig.Entity entity : this.config.document.entities) {
            this.initEntity(entity, fields, false);
        }
    }
    
    private void identifyPk(final DataConfig.Entity entity) {
        final SchemaField uniqueKey = this.schema.getUniqueKeyField();
        String schemaPk = "";
        if (uniqueKey != null) {
            schemaPk = uniqueKey.getName();
            entity.pkMappingFromSchema = schemaPk;
            for (final DataConfig.Field field : entity.fields) {
                if (field.getName().equals(schemaPk)) {
                    entity.pkMappingFromSchema = field.column;
                    break;
                }
            }
        }
    }
    
    private void loadDataConfig(final InputSource configFile) {
        try {
            final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            if (this.core != null && configFile.getSystemId() != null) {
                try {
                    dbf.setXIncludeAware(true);
                    dbf.setNamespaceAware(true);
                }
                catch (UnsupportedOperationException e2) {
                    DataImporter.LOG.warn("XML parser doesn't support XInclude option");
                }
            }
            final DocumentBuilder builder = dbf.newDocumentBuilder();
            if (this.core != null) {
                builder.setEntityResolver((EntityResolver)new SystemIdResolver((ResourceLoader)this.core.getResourceLoader()));
            }
            builder.setErrorHandler((ErrorHandler)DataImporter.XMLLOG);
            Document document;
            try {
                document = builder.parse(configFile);
            }
            finally {
                IOUtils.closeQuietly(configFile.getByteStream());
            }
            this.config = new DataConfig();
            final NodeList elems = document.getElementsByTagName("dataConfig");
            if (elems == null || elems.getLength() == 0) {
                throw new DataImportHandlerException(500, "the root node '<dataConfig>' is missing");
            }
            this.config.readFromXml((Element)elems.item(0));
            DataImporter.LOG.info("Data Configuration loaded successfully");
        }
        catch (Exception e) {
            SolrConfig.severeErrors.add(e);
            throw new DataImportHandlerException(500, "Exception occurred while initializing context", e);
        }
    }
    
    private void initEntity(final DataConfig.Entity e, final Map<String, DataConfig.Field> fields, final boolean docRootFound) {
        e.allAttributes.put("dataSource", e.dataSource);
        if (!docRootFound && !"false".equals(e.docRoot)) {
            e.isDocRoot = true;
        }
        if (e.allAttributes.get("threads") != null) {
            if (docRootFound) {
                throw new DataImportHandlerException(500, "'threads' not allowed below rootEntity ");
            }
            DataImporter.LOG.warn("The DataImportHandler 'threads' parameter is deprecated and will be removed in a future release.");
            this.config.isMultiThreaded = true;
        }
        if (e.fields != null) {
            for (final DataConfig.Field f : e.fields) {
                if (this.schema != null) {
                    if (f.name != null && f.name.contains("${")) {
                        f.dynamicName = true;
                        continue;
                    }
                    SchemaField schemaField = this.schema.getFieldOrNull(f.getName());
                    if (schemaField == null) {
                        schemaField = this.config.lowerNameVsSchemaField.get(f.getName().toLowerCase(Locale.ENGLISH));
                        if (schemaField != null) {
                            f.name = schemaField.getName();
                        }
                    }
                    if (schemaField != null) {
                        f.multiValued = schemaField.multiValued();
                        f.allAttributes.put("multiValued", Boolean.toString(schemaField.multiValued()));
                        f.allAttributes.put("type", schemaField.getType().getTypeName());
                        f.allAttributes.put("indexed", Boolean.toString(schemaField.indexed()));
                        f.allAttributes.put("stored", Boolean.toString(schemaField.stored()));
                        f.allAttributes.put("defaultValue", schemaField.getDefaultValue());
                    }
                    else {
                        f.toWrite = false;
                    }
                }
                fields.put(f.getName(), f);
                f.entity = e;
                f.allAttributes.put("boost", f.boost.toString());
                f.allAttributes.put("toWrite", Boolean.toString(f.toWrite));
                e.allFieldsList.add(Collections.unmodifiableMap((Map<? extends String, ? extends String>)f.allAttributes));
            }
        }
        e.allFieldsList = Collections.unmodifiableList((List<? extends Map<String, String>>)e.allFieldsList);
        e.allAttributes = Collections.unmodifiableMap((Map<? extends String, ? extends String>)e.allAttributes);
        if (e.entities == null) {
            return;
        }
        for (final DataConfig.Entity e2 : e.entities) {
            e2.parentEntity = e;
            this.initEntity(e2, fields, e.isDocRoot || docRootFound);
        }
    }
    
    DataConfig getConfig() {
        return this.config;
    }
    
    Date getIndexStartTime() {
        return this.indexStartTime;
    }
    
    void setIndexStartTime(final Date indextStartTime) {
        this.indexStartTime = indextStartTime;
    }
    
    void store(final Object key, final Object value) {
        this.store.put(key, value);
    }
    
    Object retrieve(final Object key) {
        return ((Hashtable<K, Object>)this.store).get(key);
    }
    
    DataSource getDataSourceInstance(final DataConfig.Entity key, final String name, final Context ctx) {
        Properties p = this.dataSourceProps.get(name);
        if (p == null) {
            p = this.config.dataSources.get(name);
        }
        if (p == null) {
            p = this.dataSourceProps.get(null);
        }
        if (p == null) {
            p = this.config.dataSources.get(null);
        }
        if (p == null) {
            throw new DataImportHandlerException(500, "No dataSource :" + name + " available for entity :" + key.name);
        }
        final String type = p.getProperty("type");
        DataSource dataSrc = null;
        if (type == null) {
            dataSrc = new JdbcDataSource();
        }
        else {
            try {
                dataSrc = DocBuilder.loadClass(type, this.getCore()).newInstance();
            }
            catch (Exception e) {
                DataImportHandlerException.wrapAndThrow(500, e, "Invalid type for data source: " + type);
            }
        }
        try {
            final Properties copyProps = new Properties();
            copyProps.putAll(p);
            final Map<String, Object> map = ctx.getRequestParameters();
            if (map.containsKey("rows")) {
                int rows = Integer.parseInt(map.get("rows"));
                if (map.containsKey("start")) {
                    rows += Integer.parseInt(map.get("start"));
                }
                copyProps.setProperty("maxRows", String.valueOf(rows));
            }
            dataSrc.init(ctx, copyProps);
        }
        catch (Exception e) {
            DataImportHandlerException.wrapAndThrow(500, e, "Failed to initialize DataSource: " + key.dataSource);
        }
        return dataSrc;
    }
    
    public Status getStatus() {
        return this.status;
    }
    
    public void setStatus(final Status status) {
        this.status = status;
    }
    
    public boolean isBusy() {
        return this.importLock.isLocked();
    }
    
    public void doFullImport(final SolrWriter writer, final RequestParams requestParams) {
        DataImporter.LOG.info("Starting Full Import");
        this.setStatus(Status.RUNNING_FULL_DUMP);
        this.setIndexStartTime(new Date());
        try {
            this.docBuilder = new DocBuilder(this, writer, this.propWriter, requestParams);
            this.checkWritablePersistFile(writer);
            this.docBuilder.execute();
            if (!requestParams.debug) {
                this.cumulativeStatistics.add(this.docBuilder.importStatistics);
            }
        }
        catch (Throwable t) {
            SolrException.log(DataImporter.LOG, "Full Import failed", t);
            this.docBuilder.rollback();
        }
        finally {
            this.setStatus(Status.IDLE);
            this.config.clearCaches();
            DocBuilder.INSTANCE.set(null);
            this.docBuilder.destroy();
        }
    }
    
    private void checkWritablePersistFile(final SolrWriter writer) {
        if (this.isDeltaImportSupported && !this.propWriter.isWritable()) {
            throw new DataImportHandlerException(500, "Properties is not writable. Delta imports are supported by data config but will not work.");
        }
    }
    
    public void doDeltaImport(final SolrWriter writer, final RequestParams requestParams) {
        DataImporter.LOG.info("Starting Delta Import");
        this.setStatus(Status.RUNNING_DELTA_DUMP);
        try {
            this.setIndexStartTime(new Date());
            this.docBuilder = new DocBuilder(this, writer, this.propWriter, requestParams);
            this.checkWritablePersistFile(writer);
            this.docBuilder.execute();
            if (!requestParams.debug) {
                this.cumulativeStatistics.add(this.docBuilder.importStatistics);
            }
        }
        catch (Throwable t) {
            DataImporter.LOG.error("Delta Import Failed", t);
            this.docBuilder.rollback();
        }
        finally {
            this.setStatus(Status.IDLE);
            this.config.clearCaches();
            DocBuilder.INSTANCE.set(null);
            this.docBuilder.destroy();
        }
    }
    
    public void runAsync(final RequestParams reqParams, final SolrWriter sw) {
        new Thread() {
            @Override
            public void run() {
                DataImporter.this.runCmd(reqParams, sw);
            }
        }.start();
    }
    
    void runCmd(final RequestParams reqParams, final SolrWriter sw) {
        final String command = reqParams.command;
        if (command.equals("abort")) {
            if (this.docBuilder != null) {
                this.docBuilder.abort();
            }
            return;
        }
        if (!this.importLock.tryLock()) {
            DataImporter.LOG.warn("Import command failed . another import is running");
            return;
        }
        try {
            if ("full-import".equals(command) || "import".equals(command)) {
                this.doFullImport(sw, reqParams);
            }
            else if (command.equals("delta-import")) {
                this.doDeltaImport(sw, reqParams);
            }
        }
        finally {
            this.importLock.unlock();
        }
    }
    
    Map<String, String> getStatusMessages() {
        final Map statusMessages = (Map)this.retrieve("status-messages");
        final Map<String, String> result = new LinkedHashMap<String, String>();
        if (statusMessages != null) {
            synchronized (statusMessages) {
                for (final Object o : statusMessages.entrySet()) {
                    final Map.Entry e = (Map.Entry)o;
                    result.put(e.getKey(), e.getValue().toString());
                }
            }
        }
        return result;
    }
    
    DocBuilder getDocBuilder() {
        return this.docBuilder;
    }
    
    IndexSchema getSchema() {
        return this.schema;
    }
    
    Map<String, Object> getCoreScopeSession() {
        return this.coreScopeSession;
    }
    
    SolrCore getCore() {
        return this.core;
    }
    
    static {
        LOG = LoggerFactory.getLogger((Class)DataImporter.class);
        XMLLOG = new XMLErrorLogger(DataImporter.LOG);
        QUERY_COUNT = new ThreadLocal<AtomicLong>() {
            @Override
            protected AtomicLong initialValue() {
                return new AtomicLong();
            }
        };
        DATE_TIME_FORMAT = new ThreadLocal<SimpleDateFormat>() {
            @Override
            protected SimpleDateFormat initialValue() {
                return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            }
        };
    }
    
    public enum Status
    {
        IDLE, 
        RUNNING_FULL_DUMP, 
        RUNNING_DELTA_DUMP, 
        JOB_FAILED;
    }
    
    static final class MSG
    {
        public static final String NO_CONFIG_FOUND = "Configuration not found";
        public static final String NO_INIT = "DataImportHandler started. Not Initialized. No commands can be run";
        public static final String INVALID_CONFIG = "FATAL: Could not create importer. DataImporter config invalid";
        public static final String LOAD_EXP = "Exception while loading DataImporter";
        public static final String JMX_DESC = "Manage data import from databases to Solr";
        public static final String CMD_RUNNING = "A command is still running...";
        public static final String DEBUG_NOT_ENABLED = "Debug not enabled. Add a tag <str name=\"enableDebug\">true</str> in solrconfig.xml";
        public static final String CONFIG_RELOADED = "Configuration Re-loaded sucessfully";
        public static final String TOTAL_DOC_PROCESSED = "Total Documents Processed";
        public static final String TOTAL_FAILED_DOCS = "Total Documents Failed";
        public static final String TOTAL_QUERIES_EXECUTED = "Total Requests made to DataSource";
        public static final String TOTAL_ROWS_EXECUTED = "Total Rows Fetched";
        public static final String TOTAL_DOCS_DELETED = "Total Documents Deleted";
        public static final String TOTAL_DOCS_SKIPPED = "Total Documents Skipped";
    }
    
    static final class RequestParams
    {
        public String command;
        public boolean debug;
        public boolean verbose;
        public boolean syncMode;
        public boolean commit;
        public boolean optimize;
        public int start;
        public long rows;
        public boolean clean;
        public List<String> entities;
        public Map<String, Object> requestParams;
        public String dataConfig;
        public ContentStream contentStream;
        public List<SolrInputDocument> debugDocuments;
        public NamedList debugVerboseOutput;
        
        public RequestParams() {
            this.command = null;
            this.debug = false;
            this.verbose = false;
            this.syncMode = false;
            this.commit = true;
            this.optimize = false;
            this.start = 0;
            this.rows = 2147483647L;
            this.clean = true;
            this.debugDocuments = new ArrayList<SolrInputDocument>(0);
            this.debugVerboseOutput = null;
        }
        
        public RequestParams(final Map<String, Object> requestParams) {
            this.command = null;
            this.debug = false;
            this.verbose = false;
            this.syncMode = false;
            this.commit = true;
            this.optimize = false;
            this.start = 0;
            this.rows = 2147483647L;
            this.clean = true;
            this.debugDocuments = new ArrayList<SolrInputDocument>(0);
            this.debugVerboseOutput = null;
            if (requestParams.containsKey("command")) {
                this.command = requestParams.get("command");
            }
            if (StrUtils.parseBool((String)requestParams.get("debug"), false)) {
                this.debug = true;
                this.rows = 10L;
                this.commit = false;
                this.clean = false;
                this.verbose = StrUtils.parseBool((String)requestParams.get("verbose"), false);
            }
            this.syncMode = StrUtils.parseBool((String)requestParams.get("synchronous"), false);
            if ("delta-import".equals(this.command) || "import".equals(this.command)) {
                this.clean = false;
            }
            if (requestParams.containsKey("commit")) {
                this.commit = StrUtils.parseBool((String)requestParams.get("commit"), true);
            }
            if (requestParams.containsKey("start")) {
                this.start = Integer.parseInt(requestParams.get("start"));
            }
            if (requestParams.containsKey("rows")) {
                this.rows = Integer.parseInt(requestParams.get("rows"));
            }
            if (requestParams.containsKey("clean")) {
                this.clean = StrUtils.parseBool((String)requestParams.get("clean"), true);
            }
            if (requestParams.containsKey("optimize")) {
                this.optimize = StrUtils.parseBool((String)requestParams.get("optimize"), false);
                if (this.optimize) {
                    this.commit = true;
                }
            }
            final Object o = requestParams.get("entity");
            if (o != null && o instanceof String) {
                (this.entities = new ArrayList<String>()).add((String)o);
            }
            else if (o != null && o instanceof List) {
                this.entities = requestParams.get("entity");
            }
            this.dataConfig = requestParams.get("dataConfig");
            if (this.dataConfig != null && this.dataConfig.trim().length() == 0) {
                this.dataConfig = null;
            }
            this.requestParams = requestParams;
        }
    }
}
