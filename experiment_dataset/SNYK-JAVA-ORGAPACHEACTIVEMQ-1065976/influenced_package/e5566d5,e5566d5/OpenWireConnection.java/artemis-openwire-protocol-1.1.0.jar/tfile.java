// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.activemq.artemis.core.protocol.openwire;

import org.apache.activemq.wireformat.WireFormat;
import org.apache.activemq.command.ConnectionId;
import org.apache.activemq.command.DataStructure;
import org.apache.activemq.command.DataArrayResponse;
import org.apache.activemq.command.MessagePull;
import org.apache.activemq.command.MessageDispatchNotification;
import org.apache.activemq.state.ProducerState;
import org.apache.activemq.command.ProducerAck;
import javax.jms.ResourceAllocationException;
import org.apache.activemq.command.Message;
import org.apache.activemq.command.FlushCommand;
import org.apache.activemq.command.ControlCommand;
import org.apache.activemq.command.ConsumerControl;
import org.apache.activemq.command.BrokerInfo;
import javax.jms.InvalidDestinationException;
import org.apache.activemq.artemis.api.core.ActiveMQNonExistentQueueException;
import org.apache.activemq.command.SessionId;
import org.apache.activemq.state.ConsumerState;
import org.apache.activemq.state.SessionState;
import org.apache.activemq.artemis.core.protocol.openwire.amq.AMQCompositeConsumerBrokerExchange;
import org.apache.activemq.artemis.core.protocol.openwire.amq.AMQSingleConsumerBrokerExchange;
import org.apache.activemq.artemis.core.protocol.openwire.amq.AMQConsumer;
import javax.jms.JMSSecurityException;
import org.apache.activemq.artemis.api.core.ActiveMQSecurityException;
import org.apache.activemq.command.ConnectionError;
import org.apache.activemq.transport.TransmitCallback;
import org.apache.activemq.command.MessageDispatch;
import org.apache.activemq.command.ConnectionControl;
import org.apache.activemq.util.ByteSequence;
import java.util.Iterator;
import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.ActiveMQBuffers;
import java.util.Collections;
import java.util.Collection;
import java.util.ArrayList;
import java.io.IOException;
import org.apache.activemq.artemis.core.server.ActiveMQServerLogger;
import org.apache.activemq.command.Response;
import org.apache.activemq.command.ExceptionResponse;
import org.apache.activemq.command.RemoveSubscriptionInfo;
import org.apache.activemq.command.ShutdownInfo;
import org.apache.activemq.command.DestinationInfo;
import org.apache.activemq.command.MessageAck;
import org.apache.activemq.command.ActiveMQMessage;
import org.apache.activemq.command.ProducerInfo;
import org.apache.activemq.command.SessionInfo;
import org.apache.activemq.command.RemoveInfo;
import org.apache.activemq.command.ConsumerInfo;
import org.apache.activemq.command.WireFormatInfo;
import org.apache.activemq.command.KeepAliveInfo;
import java.io.DataInput;
import org.apache.activemq.artemis.api.core.ActiveMQBuffer;
import org.apache.activemq.command.ConnectionInfo;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.activemq.artemis.utils.ConcurrentHashSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.activemq.artemis.core.protocol.openwire.amq.AMQSession;
import org.apache.activemq.command.TransactionInfo;
import org.apache.activemq.command.TransactionId;
import org.apache.activemq.command.ActiveMQDestination;
import java.util.Set;
import org.apache.activemq.state.ConnectionState;
import org.apache.activemq.artemis.core.protocol.openwire.amq.AMQProducerBrokerExchange;
import org.apache.activemq.command.ProducerId;
import org.apache.activemq.artemis.core.protocol.openwire.amq.AMQConsumerBrokerExchange;
import org.apache.activemq.command.ConsumerId;
import java.util.Map;
import org.apache.activemq.command.Command;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.activemq.artemis.core.protocol.openwire.amq.AMQConnectionContext;
import org.apache.activemq.openwire.OpenWireFormat;
import org.apache.activemq.artemis.spi.core.remoting.Acceptor;
import org.apache.activemq.artemis.core.remoting.CloseListener;
import org.apache.activemq.artemis.core.remoting.FailureListener;
import java.util.List;
import org.apache.activemq.artemis.spi.core.remoting.Connection;
import org.apache.activemq.artemis.core.security.SecurityAuth;
import org.apache.activemq.state.CommandVisitor;
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;

