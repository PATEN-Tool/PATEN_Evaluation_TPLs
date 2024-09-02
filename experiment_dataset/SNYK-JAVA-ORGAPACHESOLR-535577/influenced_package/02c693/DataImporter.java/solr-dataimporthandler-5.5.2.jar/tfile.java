// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.solr.handler.dataimport;

import java.util.Hashtable;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import org.apache.solr.common.SolrException;
import org.w3c.dom.NodeList;
import java.util.List;
import org.apache.solr.handler.dataimport.config.PropertyWriter;
import org.apache.solr.handler.dataimport.config.Script;
import org.apache.solr.handler.dataimport.config.ConfigParseUtil;
import org.w3c.dom.Element;
import java.util.ArrayList;
import java.util.Iterator;
import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilder;
import org.apache.solr.handler.dataimport.config.Entity;
import org.apache.commons.io.IOUtils;
import org.xml.sax.ErrorHandler;
import org.xml.sax.EntityResolver;
import org.apache.lucene.analysis.util.ResourceLoader;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.util.Collections;
import org.apache.solr.util.SystemIdResolver;
import java.io.Reader;
import org.xml.sax.InputSource;
import java.io.StringReader;
import org.apache.solr.common.util.NamedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.solr.core.SolrCore;
import org.apache.solr.schema.IndexSchema;
import java.util.Map;
import java.util.Properties;
import java.util.Date;
import org.apache.solr.handler.dataimport.config.DIHConfiguration;
import org.apache.solr.common.util.XMLErrorLogger;
import org.slf4j.Logger;

