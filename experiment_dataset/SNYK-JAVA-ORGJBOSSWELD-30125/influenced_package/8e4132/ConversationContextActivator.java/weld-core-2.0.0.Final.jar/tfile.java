// 
// Decompiled by Procyon v0.5.36
// 

package org.jboss.weld.servlet;

import org.jboss.weld.logging.Category;
import org.jboss.weld.logging.LoggerFactory;
import org.jboss.weld.literal.DestroyedLiteral;
import org.jboss.weld.logging.messages.ConversationMessage;
import org.jboss.weld.literal.InitializedLiteral;
import org.jboss.weld.logging.messages.JsfMessage;
import org.jboss.weld.context.ConversationContext;
import javax.servlet.http.HttpServletRequest;
import java.lang.annotation.Annotation;
import org.jboss.weld.context.http.HttpConversationContext;
import javax.enterprise.inject.Instance;
import org.jboss.weld.manager.BeanManagerImpl;
import org.slf4j.cal10n.LocLogger;

public class ConversationContextActivator
{
    private static final String NO_CID = "nocid";
    private static final String CONVERSATION_PROPAGATION = "conversationPropagation";
    private static final String CONVERSATION_PROPAGATION_NONE = "none";
    private static final String CONTEXT_ACTIVATED_IN_REQUEST;
    private static final LocLogger log;
    private final BeanManagerImpl beanManager;
    private final Instance<HttpConversationContext> httpConversationContext;
    
    protected ConversationContextActivator(final BeanManagerImpl beanManager) {
        this.beanManager = beanManager;
        this.httpConversationContext = (Instance<HttpConversationContext>)beanManager.instance().select((Class)HttpConversationContext.class, new Annotation[0]);
    }
    
    public void startConversationContext(final HttpServletRequest request) {
        this.associateConversationContext(request);
        this.activateConversationContext(request);
    }
    
    public void stopConversationContext(final HttpServletRequest request) {
        this.deactivateConversationContext(request);
    }
    
    protected void activateConversationContext(final HttpServletRequest request) {
        final HttpConversationContext conversationContext = (HttpConversationContext)this.httpConversationContext.get();
        final String cid = getConversationId(request, (ConversationContext)conversationContext);
        ConversationContextActivator.log.debug((Enum)JsfMessage.RESUMING_CONVERSATION, new Object[] { cid });
        if (!this.isContextActivatedInRequest(request)) {
            this.setContextActivatedInRequest(request);
            conversationContext.activate(cid);
            if (cid == null) {
                this.beanManager.getAccessibleLenientObserverNotifier().fireEvent(request, (Annotation)InitializedLiteral.CONVERSATION);
            }
        }
        else {
            conversationContext.dissociate((Object)request);
            conversationContext.associate((Object)request);
            conversationContext.activate(cid);
        }
    }
    
    protected void associateConversationContext(final HttpServletRequest request) {
        ((HttpConversationContext)this.httpConversationContext.get()).associate((Object)request);
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
        ConversationContextActivator.log.trace((Enum)JsfMessage.FOUND_CONVERSATION_FROM_REQUEST, new Object[] { cid });
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
        final ConversationContext conversationContext = (ConversationContext)this.httpConversationContext.get();
        if (conversationContext.isActive()) {
            final boolean isTransient = conversationContext.getCurrentConversation().isTransient();
            if (ConversationContextActivator.log.isTraceEnabled()) {
                if (isTransient) {
                    ConversationContextActivator.log.trace((Enum)ConversationMessage.CLEANING_UP_TRANSIENT_CONVERSATION, new Object[0]);
                }
                else {
                    ConversationContextActivator.log.trace((Enum)JsfMessage.CLEANING_UP_CONVERSATION, new Object[] { conversationContext.getCurrentConversation().getId() });
                }
            }
            conversationContext.invalidate();
            conversationContext.deactivate();
            if (isTransient) {
                this.beanManager.getAccessibleLenientObserverNotifier().fireEvent(request, (Annotation)DestroyedLiteral.CONVERSATION);
            }
        }
    }
    
    protected void disassociateConversationContext(final HttpServletRequest request) {
        ((HttpConversationContext)this.httpConversationContext.get()).dissociate((Object)request);
    }
    
    static {
        CONTEXT_ACTIVATED_IN_REQUEST = WeldListener.class.getName() + "CONTEXT_ACTIVATED_IN_REQUEST";
        log = LoggerFactory.loggerFactory().getLogger(Category.SERVLET);
    }
}
