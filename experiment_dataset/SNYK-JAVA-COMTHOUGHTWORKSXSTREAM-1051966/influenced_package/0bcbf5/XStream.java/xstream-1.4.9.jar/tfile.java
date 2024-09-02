// 
// Decompiled by Procyon v0.5.36
// 

package com.thoughtworks.xstream;

import com.thoughtworks.xstream.security.NoPermission;
import com.thoughtworks.xstream.security.WildcardTypePermission;
import com.thoughtworks.xstream.security.RegExpTypePermission;
import com.thoughtworks.xstream.security.TypeHierarchyPermission;
import com.thoughtworks.xstream.security.ExplicitTypePermission;
import com.thoughtworks.xstream.security.TypePermission;
import java.io.ObjectInputValidation;
import java.io.EOFException;
import com.thoughtworks.xstream.core.util.CustomObjectInputStream;
import java.io.ObjectInputStream;
import java.io.NotActiveException;
import com.thoughtworks.xstream.core.util.CustomObjectOutputStream;
import com.thoughtworks.xstream.io.StatefulWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import com.thoughtworks.xstream.core.MapBackedDataHolder;
import com.thoughtworks.xstream.core.ReferenceByXPathMarshallingStrategy;
import com.thoughtworks.xstream.core.ReferenceByIdMarshallingStrategy;
import com.thoughtworks.xstream.core.TreeMarshallingStrategy;
import com.thoughtworks.xstream.converters.SingleValueConverterWrapper;
import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import com.thoughtworks.xstream.converters.DataHolder;
import java.io.OutputStream;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.io.Writer;
import java.io.StringWriter;
import com.thoughtworks.xstream.core.util.SelfStreamingInstanceChecker;
import com.thoughtworks.xstream.converters.extended.GregorianCalendarConverter;
import com.thoughtworks.xstream.converters.extended.LocaleConverter;
import com.thoughtworks.xstream.converters.extended.LookAndFeelConverter;
import com.thoughtworks.xstream.converters.extended.TextAttributeConverter;
import com.thoughtworks.xstream.converters.extended.ColorConverter;
import com.thoughtworks.xstream.converters.extended.FontConverter;
import com.thoughtworks.xstream.converters.extended.JavaFieldConverter;
import com.thoughtworks.xstream.converters.extended.JavaMethodConverter;
import com.thoughtworks.xstream.converters.extended.JavaClassConverter;
import com.thoughtworks.xstream.converters.extended.DynamicProxyConverter;
import com.thoughtworks.xstream.converters.extended.SqlDateConverter;
import com.thoughtworks.xstream.converters.extended.SqlTimeConverter;
import com.thoughtworks.xstream.converters.extended.SqlTimestampConverter;
import com.thoughtworks.xstream.converters.extended.FileConverter;
import com.thoughtworks.xstream.converters.extended.EncodedByteArrayConverter;
import com.thoughtworks.xstream.converters.collections.PropertiesConverter;
import com.thoughtworks.xstream.converters.collections.SingletonMapConverter;
import com.thoughtworks.xstream.converters.collections.SingletonCollectionConverter;
import com.thoughtworks.xstream.converters.collections.TreeSetConverter;
import com.thoughtworks.xstream.converters.collections.TreeMapConverter;
import com.thoughtworks.xstream.converters.collections.MapConverter;
import com.thoughtworks.xstream.converters.collections.CollectionConverter;
import com.thoughtworks.xstream.converters.collections.CharArrayConverter;
import com.thoughtworks.xstream.converters.collections.ArrayConverter;
import com.thoughtworks.xstream.converters.basic.BigDecimalConverter;
import com.thoughtworks.xstream.converters.basic.BigIntegerConverter;
import com.thoughtworks.xstream.converters.basic.URLConverter;
import com.thoughtworks.xstream.converters.basic.URIConverter;
import com.thoughtworks.xstream.converters.collections.BitSetConverter;
import com.thoughtworks.xstream.converters.basic.DateConverter;
import com.thoughtworks.xstream.converters.basic.StringBufferConverter;
import com.thoughtworks.xstream.converters.basic.StringConverter;
import com.thoughtworks.xstream.converters.basic.ByteConverter;
import com.thoughtworks.xstream.converters.basic.BooleanConverter;
import com.thoughtworks.xstream.converters.basic.CharConverter;
import com.thoughtworks.xstream.converters.basic.ShortConverter;
import com.thoughtworks.xstream.converters.basic.LongConverter;
import com.thoughtworks.xstream.converters.basic.DoubleConverter;
import com.thoughtworks.xstream.converters.basic.FloatConverter;
import com.thoughtworks.xstream.converters.SingleValueConverter;
import com.thoughtworks.xstream.converters.basic.IntConverter;
import com.thoughtworks.xstream.converters.basic.NullConverter;
import com.thoughtworks.xstream.converters.reflection.ExternalizableConverter;
import com.thoughtworks.xstream.converters.reflection.SerializableConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Calendar;
import java.util.Locale;
import java.io.File;
import java.util.Collections;
import java.util.Hashtable;
import java.util.TreeSet;
import java.util.TreeMap;
import java.util.Vector;
import java.util.LinkedList;
import java.util.SortedSet;
import java.util.Set;
import java.util.List;
import java.util.Properties;
import java.util.Map;
import java.util.BitSet;
import java.net.URL;
import java.net.URI;
import java.util.Date;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import com.thoughtworks.xstream.security.AnyTypePermission;
import java.lang.reflect.Constructor;
import com.thoughtworks.xstream.mapper.CachingMapper;
import com.thoughtworks.xstream.mapper.MapperWrapper;
import com.thoughtworks.xstream.mapper.ArrayMapper;
import com.thoughtworks.xstream.mapper.OuterClassMapper;
import com.thoughtworks.xstream.mapper.DynamicProxyMapper;
import com.thoughtworks.xstream.mapper.XStream11XmlFriendlyMapper;
import com.thoughtworks.xstream.mapper.DefaultMapper;
import com.thoughtworks.xstream.core.JVM;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.core.DefaultConverterLookup;
import com.thoughtworks.xstream.core.util.CompositeClassLoader;
import com.thoughtworks.xstream.io.xml.XppDriver;
import java.util.regex.Pattern;
import com.thoughtworks.xstream.mapper.AnnotationConfiguration;
import com.thoughtworks.xstream.mapper.SecurityMapper;
import com.thoughtworks.xstream.mapper.LocalConversionMapper;
import com.thoughtworks.xstream.mapper.ImplicitCollectionMapper;
import com.thoughtworks.xstream.mapper.ImmutableTypesMapper;
import com.thoughtworks.xstream.mapper.DefaultImplementationsMapper;
import com.thoughtworks.xstream.mapper.AttributeMapper;
import com.thoughtworks.xstream.mapper.SystemAttributeAliasingMapper;
import com.thoughtworks.xstream.mapper.AttributeAliasingMapper;
import com.thoughtworks.xstream.mapper.FieldAliasingMapper;
import com.thoughtworks.xstream.mapper.ClassAliasingMapper;
import com.thoughtworks.xstream.mapper.PackageAliasingMapper;
import com.thoughtworks.xstream.mapper.Mapper;
import com.thoughtworks.xstream.converters.ConverterRegistry;
import com.thoughtworks.xstream.converters.ConverterLookup;
import com.thoughtworks.xstream.core.ClassLoaderReference;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;

