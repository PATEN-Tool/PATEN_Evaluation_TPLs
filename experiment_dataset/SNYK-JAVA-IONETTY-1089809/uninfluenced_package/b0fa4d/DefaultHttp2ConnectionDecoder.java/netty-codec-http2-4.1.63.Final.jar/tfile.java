// 
// Decompiled by Procyon v0.5.36
// 

package io.netty.handler.codec.http2;

import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpStatusClass;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.SystemPropertyUtil;
import java.util.List;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.logging.InternalLogger;

public class DefaultHttp2ConnectionDecoder implements Http2ConnectionDecoder
{
    private static final boolean VALIDATE_CONTENT_LENGTH;
    private static final InternalLogger logger;
    private Http2FrameListener internalFrameListener;
    private final Http2Connection connection;
    private Http2LifecycleManager lifecycleManager;
    private final Http2ConnectionEncoder encoder;
    private final Http2FrameReader frameReader;
    private Http2FrameListener listener;
    private final Http2PromisedRequestVerifier requestVerifier;
    private final Http2SettingsReceivedConsumer settingsReceivedConsumer;
    private final boolean autoAckPing;
    private final Http2Connection.PropertyKey contentLengthKey;
    
    public DefaultHttp2ConnectionDecoder(final Http2Connection connection, final Http2ConnectionEncoder encoder, final Http2FrameReader frameReader) {
        this(connection, encoder, frameReader, Http2PromisedRequestVerifier.ALWAYS_VERIFY);
    }
    
    public DefaultHttp2ConnectionDecoder(final Http2Connection connection, final Http2ConnectionEncoder encoder, final Http2FrameReader frameReader, final Http2PromisedRequestVerifier requestVerifier) {
        this(connection, encoder, frameReader, requestVerifier, true);
    }
    
    public DefaultHttp2ConnectionDecoder(final Http2Connection connection, final Http2ConnectionEncoder encoder, final Http2FrameReader frameReader, final Http2PromisedRequestVerifier requestVerifier, final boolean autoAckSettings) {
        this(connection, encoder, frameReader, requestVerifier, autoAckSettings, true);
    }
    
    public DefaultHttp2ConnectionDecoder(final Http2Connection connection, final Http2ConnectionEncoder encoder, final Http2FrameReader frameReader, final Http2PromisedRequestVerifier requestVerifier, final boolean autoAckSettings, final boolean autoAckPing) {
        this.internalFrameListener = new PrefaceFrameListener();
        this.autoAckPing = autoAckPing;
        if (autoAckSettings) {
            this.settingsReceivedConsumer = null;
        }
        else {
            if (!(encoder instanceof Http2SettingsReceivedConsumer)) {
                throw new IllegalArgumentException("disabling autoAckSettings requires the encoder to be a " + Http2SettingsReceivedConsumer.class);
            }
            this.settingsReceivedConsumer = (Http2SettingsReceivedConsumer)encoder;
        }
        this.connection = (Http2Connection)ObjectUtil.checkNotNull((Object)connection, "connection");
        this.contentLengthKey = this.connection.newKey();
        this.frameReader = (Http2FrameReader)ObjectUtil.checkNotNull((Object)frameReader, "frameReader");
        this.encoder = (Http2ConnectionEncoder)ObjectUtil.checkNotNull((Object)encoder, "encoder");
        this.requestVerifier = (Http2PromisedRequestVerifier)ObjectUtil.checkNotNull((Object)requestVerifier, "requestVerifier");
        if (connection.local().flowController() == null) {
            connection.local().flowController(new DefaultHttp2LocalFlowController(connection));
        }
        connection.local().flowController().frameWriter(encoder.frameWriter());
    }
    
    @Override
    public void lifecycleManager(final Http2LifecycleManager lifecycleManager) {
        this.lifecycleManager = (Http2LifecycleManager)ObjectUtil.checkNotNull((Object)lifecycleManager, "lifecycleManager");
    }
    
