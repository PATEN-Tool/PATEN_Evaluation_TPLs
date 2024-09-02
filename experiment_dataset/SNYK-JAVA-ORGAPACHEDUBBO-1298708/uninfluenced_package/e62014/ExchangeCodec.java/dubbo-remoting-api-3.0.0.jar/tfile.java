// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.dubbo.remoting.exchange.codec;

import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.serialize.ObjectInput;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.transport.ExceedPayloadLimitException;
import org.apache.dubbo.common.serialize.ObjectOutput;
import org.apache.dubbo.common.serialize.Serialization;
import org.apache.dubbo.common.serialize.Cleanable;
import java.io.OutputStream;
import org.apache.dubbo.remoting.buffer.ChannelBufferOutputStream;
import org.apache.dubbo.remoting.exchange.support.DefaultFuture;
import org.apache.dubbo.common.Version;
import org.apache.dubbo.common.utils.StringUtils;
import java.io.ByteArrayInputStream;
import org.apache.dubbo.remoting.transport.CodecSupport;
import org.apache.dubbo.common.io.StreamUtils;
import java.io.InputStream;
import org.apache.dubbo.remoting.buffer.ChannelBufferInputStream;
import org.apache.dubbo.remoting.transport.AbstractCodec;
import org.apache.dubbo.remoting.Codec2;
import org.apache.dubbo.common.io.Bytes;
import java.io.IOException;
import org.apache.dubbo.remoting.exchange.Response;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.remoting.buffer.ChannelBuffer;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.remoting.telnet.codec.TelnetCodec;

public class ExchangeCodec extends TelnetCodec
{
    protected static final int HEADER_LENGTH = 16;
    protected static final short MAGIC = -9541;
    protected static final byte MAGIC_HIGH;
    protected static final byte MAGIC_LOW;
    protected static final byte FLAG_REQUEST = Byte.MIN_VALUE;
    protected static final byte FLAG_TWOWAY = 64;
    protected static final byte FLAG_EVENT = 32;
    protected static final int SERIALIZATION_MASK = 31;
    private static final Logger logger;
    
    public Short getMagicCode() {
        return -9541;
    }
    
    @Override
    public void encode(final Channel channel, final ChannelBuffer buffer, final Object msg) throws IOException {
        if (msg instanceof Request) {
            this.encodeRequest(channel, buffer, (Request)msg);
        }
        else if (msg instanceof Response) {
            this.encodeResponse(channel, buffer, (Response)msg);
        }
        else {
            super.encode(channel, buffer, msg);
        }
    }
    
    @Override
    public Object decode(final Channel channel, final ChannelBuffer buffer) throws IOException {
        final int readable = buffer.readableBytes();
        final byte[] header = new byte[Math.min(readable, 16)];
        buffer.readBytes(header);
        return this.decode(channel, buffer, readable, header);
    }
    
    @Override
    protected Object decode(final Channel channel, final ChannelBuffer buffer, final int readable, byte[] header) throws IOException {
        if ((readable > 0 && header[0] != ExchangeCodec.MAGIC_HIGH) || (readable > 1 && header[1] != ExchangeCodec.MAGIC_LOW)) {
            final int length = header.length;
            if (header.length < readable) {
                header = Bytes.copyOf(header, readable);
                buffer.readBytes(header, length, readable - length);
            }
            for (int i = 1; i < header.length - 1; ++i) {
                if (header[i] == ExchangeCodec.MAGIC_HIGH && header[i + 1] == ExchangeCodec.MAGIC_LOW) {
                    buffer.readerIndex(buffer.readerIndex() - header.length + i);
                    header = Bytes.copyOf(header, i);
                    break;
                }
            }
            return super.decode(channel, buffer, readable, header);
        }
        if (readable < 16) {
            return Codec2.DecodeResult.NEED_MORE_INPUT;
        }
        final int len = Bytes.bytes2int(header, 12);
        final Object obj = this.finishRespWhenOverPayload(channel, len, header);
        if (null != obj) {
            return obj;
        }
        AbstractCodec.checkPayload(channel, len);
        final int tt = len + 16;
        if (readable < tt) {
            return Codec2.DecodeResult.NEED_MORE_INPUT;
        }
        final ChannelBufferInputStream is = new ChannelBufferInputStream(buffer, len);
        try {
            return this.decodeBody(channel, is, header);
        }
        finally {
            if (is.available() > 0) {
                try {
                    if (ExchangeCodec.logger.isWarnEnabled()) {
                        ExchangeCodec.logger.warn("Skip input stream " + is.available());
                    }
                    StreamUtils.skipUnusedStream((InputStream)is);
                }
                catch (IOException e) {
                    ExchangeCodec.logger.warn(e.getMessage(), (Throwable)e);
                }
            }
        }
    }
    
