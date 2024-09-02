// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.tomcat.websocket;

import java.util.List;
import javax.websocket.Extension;
import javax.websocket.Session;
import org.apache.tomcat.util.ExceptionUtils;
import javax.websocket.PongMessage;
import java.nio.charset.CoderResult;
import org.apache.juli.logging.Log;
import javax.websocket.CloseReason;
import java.io.IOException;
import java.nio.charset.CodingErrorAction;
import org.apache.tomcat.util.buf.Utf8Decoder;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import javax.websocket.MessageHandler;
import java.nio.charset.CharsetDecoder;
import java.nio.CharBuffer;
import java.nio.ByteBuffer;
import org.apache.tomcat.util.res.StringManager;

public abstract class WsFrameBase
{
    private static final StringManager sm;
    protected final WsSession wsSession;
    protected final ByteBuffer inputBuffer;
    private final Transformation transformation;
    private final ByteBuffer controlBufferBinary;
    private final CharBuffer controlBufferText;
    private final CharsetDecoder utf8DecoderControl;
    private final CharsetDecoder utf8DecoderMessage;
    private boolean continuationExpected;
    private boolean textMessage;
    private ByteBuffer messageBufferBinary;
    private CharBuffer messageBufferText;
    private MessageHandler binaryMsgHandler;
    private MessageHandler textMsgHandler;
    private boolean fin;
    private int rsv;
    private byte opCode;
    private final byte[] mask;
    private int maskIndex;
    private long payloadLength;
    private volatile long payloadWritten;
    private volatile State state;
    private volatile boolean open;
    private static final AtomicReferenceFieldUpdater<WsFrameBase, ReadState> READ_STATE_UPDATER;
    private volatile ReadState readState;
    
    public WsFrameBase(final WsSession wsSession, final Transformation transformation) {
        this.controlBufferBinary = ByteBuffer.allocate(125);
        this.controlBufferText = CharBuffer.allocate(125);
        this.utf8DecoderControl = new Utf8Decoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
        this.utf8DecoderMessage = new Utf8Decoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
        this.continuationExpected = false;
        this.textMessage = false;
        this.binaryMsgHandler = null;
        this.textMsgHandler = null;
        this.fin = false;
        this.rsv = 0;
        this.opCode = 0;
        this.mask = new byte[4];
        this.maskIndex = 0;
        this.payloadLength = 0L;
        this.payloadWritten = 0L;
        this.state = State.NEW_FRAME;
        this.open = true;
        this.readState = ReadState.WAITING;
        this.inputBuffer = ByteBuffer.allocate(Constants.DEFAULT_BUFFER_SIZE);
        this.inputBuffer.position(0).limit(0);
        this.messageBufferBinary = ByteBuffer.allocate(wsSession.getMaxBinaryMessageBufferSize());
        this.messageBufferText = CharBuffer.allocate(wsSession.getMaxTextMessageBufferSize());
        wsSession.setWsFrame(this);
        this.wsSession = wsSession;
        Transformation finalTransformation;
        if (this.isMasked()) {
            finalTransformation = new UnmaskTransformation();
        }
        else {
            finalTransformation = new NoopTransformation();
        }
        if (transformation == null) {
            this.transformation = finalTransformation;
        }
        else {
            transformation.setNext(finalTransformation);
            this.transformation = transformation;
        }
    }
    
    protected void processInputBuffer() throws IOException {
        while (!this.isSuspended()) {
            this.wsSession.updateLastActive();
            if (this.state == State.NEW_FRAME) {
                if (!this.processInitialHeader()) {
                    break;
                }
                if (!this.open) {
                    throw new IOException(WsFrameBase.sm.getString("wsFrame.closed"));
                }
            }
            if (this.state == State.PARTIAL_HEADER && !this.processRemainingHeader()) {
                break;
            }
            if (this.state == State.DATA && !this.processData()) {
                break;
            }
        }
    }
    