public class XStream
{
    private ReflectionProvider reflectionProvider;
    private HierarchicalStreamDriver hierarchicalStreamDriver;
    private ClassLoaderReference classLoaderReference;
    private MarshallingStrategy marshallingStrategy;
    private ConverterLookup converterLookup;
    private ConverterRegistry converterRegistry;
    private Mapper mapper;
    private PackageAliasingMapper packageAliasingMapper;
    private ClassAliasingMapper classAliasingMapper;
    private FieldAliasingMapper fieldAliasingMapper;
    private AttributeAliasingMapper attributeAliasingMapper;
    private SystemAttributeAliasingMapper systemAttributeAliasingMapper;
    private AttributeMapper attributeMapper;
    private DefaultImplementationsMapper defaultImplementationsMapper;
    private ImmutableTypesMapper immutableTypesMapper;
    private ImplicitCollectionMapper implicitCollectionMapper;
    private LocalConversionMapper localConversionMapper;
    private SecurityMapper securityMapper;
    private AnnotationConfiguration annotationConfiguration;
    public static final int NO_REFERENCES = 1001;
    public static final int ID_REFERENCES = 1002;
    public static final int XPATH_RELATIVE_REFERENCES = 1003;
    public static final int XPATH_ABSOLUTE_REFERENCES = 1004;
    public static final int SINGLE_NODE_XPATH_RELATIVE_REFERENCES = 1005;
    public static final int SINGLE_NODE_XPATH_ABSOLUTE_REFERENCES = 1006;
    public static final int PRIORITY_VERY_HIGH = 10000;
    public static final int PRIORITY_NORMAL = 0;
    public static final int PRIORITY_LOW = -10;
    public static final int PRIORITY_VERY_LOW = -20;
    private static final String ANNOTATION_MAPPER_TYPE = "com.thoughtworks.xstream.mapper.AnnotationMapper";
    private static final Pattern IGNORE_ALL;
    
    public XStream() {
        this(null, null, new XppDriver());
    }
    
    public XStream(final ReflectionProvider reflectionProvider) {
        this(reflectionProvider, null, new XppDriver());
    }
    
    public XStream(final HierarchicalStreamDriver hierarchicalStreamDriver) {
        this(null, null, hierarchicalStreamDriver);
    }
    
    public XStream(final ReflectionProvider reflectionProvider, final HierarchicalStreamDriver hierarchicalStreamDriver) {
        this(reflectionProvider, null, hierarchicalStreamDriver);
    }
    
    @Deprecated
    public XStream(final ReflectionProvider reflectionProvider, final Mapper mapper, final HierarchicalStreamDriver driver) {
        this(reflectionProvider, driver, new CompositeClassLoader(), mapper);
    }
    
    public XStream(final ReflectionProvider reflectionProvider, final HierarchicalStreamDriver driver, final ClassLoaderReference classLoaderReference) {
        this(reflectionProvider, driver, classLoaderReference, null);
    }
    
    @Deprecated
    public XStream(final ReflectionProvider reflectionProvider, final HierarchicalStreamDriver driver, final ClassLoader classLoader) {
        this(reflectionProvider, driver, classLoader, null);
    }
    
    @Deprecated
    public XStream(final ReflectionProvider reflectionProvider, final HierarchicalStreamDriver driver, final ClassLoader classLoader, final Mapper mapper) {
        this(reflectionProvider, driver, new ClassLoaderReference(classLoader), mapper, new DefaultConverterLookup());
    }
    
    public XStream(final ReflectionProvider reflectionProvider, final HierarchicalStreamDriver driver, final ClassLoaderReference classLoaderReference, final Mapper mapper) {
        this(reflectionProvider, driver, classLoaderReference, mapper, new DefaultConverterLookup());
    }
    
    private XStream(final ReflectionProvider reflectionProvider, final HierarchicalStreamDriver driver, final ClassLoaderReference classLoader, final Mapper mapper, final DefaultConverterLookup defaultConverterLookup) {
        this(reflectionProvider, driver, classLoader, mapper, new ConverterLookup() {
            public Converter lookupConverterForType(final Class type) {
                return defaultConverterLookup.lookupConverterForType(type);
            }
        }, new ConverterRegistry() {
            public void registerConverter(final Converter converter, final int priority) {
                defaultConverterLookup.registerConverter(converter, priority);
            }
        });
    }
    
    @Deprecated
    public XStream(final ReflectionProvider reflectionProvider, final HierarchicalStreamDriver driver, final ClassLoader classLoader, final Mapper mapper, final ConverterLookup converterLookup, final ConverterRegistry converterRegistry) {
        this(reflectionProvider, driver, new ClassLoaderReference(classLoader), mapper, converterLookup, converterRegistry);
    }
    
    public XStream(ReflectionProvider reflectionProvider, final HierarchicalStreamDriver driver, final ClassLoaderReference classLoaderReference, final Mapper mapper, final ConverterLookup converterLookup, final ConverterRegistry converterRegistry) {
        if (reflectionProvider == null) {
            reflectionProvider = JVM.newReflectionProvider();
        }
        this.reflectionProvider = reflectionProvider;
        this.hierarchicalStreamDriver = driver;
        this.classLoaderReference = classLoaderReference;
        this.converterLookup = converterLookup;
        this.converterRegistry = converterRegistry;
        this.mapper = ((mapper == null) ? this.buildMapper() : mapper);
        this.setupMappers();
        this.setupSecurity();
        this.setupAliases();
        this.setupDefaultImplementations();
        this.setupConverters();
        this.setupImmutableTypes();
        this.setMode(1003);
    }
    
