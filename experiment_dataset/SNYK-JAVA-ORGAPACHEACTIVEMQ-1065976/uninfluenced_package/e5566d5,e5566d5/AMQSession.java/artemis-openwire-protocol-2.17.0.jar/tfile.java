// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.activemq.artemis.core.protocol.openwire.amq;

import org.apache.activemq.command.MessageDispatch;
import org.apache.activemq.artemis.core.protocol.openwire.util.OpenWireUtil;
import java.io.IOException;
import org.apache.activemq.command.Response;
import org.apache.activemq.artemis.core.io.IOCallback;
import org.apache.activemq.artemis.core.paging.PagingStore;
import org.apache.activemq.command.Command;
import org.apache.activemq.command.ProducerAck;
import javax.jms.ResourceAllocationException;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.activemq.artemis.reader.MessageUtil;
import org.apache.activemq.wireformat.WireFormat;
import org.apache.activemq.artemis.core.protocol.openwire.OpenWireMessageConverter;
import org.apache.activemq.command.ProducerInfo;
import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.spi.core.remoting.ReadyListener;
import org.apache.activemq.artemis.core.server.QueueQueryResult;
import org.apache.activemq.artemis.core.server.BindingQueryResult;
import org.apache.activemq.artemis.api.core.ActiveMQQueueExistsException;
import org.apache.activemq.artemis.api.core.QueueConfiguration;
import org.apache.activemq.artemis.core.settings.impl.AddressSettings;
import org.apache.activemq.artemis.utils.CompositeAddress;
import org.apache.activemq.artemis.api.core.RoutingType;
import javax.jms.InvalidDestinationException;
import org.apache.activemq.advisory.AdvisorySupport;
import java.util.LinkedList;
import org.apache.activemq.command.ActiveMQDestination;
import java.util.List;
import org.apache.activemq.artemis.core.server.SlowConsumerDetectionListener;
import org.apache.activemq.command.ConsumerInfo;
import org.apache.activemq.artemis.core.server.MessageReference;
import org.apache.activemq.artemis.core.server.ServerConsumer;
import org.apache.activemq.artemis.core.server.ActiveMQServerLogger;
import java.util.Map;
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;
import org.apache.activemq.artemis.utils.SimpleIDGenerator;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.persistence.CoreMessageObjectPools;
import org.apache.activemq.artemis.core.protocol.openwire.OpenWireProtocolManager;
import org.apache.activemq.openwire.OpenWireFormat;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.activemq.artemis.core.protocol.openwire.OpenWireConnection;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.command.SessionInfo;
import org.apache.activemq.artemis.core.server.ServerSession;
import org.apache.activemq.command.ConnectionInfo;
import org.apache.activemq.artemis.utils.IDGenerator;
import org.jboss.logging.Logger;
import org.apache.activemq.artemis.spi.core.protocol.SessionCallback;

public class AMQSession implements SessionCallback
{
    private final Logger logger;
    protected final IDGenerator consumerIDGenerator;
    private final ConnectionInfo connInfo;
    private ServerSession coreSession;
    private final SessionInfo sessInfo;
    private final ActiveMQServer server;
    private final OpenWireConnection connection;
    private final AtomicBoolean started;
    private final ScheduledExecutorService scheduledPool;
    private final OpenWireFormat protocolManagerWireFormat;
    private final OpenWireProtocolManager protocolManager;
    private final Runnable enableAutoReadAndTtl;
    private final CoreMessageObjectPools coreMessageObjectPools;
    private String[] existingQueuesCache;
    private final SimpleString clientId;
    
