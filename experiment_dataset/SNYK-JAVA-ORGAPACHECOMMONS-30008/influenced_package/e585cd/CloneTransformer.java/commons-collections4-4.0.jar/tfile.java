// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.commons.collections4.functors;

import java.io.Serializable;
import org.apache.commons.collections4.Transformer;

public class CloneTransformer<T> implements Transformer<T, T>, Serializable
{
    private static final long serialVersionUID = -8188742709499652567L;
    public static final Transformer INSTANCE;
    
    public static <T> Transformer<T, T> cloneTransformer() {
        return (Transformer<T, T>)CloneTransformer.INSTANCE;
    }
    
    private CloneTransformer() {
    }
    
    public T transform(final T input) {
        if (input == null) {
            return null;
        }
        return PrototypeFactory.prototypeFactory(input).create();
    }
    
    private Object readResolve() {
        return CloneTransformer.INSTANCE;
    }
    
    static {
        INSTANCE = new CloneTransformer();
    }
}
