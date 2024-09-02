// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.dubbo.rpc;

import org.apache.dubbo.common.logger.LoggerFactory;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.apache.dubbo.common.logger.Logger;

public class AsyncRpcResult extends AbstractResult
{
    private static final Logger logger;
    private RpcContext storedContext;
    private RpcContext storedServerContext;
    private Invocation invocation;
    private RpcContext tmpContext;
    private RpcContext tmpServerContext;
    private Function<Result, Result> beforeContext;
    private Function<Result, Result> afterContext;
    
    public AsyncRpcResult(final Invocation invocation) {
        this.beforeContext = (Function<Result, Result>)(appResponse -> {
            this.tmpContext = RpcContext.getContext();
            this.tmpServerContext = RpcContext.getServerContext();
            RpcContext.restoreContext(this.storedContext);
            RpcContext.restoreServerContext(this.storedServerContext);
            return appResponse;
        });
        this.afterContext = (Function<Result, Result>)(appResponse -> {
            RpcContext.restoreContext(this.tmpContext);
            RpcContext.restoreServerContext(this.tmpServerContext);
            return appResponse;
        });
        this.invocation = invocation;
        this.storedContext = RpcContext.getContext();
        this.storedServerContext = RpcContext.getServerContext();
    }
    
    public AsyncRpcResult(final AsyncRpcResult asyncRpcResult) {
        this.beforeContext = (Function<Result, Result>)(appResponse -> {
            this.tmpContext = RpcContext.getContext();
            this.tmpServerContext = RpcContext.getServerContext();
            RpcContext.restoreContext(this.storedContext);
            RpcContext.restoreServerContext(this.storedServerContext);
            return appResponse;
        });
        this.afterContext = (Function<Result, Result>)(appResponse -> {
            RpcContext.restoreContext(this.tmpContext);
            RpcContext.restoreServerContext(this.tmpServerContext);
            return appResponse;
        });
        this.invocation = asyncRpcResult.getInvocation();
        this.storedContext = asyncRpcResult.getStoredContext();
        this.storedServerContext = asyncRpcResult.getStoredServerContext();
    }
    
    @Override
    public Object getValue() {
        return this.getAppResponse().getValue();
    }
    
    @Override
    public void setValue(final Object value) {
        final AppResponse appResponse = new AppResponse();
        appResponse.setValue(value);
        ((CompletableFuture<AppResponse>)this).complete(appResponse);
    }
    
    @Override
    public Throwable getException() {
        return this.getAppResponse().getException();
    }
    
    @Override
    public void setException(final Throwable t) {
        final AppResponse appResponse = new AppResponse();
        appResponse.setException(t);
        ((CompletableFuture<AppResponse>)this).complete(appResponse);
    }
    
    @Override
    public boolean hasException() {
        return this.getAppResponse().hasException();
    }
    
    public Result getAppResponse() {
        try {
            if (this.isDone()) {
                return this.get();
            }
        }
        catch (Exception e) {
            AsyncRpcResult.logger.error("Got exception when trying to fetch the underlying result from AsyncRpcResult.", (Throwable)e);
        }
        return new AppResponse();
    }
    
    @Override
    public Object recreate() throws Throwable {
        final RpcInvocation rpcInvocation = (RpcInvocation)this.invocation;
        if (InvokeMode.FUTURE == rpcInvocation.getInvokeMode()) {
            final AppResponse appResponse = new AppResponse();
            final CompletableFuture<Object> future = new CompletableFuture<Object>();
            appResponse.setValue(future);
            final CompletableFuture<Object> completableFuture;
            this.whenComplete((result, t) -> {
                if (t != null) {
                    if (t instanceof CompletionException) {
                        t = t.getCause();
                    }
                    completableFuture.completeExceptionally(t);
                }
                else if (result.hasException()) {
                    completableFuture.completeExceptionally(result.getException());
                }
                else {
                    completableFuture.complete(result.getValue());
                }
                return;
            });
            return appResponse.recreate();
        }
        if (this.isDone()) {
            return this.get().recreate();
        }
        return new AppResponse().recreate();
    }
    
    @Override
    public Result thenApplyWithContext(final Function<Result, Result> fn) {
        this.thenApply((Function<? super Result, ?>)fn.compose((Function<? super Object, ? extends Result>)this.beforeContext).andThen((Function<? super Result, ?>)this.afterContext));
        return this;
    }
    
    public void subscribeTo(final CompletableFuture<?> future) {
        future.whenComplete((obj, t) -> {
            if (t != null) {
                this.completeExceptionally(t);
            }
            else {
                this.complete(obj);
            }
        });
    }
    
    @Override
    public Map<String, String> getAttachments() {
        return this.getAppResponse().getAttachments();
    }
    
    @Override
    public void setAttachments(final Map<String, String> map) {
        this.getAppResponse().setAttachments(map);
    }
    
    @Override
    public void addAttachments(final Map<String, String> map) {
        this.getAppResponse().addAttachments(map);
    }
    
    @Override
    public String getAttachment(final String key) {
        return this.getAppResponse().getAttachment(key);
    }
    
    @Override
    public String getAttachment(final String key, final String defaultValue) {
        return this.getAppResponse().getAttachment(key, defaultValue);
    }
    
    @Override
    public void setAttachment(final String key, final String value) {
        this.getAppResponse().setAttachment(key, value);
    }
    
    public RpcContext getStoredContext() {
        return this.storedContext;
    }
    
    public RpcContext getStoredServerContext() {
        return this.storedServerContext;
    }
    
    public Invocation getInvocation() {
        return this.invocation;
    }
    
    public static AsyncRpcResult newDefaultAsyncResult(final AppResponse appResponse, final Invocation invocation) {
        final AsyncRpcResult asyncRpcResult = new AsyncRpcResult(invocation);
        ((CompletableFuture<AppResponse>)asyncRpcResult).complete(appResponse);
        return asyncRpcResult;
    }
    
    public static AsyncRpcResult newDefaultAsyncResult(final Invocation invocation) {
        return newDefaultAsyncResult(null, null, invocation);
    }
    
    public static AsyncRpcResult newDefaultAsyncResult(final Object value, final Invocation invocation) {
        return newDefaultAsyncResult(value, null, invocation);
    }
    
    public static AsyncRpcResult newDefaultAsyncResult(final Throwable t, final Invocation invocation) {
        return newDefaultAsyncResult(null, t, invocation);
    }
    
    public static AsyncRpcResult newDefaultAsyncResult(final Object value, final Throwable t, final Invocation invocation) {
        final AsyncRpcResult asyncRpcResult = new AsyncRpcResult(invocation);
        final AppResponse appResponse = new AppResponse();
        if (t != null) {
            appResponse.setException(t);
        }
        else {
            appResponse.setValue(value);
        }
        ((CompletableFuture<AppResponse>)asyncRpcResult).complete(appResponse);
        return asyncRpcResult;
    }
    
    static {
        logger = LoggerFactory.getLogger((Class)AsyncRpcResult.class);
    }
}
