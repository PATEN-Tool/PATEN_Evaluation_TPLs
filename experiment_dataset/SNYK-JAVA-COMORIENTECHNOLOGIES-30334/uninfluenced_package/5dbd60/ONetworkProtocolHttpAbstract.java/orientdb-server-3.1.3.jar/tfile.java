// 
// Decompiled by Procyon v0.5.36
// 

package com.orientechnologies.orient.server.network.protocol.http;

import java.util.Optional;
import java.net.InetSocketAddress;
import com.orientechnologies.orient.server.config.OServerCommandConfiguration;
import com.orientechnologies.orient.server.network.protocol.http.command.get.OServerCommandGetPing;
import com.orientechnologies.orient.server.network.protocol.http.command.get.OServerCommandGetSSO;
import com.orientechnologies.orient.server.network.protocol.http.command.post.OServerCommandPostAuthToken;
import com.orientechnologies.orient.server.network.protocol.http.command.get.OServerCommandGetSupportedLanguages;
import com.orientechnologies.orient.server.network.protocol.http.command.post.OServerCommandPostKillDbConnection;
import com.orientechnologies.orient.server.network.protocol.http.command.all.OServerCommandFunction;
import com.orientechnologies.orient.server.network.protocol.http.command.options.OServerCommandOptions;
import com.orientechnologies.orient.server.network.protocol.http.command.delete.OServerCommandDeleteIndex;
import com.orientechnologies.orient.server.network.protocol.http.command.delete.OServerCommandDeleteProperty;
import com.orientechnologies.orient.server.network.protocol.http.command.delete.OServerCommandDeleteDocument;
import com.orientechnologies.orient.server.network.protocol.http.command.delete.OServerCommandDeleteDatabase;
import com.orientechnologies.orient.server.network.protocol.http.command.delete.OServerCommandDeleteClass;
import com.orientechnologies.orient.server.network.protocol.http.command.put.OServerCommandPutIndex;
import com.orientechnologies.orient.server.network.protocol.http.command.put.OServerCommandPutDocument;
import com.orientechnologies.orient.server.network.protocol.http.command.post.OServerCommandPostStudio;
import com.orientechnologies.orient.server.network.protocol.http.command.post.OServerCommandPostServer;
import com.orientechnologies.orient.server.network.protocol.http.command.put.OServerCommandPostConnection;
import com.orientechnologies.orient.server.network.protocol.http.command.post.OServerCommandPostProperty;
import com.orientechnologies.orient.server.network.protocol.http.command.post.OServerCommandPostImportRecords;
import com.orientechnologies.orient.server.network.protocol.http.command.post.OServerCommandPostDocument;
import com.orientechnologies.orient.server.network.protocol.http.command.post.OServerCommandPostInstallDatabase;
import com.orientechnologies.orient.server.network.protocol.http.command.post.OServerCommandPostDatabase;
import com.orientechnologies.orient.server.network.protocol.http.command.post.OServerCommandPostCommandGraph;
import com.orientechnologies.orient.server.network.protocol.http.command.post.OServerCommandPostClass;
import com.orientechnologies.orient.server.network.protocol.http.command.post.OServerCommandPostBatch;
import com.orientechnologies.orient.server.network.protocol.http.command.patch.OServerCommandPatchDocument;
import com.orientechnologies.orient.server.network.protocol.http.command.get.OServerCommandGetExportDatabase;
import com.orientechnologies.orient.server.network.protocol.http.command.get.OServerCommandIsEnterprise;
import com.orientechnologies.orient.server.network.protocol.http.command.get.OServerCommandGetListDatabases;
import com.orientechnologies.orient.server.network.protocol.http.command.get.OServerCommandGetIndex;
import com.orientechnologies.orient.server.network.protocol.http.command.get.OServerCommandGetFileDownload;
import com.orientechnologies.orient.server.network.protocol.http.command.get.OServerCommandGetStorageAllocation;
import com.orientechnologies.orient.server.network.protocol.http.command.get.OServerCommandGetConnections;
import com.orientechnologies.orient.server.network.protocol.http.command.get.OServerCommandGetServerVersion;
import com.orientechnologies.orient.server.network.protocol.http.command.get.OServerCommandGetServer;
import com.orientechnologies.orient.server.network.protocol.http.command.get.OServerCommandGetQuery;
import com.orientechnologies.orient.server.network.protocol.http.command.get.OServerCommandGetDocumentByClass;
import com.orientechnologies.orient.server.network.protocol.http.command.get.OServerCommandGetDocument;
import com.orientechnologies.orient.server.network.protocol.http.command.get.OServerCommandGetDictionary;
import com.orientechnologies.orient.server.network.protocol.http.command.get.OServerCommandGetDatabase;
import com.orientechnologies.orient.server.network.protocol.http.command.get.OServerCommandGetCluster;
import com.orientechnologies.orient.server.network.protocol.http.command.get.OServerCommandGetClass;
import com.orientechnologies.orient.server.network.protocol.http.command.get.OServerCommandGetDisconnect;
import com.orientechnologies.orient.server.network.protocol.http.command.get.OServerCommandGetConnect;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.io.ByteArrayInputStream;
import com.orientechnologies.orient.core.Orient;
import java.net.SocketTimeoutException;
import java.net.SocketException;
import java.util.Locale;
import com.orientechnologies.orient.server.network.protocol.http.multipart.OHttpMultipartBaseInputStream;
import java.util.Base64;
import com.orientechnologies.orient.core.serialization.serializer.OStringSerializerHelper;
import java.util.Date;
import java.util.ArrayList;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQLParsingException;
import com.orientechnologies.orient.core.exception.OCommandExecutionException;
import com.orientechnologies.orient.core.exception.OSecurityAccessException;
import com.orientechnologies.orient.core.exception.ODatabaseException;
import com.orientechnologies.common.concur.lock.OLockException;
import com.orientechnologies.orient.core.exception.OConcurrentModificationException;
import com.orientechnologies.orient.core.exception.ORecordNotFoundException;
import java.util.InputMismatchException;
import java.util.IllegalFormatException;
import com.orientechnologies.orient.enterprise.channel.OChannel;
import com.orientechnologies.orient.core.sql.executor.OResultSet;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.function.Function;
import com.orientechnologies.orient.core.sql.executor.OInternalExecutionPlan;
import java.util.List;
import com.orientechnologies.orient.core.db.ODatabaseDocumentInternal;
import com.orientechnologies.orient.enterprise.channel.binary.ONetworkProtocolException;
import java.util.Iterator;
import com.orientechnologies.orient.server.OClientConnectionStats;
import com.orientechnologies.common.log.OLogManager;
import java.net.URLDecoder;
import java.util.Map;
import java.util.HashMap;
import com.orientechnologies.orient.server.network.protocol.http.command.OServerCommand;
import java.io.IOException;
import com.orientechnologies.orient.server.plugin.OServerPluginHelper;
import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import java.net.Socket;
import com.orientechnologies.orient.server.network.OServerNetworkListener;
import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.core.config.OContextConfiguration;
import com.orientechnologies.orient.core.metadata.security.OUser;
import com.orientechnologies.orient.enterprise.channel.text.OChannelTextServer;
import com.orientechnologies.orient.server.OClientConnection;
import java.nio.charset.Charset;
import com.orientechnologies.orient.server.network.protocol.ONetworkProtocol;