public class OpenWireConnection implements RemotingConnection, CommandVisitor, SecurityAuth
{
    private final OpenWireProtocolManager protocolManager;
    private final Connection transportConnection;
    private final long creationTime;
    private final List<FailureListener> failureListeners;
    private final List<CloseListener> closeListeners;
    private boolean destroyed;
    private final Object sendLock;
    private volatile boolean dataReceived;
    private final Acceptor acceptorUsed;
    private OpenWireFormat wireFormat;
    private AMQConnectionContext context;
    private boolean pendingStop;
    private Throwable stopError;
    private final AtomicBoolean stopping;
    private final ReentrantReadWriteLock serviceLock;
    protected final List<Command> dispatchQueue;
    private boolean inServiceException;
    private final AtomicBoolean asyncException;
    private final Map<ConsumerId, AMQConsumerBrokerExchange> consumerExchanges;
    private final Map<ProducerId, AMQProducerBrokerExchange> producerExchanges;
    private ConnectionState state;
    private final Set<ActiveMQDestination> tempQueues;
    private Map<TransactionId, TransactionInfo> txMap;
    private volatile AMQSession advisorySession;
    
    public OpenWireConnection(final Acceptor acceptorUsed, final Connection connection, final OpenWireProtocolManager openWireProtocolManager, final OpenWireFormat wf) {
        this.failureListeners = new CopyOnWriteArrayList<FailureListener>();
        this.closeListeners = new CopyOnWriteArrayList<CloseListener>();
        this.destroyed = false;
        this.sendLock = new Object();
        this.stopError = null;
        this.stopping = new AtomicBoolean(false);
        this.serviceLock = new ReentrantReadWriteLock();
        this.dispatchQueue = new LinkedList<Command>();
        this.asyncException = new AtomicBoolean(false);
        this.consumerExchanges = new HashMap<ConsumerId, AMQConsumerBrokerExchange>();
        this.producerExchanges = new HashMap<ProducerId, AMQProducerBrokerExchange>();
        this.tempQueues = (Set<ActiveMQDestination>)new ConcurrentHashSet();
        this.txMap = new ConcurrentHashMap<TransactionId, TransactionInfo>();
        this.protocolManager = openWireProtocolManager;
        this.transportConnection = connection;
        this.acceptorUsed = acceptorUsed;
        this.wireFormat = wf;
        this.creationTime = System.currentTimeMillis();
    }
    
    public String getUsername() {
        final ConnectionInfo info = this.getConnectionInfo();
        if (info == null) {
            return null;
        }
        return info.getUserName();
    }
    
    public String getPassword() {
        final ConnectionInfo info = this.getConnectionInfo();
        if (info == null) {
            return null;
        }
        return info.getPassword();
    }
    
    private ConnectionInfo getConnectionInfo() {
        if (this.state == null) {
            return null;
        }
        final ConnectionInfo info = this.state.getInfo();
        if (info == null) {
            return null;
        }
        return info;
    }
    
    public String getLocalAddress() {
        return this.transportConnection.getLocalAddress();
    }
    
    public void bufferReceived(final Object connectionID, final ActiveMQBuffer buffer) {
        try {
            this.dataReceived = true;
            final Command command = (Command)this.wireFormat.unmarshal((DataInput)buffer);
            final boolean responseRequired = command.isResponseRequired();
            final int commandId = command.getCommandId();
            if (command.getClass() == KeepAliveInfo.class) {
                final KeepAliveInfo info = (KeepAliveInfo)command;
                info.setResponseRequired(false);
                this.protocolManager.sendReply(this, (Command)info);
            }
            else if (command.getClass() == WireFormatInfo.class) {
                this.negotiate((WireFormatInfo)command);
            }
            else if (command.getClass() == ConnectionInfo.class || command.getClass() == ConsumerInfo.class || command.getClass() == RemoveInfo.class || command.getClass() == SessionInfo.class || command.getClass() == ProducerInfo.class || ActiveMQMessage.class.isAssignableFrom(command.getClass()) || command.getClass() == MessageAck.class || command.getClass() == TransactionInfo.class || command.getClass() == DestinationInfo.class || command.getClass() == ShutdownInfo.class || command.getClass() == RemoveSubscriptionInfo.class) {
                Response response = null;
                if (this.pendingStop) {
                    response = (Response)new ExceptionResponse(this.stopError);
                }
                else {
                    try {
                        response = command.visit((CommandVisitor)this);
                    }
                    catch (Exception e) {
                        if (responseRequired) {
                            response = (Response)new ExceptionResponse((Throwable)e);
                        }
                    }
                    if (response instanceof ExceptionResponse && !responseRequired) {
                        final Throwable cause = ((ExceptionResponse)response).getException();
                        this.serviceException(cause);
                        response = null;
                    }
                }
                if (responseRequired && response == null) {
                    response = new Response();
                }
                if (this.context != null && this.context.isDontSendReponse()) {
                    this.context.setDontSendReponse(false);
                    response = null;
                }
                if (response != null && !this.protocolManager.isStopping()) {
                    response.setCorrelationId(commandId);
                    this.dispatchSync((Command)response);
                }
            }
            else {
                this.protocolManager.handleCommand(this, command);
            }
        }
        catch (IOException e2) {
            ActiveMQServerLogger.LOGGER.error((Object)"error decoding", (Throwable)e2);
        }
        catch (Throwable t) {
            ActiveMQServerLogger.LOGGER.error((Object)"error decoding", t);
        }
    }
    