    private Mapper buildMapper() {
        Mapper mapper = new DefaultMapper(this.classLoaderReference);
        if (this.useXStream11XmlFriendlyMapper()) {
            mapper = new XStream11XmlFriendlyMapper(mapper);
        }
        mapper = new DynamicProxyMapper(mapper);
        mapper = new PackageAliasingMapper(mapper);
        mapper = new ClassAliasingMapper(mapper);
        mapper = new FieldAliasingMapper(mapper);
        mapper = new AttributeAliasingMapper(mapper);
        mapper = new SystemAttributeAliasingMapper(mapper);
        mapper = new ImplicitCollectionMapper(mapper);
        mapper = new OuterClassMapper(mapper);
        mapper = new ArrayMapper(mapper);
        mapper = new DefaultImplementationsMapper(mapper);
        mapper = new AttributeMapper(mapper, this.converterLookup, this.reflectionProvider);
        if (JVM.is15()) {
            mapper = this.buildMapperDynamically("com.thoughtworks.xstream.mapper.EnumMapper", new Class[] { Mapper.class }, new Object[] { mapper });
        }
        mapper = new LocalConversionMapper(mapper);
        mapper = new ImmutableTypesMapper(mapper);
        if (JVM.is18()) {
            mapper = this.buildMapperDynamically("com.thoughtworks.xstream.mapper.LambdaMapper", new Class[] { Mapper.class }, new Object[] { mapper });
        }
        mapper = new SecurityMapper(mapper);
        if (JVM.is15()) {
            mapper = this.buildMapperDynamically("com.thoughtworks.xstream.mapper.AnnotationMapper", new Class[] { Mapper.class, ConverterRegistry.class, ConverterLookup.class, ClassLoaderReference.class, ReflectionProvider.class }, new Object[] { mapper, this.converterRegistry, this.converterLookup, this.classLoaderReference, this.reflectionProvider });
        }
        mapper = this.wrapMapper((MapperWrapper)mapper);
        mapper = new CachingMapper(mapper);
        return mapper;
    }
    
    private Mapper buildMapperDynamically(final String className, final Class[] constructorParamTypes, final Object[] constructorParamValues) {
        try {
            final Class type = Class.forName(className, false, this.classLoaderReference.getReference());
            final Constructor constructor = type.getConstructor((Class[])constructorParamTypes);
            return constructor.newInstance(constructorParamValues);
        }
        catch (Exception e) {
            throw new com.thoughtworks.xstream.InitializationException("Could not instantiate mapper : " + className, e);
        }
        catch (LinkageError e2) {
            throw new com.thoughtworks.xstream.InitializationException("Could not instantiate mapper : " + className, e2);
        }
    }
    
    protected MapperWrapper wrapMapper(final MapperWrapper next) {
        return next;
    }
    
    @Deprecated
    protected boolean useXStream11XmlFriendlyMapper() {
        return false;
    }
    
    private void setupMappers() {
        this.packageAliasingMapper = (PackageAliasingMapper)this.mapper.lookupMapperOfType(PackageAliasingMapper.class);
        this.classAliasingMapper = (ClassAliasingMapper)this.mapper.lookupMapperOfType(ClassAliasingMapper.class);
        this.fieldAliasingMapper = (FieldAliasingMapper)this.mapper.lookupMapperOfType(FieldAliasingMapper.class);
        this.attributeMapper = (AttributeMapper)this.mapper.lookupMapperOfType(AttributeMapper.class);
        this.attributeAliasingMapper = (AttributeAliasingMapper)this.mapper.lookupMapperOfType(AttributeAliasingMapper.class);
        this.systemAttributeAliasingMapper = (SystemAttributeAliasingMapper)this.mapper.lookupMapperOfType(SystemAttributeAliasingMapper.class);
        this.implicitCollectionMapper = (ImplicitCollectionMapper)this.mapper.lookupMapperOfType(ImplicitCollectionMapper.class);
        this.defaultImplementationsMapper = (DefaultImplementationsMapper)this.mapper.lookupMapperOfType(DefaultImplementationsMapper.class);
        this.immutableTypesMapper = (ImmutableTypesMapper)this.mapper.lookupMapperOfType(ImmutableTypesMapper.class);
        this.localConversionMapper = (LocalConversionMapper)this.mapper.lookupMapperOfType(LocalConversionMapper.class);
        this.securityMapper = (SecurityMapper)this.mapper.lookupMapperOfType(SecurityMapper.class);
        this.annotationConfiguration = (AnnotationConfiguration)this.mapper.lookupMapperOfType(AnnotationConfiguration.class);
    }
    
    protected void setupSecurity() {
        if (this.securityMapper == null) {
            return;
        }
        this.addPermission(AnyTypePermission.ANY);
    }
    
