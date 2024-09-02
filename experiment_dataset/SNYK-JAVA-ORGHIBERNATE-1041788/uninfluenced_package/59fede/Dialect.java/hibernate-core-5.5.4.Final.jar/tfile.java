// 
// Decompiled by Procyon v0.5.36
// 

package org.hibernate.dialect;

import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.engine.jdbc.LobCreationContext;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import java.io.InputStream;
import java.io.OutputStream;
import org.hibernate.internal.util.io.StreamCopier;
import java.sql.NClob;
import java.sql.Clob;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import java.sql.Blob;
import java.util.regex.Matcher;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.procedure.internal.StandardCallableStatementSupport;
import org.hibernate.procedure.spi.CallableStatementSupport;
import org.hibernate.ScrollMode;
import java.util.List;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.NullPrecedence;
import org.hibernate.engine.jdbc.env.internal.DefaultSchemaNameResolver;
import org.hibernate.engine.jdbc.env.spi.SchemaNameResolver;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.mapping.Constraint;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Index;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.mapping.Table;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.engine.jdbc.env.spi.AnsiSqlKeywords;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import java.sql.DatabaseMetaData;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.sql.ANSICaseFragment;
import org.hibernate.sql.CaseFragment;
import org.hibernate.sql.ANSIJoinFragment;
import org.hibernate.sql.JoinFragment;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.SQLExceptionConverter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.CallableStatement;
import org.hibernate.hql.spi.id.persistent.PersistentTableBulkIdStrategy;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.sql.ForUpdateFragment;
import java.util.Iterator;
import org.hibernate.LockOptions;
import org.hibernate.dialect.lock.SelectLockingStrategy;
import org.hibernate.dialect.lock.OptimisticForceIncrementLockingStrategy;
import org.hibernate.dialect.lock.OptimisticLockingStrategy;
import org.hibernate.dialect.lock.PessimisticReadSelectLockingStrategy;
import org.hibernate.dialect.lock.PessimisticWriteSelectLockingStrategy;
import org.hibernate.dialect.lock.PessimisticForceIncrementLockingStrategy;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.LockMode;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.dialect.pagination.LegacyLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorLegacyImpl;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorNoOpImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.MappingException;
import org.hibernate.dialect.identity.IdentityColumnSupportImpl;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.id.IdentityGenerator;
import java.util.Locale;
import org.hibernate.type.descriptor.sql.ClobTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.HibernateException;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.unique.DefaultUniqueDelegate;
import org.hibernate.dialect.function.CastFunction;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.type.Type;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.dialect.function.StandardAnsiSqlAggregationFunctions;
import java.util.HashSet;
import java.util.HashMap;
import org.hibernate.loader.BatchLoadSizingStrategy;
import org.hibernate.tool.schema.internal.StandardAuxiliaryDatabaseObjectExporter;
import org.hibernate.tool.schema.internal.StandardUniqueKeyExporter;
import org.hibernate.tool.schema.internal.StandardForeignKeyExporter;
import org.hibernate.tool.schema.internal.StandardIndexExporter;
import org.hibernate.tool.schema.internal.StandardSequenceExporter;
import org.hibernate.tool.schema.internal.StandardTableExporter;
import org.hibernate.exception.spi.ViolatedConstraintNameExtracter;
import org.hibernate.dialect.unique.UniqueDelegate;
import java.util.Set;
import org.hibernate.dialect.function.SQLFunction;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.exception.spi.ConversionContext;

public abstract class Dialect implements ConversionContext
{
    private static final CoreMessageLogger LOG;
    public static final String DEFAULT_BATCH_SIZE = "15";
    public static final String NO_BATCH = "0";
    public static final String QUOTE = "`\"[";
    public static final String CLOSED_QUOTE = "`\"]";
    private static final Pattern SINGLE_QUOTE_PATTERN;
    private static final Pattern ESCAPE_CLOSING_COMMENT_PATTERN;
    private static final Pattern ESCAPE_OPENING_COMMENT_PATTERN;
    public static final String TWO_SINGLE_QUOTES_REPLACEMENT;
    private final TypeNames typeNames;
    private final TypeNames hibernateTypeNames;
    private final Properties properties;
    private final Map<String, SQLFunction> sqlFunctions;
    private final Set<String> sqlKeywords;
    private final UniqueDelegate uniqueDelegate;
    private boolean legacyLimitHandlerBehavior;
    protected static final LobMergeStrategy LEGACY_LOB_MERGE_STRATEGY;
    protected static final LobMergeStrategy STREAM_XFER_LOB_MERGE_STRATEGY;
    protected static final LobMergeStrategy NEW_LOCATOR_LOB_MERGE_STRATEGY;
    private static final ViolatedConstraintNameExtracter EXTRACTER;
    private StandardTableExporter tableExporter;
    private StandardSequenceExporter sequenceExporter;
    private StandardIndexExporter indexExporter;
    private StandardForeignKeyExporter foreignKeyExporter;
    private StandardUniqueKeyExporter uniqueKeyExporter;
    private StandardAuxiliaryDatabaseObjectExporter auxiliaryObjectExporter;
    protected final BatchLoadSizingStrategy STANDARD_DEFAULT_BATCH_LOAD_SIZING_STRATEGY;
    
