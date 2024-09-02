// 
// Decompiled by Procyon v0.5.36
// 

package delight.nashornsandbox.internal;

import org.slf4j.LoggerFactory;
import java.io.Writer;
import delight.nashornsandbox.exceptions.ScriptMemoryAbuseException;
import javax.script.ScriptException;
import delight.nashornsandbox.exceptions.ScriptCPUAbuseException;
import javax.script.ScriptContext;
import javax.script.Bindings;
import jdk.nashorn.api.scripting.ClassFilter;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import delight.nashornsandbox.SecuredJsCache;
import javax.script.Invocable;
import java.util.concurrent.ExecutorService;
import javax.script.ScriptEngine;
import org.slf4j.Logger;
import delight.nashornsandbox.NashornSandbox;

public class NashornSandboxImpl implements NashornSandbox
{
    static final Logger LOG;
    protected final SandboxClassFilter sandboxClassFilter;
    protected final ScriptEngine scriptEngine;
    protected long maxCPUTime;
    protected long maxMemory;
    protected ExecutorService executor;
    protected boolean allowPrintFunctions;
    protected boolean allowReadFunctions;
    protected boolean allowLoadFunctions;
    protected boolean allowExitFunctions;
    protected boolean allowGlobalsObjects;
    protected boolean allowNoBraces;
    protected JsEvaluator evaluator;
    protected JsSanitizer sanitizer;
    protected boolean engineAsserted;
    protected Invocable lazyInvocable;
    protected int maxPreparedStatements;
    protected SecuredJsCache suppliedCache;
    
    public NashornSandboxImpl() {
        this(new String[0]);
    }
    
    public NashornSandboxImpl(final String... params) {
        this.maxCPUTime = 0L;
        this.maxMemory = 0L;
        this.allowPrintFunctions = false;
        this.allowReadFunctions = false;
        this.allowLoadFunctions = false;
        this.allowExitFunctions = false;
        this.allowGlobalsObjects = false;
        this.allowNoBraces = false;
        this.maxPreparedStatements = 0;
        this.sandboxClassFilter = new SandboxClassFilter();
        final NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
        for (final String param : params) {
            if (param.equals("--no-java")) {
                throw new IllegalArgumentException("The engine parameter --no-java is not supported. Using it would interfere with the injected code to test for infinite loops.");
            }
        }
        this.scriptEngine = factory.getScriptEngine(params, this.getClass().getClassLoader(), this.sandboxClassFilter);
        this.allow(InterruptTest.class);
    }
    
    private void assertScriptEngine() {
        this.assertScriptEngine(this.scriptEngine.getBindings(100));
    }
    