    private void negotiate(final WireFormatInfo command) throws IOException {
        this.wireFormat.renegotiateWireFormat(command);
        this.protocolManager.sendBrokerInfo(this);
    }
    
    public Object getID() {
        return this.transportConnection.getID();
    }
    
    public long getCreationTime() {
        return this.creationTime;
    }
    
    public String getRemoteAddress() {
        return this.transportConnection.getRemoteAddress();
    }
    
    public void addFailureListener(final FailureListener listener) {
        if (listener == null) {
            throw new IllegalStateException("FailureListener cannot be null");
        }
        this.failureListeners.add(listener);
    }
    
    public boolean removeFailureListener(final FailureListener listener) {
        if (listener == null) {
            throw new IllegalStateException("FailureListener cannot be null");
        }
        return this.failureListeners.remove(listener);
    }
    
    public void addCloseListener(final CloseListener listener) {
        if (listener == null) {
            throw new IllegalStateException("CloseListener cannot be null");
        }
        this.closeListeners.add(listener);
    }
    
    public boolean removeCloseListener(final CloseListener listener) {
        if (listener == null) {
            throw new IllegalStateException("CloseListener cannot be null");
        }
        return this.closeListeners.remove(listener);
    }
    
    public List<CloseListener> removeCloseListeners() {
        final List<CloseListener> ret = new ArrayList<CloseListener>(this.closeListeners);
        this.closeListeners.clear();
        return ret;
    }
    
    public void setCloseListeners(final List<CloseListener> listeners) {
        this.closeListeners.clear();
        this.closeListeners.addAll(listeners);
    }
    
    public List<FailureListener> getFailureListeners() {
        return Collections.emptyList();
    }
    
    public List<FailureListener> removeFailureListeners() {
        final List<FailureListener> ret = new ArrayList<FailureListener>(this.failureListeners);
        this.failureListeners.clear();
        return ret;
    }
    
    public void setFailureListeners(final List<FailureListener> listeners) {
        this.failureListeners.clear();
        this.failureListeners.addAll(listeners);
    }
    
    public ActiveMQBuffer createTransportBuffer(final int size) {
        return ActiveMQBuffers.dynamicBuffer(size);
    }
    
    public void fail(final ActiveMQException me) {
        if (me != null) {
            ActiveMQServerLogger.LOGGER.connectionFailureDetected(me.getMessage(), me.getType());
        }
        this.callFailureListeners(me);
        this.callClosingListeners();
        this.destroyed = true;
        this.transportConnection.close();
    }
    
    public void destroy() {
        this.destroyed = true;
        this.transportConnection.close();
        try {
            this.deleteTempQueues();
        }
        catch (Exception ex) {}
        synchronized (this.sendLock) {
            this.callClosingListeners();
        }
    }
    
    private void deleteTempQueues() throws Exception {
        for (final ActiveMQDestination q : this.tempQueues) {
            this.protocolManager.removeDestination(this, q);
        }
    }
    
    public Connection getTransportConnection() {
        return this.transportConnection;
    }
    
    public boolean isClient() {
        return false;
    }
    
    public boolean isDestroyed() {
        return this.destroyed;
    }
    
    public void disconnect(final boolean criticalError) {
        this.fail(null);
    }
    
    public boolean checkDataReceived() {
        final boolean res = this.dataReceived;
        this.dataReceived = false;
        return res;
    }
    
    public void flush() {
    }
    
