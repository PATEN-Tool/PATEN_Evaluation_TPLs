// 
// Decompiled by Procyon v0.5.36
// 

package org.elasticsearch.common.io;

import java.util.Collections;
import org.elasticsearch.common.joda.time.DateTimeFieldType;
import org.elasticsearch.common.collect.ImmutableMap;
import java.util.List;
import org.elasticsearch.common.jackson.core.JsonLocation;
import java.net.SocketAddress;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.Inet6Address;
import org.elasticsearch.common.collect.IdentityHashSet;
import java.io.NotSerializableException;
import org.elasticsearch.common.Classes;
import java.io.EOFException;
import java.io.ObjectStreamClass;
import java.io.StreamCorruptedException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.io.ObjectInputStream;

public class ThrowableObjectInputStream extends ObjectInputStream
{
    private final ClassLoader classLoader;
    private static final Set<Class<?>> CLASS_WHITELIST;
    private static final Set<Package> PKG_WHITELIST;
    
    public ThrowableObjectInputStream(final InputStream in) throws IOException {
        this(in, null);
    }
    
    public ThrowableObjectInputStream(final InputStream in, final ClassLoader classLoader) throws IOException {
        super(in);
        this.classLoader = classLoader;
    }
    
    @Override
    protected void readStreamHeader() throws IOException, StreamCorruptedException {
        final int version = this.readByte() & 0xFF;
        if (version != 5) {
            throw new StreamCorruptedException("Unsupported version: " + version);
        }
    }
    
    @Override
    protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
        final int type = this.read();
        if (type < 0) {
            throw new EOFException();
        }
        switch (type) {
            case 2: {
                return ObjectStreamClass.lookup(Exception.class);
            }
            case 3: {
                return ObjectStreamClass.lookup(StackTraceElement.class);
            }
            case 0: {
                return this.verify(super.readClassDescriptor());
            }
            case 1: {
                final String className = this.readUTF();
                final Class<?> clazz = this.loadClass(className);
                return this.verify(ObjectStreamClass.lookup(clazz));
            }
            default: {
                throw new StreamCorruptedException("Unexpected class descriptor type: " + type);
            }
        }
    }
    
    @Override
    protected Class<?> resolveClass(final ObjectStreamClass desc) throws IOException, ClassNotFoundException {
        final String className = desc.getName();
        try {
            return this.loadClass(className);
        }
        catch (ClassNotFoundException ex) {
            return super.resolveClass(desc);
        }
    }
    
    protected Class<?> loadClass(final String className) throws ClassNotFoundException {
        ClassLoader classLoader = this.classLoader;
        if (classLoader == null) {
            classLoader = Classes.getDefaultClassLoader();
        }
        Class<?> clazz;
        if (classLoader != null) {
            clazz = classLoader.loadClass(className);
        }
        else {
            clazz = Class.forName(className);
        }
        return clazz;
    }
    
    private ObjectStreamClass verify(final ObjectStreamClass streamClass) throws IOException, ClassNotFoundException {
        final Class<?> aClass = this.resolveClass(streamClass);
        final Package pkg = aClass.getPackage();
        if (aClass.isPrimitive() || aClass.isArray() || Throwable.class.isAssignableFrom(aClass) || ThrowableObjectInputStream.CLASS_WHITELIST.contains(aClass) || ThrowableObjectInputStream.PKG_WHITELIST.contains(aClass.getPackage()) || pkg.getName().startsWith("org.elasticsearch")) {
            return streamClass;
        }
        throw new NotSerializableException(aClass.getName());
    }
    
    static {
        final IdentityHashSet<Class<?>> classes = new IdentityHashSet<Class<?>>();
        classes.add(String.class);
        classes.add(Inet6Address.class);
        classes.add(Inet4Address.class);
        classes.add(InetAddress.class);
        classes.add(InetSocketAddress.class);
        classes.add(SocketAddress.class);
        classes.add(StackTraceElement.class);
        classes.add(JsonLocation.class);
        final IdentityHashSet<Package> packages = new IdentityHashSet<Package>();
        packages.add(Integer.class.getPackage());
        packages.add(List.class.getPackage());
        packages.add(ImmutableMap.class.getPackage());
        packages.add(DateTimeFieldType.class.getPackage());
        CLASS_WHITELIST = Collections.unmodifiableSet((Set<? extends Class<?>>)classes);
        PKG_WHITELIST = Collections.unmodifiableSet((Set<? extends Package>)packages);
    }
}
