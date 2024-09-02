// 
// Decompiled by Procyon v0.5.36
// 

package com.mysql.cj.protocol.x;

import com.mysql.cj.protocol.AuthenticationProvider;
import com.mysql.cj.MessageBuilder;
import com.mysql.cj.CharsetMapping;
import java.io.InputStream;
import com.mysql.cj.protocol.Resultset;
import com.mysql.cj.exceptions.CJOperationNotSupportedException;
import com.mysql.cj.x.protobuf.MysqlxDatatypes;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.mysql.cj.x.protobuf.MysqlxConnection;
import com.mysql.cj.protocol.ServerCapabilities;
import com.mysql.cj.exceptions.ConnectionIsClosedException;
import com.mysql.cj.protocol.MessageListener;
import java.util.concurrent.CompletableFuture;
import java.lang.ref.Reference;
import com.mysql.cj.result.DefaultColumnDefinition;
import com.mysql.cj.result.Field;
import java.util.ArrayList;
import java.util.LinkedList;
import com.mysql.cj.exceptions.AssertionFailedException;
import com.mysql.cj.x.protobuf.MysqlxSession;
import java.util.Iterator;
import java.util.Optional;
import com.mysql.cj.protocol.ColumnDefinition;
import com.mysql.cj.result.ValueFactory;
import com.mysql.cj.result.LongValueFactory;
import java.util.function.Consumer;
import com.mysql.cj.protocol.Message;
import com.mysql.cj.Constants;
import com.mysql.cj.Messages;
import java.util.List;
import java.util.Arrays;
import com.mysql.cj.exceptions.ExceptionFactory;
import com.mysql.cj.exceptions.WrongArgumentException;
import com.mysql.cj.conf.PropertyDefinitions;
import java.io.IOException;
import com.mysql.cj.exceptions.FeatureNotAvailableException;
import com.mysql.cj.exceptions.SSLParamsException;
import com.mysql.cj.exceptions.CJCommunicationsException;
import com.mysql.cj.exceptions.CJConnectionFeatureNotAvailableException;
import com.mysql.cj.protocol.ExportControlled;
import com.mysql.cj.QueryResult;
import com.mysql.cj.protocol.ResultBuilder;
import com.mysql.cj.protocol.ServerSession;
import com.mysql.cj.x.protobuf.Mysqlx;
import com.mysql.cj.x.protobuf.MysqlxSql;
import com.mysql.cj.x.protobuf.MysqlxNotice;
import com.mysql.cj.x.protobuf.MysqlxResultset;
import com.mysql.cj.util.StringUtils;
import com.mysql.cj.conf.HostInfo;
import com.mysql.cj.protocol.SocketConnection;
import com.mysql.cj.conf.RuntimeProperty;
import com.mysql.cj.TransactionEventHandler;
import com.mysql.cj.Session;
import com.mysql.cj.log.Log;
import com.mysql.cj.exceptions.ExceptionInterceptor;
import com.mysql.cj.protocol.a.NativeSocketConnection;
import com.mysql.cj.conf.PropertyKey;
import java.util.TreeMap;
import java.util.HashMap;
import com.mysql.cj.conf.PropertySet;
import com.mysql.cj.protocol.ProtocolEntity;
import com.mysql.cj.protocol.ProtocolEntityFactory;
import com.google.protobuf.GeneratedMessageV3;
import com.mysql.cj.xdevapi.PreparableStatement;
import java.lang.ref.ReferenceQueue;
import com.mysql.cj.util.SequentialIdLease;
import java.util.Map;
import com.mysql.cj.protocol.ResultStreamer;
import java.io.Closeable;
import com.mysql.cj.protocol.MessageSender;
import com.mysql.cj.protocol.MessageReader;
import com.mysql.cj.protocol.Protocol;
import com.mysql.cj.protocol.AbstractProtocol;

public class XProtocol extends AbstractProtocol<XMessage> implements Protocol<XMessage>
{
    private static int RETRY_PREPARE_STATEMENT_COUNTDOWN;
    private MessageReader<XMessageHeader, XMessage> reader;
    private MessageSender<XMessage> sender;
    private Closeable managedResource;
    private ResultStreamer currentResultStreamer;
    XServerSession serverSession;
    Boolean useSessionResetKeepOpen;
    public String defaultSchemaName;
    private Map<String, Object> clientCapabilities;
    private boolean supportsPreparedStatements;
    private int retryPrepareStatementCountdown;
    private SequentialIdLease preparedStatementIds;
    private ReferenceQueue<PreparableStatement<?>> preparableStatementRefQueue;
    private Map<Integer, PreparableStatement.PreparableStatementFinalizer> preparableStatementFinalizerReferences;
    private Map<Class<? extends GeneratedMessageV3>, ProtocolEntityFactory<? extends ProtocolEntity, XMessage>> messageToProtocolEntityFactory;
    private String currUser;
    private String currPassword;
    private String currDatabase;
    public static Map<String, Integer> COLLATION_NAME_TO_COLLATION_INDEX;
    