    protected Object decodeBody(final Channel channel, final InputStream is, final byte[] header) throws IOException {
        final byte flag = header[2];
        final byte proto = (byte)(flag & 0x1F);
        final long id = Bytes.bytes2long(header, 4);
        if ((flag & 0xFFFFFF80) == 0x0) {
            final Response res = new Response(id);
            if ((flag & 0x20) != 0x0) {
                res.setEvent(true);
            }
            final byte status = header[3];
            res.setStatus(status);
            try {
                if (status == 20) {
                    Object data;
                    if (res.isEvent()) {
                        final byte[] eventPayload = CodecSupport.getPayload(is);
                        if (CodecSupport.isHeartBeat(eventPayload, proto)) {
                            data = null;
                        }
                        else {
                            data = this.decodeEventData(channel, CodecSupport.deserialize(channel.getUrl(), new ByteArrayInputStream(eventPayload), proto), eventPayload);
                        }
                    }
                    else {
                        data = this.decodeResponseData(channel, CodecSupport.deserialize(channel.getUrl(), is, proto), this.getRequestData(id));
                    }
                    res.setResult(data);
                }
                else {
                    res.setErrorMessage(CodecSupport.deserialize(channel.getUrl(), is, proto).readUTF());
                }
            }
            catch (Throwable t) {
                res.setStatus((byte)90);
                res.setErrorMessage(StringUtils.toString(t));
            }
            return res;
        }
        final Request req = new Request(id);
        req.setVersion(Version.getProtocolVersion());
        req.setTwoWay((flag & 0x40) != 0x0);
        if ((flag & 0x20) != 0x0) {
            req.setEvent(true);
        }
        try {
            Object data2;
            if (req.isEvent()) {
                final byte[] eventPayload2 = CodecSupport.getPayload(is);
                if (CodecSupport.isHeartBeat(eventPayload2, proto)) {
                    data2 = null;
                }
                else {
                    data2 = this.decodeEventData(channel, CodecSupport.deserialize(channel.getUrl(), new ByteArrayInputStream(eventPayload2), proto), eventPayload2);
                }
            }
            else {
                data2 = this.decodeRequestData(channel, CodecSupport.deserialize(channel.getUrl(), is, proto));
            }
            req.setData(data2);
        }
        catch (Throwable t2) {
            req.setBroken(true);
            req.setData(t2);
        }
        return req;
    }
    
    protected Object getRequestData(final long id) {
        final DefaultFuture future = DefaultFuture.getFuture(id);
        if (future == null) {
            return null;
        }
        final Request req = future.getRequest();
        if (req == null) {
            return null;
        }
        return req.getData();
    }
    
    protected void encodeRequest(final Channel channel, final ChannelBuffer buffer, final Request req) throws IOException {
        final Serialization serialization = this.getSerialization(channel, req);
        final byte[] header = new byte[16];
        Bytes.short2bytes((short)(-9541), header);
        header[2] = (byte)(0xFFFFFF80 | serialization.getContentTypeId());
        if (req.isTwoWay()) {
            final byte[] array = header;
            final int n = 2;
            array[n] |= 0x40;
        }
        if (req.isEvent()) {
            final byte[] array2 = header;
            final int n2 = 2;
            array2[n2] |= 0x20;
        }
        Bytes.long2bytes(req.getId(), header, 4);
        final int savedWriteIndex = buffer.writerIndex();
        buffer.writerIndex(savedWriteIndex + 16);
        final ChannelBufferOutputStream bos = new ChannelBufferOutputStream(buffer);
        if (req.isHeartbeat()) {
            bos.write(CodecSupport.getNullBytesOf(serialization));
        }
        else {
            final ObjectOutput out = serialization.serialize(channel.getUrl(), (OutputStream)bos);
            if (req.isEvent()) {
                this.encodeEventData(channel, out, req.getData());
            }
            else {
                this.encodeRequestData(channel, out, req.getData(), req.getVersion());
            }
            out.flushBuffer();
            if (out instanceof Cleanable) {
                ((Cleanable)out).cleanup();
            }
        }
        bos.flush();
        bos.close();
        final int len = bos.writtenBytes();
        AbstractCodec.checkPayload(channel, len);
        Bytes.int2bytes(len, header, 12);
        buffer.writerIndex(savedWriteIndex);
        buffer.writeBytes(header);
        buffer.writerIndex(savedWriteIndex + 16 + len);
    }
    
