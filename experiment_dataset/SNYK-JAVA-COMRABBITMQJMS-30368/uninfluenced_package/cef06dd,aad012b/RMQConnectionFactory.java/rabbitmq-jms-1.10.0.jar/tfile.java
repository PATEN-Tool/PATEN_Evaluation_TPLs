// 
// Decompiled by Procyon v0.5.36
// 

package com.rabbitmq.jms.admin;

import javax.jms.QueueConnection;
import javax.jms.TopicConnection;
import javax.naming.RefAddr;
import javax.naming.StringRefAddr;
import javax.naming.NamingException;
import javax.naming.Reference;
import com.rabbitmq.jms.util.UriCodec;
import java.security.NoSuchAlgorithmException;
import java.net.URISyntaxException;
import java.util.concurrent.TimeoutException;
import com.rabbitmq.jms.util.RMQJMSException;
import java.io.IOException;
import javax.net.ssl.SSLException;
import com.rabbitmq.jms.util.RMQJMSSecurityException;
import com.rabbitmq.jms.client.RMQConnection;
import com.rabbitmq.jms.client.ConnectionParams;
import java.util.Iterator;
import com.rabbitmq.client.Address;
import javax.jms.JMSException;
import javax.jms.Connection;
import java.util.ArrayList;
import com.rabbitmq.jms.util.WhiteListObjectInputStream;
import com.rabbitmq.client.NoOpMetricsCollector;
import org.slf4j.LoggerFactory;
import java.net.URI;
import java.util.List;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import com.rabbitmq.jms.client.AmqpConnectionFactoryPostProcessor;
import com.rabbitmq.client.MetricsCollector;
import com.rabbitmq.jms.client.AmqpPropertiesCustomiser;
import org.slf4j.Logger;
import javax.jms.TopicConnectionFactory;
import javax.jms.QueueConnectionFactory;
import java.io.Serializable;
import javax.naming.Referenceable;
import javax.jms.ConnectionFactory;

public class RMQConnectionFactory implements ConnectionFactory, Referenceable, Serializable, QueueConnectionFactory, TopicConnectionFactory
{
    private final Logger logger;
    private static final long serialVersionUID = -4953157213762979615L;
    private static final int DEFAULT_RABBITMQ_SSL_PORT = 5671;
    private static final int DEFAULT_RABBITMQ_PORT = 5672;
    private String username;
    private String password;
    private String virtualHost;
    private String host;
    private int port;
    private int onMessageTimeoutMs;
    private boolean preferProducerMessageProperty;
    private boolean requeueOnMessageListenerException;
    private boolean cleanUpServerNamedQueuesForNonDurableTopicsOnSessionClose;
    private AmqpPropertiesCustomiser amqpPropertiesCustomiser;
    private MetricsCollector metricsCollector;
    private AmqpConnectionFactoryPostProcessor amqpConnectionFactoryPostProcessor;
    private boolean throwExceptionOnConsumerStartFailure;
    private boolean ssl;
    private String tlsProtocol;
    private SSLContext sslContext;
    private boolean useDefaultSslContext;
    private boolean hostnameVerification;
    private HostnameVerifier hostnameVerifier;
    private int queueBrowserReadMax;
    private volatile long terminationTimeout;
    private int channelsQos;
    private List<String> trustedPackages;
    private List<URI> uris;
    
    public RMQConnectionFactory() {
        this.logger = LoggerFactory.getLogger((Class)RMQConnectionFactory.class);
        this.username = "guest";
        this.password = "guest";
        this.virtualHost = "/";
        this.host = "localhost";
        this.port = -1;
        this.onMessageTimeoutMs = 2000;
        this.preferProducerMessageProperty = true;
        this.requeueOnMessageListenerException = false;
        this.cleanUpServerNamedQueuesForNonDurableTopicsOnSessionClose = false;
        this.metricsCollector = (MetricsCollector)new NoOpMetricsCollector();
        this.amqpConnectionFactoryPostProcessor = new AmqpConnectionFactoryPostProcessor() {
            @Override
            public void postProcess(final com.rabbitmq.client.ConnectionFactory connectionFactory) {
            }
        };
        this.throwExceptionOnConsumerStartFailure = false;
        this.ssl = false;
        this.useDefaultSslContext = false;
        this.hostnameVerification = false;
        this.queueBrowserReadMax = Math.max(0, Integer.getInteger("rabbit.jms.queueBrowserReadMax", 0));
        this.terminationTimeout = Long.getLong("rabbit.jms.terminationTimeout", 15000L);
        this.channelsQos = -1;
        this.trustedPackages = WhiteListObjectInputStream.DEFAULT_TRUSTED_PACKAGES;
        this.uris = new ArrayList<URI>();
    }
    
