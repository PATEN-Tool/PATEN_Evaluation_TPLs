// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.logging.log4j.core.net.server;

import java.util.Collection;
import org.apache.logging.log4j.core.util.FilteredObjectInputStream;
import java.io.InputStream;
import java.io.IOException;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LogEventListener;
import java.util.Collections;
import java.util.List;
import java.io.ObjectInputStream;

public class ObjectInputStreamLogEventBridge extends AbstractLogEventBridge<ObjectInputStream>
{
    private final List<String> allowedClasses;
    
    public ObjectInputStreamLogEventBridge() {
        this(Collections.emptyList());
    }
    
    public ObjectInputStreamLogEventBridge(final List<String> allowedClasses) {
        this.allowedClasses = allowedClasses;
    }
    
    @Override
    public void logEvents(final ObjectInputStream inputStream, final LogEventListener logEventListener) throws IOException {
        try {
            logEventListener.log((LogEvent)inputStream.readObject());
        }
        catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
    }
    
    @Override
    public ObjectInputStream wrapStream(final InputStream inputStream) throws IOException {
        return new FilteredObjectInputStream(inputStream, this.allowedClasses);
    }
}
