// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.qpid.server.protocol.v0_8;

import org.apache.qpid.server.message.MessageContentSource;
import org.apache.qpid.server.filter.Filterable;
import org.slf4j.LoggerFactory;
import org.apache.qpid.server.transport.AMQPConnection;
import org.apache.qpid.server.protocol.v0_8.transport.ConfirmSelectOkBody;
import org.apache.qpid.server.protocol.v0_8.transport.TxSelectOkBody;
import org.apache.qpid.server.protocol.v0_8.transport.QueueDeleteOkBody;
import org.apache.qpid.server.protocol.v0_8.transport.QueueDeclareOkBody;
import org.apache.qpid.server.model.ExclusivityPolicy;
import org.apache.qpid.server.queue.QueueArgumentsConverter;
import java.util.UUID;
import org.apache.qpid.server.protocol.v0_8.transport.ExchangeDeleteOkBody;
import org.apache.qpid.server.virtualhost.RequiredExchangeException;
import org.apache.qpid.server.virtualhost.MessageDestinationIsAlternateException;
import org.apache.qpid.server.model.ConfiguredObjectTypeRegistry;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.Objects;
import org.apache.qpid.server.model.ConfiguredObjectAttribute;
import org.apache.qpid.server.configuration.IllegalConfigurationException;
import org.apache.qpid.server.virtualhost.UnknownAlternateBindingException;
import org.apache.qpid.server.model.NoFactoryForTypeException;
import org.apache.qpid.server.model.AbstractConfiguredObject;
import org.apache.qpid.server.virtualhost.ReservedExchangeNameException;
import org.apache.qpid.server.model.LifetimePolicy;
import org.apache.qpid.server.protocol.v0_8.transport.ExchangeBoundOkBody;
import org.apache.qpid.server.model.Exchange;
import org.apache.qpid.server.protocol.v0_8.transport.BasicContentHeaderProperties;
import org.apache.qpid.server.transport.util.Functions;
import org.apache.qpid.server.bytebuffer.QpidByteBuffer;
import org.apache.qpid.server.protocol.v0_8.transport.BasicGetEmptyBody;
import org.apache.qpid.server.protocol.v0_8.transport.BasicCancelOkBody;
import com.google.common.collect.Collections2;
import org.apache.qpid.server.protocol.v0_8.transport.AccessRequestOkBody;
import org.apache.qpid.server.protocol.ProtocolVersion;
import org.apache.qpid.server.store.TransactionLogResource;
import java.util.function.Predicate;
import org.apache.qpid.server.model.NamedAddressSpace;
import org.apache.qpid.server.session.AMQPSession;
import javax.security.auth.Subject;
import java.util.LinkedHashMap;
import org.apache.qpid.server.message.MessageInstanceConsumer;
import org.apache.qpid.server.txn.LocalTransaction;
import java.util.Iterator;
import org.apache.qpid.server.filter.MessageFilter;
import org.apache.qpid.server.filter.ArrivalTimeFilter;
import org.apache.qpid.server.filter.AMQInvalidArgumentException;
import org.apache.qpid.server.filter.FilterManagerFactory;
import org.apache.qpid.server.filter.AMQPFilterTypes;
import java.util.Collection;
import org.apache.qpid.server.message.RoutingResult;
import org.apache.qpid.server.message.MessageReference;
import org.apache.qpid.server.store.StoredMessage;
import org.apache.qpid.server.protocol.v0_8.transport.ContentBody;
import org.apache.qpid.server.store.MessageHandle;
import java.security.AccessControlException;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Futures;
import org.apache.qpid.server.logging.messages.ExchangeMessages;
import org.apache.qpid.server.protocol.v0_8.transport.BasicAckBody;
import org.apache.qpid.server.protocol.v0_8.transport.AMQBody;
import org.apache.qpid.server.protocol.v0_8.transport.AMQFrame;
import org.apache.qpid.server.protocol.v0_8.transport.BasicNackBody;
import org.apache.qpid.server.message.RejectType;
import org.apache.qpid.server.util.Action;
import org.apache.qpid.server.message.ServerMessage;
import org.apache.qpid.server.message.InstanceProperties;
import org.apache.qpid.server.store.StorableMessageMetaData;
import org.apache.qpid.server.protocol.v0_8.transport.ContentHeaderBody;
import org.apache.qpid.server.message.MessageDestination;
import org.apache.qpid.server.protocol.v0_8.transport.MessagePublishInfo;
import org.apache.qpid.server.filter.FilterManager;
import org.apache.qpid.server.consumer.ConsumerTarget;
import java.util.EnumSet;
import org.apache.qpid.server.consumer.ConsumerOption;
import org.apache.qpid.server.message.MessageSource;
import java.security.AccessControlContext;
import org.apache.qpid.server.logging.LogMessage;
import org.apache.qpid.server.protocol.v0_8.transport.AMQMethodBody;
import org.apache.qpid.server.protocol.v0_8.transport.MethodRegistry;
import java.security.AccessController;
import org.apache.qpid.server.logging.messages.ChannelMessages;
import java.security.PrivilegedAction;
import org.apache.qpid.server.protocol.v0_8.transport.AMQDataBlock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.HashMap;
import org.apache.qpid.server.model.Connection;
import java.util.List;
import java.util.Set;
import org.apache.qpid.server.txn.ServerTransaction;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.qpid.server.txn.AsyncCommand;
import org.apache.qpid.server.store.MessageStore;
import java.util.Map;
import org.apache.qpid.server.model.Queue;
import org.apache.qpid.server.message.MessageInstance;
import com.google.common.base.Function;
import org.slf4j.Logger;
import org.apache.qpid.server.util.Deletable;
import org.apache.qpid.server.logging.EventLoggerProvider;
import org.apache.qpid.server.protocol.v0_8.transport.ServerChannelMethodProcessor;
import org.apache.qpid.server.txn.AsyncAutoCommitTransaction;
import org.apache.qpid.server.session.AbstractAMQPSession;

public class AMQChannel extends AbstractAMQPSession<AMQChannel, ConsumerTarget_0_8> implements AsyncAutoCommitTransaction.FutureRecorder, ServerChannelMethodProcessor, EventLoggerProvider, CreditRestorer, Deletable<AMQChannel>
{
    public static final int DEFAULT_PREFETCH = 4096;
    private static final Logger LOGGER;
    private static final InfiniteCreditCreditManager INFINITE_CREDIT_CREDIT_MANAGER;
    private static final Function<MessageConsumerAssociation, MessageInstance> MESSAGE_INSTANCE_FUNCTION;
    private static final String ALTERNATE_EXCHANGE = "alternateExchange";
    private final DefaultQueueAssociationClearingTask _defaultQueueAssociationClearingTask;
    private final int _channelId;
    private final Pre0_10CreditManager _creditManager;
    private final boolean _forceMessageValidation;
    private long _deliveryTag;
    private volatile Queue<?> _defaultQueue;
    private int _consumerTag;
    private IncomingMessage _currentMessage;
    private final Map<AMQShortString, ConsumerTarget_0_8> _tag2SubscriptionTargetMap;
    private final MessageStore _messageStore;
    private final java.util.Queue<AsyncCommand> _unfinishedCommandsQueue;
    private final UnacknowledgedMessageMap _unacknowledgedMessageMap;
    private final AtomicBoolean _suspended;
    private volatile ServerTransaction _transaction;
    private final AMQPConnection_0_8 _connection;
    private final AtomicBoolean _closing;
    private final Set<Object> _blockingEntities;
    private final AtomicBoolean _blocking;
    private volatile boolean _rollingBack;
    private List<MessageConsumerAssociation> _resendList;
    private static final AMQShortString IMMEDIATE_DELIVERY_REPLY_TEXT;
    private final ClientDeliveryMethod _clientDeliveryMethod;
    private final ImmediateAction _immediateAction;
    private long _blockTime;
    private long _blockingTimeout;
    private boolean _confirmOnPublish;
    private long _confirmedMessageCounter;
    private boolean _wireBlockingState;
    private boolean _prefetchLoggedForChannel;
    private boolean _logChannelFlowMessages;
    private final CachedFrame _txCommitOkFrame;
    private boolean _channelFlow;
    private final String id;
    
