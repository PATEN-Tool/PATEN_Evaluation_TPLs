// 
// Decompiled by Procyon v0.5.36
// 

package com.rabbitmq.jms.client;

import javax.jms.TextMessage;
import javax.jms.StreamMessage;
import javax.jms.ObjectMessage;
import com.rabbitmq.jms.client.message.RMQMapMessage;
import javax.jms.MapMessage;
import javax.jms.BytesMessage;
import com.rabbitmq.jms.util.Util;
import java.util.Date;
import java.lang.reflect.Constructor;
import com.rabbitmq.jms.client.message.RMQStreamMessage;
import com.rabbitmq.jms.client.message.RMQObjectMessage;
import java.lang.reflect.InvocationTargetException;
import java.io.InputStream;
import com.rabbitmq.jms.util.WhiteListObjectInputStream;
import java.util.List;
import java.io.OutputStream;
import java.io.ObjectOutputStream;
import com.rabbitmq.client.BasicProperties;
import com.rabbitmq.jms.client.message.RMQBytesMessage;
import com.rabbitmq.jms.client.message.RMQTextMessage;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.jms.admin.RMQDestination;
import java.io.ByteArrayInputStream;
import java.io.ObjectInput;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutput;
import com.rabbitmq.jms.util.RMQJMSException;
import javax.jms.MessageNotWriteableException;
import java.util.Iterator;
import com.rabbitmq.jms.util.IteratorEnum;
import java.util.Enumeration;
import javax.jms.MessageFormatException;
import javax.jms.Destination;
import javax.jms.JMSException;
import java.util.HashMap;
import org.slf4j.LoggerFactory;
import com.rabbitmq.jms.util.HexDisplay;
import java.io.Serializable;
import java.util.Map;
import java.nio.charset.Charset;
import org.slf4j.Logger;
import javax.jms.Message;

public abstract class RMQMessage implements Message, Cloneable
{
    protected final Logger logger;
    private static final String DIRECT_REPLY_TO = "amq.rabbitmq.reply-to";
    protected static final String NOT_READABLE = "Message not readable";
    protected static final String NOT_WRITEABLE = "Message not writeable";
    protected static final String UNABLE_TO_CAST = "Unable to cast the object, %s, into the specified type %s";
    protected static final String MSG_EOF = "Message EOF";
    private static final String[] RESERVED_NAMES;
    private static final char[] INVALID_STARTS_WITH;
    private static final char[] MAY_NOT_CONTAIN;
    protected static final int DEFAULT_MESSAGE_BODY_SIZE;
    private static final String PREFIX = "rmq.";
    private static final String JMS_MESSAGE_ID = "rmq.jms.message.id";
    private static final String JMS_MESSAGE_TIMESTAMP = "rmq.jms.message.timestamp";
    private static final String JMS_MESSAGE_CORR_ID = "rmq.jms.message.correlation.id";
    private static final String JMS_MESSAGE_REPLY_TO = "rmq.jms.message.reply.to";
    private static final String JMS_MESSAGE_DESTINATION = "rmq.jms.message.destination";
    private static final String JMS_MESSAGE_REDELIVERED = "rmq.jms.message.redelivered";
    private static final String JMS_MESSAGE_TYPE = "rmq.jms.message.type";
    static final String JMS_MESSAGE_DELIVERY_MODE = "rmq.jms.message.delivery.mode";
    static final String JMS_MESSAGE_EXPIRATION = "rmq.jms.message.expiration";
    static final String JMS_MESSAGE_PRIORITY = "rmq.jms.message.priority";
    private static final Charset CHARSET;
    private final Map<String, Serializable> rmqProperties;
    private final Map<String, Serializable> userJmsProperties;
    private volatile String internalMessageID;
    private volatile boolean readonlyProperties;
    private volatile boolean readonlyBody;
    private long rabbitDeliveryTag;
    private transient volatile RMQSession session;
    
    protected void loggerDebugByteArray(final String format, final byte[] buffer, final Object arg) {
        if (this.logger.isDebugEnabled()) {
            final StringBuilder bufferOutput = new StringBuilder("Byte array, length ").append(buffer.length).append(" :\n");
            HexDisplay.decodeByteArrayIntoStringBuilder(buffer, bufferOutput);
            this.logger.debug(format, (Object)bufferOutput.append("end of byte array, length ").append(buffer.length).append('.'), arg);
        }
    }
    
    protected boolean isReadonlyBody() {
        return this.readonlyBody;
    }
    
    protected boolean isReadOnlyProperties() {
        return this.readonlyProperties;
    }
    
    protected void setReadonly(final boolean readonly) {
        this.readonlyBody = readonly;
        this.readonlyProperties = readonly;
    }
    
