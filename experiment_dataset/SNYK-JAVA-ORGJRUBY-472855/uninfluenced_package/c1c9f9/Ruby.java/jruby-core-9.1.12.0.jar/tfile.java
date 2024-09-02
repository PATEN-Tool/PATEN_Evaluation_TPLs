// 
// Decompiled by Procyon v0.5.36
// 

package org.jruby;

import org.jruby.util.SafePropertyAccessor;
import org.jruby.util.log.LoggerFactory;
import java.util.concurrent.Callable;
import org.jruby.runtime.profile.builtin.ProfiledMethods;
import java.lang.invoke.MethodHandles;
import org.jruby.runtime.invokedynamic.InvokeDynamicSupport;
import java.lang.invoke.MethodType;
import java.security.AccessControlException;
import jnr.constants.ConstantSet;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.nio.channels.ClosedChannelException;
import java.net.BindException;
import org.jruby.platform.Platform;
import org.jruby.runtime.Binding;
import org.jruby.runtime.profile.ProfilingService;
import org.jruby.runtime.profile.ProfileCollection;
import java.util.Collection;
import org.jruby.runtime.RubyEvent;
import java.util.Arrays;
import org.jruby.runtime.load.BasicLibraryService;
import org.jruby.parser.StaticScope;
import java.io.FileDescriptor;
import org.jruby.util.IOInputStream;
import org.jruby.util.IOOutputStream;
import java.nio.charset.Charset;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.parser.ParserConfiguration;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRReader;
import org.jruby.ir.persistence.IRReaderStream;
import org.jruby.ir.persistence.util.IRFileExpert;
import org.jruby.util.SelfFirstJRubyClassLoader;
import org.jruby.ext.LateLoadingLibrary;
import org.jruby.runtime.load.Library;
import jnr.constants.Constant;
import jnr.constants.platform.Errno;
import org.jruby.ext.fiber.ThreadFiberLibrary;
import org.jruby.ext.thread.ThreadLibrary;
import org.jruby.ext.tracepoint.TracePoint;
import org.jruby.internal.runtime.methods.JavaMethod;
import org.jruby.runtime.Visibility;
import org.jruby.util.ByteList;
import org.jruby.javasupport.JavaSupportImpl;
import org.jruby.ext.fiber.ThreadFiber;
import org.jruby.util.SecurityHelper;
import java.util.List;
import org.jruby.ir.instructions.Instr;
import java.util.ArrayList;
import org.jruby.ir.IRScriptBody;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import org.jruby.threading.DaemonThreadFactory;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import org.jruby.common.IRubyWarnings;
import org.jruby.runtime.CallSite;
import org.jruby.embed.Extension;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.util.func.Function1;
import org.jruby.runtime.Block;
import org.jruby.compiler.NotCompilableException;
import org.jruby.ir.Compiler;
import org.jruby.exceptions.RaiseException;
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
import org.jruby.ast.BlockNode;
import org.jruby.ast.executable.Script;
import org.jruby.runtime.Helpers;
import org.jruby.util.cli.Options;
import org.jruby.ir.IRScope;
import java.util.Iterator;
import org.jruby.runtime.IAccessor;
import org.jruby.exceptions.MainExitException;
import java.io.File;
import org.jruby.runtime.load.CompiledScriptLoader;
import java.io.IOException;
import org.jruby.internal.runtime.ValueAccessor;
import java.io.ByteArrayInputStream;
import org.jruby.ast.RootNode;
import org.jruby.ast.Node;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.scope.ManyVarsDynamicScope;
import org.jruby.management.ParserStatsMBean;
import org.jruby.management.ConfigMBean;
import org.jruby.compiler.JITCompilerMBean;
import jnr.posix.POSIXHandler;
import jnr.posix.POSIXFactory;
import org.jruby.ext.JRubyPOSIXHandler;
import org.jruby.runtime.invokedynamic.MethodNames;
import org.jruby.runtime.ClassIndex;
import java.security.SecureRandom;
import org.jruby.management.BeanManagerFactory;
import java.util.Collections;
import java.util.WeakHashMap;
import org.jruby.runtime.opto.OptoFactory;
import org.jruby.util.collections.WeakHashSet;
import java.util.HashMap;
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
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.jruby.util.KCode;
import java.util.Stack;
import org.jruby.common.RubyWarnings;
import org.jruby.internal.runtime.GlobalVariables;
import org.jruby.runtime.encoding.EncodingService;
import org.jcodings.Encoding;
import org.jruby.runtime.load.LoadService;
import org.jruby.parser.Parser;
import org.jruby.compiler.JITCompiler;
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
import java.util.Map;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.log.Logger;
import org.jruby.compiler.Constantizable;

public final class Ruby implements Constantizable
{
    private static final Logger LOG;
    public static final int NIL_PREFILLED_ARRAY_SIZE = 128;
    private final IRubyObject[] nilPrefilledArray;
    private Map<Integer, RubyClass> errnos;
    private RubyRandom.RandomType defaultRand;
    private RubyHash charsetMap;
    private final CallTraceFuncHook callTraceFuncHook;
    private static final Pattern ADDR_NOT_AVAIL_PATTERN;
    private ThreadLocal<Map<Object, Object>> inspect;
    @Deprecated
    private static final RecursiveFunctionEx<RecursiveFunction> LEGACY_RECURSE;
    private final ConcurrentHashMap<String, Invalidator> constantNameInvalidators;
    private final Invalidator checkpointInvalidator;
    private final ThreadService threadService;
    private final POSIX posix;
    private final ObjectSpace objectSpace;
    private final RubySymbol.SymbolTable symbolTable;
    private static final EventHook[] EMPTY_HOOKS;
    private volatile EventHook[] eventHooks;
    private boolean hasEventHooks;
    private boolean globalAbortOnExceptionEnabled;
    private boolean doNotReverseLookupEnabled;
    private volatile boolean objectSpaceEnabled;
    private boolean siphashEnabled;
    private long globalState;
    private IRubyObject topSelf;
    private IRubyObject rootFiber;
    private RubyNil nilObject;
    private IRubyObject[] singleNilArray;
    private RubyBoolean trueObject;
    private RubyBoolean falseObject;
    final RubyFixnum[] fixnumCache;
    final Object[] fixnumConstants;
    private boolean verbose;
    private boolean warningsEnabled;
    private boolean debug;
    private IRubyObject verboseValue;
    private RubyThreadGroup defaultThreadGroup;
    private RubyClass basicObjectClass;
    private RubyClass objectClass;
    private RubyClass moduleClass;
    private RubyClass classClass;
    private RubyClass nilClass;
    private RubyClass trueClass;
    private RubyClass falseClass;
    private RubyClass numericClass;
    private RubyClass floatClass;
    private RubyClass integerClass;
    private RubyClass fixnumClass;
    private RubyClass complexClass;
    private RubyClass rationalClass;
    private RubyClass enumeratorClass;
    private RubyClass yielderClass;
    private RubyClass fiberClass;
    private RubyClass generatorClass;
    private RubyClass arrayClass;
    private RubyClass hashClass;
    private RubyClass rangeClass;
    private RubyClass stringClass;
    private RubyClass encodingClass;
    private RubyClass converterClass;
    private RubyClass symbolClass;
    private RubyClass procClass;
    private RubyClass bindingClass;
    private RubyClass methodClass;
    private RubyClass unboundMethodClass;
    private RubyClass matchDataClass;
    private RubyClass regexpClass;
    private RubyClass timeClass;
    private RubyClass bignumClass;
    private RubyClass dirClass;
    private RubyClass fileClass;
    private RubyClass fileStatClass;
    private RubyClass ioClass;
    private RubyClass threadClass;
    private RubyClass threadGroupClass;
    private RubyClass continuationClass;
    private RubyClass structClass;
    private RubyClass tmsStruct;
    private RubyClass passwdStruct;
    private RubyClass groupStruct;
    private RubyClass procStatusClass;
    private RubyClass exceptionClass;
    private RubyClass runtimeError;
    private RubyClass ioError;
    private RubyClass scriptError;
    private RubyClass nameError;
    private RubyClass nameErrorMessage;
    private RubyClass noMethodError;
    private RubyClass signalException;
    private RubyClass rangeError;
    private RubyClass dummyClass;
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
    private RubyClass randomClass;
    private RubyClass keyError;
    private RubyClass locationClass;
    private RubyClass interruptedRegexpError;
    private RubyModule kernelModule;
    private RubyModule comparableModule;
    private RubyModule enumerableModule;
    private RubyModule mathModule;
    private RubyModule marshalModule;
    private RubyModule etcModule;
    private RubyModule fileTestModule;
    private RubyModule gcModule;
    private RubyModule objectSpaceModule;
    private RubyModule processModule;
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
    private final RubyInstanceConfig config;
    private InputStream in;
    private PrintStream out;
    private PrintStream err;
    private JavaSupport javaSupport;
    private JRubyClassLoader jrubyClassLoader;
    private BeanManager beanManager;
    private ParserStats parserStats;
    private final JITCompiler jitCompiler;
    private static volatile boolean securityRestricted;
    private final Parser parser;
    private LoadService loadService;
    private Encoding defaultInternalEncoding;
    private Encoding defaultExternalEncoding;
    private EncodingService encodingService;
    private GlobalVariables globalVariables;
    private final RubyWarnings warnings;
    private final Stack<RubyProc> atExitBlocks;
    private Profile profile;
    private KCode kcode;
    private final AtomicInteger symbolLastId;
    private final AtomicInteger moduleLastId;
    private final Set<RubyModule> allModules;
    private final Map<String, DateTimeZone> timeZoneCache;
    private Map<Finalizable, Object> finalizers;
    private Map<Finalizable, Object> internalFinalizers;
    private final Object finalizersMutex;
    private final Object internalFinalizersMutex;
    private ExecutorService executor;
    private ExecutorService fiberExecutor;
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
    private Invalidator fixnumInvalidator;
    private Invalidator floatInvalidator;
    private boolean fixnumReopened;
    private boolean floatReopened;
    private volatile boolean bootingCore;
    private volatile boolean bootingRuntime;
    private RubyHash envObject;
    private final CoverageData coverageData;
    private static volatile Ruby globalRuntime;
    private static ThreadLocal<Ruby> threadLocalRuntime;
    private final Random random;
    private final long hashSeedK0;
    private final long hashSeedK1;
    private StaticScopeFactory staticScopeFactory;
    private IRManager irManager;
    private FFI ffi;
    private JavaProxyClassFactory javaProxyClassFactory;
    private final ProfilingServiceLookup profilingServiceLookup;
    private EnumMap<DefinedMessage, RubyString> definedMessages;
    private EnumMap<RubyThread.Status, RubyString> threadStatuses;
    private static final ObjectSpacer DISABLED_OBJECTSPACE;
    private static final ObjectSpacer ENABLED_OBJECTSPACE;
    private final ObjectSpacer objectSpacer;
    private RubyArray emptyFrozenArray;
    private Map<RubyString, WeakReference<RubyString>> dedupMap;
    private static final AtomicInteger RUNTIME_NUMBER;
    private final int runtimeNumber;
    private final Config configBean;
    private final Runtime runtimeBean;
    private final FilenoUtil filenoUtil;
    private Interpreter interpreter;
    private final Object constant;
    private DynamicMethod baseNewMethod;
    private MethodHandle nullToNil;
    public final ClassValue<TypePopulator> POPULATORS;
    public final JavaSites sites;
    private volatile MRIRecursionGuard mriRecursionGuard;
    
