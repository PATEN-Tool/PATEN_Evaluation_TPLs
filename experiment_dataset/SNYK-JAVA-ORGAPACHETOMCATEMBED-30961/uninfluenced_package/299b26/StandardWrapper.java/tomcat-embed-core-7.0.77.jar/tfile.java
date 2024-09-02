// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.catalina.core;

import javax.servlet.ServletResponse;
import javax.servlet.ServletRequest;
import org.apache.juli.logging.LogFactory;
import javax.management.ListenerNotFoundException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import org.apache.tomcat.util.modeler.Util;
import org.apache.catalina.LifecycleException;
import javax.management.Notification;
import javax.servlet.ServletContext;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import org.apache.catalina.security.SecurityUtil;
import org.apache.catalina.Globals;
import javax.servlet.ServletSecurityElement;
import javax.servlet.annotation.ServletSecurity;
import org.apache.tomcat.InstanceManager;
import java.io.PrintStream;
import javax.servlet.SingleThreadModel;
import org.apache.catalina.ContainerServlet;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.UnavailableException;
import org.apache.tomcat.util.log.SystemLogHandler;
import org.apache.tomcat.util.modeler.Registry;
import org.apache.catalina.mbeans.MBeanUtils;
import org.apache.tomcat.util.ExceptionUtils;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.InstanceListener;
import org.apache.tomcat.PeriodicEventListener;
import javax.servlet.ServletException;
import java.lang.reflect.Method;
import java.util.HashSet;
import javax.servlet.http.HttpServlet;
import org.apache.catalina.Context;
import org.apache.catalina.Container;
import org.apache.catalina.Valve;
import javax.management.MBeanNotificationInfo;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.servlet.MultipartConfigElement;
import javax.management.ObjectName;
import java.util.Stack;
import java.util.HashMap;
import java.util.ArrayList;
import org.apache.catalina.util.InstanceSupport;
import javax.servlet.Servlet;
import java.util.concurrent.atomic.AtomicInteger;
import javax.management.NotificationBroadcasterSupport;
import org.apache.juli.logging.Log;
import javax.management.NotificationEmitter;
import org.apache.catalina.Wrapper;
import javax.servlet.ServletConfig;

public class StandardWrapper extends ContainerBase implements ServletConfig, Wrapper, NotificationEmitter
{
    private static final Log log;
    protected static final String[] DEFAULT_SERVLET_METHODS;
    protected long available;
    protected NotificationBroadcasterSupport broadcaster;
    protected AtomicInteger countAllocated;
    protected StandardWrapperFacade facade;
    protected static final String info = "org.apache.catalina.core.StandardWrapper/1.0";
    protected volatile Servlet instance;
    protected volatile boolean instanceInitialized;
    protected InstanceSupport instanceSupport;
    protected int loadOnStartup;
    protected ArrayList<String> mappings;
    protected HashMap<String, String> parameters;
    protected HashMap<String, String> references;
    protected String runAs;
    protected long sequenceNumber;
    protected String servletClass;
    protected volatile boolean singleThreadModel;
    protected volatile boolean unloading;
    protected int maxInstances;
    protected int nInstances;
    protected Stack<Servlet> instancePool;
    protected long unloadDelay;
    protected boolean isJspServlet;
    protected ObjectName jspMonitorON;
    protected boolean swallowOutput;
    protected StandardWrapperValve swValve;
    protected long loadTime;
    protected int classLoadTime;
    protected MultipartConfigElement multipartConfigElement;
    protected boolean asyncSupported;
    protected boolean enabled;
    protected volatile boolean servletSecurityAnnotationScanRequired;
    private boolean overridable;
    protected static Class<?>[] classType;
    @Deprecated
    protected static Class<?>[] classTypeUsedInService;
    private final ReentrantReadWriteLock parametersLock;
    private final ReentrantReadWriteLock mappingsLock;
    private final ReentrantReadWriteLock referencesLock;
    protected MBeanNotificationInfo[] notificationInfo;
    
    public StandardWrapper() {
        this.available = 0L;
        this.broadcaster = null;
        this.countAllocated = new AtomicInteger(0);
        this.facade = new StandardWrapperFacade(this);
        this.instance = null;
        this.instanceInitialized = false;
        this.instanceSupport = new InstanceSupport(this);
        this.loadOnStartup = -1;
        this.mappings = new ArrayList<String>();
        this.parameters = new HashMap<String, String>();
        this.references = new HashMap<String, String>();
        this.runAs = null;
        this.sequenceNumber = 0L;
        this.servletClass = null;
        this.singleThreadModel = false;
        this.unloading = false;
        this.maxInstances = 20;
        this.nInstances = 0;
        this.instancePool = null;
        this.unloadDelay = 2000L;
        this.swallowOutput = false;
        this.loadTime = 0L;
        this.classLoadTime = 0;
        this.multipartConfigElement = null;
        this.asyncSupported = false;
        this.enabled = true;
        this.servletSecurityAnnotationScanRequired = false;
        this.overridable = false;
        this.parametersLock = new ReentrantReadWriteLock();
        this.mappingsLock = new ReentrantReadWriteLock();
        this.referencesLock = new ReentrantReadWriteLock();
        this.swValve = new StandardWrapperValve();
        this.pipeline.setBasic(this.swValve);
        this.broadcaster = new NotificationBroadcasterSupport();
    }
    