    private boolean processInitialHeader() throws IOException {
        if (this.inputBuffer.remaining() < 2) {
            return false;
        }
        int b = this.inputBuffer.get();
        this.fin = ((b & 0x80) > 0);
        this.rsv = (b & 0x70) >>> 4;
        this.opCode = (byte)(b & 0xF);
        if (!this.transformation.validateRsv(this.rsv, this.opCode)) {
            throw new WsIOException(new CloseReason((CloseReason.CloseCode)CloseReason.CloseCodes.PROTOCOL_ERROR, WsFrameBase.sm.getString("wsFrame.wrongRsv", new Object[] { this.rsv, this.opCode })));
        }
        if (Util.isControl(this.opCode)) {
            if (!this.fin) {
                throw new WsIOException(new CloseReason((CloseReason.CloseCode)CloseReason.CloseCodes.PROTOCOL_ERROR, WsFrameBase.sm.getString("wsFrame.controlFragmented")));
            }
            if (this.opCode != 9 && this.opCode != 10 && this.opCode != 8) {
                throw new WsIOException(new CloseReason((CloseReason.CloseCode)CloseReason.CloseCodes.PROTOCOL_ERROR, WsFrameBase.sm.getString("wsFrame.invalidOpCode", new Object[] { this.opCode })));
            }
        }
        else {
            if (this.continuationExpected) {
                if (!Util.isContinuation(this.opCode)) {
                    throw new WsIOException(new CloseReason((CloseReason.CloseCode)CloseReason.CloseCodes.PROTOCOL_ERROR, WsFrameBase.sm.getString("wsFrame.noContinuation")));
                }
            }
            else {
                try {
                    if (this.opCode == 2) {
                        this.textMessage = false;
                        final int size = this.wsSession.getMaxBinaryMessageBufferSize();
                        if (size != this.messageBufferBinary.capacity()) {
                            this.messageBufferBinary = ByteBuffer.allocate(size);
                        }
                        this.binaryMsgHandler = this.wsSession.getBinaryMessageHandler();
                        this.textMsgHandler = null;
                    }
                    else {
                        if (this.opCode != 1) {
                            throw new WsIOException(new CloseReason((CloseReason.CloseCode)CloseReason.CloseCodes.PROTOCOL_ERROR, WsFrameBase.sm.getString("wsFrame.invalidOpCode", new Object[] { this.opCode })));
                        }
                        this.textMessage = true;
                        final int size = this.wsSession.getMaxTextMessageBufferSize();
                        if (size != this.messageBufferText.capacity()) {
                            this.messageBufferText = CharBuffer.allocate(size);
                        }
                        this.binaryMsgHandler = null;
                        this.textMsgHandler = this.wsSession.getTextMessageHandler();
                    }
                }
                catch (IllegalStateException ise) {
                    throw new WsIOException(new CloseReason((CloseReason.CloseCode)CloseReason.CloseCodes.PROTOCOL_ERROR, WsFrameBase.sm.getString("wsFrame.sessionClosed")));
                }
            }
            this.continuationExpected = !this.fin;
        }
        b = this.inputBuffer.get();
        if ((b & 0x80) == 0x0 && this.isMasked()) {
            throw new WsIOException(new CloseReason((CloseReason.CloseCode)CloseReason.CloseCodes.PROTOCOL_ERROR, WsFrameBase.sm.getString("wsFrame.notMasked")));
        }
        this.payloadLength = (b & 0x7F);
        this.state = State.PARTIAL_HEADER;
        if (this.getLog().isDebugEnabled()) {
            this.getLog().debug((Object)WsFrameBase.sm.getString("wsFrame.partialHeaderComplete", new Object[] { Boolean.toString(this.fin), Integer.toString(this.rsv), Integer.toString(this.opCode), Long.toString(this.payloadLength) }));
        }
        return true;
    }
    
    protected abstract boolean isMasked();
    
    protected abstract Log getLog();
    
