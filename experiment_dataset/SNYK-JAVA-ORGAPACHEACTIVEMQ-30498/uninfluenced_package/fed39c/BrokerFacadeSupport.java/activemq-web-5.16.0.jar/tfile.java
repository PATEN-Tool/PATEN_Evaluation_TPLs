// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.activemq.web;

import javax.management.openmbean.TabularData;
import javax.management.openmbean.CompositeData;
import org.apache.activemq.broker.jmx.JobSchedulerViewMBean;
import org.apache.activemq.broker.jmx.ProducerViewMBean;
import org.apache.activemq.broker.jmx.NetworkBridgeViewMBean;
import org.apache.activemq.broker.jmx.NetworkConnectorViewMBean;
import org.apache.activemq.broker.jmx.ConnectorViewMBean;
import org.apache.activemq.broker.jmx.ConnectionViewMBean;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.activemq.web.util.ExceptionUtils;
import javax.management.InstanceNotFoundException;
import org.apache.activemq.broker.jmx.DestinationViewMBean;
import org.apache.activemq.broker.jmx.DurableSubscriptionViewMBean;
import org.springframework.util.StringUtils;
import org.apache.activemq.broker.jmx.SubscriptionViewMBean;
import org.apache.activemq.broker.jmx.TopicViewMBean;
import org.apache.activemq.broker.jmx.BrokerViewMBean;
import java.util.Collections;
import org.apache.activemq.broker.jmx.QueueViewMBean;
import java.util.Collection;
import java.util.Set;
import javax.management.QueryExp;
import javax.management.ObjectName;
import org.apache.activemq.broker.jmx.ManagementContext;

public abstract class BrokerFacadeSupport implements BrokerFacade
{
    public abstract ManagementContext getManagementContext();
    
    public abstract Set queryNames(final ObjectName p0, final QueryExp p1) throws Exception;
    
    public abstract Object newProxyInstance(final ObjectName p0, final Class p1, final boolean p2) throws Exception;
    
    @Override
    public Collection<QueueViewMBean> getQueues() throws Exception {
        final BrokerViewMBean broker = this.getBrokerAdmin();
        if (broker == null) {
            return (Collection<QueueViewMBean>)Collections.EMPTY_LIST;
        }
        final ObjectName[] queues = broker.getQueues();
        return this.getManagedObjects(queues, QueueViewMBean.class);
    }
    
    @Override
    public Collection<TopicViewMBean> getTopics() throws Exception {
        final BrokerViewMBean broker = this.getBrokerAdmin();
        if (broker == null) {
            return (Collection<TopicViewMBean>)Collections.EMPTY_LIST;
        }
        final ObjectName[] topics = broker.getTopics();
        return this.getManagedObjects(topics, TopicViewMBean.class);
    }
    
    @Override
    public Collection<SubscriptionViewMBean> getTopicSubscribers(String topicName) throws Exception {
        final String brokerName = this.getBrokerName();
        topicName = StringUtils.replace(topicName, "\"", "_");
        final ObjectName query = new ObjectName("org.apache.activemq:type=Broker,brokerName=" + brokerName + ",destinationType=Topic,destinationName=" + topicName + ",endpoint=Consumer,*");
        final Set<ObjectName> queryResult = (Set<ObjectName>)this.queryNames(query, null);
        return this.getManagedObjects(queryResult.toArray(new ObjectName[queryResult.size()]), SubscriptionViewMBean.class);
    }
    
    @Override
    public Collection<SubscriptionViewMBean> getNonDurableTopicSubscribers() throws Exception {
        final BrokerViewMBean broker = this.getBrokerAdmin();
        if (broker == null) {
            return (Collection<SubscriptionViewMBean>)Collections.EMPTY_LIST;
        }
        final ObjectName[] subscribers = broker.getTopicSubscribers();
        return this.getManagedObjects(subscribers, SubscriptionViewMBean.class);
    }
    
    @Override
    public Collection<DurableSubscriptionViewMBean> getDurableTopicSubscribers() throws Exception {
        final BrokerViewMBean broker = this.getBrokerAdmin();
        if (broker == null) {
            return (Collection<DurableSubscriptionViewMBean>)Collections.EMPTY_LIST;
        }
        final ObjectName[] subscribers = broker.getDurableTopicSubscribers();
        return this.getManagedObjects(subscribers, DurableSubscriptionViewMBean.class);
    }
    
    @Override
    public Collection<DurableSubscriptionViewMBean> getInactiveDurableTopicSubscribers() throws Exception {
        final BrokerViewMBean broker = this.getBrokerAdmin();
        if (broker == null) {
            return (Collection<DurableSubscriptionViewMBean>)Collections.EMPTY_LIST;
        }
        final ObjectName[] subscribers = broker.getInactiveDurableTopicSubscribers();
        return this.getManagedObjects(subscribers, DurableSubscriptionViewMBean.class);
    }
    