    private Ruby(final RubyInstanceConfig config) {
        this.nilPrefilledArray = new IRubyObject[128];
        this.errnos = new HashMap<Integer, RubyClass>();
        this.callTraceFuncHook = new CallTraceFuncHook();
        this.inspect = new ThreadLocal<Map<Object, Object>>();
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
        this.atExitBlocks = new Stack<RubyProc>();
        this.kcode = KCode.NONE;
        this.symbolLastId = new AtomicInteger(128);
        this.moduleLastId = new AtomicInteger(0);
        this.allModules = new WeakHashSet<RubyModule>();
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
        this.bootingCore = true;
        this.bootingRuntime = true;
        this.coverageData = new CoverageData();
        this.definedMessages = new EnumMap<DefinedMessage, RubyString>(DefinedMessage.class);
        this.threadStatuses = new EnumMap<RubyThread.Status, RubyString>(RubyThread.Status.class);
        this.dedupMap = Collections.synchronizedMap(new WeakHashMap<RubyString, WeakReference<RubyString>>());
        this.runtimeNumber = Ruby.RUNTIME_NUMBER.getAndIncrement();
        this.interpreter = new Interpreter();
        this.POPULATORS = new ClassValue<TypePopulator>() {
            @Override
            protected TypePopulator computeValue(final Class<?> type) {
                return RubyModule.loadPopulatorFor(type);
            }
        };
        this.sites = new JavaSites();
        this.config = config;
        this.threadService = new ThreadService(this);
        if (config.isProfiling()) {
            this.profilingServiceLookup = new ProfilingServiceLookup(this);
        }
        else {
            this.profilingServiceLookup = null;
        }
        this.constant = OptoFactory.newConstantWrapper(Ruby.class, this);
        this.getJRubyClassLoader();
        this.staticScopeFactory = new StaticScopeFactory(this);
        this.beanManager = BeanManagerFactory.create(this, config.isManagementEnabled());
        this.jitCompiler = new JITCompiler(this);
        this.parserStats = new ParserStats(this);
        Random myRandom;
        try {
            myRandom = new SecureRandom();
        }
        catch (Throwable t) {
            Ruby.LOG.debug("unable to instantiate SecureRandom, falling back on Random", t);
            myRandom = new Random();
        }
        this.random = myRandom;
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
        this.checkpointInvalidator = OptoFactory.newConstantInvalidator();
        if (config.isObjectSpaceEnabled()) {
            this.objectSpacer = Ruby.ENABLED_OBJECTSPACE;
        }
        else {
            this.objectSpacer = Ruby.DISABLED_OBJECTSPACE;
        }
        this.posix = POSIXFactory.getPOSIX((POSIXHandler)new JRubyPOSIXHandler(this), config.isNativeEnabled());
        this.filenoUtil = new FilenoUtil(this.posix);
        this.reinitialize(false);
    }
    
    public void registerMBeans() {
        this.beanManager.register(this.jitCompiler);
        this.beanManager.register(this.configBean);
        this.beanManager.register(this.parserStats);
        this.beanManager.register(this.runtimeBean);
    }
    
