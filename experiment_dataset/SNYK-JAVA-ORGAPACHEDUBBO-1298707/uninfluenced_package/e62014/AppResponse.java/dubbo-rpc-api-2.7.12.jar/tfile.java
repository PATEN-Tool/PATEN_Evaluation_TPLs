// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.dubbo.rpc;

import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.BiConsumer;
import org.apache.dubbo.rpc.proxy.InvokerInvocationHandler;
import java.util.HashMap;
import java.util.Map;

public class AppResponse implements Result
{
    private static final long serialVersionUID = -6925924956850004727L;
    private Object result;
    private Throwable exception;
    private Map<String, Object> attachments;
    private Map<String, Object> attributes;
    
    public AppResponse() {
        this.attachments = new HashMap<String, Object>();
        this.attributes = new HashMap<String, Object>();
    }
    
    public AppResponse(final Invocation invocation) {
        this.attachments = new HashMap<String, Object>();
        this.attributes = new HashMap<String, Object>();
        this.setAttribute("invocation", invocation);
    }
    
    public AppResponse(final Object result) {
        this.attachments = new HashMap<String, Object>();
        this.attributes = new HashMap<String, Object>();
        this.result = result;
    }
    
    public AppResponse(final Throwable exception) {
        this.attachments = new HashMap<String, Object>();
        this.attributes = new HashMap<String, Object>();
        this.exception = exception;
    }
    
    @Override
    public Object recreate() throws Throwable {
        if (this.exception != null) {
            try {
                final Object stackTrace = InvokerInvocationHandler.stackTraceField.get(this.exception);
                if (stackTrace == null) {
                    this.exception.setStackTrace(new StackTraceElement[0]);
                }
            }
            catch (Exception ex) {}
            throw this.exception;
        }
        return this.result;
    }
    
    @Override
    public Object getValue() {
        return this.result;
    }
    
    @Override
    public void setValue(final Object value) {
        this.result = value;
    }
    
    @Override
    public Throwable getException() {
        return this.exception;
    }
    
    @Override
    public void setException(final Throwable e) {
        this.exception = e;
    }
    
    @Override
    public boolean hasException() {
        return this.exception != null;
    }
    
    @Deprecated
    @Override
    public Map<String, String> getAttachments() {
        return new AttachmentsAdapter.ObjectToStringMap(this.attachments);
    }
    
    @Override
    public Map<String, Object> getObjectAttachments() {
        return this.attachments;
    }
    
    @Override
    public void setAttachments(final Map<String, String> map) {
        this.attachments = ((map == null) ? new HashMap<String, Object>() : new HashMap<String, Object>(map));
    }
    
    @Override
    public void setObjectAttachments(final Map<String, Object> map) {
        this.attachments = ((map == null) ? new HashMap<String, Object>() : map);
    }
    
    @Override
    public void addAttachments(final Map<String, String> map) {
        if (map == null) {
            return;
        }
        if (this.attachments == null) {
            this.attachments = new HashMap<String, Object>();
        }
        this.attachments.putAll(map);
    }
    
    @Override
    public void addObjectAttachments(final Map<String, Object> map) {
        if (map == null) {
            return;
        }
        if (this.attachments == null) {
            this.attachments = new HashMap<String, Object>();
        }
        this.attachments.putAll(map);
    }
    
    @Override
    public String getAttachment(final String key) {
        final Object value = this.attachments.get(key);
        if (value instanceof String) {
            return (String)value;
        }
        return null;
    }
    
    @Override
    public Object getObjectAttachment(final String key) {
        return this.attachments.get(key);
    }
    
    @Override
    public String getAttachment(final String key, final String defaultValue) {
        final Object result = this.attachments.get(key);
        if (result == null) {
            return defaultValue;
        }
        if (result instanceof String) {
            return (String)result;
        }
        return defaultValue;
    }
    
    @Override
    public Object getObjectAttachment(final String key, final Object defaultValue) {
        Object result = this.attachments.get(key);
        if (result == null) {
            result = defaultValue;
        }
        return result;
    }
    
    @Override
    public void setAttachment(final String key, final String value) {
        this.setObjectAttachment(key, value);
    }
    
    @Override
    public void setAttachment(final String key, final Object value) {
        this.setObjectAttachment(key, value);
    }
    
    @Override
    public void setObjectAttachment(final String key, final Object value) {
        this.attachments.put(key, value);
    }
    
    public Object getAttribute(final String key) {
        return this.attributes.get(key);
    }
    
    public void setAttribute(final String key, final Object value) {
        this.attributes.put(key, value);
    }
    
    @Override
    public Result whenCompleteWithContext(final BiConsumer<Result, Throwable> fn) {
        throw new UnsupportedOperationException("AppResponse represents an concrete business response, there will be no status changes, you should get internal values directly.");
    }
    
    @Override
    public <U> CompletableFuture<U> thenApply(final Function<Result, ? extends U> fn) {
        throw new UnsupportedOperationException("AppResponse represents an concrete business response, there will be no status changes, you should get internal values directly.");
    }
    
    @Override
    public Result get() throws InterruptedException, ExecutionException {
        throw new UnsupportedOperationException("AppResponse represents an concrete business response, there will be no status changes, you should get internal values directly.");
    }
    
    @Override
    public Result get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        throw new UnsupportedOperationException("AppResponse represents an concrete business response, there will be no status changes, you should get internal values directly.");
    }
    
    public void clear() {
        this.result = null;
        this.exception = null;
        this.attachments.clear();
    }
    
    @Override
    public String toString() {
        return "AppResponse [value=" + this.result + ", exception=" + this.exception + "]";
    }
}
