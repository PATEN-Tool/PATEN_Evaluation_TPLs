// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.activemq.broker;

import org.slf4j.LoggerFactory;
import org.apache.activemq.command.BrokerSubscriptionInfo;
import org.apache.activemq.command.ConsumerControl;
import org.apache.activemq.command.ControlCommand;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.activemq.transport.TransportDisposedIOException;
import org.apache.activemq.network.NetworkConnector;
import org.apache.activemq.network.NetworkBridgeListener;
import org.apache.activemq.network.MBeanNetworkListener;
import org.apache.activemq.transport.ResponseCorrelator;
import org.apache.activemq.network.NetworkBridgeFactory;
import org.apache.activemq.util.NetworkBridgeUtils;
import java.util.Properties;
import org.apache.activemq.util.IntrospectionSupport;
import org.apache.activemq.util.MarshallingSupport;
import org.apache.activemq.network.NetworkBridgeConfiguration;
import java.util.concurrent.TimeUnit;
import org.apache.activemq.transport.TransmitCallback;
import org.apache.activemq.command.MessageDispatch;
import org.apache.activemq.command.ProducerAck;
import org.apache.activemq.command.ConnectionControl;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.activemq.command.ConnectionInfo;
import org.apache.activemq.command.SessionInfo;
import org.apache.activemq.state.ConsumerState;
import org.apache.activemq.command.ConsumerInfo;
import org.apache.activemq.state.ProducerState;
import org.apache.activemq.command.ActiveMQDestination;
import org.apache.activemq.state.SessionState;
import org.apache.activemq.command.SessionId;
import org.apache.activemq.advisory.AdvisorySupport;
import org.apache.activemq.command.ProducerInfo;
import org.apache.activemq.command.DestinationInfo;
import org.apache.activemq.command.MessageDispatchNotification;
import org.apache.activemq.command.MessagePull;
import org.apache.activemq.command.TransactionId;
import org.apache.activemq.command.DataStructure;
import org.apache.activemq.command.DataArrayResponse;
import org.apache.activemq.command.IntegerResponse;
import java.util.Collection;
import org.apache.activemq.state.TransactionState;
import java.util.Iterator;
import org.apache.activemq.command.TransactionInfo;
import org.apache.activemq.command.FlushCommand;
import org.apache.activemq.command.ShutdownInfo;
import org.apache.activemq.command.RemoveSubscriptionInfo;
import org.apache.activemq.command.KeepAliveInfo;
import org.apache.activemq.command.MessageAck;
import org.apache.activemq.command.Message;
import org.apache.activemq.transaction.Transaction;
import org.apache.activemq.command.ExceptionResponse;
import org.slf4j.MDC;
import org.apache.activemq.command.ConnectionError;
import java.net.URI;
import java.io.EOFException;
import java.net.SocketException;
import org.apache.activemq.transport.TransportListener;
import java.io.IOException;
import org.apache.activemq.command.Response;
import org.apache.activemq.transport.DefaultTransportListener;
import org.apache.activemq.broker.region.RegionBroker;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.activemq.thread.TaskRunnerFactory;
import org.apache.activemq.network.DemandForwardingBridge;
import org.apache.activemq.command.ConsumerId;
import org.apache.activemq.command.ProducerId;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.activemq.broker.region.ConnectionStatistics;
import org.apache.activemq.command.WireFormatInfo;
import org.apache.activemq.security.MessageAuthorizationPolicy;
import org.apache.activemq.transport.Transport;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.activemq.thread.TaskRunner;
import org.apache.activemq.command.Command;
import java.util.List;
import org.apache.activemq.command.BrokerInfo;
import org.apache.activemq.state.ConnectionState;
import org.apache.activemq.command.ConnectionId;
import java.util.Map;
import org.slf4j.Logger;
import org.apache.activemq.state.CommandVisitor;
import org.apache.activemq.thread.Task;

public class TransportConnection implements Connection, Task, CommandVisitor
{
    private static final Logger LOG;
    private static final Logger TRANSPORTLOG;
    private static final Logger SERVICELOG;
    protected final Broker broker;
    protected final BrokerService brokerService;
    protected final TransportConnector connector;
    protected final Map<ConnectionId, ConnectionState> brokerConnectionStates;
    protected BrokerInfo brokerInfo;
    protected final List<Command> dispatchQueue;
    protected TaskRunner taskRunner;
    protected final AtomicReference<Throwable> transportException;
    protected AtomicBoolean dispatchStopped;
    private final Transport transport;
    private MessageAuthorizationPolicy messageAuthorizationPolicy;
    private WireFormatInfo wireFormatInfo;
    private boolean inServiceException;
    private final ConnectionStatistics statistics;
    private boolean manageable;
    private boolean slow;
    private boolean markedCandidate;
    private boolean blockedCandidate;
    private boolean blocked;
    private boolean connected;
    private boolean active;
    private static final int NEW = 0;
    private static final int STARTING = 1;
    private static final int STARTED = 2;
    private static final int PENDING_STOP = 3;
    private final AtomicInteger status;
    private long timeStamp;
    private final AtomicBoolean stopping;
    private final CountDownLatch stopped;
    private final AtomicBoolean asyncException;
    private final Map<ProducerId, ProducerBrokerExchange> producerExchanges;
    private final Map<ConsumerId, ConsumerBrokerExchange> consumerExchanges;
    private final CountDownLatch dispatchStoppedLatch;
    private ConnectionContext context;
    private boolean networkConnection;
    private boolean faultTolerantConnection;
    private final AtomicInteger protocolVersion;
    private DemandForwardingBridge duplexBridge;
    private final TaskRunnerFactory taskRunnerFactory;
    private final TaskRunnerFactory stopTaskRunnerFactory;
    private TransportConnectionStateRegister connectionStateRegister;
    private final ReentrantReadWriteLock serviceLock;
    private String duplexNetworkConnectorId;
    
