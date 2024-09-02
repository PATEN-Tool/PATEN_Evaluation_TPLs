// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.activemq.broker;

import org.apache.commons.logging.LogFactory;
import org.apache.activemq.command.ConsumerControl;
import org.apache.activemq.command.ControlCommand;
import java.net.URI;
import java.util.Properties;
import org.apache.activemq.network.NetworkBridgeFactory;
import org.apache.activemq.transport.ResponseCorrelator;
import org.apache.activemq.transport.TransportFactory;
import org.apache.activemq.util.URISupport;
import org.apache.activemq.util.IntrospectionSupport;
import org.apache.activemq.network.NetworkBridgeConfiguration;
import org.apache.activemq.util.MarshallingSupport;
import org.apache.activemq.thread.DefaultThreadPools;
import java.util.concurrent.TimeUnit;
import org.apache.activemq.command.MessageDispatch;
import org.apache.activemq.command.ProducerAck;
import org.apache.activemq.command.ConnectionControl;
import org.apache.activemq.transaction.Transaction;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.activemq.Service;
import org.apache.activemq.util.ServiceSupport;
import org.apache.activemq.command.ConnectionInfo;
import java.util.Iterator;
import org.apache.activemq.command.SessionInfo;
import org.apache.activemq.state.ConsumerState;
import org.apache.activemq.command.ConsumerInfo;
import org.apache.activemq.state.ProducerState;
import org.apache.activemq.state.SessionState;
import org.apache.activemq.command.SessionId;
import org.apache.activemq.command.ProducerInfo;
import org.apache.activemq.command.DestinationInfo;
import org.apache.activemq.command.MessageDispatchNotification;
import org.apache.activemq.command.MessagePull;
import org.apache.activemq.command.MessageAck;
import org.apache.activemq.command.Message;
import org.apache.activemq.command.TransactionId;
import org.apache.activemq.command.DataStructure;
import org.apache.activemq.command.DataArrayResponse;
import org.apache.activemq.state.TransactionState;
import org.apache.activemq.command.IntegerResponse;
import org.apache.activemq.command.TransactionInfo;
import org.apache.activemq.command.FlushCommand;
import org.apache.activemq.command.ShutdownInfo;
import org.apache.activemq.command.RemoveSubscriptionInfo;
import org.apache.activemq.command.KeepAliveInfo;
import org.apache.activemq.command.ExceptionResponse;
import org.apache.activemq.command.ConnectionError;
import org.apache.activemq.transport.TransportListener;
import org.apache.activemq.command.Response;
import org.apache.activemq.transport.DefaultTransportListener;
import org.apache.activemq.broker.region.RegionBroker;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.activemq.thread.TaskRunnerFactory;
import org.apache.activemq.network.DemandForwardingBridge;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.activemq.command.ConsumerId;
import org.apache.activemq.command.ProducerId;
import java.util.concurrent.CountDownLatch;
import org.apache.activemq.broker.region.ConnectionStatistics;
import org.apache.activemq.command.WireFormatInfo;
import org.apache.activemq.security.MessageAuthorizationPolicy;
import org.apache.activemq.transport.Transport;
import org.apache.activemq.broker.ft.MasterBroker;
import java.util.concurrent.atomic.AtomicBoolean;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.activemq.thread.TaskRunner;
import org.apache.activemq.command.Command;
import java.util.List;
import org.apache.activemq.command.BrokerInfo;
import org.apache.activemq.state.ConnectionState;
import org.apache.activemq.command.ConnectionId;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.activemq.state.CommandVisitor;
import org.apache.activemq.thread.Task;

public class TransportConnection implements Connection, Task, CommandVisitor
{
    private static final Log LOG;
    private static final Log TRANSPORTLOG;
    private static final Log SERVICELOG;
    protected final Broker broker;
    protected final TransportConnector connector;
    protected final Map<ConnectionId, ConnectionState> brokerConnectionStates;
    protected BrokerInfo brokerInfo;
    protected final List<Command> dispatchQueue;
    protected TaskRunner taskRunner;
    protected final AtomicReference<IOException> transportException;
    protected AtomicBoolean dispatchStopped;
    private MasterBroker masterBroker;
    private final Transport transport;
    private MessageAuthorizationPolicy messageAuthorizationPolicy;
    private WireFormatInfo wireFormatInfo;
    private boolean inServiceException;
    private ConnectionStatistics statistics;
    private boolean manageable;
    private boolean slow;
    private boolean markedCandidate;
    private boolean blockedCandidate;
    private boolean blocked;
    private boolean connected;
    private boolean active;
    private boolean starting;
    private boolean pendingStop;
    private long timeStamp;
    private final AtomicBoolean stopping;
    private CountDownLatch stopped;
    private final AtomicBoolean asyncException;
    private final Map<ProducerId, ProducerBrokerExchange> producerExchanges;
    private final Map<ConsumerId, ConsumerBrokerExchange> consumerExchanges;
    private CountDownLatch dispatchStoppedLatch;
    private ConnectionContext context;
    private boolean networkConnection;
    private boolean faultTolerantConnection;
    private AtomicInteger protocolVersion;
    private DemandForwardingBridge duplexBridge;
    private final TaskRunnerFactory taskRunnerFactory;
    private TransportConnectionStateRegister connectionStateRegister;
    private final ReentrantReadWriteLock serviceLock;
    
