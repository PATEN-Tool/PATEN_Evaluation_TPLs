// 
// Decompiled by Procyon v0.5.36
// 

package jodd.json;

import jodd.typeconverter.TypeConverterManager;
import java.util.HashMap;
import java.lang.reflect.InvocationTargetException;
import jodd.introspector.Setter;
import jodd.introspector.PropertyDescriptor;
import java.util.Iterator;
import jodd.introspector.ClassDescriptor;
import jodd.util.ClassUtil;
import java.util.List;
import jodd.introspector.ClassIntrospector;
import jodd.util.ClassLoaderUtil;
import java.util.Map;

public class MapToBean
{
    protected boolean declared;
    protected final JsonParserBase jsonParser;
    protected final String classMetadataName;
    
    public MapToBean(final JsonParserBase jsonParser, final String classMetadataName) {
        this.declared = true;
        this.jsonParser = jsonParser;
        this.classMetadataName = classMetadataName;
    }
    
    public Object map2bean(final Map map, Class targetType) {
        Object target = null;
        final String className = map.get(this.classMetadataName);
        if (className == null) {
            if (targetType == null) {
                target = map;
            }
        }
        else {
            try {
                targetType = ClassLoaderUtil.loadClass(className);
            }
            catch (ClassNotFoundException cnfex) {
                throw new JsonException(cnfex);
            }
        }
        if (target == null) {
            target = this.jsonParser.newObjectInstance(targetType);
        }
        final ClassDescriptor cd = ClassIntrospector.get().lookup((Class)target.getClass());
        final boolean targetIsMap = target instanceof Map;
        for (final Object key : map.keySet()) {
            final String keyName = key.toString();
            if (this.classMetadataName != null && keyName.equals(this.classMetadataName)) {
                continue;
            }
            final PropertyDescriptor pd = cd.getPropertyDescriptor(keyName, this.declared);
            if (!targetIsMap && pd == null) {
                continue;
            }
            Object value = map.get(key);
            final Class propertyType = (pd == null) ? null : pd.getType();
            final Class componentType = (pd == null) ? null : pd.resolveComponentType(true);
            if (value != null) {
                if (value instanceof List) {
                    if (componentType != null && componentType != String.class) {
                        value = this.generifyList((List)value, componentType);
                    }
                }
                else if (value instanceof Map) {
                    if (!ClassUtil.isTypeOf(propertyType, (Class)Map.class)) {
                        value = this.map2bean((Map)value, propertyType);
                    }
                    else {
                        final Class keyType = (pd == null) ? null : pd.resolveKeyType(true);
                        if (keyType != String.class || componentType != String.class) {
                            value = this.generifyMap((Map<Object, Object>)value, (Class<Object>)keyType, (Class<Object>)componentType);
                        }
                    }
                }
            }
            if (targetIsMap) {
                ((Map)target).put(keyName, value);
            }
            else {
                try {
                    this.setValue(target, pd, value);
                }
                catch (Exception ignore) {
                    ignore.printStackTrace();
                }
            }
        }
        return target;
    }
    
    private Object generifyList(final List list, final Class componentType) {
        for (int i = 0; i < list.size(); ++i) {
            final Object element = list.get(i);
            if (element != null) {
                if (element instanceof Map) {
                    final Object bean = this.map2bean((Map)element, componentType);
                    list.set(i, bean);
                }
                else {
                    final Object value = this.convert(element, componentType);
                    list.set(i, value);
                }
            }
        }
        return list;
    }
    
    private void setValue(final Object target, final PropertyDescriptor pd, Object value) throws InvocationTargetException, IllegalAccessException {
        final Setter setter = pd.getSetter(true);
        if (setter != null) {
            if (value != null) {
                final Class propertyType = setter.getSetterRawType();
                value = this.jsonParser.convertType(value, propertyType);
            }
            setter.invokeSetter(target, value);
        }
    }
    
    protected <K, V> Map<K, V> generifyMap(final Map<Object, Object> map, final Class<K> keyType, final Class<V> valueType) {
        if (keyType == String.class) {
            for (final Map.Entry<Object, Object> entry : map.entrySet()) {
                final Object value = entry.getValue();
                final Object newValue = this.convert(value, valueType);
                if (value != newValue) {
                    entry.setValue(newValue);
                }
            }
            return (Map<K, V>)map;
        }
        final Map<K, V> newMap = new HashMap<K, V>(map.size());
        for (final Map.Entry<Object, Object> entry2 : map.entrySet()) {
            final Object key = entry2.getKey();
            final Object newKey = this.convert(key, keyType);
            final Object value2 = entry2.getValue();
            final Object newValue2 = this.convert(value2, valueType);
            newMap.put((K)newKey, (V)newValue2);
        }
        return newMap;
    }
    
    protected Object convert(final Object value, final Class targetType) {
        final Class valueClass = value.getClass();
        if (valueClass == targetType) {
            return value;
        }
        if (value instanceof Map) {
            if (targetType == Map.class) {
                return value;
            }
            return this.map2bean((Map)value, targetType);
        }
        else {
            try {
                return TypeConverterManager.get().convertType(value, targetType);
            }
            catch (Exception ex) {
                throw new JsonException("Type conversion failed", ex);
            }
        }
    }
}