    public XProtocol(final String host, final int port, final String defaultSchema, final PropertySet propertySet) {
        this.serverSession = null;
        this.useSessionResetKeepOpen = null;
        this.clientCapabilities = new HashMap<String, Object>();
        this.supportsPreparedStatements = true;
        this.retryPrepareStatementCountdown = 0;
        this.preparedStatementIds = new SequentialIdLease();
        this.preparableStatementRefQueue = new ReferenceQueue<PreparableStatement<?>>();
        this.preparableStatementFinalizerReferences = new TreeMap<Integer, PreparableStatement.PreparableStatementFinalizer>();
        this.messageToProtocolEntityFactory = new HashMap<Class<? extends GeneratedMessageV3>, ProtocolEntityFactory<? extends ProtocolEntity, XMessage>>();
        this.currUser = null;
        this.currPassword = null;
        this.currDatabase = null;
        this.defaultSchemaName = defaultSchema;
        final RuntimeProperty<Integer> connectTimeout = propertySet.getIntegerProperty(PropertyKey.connectTimeout);
        final RuntimeProperty<Integer> xdevapiConnectTimeout = propertySet.getIntegerProperty(PropertyKey.xdevapiConnectTimeout);
        if (xdevapiConnectTimeout.isExplicitlySet() || !connectTimeout.isExplicitlySet()) {
            connectTimeout.setValue(xdevapiConnectTimeout.getValue());
        }
        final SocketConnection socketConn = ((boolean)propertySet.getBooleanProperty(PropertyKey.xdevapiUseAsyncProtocol).getValue()) ? new XAsyncSocketConnection() : new NativeSocketConnection();
        socketConn.connect(host, port, propertySet, null, null, 0);
        this.init(null, socketConn, propertySet, null);
    }
    
    public XProtocol(final HostInfo hostInfo, final PropertySet propertySet) {
        this.serverSession = null;
        this.useSessionResetKeepOpen = null;
        this.clientCapabilities = new HashMap<String, Object>();
        this.supportsPreparedStatements = true;
        this.retryPrepareStatementCountdown = 0;
        this.preparedStatementIds = new SequentialIdLease();
        this.preparableStatementRefQueue = new ReferenceQueue<PreparableStatement<?>>();
        this.preparableStatementFinalizerReferences = new TreeMap<Integer, PreparableStatement.PreparableStatementFinalizer>();
        this.messageToProtocolEntityFactory = new HashMap<Class<? extends GeneratedMessageV3>, ProtocolEntityFactory<? extends ProtocolEntity, XMessage>>();
        this.currUser = null;
        this.currPassword = null;
        this.currDatabase = null;
        String host = hostInfo.getHost();
        if (host == null || StringUtils.isEmptyOrWhitespaceOnly(host)) {
            host = "localhost";
        }
        int port = hostInfo.getPort();
        if (port < 0) {
            port = 33060;
        }
        this.defaultSchemaName = hostInfo.getDatabase();
        final RuntimeProperty<Integer> connectTimeout = propertySet.getIntegerProperty(PropertyKey.connectTimeout);
        final RuntimeProperty<Integer> xdevapiConnectTimeout = propertySet.getIntegerProperty(PropertyKey.xdevapiConnectTimeout);
        if (xdevapiConnectTimeout.isExplicitlySet() || !connectTimeout.isExplicitlySet()) {
            connectTimeout.setValue(xdevapiConnectTimeout.getValue());
        }
        final SocketConnection socketConn = ((boolean)propertySet.getBooleanProperty(PropertyKey.xdevapiUseAsyncProtocol).getValue()) ? new XAsyncSocketConnection() : new NativeSocketConnection();
        socketConn.connect(host, port, propertySet, null, null, 0);
        this.init(null, socketConn, propertySet, null);
    }
    
    @Override
    public void init(final Session sess, final SocketConnection socketConn, final PropertySet propSet, final TransactionEventHandler trManager) {
        super.init(sess, socketConn, propSet, trManager);
        this.messageBuilder = (MessageBuilder<M>)new XMessageBuilder();
        (this.authProvider = (AuthenticationProvider<M>)new XAuthenticationProvider()).init((Protocol<M>)this, propSet, null);
        this.useSessionResetKeepOpen = null;
        this.messageToProtocolEntityFactory.put(MysqlxResultset.ColumnMetaData.class, new FieldFactory("latin1"));
        this.messageToProtocolEntityFactory.put(MysqlxNotice.Frame.class, new NoticeFactory());
        this.messageToProtocolEntityFactory.put(MysqlxResultset.Row.class, new XProtocolRowFactory());
        this.messageToProtocolEntityFactory.put(MysqlxResultset.FetchDoneMoreResultsets.class, new FetchDoneMoreResultsFactory());
        this.messageToProtocolEntityFactory.put(MysqlxResultset.FetchDone.class, new FetchDoneEntityFactory());
        this.messageToProtocolEntityFactory.put(MysqlxSql.StmtExecuteOk.class, new StatementExecuteOkFactory());
        this.messageToProtocolEntityFactory.put(Mysqlx.Ok.class, new OkFactory());
    }
    
