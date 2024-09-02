// 
// Decompiled by Procyon v0.5.36
// 

package com.orientechnologies.orient.server.network.protocol.http;

import com.orientechnologies.orient.server.config.OServerCommandConfiguration;
import com.orientechnologies.orient.server.network.protocol.http.command.post.OServerCommandPostAuthToken;
import com.orientechnologies.orient.server.network.protocol.http.command.get.OServerCommandGetSupportedLanguages;
import com.orientechnologies.orient.server.network.protocol.http.command.get.OServerCommandKillDbConnection;
import com.orientechnologies.orient.server.network.protocol.http.command.all.OServerCommandAction;
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
import com.orientechnologies.orient.server.network.protocol.http.command.put.OServerCommandPostConnection;
import com.orientechnologies.orient.server.network.protocol.http.command.post.OServerCommandPostProperty;
import com.orientechnologies.orient.server.network.protocol.http.command.post.OServerCommandPostImportRecords;
import com.orientechnologies.orient.server.network.protocol.http.command.post.OServerCommandPostDocument;
import com.orientechnologies.orient.server.network.protocol.http.command.post.OServerCommandPostInstallDatabase;
import com.orientechnologies.orient.server.network.protocol.http.command.post.OServerCommandPostDatabase;
import com.orientechnologies.orient.server.network.protocol.http.command.post.OServerCommandPostCommand;
import com.orientechnologies.orient.server.network.protocol.http.command.post.OServerCommandPostClass;
import com.orientechnologies.orient.server.network.protocol.http.command.post.OServerCommandPostBatch;
import com.orientechnologies.orient.server.network.protocol.http.command.patch.OServerCommandPatchDocument;
import com.orientechnologies.orient.server.network.protocol.http.command.get.OServerCommandGetExportDatabase;
import com.orientechnologies.orient.server.network.protocol.http.command.get.OServerCommandGetListDatabases;
import com.orientechnologies.orient.server.network.protocol.http.command.get.OServerCommandGetIndex;
import com.orientechnologies.orient.server.network.protocol.http.command.get.OServerCommandGetFileDownload;
import com.orientechnologies.orient.server.network.protocol.http.command.get.OServerCommandGetStorageAllocation;
import com.orientechnologies.orient.server.network.protocol.http.command.get.OServerCommandGetConnections;
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
import java.net.SocketTimeoutException;
import java.net.SocketException;
import com.orientechnologies.orient.server.network.protocol.http.multipart.OHttpMultipartBaseInputStream;
import com.orientechnologies.orient.core.serialization.OBase64Utils;
import com.orientechnologies.orient.core.serialization.serializer.OStringSerializerHelper;
import java.util.Date;
import com.orientechnologies.orient.core.exception.OCommandExecutionException;
import com.orientechnologies.orient.core.exception.OSecurityAccessException;
import com.orientechnologies.orient.core.exception.ODatabaseException;
import com.orientechnologies.common.concur.lock.OLockException;
import com.orientechnologies.orient.core.exception.OConcurrentModificationException;
import com.orientechnologies.orient.core.exception.ORecordNotFoundException;
import java.util.InputMismatchException;
import java.util.IllegalFormatException;
import com.orientechnologies.orient.enterprise.channel.OChannel;
import com.orientechnologies.orient.enterprise.channel.binary.ONetworkProtocolException;
import java.util.Iterator;
import com.orientechnologies.orient.server.network.protocol.ONetworkProtocolData;
import com.orientechnologies.common.log.OLogManager;
import java.net.URLDecoder;
import java.util.Map;
import java.util.HashMap;
import com.orientechnologies.orient.server.network.protocol.http.command.OServerCommand;
import java.io.IOException;
import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.server.OClientConnectionManager;
import com.orientechnologies.orient.core.config.OContextConfiguration;
import java.net.Socket;
import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.network.OServerNetworkListener;
import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.metadata.security.OUser;
import com.orientechnologies.orient.enterprise.channel.text.OChannelTextServer;
import com.orientechnologies.orient.server.OClientConnection;
import java.nio.charset.Charset;
import com.orientechnologies.orient.server.network.protocol.ONetworkProtocol;