    private void callFailureListeners(final ActiveMQException me) {
        final List<FailureListener> listenersClone = new ArrayList<FailureListener>(this.failureListeners);
        for (final FailureListener listener : listenersClone) {
            try {
                listener.connectionFailed(me, false);
            }
            catch (Throwable t) {
                ActiveMQServerLogger.LOGGER.errorCallingFailureListener(t);
            }
        }
    }
    
    private void callClosingListeners() {
        final List<CloseListener> listenersClone = new ArrayList<CloseListener>(this.closeListeners);
        for (final CloseListener listener : listenersClone) {
            try {
                listener.connectionClosed();
            }
            catch (Throwable t) {
                ActiveMQServerLogger.LOGGER.errorCallingFailureListener(t);
            }
        }
    }
    
    public void init() {
        final WireFormatInfo info = this.wireFormat.getPreferedWireFormatInfo();
        this.protocolManager.send(this, (Command)info);
    }
    
    public ConnectionState getState() {
        return this.state;
    }
    
    public void physicalSend(final Command command) throws IOException {
        try {
            final ByteSequence bytes = this.wireFormat.marshal((Object)command);
            final ActiveMQBuffer buffer = OpenWireUtil.toActiveMQBuffer(bytes);
            synchronized (this.sendLock) {
                this.getTransportConnection().write(buffer, false, false);
            }
        }
        catch (IOException e) {
            throw e;
        }
        catch (Throwable t) {
            ActiveMQServerLogger.LOGGER.error((Object)"error sending", t);
        }
    }
    
    public Response processAddConnection(final ConnectionInfo info) throws Exception {
        final WireFormatInfo wireFormatInfo = this.wireFormat.getPreferedWireFormatInfo();
        if (wireFormatInfo != null && wireFormatInfo.getVersion() <= 2) {
            info.setClientMaster(true);
        }
        this.state = new ConnectionState(info);
        this.context = new AMQConnectionContext();
        this.state.reset(info);
        final String clientId = info.getClientId();
        this.context.setBroker(this.protocolManager);
        this.context.setClientId(clientId);
        this.context.setClientMaster(info.isClientMaster());
        this.context.setConnection(this);
        this.context.setConnectionId(info.getConnectionId());
        this.context.setFaultTolerant(info.isFaultTolerant());
        this.context.setUserName(info.getUserName());
        this.context.setWireFormatInfo(wireFormatInfo);
        this.context.setReconnect(info.isFailoverReconnect());
        this.context.setConnectionState(this.state);
        if (info.getClientIp() == null) {
            info.setClientIp(this.getRemoteAddress());
        }
        try {
            this.protocolManager.addConnection(this.context, info);
        }
        catch (Exception e) {
            if (e instanceof SecurityException) {
                this.delayedStop(2000, "Failed with SecurityException: " + e.getLocalizedMessage(), e);
            }
            final Response resp = (Response)new ExceptionResponse((Throwable)e);
            return resp;
        }
        if (info.isManageable()) {
            final ConnectionControl command = new ConnectionControl();
            command.setFaultTolerant(this.protocolManager.isFaultTolerantConfiguration());
            if (info.isFailoverReconnect()) {
                command.setRebalanceConnection(false);
            }
            this.dispatchAsync((Command)command);
        }
        return null;
    }
    
    public void dispatchAsync(final Command message) {
        if (!this.stopping.get()) {
            this.dispatchSync(message);
        }
        else if (message.isMessageDispatch()) {
            final MessageDispatch md = (MessageDispatch)message;
            final TransmitCallback sub = md.getTransmitCallback();
            this.protocolManager.postProcessDispatch(md);
            if (sub != null) {
                sub.onFailure();
            }
        }
    }
    
    public void dispatchSync(final Command message) {
        try {
            this.processDispatch(message);
        }
        catch (IOException e) {
            this.serviceExceptionAsync(e);
        }
    }
    
    public void serviceExceptionAsync(final IOException e) {
        if (this.asyncException.compareAndSet(false, true)) {
            new Thread("Async Exception Handler") {
                @Override
                public void run() {
                    OpenWireConnection.this.serviceException(e);
                }
            }.start();
        }
    }
    
