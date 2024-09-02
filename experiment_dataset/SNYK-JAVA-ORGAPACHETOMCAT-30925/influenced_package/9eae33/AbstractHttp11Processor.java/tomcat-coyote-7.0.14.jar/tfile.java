// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.coyote.http11;

import java.util.concurrent.Executor;
import org.apache.tomcat.util.ExceptionUtils;
import org.apache.tomcat.util.net.AbstractEndpoint;
import org.apache.tomcat.util.http.MimeHeaders;
import org.apache.tomcat.util.http.FastHttpDateFormat;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.coyote.AsyncContextCallback;
import org.apache.coyote.http11.filters.SavedRequestInputFilter;
import java.io.IOException;
import org.apache.coyote.ActionCode;
import org.apache.coyote.http11.filters.GzipOutputFilter;
import org.apache.coyote.http11.filters.BufferedInputFilter;
import org.apache.coyote.http11.filters.VoidOutputFilter;
import org.apache.coyote.http11.filters.VoidInputFilter;
import org.apache.coyote.http11.filters.ChunkedOutputFilter;
import org.apache.coyote.http11.filters.ChunkedInputFilter;
import org.apache.coyote.http11.filters.IdentityOutputFilter;
import org.apache.coyote.http11.filters.IdentityInputFilter;
import org.apache.tomcat.util.buf.Ascii;
import org.apache.tomcat.util.buf.ByteChunk;
import org.apache.tomcat.util.buf.MessageBytes;
import java.util.StringTokenizer;
import org.apache.juli.logging.Log;
import org.apache.coyote.AsyncStateMachine;
import java.util.regex.Pattern;
import org.apache.coyote.Response;
import org.apache.coyote.Request;
import org.apache.coyote.Adapter;
import org.apache.tomcat.util.res.StringManager;
import org.apache.coyote.Processor;
import org.apache.coyote.ActionHook;

public abstract class AbstractHttp11Processor implements ActionHook, Processor
{
    protected static final StringManager sm;
    private int pluggableFilterIndex;
    protected Adapter adapter;
    protected Request request;
    protected Response response;
    protected boolean error;
    protected boolean keepAlive;
    protected boolean http11;
    protected boolean http09;
    protected boolean contentDelimitation;
    protected boolean expectation;
    protected Pattern restrictedUserAgents;
    protected int maxKeepAliveRequests;
    protected int keepAliveTimeout;
    protected String remoteAddr;
    protected String remoteHost;
    protected String localName;
    protected int localPort;
    protected int remotePort;
    protected String localAddr;
    protected int connectionUploadTimeout;
    protected boolean disableUploadTimeout;
    protected int compressionLevel;
    protected int compressionMinSize;
    protected int socketBuffer;
    protected int maxSavePostSize;
    protected Pattern noCompressionUserAgents;
    protected String[] compressableMimeTypes;
    protected char[] hostNameC;
    protected String server;
    protected AsyncStateMachine asyncStateMachine;
    
    public AbstractHttp11Processor() {
        this.pluggableFilterIndex = Integer.MAX_VALUE;
        this.adapter = null;
        this.request = null;
        this.response = null;
        this.error = false;
        this.keepAlive = true;
        this.http11 = true;
        this.http09 = false;
        this.contentDelimitation = true;
        this.expectation = false;
        this.restrictedUserAgents = null;
        this.maxKeepAliveRequests = -1;
        this.keepAliveTimeout = -1;
        this.remoteAddr = null;
        this.remoteHost = null;
        this.localName = null;
        this.localPort = -1;
        this.remotePort = -1;
        this.localAddr = null;
        this.connectionUploadTimeout = 300000;
        this.disableUploadTimeout = false;
        this.compressionLevel = 0;
        this.compressionMinSize = 2048;
        this.socketBuffer = -1;
        this.maxSavePostSize = 4096;
        this.noCompressionUserAgents = null;
        this.compressableMimeTypes = new String[] { "text/html", "text/xml", "text/plain" };
        this.hostNameC = new char[0];
        this.server = null;
        this.asyncStateMachine = new AsyncStateMachine(this);
    }
    
    protected abstract Log getLog();
    
