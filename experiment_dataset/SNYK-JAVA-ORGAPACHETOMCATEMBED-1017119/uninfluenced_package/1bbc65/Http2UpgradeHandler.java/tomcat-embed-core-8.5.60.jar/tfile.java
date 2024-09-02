// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.coyote.http2;

import org.apache.juli.logging.LogFactory;
import java.io.EOFException;
import java.util.TreeSet;
import java.util.Map;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.nio.ByteBuffer;
import org.apache.tomcat.util.http.MimeHeaders;
import java.nio.charset.StandardCharsets;
import org.apache.coyote.http11.upgrade.UpgradeInfo;
import org.apache.tomcat.util.net.AbstractEndpoint;
import org.apache.tomcat.util.net.SocketEvent;
import java.io.IOException;
import org.apache.coyote.ProtocolException;
import org.apache.tomcat.util.codec.binary.Base64;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.servlet.http.WebConnection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import org.apache.coyote.Request;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Set;
import java.util.Queue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.tomcat.util.net.SSLSupport;
import org.apache.tomcat.util.net.SocketWrapperBase;
import org.apache.coyote.Adapter;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.tomcat.util.res.StringManager;
import org.apache.juli.logging.Log;
import org.apache.coyote.http11.upgrade.InternalHttpUpgradeHandler;

public class Http2UpgradeHandler extends AbstractStream implements InternalHttpUpgradeHandler, Http2Parser.Input, Http2Parser.Output
{
    private static final Log log;
    private static final StringManager sm;
    private static final AtomicInteger connectionIdGenerator;
    private static final Integer STREAM_ID_ZERO;
    private static final int FLAG_END_OF_STREAM = 1;
    private static final int FLAG_END_OF_HEADERS = 4;
    private static final byte[] PING;
    private static final byte[] PING_ACK;
    private static final byte[] SETTINGS_ACK;
    private static final byte[] GOAWAY;
    private static final String HTTP2_SETTINGS_HEADER = "HTTP2-Settings";
    private static final HeaderSink HEADER_SINK;
    private final Object priorityTreeLock;
    private final String connectionId;
    private final Http2Protocol protocol;
    private final Adapter adapter;
    private volatile SocketWrapperBase<?> socketWrapper;
    private volatile SSLSupport sslSupport;
    private volatile Http2Parser parser;
    private AtomicReference<ConnectionState> connectionState;
    private volatile long pausedNanoTime;
    private final ConnectionSettingsRemote remoteSettings;
    private final ConnectionSettingsLocal localSettings;
    private HpackDecoder hpackDecoder;
    private HpackEncoder hpackEncoder;
    private long readTimeout;
    private long keepAliveTimeout;
    private long writeTimeout;
    private final ConcurrentNavigableMap<Integer, AbstractNonZeroStream> streams;
    protected final AtomicInteger activeRemoteStreamCount;
    private volatile int maxActiveRemoteStreamId;
    private volatile int maxProcessedStreamId;
    private final AtomicInteger nextLocalStreamId;
    private final PingManager pingManager;
    private volatile int newStreamsSinceLastPrune;
    private final ConcurrentMap<AbstractStream, BacklogTracker> backLogStreams;
    private long backLogSize;
    private volatile long connectionTimeout;
    private int maxConcurrentStreamExecution;
    private AtomicInteger streamConcurrency;
    private Queue<StreamRunnable> queuedRunnable;
    private Set<String> allowedTrailerHeaders;
    private int maxHeaderCount;
    private int maxHeaderSize;
    private int maxTrailerCount;
    private int maxTrailerSize;
    private final AtomicLong overheadCount;
    private volatile int lastNonFinalDataPayload;
    private volatile int lastWindowUpdate;
    
    public Http2UpgradeHandler(final Http2Protocol protocol, final Adapter adapter, final Request coyoteRequest) {
        super(Http2UpgradeHandler.STREAM_ID_ZERO);
        this.priorityTreeLock = new Object();
        this.connectionState = new AtomicReference<ConnectionState>(ConnectionState.NEW);
        this.pausedNanoTime = Long.MAX_VALUE;
        this.readTimeout = 5000L;
        this.keepAliveTimeout = 20000L;
        this.writeTimeout = 5000L;
        this.streams = new ConcurrentSkipListMap<Integer, AbstractNonZeroStream>();
        this.activeRemoteStreamCount = new AtomicInteger(0);
        this.maxActiveRemoteStreamId = -1;
        this.nextLocalStreamId = new AtomicInteger(2);
        this.pingManager = new PingManager();
        this.newStreamsSinceLastPrune = 0;
        this.backLogStreams = new ConcurrentHashMap<AbstractStream, BacklogTracker>();
        this.backLogSize = 0L;
        this.connectionTimeout = -1L;
        this.maxConcurrentStreamExecution = 20;
        this.streamConcurrency = null;
        this.queuedRunnable = null;
        this.allowedTrailerHeaders = Collections.emptySet();
        this.maxHeaderCount = 100;
        this.maxHeaderSize = 8192;
        this.maxTrailerCount = 100;
        this.maxTrailerSize = 8192;
        this.overheadCount = new AtomicLong(-10L);
        this.protocol = protocol;
        this.adapter = adapter;
        this.connectionId = Integer.toString(Http2UpgradeHandler.connectionIdGenerator.getAndIncrement());
        this.lastNonFinalDataPayload = protocol.getOverheadDataThreshold() * 2;
        this.lastWindowUpdate = protocol.getOverheadWindowUpdateThreshold() * 2;
        this.remoteSettings = new ConnectionSettingsRemote(this.connectionId);
        this.localSettings = new ConnectionSettingsLocal(this.connectionId);
        if (coyoteRequest != null) {
            if (Http2UpgradeHandler.log.isDebugEnabled()) {
                Http2UpgradeHandler.log.debug(Http2UpgradeHandler.sm.getString("upgradeHandler.upgrade", this.connectionId));
            }
            final Integer key = 1;
            final Stream stream = new Stream(key, this, coyoteRequest);
            this.streams.put(key, stream);
            this.maxActiveRemoteStreamId = 1;
            this.activeRemoteStreamCount.set(1);
            this.maxProcessedStreamId = 1;
        }
    }
    
    @Override
    public void init(final WebConnection webConnection) {
        if (Http2UpgradeHandler.log.isDebugEnabled()) {
            Http2UpgradeHandler.log.debug(Http2UpgradeHandler.sm.getString("upgradeHandler.init", this.connectionId, this.connectionState.get()));
        }
        if (!this.connectionState.compareAndSet(ConnectionState.NEW, ConnectionState.CONNECTED)) {
            return;
        }
        if (this.maxConcurrentStreamExecution < this.localSettings.getMaxConcurrentStreams()) {
            this.streamConcurrency = new AtomicInteger(0);
            this.queuedRunnable = new ConcurrentLinkedQueue<StreamRunnable>();
        }
        this.parser = new Http2Parser(this.connectionId, this, this);
        Stream stream = null;
        this.socketWrapper.setReadTimeout(this.getReadTimeout());
        this.socketWrapper.setWriteTimeout(this.getWriteTimeout());
        if (webConnection != null) {
            try {
                stream = this.getStream(1, true);
                final String base64Settings = stream.getCoyoteRequest().getHeader("HTTP2-Settings");
                final byte[] settings = Base64.decodeBase64URLSafe(base64Settings);
                FrameType.SETTINGS.check(0, settings.length);
                for (int i = 0; i < settings.length % 6; ++i) {
                    final int id = ByteUtil.getTwoBytes(settings, i * 6);
                    final long value = ByteUtil.getFourBytes(settings, i * 6 + 2);
                    this.remoteSettings.set(Setting.valueOf(id), value);
                }
            }
            catch (Http2Exception e) {
                throw new ProtocolException(Http2UpgradeHandler.sm.getString("upgradeHandler.upgrade.fail", this.connectionId));
            }
        }
        this.writeSettings();
        try {
            this.parser.readConnectionPreface();
        }
        catch (Http2Exception e) {
            final String msg = Http2UpgradeHandler.sm.getString("upgradeHandler.invalidPreface", this.connectionId);
            if (Http2UpgradeHandler.log.isDebugEnabled()) {
                Http2UpgradeHandler.log.debug(msg, e);
            }
            throw new ProtocolException(msg);
        }
        if (Http2UpgradeHandler.log.isDebugEnabled()) {
            Http2UpgradeHandler.log.debug(Http2UpgradeHandler.sm.getString("upgradeHandler.prefaceReceived", this.connectionId));
        }
        try {
            this.pingManager.sendPing(true);
        }
        catch (IOException ioe) {
            throw new ProtocolException(Http2UpgradeHandler.sm.getString("upgradeHandler.pingFailed"), ioe);
        }
        if (webConnection != null) {
            this.processStreamOnContainerThread(stream);
        }
    }
    