    @Override
    public boolean isOverridable() {
        return this.overridable;
    }
    
    @Override
    public void setOverridable(final boolean overridable) {
        this.overridable = overridable;
    }
    
    @Override
    public long getAvailable() {
        return this.available;
    }
    
    @Override
    public void setAvailable(final long available) {
        final long oldAvailable = this.available;
        if (available > System.currentTimeMillis()) {
            this.available = available;
        }
        else {
            this.available = 0L;
        }
        this.support.firePropertyChange("available", oldAvailable, this.available);
    }
    
    public int getCountAllocated() {
        return this.countAllocated.get();
    }
    
    @Override
    public String getInfo() {
        return "org.apache.catalina.core.StandardWrapper/1.0";
    }
    
    public InstanceSupport getInstanceSupport() {
        return this.instanceSupport;
    }
    
    @Override
    public int getLoadOnStartup() {
        if (this.isJspServlet && this.loadOnStartup < 0) {
            return Integer.MAX_VALUE;
        }
        return this.loadOnStartup;
    }
    
    @Override
    public void setLoadOnStartup(final int value) {
        final int oldLoadOnStartup = this.loadOnStartup;
        this.loadOnStartup = value;
        this.support.firePropertyChange("loadOnStartup", oldLoadOnStartup, (Object)this.loadOnStartup);
    }
    
    public void setLoadOnStartupString(final String value) {
        try {
            this.setLoadOnStartup(Integer.parseInt(value));
        }
        catch (NumberFormatException e) {
            this.setLoadOnStartup(0);
        }
    }
    
    public String getLoadOnStartupString() {
        return Integer.toString(this.getLoadOnStartup());
    }
    
    public int getMaxInstances() {
        return this.maxInstances;
    }
    
    public void setMaxInstances(final int maxInstances) {
        final int oldMaxInstances = this.maxInstances;
        this.maxInstances = maxInstances;
        this.support.firePropertyChange("maxInstances", oldMaxInstances, this.maxInstances);
    }
    
    @Override
    public void setParent(final Container container) {
        if (container != null && !(container instanceof Context)) {
            throw new IllegalArgumentException(StandardWrapper.sm.getString("standardWrapper.notContext"));
        }
        if (container instanceof StandardContext) {
            this.swallowOutput = ((StandardContext)container).getSwallowOutput();
            this.unloadDelay = ((StandardContext)container).getUnloadDelay();
        }
        super.setParent(container);
    }
    
    @Override
    public String getRunAs() {
        return this.runAs;
    }
    
    @Override
    public void setRunAs(final String runAs) {
        final String oldRunAs = this.runAs;
        this.runAs = runAs;
        this.support.firePropertyChange("runAs", oldRunAs, this.runAs);
    }
    
    @Override
    public String getServletClass() {
        return this.servletClass;
    }
    
    @Override
    public void setServletClass(final String servletClass) {
        final String oldServletClass = this.servletClass;
        this.servletClass = servletClass;
        this.support.firePropertyChange("servletClass", oldServletClass, this.servletClass);
        if ("org.apache.jasper.servlet.JspServlet".equals(servletClass)) {
            this.isJspServlet = true;
        }
    }
    
    public void setServletName(final String name) {
        this.setName(name);
    }
    
    public Boolean isSingleThreadModel() {
        if (this.singleThreadModel || this.instance != null) {
            return this.singleThreadModel;
        }
        return null;
    }
    
    @Override
    public boolean isUnavailable() {
        if (!this.isEnabled()) {
            return true;
        }
        if (this.available == 0L) {
            return false;
        }
        if (this.available <= System.currentTimeMillis()) {
            this.available = 0L;
            return false;
        }
        return true;
    }
    
