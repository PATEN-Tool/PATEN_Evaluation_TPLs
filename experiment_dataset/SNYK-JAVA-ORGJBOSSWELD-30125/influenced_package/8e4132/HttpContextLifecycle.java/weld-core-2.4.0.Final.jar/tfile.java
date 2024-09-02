// 
// Decompiled by Procyon v0.5.36
// 

package org.jboss.weld.servlet;

import org.jboss.weld.context.BoundContext;
import org.jboss.weld.context.ManagedContext;
import javax.servlet.ServletRequest;
import org.jboss.weld.logging.ServletLogger;
import org.jboss.weld.util.reflection.Reflections;
import org.jboss.weld.context.http.HttpRequestContextImpl;
import org.jboss.weld.context.cache.RequestScopedCache;
import javax.enterprise.inject.spi.EventMetadata;
import javax.enterprise.inject.spi.InjectionPoint;
import org.jboss.weld.event.EventMetadataImpl;
import java.util.Collections;
import java.lang.reflect.Type;
import javax.servlet.ServletContext;
import org.jboss.weld.bootstrap.BeanDeploymentModules;
import org.jboss.weld.literal.DestroyedLiteral;
import org.jboss.weld.literal.InitializedLiteral;
import java.lang.annotation.Annotation;
import org.jboss.weld.bootstrap.BeanDeploymentModule;
import org.jboss.weld.Container;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import org.jboss.weld.event.FastEvent;
import org.jboss.weld.servlet.spi.HttpContextActivationFilter;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.context.http.HttpRequestContext;
import org.jboss.weld.context.http.HttpSessionContext;
import org.jboss.weld.context.http.HttpSessionDestructionContext;
import org.jboss.weld.bootstrap.api.Service;

public class HttpContextLifecycle implements Service
{
    public static final String ASYNC_STARTED_ATTR_NAME = "org.jboss.weld.context.asyncStarted";
    private static final String HTTP_SESSION;
    private static final String INCLUDE_HEADER = "javax.servlet.include.request_uri";
    private static final String FORWARD_HEADER = "javax.servlet.forward.request_uri";
    private static final String REQUEST_DESTROYED;
    private static final String GUARD_PARAMETER_NAME = "org.jboss.weld.context.ignore.guard.marker";
    private static final Object GUARD_PARAMETER_VALUE;
    private HttpSessionDestructionContext sessionDestructionContextCache;
    private HttpSessionContext sessionContextCache;
    private HttpRequestContext requestContextCache;
    private volatile Boolean conversationActivationEnabled;
    private final boolean ignoreForwards;
    private final boolean ignoreIncludes;
    private final BeanManagerImpl beanManager;
    private final ConversationContextActivator conversationContextActivator;
    private final HttpContextActivationFilter contextActivationFilter;
    private final FastEvent<HttpServletRequest> requestInitializedEvent;
    private final FastEvent<HttpServletRequest> requestDestroyedEvent;
    private final FastEvent<HttpSession> sessionInitializedEvent;
    private final FastEvent<HttpSession> sessionDestroyedEvent;
    private final ServletApiAbstraction servletApi;
    private final ServletContextService servletContextService;
    private final Container container;
    private final BeanDeploymentModule module;
    private static final ThreadLocal<Counter> nestedInvocationGuard;
    private final boolean nestedInvocationGuardEnabled;
    
    public HttpContextLifecycle(final BeanManagerImpl beanManager, final HttpContextActivationFilter contextActivationFilter, final boolean ignoreForwards, final boolean ignoreIncludes, final boolean lazyConversationContext, final boolean nestedInvocationGuardEnabled) {
        this.beanManager = beanManager;
        this.conversationContextActivator = new ConversationContextActivator(beanManager, lazyConversationContext);
        this.conversationActivationEnabled = null;
        this.ignoreForwards = ignoreForwards;
        this.ignoreIncludes = ignoreIncludes;
        this.contextActivationFilter = contextActivationFilter;
        this.requestInitializedEvent = FastEvent.of(HttpServletRequest.class, beanManager, (Annotation)InitializedLiteral.REQUEST);
        this.requestDestroyedEvent = FastEvent.of(HttpServletRequest.class, beanManager, (Annotation)DestroyedLiteral.REQUEST);
        this.sessionInitializedEvent = FastEvent.of(HttpSession.class, beanManager, (Annotation)InitializedLiteral.SESSION);
        this.sessionDestroyedEvent = FastEvent.of(HttpSession.class, beanManager, (Annotation)DestroyedLiteral.SESSION);
        this.servletApi = (ServletApiAbstraction)beanManager.getServices().get((Class)ServletApiAbstraction.class);
        this.servletContextService = (ServletContextService)beanManager.getServices().get((Class)ServletContextService.class);
        this.nestedInvocationGuardEnabled = nestedInvocationGuardEnabled;
        this.container = Container.instance(beanManager);
        final BeanDeploymentModules beanDeploymentModules = (BeanDeploymentModules)beanManager.getServices().get((Class)BeanDeploymentModules.class);
        this.module = ((beanDeploymentModules != null) ? beanDeploymentModules.getModule(beanManager) : null);
    }
    