    private void processStreamOnContainerThread(final Stream stream) {
        final StreamProcessor streamProcessor = new StreamProcessor(this, stream, this.adapter, this.socketWrapper);
        streamProcessor.setSslSupport(this.sslSupport);
        this.processStreamOnContainerThread(streamProcessor, SocketEvent.OPEN_READ);
    }
    
    void processStreamOnContainerThread(final StreamProcessor streamProcessor, final SocketEvent event) {
        final StreamRunnable streamRunnable = new StreamRunnable(streamProcessor, event);
        if (this.streamConcurrency == null) {
            this.socketWrapper.getEndpoint().getExecutor().execute(streamRunnable);
        }
        else if (this.getStreamConcurrency() < this.maxConcurrentStreamExecution) {
            this.increaseStreamConcurrency();
            this.socketWrapper.getEndpoint().getExecutor().execute(streamRunnable);
        }
        else {
            this.queuedRunnable.offer(streamRunnable);
        }
    }
    
    @Override
    public void setSocketWrapper(final SocketWrapperBase<?> wrapper) {
        this.socketWrapper = wrapper;
    }
    
    @Override
    public void setSslSupport(final SSLSupport sslSupport) {
        this.sslSupport = sslSupport;
    }
    
    @Override
    public AbstractEndpoint.Handler.SocketState upgradeDispatch(final SocketEvent status) {
        if (Http2UpgradeHandler.log.isDebugEnabled()) {
            Http2UpgradeHandler.log.debug(Http2UpgradeHandler.sm.getString("upgradeHandler.upgradeDispatch.entry", this.connectionId, status));
        }
        this.init(null);
        AbstractEndpoint.Handler.SocketState result = AbstractEndpoint.Handler.SocketState.CLOSED;
        try {
            switch (status) {
                case OPEN_READ: {
                    synchronized (this.socketWrapper) {
                        if (!this.socketWrapper.canWrite()) {
                            this.pingManager.sendPing(false);
                        }
                    }
                    try {
                        this.socketWrapper.setReadTimeout(this.getReadTimeout());
                        this.setConnectionTimeout(-1L);
                        while (true) {
                            try {
                                if (!this.parser.readFrame(false)) {}
                            }
                            catch (StreamException se) {
                                final Stream stream = this.getStream(se.getStreamId(), false);
                                if (stream == null) {
                                    this.sendStreamReset(se);
                                }
                                else {
                                    stream.close(se);
                                }
                            }
                            finally {
                                if (this.overheadCount.get() > 0L) {
                                    throw new ConnectionException(Http2UpgradeHandler.sm.getString("upgradeHandler.tooMuchOverhead", this.connectionId), Http2Error.ENHANCE_YOUR_CALM);
                                }
                            }
                        }
                    }
                    catch (Http2Exception ce) {
                        if (Http2UpgradeHandler.log.isDebugEnabled()) {
                            Http2UpgradeHandler.log.debug(Http2UpgradeHandler.sm.getString("upgradeHandler.connectionError"), ce);
                        }
                        this.closeConnection(ce);
                        break;
                    }
                    if (this.connectionState.get() != ConnectionState.CLOSED) {
                        result = AbstractEndpoint.Handler.SocketState.UPGRADED;
                        break;
                    }
                    break;
                }
                case OPEN_WRITE: {
                    this.processWrites();
                    result = AbstractEndpoint.Handler.SocketState.UPGRADED;
                    break;
                }
                case TIMEOUT: {
                    this.closeConnection(null);
                    break;
                }
                case DISCONNECT:
                case ERROR:
                case STOP:
                case CONNECT_FAIL: {
                    this.close();
                    break;
                }
            }
        }
        catch (IOException ioe) {
            if (Http2UpgradeHandler.log.isDebugEnabled()) {
                Http2UpgradeHandler.log.debug(Http2UpgradeHandler.sm.getString("upgradeHandler.ioerror", this.connectionId), ioe);
            }
            this.close();
        }
        if (Http2UpgradeHandler.log.isDebugEnabled()) {
            Http2UpgradeHandler.log.debug(Http2UpgradeHandler.sm.getString("upgradeHandler.upgradeDispatch.exit", this.connectionId, result));
        }
        return result;
    }
    
    protected void setConnectionTimeoutForStreamCount(final int streamCount) {
        if (streamCount == 0) {
            final long keepAliveTimeout = this.protocol.getKeepAliveTimeout();
            if (keepAliveTimeout == -1L) {
                this.setConnectionTimeout(-1L);
            }
            else {
                this.setConnectionTimeout(System.currentTimeMillis() + keepAliveTimeout);
            }
        }
        else {
            this.setConnectionTimeout(-1L);
        }
    }
    
    private void setConnectionTimeout(final long connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }
    
    @Override
    public void timeoutAsync(final long now) {
        final long connectionTimeout = this.connectionTimeout;
        if (now == -1L || (connectionTimeout > -1L && now > connectionTimeout)) {
            this.socketWrapper.processSocket(SocketEvent.TIMEOUT, true);
        }
    }
    
    ConnectionSettingsRemote getRemoteSettings() {
        return this.remoteSettings;
    }
    
    ConnectionSettingsLocal getLocalSettings() {
        return this.localSettings;
    }
    
    Http2Protocol getProtocol() {
        return this.protocol;
    }
    
    @Override
    public void pause() {
        if (Http2UpgradeHandler.log.isDebugEnabled()) {
            Http2UpgradeHandler.log.debug(Http2UpgradeHandler.sm.getString("upgradeHandler.pause.entry", this.connectionId));
        }
        if (this.connectionState.compareAndSet(ConnectionState.CONNECTED, ConnectionState.PAUSING)) {
            this.pausedNanoTime = System.nanoTime();
            try {
                this.writeGoAwayFrame(Integer.MAX_VALUE, Http2Error.NO_ERROR.getCode(), null);
            }
            catch (IOException ex) {}
        }
    }
    
    @Override
    public void destroy() {
    }
    
    @Override
    public UpgradeInfo getUpgradeInfo() {
        return null;
    }
    
    private void checkPauseState() throws IOException {
        if (this.connectionState.get() == ConnectionState.PAUSING && this.pausedNanoTime + this.pingManager.getRoundTripTimeNano() < System.nanoTime()) {
            this.connectionState.compareAndSet(ConnectionState.PAUSING, ConnectionState.PAUSED);
            this.writeGoAwayFrame(this.maxProcessedStreamId, Http2Error.NO_ERROR.getCode(), null);
        }
    }
    
    private int increaseStreamConcurrency() {
        return this.streamConcurrency.incrementAndGet();
    }
    
    private int decreaseStreamConcurrency() {
        return this.streamConcurrency.decrementAndGet();
    }
    
    private int getStreamConcurrency() {
        return this.streamConcurrency.get();
    }
    