    private boolean processRemainingHeader() throws IOException {
        int headerLength;
        if (this.isMasked()) {
            headerLength = 4;
        }
        else {
            headerLength = 0;
        }
        if (this.payloadLength == 126L) {
            headerLength += 2;
        }
        else if (this.payloadLength == 127L) {
            headerLength += 8;
        }
        if (this.inputBuffer.remaining() < headerLength) {
            return false;
        }
        if (this.payloadLength == 126L) {
            this.payloadLength = byteArrayToLong(this.inputBuffer.array(), this.inputBuffer.arrayOffset() + this.inputBuffer.position(), 2);
            this.inputBuffer.position(this.inputBuffer.position() + 2);
        }
        else if (this.payloadLength == 127L) {
            this.payloadLength = byteArrayToLong(this.inputBuffer.array(), this.inputBuffer.arrayOffset() + this.inputBuffer.position(), 8);
            this.inputBuffer.position(this.inputBuffer.position() + 8);
        }
        if (Util.isControl(this.opCode)) {
            if (this.payloadLength > 125L) {
                throw new WsIOException(new CloseReason((CloseReason.CloseCode)CloseReason.CloseCodes.PROTOCOL_ERROR, WsFrameBase.sm.getString("wsFrame.controlPayloadTooBig", new Object[] { this.payloadLength })));
            }
            if (!this.fin) {
                throw new WsIOException(new CloseReason((CloseReason.CloseCode)CloseReason.CloseCodes.PROTOCOL_ERROR, WsFrameBase.sm.getString("wsFrame.controlNoFin")));
            }
        }
        if (this.isMasked()) {
            this.inputBuffer.get(this.mask, 0, 4);
        }
        this.state = State.DATA;
        return true;
    }
    
    private boolean processData() throws IOException {
        boolean result;
        if (Util.isControl(this.opCode)) {
            result = this.processDataControl();
        }
        else if (this.textMessage) {
            if (this.textMsgHandler == null) {
                result = this.swallowInput();
            }
            else {
                result = this.processDataText();
            }
        }
        else if (this.binaryMsgHandler == null) {
            result = this.swallowInput();
        }
        else {
            result = this.processDataBinary();
        }
        this.checkRoomPayload();
        return result;
    }
    
    private boolean processDataControl() throws IOException {
        final TransformationResult tr = this.transformation.getMoreData(this.opCode, this.fin, this.rsv, this.controlBufferBinary);
        if (TransformationResult.UNDERFLOW.equals(tr)) {
            return false;
        }
        this.controlBufferBinary.flip();
        if (this.opCode == 8) {
            this.open = false;
            String reason = null;
            int code = CloseReason.CloseCodes.NORMAL_CLOSURE.getCode();
            if (this.controlBufferBinary.remaining() == 1) {
                this.controlBufferBinary.clear();
                throw new WsIOException(new CloseReason((CloseReason.CloseCode)CloseReason.CloseCodes.PROTOCOL_ERROR, WsFrameBase.sm.getString("wsFrame.oneByteCloseCode")));
            }
            if (this.controlBufferBinary.remaining() > 1) {
                code = this.controlBufferBinary.getShort();
                if (this.controlBufferBinary.remaining() > 0) {
                    final CoderResult cr = this.utf8DecoderControl.decode(this.controlBufferBinary, this.controlBufferText, true);
                    if (cr.isError()) {
                        this.controlBufferBinary.clear();
                        this.controlBufferText.clear();
                        throw new WsIOException(new CloseReason((CloseReason.CloseCode)CloseReason.CloseCodes.PROTOCOL_ERROR, WsFrameBase.sm.getString("wsFrame.invalidUtf8Close")));
                    }
                    this.controlBufferText.flip();
                    reason = this.controlBufferText.toString();
                }
            }
            this.wsSession.onClose(new CloseReason(Util.getCloseCode(code), reason));
        }
        else if (this.opCode == 9) {
            if (this.wsSession.isOpen()) {
                this.wsSession.getBasicRemote().sendPong(this.controlBufferBinary);
            }
        }
        else {
            if (this.opCode != 10) {
                this.controlBufferBinary.clear();
                throw new WsIOException(new CloseReason((CloseReason.CloseCode)CloseReason.CloseCodes.PROTOCOL_ERROR, WsFrameBase.sm.getString("wsFrame.invalidOpCode", new Object[] { this.opCode })));
            }
            final MessageHandler.Whole<PongMessage> mhPong = this.wsSession.getPongMessageHandler();
            if (mhPong != null) {
                try {
                    mhPong.onMessage((Object)new WsPongMessage(this.controlBufferBinary));
                }
                catch (Throwable t) {
                    this.handleThrowableOnSend(t);
                }
                finally {
                    this.controlBufferBinary.clear();
                }
            }
        }
        this.controlBufferBinary.clear();
        this.newFrame();
        return true;
    }
    
