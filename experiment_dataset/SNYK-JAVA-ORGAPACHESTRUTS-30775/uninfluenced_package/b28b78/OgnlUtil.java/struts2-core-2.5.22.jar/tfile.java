// 
// Decompiled by Procyon v0.5.36
// 

package com.opensymphony.xwork2.ognl;

import org.apache.logging.log4j.LogManager;
import ognl.MemberAccess;
import com.opensymphony.xwork2.ognl.accessor.CompoundRootAccessor;
import java.beans.Introspector;
import java.lang.reflect.Method;
import java.beans.PropertyDescriptor;
import java.util.HashMap;
import ognl.SimpleNode;
import ognl.OgnlException;
import java.beans.IntrospectionException;
import ognl.OgnlContext;
import com.opensymphony.xwork2.util.CompoundRoot;
import ognl.ClassResolver;
import com.opensymphony.xwork2.util.reflection.ReflectionException;
import java.util.Map;
import ognl.OgnlRuntime;
import ognl.Ognl;
import java.util.Iterator;
import com.opensymphony.xwork2.config.ConfigurationException;
import com.opensymphony.xwork2.util.TextParseUtil;
import java.util.Collection;
import org.apache.commons.lang3.BooleanUtils;
import com.opensymphony.xwork2.inject.Inject;
import com.opensymphony.xwork2.conversion.impl.XWorkConverter;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import com.opensymphony.xwork2.inject.Container;
import java.util.regex.Pattern;
import java.util.Set;
import ognl.TypeConverter;
import java.beans.BeanInfo;
import java.util.concurrent.ConcurrentMap;
import org.apache.logging.log4j.Logger;

public class OgnlUtil
{
    private static final Logger LOG;
    private final ConcurrentMap<String, Object> expressions;
    private final ConcurrentMap<Class, BeanInfo> beanInfoCache;
    private TypeConverter defaultConverter;
    private boolean devMode;
    private boolean enableExpressionCache;
    private boolean enableEvalExpression;
    private Set<Class<?>> excludedClasses;
    private Set<Pattern> excludedPackageNamePatterns;
    private Set<String> excludedPackageNames;
    private Container container;
    private boolean allowStaticMethodAccess;
    private boolean disallowProxyMemberAccess;
    
    public OgnlUtil() {
        this.expressions = new ConcurrentHashMap<String, Object>();
        this.beanInfoCache = new ConcurrentHashMap<Class, BeanInfo>();
        this.enableExpressionCache = true;
        this.excludedClasses = new HashSet<Class<?>>();
        this.excludedPackageNamePatterns = new HashSet<Pattern>();
        this.excludedPackageNames = new HashSet<String>();
        this.excludedClasses = Collections.unmodifiableSet((Set<? extends Class<?>>)this.excludedClasses);
        this.excludedPackageNamePatterns = Collections.unmodifiableSet((Set<? extends Pattern>)this.excludedPackageNamePatterns);
        this.excludedPackageNames = Collections.unmodifiableSet((Set<? extends String>)this.excludedPackageNames);
    }
    
    @Inject
    protected void setXWorkConverter(final XWorkConverter conv) {
        this.defaultConverter = (TypeConverter)new OgnlTypeConverterWrapper(conv);
    }
    
    @Inject("devMode")
    protected void setDevMode(final String mode) {
        this.devMode = BooleanUtils.toBoolean(mode);
    }
    
    @Inject("enableOGNLExpressionCache")
    protected void setEnableExpressionCache(final String cache) {
        this.enableExpressionCache = BooleanUtils.toBoolean(cache);
    }
    
    @Inject(value = "enableOGNLEvalExpression", required = false)
    protected void setEnableEvalExpression(final String evalExpression) {
        this.enableEvalExpression = BooleanUtils.toBoolean(evalExpression);
        if (this.enableEvalExpression) {
            OgnlUtil.LOG.warn("Enabling OGNL expression evaluation may introduce security risks (see http://struts.apache.org/release/2.3.x/docs/s2-013.html for further details)");
        }
    }
    
