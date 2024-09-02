// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.activemq.transport.xstream;

import org.apache.activemq.transport.stomp.XStreamSupport;
import java.io.IOException;
import org.apache.activemq.command.MessageDispatch;
import org.apache.activemq.command.MarshallAware;
import org.apache.activemq.command.TransientInitializer;
import java.io.Reader;
import org.apache.activemq.wireformat.WireFormat;
import com.thoughtworks.xstream.XStream;
import org.apache.activemq.transport.util.TextWireFormat;

public class XStreamWireFormat extends TextWireFormat
{
    private XStream xStream;
    private int version;
    
    @Override
    public int getVersion() {
        return this.version;
    }
    
    @Override
    public void setVersion(final int version) {
        this.version = version;
    }
    
    public WireFormat copy() {
        return new XStreamWireFormat();
    }
    
    @Override
    public Object unmarshalText(final String text) {
        return this.getXStream().fromXML(text);
    }
    
    @Override
    public Object unmarshalText(final Reader reader) {
        final Object val = this.getXStream().fromXML(reader);
        if (val instanceof TransientInitializer) {
            ((TransientInitializer)val).initTransients();
        }
        return val;
    }
    
    @Override
    public String marshalText(final Object command) throws IOException {
        if (command instanceof MarshallAware) {
            ((MarshallAware)command).beforeMarshall(this);
        }
        else if (command instanceof MessageDispatch) {
            final MessageDispatch dispatch = (MessageDispatch)command;
            if (dispatch != null && dispatch.getMessage() != null) {
                dispatch.getMessage().beforeMarshall(this);
            }
        }
        return this.getXStream().toXML(command);
    }
    
    public boolean canProcessWireFormatVersion(final int version) {
        return true;
    }
    
    public int getCurrentWireFormatVersion() {
        return 1;
    }
    
    public XStream getXStream() {
        if (this.xStream == null) {
            (this.xStream = this.createXStream()).setClassLoader(this.getClass().getClassLoader());
        }
        return this.xStream;
    }
    
    public void setXStream(final XStream xStream) {
        this.xStream = xStream;
    }
    
    protected XStream createXStream() {
        final XStream xstream = XStreamSupport.createXStream();
        xstream.ignoreUnknownElements();
        return xstream;
    }
}