    @Override
    public String[] getServletMethods() throws ServletException {
        this.instance = this.loadServlet();
        final Class<? extends Servlet> servletClazz = this.instance.getClass();
        if (!HttpServlet.class.isAssignableFrom(servletClazz)) {
            return StandardWrapper.DEFAULT_SERVLET_METHODS;
        }
        final HashSet<String> allow = new HashSet<String>();
        allow.add("TRACE");
        allow.add("OPTIONS");
        final Method[] methods = this.getAllDeclaredMethods(servletClazz);
        for (int i = 0; methods != null && i < methods.length; ++i) {
            final Method m = methods[i];
            if (m.getName().equals("doGet")) {
                allow.add("GET");
                allow.add("HEAD");
            }
            else if (m.getName().equals("doPost")) {
                allow.add("POST");
            }
            else if (m.getName().equals("doPut")) {
                allow.add("PUT");
            }
            else if (m.getName().equals("doDelete")) {
                allow.add("DELETE");
            }
        }
        final String[] methodNames = new String[allow.size()];
        return allow.toArray(methodNames);
    }
    
    @Override
    public Servlet getServlet() {
        return this.instance;
    }
    
    @Override
    public void setServlet(final Servlet servlet) {
        this.instance = servlet;
    }
    
    @Override
    public void setServletSecurityAnnotationScanRequired(final boolean b) {
        this.servletSecurityAnnotationScanRequired = b;
    }
    
    @Override
    public void backgroundProcess() {
        super.backgroundProcess();
        if (!this.getState().isAvailable()) {
            return;
        }
        if (this.getServlet() != null && this.getServlet() instanceof PeriodicEventListener) {
            ((PeriodicEventListener)this.getServlet()).periodicEvent();
        }
    }
    
    public static Throwable getRootCause(final ServletException e) {
        Throwable rootCause = e;
        Throwable rootCauseCheck = null;
        int loops = 0;
        do {
            ++loops;
            rootCauseCheck = rootCause.getCause();
            if (rootCauseCheck != null) {
                rootCause = rootCauseCheck;
            }
        } while (rootCauseCheck != null && loops < 20);
        return rootCause;
    }
    
    @Override
    public void addChild(final Container child) {
        throw new IllegalStateException(StandardWrapper.sm.getString("standardWrapper.notChild"));
    }
    
    @Override
    public void addInitParameter(final String name, final String value) {
        try {
            this.parametersLock.writeLock().lock();
            this.parameters.put(name, value);
        }
        finally {
            this.parametersLock.writeLock().unlock();
        }
        this.fireContainerEvent("addInitParameter", name);
    }
    
    @Override
    public void addInstanceListener(final InstanceListener listener) {
        this.instanceSupport.addInstanceListener(listener);
    }
    
    @Override
    public void addMapping(final String mapping) {
        try {
            this.mappingsLock.writeLock().lock();
            this.mappings.add(mapping);
        }
        finally {
            this.mappingsLock.writeLock().unlock();
        }
        if (this.parent.getState().equals(LifecycleState.STARTED)) {
            this.fireContainerEvent("addMapping", mapping);
        }
    }
    
    @Override
    public void addSecurityReference(final String name, final String link) {
        try {
            this.referencesLock.writeLock().lock();
            this.references.put(name, link);
        }
        finally {
            this.referencesLock.writeLock().unlock();
        }
        this.fireContainerEvent("addSecurityReference", name);
    }
    
    @Override
    public Servlet allocate() throws ServletException {
        if (this.unloading) {
            throw new ServletException(StandardWrapper.sm.getString("standardWrapper.unloading", this.getName()));
        }
        boolean newInstance = false;
        if (!this.singleThreadModel) {
            if (this.instance == null || !this.instanceInitialized) {
                synchronized (this) {
                    if (this.instance == null) {
                        try {
                            if (StandardWrapper.log.isDebugEnabled()) {
                                StandardWrapper.log.debug((Object)"Allocating non-STM instance");
                            }
                            this.instance = this.loadServlet();
                            newInstance = true;
                            if (!this.singleThreadModel) {
                                this.countAllocated.incrementAndGet();
                            }
                        }
                        catch (ServletException e) {
                            throw e;
                        }
                        catch (Throwable e2) {
                            ExceptionUtils.handleThrowable(e2);
                            throw new ServletException(StandardWrapper.sm.getString("standardWrapper.allocate"), e2);
                        }
                    }
                    if (!this.instanceInitialized) {
                        this.initServlet(this.instance);
                    }
                }
            }
            if (!this.singleThreadModel) {
                if (StandardWrapper.log.isTraceEnabled()) {
                    StandardWrapper.log.trace((Object)"  Returning non-STM instance");
                }
                if (!newInstance) {
                    this.countAllocated.incrementAndGet();
                }
                return this.instance;
            }
            if (newInstance) {
                synchronized (this.instancePool) {
                    this.instancePool.push(this.instance);
                    ++this.nInstances;
                }
            }
        }
        synchronized (this.instancePool) {
            while (this.countAllocated.get() >= this.nInstances) {
                if (this.nInstances < this.maxInstances) {
                    try {
                        this.instancePool.push(this.loadServlet());
                        ++this.nInstances;
                        continue;
                    }
                    catch (ServletException e) {
                        throw e;
                    }
                    catch (Throwable e2) {
                        ExceptionUtils.handleThrowable(e2);
                        throw new ServletException(StandardWrapper.sm.getString("standardWrapper.allocate"), e2);
                    }
                }
                try {
                    this.instancePool.wait();
                }
                catch (InterruptedException e3) {}
            }
            if (StandardWrapper.log.isTraceEnabled()) {
                StandardWrapper.log.trace((Object)"  Returning allocated STM instance");
            }
            this.countAllocated.incrementAndGet();
            return this.instancePool.pop();
        }
    }
    