    void executeQueuedStream() {
        if (this.streamConcurrency == null) {
            return;
        }
        this.decreaseStreamConcurrency();
        if (this.getStreamConcurrency() < this.maxConcurrentStreamExecution) {
            final StreamRunnable streamRunnable = this.queuedRunnable.poll();
            if (streamRunnable != null) {
                this.increaseStreamConcurrency();
                this.socketWrapper.getEndpoint().getExecutor().execute(streamRunnable);
            }
        }
    }
    
    void sendStreamReset(final StreamException se) throws IOException {
        if (Http2UpgradeHandler.log.isDebugEnabled()) {
            Http2UpgradeHandler.log.debug(Http2UpgradeHandler.sm.getString("upgradeHandler.rst.debug", this.connectionId, Integer.toString(se.getStreamId()), se.getError(), se.getMessage()));
        }
        final byte[] rstFrame = new byte[13];
        ByteUtil.setThreeBytes(rstFrame, 0, 4);
        rstFrame[3] = FrameType.RST.getIdByte();
        ByteUtil.set31Bits(rstFrame, 5, se.getStreamId());
        ByteUtil.setFourBytes(rstFrame, 9, se.getError().getCode());
        synchronized (this.socketWrapper) {
            this.socketWrapper.write(true, rstFrame, 0, rstFrame.length);
            this.socketWrapper.flush(true);
        }
    }
    
    void closeConnection(final Http2Exception ce) {
        long code;
        byte[] msg;
        if (ce == null) {
            code = Http2Error.NO_ERROR.getCode();
            msg = null;
        }
        else {
            code = ce.getError().getCode();
            msg = ce.getMessage().getBytes(StandardCharsets.UTF_8);
        }
        try {
            this.writeGoAwayFrame(this.maxProcessedStreamId, code, msg);
        }
        catch (IOException ex) {}
        this.close();
    }
    
    private void writeSettings() {
        try {
            final byte[] settings = this.localSettings.getSettingsFrameForPending();
            this.socketWrapper.write(true, settings, 0, settings.length);
            final byte[] windowUpdateFrame = this.createWindowUpdateForSettings();
            if (windowUpdateFrame.length > 0) {
                this.socketWrapper.write(true, windowUpdateFrame, 0, windowUpdateFrame.length);
            }
            this.socketWrapper.flush(true);
        }
        catch (IOException ioe) {
            final String msg = Http2UpgradeHandler.sm.getString("upgradeHandler.sendPrefaceFail", this.connectionId);
            if (Http2UpgradeHandler.log.isDebugEnabled()) {
                Http2UpgradeHandler.log.debug(msg);
            }
            throw new ProtocolException(msg, ioe);
        }
    }
    
    protected byte[] createWindowUpdateForSettings() {
        final int increment = this.protocol.getInitialWindowSize() - 65535;
        byte[] windowUpdateFrame;
        if (increment > 0) {
            windowUpdateFrame = new byte[13];
            ByteUtil.setThreeBytes(windowUpdateFrame, 0, 4);
            windowUpdateFrame[3] = FrameType.WINDOW_UPDATE.getIdByte();
            ByteUtil.set31Bits(windowUpdateFrame, 9, increment);
        }
        else {
            windowUpdateFrame = new byte[0];
        }
        return windowUpdateFrame;
    }
    
    private void writeGoAwayFrame(final int maxStreamId, final long errorCode, final byte[] debugMsg) throws IOException {
        final byte[] fixedPayload = new byte[8];
        ByteUtil.set31Bits(fixedPayload, 0, maxStreamId);
        ByteUtil.setFourBytes(fixedPayload, 4, errorCode);
        int len = 8;
        if (debugMsg != null) {
            len += debugMsg.length;
        }
        final byte[] payloadLength = new byte[3];
        ByteUtil.setThreeBytes(payloadLength, 0, len);
        synchronized (this.socketWrapper) {
            this.socketWrapper.write(true, payloadLength, 0, payloadLength.length);
            this.socketWrapper.write(true, Http2UpgradeHandler.GOAWAY, 0, Http2UpgradeHandler.GOAWAY.length);
            this.socketWrapper.write(true, fixedPayload, 0, 8);
            if (debugMsg != null) {
                this.socketWrapper.write(true, debugMsg, 0, debugMsg.length);
            }
            this.socketWrapper.flush(true);
        }
    }
    
    void writeHeaders(final Stream stream, final int pushedStreamId, final MimeHeaders mimeHeaders, final boolean endOfStream, final int payloadSize) throws IOException {
        synchronized (this.socketWrapper) {
            this.doWriteHeaders(stream, pushedStreamId, mimeHeaders, endOfStream, payloadSize);
        }
        stream.sentHeaders();
        if (endOfStream) {
            stream.sentEndOfStream();
        }
    }
    
    protected void doWriteHeaders(final Stream stream, final int pushedStreamId, final MimeHeaders mimeHeaders, final boolean endOfStream, final int payloadSize) throws IOException {
        if (Http2UpgradeHandler.log.isDebugEnabled()) {
            if (pushedStreamId == 0) {
                Http2UpgradeHandler.log.debug(Http2UpgradeHandler.sm.getString("upgradeHandler.writeHeaders", this.connectionId, stream.getIdAsString(), endOfStream));
            }
            else {
                Http2UpgradeHandler.log.debug(Http2UpgradeHandler.sm.getString("upgradeHandler.writePushHeaders", this.connectionId, stream.getIdAsString(), pushedStreamId, endOfStream));
            }
        }
        if (!stream.canWrite()) {
            return;
        }
        final byte[] header = new byte[9];
        ByteBuffer payload = ByteBuffer.allocate(payloadSize);
        byte[] pushedStreamIdBytes = null;
        if (pushedStreamId > 0) {
            pushedStreamIdBytes = new byte[4];
            ByteUtil.set31Bits(pushedStreamIdBytes, 0, pushedStreamId);
        }
        boolean first = true;
        HpackEncoder.State state = null;
        while (state != HpackEncoder.State.COMPLETE) {
            if (first && pushedStreamIdBytes != null) {
                payload.put(pushedStreamIdBytes);
            }
            state = this.getHpackEncoder().encode(mimeHeaders, payload);
            payload.flip();
            if (state == HpackEncoder.State.COMPLETE || payload.limit() > 0) {
                ByteUtil.setThreeBytes(header, 0, payload.limit());
                if (first) {
                    first = false;
                    if (pushedStreamIdBytes == null) {
                        header[3] = FrameType.HEADERS.getIdByte();
                    }
                    else {
                        header[3] = FrameType.PUSH_PROMISE.getIdByte();
                    }
                    if (endOfStream) {
                        header[4] = 1;
                    }
                }
                else {
                    header[3] = FrameType.CONTINUATION.getIdByte();
                }
                if (state == HpackEncoder.State.COMPLETE) {
                    final byte[] array = header;
                    final int n = 4;
                    array[n] += 4;
                }
                if (Http2UpgradeHandler.log.isDebugEnabled()) {
                    Http2UpgradeHandler.log.debug(payload.limit() + " bytes");
                }
                ByteUtil.set31Bits(header, 5, stream.getIdAsInt());
                try {
                    this.socketWrapper.write(true, header, 0, header.length);
                    this.socketWrapper.write(true, payload);
                    this.socketWrapper.flush(true);
                }
                catch (IOException ioe) {
                    this.handleAppInitiatedIOException(ioe);
                }
                payload.clear();
            }
            else {
                if (state != HpackEncoder.State.UNDERFLOW) {
                    continue;
                }
                payload = ByteBuffer.allocate(payload.capacity() * 2);
            }
        }
    }
    
    private HpackEncoder getHpackEncoder() {
        if (this.hpackEncoder == null) {
            this.hpackEncoder = new HpackEncoder();
        }
        this.hpackEncoder.setMaxTableSize(this.remoteSettings.getHeaderTableSize());
        return this.hpackEncoder;
    }
    
