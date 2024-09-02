// 
// Decompiled by Procyon v0.5.36
// 

package org.jruby;

import java.util.Set;
import org.jruby.util.SafePropertyAccessor;
import org.jruby.util.log.LoggerFactory;
import java.util.concurrent.Callable;
import org.jruby.runtime.profile.builtin.ProfiledMethods;
import java.lang.invoke.MethodHandles;
import org.jruby.runtime.invokedynamic.InvokeDynamicSupport;
import java.lang.invoke.MethodType;
import org.jruby.util.StrptimeParser;
import jnr.constants.ConstantSet;
import java.util.IdentityHashMap;
import java.net.BindException;
import org.jruby.runtime.Binding;
import org.jruby.runtime.profile.ProfilingService;
import org.jruby.runtime.profile.ProfileCollection;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Arrays;
import org.jruby.util.StringSupport;
import org.jruby.runtime.load.BasicLibraryService;
import org.jruby.parser.StaticScope;
import java.io.ByteArrayOutputStream;
import org.jruby.exceptions.RaiseException;
import java.io.FileDescriptor;
import org.jruby.javasupport.JavaClass;
import org.jruby.javasupport.JavaPackage;
import org.jruby.util.IOInputStream;
import org.jruby.util.IOOutputStream;
import org.jruby.util.io.EncodingUtils;
import java.nio.charset.Charset;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.parser.ParserConfiguration;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRReader;
import org.jruby.ir.persistence.IRReaderStream;
import org.jruby.ir.persistence.util.IRFileExpert;
import org.jruby.ext.jruby.JRubyUtilLibrary;
import org.jruby.ext.jruby.JRubyLibrary;
import org.jruby.javasupport.Java;
import jnr.constants.Constant;
import jnr.constants.platform.Errno;
import org.jruby.javasupport.JavaSupportImpl;
import org.jruby.common.IRubyWarnings;
import org.jruby.util.RubyStringBuilder;
import org.jruby.runtime.CallSite;
import org.jruby.embed.Extension;
import org.jruby.util.func.Function1;
import java.util.Enumeration;
import org.jruby.ir.runtime.IRReturnJump;
import org.jruby.compiler.NotCompilableException;
import org.jruby.ir.Compiler;
import org.jruby.util.ClassDefiningClassLoader;
import org.jruby.util.ClassDefiningJRubyClassLoader;
import org.jruby.ast.executable.ScriptAndCode;
import org.jruby.org.objectweb.asm.ClassVisitor;
import org.jruby.org.objectweb.asm.ClassReader;
import org.jruby.org.objectweb.asm.util.TraceClassVisitor;
import java.io.OutputStream;
import java.io.PrintWriter;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.ast.FCallNode;
import org.jruby.ast.ArrayNode;
import org.jruby.ast.ListNode;
import org.jruby.ast.CallNode;
import org.jruby.ast.WhileNode;
import org.jruby.ast.VCallNode;
import org.jruby.ast.GlobalVarNode;
import org.jruby.ast.GlobalAsgnNode;
import org.jruby.ast.StrNode;
import org.jruby.util.CommonByteLists;
import org.jruby.ast.BlockNode;
import org.jruby.ast.executable.Script;
import org.jruby.runtime.Helpers;
import org.jruby.util.cli.Options;
import org.jruby.runtime.IAccessor;
import java.io.File;
import org.jruby.runtime.load.CompiledScriptLoader;
import org.jruby.ast.Node;
import java.io.IOException;
import org.jruby.internal.runtime.ValueAccessor;
import java.io.ByteArrayInputStream;
import org.jruby.ast.RootNode;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.scope.ManyVarsDynamicScope;
import java.util.Iterator;
import org.jruby.management.CachesMBean;
import org.jruby.management.ParserStatsMBean;
import org.jruby.management.ConfigMBean;
import org.jruby.compiler.JITCompilerMBean;
import java.security.SecureRandom;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.platform.Platform;
import org.jruby.exceptions.MainExitException;
import org.jruby.util.ByteList;
import org.jruby.util.SelfFirstJRubyClassLoader;
import org.jruby.runtime.Block;
import org.jruby.internal.runtime.methods.JavaMethod;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.ThreadContext;
import org.jruby.util.SecurityHelper;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.IRScope;
import java.util.Collections;
import org.jruby.ir.IRScriptBody;
import org.jruby.ext.fiber.ThreadFiber;
import org.jruby.ext.fiber.ThreadFiberLibrary;
import org.jruby.ext.thread.SizedQueue;
import org.jruby.ext.thread.Queue;
import org.jruby.ext.thread.ConditionVariable;
import org.jruby.ext.thread.Mutex;
import org.jruby.ext.tracepoint.TracePoint;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import org.jruby.threading.DaemonThreadFactory;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import jnr.posix.POSIXHandler;
import jnr.posix.POSIXFactory;
import org.jruby.ext.JRubyPOSIXHandler;
import org.jruby.runtime.invokedynamic.MethodNames;
import org.jruby.runtime.ClassIndex;
import org.jruby.management.BeanManagerFactory;
import java.util.WeakHashMap;
import org.jruby.runtime.opto.OptoFactory;
import java.util.function.Supplier;
import java.util.HashMap;
import org.jruby.util.StrptimeToken;
import java.util.List;
import java.util.function.Consumer;
import org.jruby.util.MRIRecursionGuard;
import org.jruby.runtime.JavaSites;
import org.jruby.anno.TypePopulator;
import java.lang.invoke.MethodHandle;
import org.jruby.ir.interpreter.Interpreter;
import org.jruby.util.io.FilenoUtil;
import org.jruby.management.Runtime;
import org.jruby.management.Config;
import java.lang.ref.WeakReference;
import org.jruby.util.DefinedMessage;
import java.util.EnumMap;
import org.jruby.runtime.profile.ProfilingServiceLookup;
import org.jruby.javasupport.proxy.JavaProxyClassFactory;
import org.jruby.ext.ffi.FFI;
import org.jruby.ir.IRManager;
import org.jruby.parser.StaticScopeFactory;
import java.util.Random;
import org.jruby.ext.coverage.CoverageData;
import org.jruby.ast.executable.RuntimeCache;
import org.jruby.util.io.SelectorPool;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ExecutorService;
import org.joda.time.DateTimeZone;
import org.jruby.util.collections.ConcurrentWeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.jruby.util.KCode;
import java.util.Stack;
import org.joni.WarnCallback;
import org.jruby.common.RubyWarnings;
import org.jruby.internal.runtime.GlobalVariables;
import org.jruby.runtime.encoding.EncodingService;
import org.jcodings.Encoding;
import org.jruby.runtime.load.LoadService;
import org.jruby.parser.Parser;
import org.jruby.management.Caches;
import org.jruby.compiler.JITCompiler;
import org.jruby.management.InlineStats;
import org.jruby.management.ParserStats;
import org.jruby.management.BeanManager;
import org.jruby.util.JRubyClassLoader;
import org.jruby.javasupport.JavaSupport;
import java.io.PrintStream;
import java.io.InputStream;
import org.jruby.runtime.GlobalVariable;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.EventHook;
import org.jruby.runtime.ObjectSpace;
import jnr.posix.POSIX;
import org.jruby.internal.runtime.ThreadService;
import org.jruby.runtime.opto.Invalidator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.jruby.runtime.RubyEvent;
import java.util.EnumSet;
import java.util.Map;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.log.Logger;
import org.jruby.compiler.Constantizable;

public final class Ruby implements Constantizable
{
    private static final Logger LOG;
    public static final int NIL_PREFILLED_ARRAY_SIZE = 128;
    private final IRubyObject[] nilPrefilledArray;
    private final Map<Integer, RubyClass> errnos;
    RubyRandom.RandomType defaultRand;
    private RubyHash charsetMap;
    static final String ROOT_FRAME_NAME = "(root)";
    private static final EnumSet<RubyEvent> interest;
    private final CallTraceFuncHook callTraceFuncHook;
    private static final Pattern ADDR_NOT_AVAIL_PATTERN;
    private final ThreadLocal<Map<Object, Object>> inspect;
    private final ThreadLocal<FStringEqual> DEDUP_WRAPPER_CACHE;
    @Deprecated
    private static final RecursiveFunctionEx<RecursiveFunction> LEGACY_RECURSE;
    private final ConcurrentHashMap<String, Invalidator> constantNameInvalidators;
    private final Invalidator checkpointInvalidator;
    private ThreadService threadService;
    private final POSIX posix;
    private final ObjectSpace objectSpace;
    private final RubySymbol.SymbolTable symbolTable;
    private static final EventHook[] EMPTY_HOOKS;
    private volatile EventHook[] eventHooks;
    private boolean hasEventHooks;
    private boolean globalAbortOnExceptionEnabled;
    private IRubyObject reportOnException;
    private boolean doNotReverseLookupEnabled;
    private volatile boolean objectSpaceEnabled;
    private boolean siphashEnabled;
    @Deprecated
    private long globalState;
    private final IRubyObject topSelf;
    private final RubyNil nilObject;
    private final IRubyObject[] singleNilArray;
    private final RubyBoolean trueObject;
    private final RubyBoolean falseObject;
    final RubyFixnum[] fixnumCache;
    final Object[] fixnumConstants;
    @Deprecated
    private IRubyObject rootFiber;
    private boolean verbose;
    private boolean warningsEnabled;
    private boolean debug;
    private IRubyObject verboseValue;
    private RubyThreadGroup defaultThreadGroup;
    private final RubyClass basicObjectClass;
    private final RubyClass objectClass;
    private final RubyClass moduleClass;
    private final RubyClass classClass;
    private final RubyClass nilClass;
    private final RubyClass trueClass;
    private final RubyClass falseClass;
    private final RubyClass numericClass;
    private final RubyClass floatClass;
    private final RubyClass integerClass;
    private final RubyClass fixnumClass;
    private final RubyClass complexClass;
    private final RubyClass rationalClass;
    private final RubyClass enumeratorClass;
    private final RubyClass yielderClass;
    private final RubyClass fiberClass;
    private final RubyClass generatorClass;
    private final RubyClass arrayClass;
    private final RubyClass hashClass;
    private final RubyClass rangeClass;
    private final RubyClass stringClass;
    private final RubyClass encodingClass;
    private final RubyClass converterClass;
    private final RubyClass symbolClass;
    private final RubyClass procClass;
    private final RubyClass bindingClass;
    private final RubyClass methodClass;
    private final RubyClass unboundMethodClass;
    private final RubyClass matchDataClass;
    private final RubyClass regexpClass;
    private final RubyClass timeClass;
    private final RubyClass bignumClass;
    private final RubyClass dirClass;
    private final RubyClass fileClass;
    private final RubyClass fileStatClass;
    private final RubyClass ioClass;
    private final RubyClass threadClass;
    private final RubyClass threadGroupClass;
    private final RubyClass continuationClass;
    private final RubyClass structClass;
    private final RubyClass exceptionClass;
    private final RubyClass dummyClass;
    private final RubyClass randomClass;
    private final RubyClass dataClass;
    private RubyClass tmsStruct;
    private RubyClass passwdStruct;
    private RubyClass groupStruct;
    private RubyClass procStatusClass;
    private RubyClass runtimeError;
    private RubyClass frozenError;
    private RubyClass ioError;
    private RubyClass scriptError;
    private RubyClass nameError;
    private RubyClass nameErrorMessage;
    private RubyClass noMethodError;
    private RubyClass signalException;
    private RubyClass rangeError;
    private RubyClass systemExit;
    private RubyClass localJumpError;
    private RubyClass nativeException;
    private RubyClass systemCallError;
    private RubyClass fatal;
    private RubyClass interrupt;
    private RubyClass typeError;
    private RubyClass argumentError;
    private RubyClass uncaughtThrowError;
    private RubyClass indexError;
    private RubyClass stopIteration;
    private RubyClass syntaxError;
    private RubyClass standardError;
    private RubyClass loadError;
    private RubyClass notImplementedError;
    private RubyClass securityError;
    private RubyClass noMemoryError;
    private RubyClass regexpError;
    private RubyClass eofError;
    private RubyClass threadError;
    private RubyClass concurrencyError;
    private RubyClass systemStackError;
    private RubyClass zeroDivisionError;
    private RubyClass floatDomainError;
    private RubyClass mathDomainError;
    private RubyClass encodingError;
    private RubyClass encodingCompatibilityError;
    private RubyClass converterNotFoundError;
    private RubyClass undefinedConversionError;
    private RubyClass invalidByteSequenceError;
    private RubyClass fiberError;
    private RubyClass keyError;
    private RubyClass locationClass;
    private RubyClass interruptedRegexpError;
    private final RubyModule kernelModule;
    private final RubyModule comparableModule;
    private final RubyModule enumerableModule;
    private final RubyModule mathModule;
    private final RubyModule marshalModule;
    private final RubyModule fileTestModule;
    private final RubyModule gcModule;
    private final RubyModule objectSpaceModule;
    private final RubyModule processModule;
    private final RubyModule warningModule;
    private RubyModule etcModule;
    private RubyModule procUIDModule;
    private RubyModule procGIDModule;
    private RubyModule procSysModule;
    private RubyModule precisionModule;
    private RubyModule errnoModule;
    private DynamicMethod privateMethodMissing;
    private DynamicMethod protectedMethodMissing;
    private DynamicMethod variableMethodMissing;
    private DynamicMethod superMethodMissing;
    private DynamicMethod normalMethodMissing;
    private DynamicMethod defaultMethodMissing;
    private DynamicMethod defaultModuleMethodMissing;
    private DynamicMethod respondTo;
    private DynamicMethod respondToMissing;
    private GlobalVariable recordSeparatorVar;
    private volatile String currentDirectory;
    private volatile int currentLine;
    private volatile IRubyObject argsFile;
    private final long startTime;
    final RubyInstanceConfig config;
    private InputStream in;
    private PrintStream out;
    private PrintStream err;
    private JavaSupport javaSupport;
    private final JRubyClassLoader jrubyClassLoader;
    private final BeanManager beanManager;
    private final ParserStats parserStats;
    private InlineStats inlineStats;
    private final JITCompiler jitCompiler;
    private final Caches caches;
    private static volatile boolean securityRestricted;
    private final Parser parser;
    private final LoadService loadService;
    private Encoding defaultInternalEncoding;
    private Encoding defaultExternalEncoding;
    private Encoding defaultFilesystemEncoding;
    private final EncodingService encodingService;
    private final GlobalVariables globalVariables;
    private final RubyWarnings warnings;
    private final WarnCallback regexpWarnings;
    private final Stack<RubyProc> atExitBlocks;
    private Profile profile;
    private KCode kcode;
    private final AtomicInteger symbolLastId;
    private final AtomicInteger moduleLastId;
    private final ConcurrentWeakHashMap<RubyModule, Object> allModules;
    private final Map<String, DateTimeZone> timeZoneCache;
    private Map<Finalizable, Object> finalizers;
    private Map<Finalizable, Object> internalFinalizers;
    private final Object finalizersMutex;
    private final Object internalFinalizersMutex;
    private final ExecutorService executor;
    private final ExecutorService fiberExecutor;
    private final Object hierarchyLock;
    private final AtomicLong dynamicMethodSerial;
    private final AtomicInteger moduleGeneration;
    private final Map<String, Map<String, String>> boundMethods;
    private final SelectorPool selectorPool;
    private final RuntimeCache runtimeCache;
    public static final String ERRNO_BACKTRACE_MESSAGE = "errno backtraces disabled; run with -Xerrno.backtrace=true to enable";
    public static final String STOPIERATION_BACKTRACE_MESSAGE = "StopIteration backtraces disabled; run with -Xstop_iteration.backtrace=true to enable";
    private final AtomicInteger exceptionCount;
    private final AtomicInteger backtraceCount;
    private final AtomicInteger callerCount;
    private final AtomicInteger warningCount;
    private final Invalidator fixnumInvalidator;
    private final Invalidator floatInvalidator;
    private boolean fixnumReopened;
    private boolean floatReopened;
    private final boolean coreIsBooted;
    private final boolean runtimeIsBooted;
    private RubyHash envObject;
    private final CoverageData coverageData;
    private static volatile Ruby globalRuntime;
    private static final ThreadLocal<Ruby> threadLocalRuntime;
    final Random random;
    private final long hashSeedK0;
    private final long hashSeedK1;
    private final StaticScopeFactory staticScopeFactory;
    private final IRManager irManager;
    private FFI ffi;
    private JavaProxyClassFactory javaProxyClassFactory;
    private final ProfilingServiceLookup profilingServiceLookup;
    private final EnumMap<DefinedMessage, RubyString> definedMessages;
    private final EnumMap<RubyThread.Status, RubyString> threadStatuses;
    private static final ObjectSpacer DISABLED_OBJECTSPACE;
    private static final ObjectSpacer ENABLED_OBJECTSPACE;
    private final ObjectSpacer objectSpacer;
    private final RubyArray emptyFrozenArray;
    private final ConcurrentHashMap<FStringEqual, WeakReference<RubyString>> dedupMap;
    private static final AtomicInteger RUNTIME_NUMBER;
    private final int runtimeNumber;
    private final Config configBean;
    private final Runtime runtimeBean;
    private final FilenoUtil filenoUtil;
    private final Interpreter interpreter;
    private final Object constant;
    private DynamicMethod baseNewMethod;
    private MethodHandle nullToNil;
    public final ClassValue<TypePopulator> POPULATORS;
    public final JavaSites sites;
    private volatile MRIRecursionGuard mriRecursionGuard;
    private final Map<Class, Consumer<RubyModule>> javaExtensionDefinitions;
    private final Map<String, List<StrptimeToken>> strptimeFormatCache;
    transient RubyString tzVar;
    