    public void serviceException(final Throwable e) {
        if (e instanceof IOException) {
            this.serviceTransportException((IOException)e);
        }
        else if (!this.stopping.get() && !this.inServiceException) {
            this.inServiceException = true;
            try {
                final ConnectionError ce = new ConnectionError();
                ce.setException(e);
                if (this.pendingStop) {
                    this.dispatchSync((Command)ce);
                }
                else {
                    this.dispatchAsync((Command)ce);
                }
            }
            finally {
                this.inServiceException = false;
            }
        }
    }
    
    public void serviceTransportException(final IOException e) {
    }
    
    protected void dispatch(final Command command) throws IOException {
        this.physicalSend(command);
    }
    
    protected void processDispatch(final Command command) throws IOException {
        MessageDispatch messageDispatch = (MessageDispatch)(command.isMessageDispatch() ? command : null);
        try {
            if (!this.stopping.get()) {
                if (messageDispatch != null) {
                    this.protocolManager.preProcessDispatch(messageDispatch);
                }
                this.dispatch(command);
            }
        }
        catch (IOException e) {
            if (messageDispatch != null) {
                final TransmitCallback sub = messageDispatch.getTransmitCallback();
                this.protocolManager.postProcessDispatch(messageDispatch);
                if (sub != null) {
                    sub.onFailure();
                }
                messageDispatch = null;
                throw e;
            }
        }
        finally {
            if (messageDispatch != null) {
                final TransmitCallback sub2 = messageDispatch.getTransmitCallback();
                this.protocolManager.postProcessDispatch(messageDispatch);
                if (sub2 != null) {
                    sub2.onSuccess();
                }
            }
        }
    }
    
    public void delayedStop(final int waitTime, final String reason, final Throwable cause) {
        if (waitTime > 0) {
            synchronized (this) {
                this.pendingStop = true;
                this.stopError = cause;
            }
        }
    }
    
    public void stopAsync() {
        synchronized (this) {
            this.pendingStop = true;
        }
        if (this.stopping.compareAndSet(false, true) && this.context != null) {
            this.context.getStopping().set(true);
        }
    }
    
    protected void doStop() throws Exception {
        try {
            this.getTransportConnection().close();
        }
        catch (Exception ex) {}
        synchronized (this.dispatchQueue) {
            for (final Command command : this.dispatchQueue) {
                if (command.isMessageDispatch()) {
                    final MessageDispatch md = (MessageDispatch)command;
                    final TransmitCallback sub = md.getTransmitCallback();
                    this.protocolManager.postProcessDispatch(md);
                    if (sub == null) {
                        continue;
                    }
                    sub.onFailure();
                }
            }
            this.dispatchQueue.clear();
        }
        if (!this.protocolManager.isStopped()) {
            this.context.getStopping().set(true);
            try {
                this.processRemoveConnection(this.state.getInfo().getConnectionId(), 0L);
            }
            catch (Throwable ignore) {
                ignore.printStackTrace();
            }
        }
    }
    
    public Response processAddConsumer(final ConsumerInfo info) {
        Response resp = null;
        try {
            this.protocolManager.addConsumer(this, info);
        }
        catch (Exception e) {
            if (e instanceof ActiveMQSecurityException) {
                resp = (Response)new ExceptionResponse((Throwable)new JMSSecurityException(e.getMessage()));
            }
            else {
                resp = (Response)new ExceptionResponse((Throwable)e);
            }
        }
        return resp;
    }
    
    public void addConsumerBrokerExchange(final ConsumerId id, final AMQSession amqSession, final Map<ActiveMQDestination, AMQConsumer> consumerMap) {
        AMQConsumerBrokerExchange result = this.consumerExchanges.get(id);
        if (result == null) {
            if (consumerMap.size() == 1) {
                result = new AMQSingleConsumerBrokerExchange(amqSession, consumerMap.values().iterator().next());
            }
            else {
                result = new AMQCompositeConsumerBrokerExchange(amqSession, consumerMap);
            }
            synchronized (this.consumerExchanges) {
                result.setConnectionContext(this.context);
                final SessionState ss = this.state.getSessionState(id.getParentId());
                if (ss != null) {
                    final ConsumerState cs = ss.getConsumerState(id);
                    if (cs != null) {
                        final ConsumerInfo info = cs.getInfo();
                        if (info != null && info.getDestination() != null && info.getDestination().isPattern()) {
                            result.setWildcard(true);
                        }
                    }
                }
                this.consumerExchanges.put(id, result);
            }
        }
    }
    
    public int getConsumerCount() {
        int result = 0;
        for (final SessionId sessionId : this.state.getSessionIds()) {
            final SessionState sessionState = this.state.getSessionState(sessionId);
            if (sessionState != null) {
                result += sessionState.getConsumerIds().size();
            }
        }
        return result;
    }
    
