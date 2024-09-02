// 
// Decompiled by Procyon v0.5.36
// 

package org.postgresql.jdbc;

import java.util.Hashtable;
import org.postgresql.core.ResultCursor;
import java.util.List;
import org.postgresql.core.Field;
import java.sql.Savepoint;
import java.util.concurrent.Executor;
import java.sql.SQLClientInfoException;
import java.sql.ClientInfoStatus;
import java.util.HashMap;
import java.sql.Struct;
import java.sql.NClob;
import java.sql.SQLXML;
import java.sql.Clob;
import java.sql.Blob;
import java.lang.reflect.Array;
import java.util.TimerTask;
import org.postgresql.PGNotification;
import java.io.IOException;
import org.postgresql.core.Encoding;
import java.util.NoSuchElementException;
import java.util.Locale;
import org.postgresql.core.ResultHandler;
import org.postgresql.core.ParameterList;
import java.util.Enumeration;
import org.postgresql.util.PGInterval;
import org.postgresql.util.PGmoney;
import org.postgresql.geometric.PGpolygon;
import org.postgresql.geometric.PGpoint;
import org.postgresql.geometric.PGpath;
import org.postgresql.geometric.PGlseg;
import org.postgresql.geometric.PGline;
import org.postgresql.geometric.PGcircle;
import org.postgresql.geometric.PGbox;
import org.postgresql.util.PGBinaryObject;
import org.postgresql.util.PGobject;
import org.postgresql.core.BaseStatement;
import java.sql.ResultSet;
import org.postgresql.core.QueryExecutor;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Iterator;
import org.postgresql.core.Oid;
import java.util.StringTokenizer;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;
import org.postgresql.util.GT;
import java.util.Collection;
import org.postgresql.core.Version;
import org.postgresql.core.ServerVersion;
import java.util.HashSet;
import org.postgresql.core.Utils;
import org.postgresql.core.ConnectionFactory;
import org.postgresql.PGProperty;
import org.postgresql.Driver;
import org.postgresql.util.HostSpec;
import java.sql.SQLException;
import org.postgresql.copy.CopyManager;
import java.sql.DatabaseMetaData;
import org.postgresql.largeobject.LargeObjectManager;
import org.postgresql.fastpath.Fastpath;
import java.util.Map;
import org.postgresql.core.CachedQuery;
import org.postgresql.util.LruCache;
import java.util.Timer;
import java.util.Set;
import java.sql.SQLWarning;
import org.postgresql.core.TypeInfo;
import org.postgresql.core.Query;
import org.postgresql.core.ProtocolConnection;
import org.postgresql.core.Logger;
import java.util.Properties;
import java.sql.SQLPermission;
import org.postgresql.core.BaseConnection;

public class PgConnection implements BaseConnection
{
    private static final SQLPermission SQL_PERMISSION_ABORT;
    private static int nextConnectionID;
    private final Properties _clientInfo;
    private final Logger logger;
    private final String creatingURL;
    private Throwable openStackTrace;
    private final ProtocolConnection protoConnection;
    private final int compatibleInt;
    private final Query commitQuery;
    private final Query rollbackQuery;
    private TypeInfo _typeCache;
    private boolean disableColumnSanitiser;
    protected int prepareThreshold;
    protected int defaultFetchSize;
    protected boolean forcebinary;
    private int rsHoldability;
    private int savepointId;
    private boolean autoCommit;
    private boolean readOnly;
    private final boolean bindStringAsVarchar;
    private SQLWarning firstWarning;
    private Set<Integer> useBinarySendForOids;
    private Set<Integer> useBinaryReceiveForOids;
    private volatile Timer cancelTimer;
    private final LruCache<Object, CachedQuery> statementCache;
    private final TimestampUtils timestampUtils;
    protected Map typemap;
    private Fastpath fastpath;
    private LargeObjectManager largeobject;
    protected DatabaseMetaData metadata;
    private CopyManager copyManager;
    
    CachedQuery borrowQuery(final String sql, final boolean isCallable) throws SQLException {
        final Object key = isCallable ? new CallableQueryKey(sql) : sql;
        synchronized (this.statementCache) {
            return this.statementCache.borrow(key);
        }
    }
    
    void releaseQuery(final CachedQuery cachedQuery) {
        synchronized (this.statementCache) {
            this.statementCache.put(cachedQuery.key, cachedQuery);
        }
    }
    
