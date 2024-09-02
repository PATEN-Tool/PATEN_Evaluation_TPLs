// 
// Decompiled by Procyon v0.5.36
// 

package org.jolokia.backend;

import javax.management.MalformedObjectNameException;
import javax.management.MBeanRegistrationException;
import javax.management.NotCompliantMBeanException;
import java.util.Iterator;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.jolokia.util.ClassUtil;
import java.util.ArrayList;
import org.jolokia.detector.ServerHandle;
import org.jolokia.config.ConfigKey;
import javax.management.JMException;
import java.io.IOException;
import javax.management.MBeanException;
import javax.management.ReflectionException;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import org.jolokia.backend.executor.NotChangedException;
import org.json.simple.JSONObject;
import org.jolokia.request.JmxRequest;
import org.jolokia.restrictor.AllowAllRestrictor;
import org.jolokia.config.Configuration;
import org.jolokia.discovery.AgentDetails;
import java.util.List;
import org.jolokia.util.LogHandler;
import org.jolokia.util.DebugStore;
import org.jolokia.history.HistoryStore;
import org.jolokia.restrictor.Restrictor;
import org.jolokia.converter.json.JsonConvertOptions;
import org.jolokia.converter.Converters;
import org.jolokia.discovery.AgentDetailsHolder;

public class BackendManager implements AgentDetailsHolder
{
    private LocalRequestDispatcher localDispatcher;
    private Converters converters;
    private JsonConvertOptions.Builder convertOptionsBuilder;
    private Restrictor restrictor;
    private HistoryStore historyStore;
    private DebugStore debugStore;
    private LogHandler logHandler;
    private List<RequestDispatcher> requestDispatchers;
    private volatile Initializer initializer;
    private AgentDetails agentDetails;
    
    public BackendManager(final Configuration pConfig, final LogHandler pLogHandler) {
        this(pConfig, pLogHandler, null);
    }
    
    public BackendManager(final Configuration pConfig, final LogHandler pLogHandler, final Restrictor pRestrictor) {
        this(pConfig, pLogHandler, pRestrictor, false);
    }
    
    public BackendManager(final Configuration pConfig, final LogHandler pLogHandler, final Restrictor pRestrictor, final boolean pLazy) {
        this.restrictor = ((pRestrictor != null) ? pRestrictor : new AllowAllRestrictor());
        this.logHandler = pLogHandler;
        this.agentDetails = new AgentDetails(pConfig);
        if (pLazy) {
            this.initializer = new Initializer(pConfig);
        }
        else {
            this.init(pConfig);
            this.initializer = null;
        }
    }
    
    public JSONObject handleRequest(final JmxRequest pJmxReq) throws InstanceNotFoundException, AttributeNotFoundException, ReflectionException, MBeanException, IOException {
        this.lazyInitIfNeeded();
        final boolean debug = this.isDebug();
        long time = 0L;
        if (debug) {
            time = System.currentTimeMillis();
        }
        JSONObject json;
        try {
            json = this.callRequestDispatcher(pJmxReq);
            this.historyStore.updateAndAdd(pJmxReq, json);
            json.put((Object)"status", (Object)200);
        }
        catch (NotChangedException exp) {
            json = new JSONObject();
            json.put((Object)"request", (Object)pJmxReq.toJSON());
            json.put((Object)"status", (Object)304);
            json.put((Object)"timestamp", (Object)(System.currentTimeMillis() / 1000L));
        }
        if (debug) {
            this.debug("Execution time: " + (System.currentTimeMillis() - time) + " ms");
            this.debug("Response: " + json);
        }
        return json;
    }
    
    public Object convertExceptionToJson(final Throwable pExp, final JmxRequest pJmxReq) {
        final JsonConvertOptions opts = this.getJsonConvertOptions(pJmxReq);
        try {
            final JSONObject expObj = (JSONObject)this.converters.getToJsonConverter().convertToJson(pExp, null, opts);
            return expObj;
        }
        catch (AttributeNotFoundException e) {
            return null;
        }
    }
    