    protected Dialect() {
        this.typeNames = new TypeNames();
        this.hibernateTypeNames = new TypeNames();
        this.properties = new Properties();
        this.sqlFunctions = new HashMap<String, SQLFunction>();
        this.sqlKeywords = new HashSet<String>();
        this.tableExporter = new StandardTableExporter(this);
        this.sequenceExporter = new StandardSequenceExporter(this);
        this.indexExporter = new StandardIndexExporter(this);
        this.foreignKeyExporter = new StandardForeignKeyExporter(this);
        this.uniqueKeyExporter = new StandardUniqueKeyExporter(this);
        this.auxiliaryObjectExporter = new StandardAuxiliaryDatabaseObjectExporter(this);
        this.STANDARD_DEFAULT_BATCH_LOAD_SIZING_STRATEGY = new BatchLoadSizingStrategy() {
            @Override
            public int determineOptimalBatchLoadSize(final int numberOfKeyColumns, final int numberOfKeys) {
                return 50;
            }
        };
        Dialect.LOG.usingDialect(this);
        StandardAnsiSqlAggregationFunctions.primeFunctionMap(this.sqlFunctions);
        this.registerFunction("substring", new SQLFunctionTemplate(StandardBasicTypes.STRING, "substring(?1, ?2, ?3)"));
        this.registerFunction("locate", new SQLFunctionTemplate(StandardBasicTypes.INTEGER, "locate(?1, ?2, ?3)"));
        this.registerFunction("trim", new SQLFunctionTemplate(StandardBasicTypes.STRING, "trim(?1 ?2 ?3 ?4)"));
        this.registerFunction("length", new StandardSQLFunction("length", StandardBasicTypes.INTEGER));
        this.registerFunction("bit_length", new StandardSQLFunction("bit_length", StandardBasicTypes.INTEGER));
        this.registerFunction("coalesce", new StandardSQLFunction("coalesce"));
        this.registerFunction("nullif", new StandardSQLFunction("nullif"));
        this.registerFunction("abs", new StandardSQLFunction("abs"));
        this.registerFunction("mod", new StandardSQLFunction("mod", StandardBasicTypes.INTEGER));
        this.registerFunction("sqrt", new StandardSQLFunction("sqrt", StandardBasicTypes.DOUBLE));
        this.registerFunction("upper", new StandardSQLFunction("upper"));
        this.registerFunction("lower", new StandardSQLFunction("lower"));
        this.registerFunction("cast", new CastFunction());
        this.registerFunction("extract", new SQLFunctionTemplate(StandardBasicTypes.INTEGER, "extract(?1 ?2 ?3)"));
        this.registerFunction("second", new SQLFunctionTemplate(StandardBasicTypes.INTEGER, "extract(second from ?1)"));
        this.registerFunction("minute", new SQLFunctionTemplate(StandardBasicTypes.INTEGER, "extract(minute from ?1)"));
        this.registerFunction("hour", new SQLFunctionTemplate(StandardBasicTypes.INTEGER, "extract(hour from ?1)"));
        this.registerFunction("day", new SQLFunctionTemplate(StandardBasicTypes.INTEGER, "extract(day from ?1)"));
        this.registerFunction("month", new SQLFunctionTemplate(StandardBasicTypes.INTEGER, "extract(month from ?1)"));
        this.registerFunction("year", new SQLFunctionTemplate(StandardBasicTypes.INTEGER, "extract(year from ?1)"));
        this.registerFunction("str", new SQLFunctionTemplate(StandardBasicTypes.STRING, "cast(?1 as char)"));
        this.registerColumnType(-7, "bit");
        this.registerColumnType(16, "boolean");
        this.registerColumnType(-6, "tinyint");
        this.registerColumnType(5, "smallint");
        this.registerColumnType(4, "integer");
        this.registerColumnType(-5, "bigint");
        this.registerColumnType(6, "float($p)");
        this.registerColumnType(8, "double precision");
        this.registerColumnType(2, "numeric($p,$s)");
        this.registerColumnType(3, "decimal($p,$s)");
        this.registerColumnType(7, "real");
        this.registerColumnType(91, "date");
        this.registerColumnType(92, "time");
        this.registerColumnType(93, "timestamp");
        this.registerColumnType(-3, "bit varying($l)");
        this.registerColumnType(-4, "bit varying($l)");
        this.registerColumnType(2004, "blob");
        this.registerColumnType(1, "char($l)");
        this.registerColumnType(12, "varchar($l)");
        this.registerColumnType(-1, "varchar($l)");
        this.registerColumnType(2005, "clob");
        this.registerColumnType(-15, "nchar($l)");
        this.registerColumnType(-9, "nvarchar($l)");
        this.registerColumnType(-16, "nvarchar($l)");
        this.registerColumnType(2011, "nclob");
        this.registerHibernateType(-5, StandardBasicTypes.BIG_INTEGER.getName());
        this.registerHibernateType(-2, StandardBasicTypes.BINARY.getName());
        this.registerHibernateType(-7, StandardBasicTypes.BOOLEAN.getName());
        this.registerHibernateType(16, StandardBasicTypes.BOOLEAN.getName());
        this.registerHibernateType(1, StandardBasicTypes.CHARACTER.getName());
        this.registerHibernateType(1, 1L, StandardBasicTypes.CHARACTER.getName());
        this.registerHibernateType(1, 255L, StandardBasicTypes.STRING.getName());
        this.registerHibernateType(91, StandardBasicTypes.DATE.getName());
        this.registerHibernateType(8, StandardBasicTypes.DOUBLE.getName());
        this.registerHibernateType(6, StandardBasicTypes.FLOAT.getName());
        this.registerHibernateType(4, StandardBasicTypes.INTEGER.getName());
        this.registerHibernateType(5, StandardBasicTypes.SHORT.getName());
        this.registerHibernateType(-6, StandardBasicTypes.BYTE.getName());
        this.registerHibernateType(92, StandardBasicTypes.TIME.getName());
        this.registerHibernateType(93, StandardBasicTypes.TIMESTAMP.getName());
        this.registerHibernateType(12, StandardBasicTypes.STRING.getName());
        this.registerHibernateType(-9, StandardBasicTypes.NSTRING.getName());
        this.registerHibernateType(-3, StandardBasicTypes.BINARY.getName());
        this.registerHibernateType(-1, StandardBasicTypes.TEXT.getName());
        this.registerHibernateType(-4, StandardBasicTypes.IMAGE.getName());
        this.registerHibernateType(2, StandardBasicTypes.BIG_DECIMAL.getName());
        this.registerHibernateType(3, StandardBasicTypes.BIG_DECIMAL.getName());
        this.registerHibernateType(2004, StandardBasicTypes.BLOB.getName());
        this.registerHibernateType(2005, StandardBasicTypes.CLOB.getName());
        this.registerHibernateType(7, StandardBasicTypes.FLOAT.getName());
        if (this.supportsPartitionBy()) {
            this.registerKeyword("PARTITION");
        }
        this.uniqueDelegate = new DefaultUniqueDelegate(this);
    }
    