    private Ruby(final RubyInstanceConfig config) {
        this.errnos = new HashMap<Integer, RubyClass>();
        this.callTraceFuncHook = new CallTraceFuncHook();
        this.inspect = new ThreadLocal<Map<Object, Object>>();
        this.DEDUP_WRAPPER_CACHE = ThreadLocal.withInitial((Supplier<? extends FStringEqual>)FStringEqual::new);
        this.constantNameInvalidators = new ConcurrentHashMap<String, Invalidator>(16, 0.75f, 1);
        this.objectSpace = new ObjectSpace();
        this.symbolTable = new RubySymbol.SymbolTable(this);
        this.eventHooks = Ruby.EMPTY_HOOKS;
        this.globalAbortOnExceptionEnabled = false;
        this.doNotReverseLookupEnabled = false;
        this.globalState = 1L;
        this.fixnumCache = new RubyFixnum[2 * RubyFixnum.CACHE_OFFSET];
        this.fixnumConstants = new Object[this.fixnumCache.length];
        this.currentLine = 0;
        this.startTime = System.currentTimeMillis();
        this.parser = new Parser(this);
        this.globalVariables = new GlobalVariables(this);
        this.warnings = new RubyWarnings(this);
        this.regexpWarnings = (WarnCallback)new WarnCallback() {
            public void warn(final String message) {
                Ruby.this.getWarnings().warning(message);
            }
        };
        this.atExitBlocks = new Stack<RubyProc>();
        this.kcode = KCode.NONE;
        this.symbolLastId = new AtomicInteger(128);
        this.moduleLastId = new AtomicInteger(0);
        this.allModules = new ConcurrentWeakHashMap<RubyModule, Object>(128);
        this.timeZoneCache = new HashMap<String, DateTimeZone>();
        this.finalizersMutex = new Object();
        this.internalFinalizersMutex = new Object();
        this.hierarchyLock = new Object();
        this.dynamicMethodSerial = new AtomicLong(1L);
        this.moduleGeneration = new AtomicInteger(1);
        this.boundMethods = new HashMap<String, Map<String, String>>();
        this.selectorPool = new SelectorPool();
        this.exceptionCount = new AtomicInteger();
        this.backtraceCount = new AtomicInteger();
        this.callerCount = new AtomicInteger();
        this.warningCount = new AtomicInteger();
        this.fixnumInvalidator = OptoFactory.newGlobalInvalidator(0);
        this.floatInvalidator = OptoFactory.newGlobalInvalidator(0);
        this.coverageData = new CoverageData();
        this.definedMessages = new EnumMap<DefinedMessage, RubyString>(DefinedMessage.class);
        this.threadStatuses = new EnumMap<RubyThread.Status, RubyString>(RubyThread.Status.class);
        this.dedupMap = new ConcurrentHashMap<FStringEqual, WeakReference<RubyString>>();
        this.runtimeNumber = Ruby.RUNTIME_NUMBER.getAndIncrement();
        this.interpreter = new Interpreter();
        this.POPULATORS = new ClassValue<TypePopulator>() {
            @Override
            protected TypePopulator computeValue(final Class<?> type) {
                return RubyModule.loadPopulatorFor(type);
            }
        };
        this.sites = new JavaSites();
        this.javaExtensionDefinitions = new WeakHashMap<Class, Consumer<RubyModule>>();
        this.strptimeFormatCache = new ConcurrentHashMap<String, List<StrptimeToken>>();
        this.config = config;
        this.threadService = new ThreadService(this);
        this.profilingServiceLookup = (config.isProfiling() ? new ProfilingServiceLookup(this) : null);
        this.constant = OptoFactory.newConstantWrapper(Ruby.class, this);
        this.jrubyClassLoader = this.initJRubyClassLoader(config);
        this.staticScopeFactory = new StaticScopeFactory(this);
        this.beanManager = BeanManagerFactory.create(this, config.isManagementEnabled());
        this.jitCompiler = new JITCompiler(this);
        this.parserStats = new ParserStats(this);
        this.inlineStats = new InlineStats();
        this.caches = new Caches();
        this.random = this.initRandom();
        if (RubyInstanceConfig.CONSISTENT_HASHING_ENABLED) {
            this.hashSeedK0 = -561135208506705104L;
            this.hashSeedK1 = 7114160726623585955L;
        }
        else {
            this.hashSeedK0 = this.random.nextLong();
            this.hashSeedK1 = this.random.nextLong();
        }
        this.configBean = new Config(this);
        this.runtimeBean = new Runtime(this);
        this.registerMBeans();
        (this.runtimeCache = new RuntimeCache()).initMethodCache(ClassIndex.MAX_CLASSES.ordinal() * MethodNames.values().length - 1);
        this.checkpointInvalidator = OptoFactory.newConstantInvalidator(this);
        this.objectSpacer = this.initObjectSpacer(config);
        this.posix = POSIXFactory.getPOSIX((POSIXHandler)new JRubyPOSIXHandler(this), config.isNativeEnabled());
        this.filenoUtil = new FilenoUtil(this.posix);
        this.reinitialize(false);
        this.loadService = this.config.createLoadService(this);
        this.javaSupport = this.loadJavaSupport();
        this.executor = new ThreadPoolExecutor(RubyInstanceConfig.POOL_MIN, RubyInstanceConfig.POOL_MAX, RubyInstanceConfig.POOL_TTL, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new DaemonThreadFactory("Ruby-" + this.getRuntimeNumber() + "-Worker"));
        this.fiberExecutor = new ThreadPoolExecutor(0, Integer.MAX_VALUE, RubyInstanceConfig.FIBER_POOL_TTL, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new DaemonThreadFactory("Ruby-" + this.getRuntimeNumber() + "-Fiber"));
        this.basicObjectClass = RubyClass.createBootstrapClass(this, "BasicObject", null, RubyBasicObject.BASICOBJECT_ALLOCATOR);
        this.objectClass = RubyClass.createBootstrapClass(this, "Object", this.basicObjectClass, RubyObject.OBJECT_ALLOCATOR);
        this.moduleClass = RubyClass.createBootstrapClass(this, "Module", this.objectClass, RubyModule.MODULE_ALLOCATOR);
        this.classClass = RubyClass.createBootstrapClass(this, "Class", this.moduleClass, RubyClass.CLASS_ALLOCATOR);
        this.basicObjectClass.setMetaClass(this.classClass);
        this.objectClass.setMetaClass(this.basicObjectClass);
        this.moduleClass.setMetaClass(this.classClass);
        this.classClass.setMetaClass(this.classClass);
        RubyClass metaClass = this.basicObjectClass.makeMetaClass(this.classClass);
        metaClass = this.objectClass.makeMetaClass(metaClass);
        metaClass = this.moduleClass.makeMetaClass(metaClass);
        this.classClass.makeMetaClass(metaClass);
        RubyBasicObject.createBasicObjectClass(this, this.basicObjectClass);
        RubyObject.createObjectClass(this, this.objectClass);
        RubyModule.createModuleClass(this, this.moduleClass);
        RubyClass.createClassClass(this, this.classClass);
        this.basicObjectClass.setConstant("BasicObject", this.basicObjectClass);
        this.objectClass.setConstant("BasicObject", this.basicObjectClass);
        this.objectClass.setConstant("Object", this.objectClass);
        this.objectClass.setConstant("Class", this.classClass);
        this.objectClass.setConstant("Module", this.moduleClass);
        final RubyModule kernelModule = RubyKernel.createKernelModule(this);
        this.kernelModule = kernelModule;
        final RubyModule kernel = kernelModule;
        this.objectClass.includeModule(this.kernelModule);
        this.initKernelGsub(kernel);
        this.topSelf = TopSelfFactory.createTopSelf(this, false);
        this.nilClass = RubyNil.createNilClass(this);
        this.falseClass = RubyBoolean.createFalseClass(this);
        this.trueClass = RubyBoolean.createTrueClass(this);
        this.nilObject = new RubyNil(this);
        this.nilPrefilledArray = new IRubyObject[128];
        for (int i = 0; i < 128; ++i) {
            this.nilPrefilledArray[i] = this.nilObject;
        }
        this.singleNilArray = new IRubyObject[] { this.nilObject };
        (this.falseObject = new RubyBoolean.False(this)).setFrozen(true);
        (this.trueObject = new RubyBoolean.True(this)).setFrozen(true);
        this.reportOnException = this.trueObject;
        this.threadService.initMainThread();
        final ThreadContext context = this.getCurrentContext();
        context.prepareTopLevel(this.objectClass, this.topSelf);
        this.dataClass = this.initDataClass();
        this.comparableModule = RubyComparable.createComparable(this);
        this.enumerableModule = RubyEnumerable.createEnumerableModule(this);
        this.stringClass = RubyString.createStringClass(this);
        this.encodingService = new EncodingService(this);
        this.symbolClass = RubySymbol.createSymbolClass(this);
        this.threadGroupClass = (this.profile.allowClass("ThreadGroup") ? RubyThreadGroup.createThreadGroupClass(this) : null);
        this.threadClass = (this.profile.allowClass("Thread") ? RubyThread.createThreadClass(this) : null);
        this.exceptionClass = (this.profile.allowClass("Exception") ? RubyException.createExceptionClass(this) : null);
        this.numericClass = (this.profile.allowClass("Numeric") ? RubyNumeric.createNumericClass(this) : null);
        this.integerClass = (this.profile.allowClass("Integer") ? RubyInteger.createIntegerClass(this) : null);
        this.fixnumClass = (this.profile.allowClass("Fixnum") ? RubyFixnum.createFixnumClass(this) : null);
        this.encodingClass = RubyEncoding.createEncodingClass(this);
        this.converterClass = RubyConverter.createConverterClass(this);
        this.encodingService.defineEncodings();
        this.encodingService.defineAliases();
        this.initDefaultEncodings();
        this.complexClass = (this.profile.allowClass("Complex") ? RubyComplex.createComplexClass(this) : null);
        this.rationalClass = (this.profile.allowClass("Rational") ? RubyRational.createRationalClass(this) : null);
        this.hashClass = (this.profile.allowClass("Hash") ? RubyHash.createHashClass(this) : null);
        if (this.profile.allowClass("Array")) {
            this.arrayClass = RubyArray.createArrayClass(this);
            (this.emptyFrozenArray = this.newEmptyArray()).setFrozen(true);
        }
        else {
            this.arrayClass = null;
            this.emptyFrozenArray = null;
        }
        this.floatClass = (this.profile.allowClass("Float") ? RubyFloat.createFloatClass(this) : null);
        if (this.profile.allowClass("Bignum")) {
            this.bignumClass = RubyBignum.createBignumClass(this);
            this.randomClass = RubyRandom.createRandomClass(this);
        }
        else {
            this.bignumClass = null;
            this.randomClass = null;
            this.defaultRand = null;
        }
        this.ioClass = RubyIO.createIOClass(this);
        this.structClass = (this.profile.allowClass("Struct") ? RubyStruct.createStructClass(this) : null);
        this.bindingClass = (this.profile.allowClass("Binding") ? RubyBinding.createBindingClass(this) : null);
        this.mathModule = (this.profile.allowModule("Math") ? RubyMath.createMathModule(this) : null);
        this.regexpClass = (this.profile.allowClass("Regexp") ? RubyRegexp.createRegexpClass(this) : null);
        this.rangeClass = (this.profile.allowClass("Range") ? RubyRange.createRangeClass(this) : null);
        this.objectSpaceModule = (this.profile.allowModule("ObjectSpace") ? RubyObjectSpace.createObjectSpaceModule(this) : null);
        this.gcModule = (this.profile.allowModule("GC") ? RubyGC.createGCModule(this) : null);
        this.procClass = (this.profile.allowClass("Proc") ? RubyProc.createProcClass(this) : null);
        this.methodClass = (this.profile.allowClass("Method") ? RubyMethod.createMethodClass(this) : null);
        this.matchDataClass = (this.profile.allowClass("MatchData") ? RubyMatchData.createMatchDataClass(this) : null);
        this.marshalModule = (this.profile.allowModule("Marshal") ? RubyMarshal.createMarshalModule(this) : null);
        this.dirClass = (this.profile.allowClass("Dir") ? RubyDir.createDirClass(this) : null);
        this.fileTestModule = (this.profile.allowModule("FileTest") ? RubyFileTest.createFileTestModule(this) : null);
        this.fileClass = (this.profile.allowClass("File") ? RubyFile.createFileClass(this) : null);
        this.fileStatClass = (this.profile.allowClass("File::Stat") ? RubyFileStat.createFileStatClass(this) : null);
        this.processModule = (this.profile.allowModule("Process") ? RubyProcess.createProcessModule(this) : null);
        this.timeClass = (this.profile.allowClass("Time") ? RubyTime.createTimeClass(this) : null);
        this.unboundMethodClass = (this.profile.allowClass("UnboundMethod") ? RubyUnboundMethod.defineUnboundMethodClass(this) : null);
        if (this.profile.allowModule("Signal")) {
            RubySignal.createSignal(this);
        }
        if (this.profile.allowClass("Enumerator")) {
            this.enumeratorClass = RubyEnumerator.defineEnumerator(this, this.enumerableModule);
            this.generatorClass = RubyGenerator.createGeneratorClass(this, this.enumeratorClass);
            this.yielderClass = RubyYielder.createYielderClass(this);
        }
        else {
            this.enumeratorClass = null;
            this.generatorClass = null;
            this.yielderClass = null;
        }
        this.continuationClass = this.initContinuation();
        TracePoint.createTracePointClass(this);
        this.warningModule = RubyWarnings.createWarningModule(this);
        this.initExceptions();
        Mutex.setup(this);
        ConditionVariable.setup(this);
        Queue.setup(this);
        SizedQueue.setup(this);
        this.fiberClass = new ThreadFiberLibrary().createFiberClass(this);
        ThreadFiber.initRootFiber(context, context.getThread());
        this.initDefinedMessages();
        this.initThreadStatuses();
        this.irManager = new IRManager(this, this.getInstanceConfig());
        final IRScope top = new IRScriptBody(this.irManager, "", context.getCurrentScope().getStaticScope());
        top.allocateInterpreterContext(Collections.EMPTY_LIST, 0, IRScope.allocateInitialFlags(top));
        (this.dummyClass = new RubyClass(this, this.classClass)).setFrozen(true);
        RubyGlobal.createGlobals(this);
        this.getLoadService().init(this.config.getLoadPaths());
        this.coreIsBooted = true;
        if (!RubyInstanceConfig.DEBUG_PARSER) {
            this.initBootLibraries();
        }
        SecurityHelper.checkCryptoRestrictions(this);
        if (this.config.isProfiling()) {
            this.initProfiling();
        }
        if (this.config.getLoadGemfile()) {
            this.loadBundler();
        }
        this.deprecatedNetworkStackProperty();
        this.runtimeIsBooted = true;
    }
    
    private void initProfiling() {
        this.getLoadService().require("jruby/profiler/shutdown_hook");
        this.kernelModule.invalidateCacheDescendants();
        RubyKernel.recacheBuiltinMethods(this, this.kernelModule);
        RubyBasicObject.recacheBuiltinMethods(this);
    }
    
    private void initBootLibraries() {
        this.initJavaSupport();
        this.initRubyKernel();
        if (!this.config.isDisableGems()) {
            this.defineModule("Gem");
        }
        if (!this.config.isDisableDidYouMean()) {
            this.defineModule("DidYouMean");
        }
        this.loadService.provide("enumerator", "enumerator.rb");
        this.loadService.provide("rational", "rational.rb");
        this.loadService.provide("complex", "complex.rb");
        this.loadService.provide("thread", "thread.rb");
        this.initRubyPreludes();
    }
    
    private void initKernelGsub(final RubyModule kernel) {
        if (this.config.getKernelGsubDefined()) {
            kernel.addMethod("gsub", new JavaMethod(kernel, Visibility.PRIVATE, "gsub") {
                @Override
                public IRubyObject call(final ThreadContext context1, final IRubyObject self, final RubyModule clazz, final String name, final IRubyObject[] args, final Block block) {
                    switch (args.length) {
                        case 1: {
                            return RubyKernel.gsub(context1, self, args[0], block);
                        }
                        case 2: {
                            return RubyKernel.gsub(context1, self, args[0], args[1], block);
                        }
                        default: {
                            throw Ruby.this.newArgumentError(String.format("wrong number of arguments %d for 1..2", args.length));
                        }
                    }
                }
            });
        }
    }
    
    private ObjectSpacer initObjectSpacer(final RubyInstanceConfig config) {
        ObjectSpacer objectSpacer;
        if (config.isObjectSpaceEnabled()) {
            objectSpacer = Ruby.ENABLED_OBJECTSPACE;
        }
        else {
            objectSpacer = Ruby.DISABLED_OBJECTSPACE;
        }
        return objectSpacer;
    }
    
    private JRubyClassLoader initJRubyClassLoader(final RubyInstanceConfig config) {
        JRubyClassLoader jrubyClassLoader;
        if (!isSecurityRestricted()) {
            if (config.isClassloaderDelegate()) {
                jrubyClassLoader = new JRubyClassLoader(config.getLoader());
            }
            else {
                jrubyClassLoader = new SelfFirstJRubyClassLoader(config.getLoader());
            }
        }
        else {
            jrubyClassLoader = null;
        }
        return jrubyClassLoader;
    }
    
    private void initDefaultEncodings() {
        String encoding = this.config.getExternalEncoding();
        if (encoding != null && !encoding.equals("")) {
            final Encoding loadedEncoding = this.encodingService.loadEncoding(ByteList.create(encoding));
            if (loadedEncoding == null) {
                throw new MainExitException(1, "unknown encoding name - " + encoding);
            }
            this.setDefaultExternalEncoding(loadedEncoding);
        }
        else {
            final Encoding consoleEncoding = this.encodingService.getConsoleEncoding();
            final Encoding availableEncoding = (consoleEncoding == null) ? this.encodingService.getLocaleEncoding() : consoleEncoding;
            this.setDefaultExternalEncoding(availableEncoding);
        }
        if (Platform.IS_WINDOWS) {
            this.setDefaultFilesystemEncoding(this.encodingService.getWindowsFilesystemEncoding(this));
        }
        else {
            this.setDefaultFilesystemEncoding(this.getDefaultExternalEncoding());
        }
        encoding = this.config.getInternalEncoding();
        if (encoding != null && !encoding.equals("")) {
            final Encoding loadedEncoding = this.encodingService.loadEncoding(ByteList.create(encoding));
            if (loadedEncoding == null) {
                throw new MainExitException(1, "unknown encoding name - " + encoding);
            }
            this.setDefaultInternalEncoding(loadedEncoding);
        }
    }
    
