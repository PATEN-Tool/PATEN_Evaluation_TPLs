// 
// Decompiled by Procyon v0.5.36
// 

package org.jruby;

import java.lang.ref.WeakReference;
import org.jruby.util.SafePropertyAccessor;
import com.kenai.constantine.ConstantSet;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.lang.ref.Reference;
import org.jruby.runtime.Binding;
import java.io.FileDescriptor;
import java.util.Collection;
import org.jruby.exceptions.RaiseException;
import java.util.WeakHashMap;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.RubyEvent;
import java.util.ArrayList;
import org.jruby.util.IOInputStream;
import java.io.OutputStream;
import org.jruby.util.IOOutputStream;
import org.jruby.util.ByteList;
import org.jruby.parser.ParserConfiguration;
import org.jruby.ext.LateLoadingLibrary;
import org.jruby.util.BuiltinScript;
import java.io.IOException;
import org.jruby.runtime.load.Library;
import com.kenai.constantine.Constant;
import com.kenai.constantine.platform.Errno;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import org.jruby.threading.DaemonThreadFactory;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import org.jruby.ext.posix.POSIXHandler;
import org.jruby.ext.posix.POSIXFactory;
import org.jruby.ext.JRubyPOSIXHandler;
import org.jruby.common.IRubyWarnings;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.compiler.ASTCompiler;
import org.jruby.compiler.NotCompilableException;
import org.jruby.compiler.ScriptCompiler;
import org.jruby.compiler.impl.StandardASMCompiler;
import org.jruby.compiler.ASTInspector;
import org.jruby.util.JavaNameMangler;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.ast.RootNode;
import java.util.Iterator;
import org.jruby.runtime.IAccessor;
import org.jruby.exceptions.MainExitException;
import org.jruby.runtime.load.CompiledScriptLoader;
import org.jruby.internal.runtime.ValueAccessor;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import org.jruby.ast.Node;
import org.jruby.runtime.ThreadContext;
import org.jruby.exceptions.JumpException;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.scope.ManyVarsDynamicScope;
import org.jruby.parser.EvalStaticScope;
import org.jruby.management.ClassCacheMBean;
import org.jruby.management.ClassCache;
import org.jruby.management.ParserStatsMBean;
import org.jruby.management.ConfigMBean;
import org.jruby.management.Config;
import org.jruby.management.BeanManagerFactory;
import org.jruby.util.SimpleSampler;
import java.util.Collections;
import org.jruby.util.collections.WeakHashSet;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import org.joda.time.DateTimeZone;
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
import org.jruby.ast.executable.Script;
import java.util.Set;
import org.jruby.runtime.EventHook;
import java.util.List;
import java.util.Random;
import org.jruby.util.io.ChannelDescriptor;
import java.lang.ref.ReferenceQueue;
import org.jruby.runtime.ObjectSpace;
import org.jruby.ext.posix.POSIX;
import org.jruby.internal.runtime.ThreadService;
import java.util.Map;
import org.jruby.runtime.builtin.IRubyObject;

public final class Ruby
{
    private static Ruby globalRuntime;
    public static final int NIL_PREFILLED_ARRAY_SIZE = 128;
    private final IRubyObject[] nilPrefilledArray;
    private Map<Integer, RubyClass> errnos;
    private RubyHash charsetMap;
    private final CallTraceFuncHook callTraceFuncHook;
    private ThreadLocal<Map<Object, Object>> inspect;
    private volatile int constantGeneration;
    private final ThreadService threadService;
    private POSIX posix;
    private int stackTraces;
    private ObjectSpace objectSpace;
    private final RubySymbol.SymbolTable symbolTable;
    private Map<Integer, WeakDescriptorReference> descriptors;
    private ReferenceQueue<ChannelDescriptor> descriptorQueue;
    private Map<Integer, ChannelDescriptor> retainedDescriptors;
    private long randomSeed;
    private long randomSeedSequence;
    private Random random;
    private List<EventHook> eventHooks;
    private boolean hasEventHooks;
    private boolean globalAbortOnExceptionEnabled;
    private boolean doNotReverseLookupEnabled;
    private volatile boolean objectSpaceEnabled;
    private final Set<Script> jittedMethods;
    private static ThreadLocal<Ruby> currentRuntime;
    private long globalState;
    private int safeLevel;
    private IRubyObject topSelf;
    private RubyNil nilObject;
    private IRubyObject[] singleNilArray;
    private RubyBoolean trueObject;
    private RubyBoolean falseObject;
    public final RubyFixnum[] fixnumCache;
    private IRubyObject verbose;
    private boolean isVerbose;
    private boolean warningsEnabled;
    private IRubyObject debug;
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
    private RubyClass generatorClass;
    private RubyClass yielderClass;
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
    private RubyClass encodingError;
    private RubyClass encodingCompatibilityError;
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
    private GlobalVariable recordSeparatorVar;
    private String currentDirectory;
    private long startTime;
    private final RubyInstanceConfig config;
    private final boolean is1_9;
    private final InputStream in;
    private final PrintStream out;
    private final PrintStream err;
    private JavaSupport javaSupport;
    private JRubyClassLoader jrubyClassLoader;
    private BeanManager beanManager;
    private ParserStats parserStats;
    private final JITCompiler jitCompiler;
    private static volatile boolean securityRestricted;
    private Parser parser;
    private LoadService loadService;
    private Encoding defaultInternalEncoding;
    private Encoding defaultExternalEncoding;
    private EncodingService encodingService;
    private GlobalVariables globalVariables;
    private RubyWarnings warnings;
    private Stack<RubyProc> atExitBlocks;
    private Profile profile;
    private KCode kcode;
    private AtomicInteger symbolLastId;
    private AtomicInteger moduleLastId;
    private Object respondToMethod;
    private Object objectToYamlMethod;
    private Map<String, DateTimeZone> timeZoneCache;
    private Map<Finalizable, Object> finalizers;
    private Map<Finalizable, Object> internalFinalizers;
    private final Object finalizersMutex;
    private final Object internalFinalizersMutex;
    private ExecutorService executor;
    private Object hierarchyLock;
    
    public static Ruby newInstance() {
        return newInstance(new RubyInstanceConfig());
    }
    
    public static Ruby newInstance(final RubyInstanceConfig config) {
        final Ruby ruby = new Ruby(config);
        ruby.init();
        return ruby;
    }
    
    public static Ruby newInstance(final InputStream in, final PrintStream out, final PrintStream err) {
        final RubyInstanceConfig config = new RubyInstanceConfig();
        config.setInput(in);
        config.setOutput(out);
        config.setError(err);
        return newInstance(config);
    }
    
    public static synchronized Ruby getGlobalRuntime() {
        if (Ruby.globalRuntime == null) {
            initGlobalRuntime();
        }
        return Ruby.globalRuntime;
    }
    
    private static void initGlobalRuntime() {
        Ruby.globalRuntime = newInstance();
    }
    
