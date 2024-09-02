// 
// Decompiled by Procyon v0.5.36
// 

package com.opensymphony.xwork2.interceptor;

import org.apache.logging.log4j.LogManager;
import org.apache.struts2.dispatcher.HttpParameters;
import java.util.Collections;
import java.util.Iterator;
import com.opensymphony.xwork2.util.ValueStack;
import com.opensymphony.xwork2.config.entities.ActionConfig;
import com.opensymphony.xwork2.util.TextParseUtil;
import java.util.Map;
import com.opensymphony.xwork2.util.ClearableValueStack;
import com.opensymphony.xwork2.util.reflection.ReflectionContextState;
import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.config.entities.Parameterizable;
import com.opensymphony.xwork2.ActionInvocation;
import org.apache.commons.lang3.BooleanUtils;
import com.opensymphony.xwork2.inject.Inject;
import com.opensymphony.xwork2.LocalizedTextProvider;
import com.opensymphony.xwork2.util.ValueStackFactory;
import org.apache.logging.log4j.Logger;

public class StaticParametersInterceptor extends AbstractInterceptor
{
    private boolean parse;
    private boolean overwrite;
    private boolean merge;
    private boolean devMode;
    private static final Logger LOG;
    private ValueStackFactory valueStackFactory;
    private LocalizedTextProvider localizedTextProvider;
    
    public StaticParametersInterceptor() {
        this.merge = true;
        this.devMode = false;
    }
    
    @Inject
    public void setValueStackFactory(final ValueStackFactory valueStackFactory) {
        this.valueStackFactory = valueStackFactory;
    }
    
    @Inject("devMode")
    public void setDevMode(final String mode) {
        this.devMode = BooleanUtils.toBoolean(mode);
    }
    
    @Inject
    public void setLocalizedTextProvider(final LocalizedTextProvider localizedTextProvider) {
        this.localizedTextProvider = localizedTextProvider;
    }
    
    public void setParse(final String value) {
        this.parse = BooleanUtils.toBoolean(value);
    }
    
    public void setMerge(final String value) {
        this.merge = BooleanUtils.toBoolean(value);
    }
    
    public void setOverwrite(final String value) {
        this.overwrite = BooleanUtils.toBoolean(value);
    }
    
    @Override
    public String intercept(final ActionInvocation invocation) throws Exception {
        final ActionConfig config = invocation.getProxy().getConfig();
        final Object action = invocation.getAction();
        final Map<String, String> parameters = config.getParams();
        StaticParametersInterceptor.LOG.debug("Setting static parameters: {}", (Object)parameters);
        if (action instanceof Parameterizable) {
            ((Parameterizable)action).setParams(parameters);
        }
        if (parameters != null) {
            final ActionContext ac = ActionContext.getContext();
            final Map<String, Object> contextMap = ac.getContextMap();
            try {
                ReflectionContextState.setCreatingNullObjects(contextMap, true);
                ReflectionContextState.setReportingConversionErrors(contextMap, true);
                final ValueStack stack = ac.getValueStack();
                final ValueStack newStack = this.valueStackFactory.createValueStack(stack);
                final boolean clearableStack = newStack instanceof ClearableValueStack;
                if (clearableStack) {
                    ((ClearableValueStack)newStack).clearContextValues();
                    final Map<String, Object> context = newStack.getContext();
                    ReflectionContextState.setCreatingNullObjects(context, true);
                    ReflectionContextState.setDenyMethodExecution(context, true);
                    ReflectionContextState.setReportingConversionErrors(context, true);
                    context.put("com.opensymphony.xwork2.ActionContext.locale", stack.getContext().get("com.opensymphony.xwork2.ActionContext.locale"));
                }
                for (final Map.Entry<String, String> entry : parameters.entrySet()) {
                    Object val = entry.getValue();
                    if (this.parse && val instanceof String) {
                        val = TextParseUtil.translateVariables(val.toString(), stack);
                    }
                    try {
                        newStack.setValue(entry.getKey(), val);
                    }
                    catch (RuntimeException e) {
                        if (!this.devMode) {
                            continue;
                        }
                        final String developerNotification = this.localizedTextProvider.findText(ParametersInterceptor.class, "devmode.notification", ActionContext.getContext().getLocale(), "Developer Notification:\n{0}", new Object[] { "Unexpected Exception caught setting '" + entry.getKey() + "' on '" + action.getClass() + ": " + e.getMessage() });
                        StaticParametersInterceptor.LOG.error(developerNotification);
                        if (!(action instanceof ValidationAware)) {
                            continue;
                        }
                        ((ValidationAware)action).addActionMessage(developerNotification);
                    }
                }
                if (clearableStack && stack.getContext() != null && newStack.getContext() != null) {
                    stack.getContext().put("com.opensymphony.xwork2.ActionContext.conversionErrors", newStack.getContext().get("com.opensymphony.xwork2.ActionContext.conversionErrors"));
                }
                if (this.merge) {
                    this.addParametersToContext(ac, parameters);
                }
            }
            finally {
                ReflectionContextState.setCreatingNullObjects(contextMap, false);
                ReflectionContextState.setReportingConversionErrors(contextMap, false);
            }
        }
        return invocation.invoke();
    }
    
    protected Map<String, String> retrieveParameters(final ActionContext ac) {
        final ActionConfig config = ac.getActionInvocation().getProxy().getConfig();
        if (config != null) {
            return config.getParams();
        }
        return Collections.emptyMap();
    }
    
    protected void addParametersToContext(final ActionContext ac, final Map<String, ?> newParams) {
        final HttpParameters previousParams = ac.getParameters();
        HttpParameters.Builder combinedParams = HttpParameters.create();
        if (this.overwrite) {
            if (previousParams != null) {
                combinedParams = combinedParams.withParent(previousParams);
            }
            if (newParams != null) {
                combinedParams = combinedParams.withExtraParams(newParams);
            }
        }
        else {
            if (newParams != null) {
                combinedParams = combinedParams.withExtraParams(newParams);
            }
            if (previousParams != null) {
                combinedParams = combinedParams.withParent(previousParams);
            }
        }
        ac.setParameters(combinedParams.build());
    }
    
    static {
        LOG = LogManager.getLogger((Class)StaticParametersInterceptor.class);
    }
}