    @Inject(value = "ognlExcludedClasses", required = false)
    protected void setExcludedClasses(final String commaDelimitedClasses) {
        final Set<Class<?>> excludedClasses = new HashSet<Class<?>>();
        excludedClasses.addAll(this.excludedClasses);
        excludedClasses.addAll(this.parseExcludedClasses(commaDelimitedClasses));
        this.excludedClasses = Collections.unmodifiableSet((Set<? extends Class<?>>)excludedClasses);
    }
    
    private Set<Class<?>> parseExcludedClasses(final String commaDelimitedClasses) {
        final Set<String> classNames = TextParseUtil.commaDelimitedStringToSet(commaDelimitedClasses);
        final Set<Class<?>> classes = new HashSet<Class<?>>();
        for (final String className : classNames) {
            try {
                classes.add(Class.forName(className));
            }
            catch (ClassNotFoundException e) {
                throw new ConfigurationException("Cannot load excluded class: " + className, e);
            }
        }
        return classes;
    }
    
    @Inject(value = "ognlExcludedPackageNamePatterns", required = false)
    protected void setExcludedPackageNamePatterns(final String commaDelimitedPackagePatterns) {
        final Set<Pattern> excludedPackageNamePatterns = new HashSet<Pattern>();
        excludedPackageNamePatterns.addAll(this.excludedPackageNamePatterns);
        excludedPackageNamePatterns.addAll(this.parseExcludedPackageNamePatterns(commaDelimitedPackagePatterns));
        this.excludedPackageNamePatterns = Collections.unmodifiableSet((Set<? extends Pattern>)excludedPackageNamePatterns);
    }
    
    private Set<Pattern> parseExcludedPackageNamePatterns(final String commaDelimitedPackagePatterns) {
        final Set<String> packagePatterns = TextParseUtil.commaDelimitedStringToSet(commaDelimitedPackagePatterns);
        final Set<Pattern> packageNamePatterns = new HashSet<Pattern>();
        for (final String pattern : packagePatterns) {
            packageNamePatterns.add(Pattern.compile(pattern));
        }
        return packageNamePatterns;
    }
    
    @Inject(value = "ognlExcludedPackageNames", required = false)
    protected void setExcludedPackageNames(final String commaDelimitedPackageNames) {
        final Set<String> excludedPackageNames = new HashSet<String>();
        excludedPackageNames.addAll(this.excludedPackageNames);
        excludedPackageNames.addAll(this.parseExcludedPackageNames(commaDelimitedPackageNames));
        this.excludedPackageNames = Collections.unmodifiableSet((Set<? extends String>)excludedPackageNames);
    }
    
    private Set<String> parseExcludedPackageNames(final String commaDelimitedPackageNames) {
        return TextParseUtil.commaDelimitedStringToSet(commaDelimitedPackageNames);
    }
    
    public Set<Class<?>> getExcludedClasses() {
        return this.excludedClasses;
    }
    
    public Set<Pattern> getExcludedPackageNamePatterns() {
        return this.excludedPackageNamePatterns;
    }
    
    public Set<String> getExcludedPackageNames() {
        return this.excludedPackageNames;
    }
    
    @Inject
    protected void setContainer(final Container container) {
        this.container = container;
    }
    
    @Inject(value = "allowStaticMethodAccess", required = false)
    protected void setAllowStaticMethodAccess(final String allowStaticMethodAccess) {
        this.allowStaticMethodAccess = BooleanUtils.toBoolean(allowStaticMethodAccess);
    }
    
    @Inject(value = "struts.disallowProxyMemberAccess", required = false)
    protected void setDisallowProxyMemberAccess(final String disallowProxyMemberAccess) {
        this.disallowProxyMemberAccess = Boolean.parseBoolean(disallowProxyMemberAccess);
    }
    