    @Override
    public QueueViewMBean getQueue(final String name) throws Exception {
        return (QueueViewMBean)this.getDestinationByName((Collection<? extends DestinationViewMBean>)this.getQueues(), name);
    }
    
    @Override
    public TopicViewMBean getTopic(final String name) throws Exception {
        return (TopicViewMBean)this.getDestinationByName((Collection<? extends DestinationViewMBean>)this.getTopics(), name);
    }
    
    protected DestinationViewMBean getDestinationByName(final Collection<? extends DestinationViewMBean> collection, final String name) {
        final Iterator<? extends DestinationViewMBean> iter = collection.iterator();
        while (iter.hasNext()) {
            try {
                final DestinationViewMBean destinationViewMBean = (DestinationViewMBean)iter.next();
                if (name.equals(destinationViewMBean.getName())) {
                    return destinationViewMBean;
                }
                continue;
            }
            catch (Exception ex) {
                if (!ExceptionUtils.isRootCause(ex, InstanceNotFoundException.class)) {
                    throw ex;
                }
                continue;
            }
        }
        return null;
    }
    
    protected <T> Collection<T> getManagedObjects(final ObjectName[] names, final Class<T> type) throws Exception {
        final List<T> answer = new ArrayList<T>();
        for (int i = 0; i < names.length; ++i) {
            final ObjectName name = names[i];
            final T value = (T)this.newProxyInstance(name, type, true);
            if (value != null) {
                answer.add(value);
            }
        }
        return answer;
    }
    
    @Override
    public Collection<ConnectionViewMBean> getConnections() throws Exception {
        final String brokerName = this.getBrokerName();
        final ObjectName query = new ObjectName("org.apache.activemq:type=Broker,brokerName=" + brokerName + ",connector=clientConnectors,connectorName=*,connectionName=*");
        final Set<ObjectName> queryResult = (Set<ObjectName>)this.queryNames(query, null);
        return this.getManagedObjects(queryResult.toArray(new ObjectName[queryResult.size()]), ConnectionViewMBean.class);
    }
    
    @Override
    public Collection<String> getConnections(final String connectorName) throws Exception {
        final String brokerName = this.getBrokerName();
        final ObjectName query = new ObjectName("org.apache.activemq:type=Broker,brokerName=" + brokerName + ",connector=clientConnectors,connectorName=" + connectorName + ",connectionViewType=clientId,connectionName=*");
        final Set<ObjectName> queryResult = (Set<ObjectName>)this.queryNames(query, null);
        final Collection<String> result = new ArrayList<String>(queryResult.size());
        for (final ObjectName on : queryResult) {
            final String name = StringUtils.replace(on.getKeyProperty("connectionName"), "_", ":");
            result.add(name);
        }
        return result;
    }
    
    @Override
    public ConnectionViewMBean getConnection(String connectionName) throws Exception {
        connectionName = StringUtils.replace(connectionName, ":", "_");
        final String brokerName = this.getBrokerName();
        final ObjectName query = new ObjectName("org.apache.activemq:type=Broker,brokerName=" + brokerName + ",connector=clientConnectors,*,connectionName=" + connectionName);
        final Set<ObjectName> queryResult = (Set<ObjectName>)this.queryNames(query, null);
        if (queryResult.size() == 0) {
            return null;
        }
        final ObjectName objectName = queryResult.iterator().next();
        return (ConnectionViewMBean)this.newProxyInstance(objectName, ConnectionViewMBean.class, true);
    }
    
    @Override
    public Collection<String> getConnectors() throws Exception {
        final String brokerName = this.getBrokerName();
        final ObjectName query = new ObjectName("org.apache.activemq:type=Broker,brokerName=" + brokerName + ",connector=clientConnectors,connectorName=*");
        final Set<ObjectName> queryResult = (Set<ObjectName>)this.queryNames(query, null);
        final Collection<String> result = new ArrayList<String>(queryResult.size());
        for (final ObjectName on : queryResult) {
            result.add(on.getKeyProperty("connectorName"));
        }
        return result;
    }
    
    @Override
    public ConnectorViewMBean getConnector(final String name) throws Exception {
        final String brokerName = this.getBrokerName();
        final ObjectName objectName = new ObjectName("org.apache.activemq:type=Broker,brokerName=" + brokerName + ",connector=clientConnectors,connectorName=" + name);
        return (ConnectorViewMBean)this.newProxyInstance(objectName, ConnectorViewMBean.class, true);
    }
    
