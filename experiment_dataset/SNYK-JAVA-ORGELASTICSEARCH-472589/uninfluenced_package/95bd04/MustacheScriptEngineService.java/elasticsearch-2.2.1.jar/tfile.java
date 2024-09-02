// 
// Decompiled by Procyon v0.5.36
// 

package org.elasticsearch.script.mustache;

import org.elasticsearch.script.ScriptException;
import java.io.Writer;
import com.github.mustachejava.Mustache;
import java.io.OutputStream;
import org.elasticsearch.common.io.stream.BytesStreamOutput;
import java.util.Collections;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.script.SearchScript;
import org.elasticsearch.search.lookup.SearchLookup;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.common.Nullable;
import java.util.Map;
import org.elasticsearch.script.CompiledScript;
import java.io.Reader;
import org.elasticsearch.common.io.FastStringReader;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.io.UTF8StreamWriter;
import java.lang.ref.SoftReference;
import org.elasticsearch.script.ScriptEngineService;
import org.elasticsearch.common.component.AbstractComponent;

public class MustacheScriptEngineService extends AbstractComponent implements ScriptEngineService
{
    public static final String NAME = "mustache";
    private static ThreadLocal<SoftReference<UTF8StreamWriter>> utf8StreamWriter;
    
    private static UTF8StreamWriter utf8StreamWriter() {
        final SoftReference<UTF8StreamWriter> ref = MustacheScriptEngineService.utf8StreamWriter.get();
        UTF8StreamWriter writer = (ref == null) ? null : ref.get();
        if (writer == null) {
            writer = new UTF8StreamWriter(4096);
            MustacheScriptEngineService.utf8StreamWriter.set(new SoftReference<UTF8StreamWriter>(writer));
        }
        writer.reset();
        return writer;
    }
    
    @Inject
    public MustacheScriptEngineService(final Settings settings) {
        super(settings);
    }
    
    @Override
    public Object compile(final String template) {
        return new JsonEscapingMustacheFactory().compile((Reader)new FastStringReader(template), "query-template");
    }
    
    @Override
    public String[] types() {
        return new String[] { "mustache" };
    }
    
    @Override
    public String[] extensions() {
        return new String[] { "mustache" };
    }
    
    @Override
    public boolean sandboxed() {
        return true;
    }
    
    @Override
    public ExecutableScript executable(final CompiledScript compiledScript, @Nullable final Map<String, Object> vars) {
        return new MustacheExecutableScript(compiledScript, vars);
    }
    
    @Override
    public SearchScript search(final CompiledScript compiledScript, final SearchLookup lookup, @Nullable final Map<String, Object> vars) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void close() {
    }
    
    @Override
    public void scriptRemoved(final CompiledScript script) {
    }
    
    static {
        MustacheScriptEngineService.utf8StreamWriter = new ThreadLocal<SoftReference<UTF8StreamWriter>>();
    }
    
    private class MustacheExecutableScript implements ExecutableScript
    {
        private CompiledScript template;
        private Map<String, Object> vars;
        
        public MustacheExecutableScript(final CompiledScript template, final Map<String, Object> vars) {
            this.template = template;
            this.vars = ((vars == null) ? Collections.emptyMap() : vars);
        }
        
        @Override
        public void setNextVar(final String name, final Object value) {
            this.vars.put(name, value);
        }
        
        @Override
        public Object run() {
            final BytesStreamOutput result = new BytesStreamOutput();
            try (final UTF8StreamWriter writer = utf8StreamWriter().setOutput(result)) {
                ((Mustache)this.template.compiled()).execute((Writer)writer, (Object)this.vars);
            }
            catch (Exception e) {
                MustacheScriptEngineService.this.logger.error("Error running " + this.template, e, new Object[0]);
                throw new ScriptException("Error running " + this.template, e);
            }
            return result.bytes();
        }
        
        @Override
        public Object unwrap(final Object value) {
            return value;
        }
    }
}