    @Inject(value = "struts.ognl.expressionMaxLength", required = false)
    protected void applyExpressionMaxLength(final String maxLength) {
        try {
            if (maxLength == null || maxLength.isEmpty()) {
                Ognl.applyExpressionMaxLength((Integer)null);
            }
            else {
                Ognl.applyExpressionMaxLength(Integer.valueOf(Integer.parseInt(maxLength)));
            }
        }
        catch (Exception ex) {
            OgnlUtil.LOG.error("Unable to set OGNL Expression Max Length {}.", (Object)maxLength);
            throw ex;
        }
    }
    
    public boolean isDisallowProxyMemberAccess() {
        return this.disallowProxyMemberAccess;
    }
    
    public static void clearRuntimeCache() {
        OgnlRuntime.clearCache();
    }
    
    public void clearExpressionCache() {
        this.expressions.clear();
    }
    
    public int expressionCacheSize() {
        return this.expressions.size();
    }
    
    public void clearBeanInfoCache() {
        this.beanInfoCache.clear();
    }
    
    public int beanInfoCacheSize() {
        return this.beanInfoCache.size();
    }
    
    public void setProperties(final Map<String, ?> props, final Object o, final Map<String, Object> context) {
        this.setProperties(props, o, context, false);
    }
    
    public void setProperties(final Map<String, ?> props, final Object o, final Map<String, Object> context, final boolean throwPropertyExceptions) throws ReflectionException {
        if (props == null) {
            return;
        }
        Ognl.setTypeConverter((Map)context, this.getTypeConverterFromContext(context));
        final Object oldRoot = Ognl.getRoot((Map)context);
        Ognl.setRoot((Map)context, o);
        for (final Map.Entry<String, ?> entry : props.entrySet()) {
            final String expression = entry.getKey();
            this.internalSetProperty(expression, entry.getValue(), o, context, throwPropertyExceptions);
        }
        Ognl.setRoot((Map)context, oldRoot);
    }
    
    public void setProperties(final Map<String, ?> properties, final Object o) {
        this.setProperties(properties, o, false);
    }
    
    public void setProperties(final Map<String, ?> properties, final Object o, final boolean throwPropertyExceptions) {
        final Map context = this.createDefaultContext(o, null);
        this.setProperties(properties, o, context, throwPropertyExceptions);
    }
    
    public void setProperty(final String name, final Object value, final Object o, final Map<String, Object> context) {
        this.setProperty(name, value, o, context, false);
    }
    
    public void setProperty(final String name, final Object value, final Object o, final Map<String, Object> context, final boolean throwPropertyExceptions) {
        Ognl.setTypeConverter((Map)context, this.getTypeConverterFromContext(context));
        final Object oldRoot = Ognl.getRoot((Map)context);
        Ognl.setRoot((Map)context, o);
        this.internalSetProperty(name, value, o, context, throwPropertyExceptions);
        Ognl.setRoot((Map)context, oldRoot);
    }
    
    public Object getRealTarget(final String property, final Map<String, Object> context, final Object root) throws OgnlException {
        if ("top".equals(property)) {
            return root;
        }
        if (root instanceof CompoundRoot) {
            final CompoundRoot cr = (CompoundRoot)root;
            try {
                for (final Object target : cr) {
                    if (OgnlRuntime.hasSetProperty((OgnlContext)context, target, (Object)property) || OgnlRuntime.hasGetProperty((OgnlContext)context, target, (Object)property) || OgnlRuntime.getIndexedPropertyType((OgnlContext)context, (Class)target.getClass(), property) != OgnlRuntime.INDEXED_PROPERTY_NONE) {
                        return target;
                    }
                }
            }
            catch (IntrospectionException ex) {
                throw new ReflectionException("Cannot figure out real target class", ex);
            }
            return null;
        }
        return root;
    }
    
    public void setValue(final String name, final Map<String, Object> context, final Object root, final Object value) throws OgnlException {
        this.compileAndExecute(name, context, (OgnlTask<Object>)new OgnlTask<Void>() {
            @Override
            public Void execute(final Object tree) throws OgnlException {
                if (OgnlUtil.this.isEvalExpression(tree, context)) {
                    throw new OgnlException("Eval expression/chained expressions cannot be used as parameter name");
                }
                if (OgnlUtil.this.isArithmeticExpression(tree, context)) {
                    throw new OgnlException("Arithmetic expressions cannot be used as parameter name");
                }
                Ognl.setValue(tree, context, root, value);
                return null;
            }
        });
    }
    