    private RubyClass initDataClass() {
        RubyClass dataClass = null;
        if (this.profile.allowClass("Data")) {
            dataClass = this.defineClass("Data", this.objectClass, ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
            this.getObject().deprecateConstant(this, "Data");
        }
        return dataClass;
    }
    
    private Random initRandom() {
        Random myRandom;
        try {
            myRandom = new SecureRandom();
        }
        catch (Throwable t) {
            Ruby.LOG.debug("unable to instantiate SecureRandom, falling back on Random", t);
            myRandom = new Random();
        }
        return myRandom;
    }
    
    public void registerMBeans() {
        this.beanManager.register(this.jitCompiler);
        this.beanManager.register(this.configBean);
        this.beanManager.register(this.parserStats);
        this.beanManager.register(this.runtimeBean);
        this.beanManager.register(this.caches);
        this.beanManager.register(this.inlineStats);
    }
    
    void reinitialize(final boolean reinitCore) {
        this.doNotReverseLookupEnabled = true;
        this.in = this.config.getInput();
        this.out = this.config.getOutput();
        this.err = this.config.getError();
        this.objectSpaceEnabled = this.config.isObjectSpaceEnabled();
        this.siphashEnabled = this.config.isSiphashEnabled();
        this.profile = this.config.getProfile();
        this.currentDirectory = this.config.getCurrentDirectory();
        this.kcode = this.config.getKCode();
        if (reinitCore) {
            RubyGlobal.initARGV(this);
            RubyGlobal.initSTDIO(this, this.globalVariables);
        }
    }
    
    public static Ruby newInstance() {
        return newInstance(new RubyInstanceConfig());
    }
    
    public static Ruby newInstance(final RubyInstanceConfig config) {
        final Ruby ruby = new Ruby(config);
        ruby.loadRequiredLibraries();
        setGlobalRuntimeFirstTimeOnly(ruby);
        return ruby;
    }
    
    private void loadRequiredLibraries() {
        final ThreadContext context = this.getCurrentContext();
        for (final String scriptName : this.config.getRequiredLibraries()) {
            this.topSelf.callMethod(context, "require", RubyString.newString(this, scriptName));
        }
    }
    
    public static Ruby newInstance(final InputStream in, final PrintStream out, final PrintStream err) {
        final RubyInstanceConfig config = new RubyInstanceConfig();
        config.setInput(in);
        config.setOutput(out);
        config.setError(err);
        return newInstance(config);
    }
    
    public static boolean isGlobalRuntimeReady() {
        return Ruby.globalRuntime != null;
    }
    
    private static synchronized void setGlobalRuntimeFirstTimeOnly(final Ruby runtime) {
        if (Ruby.globalRuntime == null) {
            Ruby.globalRuntime = runtime;
        }
    }
    
    public static synchronized Ruby getGlobalRuntime() {
        if (Ruby.globalRuntime == null) {
            newInstance();
        }
        return Ruby.globalRuntime;
    }
    
    public void useAsGlobalRuntime() {
        synchronized (Ruby.class) {
            Ruby.globalRuntime = this;
        }
    }
    
    public static void clearGlobalRuntime() {
        Ruby.globalRuntime = null;
    }
    
    public static Ruby getThreadLocalRuntime() {
        return Ruby.threadLocalRuntime.get();
    }
    
    public static void setThreadLocalRuntime(final Ruby ruby) {
        Ruby.threadLocalRuntime.set(ruby);
    }
    
    public IRubyObject evalScriptlet(final String script) {
        final ThreadContext context = this.getCurrentContext();
        final DynamicScope currentScope = context.getCurrentScope();
        final ManyVarsDynamicScope newScope = new ManyVarsDynamicScope(this.getStaticScopeFactory().newEvalScope(currentScope.getStaticScope()), currentScope);
        return this.evalScriptlet(script, newScope);
    }
    
    public IRubyObject evalScriptlet(final String script, final DynamicScope scope) {
        final ThreadContext context = this.getCurrentContext();
        final RootNode rootNode = (RootNode)this.parseEval(script, "<script>", scope, 0);
        context.preEvalScriptlet(scope);
        try {
            return this.interpreter.execute(this, rootNode, context.getFrameSelf());
        }
        finally {
            context.postEvalScriptlet();
        }
    }
    
    public IRubyObject executeScript(final String script, final String filename) {
        final byte[] bytes = this.encodeToBytes(script);
        final ParseResult root = (ParseResult)this.parseInline(new ByteArrayInputStream(bytes), filename, null);
        final ThreadContext context = this.getCurrentContext();
        final String oldFile = context.getFile();
        final int oldLine = context.getLine();
        try {
            context.setFileAndLine(root.getFile(), root.getLine());
            return this.runInterpreter(root);
        }
        finally {
            context.setFileAndLine(oldFile, oldLine);
        }
    }
    
    public void runFromMain(final InputStream inputStream, final String filename) {
        final IAccessor d = new ValueAccessor(this.newString(filename));
        this.getGlobalVariables().define("$PROGRAM_NAME", d, org.jruby.internal.runtime.GlobalVariable.Scope.GLOBAL);
        this.getGlobalVariables().define("$0", d, org.jruby.internal.runtime.GlobalVariable.Scope.GLOBAL);
        for (final Map.Entry<String, String> entry : this.config.getOptionGlobals().entrySet()) {
            IRubyObject varvalue;
            if (entry.getValue() != null) {
                varvalue = this.newString(entry.getValue());
            }
            else {
                varvalue = this.getTrue();
            }
            this.getGlobalVariables().set('$' + entry.getKey(), varvalue);
        }
        if (!filename.endsWith(".class")) {
            final ParseResult parseResult = this.parseFromMain(filename, inputStream);
            if (this.fetchGlobalConstant("DATA") == null) {
                try {
                    inputStream.close();
                }
                catch (IOException ex) {}
            }
            if (parseResult instanceof RootNode) {
                final RootNode scriptNode = (RootNode)parseResult;
                final ThreadContext context = this.getCurrentContext();
                final String oldFile = context.getFile();
                final int oldLine = context.getLine();
                try {
                    context.setFileAndLine(scriptNode.getFile(), scriptNode.getLine());
                    if (this.config.isAssumePrinting() || this.config.isAssumeLoop()) {
                        this.runWithGetsLoop(scriptNode, this.config.isAssumePrinting(), this.config.isProcessLineEnds(), this.config.isSplit());
                    }
                    else {
                        this.runNormally(scriptNode);
                    }
                }
                finally {
                    context.setFileAndLine(oldFile, oldLine);
                }
            }
            else {
                this.runInterpreter(parseResult);
            }
            return;
        }
        final IRScope script = CompiledScriptLoader.loadScriptFromFile(this, inputStream, null, filename, false);
        if (script == null) {
            throw new MainExitException(1, "error: .class file specified is not a compiled JRuby script");
        }
        script.setFileName(filename);
        this.runInterpreter(script);
    }
    
    public Node parseFromMain(final InputStream inputStream, final String filename) {
        if (this.config.isInlineScript()) {
            return this.parseInline(inputStream, filename, this.getCurrentContext().getCurrentScope());
        }
        return this.parseFileFromMain(inputStream, filename, this.getCurrentContext().getCurrentScope());
    }
    
    public ParseResult parseFromMain(final String fileName, final InputStream in) {
        if (this.config.isInlineScript()) {
            return (ParseResult)this.parseInline(in, fileName, this.getCurrentContext().getCurrentScope());
        }
        return this.parseFileFromMain(fileName, in, this.getCurrentContext().getCurrentScope());
    }
    
    @Deprecated
    public IRubyObject runWithGetsLoop(final Node scriptNode, final boolean printing, final boolean processLineEnds, final boolean split, final boolean unused) {
        return this.runWithGetsLoop((RootNode)scriptNode, printing, processLineEnds, split);
    }
    
    public IRubyObject runWithGetsLoop(RootNode scriptNode, final boolean printing, final boolean processLineEnds, final boolean split) {
        final ThreadContext context = this.getCurrentContext();
        scriptNode = this.addGetsLoop(scriptNode, printing, processLineEnds, split);
        Script script = null;
        final boolean compile = this.getInstanceConfig().getCompileMode().shouldPrecompileCLI();
        if (compile) {
            try {
                script = this.tryCompile(scriptNode);
                if (Options.JIT_LOGGING.load()) {
                    Ruby.LOG.info("successfully compiled: {}", scriptNode.getFile());
                }
            }
            catch (Throwable e) {
                if (Options.JIT_LOGGING.load()) {
                    if (Options.JIT_LOGGING_VERBOSE.load()) {
                        Ruby.LOG.error("failed to compile: " + scriptNode.getFile(), e);
                    }
                    else {
                        Ruby.LOG.error("failed to compile: " + scriptNode.getFile() + " - " + e, new Object[0]);
                    }
                }
            }
            if (!compile || script == null) {}
        }
        Helpers.preLoad(context, scriptNode.getStaticScope().getVariables());
        try {
            if (script != null) {
                this.runScriptBody(script);
            }
            else {
                this.runInterpreterBody(scriptNode);
            }
        }
        finally {
            Helpers.postLoad(context);
        }
        return this.getNil();
    }
    
    private RootNode addGetsLoop(final RootNode oldRoot, final boolean printing, final boolean processLineEndings, final boolean split) {
        final ISourcePosition pos = oldRoot.getPosition();
        final BlockNode newBody = new BlockNode(pos);
        final RubySymbol dollarSlash = this.newSymbol(CommonByteLists.DOLLAR_SLASH);
        newBody.add(new GlobalAsgnNode(pos, dollarSlash, new StrNode(pos, ((RubyString)this.globalVariables.get("$/")).getByteList())));
        if (processLineEndings) {
            newBody.add(new GlobalAsgnNode(pos, this.newSymbol(CommonByteLists.DOLLAR_BACKSLASH), new GlobalVarNode(pos, dollarSlash)));
        }
        final GlobalVarNode dollarUnderscore = new GlobalVarNode(pos, this.newSymbol("$_"));
        final BlockNode whileBody = new BlockNode(pos);
        newBody.add(new WhileNode(pos, new VCallNode(pos, this.newSymbol("gets")), whileBody));
        if (processLineEndings) {
            whileBody.add(new CallNode(pos, dollarUnderscore, this.newSymbol("chomp!"), null, null, false));
        }
        if (split) {
            whileBody.add(new GlobalAsgnNode(pos, this.newSymbol("$F"), new CallNode(pos, dollarUnderscore, this.newSymbol("split"), null, null, false)));
        }
        if (oldRoot.getBodyNode() instanceof BlockNode) {
            whileBody.addAll((ListNode)oldRoot.getBodyNode());
        }
        else {
            whileBody.add(oldRoot.getBodyNode());
        }
        if (printing) {
            whileBody.add(new FCallNode(pos, this.newSymbol("puts"), new ArrayNode(pos, dollarUnderscore), null));
        }
        return new RootNode(pos, oldRoot.getScope(), newBody, oldRoot.getFile());
    }
    
    public IRubyObject runNormally(final Node scriptNode, final boolean wrap) {
        ScriptAndCode scriptAndCode = null;
        final boolean compile = this.getInstanceConfig().getCompileMode().shouldPrecompileCLI();
        if (compile || this.config.isShowBytecode()) {
            scriptAndCode = this.precompileCLI((RootNode)scriptNode);
        }
        if (scriptAndCode == null) {
            return this.runInterpreter(scriptNode);
        }
        if (this.config.isShowBytecode()) {
            final TraceClassVisitor tracer = new TraceClassVisitor(new PrintWriter(System.err));
            final ClassReader reader = new ClassReader(scriptAndCode.bytecode());
            reader.accept(tracer, 0);
            return this.getNil();
        }
        return this.runScript(scriptAndCode.script(), wrap);
    }
    
    public IRubyObject runNormally(final Node scriptNode) {
        return this.runNormally(scriptNode, false);
    }
    
    private ScriptAndCode precompileCLI(final RootNode scriptNode) {
        ScriptAndCode scriptAndCode = null;
        try {
            scriptAndCode = this.tryCompile(scriptNode, new ClassDefiningJRubyClassLoader(this.getJRubyClassLoader()));
            if (scriptAndCode != null && (boolean)Options.JIT_LOGGING.load()) {
                Ruby.LOG.info("done compiling target script: {}", scriptNode.getFile());
            }
        }
        catch (Exception e) {
            if (Options.JIT_LOGGING.load()) {
                if (Options.JIT_LOGGING_VERBOSE.load()) {
                    Ruby.LOG.error("failed to compile target script: " + scriptNode.getFile(), e);
                }
                else {
                    Ruby.LOG.error("failed to compile target script: " + scriptNode.getFile() + " - " + e, new Object[0]);
                }
            }
        }
        return scriptAndCode;
    }
    
    public Script tryCompile(final Node node) {
        return this.tryCompile((RootNode)node, new ClassDefiningJRubyClassLoader(this.getJRubyClassLoader())).script();
    }
    
    private ScriptAndCode tryCompile(final RootNode root, final ClassDefiningClassLoader classLoader) {
        try {
            return Compiler.getInstance().execute(this, root, classLoader);
        }
        catch (NotCompilableException e) {
            if (Options.JIT_LOGGING.load()) {
                if (Options.JIT_LOGGING_VERBOSE.load()) {
                    Ruby.LOG.error("failed to compile target script: " + root.getFile(), e);
                }
                else {
                    Ruby.LOG.error("failed to compile target script: " + root.getFile() + " - " + e.getLocalizedMessage(), new Object[0]);
                }
            }
            return null;
        }
    }
    
    public IRubyObject runScript(final Script script) {
        return this.runScript(script, false);
    }
    
    public IRubyObject runScript(final Script script, final boolean wrap) {
        return script.load(this.getCurrentContext(), this.getTopSelf(), wrap);
    }
    
    public IRubyObject runScriptBody(final Script script) {
        return script.__file__(this.getCurrentContext(), this.getTopSelf(), Block.NULL_BLOCK);
    }
    
    public IRubyObject runInterpreter(final ThreadContext context, final ParseResult parseResult, final IRubyObject self) {
        try {
            return this.interpreter.execute(this, parseResult, self);
        }
        catch (IRReturnJump ex) {
            if (!ex.methodToReturnFrom.getStaticScope().getIRScope().isScriptScope()) {
                System.err.println("Unexpected 'return' escaped the runtime from " + ex.returnScope + " to " + ex.methodToReturnFrom.getStaticScope().getIRScope());
                System.err.println(ThreadContext.createRawBacktraceStringFromThrowable(ex, false));
                Throwable t = ex;
                while ((t = t.getCause()) != null) {
                    System.err.println("Caused by:");
                    System.err.println(ThreadContext.createRawBacktraceStringFromThrowable(t, false));
                }
            }
            return context.nil;
        }
    }
    
    public IRubyObject runInterpreter(final ThreadContext context, final Node rootNode, final IRubyObject self) {
        assert rootNode != null : "scriptNode is not null";
        return this.interpreter.execute(this, (ParseResult)rootNode, self);
    }
    
    public IRubyObject runInterpreter(final Node scriptNode) {
        return this.runInterpreter(this.getCurrentContext(), scriptNode, this.getTopSelf());
    }
    
    public IRubyObject runInterpreter(final ParseResult parseResult) {
        return this.runInterpreter(this.getCurrentContext(), parseResult, this.getTopSelf());
    }
    
    public IRubyObject runInterpreterBody(final Node scriptNode) {
        assert scriptNode != null : "scriptNode is not null";
        assert scriptNode instanceof RootNode : "scriptNode is not a RootNode";
        return this.runInterpreter(scriptNode);
    }
    
    public Parser getParser() {
        return this.parser;
    }
    
    public BeanManager getBeanManager() {
        return this.beanManager;
    }
    
    public JITCompiler getJITCompiler() {
        return this.jitCompiler;
    }
    
    public InlineStats getInlineStats() {
        return this.inlineStats;
    }
    
    public Caches getCaches() {
        return this.caches;
    }
    
    @Deprecated
    public static Ruby getDefaultInstance() {
        return newInstance();
    }
    
    @Deprecated
    public static Ruby getCurrentInstance() {
        return null;
    }
    
    @Deprecated
    public static void setCurrentInstance(final Ruby runtime) {
    }
    
    public int allocSymbolId() {
        return this.symbolLastId.incrementAndGet();
    }
    
    public int allocModuleId() {
        return this.moduleLastId.incrementAndGet();
    }
    
    public void addModule(final RubyModule module) {
        this.allModules.put(module, RubyBasicObject.NEVER);
    }
    
    public void eachModule(final Consumer<RubyModule> func) {
        final Enumeration<RubyModule> e = this.allModules.keys();
        while (e.hasMoreElements()) {
            func.accept(e.nextElement());
        }
    }
    
    @Deprecated
    public void eachModule(final Function1<Object, IRubyObject> func) {
        final Enumeration<RubyModule> e = this.allModules.keys();
        while (e.hasMoreElements()) {
            func.apply(e.nextElement());
        }
    }
    
    public RubyModule getModule(final String name) {
        return this.objectClass.getModule(name);
    }
    
    @Deprecated
    public RubyModule fastGetModule(final String internedName) {
        return this.getModule(internedName);
    }
    
    public RubyClass getClass(final String name) {
        return this.objectClass.getClass(name);
    }
    
    @Deprecated
    public RubyClass fastGetClass(final String internedName) {
        return this.getClass(internedName);
    }
    
    @Extension
    public RubyClass defineClass(final String name, final RubyClass superClass, final ObjectAllocator allocator) {
        return this.defineClassUnder(name, superClass, allocator, this.objectClass);
    }
    
    public RubyClass defineClass(final String name, final RubyClass superClass, final ObjectAllocator allocator, final CallSite[] callSites) {
        return this.defineClassUnder(name, superClass, allocator, this.objectClass, callSites);
    }
    
    @Extension
    public RubyClass defineClassUnder(final String name, final RubyClass superClass, final ObjectAllocator allocator, final RubyModule parent) {
        return this.defineClassUnder(name, superClass, allocator, parent, null);
    }
    
    public RubyClass defineClassUnder(final String id, RubyClass superClass, final ObjectAllocator allocator, final RubyModule parent, final CallSite[] callSites) {
        final IRubyObject classObj = parent.getConstantAt(id);
        if (classObj == null) {
            final boolean parentIsObject = parent == this.objectClass;
            if (superClass == null) {
                final IRubyObject className = parentIsObject ? RubyStringBuilder.ids(this, id) : parent.toRubyString(this.getCurrentContext()).append(this.newString("::")).append(RubyStringBuilder.ids(this, id));
                this.warnings.warn(IRubyWarnings.ID.NO_SUPER_CLASS, RubyStringBuilder.str(this, "no super class for `", className, "', Object assumed"));
                superClass = this.objectClass;
            }
            return RubyClass.newClass(this, superClass, id, allocator, parent, !parentIsObject, callSites);
        }
        if (!(classObj instanceof RubyClass)) {
            throw this.newTypeError(RubyStringBuilder.str(this, RubyStringBuilder.ids(this, id), " is not a class"));
        }
        final RubyClass klazz = (RubyClass)classObj;
        if (klazz.getSuperClass().getRealClass() != superClass) {
            throw this.newNameError(RubyStringBuilder.str(this, RubyStringBuilder.ids(this, id), " is already defined"), id);
        }
        if (klazz.getAllocator() != allocator) {
            klazz.setAllocator(allocator);
        }
        return klazz;
    }
    
    @Extension
    public RubyModule defineModule(final String name) {
        return this.defineModuleUnder(name, this.objectClass);
    }
    
    @Extension
    public RubyModule defineModuleUnder(final String name, final RubyModule parent) {
        final IRubyObject moduleObj = parent.getConstantAt(name);
        final boolean parentIsObject = parent == this.objectClass;
        if (moduleObj == null) {
            return RubyModule.newModule(this, name, parent, !parentIsObject);
        }
        if (moduleObj.isModule()) {
            return (RubyModule)moduleObj;
        }
        final RubyString typeName = parentIsObject ? RubyStringBuilder.types(this, moduleObj.getMetaClass()) : RubyStringBuilder.types(this, parent, moduleObj.getMetaClass());
        throw this.newTypeError(RubyStringBuilder.str(this, typeName, " is not a module"));
    }
    
    public RubyModule getOrCreateModule(final String id) {
        IRubyObject module = this.objectClass.getConstantAt(id);
        if (module == null) {
            module = this.defineModule(id);
        }
        else if (!module.isModule()) {
            throw this.newTypeError(RubyStringBuilder.str(this, RubyStringBuilder.ids(this, id), " is not a Module"));
        }
        return (RubyModule)module;
    }
    
    public KCode getKCode() {
        return this.kcode;
    }
    
    public void setKCode(final KCode kcode) {
        this.kcode = kcode;
    }
    
    public void defineGlobalConstant(final String name, final IRubyObject value) {
        this.objectClass.defineConstant(name, value);
    }
    
    public IRubyObject fetchGlobalConstant(final String name) {
        return this.objectClass.fetchConstant(name, false);
    }
    
    public boolean isClassDefined(final String name) {
        return this.getModule(name) != null;
    }
    
    public JavaSupport loadJavaSupport() {
        return new JavaSupportImpl(this);
    }
    
    private void loadBundler() {
        this.loadService.loadFromClassLoader(getClassLoader(), "jruby/bundler/startup.rb", false);
    }
    
    private boolean doesReflectionWork() {
        try {
            ClassLoader.class.getDeclaredMethod("getResourceAsStream", String.class);
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }
    
    private void initDefinedMessages() {
        for (final DefinedMessage definedMessage : DefinedMessage.values()) {
            final RubyString str = this.freezeAndDedupString(RubyString.newString(this, ByteList.create(definedMessage.getText())));
            this.definedMessages.put(definedMessage, str);
        }
    }
    
    private void initThreadStatuses() {
        for (final RubyThread.Status status : RubyThread.Status.values()) {
            final RubyString str = RubyString.newString(this, status.bytes);
            str.setFrozen(true);
            this.threadStatuses.put(status, str);
        }
    }
    
    private RubyClass initContinuation() {
        if (this.profile.allowClass("Continuation")) {
            return RubyContinuation.createContinuation(this);
        }
        return null;
    }
    
    public IRubyObject[] getNilPrefilledArray() {
        return this.nilPrefilledArray;
    }
    
    private void initExceptions() {
        this.ifAllowed("StandardError", ruby -> this.standardError = RubyStandardError.define(ruby, this.exceptionClass));
        this.ifAllowed("RubyError", ruby -> this.runtimeError = RubyRuntimeError.define(ruby, this.standardError));
        this.ifAllowed("FrozenError", ruby -> this.frozenError = RubyFrozenError.define(ruby, this.runtimeError));
        this.ifAllowed("IOError", ruby -> this.ioError = RubyIOError.define(ruby, this.standardError));
        this.ifAllowed("ScriptError", ruby -> this.scriptError = RubyScriptError.define(ruby, this.exceptionClass));
        this.ifAllowed("RangeError", ruby -> this.rangeError = RubyRangeError.define(ruby, this.standardError));
        this.ifAllowed("SignalException", ruby -> this.signalException = RubySignalException.define(ruby, this.exceptionClass));
        this.ifAllowed("NameError", ruby -> {
            this.nameError = RubyNameError.define(ruby, this.standardError);
            this.nameErrorMessage = RubyNameError.RubyNameErrorMessage.define(ruby, this.nameError);
            return;
        });
        this.ifAllowed("NoMethodError", ruby -> this.noMethodError = RubyNoMethodError.define(ruby, this.nameError));
        this.ifAllowed("SystemExit", ruby -> this.systemExit = RubySystemExit.define(ruby, this.exceptionClass));
        this.ifAllowed("LocalJumpError", ruby -> this.localJumpError = RubyLocalJumpError.define(ruby, this.standardError));
        this.ifAllowed("SystemCallError", ruby -> this.systemCallError = RubySystemCallError.define(ruby, this.standardError));
        this.ifAllowed("Fatal", ruby -> this.fatal = RubyFatal.define(ruby, this.exceptionClass));
        this.ifAllowed("Interrupt", ruby -> this.interrupt = RubyInterrupt.define(ruby, this.signalException));
        this.ifAllowed("TypeError", ruby -> this.typeError = RubyTypeError.define(ruby, this.standardError));
        this.ifAllowed("ArgumentError", ruby -> this.argumentError = RubyArgumentError.define(ruby, this.standardError));
        this.ifAllowed("UncaughtThrowError", ruby -> this.uncaughtThrowError = RubyUncaughtThrowError.define(ruby, this.argumentError));
        this.ifAllowed("IndexError", ruby -> this.indexError = RubyIndexError.define(ruby, this.standardError));
        this.ifAllowed("StopIteration", ruby -> this.stopIteration = RubyStopIteration.define(ruby, this.indexError));
        this.ifAllowed("SyntaxError", ruby -> this.syntaxError = RubySyntaxError.define(ruby, this.scriptError));
        this.ifAllowed("LoadError", ruby -> this.loadError = RubyLoadError.define(ruby, this.scriptError));
        this.ifAllowed("NotImplementedError", ruby -> this.notImplementedError = RubyNotImplementedError.define(ruby, this.scriptError));
        this.ifAllowed("SecurityError", ruby -> this.securityError = RubySecurityError.define(ruby, this.exceptionClass));
        this.ifAllowed("NoMemoryError", ruby -> this.noMemoryError = RubyNoMemoryError.define(ruby, this.exceptionClass));
        this.ifAllowed("RegexpError", ruby -> this.regexpError = RubyRegexpError.define(ruby, this.standardError));
        this.ifAllowed("InterruptedRegexpError", ruby -> this.interruptedRegexpError = RubyInterruptedRegexpError.define(ruby, this.regexpError));
        this.ifAllowed("EOFError", ruby -> this.eofError = RubyEOFError.define(ruby, this.ioError));
        this.ifAllowed("ThreadError", ruby -> this.threadError = RubyThreadError.define(ruby, this.standardError));
        this.ifAllowed("ConcurrencyError", ruby -> this.concurrencyError = RubyConcurrencyError.define(ruby, this.threadError));
        this.ifAllowed("SystemStackError", ruby -> this.systemStackError = RubySystemStackError.define(ruby, this.exceptionClass));
        this.ifAllowed("ZeroDivisionError", ruby -> this.zeroDivisionError = RubyZeroDivisionError.define(ruby, this.standardError));
        this.ifAllowed("FloatDomainError", ruby -> this.floatDomainError = RubyFloatDomainError.define(ruby, this.rangeError));
        this.ifAllowed("EncodingError", ruby -> {
            this.encodingError = RubyEncodingError.define(ruby, this.standardError);
            this.encodingCompatibilityError = RubyEncodingError.RubyCompatibilityError.define(ruby, this.encodingError, this.encodingClass);
            this.invalidByteSequenceError = RubyEncodingError.RubyInvalidByteSequenceError.define(ruby, this.encodingError, this.encodingClass);
            this.undefinedConversionError = RubyEncodingError.RubyUndefinedConversionError.define(ruby, this.encodingError, this.encodingClass);
            this.converterNotFoundError = RubyEncodingError.RubyConverterNotFoundError.define(ruby, this.encodingError, this.encodingClass);
            return;
        });
        this.ifAllowed("Fiber", ruby -> this.fiberError = RubyFiberError.define(ruby, this.standardError));
        this.ifAllowed("ConcurrencyError", ruby -> this.concurrencyError = RubyConcurrencyError.define(ruby, this.threadError));
        this.ifAllowed("KeyError", ruby -> this.keyError = RubyKeyError.define(ruby, this.indexError));
        this.ifAllowed("DomainError", ruby -> this.mathDomainError = RubyDomainError.define(ruby, this.argumentError, this.mathModule));
        this.initErrno();
        this.initNativeException();
    }
    
    private void ifAllowed(final String name, final Consumer<Ruby> callback) {
        if (this.profile.allowClass(name)) {
            callback.accept(this);
        }
    }
    
    private void initNativeException() {
        if (this.profile.allowClass("NativeException")) {
            this.nativeException = NativeException.createClass(this, this.runtimeError);
        }
    }
    
    public RubyClass getErrno(final int n) {
        return this.errnos.get(n);
    }
    
    private void initErrno() {
        if (this.profile.allowModule("Errno")) {
            this.errnoModule = this.defineModule("Errno");
            try {
                this.createSysErr(Errno.EAGAIN.intValue(), Errno.EAGAIN.name());
                for (final Constant c : Errno.values()) {
                    final Errno e = (Errno)c;
                    if (Character.isUpperCase(c.name().charAt(0))) {
                        this.createSysErr(c.intValue(), c.name());
                    }
                }
                this.errnos.put(Errno.ENOSYS.intValue(), this.notImplementedError);
            }
            catch (Exception e2) {
                Ruby.LOG.error(e2);
            }
        }
    }
    
    private void createSysErr(final int i, final String name) {
        if (this.profile.allowClass(name)) {
            if (this.errnos.get(i) == null) {
                final RubyClass errno = this.getErrno().defineClassUnder(name, this.systemCallError, this.systemCallError.getAllocator());
                this.errnos.put(i, errno);
                errno.defineConstant("Errno", this.newFixnum(i));
            }
            else {
                this.getErrno().setConstant(name, this.errnos.get(i));
            }
        }
    }
    
    private void initJavaSupport() {
        final boolean reflectionWorks = this.doesReflectionWork();
        if (reflectionWorks) {
            new Java().load(this, false);
            new JRubyLibrary().load(this, false);
            new JRubyUtilLibrary().load(this, false);
            this.loadService.provide("java", "java.rb");
            this.loadService.provide("jruby", "jruby.rb");
            this.loadService.provide("jruby/util", "jruby/util.rb");
        }
    }
    
    private void initRubyKernel() {
        this.loadService.loadFromClassLoader(getClassLoader(), "jruby/kernel.rb", false);
    }
    
    private void initRubyPreludes() {
        if (RubyInstanceConfig.DEBUG_PARSER) {
            return;
        }
        this.loadService.loadFromClassLoader(getClassLoader(), "jruby/preludes.rb", false);
    }
    
    public IRManager getIRManager() {
        return this.irManager;
    }
    
    public IRubyObject getTopSelf() {
        return this.topSelf;
    }
    
    public void setCurrentDirectory(final String dir) {
        this.currentDirectory = dir;
    }
    
    public String getCurrentDirectory() {
        return this.currentDirectory;
    }
    
    public void setCurrentLine(final int line) {
        this.currentLine = line;
    }
    
    public int getCurrentLine() {
        return this.currentLine;
    }
    
    public void setArgsFile(final IRubyObject argsFile) {
        this.argsFile = argsFile;
    }
    
    public IRubyObject getArgsFile() {
        return this.argsFile;
    }
    
    public RubyModule getEtc() {
        return this.etcModule;
    }
    
    public void setEtc(final RubyModule etcModule) {
        this.etcModule = etcModule;
    }
    
    public RubyClass getObject() {
        return this.objectClass;
    }
    
    public RubyClass getBasicObject() {
        return this.basicObjectClass;
    }
    
    public RubyClass getModule() {
        return this.moduleClass;
    }
    
    public RubyClass getClassClass() {
        return this.classClass;
    }
    
    public RubyModule getKernel() {
        return this.kernelModule;
    }
    
    public DynamicMethod getPrivateMethodMissing() {
        return this.privateMethodMissing;
    }
    
    public void setPrivateMethodMissing(final DynamicMethod method) {
        this.privateMethodMissing = method;
    }
    
    public DynamicMethod getProtectedMethodMissing() {
        return this.protectedMethodMissing;
    }
    
    public void setProtectedMethodMissing(final DynamicMethod method) {
        this.protectedMethodMissing = method;
    }
    
    public DynamicMethod getVariableMethodMissing() {
        return this.variableMethodMissing;
    }
    
    public void setVariableMethodMissing(final DynamicMethod method) {
        this.variableMethodMissing = method;
    }
    
    public DynamicMethod getSuperMethodMissing() {
        return this.superMethodMissing;
    }
    
    public void setSuperMethodMissing(final DynamicMethod method) {
        this.superMethodMissing = method;
    }
    
    public DynamicMethod getNormalMethodMissing() {
        return this.normalMethodMissing;
    }
    
    public void setNormalMethodMissing(final DynamicMethod method) {
        this.normalMethodMissing = method;
    }
    
    public DynamicMethod getDefaultMethodMissing() {
        return this.defaultMethodMissing;
    }
    
    public boolean isDefaultMethodMissing(final DynamicMethod method) {
        return this.defaultMethodMissing == method || this.defaultModuleMethodMissing == method;
    }
    
    public void setDefaultMethodMissing(final DynamicMethod method, final DynamicMethod moduleMethod) {
        this.defaultMethodMissing = method;
        this.defaultModuleMethodMissing = moduleMethod;
    }
    
    public DynamicMethod getRespondToMethod() {
        return this.respondTo;
    }
    
    public void setRespondToMethod(final DynamicMethod rtm) {
        this.respondTo = rtm;
    }
    
    public DynamicMethod getRespondToMissingMethod() {
        return this.respondToMissing;
    }
    
    public void setRespondToMissingMethod(final DynamicMethod rtmm) {
        this.respondToMissing = rtmm;
    }
    
    public RubyClass getDummy() {
        return this.dummyClass;
    }
    
    public RubyModule getComparable() {
        return this.comparableModule;
    }
    
    public RubyClass getNumeric() {
        return this.numericClass;
    }
    
    public RubyClass getFloat() {
        return this.floatClass;
    }
    
    public RubyClass getInteger() {
        return this.integerClass;
    }
    
    public RubyClass getFixnum() {
        return this.fixnumClass;
    }
    
    public RubyClass getComplex() {
        return this.complexClass;
    }
    
    public RubyClass getRational() {
        return this.rationalClass;
    }
    
    public RubyModule getEnumerable() {
        return this.enumerableModule;
    }
    
    public RubyClass getEnumerator() {
        return this.enumeratorClass;
    }
    
    public RubyClass getYielder() {
        return this.yielderClass;
    }
    
    public RubyClass getGenerator() {
        return this.generatorClass;
    }
    
    public RubyClass getFiber() {
        return this.fiberClass;
    }
    
    public RubyClass getString() {
        return this.stringClass;
    }
    
    public RubyClass getEncoding() {
        return this.encodingClass;
    }
    
    public RubyClass getConverter() {
        return this.converterClass;
    }
    
    public RubyClass getSymbol() {
        return this.symbolClass;
    }
    
    public RubyClass getArray() {
        return this.arrayClass;
    }
    
    public RubyClass getHash() {
        return this.hashClass;
    }
    
    public RubyClass getRange() {
        return this.rangeClass;
    }
    
    public RubyBoolean getTrue() {
        return this.trueObject;
    }
    
    public RubyBoolean getFalse() {
        return this.falseObject;
    }
    
    public IRubyObject getNil() {
        return this.nilObject;
    }
    
    public IRubyObject[] getSingleNilArray() {
        return this.singleNilArray;
    }
    
    public RubyClass getNilClass() {
        return this.nilClass;
    }
    
    public RubyClass getTrueClass() {
        return this.trueClass;
    }
    
    public RubyClass getFalseClass() {
        return this.falseClass;
    }
    
    public RubyClass getProc() {
        return this.procClass;
    }
    
    public RubyClass getBinding() {
        return this.bindingClass;
    }
    
    public RubyClass getMethod() {
        return this.methodClass;
    }
    
    public RubyClass getUnboundMethod() {
        return this.unboundMethodClass;
    }
    
    public RubyClass getMatchData() {
        return this.matchDataClass;
    }
    
    public RubyClass getRegexp() {
        return this.regexpClass;
    }
    
    public RubyClass getTime() {
        return this.timeClass;
    }
    
    public RubyModule getMath() {
        return this.mathModule;
    }
    
    public RubyModule getMarshal() {
        return this.marshalModule;
    }
    
    public RubyClass getBignum() {
        return this.bignumClass;
    }
    
    public RubyClass getDir() {
        return this.dirClass;
    }
    
    public RubyClass getFile() {
        return this.fileClass;
    }
    
    public RubyClass getFileStat() {
        return this.fileStatClass;
    }
    
    public RubyModule getFileTest() {
        return this.fileTestModule;
    }
    
    public RubyClass getIO() {
        return this.ioClass;
    }
    
    public RubyClass getThread() {
        return this.threadClass;
    }
    
    public RubyClass getThreadGroup() {
        return this.threadGroupClass;
    }
    
    public RubyThreadGroup getDefaultThreadGroup() {
        return this.defaultThreadGroup;
    }
    
    void setDefaultThreadGroup(final RubyThreadGroup defaultThreadGroup) {
        this.defaultThreadGroup = defaultThreadGroup;
    }
    
    public RubyClass getContinuation() {
        return this.continuationClass;
    }
    
    public RubyClass getStructClass() {
        return this.structClass;
    }
    
    public RubyClass getRandomClass() {
        return this.randomClass;
    }
    
    public IRubyObject getTmsStruct() {
        return this.tmsStruct;
    }
    
    void setTmsStruct(final RubyClass tmsStruct) {
        this.tmsStruct = tmsStruct;
    }
    
    public IRubyObject getPasswdStruct() {
        return this.passwdStruct;
    }
    
    public void setPasswdStruct(final RubyClass passwdStruct) {
        this.passwdStruct = passwdStruct;
    }
    
    public IRubyObject getGroupStruct() {
        return this.groupStruct;
    }
    
    public void setGroupStruct(final RubyClass groupStruct) {
        this.groupStruct = groupStruct;
    }
    
    public RubyModule getGC() {
        return this.gcModule;
    }
    
    public RubyModule getObjectSpaceModule() {
        return this.objectSpaceModule;
    }
    
    public RubyModule getProcess() {
        return this.processModule;
    }
    
    public RubyClass getProcStatus() {
        return this.procStatusClass;
    }
    
    void setProcStatus(final RubyClass procStatusClass) {
        this.procStatusClass = procStatusClass;
    }
    
    public RubyModule getProcUID() {
        return this.procUIDModule;
    }
    
    void setProcUID(final RubyModule procUIDModule) {
        this.procUIDModule = procUIDModule;
    }
    
    public RubyModule getProcGID() {
        return this.procGIDModule;
    }
    
    void setProcGID(final RubyModule procGIDModule) {
        this.procGIDModule = procGIDModule;
    }
    
    public RubyModule getProcSysModule() {
        return this.procSysModule;
    }
    
    void setProcSys(final RubyModule procSysModule) {
        this.procSysModule = procSysModule;
    }
    
    public RubyModule getPrecision() {
        return this.precisionModule;
    }
    
    void setPrecision(final RubyModule precisionModule) {
        this.precisionModule = precisionModule;
    }
    
    public RubyHash getENV() {
        return this.envObject;
    }
    
    public void setENV(final RubyHash env) {
        this.envObject = env;
    }
    
    public RubyClass getLocation() {
        return this.locationClass;
    }
    
    public void setLocation(final RubyClass location) {
        this.locationClass = location;
    }
    
    public RubyModule getWarning() {
        return this.warningModule;
    }
    
    public RubyModule getErrno() {
        return this.errnoModule;
    }
    
    public RubyClass getException() {
        return this.exceptionClass;
    }
    
    public RubyClass getNameError() {
        return this.nameError;
    }
    
    public RubyClass getNameErrorMessage() {
        return this.nameErrorMessage;
    }
    
    public RubyClass getNoMethodError() {
        return this.noMethodError;
    }
    
    public RubyClass getSignalException() {
        return this.signalException;
    }
    
    public RubyClass getRangeError() {
        return this.rangeError;
    }
    
    public RubyClass getSystemExit() {
        return this.systemExit;
    }
    
    public RubyClass getLocalJumpError() {
        return this.localJumpError;
    }
    
    public RubyClass getNativeException() {
        return this.nativeException;
    }
    
    public RubyClass getSystemCallError() {
        return this.systemCallError;
    }
    
    public RubyClass getKeyError() {
        return this.keyError;
    }
    
    public RubyClass getFatal() {
        return this.fatal;
    }
    
    public RubyClass getInterrupt() {
        return this.interrupt;
    }
    
    public RubyClass getTypeError() {
        return this.typeError;
    }
    
    public RubyClass getArgumentError() {
        return this.argumentError;
    }
    
    public RubyClass getUncaughtThrowError() {
        return this.uncaughtThrowError;
    }
    
    public RubyClass getIndexError() {
        return this.indexError;
    }
    
    public RubyClass getStopIteration() {
        return this.stopIteration;
    }
    
    public RubyClass getSyntaxError() {
        return this.syntaxError;
    }
    
    public RubyClass getStandardError() {
        return this.standardError;
    }
    
    public RubyClass getRuntimeError() {
        return this.runtimeError;
    }
    
    public RubyClass getFrozenError() {
        return this.frozenError;
    }
    
    public RubyClass getIOError() {
        return this.ioError;
    }
    
    public RubyClass getLoadError() {
        return this.loadError;
    }
    
    public RubyClass getNotImplementedError() {
        return this.notImplementedError;
    }
    
    public RubyClass getSecurityError() {
        return this.securityError;
    }
    
    public RubyClass getNoMemoryError() {
        return this.noMemoryError;
    }
    
    public RubyClass getRegexpError() {
        return this.regexpError;
    }
    
    public RubyClass getInterruptedRegexpError() {
        return this.interruptedRegexpError;
    }
    
    public RubyClass getEOFError() {
        return this.eofError;
    }
    
    public RubyClass getThreadError() {
        return this.threadError;
    }
    
    public RubyClass getConcurrencyError() {
        return this.concurrencyError;
    }
    
    public RubyClass getSystemStackError() {
        return this.systemStackError;
    }
    
    public RubyClass getZeroDivisionError() {
        return this.zeroDivisionError;
    }
    
    public RubyClass getFloatDomainError() {
        return this.floatDomainError;
    }
    
    public RubyClass getMathDomainError() {
        return this.mathDomainError;
    }
    
    public RubyClass getEncodingError() {
        return this.encodingError;
    }
    
    public RubyClass getEncodingCompatibilityError() {
        return this.encodingCompatibilityError;
    }
    
    public RubyClass getConverterNotFoundError() {
        return this.converterNotFoundError;
    }
    
    public RubyClass getFiberError() {
        return this.fiberError;
    }
    
    public RubyClass getUndefinedConversionError() {
        return this.undefinedConversionError;
    }
    
    public RubyClass getInvalidByteSequenceError() {
        return this.invalidByteSequenceError;
    }
    
    @Deprecated
    public RubyRandom.RandomType getDefaultRand() {
        return this.defaultRand;
    }
    
    public void setDefaultRand(final RubyRandom.RandomType defaultRand) {
        this.defaultRand = defaultRand;
    }
    
    @Deprecated
    public RubyHash getCharsetMap() {
        if (this.charsetMap == null) {
            this.charsetMap = new RubyHash(this);
        }
        return this.charsetMap;
    }
    
    public IRubyObject getVerbose() {
        return this.verboseValue;
    }
    
    public boolean isVerbose() {
        return this.verbose;
    }
    
    public boolean warningsEnabled() {
        return this.warningsEnabled;
    }
    
    public void setVerbose(final IRubyObject verbose) {
        this.verbose = verbose.isTrue();
        this.verboseValue = verbose;
        this.warningsEnabled = !verbose.isNil();
    }
    
    public IRubyObject getDebug() {
        return this.debug ? this.trueObject : this.falseObject;
    }
    
    public boolean isDebug() {
        return this.debug;
    }
    
    public void setDebug(final IRubyObject debug) {
        this.debug = debug.isTrue();
    }
    
    public JavaSupport getJavaSupport() {
        return this.javaSupport;
    }
    
    public static ClassLoader getClassLoader() {
        ClassLoader loader = Ruby.class.getClassLoader();
        if (loader == null) {
            loader = ClassLoader.getSystemClassLoader();
        }
        return loader;
    }
    
    public JRubyClassLoader getJRubyClassLoader() {
        return this.jrubyClassLoader;
    }
    
    public void defineVariable(final GlobalVariable variable, final org.jruby.internal.runtime.GlobalVariable.Scope scope) {
        this.globalVariables.define(variable.name(), new IAccessor() {
            @Override
            public IRubyObject getValue() {
                return variable.get();
            }
            
            @Override
            public IRubyObject setValue(final IRubyObject newValue) {
                return variable.set(newValue);
            }
        }, scope);
    }
    
    public void defineReadonlyVariable(final String name, final IRubyObject value, final org.jruby.internal.runtime.GlobalVariable.Scope scope) {
        this.globalVariables.defineReadonly(name, new ValueAccessor(value), scope);
    }
    
    public Node parseFile(final InputStream in, final String file, final DynamicScope scope) {
        return this.parseFile(in, file, scope, 0);
    }
    
    public ParseResult parseFile(final String file, final InputStream in, final DynamicScope scope) {
        return this.parseFile(file, in, scope, 0);
    }
    
    public Node parseFile(final InputStream in, final String file, final DynamicScope scope, final int lineNumber) {
        this.addLoadParseToStats();
        return this.parseFileAndGetAST(in, file, scope, lineNumber, false);
    }
    
    public ParseResult parseFile(final String file, final InputStream in, final DynamicScope scope, final int lineNumber) {
        this.addLoadParseToStats();
        if (!RubyInstanceConfig.IR_READING) {
            return (ParseResult)this.parseFileAndGetAST(in, file, scope, lineNumber, false);
        }
        try {
            return IRReader.load(this.getIRManager(), new IRReaderStream(this.getIRManager(), IRFileExpert.getIRPersistedFile(file), new ByteList(file.getBytes())));
        }
        catch (IOException e) {
            return (ParseResult)this.parseFileAndGetAST(in, file, scope, lineNumber, false);
        }
    }
    
    public Node parseFileFromMain(final InputStream in, final String file, final DynamicScope scope) {
        this.addLoadParseToStats();
        return this.parseFileFromMainAndGetAST(in, file, scope);
    }
    
    public ParseResult parseFileFromMain(final String file, final InputStream in, final DynamicScope scope) {
        this.addLoadParseToStats();
        if (!RubyInstanceConfig.IR_READING) {
            return (ParseResult)this.parseFileFromMainAndGetAST(in, file, scope);
        }
        try {
            return IRReader.load(this.getIRManager(), new IRReaderStream(this.getIRManager(), IRFileExpert.getIRPersistedFile(file), new ByteList(file.getBytes())));
        }
        catch (IOException ex) {
            if (this.config.isVerbose()) {
                Ruby.LOG.info(ex);
            }
            else {
                Ruby.LOG.debug(ex);
            }
            return (ParseResult)this.parseFileFromMainAndGetAST(in, file, scope);
        }
    }
    
    private Node parseFileFromMainAndGetAST(final InputStream in, final String file, final DynamicScope scope) {
        return this.parseFileAndGetAST(in, file, scope, 0, true);
    }
    
    private Node parseFileAndGetAST(final InputStream in, final String file, final DynamicScope scope, final int lineNumber, final boolean isFromMain) {
        final ParserConfiguration parserConfig = new ParserConfiguration(this, lineNumber, false, true, isFromMain, this.config);
        this.setupSourceEncoding(parserConfig, (Encoding)UTF8Encoding.INSTANCE);
        return this.parser.parse(file, in, scope, parserConfig);
    }
    
    public Node parseInline(final InputStream in, final String file, final DynamicScope scope) {
        this.addEvalParseToStats();
        final ParserConfiguration parserConfig = new ParserConfiguration(this, 0, false, true, false, this.config);
        this.setupSourceEncoding(parserConfig, this.getEncodingService().getLocaleEncoding());
        return this.parser.parse(file, in, scope, parserConfig);
    }
    
    private void setupSourceEncoding(final ParserConfiguration parserConfig, final Encoding defaultEncoding) {
        if (this.config.getSourceEncoding() != null) {
            if (this.config.isVerbose()) {
                this.config.getError().println("-K is specified; it is for 1.8 compatibility and may cause odd behavior");
            }
            parserConfig.setDefaultEncoding(this.getEncodingService().getEncodingFromString(this.config.getSourceEncoding()));
        }
        else {
            parserConfig.setDefaultEncoding(defaultEncoding);
        }
    }
    
    public Node parseEval(final String content, final String file, final DynamicScope scope, final int lineNumber) {
        this.addEvalParseToStats();
        return this.parser.parse(file, this.encodeToBytes(content), scope, new ParserConfiguration(this, lineNumber, false, false, this.config));
    }
    
    private byte[] encodeToBytes(final String string) {
        final Charset charset = this.getDefaultCharset();
        final byte[] bytes = (charset == null) ? string.getBytes() : string.getBytes(charset);
        return bytes;
    }
    
    @Deprecated
    public Node parse(final String content, final String file, final DynamicScope scope, final int lineNumber, final boolean extraPositionInformation) {
        return this.parser.parse(file, content.getBytes(), scope, new ParserConfiguration(this, lineNumber, extraPositionInformation, false, true, this.config));
    }
    
    public Node parseEval(final ByteList content, final String file, final DynamicScope scope, final int lineNumber) {
        this.addEvalParseToStats();
        return this.parser.parse(file, content, scope, new ParserConfiguration(this, lineNumber, false, false, false, this.config));
    }
    
    public Node parse(final ByteList content, final String file, final DynamicScope scope, final int lineNumber, final boolean extraPositionInformation) {
        this.addEvalParseToStats();
        return this.parser.parse(file, content, scope, new ParserConfiguration(this, lineNumber, extraPositionInformation, false, true, this.config));
    }
    
    public ThreadService getThreadService() {
        return this.threadService;
    }
    
    public ThreadContext getCurrentContext() {
        return ThreadService.getCurrentContext(this.threadService);
    }
    
    public LoadService getLoadService() {
        return this.loadService;
    }
    
    public Encoding getDefaultInternalEncoding() {
        return this.defaultInternalEncoding;
    }
    
    public void setDefaultInternalEncoding(final Encoding defaultInternalEncoding) {
        this.defaultInternalEncoding = defaultInternalEncoding;
    }
    
    public Encoding getDefaultExternalEncoding() {
        return this.defaultExternalEncoding;
    }
    
    public void setDefaultExternalEncoding(final Encoding defaultExternalEncoding) {
        this.defaultExternalEncoding = defaultExternalEncoding;
    }
    
    public Encoding getDefaultFilesystemEncoding() {
        return this.defaultFilesystemEncoding;
    }
    
    public void setDefaultFilesystemEncoding(final Encoding defaultFilesystemEncoding) {
        this.defaultFilesystemEncoding = defaultFilesystemEncoding;
    }
    
    public Charset getDefaultCharset() {
        final Encoding enc = this.getDefaultEncoding();
        final Charset charset = EncodingUtils.charsetForEncoding(enc);
        return charset;
    }
    
    public Encoding getDefaultEncoding() {
        Encoding enc = this.getDefaultInternalEncoding();
        if (enc == null) {
            enc = (Encoding)UTF8Encoding.INSTANCE;
        }
        return enc;
    }
    
    public EncodingService getEncodingService() {
        return this.encodingService;
    }
    
    public RubyWarnings getWarnings() {
        return this.warnings;
    }
    
    WarnCallback getRegexpWarnings() {
        return this.regexpWarnings;
    }
    
    public PrintStream getErrorStream() {
        return new PrintStream(new IOOutputStream(this.getGlobalVariables().get("$stderr")));
    }
    
    public InputStream getInputStream() {
        return new IOInputStream(this.getGlobalVariables().get("$stdin"));
    }
    
    public PrintStream getOutputStream() {
        return new PrintStream(new IOOutputStream(this.getGlobalVariables().get("$stdout")));
    }
    
    public RubyModule getClassFromPath(final String path) {
        return this.getClassFromPath(path, this.getTypeError(), true);
    }
    
    public RubyModule getClassFromPath(final String path, final RubyClass undefinedExceptionClass, final boolean flexibleSearch) {
        if (path.length() == 0 || path.charAt(0) == '#') {
            throw this.newRaiseException(this.getTypeError(), RubyStringBuilder.str(this, "can't retrieve anonymous class ", RubyStringBuilder.ids(this, path)));
        }
        final ThreadContext context = this.getCurrentContext();
        RubyModule c = this.getObject();
        int pbeg = 0;
        int p = 0;
        final int l = path.length();
        while (p < l) {
            while (p < l && path.charAt(p) != ':') {
                ++p;
            }
            final String str = path.substring(pbeg, p);
            if (p < l && path.charAt(p) == ':') {
                if (++p < l && path.charAt(p) != ':') {
                    throw this.newRaiseException(undefinedExceptionClass, RubyStringBuilder.str(this, "undefined class/module ", RubyStringBuilder.ids(this, path)));
                }
                pbeg = ++p;
            }
            final boolean isJava = c instanceof JavaPackage || JavaClass.isProxyType(context, c);
            final IRubyObject cc = (flexibleSearch || isJava) ? c.getConstant(str) : c.getConstantAt(str);
            if (!flexibleSearch && cc == null) {
                return null;
            }
            if (!(cc instanceof RubyModule)) {
                throw this.newRaiseException(this.getTypeError(), RubyStringBuilder.str(this, RubyStringBuilder.ids(this, path), " does not refer to class/module"));
            }
            c = (RubyModule)cc;
        }
        return c;
    }
    
    public void printError(final RubyException ex) {
        if (ex == null) {
            return;
        }
        final PrintStream errorStream = this.getErrorStream();
        final String backtrace = this.config.getTraceType().printBacktrace(ex, errorStream == System.err && this.getPosix().isatty(FileDescriptor.err));
        try {
            errorStream.print(backtrace);
        }
        catch (Exception e) {
            System.err.print(backtrace);
        }
    }
    
    public void printError(final Throwable ex) {
        if (ex instanceof RaiseException) {
            this.printError(((RaiseException)ex).getException());
            return;
        }
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final PrintStream errorStream = this.getErrorStream();
        ex.printStackTrace(new PrintStream(baos));
        try {
            errorStream.write(baos.toByteArray());
        }
        catch (Exception e) {
            try {
                System.err.write(baos.toByteArray());
            }
            catch (IOException ioe) {
                ioe.initCause(e);
                throw new RuntimeException("BUG: could not write exception trace", ioe);
            }
        }
    }
    
    public void loadFile(final String scriptName, final InputStream in, final boolean wrap) {
        final IRubyObject self = wrap ? this.getTopSelf().rbClone() : this.getTopSelf();
        final ThreadContext context = this.getCurrentContext();
        try {
            context.preNodeEval(self);
            final ParseResult parseResult = this.parseFile(scriptName, in, null);
            if (wrap) {
                this.wrapWithModule((RubyBasicObject)self, parseResult);
            }
            this.runInterpreter(context, parseResult, self);
        }
        finally {
            context.postNodeEval();
        }
    }
    
    public void loadScope(final IRScope scope, final boolean wrap) {
        final IRubyObject self = wrap ? TopSelfFactory.createTopSelf(this, true) : this.getTopSelf();
        if (wrap) {
            scope.getStaticScope().setModule(RubyModule.newModule(this));
        }
        this.runInterpreter(this.getCurrentContext(), scope, self);
    }
    
    public void compileAndLoadFile(final String filename, final InputStream in, final boolean wrap) {
        final IRubyObject self = wrap ? this.getTopSelf().rbClone() : this.getTopSelf();
        final ParseResult parseResult = this.parseFile(filename, in, null);
        final RootNode root = (RootNode)parseResult;
        if (wrap) {
            this.wrapWithModule((RubyBasicObject)self, root);
        }
        else {
            root.getStaticScope().setModule(this.getObject());
        }
        this.runNormally(root, wrap);
    }
    
    private void wrapWithModule(final RubyBasicObject self, final ParseResult result) {
        final RubyModule wrapper = RubyModule.newModule(this);
        self.extend(new IRubyObject[] { wrapper });
        final StaticScope top = result.getStaticScope();
        final StaticScope newTop = this.staticScopeFactory.newLocalScope(null);
        top.setPreviousCRefScope(newTop);
        top.setModule(wrapper);
    }
    
    public void loadScript(final Script script) {
        this.loadScript(script, false);
    }
    
    public void loadScript(final Script script, final boolean wrap) {
        script.load(this.getCurrentContext(), this.getTopSelf(), wrap);
    }
    
    public void loadExtension(final String extName, final BasicLibraryService extension, final boolean wrap) {
        final IRubyObject self = wrap ? TopSelfFactory.createTopSelf(this, true) : this.getTopSelf();
        final ThreadContext context = this.getCurrentContext();
        try {
            context.preExtensionLoad(self);
            extension.basicLoad(this);
        }
        catch (IOException ioe) {
            throw this.newIOErrorFromException(ioe);
        }
        finally {
            context.postNodeEval();
        }
    }
    
    public void addBoundMethod(final String className, final String methodName, final String rubyName) {
        Map<String, String> javaToRuby = this.boundMethods.get(className);
        if (javaToRuby == null) {
            this.boundMethods.put(className, javaToRuby = new HashMap<String, String>());
        }
        javaToRuby.put(methodName, rubyName);
    }
    
    public void addBoundMethods(final String className, final String... tuples) {
        Map<String, String> javaToRuby = this.boundMethods.get(className);
        if (javaToRuby == null) {
            this.boundMethods.put(className, javaToRuby = new HashMap<String, String>(tuples.length / 2 + 1, 1.0f));
        }
        for (int i = 0; i < tuples.length; i += 2) {
            javaToRuby.put(tuples[i], tuples[i + 1]);
        }
    }
    
    @Deprecated
    public void addBoundMethodsPacked(final String className, final String packedTuples) {
        final List<String> names = StringSupport.split(packedTuples, ';');
        for (int i = 0; i < names.size(); i += 2) {
            this.addBoundMethod(className, names.get(i), names.get(i + 1));
        }
    }
    
    @Deprecated
    public void addSimpleBoundMethodsPacked(final String className, final String packedNames) {
        final List<String> names = StringSupport.split(packedNames, ';');
        for (final String name : names) {
            this.addBoundMethod(className, name, name);
        }
    }
    
    public Map<String, Map<String, String>> getBoundMethods() {
        return this.boundMethods;
    }
    
    public void setJavaProxyClassFactory(final JavaProxyClassFactory factory) {
        this.javaProxyClassFactory = factory;
    }
    
    public JavaProxyClassFactory getJavaProxyClassFactory() {
        return this.javaProxyClassFactory;
    }
    
    public synchronized void addEventHook(final EventHook hook) {
        if (!RubyInstanceConfig.FULL_TRACE_ENABLED && hook.needsDebug()) {
            this.getWarnings().warn("tracing (e.g. set_trace_func) will not capture all events without --debug flag");
        }
        final EventHook[] hooks = this.eventHooks;
        final EventHook[] newHooks = Arrays.copyOf(hooks, hooks.length + 1);
        newHooks[hooks.length] = hook;
        this.eventHooks = newHooks;
        this.hasEventHooks = true;
    }
    
    public synchronized void removeEventHook(final EventHook hook) {
        final EventHook[] hooks = this.eventHooks;
        if (hooks.length == 0) {
            return;
        }
        int pivot = -1;
        for (int i = 0; i < hooks.length; ++i) {
            if (hooks[i] == hook) {
                pivot = i;
                break;
            }
        }
        if (pivot == -1) {
            return;
        }
        final EventHook[] newHooks = new EventHook[hooks.length - 1];
        if (pivot != 0) {
            System.arraycopy(hooks, 0, newHooks, 0, pivot);
        }
        if (pivot != hooks.length - 1) {
            System.arraycopy(hooks, pivot + 1, newHooks, pivot, hooks.length - (pivot + 1));
        }
        this.eventHooks = newHooks;
        this.hasEventHooks = (newHooks.length > 0);
    }
    
    public void setTraceFunction(final RubyProc traceFunction) {
        this.removeEventHook(this.callTraceFuncHook);
        if (traceFunction == null) {
            return;
        }
        this.callTraceFuncHook.setTraceFunc(traceFunction);
        this.addEventHook(this.callTraceFuncHook);
    }
    
    public void callEventHooks(final ThreadContext context, final RubyEvent event, final String file, final int line, final String name, final IRubyObject type) {
        if (context.isEventHooksEnabled()) {
            final EventHook[] eventHooks;
            final EventHook[] hooks = eventHooks = this.eventHooks;
            for (final EventHook eventHook : eventHooks) {
                if (eventHook.isInterestedInEvent(event)) {
                    IRubyObject klass = context.nil;
                    if (type instanceof RubyModule) {
                        if (((RubyModule)type).isIncluded()) {
                            klass = ((RubyModule)type).getNonIncludedClass();
                        }
                        else if (((RubyModule)type).isSingleton()) {
                            klass = ((MetaClass)type).getAttached();
                        }
                    }
                    eventHook.event(context, event, file, line, name, klass);
                }
            }
        }
    }
    
    public boolean hasEventHooks() {
        return this.hasEventHooks;
    }
    
    public GlobalVariables getGlobalVariables() {
        return this.globalVariables;
    }
    
    @Deprecated
    public void setGlobalVariables(final GlobalVariables globalVariables) {
    }
    
    public IRubyObject pushExitBlock(final RubyProc proc) {
        this.atExitBlocks.push(proc);
        return proc;
    }
    
    public void pushEndBlock(final RubyProc proc) {
        if (this.alreadyRegisteredEndBlock(proc) != null) {
            return;
        }
        this.pushExitBlock(proc);
    }
    
    private RubyProc alreadyRegisteredEndBlock(final RubyProc newProc) {
        final Block block = newProc.getBlock();
        for (final RubyProc proc : this.atExitBlocks) {
            if (block.equals(proc.getBlock())) {
                return proc;
            }
        }
        return null;
    }
    
    public void addInternalFinalizer(final Finalizable finalizer) {
        synchronized (this.internalFinalizersMutex) {
            if (this.internalFinalizers == null) {
                this.internalFinalizers = new WeakHashMap<Finalizable, Object>();
            }
            this.internalFinalizers.put(finalizer, null);
        }
    }
    
    public void addFinalizer(final Finalizable finalizer) {
        synchronized (this.finalizersMutex) {
            if (this.finalizers == null) {
                this.finalizers = new WeakHashMap<Finalizable, Object>();
            }
            this.finalizers.put(finalizer, null);
        }
    }
    
    public void removeInternalFinalizer(final Finalizable finalizer) {
        synchronized (this.internalFinalizersMutex) {
            if (this.internalFinalizers != null) {
                this.internalFinalizers.remove(finalizer);
            }
        }
    }
    
    public void removeFinalizer(final Finalizable finalizer) {
        synchronized (this.finalizersMutex) {
            if (this.finalizers != null) {
                this.finalizers.remove(finalizer);
            }
        }
    }
    
    public void tearDown() {
        this.tearDown(true);
    }
    
    public void tearDown(final boolean systemExit) {
        int status = 0;
        this.mriRecursionGuard = null;
        final ThreadContext context = this.getCurrentContext();
        if (!context.hasAnyScopes()) {
            final StaticScope topStaticScope = this.getStaticScopeFactory().newLocalScope(null);
            context.pushScope(new ManyVarsDynamicScope(topStaticScope, null));
        }
        while (!this.atExitBlocks.empty()) {
            final RubyProc proc = this.atExitBlocks.pop();
            try {
                proc.call(context, IRubyObject.NULL_ARRAY);
            }
            catch (RaiseException rj) {
                if (rj.getException() instanceof RubyLocalJumpError) {
                    final RubyLocalJumpError rlje = (RubyLocalJumpError)rj.getException();
                    final String filename = proc.getBlock().getBinding().filename;
                    if (rlje.getReason() == RubyLocalJumpError.Reason.RETURN) {
                        this.getWarnings().warn(filename, "unexpected return");
                    }
                    else {
                        this.getWarnings().warn(filename, "break from proc-closure");
                    }
                }
                else {
                    final RubyException raisedException = rj.getException();
                    if (!this.getSystemExit().isInstance(raisedException)) {
                        status = 1;
                        this.printError(raisedException);
                    }
                    else {
                        final IRubyObject statusObj = raisedException.callMethod(context, "status");
                        if (statusObj == null || statusObj.isNil()) {
                            continue;
                        }
                        status = RubyNumeric.fix2int(statusObj);
                    }
                }
            }
            catch (IRReturnJump e) {
                this.getWarnings().warn(proc.getBlock().getBinding().filename, "unexpected return");
            }
        }
        final IRubyObject trapResult = RubySignal.__jtrap_osdefault_kernel(this.getNil(), this.newString("EXIT"));
        if (trapResult instanceof RubyArray) {
            final IRubyObject[] trapResultEntries = ((RubyArray)trapResult).toJavaArray();
            final IRubyObject exitHandlerProc = trapResultEntries[0];
            if (exitHandlerProc instanceof RubyProc) {
                ((RubyProc)exitHandlerProc).call(context, this.getSingleNilArray());
            }
        }
        if (this.finalizers != null) {
            synchronized (this.finalizersMutex) {
                final Iterator<Finalizable> finalIter = new ArrayList<Finalizable>(this.finalizers.keySet()).iterator();
                while (finalIter.hasNext()) {
                    final Finalizable f = finalIter.next();
                    if (f != null) {
                        try {
                            f.finalize();
                        }
                        catch (Throwable t) {}
                    }
                    finalIter.remove();
                }
            }
        }
        synchronized (this.internalFinalizersMutex) {
            if (this.internalFinalizers != null) {
                final Iterator<Finalizable> finalIter = new ArrayList<Finalizable>(this.internalFinalizers.keySet()).iterator();
                while (finalIter.hasNext()) {
                    final Finalizable f = finalIter.next();
                    if (f != null) {
                        try {
                            f.finalize();
                        }
                        catch (Throwable t2) {}
                    }
                    finalIter.remove();
                }
            }
        }
        this.getBeanManager().unregisterCompiler();
        this.getBeanManager().unregisterConfig();
        this.getBeanManager().unregisterParserStats();
        this.getBeanManager().unregisterMethodCache();
        this.getBeanManager().unregisterRuntime();
        this.getSelectorPool().cleanup();
        if (this.config.isProfilingEntireRun()) {
            final ProfileCollection profileCollection = this.threadService.getMainThread().getContext().getProfileCollection();
            this.printProfileData(profileCollection);
        }
        this.threadService = new ThreadService(this);
        this.getJITCompiler().shutdown();
        this.getExecutor().shutdown();
        this.getFiberExecutor().shutdown();
        if (systemExit && status != 0) {
            throw this.newSystemExit(status);
        }
        if (this == Ruby.globalRuntime) {
            synchronized (Ruby.class) {
                if (this == Ruby.globalRuntime) {
                    Ruby.globalRuntime = null;
                }
            }
        }
    }
    
    public void releaseClassLoader() {
        if (this.jrubyClassLoader != null) {
            this.jrubyClassLoader.close();
        }
    }
    
    public synchronized void printProfileData(final ProfileCollection profileData) {
        this.getProfilingService().newProfileReporter(this.getCurrentContext()).report(profileData);
    }
    
    private ProfilingServiceLookup getProfilingServiceLookup() {
        return this.profilingServiceLookup;
    }
    
    public ProfilingService getProfilingService() {
        final ProfilingServiceLookup lockup = this.getProfilingServiceLookup();
        return (lockup == null) ? null : lockup.getService();
    }
    
    public RubyArray newEmptyArray() {
        return RubyArray.newEmptyArray(this);
    }
    
    public RubyArray newArray() {
        return RubyArray.newArray(this);
    }
    
    public RubyArray newArrayLight() {
        return RubyArray.newArrayLight(this);
    }
    
    public RubyArray newArray(final IRubyObject object) {
        return RubyArray.newArray(this, object);
    }
    
    public RubyArray newArray(final IRubyObject car, final IRubyObject cdr) {
        return RubyArray.newArray(this, car, cdr);
    }
    
    public RubyArray newArray(final IRubyObject... objects) {
        return RubyArray.newArray(this, objects);
    }
    
    public RubyArray newArrayNoCopy(final IRubyObject... objects) {
        return RubyArray.newArrayNoCopy(this, objects);
    }
    
    public RubyArray newArrayNoCopyLight(final IRubyObject... objects) {
        return RubyArray.newArrayNoCopyLight(this, objects);
    }
    
    public RubyArray newArray(final List<IRubyObject> list) {
        return RubyArray.newArray(this, list);
    }
    
    public RubyArray newArray(final int size) {
        return RubyArray.newArray(this, size);
    }
    
    public RubyArray getEmptyFrozenArray() {
        return this.emptyFrozenArray;
    }
    
    public RubyBoolean newBoolean(final boolean value) {
        return value ? this.trueObject : this.falseObject;
    }
    
    public RubyFileStat newFileStat(final String filename, final boolean lstat) {
        return RubyFileStat.newFileStat(this, filename, lstat);
    }
    
    public RubyFileStat newFileStat(final FileDescriptor descriptor) {
        return RubyFileStat.newFileStat(this, descriptor);
    }
    
    public RubyFixnum newFixnum(final long value) {
        return RubyFixnum.newFixnum(this, value);
    }
    
    public RubyFixnum newFixnum(final int value) {
        return RubyFixnum.newFixnum(this, value);
    }
    
    public RubyFixnum newFixnum(final Constant value) {
        return RubyFixnum.newFixnum(this, value.intValue());
    }
    
    public RubyFloat newFloat(final double value) {
        return RubyFloat.newFloat(this, value);
    }
    
    public RubyNumeric newNumeric() {
        return RubyNumeric.newNumeric(this);
    }
    
    public RubyRational newRational(final long num, final long den) {
        return RubyRational.newRationalRaw(this, this.newFixnum(num), this.newFixnum(den));
    }
    
    public RubyRational newRationalReduced(final long num, final long den) {
        return (RubyRational)RubyRational.newRationalConvert(this.getCurrentContext(), this.newFixnum(num), this.newFixnum(den));
    }
    
    public RubyProc newProc(final Block.Type type, final Block block) {
        if (type != Block.Type.LAMBDA && block.getProcObject() != null) {
            return block.getProcObject();
        }
        return RubyProc.newProc(this, block, type);
    }
    
    public RubyProc newBlockPassProc(final Block.Type type, final Block block) {
        if (type != Block.Type.LAMBDA && block.getProcObject() != null) {
            return block.getProcObject();
        }
        return RubyProc.newProc(this, block, type);
    }
    
    public RubyBinding newBinding() {
        return RubyBinding.newBinding(this, this.getCurrentContext().currentBinding());
    }
    
    public RubyBinding newBinding(final Binding binding) {
        return RubyBinding.newBinding(this, binding);
    }
    
    public RubyString newString() {
        return RubyString.newString(this, new ByteList());
    }
    
    public RubyString newString(final String string) {
        return RubyString.newString(this, string);
    }
    
    public RubyString newString(final ByteList byteList) {
        return RubyString.newString(this, byteList);
    }
    
    @Deprecated
    public RubyString newStringShared(final ByteList byteList) {
        return RubyString.newStringShared(this, byteList);
    }
    
    public RubySymbol newSymbol(final String name) {
        return this.symbolTable.getSymbol(name);
    }
    
    public RubySymbol newSymbol(final String name, final Encoding encoding) {
        final ByteList byteList = RubyString.encodeBytelist(name, encoding);
        return this.symbolTable.getSymbol(byteList);
    }
    
    public RubySymbol newSymbol(final ByteList name) {
        return this.symbolTable.getSymbol(name);
    }
    
    public RubySymbol fastNewSymbol(final String internedName) {
        return this.symbolTable.fastGetSymbol(internedName);
    }
    
    public RubyTime newTime(final long milliseconds) {
        return RubyTime.newTime(this, milliseconds);
    }
    
    public RaiseException newRuntimeError(final String message) {
        return this.newRaiseException(this.getRuntimeError(), message);
    }
    
    public RaiseException newArgumentError(final String message) {
        return this.newRaiseException(this.getArgumentError(), message);
    }
    
    public RaiseException newArgumentError(final int got, final int expected) {
        return this.newArgumentError(got, expected, expected);
    }
    
    public RaiseException newArgumentError(final int got, final int min, final int max) {
        if (min == max) {
            return this.newRaiseException(this.getArgumentError(), "wrong number of arguments (given " + got + ", expected " + min + ")");
        }
        if (max == -1) {
            return this.newRaiseException(this.getArgumentError(), "wrong number of arguments (given " + got + ", expected " + min + "+)");
        }
        return this.newRaiseException(this.getArgumentError(), "wrong number of arguments (given " + got + ", expected " + min + ".." + max + ")");
    }
    
    public RaiseException newArgumentError(final String name, final int got, final int expected) {
        return this.newArgumentError(name, got, expected, expected);
    }
    
    public RaiseException newArgumentError(final String name, final int got, final int min, final int max) {
        if (min == max) {
            return this.newRaiseException(this.getArgumentError(), RubyStringBuilder.str(this, "wrong number of arguments calling `", RubyStringBuilder.ids(this, name), "` (given " + got + ", expected " + min + ")"));
        }
        if (max == -1) {
            return this.newRaiseException(this.getArgumentError(), RubyStringBuilder.str(this, "wrong number of arguments calling `", RubyStringBuilder.ids(this, name), "` (given " + got + ", expected " + min + "+)"));
        }
        return this.newRaiseException(this.getArgumentError(), RubyStringBuilder.str(this, "wrong number of arguments calling `", RubyStringBuilder.ids(this, name), "` (given " + got + ", expected " + min + ".." + max + ")"));
    }
    
    public RaiseException newErrnoEBADFError() {
        return this.newRaiseException(this.getErrno().getClass("EBADF"), "Bad file descriptor");
    }
    
    public RaiseException newErrnoEISCONNError() {
        return this.newRaiseException(this.getErrno().getClass("EISCONN"), "Socket is already connected");
    }
    
    public RaiseException newErrnoEINPROGRESSError() {
        return this.newRaiseException(this.getErrno().getClass("EINPROGRESS"), "Operation now in progress");
    }
    
    public RaiseException newErrnoEINPROGRESSWritableError() {
        return this.newLightweightErrnoException(this.getIO().getClass("EINPROGRESSWaitWritable"), "");
    }
    
    public RaiseException newErrnoENOPROTOOPTError() {
        return this.newRaiseException(this.getErrno().getClass("ENOPROTOOPT"), "Protocol not available");
    }
    
    public RaiseException newErrnoEPIPEError() {
        return this.newRaiseException(this.getErrno().getClass("EPIPE"), "Broken pipe");
    }
    
    public RaiseException newErrnoECONNABORTEDError() {
        return this.newRaiseException(this.getErrno().getClass("ECONNABORTED"), "An established connection was aborted by the software in your host machine");
    }
    
    public RaiseException newErrnoECONNREFUSEDError() {
        return this.newRaiseException(this.getErrno().getClass("ECONNREFUSED"), "Connection refused");
    }
    
    public RaiseException newErrnoECONNREFUSEDError(final String message) {
        return this.newRaiseException(this.getErrno().getClass("ECONNREFUSED"), message);
    }
    
    public RaiseException newErrnoECONNRESETError() {
        return this.newRaiseException(this.getErrno().getClass("ECONNRESET"), "Connection reset by peer");
    }
    
    public RaiseException newErrnoEADDRINUSEError() {
        return this.newRaiseException(this.getErrno().getClass("EADDRINUSE"), "Address in use");
    }
    
    public RaiseException newErrnoEADDRINUSEError(final String message) {
        return this.newRaiseException(this.getErrno().getClass("EADDRINUSE"), message);
    }
    
    public RaiseException newErrnoEHOSTUNREACHError(final String message) {
        return this.newRaiseException(this.getErrno().getClass("EHOSTUNREACH"), message);
    }
    
    public RaiseException newErrnoEINVALError() {
        return this.newRaiseException(this.getErrno().getClass("EINVAL"), "Invalid file");
    }
    
    public RaiseException newErrnoELOOPError() {
        return this.newRaiseException(this.getErrno().getClass("ELOOP"), "Too many levels of symbolic links");
    }
    
    public RaiseException newErrnoEMFILEError() {
        return this.newRaiseException(this.getErrno().getClass("EMFILE"), "Too many open files");
    }
    
    public RaiseException newErrnoENFILEError() {
        return this.newRaiseException(this.getErrno().getClass("ENFILE"), "Too many open files in system");
    }
    
    public RaiseException newErrnoENOENTError() {
        return this.newRaiseException(this.getErrno().getClass("ENOENT"), "File not found");
    }
    
    public RaiseException newErrnoEACCESError(final String message) {
        return this.newRaiseException(this.getErrno().getClass("EACCES"), message);
    }
    
    public RaiseException newErrnoEAGAINError(final String message) {
        return this.newLightweightErrnoException(this.getErrno().getClass("EAGAIN"), message);
    }
    
    public RaiseException newErrnoEAGAINReadableError(final String message) {
        return this.newLightweightErrnoException(this.getIO().getClass("EAGAINWaitReadable"), message);
    }
    
    public RaiseException newErrnoEAGAINWritableError(final String message) {
        return this.newLightweightErrnoException(this.getIO().getClass("EAGAINWaitWritable"), message);
    }
    
    public RaiseException newErrnoEISDirError(final String message) {
        return this.newRaiseException(this.getErrno().getClass("EISDIR"), message);
    }
    
    public RaiseException newErrnoEPERMError(final String name) {
        return this.newRaiseException(this.getErrno().getClass("EPERM"), "Operation not permitted - " + name);
    }
    
    public RaiseException newErrnoEISDirError() {
        return this.newErrnoEISDirError("Is a directory");
    }
    
    public RaiseException newErrnoESPIPEError() {
        return this.newRaiseException(this.getErrno().getClass("ESPIPE"), "Illegal seek");
    }
    
    public RaiseException newErrnoEBADFError(final String message) {
        return this.newRaiseException(this.getErrno().getClass("EBADF"), message);
    }
    
    public RaiseException newErrnoEINPROGRESSError(final String message) {
        return this.newRaiseException(this.getErrno().getClass("EINPROGRESS"), message);
    }
    
    public RaiseException newErrnoEINPROGRESSWritableError(final String message) {
        return this.newLightweightErrnoException(this.getIO().getClass("EINPROGRESSWaitWritable"), message);
    }
    
    public RaiseException newErrnoEISCONNError(final String message) {
        return this.newRaiseException(this.getErrno().getClass("EISCONN"), message);
    }
    
    public RaiseException newErrnoEINVALError(final String message) {
        return this.newRaiseException(this.getErrno().getClass("EINVAL"), message);
    }
    
    public RaiseException newErrnoENOTDIRError(final String message) {
        return this.newRaiseException(this.getErrno().getClass("ENOTDIR"), message);
    }
    
    public RaiseException newErrnoENOTEMPTYError(final String message) {
        return this.newRaiseException(this.getErrno().getClass("ENOTEMPTY"), message);
    }
    
    public RaiseException newErrnoENOTSOCKError(final String message) {
        return this.newRaiseException(this.getErrno().getClass("ENOTSOCK"), message);
    }
    
    public RaiseException newErrnoENOTCONNError(final String message) {
        return this.newRaiseException(this.getErrno().getClass("ENOTCONN"), message);
    }
    
    public RaiseException newErrnoENOTCONNError() {
        return this.newRaiseException(this.getErrno().getClass("ENOTCONN"), "Socket is not connected");
    }
    
    public RaiseException newErrnoENOENTError(final String message) {
        return this.newRaiseException(this.getErrno().getClass("ENOENT"), message);
    }
    
    public RaiseException newErrnoEOPNOTSUPPError(final String message) {
        return this.newRaiseException(this.getErrno().getClass("EOPNOTSUPP"), message);
    }
    
    public RaiseException newErrnoESPIPEError(final String message) {
        return this.newRaiseException(this.getErrno().getClass("ESPIPE"), message);
    }
    
    public RaiseException newErrnoEEXISTError(final String message) {
        return this.newRaiseException(this.getErrno().getClass("EEXIST"), message);
    }
    
    public RaiseException newErrnoEDOMError(final String message) {
        return this.newRaiseException(this.getErrno().getClass("EDOM"), "Domain error - " + message);
    }
    
    public RaiseException newErrnoECHILDError() {
        return this.newRaiseException(this.getErrno().getClass("ECHILD"), "No child processes");
    }
    
    public RaiseException newErrnoEADDRNOTAVAILError(final String message) {
        return this.newRaiseException(this.getErrno().getClass("EADDRNOTAVAIL"), message);
    }
    
    public RaiseException newErrnoESRCHError() {
        return this.newRaiseException(this.getErrno().getClass("ESRCH"), null);
    }
    
    public RaiseException newErrnoEWOULDBLOCKError() {
        return this.newRaiseException(this.getErrno().getClass("EWOULDBLOCK"), null);
    }
    
    public RaiseException newErrnoEDESTADDRREQError(final String func) {
        return this.newRaiseException(this.getErrno().getClass("EDESTADDRREQ"), func);
    }
    
    public RaiseException newErrnoENETUNREACHError() {
        return this.newRaiseException(this.getErrno().getClass("ENETUNREACH"), null);
    }
    
    public RaiseException newErrnoEMSGSIZEError() {
        return this.newRaiseException(this.getErrno().getClass("EMSGSIZE"), null);
    }
    
    public RaiseException newErrnoEXDEVError(final String message) {
        return this.newRaiseException(this.getErrno().getClass("EXDEV"), message);
    }
    
    public RaiseException newIndexError(final String message) {
        return this.newRaiseException(this.getIndexError(), message);
    }
    
    public RaiseException newSecurityError(final String message) {
        return this.newRaiseException(this.getSecurityError(), message);
    }
    
    public RaiseException newSystemCallError(final String message) {
        return this.newRaiseException(this.getSystemCallError(), message);
    }
    
    public RaiseException newKeyError(final String message, final IRubyObject recv, final IRubyObject key) {
        return new RubyKeyError(this, this.getKeyError(), message, recv, key).toThrowable();
    }
    
    public RaiseException newErrnoEINTRError() {
        return this.newRaiseException(this.getErrno().getClass("EINTR"), "Interrupted");
    }
    
    public RaiseException newErrnoEAFNOSUPPORTError(final String message) {
        return this.newRaiseException(this.getErrno().getClass("EAFNOSUPPORT"), message);
    }
    
    public RaiseException newErrnoFromLastPOSIXErrno() {
        RubyClass errnoClass = this.getErrno(this.getPosix().errno());
        if (errnoClass == null) {
            errnoClass = this.systemCallError;
        }
        return this.newRaiseException(errnoClass, null);
    }
    
    public RaiseException newErrnoFromInt(final int errno, final String methodName, final String message) {
        if (Platform.IS_WINDOWS && ("stat".equals(methodName) || "lstat".equals(methodName))) {
            if (errno == 20047) {
                return this.newErrnoENOENTError(message);
            }
            if (errno == Errno.ESRCH.intValue()) {
                return this.newErrnoENOENTError(message);
            }
        }
        return this.newErrnoFromInt(errno, message);
    }
    
    public RaiseException newErrnoFromInt(final int errno, final String message) {
        final RubyClass errnoClass = this.getErrno(errno);
        if (errnoClass != null) {
            return this.newRaiseException(errnoClass, message);
        }
        return this.newSystemCallError("Unknown Error (" + errno + ") - " + message);
    }
    
    public RaiseException newErrnoFromErrno(final Errno errno, final String message) {
        if (errno == null || errno == Errno.__UNKNOWN_CONSTANT__) {
            return this.newSystemCallError(message);
        }
        return this.newErrnoFromInt(errno.intValue(), message);
    }
    
    public RaiseException newErrnoFromInt(final int errno) {
        final Errno errnoObj = Errno.valueOf((long)errno);
        if (errnoObj == null) {
            return this.newSystemCallError("Unknown Error (" + errno + ")");
        }
        final String message = errnoObj.description();
        return this.newErrnoFromInt(errno, message);
    }
    
    public RaiseException newErrnoFromBindException(final BindException be, final String contextMessage) {
        final Errno errno = Helpers.errnoFromException(be);
        if (errno != null) {
            return this.newErrnoFromErrno(errno, contextMessage);
        }
        return this.newErrnoEADDRFromBindException(be, contextMessage);
    }
    
    public RaiseException newErrnoEADDRFromBindException(final BindException be, final String contextMessage) {
        String msg = be.getMessage();
        if (msg == null) {
            msg = "bind";
        }
        else {
            msg = "bind - " + msg;
        }
        if (contextMessage != null) {
            msg += contextMessage;
        }
        if (Ruby.ADDR_NOT_AVAIL_PATTERN.matcher(msg).find()) {
            return this.newErrnoEADDRNOTAVAILError(msg);
        }
        return this.newErrnoEADDRINUSEError(msg);
    }
    
    public RaiseException newTypeError(final String message) {
        return this.newRaiseException(this.getTypeError(), message);
    }
    
    public RaiseException newThreadError(final String message) {
        return this.newRaiseException(this.getThreadError(), message);
    }
    
    public RaiseException newConcurrencyError(final String message) {
        return this.newRaiseException(this.getConcurrencyError(), message);
    }
    
    public RaiseException newSyntaxError(final String message) {
        return this.newRaiseException(this.getSyntaxError(), message);
    }
    
    public RaiseException newRegexpError(final String message) {
        return this.newRaiseException(this.getRegexpError(), message);
    }
    
    public RaiseException newInterruptedRegexpError(final String message) {
        return this.newRaiseException(this.getInterruptedRegexpError(), message);
    }
    
    public RaiseException newRangeError(final String message) {
        return this.newRaiseException(this.getRangeError(), message);
    }
    
    public RaiseException newNotImplementedError(final String message) {
        return this.newRaiseException(this.getNotImplementedError(), message);
    }
    
    @Deprecated
    public RaiseException newInvalidEncoding(final String message) {
        return this.newRaiseException(this.getClass("Iconv").getClass("InvalidEncoding"), message);
    }
    
    @Deprecated
    public RaiseException newIllegalSequence(final String message) {
        return this.newRaiseException(this.getClass("Iconv").getClass("IllegalSequence"), message);
    }
    
    public RaiseException newNameError(final String message, final IRubyObject recv, final IRubyObject name) {
        return this.newNameError(message, recv, name, false);
    }
    
    public RaiseException newNameError(final String message, final IRubyObject recv, final IRubyObject name, final boolean privateCall) {
        final IRubyObject msg = new RubyNameError.RubyNameErrorMessage(this, message, recv, name);
        final RubyException err = RubyNameError.newNameError(this.getNameError(), msg, name, privateCall);
        return err.toThrowable();
    }
    
    public RaiseException newNameError(final String message, final IRubyObject recv, final String name) {
        return this.newNameError(message, recv, name, false);
    }
    
    public RaiseException newNameError(final String message, final IRubyObject recv, final String name, final boolean privateCall) {
        final RubySymbol nameSym = this.newSymbol(name);
        return this.newNameError(message, recv, nameSym, privateCall);
    }
    
    public RaiseException newNameError(final String message, final String name, final Throwable exception, final boolean printWhenVerbose) {
        if (exception != null) {
            if (printWhenVerbose && this.isVerbose()) {
                Ruby.LOG.error(exception);
            }
            else if (this.isDebug()) {
                Ruby.LOG.debug(exception);
            }
        }
        return new RubyNameError(this, this.getNameError(), message, name).toThrowable();
    }
    
    public RaiseException newNameError(final String message, final String name) {
        return this.newNameError(message, name, null);
    }
    
    public RaiseException newNameError(final String message, final String name, final Throwable origException) {
        return this.newNameError(message, name, origException, false);
    }
    
    public RaiseException newNoMethodError(final String message, final IRubyObject recv, final String name, final RubyArray args) {
        return this.newNoMethodError(message, recv, name, args, false);
    }
    
    public RaiseException newNoMethodError(final String message, final IRubyObject recv, final String name, final RubyArray args, final boolean privateCall) {
        final RubySymbol nameStr = this.newSymbol(name);
        final IRubyObject msg = new RubyNameError.RubyNameErrorMessage(this, message, recv, nameStr);
        final RubyException err = RubyNoMethodError.newNoMethodError(this.getNoMethodError(), msg, nameStr, args, privateCall);
        return err.toThrowable();
    }
    
    public RaiseException newNoMethodError(final String message, final String name, final IRubyObject args) {
        return new RubyNoMethodError(this, this.getNoMethodError(), message, name, args).toThrowable();
    }
    
    public RaiseException newLocalJumpError(final RubyLocalJumpError.Reason reason, final IRubyObject exitValue, final String message) {
        return new RubyLocalJumpError(this, this.getLocalJumpError(), message, reason, exitValue).toThrowable();
    }
    
    public RaiseException newLocalJumpErrorNoBlock() {
        return this.newLocalJumpError(RubyLocalJumpError.Reason.NOREASON, this.getNil(), "no block given");
    }
    
    public RaiseException newRedoLocalJumpError() {
        return this.newLocalJumpError(RubyLocalJumpError.Reason.REDO, this.getNil(), "unexpected redo");
    }
    
    public RaiseException newLoadError(final String message) {
        return this.newRaiseException(this.getLoadError(), message);
    }
    
    public RaiseException newLoadError(final String message, final String path) {
        final RaiseException loadError = this.newRaiseException(this.getLoadError(), message);
        loadError.getException().setInstanceVariable("@path", this.newString(path));
        return loadError;
    }
    
    public RaiseException newFrozenError(final String objectType) {
        return this.newFrozenError(objectType, false);
    }
    
    public RaiseException newFrozenError(final RubyModule type) {
        return this.newRaiseException(this.getFrozenError(), RubyStringBuilder.str(this, "can't modify frozen ", RubyStringBuilder.types(this, type)));
    }
    
    public RaiseException newFrozenError(final String objectType, final boolean runtimeError) {
        return this.newRaiseException(this.getFrozenError(), RubyStringBuilder.str(this, "can't modify frozen ", RubyStringBuilder.ids(this, objectType)));
    }
    
    public RaiseException newSystemStackError(final String message) {
        return this.newRaiseException(this.getSystemStackError(), message);
    }
    
    public RaiseException newSystemStackError(final String message, final StackOverflowError error) {
        if (this.isDebug()) {
            Ruby.LOG.debug(error);
        }
        return this.newRaiseException(this.getSystemStackError(), message);
    }
    
    public RaiseException newSystemExit(final int status) {
        return RubySystemExit.newInstance(this, status, "exit").toThrowable();
    }
    
    public RaiseException newSystemExit(final int status, final String message) {
        return RubySystemExit.newInstance(this, status, message).toThrowable();
    }
    
    public RaiseException newIOError(final String message) {
        return this.newRaiseException(this.getIOError(), message);
    }
    
    public RaiseException newStandardError(final String message) {
        return this.newRaiseException(this.getStandardError(), message);
    }
    
    public RaiseException newIOErrorFromException(final IOException ex) {
        return Helpers.newIOErrorFromException(this, ex);
    }
    
    public RaiseException newTypeError(final IRubyObject receivedObject, final RubyClass expectedType) {
        return this.newTypeError(receivedObject, expectedType.getName());
    }
    
    public RaiseException newTypeError(final IRubyObject receivedObject, final RubyModule expectedType) {
        return this.newTypeError(receivedObject, expectedType.getName());
    }
    
    public RaiseException newTypeError(final IRubyObject receivedObject, final String expectedType) {
        return this.newRaiseException(this.getTypeError(), RubyStringBuilder.str(this, "wrong argument type ", receivedObject.getMetaClass().getRealClass().toRubyString(this.getCurrentContext()), " (expected ", RubyStringBuilder.ids(this, expectedType), ")"));
    }
    
    public RaiseException newEOFError() {
        return this.newRaiseException(this.getEOFError(), "End of file reached");
    }
    
    public RaiseException newEOFError(final String message) {
        return this.newRaiseException(this.getEOFError(), message);
    }
    
    public RaiseException newZeroDivisionError() {
        return this.newRaiseException(this.getZeroDivisionError(), "divided by 0");
    }
    
    public RaiseException newFloatDomainError(final String message) {
        return this.newRaiseException(this.getFloatDomainError(), message);
    }
    
    public RaiseException newMathDomainError(final String message) {
        return this.newRaiseException(this.getMathDomainError(), "Numerical argument is out of domain - \"" + message + "\"");
    }
    
    public RaiseException newEncodingError(final String message) {
        return this.newRaiseException(this.getEncodingError(), message);
    }
    
    public RaiseException newEncodingCompatibilityError(final String message) {
        return this.newRaiseException(this.getEncodingCompatibilityError(), message);
    }
    
    public RaiseException newConverterNotFoundError(final String message) {
        return this.newRaiseException(this.getConverterNotFoundError(), message);
    }
    
    public RaiseException newFiberError(final String message) {
        return this.newRaiseException(this.getFiberError(), message);
    }
    
    public RaiseException newUndefinedConversionError(final String message) {
        return this.newRaiseException(this.getUndefinedConversionError(), message);
    }
    
    public RaiseException newInvalidByteSequenceError(final String message) {
        return this.newRaiseException(this.getInvalidByteSequenceError(), message);
    }
    
    public RaiseException newRaiseException(final RubyClass exceptionClass, final String message) {
        return RaiseException.from(this, exceptionClass, message);
    }
    
    private RaiseException newLightweightErrnoException(final RubyClass exceptionClass, final String message) {
        if (RubyInstanceConfig.ERRNO_BACKTRACE) {
            return RaiseException.from(this, exceptionClass, message);
        }
        return RaiseException.from(this, exceptionClass, "errno backtraces disabled; run with -Xerrno.backtrace=true to enable", this.disabledBacktrace());
    }
    
    public RaiseException newStopIteration(final IRubyObject result, String message) {
        final ThreadContext context = this.getCurrentContext();
        if (message == null) {
            message = "StopIteration backtraces disabled; run with -Xstop_iteration.backtrace=true to enable";
        }
        final RubyException ex = RubyStopIteration.newInstance(context, result, message);
        if (!RubyInstanceConfig.STOPITERATION_BACKTRACE) {
            ex.setBacktrace(this.disabledBacktrace());
        }
        return ex.toThrowable();
    }
    
    @Deprecated
    public RaiseException newLightweightStopIterationError(final String message) {
        return this.newStopIteration(null, message);
    }
    
    private IRubyObject disabledBacktrace() {
        return RubyArray.newEmptyArray(this);
    }
    
    public RubyObject.Data newData(final RubyClass objectClass, final Object sval) {
        return new RubyObject.Data(this, objectClass, sval);
    }
    
    public RubySymbol.SymbolTable getSymbolTable() {
        return this.symbolTable;
    }
    
    public ObjectSpace getObjectSpace() {
        return this.objectSpace;
    }
    
    public InputStream getIn() {
        return this.in;
    }
    
    public PrintStream getOut() {
        return this.out;
    }
    
    public PrintStream getErr() {
        return this.err;
    }
    
    public boolean isGlobalAbortOnExceptionEnabled() {
        return this.globalAbortOnExceptionEnabled;
    }
    
    public void setGlobalAbortOnExceptionEnabled(final boolean enable) {
        this.globalAbortOnExceptionEnabled = enable;
    }
    
    public IRubyObject getReportOnException() {
        return this.reportOnException;
    }
    
    public void setReportOnException(final IRubyObject enable) {
        this.reportOnException = enable;
    }
    
    public boolean isDoNotReverseLookupEnabled() {
        return this.doNotReverseLookupEnabled;
    }
    
    public void setDoNotReverseLookupEnabled(final boolean b) {
        this.doNotReverseLookupEnabled = b;
    }
    
    public void registerInspecting(final Object obj) {
        Map<Object, Object> val = this.inspect.get();
        if (val == null) {
            this.inspect.set(val = new IdentityHashMap<Object, Object>(8));
        }
        val.put(obj, null);
    }
    
    public boolean isInspecting(final Object obj) {
        final Map<Object, Object> val = this.inspect.get();
        return val != null && val.containsKey(obj);
    }
    
    public void unregisterInspecting(final Object obj) {
        final Map<Object, Object> val = this.inspect.get();
        if (val != null) {
            val.remove(obj);
        }
    }
    
    public boolean isObjectSpaceEnabled() {
        return this.objectSpaceEnabled;
    }
    
    public void setObjectSpaceEnabled(final boolean objectSpaceEnabled) {
        this.objectSpaceEnabled = objectSpaceEnabled;
    }
    
    public boolean isSiphashEnabled() {
        return this.siphashEnabled;
    }
    
    public long getStartTime() {
        return this.startTime;
    }
    
    public Profile getProfile() {
        return this.profile;
    }
    
    public String getJRubyHome() {
        return this.config.getJRubyHome();
    }
    
    public void setJRubyHome(final String home) {
        this.config.setJRubyHome(home);
    }
    
    public RubyInstanceConfig getInstanceConfig() {
        return this.config;
    }
    
    @Deprecated
    public boolean is2_0() {
        return true;
    }
    
    @Deprecated
    public long getGlobalState() {
        synchronized (this) {
            return this.globalState;
        }
    }
    
    @Deprecated
    public void incGlobalState() {
        synchronized (this) {
            this.globalState = (this.globalState + 1L & 0xFFFFFFFF8FFFFFFFL);
        }
    }
    
    public static boolean isSecurityRestricted() {
        return Ruby.securityRestricted;
    }
    
    public static void setSecurityRestricted(final boolean restricted) {
        Ruby.securityRestricted = restricted;
    }
    
    public POSIX getPosix() {
        return this.posix;
    }
    
    public void setRecordSeparatorVar(final GlobalVariable recordSeparatorVar) {
        this.recordSeparatorVar = recordSeparatorVar;
    }
    
    public GlobalVariable getRecordSeparatorVar() {
        return this.recordSeparatorVar;
    }
    
    public ExecutorService getExecutor() {
        return this.executor;
    }
    
    public ExecutorService getFiberExecutor() {
        return this.fiberExecutor;
    }
    
    public Map<String, DateTimeZone> getTimezoneCache() {
        return this.timeZoneCache;
    }
    
    @Deprecated
    public int getConstantGeneration() {
        return -1;
    }
    
    public Invalidator getConstantInvalidator(final String constantName) {
        final Invalidator invalidator = this.constantNameInvalidators.get(constantName);
        if (invalidator != null) {
            return invalidator;
        }
        return this.addConstantInvalidator(constantName);
    }
    
    private Invalidator addConstantInvalidator(final String constantName) {
        final Invalidator invalidator = OptoFactory.newConstantInvalidator(this);
        this.constantNameInvalidators.putIfAbsent(constantName, invalidator);
        return this.constantNameInvalidators.get(constantName);
    }
    
    public Invalidator getCheckpointInvalidator() {
        return this.checkpointInvalidator;
    }
    
    public <E extends Enum<E>> void loadConstantSet(final RubyModule module, final Class<E> enumClass) {
        for (final E e : EnumSet.allOf(enumClass)) {
            final Constant c = (Constant)e;
            if (Character.isUpperCase(c.name().charAt(0))) {
                module.setConstant(c.name(), this.newFixnum(c.intValue()));
            }
        }
    }
    
    public void loadConstantSet(final RubyModule module, final String constantSetName) {
        for (final Constant c : ConstantSet.getConstantSet(constantSetName)) {
            if (Character.isUpperCase(c.name().charAt(0))) {
                module.setConstant(c.name(), this.newFixnum(c.intValue()));
            }
        }
    }
    
    public long getNextDynamicMethodSerial() {
        return this.dynamicMethodSerial.getAndIncrement();
    }
    
    public int getNextModuleGeneration() {
        return this.moduleGeneration.incrementAndGet();
    }
    
    public Object getHierarchyLock() {
        return this.hierarchyLock;
    }
    
    public SelectorPool getSelectorPool() {
        return this.selectorPool;
    }
    
    public RuntimeCache getRuntimeCache() {
        return this.runtimeCache;
    }
    
    public List<StrptimeToken> getCachedStrptimePattern(final String pattern) {
        List<StrptimeToken> tokens = this.strptimeFormatCache.get(pattern);
        if (tokens == null) {
            tokens = new StrptimeParser().compilePattern(pattern);
            this.strptimeFormatCache.put(pattern, tokens);
        }
        return tokens;
    }
    
    void addProfiledMethod(final String id, final DynamicMethod method) {
        if (!this.config.isProfiling() || method.isUndefined()) {
            return;
        }
        this.getProfilingService().addProfiledMethod(id, method);
    }
    
    public void incrementExceptionCount() {
        this.exceptionCount.incrementAndGet();
    }
    
    public int getExceptionCount() {
        return this.exceptionCount.get();
    }
    
    public void incrementBacktraceCount() {
        this.backtraceCount.incrementAndGet();
    }
    
    public int getBacktraceCount() {
        return this.backtraceCount.get();
    }
    
    public void incrementWarningCount() {
        this.warningCount.incrementAndGet();
    }
    
    public int getWarningCount() {
        return this.warningCount.get();
    }
    
    public void incrementCallerCount() {
        this.callerCount.incrementAndGet();
    }
    
    public int getCallerCount() {
        return this.callerCount.get();
    }
    
    @Deprecated
    public void reopenFixnum() {
        this.fixnumInvalidator.invalidate();
        this.fixnumReopened = true;
    }
    
    @Deprecated
    public Invalidator getFixnumInvalidator() {
        return this.fixnumInvalidator;
    }
    
    @Deprecated
    public boolean isFixnumReopened() {
        return this.fixnumReopened;
    }
    
    @Deprecated
    public void reopenFloat() {
        this.floatInvalidator.invalidate();
        this.floatReopened = true;
    }
    
    @Deprecated
    public Invalidator getFloatInvalidator() {
        return this.floatInvalidator;
    }
    
    @Deprecated
    public boolean isFloatReopened() {
        return this.floatReopened;
    }
    
    public boolean isBootingCore() {
        return !this.coreIsBooted;
    }
    
    public boolean isBooting() {
        return !this.runtimeIsBooted;
    }
    
    public CoverageData getCoverageData() {
        return this.coverageData;
    }
    
    @Deprecated
    public Random getRandom() {
        return this.random;
    }
    
    public long getHashSeedK0() {
        return this.hashSeedK0;
    }
    
    public long getHashSeedK1() {
        return this.hashSeedK1;
    }
    
    public StaticScopeFactory getStaticScopeFactory() {
        return this.staticScopeFactory;
    }
    
    public FFI getFFI() {
        return this.ffi;
    }
    
    public void setFFI(final FFI ffi) {
        this.ffi = ffi;
    }
    
    public RubyString getDefinedMessage(final DefinedMessage definedMessage) {
        return this.definedMessages.get(definedMessage);
    }
    
    public RubyString getThreadStatus(final RubyThread.Status status) {
        return this.threadStatuses.get(status);
    }
    
    public RubyString freezeAndDedupString(final RubyString string) {
        if (string.getMetaClass() != this.stringClass) {
            final RubyString duped = string.strDup(this);
            duped.setFrozen(true);
            return duped;
        }
        final FStringEqual wrapper = this.DEDUP_WRAPPER_CACHE.get();
        wrapper.string = string;
        WeakReference<RubyString> dedupedRef = this.dedupMap.get(wrapper);
        RubyString deduped;
        if (dedupedRef != null && (deduped = dedupedRef.get()) != null) {
            wrapper.string = null;
            return deduped;
        }
        this.DEDUP_WRAPPER_CACHE.remove();
        deduped = string.strDup(this);
        deduped.setFrozen(true);
        final WeakReference<RubyString> weakref = new WeakReference<RubyString>(deduped);
        wrapper.string = deduped;
        dedupedRef = this.dedupMap.computeIfAbsent(wrapper, key -> weakref);
        if (dedupedRef == null) {
            return deduped;
        }
        RubyString unduped = dedupedRef.get();
        if (unduped != null) {
            return unduped;
        }
        do {
            wrapper.string = string;
            final WeakReference weakReference;
            dedupedRef = this.dedupMap.computeIfPresent(wrapper, (key, old) -> (old.get() == null) ? weakReference : old);
            unduped = dedupedRef.get();
        } while (unduped == null);
        return unduped;
    }
    
    public int getRuntimeNumber() {
        return this.runtimeNumber;
    }
    
    @Override
    public Object constant() {
        return this.constant;
    }
    
    public void setBaseNewMethod(final DynamicMethod baseNewMethod) {
        this.baseNewMethod = baseNewMethod;
    }
    
    public DynamicMethod getBaseNewMethod() {
        return this.baseNewMethod;
    }
    
    public MethodHandle getNullToNilHandle() {
        MethodHandle nullToNil = this.nullToNil;
        if (nullToNil != null) {
            return nullToNil;
        }
        nullToNil = InvokeDynamicSupport.findStatic(Helpers.class, "nullToNil", MethodType.methodType(IRubyObject.class, IRubyObject.class, IRubyObject.class));
        nullToNil = MethodHandles.insertArguments(nullToNil, 1, this.nilObject);
        nullToNil = MethodHandles.explicitCastArguments(nullToNil, MethodType.methodType(IRubyObject.class, Object.class));
        return this.nullToNil = nullToNil;
    }
    
    private void addLoadParseToStats() {
        if (this.parserStats != null) {
            this.parserStats.addLoadParse();
        }
    }
    
    private void addEvalParseToStats() {
        if (this.parserStats != null) {
            this.parserStats.addEvalParse();
        }
    }
    
    public FilenoUtil getFilenoUtil() {
        return this.filenoUtil;
    }
    
    public RubyClass getData() {
        return this.dataClass;
    }
    
    public Map<Class, Consumer<RubyModule>> getJavaExtensionDefinitions() {
        return this.javaExtensionDefinitions;
    }
    
    @Deprecated
    public int getSafeLevel() {
        return 0;
    }
    
    @Deprecated
    public void setSafeLevel(final int safeLevel) {
    }
    
    @Deprecated
    public void checkSafeString(final IRubyObject object) {
    }
    
    @Deprecated
    public void secure(final int level) {
    }
    
    @Deprecated
    public RaiseException newNameErrorObject(final String message, final IRubyObject name) {
        final RubyException error = new RubyNameError(this, this.getNameError(), message, name);
        return error.toThrowable();
    }
    
    @Deprecated
    public boolean is1_8() {
        return false;
    }
    
    @Deprecated
    public boolean is1_9() {
        return true;
    }
    
    @Deprecated
    public IRubyObject safeRecurse(final RecursiveFunction func, final IRubyObject obj, final String name, final boolean outer) {
        return this.safeRecurse(Ruby.LEGACY_RECURSE, this.getCurrentContext(), func, obj, name, outer);
    }
    
    @Deprecated
    public ProfiledMethods getProfiledMethods() {
        return new ProfiledMethods(this);
    }
    
    @Deprecated
    public <T> IRubyObject safeRecurse(final RecursiveFunctionEx<T> func, final ThreadContext context, final T state, final IRubyObject obj, final String name, final boolean outer) {
        return context.safeRecurse(func, state, obj, name, outer);
    }
    
    @Deprecated
    public IRubyObject execRecursive(final RecursiveFunction func, final IRubyObject obj) {
        return this.oldRecursionGuard().execRecursive(func, obj);
    }
    
    @Deprecated
    public IRubyObject execRecursiveOuter(final RecursiveFunction func, final IRubyObject obj) {
        return this.oldRecursionGuard().execRecursiveOuter(func, obj);
    }
    
    @Deprecated
    public <T extends IRubyObject> T recursiveListOperation(final Callable<T> body) {
        return this.oldRecursionGuard().recursiveListOperation(body);
    }
    
    @Deprecated
    private MRIRecursionGuard oldRecursionGuard() {
        MRIRecursionGuard mriRecursionGuard = this.mriRecursionGuard;
        if (mriRecursionGuard != null) {
            return mriRecursionGuard;
        }
        synchronized (this) {
            mriRecursionGuard = this.mriRecursionGuard;
            if (mriRecursionGuard != null) {
                return mriRecursionGuard;
            }
            return mriRecursionGuard = new MRIRecursionGuard(this);
        }
    }
    
    @Deprecated
    public IRubyObject getRootFiber() {
        return this.rootFiber;
    }
    
    @Deprecated
    public void setRootFiber(final IRubyObject fiber) {
        this.rootFiber = fiber;
    }
    
    @Deprecated
    void setKernel(final RubyModule kernelModule) {
    }
    
    @Deprecated
    void setComparable(final RubyModule comparableModule) {
    }
    
    @Deprecated
    void setNumeric(final RubyClass numericClass) {
    }
    
    @Deprecated
    void setFloat(final RubyClass floatClass) {
    }
    
    @Deprecated
    void setInteger(final RubyClass integerClass) {
    }
    
    @Deprecated
    void setFixnum(final RubyClass fixnumClass) {
    }
    
    @Deprecated
    void setComplex(final RubyClass complexClass) {
    }
    
    @Deprecated
    void setRational(final RubyClass rationalClass) {
    }
    
    @Deprecated
    void setEnumerable(final RubyModule enumerableModule) {
    }
    
    @Deprecated
    void setEnumerator(final RubyClass enumeratorClass) {
    }
    
    @Deprecated
    void setYielder(final RubyClass yielderClass) {
    }
    
    @Deprecated
    public void setGenerator(final RubyClass generatorClass) {
    }
    
    @Deprecated
    public void setFiber(final RubyClass fiberClass) {
    }
    
    @Deprecated
    void setString(final RubyClass stringClass) {
    }
    
    @Deprecated
    void setEncoding(final RubyClass encodingClass) {
    }
    
    @Deprecated
    void setConverter(final RubyClass converterClass) {
    }
    
    @Deprecated
    void setSymbol(final RubyClass symbolClass) {
    }
    
    @Deprecated
    void setArray(final RubyClass arrayClass) {
    }
    
    @Deprecated
    void setHash(final RubyClass hashClass) {
    }
    
    @Deprecated
    void setRange(final RubyClass rangeClass) {
    }
    
    @Deprecated
    void setNilClass(final RubyClass nilClass) {
    }
    
    @Deprecated
    void setTrueClass(final RubyClass trueClass) {
    }
    
    @Deprecated
    void setFalseClass(final RubyClass falseClass) {
    }
    
    @Deprecated
    void setProc(final RubyClass procClass) {
    }
    
    @Deprecated
    void setBinding(final RubyClass bindingClass) {
    }
    
    @Deprecated
    void setMethod(final RubyClass methodClass) {
    }
    
    @Deprecated
    void setUnboundMethod(final RubyClass unboundMethodClass) {
    }
    
    @Deprecated
    void setMatchData(final RubyClass matchDataClass) {
    }
    
    @Deprecated
    void setRegexp(final RubyClass regexpClass) {
    }
    
    @Deprecated
    void setTime(final RubyClass timeClass) {
    }
    
    @Deprecated
    void setMath(final RubyModule mathModule) {
    }
    
    @Deprecated
    void setMarshal(final RubyModule marshalModule) {
    }
    
    @Deprecated
    void setBignum(final RubyClass bignumClass) {
    }
    
    @Deprecated
    void setDir(final RubyClass dirClass) {
    }
    
    @Deprecated
    void setFile(final RubyClass fileClass) {
    }
    
    @Deprecated
    void setFileStat(final RubyClass fileStatClass) {
    }
    
    @Deprecated
    void setFileTest(final RubyModule fileTestModule) {
    }
    
    @Deprecated
    void setIO(final RubyClass ioClass) {
    }
    
    @Deprecated
    void setThread(final RubyClass threadClass) {
    }
    
    @Deprecated
    void setThreadGroup(final RubyClass threadGroupClass) {
    }
    
    @Deprecated
    void setContinuation(final RubyClass continuationClass) {
    }
    
    @Deprecated
    void setStructClass(final RubyClass structClass) {
    }
    
    @Deprecated
    void setRandomClass(final RubyClass randomClass) {
    }
    
    @Deprecated
    void setGC(final RubyModule gcModule) {
    }
    
    @Deprecated
    void setObjectSpaceModule(final RubyModule objectSpaceModule) {
    }
    
    @Deprecated
    void setProcess(final RubyModule processModule) {
    }
    
    @Deprecated
    public void setWarning(final RubyModule warningModule) {
    }
    
    @Deprecated
    void setException(final RubyClass exceptionClass) {
    }
    
    public void addToObjectSpace(final boolean useObjectSpace, final IRubyObject object) {
        this.objectSpacer.addToObjectSpace(this, useObjectSpace, object);
    }
    
    @Deprecated
    private void setNetworkStack() {
        this.deprecatedNetworkStackProperty();
    }
    
    private void deprecatedNetworkStackProperty() {
        if (Options.PREFER_IPV4.load()) {
            Ruby.LOG.warn("Warning: not setting network stack system property because socket subsystem may already be booted.If you need this option please set it manually as a JVM property.\nUse JAVA_OPTS=-Djava.net.preferIPv4Stack=true OR prepend -J as a JRuby option.", new Object[0]);
        }
    }
    
    @Deprecated
    public RaiseException newErrnoEADDRFromBindException(final BindException be) {
        return this.newErrnoEADDRFromBindException(be, null);
    }
    
    static {
        LOG = LoggerFactory.getLogger(Ruby.class);
        if (RubyInstanceConfig.JIT_LOADING_DEBUG) {
            Ruby.LOG.setDebugEnable(true);
        }
        interest = EnumSet.of(RubyEvent.C_CALL, RubyEvent.C_RETURN, RubyEvent.CALL, RubyEvent.CLASS, RubyEvent.END, RubyEvent.LINE, RubyEvent.RAISE, RubyEvent.RETURN);
        ADDR_NOT_AVAIL_PATTERN = Pattern.compile("assign.*address");
        LEGACY_RECURSE = new RecursiveFunctionEx<RecursiveFunction>() {
            @Override
            public IRubyObject call(final ThreadContext context, final RecursiveFunction func, final IRubyObject obj, final boolean recur) {
                return func.call(obj, recur);
            }
        };
        EMPTY_HOOKS = new EventHook[0];
        Ruby.securityRestricted = false;
        if (SafePropertyAccessor.isSecurityProtected("jruby.reflected.handles")) {
            Ruby.securityRestricted = true;
        }
        else {
            final SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                try {
                    sm.checkCreateClassLoader();
                }
                catch (SecurityException se) {
                    Ruby.securityRestricted = true;
                }
            }
        }
        threadLocalRuntime = new ThreadLocal<Ruby>();
        DISABLED_OBJECTSPACE = new ObjectSpacer() {
            @Override
            public void addToObjectSpace(final Ruby runtime, final boolean useObjectSpace, final IRubyObject object) {
            }
        };
        ENABLED_OBJECTSPACE = new ObjectSpacer() {
            @Override
            public void addToObjectSpace(final Ruby runtime, final boolean useObjectSpace, final IRubyObject object) {
                if (useObjectSpace) {
                    runtime.objectSpace.add(object);
                }
            }
        };
        RUNTIME_NUMBER = new AtomicInteger(0);
    }
    
    public static class CallTraceFuncHook extends EventHook
    {
        private RubyProc traceFunc;
        
        public void setTraceFunc(final RubyProc traceFunc) {
            this.traceFunc = traceFunc;
        }
        
        @Override
        public void eventHandler(final ThreadContext context, String eventName, String file, final int line, final String name, IRubyObject type) {
            if (!context.isWithinTrace()) {
                if (file == null) {
                    file = "(ruby)";
                }
                if (type == null) {
                    type = context.nil;
                }
                final Ruby runtime = context.runtime;
                final RubyBinding binding = RubyBinding.newBinding(runtime, context.currentBinding());
                final String s = eventName;
                switch (s) {
                    case "c_return": {
                        eventName = "c-return";
                        break;
                    }
                    case "c_call": {
                        eventName = "c-call";
                        break;
                    }
                }
                context.preTrace();
                try {
                    this.traceFunc.call(context, runtime.newString(eventName), runtime.newString(file), runtime.newFixnum(line), (name != null) ? runtime.newSymbol(name) : runtime.getNil(), binding, type);
                }
                finally {
                    context.postTrace();
                }
            }
        }
        
        @Override
        public boolean isInterestedInEvent(final RubyEvent event) {
            return Ruby.interest.contains(event);
        }
        
        @Override
        public EnumSet<RubyEvent> eventSet() {
            return Ruby.interest;
        }
    }
    
    static class FStringEqual
    {
        RubyString string;
        
        @Override
        public boolean equals(final Object other) {
            if (other instanceof FStringEqual) {
                final RubyString otherString = ((FStringEqual)other).string;
                final RubyString string = this.string;
                return string != null && otherString != null && string.equals(otherString) && string.getEncoding() == otherString.getEncoding();
            }
            return false;
        }
        
        @Override
        public int hashCode() {
            final RubyString string = this.string;
            if (string == null) {
                return 0;
            }
            return string.hashCode();
        }
    }
    
    public interface ObjectSpacer
    {
        void addToObjectSpace(final Ruby p0, final boolean p1, final IRubyObject p2);
    }
    
    @Deprecated
    public interface RecursiveFunction extends MRIRecursionGuard.RecursiveFunction
    {
    }
    
    public interface RecursiveFunctionEx<T> extends ThreadContext.RecursiveFunctionEx<T>
    {
        IRubyObject call(final ThreadContext p0, final T p1, final IRubyObject p2, final boolean p3);
    }
}
