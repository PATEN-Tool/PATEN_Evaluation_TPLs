// 
// Decompiled by Procyon v0.5.36
// 

package com.opensymphony.xwork2.interceptor;

import java.util.Iterator;
import com.opensymphony.xwork2.util.ValueStack;
import com.opensymphony.xwork2.ActionContext;
import java.util.HashMap;
import com.opensymphony.xwork2.ValidationAware;
import com.opensymphony.xwork2.conversion.impl.XWorkConverter;
import java.util.Map;
import org.apache.commons.lang.StringEscapeUtils;
import com.opensymphony.xwork2.ActionInvocation;

public class ConversionErrorInterceptor extends AbstractInterceptor
{
    public static final String ORIGINAL_PROPERTY_OVERRIDE = "original.property.override";
    
    protected Object getOverrideExpr(final ActionInvocation invocation, final Object value) {
        return this.escape(value);
    }
    
    protected String escape(final Object value) {
        return "\"" + StringEscapeUtils.escapeJava(String.valueOf(value)) + "\"";
    }
    
    @Override
    public String intercept(final ActionInvocation invocation) throws Exception {
        final ActionContext invocationContext = invocation.getInvocationContext();
        final Map<String, Object> conversionErrors = invocationContext.getConversionErrors();
        final ValueStack stack = invocationContext.getValueStack();
        HashMap<Object, Object> fakie = null;
        for (final Map.Entry<String, Object> entry : conversionErrors.entrySet()) {
            final String propertyName = entry.getKey();
            final Object value = entry.getValue();
            if (this.shouldAddError(propertyName, value)) {
                final String message = XWorkConverter.getConversionErrorMessage(propertyName, stack);
                final Object action = invocation.getAction();
                if (action instanceof ValidationAware) {
                    final ValidationAware va = (ValidationAware)action;
                    va.addFieldError(propertyName, message);
                }
                if (fakie == null) {
                    fakie = new HashMap<Object, Object>();
                }
                fakie.put(propertyName, this.getOverrideExpr(invocation, value));
            }
        }
        if (fakie != null) {
            stack.getContext().put("original.property.override", fakie);
            invocation.addPreResultListener(new PreResultListener() {
                public void beforeResult(final ActionInvocation invocation, final String resultCode) {
                    final Map<Object, Object> fakie = (Map<Object, Object>)invocation.getInvocationContext().get("original.property.override");
                    if (fakie != null) {
                        invocation.getStack().setExprOverrides(fakie);
                    }
                }
            });
        }
        return invocation.invoke();
    }
    
    protected boolean shouldAddError(final String propertyName, final Object value) {
        return true;
    }
}