    public void setCompression(final String compression) {
        if (compression.equals("on")) {
            this.compressionLevel = 1;
        }
        else if (compression.equals("force")) {
            this.compressionLevel = 2;
        }
        else if (compression.equals("off")) {
            this.compressionLevel = 0;
        }
        else {
            try {
                this.compressionMinSize = Integer.parseInt(compression);
                this.compressionLevel = 1;
            }
            catch (Exception e) {
                this.compressionLevel = 0;
            }
        }
    }
    
    public void setCompressionMinSize(final int compressionMinSize) {
        this.compressionMinSize = compressionMinSize;
    }
    
    public void setNoCompressionUserAgents(final String noCompressionUserAgents) {
        if (noCompressionUserAgents == null || noCompressionUserAgents.length() == 0) {
            this.noCompressionUserAgents = null;
        }
        else {
            this.noCompressionUserAgents = Pattern.compile(noCompressionUserAgents);
        }
    }
    
    public void addCompressableMimeType(final String mimeType) {
        this.compressableMimeTypes = this.addStringArray(this.compressableMimeTypes, mimeType);
    }
    
    public void setCompressableMimeTypes(final String[] compressableMimeTypes) {
        this.compressableMimeTypes = compressableMimeTypes;
    }
    
    public void setCompressableMimeTypes(final String compressableMimeTypes) {
        if (compressableMimeTypes != null) {
            this.compressableMimeTypes = null;
            final StringTokenizer st = new StringTokenizer(compressableMimeTypes, ",");
            while (st.hasMoreTokens()) {
                this.addCompressableMimeType(st.nextToken().trim());
            }
        }
    }
    
    public String getCompression() {
        switch (this.compressionLevel) {
            case 0: {
                return "off";
            }
            case 1: {
                return "on";
            }
            case 2: {
                return "force";
            }
            default: {
                return "off";
            }
        }
    }
    
    private String[] addStringArray(final String[] sArray, final String value) {
        String[] result = null;
        if (sArray == null) {
            result = new String[] { value };
        }
        else {
            result = new String[sArray.length + 1];
            for (int i = 0; i < sArray.length; ++i) {
                result[i] = sArray[i];
            }
            result[sArray.length] = value;
        }
        return result;
    }
    
    private boolean startsWithStringArray(final String[] sArray, final String value) {
        if (value == null) {
            return false;
        }
        for (int i = 0; i < sArray.length; ++i) {
            if (value.startsWith(sArray[i])) {
                return true;
            }
        }
        return false;
    }
    
    public void setRestrictedUserAgents(final String restrictedUserAgents) {
        if (restrictedUserAgents == null || restrictedUserAgents.length() == 0) {
            this.restrictedUserAgents = null;
        }
        else {
            this.restrictedUserAgents = Pattern.compile(restrictedUserAgents);
        }
    }
    
    public void setMaxKeepAliveRequests(final int mkar) {
        this.maxKeepAliveRequests = mkar;
    }
    
    public int getMaxKeepAliveRequests() {
        return this.maxKeepAliveRequests;
    }
    
    public void setKeepAliveTimeout(final int timeout) {
        this.keepAliveTimeout = timeout;
    }
    
    public int getKeepAliveTimeout() {
        return this.keepAliveTimeout;
    }
    
    public void setMaxSavePostSize(final int msps) {
        this.maxSavePostSize = msps;
    }
    
    public int getMaxSavePostSize() {
        return this.maxSavePostSize;
    }
    
    public void setDisableUploadTimeout(final boolean isDisabled) {
        this.disableUploadTimeout = isDisabled;
    }
    
    public boolean getDisableUploadTimeout() {
        return this.disableUploadTimeout;
    }
    
    public void setSocketBuffer(final int socketBuffer) {
        this.socketBuffer = socketBuffer;
    }
    
    public int getSocketBuffer() {
        return this.socketBuffer;
    }
    
    public void setConnectionUploadTimeout(final int timeout) {
        this.connectionUploadTimeout = timeout;
    }
    
    public int getConnectionUploadTimeout() {
        return this.connectionUploadTimeout;
    }
    
    public void setServer(final String server) {
        if (server == null || server.equals("")) {
            this.server = null;
        }
        else {
            this.server = server;
        }
    }
    