    public TransportConnection(final TransportConnector connector, final Transport transport, final Broker broker, final TaskRunnerFactory taskRunnerFactory) {
        this.dispatchQueue = new LinkedList<Command>();
        this.transportException = new AtomicReference<IOException>();
        this.dispatchStopped = new AtomicBoolean(false);
        this.statistics = new ConnectionStatistics();
        this.stopping = new AtomicBoolean(false);
        this.stopped = new CountDownLatch(1);
        this.asyncException = new AtomicBoolean(false);
        this.producerExchanges = new HashMap<ProducerId, ProducerBrokerExchange>();
        this.consumerExchanges = new HashMap<ConsumerId, ConsumerBrokerExchange>();
        this.dispatchStoppedLatch = new CountDownLatch(1);
        this.protocolVersion = new AtomicInteger(5);
        this.connectionStateRegister = new SingleTransportConnectionStateRegister();
        this.serviceLock = new ReentrantReadWriteLock();
        this.connector = connector;
        this.broker = broker;
        this.messageAuthorizationPolicy = connector.getMessageAuthorizationPolicy();
        final RegionBroker rb = (RegionBroker)broker.getAdaptor(RegionBroker.class);
        this.brokerConnectionStates = rb.getConnectionStates();
        if (connector != null) {
            this.statistics.setParent(connector.getStatistics());
        }
        this.taskRunnerFactory = taskRunnerFactory;
        (this.transport = transport).setTransportListener(new DefaultTransportListener() {
            @Override
            public void onCommand(final Object o) {
                TransportConnection.this.serviceLock.readLock().lock();
                try {
                    if (!(o instanceof Command)) {
                        throw new RuntimeException("Protocol violation - Command corrupted: " + o.toString());
                    }
                    final Command command = (Command)o;
                    final Response response = TransportConnection.this.service(command);
                    if (response != null) {
                        TransportConnection.this.dispatchSync(response);
                    }
                }
                finally {
                    TransportConnection.this.serviceLock.readLock().unlock();
                }
            }
            
            @Override
            public void onException(final IOException exception) {
                TransportConnection.this.serviceLock.readLock().lock();
                try {
                    TransportConnection.this.serviceTransportException(exception);
                }
                finally {
                    TransportConnection.this.serviceLock.readLock().unlock();
                }
            }
        });
        this.connected = true;
    }
    
    public int getDispatchQueueSize() {
        synchronized (this.dispatchQueue) {
            return this.dispatchQueue.size();
        }
    }
    
    public void serviceTransportException(final IOException e) {
        final BrokerService bService = this.connector.getBrokerService();
        if (bService.isShutdownOnSlaveFailure() && this.brokerInfo != null && this.brokerInfo.isSlaveBroker()) {
            TransportConnection.LOG.error((Object)("Slave has exception: " + e.getMessage() + " shutting down master now."), (Throwable)e);
            try {
                this.doStop();
                bService.stop();
            }
            catch (Exception ex) {
                TransportConnection.LOG.warn((Object)"Failed to stop the master", (Throwable)ex);
            }
        }
        if (!this.stopping.get()) {
            this.transportException.set(e);
            if (TransportConnection.TRANSPORTLOG.isDebugEnabled()) {
                TransportConnection.TRANSPORTLOG.debug((Object)("Transport failed: " + e), (Throwable)e);
            }
            this.stopAsync();
        }
    }
    
    public void serviceExceptionAsync(final IOException e) {
        if (this.asyncException.compareAndSet(false, true)) {
            new Thread("Async Exception Handler") {
                @Override
                public void run() {
                    TransportConnection.this.serviceException(e);
                }
            }.start();
        }
    }
    
    public void serviceException(final Throwable e) {
        if (e instanceof IOException) {
            this.serviceTransportException((IOException)e);
        }
        else if (e.getClass() == BrokerStoppedException.class) {
            if (!this.stopping.get()) {
                if (TransportConnection.SERVICELOG.isDebugEnabled()) {
                    TransportConnection.SERVICELOG.debug((Object)"Broker has been stopped.  Notifying client and closing his connection.");
                }
                final ConnectionError ce = new ConnectionError();
                ce.setException(e);
                this.dispatchSync(ce);
                try {
                    Thread.sleep(500L);
                }
                catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                this.stopAsync();
            }
        }
        else if (!this.stopping.get() && !this.inServiceException) {
            this.inServiceException = true;
            try {
                TransportConnection.SERVICELOG.warn((Object)("Async error occurred: " + e), e);
                final ConnectionError ce = new ConnectionError();
                ce.setException(e);
                this.dispatchAsync(ce);
            }
            finally {
                this.inServiceException = false;
            }
        }
    }
    
    public Response service(final Command command) {
        Response response = null;
        final boolean responseRequired = command.isResponseRequired();
        final int commandId = command.getCommandId();
        try {
            response = command.visit(this);
        }
        catch (Throwable e) {
            if (TransportConnection.SERVICELOG.isDebugEnabled() && e.getClass() != BrokerStoppedException.class) {
                TransportConnection.SERVICELOG.debug((Object)("Error occured while processing " + (responseRequired ? "sync" : "async") + " command: " + command + ", exception: " + e), e);
            }
            if (responseRequired) {
                response = new ExceptionResponse(e);
            }
            else {
                this.serviceException(e);
            }
        }
        if (responseRequired) {
            if (response == null) {
                response = new Response();
            }
            response.setCorrelationId(commandId);
        }
        if (this.context != null) {
            if (this.context.isDontSendReponse()) {
                this.context.setDontSendReponse(false);
                response = null;
            }
            this.context = null;
        }
        return response;
    }
    
