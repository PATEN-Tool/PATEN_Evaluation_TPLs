// 
// Decompiled by Procyon v0.5.36
// 

package ognl;

import java.util.Enumeration;
import java.util.Set;
import java.math.BigInteger;
import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import ognl.internal.ClassCacheImpl;
import ognl.enhance.ExpressionCompiler;
import java.beans.MethodDescriptor;
import java.beans.BeanInfo;
import java.beans.IndexedPropertyDescriptor;
import java.beans.PropertyDescriptor;
import java.util.Iterator;
import java.beans.Introspector;
import java.lang.reflect.Field;
import java.util.Collections;
import java.beans.IntrospectionException;
import java.util.ArrayList;
import java.lang.reflect.Member;
import java.util.Collection;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.security.Permission;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.util.IdentityHashMap;
import ognl.enhance.OgnlExpressionCompiler;
import java.lang.reflect.Method;
import ognl.internal.ClassCache;
import java.util.Map;
import java.util.List;

public class OgnlRuntime
{
    public static final Object NotFound;
    public static final List NotFoundList;
    public static final Map NotFoundMap;
    public static final Object[] NoArguments;
    public static final Class[] NoArgumentTypes;
    public static final Object NoConversionPossible;
    public static int INDEXED_PROPERTY_NONE;
    public static int INDEXED_PROPERTY_INT;
    public static int INDEXED_PROPERTY_OBJECT;
    public static final String NULL_STRING;
    private static final String SET_PREFIX = "set";
    private static final String GET_PREFIX = "get";
    private static final String IS_PREFIX = "is";
    private static final Map HEX_PADDING;
    private static final int HEX_LENGTH = 8;
    private static final String NULL_OBJECT_STRING = "<null>";
    private static boolean _jdk15;
    private static boolean _jdkChecked;
    static final ClassCache _methodAccessors;
    static final ClassCache _propertyAccessors;
    static final ClassCache _elementsAccessors;
    static final ClassCache _nullHandlers;
    static final ClassCache _propertyDescriptorCache;
    static final ClassCache _constructorCache;
    static final ClassCache _staticMethodCache;
    static final ClassCache _instanceMethodCache;
    static final ClassCache _invokePermissionCache;
    static final ClassCache _fieldCache;
    static final List _superclasses;
    static final ClassCache[] _declaredMethods;
    static final Map _primitiveTypes;
    static final ClassCache _primitiveDefaults;
    static final Map _methodParameterTypesCache;
    static final Map _genericMethodParameterTypesCache;
    static final Map _ctorParameterTypesCache;
    static SecurityManager _securityManager;
    static final EvaluationPool _evaluationPool;
    static final ObjectArrayPool _objectArrayPool;
    static final Map<Method, Boolean> _methodAccessCache;
    static final Map<Method, Boolean> _methodPermCache;
    static final ClassPropertyMethodCache cacheSetMethod;
    static final ClassPropertyMethodCache cacheGetMethod;
    static ClassCacheInspector _cacheInspector;
    private static OgnlExpressionCompiler _compiler;
    private static final Class[] EMPTY_CLASS_ARRAY;
    private static IdentityHashMap PRIMITIVE_WRAPPER_CLASSES;
    private static final Map NUMERIC_CASTS;
    private static final Map NUMERIC_VALUES;
    private static final Map NUMERIC_LITERALS;
    private static final Map NUMERIC_DEFAULTS;
    public static final ArgsCompatbilityReport NoArgsReport;
    
    public static void clearCache() {
        OgnlRuntime._methodParameterTypesCache.clear();
        OgnlRuntime._ctorParameterTypesCache.clear();
        OgnlRuntime._propertyDescriptorCache.clear();
        OgnlRuntime._constructorCache.clear();
        OgnlRuntime._staticMethodCache.clear();
        OgnlRuntime._instanceMethodCache.clear();
        OgnlRuntime._invokePermissionCache.clear();
        OgnlRuntime._fieldCache.clear();
        OgnlRuntime._superclasses.clear();
        OgnlRuntime._declaredMethods[0].clear();
        OgnlRuntime._declaredMethods[1].clear();
        OgnlRuntime._methodAccessCache.clear();
        OgnlRuntime._methodPermCache.clear();
    }
    
    public static boolean isJdk15() {
        if (OgnlRuntime._jdkChecked) {
            return OgnlRuntime._jdk15;
        }
        try {
            Class.forName("java.lang.annotation.Annotation");
            OgnlRuntime._jdk15 = true;
        }
        catch (Exception ex) {}
        OgnlRuntime._jdkChecked = true;
        return OgnlRuntime._jdk15;
    }
    
    public static String getNumericValueGetter(final Class type) {
        return OgnlRuntime.NUMERIC_VALUES.get(type);
    }
    
    public static Class getPrimitiveWrapperClass(final Class primitiveClass) {
        return OgnlRuntime.PRIMITIVE_WRAPPER_CLASSES.get(primitiveClass);
    }
    
    public static String getNumericCast(final Class type) {
        return OgnlRuntime.NUMERIC_CASTS.get(type);
    }
    
    public static String getNumericLiteral(final Class type) {
        return OgnlRuntime.NUMERIC_LITERALS.get(type);
    }
    
    public static void setCompiler(final OgnlExpressionCompiler compiler) {
        OgnlRuntime._compiler = compiler;
    }
    
    public static OgnlExpressionCompiler getCompiler() {
        return OgnlRuntime._compiler;
    }
    
    public static void compileExpression(final OgnlContext context, final Node expression, final Object root) throws Exception {
        OgnlRuntime._compiler.compileExpression(context, expression, root);
    }
    
    public static Class getTargetClass(final Object o) {
        return (o == null) ? null : ((o instanceof Class) ? ((Class)o) : o.getClass());
    }
    
    public static String getBaseName(final Object o) {
        return (o == null) ? null : getClassBaseName(o.getClass());
    }
    
    public static String getClassBaseName(final Class c) {
        final String s = c.getName();
        return s.substring(s.lastIndexOf(46) + 1);
    }
    
    public static String getClassName(Object o, final boolean fullyQualified) {
        if (!(o instanceof Class)) {
            o = o.getClass();
        }
        return getClassName((Class)o, fullyQualified);
    }
    
    public static String getClassName(final Class c, final boolean fullyQualified) {
        return fullyQualified ? c.getName() : getClassBaseName(c);
    }
    
    public static String getPackageName(final Object o) {
        return (o == null) ? null : getClassPackageName(o.getClass());
    }
    
    public static String getClassPackageName(final Class c) {
        final String s = c.getName();
        final int i = s.lastIndexOf(46);
        return (i < 0) ? null : s.substring(0, i);
    }
    
    public static String getPointerString(final int num) {
        final StringBuffer result = new StringBuffer();
        final String hex = Integer.toHexString(num);
        final Integer l = new Integer(hex.length());
        String pad;
        if ((pad = OgnlRuntime.HEX_PADDING.get(l)) == null) {
            final StringBuffer pb = new StringBuffer();
            for (int i = hex.length(); i < 8; ++i) {
                pb.append('0');
            }
            pad = new String(pb);
            OgnlRuntime.HEX_PADDING.put(l, pad);
        }
        result.append(pad);
        result.append(hex);
        return new String(result);
    }
    
    public static String getPointerString(final Object o) {
        return getPointerString((o == null) ? 0 : System.identityHashCode(o));
    }
    
    public static String getUniqueDescriptor(Object object, final boolean fullyQualified) {
        final StringBuffer result = new StringBuffer();
        if (object != null) {
            if (object instanceof Proxy) {
                final Class interfaceClass = object.getClass().getInterfaces()[0];
                result.append(getClassName(interfaceClass, fullyQualified));
                result.append('^');
                object = Proxy.getInvocationHandler(object);
            }
            result.append(getClassName(object, fullyQualified));
            result.append('@');
            result.append(getPointerString(object));
        }
        else {
            result.append("<null>");
        }
        return new String(result);
    }
    
    public static String getUniqueDescriptor(final Object object) {
        return getUniqueDescriptor(object, false);
    }
    
    public static Object[] toArray(final List list) {
        final int size = list.size();
        Object[] result;
        if (size == 0) {
            result = OgnlRuntime.NoArguments;
        }
        else {
            result = getObjectArrayPool().create(list.size());
            for (int i = 0; i < size; ++i) {
                result[i] = list.get(i);
            }
        }
        return result;
    }
    
    public static Class[] getParameterTypes(final Method m) {
        synchronized (OgnlRuntime._methodParameterTypesCache) {
            Class[] result;
            if ((result = OgnlRuntime._methodParameterTypesCache.get(m)) == null) {
                OgnlRuntime._methodParameterTypesCache.put(m, result = m.getParameterTypes());
            }
            return result;
        }
    }
    
    public static Class[] findParameterTypes(final Class type, final Method m) {
        final Type[] genTypes = m.getGenericParameterTypes();
        Class[] types = new Class[genTypes.length];
        boolean noGenericParameter = true;
        for (int i = 0; i < genTypes.length; ++i) {
            if (!Class.class.isInstance(genTypes[i])) {
                noGenericParameter = false;
                break;
            }
            types[i] = (Class)genTypes[i];
        }
        if (noGenericParameter) {
            return types;
        }
        if (type == null || !isJdk15()) {
            return getParameterTypes(m);
        }
        final Type typeGenericSuperclass = type.getGenericSuperclass();
        if (typeGenericSuperclass == null || !ParameterizedType.class.isInstance(typeGenericSuperclass) || m.getDeclaringClass().getTypeParameters() == null) {
            return getParameterTypes(m);
        }
        if ((types = OgnlRuntime._genericMethodParameterTypesCache.get(m)) != null) {
            final ParameterizedType genericSuperclass = (ParameterizedType)typeGenericSuperclass;
            if (Arrays.equals(types, genericSuperclass.getActualTypeArguments())) {
                return types;
            }
        }
        final ParameterizedType param = (ParameterizedType)typeGenericSuperclass;
        final TypeVariable[] declaredTypes = m.getDeclaringClass().getTypeParameters();
        types = new Class[genTypes.length];
        for (int j = 0; j < genTypes.length; ++j) {
            TypeVariable paramType = null;
            if (TypeVariable.class.isInstance(genTypes[j])) {
                paramType = (TypeVariable)genTypes[j];
            }
            else if (GenericArrayType.class.isInstance(genTypes[j])) {
                paramType = (TypeVariable)((GenericArrayType)genTypes[j]).getGenericComponentType();
            }
            else {
                if (ParameterizedType.class.isInstance(genTypes[j])) {
                    types[j] = (Class)((ParameterizedType)genTypes[j]).getRawType();
                    continue;
                }
                if (Class.class.isInstance(genTypes[j])) {
                    types[j] = (Class)genTypes[j];
                    continue;
                }
            }
            Class resolved = resolveType(param, paramType, declaredTypes);
            if (resolved != null) {
                if (GenericArrayType.class.isInstance(genTypes[j])) {
                    resolved = Array.newInstance(resolved, 0).getClass();
                }
                types[j] = resolved;
            }
            else {
                types[j] = m.getParameterTypes()[j];
            }
        }
        synchronized (OgnlRuntime._genericMethodParameterTypesCache) {
            OgnlRuntime._genericMethodParameterTypesCache.put(m, types);
        }
        return types;
    }
    
    static Class resolveType(final ParameterizedType param, final TypeVariable var, final TypeVariable[] declaredTypes) {
        if (param.getActualTypeArguments().length < 1) {
            return null;
        }
        for (int i = 0; i < declaredTypes.length; ++i) {
            if (!TypeVariable.class.isInstance(param.getActualTypeArguments()[i]) && declaredTypes[i].getName().equals(var.getName())) {
                return (Class)param.getActualTypeArguments()[i];
            }
        }
        return null;
    }
    