    protected void setupAliases() {
        if (this.classAliasingMapper == null) {
            return;
        }
        this.alias("null", Mapper.Null.class);
        this.alias("int", Integer.class);
        this.alias("float", Float.class);
        this.alias("double", Double.class);
        this.alias("long", Long.class);
        this.alias("short", Short.class);
        this.alias("char", Character.class);
        this.alias("byte", Byte.class);
        this.alias("boolean", Boolean.class);
        this.alias("number", Number.class);
        this.alias("object", Object.class);
        this.alias("big-int", BigInteger.class);
        this.alias("big-decimal", BigDecimal.class);
        this.alias("string-buffer", StringBuffer.class);
        this.alias("string", String.class);
        this.alias("java-class", Class.class);
        this.alias("method", Method.class);
        this.alias("constructor", Constructor.class);
        this.alias("field", Field.class);
        this.alias("date", Date.class);
        this.alias("uri", URI.class);
        this.alias("url", URL.class);
        this.alias("bit-set", BitSet.class);
        this.alias("map", Map.class);
        this.alias("entry", Map.Entry.class);
        this.alias("properties", Properties.class);
        this.alias("list", List.class);
        this.alias("set", Set.class);
        this.alias("sorted-set", SortedSet.class);
        this.alias("linked-list", LinkedList.class);
        this.alias("vector", Vector.class);
        this.alias("tree-map", TreeMap.class);
        this.alias("tree-set", TreeSet.class);
        this.alias("hashtable", Hashtable.class);
        this.alias("empty-list", Collections.EMPTY_LIST.getClass());
        this.alias("empty-map", Collections.EMPTY_MAP.getClass());
        this.alias("empty-set", Collections.EMPTY_SET.getClass());
        this.alias("singleton-list", Collections.singletonList(this).getClass());
        this.alias("singleton-map", Collections.singletonMap(this, (Object)null).getClass());
        this.alias("singleton-set", Collections.singleton(this).getClass());
        if (JVM.isAWTAvailable()) {
            this.alias("awt-color", JVM.loadClassForName("java.awt.Color", false));
            this.alias("awt-font", JVM.loadClassForName("java.awt.Font", false));
            this.alias("awt-text-attribute", JVM.loadClassForName("java.awt.font.TextAttribute"));
            final Class type = JVM.loadClassForName("javax.activation.ActivationDataFlavor");
            if (type != null) {
                this.alias("activation-data-flavor", type);
            }
        }
        if (JVM.isSQLAvailable()) {
            this.alias("sql-timestamp", JVM.loadClassForName("java.sql.Timestamp"));
            this.alias("sql-time", JVM.loadClassForName("java.sql.Time"));
            this.alias("sql-date", JVM.loadClassForName("java.sql.Date"));
        }
        this.alias("file", File.class);
        this.alias("locale", Locale.class);
        this.alias("gregorian-calendar", Calendar.class);
        if (JVM.is14()) {
            this.aliasDynamically("auth-subject", "javax.security.auth.Subject");
            this.alias("linked-hash-map", JVM.loadClassForName("java.util.LinkedHashMap"));
            this.alias("linked-hash-set", JVM.loadClassForName("java.util.LinkedHashSet"));
            this.alias("trace", JVM.loadClassForName("java.lang.StackTraceElement"));
            this.alias("currency", JVM.loadClassForName("java.util.Currency"));
            this.aliasType("charset", JVM.loadClassForName("java.nio.charset.Charset"));
        }
        if (JVM.is15()) {
            this.aliasDynamically("duration", "javax.xml.datatype.Duration");
            this.alias("concurrent-hash-map", JVM.loadClassForName("java.util.concurrent.ConcurrentHashMap"));
            this.alias("enum-set", JVM.loadClassForName("java.util.EnumSet"));
            this.alias("enum-map", JVM.loadClassForName("java.util.EnumMap"));
            this.alias("string-builder", JVM.loadClassForName("java.lang.StringBuilder"));
            this.alias("uuid", JVM.loadClassForName("java.util.UUID"));
        }
        if (JVM.is17()) {
            this.aliasType("path", JVM.loadClassForName("java.nio.file.Path"));
        }
        if (JVM.loadClassForName("java.lang.invoke.SerializedLambda") != null) {
            this.aliasDynamically("serialized-lambda", "java.lang.invoke.SerializedLambda");
        }
    }
    
    private void aliasDynamically(final String alias, final String className) {
        final Class type = JVM.loadClassForName(className);
        if (type != null) {
            this.alias(alias, type);
        }
    }
    
    protected void setupDefaultImplementations() {
        if (this.defaultImplementationsMapper == null) {
            return;
        }
        this.addDefaultImplementation(HashMap.class, Map.class);
        this.addDefaultImplementation(ArrayList.class, List.class);
        this.addDefaultImplementation(HashSet.class, Set.class);
        this.addDefaultImplementation(TreeSet.class, SortedSet.class);
        this.addDefaultImplementation(GregorianCalendar.class, Calendar.class);
    }
    