    public Response processKeepAlive(final KeepAliveInfo info) throws Exception {
        return null;
    }
    
    public Response processRemoveSubscription(final RemoveSubscriptionInfo info) throws Exception {
        this.broker.removeSubscription(this.lookupConnectionState(info.getConnectionId()).getContext(), info);
        return null;
    }
    
    public Response processWireFormat(final WireFormatInfo info) throws Exception {
        this.wireFormatInfo = info;
        this.protocolVersion.set(info.getVersion());
        return null;
    }
    
    public Response processShutdown(final ShutdownInfo info) throws Exception {
        this.stopAsync();
        return null;
    }
    
    public Response processFlush(final FlushCommand command) throws Exception {
        return null;
    }
    
    public Response processBeginTransaction(final TransactionInfo info) throws Exception {
        final TransportConnectionState cs = this.lookupConnectionState(info.getConnectionId());
        this.context = null;
        if (cs != null) {
            this.context = cs.getContext();
        }
        if (cs == null) {
            throw new NullPointerException("Context is null");
        }
        if (cs.getTransactionState(info.getTransactionId()) == null) {
            cs.addTransactionState(info.getTransactionId());
            this.broker.beginTransaction(this.context, info.getTransactionId());
        }
        return null;
    }
    
    public Response processEndTransaction(final TransactionInfo info) throws Exception {
        return null;
    }
    
    public Response processPrepareTransaction(final TransactionInfo info) throws Exception {
        final TransportConnectionState cs = this.lookupConnectionState(info.getConnectionId());
        this.context = null;
        if (cs != null) {
            this.context = cs.getContext();
        }
        if (cs == null) {
            throw new NullPointerException("Context is null");
        }
        final TransactionState transactionState = cs.getTransactionState(info.getTransactionId());
        if (transactionState == null) {
            throw new IllegalStateException("Cannot prepare a transaction that had not been started: " + info.getTransactionId());
        }
        if (!transactionState.isPrepared()) {
            transactionState.setPrepared(true);
            final int result = this.broker.prepareTransaction(this.context, info.getTransactionId());
            transactionState.setPreparedResult(result);
            final IntegerResponse response = new IntegerResponse(result);
            return response;
        }
        final IntegerResponse response2 = new IntegerResponse(transactionState.getPreparedResult());
        return response2;
    }
    
    public Response processCommitTransactionOnePhase(final TransactionInfo info) throws Exception {
        final TransportConnectionState cs = this.lookupConnectionState(info.getConnectionId());
        this.context = cs.getContext();
        cs.removeTransactionState(info.getTransactionId());
        this.broker.commitTransaction(this.context, info.getTransactionId(), true);
        return null;
    }
    
    public Response processCommitTransactionTwoPhase(final TransactionInfo info) throws Exception {
        final TransportConnectionState cs = this.lookupConnectionState(info.getConnectionId());
        this.context = cs.getContext();
        cs.removeTransactionState(info.getTransactionId());
        this.broker.commitTransaction(this.context, info.getTransactionId(), false);
        return null;
    }
    
    public Response processRollbackTransaction(final TransactionInfo info) throws Exception {
        final TransportConnectionState cs = this.lookupConnectionState(info.getConnectionId());
        this.context = cs.getContext();
        cs.removeTransactionState(info.getTransactionId());
        this.broker.rollbackTransaction(this.context, info.getTransactionId());
        return null;
    }
    
    public Response processForgetTransaction(final TransactionInfo info) throws Exception {
        final TransportConnectionState cs = this.lookupConnectionState(info.getConnectionId());
        this.context = cs.getContext();
        this.broker.forgetTransaction(this.context, info.getTransactionId());
        return null;
    }
    
    public Response processRecoverTransactions(final TransactionInfo info) throws Exception {
        final TransportConnectionState cs = this.lookupConnectionState(info.getConnectionId());
        this.context = cs.getContext();
        final TransactionId[] preparedTransactions = this.broker.getPreparedTransactions(this.context);
        return new DataArrayResponse(preparedTransactions);
    }
    
    public Response processMessage(final Message messageSend) throws Exception {
        final ProducerId producerId = messageSend.getProducerId();
        final ProducerBrokerExchange producerExchange = this.getProducerBrokerExchange(producerId);
        this.broker.send(producerExchange, messageSend);
        return null;
    }
    
    public Response processMessageAck(final MessageAck ack) throws Exception {
        final ConsumerBrokerExchange consumerExchange = this.getConsumerBrokerExchange(ack.getConsumerId());
        this.broker.acknowledge(consumerExchange, ack);
        return null;
    }
    
    public Response processMessagePull(final MessagePull pull) throws Exception {
        return this.broker.messagePull(this.lookupConnectionState(pull.getConsumerId()).getContext(), pull);
    }
    
    public Response processMessageDispatchNotification(final MessageDispatchNotification notification) throws Exception {
        this.broker.processDispatchNotification(notification);
        return null;
    }
    
    public Response processAddDestination(final DestinationInfo info) throws Exception {
        final TransportConnectionState cs = this.lookupConnectionState(info.getConnectionId());
        this.broker.addDestinationInfo(cs.getContext(), info);
        if (info.getDestination().isTemporary()) {
            cs.addTempDestination(info);
        }
        return null;
    }
    
