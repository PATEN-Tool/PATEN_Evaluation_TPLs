// 
// Decompiled by Procyon v0.5.36
// 

package org.elasticsearch.script;

import java.io.IOException;
import org.apache.lucene.index.LeafReaderContext;
import org.elasticsearch.search.lookup.SearchLookup;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.inject.Inject;
import java.util.Map;
import org.elasticsearch.common.settings.Settings;
import com.google.common.collect.ImmutableMap;
import org.elasticsearch.common.component.AbstractComponent;

public class NativeScriptEngineService extends AbstractComponent implements ScriptEngineService
{
    public static final String NAME = "native";
    private final ImmutableMap<String, NativeScriptFactory> scripts;
    
    @Inject
    public NativeScriptEngineService(final Settings settings, final Map<String, NativeScriptFactory> scripts) {
        super(settings);
        this.scripts = (ImmutableMap<String, NativeScriptFactory>)ImmutableMap.copyOf((Map)scripts);
    }
    
    @Override
    public String[] types() {
        return new String[] { "native" };
    }
    
    @Override
    public String[] extensions() {
        return new String[0];
    }
    
    @Override
    public boolean sandboxed() {
        return false;
    }
    
    @Override
    public Object compile(final String script) {
        final NativeScriptFactory scriptFactory = (NativeScriptFactory)this.scripts.get((Object)script);
        if (scriptFactory != null) {
            return scriptFactory;
        }
        throw new IllegalArgumentException("Native script [" + script + "] not found");
    }
    
    @Override
    public ExecutableScript executable(final CompiledScript compiledScript, @Nullable final Map<String, Object> vars) {
        final NativeScriptFactory scriptFactory = (NativeScriptFactory)compiledScript.compiled();
        return scriptFactory.newScript(vars);
    }
    
    @Override
    public SearchScript search(final CompiledScript compiledScript, final SearchLookup lookup, @Nullable final Map<String, Object> vars) {
        final NativeScriptFactory scriptFactory = (NativeScriptFactory)compiledScript.compiled();
        return new SearchScript() {
            @Override
            public LeafSearchScript getLeafSearchScript(final LeafReaderContext context) throws IOException {
                final AbstractSearchScript script = (AbstractSearchScript)scriptFactory.newScript(vars);
                script.setLookup(lookup.getLeafSearchLookup(context));
                return script;
            }
            
            @Override
            public boolean needsScores() {
                return scriptFactory.needsScores();
            }
        };
    }
    
    @Override
    public Object execute(final CompiledScript compiledScript, final Map<String, Object> vars) {
        return this.executable(compiledScript, vars).run();
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
}