    private boolean isEvalExpression(final Object tree, final Map<String, Object> context) throws OgnlException {
        if (tree instanceof SimpleNode) {
            final SimpleNode node = (SimpleNode)tree;
            OgnlContext ognlContext = null;
            if (context != null && context instanceof OgnlContext) {
                ognlContext = (OgnlContext)context;
            }
            return node.isEvalChain(ognlContext) || node.isSequence(ognlContext);
        }
        return false;
    }
    
    private boolean isArithmeticExpression(final Object tree, final Map<String, Object> context) throws OgnlException {
        if (tree instanceof SimpleNode) {
            final SimpleNode node = (SimpleNode)tree;
            OgnlContext ognlContext = null;
            if (context != null && context instanceof OgnlContext) {
                ognlContext = (OgnlContext)context;
            }
            return node.isOperation(ognlContext);
        }
        return false;
    }
    
    private boolean isSimpleMethod(final Object tree, final Map<String, Object> context) throws OgnlException {
        if (tree instanceof SimpleNode) {
            final SimpleNode node = (SimpleNode)tree;
            OgnlContext ognlContext = null;
            if (context != null && context instanceof OgnlContext) {
                ognlContext = (OgnlContext)context;
            }
            return node.isSimpleMethod(ognlContext) && !node.isChain(ognlContext);
        }
        return false;
    }
    
    public Object getValue(final String name, final Map<String, Object> context, final Object root) throws OgnlException {
        return this.compileAndExecute(name, context, (OgnlTask<Object>)new OgnlTask<Object>() {
            @Override
            public Object execute(final Object tree) throws OgnlException {
                return Ognl.getValue(tree, context, root);
            }
        });
    }
    
    public Object callMethod(final String name, final Map<String, Object> context, final Object root) throws OgnlException {
        return this.compileAndExecuteMethod(name, context, (OgnlTask<Object>)new OgnlTask<Object>() {
            @Override
            public Object execute(final Object tree) throws OgnlException {
                return Ognl.getValue(tree, context, root);
            }
        });
    }
    
    public Object getValue(final String name, final Map<String, Object> context, final Object root, final Class resultType) throws OgnlException {
        return this.compileAndExecute(name, context, (OgnlTask<Object>)new OgnlTask<Object>() {
            @Override
            public Object execute(final Object tree) throws OgnlException {
                return Ognl.getValue(tree, context, root, resultType);
            }
        });
    }
    
    public Object compile(final String expression) throws OgnlException {
        return this.compile(expression, null);
    }
    
    private <T> Object compileAndExecute(final String expression, final Map<String, Object> context, final OgnlTask<T> task) throws OgnlException {
        Object tree;
        if (this.enableExpressionCache) {
            tree = this.expressions.get(expression);
            if (tree == null) {
                tree = Ognl.parseExpression(expression);
                this.checkEnableEvalExpression(tree, context);
            }
        }
        else {
            tree = Ognl.parseExpression(expression);
            this.checkEnableEvalExpression(tree, context);
        }
        final T exec = task.execute(tree);
        if (this.enableExpressionCache) {
            this.expressions.putIfAbsent(expression, tree);
        }
        return exec;
    }
    
    private <T> Object compileAndExecuteMethod(final String expression, final Map<String, Object> context, final OgnlTask<T> task) throws OgnlException {
        Object tree;
        if (this.enableExpressionCache) {
            tree = this.expressions.get(expression);
            if (tree == null) {
                tree = Ognl.parseExpression(expression);
                this.checkSimpleMethod(tree, context);
            }
        }
        else {
            tree = Ognl.parseExpression(expression);
            this.checkSimpleMethod(tree, context);
        }
        final T exec = task.execute(tree);
        if (this.enableExpressionCache) {
            this.expressions.putIfAbsent(expression, tree);
        }
        return exec;
    }
    