    @Override
    public ServerSession getServerSession() {
        return this.serverSession;
    }
    
    public void sendCapabilities(final Map<String, Object> keyValuePair) {
        keyValuePair.forEach((k, v) -> ((XServerCapabilities)this.getServerSession().getCapabilities()).setCapability(k, v));
        this.sender.send(((XMessageBuilder)this.messageBuilder).buildCapabilitiesSet(keyValuePair));
        this.readQueryResult((ResultBuilder<QueryResult>)new OkBuilder());
    }
    
    @Override
    public void negotiateSSLConnection(final int packLength) {
        if (!ExportControlled.enabled()) {
            throw new CJConnectionFeatureNotAvailableException();
        }
        if (!((XServerCapabilities)this.serverSession.getCapabilities()).hasCapability(XServerCapabilities.KEY_TLS)) {
            throw new CJCommunicationsException("A secure connection is required but the server is not configured with SSL.");
        }
        this.reader.stopAfterNextMessage();
        final Map<String, Object> tlsCapabilities = new HashMap<String, Object>();
        tlsCapabilities.put(XServerCapabilities.KEY_TLS, true);
        this.sendCapabilities(tlsCapabilities);
        try {
            this.socketConnection.performTlsHandshake(null);
        }
        catch (SSLParamsException | FeatureNotAvailableException | IOException ex2) {
            final Exception ex;
            final Exception e = ex;
            throw new CJCommunicationsException(e);
        }
        try {
            if (this.socketConnection.isSynchronous()) {
                this.sender = new SyncMessageSender(this.socketConnection.getMysqlOutput());
                this.reader = new SyncMessageReader(this.socketConnection.getMysqlInput());
            }
            else {
                ((AsyncMessageSender)this.sender).setChannel(this.socketConnection.getAsynchronousSocketChannel());
                this.reader.start();
            }
        }
        catch (IOException e2) {
            throw new XProtocolError(e2.getMessage(), e2);
        }
    }
    
