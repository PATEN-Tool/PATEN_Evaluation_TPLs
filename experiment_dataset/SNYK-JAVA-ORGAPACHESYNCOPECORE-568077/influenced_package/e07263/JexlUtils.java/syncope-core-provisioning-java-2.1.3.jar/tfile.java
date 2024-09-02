// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.syncope.core.provisioning.java.jexl;

import java.util.HashMap;
import org.slf4j.LoggerFactory;
import org.apache.commons.jexl3.MapContext;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.apache.syncope.core.provisioning.api.DerAttrHandler;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.common.lib.to.AttrTO;
import java.util.List;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.provisioning.api.utils.FormatUtils;
import java.util.Date;
import java.beans.IntrospectionException;
import java.util.Collection;
import org.apache.commons.lang3.ArrayUtils;
import java.beans.Introspector;
import org.apache.commons.lang3.ClassUtils;
import java.util.HashSet;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JxltEngine;
import java.util.Collections;
import org.apache.commons.jexl3.introspection.JexlUberspect;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlEngine;
import java.lang.reflect.Field;
import java.beans.PropertyDescriptor;
import org.apache.commons.lang3.tuple.Pair;
import java.util.Set;
import java.util.Map;
import org.slf4j.Logger;

public final class JexlUtils
{
    private static final Logger LOG;
    private static final String[] IGNORE_FIELDS;
    private static final Map<Class<?>, Set<Pair<PropertyDescriptor, Field>>> FIELD_CACHE;
    private static JexlEngine JEXL_ENGINE;
    
    private static JexlEngine getEngine() {
        synchronized (JexlUtils.LOG) {
            if (JexlUtils.JEXL_ENGINE == null) {
                JexlUtils.JEXL_ENGINE = new JexlBuilder().uberspect((JexlUberspect)new ClassFreeUberspect()).loader((ClassLoader)new EmptyClassLoader()).namespaces((Map)Collections.singletonMap("syncope", new SyncopeJexlFunctions())).cache(512).silent(false).strict(false).create();
            }
        }
        return JexlUtils.JEXL_ENGINE;
    }
    
    public static JxltEngine newJxltEngine() {
        return getEngine().createJxltEngine(false);
    }
    
    public static boolean isExpressionValid(final String expression) {
        boolean result;
        try {
            getEngine().createExpression(expression);
            result = true;
        }
        catch (JexlException e) {
            JexlUtils.LOG.error("Invalid jexl expression: " + expression, (Throwable)e);
            result = false;
        }
        return result;
    }
    
    public static String evaluate(final String expression, final JexlContext jexlContext) {
        String result = "";
        if (StringUtils.isNotBlank((CharSequence)expression) && jexlContext != null) {
            try {
                final JexlExpression jexlExpression = getEngine().createExpression(expression);
                final Object evaluated = jexlExpression.evaluate(jexlContext);
                if (evaluated != null) {
                    result = evaluated.toString();
                }
            }
            catch (Exception e) {
                JexlUtils.LOG.error("Error while evaluating JEXL expression: " + expression, (Throwable)e);
            }
        }
        else {
            JexlUtils.LOG.debug("Expression not provided or invalid context");
        }
        return result;
    }
    