    @Override
    public Http2Connection connection() {
        return this.connection;
    }
    
    @Override
    public final Http2LocalFlowController flowController() {
        return this.connection.local().flowController();
    }
    
    @Override
    public void frameListener(final Http2FrameListener listener) {
        this.listener = (Http2FrameListener)ObjectUtil.checkNotNull((Object)listener, "listener");
    }
    
    @Override
    public Http2FrameListener frameListener() {
        return this.listener;
    }
    
    Http2FrameListener internalFrameListener() {
        return this.internalFrameListener;
    }
    
    @Override
    public boolean prefaceReceived() {
        return FrameReadListener.class == this.internalFrameListener.getClass();
    }
    
    @Override
    public void decodeFrame(final ChannelHandlerContext ctx, final ByteBuf in, final List<Object> out) throws Http2Exception {
        this.frameReader.readFrame(ctx, in, this.internalFrameListener);
    }
    
    @Override
    public Http2Settings localSettings() {
        final Http2Settings settings = new Http2Settings();
        final Http2FrameReader.Configuration config = this.frameReader.configuration();
        final Http2HeadersDecoder.Configuration headersConfig = config.headersConfiguration();
        final Http2FrameSizePolicy frameSizePolicy = config.frameSizePolicy();
        settings.initialWindowSize(this.flowController().initialWindowSize());
        settings.maxConcurrentStreams(this.connection.remote().maxActiveStreams());
        settings.headerTableSize(headersConfig.maxHeaderTableSize());
        settings.maxFrameSize(frameSizePolicy.maxFrameSize());
        settings.maxHeaderListSize(headersConfig.maxHeaderListSize());
        if (!this.connection.isServer()) {
            settings.pushEnabled(this.connection.local().allowPushTo());
        }
        return settings;
    }
    
    @Override
    public void close() {
        this.frameReader.close();
    }
    
    protected long calculateMaxHeaderListSizeGoAway(final long maxHeaderListSize) {
        return Http2CodecUtil.calculateMaxHeaderListSizeGoAway(maxHeaderListSize);
    }
    
    private int unconsumedBytes(final Http2Stream stream) {
        return this.flowController().unconsumedBytes(stream);
    }
    
    void onGoAwayRead0(final ChannelHandlerContext ctx, final int lastStreamId, final long errorCode, final ByteBuf debugData) throws Http2Exception {
        this.listener.onGoAwayRead(ctx, lastStreamId, errorCode, debugData);
        this.connection.goAwayReceived(lastStreamId, errorCode, debugData);
    }
    
    void onUnknownFrame0(final ChannelHandlerContext ctx, final byte frameType, final int streamId, final Http2Flags flags, final ByteBuf payload) throws Http2Exception {
        this.listener.onUnknownFrame(ctx, frameType, streamId, flags, payload);
    }
    
    private void verifyContentLength(final Http2Stream stream, final int data, final boolean isEnd) throws Http2Exception {
        if (!DefaultHttp2ConnectionDecoder.VALIDATE_CONTENT_LENGTH) {
            return;
        }
        final ContentLength contentLength = stream.getProperty(this.contentLengthKey);
        if (contentLength != null) {
            try {
                contentLength.increaseReceivedBytes(this.connection.isServer(), stream.id(), data, isEnd);
            }
            finally {
                if (isEnd) {
                    stream.removeProperty(this.contentLengthKey);
                }
            }
        }
    }
    
    static {
        VALIDATE_CONTENT_LENGTH = SystemPropertyUtil.getBoolean("io.netty.http2.validateContentLength", true);
        logger = InternalLoggerFactory.getInstance((Class)DefaultHttp2ConnectionDecoder.class);
    }
    
