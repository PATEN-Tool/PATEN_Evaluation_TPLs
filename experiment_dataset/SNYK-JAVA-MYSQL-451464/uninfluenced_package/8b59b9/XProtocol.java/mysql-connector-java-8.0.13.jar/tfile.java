// 
// Decompiled by Procyon v0.5.36
// 

package com.mysql.cj.protocol.x;

import com.mysql.cj.protocol.AuthenticationProvider;
import com.mysql.cj.MessageBuilder;
import com.mysql.cj.CharsetMapping;
import java.util.HashMap;
import com.mysql.cj.result.RowList;
import java.io.InputStream;
import com.mysql.cj.protocol.Resultset;
import com.mysql.cj.protocol.ProtocolEntity;
import com.mysql.cj.exceptions.ExceptionFactory;
import com.mysql.cj.exceptions.CJOperationNotSupportedException;
import com.mysql.cj.x.protobuf.MysqlxDatatypes;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.mysql.cj.x.protobuf.MysqlxConnection;
import com.mysql.cj.protocol.ServerCapabilities;
import com.mysql.cj.exceptions.ConnectionIsClosedException;
import com.mysql.cj.protocol.ResultListener;
import com.mysql.cj.xdevapi.FilterParams;
import java.nio.channels.CompletionHandler;
import com.mysql.cj.protocol.MessageListener;
import com.mysql.cj.xdevapi.SqlResult;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import com.mysql.cj.result.DefaultColumnDefinition;
import java.util.ArrayList;
import com.mysql.cj.x.protobuf.MysqlxResultset;
import java.util.LinkedList;
import com.mysql.cj.QueryResult;
import com.mysql.cj.exceptions.AssertionFailedException;
import com.mysql.cj.x.protobuf.MysqlxSession;
import com.mysql.cj.exceptions.WrongArgumentException;
import java.util.Optional;
import com.mysql.cj.protocol.ColumnDefinition;
import com.mysql.cj.result.ValueFactory;
import com.mysql.cj.result.LongValueFactory;
import com.mysql.cj.protocol.Message;
import com.mysql.cj.conf.PropertyDefinitions;
import java.io.IOException;
import com.mysql.cj.exceptions.FeatureNotAvailableException;
import com.mysql.cj.exceptions.SSLParamsException;
import com.mysql.cj.exceptions.CJCommunicationsException;
import com.mysql.cj.exceptions.CJConnectionFeatureNotAvailableException;
import com.mysql.cj.protocol.ExportControlled;
import com.mysql.cj.protocol.ServerSession;
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
import com.mysql.cj.conf.PropertySet;
import java.util.Map;
import com.mysql.cj.protocol.ResultStreamer;
import com.mysql.cj.result.Field;
import com.mysql.cj.protocol.ProtocolEntityFactory;
import java.io.Closeable;
import com.mysql.cj.protocol.MessageSender;
import com.mysql.cj.protocol.MessageReader;
import com.mysql.cj.protocol.Protocol;
import com.mysql.cj.protocol.AbstractProtocol;

public class XProtocol extends AbstractProtocol<XMessage> implements Protocol<XMessage>
{
    private MessageReader<XMessageHeader, XMessage> reader;
    private MessageSender<XMessage> sender;
    private Closeable managedResource;
    private ProtocolEntityFactory<Field, XMessage> fieldFactory;
    private ProtocolEntityFactory<Notice, XMessage> noticeFactory;
    private String metadataCharacterSet;
    private ResultStreamer currentResultStreamer;
    XServerSession serverSession;
    public String defaultSchemaName;
    private String currUser;
    private String currPassword;
    private String currDatabase;
    public static Map<String, Integer> COLLATION_NAME_TO_COLLATION_INDEX;
    