    protected void setReadOnlyBody(final boolean readonly) {
        this.readonlyBody = readonly;
    }
    
    protected void setReadOnlyProperties(final boolean readonly) {
        this.readonlyProperties = readonly;
    }
    
    public long getRabbitDeliveryTag() {
        return this.rabbitDeliveryTag;
    }
    
    protected void setRabbitDeliveryTag(final long rabbitDeliveryTag) {
        this.rabbitDeliveryTag = rabbitDeliveryTag;
    }
    
    public RMQSession getSession() {
        return this.session;
    }
    
    protected void setSession(final RMQSession session) {
        this.session = session;
    }
    
    public RMQMessage() {
        this.logger = LoggerFactory.getLogger((Class)RMQMessage.class);
        this.rmqProperties = new HashMap<String, Serializable>();
        this.userJmsProperties = new HashMap<String, Serializable>();
        this.internalMessageID = null;
        this.readonlyProperties = false;
        this.readonlyBody = false;
        this.rabbitDeliveryTag = -1L;
        this.session = null;
    }
    
    public String getJMSMessageID() throws JMSException {
        return this.getStringProperty("rmq.jms.message.id");
    }
    
    public void setJMSMessageID(final String id) throws JMSException {
        this.setStringProperty("rmq.jms.message.id", id);
    }
    
    public long getJMSTimestamp() throws JMSException {
        final Object timestamp = this.getObjectProperty("rmq.jms.message.timestamp");
        if (timestamp == null) {
            return 0L;
        }
        return convertToLong(timestamp);
    }
    
    public void setJMSTimestamp(final long timestamp) throws JMSException {
        this.setLongProperty("rmq.jms.message.timestamp", timestamp);
    }
    
    public byte[] getJMSCorrelationIDAsBytes() throws JMSException {
        final String id = this.getStringProperty("rmq.jms.message.correlation.id");
        if (id != null) {
            return id.getBytes(getCharset());
        }
        return null;
    }
    
    public void setJMSCorrelationIDAsBytes(final byte[] correlationID) throws JMSException {
        final String id = (correlationID != null) ? new String(correlationID, getCharset()) : null;
        this.setStringProperty("rmq.jms.message.correlation.id", id);
    }
    
    public void setJMSCorrelationID(final String correlationID) throws JMSException {
        this.setStringProperty("rmq.jms.message.correlation.id", correlationID);
    }
    
    public String getJMSCorrelationID() throws JMSException {
        return this.getStringProperty("rmq.jms.message.correlation.id");
    }
    
    public Destination getJMSReplyTo() throws JMSException {
        return (Destination)this.getObjectProperty("rmq.jms.message.reply.to");
    }
    
    public void setJMSReplyTo(final Destination replyTo) throws JMSException {
        this.setObjectProperty("rmq.jms.message.reply.to", replyTo);
    }
    
    public Destination getJMSDestination() throws JMSException {
        return (Destination)this.getObjectProperty("rmq.jms.message.destination");
    }
    
    public void setJMSDestination(final Destination destination) throws JMSException {
        this.setObjectProperty("rmq.jms.message.destination", destination);
    }
    
    public int getJMSDeliveryMode() throws JMSException {
        return this.getIntProperty("rmq.jms.message.delivery.mode");
    }
    
    public void setJMSDeliveryMode(final int deliveryMode) throws JMSException {
        this.setIntProperty("rmq.jms.message.delivery.mode", deliveryMode);
    }
    
    public boolean getJMSRedelivered() throws JMSException {
        return this.getBooleanProperty("rmq.jms.message.redelivered");
    }
    
    public void setJMSRedelivered(final boolean redelivered) throws JMSException {
        this.setBooleanProperty("rmq.jms.message.redelivered", redelivered);
    }
    
    public String getJMSType() throws JMSException {
        return this.getStringProperty("rmq.jms.message.type");
    }
    
    public void setJMSType(final String type) throws JMSException {
        this.setStringProperty("rmq.jms.message.type", type);
    }
    
    public long getJMSExpiration() throws JMSException {
        return this.getLongProperty("rmq.jms.message.expiration");
    }
    
    public void setJMSExpiration(final long expiration) throws JMSException {
        this.setLongProperty("rmq.jms.message.expiration", expiration);
    }
    
    public int getJMSPriority() throws JMSException {
        return this.getIntProperty("rmq.jms.message.priority");
    }
    
    public void setJMSPriority(final int priority) throws JMSException {
        this.setIntProperty("rmq.jms.message.priority", priority);
    }
    
    public final void clearProperties() throws JMSException {
        this.userJmsProperties.clear();
        this.setReadOnlyProperties(false);
    }
    
    public boolean propertyExists(final String name) throws JMSException {
        return this.userJmsProperties.containsKey(name) || this.rmqProperties.containsKey(name);
    }
    