    private final class FrameReadListener implements Http2FrameListener
    {
        @Override
        public int onDataRead(final ChannelHandlerContext ctx, final int streamId, final ByteBuf data, final int padding, final boolean endOfStream) throws Http2Exception {
            final Http2Stream stream = DefaultHttp2ConnectionDecoder.this.connection.stream(streamId);
            final Http2LocalFlowController flowController = DefaultHttp2ConnectionDecoder.this.flowController();
            final int readable = data.readableBytes();
            int bytesToReturn = readable + padding;
            boolean shouldIgnore;
            try {
                shouldIgnore = this.shouldIgnoreHeadersOrDataFrame(ctx, streamId, stream, "DATA");
            }
            catch (Http2Exception e) {
                flowController.receiveFlowControlledFrame(stream, data, padding, endOfStream);
                flowController.consumeBytes(stream, bytesToReturn);
                throw e;
            }
            catch (Throwable t) {
                throw Http2Exception.connectionError(Http2Error.INTERNAL_ERROR, t, "Unhandled error on data stream id %d", streamId);
            }
            if (shouldIgnore) {
                flowController.receiveFlowControlledFrame(stream, data, padding, endOfStream);
                flowController.consumeBytes(stream, bytesToReturn);
                this.verifyStreamMayHaveExisted(streamId);
                return bytesToReturn;
            }
            Http2Exception error = null;
            switch (stream.state()) {
                case OPEN:
                case HALF_CLOSED_LOCAL: {
                    break;
                }
                case HALF_CLOSED_REMOTE:
                case CLOSED: {
                    error = Http2Exception.streamError(stream.id(), Http2Error.STREAM_CLOSED, "Stream %d in unexpected state: %s", stream.id(), stream.state());
                    break;
                }
                default: {
                    error = Http2Exception.streamError(stream.id(), Http2Error.PROTOCOL_ERROR, "Stream %d in unexpected state: %s", stream.id(), stream.state());
                    break;
                }
            }
            int unconsumedBytes = DefaultHttp2ConnectionDecoder.this.unconsumedBytes(stream);
            try {
                flowController.receiveFlowControlledFrame(stream, data, padding, endOfStream);
                unconsumedBytes = DefaultHttp2ConnectionDecoder.this.unconsumedBytes(stream);
                if (error != null) {
                    throw error;
                }
                DefaultHttp2ConnectionDecoder.this.verifyContentLength(stream, readable, endOfStream);
                bytesToReturn = DefaultHttp2ConnectionDecoder.this.listener.onDataRead(ctx, streamId, data, padding, endOfStream);
                if (endOfStream) {
                    DefaultHttp2ConnectionDecoder.this.lifecycleManager.closeStreamRemote(stream, ctx.newSucceededFuture());
                }
                return bytesToReturn;
            }
            catch (Http2Exception e2) {
                final int delta = unconsumedBytes - DefaultHttp2ConnectionDecoder.this.unconsumedBytes(stream);
                bytesToReturn -= delta;
                throw e2;
            }
            catch (RuntimeException e3) {
                final int delta = unconsumedBytes - DefaultHttp2ConnectionDecoder.this.unconsumedBytes(stream);
                bytesToReturn -= delta;
                throw e3;
            }
            finally {
                flowController.consumeBytes(stream, bytesToReturn);
            }
        }
        
        @Override
        public void onHeadersRead(final ChannelHandlerContext ctx, final int streamId, final Http2Headers headers, final int padding, final boolean endOfStream) throws Http2Exception {
            this.onHeadersRead(ctx, streamId, headers, 0, (short)16, false, padding, endOfStream);
        }
        
