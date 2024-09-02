// 
// Decompiled by Procyon v0.5.36
// 

package org.elasticsearch.script;

import org.elasticsearch.watcher.AbstractResourceWatcher;
import java.io.IOException;
import java.io.Reader;
import org.elasticsearch.common.io.Streams;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.elasticsearch.common.base.Charsets;
import java.io.FileInputStream;
import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.watcher.FileChangesListener;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.index.fielddata.IndexFieldDataService;
import org.elasticsearch.index.mapper.MapperService;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.search.lookup.SearchLookup;
import java.util.Map;
import org.elasticsearch.ElasticSearchIllegalArgumentException;
import org.elasticsearch.common.inject.Inject;
import java.util.Iterator;
import org.elasticsearch.watcher.ResourceWatcher;
import org.elasticsearch.watcher.FileWatcher;
import java.util.concurrent.TimeUnit;
import org.elasticsearch.common.cache.CacheBuilder;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.util.concurrent.ConcurrentCollections;
import org.elasticsearch.watcher.ResourceWatcherService;
import java.util.Set;
import org.elasticsearch.env.Environment;
import org.elasticsearch.common.settings.Settings;
import java.io.File;
import org.elasticsearch.common.cache.Cache;
import java.util.concurrent.ConcurrentMap;
import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.component.AbstractComponent;

public class ScriptService extends AbstractComponent
{
    private final String defaultLang;
    private final ImmutableMap<String, ScriptEngineService> scriptEngines;
    private final ConcurrentMap<String, CompiledScript> staticCache;
    private final Cache<CacheKey, CompiledScript> cache;
    private final File scriptsDirectory;
    private final boolean disableDynamic;
    
    @Inject
    public ScriptService(final Settings settings, final Environment env, final Set<ScriptEngineService> scriptEngines, final ResourceWatcherService resourceWatcherService) {
        super(settings);
        this.staticCache = ConcurrentCollections.newConcurrentMap();
        final int cacheMaxSize = this.componentSettings.getAsInt("cache.max_size", 500);
        final TimeValue cacheExpire = this.componentSettings.getAsTime("cache.expire", null);
        this.logger.debug("using script cache with max_size [{}], expire [{}]", cacheMaxSize, cacheExpire);
        this.defaultLang = this.componentSettings.get("default_lang", "mvel");
        this.disableDynamic = this.componentSettings.getAsBoolean("disable_dynamic", false);
        final CacheBuilder cacheBuilder = CacheBuilder.newBuilder();
        if (cacheMaxSize >= 0) {
            cacheBuilder.maximumSize(cacheMaxSize);
        }
        if (cacheExpire != null) {
            cacheBuilder.expireAfterAccess(cacheExpire.nanos(), TimeUnit.NANOSECONDS);
        }
        this.cache = cacheBuilder.build();
        final ImmutableMap.Builder<String, ScriptEngineService> builder = ImmutableMap.builder();
        for (final ScriptEngineService scriptEngine : scriptEngines) {
            for (final String type : scriptEngine.types()) {
                builder.put(type, scriptEngine);
            }
        }
        this.scriptEngines = builder.build();
        this.staticCache.put("doc.score", new CompiledScript("native", new DocScoreNativeScriptFactory()));
        this.scriptsDirectory = new File(env.configFile(), "scripts");
        final FileWatcher fileWatcher = new FileWatcher(this.scriptsDirectory);
        ((AbstractResourceWatcher<ScriptChangesListener>)fileWatcher).addListener(new ScriptChangesListener());
        if (this.componentSettings.getAsBoolean("auto_reload_enabled", true)) {
            resourceWatcherService.add(fileWatcher);
        }
        else {
            fileWatcher.init();
        }
    }
    
    public void close() {
        for (final ScriptEngineService engineService : this.scriptEngines.values()) {
            engineService.close();
        }
    }
    
    public CompiledScript compile(final String script) {
        return this.compile(this.defaultLang, script);
    }
    
    public CompiledScript compile(String lang, final String script) {
        CompiledScript compiled = this.staticCache.get(script);
        if (compiled != null) {
            return compiled;
        }
        if (lang == null) {
            lang = this.defaultLang;
        }
        if (this.dynamicScriptDisabled(lang)) {
            throw new ScriptException("dynamic scripting disabled");
        }
        final CacheKey cacheKey = new CacheKey(lang, script);
        compiled = this.cache.getIfPresent(cacheKey);
        if (compiled != null) {
            return compiled;
        }
        final ScriptEngineService service = this.scriptEngines.get(lang);
        if (service == null) {
            throw new ElasticSearchIllegalArgumentException("script_lang not supported [" + lang + "]");
        }
        compiled = new CompiledScript(lang, service.compile(script));
        this.cache.put(cacheKey, compiled);
        return compiled;
    }
    