    @Override
    public void beforeHandshake() {
        this.serverSession = new XServerSession();
        try {
            if (this.socketConnection.isSynchronous()) {
                this.sender = new SyncMessageSender(this.socketConnection.getMysqlOutput());
                this.reader = new SyncMessageReader(this.socketConnection.getMysqlInput());
                this.managedResource = this.socketConnection.getMysqlSocket();
            }
            else {
                this.sender = new AsyncMessageSender(this.socketConnection.getAsynchronousSocketChannel());
                (this.reader = new AsyncMessageReader(this.propertySet, this.socketConnection)).start();
                this.managedResource = this.socketConnection.getAsynchronousSocketChannel();
            }
        }
        catch (IOException e) {
            throw new XProtocolError(e.getMessage(), e);
        }
        this.serverSession.setCapabilities(this.readServerCapabilities());
        final String attributes = this.propertySet.getStringProperty(PropertyKey.xdevapiConnectionAttributes).getValue();
        if (attributes == null || !attributes.equalsIgnoreCase("false")) {
            final Map<String, String> attMap = this.getConnectionAttributesMap("true".equalsIgnoreCase(attributes) ? "" : attributes);
            this.clientCapabilities.put(XServerCapabilities.KEY_SESSION_CONNECT_ATTRS, attMap);
        }
        final RuntimeProperty<PropertyDefinitions.XdevapiSslMode> xdevapiSslMode = this.propertySet.getEnumProperty(PropertyKey.xdevapiSSLMode);
        if (xdevapiSslMode.isExplicitlySet()) {
            this.propertySet.getEnumProperty(PropertyKey.sslMode).setValue(PropertyDefinitions.SslMode.valueOf(xdevapiSslMode.getValue().toString()));
        }
        final RuntimeProperty<String> sslTrustStoreUrl = this.propertySet.getStringProperty(PropertyKey.xdevapiSSLTrustStoreUrl);
        if (sslTrustStoreUrl.isExplicitlySet()) {
            this.propertySet.getStringProperty(PropertyKey.trustCertificateKeyStoreUrl).setValue(sslTrustStoreUrl.getValue());
        }
        final RuntimeProperty<String> sslTrustStoreType = this.propertySet.getStringProperty(PropertyKey.xdevapiSSLTrustStoreType);
        if (sslTrustStoreType.isExplicitlySet()) {
            this.propertySet.getStringProperty(PropertyKey.trustCertificateKeyStoreType).setValue(sslTrustStoreType.getValue());
        }
        final RuntimeProperty<String> sslTrustStorePassword = this.propertySet.getStringProperty(PropertyKey.xdevapiSSLTrustStorePassword);
        if (sslTrustStorePassword.isExplicitlySet()) {
            this.propertySet.getStringProperty(PropertyKey.trustCertificateKeyStorePassword).setValue(sslTrustStorePassword.getValue());
        }
        final RuntimeProperty<PropertyDefinitions.SslMode> sslMode = this.propertySet.getEnumProperty(PropertyKey.sslMode);
        if (sslMode.getValue() == PropertyDefinitions.SslMode.PREFERRED) {
            sslMode.setValue(PropertyDefinitions.SslMode.REQUIRED);
        }
        final boolean verifyServerCert = sslMode.getValue() == PropertyDefinitions.SslMode.VERIFY_CA || sslMode.getValue() == PropertyDefinitions.SslMode.VERIFY_IDENTITY;
        final String trustStoreUrl = this.propertySet.getStringProperty(PropertyKey.trustCertificateKeyStoreUrl).getValue();
        final RuntimeProperty<String> xdevapiTlsVersions = this.propertySet.getStringProperty(PropertyKey.xdevapiTlsVersions);
        if (xdevapiTlsVersions.isExplicitlySet()) {
            if (sslMode.getValue() == PropertyDefinitions.SslMode.DISABLED) {
                throw ExceptionFactory.createException(WrongArgumentException.class, "Option '" + PropertyKey.xdevapiTlsVersions.getKeyName() + "' can not be specified when SSL connections are disabled.");
            }
            if (xdevapiTlsVersions.getValue().trim().isEmpty()) {
                throw ExceptionFactory.createException(WrongArgumentException.class, "At least one TLS protocol version must be specified in '" + PropertyKey.xdevapiTlsVersions.getKeyName() + "' list.");
            }
            final String[] tlsVersions = xdevapiTlsVersions.getValue().split("\\s*,\\s*");
            final List<String> tryProtocols = Arrays.asList(tlsVersions);
            ExportControlled.checkValidProtocols(tryProtocols);
            this.propertySet.getStringProperty(PropertyKey.enabledTLSProtocols).setValue(xdevapiTlsVersions.getValue());
        }
        final RuntimeProperty<String> xdevapiTlsCiphersuites = this.propertySet.getStringProperty(PropertyKey.xdevapiTlsCiphersuites);
        if (xdevapiTlsCiphersuites.isExplicitlySet()) {
            if (sslMode.getValue() == PropertyDefinitions.SslMode.DISABLED) {
                throw ExceptionFactory.createException(WrongArgumentException.class, "Option '" + PropertyKey.xdevapiTlsCiphersuites.getKeyName() + "' can not be specified when SSL connections are disabled.");
            }
            this.propertySet.getStringProperty(PropertyKey.enabledSSLCipherSuites).setValue(xdevapiTlsCiphersuites.getValue());
        }
        if (!verifyServerCert && !StringUtils.isNullOrEmpty(trustStoreUrl)) {
            final StringBuilder msg = new StringBuilder("Incompatible security settings. The property '");
            msg.append(PropertyKey.xdevapiSSLTrustStoreUrl.getKeyName()).append("' requires '");
            msg.append(PropertyKey.xdevapiSSLMode.getKeyName()).append("' as '");
            msg.append(PropertyDefinitions.SslMode.VERIFY_CA).append("' or '");
            msg.append(PropertyDefinitions.SslMode.VERIFY_IDENTITY).append("'.");
            throw new CJCommunicationsException(msg.toString());
        }
        if (this.clientCapabilities.size() > 0) {
            try {
                this.sendCapabilities(this.clientCapabilities);
            }
            catch (XProtocolError e2) {
                if (e2.getErrorCode() != 5002 && !e2.getMessage().contains(XServerCapabilities.KEY_SESSION_CONNECT_ATTRS)) {
                    throw e2;
                }
                this.clientCapabilities.remove(XServerCapabilities.KEY_SESSION_CONNECT_ATTRS);
            }
        }
        if (xdevapiSslMode.getValue() != PropertyDefinitions.XdevapiSslMode.DISABLED) {
            this.negotiateSSLConnection(0);
        }
    }
    
    private Map<String, String> getConnectionAttributesMap(String attStr) {
        final Map<String, String> attMap = new HashMap<String, String>();
        if (attStr != null) {
            if (attStr.startsWith("[") && attStr.endsWith("]")) {
                attStr = attStr.substring(1, attStr.length() - 1);
            }
            if (!StringUtils.isNullOrEmpty(attStr)) {
                final String[] split;
                final String[] pairs = split = attStr.split(",");
                for (final String pair : split) {
                    final String[] kv = pair.split("=");
                    final String key = kv[0].trim();
                    final String value = (kv.length > 1) ? kv[1].trim() : "";
                    if (key.startsWith("_")) {
                        throw ExceptionFactory.createException(WrongArgumentException.class, Messages.getString("Protocol.WrongAttributeName"));
                    }
                    if (attMap.put(key, value) != null) {
                        throw ExceptionFactory.createException(WrongArgumentException.class, Messages.getString("Protocol.DuplicateAttribute", new Object[] { key }));
                    }
                }
            }
        }
        attMap.put("_platform", Constants.OS_ARCH);
        attMap.put("_os", Constants.OS_NAME + "-" + Constants.OS_VERSION);
        attMap.put("_client_name", "MySQL Connector/J");
        attMap.put("_client_version", "8.0.19");
        attMap.put("_client_license", "GPL");
        attMap.put("_runtime_version", Constants.JVM_VERSION);
        attMap.put("_runtime_vendor", Constants.JVM_VENDOR);
        return attMap;
    }
    
