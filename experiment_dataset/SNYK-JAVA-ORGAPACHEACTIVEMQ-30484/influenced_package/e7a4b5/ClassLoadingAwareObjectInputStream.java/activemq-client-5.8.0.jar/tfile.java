// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.activemq.util;

import java.lang.reflect.Proxy;
import java.io.ObjectStreamClass;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.io.ObjectInputStream;

public class ClassLoadingAwareObjectInputStream extends ObjectInputStream
{
    private static final ClassLoader FALLBACK_CLASS_LOADER;
    private static final HashMap<String, Class> primClasses;
    private final ClassLoader inLoader;
    
    public ClassLoadingAwareObjectInputStream(final InputStream in) throws IOException {
        super(in);
        this.inLoader = in.getClass().getClassLoader();
    }
    
    @Override
    protected Class<?> resolveClass(final ObjectStreamClass classDesc) throws IOException, ClassNotFoundException {
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        return this.load(classDesc.getName(), cl, this.inLoader);
    }
    
    @Override
    protected Class<?> resolveProxyClass(final String[] interfaces) throws IOException, ClassNotFoundException {
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        final Class[] cinterfaces = new Class[interfaces.length];
        for (int i = 0; i < interfaces.length; ++i) {
            cinterfaces[i] = this.load(interfaces[i], cl);
        }
        try {
            return Proxy.getProxyClass(cl, (Class<?>[])cinterfaces);
        }
        catch (IllegalArgumentException e) {
            try {
                return Proxy.getProxyClass(this.inLoader, (Class<?>[])cinterfaces);
            }
            catch (IllegalArgumentException e2) {
                try {
                    return Proxy.getProxyClass(ClassLoadingAwareObjectInputStream.FALLBACK_CLASS_LOADER, (Class<?>[])cinterfaces);
                }
                catch (IllegalArgumentException e3) {
                    throw new ClassNotFoundException(null, e);
                }
            }
        }
    }
    
    private Class<?> load(final String className, final ClassLoader... cl) throws ClassNotFoundException {
        final ClassLoader[] arr$ = cl;
        final int len$ = arr$.length;
        int i$ = 0;
        while (i$ < len$) {
            final ClassLoader loader = arr$[i$];
            try {
                return Class.forName(className, false, loader);
            }
            catch (ClassNotFoundException e) {
                ++i$;
                continue;
            }
            break;
        }
        final Class<?> clazz = ClassLoadingAwareObjectInputStream.primClasses.get(className);
        if (clazz != null) {
            return clazz;
        }
        return Class.forName(className, false, ClassLoadingAwareObjectInputStream.FALLBACK_CLASS_LOADER);
    }
    
    static {
        FALLBACK_CLASS_LOADER = ClassLoadingAwareObjectInputStream.class.getClassLoader();
        (primClasses = new HashMap<String, Class>(8, 1.0f)).put("boolean", Boolean.TYPE);
        ClassLoadingAwareObjectInputStream.primClasses.put("byte", Byte.TYPE);
        ClassLoadingAwareObjectInputStream.primClasses.put("char", Character.TYPE);
        ClassLoadingAwareObjectInputStream.primClasses.put("short", Short.TYPE);
        ClassLoadingAwareObjectInputStream.primClasses.put("int", Integer.TYPE);
        ClassLoadingAwareObjectInputStream.primClasses.put("long", Long.TYPE);
        ClassLoadingAwareObjectInputStream.primClasses.put("float", Float.TYPE);
        ClassLoadingAwareObjectInputStream.primClasses.put("double", Double.TYPE);
        ClassLoadingAwareObjectInputStream.primClasses.put("void", Void.TYPE);
    }
}