    public Response processRemoveDestination(final DestinationInfo info) throws Exception {
        final TransportConnectionState cs = this.lookupConnectionState(info.getConnectionId());
        this.broker.removeDestinationInfo(cs.getContext(), info);
        if (info.getDestination().isTemporary()) {
            cs.removeTempDestination(info.getDestination());
        }
        return null;
    }
    
    public Response processAddProducer(final ProducerInfo info) throws Exception {
        final SessionId sessionId = info.getProducerId().getParentId();
        final ConnectionId connectionId = sessionId.getParentId();
        final TransportConnectionState cs = this.lookupConnectionState(connectionId);
        final SessionState ss = cs.getSessionState(sessionId);
        if (ss == null) {
            throw new IllegalStateException("Cannot add a producer to a session that had not been registered: " + sessionId);
        }
        if (!ss.getProducerIds().contains(info.getProducerId())) {
            this.broker.addProducer(cs.getContext(), info);
            try {
                ss.addProducer(info);
            }
            catch (IllegalStateException e) {
                this.broker.removeProducer(cs.getContext(), info);
            }
        }
        return null;
    }
    
    public Response processRemoveProducer(final ProducerId id) throws Exception {
        final SessionId sessionId = id.getParentId();
        final ConnectionId connectionId = sessionId.getParentId();
        final TransportConnectionState cs = this.lookupConnectionState(connectionId);
        final SessionState ss = cs.getSessionState(sessionId);
        if (ss == null) {
            throw new IllegalStateException("Cannot remove a producer from a session that had not been registered: " + sessionId);
        }
        final ProducerState ps = ss.removeProducer(id);
        if (ps == null) {
            throw new IllegalStateException("Cannot remove a producer that had not been registered: " + id);
        }
        this.removeProducerBrokerExchange(id);
        this.broker.removeProducer(cs.getContext(), ps.getInfo());
        return null;
    }
    
    public Response processAddConsumer(final ConsumerInfo info) throws Exception {
        final SessionId sessionId = info.getConsumerId().getParentId();
        final ConnectionId connectionId = sessionId.getParentId();
        final TransportConnectionState cs = this.lookupConnectionState(connectionId);
        final SessionState ss = cs.getSessionState(sessionId);
        if (ss == null) {
            throw new IllegalStateException(this.broker.getBrokerName() + " Cannot add a consumer to a session that had not been registered: " + sessionId);
        }
        if (!ss.getConsumerIds().contains(info.getConsumerId())) {
            this.broker.addConsumer(cs.getContext(), info);
            try {
                ss.addConsumer(info);
            }
            catch (IllegalStateException e) {
                this.broker.removeConsumer(cs.getContext(), info);
            }
        }
        return null;
    }
    
    public Response processRemoveConsumer(final ConsumerId id, final long lastDeliveredSequenceId) throws Exception {
        final SessionId sessionId = id.getParentId();
        final ConnectionId connectionId = sessionId.getParentId();
        final TransportConnectionState cs = this.lookupConnectionState(connectionId);
        if (cs == null) {
            throw new IllegalStateException("Cannot remove a consumer from a connection that had not been registered: " + connectionId);
        }
        final SessionState ss = cs.getSessionState(sessionId);
        if (ss == null) {
            throw new IllegalStateException("Cannot remove a consumer from a session that had not been registered: " + sessionId);
        }
        final ConsumerState consumerState = ss.removeConsumer(id);
        if (consumerState == null) {
            throw new IllegalStateException("Cannot remove a consumer that had not been registered: " + id);
        }
        final ConsumerInfo info = consumerState.getInfo();
        info.setLastDeliveredSequenceId(lastDeliveredSequenceId);
        this.broker.removeConsumer(cs.getContext(), consumerState.getInfo());
        this.removeConsumerBrokerExchange(id);
        return null;
    }
    
    public Response processAddSession(final SessionInfo info) throws Exception {
        final ConnectionId connectionId = info.getSessionId().getParentId();
        final TransportConnectionState cs = this.lookupConnectionState(connectionId);
        if (!cs.getSessionIds().contains(info.getSessionId())) {
            this.broker.addSession(cs.getContext(), info);
            try {
                cs.addSession(info);
            }
            catch (IllegalStateException e) {
                e.printStackTrace();
                this.broker.removeSession(cs.getContext(), info);
            }
        }
        return null;
    }
    
    public Response processRemoveSession(final SessionId id, final long lastDeliveredSequenceId) throws Exception {
        final ConnectionId connectionId = id.getParentId();
        final TransportConnectionState cs = this.lookupConnectionState(connectionId);
        if (cs == null) {
            throw new IllegalStateException("Cannot remove session from connection that had not been registered: " + connectionId);
        }
        final SessionState session = cs.getSessionState(id);
        if (session == null) {
            throw new IllegalStateException("Cannot remove session that had not been registered: " + id);
        }
        session.shutdown();
        for (final ConsumerId consumerId : session.getConsumerIds()) {
            try {
                this.processRemoveConsumer(consumerId, lastDeliveredSequenceId);
            }
            catch (Throwable e) {
                TransportConnection.LOG.warn((Object)("Failed to remove consumer: " + consumerId + ". Reason: " + e), e);
            }
        }
        for (final ProducerId producerId : session.getProducerIds()) {
            try {
                this.processRemoveProducer(producerId);
            }
            catch (Throwable e) {
                TransportConnection.LOG.warn((Object)("Failed to remove producer: " + producerId + ". Reason: " + e), e);
            }
        }
        cs.removeSession(id);
        this.broker.removeSession(cs.getContext(), session.getInfo());
        return null;
    }
    