    protected void sendMessageText(final boolean last) throws WsIOException {
        if (this.textMsgHandler instanceof WrappedMessageHandler) {
            final long maxMessageSize = ((WrappedMessageHandler)this.textMsgHandler).getMaxMessageSize();
            if (maxMessageSize > -1L && this.messageBufferText.remaining() > maxMessageSize) {
                throw new WsIOException(new CloseReason((CloseReason.CloseCode)CloseReason.CloseCodes.TOO_BIG, WsFrameBase.sm.getString("wsFrame.messageTooBig", new Object[] { this.messageBufferText.remaining(), maxMessageSize })));
            }
        }
        try {
            if (this.textMsgHandler instanceof MessageHandler.Partial) {
                ((MessageHandler.Partial)this.textMsgHandler).onMessage((Object)this.messageBufferText.toString(), last);
            }
            else {
                ((MessageHandler.Whole)this.textMsgHandler).onMessage((Object)this.messageBufferText.toString());
            }
        }
        catch (Throwable t) {
            this.handleThrowableOnSend(t);
        }
        finally {
            this.messageBufferText.clear();
        }
    }
    
    private boolean processDataText() throws IOException {
        for (TransformationResult tr = this.transformation.getMoreData(this.opCode, this.fin, this.rsv, this.messageBufferBinary); !TransformationResult.END_OF_FRAME.equals(tr); tr = this.transformation.getMoreData(this.opCode, this.fin, this.rsv, this.messageBufferBinary)) {
            this.messageBufferBinary.flip();
            while (true) {
                final CoderResult cr = this.utf8DecoderMessage.decode(this.messageBufferBinary, this.messageBufferText, false);
                if (cr.isError()) {
                    throw new WsIOException(new CloseReason((CloseReason.CloseCode)CloseReason.CloseCodes.NOT_CONSISTENT, WsFrameBase.sm.getString("wsFrame.invalidUtf8")));
                }
                if (cr.isOverflow()) {
                    if (!this.usePartial()) {
                        throw new WsIOException(new CloseReason((CloseReason.CloseCode)CloseReason.CloseCodes.TOO_BIG, WsFrameBase.sm.getString("wsFrame.textMessageTooBig")));
                    }
                    this.messageBufferText.flip();
                    this.sendMessageText(false);
                    this.messageBufferText.clear();
                }
                else {
                    if (!cr.isUnderflow()) {
                        continue;
                    }
                    this.messageBufferBinary.compact();
                    if (TransformationResult.OVERFLOW.equals(tr)) {
                        break;
                    }
                    return false;
                }
            }
        }
        this.messageBufferBinary.flip();
        boolean last = false;
        while (true) {
            final CoderResult cr2 = this.utf8DecoderMessage.decode(this.messageBufferBinary, this.messageBufferText, last);
            if (cr2.isError()) {
                throw new WsIOException(new CloseReason((CloseReason.CloseCode)CloseReason.CloseCodes.NOT_CONSISTENT, WsFrameBase.sm.getString("wsFrame.invalidUtf8")));
            }
            if (cr2.isOverflow()) {
                if (!this.usePartial()) {
                    throw new WsIOException(new CloseReason((CloseReason.CloseCode)CloseReason.CloseCodes.TOO_BIG, WsFrameBase.sm.getString("wsFrame.textMessageTooBig")));
                }
                this.messageBufferText.flip();
                this.sendMessageText(false);
                this.messageBufferText.clear();
            }
            else {
                if (!cr2.isUnderflow() || last) {
                    this.messageBufferText.flip();
                    this.sendMessageText(true);
                    this.newMessage();
                    return true;
                }
                if (this.continuationExpected) {
                    if (this.usePartial()) {
                        this.messageBufferText.flip();
                        this.sendMessageText(false);
                        this.messageBufferText.clear();
                    }
                    this.messageBufferBinary.compact();
                    this.newFrame();
                    return true;
                }
                last = true;
            }
        }
    }
    