    public String getServer() {
        return this.server;
    }
    
    public Request getRequest() {
        return this.request;
    }
    
    public void setAdapter(final Adapter adapter) {
        this.adapter = adapter;
    }
    
    public Adapter getAdapter() {
        return this.adapter;
    }
    
    private boolean isCompressable() {
        final MessageBytes contentEncodingMB = this.response.getMimeHeaders().getValue("Content-Encoding");
        if (contentEncodingMB != null && contentEncodingMB.indexOf("gzip") != -1) {
            return false;
        }
        if (this.compressionLevel == 2) {
            return true;
        }
        final long contentLength = this.response.getContentLengthLong();
        return (contentLength == -1L || contentLength > this.compressionMinSize) && this.compressableMimeTypes != null && this.startsWithStringArray(this.compressableMimeTypes, this.response.getContentType());
    }
    
    private boolean useCompression() {
        final MessageBytes acceptEncodingMB = this.request.getMimeHeaders().getValue("accept-encoding");
        if (acceptEncodingMB == null || acceptEncodingMB.indexOf("gzip") == -1) {
            return false;
        }
        if (this.compressionLevel == 2) {
            return true;
        }
        if (this.noCompressionUserAgents != null) {
            final MessageBytes userAgentValueMB = this.request.getMimeHeaders().getValue("user-agent");
            if (userAgentValueMB != null) {
                final String userAgentValue = userAgentValueMB.toString();
                if (this.noCompressionUserAgents != null && this.noCompressionUserAgents.matcher(userAgentValue).matches()) {
                    return false;
                }
            }
        }
        return true;
    }
    
    protected int findBytes(final ByteChunk bc, final byte[] b) {
        final byte first = b[0];
        final byte[] buff = bc.getBuffer();
        final int start = bc.getStart();
        for (int end = bc.getEnd(), srcEnd = b.length, i = start; i <= end - srcEnd; ++i) {
            if (Ascii.toLower(buff[i]) == first) {
                int myPos = i + 1;
                int srcPos = 1;
                while (srcPos < srcEnd) {
                    if (Ascii.toLower(buff[myPos++]) != b[srcPos++]) {
                        break;
                    }
                    if (srcPos == srcEnd) {
                        return i - start;
                    }
                }
            }
        }
        return -1;
    }
    
    protected boolean statusDropsConnection(final int status) {
        return status == 400 || status == 408 || status == 411 || status == 413 || status == 414 || status == 500 || status == 503 || status == 501;
    }
    
    protected abstract AbstractInputBuffer getInputBuffer();
    
    protected abstract AbstractOutputBuffer getOutputBuffer();
    
    protected void initializeFilters(final int maxTrailerSize) {
        this.getInputBuffer().addFilter(new IdentityInputFilter());
        this.getOutputBuffer().addFilter(new IdentityOutputFilter());
        this.getInputBuffer().addFilter(new ChunkedInputFilter(maxTrailerSize));
        this.getOutputBuffer().addFilter(new ChunkedOutputFilter());
        this.getInputBuffer().addFilter(new VoidInputFilter());
        this.getOutputBuffer().addFilter(new VoidOutputFilter());
        this.getInputBuffer().addFilter(new BufferedInputFilter());
        this.getOutputBuffer().addFilter(new GzipOutputFilter());
        this.pluggableFilterIndex = this.getInputBuffer().getFilters().length;
    }
    
    protected boolean addInputFilter(final InputFilter[] inputFilters, final String encodingName) {
        if (!encodingName.equals("identity")) {
            if (!encodingName.equals("chunked")) {
                for (int i = this.pluggableFilterIndex; i < inputFilters.length; ++i) {
                    if (inputFilters[i].getEncodingName().toString().equals(encodingName)) {
                        this.getInputBuffer().addActiveFilter(inputFilters[i]);
                        return true;
                    }
                }
                return false;
            }
            this.getInputBuffer().addActiveFilter(inputFilters[1]);
            this.contentDelimitation = true;
        }
        return true;
    }
    