    public boolean getBooleanProperty(final String name) throws JMSException {
        final Object o = this.getObjectProperty(name);
        if (o == null) {
            return false;
        }
        if (o instanceof String) {
            return Boolean.parseBoolean((String)o);
        }
        if (o instanceof Boolean) {
            return (boolean)o;
        }
        throw new MessageFormatException(String.format("Unable to convert from class [%s]", o.getClass().getName()));
    }
    
    public byte getByteProperty(final String name) throws JMSException {
        final Object o = this.getObjectProperty(name);
        if (o == null) {
            throw new NumberFormatException("Null is not a valid byte");
        }
        if (o instanceof String) {
            return Byte.parseByte((String)o);
        }
        if (o instanceof Byte) {
            return (byte)o;
        }
        throw new MessageFormatException(String.format("Unable to convert from class [%s]", o.getClass().getName()));
    }
    
    public short getShortProperty(final String name) throws JMSException {
        final Object o = this.getObjectProperty(name);
        if (o == null) {
            throw new NumberFormatException("Null is not a valid short");
        }
        if (o instanceof String) {
            return Short.parseShort((String)o);
        }
        if (o instanceof Byte) {
            return (byte)o;
        }
        if (o instanceof Short) {
            return (short)o;
        }
        throw new MessageFormatException(String.format("Unable to convert from class [%s]", o.getClass().getName()));
    }
    
    public int getIntProperty(final String name) throws JMSException {
        final Object o = this.getObjectProperty(name);
        if (o == null) {
            throw new NumberFormatException("Null is not a valid int");
        }
        if (o instanceof String) {
            return Integer.parseInt((String)o);
        }
        if (o instanceof Byte) {
            return (byte)o;
        }
        if (o instanceof Short) {
            return (short)o;
        }
        if (o instanceof Integer) {
            return (int)o;
        }
        throw new MessageFormatException(String.format("Unable to convert from class [%s]", o.getClass().getName()));
    }
    
    public long getLongProperty(final String name) throws JMSException {
        return convertToLong(this.getObjectProperty(name));
    }
    
    private static long convertToLong(final Object o) throws JMSException {
        if (o == null) {
            throw new NumberFormatException("Null is not a valid long");
        }
        if (o instanceof String) {
            return Long.parseLong((String)o);
        }
        if (o instanceof Byte) {
            return (byte)o;
        }
        if (o instanceof Short) {
            return (short)o;
        }
        if (o instanceof Integer) {
            return (int)o;
        }
        if (o instanceof Long) {
            return (long)o;
        }
        throw new MessageFormatException(String.format("Unable to convert from class [%s]", o.getClass().getName()));
    }
    
    public float getFloatProperty(final String name) throws JMSException {
        final Object o = this.getObjectProperty(name);
        if (o == null) {
            throw new NumberFormatException("Null is not a valid float");
        }
        if (o instanceof String) {
            return Float.parseFloat((String)o);
        }
        if (o instanceof Float) {
            return (float)o;
        }
        throw new MessageFormatException(String.format("Unable to convert from class [%s]", o.getClass().getName()));
    }
    
    public double getDoubleProperty(final String name) throws JMSException {
        final Object o = this.getObjectProperty(name);
        if (o == null) {
            throw new NumberFormatException("Null is not a valid double");
        }
        if (o instanceof String) {
            return Double.parseDouble((String)o);
        }
        if (o instanceof Float) {
            return (float)o;
        }
        if (o instanceof Double) {
            return (double)o;
        }
        throw new MessageFormatException(String.format("Unable to convert from class [%s]", o.getClass().getName()));
    }
    
    public String getStringProperty(final String name) throws JMSException {
        final Object o = this.getObjectProperty(name);
        if (o == null) {
            return null;
        }
        if (o instanceof String) {
            return (String)o;
        }
        return o.toString();
    }
    
    public Object getObjectProperty(final String name) throws JMSException {
        if (name.startsWith("rmq.")) {
            return this.rmqProperties.get(name);
        }
        return this.userJmsProperties.get(name);
    }
    
    public Enumeration<?> getPropertyNames() throws JMSException {
        return new IteratorEnum<Object>(this.userJmsProperties.keySet().iterator());
    }
    
    public void setBooleanProperty(final String name, final boolean value) throws JMSException {
        this.setObjectProperty(name, value);
    }
    
    public void setByteProperty(final String name, final byte value) throws JMSException {
        this.setObjectProperty(name, value);
    }
    
    public void setShortProperty(final String name, final short value) throws JMSException {
        this.setObjectProperty(name, value);
    }
    
    public void setIntProperty(final String name, final int value) throws JMSException {
        this.setObjectProperty(name, value);
    }
    