    public AMQSession(final ConnectionInfo connInfo, final SessionInfo sessInfo, final ActiveMQServer server, final OpenWireConnection connection, final OpenWireProtocolManager protocolManager, final CoreMessageObjectPools coreMessageObjectPools) {
        this.logger = Logger.getLogger((Class)AMQSession.class);
        this.consumerIDGenerator = (IDGenerator)new SimpleIDGenerator(0L);
        this.started = new AtomicBoolean(false);
        this.connInfo = connInfo;
        this.sessInfo = sessInfo;
        this.clientId = SimpleString.toSimpleString(connInfo.getClientId());
        this.server = server;
        this.connection = connection;
        this.protocolManager = protocolManager;
        this.scheduledPool = protocolManager.getScheduledPool();
        this.protocolManagerWireFormat = protocolManager.wireFormat().copy();
        this.enableAutoReadAndTtl = this::enableAutoReadAndTtl;
        this.existingQueuesCache = null;
        this.coreMessageObjectPools = coreMessageObjectPools;
    }
    
    public boolean isClosed() {
        return this.coreSession.isClosed();
    }
    
    public OpenWireFormat wireFormat() {
        return this.protocolManagerWireFormat;
    }
    
    public void initialize() {
        final String name = this.sessInfo.getSessionId().toString();
        final String username = this.connInfo.getUserName();
        final String password = this.connInfo.getPassword();
        final int minLargeMessageSize = Integer.MAX_VALUE;
        try {
            this.coreSession = this.server.createSession(name, username, password, minLargeMessageSize, (RemotingConnection)this.connection, true, false, false, false, (String)null, (SessionCallback)this, true, this.connection.getOperationContext(), (Map)this.protocolManager.getPrefixes(), this.protocolManager.getSecurityDomain());
        }
        catch (Exception e) {
            ActiveMQServerLogger.LOGGER.error((Object)"error init session", (Throwable)e);
        }
    }
    
    public boolean supportsDirectDelivery() {
        return false;
    }
    
    public boolean updateDeliveryCountAfterCancel(final ServerConsumer consumer, final MessageReference ref, final boolean failed) {
        return consumer.getProtocolData() != null && ((AMQConsumer)consumer.getProtocolData()).updateDeliveryCountAfterCancel(ref);
    }
    
    public List<AMQConsumer> createConsumer(final ConsumerInfo info, final SlowConsumerDetectionListener slowConsumerDetectionListener) throws Exception {
        final ActiveMQDestination dest = info.getDestination();
        ActiveMQDestination[] dests = null;
        if (dest.isComposite()) {
            dests = dest.getCompositeDestinations();
        }
        else {
            dests = new ActiveMQDestination[] { dest };
        }
        final List<AMQConsumer> consumersList = new LinkedList<AMQConsumer>();
        for (ActiveMQDestination openWireDest : dests) {
            boolean isInternalAddress = false;
            Label_0225: {
                if (AdvisorySupport.isAdvisoryTopic(dest)) {
                    if (!this.connection.isSuppportAdvisory()) {
                        break Label_0225;
                    }
                    isInternalAddress = this.connection.isSuppressInternalManagementObjects();
                }
                if (openWireDest.isQueue()) {
                    openWireDest = this.protocolManager.virtualTopicConsumerToFQQN(openWireDest);
                    final SimpleString queueName = new SimpleString(this.convertWildcard(openWireDest));
                    if (!this.checkAutoCreateQueue(queueName, openWireDest.isTemporary())) {
                        throw new InvalidDestinationException("Destination doesn't exist: " + queueName);
                    }
                }
                final AMQConsumer consumer = new AMQConsumer(this, openWireDest, info, this.scheduledPool, isInternalAddress);
                final long nativeID = this.consumerIDGenerator.generateID();
                consumer.init(slowConsumerDetectionListener, nativeID);
                consumersList.add(consumer);
            }
        }
        return consumersList;
    }
    
    private boolean checkCachedExistingQueues(final SimpleString address, final String physicalName, final boolean isTemporary) throws Exception {
        String[] existingQueuesCache = this.existingQueuesCache;
        if (existingQueuesCache == null) {
            existingQueuesCache = new String[16];
            assert Integer.bitCount(existingQueuesCache.length) == 1 : "existingQueuesCache.length must be power of 2";
            this.existingQueuesCache = existingQueuesCache;
        }
        final int hashCode = physicalName.hashCode();
        final int mask = existingQueuesCache.length - 1;
        final int index = hashCode & mask;
        final String existingQueue = existingQueuesCache[index];
        if (existingQueue != null && existingQueue.equals(physicalName)) {
            return true;
        }
        final boolean hasQueue = this.checkAutoCreateQueue(address, isTemporary);
        if (hasQueue) {
            existingQueuesCache[index] = physicalName;
        }
        return hasQueue;
    }
    
