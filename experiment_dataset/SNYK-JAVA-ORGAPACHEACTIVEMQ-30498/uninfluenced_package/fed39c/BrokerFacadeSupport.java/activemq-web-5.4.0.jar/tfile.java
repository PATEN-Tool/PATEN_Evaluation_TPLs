// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.activemq.web;

import javax.management.openmbean.TabularData;
import javax.management.openmbean.CompositeData;
import org.apache.activemq.broker.jmx.JobSchedulerViewMBean;
import org.apache.activemq.broker.jmx.SubscriptionViewMBean;
import org.apache.activemq.broker.jmx.NetworkConnectorViewMBean;
import org.apache.activemq.broker.jmx.ConnectorViewMBean;
import org.springframework.util.StringUtils;
import org.apache.activemq.broker.jmx.ConnectionViewMBean;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.activemq.broker.jmx.DestinationViewMBean;
import org.apache.activemq.broker.jmx.DurableSubscriptionViewMBean;
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
    
    public Collection<QueueViewMBean> getQueues() throws Exception {
        final BrokerViewMBean broker = this.getBrokerAdmin();
        if (broker == null) {
            return (Collection<QueueViewMBean>)Collections.EMPTY_LIST;
        }
        final ObjectName[] queues = broker.getQueues();
        return this.getManagedObjects(queues, QueueViewMBean.class);
    }
    
    public Collection<TopicViewMBean> getTopics() throws Exception {
        final BrokerViewMBean broker = this.getBrokerAdmin();
        if (broker == null) {
            return (Collection<TopicViewMBean>)Collections.EMPTY_LIST;
        }
        final ObjectName[] queues = broker.getTopics();
        return this.getManagedObjects(queues, TopicViewMBean.class);
    }
    
    public Collection<DurableSubscriptionViewMBean> getDurableTopicSubscribers() throws Exception {
        final BrokerViewMBean broker = this.getBrokerAdmin();
        if (broker == null) {
            return (Collection<DurableSubscriptionViewMBean>)Collections.EMPTY_LIST;
        }
        final ObjectName[] queues = broker.getDurableTopicSubscribers();
        return this.getManagedObjects(queues, DurableSubscriptionViewMBean.class);
    }
    
    public QueueViewMBean getQueue(final String name) throws Exception {
        return (QueueViewMBean)this.getDestinationByName((Collection<? extends DestinationViewMBean>)this.getQueues(), name);
    }
    
    public TopicViewMBean getTopic(final String name) throws Exception {
        return (TopicViewMBean)this.getDestinationByName((Collection<? extends DestinationViewMBean>)this.getTopics(), name);
    }
    
    protected DestinationViewMBean getDestinationByName(final Collection<? extends DestinationViewMBean> collection, final String name) {
        for (final DestinationViewMBean destinationViewMBean : collection) {
            if (name.equals(destinationViewMBean.getName())) {
                return destinationViewMBean;
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
    
    public Collection<ConnectionViewMBean> getConnections() throws Exception {
        final String brokerName = this.getBrokerName();
        final ObjectName query = new ObjectName("org.apache.activemq:BrokerName=" + brokerName + ",Type=Connection,*");
        final Set<ObjectName> queryResult = (Set<ObjectName>)this.queryNames(query, null);
        return this.getManagedObjects(queryResult.toArray(new ObjectName[queryResult.size()]), ConnectionViewMBean.class);
    }
    
    public Collection<String> getConnections(final String connectorName) throws Exception {
        final String brokerName = this.getBrokerName();
        final ObjectName query = new ObjectName("org.apache.activemq:BrokerName=" + brokerName + ",Type=Connection,ConnectorName=" + connectorName + ",*");
        final Set<ObjectName> queryResult = (Set<ObjectName>)this.queryNames(query, null);
        final Collection<String> result = new ArrayList<String>(queryResult.size());
        for (final ObjectName on : queryResult) {
            final String name = StringUtils.replace(on.getKeyProperty("Connection"), "_", ":");
            result.add(name);
        }
        return result;
    }
    
    public ConnectionViewMBean getConnection(String connectionName) throws Exception {
        connectionName = StringUtils.replace(connectionName, ":", "_");
        final String brokerName = this.getBrokerName();
        final ObjectName query = new ObjectName("org.apache.activemq:BrokerName=" + brokerName + ",Type=Connection,*,Connection=" + connectionName);
        final Set<ObjectName> queryResult = (Set<ObjectName>)this.queryNames(query, null);
        if (queryResult.size() == 0) {
            return null;
        }
        final ObjectName objectName = queryResult.iterator().next();
        return (ConnectionViewMBean)this.newProxyInstance(objectName, ConnectionViewMBean.class, true);
    }
    
    public Collection<String> getConnectors() throws Exception {
        final String brokerName = this.getBrokerName();
        final ObjectName query = new ObjectName("org.apache.activemq:BrokerName=" + brokerName + ",Type=Connector,*");
        final Set<ObjectName> queryResult = (Set<ObjectName>)this.queryNames(query, null);
        final Collection<String> result = new ArrayList<String>(queryResult.size());
        for (final ObjectName on : queryResult) {
            result.add(on.getKeyProperty("ConnectorName"));
        }
        return result;
    }
    
    public ConnectorViewMBean getConnector(final String name) throws Exception {
        final String brokerName = this.getBrokerName();
        final ObjectName objectName = new ObjectName("org.apache.activemq:BrokerName=" + brokerName + ",Type=Connector,ConnectorName=" + name);
        return (ConnectorViewMBean)this.newProxyInstance(objectName, ConnectorViewMBean.class, true);
    }
    
    public Collection<NetworkConnectorViewMBean> getNetworkConnectors() throws Exception {
        final String brokerName = this.getBrokerName();
        final ObjectName query = new ObjectName("org.apache.activemq:BrokerName=" + brokerName + ",Type=NetworkConnector,*");
        final Set<ObjectName> queryResult = (Set<ObjectName>)this.queryNames(query, null);
        return this.getManagedObjects(queryResult.toArray(new ObjectName[queryResult.size()]), NetworkConnectorViewMBean.class);
    }
    
    public Collection<SubscriptionViewMBean> getQueueConsumers(String queueName) throws Exception {
        final String brokerName = this.getBrokerName();
        queueName = StringUtils.replace(queueName, "\"", "_");
        final ObjectName query = new ObjectName("org.apache.activemq:BrokerName=" + brokerName + ",Type=Subscription,destinationType=Queue,destinationName=" + queueName + ",*");
        final Set<ObjectName> queryResult = (Set<ObjectName>)this.queryNames(query, null);
        return this.getManagedObjects(queryResult.toArray(new ObjectName[queryResult.size()]), SubscriptionViewMBean.class);
    }
    
    public Collection<SubscriptionViewMBean> getConsumersOnConnection(String connectionName) throws Exception {
        connectionName = StringUtils.replace(connectionName, ":", "_");
        final String brokerName = this.getBrokerName();
        final ObjectName query = new ObjectName("org.apache.activemq:BrokerName=" + brokerName + ",Type=Subscription,clientId=" + connectionName + ",*");
        final Set<ObjectName> queryResult = (Set<ObjectName>)this.queryNames(query, null);
        return this.getManagedObjects(queryResult.toArray(new ObjectName[queryResult.size()]), SubscriptionViewMBean.class);
    }
    
    public JobSchedulerViewMBean getJobScheduler() throws Exception {
        final ObjectName name = this.getBrokerAdmin().getJMSJobScheduler();
        return (JobSchedulerViewMBean)this.newProxyInstance(name, JobSchedulerViewMBean.class, true);
    }
    
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
}