    void writeBody(final Stream stream, final ByteBuffer data, final int len, final boolean finished) throws IOException {
        if (Http2UpgradeHandler.log.isDebugEnabled()) {
            Http2UpgradeHandler.log.debug(Http2UpgradeHandler.sm.getString("upgradeHandler.writeBody", this.connectionId, stream.getIdAsString(), Integer.toString(len)));
        }
        this.reduceOverheadCount();
        final boolean writeable = stream.canWrite();
        final byte[] header = new byte[9];
        ByteUtil.setThreeBytes(header, 0, len);
        header[3] = FrameType.DATA.getIdByte();
        if (finished) {
            header[4] = 1;
            stream.sentEndOfStream();
            if (!stream.isActive()) {
                this.setConnectionTimeoutForStreamCount(this.activeRemoteStreamCount.decrementAndGet());
            }
        }
        if (writeable) {
            ByteUtil.set31Bits(header, 5, stream.getIdAsInt());
            synchronized (this.socketWrapper) {
                try {
                    this.socketWrapper.write(true, header, 0, header.length);
                    final int orgLimit = data.limit();
                    data.limit(data.position() + len);
                    this.socketWrapper.write(true, data);
                    data.limit(orgLimit);
                    this.socketWrapper.flush(true);
                }
                catch (IOException ioe) {
                    this.handleAppInitiatedIOException(ioe);
                }
            }
        }
    }
    
    private void handleAppInitiatedIOException(final IOException ioe) throws IOException {
        this.close();
        throw ioe;
    }
    
    void writeWindowUpdate(final AbstractNonZeroStream stream, final int increment, final boolean applicationInitiated) throws IOException {
        synchronized (this.socketWrapper) {
            final byte[] frame = new byte[13];
            ByteUtil.setThreeBytes(frame, 0, 4);
            frame[3] = FrameType.WINDOW_UPDATE.getIdByte();
            ByteUtil.set31Bits(frame, 9, increment);
            this.socketWrapper.write(true, frame, 0, frame.length);
            if (stream instanceof Stream && ((Stream)stream).canWrite()) {
                ByteUtil.set31Bits(frame, 5, stream.getIdAsInt());
                try {
                    this.socketWrapper.write(true, frame, 0, frame.length);
                    this.socketWrapper.flush(true);
                }
                catch (IOException ioe) {
                    if (!applicationInitiated) {
                        throw ioe;
                    }
                    this.handleAppInitiatedIOException(ioe);
                }
            }
            else {
                this.socketWrapper.flush(true);
            }
        }
    }
    
    private void processWrites() throws IOException {
        synchronized (this.socketWrapper) {
            if (this.socketWrapper.flush(false)) {
                this.socketWrapper.registerWriteInterest();
            }
            else {
                this.pingManager.sendPing(false);
            }
        }
    }
    
    int reserveWindowSize(final Stream stream, final int reservation, final boolean block) throws IOException {
        int allocation = 0;
        synchronized (stream) {
            do {
                synchronized (this) {
                    if (!stream.canWrite()) {
                        stream.doStreamCancel(Http2UpgradeHandler.sm.getString("upgradeHandler.stream.notWritable"), Http2Error.STREAM_CLOSED);
                    }
                    final long windowSize = this.getWindowSize();
                    if (windowSize < 1L || this.backLogSize > 0L) {
                        BacklogTracker tracker = this.backLogStreams.get(stream);
                        if (tracker == null) {
                            tracker = new BacklogTracker(reservation);
                            this.backLogStreams.put(stream, tracker);
                            this.backLogSize += reservation;
                            for (AbstractStream parent = stream.getParentStream(); parent != null && this.backLogStreams.putIfAbsent(parent, new BacklogTracker()) == null; parent = parent.getParentStream()) {}
                        }
                        else if (tracker.getUnusedAllocation() > 0) {
                            allocation = tracker.getUnusedAllocation();
                            this.decrementWindowSize(allocation);
                            if (tracker.getRemainingReservation() == 0) {
                                this.backLogStreams.remove(stream);
                            }
                            else {
                                tracker.useAllocation();
                            }
                        }
                    }
                    else if (windowSize < reservation) {
                        allocation = (int)windowSize;
                        this.decrementWindowSize(allocation);
                    }
                    else {
                        allocation = reservation;
                        this.decrementWindowSize(allocation);
                    }
                }
                if (allocation == 0) {
                    if (block) {
                        try {
                            final long writeTimeout = this.protocol.getWriteTimeout();
                            stream.waitForConnectionAllocation(writeTimeout);
                            final BacklogTracker tracker2;
                            synchronized (this) {
                                tracker2 = this.backLogStreams.get(stream);
                            }
                            if (tracker2 == null || tracker2.getUnusedAllocation() != 0) {
                                continue;
                            }
                            String msg;
                            Http2Error error;
                            if (stream.isActive()) {
                                if (Http2UpgradeHandler.log.isDebugEnabled()) {
                                    Http2UpgradeHandler.log.debug(Http2UpgradeHandler.sm.getString("upgradeHandler.noAllocation", this.connectionId, stream.getIdAsString()));
                                }
                                this.close();
                                msg = Http2UpgradeHandler.sm.getString("stream.writeTimeout");
                                error = Http2Error.ENHANCE_YOUR_CALM;
                            }
                            else {
                                msg = Http2UpgradeHandler.sm.getString("stream.clientCancel");
                                error = Http2Error.STREAM_CLOSED;
                            }
                            stream.doStreamCancel(msg, error);
                            continue;
                        }
                        catch (InterruptedException e) {
                            throw new IOException(Http2UpgradeHandler.sm.getString("upgradeHandler.windowSizeReservationInterrupted", this.connectionId, stream.getIdAsString(), Integer.toString(reservation)), e);
                        }
                    }
                    stream.waitForConnectionAllocationNonBlocking();
                    return 0;
                }
            } while (allocation == 0);
        }
        return allocation;
    }
    
    @Override
    protected void incrementWindowSize(final int increment) throws Http2Exception {
        Set<AbstractStream> streamsToNotify = null;
        synchronized (this) {
            final long windowSize = this.getWindowSize();
            if (windowSize < 1L && windowSize + increment > 0L) {
                streamsToNotify = this.releaseBackLog((int)(windowSize + increment));
            }
            super.incrementWindowSize(increment);
        }
        if (streamsToNotify != null) {
            for (final AbstractStream stream : streamsToNotify) {
                if (Http2UpgradeHandler.log.isDebugEnabled()) {
                    Http2UpgradeHandler.log.debug(Http2UpgradeHandler.sm.getString("upgradeHandler.releaseBacklog", this.connectionId, stream.getIdAsString()));
                }
                if (this == stream) {
                    continue;
                }
                ((Stream)stream).notifyConnection();
            }
        }
    }
    
    private synchronized Set<AbstractStream> releaseBackLog(final int increment) {
        final Set<AbstractStream> result = new HashSet<AbstractStream>();
        if (this.backLogSize < increment) {
            result.addAll((Collection<? extends AbstractStream>)this.backLogStreams.keySet());
            this.backLogStreams.clear();
            this.backLogSize = 0L;
        }
        else {
            for (int leftToAllocate = increment; leftToAllocate > 0; leftToAllocate = this.allocate(this, leftToAllocate)) {}
            for (final Map.Entry<AbstractStream, BacklogTracker> entry : this.backLogStreams.entrySet()) {
                final int allocation = entry.getValue().getUnusedAllocation();
                if (allocation > 0) {
                    this.backLogSize -= allocation;
                    if (entry.getValue().isNotifyInProgress()) {
                        continue;
                    }
                    result.add(entry.getKey());
                    entry.getValue().startNotify();
                }
            }
        }
        return result;
    }
    