    private boolean checkAutoCreateQueue(final SimpleString queueName, final boolean isTemporary) throws Exception {
        boolean hasQueue = true;
        if (!this.connection.containsKnownDestination(queueName)) {
            final BindingQueryResult bindingQuery = this.server.bindingQuery(queueName);
            final QueueQueryResult queueBinding = this.server.queueQuery(queueName);
            try {
                if (!queueBinding.isExists()) {
                    if (bindingQuery.isAutoCreateQueues()) {
                        SimpleString queueNameToUse = queueName;
                        SimpleString addressToUse = queueName;
                        RoutingType routingTypeToUse = RoutingType.ANYCAST;
                        if (CompositeAddress.isFullyQualified(queueName.toString())) {
                            addressToUse = CompositeAddress.extractAddressName(queueName);
                            queueNameToUse = CompositeAddress.extractQueueName(queueName);
                            if (bindingQuery.getAddressInfo() != null) {
                                routingTypeToUse = bindingQuery.getAddressInfo().getRoutingType();
                            }
                            else {
                                final AddressSettings as = (AddressSettings)this.server.getAddressSettingsRepository().getMatch(addressToUse.toString());
                                routingTypeToUse = as.getDefaultAddressRoutingType();
                            }
                        }
                        this.coreSession.createQueue(new QueueConfiguration(queueNameToUse).setAddress(addressToUse).setRoutingType(routingTypeToUse).setTemporary(Boolean.valueOf(isTemporary)).setAutoCreated(Boolean.valueOf(true)));
                        this.connection.addKnownDestination(queueName);
                    }
                    else {
                        hasQueue = false;
                    }
                }
            }
            catch (ActiveMQQueueExistsException e) {
                hasQueue = true;
            }
        }
        return hasQueue;
    }
    
    public void start() {
        this.coreSession.start();
        this.started.set(true);
    }
    
    public void afterDelivery() throws Exception {
    }
    
    public void browserFinished(final ServerConsumer consumer) {
        final AMQConsumer theConsumer = (AMQConsumer)consumer.getProtocolData();
        if (theConsumer != null) {
            theConsumer.browseFinished();
        }
    }
    
    public boolean isWritable(final ReadyListener callback, final Object protocolContext) {
        return this.connection.isWritable(callback);
    }
    
    public void sendProducerCreditsMessage(final int credits, final SimpleString address) {
    }
    
    public void sendProducerCreditsFailMessage(final int credits, final SimpleString address) {
    }
    
    public int sendMessage(final MessageReference reference, final Message message, final ServerConsumer consumer, final int deliveryCount) {
        final AMQConsumer theConsumer = (AMQConsumer)consumer.getProtocolData();
        theConsumer.removeRolledback(reference);
        return theConsumer.handleDeliver(reference, message.toCore(), deliveryCount);
    }
    
    public int sendLargeMessage(final MessageReference reference, final Message message, final ServerConsumer consumerID, final long bodySize, final int deliveryCount) {
        return 0;
    }
    
    public int sendLargeMessageContinuation(final ServerConsumer consumerID, final byte[] body, final boolean continues, final boolean requiresResponse) {
        return 0;
    }
    
    public void closed() {
    }
    
    public boolean hasCredits(final ServerConsumer consumer) {
        AMQConsumer amqConsumer = null;
        if (consumer.getProtocolData() != null) {
            amqConsumer = (AMQConsumer)consumer.getProtocolData();
        }
        return amqConsumer != null && amqConsumer.hasCredits();
    }
    
