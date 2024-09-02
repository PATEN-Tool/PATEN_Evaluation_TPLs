// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.syncope.core.provisioning.java.jexl;

import org.slf4j.LoggerFactory;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.apache.syncope.core.provisioning.api.DerAttrHandler;
import java.util.List;
import java.util.Iterator;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import java.util.Collection;
import java.lang.reflect.Field;
import java.beans.PropertyDescriptor;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.Any;
import java.beans.IntrospectionException;
import org.apache.syncope.core.provisioning.api.utils.FormatUtils;
import java.util.Date;
import org.apache.commons.lang3.ArrayUtils;
import java.beans.Introspector;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JxltEngine;
import java.util.Map;
import java.util.Collections;
import org.apache.commons.jexl3.introspection.JexlUberspect;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlEngine;
import org.slf4j.Logger;

public final class JexlUtils
{
    private static final Logger LOG;
    private static final String[] IGNORE_FIELDS;
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
    
    public static JexlContext addFieldsToContext(final Object object, final JexlContext jexlContext) {
        final JexlContext context = (JexlContext)((jexlContext == null) ? new MapContext() : jexlContext);
        try {
            for (final PropertyDescriptor desc : Introspector.getBeanInfo(object.getClass()).getPropertyDescriptors()) {
                final Class<?> type = desc.getPropertyType();
                final String fieldName = desc.getName();
                if (!fieldName.startsWith("pc") && !ArrayUtils.contains((Object[])JexlUtils.IGNORE_FIELDS, (Object)fieldName) && !Iterable.class.isAssignableFrom(type) && !type.isArray()) {
                    try {
                        Object fieldValue;
                        if (desc.getReadMethod() == null) {
                            final Field field = object.getClass().getDeclaredField(fieldName);
                            field.setAccessible(true);
                            fieldValue = field.get(object);
                        }
                        else {
                            fieldValue = desc.getReadMethod().invoke(object, new Object[0]);
                        }
                        context.set(fieldName, (fieldValue == null) ? "" : (type.equals(Date.class) ? FormatUtils.format((Date)fieldValue, false) : fieldValue));
                        JexlUtils.LOG.debug("Add field {} with value {}", (Object)fieldName, fieldValue);
                    }
                    catch (Exception iae) {
                        JexlUtils.LOG.error("Reading '{}' value error", (Object)fieldName, (Object)iae);
                    }
                }
            }
        }
        catch (IntrospectionException ie) {
            JexlUtils.LOG.error("Reading class attributes error", (Throwable)ie);
        }
        if (object instanceof Any) {
            final Any<?> any = (Any<?>)object;
            if (any.getRealm() != null) {
                context.set("realm", (Object)any.getRealm().getFullPath());
            }
        }
        else if (object instanceof Realm) {
            final Realm realm = (Realm)object;
            context.set("fullPath", (Object)realm.getFullPath());
        }
        return context;
    }
    
    public static void addPlainAttrsToContext(final Collection<? extends PlainAttr<?>> attrs, final JexlContext jexlContext) {
        for (final PlainAttr<?> attr : attrs) {
            if (attr.getSchema() != null) {
                final List<String> attrValues = (List<String>)attr.getValuesAsStrings();
                final String expressionValue = attrValues.isEmpty() ? "" : attrValues.get(0);
                JexlUtils.LOG.debug("Add attribute {} with value {}", (Object)attr.getSchema().getKey(), (Object)expressionValue);
                jexlContext.set(attr.getSchema().getKey(), (Object)expressionValue);
            }
        }
    }
    
    public static void addDerAttrsToContext(final Any<?> any, final JexlContext jexlContext) {
        final Map<DerSchema, String> derAttrs = (Map<DerSchema, String>)((DerAttrHandler)ApplicationContextProvider.getBeanFactory().getBean((Class)DerAttrHandler.class)).getValues((Any)any);
        for (final Map.Entry<DerSchema, String> entry : derAttrs.entrySet()) {
            jexlContext.set(entry.getKey().getKey(), (Object)entry.getValue());
        }
    }
    
    public static boolean evaluateMandatoryCondition(final String mandatoryCondition, final Any<?> any) {
        final JexlContext jexlContext = (JexlContext)new MapContext();
        addPlainAttrsToContext(any.getPlainAttrs(), jexlContext);
        addDerAttrsToContext(any, jexlContext);
        return Boolean.parseBoolean(evaluate(mandatoryCondition, jexlContext));
    }
    
    public static String evaluate(final String expression, final AnyTO anyTO, final JexlContext context) {
        addFieldsToContext(anyTO, context);
        for (final AttrTO plainAttr : anyTO.getPlainAttrs()) {
            final List<String> values = (List<String>)plainAttr.getValues();
            final String expressionValue = values.isEmpty() ? "" : values.get(0);
            JexlUtils.LOG.debug("Add plain attribute {} with value {}", (Object)plainAttr.getSchema(), (Object)expressionValue);
            context.set(plainAttr.getSchema(), (Object)expressionValue);
        }
        for (final AttrTO derAttr : anyTO.getDerAttrs()) {
            final List<String> values = (List<String>)derAttr.getValues();
            final String expressionValue = values.isEmpty() ? "" : values.get(0);
            JexlUtils.LOG.debug("Add derived attribute {} with value {}", (Object)derAttr.getSchema(), (Object)expressionValue);
            context.set(derAttr.getSchema(), (Object)expressionValue);
        }
        for (final AttrTO virAttr : anyTO.getVirAttrs()) {
            final List<String> values = (List<String>)virAttr.getValues();
            final String expressionValue = values.isEmpty() ? "" : values.get(0);
            JexlUtils.LOG.debug("Add virtual attribute {} with value {}", (Object)virAttr.getSchema(), (Object)expressionValue);
            context.set(virAttr.getSchema(), (Object)expressionValue);
        }
        return evaluate(expression, context);
    }
    
    private JexlUtils() {
    }
    
    static {
        LOG = LoggerFactory.getLogger((Class)JexlUtils.class);
        IGNORE_FIELDS = new String[] { "password", "clearPassword", "serialVersionUID", "class" };
    }
}