    private boolean processDataBinary() throws IOException {
        for (TransformationResult tr = this.transformation.getMoreData(this.opCode, this.fin, this.rsv, this.messageBufferBinary); !TransformationResult.END_OF_FRAME.equals(tr); tr = this.transformation.getMoreData(this.opCode, this.fin, this.rsv, this.messageBufferBinary)) {
            if (TransformationResult.UNDERFLOW.equals(tr)) {
                return false;
            }
            if (!this.usePartial()) {
                final CloseReason cr = new CloseReason((CloseReason.CloseCode)CloseReason.CloseCodes.TOO_BIG, WsFrameBase.sm.getString("wsFrame.bufferTooSmall", new Object[] { this.messageBufferBinary.capacity(), this.payloadLength }));
                throw new WsIOException(cr);
            }
            this.messageBufferBinary.flip();
            final ByteBuffer copy = ByteBuffer.allocate(this.messageBufferBinary.limit());
            copy.put(this.messageBufferBinary);
            copy.flip();
            this.sendMessageBinary(copy, false);
            this.messageBufferBinary.clear();
        }
        if (this.usePartial() || !this.continuationExpected) {
            this.messageBufferBinary.flip();
            final ByteBuffer copy = ByteBuffer.allocate(this.messageBufferBinary.limit());
            copy.put(this.messageBufferBinary);
            copy.flip();
            this.sendMessageBinary(copy, !this.continuationExpected);
            this.messageBufferBinary.clear();
        }
        if (this.continuationExpected) {
            this.newFrame();
        }
        else {
            this.newMessage();
        }
        return true;
    }
    
    private void handleThrowableOnSend(final Throwable t) throws WsIOException {
        ExceptionUtils.handleThrowable(t);
        this.wsSession.getLocal().onError((Session)this.wsSession, t);
        final CloseReason cr = new CloseReason((CloseReason.CloseCode)CloseReason.CloseCodes.CLOSED_ABNORMALLY, WsFrameBase.sm.getString("wsFrame.ioeTriggeredClose"));
        throw new WsIOException(cr);
    }
    
    protected void sendMessageBinary(final ByteBuffer msg, final boolean last) throws WsIOException {
        if (this.binaryMsgHandler instanceof WrappedMessageHandler) {
            final long maxMessageSize = ((WrappedMessageHandler)this.binaryMsgHandler).getMaxMessageSize();
            if (maxMessageSize > -1L && msg.remaining() > maxMessageSize) {
                throw new WsIOException(new CloseReason((CloseReason.CloseCode)CloseReason.CloseCodes.TOO_BIG, WsFrameBase.sm.getString("wsFrame.messageTooBig", new Object[] { msg.remaining(), maxMessageSize })));
            }
        }
        try {
            if (this.binaryMsgHandler instanceof MessageHandler.Partial) {
                ((MessageHandler.Partial)this.binaryMsgHandler).onMessage((Object)msg, last);
            }
            else {
                ((MessageHandler.Whole)this.binaryMsgHandler).onMessage((Object)msg);
            }
        }
        catch (Throwable t) {
            this.handleThrowableOnSend(t);
        }
    }
    
    private void newMessage() {
        this.messageBufferBinary.clear();
        this.messageBufferText.clear();
        this.utf8DecoderMessage.reset();
        this.continuationExpected = false;
        this.newFrame();
    }
    
    private void newFrame() {
        if (this.inputBuffer.remaining() == 0) {
            this.inputBuffer.position(0).limit(0);
        }
        this.maskIndex = 0;
        this.payloadWritten = 0L;
        this.state = State.NEW_FRAME;
        this.checkRoomHeaders();
    }
    
    private void checkRoomHeaders() {
        if (this.inputBuffer.capacity() - this.inputBuffer.position() < 131) {
            this.makeRoom();
        }
    }
    
    private void checkRoomPayload() {
        if (this.inputBuffer.capacity() - this.inputBuffer.position() - this.payloadLength + this.payloadWritten < 0L) {
            this.makeRoom();
        }
    }
    
    private void makeRoom() {
        this.inputBuffer.compact();
        this.inputBuffer.flip();
    }
    
    private boolean usePartial() {
        if (Util.isControl(this.opCode)) {
            return false;
        }
        if (this.textMessage) {
            return this.textMsgHandler instanceof MessageHandler.Partial;
        }
        return this.binaryMsgHandler instanceof MessageHandler.Partial;
    }
    