public abstract class ONetworkProtocolHttpAbstract extends ONetworkProtocol
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
    private String[] additionalResponseHeaders;
    private String listeningAddress;
    
    public ONetworkProtocolHttpAbstract() {
        super(Orient.instance().getThreadGroup(), "IO-HTTP");
        this.requestContent = new StringBuilder(512);
        this.listeningAddress = "?";
    }
    
    @Override
    public void config(final OServerNetworkListener iListener, final OServer iServer, final Socket iSocket, final OContextConfiguration iConfiguration) throws IOException {
        this.registerStatelessCommands(iListener);
        final String addHeaders = iConfiguration.getValueAsString("network.http.additionalResponseHeaders", (String)null);
        if (addHeaders != null) {
            this.additionalResponseHeaders = addHeaders.split(";");
        }
        this.connection = OClientConnectionManager.instance().connect(this);
        this.server = iServer;
        ONetworkProtocolHttpAbstract.requestMaxContentLength = iConfiguration.getValueAsInteger(OGlobalConfiguration.NETWORK_HTTP_MAX_CONTENT_LENGTH);
        ONetworkProtocolHttpAbstract.socketTimeout = iConfiguration.getValueAsInteger(OGlobalConfiguration.NETWORK_SOCKET_TIMEOUT);
        this.responseCharSet = iConfiguration.getValueAsString(OGlobalConfiguration.NETWORK_HTTP_CONTENT_CHARSET);
        (this.channel = new OChannelTextServer(iSocket, iConfiguration)).connected();
        this.request = new OHttpRequest(this, this.channel.inStream, this.connection.data, iConfiguration);
        this.connection.data.caller = this.channel.toString();
        this.listeningAddress = this.getListeningAddress();
        this.start();
    }
    
    public void service() throws ONetworkProtocolException, IOException {
        final ONetworkProtocolData data = this.connection.data;
        ++data.totalRequests;
        this.connection.data.commandInfo = null;
        this.connection.data.commandDetail = null;
        String callbackF;
        if (this.request.parameters != null && this.request.parameters.containsKey("callback")) {
            callbackF = this.request.parameters.get("callback");
        }
        else {
            callbackF = null;
        }
        this.response = new OHttpResponse(this.channel.outStream, this.request.httpVersion, this.additionalResponseHeaders, this.responseCharSet, this.connection.data.serverInfo, this.request.sessionId, callbackF, this.request.keepAlive, this.connection);
        if (this.request.contentEncoding != null && this.request.contentEncoding.equals("gzip")) {
            this.response.setContentEncoding("gzip");
        }
        this.waitNodeIsOnline();
        final long begin = System.currentTimeMillis();
        boolean isChain;
        do {
            isChain = false;
            String command;
            if (this.request.url.length() < 2) {
                command = "";
            }
            else {
                command = this.request.url.substring(1);
            }
            final String commandString = this.getCommandString(command);
            final OServerCommand cmd = (OServerCommand)this.cmdManager.getCommand(commandString);
            final Map<String, String> requestParams = this.cmdManager.extractUrlTokens(commandString);
            if (requestParams != null) {
                if (this.request.parameters == null) {
                    this.request.parameters = new HashMap<String, String>();
                }
                for (final Map.Entry<String, String> entry : requestParams.entrySet()) {
                    this.request.parameters.put(entry.getKey(), URLDecoder.decode(entry.getValue(), "UTF-8"));
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
                    this.handleError(e);
                }
            }
            else {
                try {
                    OLogManager.instance().warn((Object)this, "->" + this.channel.socket.getInetAddress().getHostAddress() + ": Command not found: " + this.request.httpMethod + "." + URLDecoder.decode(command, "UTF-8"), new Object[0]);
                    this.sendTextContent(405, "Method Not Allowed", null, "text/plain", "Command not found: " + command, this.request.keepAlive);
                }
                catch (IOException e2) {
                    this.sendShutdown();
                }
            }
        } while (isChain);
        this.connection.data.lastCommandInfo = this.connection.data.commandInfo;
        this.connection.data.lastCommandDetail = this.connection.data.commandDetail;
        this.connection.data.lastCommandExecutionTime = System.currentTimeMillis() - begin;
        final ONetworkProtocolData data2 = this.connection.data;
        data2.totalCommandExecutionTime += this.connection.data.lastCommandExecutionTime;
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
            OClientConnectionManager.instance().disconnect(this.connection.id);
            if (OLogManager.instance().isDebugEnabled()) {
                OLogManager.instance().debug((Object)this, "Connection shutdowned", new Object[0]);
            }
        }
    }
    
    @Override
    public OChannel getChannel() {
        return (OChannel)this.channel;
    }
    
    public OUser getAccount() {
        return this.account;
    }
    
    public String getSessionID() {
        return this.request.sessionId;
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
    
    protected void handleError(Throwable e) {
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
                        responseHeaders = "WWW-Authenticate: Basic realm=\"OrientDB db-" + ((OSecurityAccessException)cause).getDatabaseName() + "\"";
                        errorMessage = null;
                        break;
                    }
                    errorCode = 530;
                    errorReason = "Current user has not the privileges to execute the request.";
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
            OLogManager.instance().error((Object)this, "Internal server error:\n%s", new Object[] { errorMessage });
        }
        try {
            this.sendTextContent(errorCode, errorReason, responseHeaders, "text/plain", errorMessage, this.request.keepAlive);
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
    
    protected void writeLine(final String iContent) throws IOException {
        if (iContent != null) {
            this.channel.outStream.write(iContent.getBytes());
        }
        this.channel.outStream.write(OHttpUtils.EOL);
    }
    
    protected void sendStatus(final int iStatus, final String iReason) throws IOException {
        this.writeLine(this.request.httpVersion + " " + iStatus + " " + iReason);
    }
    
    protected void sendResponseHeaders(final String iContentType, final boolean iKeepAlive) throws IOException {
        this.writeLine("Cache-Control: no-cache, no-store, max-age=0, must-revalidate");
        this.writeLine("Pragma: no-cache");
        this.writeLine("Date: " + new Date());
        this.writeLine("Content-Type: " + iContentType + "; charset=" + this.responseCharSet);
        this.writeLine("Server: " + this.connection.data.serverInfo);
        this.writeLine("Connection: " + (iKeepAlive ? "Keep-Alive" : "close"));
        if (this.getAdditionalResponseHeaders() != null) {
            for (final String h : this.getAdditionalResponseHeaders()) {
                this.writeLine(h);
            }
        }
    }
    
    protected void readAllContent(final OHttpRequest iRequest) throws IOException {
        iRequest.content = null;
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
                            iRequest.authorization = auth.substring("Basic".length() + 1);
                            iRequest.authorization = new String(OBase64Utils.decode(iRequest.authorization));
                        }
                        else {
                            if (!OStringSerializerHelper.startsWithIgnoreCase(auth, "Bearer")) {
                                throw new IllegalArgumentException("Only HTTP Basic and Bearer authorization are supported");
                            }
                            iRequest.bearerTokenRaw = auth.substring("Bearer".length() + 1);
                        }
                    }
                    else if (OStringSerializerHelper.startsWithIgnoreCase(line, "Connection: ")) {
                        iRequest.keepAlive = line.substring("Connection: ".length()).equalsIgnoreCase("Keep-Alive");
                    }
                    else if (OStringSerializerHelper.startsWithIgnoreCase(line, "Cookie: ")) {
                        final String sessionPair = line.substring("Cookie: ".length());
                        final String[] split;
                        final String[] sessionItems = split = sessionPair.split(";");
                        for (final String sessionItem : split) {
                            final String[] sessionPairItems = sessionItem.trim().split("=");
                            if (sessionPairItems.length == 2 && "OSESSIONID".equals(sessionPairItems[0])) {
                                iRequest.sessionId = sessionPairItems[1];
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
                        iRequest.contentType = line.substring("Content-Type: ".length());
                        if (OStringSerializerHelper.startsWithIgnoreCase(iRequest.contentType, "multipart/form-data")) {
                            iRequest.isMultipart = true;
                            iRequest.boundary = new String(line.substring("Content-Type: ".length() + "multipart/form-data".length() + 2 + "boundary".length() + 1));
                        }
                    }
                    else if (OStringSerializerHelper.startsWithIgnoreCase(line, "If-Match: ")) {
                        iRequest.ifMatch = line.substring("If-Match: ".length());
                    }
                    else if (OStringSerializerHelper.startsWithIgnoreCase(line, "X-Forwarded-For: ")) {
                        this.connection.data.caller = line.substring("X-Forwarded-For: ".length());
                    }
                    else if (OStringSerializerHelper.startsWithIgnoreCase(line, "OAuthentication: ")) {
                        iRequest.authentication = line.substring("OAuthentication: ".length());
                    }
                    else if (OStringSerializerHelper.startsWithIgnoreCase(line, "Expect: 100-continue")) {
                        this.sendTextContent(100, null, null, null, null, iRequest.keepAlive);
                    }
                    else if (OStringSerializerHelper.startsWithIgnoreCase(line, "Accept-Encoding: ")) {
                        iRequest.contentEncoding = line.substring("Accept-Encoding: ".length());
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
                if (iRequest.isMultipart) {
                    iRequest.content = "";
                    iRequest.multipartStream = new OHttpMultipartBaseInputStream(this.channel.inStream, currChar, contentLength);
                    return;
                }
                final byte[] buffer = new byte[contentLength];
                buffer[0] = (byte)currChar;
                this.channel.read(buffer, 1, contentLength - 1);
                if (iRequest.contentEncoding != null && iRequest.contentEncoding.equals("gzip")) {
                    iRequest.content = this.deCompress(buffer);
                }
                else {
                    iRequest.content = new String(buffer);
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
        this.connection.data.commandInfo = "Listening";
        this.connection.data.commandDetail = null;
        try {
            this.channel.socket.setSoTimeout(ONetworkProtocolHttpAbstract.socketTimeout);
            this.connection.data.lastCommandReceived = -1L;
            char c = (char)this.channel.read();
            if (this.channel.inStream.available() == 0) {
                this.connectionClosed();
                return;
            }
            this.channel.socket.setSoTimeout(ONetworkProtocolHttpAbstract.socketTimeout);
            this.connection.data.lastCommandReceived = Orient.instance().getProfiler().startChrono();
            this.requestContent.setLength(0);
            this.request.isMultipart = false;
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
                    this.request.httpMethod = words[0].toUpperCase();
                    this.request.url = words[1].trim();
                    final int parametersPos = this.request.url.indexOf(63);
                    if (parametersPos > -1) {
                        this.request.parameters = OHttpUtils.getParameters(this.request.url.substring(parametersPos));
                        this.request.url = this.request.url.substring(0, parametersPos);
                    }
                    this.request.httpVersion = words[2];
                    this.readAllContent(this.request);
                    if (this.request.content != null && this.request.contentType != null && this.request.contentType.equals("application/x-www-form-urlencoded")) {
                        this.request.content = URLDecoder.decode(this.request.content, "UTF-8").trim();
                    }
                    if (OLogManager.instance().isDebugEnabled()) {
                        OLogManager.instance().debug((Object)this, "[ONetworkProtocolHttpAbstract.execute] Requested: %s %s", new Object[] { this.request.httpMethod, this.request.url });
                    }
                    this.service();
                    return;
                }
                else {
                    this.requestContent.append(c);
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
        catch (Throwable t) {
            if (this.request.httpMethod != null && this.request.url != null) {
                try {
                    this.sendTextContent(505, "Error on executing of " + this.request.httpMethod + " for the resource: " + this.request.url, null, "text/plain", t.toString(), this.request.keepAlive);
                }
                catch (IOException e3) {}
            }
            else {
                this.sendTextContent(505, "Error on executing request", null, "text/plain", t.toString(), this.request.keepAlive);
            }
            this.readAllContent(this.request);
        }
        finally {
            if (this.connection.data.lastCommandReceived > -1L) {
                Orient.instance().getProfiler().stopChrono("server.network.requests", "Total received requests", this.connection.data.lastCommandReceived, "server.network.requests");
            }
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
    
    protected void registerStatelessCommands(final OServerNetworkListener iListener) {
        (this.cmdManager = new OHttpNetworkCommandManager(this.server, null)).registerCommand(new OServerCommandGetConnect());
        this.cmdManager.registerCommand(new OServerCommandGetDisconnect());
        this.cmdManager.registerCommand(new OServerCommandGetClass());
        this.cmdManager.registerCommand(new OServerCommandGetCluster());
        this.cmdManager.registerCommand(new OServerCommandGetDatabase());
        this.cmdManager.registerCommand(new OServerCommandGetDictionary());
        this.cmdManager.registerCommand(new OServerCommandGetDocument());
        this.cmdManager.registerCommand(new OServerCommandGetDocumentByClass());
        this.cmdManager.registerCommand(new OServerCommandGetQuery());
        this.cmdManager.registerCommand(new OServerCommandGetServer());
        this.cmdManager.registerCommand(new OServerCommandGetConnections());
        this.cmdManager.registerCommand(new OServerCommandGetStorageAllocation());
        this.cmdManager.registerCommand(new OServerCommandGetFileDownload());
        this.cmdManager.registerCommand(new OServerCommandGetIndex());
        this.cmdManager.registerCommand(new OServerCommandGetListDatabases());
        this.cmdManager.registerCommand(new OServerCommandGetExportDatabase());
        this.cmdManager.registerCommand(new OServerCommandPatchDocument());
        this.cmdManager.registerCommand(new OServerCommandPostBatch());
        this.cmdManager.registerCommand(new OServerCommandPostClass());
        this.cmdManager.registerCommand(new OServerCommandPostCommand());
        this.cmdManager.registerCommand(new OServerCommandPostDatabase());
        this.cmdManager.registerCommand(new OServerCommandPostInstallDatabase());
        this.cmdManager.registerCommand(new OServerCommandPostDocument());
        this.cmdManager.registerCommand(new OServerCommandPostImportRecords());
        this.cmdManager.registerCommand(new OServerCommandPostProperty());
        this.cmdManager.registerCommand(new OServerCommandPostConnection());
        this.cmdManager.registerCommand(new OServerCommandPostStudio());
        this.cmdManager.registerCommand(new OServerCommandPutDocument());
        this.cmdManager.registerCommand(new OServerCommandPutIndex());
        this.cmdManager.registerCommand(new OServerCommandDeleteClass());
        this.cmdManager.registerCommand(new OServerCommandDeleteDatabase());
        this.cmdManager.registerCommand(new OServerCommandDeleteDocument());
        this.cmdManager.registerCommand(new OServerCommandDeleteProperty());
        this.cmdManager.registerCommand(new OServerCommandDeleteIndex());
        this.cmdManager.registerCommand(new OServerCommandOptions());
        this.cmdManager.registerCommand(new OServerCommandFunction());
        this.cmdManager.registerCommand(new OServerCommandAction());
        this.cmdManager.registerCommand(new OServerCommandKillDbConnection());
        this.cmdManager.registerCommand(new OServerCommandGetSupportedLanguages());
        this.cmdManager.registerCommand(new OServerCommandPostAuthToken());
        for (final OServerCommandConfiguration c : iListener.getStatefulCommands()) {
            try {
                this.cmdManager.registerCommand(OServerNetworkListener.createCommand(this.server, c));
            }
            catch (Exception e) {
                OLogManager.instance().error((Object)this, "Error on creating stateful command '%s'", (Throwable)e, new Object[] { c.implementation });
            }
        }
        for (final OServerCommand c2 : iListener.getStatelessCommands()) {
            this.cmdManager.registerCommand(c2);
        }
    }
    
    private String getCommandString(final String command) {
        final int getQueryPosition = command.indexOf(63);
        final StringBuilder commandString = new StringBuilder(256);
        commandString.append(this.request.httpMethod);
        commandString.append("|");
        if (getQueryPosition > -1) {
            commandString.append(command.substring(0, getQueryPosition));
        }
        else {
            commandString.append(command);
        }
        return commandString.toString();
    }
    
    static {
        utf8 = Charset.forName("utf8");
    }
}