    public boolean equivalentTypes(final int typeCode1, final int typeCode2) {
        return typeCode1 == typeCode2 || (isNumericOrDecimal(typeCode1) && isNumericOrDecimal(typeCode2)) || (isFloatOrRealOrDouble(typeCode1) && isFloatOrRealOrDouble(typeCode2));
    }
    
    private static boolean isNumericOrDecimal(final int typeCode) {
        return typeCode == 2 || typeCode == 3;
    }
    
    private static boolean isFloatOrRealOrDouble(final int typeCode) {
        return typeCode == 6 || typeCode == 7 || typeCode == 8;
    }
    
    @Deprecated
    public static Dialect getDialect() throws HibernateException {
        return instantiateDialect(Environment.getProperties().getProperty("hibernate.dialect"));
    }
    
    @Deprecated
    public static Dialect getDialect(final Properties props) throws HibernateException {
        final String dialectName = props.getProperty("hibernate.dialect");
        if (dialectName == null) {
            return getDialect();
        }
        return instantiateDialect(dialectName);
    }
    
    @Deprecated
    private static Dialect instantiateDialect(final String dialectName) throws HibernateException {
        if (dialectName == null) {
            throw new HibernateException("The dialect was not set. Set the property hibernate.dialect.");
        }
        try {
            return ReflectHelper.classForName(dialectName).newInstance();
        }
        catch (ClassNotFoundException cnfe) {
            throw new HibernateException("Dialect class not found: " + dialectName);
        }
        catch (Exception e) {
            throw new HibernateException("Could not instantiate given dialect class: " + dialectName, e);
        }
    }
    
    public final Properties getDefaultProperties() {
        return this.properties;
    }
    
    @Override
    public String toString() {
        return this.getClass().getName();
    }
    
    public void contributeTypes(final TypeContributions typeContributions, final ServiceRegistry serviceRegistry) {
        this.resolveLegacyLimitHandlerBehavior(serviceRegistry);
    }
    
    public String getTypeName(final int code) throws HibernateException {
        final String result = this.typeNames.get(code);
        if (result == null) {
            throw new HibernateException("No default type mapping for (java.sql.Types) " + code);
        }
        return result;
    }
    
    public String getTypeName(final int code, final long length, final int precision, final int scale) throws HibernateException {
        final String result = this.typeNames.get(code, length, precision, scale);
        if (result == null) {
            throw new HibernateException(String.format("No type mapping for java.sql.Types code: %s, length: %s", code, length));
        }
        return result;
    }
    
    public String getCastTypeName(final int code) {
        return this.getTypeName(code, 255L, 19, 2);
    }
    
    public String cast(final String value, final int jdbcTypeCode, final int length, final int precision, final int scale) {
        if (jdbcTypeCode == 1) {
            return "cast(" + value + " as char(" + length + "))";
        }
        return "cast(" + value + "as " + this.getTypeName(jdbcTypeCode, length, precision, scale) + ")";
    }
    
    public String cast(final String value, final int jdbcTypeCode, final int length) {
        return this.cast(value, jdbcTypeCode, length, 19, 2);
    }
    
    public String cast(final String value, final int jdbcTypeCode, final int precision, final int scale) {
        return this.cast(value, jdbcTypeCode, 255, precision, scale);
    }
    
    protected void registerColumnType(final int code, final long capacity, final String name) {
        this.typeNames.put(code, capacity, name);
    }
    
    protected void registerColumnType(final int code, final String name) {
        this.typeNames.put(code, name);
    }
    
    public SqlTypeDescriptor remapSqlTypeDescriptor(final SqlTypeDescriptor sqlTypeDescriptor) {
        if (sqlTypeDescriptor == null) {
            throw new IllegalArgumentException("sqlTypeDescriptor is null");
        }
        if (!sqlTypeDescriptor.canBeRemapped()) {
            return sqlTypeDescriptor;
        }
        final SqlTypeDescriptor overridden = this.getSqlTypeDescriptorOverride(sqlTypeDescriptor.getSqlType());
        return (overridden == null) ? sqlTypeDescriptor : overridden;
    }
    
    protected SqlTypeDescriptor getSqlTypeDescriptorOverride(final int sqlCode) {
        SqlTypeDescriptor descriptor = null;
        switch (sqlCode) {
            case 2005: {
                descriptor = (this.useInputStreamToInsertBlob() ? ClobTypeDescriptor.STREAM_BINDING : null);
                break;
            }
            default: {
                descriptor = null;
                break;
            }
        }
        return descriptor;
    }
    
    public LobMergeStrategy getLobMergeStrategy() {
        return Dialect.NEW_LOCATOR_LOB_MERGE_STRATEGY;
    }
    
    public String getHibernateTypeName(final int code) throws HibernateException {
        final String result = this.hibernateTypeNames.get(code);
        if (result == null) {
            throw new HibernateException("No Hibernate type mapping for java.sql.Types code: " + code);
        }
        return result;
    }
    
    public boolean isTypeNameRegistered(final String typeName) {
        return this.typeNames.containsTypeName(typeName);
    }
    
    public String getHibernateTypeName(final int code, final int length, final int precision, final int scale) throws HibernateException {
        final String result = this.hibernateTypeNames.get(code, length, precision, scale);
        if (result == null) {
            throw new HibernateException(String.format("No Hibernate type mapping for type [code=%s, length=%s]", code, length));
        }
        return result;
    }
    
    protected void registerHibernateType(final int code, final long capacity, final String name) {
        this.hibernateTypeNames.put(code, capacity, name);
    }
    
    protected void registerHibernateType(final int code, final String name) {
        this.hibernateTypeNames.put(code, name);
    }
    
    protected void registerFunction(final String name, final SQLFunction function) {
        this.sqlFunctions.put(name.toLowerCase(Locale.ROOT), function);
    }
    