    public ExecutableScript executable(final String lang, final String script, final Map vars) {
        return this.executable(this.compile(lang, script), vars);
    }
    
    public ExecutableScript executable(final CompiledScript compiledScript, final Map vars) {
        return this.scriptEngines.get(compiledScript.lang()).executable(compiledScript.compiled(), vars);
    }
    
    public SearchScript search(final CompiledScript compiledScript, final SearchLookup lookup, @Nullable final Map<String, Object> vars) {
        return this.scriptEngines.get(compiledScript.lang()).search(compiledScript.compiled(), lookup, vars);
    }
    
    public SearchScript search(final SearchLookup lookup, final String lang, final String script, @Nullable final Map<String, Object> vars) {
        return this.search(this.compile(lang, script), lookup, vars);
    }
    
    public SearchScript search(final MapperService mapperService, final IndexFieldDataService fieldDataService, final String lang, final String script, @Nullable final Map<String, Object> vars) {
        return this.search(this.compile(lang, script), new SearchLookup(mapperService, fieldDataService, null), vars);
    }
    
    public Object execute(final CompiledScript compiledScript, final Map vars) {
        return this.scriptEngines.get(compiledScript.lang()).execute(compiledScript.compiled(), vars);
    }
    
    public void clear() {
        this.cache.invalidateAll();
    }
    
    private boolean dynamicScriptDisabled(final String lang) {
        return this.disableDynamic && !"native".equals(lang);
    }
    
    private class ScriptChangesListener extends FileChangesListener
    {
        private Tuple<String, String> scriptNameExt(final File file) {
            final String scriptPath = ScriptService.this.scriptsDirectory.toURI().relativize(file.toURI()).getPath();
            final int extIndex = scriptPath.lastIndexOf(46);
            if (extIndex != -1) {
                final String ext = scriptPath.substring(extIndex + 1);
                final String scriptName = scriptPath.substring(0, extIndex).replace(File.separatorChar, '_');
                return new Tuple<String, String>(scriptName, ext);
            }
            return null;
        }
        
        @Override
        public void onFileInit(final File file) {
            final Tuple<String, String> scriptNameExt = this.scriptNameExt(file);
            if (scriptNameExt != null) {
                boolean found = false;
                for (final ScriptEngineService engineService : ScriptService.this.scriptEngines.values()) {
                    for (final String s : engineService.extensions()) {
                        if (s.equals(scriptNameExt.v2())) {
                            found = true;
                            try {
                                ScriptService.this.logger.trace("compiling script file " + file.getAbsolutePath(), new Object[0]);
                                final String script = Streams.copyToString(new InputStreamReader(new FileInputStream(file), Charsets.UTF_8));
                                ScriptService.this.staticCache.put(scriptNameExt.v1(), new CompiledScript(engineService.types()[0], engineService.compile(script)));
                            }
                            catch (Throwable e) {
                                ScriptService.this.logger.warn("failed to load/compile script [{}]", e, scriptNameExt.v1());
                            }
                            break;
                        }
                    }
                    if (found) {
                        break;
                    }
                }
                if (!found) {
                    ScriptService.this.logger.warn("no script engine found for [{}]", scriptNameExt.v2());
                }
            }
        }
        
        @Override
        public void onFileCreated(final File file) {
            this.onFileInit(file);
        }
        
        @Override
        public void onFileDeleted(final File file) {
            final Tuple<String, String> scriptNameExt = this.scriptNameExt(file);
            ScriptService.this.logger.trace("removing script file " + file.getAbsolutePath(), new Object[0]);
            ScriptService.this.staticCache.remove(scriptNameExt.v1());
        }
        
        @Override
        public void onFileChanged(final File file) {
            this.onFileInit(file);
        }
    }
    
    public static class CacheKey
    {
        public final String lang;
        public final String script;
        
        public CacheKey(final String lang, final String script) {
            this.lang = lang;
            this.script = script;
        }
        
        @Override
        public boolean equals(final Object o) {
            final CacheKey other = (CacheKey)o;
            return this.lang.equals(other.lang) && this.script.equals(other.script);
        }
        
        @Override
        public int hashCode() {
            return this.lang.hashCode() + 31 * this.script.hashCode();
        }
    }
    
    public static class DocScoreNativeScriptFactory implements NativeScriptFactory
    {
        @Override
        public ExecutableScript newScript(@Nullable final Map<String, Object> params) {
            return new DocScoreSearchScript();
        }
    }
    
    public static class DocScoreSearchScript extends AbstractFloatSearchScript
    {
        @Override
        public float runAsFloat() {
            try {
                return this.doc().score();
            }
            catch (IOException e) {
                return 0.0f;
            }
        }
    }
}