    @Override
    public void connect(final String user, final String password, final String database) {
        this.currUser = user;
        this.currPassword = password;
        this.currDatabase = database;
        this.beforeHandshake();
        this.authProvider.connect(null, user, password, database);
    }
    
    @Override
    public void changeUser(final String user, final String password, final String database) {
        this.currUser = user;
        this.currPassword = password;
        this.currDatabase = database;
        this.authProvider.changeUser(null, user, password, database);
    }
    
    @Override
    public void afterHandshake() {
        this.initServerSession();
    }
    
    @Override
    public void configureTimezone() {
    }
    
    @Override
    public void initServerSession() {
        this.configureTimezone();
        this.send(this.messageBuilder.buildSqlStatement("select @@mysqlx_max_allowed_packet"), 0);
        final ColumnDefinition metadata = this.readMetadata();
        final long count = new XProtocolRowInputStream(metadata, this, null).next().getValue(0, (ValueFactory<Long>)new LongValueFactory(this.propertySet));
        this.readQueryResult((ResultBuilder<QueryResult>)new StatementExecuteOkBuilder());
        this.setMaxAllowedPacket((int)count);
    }
    
    public void readAuthenticateOk() {
        try {
            final XMessage mess = this.reader.readMessage(null, 4);
            if (mess != null && mess.getNotices() != null) {
                for (final Notice notice : mess.getNotices()) {
                    if (notice instanceof Notice.XSessionStateChanged) {
                        switch (((Notice.XSessionStateChanged)notice).getParamType()) {
                            case 11: {
                                this.getServerSession().setThreadId(((Notice.XSessionStateChanged)notice).getValue().getVUnsignedInt());
                            }
                            case 2: {
                                continue;
                            }
                        }
                    }
                }
            }
        }
        catch (IOException e) {
            throw new XProtocolError(e.getMessage(), e);
        }
    }
    
    public byte[] readAuthenticateContinue() {
        try {
            final MysqlxSession.AuthenticateContinue msg = (MysqlxSession.AuthenticateContinue)this.reader.readMessage(null, 3).getMessage();
            final byte[] data = msg.getAuthData().toByteArray();
            if (data.length != 20) {
                throw AssertionFailedException.shouldNotHappen("Salt length should be 20, but is " + data.length);
            }
            return data;
        }
        catch (IOException e) {
            throw new XProtocolError(e.getMessage(), e);
        }
    }
    
    public boolean hasMoreResults() {
        try {
            final XMessageHeader header;
            if ((header = this.reader.readHeader()).getMessageType() == 16) {
                this.reader.readMessage(null, header);
                return this.reader.readHeader().getMessageType() != 14;
            }
            return false;
        }
        catch (IOException e) {
            throw new XProtocolError(e.getMessage(), e);
        }
    }
    
    @Override
    public <T extends QueryResult> T readQueryResult(final ResultBuilder<T> resultBuilder) {
        try {
            XMessage mess;
            Class<? extends GeneratedMessageV3> msgClass;
            for (boolean done = false; !done; done = resultBuilder.addProtocolEntity((ProtocolEntity)this.messageToProtocolEntityFactory.get(msgClass).createFromMessage(mess))) {
                final XMessageHeader header = this.reader.readHeader();
                mess = this.reader.readMessage(null, header);
                msgClass = (Class<? extends GeneratedMessageV3>)mess.getMessage().getClass();
                if (Mysqlx.Error.class.equals(msgClass)) {
                    throw new XProtocolError(Mysqlx.Error.class.cast(mess.getMessage()));
                }
                if (!this.messageToProtocolEntityFactory.containsKey(msgClass)) {
                    throw new WrongArgumentException("Unhandled msg class (" + msgClass + ") + msg=" + mess.getMessage());
                }
                final List<Notice> notices;
                if ((notices = mess.getNotices()) != null) {
                    notices.stream().forEach((Consumer<? super Object>)resultBuilder::addProtocolEntity);
                }
            }
            return resultBuilder.build();
        }
        catch (IOException e) {
            throw new XProtocolError(e.getMessage(), e);
        }
    }
    
    public boolean hasResults() {
        try {
            return this.reader.readHeader().getMessageType() == 12;
        }
        catch (IOException e) {
            throw new XProtocolError(e.getMessage(), e);
        }
    }
    
