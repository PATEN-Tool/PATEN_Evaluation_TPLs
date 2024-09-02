// 
// Decompiled by Procyon v0.5.36
// 

package org.jboss.weld.servlet;

import org.jboss.weld.context.AbstractConversationContext;
import javax.servlet.http.HttpSession;
import javax.enterprise.context.spi.Context;
import org.jboss.weld.logging.ServletLogger;
import org.jboss.weld.context.ConversationContext;
import org.jboss.weld.logging.ConversationLogger;
import org.jboss.weld.context.http.LazyHttpConversationContextImpl;
import org.jboss.weld.literal.DestroyedLiteral;
import org.jboss.weld.literal.InitializedLiteral;
import java.lang.annotation.Annotation;
import org.jboss.weld.util.Consumer;
import javax.servlet.http.HttpServletRequest;
import org.jboss.weld.event.FastEvent;
import org.jboss.weld.context.http.HttpConversationContext;
import org.jboss.weld.manager.BeanManagerImpl;

public class ConversationContextActivator
{
    private static final String NO_CID = "nocid";
    private static final String CONVERSATION_PROPAGATION = "conversationPropagation";
    private static final String CONVERSATION_PROPAGATION_NONE = "none";
    private static final String CONTEXT_ACTIVATED_IN_REQUEST;
    private final BeanManagerImpl beanManager;
    private HttpConversationContext httpConversationContextCache;
    private final FastEvent<HttpServletRequest> conversationInitializedEvent;
    private final FastEvent<HttpServletRequest> conversationDestroyedEvent;
    private final Consumer<HttpServletRequest> lazyInitializationCallback;
    private final boolean lazy;
    
    protected ConversationContextActivator(final BeanManagerImpl beanManager, final boolean lazy) {
        this.beanManager = beanManager;
        this.conversationInitializedEvent = FastEvent.of(HttpServletRequest.class, beanManager, (Annotation)InitializedLiteral.CONVERSATION);
        this.conversationDestroyedEvent = FastEvent.of(HttpServletRequest.class, beanManager, (Annotation)DestroyedLiteral.CONVERSATION);
        this.lazyInitializationCallback = (lazy ? new Consumer<HttpServletRequest>() {
            @Override
            public void accept(final HttpServletRequest input) {
                ConversationContextActivator.this.conversationInitializedEvent.fire(input);
            }
        } : null);
        this.lazy = lazy;
    }
    
    private HttpConversationContext httpConversationContext() {
        if (this.httpConversationContextCache == null) {
            this.httpConversationContextCache = (HttpConversationContext)this.beanManager.instance().select((Class)HttpConversationContext.class, new Annotation[0]).get();
        }
        return this.httpConversationContextCache;
    }
    
    public void startConversationContext(final HttpServletRequest request) {
        this.associateConversationContext(request);
        this.activateConversationContext(request);
    }
    
    public void stopConversationContext(final HttpServletRequest request) {
        this.deactivateConversationContext(request);
    }
    
    protected void activateConversationContext(final HttpServletRequest request) {
        final HttpConversationContext conversationContext = this.httpConversationContext();
        if (!this.isContextActivatedInRequest(request)) {
            this.setContextActivatedInRequest(request);
            this.activate(conversationContext, request);
        }
        else {
            conversationContext.dissociate((Object)request);
            conversationContext.associate((Object)request);
            this.activate(conversationContext, request);
        }
    }
    
    private void activate(final HttpConversationContext conversationContext, final HttpServletRequest request) {
        if (this.lazy && conversationContext instanceof LazyHttpConversationContextImpl) {
            final LazyHttpConversationContextImpl lazyConversationContext = (LazyHttpConversationContextImpl)conversationContext;
            lazyConversationContext.activate(this.lazyInitializationCallback);
        }
        else {
            final String cid = determineConversationId(request, conversationContext.getParameterName());
            conversationContext.activate(cid);
            if (cid == null) {
                this.conversationInitializedEvent.fire(request);
            }
        }
    }
    
    protected void associateConversationContext(final HttpServletRequest request) {
        this.httpConversationContext().associate((Object)request);
    }
    
    private void setContextActivatedInRequest(final HttpServletRequest request) {
        request.setAttribute(ConversationContextActivator.CONTEXT_ACTIVATED_IN_REQUEST, (Object)true);
    }
    
    private boolean isContextActivatedInRequest(final HttpServletRequest request) {
        final Object result = request.getAttribute(ConversationContextActivator.CONTEXT_ACTIVATED_IN_REQUEST);
        return result != null && (boolean)result;
    }
    
    protected void deactivateConversationContext(final HttpServletRequest request) {
        final ConversationContext conversationContext = (ConversationContext)this.httpConversationContext();
        if (conversationContext.isActive()) {
            if (conversationContext instanceof LazyHttpConversationContextImpl) {
                final LazyHttpConversationContextImpl lazyConversationContext = (LazyHttpConversationContextImpl)conversationContext;
                if (!lazyConversationContext.isInitialized()) {
                    lazyConversationContext.deactivate();
                    return;
                }
            }
            final boolean isTransient = conversationContext.getCurrentConversation().isTransient();
            if (ConversationLogger.LOG.isTraceEnabled()) {
                if (isTransient) {
                    ConversationLogger.LOG.cleaningUpTransientConversation();
                }
                else {
                    ConversationLogger.LOG.cleaningUpConversation(conversationContext.getCurrentConversation().getId());
                }
            }
            conversationContext.invalidate();
            conversationContext.deactivate();
            if (isTransient) {
                this.conversationDestroyedEvent.fire(request);
            }
        }
    }
    
    protected void disassociateConversationContext(final HttpServletRequest request) {
        try {
            this.httpConversationContext().dissociate((Object)request);
        }
        catch (Exception e) {
            ServletLogger.LOG.unableToDissociateContext((Context)this.httpConversationContext(), request);
            ServletLogger.LOG.catchingDebug(e);
        }
    }
    
    public void sessionCreated(final HttpSession session) {
        final HttpConversationContext httpConversationContext = this.httpConversationContext();
        if (httpConversationContext instanceof AbstractConversationContext) {
            final AbstractConversationContext<?, ?> abstractConversationContext = (AbstractConversationContext<?, ?>)httpConversationContext;
            abstractConversationContext.sessionCreated();
        }
    }
    
    public static String determineConversationId(final HttpServletRequest request, final String parameterName) {
        if (request == null) {
            throw ConversationLogger.LOG.mustCallAssociateBeforeActivate();
        }
        if (request.getParameter("nocid") != null) {
            return null;
        }
        if ("none".equals(request.getParameter("conversationPropagation"))) {
            return null;
        }
        final String cidName = parameterName;
        final String cid = request.getParameter(cidName);
        ConversationLogger.LOG.foundConversationFromRequest(cid);
        return cid;
    }
    
    static {
        CONTEXT_ACTIVATED_IN_REQUEST = ConversationContextActivator.class.getName() + "CONTEXT_ACTIVATED_IN_REQUEST";
    }
}