    protected void setupConverters() {
        this.registerConverter(new ReflectionConverter(this.mapper, this.reflectionProvider), -20);
        this.registerConverter(new SerializableConverter(this.mapper, this.reflectionProvider, this.classLoaderReference), -10);
        this.registerConverter(new ExternalizableConverter(this.mapper, this.classLoaderReference), -10);
        this.registerConverter(new NullConverter(), 10000);
        this.registerConverter(new IntConverter(), 0);
        this.registerConverter(new FloatConverter(), 0);
        this.registerConverter(new DoubleConverter(), 0);
        this.registerConverter(new LongConverter(), 0);
        this.registerConverter(new ShortConverter(), 0);
        this.registerConverter((Converter)new CharConverter(), 0);
        this.registerConverter(new BooleanConverter(), 0);
        this.registerConverter(new ByteConverter(), 0);
        this.registerConverter(new StringConverter(), 0);
        this.registerConverter(new StringBufferConverter(), 0);
        this.registerConverter(new DateConverter(), 0);
        this.registerConverter(new BitSetConverter(), 0);
        this.registerConverter(new URIConverter(), 0);
        this.registerConverter(new URLConverter(), 0);
        this.registerConverter(new BigIntegerConverter(), 0);
        this.registerConverter(new BigDecimalConverter(), 0);
        this.registerConverter(new ArrayConverter(this.mapper), 0);
        this.registerConverter(new CharArrayConverter(), 0);
        this.registerConverter(new CollectionConverter(this.mapper), 0);
        this.registerConverter(new MapConverter(this.mapper), 0);
        this.registerConverter(new TreeMapConverter(this.mapper), 0);
        this.registerConverter(new TreeSetConverter(this.mapper), 0);
        this.registerConverter(new SingletonCollectionConverter(this.mapper), 0);
        this.registerConverter(new SingletonMapConverter(this.mapper), 0);
        this.registerConverter(new PropertiesConverter(), 0);
        this.registerConverter((Converter)new EncodedByteArrayConverter(), 0);
        this.registerConverter(new FileConverter(), 0);
        if (JVM.isSQLAvailable()) {
            this.registerConverter(new SqlTimestampConverter(), 0);
            this.registerConverter(new SqlTimeConverter(), 0);
            this.registerConverter(new SqlDateConverter(), 0);
        }
        this.registerConverter(new DynamicProxyConverter(this.mapper, this.classLoaderReference), 0);
        this.registerConverter(new JavaClassConverter(this.classLoaderReference), 0);
        this.registerConverter(new JavaMethodConverter(this.classLoaderReference), 0);
        this.registerConverter(new JavaFieldConverter(this.classLoaderReference), 0);
        if (JVM.isAWTAvailable()) {
            this.registerConverter(new FontConverter(this.mapper), 0);
            this.registerConverter(new ColorConverter(), 0);
            this.registerConverter(new TextAttributeConverter(), 0);
        }
        if (JVM.isSwingAvailable()) {
            this.registerConverter(new LookAndFeelConverter(this.mapper, this.reflectionProvider), 0);
        }
        this.registerConverter(new LocaleConverter(), 0);
        this.registerConverter(new GregorianCalendarConverter(), 0);
        if (JVM.is14()) {
            this.registerConverterDynamically("com.thoughtworks.xstream.converters.extended.SubjectConverter", 0, new Class[] { Mapper.class }, new Object[] { this.mapper });
            this.registerConverterDynamically("com.thoughtworks.xstream.converters.extended.ThrowableConverter", 0, new Class[] { ConverterLookup.class }, new Object[] { this.converterLookup });
            this.registerConverterDynamically("com.thoughtworks.xstream.converters.extended.StackTraceElementConverter", 0, null, null);
            this.registerConverterDynamically("com.thoughtworks.xstream.converters.extended.CurrencyConverter", 0, null, null);
            this.registerConverterDynamically("com.thoughtworks.xstream.converters.extended.RegexPatternConverter", 0, null, null);
            this.registerConverterDynamically("com.thoughtworks.xstream.converters.extended.CharsetConverter", 0, null, null);
        }
        if (JVM.is15()) {
            if (JVM.loadClassForName("javax.xml.datatype.Duration") != null) {
                this.registerConverterDynamically("com.thoughtworks.xstream.converters.extended.DurationConverter", 0, null, null);
            }
            this.registerConverterDynamically("com.thoughtworks.xstream.converters.enums.EnumConverter", 0, null, null);
            this.registerConverterDynamically("com.thoughtworks.xstream.converters.enums.EnumSetConverter", 0, new Class[] { Mapper.class }, new Object[] { this.mapper });
            this.registerConverterDynamically("com.thoughtworks.xstream.converters.enums.EnumMapConverter", 0, new Class[] { Mapper.class }, new Object[] { this.mapper });
            this.registerConverterDynamically("com.thoughtworks.xstream.converters.basic.StringBuilderConverter", 0, null, null);
            this.registerConverterDynamically("com.thoughtworks.xstream.converters.basic.UUIDConverter", 0, null, null);
        }
        if (JVM.loadClassForName("javax.activation.ActivationDataFlavor") != null) {
            this.registerConverterDynamically("com.thoughtworks.xstream.converters.extended.ActivationDataFlavorConverter", 0, null, null);
        }
        if (JVM.is17()) {
            this.registerConverterDynamically("com.thoughtworks.xstream.converters.extended.PathConverter", 0, null, null);
        }
        if (JVM.is18()) {
            this.registerConverterDynamically("com.thoughtworks.xstream.converters.reflection.LambdaConverter", 0, new Class[] { Mapper.class, ReflectionProvider.class, ClassLoaderReference.class }, new Object[] { this.mapper, this.reflectionProvider, this.classLoaderReference });
        }
        this.registerConverter(new SelfStreamingInstanceChecker(this.converterLookup, this), 0);
    }
    
    private void registerConverterDynamically(final String className, final int priority, final Class[] constructorParamTypes, final Object[] constructorParamValues) {
        try {
            final Class type = Class.forName(className, false, this.classLoaderReference.getReference());
            final Constructor constructor = type.getConstructor((Class[])constructorParamTypes);
            final Object instance = constructor.newInstance(constructorParamValues);
            if (instance instanceof Converter) {
                this.registerConverter((Converter)instance, priority);
            }
            else if (instance instanceof SingleValueConverter) {
                this.registerConverter((SingleValueConverter)instance, priority);
            }
        }
        catch (Exception e) {
            throw new com.thoughtworks.xstream.InitializationException("Could not instantiate converter : " + className, e);
        }
        catch (LinkageError e2) {
            throw new com.thoughtworks.xstream.InitializationException("Could not instantiate converter : " + className, e2);
        }
    }
    
    protected void setupImmutableTypes() {
        if (this.immutableTypesMapper == null) {
            return;
        }
        this.addImmutableType(Boolean.TYPE, false);
        this.addImmutableType(Boolean.class, false);
        this.addImmutableType(Byte.TYPE, false);
        this.addImmutableType(Byte.class, false);
        this.addImmutableType(Character.TYPE, false);
        this.addImmutableType(Character.class, false);
        this.addImmutableType(Double.TYPE, false);
        this.addImmutableType(Double.class, false);
        this.addImmutableType(Float.TYPE, false);
        this.addImmutableType(Float.class, false);
        this.addImmutableType(Integer.TYPE, false);
        this.addImmutableType(Integer.class, false);
        this.addImmutableType(Long.TYPE, false);
        this.addImmutableType(Long.class, false);
        this.addImmutableType(Short.TYPE, false);
        this.addImmutableType(Short.class, false);
        this.addImmutableType(Mapper.Null.class, false);
        this.addImmutableType(BigDecimal.class, false);
        this.addImmutableType(BigInteger.class, false);
        this.addImmutableType(String.class, false);
        this.addImmutableType(URL.class, false);
        this.addImmutableType(File.class, false);
        this.addImmutableType(Class.class, false);
        if (JVM.isAWTAvailable()) {
            this.addImmutableTypeDynamically("java.awt.font.TextAttribute", false);
        }
        if (JVM.is14()) {
            this.addImmutableTypeDynamically("java.nio.charset.Charset", true);
            this.addImmutableTypeDynamically("java.util.Currency", true);
        }
        if (JVM.is15()) {
            this.addImmutableTypeDynamically("java.util.UUID", true);
        }
        this.addImmutableType(URI.class, true);
        this.addImmutableType(Collections.EMPTY_LIST.getClass(), true);
        this.addImmutableType(Collections.EMPTY_SET.getClass(), true);
        this.addImmutableType(Collections.EMPTY_MAP.getClass(), true);
    }
    
    private void addImmutableTypeDynamically(final String className, final boolean isReferenceable) {
        final Class type = JVM.loadClassForName(className);
        if (type != null) {
            this.addImmutableType(type, isReferenceable);
        }
    }
    