    public PgConnection(final HostSpec[] hostSpecs, final String user, final String database, final Properties info, final String url) throws SQLException {
        this.disableColumnSanitiser = false;
        this.forcebinary = false;
        this.rsHoldability = 2;
        this.savepointId = 0;
        this.autoCommit = true;
        this.readOnly = false;
        this.firstWarning = null;
        this.cancelTimer = null;
        this.fastpath = null;
        this.largeobject = null;
        this.copyManager = null;
        this.creatingURL = url;
        int logLevel = Driver.getLogLevel();
        final String connectionLogLevel = PGProperty.LOG_LEVEL.getSetString(info);
        if (connectionLogLevel != null) {
            try {
                logLevel = Integer.parseInt(connectionLogLevel);
            }
            catch (NumberFormatException ex) {}
        }
        synchronized (PgConnection.class) {
            (this.logger = new Logger(PgConnection.nextConnectionID++)).setLogLevel(logLevel);
        }
        this.setDefaultFetchSize(PGProperty.DEFAULT_ROW_FETCH_SIZE.getInt(info));
        this.prepareThreshold = PGProperty.PREPARE_THRESHOLD.getInt(info);
        if (this.prepareThreshold == -1) {
            this.forcebinary = true;
        }
        final boolean binaryTransfer = PGProperty.BINARY_TRANSFER.getBoolean(info);
        if (this.logger.logInfo()) {
            this.logger.info(Driver.getVersion());
        }
        this.protoConnection = ConnectionFactory.openConnection(hostSpecs, user, database, info, this.logger);
        int compat = Utils.parseServerVersionStr(PGProperty.COMPATIBLE.get(info));
        if (compat == 0) {
            compat = 90400;
        }
        this.compatibleInt = compat;
        if (PGProperty.READ_ONLY.getBoolean(info)) {
            this.setReadOnly(true);
        }
        final Set<Integer> binaryOids = new HashSet<Integer>();
        if (binaryTransfer && this.protoConnection.getProtocolVersion() >= 3) {
            binaryOids.add(17);
            binaryOids.add(21);
            binaryOids.add(23);
            binaryOids.add(20);
            binaryOids.add(700);
            binaryOids.add(701);
            binaryOids.add(1083);
            binaryOids.add(1082);
            binaryOids.add(1266);
            binaryOids.add(1114);
            binaryOids.add(1184);
            binaryOids.add(1005);
            binaryOids.add(1007);
            binaryOids.add(1016);
            binaryOids.add(1021);
            binaryOids.add(1022);
            binaryOids.add(1022);
            binaryOids.add(1015);
            binaryOids.add(1009);
            binaryOids.add(600);
            binaryOids.add(603);
            binaryOids.add(2950);
        }
        if (!this.haveMinimumCompatibleVersion(ServerVersion.v8_0)) {
            binaryOids.remove(1083);
            binaryOids.remove(1266);
            binaryOids.remove(1114);
            binaryOids.remove(1184);
        }
        if (!this.haveMinimumCompatibleVersion(ServerVersion.v8_3)) {
            binaryOids.remove(1005);
            binaryOids.remove(1007);
            binaryOids.remove(1016);
            binaryOids.remove(1021);
            binaryOids.remove(1022);
            binaryOids.remove(1022);
            binaryOids.remove(1015);
            binaryOids.remove(1009);
        }
        binaryOids.addAll(this.getOidSet(PGProperty.BINARY_TRANSFER_ENABLE.get(info)));
        binaryOids.removeAll(this.getOidSet(PGProperty.BINARY_TRANSFER_DISABLE.get(info)));
        (this.useBinarySendForOids = new HashSet<Integer>()).addAll(binaryOids);
        (this.useBinaryReceiveForOids = new HashSet<Integer>()).addAll(binaryOids);
        this.useBinarySendForOids.remove(1082);
        this.protoConnection.setBinaryReceiveOids(this.useBinaryReceiveForOids);
        if (this.logger.logDebug()) {
            this.logger.debug("    compatible = " + this.compatibleInt);
            this.logger.debug("    loglevel = " + logLevel);
            this.logger.debug("    prepare threshold = " + this.prepareThreshold);
            this.logger.debug("    types using binary send = " + this.oidsToString(this.useBinarySendForOids));
            this.logger.debug("    types using binary receive = " + this.oidsToString(this.useBinaryReceiveForOids));
            this.logger.debug("    integer date/time = " + this.protoConnection.getIntegerDateTimes());
        }
        final String stringType = PGProperty.STRING_TYPE.get(info);
        if (stringType != null) {
            if (stringType.equalsIgnoreCase("unspecified")) {
                this.bindStringAsVarchar = false;
            }
            else {
                if (!stringType.equalsIgnoreCase("varchar")) {
                    throw new PSQLException(GT.tr("Unsupported value for stringtype parameter: {0}", stringType), PSQLState.INVALID_PARAMETER_VALUE);
                }
                this.bindStringAsVarchar = true;
            }
        }
        else {
            this.bindStringAsVarchar = this.haveMinimumCompatibleVersion(ServerVersion.v8_0);
        }
        this.timestampUtils = new TimestampUtils(this.haveMinimumServerVersion(ServerVersion.v7_4), this.haveMinimumServerVersion(ServerVersion.v8_2), !this.protoConnection.getIntegerDateTimes());
        this.commitQuery = this.getQueryExecutor().createSimpleQuery("COMMIT");
        this.rollbackQuery = this.getQueryExecutor().createSimpleQuery("ROLLBACK");
        final int unknownLength = PGProperty.UNKNOWN_LENGTH.getInt(info);
        this._typeCache = this.createTypeInfo(this, unknownLength);
        this.initObjectTypes(info);
        if (PGProperty.LOG_UNCLOSED_CONNECTIONS.getBoolean(info)) {
            this.openStackTrace = new Throwable("Connection was created at this point:");
        }
        this.disableColumnSanitiser = PGProperty.DISABLE_COLUMN_SANITISER.getBoolean(info);
        this.statementCache = new LruCache<Object, CachedQuery>(Math.max(0, PGProperty.PREPARED_STATEMENT_CACHE_QUERIES.getInt(info)), Math.max(0, PGProperty.PREPARED_STATEMENT_CACHE_SIZE_MIB.getInt(info) * 1024 * 1024), new CachedQueryCreateAction(this, this.protoConnection.getServerVersionNum()), new LruCache.EvictAction<CachedQuery>() {
            public void evict(final CachedQuery cachedQuery) throws SQLException {
                cachedQuery.query.close();
            }
        });
        final TypeInfo types1 = this.getTypeInfo();
        if (this.haveMinimumServerVersion(ServerVersion.v8_3)) {
            types1.addCoreType("uuid", 2950, 1111, "java.util.UUID", 2951);
        }
        final TypeInfo types2 = this.getTypeInfo();
        if (this.haveMinimumServerVersion(ServerVersion.v8_3)) {
            types2.addCoreType("xml", 142, 2009, "java.sql.SQLXML", 143);
        }
        this._clientInfo = new Properties();
        if (this.haveMinimumServerVersion(ServerVersion.v9_0)) {
            String appName = PGProperty.APPLICATION_NAME.get(info);
            if (appName == null) {
                appName = "";
            }
            ((Hashtable<String, String>)this._clientInfo).put("ApplicationName", appName);
        }
    }
    