    public int getProducerCount() {
        int result = 0;
        for (final SessionId sessionId : this.state.getSessionIds()) {
            final SessionState sessionState = this.state.getSessionState(sessionId);
            if (sessionState != null) {
                result += sessionState.getProducerIds().size();
            }
        }
        return result;
    }
    
    public Response processAddDestination(final DestinationInfo dest) throws Exception {
        Response resp = null;
        try {
            this.protocolManager.addDestination(this, dest);
        }
        catch (Exception e) {
            if (e instanceof ActiveMQSecurityException) {
                resp = (Response)new ExceptionResponse((Throwable)new JMSSecurityException(e.getMessage()));
            }
            else {
                resp = (Response)new ExceptionResponse((Throwable)e);
            }
        }
        return resp;
    }
    
    public Response processAddProducer(final ProducerInfo info) throws Exception {
        Response resp = null;
        try {
            this.protocolManager.addProducer(this, info);
        }
        catch (Exception e) {
            if (e instanceof ActiveMQSecurityException) {
                resp = (Response)new ExceptionResponse((Throwable)new JMSSecurityException(e.getMessage()));
            }
            else if (e instanceof ActiveMQNonExistentQueueException) {
                resp = (Response)new ExceptionResponse((Throwable)new InvalidDestinationException(e.getMessage()));
            }
            else {
                resp = (Response)new ExceptionResponse((Throwable)e);
            }
        }
        return resp;
    }
    
    public Response processAddSession(final SessionInfo info) throws Exception {
        if (!this.state.getSessionIds().contains(info.getSessionId())) {
            this.protocolManager.addSession(this, info);
            try {
                this.state.addSession(info);
            }
            catch (IllegalStateException e) {
                e.printStackTrace();
                this.protocolManager.removeSession(this.context, info);
            }
        }
        return null;
    }
    
    public Response processBeginTransaction(final TransactionInfo info) throws Exception {
        final TransactionId txId = info.getTransactionId();
        if (!this.txMap.containsKey(txId)) {
            this.txMap.put(txId, info);
        }
        return null;
    }
    
    public Response processBrokerInfo(final BrokerInfo arg0) throws Exception {
        throw new IllegalStateException("not implemented! ");
    }
    
    public Response processCommitTransactionOnePhase(final TransactionInfo info) throws Exception {
        this.protocolManager.commitTransactionOnePhase(info);
        final TransactionId txId = info.getTransactionId();
        this.txMap.remove(txId);
        return null;
    }
    
    public Response processCommitTransactionTwoPhase(final TransactionInfo info) throws Exception {
        this.protocolManager.commitTransactionTwoPhase(info);
        final TransactionId txId = info.getTransactionId();
        this.txMap.remove(txId);
        return null;
    }
    
    public Response processConnectionControl(final ConnectionControl arg0) throws Exception {
        throw new IllegalStateException("not implemented! ");
    }
    
    public Response processConnectionError(final ConnectionError arg0) throws Exception {
        throw new IllegalStateException("not implemented! ");
    }
    
    public Response processConsumerControl(final ConsumerControl arg0) throws Exception {
        throw new IllegalStateException("not implemented! ");
    }
    
    public Response processControlCommand(final ControlCommand arg0) throws Exception {
        throw new IllegalStateException("not implemented! ");
    }
    
    public Response processEndTransaction(final TransactionInfo info) throws Exception {
        this.protocolManager.endTransaction(info);
        final TransactionId txId = info.getTransactionId();
        if (!this.txMap.containsKey(txId)) {
            this.txMap.put(txId, info);
        }
        return null;
    }
    
    public Response processFlush(final FlushCommand arg0) throws Exception {
        throw new IllegalStateException("not implemented! ");
    }
    
    public Response processForgetTransaction(final TransactionInfo info) throws Exception {
        final TransactionId txId = info.getTransactionId();
        this.txMap.remove(txId);
        this.protocolManager.forgetTransaction(info.getTransactionId());
        return null;
    }
    
    public Response processKeepAlive(final KeepAliveInfo arg0) throws Exception {
        throw new IllegalStateException("not implemented! ");
    }
    
