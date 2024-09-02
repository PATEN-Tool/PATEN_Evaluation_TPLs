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
import java.util.List;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.common.lib.to.AttrTO;
import java.util.Collection;
import java.lang.reflect.Field;
import java.util.Iterator;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.provisioning.api.utils.FormatUtils;
import java.util.Date;
import java.beans.IntrospectionException;
import org.apache.commons.lang3.ArrayUtils;
import java.beans.Introspector;
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
import java.beans.PropertyDescriptor;
import java.util.Set;
import java.util.Map;
import org.slf4j.Logger;

public final class JexlUtils
{
    private static final Logger LOG;
    private static final String[] IGNORE_FIELDS;
    private static final Map<Class<?>, Set<PropertyDescriptor>> FIELD_CACHE;
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
        Set<PropertyDescriptor> cached = JexlUtils.FIELD_CACHE.get(object.getClass());
        if (cached == null) {
            cached = new HashSet<PropertyDescriptor>();
            JexlUtils.FIELD_CACHE.put(object.getClass(), cached);
            try {
                for (final PropertyDescriptor desc : Introspector.getBeanInfo(object.getClass()).getPropertyDescriptors()) {
                    if (!desc.getName().startsWith("pc") && !ArrayUtils.contains((Object[])JexlUtils.IGNORE_FIELDS, (Object)desc.getName()) && !Iterable.class.isAssignableFrom(desc.getPropertyType()) && !desc.getPropertyType().isArray()) {
                        cached.add(desc);
                    }
                }
            }
            catch (IntrospectionException ie) {
                JexlUtils.LOG.error("Reading class attributes error", (Throwable)ie);
            }
        }
        for (final PropertyDescriptor desc2 : cached) {
            final String fieldName = desc2.getName();
            final Class<?> fieldType = desc2.getPropertyType();
            try {
                Object fieldValue;
                if (desc2.getReadMethod() == null) {
                    final Field field = object.getClass().getDeclaredField(fieldName);
                    field.setAccessible(true);
                    fieldValue = field.get(object);
                }
                else {
                    fieldValue = desc2.getReadMethod().invoke(object, new Object[0]);
                }
                fieldValue = ((fieldValue == null) ? "" : (fieldType.equals(Date.class) ? FormatUtils.format((Date)fieldValue, false) : fieldValue));
                jexlContext.set(fieldName, fieldValue);
                JexlUtils.LOG.debug("Add field {} with value {}", (Object)fieldName, fieldValue);
            }
            catch (Exception iae) {
                JexlUtils.LOG.error("Reading '{}' value error", (Object)fieldName, (Object)iae);
            }
        }
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
        for (final AttrTO attr : attrs) {
            if (attr.getSchema() != null) {
                final String expressionValue = attr.getValues().isEmpty() ? "" : attr.getValues().get(0);
                JexlUtils.LOG.debug("Add attribute {} with value {}", (Object)attr.getSchema(), (Object)expressionValue);
                jexlContext.set(attr.getSchema(), (Object)expressionValue);
            }
        }
    }
    
    public static void addPlainAttrsToContext(final Collection<? extends PlainAttr<?>> attrs, final JexlContext jexlContext) {
        final List<String> attrValues;
        final String expressionValue;
        attrs.stream().filter(attr -> attr.getSchema() != null).forEachOrdered(attr -> {
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
        FIELD_CACHE = Collections.synchronizedMap(new HashMap<Class<?>, Set<PropertyDescriptor>>());
    }
}