    private void assertScriptEngine(final Bindings bindings) {
        try {
            this.scriptEngine.eval(this.preScript(), this.sanitizeBindings(bindings));
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    private String preScript() {
        final StringBuilder sb = new StringBuilder();
        if (!this.allowExitFunctions) {
            sb.append("var quit=function(){};var exit=function(){};");
        }
        if (!this.allowPrintFunctions) {
            sb.append("var print=function(){};var echo = function(){};");
        }
        if (!this.allowReadFunctions) {
            sb.append("var readFully=function(){};").append("var readLine=function(){};");
        }
        if (!this.allowLoadFunctions) {
            sb.append("var load=function(){};var loadWithNewGlobal=function(){};");
        }
        if (!this.allowGlobalsObjects) {
            sb.append("var $ARG=null;var $ENV=null;var $EXEC=null;");
            sb.append("var $OPTIONS=null;var $OUT=null;var $ERR=null;var $EXIT=null;");
        }
        return sb.toString();
    }
    
    private Bindings sanitizeBindings(final Bindings bindings) {
        if (!this.allowExitFunctions) {
            bindings.remove("quit");
            bindings.remove("exit");
        }
        if (!this.allowPrintFunctions) {
            bindings.remove("print");
            bindings.remove("echo");
        }
        if (!this.allowReadFunctions) {
            bindings.remove("readFully");
            bindings.remove("readLine");
        }
        if (!this.allowLoadFunctions) {
            bindings.remove("load");
            bindings.remove("loadWithNewGlobal");
        }
        return bindings;
    }
    
    @Override
    public Object eval(final String js) throws ScriptCPUAbuseException, ScriptException {
        return this.eval(js, null, null);
    }
    
    @Override
    public Object eval(final String js, final Bindings bindings) throws ScriptCPUAbuseException, ScriptException {
        return this.eval(js, null, bindings);
    }
    
    @Override
    public Object eval(final String js, final ScriptContext scriptContext) throws ScriptCPUAbuseException, ScriptException {
        return this.eval(js, scriptContext, null);
    }
    
    @Override
    public Object eval(final String js, final ScriptContext scriptContext, final Bindings bindings) throws ScriptCPUAbuseException, ScriptException {
        final JsSanitizer sanitizer = this.getSanitizer();
        final String securedJs = sanitizer.secureJs(js);
        final EvaluateOperation op = new EvaluateOperation(securedJs, scriptContext, bindings);
        return this.executeSandboxedOperation(op, bindings);
    }
    
    private Object executeSandboxedOperation(final ScriptEngineOperation op) throws ScriptCPUAbuseException, ScriptException {
        return this.executeSandboxedOperation(op, null);
    }
    
    private Object executeSandboxedOperation(final ScriptEngineOperation op, final Bindings bindings) throws ScriptCPUAbuseException, ScriptException {
        if (!this.engineAsserted) {
            this.engineAsserted = true;
            this.assertScriptEngine();
        }
        if (bindings != null) {
            this.assertScriptEngine(bindings);
        }
        try {
            if (this.maxCPUTime == 0L && this.maxMemory == 0L) {
                return op.executeScriptEngineOperation(this.scriptEngine);
            }
            this.checkExecutorPresence();
            final JsEvaluator evaluator = this.getEvaluator(op);
            this.executor.execute(evaluator);
            evaluator.runMonitor();
            if (evaluator.isCPULimitExceeded()) {
                throw new ScriptCPUAbuseException("Script used more than the allowed [" + this.maxCPUTime + " ms] of CPU time.", evaluator.isScriptKilled(), evaluator.getException());
            }
            if (evaluator.isMemoryLimitExceeded()) {
                throw new ScriptMemoryAbuseException("Script used more than the allowed [" + this.maxMemory + " B] of memory.", evaluator.isScriptKilled(), evaluator.getException());
            }
            if (evaluator.getException() != null) {
                throw evaluator.getException();
            }
            return evaluator.getResult();
        }
        catch (RuntimeException | ScriptException ex2) {
            final Exception ex;
            final Exception e = ex;
            throw e;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    private JsEvaluator getEvaluator(final ScriptEngineOperation op) {
        return new JsEvaluator(this.scriptEngine, this.maxCPUTime, this.maxMemory, op);
    }
    
    private void checkExecutorPresence() {
        if (this.executor == null) {
            throw new IllegalStateException("When a CPU time or memory limit is set, an executor needs to be provided by calling .setExecutor(...)");
        }
    }
    
    @Override
    public void setMaxCPUTime(final long limit) {
        this.maxCPUTime = limit;
    }
    
    @Override
    public void setMaxMemory(final long limit) {
        this.maxMemory = limit;
    }
    
    JsSanitizer getSanitizer() {
        if (this.sanitizer == null) {
            if (this.suppliedCache == null) {
                this.sanitizer = new JsSanitizer(this.scriptEngine, this.maxPreparedStatements, this.allowNoBraces);
            }
            else {
                this.sanitizer = new JsSanitizer(this.scriptEngine, this.allowNoBraces, this.suppliedCache);
            }
        }
        return this.sanitizer;
    }
    
    @Override
    public void allow(final Class<?> clazz) {
        this.sandboxClassFilter.add(clazz);
    }
    
    @Override
    public void disallow(final Class<?> clazz) {
        this.sandboxClassFilter.remove(clazz);
    }
    
    @Override
    public boolean isAllowed(final Class<?> clazz) {
        return this.sandboxClassFilter.contains(clazz);
    }
    
    @Override
    public void disallowAllClasses() {
        this.sandboxClassFilter.clear();
        this.allow(InterruptTest.class);
    }
    
    @Override
    public void inject(final String variableName, final Object object) {
        if (object != null && !this.sandboxClassFilter.contains(object.getClass())) {
            this.allow(object.getClass());
        }
        this.scriptEngine.put(variableName, object);
    }
    
    @Override
    public void setExecutor(final ExecutorService executor) {
        this.executor = executor;
    }
    
    @Override
    public ExecutorService getExecutor() {
        return this.executor;
    }
    
    @Override
    public Object get(final String variableName) {
        return this.scriptEngine.get(variableName);
    }
    
    @Override
    public void allowPrintFunctions(final boolean v) {
        if (this.engineAsserted) {
            throw new IllegalStateException("Please set this property before calling eval.");
        }
        this.allowPrintFunctions = v;
    }
    
    @Override
    public void allowReadFunctions(final boolean v) {
        if (this.engineAsserted) {
            throw new IllegalStateException("Please set this property before calling eval.");
        }
        this.allowReadFunctions = v;
    }
    
    @Override
    public void allowLoadFunctions(final boolean v) {
        if (this.engineAsserted) {
            throw new IllegalStateException("Please set this property before calling eval.");
        }
        this.allowLoadFunctions = v;
    }
    
    @Override
    public void allowExitFunctions(final boolean v) {
        if (this.engineAsserted) {
            throw new IllegalStateException("Please set this property before calling eval.");
        }
        this.allowExitFunctions = v;
    }
    
    @Override
    public void allowGlobalsObjects(final boolean v) {
        if (this.engineAsserted) {
            throw new IllegalStateException("Please set this property before calling eval.");
        }
        this.allowGlobalsObjects = v;
    }
    
    @Override
    public void allowNoBraces(final boolean v) {
        if (this.allowNoBraces != v) {
            this.sanitizer = null;
        }
        this.allowNoBraces = v;
    }
    
    @Override
    public void setWriter(final Writer writer) {
        this.scriptEngine.getContext().setWriter(writer);
    }
    
    @Override
    public void setMaxPreparedStatements(final int max) {
        if (this.maxPreparedStatements != max) {
            this.sanitizer = null;
        }
        this.maxPreparedStatements = max;
    }
    
    @Override
    public Bindings createBindings() {
        return this.scriptEngine.createBindings();
    }
    
    @Override
    public Invocable getSandboxedInvocable() {
        if (this.maxMemory == 0L && this.maxCPUTime == 0L) {
            return (Invocable)this.scriptEngine;
        }
        return this.getLazySandboxedInvocable();
    }
    
    private Invocable getLazySandboxedInvocable() {
        if (this.lazyInvocable == null) {
            final Invocable sandboxInvocable = new Invocable() {
                @Override
                public Object invokeMethod(final Object thiz, final String name, final Object... args) throws ScriptException, NoSuchMethodException {
                    final InvokeOperation op = new InvokeOperation(thiz, name, args);
                    try {
                        return NashornSandboxImpl.this.executeSandboxedOperation(op);
                    }
                    catch (ScriptException e) {
                        throw e;
                    }
                    catch (Exception e2) {
                        throw new ScriptException(e2);
                    }
                }
                
                @Override
                public Object invokeFunction(final String name, final Object... args) throws ScriptException, NoSuchMethodException {
                    final InvokeOperation op = new InvokeOperation(null, name, args);
                    try {
                        return NashornSandboxImpl.this.executeSandboxedOperation(op);
                    }
                    catch (ScriptException e) {
                        throw e;
                    }
                    catch (Exception e2) {
                        throw new ScriptException(e2);
                    }
                }
                
                @Override
                public <T> T getInterface(final Object thiz, final Class<T> clasz) {
                    throw new IllegalStateException("Not yet implemented");
                }
                
                @Override
                public <T> T getInterface(final Class<T> clasz) {
                    throw new IllegalStateException("Not yet implemented");
                }
            };
            this.lazyInvocable = sandboxInvocable;
        }
        return this.lazyInvocable;
    }
    
    @Override
    public void setScriptCache(final SecuredJsCache cache) {
        this.suppliedCache = cache;
    }
    
    static {
        LOG = LoggerFactory.getLogger((Class)NashornSandbox.class);
    }
}
