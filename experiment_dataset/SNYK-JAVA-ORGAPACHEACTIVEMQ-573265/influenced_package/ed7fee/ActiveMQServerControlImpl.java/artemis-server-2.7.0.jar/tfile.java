// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.activemq.artemis.core.management.impl;

import org.apache.activemq.artemis.core.filter.Filter;
import java.net.URL;
import org.apache.activemq.artemis.spi.core.security.jaas.PropertiesLoginModuleConfigurator;
import org.apache.activemq.artemis.utils.PasswordMaskingUtil;
import org.apache.activemq.artemis.utils.collections.TypedProperties;
import org.apache.activemq.artemis.api.core.management.ManagementHelper;
import org.apache.activemq.artemis.core.server.management.Notification;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanOperationInfo;
import org.apache.activemq.artemis.api.core.management.CoreNotificationType;
import javax.management.MBeanNotificationInfo;
import javax.management.ListenerNotFoundException;
import javax.management.NotificationFilter;
import org.apache.activemq.artemis.core.client.impl.Topology;
import org.apache.activemq.artemis.core.server.cluster.ClusterManager;
import org.apache.activemq.artemis.core.client.impl.TopologyMemberImpl;
import org.apache.activemq.artemis.core.server.cluster.ha.ScaleDownPolicy;
import org.apache.activemq.artemis.core.server.cluster.ha.LiveOnlyPolicy;
import org.apache.activemq.artemis.core.postoffice.DuplicateIDCache;
import org.apache.activemq.artemis.core.server.ConnectorServiceFactory;
import org.apache.activemq.artemis.core.config.ConnectorServiceConfiguration;
import org.apache.activemq.artemis.core.config.BridgeConfiguration;
import org.apache.activemq.artemis.api.core.management.BridgeControl;
import org.apache.activemq.artemis.core.server.ComponentConfigurationRoutingType;
import org.apache.activemq.artemis.core.config.DivertConfiguration;
import org.apache.activemq.artemis.core.config.TransformerConfiguration;
import org.apache.activemq.artemis.api.core.JsonUtil;
import org.apache.activemq.artemis.api.config.ActiveMQDefaultConfiguration;
import org.apache.activemq.artemis.api.core.management.DivertControl;
import org.apache.activemq.artemis.core.server.group.GroupingHandler;
import org.apache.activemq.artemis.core.persistence.config.PersistedAddressSetting;
import org.apache.activemq.artemis.core.settings.impl.SlowConsumerPolicy;
import org.apache.activemq.artemis.core.settings.impl.AddressFullMessagePolicy;
import org.apache.activemq.artemis.core.security.CheckType;
import org.apache.activemq.artemis.core.security.Role;
import org.apache.activemq.artemis.core.persistence.config.PersistedRoles;
import org.apache.activemq.artemis.utils.SecurityFormatter;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import javax.json.JsonObjectBuilder;
import org.apache.activemq.artemis.core.management.impl.view.ProducerView;
import org.apache.activemq.artemis.core.server.ServerProducer;
import org.apache.activemq.artemis.api.core.management.Parameter;
import org.apache.activemq.artemis.core.management.impl.view.QueueView;
import org.apache.activemq.artemis.core.management.impl.view.AddressView;
import org.apache.activemq.artemis.core.management.impl.view.ConsumerView;
import java.util.HashSet;
import org.apache.activemq.artemis.core.management.impl.view.SessionView;
import org.apache.activemq.artemis.core.management.impl.view.ConnectionView;
import org.apache.activemq.artemis.core.server.ServerSession;
import org.apache.activemq.artemis.core.server.ServerConsumer;
import org.apache.activemq.artemis.core.server.Consumer;
import org.apache.activemq.artemis.core.postoffice.impl.LocalQueueBinding;
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;
import javax.json.JsonArray;
import javax.json.JsonObject;
import org.apache.activemq.artemis.core.transaction.TransactionDetail;
import org.apache.activemq.artemis.core.transaction.Transaction;
import javax.json.JsonArrayBuilder;
import javax.json.JsonValue;
import org.apache.activemq.artemis.utils.JsonLoader;
import org.apache.activemq.artemis.core.transaction.TransactionDetailFactory;
import org.apache.activemq.artemis.core.transaction.impl.CoreTransactionDetail;
import org.apache.activemq.artemis.core.transaction.impl.XidImpl;
import java.util.Date;
import java.util.Collections;
import javax.transaction.xa.Xid;
import java.util.Map;
import java.text.DateFormat;
import org.apache.activemq.artemis.core.server.ActiveMQServerLogger;
import java.util.Set;
import java.util.Collection;
import java.util.TreeSet;
import java.util.Comparator;
import org.apache.activemq.artemis.core.postoffice.Bindings;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.function.Function;
import org.apache.activemq.artemis.core.postoffice.Binding;
import org.apache.activemq.artemis.api.core.ActiveMQAddressDoesNotExistException;
import org.apache.activemq.artemis.api.core.management.AddressControl;
import org.apache.activemq.artemis.core.server.cluster.ClusterConnection;
import java.util.List;
import java.util.ArrayList;
import org.apache.activemq.artemis.api.core.management.QueueControl;
import org.apache.activemq.artemis.core.server.Queue;
import org.apache.activemq.artemis.core.settings.impl.AddressSettings;
import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.core.security.SecurityAuth;
import java.util.Iterator;
import org.apache.activemq.artemis.core.server.ActiveMQMessageBundle;
import org.apache.activemq.artemis.core.server.impl.AddressInfo;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.utils.ListUtil;
import java.util.EnumSet;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.core.server.impl.Activation;
import org.apache.activemq.artemis.core.server.impl.SharedNothingLiveActivation;
import org.apache.activemq.artemis.core.server.cluster.ha.HAPolicy;
import org.apache.activemq.artemis.core.server.cluster.ha.SharedStoreSlavePolicy;
import org.apache.activemq.artemis.core.server.JournalType;
import org.apache.activemq.artemis.logs.AuditLogger;
import org.apache.activemq.artemis.core.persistence.StorageManager;
import java.util.concurrent.atomic.AtomicLong;
import javax.management.NotificationBroadcasterSupport;
import org.apache.activemq.artemis.core.messagecounter.MessageCounterManager;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.remoting.server.RemotingService;
import org.apache.activemq.artemis.core.transaction.ResourceManager;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.postoffice.PostOffice;
import org.jboss.logging.Logger;
import org.apache.activemq.artemis.core.server.management.NotificationListener;
import javax.management.NotificationEmitter;
import org.apache.activemq.artemis.api.core.management.ActiveMQServerControl;

public class ActiveMQServerControlImpl extends AbstractControl implements ActiveMQServerControl, NotificationEmitter, NotificationListener
{
    private static final Logger logger;
    private final PostOffice postOffice;
    private final Configuration configuration;
    private final ResourceManager resourceManager;
    private final RemotingService remotingService;
    private final ActiveMQServer server;
    private final MessageCounterManager messageCounterManager;
    private final NotificationBroadcasterSupport broadcaster;
    private final AtomicLong notifSeq;
    
    public ActiveMQServerControlImpl(final PostOffice postOffice, final Configuration configuration, final ResourceManager resourceManager, final RemotingService remotingService, final ActiveMQServer messagingServer, final MessageCounterManager messageCounterManager, final StorageManager storageManager, final NotificationBroadcasterSupport broadcaster) throws Exception {
        super(ActiveMQServerControl.class, storageManager);
        this.notifSeq = new AtomicLong(0L);
        this.postOffice = postOffice;
        this.configuration = configuration;
        this.resourceManager = resourceManager;
        this.remotingService = remotingService;
        this.server = messagingServer;
        this.messageCounterManager = messageCounterManager;
        this.broadcaster = broadcaster;
        this.server.getManagementService().addNotificationListener((NotificationListener)this);
    }
    