    private Set<Integer> getOidSet(final String oidList) throws PSQLException {
        final Set oids = new HashSet();
        final StringTokenizer tokenizer = new StringTokenizer(oidList, ",");
        while (tokenizer.hasMoreTokens()) {
            final String oid = tokenizer.nextToken();
            oids.add(Oid.valueOf(oid));
        }
        return (Set<Integer>)oids;
    }
    
    private String oidsToString(final Set<Integer> oids) {
        final StringBuilder sb = new StringBuilder();
        for (final Integer oid : oids) {
            sb.append(Oid.toString(oid));
            sb.append(',');
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        else {
            sb.append(" <none>");
        }
        return sb.toString();
    }
    
    public TimestampUtils getTimestampUtils() {
        return this.timestampUtils;
    }
    
    public Statement createStatement() throws SQLException {
        return this.createStatement(1003, 1007);
    }
    
    public PreparedStatement prepareStatement(final String sql) throws SQLException {
        return this.prepareStatement(sql, 1003, 1007);
    }
    
    public CallableStatement prepareCall(final String sql) throws SQLException {
        return this.prepareCall(sql, 1003, 1007);
    }
    
    public Map getTypeMap() throws SQLException {
        this.checkClosed();
        return this.typemap;
    }
    
    public QueryExecutor getQueryExecutor() {
        return this.protoConnection.getQueryExecutor();
    }
    
    public void addWarning(final SQLWarning warn) {
        if (this.firstWarning != null) {
            this.firstWarning.setNextWarning(warn);
        }
        else {
            this.firstWarning = warn;
        }
    }
    
    public ResultSet execSQLQuery(final String s) throws SQLException {
        return this.execSQLQuery(s, 1003, 1007);
    }
    
    public ResultSet execSQLQuery(final String s, final int resultSetType, final int resultSetConcurrency) throws SQLException {
        BaseStatement stat;
        boolean hasResultSet;
        for (stat = (BaseStatement)this.createStatement(resultSetType, resultSetConcurrency), hasResultSet = stat.executeWithFlags(s, 16); !hasResultSet && stat.getUpdateCount() != -1; hasResultSet = stat.getMoreResults()) {}
        if (!hasResultSet) {
            throw new PSQLException(GT.tr("No results were returned by the query."), PSQLState.NO_DATA);
        }
        final SQLWarning warnings = stat.getWarnings();
        if (warnings != null) {
            this.addWarning(warnings);
        }
        return stat.getResultSet();
    }
    
    public void execSQLUpdate(final String s) throws SQLException {
        final BaseStatement stmt = (BaseStatement)this.createStatement();
        if (stmt.executeWithFlags(s, 22)) {
            throw new PSQLException(GT.tr("A result was returned when none was expected."), PSQLState.TOO_MANY_RESULTS);
        }
        final SQLWarning warnings = stmt.getWarnings();
        if (warnings != null) {
            this.addWarning(warnings);
        }
        stmt.close();
    }
    
    public void setCursorName(final String cursor) throws SQLException {
        this.checkClosed();
    }
    
    public String getCursorName() throws SQLException {
        this.checkClosed();
        return null;
    }
    
    public String getURL() throws SQLException {
        return this.creatingURL;
    }
    
    public String getUserName() throws SQLException {
        return this.protoConnection.getUser();
    }
    
    public Fastpath getFastpathAPI() throws SQLException {
        this.checkClosed();
        if (this.fastpath == null) {
            this.fastpath = new Fastpath(this);
        }
        return this.fastpath;
    }
    
    public LargeObjectManager getLargeObjectAPI() throws SQLException {
        this.checkClosed();
        if (this.largeobject == null) {
            this.largeobject = new LargeObjectManager(this);
        }
        return this.largeobject;
    }
    
    public Object getObject(final String type, final String value, final byte[] byteValue) throws SQLException {
        if (this.typemap != null) {
            final Class c = this.typemap.get(type);
            if (c != null) {
                throw new PSQLException(GT.tr("Custom type maps are not supported."), PSQLState.NOT_IMPLEMENTED);
            }
        }
        PGobject obj = null;
        if (this.logger.logDebug()) {
            this.logger.debug("Constructing object from type=" + type + " value=<" + value + ">");
        }
        try {
            final Class klass = this._typeCache.getPGobject(type);
            if (klass != null) {
                obj = klass.newInstance();
                obj.setType(type);
                if (byteValue != null && obj instanceof PGBinaryObject) {
                    final PGBinaryObject binObj = (PGBinaryObject)obj;
                    binObj.setByteValue(byteValue, 0);
                }
                else {
                    obj.setValue(value);
                }
            }
            else {
                obj = new PGobject();
                obj.setType(type);
                obj.setValue(value);
            }
            return obj;
        }
        catch (SQLException sx) {
            throw sx;
        }
        catch (Exception ex) {
            throw new PSQLException(GT.tr("Failed to create object for: {0}.", type), PSQLState.CONNECTION_FAILURE, ex);
        }
    }
    
    protected TypeInfo createTypeInfo(final BaseConnection conn, final int unknownLength) {
        return new TypeInfoCache(conn, unknownLength);
    }
    
    public TypeInfo getTypeInfo() {
        return this._typeCache;
    }
    
    public void addDataType(final String type, final String name) {
        try {
            this.addDataType(type, Class.forName(name));
        }
        catch (Exception e) {
            throw new RuntimeException("Cannot register new type: " + e);
        }
    }
    
    public void addDataType(final String type, final Class klass) throws SQLException {
        this.checkClosed();
        this._typeCache.addDataType(type, klass);
    }
    
    private void initObjectTypes(final Properties info) throws SQLException {
        this.addDataType("box", PGbox.class);
        this.addDataType("circle", PGcircle.class);
        this.addDataType("line", PGline.class);
        this.addDataType("lseg", PGlseg.class);
        this.addDataType("path", PGpath.class);
        this.addDataType("point", PGpoint.class);
        this.addDataType("polygon", PGpolygon.class);
        this.addDataType("money", PGmoney.class);
        this.addDataType("interval", PGInterval.class);
        final Enumeration e = info.propertyNames();
        while (e.hasMoreElements()) {
            final String propertyName = e.nextElement();
            if (propertyName.startsWith("datatype.")) {
                final String typeName = propertyName.substring(9);
                final String className = info.getProperty(propertyName);
                Class klass;
                try {
                    klass = Class.forName(className);
                }
                catch (ClassNotFoundException cnfe) {
                    throw new PSQLException(GT.tr("Unable to load the class {0} responsible for the datatype {1}", new Object[] { className, typeName }), PSQLState.SYSTEM_ERROR, cnfe);
                }
                this.addDataType(typeName, klass);
            }
        }
    }
    
    public void close() throws SQLException {
        this.releaseTimer();
        this.protoConnection.close();
        this.openStackTrace = null;
    }
    
    public String nativeSQL(final String sql) throws SQLException {
        this.checkClosed();
        final StringBuilder buf = new StringBuilder(sql.length());
        PgStatement.parseSql(sql, 0, buf, false, this.getStandardConformingStrings());
        return buf.toString();
    }
    
    public synchronized SQLWarning getWarnings() throws SQLException {
        this.checkClosed();
        final SQLWarning newWarnings = this.protoConnection.getWarnings();
        if (this.firstWarning == null) {
            this.firstWarning = newWarnings;
        }
        else {
            this.firstWarning.setNextWarning(newWarnings);
        }
        return this.firstWarning;
    }
    
    public synchronized void clearWarnings() throws SQLException {
        this.checkClosed();
        this.protoConnection.getWarnings();
        this.firstWarning = null;
    }
    
    public void setReadOnly(final boolean readOnly) throws SQLException {
        this.checkClosed();
        if (this.protoConnection.getTransactionState() != 0) {
            throw new PSQLException(GT.tr("Cannot change transaction read-only property in the middle of a transaction."), PSQLState.ACTIVE_SQL_TRANSACTION);
        }
        if (this.haveMinimumServerVersion(ServerVersion.v7_4) && readOnly != this.readOnly) {
            final String readOnlySql = "SET SESSION CHARACTERISTICS AS TRANSACTION " + (readOnly ? "READ ONLY" : "READ WRITE");
            this.execSQLUpdate(readOnlySql);
        }
        this.readOnly = readOnly;
    }
    
    public boolean isReadOnly() throws SQLException {
        this.checkClosed();
        return this.readOnly;
    }
    
    public void setAutoCommit(final boolean autoCommit) throws SQLException {
        this.checkClosed();
        if (this.autoCommit == autoCommit) {
            return;
        }
        if (!this.autoCommit) {
            this.commit();
        }
        this.autoCommit = autoCommit;
    }
    
    public boolean getAutoCommit() throws SQLException {
        this.checkClosed();
        return this.autoCommit;
    }
    
    private void executeTransactionCommand(final Query query) throws SQLException {
        int flags = 22;
        if (this.prepareThreshold == 0) {
            flags |= 0x1;
        }
        this.getQueryExecutor().execute(query, null, new TransactionCommandHandler(), 0, 0, flags);
    }
    
    public void commit() throws SQLException {
        this.checkClosed();
        if (this.autoCommit) {
            throw new PSQLException(GT.tr("Cannot commit when autoCommit is enabled."), PSQLState.NO_ACTIVE_SQL_TRANSACTION);
        }
        if (this.protoConnection.getTransactionState() != 0) {
            this.executeTransactionCommand(this.commitQuery);
        }
    }
    
    protected void checkClosed() throws SQLException {
        if (this.isClosed()) {
            throw new PSQLException(GT.tr("This connection has been closed."), PSQLState.CONNECTION_DOES_NOT_EXIST);
        }
    }
    
    public void rollback() throws SQLException {
        this.checkClosed();
        if (this.autoCommit) {
            throw new PSQLException(GT.tr("Cannot rollback when autoCommit is enabled."), PSQLState.NO_ACTIVE_SQL_TRANSACTION);
        }
        if (this.protoConnection.getTransactionState() != 0) {
            this.executeTransactionCommand(this.rollbackQuery);
        }
    }
    
    public int getTransactionState() {
        return this.protoConnection.getTransactionState();
    }
    
    public int getTransactionIsolation() throws SQLException {
        this.checkClosed();
        String level = null;
        if (this.haveMinimumServerVersion(ServerVersion.v7_3)) {
            final ResultSet rs = this.execSQLQuery("SHOW TRANSACTION ISOLATION LEVEL");
            if (rs.next()) {
                level = rs.getString(1);
            }
            rs.close();
        }
        else {
            final SQLWarning saveWarnings = this.getWarnings();
            this.clearWarnings();
            this.execSQLUpdate("SHOW TRANSACTION ISOLATION LEVEL");
            final SQLWarning warning = this.getWarnings();
            if (warning != null) {
                level = warning.getMessage();
            }
            this.clearWarnings();
            if (saveWarnings != null) {
                this.addWarning(saveWarnings);
            }
        }
        if (level == null) {
            return 2;
        }
        level = level.toUpperCase(Locale.US);
        if (level.contains("READ COMMITTED")) {
            return 2;
        }
        if (level.contains("READ UNCOMMITTED")) {
            return 1;
        }
        if (level.contains("REPEATABLE READ")) {
            return 4;
        }
        if (level.contains("SERIALIZABLE")) {
            return 8;
        }
        return 2;
    }
    
    public void setTransactionIsolation(final int level) throws SQLException {
        this.checkClosed();
        if (this.protoConnection.getTransactionState() != 0) {
            throw new PSQLException(GT.tr("Cannot change transaction isolation level in the middle of a transaction."), PSQLState.ACTIVE_SQL_TRANSACTION);
        }
        final String isolationLevelName = this.getIsolationLevelName(level);
        if (isolationLevelName == null) {
            throw new PSQLException(GT.tr("Transaction isolation level {0} not supported.", level), PSQLState.NOT_IMPLEMENTED);
        }
        final String isolationLevelSQL = "SET SESSION CHARACTERISTICS AS TRANSACTION ISOLATION LEVEL " + isolationLevelName;
        this.execSQLUpdate(isolationLevelSQL);
    }
    
    protected String getIsolationLevelName(final int level) {
        final boolean pg80 = this.haveMinimumServerVersion(ServerVersion.v8_0);
        if (level == 2) {
            return "READ COMMITTED";
        }
        if (level == 8) {
            return "SERIALIZABLE";
        }
        if (pg80 && level == 1) {
            return "READ UNCOMMITTED";
        }
        if (pg80 && level == 4) {
            return "REPEATABLE READ";
        }
        return null;
    }
    
    public void setCatalog(final String catalog) throws SQLException {
        this.checkClosed();
    }
    
    public String getCatalog() throws SQLException {
        this.checkClosed();
        return this.protoConnection.getDatabase();
    }
    
    @Override
    protected void finalize() throws Throwable {
        try {
            if (this.openStackTrace != null) {
                this.logger.log(GT.tr("Finalizing a Connection that was never closed:"), this.openStackTrace);
            }
            this.close();
        }
        finally {
            super.finalize();
        }
    }
    
    public String getDBVersionNumber() {
        return this.protoConnection.getServerVersion();
    }
    
    public int getServerMajorVersion() {
        try {
            final StringTokenizer versionTokens = new StringTokenizer(this.protoConnection.getServerVersion(), ".");
            return integerPart(versionTokens.nextToken());
        }
        catch (NoSuchElementException e) {
            return 0;
        }
    }
    
    public int getServerMinorVersion() {
        try {
            final StringTokenizer versionTokens = new StringTokenizer(this.protoConnection.getServerVersion(), ".");
            versionTokens.nextToken();
            return integerPart(versionTokens.nextToken());
        }
        catch (NoSuchElementException e) {
            return 0;
        }
    }
    
    public boolean haveMinimumServerVersion(final String ver) {
        final int requiredver = Utils.parseServerVersionStr(ver);
        if (requiredver == 0) {
            return this.protoConnection.getServerVersion().compareTo(ver) >= 0;
        }
        return this.haveMinimumServerVersion(requiredver);
    }
    
    public boolean haveMinimumServerVersion(final int ver) {
        return this.protoConnection.getServerVersionNum() >= ver;
    }
    
    public boolean haveMinimumServerVersion(final Version ver) {
        return this.haveMinimumServerVersion(ver.getVersionNum());
    }
    
    public boolean haveMinimumCompatibleVersion(final int ver) {
        return this.compatibleInt >= ver;
    }
    
    public boolean haveMinimumCompatibleVersion(final String ver) {
        return this.haveMinimumCompatibleVersion(ServerVersion.from(ver));
    }
    
    public boolean haveMinimumCompatibleVersion(final Version ver) {
        return this.haveMinimumCompatibleVersion(ver.getVersionNum());
    }
    
    public Encoding getEncoding() {
        return this.protoConnection.getEncoding();
    }
    
    public byte[] encodeString(final String str) throws SQLException {
        try {
            return this.getEncoding().encode(str);
        }
        catch (IOException ioe) {
            throw new PSQLException(GT.tr("Unable to translate data into the desired encoding."), PSQLState.DATA_ERROR, ioe);
        }
    }
    
    public String escapeString(final String str) throws SQLException {
        return Utils.escapeLiteral(null, str, this.protoConnection.getStandardConformingStrings()).toString();
    }
    
    public boolean getStandardConformingStrings() {
        return this.protoConnection.getStandardConformingStrings();
    }
    
    public boolean isClosed() throws SQLException {
        return this.protoConnection.isClosed();
    }
    
    public void cancelQuery() throws SQLException {
        this.checkClosed();
        this.protoConnection.sendQueryCancel();
    }
    
    public PGNotification[] getNotifications() throws SQLException {
        this.checkClosed();
        this.getQueryExecutor().processNotifies();
        final PGNotification[] notifications = this.protoConnection.getNotifications();
        return (PGNotification[])((notifications.length == 0) ? null : notifications);
    }
    
    public int getPrepareThreshold() {
        return this.prepareThreshold;
    }
    
    public void setDefaultFetchSize(final int fetchSize) throws SQLException {
        if (fetchSize < 0) {
            throw new PSQLException(GT.tr("Fetch size must be a value greater to or equal to 0."), PSQLState.INVALID_PARAMETER_VALUE);
        }
        this.defaultFetchSize = fetchSize;
    }
    
    public int getDefaultFetchSize() {
        return this.defaultFetchSize;
    }
    
    public void setPrepareThreshold(final int newThreshold) {
        this.prepareThreshold = newThreshold;
    }
    
    public boolean getForceBinary() {
        return this.forcebinary;
    }
    
    public void setForceBinary(final boolean newValue) {
        this.forcebinary = newValue;
    }
    
    public void setTypeMapImpl(final Map map) throws SQLException {
        this.typemap = map;
    }
    
    public Logger getLogger() {
        return this.logger;
    }
    
    public int getProtocolVersion() {
        return this.protoConnection.getProtocolVersion();
    }
    
    public boolean getStringVarcharFlag() {
        return this.bindStringAsVarchar;
    }
    
    public CopyManager getCopyAPI() throws SQLException {
        this.checkClosed();
        if (this.copyManager == null) {
            this.copyManager = new CopyManager(this);
        }
        return this.copyManager;
    }
    
    public boolean binaryTransferSend(final int oid) {
        return this.useBinarySendForOids.contains(oid);
    }
    
    public int getBackendPID() {
        return this.protoConnection.getBackendPID();
    }
    
    public boolean isColumnSanitiserDisabled() {
        return this.disableColumnSanitiser;
    }
    
    public void setDisableColumnSanitiser(final boolean disableColumnSanitiser) {
        this.disableColumnSanitiser = disableColumnSanitiser;
    }
    
    protected void abort() {
        this.protoConnection.abort();
    }
    
    private synchronized Timer getTimer() {
        if (this.cancelTimer == null) {
            this.cancelTimer = Driver.getSharedTimer().getTimer();
        }
        return this.cancelTimer;
    }
    
    private synchronized void releaseTimer() {
        if (this.cancelTimer != null) {
            this.cancelTimer = null;
            Driver.getSharedTimer().releaseTimer();
        }
    }
    
    public void addTimerTask(final TimerTask timerTask, final long milliSeconds) {
        final Timer timer = this.getTimer();
        timer.schedule(timerTask, milliSeconds);
    }
    
    public void purgeTimerTasks() {
        final Timer timer = this.cancelTimer;
        if (timer != null) {
            timer.purge();
        }
    }
    
    public String escapeIdentifier(final String identifier) throws SQLException {
        return Utils.escapeIdentifier(null, identifier).toString();
    }
    
    public String escapeLiteral(final String literal) throws SQLException {
        return Utils.escapeLiteral(null, literal, this.protoConnection.getStandardConformingStrings()).toString();
    }
    
    private static void appendArray(final StringBuilder sb, final Object elements, final char delim) {
        sb.append('{');
        for (int nElements = Array.getLength(elements), i = 0; i < nElements; ++i) {
            if (i > 0) {
                sb.append(delim);
            }
            final Object o = Array.get(elements, i);
            if (o == null) {
                sb.append("NULL");
            }
            else if (o.getClass().isArray()) {
                appendArray(sb, o, delim);
            }
            else {
                final String s = o.toString();
                PgArray.escapeArrayElement(sb, s);
            }
        }
        sb.append('}');
    }
    
    private static int integerPart(final String dirtyString) {
        int start;
        for (start = 0; start < dirtyString.length() && !Character.isDigit(dirtyString.charAt(start)); ++start) {}
        int end;
        for (end = start; end < dirtyString.length() && Character.isDigit(dirtyString.charAt(end)); ++end) {}
        if (start == end) {
            return 0;
        }
        return Integer.parseInt(dirtyString.substring(start, end));
    }
    
    public Statement createStatement(final int resultSetType, final int resultSetConcurrency, final int resultSetHoldability) throws SQLException {
        this.checkClosed();
        return new PgStatement(this, resultSetType, resultSetConcurrency, resultSetHoldability);
    }
    
    public PreparedStatement prepareStatement(final String sql, final int resultSetType, final int resultSetConcurrency, final int resultSetHoldability) throws SQLException {
        this.checkClosed();
        return new PgPreparedStatement(this, sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }
    
    public CallableStatement prepareCall(final String sql, final int resultSetType, final int resultSetConcurrency, final int resultSetHoldability) throws SQLException {
        this.checkClosed();
        return new PgCallableStatement(this, sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }
    
    public DatabaseMetaData getMetaData() throws SQLException {
        this.checkClosed();
        if (this.metadata == null) {
            this.metadata = new PgDatabaseMetaData(this);
        }
        return this.metadata;
    }
    
    public void setTypeMap(final Map<String, Class<?>> map) throws SQLException {
        this.setTypeMapImpl(map);
    }
    
    protected java.sql.Array makeArray(final int oid, final String fieldString) throws SQLException {
        return new PgArray(this, oid, fieldString);
    }
    
    protected Blob makeBlob(final long oid) throws SQLException {
        return new PgBlob(this, oid);
    }
    
    protected Clob makeClob(final long oid) throws SQLException {
        return new PgClob(this, oid);
    }
    
    protected SQLXML makeSQLXML() throws SQLException {
        return new PgSQLXML(this);
    }
    
    public Clob createClob() throws SQLException {
        this.checkClosed();
        throw Driver.notImplemented(this.getClass(), "createClob()");
    }
    
    public Blob createBlob() throws SQLException {
        this.checkClosed();
        throw Driver.notImplemented(this.getClass(), "createBlob()");
    }
    
    public NClob createNClob() throws SQLException {
        this.checkClosed();
        throw Driver.notImplemented(this.getClass(), "createNClob()");
    }
    
    public SQLXML createSQLXML() throws SQLException {
        this.checkClosed();
        return this.makeSQLXML();
    }
    
    public Struct createStruct(final String typeName, final Object[] attributes) throws SQLException {
        this.checkClosed();
        throw Driver.notImplemented(this.getClass(), "createStruct(String, Object[])");
    }
    
    public java.sql.Array createArrayOf(final String typeName, final Object[] elements) throws SQLException {
        this.checkClosed();
        final int oid = this.getTypeInfo().getPGArrayType(typeName);
        if (oid == 0) {
            throw new PSQLException(GT.tr("Unable to find server array type for provided name {0}.", typeName), PSQLState.INVALID_NAME);
        }
        final char delim = this.getTypeInfo().getArrayDelimiter(oid);
        final StringBuilder sb = new StringBuilder();
        appendArray(sb, elements, delim);
        return this.makeArray(oid, sb.toString());
    }
    
    public boolean isValid(final int timeout) throws SQLException {
        if (this.isClosed()) {
            return false;
        }
        if (timeout < 0) {
            throw new PSQLException(GT.tr("Invalid timeout ({0}<0).", timeout), PSQLState.INVALID_PARAMETER_VALUE);
        }
        boolean valid = false;
        Statement stmt = null;
        try {
            if (!this.isClosed()) {
                stmt = this.createStatement();
                stmt.setQueryTimeout(timeout);
                stmt.executeUpdate("");
                valid = true;
            }
        }
        catch (SQLException e) {
            this.getLogger().log(GT.tr("Validating connection."), e);
        }
        finally {
            if (stmt != null) {
                try {
                    stmt.close();
                }
                catch (Exception ex) {}
            }
        }
        return valid;
    }
    
    public void setClientInfo(final String name, String value) throws SQLClientInfoException {
        try {
            this.checkClosed();
        }
        catch (SQLException cause) {
            final Map<String, ClientInfoStatus> failures = new HashMap<String, ClientInfoStatus>();
            failures.put(name, ClientInfoStatus.REASON_UNKNOWN);
            throw new SQLClientInfoException(GT.tr("This connection has been closed."), failures, cause);
        }
        if (this.haveMinimumServerVersion(ServerVersion.v9_0) && "ApplicationName".equals(name)) {
            if (value == null) {
                value = "";
            }
            try {
                final StringBuilder sql = new StringBuilder("SET application_name = '");
                Utils.escapeLiteral(sql, value, this.getStandardConformingStrings());
                sql.append("'");
                this.execSQLUpdate(sql.toString());
            }
            catch (SQLException sqle) {
                final Map<String, ClientInfoStatus> failures = new HashMap<String, ClientInfoStatus>();
                failures.put(name, ClientInfoStatus.REASON_UNKNOWN);
                throw new SQLClientInfoException(GT.tr("Failed to set ClientInfo property: {0}", "ApplicationName"), sqle.getSQLState(), failures, sqle);
            }
            ((Hashtable<String, String>)this._clientInfo).put(name, value);
            return;
        }
        this.addWarning(new SQLWarning(GT.tr("ClientInfo property not supported."), PSQLState.NOT_IMPLEMENTED.getState()));
    }
    
    public void setClientInfo(final Properties properties) throws SQLClientInfoException {
        try {
            this.checkClosed();
        }
        catch (SQLException cause) {
            final Map<String, ClientInfoStatus> failures = new HashMap<String, ClientInfoStatus>();
            for (final Map.Entry<Object, Object> e : properties.entrySet()) {
                failures.put(e.getKey(), ClientInfoStatus.REASON_UNKNOWN);
            }
            throw new SQLClientInfoException(GT.tr("This connection has been closed."), failures, cause);
        }
        final Map<String, ClientInfoStatus> failures2 = new HashMap<String, ClientInfoStatus>();
        for (final String name : new String[] { "ApplicationName" }) {
            try {
                this.setClientInfo(name, properties.getProperty(name, null));
            }
            catch (SQLClientInfoException e2) {
                failures2.putAll(e2.getFailedProperties());
            }
        }
        if (!failures2.isEmpty()) {
            throw new SQLClientInfoException(GT.tr("One ore more ClientInfo failed."), PSQLState.NOT_IMPLEMENTED.getState(), failures2);
        }
    }
    
    public String getClientInfo(final String name) throws SQLException {
        this.checkClosed();
        return this._clientInfo.getProperty(name);
    }
    
    public Properties getClientInfo() throws SQLException {
        this.checkClosed();
        return this._clientInfo;
    }
    
    public <T> T createQueryObject(final Class<T> ifc) throws SQLException {
        this.checkClosed();
        throw Driver.notImplemented(this.getClass(), "createQueryObject(Class<T>)");
    }
    
    public boolean isWrapperFor(final Class<?> iface) throws SQLException {
        this.checkClosed();
        return iface.isAssignableFrom(this.getClass());
    }
    
    public <T> T unwrap(final Class<T> iface) throws SQLException {
        this.checkClosed();
        if (iface.isAssignableFrom(this.getClass())) {
            return iface.cast(this);
        }
        throw new SQLException("Cannot unwrap to " + iface.getName());
    }
    
    public String getSchema() throws SQLException {
        this.checkClosed();
        final Statement stmt = this.createStatement();
        try {
            final ResultSet rs = stmt.executeQuery("select current_schema()");
            try {
                if (!rs.next()) {
                    return null;
                }
                return rs.getString(1);
            }
            finally {
                rs.close();
            }
        }
        finally {
            stmt.close();
        }
    }
    
    public void setSchema(final String schema) throws SQLException {
        this.checkClosed();
        final Statement stmt = this.createStatement();
        try {
            if (schema == null) {
                stmt.executeUpdate("SET SESSION search_path TO DEFAULT");
            }
            else {
                final StringBuilder sb = new StringBuilder();
                sb.append("SET SESSION search_path TO '");
                Utils.escapeLiteral(sb, schema, this.getStandardConformingStrings());
                sb.append("'");
                stmt.executeUpdate(sb.toString());
            }
        }
        finally {
            stmt.close();
        }
    }
    
    public void abort(final Executor executor) throws SQLException {
        if (this.isClosed()) {
            return;
        }
        PgConnection.SQL_PERMISSION_ABORT.checkGuard(this);
        final AbortCommand command = new AbortCommand();
        if (executor != null) {
            executor.execute(command);
        }
        else {
            command.run();
        }
    }
    
    public void setNetworkTimeout(final Executor executor, final int milliseconds) throws SQLException {
        throw Driver.notImplemented(this.getClass(), "setNetworkTimeout(Executor, int)");
    }
    
    public int getNetworkTimeout() throws SQLException {
        throw Driver.notImplemented(this.getClass(), "getNetworkTimeout()");
    }
    
    public void setHoldability(final int holdability) throws SQLException {
        this.checkClosed();
        switch (holdability) {
            case 2: {
                this.rsHoldability = holdability;
                break;
            }
            case 1: {
                this.rsHoldability = holdability;
                break;
            }
            default: {
                throw new PSQLException(GT.tr("Unknown ResultSet holdability setting: {0}.", holdability), PSQLState.INVALID_PARAMETER_VALUE);
            }
        }
    }
    
    public int getHoldability() throws SQLException {
        this.checkClosed();
        return this.rsHoldability;
    }
    
    public Savepoint setSavepoint() throws SQLException {
        this.checkClosed();
        if (!this.haveMinimumServerVersion(ServerVersion.v8_0)) {
            throw new PSQLException(GT.tr("Server versions prior to 8.0 do not support savepoints."), PSQLState.NOT_IMPLEMENTED);
        }
        if (this.getAutoCommit()) {
            throw new PSQLException(GT.tr("Cannot establish a savepoint in auto-commit mode."), PSQLState.NO_ACTIVE_SQL_TRANSACTION);
        }
        final PSQLSavepoint savepoint = new PSQLSavepoint(this.savepointId++);
        final String pgName = savepoint.getPGName();
        final Statement stmt = this.createStatement();
        stmt.executeUpdate("SAVEPOINT " + pgName);
        stmt.close();
        return savepoint;
    }
    
    public Savepoint setSavepoint(final String name) throws SQLException {
        this.checkClosed();
        if (!this.haveMinimumServerVersion(ServerVersion.v8_0)) {
            throw new PSQLException(GT.tr("Server versions prior to 8.0 do not support savepoints."), PSQLState.NOT_IMPLEMENTED);
        }
        if (this.getAutoCommit()) {
            throw new PSQLException(GT.tr("Cannot establish a savepoint in auto-commit mode."), PSQLState.NO_ACTIVE_SQL_TRANSACTION);
        }
        final PSQLSavepoint savepoint = new PSQLSavepoint(name);
        final Statement stmt = this.createStatement();
        stmt.executeUpdate("SAVEPOINT " + savepoint.getPGName());
        stmt.close();
        return savepoint;
    }
    
    public void rollback(final Savepoint savepoint) throws SQLException {
        this.checkClosed();
        if (!this.haveMinimumServerVersion(ServerVersion.v8_0)) {
            throw new PSQLException(GT.tr("Server versions prior to 8.0 do not support savepoints."), PSQLState.NOT_IMPLEMENTED);
        }
        final PSQLSavepoint pgSavepoint = (PSQLSavepoint)savepoint;
        this.execSQLUpdate("ROLLBACK TO SAVEPOINT " + pgSavepoint.getPGName());
    }
    
    public void releaseSavepoint(final Savepoint savepoint) throws SQLException {
        this.checkClosed();
        if (!this.haveMinimumServerVersion(ServerVersion.v8_0)) {
            throw new PSQLException(GT.tr("Server versions prior to 8.0 do not support savepoints."), PSQLState.NOT_IMPLEMENTED);
        }
        final PSQLSavepoint pgSavepoint = (PSQLSavepoint)savepoint;
        this.execSQLUpdate("RELEASE SAVEPOINT " + pgSavepoint.getPGName());
        pgSavepoint.invalidate();
    }
    
    public Statement createStatement(final int resultSetType, final int resultSetConcurrency) throws SQLException {
        this.checkClosed();
        return this.createStatement(resultSetType, resultSetConcurrency, this.getHoldability());
    }
    
    public PreparedStatement prepareStatement(final String sql, final int resultSetType, final int resultSetConcurrency) throws SQLException {
        this.checkClosed();
        return this.prepareStatement(sql, resultSetType, resultSetConcurrency, this.getHoldability());
    }
    
    public CallableStatement prepareCall(final String sql, final int resultSetType, final int resultSetConcurrency) throws SQLException {
        this.checkClosed();
        return this.prepareCall(sql, resultSetType, resultSetConcurrency, this.getHoldability());
    }
    
    public PreparedStatement prepareStatement(String sql, final int autoGeneratedKeys) throws SQLException {
        this.checkClosed();
        if (autoGeneratedKeys != 2) {
            sql = PgStatement.addReturning(this, sql, new String[] { "*" }, false);
        }
        final PreparedStatement ps = this.prepareStatement(sql);
        if (autoGeneratedKeys != 2) {
            ((PgStatement)ps).wantsGeneratedKeysAlways = true;
        }
        return ps;
    }
    
    public PreparedStatement prepareStatement(final String sql, final int[] columnIndexes) throws SQLException {
        if (columnIndexes == null || columnIndexes.length == 0) {
            return this.prepareStatement(sql);
        }
        this.checkClosed();
        throw new PSQLException(GT.tr("Returning autogenerated keys is not supported."), PSQLState.NOT_IMPLEMENTED);
    }
    
    public PreparedStatement prepareStatement(String sql, final String[] columnNames) throws SQLException {
        if (columnNames != null && columnNames.length != 0) {
            sql = PgStatement.addReturning(this, sql, columnNames, true);
        }
        final PreparedStatement ps = this.prepareStatement(sql);
        if (columnNames != null && columnNames.length != 0) {
            ((PgStatement)ps).wantsGeneratedKeysAlways = true;
        }
        return ps;
    }
    
    static {
        SQL_PERMISSION_ABORT = new SQLPermission("callAbort");
        PgConnection.nextConnectionID = 1;
    }
    
    private class TransactionCommandHandler implements ResultHandler
    {
        private SQLException error;
        
        public void handleResultRows(final Query fromQuery, final Field[] fields, final List tuples, final ResultCursor cursor) {
        }
        
        public void handleCommandStatus(final String status, final int updateCount, final long insertOID) {
        }
        
        public void handleWarning(final SQLWarning warning) {
            PgConnection.this.addWarning(warning);
        }
        
        public void handleError(final SQLException newError) {
            if (this.error == null) {
                this.error = newError;
            }
            else {
                this.error.setNextException(newError);
            }
        }
        
        public void handleCompletion() throws SQLException {
            if (this.error != null) {
                throw this.error;
            }
        }
    }
    
    public class AbortCommand implements Runnable
    {
        public void run() {
            PgConnection.this.abort();
        }
    }
}
