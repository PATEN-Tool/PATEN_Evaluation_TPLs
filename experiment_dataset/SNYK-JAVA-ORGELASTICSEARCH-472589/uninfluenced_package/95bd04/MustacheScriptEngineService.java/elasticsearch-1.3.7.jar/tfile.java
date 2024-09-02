// 
// Decompiled by Procyon v0.5.36
// 

package org.elasticsearch.script.mustache;

import java.util.Collections;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.script.CompiledScript;
import org.elasticsearch.script.SearchScript;
import org.elasticsearch.search.lookup.SearchLookup;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.common.Nullable;
import java.io.IOException;
import java.io.Writer;
import org.elasticsearch.common.mustache.Mustache;
import java.io.OutputStream;
import org.elasticsearch.common.io.stream.BytesStreamOutput;
import java.util.Map;
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
        return new JsonEscapingMustacheFactory().compile(new FastStringReader(template), "query-template");
    }
    
    @Override
    public Object execute(final Object template, final Map<String, Object> vars) {
        final BytesStreamOutput result = new BytesStreamOutput();
        final UTF8StreamWriter writer = utf8StreamWriter().setOutput(result);
        ((Mustache)template).execute(writer, vars);
        try {
            writer.flush();
        }
        catch (IOException e) {
            this.logger.error("Could not execute query template (failed to flush writer): ", e, new Object[0]);
            try {
                writer.close();
            }
            catch (IOException e) {
                this.logger.error("Could not execute query template (failed to close writer): ", e, new Object[0]);
            }
        }
        finally {
            try {
                writer.close();
            }
            catch (IOException e2) {
                this.logger.error("Could not execute query template (failed to close writer): ", e2, new Object[0]);
            }
        }
        return result.bytes();
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
    public ExecutableScript executable(final Object mustache, @Nullable final Map<String, Object> vars) {
        return new MustacheExecutableScript((Mustache)mustache, vars);
    }
    
    @Override
    public SearchScript search(final Object compiledScript, final SearchLookup lookup, @Nullable final Map<String, Object> vars) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public Object unwrap(final Object value) {
        return value;
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
        private Mustache mustache;
        private Map<String, Object> vars;
        
        public MustacheExecutableScript(final Mustache mustache, final Map<String, Object> vars) {
            this.mustache = mustache;
            this.vars = ((vars == null) ? Collections.EMPTY_MAP : vars);
        }
        
        @Override
        public void setNextVar(final String name, final Object value) {
            this.vars.put(name, value);
        }
        
        @Override
        public Object run() {
            final BytesStreamOutput result = new BytesStreamOutput();
            final UTF8StreamWriter writer = utf8StreamWriter().setOutput(result);
            this.mustache.execute(writer, this.vars);
            try {
                writer.flush();
            }
            catch (IOException e) {
                MustacheScriptEngineService.this.logger.error("Could not execute query template (failed to flush writer): ", e, new Object[0]);
                try {
                    writer.close();
                }
                catch (IOException e) {
                    MustacheScriptEngineService.this.logger.error("Could not execute query template (failed to close writer): ", e, new Object[0]);
                }
            }
            finally {
                try {
                    writer.close();
                }
                catch (IOException e2) {
                    MustacheScriptEngineService.this.logger.error("Could not execute query template (failed to close writer): ", e2, new Object[0]);
                }
            }
            return result.bytes();
        }
        
        @Override
        public Object unwrap(final Object value) {
            return value;
        }
    }
}
