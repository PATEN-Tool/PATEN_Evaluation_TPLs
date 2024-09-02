// 
// Decompiled by Procyon v0.5.36
// 

package com.rabbitmq.jms.client.message;

import javax.jms.Message;
import java.io.InputStream;
import java.io.UTFDataFormatException;
import com.rabbitmq.jms.util.RMQMessageFormatException;
import java.io.EOFException;
import javax.jms.MessageEOFException;
import java.io.ObjectInput;
import javax.jms.MessageFormatException;
import javax.jms.MessageNotReadableException;
import javax.jms.JMSException;
import com.rabbitmq.jms.util.RMQJMSException;
import java.io.ObjectOutput;
import javax.jms.MessageNotWriteableException;
import java.io.IOException;
import java.io.OutputStream;
import com.rabbitmq.jms.util.WhiteListObjectInputStream;
import java.util.List;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import javax.jms.StreamMessage;
import com.rabbitmq.jms.client.RMQMessage;

public class RMQStreamMessage extends RMQMessage implements StreamMessage
{
    private static final byte[] EOF_ARRAY;
    private volatile boolean reading;
    private transient ObjectInputStream in;
    private transient ByteArrayInputStream bin;
    private transient ObjectOutputStream out;
    private transient ByteArrayOutputStream bout;
    private transient volatile byte[] buf;
    private transient volatile byte[] readbuf;
    private final List<String> trustedPackages;
    
    public RMQStreamMessage(final List<String> trustedPackages) {
        this(false, trustedPackages);
    }
    
    public RMQStreamMessage() {
        this(false, WhiteListObjectInputStream.DEFAULT_TRUSTED_PACKAGES);
    }
    
    private RMQStreamMessage(final boolean reading, final List<String> trustedPackages) {
        this.readbuf = null;
        this.reading = reading;
        this.trustedPackages = trustedPackages;
        if (!reading) {
            this.bout = new ByteArrayOutputStream(RMQMessage.DEFAULT_MESSAGE_BODY_SIZE);
            try {
                this.out = new ObjectOutputStream(this.bout);
            }
            catch (IOException x) {
                throw new RuntimeException(x);
            }
        }
    }
    
    private void writePrimitive(final Object value) throws JMSException {
        if (this.reading || this.isReadonlyBody()) {
            throw new MessageNotWriteableException("Message not writeable");
        }
        try {
            RMQMessage.writePrimitive(value, this.out);
        }
        catch (IOException x) {
            throw new RMQJMSException(x);
        }
    }
    
