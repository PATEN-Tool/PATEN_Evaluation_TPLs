// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.dubbo.remoting.transport;

import java.util.Iterator;
import java.util.Set;
import java.util.HashMap;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.serialize.ObjectInput;
import java.io.InputStream;
import java.io.IOException;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.serialize.Serialization;
import java.util.Map;
import org.apache.dubbo.common.logger.Logger;

public class CodecSupport
{
    private static final Logger logger;
    private static Map<Byte, Serialization> ID_SERIALIZATION_MAP;
    private static Map<Byte, String> ID_SERIALIZATIONNAME_MAP;
    
    private CodecSupport() {
    }
    
    public static Serialization getSerializationById(final Byte id) {
        return CodecSupport.ID_SERIALIZATION_MAP.get(id);
    }
    
    public static Serialization getSerialization(final URL url) {
        return (Serialization)ExtensionLoader.getExtensionLoader((Class)Serialization.class).getExtension(url.getParameter("serialization", "hessian2"));
    }
    
    public static Serialization getSerialization(final URL url, final Byte id) throws IOException {
        final Serialization serialization = getSerializationById(id);
        final String serializationName = url.getParameter("serialization", "hessian2");
        if (serialization == null || ((id == 3 || id == 7 || id == 4) && !serializationName.equals(CodecSupport.ID_SERIALIZATIONNAME_MAP.get(id)))) {
            throw new IOException("Unexpected serialization id:" + id + " received from network, please check if the peer send the right id.");
        }
        return serialization;
    }
    
    public static ObjectInput deserialize(final URL url, final InputStream is, final byte proto) throws IOException {
        final Serialization s = getSerialization(url, proto);
        return s.deserialize(url, is);
    }
    
    static {
        logger = LoggerFactory.getLogger((Class)CodecSupport.class);
        CodecSupport.ID_SERIALIZATION_MAP = new HashMap<Byte, Serialization>();
        CodecSupport.ID_SERIALIZATIONNAME_MAP = new HashMap<Byte, String>();
        final Set<String> supportedExtensions = (Set<String>)ExtensionLoader.getExtensionLoader((Class)Serialization.class).getSupportedExtensions();
        for (final String name : supportedExtensions) {
            final Serialization serialization = (Serialization)ExtensionLoader.getExtensionLoader((Class)Serialization.class).getExtension(name);
            final byte idByte = serialization.getContentTypeId();
            if (CodecSupport.ID_SERIALIZATION_MAP.containsKey(idByte)) {
                CodecSupport.logger.error("Serialization extension " + serialization.getClass().getName() + " has duplicate id to Serialization extension " + CodecSupport.ID_SERIALIZATION_MAP.get(idByte).getClass().getName() + ", ignore this Serialization extension");
            }
            else {
                CodecSupport.ID_SERIALIZATION_MAP.put(idByte, serialization);
                CodecSupport.ID_SERIALIZATIONNAME_MAP.put(idByte, name);
            }
        }
    }
}