    public final Map<String, SQLFunction> getFunctions() {
        return this.sqlFunctions;
    }
    
    @Deprecated
    public Class getNativeIdentifierGeneratorClass() {
        if (this.getIdentityColumnSupport().supportsIdentityColumns()) {
            return IdentityGenerator.class;
        }
        return SequenceStyleGenerator.class;
    }
    
    public String getNativeIdentifierGeneratorStrategy() {
        if (this.getIdentityColumnSupport().supportsIdentityColumns()) {
            return "identity";
        }
        return "sequence";
    }
    
    public IdentityColumnSupport getIdentityColumnSupport() {
        return new IdentityColumnSupportImpl();
    }
    
    public boolean supportsSequences() {
        return false;
    }
    
    public boolean supportsPooledSequences() {
        return false;
    }
    
    public String getSequenceNextValString(final String sequenceName) throws MappingException {
        throw new MappingException(this.getClass().getName() + " does not support sequences");
    }
    
    public String getSelectSequenceNextValString(final String sequenceName) throws MappingException {
        throw new MappingException(this.getClass().getName() + " does not support sequences");
    }
    
    @Deprecated
    public String[] getCreateSequenceStrings(final String sequenceName) throws MappingException {
        return new String[] { this.getCreateSequenceString(sequenceName) };
    }
    
    public String[] getCreateSequenceStrings(final String sequenceName, final int initialValue, final int incrementSize) throws MappingException {
        return new String[] { this.getCreateSequenceString(sequenceName, initialValue, incrementSize) };
    }
    
    protected String getCreateSequenceString(final String sequenceName) throws MappingException {
        throw new MappingException(this.getClass().getName() + " does not support sequences");
    }
    
    protected String getCreateSequenceString(final String sequenceName, final int initialValue, final int incrementSize) throws MappingException {
        if (this.supportsPooledSequences()) {
            return this.getCreateSequenceString(sequenceName) + " start with " + initialValue + " increment by " + incrementSize;
        }
        throw new MappingException(this.getClass().getName() + " does not support pooled sequences");
    }
    
    public String[] getDropSequenceStrings(final String sequenceName) throws MappingException {
        return new String[] { this.getDropSequenceString(sequenceName) };
    }
    
    protected String getDropSequenceString(final String sequenceName) throws MappingException {
        throw new MappingException(this.getClass().getName() + " does not support sequences");
    }
    
    public String getQuerySequencesString() {
        return null;
    }
    
    public SequenceInformationExtractor getSequenceInformationExtractor() {
        if (this.getQuerySequencesString() == null) {
            return SequenceInformationExtractorNoOpImpl.INSTANCE;
        }
        return SequenceInformationExtractorLegacyImpl.INSTANCE;
    }
    
    public String getSelectGUIDString() {
        throw new UnsupportedOperationException(this.getClass().getName() + " does not support GUIDs");
    }
    
    public LimitHandler getLimitHandler() {
        return new LegacyLimitHandler(this);
    }
    
    @Deprecated
    public boolean supportsLimit() {
        return false;
    }
    
    @Deprecated
    public boolean supportsLimitOffset() {
        return this.supportsLimit();
    }
    
    @Deprecated
    public boolean supportsVariableLimit() {
        return this.supportsLimit();
    }
    
    @Deprecated
    public boolean bindLimitParametersInReverseOrder() {
        return false;
    }
    
    @Deprecated
    public boolean bindLimitParametersFirst() {
        return false;
    }
    
    @Deprecated
    public boolean useMaxForLimit() {
        return false;
    }
    
    @Deprecated
    public boolean forceLimitUsage() {
        return false;
    }
    
    @Deprecated
    public String getLimitString(final String query, final int offset, final int limit) {
        return this.getLimitString(query, offset > 0 || this.forceLimitUsage());
    }
    
    @Deprecated
    protected String getLimitString(final String query, final boolean hasOffset) {
        throw new UnsupportedOperationException("Paged queries not supported by " + this.getClass().getName());
    }
    
    @Deprecated
    public int convertToFirstRowValue(final int zeroBasedFirstResult) {
        return zeroBasedFirstResult;
    }
    
    public boolean supportsLockTimeouts() {
        return true;
    }
    
    public boolean isLockTimeoutParameterized() {
        return false;
    }
    
    public LockingStrategy getLockingStrategy(final Lockable lockable, final LockMode lockMode) {
        switch (lockMode) {
            case PESSIMISTIC_FORCE_INCREMENT: {
                return new PessimisticForceIncrementLockingStrategy(lockable, lockMode);
            }
            case PESSIMISTIC_WRITE: {
                return new PessimisticWriteSelectLockingStrategy(lockable, lockMode);
            }
            case PESSIMISTIC_READ: {
                return new PessimisticReadSelectLockingStrategy(lockable, lockMode);
            }
            case OPTIMISTIC: {
                return new OptimisticLockingStrategy(lockable, lockMode);
            }
            case OPTIMISTIC_FORCE_INCREMENT: {
                return new OptimisticForceIncrementLockingStrategy(lockable, lockMode);
            }
            default: {
                return new SelectLockingStrategy(lockable, lockMode);
            }
        }
    }
    
    public String getForUpdateString(final LockOptions lockOptions) {
        final LockMode lockMode = lockOptions.getLockMode();
        return this.getForUpdateString(lockMode, lockOptions.getTimeOut());
    }
    
    private String getForUpdateString(final LockMode lockMode, final int timeout) {
        switch (lockMode) {
            case UPGRADE: {
                return this.getForUpdateString();
            }
            case PESSIMISTIC_READ: {
                return this.getReadLockString(timeout);
            }
            case PESSIMISTIC_WRITE: {
                return this.getWriteLockString(timeout);
            }
            case PESSIMISTIC_FORCE_INCREMENT:
            case UPGRADE_NOWAIT:
            case FORCE: {
                return this.getForUpdateNowaitString();
            }
            case UPGRADE_SKIPLOCKED: {
                return this.getForUpdateSkipLockedString();
            }
            default: {
                return "";
            }
        }
    }
    
