// 
// Decompiled by Procyon v0.5.36
// 

package com.rabbitmq.jms.client;

import javax.jms.Destination;
import javax.jms.Queue;
import javax.jms.QueueSession;
import javax.jms.ConnectionConsumer;
import javax.jms.ServerSessionPool;
import javax.jms.Topic;
import javax.jms.TopicSession;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import com.rabbitmq.jms.util.RMQJMSException;
import com.rabbitmq.client.ShutdownSignalException;
import java.util.Iterator;
import javax.jms.InvalidClientIDException;
import javax.jms.IllegalStateException;
import javax.jms.JMSException;
import javax.jms.Session;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.jms.util.WhiteListObjectInputStream;
import java.util.Collections;
import java.util.ArrayList;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.List;
import javax.jms.ExceptionListener;
import java.util.concurrent.atomic.AtomicReference;
import javax.jms.ConnectionMetaData;
import org.slf4j.Logger;
import javax.jms.TopicConnection;
import javax.jms.QueueConnection;
import javax.jms.Connection;

public class RMQConnection implements Connection, QueueConnection, TopicConnection
{
    public static final int NO_CHANNEL_QOS = -1;
    private final Logger logger;
    private final com.rabbitmq.client.Connection rabbitConnection;
    private static final ConnectionMetaData connectionMetaData;
    private String clientID;
    private final AtomicReference<ExceptionListener> exceptionListener;
    private final List<RMQSession> sessions;
    private volatile boolean closed;
    private final AtomicBoolean stopped;
    private final long terminationTimeout;
    private final int queueBrowserReadMax;
    private final int onMessageTimeoutMs;
    private static ConcurrentHashMap<String, String> CLIENT_IDS;
    private final Map<String, RMQMessageConsumer> subscriptions;
    private volatile boolean canSetClientID;
    private final int channelsQos;
    private boolean preferProducerMessageProperty;
    private boolean requeueOnMessageListenerException;
    private final boolean cleanUpServerNamedQueuesForNonDurableTopicsOnSessionClose;
    private final AmqpPropertiesCustomiser amqpPropertiesCustomiser;
    private final boolean throwExceptionOnConsumerStartFailure;
    private final SendingContextConsumer sendingContextConsumer;
    private final ReceivingContextConsumer receivingContextConsumer;
    private final ConfirmListener confirmListener;
    private List<String> trustedPackages;
    private static final long FIFTEEN_SECONDS_MS = 15000L;
    private static final int TWO_SECONDS_MS = 2000;
    
    public RMQConnection(final ConnectionParams connectionParams) {
        this.logger = LoggerFactory.getLogger((Class)RMQConnection.class);
        this.exceptionListener = new AtomicReference<ExceptionListener>();
        this.sessions = Collections.synchronizedList(new ArrayList<RMQSession>());
        this.closed = false;
        this.stopped = new AtomicBoolean(true);
        this.subscriptions = new ConcurrentHashMap<String, RMQMessageConsumer>();
        this.canSetClientID = true;
        this.trustedPackages = WhiteListObjectInputStream.DEFAULT_TRUSTED_PACKAGES;
        connectionParams.getRabbitConnection().addShutdownListener((ShutdownListener)new RMQConnectionShutdownListener());
        this.rabbitConnection = connectionParams.getRabbitConnection();
        this.terminationTimeout = connectionParams.getTerminationTimeout();
        this.queueBrowserReadMax = connectionParams.getQueueBrowserReadMax();
        this.onMessageTimeoutMs = connectionParams.getOnMessageTimeoutMs();
        this.channelsQos = connectionParams.getChannelsQos();
        this.preferProducerMessageProperty = connectionParams.willPreferProducerMessageProperty();
        this.requeueOnMessageListenerException = connectionParams.willRequeueOnMessageListenerException();
        this.cleanUpServerNamedQueuesForNonDurableTopicsOnSessionClose = connectionParams.isCleanUpServerNamedQueuesForNonDurableTopicsOnSessionClose();
        this.amqpPropertiesCustomiser = connectionParams.getAmqpPropertiesCustomiser();
        this.throwExceptionOnConsumerStartFailure = connectionParams.willThrowExceptionOnConsumerStartFailure();
        this.sendingContextConsumer = connectionParams.getSendingContextConsumer();
        this.receivingContextConsumer = connectionParams.getReceivingContextConsumer();
        this.confirmListener = connectionParams.getConfirmListener();
    }
    