    private boolean swallowInput() {
        final long toSkip = Math.min(this.payloadLength - this.payloadWritten, this.inputBuffer.remaining());
        this.inputBuffer.position(this.inputBuffer.position() + (int)toSkip);
        this.payloadWritten += toSkip;
        if (this.payloadWritten == this.payloadLength) {
            if (this.continuationExpected) {
                this.newFrame();
            }
            else {
                this.newMessage();
            }
            return true;
        }
        return false;
    }
    
    protected static long byteArrayToLong(final byte[] b, final int start, final int len) throws IOException {
        if (len > 8) {
            throw new IOException(WsFrameBase.sm.getString("wsFrame.byteToLongFail", new Object[] { len }));
        }
        int shift = 0;
        long result = 0L;
        for (int i = start + len - 1; i >= start; --i) {
            result += (b[i] & 0xFF) << shift;
            shift += 8;
        }
        return result;
    }
    
    protected boolean isOpen() {
        return this.open;
    }
    
    protected Transformation getTransformation() {
        return this.transformation;
    }
    
    public void suspend() {
        while (true) {
            switch (this.readState) {
                case WAITING: {
                    if (!WsFrameBase.READ_STATE_UPDATER.compareAndSet(this, ReadState.WAITING, ReadState.SUSPENDING_WAIT)) {
                        continue;
                    }
                }
                case PROCESSING: {
                    if (!WsFrameBase.READ_STATE_UPDATER.compareAndSet(this, ReadState.PROCESSING, ReadState.SUSPENDING_PROCESS)) {
                        continue;
                    }
                }
                case SUSPENDING_WAIT: {
                    if (this.readState != ReadState.SUSPENDING_WAIT) {
                        continue;
                    }
                    if (this.getLog().isWarnEnabled()) {
                        this.getLog().warn((Object)WsFrameBase.sm.getString("wsFrame.suspendRequested"));
                    }
                }
                case SUSPENDING_PROCESS: {
                    if (this.readState != ReadState.SUSPENDING_PROCESS) {
                        continue;
                    }
                    if (this.getLog().isWarnEnabled()) {
                        this.getLog().warn((Object)WsFrameBase.sm.getString("wsFrame.suspendRequested"));
                    }
                }
                case SUSPENDED: {
                    if (this.readState != ReadState.SUSPENDED) {
                        continue;
                    }
                    if (this.getLog().isWarnEnabled()) {
                        this.getLog().warn((Object)WsFrameBase.sm.getString("wsFrame.alreadySuspended"));
                    }
                }
                case CLOSING: {}
                default: {
                    throw new IllegalStateException(WsFrameBase.sm.getString("wsFrame.illegalReadState", new Object[] { this.state }));
                }
            }
        }
    }
    
    public void resume() {
        while (true) {
            switch (this.readState) {
                case WAITING: {
                    if (this.readState != ReadState.WAITING) {
                        continue;
                    }
                    if (this.getLog().isWarnEnabled()) {
                        this.getLog().warn((Object)WsFrameBase.sm.getString("wsFrame.alreadyResumed"));
                    }
                }
                case PROCESSING: {
                    if (this.readState != ReadState.PROCESSING) {
                        continue;
                    }
                    if (this.getLog().isWarnEnabled()) {
                        this.getLog().warn((Object)WsFrameBase.sm.getString("wsFrame.alreadyResumed"));
                    }
                }
                case SUSPENDING_WAIT: {
                    if (!WsFrameBase.READ_STATE_UPDATER.compareAndSet(this, ReadState.SUSPENDING_WAIT, ReadState.WAITING)) {
                        continue;
                    }
                }
                case SUSPENDING_PROCESS: {
                    if (!WsFrameBase.READ_STATE_UPDATER.compareAndSet(this, ReadState.SUSPENDING_PROCESS, ReadState.PROCESSING)) {
                        continue;
                    }
                }
                case SUSPENDED: {
                    if (!WsFrameBase.READ_STATE_UPDATER.compareAndSet(this, ReadState.SUSPENDED, ReadState.WAITING)) {
                        continue;
                    }
                    this.resumeProcessing();
                }
                case CLOSING: {}
                default: {
                    throw new IllegalStateException(WsFrameBase.sm.getString("wsFrame.illegalReadState", new Object[] { this.state }));
                }
            }
        }
    }
    
    protected boolean isSuspended() {
        return this.readState.isSuspended();
    }
    
    protected ReadState getReadState() {
        return this.readState;
    }
    