    public XProtocol(final String host, final int port, final String defaultSchema, final PropertySet propertySet) {
        this.serverSession = null;
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
    public void init(final Session sess, final SocketConnection socketConn, final PropertySet propSet, final TransactionEventHandler transactionManager) {
        this.socketConnection = socketConn;
        this.propertySet = propSet;
        this.messageBuilder = (MessageBuilder<M>)new XMessageBuilder();
        (this.authProvider = (AuthenticationProvider<M>)new XAuthenticationProvider()).init((Protocol<M>)this, propSet, null);
        this.metadataCharacterSet = "latin1";
        this.fieldFactory = new FieldFactory(this.metadataCharacterSet);
        this.noticeFactory = new NoticeFactory();
    }
    
    @Override
    public ServerSession getServerSession() {
        return this.serverSession;
    }
    
    public void setCapability(final String name, final Object value) {
        ((XServerCapabilities)this.getServerSession().getCapabilities()).setCapability(name, value);
        this.sender.send(((XMessageBuilder)this.messageBuilder).buildCapabilitiesSet(name, value));
        this.readOk();
    }
    
    @Override
    public void negotiateSSLConnection(final int packLength) {
        if (!ExportControlled.enabled()) {
            throw new CJConnectionFeatureNotAvailableException();
        }
        if (!((XServerCapabilities)this.serverSession.getCapabilities()).hasCapability("tls")) {
            throw new CJCommunicationsException("A secure connection is required but the server is not configured with SSL.");
        }
        this.reader.stopAfterNextMessage();
        this.setCapability("tls", true);
        try {
            this.socketConnection.performTlsHandshake(null);
        }
        catch (SSLParamsException | FeatureNotAvailableException | IOException ex2) {
            final Exception ex;
            final Exception e = ex;
            throw new CJCommunicationsException(e);
        }
        if (this.socketConnection.isSynchronous()) {
            this.sender = new SyncMessageSender(this.socketConnection.getMysqlOutput());
            this.reader = new SyncMessageReader(this.socketConnection.getMysqlInput());
        }
        else {
            ((AsyncMessageSender)this.sender).setChannel(this.socketConnection.getAsynchronousSocketChannel());
            this.reader.start();
        }
    }
    
    @Override
    public void beforeHandshake() {
        this.serverSession = new XServerSession();
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
        this.serverSession.setCapabilities(this.readServerCapabilities());
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
        if (!verifyServerCert && !StringUtils.isNullOrEmpty(trustStoreUrl)) {
            final StringBuilder msg = new StringBuilder("Incompatible security settings. The property '");
            msg.append(PropertyKey.xdevapiSSLTrustStoreUrl.getKeyName()).append("' requires '");
            msg.append(PropertyKey.xdevapiSSLMode.getKeyName()).append("' as '");
            msg.append(PropertyDefinitions.SslMode.VERIFY_CA).append("' or '");
            msg.append(PropertyDefinitions.SslMode.VERIFY_IDENTITY).append("'.");
            throw new CJCommunicationsException(msg.toString());
        }
        if (xdevapiSslMode.getValue() != PropertyDefinitions.XdevapiSslMode.DISABLED) {
            this.negotiateSSLConnection(0);
        }
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
        final long count = this.getRowInputStream(metadata).next().getValue(0, (ValueFactory<Long>)new LongValueFactory());
        this.readQueryResult();
        this.setMaxAllowedPacket((int)count);
    }
    
    public void readOk() {
        try {
            XMessageHeader header;
            while ((header = this.reader.readHeader()).getMessageType() == 11) {
                this.reader.readMessage(null, header);
            }
            this.reader.readMessage(null, 0);
        }
        catch (IOException e) {
            throw new XProtocolError(e.getMessage(), e);
        }
    }
    
    public void readAuthenticateOk() {
        try {
            XMessageHeader header;
            while ((header = this.reader.readHeader()).getMessageType() == 11) {
                final Notice notice = this.noticeFactory.createFromMessage(this.reader.readMessage(null, header));
                if (notice.getType() != 3) {
                    throw new WrongArgumentException("Unknown notice received during authentication: " + notice);
                }
                switch (notice.getParamType()) {
                    case 11: {
                        this.getServerSession().setThreadId(notice.getValue().getVUnsignedInt());
                        continue;
                    }
                    default: {
                        throw new WrongArgumentException("Unknown SessionStateChanged notice received during authentication: " + notice.getParamType());
                    }
                }
            }
            this.reader.readMessage(null, 4);
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
    public <QR extends QueryResult> QR readQueryResult() {
        try {
            final StatementExecuteOkBuilder builder = new StatementExecuteOkBuilder();
            XMessageHeader header;
            if ((header = this.reader.readHeader()).getMessageType() == 14) {
                this.reader.readMessage(null, header);
            }
            while ((header = this.reader.readHeader()).getMessageType() == 11) {
                builder.addNotice(this.noticeFactory.createFromMessage(this.reader.readMessage(null, header)));
            }
            this.reader.readMessage(null, 17);
            return (QR)builder.build();
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
        catch (IOException e) {
            throw new XProtocolError(e.getMessage(), e);
        }
    }
    
    @Override
    public ColumnDefinition readMetadata() {
        try {
            XMessageHeader header;
            while ((header = this.reader.readHeader()).getMessageType() == 11) {
                this.reader.readMessage(null, header);
            }
            final List<MysqlxResultset.ColumnMetaData> fromServer = new LinkedList<MysqlxResultset.ColumnMetaData>();
            do {
                fromServer.add((MysqlxResultset.ColumnMetaData)this.reader.readMessage(null, 12).getMessage());
            } while (this.reader.readHeader().getMessageType() == 12);
            final ArrayList<Field> metadata = new ArrayList<Field>(fromServer.size());
            fromServer.forEach(col -> metadata.add(this.fieldFactory.createFromMessage(new XMessage(col))));
            return new DefaultColumnDefinition(metadata.toArray(new Field[0]));
        }
        catch (IOException e) {
            throw new XProtocolError(e.getMessage(), e);
        }
    }
    
    public XProtocolRow readRowOrNull(final ColumnDefinition metadata) {
        try {
            final XMessageHeader header;
            if ((header = this.reader.readHeader()).getMessageType() == 13) {
                final MysqlxResultset.Row r = (MysqlxResultset.Row)this.reader.readMessage(null, header).getMessage();
                return new XProtocolRow(metadata, r);
            }
            return null;
        }
        catch (IOException e) {
            throw new XProtocolError(e.getMessage(), e);
        }
    }
    
    @Override
    public XProtocolRowInputStream getRowInputStream(final ColumnDefinition metadata) {
        return new XProtocolRowInputStream(metadata, this);
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
    }
    
    @Override
    public void setCurrentResultStreamer(final ResultStreamer currentResultStreamer) {
        this.currentResultStreamer = currentResultStreamer;
    }
    
    public CompletableFuture<SqlResult> asyncExecuteSql(final String sql, final List<Object> args) {
        this.newCommand();
        final CompletableFuture<SqlResult> f = new CompletableFuture<SqlResult>();
        final MessageListener<XMessage> l = new SqlResultMessageListener(f, this.fieldFactory, this.noticeFactory, this.serverSession.getDefaultTimeZone());
        final CompletionHandler<Long, Void> resultHandler = new ErrorToFutureCompletionHandler<Long>(f, () -> this.reader.pushMessageListener(l));
        this.sender.send((XMessage)this.messageBuilder.buildSqlStatement(sql, args), resultHandler);
        return f;
    }
    
    public void asyncFind(final FilterParams filterParams, final ResultListener<StatementExecuteOk> callbacks, final CompletableFuture<?> errorFuture) {
        this.newCommand();
        final MessageListener<XMessage> l = new ResultMessageListener(this.fieldFactory, this.noticeFactory, callbacks);
        final CompletionHandler<Long, Void> resultHandler = new ErrorToFutureCompletionHandler<Long>(errorFuture, () -> this.reader.pushMessageListener(l));
        this.sender.send(((XMessageBuilder)this.messageBuilder).buildFind(filterParams), resultHandler);
    }
    
    public boolean isOpen() {
        return this.managedResource != null;
    }
    
    @Override
    public void close() throws IOException {
        try {
            this.send(this.messageBuilder.buildClose(), 0);
            this.readOk();
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
    public <RES extends QueryResult> CompletableFuture<RES> sendAsync(final Message message) {
        this.newCommand();
        final CompletableFuture<StatementExecuteOk> f = new CompletableFuture<StatementExecuteOk>();
        final StatementExecuteOkMessageListener l = new StatementExecuteOkMessageListener(f, this.noticeFactory);
        final CompletionHandler<Long, Void> resultHandler = new ErrorToFutureCompletionHandler<Long>(f, () -> this.reader.pushMessageListener(l));
        this.sender.send((XMessage)message, resultHandler);
        return (CompletableFuture<RES>)f;
    }
    
    @Override
    public ServerCapabilities readServerCapabilities() {
        try {
            this.sender.send(((XMessageBuilder)this.messageBuilder).buildCapabilitiesGet());
            return new XServerCapabilities((Map<String, MysqlxDatatypes.Any>)((MysqlxConnection.Capabilities)this.reader.readMessage(null, 2).getMessage()).getCapabilitiesList().stream().collect(Collectors.toMap((Function<? super Object, ?>)MysqlxConnection.Capability::getName, (Function<? super Object, ?>)MysqlxConnection.Capability::getValue)));
        }
        catch (IOException e) {
            throw new XProtocolError(e.getMessage(), e);
        }
    }
    
    @Override
    public void reset() {
        this.newCommand();
        this.propertySet.reset();
        this.send(((XMessageBuilder)this.messageBuilder).buildSessionReset(), 0);
        this.readOk();
        this.authProvider.changeUser(null, this.currUser, this.currPassword, this.currDatabase);
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
        XProtocol.COLLATION_NAME_TO_COLLATION_INDEX = new HashMap<String, Integer>();
        for (int i = 0; i < CharsetMapping.COLLATION_INDEX_TO_COLLATION_NAME.length; ++i) {
            XProtocol.COLLATION_NAME_TO_COLLATION_INDEX.put(CharsetMapping.COLLATION_INDEX_TO_COLLATION_NAME[i], i);
        }
    }
}