    public void setMarshallingStrategy(final MarshallingStrategy marshallingStrategy) {
        this.marshallingStrategy = marshallingStrategy;
    }
    
    public String toXML(final Object obj) {
        final Writer writer = new StringWriter();
        this.toXML(obj, writer);
        return writer.toString();
    }
    
    public void toXML(final Object obj, final Writer out) {
        final HierarchicalStreamWriter writer = this.hierarchicalStreamDriver.createWriter(out);
        try {
            this.marshal(obj, writer);
        }
        finally {
            writer.flush();
        }
    }
    
    public void toXML(final Object obj, final OutputStream out) {
        final HierarchicalStreamWriter writer = this.hierarchicalStreamDriver.createWriter(out);
        try {
            this.marshal(obj, writer);
        }
        finally {
            writer.flush();
        }
    }
    
    public void marshal(final Object obj, final HierarchicalStreamWriter writer) {
        this.marshal(obj, writer, null);
    }
    
    public void marshal(final Object obj, final HierarchicalStreamWriter writer, final DataHolder dataHolder) {
        this.marshallingStrategy.marshal(writer, obj, this.converterLookup, this.mapper, dataHolder);
    }
    
    public Object fromXML(final String xml) {
        return this.fromXML(new StringReader(xml));
    }
    
    public Object fromXML(final Reader reader) {
        return this.unmarshal(this.hierarchicalStreamDriver.createReader(reader), null);
    }
    
    public Object fromXML(final InputStream input) {
        return this.unmarshal(this.hierarchicalStreamDriver.createReader(input), null);
    }
    
    public Object fromXML(final URL url) {
        return this.fromXML(url, null);
    }
    
    public Object fromXML(final File file) {
        return this.fromXML(file, null);
    }
    
    public Object fromXML(final String xml, final Object root) {
        return this.fromXML(new StringReader(xml), root);
    }
    
    public Object fromXML(final Reader xml, final Object root) {
        return this.unmarshal(this.hierarchicalStreamDriver.createReader(xml), root);
    }
    
    public Object fromXML(final URL url, final Object root) {
        return this.unmarshal(this.hierarchicalStreamDriver.createReader(url), root);
    }
    
    public Object fromXML(final File file, final Object root) {
        final HierarchicalStreamReader reader = this.hierarchicalStreamDriver.createReader(file);
        try {
            return this.unmarshal(reader, root);
        }
        finally {
            reader.close();
        }
    }
    
    public Object fromXML(final InputStream input, final Object root) {
        return this.unmarshal(this.hierarchicalStreamDriver.createReader(input), root);
    }
    
    public Object unmarshal(final HierarchicalStreamReader reader) {
        return this.unmarshal(reader, null, null);
    }
    
    public Object unmarshal(final HierarchicalStreamReader reader, final Object root) {
        return this.unmarshal(reader, root, null);
    }
    
    public Object unmarshal(final HierarchicalStreamReader reader, final Object root, final DataHolder dataHolder) {
        try {
            return this.marshallingStrategy.unmarshal(root, reader, dataHolder, this.converterLookup, this.mapper);
        }
        catch (ConversionException e) {
            final Package pkg = this.getClass().getPackage();
            final String version = (pkg != null) ? pkg.getImplementationVersion() : null;
            e.add("version", (version != null) ? version : "not available");
            throw e;
        }
    }
    
    public void alias(final String name, final Class type) {
        if (this.classAliasingMapper == null) {
            throw new com.thoughtworks.xstream.InitializationException("No " + ClassAliasingMapper.class.getName() + " available");
        }
        this.classAliasingMapper.addClassAlias(name, type);
    }
    
    public void aliasType(final String name, final Class type) {
        if (this.classAliasingMapper == null) {
            throw new com.thoughtworks.xstream.InitializationException("No " + ClassAliasingMapper.class.getName() + " available");
        }
        this.classAliasingMapper.addTypeAlias(name, type);
    }
    
    public void alias(final String name, final Class type, final Class defaultImplementation) {
        this.alias(name, type);
        this.addDefaultImplementation(defaultImplementation, type);
    }
    
    public void aliasPackage(final String name, final String pkgName) {
        if (this.packageAliasingMapper == null) {
            throw new com.thoughtworks.xstream.InitializationException("No " + PackageAliasingMapper.class.getName() + " available");
        }
        this.packageAliasingMapper.addPackageAlias(name, pkgName);
    }
    
    public void aliasField(final String alias, final Class definedIn, final String fieldName) {
        if (this.fieldAliasingMapper == null) {
            throw new com.thoughtworks.xstream.InitializationException("No " + FieldAliasingMapper.class.getName() + " available");
        }
        this.fieldAliasingMapper.addFieldAlias(alias, definedIn, fieldName);
    }
    
    public void aliasAttribute(final String alias, final String attributeName) {
        if (this.attributeAliasingMapper == null) {
            throw new com.thoughtworks.xstream.InitializationException("No " + AttributeAliasingMapper.class.getName() + " available");
        }
        this.attributeAliasingMapper.addAliasFor(attributeName, alias);
    }
    
    public void aliasSystemAttribute(final String alias, final String systemAttributeName) {
        if (this.systemAttributeAliasingMapper == null) {
            throw new com.thoughtworks.xstream.InitializationException("No " + SystemAttributeAliasingMapper.class.getName() + " available");
        }
        this.systemAttributeAliasingMapper.addAliasFor(systemAttributeName, alias);
    }
    
    public void aliasAttribute(final Class definedIn, final String attributeName, final String alias) {
        this.aliasField(alias, definedIn, attributeName);
        this.useAttributeFor(definedIn, attributeName);
    }
    
    public void useAttributeFor(final String fieldName, final Class type) {
        if (this.attributeMapper == null) {
            throw new com.thoughtworks.xstream.InitializationException("No " + AttributeMapper.class.getName() + " available");
        }
        this.attributeMapper.addAttributeFor(fieldName, type);
    }
    
    public void useAttributeFor(final Class definedIn, final String fieldName) {
        if (this.attributeMapper == null) {
            throw new com.thoughtworks.xstream.InitializationException("No " + AttributeMapper.class.getName() + " available");
        }
        this.attributeMapper.addAttributeFor(definedIn, fieldName);
    }
    
    public void useAttributeFor(final Class type) {
        if (this.attributeMapper == null) {
            throw new com.thoughtworks.xstream.InitializationException("No " + AttributeMapper.class.getName() + " available");
        }
        this.attributeMapper.addAttributeFor(type);
    }
    