    public void setLongProperty(final String name, final long value) throws JMSException {
        this.setObjectProperty(name, value);
    }
    
    public void setFloatProperty(final String name, final float value) throws JMSException {
        this.setObjectProperty(name, value);
    }
    
    public void setDoubleProperty(final String name, final double value) throws JMSException {
        this.setObjectProperty(name, value);
    }
    
    public void setStringProperty(final String name, final String value) throws JMSException {
        this.setObjectProperty(name, value);
    }
    
    private void checkName(final String name) throws JMSException {
        if (name == null || name.trim().length() == 0) {
            throw new IllegalArgumentException("Invalid identifier:null");
        }
        final char c = name.charAt(0);
        for (final char aINVALID_STARTS_WITH : RMQMessage.INVALID_STARTS_WITH) {
            if (c == aINVALID_STARTS_WITH) {
                throw new JMSException(String.format("Identifier may not start with character [%s]", c));
            }
        }
        for (final char aMAY_NOT_CONTAIN : RMQMessage.MAY_NOT_CONTAIN) {
            if (name.indexOf(aMAY_NOT_CONTAIN) >= 0) {
                throw new JMSException(String.format("Identifier may not contain character [%s]", aMAY_NOT_CONTAIN));
            }
        }
        for (final String RESERVED_NAME : RMQMessage.RESERVED_NAMES) {
            if (name.equalsIgnoreCase(RESERVED_NAME)) {
                throw new JMSException(String.format("Invalid identifier [%s]", RESERVED_NAME));
            }
        }
    }
    
    public void setObjectProperty(final String name, final Object value) throws JMSException {
        try {
            if ("JMSXGroupSeq".equals(name)) {
                if (!(value instanceof Integer)) {
                    throw new MessageFormatException(String.format("Property [%s] can only be of type int", "JMSXGroupSeq"));
                }
                final int val = (int)value;
                if (val <= 0) {
                    throw new JMSException(String.format("Property [%s] must be >0", "JMSXGroupSeq"));
                }
            }
            else if ("JMSXGroupID".equals(name) && value != null && !(value instanceof String)) {
                throw new MessageFormatException(String.format("Property [%s] can only be of type String", "JMSXGroupID"));
            }
            if (name != null && name.startsWith("rmq.")) {
                if (value == null) {
                    this.rmqProperties.remove(name);
                }
                else {
                    this.rmqProperties.put(name, (Serializable)value);
                }
            }
            else {
                if (this.isReadOnlyProperties()) {
                    throw new MessageNotWriteableException("Message not writeable");
                }
                this.checkName(name);
                if (value == null) {
                    this.userJmsProperties.remove(name);
                }
                else {
                    if (!this.validPropertyValueType(value)) {
                        throw new MessageFormatException(String.format("Property [%s] has incorrect value type.", name));
                    }
                    this.userJmsProperties.put(name, (Serializable)value);
                }
            }
        }
        catch (ClassCastException x) {
            throw new RMQJMSException("Property value not serializable.", x);
        }
    }
    
    private boolean validPropertyValueType(final Object value) {
        return value instanceof String || value instanceof Boolean || value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long || value instanceof Float || value instanceof Double;
    }
    
    public void acknowledge() throws JMSException {
        this.getSession().acknowledgeMessage(this);
    }
    
    public final void clearBody() throws JMSException {
        this.setReadOnlyBody(false);
        this.clearBodyInternal();
    }
    
    protected abstract void clearBodyInternal() throws JMSException;
    
    private static Charset getCharset() {
        return RMQMessage.CHARSET;
    }
    
    protected abstract void writeBody(final ObjectOutput p0, final ByteArrayOutputStream p1) throws IOException;
    
    protected abstract void writeAmqpBody(final ByteArrayOutputStream p0) throws IOException;
    
    protected abstract void readBody(final ObjectInput p0, final ByteArrayInputStream p1) throws IOException, ClassNotFoundException;
    
    protected abstract void readAmqpBody(final byte[] p0);
    
    Map<String, Object> toHeaders() throws IOException, JMSException {
        final Map<String, Object> hdrs = new HashMap<String, Object>();
        for (final Map.Entry<String, Serializable> e : this.userJmsProperties.entrySet()) {
            putIfNotNull(hdrs, e.getKey(), e.getValue());
        }
        hdrs.put("JMSDeliveryMode", (this.getJMSDeliveryMode() == 2) ? "PERSISTENT" : "NON_PERSISTENT");
        putIfNotNull(hdrs, "JMSMessageID", this.getJMSMessageID());
        hdrs.put("JMSTimestamp", this.getJMSTimestamp());
        hdrs.put("JMSPriority", this.getJMSPriority());
        putIfNotNull(hdrs, "JMSCorrelationID", this.getJMSCorrelationID());
        putIfNotNull(hdrs, "JMSType", this.getJMSType());
        return hdrs;
    }
    