    public void drainRows() {
        try {
            XMessageHeader header;
            while ((header = this.reader.readHeader()).getMessageType() == 13) {
                this.reader.readMessage(null, header);
            }
        }
        catch (XProtocolError e) {
            this.currentResultStreamer = null;
            throw e;
        }
        catch (IOException e2) {
            this.currentResultStreamer = null;
            throw new XProtocolError(e2.getMessage(), e2);
        }
    }
    
    @Override
    public ColumnDefinition readMetadata() {
        try {
            final List<MysqlxResultset.ColumnMetaData> fromServer = new LinkedList<MysqlxResultset.ColumnMetaData>();
            do {
                fromServer.add((MysqlxResultset.ColumnMetaData)this.reader.readMessage(null, 12).getMessage());
            } while (this.reader.readHeader().getMessageType() == 12);
            final ArrayList<Field> metadata = new ArrayList<Field>(fromServer.size());
            final ProtocolEntityFactory<Field, XMessage> fieldFactory = this.messageToProtocolEntityFactory.get(MysqlxResultset.ColumnMetaData.class);
            fromServer.forEach(col -> metadata.add(fieldFactory.createFromMessage(new XMessage(col))));
            return new DefaultColumnDefinition(metadata.toArray(new Field[0]));
        }
        catch (IOException e) {
            throw new XProtocolError(e.getMessage(), e);
        }
    }
    
    public ColumnDefinition readMetadata(final Field f, final Consumer<Notice> noticeConsumer) {
        try {
            final List<MysqlxResultset.ColumnMetaData> fromServer = new LinkedList<MysqlxResultset.ColumnMetaData>();
            while (this.reader.readHeader().getMessageType() == 12) {
                final XMessage mess = this.reader.readMessage(null, 12);
                final List<Notice> notices;
                if (noticeConsumer != null && (notices = mess.getNotices()) != null) {
                    notices.stream().forEach(noticeConsumer::accept);
                }
                fromServer.add((MysqlxResultset.ColumnMetaData)mess.getMessage());
            }
            final ArrayList<Field> metadata = new ArrayList<Field>(fromServer.size());
            metadata.add(f);
            final ProtocolEntityFactory<Field, XMessage> fieldFactory = this.messageToProtocolEntityFactory.get(MysqlxResultset.ColumnMetaData.class);
            fromServer.forEach(col -> metadata.add(fieldFactory.createFromMessage(new XMessage(col))));
            return new DefaultColumnDefinition(metadata.toArray(new Field[0]));
        }
        catch (IOException e) {
            throw new XProtocolError(e.getMessage(), e);
        }
    }
    
    public XProtocolRow readRowOrNull(final ColumnDefinition metadata, final Consumer<Notice> noticeConsumer) {
        try {
            final XMessageHeader header;
            if ((header = this.reader.readHeader()).getMessageType() == 13) {
                final XMessage mess = this.reader.readMessage(null, header);
                final List<Notice> notices;
                if (noticeConsumer != null && (notices = mess.getNotices()) != null) {
                    notices.stream().forEach(noticeConsumer::accept);
                }
                final XProtocolRow res = new XProtocolRow((MysqlxResultset.Row)mess.getMessage());
                res.setMetadata(metadata);
                return res;
            }
            return null;
        }
        catch (XProtocolError e) {
            this.currentResultStreamer = null;
            throw e;
        }
        catch (IOException e2) {
            this.currentResultStreamer = null;
            throw new XProtocolError(e2.getMessage(), e2);
        }
    }
    
    public boolean supportsPreparedStatements() {
        return this.supportsPreparedStatements;
    }
    
    public boolean readyForPreparingStatements() {
        if (this.retryPrepareStatementCountdown == 0) {
            return true;
        }
        --this.retryPrepareStatementCountdown;
        return false;
    }
    
    public int getNewPreparedStatementId(final PreparableStatement<?> preparableStatement) {
        if (!this.supportsPreparedStatements) {
            throw new XProtocolError("The connected MySQL server does not support prepared statements.");
        }
        final int preparedStatementId = this.preparedStatementIds.allocateSequentialId();
        this.preparableStatementFinalizerReferences.put(preparedStatementId, new PreparableStatement.PreparableStatementFinalizer(preparableStatement, this.preparableStatementRefQueue, preparedStatementId));
        return preparedStatementId;
    }
    
    public void freePreparedStatementId(final int preparedStatementId) {
        if (!this.supportsPreparedStatements) {
            throw new XProtocolError("The connected MySQL server does not support prepared statements.");
        }
        this.preparedStatementIds.releaseSequentialId(preparedStatementId);
        this.preparableStatementFinalizerReferences.remove(preparedStatementId);
    }
    