        @Override
        public void onHeadersRead(final ChannelHandlerContext ctx, final int streamId, final Http2Headers headers, final int streamDependency, final short weight, final boolean exclusive, final int padding, final boolean endOfStream) throws Http2Exception {
            Http2Stream stream = DefaultHttp2ConnectionDecoder.this.connection.stream(streamId);
            boolean allowHalfClosedRemote = false;
            boolean isTrailers = false;
            if (stream == null && !DefaultHttp2ConnectionDecoder.this.connection.streamMayHaveExisted(streamId)) {
                stream = DefaultHttp2ConnectionDecoder.this.connection.remote().createStream(streamId, endOfStream);
                allowHalfClosedRemote = (stream.state() == Http2Stream.State.HALF_CLOSED_REMOTE);
            }
            else if (stream != null) {
                isTrailers = stream.isHeadersReceived();
            }
            if (this.shouldIgnoreHeadersOrDataFrame(ctx, streamId, stream, "HEADERS")) {
                return;
            }
            final boolean isInformational = !DefaultHttp2ConnectionDecoder.this.connection.isServer() && HttpStatusClass.valueOf(headers.status()) == HttpStatusClass.INFORMATIONAL;
            if (((isInformational || !endOfStream) && stream.isHeadersReceived()) || stream.isTrailersReceived()) {
                throw Http2Exception.streamError(streamId, Http2Error.PROTOCOL_ERROR, "Stream %d received too many headers EOS: %s state: %s", streamId, endOfStream, stream.state());
            }
            switch (stream.state()) {
                case RESERVED_REMOTE: {
                    stream.open(endOfStream);
                    break;
                }
                case OPEN:
                case HALF_CLOSED_LOCAL: {
                    break;
                }
                case HALF_CLOSED_REMOTE: {
                    if (!allowHalfClosedRemote) {
                        throw Http2Exception.streamError(stream.id(), Http2Error.STREAM_CLOSED, "Stream %d in unexpected state: %s", stream.id(), stream.state());
                    }
                    break;
                }
                case CLOSED: {
                    throw Http2Exception.streamError(stream.id(), Http2Error.STREAM_CLOSED, "Stream %d in unexpected state: %s", stream.id(), stream.state());
                }
                default: {
                    throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Stream %d in unexpected state: %s", stream.id(), stream.state());
                }
            }
            if (!isTrailers) {
                final List<? extends CharSequence> contentLength = (List<? extends CharSequence>)headers.getAll((Object)HttpHeaderNames.CONTENT_LENGTH);
                if (contentLength != null && !contentLength.isEmpty()) {
                    try {
                        final long cLength = HttpUtil.normalizeAndGetContentLength((List)contentLength, false, true);
                        if (cLength != -1L) {
                            headers.setLong((Object)HttpHeaderNames.CONTENT_LENGTH, cLength);
                            stream.setProperty(DefaultHttp2ConnectionDecoder.this.contentLengthKey, new ContentLength(cLength));
                        }
                    }
                    catch (IllegalArgumentException e) {
                        throw Http2Exception.streamError(stream.id(), Http2Error.PROTOCOL_ERROR, e, "Multiple content-length headers received", new Object[0]);
                    }
                }
            }
            stream.headersReceived(isInformational);
            DefaultHttp2ConnectionDecoder.this.verifyContentLength(stream, 0, endOfStream);
            DefaultHttp2ConnectionDecoder.this.encoder.flowController().updateDependencyTree(streamId, streamDependency, weight, exclusive);
            DefaultHttp2ConnectionDecoder.this.listener.onHeadersRead(ctx, streamId, headers, streamDependency, weight, exclusive, padding, endOfStream);
            if (endOfStream) {
                DefaultHttp2ConnectionDecoder.this.lifecycleManager.closeStreamRemote(stream, ctx.newSucceededFuture());
            }
        }
        
        @Override
        public void onPriorityRead(final ChannelHandlerContext ctx, final int streamId, final int streamDependency, final short weight, final boolean exclusive) throws Http2Exception {
            DefaultHttp2ConnectionDecoder.this.encoder.flowController().updateDependencyTree(streamId, streamDependency, weight, exclusive);
            DefaultHttp2ConnectionDecoder.this.listener.onPriorityRead(ctx, streamId, streamDependency, weight, exclusive);
        }
        