    static RMQMessage convertMessage(final RMQSession session, final RMQDestination dest, final GetResponse response, final ReceivingContextConsumer receivingContextConsumer) throws JMSException {
        if (response == null) {
            return null;
        }
        if (dest.isAmqp()) {
            return convertAmqpMessage(session, dest, response, receivingContextConsumer);
        }
        return convertJmsMessage(session, response, receivingContextConsumer);
    }
    
    static RMQMessage convertJmsMessage(final RMQSession session, final GetResponse response, final ReceivingContextConsumer receivingContextConsumer) throws JMSException {
        final RMQMessage message = fromMessage(response.getBody(), session.getTrustedPackages());
        message.setSession(session);
        message.setJMSRedelivered(response.getEnvelope().isRedeliver());
        message.setRabbitDeliveryTag(response.getEnvelope().getDeliveryTag());
        message.setReadonly(true);
        maybeSetupDirectReplyTo(message, response.getProps().getReplyTo());
        receivingContextConsumer.accept(new ReceivingContext((Message)message));
        return message;
    }
    
    private static RMQMessage convertAmqpMessage(final RMQSession session, final RMQDestination dest, final GetResponse response, final ReceivingContextConsumer receivingContextConsumer) throws JMSException {
        try {
            final BasicProperties props = (BasicProperties)response.getProps();
            RMQMessage message = isAmqpTextMessage(props.getHeaders()) ? new RMQTextMessage() : new RMQBytesMessage();
            message = fromAmqpMessage(response.getBody(), message);
            message.setSession(session);
            message.setJMSRedelivered(response.getEnvelope().isRedeliver());
            message.setRabbitDeliveryTag(response.getEnvelope().getDeliveryTag());
            message.setJMSDestination((Destination)dest);
            message.setJMSPropertiesFromAmqpProperties(props);
            message.setReadonly(true);
            maybeSetupDirectReplyTo(message, response.getProps().getReplyTo());
            receivingContextConsumer.accept(new ReceivingContext((Message)message));
            return message;
        }
        catch (IOException x) {
            throw new RMQJMSException(x);
        }
    }
    
    private static void maybeSetupDirectReplyTo(final RMQMessage message, final String replyTo) throws JMSException {
        if (replyTo != null && replyTo.startsWith("amq.rabbitmq.reply-to")) {
            final RMQDestination replyToDestination = new RMQDestination("amq.rabbitmq.reply-to", "", replyTo, replyTo);
            message.setJMSReplyTo((Destination)replyToDestination);
        }
    }
    
    public static void doNotDeclareReplyToDestination(final Message message) throws JMSException {
        if (message instanceof RMQMessage && message.getJMSReplyTo() != null && message.getJMSReplyTo() instanceof RMQDestination) {
            ((RMQDestination)message.getJMSReplyTo()).setDeclared(true);
        }
    }
    
    Map<String, Object> toAmqpHeaders() throws IOException, JMSException {
        final Map<String, Object> hdrs = new HashMap<String, Object>();
        for (final Map.Entry<String, Serializable> e : this.userJmsProperties.entrySet()) {
            putIfNotNullAndAmqpType(hdrs, e.getKey(), e.getValue());
        }
        hdrs.put("JMSDeliveryMode", (this.getJMSDeliveryMode() == 2) ? "PERSISTENT" : "NON_PERSISTENT");
        putIfNotNull(hdrs, "JMSMessageID", this.getJMSMessageID());
        hdrs.put("JMSTimestamp", this.getJMSTimestamp());
        hdrs.put("JMSPriority", this.getJMSPriority());
        putIfNotNull(hdrs, "JMSCorrelationID", this.getJMSCorrelationID());
        putIfNotNull(hdrs, "JMSType", this.getJMSType());
        return hdrs;
    }
    
    private static void putIfNotNullAndAmqpType(final Map<String, Object> hdrs, final String key, final Object val) {
        if (val != null && (val instanceof String || val instanceof Integer || val instanceof Float || val instanceof Double || val instanceof Long || val instanceof Short || val instanceof Byte)) {
            hdrs.put(key, val);
        }
    }
    
    private static void putIfNotNull(final Map<String, Object> hdrs, final String key, final Object val) {
        if (val != null) {
            hdrs.put(key, val);
        }
    }
    
    byte[] toAmqpByteArray() throws IOException, JMSException {
        final ByteArrayOutputStream bout = new ByteArrayOutputStream(RMQMessage.DEFAULT_MESSAGE_BODY_SIZE);
        this.writeAmqpBody(bout);
        bout.flush();
        return bout.toByteArray();
    }
    