public class DataImporter
{
    private static final Logger LOG;
    private static final XMLErrorLogger XMLLOG;
    private Status status;
    private DIHConfiguration config;
    private Date indexStartTime;
    private Properties store;
    private Map<String, Map<String, String>> requestLevelDataSourceProps;
    private IndexSchema schema;
    public DocBuilder docBuilder;
    public DocBuilder.Statistics cumulativeStatistics;
    private SolrCore core;
    private Map<String, Object> coreScopeSession;
    private ReentrantLock importLock;
    private boolean isDeltaImportSupported;
    private final String handlerName;
    static final ThreadLocal<AtomicLong> QUERY_COUNT;
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
        this.requestLevelDataSourceProps = new HashMap<String, Map<String, String>>();
        this.cumulativeStatistics = new DocBuilder.Statistics();
        this.coreScopeSession = new ConcurrentHashMap<String, Object>();
        this.importLock = new ReentrantLock();
        this.isDeltaImportSupported = false;
        this.handlerName = "dataimport";
    }
    
    DataImporter(final SolrCore core, final String handlerName) {
        this.status = Status.IDLE;
        this.store = new Properties();
        this.requestLevelDataSourceProps = new HashMap<String, Map<String, String>>();
        this.cumulativeStatistics = new DocBuilder.Statistics();
        this.coreScopeSession = new ConcurrentHashMap<String, Object>();
        this.importLock = new ReentrantLock();
        this.isDeltaImportSupported = false;
        this.handlerName = handlerName;
        this.core = core;
        this.schema = core.getLatestSchema();
    }
    
    boolean maybeReloadConfiguration(final RequestInfo params, final NamedList<?> defaultParams) throws IOException {
        if (this.importLock.tryLock()) {
            boolean success = false;
            try {
                if (null != params.getRequest() && this.schema != params.getRequest().getSchema()) {
                    this.schema = params.getRequest().getSchema();
                }
                final String dataConfigText = params.getDataConfig();
                final String dataconfigFile = params.getConfigFile();
                InputSource is = null;
                if (dataConfigText != null && dataConfigText.length() > 0) {
                    is = new InputSource(new StringReader(dataConfigText));
                }
                else if (dataconfigFile != null) {
                    is = new InputSource(this.core.getResourceLoader().openResource(dataconfigFile));
                    is.setSystemId(SystemIdResolver.createSystemIdFromResourceName(dataconfigFile));
                    DataImporter.LOG.info("Loading DIH Configuration: " + dataconfigFile);
                }
                if (is != null) {
                    this.config = this.loadDataConfig(is);
                    success = true;
                }
                final Map<String, Map<String, String>> dsProps = new HashMap<String, Map<String, String>>();
                if (defaultParams != null) {
                    for (int position = 0; position < defaultParams.size(); ++position) {
                        if (defaultParams.getName(position) == null) {
                            break;
                        }
                        final String name = defaultParams.getName(position);
                        if (name.equals("datasource")) {
                            success = true;
                            final NamedList dsConfig = (NamedList)defaultParams.getVal(position);
                            DataImporter.LOG.info("Getting configuration for Global Datasource...");
                            final Map<String, String> props = new HashMap<String, String>();
                            for (int i = 0; i < dsConfig.size(); ++i) {
                                props.put(dsConfig.getName(i), dsConfig.getVal(i).toString());
                            }
                            DataImporter.LOG.info("Adding properties to datasource: " + props);
                            dsProps.put((String)dsConfig.get("name"), props);
                        }
                    }
                }
                this.requestLevelDataSourceProps = Collections.unmodifiableMap((Map<? extends String, ? extends Map<String, String>>)dsProps);
            }
            catch (IOException ioe) {
                throw ioe;
            }
            finally {
                this.importLock.unlock();
            }
            return success;
        }
        return false;
    }
    
    public String getHandlerName() {
        return this.handlerName;
    }
    
    public IndexSchema getSchema() {
        return this.schema;
    }
    
    public void loadAndInit(final String configStr) {
        this.config = this.loadDataConfig(new InputSource(new StringReader(configStr)));
    }
    
    public void loadAndInit(final InputSource configFile) {
        this.config = this.loadDataConfig(configFile);
    }
    
    public DIHConfiguration loadDataConfig(final InputSource configFile) {
        DIHConfiguration dihcfg = null;
        try {
            final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            if (this.core != null && configFile.getSystemId() != null) {
                try {
                    dbf.setXIncludeAware(true);
                    dbf.setNamespaceAware(true);
                }
                catch (UnsupportedOperationException e3) {
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
            dihcfg = this.readFromXml(document);
            DataImporter.LOG.info("Data Configuration loaded successfully");
        }
        catch (Exception e) {
            throw new DataImportHandlerException(500, "Data Config problem: " + e.getMessage(), e);
        }
        for (final Entity e2 : dihcfg.getEntities()) {
            if (e2.getAllAttributes().containsKey("deltaQuery")) {
                this.isDeltaImportSupported = true;
                break;
            }
        }
        return dihcfg;
    }
    
    public DIHConfiguration readFromXml(final Document xmlDocument) {
        final List<Map<String, String>> functions = new ArrayList<Map<String, String>>();
        Script script = null;
        final Map<String, Map<String, String>> dataSources = new HashMap<String, Map<String, String>>();
        final NodeList dataConfigTags = xmlDocument.getElementsByTagName("dataConfig");
        if (dataConfigTags == null || dataConfigTags.getLength() == 0) {
            throw new DataImportHandlerException(500, "the root node '<dataConfig>' is missing");
        }
        final Element e = (Element)dataConfigTags.item(0);
        final List<Element> documentTags = ConfigParseUtil.getChildNodes(e, "document");
        if (documentTags.isEmpty()) {
            throw new DataImportHandlerException(500, "DataImportHandler configuration file must have one <document> node.");
        }
        final List<Element> scriptTags = ConfigParseUtil.getChildNodes(e, "script");
        if (!scriptTags.isEmpty()) {
            script = new Script(scriptTags.get(0));
        }
        final List<Element> functionTags = ConfigParseUtil.getChildNodes(e, "function");
        if (!functionTags.isEmpty()) {
            for (final Element element : functionTags) {
                final String func = ConfigParseUtil.getStringAttribute(element, "name", null);
                final String clz = ConfigParseUtil.getStringAttribute(element, "class", null);
                if (func == null || clz == null) {
                    throw new DataImportHandlerException(500, "<function> must have a 'name' and 'class' attributes");
                }
                functions.add(ConfigParseUtil.getAllAttributes(element));
            }
        }
        final List<Element> dataSourceTags = ConfigParseUtil.getChildNodes(e, "dataSource");
        if (!dataSourceTags.isEmpty()) {
            for (final Element element2 : dataSourceTags) {
                final Map<String, String> p = new HashMap<String, String>();
                final HashMap<String, String> attrs = ConfigParseUtil.getAllAttributes(element2);
                for (final Map.Entry<String, String> entry : attrs.entrySet()) {
                    p.put(entry.getKey(), entry.getValue());
                }
                dataSources.put(p.get("name"), p);
            }
        }
        if (dataSources.get(null) == null) {
            final Iterator i$2 = dataSources.values().iterator();
            if (i$2.hasNext()) {
                final Map<String, String> properties = i$2.next();
                dataSources.put(null, properties);
            }
        }
        PropertyWriter pw = null;
        final List<Element> propertyWriterTags = ConfigParseUtil.getChildNodes(e, "propertyWriter");
        if (propertyWriterTags.isEmpty()) {
            boolean zookeeper = false;
            if (this.core != null && this.core.getCoreDescriptor().getCoreContainer().isZooKeeperAware()) {
                zookeeper = true;
            }
            pw = new PropertyWriter(zookeeper ? "ZKPropertiesWriter" : "SimplePropertiesWriter", Collections.emptyMap());
        }
        else {
            if (propertyWriterTags.size() > 1) {
                throw new DataImportHandlerException(500, "Only one propertyWriter can be configured.");
            }
            final Element pwElement = propertyWriterTags.get(0);
            String type = null;
            final Map<String, String> params = new HashMap<String, String>();
            for (final Map.Entry<String, String> entry2 : ConfigParseUtil.getAllAttributes(pwElement).entrySet()) {
                if ("type".equals(entry2.getKey())) {
                    type = entry2.getValue();
                }
                else {
                    params.put(entry2.getKey(), entry2.getValue());
                }
            }
            if (type == null) {
                throw new DataImportHandlerException(500, "The propertyWriter element must specify type");
            }
            pw = new PropertyWriter(type, params);
        }
        return new DIHConfiguration(documentTags.get(0), this, functions, script, dataSources, pw);
    }
    
    private DIHProperties createPropertyWriter() {
        DIHProperties propWriter = null;
        final PropertyWriter configPw = this.config.getPropertyWriter();
        try {
            final Class<DIHProperties> writerClass = (Class<DIHProperties>)DocBuilder.loadClass(configPw.getType(), this.core);
            propWriter = writerClass.newInstance();
            propWriter.init(this, configPw.getParameters());
        }
        catch (Exception e) {
            throw new DataImportHandlerException(500, "Unable to PropertyWriter implementation:" + configPw.getType(), e);
        }
        return propWriter;
    }
    
    public DIHConfiguration getConfig() {
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
    
    public DataSource getDataSourceInstance(final Entity key, final String name, final Context ctx) {
        Map<String, String> p = this.requestLevelDataSourceProps.get(name);
        if (p == null) {
            p = this.config.getDataSources().get(name);
        }
        if (p == null) {
            p = this.requestLevelDataSourceProps.get(null);
        }
        if (p == null) {
            p = this.config.getDataSources().get(null);
        }
        if (p == null) {
            throw new DataImportHandlerException(500, "No dataSource :" + name + " available for entity :" + key.getName());
        }
        final String type = p.get("type");
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
            DataImportHandlerException.wrapAndThrow(500, e, "Failed to initialize DataSource: " + key.getDataSourceName());
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
    
    public void doFullImport(final DIHWriter writer, final RequestInfo requestParams) {
        DataImporter.LOG.info("Starting Full Import");
        this.setStatus(Status.RUNNING_FULL_DUMP);
        try {
            final DIHProperties dihPropWriter = this.createPropertyWriter();
            this.setIndexStartTime(dihPropWriter.getCurrentTimestamp());
            this.docBuilder = new DocBuilder(this, writer, dihPropWriter, requestParams);
            this.checkWritablePersistFile(writer, dihPropWriter);
            this.docBuilder.execute();
            if (!requestParams.isDebug()) {
                this.cumulativeStatistics.add(this.docBuilder.importStatistics);
            }
        }
        catch (Exception e) {
            SolrException.log(DataImporter.LOG, "Full Import failed", (Throwable)e);
            this.docBuilder.handleError("Full Import failed", e);
        }
        finally {
            this.setStatus(Status.IDLE);
            DocBuilder.INSTANCE.set(null);
        }
    }
    
    private void checkWritablePersistFile(final DIHWriter writer, final DIHProperties dihPropWriter) {
        if (this.isDeltaImportSupported && !dihPropWriter.isWritable()) {
            throw new DataImportHandlerException(500, "Properties is not writable. Delta imports are supported by data config but will not work.");
        }
    }
    
    public void doDeltaImport(final DIHWriter writer, final RequestInfo requestParams) {
        DataImporter.LOG.info("Starting Delta Import");
        this.setStatus(Status.RUNNING_DELTA_DUMP);
        try {
            final DIHProperties dihPropWriter = this.createPropertyWriter();
            this.setIndexStartTime(dihPropWriter.getCurrentTimestamp());
            this.docBuilder = new DocBuilder(this, writer, dihPropWriter, requestParams);
            this.checkWritablePersistFile(writer, dihPropWriter);
            this.docBuilder.execute();
            if (!requestParams.isDebug()) {
                this.cumulativeStatistics.add(this.docBuilder.importStatistics);
            }
        }
        catch (Exception e) {
            DataImporter.LOG.error("Delta Import Failed", (Throwable)e);
            this.docBuilder.handleError("Delta Import Failed", e);
        }
        finally {
            this.setStatus(Status.IDLE);
            DocBuilder.INSTANCE.set(null);
        }
    }
    
    public void runAsync(final RequestInfo reqParams, final DIHWriter sw) {
        new Thread() {
            @Override
            public void run() {
                DataImporter.this.runCmd(reqParams, sw);
            }
        }.start();
    }
    
    void runCmd(final RequestInfo reqParams, final DIHWriter sw) {
        final String command = reqParams.getCommand();
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
    
    public DocBuilder getDocBuilder() {
        return this.docBuilder;
    }
    
    public DocBuilder getDocBuilder(final DIHWriter writer, final RequestInfo requestParams) {
        final DIHProperties dihPropWriter = this.createPropertyWriter();
        return new DocBuilder(this, writer, dihPropWriter, requestParams);
    }
    
    Map<String, Evaluator> getEvaluators() {
        return this.getEvaluators(this.config.getFunctions());
    }
    
    Map<String, Evaluator> getEvaluators(final List<Map<String, String>> fn) {
        final Map<String, Evaluator> evaluators = new HashMap<String, Evaluator>();
        evaluators.put("formatDate", new DateFormatEvaluator());
        evaluators.put("escapeSql", new SqlEscapingEvaluator());
        evaluators.put("encodeUrl", new UrlEvaluator());
        evaluators.put("escapeQueryChars", new SolrQueryEscapingEvaluator());
        final SolrCore core = (this.docBuilder == null) ? null : this.docBuilder.dataImporter.getCore();
        for (final Map<String, String> map : fn) {
            try {
                evaluators.put(map.get("name"), DocBuilder.loadClass(map.get("class"), core).newInstance());
            }
            catch (Exception e) {
                DataImportHandlerException.wrapAndThrow(500, e, "Unable to instantiate evaluator: " + map.get("class"));
            }
        }
        return evaluators;
    }
    
    public SolrCore getCore() {
        return this.core;
    }
    
    void putToCoreScopeSession(final String key, final Object val) {
        this.coreScopeSession.put(key, val);
    }
    
    Object getFromCoreScopeSession(final String key) {
        return this.coreScopeSession.get(key);
    }
    
    static {
        LOG = LoggerFactory.getLogger((Class)MethodHandles.lookup().lookupClass());
        XMLLOG = new XMLErrorLogger(DataImporter.LOG);
        QUERY_COUNT = new ThreadLocal<AtomicLong>() {
            @Override
            protected AtomicLong initialValue() {
                return new AtomicLong();
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
        public static final String CONFIG_NOT_RELOADED = "Configuration NOT Re-loaded...Data Importer is busy.";
        public static final String TOTAL_DOC_PROCESSED = "Total Documents Processed";
        public static final String TOTAL_FAILED_DOCS = "Total Documents Failed";
        public static final String TOTAL_QUERIES_EXECUTED = "Total Requests made to DataSource";
        public static final String TOTAL_ROWS_EXECUTED = "Total Rows Fetched";
        public static final String TOTAL_DOCS_DELETED = "Total Documents Deleted";
        public static final String TOTAL_DOCS_SKIPPED = "Total Documents Skipped";
    }
}