    public void addDefaultImplementation(final Class defaultImplementation, final Class ofType) {
        if (this.defaultImplementationsMapper == null) {
            throw new com.thoughtworks.xstream.InitializationException("No " + DefaultImplementationsMapper.class.getName() + " available");
        }
        this.defaultImplementationsMapper.addDefaultImplementation(defaultImplementation, ofType);
    }
    
    @Deprecated
    public void addImmutableType(final Class type) {
        this.addImmutableType(type, true);
    }
    
    public void addImmutableType(final Class type, final boolean isReferenceable) {
        if (this.immutableTypesMapper == null) {
            throw new com.thoughtworks.xstream.InitializationException("No " + ImmutableTypesMapper.class.getName() + " available");
        }
        this.immutableTypesMapper.addImmutableType(type, isReferenceable);
    }
    
    public void registerConverter(final Converter converter) {
        this.registerConverter(converter, 0);
    }
    
    public void registerConverter(final Converter converter, final int priority) {
        if (this.converterRegistry != null) {
            this.converterRegistry.registerConverter(converter, priority);
        }
    }
    
    public void registerConverter(final SingleValueConverter converter) {
        this.registerConverter(converter, 0);
    }
    
    public void registerConverter(final SingleValueConverter converter, final int priority) {
        if (this.converterRegistry != null) {
            this.converterRegistry.registerConverter(new SingleValueConverterWrapper(converter), priority);
        }
    }
    
    public void registerLocalConverter(final Class definedIn, final String fieldName, final Converter converter) {
        if (this.localConversionMapper == null) {
            throw new com.thoughtworks.xstream.InitializationException("No " + LocalConversionMapper.class.getName() + " available");
        }
        this.localConversionMapper.registerLocalConverter(definedIn, fieldName, converter);
    }
    
    public void registerLocalConverter(final Class definedIn, final String fieldName, final SingleValueConverter converter) {
        this.registerLocalConverter(definedIn, fieldName, (Converter)new SingleValueConverterWrapper(converter));
    }
    
    public Mapper getMapper() {
        return this.mapper;
    }
    
    public ReflectionProvider getReflectionProvider() {
        return this.reflectionProvider;
    }
    
    public ConverterLookup getConverterLookup() {
        return this.converterLookup;
    }
    
    public void setMode(final int mode) {
        switch (mode) {
            case 1001: {
                this.setMarshallingStrategy(new TreeMarshallingStrategy());
                break;
            }
            case 1002: {
                this.setMarshallingStrategy(new ReferenceByIdMarshallingStrategy());
                break;
            }
            case 1003: {
                this.setMarshallingStrategy(new ReferenceByXPathMarshallingStrategy(ReferenceByXPathMarshallingStrategy.RELATIVE));
                break;
            }
            case 1004: {
                this.setMarshallingStrategy(new ReferenceByXPathMarshallingStrategy(ReferenceByXPathMarshallingStrategy.ABSOLUTE));
                break;
            }
            case 1005: {
                this.setMarshallingStrategy(new ReferenceByXPathMarshallingStrategy(ReferenceByXPathMarshallingStrategy.RELATIVE | ReferenceByXPathMarshallingStrategy.SINGLE_NODE));
                break;
            }
            case 1006: {
                this.setMarshallingStrategy(new ReferenceByXPathMarshallingStrategy(ReferenceByXPathMarshallingStrategy.ABSOLUTE | ReferenceByXPathMarshallingStrategy.SINGLE_NODE));
                break;
            }
            default: {
                throw new IllegalArgumentException("Unknown mode : " + mode);
            }
        }
    }
    
    public void addImplicitCollection(final Class ownerType, final String fieldName) {
        this.addImplicitCollection(ownerType, fieldName, null, null);
    }
    
    public void addImplicitCollection(final Class ownerType, final String fieldName, final Class itemType) {
        this.addImplicitCollection(ownerType, fieldName, null, itemType);
    }
    
    public void addImplicitCollection(final Class ownerType, final String fieldName, final String itemFieldName, final Class itemType) {
        this.addImplicitMap(ownerType, fieldName, itemFieldName, itemType, null);
    }
    
    public void addImplicitArray(final Class ownerType, final String fieldName) {
        this.addImplicitCollection(ownerType, fieldName);
    }
    
    public void addImplicitArray(final Class ownerType, final String fieldName, final Class itemType) {
        this.addImplicitCollection(ownerType, fieldName, itemType);
    }
    
    public void addImplicitArray(final Class ownerType, final String fieldName, final String itemName) {
        this.addImplicitCollection(ownerType, fieldName, itemName, null);
    }
    
    public void addImplicitMap(final Class ownerType, final String fieldName, final Class itemType, final String keyFieldName) {
        this.addImplicitMap(ownerType, fieldName, null, itemType, keyFieldName);
    }
    
    public void addImplicitMap(final Class ownerType, final String fieldName, final String itemName, final Class itemType, final String keyFieldName) {
        if (this.implicitCollectionMapper == null) {
            throw new com.thoughtworks.xstream.InitializationException("No " + ImplicitCollectionMapper.class.getName() + " available");
        }
        this.implicitCollectionMapper.add(ownerType, fieldName, itemName, itemType, keyFieldName);
    }
    
    public DataHolder newDataHolder() {
        return new MapBackedDataHolder();
    }
    
    public ObjectOutputStream createObjectOutputStream(final Writer writer) throws IOException {
        return this.createObjectOutputStream(this.hierarchicalStreamDriver.createWriter(writer), "object-stream");
    }
    
    public ObjectOutputStream createObjectOutputStream(final HierarchicalStreamWriter writer) throws IOException {
        return this.createObjectOutputStream(writer, "object-stream");
    }
    
    public ObjectOutputStream createObjectOutputStream(final Writer writer, final String rootNodeName) throws IOException {
        return this.createObjectOutputStream(this.hierarchicalStreamDriver.createWriter(writer), rootNodeName);
    }
    
    public ObjectOutputStream createObjectOutputStream(final OutputStream out) throws IOException {
        return this.createObjectOutputStream(this.hierarchicalStreamDriver.createWriter(out), "object-stream");
    }
    
