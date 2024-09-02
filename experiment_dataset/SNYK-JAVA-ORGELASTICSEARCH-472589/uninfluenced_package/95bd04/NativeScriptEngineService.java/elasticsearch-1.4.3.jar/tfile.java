// 
// Decompiled by Procyon v0.5.36
// 

package org.elasticsearch.script;

import org.elasticsearch.search.lookup.SearchLookup;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.ElasticsearchIllegalArgumentException;
import org.elasticsearch.common.inject.Inject;
import java.util.Map;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.component.AbstractComponent;

public class NativeScriptEngineService extends AbstractComponent implements ScriptEngineService
{
    private final ImmutableMap<String, NativeScriptFactory> scripts;
    
    @Inject
    public NativeScriptEngineService(final Settings settings, final Map<String, NativeScriptFactory> scripts) {
        super(settings);
        this.scripts = ImmutableMap.copyOf((Map<? extends String, ? extends NativeScriptFactory>)scripts);
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
        final NativeScriptFactory scriptFactory = this.scripts.get(script);
        if (scriptFactory != null) {
            return scriptFactory;
        }
        throw new ElasticsearchIllegalArgumentException("Native script [" + script + "] not found");
    }
    
    @Override
    public ExecutableScript executable(final Object compiledScript, @Nullable final Map<String, Object> vars) {
        final NativeScriptFactory scriptFactory = (NativeScriptFactory)compiledScript;
        return scriptFactory.newScript(vars);
    }
    
    @Override
    public SearchScript search(final Object compiledScript, final SearchLookup lookup, @Nullable final Map<String, Object> vars) {
        final NativeScriptFactory scriptFactory = (NativeScriptFactory)compiledScript;
        final AbstractSearchScript script = (AbstractSearchScript)scriptFactory.newScript(vars);
        script.setLookup(lookup);
        return script;
    }
    
    @Override
    public Object execute(final Object compiledScript, final Map<String, Object> vars) {
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