    public Response processAddConnection(final ConnectionInfo info) throws Exception {
        if (!info.isBrokerMasterConnector() && this.connector.getBrokerService().isWaitForSlave() && this.connector.getBrokerService().getSlaveStartSignal().getCount() == 1L) {
            ServiceSupport.dispose(this.transport);
            return new ExceptionResponse(new Exception("Master's slave not attached yet."));
        }
        if (this.wireFormatInfo != null && this.wireFormatInfo.getVersion() <= 2) {
            info.setClientMaster(true);
        }
        TransportConnectionState state;
        synchronized (this.brokerConnectionStates) {
            state = this.brokerConnectionStates.get(info.getConnectionId());
            if (state == null) {
                state = new TransportConnectionState(info, this);
                this.brokerConnectionStates.put(info.getConnectionId(), state);
            }
            state.incrementReference();
        }
        synchronized (state.getConnectionMutex()) {
            if (state.getConnection() != this) {
                TransportConnection.LOG.debug((Object)("Killing previous stale connection: " + state.getConnection().getRemoteAddress()));
                state.getConnection().stop();
                TransportConnection.LOG.debug((Object)("Connection " + this.getRemoteAddress() + " taking over previous connection: " + state.getConnection().getRemoteAddress()));
                state.setConnection(this);
                state.reset(info);
            }
        }
        this.registerConnectionState(info.getConnectionId(), state);
        TransportConnection.LOG.debug((Object)("Setting up new connection id: " + info.getConnectionId() + ", address: " + this.getRemoteAddress()));
        final String clientId = info.getClientId();
        (this.context = new ConnectionContext()).setBroker(this.broker);
        this.context.setClientId(clientId);
        this.context.setClientMaster(info.isClientMaster());
        this.context.setConnection(this);
        this.context.setConnectionId(info.getConnectionId());
        this.context.setConnector(this.connector);
        this.context.setMessageAuthorizationPolicy(this.getMessageAuthorizationPolicy());
        this.context.setNetworkConnection(this.networkConnection);
        this.context.setFaultTolerant(this.faultTolerantConnection);
        this.context.setTransactions(new ConcurrentHashMap<TransactionId, Transaction>());
        this.context.setUserName(info.getUserName());
        this.context.setWireFormatInfo(this.wireFormatInfo);
        this.manageable = info.isManageable();
        state.setContext(this.context);
        state.setConnection(this);
        try {
            this.broker.addConnection(this.context, info);
        }
        catch (Exception e) {
            this.brokerConnectionStates.remove(info);
            TransportConnection.LOG.warn((Object)"Failed to add Connection", (Throwable)e);
            throw e;
        }
        if (info.isManageable() && this.broker.isFaultTolerantConfiguration()) {
            final ConnectionControl command = new ConnectionControl();
            command.setFaultTolerant(this.broker.isFaultTolerantConfiguration());
            this.dispatchAsync(command);
        }
        return null;
    }
    
    public synchronized Response processRemoveConnection(final ConnectionId id, final long lastDeliveredSequenceId) throws InterruptedException {
        TransportConnection.LOG.debug((Object)("remove connection id: " + id));
        final TransportConnectionState cs = this.lookupConnectionState(id);
        if (cs != null) {
            cs.shutdown();
            for (final SessionId sessionId : cs.getSessionIds()) {
                try {
                    this.processRemoveSession(sessionId, lastDeliveredSequenceId);
                }
                catch (Throwable e) {
                    TransportConnection.SERVICELOG.warn((Object)("Failed to remove session " + sessionId), e);
                }
            }
            final Iterator iter = cs.getTempDesinations().iterator();
            while (iter.hasNext()) {
                final DestinationInfo di = iter.next();
                try {
                    this.broker.removeDestination(cs.getContext(), di.getDestination(), 0L);
                }
                catch (Throwable e) {
                    TransportConnection.SERVICELOG.warn((Object)("Failed to remove tmp destination " + di.getDestination()), e);
                }
                iter.remove();
            }
            try {
                this.broker.removeConnection(cs.getContext(), cs.getInfo(), null);
            }
            catch (Throwable e2) {
                TransportConnection.SERVICELOG.warn((Object)("Failed to remove connection " + cs.getInfo()), e2);
            }
            final TransportConnectionState state = this.unregisterConnectionState(id);
            if (state != null) {
                synchronized (this.brokerConnectionStates) {
                    if (state.decrementReference() == 0) {
                        this.brokerConnectionStates.remove(id);
                    }
                }
            }
        }
        return null;
    }
    
    public Response processProducerAck(final ProducerAck ack) throws Exception {
        return null;
    }
    
    public Connector getConnector() {
        return this.connector;
    }
    
    public void dispatchSync(final Command message) {
        try {
            this.processDispatch(message);
        }
        catch (IOException e) {
            this.serviceExceptionAsync(e);
        }
    }
    