public abstract class ONetworkProtocolHttpAbstract extends ONetworkProtocol implements ONetworkHttpExecutor
{
    private static final String COMMAND_SEPARATOR = "|";
    private static final Charset utf8;
    private static int requestMaxContentLength;
    private static int socketTimeout;
    private final StringBuilder requestContent;
    protected OClientConnection connection;
    protected OChannelTextServer channel;
    protected OUser account;
    protected OHttpRequest request;
    protected OHttpResponse response;
    protected OHttpNetworkCommandManager cmdManager;
    private String responseCharSet;
    private boolean jsonResponseError;
    private boolean sameSiteCookie;
    private String[] additionalResponseHeaders;
    private String listeningAddress;
    private OContextConfiguration configuration;
    
    public ONetworkProtocolHttpAbstract(final OServer server) {
        super(server.getThreadGroup(), "IO-HTTP");
        this.requestContent = new StringBuilder(512);
        this.listeningAddress = "?";
    }
    
    @Override
    public void config(final OServerNetworkListener iListener, final OServer iServer, final Socket iSocket, final OContextConfiguration iConfiguration) throws IOException {
        this.configuration = iConfiguration;
        final boolean installDefaultCommands = iConfiguration.getValueAsBoolean(OGlobalConfiguration.NETWORK_HTTP_INSTALL_DEFAULT_COMMANDS);
        if (installDefaultCommands) {
            this.registerStatelessCommands(iListener);
        }
        final String addHeaders = iConfiguration.getValueAsString("network.http.additionalResponseHeaders", (String)null);
        if (addHeaders != null) {
            this.additionalResponseHeaders = addHeaders.split(";");
        }
        this.connection = iServer.getClientConnectionManager().connect(this);
        this.server = iServer;
        ONetworkProtocolHttpAbstract.requestMaxContentLength = iConfiguration.getValueAsInteger(OGlobalConfiguration.NETWORK_HTTP_MAX_CONTENT_LENGTH);
        ONetworkProtocolHttpAbstract.socketTimeout = iConfiguration.getValueAsInteger(OGlobalConfiguration.NETWORK_SOCKET_TIMEOUT);
        this.responseCharSet = iConfiguration.getValueAsString(OGlobalConfiguration.NETWORK_HTTP_CONTENT_CHARSET);
        this.jsonResponseError = iConfiguration.getValueAsBoolean(OGlobalConfiguration.NETWORK_HTTP_JSON_RESPONSE_ERROR);
        this.sameSiteCookie = iConfiguration.getValueAsBoolean(OGlobalConfiguration.NETWORK_HTTP_SESSION_COOKIE_SAME_SITE);
        (this.channel = new OChannelTextServer(iSocket, iConfiguration)).connected();
        this.connection.getData().caller = this.channel.toString();
        this.listeningAddress = this.getListeningAddress();
        OServerPluginHelper.invokeHandlerCallbackOnSocketAccepted(this.server, this);
        this.start();
    }
    