    public AMQChannel(final AMQPConnection_0_8 connection, final int channelId, final MessageStore messageStore) {
        super((Connection)connection, channelId);
        this._defaultQueueAssociationClearingTask = new DefaultQueueAssociationClearingTask();
        this._deliveryTag = 0L;
        this._tag2SubscriptionTargetMap = new HashMap<AMQShortString, ConsumerTarget_0_8>();
        this._unfinishedCommandsQueue = new ConcurrentLinkedQueue<AsyncCommand>();
        this._suspended = new AtomicBoolean(false);
        this._closing = new AtomicBoolean(false);
        this._blockingEntities = Collections.synchronizedSet(new HashSet<Object>());
        this._blocking = new AtomicBoolean(false);
        this._resendList = new ArrayList<MessageConsumerAssociation>();
        this._immediateAction = new ImmediateAction();
        this._prefetchLoggedForChannel = false;
        this._logChannelFlowMessages = true;
        this._channelFlow = true;
        this.id = "(" + System.identityHashCode(this) + ")";
        this._creditManager = new Pre0_10CreditManager(0L, 0L, (long)connection.getContextValue((Class)Long.class, "connection.high_prefetch_limit"), (long)connection.getContextValue((Class)Long.class, "connection.batch_limit"));
        this._unacknowledgedMessageMap = new UnacknowledgedMessageMapImpl(4096, this);
        this._connection = connection;
        this._channelId = channelId;
        this._messageStore = messageStore;
        this._blockingTimeout = (long)connection.getBroker().getContextValue((Class)Long.class, "channel.flowControlEnforcementTimeout");
        this._transaction = (ServerTransaction)new AsyncAutoCommitTransaction(this._messageStore, (AsyncAutoCommitTransaction.FutureRecorder)this);
        final MethodRegistry methodRegistry = this._connection.getMethodRegistry();
        final AMQMethodBody responseBody = methodRegistry.createTxCommitOkBody();
        this._txCommitOkFrame = new CachedFrame(responseBody.generateFrame(this._channelId));
        this._clientDeliveryMethod = connection.createDeliveryMethod(this._channelId);
        AccessController.doPrivileged((PrivilegedAction<Object>)new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                AMQChannel.this.message(ChannelMessages.CREATE());
                return null;
            }
        }, this._accessControllerContext);
        this._forceMessageValidation = (boolean)connection.getContextValue((Class)Boolean.class, "qpid.connection.forceValidation");
    }
    
    private void message(final LogMessage message) {
        this.getEventLogger().message(message);
    }
    
    public AccessControlContext getAccessControllerContext() {
        return this._accessControllerContext;
    }
    
    private boolean performGet(final MessageSource queue, final boolean acks) throws MessageSource.ExistingConsumerPreventsExclusive, MessageSource.ExistingExclusiveConsumer, MessageSource.ConsumerAccessRefused, MessageSource.QueueDeleted {
        final GetDeliveryMethod getDeliveryMethod = new GetDeliveryMethod(queue);
        final EnumSet<ConsumerOption> options = EnumSet.of(ConsumerOption.TRANSIENT, ConsumerOption.ACQUIRES, ConsumerOption.SEES_REQUEUES);
        ConsumerTarget_0_8 target;
        if (acks) {
            target = ConsumerTarget_0_8.createGetAckTarget(this, AMQShortString.EMPTY_STRING, null, AMQChannel.INFINITE_CREDIT_CREDIT_MANAGER, getDeliveryMethod);
        }
        else {
            target = ConsumerTarget_0_8.createGetNoAckTarget(this, AMQShortString.EMPTY_STRING, null, AMQChannel.INFINITE_CREDIT_CREDIT_MANAGER, getDeliveryMethod);
        }
        queue.addConsumer((ConsumerTarget)target, (FilterManager)null, (Class)AMQMessage.class, "", (EnumSet)options, (Integer)null);
        target.updateNotifyWorkDesired();
        boolean canCallSendNextMessageAgain;
        do {
            canCallSendNextMessageAgain = target.sendNextMessage();
        } while (canCallSendNextMessageAgain && !getDeliveryMethod.hasDeliveredMessage());
        target.close();
        return getDeliveryMethod.hasDeliveredMessage();
    }
    
    boolean isTransactional() {
        return this._transaction.isTransactional();
    }
    
    ServerTransaction getTransaction() {
        return this._transaction;
    }
    
    public void receivedComplete() {
        AccessController.doPrivileged((PrivilegedAction<Object>)new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                AMQChannel.this.sync();
                return null;
            }
        }, this.getAccessControllerContext());
    }
    
    private void setPublishFrame(final MessagePublishInfo info, final MessageDestination e) {
        (this._currentMessage = new IncomingMessage(info)).setMessageDestination(e);
    }
    
    private void publishContentHeader(final ContentHeaderBody contentHeaderBody) {
        if (AMQChannel.LOGGER.isDebugEnabled()) {
            AMQChannel.LOGGER.debug("Content header received on channel " + this._channelId);
        }
        this._currentMessage.setContentHeaderBody(contentHeaderBody);
        this.deliverCurrentMessageIfComplete();
    }
    
    private void deliverCurrentMessageIfComplete() {
        if (this._currentMessage.allContentReceived()) {
            final MessagePublishInfo info = this._currentMessage.getMessagePublishInfo();
            final String routingKey = AMQShortString.toString(info.getRoutingKey());
            final String exchangeName = AMQShortString.toString(info.getExchange());
            try {
                final MessageDestination destination = this._currentMessage.getDestination();
                final ContentHeaderBody contentHeader = this._currentMessage.getContentHeader();
                this._connection.checkAuthorizedMessagePrincipal(AMQShortString.toString(contentHeader.getProperties().getUserId()));
                this._publishAuthCache.authorisePublish(destination, routingKey, info.isImmediate(), this._connection.getLastReadTime());
                if (this._confirmOnPublish) {
                    ++this._confirmedMessageCounter;
                }
                final long bodySize = this._currentMessage.getSize();
                try {
                    final MessageMetaData messageMetaData = new MessageMetaData(info, contentHeader, this.getConnection().getLastReadTime());
                    final MessageHandle<MessageMetaData> handle = (MessageHandle<MessageMetaData>)this._messageStore.addMessage((StorableMessageMetaData)messageMetaData);
                    final int bodyCount = this._currentMessage.getBodyCount();
                    if (bodyCount > 0) {
                        for (int i = 0; i < bodyCount; ++i) {
                            final ContentBody contentChunk = this._currentMessage.getContentChunk(i);
                            handle.addContent(contentChunk.getPayload());
                            contentChunk.dispose();
                        }
                    }
                    final StoredMessage<MessageMetaData> storedMessage = (StoredMessage<MessageMetaData>)handle.allContentAdded();
                    final AMQMessage amqMessage = new AMQMessage(storedMessage, this._connection.getReference());
                    try (final MessageReference reference = amqMessage.newReference()) {
                        this._currentMessage = null;
                        final InstanceProperties instanceProperties = (InstanceProperties)new InstanceProperties() {
                            public Object getProperty(final InstanceProperties.Property prop) {
                                switch (prop) {
                                    case EXPIRATION: {
                                        return amqMessage.getExpiration();
                                    }
                                    case IMMEDIATE: {
                                        return amqMessage.isImmediate();
                                    }
                                    case PERSISTENT: {
                                        return amqMessage.isPersistent();
                                    }
                                    case MANDATORY: {
                                        return amqMessage.isMandatory();
                                    }
                                    case REDELIVERED: {
                                        return false;
                                    }
                                    default: {
                                        return null;
                                    }
                                }
                            }
                        };
                        final RoutingResult<AMQMessage> result = (RoutingResult<AMQMessage>)destination.route((ServerMessage)amqMessage, amqMessage.getInitialRoutingAddress(), instanceProperties);
                        final int enqueues = result.send(this._transaction, (Action)(amqMessage.isImmediate() ? this._immediateAction : null));
                        if (enqueues == 0) {
                            final boolean mandatory = amqMessage.isMandatory();
                            final boolean closeOnNoRoute = this._connection.isCloseWhenNoRoute();
                            if (AMQChannel.LOGGER.isDebugEnabled()) {
                                AMQChannel.LOGGER.debug("Unroutable message exchange='{}', routing key='{}', mandatory={}, transactionalSession={}, closeOnNoRoute={}, confirmOnPublish={}", new Object[] { exchangeName, routingKey, mandatory, this.isTransactional(), closeOnNoRoute, this._confirmOnPublish });
                            }
                            int errorCode = 312;
                            String errorMessage = String.format("No route for message with exchange '%s' and routing key '%s'", exchangeName, routingKey);
                            if (result.containsReject(new RejectType[] { RejectType.LIMIT_EXCEEDED })) {
                                errorCode = 506;
                                errorMessage = errorMessage + ":" + result.getRejectReason();
                            }
                            if (mandatory && this.isTransactional() && !this._confirmOnPublish && this._connection.isCloseWhenNoRoute()) {
                                this._connection.sendConnectionClose(errorCode, errorMessage, this._channelId);
                            }
                            else if (mandatory || amqMessage.isImmediate()) {
                                if (this._confirmOnPublish) {
                                    this._connection.writeFrame(new AMQFrame(this._channelId, new BasicNackBody(this._confirmedMessageCounter, false, false)));
                                }
                                this._transaction.addPostTransactionAction((ServerTransaction.Action)new WriteReturnAction(errorCode, errorMessage, amqMessage));
                            }
                            else {
                                if (this._confirmOnPublish) {
                                    this._connection.writeFrame(new AMQFrame(this._channelId, new BasicAckBody(this._confirmedMessageCounter, false)));
                                }
                                this.message(ExchangeMessages.DISCARDMSG(exchangeName, routingKey));
                            }
                        }
                        else if (this._confirmOnPublish) {
                            this.recordFuture((ListenableFuture<Void>)Futures.immediateFuture((Object)null), (ServerTransaction.Action)new ServerTransaction.Action() {
                                private final long _deliveryTag = AMQChannel.this._confirmedMessageCounter;
                                
                                public void postCommit() {
                                    final BasicAckBody body = AMQChannel.this._connection.getMethodRegistry().createBasicAckBody(this._deliveryTag, false);
                                    AMQChannel.this._connection.writeFrame(body.generateFrame(AMQChannel.this._channelId));
                                }
                                
                                public void onRollback() {
                                    final BasicNackBody body = new BasicNackBody(this._deliveryTag, false, false);
                                    AMQChannel.this._connection.writeFrame(new AMQFrame(AMQChannel.this._channelId, body));
                                }
                            });
                        }
                    }
                }
                finally {
                    this.registerMessageReceived(bodySize);
                    if (this.isTransactional()) {
                        this.registerTransactedMessageReceived();
                    }
                    this._currentMessage = null;
                }
            }
            catch (AccessControlException e) {
                this._connection.sendConnectionClose(403, e.getMessage(), this.getChannelId());
            }
        }
    }
    
    private void publishContentBody(final ContentBody contentBody) {
        if (AMQChannel.LOGGER.isDebugEnabled()) {
            AMQChannel.LOGGER.debug(this.debugIdentity() + " content body received on channel " + this._channelId);
        }
        try {
            final long currentSize = this._currentMessage.addContentBodyFrame(contentBody);
            if (currentSize > this._currentMessage.getSize()) {
                this._connection.sendConnectionClose(501, "More message data received than content header defined", this._channelId);
            }
            else {
                this.deliverCurrentMessageIfComplete();
            }
        }
        catch (RuntimeException e) {
            this._currentMessage = null;
            throw e;
        }
    }
    
    public long getNextDeliveryTag() {
        return ++this._deliveryTag;
    }
    
    private int getNextConsumerTag() {
        return ++this._consumerTag;
    }
    
    private AMQShortString consumeFromSource(AMQShortString tag, final Collection<MessageSource> sources, final boolean acks, final FieldTable arguments, final boolean exclusive, final boolean noLocal) throws MessageSource.ExistingConsumerPreventsExclusive, MessageSource.ExistingExclusiveConsumer, AMQInvalidArgumentException, MessageSource.ConsumerAccessRefused, ConsumerTagInUseException, MessageSource.QueueDeleted {
        if (tag == null) {
            tag = AMQShortString.createAMQShortString("sgen_" + this.getNextConsumerTag());
        }
        if (this._tag2SubscriptionTargetMap.containsKey(tag)) {
            throw new ConsumerTagInUseException("Consumer already exists with same tag: " + tag);
        }
        final EnumSet<ConsumerOption> options = EnumSet.noneOf(ConsumerOption.class);
        final boolean multiQueue = sources.size() > 1;
        ConsumerTarget_0_8 target;
        if (arguments != null && Boolean.TRUE.equals(arguments.get(AMQPFilterTypes.NO_CONSUME.getValue()))) {
            target = ConsumerTarget_0_8.createBrowserTarget(this, tag, arguments, AMQChannel.INFINITE_CREDIT_CREDIT_MANAGER, multiQueue);
        }
        else if (acks) {
            target = ConsumerTarget_0_8.createAckTarget(this, tag, arguments, this._creditManager, multiQueue);
            options.add(ConsumerOption.ACQUIRES);
            options.add(ConsumerOption.SEES_REQUEUES);
        }
        else {
            target = ConsumerTarget_0_8.createNoAckTarget(this, tag, arguments, AMQChannel.INFINITE_CREDIT_CREDIT_MANAGER, multiQueue);
            options.add(ConsumerOption.ACQUIRES);
            options.add(ConsumerOption.SEES_REQUEUES);
        }
        if (exclusive) {
            options.add(ConsumerOption.EXCLUSIVE);
        }
        this._tag2SubscriptionTargetMap.put(tag, target);
        try {
            FilterManager filterManager = FilterManagerFactory.createManager((Map)FieldTable.convertToMap(arguments));
            if (noLocal) {
                if (filterManager == null) {
                    filterManager = new FilterManager();
                }
                final MessageFilter filter = (MessageFilter)new NoLocalFilter();
                filterManager.add(filter.getName(), filter);
            }
            if (arguments != null && arguments.containsKey(AMQPFilterTypes.REPLAY_PERIOD.toString())) {
                final Object value = arguments.get(AMQPFilterTypes.REPLAY_PERIOD.toString());
                long period = 0L;
                Label_0448: {
                    if (!(value instanceof Number)) {
                        if (value instanceof String) {
                            try {
                                period = Long.parseLong(value.toString());
                                break Label_0448;
                            }
                            catch (NumberFormatException e2) {
                                throw new AMQInvalidArgumentException("Cannot parse value " + value + " as a number for filter " + AMQPFilterTypes.REPLAY_PERIOD.toString());
                            }
                        }
                        throw new AMQInvalidArgumentException("Cannot parse value " + value + " as a number for filter " + AMQPFilterTypes.REPLAY_PERIOD.toString());
                    }
                    period = ((Number)value).longValue();
                }
                final long startingFrom = System.currentTimeMillis() - 1000L * period;
                if (filterManager == null) {
                    filterManager = new FilterManager();
                }
                final MessageFilter filter2 = (MessageFilter)new ArrivalTimeFilter(startingFrom, period == 0L);
                filterManager.add(filter2.getName(), filter2);
            }
            Integer priority = null;
            Label_0596: {
                if (arguments != null && arguments.containsKey("x-priority")) {
                    final Object value2 = arguments.get("x-priority");
                    if (value2 instanceof Number) {
                        priority = ((Number)value2).intValue();
                    }
                    else {
                        if (!(value2 instanceof String)) {
                            if (!(value2 instanceof AMQShortString)) {
                                break Label_0596;
                            }
                        }
                        try {
                            priority = Integer.parseInt(value2.toString());
                        }
                        catch (NumberFormatException ex2) {}
                    }
                }
            }
            for (final MessageSource source : sources) {
                source.addConsumer((ConsumerTarget)target, filterManager, (Class)AMQMessage.class, AMQShortString.toString(tag), (EnumSet)options, priority);
            }
            target.updateNotifyWorkDesired();
        }
        catch (AccessControlException | MessageSource.ExistingExclusiveConsumer | MessageSource.ExistingConsumerPreventsExclusive | MessageSource.QueueDeleted | AMQInvalidArgumentException | MessageSource.ConsumerAccessRefused ex3) {
            final Exception ex;
            final Exception e = ex;
            this._tag2SubscriptionTargetMap.remove(tag);
            throw e;
        }
        return tag;
    }
    
    private boolean unsubscribeConsumer(final AMQShortString consumerTag) {
        if (AMQChannel.LOGGER.isDebugEnabled()) {
            AMQChannel.LOGGER.debug("Unsubscribing consumer '{}' on channel {}", (Object)consumerTag, (Object)this);
        }
        final ConsumerTarget_0_8 target = this._tag2SubscriptionTargetMap.remove(consumerTag);
        if (target != null) {
            target.close();
            return true;
        }
        AMQChannel.LOGGER.warn("Attempt to unsubscribe consumer with tag '" + consumerTag + "' which is not registered.");
        return false;
    }
    
    public void close() {
        this.close(0, null);
    }
    
    public void close(final int cause, final String message) {
        if (!this._closing.compareAndSet(false, true)) {
            return;
        }
        try {
            this.unsubscribeAllConsumers();
            this.setDefaultQueue(null);
            for (final Action<? super AMQChannel> task : this._taskList) {
                task.performAction((Object)this);
            }
            if (this._transaction instanceof LocalTransaction) {
                if (((LocalTransaction)this._transaction).hasOutstandingWork()) {
                    this._connection.incrementTransactionRollbackCounter();
                }
                this._connection.decrementTransactionOpenCounter();
                this._connection.unregisterTransactionTickers(this._transaction);
            }
            this._transaction.rollback();
            this.requeue();
        }
        finally {
            this.dispose();
            final LogMessage operationalLogMessage = (cause == 0) ? ChannelMessages.CLOSE() : ChannelMessages.CLOSE_FORCED((Number)cause, message);
            this.messageWithSubject(operationalLogMessage);
        }
    }
    
    private void messageWithSubject(final LogMessage operationalLogMessage) {
        this.getEventLogger().message(this._logSubject, operationalLogMessage);
    }
    
    private void unsubscribeAllConsumers() {
        if (AMQChannel.LOGGER.isDebugEnabled()) {
            if (!this._tag2SubscriptionTargetMap.isEmpty()) {
                AMQChannel.LOGGER.debug("Unsubscribing all consumers on channel " + this.toString());
            }
            else {
                AMQChannel.LOGGER.debug("No consumers to unsubscribe on channel " + this.toString());
            }
        }
        final Set<AMQShortString> subscriptionTags = new HashSet<AMQShortString>(this._tag2SubscriptionTargetMap.keySet());
        for (final AMQShortString tag : subscriptionTags) {
            this.unsubscribeConsumer(tag);
        }
    }
    
    public void addUnacknowledgedMessage(final MessageInstance entry, final long deliveryTag, final MessageInstanceConsumer consumer, final boolean usesCredit) {
        if (AMQChannel.LOGGER.isDebugEnabled()) {
            AMQChannel.LOGGER.debug(this.debugIdentity() + " Adding unacked message(" + entry.getMessage().toString() + " DT:" + deliveryTag + ") for " + consumer + " on " + entry.getOwningResource().getName());
        }
        this._unacknowledgedMessageMap.add(deliveryTag, entry, consumer, usesCredit);
    }
    
    private String debugIdentity() {
        return this._channelId + this.id;
    }
    
    private void requeue() {
        final Map<Long, MessageConsumerAssociation> copy = new LinkedHashMap<Long, MessageConsumerAssociation>();
        this._unacknowledgedMessageMap.visit(new UnacknowledgedMessageMap.Visitor() {
            @Override
            public boolean callback(final long deliveryTag, final MessageConsumerAssociation messageConsumerPair) {
                copy.put(deliveryTag, messageConsumerPair);
                return false;
            }
            
            @Override
            public void visitComplete() {
            }
        });
        if (!copy.isEmpty() && AMQChannel.LOGGER.isDebugEnabled()) {
            AMQChannel.LOGGER.debug("Requeuing {} unacked messages", (Object)copy.size());
        }
        for (final Map.Entry<Long, MessageConsumerAssociation> entry : copy.entrySet()) {
            final MessageInstance unacked = entry.getValue().getMessageInstance();
            final MessageInstanceConsumer consumer = entry.getValue().getConsumer();
            unacked.setRedelivered();
            this._unacknowledgedMessageMap.remove(entry.getKey(), true);
            unacked.release(consumer);
        }
    }
    
    private void requeue(final long deliveryTag) {
        final MessageConsumerAssociation association = this._unacknowledgedMessageMap.remove(deliveryTag, true);
        if (association != null) {
            final MessageInstance unacked = association.getMessageInstance();
            unacked.setRedelivered();
            unacked.release(association.getConsumer());
        }
        else {
            AMQChannel.LOGGER.warn("Requested requeue of message: {} but no such delivery tag exists.", (Object)deliveryTag);
        }
    }
    
    private boolean isMaxDeliveryCountEnabled(final long deliveryTag) {
        final MessageInstance queueEntry = this._unacknowledgedMessageMap.get(deliveryTag);
        if (queueEntry != null) {
            final int maximumDeliveryCount = queueEntry.getMaximumDeliveryCount();
            return maximumDeliveryCount > 0;
        }
        return false;
    }
    
    private boolean isDeliveredTooManyTimes(final long deliveryTag) {
        final MessageInstance queueEntry = this._unacknowledgedMessageMap.get(deliveryTag);
        if (queueEntry != null) {
            final int maximumDeliveryCount = queueEntry.getMaximumDeliveryCount();
            final int numDeliveries = queueEntry.getDeliveryCount();
            return maximumDeliveryCount != 0 && numDeliveries >= maximumDeliveryCount;
        }
        return false;
    }
    
    private void resend() {
        final Map<Long, MessageConsumerAssociation> msgToRequeue = new LinkedHashMap<Long, MessageConsumerAssociation>();
        final Map<Long, MessageConsumerAssociation> msgToResend = new LinkedHashMap<Long, MessageConsumerAssociation>();
        if (AMQChannel.LOGGER.isDebugEnabled()) {
            AMQChannel.LOGGER.debug("Unacknowledged messages: {}", (Object)this._unacknowledgedMessageMap.size());
        }
        this._unacknowledgedMessageMap.visit(new UnacknowledgedMessageMap.Visitor() {
            @Override
            public boolean callback(final long deliveryTag, final MessageConsumerAssociation association) {
                if (association.getConsumer().isClosed()) {
                    msgToRequeue.put(deliveryTag, association);
                }
                else {
                    msgToResend.put(deliveryTag, association);
                }
                return false;
            }
            
            @Override
            public void visitComplete() {
            }
        });
        for (final Map.Entry<Long, MessageConsumerAssociation> entry : msgToResend.entrySet()) {
            final long deliveryTag = entry.getKey();
            final MessageInstance message = entry.getValue().getMessageInstance();
            final MessageInstanceConsumer consumer = entry.getValue().getConsumer();
            message.setRedelivered();
            if (message.makeAcquisitionUnstealable(consumer)) {
                message.decrementDeliveryCount();
                consumer.getTarget().send(consumer, message, false);
                this._unacknowledgedMessageMap.remove(deliveryTag, false);
            }
            else {
                msgToRequeue.put(deliveryTag, entry.getValue());
            }
        }
        for (final Map.Entry<Long, MessageConsumerAssociation> entry : msgToRequeue.entrySet()) {
            final long deliveryTag = entry.getKey();
            final MessageInstance message = entry.getValue().getMessageInstance();
            final MessageInstanceConsumer consumer = entry.getValue().getConsumer();
            message.decrementDeliveryCount();
            this._unacknowledgedMessageMap.remove(deliveryTag, true);
            message.setRedelivered();
            message.release(consumer);
        }
    }
    
    private UnacknowledgedMessageMap getUnacknowledgedMessageMap() {
        return this._unacknowledgedMessageMap;
    }
    
    private void setSuspended(final boolean suspended) {
        final boolean wasSuspended = this._suspended.getAndSet(suspended);
        if (wasSuspended != suspended) {
            if (!suspended && this._logChannelFlowMessages) {
                this.messageWithSubject(ChannelMessages.FLOW("Started"));
            }
            if (wasSuspended) {
                for (final ConsumerTarget_0_8 s : this.getConsumerTargets()) {
                    for (final MessageInstanceConsumer sub : s.getConsumers()) {
                        sub.externalStateChange();
                    }
                }
            }
            if (suspended && this._logChannelFlowMessages) {
                this.messageWithSubject(ChannelMessages.FLOW("Stopped"));
            }
        }
    }
    
    private void commit(final Runnable immediateAction, final boolean async) {
        if (async && this._transaction instanceof LocalTransaction) {
            ((LocalTransaction)this._transaction).commitAsync((Runnable)new Runnable() {
                @Override
                public void run() {
                    try {
                        immediateAction.run();
                    }
                    finally {
                        AMQChannel.this._connection.incrementTransactionBeginCounter();
                    }
                }
            });
        }
        else {
            this._transaction.commit(immediateAction);
            this._connection.incrementTransactionBeginCounter();
        }
    }
    
    private void rollback(final Runnable postRollbackTask) {
        this._rollingBack = true;
        final boolean requiresSuspend = this._suspended.compareAndSet(false, true);
        try {
            this._transaction.rollback();
        }
        finally {
            this._rollingBack = false;
            this._connection.incrementTransactionRollbackCounter();
            this._connection.incrementTransactionBeginCounter();
        }
        postRollbackTask.run();
        for (final MessageConsumerAssociation association : this._resendList) {
            final MessageInstance messageInstance = association.getMessageInstance();
            final MessageInstanceConsumer consumer = association.getConsumer();
            if (consumer.isClosed()) {
                messageInstance.release(consumer);
            }
            else if (messageInstance.makeAcquisitionUnstealable(consumer) && this._creditManager.useCreditForMessage(association.getSize())) {
                consumer.getTarget().send(consumer, messageInstance, false);
            }
            else {
                messageInstance.release(consumer);
            }
        }
        this._resendList.clear();
        if (requiresSuspend) {
            this._suspended.set(false);
            for (final ConsumerTarget_0_8 target : this.getConsumerTargets()) {
                for (final MessageInstanceConsumer sub : target.getConsumers()) {
                    sub.externalStateChange();
                }
            }
        }
    }
    
    public String toString() {
        return "(" + this._suspended.get() + ", " + this._closing.get() + ", " + this._connection.isClosing() + ") [" + this._connection.toString() + ":" + this._channelId + "]";
    }
    
    public boolean isClosing() {
        return this._closing.get() || this.getConnection().isClosing();
    }
    
    public AMQPConnection_0_8<?> getConnection() {
        return (AMQPConnection_0_8<?>)this._connection;
    }
    
    private void setCredit(final long prefetchSize, final int prefetchCount) {
        if (!this._prefetchLoggedForChannel) {
            this.message(ChannelMessages.PREFETCH_SIZE((Number)prefetchSize, (Number)prefetchCount));
            this._prefetchLoggedForChannel = true;
        }
        if (prefetchCount <= 1 && prefetchSize == 0L) {
            this._logChannelFlowMessages = false;
        }
        final boolean hasCredit = this._creditManager.hasCredit();
        this._creditManager.setCreditLimits(prefetchSize, prefetchCount);
        if (hasCredit != this._creditManager.hasCredit()) {
            this.updateAllConsumerNotifyWorkDesired();
        }
    }
    
    public ClientDeliveryMethod getClientDeliveryMethod() {
        return this._clientDeliveryMethod;
    }
    
    public Subject getSubject() {
        return this._subject;
    }
    
    private boolean hasCurrentMessage() {
        return this._currentMessage != null;
    }
    
    public boolean isChannelFlow() {
        return this._channelFlow;
    }
    
    public synchronized void block() {
        if (this._blockingEntities.add(this) && this._blocking.compareAndSet(false, true)) {
            this.messageWithSubject(ChannelMessages.FLOW_ENFORCED("** All Queues **"));
            this.getConnection().notifyWork((AMQPSession)this);
        }
    }
    
    public synchronized void unblock() {
        if (this._blockingEntities.remove(this) && this._blockingEntities.isEmpty() && this._blocking.compareAndSet(true, false)) {
            this.messageWithSubject(ChannelMessages.FLOW_REMOVED());
            this.getConnection().notifyWork((AMQPSession)this);
        }
    }
    
    public synchronized void block(final Queue<?> queue) {
        if (this._blockingEntities.add(queue) && this._blocking.compareAndSet(false, true)) {
            this.messageWithSubject(ChannelMessages.FLOW_ENFORCED(queue.getName()));
            this.getConnection().notifyWork((AMQPSession)this);
        }
    }
    
    public synchronized void unblock(final Queue<?> queue) {
        if (this._blockingEntities.remove(queue) && this._blockingEntities.isEmpty() && this._blocking.compareAndSet(true, false) && !this.isClosing()) {
            this.messageWithSubject(ChannelMessages.FLOW_REMOVED());
            this.getConnection().notifyWork((AMQPSession)this);
        }
    }
    
    public void transportStateChanged() {
        this.updateAllConsumerNotifyWorkDesired();
        this._creditManager.restoreCredit(0L, 0L);
        AMQChannel.INFINITE_CREDIT_CREDIT_MANAGER.restoreCredit(0L, 0L);
        if (!this._consumersWithPendingWork.isEmpty() && !this.getAMQPConnection().isTransportBlockedForWriting()) {
            this.getAMQPConnection().notifyWork((AMQPSession)this);
        }
    }
    
    void updateAllConsumerNotifyWorkDesired() {
        for (final ConsumerTarget_0_8 target : this._tag2SubscriptionTargetMap.values()) {
            target.updateNotifyWorkDesired();
        }
    }
    
    public Object getConnectionReference() {
        return this.getConnection().getReference();
    }
    
    public int getUnacknowledgedMessageCount() {
        return this.getUnacknowledgedMessageMap().size();
    }
    
    private void sendFlow(final boolean flow) {
        final MethodRegistry methodRegistry = this._connection.getMethodRegistry();
        final AMQMethodBody responseBody = methodRegistry.createChannelFlowBody(flow);
        this._connection.writeFrame(responseBody.generateFrame(this._channelId));
    }
    
    public boolean getBlocking() {
        return this._blocking.get();
    }
    
    public NamedAddressSpace getAddressSpace() {
        return this.getConnection().getAddressSpace();
    }
    
    private void deadLetter(final long deliveryTag) {
        final UnacknowledgedMessageMap unackedMap = this.getUnacknowledgedMessageMap();
        final MessageConsumerAssociation association = unackedMap.remove(deliveryTag, true);
        if (association == null) {
            AMQChannel.LOGGER.warn("No message found, unable to DLQ delivery tag: " + deliveryTag);
        }
        else {
            final MessageInstance messageInstance = association.getMessageInstance();
            final ServerMessage msg = messageInstance.getMessage();
            int requeues = 0;
            if (messageInstance.makeAcquisitionUnstealable(association.getConsumer())) {
                requeues = messageInstance.routeToAlternate((Action)new Action<MessageInstance>() {
                    public void performAction(final MessageInstance requeueEntry) {
                        AMQChannel.this.messageWithSubject(ChannelMessages.DEADLETTERMSG((Number)msg.getMessageNumber(), requeueEntry.getOwningResource().getName()));
                    }
                }, (ServerTransaction)null, (Predicate)null);
            }
            if (requeues == 0) {
                final TransactionLogResource owningResource = messageInstance.getOwningResource();
                if (owningResource instanceof Queue) {
                    final Queue<?> queue = (Queue<?>)owningResource;
                    final MessageDestination alternateBindingDestination = queue.getAlternateBindingDestination();
                    if (alternateBindingDestination == null) {
                        this.messageWithSubject(ChannelMessages.DISCARDMSG_NOALTEXCH((Number)msg.getMessageNumber(), queue.getName(), msg.getInitialRoutingAddress()));
                    }
                    else {
                        this.messageWithSubject(ChannelMessages.DISCARDMSG_NOROUTE((Number)msg.getMessageNumber(), alternateBindingDestination.getName()));
                    }
                }
            }
        }
    }
    
    public void recordFuture(final ListenableFuture<Void> future, final ServerTransaction.Action action) {
        this._unfinishedCommandsQueue.add(new AsyncCommand((ListenableFuture)future, action));
    }
    
    private void sync() {
        if (AMQChannel.LOGGER.isDebugEnabled()) {
            AMQChannel.LOGGER.debug("sync() called on channel " + this.debugIdentity());
        }
        AsyncCommand cmd;
        while ((cmd = this._unfinishedCommandsQueue.poll()) != null) {
            cmd.complete();
        }
        if (this._transaction instanceof LocalTransaction) {
            ((LocalTransaction)this._transaction).sync();
        }
    }
    
    public long getTransactionStartTimeLong() {
        final ServerTransaction serverTransaction = this._transaction;
        if (serverTransaction.isTransactional()) {
            return serverTransaction.getTransactionStartTime();
        }
        return 0L;
    }
    
    public long getTransactionUpdateTimeLong() {
        final ServerTransaction serverTransaction = this._transaction;
        if (serverTransaction.isTransactional()) {
            return serverTransaction.getTransactionUpdateTime();
        }
        return 0L;
    }
    
    public void receiveAccessRequest(final AMQShortString realm, final boolean exclusive, final boolean passive, final boolean active, final boolean write, final boolean read) {
        if (AMQChannel.LOGGER.isDebugEnabled()) {
            AMQChannel.LOGGER.debug("RECV[" + this._channelId + "] AccessRequest[ realm: " + realm + " exclusive: " + exclusive + " passive: " + passive + " active: " + active + " write: " + write + " read: " + read + " ]");
        }
        final MethodRegistry methodRegistry = this._connection.getMethodRegistry();
        if (ProtocolVersion.v0_91.equals(this._connection.getProtocolVersion())) {
            this._connection.sendConnectionClose(503, "AccessRequest not present in AMQP versions other than 0-8, 0-9", this._channelId);
        }
        else {
            final AccessRequestOkBody response = methodRegistry.createAccessRequestOkBody(0);
            this.sync();
            this._connection.writeFrame(response.generateFrame(this._channelId));
        }
    }
    
    public void receiveBasicAck(final long deliveryTag, final boolean multiple) {
        if (AMQChannel.LOGGER.isDebugEnabled()) {
            AMQChannel.LOGGER.debug("RECV[" + this._channelId + "] BasicAck[ deliveryTag: " + deliveryTag + " multiple: " + multiple + " ]");
        }
        final Collection<MessageConsumerAssociation> ackedMessages = this._unacknowledgedMessageMap.acknowledge(deliveryTag, multiple);
        if (!ackedMessages.isEmpty()) {
            final Collection<MessageInstance> messages = (Collection<MessageInstance>)Collections2.transform((Collection)ackedMessages, (Function)AMQChannel.MESSAGE_INSTANCE_FUNCTION);
            this._transaction.dequeue((Collection)messages, (ServerTransaction.Action)new MessageAcknowledgeAction(ackedMessages));
        }
    }
    
    public void receiveBasicCancel(final AMQShortString consumerTag, final boolean nowait) {
        if (AMQChannel.LOGGER.isDebugEnabled()) {
            AMQChannel.LOGGER.debug("RECV[" + this._channelId + "] BasicCancel[ consumerTag: " + consumerTag + " noWait: " + nowait + " ]");
        }
        this.unsubscribeConsumer(consumerTag);
        if (!nowait) {
            final MethodRegistry methodRegistry = this._connection.getMethodRegistry();
            final BasicCancelOkBody cancelOkBody = methodRegistry.createBasicCancelOkBody(consumerTag);
            this.sync();
            this._connection.writeFrame(cancelOkBody.generateFrame(this._channelId));
        }
    }
    
    public void receiveBasicConsume(final AMQShortString queue, final AMQShortString consumerTag, final boolean noLocal, final boolean noAck, final boolean exclusive, final boolean nowait, final FieldTable arguments) {
        if (AMQChannel.LOGGER.isDebugEnabled()) {
            AMQChannel.LOGGER.debug("RECV[" + this._channelId + "] BasicConsume[ queue: " + queue + " consumerTag: " + consumerTag + " noLocal: " + noLocal + " noAck: " + noAck + " exclusive: " + exclusive + " nowait: " + nowait + " arguments: " + arguments + " ]");
        }
        AMQShortString consumerTag2 = consumerTag;
        final NamedAddressSpace vHost = this._connection.getAddressSpace();
        this.sync();
        String queueName = AMQShortString.toString(queue);
        final MessageSource queue2 = (MessageSource)((queueName == null) ? this.getDefaultQueue() : vHost.getAttainedMessageSource(queueName));
        final Collection<MessageSource> sources = new HashSet<MessageSource>();
        if (arguments != null && arguments.get("x-multiqueue") instanceof Collection) {
            for (final Object object : (Collection)arguments.get("x-multiqueue")) {
                String sourceName = String.valueOf(object);
                sourceName = sourceName.trim();
                if (sourceName.length() != 0) {
                    final MessageSource source = vHost.getAttainedMessageSource(sourceName);
                    if (source == null) {
                        sources.clear();
                        break;
                    }
                    sources.add(source);
                }
            }
            queueName = arguments.get("x-multiqueue").toString();
        }
        else if (queue2 != null) {
            sources.add(queue2);
        }
        if (sources.isEmpty()) {
            if (AMQChannel.LOGGER.isDebugEnabled()) {
                AMQChannel.LOGGER.debug("No queue for '" + queueName + "'");
            }
            if (queueName != null) {
                this.closeChannel(404, "No such queue, '" + queueName + "'");
            }
            else {
                this._connection.sendConnectionClose(530, "No queue name provided, no default queue defined.", this._channelId);
            }
        }
        else {
            try {
                consumerTag2 = this.consumeFromSource(consumerTag2, sources, !noAck, arguments, exclusive, noLocal);
                if (!nowait) {
                    final MethodRegistry methodRegistry = this._connection.getMethodRegistry();
                    final AMQMethodBody responseBody = methodRegistry.createBasicConsumeOkBody(consumerTag2);
                    this._connection.writeFrame(responseBody.generateFrame(this._channelId));
                }
            }
            catch (ConsumerTagInUseException cte) {
                this._connection.sendConnectionClose(530, "Non-unique consumer tag, '" + consumerTag2 + "'", this._channelId);
            }
            catch (AMQInvalidArgumentException ise) {
                this._connection.sendConnectionClose(409, ise.getMessage(), this._channelId);
            }
            catch (MessageSource.ExistingExclusiveConsumer e) {
                this._connection.sendConnectionClose(403, "Cannot subscribe to queue '" + queue2.getName() + "' as it already has an existing exclusive consumer", this._channelId);
            }
            catch (MessageSource.ExistingConsumerPreventsExclusive e2) {
                this._connection.sendConnectionClose(403, "Cannot subscribe to queue '" + queue2.getName() + "' exclusively as it already has a consumer", this._channelId);
            }
            catch (AccessControlException e3) {
                this._connection.sendConnectionClose(403, "Cannot subscribe to queue '" + queue2.getName() + "' permission denied", this._channelId);
            }
            catch (MessageSource.ConsumerAccessRefused consumerAccessRefused) {
                this._connection.sendConnectionClose(403, "Cannot subscribe to queue '" + queue2.getName() + "' as it already has an incompatible exclusivity policy", this._channelId);
            }
            catch (MessageSource.QueueDeleted queueDeleted) {
                this._connection.sendConnectionClose(404, "Cannot subscribe to queue '" + queue2.getName() + "' as it has been deleted", this._channelId);
            }
        }
    }
    
    public void receiveBasicGet(final AMQShortString queueName, final boolean noAck) {
        if (AMQChannel.LOGGER.isDebugEnabled()) {
            AMQChannel.LOGGER.debug("RECV[" + this._channelId + "] BasicGet[ queue: " + queueName + " noAck: " + noAck + " ]");
        }
        final NamedAddressSpace vHost = this._connection.getAddressSpace();
        this.sync();
        final MessageSource queue = (MessageSource)((queueName == null) ? this.getDefaultQueue() : vHost.getAttainedMessageSource(queueName.toString()));
        if (queue == null) {
            if (AMQChannel.LOGGER.isDebugEnabled()) {
                AMQChannel.LOGGER.debug("No queue for '" + queueName + "'");
            }
            if (queueName != null) {
                this._connection.sendConnectionClose(404, "No such queue, '" + queueName + "'", this._channelId);
            }
            else {
                this._connection.sendConnectionClose(530, "No queue name provided, no default queue defined.", this._channelId);
            }
        }
        else {
            try {
                if (!this.performGet(queue, !noAck)) {
                    final MethodRegistry methodRegistry = this._connection.getMethodRegistry();
                    final BasicGetEmptyBody responseBody = methodRegistry.createBasicGetEmptyBody(null);
                    this._connection.writeFrame(responseBody.generateFrame(this._channelId));
                }
            }
            catch (AccessControlException e) {
                this._connection.sendConnectionClose(403, e.getMessage(), this._channelId);
            }
            catch (MessageSource.ExistingExclusiveConsumer e2) {
                this._connection.sendConnectionClose(530, "Queue has an exclusive consumer", this._channelId);
            }
            catch (MessageSource.ExistingConsumerPreventsExclusive e3) {
                this._connection.sendConnectionClose(541, "The GET request has been evaluated as an exclusive consumer, this is likely due to a programming error in the Qpid broker", this._channelId);
            }
            catch (MessageSource.ConsumerAccessRefused consumerAccessRefused) {
                this._connection.sendConnectionClose(530, "Queue has an incompatible exclusivity policy", this._channelId);
            }
            catch (MessageSource.QueueDeleted queueDeleted) {
                this._connection.sendConnectionClose(404, "Queue has been deleted", this._channelId);
            }
        }
    }
    
    public void receiveBasicPublish(final AMQShortString exchangeName, final AMQShortString routingKey, final boolean mandatory, final boolean immediate) {
        if (AMQChannel.LOGGER.isDebugEnabled()) {
            AMQChannel.LOGGER.debug("RECV[" + this._channelId + "] BasicPublish[ exchange: " + exchangeName + " routingKey: " + routingKey + " mandatory: " + mandatory + " immediate: " + immediate + " ]");
        }
        final NamedAddressSpace vHost = this._connection.getAddressSpace();
        if (this.blockingTimeoutExceeded()) {
            this.message(ChannelMessages.FLOW_CONTROL_IGNORED());
            this.closeChannel(311, "Channel flow control was requested, but not enforced by sender");
        }
        else {
            MessageDestination destination;
            if (this.isDefaultExchange(exchangeName)) {
                destination = vHost.getDefaultDestination();
            }
            else {
                destination = vHost.getAttainedMessageDestination(exchangeName.toString(), true);
            }
            if (destination == null) {
                this.closeChannel(404, "Unknown exchange name: '" + exchangeName + "'");
            }
            else {
                final MessagePublishInfo info = new MessagePublishInfo(exchangeName, immediate, mandatory, routingKey);
                try {
                    this.setPublishFrame(info, destination);
                }
                catch (AccessControlException e) {
                    this._connection.sendConnectionClose(403, e.getMessage(), this.getChannelId());
                }
            }
        }
    }
    
    private boolean blockingTimeoutExceeded() {
        return this._wireBlockingState && System.currentTimeMillis() - this._blockTime > this._blockingTimeout;
    }
    
    public void receiveBasicQos(final long prefetchSize, final int prefetchCount, final boolean global) {
        if (AMQChannel.LOGGER.isDebugEnabled()) {
            AMQChannel.LOGGER.debug("RECV[" + this._channelId + "] BasicQos[ prefetchSize: " + prefetchSize + " prefetchCount: " + prefetchCount + " global: " + global + " ]");
        }
        this.sync();
        this.setCredit(prefetchSize, prefetchCount);
        final MethodRegistry methodRegistry = this._connection.getMethodRegistry();
        final AMQMethodBody responseBody = methodRegistry.createBasicQosOkBody();
        this._connection.writeFrame(responseBody.generateFrame(this.getChannelId()));
    }
    
    public void receiveBasicRecover(final boolean requeue, final boolean sync) {
        if (AMQChannel.LOGGER.isDebugEnabled()) {
            AMQChannel.LOGGER.debug("RECV[" + this._channelId + "] BasicRecover[ requeue: " + requeue + " sync: " + sync + " ]");
        }
        if (requeue) {
            this.requeue();
        }
        else {
            this.resend();
        }
        if (sync) {
            final MethodRegistry methodRegistry = this._connection.getMethodRegistry();
            final AMQMethodBody recoverOk = methodRegistry.createBasicRecoverSyncOkBody();
            this.sync();
            this._connection.writeFrame(recoverOk.generateFrame(this.getChannelId()));
        }
    }
    
    public void receiveBasicReject(final long deliveryTag, final boolean requeue) {
        if (AMQChannel.LOGGER.isDebugEnabled()) {
            AMQChannel.LOGGER.debug("RECV[" + this._channelId + "] BasicReject[ deliveryTag: " + deliveryTag + " requeue: " + requeue + " ]");
        }
        final MessageInstance message = this.getUnacknowledgedMessageMap().get(deliveryTag);
        if (message == null) {
            AMQChannel.LOGGER.warn("Dropping reject request as message is null for tag:" + deliveryTag);
        }
        else if (message.getMessage() == null) {
            AMQChannel.LOGGER.warn("Message has already been purged, unable to Reject.");
        }
        else {
            if (AMQChannel.LOGGER.isDebugEnabled()) {
                AMQChannel.LOGGER.debug("Rejecting: DT:" + deliveryTag + "-" + message.getMessage() + ": Requeue:" + requeue + " on channel:" + this.debugIdentity());
            }
            if (requeue) {
                message.decrementDeliveryCount();
                this.requeue(deliveryTag);
            }
            else {
                final boolean maxDeliveryCountEnabled = this.isMaxDeliveryCountEnabled(deliveryTag);
                if (AMQChannel.LOGGER.isDebugEnabled()) {
                    AMQChannel.LOGGER.debug("maxDeliveryCountEnabled: " + maxDeliveryCountEnabled + " deliveryTag " + deliveryTag);
                }
                if (maxDeliveryCountEnabled) {
                    final boolean deliveredTooManyTimes = this.isDeliveredTooManyTimes(deliveryTag);
                    if (AMQChannel.LOGGER.isDebugEnabled()) {
                        AMQChannel.LOGGER.debug("deliveredTooManyTimes: " + deliveredTooManyTimes + " deliveryTag " + deliveryTag);
                    }
                    if (deliveredTooManyTimes) {
                        this.deadLetter(deliveryTag);
                    }
                    else {
                        message.incrementDeliveryCount();
                    }
                }
                else {
                    this.requeue(deliveryTag);
                }
            }
        }
    }
    
    public void receiveChannelClose(final int replyCode, final AMQShortString replyText, final int classId, final int methodId) {
        if (AMQChannel.LOGGER.isDebugEnabled()) {
            AMQChannel.LOGGER.debug("RECV[" + this._channelId + "] ChannelClose[ replyCode: " + replyCode + " replyText: " + replyText + " classId: " + classId + " methodId: " + methodId + " ]");
        }
        this.sync();
        this._connection.closeChannel(this);
        this._connection.writeFrame(new AMQFrame(this.getChannelId(), this._connection.getMethodRegistry().createChannelCloseOkBody()));
    }
    
    public void receiveChannelCloseOk() {
        if (AMQChannel.LOGGER.isDebugEnabled()) {
            AMQChannel.LOGGER.debug("RECV[" + this._channelId + "] ChannelCloseOk");
        }
        this._connection.closeChannelOk(this.getChannelId());
    }
    
    public void receiveMessageContent(final QpidByteBuffer data) {
        if (AMQChannel.LOGGER.isDebugEnabled()) {
            AMQChannel.LOGGER.debug("RECV[" + this._channelId + "] MessageContent[ data: " + Functions.hex(data, this._connection.getBinaryDataLimit()) + " ] ");
        }
        if (this.hasCurrentMessage()) {
            this.publishContentBody(new ContentBody(data));
        }
        else {
            this._connection.sendConnectionClose(503, "Attempt to send a content header without first sending a publish frame", this._channelId);
        }
    }
    
    public void receiveMessageHeader(final BasicContentHeaderProperties properties, final long bodySize) {
        if (AMQChannel.LOGGER.isDebugEnabled()) {
            AMQChannel.LOGGER.debug("RECV[" + this._channelId + "] MessageHeader[ properties: {" + properties + "} bodySize: " + bodySize + " ]");
        }
        if (this.hasCurrentMessage()) {
            if (bodySize > this._connection.getMaxMessageSize()) {
                properties.dispose();
                this.closeChannel(311, "Message size of " + bodySize + " greater than allowed maximum of " + this._connection.getMaxMessageSize());
            }
            else if (!this._forceMessageValidation || properties.checkValid()) {
                this.publishContentHeader(new ContentHeaderBody(properties, bodySize));
            }
            else {
                properties.dispose();
                this._connection.sendConnectionClose(501, "Attempt to send a malformed content header", this._channelId);
            }
        }
        else {
            properties.dispose();
            this._connection.sendConnectionClose(503, "Attempt to send a content header without first sending a publish frame", this._channelId);
        }
    }
    
    public boolean ignoreAllButCloseOk() {
        return this._connection.ignoreAllButCloseOk() || this._connection.channelAwaitingClosure(this._channelId);
    }
    
    public void receiveBasicNack(final long deliveryTag, final boolean multiple, final boolean requeue) {
        if (AMQChannel.LOGGER.isDebugEnabled()) {
            AMQChannel.LOGGER.debug("RECV[" + this._channelId + "] BasicNack[ deliveryTag: " + deliveryTag + " multiple: " + multiple + " requeue: " + requeue + " ]");
        }
        final Map<Long, MessageConsumerAssociation> nackedMessageMap = new LinkedHashMap<Long, MessageConsumerAssociation>();
        this._unacknowledgedMessageMap.collect(deliveryTag, multiple, nackedMessageMap);
        for (final MessageConsumerAssociation unackedMessageConsumerAssociation : nackedMessageMap.values()) {
            if (unackedMessageConsumerAssociation == null) {
                AMQChannel.LOGGER.warn("Ignoring nack request as message is null for tag:" + deliveryTag);
            }
            else {
                final MessageInstance message = unackedMessageConsumerAssociation.getMessageInstance();
                if (message.getMessage() == null) {
                    AMQChannel.LOGGER.warn("Message has already been purged, unable to nack.");
                }
                else {
                    if (AMQChannel.LOGGER.isDebugEnabled()) {
                        AMQChannel.LOGGER.debug("Nack-ing: DT:" + deliveryTag + "-" + message.getMessage() + ": Requeue:" + requeue + " on channel:" + this.debugIdentity());
                    }
                    if (requeue) {
                        message.decrementDeliveryCount();
                        this.requeue(deliveryTag);
                    }
                    else {
                        message.reject(unackedMessageConsumerAssociation.getConsumer());
                        final boolean maxDeliveryCountEnabled = this.isMaxDeliveryCountEnabled(deliveryTag);
                        if (AMQChannel.LOGGER.isDebugEnabled()) {
                            AMQChannel.LOGGER.debug("maxDeliveryCountEnabled: " + maxDeliveryCountEnabled + " deliveryTag " + deliveryTag);
                        }
                        if (maxDeliveryCountEnabled) {
                            final boolean deliveredTooManyTimes = this.isDeliveredTooManyTimes(deliveryTag);
                            if (AMQChannel.LOGGER.isDebugEnabled()) {
                                AMQChannel.LOGGER.debug("deliveredTooManyTimes: " + deliveredTooManyTimes + " deliveryTag " + deliveryTag);
                            }
                            if (deliveredTooManyTimes) {
                                this.deadLetter(deliveryTag);
                            }
                            else {
                                message.incrementDeliveryCount();
                                message.release(unackedMessageConsumerAssociation.getConsumer());
                            }
                        }
                        else {
                            this.requeue(deliveryTag);
                        }
                    }
                }
            }
        }
    }
    
    public void receiveChannelFlow(final boolean active) {
        if (AMQChannel.LOGGER.isDebugEnabled()) {
            AMQChannel.LOGGER.debug("RECV[" + this._channelId + "] ChannelFlow[ active: " + active + " ]");
        }
        this.sync();
        if (this._channelFlow != active) {
            this._channelFlow = active;
            this.updateAllConsumerNotifyWorkDesired();
        }
        this.setSuspended(!active);
        final MethodRegistry methodRegistry = this._connection.getMethodRegistry();
        final AMQMethodBody responseBody = methodRegistry.createChannelFlowOkBody(active);
        this._connection.writeFrame(responseBody.generateFrame(this.getChannelId()));
    }
    
    public void receiveChannelFlowOk(final boolean active) {
        if (AMQChannel.LOGGER.isDebugEnabled()) {
            AMQChannel.LOGGER.debug("RECV[" + this._channelId + "] ChannelFlowOk[ active: " + active + " ]");
        }
    }
    
    public void receiveExchangeBound(final AMQShortString exchangeName, final AMQShortString routingKey, final AMQShortString queueName) {
        if (AMQChannel.LOGGER.isDebugEnabled()) {
            AMQChannel.LOGGER.debug("RECV[" + this._channelId + "] ExchangeBound[ exchange: " + exchangeName + " routingKey: " + routingKey + " queue: " + queueName + " ]");
        }
        final NamedAddressSpace virtualHost = this._connection.getAddressSpace();
        final MethodRegistry methodRegistry = this._connection.getMethodRegistry();
        this.sync();
        int replyCode;
        String replyText;
        if (this.isDefaultExchange(exchangeName)) {
            if (routingKey == null) {
                if (queueName == null) {
                    replyCode = (virtualHost.hasMessageSources() ? 0 : 3);
                    replyText = null;
                }
                else {
                    final MessageSource queue = virtualHost.getAttainedMessageSource(queueName.toString());
                    if (queue == null) {
                        replyCode = 2;
                        replyText = "Queue '" + queueName + "' not found";
                    }
                    else {
                        replyCode = 0;
                        replyText = null;
                    }
                }
            }
            else if (queueName == null) {
                replyCode = ((virtualHost.getAttainedMessageDestination(routingKey.toString(), false) instanceof Queue) ? 0 : 5);
                replyText = null;
            }
            else {
                final MessageDestination destination = virtualHost.getAttainedMessageDestination(queueName.toString(), false);
                final Queue<?> queue2 = (destination instanceof Queue) ? destination : null;
                if (queue2 == null) {
                    replyCode = 2;
                    replyText = "Queue '" + queueName + "' not found";
                }
                else {
                    replyCode = (queueName.equals(routingKey) ? 0 : 6);
                    replyText = null;
                }
            }
        }
        else {
            final MessageDestination destination = this.getAddressSpace().getAttainedMessageDestination(exchangeName.toString(), true);
            if (!(destination instanceof Exchange)) {
                replyCode = 1;
                replyText = "Exchange '" + exchangeName + "' not found";
            }
            else if (routingKey == null) {
                final Exchange<?> exchange = (Exchange<?>)destination;
                if (queueName == null) {
                    if (exchange.hasBindings()) {
                        replyCode = 0;
                        replyText = null;
                    }
                    else {
                        replyCode = 3;
                        replyText = null;
                    }
                }
                else {
                    final Queue<?> queue3 = this.getQueue(queueName.toString());
                    if (queue3 == null) {
                        replyCode = 2;
                        replyText = "Queue '" + queueName + "' not found";
                    }
                    else if (exchange.isBound((Queue)queue3)) {
                        replyCode = 0;
                        replyText = null;
                    }
                    else {
                        replyCode = 4;
                        replyText = "Queue '" + queueName + "' not bound to exchange '" + exchangeName + "'";
                    }
                }
            }
            else if (queueName != null) {
                final Exchange<?> exchange = (Exchange<?>)destination;
                final Queue<?> queue3 = this.getQueue(queueName.toString());
                if (queue3 == null) {
                    replyCode = 2;
                    replyText = "Queue '" + queueName + "' not found";
                }
                else {
                    final String bindingKey = (routingKey == null) ? null : routingKey.toString();
                    if (exchange.isBound(bindingKey, (Queue)queue3)) {
                        replyCode = 0;
                        replyText = null;
                    }
                    else {
                        replyCode = 6;
                        replyText = "Queue '" + queueName + "' not bound with routing key '" + routingKey + "' to exchange '" + exchangeName + "'";
                    }
                }
            }
            else {
                final Exchange<?> exchange = (Exchange<?>)destination;
                if (exchange.isBound((routingKey == null) ? "" : routingKey.toString())) {
                    replyCode = 0;
                    replyText = null;
                }
                else {
                    replyCode = 5;
                    replyText = "No queue bound with routing key '" + routingKey + "' to exchange '" + exchangeName + "'";
                }
            }
        }
        final ExchangeBoundOkBody exchangeBoundOkBody = methodRegistry.createExchangeBoundOkBody(replyCode, AMQShortString.validValueOf(replyText));
        this._connection.writeFrame(exchangeBoundOkBody.generateFrame(this.getChannelId()));
    }
    
    public void receiveExchangeDeclare(final AMQShortString exchangeName, final AMQShortString type, final boolean passive, final boolean durable, final boolean autoDelete, final boolean internal, final boolean nowait, final FieldTable arguments) {
        if (AMQChannel.LOGGER.isDebugEnabled()) {
            AMQChannel.LOGGER.debug("RECV[" + this._channelId + "] ExchangeDeclare[ exchange: " + exchangeName + " type: " + type + " passive: " + passive + " durable: " + durable + " autoDelete: " + autoDelete + " internal: " + internal + " nowait: " + nowait + " arguments: " + arguments + " ]");
        }
        final MethodRegistry methodRegistry = this._connection.getMethodRegistry();
        final AMQMethodBody declareOkBody = methodRegistry.createExchangeDeclareOkBody();
        final NamedAddressSpace virtualHost = this._connection.getAddressSpace();
        if (this.isDefaultExchange(exchangeName)) {
            if (!AMQShortString.createAMQShortString("direct").equals(type)) {
                this._connection.sendConnectionClose(530, "Attempt to redeclare default exchange:  of type direct to " + type + ".", this.getChannelId());
            }
            else if (!nowait) {
                this.sync();
                this._connection.writeFrame(declareOkBody.generateFrame(this.getChannelId()));
            }
        }
        else if (passive) {
            final Exchange<?> exchange = this.getExchange(exchangeName.toString());
            if (exchange == null) {
                this.closeChannel(404, "Unknown exchange: '" + exchangeName + "'");
            }
            else if (type != null && type.length() != 0 && !exchange.getType().equals(type.toString())) {
                this._connection.sendConnectionClose(530, "Attempt to redeclare exchange: '" + exchangeName + "' of type " + exchange.getType() + " to " + type + ".", this.getChannelId());
            }
            else if (!nowait) {
                this.sync();
                this._connection.writeFrame(declareOkBody.generateFrame(this.getChannelId()));
            }
        }
        else {
            final String name = exchangeName.toString();
            final String typeString = (type == null) ? null : type.toString();
            try {
                final Map<String, Object> attributes = new HashMap<String, Object>();
                if (arguments != null) {
                    attributes.putAll(FieldTable.convertToMap(arguments));
                }
                attributes.put("name", name);
                attributes.put("type", typeString);
                attributes.put("durable", durable);
                attributes.put("lifetimePolicy", autoDelete ? LifetimePolicy.DELETE_ON_NO_LINKS : LifetimePolicy.PERMANENT);
                final Object alternateExchange = attributes.remove("alternateExchange");
                if (alternateExchange != null) {
                    final String alternateExchangeName = String.valueOf(alternateExchange);
                    this.validateAlternateExchangeIsNotQueue(virtualHost, alternateExchangeName);
                    attributes.put("alternateBinding", Collections.singletonMap("destination", alternateExchangeName));
                }
                this.validateAndSanitizeExchangeDeclareArguments(attributes);
                final Exchange<?> exchange = (Exchange<?>)virtualHost.createMessageDestination((Class)Exchange.class, (Map)attributes);
                if (!nowait) {
                    this.sync();
                    this._connection.writeFrame(declareOkBody.generateFrame(this.getChannelId()));
                }
            }
            catch (ReservedExchangeNameException e6) {
                final Exchange existing = this.getExchange(name);
                if (existing == null || !existing.getType().equals(typeString)) {
                    this._connection.sendConnectionClose(530, "Attempt to declare exchange: '" + exchangeName + "' which begins with reserved prefix.", this.getChannelId());
                }
                else if (!nowait) {
                    this.sync();
                    this._connection.writeFrame(declareOkBody.generateFrame(this.getChannelId()));
                }
            }
            catch (AbstractConfiguredObject.DuplicateNameException e) {
                final Exchange<?> exchange = (Exchange<?>)e.getExisting();
                if (!exchange.getType().equals(typeString)) {
                    this._connection.sendConnectionClose(530, "Attempt to redeclare exchange: '" + exchangeName + "' of type " + exchange.getType() + " to " + type + ".", this.getChannelId());
                }
                else if (!nowait) {
                    this.sync();
                    this._connection.writeFrame(declareOkBody.generateFrame(this.getChannelId()));
                }
            }
            catch (NoFactoryForTypeException e2) {
                this._connection.sendConnectionClose(503, "Unknown exchange type '" + e2.getType() + "' for exchange '" + exchangeName + "'", this.getChannelId());
            }
            catch (AccessControlException e3) {
                this._connection.sendConnectionClose(403, e3.getMessage(), this.getChannelId());
            }
            catch (UnknownAlternateBindingException e4) {
                final String message = String.format("Unknown alternate destination '%s'", e4.getAlternateBindingName());
                this._connection.sendConnectionClose(404, message, this.getChannelId());
            }
            catch (IllegalArgumentException | IllegalConfigurationException ex2) {
                final RuntimeException ex;
                final RuntimeException e5 = ex;
                this._connection.sendConnectionClose(542, "Error creating exchange '" + exchangeName + "': " + e5.getMessage(), this.getChannelId());
            }
        }
    }
    
    private void validateAndSanitizeExchangeDeclareArguments(final Map<String, Object> attributes) {
        final ConfiguredObjectTypeRegistry typeRegistry = this.getModel().getTypeRegistry();
        final List<ConfiguredObjectAttribute<?, ?>> types = new ArrayList<ConfiguredObjectAttribute<?, ?>>(typeRegistry.getAttributeTypes((Class)Exchange.class).values());
        typeRegistry.getTypeSpecialisations((Class)Exchange.class).forEach(type -> types.addAll(typeRegistry.getTypeSpecificAttributes(type)));
        final Set<String> unsupported = attributes.keySet().stream().filter(name -> types.stream().noneMatch(a -> Objects.equals(name, a.getName()) && !a.isDerived())).collect((Collector<? super Object, ?, Set<String>>)Collectors.toSet());
        if (!unsupported.isEmpty()) {
            final Exchange.BehaviourOnUnknownDeclareArgument unknownArgumentBehaviour = (Exchange.BehaviourOnUnknownDeclareArgument)this.getConnection().getContextValue((Class)Exchange.BehaviourOnUnknownDeclareArgument.class, "exchange.behaviourOnUnknownDeclareArgument");
            switch (unknownArgumentBehaviour) {
                case LOG: {
                    AMQChannel.LOGGER.warn("Unsupported exchange declare arguments : {}", (Object)String.join(",", unsupported));
                }
                case IGNORE: {
                    attributes.keySet().removeAll(unsupported);
                    break;
                }
                default: {
                    throw new IllegalArgumentException(String.format("Unsupported exchange declare arguments : %s", String.join(",", unsupported)));
                }
            }
        }
    }
    
    private void validateAlternateExchangeIsNotQueue(final NamedAddressSpace addressSpace, final String alternateExchangeName) {
        final MessageDestination alternateMessageDestination = addressSpace.getAttainedMessageDestination(alternateExchangeName, false);
        if (alternateMessageDestination != null && !(alternateMessageDestination instanceof Exchange)) {
            throw new IllegalConfigurationException(String.format("Alternate exchange '%s' is not a destination of type 'exchange'.", alternateExchangeName));
        }
    }
    
    public void receiveExchangeDelete(final AMQShortString exchangeStr, final boolean ifUnused, final boolean nowait) {
        if (AMQChannel.LOGGER.isDebugEnabled()) {
            AMQChannel.LOGGER.debug("RECV[" + this._channelId + "] ExchangeDelete[ exchange: " + exchangeStr + " ifUnused: " + ifUnused + " nowait: " + nowait + " ]");
        }
        final NamedAddressSpace virtualHost = this._connection.getAddressSpace();
        this.sync();
        if (this.isDefaultExchange(exchangeStr)) {
            this._connection.sendConnectionClose(530, "Default Exchange cannot be deleted", this.getChannelId());
        }
        else {
            final String exchangeName = exchangeStr.toString();
            final Exchange<?> exchange = this.getExchange(exchangeName);
            if (exchange == null) {
                this.closeChannel(404, "No such exchange: '" + exchangeStr + "'");
            }
            else if (ifUnused && exchange.hasBindings()) {
                this.closeChannel(406, "Exchange has bindings");
            }
            else {
                try {
                    exchange.delete();
                    if (!nowait) {
                        final ExchangeDeleteOkBody responseBody = this._connection.getMethodRegistry().createExchangeDeleteOkBody();
                        this._connection.writeFrame(responseBody.generateFrame(this.getChannelId()));
                    }
                }
                catch (MessageDestinationIsAlternateException e2) {
                    this.closeChannel(530, "Exchange in use as an alternate binding destination");
                }
                catch (RequiredExchangeException e3) {
                    this.closeChannel(530, "Exchange '" + exchangeStr + "' cannot be deleted");
                }
                catch (AccessControlException e) {
                    this._connection.sendConnectionClose(403, e.getMessage(), this.getChannelId());
                }
            }
        }
    }
    
    public void receiveQueueBind(final AMQShortString queueName, final AMQShortString exchange, AMQShortString bindingKey, final boolean nowait, final FieldTable argumentsTable) {
        if (AMQChannel.LOGGER.isDebugEnabled()) {
            AMQChannel.LOGGER.debug("RECV[" + this._channelId + "] QueueBind[ queue: " + queueName + " exchange: " + exchange + " bindingKey: " + bindingKey + " nowait: " + nowait + " arguments: " + argumentsTable + " ]");
        }
        final NamedAddressSpace virtualHost = this._connection.getAddressSpace();
        Queue<?> queue;
        if (queueName == null) {
            queue = this.getDefaultQueue();
            if (queue != null && bindingKey == null) {
                bindingKey = AMQShortString.valueOf(queue.getName());
            }
        }
        else {
            queue = this.getQueue(queueName.toString());
        }
        if (queue == null) {
            final String message = (queueName == null) ? "No default queue defined on channel and queue was null" : ("Queue " + queueName + " does not exist.");
            this.closeChannel(404, message);
        }
        else if (this.isDefaultExchange(exchange)) {
            this._connection.sendConnectionClose(530, "Cannot bind the queue '" + queueName + "' to the default exchange", this.getChannelId());
        }
        else {
            final String exchangeName = exchange.toString();
            final Exchange<?> exch = this.getExchange(exchangeName);
            if (exch == null) {
                this.closeChannel(404, "Exchange '" + exchangeName + "' does not exist.");
            }
            else {
                try {
                    final Map<String, Object> arguments = FieldTable.convertToMap(argumentsTable);
                    final String bindingKeyStr = (bindingKey == null) ? "" : AMQShortString.toString(bindingKey);
                    if (!exch.isBound(bindingKeyStr, (Map)arguments, (Queue)queue)) {
                        try {
                            if (!exch.addBinding(bindingKeyStr, (Queue)queue, (Map)arguments) && "topic".equals(exch.getType())) {
                                exch.replaceBinding(bindingKeyStr, (Queue)queue, (Map)arguments);
                            }
                        }
                        catch (AMQInvalidArgumentException e) {
                            this._connection.sendConnectionClose(409, String.format("Cannot bind queue '%s' to exchange '%s' due to invalid argument : %s", queueName, exch.getName(), e.getMessage()), this.getChannelId());
                        }
                    }
                    if (AMQChannel.LOGGER.isDebugEnabled()) {
                        AMQChannel.LOGGER.debug("Binding queue " + queue + " to exchange " + exch + " with routing key " + bindingKeyStr);
                    }
                    if (!nowait) {
                        this.sync();
                        final MethodRegistry methodRegistry = this._connection.getMethodRegistry();
                        final AMQMethodBody responseBody = methodRegistry.createQueueBindOkBody();
                        this._connection.writeFrame(responseBody.generateFrame(this.getChannelId()));
                    }
                }
                catch (AccessControlException e2) {
                    this._connection.sendConnectionClose(403, e2.getMessage(), this.getChannelId());
                }
            }
        }
    }
    
    public void receiveQueueDeclare(final AMQShortString queueStr, final boolean passive, final boolean durable, final boolean exclusive, final boolean autoDelete, final boolean nowait, final FieldTable arguments) {
        if (AMQChannel.LOGGER.isDebugEnabled()) {
            AMQChannel.LOGGER.debug("RECV[" + this._channelId + "] QueueDeclare[ queue: " + queueStr + " passive: " + passive + " durable: " + durable + " exclusive: " + exclusive + " autoDelete: " + autoDelete + " nowait: " + nowait + " arguments: " + arguments + " ]");
        }
        final NamedAddressSpace virtualHost = this._connection.getAddressSpace();
        AMQShortString queueName;
        if (queueStr == null || queueStr.length() == 0) {
            queueName = AMQShortString.createAMQShortString("tmp_" + UUID.randomUUID());
        }
        else {
            queueName = queueStr;
        }
        if (passive) {
            final Queue<?> queue = this.getQueue(queueName.toString());
            if (queue == null) {
                this.closeChannel(404, "Queue: '" + queueName + "' not found on VirtualHost '" + virtualHost.getName() + "'.");
            }
            else if (!queue.verifySessionAccess((AMQPSession)this)) {
                this._connection.sendConnectionClose(530, "Queue '" + queue.getName() + "' is exclusive, but not created on this Connection.", this.getChannelId());
            }
            else {
                this.setDefaultQueue(queue);
                if (!nowait) {
                    this.sync();
                    final MethodRegistry methodRegistry = this._connection.getMethodRegistry();
                    final QueueDeclareOkBody responseBody = methodRegistry.createQueueDeclareOkBody(queueName, queue.getQueueDepthMessages(), queue.getConsumerCount());
                    this._connection.writeFrame(responseBody.generateFrame(this.getChannelId()));
                    if (AMQChannel.LOGGER.isDebugEnabled()) {
                        AMQChannel.LOGGER.debug("Queue " + queueName + " declared successfully");
                    }
                }
            }
        }
        else {
            try {
                final String queueNameString = AMQShortString.toString(queueName);
                final Map<String, Object> wireArguments = FieldTable.convertToMap(arguments);
                final Object alternateExchange = wireArguments.get("alternateExchange");
                if (alternateExchange != null) {
                    final String alternateExchangeName = String.valueOf(alternateExchange);
                    this.validateAlternateExchangeIsNotQueue(virtualHost, alternateExchangeName);
                }
                final Queue.BehaviourOnUnknownDeclareArgument unknownArgumentBehaviour = (Queue.BehaviourOnUnknownDeclareArgument)this.getConnection().getContextValue((Class)Queue.BehaviourOnUnknownDeclareArgument.class, "queue.behaviourOnUnknownDeclareArgument");
                final Map<String, Object> attributes = (Map<String, Object>)QueueArgumentsConverter.convertWireArgsToModel(queueNameString, (Map)wireArguments, this.getModel(), unknownArgumentBehaviour);
                attributes.put("name", queueNameString);
                attributes.put("durable", durable);
                LifetimePolicy lifetimePolicy;
                ExclusivityPolicy exclusivityPolicy;
                if (exclusive) {
                    lifetimePolicy = (autoDelete ? LifetimePolicy.DELETE_ON_NO_OUTBOUND_LINKS : (durable ? LifetimePolicy.PERMANENT : LifetimePolicy.DELETE_ON_CONNECTION_CLOSE));
                    exclusivityPolicy = (durable ? ExclusivityPolicy.CONTAINER : ExclusivityPolicy.CONNECTION);
                }
                else {
                    lifetimePolicy = (autoDelete ? LifetimePolicy.DELETE_ON_NO_OUTBOUND_LINKS : LifetimePolicy.PERMANENT);
                    exclusivityPolicy = ExclusivityPolicy.NONE;
                }
                if (!attributes.containsKey("exclusive")) {
                    attributes.put("exclusive", exclusivityPolicy);
                }
                if (!attributes.containsKey("lifetimePolicy")) {
                    attributes.put("lifetimePolicy", lifetimePolicy);
                }
                final Queue<?> queue = (Queue<?>)virtualHost.createMessageSource((Class)Queue.class, (Map)attributes);
                this.setDefaultQueue(queue);
                if (!nowait) {
                    this.sync();
                    final MethodRegistry methodRegistry2 = this._connection.getMethodRegistry();
                    final QueueDeclareOkBody responseBody2 = methodRegistry2.createQueueDeclareOkBody(queueName, queue.getQueueDepthMessages(), queue.getConsumerCount());
                    this._connection.writeFrame(responseBody2.generateFrame(this.getChannelId()));
                    if (AMQChannel.LOGGER.isDebugEnabled()) {
                        AMQChannel.LOGGER.debug("Queue " + queueName + " declared successfully");
                    }
                }
            }
            catch (AbstractConfiguredObject.DuplicateNameException qe) {
                final Queue<?> queue = (Queue<?>)qe.getExisting();
                if (!queue.verifySessionAccess((AMQPSession)this)) {
                    this._connection.sendConnectionClose(530, "Queue '" + queue.getName() + "' is exclusive, but not created on this Connection.", this.getChannelId());
                }
                else if (queue.isExclusive() != exclusive) {
                    this.closeChannel(405, "Cannot re-declare queue '" + queue.getName() + "' with different exclusivity (was: " + queue.isExclusive() + " requested " + exclusive + ")");
                }
                else if ((autoDelete && queue.getLifetimePolicy() == LifetimePolicy.PERMANENT) || (!autoDelete && queue.getLifetimePolicy() != ((exclusive && !durable) ? LifetimePolicy.DELETE_ON_CONNECTION_CLOSE : LifetimePolicy.PERMANENT))) {
                    this.closeChannel(405, "Cannot re-declare queue '" + queue.getName() + "' with different lifetime policy (was: " + queue.getLifetimePolicy() + " requested autodelete: " + autoDelete + ")");
                }
                else {
                    this.setDefaultQueue(queue);
                    if (!nowait) {
                        this.sync();
                        final MethodRegistry methodRegistry3 = this._connection.getMethodRegistry();
                        final QueueDeclareOkBody responseBody3 = methodRegistry3.createQueueDeclareOkBody(queueName, queue.getQueueDepthMessages(), queue.getConsumerCount());
                        this._connection.writeFrame(responseBody3.generateFrame(this.getChannelId()));
                        if (AMQChannel.LOGGER.isDebugEnabled()) {
                            AMQChannel.LOGGER.debug("Queue " + queueName + " declared successfully");
                        }
                    }
                }
            }
            catch (AccessControlException e) {
                this._connection.sendConnectionClose(403, e.getMessage(), this.getChannelId());
            }
            catch (UnknownAlternateBindingException e2) {
                final String message = String.format("Unknown alternate destination: '%s'", e2.getAlternateBindingName());
                this._connection.sendConnectionClose(404, message, this.getChannelId());
            }
            catch (IllegalArgumentException | IllegalConfigurationException ex2) {
                final RuntimeException ex;
                final RuntimeException e3 = ex;
                final String message = String.format("Error creating queue '%s': %s", queueName, e3.getMessage());
                this._connection.sendConnectionClose(542, message, this.getChannelId());
            }
        }
    }
    
    public void receiveQueueDelete(final AMQShortString queueName, final boolean ifUnused, final boolean ifEmpty, final boolean nowait) {
        if (AMQChannel.LOGGER.isDebugEnabled()) {
            AMQChannel.LOGGER.debug("RECV[" + this._channelId + "] QueueDelete[ queue: " + queueName + " ifUnused: " + ifUnused + " ifEmpty: " + ifEmpty + " nowait: " + nowait + " ]");
        }
        final NamedAddressSpace virtualHost = this._connection.getAddressSpace();
        this.sync();
        Queue<?> queue;
        if (queueName == null) {
            queue = this.getDefaultQueue();
        }
        else {
            queue = this.getQueue(queueName.toString());
        }
        if (queue == null) {
            this.closeChannel(404, "Queue '" + queueName + "' does not exist.");
        }
        else if (ifEmpty && !queue.isEmpty()) {
            this.closeChannel(406, "Queue: '" + queueName + "' is not empty.");
        }
        else if (ifUnused && !queue.isUnused()) {
            this.closeChannel(406, "Queue: '" + queueName + "' is still used.");
        }
        else if (!queue.verifySessionAccess((AMQPSession)this)) {
            this._connection.sendConnectionClose(530, "Queue '" + queue.getName() + "' is exclusive, but not created on this Connection.", this.getChannelId());
        }
        else {
            try {
                final int purged = queue.deleteAndReturnCount();
                if (!nowait || this._connection.isSendQueueDeleteOkRegardless()) {
                    final MethodRegistry methodRegistry = this._connection.getMethodRegistry();
                    final QueueDeleteOkBody responseBody = methodRegistry.createQueueDeleteOkBody(purged);
                    this._connection.writeFrame(responseBody.generateFrame(this.getChannelId()));
                }
            }
            catch (AccessControlException e) {
                this._connection.sendConnectionClose(403, e.getMessage(), this.getChannelId());
            }
        }
    }
    
    public void receiveQueuePurge(final AMQShortString queueName, final boolean nowait) {
        if (AMQChannel.LOGGER.isDebugEnabled()) {
            AMQChannel.LOGGER.debug("RECV[" + this._channelId + "] QueuePurge[ queue: " + queueName + " nowait: " + nowait + " ]");
        }
        final NamedAddressSpace virtualHost = this._connection.getAddressSpace();
        Queue<?> queue = null;
        if (queueName == null && (queue = this.getDefaultQueue()) == null) {
            this._connection.sendConnectionClose(530, "No queue specified.", this.getChannelId());
        }
        else if (queueName != null && (queue = this.getQueue(queueName.toString())) == null) {
            this.closeChannel(404, "Queue '" + queueName + "' does not exist.");
        }
        else if (!queue.verifySessionAccess((AMQPSession)this)) {
            this._connection.sendConnectionClose(530, "Queue is exclusive, but not created on this Connection.", this.getChannelId());
        }
        else {
            try {
                final long purged = queue.clearQueue();
                if (!nowait) {
                    this.sync();
                    final MethodRegistry methodRegistry = this._connection.getMethodRegistry();
                    final AMQMethodBody responseBody = methodRegistry.createQueuePurgeOkBody(purged);
                    this._connection.writeFrame(responseBody.generateFrame(this.getChannelId()));
                }
            }
            catch (AccessControlException e) {
                this._connection.sendConnectionClose(403, e.getMessage(), this.getChannelId());
            }
        }
    }
    
    public void receiveQueueUnbind(final AMQShortString queueName, final AMQShortString exchange, final AMQShortString bindingKey, final FieldTable arguments) {
        if (AMQChannel.LOGGER.isDebugEnabled()) {
            AMQChannel.LOGGER.debug("RECV[" + this._channelId + "] QueueUnbind[ queue: " + queueName + " exchange: " + exchange + " bindingKey: " + bindingKey + " arguments: " + arguments + " ]");
        }
        final NamedAddressSpace virtualHost = this._connection.getAddressSpace();
        final boolean useDefaultQueue = queueName == null;
        final Queue<?> queue = useDefaultQueue ? this.getDefaultQueue() : this.getQueue(queueName.toString());
        if (queue == null) {
            final String message = useDefaultQueue ? "No default queue defined on channel and queue was null" : ("Queue '" + queueName + "' does not exist.");
            this.closeChannel(404, message);
        }
        else if (this.isDefaultExchange(exchange)) {
            this._connection.sendConnectionClose(530, "Cannot unbind the queue '" + queue.getName() + "' from the default exchange", this.getChannelId());
        }
        else {
            final Exchange<?> exch = this.getExchange(exchange.toString());
            final String bindingKeyStr = (bindingKey == null) ? "" : AMQShortString.toString(bindingKey);
            if (exch == null) {
                this.closeChannel(404, "Exchange '" + exchange + "' does not exist.");
            }
            else if (!exch.hasBinding(bindingKeyStr, (Queue)queue)) {
                this.closeChannel(404, "No such binding");
            }
            else {
                try {
                    exch.deleteBinding(bindingKeyStr, (Queue)queue);
                    final AMQMethodBody responseBody = this._connection.getMethodRegistry().createQueueUnbindOkBody();
                    this.sync();
                    this._connection.writeFrame(responseBody.generateFrame(this.getChannelId()));
                }
                catch (AccessControlException e) {
                    this._connection.sendConnectionClose(403, e.getMessage(), this.getChannelId());
                }
            }
        }
    }
    
    public void receiveTxSelect() {
        if (AMQChannel.LOGGER.isDebugEnabled()) {
            AMQChannel.LOGGER.debug("RECV[" + this._channelId + "] TxSelect");
        }
        final ServerTransaction txn = this._transaction;
        if (txn instanceof LocalTransaction) {
            this.getConnection().unregisterTransactionTickers(this._transaction);
        }
        this._transaction = (ServerTransaction)this._connection.createLocalTransaction();
        final long notificationRepeatPeriod = (long)this.getContextValue((Class)Long.class, "qpid.session.transactionTimeoutNotificationRepeatPeriod");
        this.getConnection().registerTransactionTickers(this._transaction, message -> this._connection.sendConnectionCloseAsync(AMQPConnection.CloseReason.TRANSACTION_TIMEOUT, message), notificationRepeatPeriod);
        final MethodRegistry methodRegistry = this._connection.getMethodRegistry();
        final TxSelectOkBody responseBody = methodRegistry.createTxSelectOkBody();
        this._connection.writeFrame(responseBody.generateFrame(this._channelId));
    }
    
    public void receiveTxCommit() {
        if (AMQChannel.LOGGER.isDebugEnabled()) {
            AMQChannel.LOGGER.debug("RECV[" + this._channelId + "] TxCommit");
        }
        if (!this.isTransactional()) {
            this.closeChannel(503, "Fatal error: commit called on non-transactional channel");
        }
        else {
            this.commit(new Runnable() {
                @Override
                public void run() {
                    AMQChannel.this._connection.writeFrame(AMQChannel.this._txCommitOkFrame);
                }
            }, true);
        }
    }
    
    public void receiveTxRollback() {
        if (AMQChannel.LOGGER.isDebugEnabled()) {
            AMQChannel.LOGGER.debug("RECV[" + this._channelId + "] TxRollback");
        }
        if (!this.isTransactional()) {
            this.closeChannel(503, "Fatal error: rollback called on non-transactional channel");
        }
        else {
            final MethodRegistry methodRegistry = this._connection.getMethodRegistry();
            final AMQMethodBody responseBody = methodRegistry.createTxRollbackOkBody();
            final Runnable task = () -> this._connection.writeFrame(responseBody.generateFrame(this._channelId));
            this.rollback(task);
            this.resend();
        }
    }
    
    public void receiveConfirmSelect(final boolean nowait) {
        if (AMQChannel.LOGGER.isDebugEnabled()) {
            AMQChannel.LOGGER.debug("RECV[" + this._channelId + "] ConfirmSelect [ nowait: " + nowait + " ]");
        }
        this._confirmOnPublish = true;
        if (!nowait) {
            this._connection.writeFrame(new AMQFrame(this._channelId, ConfirmSelectOkBody.INSTANCE));
        }
    }
    
    private void closeChannel(final int cause, final String message) {
        this._connection.closeChannelAndWriteFrame(this, cause, message);
    }
    
    private boolean isDefaultExchange(final AMQShortString exchangeName) {
        return exchangeName == null || AMQShortString.EMPTY_STRING.equals(exchangeName);
    }
    
    private void setDefaultQueue(final Queue<?> queue) {
        final Queue<?> currentDefaultQueue = this._defaultQueue;
        if (queue != currentDefaultQueue) {
            if (currentDefaultQueue != null) {
                currentDefaultQueue.removeDeleteTask((Action)this._defaultQueueAssociationClearingTask);
            }
            if (queue != null) {
                queue.addDeleteTask((Action)this._defaultQueueAssociationClearingTask);
            }
        }
        this._defaultQueue = queue;
    }
    
    private Queue<?> getDefaultQueue() {
        return this._defaultQueue;
    }
    
    protected void updateBlockedStateIfNecessary() {
        final boolean desiredBlockingState = this._blocking.get();
        if (desiredBlockingState != this._wireBlockingState) {
            this._wireBlockingState = desiredBlockingState;
            this.sendFlow(!desiredBlockingState);
            this._blockTime = (desiredBlockingState ? System.currentTimeMillis() : 0L);
        }
    }
    
    public void restoreCredit(final ConsumerTarget target, final int count, final long size) {
        final boolean hasCredit = this._creditManager.hasCredit();
        this._creditManager.restoreCredit(count, size);
        if (this._creditManager.hasCredit() != hasCredit) {
            if (hasCredit || !this._creditManager.isNotBytesLimitedAndHighPrefetch()) {
                this.updateAllConsumerNotifyWorkDesired();
            }
        }
        else if (hasCredit) {
            if (this._creditManager.isNotBytesLimitedAndHighPrefetch()) {
                if (this._creditManager.isCreditOverBatchLimit()) {
                    this.updateAllConsumerNotifyWorkDesired();
                }
            }
            else if (this._creditManager.isBytesLimited()) {
                target.notifyWork();
            }
        }
    }
    
    private Collection<ConsumerTarget_0_8> getConsumerTargets() {
        return this._tag2SubscriptionTargetMap.values();
    }
    
    private Exchange<?> getExchange(final String name) {
        final MessageDestination destination = this.getAddressSpace().getAttainedMessageDestination(name, false);
        return (destination instanceof Exchange) ? destination : null;
    }
    
    private Queue<?> getQueue(final String name) {
        final MessageSource source = this.getAddressSpace().getAttainedMessageSource(name);
        return (source instanceof Queue) ? source : null;
    }
    
    public void dispose() {
        this._txCommitOkFrame.dispose();
        final IncomingMessage currentMessage = this._currentMessage;
        if (currentMessage != null) {
            this._currentMessage = null;
            final ContentHeaderBody contentHeader = currentMessage.getContentHeader();
            if (contentHeader != null) {
                contentHeader.dispose();
            }
            final int bodyCount = currentMessage.getBodyCount();
            if (bodyCount > 0) {
                for (int i = 0; i < bodyCount; ++i) {
                    currentMessage.getContentChunk(i).dispose();
                }
            }
        }
    }
    
    static {
        LOGGER = LoggerFactory.getLogger((Class)AMQChannel.class);
        INFINITE_CREDIT_CREDIT_MANAGER = new InfiniteCreditCreditManager();
        MESSAGE_INSTANCE_FUNCTION = (Function)new Function<MessageConsumerAssociation, MessageInstance>() {
            public MessageInstance apply(final MessageConsumerAssociation input) {
                return input.getMessageInstance();
            }
        };
        IMMEDIATE_DELIVERY_REPLY_TEXT = AMQShortString.createAMQShortString("Immediate delivery is not possible.");
    }
    
    private class NoLocalFilter implements MessageFilter
    {
        private final Object _connectionReference;
        
        public NoLocalFilter() {
            this._connectionReference = AMQChannel.this.getConnectionReference();
        }
        
        public String getName() {
            return AMQPFilterTypes.NO_LOCAL.toString();
        }
        
        public boolean matches(final Filterable message) {
            return message.getConnectionReference() != this._connectionReference;
        }
        
        public boolean startAtTail() {
            return false;
        }
        
        @Override
        public String toString() {
            return "NoLocalFilter[]";
        }
    }
    
    private class GetDeliveryMethod implements ClientDeliveryMethod
    {
        private final MessageSource _queue;
        private boolean _deliveredMessage;
        
        public GetDeliveryMethod(final MessageSource queue) {
            this._queue = queue;
        }
        
        @Override
        public long deliverToClient(final ConsumerTarget_0_8 target, final AMQMessage message, final InstanceProperties props, final long deliveryTag) {
            final int queueSize = (this._queue instanceof Queue) ? ((Queue)this._queue).getQueueDepthMessages() : 0;
            final long size = AMQChannel.this._connection.getProtocolOutputConverter().writeGetOk(message, props, AMQChannel.this.getChannelId(), deliveryTag, queueSize);
            this._deliveredMessage = true;
            return size;
        }
        
        public boolean hasDeliveredMessage() {
            return this._deliveredMessage;
        }
    }
    
    private class ImmediateAction implements Action<MessageInstance>
    {
        public ImmediateAction() {
        }
        
        public void performAction(final MessageInstance entry) {
            if (!entry.getDeliveredToConsumer() && entry.acquire()) {
                final ServerTransaction txn = (ServerTransaction)new LocalTransaction(AMQChannel.this._messageStore);
                final AMQMessage message = (AMQMessage)entry.getMessage();
                final MessageReference ref = message.newReference();
                try {
                    entry.delete();
                    txn.dequeue(entry.getEnqueueRecord(), (ServerTransaction.Action)new ServerTransaction.Action() {
                        public void postCommit() {
                            final ProtocolOutputConverter outputConverter = AMQChannel.this._connection.getProtocolOutputConverter();
                            outputConverter.writeReturn(message.getMessagePublishInfo(), message.getContentHeaderBody(), (MessageContentSource)message, AMQChannel.this._channelId, 313, AMQChannel.IMMEDIATE_DELIVERY_REPLY_TEXT);
                        }
                        
                        public void onRollback() {
                        }
                    });
                    txn.commit();
                }
                finally {
                    ref.release();
                }
            }
        }
    }
    
    private class MessageAcknowledgeAction implements ServerTransaction.Action
    {
        private Collection<MessageConsumerAssociation> _ackedMessages;
        
        public MessageAcknowledgeAction(final Collection<MessageConsumerAssociation> ackedMessages) {
            this._ackedMessages = ackedMessages;
        }
        
        public void postCommit() {
            try {
                for (final MessageConsumerAssociation association : this._ackedMessages) {
                    association.getMessageInstance().delete();
                }
            }
            finally {
                this._ackedMessages = (Collection<MessageConsumerAssociation>)Collections.emptySet();
            }
        }
        
        public void onRollback() {
            if (AMQChannel.this._rollingBack) {
                for (final MessageConsumerAssociation association : this._ackedMessages) {
                    association.getMessageInstance().makeAcquisitionStealable();
                }
                AMQChannel.this._resendList.addAll(this._ackedMessages);
            }
            else {
                try {
                    for (final MessageConsumerAssociation association : this._ackedMessages) {
                        final MessageInstance messageInstance = association.getMessageInstance();
                        messageInstance.release(association.getConsumer());
                    }
                }
                finally {
                    this._ackedMessages = (Collection<MessageConsumerAssociation>)Collections.emptySet();
                }
            }
        }
    }
    
    private class WriteReturnAction implements ServerTransaction.Action
    {
        private final int _errorCode;
        private final String _description;
        private final MessageReference<AMQMessage> _reference;
        
        public WriteReturnAction(final int errorCode, final String description, final AMQMessage message) {
            this._errorCode = errorCode;
            this._description = description;
            this._reference = (MessageReference<AMQMessage>)message.newReference();
        }
        
        public void postCommit() {
            final AMQMessage message = (AMQMessage)this._reference.getMessage();
            AMQChannel.this._connection.getProtocolOutputConverter().writeReturn(message.getMessagePublishInfo(), message.getContentHeaderBody(), (MessageContentSource)message, AMQChannel.this._channelId, this._errorCode, AMQShortString.validValueOf(this._description));
            this._reference.release();
        }
        
        public void onRollback() {
            this._reference.release();
        }
    }
    
    private class DefaultQueueAssociationClearingTask implements Action<Queue<?>>
    {
        public void performAction(final Queue<?> queue) {
            if (queue == AMQChannel.this._defaultQueue) {
                AMQChannel.this._defaultQueue = null;
            }
        }
    }
}