    @Override
    public final void action(final ActionCode actionCode, final Object param) {
        if (actionCode == ActionCode.COMMIT) {
            if (this.response.isCommitted()) {
                return;
            }
            try {
                this.prepareResponse();
                this.getOutputBuffer().commit();
            }
            catch (IOException e) {
                this.error = true;
            }
        }
        else if (actionCode == ActionCode.ACK) {
            if (this.response.isCommitted() || !this.expectation) {
                return;
            }
            this.getInputBuffer().setSwallowInput(true);
            try {
                this.getOutputBuffer().sendAck();
            }
            catch (IOException e) {
                this.error = true;
            }
        }
        else if (actionCode == ActionCode.CLIENT_FLUSH) {
            try {
                this.getOutputBuffer().flush();
            }
            catch (IOException e) {
                this.error = true;
                this.response.setErrorException(e);
            }
        }
        else if (actionCode == ActionCode.DISABLE_SWALLOW_INPUT) {
            this.error = true;
            this.getInputBuffer().setSwallowInput(false);
        }
        else if (actionCode == ActionCode.RESET) {
            this.getOutputBuffer().reset();
        }
        else if (actionCode != ActionCode.CUSTOM) {
            if (actionCode == ActionCode.REQ_SET_BODY_REPLAY) {
                final ByteChunk body = (ByteChunk)param;
                final InputFilter savedBody = new SavedRequestInputFilter(body);
                savedBody.setRequest(this.request);
                final AbstractInputBuffer internalBuffer = (AbstractInputBuffer)this.request.getInputBuffer();
                internalBuffer.addActiveFilter(savedBody);
            }
            else if (actionCode == ActionCode.ASYNC_START) {
                this.asyncStateMachine.asyncStart((AsyncContextCallback)param);
            }
            else if (actionCode == ActionCode.ASYNC_DISPATCHED) {
                this.asyncStateMachine.asyncDispatched();
            }
            else if (actionCode == ActionCode.ASYNC_TIMEOUT) {
                final AtomicBoolean result = (AtomicBoolean)param;
                result.set(this.asyncStateMachine.asyncTimeout());
            }
            else if (actionCode == ActionCode.ASYNC_RUN) {
                this.asyncStateMachine.asyncRun((Runnable)param);
            }
            else if (actionCode == ActionCode.ASYNC_ERROR) {
                this.asyncStateMachine.asyncError();
            }
            else if (actionCode == ActionCode.ASYNC_IS_STARTED) {
                ((AtomicBoolean)param).set(this.asyncStateMachine.isAsyncStarted());
            }
            else if (actionCode == ActionCode.ASYNC_IS_DISPATCHING) {
                ((AtomicBoolean)param).set(this.asyncStateMachine.isAsyncDispatching());
            }
            else if (actionCode == ActionCode.ASYNC_IS_ASYNC) {
                ((AtomicBoolean)param).set(this.asyncStateMachine.isAsync());
            }
            else if (actionCode == ActionCode.ASYNC_IS_TIMINGOUT) {
                ((AtomicBoolean)param).set(this.asyncStateMachine.isAsyncTimingOut());
            }
            else {
                this.actionInternal(actionCode, param);
            }
        }
    }
    
    abstract void actionInternal(final ActionCode p0, final Object p1);
    