    public boolean failedPreparingStatement(final int preparedStatementId, final XProtocolError e) {
        this.freePreparedStatementId(preparedStatementId);
        if (e.getErrorCode() == 1461) {
            this.retryPrepareStatementCountdown = XProtocol.RETRY_PREPARE_STATEMENT_COUNTDOWN;
            return true;
        }
        if (e.getErrorCode() == 1047 && this.preparableStatementFinalizerReferences.isEmpty()) {
            this.supportsPreparedStatements = false;
            this.retryPrepareStatementCountdown = 0;
            this.preparedStatementIds = null;
            this.preparableStatementRefQueue = null;
            this.preparableStatementFinalizerReferences = null;
            return true;
        }
        return false;
    }
    
    protected void newCommand() {
        if (this.currentResultStreamer != null) {
            try {
                this.currentResultStreamer.finishStreaming();
            }
            finally {
                this.currentResultStreamer = null;
            }
        }
        if (this.supportsPreparedStatements) {
            Reference<? extends PreparableStatement<?>> ref;
            while ((ref = this.preparableStatementRefQueue.poll()) != null) {
                final PreparableStatement.PreparableStatementFinalizer psf = (PreparableStatement.PreparableStatementFinalizer)ref;
                psf.clear();
                try {
                    this.sender.send(((XMessageBuilder)this.messageBuilder).buildPrepareDeallocate(psf.getPreparedStatementId()));
                    this.readQueryResult((ResultBuilder<QueryResult>)new OkBuilder());
                }
                catch (XProtocolError e) {
                    if (e.getErrorCode() != 5110) {
                        throw e;
                    }
                    continue;
                }
                finally {
                    this.freePreparedStatementId(psf.getPreparedStatementId());
                }
            }
        }
    }
    
    public <M extends Message, R extends QueryResult> R query(final M message, final ResultBuilder<R> resultBuilder) {
        this.send(message, 0);
        final R res = this.readQueryResult(resultBuilder);
        if (ResultStreamer.class.isAssignableFrom(res.getClass())) {
            this.currentResultStreamer = (ResultStreamer)res;
        }
        return res;
    }
    
    public <M extends Message, R extends QueryResult> CompletableFuture<R> queryAsync(final M message, final ResultBuilder<R> resultBuilder) {
        this.newCommand();
        final CompletableFuture<R> f = new CompletableFuture<R>();
        final MessageListener<XMessage> l = new ResultMessageListener<Object>(this.messageToProtocolEntityFactory, resultBuilder, f);
        this.sender.send((XMessage)message, f, () -> this.reader.pushMessageListener(l));
        return f;
    }
    
    public boolean isOpen() {
        return this.managedResource != null;
    }
    
    @Override
    public void close() throws IOException {
        try {
            this.send(this.messageBuilder.buildClose(), 0);
            this.readQueryResult((ResultBuilder<QueryResult>)new OkBuilder());
        }
        catch (Exception ex3) {
            try {
                if (this.managedResource == null) {
                    throw new ConnectionIsClosedException();
                }
                this.managedResource.close();
                this.managedResource = null;
            }
            catch (IOException ex) {
                throw new CJCommunicationsException(ex);
            }
        }
        finally {
            try {
                if (this.managedResource == null) {
                    throw new ConnectionIsClosedException();
                }
                this.managedResource.close();
                this.managedResource = null;
            }
            catch (IOException ex2) {
                throw new CJCommunicationsException(ex2);
            }
        }
    }
    
    public boolean isSqlResultPending() {
        try {
            final XMessageHeader header;
            switch ((header = this.reader.readHeader()).getMessageType()) {
                case 12: {
                    return true;
                }
                case 16: {
                    this.reader.readMessage(null, header);
                    break;
                }
            }
            return false;
        }
        catch (IOException e) {
            throw new XProtocolError(e.getMessage(), e);
        }
    }
    
    public void setMaxAllowedPacket(final int maxAllowedPacket) {
        this.sender.setMaxAllowedPacket(maxAllowedPacket);
    }
    
    @Override
    public void send(final Message message, final int packetLen) {
        this.newCommand();
        this.sender.send((XMessage)message);
    }
    
    @Override
    public ServerCapabilities readServerCapabilities() {
        try {
            this.sender.send(((XMessageBuilder)this.messageBuilder).buildCapabilitiesGet());
            return new XServerCapabilities((Map<String, MysqlxDatatypes.Any>)((MysqlxConnection.Capabilities)this.reader.readMessage(null, 2).getMessage()).getCapabilitiesList().stream().collect(Collectors.toMap((Function<? super Object, ?>)MysqlxConnection.Capability::getName, (Function<? super Object, ?>)MysqlxConnection.Capability::getValue)));
        }
        catch (IOException | AssertionFailedException ex2) {
            final Exception ex;
            final Exception e = ex;
            throw new XProtocolError(e.getMessage(), e);
        }
    }
    
