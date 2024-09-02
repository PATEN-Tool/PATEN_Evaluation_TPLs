// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.dubbo.rpc.protocol;

import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.ThreadlessExecutor;
import org.apache.dubbo.common.threadpool.manager.ExecutorRepository;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ExecutionException;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.TimeoutException;
import java.util.concurrent.TimeUnit;
import org.apache.dubbo.rpc.InvokeMode;
import java.util.concurrent.CompletableFuture;
import org.apache.dubbo.rpc.protocol.dubbo.FutureAdapter;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.Set;
import java.util.Collection;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.rpc.PenetrateAttachmentSelector;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.remoting.transport.CodecSupport;
import org.apache.dubbo.rpc.support.RpcUtils;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.common.Version;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.Invocation;
import java.util.HashMap;
import org.apache.dubbo.common.utils.ArrayUtils;
import java.util.Collections;
import java.util.Map;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.rpc.Invoker;

public abstract class AbstractInvoker<T> implements Invoker<T>
{
    protected static final Logger logger;
    private final Class<T> type;
    private final URL url;
    private final Map<String, Object> attachment;
    private volatile boolean available;
    private boolean destroyed;
    
    public AbstractInvoker(final Class<T> type, final URL url) {
        this(type, url, (Map)null);
    }
    
    public AbstractInvoker(final Class<T> type, final URL url, final String[] keys) {
        this(type, url, convertAttachment(url, keys));
    }
    
    public AbstractInvoker(final Class<T> type, final URL url, final Map<String, Object> attachment) {
        this.available = true;
        this.destroyed = false;
        if (type == null) {
            throw new IllegalArgumentException("service type == null");
        }
        if (url == null) {
            throw new IllegalArgumentException("service url == null");
        }
        this.type = type;
        this.url = url;
        this.attachment = ((attachment == null) ? null : Collections.unmodifiableMap((Map<? extends String, ?>)attachment));
    }
    
    private static Map<String, Object> convertAttachment(final URL url, final String[] keys) {
        if (ArrayUtils.isEmpty((Object[])keys)) {
            return null;
        }
        final Map<String, Object> attachment = new HashMap<String, Object>();
        for (final String key : keys) {
            final String value = url.getParameter(key);
            if (value != null && value.length() > 0) {
                attachment.put(key, value);
            }
        }
        return attachment;
    }
    
    @Override
    public Class<T> getInterface() {
        return this.type;
    }
    
    public URL getUrl() {
        return this.url;
    }
    
    public boolean isAvailable() {
        return this.available;
    }
    
    public void destroy() {
        this.destroyed = true;
        this.setAvailable(false);
    }
    
    protected void setAvailable(final boolean available) {
        this.available = available;
    }
    
    public boolean isDestroyed() {
        return this.destroyed;
    }
    
    @Override
    public String toString() {
        return this.getInterface() + " -> " + ((this.getUrl() == null) ? "" : this.getUrl().getAddress());
    }
    
    @Override
    public Result invoke(final Invocation inv) throws RpcException {
        if (this.isDestroyed()) {
            AbstractInvoker.logger.warn("Invoker for service " + this + " on consumer " + NetUtils.getLocalHost() + " is destroyed, , dubbo version is " + Version.getVersion() + ", this invoker should not be used any longer");
        }
        final RpcInvocation invocation = (RpcInvocation)inv;
        this.prepareInvocation(invocation);
        final AsyncRpcResult asyncResult = this.doInvokeAndReturn(invocation);
        this.waitForResultIfSync(asyncResult, invocation);
        return asyncResult;
    }
    
    private void prepareInvocation(final RpcInvocation inv) {
        inv.setInvoker(this);
        this.addInvocationAttachments(inv);
        inv.setInvokeMode(RpcUtils.getInvokeMode(this.url, inv));
        RpcUtils.attachInvocationIdIfAsync(this.getUrl(), inv);
        final Byte serializationId = CodecSupport.getIDByName(this.getUrl().getParameter("serialization", "hessian2"));
        if (serializationId != null) {
            inv.put("serialization_id", serializationId);
        }
    }
    
