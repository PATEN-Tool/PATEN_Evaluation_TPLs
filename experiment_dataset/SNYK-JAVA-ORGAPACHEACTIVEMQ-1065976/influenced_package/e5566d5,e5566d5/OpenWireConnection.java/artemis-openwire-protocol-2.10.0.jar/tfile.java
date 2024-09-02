// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.activemq.artemis.core.protocol.openwire;

import org.apache.activemq.command.DataArrayResponse;
import org.apache.activemq.command.DataStructure;
import org.apache.activemq.command.XATransactionId;
import org.apache.activemq.command.ProducerAck;
import org.apache.activemq.command.MessagePull;
import org.apache.activemq.command.MessageDispatchNotification;
import org.apache.activemq.command.MessageAck;
import org.apache.activemq.command.FlushCommand;
import org.apache.activemq.command.ControlCommand;
import org.apache.activemq.command.BrokerInfo;
import org.apache.activemq.command.IntegerResponse;
import org.apache.activemq.artemis.core.transaction.TransactionOperation;
import org.apache.activemq.artemis.core.transaction.TransactionOperationAbstract;
import org.apache.activemq.command.ShutdownInfo;
import java.util.ListIterator;
import org.apache.activemq.artemis.core.server.MessageReference;
import org.apache.activemq.artemis.core.server.impl.RefsOperation;
import org.apache.activemq.artemis.core.transaction.ResourceManager;
import org.apache.activemq.command.TransactionInfo;
import org.apache.activemq.command.RemoveSubscriptionInfo;
import org.apache.activemq.command.ProducerInfo;
import org.apache.activemq.command.BrokerSubscriptionInfo;
import org.apache.activemq.command.ActiveMQMessage;
import org.apache.activemq.artemis.core.server.ServerConsumer;
import javax.transaction.xa.XAException;
import javax.transaction.xa.Xid;
import org.apache.activemq.artemis.core.protocol.openwire.util.OpenWireUtil;
import org.apache.activemq.state.ConsumerState;
import org.apache.activemq.artemis.core.server.BindingQueryResult;
import org.apache.activemq.artemis.core.server.ActiveMQMessageBundle;
import org.apache.activemq.artemis.core.postoffice.Bindings;
import org.apache.activemq.artemis.core.server.Queue;
import org.apache.activemq.artemis.core.postoffice.Binding;
import org.apache.activemq.command.SessionInfo;
import org.apache.activemq.command.ActiveMQTempQueue;
import org.apache.activemq.command.ConnectionId;
import org.apache.activemq.artemis.core.server.SlowConsumerDetectionListener;
import javax.jms.IllegalStateException;
import org.apache.activemq.command.ConsumerInfo;
import org.apache.activemq.command.ConsumerControl;
import org.apache.activemq.command.ActiveMQTopic;
import org.apache.activemq.command.ActiveMQDestination;
import org.apache.activemq.artemis.api.core.ActiveMQAddressExistsException;
import org.apache.activemq.artemis.api.core.ActiveMQQueueExistsException;
import org.apache.activemq.artemis.core.server.impl.AddressInfo;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.core.settings.impl.AddressSettings;
import org.apache.activemq.advisory.AdvisorySupport;
import org.apache.activemq.command.DestinationInfo;
import org.apache.activemq.artemis.spi.core.protocol.SessionCallback;
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;
import org.apache.activemq.artemis.utils.UUIDGenerator;
import org.apache.activemq.command.ConnectionControl;
import java.util.concurrent.TimeUnit;
import org.apache.activemq.artemis.api.core.ActiveMQExceptionType;
import javax.jms.InvalidClientIDException;
import org.apache.activemq.artemis.core.client.ActiveMQClientLogger;
import org.apache.activemq.artemis.api.core.ActiveMQRemoteDisconnectException;
import org.apache.activemq.command.Message;
import org.apache.activemq.state.ProducerState;
import org.apache.activemq.state.SessionState;
import org.apache.activemq.artemis.core.protocol.openwire.amq.AMQCompositeConsumerBrokerExchange;
import org.apache.activemq.artemis.core.protocol.openwire.amq.AMQSingleConsumerBrokerExchange;
import org.apache.activemq.artemis.core.protocol.openwire.amq.AMQConsumer;
import org.apache.activemq.transport.TransmitCallback;
import org.apache.activemq.command.MessageDispatch;
import org.apache.activemq.command.ConnectionError;
import org.apache.activemq.util.ByteSequence;
import org.apache.activemq.command.WireFormatInfo;
import java.util.Iterator;
import java.util.List;
import java.util.Collection;
import org.apache.activemq.artemis.core.remoting.FailureListener;
import java.util.ArrayList;
import org.apache.activemq.artemis.api.core.ActiveMQException;
import javax.jms.InvalidDestinationException;
import org.apache.activemq.artemis.api.core.ActiveMQNonExistentQueueException;
import javax.jms.JMSSecurityException;
import org.apache.activemq.artemis.api.core.ActiveMQSecurityException;
import java.io.IOException;
import org.apache.activemq.artemis.core.io.IOCallback;
import org.apache.activemq.command.Response;
import org.apache.activemq.command.ExceptionResponse;
import org.apache.activemq.artemis.core.server.ActiveMQServerLogger;
import org.apache.activemq.state.CommandVisitor;
import java.io.DataInput;
import org.apache.activemq.artemis.api.core.ActiveMQBuffer;
import org.apache.activemq.command.Command;
import org.apache.activemq.command.ConnectionInfo;
import org.apache.activemq.artemis.utils.collections.ConcurrentHashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import org.apache.activemq.artemis.spi.core.remoting.Connection;
import org.apache.activemq.artemis.api.core.SimpleString;
import java.util.Set;
import org.apache.activemq.artemis.spi.core.protocol.ConnectionEntry;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import org.apache.activemq.artemis.core.persistence.OperationContext;
import org.apache.activemq.artemis.core.server.ServerSession;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.transaction.Transaction;
import org.apache.activemq.command.TransactionId;
import org.apache.activemq.state.ConnectionState;
import org.apache.activemq.artemis.core.protocol.openwire.amq.AMQSession;
import org.apache.activemq.artemis.core.protocol.openwire.amq.AMQProducerBrokerExchange;
import org.apache.activemq.command.ProducerId;
import org.apache.activemq.artemis.core.protocol.openwire.amq.AMQConsumerBrokerExchange;
import org.apache.activemq.command.ConsumerId;
import org.apache.activemq.command.SessionId;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.activemq.artemis.core.protocol.openwire.amq.AMQConnectionContext;
import org.apache.activemq.openwire.OpenWireFormat;
import org.apache.activemq.command.KeepAliveInfo;
import org.jboss.logging.Logger;
import org.apache.activemq.artemis.core.server.TempQueueObserver;
import org.apache.activemq.artemis.core.security.SecurityAuth;
import org.apache.activemq.artemis.spi.core.protocol.AbstractRemotingConnection;