        @Override
        public void onRstStreamRead(final ChannelHandlerContext ctx, final int streamId, final long errorCode) throws Http2Exception {
            final Http2Stream stream = DefaultHttp2ConnectionDecoder.this.connection.stream(streamId);
            if (stream == null) {
                this.verifyStreamMayHaveExisted(streamId);
                return;
            }
            switch (stream.state()) {
                case IDLE: {
                    throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "RST_STREAM received for IDLE stream %d", streamId);
                }
                case CLOSED: {}
                default: {
                    DefaultHttp2ConnectionDecoder.this.listener.onRstStreamRead(ctx, streamId, errorCode);
                    DefaultHttp2ConnectionDecoder.this.lifecycleManager.closeStream(stream, ctx.newSucceededFuture());
                }
            }
        }
        
        @Override
        public void onSettingsAckRead(final ChannelHandlerContext ctx) throws Http2Exception {
            final Http2Settings settings = DefaultHttp2ConnectionDecoder.this.encoder.pollSentSettings();
            if (settings != null) {
                this.applyLocalSettings(settings);
            }
            DefaultHttp2ConnectionDecoder.this.listener.onSettingsAckRead(ctx);
        }
        
        private void applyLocalSettings(final Http2Settings settings) throws Http2Exception {
            final Boolean pushEnabled = settings.pushEnabled();
            final Http2FrameReader.Configuration config = DefaultHttp2ConnectionDecoder.this.frameReader.configuration();
            final Http2HeadersDecoder.Configuration headerConfig = config.headersConfiguration();
            final Http2FrameSizePolicy frameSizePolicy = config.frameSizePolicy();
            if (pushEnabled != null) {
                if (DefaultHttp2ConnectionDecoder.this.connection.isServer()) {
                    throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Server sending SETTINGS frame with ENABLE_PUSH specified", new Object[0]);
                }
                DefaultHttp2ConnectionDecoder.this.connection.local().allowPushTo(pushEnabled);
            }
            final Long maxConcurrentStreams = settings.maxConcurrentStreams();
            if (maxConcurrentStreams != null) {
                DefaultHttp2ConnectionDecoder.this.connection.remote().maxActiveStreams((int)Math.min(maxConcurrentStreams, 2147483647L));
            }
            final Long headerTableSize = settings.headerTableSize();
            if (headerTableSize != null) {
                headerConfig.maxHeaderTableSize(headerTableSize);
            }
            final Long maxHeaderListSize = settings.maxHeaderListSize();
            if (maxHeaderListSize != null) {
                headerConfig.maxHeaderListSize(maxHeaderListSize, DefaultHttp2ConnectionDecoder.this.calculateMaxHeaderListSizeGoAway(maxHeaderListSize));
            }
            final Integer maxFrameSize = settings.maxFrameSize();
            if (maxFrameSize != null) {
                frameSizePolicy.maxFrameSize(maxFrameSize);
            }
            final Integer initialWindowSize = settings.initialWindowSize();
            if (initialWindowSize != null) {
                DefaultHttp2ConnectionDecoder.this.flowController().initialWindowSize(initialWindowSize);
            }
        }
        
        @Override
        public void onSettingsRead(final ChannelHandlerContext ctx, final Http2Settings settings) throws Http2Exception {
            if (DefaultHttp2ConnectionDecoder.this.settingsReceivedConsumer == null) {
                DefaultHttp2ConnectionDecoder.this.encoder.writeSettingsAck(ctx, ctx.newPromise());
                DefaultHttp2ConnectionDecoder.this.encoder.remoteSettings(settings);
            }
            else {
                DefaultHttp2ConnectionDecoder.this.settingsReceivedConsumer.consumeReceivedSettings(settings);
            }
            DefaultHttp2ConnectionDecoder.this.listener.onSettingsRead(ctx, settings);
        }
        
        @Override
        public void onPingRead(final ChannelHandlerContext ctx, final long data) throws Http2Exception {
            if (DefaultHttp2ConnectionDecoder.this.autoAckPing) {
                DefaultHttp2ConnectionDecoder.this.encoder.writePing(ctx, true, data, ctx.newPromise());
            }
            DefaultHttp2ConnectionDecoder.this.listener.onPingRead(ctx, data);
        }
        