    public void service() throws ONetworkProtocolException, IOException {
        final OClientConnectionStats stats = this.connection.getStats();
        ++stats.totalRequests;
        this.connection.getData().commandInfo = null;
        this.connection.getData().commandDetail = null;
        String callbackF;
        if (this.server.getContextConfiguration().getValueAsBoolean(OGlobalConfiguration.NETWORK_HTTP_JSONP_ENABLED) && this.request.getParameters() != null && this.request.getParameters().containsKey("callback")) {
            callbackF = this.request.getParameters().get("callback");
        }
        else {
            callbackF = null;
        }
        (this.response = new OHttpResponseImpl(this.channel.outStream, this.request.getHttpVersion(), this.additionalResponseHeaders, this.responseCharSet, this.connection.getData().serverInfo, this.request.getSessionId(), callbackF, this.request.isKeepAlive(), this.connection, this.server.getContextConfiguration())).setJsonErrorResponse(this.jsonResponseError);
        this.response.setSameSiteCookie(this.sameSiteCookie);
        if (this.request.getContentEncoding() != null && this.request.getContentEncoding().equals("gzip")) {
            this.response.setContentEncoding("gzip");
        }
        if (this.request.getContentEncoding() != null && this.request.getContentEncoding().contains("gzip")) {
            this.response.setStaticEncoding("gzip");
        }
        final long begin = System.currentTimeMillis();
        boolean isChain;
        do {
            isChain = false;
            String command;
            if (this.request.getUrl().length() < 2) {
                command = "";
            }
            else {
                command = this.request.getUrl().substring(1);
            }
            final String commandString = getCommandString(command, this.request.getHttpMethod());
            final OServerCommand cmd = (OServerCommand)this.cmdManager.getCommand(commandString);
            final Map<String, String> requestParams = this.cmdManager.extractUrlTokens(commandString);
            if (requestParams != null) {
                if (this.request.getParameters() == null) {
                    this.request.setParameters(new HashMap<String, String>());
                }
                for (final Map.Entry<String, String> entry : requestParams.entrySet()) {
                    this.request.getParameters().put(entry.getKey(), URLDecoder.decode(entry.getValue(), "UTF-8"));
                }
            }
            if (cmd != null) {
                try {
                    if (!cmd.beforeExecute(this.request, this.response)) {
                        continue;
                    }
                    try {
                        isChain = cmd.execute(this.request, this.response);
                    }
                    finally {
                        cmd.afterExecute(this.request, this.response);
                    }
                }
                catch (Exception e) {
                    this.handleError(e, this.request);
                }
            }
            else {
                try {
                    OLogManager.instance().warn((Object)this, "->" + this.channel.socket.getInetAddress().getHostAddress() + ": Command not found: " + this.request.getHttpMethod() + "." + URLDecoder.decode(command, "UTF-8"), new Object[0]);
                    this.sendError(405, "Method Not Allowed", null, "text/plain", "Command not found: " + command, this.request.isKeepAlive());
                }
                catch (IOException e2) {
                    this.sendShutdown();
                }
            }
        } while (isChain);
        this.connection.getStats().lastCommandInfo = this.connection.getData().commandInfo;
        this.connection.getStats().lastCommandDetail = this.connection.getData().commandDetail;
        this.connection.getStats().activeQueries = this.getActiveQueries(this.connection.getDatabase());
        this.connection.getStats().lastCommandExecutionTime = System.currentTimeMillis() - begin;
        final OClientConnectionStats stats2 = this.connection.getStats();
        stats2.totalCommandExecutionTime += this.connection.getStats().lastCommandExecutionTime;
        OServerPluginHelper.invokeHandlerCallbackOnAfterClientRequest(this.server, this.connection, (byte)(-1));
    }
    