public class OpenWireConnection extends AbstractRemotingConnection implements SecurityAuth, TempQueueObserver
{
    private static final Logger logger;
    private static final KeepAliveInfo PING;
    private final OpenWireProtocolManager protocolManager;
    private boolean destroyed;
    private final OpenWireFormat inWireFormat;
    private final OpenWireFormat outWireFormat;
    private AMQConnectionContext context;
    private final AtomicBoolean stopping;
    private final Map<String, SessionId> sessionIdMap;
    private final Map<ConsumerId, AMQConsumerBrokerExchange> consumerExchanges;
    private final Map<ProducerId, AMQProducerBrokerExchange> producerExchanges;
    private final Map<SessionId, AMQSession> sessions;
    private ConnectionState state;
    private volatile boolean noLocal;
    private final Map<TransactionId, Transaction> txMap;
    private volatile AMQSession advisorySession;
    private final ActiveMQServer server;
    private ServerSession internalSession;
    private final OperationContext operationContext;
    private static final AtomicLongFieldUpdater<OpenWireConnection> LAST_SENT_UPDATER;
    private volatile long lastSent;
    private ConnectionEntry connectionEntry;
    private boolean useKeepAlive;
    private long maxInactivityDuration;
    private final Set<SimpleString> knownDestinations;
    private final AtomicBoolean disableTtl;
    CommandProcessor commandProcessorInstance;
    
    public OpenWireConnection(final Connection connection, final ActiveMQServer server, final OpenWireProtocolManager openWireProtocolManager, final OpenWireFormat wf, final Executor executor) {
        super(connection, executor);
        this.destroyed = false;
        this.stopping = new AtomicBoolean(false);
        this.sessionIdMap = new ConcurrentHashMap<String, SessionId>();
        this.consumerExchanges = new ConcurrentHashMap<ConsumerId, AMQConsumerBrokerExchange>();
        this.producerExchanges = new ConcurrentHashMap<ProducerId, AMQProducerBrokerExchange>();
        this.sessions = new ConcurrentHashMap<SessionId, AMQSession>();
        this.txMap = new ConcurrentHashMap<TransactionId, Transaction>();
        this.lastSent = -1L;
        this.knownDestinations = (Set<SimpleString>)new ConcurrentHashSet();
        this.disableTtl = new AtomicBoolean(false);
        this.commandProcessorInstance = new CommandProcessor();
        this.server = server;
        this.operationContext = server.newOperationContext();
        this.protocolManager = openWireProtocolManager;
        this.inWireFormat = wf;
        this.outWireFormat = wf.copy();
        this.useKeepAlive = openWireProtocolManager.isUseKeepAlive();
        this.maxInactivityDuration = openWireProtocolManager.getMaxInactivityDuration();
    }
    
    public String getUsername() {
        final ConnectionInfo info = this.getConnectionInfo();
        if (info == null) {
            return null;
        }
        return info.getUserName();
    }
    
    public OperationContext getOperationContext() {
        return this.operationContext;
    }
    