    public void destroy() {
        try {
            this.localDispatcher.destroy();
        }
        catch (JMException e) {
            this.error("Cannot unregister MBean: " + e, e);
        }
    }
    
    public boolean isRemoteAccessAllowed(final String pRemoteHost, final String pRemoteAddr) {
        return this.restrictor.isRemoteAccessAllowed((pRemoteHost != null) ? new String[] { pRemoteHost, pRemoteAddr } : new String[] { pRemoteAddr });
    }
    
    public boolean isOriginAllowed(final String pOrigin, final boolean pStrictChecking) {
        return this.restrictor.isOriginAllowed(pOrigin, pStrictChecking);
    }
    
    public void info(final String msg) {
        this.logHandler.info(msg);
        if (this.debugStore != null) {
            this.debugStore.log(msg);
        }
    }
    
    public void debug(final String msg) {
        this.logHandler.debug(msg);
        if (this.debugStore != null) {
            this.debugStore.log(msg);
        }
    }
    
    public void error(final String message, final Throwable t) {
        this.logHandler.error(message, t);
        if (this.debugStore != null) {
            this.debugStore.log(message, t);
        }
    }
    
    public boolean isDebug() {
        return this.debugStore != null && this.debugStore.isDebug();
    }
    
    public AgentDetails getAgentDetails() {
        return this.agentDetails;
    }
    
    private void lazyInitIfNeeded() {
        if (this.initializer != null) {
            synchronized (this) {
                if (this.initializer != null) {
                    this.initializer.init();
                    this.initializer = null;
                }
            }
        }
    }
    
    private void init(final Configuration pConfig) {
        this.converters = new Converters();
        this.initLimits(pConfig);
        this.localDispatcher = new LocalRequestDispatcher(this.converters, this.restrictor, pConfig, this.logHandler);
        final ServerHandle serverHandle = this.localDispatcher.getServerHandle();
        (this.requestDispatchers = this.createRequestDispatchers((pConfig != null) ? pConfig.get(ConfigKey.DISPATCHER_CLASSES) : null, this.converters, serverHandle, this.restrictor)).add(this.localDispatcher);
        this.initMBeans(pConfig);
        this.agentDetails.setServerInfo(serverHandle.getVendor(), serverHandle.getProduct(), serverHandle.getVersion());
    }
    
    private void initLimits(final Configuration pConfig) {
        if (pConfig != null) {
            this.convertOptionsBuilder = new JsonConvertOptions.Builder(this.getNullSaveIntLimit(pConfig.get(ConfigKey.MAX_DEPTH)), this.getNullSaveIntLimit(pConfig.get(ConfigKey.MAX_COLLECTION_SIZE)), this.getNullSaveIntLimit(pConfig.get(ConfigKey.MAX_OBJECTS)));
        }
        else {
            this.convertOptionsBuilder = new JsonConvertOptions.Builder();
        }
    }
    
    private int getNullSaveIntLimit(final String pValue) {
        return (pValue != null) ? Integer.parseInt(pValue) : 0;
    }
    
    private List<RequestDispatcher> createRequestDispatchers(final String pClasses, final Converters pConverters, final ServerHandle pServerHandle, final Restrictor pRestrictor) {
        final List<RequestDispatcher> ret = new ArrayList<RequestDispatcher>();
        if (pClasses != null && pClasses.length() > 0) {
            final String[] split;
            final String[] names = split = pClasses.split("\\s*,\\s*");
            for (final String name : split) {
                ret.add(this.createDispatcher(name, pConverters, pServerHandle, pRestrictor));
            }
        }
        return ret;
    }
    