    @Override
    public Collection<NetworkConnectorViewMBean> getNetworkConnectors() throws Exception {
        final String brokerName = this.getBrokerName();
        final ObjectName query = new ObjectName("org.apache.activemq:type=Broker,brokerName=" + brokerName + ",connector=networkConnectors,networkConnectorName=*");
        final Set<ObjectName> queryResult = (Set<ObjectName>)this.queryNames(query, null);
        return this.getManagedObjects(queryResult.toArray(new ObjectName[queryResult.size()]), NetworkConnectorViewMBean.class);
    }
    
    @Override
    public Collection<NetworkBridgeViewMBean> getNetworkBridges() throws Exception {
        final String brokerName = this.getBrokerName();
        final ObjectName query = new ObjectName("org.apache.activemq:type=Broker,brokerName=" + brokerName + ",connector=*,networkConnectorName=*,networkBridge=*");
        final Set<ObjectName> queryResult = (Set<ObjectName>)this.queryNames(query, null);
        return this.getManagedObjects(queryResult.toArray(new ObjectName[queryResult.size()]), NetworkBridgeViewMBean.class);
    }
    
    @Override
    public Collection<SubscriptionViewMBean> getQueueConsumers(String queueName) throws Exception {
        final String brokerName = this.getBrokerName();
        queueName = StringUtils.replace(queueName, "\"", "_");
        final ObjectName query = new ObjectName("org.apache.activemq:type=Broker,brokerName=" + brokerName + ",destinationType=Queue,destinationName=" + queueName + ",endpoint=Consumer,*");
        final Set<ObjectName> queryResult = (Set<ObjectName>)this.queryNames(query, null);
        return this.getManagedObjects(queryResult.toArray(new ObjectName[queryResult.size()]), SubscriptionViewMBean.class);
    }
    
    @Override
    public Collection<ProducerViewMBean> getQueueProducers(String queueName) throws Exception {
        final String brokerName = this.getBrokerName();
        queueName = StringUtils.replace(queueName, "\"", "_");
        final ObjectName query = new ObjectName("org.apache.activemq:type=Broker,brokerName=" + brokerName + ",destinationType=Queue,destinationName=" + queueName + ",endpoint=Producer,*");
        final Set<ObjectName> queryResult = (Set<ObjectName>)this.queryNames(query, null);
        return this.getManagedObjects(queryResult.toArray(new ObjectName[queryResult.size()]), ProducerViewMBean.class);
    }
    
    @Override
    public Collection<ProducerViewMBean> getTopicProducers(String topicName) throws Exception {
        final String brokerName = this.getBrokerName();
        topicName = StringUtils.replace(topicName, "\"", "_");
        final ObjectName query = new ObjectName("org.apache.activemq:type=Broker,brokerName=" + brokerName + ",destinationType=Topic,destinationName=" + topicName + ",endpoint=Producer,*");
        final Set<ObjectName> queryResult = (Set<ObjectName>)this.queryNames(query, null);
        return this.getManagedObjects(queryResult.toArray(new ObjectName[queryResult.size()]), ProducerViewMBean.class);
    }
    
    @Override
    public Collection<SubscriptionViewMBean> getConsumersOnConnection(String connectionName) throws Exception {
        connectionName = StringUtils.replace(connectionName, ":", "_");
        final String brokerName = this.getBrokerName();
        final ObjectName query = new ObjectName("org.apache.activemq:type=Broker,brokerName=" + brokerName + ",*,endpoint=Consumer,clientId=" + connectionName);
        final Set<ObjectName> queryResult = (Set<ObjectName>)this.queryNames(query, null);
        return this.getManagedObjects(queryResult.toArray(new ObjectName[queryResult.size()]), SubscriptionViewMBean.class);
    }
    
    @Override
    public JobSchedulerViewMBean getJobScheduler() throws Exception {
        final ObjectName name = this.getBrokerAdmin().getJMSJobScheduler();
        return (JobSchedulerViewMBean)this.newProxyInstance(name, JobSchedulerViewMBean.class, true);
    }
    
    @Override
    public Collection<JobFacade> getScheduledJobs() throws Exception {
        final JobSchedulerViewMBean jobScheduler = this.getJobScheduler();
        final List<JobFacade> result = new ArrayList<JobFacade>();
        final TabularData table = jobScheduler.getAllJobs();
        for (final Object object : table.values()) {
            final CompositeData cd = (CompositeData)object;
            final JobFacade jf = new JobFacade(cd);
            result.add(jf);
        }
        return result;
    }
    
    @Override
    public boolean isJobSchedulerStarted() {
        try {
            final JobSchedulerViewMBean jobScheduler = this.getJobScheduler();
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }
}