    public String getForUpdateString(final LockMode lockMode) {
        return this.getForUpdateString(lockMode, -1);
    }
    
    public String getForUpdateString() {
        return " for update";
    }
    
    public String getWriteLockString(final int timeout) {
        return this.getForUpdateString();
    }
    
    public String getWriteLockString(final String aliases, final int timeout) {
        return this.getWriteLockString(timeout);
    }
    
    public String getReadLockString(final int timeout) {
        return this.getForUpdateString();
    }
    
    public String getReadLockString(final String aliases, final int timeout) {
        return this.getReadLockString(timeout);
    }
    
    public boolean forUpdateOfColumns() {
        return false;
    }
    
    public boolean supportsOuterJoinForUpdate() {
        return true;
    }
    
    public String getForUpdateString(final String aliases) {
        return this.getForUpdateString();
    }
    
    public String getForUpdateString(final String aliases, final LockOptions lockOptions) {
        LockMode lockMode = lockOptions.getLockMode();
        final Iterator<Map.Entry<String, LockMode>> itr = lockOptions.getAliasLockIterator();
        while (itr.hasNext()) {
            final Map.Entry<String, LockMode> entry = itr.next();
            final LockMode lm = entry.getValue();
            if (lm.greaterThan(lockMode)) {
                lockMode = lm;
            }
        }
        lockOptions.setLockMode(lockMode);
        return this.getForUpdateString(lockOptions);
    }
    
    public String getForUpdateNowaitString() {
        return this.getForUpdateString();
    }
    
    public String getForUpdateSkipLockedString() {
        return this.getForUpdateString();
    }
    
    public String getForUpdateNowaitString(final String aliases) {
        return this.getForUpdateString(aliases);
    }
    
    public String getForUpdateSkipLockedString(final String aliases) {
        return this.getForUpdateString(aliases);
    }
    
    @Deprecated
    public String appendLockHint(final LockMode mode, final String tableName) {
        return this.appendLockHint(new LockOptions(mode), tableName);
    }
    
    public String appendLockHint(final LockOptions lockOptions, final String tableName) {
        return tableName;
    }
    
    public String applyLocksToSql(final String sql, final LockOptions aliasedLockOptions, final Map<String, String[]> keyColumnNames) {
        return sql + new ForUpdateFragment(this, aliasedLockOptions, keyColumnNames).toFragmentString();
    }
    
    public String getCreateTableString() {
        return "create table";
    }
    
    public String getAlterTableString(final String tableName) {
        final StringBuilder sb = new StringBuilder("alter table ");
        if (this.supportsIfExistsAfterAlterTable()) {
            sb.append("if exists ");
        }
        sb.append(tableName);
        return sb.toString();
    }
    
    public String getCreateMultisetTableString() {
        return this.getCreateTableString();
    }
    
    public MultiTableBulkIdStrategy getDefaultMultiTableBulkIdStrategy() {
        return new PersistentTableBulkIdStrategy();
    }
    
    public int registerResultSetOutParameter(final CallableStatement statement, final int position) throws SQLException {
        throw new UnsupportedOperationException(this.getClass().getName() + " does not support resultsets via stored procedures");
    }
    
    public int registerResultSetOutParameter(final CallableStatement statement, final String name) throws SQLException {
        throw new UnsupportedOperationException(this.getClass().getName() + " does not support resultsets via stored procedures");
    }
    
    public ResultSet getResultSet(final CallableStatement statement) throws SQLException {
        throw new UnsupportedOperationException(this.getClass().getName() + " does not support resultsets via stored procedures");
    }
    
    public ResultSet getResultSet(final CallableStatement statement, final int position) throws SQLException {
        throw new UnsupportedOperationException(this.getClass().getName() + " does not support resultsets via stored procedures");
    }
    
    public ResultSet getResultSet(final CallableStatement statement, final String name) throws SQLException {
        throw new UnsupportedOperationException(this.getClass().getName() + " does not support resultsets via stored procedures");
    }
    
    public boolean supportsCurrentTimestampSelection() {
        return false;
    }
    
    public boolean isCurrentTimestampSelectStringCallable() {
        throw new UnsupportedOperationException("Database not known to define a current timestamp function");
    }
    
    public String getCurrentTimestampSelectString() {
        throw new UnsupportedOperationException("Database not known to define a current timestamp function");
    }
    
    public String getCurrentTimestampSQLFunctionName() {
        return "current_timestamp";
    }
    
    @Deprecated
    public SQLExceptionConverter buildSQLExceptionConverter() {
        return null;
    }
    