    @Deprecated
    @Override
    protected synchronized void doNotifyAll() {
    }
    
    private int allocate(final AbstractStream stream, final int allocation) {
        if (Http2UpgradeHandler.log.isDebugEnabled()) {
            Http2UpgradeHandler.log.debug(Http2UpgradeHandler.sm.getString("upgradeHandler.allocate.debug", this.getConnectionId(), stream.getIdAsString(), Integer.toString(allocation)));
        }
        final BacklogTracker tracker = this.backLogStreams.get(stream);
        int leftToAllocate = tracker.allocate(allocation);
        if (leftToAllocate == 0) {
            return 0;
        }
        if (Http2UpgradeHandler.log.isDebugEnabled()) {
            Http2UpgradeHandler.log.debug(Http2UpgradeHandler.sm.getString("upgradeHandler.allocate.left", this.getConnectionId(), stream.getIdAsString(), Integer.toString(leftToAllocate)));
        }
        final Set<AbstractStream> recipients = new HashSet<AbstractStream>();
        recipients.addAll(stream.getChildStreams());
        recipients.retainAll(this.backLogStreams.keySet());
        while (leftToAllocate > 0) {
            if (recipients.size() == 0) {
                this.backLogStreams.remove(stream);
                return leftToAllocate;
            }
            int totalWeight = 0;
            for (final AbstractStream recipient : recipients) {
                if (Http2UpgradeHandler.log.isDebugEnabled()) {
                    Http2UpgradeHandler.log.debug(Http2UpgradeHandler.sm.getString("upgradeHandler.allocate.recipient", this.getConnectionId(), stream.getIdAsString(), recipient.getIdAsString(), Integer.toString(recipient.getWeight())));
                }
                totalWeight += recipient.getWeight();
            }
            final Iterator<AbstractStream> iter = recipients.iterator();
            int allocated = 0;
            while (iter.hasNext()) {
                final AbstractStream recipient2 = iter.next();
                int share = leftToAllocate * recipient2.getWeight() / totalWeight;
                if (share == 0) {
                    share = 1;
                }
                final int remainder = this.allocate(recipient2, share);
                if (remainder > 0) {
                    iter.remove();
                }
                allocated += share - remainder;
            }
            leftToAllocate -= allocated;
        }
        return 0;
    }
    
    private Stream getStream(final int streamId, final boolean unknownIsError) throws ConnectionException {
        final Integer key = streamId;
        final AbstractStream result = this.streams.get(key);
        if (result instanceof Stream) {
            return (Stream)result;
        }
        if (unknownIsError) {
            throw new ConnectionException(Http2UpgradeHandler.sm.getString("upgradeHandler.stream.closed", key.toString()), Http2Error.PROTOCOL_ERROR);
        }
        return null;
    }
    
    private AbstractNonZeroStream getStreamMayBeClosed(final int streamId, final boolean unknownIsError) throws ConnectionException {
        final Integer key = streamId;
        final AbstractNonZeroStream result = this.streams.get(key);
        if (result == null && unknownIsError) {
            throw new ConnectionException(Http2UpgradeHandler.sm.getString("upgradeHandler.stream.closed", key.toString()), Http2Error.PROTOCOL_ERROR);
        }
        return result;
    }
    
    private Stream createRemoteStream(final int streamId) throws ConnectionException {
        final Integer key = streamId;
        if (streamId % 2 != 1) {
            throw new ConnectionException(Http2UpgradeHandler.sm.getString("upgradeHandler.stream.even", key), Http2Error.PROTOCOL_ERROR);
        }
        this.pruneClosedStreams(streamId);
        final Stream result = new Stream(key, this);
        this.streams.put(key, result);
        return result;
    }
    
    private Stream createLocalStream(final Request request) {
        final int streamId = this.nextLocalStreamId.getAndAdd(2);
        final Integer key = streamId;
        final Stream result = new Stream(key, this, request);
        this.streams.put(key, result);
        return result;
    }
    
    private void close() {
        final ConnectionState previous = this.connectionState.getAndSet(ConnectionState.CLOSED);
        if (previous == ConnectionState.CLOSED) {
            return;
        }
        for (final AbstractNonZeroStream stream : this.streams.values()) {
            if (stream instanceof Stream) {
                ((Stream)stream).receiveReset(Http2Error.CANCEL.getCode());
            }
        }
        try {
            this.socketWrapper.close();
        }
        catch (IOException ioe) {
            Http2UpgradeHandler.log.debug(Http2UpgradeHandler.sm.getString("upgradeHandler.socketCloseFailed"), ioe);
        }
    }
    
    private void pruneClosedStreams(final int streamId) {
        if (this.newStreamsSinceLastPrune < 9) {
            ++this.newStreamsSinceLastPrune;
            return;
        }
        this.newStreamsSinceLastPrune = 0;
        long max = this.localSettings.getMaxConcurrentStreams();
        final int size = this.streams.size();
        if (Http2UpgradeHandler.log.isDebugEnabled()) {
            Http2UpgradeHandler.log.debug(Http2UpgradeHandler.sm.getString("upgradeHandler.pruneStart", this.connectionId, Long.toString(max), Integer.toString(size)));
        }
        max *= 5L;
        if (max > 2147483647L) {
            max = 2147483647L;
        }
        int toClose = size - (int)max;
        if (toClose < 1) {
            return;
        }
        final TreeSet<Integer> candidatesStepTwo = new TreeSet<Integer>();
        final TreeSet<Integer> candidatesStepThree = new TreeSet<Integer>();
        for (AbstractNonZeroStream stream : this.streams.values()) {
            if (stream instanceof Stream && ((Stream)stream).isActive()) {
                continue;
            }
            if (stream.isClosedFinal()) {
                candidatesStepThree.add(stream.getIdentifier());
            }
            else if (stream.getChildStreams().size() == 0) {
                AbstractStream parent = stream.getParentStream();
                this.streams.remove(stream.getIdentifier());
                stream.detachFromParent();
                if (Http2UpgradeHandler.log.isDebugEnabled()) {
                    Http2UpgradeHandler.log.debug(Http2UpgradeHandler.sm.getString("upgradeHandler.pruned", this.connectionId, stream.getIdAsString()));
                }
                if (--toClose < 1) {
                    return;
                }
                while (toClose > 0 && parent.getIdAsInt() > 0 && parent.getIdAsInt() < stream.getIdAsInt() && parent.getChildStreams().isEmpty()) {
                    stream = (AbstractNonZeroStream)parent;
                    parent = stream.getParentStream();
                    this.streams.remove(stream.getIdentifier());
                    stream.detachFromParent();
                    if (Http2UpgradeHandler.log.isDebugEnabled()) {
                        Http2UpgradeHandler.log.debug(Http2UpgradeHandler.sm.getString("upgradeHandler.pruned", this.connectionId, stream.getIdAsString()));
                    }
                    if (--toClose < 1) {
                        return;
                    }
                    candidatesStepTwo.remove(stream.getIdentifier());
                }
            }
            else {
                candidatesStepTwo.add(stream.getIdentifier());
            }
        }
        for (final Integer streamIdToRemove : candidatesStepTwo) {
            this.removeStreamFromPriorityTree(streamIdToRemove);
            if (Http2UpgradeHandler.log.isDebugEnabled()) {
                Http2UpgradeHandler.log.debug(Http2UpgradeHandler.sm.getString("upgradeHandler.pruned", this.connectionId, streamIdToRemove));
            }
            if (--toClose < 1) {
                return;
            }
        }
        while (toClose > 0 && candidatesStepThree.size() > 0) {
            final Integer streamIdToRemove2 = candidatesStepThree.pollLast();
            this.removeStreamFromPriorityTree(streamIdToRemove2);
            if (Http2UpgradeHandler.log.isDebugEnabled()) {
                Http2UpgradeHandler.log.debug(Http2UpgradeHandler.sm.getString("upgradeHandler.prunedPriority", this.connectionId, streamIdToRemove2));
            }
            if (--toClose < 1) {
                return;
            }
        }
        if (toClose > 0) {
            Http2UpgradeHandler.log.warn(Http2UpgradeHandler.sm.getString("upgradeHandler.pruneIncomplete", this.connectionId, Integer.toString(streamId), Integer.toString(toClose)));
        }
    }
    
