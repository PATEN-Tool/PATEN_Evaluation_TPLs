// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.dubbo.remoting.transport;

import java.util.Iterator;
import java.util.Set;
import java.util.HashMap;
import org.apache.dubbo.common.logger.LoggerFactory;
import java.util.List;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceRepository;
import java.util.Collection;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.rpc.model.ApplicationModel;
import java.util.Arrays;
import org.apache.dubbo.common.serialize.ObjectOutput;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
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
    private static Map<String, Byte> SERIALIZATIONNAME_ID_MAP;
    private static Map<Byte, byte[]> ID_NULLBYTES_MAP;
    private static final ThreadLocal<byte[]> TL_BUFFER;
    
    private CodecSupport() {
    }
    
    public static Serialization getSerializationById(final Byte id) {
        return CodecSupport.ID_SERIALIZATION_MAP.get(id);
    }
    
    public static Byte getIDByName(final String name) {
        return CodecSupport.SERIALIZATIONNAME_ID_MAP.get(name);
    }
    
    public static Serialization getSerialization(final URL url) {
        return (Serialization)ExtensionLoader.getExtensionLoader((Class)Serialization.class).getExtension(url.getParameter("serialization", "hessian2"));
    }
    
    public static Serialization getSerialization(final URL url, final Byte id) throws IOException {
        final Serialization result = getSerializationById(id);
        if (result == null) {
            throw new IOException("Unrecognized serialize type from consumer: " + id);
        }
        return result;
    }
    
    public static ObjectInput deserialize(final URL url, final InputStream is, final byte proto) throws IOException {
        final Serialization s = getSerialization(url, proto);
        return s.deserialize(url, is);
    }
    
    public static byte[] getNullBytesOf(final Serialization s) {
        final ByteArrayOutputStream baos;
        byte[] nullBytes;
        ObjectOutput out;
        return CodecSupport.ID_NULLBYTES_MAP.computeIfAbsent(s.getContentTypeId(), k -> {
            baos = new ByteArrayOutputStream();
            nullBytes = new byte[0];
            try {
                out = s.serialize((URL)null, (OutputStream)baos);
                out.writeObject((Object)null);
                out.flushBuffer();
                nullBytes = baos.toByteArray();
                baos.close();
            }
            catch (Exception e) {
                CodecSupport.logger.warn("Serialization extension " + s.getClass().getName() + " not support serializing null object, return an empty bytes instead.");
            }
            return nullBytes;
        });
    }
    
    public static byte[] getPayload(final InputStream is) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final byte[] buffer = getBuffer(is.available());
        int len;
        while ((len = is.read(buffer)) > -1) {
            baos.write(buffer, 0, len);
        }
        baos.flush();
        return baos.toByteArray();
    }
    
    private static byte[] getBuffer(final int size) {
        final byte[] bytes = CodecSupport.TL_BUFFER.get();
        if (size <= bytes.length) {
            return bytes;
        }
        return new byte[size];
    }
    
    public static boolean isHeartBeat(final byte[] payload, final byte proto) {
        return Arrays.equals(payload, getNullBytesOf(getSerializationById(proto)));
    }
    
    public static void checkSerialization(final String path, final String version, final Byte id) throws IOException {
        final ServiceRepository repository = ApplicationModel.getServiceRepository();
        final ProviderModel providerModel = repository.lookupExportedServiceWithoutGroup(path + ":" + version);
        if (providerModel == null) {
            if (CodecSupport.logger.isWarnEnabled()) {
                CodecSupport.logger.warn("Serialization security check is enabled but cannot work as expected because there's no matched provider model for path " + path + ", version " + version);
            }
        }
        else {
            final List<URL> urls = (List<URL>)providerModel.getServiceConfig().getExportedUrls();
            if (CollectionUtils.isNotEmpty((Collection)urls)) {
                final URL url = urls.get(0);
                final String serializationName = url.getParameter("serialization", "hessian2");
                final Byte localId = CodecSupport.SERIALIZATIONNAME_ID_MAP.get(serializationName);
                if (localId != null && !localId.equals(id)) {
                    throw new IOException("Unexpected serialization id:" + id + " received from network, please check if the peer send the right id.");
                }
            }
        }
    }
    
    static {
        logger = LoggerFactory.getLogger((Class)CodecSupport.class);
        CodecSupport.ID_SERIALIZATION_MAP = new HashMap<Byte, Serialization>();
        CodecSupport.ID_SERIALIZATIONNAME_MAP = new HashMap<Byte, String>();
        CodecSupport.SERIALIZATIONNAME_ID_MAP = new HashMap<String, Byte>();
        CodecSupport.ID_NULLBYTES_MAP = new HashMap<Byte, byte[]>();
        TL_BUFFER = ThreadLocal.withInitial(() -> new byte[1024]);
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
                CodecSupport.SERIALIZATIONNAME_ID_MAP.put(name, idByte);
            }
        }
    }
}