    public void dispatchAsync(final Command message) {
        if (!this.stopping.get()) {
            if (this.taskRunner == null) {
                this.dispatchSync(message);
            }
            else {
                synchronized (this.dispatchQueue) {
                    this.dispatchQueue.add(message);
                }
                try {
                    this.taskRunner.wakeup();
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        else if (message.isMessageDispatch()) {
            final MessageDispatch md = (MessageDispatch)message;
            final Runnable sub = md.getTransmitCallback();
            this.broker.postProcessDispatch(md);
            if (sub != null) {
                sub.run();
            }
        }
    }
    
    protected void processDispatch(final Command command) throws IOException {
        final MessageDispatch messageDispatch = (MessageDispatch)(command.isMessageDispatch() ? command : null);
        try {
            if (!this.stopping.get()) {
                if (messageDispatch != null) {
                    this.broker.preProcessDispatch(messageDispatch);
                }
                this.dispatch(command);
            }
        }
        finally {
            if (messageDispatch != null) {
                final Runnable sub = messageDispatch.getTransmitCallback();
                this.broker.postProcessDispatch(messageDispatch);
                if (sub != null) {
                    sub.run();
                }
            }
        }
    }
    
    public boolean iterate() {
        try {
            if (this.stopping.get()) {
                if (this.dispatchStopped.compareAndSet(false, true)) {
                    if (this.transportException.get() == null) {
                        try {
                            this.dispatch(new ShutdownInfo());
                        }
                        catch (Throwable t) {}
                    }
                    this.dispatchStoppedLatch.countDown();
                }
                return false;
            }
            if (!this.dispatchStopped.get()) {
                Command command = null;
                synchronized (this.dispatchQueue) {
                    if (this.dispatchQueue.isEmpty()) {
                        return false;
                    }
                    command = this.dispatchQueue.remove(0);
                }
                this.processDispatch(command);
                return true;
            }
            return false;
        }
        catch (IOException e) {
            if (this.dispatchStopped.compareAndSet(false, true)) {
                this.dispatchStoppedLatch.countDown();
            }
            this.serviceExceptionAsync(e);
            return false;
        }
    }
    
    public ConnectionStatistics getStatistics() {
        return this.statistics;
    }
    
    public MessageAuthorizationPolicy getMessageAuthorizationPolicy() {
        return this.messageAuthorizationPolicy;
    }
    
    public void setMessageAuthorizationPolicy(final MessageAuthorizationPolicy messageAuthorizationPolicy) {
        this.messageAuthorizationPolicy = messageAuthorizationPolicy;
    }
    
    public boolean isManageable() {
        return this.manageable;
    }
    
    public void start() throws Exception {
        this.starting = true;
        try {
            synchronized (this) {
                if (this.taskRunnerFactory != null) {
                    this.taskRunner = this.taskRunnerFactory.createTaskRunner(this, "ActiveMQ Connection Dispatcher: " + this.getRemoteAddress());
                }
                else {
                    this.taskRunner = null;
                }
                this.transport.start();
                this.active = true;
                this.dispatchAsync(this.connector.getBrokerInfo());
                this.connector.onStarted(this);
            }
        }
        catch (Exception e) {
            this.stop();
            throw e;
        }
        finally {
            this.starting = false;
            if (this.pendingStop) {
                TransportConnection.LOG.debug((Object)"Calling the delayed stop()");
                this.stop();
            }
        }
    }
    
    public void stop() throws Exception {
        synchronized (this) {
            this.pendingStop = true;
            if (this.starting) {
                TransportConnection.LOG.debug((Object)"stop() called in the middle of start(). Delaying...");
                return;
            }
        }
        this.stopAsync();
        while (!this.stopped.await(5L, TimeUnit.SECONDS)) {
            TransportConnection.LOG.info((Object)("The connection to '" + this.transport.getRemoteAddress() + "' is taking a long time to shutdown."));
        }
    }
    
    public void stopAsync() {
        if (this.stopping.compareAndSet(false, true)) {
            final List<TransportConnectionState> connectionStates = this.listConnectionStates();
            for (final TransportConnectionState cs : connectionStates) {
                cs.getContext().getStopping().set(true);
            }
            try {
                DefaultThreadPools.getDefaultTaskRunnerFactory().execute(new Runnable() {
                    public void run() {
                        TransportConnection.this.serviceLock.writeLock().lock();
                        try {
                            TransportConnection.this.doStop();
                        }
                        catch (Throwable e) {
                            TransportConnection.LOG.debug((Object)("Error occured while shutting down a connection to '" + TransportConnection.this.transport.getRemoteAddress() + "': "), e);
                        }
                        finally {
                            TransportConnection.this.stopped.countDown();
                            TransportConnection.this.serviceLock.writeLock().unlock();
                        }
                    }
                });
            }
            catch (Throwable t) {
                TransportConnection.LOG.warn((Object)"cannot create async transport stopper thread.. not waiting for stop to complete, reason:", t);
                this.stopped.countDown();
            }
        }
    }
    
    @Override
    public String toString() {
        return "Transport Connection to: " + this.transport.getRemoteAddress();
    }
    
    protected void doStop() throws Exception, InterruptedException {
        TransportConnection.LOG.debug((Object)("Stopping connection: " + this.transport.getRemoteAddress()));
        this.connector.onStopped(this);
        try {
            synchronized (this) {
                if (this.masterBroker != null) {
                    this.masterBroker.stop();
                }
                if (this.duplexBridge != null) {
                    this.duplexBridge.stop();
                }
            }
        }
        catch (Exception ignore) {
            TransportConnection.LOG.trace((Object)"Exception caught stopping", (Throwable)ignore);
        }
        try {
            this.transport.stop();
            TransportConnection.LOG.debug((Object)("Stopped transport: " + this.transport.getRemoteAddress()));
        }
        catch (Exception e) {
            TransportConnection.LOG.debug((Object)("Could not stop transport: " + e), (Throwable)e);
        }
        if (this.taskRunner != null) {
            this.taskRunner.shutdown(1L);
        }
        this.active = false;
        synchronized (this.dispatchQueue) {
            for (final Command command : this.dispatchQueue) {
                if (command.isMessageDispatch()) {
                    final MessageDispatch md = (MessageDispatch)command;
                    final Runnable sub = md.getTransmitCallback();
                    this.broker.postProcessDispatch(md);
                    if (sub == null) {
                        continue;
                    }
                    sub.run();
                }
            }
            this.dispatchQueue.clear();
        }
        if (!this.broker.isStopped()) {
            List<TransportConnectionState> connectionStates = this.listConnectionStates();
            connectionStates = this.listConnectionStates();
            for (final TransportConnectionState cs : connectionStates) {
                cs.getContext().getStopping().set(true);
                try {
                    TransportConnection.LOG.debug((Object)("Cleaning up connection resources: " + this.getRemoteAddress()));
                    this.processRemoveConnection(cs.getInfo().getConnectionId(), 0L);
                }
                catch (Throwable ignore2) {
                    ignore2.printStackTrace();
                }
            }
            if (this.brokerInfo != null) {
                this.broker.removeBroker(this, this.brokerInfo);
            }
        }
        TransportConnection.LOG.debug((Object)("Connection Stopped: " + this.getRemoteAddress()));
    }
    
    public boolean isBlockedCandidate() {
        return this.blockedCandidate;
    }
    
    public void setBlockedCandidate(final boolean blockedCandidate) {
        this.blockedCandidate = blockedCandidate;
    }
    
    public boolean isMarkedCandidate() {
        return this.markedCandidate;
    }
    
    public void setMarkedCandidate(final boolean markedCandidate) {
        if (!(this.markedCandidate = markedCandidate)) {
            this.timeStamp = 0L;
            this.blockedCandidate = false;
        }
    }
    
    public void setSlow(final boolean slow) {
        this.slow = slow;
    }
    
    public boolean isSlow() {
        return this.slow;
    }
    
    public boolean isMarkedBlockedCandidate() {
        return this.markedCandidate;
    }
    
    public void doMark() {
        if (this.timeStamp == 0L) {
            this.timeStamp = System.currentTimeMillis();
        }
    }
    
    public boolean isBlocked() {
        return this.blocked;
    }
    
    public boolean isConnected() {
        return this.connected;
    }
    
    public void setBlocked(final boolean blocked) {
        this.blocked = blocked;
    }
    
    public void setConnected(final boolean connected) {
        this.connected = connected;
    }
    
    public boolean isActive() {
        return this.active;
    }
    
    public void setActive(final boolean active) {
        this.active = active;
    }
    
    public synchronized boolean isStarting() {
        return this.starting;
    }
    
    public synchronized boolean isNetworkConnection() {
        return this.networkConnection;
    }
    
    protected synchronized void setStarting(final boolean starting) {
        this.starting = starting;
    }
    
    public synchronized boolean isPendingStop() {
        return this.pendingStop;
    }
    
    protected synchronized void setPendingStop(final boolean pendingStop) {
        this.pendingStop = pendingStop;
    }
    
    public Response processBrokerInfo(final BrokerInfo info) {
        if (info.isSlaveBroker()) {
            final BrokerService bService = this.connector.getBrokerService();
            final boolean passive = bService.isPassiveSlave() || info.isPassiveSlave();
            if (!passive) {
                final MutableBrokerFilter parent = (MutableBrokerFilter)this.broker.getAdaptor(MutableBrokerFilter.class);
                (this.masterBroker = new MasterBroker(parent, this.transport)).startProcessing();
            }
            TransportConnection.LOG.info((Object)((passive ? "Passive" : "Active") + " Slave Broker " + info.getBrokerName() + " is attached"));
            bService.slaveConnectionEstablished();
        }
        else if (info.isNetworkConnection() && info.isDuplexConnection()) {
            try {
                final Properties properties = MarshallingSupport.stringToProperties(info.getNetworkProperties());
                final Map<String, String> props = this.createMap(properties);
                final NetworkBridgeConfiguration config = new NetworkBridgeConfiguration();
                IntrospectionSupport.setProperties(config, props, "");
                config.setBrokerName(this.broker.getBrokerName());
                URI uri = this.broker.getVmConnectorURI();
                final HashMap<String, String> map = new HashMap<String, String>(URISupport.parseParamters(uri));
                map.put("network", "true");
                map.put("async", "false");
                uri = URISupport.createURIWithQuery(uri, URISupport.createQueryString(map));
                final Transport localTransport = TransportFactory.connect(uri);
                final Transport remoteBridgeTransport = new ResponseCorrelator(this.transport);
                (this.duplexBridge = NetworkBridgeFactory.createBridge(config, localTransport, remoteBridgeTransport)).setBrokerService(this.broker.getBrokerService());
                info.setDuplexConnection(false);
                this.duplexBridge.setCreatedByDuplex(true);
                this.duplexBridge.duplexStart(this, this.brokerInfo, info);
                TransportConnection.LOG.info((Object)("Created Duplex Bridge back to " + info.getBrokerName()));
                return null;
            }
            catch (Exception e) {
                TransportConnection.LOG.error((Object)"Creating duplex network bridge", (Throwable)e);
            }
        }
        if (this.brokerInfo != null) {
            TransportConnection.LOG.warn((Object)("Unexpected extra broker info command received: " + info));
        }
        this.brokerInfo = info;
        this.broker.addBroker(this, info);
        this.networkConnection = true;
        final List<TransportConnectionState> connectionStates = this.listConnectionStates();
        for (final TransportConnectionState cs : connectionStates) {
            cs.getContext().setNetworkConnection(true);
        }
        return null;
    }
    
    private HashMap<String, String> createMap(final Properties properties) {
        return new HashMap<String, String>((Map<? extends String, ? extends String>)properties);
    }
    
    protected void dispatch(final Command command) throws IOException {
        try {
            this.setMarkedCandidate(true);
            this.transport.oneway(command);
        }
        finally {
            this.setMarkedCandidate(false);
        }
    }
    
    public String getRemoteAddress() {
        return this.transport.getRemoteAddress();
    }
    
    public String getConnectionId() {
        final List<TransportConnectionState> connectionStates = this.listConnectionStates();
        final Iterator i$ = connectionStates.iterator();
        if (!i$.hasNext()) {
            return null;
        }
        final TransportConnectionState cs = i$.next();
        if (cs.getInfo().getClientId() != null) {
            return cs.getInfo().getClientId();
        }
        return cs.getInfo().getConnectionId().toString();
    }
    
    private ProducerBrokerExchange getProducerBrokerExchange(final ProducerId id) {
        ProducerBrokerExchange result = this.producerExchanges.get(id);
        if (result == null) {
            synchronized (this.producerExchanges) {
                result = new ProducerBrokerExchange();
                final TransportConnectionState state = this.lookupConnectionState(id);
                result.setConnectionContext(this.context = state.getContext());
                final SessionState ss = state.getSessionState(id.getParentId());
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
        else {
            this.context = result.getConnectionContext();
        }
        return result;
    }
    
    private void removeProducerBrokerExchange(final ProducerId id) {
        synchronized (this.producerExchanges) {
            this.producerExchanges.remove(id);
        }
    }
    
    private ConsumerBrokerExchange getConsumerBrokerExchange(final ConsumerId id) {
        ConsumerBrokerExchange result = this.consumerExchanges.get(id);
        if (result == null) {
            synchronized (this.consumerExchanges) {
                result = new ConsumerBrokerExchange();
                final TransportConnectionState state = this.lookupConnectionState(id);
                result.setConnectionContext(this.context = state.getContext());
                final SessionState ss = state.getSessionState(id.getParentId());
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
        return result;
    }
    
    private void removeConsumerBrokerExchange(final ConsumerId id) {
        synchronized (this.consumerExchanges) {
            this.consumerExchanges.remove(id);
        }
    }
    
    public int getProtocolVersion() {
        return this.protocolVersion.get();
    }
    
    public Response processControlCommand(final ControlCommand command) throws Exception {
        final String control = command.getCommand();
        if (control != null && control.equals("shutdown")) {
            System.exit(0);
        }
        return null;
    }
    
    public Response processMessageDispatch(final MessageDispatch dispatch) throws Exception {
        return null;
    }
    
    public Response processConnectionControl(final ConnectionControl control) throws Exception {
        if (control != null) {
            this.faultTolerantConnection = control.isFaultTolerant();
        }
        return null;
    }
    
    public Response processConnectionError(final ConnectionError error) throws Exception {
        return null;
    }
    
    public Response processConsumerControl(final ConsumerControl control) throws Exception {
        return null;
    }
    
    protected synchronized TransportConnectionState registerConnectionState(final ConnectionId connectionId, final TransportConnectionState state) {
        TransportConnectionState cs = null;
        if (!this.connectionStateRegister.isEmpty() && !this.connectionStateRegister.doesHandleMultipleConnectionStates()) {
            final TransportConnectionStateRegister newRegister = new MapTransportConnectionStateRegister();
            newRegister.intialize(this.connectionStateRegister);
            this.connectionStateRegister = newRegister;
        }
        cs = this.connectionStateRegister.registerConnectionState(connectionId, state);
        return cs;
    }
    
    protected synchronized TransportConnectionState unregisterConnectionState(final ConnectionId connectionId) {
        return this.connectionStateRegister.unregisterConnectionState(connectionId);
    }
    
    protected synchronized List<TransportConnectionState> listConnectionStates() {
        return this.connectionStateRegister.listConnectionStates();
    }
    
    protected synchronized TransportConnectionState lookupConnectionState(final String connectionId) {
        return this.connectionStateRegister.lookupConnectionState(connectionId);
    }
    
    protected synchronized TransportConnectionState lookupConnectionState(final ConsumerId id) {
        return this.connectionStateRegister.lookupConnectionState(id);
    }
    
    protected synchronized TransportConnectionState lookupConnectionState(final ProducerId id) {
        return this.connectionStateRegister.lookupConnectionState(id);
    }
    
    protected synchronized TransportConnectionState lookupConnectionState(final SessionId id) {
        return this.connectionStateRegister.lookupConnectionState(id);
    }
    
    protected synchronized TransportConnectionState lookupConnectionState(final ConnectionId connectionId) {
        return this.connectionStateRegister.lookupConnectionState(connectionId);
    }
    
    static {
        LOG = LogFactory.getLog((Class)TransportConnection.class);
        TRANSPORTLOG = LogFactory.getLog(TransportConnection.class.getName() + ".Transport");
        SERVICELOG = LogFactory.getLog(TransportConnection.class.getName() + ".Service");
    }
}