    public Object compile(final String expression, final Map<String, Object> context) throws OgnlException {
        return this.compileAndExecute(expression, context, (OgnlTask<Object>)new OgnlTask<Object>() {
            @Override
            public Object execute(final Object tree) throws OgnlException {
                return tree;
            }
        });
    }
    
    private void checkEnableEvalExpression(final Object tree, final Map<String, Object> context) throws OgnlException {
        if (!this.enableEvalExpression && this.isEvalExpression(tree, context)) {
            throw new OgnlException("Eval expressions/chained expressions have been disabled!");
        }
    }
    
    private void checkSimpleMethod(final Object tree, final Map<String, Object> context) throws OgnlException {
        if (!this.isSimpleMethod(tree, context)) {
            throw new OgnlException("It isn't a simple method which can be called!");
        }
    }
    
    public void copy(final Object from, final Object to, final Map<String, Object> context, final Collection<String> exclusions, final Collection<String> inclusions) {
        this.copy(from, to, context, exclusions, inclusions, null);
    }
    
    public void copy(final Object from, final Object to, final Map<String, Object> context, final Collection<String> exclusions, final Collection<String> inclusions, final Class<?> editable) {
        if (from == null || to == null) {
            OgnlUtil.LOG.warn("Attempting to copy from or to a null source. This is illegal and is bein skipped. This may be due to an error in an OGNL expression, action chaining, or some other event.");
            return;
        }
        final TypeConverter converter = this.getTypeConverterFromContext(context);
        final Map contextFrom = this.createDefaultContext(from, null);
        Ognl.setTypeConverter(contextFrom, converter);
        final Map contextTo = this.createDefaultContext(to, null);
        Ognl.setTypeConverter(contextTo, converter);
        PropertyDescriptor[] fromPds;
        PropertyDescriptor[] toPds;
        try {
            fromPds = this.getPropertyDescriptors(from);
            if (editable != null) {
                toPds = this.getPropertyDescriptors(editable);
            }
            else {
                toPds = this.getPropertyDescriptors(to);
            }
        }
        catch (IntrospectionException e) {
            OgnlUtil.LOG.error("An error occurred", (Throwable)e);
            return;
        }
        final Map<String, PropertyDescriptor> toPdHash = new HashMap<String, PropertyDescriptor>();
        for (final PropertyDescriptor toPd : toPds) {
            toPdHash.put(toPd.getName(), toPd);
        }
        for (final PropertyDescriptor fromPd : fromPds) {
            if (fromPd.getReadMethod() != null) {
                boolean copy = true;
                if (exclusions != null && exclusions.contains(fromPd.getName())) {
                    copy = false;
                }
                else if (inclusions != null && !inclusions.contains(fromPd.getName())) {
                    copy = false;
                }
                if (copy) {
                    final PropertyDescriptor toPd2 = toPdHash.get(fromPd.getName());
                    if (toPd2 != null && toPd2.getWriteMethod() != null) {
                        try {
                            this.compileAndExecute(fromPd.getName(), context, (OgnlTask<Object>)new OgnlTask<Object>() {
                                @Override
                                public Void execute(final Object expr) throws OgnlException {
                                    final Object value = Ognl.getValue(expr, contextFrom, from);
                                    Ognl.setValue(expr, contextTo, to, value);
                                    return null;
                                }
                            });
                        }
                        catch (OgnlException e2) {
                            OgnlUtil.LOG.debug("Got OGNL exception", (Throwable)e2);
                        }
                    }
                }
            }
        }
    }
    
    public void copy(final Object from, final Object to, final Map<String, Object> context) {
        this.copy(from, to, context, null, null);
    }
    
    public PropertyDescriptor[] getPropertyDescriptors(final Object source) throws IntrospectionException {
        final BeanInfo beanInfo = this.getBeanInfo(source);
        return beanInfo.getPropertyDescriptors();
    }
    