    private List<String> getActiveQueries(final ODatabaseDocumentInternal database) {
        if (database == null) {
            return null;
        }
        try {
            final Map<String, OResultSet> queries = (Map<String, OResultSet>)database.getActiveQueries();
            return queries.values().stream().map(x -> x.getExecutionPlan()).filter(x -> x.isPresent() && x.get() instanceof OInternalExecutionPlan).map((Function<? super Object, ?>)OInternalExecutionPlan.class::cast).map(x -> x.getStatement()).collect((Collector<? super Object, ?, List<String>>)Collectors.toList());
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public void sendShutdown() {
        super.sendShutdown();
        try {
            if (this.channel.socket != null) {
                this.channel.socket.close();
            }
        }
        catch (Exception ex) {}
    }
    
    public void shutdown() {
        try {
            this.sendShutdown();
            this.channel.close();
        }
        finally {
            this.server.getClientConnectionManager().disconnect(this.connection.getId());
            OServerPluginHelper.invokeHandlerCallbackOnSocketDestroyed(this.server, this);
            if (OLogManager.instance().isDebugEnabled()) {
                OLogManager.instance().debug((Object)this, "Connection closed", new Object[0]);
            }
        }
    }
    
    public OHttpRequest getRequest() {
        return this.request;
    }
    
    public OHttpResponse getResponse() {
        return this.response;
    }
    
    @Override
    public OChannel getChannel() {
        return (OChannel)this.channel;
    }
    
    public OUser getAccount() {
        return this.account;
    }
    
    public String getSessionID() {
        return this.request.getSessionId();
    }
    
    public String getResponseCharSet() {
        return this.responseCharSet;
    }
    
    public void setResponseCharSet(final String responseCharSet) {
        this.responseCharSet = responseCharSet;
    }
    
    public String[] getAdditionalResponseHeaders() {
        return this.additionalResponseHeaders;
    }
    
    public OHttpNetworkCommandManager getCommandManager() {
        return this.cmdManager;
    }
    
    protected void handleError(Throwable e, final OHttpRequest iRequest) {
        if (OLogManager.instance().isDebugEnabled()) {
            OLogManager.instance().debug((Object)this, "Caught exception", e, new Object[0]);
        }
        int errorCode = 500;
        String errorReason = null;
        String errorMessage = null;
        String responseHeaders = null;
        if (e instanceof IllegalFormatException || e instanceof InputMismatchException) {
            errorCode = 400;
            errorReason = "Bad request";
        }
        else if (e instanceof ORecordNotFoundException) {
            errorCode = 404;
            errorReason = "Not Found";
        }
        else if (e instanceof OConcurrentModificationException) {
            errorCode = 409;
            errorReason = "Conflict";
        }
        else if (e instanceof OLockException) {
            errorCode = 423;
        }
        else if (e instanceof UnsupportedOperationException) {
            errorCode = 501;
            errorReason = "Not Implemented";
        }
        else if (e instanceof IllegalArgumentException) {
            errorCode = 500;
        }
        if (e instanceof ODatabaseException || e instanceof OSecurityAccessException || e instanceof OCommandExecutionException || e instanceof OLockException) {
            Throwable cause;
            do {
                cause = ((e instanceof OSecurityAccessException) ? e : e.getCause());
                if (cause instanceof OSecurityAccessException) {
                    if (this.account == null) {
                        errorCode = 401;
                        errorReason = "Unauthorized";
                        final String xRequestedWithHeader = iRequest.getHeader("X-Requested-With");
                        if (xRequestedWithHeader == null || !xRequestedWithHeader.equals("XMLHttpRequest")) {
                            responseHeaders = this.server.getSecurity().getAuthenticationHeader(((OSecurityAccessException)cause).getDatabaseName());
                        }
                        errorMessage = null;
                        break;
                    }
                    errorCode = 530;
                    errorReason = "The current user does not have the privileges to execute the request.";
                    errorMessage = "530 User access denied";
                    break;
                }
                else {
                    if (cause == null) {
                        continue;
                    }
                    e = cause;
                }
            } while (cause != null);
        }
        else if (e instanceof OCommandSQLParsingException) {
            errorMessage = e.getMessage();
            errorCode = 400;
        }
        if (errorMessage == null) {
            final StringBuilder buffer = new StringBuilder(256);
            buffer.append(e);
            for (Throwable cause2 = e.getCause(); cause2 != null && cause2 != cause2.getCause(); cause2 = cause2.getCause()) {
                buffer.append("\r\n--> ");
                buffer.append(cause2);
            }
            errorMessage = buffer.toString();
        }
        if (errorReason == null) {
            errorReason = "Internal Server Error";
            if (e instanceof NullPointerException) {
                OLogManager.instance().error((Object)this, "Internal server error:\n", e, new Object[0]);
            }
            else {
                OLogManager.instance().debug((Object)this, "Internal server error:\n", e, new Object[0]);
            }
        }
        try {
            this.sendError(errorCode, errorReason, responseHeaders, "text/plain", errorMessage, this.request.isKeepAlive());
        }
        catch (IOException e2) {
            this.sendShutdown();
        }
    }
    
    protected void sendTextContent(final int iCode, final String iReason, final String iHeaders, final String iContentType, final String iContent, final boolean iKeepAlive) throws IOException {
        final boolean empty = iContent == null || iContent.length() == 0;
        this.sendStatus((empty && iCode == 200) ? 204 : iCode, iReason);
        this.sendResponseHeaders(iContentType, iKeepAlive);
        if (iHeaders != null) {
            this.writeLine(iHeaders);
        }
        final byte[] binaryContent = (byte[])(empty ? null : iContent.getBytes(ONetworkProtocolHttpAbstract.utf8));
        this.writeLine("Content-Length: " + (empty ? 0 : binaryContent.length));
        this.writeLine(null);
        if (binaryContent != null) {
            this.channel.writeBytes(binaryContent);
        }
        this.channel.flush();
    }
    
    protected void sendError(final int iCode, final String iReason, final String iHeaders, final String iContentType, final String iContent, final boolean iKeepAlive) throws IOException {
        if (!this.jsonResponseError) {
            this.sendTextContent(iCode, iReason, iHeaders, iContentType, iContent, iKeepAlive);
            return;
        }
        this.sendStatus(iCode, iReason);
        this.sendResponseHeaders("application/json", iKeepAlive);
        if (iHeaders != null) {
            this.writeLine(iHeaders);
        }
        final ODocument response = new ODocument();
        final ODocument error = new ODocument();
        error.field("code", (Object)iCode);
        error.field("reason", (Object)iCode);
        error.field("content", (Object)iContent);
        final List<ODocument> errors = new ArrayList<ODocument>();
        errors.add(error);
        response.field("errors", (Object)errors);
        final byte[] binaryContent = response.toJSON("prettyPrint").getBytes(ONetworkProtocolHttpAbstract.utf8);
        this.writeLine("Content-Length: " + ((binaryContent != null) ? binaryContent.length : 0));
        this.writeLine(null);
        if (binaryContent != null) {
            this.channel.writeBytes(binaryContent);
        }
        this.channel.flush();
    }
    
    protected void writeLine(final String iContent) throws IOException {
        if (iContent != null) {
            this.channel.outStream.write(iContent.getBytes());
        }
        this.channel.outStream.write(OHttpUtils.EOL);
    }
    
    protected void sendStatus(final int iStatus, final String iReason) throws IOException {
        this.writeLine(this.request.getHttpVersion() + " " + iStatus + " " + iReason);
    }
    
    protected void sendResponseHeaders(final String iContentType, final boolean iKeepAlive) throws IOException {
        this.writeLine("Cache-Control: no-cache, no-store, max-age=0, must-revalidate");
        this.writeLine("Pragma: no-cache");
        this.writeLine("Date: " + new Date());
        this.writeLine("Content-Type: " + iContentType + "; charset=" + this.responseCharSet);
        this.writeLine("Server: " + this.connection.getData().serverInfo);
        this.writeLine("Connection: " + (iKeepAlive ? "Keep-Alive" : "close"));
        if (this.getAdditionalResponseHeaders() != null) {
            for (final String h : this.getAdditionalResponseHeaders()) {
                this.writeLine(h);
            }
        }
    }
    
    protected void readAllContent(final OHttpRequest iRequest) throws IOException {
        iRequest.setContent(null);
        int contentLength = -1;
        boolean endOfHeaders = false;
        final StringBuilder request = new StringBuilder(512);
        while (!this.channel.socket.isInputShutdown()) {
            int in = this.channel.read();
            if (in == -1) {
                break;
            }
            char currChar = (char)in;
            if (currChar == '\r') {
                if (request.length() > 0 && !endOfHeaders) {
                    final String line = request.toString();
                    if (OStringSerializerHelper.startsWithIgnoreCase(line, "Authorization: ")) {
                        final String auth = line.substring("Authorization: ".length());
                        if (OStringSerializerHelper.startsWithIgnoreCase(auth, "Basic")) {
                            iRequest.setAuthorization(auth.substring("Basic".length() + 1));
                            iRequest.setAuthorization(new String(Base64.getDecoder().decode(iRequest.getAuthorization())));
                        }
                        else if (OStringSerializerHelper.startsWithIgnoreCase(auth, "Bearer")) {
                            iRequest.setBearerTokenRaw(auth.substring("Bearer".length() + 1));
                        }
                        else {
                            if (!OStringSerializerHelper.startsWithIgnoreCase(auth, "Negotiate")) {
                                throw new IllegalArgumentException("Only HTTP Basic and Bearer authorization are supported");
                            }
                            iRequest.setAuthorization("Negotiate:" + auth.substring("Negotiate".length() + 1));
                        }
                    }
                    else if (OStringSerializerHelper.startsWithIgnoreCase(line, "Connection: ")) {
                        iRequest.setKeepAlive(line.substring("Connection: ".length()).equalsIgnoreCase("Keep-Alive"));
                    }
                    else if (OStringSerializerHelper.startsWithIgnoreCase(line, "Cookie: ")) {
                        final String sessionPair = line.substring("Cookie: ".length());
                        final String[] split;
                        final String[] sessionItems = split = sessionPair.split(";");
                        for (final String sessionItem : split) {
                            final String[] sessionPairItems = sessionItem.trim().split("=");
                            if (sessionPairItems.length == 2 && "OSESSIONID".equals(sessionPairItems[0])) {
                                iRequest.setSessionId(sessionPairItems[1]);
                                break;
                            }
                        }
                    }
                    else if (OStringSerializerHelper.startsWithIgnoreCase(line, "Content-Length: ")) {
                        contentLength = Integer.parseInt(line.substring("Content-Length: ".length()));
                        if (contentLength > ONetworkProtocolHttpAbstract.requestMaxContentLength) {
                            OLogManager.instance().warn((Object)this, "->" + this.channel.socket.getInetAddress().getHostAddress() + ": Error on content size " + contentLength + ": the maximum allowed is " + ONetworkProtocolHttpAbstract.requestMaxContentLength, new Object[0]);
                        }
                    }
                    else if (OStringSerializerHelper.startsWithIgnoreCase(line, "Content-Type: ")) {
                        iRequest.setContentType(line.substring("Content-Type: ".length()));
                        if (OStringSerializerHelper.startsWithIgnoreCase(iRequest.getContentType(), "multipart/form-data")) {
                            iRequest.setMultipart(true);
                            iRequest.setBoundary(new String(line.substring("Content-Type: ".length() + "multipart/form-data".length() + 2 + "boundary".length() + 1)));
                        }
                    }
                    else if (OStringSerializerHelper.startsWithIgnoreCase(line, "If-Match: ")) {
                        iRequest.setIfMatch(line.substring("If-Match: ".length()));
                    }
                    else if (OStringSerializerHelper.startsWithIgnoreCase(line, "X-Forwarded-For: ")) {
                        this.connection.getData().caller = line.substring("X-Forwarded-For: ".length());
                    }
                    else if (OStringSerializerHelper.startsWithIgnoreCase(line, "OAuthentication: ")) {
                        iRequest.setAuthentication(line.substring("OAuthentication: ".length()));
                    }
                    else if (OStringSerializerHelper.startsWithIgnoreCase(line, "Expect: 100-continue")) {
                        this.sendTextContent(100, null, null, null, null, iRequest.isKeepAlive());
                    }
                    else if (OStringSerializerHelper.startsWithIgnoreCase(line, "Accept-Encoding: ")) {
                        iRequest.setContentEncoding(line.substring("Accept-Encoding: ".length()));
                    }
                    iRequest.addHeader(line);
                }
                in = this.channel.read();
                if (in == -1) {
                    break;
                }
                currChar = (char)in;
                if (!endOfHeaders && request.length() == 0) {
                    if (contentLength <= 0) {
                        return;
                    }
                    endOfHeaders = true;
                }
                request.setLength(0);
            }
            else if (endOfHeaders && request.length() == 0 && currChar != '\r' && currChar != '\n') {
                if (iRequest.isMultipart()) {
                    iRequest.setContent("");
                    iRequest.setMultipartStream(new OHttpMultipartBaseInputStream(this.channel.inStream, currChar, contentLength));
                    return;
                }
                final byte[] buffer = new byte[contentLength];
                buffer[0] = (byte)currChar;
                this.channel.read(buffer, 1, contentLength - 1);
                if (iRequest.getContentEncoding() != null && iRequest.getContentEncoding().equals("gzip")) {
                    iRequest.setContent(this.deCompress(buffer));
                }
                else {
                    iRequest.setContent(new String(buffer));
                }
                return;
            }
            else {
                request.append(currChar);
            }
        }
        if (OLogManager.instance().isDebugEnabled()) {
            OLogManager.instance().debug((Object)this, "Error on parsing HTTP content from client %s:\n%s", new Object[] { this.channel.socket.getInetAddress().getHostAddress(), request });
        }
    }
    
    protected void execute() throws Exception {
        if (this.channel.socket.isInputShutdown() || this.channel.socket.isClosed()) {
            this.connectionClosed();
            return;
        }
        this.connection.getData().commandInfo = "Listening";
        this.connection.getData().commandDetail = null;
        this.server.getMemoryManager().checkAndWaitMemoryThreshold();
        try {
            this.channel.socket.setSoTimeout(ONetworkProtocolHttpAbstract.socketTimeout);
            this.connection.getStats().lastCommandReceived = -1L;
            char c = (char)this.channel.read();
            if (this.channel.inStream.available() == 0) {
                this.connectionClosed();
                return;
            }
            this.channel.socket.setSoTimeout(ONetworkProtocolHttpAbstract.socketTimeout);
            this.connection.getStats().lastCommandReceived = System.currentTimeMillis();
            this.request = new OHttpRequestImpl(this, this.channel.inStream, this.connection.getData(), this.configuration);
            this.requestContent.setLength(0);
            this.request.setMultipart(false);
            if (c != '\n') {
                this.requestContent.append(c);
            }
            while (!this.channel.socket.isInputShutdown()) {
                c = (char)this.channel.read();
                if (c == '\r') {
                    final String[] words = this.requestContent.toString().split(" ");
                    if (words.length < 3) {
                        OLogManager.instance().warn((Object)this, "->" + this.channel.socket.getInetAddress().getHostAddress() + ": Error on invalid content:\n" + (Object)this.requestContent, new Object[0]);
                        while (this.channel.inStream.available() > 0) {
                            this.channel.read();
                        }
                        break;
                    }
                    this.channel.read();
                    this.request.setHttpMethod(words[0].toUpperCase(Locale.ENGLISH));
                    this.request.setUrl(words[1].trim());
                    final int parametersPos = this.request.getUrl().indexOf(63);
                    if (parametersPos > -1) {
                        this.request.setParameters(OHttpUtils.getParameters(this.request.getUrl().substring(parametersPos)));
                        this.request.setUrl(this.request.getUrl().substring(0, parametersPos));
                    }
                    this.request.setHttpVersion(words[2]);
                    this.readAllContent(this.request);
                    if (this.request.getContent() != null && this.request.getContentType() != null && this.request.getContentType().equals("application/x-www-form-urlencoded")) {
                        this.request.setContent(URLDecoder.decode(this.request.getContent(), "UTF-8").trim());
                    }
                    if (OLogManager.instance().isDebugEnabled()) {
                        OLogManager.instance().debug((Object)this, "[ONetworkProtocolHttpAbstract.execute] Requested: %s %s", new Object[] { this.request.getHttpMethod(), this.request.getUrl() });
                    }
                    this.service();
                    return;
                }
                else {
                    this.requestContent.append(c);
                    if (OGlobalConfiguration.NETWORK_HTTP_MAX_CONTENT_LENGTH.getValueAsInteger() > -1 && this.requestContent.length() >= 10000 + OGlobalConfiguration.NETWORK_HTTP_MAX_CONTENT_LENGTH.getValueAsInteger() * 2) {
                        while (this.channel.inStream.available() > 0) {
                            this.channel.read();
                        }
                        throw new ONetworkProtocolException("Invalid http request, max content length exceeded");
                    }
                    continue;
                }
            }
            if (OLogManager.instance().isDebugEnabled()) {
                OLogManager.instance().debug((Object)this, "Parsing request from client " + this.channel.socket.getInetAddress().getHostAddress() + ":\n" + (Object)this.requestContent, new Object[0]);
            }
        }
        catch (SocketException e) {
            this.connectionError();
        }
        catch (SocketTimeoutException e2) {
            this.timeout();
        }
        catch (Exception t) {
            if (this.request.getHttpMethod() != null && this.request.getUrl() != null) {
                try {
                    this.sendError(505, "Error on executing of " + this.request.getHttpMethod() + " for the resource: " + this.request.getUrl(), null, "text/plain", t.toString(), this.request.isKeepAlive());
                }
                catch (IOException ex) {}
            }
            else {
                this.sendError(505, "Error on executing request", null, "text/plain", t.toString(), this.request.isKeepAlive());
            }
            this.readAllContent(this.request);
        }
        finally {
            if (this.connection.getStats().lastCommandReceived > -1L) {
                Orient.instance().getProfiler().stopChrono("server.network.requests", "Total received requests", this.connection.getStats().lastCommandReceived, "server.network.requests");
            }
            this.request = null;
            this.response = null;
        }
    }
    
    protected String deCompress(final byte[] zipBytes) {
        if (zipBytes == null || zipBytes.length == 0) {
            return null;
        }
        GZIPInputStream gzip = null;
        ByteArrayInputStream in = null;
        ByteArrayOutputStream baos = null;
        try {
            in = new ByteArrayInputStream(zipBytes);
            gzip = new GZIPInputStream(in, 16384);
            final byte[] buffer = new byte[1024];
            baos = new ByteArrayOutputStream();
            int len = -1;
            while ((len = gzip.read(buffer, 0, buffer.length)) != -1) {
                baos.write(buffer, 0, len);
            }
            final String newstr = new String(baos.toByteArray(), "UTF-8");
            return newstr;
        }
        catch (Exception ex) {
            OLogManager.instance().error((Object)this, "Error on decompressing HTTP response", (Throwable)ex, new Object[0]);
        }
        finally {
            try {
                if (gzip != null) {
                    gzip.close();
                }
                if (in != null) {
                    in.close();
                }
                if (baos != null) {
                    baos.close();
                }
            }
            catch (Exception ex2) {}
        }
        return null;
    }
    
    protected void connectionClosed() {
        Orient.instance().getProfiler().updateCounter("server.http." + this.listeningAddress + ".closed", "Close HTTP connection", 1L, "server.http.*.closed");
        this.sendShutdown();
    }
    
    protected void timeout() {
        Orient.instance().getProfiler().updateCounter("server.http." + this.listeningAddress + ".timeout", "Timeout of HTTP connection", 1L, "server.http.*.timeout");
        this.sendShutdown();
    }
    
    protected void connectionError() {
        Orient.instance().getProfiler().updateCounter("server.http." + this.listeningAddress + ".errors", "Error on HTTP connection", 1L, "server.http.*.errors");
        this.sendShutdown();
    }
    
    public static void registerHandlers(final Object caller, final OServer server, final OServerNetworkListener iListener, final OHttpNetworkCommandManager cmdManager) {
        cmdManager.registerCommand(new OServerCommandGetConnect());
        cmdManager.registerCommand(new OServerCommandGetDisconnect());
        cmdManager.registerCommand(new OServerCommandGetClass());
        cmdManager.registerCommand(new OServerCommandGetCluster());
        cmdManager.registerCommand(new OServerCommandGetDatabase());
        cmdManager.registerCommand(new OServerCommandGetDictionary());
        cmdManager.registerCommand(new OServerCommandGetDocument());
        cmdManager.registerCommand(new OServerCommandGetDocumentByClass());
        cmdManager.registerCommand(new OServerCommandGetQuery());
        cmdManager.registerCommand(new OServerCommandGetServer());
        cmdManager.registerCommand(new OServerCommandGetServerVersion());
        cmdManager.registerCommand(new OServerCommandGetConnections());
        cmdManager.registerCommand(new OServerCommandGetStorageAllocation());
        cmdManager.registerCommand(new OServerCommandGetFileDownload());
        cmdManager.registerCommand(new OServerCommandGetIndex());
        cmdManager.registerCommand(new OServerCommandGetListDatabases());
        cmdManager.registerCommand(new OServerCommandIsEnterprise());
        cmdManager.registerCommand(new OServerCommandGetExportDatabase());
        cmdManager.registerCommand(new OServerCommandPatchDocument());
        cmdManager.registerCommand(new OServerCommandPostBatch());
        cmdManager.registerCommand(new OServerCommandPostClass());
        cmdManager.registerCommand(new OServerCommandPostCommandGraph());
        cmdManager.registerCommand(new OServerCommandPostDatabase());
        cmdManager.registerCommand(new OServerCommandPostInstallDatabase());
        cmdManager.registerCommand(new OServerCommandPostDocument());
        cmdManager.registerCommand(new OServerCommandPostImportRecords());
        cmdManager.registerCommand(new OServerCommandPostProperty());
        cmdManager.registerCommand(new OServerCommandPostConnection());
        cmdManager.registerCommand(new OServerCommandPostServer());
        cmdManager.registerCommand(new OServerCommandPostStudio());
        cmdManager.registerCommand(new OServerCommandPutDocument());
        cmdManager.registerCommand(new OServerCommandPutIndex());
        cmdManager.registerCommand(new OServerCommandDeleteClass());
        cmdManager.registerCommand(new OServerCommandDeleteDatabase());
        cmdManager.registerCommand(new OServerCommandDeleteDocument());
        cmdManager.registerCommand(new OServerCommandDeleteProperty());
        cmdManager.registerCommand(new OServerCommandDeleteIndex());
        cmdManager.registerCommand(new OServerCommandOptions());
        cmdManager.registerCommand(new OServerCommandFunction());
        cmdManager.registerCommand(new OServerCommandPostKillDbConnection());
        cmdManager.registerCommand(new OServerCommandGetSupportedLanguages());
        cmdManager.registerCommand(new OServerCommandPostAuthToken());
        cmdManager.registerCommand(new OServerCommandGetSSO());
        cmdManager.registerCommand(new OServerCommandGetPing());
        for (final OServerCommandConfiguration c : iListener.getStatefulCommands()) {
            try {
                cmdManager.registerCommand(OServerNetworkListener.createCommand(server, c));
            }
            catch (Exception e) {
                OLogManager.instance().error(caller, "Error on creating stateful command '%s'", (Throwable)e, new Object[] { c.implementation });
            }
        }
        for (final OServerCommand c2 : iListener.getStatelessCommands()) {
            cmdManager.registerCommand(c2);
        }
    }
    
    protected void registerStatelessCommands(final OServerNetworkListener iListener) {
        this.cmdManager = new OHttpNetworkCommandManager(this.server, null);
        registerHandlers(this, this.server, iListener, this.cmdManager);
    }
    
    public OClientConnection getConnection() {
        return this.connection;
    }
    
    public static String getCommandString(final String command, final String method) {
        final int getQueryPosition = command.indexOf(63);
        final StringBuilder commandString = new StringBuilder(256);
        commandString.append(method);
        commandString.append("|");
        if (getQueryPosition > -1) {
            commandString.append(command.substring(0, getQueryPosition));
        }
        else {
            commandString.append(command);
        }
        return commandString.toString();
    }
    
    @Override
    public String getRemoteAddress() {
        return ((InetSocketAddress)this.channel.socket.getRemoteSocketAddress()).getAddress().getHostAddress();
    }
    
    @Override
    public void setDatabase(final ODatabaseDocumentInternal db) {
        this.connection.setDatabase(db);
    }
    
    static {
        utf8 = Charset.forName("utf8");
    }
}