    private void prepareResponse() {
        boolean entityBody = true;
        this.contentDelimitation = false;
        final OutputFilter[] outputFilters = this.getOutputBuffer().getFilters();
        if (this.http09) {
            this.getOutputBuffer().addActiveFilter(outputFilters[0]);
            return;
        }
        final int statusCode = this.response.getStatus();
        if (statusCode == 204 || statusCode == 205 || statusCode == 304) {
            this.getOutputBuffer().addActiveFilter(outputFilters[2]);
            entityBody = false;
            this.contentDelimitation = true;
        }
        final MessageBytes methodMB = this.request.method();
        if (methodMB.equals("HEAD")) {
            this.getOutputBuffer().addActiveFilter(outputFilters[2]);
            this.contentDelimitation = true;
        }
        boolean sendingWithSendfile = false;
        if (this.getEndpoint().getUseSendfile()) {
            sendingWithSendfile = this.prepareSendfile(outputFilters);
        }
        boolean isCompressable = false;
        boolean useCompression = false;
        if (entityBody && this.compressionLevel > 0 && !sendingWithSendfile) {
            isCompressable = this.isCompressable();
            if (isCompressable) {
                useCompression = this.useCompression();
            }
            if (useCompression) {
                this.response.setContentLength(-1);
            }
        }
        final MimeHeaders headers = this.response.getMimeHeaders();
        if (!entityBody) {
            this.response.setContentLength(-1);
        }
        else {
            final String contentType = this.response.getContentType();
            if (contentType != null) {
                headers.setValue("Content-Type").setString(contentType);
            }
            final String contentLanguage = this.response.getContentLanguage();
            if (contentLanguage != null) {
                headers.setValue("Content-Language").setString(contentLanguage);
            }
        }
        final long contentLength = this.response.getContentLengthLong();
        if (contentLength != -1L) {
            headers.setValue("Content-Length").setLong(contentLength);
            this.getOutputBuffer().addActiveFilter(outputFilters[0]);
            this.contentDelimitation = true;
        }
        else if (entityBody && this.http11) {
            this.getOutputBuffer().addActiveFilter(outputFilters[1]);
            this.contentDelimitation = true;
            headers.addValue("Transfer-Encoding").setString("chunked");
        }
        else {
            this.getOutputBuffer().addActiveFilter(outputFilters[0]);
        }
        if (useCompression) {
            this.getOutputBuffer().addActiveFilter(outputFilters[3]);
            headers.setValue("Content-Encoding").setString("gzip");
        }
        if (isCompressable) {
            final MessageBytes vary = headers.getValue("Vary");
            if (vary == null) {
                headers.setValue("Vary").setString("Accept-Encoding");
            }
            else if (!vary.equals("*")) {
                headers.setValue("Vary").setString(vary.getString() + ",Accept-Encoding");
            }
        }
        headers.setValue("Date").setString(FastHttpDateFormat.getCurrentDate());
        if (entityBody && !this.contentDelimitation) {
            this.keepAlive = false;
        }
        if (!(this.keepAlive = (this.keepAlive && !this.statusDropsConnection(statusCode)))) {
            headers.addValue("Connection").setString("close");
        }
        else if (!this.http11 && !this.error) {
            headers.addValue("Connection").setString("keep-alive");
        }
        this.getOutputBuffer().sendStatus();
        if (this.server != null) {
            headers.setValue("Server").setString(this.server);
        }
        else if (headers.getValue("Server") == null) {
            this.getOutputBuffer().write(Constants.SERVER_BYTES);
        }
        for (int size = headers.size(), i = 0; i < size; ++i) {
            this.getOutputBuffer().sendHeader(headers.getName(i), headers.getValue(i));
        }
        this.getOutputBuffer().endHeaders();
    }
    
    abstract AbstractEndpoint getEndpoint();
    
    abstract boolean prepareSendfile(final OutputFilter[] p0);
    
    public void endRequest() {
        try {
            this.getInputBuffer().endRequest();
        }
        catch (IOException e) {
            this.error = true;
        }
        catch (Throwable t) {
            ExceptionUtils.handleThrowable(t);
            this.getLog().error((Object)AbstractHttp11Processor.sm.getString("http11processor.request.finish"), t);
            this.response.setStatus(500);
            this.adapter.log(this.request, this.response, 0L);
            this.error = true;
        }
        try {
            this.getOutputBuffer().endRequest();
        }
        catch (IOException e) {
            this.error = true;
        }
        catch (Throwable t) {
            ExceptionUtils.handleThrowable(t);
            this.getLog().error((Object)AbstractHttp11Processor.sm.getString("http11processor.response.finish"), t);
            this.error = true;
        }
    }
    
    public final void recycle() {
        this.getInputBuffer().recycle();
        this.getOutputBuffer().recycle();
        this.asyncStateMachine.recycle();
        this.recycleInternal();
    }
    
    protected abstract void recycleInternal();
    
    @Override
    public abstract Executor getExecutor();
    
    protected boolean isAsync() {
        return this.asyncStateMachine.isAsync();
    }
    
    protected AbstractEndpoint.Handler.SocketState asyncPostProcess() {
        return this.asyncStateMachine.asyncPostProcess();
    }
    
    static {
        sm = StringManager.getManager("org.apache.coyote.http11");
    }
}