    public RMQConnection(final com.rabbitmq.client.Connection rabbitConnection, final long terminationTimeout, final int queueBrowserReadMax, final int onMessageTimeoutMs) {
        this(new ConnectionParams().setRabbitConnection(rabbitConnection).setTerminationTimeout(terminationTimeout).setQueueBrowserReadMax(queueBrowserReadMax).setOnMessageTimeoutMs(onMessageTimeoutMs));
    }
    
    public RMQConnection(final com.rabbitmq.client.Connection rabbitConnection) {
        this(rabbitConnection, 15000L, 0, 2000);
    }
    
    int getQueueBrowserReadMax() {
        return this.queueBrowserReadMax;
    }
    
    public Session createSession(final boolean transacted, final int acknowledgeMode) throws JMSException {
        this.logger.trace("transacted={}, acknowledgeMode={}", (Object)transacted, (Object)acknowledgeMode);
        this.illegalStateExceptionIfClosed();
        this.freezeClientID();
        final RMQSession session = new RMQSession(new SessionParams().setConnection(this).setTransacted(transacted).setOnMessageTimeoutMs(this.onMessageTimeoutMs).setMode(acknowledgeMode).setSubscriptions(this.subscriptions).setPreferProducerMessageProperty(this.preferProducerMessageProperty).setRequeueOnMessageListenerException(this.requeueOnMessageListenerException).setCleanUpServerNamedQueuesForNonDurableTopics(this.cleanUpServerNamedQueuesForNonDurableTopicsOnSessionClose).setAmqpPropertiesCustomiser(this.amqpPropertiesCustomiser).setThrowExceptionOnConsumerStartFailure(this.throwExceptionOnConsumerStartFailure).setSendingContextConsumer(this.sendingContextConsumer).setReceivingContextConsumer(this.receivingContextConsumer).setConfirmListener(this.confirmListener));
        session.setTrustedPackages(this.trustedPackages);
        this.sessions.add(session);
        return (Session)session;
    }
    
    private void freezeClientID() {
        this.canSetClientID = false;
    }
    
    private void illegalStateExceptionIfClosed() throws IllegalStateException {
        if (this.closed) {
            throw new IllegalStateException("Connection is closed");
        }
    }
    
    public String getClientID() throws JMSException {
        this.illegalStateExceptionIfClosed();
        return this.clientID;
    }
    
    public void setClientID(final String clientID) throws JMSException {
        this.logger.trace("set ClientID to '{}'", (Object)clientID);
        this.illegalStateExceptionIfClosed();
        if (!this.canSetClientID) {
            throw new IllegalStateException("Client ID can only be set right after connection creation");
        }
        if (this.clientID != null) {
            throw new IllegalStateException("Client ID already set.");
        }
        if (RMQConnection.CLIENT_IDS.putIfAbsent(clientID, clientID) == null) {
            this.clientID = clientID;
            return;
        }
        throw new InvalidClientIDException(String.format("A connection with client ID [%s] already exists.", clientID));
    }
    
    public List<String> getTrustedPackages() {
        return this.trustedPackages;
    }
    
    public void setTrustedPackages(final List<String> value) {
        this.trustedPackages = value;
    }
    
    public ConnectionMetaData getMetaData() throws JMSException {
        this.illegalStateExceptionIfClosed();
        this.freezeClientID();
        return RMQConnection.connectionMetaData;
    }
    
    public ExceptionListener getExceptionListener() throws JMSException {
        this.illegalStateExceptionIfClosed();
        this.freezeClientID();
        return this.exceptionListener.get();
    }
    
    public void setExceptionListener(final ExceptionListener listener) throws JMSException {
        this.logger.trace("set ExceptionListener ({}) on connection ({})", (Object)listener, (Object)this);
        this.illegalStateExceptionIfClosed();
        this.freezeClientID();
        this.exceptionListener.set(listener);
    }
    
    public void start() throws JMSException {
        this.logger.trace("starting connection ({})", (Object)this);
        this.illegalStateExceptionIfClosed();
        this.freezeClientID();
        if (this.stopped.compareAndSet(true, false)) {
            for (final RMQSession session : this.sessions) {
                session.resume();
            }
        }
    }
    
