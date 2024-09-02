// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.qpid.server.protocol.v0_8;

import org.apache.qpid.server.model.State;
import org.apache.qpid.server.model.ConfiguredObject;
import java.util.concurrent.ExecutionException;
import org.apache.qpid.server.util.ServerScopedRuntimeException;
import org.apache.qpid.server.protocol.CapacityChecker;
import org.apache.qpid.server.message.MessageContentSource;
import org.apache.qpid.server.filter.Filterable;
import org.slf4j.LoggerFactory;
import org.apache.qpid.transport.network.Ticker;
import org.apache.qpid.framing.ConfirmSelectOkBody;
import org.apache.qpid.framing.TxSelectOkBody;
import org.apache.qpid.framing.QueueDeleteOkBody;
import org.apache.qpid.framing.QueueDeclareOkBody;
import org.apache.qpid.server.virtualhost.QueueExistsException;
import org.apache.qpid.server.model.ExclusivityPolicy;
import org.apache.qpid.server.queue.QueueArgumentsConverter;
import org.apache.qpid.framing.ExchangeDeleteOkBody;
import org.apache.qpid.server.virtualhost.RequiredExchangeException;
import org.apache.qpid.server.virtualhost.ExchangeIsAlternateException;
import org.apache.qpid.server.model.UnknownConfiguredObjectException;
import org.apache.qpid.server.model.NoFactoryForTypeException;
import org.apache.qpid.server.virtualhost.ExchangeExistsException;
import org.apache.qpid.server.virtualhost.ReservedExchangeNameException;
import org.apache.qpid.server.model.LifetimePolicy;
import org.apache.qpid.framing.ExchangeBoundOkBody;
import org.apache.qpid.server.exchange.ExchangeImpl;
import org.apache.qpid.framing.BasicContentHeaderProperties;
import org.apache.qpid.transport.util.Functions;
import org.apache.qpid.bytebuffer.QpidByteBuffer;
import org.apache.qpid.framing.BasicGetEmptyBody;
import org.apache.qpid.framing.BasicCancelOkBody;
import org.apache.qpid.framing.AccessRequestOkBody;
import org.apache.qpid.framing.ProtocolVersion;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.qpid.server.model.Exchange;
import org.apache.qpid.server.store.TransactionLogResource;
import org.apache.qpid.framing.AMQMethodBody;
import org.apache.qpid.framing.MethodRegistry;
import org.apache.qpid.server.transport.AMQPConnection;
import java.util.LinkedHashMap;
import org.apache.qpid.server.logging.LogMessage;
import org.apache.qpid.server.filter.MessageFilter;
import java.security.AccessControlException;
import org.apache.qpid.server.filter.ArrivalTimeFilter;
import org.apache.qpid.server.filter.AMQInvalidArgumentException;
import org.apache.qpid.server.filter.FilterManagerFactory;
import org.apache.qpid.common.AMQPFilterTypes;
import java.util.Collection;
import org.apache.qpid.server.logging.messages.ExchangeMessages;
import java.util.Iterator;
import org.apache.qpid.framing.BasicAckBody;
import org.apache.qpid.server.message.MessageReference;
import org.apache.qpid.framing.ContentBody;
import org.apache.qpid.server.store.MessageHandle;
import org.apache.qpid.server.message.ServerMessage;
import org.apache.qpid.server.message.InstanceProperties;
import org.apache.qpid.framing.AMQDataBlock;
import org.apache.qpid.framing.AMQBody;
import org.apache.qpid.framing.AMQFrame;
import org.apache.qpid.framing.BasicNackBody;
import org.apache.qpid.server.store.StorableMessageMetaData;
import org.apache.qpid.framing.ContentHeaderBody;
import org.apache.qpid.server.virtualhost.VirtualHostImpl;
import org.apache.qpid.server.message.MessageDestination;
import org.apache.qpid.framing.MessagePublishInfo;
import org.apache.qpid.server.txn.LocalTransaction;
import org.apache.qpid.server.filter.FilterManager;
import org.apache.qpid.server.consumer.ConsumerTarget;
import org.apache.qpid.framing.FieldTable;
import java.util.EnumSet;
import org.apache.qpid.server.message.MessageSource;
import java.security.AccessController;
import org.apache.qpid.server.logging.messages.ChannelMessages;
import java.security.PrivilegedAction;
import org.apache.qpid.server.logging.EventLoggerProvider;
import org.apache.qpid.protocol.AMQConstant;
import org.apache.qpid.server.logging.subjects.ChannelLogSubject;
import org.apache.qpid.server.security.SecurityManager;
import org.apache.qpid.server.connection.SessionPrincipal;
import java.security.Principal;
import org.apache.qpid.server.transport.ProtocolEngine;
import org.apache.qpid.server.consumer.ConsumerImpl;
import java.util.Collections;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.qpid.server.store.StoredMessage;
import org.apache.qpid.server.model.Session;
import org.apache.qpid.server.protocol.ConsumerListener;
import org.apache.qpid.server.model.ConfigurationChangeListener;
import org.apache.qpid.server.model.Consumer;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.security.auth.Subject;
import org.apache.qpid.server.util.Action;
import java.util.UUID;
import org.apache.qpid.server.TransactionTimeoutHelper;
import org.apache.qpid.server.message.MessageInstance;
import org.apache.qpid.server.logging.LogSubject;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.qpid.server.txn.ServerTransaction;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.LinkedList;
import org.apache.qpid.server.store.MessageStore;
import java.util.List;
import org.apache.qpid.framing.AMQShortString;
import java.util.Map;
import org.apache.qpid.server.queue.AMQQueue;
import java.security.AccessControlContext;
import org.apache.qpid.server.flow.FlowCreditManager;
import org.slf4j.Logger;
import org.apache.qpid.framing.ServerChannelMethodProcessor;
import org.apache.qpid.server.txn.AsyncAutoCommitTransaction;
import org.apache.qpid.server.protocol.AMQSessionModel;

public class AMQChannel implements AMQSessionModel<AMQChannel>, AsyncAutoCommitTransaction.FutureRecorder, ServerChannelMethodProcessor
{
    public static final int DEFAULT_PREFETCH = 4096;
    private static final Logger _logger;
    private final DefaultQueueAssociationClearingTask _defaultQueueAssociationClearingTask;
    private final boolean _messageAuthorizationRequired;
    private final int _channelId;
    private final Pre0_10CreditManager _creditManager;
    private final FlowCreditManager _noAckCreditManager;
    private final AccessControlContext _accessControllerContext;
    private long _deliveryTag;
    private volatile AMQQueue<?> _defaultQueue;
    private int _consumerTag;
    private IncomingMessage _currentMessage;
    private final Map<AMQShortString, ConsumerTarget_0_8> _tag2SubscriptionTargetMap;
    private final List<ConsumerTarget_0_8> _consumersWithPendingWork;
    private final MessageStore _messageStore;
    private final LinkedList<AsyncCommand> _unfinishedCommandsQueue;
    private UnacknowledgedMessageMap _unacknowledgedMessageMap;
    private final AtomicBoolean _suspended;
    private ServerTransaction _transaction;
    private final AtomicLong _txnStarts;
    private final AtomicLong _txnCommits;
    private final AtomicLong _txnRejects;
    private final AtomicLong _txnCount;
    private final AMQPConnection_0_8 _connection;
    private AtomicBoolean _closing;
    private final Set<Object> _blockingEntities;
    private final AtomicBoolean _blocking;
    private LogSubject _logSubject;
    private volatile boolean _rollingBack;
    private List<MessageInstance> _resendList;
    private static final AMQShortString IMMEDIATE_DELIVERY_REPLY_TEXT;
    private final ClientDeliveryMethod _clientDeliveryMethod;
    private final TransactionTimeoutHelper _transactionTimeoutHelper;
    private final UUID _id;
    private final List<Action<? super AMQChannel>> _taskList;
    private final CapacityCheckAction _capacityCheckAction;
    private final ImmediateAction _immediateAction;
    private final Subject _subject;
    private final CopyOnWriteArrayList<Consumer<?>> _consumers;
    private final ConfigurationChangeListener _consumerClosedListener;
    private final CopyOnWriteArrayList<ConsumerListener> _consumerListeners;
    private Session<?> _modelObject;
    private long _blockTime;
    private long _blockingTimeout;
    private boolean _confirmOnPublish;
    private long _confirmedMessageCounter;
    private volatile long _uncommittedMessageSize;
    private final List<StoredMessage<MessageMetaData>> _uncommittedMessages;
    private long _maxUncommittedInMemorySize;
    private boolean _wireBlockingState;
    private boolean _prefetchLoggedForChannel;
    private boolean _logChannelFlowMessages;
    private final String id;
    private final RecordDeliveryMethod _recordDeliveryMethod;
    