    private HttpSessionDestructionContext getSessionDestructionContext() {
        if (this.sessionDestructionContextCache == null) {
            this.sessionDestructionContextCache = (HttpSessionDestructionContext)this.beanManager.instance().select((Class)HttpSessionDestructionContext.class, new Annotation[0]).get();
        }
        return this.sessionDestructionContextCache;
    }
    
    private HttpSessionContext getSessionContext() {
        if (this.sessionContextCache == null) {
            this.sessionContextCache = (HttpSessionContext)this.beanManager.instance().select((Class)HttpSessionContext.class, new Annotation[0]).get();
        }
        return this.sessionContextCache;
    }
    
    public HttpRequestContext getRequestContext() {
        if (this.requestContextCache == null) {
            this.requestContextCache = (HttpRequestContext)this.beanManager.instance().select((Class)HttpRequestContext.class, new Annotation[0]).get();
        }
        return this.requestContextCache;
    }
    
    public void contextInitialized(final ServletContext ctx) {
        this.servletContextService.contextInitialized(ctx);
        synchronized (this.container) {
            this.fireEventForApplicationScope(ctx, (Annotation)InitializedLiteral.APPLICATION);
        }
    }
    
    public void contextDestroyed(final ServletContext ctx) {
        synchronized (this.container) {
            this.fireEventForApplicationScope(ctx, (Annotation)DestroyedLiteral.APPLICATION);
        }
    }
    
    private void fireEventForApplicationScope(final ServletContext ctx, final Annotation qualifier) {
        if (this.module != null) {
            if (this.module.isWebModule()) {
                this.module.fireEvent(ServletContext.class, ctx, qualifier);
            }
            else {
                final EventMetadata metadata = (EventMetadata)new EventMetadataImpl(ServletContext.class, null, Collections.singleton(qualifier));
                this.beanManager.getAccessibleLenientObserverNotifier().fireEvent(ServletContext.class, ctx, metadata, qualifier);
            }
        }
    }
    
    public void sessionCreated(final HttpSession session) {
        SessionHolder.sessionCreated(session);
        this.conversationContextActivator.sessionCreated(session);
        this.sessionInitializedEvent.fire(session);
    }
    
    public void sessionDestroyed(final HttpSession session) {
        this.deactivateSessionDestructionContext(session);
        final boolean destroyed = this.getSessionContext().destroy(session);
        SessionHolder.clear();
        RequestScopedCache.endRequest();
        if (destroyed) {
            this.sessionDestroyedEvent.fire(session);
        }
        else if (this.getRequestContext() instanceof HttpRequestContextImpl) {
            final HttpServletRequest request = Reflections.cast(this.getRequestContext()).getHttpServletRequest();
            request.setAttribute(HttpContextLifecycle.HTTP_SESSION, (Object)session);
        }
    }
    
    private void deactivateSessionDestructionContext(final HttpSession session) {
        final HttpSessionDestructionContext context = this.getSessionDestructionContext();
        if (context.isActive()) {
            context.deactivate();
            context.dissociate(session);
        }
    }
    
    public void requestInitialized(final HttpServletRequest request, final ServletContext ctx) {
        if (this.nestedInvocationGuardEnabled) {
            final Counter counter = HttpContextLifecycle.nestedInvocationGuard.get();
            final Object marker = request.getAttribute("org.jboss.weld.context.ignore.guard.marker");
            if (counter != null && marker != null) {
                counter.value++;
                return;
            }
            if (counter != null && marker == null) {
                ServletLogger.LOG.guardLeak(counter.value);
            }
            HttpContextLifecycle.nestedInvocationGuard.set(new Counter());
            request.setAttribute("org.jboss.weld.context.ignore.guard.marker", HttpContextLifecycle.GUARD_PARAMETER_VALUE);
        }
        if (this.ignoreForwards && this.isForwardedRequest(request)) {
            return;
        }
        if (this.ignoreIncludes && this.isIncludedRequest(request)) {
            return;
        }
        if (!this.contextActivationFilter.accepts(request)) {
            return;
        }
        ServletLogger.LOG.requestInitialized(request);
        SessionHolder.requestInitialized(request);
        this.getRequestContext().associate((Object)request);
        this.getSessionContext().associate((Object)request);
        if (this.conversationActivationEnabled) {
            this.conversationContextActivator.associateConversationContext(request);
        }
        this.getRequestContext().activate();
        this.getSessionContext().activate();
        try {
            if (this.conversationActivationEnabled) {
                this.conversationContextActivator.activateConversationContext(request);
            }
            this.requestInitializedEvent.fire(request);
        }
        catch (RuntimeException e) {
            try {
                this.requestDestroyed(request);
            }
            catch (Exception ex) {}
            request.setAttribute(HttpContextLifecycle.REQUEST_DESTROYED, (Object)Boolean.TRUE);
            throw e;
        }
    }
    