    public void stop() throws JMSException {
        this.logger.trace("stopping connection ({})", (Object)this);
        this.illegalStateExceptionIfClosed();
        this.freezeClientID();
        if (this.stopped.compareAndSet(false, true)) {
            for (final RMQSession session : this.sessions) {
                session.pause();
            }
        }
    }
    
    public boolean isStopped() {
        return this.stopped.get();
    }
    
    public void close() throws JMSException {
        this.logger.trace("closing connection ({})", (Object)this);
        if (this.closed) {
            return;
        }
        this.closed = true;
        this.removeClientID();
        this.exceptionListener.set(null);
        this.closeAllSessions();
        try {
            this.rabbitConnection.close();
        }
        catch (ShutdownSignalException ex) {}
        catch (IOException x) {
            if (!(x.getCause() instanceof ShutdownSignalException)) {
                throw new RMQJMSException(x);
            }
        }
    }
    
    private void removeClientID() throws JMSException {
        final String cID = this.clientID;
        if (cID != null) {
            RMQConnection.CLIENT_IDS.remove(cID);
        }
    }
    
    private void closeAllSessions() {
        for (final RMQSession session : this.sessions) {
            try {
                session.internalClose();
            }
            catch (Exception e) {
                if (e instanceof ShutdownSignalException) {
                    continue;
                }
                this.logger.error("exception closing session ({})", (Object)session, (Object)e);
            }
        }
        this.sessions.clear();
    }
    
    Channel createRabbitChannel(final boolean transactional) throws IOException {
        final Channel channel = this.rabbitConnection.createChannel();
        if (this.channelsQos != -1) {
            channel.basicQos(this.channelsQos);
        }
        if (transactional) {
            channel.txSelect();
        }
        if (this.confirmListener != null) {
            channel.confirmSelect();
        }
        return channel;
    }
    
    public TopicSession createTopicSession(final boolean transacted, final int acknowledgeMode) throws JMSException {
        return (TopicSession)this.createSession(transacted, acknowledgeMode);
    }
    
    public ConnectionConsumer createConnectionConsumer(final Topic topic, final String messageSelector, final ServerSessionPool sessionPool, final int maxMessages) throws JMSException {
        throw new UnsupportedOperationException();
    }
    
    public QueueSession createQueueSession(final boolean transacted, final int acknowledgeMode) throws JMSException {
        return (QueueSession)this.createSession(transacted, acknowledgeMode);
    }
    
    public ConnectionConsumer createConnectionConsumer(final Queue queue, final String messageSelector, final ServerSessionPool sessionPool, final int maxMessages) throws JMSException {
        throw new UnsupportedOperationException();
    }
    
    public ConnectionConsumer createConnectionConsumer(final Destination destination, final String messageSelector, final ServerSessionPool sessionPool, final int maxMessages) throws JMSException {
        throw new UnsupportedOperationException();
    }
    
    public ConnectionConsumer createDurableConnectionConsumer(final Topic topic, final String subscriptionName, final String messageSelector, final ServerSessionPool sessionPool, final int maxMessages) throws JMSException {
        throw new UnsupportedOperationException();
    }
    
    void sessionClose(final RMQSession session) throws JMSException {
        this.logger.trace("internal:sessionClose({})", (Object)session);
        if (this.sessions.remove(session)) {
            session.internalClose();
        }
    }
    
    long getTerminationTimeout() {
        return this.terminationTimeout;
    }
    
    @Override
    public String toString() {
        return "RMQConnection{" + "rabbitConnection=" + this.rabbitConnection + ", stopped=" + this.stopped.get() + ", queueBrowserReadMax=" + this.queueBrowserReadMax + '}';
    }
    
    static {
        connectionMetaData = (ConnectionMetaData)new RMQConnectionMetaData();
        RMQConnection.CLIENT_IDS = new ConcurrentHashMap<String, String>();
    }
    
    private class RMQConnectionShutdownListener implements ShutdownListener
    {
        public void shutdownCompleted(final ShutdownSignalException cause) {
            if (null == RMQConnection.this.exceptionListener.get() || cause.isInitiatedByApplication()) {
                return;
            }
            RMQConnection.this.exceptionListener.get().onException((JMSException)new RMQJMSException(String.format("error in %s, connection closed, with reason %s", cause.getReference(), cause.getReason()), (Throwable)cause));
        }
    }
}