    public Connection createConnection() throws JMSException {
        return this.createConnection(this.username, this.password);
    }
    
    public Connection createConnection(final List<Address> endpoints) throws JMSException {
        return this.createConnection(this.username, this.password, endpoints);
    }
    
    public Connection createConnection(final String username, final String password) throws JMSException {
        if (this.uris == null || this.uris.isEmpty()) {
            return this.createConnection(username, password, new ConnectionCreator() {
                @Override
                public com.rabbitmq.client.Connection create(final com.rabbitmq.client.ConnectionFactory cf) throws Exception {
                    return cf.newConnection();
                }
            });
        }
        final List<Address> addresses = new ArrayList<Address>(this.uris.size());
        for (final URI uri : this.uris) {
            final String host = uri.getHost();
            int port = uri.getPort();
            if (port == -1) {
                port = (this.isSsl() ? 5671 : 5672);
            }
            addresses.add(new Address(host, port));
        }
        return this.createConnection(username, password, new ConnectionCreator() {
            @Override
            public com.rabbitmq.client.Connection create(final com.rabbitmq.client.ConnectionFactory cf) throws Exception {
                return cf.newConnection(addresses);
            }
        });
    }
    
    public Connection createConnection(final String username, final String password, final List<Address> endpoints) throws JMSException {
        return this.createConnection(username, password, new ConnectionCreator() {
            @Override
            public com.rabbitmq.client.Connection create(final com.rabbitmq.client.ConnectionFactory cf) throws Exception {
                return cf.newConnection(endpoints);
            }
        });
    }
    
    protected Connection createConnection(final String username, final String password, final ConnectionCreator connectionCreator) throws JMSException {
        this.logger.trace("Creating a connection for username '{}', password 'xxxxxxxx'.", (Object)username);
        this.username = username;
        this.password = password;
        final com.rabbitmq.client.ConnectionFactory cf = this.createConnectionFactory();
        setRabbitUri(this.logger, this, cf, this.getUri());
        this.maybeEnableTLS(cf);
        this.maybeEnableHostnameVerification(cf);
        cf.setMetricsCollector(this.metricsCollector);
        if (this.amqpConnectionFactoryPostProcessor != null) {
            this.amqpConnectionFactoryPostProcessor.postProcess(cf);
        }
        final com.rabbitmq.client.Connection rabbitConnection = this.instantiateNodeConnection(cf, connectionCreator);
        final RMQConnection conn = new RMQConnection(new ConnectionParams().setRabbitConnection(rabbitConnection).setTerminationTimeout(this.getTerminationTimeout()).setQueueBrowserReadMax(this.getQueueBrowserReadMax()).setOnMessageTimeoutMs(this.getOnMessageTimeoutMs()).setChannelsQos(this.channelsQos).setPreferProducerMessageProperty(this.preferProducerMessageProperty).setRequeueOnMessageListenerException(this.requeueOnMessageListenerException).setCleanUpServerNamedQueuesForNonDurableTopicsOnSessionClose(this.cleanUpServerNamedQueuesForNonDurableTopicsOnSessionClose).setAmqpPropertiesCustomiser(this.amqpPropertiesCustomiser).setThrowExceptionOnConsumerStartFailure(this.throwExceptionOnConsumerStartFailure));
        conn.setTrustedPackages(this.trustedPackages);
        this.logger.debug("Connection {} created.", (Object)conn);
        return (Connection)conn;
    }
    
    protected com.rabbitmq.client.ConnectionFactory createConnectionFactory() {
        return new com.rabbitmq.client.ConnectionFactory();
    }
    
