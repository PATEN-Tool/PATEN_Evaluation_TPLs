// 
// Decompiled by Procyon v0.5.36
// 

package org.fusesource.mqtt.codec;

import org.fusesource.mqtt.client.QoS;
import java.io.IOException;
import org.fusesource.hawtbuf.DataByteArrayOutputStream;
import org.fusesource.hawtbuf.Buffer;
import java.net.ProtocolException;
import org.fusesource.hawtbuf.UTF8Buffer;
import org.fusesource.hawtbuf.DataByteArrayInputStream;

public class MessageSupport
{
    protected static UTF8Buffer readUTF(final DataByteArrayInputStream is) throws ProtocolException {
        final int size = is.readShort();
        final Buffer buffer = is.readBuffer(size);
        if (buffer == null || buffer.length != size) {
            throw new ProtocolException("Invalid message encoding");
        }
        return buffer.utf8();
    }
    
    protected static void writeUTF(final DataByteArrayOutputStream os, final Buffer buffer) throws IOException {
        os.writeShort(buffer.length);
        os.write(buffer);
    }
    
    public abstract static class AckBase
    {
        short messageId;
        
        abstract byte messageType();
        
        protected AckBase decode(final MQTTFrame frame) throws ProtocolException {
            assert frame.buffers.length == 1;
            final DataByteArrayInputStream is = new DataByteArrayInputStream(frame.buffers[0]);
            this.messageId = is.readShort();
            return this;
        }
        
        public MQTTFrame encode() {
            try {
                final DataByteArrayOutputStream os = new DataByteArrayOutputStream(2);
                os.writeShort((int)this.messageId);
                final MQTTFrame frame = new MQTTFrame();
                frame.commandType(this.messageType());
                return frame.buffer(os.toBuffer());
            }
            catch (IOException e) {
                throw new RuntimeException("The impossible happened");
            }
        }
        
        public short messageId() {
            return this.messageId;
        }
        
        protected AckBase messageId(final short messageId) {
            this.messageId = messageId;
            return this;
        }
        
        @Override
        public String toString() {
            return this.getClass().getSimpleName() + "{" + "messageId=" + this.messageId + '}';
        }
    }
    
    public abstract static class EmptyBase
    {
        abstract byte messageType();
        
        protected EmptyBase decode(final MQTTFrame frame) throws ProtocolException {
            return this;
        }
        
        public MQTTFrame encode() {
            return new MQTTFrame().commandType(this.messageType());
        }
    }
    
    public static class HeaderBase
    {
        protected byte header;
        
        protected byte header() {
            return this.header;
        }
        
        protected HeaderBase header(final byte header) {
            this.header = header;
            return this;
        }
        
        protected byte messageType() {
            return (byte)((this.header & 0xF0) >>> 4);
        }
        
        protected HeaderBase commandType(final int type) {
            this.header &= 0xF;
            this.header |= (byte)(type << 4 & 0xF0);
            return this;
        }
        
        protected QoS qos() {
            return QoS.values()[(this.header & 0x6) >>> 1];
        }
        
        protected HeaderBase qos(final QoS qos) {
            this.header &= (byte)249;
            this.header |= (byte)(qos.ordinal() << 1 & 0x6);
            return this;
        }
        
        protected boolean dup() {
            return (this.header & 0x8) > 0;
        }
        
        protected HeaderBase dup(final boolean dup) {
            if (dup) {
                this.header |= 0x8;
            }
            else {
                this.header &= (byte)247;
            }
            return this;
        }
        
        protected boolean retain() {
            return (this.header & 0x1) > 0;
        }
        
        protected HeaderBase retain(final boolean retain) {
            if (retain) {
                this.header |= 0x1;
            }
            else {
                this.header &= (byte)254;
            }
            return this;
        }
    }
    
    public interface Acked extends Message
    {
        boolean dup();
        
        Acked dup(final boolean p0);
        
        QoS qos();
        
        short messageId();
        
        Acked messageId(final short p0);
    }
    
    public interface Message
    {
        byte messageType();
        
        Message decode(final MQTTFrame p0) throws ProtocolException;
        
        MQTTFrame encode();
    }
}
