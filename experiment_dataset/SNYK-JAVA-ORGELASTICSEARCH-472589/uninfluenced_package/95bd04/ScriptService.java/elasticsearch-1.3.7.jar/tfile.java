// 
// Decompiled by Procyon v0.5.36
// 

package org.elasticsearch.script;

import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.watcher.AbstractResourceWatcher;
import java.io.Reader;
import org.elasticsearch.common.io.Streams;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.elasticsearch.common.base.Charsets;
import java.io.FileInputStream;
import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.watcher.FileChangesListener;
import org.elasticsearch.common.cache.RemovalNotification;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.StreamInput;
import java.util.Locale;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.index.fielddata.IndexFieldDataService;
import org.elasticsearch.index.mapper.MapperService;
import org.elasticsearch.search.lookup.SearchLookup;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.ElasticsearchIllegalStateException;
import java.util.Map;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.common.Nullable;
import java.io.IOException;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.Strings;
import org.elasticsearch.index.query.TemplateQueryParser;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.indexedscripts.get.GetIndexedScriptRequest;
import org.elasticsearch.index.VersionType;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.ElasticsearchIllegalArgumentException;
import org.elasticsearch.common.inject.Inject;
import java.util.Iterator;
import org.elasticsearch.watcher.ResourceWatcher;
import org.elasticsearch.watcher.FileWatcher;
import org.elasticsearch.common.cache.RemovalListener;
import java.util.concurrent.TimeUnit;
import org.elasticsearch.common.cache.CacheBuilder;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.util.concurrent.ConcurrentCollections;
import org.elasticsearch.watcher.ResourceWatcherService;
import java.util.Set;
import org.elasticsearch.env.Environment;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.client.Client;
import java.io.File;
import org.elasticsearch.common.cache.Cache;
import java.util.concurrent.ConcurrentMap;
import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.component.AbstractComponent;

public class ScriptService extends AbstractComponent
{
    public static final String DEFAULT_SCRIPTING_LANGUAGE_SETTING = "script.default_lang";
    public static final String DISABLE_DYNAMIC_SCRIPTING_SETTING = "script.disable_dynamic";
    public static final String DISABLE_DYNAMIC_SCRIPTING_DEFAULT = "sandbox";
    public static final String SCRIPT_INDEX = ".scripts";
    private static String defaultLang;
    private final ImmutableMap<String, ScriptEngineService> scriptEngines;
    private final ConcurrentMap<String, CompiledScript> staticCache;
    private final Cache<CacheKey, CompiledScript> cache;
    private final File scriptsDirectory;
    private final DynamicScriptDisabling dynamicScriptingDisabled;
    private Client client;
    public static final ParseField SCRIPT_LANG;
    public static final ParseField VALUE_SCRIPT_FILE;
    public static final ParseField VALUE_SCRIPT_ID;
    public static final ParseField VALUE_SCRIPT_INLINE;
    public static final ParseField KEY_SCRIPT_FILE;
    public static final ParseField KEY_SCRIPT_ID;
    public static final ParseField KEY_SCRIPT_INLINE;
    public static final ParseField SCRIPT_FILE;
    public static final ParseField SCRIPT_ID;
    public static final ParseField SCRIPT_INLINE;
    
    @Inject
    public ScriptService(final Settings settings, final Environment env, final Set<ScriptEngineService> scriptEngines, final ResourceWatcherService resourceWatcherService) {
        super(settings);
        this.staticCache = ConcurrentCollections.newConcurrentMap();
        this.client = null;
        final int cacheMaxSize = this.componentSettings.getAsInt("cache.max_size", 100);
        final TimeValue cacheExpire = this.componentSettings.getAsTime("cache.expire", null);
        this.logger.debug("using script cache with max_size [{}], expire [{}]", cacheMaxSize, cacheExpire);
        ScriptService.defaultLang = settings.get("script.default_lang", "mvel");
        this.dynamicScriptingDisabled = DynamicScriptDisabling.parse(settings.get("script.disable_dynamic", "sandbox"));
        final CacheBuilder cacheBuilder = CacheBuilder.newBuilder();
        if (cacheMaxSize >= 0) {
            cacheBuilder.maximumSize(cacheMaxSize);
        }
        if (cacheExpire != null) {
            cacheBuilder.expireAfterAccess(cacheExpire.nanos(), TimeUnit.NANOSECONDS);
        }
        cacheBuilder.removalListener(new ScriptCacheRemovalListener());
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
        if (this.logger.isTraceEnabled()) {
            this.logger.trace("Using scripts directory [{}] ", this.scriptsDirectory);
        }
        final FileWatcher fileWatcher = new FileWatcher(this.scriptsDirectory);
        ((AbstractResourceWatcher<ScriptChangesListener>)fileWatcher).addListener(new ScriptChangesListener());
        if (this.componentSettings.getAsBoolean("auto_reload_enabled", true)) {
            resourceWatcherService.add(fileWatcher);
        }
        else {
            fileWatcher.init();
        }
    }
    