    private com.rabbitmq.client.Connection instantiateNodeConnection(final com.rabbitmq.client.ConnectionFactory cf, final ConnectionCreator connectionCreator) throws JMSException {
        try {
            return connectionCreator.create(cf);
        }
        catch (SSLException ssle) {
            throw new RMQJMSSecurityException("SSL Exception establishing RabbitMQ Connection", ssle);
        }
        catch (Exception x) {
            if (x instanceof IOException) {
                final IOException ioe = (IOException)x;
                final String msg = ioe.getMessage();
                if (msg != null) {
                    if (msg.contains("authentication failure") || msg.contains("refused using authentication")) {
                        throw new RMQJMSSecurityException(ioe);
                    }
                    if (msg.contains("Connection refused")) {
                        throw new RMQJMSException("RabbitMQ connection was refused. RabbitMQ broker may not be available.", ioe);
                    }
                }
                throw new RMQJMSException(ioe);
            }
            if (x instanceof TimeoutException) {
                final TimeoutException te = (TimeoutException)x;
                throw new RMQJMSException("Timed out establishing RabbitMQ Connection", te);
            }
            throw new RMQJMSException("Unexpected exception thrown by newConnection()", x);
        }
    }
    
    public String getUri() {
        final StringBuilder sb = new StringBuilder(scheme(this.isSsl())).append("://");
        sb.append(uriUInfoEscape(this.username, this.password)).append('@');
        sb.append(uriHostEscape(this.host)).append(':').append(this.getPort()).append("/");
        sb.append(uriVirtualHostEscape(this.virtualHost));
        return sb.toString();
    }
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("RMQConnectionFactory{");
        return (this.isSsl() ? sb.append("SSL, ") : sb).append("user='").append(this.username).append("', password").append((this.password != null) ? "=xxxxxxxx" : " not set").append(", host='").append(this.host).append("', port=").append(this.getPort()).append(", virtualHost='").append(this.virtualHost).append("', onMessageTimeoutMs=").append(this.onMessageTimeoutMs).append(", queueBrowserReadMax=").append(this.queueBrowserReadMax).append('}').toString();
    }
    
    public void setUri(final String uriString) throws JMSException {
        this.logger.trace("Set connection factory parameters by URI '{}'", (Object)uriString);
        if (uriString != null && !uriString.trim().isEmpty()) {
            final com.rabbitmq.client.ConnectionFactory factory = this.createConnectionFactory();
            setRabbitUri(this.logger, this, factory, uriString);
            this.host = factory.getHost();
            this.password = factory.getPassword();
            this.port = factory.getPort();
            this.ssl = factory.isSSL();
            this.username = factory.getUsername();
            this.virtualHost = factory.getVirtualHost();
        }
        else {
            this.host = null;
            this.password = null;
            this.port = -1;
            this.ssl = false;
            this.username = null;
            this.virtualHost = null;
        }
    }
    
    public void setUris(final List<String> urisAsStrings) throws JMSException {
        if (urisAsStrings != null && !urisAsStrings.isEmpty()) {
            final List<URI> uris = new ArrayList<URI>(urisAsStrings.size());
            for (final String uriAsString : urisAsStrings) {
                try {
                    final URI uri = new URI(uriAsString);
                    if (uri.getScheme() == null || (!"amqp".equals(uri.getScheme()) && !"amqps".equals(uri.getScheme()))) {
                        throw new IllegalArgumentException("Wrong scheme in AMQP URI: " + uri.getScheme());
                    }
                    uris.add(uri);
                }
                catch (URISyntaxException e) {
                    throw new IllegalArgumentException("Invalid URI: " + uriAsString);
                }
            }
            this.uris = uris;
            this.setUri(urisAsStrings.get(0));
        }
        else {
            this.uris = null;
            this.setUri(null);
        }
    }
    
    public void setTrustedPackages(final List<String> value) {
        this.trustedPackages = value;
    }
    
    public List<String> getTrustedPackages() {
        return this.trustedPackages;
    }
    
    private static void setRabbitUri(final Logger logger, final RMQConnectionFactory rmqFactory, final com.rabbitmq.client.ConnectionFactory factory, final String uriString) throws RMQJMSException {
        if (uriString != null) {
            try {
                factory.setUri(uriString);
            }
            catch (Exception e) {
                logger.error("Could not set URI on {}", (Object)rmqFactory, (Object)e);
                throw new RMQJMSException("Could not set URI on RabbitMQ connection factory.", e);
            }
        }
    }
    
    private void maybeEnableTLS(final com.rabbitmq.client.ConnectionFactory factory) {
        if (this.ssl) {
            try {
                if (this.useDefaultSslContext) {
                    factory.useSslProtocol(SSLContext.getDefault());
                }
                else if (this.sslContext != null) {
                    factory.useSslProtocol(this.sslContext);
                }
                else if (this.tlsProtocol != null) {
                    factory.useSslProtocol(this.tlsProtocol);
                }
                else {
                    factory.useSslProtocol();
                }
            }
            catch (Exception e) {
                this.logger.warn("Could not set SSL protocol on connection factory, {}. SSL set off.", (Object)this, (Object)e);
                this.ssl = false;
            }
        }
    }
    
    private void maybeEnableHostnameVerification(final com.rabbitmq.client.ConnectionFactory factory) {
        if (this.hostnameVerification || this.hostnameVerifier != null) {
            if (this.ssl) {
                if (this.hostnameVerifier == null) {
                    factory.enableHostnameVerification();
                }
                else {
                    factory.enableHostnameVerification(this.hostnameVerifier);
                }
            }
            else {
                this.logger.warn("Hostname verification enabled, but not TLS, please enable TLS too.");
            }
        }
    }
    
    public boolean isSsl() {
        return this.ssl;
    }
    
    @Deprecated
    public void setSsl(final boolean ssl) {
        this.ssl = ssl;
    }
    
    public void useSslProtocol() throws NoSuchAlgorithmException {
        this.useSslProtocol(com.rabbitmq.client.ConnectionFactory.computeDefaultTlsProcotol(SSLContext.getDefault().getSupportedSSLParameters().getProtocols()));
    }
    
    public void useSslProtocol(final String protocol) {
        this.tlsProtocol = protocol;
        this.ssl = true;
    }
    
    public void useSslProtocol(final SSLContext context) {
        this.sslContext = context;
        this.ssl = true;
    }
    
    public void useDefaultSslContext(final boolean useDefaultSslContext) {
        this.useDefaultSslContext = useDefaultSslContext;
        this.ssl = true;
    }
    
    public boolean isUseDefaultSslContext() {
        return this.useDefaultSslContext;
    }
    
    public void setUseDefaultSslContext(final boolean useDefaultSslContext) {
        this.useDefaultSslContext(useDefaultSslContext);
    }
    
    private static String scheme(final boolean isSsl) {
        return isSsl ? "amqps" : "amqp";
    }
    
    private static String uriUInfoEscape(final String user, final String pass) {
        if (null == user) {
            return null;
        }
        if (null == pass) {
            return UriCodec.encUserinfo(user, "UTF-8");
        }
        return UriCodec.encUserinfo(user + ":" + pass, "UTF-8");
    }
    
    private static String uriHostEscape(final String host) {
        return UriCodec.encHost(host, "UTF-8");
    }
    
    private static String uriVirtualHostEscape(final String vHost) {
        return UriCodec.encSegment(vHost, "UTF-8");
    }
    
    public Reference getReference() throws NamingException {
        final Reference ref = new Reference(RMQConnectionFactory.class.getName());
        addStringRefProperty(ref, "uri", this.getUri());
        addIntegerRefProperty(ref, "queueBrowserReadMax", this.getQueueBrowserReadMax());
        addIntegerRefProperty(ref, "onMessageTimeoutMs", this.getOnMessageTimeoutMs());
        return ref;
    }
    
    private static void addStringRefProperty(final Reference ref, final String propertyName, final String value) {
        if (value == null || propertyName == null) {
            return;
        }
        final RefAddr ra = new StringRefAddr(propertyName, value);
        ref.add(ra);
    }
    
    private static void addIntegerRefProperty(final Reference ref, final String propertyName, final Integer value) {
        if (value == null || propertyName == null) {
            return;
        }
        final RefAddr ra = new StringRefAddr(propertyName, String.valueOf(value));
        ref.add(ra);
    }
    
    public TopicConnection createTopicConnection() throws JMSException {
        return (TopicConnection)this.createConnection();
    }
    
    public TopicConnection createTopicConnection(final String userName, final String password) throws JMSException {
        return (TopicConnection)this.createConnection(userName, password);
    }
    
    public QueueConnection createQueueConnection() throws JMSException {
        return (QueueConnection)this.createConnection();
    }
    
    public QueueConnection createQueueConnection(final String userName, final String password) throws JMSException {
        return (QueueConnection)this.createConnection(userName, password);
    }
    
    public String getUsername() {
        return this.username;
    }
    
    public void setUsername(final String username) {
        if (username != null) {
            this.username = username;
        }
        else {
            this.logger.warn("Cannot set username to null (on {})", (Object)this);
        }
    }
    
    public String getPassword() {
        return this.password;
    }
    
    public void setPassword(final String password) {
        this.password = password;
    }
    
    public String getVirtualHost() {
        return this.virtualHost;
    }
    
    public void setVirtualHost(final String virtualHost) {
        if (virtualHost != null) {
            this.virtualHost = virtualHost;
        }
        else {
            this.logger.warn("Cannot set virtualHost to null (on {})", (Object)this);
        }
    }
    
    public String getHost() {
        return this.host;
    }
    
    public void setHost(final String host) {
        if (host != null) {
            this.host = host;
        }
        else {
            this.logger.warn("Cannot set host to null (on {})", (Object)this);
        }
    }
    
    public int getPort() {
        return (this.port != -1) ? this.port : (this.isSsl() ? 5671 : 5672);
    }
    
    public void setPort(final int port) {
        this.port = port;
    }
    
    public long getTerminationTimeout() {
        return this.terminationTimeout;
    }
    
    public void setTerminationTimeout(final long terminationTimeout) {
        this.terminationTimeout = terminationTimeout;
    }
    
    public int getQueueBrowserReadMax() {
        return this.queueBrowserReadMax;
    }
    
    public void setQueueBrowserReadMax(final int queueBrowserReadMax) {
        this.queueBrowserReadMax = Math.max(0, queueBrowserReadMax);
    }
    
    public int getOnMessageTimeoutMs() {
        return this.onMessageTimeoutMs;
    }
    
    public void setOnMessageTimeoutMs(final int onMessageTimeoutMs) {
        if (onMessageTimeoutMs > 0) {
            this.onMessageTimeoutMs = onMessageTimeoutMs;
        }
        else {
            this.logger.warn("Cannot set onMessageTimeoutMs to non-positive value {} (on {})", (Object)onMessageTimeoutMs, (Object)this);
        }
    }
    
    public int getChannelsQos() {
        return this.channelsQos;
    }
    
    public void setChannelsQos(final int channelsQos) {
        this.channelsQos = channelsQos;
    }
    
    public void setPreferProducerMessageProperty(final boolean preferProducerMessageProperty) {
        this.preferProducerMessageProperty = preferProducerMessageProperty;
    }
    
    public boolean isPreferProducerMessageProperty() {
        return this.preferProducerMessageProperty;
    }
    
    public void setRequeueOnMessageListenerException(final boolean requeueOnMessageListenerException) {
        this.requeueOnMessageListenerException = requeueOnMessageListenerException;
    }
    
    public boolean isRequeueOnMessageListenerException() {
        return this.requeueOnMessageListenerException;
    }
    
    public void setCleanUpServerNamedQueuesForNonDurableTopicsOnSessionClose(final boolean cleanUpServerNamedQueuesForNonDurableTopicsOnSessionClose) {
        this.cleanUpServerNamedQueuesForNonDurableTopicsOnSessionClose = cleanUpServerNamedQueuesForNonDurableTopicsOnSessionClose;
    }
    
    public boolean isCleanUpServerNamedQueuesForNonDurableTopicsOnSessionClose() {
        return this.cleanUpServerNamedQueuesForNonDurableTopicsOnSessionClose;
    }
    
    public void setAmqpPropertiesCustomiser(final AmqpPropertiesCustomiser amqpPropertiesCustomiser) {
        this.amqpPropertiesCustomiser = amqpPropertiesCustomiser;
    }
    
    public void setMetricsCollector(final MetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
    }
    
    public void setHostnameVerification(final boolean hostnameVerification) {
        this.hostnameVerification = hostnameVerification;
    }
    
    public void setHostnameVerifier(final HostnameVerifier hostnameVerifier) {
        this.hostnameVerifier = hostnameVerifier;
    }
    
    public List<String> getUris() {
        final List<String> urisAsStrings = new ArrayList<String>(this.uris.size());
        for (final URI uri : this.uris) {
            urisAsStrings.add(uri.toString());
        }
        return urisAsStrings;
    }
    
    public void setAmqpConnectionFactoryPostProcessor(final AmqpConnectionFactoryPostProcessor amqpConnectionFactoryPostProcessor) {
        this.amqpConnectionFactoryPostProcessor = amqpConnectionFactoryPostProcessor;
    }
    
    public void setThrowExceptionOnConsumerStartFailure(final boolean throwExceptionOnConsumerStartFailure) {
        this.throwExceptionOnConsumerStartFailure = throwExceptionOnConsumerStartFailure;
    }
    
    private interface ConnectionCreator
    {
        com.rabbitmq.client.Connection create(final com.rabbitmq.client.ConnectionFactory p0) throws Exception;
    }
}
