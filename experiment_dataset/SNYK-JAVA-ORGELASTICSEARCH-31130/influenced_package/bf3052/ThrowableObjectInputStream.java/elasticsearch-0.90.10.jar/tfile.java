// 
// Decompiled by Procyon v0.5.36
// 

package org.elasticsearch.common.io;

import org.elasticsearch.common.Classes;
import java.io.EOFException;
import java.io.ObjectStreamClass;
import java.io.StreamCorruptedException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

public class ThrowableObjectInputStream extends ObjectInputStream
{
    private final ClassLoader classLoader;
    
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
                return super.readClassDescriptor();
            }
            case 1: {
                final String className = this.readUTF();
                final Class<?> clazz = this.loadClass(className);
                return ObjectStreamClass.lookup(clazz);
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
}