    byte[] toByteArray() throws IOException, JMSException {
        final ByteArrayOutputStream bout = new ByteArrayOutputStream(RMQMessage.DEFAULT_MESSAGE_BODY_SIZE);
        final ObjectOutputStream out = new ObjectOutputStream(bout);
        out.writeUTF(this.getClass().getName());
        out.writeUTF(this.internalMessageID);
        out.writeInt(this.rmqProperties.size());
        for (final Map.Entry<String, Serializable> entry : this.rmqProperties.entrySet()) {
            out.writeUTF(entry.getKey());
            writePrimitive(entry.getValue(), out, true);
        }
        out.writeInt(this.userJmsProperties.size());
        for (final Map.Entry<String, Serializable> entry : this.userJmsProperties.entrySet()) {
            out.writeUTF(entry.getKey());
            writePrimitive(entry.getValue(), out, true);
        }
        out.flush();
        this.writeBody(out, bout);
        out.flush();
        return bout.toByteArray();
    }
    
    static RMQMessage fromMessage(final byte[] b, final List<String> trustedPackages) throws RMQJMSException {
        try {
            final ByteArrayInputStream bin = new ByteArrayInputStream(b);
            final WhiteListObjectInputStream in = new WhiteListObjectInputStream(bin, trustedPackages);
            final String clazz = in.readUTF();
            final RMQMessage msg = instantiateRmqMessage(clazz, trustedPackages);
            msg.internalMessageID = in.readUTF();
            for (int propsize = in.readInt(), i = 0; i < propsize; ++i) {
                final String name = in.readUTF();
                final Object value = readPrimitive(in);
                msg.rmqProperties.put(name, (Serializable)value);
            }
            for (int propsize = in.readInt(), i = 0; i < propsize; ++i) {
                final String name = in.readUTF();
                final Object value = readPrimitive(in);
                msg.userJmsProperties.put(name, (Serializable)value);
            }
            msg.readBody(in, bin);
            return msg;
        }
        catch (IOException x) {
            throw new RMQJMSException(x);
        }
        catch (ClassNotFoundException x2) {
            throw new RMQJMSException(x2);
        }
    }
    
    private static RMQMessage instantiateRmqMessage(final String messageClass, final List<String> trustedPackages) throws RMQJMSException {
        if (isRmqObjectMessageClass(messageClass)) {
            return instantiateRmqObjectMessageWithTrustedPackages(trustedPackages);
        }
        if (isRmqStreamMessageClass(messageClass)) {
            return instantiateRmqStreamMessageWithTrustedPackages(trustedPackages);
        }
        try {
            return (RMQMessage)Class.forName(messageClass, true, Thread.currentThread().getContextClassLoader()).getDeclaredConstructor((Class<?>[])new Class[0]).newInstance(new Object[0]);
        }
        catch (InstantiationException e) {
            throw new RMQJMSException(e);
        }
        catch (IllegalAccessException e2) {
            throw new RMQJMSException(e2);
        }
        catch (ClassNotFoundException e3) {
            throw new RMQJMSException(e3);
        }
        catch (NoSuchMethodException e4) {
            throw new RMQJMSException(e4);
        }
        catch (InvocationTargetException e5) {
            throw new RMQJMSException(e5);
        }
    }
    
    private static boolean isRmqObjectMessageClass(final String clazz) {
        return RMQObjectMessage.class.getName().equals(clazz);
    }
    
    private static boolean isRmqStreamMessageClass(final String clazz) {
        return RMQStreamMessage.class.getName().equals(clazz);
    }
    
    private static RMQObjectMessage instantiateRmqObjectMessageWithTrustedPackages(final List<String> trustedPackages) throws RMQJMSException {
        return (RMQObjectMessage)instantiateRmqMessageWithTrustedPackages(RMQObjectMessage.class.getName(), trustedPackages);
    }
    
    private static RMQStreamMessage instantiateRmqStreamMessageWithTrustedPackages(final List<String> trustedPackages) throws RMQJMSException {
        return (RMQStreamMessage)instantiateRmqMessageWithTrustedPackages(RMQStreamMessage.class.getName(), trustedPackages);
    }
    
    private static RMQMessage instantiateRmqMessageWithTrustedPackages(final String messageClazz, final List<String> trustedPackages) throws RMQJMSException {
        try {
            final Class<?> messageClass = Class.forName(messageClazz, true, Thread.currentThread().getContextClassLoader());
            final Constructor<?> constructor = messageClass.getConstructor(List.class);
            return (RMQMessage)constructor.newInstance(trustedPackages);
        }
        catch (NoSuchMethodException e) {
            throw new RMQJMSException(e);
        }
        catch (InvocationTargetException e2) {
            throw new RMQJMSException(e2);
        }
        catch (IllegalAccessException e3) {
            throw new RMQJMSException(e3);
        }
        catch (InstantiationException e4) {
            throw new RMQJMSException(e4);
        }
        catch (ClassNotFoundException e5) {
            throw new RMQJMSException(e5);
        }
    }
    
