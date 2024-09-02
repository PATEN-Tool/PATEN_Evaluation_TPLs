// 
// Decompiled by Procyon v0.5.36
// 

package org.jboss.weld.servlet;

import org.jboss.weld.context.AbstractConversationContext;
import org.jboss.weld.context.http.HttpRequestContextImpl;
import javax.servlet.http.HttpSession;
import javax.enterprise.context.spi.Context;
import org.jboss.weld.logging.ServletLogger;
import org.jboss.weld.logging.ConversationLogger;
import org.jboss.weld.context.ConversationContext;
import org.jboss.weld.literal.DestroyedLiteral;
import org.jboss.weld.literal.InitializedLiteral;
import java.lang.annotation.Annotation;
import javax.servlet.http.HttpServletRequest;
import org.jboss.weld.event.FastEvent;
import org.jboss.weld.context.http.HttpRequestContext;
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
    private HttpRequestContext requestContextCache;
    private final FastEvent<HttpServletRequest> conversationInitializedEvent;
    private final FastEvent<HttpServletRequest> conversationDestroyedEvent;
    
    protected ConversationContextActivator(final BeanManagerImpl beanManager) {
        this.beanManager = beanManager;
        this.conversationInitializedEvent = FastEvent.of(HttpServletRequest.class, beanManager, (Annotation)InitializedLiteral.CONVERSATION);
        this.conversationDestroyedEvent = FastEvent.of(HttpServletRequest.class, beanManager, (Annotation)DestroyedLiteral.CONVERSATION);
    }
    
    private HttpConversationContext httpConversationContext() {
        if (this.httpConversationContextCache == null) {
            this.httpConversationContextCache = (HttpConversationContext)this.beanManager.instance().select((Class)HttpConversationContext.class, new Annotation[0]).get();
        }
        return this.httpConversationContextCache;
    }
    
    private HttpRequestContext getRequestContext() {
        if (this.requestContextCache == null) {
            this.requestContextCache = (HttpRequestContext)this.beanManager.instance().select((Class)HttpRequestContext.class, new Annotation[0]).get();
        }
        return this.requestContextCache;
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
        final String cid = getConversationId(request, (ConversationContext)this.httpConversationContext());
        ConversationLogger.LOG.resumingConversation(cid);
        if (!this.isContextActivatedInRequest(request)) {
            this.setContextActivatedInRequest(request);
            conversationContext.activate(cid);
            if (cid == null) {
                this.conversationInitializedEvent.fire(request);
            }
        }
        else {
            conversationContext.dissociate((Object)request);
            conversationContext.associate((Object)request);
            conversationContext.activate(cid);
        }
    }
    
    protected void associateConversationContext(final HttpServletRequest request) {
        this.httpConversationContext().associate((Object)request);
    }
    
    private static String getConversationId(final HttpServletRequest request, final ConversationContext conversationContext) {
        if (request.getParameter("nocid") != null) {
            return null;
        }
        if ("none".equals(request.getParameter("conversationPropagation"))) {
            return null;
        }
        final String cidName = conversationContext.getParameterName();
        final String cid = request.getParameter(cidName);
        ConversationLogger.LOG.foundConversationFromRequest(cid);
        return cid;
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
        final HttpRequestContext requestContext = this.getRequestContext();
        final HttpConversationContext httpConversationContext = this.httpConversationContext();
        if (requestContext instanceof HttpRequestContextImpl && httpConversationContext instanceof AbstractConversationContext) {
            final HttpRequestContextImpl httpRequestContext = (HttpRequestContextImpl)requestContext;
            final HttpServletRequest request = httpRequestContext.getHttpServletRequest();
            final AbstractConversationContext abstractConversationContext = (AbstractConversationContext)httpConversationContext;
            abstractConversationContext.sessionCreated(request);
        }
    }
    
    static {
        CONTEXT_ACTIVATED_IN_REQUEST = ConversationContextActivator.class.getName() + "CONTEXT_ACTIVATED_IN_REQUEST";
    }
}