    void reinitialize(final boolean reinitCore) {
        this.doNotReverseLookupEnabled = true;
        this.staticScopeFactory = new StaticScopeFactory(this);
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
        ruby.init();
        setGlobalRuntimeFirstTimeOnly(ruby);
        return ruby;
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
        final Node rootNode = this.parseEval(script, "<script>", scope, 0);
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
        final RootNode root = (RootNode)this.parseInline(new ByteArrayInputStream(bytes), filename, null);
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
            return this.parseInline(in, fileName, this.getCurrentContext().getCurrentScope());
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
                    Ruby.LOG.info("Successfully compiled: {}", scriptNode.getFile());
                }
            }
            catch (Throwable e) {
                if (Options.JIT_LOGGING.load()) {
                    if (Options.JIT_LOGGING_VERBOSE.load()) {
                        Ruby.LOG.error("Failed to compile: " + scriptNode.getFile(), e);
                    }
                    else {
                        Ruby.LOG.error("Failed to compile: " + scriptNode.getFile(), new Object[0]);
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
        newBody.add(new GlobalAsgnNode(pos, "$/", new StrNode(pos, ((RubyString)this.globalVariables.get("$/")).getByteList())));
        if (processLineEndings) {
            newBody.add(new GlobalAsgnNode(pos, "$\\", new GlobalVarNode(pos, "$/")));
        }
        final GlobalVarNode dollarUnderscore = new GlobalVarNode(pos, "$_");
        final BlockNode whileBody = new BlockNode(pos);
        newBody.add(new WhileNode(pos, new VCallNode(pos, "gets"), whileBody));
        if (processLineEndings) {
            whileBody.add(new CallNode(pos, dollarUnderscore, "chop!", null, null));
        }
        if (split) {
            whileBody.add(new GlobalAsgnNode(pos, "$F", new CallNode(pos, dollarUnderscore, "split", null, null)));
        }
        if (oldRoot.getBodyNode() instanceof BlockNode) {
            whileBody.addAll((ListNode)oldRoot.getBodyNode());
        }
        else {
            whileBody.add(oldRoot.getBodyNode());
        }
        if (printing) {
            whileBody.add(new FCallNode(pos, "puts", new ArrayNode(pos, dollarUnderscore), null));
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
                Ruby.LOG.info("done compiling target script: " + scriptNode.getFile(), new Object[0]);
            }
        }
        catch (Exception e) {
            if (Options.JIT_LOGGING.load()) {
                Ruby.LOG.error("failed to compile target script '" + scriptNode.getFile() + "'", new Object[0]);
                if (Options.JIT_LOGGING_VERBOSE.load()) {
                    e.printStackTrace();
                }
            }
        }
        return scriptAndCode;
    }
    
    public Script tryCompile(final Node node) {
        return this.tryCompile((RootNode)node, new ClassDefiningJRubyClassLoader(this.getJRubyClassLoader())).script();
    }
    
    private void failForcedCompile(final RootNode scriptNode) throws RaiseException {
        if (this.config.getCompileMode().shouldPrecompileAll()) {
            throw this.newRuntimeError("could not compile and compile mode is 'force': " + scriptNode.getFile());
        }
    }
    
    private ScriptAndCode tryCompile(final RootNode root, final ClassDefiningClassLoader classLoader) {
        try {
            return Compiler.getInstance().execute(this, root, classLoader);
        }
        catch (NotCompilableException e) {
            if (Options.JIT_LOGGING.load()) {
                if (Options.JIT_LOGGING_VERBOSE.load()) {
                    Ruby.LOG.error("failed to compile target script " + root.getFile() + ": ", e);
                }
                else {
                    Ruby.LOG.error("failed to compile target script " + root.getFile() + ": " + e.getLocalizedMessage(), new Object[0]);
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
        return this.interpreter.execute(this, parseResult, self);
    }
    
    public IRubyObject runInterpreter(final ThreadContext context, final Node rootNode, final IRubyObject self) {
        assert rootNode != null : "scriptNode is not null";
        return this.interpreter.execute(this, rootNode, self);
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
        synchronized (this.allModules) {
            this.allModules.add(module);
        }
    }
    
    public void eachModule(final Function1<Object, IRubyObject> func) {
        synchronized (this.allModules) {
            for (final RubyModule module : this.allModules) {
                func.apply(module);
            }
        }
    }
    
    public RubyModule getModule(final String name) {
        return (RubyModule)this.objectClass.getConstantAt(name);
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
    
    public RubyClass defineClassUnder(final String name, RubyClass superClass, final ObjectAllocator allocator, final RubyModule parent, final CallSite[] callSites) {
        final IRubyObject classObj = parent.getConstantAt(name);
        if (classObj == null) {
            final boolean parentIsObject = parent == this.objectClass;
            if (superClass == null) {
                final String className = parentIsObject ? name : (parent.getName() + "::" + name);
                this.warnings.warn(IRubyWarnings.ID.NO_SUPER_CLASS, "no super class for `" + className + "', Object assumed");
                superClass = this.objectClass;
            }
            return RubyClass.newClass(this, superClass, name, allocator, parent, !parentIsObject, callSites);
        }
        if (!(classObj instanceof RubyClass)) {
            throw this.newTypeError(name + " is not a class");
        }
        final RubyClass klazz = (RubyClass)classObj;
        if (klazz.getSuperClass().getRealClass() != superClass) {
            throw this.newNameError(name + " is already defined", name);
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
        if (parentIsObject) {
            throw this.newTypeError(moduleObj.getMetaClass().getName() + " is not a module");
        }
        throw this.newTypeError(parent.getName() + "::" + moduleObj.getMetaClass().getName() + " is not a module");
    }
    
    public RubyModule getOrCreateModule(final String name) {
        IRubyObject module = this.objectClass.getConstantAt(name);
        if (module == null) {
            module = this.defineModule(name);
        }
        else if (!module.isModule()) {
            throw this.newTypeError(name + " is not a Module");
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
    
    private void init() {
        this.loadService = this.config.createLoadService(this);
        this.javaSupport = this.loadJavaSupport();
        this.executor = new ThreadPoolExecutor(RubyInstanceConfig.POOL_MIN, RubyInstanceConfig.POOL_MAX, RubyInstanceConfig.POOL_TTL, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new DaemonThreadFactory("Ruby-" + this.getRuntimeNumber() + "-Worker"));
        this.fiberExecutor = new ThreadPoolExecutor(0, Integer.MAX_VALUE, RubyInstanceConfig.FIBER_POOL_TTL, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new DaemonThreadFactory("Ruby-" + this.getRuntimeNumber() + "-Fiber"));
        this.initRoot();
        this.threadService.initMainThread();
        final ThreadContext context = this.getCurrentContext();
        context.prepareTopLevel(this.objectClass, this.topSelf);
        this.bootstrap();
        this.initDefinedMessages();
        this.initThreadStatuses();
        this.irManager = new IRManager(this.getInstanceConfig());
        final IRScope top = new IRScriptBody(this.irManager, "", context.getCurrentScope().getStaticScope());
        top.allocateInterpreterContext(new ArrayList<Instr>());
        (this.dummyClass = new RubyClass(this, this.classClass)).freeze(context);
        RubyGlobal.createGlobals(context, this);
        this.getLoadService().init(this.config.getLoadPaths());
        this.initBuiltins();
        final boolean reflectionWorks = this.doesReflectionWork();
        if (!RubyInstanceConfig.DEBUG_PARSER && reflectionWorks) {
            this.loadService.require("jruby");
        }
        SecurityHelper.checkCryptoRestrictions(this);
        this.bootingCore = false;
        this.initRubyKernel();
        if (!this.config.isDisableGems()) {
            this.defineModule("Gem");
        }
        if (!this.config.isDisableDidYouMean()) {
            this.defineModule("DidYouMean");
        }
        this.initRubyPreludes();
        ThreadFiber.initRootFiber(context);
        if (this.config.isProfiling()) {
            this.getLoadService().require("jruby/profiler/shutdown_hook");
            this.kernelModule.invalidateCacheDescendants();
            RubyKernel.recacheBuiltinMethods(this);
            RubyBasicObject.recacheBuiltinMethods(this);
        }
        if (this.config.getLoadGemfile()) {
            this.loadBundler();
        }
        this.setNetworkStack();
        this.bootingRuntime = false;
        for (final String scriptName : this.config.getRequiredLibraries()) {
            this.topSelf.callMethod(context, "require", RubyString.newString(this, scriptName));
        }
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
    
    private void bootstrap() {
        this.initCore();
        this.initExceptions();
        this.initLibraries();
    }
    
    private void initDefinedMessages() {
        for (final DefinedMessage definedMessage : DefinedMessage.values()) {
            final RubyString str = this.freezeAndDedupString(RubyString.newString(this, ByteList.create((CharSequence)definedMessage.getText())));
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
    
    private void initRoot() {
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
        metaClass = this.classClass.makeMetaClass(metaClass);
        RubyBasicObject.createBasicObjectClass(this, this.basicObjectClass);
        RubyObject.createObjectClass(this, this.objectClass);
        RubyModule.createModuleClass(this, this.moduleClass);
        RubyClass.createClassClass(this, this.classClass);
        this.basicObjectClass.setConstant("BasicObject", this.basicObjectClass);
        this.objectClass.setConstant("Object", this.objectClass);
        this.objectClass.setConstant("Class", this.classClass);
        this.objectClass.setConstant("Module", this.moduleClass);
        final RubyModule kernel = RubyKernel.createKernelModule(this);
        this.objectClass.includeModule(this.kernelModule);
        if (this.config.getKernelGsubDefined()) {
            kernel.addMethod("gsub", new JavaMethod(kernel, Visibility.PRIVATE) {
                @Override
                public IRubyObject call(final ThreadContext context, final IRubyObject self, final RubyModule clazz, final String name, final IRubyObject[] args, final Block block) {
                    switch (args.length) {
                        case 1: {
                            return RubyKernel.gsub(context, self, args[0], block);
                        }
                        case 2: {
                            return RubyKernel.gsub(context, self, args[0], args[1], block);
                        }
                        default: {
                            throw Ruby.this.newArgumentError(String.format("wrong number of arguments %d for 1..2", args.length));
                        }
                    }
                }
            });
        }
        this.topSelf = TopSelfFactory.createTopSelf(this, false);
        RubyNil.createNilClass(this);
        RubyBoolean.createFalseClass(this);
        RubyBoolean.createTrueClass(this);
        this.nilObject = new RubyNil(this);
        for (int i = 0; i < 128; ++i) {
            this.nilPrefilledArray[i] = this.nilObject;
        }
        this.singleNilArray = new IRubyObject[] { this.nilObject };
        (this.falseObject = new RubyBoolean.False(this)).setFrozen(true);
        (this.trueObject = new RubyBoolean.True(this)).setFrozen(true);
    }
    
    private void initCore() {
        if (this.profile.allowClass("Data")) {
            this.defineClass("Data", this.objectClass, ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        }
        RubyComparable.createComparable(this);
        RubyEnumerable.createEnumerableModule(this);
        RubyString.createStringClass(this);
        this.encodingService = new EncodingService(this);
        RubySymbol.createSymbolClass(this);
        if (this.profile.allowClass("ThreadGroup")) {
            RubyThreadGroup.createThreadGroupClass(this);
        }
        if (this.profile.allowClass("Thread")) {
            RubyThread.createThreadClass(this);
        }
        if (this.profile.allowClass("Exception")) {
            RubyException.createExceptionClass(this);
        }
        if (this.profile.allowClass("Numeric")) {
            RubyNumeric.createNumericClass(this);
        }
        if (this.profile.allowClass("Integer")) {
            RubyInteger.createIntegerClass(this);
        }
        if (this.profile.allowClass("Fixnum")) {
            RubyFixnum.createFixnumClass(this);
        }
        RubyEncoding.createEncodingClass(this);
        RubyConverter.createConverterClass(this);
        this.encodingService.defineEncodings();
        this.encodingService.defineAliases();
        String encoding = this.config.getExternalEncoding();
        if (encoding != null && !encoding.equals("")) {
            final Encoding loadedEncoding = this.encodingService.loadEncoding(ByteList.create((CharSequence)encoding));
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
        encoding = this.config.getInternalEncoding();
        if (encoding != null && !encoding.equals("")) {
            final Encoding loadedEncoding = this.encodingService.loadEncoding(ByteList.create((CharSequence)encoding));
            if (loadedEncoding == null) {
                throw new MainExitException(1, "unknown encoding name - " + encoding);
            }
            this.setDefaultInternalEncoding(loadedEncoding);
        }
        if (this.profile.allowClass("Complex")) {
            RubyComplex.createComplexClass(this);
        }
        if (this.profile.allowClass("Rational")) {
            RubyRational.createRationalClass(this);
        }
        if (this.profile.allowClass("Hash")) {
            RubyHash.createHashClass(this);
        }
        if (this.profile.allowClass("Array")) {
            RubyArray.createArrayClass(this);
            (this.emptyFrozenArray = this.newEmptyArray()).setFrozen(true);
        }
        if (this.profile.allowClass("Float")) {
            RubyFloat.createFloatClass(this);
        }
        if (this.profile.allowClass("Bignum")) {
            RubyBignum.createBignumClass(this);
            RubyRandom.createRandomClass(this);
        }
        this.ioClass = RubyIO.createIOClass(this);
        if (this.profile.allowClass("Struct")) {
            RubyStruct.createStructClass(this);
        }
        if (this.profile.allowClass("Binding")) {
            RubyBinding.createBindingClass(this);
        }
        if (this.profile.allowModule("Math")) {
            RubyMath.createMathModule(this);
        }
        if (this.profile.allowClass("Regexp")) {
            RubyRegexp.createRegexpClass(this);
        }
        if (this.profile.allowClass("Range")) {
            RubyRange.createRangeClass(this);
        }
        if (this.profile.allowModule("ObjectSpace")) {
            RubyObjectSpace.createObjectSpaceModule(this);
        }
        if (this.profile.allowModule("GC")) {
            RubyGC.createGCModule(this);
        }
        if (this.profile.allowClass("Proc")) {
            RubyProc.createProcClass(this);
        }
        if (this.profile.allowClass("Method")) {
            RubyMethod.createMethodClass(this);
        }
        if (this.profile.allowClass("MatchData")) {
            RubyMatchData.createMatchDataClass(this);
        }
        if (this.profile.allowModule("Marshal")) {
            RubyMarshal.createMarshalModule(this);
        }
        if (this.profile.allowClass("Dir")) {
            RubyDir.createDirClass(this);
        }
        if (this.profile.allowModule("FileTest")) {
            RubyFileTest.createFileTestModule(this);
        }
        if (this.profile.allowClass("File")) {
            RubyFile.createFileClass(this);
        }
        if (this.profile.allowClass("File::Stat")) {
            RubyFileStat.createFileStatClass(this);
        }
        if (this.profile.allowModule("Process")) {
            RubyProcess.createProcessModule(this);
        }
        if (this.profile.allowClass("Time")) {
            RubyTime.createTimeClass(this);
        }
        if (this.profile.allowClass("UnboundMethod")) {
            RubyUnboundMethod.defineUnboundMethodClass(this);
        }
        if (this.profile.allowModule("Signal")) {
            RubySignal.createSignal(this);
        }
        if (this.profile.allowClass("Continuation")) {
            RubyContinuation.createContinuation(this);
        }
        if (this.profile.allowClass("Enumerator")) {
            RubyEnumerator.defineEnumerator(this);
        }
        TracePoint.createTracePointClass(this);
    }
    
    public IRubyObject[] getNilPrefilledArray() {
        return this.nilPrefilledArray;
    }
    
    private void initExceptions() {
        this.standardError = this.defineClassIfAllowed("StandardError", this.exceptionClass);
        this.runtimeError = this.defineClassIfAllowed("RuntimeError", this.standardError);
        this.ioError = this.defineClassIfAllowed("IOError", this.standardError);
        this.scriptError = this.defineClassIfAllowed("ScriptError", this.exceptionClass);
        this.rangeError = this.defineClassIfAllowed("RangeError", this.standardError);
        if (this.profile.allowClass("SignalException")) {
            this.signalException = RubySignalException.createSignalExceptionClass(this, this.exceptionClass);
        }
        if (this.profile.allowClass("NameError")) {
            this.nameError = RubyNameError.createNameErrorClass(this, this.standardError);
            this.nameErrorMessage = RubyNameError.createNameErrorMessageClass(this, this.nameError);
        }
        if (this.profile.allowClass("NoMethodError")) {
            this.noMethodError = RubyNoMethodError.createNoMethodErrorClass(this, this.nameError);
        }
        if (this.profile.allowClass("SystemExit")) {
            this.systemExit = RubySystemExit.createSystemExitClass(this, this.exceptionClass);
        }
        if (this.profile.allowClass("LocalJumpError")) {
            this.localJumpError = RubyLocalJumpError.createLocalJumpErrorClass(this, this.standardError);
        }
        if (this.profile.allowClass("NativeException")) {
            this.nativeException = NativeException.createClass(this, this.runtimeError);
        }
        if (this.profile.allowClass("SystemCallError")) {
            this.systemCallError = RubySystemCallError.createSystemCallErrorClass(this, this.standardError);
        }
        this.fatal = this.defineClassIfAllowed("Fatal", this.exceptionClass);
        if (this.profile.allowClass("Interrupt")) {
            this.interrupt = RubyInterrupt.createInterruptClass(this, this.signalException);
        }
        this.typeError = this.defineClassIfAllowed("TypeError", this.standardError);
        this.argumentError = this.defineClassIfAllowed("ArgumentError", this.standardError);
        if (this.profile.allowClass("UncaughtThrowError")) {
            this.uncaughtThrowError = RubyUncaughtThrowError.createUncaughtThrowErrorClass(this, this.argumentError);
        }
        this.indexError = this.defineClassIfAllowed("IndexError", this.standardError);
        if (this.profile.allowClass("StopIteration")) {
            this.stopIteration = RubyStopIteration.createStopIterationClass(this, this.indexError);
        }
        this.syntaxError = this.defineClassIfAllowed("SyntaxError", this.scriptError);
        this.loadError = this.defineClassIfAllowed("LoadError", this.scriptError);
        this.notImplementedError = this.defineClassIfAllowed("NotImplementedError", this.scriptError);
        this.securityError = this.defineClassIfAllowed("SecurityError", this.standardError);
        this.noMemoryError = this.defineClassIfAllowed("NoMemoryError", this.exceptionClass);
        this.regexpError = this.defineClassIfAllowed("RegexpError", this.standardError);
        this.interruptedRegexpError = this.defineClassIfAllowed("InterruptedRegexpError", this.regexpError);
        this.eofError = this.defineClassIfAllowed("EOFError", this.ioError);
        this.threadError = this.defineClassIfAllowed("ThreadError", this.standardError);
        this.concurrencyError = this.defineClassIfAllowed("ConcurrencyError", this.threadError);
        this.systemStackError = this.defineClassIfAllowed("SystemStackError", this.exceptionClass);
        this.zeroDivisionError = this.defineClassIfAllowed("ZeroDivisionError", this.standardError);
        this.floatDomainError = this.defineClassIfAllowed("FloatDomainError", this.rangeError);
        if (this.profile.allowClass("EncodingError")) {
            this.encodingError = this.defineClass("EncodingError", this.standardError, this.standardError.getAllocator());
            this.encodingCompatibilityError = this.defineClassUnder("CompatibilityError", this.encodingError, this.encodingError.getAllocator(), this.encodingClass);
            (this.invalidByteSequenceError = this.defineClassUnder("InvalidByteSequenceError", this.encodingError, this.encodingError.getAllocator(), this.encodingClass)).defineAnnotatedMethods(RubyConverter.EncodingErrorMethods.class);
            this.invalidByteSequenceError.defineAnnotatedMethods(RubyConverter.InvalidByteSequenceErrorMethods.class);
            (this.undefinedConversionError = this.defineClassUnder("UndefinedConversionError", this.encodingError, this.encodingError.getAllocator(), this.encodingClass)).defineAnnotatedMethods(RubyConverter.EncodingErrorMethods.class);
            this.undefinedConversionError.defineAnnotatedMethods(RubyConverter.UndefinedConversionErrorMethods.class);
            this.converterNotFoundError = this.defineClassUnder("ConverterNotFoundError", this.encodingError, this.encodingError.getAllocator(), this.encodingClass);
            this.fiberError = this.defineClass("FiberError", this.standardError, this.standardError.getAllocator());
        }
        this.concurrencyError = this.defineClassIfAllowed("ConcurrencyError", this.threadError);
        this.keyError = this.defineClassIfAllowed("KeyError", this.indexError);
        this.mathDomainError = this.defineClassUnder("DomainError", this.argumentError, this.argumentError.getAllocator(), this.mathModule);
        this.initErrno();
    }
    
    private void initLibraries() {
        new ThreadLibrary().load(this, false);
        new ThreadFiberLibrary().load(this, false);
    }
    
    private RubyClass defineClassIfAllowed(final String name, final RubyClass superClass) {
        if (superClass != null && this.profile.allowClass(name)) {
            return this.defineClass(name, superClass, superClass.getAllocator());
        }
        return null;
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
                Ruby.LOG.error(e2.getMessage(), e2);
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
    
    private void initBuiltins() {
        if (RubyInstanceConfig.DEBUG_PARSER) {
            return;
        }
        this.addLazyBuiltin("java.rb", "java", "org.jruby.javasupport.Java");
        this.addLazyBuiltin("jruby.rb", "jruby", "org.jruby.ext.jruby.JRubyLibrary");
        this.addLazyBuiltin("jruby/util.rb", "jruby/util", "org.jruby.ext.jruby.JRubyUtilLibrary");
        this.addLazyBuiltin("jruby/type.rb", "jruby/type", "org.jruby.ext.jruby.JRubyTypeLibrary");
        this.addLazyBuiltin("nkf.jar", "nkf", "org.jruby.ext.nkf.NKFLibrary");
        this.addLazyBuiltin("stringio.jar", "stringio", "org.jruby.ext.stringio.StringIOLibrary");
        this.addLazyBuiltin("strscan.jar", "strscan", "org.jruby.ext.strscan.StringScannerLibrary");
        this.addLazyBuiltin("zlib.jar", "zlib", "org.jruby.ext.zlib.ZlibLibrary");
        this.addLazyBuiltin("thread.jar", "thread", "org.jruby.ext.thread.ThreadLibrary");
        this.addLazyBuiltin("thread.rb", "thread", "org.jruby.ext.thread.ThreadLibrary");
        this.addLazyBuiltin("digest.jar", "digest.so", "org.jruby.ext.digest.DigestLibrary");
        this.addLazyBuiltin("digest/md5.jar", "digest/md5", "org.jruby.ext.digest.MD5");
        this.addLazyBuiltin("digest/rmd160.jar", "digest/rmd160", "org.jruby.ext.digest.RMD160");
        this.addLazyBuiltin("digest/sha1.jar", "digest/sha1", "org.jruby.ext.digest.SHA1");
        this.addLazyBuiltin("digest/sha2.jar", "digest/sha2", "org.jruby.ext.digest.SHA2");
        this.addLazyBuiltin("digest/bubblebabble.jar", "digest/bubblebabble", "org.jruby.ext.digest.BubbleBabble");
        this.addLazyBuiltin("bigdecimal.jar", "bigdecimal", "org.jruby.ext.bigdecimal.BigDecimalLibrary");
        this.addLazyBuiltin("io/wait.jar", "io/wait", "org.jruby.ext.io.wait.IOWaitLibrary");
        this.addLazyBuiltin("etc.jar", "etc", "org.jruby.ext.etc.EtcLibrary");
        this.addLazyBuiltin("weakref.rb", "weakref", "org.jruby.ext.weakref.WeakRefLibrary");
        this.addLazyBuiltin("timeout.rb", "timeout", "org.jruby.ext.timeout.Timeout");
        this.addLazyBuiltin("socket.jar", "socket", "org.jruby.ext.socket.SocketLibrary");
        this.addLazyBuiltin("rbconfig.rb", "rbconfig", "org.jruby.ext.rbconfig.RbConfigLibrary");
        this.addLazyBuiltin("jruby/serialization.rb", "serialization", "org.jruby.ext.jruby.JRubySerializationLibrary");
        this.addLazyBuiltin("ffi-internal.jar", "ffi-internal", "org.jruby.ext.ffi.FFIService");
        this.addLazyBuiltin("tempfile.jar", "tempfile", "org.jruby.ext.tempfile.TempfileLibrary");
        this.addLazyBuiltin("fcntl.rb", "fcntl", "org.jruby.ext.fcntl.FcntlLibrary");
        this.addLazyBuiltin("pathname.jar", "pathname", "org.jruby.ext.pathname.PathnameLibrary");
        this.addLazyBuiltin("mathn/complex.jar", "mathn/complex", "org.jruby.ext.mathn.Complex");
        this.addLazyBuiltin("mathn/rational.jar", "mathn/rational", "org.jruby.ext.mathn.Rational");
        this.addLazyBuiltin("ripper.jar", "ripper", "org.jruby.ext.ripper.RipperLibrary");
        this.addLazyBuiltin("coverage.jar", "coverage", "org.jruby.ext.coverage.CoverageLibrary");
        this.addBuiltinIfAllowed("continuation.rb", Library.DUMMY);
        this.loadService.provide("enumerator.jar");
        this.loadService.provide("rational.jar");
        this.loadService.provide("complex.jar");
        this.addBuiltinIfAllowed("thread.rb", Library.DUMMY);
        if (RubyInstanceConfig.NATIVE_NET_PROTOCOL) {
            this.addLazyBuiltin("net/protocol.rb", "net/protocol", "org.jruby.ext.net.protocol.NetProtocolBufferedIOLibrary");
        }
        this.addBuiltinIfAllowed("win32ole.jar", new Library() {
            @Override
            public void load(final Ruby runtime, final boolean wrap) throws IOException {
                runtime.getLoadService().require("jruby/win32ole/stub");
            }
        });
        this.addLazyBuiltin("cgi/escape.jar", "cgi/escape", "org.jruby.ext.cgi.escape.CGIEscape");
    }
    
    private void initRubyKernel() {
        if (RubyInstanceConfig.DEBUG_PARSER) {
            return;
        }
        this.loadService.loadFromClassLoader(getClassLoader(), "jruby/kernel.rb", false);
    }
    
    private void initRubyPreludes() {
        if (RubyInstanceConfig.DEBUG_PARSER) {
            return;
        }
        this.loadService.loadFromClassLoader(getClassLoader(), "jruby/preludes.rb", false);
    }
    
    private void addLazyBuiltin(final String name, final String shortName, final String className) {
        this.addBuiltinIfAllowed(name, new LateLoadingLibrary(shortName, className, getClassLoader()));
    }
    
    private void addBuiltinIfAllowed(final String name, final Library lib) {
        if (this.profile.allowBuiltin(name)) {
            this.loadService.addBuiltinLibrary(name, lib);
        }
    }
    
    public IRManager getIRManager() {
        return this.irManager;
    }
    
    public IRubyObject getTopSelf() {
        return this.topSelf;
    }
    
    public IRubyObject getRootFiber() {
        return this.rootFiber;
    }
    
    public void setRootFiber(final IRubyObject fiber) {
        this.rootFiber = fiber;
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
    
    void setKernel(final RubyModule kernelModule) {
        this.kernelModule = kernelModule;
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
    
    void setComparable(final RubyModule comparableModule) {
        this.comparableModule = comparableModule;
    }
    
    public RubyClass getNumeric() {
        return this.numericClass;
    }
    
    void setNumeric(final RubyClass numericClass) {
        this.numericClass = numericClass;
    }
    
    public RubyClass getFloat() {
        return this.floatClass;
    }
    
    void setFloat(final RubyClass floatClass) {
        this.floatClass = floatClass;
    }
    
    public RubyClass getInteger() {
        return this.integerClass;
    }
    
    void setInteger(final RubyClass integerClass) {
        this.integerClass = integerClass;
    }
    
    public RubyClass getFixnum() {
        return this.fixnumClass;
    }
    
    void setFixnum(final RubyClass fixnumClass) {
        this.fixnumClass = fixnumClass;
    }
    
    public RubyClass getComplex() {
        return this.complexClass;
    }
    
    void setComplex(final RubyClass complexClass) {
        this.complexClass = complexClass;
    }
    
    public RubyClass getRational() {
        return this.rationalClass;
    }
    
    void setRational(final RubyClass rationalClass) {
        this.rationalClass = rationalClass;
    }
    
    public RubyModule getEnumerable() {
        return this.enumerableModule;
    }
    
    void setEnumerable(final RubyModule enumerableModule) {
        this.enumerableModule = enumerableModule;
    }
    
    public RubyClass getEnumerator() {
        return this.enumeratorClass;
    }
    
    void setEnumerator(final RubyClass enumeratorClass) {
        this.enumeratorClass = enumeratorClass;
    }
    
    public RubyClass getYielder() {
        return this.yielderClass;
    }
    
    void setYielder(final RubyClass yielderClass) {
        this.yielderClass = yielderClass;
    }
    
    public RubyClass getGenerator() {
        return this.generatorClass;
    }
    
    public void setGenerator(final RubyClass generatorClass) {
        this.generatorClass = generatorClass;
    }
    
    public RubyClass getFiber() {
        return this.fiberClass;
    }
    
    public void setFiber(final RubyClass fiberClass) {
        this.fiberClass = fiberClass;
    }
    
    public RubyClass getString() {
        return this.stringClass;
    }
    
    void setString(final RubyClass stringClass) {
        this.stringClass = stringClass;
    }
    
    public RubyClass getEncoding() {
        return this.encodingClass;
    }
    
    void setEncoding(final RubyClass encodingClass) {
        this.encodingClass = encodingClass;
    }
    
    public RubyClass getConverter() {
        return this.converterClass;
    }
    
    void setConverter(final RubyClass converterClass) {
        this.converterClass = converterClass;
    }
    
    public RubyClass getSymbol() {
        return this.symbolClass;
    }
    
    void setSymbol(final RubyClass symbolClass) {
        this.symbolClass = symbolClass;
    }
    
    public RubyClass getArray() {
        return this.arrayClass;
    }
    
    void setArray(final RubyClass arrayClass) {
        this.arrayClass = arrayClass;
    }
    
    public RubyClass getHash() {
        return this.hashClass;
    }
    
    void setHash(final RubyClass hashClass) {
        this.hashClass = hashClass;
    }
    
    public RubyClass getRange() {
        return this.rangeClass;
    }
    
    void setRange(final RubyClass rangeClass) {
        this.rangeClass = rangeClass;
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
    
    void setNilClass(final RubyClass nilClass) {
        this.nilClass = nilClass;
    }
    
    public RubyClass getTrueClass() {
        return this.trueClass;
    }
    
    void setTrueClass(final RubyClass trueClass) {
        this.trueClass = trueClass;
    }
    
    public RubyClass getFalseClass() {
        return this.falseClass;
    }
    
    void setFalseClass(final RubyClass falseClass) {
        this.falseClass = falseClass;
    }
    
    public RubyClass getProc() {
        return this.procClass;
    }
    
    void setProc(final RubyClass procClass) {
        this.procClass = procClass;
    }
    
    public RubyClass getBinding() {
        return this.bindingClass;
    }
    
    void setBinding(final RubyClass bindingClass) {
        this.bindingClass = bindingClass;
    }
    
    public RubyClass getMethod() {
        return this.methodClass;
    }
    
    void setMethod(final RubyClass methodClass) {
        this.methodClass = methodClass;
    }
    
    public RubyClass getUnboundMethod() {
        return this.unboundMethodClass;
    }
    
    void setUnboundMethod(final RubyClass unboundMethodClass) {
        this.unboundMethodClass = unboundMethodClass;
    }
    
    public RubyClass getMatchData() {
        return this.matchDataClass;
    }
    
    void setMatchData(final RubyClass matchDataClass) {
        this.matchDataClass = matchDataClass;
    }
    
    public RubyClass getRegexp() {
        return this.regexpClass;
    }
    
    void setRegexp(final RubyClass regexpClass) {
        this.regexpClass = regexpClass;
    }
    
    public RubyClass getTime() {
        return this.timeClass;
    }
    
    void setTime(final RubyClass timeClass) {
        this.timeClass = timeClass;
    }
    
    public RubyModule getMath() {
        return this.mathModule;
    }
    
    void setMath(final RubyModule mathModule) {
        this.mathModule = mathModule;
    }
    
    public RubyModule getMarshal() {
        return this.marshalModule;
    }
    
    void setMarshal(final RubyModule marshalModule) {
        this.marshalModule = marshalModule;
    }
    
    public RubyClass getBignum() {
        return this.bignumClass;
    }
    
    void setBignum(final RubyClass bignumClass) {
        this.bignumClass = bignumClass;
    }
    
    public RubyClass getDir() {
        return this.dirClass;
    }
    
    void setDir(final RubyClass dirClass) {
        this.dirClass = dirClass;
    }
    
    public RubyClass getFile() {
        return this.fileClass;
    }
    
    void setFile(final RubyClass fileClass) {
        this.fileClass = fileClass;
    }
    
    public RubyClass getFileStat() {
        return this.fileStatClass;
    }
    
    void setFileStat(final RubyClass fileStatClass) {
        this.fileStatClass = fileStatClass;
    }
    
    public RubyModule getFileTest() {
        return this.fileTestModule;
    }
    
    void setFileTest(final RubyModule fileTestModule) {
        this.fileTestModule = fileTestModule;
    }
    
    public RubyClass getIO() {
        return this.ioClass;
    }
    
    void setIO(final RubyClass ioClass) {
        this.ioClass = ioClass;
    }
    
    public RubyClass getThread() {
        return this.threadClass;
    }
    
    void setThread(final RubyClass threadClass) {
        this.threadClass = threadClass;
    }
    
    public RubyClass getThreadGroup() {
        return this.threadGroupClass;
    }
    
    void setThreadGroup(final RubyClass threadGroupClass) {
        this.threadGroupClass = threadGroupClass;
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
    
    void setContinuation(final RubyClass continuationClass) {
        this.continuationClass = continuationClass;
    }
    
    public RubyClass getStructClass() {
        return this.structClass;
    }
    
    void setStructClass(final RubyClass structClass) {
        this.structClass = structClass;
    }
    
    public RubyClass getRandomClass() {
        return this.randomClass;
    }
    
    void setRandomClass(final RubyClass randomClass) {
        this.randomClass = randomClass;
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
    
    void setGC(final RubyModule gcModule) {
        this.gcModule = gcModule;
    }
    
    public RubyModule getObjectSpaceModule() {
        return this.objectSpaceModule;
    }
    
    void setObjectSpaceModule(final RubyModule objectSpaceModule) {
        this.objectSpaceModule = objectSpaceModule;
    }
    
    public RubyModule getProcess() {
        return this.processModule;
    }
    
    void setProcess(final RubyModule processModule) {
        this.processModule = processModule;
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
    
    public RubyModule getErrno() {
        return this.errnoModule;
    }
    
    public RubyClass getException() {
        return this.exceptionClass;
    }
    
    void setException(final RubyClass exceptionClass) {
        this.exceptionClass = exceptionClass;
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
    
    public RubyRandom.RandomType getDefaultRand() {
        return this.defaultRand;
    }
    
    public void setDefaultRand(final RubyRandom.RandomType defaultRand) {
        this.defaultRand = defaultRand;
    }
    
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
    
    public synchronized JRubyClassLoader getJRubyClassLoader() {
        if (!isSecurityRestricted() && this.jrubyClassLoader == null) {
            if (this.config.isClassloaderDelegate()) {
                this.jrubyClassLoader = new JRubyClassLoader(this.config.getLoader());
            }
            else {
                this.jrubyClassLoader = new SelfFirstJRubyClassLoader(this.config.getLoader());
            }
        }
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
            return this.parseFileAndGetAST(in, file, scope, lineNumber, false);
        }
        try {
            return IRReader.load(this.getIRManager(), new IRReaderStream(this.getIRManager(), IRFileExpert.getIRPersistedFile(file), new ByteList(file.getBytes())));
        }
        catch (IOException e) {
            return this.parseFileAndGetAST(in, file, scope, lineNumber, false);
        }
    }
    
    public Node parseFileFromMain(final InputStream in, final String file, final DynamicScope scope) {
        this.addLoadParseToStats();
        return this.parseFileFromMainAndGetAST(in, file, scope);
    }
    
    public ParseResult parseFileFromMain(final String file, final InputStream in, final DynamicScope scope) {
        this.addLoadParseToStats();
        if (!RubyInstanceConfig.IR_READING) {
            return this.parseFileFromMainAndGetAST(in, file, scope);
        }
        try {
            return IRReader.load(this.getIRManager(), new IRReaderStream(this.getIRManager(), IRFileExpert.getIRPersistedFile(file), new ByteList(file.getBytes())));
        }
        catch (IOException e) {
            System.out.println(e);
            e.printStackTrace();
            return this.parseFileFromMainAndGetAST(in, file, scope);
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
        return this.threadService.getCurrentContext();
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
    
    public Charset getDefaultCharset() {
        Encoding enc = this.getDefaultInternalEncoding();
        if (enc == null) {
            enc = (Encoding)UTF8Encoding.INSTANCE;
        }
        final Charset charset = enc.getCharset();
        return charset;
    }
    
    public EncodingService getEncodingService() {
        return this.encodingService;
    }
    
    public RubyWarnings getWarnings() {
        return this.warnings;
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
        if (path.length() == 0 || path.charAt(0) == '#') {
            throw this.newTypeError("can't retrieve anonymous class " + path);
        }
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
                    throw this.newTypeError("undefined class/module " + str);
                }
                pbeg = ++p;
            }
            final IRubyObject cc = c.getConstant(str);
            if (!(cc instanceof RubyModule)) {
                throw this.newTypeError(path + " does not refer to class/module");
            }
            c = (RubyModule)cc;
        }
        return c;
    }
    
    public void printError(final RubyException excp) {
        if (excp == null || excp.isNil()) {
            return;
        }
        final PrintStream errorStream = this.getErrorStream();
        final String backtrace = this.config.getTraceType().printBacktrace(excp, errorStream == System.err && this.getPosix().isatty(FileDescriptor.err));
        try {
            errorStream.print(backtrace);
        }
        catch (Exception e) {
            System.err.print(backtrace);
        }
    }
    
    public void loadFile(final String scriptName, final InputStream in, final boolean wrap) {
        final IRubyObject self = wrap ? this.getTopSelf().rbClone() : this.getTopSelf();
        final ThreadContext context = this.getCurrentContext();
        final String file = context.getFile();
        try {
            ThreadContext.pushBacktrace(context, "(root)", file, 0);
            context.preNodeEval(self);
            final ParseResult parseResult = this.parseFile(scriptName, in, null);
            final RootNode root = (RootNode)parseResult;
            if (wrap) {
                this.wrapRootForLoad((RubyBasicObject)self, root);
            }
            this.runInterpreter(context, parseResult, self);
        }
        finally {
            context.postNodeEval();
            ThreadContext.popBacktrace(context);
        }
    }
    
    public void loadScope(final IRScope scope, final boolean wrap) {
        final IRubyObject self = wrap ? TopSelfFactory.createTopSelf(this, true) : this.getTopSelf();
        final ThreadContext context = this.getCurrentContext();
        final String file = context.getFile();
        try {
            ThreadContext.pushBacktrace(context, "(root)", file, 0);
            context.preNodeEval(self);
            if (wrap) {
                scope.getStaticScope().setModule(RubyModule.newModule(this));
            }
            this.runInterpreter(context, scope, self);
        }
        finally {
            context.postNodeEval();
            ThreadContext.popBacktrace(context);
        }
    }
    
    public void compileAndLoadFile(final String filename, final InputStream in, final boolean wrap) {
        final IRubyObject self = wrap ? this.getTopSelf().rbClone() : this.getTopSelf();
        final ThreadContext context = this.getCurrentContext();
        final InputStream readStream = in;
        final String oldFile = context.getFile();
        final int oldLine = context.getLine();
        try {
            context.preNodeEval(self);
            final ParseResult parseResult = this.parseFile(filename, in, null);
            final RootNode root = (RootNode)parseResult;
            if (wrap) {
                this.wrapRootForLoad((RubyBasicObject)self, root);
            }
            else {
                root.getStaticScope().setModule(this.getObject());
            }
            this.runNormally(root, wrap);
        }
        finally {
            context.postNodeEval();
        }
    }
    
    private void wrapRootForLoad(final RubyBasicObject self, final RootNode root) {
        final RubyModule wrapper = RubyModule.newModule(this);
        self.extend(new IRubyObject[] { wrapper });
        final StaticScope top = root.getStaticScope();
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
            javaToRuby = new HashMap<String, String>();
            this.boundMethods.put(className, javaToRuby);
        }
        javaToRuby.put(methodName, rubyName);
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
        if (!RubyInstanceConfig.FULL_TRACE_ENABLED) {
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
            for (int i = 0; i < this.eventHooks.length; ++i) {
                final EventHook eventHook = this.eventHooks[i];
                if (eventHook.isInterestedInEvent(event)) {
                    eventHook.event(context, event, file, line, name, type);
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
    
    public void setGlobalVariables(final GlobalVariables globalVariables) {
        this.globalVariables = globalVariables;
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
        this.getThreadService().disposeCurrentThread();
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
            this.getJRubyClassLoader().close();
            this.jrubyClassLoader = null;
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
        return this.newRaiseException(this.getArgumentError(), "wrong number of arguments (" + got + " for " + expected + ")");
    }
    
    public RaiseException newArgumentError(final String name, final int got, final int expected) {
        return this.newRaiseException(this.getArgumentError(), "wrong number of arguments calling `" + name + "` (" + got + " for " + expected + ")");
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
        return this.newLightweightErrnoException(this.getModule("IO").getClass("EAGAINWaitReadable"), message);
    }
    
    public RaiseException newErrnoEAGAINWritableError(final String message) {
        return this.newLightweightErrnoException(this.getModule("IO").getClass("EAGAINWaitWritable"), message);
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
    
    public RaiseException newIndexError(final String message) {
        return this.newRaiseException(this.getIndexError(), message);
    }
    
    public RaiseException newSecurityError(final String message) {
        return this.newRaiseException(this.getSecurityError(), message);
    }
    
    public RaiseException newSystemCallError(final String message) {
        return this.newRaiseException(this.getSystemCallError(), message);
    }
    
    public RaiseException newKeyError(final String message) {
        return this.newRaiseException(this.getKeyError(), message);
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
    
    public RaiseException newErrnoEADDRFromBindException(final BindException be) {
        return this.newErrnoEADDRFromBindException(be, null);
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
        final IRubyObject msg = new RubyNameError.RubyNameErrorMessage(this, message, recv, name);
        final RubyException err = RubyNameError.newNameError(this.getNameError(), msg, name);
        return new RaiseException(err);
    }
    
    public RaiseException newNameError(final String message, final IRubyObject recv, final String name) {
        final RubySymbol nameSym = this.newSymbol(name);
        return this.newNameError(message, recv, nameSym);
    }
    
    public RaiseException newNameError(final String message, final String name, final Throwable origException, final boolean printWhenVerbose) {
        if (origException != null) {
            if (printWhenVerbose && this.isVerbose()) {
                Ruby.LOG.error(origException.getMessage(), origException);
            }
            else if (this.isDebug()) {
                Ruby.LOG.debug(origException.getMessage(), origException);
            }
        }
        return new RaiseException(new RubyNameError(this, this.getNameError(), message, name), false);
    }
    
    public RaiseException newNameError(final String message, final String name) {
        return this.newNameError(message, name, null);
    }
    
    public RaiseException newNameError(final String message, final String name, final Throwable origException) {
        return this.newNameError(message, name, origException, false);
    }
    
    public RaiseException newNoMethodError(final String message, final IRubyObject recv, final String name, final RubyArray args) {
        final RubySymbol nameStr = this.newSymbol(name);
        final IRubyObject msg = new RubyNameError.RubyNameErrorMessage(this, message, recv, nameStr);
        final RubyException err = RubyNoMethodError.newNoMethodError(this.getNoMethodError(), msg, nameStr, args);
        return new RaiseException(err);
    }
    
    public RaiseException newNoMethodError(final String message, final String name, final IRubyObject args) {
        return new RaiseException(new RubyNoMethodError(this, this.getNoMethodError(), message, name, args), true);
    }
    
    public RaiseException newLocalJumpError(final RubyLocalJumpError.Reason reason, final IRubyObject exitValue, final String message) {
        return new RaiseException(new RubyLocalJumpError(this, this.getLocalJumpError(), message, reason, exitValue), true);
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
    
    public RaiseException newFrozenError(final String objectType, final boolean runtimeError) {
        return this.newRaiseException(this.getRuntimeError(), "can't modify frozen " + objectType);
    }
    
    public RaiseException newSystemStackError(final String message) {
        return this.newRaiseException(this.getSystemStackError(), message);
    }
    
    public RaiseException newSystemStackError(final String message, final StackOverflowError soe) {
        if (this.isDebug()) {
            Ruby.LOG.debug(soe);
        }
        return this.newRaiseException(this.getSystemStackError(), message);
    }
    
    public RaiseException newSystemExit(final int status) {
        return new RaiseException(RubySystemExit.newInstance(this, status, "exit"));
    }
    
    public RaiseException newSystemExit(final int status, final String message) {
        return new RaiseException(RubySystemExit.newInstance(this, status, message));
    }
    
    public RaiseException newIOError(final String message) {
        return this.newRaiseException(this.getIOError(), message);
    }
    
    public RaiseException newStandardError(final String message) {
        return this.newRaiseException(this.getStandardError(), message);
    }
    
    public RaiseException newIOErrorFromException(final IOException ex) {
        final String errorMessage = ex.getMessage();
        if (errorMessage != null) {
            final String s = errorMessage;
            switch (s) {
                case "Bad file descriptor": {
                    if (ex instanceof ClosedChannelException) {
                        throw this.newErrnoEBADFError();
                    }
                    break;
                }
                case "File not open": {
                    return this.newIOError(errorMessage);
                }
                case "An established connection was aborted by the software in your host machine": {
                    return this.newErrnoECONNABORTEDError();
                }
                case "Broken pipe": {
                    return this.newErrnoEPIPEError();
                }
                case "Connection reset by peer":
                case "An existing connection was forcibly closed by the remote host": {
                    return this.newErrnoECONNRESETError();
                }
                case "Too many levels of symbolic links": {
                    return this.newErrnoELOOPError();
                }
                case "Too many open files": {
                    return this.newErrnoEMFILEError();
                }
                case "Too many open files in system": {
                    return this.newErrnoENFILEError();
                }
                case "Network is unreachable": {
                    return this.newErrnoENETUNREACHError();
                }
                case "Address already in use": {
                    return this.newErrnoEADDRINUSEError();
                }
                default: {
                    if (Platform.IS_WINDOWS && errorMessage.contains("connection was aborted")) {
                        return this.newErrnoECONNRESETError();
                    }
                    break;
                }
            }
            return this.newRaiseException(this.getIOError(), errorMessage);
        }
        return this.newRaiseException(this.getIOError(), "IO Error");
    }
    
    public RaiseException newTypeError(final IRubyObject receivedObject, final RubyClass expectedType) {
        return this.newTypeError(receivedObject, expectedType.getName());
    }
    
    public RaiseException newTypeError(final IRubyObject receivedObject, final RubyModule expectedType) {
        return this.newTypeError(receivedObject, expectedType.getName());
    }
    
    public RaiseException newTypeError(final IRubyObject receivedObject, final String expectedType) {
        return this.newRaiseException(this.getTypeError(), "wrong argument type " + receivedObject.getMetaClass().getRealClass() + " (expected " + expectedType + ")");
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
        return new RaiseException(this, exceptionClass, message, true);
    }
    
    private RaiseException newLightweightErrnoException(final RubyClass exceptionClass, final String message) {
        if (RubyInstanceConfig.ERRNO_BACKTRACE) {
            return new RaiseException(this, exceptionClass, message, true);
        }
        return new RaiseException(this, exceptionClass, "errno backtraces disabled; run with -Xerrno.backtrace=true to enable", this.disabledBacktrace(), true);
    }
    
    public RaiseException newStopIteration(final IRubyObject result, String message) {
        final ThreadContext context = this.getCurrentContext();
        if (RubyInstanceConfig.STOPITERATION_BACKTRACE) {
            final RubyException ex = RubyStopIteration.newInstance(context, result, message);
            return new RaiseException(ex);
        }
        if (message == null) {
            message = "StopIteration backtraces disabled; run with -Xstop_iteration.backtrace=true to enable";
        }
        final RubyException ex = RubyStopIteration.newInstance(context, result, message);
        return new RaiseException(ex, this.disabledBacktrace());
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
    
    public boolean isDoNotReverseLookupEnabled() {
        return this.doNotReverseLookupEnabled;
    }
    
    public void setDoNotReverseLookupEnabled(final boolean b) {
        this.doNotReverseLookupEnabled = b;
    }
    
    public void registerInspecting(final Object obj) {
        Map<Object, Object> val = this.inspect.get();
        if (val == null) {
            this.inspect.set(val = new IdentityHashMap<Object, Object>());
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
    
    public boolean is2_0() {
        return true;
    }
    
    public long getGlobalState() {
        synchronized (this) {
            return this.globalState;
        }
    }
    
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
        final Invalidator invalidator = OptoFactory.newConstantInvalidator();
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
    
    void addProfiledMethod(final String name, final DynamicMethod method) {
        if (!this.config.isProfiling()) {
            return;
        }
        if (method.isUndefined()) {
            return;
        }
        this.getProfilingService().addProfiledMethod(name, method);
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
    
    public void reopenFixnum() {
        this.fixnumInvalidator.invalidate();
        this.fixnumReopened = true;
    }
    
    public Invalidator getFixnumInvalidator() {
        return this.fixnumInvalidator;
    }
    
    public boolean isFixnumReopened() {
        return this.fixnumReopened;
    }
    
    public void reopenFloat() {
        this.floatInvalidator.invalidate();
        this.floatReopened = true;
    }
    
    public Invalidator getFloatInvalidator() {
        return this.floatInvalidator;
    }
    
    public boolean isFloatReopened() {
        return this.floatReopened;
    }
    
    public boolean isBootingCore() {
        return this.bootingCore;
    }
    
    public boolean isBooting() {
        return this.bootingRuntime;
    }
    
    public CoverageData getCoverageData() {
        return this.coverageData;
    }
    
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
        final WeakReference<RubyString> dedupedRef = this.dedupMap.get(string);
        RubyString deduped;
        if (dedupedRef == null || (deduped = dedupedRef.get()) == null) {
            deduped = string.strDup(this);
            deduped.setFrozen(true);
            this.dedupMap.put(string, new WeakReference<RubyString>(deduped));
        }
        else if (deduped.getEncoding() != string.getEncoding()) {
            deduped = string.strDup(this);
            deduped.setFrozen(true);
        }
        return deduped;
    }
    
    public int getRuntimeNumber() {
        return this.runtimeNumber;
    }
    
    private void setNetworkStack() {
        try {
            if (this.config.getIPv4Preferred()) {
                System.setProperty("java.net.preferIPv4Stack", "true");
            }
            else {
                System.setProperty("java.net.preferIPv4Stack", "false");
            }
        }
        catch (AccessControlException ace) {
            if (this.isVerbose()) {
                Ruby.LOG.warn("warning: unable to set network stack system property due to security restrictions, please set it manually as JVM parameter (-Djava.net.preferIPv4Stack=true|false)", new Object[0]);
            }
        }
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
        return new RaiseException(error, false);
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
    
    public void addToObjectSpace(final boolean useObjectSpace, final IRubyObject object) {
        this.objectSpacer.addToObjectSpace(this, useObjectSpace, object);
    }
    
    static {
        LOG = LoggerFactory.getLogger(Ruby.class);
        if (RubyInstanceConfig.JIT_LOADING_DEBUG) {
            Ruby.LOG.setDebugEnable(true);
        }
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
        Ruby.threadLocalRuntime = new ThreadLocal<Ruby>();
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
    
    public class CallTraceFuncHook extends EventHook
    {
        private RubyProc traceFunc;
        private EnumSet<RubyEvent> interest;
        
        public CallTraceFuncHook() {
            this.interest = EnumSet.of(RubyEvent.C_CALL, RubyEvent.C_RETURN, RubyEvent.CALL, RubyEvent.CLASS, RubyEvent.END, RubyEvent.LINE, RubyEvent.RAISE, RubyEvent.RETURN);
        }
        
        public void setTraceFunc(final RubyProc traceFunc) {
            this.traceFunc = traceFunc;
        }
        
        @Override
        public void eventHandler(final ThreadContext context, final String eventName, String file, final int line, final String name, IRubyObject type) {
            if (!context.isWithinTrace()) {
                if (file == null) {
                    file = "(ruby)";
                }
                if (type == null) {
                    type = Ruby.this.getNil();
                }
                final RubyBinding binding = RubyBinding.newBinding(Ruby.this, context.currentBinding());
                context.preTrace();
                try {
                    this.traceFunc.call(context, new IRubyObject[] { Ruby.this.newString(eventName), Ruby.this.newString(file), Ruby.this.newFixnum(line), (name != null) ? Ruby.this.newSymbol(name) : Ruby.this.getNil(), binding, type });
                }
                finally {
                    context.postTrace();
                }
            }
        }
        
        @Override
        public boolean isInterestedInEvent(final RubyEvent event) {
            return this.interest.contains(event);
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
