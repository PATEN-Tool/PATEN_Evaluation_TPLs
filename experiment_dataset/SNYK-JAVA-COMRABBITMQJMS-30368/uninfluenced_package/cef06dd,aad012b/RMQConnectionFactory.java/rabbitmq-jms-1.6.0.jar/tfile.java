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
import java.util.concurrent.TimeoutException;
import com.rabbitmq.jms.util.RMQJMSException;
import java.io.IOException;
import javax.net.ssl.SSLException;
import com.rabbitmq.jms.util.RMQJMSSecurityException;
import com.rabbitmq.jms.client.RMQConnection;
import com.rabbitmq.jms.client.ConnectionParams;
import com.rabbitmq.client.Address;
import javax.jms.JMSException;
import javax.jms.Connection;
import com.rabbitmq.jms.util.WhiteListObjectInputStream;
import org.slf4j.LoggerFactory;
import java.util.List;
import javax.net.ssl.SSLContext;
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
    private boolean ssl;
    private String tlsProtocol;
    private SSLContext sslContext;
    private boolean useDefaultSslContext;
    private int queueBrowserReadMax;
    private volatile long terminationTimeout;
    private int channelsQos;
    private List<String> trustedPackages;
    
    public RMQConnectionFactory() {
        this.logger = LoggerFactory.getLogger((Class)RMQConnectionFactory.class);
        this.username = "guest";
        this.password = "guest";
        this.virtualHost = "/";
        this.host = "localhost";
        this.port = -1;
        this.onMessageTimeoutMs = 2000;
        this.ssl = false;
        this.useDefaultSslContext = false;
        this.queueBrowserReadMax = Math.max(0, Integer.getInteger("rabbit.jms.queueBrowserReadMax", 0));
        this.terminationTimeout = Long.getLong("rabbit.jms.terminationTimeout", 15000L);
        this.channelsQos = -1;
        this.trustedPackages = WhiteListObjectInputStream.DEFAULT_TRUSTED_PACKAGES;
    }
    
    public Connection createConnection() throws JMSException {
        return this.createConnection(this.username, this.password);
    }
    
    public Connection createConnection(final List<Address> endpoints) throws JMSException {
        return this.createConnection(this.username, this.password, endpoints);
    }
    
    public Connection createConnection(final String username, final String password) throws JMSException {
        this.logger.trace("Creating a connection for username '{}', password 'xxxxxxxx'.", (Object)username);
        this.username = username;
        this.password = password;
        final com.rabbitmq.client.ConnectionFactory factory = new com.rabbitmq.client.ConnectionFactory();
        setRabbitUri(this.logger, this, factory, this.getUri());
        this.maybeEnableTLS(factory);
        final com.rabbitmq.client.Connection rabbitConnection = this.instantiateNodeConnection(factory);
        final RMQConnection conn = new RMQConnection(new ConnectionParams().setRabbitConnection(rabbitConnection).setTerminationTimeout(this.getTerminationTimeout()).setQueueBrowserReadMax(this.getQueueBrowserReadMax()).setOnMessageTimeoutMs(this.getOnMessageTimeoutMs()).setChannelsQos(this.channelsQos));
        conn.setTrustedPackages(this.trustedPackages);
        this.logger.debug("Connection {} created.", (Object)conn);
        return (Connection)conn;
    }
    
    public Connection createConnection(final String username, final String password, final List<Address> endpoints) throws JMSException {
        this.logger.trace("Creating a connection for username '{}', password 'xxxxxxxx'.", (Object)username);
        this.username = username;
        this.password = password;
        final com.rabbitmq.client.ConnectionFactory cf = new com.rabbitmq.client.ConnectionFactory();
        this.maybeEnableTLS(cf);
        final com.rabbitmq.client.Connection rabbitConnection = this.instantiateNodeConnection(cf, endpoints);
        final RMQConnection conn = new RMQConnection(new ConnectionParams().setRabbitConnection(rabbitConnection).setTerminationTimeout(this.getTerminationTimeout()).setQueueBrowserReadMax(this.getQueueBrowserReadMax()).setOnMessageTimeoutMs(this.getOnMessageTimeoutMs()));
        conn.setTrustedPackages(this.trustedPackages);
        this.logger.debug("Connection {} created.", (Object)conn);
        return (Connection)conn;
    }
    
    private com.rabbitmq.client.Connection instantiateNodeConnection(final com.rabbitmq.client.ConnectionFactory cf) throws JMSException {
        try {
            return cf.newConnection();
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
    
    private com.rabbitmq.client.Connection instantiateNodeConnection(final com.rabbitmq.client.ConnectionFactory cf, final List<Address> endpoints) throws JMSException {
        try {
            return cf.newConnection((List)endpoints);
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
        final com.rabbitmq.client.ConnectionFactory factory = new com.rabbitmq.client.ConnectionFactory();
        setRabbitUri(this.logger, this, factory, uriString);
        this.host = factory.getHost();
        this.password = factory.getPassword();
        this.port = factory.getPort();
        this.ssl = factory.isSSL();
        this.username = factory.getUsername();
        this.virtualHost = factory.getVirtualHost();
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
}