    @Override
    public void reset() {
        this.newCommand();
        this.propertySet.reset();
        if (this.useSessionResetKeepOpen == null) {
            try {
                this.send(((XMessageBuilder)this.messageBuilder).buildExpectOpen(), 0);
                this.readQueryResult((ResultBuilder<QueryResult>)new OkBuilder());
                this.useSessionResetKeepOpen = true;
            }
            catch (XProtocolError e) {
                if (e.getErrorCode() != 5168 && e.getErrorCode() != 5160) {
                    throw e;
                }
                this.useSessionResetKeepOpen = false;
            }
        }
        if (this.useSessionResetKeepOpen) {
            this.send(((XMessageBuilder)this.messageBuilder).buildSessionResetKeepOpen(), 0);
            this.readQueryResult((ResultBuilder<QueryResult>)new OkBuilder());
        }
        else {
            this.send(((XMessageBuilder)this.messageBuilder).buildSessionResetAndClose(), 0);
            this.readQueryResult((ResultBuilder<QueryResult>)new OkBuilder());
            if (this.clientCapabilities.containsKey(XServerCapabilities.KEY_SESSION_CONNECT_ATTRS)) {
                final Map<String, Object> reducedClientCapabilities = new HashMap<String, Object>();
                reducedClientCapabilities.put(XServerCapabilities.KEY_SESSION_CONNECT_ATTRS, this.clientCapabilities.get(XServerCapabilities.KEY_SESSION_CONNECT_ATTRS));
                if (reducedClientCapabilities.size() > 0) {
                    this.sendCapabilities(reducedClientCapabilities);
                }
            }
            this.authProvider.changeUser(null, this.currUser, this.currPassword, this.currDatabase);
        }
        if (this.supportsPreparedStatements) {
            this.retryPrepareStatementCountdown = 0;
            this.preparedStatementIds = new SequentialIdLease();
            this.preparableStatementRefQueue = new ReferenceQueue<PreparableStatement<?>>();
            this.preparableStatementFinalizerReferences = new TreeMap<Integer, PreparableStatement.PreparableStatementFinalizer>();
        }
    }
    
    @Override
    public ExceptionInterceptor getExceptionInterceptor() {
        throw ExceptionFactory.createException(CJOperationNotSupportedException.class, "Not supported");
    }
    
    @Override
    public void changeDatabase(final String database) {
        throw ExceptionFactory.createException(CJOperationNotSupportedException.class, "Not supported");
    }
    
    @Override
    public String getPasswordCharacterEncoding() {
        throw ExceptionFactory.createException(CJOperationNotSupportedException.class, "Not supported");
    }
    
    @Override
    public boolean versionMeetsMinimum(final int major, final int minor, final int subminor) {
        throw ExceptionFactory.createException(CJOperationNotSupportedException.class, "Not supported");
    }
    
    @Override
    public XMessage readMessage(final XMessage reuse) {
        throw ExceptionFactory.createException(CJOperationNotSupportedException.class, "Not supported");
    }
    
    @Override
    public XMessage checkErrorMessage() {
        throw ExceptionFactory.createException(CJOperationNotSupportedException.class, "Not supported");
    }
    
    @Override
    public XMessage sendCommand(final Message queryPacket, final boolean skipCheck, final int timeoutMillis) {
        throw ExceptionFactory.createException(CJOperationNotSupportedException.class, "Not supported");
    }
    
    @Override
    public <T extends ProtocolEntity> T read(final Class<T> requiredClass, final ProtocolEntityFactory<T, XMessage> protocolEntityFactory) throws IOException {
        throw ExceptionFactory.createException(CJOperationNotSupportedException.class, "Not supported");
    }
    
    @Override
    public <T extends ProtocolEntity> T read(final Class<Resultset> requiredClass, final int maxRows, final boolean streamResults, final XMessage resultPacket, final boolean isBinaryEncoded, final ColumnDefinition metadata, final ProtocolEntityFactory<T, XMessage> protocolEntityFactory) throws IOException {
        throw ExceptionFactory.createException(CJOperationNotSupportedException.class, "Not supported");
    }
    
    @Override
    public void setLocalInfileInputStream(final InputStream stream) {
        throw ExceptionFactory.createException(CJOperationNotSupportedException.class, "Not supported");
    }
    
    @Override
    public InputStream getLocalInfileInputStream() {
        throw ExceptionFactory.createException(CJOperationNotSupportedException.class, "Not supported");
    }
    
    @Override
    public String getQueryComment() {
        throw ExceptionFactory.createException(CJOperationNotSupportedException.class, "Not supported");
    }
    
    @Override
    public void setQueryComment(final String comment) {
        throw ExceptionFactory.createException(CJOperationNotSupportedException.class, "Not supported");
    }
    
    static {
        XProtocol.RETRY_PREPARE_STATEMENT_COUNTDOWN = 100;
        XProtocol.COLLATION_NAME_TO_COLLATION_INDEX = new HashMap<String, Integer>();
        for (int i = 0; i < CharsetMapping.COLLATION_INDEX_TO_COLLATION_NAME.length; ++i) {
            XProtocol.COLLATION_NAME_TO_COLLATION_INDEX.put(CharsetMapping.COLLATION_INDEX_TO_COLLATION_NAME[i], i);
        }
    }
}
