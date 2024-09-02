// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.dubbo.rpc;

import org.apache.dubbo.common.logger.LoggerFactory;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.apache.dubbo.rpc.protocol.dubbo.FutureAdapter;
import java.util.function.BiConsumer;
import org.apache.dubbo.common.logger.Logger;

public class AsyncRpcResult extends AbstractResult
{
    private static final Logger logger;
    private RpcContext storedContext;
    private RpcContext storedServerContext;
    private Invocation invocation;
    private RpcContext tmpContext;
    private RpcContext tmpServerContext;
    private BiConsumer<Result, Throwable> beforeContext;
    private BiConsumer<Result, Throwable> afterContext;
    
    public AsyncRpcResult(final Invocation invocation) {
        this.beforeContext = ((appResponse, t) -> {
            this.tmpContext = RpcContext.getContext();
            this.tmpServerContext = RpcContext.getServerContext();
            RpcContext.restoreContext(this.storedContext);
            RpcContext.restoreServerContext(this.storedServerContext);
            return;
        });
        this.afterContext = ((appResponse, t) -> {
            RpcContext.restoreContext(this.tmpContext);
            RpcContext.restoreServerContext(this.tmpServerContext);
            return;
        });
        this.invocation = invocation;
        this.storedContext = RpcContext.getContext();
        this.storedServerContext = RpcContext.getServerContext();
    }
    
    public AsyncRpcResult(final AsyncRpcResult asyncRpcResult) {
        this.beforeContext = ((appResponse, t) -> {
            this.tmpContext = RpcContext.getContext();
            this.tmpServerContext = RpcContext.getServerContext();
            RpcContext.restoreContext(this.storedContext);
            RpcContext.restoreServerContext(this.storedServerContext);
            return;
        });
        this.afterContext = ((appResponse, t) -> {
            RpcContext.restoreContext(this.tmpContext);
            RpcContext.restoreServerContext(this.tmpServerContext);
            return;
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
        try {
            if (this.isDone()) {
                this.get().setValue(value);
            }
            else {
                final AppResponse appResponse = new AppResponse();
                appResponse.setValue(value);
                ((CompletableFuture<AppResponse>)this).complete(appResponse);
            }
        }
        catch (Exception e) {
            AsyncRpcResult.logger.error("Got exception when trying to change the value of the underlying result from AsyncRpcResult.", (Throwable)e);
        }
    }
    
    @Override
    public Throwable getException() {
        return this.getAppResponse().getException();
    }
    
    @Override
    public void setException(final Throwable t) {
        try {
            if (this.isDone()) {
                this.get().setException(t);
            }
            else {
                final AppResponse appResponse = new AppResponse();
                appResponse.setException(t);
                ((CompletableFuture<AppResponse>)this).complete(appResponse);
            }
        }
        catch (Exception e) {
            AsyncRpcResult.logger.error("Got exception when trying to change the value of the underlying result from AsyncRpcResult.", (Throwable)e);
        }
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
        final FutureAdapter future = new FutureAdapter((CompletableFuture<AppResponse>)this);
        RpcContext.getContext().setFuture(future);
        if (InvokeMode.FUTURE == rpcInvocation.getInvokeMode()) {
            return future;
        }
        return this.getAppResponse().recreate();
    }
    
    @Override
    public Result whenCompleteWithContext(final BiConsumer<Result, Throwable> fn) {
        final CompletableFuture<Result> future = this.whenComplete((v, t) -> {
            this.beforeContext.accept(v, t);
            fn.accept(v, t);
            this.afterContext.accept(v, t);
            return;
        });
        final AsyncRpcResult nextStage = new AsyncRpcResult(this);
        nextStage.subscribeTo(future);
        return nextStage;
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
