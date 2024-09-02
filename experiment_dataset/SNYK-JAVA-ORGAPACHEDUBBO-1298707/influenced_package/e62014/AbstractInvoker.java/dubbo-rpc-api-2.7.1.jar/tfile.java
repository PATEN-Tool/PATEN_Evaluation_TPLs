// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.dubbo.rpc.protocol;

import java.lang.reflect.InvocationTargetException;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcResult;
import org.apache.dubbo.rpc.support.RpcUtils;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.common.Version;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.Invocation;
import java.util.HashMap;
import org.apache.dubbo.common.utils.ArrayUtils;
import java.util.Collections;
import org.apache.dubbo.common.logger.LoggerFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Map;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.rpc.Invoker;

public abstract class AbstractInvoker<T> implements Invoker<T>
{
    protected final Logger logger;
    private final Class<T> type;
    private final URL url;
    private final Map<String, String> attachment;
    private volatile boolean available;
    private AtomicBoolean destroyed;
    
    public AbstractInvoker(final Class<T> type, final URL url) {
        this(type, url, (Map)null);
    }
    
    public AbstractInvoker(final Class<T> type, final URL url, final String[] keys) {
        this(type, url, convertAttachment(url, keys));
    }
    
    public AbstractInvoker(final Class<T> type, final URL url, final Map<String, String> attachment) {
        this.logger = LoggerFactory.getLogger((Class)this.getClass());
        this.available = true;
        this.destroyed = new AtomicBoolean(false);
        if (type == null) {
            throw new IllegalArgumentException("service type == null");
        }
        if (url == null) {
            throw new IllegalArgumentException("service url == null");
        }
        this.type = type;
        this.url = url;
        this.attachment = ((attachment == null) ? null : Collections.unmodifiableMap((Map<? extends String, ? extends String>)attachment));
    }
    
    private static Map<String, String> convertAttachment(final URL url, final String[] keys) {
        if (ArrayUtils.isEmpty((Object[])keys)) {
            return null;
        }
        final Map<String, String> attachment = new HashMap<String, String>();
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
    
    protected void setAvailable(final boolean available) {
        this.available = available;
    }
    
    public void destroy() {
        if (!this.destroyed.compareAndSet(false, true)) {
            return;
        }
        this.setAvailable(false);
    }
    
    public boolean isDestroyed() {
        return this.destroyed.get();
    }
    
    @Override
    public String toString() {
        return this.getInterface() + " -> " + ((this.getUrl() == null) ? "" : this.getUrl().toString());
    }
    
    @Override
    public Result invoke(final Invocation inv) throws RpcException {
        if (this.destroyed.get()) {
            this.logger.warn("Invoker for service " + this + " on consumer " + NetUtils.getLocalHost() + " is destroyed, , dubbo version is " + Version.getVersion() + ", this invoker should not be used any longer");
        }
        final RpcInvocation invocation = (RpcInvocation)inv;
        invocation.setInvoker(this);
        if (CollectionUtils.isNotEmptyMap((Map)this.attachment)) {
            invocation.addAttachmentsIfAbsent(this.attachment);
        }
        final Map<String, String> contextAttachments = RpcContext.getContext().getAttachments();
        if (CollectionUtils.isNotEmptyMap((Map)contextAttachments)) {
            invocation.addAttachments(contextAttachments);
        }
        if (this.getUrl().getMethodParameter(invocation.getMethodName(), "async", false)) {
            invocation.setAttachment("async", Boolean.TRUE.toString());
        }
        RpcUtils.attachInvocationIdIfAsync(this.getUrl(), invocation);
        try {
            return this.doInvoke(invocation);
        }
        catch (InvocationTargetException e) {
            final Throwable te = e.getTargetException();
            if (te == null) {
                return new RpcResult(e);
            }
            if (te instanceof RpcException) {
                ((RpcException)te).setCode(3);
            }
            return new RpcResult(te);
        }
        catch (RpcException e2) {
            if (e2.isBiz()) {
                return new RpcResult(e2);
            }
            throw e2;
        }
        catch (Throwable e3) {
            return new RpcResult(e3);
        }
    }
    
    protected abstract Result doInvoke(final Invocation p0) throws Throwable;
}
