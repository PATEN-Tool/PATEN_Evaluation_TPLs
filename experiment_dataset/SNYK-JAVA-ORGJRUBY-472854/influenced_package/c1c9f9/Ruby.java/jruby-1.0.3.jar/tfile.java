// 
// Decompiled by Procyon v0.5.36
// 

package org.jruby;

import org.jruby.util.NormalizedFile;
import java.util.IdentityHashMap;
import org.jruby.exceptions.RaiseException;
import org.jruby.util.ByteList;
import java.util.Iterator;
import java.util.Collection;
import java.util.ArrayList;
import java.util.WeakHashMap;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.EventHook;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.StringReader;
import org.jruby.util.IOInputStream;
import java.io.OutputStream;
import org.jruby.util.IOOutputStream;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.GlobalVariable;
import org.jruby.runtime.IAccessor;
import org.jruby.internal.runtime.ValueAccessor;
import org.jruby.ext.LateLoadingLibrary;
import org.jruby.libraries.RbConfigLibrary;
import org.jruby.ext.socket.RubySocket;
import java.io.IOException;
import org.jruby.javasupport.Java;
import org.jruby.runtime.load.Library;
import org.jruby.util.collections.SinglyLinkedList;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.ast.executable.YARVCompiledRunner;
import org.jruby.ast.RootNode;
import org.jruby.compiler.NodeCompiler;
import org.jruby.compiler.yarv.StandardYARVCompiler;
import org.jruby.compiler.NotCompilableException;
import org.jruby.ast.executable.Script;
import org.jruby.compiler.Compiler;
import org.jruby.compiler.NodeCompilerFactory;
import org.jruby.compiler.impl.StandardASMCompiler;
import org.jruby.runtime.ThreadContext;
import org.jruby.exceptions.JumpException;
import org.jruby.evaluator.EvaluationState;
import org.jruby.runtime.Block;
import org.jruby.ast.Node;
import java.io.Reader;
import java.util.LinkedList;
import java.util.Map;
import org.jruby.util.KCode;
import java.util.Stack;
import org.jruby.common.RubyWarnings;
import org.jruby.internal.runtime.GlobalVariables;
import org.jruby.runtime.load.LoadService;
import org.jruby.parser.Parser;
import org.jruby.util.JRubyClassLoader;
import org.jruby.javasupport.JavaSupport;
import java.io.PrintStream;
import java.io.InputStream;
import org.jruby.runtime.builtin.IRubyObject;
import java.util.List;
import java.util.Random;
import org.jruby.runtime.ObjectSpace;
import org.jruby.runtime.MethodSelectorTable;
import java.util.Hashtable;
import org.jruby.internal.runtime.ThreadService;
import org.jruby.util.MethodCache;

public final class Ruby
{
    private static String[] BUILTIN_LIBRARIES;
    private MethodCache methodCache;
    private ThreadService threadService;
    private Hashtable runtimeInformation;
    private final MethodSelectorTable selectorTable;
    private int stackTraces;
    private ObjectSpace objectSpace;
    private final RubyFixnum[] fixnumCache;
    private final RubySymbol.SymbolTable symbolTable;
    private Hashtable ioHandlers;
    private long randomSeed;
    private long randomSeedSequence;
    private Random random;
    private List eventHooks;
    private boolean globalAbortOnExceptionEnabled;
    private boolean doNotReverseLookupEnabled;
    private final boolean objectSpaceEnabled;
    private long globalState;
    private int safeLevel;
    private IRubyObject nilObject;
    private RubyBoolean trueObject;
    private RubyBoolean falseObject;
    private RubyClass objectClass;
    private RubyClass stringClass;
    private RubyModule enumerableModule;
    private RubyClass systemCallError;
    private RubyModule errnoModule;
    private IRubyObject topSelf;
    private String currentDirectory;
    private long startTime;
    private RubyInstanceConfig config;
    private InputStream in;
    private PrintStream out;
    private PrintStream err;
    private IRubyObject verbose;
    private IRubyObject debug;
    private JavaSupport javaSupport;
    private JRubyClassLoader jrubyClassLoader;
    private static boolean securityRestricted;
    private Parser parser;
    private LoadService loadService;
    private GlobalVariables globalVariables;
    private RubyWarnings warnings;
    private Stack atExitBlocks;
    private RubyModule kernelModule;
    private RubyClass nilClass;
    private RubyClass fixnumClass;
    private RubyClass arrayClass;
    private RubyClass hashClass;
    private IRubyObject tmsStruct;
    private IRubyObject undef;
    private Profile profile;
    private String jrubyHome;
    private KCode kcode;
    public int symbolLastId;
    public int moduleLastId;
    private Object respondToMethod;
    private Map finalizers;
    private String[] argv;
    private final CallTraceFuncHook callTraceFuncHook;
    private ThreadLocal inspect;
    
    private Ruby(final RubyInstanceConfig config) {
        this.methodCache = new MethodCache();
        this.threadService = new ThreadService(this);
        this.selectorTable = new MethodSelectorTable();
        this.stackTraces = 0;
        this.objectSpace = new ObjectSpace();
        this.fixnumCache = new RubyFixnum[256];
        this.symbolTable = new RubySymbol.SymbolTable();
        this.ioHandlers = new Hashtable();
        this.randomSeed = 0L;
        this.randomSeedSequence = 0L;
        this.random = new Random();
        this.eventHooks = new LinkedList();
        this.globalAbortOnExceptionEnabled = false;
        this.doNotReverseLookupEnabled = false;
        this.globalState = 1L;
        this.safeLevel = 0;
        this.systemCallError = null;
        this.errnoModule = null;
        this.startTime = System.currentTimeMillis();
        this.parser = new Parser(this);
        this.globalVariables = new GlobalVariables(this);
        this.warnings = new RubyWarnings(this);
        this.atExitBlocks = new Stack();
        this.kcode = KCode.NONE;
        this.symbolLastId = 128;
        this.moduleLastId = 0;
        this.callTraceFuncHook = new CallTraceFuncHook();
        this.inspect = new ThreadLocal();
        this.config = config;
        this.in = config.getInput();
        this.out = config.getOutput();
        this.err = config.getError();
        this.objectSpaceEnabled = config.isObjectSpaceEnabled();
        this.profile = config.getProfile();
        this.currentDirectory = config.getCurrentDirectory();
        this.argv = config.getArgv();
    }
    