    public void disconnect(final ServerConsumer consumerId, final SimpleString queueName) {
    }
    
    public void send(final ProducerInfo producerInfo, final org.apache.activemq.command.Message messageSend, final boolean sendProducerAck) throws Exception {
        messageSend.setBrokerInTime(System.currentTimeMillis());
        final ActiveMQDestination destination = messageSend.getDestination();
        ActiveMQDestination[] actualDestinations;
        int actualDestinationsCount;
        if (destination.isComposite()) {
            actualDestinations = destination.getCompositeDestinations();
            messageSend.setOriginalDestination(destination);
            actualDestinationsCount = actualDestinations.length;
        }
        else {
            actualDestinations = null;
            actualDestinationsCount = 1;
        }
        final Message originalCoreMsg = OpenWireMessageConverter.inbound(messageSend, (WireFormat)this.protocolManagerWireFormat, this.coreMessageObjectPools);
        assert this.clientId.toString().equals(this.connection.getState().getInfo().getClientId()) : "Session cached clientId must be the same of the connection";
        originalCoreMsg.putStringProperty(MessageUtil.CONNECTION_ID_PROPERTY_NAME, this.clientId);
        if (this.connection.getContext().isFaultTolerant() && !messageSend.getProperties().containsKey(Message.HDR_DUPLICATE_DETECTION_ID.toString())) {
            originalCoreMsg.putStringProperty(Message.HDR_DUPLICATE_DETECTION_ID, SimpleString.toSimpleString(messageSend.getMessageId().toString()));
        }
        final boolean shouldBlockProducer = producerInfo.getWindowSize() > 0 || messageSend.isResponseRequired();
        final AtomicInteger count = (actualDestinations != null) ? new AtomicInteger(actualDestinationsCount) : null;
        if (shouldBlockProducer) {
            this.connection.getContext().setDontSendReponse(true);
        }
        for (int i = 0; i < actualDestinationsCount; ++i) {
            final ActiveMQDestination dest = (actualDestinations != null) ? actualDestinations[i] : destination;
            final String physicalName = dest.getPhysicalName();
            final SimpleString address = SimpleString.toSimpleString(physicalName, this.coreMessageObjectPools.getAddressStringSimpleStringPool());
            final Message coreMsg = (i == actualDestinationsCount - 1) ? originalCoreMsg : originalCoreMsg.copy();
            coreMsg.setAddress(address);
            if (dest.isQueue()) {
                this.checkCachedExistingQueues(address, physicalName, dest.isTemporary());
                coreMsg.setRoutingType(RoutingType.ANYCAST);
            }
            else {
                coreMsg.setRoutingType(RoutingType.MULTICAST);
            }
            final PagingStore store = this.server.getPagingManager().getPageStore(address);
            this.connection.disableTtl();
            if (shouldBlockProducer) {
                this.sendShouldBlockProducer(producerInfo, messageSend, sendProducerAck, store, dest, count, coreMsg, address);
            }
            else {
                this.connection.getTransportConnection().setAutoRead(false);
                if (store != null) {
                    if (!store.checkMemory(this.enableAutoReadAndTtl)) {
                        this.enableAutoReadAndTtl();
                        throw new ResourceAllocationException("Queue is full " + address);
                    }
                }
                else {
                    this.enableAutoReadAndTtl.run();
                }
                this.getCoreSession().send(coreMsg, false, dest.isTemporary());
                if ((count == null || count.decrementAndGet() == 0) && sendProducerAck) {
                    final ProducerAck ack = new ProducerAck(producerInfo.getProducerId(), messageSend.getSize());
                    this.connection.dispatchAsync((Command)ack);
                }
            }
        }
    }
    