    private RequestDispatcher createDispatcher(final String pDispatcherClass, final Converters pConverters, final ServerHandle pServerHandle, final Restrictor pRestrictor) {
        try {
            final Class clazz = ClassUtil.classForName(pDispatcherClass, this.getClass().getClassLoader());
            if (clazz == null) {
                throw new IllegalArgumentException("Couldn't lookup dispatcher " + pDispatcherClass);
            }
            final Constructor constructor = clazz.getConstructor(Converters.class, ServerHandle.class, Restrictor.class);
            return constructor.newInstance(pConverters, pServerHandle, pRestrictor);
        }
        catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Class " + pDispatcherClass + " has invalid constructor: " + e, e);
        }
        catch (IllegalAccessException e2) {
            throw new IllegalArgumentException("Constructor of " + pDispatcherClass + " couldn't be accessed: " + e2, e2);
        }
        catch (InvocationTargetException e3) {
            throw new IllegalArgumentException(e3);
        }
        catch (InstantiationException e4) {
            throw new IllegalArgumentException(pDispatcherClass + " couldn't be instantiated: " + e4, e4);
        }
    }
    
    private JSONObject callRequestDispatcher(final JmxRequest pJmxReq) throws InstanceNotFoundException, AttributeNotFoundException, ReflectionException, MBeanException, IOException, NotChangedException {
        Object retValue = null;
        boolean useValueWithPath = false;
        boolean found = false;
        for (final RequestDispatcher dispatcher : this.requestDispatchers) {
            if (dispatcher.canHandle(pJmxReq)) {
                retValue = dispatcher.dispatchRequest(pJmxReq);
                useValueWithPath = dispatcher.useReturnValueWithPath(pJmxReq);
                found = true;
                break;
            }
        }
        if (!found) {
            throw new IllegalStateException("Internal error: No dispatcher found for handling " + pJmxReq);
        }
        final JsonConvertOptions opts = this.getJsonConvertOptions(pJmxReq);
        final Object jsonResult = this.converters.getToJsonConverter().convertToJson(retValue, useValueWithPath ? pJmxReq.getPathParts() : null, opts);
        final JSONObject jsonObject = new JSONObject();
        jsonObject.put((Object)"value", jsonResult);
        jsonObject.put((Object)"request", (Object)pJmxReq.toJSON());
        return jsonObject;
    }
    
    private JsonConvertOptions getJsonConvertOptions(final JmxRequest pJmxReq) {
        return this.convertOptionsBuilder.maxDepth(pJmxReq.getParameterAsInt(ConfigKey.MAX_DEPTH)).maxCollectionSize(pJmxReq.getParameterAsInt(ConfigKey.MAX_COLLECTION_SIZE)).maxObjects(pJmxReq.getParameterAsInt(ConfigKey.MAX_OBJECTS)).faultHandler(pJmxReq.getValueFaultHandler()).useAttributeFilter(pJmxReq.getPathParts() != null).build();
    }
    
    private void initMBeans(final Configuration pConfig) {
        final int maxEntries = pConfig.getAsInt(ConfigKey.HISTORY_MAX_ENTRIES);
        final int maxDebugEntries = pConfig.getAsInt(ConfigKey.DEBUG_MAX_ENTRIES);
        this.historyStore = new HistoryStore(maxEntries);
        this.debugStore = new DebugStore(maxDebugEntries, pConfig.getAsBoolean(ConfigKey.DEBUG));
        try {
            this.localDispatcher.initMBeans(this.historyStore, this.debugStore);
        }
        catch (NotCompliantMBeanException e) {
            this.intError("Error registering config MBean: " + e, e);
        }
        catch (MBeanRegistrationException e2) {
            this.intError("Cannot register MBean: " + e2, e2);
        }
        catch (MalformedObjectNameException e3) {
            this.intError("Invalid name for config MBean: " + e3, e3);
        }
    }
    
    private void intError(final String message, final Throwable t) {
        this.logHandler.error(message, t);
        this.debugStore.log(message, t);
    }
    
    private final class Initializer
    {
        private Configuration config;
        
        private Initializer(final Configuration pConfig) {
            this.config = pConfig;
        }
        
        void init() {
            BackendManager.this.init(this.config);
        }
    }
}
