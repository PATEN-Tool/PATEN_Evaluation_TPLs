// 
// Decompiled by Procyon v0.5.36
// 

package com.orientechnologies.orient.server.network.protocol.http;

import com.orientechnologies.orient.enterprise.channel.OChannel;
import java.net.SocketTimeoutException;
import java.net.SocketException;
import java.net.URLDecoder;
import com.orientechnologies.orient.server.network.protocol.http.multipart.OHttpMultipartBaseInputStream;
import com.orientechnologies.orient.core.serialization.OBase64Utils;
import java.util.Date;
import com.orientechnologies.orient.core.serialization.serializer.OStringSerializerHelper;
import com.orientechnologies.orient.core.exception.OCommandExecutionException;
import com.orientechnologies.orient.core.exception.OSecurityAccessException;
import com.orientechnologies.orient.core.exception.ODatabaseException;
import com.orientechnologies.common.concur.lock.OLockException;
import com.orientechnologies.orient.core.exception.OConcurrentModificationException;
import com.orientechnologies.orient.core.exception.ORecordNotFoundException;
import java.util.InputMismatchException;
import java.util.IllegalFormatException;
import com.orientechnologies.orient.enterprise.channel.binary.ONetworkProtocolException;
import java.util.Iterator;
import com.orientechnologies.orient.server.network.protocol.ONetworkProtocolData;
import com.orientechnologies.common.log.OLogManager;
import java.io.IOException;
import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.server.OClientConnectionManager;
import com.orientechnologies.orient.core.config.OContextConfiguration;
import java.net.Socket;
import com.orientechnologies.orient.server.OServer;
import java.util.HashMap;
import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.server.network.protocol.http.command.OServerCommand;
import java.util.Map;
import com.orientechnologies.orient.core.metadata.security.OUser;
import com.orientechnologies.orient.enterprise.channel.text.OChannelTextServer;
import com.orientechnologies.orient.server.OClientConnection;
import com.orientechnologies.orient.server.network.protocol.ONetworkProtocol;

public abstract class ONetworkProtocolHttpAbstract extends ONetworkProtocol
{
    private static final String COMMAND_SEPARATOR = "|";
    private static int requestMaxContentLength;
    private static int socketTimeout;
    protected OClientConnection connection;
    protected OChannelTextServer channel;
    protected OUser account;
    protected OHttpRequest request;
    protected OHttpResponse response;
    private final StringBuilder requestContent;
    private final Map<String, OServerCommand> exactCommands;
    private final Map<String, OServerCommand> wildcardCommands;
    private String responseCharSet;
    private String[] additionalResponseHeaders;
    private String listeningAddress;
    
    public ONetworkProtocolHttpAbstract() {
        super(Orient.getThreadGroup(), "IO-HTTP");
        this.requestContent = new StringBuilder();
        this.exactCommands = new HashMap<String, OServerCommand>();
        this.wildcardCommands = new HashMap<String, OServerCommand>();
        this.listeningAddress = "?";
    }
    