    public boolean isStarted() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.isStarted((Object)this.server);
        }
        this.clearIO();
        try {
            return this.server.isStarted();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public String getVersion() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.getVersion((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            return this.server.getVersion().getFullVersion();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public boolean isBackup() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.isBackup((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            return this.server.getHAPolicy().isBackup();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public boolean isSharedStore() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.isSharedStore((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            return this.server.getHAPolicy().isSharedStore();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public String getBindingsDirectory() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.getBindingsDirectory((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            return this.configuration.getBindingsDirectory();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public String[] getInterceptorClassNames() {
        this.checkStarted();
        this.clearIO();
        try {
            return this.configuration.getIncomingInterceptorClassNames().toArray(new String[this.configuration.getIncomingInterceptorClassNames().size()]);
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public String[] getIncomingInterceptorClassNames() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.getIncomingInterceptorClassNames((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            return this.configuration.getIncomingInterceptorClassNames().toArray(new String[this.configuration.getIncomingInterceptorClassNames().size()]);
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public String[] getOutgoingInterceptorClassNames() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.getOutgoingInterceptorClassNames((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            return this.configuration.getOutgoingInterceptorClassNames().toArray(new String[this.configuration.getOutgoingInterceptorClassNames().size()]);
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public int getJournalBufferSize() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.getJournalBufferSize((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            return (this.configuration.getJournalType() == JournalType.ASYNCIO) ? this.configuration.getJournalBufferSize_AIO() : this.configuration.getJournalBufferSize_NIO();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public int getJournalBufferTimeout() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.getJournalBufferTimeout((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            return (this.configuration.getJournalType() == JournalType.ASYNCIO) ? this.configuration.getJournalBufferTimeout_AIO() : this.configuration.getJournalBufferTimeout_NIO();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public void setFailoverOnServerShutdown(final boolean failoverOnServerShutdown) {
        if (AuditLogger.isEnabled()) {
            AuditLogger.setFailoverOnServerShutdown((Object)this.server, new Object[] { failoverOnServerShutdown });
        }
        this.checkStarted();
        this.clearIO();
        try {
            final HAPolicy haPolicy = this.server.getHAPolicy();
            if (haPolicy instanceof SharedStoreSlavePolicy) {
                ((SharedStoreSlavePolicy)haPolicy).setFailoverOnServerShutdown(failoverOnServerShutdown);
            }
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public boolean isFailoverOnServerShutdown() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.isFailoverOnServerShutdown((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            final HAPolicy haPolicy = this.server.getHAPolicy();
            return haPolicy instanceof SharedStoreSlavePolicy && ((SharedStoreSlavePolicy)haPolicy).isFailoverOnServerShutdown();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public int getJournalMaxIO() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.getJournalMaxIO((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            return (this.configuration.getJournalType() == JournalType.ASYNCIO) ? this.configuration.getJournalMaxIO_AIO() : this.configuration.getJournalMaxIO_NIO();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public String getJournalDirectory() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.getJournalDirectory((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            return this.configuration.getJournalDirectory();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public int getJournalFileSize() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.getJournalFileSize((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            return this.configuration.getJournalFileSize();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public int getJournalMinFiles() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.getJournalMinFiles((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            return this.configuration.getJournalMinFiles();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public int getJournalCompactMinFiles() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.getJournalCompactMinFiles((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            return this.configuration.getJournalCompactMinFiles();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public int getJournalCompactPercentage() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.getJournalCompactPercentage((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            return this.configuration.getJournalCompactPercentage();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public boolean isPersistenceEnabled() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.isPersistenceEnabled((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            return this.configuration.isPersistenceEnabled();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public String getJournalType() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.getJournalType((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            return this.configuration.getJournalType().toString();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public String getPagingDirectory() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.getPagingDirectory((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            return this.configuration.getPagingDirectory();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public int getScheduledThreadPoolMaxSize() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.getScheduledThreadPoolMaxSize((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            return this.configuration.getScheduledThreadPoolMaxSize();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public int getThreadPoolMaxSize() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.getThreadPoolMaxSize((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            return this.configuration.getThreadPoolMaxSize();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public long getSecurityInvalidationInterval() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.getSecurityInvalidationInterval((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            return this.configuration.getSecurityInvalidationInterval();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public boolean isClustered() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.isClustered((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            return this.configuration.isClustered();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public boolean isCreateBindingsDir() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.isCreateBindingsDir((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            return this.configuration.isCreateBindingsDir();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public boolean isCreateJournalDir() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.isCreateJournalDir((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            return this.configuration.isCreateJournalDir();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public boolean isJournalSyncNonTransactional() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.isJournalSyncNonTransactional((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            return this.configuration.isJournalSyncNonTransactional();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public boolean isJournalSyncTransactional() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.isJournalSyncTransactional((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            return this.configuration.isJournalSyncTransactional();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public boolean isSecurityEnabled() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.isSecurityEnabled((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            return this.configuration.isSecurityEnabled();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public boolean isAsyncConnectionExecutionEnabled() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.isAsyncConnectionExecutionEnabled((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            return this.configuration.isAsyncConnectionExecutionEnabled();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public int getDiskScanPeriod() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.getDiskScanPeriod((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            return this.configuration.getDiskScanPeriod();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public int getMaxDiskUsage() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.getMaxDiskUsage((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            return this.configuration.getMaxDiskUsage();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public long getGlobalMaxSize() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.getGlobalMaxSize((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            return this.configuration.getGlobalMaxSize();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public long getAddressMemoryUsage() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.getAddressMemoryUsage((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            if (this.server.getPagingManager() == null) {
                return -1L;
            }
            return this.server.getPagingManager().getGlobalSize();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public int getAddressMemoryUsagePercentage() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.getAddressMemoryUsagePercentage((Object)this.server);
        }
        final long globalMaxSize = this.getGlobalMaxSize();
        if (globalMaxSize <= 0L) {
            return 0;
        }
        final long memoryUsed = this.getAddressMemoryUsage();
        if (memoryUsed <= 0L) {
            return 0;
        }
        final double result = 100.0 * memoryUsed / globalMaxSize;
        return (int)result;
    }
    
    public boolean freezeReplication() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.freezeReplication((Object)this.server);
        }
        final Activation activation = this.server.getActivation();
        if (activation instanceof SharedNothingLiveActivation) {
            final SharedNothingLiveActivation liveActivation = (SharedNothingLiveActivation)activation;
            liveActivation.freezeReplication();
            return true;
        }
        return false;
    }
    
    public String createAddress(final String name, final String routingTypes) throws Exception {
        if (AuditLogger.isEnabled()) {
            AuditLogger.createAddress((Object)this.server, new Object[] { name, routingTypes });
        }
        this.checkStarted();
        this.clearIO();
        try {
            final EnumSet<RoutingType> set = EnumSet.noneOf(RoutingType.class);
            for (final String routingType : ListUtil.toList(routingTypes)) {
                set.add(RoutingType.valueOf(routingType));
            }
            final AddressInfo addressInfo = new AddressInfo(new SimpleString(name), set);
            if (this.server.addAddressInfo(addressInfo)) {
                return AddressInfoTextFormatter.Long.format(addressInfo, new StringBuilder()).toString();
            }
            throw ActiveMQMessageBundle.BUNDLE.addressAlreadyExists(addressInfo.getName());
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public String updateAddress(final String name, final String routingTypes) throws Exception {
        if (AuditLogger.isEnabled()) {
            AuditLogger.updateAddress((Object)this.server, new Object[] { name, routingTypes });
        }
        this.checkStarted();
        this.clearIO();
        try {
            EnumSet<RoutingType> routingTypeSet;
            if (routingTypes == null) {
                routingTypeSet = null;
            }
            else {
                routingTypeSet = EnumSet.noneOf(RoutingType.class);
                final String[] split;
                final String[] routingTypeNames = split = routingTypes.split(",");
                for (final String routingTypeName : split) {
                    routingTypeSet.add(RoutingType.valueOf(routingTypeName));
                }
            }
            if (!this.server.updateAddressInfo(SimpleString.toSimpleString(name), routingTypeSet)) {
                throw ActiveMQMessageBundle.BUNDLE.addressDoesNotExist(SimpleString.toSimpleString(name));
            }
            return AddressInfoTextFormatter.Long.format(this.server.getAddressInfo(SimpleString.toSimpleString(name)), new StringBuilder()).toString();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public void deleteAddress(final String name) throws Exception {
        this.deleteAddress(name, false);
    }
    
    public void deleteAddress(final String name, final boolean force) throws Exception {
        if (AuditLogger.isEnabled()) {
            AuditLogger.deleteAddress((Object)this.server, new Object[] { name, force });
        }
        this.checkStarted();
        this.clearIO();
        try {
            this.server.removeAddressInfo(new SimpleString(name), null, force);
        }
        catch (ActiveMQException e) {
            throw new IllegalStateException(e.getMessage());
        }
        finally {
            this.blockOnIO();
        }
    }
    
    @Deprecated
    public void deployQueue(final String address, final String name, final String filterString) throws Exception {
        this.deployQueue(address, name, filterString, true);
    }
    
    @Deprecated
    public void deployQueue(final String address, final String name, final String filterStr, final boolean durable) throws Exception {
        if (AuditLogger.isEnabled()) {
            AuditLogger.deployQueue((Object)this.server, new Object[] { address, name, filterStr, durable });
        }
        this.checkStarted();
        final SimpleString filter = (filterStr == null) ? null : new SimpleString(filterStr);
        this.clearIO();
        try {
            this.server.createQueue(SimpleString.toSimpleString(address), this.server.getAddressSettingsRepository().getMatch(address).getDefaultQueueRoutingType(), new SimpleString(name), filter, durable, false);
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public void createQueue(final String address, final String name) throws Exception {
        this.createQueue(address, name, true);
    }
    
    public void createQueue(final String address, final String name, final String routingType) throws Exception {
        this.createQueue(address, name, true, routingType);
    }
    
    public void createQueue(final String address, final String name, final boolean durable) throws Exception {
        this.createQueue(address, name, null, durable);
    }
    
    public void createQueue(final String address, final String name, final boolean durable, final String routingType) throws Exception {
        this.createQueue(address, name, null, durable, routingType);
    }
    
    public void createQueue(final String address, final String name, final String filterStr, final boolean durable) throws Exception {
        this.createQueue(address, name, filterStr, durable, this.server.getAddressSettingsRepository().getMatch((address == null) ? name : address).getDefaultQueueRoutingType().toString());
    }
    
    public void createQueue(final String address, final String name, final String filterStr, final boolean durable, final String routingType) throws Exception {
        final AddressSettings addressSettings = this.server.getAddressSettingsRepository().getMatch((address == null) ? name : address);
        this.createQueue(address, routingType, name, filterStr, durable, addressSettings.getDefaultMaxConsumers(), addressSettings.isDefaultPurgeOnNoConsumers(), addressSettings.isAutoCreateAddresses());
    }
    
    public String createQueue(final String address, final String routingType, final String name, final String filterStr, final boolean durable, final int maxConsumers, final boolean purgeOnNoConsumers, final boolean autoCreateAddress) throws Exception {
        final AddressSettings addressSettings = this.server.getAddressSettingsRepository().getMatch((address == null) ? name : address);
        return this.createQueue(address, routingType, name, filterStr, durable, maxConsumers, purgeOnNoConsumers, addressSettings.isDefaultExclusiveQueue(), addressSettings.isDefaultGroupRebalance(), addressSettings.getDefaultGroupBuckets(), addressSettings.isDefaultLastValueQueue(), (addressSettings.getDefaultLastValueKey() == null) ? null : addressSettings.getDefaultLastValueKey().toString(), addressSettings.isDefaultNonDestructive(), addressSettings.getDefaultConsumersBeforeDispatch(), addressSettings.getDefaultDelayBeforeDispatch(), addressSettings.isAutoDeleteQueues(), addressSettings.getAutoDeleteQueuesDelay(), addressSettings.getAutoDeleteQueuesMessageCount(), autoCreateAddress);
    }
    
    public String createQueue(final String address, final String routingType, final String name, final String filterStr, final boolean durable, final int maxConsumers, final boolean purgeOnNoConsumers, final boolean exclusive, final boolean groupRebalance, final int groupBuckets, final boolean lastValue, final String lastValueKey, final boolean nonDestructive, final int consumersBeforeDispatch, final long delayBeforeDispatch, final boolean autoDelete, final long autoDeleteDelay, final long autoDeleteMessageCount, final boolean autoCreateAddress) throws Exception {
        if (AuditLogger.isEnabled()) {
            AuditLogger.createQueue((Object)this.server, address, new Object[] { routingType, name, filterStr, durable, maxConsumers, purgeOnNoConsumers, exclusive, groupBuckets, lastValue, lastValueKey, nonDestructive, consumersBeforeDispatch, delayBeforeDispatch, autoDelete, autoDeleteDelay, autoDeleteMessageCount, autoCreateAddress });
        }
        this.checkStarted();
        this.clearIO();
        SimpleString filter = (filterStr == null) ? null : new SimpleString(filterStr);
        try {
            if (filterStr != null && !filterStr.trim().equals("")) {
                filter = new SimpleString(filterStr);
            }
            final Queue queue = this.server.createQueue(SimpleString.toSimpleString(address), RoutingType.valueOf(routingType.toUpperCase()), SimpleString.toSimpleString(name), filter, durable, false, maxConsumers, purgeOnNoConsumers, exclusive, groupRebalance, groupBuckets, lastValue, SimpleString.toSimpleString(lastValueKey), nonDestructive, consumersBeforeDispatch, delayBeforeDispatch, autoDelete, autoDeleteDelay, autoDeleteMessageCount, autoCreateAddress);
            return QueueTextFormatter.Long.format(queue, new StringBuilder()).toString();
        }
        catch (ActiveMQException e) {
            throw new IllegalStateException(e.getMessage());
        }
        finally {
            this.blockOnIO();
        }
    }
    
    @Deprecated
    public String updateQueue(final String name, final String routingType, final Integer maxConsumers, final Boolean purgeOnNoConsumers) throws Exception {
        return this.updateQueue(name, routingType, maxConsumers, purgeOnNoConsumers, null);
    }
    
    @Deprecated
    public String updateQueue(final String name, final String routingType, final Integer maxConsumers, final Boolean purgeOnNoConsumers, final Boolean exclusive) throws Exception {
        return this.updateQueue(name, routingType, maxConsumers, purgeOnNoConsumers, exclusive, null);
    }
    
    public String updateQueue(final String name, final String routingType, final Integer maxConsumers, final Boolean purgeOnNoConsumers, final Boolean exclusive, final String user) throws Exception {
        return this.updateQueue(name, routingType, null, maxConsumers, purgeOnNoConsumers, exclusive, null, null, null, null, null, user);
    }
    
    public String updateQueue(final String name, final String routingType, final String filter, final Integer maxConsumers, final Boolean purgeOnNoConsumers, final Boolean exclusive, final Boolean groupRebalance, final Integer groupBuckets, final Boolean nonDestructive, final Integer consumersBeforeDispatch, final Long delayBeforeDispatch, final String user) throws Exception {
        if (AuditLogger.isEnabled()) {
            AuditLogger.updateQueue((Object)this.server, new Object[] { name, routingType, filter, maxConsumers, purgeOnNoConsumers, exclusive, groupRebalance, groupBuckets, nonDestructive, consumersBeforeDispatch, delayBeforeDispatch, user });
        }
        this.checkStarted();
        this.clearIO();
        try {
            final Queue queue = this.server.updateQueue(name, (routingType != null) ? RoutingType.valueOf(routingType) : null, filter, maxConsumers, purgeOnNoConsumers, exclusive, groupRebalance, groupBuckets, nonDestructive, consumersBeforeDispatch, delayBeforeDispatch, user);
            if (queue == null) {
                throw ActiveMQMessageBundle.BUNDLE.noSuchQueue(new SimpleString(name));
            }
            return QueueTextFormatter.Long.format(queue, new StringBuilder()).toString();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public String[] getQueueNames() {
        return this.getQueueNames(null);
    }
    
    public String[] getQueueNames(final String routingType) {
        if (AuditLogger.isEnabled()) {
            AuditLogger.getQueueNames((Object)this.server, new Object[] { routingType });
        }
        this.checkStarted();
        this.clearIO();
        try {
            final Object[] queueControls = this.server.getManagementService().getResources(QueueControl.class);
            final List<String> names = new ArrayList<String>();
            for (int i = 0; i < queueControls.length; ++i) {
                final QueueControl queueControl = (QueueControl)queueControls[i];
                if (routingType != null && queueControl.getRoutingType().equals(routingType.toUpperCase())) {
                    names.add(queueControl.getName());
                }
                else if (routingType == null) {
                    names.add(queueControl.getName());
                }
            }
            final String[] result = new String[names.size()];
            return names.toArray(result);
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public String[] getClusterConnectionNames() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.getClusterConnectionNames((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            final List<String> names = new ArrayList<String>();
            for (final ClusterConnection clusterConnection : this.server.getClusterManager().getClusterConnections()) {
                names.add(clusterConnection.getName().toString());
            }
            final String[] result = new String[names.size()];
            return names.toArray(result);
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public String getUptime() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.getUptime((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            return this.server.getUptime();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public long getUptimeMillis() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.getUptimeMillis((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            return this.server.getUptimeMillis();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public boolean isReplicaSync() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.isReplicaSync((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            return this.server.isReplicaSync();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public String[] getAddressNames() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.getAddressNames((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            final Object[] addresses = this.server.getManagementService().getResources(AddressControl.class);
            final String[] names = new String[addresses.length];
            for (int i = 0; i < addresses.length; ++i) {
                final AddressControl address = (AddressControl)addresses[i];
                names[i] = address.getAddress();
            }
            return names;
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public void destroyQueue(final String name, final boolean removeConsumers, final boolean autoDeleteAddress) throws Exception {
        if (AuditLogger.isEnabled()) {
            AuditLogger.destroyQueue((Object)this.server, name, new Object[] { removeConsumers, autoDeleteAddress });
        }
        this.checkStarted();
        this.clearIO();
        try {
            final SimpleString queueName = new SimpleString(name);
            this.server.destroyQueue(queueName, null, !removeConsumers, removeConsumers, autoDeleteAddress);
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public void destroyQueue(final String name, final boolean removeConsumers) throws Exception {
        this.destroyQueue(name, removeConsumers, false);
    }
    
    public void destroyQueue(final String name) throws Exception {
        this.destroyQueue(name, false);
    }
    
    public String getAddressInfo(final String address) throws ActiveMQAddressDoesNotExistException {
        if (AuditLogger.isEnabled()) {
            AuditLogger.getAddressInfo((Object)this.server, new Object[] { address });
        }
        this.checkStarted();
        this.clearIO();
        try {
            final AddressInfo addressInfo = this.server.getAddressInfo(SimpleString.toSimpleString(address));
            if (addressInfo == null) {
                throw ActiveMQMessageBundle.BUNDLE.addressDoesNotExist(SimpleString.toSimpleString(address));
            }
            return AddressInfoTextFormatter.Long.format(addressInfo, new StringBuilder()).toString();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public String listBindingsForAddress(final String address) throws Exception {
        if (AuditLogger.isEnabled()) {
            AuditLogger.listBindingsForAddress((Object)this.server, new Object[] { address });
        }
        this.checkStarted();
        this.clearIO();
        try {
            final Bindings bindings = this.server.getPostOffice().lookupBindingsForAddress(new SimpleString(address));
            return (bindings == null) ? "" : bindings.getBindings().stream().map((Function<? super Binding, ?>)Binding::toManagementString).collect((Collector<? super Object, ?, String>)Collectors.joining(","));
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public String listAddresses(final String separator) throws Exception {
        if (AuditLogger.isEnabled()) {
            AuditLogger.listAddresses((Object)this.server, new Object[] { separator });
        }
        this.checkStarted();
        this.clearIO();
        try {
            final Set<SimpleString> addresses = this.server.getPostOffice().getAddresses();
            final TreeSet<SimpleString> sortAddress = new TreeSet<SimpleString>(new Comparator<SimpleString>() {
                @Override
                public int compare(final SimpleString o1, final SimpleString o2) {
                    return o1.toString().compareToIgnoreCase(o2.toString());
                }
            });
            sortAddress.addAll(addresses);
            final StringBuilder result = new StringBuilder();
            for (final SimpleString string : sortAddress) {
                if (result.length() > 0) {
                    result.append(separator);
                }
                result.append((CharSequence)string);
            }
            return result.toString();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public int getConnectionCount() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.getConnectionCount((Object)this.server, new Object[0]);
        }
        this.checkStarted();
        this.clearIO();
        try {
            return this.server.getConnectionCount();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public long getTotalConnectionCount() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.getTotalConnectionCount((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            return this.server.getTotalConnectionCount();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public long getTotalMessageCount() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.getTotalMessageCount((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            return this.server.getTotalMessageCount();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public long getTotalMessagesAdded() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.getTotalMessagesAdded((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            return this.server.getTotalMessagesAdded();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public long getTotalMessagesAcknowledged() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.getTotalMessagesAcknowledged((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            return this.server.getTotalMessagesAcknowledged();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public long getTotalConsumerCount() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.getTotalConsumerCount((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            return this.server.getTotalConsumerCount();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public void enableMessageCounters() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.enableMessageCounters((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            this.setMessageCounterEnabled(true);
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public void disableMessageCounters() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.disableMessageCounters((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            this.setMessageCounterEnabled(false);
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public void resetAllMessageCounters() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.resetAllMessageCounters((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            this.messageCounterManager.resetAllCounters();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public void resetAllMessageCounterHistories() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.resetAllMessageCounterHistories((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            this.messageCounterManager.resetAllCounterHistories();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public boolean isMessageCounterEnabled() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.isMessageCounterEnabled((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            return this.configuration.isMessageCounterEnabled();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public synchronized long getMessageCounterSamplePeriod() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.getMessageCounterSamplePeriod((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            return this.messageCounterManager.getSamplePeriod();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public synchronized void setMessageCounterSamplePeriod(final long newPeriod) {
        if (AuditLogger.isEnabled()) {
            AuditLogger.setMessageCounterSamplePeriod((Object)this.server, new Object[] { newPeriod });
        }
        this.checkStarted();
        this.clearIO();
        try {
            if (newPeriod < 1000L) {
                if (newPeriod <= 0L) {
                    throw ActiveMQMessageBundle.BUNDLE.periodMustGreaterThanZero(newPeriod);
                }
                ActiveMQServerLogger.LOGGER.invalidMessageCounterPeriod(newPeriod);
            }
            if (this.messageCounterManager != null && newPeriod != this.messageCounterManager.getSamplePeriod()) {
                this.messageCounterManager.reschedule(newPeriod);
            }
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public int getMessageCounterMaxDayCount() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.getMessageCounterMaxDayCount((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            return this.messageCounterManager.getMaxDayCount();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public void setMessageCounterMaxDayCount(final int count) {
        if (AuditLogger.isEnabled()) {
            AuditLogger.setMessageCounterMaxDayCount((Object)this.server, new Object[] { count });
        }
        this.checkStarted();
        this.clearIO();
        try {
            if (count <= 0) {
                throw ActiveMQMessageBundle.BUNDLE.greaterThanZero(count);
            }
            this.messageCounterManager.setMaxDayCount(count);
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public String[] listPreparedTransactions() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.listPreparedTransactions((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            final DateFormat dateFormat = DateFormat.getDateTimeInstance(3, 2);
            final Map<Xid, Long> xids = this.resourceManager.getPreparedTransactionsWithCreationTime();
            final ArrayList<Map.Entry<Xid, Long>> xidsSortedByCreationTime = new ArrayList<Map.Entry<Xid, Long>>(xids.entrySet());
            Collections.sort(xidsSortedByCreationTime, new Comparator<Map.Entry<Xid, Long>>() {
                @Override
                public int compare(final Map.Entry<Xid, Long> entry1, final Map.Entry<Xid, Long> entry2) {
                    return (int)(entry1.getValue() - entry2.getValue());
                }
            });
            final String[] s = new String[xidsSortedByCreationTime.size()];
            int i = 0;
            for (final Map.Entry<Xid, Long> entry : xidsSortedByCreationTime) {
                final Date creation = new Date(entry.getValue());
                final Xid xid = entry.getKey();
                s[i++] = dateFormat.format(creation) + " base64: " + XidImpl.toBase64String(xid) + " " + xid.toString();
            }
            return s;
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public String listPreparedTransactionDetailsAsJSON() throws Exception {
        return this.listPreparedTransactionDetailsAsJSON((xid, tx, creation) -> new CoreTransactionDetail(xid, tx, creation));
    }
    
    public String listPreparedTransactionDetailsAsJSON(final TransactionDetailFactory factory) throws Exception {
        if (AuditLogger.isEnabled()) {
            AuditLogger.listPreparedTransactionDetailsAsJSON((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            final Map<Xid, Long> xids = this.resourceManager.getPreparedTransactionsWithCreationTime();
            if (xids == null || xids.size() == 0) {
                return "";
            }
            final ArrayList<Map.Entry<Xid, Long>> xidsSortedByCreationTime = new ArrayList<Map.Entry<Xid, Long>>(xids.entrySet());
            Collections.sort(xidsSortedByCreationTime, new Comparator<Map.Entry<Xid, Long>>() {
                @Override
                public int compare(final Map.Entry<Xid, Long> entry1, final Map.Entry<Xid, Long> entry2) {
                    return (int)(entry1.getValue() - entry2.getValue());
                }
            });
            final JsonArrayBuilder txDetailListJson = JsonLoader.createArrayBuilder();
            for (final Map.Entry<Xid, Long> entry : xidsSortedByCreationTime) {
                final Xid xid = entry.getKey();
                final Transaction tx = this.resourceManager.getTransaction(xid);
                if (tx == null) {
                    continue;
                }
                final TransactionDetail detail = factory.createTransactionDetail(xid, tx, entry.getValue());
                txDetailListJson.add((JsonValue)detail.toJSON());
            }
            return txDetailListJson.build().toString();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public String listPreparedTransactionDetailsAsHTML() throws Exception {
        return this.listPreparedTransactionDetailsAsHTML((xid, tx, creation) -> new CoreTransactionDetail(xid, tx, creation));
    }
    
    public String listPreparedTransactionDetailsAsHTML(final TransactionDetailFactory factory) throws Exception {
        if (AuditLogger.isEnabled()) {
            AuditLogger.listPreparedTransactionDetailsAsHTML((Object)this.server, new Object[] { factory });
        }
        this.checkStarted();
        this.clearIO();
        try {
            final Map<Xid, Long> xids = this.resourceManager.getPreparedTransactionsWithCreationTime();
            if (xids == null || xids.size() == 0) {
                return "<h3>*** Prepared Transaction Details ***</h3><p>No entry.</p>";
            }
            final ArrayList<Map.Entry<Xid, Long>> xidsSortedByCreationTime = new ArrayList<Map.Entry<Xid, Long>>(xids.entrySet());
            Collections.sort(xidsSortedByCreationTime, new Comparator<Map.Entry<Xid, Long>>() {
                @Override
                public int compare(final Map.Entry<Xid, Long> entry1, final Map.Entry<Xid, Long> entry2) {
                    return (int)(entry1.getValue() - entry2.getValue());
                }
            });
            final StringBuilder html = new StringBuilder();
            html.append("<h3>*** Prepared Transaction Details ***</h3>");
            for (final Map.Entry<Xid, Long> entry : xidsSortedByCreationTime) {
                final Xid xid = entry.getKey();
                final Transaction tx = this.resourceManager.getTransaction(xid);
                if (tx == null) {
                    continue;
                }
                final TransactionDetail detail = factory.createTransactionDetail(xid, tx, entry.getValue());
                final JsonObject txJson = detail.toJSON();
                html.append("<table border=\"1\">");
                html.append("<tr><th>creation_time</th>");
                html.append("<td>" + txJson.get((Object)"creation_time") + "</td>");
                html.append("<th>xid_as_base_64</th>");
                html.append("<td colspan=\"3\">" + txJson.get((Object)"xid_as_base64") + "</td></tr>");
                html.append("<tr><th>xid_format_id</th>");
                html.append("<td>" + txJson.get((Object)"xid_format_id") + "</td>");
                html.append("<th>xid_global_txid</th>");
                html.append("<td>" + txJson.get((Object)"xid_global_txid") + "</td>");
                html.append("<th>xid_branch_qual</th>");
                html.append("<td>" + txJson.get((Object)"xid_branch_qual") + "</td></tr>");
                html.append("<tr><th colspan=\"6\">Message List</th></tr>");
                html.append("<tr><td colspan=\"6\">");
                html.append("<table border=\"1\" cellspacing=\"0\" cellpadding=\"0\">");
                final JsonArray msgs = txJson.getJsonArray("tx_related_messages");
                for (int i = 0; i < msgs.size(); ++i) {
                    final JsonObject msgJson = msgs.getJsonObject(i);
                    final JsonObject props = msgJson.getJsonObject("message_properties");
                    final StringBuilder propstr = new StringBuilder();
                    final Set<String> keys = (Set<String>)props.keySet();
                    for (final String key : keys) {
                        propstr.append(key);
                        propstr.append("=");
                        propstr.append(props.get((Object)key));
                        propstr.append(", ");
                    }
                    html.append("<th>operation_type</th>");
                    html.append("<td>" + msgJson.get((Object)"message_operation_type") + "</th>");
                    html.append("<th>message_type</th>");
                    html.append("<td>" + msgJson.get((Object)"message_type") + "</td></tr>");
                    html.append("<tr><th>properties</th>");
                    html.append("<td colspan=\"3\">" + propstr.toString() + "</td></tr>");
                }
                html.append("</table></td></tr>");
                html.append("</table><br>");
            }
            return html.toString();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public String[] listHeuristicCommittedTransactions() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.listHeuristicCommittedTransactions((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            final List<Xid> xids = this.resourceManager.getHeuristicCommittedTransactions();
            final String[] s = new String[xids.size()];
            int i = 0;
            for (final Xid xid : xids) {
                s[i++] = XidImpl.toBase64String(xid);
            }
            return s;
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public String[] listHeuristicRolledBackTransactions() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.listHeuristicRolledBackTransactions((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            final List<Xid> xids = this.resourceManager.getHeuristicRolledbackTransactions();
            final String[] s = new String[xids.size()];
            int i = 0;
            for (final Xid xid : xids) {
                s[i++] = XidImpl.toBase64String(xid);
            }
            return s;
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public synchronized boolean commitPreparedTransaction(final String transactionAsBase64) throws Exception {
        if (AuditLogger.isEnabled()) {
            AuditLogger.commitPreparedTransaction((Object)this.server, new Object[] { transactionAsBase64 });
        }
        this.checkStarted();
        this.clearIO();
        try {
            final List<Xid> xids = this.resourceManager.getPreparedTransactions();
            for (final Xid xid : xids) {
                if (XidImpl.toBase64String(xid).equals(transactionAsBase64)) {
                    final Transaction transaction = this.resourceManager.removeTransaction(xid);
                    transaction.commit(false);
                    final long recordID = this.server.getStorageManager().storeHeuristicCompletion(xid, true);
                    this.storageManager.waitOnOperations();
                    this.resourceManager.putHeuristicCompletion(recordID, xid, true);
                    return true;
                }
            }
            return false;
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public synchronized boolean rollbackPreparedTransaction(final String transactionAsBase64) throws Exception {
        if (AuditLogger.isEnabled()) {
            AuditLogger.rollbackPreparedTransaction((Object)this.server, new Object[] { transactionAsBase64 });
        }
        this.checkStarted();
        this.clearIO();
        try {
            final List<Xid> xids = this.resourceManager.getPreparedTransactions();
            for (final Xid xid : xids) {
                if (XidImpl.toBase64String(xid).equals(transactionAsBase64)) {
                    final Transaction transaction = this.resourceManager.removeTransaction(xid);
                    transaction.rollback();
                    final long recordID = this.server.getStorageManager().storeHeuristicCompletion(xid, false);
                    this.server.getStorageManager().waitOnOperations();
                    this.resourceManager.putHeuristicCompletion(recordID, xid, false);
                    return true;
                }
            }
            return false;
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public String[] listRemoteAddresses() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.listRemoteAddresses((Object)this.server, new Object[0]);
        }
        this.checkStarted();
        this.clearIO();
        try {
            final Set<RemotingConnection> connections = this.remotingService.getConnections();
            final String[] remoteAddresses = new String[connections.size()];
            int i = 0;
            for (final RemotingConnection connection : connections) {
                remoteAddresses[i++] = connection.getRemoteAddress();
            }
            return remoteAddresses;
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public String[] listRemoteAddresses(final String ipAddress) {
        if (AuditLogger.isEnabled()) {
            AuditLogger.listRemoteAddresses((Object)this.server, new Object[] { ipAddress });
        }
        this.checkStarted();
        this.clearIO();
        try {
            final Set<RemotingConnection> connections = this.remotingService.getConnections();
            final List<String> remoteConnections = new ArrayList<String>();
            for (final RemotingConnection connection : connections) {
                final String remoteAddress = connection.getRemoteAddress();
                if (remoteAddress.contains(ipAddress)) {
                    remoteConnections.add(connection.getRemoteAddress());
                }
            }
            return remoteConnections.toArray(new String[remoteConnections.size()]);
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public boolean closeConnectionsForAddress(final String ipAddress) {
        if (AuditLogger.isEnabled()) {
            AuditLogger.closeConnectionsForAddress((Object)this.server, new Object[] { ipAddress });
        }
        this.checkStarted();
        this.clearIO();
        try {
            boolean closed = false;
            final Set<RemotingConnection> connections = this.remotingService.getConnections();
            for (final RemotingConnection connection : connections) {
                final String remoteAddress = connection.getRemoteAddress();
                if (remoteAddress.contains(ipAddress)) {
                    connection.fail((ActiveMQException)ActiveMQMessageBundle.BUNDLE.connectionsClosedByManagement(ipAddress));
                    this.remotingService.removeConnection(connection.getID());
                    closed = true;
                }
            }
            return closed;
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public boolean closeConsumerConnectionsForAddress(final String address) {
        if (AuditLogger.isEnabled()) {
            AuditLogger.closeConsumerConnectionsForAddress((Object)this.server, new Object[] { address });
        }
        boolean closed = false;
        this.checkStarted();
        this.clearIO();
        try {
            for (final Binding binding : this.postOffice.getMatchingBindings(SimpleString.toSimpleString(address)).getBindings()) {
                if (binding instanceof LocalQueueBinding) {
                    final Queue queue = ((LocalQueueBinding)binding).getQueue();
                    for (final Consumer consumer : queue.getConsumers()) {
                        if (consumer instanceof ServerConsumer) {
                            final ServerConsumer serverConsumer = (ServerConsumer)consumer;
                            RemotingConnection connection = null;
                            for (final RemotingConnection potentialConnection : this.remotingService.getConnections()) {
                                if (potentialConnection.getID().toString().equals(serverConsumer.getConnectionID())) {
                                    connection = potentialConnection;
                                }
                            }
                            if (connection == null) {
                                continue;
                            }
                            this.remotingService.removeConnection(connection.getID());
                            connection.fail((ActiveMQException)ActiveMQMessageBundle.BUNDLE.consumerConnectionsClosedByManagement(address));
                            closed = true;
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            ActiveMQServerLogger.LOGGER.failedToCloseConsumerConnectionsForAddress(address, e);
        }
        finally {
            this.blockOnIO();
        }
        return closed;
    }
    
    public boolean closeConnectionsForUser(final String userName) {
        if (AuditLogger.isEnabled()) {
            AuditLogger.closeConnectionsForUser((Object)this.server, new Object[] { userName });
        }
        boolean closed = false;
        this.checkStarted();
        this.clearIO();
        try {
            for (final ServerSession serverSession : this.server.getSessions()) {
                if (serverSession.getUsername() != null && serverSession.getUsername().equals(userName)) {
                    RemotingConnection connection = null;
                    for (final RemotingConnection potentialConnection : this.remotingService.getConnections()) {
                        if (potentialConnection.getID().toString().equals(serverSession.getConnectionID().toString())) {
                            connection = potentialConnection;
                        }
                    }
                    if (connection == null) {
                        continue;
                    }
                    this.remotingService.removeConnection(connection.getID());
                    connection.fail((ActiveMQException)ActiveMQMessageBundle.BUNDLE.connectionsForUserClosedByManagement(userName));
                    closed = true;
                }
            }
        }
        finally {
            this.blockOnIO();
        }
        return closed;
    }
    
    public boolean closeConnectionWithID(final String ID) {
        if (AuditLogger.isEnabled()) {
            AuditLogger.closeConnectionWithID((Object)this.server, new Object[] { ID });
        }
        this.checkStarted();
        this.clearIO();
        try {
            for (final RemotingConnection connection : this.remotingService.getConnections()) {
                if (connection.getID().toString().equals(ID)) {
                    this.remotingService.removeConnection(connection.getID());
                    connection.fail((ActiveMQException)ActiveMQMessageBundle.BUNDLE.connectionWithIDClosedByManagement(ID));
                    return true;
                }
            }
        }
        finally {
            this.blockOnIO();
        }
        return false;
    }
    
    public boolean closeSessionWithID(final String connectionID, final String ID) throws Exception {
        if (AuditLogger.isEnabled()) {
            AuditLogger.closeSessionWithID((Object)this.server, new Object[] { connectionID, ID });
        }
        this.checkStarted();
        this.clearIO();
        try {
            final List<ServerSession> sessions = this.server.getSessions(connectionID);
            for (final ServerSession session : sessions) {
                if (session.getName().equals(ID.toString())) {
                    session.close(true);
                    return true;
                }
            }
        }
        finally {
            this.blockOnIO();
        }
        return false;
    }
    
    public boolean closeConsumerWithID(final String sessionID, final String ID) throws Exception {
        if (AuditLogger.isEnabled()) {
            AuditLogger.closeConsumerWithID((Object)this.server, new Object[] { sessionID, ID });
        }
        this.checkStarted();
        this.clearIO();
        try {
            final Set<ServerSession> sessions = this.server.getSessions();
            for (final ServerSession session : sessions) {
                if (session.getName().equals(sessionID.toString())) {
                    final Set<ServerConsumer> serverConsumers = session.getServerConsumers();
                    for (final ServerConsumer serverConsumer : serverConsumers) {
                        if (serverConsumer.sequentialID() == Long.valueOf(ID)) {
                            serverConsumer.disconnect();
                            return true;
                        }
                    }
                }
            }
        }
        finally {
            this.blockOnIO();
        }
        return false;
    }
    
    public String[] listConnectionIDs() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.listConnectionIDs((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            final Set<RemotingConnection> connections = this.remotingService.getConnections();
            final String[] connectionIDs = new String[connections.size()];
            int i = 0;
            for (final RemotingConnection connection : connections) {
                connectionIDs[i++] = connection.getID().toString();
            }
            return connectionIDs;
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public String[] listSessions(final String connectionID) {
        if (AuditLogger.isEnabled()) {
            AuditLogger.listSessions((Object)this.server, new Object[] { connectionID });
        }
        this.checkStarted();
        this.clearIO();
        try {
            final List<ServerSession> sessions = this.server.getSessions(connectionID);
            final String[] sessionIDs = new String[sessions.size()];
            int i = 0;
            for (final ServerSession serverSession : sessions) {
                sessionIDs[i++] = serverSession.getName();
            }
            return sessionIDs;
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public String listProducersInfoAsJSON() throws Exception {
        if (AuditLogger.isEnabled()) {
            AuditLogger.listProducersInfoAsJSON((Object)this.server);
        }
        final JsonArrayBuilder producers = JsonLoader.createArrayBuilder();
        for (final ServerSession session : this.server.getSessions()) {
            session.describeProducersInfo(producers);
        }
        return producers.build().toString();
    }
    
    public String listConnections(final String options, final int page, final int pageSize) throws Exception {
        if (AuditLogger.isEnabled()) {
            AuditLogger.listConnections((Object)this.server, new Object[] { options, page, pageSize });
        }
        this.checkStarted();
        this.clearIO();
        try {
            this.server.getPostOffice().getAddresses();
            final ConnectionView view = new ConnectionView(this.server);
            view.setCollection(this.server.getRemotingService().getConnections());
            view.setOptions(options);
            return view.getResultsAsJson(page, pageSize);
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public String listSessions(final String options, final int page, final int pageSize) throws Exception {
        if (AuditLogger.isEnabled()) {
            AuditLogger.listSessions((Object)this.server, new Object[] { options, page, pageSize });
        }
        this.checkStarted();
        this.clearIO();
        try {
            final SessionView view = new SessionView();
            view.setCollection(this.server.getSessions());
            view.setOptions(options);
            return view.getResultsAsJson(page, pageSize);
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public String listConsumers(final String options, final int page, final int pageSize) throws Exception {
        if (AuditLogger.isEnabled()) {
            AuditLogger.listConsumers((Object)this.server, new Object[] { options, page, pageSize });
        }
        this.checkStarted();
        this.clearIO();
        try {
            final Set<ServerConsumer> consumers = new HashSet<ServerConsumer>();
            for (final ServerSession session : this.server.getSessions()) {
                consumers.addAll(session.getServerConsumers());
            }
            final ConsumerView view = new ConsumerView(this.server);
            view.setCollection(consumers);
            view.setOptions(options);
            return view.getResultsAsJson(page, pageSize);
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public String listAddresses(final String options, final int page, final int pageSize) throws Exception {
        if (AuditLogger.isEnabled()) {
            AuditLogger.listAddresses((Object)this.server, new Object[] { options, page, pageSize });
        }
        this.checkStarted();
        this.clearIO();
        try {
            final Set<SimpleString> addresses = this.server.getPostOffice().getAddresses();
            final List<AddressInfo> addressInfo = new ArrayList<AddressInfo>();
            for (final SimpleString address : addresses) {
                final AddressInfo info = this.server.getPostOffice().getAddressInfo(address);
                if (info != null) {
                    addressInfo.add(info);
                }
            }
            final AddressView view = new AddressView(this.server);
            view.setCollection(addressInfo);
            view.setOptions(options);
            return view.getResultsAsJson(page, pageSize);
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public String listQueues(final String options, final int page, final int pageSize) throws Exception {
        if (AuditLogger.isEnabled()) {
            AuditLogger.listQueues((Object)this.server, new Object[] { options, page, pageSize });
        }
        this.checkStarted();
        this.clearIO();
        try {
            final List<QueueControl> queues = new ArrayList<QueueControl>();
            final Object[] qs = this.server.getManagementService().getResources(QueueControl.class);
            for (int i = 0; i < qs.length; ++i) {
                queues.add((QueueControl)qs[i]);
            }
            final QueueView view = new QueueView(this.server);
            view.setCollection(queues);
            view.setOptions(options);
            return view.getResultsAsJson(page, pageSize);
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public String listProducers(@Parameter(name = "Options") final String options, @Parameter(name = "Page Number") final int page, @Parameter(name = "Page Size") final int pageSize) throws Exception {
        if (AuditLogger.isEnabled()) {
            AuditLogger.listProducers((Object)this.server, new Object[] { options, page, pageSize });
        }
        this.checkStarted();
        this.clearIO();
        try {
            final Set<ServerProducer> producers = new HashSet<ServerProducer>();
            for (final ServerSession session : this.server.getSessions()) {
                producers.addAll(session.getServerProducers().values());
            }
            final ProducerView view = new ProducerView(this.server);
            view.setCollection(producers);
            view.setOptions(options);
            return view.getResultsAsJson(page, pageSize);
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public String listConnectionsAsJSON() throws Exception {
        if (AuditLogger.isEnabled()) {
            AuditLogger.listConnectionsAsJSON((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            final JsonArrayBuilder array = JsonLoader.createArrayBuilder();
            final Set<RemotingConnection> connections = this.server.getRemotingService().getConnections();
            for (final RemotingConnection connection : connections) {
                final JsonObjectBuilder obj = JsonLoader.createObjectBuilder().add("connectionID", connection.getID().toString()).add("clientAddress", connection.getRemoteAddress()).add("creationTime", connection.getCreationTime()).add("implementation", connection.getClass().getSimpleName()).add("sessionCount", this.server.getSessions(connection.getID().toString()).size());
                array.add(obj);
            }
            return array.build().toString();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public String listSessionsAsJSON(final String connectionID) throws Exception {
        if (AuditLogger.isEnabled()) {
            AuditLogger.listSessionsAsJSON((Object)this.server, new Object[] { connectionID });
        }
        this.checkStarted();
        this.clearIO();
        final JsonArrayBuilder array = JsonLoader.createArrayBuilder();
        try {
            final List<ServerSession> sessions = this.server.getSessions(connectionID);
            for (final ServerSession sess : sessions) {
                this.buildSessionJSON(array, sess);
            }
        }
        finally {
            this.blockOnIO();
        }
        return array.build().toString();
    }
    
    public String listAllSessionsAsJSON() throws Exception {
        if (AuditLogger.isEnabled()) {
            AuditLogger.listAllSessionsAsJSON((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        final JsonArrayBuilder array = JsonLoader.createArrayBuilder();
        try {
            final Set<ServerSession> sessions = this.server.getSessions();
            for (final ServerSession sess : sessions) {
                this.buildSessionJSON(array, sess);
            }
        }
        finally {
            this.blockOnIO();
        }
        return array.build().toString();
    }
    
    public void buildSessionJSON(final JsonArrayBuilder array, final ServerSession sess) {
        final JsonObjectBuilder obj = JsonLoader.createObjectBuilder().add("sessionID", sess.getName()).add("creationTime", sess.getCreationTime()).add("consumerCount", sess.getServerConsumers().size());
        if (sess.getValidatedUser() != null) {
            obj.add("principal", sess.getValidatedUser());
        }
        if (sess.getMetaData() != null) {
            final JsonObjectBuilder metadata = JsonLoader.createObjectBuilder();
            for (final Map.Entry<String, String> entry : sess.getMetaData().entrySet()) {
                metadata.add((String)entry.getKey(), (String)entry.getValue());
            }
            obj.add("metadata", metadata);
        }
        array.add(obj);
    }
    
    public String listConsumersAsJSON(final String connectionID) throws Exception {
        if (AuditLogger.isEnabled()) {
            AuditLogger.listConsumersAsJSON((Object)this.server, new Object[] { connectionID });
        }
        this.checkStarted();
        this.clearIO();
        try {
            final JsonArrayBuilder array = JsonLoader.createArrayBuilder();
            final Set<RemotingConnection> connections = this.server.getRemotingService().getConnections();
            for (final RemotingConnection connection : connections) {
                if (connectionID.equals(connection.getID().toString())) {
                    final List<ServerSession> sessions = this.server.getSessions(connectionID);
                    for (final ServerSession session : sessions) {
                        final Set<ServerConsumer> consumers = session.getServerConsumers();
                        for (final ServerConsumer consumer : consumers) {
                            final JsonObject obj = this.toJSONObject(consumer);
                            if (obj != null) {
                                array.add((JsonValue)obj);
                            }
                        }
                    }
                }
            }
            return array.build().toString();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public String listAllConsumersAsJSON() throws Exception {
        if (AuditLogger.isEnabled()) {
            AuditLogger.listAllConsumersAsJSON((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            final JsonArrayBuilder array = JsonLoader.createArrayBuilder();
            final Set<ServerSession> sessions = this.server.getSessions();
            for (final ServerSession session : sessions) {
                final Set<ServerConsumer> consumers = session.getServerConsumers();
                for (final ServerConsumer consumer : consumers) {
                    final JsonObject obj = this.toJSONObject(consumer);
                    if (obj != null) {
                        array.add((JsonValue)obj);
                    }
                }
            }
            return array.build().toString();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    private JsonObject toJSONObject(final ServerConsumer consumer) throws Exception {
        final JsonObjectBuilder obj = JsonLoader.createObjectBuilder().add("consumerID", consumer.getID()).add("connectionID", consumer.getConnectionID().toString()).add("sessionID", consumer.getSessionID()).add("queueName", consumer.getQueue().getName().toString()).add("browseOnly", consumer.isBrowseOnly()).add("creationTime", consumer.getCreationTime()).add("deliveringCount", consumer.getDeliveringMessages().size());
        if (consumer.getFilter() != null) {
            obj.add("filter", consumer.getFilter().getFilterString().toString());
        }
        return obj.build();
    }
    
    public Object[] getConnectors() throws Exception {
        if (AuditLogger.isEnabled()) {
            AuditLogger.getConnectors((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            final Collection<TransportConfiguration> connectorConfigurations = this.configuration.getConnectorConfigurations().values();
            final Object[] ret = new Object[connectorConfigurations.size()];
            int i = 0;
            for (final TransportConfiguration config : connectorConfigurations) {
                final Object[] tc = { config.getName(), config.getFactoryClassName(), config.getParams() };
                ret[i++] = tc;
            }
            return ret;
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public String getConnectorsAsJSON() throws Exception {
        if (AuditLogger.isEnabled()) {
            AuditLogger.getConnectorsAsJSON((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            final JsonArrayBuilder array = JsonLoader.createArrayBuilder();
            for (final TransportConfiguration config : this.configuration.getConnectorConfigurations().values()) {
                array.add((JsonValue)config.toJson());
            }
            return array.build().toString();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public void addSecuritySettings(final String addressMatch, final String sendRoles, final String consumeRoles, final String createDurableQueueRoles, final String deleteDurableQueueRoles, final String createNonDurableQueueRoles, final String deleteNonDurableQueueRoles, final String manageRoles) throws Exception {
        this.addSecuritySettings(addressMatch, sendRoles, consumeRoles, createDurableQueueRoles, deleteDurableQueueRoles, createNonDurableQueueRoles, deleteNonDurableQueueRoles, manageRoles, "");
    }
    
    public void addSecuritySettings(final String addressMatch, final String sendRoles, final String consumeRoles, final String createDurableQueueRoles, final String deleteDurableQueueRoles, final String createNonDurableQueueRoles, final String deleteNonDurableQueueRoles, final String manageRoles, final String browseRoles) throws Exception {
        this.addSecuritySettings(addressMatch, sendRoles, consumeRoles, createDurableQueueRoles, deleteDurableQueueRoles, createNonDurableQueueRoles, deleteNonDurableQueueRoles, manageRoles, browseRoles, "", "");
    }
    
    public void addSecuritySettings(final String addressMatch, final String sendRoles, final String consumeRoles, final String createDurableQueueRoles, final String deleteDurableQueueRoles, final String createNonDurableQueueRoles, final String deleteNonDurableQueueRoles, final String manageRoles, final String browseRoles, final String createAddressRoles, final String deleteAddressRoles) throws Exception {
        if (AuditLogger.isEnabled()) {
            AuditLogger.addSecuritySettings((Object)this.server, new Object[] { addressMatch, sendRoles, consumeRoles, createDurableQueueRoles, deleteDurableQueueRoles, createNonDurableQueueRoles, deleteNonDurableQueueRoles, manageRoles, browseRoles, createAddressRoles, deleteAddressRoles });
        }
        this.checkStarted();
        this.clearIO();
        try {
            final Set<Role> roles = (Set<Role>)SecurityFormatter.createSecurity(sendRoles, consumeRoles, createDurableQueueRoles, deleteDurableQueueRoles, createNonDurableQueueRoles, deleteNonDurableQueueRoles, manageRoles, browseRoles, createAddressRoles, deleteAddressRoles);
            this.server.getSecurityRepository().addMatch(addressMatch, roles);
            final PersistedRoles persistedRoles = new PersistedRoles(addressMatch, sendRoles, consumeRoles, createDurableQueueRoles, deleteDurableQueueRoles, createNonDurableQueueRoles, deleteNonDurableQueueRoles, manageRoles, browseRoles, createAddressRoles, deleteAddressRoles);
            this.storageManager.storeSecurityRoles(persistedRoles);
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public void removeSecuritySettings(final String addressMatch) throws Exception {
        if (AuditLogger.isEnabled()) {
            AuditLogger.removeSecuritySettings((Object)this.server, new Object[] { addressMatch });
        }
        this.checkStarted();
        this.clearIO();
        try {
            this.server.getSecurityRepository().removeMatch(addressMatch);
            this.storageManager.deleteSecurityRoles(new SimpleString(addressMatch));
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public Object[] getRoles(final String addressMatch) throws Exception {
        if (AuditLogger.isEnabled()) {
            AuditLogger.getRoles((Object)this.server, new Object[] { addressMatch });
        }
        this.checkStarted();
        this.clearIO();
        try {
            final Set<Role> roles = this.server.getSecurityRepository().getMatch(addressMatch);
            final Object[] objRoles = new Object[roles.size()];
            int i = 0;
            for (final Role role : roles) {
                objRoles[i++] = new Object[] { role.getName(), CheckType.SEND.hasRole(role), CheckType.CONSUME.hasRole(role), CheckType.CREATE_DURABLE_QUEUE.hasRole(role), CheckType.DELETE_DURABLE_QUEUE.hasRole(role), CheckType.CREATE_NON_DURABLE_QUEUE.hasRole(role), CheckType.DELETE_NON_DURABLE_QUEUE.hasRole(role), CheckType.MANAGE.hasRole(role) };
            }
            return objRoles;
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public String getRolesAsJSON(final String addressMatch) throws Exception {
        if (AuditLogger.isEnabled()) {
            AuditLogger.getRolesAsJSON((Object)this.server, new Object[] { addressMatch });
        }
        this.checkStarted();
        this.clearIO();
        try {
            final JsonArrayBuilder json = JsonLoader.createArrayBuilder();
            final Set<Role> roles = this.server.getSecurityRepository().getMatch(addressMatch);
            for (final Role role : roles) {
                json.add((JsonValue)role.toJson());
            }
            return json.build().toString();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public String getAddressSettingsAsJSON(final String address) throws Exception {
        if (AuditLogger.isEnabled()) {
            AuditLogger.getAddressSettingsAsJSON((Object)this.server, new Object[] { address });
        }
        this.checkStarted();
        final AddressSettings addressSettings = this.server.getAddressSettingsRepository().getMatch(address);
        final String policy = (addressSettings.getAddressFullMessagePolicy() == AddressFullMessagePolicy.PAGE) ? "PAGE" : ((addressSettings.getAddressFullMessagePolicy() == AddressFullMessagePolicy.BLOCK) ? "BLOCK" : ((addressSettings.getAddressFullMessagePolicy() == AddressFullMessagePolicy.DROP) ? "DROP" : "FAIL"));
        final String consumerPolicy = (addressSettings.getSlowConsumerPolicy() == SlowConsumerPolicy.NOTIFY) ? "NOTIFY" : "KILL";
        final JsonObjectBuilder settings = JsonLoader.createObjectBuilder();
        if (addressSettings.getDeadLetterAddress() != null) {
            settings.add("DLA", addressSettings.getDeadLetterAddress().toString());
        }
        if (addressSettings.getExpiryAddress() != null) {
            settings.add("expiryAddress", addressSettings.getExpiryAddress().toString());
        }
        return settings.add("expiryDelay", (long)addressSettings.getExpiryDelay()).add("maxDeliveryAttempts", addressSettings.getMaxDeliveryAttempts()).add("pageCacheMaxSize", addressSettings.getPageCacheMaxSize()).add("maxSizeBytes", addressSettings.getMaxSizeBytes()).add("pageSizeBytes", addressSettings.getPageSizeBytes()).add("redeliveryDelay", addressSettings.getRedeliveryDelay()).add("redeliveryMultiplier", addressSettings.getRedeliveryMultiplier()).add("maxRedeliveryDelay", addressSettings.getMaxRedeliveryDelay()).add("redistributionDelay", addressSettings.getRedistributionDelay()).add("lastValueQueue", addressSettings.isDefaultLastValueQueue()).add("sendToDLAOnNoRoute", addressSettings.isSendToDLAOnNoRoute()).add("addressFullMessagePolicy", policy).add("slowConsumerThreshold", addressSettings.getSlowConsumerThreshold()).add("slowConsumerCheckPeriod", addressSettings.getSlowConsumerCheckPeriod()).add("slowConsumerPolicy", consumerPolicy).add("autoCreateJmsQueues", addressSettings.isAutoCreateJmsQueues()).add("autoCreateJmsTopics", addressSettings.isAutoCreateJmsTopics()).add("autoDeleteJmsQueues", addressSettings.isAutoDeleteJmsQueues()).add("autoDeleteJmsTopics", addressSettings.isAutoDeleteJmsQueues()).add("autoCreateQueues", addressSettings.isAutoCreateQueues()).add("autoDeleteQueues", addressSettings.isAutoDeleteQueues()).add("autoCreateAddress", addressSettings.isAutoCreateAddresses()).add("autoDeleteAddress", addressSettings.isAutoDeleteAddresses()).build().toString();
    }
    
    public void addAddressSettings(final String address, final String DLA, final String expiryAddress, final long expiryDelay, final boolean lastValueQueue, final int deliveryAttempts, final long maxSizeBytes, final int pageSizeBytes, final int pageMaxCacheSize, final long redeliveryDelay, final double redeliveryMultiplier, final long maxRedeliveryDelay, final long redistributionDelay, final boolean sendToDLAOnNoRoute, final String addressFullMessagePolicy, final long slowConsumerThreshold, final long slowConsumerCheckPeriod, final String slowConsumerPolicy, final boolean autoCreateJmsQueues, final boolean autoDeleteJmsQueues, final boolean autoCreateJmsTopics, final boolean autoDeleteJmsTopics) throws Exception {
        this.addAddressSettings(address, DLA, expiryAddress, expiryDelay, lastValueQueue, deliveryAttempts, maxSizeBytes, pageSizeBytes, pageMaxCacheSize, redeliveryDelay, redeliveryMultiplier, maxRedeliveryDelay, redistributionDelay, sendToDLAOnNoRoute, addressFullMessagePolicy, slowConsumerThreshold, slowConsumerCheckPeriod, slowConsumerPolicy, autoCreateJmsQueues, autoDeleteJmsQueues, autoCreateJmsTopics, autoDeleteJmsTopics, true, true, true, true);
    }
    
    public void addAddressSettings(final String address, final String DLA, final String expiryAddress, final long expiryDelay, final boolean lastValueQueue, final int deliveryAttempts, final long maxSizeBytes, final int pageSizeBytes, final int pageMaxCacheSize, final long redeliveryDelay, final double redeliveryMultiplier, final long maxRedeliveryDelay, final long redistributionDelay, final boolean sendToDLAOnNoRoute, final String addressFullMessagePolicy, final long slowConsumerThreshold, final long slowConsumerCheckPeriod, final String slowConsumerPolicy, final boolean autoCreateJmsQueues, final boolean autoDeleteJmsQueues, final boolean autoCreateJmsTopics, final boolean autoDeleteJmsTopics, final boolean autoCreateQueues, final boolean autoDeleteQueues, final boolean autoCreateAddresses, final boolean autoDeleteAddresses) throws Exception {
        if (AuditLogger.isEnabled()) {
            AuditLogger.addAddressSettings((Object)this.server, new Object[] { address, DLA, expiryAddress, expiryDelay, lastValueQueue, deliveryAttempts, maxSizeBytes, pageSizeBytes, pageMaxCacheSize, redeliveryDelay, redeliveryMultiplier, maxRedeliveryDelay, redistributionDelay, sendToDLAOnNoRoute, addressFullMessagePolicy, slowConsumerThreshold, slowConsumerCheckPeriod, slowConsumerPolicy, autoCreateJmsQueues, autoDeleteJmsQueues, autoCreateJmsTopics, autoDeleteJmsTopics, autoCreateQueues, autoDeleteQueues, autoCreateAddresses, autoDeleteAddresses });
        }
        this.checkStarted();
        if (pageSizeBytes > maxSizeBytes && maxSizeBytes > 0L) {
            throw new IllegalStateException("pageSize has to be lower than maxSizeBytes. Invalid argument (" + pageSizeBytes + " < " + maxSizeBytes + ")");
        }
        if (maxSizeBytes < -1L) {
            throw new IllegalStateException("Invalid argument on maxSizeBytes");
        }
        final AddressSettings addressSettings = new AddressSettings();
        addressSettings.setDeadLetterAddress((DLA == null) ? null : new SimpleString(DLA));
        addressSettings.setExpiryAddress((expiryAddress == null) ? null : new SimpleString(expiryAddress));
        addressSettings.setExpiryDelay(expiryDelay);
        addressSettings.setDefaultLastValueQueue(lastValueQueue);
        addressSettings.setMaxDeliveryAttempts(deliveryAttempts);
        addressSettings.setPageCacheMaxSize(pageMaxCacheSize);
        addressSettings.setMaxSizeBytes(maxSizeBytes);
        addressSettings.setPageSizeBytes(pageSizeBytes);
        addressSettings.setRedeliveryDelay(redeliveryDelay);
        addressSettings.setRedeliveryMultiplier(redeliveryMultiplier);
        addressSettings.setMaxRedeliveryDelay(maxRedeliveryDelay);
        addressSettings.setRedistributionDelay(redistributionDelay);
        addressSettings.setSendToDLAOnNoRoute(sendToDLAOnNoRoute);
        if (addressFullMessagePolicy == null) {
            addressSettings.setAddressFullMessagePolicy(AddressFullMessagePolicy.PAGE);
        }
        else if (addressFullMessagePolicy.equalsIgnoreCase("PAGE")) {
            addressSettings.setAddressFullMessagePolicy(AddressFullMessagePolicy.PAGE);
        }
        else if (addressFullMessagePolicy.equalsIgnoreCase("DROP")) {
            addressSettings.setAddressFullMessagePolicy(AddressFullMessagePolicy.DROP);
        }
        else if (addressFullMessagePolicy.equalsIgnoreCase("BLOCK")) {
            addressSettings.setAddressFullMessagePolicy(AddressFullMessagePolicy.BLOCK);
        }
        else if (addressFullMessagePolicy.equalsIgnoreCase("FAIL")) {
            addressSettings.setAddressFullMessagePolicy(AddressFullMessagePolicy.FAIL);
        }
        addressSettings.setSlowConsumerThreshold(slowConsumerThreshold);
        addressSettings.setSlowConsumerCheckPeriod(slowConsumerCheckPeriod);
        if (slowConsumerPolicy == null) {
            addressSettings.setSlowConsumerPolicy(SlowConsumerPolicy.NOTIFY);
        }
        else if (slowConsumerPolicy.equalsIgnoreCase("NOTIFY")) {
            addressSettings.setSlowConsumerPolicy(SlowConsumerPolicy.NOTIFY);
        }
        else if (slowConsumerPolicy.equalsIgnoreCase("KILL")) {
            addressSettings.setSlowConsumerPolicy(SlowConsumerPolicy.KILL);
        }
        addressSettings.setAutoCreateJmsQueues(autoCreateJmsQueues);
        addressSettings.setAutoDeleteJmsQueues(autoDeleteJmsQueues);
        addressSettings.setAutoCreateJmsTopics(autoCreateJmsTopics);
        addressSettings.setAutoDeleteJmsTopics(autoDeleteJmsTopics);
        addressSettings.setAutoCreateQueues(autoCreateQueues);
        addressSettings.setAutoDeleteQueues(autoDeleteQueues);
        addressSettings.setAutoCreateAddresses(autoCreateAddresses);
        addressSettings.setAutoDeleteAddresses(autoDeleteAddresses);
        this.server.getAddressSettingsRepository().addMatch(address, addressSettings);
        this.storageManager.storeAddressSetting(new PersistedAddressSetting(new SimpleString(address), addressSettings));
    }
    
    public void removeAddressSettings(final String addressMatch) throws Exception {
        if (AuditLogger.isEnabled()) {
            AuditLogger.removeAddressSettings((Object)this.server, new Object[] { addressMatch });
        }
        this.checkStarted();
        this.server.getAddressSettingsRepository().removeMatch(addressMatch);
        this.storageManager.deleteAddressSetting(new SimpleString(addressMatch));
    }
    
    public void sendQueueInfoToQueue(final String queueName, final String address) throws Exception {
        this.checkStarted();
        this.clearIO();
        try {
            this.postOffice.sendQueueInfoToQueue(new SimpleString(queueName), new SimpleString((address == null) ? "" : address));
            final GroupingHandler handler = this.server.getGroupingHandler();
            if (handler != null) {
                handler.resendPending();
            }
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public String[] getDivertNames() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.getDivertNames((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            final Object[] diverts = this.server.getManagementService().getResources(DivertControl.class);
            final String[] names = new String[diverts.length];
            for (int i = 0; i < diverts.length; ++i) {
                final DivertControl divert = (DivertControl)diverts[i];
                names[i] = divert.getUniqueName();
            }
            return names;
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public void createDivert(final String name, final String routingName, final String address, final String forwardingAddress, final boolean exclusive, final String filterString, final String transformerClassName) throws Exception {
        this.createDivert(name, routingName, address, forwardingAddress, exclusive, filterString, transformerClassName, ActiveMQDefaultConfiguration.getDefaultDivertRoutingType());
    }
    
    public void createDivert(final String name, final String routingName, final String address, final String forwardingAddress, final boolean exclusive, final String filterString, final String transformerClassName, final String routingType) throws Exception {
        this.createDivert(name, routingName, address, forwardingAddress, exclusive, filterString, transformerClassName, (String)null, routingType);
    }
    
    public void createDivert(final String name, final String routingName, final String address, final String forwardingAddress, final boolean exclusive, final String filterString, final String transformerClassName, final String transformerPropertiesAsJSON, final String routingType) throws Exception {
        this.createDivert(name, routingName, address, forwardingAddress, exclusive, filterString, transformerClassName, JsonUtil.readJsonProperties(transformerPropertiesAsJSON), routingType);
    }
    
    public void createDivert(final String name, final String routingName, final String address, final String forwardingAddress, final boolean exclusive, final String filterString, final String transformerClassName, final Map<String, String> transformerProperties, final String routingType) throws Exception {
        if (AuditLogger.isEnabled()) {
            AuditLogger.createDivert((Object)this.server, new Object[] { name, routingName, address, forwardingAddress, exclusive, filterString, transformerClassName, transformerProperties, routingType });
        }
        this.checkStarted();
        this.clearIO();
        try {
            final TransformerConfiguration transformerConfiguration = (transformerClassName == null) ? null : new TransformerConfiguration(transformerClassName).setProperties(transformerProperties);
            final DivertConfiguration config = new DivertConfiguration().setName(name).setRoutingName(routingName).setAddress(address).setForwardingAddress(forwardingAddress).setExclusive(exclusive).setFilterString(filterString).setTransformerConfiguration(transformerConfiguration).setRoutingType(ComponentConfigurationRoutingType.valueOf(routingType));
            this.server.deployDivert(config);
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public void destroyDivert(final String name) throws Exception {
        if (AuditLogger.isEnabled()) {
            AuditLogger.destroyDivert((Object)this.server, new Object[] { name });
        }
        this.checkStarted();
        this.clearIO();
        try {
            this.server.destroyDivert(SimpleString.toSimpleString(name));
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public String[] getBridgeNames() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.getBridgeNames((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            final Object[] bridges = this.server.getManagementService().getResources(BridgeControl.class);
            final String[] names = new String[bridges.length];
            for (int i = 0; i < bridges.length; ++i) {
                final BridgeControl bridge = (BridgeControl)bridges[i];
                names[i] = bridge.getName();
            }
            return names;
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public void createBridge(final String name, final String queueName, final String forwardingAddress, final String filterString, final String transformerClassName, final long retryInterval, final double retryIntervalMultiplier, final int initialConnectAttempts, final int reconnectAttempts, final boolean useDuplicateDetection, final int confirmationWindowSize, final int producerWindowSize, final long clientFailureCheckPeriod, final String staticConnectorsOrDiscoveryGroup, final boolean useDiscoveryGroup, final boolean ha, final String user, final String password) throws Exception {
        this.createBridge(name, queueName, forwardingAddress, filterString, transformerClassName, (String)null, retryInterval, retryIntervalMultiplier, initialConnectAttempts, reconnectAttempts, useDuplicateDetection, confirmationWindowSize, producerWindowSize, clientFailureCheckPeriod, staticConnectorsOrDiscoveryGroup, useDiscoveryGroup, ha, user, password);
    }
    
    public void createBridge(final String name, final String queueName, final String forwardingAddress, final String filterString, final String transformerClassName, final String transformerPropertiesAsJSON, final long retryInterval, final double retryIntervalMultiplier, final int initialConnectAttempts, final int reconnectAttempts, final boolean useDuplicateDetection, final int confirmationWindowSize, final int producerWindowSize, final long clientFailureCheckPeriod, final String staticConnectorsOrDiscoveryGroup, final boolean useDiscoveryGroup, final boolean ha, final String user, final String password) throws Exception {
        this.createBridge(name, queueName, forwardingAddress, filterString, transformerClassName, JsonUtil.readJsonProperties(transformerPropertiesAsJSON), retryInterval, retryIntervalMultiplier, initialConnectAttempts, reconnectAttempts, useDuplicateDetection, confirmationWindowSize, producerWindowSize, clientFailureCheckPeriod, staticConnectorsOrDiscoveryGroup, useDiscoveryGroup, ha, user, password);
    }
    
    public void createBridge(final String name, final String queueName, final String forwardingAddress, final String filterString, final String transformerClassName, final Map<String, String> transformerProperties, final long retryInterval, final double retryIntervalMultiplier, final int initialConnectAttempts, final int reconnectAttempts, final boolean useDuplicateDetection, final int confirmationWindowSize, final int producerWindowSize, final long clientFailureCheckPeriod, final String staticConnectorsOrDiscoveryGroup, final boolean useDiscoveryGroup, final boolean ha, final String user, final String password) throws Exception {
        if (AuditLogger.isEnabled()) {
            AuditLogger.createBridge((Object)this.server, new Object[] { name, queueName, forwardingAddress, filterString, transformerClassName, transformerProperties, retryInterval, retryIntervalMultiplier, initialConnectAttempts, reconnectAttempts, useDuplicateDetection, confirmationWindowSize, producerWindowSize, clientFailureCheckPeriod, staticConnectorsOrDiscoveryGroup, useDiscoveryGroup, ha, user, "****" });
        }
        this.checkStarted();
        this.clearIO();
        try {
            final TransformerConfiguration transformerConfiguration = (transformerClassName == null) ? null : new TransformerConfiguration(transformerClassName).setProperties(transformerProperties);
            final BridgeConfiguration config = new BridgeConfiguration().setName(name).setQueueName(queueName).setForwardingAddress(forwardingAddress).setFilterString(filterString).setTransformerConfiguration(transformerConfiguration).setClientFailureCheckPeriod(clientFailureCheckPeriod).setRetryInterval(retryInterval).setRetryIntervalMultiplier(retryIntervalMultiplier).setInitialConnectAttempts(initialConnectAttempts).setReconnectAttempts(reconnectAttempts).setUseDuplicateDetection(useDuplicateDetection).setConfirmationWindowSize(confirmationWindowSize).setProducerWindowSize(producerWindowSize).setHA(ha).setUser(user).setPassword(password);
            if (useDiscoveryGroup) {
                config.setDiscoveryGroupName(staticConnectorsOrDiscoveryGroup);
            }
            else {
                config.setStaticConnectors(ListUtil.toList(staticConnectorsOrDiscoveryGroup));
            }
            this.server.deployBridge(config);
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public void createBridge(final String name, final String queueName, final String forwardingAddress, final String filterString, final String transformerClassName, final long retryInterval, final double retryIntervalMultiplier, final int initialConnectAttempts, final int reconnectAttempts, final boolean useDuplicateDetection, final int confirmationWindowSize, final long clientFailureCheckPeriod, final String staticConnectorsOrDiscoveryGroup, final boolean useDiscoveryGroup, final boolean ha, final String user, final String password) throws Exception {
        if (AuditLogger.isEnabled()) {
            AuditLogger.createBridge((Object)this.server, new Object[] { name, queueName, forwardingAddress, filterString, transformerClassName, retryInterval, retryIntervalMultiplier, initialConnectAttempts, reconnectAttempts, useDuplicateDetection, confirmationWindowSize, clientFailureCheckPeriod, staticConnectorsOrDiscoveryGroup, useDiscoveryGroup, ha, user, "****" });
        }
        this.checkStarted();
        this.clearIO();
        try {
            final TransformerConfiguration transformerConfiguration = (transformerClassName == null) ? null : new TransformerConfiguration(transformerClassName);
            final BridgeConfiguration config = new BridgeConfiguration().setName(name).setQueueName(queueName).setForwardingAddress(forwardingAddress).setFilterString(filterString).setTransformerConfiguration(transformerConfiguration).setClientFailureCheckPeriod(clientFailureCheckPeriod).setRetryInterval(retryInterval).setRetryIntervalMultiplier(retryIntervalMultiplier).setInitialConnectAttempts(initialConnectAttempts).setReconnectAttempts(reconnectAttempts).setUseDuplicateDetection(useDuplicateDetection).setConfirmationWindowSize(confirmationWindowSize).setHA(ha).setUser(user).setPassword(password);
            if (useDiscoveryGroup) {
                config.setDiscoveryGroupName(staticConnectorsOrDiscoveryGroup);
            }
            else {
                config.setStaticConnectors(ListUtil.toList(staticConnectorsOrDiscoveryGroup));
            }
            this.server.deployBridge(config);
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public void destroyBridge(final String name) throws Exception {
        if (AuditLogger.isEnabled()) {
            AuditLogger.destroyBridge((Object)this.server, new Object[] { name });
        }
        this.checkStarted();
        this.clearIO();
        try {
            this.server.destroyBridge(name);
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public void createConnectorService(final String name, final String factoryClass, final Map<String, Object> parameters) throws Exception {
        if (AuditLogger.isEnabled()) {
            AuditLogger.createConnectorService((Object)this.server, new Object[] { name, factoryClass, parameters });
        }
        this.checkStarted();
        this.clearIO();
        try {
            final ConnectorServiceConfiguration config = new ConnectorServiceConfiguration().setName(name).setFactoryClassName(factoryClass).setParams(parameters);
            final ConnectorServiceFactory factory = this.server.getServiceRegistry().getConnectorService(config);
            this.server.getConnectorsService().createService(config, factory);
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public void destroyConnectorService(final String name) throws Exception {
        if (AuditLogger.isEnabled()) {
            AuditLogger.destroyConnectorService((Object)this.server, new Object[] { name });
        }
        this.checkStarted();
        this.clearIO();
        try {
            this.server.getConnectorsService().destroyService(name);
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public String[] getConnectorServices() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.getConnectorServices((Object)this.server, new Object[0]);
        }
        this.checkStarted();
        this.clearIO();
        try {
            return this.server.getConnectorsService().getConnectors().keySet().toArray(new String[0]);
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public void forceFailover() throws Exception {
        if (AuditLogger.isEnabled()) {
            AuditLogger.forceFailover((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        final Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    ActiveMQServerControlImpl.this.server.stop(true, true);
                }
                catch (Throwable e) {
                    ActiveMQServerControlImpl.logger.warn((Object)e.getMessage(), e);
                }
            }
        };
        t.start();
    }
    
    public void updateDuplicateIdCache(final String address, final Object[] ids) throws Exception {
        this.clearIO();
        try {
            final DuplicateIDCache duplicateIDCache = this.server.getPostOffice().getDuplicateIDCache(new SimpleString(address));
            for (final Object id : ids) {
                duplicateIDCache.addToCache(((String)id).getBytes(), null);
            }
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public void scaleDown(final String connector) throws Exception {
        if (AuditLogger.isEnabled()) {
            AuditLogger.scaleDown((Object)this.server, new Object[] { connector });
        }
        this.checkStarted();
        this.clearIO();
        final HAPolicy haPolicy = this.server.getHAPolicy();
        if (haPolicy instanceof LiveOnlyPolicy) {
            final LiveOnlyPolicy liveOnlyPolicy = (LiveOnlyPolicy)haPolicy;
            if (liveOnlyPolicy.getScaleDownPolicy() == null) {
                liveOnlyPolicy.setScaleDownPolicy(new ScaleDownPolicy());
            }
            liveOnlyPolicy.getScaleDownPolicy().setEnabled(true);
            if (connector != null) {
                liveOnlyPolicy.getScaleDownPolicy().getConnectors().add(0, connector);
            }
            this.server.fail(true);
        }
    }
    
    public String listNetworkTopology() throws Exception {
        if (AuditLogger.isEnabled()) {
            AuditLogger.listNetworkTopology((Object)this.server);
        }
        this.checkStarted();
        this.clearIO();
        try {
            final JsonArrayBuilder brokers = JsonLoader.createArrayBuilder();
            final ClusterManager clusterManager = this.server.getClusterManager();
            if (clusterManager != null) {
                final Set<ClusterConnection> clusterConnections = clusterManager.getClusterConnections();
                for (final ClusterConnection clusterConnection : clusterConnections) {
                    final Topology topology = clusterConnection.getTopology();
                    final Collection<TopologyMemberImpl> members = (Collection<TopologyMemberImpl>)topology.getMembers();
                    for (final TopologyMemberImpl member : members) {
                        final JsonObjectBuilder obj = JsonLoader.createObjectBuilder();
                        final TransportConfiguration live = member.getLive();
                        if (live != null) {
                            obj.add("nodeID", member.getNodeId()).add("live", live.getParams().get("host") + ":" + live.getParams().get("port"));
                            final TransportConfiguration backup = member.getBackup();
                            if (backup != null) {
                                obj.add("backup", backup.getParams().get("host") + ":" + backup.getParams().get("port"));
                            }
                        }
                        brokers.add(obj);
                    }
                }
            }
            return brokers.build().toString();
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public void removeNotificationListener(final javax.management.NotificationListener listener, final NotificationFilter filter, final Object handback) throws ListenerNotFoundException {
        if (AuditLogger.isEnabled()) {
            AuditLogger.removeNotificationListener((Object)this.server, new Object[] { listener, filter, handback });
        }
        this.clearIO();
        try {
            this.broadcaster.removeNotificationListener(listener, filter, handback);
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public void removeNotificationListener(final javax.management.NotificationListener listener) throws ListenerNotFoundException {
        if (AuditLogger.isEnabled()) {
            AuditLogger.removeNotificationListener((Object)this.server, new Object[] { listener });
        }
        this.clearIO();
        try {
            this.broadcaster.removeNotificationListener(listener);
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public void addNotificationListener(final javax.management.NotificationListener listener, final NotificationFilter filter, final Object handback) throws IllegalArgumentException {
        if (AuditLogger.isEnabled()) {
            AuditLogger.addNotificationListener((Object)this.server, new Object[] { listener, filter, handback });
        }
        this.clearIO();
        try {
            this.broadcaster.addNotificationListener(listener, filter, handback);
        }
        finally {
            this.blockOnIO();
        }
    }
    
    public MBeanNotificationInfo[] getNotificationInfo() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.getNotificationInfo((Object)this.server);
        }
        final CoreNotificationType[] values = CoreNotificationType.values();
        final String[] names = new String[values.length];
        for (int i = 0; i < values.length; ++i) {
            names[i] = values[i].toString();
        }
        return new MBeanNotificationInfo[] { new MBeanNotificationInfo(names, this.getClass().getName(), "Notifications emitted by a Core Server") };
    }
    
    private synchronized void setMessageCounterEnabled(final boolean enable) {
        if (this.isStarted()) {
            if (this.configuration.isMessageCounterEnabled() && !enable) {
                this.stopMessageCounters();
            }
            else if (!this.configuration.isMessageCounterEnabled() && enable) {
                this.startMessageCounters();
            }
        }
        this.configuration.setMessageCounterEnabled(enable);
    }
    
    private void startMessageCounters() {
        this.messageCounterManager.start();
    }
    
    private void stopMessageCounters() {
        this.messageCounterManager.stop();
        this.messageCounterManager.resetAllCounters();
        this.messageCounterManager.resetAllCounterHistories();
    }
    
    public long getConnectionTTLOverride() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.getConnectionTTLOverride((Object)this.server);
        }
        return this.configuration.getConnectionTTLOverride();
    }
    
    public int getIDCacheSize() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.getIDCacheSize((Object)this.server);
        }
        return this.configuration.getIDCacheSize();
    }
    
    public String getLargeMessagesDirectory() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.getLargeMessagesDirectory((Object)this.server);
        }
        return this.configuration.getLargeMessagesDirectory();
    }
    
    public String getManagementAddress() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.getManagementAddress((Object)this.server);
        }
        return this.configuration.getManagementAddress().toString();
    }
    
    public String getNodeID() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.getNodeID((Object)this.server);
        }
        return this.server.getNodeID().toString();
    }
    
    public String getManagementNotificationAddress() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.getManagementNotificationAddress((Object)this.server);
        }
        return this.configuration.getManagementNotificationAddress().toString();
    }
    
    public long getMessageExpiryScanPeriod() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.getMessageExpiryScanPeriod((Object)this.server);
        }
        return this.configuration.getMessageExpiryScanPeriod();
    }
    
    public long getMessageExpiryThreadPriority() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.getMessageExpiryThreadPriority((Object)this.server);
        }
        return this.configuration.getMessageExpiryThreadPriority();
    }
    
    public long getTransactionTimeout() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.getTransactionTimeout((Object)this.server);
        }
        return this.configuration.getTransactionTimeout();
    }
    
    public long getTransactionTimeoutScanPeriod() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.getTransactionTimeoutScanPeriod((Object)this.server);
        }
        return this.configuration.getTransactionTimeoutScanPeriod();
    }
    
    public boolean isPersistDeliveryCountBeforeDelivery() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.isPersistDeliveryCountBeforeDelivery((Object)this.server);
        }
        return this.configuration.isPersistDeliveryCountBeforeDelivery();
    }
    
    public boolean isPersistIDCache() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.isPersistIDCache((Object)this.server);
        }
        return this.configuration.isPersistIDCache();
    }
    
    public boolean isWildcardRoutingEnabled() {
        if (AuditLogger.isEnabled()) {
            AuditLogger.isWildcardRoutingEnabled((Object)this.server);
        }
        return this.configuration.isWildcardRoutingEnabled();
    }
    
    @Override
    protected MBeanOperationInfo[] fillMBeanOperationInfo() {
        return MBeanInfoHelper.getMBeanOperationsInfo(ActiveMQServerControl.class);
    }
    
    @Override
    protected MBeanAttributeInfo[] fillMBeanAttributeInfo() {
        return MBeanInfoHelper.getMBeanAttributesInfo(ActiveMQServerControl.class);
    }
    
    private void checkStarted() {
        if (!this.server.isStarted()) {
            throw new IllegalStateException("Broker is not started. It can not be managed yet");
        }
    }
    
    public String[] listTargetAddresses(final String sessionID) {
        final ServerSession session = this.server.getSessionByID(sessionID);
        if (session != null) {
            return session.getTargetAddresses();
        }
        return new String[0];
    }
    
    public void onNotification(final Notification notification) {
        if (!(notification.getType() instanceof CoreNotificationType)) {
            return;
        }
        final CoreNotificationType type = (CoreNotificationType)notification.getType();
        if (type == CoreNotificationType.SESSION_CREATED) {
            final TypedProperties props = notification.getProperties();
            if (props.getIntProperty(ManagementHelper.HDR_DISTANCE) > 0) {
                return;
            }
        }
        this.broadcaster.sendNotification(new javax.management.Notification(type.toString(), this, this.notifSeq.incrementAndGet(), notification.toString()));
    }
    
    public void addUser(final String username, final String password, final String roles, final boolean plaintext) throws Exception {
        if (AuditLogger.isEnabled()) {
            AuditLogger.addUser((Object)this.server, new Object[] { username, "****", roles, plaintext });
        }
        this.tcclInvoke(ActiveMQServerControlImpl.class.getClassLoader(), () -> this.internalAddUser(username, password, roles, plaintext));
    }
    
    private void internalAddUser(final String username, final String password, final String roles, final boolean plaintext) throws Exception {
        final PropertiesLoginModuleConfigurator config = this.getPropertiesLoginModuleConfigurator();
        config.addNewUser(username, plaintext ? password : PasswordMaskingUtil.getHashProcessor().hash(password), roles.split(","));
        config.save();
    }
    
    private String getSecurityDomain() {
        return this.server.getSecurityManager().getDomain();
    }
    
    public String listUser(final String username) throws Exception {
        if (AuditLogger.isEnabled()) {
            AuditLogger.listUser((Object)this.server, new Object[] { username });
        }
        return (String)this.tcclCall(ActiveMQServerControlImpl.class.getClassLoader(), () -> this.internaListUser(username));
    }
    
    private String internaListUser(final String username) throws Exception {
        final PropertiesLoginModuleConfigurator config = this.getPropertiesLoginModuleConfigurator();
        final Map<String, Set<String>> info = config.listUser(username);
        final JsonArrayBuilder users = JsonLoader.createArrayBuilder();
        for (final Map.Entry<String, Set<String>> entry : info.entrySet()) {
            final JsonObjectBuilder user = JsonLoader.createObjectBuilder();
            user.add("username", (String)entry.getKey());
            final JsonArrayBuilder roles = JsonLoader.createArrayBuilder();
            for (final String role : entry.getValue()) {
                roles.add(role);
            }
            user.add("roles", roles);
            users.add(user);
        }
        return users.build().toString();
    }
    
    public void removeUser(final String username) throws Exception {
        if (AuditLogger.isEnabled()) {
            AuditLogger.removeUser((Object)this.server, new Object[] { username });
        }
        this.tcclInvoke(ActiveMQServerControlImpl.class.getClassLoader(), () -> this.internalRemoveUser(username));
    }
    
    private void internalRemoveUser(final String username) throws Exception {
        final PropertiesLoginModuleConfigurator config = this.getPropertiesLoginModuleConfigurator();
        config.removeUser(username);
        config.save();
    }
    
    public void resetUser(final String username, final String password, final String roles) throws Exception {
        if (AuditLogger.isEnabled()) {
            AuditLogger.resetUser((Object)this.server, new Object[] { username, "****", roles });
        }
        this.tcclInvoke(ActiveMQServerControlImpl.class.getClassLoader(), () -> this.internalresetUser(username, password, roles));
    }
    
    private void internalresetUser(final String username, final String password, final String roles) throws Exception {
        final PropertiesLoginModuleConfigurator config = this.getPropertiesLoginModuleConfigurator();
        config.updateUser(username, password, (String[])((roles == null) ? null : roles.split(",")));
        config.save();
    }
    
    private PropertiesLoginModuleConfigurator getPropertiesLoginModuleConfigurator() throws Exception {
        final URL configurationUrl = this.server.getConfiguration().getConfigurationUrl();
        if (configurationUrl == null) {
            throw ActiveMQMessageBundle.BUNDLE.failedToLocateConfigURL();
        }
        final String path = configurationUrl.getPath();
        return new PropertiesLoginModuleConfigurator(this.getSecurityDomain(), path.substring(0, path.lastIndexOf("/")));
    }
    
    static {
        logger = Logger.getLogger((Class)ActiveMQServerControlImpl.class);
    }
}