    private void addInvocationAttachments(final RpcInvocation invocation) {
        if (CollectionUtils.isNotEmptyMap((Map)this.attachment)) {
            invocation.addObjectAttachmentsIfAbsent(this.attachment);
        }
        final Map<String, Object> clientContextAttachments = RpcContext.getClientAttachment().getObjectAttachments();
        if (CollectionUtils.isNotEmptyMap((Map)clientContextAttachments)) {
            invocation.addObjectAttachmentsIfAbsent(clientContextAttachments);
        }
        final ExtensionLoader<PenetrateAttachmentSelector> selectorExtensionLoader = (ExtensionLoader<PenetrateAttachmentSelector>)ExtensionLoader.getExtensionLoader((Class)PenetrateAttachmentSelector.class);
        final Set<String> supportedSelectors = (Set<String>)selectorExtensionLoader.getSupportedExtensions();
        if (CollectionUtils.isNotEmpty((Collection)supportedSelectors)) {
            for (final String supportedSelector : supportedSelectors) {
                final Map<String, Object> selected = ((PenetrateAttachmentSelector)selectorExtensionLoader.getExtension(supportedSelector)).select();
                if (CollectionUtils.isNotEmptyMap((Map)selected)) {
                    invocation.addObjectAttachmentsIfAbsent(selected);
                }
            }
        }
        else {
            final Map<String, Object> serverContextAttachments = RpcContext.getServerAttachment().getObjectAttachments();
            invocation.addObjectAttachmentsIfAbsent(serverContextAttachments);
        }
    }
    
    private AsyncRpcResult doInvokeAndReturn(final RpcInvocation invocation) {
        AsyncRpcResult asyncResult;
        try {
            asyncResult = (AsyncRpcResult)this.doInvoke(invocation);
        }
        catch (InvocationTargetException e) {
            final Throwable te = e.getTargetException();
            if (te != null) {
                if (te instanceof RpcException) {
                    ((RpcException)te).setCode(3);
                }
                asyncResult = AsyncRpcResult.newDefaultAsyncResult(null, te, invocation);
            }
            else {
                asyncResult = AsyncRpcResult.newDefaultAsyncResult(null, e, invocation);
            }
        }
        catch (RpcException e2) {
            if (!e2.isBiz()) {
                throw e2;
            }
            asyncResult = AsyncRpcResult.newDefaultAsyncResult(null, e2, invocation);
        }
        catch (Throwable e3) {
            asyncResult = AsyncRpcResult.newDefaultAsyncResult(null, e3, invocation);
        }
        RpcContext.getServiceContext().setFuture(new FutureAdapter<Object>(asyncResult.getResponseFuture()));
        return asyncResult;
    }
    
    private void waitForResultIfSync(final AsyncRpcResult asyncResult, final RpcInvocation invocation) {
        if (InvokeMode.SYNC != invocation.getInvokeMode()) {
            return;
        }
        try {
            asyncResult.get(2147483647L, TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException e) {
            throw new RpcException("Interrupted unexpectedly while waiting for remote result to return! method: " + invocation.getMethodName() + ", provider: " + this.getUrl() + ", cause: " + e.getMessage(), e);
        }
        catch (ExecutionException e2) {
            final Throwable rootCause = e2.getCause();
            if (rootCause instanceof TimeoutException) {
                throw new RpcException(2, "Invoke remote method timeout. method: " + invocation.getMethodName() + ", provider: " + this.getUrl() + ", cause: " + e2.getMessage(), e2);
            }
            if (rootCause instanceof RemotingException) {
                throw new RpcException(1, "Failed to invoke remote method: " + invocation.getMethodName() + ", provider: " + this.getUrl() + ", cause: " + e2.getMessage(), e2);
            }
            throw new RpcException(0, "Fail to invoke remote method: " + invocation.getMethodName() + ", provider: " + this.getUrl() + ", cause: " + e2.getMessage(), e2);
        }
        catch (Throwable e3) {
            throw new RpcException(e3.getMessage(), e3);
        }
    }
    
    protected ExecutorService getCallbackExecutor(final URL url, final Invocation inv) {
        final ExecutorService sharedExecutor = ((ExecutorRepository)ExtensionLoader.getExtensionLoader((Class)ExecutorRepository.class).getDefaultExtension()).getExecutor(url);
        if (InvokeMode.SYNC == RpcUtils.getInvokeMode(this.getUrl(), inv)) {
            return (ExecutorService)new ThreadlessExecutor(sharedExecutor);
        }
        return sharedExecutor;
    }
    
    protected abstract Result doInvoke(final Invocation invocation) throws Throwable;
    
    static {
        logger = LoggerFactory.getLogger((Class)AbstractInvoker.class);
    }
}