    public Response processMessage(final Message messageSend) {
        Response resp = null;
        try {
            final ProducerId producerId = messageSend.getProducerId();
            final AMQProducerBrokerExchange producerExchange = this.getProducerBrokerExchange(producerId);
            final AMQConnectionContext pcontext = producerExchange.getConnectionContext();
            final ProducerInfo producerInfo = producerExchange.getProducerState().getInfo();
            final boolean sendProducerAck = !messageSend.isResponseRequired() && producerInfo.getWindowSize() > 0 && !pcontext.isInRecoveryMode();
            final AMQSession session = this.protocolManager.getSession(producerId.getParentId());
            if (producerExchange.canDispatch(messageSend)) {
                final SendingResult result = session.send(producerExchange, messageSend, sendProducerAck);
                if (result.isBlockNextSend()) {
                    if (!this.context.isNetworkConnection() && result.isSendFailIfNoSpace()) {
                        throw new ResourceAllocationException("Usage Manager Memory Limit reached. Stopping producer (" + producerId + ") to prevent flooding " + result.getBlockingAddress() + "." + " See http://activemq.apache.org/producer-flow-control.html for more info");
                    }
                    if (producerInfo.getWindowSize() > 0 || messageSend.isResponseRequired()) {
                        this.context.setDontSendReponse(true);
                    }
                    else {
                        session.blockingWaitForSpace(producerExchange, result);
                    }
                }
                else if (sendProducerAck) {
                    final ProducerAck ack = new ProducerAck(producerInfo.getProducerId(), messageSend.getSize());
                    this.dispatchAsync((Command)ack);
                }
            }
        }
        catch (Throwable e) {
            if (e instanceof ActiveMQSecurityException) {
                resp = (Response)new ExceptionResponse((Throwable)new JMSSecurityException(e.getMessage()));
            }
            else {
                resp = (Response)new ExceptionResponse(e);
            }
        }
        return resp;
    }
    
    private AMQProducerBrokerExchange getProducerBrokerExchange(final ProducerId id) throws IOException {
        AMQProducerBrokerExchange result = this.producerExchanges.get(id);
        if (result == null) {
            synchronized (this.producerExchanges) {
                result = new AMQProducerBrokerExchange();
                result.setConnectionContext(this.context);
                if (this.context.isReconnect() || this.context.isNetworkConnection()) {
                    result.setLastStoredSequenceId(0L);
                }
                final SessionState ss = this.state.getSessionState(id.getParentId());
                if (ss != null) {
                    result.setProducerState(ss.getProducerState(id));
                    final ProducerState producerState = ss.getProducerState(id);
                    if (producerState != null && producerState.getInfo() != null) {
                        final ProducerInfo info = producerState.getInfo();
                        result.setMutable(info.getDestination() == null || info.getDestination().isComposite());
                    }
                }
                this.producerExchanges.put(id, result);
            }
        }
        return result;
    }
    
    public Response processMessageAck(final MessageAck ack) throws Exception {
        final AMQConsumerBrokerExchange consumerBrokerExchange = this.consumerExchanges.get(ack.getConsumerId());
        consumerBrokerExchange.acknowledge(ack);
        return null;
    }
    
    public Response processMessageDispatch(final MessageDispatch arg0) throws Exception {
        throw new IllegalStateException("not implemented! ");
    }
    
    public Response processMessageDispatchNotification(final MessageDispatchNotification arg0) throws Exception {
        throw new IllegalStateException("not implemented! ");
    }
    
    public Response processMessagePull(final MessagePull arg0) throws Exception {
        final AMQConsumerBrokerExchange amqConsumerBrokerExchange = this.consumerExchanges.get(arg0.getConsumerId());
        if (amqConsumerBrokerExchange == null) {
            throw new IllegalStateException("Consumer does not exist");
        }
        amqConsumerBrokerExchange.processMessagePull(arg0);
        return null;
    }
    
    public Response processPrepareTransaction(final TransactionInfo info) throws Exception {
        this.protocolManager.prepareTransaction(info);
        return null;
    }
    
    public Response processProducerAck(final ProducerAck arg0) throws Exception {
        throw new IllegalStateException("not implemented! ");
    }
    
    public Response processRecoverTransactions(final TransactionInfo info) throws Exception {
        final Set<SessionId> sIds = (Set<SessionId>)this.state.getSessionIds();
        final TransactionId[] recovered = this.protocolManager.recoverTransactions(sIds);
        return (Response)new DataArrayResponse((DataStructure[])recovered);
    }
    