    public void requestDestroyed(final HttpServletRequest request) {
        if (this.isRequestDestroyed(request)) {
            return;
        }
        if (this.nestedInvocationGuardEnabled) {
            final Counter counter = HttpContextLifecycle.nestedInvocationGuard.get();
            if (counter == null) {
                ServletLogger.LOG.guardNotSet();
                return;
            }
            counter.value--;
            if (counter.value > 0) {
                return;
            }
            HttpContextLifecycle.nestedInvocationGuard.remove();
            request.removeAttribute("org.jboss.weld.context.ignore.guard.marker");
        }
        if (this.ignoreForwards && this.isForwardedRequest(request)) {
            return;
        }
        if (this.ignoreIncludes && this.isIncludedRequest(request)) {
            return;
        }
        if (!this.contextActivationFilter.accepts(request)) {
            return;
        }
        ServletLogger.LOG.requestDestroyed(request);
        try {
            this.conversationContextActivator.deactivateConversationContext(request);
            if (this.servletApi.isAsyncSupported() && this.servletApi.isAsyncStarted((ServletRequest)request)) {
                request.setAttribute("org.jboss.weld.context.asyncStarted", (Object)true);
            }
            else {
                this.getRequestContext().invalidate();
            }
            this.safelyDeactivate((ManagedContext)this.getRequestContext(), request);
            this.requestDestroyedEvent.fire(request);
            this.safelyDeactivate((ManagedContext)this.getSessionContext(), request);
            final Object destroyedHttpSession = request.getAttribute(HttpContextLifecycle.HTTP_SESSION);
            if (destroyedHttpSession != null) {
                this.sessionDestroyedEvent.fire((HttpSession)destroyedHttpSession);
            }
        }
        finally {
            this.safelyDissociate((org.jboss.weld.context.BoundContext<HttpServletRequest>)this.getRequestContext(), request);
            this.safelyDissociate((org.jboss.weld.context.BoundContext<HttpServletRequest>)this.getSessionContext(), request);
            this.conversationContextActivator.disassociateConversationContext(request);
            SessionHolder.clear();
        }
    }
    
    public boolean isConversationActivationSet() {
        return this.conversationActivationEnabled != null;
    }
    
    public void setConversationActivationEnabled(final boolean conversationActivationEnabled) {
        this.conversationActivationEnabled = conversationActivationEnabled;
    }
    
    public void cleanup() {
    }
    
    private boolean isIncludedRequest(final HttpServletRequest request) {
        return request.getAttribute("javax.servlet.include.request_uri") != null;
    }
    
    private boolean isForwardedRequest(final HttpServletRequest request) {
        return request.getAttribute("javax.servlet.forward.request_uri") != null;
    }
    
    private boolean isRequestDestroyed(final HttpServletRequest request) {
        return request.getAttribute(HttpContextLifecycle.REQUEST_DESTROYED) != null;
    }
    
    private <T> void safelyDissociate(final BoundContext<T> context, final T storage) {
        try {
            context.dissociate((Object)storage);
        }
        catch (Exception e) {
            ServletLogger.LOG.unableToDissociateContext(context, storage);
            ServletLogger.LOG.catchingDebug(e);
        }
    }
    
    private void safelyDeactivate(final ManagedContext context, final HttpServletRequest request) {
        try {
            context.deactivate();
        }
        catch (Exception e) {
            ServletLogger.LOG.unableToDeactivateContext(context, request);
            ServletLogger.LOG.catchingDebug(e);
        }
    }
    
    static {
        HTTP_SESSION = "org.jboss.weld." + HttpSession.class.getName();
        REQUEST_DESTROYED = HttpContextLifecycle.class.getName() + ".request.destroyed";
        GUARD_PARAMETER_VALUE = new Object();
        nestedInvocationGuard = new ThreadLocal<Counter>();
    }
    
    private static class Counter
    {
        private int value;
        
        private Counter() {
            this.value = 1;
        }
    }
}