    private static RMQMessage fromAmqpMessage(final byte[] b, final RMQMessage msg) throws IOException {
        msg.readAmqpBody(b);
        return msg;
    }
    
    static int rmqDeliveryMode(final int deliveryMode) {
        return (deliveryMode == 2) ? 2 : 1;
    }
    
    private static int jmsDeliveryMode(final Integer rmqDeliveryMode) {
        return (rmqDeliveryMode != null && rmqDeliveryMode == 2) ? 2 : 1;
    }
    
    private static long jmsExpiration(final String rmqExpiration, Date da) {
        if (null == da) {
            da = new Date();
        }
        if (null == rmqExpiration) {
            return 0L;
        }
        try {
            return da.getTime() + Long.valueOf(rmqExpiration);
        }
        catch (NumberFormatException e) {
            return 0L;
        }
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = 31 * result + ((this.internalMessageID == null) ? 0 : this.internalMessageID.hashCode());
        return result;
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        final RMQMessage other = (RMQMessage)obj;
        if (this.internalMessageID == null) {
            if (other.internalMessageID != null) {
                return false;
            }
        }
        else if (!this.internalMessageID.equals(other.internalMessageID)) {
            return false;
        }
        return true;
    }
    
    public String getInternalID() {
        return this.internalMessageID;
    }
    
    void generateInternalID() {
        this.internalMessageID = Util.generateUUID("");
        this.rmqProperties.put("rmq.jms.message.id", "ID:" + this.internalMessageID);
    }
    
    protected static void writePrimitive(final Object s, final ObjectOutput out) throws IOException, MessageFormatException {
        writePrimitive(s, out, false);
    }
    
    protected static void writePrimitive(final Object s, final ObjectOutput out, final boolean allowSerializable) throws IOException, MessageFormatException {
        if (s == null) {
            out.writeByte(-1);
        }
        else if (s instanceof Boolean) {
            out.writeByte(1);
            out.writeBoolean((boolean)s);
        }
        else if (s instanceof Byte) {
            out.writeByte(2);
            out.writeByte((byte)s);
        }
        else if (s instanceof Short) {
            out.writeByte(3);
            out.writeShort((short)s);
        }
        else if (s instanceof Integer) {
            out.writeByte(4);
            out.writeInt((int)s);
        }
        else if (s instanceof Long) {
            out.writeByte(5);
            out.writeLong((long)s);
        }
        else if (s instanceof Float) {
            out.writeByte(6);
            out.writeFloat((float)s);
        }
        else if (s instanceof Double) {
            out.writeByte(7);
            out.writeDouble((double)s);
        }
        else if (s instanceof String) {
            out.writeByte(8);
            out.writeUTF((String)s);
        }
        else if (s instanceof Character) {
            out.writeByte(9);
            out.writeChar((char)s);
        }
        else if (s instanceof byte[]) {
            out.writeByte(10);
            out.writeInt(((byte[])s).length);
            out.write((byte[])s);
        }
        else {
            if (!allowSerializable || !(s instanceof Serializable)) {
                throw new MessageFormatException(s + " is not a recognized primitive type.");
            }
            out.writeByte(127);
            out.writeObject(s);
        }
    }
    
    protected static Object readPrimitive(final ObjectInput in) throws IOException, ClassNotFoundException {
        final byte b = in.readByte();
        switch (b) {
            case -1: {
                return null;
            }
            case 1: {
                return in.readBoolean();
            }
            case 2: {
                return in.readByte();
            }
            case 3: {
                return in.readShort();
            }
            case 4: {
                return in.readInt();
            }
            case 5: {
                return in.readLong();
            }
            case 6: {
                return in.readFloat();
            }
            case 7: {
                return in.readDouble();
            }
            case 8: {
                return in.readUTF();
            }
            case 9: {
                return in.readChar();
            }
            case 10: {
                final int length = in.readInt();
                final byte[] buf = new byte[length];
                in.read(buf);
                return buf;
            }
            default: {
                return in.readObject();
            }
        }
    }
    
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
    