    static Class findType(final Type[] types, final Class type) {
        for (int i = 0; i < types.length; ++i) {
            if (Class.class.isInstance(types[i]) && type.isAssignableFrom((Class)types[i])) {
                return (Class)types[i];
            }
        }
        return null;
    }
    
    public static Class[] getParameterTypes(final Constructor c) {
        Class[] result;
        if ((result = OgnlRuntime._ctorParameterTypesCache.get(c)) == null) {
            synchronized (OgnlRuntime._ctorParameterTypesCache) {
                if ((result = OgnlRuntime._ctorParameterTypesCache.get(c)) == null) {
                    OgnlRuntime._ctorParameterTypesCache.put(c, result = c.getParameterTypes());
                }
            }
        }
        return result;
    }
    
    public static SecurityManager getSecurityManager() {
        return OgnlRuntime._securityManager;
    }
    
    public static void setSecurityManager(final SecurityManager value) {
        OgnlRuntime._securityManager = value;
    }
    
    public static Permission getPermission(final Method method) {
        final Class mc = method.getDeclaringClass();
        Permission result;
        synchronized (OgnlRuntime._invokePermissionCache) {
            Map permissions = (Map)OgnlRuntime._invokePermissionCache.get(mc);
            if (permissions == null) {
                OgnlRuntime._invokePermissionCache.put(mc, permissions = new HashMap(101));
            }
            if ((result = permissions.get(method.getName())) == null) {
                result = new OgnlInvokePermission("invoke." + mc.getName() + "." + method.getName());
                permissions.put(method.getName(), result);
            }
        }
        return result;
    }
    
    public static Object invokeMethod(final Object target, final Method method, final Object[] argsArray) throws InvocationTargetException, IllegalAccessException {
        boolean syncInvoke = false;
        boolean checkPermission = false;
        synchronized (method) {
            if (OgnlRuntime._methodAccessCache.get(method) == null) {
                if (!Modifier.isPublic(method.getModifiers()) || !Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
                    if (!method.isAccessible()) {
                        OgnlRuntime._methodAccessCache.put(method, Boolean.TRUE);
                    }
                    else {
                        OgnlRuntime._methodAccessCache.put(method, Boolean.FALSE);
                    }
                }
                else {
                    OgnlRuntime._methodAccessCache.put(method, Boolean.FALSE);
                }
            }
            if (OgnlRuntime._methodAccessCache.get(method) == Boolean.TRUE) {
                syncInvoke = true;
            }
            Label_0218: {
                if (OgnlRuntime._methodPermCache.get(method) == null) {
                    if (OgnlRuntime._securityManager != null) {
                        try {
                            OgnlRuntime._securityManager.checkPermission(getPermission(method));
                            OgnlRuntime._methodPermCache.put(method, Boolean.TRUE);
                            break Label_0218;
                        }
                        catch (SecurityException ex) {
                            OgnlRuntime._methodPermCache.put(method, Boolean.FALSE);
                            throw new IllegalAccessException("Method [" + method + "] cannot be accessed.");
                        }
                    }
                    OgnlRuntime._methodPermCache.put(method, Boolean.TRUE);
                }
            }
            if (OgnlRuntime._methodPermCache.get(method) == Boolean.FALSE) {
                checkPermission = true;
            }
        }
        Object result;
        if (syncInvoke) {
            synchronized (method) {
                if (checkPermission) {
                    try {
                        OgnlRuntime._securityManager.checkPermission(getPermission(method));
                    }
                    catch (SecurityException ex2) {
                        throw new IllegalAccessException("Method [" + method + "] cannot be accessed.");
                    }
                }
                method.setAccessible(true);
                result = method.invoke(target, argsArray);
                method.setAccessible(false);
            }
        }
        else {
            if (checkPermission) {
                try {
                    OgnlRuntime._securityManager.checkPermission(getPermission(method));
                }
                catch (SecurityException ex) {
                    throw new IllegalAccessException("Method [" + method + "] cannot be accessed.");
                }
            }
            result = method.invoke(target, argsArray);
        }
        return result;
    }
    
    public static final Class getArgClass(final Object arg) {
        if (arg == null) {
            return null;
        }
        final Class c = arg.getClass();
        if (c == Boolean.class) {
            return Boolean.TYPE;
        }
        if (c.getSuperclass() == Number.class) {
            if (c == Integer.class) {
                return Integer.TYPE;
            }
            if (c == Double.class) {
                return Double.TYPE;
            }
            if (c == Byte.class) {
                return Byte.TYPE;
            }
            if (c == Long.class) {
                return Long.TYPE;
            }
            if (c == Float.class) {
                return Float.TYPE;
            }
            if (c == Short.class) {
                return Short.TYPE;
            }
        }
        else if (c == Character.class) {
            return Character.TYPE;
        }
        return c;
    }
    
    public static Class[] getArgClasses(final Object[] args) {
        if (args == null) {
            return null;
        }
        final Class[] argClasses = new Class[args.length];
        for (int i = 0; i < args.length; ++i) {
            argClasses[i] = getArgClass(args[i]);
        }
        return argClasses;
    }
    
    public static final boolean isTypeCompatible(final Object object, final Class c) {
        if (object == null) {
            return true;
        }
        final ArgsCompatbilityReport report = new ArgsCompatbilityReport(0, new boolean[1]);
        return isTypeCompatible(getArgClass(object), c, 0, report) && !report.conversionNeeded[0];
    }
    
    public static final boolean isTypeCompatible(final Class parameterClass, final Class methodArgumentClass, final int index, final ArgsCompatbilityReport report) {
        if (parameterClass == null) {
            report.score += 500;
            return true;
        }
        if (parameterClass == methodArgumentClass) {
            return true;
        }
        if (methodArgumentClass.isArray()) {
            if (parameterClass.isArray()) {
                final Class pct = parameterClass.getComponentType();
                final Class mct = methodArgumentClass.getComponentType();
                if (mct.isAssignableFrom(pct)) {
                    report.score += 25;
                    return true;
                }
            }
            if (Collection.class.isAssignableFrom(parameterClass)) {
                final Class mct2 = methodArgumentClass.getComponentType();
                if (mct2 == Object.class) {
                    report.conversionNeeded[index] = true;
                    report.score += 30;
                    return true;
                }
                return false;
            }
        }
        else if (Collection.class.isAssignableFrom(methodArgumentClass)) {
            if (parameterClass.isArray()) {
                report.conversionNeeded[index] = true;
                report.score += 50;
                return true;
            }
            if (Collection.class.isAssignableFrom(parameterClass)) {
                if (methodArgumentClass.isAssignableFrom(parameterClass)) {
                    report.score += 2;
                    return true;
                }
                report.conversionNeeded[index] = true;
                report.score += 50;
                return true;
            }
        }
        if (methodArgumentClass.isAssignableFrom(parameterClass)) {
            report.score += 40;
            return true;
        }
        if (parameterClass.isPrimitive()) {
            final Class ptc = OgnlRuntime.PRIMITIVE_WRAPPER_CLASSES.get(parameterClass);
            if (methodArgumentClass == ptc) {
                report.score += 2;
                return true;
            }
            if (methodArgumentClass.isAssignableFrom(ptc)) {
                report.score += 10;
                return true;
            }
        }
        return false;
    }
    
    public static boolean areArgsCompatible(final Object[] args, final Class[] classes) {
        final ArgsCompatbilityReport report = areArgsCompatible(getArgClasses(args), classes, null);
        if (report == null) {
            return false;
        }
        for (final boolean conversionNeeded : report.conversionNeeded) {
            if (conversionNeeded) {
                return false;
            }
        }
        return true;
    }
    
    public static ArgsCompatbilityReport areArgsCompatible(final Class[] args, final Class[] classes, final Method m) {
        final boolean varArgs = m != null && isJdk15() && m.isVarArgs();
        if (args == null || args.length == 0) {
            if (classes == null || classes.length == 0) {
                return OgnlRuntime.NoArgsReport;
            }
            return null;
        }
        else {
            if (args.length != classes.length && !varArgs) {
                return null;
            }
            if (!varArgs) {
                final ArgsCompatbilityReport report = new ArgsCompatbilityReport(0, new boolean[args.length]);
                for (int index = 0, count = args.length; index < count; ++index) {
                    if (!isTypeCompatible(args[index], classes[index], index, report)) {
                        return null;
                    }
                }
                return report;
            }
            final ArgsCompatbilityReport report = new ArgsCompatbilityReport(1000, new boolean[args.length]);
            if (classes.length - 1 > args.length) {
                return null;
            }
            for (int index = 0, count = classes.length - 1; index < count; ++index) {
                if (!isTypeCompatible(args[index], classes[index], index, report)) {
                    return null;
                }
            }
            final Class varArgsType = classes[classes.length - 1].getComponentType();
            for (int index2 = classes.length - 1, count2 = args.length; index2 < count2; ++index2) {
                if (!isTypeCompatible(args[index2], varArgsType, index2, report)) {
                    return null;
                }
            }
            return report;
        }
    }
    
