// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.activemq.artemis.core.protocol.openwire;

import java.util.Collections;
import org.apache.activemq.artemis.utils.CompositeAddress;
import org.apache.activemq.filter.DestinationPath;
import org.apache.activemq.command.ActiveMQQueue;
import java.util.ArrayList;
import org.apache.activemq.command.DestinationInfo;
import java.io.IOException;
import org.apache.activemq.command.BrokerInfo;
import org.apache.activemq.command.MessageDispatch;
import java.util.Locale;
import org.apache.activemq.util.InetAddressUtil;
import org.apache.activemq.artemis.core.protocol.openwire.amq.AMQSession;
import org.apache.activemq.command.Message;
import org.apache.activemq.state.ProducerState;
import org.apache.activemq.command.ProducerInfo;
import org.apache.activemq.artemis.core.protocol.openwire.amq.AMQProducerBrokerExchange;
import org.apache.activemq.command.MessageId;
import org.apache.activemq.command.DataStructure;
import org.apache.activemq.artemis.reader.MessageUtil;
import org.apache.activemq.command.ActiveMQMessage;
import org.apache.activemq.command.ConsumerId;
import org.apache.activemq.command.ActiveMQTopic;
import org.apache.activemq.command.SessionId;
import java.util.Set;
import org.apache.activemq.command.Command;
import org.apache.activemq.advisory.AdvisorySupport;
import org.apache.activemq.artemis.api.core.ActiveMQSecurityException;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyServerConnection;
import org.apache.activemq.command.WireFormatInfo;
import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.channel.ChannelPipeline;
import org.apache.activemq.artemis.api.core.ActiveMQBuffer;
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;
import java.util.concurrent.Executor;
import org.apache.activemq.artemis.spi.core.protocol.ConnectionEntry;
import org.apache.activemq.artemis.spi.core.remoting.Connection;
import org.apache.activemq.artemis.spi.core.remoting.Acceptor;
import org.apache.activemq.artemis.api.core.BaseInterceptor;
import org.apache.activemq.artemis.spi.core.protocol.ProtocolManagerFactory;
import org.apache.activemq.command.ConnectionControl;
import java.util.Iterator;
import org.apache.activemq.artemis.core.server.ActiveMQServerLogger;
import java.util.Collection;
import javax.jms.InvalidClientIDException;
import org.apache.activemq.command.ConnectionInfo;
import org.apache.activemq.artemis.core.server.cluster.ClusterConnection;
import org.apache.activemq.artemis.core.server.cluster.ClusterManager;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import org.apache.activemq.command.ActiveMQDestination;
import org.apache.activemq.artemis.selector.impl.LRUCache;
import org.apache.activemq.filter.DestinationFilter;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.openwire.OpenWireFormat;
import java.util.concurrent.ScheduledExecutorService;
import java.util.LinkedList;
import org.apache.activemq.artemis.api.core.client.TopologyMember;
import org.apache.activemq.artemis.core.protocol.openwire.amq.AMQConnectionContext;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.activemq.command.ProducerId;
import org.apache.activemq.command.BrokerId;
import org.apache.activemq.openwire.OpenWireFormatFactory;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.util.LongSequenceGenerator;
import org.apache.activemq.util.IdGenerator;
import java.util.List;
import org.apache.activemq.artemis.api.core.client.ClusterTopologyListener;
import org.apache.activemq.artemis.api.core.Interceptor;
import org.apache.activemq.artemis.spi.core.protocol.ProtocolManager;