        @Override
        public void onPingAckRead(final ChannelHandlerContext ctx, final long data) throws Http2Exception {
            DefaultHttp2ConnectionDecoder.this.listener.onPingAckRead(ctx, data);
        }
        
        @Override
        public void onPushPromiseRead(final ChannelHandlerContext ctx, final int streamId, final int promisedStreamId, final Http2Headers headers, final int padding) throws Http2Exception {
            if (DefaultHttp2ConnectionDecoder.this.connection().isServer()) {
                throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "A client cannot push.", new Object[0]);
            }
            final Http2Stream parentStream = DefaultHttp2ConnectionDecoder.this.connection.stream(streamId);
            if (this.shouldIgnoreHeadersOrDataFrame(ctx, streamId, parentStream, "PUSH_PROMISE")) {
                return;
            }
            switch (parentStream.state()) {
                case OPEN:
                case HALF_CLOSED_LOCAL: {
                    if (!DefaultHttp2ConnectionDecoder.this.requestVerifier.isAuthoritative(ctx, headers)) {
                        throw Http2Exception.streamError(promisedStreamId, Http2Error.PROTOCOL_ERROR, "Promised request on stream %d for promised stream %d is not authoritative", streamId, promisedStreamId);
                    }
                    if (!DefaultHttp2ConnectionDecoder.this.requestVerifier.isCacheable(headers)) {
                        throw Http2Exception.streamError(promisedStreamId, Http2Error.PROTOCOL_ERROR, "Promised request on stream %d for promised stream %d is not known to be cacheable", streamId, promisedStreamId);
                    }
                    if (!DefaultHttp2ConnectionDecoder.this.requestVerifier.isSafe(headers)) {
                        throw Http2Exception.streamError(promisedStreamId, Http2Error.PROTOCOL_ERROR, "Promised request on stream %d for promised stream %d is not known to be safe", streamId, promisedStreamId);
                    }
                    DefaultHttp2ConnectionDecoder.this.connection.remote().reservePushStream(promisedStreamId, parentStream);
                    DefaultHttp2ConnectionDecoder.this.listener.onPushPromiseRead(ctx, streamId, promisedStreamId, headers, padding);
                }
                default: {
                    throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Stream %d in unexpected state for receiving push promise: %s", parentStream.id(), parentStream.state());
                }
            }
        }
        
        @Override
        public void onGoAwayRead(final ChannelHandlerContext ctx, final int lastStreamId, final long errorCode, final ByteBuf debugData) throws Http2Exception {
            DefaultHttp2ConnectionDecoder.this.onGoAwayRead0(ctx, lastStreamId, errorCode, debugData);
        }
        
        @Override
        public void onWindowUpdateRead(final ChannelHandlerContext ctx, final int streamId, final int windowSizeIncrement) throws Http2Exception {
            final Http2Stream stream = DefaultHttp2ConnectionDecoder.this.connection.stream(streamId);
            if (stream == null || stream.state() == Http2Stream.State.CLOSED || this.streamCreatedAfterGoAwaySent(streamId)) {
                this.verifyStreamMayHaveExisted(streamId);
                return;
            }
            DefaultHttp2ConnectionDecoder.this.encoder.flowController().incrementWindowSize(stream, windowSizeIncrement);
            DefaultHttp2ConnectionDecoder.this.listener.onWindowUpdateRead(ctx, streamId, windowSizeIncrement);
        }
        
        @Override
        public void onUnknownFrame(final ChannelHandlerContext ctx, final byte frameType, final int streamId, final Http2Flags flags, final ByteBuf payload) throws Http2Exception {
            DefaultHttp2ConnectionDecoder.this.onUnknownFrame0(ctx, frameType, streamId, flags, payload);
        }
        
        private boolean shouldIgnoreHeadersOrDataFrame(final ChannelHandlerContext ctx, final int streamId, final Http2Stream stream, final String frameName) throws Http2Exception {
            if (stream == null) {
                if (this.streamCreatedAfterGoAwaySent(streamId)) {
                    DefaultHttp2ConnectionDecoder.logger.info("{} ignoring {} frame for stream {}. Stream sent after GOAWAY sent", new Object[] { ctx.channel(), frameName, streamId });
                    return true;
                }
                this.verifyStreamMayHaveExisted(streamId);
                throw Http2Exception.streamError(streamId, Http2Error.STREAM_CLOSED, "Received %s frame for an unknown stream %d", frameName, streamId);
            }
            else {
                if (stream.isResetSent() || this.streamCreatedAfterGoAwaySent(streamId)) {
                    if (DefaultHttp2ConnectionDecoder.logger.isInfoEnabled()) {
                        DefaultHttp2ConnectionDecoder.logger.info("{} ignoring {} frame for stream {}", new Object[] { ctx.channel(), frameName, stream.isResetSent() ? "RST_STREAM sent." : ("Stream created after GOAWAY sent. Last known stream by peer " + DefaultHttp2ConnectionDecoder.this.connection.remote().lastStreamKnownByPeer()) });
                    }
                    return true;
                }
                return false;
            }
        }
        
        private boolean streamCreatedAfterGoAwaySent(final int streamId) {
            final Http2Connection.Endpoint<?> remote = DefaultHttp2ConnectionDecoder.this.connection.remote();
            return DefaultHttp2ConnectionDecoder.this.connection.goAwaySent() && remote.isValidStreamId(streamId) && streamId > remote.lastStreamKnownByPeer();
        }
        
        private void verifyStreamMayHaveExisted(final int streamId) throws Http2Exception {
            if (!DefaultHttp2ConnectionDecoder.this.connection.streamMayHaveExisted(streamId)) {
                throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Stream %d does not exist", streamId);
            }
        }
    }
    
    private final class PrefaceFrameListener implements Http2FrameListener
    {
        private void verifyPrefaceReceived() throws Http2Exception {
            if (!DefaultHttp2ConnectionDecoder.this.prefaceReceived()) {
                throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Received non-SETTINGS as first frame.", new Object[0]);
            }
        }
        
        @Override
        public int onDataRead(final ChannelHandlerContext ctx, final int streamId, final ByteBuf data, final int padding, final boolean endOfStream) throws Http2Exception {
            this.verifyPrefaceReceived();
            return DefaultHttp2ConnectionDecoder.this.internalFrameListener.onDataRead(ctx, streamId, data, padding, endOfStream);
        }
        
        @Override
        public void onHeadersRead(final ChannelHandlerContext ctx, final int streamId, final Http2Headers headers, final int padding, final boolean endOfStream) throws Http2Exception {
            this.verifyPrefaceReceived();
            DefaultHttp2ConnectionDecoder.this.internalFrameListener.onHeadersRead(ctx, streamId, headers, padding, endOfStream);
        }
        
        @Override
        public void onHeadersRead(final ChannelHandlerContext ctx, final int streamId, final Http2Headers headers, final int streamDependency, final short weight, final boolean exclusive, final int padding, final boolean endOfStream) throws Http2Exception {
            this.verifyPrefaceReceived();
            DefaultHttp2ConnectionDecoder.this.internalFrameListener.onHeadersRead(ctx, streamId, headers, streamDependency, weight, exclusive, padding, endOfStream);
        }
        
        @Override
        public void onPriorityRead(final ChannelHandlerContext ctx, final int streamId, final int streamDependency, final short weight, final boolean exclusive) throws Http2Exception {
            this.verifyPrefaceReceived();
            DefaultHttp2ConnectionDecoder.this.internalFrameListener.onPriorityRead(ctx, streamId, streamDependency, weight, exclusive);
        }
        
        @Override
        public void onRstStreamRead(final ChannelHandlerContext ctx, final int streamId, final long errorCode) throws Http2Exception {
            this.verifyPrefaceReceived();
            DefaultHttp2ConnectionDecoder.this.internalFrameListener.onRstStreamRead(ctx, streamId, errorCode);
        }
        
        @Override
        public void onSettingsAckRead(final ChannelHandlerContext ctx) throws Http2Exception {
            this.verifyPrefaceReceived();
            DefaultHttp2ConnectionDecoder.this.internalFrameListener.onSettingsAckRead(ctx);
        }
        
        @Override
        public void onSettingsRead(final ChannelHandlerContext ctx, final Http2Settings settings) throws Http2Exception {
            if (!DefaultHttp2ConnectionDecoder.this.prefaceReceived()) {
                DefaultHttp2ConnectionDecoder.this.internalFrameListener = new FrameReadListener();
            }
            DefaultHttp2ConnectionDecoder.this.internalFrameListener.onSettingsRead(ctx, settings);
        }
        
        @Override
        public void onPingRead(final ChannelHandlerContext ctx, final long data) throws Http2Exception {
            this.verifyPrefaceReceived();
            DefaultHttp2ConnectionDecoder.this.internalFrameListener.onPingRead(ctx, data);
        }
        
        @Override
        public void onPingAckRead(final ChannelHandlerContext ctx, final long data) throws Http2Exception {
            this.verifyPrefaceReceived();
            DefaultHttp2ConnectionDecoder.this.internalFrameListener.onPingAckRead(ctx, data);
        }
        
        @Override
        public void onPushPromiseRead(final ChannelHandlerContext ctx, final int streamId, final int promisedStreamId, final Http2Headers headers, final int padding) throws Http2Exception {
            this.verifyPrefaceReceived();
            DefaultHttp2ConnectionDecoder.this.internalFrameListener.onPushPromiseRead(ctx, streamId, promisedStreamId, headers, padding);
        }
        
        @Override
        public void onGoAwayRead(final ChannelHandlerContext ctx, final int lastStreamId, final long errorCode, final ByteBuf debugData) throws Http2Exception {
            DefaultHttp2ConnectionDecoder.this.onGoAwayRead0(ctx, lastStreamId, errorCode, debugData);
        }
        
        @Override
        public void onWindowUpdateRead(final ChannelHandlerContext ctx, final int streamId, final int windowSizeIncrement) throws Http2Exception {
            this.verifyPrefaceReceived();
            DefaultHttp2ConnectionDecoder.this.internalFrameListener.onWindowUpdateRead(ctx, streamId, windowSizeIncrement);
        }
        
        @Override
        public void onUnknownFrame(final ChannelHandlerContext ctx, final byte frameType, final int streamId, final Http2Flags flags, final ByteBuf payload) throws Http2Exception {
            DefaultHttp2ConnectionDecoder.this.onUnknownFrame0(ctx, frameType, streamId, flags, payload);
        }
    }
    
    private static final class ContentLength
    {
        private final long expected;
        private long seen;
        
        ContentLength(final long expected) {
            this.expected = expected;
        }
        
        void increaseReceivedBytes(final boolean server, final int streamId, final int bytes, final boolean isEnd) throws Http2Exception {
            this.seen += bytes;
            if (this.seen < 0L) {
                throw Http2Exception.streamError(streamId, Http2Error.PROTOCOL_ERROR, "Received amount of data did overflow and so not match content-length header %d", this.expected);
            }
            if (this.seen > this.expected) {
                throw Http2Exception.streamError(streamId, Http2Error.PROTOCOL_ERROR, "Received amount of data %d does not match content-length header %d", this.seen, this.expected);
            }
            if (isEnd) {
                if (this.seen == 0L && !server) {
                    return;
                }
                if (this.expected > this.seen) {
                    throw Http2Exception.streamError(streamId, Http2Error.PROTOCOL_ERROR, "Received amount of data %d does not match content-length header %d", this.seen, this.expected);
                }
            }
        }
    }
}