    protected void encodeResponse(final Channel channel, final ChannelBuffer buffer, final Response res) throws IOException {
        final int savedWriteIndex = buffer.writerIndex();
        try {
            final Serialization serialization = this.getSerialization(channel, res);
            final byte[] header = new byte[16];
            Bytes.short2bytes((short)(-9541), header);
            header[2] = serialization.getContentTypeId();
            if (res.isHeartbeat()) {
                final byte[] array = header;
                final int n = 2;
                array[n] |= 0x20;
            }
            final byte status = res.getStatus();
            header[3] = status;
            Bytes.long2bytes(res.getId(), header, 4);
            buffer.writerIndex(savedWriteIndex + 16);
            final ChannelBufferOutputStream bos = new ChannelBufferOutputStream(buffer);
            if (status == 20) {
                if (res.isHeartbeat()) {
                    bos.write(CodecSupport.getNullBytesOf(serialization));
                }
                else {
                    final ObjectOutput out = serialization.serialize(channel.getUrl(), (OutputStream)bos);
                    if (res.isEvent()) {
                        this.encodeEventData(channel, out, res.getResult());
                    }
                    else {
                        this.encodeResponseData(channel, out, res.getResult(), res.getVersion());
                    }
                    out.flushBuffer();
                    if (out instanceof Cleanable) {
                        ((Cleanable)out).cleanup();
                    }
                }
            }
            else {
                final ObjectOutput out = serialization.serialize(channel.getUrl(), (OutputStream)bos);
                out.writeUTF(res.getErrorMessage());
                out.flushBuffer();
                if (out instanceof Cleanable) {
                    ((Cleanable)out).cleanup();
                }
            }
            bos.flush();
            bos.close();
            final int len = bos.writtenBytes();
            AbstractCodec.checkPayload(channel, len);
            Bytes.int2bytes(len, header, 12);
            buffer.writerIndex(savedWriteIndex);
            buffer.writeBytes(header);
            buffer.writerIndex(savedWriteIndex + 16 + len);
        }
        catch (Throwable t) {
            buffer.writerIndex(savedWriteIndex);
            Label_0603: {
                if (!res.isEvent() && res.getStatus() != 50) {
                    final Response r = new Response(res.getId(), res.getVersion());
                    r.setStatus((byte)50);
                    if (t instanceof ExceedPayloadLimitException) {
                        ExchangeCodec.logger.warn(t.getMessage(), t);
                        try {
                            r.setErrorMessage(t.getMessage());
                            channel.send(r);
                            return;
                        }
                        catch (RemotingException e) {
                            ExchangeCodec.logger.warn("Failed to send bad_response info back: " + t.getMessage() + ", cause: " + e.getMessage(), (Throwable)e);
                            break Label_0603;
                        }
                    }
                    ExchangeCodec.logger.warn("Fail to encode response: " + res + ", send bad_response info instead, cause: " + t.getMessage(), t);
                    try {
                        r.setErrorMessage("Failed to send response: " + res + ", cause: " + StringUtils.toString(t));
                        channel.send(r);
                        return;
                    }
                    catch (RemotingException e) {
                        ExchangeCodec.logger.warn("Failed to send bad_response info back: " + res + ", cause: " + e.getMessage(), (Throwable)e);
                    }
                }
            }
            if (t instanceof IOException) {
                throw (IOException)t;
            }
            if (t instanceof RuntimeException) {
                throw (RuntimeException)t;
            }
            if (t instanceof Error) {
                throw (Error)t;
            }
            throw new RuntimeException(t.getMessage(), t);
        }
    }
    
    @Override
    protected Object decodeData(final ObjectInput in) throws IOException {
        return this.decodeRequestData(in);
    }
    
    protected Object decodeRequestData(final ObjectInput in) throws IOException {
        try {
            return in.readObject();
        }
        catch (ClassNotFoundException e) {
            throw new IOException(StringUtils.toString("Read object failed.", (Throwable)e));
        }
    }
    
    protected Object decodeResponseData(final ObjectInput in) throws IOException {
        try {
            return in.readObject();
        }
        catch (ClassNotFoundException e) {
            throw new IOException(StringUtils.toString("Read object failed.", (Throwable)e));
        }
    }
    