    @Override
    public void config(final OServer iServer, final Socket iSocket, final OContextConfiguration iConfiguration) throws IOException {
        final String addHeaders = iConfiguration.getValueAsString("network.http.additionalResponseHeaders", (String)null);
        if (addHeaders != null) {
            this.additionalResponseHeaders = addHeaders.split(";");
        }
        this.connection = OClientConnectionManager.instance().connect(iSocket, this);
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
        Orient.instance().getProfiler().updateCounter("server.http." + this.listeningAddress + ".requests", 1L);
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
        this.response = new OHttpResponse(this.channel.outStream, this.request.httpVersion, this.additionalResponseHeaders, this.responseCharSet, this.connection.data.serverInfo, this.request.sessionId, callbackF);
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
            OServerCommand cmd = this.exactCommands.get(commandString);
            if (cmd == null) {
                for (final Map.Entry<String, OServerCommand> entry : this.wildcardCommands.entrySet()) {
                    final int wildcardPos = entry.getKey().indexOf(42);
                    final String partLeft = entry.getKey().substring(0, wildcardPos);
                    final String partRight = entry.getKey().substring(wildcardPos + 1);
                    if (commandString.startsWith(partLeft) && commandString.endsWith(partRight)) {
                        cmd = entry.getValue();
                        break;
                    }
                }
            }
            if (cmd != null) {
                try {
                    if (!cmd.beforeExecute(this.request, this.response)) {
                        continue;
                    }
                    isChain = cmd.execute(this.request, this.response);
                }
                catch (Exception e) {
                    this.handleError(e);
                }
            }
            else {
                try {
                    OLogManager.instance().warn((Object)this, "->" + this.channel.socket.getInetAddress().getHostAddress() + ": Command not found: " + this.request.httpMethod + "." + command, new Object[0]);
                    this.sendTextContent(405, "Method Not Allowed", null, "text/plain", "Command not found: " + command);
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
            final StringBuilder buffer = new StringBuilder();
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
            this.sendTextContent(errorCode, errorReason, responseHeaders, "text/plain", errorMessage);
        }
        catch (IOException e2) {
            this.sendShutdown();
        }
    }
    
    @Override
    public void registerCommand(final Object iServerCommandInstance) {
        final OServerCommand cmd = (OServerCommand)iServerCommandInstance;
        for (final String name : cmd.getNames()) {
            if (OStringSerializerHelper.contains(name, '*')) {
                this.wildcardCommands.put(name, cmd);
            }
            else {
                this.exactCommands.put(name, cmd);
            }
        }
    }
    
    protected void sendTextContent(final int iCode, final String iReason, final String iHeaders, final String iContentType, final String iContent) throws IOException {
        final boolean empty = iContent == null || iContent.length() == 0;
        this.sendStatus((empty && iCode == 200) ? 204 : iCode, iReason);
        this.sendResponseHeaders(iContentType);
        if (iHeaders != null) {
            this.writeLine(iHeaders);
        }
        this.writeLine("Content-Length: " + (empty ? 0 : iContent.length()));
        this.writeLine(null);
        if (!empty) {
            this.writeLine(iContent);
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
    
    protected void sendResponseHeaders(final String iContentType) throws IOException {
        this.writeLine("Cache-Control: no-cache, no-store, max-age=0, must-revalidate");
        this.writeLine("Pragma: no-cache");
        this.writeLine("Date: " + new Date());
        this.writeLine("Content-Type: " + iContentType + "; charset=" + this.responseCharSet);
        this.writeLine("Server: " + this.connection.data.serverInfo);
        this.writeLine("Connection: Keep-Alive");
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
        final StringBuilder request = new StringBuilder();
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
                        if (!OStringSerializerHelper.startsWithIgnoreCase(auth, "Basic")) {
                            throw new IllegalArgumentException("Only HTTP Basic authorization is supported");
                        }
                        iRequest.authorization = auth.substring("Basic".length() + 1);
                        iRequest.authorization = new String(OBase64Utils.decode(iRequest.authorization));
                    }
                    else if (OStringSerializerHelper.startsWithIgnoreCase(line, "Cookie: ")) {
                        final String sessionPair = line.substring("Cookie: ".length());
                        final String[] arr$;
                        final String[] sessionItems = arr$ = sessionPair.split(";");
                        for (final String sessionItem : arr$) {
                            final String[] sessionPairItems = sessionItem.split("=");
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
                iRequest.content = new String(buffer);
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
        if (this.channel.socket.isInputShutdown()) {
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
                    this.request.httpMethod = words[0];
                    this.request.url = words[1].trim();
                    final int parametersPos = this.request.url.indexOf(63);
                    if (parametersPos > -1) {
                        this.request.parameters = OHttpUtils.getParameters(this.request.url.substring(parametersPos));
                        this.request.url = this.request.url.substring(0, parametersPos);
                    }
                    this.request.httpVersion = words[2];
                    this.readAllContent(this.request);
                    if (this.request.content != null && this.request.contentType.equals("application/x-www-form-urlencoded")) {
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
                    this.sendTextContent(505, "Error on executing of " + this.request.httpMethod + " for the resource: " + this.request.url, null, "text/plain", t.toString());
                }
                catch (IOException e3) {}
            }
            else {
                this.sendTextContent(505, "Error on executing request", null, "text/plain", t.toString());
            }
            this.readAllContent(this.request);
        }
        finally {
            if (this.connection.data.lastCommandReceived > -1L) {
                Orient.instance().getProfiler().stopChrono("server.http." + this.listeningAddress + ".request", this.connection.data.lastCommandReceived);
            }
        }
    }
    
    protected void connectionClosed() {
        Orient.instance().getProfiler().updateCounter("server.http." + this.listeningAddress + ".closed", 1L);
        this.sendShutdown();
    }
    
    protected void timeout() {
        Orient.instance().getProfiler().updateCounter("server.http." + this.listeningAddress + ".timeout", 1L);
        this.sendShutdown();
    }
    
    protected void connectionError() {
        Orient.instance().getProfiler().updateCounter("server.http." + this.listeningAddress + ".error", 1L);
        this.sendShutdown();
    }
    
    public void sendShutdown() {
        super.sendShutdown();
        try {
            this.channel.socket.close();
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
    
    private String getCommandString(final String command) {
        final int getQueryPosition = command.indexOf(63);
        final StringBuilder commandString = new StringBuilder();
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
    
    public String getResponseCharSet() {
        return this.responseCharSet;
    }
    
    public void setResponseCharSet(final String responseCharSet) {
        this.responseCharSet = responseCharSet;
    }
    
    public String[] getAdditionalResponseHeaders() {
        return this.additionalResponseHeaders;
    }
}
