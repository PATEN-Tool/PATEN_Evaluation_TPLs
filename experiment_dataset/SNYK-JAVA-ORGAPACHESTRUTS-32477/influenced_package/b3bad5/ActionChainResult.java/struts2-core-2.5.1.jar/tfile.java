// 
// Decompiled by Procyon v0.5.36
// 

package com.opensymphony.xwork2;

import org.apache.logging.log4j.LogManager;
import java.util.List;
import java.util.Set;
import java.util.Collection;
import java.util.HashSet;
import com.opensymphony.xwork2.util.ValueStack;
import java.util.Map;
import java.util.HashMap;
import com.opensymphony.xwork2.util.TextParseUtil;
import java.util.LinkedList;
import com.opensymphony.xwork2.inject.Inject;
import org.apache.logging.log4j.Logger;

public class ActionChainResult implements Result
{
    private static final Logger LOG;
    public static final String DEFAULT_PARAM = "actionName";
    private static final String CHAIN_HISTORY = "CHAIN_HISTORY";
    public static final String SKIP_ACTIONS_PARAM = "skipActions";
    private ActionProxy proxy;
    private String actionName;
    private String namespace;
    private String methodName;
    private String skipActions;
    private ActionProxyFactory actionProxyFactory;
    
    public ActionChainResult() {
    }
    
    public ActionChainResult(final String namespace, final String actionName, final String methodName) {
        this.namespace = namespace;
        this.actionName = actionName;
        this.methodName = methodName;
    }
    
    public ActionChainResult(final String namespace, final String actionName, final String methodName, final String skipActions) {
        this.namespace = namespace;
        this.actionName = actionName;
        this.methodName = methodName;
        this.skipActions = skipActions;
    }
    
    @Inject
    public void setActionProxyFactory(final ActionProxyFactory actionProxyFactory) {
        this.actionProxyFactory = actionProxyFactory;
    }
    
    public void setActionName(final String actionName) {
        this.actionName = actionName;
    }
    
    public void setNamespace(final String namespace) {
        this.namespace = namespace;
    }
    
    public void setSkipActions(final String actions) {
        this.skipActions = actions;
    }
    
    public void setMethod(final String method) {
        this.methodName = method;
    }
    
    public ActionProxy getProxy() {
        return this.proxy;
    }
    
    public static LinkedList<String> getChainHistory() {
        LinkedList<String> chainHistory = (LinkedList<String>)ActionContext.getContext().get("CHAIN_HISTORY");
        if (chainHistory == null) {
            chainHistory = new LinkedList<String>();
            ActionContext.getContext().put("CHAIN_HISTORY", chainHistory);
        }
        return chainHistory;
    }
    
    @Override
    public void execute(final ActionInvocation invocation) throws Exception {
        if (this.namespace == null) {
            this.namespace = invocation.getProxy().getNamespace();
        }
        final ValueStack stack = ActionContext.getContext().getValueStack();
        final String finalNamespace = TextParseUtil.translateVariables(this.namespace, stack);
        final String finalActionName = TextParseUtil.translateVariables(this.actionName, stack);
        final String finalMethodName = (this.methodName != null) ? TextParseUtil.translateVariables(this.methodName, stack) : null;
        if (this.isInChainHistory(finalNamespace, finalActionName, finalMethodName)) {
            this.addToHistory(finalNamespace, finalActionName, finalMethodName);
            throw new XWorkException("Infinite recursion detected: " + getChainHistory().toString());
        }
        if (getChainHistory().isEmpty() && invocation != null && invocation.getProxy() != null) {
            this.addToHistory(finalNamespace, invocation.getProxy().getActionName(), invocation.getProxy().getMethod());
        }
        this.addToHistory(finalNamespace, finalActionName, finalMethodName);
        final HashMap<String, Object> extraContext = new HashMap<String, Object>();
        extraContext.put("com.opensymphony.xwork2.util.ValueStack.ValueStack", ActionContext.getContext().getValueStack());
        extraContext.put("com.opensymphony.xwork2.ActionContext.parameters", ActionContext.getContext().getParameters());
        extraContext.put("CHAIN_HISTORY", getChainHistory());
        ActionChainResult.LOG.debug("Chaining to action {}", new Object[] { finalActionName });
        (this.proxy = this.actionProxyFactory.createActionProxy(finalNamespace, finalActionName, finalMethodName, extraContext)).execute();
    }
    
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        final ActionChainResult that = (ActionChainResult)o;
        Label_0062: {
            if (this.actionName != null) {
                if (this.actionName.equals(that.actionName)) {
                    break Label_0062;
                }
            }
            else if (that.actionName == null) {
                break Label_0062;
            }
            return false;
        }
        Label_0095: {
            if (this.methodName != null) {
                if (this.methodName.equals(that.methodName)) {
                    break Label_0095;
                }
            }
            else if (that.methodName == null) {
                break Label_0095;
            }
            return false;
        }
        if (this.namespace != null) {
            if (this.namespace.equals(that.namespace)) {
                return true;
            }
        }
        else if (that.namespace == null) {
            return true;
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        int result = (this.actionName != null) ? this.actionName.hashCode() : 0;
        result = 31 * result + ((this.namespace != null) ? this.namespace.hashCode() : 0);
        result = 31 * result + ((this.methodName != null) ? this.methodName.hashCode() : 0);
        return result;
    }
    
    private boolean isInChainHistory(final String namespace, final String actionName, final String methodName) {
        final LinkedList<? extends String> chainHistory = getChainHistory();
        if (chainHistory == null) {
            return false;
        }
        final Set<String> skipActionsList = new HashSet<String>();
        if (this.skipActions != null && this.skipActions.length() > 0) {
            final ValueStack stack = ActionContext.getContext().getValueStack();
            final String finalSkipActions = TextParseUtil.translateVariables(this.skipActions, stack);
            skipActionsList.addAll(TextParseUtil.commaDelimitedStringToSet(finalSkipActions));
        }
        return !skipActionsList.contains(actionName) && chainHistory.contains(this.makeKey(namespace, actionName, methodName));
    }
    
    private void addToHistory(final String namespace, final String actionName, final String methodName) {
        final List<String> chainHistory = getChainHistory();
        chainHistory.add(this.makeKey(namespace, actionName, methodName));
    }
    
    private String makeKey(final String namespace, final String actionName, final String methodName) {
        if (null == methodName) {
            return namespace + "/" + actionName;
        }
        return namespace + "/" + actionName + "!" + methodName;
    }
    
    static {
        LOG = LogManager.getLogger((Class)ActionChainResult.class);
    }
}