    public static final boolean isMoreSpecific(final Class[] classes1, final Class[] classes2) {
        for (int index = 0, count = classes1.length; index < count; ++index) {
            final Class c1 = classes1[index];
            final Class c2 = classes2[index];
            if (c1 != c2) {
                if (c1.isPrimitive()) {
                    return true;
                }
                if (c1.isAssignableFrom(c2)) {
                    return false;
                }
                if (c2.isAssignableFrom(c1)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public static String getModifierString(final int modifiers) {
        String result;
        if (Modifier.isPublic(modifiers)) {
            result = "public";
        }
        else if (Modifier.isProtected(modifiers)) {
            result = "protected";
        }
        else if (Modifier.isPrivate(modifiers)) {
            result = "private";
        }
        else {
            result = "";
        }
        if (Modifier.isStatic(modifiers)) {
            result = "static " + result;
        }
        if (Modifier.isFinal(modifiers)) {
            result = "final " + result;
        }
        if (Modifier.isNative(modifiers)) {
            result = "native " + result;
        }
        if (Modifier.isSynchronized(modifiers)) {
            result = "synchronized " + result;
        }
        if (Modifier.isTransient(modifiers)) {
            result = "transient " + result;
        }
        return result;
    }
    
    public static Class classForName(final OgnlContext context, final String className) throws ClassNotFoundException {
        Class result = OgnlRuntime._primitiveTypes.get(className);
        if (result == null) {
            ClassResolver resolver;
            if (context == null || (resolver = context.getClassResolver()) == null) {
                resolver = OgnlContext.DEFAULT_CLASS_RESOLVER;
            }
            result = resolver.classForName(className, context);
        }
        if (result == null) {
            throw new ClassNotFoundException("Unable to resolve class: " + className);
        }
        return result;
    }
    
    public static boolean isInstance(final OgnlContext context, final Object value, final String className) throws OgnlException {
        try {
            final Class c = classForName(context, className);
            return c.isInstance(value);
        }
        catch (ClassNotFoundException e) {
            throw new OgnlException("No such class: " + className, e);
        }
    }
    
    public static Object getPrimitiveDefaultValue(final Class forClass) {
        return OgnlRuntime._primitiveDefaults.get(forClass);
    }
    
    public static Object getNumericDefaultValue(final Class forClass) {
        return OgnlRuntime.NUMERIC_DEFAULTS.get(forClass);
    }
    
    public static Object getConvertedType(final OgnlContext context, final Object target, final Member member, final String propertyName, final Object value, final Class type) {
        return context.getTypeConverter().convertValue(context, target, member, propertyName, value, type);
    }
    
    public static boolean getConvertedTypes(final OgnlContext context, final Object target, final Member member, final String propertyName, final Class[] parameterTypes, final Object[] args, final Object[] newArgs) {
        boolean result = false;
        if (parameterTypes.length == args.length) {
            result = true;
            for (int i = 0, ilast = parameterTypes.length - 1; result && i <= ilast; ++i) {
                final Object arg = args[i];
                final Class type = parameterTypes[i];
                if (isTypeCompatible(arg, type)) {
                    newArgs[i] = arg;
                }
                else {
                    final Object v = getConvertedType(context, target, member, propertyName, arg, type);
                    if (v == OgnlRuntime.NoConversionPossible) {
                        result = false;
                    }
                    else {
                        newArgs[i] = v;
                    }
                }
            }
        }
        return result;
    }
    
    public static Constructor getConvertedConstructorAndArgs(final OgnlContext context, final Object target, final List constructors, final Object[] args, final Object[] newArgs) {
        Constructor result = null;
        final TypeConverter converter = context.getTypeConverter();
        if (converter != null && constructors != null) {
            for (int i = 0, icount = constructors.size(); result == null && i < icount; ++i) {
                final Constructor ctor = constructors.get(i);
                final Class[] parameterTypes = getParameterTypes(ctor);
                if (getConvertedTypes(context, target, ctor, null, parameterTypes, args, newArgs)) {
                    result = ctor;
                }
            }
        }
        return result;
    }
    
    public static Method getAppropriateMethod(final OgnlContext context, final Object source, final Object target, final String propertyName, final String methodName, final List methods, final Object[] args, final Object[] actualArgs) {
        Method result = null;
        if (methods != null) {
            Class typeClass = (target != null) ? target.getClass() : null;
            if (typeClass == null && source != null && Class.class.isInstance(source)) {
                typeClass = (Class)source;
            }
            final Class[] argClasses = getArgClasses(args);
            final MatchingMethod mm = findBestMethod(methods, typeClass, methodName, argClasses);
            if (mm != null) {
                result = mm.mMethod;
                final Class[] mParameterTypes = mm.mParameterTypes;
                System.arraycopy(args, 0, actualArgs, 0, args.length);
                for (int j = 0; j < mParameterTypes.length; ++j) {
                    final Class type = mParameterTypes[j];
                    if (mm.report.conversionNeeded[j] || (type.isPrimitive() && actualArgs[j] == null)) {
                        actualArgs[j] = getConvertedType(context, source, result, propertyName, args[j], type);
                    }
                }
            }
        }
        if (result == null) {
            result = getConvertedMethodAndArgs(context, target, propertyName, methods, args, actualArgs);
        }
        return result;
    }
    
    public static Method getConvertedMethodAndArgs(final OgnlContext context, final Object target, final String propertyName, final List methods, final Object[] args, final Object[] newArgs) {
        Method result = null;
        final TypeConverter converter = context.getTypeConverter();
        if (converter != null && methods != null) {
            for (int i = 0, icount = methods.size(); result == null && i < icount; ++i) {
                final Method m = methods.get(i);
                final Class[] parameterTypes = findParameterTypes((target != null) ? target.getClass() : null, m);
                if (getConvertedTypes(context, target, m, propertyName, parameterTypes, args, newArgs)) {
                    result = m;
                }
            }
        }
        return result;
    }
    
    private static MatchingMethod findBestMethod(final List methods, final Class typeClass, final String name, final Class[] argClasses) {
        MatchingMethod mm = null;
        IllegalArgumentException failure = null;
        for (int i = 0, icount = methods.size(); i < icount; ++i) {
            final Method m = methods.get(i);
            final Class[] mParameterTypes = findParameterTypes(typeClass, m);
            final ArgsCompatbilityReport report = areArgsCompatible(argClasses, mParameterTypes, m);
            if (report != null) {
                final String methodName = m.getName();
                int score = report.score;
                if (!name.equals(methodName)) {
                    if (name.equalsIgnoreCase(methodName)) {
                        score += 200;
                    }
                    else if (methodName.toLowerCase().endsWith(name.toLowerCase())) {
                        score += 500;
                    }
                    else {
                        score += 5000;
                    }
                }
                if (mm == null || mm.score > score) {
                    mm = new MatchingMethod(m, score, report, mParameterTypes);
                    failure = null;
                }
                else if (mm.score == score) {
                    if (Arrays.equals(mm.mMethod.getParameterTypes(), m.getParameterTypes()) && mm.mMethod.getName().equals(m.getName())) {
                        final boolean retsAreEqual = mm.mMethod.getReturnType().equals(m.getReturnType());
                        if (mm.mMethod.getDeclaringClass().isAssignableFrom(m.getDeclaringClass())) {
                            if (!retsAreEqual && !mm.mMethod.getReturnType().isAssignableFrom(m.getReturnType())) {
                                System.err.println("Two methods with same method signature but return types conflict? \"" + mm.mMethod + "\" and \"" + m + "\" please report!");
                            }
                            mm = new MatchingMethod(m, score, report, mParameterTypes);
                            failure = null;
                        }
                        else if (!m.getDeclaringClass().isAssignableFrom(mm.mMethod.getDeclaringClass())) {
                            System.err.println("Two methods with same method signature but not providing classes assignable? \"" + mm.mMethod + "\" and \"" + m + "\" please report!");
                        }
                        else if (!retsAreEqual && !m.getReturnType().isAssignableFrom(mm.mMethod.getReturnType())) {
                            System.err.println("Two methods with same method signature but return types conflict? \"" + mm.mMethod + "\" and \"" + m + "\" please report!");
                        }
                    }
                    else if (isJdk15() && (m.isVarArgs() || mm.mMethod.isVarArgs())) {
                        if (!m.isVarArgs() || mm.mMethod.isVarArgs()) {
                            if (!m.isVarArgs() && mm.mMethod.isVarArgs()) {
                                mm = new MatchingMethod(m, score, report, mParameterTypes);
                                failure = null;
                            }
                            else {
                                System.err.println("Two vararg methods with same score(" + score + "): \"" + mm.mMethod + "\" and \"" + m + "\" please report!");
                            }
                        }
                    }
                    else {
                        int scoreCurr = 0;
                        int scoreOther = 0;
                        for (int j = 0; j < argClasses.length; ++j) {
                            final Class argClass = argClasses[j];
                            final Class mcClass = mm.mParameterTypes[j];
                            final Class moClass = mParameterTypes[j];
                            if (argClass == null) {
                                if (mcClass != moClass) {
                                    if (mcClass.isAssignableFrom(moClass)) {
                                        scoreOther += 1000;
                                    }
                                    else if (moClass.isAssignableFrom(moClass)) {
                                        scoreCurr += 1000;
                                    }
                                    else {
                                        failure = new IllegalArgumentException("Can't decide wich method to use: \"" + mm.mMethod + "\" or \"" + m + "\"");
                                    }
                                }
                            }
                            else if (mcClass != moClass) {
                                if (mcClass == argClass) {
                                    scoreOther += 100;
                                }
                                else if (moClass == argClass) {
                                    scoreCurr += 100;
                                }
                                else {
                                    failure = new IllegalArgumentException("Can't decide wich method to use: \"" + mm.mMethod + "\" or \"" + m + "\"");
                                }
                            }
                        }
                        if (scoreCurr == scoreOther) {
                            if (failure == null) {
                                System.err.println("Two methods with same score(" + score + "): \"" + mm.mMethod + "\" and \"" + m + "\" please report!");
                            }
                        }
                        else if (scoreCurr > scoreOther) {
                            mm = new MatchingMethod(m, score, report, mParameterTypes);
                            failure = null;
                        }
                    }
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
        return mm;
    }
    
    public static Object callAppropriateMethod(final OgnlContext context, final Object source, final Object target, final String methodName, final String propertyName, final List methods, final Object[] args) throws MethodFailedException {
        Throwable reason = null;
        final Object[] actualArgs = OgnlRuntime._objectArrayPool.create(args.length);
        try {
            final Method method = getAppropriateMethod(context, source, target, propertyName, methodName, methods, args, actualArgs);
            if (method == null || !isMethodAccessible(context, source, method, propertyName)) {
                final StringBuffer buffer = new StringBuffer();
                String className = "";
                if (target != null) {
                    className = target.getClass().getName() + ".";
                }
                for (int i = 0, ilast = args.length - 1; i <= ilast; ++i) {
                    final Object arg = args[i];
                    buffer.append((arg == null) ? OgnlRuntime.NULL_STRING : arg.getClass().getName());
                    if (i < ilast) {
                        buffer.append(", ");
                    }
                }
                throw new NoSuchMethodException(className + methodName + "(" + (Object)buffer + ")");
            }
            Object[] convertedArgs = actualArgs;
            if (isJdk15() && method.isVarArgs()) {
                final Class[] parmTypes = method.getParameterTypes();
                for (int i = 0; i < parmTypes.length; ++i) {
                    if (parmTypes[i].isArray()) {
                        convertedArgs = new Object[i + 1];
                        System.arraycopy(actualArgs, 0, convertedArgs, 0, convertedArgs.length);
                        Object[] varArgs;
                        if (actualArgs.length > i) {
                            final ArrayList varArgsList = new ArrayList();
                            for (int j = i; j < actualArgs.length; ++j) {
                                if (actualArgs[j] != null) {
                                    varArgsList.add(actualArgs[j]);
                                }
                            }
                            varArgs = varArgsList.toArray();
                        }
                        else {
                            varArgs = new Object[0];
                        }
                        convertedArgs[i] = varArgs;
                        break;
                    }
                }
            }
            return invokeMethod(target, method, convertedArgs);
        }
        catch (NoSuchMethodException e) {
            reason = e;
        }
        catch (IllegalAccessException e2) {
            reason = e2;
        }
        catch (InvocationTargetException e3) {
            reason = e3.getTargetException();
        }
        finally {
            OgnlRuntime._objectArrayPool.recycle(actualArgs);
        }
        throw new MethodFailedException(source, methodName, reason);
    }
    
    public static Object callStaticMethod(final OgnlContext context, final String className, final String methodName, final Object[] args) throws OgnlException {
        try {
            final Class targetClass = classForName(context, className);
            if (targetClass == null) {
                throw new ClassNotFoundException("Unable to resolve class with name " + className);
            }
            final MethodAccessor ma = getMethodAccessor(targetClass);
            return ma.callStaticMethod(context, targetClass, methodName, args);
        }
        catch (ClassNotFoundException ex) {
            throw new MethodFailedException(className, methodName, ex);
        }
    }
    
    @Deprecated
    public static Object callMethod(final OgnlContext context, final Object target, final String methodName, final String propertyName, final Object[] args) throws OgnlException {
        return callMethod(context, target, (methodName == null) ? propertyName : methodName, args);
    }
    
    public static Object callMethod(final OgnlContext context, final Object target, final String methodName, final Object[] args) throws OgnlException {
        if (target == null) {
            throw new NullPointerException("target is null for method " + methodName);
        }
        return getMethodAccessor(target.getClass()).callMethod(context, target, methodName, args);
    }
    
    public static Object callConstructor(final OgnlContext context, final String className, final Object[] args) throws OgnlException {
        Throwable reason = null;
        Object[] actualArgs = args;
        try {
            Constructor ctor = null;
            Class[] ctorParameterTypes = null;
            final Class target = classForName(context, className);
            final List constructors = getConstructors(target);
            for (int i = 0, icount = constructors.size(); i < icount; ++i) {
                final Constructor c = constructors.get(i);
                final Class[] cParameterTypes = getParameterTypes(c);
                if (areArgsCompatible(args, cParameterTypes) && (ctor == null || isMoreSpecific(cParameterTypes, ctorParameterTypes))) {
                    ctor = c;
                    ctorParameterTypes = cParameterTypes;
                }
            }
            if (ctor == null) {
                actualArgs = OgnlRuntime._objectArrayPool.create(args.length);
                if ((ctor = getConvertedConstructorAndArgs(context, target, constructors, args, actualArgs)) == null) {
                    throw new NoSuchMethodException();
                }
            }
            if (!context.getMemberAccess().isAccessible(context, target, ctor, null)) {
                throw new IllegalAccessException("access denied to " + target.getName() + "()");
            }
            return ctor.newInstance(actualArgs);
        }
        catch (ClassNotFoundException e) {
            reason = e;
        }
        catch (NoSuchMethodException e2) {
            reason = e2;
        }
        catch (IllegalAccessException e3) {
            reason = e3;
        }
        catch (InvocationTargetException e4) {
            reason = e4.getTargetException();
        }
        catch (InstantiationException e5) {
            reason = e5;
        }
        finally {
            if (actualArgs != args) {
                OgnlRuntime._objectArrayPool.recycle(actualArgs);
            }
        }
        throw new MethodFailedException(className, "new", reason);
    }
    
    @Deprecated
    public static final Object getMethodValue(final OgnlContext context, final Object target, final String propertyName) throws OgnlException, IllegalAccessException, NoSuchMethodException, IntrospectionException {
        return getMethodValue(context, target, propertyName, false);
    }
    
    public static final Object getMethodValue(final OgnlContext context, final Object target, final String propertyName, final boolean checkAccessAndExistence) throws OgnlException, IllegalAccessException, NoSuchMethodException, IntrospectionException {
        Object result = null;
        Method m = getGetMethod(context, (target == null) ? null : target.getClass(), propertyName);
        if (m == null) {
            m = getReadMethod((target == null) ? null : target.getClass(), propertyName, null);
        }
        if (checkAccessAndExistence && (m == null || !context.getMemberAccess().isAccessible(context, target, m, propertyName))) {
            result = OgnlRuntime.NotFound;
        }
        if (result == null) {
            if (m != null) {
                try {
                    result = invokeMethod(target, m, OgnlRuntime.NoArguments);
                    return result;
                }
                catch (InvocationTargetException ex) {
                    throw new OgnlException(propertyName, ex.getTargetException());
                }
            }
            throw new NoSuchMethodException(propertyName);
        }
        return result;
    }
    
    @Deprecated
    public static boolean setMethodValue(final OgnlContext context, final Object target, final String propertyName, final Object value) throws OgnlException, IllegalAccessException, NoSuchMethodException, IntrospectionException {
        return setMethodValue(context, target, propertyName, value, false);
    }
    
    public static boolean setMethodValue(final OgnlContext context, final Object target, final String propertyName, final Object value, final boolean checkAccessAndExistence) throws OgnlException, IllegalAccessException, NoSuchMethodException, IntrospectionException {
        boolean result = true;
        final Method m = getSetMethod(context, (target == null) ? null : target.getClass(), propertyName);
        if (checkAccessAndExistence && (m == null || !context.getMemberAccess().isAccessible(context, target, m, propertyName))) {
            result = false;
        }
        if (result) {
            if (m != null) {
                final Object[] args = OgnlRuntime._objectArrayPool.create(value);
                try {
                    callAppropriateMethod(context, target, target, m.getName(), propertyName, Collections.nCopies(1, m), args);
                }
                finally {
                    OgnlRuntime._objectArrayPool.recycle(args);
                }
            }
            else {
                result = false;
            }
        }
        return result;
    }
    
    public static List getConstructors(final Class targetClass) {
        List result;
        if ((result = (List)OgnlRuntime._constructorCache.get(targetClass)) == null) {
            synchronized (OgnlRuntime._constructorCache) {
                if ((result = (List)OgnlRuntime._constructorCache.get(targetClass)) == null) {
                    OgnlRuntime._constructorCache.put(targetClass, result = Arrays.asList(targetClass.getConstructors()));
                }
            }
        }
        return result;
    }
    
    public static Map getMethods(final Class targetClass, final boolean staticMethods) {
        final ClassCache cache = staticMethods ? OgnlRuntime._staticMethodCache : OgnlRuntime._instanceMethodCache;
        Map result;
        if ((result = (Map)cache.get(targetClass)) == null) {
            synchronized (cache) {
                if ((result = (Map)cache.get(targetClass)) == null) {
                    result = new HashMap(23);
                    collectMethods(targetClass, result, staticMethods);
                    cache.put(targetClass, result);
                }
            }
        }
        return result;
    }
    
    private static void collectMethods(final Class c, final Map result, final boolean staticMethods) {
        final Method[] ma = c.getDeclaredMethods();
        for (int i = 0, icount = ma.length; i < icount; ++i) {
            if (c.isInterface()) {
                if (isDefaultMethod(ma[i])) {
                    addMethodToResult(result, ma[i]);
                }
            }
            else if (isMethodCallable(ma[i])) {
                if (Modifier.isStatic(ma[i].getModifiers()) == staticMethods) {
                    addMethodToResult(result, ma[i]);
                }
            }
        }
        final Class superclass = c.getSuperclass();
        if (superclass != null) {
            collectMethods(superclass, result, staticMethods);
        }
        for (final Class iface : c.getInterfaces()) {
            collectMethods(iface, result, staticMethods);
        }
    }
    
    private static void addMethodToResult(final Map result, final Method method) {
        List ml = result.get(method.getName());
        if (ml == null) {
            result.put(method.getName(), ml = new ArrayList());
        }
        ml.add(method);
    }
    
    private static boolean isDefaultMethod(final Method method) {
        return (method.getModifiers() & 0x409) == 0x1 && method.getDeclaringClass().isInterface();
    }
    
    public static Map getAllMethods(final Class targetClass, final boolean staticMethods) {
        final ClassCache cache = staticMethods ? OgnlRuntime._staticMethodCache : OgnlRuntime._instanceMethodCache;
        Map result;
        if ((result = (Map)cache.get(targetClass)) == null) {
            synchronized (cache) {
                if ((result = (Map)cache.get(targetClass)) == null) {
                    result = new HashMap(23);
                    for (Class c = targetClass; c != null; c = c.getSuperclass()) {
                        final Method[] ma = c.getMethods();
                        for (int i = 0, icount = ma.length; i < icount; ++i) {
                            if (isMethodCallable(ma[i])) {
                                if (Modifier.isStatic(ma[i].getModifiers()) == staticMethods) {
                                    List ml = result.get(ma[i].getName());
                                    if (ml == null) {
                                        result.put(ma[i].getName(), ml = new ArrayList());
                                    }
                                    ml.add(ma[i]);
                                }
                            }
                        }
                    }
                    cache.put(targetClass, result);
                }
            }
        }
        return result;
    }
    
    public static List getMethods(final Class targetClass, final String name, final boolean staticMethods) {
        return getMethods(targetClass, staticMethods).get(name);
    }
    
    public static List getAllMethods(final Class targetClass, final String name, final boolean staticMethods) {
        return getAllMethods(targetClass, staticMethods).get(name);
    }
    
    public static Map getFields(final Class targetClass) {
        Map result;
        if ((result = (Map)OgnlRuntime._fieldCache.get(targetClass)) == null) {
            synchronized (OgnlRuntime._fieldCache) {
                if ((result = (Map)OgnlRuntime._fieldCache.get(targetClass)) == null) {
                    result = new HashMap(23);
                    final Field[] fa = targetClass.getDeclaredFields();
                    for (int i = 0; i < fa.length; ++i) {
                        result.put(fa[i].getName(), fa[i]);
                    }
                    OgnlRuntime._fieldCache.put(targetClass, result);
                }
            }
        }
        return result;
    }
    
    public static Field getField(final Class inClass, final String name) {
        Field result = null;
        Object o = getFields(inClass).get(name);
        if (o == null) {
            synchronized (OgnlRuntime._fieldCache) {
                o = getFields(inClass).get(name);
                if (o == null) {
                    OgnlRuntime._superclasses.clear();
                    for (Class sc = inClass; sc != null; sc = sc.getSuperclass()) {
                        if ((o = getFields(sc).get(name)) == OgnlRuntime.NotFound) {
                            break;
                        }
                        OgnlRuntime._superclasses.add(sc);
                        if ((result = (Field)o) != null) {
                            break;
                        }
                    }
                    for (int i = 0, icount = OgnlRuntime._superclasses.size(); i < icount; ++i) {
                        getFields(OgnlRuntime._superclasses.get(i)).put(name, (result == null) ? OgnlRuntime.NotFound : result);
                    }
                }
                else if (o instanceof Field) {
                    result = (Field)o;
                }
                else if (result == OgnlRuntime.NotFound) {
                    result = null;
                }
            }
        }
        else if (o instanceof Field) {
            result = (Field)o;
        }
        else if (result == OgnlRuntime.NotFound) {
            result = null;
        }
        return result;
    }
    
    @Deprecated
    public static Object getFieldValue(final OgnlContext context, final Object target, final String propertyName) throws NoSuchFieldException {
        return getFieldValue(context, target, propertyName, false);
    }
    
    public static Object getFieldValue(final OgnlContext context, final Object target, final String propertyName, final boolean checkAccessAndExistence) throws NoSuchFieldException {
        Object result = null;
        final Field f = getField((target == null) ? null : target.getClass(), propertyName);
        if (checkAccessAndExistence && (f == null || !context.getMemberAccess().isAccessible(context, target, f, propertyName))) {
            result = OgnlRuntime.NotFound;
        }
        if (result == null) {
            if (f == null) {
                throw new NoSuchFieldException(propertyName);
            }
            try {
                Object state = null;
                if (Modifier.isStatic(f.getModifiers())) {
                    throw new NoSuchFieldException(propertyName);
                }
                state = context.getMemberAccess().setup(context, target, f, propertyName);
                result = f.get(target);
                context.getMemberAccess().restore(context, target, f, propertyName, state);
            }
            catch (IllegalAccessException ex) {
                throw new NoSuchFieldException(propertyName);
            }
        }
        return result;
    }
    
    public static boolean setFieldValue(final OgnlContext context, final Object target, final String propertyName, Object value) throws OgnlException {
        boolean result = false;
        try {
            final Field f = getField((target == null) ? null : target.getClass(), propertyName);
            if (f != null && !Modifier.isStatic(f.getModifiers())) {
                final Object state = context.getMemberAccess().setup(context, target, f, propertyName);
                try {
                    if (isTypeCompatible(value, f.getType()) || (value = getConvertedType(context, target, f, propertyName, value, f.getType())) != null) {
                        f.set(target, value);
                        result = true;
                    }
                }
                finally {
                    context.getMemberAccess().restore(context, target, f, propertyName, state);
                }
            }
        }
        catch (IllegalAccessException ex) {
            throw new NoSuchPropertyException(target, propertyName, ex);
        }
        return result;
    }
    
    public static boolean isFieldAccessible(final OgnlContext context, final Object target, final Class inClass, final String propertyName) {
        return isFieldAccessible(context, target, getField(inClass, propertyName), propertyName);
    }
    
    public static boolean isFieldAccessible(final OgnlContext context, final Object target, final Field field, final String propertyName) {
        return context.getMemberAccess().isAccessible(context, target, field, propertyName);
    }
    
    public static boolean hasField(final OgnlContext context, final Object target, final Class inClass, final String propertyName) {
        final Field f = getField(inClass, propertyName);
        return f != null && isFieldAccessible(context, target, f, propertyName);
    }
    
    public static Object getStaticField(final OgnlContext context, final String className, final String fieldName) throws OgnlException {
        Exception reason = null;
        try {
            final Class c = classForName(context, className);
            if (c == null) {
                throw new OgnlException("Unable to find class " + className + " when resolving field name of " + fieldName);
            }
            if (fieldName.equals("class")) {
                return c;
            }
            if (isJdk15() && c.isEnum()) {
                try {
                    return Enum.valueOf((Class<Object>)c, fieldName);
                }
                catch (IllegalArgumentException ex) {}
            }
            final Field f = c.getField(fieldName);
            if (!Modifier.isStatic(f.getModifiers())) {
                throw new OgnlException("Field " + fieldName + " of class " + className + " is not static");
            }
            return f.get(null);
        }
        catch (ClassNotFoundException e) {
            reason = e;
        }
        catch (NoSuchFieldException e2) {
            reason = e2;
        }
        catch (SecurityException e3) {
            reason = e3;
        }
        catch (IllegalAccessException e4) {
            reason = e4;
        }
        throw new OgnlException("Could not get static field " + fieldName + " from class " + className, reason);
    }
    
    private static String capitalizeBeanPropertyName(final String propertyName) {
        if (propertyName.length() == 1) {
            return propertyName.toUpperCase();
        }
        if (propertyName.startsWith("get") && propertyName.endsWith("()") && Character.isUpperCase(propertyName.substring(3, 4).charAt(0))) {
            return propertyName;
        }
        if (propertyName.startsWith("set") && propertyName.endsWith(")") && Character.isUpperCase(propertyName.substring(3, 4).charAt(0))) {
            return propertyName;
        }
        if (propertyName.startsWith("is") && propertyName.endsWith("()") && Character.isUpperCase(propertyName.substring(2, 3).charAt(0))) {
            return propertyName;
        }
        final char first = propertyName.charAt(0);
        final char second = propertyName.charAt(1);
        if (Character.isLowerCase(first) && Character.isUpperCase(second)) {
            return propertyName;
        }
        final char[] chars = propertyName.toCharArray();
        chars[0] = Character.toUpperCase(chars[0]);
        return new String(chars);
    }
    
    public static List getDeclaredMethods(final Class targetClass, final String propertyName, final boolean findSets) {
        List result = null;
        final ClassCache cache = OgnlRuntime._declaredMethods[!findSets];
        Map propertyCache = (Map)cache.get(targetClass);
        if (propertyCache == null || (result = propertyCache.get(propertyName)) == null) {
            synchronized (cache) {
                propertyCache = (Map)cache.get(targetClass);
                if (propertyCache == null || (result = propertyCache.get(propertyName)) == null) {
                    final String baseName = capitalizeBeanPropertyName(propertyName);
                    for (Class c = targetClass; c != null; c = c.getSuperclass()) {
                        final Method[] methods = c.getDeclaredMethods();
                        for (int i = 0; i < methods.length; ++i) {
                            if (isMethodCallable(methods[i])) {
                                final String ms = methods[i].getName();
                                if (ms.endsWith(baseName)) {
                                    boolean isSet = false;
                                    boolean isIs = false;
                                    if ((isSet = ms.startsWith("set")) || ms.startsWith("get") || (isIs = ms.startsWith("is"))) {
                                        final int prefixLength = isIs ? 2 : 3;
                                        if (isSet == findSets && baseName.length() == ms.length() - prefixLength) {
                                            if (result == null) {
                                                result = new ArrayList();
                                            }
                                            result.add(methods[i]);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (propertyCache == null) {
                        cache.put(targetClass, propertyCache = new HashMap(101));
                    }
                    propertyCache.put(propertyName, (result == null) ? OgnlRuntime.NotFoundList : result);
                }
            }
        }
        return (result == OgnlRuntime.NotFoundList) ? null : result;
    }
    
    static boolean isMethodCallable(final Method m) {
        return (!isJdk15() || !m.isSynthetic()) && !Modifier.isVolatile(m.getModifiers());
    }
    
    public static Method getGetMethod(final OgnlContext context, final Class targetClass, final String propertyName) throws IntrospectionException, OgnlException {
        Method method = OgnlRuntime.cacheGetMethod.get(targetClass, propertyName);
        if (method != null) {
            return method;
        }
        if (OgnlRuntime.cacheGetMethod.containsKey(targetClass, propertyName)) {
            return null;
        }
        method = _getGetMethod(context, targetClass, propertyName);
        OgnlRuntime.cacheGetMethod.put(targetClass, propertyName, method);
        return method;
    }
    
    private static Method _getGetMethod(final OgnlContext context, final Class targetClass, final String propertyName) throws IntrospectionException, OgnlException {
        Method result = null;
        final List methods = getDeclaredMethods(targetClass, propertyName, false);
        if (methods != null) {
            for (int i = 0, icount = methods.size(); i < icount; ++i) {
                final Method m = methods.get(i);
                final Class[] mParameterTypes = findParameterTypes(targetClass, m);
                if (mParameterTypes.length == 0) {
                    result = m;
                    break;
                }
            }
        }
        return result;
    }
    
    public static boolean isMethodAccessible(final OgnlContext context, final Object target, final Method method, final String propertyName) {
        return method != null && context.getMemberAccess().isAccessible(context, target, method, propertyName);
    }
    
    public static boolean hasGetMethod(final OgnlContext context, final Object target, final Class targetClass, final String propertyName) throws IntrospectionException, OgnlException {
        return isMethodAccessible(context, target, getGetMethod(context, targetClass, propertyName), propertyName);
    }
    
    public static Method getSetMethod(final OgnlContext context, final Class targetClass, final String propertyName) throws IntrospectionException, OgnlException {
        Method method = OgnlRuntime.cacheSetMethod.get(targetClass, propertyName);
        if (method != null) {
            return method;
        }
        if (OgnlRuntime.cacheSetMethod.containsKey(targetClass, propertyName)) {
            return null;
        }
        method = _getSetMethod(context, targetClass, propertyName);
        OgnlRuntime.cacheSetMethod.put(targetClass, propertyName, method);
        return method;
    }
    
    private static Method _getSetMethod(final OgnlContext context, final Class targetClass, final String propertyName) throws IntrospectionException, OgnlException {
        Method result = null;
        final List methods = getDeclaredMethods(targetClass, propertyName, true);
        if (methods != null) {
            for (int i = 0, icount = methods.size(); i < icount; ++i) {
                final Method m = methods.get(i);
                final Class[] mParameterTypes = findParameterTypes(targetClass, m);
                if (mParameterTypes.length == 1) {
                    result = m;
                    break;
                }
            }
        }
        return result;
    }
    
    public static final boolean hasSetMethod(final OgnlContext context, final Object target, final Class targetClass, final String propertyName) throws IntrospectionException, OgnlException {
        return isMethodAccessible(context, target, getSetMethod(context, targetClass, propertyName), propertyName);
    }
    
    public static final boolean hasGetProperty(final OgnlContext context, final Object target, final Object oname) throws IntrospectionException, OgnlException {
        final Class targetClass = (target == null) ? null : target.getClass();
        final String name = oname.toString();
        return hasGetMethod(context, target, targetClass, name) || hasField(context, target, targetClass, name);
    }
    
    public static final boolean hasSetProperty(final OgnlContext context, final Object target, final Object oname) throws IntrospectionException, OgnlException {
        final Class targetClass = (target == null) ? null : target.getClass();
        final String name = oname.toString();
        return hasSetMethod(context, target, targetClass, name) || hasField(context, target, targetClass, name);
    }
    
    private static final boolean indexMethodCheck(final List methods) {
        boolean result = false;
        if (methods.size() > 0) {
            final Method fm = methods.get(0);
            final Class[] fmpt = getParameterTypes(fm);
            final int fmpc = fmpt.length;
            Class lastMethodClass = fm.getDeclaringClass();
            result = true;
            for (int i = 1; result && i < methods.size(); ++i) {
                final Method m = methods.get(i);
                final Class c = m.getDeclaringClass();
                if (lastMethodClass == c) {
                    result = false;
                }
                else {
                    final Class[] mpt = getParameterTypes(fm);
                    final int mpc = fmpt.length;
                    if (fmpc != mpc) {
                        result = false;
                    }
                    for (int j = 0; j < fmpc; ++j) {
                        if (fmpt[j] != mpt[j]) {
                            result = false;
                            break;
                        }
                    }
                }
                lastMethodClass = c;
            }
        }
        return result;
    }
    
    static void findObjectIndexedPropertyDescriptors(final Class targetClass, final Map intoMap) throws OgnlException {
        final Map allMethods = getMethods(targetClass, false);
        final Map pairs = new HashMap(101);
        for (final String methodName : allMethods.keySet()) {
            final List methods = allMethods.get(methodName);
            if (indexMethodCheck(methods)) {
                boolean isGet = false;
                boolean isSet = false;
                final Method m = methods.get(0);
                if ((!(isSet = methodName.startsWith("set")) && !(isGet = methodName.startsWith("get"))) || methodName.length() <= 3) {
                    continue;
                }
                final String propertyName = Introspector.decapitalize(methodName.substring(3));
                final Class[] parameterTypes = getParameterTypes(m);
                final int parameterCount = parameterTypes.length;
                if (isGet && parameterCount == 1 && m.getReturnType() != Void.TYPE) {
                    List pair = pairs.get(propertyName);
                    if (pair == null) {
                        pairs.put(propertyName, pair = new ArrayList());
                    }
                    pair.add(m);
                }
                if (!isSet || parameterCount != 2 || m.getReturnType() != Void.TYPE) {
                    continue;
                }
                List pair = pairs.get(propertyName);
                if (pair == null) {
                    pairs.put(propertyName, pair = new ArrayList());
                }
                pair.add(m);
            }
        }
        for (final String propertyName2 : pairs.keySet()) {
            final List methods = pairs.get(propertyName2);
            if (methods.size() == 2) {
                final Method method1 = methods.get(0);
                final Method method2 = methods.get(1);
                final Method setMethod = (method1.getParameterTypes().length == 2) ? method1 : method2;
                final Method getMethod = (setMethod == method1) ? method2 : method1;
                final Class keyType = getMethod.getParameterTypes()[0];
                final Class propertyType = getMethod.getReturnType();
                if (keyType != setMethod.getParameterTypes()[0] || propertyType != setMethod.getParameterTypes()[1]) {
                    continue;
                }
                ObjectIndexedPropertyDescriptor propertyDescriptor;
                try {
                    propertyDescriptor = new ObjectIndexedPropertyDescriptor(propertyName2, propertyType, getMethod, setMethod);
                }
                catch (Exception ex) {
                    throw new OgnlException("creating object indexed property descriptor for '" + propertyName2 + "' in " + targetClass, ex);
                }
                intoMap.put(propertyName2, propertyDescriptor);
            }
        }
    }
    
    public static Map getPropertyDescriptors(final Class targetClass) throws IntrospectionException, OgnlException {
        Map result;
        if ((result = (Map)OgnlRuntime._propertyDescriptorCache.get(targetClass)) == null) {
            synchronized (OgnlRuntime._propertyDescriptorCache) {
                if ((result = (Map)OgnlRuntime._propertyDescriptorCache.get(targetClass)) == null) {
                    final PropertyDescriptor[] pda = Introspector.getBeanInfo(targetClass).getPropertyDescriptors();
                    result = new HashMap(101);
                    for (int i = 0, icount = pda.length; i < icount; ++i) {
                        if (pda[i].getReadMethod() != null && !isMethodCallable(pda[i].getReadMethod())) {
                            pda[i].setReadMethod(findClosestMatchingMethod(targetClass, pda[i].getReadMethod(), pda[i].getName(), pda[i].getPropertyType(), true));
                        }
                        if (pda[i].getWriteMethod() != null && !isMethodCallable(pda[i].getWriteMethod())) {
                            pda[i].setWriteMethod(findClosestMatchingMethod(targetClass, pda[i].getWriteMethod(), pda[i].getName(), pda[i].getPropertyType(), false));
                        }
                        result.put(pda[i].getName(), pda[i]);
                    }
                    findObjectIndexedPropertyDescriptors(targetClass, result);
                    OgnlRuntime._propertyDescriptorCache.put(targetClass, result);
                }
            }
        }
        return result;
    }
    
    public static PropertyDescriptor getPropertyDescriptor(final Class targetClass, final String propertyName) throws IntrospectionException, OgnlException {
        if (targetClass == null) {
            return null;
        }
        return getPropertyDescriptors(targetClass).get(propertyName);
    }
    
    static Method findClosestMatchingMethod(final Class targetClass, final Method m, final String propertyName, final Class propertyType, final boolean isReadMethod) {
        final List methods = getDeclaredMethods(targetClass, propertyName, !isReadMethod);
        if (methods != null) {
            for (final Object method1 : methods) {
                final Method method2 = (Method)method1;
                if (method2.getName().equals(m.getName()) && m.getReturnType().isAssignableFrom(m.getReturnType()) && method2.getReturnType() == propertyType && method2.getParameterTypes().length == m.getParameterTypes().length) {
                    return method2;
                }
            }
        }
        return m;
    }
    
    public static PropertyDescriptor[] getPropertyDescriptorsArray(final Class targetClass) throws IntrospectionException {
        PropertyDescriptor[] result = null;
        if (targetClass != null && (result = (PropertyDescriptor[])OgnlRuntime._propertyDescriptorCache.get(targetClass)) == null) {
            synchronized (OgnlRuntime._propertyDescriptorCache) {
                if ((result = (PropertyDescriptor[])OgnlRuntime._propertyDescriptorCache.get(targetClass)) == null) {
                    OgnlRuntime._propertyDescriptorCache.put(targetClass, result = Introspector.getBeanInfo(targetClass).getPropertyDescriptors());
                }
            }
        }
        return result;
    }
    
    public static PropertyDescriptor getPropertyDescriptorFromArray(final Class targetClass, final String name) throws IntrospectionException {
        PropertyDescriptor result = null;
        final PropertyDescriptor[] pda = getPropertyDescriptorsArray(targetClass);
        for (int i = 0, icount = pda.length; result == null && i < icount; ++i) {
            if (pda[i].getName().compareTo(name) == 0) {
                result = pda[i];
            }
        }
        return result;
    }
    
    public static void setMethodAccessor(final Class cls, final MethodAccessor accessor) {
        synchronized (OgnlRuntime._methodAccessors) {
            OgnlRuntime._methodAccessors.put(cls, accessor);
        }
    }
    
    public static MethodAccessor getMethodAccessor(final Class cls) throws OgnlException {
        final MethodAccessor answer = (MethodAccessor)getHandler(cls, OgnlRuntime._methodAccessors);
        if (answer != null) {
            return answer;
        }
        throw new OgnlException("No method accessor for " + cls);
    }
    
    public static void setPropertyAccessor(final Class cls, final PropertyAccessor accessor) {
        synchronized (OgnlRuntime._propertyAccessors) {
            OgnlRuntime._propertyAccessors.put(cls, accessor);
        }
    }
    
    public static PropertyAccessor getPropertyAccessor(final Class cls) throws OgnlException {
        final PropertyAccessor answer = (PropertyAccessor)getHandler(cls, OgnlRuntime._propertyAccessors);
        if (answer != null) {
            return answer;
        }
        throw new OgnlException("No property accessor for class " + cls);
    }
    
    public static ElementsAccessor getElementsAccessor(final Class cls) throws OgnlException {
        final ElementsAccessor answer = (ElementsAccessor)getHandler(cls, OgnlRuntime._elementsAccessors);
        if (answer != null) {
            return answer;
        }
        throw new OgnlException("No elements accessor for class " + cls);
    }
    
    public static void setElementsAccessor(final Class cls, final ElementsAccessor accessor) {
        synchronized (OgnlRuntime._elementsAccessors) {
            OgnlRuntime._elementsAccessors.put(cls, accessor);
        }
    }
    
    public static NullHandler getNullHandler(final Class cls) throws OgnlException {
        final NullHandler answer = (NullHandler)getHandler(cls, OgnlRuntime._nullHandlers);
        if (answer != null) {
            return answer;
        }
        throw new OgnlException("No null handler for class " + cls);
    }
    
    public static void setNullHandler(final Class cls, final NullHandler handler) {
        synchronized (OgnlRuntime._nullHandlers) {
            OgnlRuntime._nullHandlers.put(cls, handler);
        }
    }
    
    private static Object getHandler(final Class forClass, final ClassCache handlers) {
        Object answer = null;
        if ((answer = handlers.get(forClass)) == null) {
            synchronized (handlers) {
                if ((answer = handlers.get(forClass)) == null) {
                    Class keyFound = null;
                    Label_0163: {
                        if (forClass.isArray()) {
                            answer = handlers.get(Object[].class);
                            keyFound = null;
                        }
                        else {
                            keyFound = forClass;
                            for (Class c = forClass; c != null; c = c.getSuperclass()) {
                                answer = handlers.get(c);
                                if (answer != null) {
                                    keyFound = c;
                                    break;
                                }
                                final Class[] interfaces = c.getInterfaces();
                                for (int index = 0, count = interfaces.length; index < count; ++index) {
                                    final Class iface = interfaces[index];
                                    answer = handlers.get(iface);
                                    if (answer == null) {
                                        answer = getHandler(iface, handlers);
                                    }
                                    if (answer != null) {
                                        keyFound = iface;
                                        break Label_0163;
                                    }
                                }
                            }
                        }
                    }
                    if (answer != null && keyFound != forClass) {
                        handlers.put(forClass, answer);
                    }
                }
            }
        }
        return answer;
    }
    
    public static Object getProperty(final OgnlContext context, final Object source, final Object name) throws OgnlException {
        if (source == null) {
            throw new OgnlException("source is null for getProperty(null, \"" + name + "\")");
        }
        final PropertyAccessor accessor;
        if ((accessor = getPropertyAccessor(getTargetClass(source))) == null) {
            throw new OgnlException("No property accessor for " + getTargetClass(source).getName());
        }
        return accessor.getProperty(context, source, name);
    }
    
    public static void setProperty(final OgnlContext context, final Object target, final Object name, final Object value) throws OgnlException {
        if (target == null) {
            throw new OgnlException("target is null for setProperty(null, \"" + name + "\", " + value + ")");
        }
        final PropertyAccessor accessor;
        if ((accessor = getPropertyAccessor(getTargetClass(target))) == null) {
            throw new OgnlException("No property accessor for " + getTargetClass(target).getName());
        }
        accessor.setProperty(context, target, name, value);
    }
    
    public static int getIndexedPropertyType(final OgnlContext context, final Class sourceClass, final String name) throws OgnlException {
        int result = OgnlRuntime.INDEXED_PROPERTY_NONE;
        try {
            final PropertyDescriptor pd = getPropertyDescriptor(sourceClass, name);
            if (pd != null) {
                if (pd instanceof IndexedPropertyDescriptor) {
                    result = OgnlRuntime.INDEXED_PROPERTY_INT;
                }
                else if (pd instanceof ObjectIndexedPropertyDescriptor) {
                    result = OgnlRuntime.INDEXED_PROPERTY_OBJECT;
                }
            }
        }
        catch (Exception ex) {
            throw new OgnlException("problem determining if '" + name + "' is an indexed property", ex);
        }
        return result;
    }
    
    public static Object getIndexedProperty(final OgnlContext context, final Object source, final String name, final Object index) throws OgnlException {
        final Object[] args = OgnlRuntime._objectArrayPool.create(index);
        try {
            final PropertyDescriptor pd = getPropertyDescriptor((source == null) ? null : source.getClass(), name);
            Method m;
            if (pd instanceof IndexedPropertyDescriptor) {
                m = ((IndexedPropertyDescriptor)pd).getIndexedReadMethod();
            }
            else {
                if (!(pd instanceof ObjectIndexedPropertyDescriptor)) {
                    throw new OgnlException("property '" + name + "' is not an indexed property");
                }
                m = ((ObjectIndexedPropertyDescriptor)pd).getIndexedReadMethod();
            }
            return callMethod(context, source, m.getName(), args);
        }
        catch (OgnlException ex) {
            throw ex;
        }
        catch (Exception ex2) {
            throw new OgnlException("getting indexed property descriptor for '" + name + "'", ex2);
        }
        finally {
            OgnlRuntime._objectArrayPool.recycle(args);
        }
    }
    
    public static void setIndexedProperty(final OgnlContext context, final Object source, final String name, final Object index, final Object value) throws OgnlException {
        final Object[] args = OgnlRuntime._objectArrayPool.create(index, value);
        try {
            final PropertyDescriptor pd = getPropertyDescriptor((source == null) ? null : source.getClass(), name);
            Method m;
            if (pd instanceof IndexedPropertyDescriptor) {
                m = ((IndexedPropertyDescriptor)pd).getIndexedWriteMethod();
            }
            else {
                if (!(pd instanceof ObjectIndexedPropertyDescriptor)) {
                    throw new OgnlException("property '" + name + "' is not an indexed property");
                }
                m = ((ObjectIndexedPropertyDescriptor)pd).getIndexedWriteMethod();
            }
            callMethod(context, source, m.getName(), args);
        }
        catch (OgnlException ex) {
            throw ex;
        }
        catch (Exception ex2) {
            throw new OgnlException("getting indexed property descriptor for '" + name + "'", ex2);
        }
        finally {
            OgnlRuntime._objectArrayPool.recycle(args);
        }
    }
    
    public static EvaluationPool getEvaluationPool() {
        return OgnlRuntime._evaluationPool;
    }
    
    public static ObjectArrayPool getObjectArrayPool() {
        return OgnlRuntime._objectArrayPool;
    }
    
    public static void setClassCacheInspector(final ClassCacheInspector inspector) {
        OgnlRuntime._cacheInspector = inspector;
        OgnlRuntime._propertyDescriptorCache.setClassInspector(OgnlRuntime._cacheInspector);
        OgnlRuntime._constructorCache.setClassInspector(OgnlRuntime._cacheInspector);
        OgnlRuntime._staticMethodCache.setClassInspector(OgnlRuntime._cacheInspector);
        OgnlRuntime._instanceMethodCache.setClassInspector(OgnlRuntime._cacheInspector);
        OgnlRuntime._invokePermissionCache.setClassInspector(OgnlRuntime._cacheInspector);
        OgnlRuntime._fieldCache.setClassInspector(OgnlRuntime._cacheInspector);
        OgnlRuntime._declaredMethods[0].setClassInspector(OgnlRuntime._cacheInspector);
        OgnlRuntime._declaredMethods[1].setClassInspector(OgnlRuntime._cacheInspector);
    }
    
    public static Method getMethod(final OgnlContext context, final Class target, final String name, final Node[] children, final boolean includeStatic) throws Exception {
        Class[] parms;
        if (children != null && children.length > 0) {
            parms = new Class[children.length];
            final Class currType = context.getCurrentType();
            final Class currAccessor = context.getCurrentAccessor();
            final Object cast = context.get("_preCast");
            context.setCurrentObject(context.getRoot());
            context.setCurrentType((context.getRoot() != null) ? context.getRoot().getClass() : null);
            context.setCurrentAccessor(null);
            context.setPreviousType(null);
            for (int i = 0; i < children.length; ++i) {
                children[i].toGetSourceString(context, context.getRoot());
                parms[i] = context.getCurrentType();
            }
            context.put("_preCast", cast);
            context.setCurrentType(currType);
            context.setCurrentAccessor(currAccessor);
            context.setCurrentObject(target);
        }
        else {
            parms = OgnlRuntime.EMPTY_CLASS_ARRAY;
        }
        final List methods = getMethods(target, name, includeStatic);
        if (methods == null) {
            return null;
        }
        for (int j = 0; j < methods.size(); ++j) {
            final Method m = methods.get(j);
            final boolean varArgs = isJdk15() && m.isVarArgs();
            if (parms.length == m.getParameterTypes().length || varArgs) {
                final Class[] mparms = m.getParameterTypes();
                boolean matched = true;
                for (int p = 0; p < mparms.length; ++p) {
                    if (!varArgs || !mparms[p].isArray()) {
                        if (parms[p] == null) {
                            matched = false;
                            break;
                        }
                        if (parms[p] != mparms[p]) {
                            if (!mparms[p].isPrimitive() || Character.TYPE == mparms[p] || Byte.TYPE == mparms[p] || !Number.class.isAssignableFrom(parms[p]) || getPrimitiveWrapperClass(parms[p]) != mparms[p]) {
                                matched = false;
                                break;
                            }
                        }
                    }
                }
                if (matched) {
                    return m;
                }
            }
        }
        return null;
    }
    
    public static Method getReadMethod(final Class target, final String name) {
        return getReadMethod(target, name, null);
    }
    
    public static Method getReadMethod(final Class target, String name, final Class[] argClasses) {
        try {
            if (name.indexOf(34) >= 0) {
                name = name.replaceAll("\"", "");
            }
            name = name.toLowerCase();
            final BeanInfo info = Introspector.getBeanInfo(target);
            final MethodDescriptor[] methods = info.getMethodDescriptors();
            final ArrayList<Method> candidates = new ArrayList<Method>();
            for (int i = 0; i < methods.length; ++i) {
                if (isMethodCallable(methods[i].getMethod())) {
                    if ((methods[i].getName().equalsIgnoreCase(name) || methods[i].getName().toLowerCase().equals("get" + name) || methods[i].getName().toLowerCase().equals("has" + name) || methods[i].getName().toLowerCase().equals("is" + name)) && !methods[i].getName().startsWith("set")) {
                        candidates.add(methods[i].getMethod());
                    }
                }
            }
            if (!candidates.isEmpty()) {
                final MatchingMethod mm = findBestMethod(candidates, target, name, argClasses);
                if (mm != null) {
                    return mm.mMethod;
                }
            }
            for (int i = 0; i < methods.length; ++i) {
                if (isMethodCallable(methods[i].getMethod())) {
                    if (methods[i].getName().equalsIgnoreCase(name) && !methods[i].getName().startsWith("set") && !methods[i].getName().startsWith("get") && !methods[i].getName().startsWith("is") && !methods[i].getName().startsWith("has") && methods[i].getMethod().getReturnType() != Void.TYPE) {
                        final Method m = methods[i].getMethod();
                        if (!candidates.contains(m)) {
                            candidates.add(m);
                        }
                    }
                }
            }
            if (!candidates.isEmpty()) {
                final MatchingMethod mm = findBestMethod(candidates, target, name, argClasses);
                if (mm != null) {
                    return mm.mMethod;
                }
            }
            if (!name.startsWith("get")) {
                final Method ret = getReadMethod(target, "get" + name, argClasses);
                if (ret != null) {
                    return ret;
                }
            }
            if (!candidates.isEmpty()) {
                final int reqArgCount = (argClasses == null) ? 0 : argClasses.length;
                for (final Method j : candidates) {
                    if (j.getParameterTypes().length == reqArgCount) {
                        return j;
                    }
                }
            }
        }
        catch (Throwable t) {
            throw OgnlOps.castToRuntime(t);
        }
        return null;
    }
    
    public static Method getWriteMethod(final Class target, final String name) {
        return getWriteMethod(target, name, null);
    }
    
    public static Method getWriteMethod(final Class target, String name, final Class[] argClasses) {
        try {
            if (name.indexOf(34) >= 0) {
                name = name.replaceAll("\"", "");
            }
            final BeanInfo info = Introspector.getBeanInfo(target);
            final MethodDescriptor[] methods = info.getMethodDescriptors();
            final ArrayList<Method> candidates = new ArrayList<Method>();
            for (int i = 0; i < methods.length; ++i) {
                if (isMethodCallable(methods[i].getMethod())) {
                    if ((methods[i].getName().equalsIgnoreCase(name) || methods[i].getName().toLowerCase().equals(name.toLowerCase()) || methods[i].getName().toLowerCase().equals("set" + name.toLowerCase())) && !methods[i].getName().startsWith("get")) {
                        candidates.add(methods[i].getMethod());
                    }
                }
            }
            if (!candidates.isEmpty()) {
                final MatchingMethod mm = findBestMethod(candidates, target, name, argClasses);
                if (mm != null) {
                    return mm.mMethod;
                }
            }
            final Method[] cmethods = target.getClass().getMethods();
            for (int j = 0; j < cmethods.length; ++j) {
                if (isMethodCallable(cmethods[j])) {
                    if ((cmethods[j].getName().equalsIgnoreCase(name) || cmethods[j].getName().toLowerCase().equals(name.toLowerCase()) || cmethods[j].getName().toLowerCase().equals("set" + name.toLowerCase())) && !cmethods[j].getName().startsWith("get")) {
                        final Method m = methods[j].getMethod();
                        if (!candidates.contains(m)) {
                            candidates.add(m);
                        }
                    }
                }
            }
            if (!candidates.isEmpty()) {
                final MatchingMethod mm2 = findBestMethod(candidates, target, name, argClasses);
                if (mm2 != null) {
                    return mm2.mMethod;
                }
            }
            if (!name.startsWith("set")) {
                final Method ret = getReadMethod(target, "set" + name, argClasses);
                if (ret != null) {
                    return ret;
                }
            }
            if (!candidates.isEmpty()) {
                final int reqArgCount = (argClasses == null) ? 0 : argClasses.length;
                for (final Method k : candidates) {
                    if (k.getParameterTypes().length == reqArgCount) {
                        return k;
                    }
                }
                if (argClasses == null && candidates.size() == 1) {
                    return candidates.get(0);
                }
            }
        }
        catch (Throwable t) {
            throw OgnlOps.castToRuntime(t);
        }
        return null;
    }
    
    public static PropertyDescriptor getProperty(final Class target, final String name) {
        try {
            final BeanInfo info = Introspector.getBeanInfo(target);
            final PropertyDescriptor[] pds = info.getPropertyDescriptors();
            for (int i = 0; i < pds.length; ++i) {
                if (pds[i].getName().equalsIgnoreCase(name) || pds[i].getName().toLowerCase().equals(name.toLowerCase()) || pds[i].getName().toLowerCase().endsWith(name.toLowerCase())) {
                    return pds[i];
                }
            }
        }
        catch (Throwable t) {
            throw OgnlOps.castToRuntime(t);
        }
        return null;
    }
    
    public static boolean isBoolean(final String expression) {
        return expression != null && ("true".equals(expression) || "false".equals(expression) || "!true".equals(expression) || "!false".equals(expression) || "(true)".equals(expression) || "!(true)".equals(expression) || "(false)".equals(expression) || "!(false)".equals(expression) || expression.startsWith("ognl.OgnlOps"));
    }
    
    public static boolean shouldConvertNumericTypes(final OgnlContext context) {
        return context.getCurrentType() == null || context.getPreviousType() == null || ((context.getCurrentType() != context.getPreviousType() || !context.getCurrentType().isPrimitive() || !context.getPreviousType().isPrimitive()) && context.getCurrentType() != null && !context.getCurrentType().isArray() && context.getPreviousType() != null && !context.getPreviousType().isArray());
    }
    
    public static String getChildSource(final OgnlContext context, final Object target, final Node child) throws OgnlException {
        return getChildSource(context, target, child, false);
    }
    
    public static String getChildSource(final OgnlContext context, final Object target, final Node child, final boolean forceConversion) throws OgnlException {
        String pre = (String)context.get("_currentChain");
        if (pre == null) {
            pre = "";
        }
        try {
            child.getValue(context, target);
        }
        catch (NullPointerException ex) {}
        catch (ArithmeticException e) {
            context.setCurrentType(Integer.TYPE);
            return "0";
        }
        catch (Throwable t) {
            throw OgnlOps.castToRuntime(t);
        }
        String source = null;
        try {
            source = child.toGetSourceString(context, target);
        }
        catch (Throwable t2) {
            throw OgnlOps.castToRuntime(t2);
        }
        if (!ASTConst.class.isInstance(child) && (target == null || context.getRoot() != target)) {
            source = pre + source;
        }
        if (context.getRoot() != null) {
            source = ExpressionCompiler.getRootExpression(child, context.getRoot(), context) + source;
            context.setCurrentAccessor(context.getRoot().getClass());
        }
        if (ASTChain.class.isInstance(child)) {
            String cast = (String)context.remove("_preCast");
            if (cast == null) {
                cast = "";
            }
            source = cast + source;
        }
        if (source == null || source.trim().length() < 1) {
            source = "null";
        }
        return source;
    }
    
    static {
        NotFound = new Object();
        NotFoundList = new ArrayList();
        NotFoundMap = new HashMap();
        NoArguments = new Object[0];
        NoArgumentTypes = new Class[0];
        NoConversionPossible = "ognl.NoConversionPossible";
        OgnlRuntime.INDEXED_PROPERTY_NONE = 0;
        OgnlRuntime.INDEXED_PROPERTY_INT = 1;
        OgnlRuntime.INDEXED_PROPERTY_OBJECT = 2;
        NULL_STRING = "" + (Object)null;
        HEX_PADDING = new HashMap();
        OgnlRuntime._jdk15 = false;
        OgnlRuntime._jdkChecked = false;
        _methodAccessors = new ClassCacheImpl();
        _propertyAccessors = new ClassCacheImpl();
        _elementsAccessors = new ClassCacheImpl();
        _nullHandlers = new ClassCacheImpl();
        _propertyDescriptorCache = new ClassCacheImpl();
        _constructorCache = new ClassCacheImpl();
        _staticMethodCache = new ClassCacheImpl();
        _instanceMethodCache = new ClassCacheImpl();
        _invokePermissionCache = new ClassCacheImpl();
        _fieldCache = new ClassCacheImpl();
        _superclasses = new ArrayList();
        _declaredMethods = new ClassCache[] { new ClassCacheImpl(), new ClassCacheImpl() };
        _primitiveTypes = new HashMap(101);
        _primitiveDefaults = new ClassCacheImpl();
        _methodParameterTypesCache = new HashMap(101);
        _genericMethodParameterTypesCache = new HashMap(101);
        _ctorParameterTypesCache = new HashMap(101);
        OgnlRuntime._securityManager = System.getSecurityManager();
        _evaluationPool = new EvaluationPool();
        _objectArrayPool = new ObjectArrayPool();
        _methodAccessCache = new ConcurrentHashMap<Method, Boolean>();
        _methodPermCache = new ConcurrentHashMap<Method, Boolean>();
        cacheSetMethod = new ClassPropertyMethodCache();
        cacheGetMethod = new ClassPropertyMethodCache();
        try {
            Class.forName("javassist.ClassPool");
            OgnlRuntime._compiler = new ExpressionCompiler();
        }
        catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Javassist library is missing in classpath! Please add missed dependency!", e);
        }
        catch (RuntimeException rt) {
            throw new IllegalStateException("Javassist library cannot be loaded, is it restricted by runtime environment?");
        }
        EMPTY_CLASS_ARRAY = new Class[0];
        (OgnlRuntime.PRIMITIVE_WRAPPER_CLASSES = new IdentityHashMap()).put(Boolean.TYPE, Boolean.class);
        OgnlRuntime.PRIMITIVE_WRAPPER_CLASSES.put(Boolean.class, Boolean.TYPE);
        OgnlRuntime.PRIMITIVE_WRAPPER_CLASSES.put(Byte.TYPE, Byte.class);
        OgnlRuntime.PRIMITIVE_WRAPPER_CLASSES.put(Byte.class, Byte.TYPE);
        OgnlRuntime.PRIMITIVE_WRAPPER_CLASSES.put(Character.TYPE, Character.class);
        OgnlRuntime.PRIMITIVE_WRAPPER_CLASSES.put(Character.class, Character.TYPE);
        OgnlRuntime.PRIMITIVE_WRAPPER_CLASSES.put(Short.TYPE, Short.class);
        OgnlRuntime.PRIMITIVE_WRAPPER_CLASSES.put(Short.class, Short.TYPE);
        OgnlRuntime.PRIMITIVE_WRAPPER_CLASSES.put(Integer.TYPE, Integer.class);
        OgnlRuntime.PRIMITIVE_WRAPPER_CLASSES.put(Integer.class, Integer.TYPE);
        OgnlRuntime.PRIMITIVE_WRAPPER_CLASSES.put(Long.TYPE, Long.class);
        OgnlRuntime.PRIMITIVE_WRAPPER_CLASSES.put(Long.class, Long.TYPE);
        OgnlRuntime.PRIMITIVE_WRAPPER_CLASSES.put(Float.TYPE, Float.class);
        OgnlRuntime.PRIMITIVE_WRAPPER_CLASSES.put(Float.class, Float.TYPE);
        OgnlRuntime.PRIMITIVE_WRAPPER_CLASSES.put(Double.TYPE, Double.class);
        OgnlRuntime.PRIMITIVE_WRAPPER_CLASSES.put(Double.class, Double.TYPE);
        (NUMERIC_CASTS = new HashMap()).put(Double.class, "(double)");
        OgnlRuntime.NUMERIC_CASTS.put(Float.class, "(float)");
        OgnlRuntime.NUMERIC_CASTS.put(Integer.class, "(int)");
        OgnlRuntime.NUMERIC_CASTS.put(Long.class, "(long)");
        OgnlRuntime.NUMERIC_CASTS.put(BigDecimal.class, "(double)");
        OgnlRuntime.NUMERIC_CASTS.put(BigInteger.class, "");
        (NUMERIC_VALUES = new HashMap()).put(Double.class, "doubleValue()");
        OgnlRuntime.NUMERIC_VALUES.put(Float.class, "floatValue()");
        OgnlRuntime.NUMERIC_VALUES.put(Integer.class, "intValue()");
        OgnlRuntime.NUMERIC_VALUES.put(Long.class, "longValue()");
        OgnlRuntime.NUMERIC_VALUES.put(Short.class, "shortValue()");
        OgnlRuntime.NUMERIC_VALUES.put(Byte.class, "byteValue()");
        OgnlRuntime.NUMERIC_VALUES.put(BigDecimal.class, "doubleValue()");
        OgnlRuntime.NUMERIC_VALUES.put(BigInteger.class, "doubleValue()");
        OgnlRuntime.NUMERIC_VALUES.put(Boolean.class, "booleanValue()");
        (NUMERIC_LITERALS = new HashMap()).put(Integer.class, "");
        OgnlRuntime.NUMERIC_LITERALS.put(Integer.TYPE, "");
        OgnlRuntime.NUMERIC_LITERALS.put(Long.class, "l");
        OgnlRuntime.NUMERIC_LITERALS.put(Long.TYPE, "l");
        OgnlRuntime.NUMERIC_LITERALS.put(BigInteger.class, "d");
        OgnlRuntime.NUMERIC_LITERALS.put(Float.class, "f");
        OgnlRuntime.NUMERIC_LITERALS.put(Float.TYPE, "f");
        OgnlRuntime.NUMERIC_LITERALS.put(Double.class, "d");
        OgnlRuntime.NUMERIC_LITERALS.put(Double.TYPE, "d");
        OgnlRuntime.NUMERIC_LITERALS.put(BigInteger.class, "d");
        OgnlRuntime.NUMERIC_LITERALS.put(BigDecimal.class, "d");
        (NUMERIC_DEFAULTS = new HashMap()).put(Boolean.class, Boolean.FALSE);
        OgnlRuntime.NUMERIC_DEFAULTS.put(Byte.class, new Byte((byte)0));
        OgnlRuntime.NUMERIC_DEFAULTS.put(Short.class, new Short((short)0));
        OgnlRuntime.NUMERIC_DEFAULTS.put(Character.class, new Character('\0'));
        OgnlRuntime.NUMERIC_DEFAULTS.put(Integer.class, new Integer(0));
        OgnlRuntime.NUMERIC_DEFAULTS.put(Long.class, new Long(0L));
        OgnlRuntime.NUMERIC_DEFAULTS.put(Float.class, new Float(0.0f));
        OgnlRuntime.NUMERIC_DEFAULTS.put(Double.class, new Double(0.0));
        OgnlRuntime.NUMERIC_DEFAULTS.put(BigInteger.class, new BigInteger("0"));
        OgnlRuntime.NUMERIC_DEFAULTS.put(BigDecimal.class, new BigDecimal(0.0));
        final PropertyAccessor p = new ArrayPropertyAccessor();
        setPropertyAccessor(Object.class, new ObjectPropertyAccessor());
        setPropertyAccessor(byte[].class, p);
        setPropertyAccessor(short[].class, p);
        setPropertyAccessor(char[].class, p);
        setPropertyAccessor(int[].class, p);
        setPropertyAccessor(long[].class, p);
        setPropertyAccessor(float[].class, p);
        setPropertyAccessor(double[].class, p);
        setPropertyAccessor(Object[].class, p);
        setPropertyAccessor(List.class, new ListPropertyAccessor());
        setPropertyAccessor(Map.class, new MapPropertyAccessor());
        setPropertyAccessor(Set.class, new SetPropertyAccessor());
        setPropertyAccessor(Iterator.class, new IteratorPropertyAccessor());
        setPropertyAccessor(Enumeration.class, new EnumerationPropertyAccessor());
        final ElementsAccessor e2 = new ArrayElementsAccessor();
        setElementsAccessor(Object.class, new ObjectElementsAccessor());
        setElementsAccessor(byte[].class, e2);
        setElementsAccessor(short[].class, e2);
        setElementsAccessor(char[].class, e2);
        setElementsAccessor(int[].class, e2);
        setElementsAccessor(long[].class, e2);
        setElementsAccessor(float[].class, e2);
        setElementsAccessor(double[].class, e2);
        setElementsAccessor(Object[].class, e2);
        setElementsAccessor(Collection.class, new CollectionElementsAccessor());
        setElementsAccessor(Map.class, new MapElementsAccessor());
        setElementsAccessor(Iterator.class, new IteratorElementsAccessor());
        setElementsAccessor(Enumeration.class, new EnumerationElementsAccessor());
        setElementsAccessor(Number.class, new NumberElementsAccessor());
        final NullHandler nh = new ObjectNullHandler();
        setNullHandler(Object.class, nh);
        setNullHandler(byte[].class, nh);
        setNullHandler(short[].class, nh);
        setNullHandler(char[].class, nh);
        setNullHandler(int[].class, nh);
        setNullHandler(long[].class, nh);
        setNullHandler(float[].class, nh);
        setNullHandler(double[].class, nh);
        setNullHandler(Object[].class, nh);
        final MethodAccessor ma = new ObjectMethodAccessor();
        setMethodAccessor(Object.class, ma);
        setMethodAccessor(byte[].class, ma);
        setMethodAccessor(short[].class, ma);
        setMethodAccessor(char[].class, ma);
        setMethodAccessor(int[].class, ma);
        setMethodAccessor(long[].class, ma);
        setMethodAccessor(float[].class, ma);
        setMethodAccessor(double[].class, ma);
        setMethodAccessor(Object[].class, ma);
        OgnlRuntime._primitiveTypes.put("boolean", Boolean.TYPE);
        OgnlRuntime._primitiveTypes.put("byte", Byte.TYPE);
        OgnlRuntime._primitiveTypes.put("short", Short.TYPE);
        OgnlRuntime._primitiveTypes.put("char", Character.TYPE);
        OgnlRuntime._primitiveTypes.put("int", Integer.TYPE);
        OgnlRuntime._primitiveTypes.put("long", Long.TYPE);
        OgnlRuntime._primitiveTypes.put("float", Float.TYPE);
        OgnlRuntime._primitiveTypes.put("double", Double.TYPE);
        OgnlRuntime._primitiveDefaults.put(Boolean.TYPE, Boolean.FALSE);
        OgnlRuntime._primitiveDefaults.put(Boolean.class, Boolean.FALSE);
        OgnlRuntime._primitiveDefaults.put(Byte.TYPE, new Byte((byte)0));
        OgnlRuntime._primitiveDefaults.put(Byte.class, new Byte((byte)0));
        OgnlRuntime._primitiveDefaults.put(Short.TYPE, new Short((short)0));
        OgnlRuntime._primitiveDefaults.put(Short.class, new Short((short)0));
        OgnlRuntime._primitiveDefaults.put(Character.TYPE, new Character('\0'));
        OgnlRuntime._primitiveDefaults.put(Integer.TYPE, new Integer(0));
        OgnlRuntime._primitiveDefaults.put(Long.TYPE, new Long(0L));
        OgnlRuntime._primitiveDefaults.put(Float.TYPE, new Float(0.0f));
        OgnlRuntime._primitiveDefaults.put(Double.TYPE, new Double(0.0));
        OgnlRuntime._primitiveDefaults.put(BigInteger.class, new BigInteger("0"));
        OgnlRuntime._primitiveDefaults.put(BigDecimal.class, new BigDecimal(0.0));
        NoArgsReport = new ArgsCompatbilityReport(0, new boolean[0]);
    }
    
    public static class ArgsCompatbilityReport
    {
        int score;
        boolean[] conversionNeeded;
        
        public ArgsCompatbilityReport(final int score, final boolean[] conversionNeeded) {
            this.score = score;
            this.conversionNeeded = conversionNeeded;
        }
    }
    
    private static class MatchingMethod
    {
        Method mMethod;
        int score;
        ArgsCompatbilityReport report;
        Class[] mParameterTypes;
        
        private MatchingMethod(final Method method, final int score, final ArgsCompatbilityReport report, final Class[] mParameterTypes) {
            this.mMethod = method;
            this.score = score;
            this.report = report;
            this.mParameterTypes = mParameterTypes;
        }
    }
    
    private static final class ClassPropertyMethodCache
    {
        private static final Method NULL_REPLACEMENT;
        private final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Method>> cache;
        
        ClassPropertyMethodCache() {
            this.cache = new ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Method>>();
        }
        
        Method get(final Class clazz, final String propertyName) {
            ConcurrentHashMap<String, Method> methodsByPropertyName = this.cache.get(clazz);
            if (methodsByPropertyName == null) {
                methodsByPropertyName = new ConcurrentHashMap<String, Method>();
                this.cache.put(clazz, methodsByPropertyName);
            }
            final Method method = methodsByPropertyName.get(propertyName);
            if (method == ClassPropertyMethodCache.NULL_REPLACEMENT) {
                return null;
            }
            return method;
        }
        
        void put(final Class clazz, final String propertyName, final Method method) {
            ConcurrentHashMap<String, Method> methodsByPropertyName = this.cache.get(clazz);
            if (methodsByPropertyName == null) {
                methodsByPropertyName = new ConcurrentHashMap<String, Method>();
                this.cache.put(clazz, methodsByPropertyName);
            }
            methodsByPropertyName.put(propertyName, (method == null) ? ClassPropertyMethodCache.NULL_REPLACEMENT : method);
        }
        
        boolean containsKey(final Class clazz, final String propertyName) {
            final ConcurrentHashMap<String, Method> methodsByPropertyName = this.cache.get(clazz);
            return methodsByPropertyName != null && methodsByPropertyName.containsKey(propertyName);
        }
        
        static {
            try {
                NULL_REPLACEMENT = ClassPropertyMethodCache.class.getDeclaredMethod("get", Class.class, String.class);
            }
            catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