public class OpenWireProtocolManager implements ProtocolManager<Interceptor>, ClusterTopologyListener
{
    private static final List<String> websocketRegistryNames;
    private static final IdGenerator BROKER_ID_GENERATOR;
    private static final IdGenerator ID_GENERATOR;
    private final LongSequenceGenerator messageIdGenerator;
    private final ActiveMQServer server;
    private final OpenWireProtocolManagerFactory factory;
    private OpenWireFormatFactory wireFactory;
    private boolean prefixPacketSize;
    private BrokerId brokerId;
    protected final ProducerId advisoryProducerId;
    private final CopyOnWriteArrayList<OpenWireConnection> connections;
    private final Map<String, AMQConnectionContext> clientIdSet;
    private String brokerName;
    private final Map<String, TopologyMember> topologyMap;
    private final LinkedList<TopologyMember> members;
    private final ScheduledExecutorService scheduledPool;
    private boolean rebalanceClusterClients;
    private boolean updateClusterClients;
    private boolean updateClusterClientsOnRemove;
    private long maxInactivityDuration;
    private long maxInactivityDurationInitalDelay;
    private boolean useKeepAlive;
    private boolean supportAdvisory;
    private boolean suppressInternalManagementObjects;
    private final OpenWireFormat wireFormat;
    private final Map<SimpleString, RoutingType> prefixes;
    private final Map<DestinationFilter, Integer> vtConsumerDestinationMatchers;
    protected final LRUCache<ActiveMQDestination, ActiveMQDestination> vtDestMapCache;
    
    public OpenWireProtocolManager(final OpenWireProtocolManagerFactory factory, final ActiveMQServer server) {
        this.messageIdGenerator = new LongSequenceGenerator();
        this.prefixPacketSize = true;
        this.advisoryProducerId = new ProducerId();
        this.connections = new CopyOnWriteArrayList<OpenWireConnection>();
        this.clientIdSet = new HashMap<String, AMQConnectionContext>();
        this.topologyMap = new ConcurrentHashMap<String, TopologyMember>();
        this.members = new LinkedList<TopologyMember>();
        this.rebalanceClusterClients = false;
        this.updateClusterClients = false;
        this.updateClusterClientsOnRemove = false;
        this.maxInactivityDuration = 30000L;
        this.maxInactivityDurationInitalDelay = 10000L;
        this.useKeepAlive = true;
        this.supportAdvisory = true;
        this.suppressInternalManagementObjects = true;
        this.prefixes = new HashMap<SimpleString, RoutingType>();
        this.vtConsumerDestinationMatchers = new HashMap<DestinationFilter, Integer>();
        this.vtDestMapCache = (LRUCache<ActiveMQDestination, ActiveMQDestination>)new LRUCache();
        this.factory = factory;
        this.server = server;
        (this.wireFactory = new OpenWireFormatFactory()).setCacheEnabled(false);
        this.advisoryProducerId.setConnectionId(OpenWireProtocolManager.ID_GENERATOR.generateId());
        this.scheduledPool = server.getScheduledPool();
        this.wireFormat = (OpenWireFormat)this.wireFactory.createWireFormat();
        final ClusterManager clusterManager = this.server.getClusterManager();
        final ClusterConnection cc = clusterManager.getDefaultConnection((TransportConfiguration)null);
        if (cc != null) {
            cc.addClusterTopologyListener((ClusterTopologyListener)this);
        }
    }
    
    public void nodeUP(final TopologyMember member, final boolean last) {
        if (this.topologyMap.put(member.getNodeId(), member) == null) {
            this.updateClientClusterInfo();
        }
    }
    
    public void nodeDown(final long eventUID, final String nodeID) {
        if (this.topologyMap.remove(nodeID) != null) {
            this.updateClientClusterInfo();
        }
    }
    
    public void removeConnection(final ConnectionInfo info, final Throwable error) throws InvalidClientIDException {
        synchronized (this.clientIdSet) {
            final String clientId = info.getClientId();
            if (clientId == null) {
                throw new InvalidClientIDException("No clientID specified for connection disconnect request");
            }
            final AMQConnectionContext context = this.clientIdSet.get(clientId);
            if (context != null && context.decRefCount() == 0) {
                context.getConnection().disconnect(error != null);
                this.connections.remove(context.getConnection());
                this.clientIdSet.remove(clientId);
            }
        }
    }
    
    public ScheduledExecutorService getScheduledPool() {
        return this.scheduledPool;
    }
    