    private Object readPrimitiveType(final Class<?> type) throws JMSException {
        if (!this.reading) {
            throw new MessageNotReadableException("Message not readable");
        }
        if (this.readbuf != null) {
            throw new MessageFormatException("You must call 'int readBytes(byte[])' since the buffer is not empty");
        }
        boolean success = true;
        try {
            this.bin.mark(0);
            final Object o = RMQMessage.readPrimitive(this.in);
            if (o instanceof byte[]) {
                if (type == ByteArray.class || type == Object.class) {
                    return o;
                }
                throw new MessageFormatException(String.format("Unable to cast the object, %s, into the specified type %s", o, "byte[]"));
            }
            else if (type == ByteArray.class) {
                if (o == null) {
                    return null;
                }
                throw new MessageFormatException(String.format("Unable to cast the object, %s, into the specified type %s", o, "byte[]"));
            }
            else if (type == Boolean.class) {
                if (o == null) {
                    return Boolean.FALSE;
                }
                if (o instanceof Boolean) {
                    return o;
                }
                if (o instanceof String) {
                    return Boolean.parseBoolean((String)o);
                }
                throw new MessageFormatException(String.format("Unable to cast the object, %s, into the specified type %s", o, "boolean"));
            }
            else if (type == Byte.class) {
                if (o instanceof Byte) {
                    return o;
                }
                if (o instanceof String) {
                    return Byte.parseByte((String)o);
                }
                throw new MessageFormatException(String.format("Unable to cast the object, %s, into the specified type %s", o, "byte"));
            }
            else if (type == Short.class) {
                if (o instanceof Byte) {
                    return o;
                }
                if (o instanceof Short) {
                    return o;
                }
                if (o instanceof String) {
                    return Short.parseShort((String)o);
                }
                throw new MessageFormatException(String.format("Unable to cast the object, %s, into the specified type %s", o, "byte"));
            }
            else if (type == Integer.class) {
                if (o instanceof Byte) {
                    return o;
                }
                if (o instanceof Short) {
                    return o;
                }
                if (o instanceof Integer) {
                    return o;
                }
                if (o instanceof String) {
                    return Integer.parseInt((String)o);
                }
                throw new MessageFormatException(String.format("Unable to cast the object, %s, into the specified type %s", o, "int"));
            }
            else if (type == Character.class) {
                if (o instanceof Character) {
                    return o;
                }
                throw new MessageFormatException(String.format("Unable to cast the object, %s, into the specified type %s", o, "char"));
            }
            else if (type == Long.class) {
                if (o instanceof Byte) {
                    return o;
                }
                if (o instanceof Short) {
                    return o;
                }
                if (o instanceof Integer) {
                    return o;
                }
                if (o instanceof Long) {
                    return o;
                }
                if (o instanceof String) {
                    return Long.parseLong((String)o);
                }
                throw new MessageFormatException(String.format("Unable to cast the object, %s, into the specified type %s", o, "long"));
            }
            else if (type == Float.class) {
                if (o instanceof Float) {
                    return o;
                }
                if (o instanceof String) {
                    return Float.parseFloat((String)o);
                }
                throw new MessageFormatException(String.format("Unable to cast the object, %s, into the specified type %s", o, "float"));
            }
            else if (type == Double.class) {
                if (o instanceof Float) {
                    return o;
                }
                if (o instanceof Double) {
                    return o;
                }
                if (o instanceof String) {
                    return Double.parseDouble((String)o);
                }
                throw new MessageFormatException(String.format("Unable to cast the object, %s, into the specified type %s", o, "double"));
            }
            else if (type == String.class) {
                if (o == null) {
                    return null;
                }
                if (o instanceof byte[]) {
                    throw new MessageFormatException(String.format("Unable to cast the object, %s, into the specified type %s", o, "String"));
                }
                return o.toString();
            }
            else {
                if (type == Object.class) {
                    return o;
                }
                throw new MessageFormatException(String.format("Unable to cast the object, %s, into the specified type %s", o, type.toString()));
            }
        }
        catch (NumberFormatException x) {
            success = false;
            throw x;
        }
        catch (ClassNotFoundException x2) {
            success = false;
            throw new RMQJMSException(x2);
        }
        catch (EOFException x6) {
            success = false;
            throw new MessageEOFException("Message EOF");
        }
        catch (UTFDataFormatException x3) {
            success = false;
            throw new RMQMessageFormatException(x3);
        }
        catch (IOException x4) {
            success = false;
            throw new RMQJMSException(x4);
        }
        catch (Exception x5) {
            success = false;
            if (x5 instanceof JMSException) {
                throw (JMSException)x5;
            }
            throw new RMQJMSException(x5);
        }
        finally {
            if (!success) {
                this.bin.reset();
            }
        }
    }
    
    public boolean readBoolean() throws JMSException {
        return (boolean)this.readPrimitiveType(Boolean.class);
    }
    
    public byte readByte() throws JMSException {
        return (byte)this.readPrimitiveType(Byte.class);
    }
    
    public short readShort() throws JMSException {
        return (short)this.readPrimitiveType(Short.class);
    }
    
    public char readChar() throws JMSException {
        return (char)this.readPrimitiveType(Character.class);
    }
    
    public int readInt() throws JMSException {
        return (int)this.readPrimitiveType(Integer.class);
    }
    
    public long readLong() throws JMSException {
        return (long)this.readPrimitiveType(Long.class);
    }
    
    public float readFloat() throws JMSException {
        return (float)this.readPrimitiveType(Float.class);
    }
    
    public double readDouble() throws JMSException {
        return (double)this.readPrimitiveType(Double.class);
    }
    
    public String readString() throws JMSException {
        return (String)this.readPrimitiveType(String.class);
    }
    
    public int readBytes(final byte[] value) throws JMSException {
        if (this.readbuf == null) {
            this.readbuf = (byte[])this.readPrimitiveType(ByteArray.class);
            if (this.readbuf == null) {
                return -1;
            }
        }
        if (this.readbuf == null) {
            throw new MessageFormatException(String.format("Unable to cast the object, %s, into the specified type %s", null, "byte[]"));
        }
        if (this.readbuf == RMQStreamMessage.EOF_ARRAY) {
            this.readbuf = null;
            return -1;
        }
        if (this.readbuf.length > value.length) {
            final int result = value.length;
            final int diff = this.readbuf.length - result;
            System.arraycopy(this.readbuf, 0, value, 0, result);
            final byte[] tmp = new byte[diff];
            System.arraycopy(this.readbuf, result, tmp, 0, diff);
            this.readbuf = tmp;
            return result;
        }
        final int result = Math.min(this.readbuf.length, value.length);
        System.arraycopy(this.readbuf, 0, value, 0, result);
        this.readbuf = (byte[])((result == value.length) ? RMQStreamMessage.EOF_ARRAY : null);
        return result;
    }
    