    @Override
    protected void encodeData(final ObjectOutput out, final Object data) throws IOException {
        this.encodeRequestData(out, data);
    }
    
    private void encodeEventData(final ObjectOutput out, final Object data) throws IOException {
        out.writeEvent(data);
    }
    
    @Deprecated
    protected void encodeHeartbeatData(final ObjectOutput out, final Object data) throws IOException {
        this.encodeEventData(out, data);
    }
    
    protected void encodeRequestData(final ObjectOutput out, final Object data) throws IOException {
        out.writeObject(data);
    }
    
    protected void encodeResponseData(final ObjectOutput out, final Object data) throws IOException {
        out.writeObject(data);
    }
    
    @Override
    protected Object decodeData(final Channel channel, final ObjectInput in) throws IOException {
        return this.decodeRequestData(channel, in);
    }
    
    protected Object decodeEventData(final Channel channel, final ObjectInput in, final byte[] eventBytes) throws IOException {
        try {
            if (eventBytes != null) {
                final int dataLen = eventBytes.length;
                final int threshold = ConfigurationUtils.getSystemConfiguration().getInt("deserialization.event.size", 50);
                if (dataLen > threshold) {
                    throw new IllegalArgumentException("Event data too long, actual size " + threshold + ", threshold " + threshold + " rejected for security consideration.");
                }
            }
            return in.readEvent();
        }
        catch (IOException | ClassNotFoundException ex2) {
            final Exception ex;
            final Exception e = ex;
            throw new IOException(StringUtils.toString("Decode dubbo protocol event failed.", (Throwable)e));
        }
    }
    
    protected Object decodeRequestData(final Channel channel, final ObjectInput in) throws IOException {
        return this.decodeRequestData(in);
    }
    
    protected Object decodeResponseData(final Channel channel, final ObjectInput in) throws IOException {
        return this.decodeResponseData(in);
    }
    
    protected Object decodeResponseData(final Channel channel, final ObjectInput in, final Object requestData) throws IOException {
        return this.decodeResponseData(channel, in);
    }
    
    @Override
    protected void encodeData(final Channel channel, final ObjectOutput out, final Object data) throws IOException {
        this.encodeRequestData(channel, out, data);
    }
    
    private void encodeEventData(final Channel channel, final ObjectOutput out, final Object data) throws IOException {
        this.encodeEventData(out, data);
    }
    
    @Deprecated
    protected void encodeHeartbeatData(final Channel channel, final ObjectOutput out, final Object data) throws IOException {
        this.encodeHeartbeatData(out, data);
    }
    
    protected void encodeRequestData(final Channel channel, final ObjectOutput out, final Object data) throws IOException {
        this.encodeRequestData(out, data);
    }
    
    protected void encodeResponseData(final Channel channel, final ObjectOutput out, final Object data) throws IOException {
        this.encodeResponseData(out, data);
    }
    
    protected void encodeRequestData(final Channel channel, final ObjectOutput out, final Object data, final String version) throws IOException {
        this.encodeRequestData(out, data);
    }
    
    protected void encodeResponseData(final Channel channel, final ObjectOutput out, final Object data, final String version) throws IOException {
        this.encodeResponseData(out, data);
    }
    
    private Object finishRespWhenOverPayload(final Channel channel, final long size, final byte[] header) {
        final int payload = AbstractCodec.getPayload(channel);
        final boolean overPayload = AbstractCodec.isOverPayload(payload, size);
        if (overPayload) {
            final long reqId = Bytes.bytes2long(header, 4);
            final byte flag = header[2];
            if ((flag & 0xFFFFFF80) == 0x0) {
                final Response res = new Response(reqId);
                if ((flag & 0x20) != 0x0) {
                    res.setEvent(true);
                }
                final byte status = header[3];
                res.setStatus((byte)90);
                final String errorMsg = "Data length too large: " + size + ", max payload: " + payload + ", channel: " + channel;
                ExchangeCodec.logger.error(errorMsg);
                res.setErrorMessage(errorMsg);
                return res;
            }
        }
        return null;
    }
    
    static {
        MAGIC_HIGH = Bytes.short2bytes((short)(-9541))[0];
        MAGIC_LOW = Bytes.short2bytes((short)(-9541))[1];
        logger = LoggerFactory.getLogger((Class)ExchangeCodec.class);
    }
}