    @Override
    public void deallocate(final Servlet servlet) throws ServletException {
        if (!this.singleThreadModel) {
            this.countAllocated.decrementAndGet();
            return;
        }
        synchronized (this.instancePool) {
            this.countAllocated.decrementAndGet();
            this.instancePool.push(servlet);
            this.instancePool.notify();
        }
    }
    
    @Override
    public String findInitParameter(final String name) {
        try {
            this.parametersLock.readLock().lock();
            return this.parameters.get(name);
        }
        finally {
            this.parametersLock.readLock().unlock();
        }
    }
    
    @Override
    public String[] findInitParameters() {
        try {
            this.parametersLock.readLock().lock();
            final String[] results = new String[this.parameters.size()];
            return this.parameters.keySet().toArray(results);
        }
        finally {
            this.parametersLock.readLock().unlock();
        }
    }
    
    @Override
    public String[] findMappings() {
        try {
            this.mappingsLock.readLock().lock();
            return this.mappings.toArray(new String[this.mappings.size()]);
        }
        finally {
            this.mappingsLock.readLock().unlock();
        }
    }
    
    @Override
    public String findSecurityReference(final String name) {
        try {
            this.referencesLock.readLock().lock();
            return this.references.get(name);
        }
        finally {
            this.referencesLock.readLock().unlock();
        }
    }
    
    @Override
    public String[] findSecurityReferences() {
        try {
            this.referencesLock.readLock().lock();
            final String[] results = new String[this.references.size()];
            return this.references.keySet().toArray(results);
        }
        finally {
            this.referencesLock.readLock().unlock();
        }
    }
    
    @Deprecated
    public Wrapper findMappingObject() {
        return (Wrapper)this.getMappingObject();
    }
    
    @Override
    public synchronized void load() throws ServletException {
        this.instance = this.loadServlet();
        if (!this.instanceInitialized) {
            this.initServlet(this.instance);
        }
        if (this.isJspServlet) {
            final StringBuilder oname = new StringBuilder(MBeanUtils.getDomain(this.getParent()));
            oname.append(":type=JspMonitor,name=");
            oname.append(this.getName());
            oname.append(this.getWebModuleKeyProperties());
            try {
                this.jspMonitorON = new ObjectName(oname.toString());
                Registry.getRegistry(null, null).registerComponent(this.instance, this.jspMonitorON, null);
            }
            catch (Exception ex) {
                StandardWrapper.log.info((Object)("Error registering JSP monitoring with jmx " + this.instance));
            }
        }
    }
    
    public synchronized Servlet loadServlet() throws ServletException {
        if (this.unloading) {
            throw new ServletException(StandardWrapper.sm.getString("standardWrapper.unloading", this.getName()));
        }
        if (!this.singleThreadModel && this.instance != null) {
            return this.instance;
        }
        final PrintStream out = System.out;
        if (this.swallowOutput) {
            SystemLogHandler.startCapture();
        }
        Servlet servlet;
        try {
            final long t1 = System.currentTimeMillis();
            if (this.servletClass == null) {
                this.unavailable(null);
                throw new ServletException(StandardWrapper.sm.getString("standardWrapper.notClass", this.getName()));
            }
            final InstanceManager instanceManager = ((StandardContext)this.getParent()).getInstanceManager();
            try {
                servlet = (Servlet)instanceManager.newInstance(this.servletClass);
            }
            catch (ClassCastException e) {
                this.unavailable(null);
                throw new ServletException(StandardWrapper.sm.getString("standardWrapper.notServlet", this.servletClass), e);
            }
            catch (Throwable e2) {
                e2 = ExceptionUtils.unwrapInvocationTargetException(e2);
                ExceptionUtils.handleThrowable(e2);
                this.unavailable(null);
                if (StandardWrapper.log.isDebugEnabled()) {
                    StandardWrapper.log.debug((Object)StandardWrapper.sm.getString("standardWrapper.instantiate", this.servletClass), e2);
                }
                throw new ServletException(StandardWrapper.sm.getString("standardWrapper.instantiate", this.servletClass), e2);
            }
            if (this.multipartConfigElement == null) {
                final MultipartConfig annotation = servlet.getClass().getAnnotation(MultipartConfig.class);
                if (annotation != null) {
                    this.multipartConfigElement = new MultipartConfigElement(annotation);
                }
            }
            this.processServletSecurityAnnotation(servlet.getClass());
            if (servlet instanceof ContainerServlet && (this.isContainerProvidedServlet(this.servletClass) || ((Context)this.getParent()).getPrivileged())) {
                ((ContainerServlet)servlet).setWrapper(this);
            }
            this.classLoadTime = (int)(System.currentTimeMillis() - t1);
            if (servlet instanceof SingleThreadModel) {
                if (this.instancePool == null) {
                    this.instancePool = new Stack<Servlet>();
                }
                this.singleThreadModel = true;
            }
            this.initServlet(servlet);
            this.fireContainerEvent("load", this);
            this.loadTime = System.currentTimeMillis() - t1;
        }
        finally {
            if (this.swallowOutput) {
                final String log = SystemLogHandler.stopCapture();
                if (log != null && log.length() > 0) {
                    if (this.getServletContext() != null) {
                        this.getServletContext().log(log);
                    }
                    else {
                        out.println(log);
                    }
                }
            }
        }
        return servlet;
    }
    