    public ObjectOutputStream createObjectOutputStream(final OutputStream out, final String rootNodeName) throws IOException {
        return this.createObjectOutputStream(this.hierarchicalStreamDriver.createWriter(out), rootNodeName);
    }
    
    public ObjectOutputStream createObjectOutputStream(final HierarchicalStreamWriter writer, final String rootNodeName) throws IOException {
        final StatefulWriter statefulWriter = new StatefulWriter(writer);
        statefulWriter.startNode(rootNodeName, null);
        return new CustomObjectOutputStream(new CustomObjectOutputStream.StreamCallback() {
            public void writeToStream(final Object object) {
                XStream.this.marshal(object, statefulWriter);
            }
            
            public void writeFieldsToStream(final Map fields) throws NotActiveException {
                throw new NotActiveException("not in call to writeObject");
            }
            
            public void defaultWriteObject() throws NotActiveException {
                throw new NotActiveException("not in call to writeObject");
            }
            
            public void flush() {
                statefulWriter.flush();
            }
            
            public void close() {
                if (statefulWriter.state() != StatefulWriter.STATE_CLOSED) {
                    statefulWriter.endNode();
                    statefulWriter.close();
                }
            }
        });
    }
    
    public ObjectInputStream createObjectInputStream(final Reader xmlReader) throws IOException {
        return this.createObjectInputStream(this.hierarchicalStreamDriver.createReader(xmlReader));
    }
    
    public ObjectInputStream createObjectInputStream(final InputStream in) throws IOException {
        return this.createObjectInputStream(this.hierarchicalStreamDriver.createReader(in));
    }
    
    public ObjectInputStream createObjectInputStream(final HierarchicalStreamReader reader) throws IOException {
        return new CustomObjectInputStream(new CustomObjectInputStream.StreamCallback() {
            public Object readFromStream() throws EOFException {
                if (!reader.hasMoreChildren()) {
                    throw new EOFException();
                }
                reader.moveDown();
                final Object result = XStream.this.unmarshal(reader);
                reader.moveUp();
                return result;
            }
            
            public Map readFieldsFromStream() throws IOException {
                throw new NotActiveException("not in call to readObject");
            }
            
            public void defaultReadObject() throws NotActiveException {
                throw new NotActiveException("not in call to readObject");
            }
            
            public void registerValidation(final ObjectInputValidation validation, final int priority) throws NotActiveException {
                throw new NotActiveException("stream inactive");
            }
            
            public void close() {
                reader.close();
            }
        }, this.classLoaderReference);
    }
    
    public void setClassLoader(final ClassLoader classLoader) {
        this.classLoaderReference.setReference(classLoader);
    }
    
    public ClassLoader getClassLoader() {
        return this.classLoaderReference.getReference();
    }
    
    public ClassLoaderReference getClassLoaderReference() {
        return this.classLoaderReference;
    }
    
    public void omitField(final Class definedIn, final String fieldName) {
        if (this.fieldAliasingMapper == null) {
            throw new com.thoughtworks.xstream.InitializationException("No " + FieldAliasingMapper.class.getName() + " available");
        }
        this.fieldAliasingMapper.omitField(definedIn, fieldName);
    }
    
    public void ignoreUnknownElements() {
        this.ignoreUnknownElements(XStream.IGNORE_ALL);
    }
    
    public void ignoreUnknownElements(final String pattern) {
        this.ignoreUnknownElements(Pattern.compile(pattern));
    }
    
    public void ignoreUnknownElements(final Pattern pattern) {
        if (this.fieldAliasingMapper == null) {
            throw new com.thoughtworks.xstream.InitializationException("No " + FieldAliasingMapper.class.getName() + " available");
        }
        this.fieldAliasingMapper.addFieldsToIgnore(pattern);
    }
    
    public void processAnnotations(final Class[] types) {
        if (this.annotationConfiguration == null) {
            throw new com.thoughtworks.xstream.InitializationException("No com.thoughtworks.xstream.mapper.AnnotationMapper available");
        }
        this.annotationConfiguration.processAnnotations(types);
    }
    
    public void processAnnotations(final Class type) {
        this.processAnnotations(new Class[] { type });
    }
    
    public void autodetectAnnotations(final boolean mode) {
        if (this.annotationConfiguration != null) {
            this.annotationConfiguration.autodetectAnnotations(mode);
        }
    }
    
    public void addPermission(final TypePermission permission) {
        if (this.securityMapper != null) {
            this.securityMapper.addPermission(permission);
        }
    }
    
    public void allowTypes(final String[] names) {
        this.addPermission(new ExplicitTypePermission(names));
    }
    
    public void allowTypes(final Class[] types) {
        this.addPermission(new ExplicitTypePermission(types));
    }
    
    public void allowTypeHierarchy(final Class type) {
        this.addPermission(new TypeHierarchyPermission(type));
    }
    
    public void allowTypesByRegExp(final String[] regexps) {
        this.addPermission(new RegExpTypePermission(regexps));
    }
    
    public void allowTypesByRegExp(final Pattern[] regexps) {
        this.addPermission(new RegExpTypePermission(regexps));
    }
    
    public void allowTypesByWildcard(final String[] patterns) {
        this.addPermission(new WildcardTypePermission(patterns));
    }
    
    public void denyPermission(final TypePermission permission) {
        this.addPermission(new NoPermission(permission));
    }
    
    public void denyTypes(final String[] names) {
        this.denyPermission(new ExplicitTypePermission(names));
    }
    
    public void denyTypes(final Class[] types) {
        this.denyPermission(new ExplicitTypePermission(types));
    }
    
    public void denyTypeHierarchy(final Class type) {
        this.denyPermission(new TypeHierarchyPermission(type));
    }
    
    public void denyTypesByRegExp(final String[] regexps) {
        this.denyPermission(new RegExpTypePermission(regexps));
    }
    
    public void denyTypesByRegExp(final Pattern[] regexps) {
        this.denyPermission(new RegExpTypePermission(regexps));
    }
    
    public void denyTypesByWildcard(final String[] patterns) {
        this.denyPermission(new WildcardTypePermission(patterns));
    }
    
    static {
        IGNORE_ALL = Pattern.compile(".*");
    }
    
    public static class InitializationException extends XStreamException
    {
        @Deprecated
        public InitializationException(final String message, final Throwable cause) {
            super(message, cause);
        }
        
        @Deprecated
        public InitializationException(final String message) {
            super(message);
        }
    }
}