    public static void addFieldsToContext(final Object object, final JexlContext jexlContext) {
        Set<Pair<PropertyDescriptor, Field>> cached = JexlUtils.FIELD_CACHE.get(object.getClass());
        if (cached == null) {
            JexlUtils.FIELD_CACHE.put(object.getClass(), new HashSet<Pair<PropertyDescriptor, Field>>());
            final List<Class<?>> classes = (List<Class<?>>)ClassUtils.getAllSuperclasses((Class)object.getClass());
            classes.add(object.getClass());
            final PropertyDescriptor[] array;
            int length;
            int i = 0;
            PropertyDescriptor desc;
            Field field;
            final Exception ex;
            Exception e;
            classes.forEach(clazz -> {
                try {
                    Introspector.getBeanInfo(clazz).getPropertyDescriptors();
                    for (length = array.length; i < length; ++i) {
                        desc = array[i];
                        if (!desc.getName().startsWith("pc") && !ArrayUtils.contains((Object[])JexlUtils.IGNORE_FIELDS, (Object)desc.getName()) && !Collection.class.isAssignableFrom(desc.getPropertyType()) && !Map.class.isAssignableFrom(desc.getPropertyType()) && !desc.getPropertyType().isArray()) {
                            field = null;
                            try {
                                field = clazz.getDeclaredField(desc.getName());
                            }
                            catch (NoSuchFieldException | SecurityException ex2) {
                                e = ex;
                                JexlUtils.LOG.debug("Could not get field {} from {}", new Object[] { desc.getName(), clazz.getName(), e });
                            }
                            JexlUtils.FIELD_CACHE.get(object.getClass()).add((Pair<PropertyDescriptor, Field>)Pair.of((Object)desc, (Object)field));
                        }
                    }
                }
                catch (IntrospectionException e2) {
                    JexlUtils.LOG.warn("Could not introspect {}", (Object)clazz.getName(), (Object)e2);
                }
                return;
            });
            cached = JexlUtils.FIELD_CACHE.get(object.getClass());
        }
        final String fieldName;
        final Class<?> fieldType;
        Object fieldValue;
        Object fieldValue2;
        cached.forEach(fd -> {
            fieldName = ((PropertyDescriptor)fd.getLeft()).getName();
            fieldType = ((PropertyDescriptor)fd.getLeft()).getPropertyType();
            try {
                fieldValue = null;
                if (((PropertyDescriptor)fd.getLeft()).getReadMethod() == null) {
                    if (fd.getRight() != null) {
                        ((Field)fd.getRight()).setAccessible(true);
                        fieldValue = ((Field)fd.getRight()).get(object);
                    }
                }
                else {
                    fieldValue = ((PropertyDescriptor)fd.getLeft()).getReadMethod().invoke(object, new Object[0]);
                }
                fieldValue2 = ((fieldValue == null) ? "" : (fieldType.equals(Date.class) ? FormatUtils.format((Date)fieldValue, false) : fieldValue));
                jexlContext.set(fieldName, fieldValue2);
                JexlUtils.LOG.debug("Add field {} with value {}", (Object)fieldName, fieldValue2);
            }
            catch (Exception iae) {
                JexlUtils.LOG.error("Reading '{}' value error", (Object)fieldName, (Object)iae);
            }
            return;
        });
        if (object instanceof Any && ((Any)object).getRealm() != null) {
            jexlContext.set("realm", (Object)((Any)object).getRealm().getFullPath());
        }
        else if (object instanceof AnyTO && ((AnyTO)object).getRealm() != null) {
            jexlContext.set("realm", (Object)((AnyTO)object).getRealm());
        }
        else if (object instanceof Realm) {
            jexlContext.set("fullPath", (Object)((Realm)object).getFullPath());
        }
        else if (object instanceof RealmTO) {
            jexlContext.set("fullPath", (Object)((RealmTO)object).getFullPath());
        }
    }
    
    public static void addAttrTOsToContext(final Collection<AttrTO> attrs, final JexlContext jexlContext) {
        final String expressionValue;
        attrs.stream().filter(attr -> attr.getSchema() != null).forEach(attr -> {
            expressionValue = (String)(attr.getValues().isEmpty() ? "" : attr.getValues().get(0));
            JexlUtils.LOG.debug("Add attribute {} with value {}", (Object)attr.getSchema(), (Object)expressionValue);
            jexlContext.set(attr.getSchema(), (Object)expressionValue);
        });
    }
    
    public static void addPlainAttrsToContext(final Collection<? extends PlainAttr<?>> attrs, final JexlContext jexlContext) {
        final List<String> attrValues;
        final String expressionValue;
        attrs.stream().filter(attr -> attr.getSchema() != null).forEach(attr -> {
            attrValues = (List<String>)attr.getValuesAsStrings();
            expressionValue = (attrValues.isEmpty() ? "" : attrValues.get(0));
            JexlUtils.LOG.debug("Add attribute {} with value {}", (Object)attr.getSchema().getKey(), (Object)expressionValue);
            jexlContext.set(attr.getSchema().getKey(), (Object)expressionValue);
        });
    }
    
    public static void addDerAttrsToContext(final Any<?> any, final JexlContext jexlContext) {
        final Map<DerSchema, String> derAttrs = (Map<DerSchema, String>)((DerAttrHandler)ApplicationContextProvider.getBeanFactory().getBean((Class)DerAttrHandler.class)).getValues((Any)any);
        derAttrs.forEach((schema, value) -> jexlContext.set(schema.getKey(), (Object)value));
    }
    
    public static boolean evaluateMandatoryCondition(final String mandatoryCondition, final Any<?> any) {
        final JexlContext jexlContext = (JexlContext)new MapContext();
        addPlainAttrsToContext(any.getPlainAttrs(), jexlContext);
        addDerAttrsToContext(any, jexlContext);
        return Boolean.parseBoolean(evaluate(mandatoryCondition, jexlContext));
    }
    
    private JexlUtils() {
    }
    
    static {
        LOG = LoggerFactory.getLogger((Class)JexlUtils.class);
        IGNORE_FIELDS = new String[] { "password", "clearPassword", "serialVersionUID", "class" };
        FIELD_CACHE = Collections.synchronizedMap(new HashMap<Class<?>, Set<Pair<PropertyDescriptor, Field>>>());
    }
}