    public AMQChannel(final AMQPConnection_0_8 connection, final int channelId, final MessageStore messageStore) {
        this._defaultQueueAssociationClearingTask = new DefaultQueueAssociationClearingTask();
        this._deliveryTag = 0L;
        this._tag2SubscriptionTargetMap = new HashMap<AMQShortString, ConsumerTarget_0_8>();
        this._consumersWithPendingWork = new ArrayList<ConsumerTarget_0_8>();
        this._unfinishedCommandsQueue = new LinkedList<AsyncCommand>();
        this._unacknowledgedMessageMap = new UnacknowledgedMessageMapImpl(4096);
        this._suspended = new AtomicBoolean(false);
        this._txnStarts = new AtomicLong(0L);
        this._txnCommits = new AtomicLong(0L);
        this._txnRejects = new AtomicLong(0L);
        this._txnCount = new AtomicLong(0L);
        this._closing = new AtomicBoolean(false);
        this._blockingEntities = Collections.synchronizedSet(new HashSet<Object>());
        this._blocking = new AtomicBoolean(false);
        this._resendList = new ArrayList<MessageInstance>();
        this._id = UUID.randomUUID();
        this._taskList = new CopyOnWriteArrayList<Action<? super AMQChannel>>();
        this._capacityCheckAction = new CapacityCheckAction();
        this._immediateAction = new ImmediateAction();
        this._consumers = new CopyOnWriteArrayList<Consumer<?>>();
        this._consumerClosedListener = (ConfigurationChangeListener)new ConsumerClosedListener();
        this._consumerListeners = new CopyOnWriteArrayList<ConsumerListener>();
        this._uncommittedMessages = new ArrayList<StoredMessage<MessageMetaData>>();
        this._prefetchLoggedForChannel = false;
        this._logChannelFlowMessages = true;
        this.id = "(" + System.identityHashCode(this) + ")";
        this._recordDeliveryMethod = new RecordDeliveryMethod() {
            @Override
            public void recordMessageDelivery(final ConsumerImpl sub, final MessageInstance entry, final long deliveryTag) {
                AMQChannel.this.addUnacknowledgedMessage(entry, deliveryTag, sub);
            }
        };
        this._creditManager = new Pre0_10CreditManager(0L, 0L, (ProtocolEngine)connection);
        this._noAckCreditManager = (FlowCreditManager)new NoAckCreditManager((ProtocolEngine)connection);
        this._connection = connection;
        this._channelId = channelId;
        this._subject = new Subject(false, connection.getAuthorizedSubject().getPrincipals(), connection.getAuthorizedSubject().getPublicCredentials(), connection.getAuthorizedSubject().getPrivateCredentials());
        this._subject.getPrincipals().add((Principal)new SessionPrincipal((AMQSessionModel)this));
        this._accessControllerContext = SecurityManager.getAccessControlContextFromSubject(this._subject);
        this._maxUncommittedInMemorySize = (long)connection.getVirtualHost().getContextValue((Class)Long.class, "connection.maxUncommittedInMemorySize");
        this._messageAuthorizationRequired = (boolean)connection.getVirtualHost().getContextValue((Class)Boolean.class, "qpid.broker_msg_auth");
        this._logSubject = (LogSubject)new ChannelLogSubject((AMQSessionModel)this);
        this._messageStore = messageStore;
        this._blockingTimeout = (long)connection.getBroker().getContextValue((Class)Long.class, "channel.flowControlEnforcementTimeout");
        this._transaction = (ServerTransaction)new AsyncAutoCommitTransaction(this._messageStore, (AsyncAutoCommitTransaction.FutureRecorder)this);
        this._clientDeliveryMethod = connection.createDeliveryMethod(this._channelId);
        this._transactionTimeoutHelper = new TransactionTimeoutHelper(this._logSubject, (TransactionTimeoutHelper.CloseAction)new TransactionTimeoutHelper.CloseAction() {
            public void doTimeoutAction(final String reason) {
                AMQChannel.this._connection.sendConnectionCloseAsync(AMQConstant.RESOURCE_ERROR, reason);
            }
        }, (EventLoggerProvider)this.getVirtualHost());
        AccessController.doPrivileged((PrivilegedAction<Object>)new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                AMQChannel.this.getVirtualHost().getEventLogger().message(ChannelMessages.CREATE());
                return null;
            }
        }, this._accessControllerContext);
    }
    
    public AccessControlContext getAccessControllerContext() {
        return this._accessControllerContext;
    }
    
    private boolean performGet(final MessageSource queue, final boolean acks) throws MessageSource.ExistingConsumerPreventsExclusive, MessageSource.ExistingExclusiveConsumer, MessageSource.ConsumerAccessRefused {
        final FlowCreditManager singleMessageCredit = (FlowCreditManager)new MessageOnlyCreditManager(1L);
        final GetDeliveryMethod getDeliveryMethod = new GetDeliveryMethod(singleMessageCredit, queue);
        final RecordDeliveryMethod getRecordMethod = new RecordDeliveryMethod() {
            @Override
            public void recordMessageDelivery(final ConsumerImpl sub, final MessageInstance entry, final long deliveryTag) {
                AMQChannel.this.addUnacknowledgedMessage(entry, deliveryTag, null);
            }
        };
        final EnumSet<ConsumerImpl.Option> options = EnumSet.of(ConsumerImpl.Option.TRANSIENT, ConsumerImpl.Option.ACQUIRES, ConsumerImpl.Option.SEES_REQUEUES);
        ConsumerTarget_0_8 target;
        if (acks) {
            target = ConsumerTarget_0_8.createAckTarget(this, AMQShortString.EMPTY_STRING, null, singleMessageCredit, getDeliveryMethod, getRecordMethod);
        }
        else {
            target = ConsumerTarget_0_8.createGetNoAckTarget(this, AMQShortString.EMPTY_STRING, null, singleMessageCredit, getDeliveryMethod, getRecordMethod);
        }
        final ConsumerImpl sub = queue.addConsumer((ConsumerTarget)target, (FilterManager)null, (Class)AMQMessage.class, "", (EnumSet)options);
        sub.flush();
        sub.close();
        return getDeliveryMethod.hasDeliveredMessage();
    }
    
    public void setLocalTransactional() {
        this._transaction = (ServerTransaction)new LocalTransaction(this._messageStore, (LocalTransaction.ActivityTimeAccessor)new LocalTransaction.ActivityTimeAccessor() {
            public long getActivityTime() {
                return AMQChannel.this._connection.getLastReadTime();
            }
        });
        this._txnStarts.incrementAndGet();
    }
    
    public boolean isTransactional() {
        return this._transaction.isTransactional();
    }
    
    public void receivedComplete() {
        this.sync();
    }
    
    private void incrementOutstandingTxnsIfNecessary() {
        if (this.isTransactional()) {
            this._txnCount.compareAndSet(0L, 1L);
        }
    }
    
    private void decrementOutstandingTxnsIfNecessary() {
        if (this.isTransactional()) {
            this._txnCount.compareAndSet(1L, 0L);
        }
    }
    
    public Long getTxnCommits() {
        return this._txnCommits.get();
    }
    
    public Long getTxnRejects() {
        return this._txnRejects.get();
    }
    
    public Long getTxnCount() {
        return this._txnCount.get();
    }
    
    public Long getTxnStart() {
        return this._txnStarts.get();
    }
    
    public int getChannelId() {
        return this._channelId;
    }
    
    public void setPublishFrame(final MessagePublishInfo info, final MessageDestination e) {
        final String routingKey = AMQShortString.toString(info.getRoutingKey());
        final VirtualHostImpl virtualHost = this.getVirtualHost();
        final SecurityManager securityManager = virtualHost.getSecurityManager();
        securityManager.authorisePublish(info.isImmediate(), routingKey, e.getName(), virtualHost.getName(), this._subject);
        (this._currentMessage = new IncomingMessage(info)).setMessageDestination(e);
    }
    
    public void publishContentHeader(final ContentHeaderBody contentHeaderBody) {
        if (AMQChannel._logger.isDebugEnabled()) {
            AMQChannel._logger.debug("Content header received on channel " + this._channelId);
        }
        this._currentMessage.setContentHeaderBody(contentHeaderBody);
        this.deliverCurrentMessageIfComplete();
    }
    
    private void deliverCurrentMessageIfComplete() {
        if (this._currentMessage.allContentReceived()) {
            if (this._confirmOnPublish) {
                ++this._confirmedMessageCounter;
            }
            Runnable finallyAction = null;
            final ContentHeaderBody contentHeader = this._currentMessage.getContentHeader();
            final long bodySize = this._currentMessage.getSize();
            final long timestamp = contentHeader.getProperties().getTimestamp();
            try {
                final MessagePublishInfo messagePublishInfo = this._currentMessage.getMessagePublishInfo();
                final MessageDestination destination = this._currentMessage.getDestination();
                final MessageMetaData messageMetaData = new MessageMetaData(messagePublishInfo, contentHeader, this.getConnection().getLastReadTime());
                final MessageHandle<MessageMetaData> handle = (MessageHandle<MessageMetaData>)this._messageStore.addMessage((StorableMessageMetaData)messageMetaData);
                final int bodyCount = this._currentMessage.getBodyCount();
                if (bodyCount > 0) {
                    long bodyLengthReceived = 0L;
                    for (int i = 0; i < bodyCount; ++i) {
                        final ContentBody contentChunk = this._currentMessage.getContentChunk(i);
                        handle.addContent(contentChunk.getPayload());
                        bodyLengthReceived += contentChunk.getSize();
                        contentChunk.dispose();
                    }
                }
                final StoredMessage<MessageMetaData> storedMessage = (StoredMessage<MessageMetaData>)handle.allContentAdded();
                final AMQMessage amqMessage = this.createAMQMessage(storedMessage);
                final MessageReference reference = amqMessage.newReference();
                try {
                    this._currentMessage = null;
                    if (!this.checkMessageUserId(contentHeader)) {
                        if (this._confirmOnPublish) {
                            this._connection.writeFrame((AMQDataBlock)new AMQFrame(this._channelId, (AMQBody)new BasicNackBody(this._confirmedMessageCounter, false, false)));
                        }
                        this._transaction.addPostTransactionAction((ServerTransaction.Action)new WriteReturnAction(AMQConstant.ACCESS_REFUSED, "Access Refused", amqMessage));
                    }
                    else {
                        final boolean immediate = messagePublishInfo.isImmediate();
                        final InstanceProperties instanceProperties = (InstanceProperties)new InstanceProperties() {
                            public Object getProperty(final InstanceProperties.Property prop) {
                                switch (prop) {
                                    case EXPIRATION: {
                                        return amqMessage.getExpiration();
                                    }
                                    case IMMEDIATE: {
                                        return immediate;
                                    }
                                    case PERSISTENT: {
                                        return amqMessage.isPersistent();
                                    }
                                    case MANDATORY: {
                                        return messagePublishInfo.isMandatory();
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
                        final int enqueues = destination.send((ServerMessage)amqMessage, amqMessage.getInitialRoutingAddress(), instanceProperties, this._transaction, (Action)(immediate ? this._immediateAction : this._capacityCheckAction));
                        if (enqueues == 0) {
                            finallyAction = this.handleUnroutableMessage(amqMessage);
                        }
                        else {
                            if (this._confirmOnPublish) {
                                final BasicAckBody responseBody = this._connection.getMethodRegistry().createBasicAckBody(this._confirmedMessageCounter, false);
                                this._connection.writeFrame((AMQDataBlock)responseBody.generateFrame(this._channelId));
                            }
                            this.incrementUncommittedMessageSize(storedMessage);
                            this.incrementOutstandingTxnsIfNecessary();
                        }
                    }
                }
                finally {
                    reference.release();
                    if (finallyAction != null) {
                        finallyAction.run();
                    }
                }
            }
            finally {
                this._connection.registerMessageReceived(bodySize, timestamp);
                this._currentMessage = null;
            }
        }
    }
    
    private void incrementUncommittedMessageSize(final StoredMessage<MessageMetaData> handle) {
        if (this.isTransactional()) {
            this._uncommittedMessageSize += ((MessageMetaData)handle.getMetaData()).getContentSize();
            if (this._uncommittedMessageSize > this.getMaxUncommittedInMemorySize()) {
                handle.flowToDisk();
                if (!this._uncommittedMessages.isEmpty() || this._uncommittedMessageSize == ((MessageMetaData)handle.getMetaData()).getContentSize()) {
                    this.getVirtualHost().getEventLogger().message(this._logSubject, ChannelMessages.LARGE_TRANSACTION_WARN((Number)this._uncommittedMessageSize));
                }
                if (!this._uncommittedMessages.isEmpty()) {
                    for (final StoredMessage<MessageMetaData> uncommittedHandle : this._uncommittedMessages) {
                        uncommittedHandle.flowToDisk();
                    }
                    this._uncommittedMessages.clear();
                }
            }
            else {
                this._uncommittedMessages.add(handle);
            }
        }
    }
    
    private Runnable handleUnroutableMessage(final AMQMessage message) {
        final boolean mandatory = message.isMandatory();
        final String exchangeName = AMQShortString.toString(message.getMessagePublishInfo().getExchange());
        final String routingKey = AMQShortString.toString(message.getMessagePublishInfo().getRoutingKey());
        final String description = String.format("[Exchange: %s, Routing key: %s]", exchangeName, routingKey);
        final boolean closeOnNoRoute = this._connection.isCloseWhenNoRoute();
        Runnable returnVal = null;
        if (AMQChannel._logger.isDebugEnabled()) {
            AMQChannel._logger.debug(String.format("Unroutable message %s, mandatory=%s, transactionalSession=%s, closeOnNoRoute=%s", description, mandatory, this.isTransactional(), closeOnNoRoute));
        }
        if (mandatory && this.isTransactional() && !this._confirmOnPublish && this._connection.isCloseWhenNoRoute()) {
            returnVal = new Runnable() {
                @Override
                public void run() {
                    AMQChannel.this._connection.sendConnectionClose(AMQConstant.NO_ROUTE, "No route for message " + description, AMQChannel.this._channelId);
                }
            };
        }
        else if (mandatory || message.isImmediate()) {
            if (this._confirmOnPublish) {
                this._connection.writeFrame((AMQDataBlock)new AMQFrame(this._channelId, (AMQBody)new BasicNackBody(this._confirmedMessageCounter, false, false)));
            }
            this._transaction.addPostTransactionAction((ServerTransaction.Action)new WriteReturnAction(AMQConstant.NO_ROUTE, "No Route for message " + description, message));
        }
        else {
            this.getVirtualHost().getEventLogger().message(ExchangeMessages.DISCARDMSG(exchangeName, routingKey));
        }
        return returnVal;
    }
    
    public void publishContentBody(final ContentBody contentBody) {
        if (AMQChannel._logger.isDebugEnabled()) {
            AMQChannel._logger.debug(this.debugIdentity() + " content body received on channel " + this._channelId);
        }
        try {
            final long currentSize = this._currentMessage.addContentBodyFrame(contentBody);
            if (currentSize > this._currentMessage.getSize()) {
                this._connection.sendConnectionClose(AMQConstant.FRAME_ERROR, "More message data received than content header defined", this._channelId);
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
    
    public int getNextConsumerTag() {
        return ++this._consumerTag;
    }
    
    public ConsumerTarget getSubscription(final AMQShortString tag) {
        return (ConsumerTarget)this._tag2SubscriptionTargetMap.get(tag);
    }
    
    public AMQShortString consumeFromSource(AMQShortString tag, final Collection<MessageSource> sources, final boolean acks, final FieldTable arguments, final boolean exclusive, final boolean noLocal) throws MessageSource.ExistingConsumerPreventsExclusive, MessageSource.ExistingExclusiveConsumer, AMQInvalidArgumentException, MessageSource.ConsumerAccessRefused, ConsumerTagInUseException {
        if (tag == null) {
            tag = new AMQShortString("sgen_" + this.getNextConsumerTag());
        }
        if (this._tag2SubscriptionTargetMap.containsKey(tag)) {
            throw new ConsumerTagInUseException("Consumer already exists with same tag: " + tag);
        }
        final EnumSet<ConsumerImpl.Option> options = EnumSet.noneOf(ConsumerImpl.Option.class);
        ConsumerTarget_0_8 target;
        if (arguments != null && Boolean.TRUE.equals(arguments.get(AMQPFilterTypes.NO_CONSUME.getValue()))) {
            target = ConsumerTarget_0_8.createBrowserTarget(this, tag, arguments, this._noAckCreditManager);
        }
        else if (acks) {
            target = ConsumerTarget_0_8.createAckTarget(this, tag, arguments, (FlowCreditManager)this._creditManager);
            options.add(ConsumerImpl.Option.ACQUIRES);
            options.add(ConsumerImpl.Option.SEES_REQUEUES);
        }
        else {
            target = ConsumerTarget_0_8.createNoAckTarget(this, tag, arguments, this._noAckCreditManager);
            options.add(ConsumerImpl.Option.ACQUIRES);
            options.add(ConsumerImpl.Option.SEES_REQUEUES);
        }
        if (exclusive) {
            options.add(ConsumerImpl.Option.EXCLUSIVE);
        }
        this._tag2SubscriptionTargetMap.put(tag, target);
        try {
            FilterManager filterManager = FilterManagerFactory.createManager(FieldTable.convertToMap(arguments));
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
                Label_0438: {
                    if (!(value instanceof Number)) {
                        if (value instanceof String) {
                            try {
                                period = Long.parseLong(value.toString());
                                break Label_0438;
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
            for (final MessageSource source : sources) {
                final ConsumerImpl sub = source.addConsumer((ConsumerTarget)target, filterManager, (Class)AMQMessage.class, AMQShortString.toString(tag), (EnumSet)options);
                if (sub instanceof Consumer) {
                    final Consumer<?> modelConsumer = (Consumer<?>)sub;
                    this.consumerAdded(modelConsumer);
                    modelConsumer.addChangeListener(this._consumerClosedListener);
                    this._consumers.add(modelConsumer);
                }
            }
        }
        catch (AccessControlException | MessageSource.ExistingExclusiveConsumer | MessageSource.ExistingConsumerPreventsExclusive | AMQInvalidArgumentException | MessageSource.ConsumerAccessRefused ex2) {
            final Exception ex;
            final Exception e = ex;
            this._tag2SubscriptionTargetMap.remove(tag);
            throw e;
        }
        return tag;
    }
    
    public boolean unsubscribeConsumer(final AMQShortString consumerTag) {
        if (AMQChannel._logger.isDebugEnabled()) {
            AMQChannel._logger.debug("Unsubscribing consumer '{}' on channel {}", (Object)consumerTag, (Object)this);
        }
        final ConsumerTarget_0_8 target = this._tag2SubscriptionTargetMap.remove(consumerTag);
        final Collection<ConsumerImpl> subs = (target == null) ? null : target.getConsumers();
        if (subs != null) {
            for (final ConsumerImpl sub : subs) {
                sub.close();
                if (sub instanceof Consumer) {
                    this._consumers.remove(sub);
                }
            }
            return true;
        }
        AMQChannel._logger.warn("Attempt to unsubscribe consumer with tag '" + consumerTag + "' which is not registered.");
        return false;
    }
    
    public void close() {
        this.close(null, null);
    }
    
    public void close(final AMQConstant cause, final String message) {
        if (!this._closing.compareAndSet(false, true)) {
            return;
        }
        try {
            this.unsubscribeAllConsumers();
            this.setDefaultQueue(null);
            if (this._modelObject != null) {
                this._modelObject.delete();
            }
            for (final Action<? super AMQChannel> task : this._taskList) {
                task.performAction((Object)this);
            }
            this._transaction.rollback();
            this.requeue();
        }
        finally {
            final LogMessage operationalLogMessage = (cause == null) ? ChannelMessages.CLOSE() : ChannelMessages.CLOSE_FORCED((Number)cause.getCode(), message);
            this.getVirtualHost().getEventLogger().message(this._logSubject, operationalLogMessage);
        }
    }
    
    private void unsubscribeAllConsumers() {
        if (AMQChannel._logger.isDebugEnabled()) {
            if (!this._tag2SubscriptionTargetMap.isEmpty()) {
                AMQChannel._logger.debug("Unsubscribing all consumers on channel " + this.toString());
            }
            else {
                AMQChannel._logger.debug("No consumers to unsubscribe on channel " + this.toString());
            }
        }
        final Set<AMQShortString> subscriptionTags = new HashSet<AMQShortString>(this._tag2SubscriptionTargetMap.keySet());
        for (final AMQShortString tag : subscriptionTags) {
            this.unsubscribeConsumer(tag);
        }
    }
    
    public void addUnacknowledgedMessage(final MessageInstance entry, final long deliveryTag, final ConsumerImpl consumer) {
        if (AMQChannel._logger.isDebugEnabled()) {
            AMQChannel._logger.debug(this.debugIdentity() + " Adding unacked message(" + entry.getMessage().toString() + " DT:" + deliveryTag + ") for " + consumer + " on " + entry.getOwningResource().getName());
        }
        this._unacknowledgedMessageMap.add(deliveryTag, entry);
    }
    
    public String debugIdentity() {
        return this._channelId + this.id;
    }
    
    public void requeue() {
        final Collection<MessageInstance> messagesToBeDelivered = this._unacknowledgedMessageMap.cancelAllMessages();
        if (!messagesToBeDelivered.isEmpty() && AMQChannel._logger.isDebugEnabled()) {
            AMQChannel._logger.debug("Requeuing " + messagesToBeDelivered.size() + " unacked messages. for " + this.toString());
        }
        for (final MessageInstance unacked : messagesToBeDelivered) {
            unacked.setRedelivered();
            unacked.release();
        }
    }
    
    public void requeue(final long deliveryTag) {
        final MessageInstance unacked = this._unacknowledgedMessageMap.remove(deliveryTag);
        if (unacked != null) {
            unacked.setRedelivered();
            unacked.release();
        }
        else {
            AMQChannel._logger.warn("Requested requeue of message:" + deliveryTag + " but no such delivery tag exists." + this._unacknowledgedMessageMap.size());
        }
    }
    
    public boolean isMaxDeliveryCountEnabled(final long deliveryTag) {
        final MessageInstance queueEntry = this._unacknowledgedMessageMap.get(deliveryTag);
        if (queueEntry != null) {
            final int maximumDeliveryCount = queueEntry.getMaximumDeliveryCount();
            return maximumDeliveryCount > 0;
        }
        return false;
    }
    
    public boolean isDeliveredTooManyTimes(final long deliveryTag) {
        final MessageInstance queueEntry = this._unacknowledgedMessageMap.get(deliveryTag);
        if (queueEntry != null) {
            final int maximumDeliveryCount = queueEntry.getMaximumDeliveryCount();
            final int numDeliveries = queueEntry.getDeliveryCount();
            return maximumDeliveryCount != 0 && numDeliveries >= maximumDeliveryCount;
        }
        return false;
    }
    
    public void resend() {
        final Map<Long, MessageInstance> msgToRequeue = new LinkedHashMap<Long, MessageInstance>();
        final Map<Long, MessageInstance> msgToResend = new LinkedHashMap<Long, MessageInstance>();
        if (AMQChannel._logger.isDebugEnabled()) {
            AMQChannel._logger.debug("unacked map Size:" + this._unacknowledgedMessageMap.size());
        }
        this._unacknowledgedMessageMap.visit(new ExtractResendAndRequeue(this._unacknowledgedMessageMap, msgToRequeue, msgToResend));
        if (AMQChannel._logger.isDebugEnabled()) {
            if (!msgToResend.isEmpty()) {
                AMQChannel._logger.debug("Preparing (" + msgToResend.size() + ") message to resend.");
            }
            else {
                AMQChannel._logger.debug("No message to resend.");
            }
        }
        for (final Map.Entry<Long, MessageInstance> entry : msgToResend.entrySet()) {
            final MessageInstance message = entry.getValue();
            final long deliveryTag = entry.getKey();
            message.decrementDeliveryCount();
            message.setRedelivered();
            if (!message.resend()) {
                msgToRequeue.put(deliveryTag, message);
            }
        }
        if (AMQChannel._logger.isDebugEnabled() && !msgToRequeue.isEmpty()) {
            AMQChannel._logger.debug("Preparing (" + msgToRequeue.size() + ") message to requeue");
        }
        for (final Map.Entry<Long, MessageInstance> entry : msgToRequeue.entrySet()) {
            final MessageInstance message = entry.getValue();
            final long deliveryTag = entry.getKey();
            message.decrementDeliveryCount();
            this._unacknowledgedMessageMap.remove(deliveryTag);
            message.setRedelivered();
            message.release();
        }
    }
    
    public void acknowledgeMessage(final long deliveryTag, final boolean multiple) {
        final Collection<MessageInstance> ackedMessages = this.getAckedMessages(deliveryTag, multiple);
        this._transaction.dequeue((Collection)ackedMessages, (ServerTransaction.Action)new MessageAcknowledgeAction(ackedMessages));
    }
    
    private Collection<MessageInstance> getAckedMessages(final long deliveryTag, final boolean multiple) {
        return this._unacknowledgedMessageMap.acknowledge(deliveryTag, multiple);
    }
    
    public UnacknowledgedMessageMap getUnacknowledgedMessageMap() {
        return this._unacknowledgedMessageMap;
    }
    
    public void setSuspended(final boolean suspended) {
        final boolean wasSuspended = this._suspended.getAndSet(suspended);
        if (wasSuspended != suspended) {
            if (!suspended && this._logChannelFlowMessages) {
                this.getVirtualHost().getEventLogger().message(this._logSubject, ChannelMessages.FLOW("Started"));
            }
            if (wasSuspended) {
                for (final ConsumerTarget_0_8 s : this.getConsumerTargets()) {
                    for (final ConsumerImpl sub : s.getConsumers()) {
                        sub.externalStateChange();
                    }
                }
            }
            if (!wasSuspended) {
                this.ensureConsumersNoticedStateChange();
            }
            if (suspended && this._logChannelFlowMessages) {
                this.getVirtualHost().getEventLogger().message(this._logSubject, ChannelMessages.FLOW("Stopped"));
            }
        }
    }
    
    public boolean isSuspended() {
        return this._suspended.get() || this._closing.get() || this._connection.isClosing();
    }
    
    public void commit(final Runnable immediateAction, final boolean async) {
        if (async && this._transaction instanceof LocalTransaction) {
            ((LocalTransaction)this._transaction).commitAsync((Runnable)new Runnable() {
                @Override
                public void run() {
                    try {
                        immediateAction.run();
                    }
                    finally {
                        AMQChannel.this._txnCommits.incrementAndGet();
                        AMQChannel.this._txnStarts.incrementAndGet();
                        AMQChannel.this.decrementOutstandingTxnsIfNecessary();
                    }
                }
            });
        }
        else {
            this._transaction.commit(immediateAction);
            this._txnCommits.incrementAndGet();
            this._txnStarts.incrementAndGet();
            this.decrementOutstandingTxnsIfNecessary();
        }
        this.resetUncommittedMessages();
    }
    
    private void resetUncommittedMessages() {
        this._uncommittedMessageSize = 0L;
        this._uncommittedMessages.clear();
    }
    
    public void rollback(final Runnable postRollbackTask) {
        this._rollingBack = true;
        final boolean requiresSuspend = this._suspended.compareAndSet(false, true);
        this.ensureConsumersNoticedStateChange();
        try {
            this._transaction.rollback();
        }
        finally {
            this._rollingBack = false;
            this._txnRejects.incrementAndGet();
            this._txnStarts.incrementAndGet();
            this.decrementOutstandingTxnsIfNecessary();
            this.resetUncommittedMessages();
        }
        postRollbackTask.run();
        for (final MessageInstance entry : this._resendList) {
            final ConsumerImpl sub = entry.getDeliveredConsumer();
            if (sub == null || sub.isClosed()) {
                entry.release();
            }
            else {
                entry.resend();
            }
        }
        this._resendList.clear();
        if (requiresSuspend) {
            this._suspended.set(false);
            for (final ConsumerTarget_0_8 target : this.getConsumerTargets()) {
                for (final ConsumerImpl sub2 : target.getConsumers()) {
                    sub2.externalStateChange();
                }
            }
        }
    }
    
    @Override
    public String toString() {
        return "(" + this._suspended.get() + ", " + this._closing.get() + ", " + this._connection.isClosing() + ") " + "[" + this._connection.toString() + ":" + this._channelId + "]";
    }
    
    public boolean isClosing() {
        return this._closing.get();
    }
    
    public AMQPConnection_0_8 getConnection() {
        return this._connection;
    }
    
    public void setCredit(final long prefetchSize, final int prefetchCount) {
        if (!this._prefetchLoggedForChannel) {
            this.getVirtualHost().getEventLogger().message(ChannelMessages.PREFETCH_SIZE((Number)prefetchSize, (Number)prefetchCount));
            this._prefetchLoggedForChannel = true;
        }
        if (prefetchCount <= 1 && prefetchSize == 0L) {
            this._logChannelFlowMessages = false;
        }
        this._creditManager.setCreditLimits(prefetchSize, prefetchCount);
    }
    
    public MessageStore getMessageStore() {
        return this._messageStore;
    }
    
    public ClientDeliveryMethod getClientDeliveryMethod() {
        return this._clientDeliveryMethod;
    }
    
    public RecordDeliveryMethod getRecordDeliveryMethod() {
        return this._recordDeliveryMethod;
    }
    
    private AMQMessage createAMQMessage(final StoredMessage<MessageMetaData> handle) {
        final AMQMessage message = new AMQMessage(handle, this._connection.getReference());
        return message;
    }
    
    private boolean checkMessageUserId(final ContentHeaderBody header) {
        final AMQShortString userID = header.getProperties().getUserId();
        return !this._messageAuthorizationRequired || this._connection.getAuthorizedPrincipal().getName().equals((userID == null) ? "" : userID.toString());
    }
    
    public UUID getId() {
        return this._id;
    }
    
    public AMQPConnection<?> getAMQPConnection() {
        return (AMQPConnection<?>)this._connection;
    }
    
    public String getClientID() {
        return this._connection.getClientId();
    }
    
    public LogSubject getLogSubject() {
        return this._logSubject;
    }
    
    public int compareTo(final AMQSessionModel o) {
        return this.getId().compareTo(o.getId());
    }
    
    public void addDeleteTask(final Action<? super AMQChannel> task) {
        this._taskList.add(task);
    }
    
    public void removeDeleteTask(final Action<? super AMQChannel> task) {
        this._taskList.remove(task);
    }
    
    public Subject getSubject() {
        return this._subject;
    }
    
    public boolean hasCurrentMessage() {
        return this._currentMessage != null;
    }
    
    public long getMaxUncommittedInMemorySize() {
        return this._maxUncommittedInMemorySize;
    }
    
    public synchronized void block() {
        if (this._blockingEntities.add(this) && this._blocking.compareAndSet(false, true)) {
            this.getVirtualHost().getEventLogger().message(this._logSubject, ChannelMessages.FLOW_ENFORCED("** All Queues **"));
            this.getConnection().notifyWork();
        }
    }
    
    public synchronized void unblock() {
        if (this._blockingEntities.remove(this) && this._blockingEntities.isEmpty() && this._blocking.compareAndSet(true, false)) {
            this.getVirtualHost().getEventLogger().message(this._logSubject, ChannelMessages.FLOW_REMOVED());
            this.getConnection().notifyWork();
        }
    }
    
    public synchronized void block(final AMQQueue queue) {
        if (this._blockingEntities.add(queue) && this._blocking.compareAndSet(false, true)) {
            this.getVirtualHost().getEventLogger().message(this._logSubject, ChannelMessages.FLOW_ENFORCED(queue.getName()));
            this.getConnection().notifyWork();
        }
    }
    
    public synchronized void unblock(final AMQQueue queue) {
        if (this._blockingEntities.remove(queue) && this._blockingEntities.isEmpty() && this._blocking.compareAndSet(true, false) && !this.isClosing()) {
            this.getVirtualHost().getEventLogger().message(this._logSubject, ChannelMessages.FLOW_REMOVED());
            this.getConnection().notifyWork();
        }
    }
    
    public void transportStateChanged() {
        this._creditManager.restoreCredit(0L, 0L);
        this._noAckCreditManager.restoreCredit(0L, 0L);
    }
    
    public Object getConnectionReference() {
        return this.getConnection().getReference();
    }
    
    public int getUnacknowledgedMessageCount() {
        return this.getUnacknowledgedMessageMap().size();
    }
    
    private void flow(final boolean flow) {
        final MethodRegistry methodRegistry = this._connection.getMethodRegistry();
        final AMQMethodBody responseBody = (AMQMethodBody)methodRegistry.createChannelFlowBody(flow);
        this._connection.writeFrame((AMQDataBlock)responseBody.generateFrame(this._channelId));
    }
    
    public boolean getBlocking() {
        return this._blocking.get();
    }
    
    public VirtualHostImpl getVirtualHost() {
        return this.getConnection().getVirtualHost();
    }
    
    public void checkTransactionStatus(final long openWarn, final long openClose, final long idleWarn, final long idleClose) {
        this._transactionTimeoutHelper.checkIdleOrOpenTimes(this._transaction, openWarn, openClose, idleWarn, idleClose);
    }
    
    public void deadLetter(final long deliveryTag) {
        final UnacknowledgedMessageMap unackedMap = this.getUnacknowledgedMessageMap();
        final MessageInstance rejectedQueueEntry = unackedMap.remove(deliveryTag);
        if (rejectedQueueEntry == null) {
            AMQChannel._logger.warn("No message found, unable to DLQ delivery tag: " + deliveryTag);
        }
        else {
            final ServerMessage msg = rejectedQueueEntry.getMessage();
            final int requeues = rejectedQueueEntry.routeToAlternate((Action)new Action<MessageInstance>() {
                public void performAction(final MessageInstance requeueEntry) {
                    AMQChannel.this.getVirtualHost().getEventLogger().message(AMQChannel.this._logSubject, ChannelMessages.DEADLETTERMSG((Number)msg.getMessageNumber(), requeueEntry.getOwningResource().getName()));
                }
            }, (ServerTransaction)null);
            if (requeues == 0) {
                final TransactionLogResource owningResource = rejectedQueueEntry.getOwningResource();
                if (owningResource instanceof AMQQueue) {
                    final AMQQueue queue = (AMQQueue)owningResource;
                    final Exchange altExchange = queue.getAlternateExchange();
                    if (altExchange == null) {
                        if (AMQChannel._logger.isDebugEnabled()) {
                            AMQChannel._logger.debug("No alternate exchange configured for queue, must discard the message as unable to DLQ: delivery tag: " + deliveryTag);
                        }
                        this.getVirtualHost().getEventLogger().message(this._logSubject, ChannelMessages.DISCARDMSG_NOALTEXCH((Number)msg.getMessageNumber(), queue.getName(), msg.getInitialRoutingAddress()));
                    }
                    else {
                        if (AMQChannel._logger.isDebugEnabled()) {
                            AMQChannel._logger.debug("Routing process provided no queues to enqueue the message on, must discard message as unable to DLQ: delivery tag: " + deliveryTag);
                        }
                        this.getVirtualHost().getEventLogger().message(this._logSubject, ChannelMessages.DISCARDMSG_NOROUTE((Number)msg.getMessageNumber(), altExchange.getName()));
                    }
                }
            }
        }
    }
    
    public void recordFuture(final ListenableFuture<Void> future, final ServerTransaction.Action action) {
        this._unfinishedCommandsQueue.add(new AsyncCommand(future, action));
    }
    
    public void sync() {
        if (AMQChannel._logger.isDebugEnabled()) {
            AMQChannel._logger.debug("sync() called on channel " + this.debugIdentity());
        }
        AsyncCommand cmd;
        while ((cmd = this._unfinishedCommandsQueue.poll()) != null) {
            cmd.complete();
        }
        if (this._transaction instanceof LocalTransaction) {
            ((LocalTransaction)this._transaction).sync();
        }
    }
    
    public int getConsumerCount() {
        return this._tag2SubscriptionTargetMap.size();
    }
    
    public Collection<Consumer<?>> getConsumers() {
        return Collections.unmodifiableCollection((Collection<? extends Consumer<?>>)this._consumers);
    }
    
    private void consumerAdded(final Consumer<?> consumer) {
        for (final ConsumerListener l : this._consumerListeners) {
            l.consumerAdded((Consumer)consumer);
        }
    }
    
    private void consumerRemoved(final Consumer<?> consumer) {
        for (final ConsumerListener l : this._consumerListeners) {
            l.consumerRemoved((Consumer)consumer);
        }
    }
    
    public void addConsumerListener(final ConsumerListener listener) {
        this._consumerListeners.add(listener);
    }
    
    public void removeConsumerListener(final ConsumerListener listener) {
        this._consumerListeners.remove(listener);
    }
    
    public void setModelObject(final Session<?> session) {
        this._modelObject = session;
    }
    
    public Session<?> getModelObject() {
        return this._modelObject;
    }
    
    public long getTransactionStartTime() {
        final ServerTransaction serverTransaction = this._transaction;
        if (serverTransaction.isTransactional()) {
            return serverTransaction.getTransactionStartTime();
        }
        return 0L;
    }
    
    public long getTransactionUpdateTime() {
        final ServerTransaction serverTransaction = this._transaction;
        if (serverTransaction.isTransactional()) {
            return serverTransaction.getTransactionUpdateTime();
        }
        return 0L;
    }
    
    public void receiveAccessRequest(final AMQShortString realm, final boolean exclusive, final boolean passive, final boolean active, final boolean write, final boolean read) {
        if (AMQChannel._logger.isDebugEnabled()) {
            AMQChannel._logger.debug("RECV[" + this._channelId + "] AccessRequest[" + " realm: " + realm + " exclusive: " + exclusive + " passive: " + passive + " active: " + active + " write: " + write + " read: " + read + " ]");
        }
        final MethodRegistry methodRegistry = this._connection.getMethodRegistry();
        if (ProtocolVersion.v0_91.equals((Object)this._connection.getProtocolVersion())) {
            this._connection.sendConnectionClose(AMQConstant.COMMAND_INVALID, "AccessRequest not present in AMQP versions other than 0-8, 0-9", this._channelId);
        }
        else {
            final AccessRequestOkBody response = methodRegistry.createAccessRequestOkBody(0);
            this.sync();
            this._connection.writeFrame((AMQDataBlock)response.generateFrame(this._channelId));
        }
    }
    
    public void receiveBasicAck(final long deliveryTag, final boolean multiple) {
        if (AMQChannel._logger.isDebugEnabled()) {
            AMQChannel._logger.debug("RECV[" + this._channelId + "] BasicAck[" + " deliveryTag: " + deliveryTag + " multiple: " + multiple + " ]");
        }
        this.acknowledgeMessage(deliveryTag, multiple);
    }
    
    public void receiveBasicCancel(final AMQShortString consumerTag, final boolean nowait) {
        if (AMQChannel._logger.isDebugEnabled()) {
            AMQChannel._logger.debug("RECV[" + this._channelId + "] BasicCancel[" + " consumerTag: " + consumerTag + " noWait: " + nowait + " ]");
        }
        this.unsubscribeConsumer(consumerTag);
        if (!nowait) {
            final MethodRegistry methodRegistry = this._connection.getMethodRegistry();
            final BasicCancelOkBody cancelOkBody = methodRegistry.createBasicCancelOkBody(consumerTag);
            this.sync();
            this._connection.writeFrame((AMQDataBlock)cancelOkBody.generateFrame(this._channelId));
        }
    }
    
    public void receiveBasicConsume(final AMQShortString queue, final AMQShortString consumerTag, final boolean noLocal, final boolean noAck, final boolean exclusive, final boolean nowait, final FieldTable arguments) {
        if (AMQChannel._logger.isDebugEnabled()) {
            AMQChannel._logger.debug("RECV[" + this._channelId + "] BasicConsume[" + " queue: " + queue + " consumerTag: " + consumerTag + " noLocal: " + noLocal + " noAck: " + noAck + " exclusive: " + exclusive + " nowait: " + nowait + " arguments: " + arguments + " ]");
        }
        AMQShortString consumerTag2 = consumerTag;
        final VirtualHostImpl<?, ?, ?> vHost = this._connection.getVirtualHost();
        this.sync();
        String queueName = AMQShortString.toString(queue);
        final MessageSource queue2 = (MessageSource)((queueName == null) ? this.getDefaultQueue() : vHost.getAttainedMessageSource(queueName));
        final Collection<MessageSource> sources = new HashSet<MessageSource>();
        if (queue2 != null) {
            sources.add(queue2);
        }
        else if ((boolean)vHost.getContextValue((Class)Boolean.class, "qpid.enableMultiQueueConsumers") && arguments != null && arguments.get("x-multiqueue") instanceof Collection) {
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
        if (sources.isEmpty()) {
            if (AMQChannel._logger.isDebugEnabled()) {
                AMQChannel._logger.debug("No queue for '" + queueName + "'");
            }
            if (queueName != null) {
                this.closeChannel(AMQConstant.NOT_FOUND, "No such queue, '" + queueName + "'");
            }
            else {
                this._connection.sendConnectionClose(AMQConstant.NOT_ALLOWED, "No queue name provided, no default queue defined.", this._channelId);
            }
        }
        else {
            try {
                consumerTag2 = this.consumeFromSource(consumerTag2, sources, !noAck, arguments, exclusive, noLocal);
                if (!nowait) {
                    final MethodRegistry methodRegistry = this._connection.getMethodRegistry();
                    final AMQMethodBody responseBody = (AMQMethodBody)methodRegistry.createBasicConsumeOkBody(consumerTag2);
                    this._connection.writeFrame((AMQDataBlock)responseBody.generateFrame(this._channelId));
                }
            }
            catch (ConsumerTagInUseException cte) {
                this._connection.sendConnectionClose(AMQConstant.NOT_ALLOWED, "Non-unique consumer tag, '" + consumerTag2 + "'", this._channelId);
            }
            catch (AMQInvalidArgumentException ise) {
                this._connection.sendConnectionClose(AMQConstant.ARGUMENT_INVALID, ise.getMessage(), this._channelId);
            }
            catch (MessageSource.ExistingExclusiveConsumer e) {
                this._connection.sendConnectionClose(AMQConstant.ACCESS_REFUSED, "Cannot subscribe to queue '" + queue2.getName() + "' as it already has an existing exclusive consumer", this._channelId);
            }
            catch (MessageSource.ExistingConsumerPreventsExclusive e2) {
                this._connection.sendConnectionClose(AMQConstant.ACCESS_REFUSED, "Cannot subscribe to queue '" + queue2.getName() + "' exclusively as it already has a consumer", this._channelId);
            }
            catch (AccessControlException e3) {
                this._connection.sendConnectionClose(AMQConstant.ACCESS_REFUSED, "Cannot subscribe to queue '" + queue2.getName() + "' permission denied", this._channelId);
            }
            catch (MessageSource.ConsumerAccessRefused consumerAccessRefused) {
                this._connection.sendConnectionClose(AMQConstant.ACCESS_REFUSED, "Cannot subscribe to queue '" + queue2.getName() + "' as it already has an incompatible exclusivity policy", this._channelId);
            }
        }
    }
    
    public void receiveBasicGet(final AMQShortString queueName, final boolean noAck) {
        if (AMQChannel._logger.isDebugEnabled()) {
            AMQChannel._logger.debug("RECV[" + this._channelId + "] BasicGet[" + " queue: " + queueName + " noAck: " + noAck + " ]");
        }
        final VirtualHostImpl vHost = this._connection.getVirtualHost();
        this.sync();
        final MessageSource queue = (MessageSource)((queueName == null) ? this.getDefaultQueue() : vHost.getAttainedMessageSource(queueName.toString()));
        if (queue == null) {
            if (AMQChannel._logger.isDebugEnabled()) {
                AMQChannel._logger.debug("No queue for '" + queueName + "'");
            }
            if (queueName != null) {
                this._connection.sendConnectionClose(AMQConstant.NOT_FOUND, "No such queue, '" + queueName + "'", this._channelId);
            }
            else {
                this._connection.sendConnectionClose(AMQConstant.NOT_ALLOWED, "No queue name provided, no default queue defined.", this._channelId);
            }
        }
        else {
            try {
                if (!this.performGet(queue, !noAck)) {
                    final MethodRegistry methodRegistry = this._connection.getMethodRegistry();
                    final BasicGetEmptyBody responseBody = methodRegistry.createBasicGetEmptyBody((AMQShortString)null);
                    this._connection.writeFrame((AMQDataBlock)responseBody.generateFrame(this._channelId));
                }
            }
            catch (AccessControlException e) {
                this._connection.sendConnectionClose(AMQConstant.ACCESS_REFUSED, e.getMessage(), this._channelId);
            }
            catch (MessageSource.ExistingExclusiveConsumer e2) {
                this._connection.sendConnectionClose(AMQConstant.NOT_ALLOWED, "Queue has an exclusive consumer", this._channelId);
            }
            catch (MessageSource.ExistingConsumerPreventsExclusive e3) {
                this._connection.sendConnectionClose(AMQConstant.INTERNAL_ERROR, "The GET request has been evaluated as an exclusive consumer, this is likely due to a programming error in the Qpid broker", this._channelId);
            }
            catch (MessageSource.ConsumerAccessRefused consumerAccessRefused) {
                this._connection.sendConnectionClose(AMQConstant.NOT_ALLOWED, "Queue has an incompatible exclusivity policy", this._channelId);
            }
        }
    }
    
    public void receiveBasicPublish(final AMQShortString exchangeName, final AMQShortString routingKey, final boolean mandatory, final boolean immediate) {
        if (AMQChannel._logger.isDebugEnabled()) {
            AMQChannel._logger.debug("RECV[" + this._channelId + "] BasicPublish[" + " exchange: " + exchangeName + " routingKey: " + routingKey + " mandatory: " + mandatory + " immediate: " + immediate + " ]");
        }
        final VirtualHostImpl vHost = this._connection.getVirtualHost();
        if (this.blockingTimeoutExceeded()) {
            this.getVirtualHost().getEventLogger().message(ChannelMessages.FLOW_CONTROL_IGNORED());
            this.closeChannel(AMQConstant.MESSAGE_TOO_LARGE, "Channel flow control was requested, but not enforced by sender");
        }
        else {
            MessageDestination destination;
            if (this.isDefaultExchange(exchangeName)) {
                destination = vHost.getDefaultDestination();
            }
            else {
                destination = vHost.getAttainedMessageDestination(exchangeName.toString());
            }
            if (destination == null) {
                this.closeChannel(AMQConstant.NOT_FOUND, "Unknown exchange name: '" + exchangeName + "'");
            }
            else {
                final MessagePublishInfo info = new MessagePublishInfo(exchangeName, immediate, mandatory, routingKey);
                try {
                    this.setPublishFrame(info, destination);
                }
                catch (AccessControlException e) {
                    this._connection.sendConnectionClose(AMQConstant.ACCESS_REFUSED, e.getMessage(), this.getChannelId());
                }
            }
        }
    }
    
    private boolean blockingTimeoutExceeded() {
        return this._wireBlockingState && System.currentTimeMillis() - this._blockTime > this._blockingTimeout;
    }
    
    public void receiveBasicQos(final long prefetchSize, final int prefetchCount, final boolean global) {
        if (AMQChannel._logger.isDebugEnabled()) {
            AMQChannel._logger.debug("RECV[" + this._channelId + "] BasicQos[" + " prefetchSize: " + prefetchSize + " prefetchCount: " + prefetchCount + " global: " + global + " ]");
        }
        this.sync();
        this.setCredit(prefetchSize, prefetchCount);
        final MethodRegistry methodRegistry = this._connection.getMethodRegistry();
        final AMQMethodBody responseBody = (AMQMethodBody)methodRegistry.createBasicQosOkBody();
        this._connection.writeFrame((AMQDataBlock)responseBody.generateFrame(this.getChannelId()));
    }
    
    public void receiveBasicRecover(final boolean requeue, final boolean sync) {
        if (AMQChannel._logger.isDebugEnabled()) {
            AMQChannel._logger.debug("RECV[" + this._channelId + "] BasicRecover[" + " requeue: " + requeue + " sync: " + sync + " ]");
        }
        this.resend();
        if (sync) {
            final MethodRegistry methodRegistry = this._connection.getMethodRegistry();
            final AMQMethodBody recoverOk = (AMQMethodBody)methodRegistry.createBasicRecoverSyncOkBody();
            this.sync();
            this._connection.writeFrame((AMQDataBlock)recoverOk.generateFrame(this.getChannelId()));
        }
    }
    
    public void receiveBasicReject(final long deliveryTag, final boolean requeue) {
        if (AMQChannel._logger.isDebugEnabled()) {
            AMQChannel._logger.debug("RECV[" + this._channelId + "] BasicReject[" + " deliveryTag: " + deliveryTag + " requeue: " + requeue + " ]");
        }
        final MessageInstance message = this.getUnacknowledgedMessageMap().get(deliveryTag);
        if (message == null) {
            AMQChannel._logger.warn("Dropping reject request as message is null for tag:" + deliveryTag);
        }
        else if (message.getMessage() == null) {
            AMQChannel._logger.warn("Message has already been purged, unable to Reject.");
        }
        else {
            if (AMQChannel._logger.isDebugEnabled()) {
                AMQChannel._logger.debug("Rejecting: DT:" + deliveryTag + "-" + message.getMessage() + ": Requeue:" + requeue + " on channel:" + this.debugIdentity());
            }
            if (requeue) {
                message.decrementDeliveryCount();
                this.requeue(deliveryTag);
            }
            else {
                final boolean maxDeliveryCountEnabled = this.isMaxDeliveryCountEnabled(deliveryTag);
                if (AMQChannel._logger.isDebugEnabled()) {
                    AMQChannel._logger.debug("maxDeliveryCountEnabled: " + maxDeliveryCountEnabled + " deliveryTag " + deliveryTag);
                }
                if (maxDeliveryCountEnabled) {
                    final boolean deliveredTooManyTimes = this.isDeliveredTooManyTimes(deliveryTag);
                    if (AMQChannel._logger.isDebugEnabled()) {
                        AMQChannel._logger.debug("deliveredTooManyTimes: " + deliveredTooManyTimes + " deliveryTag " + deliveryTag);
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
        if (AMQChannel._logger.isDebugEnabled()) {
            AMQChannel._logger.debug("RECV[" + this._channelId + "] ChannelClose[" + " replyCode: " + replyCode + " replyText: " + replyText + " classId: " + classId + " methodId: " + methodId + " ]");
        }
        this.sync();
        this._connection.closeChannel(this);
        this._connection.writeFrame((AMQDataBlock)new AMQFrame(this.getChannelId(), (AMQBody)this._connection.getMethodRegistry().createChannelCloseOkBody()));
    }
    
    public void receiveChannelCloseOk() {
        if (AMQChannel._logger.isDebugEnabled()) {
            AMQChannel._logger.debug("RECV[" + this._channelId + "] ChannelCloseOk");
        }
        this._connection.closeChannelOk(this.getChannelId());
    }
    
    public void receiveMessageContent(final QpidByteBuffer data) {
        if (AMQChannel._logger.isDebugEnabled()) {
            AMQChannel._logger.debug("RECV[" + this._channelId + "] MessageContent[" + " data: " + Functions.hex(data, this._connection.getBinaryDataLimit()) + " ] ");
        }
        if (this.hasCurrentMessage()) {
            this.publishContentBody(new ContentBody(data));
        }
        else {
            this._connection.sendConnectionClose(AMQConstant.COMMAND_INVALID, "Attempt to send a content header without first sending a publish frame", this._channelId);
        }
    }
    
    public void receiveMessageHeader(final BasicContentHeaderProperties properties, final long bodySize) {
        if (AMQChannel._logger.isDebugEnabled()) {
            AMQChannel._logger.debug("RECV[" + this._channelId + "] MessageHeader[ properties: {" + properties + "} bodySize: " + bodySize + " ]");
        }
        if (this.hasCurrentMessage()) {
            if (bodySize > this._connection.getMaxMessageSize()) {
                this.closeChannel(AMQConstant.MESSAGE_TOO_LARGE, "Message size of " + bodySize + " greater than allowed maximum of " + this._connection.getMaxMessageSize());
            }
            this.publishContentHeader(new ContentHeaderBody(properties, bodySize));
        }
        else {
            this._connection.sendConnectionClose(AMQConstant.COMMAND_INVALID, "Attempt to send a content header without first sending a publish frame", this._channelId);
        }
    }
    
    public boolean ignoreAllButCloseOk() {
        return this._connection.ignoreAllButCloseOk() || this._connection.channelAwaitingClosure(this._channelId);
    }
    
    public void receiveBasicNack(final long deliveryTag, final boolean multiple, final boolean requeue) {
        if (AMQChannel._logger.isDebugEnabled()) {
            AMQChannel._logger.debug("RECV[" + this._channelId + "] BasicNack[" + " deliveryTag: " + deliveryTag + " multiple: " + multiple + " requeue: " + requeue + " ]");
        }
        final Map<Long, MessageInstance> nackedMessageMap = new LinkedHashMap<Long, MessageInstance>();
        this._unacknowledgedMessageMap.collect(deliveryTag, multiple, nackedMessageMap);
        for (final MessageInstance message : nackedMessageMap.values()) {
            if (message == null) {
                AMQChannel._logger.warn("Ignoring nack request as message is null for tag:" + deliveryTag);
            }
            else if (message.getMessage() == null) {
                AMQChannel._logger.warn("Message has already been purged, unable to nack.");
            }
            else {
                if (AMQChannel._logger.isDebugEnabled()) {
                    AMQChannel._logger.debug("Nack-ing: DT:" + deliveryTag + "-" + message.getMessage() + ": Requeue:" + requeue + " on channel:" + this.debugIdentity());
                }
                if (requeue) {
                    message.decrementDeliveryCount();
                    this.requeue(deliveryTag);
                }
                else {
                    message.reject();
                    final boolean maxDeliveryCountEnabled = this.isMaxDeliveryCountEnabled(deliveryTag);
                    if (AMQChannel._logger.isDebugEnabled()) {
                        AMQChannel._logger.debug("maxDeliveryCountEnabled: " + maxDeliveryCountEnabled + " deliveryTag " + deliveryTag);
                    }
                    if (maxDeliveryCountEnabled) {
                        final boolean deliveredTooManyTimes = this.isDeliveredTooManyTimes(deliveryTag);
                        if (AMQChannel._logger.isDebugEnabled()) {
                            AMQChannel._logger.debug("deliveredTooManyTimes: " + deliveredTooManyTimes + " deliveryTag " + deliveryTag);
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
    }
    
    public void receiveChannelFlow(final boolean active) {
        if (AMQChannel._logger.isDebugEnabled()) {
            AMQChannel._logger.debug("RECV[" + this._channelId + "] ChannelFlow[" + " active: " + active + " ]");
        }
        this.sync();
        this.setSuspended(!active);
        final MethodRegistry methodRegistry = this._connection.getMethodRegistry();
        final AMQMethodBody responseBody = (AMQMethodBody)methodRegistry.createChannelFlowOkBody(active);
        this._connection.writeFrame((AMQDataBlock)responseBody.generateFrame(this.getChannelId()));
    }
    
    public void receiveChannelFlowOk(final boolean active) {
        if (AMQChannel._logger.isDebugEnabled()) {
            AMQChannel._logger.debug("RECV[" + this._channelId + "] ChannelFlowOk[" + " active: " + active + " ]");
        }
    }
    
    public void receiveExchangeBound(final AMQShortString exchangeName, final AMQShortString routingKey, final AMQShortString queueName) {
        if (AMQChannel._logger.isDebugEnabled()) {
            AMQChannel._logger.debug("RECV[" + this._channelId + "] ExchangeBound[" + " exchange: " + exchangeName + " routingKey: " + routingKey + " queue: " + queueName + " ]");
        }
        final VirtualHostImpl virtualHost = this._connection.getVirtualHost();
        final MethodRegistry methodRegistry = this._connection.getMethodRegistry();
        this.sync();
        int replyCode;
        String replyText;
        if (this.isDefaultExchange(exchangeName)) {
            if (routingKey == null) {
                if (queueName == null) {
                    replyCode = (virtualHost.getQueues().isEmpty() ? 3 : 0);
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
                replyCode = ((virtualHost.getAttainedQueue(routingKey.toString()) == null) ? 5 : 0);
                replyText = null;
            }
            else {
                final AMQQueue queue2 = virtualHost.getAttainedQueue(queueName.toString());
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
            final ExchangeImpl exchange = virtualHost.getAttainedExchange(exchangeName.toString());
            if (exchange == null) {
                replyCode = 1;
                replyText = "Exchange '" + exchangeName + "' not found";
            }
            else if (routingKey == null) {
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
                    final AMQQueue queue3 = virtualHost.getAttainedQueue(queueName.toString());
                    if (queue3 == null) {
                        replyCode = 2;
                        replyText = "Queue '" + queueName + "' not found";
                    }
                    else if (exchange.isBound(queue3)) {
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
                final AMQQueue queue3 = virtualHost.getAttainedQueue(queueName.toString());
                if (queue3 == null) {
                    replyCode = 2;
                    replyText = "Queue '" + queueName + "' not found";
                }
                else {
                    final String bindingKey = (routingKey == null) ? null : routingKey.toString();
                    if (exchange.isBound(bindingKey, queue3)) {
                        replyCode = 0;
                        replyText = null;
                    }
                    else {
                        replyCode = 6;
                        replyText = "Queue '" + queueName + "' not bound with routing key '" + routingKey + "' to exchange '" + exchangeName + "'";
                    }
                }
            }
            else if (exchange.isBound((routingKey == null) ? "" : routingKey.toString())) {
                replyCode = 0;
                replyText = null;
            }
            else {
                replyCode = 5;
                replyText = "No queue bound with routing key '" + routingKey + "' to exchange '" + exchangeName + "'";
            }
        }
        final ExchangeBoundOkBody exchangeBoundOkBody = methodRegistry.createExchangeBoundOkBody(replyCode, AMQShortString.validValueOf((Object)replyText));
        this._connection.writeFrame((AMQDataBlock)exchangeBoundOkBody.generateFrame(this.getChannelId()));
    }
    
    public void receiveExchangeDeclare(final AMQShortString exchangeName, final AMQShortString type, final boolean passive, final boolean durable, final boolean autoDelete, final boolean internal, final boolean nowait, final FieldTable arguments) {
        if (AMQChannel._logger.isDebugEnabled()) {
            AMQChannel._logger.debug("RECV[" + this._channelId + "] ExchangeDeclare[" + " exchange: " + exchangeName + " type: " + type + " passive: " + passive + " durable: " + durable + " autoDelete: " + autoDelete + " internal: " + internal + " nowait: " + nowait + " arguments: " + arguments + " ]");
        }
        final MethodRegistry methodRegistry = this._connection.getMethodRegistry();
        final AMQMethodBody declareOkBody = (AMQMethodBody)methodRegistry.createExchangeDeclareOkBody();
        final VirtualHostImpl<?, ?, ?> virtualHost = this._connection.getVirtualHost();
        if (this.isDefaultExchange(exchangeName)) {
            if (!new AMQShortString("direct").equals(type)) {
                this._connection.sendConnectionClose(AMQConstant.NOT_ALLOWED, "Attempt to redeclare default exchange:  of type direct to " + type + ".", this.getChannelId());
            }
            else if (!nowait) {
                this.sync();
                this._connection.writeFrame((AMQDataBlock)declareOkBody.generateFrame(this.getChannelId()));
            }
        }
        else if (passive) {
            final ExchangeImpl exchange = virtualHost.getAttainedExchange(exchangeName.toString());
            if (exchange == null) {
                this.closeChannel(AMQConstant.NOT_FOUND, "Unknown exchange: '" + exchangeName + "'");
            }
            else if (type != null && type.length() != 0 && !exchange.getType().equals(type.toString())) {
                this._connection.sendConnectionClose(AMQConstant.NOT_ALLOWED, "Attempt to redeclare exchange: '" + exchangeName + "' of type " + exchange.getType() + " to " + type + ".", this.getChannelId());
            }
            else if (!nowait) {
                this.sync();
                this._connection.writeFrame((AMQDataBlock)declareOkBody.generateFrame(this.getChannelId()));
            }
        }
        else {
            try {
                final String name = exchangeName.toString();
                final String typeString = (type == null) ? null : type.toString();
                final Map<String, Object> attributes = new HashMap<String, Object>();
                if (arguments != null) {
                    attributes.putAll(FieldTable.convertToMap(arguments));
                }
                attributes.put("name", name);
                attributes.put("type", typeString);
                attributes.put("durable", durable);
                attributes.put("lifetimePolicy", autoDelete ? LifetimePolicy.DELETE_ON_NO_LINKS : LifetimePolicy.PERMANENT);
                if (!attributes.containsKey("alternateExchange")) {
                    attributes.put("alternateExchange", null);
                }
                final ExchangeImpl exchange = virtualHost.createExchange((Map)attributes);
                if (!nowait) {
                    this.sync();
                    this._connection.writeFrame((AMQDataBlock)declareOkBody.generateFrame(this.getChannelId()));
                }
            }
            catch (ReservedExchangeNameException e6) {
                final Exchange existing = (Exchange)virtualHost.getAttainedExchange(exchangeName.toString());
                if (existing != null && new AMQShortString(existing.getType()).equals(type)) {
                    this.sync();
                    this._connection.writeFrame((AMQDataBlock)declareOkBody.generateFrame(this.getChannelId()));
                }
                else {
                    this._connection.sendConnectionClose(AMQConstant.NOT_ALLOWED, "Attempt to declare exchange: '" + exchangeName + "' which begins with reserved prefix.", this.getChannelId());
                }
            }
            catch (ExchangeExistsException e) {
                final ExchangeImpl exchange = e.getExistingExchange();
                if (!new AMQShortString(exchange.getType()).equals(type)) {
                    this._connection.sendConnectionClose(AMQConstant.NOT_ALLOWED, "Attempt to redeclare exchange: '" + exchangeName + "' of type " + exchange.getType() + " to " + type + ".", this.getChannelId());
                }
                else if (!nowait) {
                    this.sync();
                    this._connection.writeFrame((AMQDataBlock)declareOkBody.generateFrame(this.getChannelId()));
                }
            }
            catch (NoFactoryForTypeException e2) {
                this._connection.sendConnectionClose(AMQConstant.COMMAND_INVALID, "Unknown exchange type '" + e2.getType() + "' for exchange '" + exchangeName + "'", this.getChannelId());
            }
            catch (AccessControlException e3) {
                this._connection.sendConnectionClose(AMQConstant.ACCESS_REFUSED, e3.getMessage(), this.getChannelId());
            }
            catch (UnknownConfiguredObjectException e4) {
                final String message = "Unknown alternate exchange " + ((e4.getName() != null) ? ("name: '" + e4.getName() + "'") : ("id: " + e4.getId()));
                this._connection.sendConnectionClose(AMQConstant.NOT_FOUND, message, this.getChannelId());
            }
            catch (IllegalArgumentException e5) {
                this._connection.sendConnectionClose(AMQConstant.COMMAND_INVALID, "Error creating exchange '" + exchangeName + "': " + e5.getMessage(), this.getChannelId());
            }
        }
    }
    
    public void receiveExchangeDelete(final AMQShortString exchangeStr, final boolean ifUnused, final boolean nowait) {
        if (AMQChannel._logger.isDebugEnabled()) {
            AMQChannel._logger.debug("RECV[" + this._channelId + "] ExchangeDelete[" + " exchange: " + exchangeStr + " ifUnused: " + ifUnused + " nowait: " + nowait + " ]");
        }
        final VirtualHostImpl virtualHost = this._connection.getVirtualHost();
        this.sync();
        if (this.isDefaultExchange(exchangeStr)) {
            this._connection.sendConnectionClose(AMQConstant.NOT_ALLOWED, "Default Exchange cannot be deleted", this.getChannelId());
        }
        else {
            final String exchangeName = exchangeStr.toString();
            final ExchangeImpl exchange = virtualHost.getAttainedExchange(exchangeName);
            if (exchange == null) {
                this.closeChannel(AMQConstant.NOT_FOUND, "No such exchange: '" + exchangeStr + "'");
            }
            else if (ifUnused && exchange.hasBindings()) {
                this.closeChannel(AMQConstant.IN_USE, "Exchange has bindings");
            }
            else {
                try {
                    exchange.delete();
                    if (!nowait) {
                        final ExchangeDeleteOkBody responseBody = this._connection.getMethodRegistry().createExchangeDeleteOkBody();
                        this._connection.writeFrame((AMQDataBlock)responseBody.generateFrame(this.getChannelId()));
                    }
                }
                catch (ExchangeIsAlternateException e2) {
                    this.closeChannel(AMQConstant.NOT_ALLOWED, "Exchange in use as an alternate exchange");
                }
                catch (RequiredExchangeException e3) {
                    this.closeChannel(AMQConstant.NOT_ALLOWED, "Exchange '" + exchangeStr + "' cannot be deleted");
                }
                catch (AccessControlException e) {
                    this._connection.sendConnectionClose(AMQConstant.ACCESS_REFUSED, e.getMessage(), this.getChannelId());
                }
            }
        }
    }
    
    public void receiveQueueBind(final AMQShortString queueName, final AMQShortString exchange, AMQShortString routingKey, final boolean nowait, final FieldTable argumentsTable) {
        if (AMQChannel._logger.isDebugEnabled()) {
            AMQChannel._logger.debug("RECV[" + this._channelId + "] QueueBind[" + " queue: " + queueName + " exchange: " + exchange + " bindingKey: " + routingKey + " nowait: " + nowait + " arguments: " + argumentsTable + " ]");
        }
        final VirtualHostImpl virtualHost = this._connection.getVirtualHost();
        AMQQueue<?> queue;
        if (queueName == null) {
            queue = (AMQQueue<?>)this.getDefaultQueue();
            if (queue != null && routingKey == null) {
                routingKey = AMQShortString.valueOf(queue.getName());
            }
        }
        else {
            queue = (AMQQueue<?>)virtualHost.getAttainedQueue(queueName.toString());
            routingKey = ((routingKey == null) ? AMQShortString.EMPTY_STRING : routingKey);
        }
        if (queue == null) {
            final String message = (queueName == null) ? "No default queue defined on channel and queue was null" : ("Queue " + queueName + " does not exist.");
            this.closeChannel(AMQConstant.NOT_FOUND, message);
        }
        else if (this.isDefaultExchange(exchange)) {
            this._connection.sendConnectionClose(AMQConstant.NOT_ALLOWED, "Cannot bind the queue '" + queueName + "' to the default exchange", this.getChannelId());
        }
        else {
            final String exchangeName = exchange.toString();
            final ExchangeImpl exch = virtualHost.getAttainedExchange(exchangeName);
            if (exch == null) {
                this.closeChannel(AMQConstant.NOT_FOUND, "Exchange '" + exchangeName + "' does not exist.");
            }
            else {
                try {
                    final Map<String, Object> arguments = (Map<String, Object>)FieldTable.convertToMap(argumentsTable);
                    final String bindingKey = String.valueOf(routingKey);
                    if (!exch.isBound(bindingKey, (Map)arguments, (AMQQueue)queue) && !exch.addBinding(bindingKey, (AMQQueue)queue, (Map)arguments) && "topic".equals(exch.getType())) {
                        exch.replaceBinding(bindingKey, (AMQQueue)queue, (Map)arguments);
                    }
                    if (AMQChannel._logger.isDebugEnabled()) {
                        AMQChannel._logger.debug("Binding queue " + queue + " to exchange " + exch + " with routing key " + routingKey);
                    }
                    if (!nowait) {
                        this.sync();
                        final MethodRegistry methodRegistry = this._connection.getMethodRegistry();
                        final AMQMethodBody responseBody = (AMQMethodBody)methodRegistry.createQueueBindOkBody();
                        this._connection.writeFrame((AMQDataBlock)responseBody.generateFrame(this.getChannelId()));
                    }
                }
                catch (AccessControlException e) {
                    this._connection.sendConnectionClose(AMQConstant.ACCESS_REFUSED, e.getMessage(), this.getChannelId());
                }
            }
        }
    }
    
    public void receiveQueueDeclare(final AMQShortString queueStr, final boolean passive, final boolean durable, final boolean exclusive, final boolean autoDelete, final boolean nowait, final FieldTable arguments) {
        if (AMQChannel._logger.isDebugEnabled()) {
            AMQChannel._logger.debug("RECV[" + this._channelId + "] QueueDeclare[" + " queue: " + queueStr + " passive: " + passive + " durable: " + durable + " exclusive: " + exclusive + " autoDelete: " + autoDelete + " nowait: " + nowait + " arguments: " + arguments + " ]");
        }
        final VirtualHostImpl virtualHost = this._connection.getVirtualHost();
        AMQShortString queueName;
        if (queueStr == null || queueStr.length() == 0) {
            queueName = new AMQShortString("tmp_" + UUID.randomUUID());
        }
        else {
            queueName = queueStr;
        }
        if (passive) {
            final AMQQueue<?> queue = (AMQQueue<?>)virtualHost.getAttainedQueue(queueName.toString());
            if (queue == null) {
                this.closeChannel(AMQConstant.NOT_FOUND, "Queue: '" + queueName + "' not found on VirtualHost '" + virtualHost.getName() + "'.");
            }
            else if (!queue.verifySessionAccess((AMQSessionModel)this)) {
                this._connection.sendConnectionClose(AMQConstant.NOT_ALLOWED, "Queue '" + queue.getName() + "' is exclusive, but not created on this Connection.", this.getChannelId());
            }
            else {
                this.setDefaultQueue(queue);
                if (!nowait) {
                    this.sync();
                    final MethodRegistry methodRegistry = this._connection.getMethodRegistry();
                    final QueueDeclareOkBody responseBody = methodRegistry.createQueueDeclareOkBody(queueName, (long)queue.getQueueDepthMessages(), (long)queue.getConsumerCount());
                    this._connection.writeFrame((AMQDataBlock)responseBody.generateFrame(this.getChannelId()));
                    if (AMQChannel._logger.isDebugEnabled()) {
                        AMQChannel._logger.debug("Queue " + queueName + " declared successfully");
                    }
                }
            }
        }
        else {
            try {
                final Map<String, Object> attributes = (Map<String, Object>)QueueArgumentsConverter.convertWireArgsToModel(FieldTable.convertToMap(arguments));
                final String queueNameString = AMQShortString.toString(queueName);
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
                attributes.put("exclusive", exclusivityPolicy);
                attributes.put("lifetimePolicy", lifetimePolicy);
                final AMQQueue<?> queue = (AMQQueue<?>)virtualHost.createQueue((Map)attributes);
                this.setDefaultQueue(queue);
                if (!nowait) {
                    this.sync();
                    final MethodRegistry methodRegistry2 = this._connection.getMethodRegistry();
                    final QueueDeclareOkBody responseBody2 = methodRegistry2.createQueueDeclareOkBody(queueName, (long)queue.getQueueDepthMessages(), (long)queue.getConsumerCount());
                    this._connection.writeFrame((AMQDataBlock)responseBody2.generateFrame(this.getChannelId()));
                    if (AMQChannel._logger.isDebugEnabled()) {
                        AMQChannel._logger.debug("Queue " + queueName + " declared successfully");
                    }
                }
            }
            catch (QueueExistsException qe) {
                final AMQQueue<?> queue = (AMQQueue<?>)qe.getExistingQueue();
                if (!queue.verifySessionAccess((AMQSessionModel)this)) {
                    this._connection.sendConnectionClose(AMQConstant.NOT_ALLOWED, "Queue '" + queue.getName() + "' is exclusive, but not created on this Connection.", this.getChannelId());
                }
                else if (queue.isExclusive() != exclusive) {
                    this.closeChannel(AMQConstant.ALREADY_EXISTS, "Cannot re-declare queue '" + queue.getName() + "' with different exclusivity (was: " + queue.isExclusive() + " requested " + exclusive + ")");
                }
                else if ((autoDelete && queue.getLifetimePolicy() != LifetimePolicy.DELETE_ON_NO_OUTBOUND_LINKS) || (!autoDelete && queue.getLifetimePolicy() != ((exclusive && !durable) ? LifetimePolicy.DELETE_ON_CONNECTION_CLOSE : LifetimePolicy.PERMANENT))) {
                    this.closeChannel(AMQConstant.ALREADY_EXISTS, "Cannot re-declare queue '" + queue.getName() + "' with different lifetime policy (was: " + queue.getLifetimePolicy() + " requested autodelete: " + autoDelete + ")");
                }
                else {
                    this.setDefaultQueue(queue);
                    if (!nowait) {
                        this.sync();
                        final MethodRegistry methodRegistry3 = this._connection.getMethodRegistry();
                        final QueueDeclareOkBody responseBody3 = methodRegistry3.createQueueDeclareOkBody(queueName, (long)queue.getQueueDepthMessages(), (long)queue.getConsumerCount());
                        this._connection.writeFrame((AMQDataBlock)responseBody3.generateFrame(this.getChannelId()));
                        if (AMQChannel._logger.isDebugEnabled()) {
                            AMQChannel._logger.debug("Queue " + queueName + " declared successfully");
                        }
                    }
                }
            }
            catch (AccessControlException e) {
                this._connection.sendConnectionClose(AMQConstant.ACCESS_REFUSED, e.getMessage(), this.getChannelId());
            }
        }
    }
    
    public void receiveQueueDelete(final AMQShortString queueName, final boolean ifUnused, final boolean ifEmpty, final boolean nowait) {
        if (AMQChannel._logger.isDebugEnabled()) {
            AMQChannel._logger.debug("RECV[" + this._channelId + "] QueueDelete[" + " queue: " + queueName + " ifUnused: " + ifUnused + " ifEmpty: " + ifEmpty + " nowait: " + nowait + " ]");
        }
        final VirtualHostImpl virtualHost = this._connection.getVirtualHost();
        this.sync();
        AMQQueue queue;
        if (queueName == null) {
            queue = this.getDefaultQueue();
        }
        else {
            queue = virtualHost.getAttainedQueue(queueName.toString());
        }
        if (queue == null) {
            this.closeChannel(AMQConstant.NOT_FOUND, "Queue '" + queueName + "' does not exist.");
        }
        else if (ifEmpty && !queue.isEmpty()) {
            this.closeChannel(AMQConstant.IN_USE, "Queue: '" + queueName + "' is not empty.");
        }
        else if (ifUnused && !queue.isUnused()) {
            this.closeChannel(AMQConstant.IN_USE, "Queue: '" + queueName + "' is still used.");
        }
        else if (!queue.verifySessionAccess((AMQSessionModel)this)) {
            this._connection.sendConnectionClose(AMQConstant.NOT_ALLOWED, "Queue '" + queue.getName() + "' is exclusive, but not created on this Connection.", this.getChannelId());
        }
        else {
            try {
                final int purged = virtualHost.removeQueue(queue);
                if (!nowait || this._connection.isSendQueueDeleteOkRegardless()) {
                    final MethodRegistry methodRegistry = this._connection.getMethodRegistry();
                    final QueueDeleteOkBody responseBody = methodRegistry.createQueueDeleteOkBody((long)purged);
                    this._connection.writeFrame((AMQDataBlock)responseBody.generateFrame(this.getChannelId()));
                }
            }
            catch (AccessControlException e) {
                this._connection.sendConnectionClose(AMQConstant.ACCESS_REFUSED, e.getMessage(), this.getChannelId());
            }
        }
    }
    
    public void receiveQueuePurge(final AMQShortString queueName, final boolean nowait) {
        if (AMQChannel._logger.isDebugEnabled()) {
            AMQChannel._logger.debug("RECV[" + this._channelId + "] QueuePurge[" + " queue: " + queueName + " nowait: " + nowait + " ]");
        }
        final VirtualHostImpl virtualHost = this._connection.getVirtualHost();
        AMQQueue queue = null;
        if (queueName == null && (queue = this.getDefaultQueue()) == null) {
            this._connection.sendConnectionClose(AMQConstant.NOT_ALLOWED, "No queue specified.", this.getChannelId());
        }
        else if (queueName != null && (queue = virtualHost.getAttainedQueue(queueName.toString())) == null) {
            this.closeChannel(AMQConstant.NOT_FOUND, "Queue '" + queueName + "' does not exist.");
        }
        else if (!queue.verifySessionAccess((AMQSessionModel)this)) {
            this._connection.sendConnectionClose(AMQConstant.NOT_ALLOWED, "Queue is exclusive, but not created on this Connection.", this.getChannelId());
        }
        else {
            try {
                final long purged = queue.clearQueue();
                if (!nowait) {
                    this.sync();
                    final MethodRegistry methodRegistry = this._connection.getMethodRegistry();
                    final AMQMethodBody responseBody = (AMQMethodBody)methodRegistry.createQueuePurgeOkBody(purged);
                    this._connection.writeFrame((AMQDataBlock)responseBody.generateFrame(this.getChannelId()));
                }
            }
            catch (AccessControlException e) {
                this._connection.sendConnectionClose(AMQConstant.ACCESS_REFUSED, e.getMessage(), this.getChannelId());
            }
        }
    }
    
    public void receiveQueueUnbind(final AMQShortString queueName, final AMQShortString exchange, final AMQShortString bindingKey, final FieldTable arguments) {
        if (AMQChannel._logger.isDebugEnabled()) {
            AMQChannel._logger.debug("RECV[" + this._channelId + "] QueueUnbind[" + " queue: " + queueName + " exchange: " + exchange + " bindingKey: " + bindingKey + " arguments: " + arguments + " ]");
        }
        final VirtualHostImpl virtualHost = this._connection.getVirtualHost();
        final boolean useDefaultQueue = queueName == null;
        final AMQQueue queue = useDefaultQueue ? this.getDefaultQueue() : virtualHost.getAttainedQueue(queueName.toString());
        if (queue == null) {
            final String message = useDefaultQueue ? "No default queue defined on channel and queue was null" : ("Queue '" + queueName + "' does not exist.");
            this.closeChannel(AMQConstant.NOT_FOUND, message);
        }
        else if (this.isDefaultExchange(exchange)) {
            this._connection.sendConnectionClose(AMQConstant.NOT_ALLOWED, "Cannot unbind the queue '" + queue.getName() + "' from the default exchange", this.getChannelId());
        }
        else {
            final ExchangeImpl exch = virtualHost.getAttainedExchange(exchange.toString());
            if (exch == null) {
                this.closeChannel(AMQConstant.NOT_FOUND, "Exchange '" + exchange + "' does not exist.");
            }
            else if (!exch.hasBinding(String.valueOf(bindingKey), queue)) {
                this.closeChannel(AMQConstant.NOT_FOUND, "No such binding");
            }
            else {
                try {
                    exch.deleteBinding(String.valueOf(bindingKey), queue);
                    final AMQMethodBody responseBody = (AMQMethodBody)this._connection.getMethodRegistry().createQueueUnbindOkBody();
                    this.sync();
                    this._connection.writeFrame((AMQDataBlock)responseBody.generateFrame(this.getChannelId()));
                }
                catch (AccessControlException e) {
                    this._connection.sendConnectionClose(AMQConstant.ACCESS_REFUSED, e.getMessage(), this.getChannelId());
                }
            }
        }
    }
    
    public void receiveTxSelect() {
        if (AMQChannel._logger.isDebugEnabled()) {
            AMQChannel._logger.debug("RECV[" + this._channelId + "] TxSelect");
        }
        this.setLocalTransactional();
        final MethodRegistry methodRegistry = this._connection.getMethodRegistry();
        final TxSelectOkBody responseBody = methodRegistry.createTxSelectOkBody();
        this._connection.writeFrame((AMQDataBlock)responseBody.generateFrame(this._channelId));
    }
    
    public void receiveTxCommit() {
        if (AMQChannel._logger.isDebugEnabled()) {
            AMQChannel._logger.debug("RECV[" + this._channelId + "] TxCommit");
        }
        if (!this.isTransactional()) {
            this.closeChannel(AMQConstant.COMMAND_INVALID, "Fatal error: commit called on non-transactional channel");
        }
        this.commit(new Runnable() {
            @Override
            public void run() {
                final MethodRegistry methodRegistry = AMQChannel.this._connection.getMethodRegistry();
                final AMQMethodBody responseBody = (AMQMethodBody)methodRegistry.createTxCommitOkBody();
                AMQChannel.this._connection.writeFrame((AMQDataBlock)responseBody.generateFrame(AMQChannel.this._channelId));
            }
        }, true);
    }
    
    public void receiveTxRollback() {
        if (AMQChannel._logger.isDebugEnabled()) {
            AMQChannel._logger.debug("RECV[" + this._channelId + "] TxRollback");
        }
        if (!this.isTransactional()) {
            this.closeChannel(AMQConstant.COMMAND_INVALID, "Fatal error: rollback called on non-transactional channel");
        }
        final MethodRegistry methodRegistry = this._connection.getMethodRegistry();
        final AMQMethodBody responseBody = (AMQMethodBody)methodRegistry.createTxRollbackOkBody();
        final Runnable task = new Runnable() {
            @Override
            public void run() {
                AMQChannel.this._connection.writeFrame((AMQDataBlock)responseBody.generateFrame(AMQChannel.this._channelId));
            }
        };
        this.rollback(task);
        this.resend();
    }
    
    public void receiveConfirmSelect(final boolean nowait) {
        if (AMQChannel._logger.isDebugEnabled()) {
            AMQChannel._logger.debug("RECV[" + this._channelId + "] ConfirmSelect [ nowait: " + nowait + " ]");
        }
        this._confirmOnPublish = true;
        if (!nowait) {
            this._connection.writeFrame((AMQDataBlock)new AMQFrame(this._channelId, (AMQBody)ConfirmSelectOkBody.INSTANCE));
        }
    }
    
    private void closeChannel(final AMQConstant cause, final String message) {
        this._connection.closeChannelAndWriteFrame(this, cause, message);
    }
    
    private boolean isDefaultExchange(final AMQShortString exchangeName) {
        return exchangeName == null || AMQShortString.EMPTY_STRING.equals(exchangeName);
    }
    
    private void setDefaultQueue(final AMQQueue<?> queue) {
        final AMQQueue<?> currentDefaultQueue = this._defaultQueue;
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
    
    private AMQQueue getDefaultQueue() {
        return this._defaultQueue;
    }
    
    public boolean processPending() {
        if (!this.getAMQPConnection().isIOThread()) {
            return false;
        }
        final boolean desiredBlockingState = this._blocking.get();
        if (desiredBlockingState != this._wireBlockingState) {
            this._wireBlockingState = desiredBlockingState;
            this.flow(!desiredBlockingState);
            this._blockTime = (desiredBlockingState ? System.currentTimeMillis() : 0L);
        }
        boolean consumerListNeedsRefreshing;
        if (this._consumersWithPendingWork.isEmpty()) {
            this._consumersWithPendingWork.addAll(this.getConsumerTargets());
            consumerListNeedsRefreshing = false;
        }
        else {
            consumerListNeedsRefreshing = true;
        }
        final Iterator<ConsumerTarget_0_8> iter = this._consumersWithPendingWork.iterator();
        boolean consumerHasMoreWork = false;
        while (iter.hasNext()) {
            final ConsumerTarget_0_8 target = iter.next();
            iter.remove();
            if (target.hasPendingWork()) {
                consumerHasMoreWork = true;
                target.processPending();
                break;
            }
        }
        return consumerHasMoreWork || consumerListNeedsRefreshing;
    }
    
    public void addTicker(final Ticker ticker) {
        this.getConnection().getAggregateTicker().addTicker(ticker);
        this.getAMQPConnection().notifyWork();
    }
    
    public void removeTicker(final Ticker ticker) {
        this.getConnection().getAggregateTicker().removeTicker(ticker);
    }
    
    public void notifyConsumerTargetCurrentStates() {
        for (final ConsumerTarget_0_8 consumerTarget : this.getConsumerTargets()) {
            consumerTarget.notifyCurrentState();
        }
    }
    
    public void ensureConsumersNoticedStateChange() {
        for (final ConsumerTarget_0_8 consumerTarget : this.getConsumerTargets()) {
            try {
                consumerTarget.getSendLock();
            }
            finally {
                consumerTarget.releaseSendLock();
            }
        }
    }
    
    private Collection<ConsumerTarget_0_8> getConsumerTargets() {
        return this._tag2SubscriptionTargetMap.values();
    }
    
    static {
        _logger = LoggerFactory.getLogger((Class)AMQChannel.class);
        IMMEDIATE_DELIVERY_REPLY_TEXT = new AMQShortString("Immediate delivery is not possible.");
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
        private final FlowCreditManager _singleMessageCredit;
        private final MessageSource _queue;
        private boolean _deliveredMessage;
        
        public GetDeliveryMethod(final FlowCreditManager singleMessageCredit, final MessageSource queue) {
            this._singleMessageCredit = singleMessageCredit;
            this._queue = queue;
        }
        
        @Override
        public long deliverToClient(final ConsumerImpl sub, final ServerMessage message, final InstanceProperties props, final long deliveryTag) {
            this._singleMessageCredit.useCreditForMessage(message.getSize());
            final int queueSize = (this._queue instanceof AMQQueue) ? ((AMQQueue)this._queue).getQueueDepthMessages() : 0;
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
            final TransactionLogResource queue = entry.getOwningResource();
            if (!entry.getDeliveredToConsumer() && entry.acquire()) {
                final ServerTransaction txn = (ServerTransaction)new LocalTransaction(AMQChannel.this._messageStore);
                final AMQMessage message = (AMQMessage)entry.getMessage();
                final MessageReference ref = message.newReference();
                try {
                    entry.delete();
                    txn.dequeue(entry.getEnqueueRecord(), (ServerTransaction.Action)new ServerTransaction.Action() {
                        public void postCommit() {
                            final ProtocolOutputConverter outputConverter = AMQChannel.this._connection.getProtocolOutputConverter();
                            outputConverter.writeReturn(message.getMessagePublishInfo(), message.getContentHeaderBody(), (MessageContentSource)message, AMQChannel.this._channelId, AMQConstant.NO_CONSUMERS.getCode(), AMQChannel.IMMEDIATE_DELIVERY_REPLY_TEXT);
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
            else if (queue instanceof CapacityChecker) {
                ((CapacityChecker)queue).checkCapacity((AMQSessionModel)AMQChannel.this);
            }
        }
    }
    
    private final class CapacityCheckAction implements Action<MessageInstance>
    {
        public void performAction(final MessageInstance entry) {
            final TransactionLogResource queue = entry.getOwningResource();
            if (queue instanceof CapacityChecker) {
                ((CapacityChecker)queue).checkCapacity((AMQSessionModel)AMQChannel.this);
            }
        }
    }
    
    private class MessageAcknowledgeAction implements ServerTransaction.Action
    {
        private Collection<MessageInstance> _ackedMessages;
        
        public MessageAcknowledgeAction(final Collection<MessageInstance> ackedMessages) {
            this._ackedMessages = ackedMessages;
        }
        
        public void postCommit() {
            try {
                for (final MessageInstance entry : this._ackedMessages) {
                    entry.delete();
                }
            }
            finally {
                this._ackedMessages = (Collection<MessageInstance>)Collections.emptySet();
            }
        }
        
        public void onRollback() {
            if (AMQChannel.this._rollingBack) {
                for (final MessageInstance entry : this._ackedMessages) {
                    entry.unlockAcquisition();
                }
                AMQChannel.this._resendList.addAll(this._ackedMessages);
            }
            else {
                try {
                    for (final MessageInstance entry : this._ackedMessages) {
                        entry.release();
                    }
                }
                finally {
                    this._ackedMessages = (Collection<MessageInstance>)Collections.emptySet();
                }
            }
        }
    }
    
    private class WriteReturnAction implements ServerTransaction.Action
    {
        private final AMQConstant _errorCode;
        private final String _description;
        private final MessageReference<AMQMessage> _reference;
        
        public WriteReturnAction(final AMQConstant errorCode, final String description, final AMQMessage message) {
            this._errorCode = errorCode;
            this._description = description;
            this._reference = (MessageReference<AMQMessage>)message.newReference();
        }
        
        public void postCommit() {
            final AMQMessage message = (AMQMessage)this._reference.getMessage();
            AMQChannel.this._connection.getProtocolOutputConverter().writeReturn(message.getMessagePublishInfo(), message.getContentHeaderBody(), (MessageContentSource)message, AMQChannel.this._channelId, this._errorCode.getCode(), AMQShortString.validValueOf((Object)this._description));
            this._reference.release();
        }
        
        public void onRollback() {
            this._reference.release();
        }
    }
    
    private static class AsyncCommand
    {
        private final ListenableFuture<Void> _future;
        private ServerTransaction.Action _action;
        
        public AsyncCommand(final ListenableFuture<Void> future, final ServerTransaction.Action action) {
            this._future = future;
            this._action = action;
        }
        
        void complete() {
            boolean interrupted = false;
            try {
                while (true) {
                    try {
                        this._future.get();
                    }
                    catch (InterruptedException e2) {
                        interrupted = true;
                        continue;
                    }
                    break;
                }
            }
            catch (ExecutionException e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException)e.getCause();
                }
                if (e.getCause() instanceof Error) {
                    throw (Error)e.getCause();
                }
                throw new ServerScopedRuntimeException(e.getCause());
            }
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
            this._action.postCommit();
            this._action = null;
        }
    }
    
    private class ConsumerClosedListener implements ConfigurationChangeListener
    {
        public void stateChanged(final ConfiguredObject object, final State oldState, final State newState) {
            if (newState == State.DELETED) {
                AMQChannel.this.consumerRemoved((Consumer<?>)object);
            }
        }
        
        public void childAdded(final ConfiguredObject object, final ConfiguredObject child) {
        }
        
        public void childRemoved(final ConfiguredObject object, final ConfiguredObject child) {
        }
        
        public void attributeSet(final ConfiguredObject object, final String attributeName, final Object oldAttributeValue, final Object newAttributeValue) {
        }
        
        public void bulkChangeStart(final ConfiguredObject<?> object) {
        }
        
        public void bulkChangeEnd(final ConfiguredObject<?> object) {
        }
    }
    
    private class DefaultQueueAssociationClearingTask implements Action<AMQQueue>
    {
        public void performAction(final AMQQueue queue) {
            if (queue == AMQChannel.this._defaultQueue) {
                AMQChannel.this._defaultQueue = null;
            }
        }
    }
}