    public Object readObject() throws JMSException {
        return this.readPrimitiveType(Object.class);
    }
    
    public void writeBoolean(final boolean value) throws JMSException {
        this.writePrimitive(value);
    }
    
    public void writeByte(final byte value) throws JMSException {
        this.writePrimitive(value);
    }
    
    public void writeShort(final short value) throws JMSException {
        this.writePrimitive(value);
    }
    
    public void writeChar(final char value) throws JMSException {
        this.writePrimitive(value);
    }
    
    public void writeInt(final int value) throws JMSException {
        this.writePrimitive(value);
    }
    
    public void writeLong(final long value) throws JMSException {
        this.writePrimitive(value);
    }
    
    public void writeFloat(final float value) throws JMSException {
        this.writePrimitive(value);
    }
    
    public void writeDouble(final double value) throws JMSException {
        this.writePrimitive(value);
    }
    
    public void writeString(final String value) throws JMSException {
        this.writePrimitive(value);
    }
    
    public void writeBytes(final byte[] value) throws JMSException {
        this.writePrimitive(value);
    }
    
    public void writeBytes(final byte[] value, final int offset, final int length) throws JMSException {
        final byte[] buf = new byte[length];
        System.arraycopy(value, offset, buf, 0, length);
        this.writePrimitive(buf);
    }
    
    public void writeObject(final Object value) throws JMSException {
        this.writeObject(value, false);
    }
    
    private void writeObject(final Object value, final boolean allowSerializable) throws JMSException {
        if (this.reading || this.isReadonlyBody()) {
            throw new MessageNotWriteableException("Message not writeable");
        }
        try {
            RMQMessage.writePrimitive(value, this.out, allowSerializable);
        }
        catch (IOException x) {
            throw new RMQJMSException(x);
        }
    }
    
    public void reset() throws JMSException {
        this.readbuf = null;
        if (this.reading) {
            try {
                this.bin = new ByteArrayInputStream(this.buf);
                this.in = new ObjectInputStream(this.bin);
                return;
            }
            catch (IOException x) {
                throw new RMQJMSException(x);
            }
        }
        try {
            this.buf = null;
            if (this.out != null) {
                this.out.flush();
                this.buf = this.bout.toByteArray();
            }
            else {
                this.buf = new byte[0];
            }
            this.bin = new ByteArrayInputStream(this.buf);
            this.in = new ObjectInputStream(this.bin);
        }
        catch (IOException x) {
            throw new RMQJMSException(x);
        }
        this.reading = true;
        this.out = null;
        this.bout = null;
    }
    
    public void clearBodyInternal() throws JMSException {
        this.bout = new ByteArrayOutputStream(RMQMessage.DEFAULT_MESSAGE_BODY_SIZE);
        try {
            this.out = new ObjectOutputStream(this.bout);
        }
        catch (IOException x) {
            throw new RMQJMSException(x);
        }
        this.bin = null;
        this.in = null;
        this.buf = null;
        this.readbuf = null;
        this.reading = false;
    }
    
    @Override
    protected void writeBody(final ObjectOutput out, final ByteArrayOutputStream bout) throws IOException {
        this.out.flush();
        final byte[] buf = this.bout.toByteArray();
        out.writeInt(buf.length);
        out.write(buf);
    }
    
    @Override
    protected void readBody(final ObjectInput inputStream, final ByteArrayInputStream bin) throws IOException, ClassNotFoundException {
        final int len = inputStream.readInt();
        inputStream.read(this.buf = new byte[len]);
        this.reading = true;
        this.bin = new ByteArrayInputStream(this.buf);
        this.in = new WhiteListObjectInputStream(this.bin, this.trustedPackages);
    }
    
    @Override
    protected void readAmqpBody(final byte[] barr) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    protected void writeAmqpBody(final ByteArrayOutputStream out) throws IOException {
        throw new UnsupportedOperationException();
    }
    
    public static final RMQMessage recreate(final StreamMessage msg) throws JMSException {
        final RMQStreamMessage rmqSMsg = new RMQStreamMessage();
        RMQMessage.copyAttributes(rmqSMsg, (Message)msg);
        msg.reset();
        boolean endOfStream = false;
        while (!endOfStream) {
            try {
                rmqSMsg.writeObject(msg.readObject());
            }
            catch (MessageEOFException e) {
                endOfStream = true;
            }
        }
        return rmqSMsg;
    }
    
    static {
        EOF_ARRAY = new byte[0];
    }
    
    private static class ByteArray
    {
    }
}