    public PropertyDescriptor[] getPropertyDescriptors(final Class clazz) throws IntrospectionException {
        final BeanInfo beanInfo = this.getBeanInfo(clazz);
        return beanInfo.getPropertyDescriptors();
    }
    
    public Map<String, Object> getBeanMap(final Object source) throws IntrospectionException, OgnlException {
        final Map<String, Object> beanMap = new HashMap<String, Object>();
        final Map sourceMap = this.createDefaultContext(source, null);
        final PropertyDescriptor[] arr$;
        final PropertyDescriptor[] propertyDescriptors = arr$ = this.getPropertyDescriptors(source);
        for (final PropertyDescriptor propertyDescriptor : arr$) {
            final String propertyName = propertyDescriptor.getDisplayName();
            final Method readMethod = propertyDescriptor.getReadMethod();
            if (readMethod != null) {
                final Object value = this.compileAndExecute(propertyName, null, (OgnlTask<Object>)new OgnlTask<Object>() {
                    @Override
                    public Object execute(final Object expr) throws OgnlException {
                        return Ognl.getValue(expr, sourceMap, source);
                    }
                });
                beanMap.put(propertyName, value);
            }
            else {
                beanMap.put(propertyName, "There is no read method for " + propertyName);
            }
        }
        return beanMap;
    }
    
    public BeanInfo getBeanInfo(final Object from) throws IntrospectionException {
        return this.getBeanInfo(from.getClass());
    }
    
    public BeanInfo getBeanInfo(final Class clazz) throws IntrospectionException {
        synchronized (this.beanInfoCache) {
            BeanInfo beanInfo = (BeanInfo)this.beanInfoCache.get(clazz);
            if (beanInfo == null) {
                beanInfo = Introspector.getBeanInfo(clazz, Object.class);
                this.beanInfoCache.putIfAbsent(clazz, beanInfo);
            }
            return beanInfo;
        }
    }
    
    void internalSetProperty(final String name, final Object value, final Object o, final Map<String, Object> context, final boolean throwPropertyExceptions) throws ReflectionException {
        try {
            this.setValue(name, context, o, value);
        }
        catch (OgnlException e) {
            final Throwable reason = e.getReason();
            if (reason instanceof SecurityException) {
                OgnlUtil.LOG.warn("Could not evaluate this expression due to security constraints: [{}]", (Object)name, (Object)e);
            }
            final String msg = "Caught OgnlException while setting property '" + name + "' on type '" + o.getClass().getName() + "'.";
            final Throwable exception = (Throwable)((reason == null) ? e : reason);
            if (throwPropertyExceptions) {
                throw new ReflectionException(msg, exception);
            }
            if (this.devMode) {
                OgnlUtil.LOG.warn(msg, exception);
            }
        }
    }
    
    TypeConverter getTypeConverterFromContext(final Map<String, Object> context) {
        return this.defaultConverter;
    }
    
    protected Map createDefaultContext(final Object root) {
        return this.createDefaultContext(root, null);
    }
    
    protected Map createDefaultContext(final Object root, final ClassResolver classResolver) {
        ClassResolver resolver = classResolver;
        if (resolver == null) {
            resolver = this.container.getInstance((Class<ClassResolver>)CompoundRootAccessor.class);
        }
        final SecurityMemberAccess memberAccess = new SecurityMemberAccess(this.allowStaticMethodAccess);
        memberAccess.setExcludedClasses(this.excludedClasses);
        memberAccess.setExcludedPackageNamePatterns(this.excludedPackageNamePatterns);
        memberAccess.setExcludedPackageNames(this.excludedPackageNames);
        memberAccess.setDisallowProxyMemberAccess(this.disallowProxyMemberAccess);
        return Ognl.createDefaultContext(root, resolver, this.defaultConverter, (MemberAccess)memberAccess);
    }
    
    static {
        LOG = LogManager.getLogger((Class)OgnlUtil.class);
    }
    
    private interface OgnlTask<T>
    {
        T execute(final Object p0) throws OgnlException;
    }
}