    private void setJMSPropertiesFromAmqpProperties(final BasicProperties props) throws JMSException {
        this.setJMSDeliveryMode(jmsDeliveryMode(props.getDeliveryMode()));
        final Date da = props.getTimestamp();
        if (null != da) {
            this.setJMSTimestamp(da.getTime() / 1000L);
        }
        final String ex = props.getExpiration();
        this.setJMSExpiration(jmsExpiration(ex, da));
        final Integer pr = props.getPriority();
        if (null != pr) {
            this.setJMSPriority(pr);
        }
        final String mi = props.getMessageId();
        if (null != mi) {
            this.setJMSMessageID(mi);
        }
        final String ci = props.getCorrelationId();
        if (null != ci) {
            this.setJMSCorrelationID(ci);
        }
        this.setJMSType(isAmqpTextMessage(props.getHeaders()) ? "TextMessage" : "BytesMessage");
        final Map<String, Object> hdrs = (Map<String, Object>)props.getHeaders();
        if (hdrs != null) {
            for (final Map.Entry<String, Object> e : hdrs.entrySet()) {
                final String key = e.getKey();
                final Object val = e.getValue();
                if (key.equals("JMSExpiration")) {
                    this.setJMSExpiration(objectToLong(val, 0L));
                }
                else if (key.equals("JMSPriority")) {
                    this.setJMSPriority(objectToInt(val, 4));
                }
                else if (key.equals("JMSTimestamp")) {
                    this.setJMSTimestamp(objectToLong(val, 0L));
                }
                else if (key.equals("JMSMessageID")) {
                    this.setJMSMessageID(val.toString());
                }
                else if (key.equals("JMSCorrelationID")) {
                    this.setJMSCorrelationID(val.toString());
                }
                else if (key.equals("JMSType")) {
                    this.setJMSType(val.toString());
                }
                else {
                    if (key.startsWith("rmq.")) {
                        continue;
                    }
                    if (key.startsWith("JMS")) {
                        continue;
                    }
                    this.userJmsProperties.put(key, val.toString());
                }
            }
        }
    }
    
    private static boolean isAmqpTextMessage(final Map<String, Object> hdrs) {
        boolean isTextMessage = false;
        if (hdrs != null) {
            final Object headerJMSType = hdrs.get("JMSType");
            isTextMessage = (headerJMSType != null && "TextMessage".equals(headerJMSType.toString()));
        }
        return isTextMessage;
    }
    
    private static long objectToLong(final Object val, final long dft) {
        if (val == null) {
            return dft;
        }
        if (val instanceof Number) {
            return ((Number)val).longValue();
        }
        try {
            if (val instanceof CharSequence) {
                return Long.valueOf(val.toString());
            }
        }
        catch (NumberFormatException ex) {}
        return dft;
    }
    
    private static int objectToInt(final Object val, final int dft) {
        if (val == null) {
            return dft;
        }
        if (val instanceof Number) {
            return ((Number)val).intValue();
        }
        try {
            if (val instanceof CharSequence) {
                return Integer.valueOf(val.toString());
            }
        }
        catch (NumberFormatException ex) {}
        return dft;
    }
    
    static RMQMessage normalise(final Message msg) throws JMSException {
        if (msg instanceof RMQMessage) {
            return (RMQMessage)msg;
        }
        if (msg instanceof BytesMessage) {
            return RMQBytesMessage.recreate((BytesMessage)msg);
        }
        if (msg instanceof MapMessage) {
            return RMQMapMessage.recreate((MapMessage)msg);
        }
        if (msg instanceof ObjectMessage) {
            return RMQObjectMessage.recreate((ObjectMessage)msg);
        }
        if (msg instanceof StreamMessage) {
            return RMQStreamMessage.recreate((StreamMessage)msg);
        }
        if (msg instanceof TextMessage) {
            return RMQTextMessage.recreate((TextMessage)msg);
        }
        return RMQNullMessage.recreate(msg);
    }
    
    protected static void copyAttributes(final RMQMessage rmqMessage, final Message message) throws JMSException {
        try {
            rmqMessage.setJMSCorrelationID(message.getJMSCorrelationID());
            rmqMessage.setJMSType(message.getJMSType());
            copyProperties(rmqMessage, message);
        }
        catch (Exception e) {
            throw new RMQJMSException("Error converting Message to RMQMessage.", e);
        }
    }
    
    private static void copyProperties(final RMQMessage rmqMsg, final Message msg) throws JMSException {
        final Enumeration<String> propNames = (Enumeration<String>)msg.getPropertyNames();
        while (propNames.hasMoreElements()) {
            final String name = propNames.nextElement();
            rmqMsg.setObjectProperty(name, msg.getObjectProperty(name));
        }
    }
    
    static {
        RESERVED_NAMES = new String[] { "NULL", "TRUE", "FALSE", "NOT", "AND", "OR", "BETWEEN", "LIKE", "IN", "IS", "ESCAPE" };
        INVALID_STARTS_WITH = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '+', '\'', '\"', '.' };
        MAY_NOT_CONTAIN = new char[] { '\'', '\"' };
        DEFAULT_MESSAGE_BODY_SIZE = Integer.getInteger("com.rabbitmq.jms.client.message.size", 512);
        CHARSET = Charset.forName("UTF-8");
    }
}