    private Ruby(final RubyInstanceConfig config) {
        this.nilPrefilledArray = new IRubyObject[128];
        this.errnos = new HashMap<Integer, RubyClass>();
        this.callTraceFuncHook = new CallTraceFuncHook();
        this.inspect = new ThreadLocal<Map<Object, Object>>();
        this.constantGeneration = 1;
        this.stackTraces = 0;
        this.objectSpace = new ObjectSpace();
        this.symbolTable = new RubySymbol.SymbolTable(this);
        this.descriptors = new ConcurrentHashMap<Integer, WeakDescriptorReference>();
        this.descriptorQueue = new ReferenceQueue<ChannelDescriptor>();
        this.retainedDescriptors = new ConcurrentHashMap<Integer, ChannelDescriptor>();
        this.randomSeed = 0L;
        this.randomSeedSequence = 0L;
        this.random = new Random();
        this.eventHooks = new Vector<EventHook>();
        this.globalAbortOnExceptionEnabled = false;
        this.doNotReverseLookupEnabled = false;
        this.jittedMethods = Collections.synchronizedSet(new WeakHashSet<Script>());
        this.globalState = 1L;
        this.safeLevel = -1;
        this.fixnumCache = new RubyFixnum[256];
        this.startTime = System.currentTimeMillis();
        this.parser = new Parser(this);
        this.globalVariables = new GlobalVariables(this);
        this.warnings = new RubyWarnings(this);
        this.atExitBlocks = new Stack<RubyProc>();
        this.kcode = KCode.NONE;
        this.symbolLastId = new AtomicInteger(128);
        this.moduleLastId = new AtomicInteger(0);
        this.timeZoneCache = new HashMap<String, DateTimeZone>();
        this.finalizersMutex = new Object();
        this.internalFinalizersMutex = new Object();
        this.hierarchyLock = new Object();
        this.config = config;
        this.is1_9 = (config.getCompatVersion() == CompatVersion.RUBY1_9);
        this.threadService = new ThreadService(this);
        if (config.isSamplingEnabled()) {
            SimpleSampler.registerThreadContext(this.threadService.getCurrentContext());
        }
        this.in = config.getInput();
        this.out = config.getOutput();
        this.err = config.getError();
        this.objectSpaceEnabled = config.isObjectSpaceEnabled();
        this.profile = config.getProfile();
        this.currentDirectory = config.getCurrentDirectory();
        this.kcode = config.getKCode();
        this.beanManager = BeanManagerFactory.create(this, config.isManagementEnabled());
        this.jitCompiler = new JITCompiler(this);
        this.parserStats = new ParserStats(this);
        this.beanManager.register(new Config(this));
        this.beanManager.register(this.parserStats);
        this.beanManager.register(new ClassCache(this));
    }
    
    public IRubyObject evalScriptlet(final String script) {
        final ThreadContext context = this.getCurrentContext();
        final DynamicScope currentScope = context.getCurrentScope();
        final ManyVarsDynamicScope newScope = new ManyVarsDynamicScope(new EvalStaticScope(currentScope.getStaticScope()), currentScope);
        final Node node = this.parseEval(script, "<script>", newScope, 0);
        try {
            context.preEvalScriptlet(newScope);
            return node.interpret(this, context, context.getFrameSelf(), Block.NULL_BLOCK);
        }
        catch (JumpException.ReturnJump rj) {
            throw this.newLocalJumpError(RubyLocalJumpError.Reason.RETURN, (IRubyObject)rj.getValue(), "unexpected return");
        }
        catch (JumpException.BreakJump bj) {
            throw this.newLocalJumpError(RubyLocalJumpError.Reason.BREAK, (IRubyObject)bj.getValue(), "unexpected break");
        }
        catch (JumpException.RedoJump rj2) {
            throw this.newLocalJumpError(RubyLocalJumpError.Reason.REDO, (IRubyObject)rj2.getValue(), "unexpected redo");
        }
        finally {
            context.postEvalScriptlet();
        }
    }
    
    public IRubyObject executeScript(final String script, final String filename) {
        byte[] bytes;
        try {
            bytes = script.getBytes(KCode.NONE.getKCode());
        }
        catch (UnsupportedEncodingException e) {
            bytes = script.getBytes();
        }
        final Node node = this.parseInline(new ByteArrayInputStream(bytes), filename, null);
        final ThreadContext context = this.getCurrentContext();
        final String oldFile = context.getFile();
        final int oldLine = context.getLine();
        try {
            context.setFileAndLine(node.getPosition());
            return this.runNormally(node);
        }
        finally {
            context.setFileAndLine(oldFile, oldLine);
        }
    }
    
