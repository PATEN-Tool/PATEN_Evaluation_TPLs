// 
// Decompiled by Procyon v0.5.36
// 

package org.elasticsearch.script.mvel;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.search.Scorer;
import java.util.Iterator;
import org.elasticsearch.common.mvel2.integration.VariableResolverFactory;
import java.util.HashMap;
import org.elasticsearch.common.mvel2.integration.impl.MapVariableResolverFactory;
import org.elasticsearch.common.mvel2.compiler.ExecutableStatement;
import org.elasticsearch.script.SearchScript;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.search.lookup.SearchLookup;
import org.elasticsearch.script.ExecutableScript;
import java.util.Map;
import org.elasticsearch.common.mvel2.ParserContext;
import org.elasticsearch.common.inject.Inject;
import java.lang.reflect.Method;
import org.elasticsearch.common.math.UnboxedMathUtils;
import org.elasticsearch.common.mvel2.MVEL;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.mvel2.ParserConfiguration;
import org.elasticsearch.script.ScriptEngineService;
import org.elasticsearch.common.component.AbstractComponent;

public class MvelScriptEngineService extends AbstractComponent implements ScriptEngineService
{
    private final ParserConfiguration parserConfiguration;
    
    @Inject
    public MvelScriptEngineService(final Settings settings) {
        super(settings);
        (this.parserConfiguration = new ParserConfiguration()).addPackageImport("java.util");
        this.parserConfiguration.addPackageImport("org.elasticsearch.common.joda.time");
        this.parserConfiguration.addImport("time", MVEL.getStaticMethod(System.class, "currentTimeMillis", new Class[0]));
        for (final Method m : UnboxedMathUtils.class.getMethods()) {
            if ((m.getModifiers() & 0x8) > 0) {
                this.parserConfiguration.addImport(m.getName(), m);
            }
        }
    }
    
    @Override
    public void close() {
    }
    
    @Override
    public String[] types() {
        return new String[] { "mvel" };
    }
    
    @Override
    public String[] extensions() {
        return new String[] { "mvel" };
    }
    
    @Override
    public boolean sandboxed() {
        return false;
    }
    
    @Override
    public Object compile(final String script) {
        return MVEL.compileExpression(script.trim(), new ParserContext(this.parserConfiguration));
    }
    
    @Override
    public Object execute(final Object compiledScript, final Map vars) {
        return MVEL.executeExpression(compiledScript, vars);
    }
    
    @Override
    public ExecutableScript executable(final Object compiledScript, final Map vars) {
        return new MvelExecutableScript(compiledScript, vars);
    }
    
    @Override
    public SearchScript search(final Object compiledScript, final SearchLookup lookup, @Nullable final Map<String, Object> vars) {
        return new MvelSearchScript(compiledScript, lookup, vars);
    }
    
    @Override
    public Object unwrap(final Object value) {
        return value;
    }
    
    public static class MvelExecutableScript implements ExecutableScript
    {
        private final ExecutableStatement script;
        private final MapVariableResolverFactory resolver;
        
        public MvelExecutableScript(final Object script, final Map vars) {
            this.script = (ExecutableStatement)script;
            if (vars != null) {
                this.resolver = new MapVariableResolverFactory(vars);
            }
            else {
                this.resolver = new MapVariableResolverFactory(new HashMap());
            }
        }
        
        @Override
        public void setNextVar(final String name, final Object value) {
            this.resolver.createVariable(name, value);
        }
        
        @Override
        public Object run() {
            return this.script.getValue(null, this.resolver);
        }
        
        @Override
        public Object unwrap(final Object value) {
            return value;
        }
    }
    
    public static class MvelSearchScript implements SearchScript
    {
        private final ExecutableStatement script;
        private final SearchLookup lookup;
        private final MapVariableResolverFactory resolver;
        
        public MvelSearchScript(final Object script, final SearchLookup lookup, final Map<String, Object> vars) {
            this.script = (ExecutableStatement)script;
            this.lookup = lookup;
            if (vars != null) {
                this.resolver = new MapVariableResolverFactory(vars);
            }
            else {
                this.resolver = new MapVariableResolverFactory(new HashMap());
            }
            for (final Map.Entry<String, Object> entry : lookup.asMap().entrySet()) {
                this.resolver.createVariable(entry.getKey(), entry.getValue());
            }
        }
        
        @Override
        public void setScorer(final Scorer scorer) {
            this.lookup.setScorer(scorer);
        }
        
        @Override
        public void setNextReader(final AtomicReaderContext context) {
            this.lookup.setNextReader(context);
        }
        
        @Override
        public void setNextDocId(final int doc) {
            this.lookup.setNextDocId(doc);
        }
        
        @Override
        public void setNextScore(final float score) {
            this.resolver.createVariable("_score", score);
        }
        
        @Override
        public void setNextVar(final String name, final Object value) {
            this.resolver.createVariable(name, value);
        }
        
        @Override
        public void setNextSource(final Map<String, Object> source) {
            this.lookup.source().setNextSource(source);
        }
        
        @Override
        public Object run() {
            return this.script.getValue(null, this.resolver);
        }
        
        @Override
        public float runAsFloat() {
            return ((Number)this.run()).floatValue();
        }
        
        @Override
        public long runAsLong() {
            return ((Number)this.run()).longValue();
        }
        
        @Override
        public double runAsDouble() {
            return ((Number)this.run()).doubleValue();
        }
        
        @Override
        public Object unwrap(final Object value) {
            return value;
        }
    }
}