    @Override
    public void servletSecurityAnnotationScan() throws ServletException {
        if (this.getServlet() == null) {
            Class<?> clazz = null;
            try {
                clazz = this.getParent().getLoader().getClassLoader().loadClass(this.getServletClass());
                this.processServletSecurityAnnotation(clazz);
            }
            catch (ClassNotFoundException ex) {}
        }
        else if (this.servletSecurityAnnotationScanRequired) {
            this.processServletSecurityAnnotation(this.getServlet().getClass());
        }
    }
    
    private void processServletSecurityAnnotation(final Class<?> clazz) {
        this.servletSecurityAnnotationScanRequired = false;
        final Context ctxt = (Context)this.getParent();
        if (ctxt.getIgnoreAnnotations()) {
            return;
        }
        final ServletSecurity secAnnotation = clazz.getAnnotation(ServletSecurity.class);
        if (secAnnotation != null) {
            ctxt.addServletSecurity(new ApplicationServletRegistration(this, ctxt), new ServletSecurityElement(secAnnotation));
        }
    }
    
    private synchronized void initServlet(final Servlet servlet) throws ServletException {
        if (this.instanceInitialized && !this.singleThreadModel) {
            return;
        }
        try {
            this.instanceSupport.fireInstanceEvent("beforeInit", servlet);
            if (Globals.IS_SECURITY_ENABLED) {
                boolean success = false;
                try {
                    final Object[] args = { this.facade };
                    SecurityUtil.doAsPrivilege("init", servlet, StandardWrapper.classType, args);
                    success = true;
                }
                finally {
                    if (!success) {
                        SecurityUtil.remove(servlet);
                    }
                }
            }
            else {
                servlet.init(this.facade);
            }
            this.instanceInitialized = true;
            this.instanceSupport.fireInstanceEvent("afterInit", servlet);
        }
        catch (UnavailableException f) {
            this.instanceSupport.fireInstanceEvent("afterInit", servlet, f);
            this.unavailable(f);
            throw f;
        }
        catch (ServletException f2) {
            this.instanceSupport.fireInstanceEvent("afterInit", servlet, f2);
            throw f2;
        }
        catch (Throwable f3) {
            ExceptionUtils.handleThrowable(f3);
            this.getServletContext().log("StandardWrapper.Throwable", f3);
            this.instanceSupport.fireInstanceEvent("afterInit", servlet, f3);
            throw new ServletException(StandardWrapper.sm.getString("standardWrapper.initException", this.getName()), f3);
        }
    }
    
    @Override
    public void removeInitParameter(final String name) {
        try {
            this.parametersLock.writeLock().lock();
            this.parameters.remove(name);
        }
        finally {
            this.parametersLock.writeLock().unlock();
        }
        this.fireContainerEvent("removeInitParameter", name);
    }
    
    @Override
    public void removeInstanceListener(final InstanceListener listener) {
        this.instanceSupport.removeInstanceListener(listener);
    }
    
    @Override
    public void removeMapping(final String mapping) {
        try {
            this.mappingsLock.writeLock().lock();
            this.mappings.remove(mapping);
        }
        finally {
            this.mappingsLock.writeLock().unlock();
        }
        if (this.parent.getState().equals(LifecycleState.STARTED)) {
            this.fireContainerEvent("removeMapping", mapping);
        }
    }
    