    public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
        return null;
    }
    
    @Override
    public ViolatedConstraintNameExtracter getViolatedConstraintNameExtracter() {
        return Dialect.EXTRACTER;
    }
    
    public String getSelectClauseNullString(final int sqlType) {
        return "null";
    }
    
    public boolean supportsUnionAll() {
        return false;
    }
    
    public JoinFragment createOuterJoinFragment() {
        return new ANSIJoinFragment();
    }
    
    public CaseFragment createCaseFragment() {
        return new ANSICaseFragment();
    }
    
    public String getNoColumnsInsertString() {
        return "values ( )";
    }
    
    public boolean supportsNoColumnsInsert() {
        return true;
    }
    
    public String getLowercaseFunction() {
        return "lower";
    }
    
    public String getCaseInsensitiveLike() {
        return "like";
    }
    
    public boolean supportsCaseInsensitiveLike() {
        return false;
    }
    
    public String transformSelectString(final String select) {
        return select;
    }
    
    public int getMaxAliasLength() {
        return 10;
    }
    
    public String toBooleanValueString(final boolean bool) {
        return bool ? "1" : "0";
    }
    
    protected void registerKeyword(final String word) {
        this.sqlKeywords.add(word.toLowerCase(Locale.ROOT));
    }
    
    @Deprecated
    public Set<String> getKeywords() {
        return this.sqlKeywords;
    }
    
    public IdentifierHelper buildIdentifierHelper(final IdentifierHelperBuilder builder, final DatabaseMetaData dbMetaData) throws SQLException {
        builder.applyIdentifierCasing(dbMetaData);
        builder.applyReservedWords(dbMetaData);
        builder.applyReservedWords(AnsiSqlKeywords.INSTANCE.sql2003());
        builder.applyReservedWords(this.sqlKeywords);
        builder.setNameQualifierSupport(this.getNameQualifierSupport());
        return builder.build();
    }
    
    public char openQuote() {
        return '\"';
    }
    
    public char closeQuote() {
        return '\"';
    }
    
    public final String quote(final String name) {
        if (name == null) {
            return null;
        }
        if (name.charAt(0) == '`') {
            return this.openQuote() + name.substring(1, name.length() - 1) + this.closeQuote();
        }
        return name;
    }
    
    public Exporter<Table> getTableExporter() {
        return this.tableExporter;
    }
    
    public Exporter<Sequence> getSequenceExporter() {
        return this.sequenceExporter;
    }
    
    public Exporter<Index> getIndexExporter() {
        return this.indexExporter;
    }
    
    public Exporter<ForeignKey> getForeignKeyExporter() {
        return this.foreignKeyExporter;
    }
    
    public Exporter<Constraint> getUniqueKeyExporter() {
        return this.uniqueKeyExporter;
    }
    
    public Exporter<AuxiliaryDatabaseObject> getAuxiliaryDatabaseObjectExporter() {
        return this.auxiliaryObjectExporter;
    }
    
    public boolean canCreateCatalog() {
        return false;
    }
    
    public String[] getCreateCatalogCommand(final String catalogName) {
        throw new UnsupportedOperationException("No create catalog syntax supported by " + this.getClass().getName());
    }
    
    public String[] getDropCatalogCommand(final String catalogName) {
        throw new UnsupportedOperationException("No drop catalog syntax supported by " + this.getClass().getName());
    }
    
    public boolean canCreateSchema() {
        return true;
    }
    
    public String[] getCreateSchemaCommand(final String schemaName) {
        return new String[] { "create schema " + schemaName };
    }
    
    public String[] getDropSchemaCommand(final String schemaName) {
        return new String[] { "drop schema " + schemaName };
    }
    
    public String getCurrentSchemaCommand() {
        return null;
    }
    
    public SchemaNameResolver getSchemaNameResolver() {
        return DefaultSchemaNameResolver.INSTANCE;
    }
    
    public boolean hasAlterTable() {
        return true;
    }
    
    public boolean dropConstraints() {
        return true;
    }
    
    public boolean qualifyIndexName() {
        return true;
    }
    
    public String getAddColumnString() {
        throw new UnsupportedOperationException("No add column syntax supported by " + this.getClass().getName());
    }
    
    public String getAddColumnSuffixString() {
        return "";
    }
    
    public String getDropForeignKeyString() {
        return " drop constraint ";
    }
    
    public String getTableTypeString() {
        return "";
    }
    
    public String getAddForeignKeyConstraintString(final String constraintName, final String[] foreignKey, final String referencedTable, final String[] primaryKey, final boolean referencesPrimaryKey) {
        final StringBuilder res = new StringBuilder(30);
        res.append(" add constraint ").append(this.quote(constraintName)).append(" foreign key (").append(String.join(", ", (CharSequence[])foreignKey)).append(") references ").append(referencedTable);
        if (!referencesPrimaryKey) {
            res.append(" (").append(String.join(", ", (CharSequence[])primaryKey)).append(')');
        }
        return res.toString();
    }
    
    public String getAddForeignKeyConstraintString(final String constraintName, final String foreignKeyDefinition) {
        return new StringBuilder(30).append(" add constraint ").append(this.quote(constraintName)).append(" ").append(foreignKeyDefinition).toString();
    }
    
    public String getAddPrimaryKeyConstraintString(final String constraintName) {
        return " add constraint " + constraintName + " primary key ";
    }
    
    public boolean hasSelfReferentialForeignKeyBug() {
        return false;
    }
    
    public String getNullColumnString() {
        return "";
    }
    
    public boolean supportsCommentOn() {
        return false;
    }
    
    public String getTableComment(final String comment) {
        return "";
    }
    
    public String getColumnComment(final String comment) {
        return "";
    }
    
    public boolean supportsIfExistsBeforeTableName() {
        return false;
    }
    
    public boolean supportsIfExistsAfterTableName() {
        return false;
    }
    
    public boolean supportsIfExistsBeforeConstraintName() {
        return false;
    }
    
    public boolean supportsIfExistsAfterConstraintName() {
        return false;
    }
    
    public boolean supportsIfExistsAfterAlterTable() {
        return false;
    }
    
    public String getDropTableString(final String tableName) {
        final StringBuilder buf = new StringBuilder("drop table ");
        if (this.supportsIfExistsBeforeTableName()) {
            buf.append("if exists ");
        }
        buf.append(tableName).append(this.getCascadeConstraintsString());
        if (this.supportsIfExistsAfterTableName()) {
            buf.append(" if exists");
        }
        return buf.toString();
    }
    
    public boolean supportsColumnCheck() {
        return true;
    }
    
    public boolean supportsTableCheck() {
        return true;
    }
    
    public boolean supportsCascadeDelete() {
        return true;
    }
    
    public String getCascadeConstraintsString() {
        return "";
    }
    
    public String getCrossJoinSeparator() {
        return " cross join ";
    }
    
    public ColumnAliasExtractor getColumnAliasExtractor() {
        return ColumnAliasExtractor.COLUMN_LABEL_EXTRACTOR;
    }
    
    public boolean supportsEmptyInList() {
        return true;
    }
    
    public boolean areStringComparisonsCaseInsensitive() {
        return false;
    }
    
    public boolean supportsRowValueConstructorSyntax() {
        return false;
    }
    
    public boolean supportsRowValueConstructorSyntaxInSet() {
        return this.supportsRowValueConstructorSyntax();
    }
    
    public boolean supportsRowValueConstructorSyntaxInInList() {
        return false;
    }
    
    public boolean useInputStreamToInsertBlob() {
        return true;
    }
    
    public boolean supportsParametersInInsertSelect() {
        return true;
    }
    
    public boolean replaceResultVariableInOrderByClauseWithPosition() {
        return false;
    }
    
    public String renderOrderByElement(final String expression, final String collation, final String order, final NullPrecedence nulls) {
        final StringBuilder orderByElement = new StringBuilder(expression);
        if (collation != null) {
            orderByElement.append(" ").append(collation);
        }
        if (order != null) {
            orderByElement.append(" ").append(order);
        }
        if (nulls != NullPrecedence.NONE) {
            orderByElement.append(" nulls ").append(nulls.name().toLowerCase(Locale.ROOT));
        }
        return orderByElement.toString();
    }
    
    public boolean requiresCastingOfParametersInSelectClause() {
        return false;
    }
    
    public boolean supportsResultSetPositionQueryMethodsOnForwardOnlyCursor() {
        return true;
    }
    
    public boolean supportsCircularCascadeDeleteConstraints() {
        return true;
    }
    
    public boolean supportsSubselectAsInPredicateLHS() {
        return true;
    }
    
    public boolean supportsExpectedLobUsagePattern() {
        return true;
    }
    
    public boolean supportsLobValueChangePropogation() {
        return true;
    }
    
    public boolean supportsUnboundedLobLocatorMaterialization() {
        return true;
    }
    
    public boolean supportsSubqueryOnMutatingTable() {
        return true;
    }
    
    public boolean supportsExistsInSelect() {
        return true;
    }
    
    public boolean doesReadCommittedCauseWritersToBlockReaders() {
        return false;
    }
    
    public boolean doesRepeatableReadCauseReadersToBlockWriters() {
        return false;
    }
    
    public boolean supportsBindAsCallableArgument() {
        return true;
    }
    
    public boolean supportsTupleCounts() {
        return false;
    }
    
    public boolean supportsTupleDistinctCounts() {
        return true;
    }
    
    public boolean requiresParensForTupleDistinctCounts() {
        return false;
    }
    
    public int getInExpressionCountLimit() {
        return 0;
    }
    
    public boolean forceLobAsLastValue() {
        return false;
    }
    
    @Deprecated
    public boolean useFollowOnLocking() {
        return this.useFollowOnLocking(null);
    }
    
    public boolean useFollowOnLocking(final QueryParameters parameters) {
        return false;
    }
    
    public String getNotExpression(final String expression) {
        return "not " + expression;
    }
    
    public UniqueDelegate getUniqueDelegate() {
        return this.uniqueDelegate;
    }
    
    @Deprecated
    public boolean supportsUnique() {
        return true;
    }
    
    @Deprecated
    public boolean supportsUniqueConstraintInCreateAlterTable() {
        return true;
    }
    
    @Deprecated
    public String getAddUniqueConstraintString(final String constraintName) {
        return " add constraint " + constraintName + " unique ";
    }
    
    @Deprecated
    public boolean supportsNotNullUnique() {
        return true;
    }
    
    public String getQueryHintString(final String query, final List<String> hintList) {
        final String hints = String.join(", ", hintList);
        if (hints.isEmpty()) {
            return query;
        }
        return this.getQueryHintString(query, hints);
    }
    
    public String getQueryHintString(final String query, final String hints) {
        return query;
    }
    
    public ScrollMode defaultScrollMode() {
        return ScrollMode.SCROLL_INSENSITIVE;
    }
    
    public boolean supportsTuplesInSubqueries() {
        return true;
    }
    
    public CallableStatementSupport getCallableStatementSupport() {
        return StandardCallableStatementSupport.NO_REF_CURSOR_INSTANCE;
    }
    
    public NameQualifierSupport getNameQualifierSupport() {
        return null;
    }
    
    public BatchLoadSizingStrategy getDefaultBatchLoadSizingStrategy() {
        return this.STANDARD_DEFAULT_BATCH_LOAD_SIZING_STRATEGY;
    }
    
    public boolean isJdbcLogWarningsEnabledByDefault() {
        return true;
    }
    
    public void augmentRecognizedTableTypes(final List<String> tableTypesList) {
    }
    
    public boolean supportsPartitionBy() {
        return false;
    }
    
    public boolean supportsNamedParameters(final DatabaseMetaData databaseMetaData) throws SQLException {
        return databaseMetaData != null && databaseMetaData.supportsNamedParameters();
    }
    
    public boolean supportsNationalizedTypes() {
        return true;
    }
    
    public boolean supportsNonQueryWithCTE() {
        return false;
    }
    
    public boolean supportsValuesList() {
        return false;
    }
    
    public boolean supportsSkipLocked() {
        return false;
    }
    
    public boolean supportsNoWait() {
        return false;
    }
    
    public boolean isLegacyLimitHandlerBehaviorEnabled() {
        return this.legacyLimitHandlerBehavior;
    }
    
    public String inlineLiteral(final String literal) {
        return String.format("'%s'", this.escapeLiteral(literal));
    }
    
    public boolean supportsJdbcConnectionLobCreation(final DatabaseMetaData databaseMetaData) {
        return true;
    }
    
    protected String escapeLiteral(final String literal) {
        return Dialect.SINGLE_QUOTE_PATTERN.matcher(literal).replaceAll(Dialect.TWO_SINGLE_QUOTES_REPLACEMENT);
    }
    
    private void resolveLegacyLimitHandlerBehavior(final ServiceRegistry serviceRegistry) {
        final ConfigurationService configurationService = serviceRegistry.getService(ConfigurationService.class);
        this.legacyLimitHandlerBehavior = configurationService.getSetting("hibernate.legacy_limit_handler", StandardConverters.BOOLEAN, false);
    }
    
    public String addSqlHintOrComment(String sql, final QueryParameters parameters, final boolean commentsEnabled) {
        if (parameters.getQueryHints() != null && parameters.getQueryHints().size() > 0) {
            sql = this.getQueryHintString(sql, parameters.getQueryHints());
        }
        if (commentsEnabled && parameters.getComment() != null) {
            sql = this.prependComment(sql, parameters.getComment());
        }
        return sql;
    }
    
    protected String prependComment(final String sql, final String comment) {
        return "/* " + escapeComment(comment) + " */ " + sql;
    }
    
    public static String escapeComment(final String comment) {
        if (StringHelper.isNotEmpty(comment)) {
            final String escaped = Dialect.ESCAPE_CLOSING_COMMENT_PATTERN.matcher(comment).replaceAll("*\\\\/");
            return Dialect.ESCAPE_OPENING_COMMENT_PATTERN.matcher(escaped).replaceAll("/\\\\*");
        }
        return comment;
    }
    
    public boolean supportsSelectAliasInGroupByClause() {
        return false;
    }
    
    public String getCreateTemporaryTableColumnAnnotation(final int sqlTypeCode) {
        return "";
    }
    
    static {
        LOG = CoreLogging.messageLogger(Dialect.class);
        SINGLE_QUOTE_PATTERN = Pattern.compile("'", 16);
        ESCAPE_CLOSING_COMMENT_PATTERN = Pattern.compile("\\*/");
        ESCAPE_OPENING_COMMENT_PATTERN = Pattern.compile("/\\*");
        TWO_SINGLE_QUOTES_REPLACEMENT = Matcher.quoteReplacement("''");
        LEGACY_LOB_MERGE_STRATEGY = new LobMergeStrategy() {
            @Override
            public Blob mergeBlob(final Blob original, final Blob target, final SharedSessionContractImplementor session) {
                return target;
            }
            
            @Override
            public Clob mergeClob(final Clob original, final Clob target, final SharedSessionContractImplementor session) {
                return target;
            }
            
            @Override
            public NClob mergeNClob(final NClob original, final NClob target, final SharedSessionContractImplementor session) {
                return target;
            }
        };
        STREAM_XFER_LOB_MERGE_STRATEGY = new LobMergeStrategy() {
            @Override
            public Blob mergeBlob(final Blob original, final Blob target, final SharedSessionContractImplementor session) {
                if (original != target) {
                    try {
                        final OutputStream connectedStream = target.setBinaryStream(1L);
                        final InputStream detachedStream = original.getBinaryStream();
                        StreamCopier.copy(detachedStream, connectedStream);
                        return target;
                    }
                    catch (SQLException e) {
                        throw session.getFactory().getSQLExceptionHelper().convert(e, "unable to merge BLOB data");
                    }
                }
                return Dialect.NEW_LOCATOR_LOB_MERGE_STRATEGY.mergeBlob(original, target, session);
            }
            
            @Override
            public Clob mergeClob(final Clob original, final Clob target, final SharedSessionContractImplementor session) {
                if (original != target) {
                    try {
                        final OutputStream connectedStream = target.setAsciiStream(1L);
                        final InputStream detachedStream = original.getAsciiStream();
                        StreamCopier.copy(detachedStream, connectedStream);
                        return target;
                    }
                    catch (SQLException e) {
                        throw session.getFactory().getSQLExceptionHelper().convert(e, "unable to merge CLOB data");
                    }
                }
                return Dialect.NEW_LOCATOR_LOB_MERGE_STRATEGY.mergeClob(original, target, session);
            }
            
            @Override
            public NClob mergeNClob(final NClob original, final NClob target, final SharedSessionContractImplementor session) {
                if (original != target) {
                    try {
                        final OutputStream connectedStream = target.setAsciiStream(1L);
                        final InputStream detachedStream = original.getAsciiStream();
                        StreamCopier.copy(detachedStream, connectedStream);
                        return target;
                    }
                    catch (SQLException e) {
                        throw session.getFactory().getSQLExceptionHelper().convert(e, "unable to merge NCLOB data");
                    }
                }
                return Dialect.NEW_LOCATOR_LOB_MERGE_STRATEGY.mergeNClob(original, target, session);
            }
        };
        NEW_LOCATOR_LOB_MERGE_STRATEGY = new LobMergeStrategy() {
            @Override
            public Blob mergeBlob(final Blob original, final Blob target, final SharedSessionContractImplementor session) {
                if (original == null && target == null) {
                    return null;
                }
                try {
                    final LobCreator lobCreator = session.getFactory().getServiceRegistry().getService(JdbcServices.class).getLobCreator(session);
                    return (original == null) ? lobCreator.createBlob(ArrayHelper.EMPTY_BYTE_ARRAY) : lobCreator.createBlob(original.getBinaryStream(), original.length());
                }
                catch (SQLException e) {
                    throw session.getFactory().getSQLExceptionHelper().convert(e, "unable to merge BLOB data");
                }
            }
            
            @Override
            public Clob mergeClob(final Clob original, final Clob target, final SharedSessionContractImplementor session) {
                if (original == null && target == null) {
                    return null;
                }
                try {
                    final LobCreator lobCreator = session.getFactory().getServiceRegistry().getService(JdbcServices.class).getLobCreator(session);
                    return (original == null) ? lobCreator.createClob("") : lobCreator.createClob(original.getCharacterStream(), original.length());
                }
                catch (SQLException e) {
                    throw session.getFactory().getSQLExceptionHelper().convert(e, "unable to merge CLOB data");
                }
            }
            
            @Override
            public NClob mergeNClob(final NClob original, final NClob target, final SharedSessionContractImplementor session) {
                if (original == null && target == null) {
                    return null;
                }
                try {
                    final LobCreator lobCreator = session.getFactory().getServiceRegistry().getService(JdbcServices.class).getLobCreator(session);
                    return (original == null) ? lobCreator.createNClob("") : lobCreator.createNClob(original.getCharacterStream(), original.length());
                }
                catch (SQLException e) {
                    throw session.getFactory().getSQLExceptionHelper().convert(e, "unable to merge NCLOB data");
                }
            }
        };
        EXTRACTER = new ViolatedConstraintNameExtracter() {
            @Override
            public String extractConstraintName(final SQLException sqle) {
                return null;
            }
        };
    }
}
