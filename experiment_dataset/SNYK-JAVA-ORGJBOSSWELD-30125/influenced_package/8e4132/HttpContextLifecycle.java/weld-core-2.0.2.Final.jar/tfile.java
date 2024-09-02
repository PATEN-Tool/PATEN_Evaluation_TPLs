// 
// Decompiled by Procyon v0.5.36
// 

package org.jboss.weld.servlet;

import org.jboss.weld.logging.Category;
import org.jboss.weld.logging.LoggerFactory;
import org.jboss.weld.bean.builtin.ee.ServletContextBean;
import org.jboss.weld.logging.messages.ServletMessage;
import javax.servlet.http.HttpServletRequest;
import org.jboss.weld.util.reflection.Reflections;
import org.jboss.weld.context.http.HttpRequestContextImpl;
import org.jboss.weld.context.cache.RequestScopedBeanCache;
import javax.servlet.http.HttpSession;
import org.jboss.weld.literal.DestroyedLiteral;
import org.jboss.weld.literal.InitializedLiteral;
import javax.servlet.ServletContext;
import java.lang.annotation.Annotation;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.context.http.HttpRequestContext;
import org.jboss.weld.context.http.HttpSessionContext;
import org.slf4j.cal10n.LocLogger;
import org.jboss.weld.bootstrap.api.Service;

public class HttpContextLifecycle implements Service
{
    private static final String HTTP_SESSION;
    private static final String INCLUDE_HEADER = "javax.servlet.include.request_uri";
    private static final String REQUEST_DESTROYED;
    private static final LocLogger log;
    private HttpSessionContext sessionContextCache;
    private HttpRequestContext requestContextCache;
    private volatile boolean conversationActivationEnabled;
    private final BeanManagerImpl beanManager;
    private final ConversationContextActivator conversationContextActivator;
    
    public HttpContextLifecycle(final BeanManagerImpl beanManager) {
        this.beanManager = beanManager;
        this.conversationContextActivator = new ConversationContextActivator(beanManager);
        this.conversationActivationEnabled = true;
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
        this.beanManager.getAccessibleLenientObserverNotifier().fireEvent(ctx, (Annotation)InitializedLiteral.APPLICATION);
    }
    
    public void contextDestroyed(final ServletContext ctx) {
        this.beanManager.getAccessibleLenientObserverNotifier().fireEvent(ctx, (Annotation)DestroyedLiteral.APPLICATION);
    }
    
    public void sessionCreated(final HttpSession session) {
        SessionHolder.sessionCreated(session);
        this.beanManager.getAccessibleLenientObserverNotifier().fireEvent(session, (Annotation)InitializedLiteral.SESSION);
    }
    
    public void sessionDestroyed(final HttpSession session) {
        final boolean destroyed = this.getSessionContext().destroy(session);
        SessionHolder.clear();
        RequestScopedBeanCache.endRequest();
        if (destroyed) {
            this.beanManager.getAccessibleLenientObserverNotifier().fireEvent(session, (Annotation)DestroyedLiteral.SESSION);
        }
        else if (this.getRequestContext() instanceof HttpRequestContextImpl) {
            final HttpServletRequest request = Reflections.cast(this.getRequestContext()).getHttpServletRequest();
            request.setAttribute(HttpContextLifecycle.HTTP_SESSION, (Object)session);
        }
    }
    
    public void requestInitialized(final HttpServletRequest request, final ServletContext ctx) {
        if (this.isIncludedRequest(request)) {
            return;
        }
        HttpContextLifecycle.log.trace((Enum)ServletMessage.REQUEST_INITIALIZED, new Object[] { request });
        SessionHolder.requestInitialized(request);
        ServletContextBean.setServletContext(ctx);
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
            this.beanManager.getAccessibleLenientObserverNotifier().fireEvent(request, (Annotation)InitializedLiteral.REQUEST);
        }
        catch (RuntimeException e) {
            this.requestDestroyed(request);
            request.setAttribute(HttpContextLifecycle.REQUEST_DESTROYED, (Object)Boolean.TRUE);
            throw e;
        }
    }
    
    public void requestDestroyed(final HttpServletRequest request) {
        if (this.isIncludedRequest(request) || this.isRequestDestroyed(request)) {
            return;
        }
        HttpContextLifecycle.log.trace(HttpContextLifecycle.REQUEST_DESTROYED, (Object)request);
        try {
            this.conversationContextActivator.deactivateConversationContext(request);
            this.getRequestContext().invalidate();
            this.getRequestContext().deactivate();
            this.beanManager.getAccessibleLenientObserverNotifier().fireEvent(request, (Annotation)DestroyedLiteral.REQUEST);
            this.getSessionContext().deactivate();
            if (!this.getSessionContext().isValid()) {
                this.beanManager.getAccessibleLenientObserverNotifier().fireEvent(request.getAttribute(HttpContextLifecycle.HTTP_SESSION), (Annotation)DestroyedLiteral.SESSION);
            }
        }
        finally {
            this.getRequestContext().dissociate((Object)request);
            this.getSessionContext().dissociate((Object)request);
            this.conversationContextActivator.disassociateConversationContext(request);
            SessionHolder.clear();
            ServletContextBean.cleanup();
        }
    }
    
    public boolean isConversationActivationEnabled() {
        return this.conversationActivationEnabled;
    }
    
    public void setConversationActivationEnabled(final boolean conversationActivationEnabled) {
        this.conversationActivationEnabled = conversationActivationEnabled;
    }
    
    private boolean isIncludedRequest(final HttpServletRequest request) {
        return request.getAttribute("javax.servlet.include.request_uri") != null;
    }
    
    private boolean isRequestDestroyed(final HttpServletRequest request) {
        return request.getAttribute(HttpContextLifecycle.REQUEST_DESTROYED) != null;
    }
    
    public void cleanup() {
    }
    
    static {
        HTTP_SESSION = "org.jboss.weld." + HttpSession.class.getName();
        REQUEST_DESTROYED = HttpContextLifecycle.class.getName() + ".request.destroyed";
        log = LoggerFactory.loggerFactory().getLogger(Category.SERVLET);
    }
}