    public TransportConnection(final TransportConnector connector, final Transport transport, final Broker broker, final TaskRunnerFactory taskRunnerFactory, final TaskRunnerFactory stopTaskRunnerFactory) {
        this.dispatchQueue = new LinkedList<Command>();
        this.transportException = new AtomicReference<Throwable>();
        this.dispatchStopped = new AtomicBoolean(false);
        this.statistics = new ConnectionStatistics();
        this.status = new AtomicInteger(0);
        this.stopping = new AtomicBoolean(false);
        this.stopped = new CountDownLatch(1);
        this.asyncException = new AtomicBoolean(false);
        this.producerExchanges = new HashMap<ProducerId, ProducerBrokerExchange>();
        this.consumerExchanges = new HashMap<ConsumerId, ConsumerBrokerExchange>();
        this.dispatchStoppedLatch = new CountDownLatch(1);
        this.protocolVersion = new AtomicInteger(12);
        this.connectionStateRegister = new SingleTransportConnectionStateRegister();
        this.serviceLock = new ReentrantReadWriteLock();
        this.connector = connector;
        this.broker = broker;
        this.brokerService = broker.getBrokerService();
        final RegionBroker rb = (RegionBroker)broker.getAdaptor(RegionBroker.class);
        this.brokerConnectionStates = rb.getConnectionStates();
        if (connector != null) {
            this.statistics.setParent(connector.getStatistics());
            this.messageAuthorizationPolicy = connector.getMessageAuthorizationPolicy();
        }
        this.taskRunnerFactory = taskRunnerFactory;
        this.stopTaskRunnerFactory = stopTaskRunnerFactory;
        this.transport = transport;
        if (this.transport instanceof BrokerServiceAware) {
            ((BrokerServiceAware)this.transport).setBrokerService(this.brokerService);
        }
        this.transport.setTransportListener((TransportListener)new DefaultTransportListener() {
            public void onCommand(final Object o) {
                TransportConnection.this.serviceLock.readLock().lock();
                try {
                    if (!(o instanceof Command)) {
                        throw new RuntimeException("Protocol violation - Command corrupted: " + o.toString());
                    }
                    final Command command = (Command)o;
                    if (TransportConnection.this.brokerService.isStopping()) {
                        throw new BrokerStoppedException("Broker " + TransportConnection.this.brokerService + " is being stopped");
                    }
                    final Response response = TransportConnection.this.service(command);
                    if (response != null && !TransportConnection.this.brokerService.isStopping()) {
                        TransportConnection.this.dispatchSync((Command)response);
                    }
                }
                finally {
                    TransportConnection.this.serviceLock.readLock().unlock();
                }
            }
            
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
    
    @Override
    public int getDispatchQueueSize() {
        synchronized (this.dispatchQueue) {
            return this.dispatchQueue.size();
        }
    }
    
    public void serviceTransportException(final IOException e) {
        if (!this.stopping.get() && this.status.get() != 3) {
            this.transportException.set(e);
            if (TransportConnection.TRANSPORTLOG.isDebugEnabled()) {
                TransportConnection.TRANSPORTLOG.debug(this + " failed: " + e, (Throwable)e);
            }
            else if (TransportConnection.TRANSPORTLOG.isWarnEnabled() && !this.expected(e)) {
                TransportConnection.TRANSPORTLOG.warn(this + " failed: " + e);
            }
            this.stopAsync(e);
        }
    }
    
    private boolean expected(final IOException e) {
        return this.isStomp() && ((e instanceof SocketException && e.getMessage().indexOf("reset") != -1) || e instanceof EOFException);
    }
    
    private boolean isStomp() {
        final URI uri = this.connector.getUri();
        return uri != null && uri.getScheme() != null && uri.getScheme().indexOf("stomp") != -1;
    }
    
    @Override
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
    
    @Override
    public void serviceException(final Throwable e) {
        if (e instanceof IOException) {
            this.serviceTransportException((IOException)e);
        }
        else if (e.getClass() == BrokerStoppedException.class) {
            if (!this.stopping.get()) {
                TransportConnection.SERVICELOG.debug("Broker has been stopped.  Notifying client and closing his connection.");
                final ConnectionError ce = new ConnectionError();
                ce.setException(e);
                this.dispatchSync((Command)ce);
                this.transportException.set(e);
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
                if (TransportConnection.SERVICELOG.isDebugEnabled()) {
                    TransportConnection.SERVICELOG.debug("Async error occurred: " + e, e);
                }
                else {
                    TransportConnection.SERVICELOG.warn("Async error occurred: " + e);
                }
                final ConnectionError ce = new ConnectionError();
                ce.setException(e);
                if (this.status.get() == 3) {
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
    
    @Override
    public Response service(final Command command) {
        MDC.put("activemq.connector", this.connector.getUri().toString());
        Response response = null;
        boolean responseRequired = command.isResponseRequired();
        final int commandId = command.getCommandId();
        try {
            if (this.status.get() != 3) {
                response = command.visit((CommandVisitor)this);
            }
            else {
                response = (Response)new ExceptionResponse((Throwable)this.transportException.get());
            }
        }
        catch (Throwable e) {
            if (TransportConnection.SERVICELOG.isDebugEnabled() && e.getClass() != BrokerStoppedException.class) {
                TransportConnection.SERVICELOG.debug("Error occured while processing " + (responseRequired ? "sync" : "async") + " command: " + command + ", exception: " + e, e);
            }
            if (e instanceof SuppressReplyException || e.getCause() instanceof SuppressReplyException) {
                TransportConnection.LOG.info("Suppressing reply to: " + command + " on: " + e + ", cause: " + e.getCause());
                responseRequired = false;
            }
            if (responseRequired) {
                if (e instanceof SecurityException || e.getCause() instanceof SecurityException) {
                    TransportConnection.SERVICELOG.warn("Security Error occurred on connection to: {}, {}", (Object)this.transport.getRemoteAddress(), (Object)e.getMessage());
                }
                response = (Response)new ExceptionResponse(e);
            }
            else {
                this.forceRollbackOnlyOnFailedAsyncTransactionOp(e, command);
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
        MDC.remove("activemq.connector");
        return response;
    }
    
    private void forceRollbackOnlyOnFailedAsyncTransactionOp(final Throwable e, final Command command) {
        if (this.brokerService.isRollbackOnlyOnAsyncException() && !(e instanceof IOException) && this.isInTransaction(command)) {
            final Transaction transaction = this.getActiveTransaction(command);
            if (transaction != null && !transaction.isRollbackOnly()) {
                TransportConnection.LOG.debug("on async exception, force rollback of transaction for: " + command, e);
                transaction.setRollbackOnly(e);
            }
        }
    }
    
    private Transaction getActiveTransaction(final Command command) {
        Transaction transaction = null;
        try {
            if (command instanceof Message) {
                final Message messageSend = (Message)command;
                final ProducerId producerId = messageSend.getProducerId();
                final ProducerBrokerExchange producerExchange = this.getProducerBrokerExchange(producerId);
                transaction = producerExchange.getConnectionContext().getTransactions().get(messageSend.getTransactionId());
            }
            else if (command instanceof MessageAck) {
                final MessageAck messageAck = (MessageAck)command;
                final ConsumerBrokerExchange consumerExchange = this.getConsumerBrokerExchange(messageAck.getConsumerId());
                if (consumerExchange != null) {
                    transaction = consumerExchange.getConnectionContext().getTransactions().get(messageAck.getTransactionId());
                }
            }
        }
        catch (Exception ignored) {
            TransportConnection.LOG.trace("failed to find active transaction for command: " + command, (Throwable)ignored);
        }
        return transaction;
    }
    
    private boolean isInTransaction(final Command command) {
        return (command instanceof Message && ((Message)command).isInTransaction()) || (command instanceof MessageAck && ((MessageAck)command).isInTransaction());
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
    
    @Override
    public int getActiveTransactionCount() {
        int rc = 0;
        for (final TransportConnectionState cs : this.connectionStateRegister.listConnectionStates()) {
            rc += cs.getTransactionStates().size();
        }
        return rc;
    }
    
    @Override
    public Long getOldestActiveTransactionDuration() {
        TransactionState oldestTX = null;
        for (final TransportConnectionState cs : this.connectionStateRegister.listConnectionStates()) {
            final Collection<TransactionState> transactions = (Collection<TransactionState>)cs.getTransactionStates();
            for (final TransactionState transaction : transactions) {
                if (oldestTX == null || oldestTX.getCreatedAt() < transaction.getCreatedAt()) {
                    oldestTX = transaction;
                }
            }
        }
        if (oldestTX == null) {
            return null;
        }
        return System.currentTimeMillis() - oldestTX.getCreatedAt();
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
            throw new IllegalStateException("Cannot prepare a transaction that had not been started or previously returned XA_RDONLY: " + info.getTransactionId());
        }
        if (!transactionState.isPrepared()) {
            transactionState.setPrepared(true);
            final int result = this.broker.prepareTransaction(this.context, info.getTransactionId());
            transactionState.setPreparedResult(result);
            if (result == 3) {
                cs.removeTransactionState(info.getTransactionId());
            }
            final IntegerResponse response = new IntegerResponse(result);
            return (Response)response;
        }
        final IntegerResponse response2 = new IntegerResponse(transactionState.getPreparedResult());
        return (Response)response2;
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
        return (Response)new DataArrayResponse((DataStructure[])preparedTransactions);
    }
    
    public Response processMessage(final Message messageSend) throws Exception {
        final ProducerId producerId = messageSend.getProducerId();
        final ProducerBrokerExchange producerExchange = this.getProducerBrokerExchange(producerId);
        if (producerExchange.canDispatch(messageSend)) {
            this.broker.send(producerExchange, messageSend);
        }
        return null;
    }
    
    public Response processMessageAck(final MessageAck ack) throws Exception {
        final ConsumerBrokerExchange consumerExchange = this.getConsumerBrokerExchange(ack.getConsumerId());
        if (consumerExchange != null) {
            this.broker.acknowledge(consumerExchange, ack);
        }
        else if (ack.isInTransaction()) {
            TransportConnection.LOG.warn("no matching consumer {}, ignoring ack {}", (Object)consumerExchange, (Object)ack);
        }
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
        if (cs == null) {
            throw new IllegalStateException("Cannot add a producer to a connection that had not been registered: " + connectionId);
        }
        final SessionState ss = cs.getSessionState(sessionId);
        if (ss == null) {
            throw new IllegalStateException("Cannot add a producer to a session that had not been registered: " + sessionId);
        }
        if (!ss.getProducerIds().contains(info.getProducerId())) {
            final ActiveMQDestination destination = info.getDestination();
            if (!AdvisorySupport.isAdvisoryTopic(destination) && this.getProducerCount(connectionId) >= this.connector.getMaximumProducersAllowedPerConnection()) {
                throw new IllegalStateException("Can't add producer on connection " + connectionId + ": at maximum limit: " + this.connector.getMaximumProducersAllowedPerConnection());
            }
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
        if (cs == null) {
            throw new IllegalStateException("Cannot add a consumer to a connection that had not been registered: " + connectionId);
        }
        final SessionState ss = cs.getSessionState(sessionId);
        if (ss == null) {
            throw new IllegalStateException(this.broker.getBrokerName() + " Cannot add a consumer to a session that had not been registered: " + sessionId);
        }
        if (!ss.getConsumerIds().contains(info.getConsumerId())) {
            final ActiveMQDestination destination = info.getDestination();
            if (destination != null && !AdvisorySupport.isAdvisoryTopic(destination) && this.getConsumerCount(connectionId) >= this.connector.getMaximumConsumersAllowedPerConnection()) {
                throw new IllegalStateException("Can't add consumer on connection " + connectionId + ": at maximum limit: " + this.connector.getMaximumConsumersAllowedPerConnection());
            }
            this.broker.addConsumer(cs.getContext(), info);
            try {
                ss.addConsumer(info);
                this.addConsumerBrokerExchange(cs, info.getConsumerId());
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
        if (cs != null && !cs.getSessionIds().contains(info.getSessionId())) {
            this.broker.addSession(cs.getContext(), info);
            try {
                cs.addSession(info);
            }
            catch (IllegalStateException e) {
                TransportConnection.LOG.warn("Failed to add session: {}", (Object)info.getSessionId(), (Object)e);
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
                TransportConnection.LOG.warn("Failed to remove consumer: {}", (Object)consumerId, (Object)e);
            }
        }
        for (final ProducerId producerId : session.getProducerIds()) {
            try {
                this.processRemoveProducer(producerId);
            }
            catch (Throwable e) {
                TransportConnection.LOG.warn("Failed to remove producer: {}", (Object)producerId, (Object)e);
            }
        }
        cs.removeSession(id);
        this.broker.removeSession(cs.getContext(), session.getInfo());
        return null;
    }
    
    public Response processAddConnection(final ConnectionInfo info) throws Exception {
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
                TransportConnection.LOG.debug("Killing previous stale connection: {}", (Object)state.getConnection().getRemoteAddress());
                state.getConnection().stop();
                TransportConnection.LOG.debug("Connection {} taking over previous connection: {}", (Object)this.getRemoteAddress(), (Object)state.getConnection().getRemoteAddress());
                state.setConnection(this);
                state.reset(info);
            }
        }
        this.registerConnectionState(info.getConnectionId(), state);
        TransportConnection.LOG.debug("Setting up new connection id: {}, address: {}, info: {}", new Object[] { info.getConnectionId(), this.getRemoteAddress(), info });
        this.faultTolerantConnection = info.isFaultTolerant();
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
        this.context.setReconnect(info.isFailoverReconnect());
        this.manageable = info.isManageable();
        this.context.setConnectionState(state);
        state.setContext(this.context);
        state.setConnection(this);
        if (info.getClientIp() == null) {
            info.setClientIp(this.getRemoteAddress());
        }
        try {
            this.broker.addConnection(this.context, info);
        }
        catch (Exception e) {
            synchronized (this.brokerConnectionStates) {
                this.brokerConnectionStates.remove(info.getConnectionId());
            }
            this.unregisterConnectionState(info.getConnectionId());
            TransportConnection.LOG.warn("Failed to add Connection id={}, clientId={}, clientIP={} due to {}", new Object[] { info.getConnectionId(), clientId, info.getClientIp(), e.getLocalizedMessage() });
            this.delayedStop(2000, "Failed with SecurityException: " + e.getLocalizedMessage(), e);
            throw e;
        }
        if (info.isManageable()) {
            final ConnectionControl command = this.connector.getConnectionControl();
            command.setFaultTolerant(this.broker.isFaultTolerantConfiguration());
            if (info.isFailoverReconnect()) {
                command.setRebalanceConnection(false);
            }
            this.dispatchAsync((Command)command);
        }
        return null;
    }
    
    public synchronized Response processRemoveConnection(final ConnectionId id, final long lastDeliveredSequenceId) throws InterruptedException {
        TransportConnection.LOG.debug("remove connection id: {}", (Object)id);
        final TransportConnectionState cs = this.lookupConnectionState(id);
        if (cs != null) {
            cs.shutdown();
            for (final SessionId sessionId : cs.getSessionIds()) {
                try {
                    this.processRemoveSession(sessionId, lastDeliveredSequenceId);
                }
                catch (Throwable e) {
                    TransportConnection.SERVICELOG.warn("Failed to remove session {}", (Object)sessionId, (Object)e);
                }
            }
            final Iterator<DestinationInfo> iter = cs.getTempDestinations().iterator();
            while (iter.hasNext()) {
                final DestinationInfo di = iter.next();
                try {
                    this.broker.removeDestination(cs.getContext(), di.getDestination(), 0L);
                }
                catch (Throwable e) {
                    TransportConnection.SERVICELOG.warn("Failed to remove tmp destination {}", (Object)di.getDestination(), (Object)e);
                }
                iter.remove();
            }
            try {
                this.broker.removeConnection(cs.getContext(), cs.getInfo(), this.transportException.get());
            }
            catch (Throwable e2) {
                TransportConnection.SERVICELOG.warn("Failed to remove connection {}", (Object)cs.getInfo(), (Object)e2);
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
    
    @Override
    public Connector getConnector() {
        return this.connector;
    }
    
    @Override
    public void dispatchSync(final Command message) {
        try {
            this.processDispatch(message);
        }
        catch (IOException e) {
            this.serviceExceptionAsync(e);
        }
    }
    
    @Override
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
            final TransmitCallback sub = md.getTransmitCallback();
            this.broker.postProcessDispatch(md);
            if (sub != null) {
                sub.onFailure();
            }
        }
    }
    
    protected void processDispatch(final Command command) throws IOException {
        MessageDispatch messageDispatch = (MessageDispatch)(command.isMessageDispatch() ? command : null);
        try {
            if (!this.stopping.get()) {
                if (messageDispatch != null) {
                    try {
                        this.broker.preProcessDispatch(messageDispatch);
                    }
                    catch (RuntimeException convertToIO) {
                        throw new IOException(convertToIO);
                    }
                }
                this.dispatch(command);
            }
        }
        catch (IOException e) {
            if (messageDispatch != null) {
                final TransmitCallback sub = messageDispatch.getTransmitCallback();
                this.broker.postProcessDispatch(messageDispatch);
                if (sub != null) {
                    sub.onFailure();
                }
                messageDispatch = null;
                throw e;
            }
            if (TransportConnection.TRANSPORTLOG.isDebugEnabled()) {
                TransportConnection.TRANSPORTLOG.debug("Unexpected exception on asyncDispatch, command of type: " + command.getDataStructureType(), (Throwable)e);
            }
        }
        finally {
            if (messageDispatch != null) {
                final TransmitCallback sub2 = messageDispatch.getTransmitCallback();
                this.broker.postProcessDispatch(messageDispatch);
                if (sub2 != null) {
                    sub2.onSuccess();
                }
            }
        }
    }
    
    public boolean iterate() {
        try {
            if (this.status.get() == 3 || this.stopping.get()) {
                if (this.dispatchStopped.compareAndSet(false, true)) {
                    if (this.transportException.get() == null) {
                        try {
                            this.dispatch((Command)new ShutdownInfo());
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
    
    @Override
    public ConnectionStatistics getStatistics() {
        return this.statistics;
    }
    
    public MessageAuthorizationPolicy getMessageAuthorizationPolicy() {
        return this.messageAuthorizationPolicy;
    }
    
    public void setMessageAuthorizationPolicy(final MessageAuthorizationPolicy messageAuthorizationPolicy) {
        this.messageAuthorizationPolicy = messageAuthorizationPolicy;
    }
    
    @Override
    public boolean isManageable() {
        return this.manageable;
    }
    
    public void start() throws Exception {
        if (this.status.compareAndSet(0, 1)) {
            try {
                synchronized (this) {
                    if (this.taskRunnerFactory != null) {
                        this.taskRunner = this.taskRunnerFactory.createTaskRunner((Task)this, "ActiveMQ Connection Dispatcher: " + this.getRemoteAddress());
                    }
                    else {
                        this.taskRunner = null;
                    }
                    this.transport.start();
                    this.active = true;
                    final BrokerInfo info = this.connector.getBrokerInfo().copy();
                    if (this.connector.isUpdateClusterClients()) {
                        info.setPeerBrokerInfos(this.broker.getPeerBrokerInfos());
                    }
                    else {
                        info.setPeerBrokerInfos((BrokerInfo[])null);
                    }
                    this.dispatchAsync((Command)info);
                    this.connector.onStarted(this);
                }
            }
            catch (Exception e) {
                this.status.set(3);
                throw e;
            }
            finally {
                if (!this.status.compareAndSet(1, 2)) {
                    TransportConnection.LOG.debug("Calling the delayed stop() after start() {}", (Object)this);
                    this.stop();
                }
            }
        }
    }
    
    public void stop() throws Exception {
        this.stopAsync();
        while (!this.stopped.await(5L, TimeUnit.SECONDS)) {
            TransportConnection.LOG.info("The connection to '{}' is taking a long time to shutdown.", (Object)this.transport.getRemoteAddress());
        }
    }
    
    public void delayedStop(final int waitTime, final String reason, final Throwable cause) {
        if (waitTime > 0) {
            this.status.compareAndSet(1, 3);
            this.transportException.set(cause);
            try {
                this.stopTaskRunnerFactory.execute((Runnable)new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(waitTime);
                            TransportConnection.this.stopAsync();
                            TransportConnection.LOG.info("Stopping {} because {}", (Object)TransportConnection.this.transport.getRemoteAddress(), (Object)reason);
                        }
                        catch (InterruptedException ex) {}
                    }
                });
            }
            catch (Throwable t) {
                TransportConnection.LOG.warn("Cannot create stopAsync. This exception will be ignored.", t);
            }
        }
    }
    
    public void stopAsync(final Throwable cause) {
        this.transportException.set(cause);
        this.stopAsync();
    }
    
    public void stopAsync() {
        if (this.status.compareAndSet(1, 3)) {
            TransportConnection.LOG.debug("stopAsync() called in the middle of start(). Delaying till start completes..");
            return;
        }
        if (this.stopping.compareAndSet(false, true)) {
            final List<TransportConnectionState> connectionStates = this.listConnectionStates();
            for (final TransportConnectionState cs : connectionStates) {
                final ConnectionContext connectionContext = cs.getContext();
                if (connectionContext != null) {
                    connectionContext.getStopping().set(true);
                }
            }
            try {
                this.stopTaskRunnerFactory.execute((Runnable)new Runnable() {
                    @Override
                    public void run() {
                        TransportConnection.this.serviceLock.writeLock().lock();
                        try {
                            TransportConnection.this.doStop();
                        }
                        catch (Throwable e) {
                            TransportConnection.LOG.debug("Error occurred while shutting down a connection {}", (Object)this, (Object)e);
                        }
                        finally {
                            TransportConnection.this.stopped.countDown();
                            TransportConnection.this.serviceLock.writeLock().unlock();
                        }
                    }
                });
            }
            catch (Throwable t) {
                TransportConnection.LOG.warn("Cannot create async transport stopper thread. This exception is ignored. Not waiting for stop to complete", t);
                this.stopped.countDown();
            }
        }
    }
    
    @Override
    public String toString() {
        return "Transport Connection to: " + this.transport.getRemoteAddress();
    }
    
    protected void doStop() throws Exception {
        TransportConnection.LOG.debug("Stopping connection: {}", (Object)this.transport.getRemoteAddress());
        this.connector.onStopped(this);
        try {
            synchronized (this) {
                if (this.duplexBridge != null) {
                    this.duplexBridge.stop();
                }
            }
        }
        catch (Exception ignore) {
            TransportConnection.LOG.trace("Exception caught stopping. This exception is ignored.", (Throwable)ignore);
        }
        try {
            this.transport.stop();
            TransportConnection.LOG.debug("Stopped transport: {}", (Object)this.transport.getRemoteAddress());
        }
        catch (Exception e) {
            TransportConnection.LOG.debug("Could not stop transport to {}. This exception is ignored.", (Object)this.transport.getRemoteAddress(), (Object)e);
        }
        if (this.taskRunner != null) {
            this.taskRunner.shutdown(1L);
            this.taskRunner = null;
        }
        this.active = false;
        synchronized (this.dispatchQueue) {
            for (final Command command : this.dispatchQueue) {
                if (command.isMessageDispatch()) {
                    final MessageDispatch md = (MessageDispatch)command;
                    final TransmitCallback sub = md.getTransmitCallback();
                    this.broker.postProcessDispatch(md);
                    if (sub == null) {
                        continue;
                    }
                    sub.onFailure();
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
                    TransportConnection.LOG.debug("Cleaning up connection resources: {}", (Object)this.getRemoteAddress());
                    this.processRemoveConnection(cs.getInfo().getConnectionId(), -2L);
                }
                catch (Throwable ignore2) {
                    TransportConnection.LOG.debug("Exception caught removing connection {}. This exception is ignored.", (Object)cs.getInfo().getConnectionId(), (Object)ignore2);
                }
            }
        }
        TransportConnection.LOG.debug("Connection Stopped: {}", (Object)this.getRemoteAddress());
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
    
    @Override
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
    
    @Override
    public boolean isBlocked() {
        return this.blocked;
    }
    
    @Override
    public boolean isConnected() {
        return this.connected;
    }
    
    public void setBlocked(final boolean blocked) {
        this.blocked = blocked;
    }
    
    public void setConnected(final boolean connected) {
        this.connected = connected;
    }
    
    @Override
    public boolean isActive() {
        return this.active;
    }
    
    public void setActive(final boolean active) {
        this.active = active;
    }
    
    public boolean isStarting() {
        return this.status.get() == 1;
    }
    
    @Override
    public synchronized boolean isNetworkConnection() {
        return this.networkConnection;
    }
    
    @Override
    public boolean isFaultTolerantConnection() {
        return this.faultTolerantConnection;
    }
    
    public boolean isPendingStop() {
        return this.status.get() == 3;
    }
    
    private NetworkBridgeConfiguration getNetworkConfiguration(final BrokerInfo info) throws IOException {
        final Properties properties = MarshallingSupport.stringToProperties(info.getNetworkProperties());
        final Map<String, String> props = this.createMap(properties);
        final NetworkBridgeConfiguration config = new NetworkBridgeConfiguration();
        IntrospectionSupport.setProperties((Object)config, (Map)props, "");
        return config;
    }
    
    public Response processBrokerInfo(final BrokerInfo info) {
        Label_0570: {
            if (info.isSlaveBroker()) {
                TransportConnection.LOG.error(" Slave Brokers are no longer supported - slave trying to attach is: {}", (Object)info.getBrokerName());
            }
            else {
                if (info.isNetworkConnection() && !info.isDuplexConnection()) {
                    try {
                        final NetworkBridgeConfiguration config = this.getNetworkConfiguration(info);
                        if (config.isSyncDurableSubs() && this.protocolVersion.get() >= 12) {
                            TransportConnection.LOG.debug("SyncDurableSubs is enabled, Sending BrokerSubscriptionInfo");
                            this.dispatchSync((Command)NetworkBridgeUtils.getBrokerSubscriptionInfo(this.broker.getBrokerService(), config));
                        }
                        break Label_0570;
                    }
                    catch (Exception e) {
                        TransportConnection.LOG.error("Failed to respond to network bridge creation from broker {}", (Object)info.getBrokerId(), (Object)e);
                        return null;
                    }
                }
                if (info.isNetworkConnection() && info.isDuplexConnection()) {
                    try {
                        final NetworkBridgeConfiguration config = this.getNetworkConfiguration(info);
                        config.setBrokerName(this.broker.getBrokerName());
                        if (config.isSyncDurableSubs() && this.protocolVersion.get() >= 12) {
                            TransportConnection.LOG.debug("SyncDurableSubs is enabled, Sending BrokerSubscriptionInfo");
                            this.dispatchSync((Command)NetworkBridgeUtils.getBrokerSubscriptionInfo(this.broker.getBrokerService(), config));
                        }
                        final String duplexNetworkConnectorId = config.getName() + "@" + info.getBrokerId();
                        final CopyOnWriteArrayList<TransportConnection> connections = this.connector.getConnections();
                        synchronized (connections) {
                            for (final TransportConnection c : connections) {
                                if (c != this && duplexNetworkConnectorId.equals(c.getDuplexNetworkConnectorId())) {
                                    TransportConnection.LOG.warn("Stopping an existing active duplex connection [{}] for network connector ({}).", (Object)c, (Object)duplexNetworkConnectorId);
                                    c.stopAsync();
                                    c.getStopped().await(1L, TimeUnit.SECONDS);
                                }
                            }
                            this.setDuplexNetworkConnectorId(duplexNetworkConnectorId);
                        }
                        final Transport localTransport = NetworkBridgeFactory.createLocalTransport(config, this.broker.getVmConnectorURI());
                        Transport remoteBridgeTransport = this.transport;
                        if (!(remoteBridgeTransport instanceof ResponseCorrelator)) {
                            remoteBridgeTransport = (Transport)new ResponseCorrelator(remoteBridgeTransport);
                        }
                        String duplexName = localTransport.toString();
                        if (duplexName.contains("#")) {
                            duplexName = duplexName.substring(duplexName.lastIndexOf("#"));
                        }
                        final MBeanNetworkListener listener = new MBeanNetworkListener(this.brokerService, config, this.brokerService.createDuplexNetworkConnectorObjectName(duplexName));
                        listener.setCreatedByDuplex(true);
                        (this.duplexBridge = config.getBridgeFactory().createNetworkBridge(config, localTransport, remoteBridgeTransport, listener)).setBrokerService(this.brokerService);
                        this.duplexBridge.setDurableDestinations(NetworkConnector.getDurableTopicDestinations(this.broker.getDurableDestinations()));
                        info.setDuplexConnection(false);
                        this.duplexBridge.setCreatedByDuplex(true);
                        this.duplexBridge.duplexStart(this, this.brokerInfo, info);
                        TransportConnection.LOG.info("Started responder end of duplex bridge {}", (Object)duplexNetworkConnectorId);
                        return null;
                    }
                    catch (TransportDisposedIOException e2) {
                        TransportConnection.LOG.warn("Duplex bridge {} was stopped before it was correctly started.", (Object)this.duplexNetworkConnectorId);
                        return null;
                    }
                    catch (Exception e) {
                        TransportConnection.LOG.error("Failed to create responder end of duplex network bridge {}", (Object)this.duplexNetworkConnectorId, (Object)e);
                        return null;
                    }
                }
            }
        }
        if (this.brokerInfo != null) {
            TransportConnection.LOG.warn("Unexpected extra broker info command received: {}", (Object)info);
        }
        this.brokerInfo = info;
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
            this.transport.oneway((Object)command);
        }
        finally {
            this.setMarkedCandidate(false);
        }
    }
    
    @Override
    public String getRemoteAddress() {
        return this.transport.getRemoteAddress();
    }
    
    public Transport getTransport() {
        return this.transport;
    }
    
    @Override
    public String getConnectionId() {
        final List<TransportConnectionState> connectionStates = this.listConnectionStates();
        final Iterator<TransportConnectionState> iterator = connectionStates.iterator();
        if (!iterator.hasNext()) {
            return null;
        }
        final TransportConnectionState cs = iterator.next();
        if (cs.getInfo().getClientId() != null) {
            return cs.getInfo().getClientId();
        }
        return cs.getInfo().getConnectionId().toString();
    }
    
    @Override
    public void updateClient(final ConnectionControl control) {
        if (this.isActive() && !this.isBlocked() && this.isFaultTolerantConnection() && this.wireFormatInfo != null && this.wireFormatInfo.getVersion() >= 6) {
            this.dispatchAsync((Command)control);
        }
    }
    
    public ProducerBrokerExchange getProducerBrokerExchangeIfExists(final ProducerInfo producerInfo) {
        ProducerBrokerExchange result = null;
        if (producerInfo != null && producerInfo.getProducerId() != null) {
            synchronized (this.producerExchanges) {
                result = this.producerExchanges.get(producerInfo.getProducerId());
            }
        }
        return result;
    }
    
    private ProducerBrokerExchange getProducerBrokerExchange(final ProducerId id) throws IOException {
        ProducerBrokerExchange result = this.producerExchanges.get(id);
        if (result == null) {
            synchronized (this.producerExchanges) {
                result = new ProducerBrokerExchange();
                final TransportConnectionState state = this.lookupConnectionState(id);
                result.setConnectionContext(this.context = state.getContext());
                if (this.context.isReconnect() || (this.context.isNetworkConnection() && this.connector.isAuditNetworkProducers())) {
                    result.setLastStoredSequenceId(this.brokerService.getPersistenceAdapter().getLastProducerSequenceId(id));
                }
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
        final ConsumerBrokerExchange result = this.consumerExchanges.get(id);
        return result;
    }
    
    private ConsumerBrokerExchange addConsumerBrokerExchange(final TransportConnectionState connectionState, final ConsumerId id) {
        ConsumerBrokerExchange result = this.consumerExchanges.get(id);
        if (result == null) {
            synchronized (this.consumerExchanges) {
                result = new ConsumerBrokerExchange();
                result.setConnectionContext(this.context = connectionState.getContext());
                final SessionState ss = connectionState.getSessionState(id.getParentId());
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
        final ConsumerBrokerExchange consumerExchange = this.getConsumerBrokerExchange(control.getConsumerId());
        this.broker.processConsumerControl(consumerExchange, control);
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
    
    public synchronized TransportConnectionState lookupConnectionState(final ConnectionId connectionId) {
        return this.connectionStateRegister.lookupConnectionState(connectionId);
    }
    
    protected synchronized void setDuplexNetworkConnectorId(final String duplexNetworkConnectorId) {
        this.duplexNetworkConnectorId = duplexNetworkConnectorId;
    }
    
    protected synchronized String getDuplexNetworkConnectorId() {
        return this.duplexNetworkConnectorId;
    }
    
    public boolean isStopping() {
        return this.stopping.get();
    }
    
    protected CountDownLatch getStopped() {
        return this.stopped;
    }
    
    private int getProducerCount(final ConnectionId connectionId) {
        int result = 0;
        final TransportConnectionState cs = this.lookupConnectionState(connectionId);
        if (cs != null) {
            for (final SessionId sessionId : cs.getSessionIds()) {
                final SessionState sessionState = cs.getSessionState(sessionId);
                if (sessionState != null) {
                    result += sessionState.getProducerIds().size();
                }
            }
        }
        return result;
    }
    
    private int getConsumerCount(final ConnectionId connectionId) {
        int result = 0;
        final TransportConnectionState cs = this.lookupConnectionState(connectionId);
        if (cs != null) {
            for (final SessionId sessionId : cs.getSessionIds()) {
                final SessionState sessionState = cs.getSessionState(sessionId);
                if (sessionState != null) {
                    result += sessionState.getConsumerIds().size();
                }
            }
        }
        return result;
    }
    
    public WireFormatInfo getRemoteWireFormatInfo() {
        return this.wireFormatInfo;
    }
    
    public Response processBrokerSubscriptionInfo(final BrokerSubscriptionInfo info) throws Exception {
        return null;
    }
    
    static {
        LOG = LoggerFactory.getLogger((Class)TransportConnection.class);
        TRANSPORTLOG = LoggerFactory.getLogger(TransportConnection.class.getName() + ".Transport");
        SERVICELOG = LoggerFactory.getLogger(TransportConnection.class.getName() + ".Service");
    }
}