    private void sendShouldBlockProducer(final ProducerInfo producerInfo, final org.apache.activemq.command.Message messageSend, final boolean sendProducerAck, final PagingStore store, final ActiveMQDestination dest, final AtomicInteger count, final Message coreMsg, final SimpleString address) throws ResourceAllocationException {
        Exception exceptionToSend;
        final Runnable task = () -> {
            exceptionToSend = null;
            try {
                this.getCoreSession().send(coreMsg, false, dest.isTemporary());
            }
            catch (Exception e) {
                this.logger.warn((Object)e.getMessage(), (Throwable)e);
                exceptionToSend = e;
            }
            this.connection.enableTtl();
            if (count == null || count.decrementAndGet() == 0) {
                if (exceptionToSend != null) {
                    this.connection.getContext().setDontSendReponse(false);
                    this.connection.sendException(exceptionToSend);
                }
                else {
                    this.server.getStorageManager().afterCompleteOperations((IOCallback)new IOCallback() {
                        final /* synthetic */ boolean val$sendProducerAck;
                        final /* synthetic */ ProducerInfo val$producerInfo;
                        final /* synthetic */ org.apache.activemq.command.Message val$messageSend;
                        
                        public void done() {
                            if (this.val$sendProducerAck) {
                                try {
                                    final ProducerAck ack = new ProducerAck(this.val$producerInfo.getProducerId(), this.val$messageSend.getSize());
                                    AMQSession.this.connection.dispatchAsync((Command)ack);
                                }
                                catch (Exception e) {
                                    AMQSession.this.connection.getContext().setDontSendReponse(false);
                                    ActiveMQServerLogger.LOGGER.warn((Object)e.getMessage(), (Throwable)e);
                                    AMQSession.this.connection.sendException(e);
                                }
                            }
                            else {
                                AMQSession.this.connection.getContext().setDontSendReponse(false);
                                try {
                                    final Response response = new Response();
                                    response.setCorrelationId(this.val$messageSend.getCommandId());
                                    AMQSession.this.connection.dispatchAsync((Command)response);
                                }
                                catch (Exception e) {
                                    ActiveMQServerLogger.LOGGER.warn((Object)e.getMessage(), (Throwable)e);
                                    AMQSession.this.connection.sendException(e);
                                }
                            }
                        }
                        
                        public void onError(final int errorCode, final String errorMessage) {
                            try {
                                final IOException e = new IOException(errorMessage);
                                ActiveMQServerLogger.LOGGER.warn((Object)errorMessage);
                                AMQSession.this.connection.serviceException(e);
                            }
                            catch (Exception ex) {
                                ActiveMQServerLogger.LOGGER.debug((Object)ex);
                            }
                        }
                    });
                }
            }
            return;
        };
        if (store != null) {
            if (!store.checkMemory(false, task)) {
                this.connection.getContext().setDontSendReponse(false);
                this.connection.enableTtl();
                throw new ResourceAllocationException("Queue is full " + address);
            }
        }
        else {
            task.run();
        }
    }
    
    private void enableAutoReadAndTtl() {
        this.connection.getTransportConnection().setAutoRead(true);
        this.connection.enableTtl();
    }
    
    public String convertWildcard(final ActiveMQDestination openWireDest) {
        if (openWireDest.isTemporary() || AdvisorySupport.isAdvisoryTopic(openWireDest)) {
            return openWireDest.getPhysicalName();
        }
        return OpenWireUtil.OPENWIRE_WILDCARD.convert(openWireDest.getPhysicalName(), this.server.getConfiguration().getWildcardConfiguration());
    }
    
    public ServerSession getCoreSession() {
        return this.coreSession;
    }
    
    public ActiveMQServer getCoreServer() {
        return this.server;
    }
    
    public ConnectionInfo getConnectionInfo() {
        return this.connInfo;
    }
    
    public void disableSecurity() {
        this.coreSession.disableSecurity();
    }
    
    public void deliverMessage(final MessageDispatch dispatch) {
        this.connection.deliverMessage(dispatch);
    }
    
    public void close() throws Exception {
        this.coreSession.close(false);
    }
    
    public OpenWireConnection getConnection() {
        return this.connection;
    }
    
    public boolean isInternal() {
        return this.sessInfo.getSessionId().getValue() == -1L;
    }
}