    public OpenWireConnection getRemotingConnection() {
        return this;
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
    
    private void bufferSent() {
        OpenWireConnection.LAST_SENT_UPDATER.lazySet(this, System.currentTimeMillis());
    }
    
    private static void traceBufferReceived(final Object connectionID, final Command command) {
        OpenWireConnection.logger.trace((Object)("connectionID: " + connectionID + " RECEIVED: " + ((command == null) ? "NULL" : command)));
    }
    
    public void bufferReceived(final Object connectionID, final ActiveMQBuffer buffer) {
        super.bufferReceived(connectionID, buffer);
        try {
            this.recoverOperationContext();
            final Command command = (Command)this.inWireFormat.unmarshal((DataInput)buffer);
            if (OpenWireConnection.logger.isTraceEnabled()) {
                traceBufferReceived(connectionID, command);
            }
            final boolean responseRequired = command.isResponseRequired();
            final int commandId = command.getCommandId();
            if (command.getClass() != KeepAliveInfo.class) {
                Response response = null;
                try {
                    this.setLastCommand(command);
                    response = command.visit((CommandVisitor)this.commandProcessorInstance);
                }
                catch (Exception e) {
                    ActiveMQServerLogger.LOGGER.warn((Object)"Errors occurred during the buffering operation ", (Throwable)e);
                    if (responseRequired) {
                        response = this.convertException(e);
                    }
                }
                finally {
                    this.setLastCommand(null);
                }
                if (response instanceof ExceptionResponse) {
                    final Throwable cause = ((ExceptionResponse)response).getException();
                    if (!responseRequired) {
                        this.serviceException(cause);
                        response = null;
                    }
                    if (command instanceof ConnectionInfo) {
                        this.delayedStop(2000, cause.getMessage(), cause);
                    }
                }
                if (responseRequired && response == null) {
                    response = new Response();
                    response.setCorrelationId(commandId);
                }
                if (this.context != null && this.context.isDontSendReponse()) {
                    this.context.setDontSendReponse(false);
                    response = null;
                }
                this.sendAsyncResponse(commandId, response);
            }
        }
        catch (Exception e2) {
            ActiveMQServerLogger.LOGGER.debug((Object)e2);
            this.sendException(e2);
        }
        finally {
            this.clearupOperationContext();
        }
    }
    
    private void sendAsyncResponse(final int commandId, final Response response) throws Exception {
        if (response != null) {
            this.operationContext.executeOnCompletion((IOCallback)new IOCallback() {
                public void done() {
                    if (!OpenWireConnection.this.protocolManager.isStopping()) {
                        try {
                            response.setCorrelationId(commandId);
                            OpenWireConnection.this.dispatchSync((Command)response);
                        }
                        catch (Exception e) {
                            OpenWireConnection.this.sendException(e);
                        }
                    }
                }
                
                public void onError(final int errorCode, final String errorMessage) {
                    OpenWireConnection.this.sendException(new IOException(errorCode + "-" + errorMessage));
                }
            });
        }
    }
    
    public void sendException(final Exception e) {
        final Response resp = this.convertException(e);
        if (this.context != null) {
            final Command command = this.context.getLastCommand();
            if (command != null) {
                resp.setCorrelationId(command.getCommandId());
            }
        }
        try {
            this.dispatch((Command)resp);
        }
        catch (IOException e2) {
            ActiveMQServerLogger.LOGGER.warn((Object)e.getMessage(), (Throwable)e2);
        }
    }
    
    private Response convertException(final Exception e) {
        Response resp;
        if (e instanceof ActiveMQSecurityException) {
            resp = (Response)new ExceptionResponse((Throwable)new JMSSecurityException(e.getMessage()));
        }
        else if (e instanceof ActiveMQNonExistentQueueException) {
            resp = (Response)new ExceptionResponse((Throwable)new InvalidDestinationException(e.getMessage()));
        }
        else {
            resp = (Response)new ExceptionResponse((Throwable)e);
        }
        return resp;
    }
    
    private void setLastCommand(final Command command) {
        if (this.context != null) {
            this.context.setLastCommand(command);
        }
    }
    
    public void destroy() {
        this.fail(null, null);
    }
    
    public boolean isClient() {
        return false;
    }
    
    public boolean isDestroyed() {
        return this.destroyed;
    }
    
    public void disconnect(final boolean criticalError) {
        this.disconnect(null, null, criticalError);
    }
    
    public void flush() {
        this.checkInactivity();
    }
    
    private void checkInactivity() {
        if (!this.useKeepAlive) {
            return;
        }
        final long dur = System.currentTimeMillis() - this.lastSent;
        if (dur >= this.maxInactivityDuration / 2L) {
            this.sendCommand((Command)OpenWireConnection.PING);
        }
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
    
    public void sendHandshake() {
        final WireFormatInfo info = this.inWireFormat.getPreferedWireFormatInfo();
        this.sendCommand((Command)info);
    }
    
    public ConnectionState getState() {
        return this.state;
    }
    
    private static void tracePhysicalSend(final Connection transportConnection, final Command command) {
        OpenWireConnection.logger.trace((Object)("connectionID: " + ((transportConnection == null) ? "" : transportConnection.getID()) + " SENDING: " + ((command == null) ? "NULL" : command)));
    }
    
    public void physicalSend(final Command command) throws IOException {
        if (OpenWireConnection.logger.isTraceEnabled()) {
            tracePhysicalSend(this.transportConnection, command);
        }
        try {
            final ByteSequence bytes = this.outWireFormat.marshal((Object)command);
            final int bufferSize = bytes.length;
            final ActiveMQBuffer buffer = this.transportConnection.createTransportBuffer(bufferSize);
            buffer.writeBytes(bytes.data, bytes.offset, bufferSize);
            this.transportConnection.write(buffer, false, false);
            this.bufferSent();
        }
        catch (IOException e) {
            throw e;
        }
        catch (Throwable t) {
            ActiveMQServerLogger.LOGGER.error((Object)"error sending", t);
        }
    }
    
    public void dispatchAsync(final Command message) throws Exception {
        this.dispatchSync(message);
    }
    
    public void dispatchSync(final Command message) throws Exception {
        this.processDispatch(message);
    }
    
    public void serviceException(final Throwable e) throws Exception {
        final ConnectionError ce = new ConnectionError();
        ce.setException(e);
        this.dispatchAsync((Command)ce);
    }
    
    public void dispatch(final Command command) throws IOException {
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
    
    private void addConsumerBrokerExchange(final ConsumerId id, final AMQSession amqSession, final List<AMQConsumer> consumerList) {
        AMQConsumerBrokerExchange result = this.consumerExchanges.get(id);
        if (result == null) {
            if (consumerList.size() == 1) {
                result = new AMQSingleConsumerBrokerExchange(amqSession, consumerList.get(0));
            }
            else {
                result = new AMQCompositeConsumerBrokerExchange(amqSession, consumerList);
            }
            synchronized (this.consumerExchanges) {
                this.consumerExchanges.put(id, result);
            }
        }
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
                        producerState.getInfo();
                    }
                }
                this.producerExchanges.put(id, result);
            }
        }
        return result;
    }
    
    public void deliverMessage(final MessageDispatch dispatch) {
        final Message m = dispatch.getMessage();
        if (m != null) {
            final long endTime = System.currentTimeMillis();
            m.setBrokerOutTime(endTime);
        }
        this.sendCommand((Command)dispatch);
    }
    
    public OpenWireFormat wireFormat() {
        return this.inWireFormat;
    }
    
    private void shutdown(final boolean fail) {
        if (fail) {
            this.transportConnection.forceClose();
        }
        else {
            this.transportConnection.close();
        }
    }
    
    private void disconnect(final ActiveMQException me, final String reason, final boolean fail) {
        if (this.context == null || this.destroyed) {
            return;
        }
        this.state.shutdown();
        try {
            for (final SessionId sessionId : this.sessionIdMap.values()) {
                final AMQSession session = this.sessions.get(sessionId);
                if (session != null) {
                    session.close();
                }
            }
            this.internalSession.close(false);
        }
        catch (Exception e) {
            ActiveMQServerLogger.LOGGER.warn((Object)e.getMessage(), (Throwable)e);
        }
        this.callFailureListeners(me);
        this.callClosingListeners();
        this.destroyed = true;
        final Command command = this.context.getLastCommand();
        if (command != null && command.isResponseRequired()) {
            final Response lastResponse = new Response();
            lastResponse.setCorrelationId(command.getCommandId());
            try {
                this.dispatchSync((Command)lastResponse);
            }
            catch (Throwable e2) {
                ActiveMQServerLogger.LOGGER.warn((Object)e2.getMessage(), e2);
            }
        }
    }
    
    public void disconnect(final String reason, final boolean fail) {
        this.disconnect(null, reason, fail);
    }
    
    public void fail(final ActiveMQException me, final String message) {
        if (me != null && !(me instanceof ActiveMQRemoteDisconnectException)) {
            ActiveMQClientLogger.LOGGER.connectionFailureDetected(this.transportConnection.getRemoteAddress(), me.getMessage(), me.getType());
        }
        try {
            if (this.getConnectionInfo() != null) {
                this.protocolManager.removeConnection(this.getConnectionInfo(), (Throwable)me);
            }
        }
        catch (InvalidClientIDException e) {
            ActiveMQServerLogger.LOGGER.warn((Object)"Couldn't close connection because invalid clientID", (Throwable)e);
        }
        this.shutdown(true);
    }
    
    private void delayedStop(final int waitTimeMillis, final String reason, final Throwable cause) {
        if (waitTimeMillis > 0) {
            try {
                this.protocolManager.getScheduledPool().schedule(() -> {
                    this.fail(new ActiveMQException(reason, cause, ActiveMQExceptionType.GENERIC_EXCEPTION), reason);
                    ActiveMQServerLogger.LOGGER.warn((Object)("Stopping " + this.transportConnection.getRemoteAddress() + "because " + reason));
                }, waitTimeMillis, TimeUnit.MILLISECONDS);
            }
            catch (Throwable t) {
                ActiveMQServerLogger.LOGGER.warn((Object)"Cannot stop connection. This exception will be ignored.", t);
            }
        }
    }
    
    public void setAdvisorySession(final AMQSession amqSession) {
        this.advisorySession = amqSession;
    }
    
    public AMQSession getAdvisorySession() {
        return this.advisorySession;
    }
    
    public AMQConnectionContext getContext() {
        return this.context;
    }
    
    public void updateClient(final ConnectionControl control) throws Exception {
        if (this.protocolManager.isUpdateClusterClients()) {
            this.dispatchAsync((Command)control);
        }
    }
    
    public AMQConnectionContext initContext(final ConnectionInfo info) throws Exception {
        final WireFormatInfo wireFormatInfo = this.inWireFormat.getPreferedWireFormatInfo();
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
        this.createInternalSession(info);
        return this.context;
    }
    
    private void createInternalSession(final ConnectionInfo info) throws Exception {
        this.internalSession = this.server.createSession(UUIDGenerator.getInstance().generateStringUUID(), this.context.getUserName(), info.getPassword(), -1, (RemotingConnection)this, true, false, false, false, (String)null, (SessionCallback)null, true, this.operationContext, (Map)this.protocolManager.getPrefixes());
    }
    
    public void reconnect(final AMQConnectionContext existingContext, final ConnectionInfo info) {
        this.context = existingContext;
        final WireFormatInfo wireFormatInfo = this.inWireFormat.getPreferedWireFormatInfo();
        if (wireFormatInfo != null && wireFormatInfo.getVersion() <= 2) {
            info.setClientMaster(true);
        }
        if (info.getClientIp() == null) {
            info.setClientIp(this.getRemoteAddress());
        }
        (this.state = new ConnectionState(info)).reset(info);
        this.context.setConnection(this);
        this.context.setConnectionState(this.state);
        this.context.setClientMaster(info.isClientMaster());
        this.context.setFaultTolerant(info.isFaultTolerant());
        this.context.setReconnect(true);
        this.context.incRefCount();
    }
    
    public boolean sendCommand(final Command command) {
        if (ActiveMQServerLogger.LOGGER.isTraceEnabled()) {
            ActiveMQServerLogger.LOGGER.trace((Object)("sending " + command));
        }
        if (this.isDestroyed()) {
            return false;
        }
        try {
            this.physicalSend(command);
        }
        catch (Exception e) {
            return false;
        }
        catch (Throwable t) {
            return false;
        }
        return true;
    }
    
    public void addDestination(final DestinationInfo info) throws Exception {
        boolean created = false;
        final ActiveMQDestination dest = info.getDestination();
        if (!this.protocolManager.isSupportAdvisory() && AdvisorySupport.isAdvisoryTopic(dest)) {
            return;
        }
        final SimpleString qName = SimpleString.toSimpleString(dest.getPhysicalName());
        Label_0266: {
            if (this.server.locateQueue(qName) == null) {
                final AddressSettings addressSettings = (AddressSettings)this.server.getAddressSettingsRepository().getMatch(dest.getPhysicalName());
                final AddressInfo addressInfo = new AddressInfo(qName, dest.isTopic() ? RoutingType.MULTICAST : RoutingType.ANYCAST);
                if (AdvisorySupport.isAdvisoryTopic(dest) && this.protocolManager.isSuppressInternalManagementObjects()) {
                    addressInfo.setInternal(true);
                }
                Label_0196: {
                    if (dest.isQueue()) {
                        if (!addressSettings.isAutoCreateQueues()) {
                            if (!dest.isTemporary()) {
                                break Label_0196;
                            }
                        }
                        try {
                            this.internalSession.createQueue(addressInfo, qName, (SimpleString)null, dest.isTemporary(), !dest.isTemporary(), !dest.isTemporary());
                            created = true;
                        }
                        catch (ActiveMQQueueExistsException ex) {}
                        break Label_0266;
                    }
                }
                if (dest.isTopic()) {
                    if (!addressSettings.isAutoCreateAddresses()) {
                        if (!dest.isTemporary()) {
                            break Label_0266;
                        }
                    }
                    try {
                        if (this.internalSession.getAddress(addressInfo.getName()) == null) {
                            this.internalSession.createAddress(addressInfo, !dest.isTemporary());
                            created = true;
                        }
                    }
                    catch (ActiveMQAddressExistsException ex2) {}
                }
            }
        }
        if (dest.isTemporary()) {
            this.state.addTempDestination(info);
        }
        if (created && !AdvisorySupport.isAdvisoryTopic(dest)) {
            final AMQConnectionContext context = this.getContext();
            final DestinationInfo advInfo = new DestinationInfo(context.getConnectionId(), (byte)0, dest);
            final ActiveMQTopic topic = AdvisorySupport.getDestinationAdvisoryTopic(dest);
            this.protocolManager.fireAdvisory(context, topic, (Command)advInfo);
        }
    }
    
    public void updateConsumer(final ConsumerControl consumerControl) {
        final ConsumerId consumerId = consumerControl.getConsumerId();
        final AMQConsumerBrokerExchange exchange = this.consumerExchanges.get(consumerId);
        if (exchange != null) {
            exchange.updateConsumerPrefetchSize(consumerControl.getPrefetch());
        }
    }
    
    public void addConsumer(final ConsumerInfo info) throws Exception {
        final SessionId sessionId = info.getConsumerId().getParentId();
        final ConnectionId connectionId = sessionId.getParentId();
        final ConnectionState cs = this.getState();
        if (cs == null) {
            throw new IllegalStateException("Cannot add a consumer to a connection that had not been registered: " + connectionId);
        }
        final SessionState ss = cs.getSessionState(sessionId);
        if (ss == null) {
            throw new IllegalStateException(this.server + " Cannot add a consumer to a session that had not been registered: " + sessionId);
        }
        if (!ss.getConsumerIds().contains(info.getConsumerId())) {
            final AMQSession amqSession = this.sessions.get(sessionId);
            if (amqSession == null) {
                throw new IllegalStateException("Session not exist! : " + sessionId);
            }
            final List<AMQConsumer> consumersList = amqSession.createConsumer(info, (SlowConsumerDetectionListener)new SlowConsumerDetection());
            this.addConsumerBrokerExchange(info.getConsumerId(), amqSession, consumersList);
            ss.addConsumer(info);
            info.setLastDeliveredSequenceId(-2L);
            if (consumersList.size() == 0) {
                return;
            }
            amqSession.start();
            if (AdvisorySupport.isAdvisoryTopic(info.getDestination()) && AdvisorySupport.isTempDestinationAdvisoryTopic(info.getDestination())) {
                final List<DestinationInfo> tmpDests = this.protocolManager.getTemporaryDestinations();
                for (final DestinationInfo di : tmpDests) {
                    final ActiveMQTopic topic = AdvisorySupport.getDestinationAdvisoryTopic(di.getDestination());
                    final String originalConnectionId = di.getConnectionId().getValue();
                    this.protocolManager.fireAdvisory(this.context, topic, (Command)di, info.getConsumerId(), originalConnectionId);
                }
            }
        }
    }
    
    public void setConnectionEntry(final ConnectionEntry connectionEntry) {
        this.connectionEntry = connectionEntry;
    }
    
    public boolean checkDataReceived() {
        return this.disableTtl.get() || super.checkDataReceived();
    }
    
    public void setUpTtl(final long inactivityDuration, final long inactivityDurationInitialDelay, final boolean useKeepAlive) {
        this.useKeepAlive = useKeepAlive;
        this.maxInactivityDuration = inactivityDuration;
        this.protocolManager.getScheduledPool().schedule(new Runnable() {
            @Override
            public void run() {
                if (inactivityDuration >= 0L) {
                    OpenWireConnection.this.connectionEntry.ttl = inactivityDuration;
                }
            }
        }, inactivityDurationInitialDelay, TimeUnit.MILLISECONDS);
        this.checkInactivity();
    }
    
    public void addKnownDestination(final SimpleString address) {
        this.knownDestinations.add(address);
    }
    
    public boolean containsKnownDestination(final SimpleString address) {
        return this.knownDestinations.contains(address);
    }
    
    public void tempQueueDeleted(final SimpleString bindingName) {
        final ActiveMQDestination dest = (ActiveMQDestination)new ActiveMQTempQueue(bindingName.toString());
        this.state.removeTempDestination(dest);
        if (!AdvisorySupport.isAdvisoryTopic(dest)) {
            final AMQConnectionContext context = this.getContext();
            final DestinationInfo advInfo = new DestinationInfo(context.getConnectionId(), (byte)1, dest);
            final ActiveMQTopic topic = AdvisorySupport.getDestinationAdvisoryTopic(dest);
            try {
                this.protocolManager.fireAdvisory(context, topic, (Command)advInfo);
            }
            catch (Exception e) {
                OpenWireConnection.logger.warn((Object)("Failed to fire advisory on " + topic), (Throwable)e);
            }
        }
    }
    
    public void disableTtl() {
        this.disableTtl.set(true);
    }
    
    public void enableTtl() {
        this.disableTtl.set(false);
    }
    
    public boolean isNoLocal() {
        return this.noLocal;
    }
    
    public void setNoLocal(final boolean noLocal) {
        this.noLocal = noLocal;
    }
    
    public List<DestinationInfo> getTemporaryDestinations() {
        return (List<DestinationInfo>)this.state.getTempDestinations();
    }
    
    public boolean isSuppressInternalManagementObjects() {
        return this.protocolManager.isSuppressInternalManagementObjects();
    }
    
    public boolean isSuppportAdvisory() {
        return this.protocolManager.isSupportAdvisory();
    }
    
    public void addSessions(final Set<SessionId> sessionSet) {
        for (final SessionId sid : sessionSet) {
            this.addSession(this.getState().getSessionState(sid).getInfo(), true);
        }
    }
    
    public AMQSession addSession(final SessionInfo ss) {
        return this.addSession(ss, false);
    }
    
    public AMQSession addSession(final SessionInfo ss, final boolean internal) {
        final AMQSession amqSession = new AMQSession(this.getState().getInfo(), ss, this.server, this, this.protocolManager);
        amqSession.initialize();
        if (internal) {
            amqSession.disableSecurity();
        }
        this.sessions.put(ss.getSessionId(), amqSession);
        this.sessionIdMap.put(amqSession.getCoreSession().getName(), ss.getSessionId());
        return amqSession;
    }
    
    public void removeSession(final AMQConnectionContext context, final SessionInfo info) throws Exception {
        final AMQSession session = this.sessions.remove(info.getSessionId());
        if (session != null) {
            this.sessionIdMap.remove(session.getCoreSession().getName());
            session.close();
        }
    }
    
    public AMQSession getSession(final SessionId sessionId) {
        return this.sessions.get(sessionId);
    }
    
    public void removeDestination(final ActiveMQDestination dest) throws Exception {
        if (dest.isQueue()) {
            try {
                this.server.destroyQueue(new SimpleString(dest.getPhysicalName()), (SecurityAuth)this.getRemotingConnection());
            }
            catch (ActiveMQNonExistentQueueException neq) {
                ActiveMQServerLogger.LOGGER.debug((Object)"queue never existed");
            }
        }
        else {
            final Bindings bindings = this.server.getPostOffice().lookupBindingsForAddress(new SimpleString(dest.getPhysicalName()));
            if (bindings != null) {
                for (final Binding binding : bindings.getBindings()) {
                    final Queue b = (Queue)binding.getBindable();
                    if (b.getConsumerCount() > 0) {
                        throw new Exception("Destination still has an active subscription: " + dest.getPhysicalName());
                    }
                    if (b.isDurable()) {
                        throw new Exception("Destination still has durable subscription: " + dest.getPhysicalName());
                    }
                    b.deleteQueue();
                }
            }
        }
        if (!AdvisorySupport.isAdvisoryTopic(dest)) {
            final AMQConnectionContext context = this.getContext();
            final DestinationInfo advInfo = new DestinationInfo(context.getConnectionId(), (byte)1, dest);
            final ActiveMQTopic topic = AdvisorySupport.getDestinationAdvisoryTopic(dest);
            this.protocolManager.fireAdvisory(context, topic, (Command)advInfo);
        }
    }
    
    private void validateDestination(final ActiveMQDestination destination) throws Exception {
        if (destination.isQueue()) {
            final SimpleString physicalName = new SimpleString(destination.getPhysicalName());
            final BindingQueryResult result = this.server.bindingQuery(physicalName);
            if (!result.isExists() && !result.isAutoCreateQueues()) {
                throw ActiveMQMessageBundle.BUNDLE.noSuchQueue(physicalName);
            }
        }
    }
    
    private void propagateLastSequenceId(final SessionState sessionState, final long lastDeliveredSequenceId) {
        for (final ConsumerState consumerState : sessionState.getConsumerStates()) {
            consumerState.getInfo().setLastDeliveredSequenceId(lastDeliveredSequenceId);
        }
    }
    
    private void recoverOperationContext() {
        this.server.getStorageManager().setContext(this.operationContext);
    }
    
    private void clearupOperationContext() {
        this.server.getStorageManager().clearContext();
    }
    
    private Transaction lookupTX(final TransactionId txID, final AMQSession session) throws IllegalStateException {
        return this.lookupTX(txID, session, false);
    }
    
    private Transaction lookupTX(final TransactionId txID, final AMQSession session, final boolean remove) throws IllegalStateException {
        if (txID == null) {
            return null;
        }
        Xid xid = null;
        Transaction transaction;
        if (txID.isXATransaction()) {
            xid = (Xid)OpenWireUtil.toXID(txID);
            transaction = (remove ? this.server.getResourceManager().removeTransaction(xid) : this.server.getResourceManager().getTransaction(xid));
        }
        else {
            transaction = (remove ? this.txMap.remove(txID) : this.txMap.get(txID));
        }
        if (transaction == null) {
            return null;
        }
        if (session != null && transaction.getProtocolData() != session) {
            transaction.setProtocolData((Object)session);
        }
        return transaction;
    }
    
    public static XAException newXAException(final String s, final int errorCode) {
        final XAException xaException = new XAException(s + " " + "xaErrorCode:" + errorCode);
        xaException.errorCode = errorCode;
        return xaException;
    }
    
    public void killMessage(final SimpleString nodeID) {
    }
    
    public String getProtocolName() {
        return "OPENWIRE";
    }
    
    public String getClientID() {
        return (this.context != null) ? this.context.getClientId() : null;
    }
    
    public String getTransportLocalAddress() {
        return this.transportConnection.getLocalAddress();
    }
    
    static {
        logger = Logger.getLogger((Class)OpenWireConnection.class);
        PING = new KeepAliveInfo();
        LAST_SENT_UPDATER = AtomicLongFieldUpdater.newUpdater(OpenWireConnection.class, "lastSent");
    }
    
    class SlowConsumerDetection implements SlowConsumerDetectionListener
    {
        public void onSlowConsumer(final ServerConsumer consumer) {
            if (consumer.getProtocolData() != null && consumer.getProtocolData() instanceof AMQConsumer) {
                final AMQConsumer amqConsumer = (AMQConsumer)consumer.getProtocolData();
                final ActiveMQTopic topic = AdvisorySupport.getSlowConsumerAdvisoryTopic(amqConsumer.getOpenwireDestination());
                final ActiveMQMessage advisoryMessage = new ActiveMQMessage();
                try {
                    advisoryMessage.setStringProperty("consumerId", amqConsumer.getId().toString());
                    OpenWireConnection.this.protocolManager.fireAdvisory(OpenWireConnection.this.context, topic, (Command)advisoryMessage, amqConsumer.getId(), null);
                }
                catch (Exception e) {
                    ActiveMQServerLogger.LOGGER.warn((Object)"Error during method invocation", (Throwable)e);
                }
            }
        }
    }
    
    public class CommandProcessor implements CommandVisitor
    {
        public AMQConnectionContext getContext() {
            return OpenWireConnection.this.getContext();
        }
        
        public Response processAddConnection(final ConnectionInfo info) throws Exception {
            try {
                OpenWireConnection.this.protocolManager.addConnection(OpenWireConnection.this, info);
            }
            catch (Exception e) {
                final Response resp = (Response)new ExceptionResponse((Throwable)e);
                return resp;
            }
            if (info.isManageable() && OpenWireConnection.this.protocolManager.isUpdateClusterClients()) {
                final ConnectionControl command = OpenWireConnection.this.protocolManager.newConnectionControl();
                command.setFaultTolerant(OpenWireConnection.this.protocolManager.isFaultTolerantConfiguration());
                if (info.isFailoverReconnect()) {
                    command.setRebalanceConnection(false);
                }
                OpenWireConnection.this.dispatchAsync((Command)command);
            }
            return null;
        }
        
        public Response processBrokerSubscriptionInfo(final BrokerSubscriptionInfo brokerSubscriptionInfo) throws Exception {
            return null;
        }
        
        public Response processAddProducer(final ProducerInfo info) throws Exception {
            final SessionId sessionId = info.getProducerId().getParentId();
            final ConnectionState cs = OpenWireConnection.this.getState();
            if (cs == null) {
                throw new IllegalStateException("Cannot add a producer to a connection that had not been registered: " + sessionId.getParentId());
            }
            final SessionState ss = cs.getSessionState(sessionId);
            if (ss == null) {
                throw new IllegalStateException("Cannot add a producer to a session that had not been registered: " + sessionId);
            }
            if (!ss.getProducerIds().contains(info.getProducerId())) {
                final ActiveMQDestination destination = info.getDestination();
                if (destination != null && !AdvisorySupport.isAdvisoryTopic(destination)) {
                    if (destination.isQueue()) {
                        OpenWireConnection.this.validateDestination(destination);
                    }
                    final DestinationInfo destInfo = new DestinationInfo(this.getContext().getConnectionId(), (byte)0, destination);
                    OpenWireConnection.this.addDestination(destInfo);
                }
                ss.addProducer(info);
            }
            return null;
        }
        
        public Response processAddConsumer(final ConsumerInfo info) throws Exception {
            OpenWireConnection.this.addConsumer(info);
            return null;
        }
        
        public Response processRemoveDestination(final DestinationInfo info) throws Exception {
            final ActiveMQDestination dest = info.getDestination();
            OpenWireConnection.this.removeDestination(dest);
            return null;
        }
        
        public Response processRemoveProducer(final ProducerId id) throws Exception {
            return null;
        }
        
        public Response processRemoveSession(final SessionId id, final long lastDeliveredSequenceId) throws Exception {
            final SessionState session = OpenWireConnection.this.state.getSessionState(id);
            if (session == null) {
                throw new IllegalStateException("Cannot remove session that had not been registered: " + id);
            }
            session.shutdown();
            for (final ProducerId producerId : session.getProducerIds()) {
                try {
                    this.processRemoveProducer(producerId);
                }
                catch (Throwable t) {}
            }
            OpenWireConnection.this.state.removeSession(id);
            OpenWireConnection.this.propagateLastSequenceId(session, lastDeliveredSequenceId);
            OpenWireConnection.this.removeSession(OpenWireConnection.this.context, session.getInfo());
            return null;
        }
        
        public Response processRemoveSubscription(final RemoveSubscriptionInfo subInfo) throws Exception {
            final SimpleString subQueueName = org.apache.activemq.artemis.jms.client.ActiveMQDestination.createQueueNameForSubscription(true, subInfo.getClientId(), subInfo.getSubscriptionName());
            OpenWireConnection.this.server.destroyQueue(subQueueName);
            return null;
        }
        
        public Response processRollbackTransaction(final TransactionInfo info) throws Exception {
            final Transaction tx = OpenWireConnection.this.lookupTX(info.getTransactionId(), null, true);
            AMQSession amqSession;
            if (tx != null) {
                amqSession = (AMQSession)tx.getProtocolData();
            }
            else {
                amqSession = null;
            }
            if (info.getTransactionId().isXATransaction() && tx == null) {
                throw OpenWireConnection.newXAException("Transaction '" + info.getTransactionId() + "' has not been started.", -4);
            }
            if (tx != null && amqSession != null) {
                amqSession.getCoreSession().resetTX(tx);
                try {
                    this.returnReferences(tx, amqSession);
                }
                finally {
                    amqSession.getCoreSession().resetTX((Transaction)null);
                }
            }
            if (info.getTransactionId().isXATransaction()) {
                final ResourceManager resourceManager = OpenWireConnection.this.server.getResourceManager();
                final Xid xid = (Xid)OpenWireUtil.toXID(info.getTransactionId());
                if (tx == null) {
                    if (resourceManager.getHeuristicCommittedTransactions().contains(xid)) {
                        final XAException ex = new XAException("transaction has been heuristically committed: " + xid);
                        ex.errorCode = 7;
                        throw ex;
                    }
                    if (resourceManager.getHeuristicRolledbackTransactions().contains(xid)) {
                        final XAException ex = new XAException("transaction has been heuristically rolled back: " + xid);
                        ex.errorCode = 6;
                        throw ex;
                    }
                    if (OpenWireConnection.logger.isTraceEnabled()) {
                        OpenWireConnection.logger.trace((Object)("xarollback into " + tx + ", xid=" + xid + " forcing a rollback regular"));
                    }
                    try {
                        if (amqSession != null) {
                            amqSession.getCoreSession().rollback(false);
                        }
                    }
                    catch (Exception e) {
                        ActiveMQServerLogger.LOGGER.warn((Object)e.getMessage(), (Throwable)e);
                    }
                    final XAException ex = new XAException("Cannot find xid in resource manager: " + xid);
                    ex.errorCode = -4;
                    throw ex;
                }
                else {
                    if (tx.getState() == Transaction.State.SUSPENDED) {
                        if (OpenWireConnection.logger.isTraceEnabled()) {
                            OpenWireConnection.logger.trace((Object)("xarollback into " + tx + " sending tx back as it was suspended"));
                        }
                        resourceManager.putTransaction(xid, tx);
                        final XAException ex = new XAException("Cannot commit transaction, it is suspended " + xid);
                        ex.errorCode = -6;
                        throw ex;
                    }
                    tx.rollback();
                }
            }
            else if (tx != null) {
                tx.rollback();
            }
            return null;
        }
        
        private void returnReferences(final Transaction tx, final AMQSession session) throws Exception {
            if (session == null || session.isClosed()) {
                return;
            }
            final RefsOperation oper = (RefsOperation)tx.getProperty(6);
            if (oper != null) {
                final List<MessageReference> ackRefs = (List<MessageReference>)oper.getReferencesToAcknowledge();
                final ListIterator<MessageReference> referenceIterator = ackRefs.listIterator(ackRefs.size());
                while (referenceIterator.hasPrevious()) {
                    final MessageReference ref = referenceIterator.previous();
                    ServerConsumer consumer = null;
                    if (ref.hasConsumerId()) {
                        consumer = session.getCoreSession().locateConsumer(ref.getConsumerId());
                    }
                    if (consumer != null) {
                        referenceIterator.remove();
                        ref.incrementDeliveryCount();
                        consumer.backToDelivering(ref);
                        final AMQConsumer amqConsumer = (AMQConsumer)consumer.getProtocolData();
                        amqConsumer.addRolledback(ref);
                    }
                }
            }
        }
        
        public Response processShutdown(final ShutdownInfo info) throws Exception {
            OpenWireConnection.this.shutdown(false);
            return null;
        }
        
        public Response processWireFormat(final WireFormatInfo command) throws Exception {
            OpenWireConnection.this.inWireFormat.renegotiateWireFormat(command);
            OpenWireConnection.this.outWireFormat.renegotiateWireFormat(command);
            OpenWireConnection.this.protocolManager.sendBrokerInfo(OpenWireConnection.this);
            OpenWireConnection.this.protocolManager.configureInactivityParams(OpenWireConnection.this, command);
            return null;
        }
        
        public Response processAddDestination(final DestinationInfo dest) throws Exception {
            Response resp = null;
            try {
                OpenWireConnection.this.addDestination(dest);
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
        
        public Response processAddSession(final SessionInfo info) throws Exception {
            if (!OpenWireConnection.this.state.getSessionIds().contains(info.getSessionId())) {
                OpenWireConnection.this.addSession(info);
                OpenWireConnection.this.state.addSession(info);
            }
            return null;
        }
        
        public Response processBeginTransaction(final TransactionInfo info) throws Exception {
            final TransactionId txID = info.getTransactionId();
            try {
                OpenWireConnection.this.internalSession.resetTX((Transaction)null);
                if (txID.isXATransaction()) {
                    final Xid xid = (Xid)OpenWireUtil.toXID(txID);
                    OpenWireConnection.this.internalSession.xaStart(xid);
                }
                else {
                    final Transaction transaction = OpenWireConnection.this.internalSession.newTransaction();
                    OpenWireConnection.this.txMap.put(txID, transaction);
                    transaction.addOperation((TransactionOperation)new TransactionOperationAbstract() {
                        public void afterCommit(final Transaction tx) {
                            OpenWireConnection.this.txMap.remove(txID);
                        }
                    });
                }
            }
            finally {
                OpenWireConnection.this.internalSession.resetTX((Transaction)null);
            }
            return null;
        }
        
        public Response processCommitTransactionOnePhase(final TransactionInfo info) throws Exception {
            return this.processCommit(info, true);
        }
        
        private Response processCommit(final TransactionInfo info, final boolean onePhase) throws Exception {
            final TransactionId txID = info.getTransactionId();
            final Transaction tx = OpenWireConnection.this.lookupTX(txID, null, true);
            if (txID.isXATransaction()) {
                final ResourceManager resourceManager = OpenWireConnection.this.server.getResourceManager();
                final Xid xid = (Xid)OpenWireUtil.toXID(txID);
                if (OpenWireConnection.logger.isTraceEnabled()) {
                    OpenWireConnection.logger.trace((Object)("XAcommit into " + tx + ", xid=" + xid));
                }
                if (tx == null) {
                    if (resourceManager.getHeuristicCommittedTransactions().contains(xid)) {
                        final XAException ex = new XAException("transaction has been heuristically committed: " + xid);
                        ex.errorCode = 7;
                        throw ex;
                    }
                    if (resourceManager.getHeuristicRolledbackTransactions().contains(xid)) {
                        final XAException ex = new XAException("transaction has been heuristically rolled back: " + xid);
                        ex.errorCode = 6;
                        throw ex;
                    }
                    if (OpenWireConnection.logger.isTraceEnabled()) {
                        OpenWireConnection.logger.trace((Object)("XAcommit into " + tx + ", xid=" + xid + " cannot find it"));
                    }
                    final XAException ex = new XAException("Cannot find xid in resource manager: " + xid);
                    ex.errorCode = -4;
                    throw ex;
                }
                else {
                    if (tx.getState() == Transaction.State.SUSPENDED) {
                        resourceManager.putTransaction(xid, tx);
                        final XAException ex = new XAException("Cannot commit transaction, it is suspended " + xid);
                        ex.errorCode = -6;
                        throw ex;
                    }
                    tx.commit(onePhase);
                }
            }
            else if (tx != null) {
                tx.commit(onePhase);
            }
            return null;
        }
        
        public Response processCommitTransactionTwoPhase(final TransactionInfo info) throws Exception {
            return this.processCommit(info, false);
        }
        
        public Response processForgetTransaction(final TransactionInfo info) throws Exception {
            final TransactionId txID = info.getTransactionId();
            if (txID.isXATransaction()) {
                try {
                    final Xid xid = (Xid)OpenWireUtil.toXID(info.getTransactionId());
                    OpenWireConnection.this.internalSession.xaForget(xid);
                    return null;
                }
                catch (Exception e) {
                    ActiveMQServerLogger.LOGGER.warn((Object)"Error during method invocation", (Throwable)e);
                    throw e;
                }
            }
            OpenWireConnection.this.txMap.remove(txID);
            return null;
        }
        
        public Response processPrepareTransaction(final TransactionInfo info) throws Exception {
            final TransactionId txID = info.getTransactionId();
            try {
                if (txID.isXATransaction()) {
                    try {
                        final Xid xid = (Xid)OpenWireUtil.toXID(info.getTransactionId());
                        OpenWireConnection.this.internalSession.xaPrepare(xid);
                        return (Response)new IntegerResponse(0);
                    }
                    catch (Exception e) {
                        ActiveMQServerLogger.LOGGER.warn((Object)"Error during method invocation", (Throwable)e);
                        throw e;
                    }
                }
                final Transaction tx = OpenWireConnection.this.lookupTX(txID, null);
                tx.prepare();
            }
            finally {
                OpenWireConnection.this.internalSession.resetTX((Transaction)null);
            }
            return (Response)new IntegerResponse(0);
        }
        
        public Response processEndTransaction(final TransactionInfo info) throws Exception {
            final TransactionId txID = info.getTransactionId();
            if (txID.isXATransaction()) {
                try {
                    final Transaction tx = OpenWireConnection.this.lookupTX(txID, null);
                    OpenWireConnection.this.internalSession.resetTX(tx);
                    try {
                        final Xid xid = (Xid)OpenWireUtil.toXID(info.getTransactionId());
                        OpenWireConnection.this.internalSession.xaEnd(xid);
                    }
                    finally {
                        OpenWireConnection.this.internalSession.resetTX((Transaction)null);
                    }
                    return null;
                }
                catch (Exception e) {
                    ActiveMQServerLogger.LOGGER.warn((Object)"Error during method invocation", (Throwable)e);
                    throw e;
                }
            }
            OpenWireConnection.this.txMap.remove(txID);
            return null;
        }
        
        public Response processBrokerInfo(final BrokerInfo arg0) throws Exception {
            throw new IllegalStateException("not implemented! ");
        }
        
        public Response processConnectionControl(final ConnectionControl connectionControl) throws Exception {
            return null;
        }
        
        public Response processConnectionError(final ConnectionError arg0) throws Exception {
            throw new IllegalStateException("not implemented! ");
        }
        
        public Response processConsumerControl(final ConsumerControl consumerControl) throws Exception {
            try {
                OpenWireConnection.this.updateConsumer(consumerControl);
            }
            catch (Exception ex) {}
            return null;
        }
        
        public Response processControlCommand(final ControlCommand arg0) throws Exception {
            throw new IllegalStateException("not implemented! ");
        }
        
        public Response processFlush(final FlushCommand arg0) throws Exception {
            throw new IllegalStateException("not implemented! ");
        }
        
        public Response processKeepAlive(final KeepAliveInfo arg0) throws Exception {
            throw new IllegalStateException("not implemented! ");
        }
        
        public Response processMessage(final Message messageSend) throws Exception {
            final ProducerId producerId = messageSend.getProducerId();
            final AMQProducerBrokerExchange producerExchange = OpenWireConnection.this.getProducerBrokerExchange(producerId);
            final AMQConnectionContext pcontext = producerExchange.getConnectionContext();
            final ProducerInfo producerInfo = producerExchange.getProducerState().getInfo();
            final boolean sendProducerAck = !messageSend.isResponseRequired() && producerInfo.getWindowSize() > 0 && !pcontext.isInRecoveryMode();
            final AMQSession session = OpenWireConnection.this.getSession(producerId.getParentId());
            final Transaction tx = OpenWireConnection.this.lookupTX(messageSend.getTransactionId(), session);
            session.getCoreSession().resetTX(tx);
            try {
                session.send(producerInfo, messageSend, sendProducerAck);
            }
            catch (Exception e) {
                if (tx != null) {
                    tx.markAsRollbackOnly(new ActiveMQException(e.getMessage()));
                }
                throw e;
            }
            finally {
                session.getCoreSession().resetTX((Transaction)null);
            }
            return null;
        }
        
        public Response processMessageAck(final MessageAck ack) throws Exception {
            final AMQSession session = OpenWireConnection.this.getSession(ack.getConsumerId().getParentId());
            final Transaction tx = OpenWireConnection.this.lookupTX(ack.getTransactionId(), session);
            session.getCoreSession().resetTX(tx);
            try {
                final AMQConsumerBrokerExchange consumerBrokerExchange = OpenWireConnection.this.consumerExchanges.get(ack.getConsumerId());
                consumerBrokerExchange.acknowledge(ack);
            }
            catch (Exception e) {
                if (tx != null) {
                    tx.markAsRollbackOnly(new ActiveMQException(e.getMessage()));
                }
            }
            finally {
                session.getCoreSession().resetTX((Transaction)null);
            }
            return null;
        }
        
        public Response processMessageDispatch(final MessageDispatch arg0) throws Exception {
            return null;
        }
        
        public Response processMessageDispatchNotification(final MessageDispatchNotification arg0) throws Exception {
            return null;
        }
        
        public Response processMessagePull(final MessagePull arg0) throws Exception {
            final AMQConsumerBrokerExchange amqConsumerBrokerExchange = OpenWireConnection.this.consumerExchanges.get(arg0.getConsumerId());
            if (amqConsumerBrokerExchange == null) {
                throw new IllegalStateException("Consumer does not exist");
            }
            amqConsumerBrokerExchange.processMessagePull(arg0);
            return null;
        }
        
        public Response processProducerAck(final ProducerAck arg0) throws Exception {
            return null;
        }
        
        public Response processRecoverTransactions(final TransactionInfo info) throws Exception {
            final List<Xid> xids = (List<Xid>)OpenWireConnection.this.server.getResourceManager().getInDoubtTransactions();
            final List<TransactionId> recovered = new ArrayList<TransactionId>();
            for (final Xid xid : xids) {
                final XATransactionId amqXid = new XATransactionId(xid);
                recovered.add((TransactionId)amqXid);
            }
            return (Response)new DataArrayResponse((DataStructure[])recovered.toArray(new TransactionId[recovered.size()]));
        }
        
        public Response processRemoveConnection(final ConnectionId id, final long lastDeliveredSequenceId) throws Exception {
            try {
                for (final SessionState sessionState : OpenWireConnection.this.state.getSessionStates()) {
                    OpenWireConnection.this.propagateLastSequenceId(sessionState, lastDeliveredSequenceId);
                }
                OpenWireConnection.this.protocolManager.removeConnection(OpenWireConnection.this.state.getInfo(), null);
            }
            catch (Throwable t) {}
            return null;
        }
        
        public Response processRemoveConsumer(final ConsumerId id, final long lastDeliveredSequenceId) throws Exception {
            if (OpenWireConnection.this.destroyed) {
                return null;
            }
            final SessionId sessionId = id.getParentId();
            final SessionState ss = OpenWireConnection.this.state.getSessionState(sessionId);
            if (ss == null) {
                throw new IllegalStateException("Cannot remove a consumer from a session that had not been registered: " + sessionId);
            }
            final ConsumerState consumerState = ss.removeConsumer(id);
            if (consumerState == null) {
                throw new IllegalStateException("Cannot remove a consumer that had not been registered: " + id);
            }
            final ConsumerInfo info = consumerState.getInfo();
            info.setLastDeliveredSequenceId(lastDeliveredSequenceId);
            final AMQConsumerBrokerExchange consumerBrokerExchange = OpenWireConnection.this.consumerExchanges.remove(id);
            consumerBrokerExchange.removeConsumer();
            return null;
        }
    }
}