    private void removeStreamFromPriorityTree(final Integer streamIdToRemove) {
        synchronized (this.priorityTreeLock) {
            final AbstractNonZeroStream streamToRemove = this.streams.remove(streamIdToRemove);
            final Set<AbstractNonZeroStream> children = streamToRemove.getChildStreams();
            if (children.size() == 1) {
                children.iterator().next().rePrioritise(streamToRemove.getParentStream(), streamToRemove.getWeight());
            }
            else {
                int totalWeight = 0;
                for (final AbstractNonZeroStream child : children) {
                    totalWeight += child.getWeight();
                }
                for (final AbstractNonZeroStream child : children) {
                    children.iterator().next().rePrioritise(streamToRemove.getParentStream(), streamToRemove.getWeight() * child.getWeight() / totalWeight);
                }
            }
            streamToRemove.detachFromParent();
            children.clear();
        }
    }
    
    void push(final Request request, final Stream associatedStream) throws IOException {
        if (this.localSettings.getMaxConcurrentStreams() < this.activeRemoteStreamCount.incrementAndGet()) {
            this.setConnectionTimeoutForStreamCount(this.activeRemoteStreamCount.decrementAndGet());
            return;
        }
        final Stream pushStream;
        synchronized (this.socketWrapper) {
            pushStream = this.createLocalStream(request);
            this.writeHeaders(associatedStream, pushStream.getIdAsInt(), request.getMimeHeaders(), false, 1024);
        }
        pushStream.sentPushPromise();
        this.processStreamOnContainerThread(pushStream);
    }
    
    @Override
    protected final String getConnectionId() {
        return this.connectionId;
    }
    
    @Override
    protected final int getWeight() {
        return 0;
    }
    
    boolean isTrailerHeaderAllowed(final String headerName) {
        return this.allowedTrailerHeaders.contains(headerName);
    }
    
    private void reduceOverheadCount() {
        this.overheadCount.decrementAndGet();
    }
    
    private void increaseOverheadCount() {
        this.overheadCount.addAndGet(this.getProtocol().getOverheadCountFactor());
    }
    
    public long getReadTimeout() {
        return this.readTimeout;
    }
    
    public void setReadTimeout(final long readTimeout) {
        this.readTimeout = readTimeout;
    }
    
    public long getKeepAliveTimeout() {
        return this.keepAliveTimeout;
    }
    
    public void setKeepAliveTimeout(final long keepAliveTimeout) {
        this.keepAliveTimeout = keepAliveTimeout;
    }
    
    public long getWriteTimeout() {
        return this.writeTimeout;
    }
    
    public void setWriteTimeout(final long writeTimeout) {
        this.writeTimeout = writeTimeout;
    }
    
    public void setMaxConcurrentStreams(final long maxConcurrentStreams) {
        this.localSettings.set(Setting.MAX_CONCURRENT_STREAMS, maxConcurrentStreams);
    }
    
    public void setMaxConcurrentStreamExecution(final int maxConcurrentStreamExecution) {
        this.maxConcurrentStreamExecution = maxConcurrentStreamExecution;
    }
    
    public void setInitialWindowSize(final int initialWindowSize) {
        this.localSettings.set(Setting.INITIAL_WINDOW_SIZE, initialWindowSize);
    }
    
    public void setAllowedTrailerHeaders(final Set<String> allowedTrailerHeaders) {
        this.allowedTrailerHeaders = allowedTrailerHeaders;
    }
    
    public void setMaxHeaderCount(final int maxHeaderCount) {
        this.maxHeaderCount = maxHeaderCount;
    }
    
    public int getMaxHeaderCount() {
        return this.maxHeaderCount;
    }
    
    public void setMaxHeaderSize(final int maxHeaderSize) {
        this.maxHeaderSize = maxHeaderSize;
    }
    
    public int getMaxHeaderSize() {
        return this.maxHeaderSize;
    }
    
    public void setMaxTrailerCount(final int maxTrailerCount) {
        this.maxTrailerCount = maxTrailerCount;
    }
    
    public int getMaxTrailerCount() {
        return this.maxTrailerCount;
    }
    
    public void setMaxTrailerSize(final int maxTrailerSize) {
        this.maxTrailerSize = maxTrailerSize;
    }
    
    public int getMaxTrailerSize() {
        return this.maxTrailerSize;
    }
    
    public void setInitiatePingDisabled(final boolean initiatePingDisabled) {
        this.pingManager.initiateDisabled = initiatePingDisabled;
    }
    
    @Override
    public boolean fill(final boolean block, final byte[] data) throws IOException {
        return this.fill(block, data, 0, data.length);
    }
    
    @Override
    public boolean fill(final boolean block, final ByteBuffer data, final int len) throws IOException {
        final boolean result = this.fill(block, data.array(), data.arrayOffset() + data.position(), len);
        if (result) {
            data.position(data.position() + len);
        }
        return result;
    }
    
    @Override
    public boolean fill(final boolean block, final byte[] data, final int offset, final int length) throws IOException {
        int len = length;
        int pos = offset;
        boolean nextReadBlock = block;
        int thisRead = 0;
        while (len > 0) {
            thisRead = this.socketWrapper.read(nextReadBlock, data, pos, len);
            if (thisRead == 0) {
                if (nextReadBlock) {
                    throw new IllegalStateException();
                }
                return false;
            }
            else if (thisRead == -1) {
                if (this.connectionState.get().isNewStreamAllowed()) {
                    throw new EOFException();
                }
                return false;
            }
            else {
                pos += thisRead;
                len -= thisRead;
                nextReadBlock = true;
            }
        }
        return true;
    }
    
    @Override
    public int getMaxFrameSize() {
        return this.localSettings.getMaxFrameSize();
    }
    
    @Override
    public HpackDecoder getHpackDecoder() {
        if (this.hpackDecoder == null) {
            this.hpackDecoder = new HpackDecoder(this.localSettings.getHeaderTableSize());
        }
        return this.hpackDecoder;
    }
    
    @Override
    public ByteBuffer startRequestBodyFrame(final int streamId, final int payloadSize, final boolean endOfStream) throws Http2Exception {
        this.reduceOverheadCount();
        if (!endOfStream) {
            final int overheadThreshold = this.protocol.getOverheadDataThreshold();
            int average = (this.lastNonFinalDataPayload >> 1) + (payloadSize >> 1);
            this.lastNonFinalDataPayload = payloadSize;
            if (average == 0) {
                average = 1;
            }
            if (average < overheadThreshold) {
                this.overheadCount.addAndGet(overheadThreshold / average);
            }
        }
        final AbstractNonZeroStream abstractNonZeroStream = this.getStreamMayBeClosed(streamId, true);
        if (abstractNonZeroStream instanceof Stream) {
            final Stream stream = (Stream)abstractNonZeroStream;
            stream.checkState(FrameType.DATA);
            stream.receivedData(payloadSize);
            return stream.getInputByteBuffer();
        }
        abstractNonZeroStream.checkState(FrameType.DATA);
        return null;
    }
    
    @Override
    public void endRequestBodyFrame(final int streamId) throws Http2Exception {
        final AbstractNonZeroStream abstractNonZeroStream = this.getStreamMayBeClosed(streamId, true);
        if (abstractNonZeroStream instanceof Stream) {
            ((Stream)abstractNonZeroStream).getInputBuffer().onDataAvailable();
        }
    }
    