    @Override
    public void removeSecurityReference(final String name) {
        try {
            this.referencesLock.writeLock().lock();
            this.references.remove(name);
        }
        finally {
            this.referencesLock.writeLock().unlock();
        }
        this.fireContainerEvent("removeSecurityReference", name);
    }
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        if (this.getParent() != null) {
            sb.append(this.getParent().toString());
            sb.append(".");
        }
        sb.append("StandardWrapper[");
        sb.append(this.getName());
        sb.append("]");
        return sb.toString();
    }
    
    @Override
    public void unavailable(final UnavailableException unavailable) {
        this.getServletContext().log(StandardWrapper.sm.getString("standardWrapper.unavailable", this.getName()));
        if (unavailable == null) {
            this.setAvailable(Long.MAX_VALUE);
        }
        else if (unavailable.isPermanent()) {
            this.setAvailable(Long.MAX_VALUE);
        }
        else {
            int unavailableSeconds = unavailable.getUnavailableSeconds();
            if (unavailableSeconds <= 0) {
                unavailableSeconds = 60;
            }
            this.setAvailable(System.currentTimeMillis() + unavailableSeconds * 1000L);
        }
    }
    
    @Override
    public synchronized void unload() throws ServletException {
        if (!this.singleThreadModel && this.instance == null) {
            return;
        }
        this.unloading = true;
        if (this.countAllocated.get() > 0) {
            int nRetries = 0;
            final long delay = this.unloadDelay / 20L;
            while (nRetries < 21 && this.countAllocated.get() > 0) {
                if (nRetries % 10 == 0) {
                    StandardWrapper.log.info((Object)StandardWrapper.sm.getString("standardWrapper.waiting", this.countAllocated.toString(), this.getName()));
                }
                try {
                    Thread.sleep(delay);
                }
                catch (InterruptedException ex) {}
                ++nRetries;
            }
        }
        if (this.instanceInitialized) {
            final PrintStream out = System.out;
            if (this.swallowOutput) {
                SystemLogHandler.startCapture();
            }
            try {
                this.instanceSupport.fireInstanceEvent("beforeDestroy", this.instance);
                if (Globals.IS_SECURITY_ENABLED) {
                    try {
                        SecurityUtil.doAsPrivilege("destroy", this.instance);
                    }
                    finally {
                        SecurityUtil.remove(this.instance);
                    }
                }
                else {
                    this.instance.destroy();
                }
                this.instanceSupport.fireInstanceEvent("afterDestroy", this.instance);
            }
            catch (Throwable t) {
                t = ExceptionUtils.unwrapInvocationTargetException(t);
                ExceptionUtils.handleThrowable(t);
                this.instanceSupport.fireInstanceEvent("afterDestroy", this.instance, t);
                this.instance = null;
                this.instancePool = null;
                this.nInstances = 0;
                this.fireContainerEvent("unload", this);
                this.unloading = false;
                throw new ServletException(StandardWrapper.sm.getString("standardWrapper.destroyException", this.getName()), t);
            }
            finally {
                if (!((Context)this.getParent()).getIgnoreAnnotations()) {
                    try {
                        ((Context)this.getParent()).getInstanceManager().destroyInstance(this.instance);
                    }
                    catch (Throwable t2) {
                        ExceptionUtils.handleThrowable(t2);
                        StandardWrapper.log.error((Object)StandardWrapper.sm.getString("standardWrapper.destroyInstance", this.getName()), t2);
                    }
                }
                if (this.swallowOutput) {
                    final String log = SystemLogHandler.stopCapture();
                    if (log != null && log.length() > 0) {
                        if (this.getServletContext() != null) {
                            this.getServletContext().log(log);
                        }
                        else {
                            out.println(log);
                        }
                    }
                }
            }
        }
        this.instance = null;
        this.instanceInitialized = false;
        if (this.isJspServlet && this.jspMonitorON != null) {
            Registry.getRegistry(null, null).unregisterComponent(this.jspMonitorON);
        }
        if (this.singleThreadModel && this.instancePool != null) {
            try {
                while (!this.instancePool.isEmpty()) {
                    final Servlet s = this.instancePool.pop();
                    if (Globals.IS_SECURITY_ENABLED) {
                        try {
                            SecurityUtil.doAsPrivilege("destroy", s);
                        }
                        finally {
                            SecurityUtil.remove(s);
                        }
                    }
                    else {
                        s.destroy();
                    }
                    if (!((Context)this.getParent()).getIgnoreAnnotations()) {
                        ((StandardContext)this.getParent()).getInstanceManager().destroyInstance(s);
                    }
                }
            }
            catch (Throwable t3) {
                t3 = ExceptionUtils.unwrapInvocationTargetException(t3);
                ExceptionUtils.handleThrowable(t3);
                this.instancePool = null;
                this.nInstances = 0;
                this.unloading = false;
                this.fireContainerEvent("unload", this);
                throw new ServletException(StandardWrapper.sm.getString("standardWrapper.destroyException", this.getName()), t3);
            }
            this.instancePool = null;
            this.nInstances = 0;
        }
        this.singleThreadModel = false;
        this.unloading = false;
        this.fireContainerEvent("unload", this);
    }
    
    @Override
    public String getInitParameter(final String name) {
        return this.findInitParameter(name);
    }
    
    @Override
    public Enumeration<String> getInitParameterNames() {
        try {
            this.parametersLock.readLock().lock();
            return Collections.enumeration(this.parameters.keySet());
        }
        finally {
            this.parametersLock.readLock().unlock();
        }
    }
    
    @Override
    public ServletContext getServletContext() {
        if (this.parent == null) {
            return null;
        }
        if (!(this.parent instanceof Context)) {
            return null;
        }
        return ((Context)this.parent).getServletContext();
    }
    
    @Override
    public String getServletName() {
        return this.getName();
    }
    
    public long getProcessingTime() {
        return this.swValve.getProcessingTime();
    }
    
    @Deprecated
    public void setProcessingTime(final long processingTime) {
        this.swValve.setProcessingTime(processingTime);
    }
    
    public long getMaxTime() {
        return this.swValve.getMaxTime();
    }
    
    @Deprecated
    public void setMaxTime(final long maxTime) {
        this.swValve.setMaxTime(maxTime);
    }
    
    public long getMinTime() {
        return this.swValve.getMinTime();
    }
    
    @Deprecated
    public void setMinTime(final long minTime) {
        this.swValve.setMinTime(minTime);
    }
    
    public int getRequestCount() {
        return this.swValve.getRequestCount();
    }
    
    @Deprecated
    public void setRequestCount(final int requestCount) {
        this.swValve.setRequestCount(requestCount);
    }
    
    public int getErrorCount() {
        return this.swValve.getErrorCount();
    }
    
    @Deprecated
    public void setErrorCount(final int errorCount) {
        this.swValve.setErrorCount(errorCount);
    }
    
    @Override
    public void incrementErrorCount() {
        this.swValve.incrementErrorCount();
    }
    
    public long getLoadTime() {
        return this.loadTime;
    }
    
    @Deprecated
    public void setLoadTime(final long loadTime) {
        this.loadTime = loadTime;
    }
    
    public int getClassLoadTime() {
        return this.classLoadTime;
    }
    
    @Override
    public MultipartConfigElement getMultipartConfigElement() {
        return this.multipartConfigElement;
    }
    
    @Override
    public void setMultipartConfigElement(final MultipartConfigElement multipartConfigElement) {
        this.multipartConfigElement = multipartConfigElement;
    }
    
    @Override
    public boolean isAsyncSupported() {
        return this.asyncSupported;
    }
    
    @Override
    public void setAsyncSupported(final boolean asyncSupported) {
        this.asyncSupported = asyncSupported;
    }
    
    @Override
    public boolean isEnabled() {
        return this.enabled;
    }
    
    @Override
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }
    
    protected boolean isContainerProvidedServlet(final String classname) {
        if (classname.startsWith("org.apache.catalina.")) {
            return true;
        }
        try {
            final Class<?> clazz = this.getClass().getClassLoader().loadClass(classname);
            return ContainerServlet.class.isAssignableFrom(clazz);
        }
        catch (Throwable t) {
            ExceptionUtils.handleThrowable(t);
            return false;
        }
    }
    
    protected Method[] getAllDeclaredMethods(final Class<?> c) {
        if (c.equals(HttpServlet.class)) {
            return null;
        }
        final Method[] parentMethods = this.getAllDeclaredMethods(c.getSuperclass());
        Method[] thisMethods = c.getDeclaredMethods();
        if (thisMethods.length == 0) {
            return parentMethods;
        }
        if (parentMethods != null && parentMethods.length > 0) {
            final Method[] allMethods = new Method[parentMethods.length + thisMethods.length];
            System.arraycopy(parentMethods, 0, allMethods, 0, parentMethods.length);
            System.arraycopy(thisMethods, 0, allMethods, parentMethods.length, thisMethods.length);
            thisMethods = allMethods;
        }
        return thisMethods;
    }
    
    @Override
    protected synchronized void startInternal() throws LifecycleException {
        if (this.getObjectName() != null) {
            final Notification notification = new Notification("j2ee.state.starting", this.getObjectName(), this.sequenceNumber++);
            this.broadcaster.sendNotification(notification);
        }
        super.startInternal();
        this.setAvailable(0L);
        if (this.getObjectName() != null) {
            final Notification notification = new Notification("j2ee.state.running", this.getObjectName(), this.sequenceNumber++);
            this.broadcaster.sendNotification(notification);
        }
    }
    
    @Override
    protected synchronized void stopInternal() throws LifecycleException {
        this.setAvailable(Long.MAX_VALUE);
        if (this.getObjectName() != null) {
            final Notification notification = new Notification("j2ee.state.stopping", this.getObjectName(), this.sequenceNumber++);
            this.broadcaster.sendNotification(notification);
        }
        try {
            this.unload();
        }
        catch (ServletException e) {
            this.getServletContext().log(StandardWrapper.sm.getString("standardWrapper.unloadException", this.getName()), e);
        }
        super.stopInternal();
        if (this.getObjectName() != null) {
            final Notification notification = new Notification("j2ee.state.stopped", this.getObjectName(), this.sequenceNumber++);
            this.broadcaster.sendNotification(notification);
        }
        final Notification notification = new Notification("j2ee.object.deleted", this.getObjectName(), this.sequenceNumber++);
        this.broadcaster.sendNotification(notification);
    }
    
    @Override
    protected String getObjectNameKeyProperties() {
        final StringBuilder keyProperties = new StringBuilder("j2eeType=Servlet,name=");
        String name = this.getName();
        if (Util.objectNameValueNeedsQuote(name)) {
            name = ObjectName.quote(name);
        }
        keyProperties.append(name);
        keyProperties.append(this.getWebModuleKeyProperties());
        return keyProperties.toString();
    }
    
    private String getWebModuleKeyProperties() {
        final StringBuilder keyProperties = new StringBuilder(",WebModule=//");
        final String hostName = this.getParent().getParent().getName();
        if (hostName == null) {
            keyProperties.append("DEFAULT");
        }
        else {
            keyProperties.append(hostName);
        }
        final String contextName = ((Context)this.getParent()).getName();
        if (!contextName.startsWith("/")) {
            keyProperties.append('/');
        }
        keyProperties.append(contextName);
        StandardContext ctx = null;
        if (this.parent instanceof StandardContext) {
            ctx = (StandardContext)this.getParent();
        }
        keyProperties.append(",J2EEApplication=");
        if (ctx == null) {
            keyProperties.append("none");
        }
        else {
            keyProperties.append(ctx.getJ2EEApplication());
        }
        keyProperties.append(",J2EEServer=");
        if (ctx == null) {
            keyProperties.append("none");
        }
        else {
            keyProperties.append(ctx.getJ2EEServer());
        }
        return keyProperties.toString();
    }
    
    public boolean isStateManageable() {
        return false;
    }
    
    @Override
    public void removeNotificationListener(final NotificationListener listener, final NotificationFilter filter, final Object object) throws ListenerNotFoundException {
        this.broadcaster.removeNotificationListener(listener, filter, object);
    }
    
    @Override
    public MBeanNotificationInfo[] getNotificationInfo() {
        if (this.notificationInfo == null) {
            this.notificationInfo = new MBeanNotificationInfo[] { new MBeanNotificationInfo(new String[] { "j2ee.object.created" }, Notification.class.getName(), "servlet is created"), new MBeanNotificationInfo(new String[] { "j2ee.state.starting" }, Notification.class.getName(), "servlet is starting"), new MBeanNotificationInfo(new String[] { "j2ee.state.running" }, Notification.class.getName(), "servlet is running"), new MBeanNotificationInfo(new String[] { "j2ee.state.stopped" }, Notification.class.getName(), "servlet start to stopped"), new MBeanNotificationInfo(new String[] { "j2ee.object.stopped" }, Notification.class.getName(), "servlet is stopped"), new MBeanNotificationInfo(new String[] { "j2ee.object.deleted" }, Notification.class.getName(), "servlet is deleted") };
        }
        return this.notificationInfo;
    }
    
    @Override
    public void addNotificationListener(final NotificationListener listener, final NotificationFilter filter, final Object object) throws IllegalArgumentException {
        this.broadcaster.addNotificationListener(listener, filter, object);
    }
    
    @Override
    public void removeNotificationListener(final NotificationListener listener) throws ListenerNotFoundException {
        this.broadcaster.removeNotificationListener(listener);
    }
    
    @Deprecated
    public boolean isEventProvider() {
        return false;
    }
    
    @Deprecated
    public boolean isStatisticsProvider() {
        return false;
    }
    
    static {
        log = LogFactory.getLog((Class)StandardWrapper.class);
        DEFAULT_SERVLET_METHODS = new String[] { "GET", "HEAD", "POST" };
        StandardWrapper.classType = (Class<?>[])new Class[] { ServletConfig.class };
        StandardWrapper.classTypeUsedInService = (Class<?>[])new Class[] { ServletRequest.class, ServletResponse.class };
    }
}