    @Inject(optional = true)
    public void setClient(final Client client) {
        this.client = client;
    }
    
    public void close() {
        for (final ScriptEngineService engineService : this.scriptEngines.values()) {
            engineService.close();
        }
    }
    
    public CompiledScript compile(final String script) {
        return this.compile(ScriptService.defaultLang, script);
    }
    
    public CompiledScript compile(final String lang, final String script) {
        return this.compile(lang, script, ScriptType.INLINE);
    }
    
    public CompiledScript compile(String lang, String script, final ScriptType scriptType) {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace("Compiling lang: [{}] type: [{}] script: {}", lang, scriptType, script);
        }
        if (scriptType == ScriptType.INDEXED) {
            if (this.client == null) {
                throw new ElasticsearchIllegalArgumentException("Got an indexed script with no Client registered.");
            }
            final IndexedScript indexedScript = new IndexedScript(lang, script);
            if (lang != null && !lang.equals(indexedScript.lang)) {
                this.logger.trace("Overriding lang to " + indexedScript.lang, new Object[0]);
                lang = indexedScript.lang;
            }
            this.verifyDynamicScripting(indexedScript.lang);
            script = this.getScriptFromIndex(this.client, indexedScript.lang, indexedScript.id);
        }
        else if (scriptType == ScriptType.FILE) {
            final CompiledScript compiled = this.staticCache.get(script);
            if (compiled != null) {
                return compiled;
            }
            throw new ElasticsearchIllegalArgumentException("Unable to find on disk script " + script);
        }
        if (scriptType != ScriptType.INDEXED) {
            final CompiledScript compiled = this.staticCache.get(script);
            if (compiled != null) {
                return compiled;
            }
        }
        if (lang == null) {
            lang = ScriptService.defaultLang;
        }
        this.verifyDynamicScripting(lang);
        CacheKey cacheKey = new CacheKey(lang, script);
        CompiledScript compiled = this.cache.getIfPresent(cacheKey);
        if (compiled != null) {
            return compiled;
        }
        if (!this.dynamicScriptEnabled(lang)) {
            throw new ScriptException("dynamic scripting for [" + lang + "] disabled");
        }
        if (cacheKey == null) {
            cacheKey = new CacheKey(lang, script);
        }
        compiled = this.getCompiledScript(lang, script);
        this.cache.put(cacheKey, compiled);
        return compiled;
    }
    
    private CompiledScript getCompiledScript(final String lang, final String script) {
        final ScriptEngineService service = this.scriptEngines.get(lang);
        if (service == null) {
            throw new ElasticsearchIllegalArgumentException("script_lang not supported [" + lang + "]");
        }
        final CompiledScript compiled = new CompiledScript(lang, service.compile(script));
        return compiled;
    }
    
    private void verifyDynamicScripting(final String lang) {
        if (!this.dynamicScriptEnabled(lang)) {
            throw new ScriptException("dynamic scripting for [" + lang + "] disabled");
        }
    }
    
    private GetResponse queryScriptIndex(final Client client, String scriptLang, final String id) {
        scriptLang = this.validateScriptLanguage(scriptLang);
        return ((ActionRequestBuilder<Request, GetResponse, RequestBuilder, Client>)client.prepareGet(".scripts", scriptLang, id).setVersion(-3L).setPreference("_local").setVersionType(VersionType.INTERNAL)).get();
    }
    
    public void queryScriptIndex(final GetIndexedScriptRequest request, final ActionListener<GetResponse> listener) {
        final String scriptLang = this.validateScriptLanguage(request.scriptLang());
        final GetRequest getRequest = new GetRequest(".scripts").type(scriptLang).id(request.id()).version(request.version()).versionType(request.versionType()).preference("_local");
        this.client.get(getRequest, listener);
    }
    
    private String validateScriptLanguage(String scriptLang) {
        if (scriptLang == null) {
            scriptLang = ScriptService.defaultLang;
        }
        else if (!this.scriptEngines.containsKey(scriptLang)) {
            throw new ElasticsearchIllegalArgumentException("script_lang not supported [" + scriptLang + "]");
        }
        return scriptLang;
    }
    
    private String getScriptFromIndex(final Client client, final String scriptLang, final String id) {
        final GetResponse responseFields = this.queryScriptIndex(client, scriptLang, id);
        if (responseFields.isExists()) {
            return getScriptFromResponse(responseFields);
        }
        throw new ElasticsearchIllegalArgumentException("Unable to find script [.scripts/" + scriptLang + "/" + id + "]");
    }
    
    private void validate(final BytesReference scriptBytes, final String scriptLang) throws IOException {
        final XContentParser parser = XContentFactory.xContent(scriptBytes).createParser(scriptBytes);
        final TemplateQueryParser.TemplateContext context = TemplateQueryParser.parse(parser, "params", "script", "template");
        if (Strings.hasLength(context.template())) {
            try {
                final CompiledScript compiledScript = this.compile(scriptLang, context.template(), ScriptType.INLINE);
                if (compiledScript == null) {
                    throw new ElasticsearchIllegalArgumentException("Unable to parse [" + context.template() + "] lang [" + scriptLang + "] (ScriptService.compile returned null)");
                }
                return;
            }
            catch (Exception e) {
                throw new ElasticsearchIllegalArgumentException("Unable to parse [" + context.template() + "] lang [" + scriptLang + "]", e);
            }
            throw new ElasticsearchIllegalArgumentException("Unable to find script in : " + scriptBytes.toUtf8());
        }
        throw new ElasticsearchIllegalArgumentException("Unable to find script in : " + scriptBytes.toUtf8());
    }
    
    public void putScriptToIndex(final Client client, final BytesReference scriptBytes, @Nullable String scriptLang, final String id, @Nullable final TimeValue timeout, @Nullable final String sOpType, final long version, final VersionType versionType, final ActionListener<IndexResponse> listener) {
        try {
            scriptLang = this.validateScriptLanguage(scriptLang);
            this.validate(scriptBytes, scriptLang);
            final IndexRequest indexRequest = new IndexRequest(".scripts", scriptLang, id);
            indexRequest.version(version);
            indexRequest.versionType(versionType);
            indexRequest.refresh(true);
            indexRequest.source(scriptBytes, true);
            if (timeout != null) {
                indexRequest.timeout(timeout);
            }
            if (sOpType != null) {
                indexRequest.opType(IndexRequest.OpType.fromString(sOpType));
            }
            client.index(indexRequest, listener);
        }
        catch (Throwable t) {
            listener.onFailure(t);
        }
    }
    
    public void deleteScriptFromIndex(final Client client, @Nullable String scriptLang, final String id, final long version, final ActionListener<DeleteResponse> listener) {
        scriptLang = this.validateScriptLanguage(scriptLang);
        client.delete(new DeleteRequest(".scripts", scriptLang, id).refresh(true).version(version), listener);
    }
    
    public static String getScriptFromResponse(final GetResponse responseFields) {
        final Map<String, Object> source = responseFields.getSourceAsMap();
        if (source.containsKey("template")) {
            try {
                final XContentBuilder builder = XContentFactory.contentBuilder(XContentType.JSON);
                final Object template = source.get("template");
                if (template instanceof Map) {
                    builder.map((Map<String, Object>)template);
                    return builder.string();
                }
                return template.toString();
            }
            catch (IOException | ClassCastException ex3) {
                final Exception ex;
                final Exception e = ex;
                throw new ElasticsearchIllegalStateException("Unable to parse " + responseFields.getSourceAsString() + " as json", e);
            }
        }
        if (source.containsKey("script")) {
            return source.get("script").toString();
        }
        try {
            final XContentBuilder builder = XContentFactory.contentBuilder(XContentType.JSON);
            builder.map(responseFields.getSource());
            return builder.string();
        }
        catch (IOException | ClassCastException ex4) {
            final Exception ex2;
            final Exception e = ex2;
            throw new ElasticsearchIllegalStateException("Unable to parse " + responseFields.getSourceAsString() + " as json", e);
        }
    }
    
    public ExecutableScript executable(final String lang, final String script, final ScriptType scriptType, final Map vars) {
        return this.executable(this.compile(lang, script, scriptType), vars);
    }
    
    public ExecutableScript executable(final CompiledScript compiledScript, final Map vars) {
        return this.scriptEngines.get(compiledScript.lang()).executable(compiledScript.compiled(), vars);
    }
    
    public SearchScript search(final CompiledScript compiledScript, final SearchLookup lookup, @Nullable final Map<String, Object> vars) {
        return this.scriptEngines.get(compiledScript.lang()).search(compiledScript.compiled(), lookup, vars);
    }
    
    public SearchScript search(final SearchLookup lookup, final String lang, final String script, final ScriptType scriptType, @Nullable final Map<String, Object> vars) {
        return this.search(this.compile(lang, script, scriptType), lookup, vars);
    }
    
    public SearchScript search(final MapperService mapperService, final IndexFieldDataService fieldDataService, final String lang, final String script, final ScriptType scriptType, @Nullable final Map<String, Object> vars) {
        return this.search(this.compile(lang, script), new SearchLookup(mapperService, fieldDataService, null), vars);
    }
    
    public Object execute(final CompiledScript compiledScript, final Map vars) {
        return this.scriptEngines.get(compiledScript.lang()).execute(compiledScript.compiled(), vars);
    }
    
    public void clear() {
        this.cache.invalidateAll();
    }
    
    private boolean dynamicScriptEnabled(final String lang) {
        final ScriptEngineService service = this.scriptEngines.get(lang);
        if (service == null) {
            throw new ElasticsearchIllegalArgumentException("script_lang not supported [" + lang + "]");
        }
        return this.dynamicScriptingDisabled == DynamicScriptDisabling.EVERYTHING_ALLOWED || "native".equals(lang) || "mustache".equals(lang) || (this.dynamicScriptingDisabled != DynamicScriptDisabling.ONLY_DISK_ALLOWED && service.sandboxed());
    }
    
    static {
        ScriptService.defaultLang = "groovy";
        SCRIPT_LANG = new ParseField("lang", new String[] { "script_lang" });
        VALUE_SCRIPT_FILE = new ParseField("value_script_file", new String[0]);
        VALUE_SCRIPT_ID = new ParseField("value_script_id", new String[0]);
        VALUE_SCRIPT_INLINE = new ParseField("value_script", new String[0]);
        KEY_SCRIPT_FILE = new ParseField("key_script_file", new String[0]);
        KEY_SCRIPT_ID = new ParseField("key_script_id", new String[0]);
        KEY_SCRIPT_INLINE = new ParseField("key_script", new String[0]);
        SCRIPT_FILE = new ParseField("script_file", new String[] { "file" });
        SCRIPT_ID = new ParseField("script_id", new String[] { "id" });
        SCRIPT_INLINE = new ParseField("script", new String[] { "scriptField" });
    }
    
    enum DynamicScriptDisabling
    {
        EVERYTHING_ALLOWED, 
        ONLY_DISK_ALLOWED, 
        SANDBOXED_ONLY;
        
        public static final DynamicScriptDisabling parse(final String s) {
            final String lowerCase = s.toLowerCase(Locale.ROOT);
            switch (lowerCase) {
                case "true":
                case "all": {
                    return DynamicScriptDisabling.ONLY_DISK_ALLOWED;
                }
                case "false":
                case "none": {
                    return DynamicScriptDisabling.EVERYTHING_ALLOWED;
                }
                case "sandbox":
                case "sandboxed": {
                    return DynamicScriptDisabling.SANDBOXED_ONLY;
                }
                default: {
                    throw new ElasticsearchIllegalArgumentException("Unrecognized script allowance setting: [" + s + "]");
                }
            }
        }
    }
    
    public enum ScriptType
    {
        INLINE, 
        INDEXED, 
        FILE;
        
        private static final int INLINE_VAL = 0;
        private static final int INDEXED_VAL = 1;
        private static final int FILE_VAL = 2;
        
        public static ScriptType readFrom(final StreamInput in) throws IOException {
            final int scriptTypeVal = in.readVInt();
            switch (scriptTypeVal) {
                case 1: {
                    return ScriptType.INDEXED;
                }
                case 0: {
                    return ScriptType.INLINE;
                }
                case 2: {
                    return ScriptType.FILE;
                }
                default: {
                    throw new ElasticsearchIllegalArgumentException("Unexpected value read for ScriptType got [" + scriptTypeVal + "] expected one of [" + 0 + "," + 1 + "," + 2 + "]");
                }
            }
        }
        
        public static void writeTo(final ScriptType scriptType, final StreamOutput out) throws IOException {
            if (scriptType == null) {
                out.writeVInt(0);
                return;
            }
            switch (scriptType) {
                case INDEXED: {
                    out.writeVInt(1);
                }
                case INLINE: {
                    out.writeVInt(0);
                }
                case FILE: {
                    out.writeVInt(2);
                }
                default: {
                    throw new ElasticsearchIllegalStateException("Unknown ScriptType " + scriptType);
                }
            }
        }
    }
    
    static class IndexedScript
    {
        String lang;
        String id;
        
        IndexedScript(final String lang, final String script) {
            this.lang = lang;
            final String[] parts = script.split("/");
            if (parts.length == 1) {
                this.id = script;
                if (this.lang == null) {
                    this.lang = ScriptService.defaultLang;
                }
            }
            else {
                if (parts.length != 3) {
                    throw new ElasticsearchIllegalArgumentException("Illegal index script format [" + script + "]" + " should be /lang/id");
                }
                if (!parts[1].equals(this.lang)) {
                    throw new ElasticsearchIllegalStateException("Conflicting script language, found [" + parts[1] + "] expected + [" + this.lang + "]");
                }
                this.id = parts[2];
            }
        }
    }
    
    private class ScriptCacheRemovalListener implements RemovalListener<CacheKey, CompiledScript>
    {
        @Override
        public void onRemoval(final RemovalNotification<CacheKey, CompiledScript> notification) {
            if (ScriptService.this.logger.isDebugEnabled()) {
                ScriptService.this.logger.debug("notifying script services of script removal due to: [{}]", notification.getCause());
            }
            for (final ScriptEngineService service : ScriptService.this.scriptEngines.values()) {
                try {
                    service.scriptRemoved(notification.getValue());
                }
                catch (Exception e) {
                    ScriptService.this.logger.warn("exception calling script removal listener for script service", e, new Object[0]);
                }
            }
        }
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
            if (ScriptService.this.logger.isTraceEnabled()) {
                ScriptService.this.logger.trace("Loading script file : [{}]", file);
            }
            final Tuple<String, String> scriptNameExt = this.scriptNameExt(file);
            if (scriptNameExt != null) {
                boolean found = false;
                for (final ScriptEngineService engineService : ScriptService.this.scriptEngines.values()) {
                    for (final String s : engineService.extensions()) {
                        if (s.equals(scriptNameExt.v2())) {
                            found = true;
                            try {
                                ScriptService.this.logger.info("compiling script file [{}]", file.getAbsolutePath());
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
            ScriptService.this.logger.info("removing script file [{}]", file.getAbsolutePath());
            ScriptService.this.staticCache.remove(scriptNameExt.v1());
        }
        
        @Override
        public void onFileChanged(final File file) {
            this.onFileInit(file);
        }
    }
    
    public static final class CacheKey
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