    @Override
    public void receivedEndOfStream(final int streamId) throws ConnectionException {
        final AbstractNonZeroStream abstractNonZeroStream = this.getStreamMayBeClosed(streamId, this.connectionState.get().isNewStreamAllowed());
        if (abstractNonZeroStream instanceof Stream) {
            final Stream stream = (Stream)abstractNonZeroStream;
            stream.receivedEndOfStream();
            if (!stream.isActive()) {
                this.setConnectionTimeoutForStreamCount(this.activeRemoteStreamCount.decrementAndGet());
            }
        }
    }
    
    @Override
    public void swallowedPadding(final int streamId, final int paddingLength) throws ConnectionException, IOException {
        final AbstractNonZeroStream abstractNonZeroStream = this.getStreamMayBeClosed(streamId, true);
        this.writeWindowUpdate(abstractNonZeroStream, paddingLength + 1, false);
    }
    
    @Override
    public HpackDecoder.HeaderEmitter headersStart(final int streamId, final boolean headersEndStream) throws Http2Exception, IOException {
        this.checkPauseState();
        if (!this.connectionState.get().isNewStreamAllowed()) {
            if (Http2UpgradeHandler.log.isDebugEnabled()) {
                Http2UpgradeHandler.log.debug(Http2UpgradeHandler.sm.getString("upgradeHandler.noNewStreams", this.connectionId, Integer.toString(streamId)));
            }
            this.reduceOverheadCount();
            return Http2UpgradeHandler.HEADER_SINK;
        }
        Stream stream = this.getStream(streamId, false);
        if (stream == null) {
            stream = this.createRemoteStream(streamId);
        }
        if (streamId < this.maxActiveRemoteStreamId) {
            throw new ConnectionException(Http2UpgradeHandler.sm.getString("upgradeHandler.stream.old", streamId, this.maxActiveRemoteStreamId), Http2Error.PROTOCOL_ERROR);
        }
        stream.checkState(FrameType.HEADERS);
        stream.receivedStartOfHeaders(headersEndStream);
        this.closeIdleStreams(streamId);
        return stream;
    }
    
    private void closeIdleStreams(final int newMaxActiveRemoteStreamId) {
        final ConcurrentNavigableMap<Integer, AbstractNonZeroStream> subMap = this.streams.subMap(Integer.valueOf(this.maxActiveRemoteStreamId), false, Integer.valueOf(newMaxActiveRemoteStreamId), false);
        for (final AbstractNonZeroStream stream : subMap.values()) {
            if (stream instanceof Stream) {
                ((Stream)stream).closeIfIdle();
            }
        }
        this.maxActiveRemoteStreamId = newMaxActiveRemoteStreamId;
    }
    
    @Override
    public void reprioritise(final int streamId, final int parentStreamId, final boolean exclusive, final int weight) throws Http2Exception {
        if (streamId == parentStreamId) {
            throw new ConnectionException(Http2UpgradeHandler.sm.getString("upgradeHandler.dependency.invalid", this.getConnectionId(), streamId), Http2Error.PROTOCOL_ERROR);
        }
        this.increaseOverheadCount();
        AbstractNonZeroStream abstractNonZeroStream = this.getStreamMayBeClosed(streamId, false);
        if (abstractNonZeroStream == null) {
            abstractNonZeroStream = this.createRemoteStream(streamId);
        }
        AbstractStream parentStream = this.getStreamMayBeClosed(parentStreamId, false);
        if (parentStream == null) {
            parentStream = this;
        }
        synchronized (this.priorityTreeLock) {
            abstractNonZeroStream.rePrioritise(parentStream, exclusive, weight);
        }
    }
    
    @Override
    public void headersContinue(final int payloadSize, final boolean endOfHeaders) {
        if (!endOfHeaders) {
            final int overheadThreshold = this.getProtocol().getOverheadContinuationThreshold();
            if (payloadSize < overheadThreshold) {
                if (payloadSize == 0) {
                    this.overheadCount.addAndGet(overheadThreshold);
                }
                else {
                    this.overheadCount.addAndGet(overheadThreshold / payloadSize);
                }
            }
        }
    }
    
    @Override
    public void headersEnd(final int streamId) throws Http2Exception {
        final AbstractNonZeroStream abstractNonZeroStream = this.getStreamMayBeClosed(streamId, this.connectionState.get().isNewStreamAllowed());
        if (abstractNonZeroStream instanceof Stream) {
            this.setMaxProcessedStream(streamId);
            final Stream stream = (Stream)abstractNonZeroStream;
            if (stream.isActive() && stream.receivedEndOfHeaders()) {
                if (this.localSettings.getMaxConcurrentStreams() < this.activeRemoteStreamCount.incrementAndGet()) {
                    this.setConnectionTimeoutForStreamCount(this.activeRemoteStreamCount.decrementAndGet());
                    this.increaseOverheadCount();
                    throw new StreamException(Http2UpgradeHandler.sm.getString("upgradeHandler.tooManyRemoteStreams", Long.toString(this.localSettings.getMaxConcurrentStreams())), Http2Error.REFUSED_STREAM, streamId);
                }
                this.reduceOverheadCount();
                this.processStreamOnContainerThread(stream);
            }
        }
    }
    
    private void setMaxProcessedStream(final int streamId) {
        if (this.maxProcessedStreamId < streamId) {
            this.maxProcessedStreamId = streamId;
        }
    }
    
    @Override
    public void reset(final int streamId, final long errorCode) throws Http2Exception {
        if (Http2UpgradeHandler.log.isDebugEnabled()) {
            Http2UpgradeHandler.log.debug(Http2UpgradeHandler.sm.getString("upgradeHandler.reset.receive", this.getConnectionId(), Integer.toString(streamId), Long.toString(errorCode)));
        }
        final AbstractNonZeroStream abstractNonZeroStream = this.getStreamMayBeClosed(streamId, true);
        abstractNonZeroStream.checkState(FrameType.RST);
        if (abstractNonZeroStream instanceof Stream) {
            final Stream stream = (Stream)abstractNonZeroStream;
            final boolean active = stream.isActive();
            stream.receiveReset(errorCode);
            if (active) {
                this.activeRemoteStreamCount.decrementAndGet();
            }
        }
    }
    
    @Override
    public void setting(final Setting setting, final long value) throws ConnectionException {
        this.increaseOverheadCount();
        if (setting == null) {
            return;
        }
        if (setting == Setting.INITIAL_WINDOW_SIZE) {
            final long oldValue = this.remoteSettings.getInitialWindowSize();
            this.remoteSettings.set(setting, value);
            final int diff = (int)(value - oldValue);
            for (final AbstractNonZeroStream stream : this.streams.values()) {
                try {
                    stream.incrementWindowSize(diff);
                }
                catch (Http2Exception h2e) {
                    ((Stream)stream).close(new StreamException(Http2UpgradeHandler.sm.getString("upgradeHandler.windowSizeTooBig", this.connectionId, stream.getIdAsString()), h2e.getError(), stream.getIdAsInt()));
                }
            }
        }
        else {
            this.remoteSettings.set(setting, value);
        }
    }
    
    @Override
    public void settingsEnd(final boolean ack) throws IOException {
        if (ack) {
            if (!this.localSettings.ack()) {
                Http2UpgradeHandler.log.warn(Http2UpgradeHandler.sm.getString("upgradeHandler.unexpectedAck", this.connectionId, this.getIdAsString()));
            }
        }
        else {
            synchronized (this.socketWrapper) {
                this.socketWrapper.write(true, Http2UpgradeHandler.SETTINGS_ACK, 0, Http2UpgradeHandler.SETTINGS_ACK.length);
                this.socketWrapper.flush(true);
            }
        }
    }
    
