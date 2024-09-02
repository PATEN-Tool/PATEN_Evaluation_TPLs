// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.dubbo.rpc;

import org.apache.dubbo.common.logger.LoggerFactory;
import java.util.Map;
import java.util.function.Function;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;
import org.apache.dubbo.common.threadpool.ThreadlessExecutor;
import java.util.function.BiConsumer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.apache.dubbo.common.logger.Logger;

public class AsyncRpcResult implements Result
{
    private static final Logger logger;
    private RpcContext storedContext;
    private RpcContext storedServerContext;
    private Executor executor;
    private Invocation invocation;
    private CompletableFuture<AppResponse> responseFuture;
    private RpcContext tmpContext;
    private RpcContext tmpServerContext;
    private BiConsumer<Result, Throwable> beforeContext;
    private BiConsumer<Result, Throwable> afterContext;
    
    public AsyncRpcResult(final CompletableFuture<AppResponse> future, final Invocation invocation) {
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
        this.responseFuture = future;
        this.invocation = invocation;
        this.storedContext = RpcContext.getContext();
        this.storedServerContext = RpcContext.getServerContext();
    }
    
    @Override
    public Object getValue() {
        return this.getAppResponse().getValue();
    }
    
    @Override
    public void setValue(final Object value) {
        try {
            if (this.responseFuture.isDone()) {
                this.responseFuture.get().setValue(value);
            }
            else {
                final AppResponse appResponse = new AppResponse();
                appResponse.setValue(value);
                this.responseFuture.complete(appResponse);
            }
        }
        catch (Exception e) {
            AsyncRpcResult.logger.error("Got exception when trying to fetch the underlying result from AsyncRpcResult.");
            throw new RpcException(e);
        }
    }
    
    @Override
    public Throwable getException() {
        return this.getAppResponse().getException();
    }
    
    @Override
    public void setException(final Throwable t) {
        try {
            if (this.responseFuture.isDone()) {
                this.responseFuture.get().setException(t);
            }
            else {
                final AppResponse appResponse = new AppResponse();
                appResponse.setException(t);
                this.responseFuture.complete(appResponse);
            }
        }
        catch (Exception e) {
            AsyncRpcResult.logger.error("Got exception when trying to fetch the underlying result from AsyncRpcResult.");
            throw new RpcException(e);
        }
    }
    
    @Override
    public boolean hasException() {
        return this.getAppResponse().hasException();
    }
    
    public CompletableFuture<AppResponse> getResponseFuture() {
        return this.responseFuture;
    }
    
    public void setResponseFuture(final CompletableFuture<AppResponse> responseFuture) {
        this.responseFuture = responseFuture;
    }
    
    public Result getAppResponse() {
        try {
            if (this.responseFuture.isDone()) {
                return this.responseFuture.get();
            }
        }
        catch (Exception e) {
            AsyncRpcResult.logger.error("Got exception when trying to fetch the underlying result from AsyncRpcResult.");
            throw new RpcException(e);
        }
        return new AppResponse();
    }
    
    @Override
    public Result get() throws InterruptedException, ExecutionException {
        if (this.executor != null && this.executor instanceof ThreadlessExecutor) {
            final ThreadlessExecutor threadlessExecutor = (ThreadlessExecutor)this.executor;
            threadlessExecutor.waitAndDrain();
        }
        return this.responseFuture.get();
    }
    
    @Override
    public Result get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (this.executor != null && this.executor instanceof ThreadlessExecutor) {
            final ThreadlessExecutor threadlessExecutor = (ThreadlessExecutor)this.executor;
            threadlessExecutor.waitAndDrain();
        }
        return this.responseFuture.get(timeout, unit);
    }
    
    @Override
    public Object recreate() throws Throwable {
        final RpcInvocation rpcInvocation = (RpcInvocation)this.invocation;
        if (InvokeMode.FUTURE == rpcInvocation.getInvokeMode()) {
            return RpcContext.getContext().getFuture();
        }
        return this.getAppResponse().recreate();
    }
    
    @Override
    public Result whenCompleteWithContext(final BiConsumer<Result, Throwable> fn) {
        this.responseFuture = this.responseFuture.whenComplete((v, t) -> {
            this.beforeContext.accept(v, t);
            fn.accept(v, t);
            this.afterContext.accept(v, t);
            return;
        });
        return this;
    }
    
    @Override
    public <U> CompletableFuture<U> thenApply(final Function<Result, ? extends U> fn) {
        return this.responseFuture.thenApply((Function<? super AppResponse, ? extends U>)fn);
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
    
    public Executor getExecutor() {
        return this.executor;
    }
    
    public void setExecutor(final Executor executor) {
        this.executor = executor;
    }
    
    public static AsyncRpcResult newDefaultAsyncResult(final AppResponse appResponse, final Invocation invocation) {
        return new AsyncRpcResult(CompletableFuture.completedFuture(appResponse), invocation);
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
        final CompletableFuture<AppResponse> future = new CompletableFuture<AppResponse>();
        final AppResponse result = new AppResponse();
        if (t != null) {
            result.setException(t);
        }
        else {
            result.setValue(value);
        }
        future.complete(result);
        return new AsyncRpcResult(future, invocation);
    }
    
    static {
        logger = LoggerFactory.getLogger((Class)AsyncRpcResult.class);
    }
}