    public Response processRemoveConnection(final ConnectionId id, final long lastDeliveredSequenceId) throws Exception {
        this.state.shutdown();
        for (final SessionId sessionId : this.state.getSessionIds()) {
            try {
                this.processRemoveSession(sessionId, lastDeliveredSequenceId);
            }
            catch (Throwable t) {}
        }
        try {
            this.protocolManager.removeConnection(this.context, this.state.getInfo(), null);
        }
        catch (Throwable t2) {}
        return null;
    }
    
    public Response processRemoveConsumer(final ConsumerId id, final long lastDeliveredSequenceId) throws Exception {
        final SessionId sessionId = id.getParentId();
        final SessionState ss = this.state.getSessionState(sessionId);
        if (ss == null) {
            throw new IllegalStateException("Cannot remove a consumer from a session that had not been registered: " + sessionId);
        }
        final ConsumerState consumerState = ss.removeConsumer(id);
        if (consumerState == null) {
            throw new IllegalStateException("Cannot remove a consumer that had not been registered: " + id);
        }
        final ConsumerInfo info = consumerState.getInfo();
        info.setLastDeliveredSequenceId(lastDeliveredSequenceId);
        final AMQConsumerBrokerExchange consumerBrokerExchange = this.consumerExchanges.get(id);
        consumerBrokerExchange.removeConsumer();
        this.removeConsumerBrokerExchange(id);
        return null;
    }
    
    private void removeConsumerBrokerExchange(final ConsumerId id) {
        synchronized (this.consumerExchanges) {
            this.consumerExchanges.remove(id);
        }
    }
    
    public Response processRemoveDestination(final DestinationInfo info) throws Exception {
        final ActiveMQDestination dest = info.getDestination();
        this.protocolManager.removeDestination(this, dest);
        return null;
    }
    
    public Response processRemoveProducer(final ProducerId id) throws Exception {
        this.protocolManager.removeProducer(id);
        return null;
    }
    
    public Response processRemoveSession(final SessionId id, final long lastDeliveredSequenceId) throws Exception {
        final SessionState session = this.state.getSessionState(id);
        if (session == null) {
            throw new IllegalStateException("Cannot remove session that had not been registered: " + id);
        }
        session.shutdown();
        for (final ConsumerId consumerId : session.getConsumerIds()) {
            try {
                this.processRemoveConsumer(consumerId, lastDeliveredSequenceId);
            }
            catch (Throwable t) {}
        }
        for (final ProducerId producerId : session.getProducerIds()) {
            try {
                this.processRemoveProducer(producerId);
            }
            catch (Throwable t2) {}
        }
        this.state.removeSession(id);
        this.protocolManager.removeSession(this.context, session.getInfo());
        return null;
    }
    
    public Response processRemoveSubscription(final RemoveSubscriptionInfo subInfo) throws Exception {
        this.protocolManager.removeSubscription(subInfo);
        return null;
    }
    
    public Response processRollbackTransaction(final TransactionInfo info) throws Exception {
        this.protocolManager.rollbackTransaction(info);
        final TransactionId txId = info.getTransactionId();
        this.txMap.remove(txId);
        return null;
    }
    
    public Response processShutdown(final ShutdownInfo info) throws Exception {
        return null;
    }
    
    public Response processWireFormat(final WireFormatInfo arg0) throws Exception {
        throw new IllegalStateException("not implemented! ");
    }
    
    public int getMaximumConsumersAllowedPerConnection() {
        return 1000000;
    }
    
    public int getMaximumProducersAllowedPerConnection() {
        return 1000000;
    }
    
    public void deliverMessage(final MessageDispatch dispatch) {
        final Message m = dispatch.getMessage();
        if (m != null) {
            final long endTime = System.currentTimeMillis();
            m.setBrokerOutTime(endTime);
        }
        this.protocolManager.send(this, (Command)dispatch);
    }
    
    public WireFormat getMarshaller() {
        return (WireFormat)this.wireFormat;
    }
    
    public void registerTempQueue(final ActiveMQDestination queue) {
        this.tempQueues.add(queue);
    }
    
    public void disconnect(final String reason, final boolean fail) {
        this.destroy();
    }
    
    public void fail(final ActiveMQException e, final String message) {
        this.destroy();
    }
    
    public void setAdvisorySession(final AMQSession amqSession) {
        this.advisorySession = amqSession;
    }
    
    public AMQSession getAdvisorySession() {
        return this.advisorySession;
    }
    
    public AMQConnectionContext getConext() {
        return this.context;
    }
}