    @Override
    public void pingReceive(final byte[] payload, final boolean ack) throws IOException {
        if (!ack) {
            this.increaseOverheadCount();
        }
        this.pingManager.receivePing(payload, ack);
    }
    
    @Override
    public void goaway(final int lastStreamId, final long errorCode, final String debugData) {
        if (Http2UpgradeHandler.log.isDebugEnabled()) {
            Http2UpgradeHandler.log.debug(Http2UpgradeHandler.sm.getString("upgradeHandler.goaway.debug", this.connectionId, Integer.toString(lastStreamId), Long.toHexString(errorCode), debugData));
        }
        this.close();
    }
    
    @Override
    public void incrementWindowSize(final int streamId, final int increment) throws Http2Exception {
        int average = (this.lastWindowUpdate >> 1) + (increment >> 1);
        final int overheadThreshold = this.protocol.getOverheadWindowUpdateThreshold();
        this.lastWindowUpdate = increment;
        if (average == 0) {
            average = 1;
        }
        if (streamId == 0) {
            if (average < overheadThreshold) {
                this.overheadCount.addAndGet(overheadThreshold / average);
            }
            this.incrementWindowSize(increment);
        }
        else {
            final AbstractNonZeroStream stream = this.getStreamMayBeClosed(streamId, true);
            if (average < overheadThreshold) {
                final BacklogTracker tracker = this.backLogStreams.get(stream);
                if (tracker == null || increment < tracker.getRemainingReservation()) {
                    this.overheadCount.addAndGet(overheadThreshold / average);
                }
            }
            stream.checkState(FrameType.WINDOW_UPDATE);
            stream.incrementWindowSize(increment);
        }
    }
    
    @Override
    public void swallowed(final int streamId, final FrameType frameType, final int flags, final int size) throws IOException {
    }
    
    void replaceStream(final AbstractNonZeroStream original, final AbstractNonZeroStream replacement) {
        synchronized (this.priorityTreeLock) {
            this.streams.replace(original.getIdentifier(), replacement);
            original.replaceStream(replacement);
        }
    }
    
    static {
        log = LogFactory.getLog(Http2UpgradeHandler.class);
        sm = StringManager.getManager(Http2UpgradeHandler.class);
        connectionIdGenerator = new AtomicInteger(0);
        STREAM_ID_ZERO = 0;
        PING = new byte[] { 0, 0, 8, 6, 0, 0, 0, 0, 0 };
        PING_ACK = new byte[] { 0, 0, 8, 6, 1, 0, 0, 0, 0 };
        SETTINGS_ACK = new byte[] { 0, 0, 0, 4, 1, 0, 0, 0, 0 };
        GOAWAY = new byte[] { 7, 0, 0, 0, 0, 0 };
        HEADER_SINK = new HeaderSink();
    }
    
    private class PingManager
    {
        protected boolean initiateDisabled;
        private final long pingIntervalNano = 10000000000L;
        private int sequence;
        private long lastPingNanoTime;
        private Queue<PingRecord> inflightPings;
        private Queue<Long> roundTripTimes;
        
        private PingManager() {
            this.initiateDisabled = false;
            this.sequence = 0;
            this.lastPingNanoTime = Long.MIN_VALUE;
            this.inflightPings = new ConcurrentLinkedQueue<PingRecord>();
            this.roundTripTimes = new ConcurrentLinkedQueue<Long>();
        }
        
        public void sendPing(final boolean force) throws IOException {
            if (this.initiateDisabled) {
                return;
            }
            final long now = System.nanoTime();
            if (force || now - this.lastPingNanoTime > 10000000000L) {
                this.lastPingNanoTime = now;
                final byte[] payload = new byte[8];
                synchronized (Http2UpgradeHandler.this.socketWrapper) {
                    final int sentSequence = ++this.sequence;
                    final PingRecord pingRecord = new PingRecord(sentSequence, now);
                    this.inflightPings.add(pingRecord);
                    ByteUtil.set31Bits(payload, 4, sentSequence);
                    Http2UpgradeHandler.this.socketWrapper.write(true, Http2UpgradeHandler.PING, 0, Http2UpgradeHandler.PING.length);
                    Http2UpgradeHandler.this.socketWrapper.write(true, payload, 0, payload.length);
                    Http2UpgradeHandler.this.socketWrapper.flush(true);
                }
            }
        }
        
        public void receivePing(final byte[] payload, final boolean ack) throws IOException {
            if (ack) {
                int receivedSequence;
                PingRecord pingRecord;
                for (receivedSequence = ByteUtil.get31Bits(payload, 4), pingRecord = this.inflightPings.poll(); pingRecord != null && pingRecord.getSequence() < receivedSequence; pingRecord = this.inflightPings.poll()) {}
                if (pingRecord != null) {
                    final long roundTripTime = System.nanoTime() - pingRecord.getSentNanoTime();
                    this.roundTripTimes.add(roundTripTime);
                    while (this.roundTripTimes.size() > 3) {
                        this.roundTripTimes.poll();
                    }
                    if (Http2UpgradeHandler.log.isDebugEnabled()) {
                        Http2UpgradeHandler.log.debug(Http2UpgradeHandler.sm.getString("pingManager.roundTripTime", Http2UpgradeHandler.this.connectionId, roundTripTime));
                    }
                }
            }
            else {
                synchronized (Http2UpgradeHandler.this.socketWrapper) {
                    Http2UpgradeHandler.this.socketWrapper.write(true, Http2UpgradeHandler.PING_ACK, 0, Http2UpgradeHandler.PING_ACK.length);
                    Http2UpgradeHandler.this.socketWrapper.write(true, payload, 0, payload.length);
                    Http2UpgradeHandler.this.socketWrapper.flush(true);
                }
            }
        }
        
        public long getRoundTripTimeNano() {
            long sum = 0L;
            long count = 0L;
            for (final Long roundTripTime : this.roundTripTimes) {
                sum += roundTripTime;
                ++count;
            }
            if (count > 0L) {
                return sum / count;
            }
            return 0L;
        }
    }
    
    private static class PingRecord
    {
        private final int sequence;
        private final long sentNanoTime;
        
        public PingRecord(final int sequence, final long sentNanoTime) {
            this.sequence = sequence;
            this.sentNanoTime = sentNanoTime;
        }
        
        public int getSequence() {
            return this.sequence;
        }
        
        public long getSentNanoTime() {
            return this.sentNanoTime;
        }
    }
    
    private enum ConnectionState
    {
        NEW(true), 
        CONNECTED(true), 
        PAUSING(true), 
        PAUSED(false), 
        CLOSED(false);
        
        private final boolean newStreamsAllowed;
        
        private ConnectionState(final boolean newStreamsAllowed) {
            this.newStreamsAllowed = newStreamsAllowed;
        }
        
        public boolean isNewStreamAllowed() {
            return this.newStreamsAllowed;
        }
    }
    
    private static class BacklogTracker
    {
        private int remainingReservation;
        private int unusedAllocation;
        private boolean notifyInProgress;
        
        public BacklogTracker() {
        }
        
        public BacklogTracker(final int reservation) {
            this.remainingReservation = reservation;
        }
        
        public int getRemainingReservation() {
            return this.remainingReservation;
        }
        
        public int getUnusedAllocation() {
            return this.unusedAllocation;
        }
        
        public boolean isNotifyInProgress() {
            return this.notifyInProgress;
        }
        
        public void useAllocation() {
            this.unusedAllocation = 0;
            this.notifyInProgress = false;
        }
        
        public void startNotify() {
            this.notifyInProgress = true;
        }
        
        private int allocate(final int allocation) {
            if (this.remainingReservation >= allocation) {
                this.remainingReservation -= allocation;
                this.unusedAllocation += allocation;
                return 0;
            }
            final int left = allocation - this.remainingReservation;
            this.unusedAllocation += this.remainingReservation;
            this.remainingReservation = 0;
            return left;
        }
    }
}