    public static Ruby getDefaultInstance() {
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
    
    public IRubyObject evalScript(final Reader reader, final String name) {
        return this.eval(this.parse(reader, name, this.getCurrentContext().getCurrentScope(), 0));
    }
    
    public IRubyObject evalScript(final String script) {
        return this.eval(this.parse(script, "<script>", this.getCurrentContext().getCurrentScope(), 0));
    }
    
    public IRubyObject eval(final Node node) {
        try {
            final ThreadContext tc = this.getCurrentContext();
            return EvaluationState.eval(this, tc, node, tc.getFrameSelf(), Block.NULL_BLOCK);
        }
        catch (JumpException je) {
            if (je.getJumpType() == JumpException.JumpType.ReturnJump) {
                throw this.newLocalJumpError("return", (IRubyObject)je.getValue(), "unexpected return");
            }
            if (je.getJumpType() == JumpException.JumpType.BreakJump) {
                throw this.newLocalJumpError("break", (IRubyObject)je.getValue(), "unexpected break");
            }
            throw je;
        }
    }
    
    public IRubyObject compileOrFallbackAndRun(final Node node) {
        try {
            final ThreadContext tc = this.getCurrentContext();
            if (this.config.isJitEnabled() && !this.hasEventHooks()) {
                Script script = null;
                try {
                    final StandardASMCompiler compiler = new StandardASMCompiler(node);
                    NodeCompilerFactory.getCompiler(node).compile(node, compiler);
                    final Class scriptClass = compiler.loadClass(this.getJRubyClassLoader());
                    script = scriptClass.newInstance();
                }
                catch (Throwable t) {
                    return this.eval(node);
                }
                return script.run(this.getCurrentContext(), tc.getFrameSelf(), IRubyObject.NULL_ARRAY, Block.NULL_BLOCK);
            }
            return this.eval(node);
        }
        catch (JumpException je) {
            if (je.getJumpType() == JumpException.JumpType.ReturnJump) {
                return (IRubyObject)je.getValue();
            }
            throw je;
        }
    }
    
    public IRubyObject compileAndRun(final Node node) {
        try {
            final ThreadContext tc = this.getCurrentContext();
            final StandardASMCompiler compiler = new StandardASMCompiler(node);
            NodeCompilerFactory.getCompiler(node).compile(node, compiler);
            final Class scriptClass = compiler.loadClass(this.getJRubyClassLoader());
            final Script script = scriptClass.newInstance();
            return script.run(this.getCurrentContext(), tc.getFrameSelf(), IRubyObject.NULL_ARRAY, Block.NULL_BLOCK);
        }
        catch (NotCompilableException nce) {
            System.err.println("Error -- Not compileable: " + nce.getMessage());
            return null;
        }
        catch (JumpException je) {
            if (je.getJumpType() == JumpException.JumpType.ReturnJump) {
                return (IRubyObject)je.getValue();
            }
            throw je;
        }
        catch (ClassNotFoundException e) {
            System.err.println("Error -- Not compileable: " + e.getMessage());
            return null;
        }
        catch (InstantiationException e2) {
            System.err.println("Error -- Not compileable: " + e2.getMessage());
            return null;
        }
        catch (IllegalAccessException e3) {
            System.err.println("Error -- Not compileable: " + e3.getMessage());
            return null;
        }
    }
    
    public IRubyObject ycompileAndRun(final Node node) {
        try {
            final StandardYARVCompiler compiler = new StandardYARVCompiler(this);
            NodeCompilerFactory.getYARVCompiler().compile(node, compiler);
            ISourcePosition p = node.getPosition();
            if (p == null && node instanceof RootNode) {
                p = ((RootNode)node).getBodyNode().getPosition();
            }
            return new YARVCompiledRunner(this, compiler.getInstructionSequence("<main>", p.getFile(), "toplevel")).run();
        }
        catch (NotCompilableException nce) {
            System.err.println("Error -- Not compileable: " + nce.getMessage());
            return null;
        }
        catch (JumpException je) {
            if (je.getJumpType() == JumpException.JumpType.ReturnJump) {
                return (IRubyObject)je.getValue();
            }
            throw je;
        }
    }
    
    Object getRespondToMethod() {
        return this.respondToMethod;
    }
    
    void setRespondToMethod(final Object rtm) {
        this.respondToMethod = rtm;
    }
    
    public RubyClass getObject() {
        return this.objectClass;
    }
    
    public IRubyObject getUndef() {
        return this.undef;
    }
    
    public RubyModule getKernel() {
        return this.kernelModule;
    }
    
    public RubyModule getEnumerable() {
        return this.enumerableModule;
    }
    
    public RubyClass getString() {
        return this.stringClass;
    }
    
    public RubyClass getFixnum() {
        return this.fixnumClass;
    }
    
    public RubyClass getHash() {
        return this.hashClass;
    }
    
    public RubyClass getArray() {
        return this.arrayClass;
    }
    
    public IRubyObject getTmsStruct() {
        return this.tmsStruct;
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
    
    public RubyClass getNilClass() {
        return this.nilClass;
    }
    
    public RubyModule getModule(final String name) {
        return (RubyModule)this.objectClass.getConstantAt(name);
    }
    
    public RubyClass getClass(final String name) {
        try {
            return this.objectClass.getClass(name);
        }
        catch (ClassCastException e) {
            throw this.newTypeError(name + " is not a Class");
        }
    }
    
    public RubyClass defineClass(final String name, final RubyClass superClass, final ObjectAllocator allocator) {
        return this.defineClassUnder(name, superClass, allocator, this.objectClass.getCRef());
    }
    
    public RubyClass defineClassUnder(final String name, RubyClass superClass, final ObjectAllocator allocator, final SinglyLinkedList parentCRef) {
        if (superClass == null) {
            superClass = this.objectClass;
        }
        return superClass.newSubClass(name, allocator, parentCRef, true);
    }
    
    public RubyModule defineModule(final String name) {
        return this.defineModuleUnder(name, this.objectClass.getCRef());
    }
    
    public RubyModule defineModuleUnder(final String name, final SinglyLinkedList parentCRef) {
        final RubyModule newModule = RubyModule.newModule(this, name, parentCRef);
        ((RubyModule)parentCRef.getValue()).setConstant(name, newModule);
        return newModule;
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
            if (!(module instanceof RubyModule)) {
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
    
    public MethodCache getMethodCache() {
        return this.methodCache;
    }
    
    public Map getRuntimeInformation() {
        return (this.runtimeInformation == null) ? (this.runtimeInformation = new Hashtable()) : this.runtimeInformation;
    }
    
    public MethodSelectorTable getSelectorTable() {
        return this.selectorTable;
    }
    
    public void defineGlobalConstant(final String name, final IRubyObject value) {
        this.objectClass.defineConstant(name, value);
    }
    
    public boolean isClassDefined(final String name) {
        return this.getModule(name) != null;
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
    
    private void init() {
        final ThreadContext tc = this.getCurrentContext();
        this.defineGlobalVERBOSE();
        this.defineGlobalDEBUG();
        this.javaSupport = new JavaSupport(this);
        tc.preInitCoreClasses();
        this.initCoreClasses();
        this.verbose = this.falseObject;
        this.debug = this.falseObject;
        this.selectorTable.init();
        this.initLibraries();
        this.topSelf = TopSelfFactory.createTopSelf(this);
        tc.preInitBuiltinClasses(this.objectClass, this.topSelf);
        RubyGlobal.createGlobals(this);
        this.defineGlobalConstant("TRUE", this.trueObject);
        this.defineGlobalConstant("FALSE", this.falseObject);
        this.defineGlobalConstant("NIL", this.nilObject);
        this.getObject().defineConstant("TOPLEVEL_BINDING", this.newBinding());
        RubyKernel.autoload(this.topSelf, this.newSymbol("Java"), this.newString("java"));
        this.methodCache.initialized();
    }
    
    private void initLibraries() {
        this.loadService = this.config.createLoadService(this);
        this.registerBuiltin("java.rb", new Library() {
            public void load(final Ruby runtime) throws IOException {
                Java.createJavaModule(runtime);
                runtime.getLoadService().smartLoad("builtin/javasupport");
                RubyClassPathVariable.createClassPathVariable(runtime);
            }
        });
        this.registerBuiltin("socket.rb", new RubySocket.Service());
        this.registerBuiltin("rbconfig.rb", new RbConfigLibrary());
        for (int i = 0; i < Ruby.BUILTIN_LIBRARIES.length; ++i) {
            if (this.profile.allowBuiltin(Ruby.BUILTIN_LIBRARIES[i])) {
                this.loadService.registerRubyBuiltin(Ruby.BUILTIN_LIBRARIES[i]);
            }
        }
        final Library NO_OP_LIBRARY = new Library() {
            public void load(final Ruby runtime) throws IOException {
            }
        };
        this.registerBuiltin("jruby.rb", new LateLoadingLibrary("jruby", "org.jruby.libraries.JRubyLibrary", this.getJRubyClassLoader()));
        this.registerBuiltin("iconv.rb", new LateLoadingLibrary("iconv", "org.jruby.libraries.IConvLibrary", this.getJRubyClassLoader()));
        this.registerBuiltin("nkf.rb", new LateLoadingLibrary("nkf", "org.jruby.libraries.NKFLibrary", this.getJRubyClassLoader()));
        this.registerBuiltin("stringio.rb", new LateLoadingLibrary("stringio", "org.jruby.libraries.StringIOLibrary", this.getJRubyClassLoader()));
        this.registerBuiltin("strscan.rb", new LateLoadingLibrary("strscan", "org.jruby.libraries.StringScannerLibrary", this.getJRubyClassLoader()));
        this.registerBuiltin("zlib.rb", new LateLoadingLibrary("zlib", "org.jruby.libraries.ZlibLibrary", this.getJRubyClassLoader()));
        this.registerBuiltin("yaml_internal.rb", new LateLoadingLibrary("yaml_internal", "org.jruby.libraries.YamlLibrary", this.getJRubyClassLoader()));
        this.registerBuiltin("enumerator.rb", new LateLoadingLibrary("enumerator", "org.jruby.libraries.EnumeratorLibrary", this.getJRubyClassLoader()));
        this.registerBuiltin("generator_internal.rb", new LateLoadingLibrary("generator_internal", "org.jruby.ext.Generator$Service", this.getJRubyClassLoader()));
        this.registerBuiltin("readline.rb", new LateLoadingLibrary("readline", "org.jruby.ext.Readline$Service", this.getJRubyClassLoader()));
        this.registerBuiltin("thread.so", new LateLoadingLibrary("thread", "org.jruby.libraries.ThreadLibrary", this.getJRubyClassLoader()));
        this.registerBuiltin("openssl.so", new Library() {
            public void load(final Ruby runtime) throws IOException {
                runtime.getLoadService().require("jruby/openssl/stub");
            }
        });
        this.registerBuiltin("digest.so", new LateLoadingLibrary("digest", "org.jruby.libraries.DigestLibrary", this.getJRubyClassLoader()));
        this.registerBuiltin("digest.rb", new LateLoadingLibrary("digest", "org.jruby.libraries.DigestLibrary", this.getJRubyClassLoader()));
        this.registerBuiltin("digest/md5.rb", new LateLoadingLibrary("digest/md5", "org.jruby.libraries.DigestLibrary$MD5", this.getJRubyClassLoader()));
        this.registerBuiltin("digest/rmd160.rb", new LateLoadingLibrary("digest/rmd160", "org.jruby.libraries.DigestLibrary$RMD160", this.getJRubyClassLoader()));
        this.registerBuiltin("digest/sha1.rb", new LateLoadingLibrary("digest/sha1", "org.jruby.libraries.DigestLibrary$SHA1", this.getJRubyClassLoader()));
        this.registerBuiltin("digest/sha2.rb", new LateLoadingLibrary("digest/sha2", "org.jruby.libraries.DigestLibrary$SHA2", this.getJRubyClassLoader()));
        this.registerBuiltin("bigdecimal.rb", new LateLoadingLibrary("bigdecimal", "org.jruby.libraries.BigDecimalLibrary", this.getJRubyClassLoader()));
        this.registerBuiltin("io/wait.so", new LateLoadingLibrary("io/wait", "org.jruby.libraries.IOWaitLibrary", this.getJRubyClassLoader()));
        this.registerBuiltin("etc.so", NO_OP_LIBRARY);
    }
    
    private void registerBuiltin(final String nm, final Library lib) {
        if (this.profile.allowBuiltin(nm)) {
            this.loadService.registerBuiltin(nm, lib);
        }
    }
    
    private void initCoreClasses() {
        this.undef = new RubyUndef();
        final RubyClass objectMetaClass = RubyClass.createBootstrapMetaClass(this, "Object", null, RubyObject.OBJECT_ALLOCATOR, null);
        RubyObject.createObjectClass(this, objectMetaClass);
        (this.objectClass = objectMetaClass).setConstant("Object", this.objectClass);
        final RubyClass moduleClass = RubyClass.createBootstrapMetaClass(this, "Module", this.objectClass, RubyModule.MODULE_ALLOCATOR, this.objectClass.getCRef());
        this.objectClass.setConstant("Module", moduleClass);
        final RubyClass classClass = RubyClass.newClassClass(this, moduleClass);
        this.objectClass.setConstant("Class", classClass);
        classClass.setMetaClass(classClass);
        moduleClass.setMetaClass(classClass);
        this.objectClass.setMetaClass(classClass);
        RubyClass metaClass = this.objectClass.makeMetaClass(classClass, objectMetaClass.getCRef());
        metaClass = moduleClass.makeMetaClass(metaClass, objectMetaClass.getCRef());
        metaClass = classClass.makeMetaClass(metaClass, objectMetaClass.getCRef());
        RubyModule.createModuleClass(this, moduleClass);
        this.kernelModule = RubyKernel.createKernelModule(this);
        this.objectClass.includeModule(this.kernelModule);
        RubyClass.createClassClass(classClass);
        this.nilClass = RubyNil.createNilClass(this);
        RubyBoolean.createFalseClass(this);
        RubyBoolean.createTrueClass(this);
        this.nilObject = new RubyNil(this);
        this.trueObject = new RubyBoolean(this, true);
        this.falseObject = new RubyBoolean(this, false);
        RubyComparable.createComparable(this);
        this.enumerableModule = RubyEnumerable.createEnumerableModule(this);
        this.stringClass = RubyString.createStringClass(this);
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
        if (this.profile.allowModule("Precision")) {
            RubyPrecision.createPrecisionModule(this);
        }
        if (this.profile.allowClass("Numeric")) {
            RubyNumeric.createNumericClass(this);
        }
        if (this.profile.allowClass("Integer")) {
            RubyInteger.createIntegerClass(this);
        }
        if (this.profile.allowClass("Fixnum")) {
            this.fixnumClass = RubyFixnum.createFixnumClass(this);
        }
        if (this.profile.allowClass("Hash")) {
            this.hashClass = RubyHash.createHashClass(this);
        }
        RubyIO.createIOClass(this);
        if (this.profile.allowClass("Array")) {
            this.arrayClass = RubyArray.createArrayClass(this);
        }
        final RubyArray argvArray = this.newArray();
        for (int i = 0; i < this.argv.length; ++i) {
            argvArray.add(this.newString(this.argv[i]));
        }
        this.defineGlobalConstant("ARGV", argvArray);
        this.getGlobalVariables().defineReadonly("$*", new ValueAccessor(argvArray));
        RubyClass structClass = null;
        if (this.profile.allowClass("Struct")) {
            structClass = RubyStruct.createStructClass(this);
        }
        if (this.profile.allowClass("Tms")) {
            this.tmsStruct = RubyStruct.newInstance(structClass, new IRubyObject[] { this.newString("Tms"), this.newSymbol("utime"), this.newSymbol("stime"), this.newSymbol("cutime"), this.newSymbol("cstime") }, Block.NULL_BLOCK);
        }
        if (this.profile.allowClass("Float")) {
            RubyFloat.createFloatClass(this);
        }
        if (this.profile.allowClass("Bignum")) {
            RubyBignum.createBignumClass(this);
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
        if (this.profile.allowModule("Process")) {
            RubyProcess.createProcessModule(this);
        }
        if (this.profile.allowClass("Time")) {
            RubyTime.createTimeClass(this);
        }
        if (this.profile.allowClass("UnboundMethod")) {
            RubyUnboundMethod.defineUnboundMethodClass(this);
        }
        final RubyClass exceptionClass = this.getClass("Exception");
        RubyClass standardError = null;
        RubyClass runtimeError = null;
        RubyClass ioError = null;
        RubyClass scriptError = null;
        RubyClass nameError = null;
        RubyClass signalException = null;
        RubyClass rangeError = null;
        if (this.profile.allowClass("StandardError")) {
            standardError = this.defineClass("StandardError", exceptionClass, exceptionClass.getAllocator());
        }
        if (this.profile.allowClass("RuntimeError")) {
            runtimeError = this.defineClass("RuntimeError", standardError, standardError.getAllocator());
        }
        if (this.profile.allowClass("IOError")) {
            ioError = this.defineClass("IOError", standardError, standardError.getAllocator());
        }
        if (this.profile.allowClass("ScriptError")) {
            scriptError = this.defineClass("ScriptError", exceptionClass, exceptionClass.getAllocator());
        }
        if (this.profile.allowClass("NameError")) {
            nameError = RubyNameError.createNameErrorClass(this, standardError);
        }
        if (this.profile.allowClass("NoMethodError")) {
            RubyNoMethodError.createNoMethodErrorClass(this, nameError);
        }
        if (this.profile.allowClass("RangeError")) {
            rangeError = this.defineClass("RangeError", standardError, standardError.getAllocator());
        }
        if (this.profile.allowClass("SystemExit")) {
            RubySystemExit.createSystemExitClass(this, exceptionClass);
        }
        if (this.profile.allowClass("Fatal")) {
            this.defineClass("Fatal", exceptionClass, exceptionClass.getAllocator());
        }
        if (this.profile.allowClass("SignalException")) {
            signalException = this.defineClass("SignalException", exceptionClass, exceptionClass.getAllocator());
        }
        if (this.profile.allowClass("Interrupt")) {
            this.defineClass("Interrupt", signalException, signalException.getAllocator());
        }
        if (this.profile.allowClass("TypeError")) {
            this.defineClass("TypeError", standardError, standardError.getAllocator());
        }
        if (this.profile.allowClass("ArgumentError")) {
            this.defineClass("ArgumentError", standardError, standardError.getAllocator());
        }
        if (this.profile.allowClass("IndexError")) {
            this.defineClass("IndexError", standardError, standardError.getAllocator());
        }
        if (this.profile.allowClass("SyntaxError")) {
            this.defineClass("SyntaxError", scriptError, scriptError.getAllocator());
        }
        if (this.profile.allowClass("LoadError")) {
            this.defineClass("LoadError", scriptError, scriptError.getAllocator());
        }
        if (this.profile.allowClass("NotImplementedError")) {
            this.defineClass("NotImplementedError", scriptError, scriptError.getAllocator());
        }
        if (this.profile.allowClass("SecurityError")) {
            this.defineClass("SecurityError", standardError, standardError.getAllocator());
        }
        if (this.profile.allowClass("NoMemoryError")) {
            this.defineClass("NoMemoryError", exceptionClass, exceptionClass.getAllocator());
        }
        if (this.profile.allowClass("RegexpError")) {
            this.defineClass("RegexpError", standardError, standardError.getAllocator());
        }
        if (this.profile.allowClass("EOFError")) {
            this.defineClass("EOFError", ioError, ioError.getAllocator());
        }
        if (this.profile.allowClass("LocalJumpError")) {
            RubyLocalJumpError.createLocalJumpErrorClass(this, standardError);
        }
        if (this.profile.allowClass("ThreadError")) {
            this.defineClass("ThreadError", standardError, standardError.getAllocator());
        }
        if (this.profile.allowClass("SystemStackError")) {
            this.defineClass("SystemStackError", exceptionClass, exceptionClass.getAllocator());
        }
        if (this.profile.allowClass("ZeroDivisionError")) {
            this.defineClass("ZeroDivisionError", standardError, standardError.getAllocator());
        }
        if (this.profile.allowClass("FloatDomainError")) {
            this.defineClass("FloatDomainError", rangeError, rangeError.getAllocator());
        }
        if (this.profile.allowClass("NativeException")) {
            NativeException.createClass(this, runtimeError);
        }
        if (this.profile.allowClass("SystemCallError")) {
            this.systemCallError = this.defineClass("SystemCallError", standardError, standardError.getAllocator());
        }
        if (this.profile.allowModule("Errno")) {
            this.errnoModule = this.defineModule("Errno");
        }
        this.initErrnoErrors();
        if (this.profile.allowClass("Data")) {
            this.defineClass("Data", this.objectClass, this.objectClass.getAllocator());
        }
        if (this.profile.allowModule("Signal")) {
            RubySignal.createSignal(this);
        }
        if (this.profile.allowClass("Continuation")) {
            RubyContinuation.createContinuation(this);
        }
    }
    
    private void initErrnoErrors() {
        this.createSysErr(1, "ENOTEMPTY");
        this.createSysErr(2, "ERANGE");
        this.createSysErr(3, "ESPIPE");
        this.createSysErr(4, "ENFILE");
        this.createSysErr(5, "EXDEV");
        this.createSysErr(6, "ENOMEM");
        this.createSysErr(7, "E2BIG");
        this.createSysErr(8, "ENOENT");
        this.createSysErr(9, "ENOSYS");
        this.createSysErr(10, "EDOM");
        this.createSysErr(11, "ENOSPC");
        this.createSysErr(42, "EINVAL");
        this.createSysErr(43, "EEXIST");
        this.createSysErr(44, "EAGAIN");
        this.createSysErr(45, "ENXIO");
        this.createSysErr(46, "EILSEQ");
        this.createSysErr(47, "ENOLCK");
        this.createSysErr(48, "EPIPE");
        this.createSysErr(49, "EFBIG");
        this.createSysErr(50, "EISDIR");
        this.createSysErr(51, "EBUSY");
        this.createSysErr(52, "ECHILD");
        this.createSysErr(53, "EIO");
        this.createSysErr(54, "EPERM");
        this.createSysErr(55, "EDEADLOCK");
        this.createSysErr(56, "ENAMETOOLONG");
        this.createSysErr(57, "EMLINK");
        this.createSysErr(58, "ENOTTY");
        this.createSysErr(59, "ENOTDIR");
        this.createSysErr(60, "EFAULT");
        this.createSysErr(61, "EBADF");
        this.createSysErr(62, "EINTR");
        this.createSysErr(63, "EWOULDBLOCK");
        this.createSysErr(64, "EDEADLK");
        this.createSysErr(65, "EROFS");
        this.createSysErr(66, "EMFILE");
        this.createSysErr(67, "ENODEV");
        this.createSysErr(68, "EACCES");
        this.createSysErr(69, "ENOEXEC");
        this.createSysErr(70, "ESRCH");
        this.createSysErr(71, "ECONNREFUSED");
        this.createSysErr(72, "ECONNRESET");
        this.createSysErr(73, "EADDRINUSE");
        this.createSysErr(74, "ECONNABORTED");
    }
    
    private void createSysErr(final int i, final String name) {
        if (this.profile.allowClass(name)) {
            this.errnoModule.defineClassUnder(name, this.systemCallError, this.systemCallError.getAllocator()).defineConstant("Errno", this.newFixnum(i));
        }
    }
    
    public IRubyObject getVerbose() {
        return this.verbose;
    }
    
    public void setVerbose(final IRubyObject verbose) {
        this.verbose = verbose;
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
    
    public JRubyClassLoader getJRubyClassLoader() {
        if (!isSecurityRestricted() && this.jrubyClassLoader == null) {
            this.jrubyClassLoader = new JRubyClassLoader(Thread.currentThread().getContextClassLoader());
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
    
    public Node parse(final Reader content, final String file, final DynamicScope scope, final int lineNumber) {
        return this.parser.parse(file, content, scope, lineNumber);
    }
    
    public Node parse(final String content, final String file, final DynamicScope scope, final int lineNumber) {
        return this.parser.parse(file, content, scope, lineNumber);
    }
    
    public Node parse(final String content, final String file, final DynamicScope scope, final int lineNumber, final boolean extraPositionInformation) {
        return this.parser.parse(file, content, scope, lineNumber, extraPositionInformation);
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
                throw this.newTypeError("" + str + " does not refer to class/module");
            }
            c = (RubyModule)cc;
        }
        return c;
    }
    
    public void printError(final RubyException excp) {
        if (excp == null || excp.isNil()) {
            return;
        }
        final ThreadContext tc = this.getCurrentContext();
        final IRubyObject backtrace = excp.callMethod(tc, "backtrace");
        final PrintStream errorStream = this.getErrorStream();
        if (backtrace.isNil() || !(backtrace instanceof RubyArray)) {
            if (tc.getSourceFile() != null) {
                errorStream.print(tc.getPosition());
            }
            else {
                errorStream.print(tc.getSourceLine());
            }
        }
        else if (((RubyArray)backtrace).getLength() == 0) {
            this.printErrorPos(errorStream);
        }
        else {
            final IRubyObject mesg = ((RubyArray)backtrace).first(IRubyObject.NULL_ARRAY);
            if (mesg.isNil()) {
                this.printErrorPos(errorStream);
            }
            else {
                errorStream.print(mesg);
            }
        }
        final RubyClass type = excp.getMetaClass();
        String info = excp.toString();
        if (type == this.getClass("RuntimeError") && (info == null || info.length() == 0)) {
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
    
    private void printErrorPos(final PrintStream errorStream) {
        final ThreadContext tc = this.getCurrentContext();
        if (tc.getSourceFile() != null) {
            if (tc.getFrameName() != null) {
                errorStream.print(tc.getPosition());
                errorStream.print(":in '" + tc.getFrameName() + '\'');
            }
            else if (tc.getSourceLine() != 0) {
                errorStream.print(tc.getPosition());
            }
            else {
                errorStream.print(tc.getSourceFile());
            }
        }
    }
    
    public void loadScript(final RubyString scriptName, final RubyString source) {
        this.loadScript(scriptName.toString(), new StringReader(source.toString()));
    }
    
    public void loadScript(String scriptName, final Reader source) {
        if (!isSecurityRestricted()) {
            final File f = new File(scriptName);
            if (f.exists() && !f.isAbsolute() && !scriptName.startsWith("./")) {
                scriptName = "./" + scriptName;
            }
        }
        final IRubyObject self = this.getTopSelf();
        final ThreadContext context = this.getCurrentContext();
        try {
            this.secure(4);
            context.preNodeEval(this.objectClass, self);
            final Node node = this.parse(source, scriptName, null, 0);
            EvaluationState.eval(this, context, node, self, Block.NULL_BLOCK);
        }
        catch (JumpException je) {
            if (je.getJumpType() != JumpException.JumpType.ReturnJump) {
                throw je;
            }
        }
        finally {
            context.postNodeEval();
        }
    }
    
    public void loadScript(final Script script) {
        final IRubyObject self = this.getTopSelf();
        final ThreadContext context = this.getCurrentContext();
        try {
            this.secure(4);
            context.preNodeEval(this.objectClass, self);
            script.run(context, self, IRubyObject.NULL_ARRAY, Block.NULL_BLOCK);
        }
        catch (JumpException je) {
            if (je.getJumpType() != JumpException.JumpType.ReturnJump) {
                throw je;
            }
        }
        finally {
            context.postNodeEval();
        }
    }
    
    public void loadNode(final String scriptName, final Node node) {
        final IRubyObject self = this.getTopSelf();
        final ThreadContext context = this.getCurrentContext();
        try {
            this.secure(4);
            context.preNodeEval(this.objectClass, self);
            EvaluationState.eval(this, context, node, self, Block.NULL_BLOCK);
        }
        catch (JumpException je) {
            if (je.getJumpType() != JumpException.JumpType.ReturnJump) {
                throw je;
            }
        }
        finally {
            context.postNodeEval();
        }
    }
    
    public void loadFile(final File file) {
        assert file != null : "No such file to load";
        BufferedReader source = null;
        try {
            source = new BufferedReader(new FileReader(file));
            this.loadScript(file.getPath().replace(File.separatorChar, '/'), source);
        }
        catch (IOException ioExcptn) {
            throw this.newIOErrorFromException(ioExcptn);
        }
        finally {
            try {
                if (source == null) {
                    source.close();
                }
            }
            catch (IOException ex) {}
        }
    }
    
    public void addEventHook(final EventHook hook) {
        this.eventHooks.add(hook);
    }
    
    public void removeEventHook(final EventHook hook) {
        this.eventHooks.remove(hook);
    }
    
    public void setTraceFunction(final RubyProc traceFunction) {
        this.removeEventHook(this.callTraceFuncHook);
        if (traceFunction == null) {
            return;
        }
        this.callTraceFuncHook.setTraceFunc(traceFunction);
        this.addEventHook(this.callTraceFuncHook);
    }
    
    public void callEventHooks(final ThreadContext context, final int event, final String file, final int line, final String name, final IRubyObject type) {
        for (int i = 0; i < this.eventHooks.size(); ++i) {
            final EventHook eventHook = this.eventHooks.get(i);
            if (eventHook.isInterestedInEvent(event)) {
                eventHook.event(context, event, file, line, name, type);
            }
        }
    }
    
    public boolean hasEventHooks() {
        return !this.eventHooks.isEmpty();
    }
    
    public GlobalVariables getGlobalVariables() {
        return this.globalVariables;
    }
    
    public void setGlobalVariables(final GlobalVariables globalVariables) {
        this.globalVariables = globalVariables;
    }
    
    public CallbackFactory callbackFactory(final Class type) {
        return CallbackFactory.createFactory(this, type);
    }
    
    public IRubyObject pushExitBlock(final RubyProc proc) {
        this.atExitBlocks.push(proc);
        return proc;
    }
    
    public void addFinalizer(final Finalizable finalizer) {
        synchronized (this) {
            if (this.finalizers == null) {
                this.finalizers = new WeakHashMap();
            }
        }
        synchronized (this.finalizers) {
            this.finalizers.put(finalizer, null);
        }
    }
    
    public void removeFinalizer(final Finalizable finalizer) {
        if (this.finalizers != null) {
            synchronized (this.finalizers) {
                this.finalizers.remove(finalizer);
            }
        }
    }
    
    public void tearDown() {
        while (!this.atExitBlocks.empty()) {
            final RubyProc proc = this.atExitBlocks.pop();
            proc.call(IRubyObject.NULL_ARRAY);
        }
        if (this.finalizers != null) {
            synchronized (this.finalizers) {
                final Iterator finalIter = new ArrayList(this.finalizers.keySet()).iterator();
                while (finalIter.hasNext()) {
                    finalIter.next().finalize();
                    finalIter.remove();
                }
            }
        }
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
    
    public RubyArray newArray(final IRubyObject[] objects) {
        return RubyArray.newArray(this, objects);
    }
    
    public RubyArray newArrayNoCopy(final IRubyObject[] objects) {
        return RubyArray.newArrayNoCopy(this, objects);
    }
    
    public RubyArray newArrayNoCopyLight(final IRubyObject[] objects) {
        return RubyArray.newArrayNoCopyLight(this, objects);
    }
    
    public RubyArray newArray(final List list) {
        return RubyArray.newArray(this, list);
    }
    
    public RubyArray newArray(final int size) {
        return RubyArray.newArray(this, size);
    }
    
    public RubyBoolean newBoolean(final boolean value) {
        return RubyBoolean.newBoolean(this, value);
    }
    
    public RubyFileStat newRubyFileStat(final String file) {
        return (RubyFileStat)this.getClass("File").getClass("Stat").callMethod(this.getCurrentContext(), "new", this.newString(file));
    }
    
    public RubyFixnum newFixnum(final long value) {
        return RubyFixnum.newFixnum(this, value);
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
    
    public RubyBinding newBinding() {
        return RubyBinding.newBinding(this);
    }
    
    public RubyBinding newBinding(final Block block) {
        return RubyBinding.newBinding(this, block);
    }
    
    public RubyString newString() {
        return RubyString.newString(this, "");
    }
    
    public RubyString newString(final String string) {
        return RubyString.newString(this, string);
    }
    
    public RubyString newString(final ByteList byteList) {
        return RubyString.newString(this, byteList);
    }
    
    public RubyString newStringShared(final ByteList byteList) {
        return RubyString.newStringShared(this, byteList);
    }
    
    public RubySymbol newSymbol(final String string) {
        return RubySymbol.newSymbol(this, string);
    }
    
    public RubyTime newTime(final long milliseconds) {
        return RubyTime.newTime(this, milliseconds);
    }
    
    public RaiseException newRuntimeError(final String message) {
        return this.newRaiseException(this.getClass("RuntimeError"), message);
    }
    
    public RaiseException newArgumentError(final String message) {
        return this.newRaiseException(this.getClass("ArgumentError"), message);
    }
    
    public RaiseException newArgumentError(final int got, final int expected) {
        return this.newRaiseException(this.getClass("ArgumentError"), "wrong # of arguments(" + got + " for " + expected + ")");
    }
    
    public RaiseException newErrnoEBADFError() {
        return this.newRaiseException(this.getModule("Errno").getClass("EBADF"), "Bad file descriptor");
    }
    
    public RaiseException newErrnoEPIPEError() {
        return this.newRaiseException(this.getModule("Errno").getClass("EPIPE"), "Broken pipe");
    }
    
    public RaiseException newErrnoECONNREFUSEDError() {
        return this.newRaiseException(this.getModule("Errno").getClass("ECONNREFUSED"), "Connection refused");
    }
    
    public RaiseException newErrnoEADDRINUSEError() {
        return this.newRaiseException(this.getModule("Errno").getClass("EADDRINUSE"), "Address in use");
    }
    
    public RaiseException newErrnoEINVALError() {
        return this.newRaiseException(this.getModule("Errno").getClass("EINVAL"), "Invalid file");
    }
    
    public RaiseException newErrnoENOENTError() {
        return this.newRaiseException(this.getModule("Errno").getClass("ENOENT"), "File not found");
    }
    
    public RaiseException newErrnoEISDirError() {
        return this.newRaiseException(this.getModule("Errno").getClass("EISDIR"), "Is a directory");
    }
    
    public RaiseException newErrnoESPIPEError() {
        return this.newRaiseException(this.getModule("Errno").getClass("ESPIPE"), "Illegal seek");
    }
    
    public RaiseException newErrnoEBADFError(final String message) {
        return this.newRaiseException(this.getModule("Errno").getClass("EBADF"), message);
    }
    
    public RaiseException newErrnoEINVALError(final String message) {
        return this.newRaiseException(this.getModule("Errno").getClass("EINVAL"), message);
    }
    
    public RaiseException newErrnoENOENTError(final String message) {
        return this.newRaiseException(this.getModule("Errno").getClass("ENOENT"), message);
    }
    
    public RaiseException newErrnoESPIPEError(final String message) {
        return this.newRaiseException(this.getModule("Errno").getClass("ESPIPE"), message);
    }
    
    public RaiseException newErrnoEEXISTError(final String message) {
        return this.newRaiseException(this.getModule("Errno").getClass("EEXIST"), message);
    }
    
    public RaiseException newErrnoEDOMError(final String message) {
        return this.newRaiseException(this.getModule("Errno").getClass("EDOM"), "Domain error - " + message);
    }
    
    public RaiseException newIndexError(final String message) {
        return this.newRaiseException(this.getClass("IndexError"), message);
    }
    
    public RaiseException newSecurityError(final String message) {
        return this.newRaiseException(this.getClass("SecurityError"), message);
    }
    
    public RaiseException newSystemCallError(final String message) {
        return this.newRaiseException(this.getClass("SystemCallError"), message);
    }
    
    public RaiseException newTypeError(final String message) {
        return this.newRaiseException(this.getClass("TypeError"), message);
    }
    
    public RaiseException newThreadError(final String message) {
        return this.newRaiseException(this.getClass("ThreadError"), message);
    }
    
    public RaiseException newSyntaxError(final String message) {
        return this.newRaiseException(this.getClass("SyntaxError"), message);
    }
    
    public RaiseException newRegexpError(final String message) {
        return this.newRaiseException(this.getClass("RegexpError"), message);
    }
    
    public RaiseException newRangeError(final String message) {
        return this.newRaiseException(this.getClass("RangeError"), message);
    }
    
    public RaiseException newNotImplementedError(final String message) {
        return this.newRaiseException(this.getClass("NotImplementedError"), message);
    }
    
    public RaiseException newInvalidEncoding(final String message) {
        return this.newRaiseException(this.getClass("Iconv").getClass("InvalidEncoding"), message);
    }
    
    public RaiseException newNoMethodError(final String message, final String name, final IRubyObject args) {
        return new RaiseException(new RubyNoMethodError(this, this.getClass("NoMethodError"), message, name, args), true);
    }
    
    public RaiseException newNameError(final String message, final String name) {
        return new RaiseException(new RubyNameError(this, this.getClass("NameError"), message, name), true);
    }
    
    public RaiseException newLocalJumpError(final String reason, final IRubyObject exitValue, final String message) {
        return new RaiseException(new RubyLocalJumpError(this, this.getClass("LocalJumpError"), message, reason, exitValue), true);
    }
    
    public RaiseException newLoadError(final String message) {
        return this.newRaiseException(this.getClass("LoadError"), message);
    }
    
    public RaiseException newFrozenError(final String objectType) {
        return this.newRaiseException(this.getClass("TypeError"), "can't modify frozen " + objectType);
    }
    
    public RaiseException newSystemStackError(final String message) {
        return this.newRaiseException(this.getClass("SystemStackError"), message);
    }
    
    public RaiseException newSystemExit(final int status) {
        return new RaiseException(RubySystemExit.newInstance(this, status));
    }
    
    public RaiseException newIOError(final String message) {
        return this.newRaiseException(this.getClass("IOError"), message);
    }
    
    public RaiseException newStandardError(final String message) {
        return this.newRaiseException(this.getClass("StandardError"), message);
    }
    
    public RaiseException newIOErrorFromException(final IOException ioe) {
        return this.newRaiseException(this.getClass("IOError"), ioe.getMessage());
    }
    
    public RaiseException newTypeError(final IRubyObject receivedObject, final RubyClass expectedType) {
        return this.newRaiseException(this.getClass("TypeError"), "wrong argument type " + receivedObject.getMetaClass() + " (expected " + expectedType + ")");
    }
    
    public RaiseException newEOFError() {
        return this.newRaiseException(this.getClass("EOFError"), "End of file reached");
    }
    
    public RaiseException newZeroDivisionError() {
        return this.newRaiseException(this.getClass("ZeroDivisionError"), "divided by 0");
    }
    
    public RaiseException newFloatDomainError(final String message) {
        return this.newRaiseException(this.getClass("FloatDomainError"), message);
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
    
    public Hashtable getIoHandlers() {
        return this.ioHandlers;
    }
    
    public RubyFixnum[] getFixnumCache() {
        return this.fixnumCache;
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
    
    public boolean registerInspecting(final Object obj) {
        Map val = this.inspect.get();
        if (null == val) {
            val = new IdentityHashMap();
            this.inspect.set(val);
        }
        if (val.containsKey(obj)) {
            return false;
        }
        val.put(obj, null);
        return true;
    }
    
    public void unregisterInspecting(final Object obj) {
        final Map val = this.inspect.get();
        val.remove(obj);
    }
    
    public boolean isObjectSpaceEnabled() {
        return this.objectSpaceEnabled;
    }
    
    public long getStartTime() {
        return this.startTime;
    }
    
    public Profile getProfile() {
        return this.profile;
    }
    
    public String getJRubyHome() {
        if (this.jrubyHome == null) {
            this.jrubyHome = this.verifyHome(System.getProperty("jruby.home", System.getProperty("user.home") + "/.jruby"));
        }
        try {
            return new NormalizedFile(this.jrubyHome).getCanonicalPath();
        }
        catch (IOException e) {
            return new NormalizedFile(this.jrubyHome).getAbsolutePath();
        }
    }
    
    public void setJRubyHome(final String home) {
        this.jrubyHome = this.verifyHome(home);
    }
    
    private String verifyHome(String home) {
        if (home.equals(".")) {
            home = System.getProperty("user.dir");
        }
        final NormalizedFile f = new NormalizedFile(home);
        if (!f.isAbsolute()) {
            home = f.getAbsolutePath();
        }
        f.mkdirs();
        return home;
    }
    
    public RubyInstanceConfig getInstanceConfig() {
        return this.config;
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
    
    private void defineGlobalVERBOSE() {
        this.getGlobalVariables().define("$VERBOSE", new IAccessor() {
            public IRubyObject getValue() {
                return Ruby.this.getVerbose();
            }
            
            public IRubyObject setValue(final IRubyObject newValue) {
                if (newValue.isNil()) {
                    Ruby.this.setVerbose(newValue);
                }
                else {
                    Ruby.this.setVerbose(Ruby.this.newBoolean(newValue != Ruby.this.getFalse()));
                }
                return newValue;
            }
        });
    }
    
    private void defineGlobalDEBUG() {
        final IAccessor d = new IAccessor() {
            public IRubyObject getValue() {
                return Ruby.this.getDebug();
            }
            
            public IRubyObject setValue(final IRubyObject newValue) {
                if (newValue.isNil()) {
                    Ruby.this.setDebug(newValue);
                }
                else {
                    Ruby.this.setDebug(Ruby.this.newBoolean(newValue != Ruby.this.getFalse()));
                }
                return newValue;
            }
        };
        this.getGlobalVariables().define("$DEBUG", d);
        this.getGlobalVariables().define("$-d", d);
    }
    
    static {
        Ruby.BUILTIN_LIBRARIES = new String[] { "fcntl", "yaml", "yaml/syck", "jsignal" };
        Ruby.securityRestricted = false;
    }
    
    public class CallTraceFuncHook implements EventHook
    {
        private RubyProc traceFunc;
        
        public void setTraceFunc(final RubyProc traceFunc) {
            this.traceFunc = traceFunc;
        }
        
        public void event(final ThreadContext context, final int event, String file, final int line, final String name, IRubyObject type) {
            if (!context.isWithinTrace()) {
                if (file == null) {
                    file = "(ruby)";
                }
                if (type == null) {
                    type = Ruby.this.getFalse();
                }
                final RubyBinding binding = RubyBinding.newBinding(Ruby.this);
                context.preTrace();
                try {
                    this.traceFunc.call(new IRubyObject[] { Ruby.this.newString(CallTraceFuncHook.EVENT_NAMES[event]), Ruby.this.newString(file), Ruby.this.newFixnum(line + 1), (name != null) ? RubySymbol.newSymbol(Ruby.this, name) : Ruby.this.getNil(), binding, type });
                }
                finally {
                    context.postTrace();
                }
            }
        }
        
        public boolean isInterestedInEvent(final int event) {
            return true;
        }
    }
}