    public ActiveMQServer getServer() {
        return this.server;
    }
    
    private void updateClientClusterInfo() {
        synchronized (this.members) {
            this.members.clear();
            this.members.addAll(this.topologyMap.values());
        }
        for (final OpenWireConnection c : this.connections) {
            final ConnectionControl control = this.newConnectionControl();
            try {
                c.updateClient(control);
            }
            catch (Exception e) {
                ActiveMQServerLogger.LOGGER.warn((Object)e.getMessage(), (Throwable)e);
                c.sendException(e);
            }
        }
    }
    
    public boolean acceptsNoHandshake() {
        return false;
    }
    
    public ProtocolManagerFactory<Interceptor> getFactory() {
        return (ProtocolManagerFactory<Interceptor>)this.factory;
    }
    
    public void updateInterceptors(final List<BaseInterceptor> incomingInterceptors, final List<BaseInterceptor> outgoingInterceptors) {
    }
    
    public ConnectionEntry createConnectionEntry(final Acceptor acceptorUsed, final Connection connection) {
        final OpenWireFormat wf = (OpenWireFormat)this.wireFactory.createWireFormat();
        final OpenWireConnection owConn = new OpenWireConnection(connection, this.server, (Executor)this.server.getExecutorFactory().getExecutor(), this, wf);
        owConn.sendHandshake();
        final ConnectionEntry entry = new ConnectionEntry((RemotingConnection)owConn, (Executor)null, System.currentTimeMillis(), -1L);
        owConn.setConnectionEntry(entry);
        return entry;
    }
    
    public void removeHandler(final String name) {
    }
    
    public void handleBuffer(final RemotingConnection connection, final ActiveMQBuffer buffer) {
    }
    