    public void runFromMain(final InputStream inputStream, final String filename) {
        final IAccessor d = new ValueAccessor(this.newString(filename));
        this.getGlobalVariables().define("$PROGRAM_NAME", d);
        this.getGlobalVariables().define("$0", d);
        for (final Map.Entry entry : this.config.getOptionGlobals().entrySet()) {
            final Object value = entry.getValue();
            IRubyObject varvalue;
            if (value != null) {
                varvalue = this.newString(value.toString());
            }
            else {
                varvalue = this.getTrue();
            }
            this.getGlobalVariables().set("$" + entry.getKey().toString(), varvalue);
        }
        if (!filename.endsWith(".class")) {
            final Node scriptNode = this.parseFromMain(inputStream, filename);
            final ThreadContext context = this.getCurrentContext();
            final String oldFile = context.getFile();
            final int oldLine = context.getLine();
            try {
                context.setFileAndLine(scriptNode.getPosition());
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
            return;
        }
        final Script script = CompiledScriptLoader.loadScriptFromFile(this, inputStream, filename);
        if (script == null) {
            throw new MainExitException(1, "error: .class file specified is not a compiled JRuby script");
        }
        script.setFilename(filename);
        this.runScript(script);
    }
    
    public Node parseFromMain(final InputStream inputStream, final String filename) {
        if (this.config.isInlineScript()) {
            return this.parseInline(inputStream, filename, this.getCurrentContext().getCurrentScope());
        }
        return this.parseFile(inputStream, filename, this.getCurrentContext().getCurrentScope());
    }
    
    @Deprecated
    public IRubyObject runWithGetsLoop(final Node scriptNode, final boolean printing, final boolean processLineEnds, final boolean split, final boolean unused) {
        return this.runWithGetsLoop(scriptNode, printing, processLineEnds, split);
    }
    
    public IRubyObject runWithGetsLoop(final Node scriptNode, final boolean printing, final boolean processLineEnds, final boolean split) {
        final ThreadContext context = this.getCurrentContext();
        Script script = null;
        final boolean compile = this.getInstanceConfig().getCompileMode().shouldPrecompileCLI();
        if (compile) {
            script = this.tryCompile(scriptNode);
            if (compile && script == null) {
                return this.getNil();
            }
        }
        if (processLineEnds) {
            this.getGlobalVariables().set("$\\", this.getGlobalVariables().get("$/"));
        }
        RuntimeHelpers.preLoad(context, ((RootNode)scriptNode).getStaticScope().getVariables());
        try {
        Label_0106_Outer:
            while (RubyKernel.gets(context, this.getTopSelf(), IRubyObject.NULL_ARRAY).isTrue()) {
                while (true) {
                    try {
                        if (processLineEnds) {
                            this.getGlobalVariables().get("$_").callMethod(context, "chop!");
                        }
                        if (split) {
                            this.getGlobalVariables().set("$F", this.getGlobalVariables().get("$_").callMethod(context, "split"));
                        }
                        if (script != null) {
                            this.runScriptBody(script);
                        }
                        else {
                            this.runInterpreterBody(scriptNode);
                        }
                        if (!printing) {
                            continue Label_0106_Outer;
                        }
                        RubyKernel.print(context, this.getKernel(), new IRubyObject[] { this.getGlobalVariables().get("$_") });
                        continue Label_0106_Outer;
                    }
                    catch (JumpException.RedoJump rj) {
                        continue;
                    }
                    catch (JumpException.NextJump nj) {
                        continue Label_0106_Outer;
                    }
                    catch (JumpException.BreakJump bj) {
                        return (IRubyObject)bj.getValue();
                    }
                    break;
                }
                break;
            }
        }
        finally {
            RuntimeHelpers.postLoad(context);
        }
        return this.getNil();
    }
    
    @Deprecated
    public IRubyObject runNormally(final Node scriptNode, final boolean unused) {
        return this.runNormally(scriptNode);
    }
    
    public IRubyObject runNormally(final Node scriptNode) {
        Script script = null;
        final boolean compile = this.getInstanceConfig().getCompileMode().shouldPrecompileCLI();
        final boolean forceCompile = this.getInstanceConfig().getCompileMode().shouldPrecompileAll();
        if (compile) {
            script = this.tryCompile(scriptNode);
            if (forceCompile && script == null) {
                return this.getNil();
            }
        }
        if (script == null) {
            if (this.config.isShowBytecode()) {
                System.err.print("error: bytecode printing only works with JVM bytecode");
            }
            return this.runInterpreter(scriptNode);
        }
        if (this.config.isShowBytecode()) {
            return this.nilObject;
        }
        return this.runScript(script);
    }
    
    private Script tryCompile(final Node node) {
        return this.tryCompile(node, new JRubyClassLoader(this.getJRubyClassLoader()));
    }
    
    private Script tryCompile(final Node node, final JRubyClassLoader classLoader) {
        Script script = null;
        try {
            final String filename = node.getPosition().getFile();
            final String classname = JavaNameMangler.mangledFilenameForStartupClasspath(filename);
            final ASTInspector inspector = new ASTInspector();
            inspector.inspect(node);
            final StandardASMCompiler asmCompiler = new StandardASMCompiler(classname, filename);
            final ASTCompiler compiler = this.config.newCompiler();
            if (this.config.isShowBytecode()) {
                compiler.compileRoot(node, asmCompiler, inspector, false, false);
                asmCompiler.dumpClass(System.out);
            }
            else {
                compiler.compileRoot(node, asmCompiler, inspector, true, false);
            }
            script = (Script)asmCompiler.loadClass(classLoader).newInstance();
            if (this.config.isJitLogging()) {
                System.err.println("compiled: " + node.getPosition().getFile());
            }
        }
        catch (NotCompilableException nce) {
            if (this.config.isJitLoggingVerbose() || this.config.isDebug()) {
                System.err.println("Error -- Not compileable: " + nce.getMessage());
                nce.printStackTrace();
            }
            else {
                System.err.println("Error, could not compile; pass -d or -J-Djruby.jit.logging.verbose=true for more details");
            }
        }
        catch (ClassNotFoundException e) {
            if (this.config.isJitLoggingVerbose() || this.config.isDebug()) {
                System.err.println("Error -- Not compileable: " + e.getMessage());
                e.printStackTrace();
            }
            else {
                System.err.println("Error, could not compile; pass -d or -J-Djruby.jit.logging.verbose=true for more details");
            }
        }
        catch (InstantiationException e2) {
            if (this.config.isJitLoggingVerbose() || this.config.isDebug()) {
                System.err.println("Error -- Not compileable: " + e2.getMessage());
                e2.printStackTrace();
            }
            else {
                System.err.println("Error, could not compile; pass -d or -J-Djruby.jit.logging.verbose=true for more details");
            }
        }
        catch (IllegalAccessException e3) {
            if (this.config.isJitLoggingVerbose() || this.config.isDebug()) {
                System.err.println("Error -- Not compileable: " + e3.getMessage());
                e3.printStackTrace();
            }
            else {
                System.err.println("Error, could not compile; pass -d or -J-Djruby.jit.logging.verbose=true for more details");
            }
        }
        catch (Throwable t) {
            if (this.config.isJitLoggingVerbose() || this.config.isDebug()) {
                System.err.println("could not compile: " + node.getPosition().getFile() + " because of: \"" + t.getMessage() + "\"");
                t.printStackTrace();
            }
            else {
                System.err.println("Error, could not compile; pass -d or -J-Djruby.jit.logging.verbose=true for more details");
            }
        }
        return script;
    }
    
    private IRubyObject runScript(final Script script) {
        final ThreadContext context = this.getCurrentContext();
        try {
            return script.load(context, context.getFrameSelf(), IRubyObject.NULL_ARRAY, Block.NULL_BLOCK);
        }
        catch (JumpException.ReturnJump rj) {
            return (IRubyObject)rj.getValue();
        }
    }
    
    private IRubyObject runScriptBody(final Script script) {
        final ThreadContext context = this.getCurrentContext();
        try {
            return script.__file__(context, context.getFrameSelf(), Block.NULL_BLOCK);
        }
        catch (JumpException.ReturnJump rj) {
            return (IRubyObject)rj.getValue();
        }
    }
    
    public IRubyObject runInterpreter(final Node scriptNode) {
        final ThreadContext context = this.getCurrentContext();
        assert scriptNode != null : "scriptNode is not null";
        try {
            return scriptNode.interpret(this, context, this.getTopSelf(), Block.NULL_BLOCK);
        }
        catch (JumpException.ReturnJump rj) {
            return (IRubyObject)rj.getValue();
        }
    }
    
    public IRubyObject runInterpreterBody(final Node scriptNode) {
        final ThreadContext context = this.getCurrentContext();
        assert scriptNode != null : "scriptNode is not null";
        assert scriptNode instanceof RootNode;
        try {
            return ((RootNode)scriptNode).interpret(this, context, this.getTopSelf(), Block.NULL_BLOCK);
        }
        catch (JumpException.ReturnJump rj) {
            return (IRubyObject)rj.getValue();
        }
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
    
    public RubyModule getModule(final String name) {
        return (RubyModule)this.objectClass.getConstantAt(name);
    }
    
    public RubyModule fastGetModule(final String internedName) {
        return (RubyModule)this.objectClass.fastGetConstantAt(internedName);
    }
    
    public RubyClass getClass(final String name) {
        return this.objectClass.getClass(name);
    }
    
    public RubyClass fastGetClass(final String internedName) {
        return this.objectClass.fastGetClass(internedName);
    }
    
    public RubyClass defineClass(final String name, final RubyClass superClass, final ObjectAllocator allocator) {
        return this.defineClassUnder(name, superClass, allocator, this.objectClass);
    }
    
    public RubyClass defineClass(final String name, final RubyClass superClass, final ObjectAllocator allocator, final CallSite[] callSites) {
        return this.defineClassUnder(name, superClass, allocator, this.objectClass, callSites);
    }
    
    public RubyClass defineClassUnder(final String name, final RubyClass superClass, final ObjectAllocator allocator, final RubyModule parent) {
        return this.defineClassUnder(name, superClass, allocator, parent, null);
    }
    
    public RubyClass defineClassUnder(final String name, RubyClass superClass, final ObjectAllocator allocator, final RubyModule parent, final CallSite[] callSites) {
        final IRubyObject classObj = parent.getConstantAt(name);
        if (classObj == null) {
            final boolean parentIsObject = parent == this.objectClass;
            if (superClass == null) {
                final String className = parentIsObject ? name : (parent.getName() + "::" + name);
                this.warnings.warn(IRubyWarnings.ID.NO_SUPER_CLASS, "no super class for `" + className + "', Object assumed", className);
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
    
    public RubyModule defineModule(final String name) {
        return this.defineModuleUnder(name, this.objectClass);
    }
    
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
        else {
            if (this.getSafeLevel() >= 4) {
                throw this.newSecurityError("Extending module prohibited.");
            }
            if (!module.isModule()) {
                throw this.newTypeError(name + " is not a Module");
            }
        }
        return (RubyModule)module;
    }
    
    public int getSafeLevel() {
        return this.safeLevel;
    }
    
    public void setSafeLevel(final int safeLevel) {
        this.safeLevel = safeLevel;
    }
    
    public KCode getKCode() {
        return this.kcode;
    }
    
    public void setKCode(final KCode kcode) {
        this.kcode = kcode;
    }
    
    public void secure(final int level) {
        if (level <= this.safeLevel) {
            throw this.newSecurityError("Insecure operation '" + this.getCurrentContext().getFrameName() + "' at level " + this.safeLevel);
        }
    }
    
    public void checkSafeString(final IRubyObject object) {
        if (this.getSafeLevel() > 0 && object.isTaint()) {
            final ThreadContext tc = this.getCurrentContext();
            if (tc.getFrameName() != null) {
                throw this.newSecurityError("Insecure operation - " + tc.getFrameName());
            }
            throw this.newSecurityError("Insecure operation: -r");
        }
        else {
            this.secure(4);
            if (!(object instanceof RubyString)) {
                throw this.newTypeError("wrong argument type " + object.getMetaClass().getName() + " (expected String)");
            }
        }
    }
    
    public void defineGlobalConstant(final String name, final IRubyObject value) {
        this.objectClass.defineConstant(name, value);
    }
    
    public boolean isClassDefined(final String name) {
        return this.getModule(name) != null;
    }
    
    private void init() {
        final ThreadContext tc = this.getCurrentContext();
        this.safeLevel = this.config.getSafeLevel();
        this.loadService = this.config.createLoadService(this);
        this.posix = POSIXFactory.getPOSIX(new JRubyPOSIXHandler(this), RubyInstanceConfig.nativeEnabled);
        this.javaSupport = new JavaSupport(this);
        if (RubyInstanceConfig.POOLING_ENABLED) {
            this.executor = new ThreadPoolExecutor(RubyInstanceConfig.POOL_MIN, RubyInstanceConfig.POOL_MAX, RubyInstanceConfig.POOL_TTL, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new DaemonThreadFactory());
        }
        this.initRoot();
        tc.prepareTopLevel(this.objectClass, this.topSelf);
        this.bootstrap();
        (this.dummyClass = new RubyClass(this, this.classClass)).freeze(tc);
        RubyGlobal.createGlobals(tc, this);
        this.getLoadService().init(this.config.loadPaths());
        this.initBuiltins();
        for (final String scriptName : this.config.requiredLibraries()) {
            RubyKernel.require(this.getTopSelf(), this.newString(scriptName), Block.NULL_BLOCK);
        }
    }
    
    private void bootstrap() {
        this.initCore();
        this.initExceptions();
    }
    
    private void initRoot() {
        final boolean oneNine = this.is1_9();
        if (oneNine) {
            this.basicObjectClass = RubyClass.createBootstrapClass(this, "BasicObject", null, RubyBasicObject.OBJECT_ALLOCATOR);
            this.objectClass = RubyClass.createBootstrapClass(this, "Object", this.basicObjectClass, RubyObject.OBJECT_ALLOCATOR);
        }
        else {
            this.objectClass = RubyClass.createBootstrapClass(this, "Object", null, RubyObject.OBJECT_ALLOCATOR);
        }
        this.moduleClass = RubyClass.createBootstrapClass(this, "Module", this.objectClass, RubyModule.MODULE_ALLOCATOR);
        this.classClass = RubyClass.createBootstrapClass(this, "Class", this.moduleClass, RubyClass.CLASS_ALLOCATOR);
        if (oneNine) {
            this.basicObjectClass.setMetaClass(this.classClass);
        }
        this.objectClass.setMetaClass(this.classClass);
        this.moduleClass.setMetaClass(this.classClass);
        this.classClass.setMetaClass(this.classClass);
        if (oneNine) {
            this.basicObjectClass.makeMetaClass(this.classClass);
        }
        RubyClass metaClass = this.objectClass.makeMetaClass(this.classClass);
        metaClass = this.moduleClass.makeMetaClass(metaClass);
        metaClass = this.classClass.makeMetaClass(metaClass);
        if (oneNine) {
            RubyBasicObject.createBasicObjectClass(this, this.basicObjectClass);
        }
        RubyObject.createObjectClass(this, this.objectClass);
        RubyModule.createModuleClass(this, this.moduleClass);
        RubyClass.createClassClass(this, this.classClass);
        if (oneNine) {
            this.objectClass.setConstant("BasicObject", this.basicObjectClass);
        }
        this.objectClass.setConstant("Object", this.objectClass);
        this.objectClass.setConstant("Class", this.classClass);
        this.objectClass.setConstant("Module", this.moduleClass);
        RubyKernel.createKernelModule(this);
        this.objectClass.includeModule(this.kernelModule);
        this.topSelf = TopSelfFactory.createTopSelf(this);
    }
    
    private void initCore() {
        RubyNil.createNilClass(this);
        RubyBoolean.createFalseClass(this);
        RubyBoolean.createTrueClass(this);
        this.nilObject = new RubyNil(this);
        for (int i = 0; i < 128; ++i) {
            this.nilPrefilledArray[i] = this.nilObject;
        }
        this.singleNilArray = new IRubyObject[] { this.nilObject };
        this.falseObject = new RubyBoolean(this, false);
        this.trueObject = new RubyBoolean(this, true);
        if (this.profile.allowClass("Data")) {
            this.defineClass("Data", this.objectClass, ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        }
        RubyComparable.createComparable(this);
        RubyEnumerable.createEnumerableModule(this);
        RubyString.createStringClass(this);
        if (this.is1_9()) {
            RubyEncoding.createEncodingClass(this);
            RubyConverter.createConverterClass(this);
            this.encodingService = new EncodingService(this);
        }
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
        if (!this.is1_9() && this.profile.allowModule("Precision")) {
            RubyPrecision.createPrecisionModule(this);
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
        if (this.is1_9()) {
            if (this.profile.allowClass("Complex")) {
                RubyComplex.createComplexClass(this);
            }
            if (this.profile.allowClass("Rational")) {
                RubyRational.createRationalClass(this);
            }
        }
        if (this.profile.allowClass("Hash")) {
            RubyHash.createHashClass(this);
        }
        if (this.profile.allowClass("Array")) {
            RubyArray.createArrayClass(this);
        }
        if (this.profile.allowClass("Float")) {
            RubyFloat.createFloatClass(this);
        }
        if (this.profile.allowClass("Bignum")) {
            RubyBignum.createBignumClass(this);
        }
        this.ioClass = RubyIO.createIOClass(this);
        if (this.profile.allowClass("Struct")) {
            RubyStruct.createStructClass(this);
        }
        if (this.profile.allowClass("Tms")) {
            this.tmsStruct = RubyStruct.newInstance(this.structClass, new IRubyObject[] { this.newString("Tms"), this.newSymbol("utime"), this.newSymbol("stime"), this.newSymbol("cutime"), this.newSymbol("cstime") }, Block.NULL_BLOCK);
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
        if (!isSecurityRestricted() && this.profile.allowModule("Signal")) {
            RubySignal.createSignal(this);
        }
        if (this.profile.allowClass("Continuation")) {
            RubyContinuation.createContinuation(this);
        }
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
        this.signalException = this.defineClassIfAllowed("SignalException", this.exceptionClass);
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
        this.interrupt = this.defineClassIfAllowed("Interrupt", this.signalException);
        this.typeError = this.defineClassIfAllowed("TypeError", this.standardError);
        this.argumentError = this.defineClassIfAllowed("ArgumentError", this.standardError);
        this.indexError = this.defineClassIfAllowed("IndexError", this.standardError);
        if (this.is1_9()) {
            this.stopIteration = this.defineClassIfAllowed("StopIteration", this.indexError);
        }
        this.syntaxError = this.defineClassIfAllowed("SyntaxError", this.scriptError);
        this.loadError = this.defineClassIfAllowed("LoadError", this.scriptError);
        this.notImplementedError = this.defineClassIfAllowed("NotImplementedError", this.scriptError);
        this.securityError = this.defineClassIfAllowed("SecurityError", this.standardError);
        this.noMemoryError = this.defineClassIfAllowed("NoMemoryError", this.exceptionClass);
        this.regexpError = this.defineClassIfAllowed("RegexpError", this.standardError);
        this.eofError = this.defineClassIfAllowed("EOFError", this.ioError);
        this.threadError = this.defineClassIfAllowed("ThreadError", this.standardError);
        this.concurrencyError = this.defineClassIfAllowed("ConcurrencyError", this.threadError);
        this.systemStackError = this.defineClassIfAllowed("SystemStackError", this.standardError);
        this.zeroDivisionError = this.defineClassIfAllowed("ZeroDivisionError", this.standardError);
        this.floatDomainError = this.defineClassIfAllowed("FloatDomainError", this.rangeError);
        if (this.is1_9() && this.profile.allowClass("EncodingError")) {
            this.encodingError = this.defineClass("EncodingError", this.standardError, this.standardError.getAllocator());
            this.encodingCompatibilityError = this.defineClassUnder("CompatibilityError", this.encodingError, this.encodingError.getAllocator(), this.encodingClass);
        }
        this.initErrno();
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
            for (final Constant c : Errno.values()) {
                final Errno e = (Errno)c;
                if (Character.isUpperCase(c.name().charAt(0))) {
                    this.createSysErr(c.value(), c.name());
                }
            }
        }
    }
    
    private void createSysErr(final int i, final String name) {
        if (this.profile.allowClass(name)) {
            final RubyClass errno = this.getErrno().defineClassUnder(name, this.systemCallError, this.systemCallError.getAllocator());
            this.errnos.put(i, errno);
            errno.defineConstant("Errno", this.newFixnum(i));
        }
    }
    
    private void initBuiltins() {
        this.addLazyBuiltin("java.rb", "java", "org.jruby.javasupport.Java");
        this.addLazyBuiltin("jruby.rb", "jruby", "org.jruby.libraries.JRubyLibrary");
        this.addLazyBuiltin("minijava.rb", "minijava", "org.jruby.java.MiniJava");
        this.addLazyBuiltin("jruby/ext.rb", "jruby/ext", "org.jruby.RubyJRuby$ExtLibrary");
        this.addLazyBuiltin("jruby/core_ext.rb", "jruby/ext", "org.jruby.RubyJRuby$CoreExtLibrary");
        this.addLazyBuiltin("jruby/type.rb", "jruby/type", "org.jruby.RubyJRuby$TypeLibrary");
        this.addLazyBuiltin("iconv.so", "iconv", "org.jruby.libraries.IConvLibrary");
        this.addLazyBuiltin("nkf.so", "nkf", "org.jruby.libraries.NKFLibrary");
        this.addLazyBuiltin("stringio.so", "stringio", "org.jruby.libraries.StringIOLibrary");
        this.addLazyBuiltin("strscan.so", "strscan", "org.jruby.libraries.StringScannerLibrary");
        this.addLazyBuiltin("zlib.so", "zlib", "org.jruby.libraries.ZlibLibrary");
        this.addLazyBuiltin("yaml_internal.rb", "yaml_internal", "org.jruby.libraries.YamlLibrary");
        this.addLazyBuiltin("enumerator.so", "enumerator", "org.jruby.libraries.EnumeratorLibrary");
        this.addLazyBuiltin("generator_internal.rb", "generator_internal", "org.jruby.ext.Generator$Service");
        this.addLazyBuiltin("readline.so", "readline", "org.jruby.ext.Readline$Service");
        this.addLazyBuiltin("thread.so", "thread", "org.jruby.libraries.ThreadLibrary");
        this.addLazyBuiltin("digest.so", "digest", "org.jruby.libraries.DigestLibrary");
        this.addLazyBuiltin("digest.rb", "digest", "org.jruby.libraries.DigestLibrary");
        this.addLazyBuiltin("digest/md5.so", "digest/md5", "org.jruby.libraries.DigestLibrary$MD5");
        this.addLazyBuiltin("digest/rmd160.so", "digest/rmd160", "org.jruby.libraries.DigestLibrary$RMD160");
        this.addLazyBuiltin("digest/sha1.so", "digest/sha1", "org.jruby.libraries.DigestLibrary$SHA1");
        this.addLazyBuiltin("digest/sha2.so", "digest/sha2", "org.jruby.libraries.DigestLibrary$SHA2");
        this.addLazyBuiltin("bigdecimal.so", "bigdecimal", "org.jruby.libraries.BigDecimalLibrary");
        this.addLazyBuiltin("io/wait.so", "io/wait", "org.jruby.libraries.IOWaitLibrary");
        this.addLazyBuiltin("etc.so", "etc", "org.jruby.libraries.EtcLibrary");
        this.addLazyBuiltin("weakref.rb", "weakref", "org.jruby.ext.WeakRef$WeakRefLibrary");
        this.addLazyBuiltin("timeout.rb", "timeout", "org.jruby.ext.Timeout");
        this.addLazyBuiltin("socket.so", "socket", "org.jruby.ext.socket.RubySocket$Service");
        this.addLazyBuiltin("rbconfig.rb", "rbconfig", "org.jruby.libraries.RbConfigLibrary");
        this.addLazyBuiltin("jruby/serialization.rb", "serialization", "org.jruby.libraries.JRubySerializationLibrary");
        this.addLazyBuiltin("ffi-internal.so", "ffi-internal", "org.jruby.ext.ffi.Factory$Service");
        this.addLazyBuiltin("tempfile.rb", "tempfile", "org.jruby.libraries.TempfileLibrary");
        this.addLazyBuiltin("fcntl.rb", "fcntl", "org.jruby.libraries.FcntlLibrary");
        if (RubyInstanceConfig.NATIVE_NET_PROTOCOL) {
            this.addLazyBuiltin("net/protocol.rb", "net/protocol", "org.jruby.libraries.NetProtocolBufferedIOLibrary");
        }
        if (this.is1_9()) {
            this.addLazyBuiltin("fiber.so", "fiber", "org.jruby.libraries.FiberLibrary");
        }
        this.addBuiltinIfAllowed("openssl.so", new Library() {
            public void load(final Ruby runtime, final boolean wrap) throws IOException {
                runtime.getLoadService().require("jruby/openssl/stub");
            }
        });
        final String[] arr$;
        final String[] builtins = arr$ = new String[] { "yaml", "yaml/syck", "jsignal" };
        for (final String library : arr$) {
            this.addBuiltinIfAllowed(library + ".rb", new BuiltinScript(library));
        }
        RubyKernel.autoload(this.topSelf, this.newSymbol("Java"), this.newString("java"));
        if (this.is1_9()) {
            this.getLoadService().require("builtin/prelude.rb");
            this.getLoadService().require("builtin/core_ext/symbol");
            this.getLoadService().require("enumerator");
        }
    }
    
    private void addLazyBuiltin(final String name, final String shortName, final String className) {
        this.addBuiltinIfAllowed(name, new LateLoadingLibrary(shortName, className, this.getJRubyClassLoader()));
    }
    
    private void addBuiltinIfAllowed(final String name, final Library lib) {
        if (this.profile.allowBuiltin(name)) {
            this.loadService.addBuiltinLibrary(name, lib);
        }
    }
    
    public Object getRespondToMethod() {
        return this.respondToMethod;
    }
    
    public void setRespondToMethod(final Object rtm) {
        this.respondToMethod = rtm;
    }
    
    public Object getObjectToYamlMethod() {
        return this.objectToYamlMethod;
    }
    
    void setObjectToYamlMethod(final Object otym) {
        this.objectToYamlMethod = otym;
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
    
    public void setDefaultMethodMissing(final DynamicMethod method) {
        this.defaultMethodMissing = method;
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
    
    public RubyClass getGenerator() {
        return this.generatorClass;
    }
    
    void setGenerator(final RubyClass generatorClass) {
        this.generatorClass = generatorClass;
    }
    
    public RubyClass getYielder() {
        return this.yielderClass;
    }
    
    void setYielder(final RubyClass yielderClass) {
        this.yielderClass = yielderClass;
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
    
    public IRubyObject getTmsStruct() {
        return this.tmsStruct;
    }
    
    void setTmsStruct(final RubyClass tmsStruct) {
        this.tmsStruct = tmsStruct;
    }
    
    public IRubyObject getPasswdStruct() {
        return this.passwdStruct;
    }
    
    void setPasswdStruct(final RubyClass passwdStruct) {
        this.passwdStruct = passwdStruct;
    }
    
    public IRubyObject getGroupStruct() {
        return this.groupStruct;
    }
    
    void setGroupStruct(final RubyClass groupStruct) {
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
    
    public RubyClass getEncodingError() {
        return this.encodingError;
    }
    
    public RubyClass getEncodingCompatibilityError() {
        return this.encodingCompatibilityError;
    }
    
    public RubyHash getCharsetMap() {
        if (this.charsetMap == null) {
            this.charsetMap = new RubyHash(this);
        }
        return this.charsetMap;
    }
    
    public IRubyObject getVerbose() {
        return this.verbose;
    }
    
    public boolean isVerbose() {
        return this.isVerbose;
    }
    
    public boolean warningsEnabled() {
        return this.warningsEnabled;
    }
    
    public void setVerbose(final IRubyObject verbose) {
        this.verbose = verbose;
        this.isVerbose = verbose.isTrue();
        this.warningsEnabled = !verbose.isNil();
    }
    
    public IRubyObject getDebug() {
        return this.debug;
    }
    
    public void setDebug(final IRubyObject debug) {
        this.debug = debug;
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
            this.jrubyClassLoader = new JRubyClassLoader(this.config.getLoader());
        }
        return this.jrubyClassLoader;
    }
    
    public void defineVariable(final GlobalVariable variable) {
        this.globalVariables.define(variable.name(), new IAccessor() {
            public IRubyObject getValue() {
                return variable.get();
            }
            
            public IRubyObject setValue(final IRubyObject newValue) {
                return variable.set(newValue);
            }
        });
    }
    
    public void defineReadonlyVariable(final String name, final IRubyObject value) {
        this.globalVariables.defineReadonly(name, new ValueAccessor(value));
    }
    
    public Node parseFile(final InputStream in, final String file, final DynamicScope scope, final int lineNumber) {
        if (this.parserStats != null) {
            this.parserStats.addLoadParse();
        }
        return this.parser.parse(file, in, scope, new ParserConfiguration(this.getKCode(), lineNumber, false, false, true, this.config));
    }
    
    public Node parseFile(final InputStream in, final String file, final DynamicScope scope) {
        return this.parseFile(in, file, scope, 0);
    }
    
    public Node parseInline(final InputStream in, final String file, final DynamicScope scope) {
        if (this.parserStats != null) {
            this.parserStats.addEvalParse();
        }
        return this.parser.parse(file, in, scope, new ParserConfiguration(this.getKCode(), 0, false, true, false, this.config));
    }
    
    public Node parseEval(final String content, final String file, final DynamicScope scope, final int lineNumber) {
        byte[] bytes;
        try {
            bytes = content.getBytes(KCode.NONE.getKCode());
        }
        catch (UnsupportedEncodingException e) {
            bytes = content.getBytes();
        }
        if (this.parserStats != null) {
            this.parserStats.addEvalParse();
        }
        return this.parser.parse(file, new ByteArrayInputStream(bytes), scope, new ParserConfiguration(this.getKCode(), lineNumber, false, false, true, this.config));
    }
    
    @Deprecated
    public Node parse(final String content, final String file, final DynamicScope scope, final int lineNumber, final boolean extraPositionInformation) {
        byte[] bytes;
        try {
            bytes = content.getBytes(KCode.NONE.getKCode());
        }
        catch (UnsupportedEncodingException e) {
            bytes = content.getBytes();
        }
        return this.parser.parse(file, new ByteArrayInputStream(bytes), scope, new ParserConfiguration(this.getKCode(), lineNumber, extraPositionInformation, false, true, this.config));
    }
    
    public Node parseEval(final ByteList content, final String file, final DynamicScope scope, final int lineNumber) {
        if (this.parserStats != null) {
            this.parserStats.addEvalParse();
        }
        return this.parser.parse(file, content, scope, new ParserConfiguration(this.getKCode(), lineNumber, false, false, true, this.config));
    }
    
    public Node parse(final ByteList content, final String file, final DynamicScope scope, final int lineNumber, final boolean extraPositionInformation) {
        if (this.parserStats != null) {
            this.parserStats.addJRubyModuleParse();
        }
        return this.parser.parse(file, content, scope, new ParserConfiguration(this.getKCode(), lineNumber, extraPositionInformation, false, true, this.config));
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
        RubyModule c = this.getObject();
        if (path.length() == 0 || path.charAt(0) == '#') {
            throw this.newTypeError("can't retrieve anonymous class " + path);
        }
        int pbeg = 0;
        int p = 0;
        final int l = path.length();
        while (p < l) {
            while (p < l && path.charAt(p) != ':') {
                ++p;
            }
            final String str = path.substring(pbeg, p);
            if (p < l && path.charAt(p) == ':') {
                if (p + 1 < l && path.charAt(p + 1) != ':') {
                    throw this.newTypeError("undefined class/module " + path.substring(pbeg, p));
                }
                p += 2;
                pbeg = p;
            }
            final IRubyObject cc = c.getConstant(str);
            if (!(cc instanceof RubyModule)) {
                throw this.newTypeError("" + path + " does not refer to class/module");
            }
            c = (RubyModule)cc;
        }
        return c;
    }
    
    public void printError(final RubyException excp) {
        if (excp == null || excp.isNil()) {
            return;
        }
        if (RubyException.TRACE_TYPE == 5) {
            this.printRubiniusTrace(excp);
            return;
        }
        final ThreadContext context = this.getCurrentContext();
        final IRubyObject backtrace = excp.callMethod(context, "backtrace");
        final PrintStream errorStream = this.getErrorStream();
        if (backtrace.isNil() || !(backtrace instanceof RubyArray)) {
            if (context.getFile() != null) {
                errorStream.print(context.getFile() + ":" + context.getLine());
            }
            else {
                errorStream.print(context.getLine());
            }
        }
        else if (((RubyArray)backtrace).getLength() == 0) {
            this.printErrorPos(context, errorStream);
        }
        else {
            final IRubyObject mesg = ((RubyArray)backtrace).first();
            if (mesg.isNil()) {
                this.printErrorPos(context, errorStream);
            }
            else {
                errorStream.print(mesg);
            }
        }
        final RubyClass type = excp.getMetaClass();
        String info = excp.toString();
        if (type == this.getRuntimeError() && (info == null || info.length() == 0)) {
            errorStream.print(": unhandled exception\n");
        }
        else {
            String path = type.getName();
            if (info.length() == 0) {
                errorStream.print(": " + path + '\n');
            }
            else {
                if (path.startsWith("#")) {
                    path = null;
                }
                String tail = null;
                if (info.indexOf("\n") != -1) {
                    tail = info.substring(info.indexOf("\n") + 1);
                    info = info.substring(0, info.indexOf("\n"));
                }
                errorStream.print(": " + info);
                if (path != null) {
                    errorStream.print(" (" + path + ")\n");
                }
                if (tail != null) {
                    errorStream.print(tail + '\n');
                }
            }
        }
        excp.printBacktrace(errorStream);
    }
    
    private void printRubiniusTrace(final RubyException exception) {
        final ThreadContext.RubyStackTraceElement[] frames = exception.getBacktraceFrames();
        final ArrayList firstParts = new ArrayList();
        int longestFirstPart = 0;
        for (final ThreadContext.RubyStackTraceElement frame : frames) {
            final String firstPart = frame.getClassName() + "#" + frame.getMethodName();
            if (firstPart.length() > longestFirstPart) {
                longestFirstPart = firstPart.length();
            }
            firstParts.add(firstPart);
        }
        final int center = longestFirstPart + 2 + 1;
        final StringBuffer buffer = new StringBuffer();
        buffer.append("An exception has occurred:\n").append("    ");
        if (exception.getMetaClass() == this.getRuntimeError() && exception.message(this.getCurrentContext()).toString().length() == 0) {
            buffer.append("No current exception (RuntimeError)");
        }
        else {
            buffer.append(exception.message(this.getCurrentContext()).toString());
        }
        buffer.append('\n').append('\n').append("Backtrace:\n");
        int i = 0;
        for (final ThreadContext.RubyStackTraceElement frame2 : frames) {
            final String firstPart2 = firstParts.get(i);
            final String secondPart = frame2.getFileName() + ":" + frame2.getLineNumber();
            buffer.append("  ");
            for (int j = 0; j < center - firstPart2.length(); ++j) {
                buffer.append(' ');
            }
            buffer.append(firstPart2);
            buffer.append(" at ");
            buffer.append(secondPart);
            buffer.append('\n');
            ++i;
        }
        final PrintStream errorStream = this.getErrorStream();
        errorStream.print(buffer.toString());
    }
    
    private void printErrorPos(final ThreadContext context, final PrintStream errorStream) {
        if (context.getFile() != null) {
            if (context.getFrameName() != null) {
                errorStream.print(context.getFile() + ":" + context.getLine());
                errorStream.print(":in '" + context.getFrameName() + '\'');
            }
            else if (context.getLine() != 0) {
                errorStream.print(context.getFile() + ":" + context.getLine());
            }
            else {
                errorStream.print(context.getFile());
            }
        }
    }
    
    public void loadFile(final String scriptName, final InputStream in, final boolean wrap) {
        final IRubyObject self = wrap ? TopSelfFactory.createTopSelf(this) : this.getTopSelf();
        final ThreadContext context = this.getCurrentContext();
        final String file = context.getFile();
        try {
            this.secure(4);
            context.setFile(scriptName);
            context.preNodeEval(this.objectClass, self, scriptName);
            this.parseFile(in, scriptName, null).interpret(this, context, self, Block.NULL_BLOCK);
        }
        catch (JumpException.ReturnJump rj) {}
        finally {
            context.postNodeEval();
            context.setFile(file);
        }
    }
    
    public void compileAndLoadFile(final String filename, final InputStream in, final boolean wrap) {
        final IRubyObject self = wrap ? TopSelfFactory.createTopSelf(this) : this.getTopSelf();
        final ThreadContext context = this.getCurrentContext();
        final String file = context.getFile();
        try {
            this.secure(4);
            context.setFile(filename);
            context.preNodeEval(this.objectClass, self, filename);
            final Node scriptNode = this.parseFile(in, filename, null);
            final Script script = this.tryCompile(scriptNode, new JRubyClassLoader(this.jrubyClassLoader));
            if (script == null) {
                System.err.println("Error, could not compile; pass -J-Djruby.jit.logging.verbose=true for more details");
            }
            this.runScript(script);
        }
        catch (JumpException.ReturnJump rj) {}
        finally {
            context.postNodeEval();
            context.setFile(file);
        }
    }
    
    public void loadScript(final Script script) {
        final IRubyObject self = this.getTopSelf();
        final ThreadContext context = this.getCurrentContext();
        try {
            this.secure(4);
            context.preNodeEval(this.objectClass, self);
            script.load(context, self, IRubyObject.NULL_ARRAY, Block.NULL_BLOCK);
        }
        catch (JumpException.ReturnJump rj) {}
        finally {
            context.postNodeEval();
        }
    }
    
    public void addEventHook(final EventHook hook) {
        this.eventHooks.add(hook);
        this.hasEventHooks = true;
    }
    
    public void removeEventHook(final EventHook hook) {
        this.eventHooks.remove(hook);
        this.hasEventHooks = !this.eventHooks.isEmpty();
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
        for (final EventHook eventHook : this.eventHooks) {
            if (eventHook.isInterestedInEvent(event)) {
                eventHook.event(context, event, file, line, name, type);
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
    
    public CallbackFactory callbackFactory(final Class<?> type) {
        return CallbackFactory.createFactory(this, type);
    }
    
    public IRubyObject pushExitBlock(final RubyProc proc) {
        this.atExitBlocks.push(proc);
        return proc;
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
        int status = 0;
        while (!this.atExitBlocks.empty()) {
            final RubyProc proc = this.atExitBlocks.pop();
            try {
                proc.call(this.getCurrentContext(), IRubyObject.NULL_ARRAY);
            }
            catch (RaiseException rj) {
                final RubyException raisedException = rj.getException();
                if (!this.getSystemExit().isInstance(raisedException)) {
                    status = 1;
                    this.printError(raisedException);
                }
                else {
                    final IRubyObject statusObj = raisedException.callMethod(this.getCurrentContext(), "status");
                    if (statusObj == null || statusObj.isNil()) {
                        continue;
                    }
                    status = RubyNumeric.fix2int(statusObj);
                }
            }
        }
        if (this.finalizers != null) {
            synchronized (this.finalizers) {
                final Iterator<Finalizable> finalIter = new ArrayList<Finalizable>(this.finalizers.keySet()).iterator();
                while (finalIter.hasNext()) {
                    final Finalizable f = finalIter.next();
                    if (f != null) {
                        f.finalize();
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
                        f.finalize();
                    }
                    finalIter.remove();
                }
            }
        }
        this.getThreadService().disposeCurrentThread();
        this.getBeanManager().unregisterCompiler();
        this.getBeanManager().unregisterConfig();
        this.getBeanManager().unregisterParserStats();
        this.getBeanManager().unregisterClassCache();
        this.getBeanManager().unregisterMethodCache();
        if (status != 0) {
            throw this.newSystemExit(status);
        }
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
        return RubyFixnum.newFixnum(this, value.value());
    }
    
    public RubyFloat newFloat(final double value) {
        return RubyFloat.newFloat(this, value);
    }
    
    public RubyNumeric newNumeric() {
        return RubyNumeric.newNumeric(this);
    }
    
    public RubyProc newProc(final Block.Type type, final Block block) {
        if (type != Block.Type.LAMBDA && block.getProcObject() != null) {
            return block.getProcObject();
        }
        final RubyProc proc = RubyProc.newProc(this, type);
        proc.callInit(IRubyObject.NULL_ARRAY, block);
        return proc;
    }
    
    public RubyProc newBlockPassProc(final Block.Type type, final Block block) {
        if (type != Block.Type.LAMBDA && block.getProcObject() != null) {
            return block.getProcObject();
        }
        final RubyProc proc = RubyProc.newProc(this, type);
        proc.initialize(this.getCurrentContext(), block);
        return proc;
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
        return this.newRaiseException(this.getArgumentError(), "wrong # of arguments(" + got + " for " + expected + ")");
    }
    
    public RaiseException newErrnoEBADFError() {
        return this.newRaiseException(this.getErrno().fastGetClass("EBADF"), "Bad file descriptor");
    }
    
    public RaiseException newErrnoENOPROTOOPTError() {
        return this.newRaiseException(this.getErrno().fastGetClass("ENOPROTOOPT"), "Protocol not available");
    }
    
    public RaiseException newErrnoEPIPEError() {
        return this.newRaiseException(this.getErrno().fastGetClass("EPIPE"), "Broken pipe");
    }
    
    public RaiseException newErrnoECONNREFUSEDError() {
        return this.newRaiseException(this.getErrno().fastGetClass("ECONNREFUSED"), "Connection refused");
    }
    
    public RaiseException newErrnoECONNRESETError() {
        return this.newRaiseException(this.getErrno().fastGetClass("ECONNRESET"), "Connection reset by peer");
    }
    
    public RaiseException newErrnoEADDRINUSEError() {
        return this.newRaiseException(this.getErrno().fastGetClass("EADDRINUSE"), "Address in use");
    }
    
    public RaiseException newErrnoEINVALError() {
        return this.newRaiseException(this.getErrno().fastGetClass("EINVAL"), "Invalid file");
    }
    
    public RaiseException newErrnoENOENTError() {
        return this.newRaiseException(this.getErrno().fastGetClass("ENOENT"), "File not found");
    }
    
    public RaiseException newErrnoEACCESError(final String message) {
        return this.newRaiseException(this.getErrno().fastGetClass("EACCES"), message);
    }
    
    public RaiseException newErrnoEAGAINError(final String message) {
        return this.newRaiseException(this.getErrno().fastGetClass("EAGAIN"), message);
    }
    
    public RaiseException newErrnoEISDirError(final String message) {
        return this.newRaiseException(this.getErrno().fastGetClass("EISDIR"), message);
    }
    
    public RaiseException newErrnoEISDirError() {
        return this.newErrnoEISDirError("Is a directory");
    }
    
    public RaiseException newErrnoESPIPEError() {
        return this.newRaiseException(this.getErrno().fastGetClass("ESPIPE"), "Illegal seek");
    }
    
    public RaiseException newErrnoEBADFError(final String message) {
        return this.newRaiseException(this.getErrno().fastGetClass("EBADF"), message);
    }
    
    public RaiseException newErrnoEINVALError(final String message) {
        return this.newRaiseException(this.getErrno().fastGetClass("EINVAL"), message);
    }
    
    public RaiseException newErrnoENOTDIRError(final String message) {
        return this.newRaiseException(this.getErrno().fastGetClass("ENOTDIR"), message);
    }
    
    public RaiseException newErrnoENOTSOCKError(final String message) {
        return this.newRaiseException(this.getErrno().fastGetClass("ENOTSOCK"), message);
    }
    
    public RaiseException newErrnoENOENTError(final String message) {
        return this.newRaiseException(this.getErrno().fastGetClass("ENOENT"), message);
    }
    
    public RaiseException newErrnoESPIPEError(final String message) {
        return this.newRaiseException(this.getErrno().fastGetClass("ESPIPE"), message);
    }
    
    public RaiseException newErrnoEEXISTError(final String message) {
        return this.newRaiseException(this.getErrno().fastGetClass("EEXIST"), message);
    }
    
    public RaiseException newErrnoEDOMError(final String message) {
        return this.newRaiseException(this.getErrno().fastGetClass("EDOM"), "Domain error - " + message);
    }
    
    public RaiseException newErrnoECHILDError() {
        return this.newRaiseException(this.getErrno().fastGetClass("ECHILD"), "No child processes");
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
    
    public RaiseException newRangeError(final String message) {
        return this.newRaiseException(this.getRangeError(), message);
    }
    
    public RaiseException newNotImplementedError(final String message) {
        return this.newRaiseException(this.getNotImplementedError(), message);
    }
    
    public RaiseException newInvalidEncoding(final String message) {
        return this.newRaiseException(this.fastGetClass("Iconv").fastGetClass("InvalidEncoding"), message);
    }
    
    public RaiseException newNoMethodError(final String message, final String name, final IRubyObject args) {
        return new RaiseException(new RubyNoMethodError(this, this.getNoMethodError(), message, name, args), true);
    }
    
    public RaiseException newNameError(final String message, final String name) {
        return this.newNameError(message, name, null);
    }
    
    public RaiseException newNameError(final String message, final String name, final Throwable origException) {
        return this.newNameError(message, name, origException, true);
    }
    
    public RaiseException newNameError(final String message, final String name, final Throwable origException, final boolean printWhenVerbose) {
        if (printWhenVerbose && origException != null && this.isVerbose()) {
            origException.printStackTrace(this.getErrorStream());
        }
        return new RaiseException(new RubyNameError(this, this.getNameError(), message, name), true);
    }
    
    public RaiseException newLocalJumpError(final RubyLocalJumpError.Reason reason, final IRubyObject exitValue, final String message) {
        return new RaiseException(new RubyLocalJumpError(this, this.getLocalJumpError(), message, reason, exitValue), true);
    }
    
    public RaiseException newLocalJumpErrorNoBlock() {
        return this.newLocalJumpError(RubyLocalJumpError.Reason.NOREASON, this.getNil(), "no block given");
    }
    
    public RaiseException newRedoLocalJumpError() {
        return new RaiseException(new RubyLocalJumpError(this, this.getLocalJumpError(), "unexpected redo", RubyLocalJumpError.Reason.REDO, this.getNil()), true);
    }
    
    public RaiseException newLoadError(final String message) {
        return this.newRaiseException(this.getLoadError(), message);
    }
    
    public RaiseException newFrozenError(final String objectType) {
        return this.newRaiseException(this.is1_9() ? this.getRuntimeError() : this.getTypeError(), "can't modify frozen " + objectType);
    }
    
    public RaiseException newSystemStackError(final String message) {
        return this.newRaiseException(this.getSystemStackError(), message);
    }
    
    public RaiseException newSystemStackError(final String message, final StackOverflowError soe) {
        if (this.getDebug().isTrue()) {
            soe.printStackTrace(this.getInstanceConfig().getError());
        }
        return this.newRaiseException(this.getSystemStackError(), message);
    }
    
    public RaiseException newSystemExit(final int status) {
        return new RaiseException(RubySystemExit.newInstance(this, status));
    }
    
    public RaiseException newIOError(final String message) {
        return this.newRaiseException(this.getIOError(), message);
    }
    
    public RaiseException newStandardError(final String message) {
        return this.newRaiseException(this.getStandardError(), message);
    }
    
    public RaiseException newIOErrorFromException(final IOException ioe) {
        if (ioe.getMessage() == null) {
            return this.newRaiseException(this.getIOError(), "IO Error");
        }
        if (ioe.getMessage().equals("Broken pipe")) {
            throw this.newErrnoEPIPEError();
        }
        if (ioe.getMessage().equals("Connection reset by peer")) {
            throw this.newErrnoECONNRESETError();
        }
        return this.newRaiseException(this.getIOError(), ioe.getMessage());
    }
    
    public RaiseException newTypeError(final IRubyObject receivedObject, final RubyClass expectedType) {
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
    
    public RaiseException newEncodingError(final String message) {
        return this.newRaiseException(this.getEncodingError(), message);
    }
    
    public RaiseException newEncodingCompatibilityError(final String message) {
        return this.newRaiseException(this.getEncodingCompatibilityError(), message);
    }
    
    private RaiseException newRaiseException(final RubyClass exceptionClass, final String message) {
        final RaiseException re = new RaiseException(this, exceptionClass, message, true);
        return re;
    }
    
    public RubySymbol.SymbolTable getSymbolTable() {
        return this.symbolTable;
    }
    
    public void setStackTraces(final int stackTraces) {
        this.stackTraces = stackTraces;
    }
    
    public int getStackTraces() {
        return this.stackTraces;
    }
    
    public void setRandomSeed(final long randomSeed) {
        this.randomSeed = randomSeed;
    }
    
    public long getRandomSeed() {
        return this.randomSeed;
    }
    
    public Random getRandom() {
        return this.random;
    }
    
    public ObjectSpace getObjectSpace() {
        return this.objectSpace;
    }
    
    public Map<Integer, WeakDescriptorReference> getDescriptors() {
        return this.descriptors;
    }
    
    private void cleanDescriptors() {
        Reference reference;
        while ((reference = this.descriptorQueue.poll()) != null) {
            final int fileno = ((WeakDescriptorReference)reference).getFileno();
            this.descriptors.remove(fileno);
        }
    }
    
    public void registerDescriptor(final ChannelDescriptor descriptor, final boolean isRetained) {
        this.cleanDescriptors();
        final int fileno = descriptor.getFileno();
        final Integer filenoKey = new Integer(fileno);
        this.descriptors.put(filenoKey, new WeakDescriptorReference(descriptor, this.descriptorQueue));
        if (isRetained) {
            this.retainedDescriptors.put(filenoKey, descriptor);
        }
    }
    
    public void registerDescriptor(final ChannelDescriptor descriptor) {
        this.registerDescriptor(descriptor, false);
    }
    
    public void unregisterDescriptor(final int aFileno) {
        this.cleanDescriptors();
        final Integer aFilenoKey = new Integer(aFileno);
        this.descriptors.remove(aFilenoKey);
        this.retainedDescriptors.remove(aFilenoKey);
    }
    
    public ChannelDescriptor getDescriptorByFileno(final int aFileno) {
        this.cleanDescriptors();
        final Reference reference = this.descriptors.get(new Integer(aFileno));
        if (reference == null) {
            return null;
        }
        return reference.get();
    }
    
    public long incrementRandomSeedSequence() {
        return this.randomSeedSequence++;
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
    
    void setObjectSpaceEnabled(final boolean objectSpaceEnabled) {
        this.objectSpaceEnabled = objectSpaceEnabled;
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
    
    public boolean is1_9() {
        return this.is1_9;
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
    
    public Set<Script> getJittedMethods() {
        return this.jittedMethods;
    }
    
    public ExecutorService getExecutor() {
        return this.executor;
    }
    
    public Map<String, DateTimeZone> getTimezoneCache() {
        return this.timeZoneCache;
    }
    
    public int getConstantGeneration() {
        return this.constantGeneration;
    }
    
    public synchronized void incrementConstantGeneration() {
        ++this.constantGeneration;
    }
    
    public <E extends Enum<E>> void loadConstantSet(final RubyModule module, final Class<E> enumClass) {
        for (final E e : EnumSet.allOf(enumClass)) {
            final Constant c = (Constant)e;
            if (Character.isUpperCase(c.name().charAt(0))) {
                module.fastSetConstant(c.name(), this.newFixnum(c.value()));
            }
        }
    }
    
    public void loadConstantSet(final RubyModule module, final String constantSetName) {
        for (final Constant c : ConstantSet.getConstantSet(constantSetName)) {
            if (Character.isUpperCase(c.name().charAt(0))) {
                module.fastSetConstant(c.name(), this.newFixnum(c.value()));
            }
        }
    }
    
    public Object getHierarchyLock() {
        return this.hierarchyLock;
    }
    
    static {
        Ruby.currentRuntime = new ThreadLocal<Ruby>();
        Ruby.securityRestricted = false;
        if (SafePropertyAccessor.isSecurityProtected("jruby.reflection")) {
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
    }
    
    public class CallTraceFuncHook extends EventHook
    {
        private RubyProc traceFunc;
        
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
                    type = Ruby.this.getFalse();
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
            return true;
        }
    }
    
    private class WeakDescriptorReference extends WeakReference
    {
        private int fileno;
        
        public WeakDescriptorReference(final ChannelDescriptor descriptor, final ReferenceQueue queue) {
            super(descriptor, queue);
            this.fileno = descriptor.getFileno();
        }
        
        public int getFileno() {
            return this.fileno;
        }
    }
}