    protected void changeReadState(final ReadState newState) {
        WsFrameBase.READ_STATE_UPDATER.set(this, newState);
    }
    
    protected boolean changeReadState(final ReadState oldState, final ReadState newState) {
        return WsFrameBase.READ_STATE_UPDATER.compareAndSet(this, oldState, newState);
    }
    
    protected abstract void resumeProcessing();
    
    static {
        sm = StringManager.getManager((Class)WsFrameBase.class);
        READ_STATE_UPDATER = AtomicReferenceFieldUpdater.newUpdater(WsFrameBase.class, ReadState.class, "readState");
    }
    
    private enum State
    {
        NEW_FRAME, 
        PARTIAL_HEADER, 
        DATA;
    }
    
    protected enum ReadState
    {
        WAITING(false), 
        PROCESSING(false), 
        SUSPENDING_WAIT(true), 
        SUSPENDING_PROCESS(true), 
        SUSPENDED(true), 
        CLOSING(false);
        
        private final boolean isSuspended;
        
        private ReadState(final boolean isSuspended) {
            this.isSuspended = isSuspended;
        }
        
        public boolean isSuspended() {
            return this.isSuspended;
        }
    }
    
    private abstract class TerminalTransformation implements Transformation
    {
        @Override
        public boolean validateRsvBits(final int i) {
            return true;
        }
        
        @Override
        public Extension getExtensionResponse() {
            return null;
        }
        
        @Override
        public void setNext(final Transformation t) {
        }
        
        @Override
        public boolean validateRsv(final int rsv, final byte opCode) {
            return rsv == 0;
        }
        
        @Override
        public void close() {
        }
    }
    
    private final class NoopTransformation extends TerminalTransformation
    {
        @Override
        public TransformationResult getMoreData(final byte opCode, final boolean fin, final int rsv, final ByteBuffer dest) {
            long toWrite = Math.min(WsFrameBase.this.payloadLength - WsFrameBase.this.payloadWritten, WsFrameBase.this.inputBuffer.remaining());
            toWrite = Math.min(toWrite, dest.remaining());
            final int orgLimit = WsFrameBase.this.inputBuffer.limit();
            WsFrameBase.this.inputBuffer.limit(WsFrameBase.this.inputBuffer.position() + (int)toWrite);
            dest.put(WsFrameBase.this.inputBuffer);
            WsFrameBase.this.inputBuffer.limit(orgLimit);
            WsFrameBase.this.payloadWritten += toWrite;
            if (WsFrameBase.this.payloadWritten == WsFrameBase.this.payloadLength) {
                return TransformationResult.END_OF_FRAME;
            }
            if (WsFrameBase.this.inputBuffer.remaining() == 0) {
                return TransformationResult.UNDERFLOW;
            }
            return TransformationResult.OVERFLOW;
        }
        
        @Override
        public List<MessagePart> sendMessagePart(final List<MessagePart> messageParts) {
            return messageParts;
        }
    }
    
    private final class UnmaskTransformation extends TerminalTransformation
    {
        @Override
        public TransformationResult getMoreData(final byte opCode, final boolean fin, final int rsv, final ByteBuffer dest) {
            while (WsFrameBase.this.payloadWritten < WsFrameBase.this.payloadLength && WsFrameBase.this.inputBuffer.remaining() > 0 && dest.hasRemaining()) {
                final byte b = (byte)((WsFrameBase.this.inputBuffer.get() ^ WsFrameBase.this.mask[WsFrameBase.this.maskIndex]) & 0xFF);
                WsFrameBase.this.maskIndex++;
                if (WsFrameBase.this.maskIndex == 4) {
                    WsFrameBase.this.maskIndex = 0;
                }
                WsFrameBase.this.payloadWritten++;
                dest.put(b);
            }
            if (WsFrameBase.this.payloadWritten == WsFrameBase.this.payloadLength) {
                return TransformationResult.END_OF_FRAME;
            }
            if (WsFrameBase.this.inputBuffer.remaining() == 0) {
                return TransformationResult.UNDERFLOW;
            }
            return TransformationResult.OVERFLOW;
        }
        
        @Override
        public List<MessagePart> sendMessagePart(final List<MessagePart> messageParts) {
            return messageParts;
        }
    }
}