    public void addChannelHandlers(final ChannelPipeline pipeline) {
        pipeline.addLast("packet-decipher", (ChannelHandler)new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4));
    }
    
    public boolean isProtocol(final byte[] array) {
        if (array.length < 8) {
            throw new IllegalArgumentException("Protocol header length changed " + array.length);
        }
        int start = this.prefixPacketSize ? 4 : 0;
        int j = 0;
        if (array[start] != 1) {
            return false;
        }
        ++start;
        final WireFormatInfo info = new WireFormatInfo();
        final byte[] magic = info.getMagic();
        final int remainingLen = array.length - start;
        int useLen = (remainingLen > magic.length) ? magic.length : remainingLen;
        useLen += start;
        for (int i = start; i < useLen; ++i) {
            if (array[i] != magic[j]) {
                return false;
            }
            ++j;
        }
        return true;
    }
    
    public void handshake(final NettyServerConnection connection, final ActiveMQBuffer buffer) {
    }
    
    public List<String> websocketSubprotocolIdentifiers() {
        return OpenWireProtocolManager.websocketRegistryNames;
    }
    
    public void addConnection(final OpenWireConnection connection, final ConnectionInfo info) throws Exception {
        final String username = info.getUserName();
        final String password = info.getPassword();
        try {
            this.validateUser(username, password, connection);
        }
        catch (ActiveMQSecurityException e) {
            final SecurityException ex = new SecurityException("User name [" + username + "] or password is invalid.");
            ex.initCause((Throwable)e);
            throw ex;
        }
        final String clientId = info.getClientId();
        if (clientId == null) {
            throw new InvalidClientIDException("No clientID specified for connection request");
        }
        synchronized (this.clientIdSet) {
            AMQConnectionContext context = this.clientIdSet.get(clientId);
            if (context != null) {
                if (!info.isFailoverReconnect()) {
                    throw new InvalidClientIDException("Broker: " + this.getBrokerName() + " - Client: " + clientId + " already connected from " + context.getConnection().getRemoteAddress());
                }
                final OpenWireConnection oldConnection = context.getConnection();
                oldConnection.disconnect(true);
                this.connections.remove(oldConnection);
                connection.reconnect(context, info);
            }
            else {
                context = connection.initContext(info);
                this.clientIdSet.put(clientId, context);
            }
            this.connections.add(connection);
            final ActiveMQTopic topic = AdvisorySupport.getConnectionAdvisoryTopic();
            final ConnectionInfo copy = info.copy();
            copy.setPassword("");
            this.fireAdvisory(context, topic, (Command)copy);
            context.getConnection().addSessions(context.getConnectionState().getSessionIds());
        }
    }
    
    public void fireAdvisory(final AMQConnectionContext context, final ActiveMQTopic topic, final Command copy) throws Exception {
        this.fireAdvisory(context, topic, copy, null, null);
    }
    
    public BrokerId getBrokerId() {
        if (this.brokerId == null) {
            this.brokerId = new BrokerId(OpenWireProtocolManager.BROKER_ID_GENERATOR.generateId());
        }
        return this.brokerId;
    }
    
    public void fireAdvisory(final AMQConnectionContext context, final ActiveMQTopic topic, final Command command, final ConsumerId targetConsumerId, String originalConnectionId) throws Exception {
        if (!this.isSupportAdvisory()) {
            return;
        }
        final ActiveMQMessage advisoryMessage = new ActiveMQMessage();
        if (originalConnectionId == null) {
            originalConnectionId = context.getConnectionId().getValue();
        }
        advisoryMessage.setStringProperty(MessageUtil.CONNECTION_ID_PROPERTY_NAME.toString(), originalConnectionId);
        advisoryMessage.setStringProperty("originBrokerName", this.getBrokerName());
        final String id = (this.getBrokerId() != null) ? this.getBrokerId().getValue() : "NOT_SET";
        advisoryMessage.setStringProperty("originBrokerId", id);
        final String url = context.getConnection().getLocalAddress();
        advisoryMessage.setStringProperty("originBrokerURL", url);
        advisoryMessage.setDataStructure((DataStructure)command);
        advisoryMessage.setPersistent(false);
        advisoryMessage.setType("Advisory");
        advisoryMessage.setMessageId(new MessageId(this.advisoryProducerId, this.messageIdGenerator.getNextSequenceId()));
        advisoryMessage.setTargetConsumerId(targetConsumerId);
        advisoryMessage.setDestination((ActiveMQDestination)topic);
        advisoryMessage.setResponseRequired(false);
        advisoryMessage.setProducerId(this.advisoryProducerId);
        final boolean originalFlowControl = context.isProducerFlowControl();
        final AMQProducerBrokerExchange producerExchange = new AMQProducerBrokerExchange();
        producerExchange.setConnectionContext(context);
        producerExchange.setProducerState(new ProducerState(new ProducerInfo()));
        try {
            context.setProducerFlowControl(false);
            final AMQSession sess = context.getConnection().getAdvisorySession();
            if (sess != null) {
                sess.send(producerExchange.getProducerState().getInfo(), (Message)advisoryMessage, false);
            }
        }
        finally {
            context.setProducerFlowControl(originalFlowControl);
        }
    }
    
    public String getBrokerName() {
        if (this.brokerName == null) {
            try {
                this.brokerName = InetAddressUtil.getLocalHostName().toLowerCase(Locale.ENGLISH);
            }
            catch (Exception e) {
                this.brokerName = this.server.getNodeID().toString();
            }
        }
        return this.brokerName;
    }
    
    protected ConnectionControl newConnectionControl() {
        final ConnectionControl control = new ConnectionControl();
        final String uri = this.generateMembersURI(this.rebalanceClusterClients);
        control.setConnectedBrokers(uri);
        control.setRebalanceConnection(this.rebalanceClusterClients);
        return control;
    }
    
    private String generateMembersURI(final boolean flip) {
        final StringBuffer connectedBrokers = new StringBuffer();
        String separator = "";
        synchronized (this.members) {
            if (this.members.size() > 0) {
                for (final TopologyMember member : this.members) {
                    connectedBrokers.append(separator).append(member.toURI());
                    separator = ",";
                }
                if (flip && this.members.size() > 1) {
                    this.members.addLast(this.members.removeFirst());
                }
            }
        }
        final String uri = connectedBrokers.toString();
        return uri;
    }
    
    public boolean isFaultTolerantConfiguration() {
        return false;
    }
    
    public void postProcessDispatch(final MessageDispatch md) {
    }
    
    public boolean isStopped() {
        return false;
    }
    
    public void preProcessDispatch(final MessageDispatch messageDispatch) {
    }
    
    public boolean isStopping() {
        return false;
    }
    
    public void validateUser(final String login, final String passcode, final OpenWireConnection connection) throws Exception {
        this.server.getSecurityStore().authenticate(login, passcode, (RemotingConnection)connection);
    }
    
    public void sendBrokerInfo(final OpenWireConnection connection) throws Exception {
        final BrokerInfo brokerInfo = new BrokerInfo();
        brokerInfo.setBrokerName(this.getBrokerName());
        brokerInfo.setBrokerId(new BrokerId("" + this.server.getNodeID()));
        brokerInfo.setPeerBrokerInfos((BrokerInfo[])null);
        brokerInfo.setFaultTolerantConfiguration(false);
        brokerInfo.setBrokerURL(connection.getLocalAddress());
        brokerInfo.setPeerBrokerInfos((BrokerInfo[])null);
        connection.dispatch((Command)brokerInfo);
    }
    
    public void configureInactivityParams(final OpenWireConnection connection, final WireFormatInfo command) throws IOException {
        final long inactivityDurationToUse = (command.getMaxInactivityDuration() > this.maxInactivityDuration) ? this.maxInactivityDuration : command.getMaxInactivityDuration();
        final long inactivityDurationInitialDelayToUse = (command.getMaxInactivityDurationInitalDelay() > this.maxInactivityDurationInitalDelay) ? this.maxInactivityDurationInitalDelay : command.getMaxInactivityDurationInitalDelay();
        final boolean useKeepAliveToUse = this.maxInactivityDuration != 0L && this.useKeepAlive;
        connection.setUpTtl(inactivityDurationToUse, inactivityDurationInitialDelayToUse, useKeepAliveToUse);
    }
    
    public void setRebalanceClusterClients(final boolean rebalance) {
        this.rebalanceClusterClients = rebalance;
    }
    
    public boolean isRebalanceClusterClients() {
        return this.rebalanceClusterClients;
    }
    
    public void setUpdateClusterClients(final boolean updateClusterClients) {
        this.updateClusterClients = updateClusterClients;
    }
    
    public boolean isUpdateClusterClients() {
        return this.updateClusterClients;
    }
    
    public void setUpdateClusterClientsOnRemove(final boolean updateClusterClientsOnRemove) {
        this.updateClusterClientsOnRemove = updateClusterClientsOnRemove;
    }
    
    public boolean isUpdateClusterClientsOnRemove() {
        return this.updateClusterClientsOnRemove;
    }
    
    public void setBrokerName(final String name) {
        this.brokerName = name;
    }
    
    public boolean isUseKeepAlive() {
        return this.useKeepAlive;
    }
    
    public void setUseKeepAlive(final boolean useKeepAlive) {
        this.useKeepAlive = useKeepAlive;
    }
    
    public long getMaxInactivityDuration() {
        return this.maxInactivityDuration;
    }
    
    public void setMaxInactivityDuration(final long maxInactivityDuration) {
        this.maxInactivityDuration = maxInactivityDuration;
    }
    
    public long getMaxInactivityDurationInitalDelay() {
        return this.maxInactivityDurationInitalDelay;
    }
    
    public void setMaxInactivityDurationInitalDelay(final long maxInactivityDurationInitalDelay) {
        this.maxInactivityDurationInitalDelay = maxInactivityDurationInitalDelay;
    }
    
    public void setAnycastPrefix(final String anycastPrefix) {
        for (final String prefix : anycastPrefix.split(",")) {
            this.prefixes.put(SimpleString.toSimpleString(prefix), RoutingType.ANYCAST);
        }
    }
    
    public void setMulticastPrefix(final String multicastPrefix) {
        for (final String prefix : multicastPrefix.split(",")) {
            this.prefixes.put(SimpleString.toSimpleString(prefix), RoutingType.MULTICAST);
        }
    }
    
    public Map<SimpleString, RoutingType> getPrefixes() {
        return this.prefixes;
    }
    
    public List<DestinationInfo> getTemporaryDestinations() {
        final List<DestinationInfo> total = new ArrayList<DestinationInfo>();
        for (final OpenWireConnection connection : this.connections) {
            total.addAll(connection.getTemporaryDestinations());
        }
        return total;
    }
    
    public OpenWireFormat wireFormat() {
        return this.wireFormat;
    }
    
    public boolean isSupportAdvisory() {
        return this.supportAdvisory;
    }
    
    public void setSupportAdvisory(final boolean supportAdvisory) {
        this.supportAdvisory = supportAdvisory;
    }
    
    public boolean isSuppressInternalManagementObjects() {
        return this.suppressInternalManagementObjects;
    }
    
    public void setSuppressInternalManagementObjects(final boolean suppressInternalManagementObjects) {
        this.suppressInternalManagementObjects = suppressInternalManagementObjects;
    }
    
    public void setVirtualTopicConsumerWildcards(final String virtualTopicConsumerWildcards) {
        for (final String filter : virtualTopicConsumerWildcards.split(",")) {
            final String[] wildcardLimitPair = filter.split(";");
            this.vtConsumerDestinationMatchers.put(DestinationFilter.parseFilter((ActiveMQDestination)new ActiveMQQueue(wildcardLimitPair[0])), Integer.valueOf(wildcardLimitPair[1]));
        }
    }
    
    public void setVirtualTopicConsumerLruCacheMax(final int max) {
        this.vtDestMapCache.setMaxCacheSize(max);
    }
    
    public ActiveMQDestination virtualTopicConsumerToFQQN(final ActiveMQDestination destination) {
        if (this.vtConsumerDestinationMatchers.isEmpty()) {
            return destination;
        }
        ActiveMQDestination mappedDestination = null;
        synchronized (this.vtDestMapCache) {
            mappedDestination = (ActiveMQDestination)this.vtDestMapCache.get((Object)destination);
        }
        if (mappedDestination != null) {
            return mappedDestination;
        }
        for (final Map.Entry<DestinationFilter, Integer> candidate : this.vtConsumerDestinationMatchers.entrySet()) {
            if (candidate.getKey().matches(destination)) {
                final String[] paths = DestinationPath.getDestinationPaths(destination);
                final StringBuilder fqqn = new StringBuilder();
                int i;
                for (int filterPathTerminus = i = candidate.getValue(); i < paths.length; ++i) {
                    if (i > filterPathTerminus) {
                        fqqn.append(".");
                    }
                    fqqn.append(paths[i]);
                }
                fqqn.append(CompositeAddress.SEPARATOR);
                for (i = 0; i < paths.length; ++i) {
                    if (i > 0) {
                        fqqn.append(".");
                    }
                    fqqn.append(paths[i]);
                }
                mappedDestination = (ActiveMQDestination)new ActiveMQQueue(fqqn.toString());
                break;
            }
        }
        if (mappedDestination == null) {
            mappedDestination = destination;
        }
        synchronized (this.vtDestMapCache) {
            final ActiveMQDestination existing = (ActiveMQDestination)this.vtDestMapCache.put((Object)destination, (Object)mappedDestination);
            if (existing != null) {
                this.vtDestMapCache.put((Object)destination, (Object)existing);
                mappedDestination = existing;
            }
        }
        return mappedDestination;
    }
    
    static {
        websocketRegistryNames = Collections.EMPTY_LIST;
        BROKER_ID_GENERATOR = new IdGenerator();
        ID_GENERATOR = new IdGenerator();
    }
}
