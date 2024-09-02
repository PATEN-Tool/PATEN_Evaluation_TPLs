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
import org.apache.catalina.LifecycleException;
import javax.management.Notification;
import javax.servlet.ServletContext;
import java.util.Collection;
import org.apache.catalina.util.Enumerator;
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
import org.apache.catalina.LifecycleState;
import org.apache.catalina.InstanceListener;
import org.apache.tomcat.PeriodicEventListener;
import javax.servlet.ServletException;
import java.lang.reflect.Method;
import java.util.HashSet;
import javax.servlet.http.HttpServlet;
import org.apache.tomcat.util.ExceptionUtils;
import org.apache.catalina.Context;
import org.apache.catalina.Container;
import org.apache.catalina.Valve;
import javax.management.MBeanNotificationInfo;
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
    protected boolean singleThreadModel;
    protected boolean unloading;
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
    protected static Class<?>[] classType;
    protected static Class<?>[] classTypeUsedInService;
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
        this.swValve = new StandardWrapperValve();
        this.pipeline.setBasic(this.swValve);
        this.broadcaster = new NotificationBroadcasterSupport();
    }
    
    public long getAvailable() {
        return this.available;
    }
    
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
    
    public int getLoadOnStartup() {
        if (this.isJspServlet && this.loadOnStartup < 0) {
            return Integer.MAX_VALUE;
        }
        return this.loadOnStartup;
    }
    
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
    
    public String getRunAs() {
        return this.runAs;
    }
    
    public void setRunAs(final String runAs) {
        final String oldRunAs = this.runAs;
        this.runAs = runAs;
        this.support.firePropertyChange("runAs", oldRunAs, this.runAs);
    }
    
    public String getServletClass() {
        return this.servletClass;
    }
    
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
    
    public boolean isSingleThreadModel() {
        try {
            this.loadServlet();
        }
        catch (Throwable t) {
            ExceptionUtils.handleThrowable(t);
        }
        return this.singleThreadModel;
    }
    
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
    
    public String[] getServletMethods() throws ServletException {
        final Class<? extends Servlet> servletClazz = this.loadServlet().getClass();
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
    
    public Servlet getServlet() {
        return this.instance;
    }
    
    public void setServlet(final Servlet servlet) {
        this.instance = servlet;
    }
    
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
        Throwable rootCause = (Throwable)e;
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
    
    public void addInitParameter(final String name, final String value) {
        synchronized (this.parameters) {
            this.parameters.put(name, value);
        }
        this.fireContainerEvent("addInitParameter", name);
    }
    
    public void addInstanceListener(final InstanceListener listener) {
        this.instanceSupport.addInstanceListener(listener);
    }
    
    public void addMapping(final String mapping) {
        synchronized (this.mappings) {
            this.mappings.add(mapping);
        }
        if (this.parent.getState().equals(LifecycleState.STARTED)) {
            this.fireContainerEvent("addMapping", mapping);
        }
    }
    
    public void addSecurityReference(final String name, final String link) {
        synchronized (this.references) {
            this.references.put(name, link);
        }
        this.fireContainerEvent("addSecurityReference", name);
    }
    
    public Servlet allocate() throws ServletException {
        if (this.unloading) {
            throw new ServletException(StandardWrapper.sm.getString("standardWrapper.unloading", new Object[] { this.getName() }));
        }
        boolean newInstance = false;
        if (!this.singleThreadModel) {
            if (this.instance == null) {
                synchronized (this) {
                    if (this.instance == null) {
                        try {
                            if (StandardWrapper.log.isDebugEnabled()) {
                                StandardWrapper.log.debug((Object)"Allocating non-STM instance");
                            }
                            this.instance = this.loadServlet();
                            if (!this.singleThreadModel) {
                                newInstance = true;
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
                }
            }
            if (!this.instanceInitialized) {
                this.initServlet(this.instance);
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
    
    public String findInitParameter(final String name) {
        synchronized (this.parameters) {
            return this.parameters.get(name);
        }
    }
    
    public String[] findInitParameters() {
        synchronized (this.parameters) {
            final String[] results = new String[this.parameters.size()];
            return this.parameters.keySet().toArray(results);
        }
    }
    
    public String[] findMappings() {
        synchronized (this.mappings) {
            return this.mappings.toArray(new String[this.mappings.size()]);
        }
    }
    
    public String findSecurityReference(final String name) {
        synchronized (this.references) {
            return this.references.get(name);
        }
    }
    
    public String[] findSecurityReferences() {
        synchronized (this.references) {
            final String[] results = new String[this.references.size()];
            return this.references.keySet().toArray(results);
        }
    }
    
    public Wrapper findMappingObject() {
        return (Wrapper)this.getMappingObject();
    }
    
    public synchronized void load() throws ServletException {
        this.instance = this.loadServlet();
        if (this.isJspServlet) {
            final StringBuilder oname = new StringBuilder(MBeanUtils.getDomain(this.getParent()));
            oname.append(":type=JspMonitor,name=");
            oname.append(this.getName());
            oname.append(this.getWebModuleKeyProperties());
            try {
                this.jspMonitorON = new ObjectName(oname.toString());
                Registry.getRegistry((Object)null, (Object)null).registerComponent((Object)this.instance, this.jspMonitorON, (String)null);
            }
            catch (Exception ex) {
                StandardWrapper.log.info((Object)("Error registering JSP monitoring with jmx " + this.instance));
            }
        }
    }
    
    public synchronized Servlet loadServlet() throws ServletException {
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
                throw new ServletException(StandardWrapper.sm.getString("standardWrapper.notClass", new Object[] { this.getName() }));
            }
            final InstanceManager instanceManager = ((StandardContext)this.getParent()).getInstanceManager();
            try {
                servlet = (Servlet)instanceManager.newInstance(this.servletClass);
            }
            catch (ClassCastException e) {
                this.unavailable(null);
                throw new ServletException(StandardWrapper.sm.getString("standardWrapper.notServlet", new Object[] { this.servletClass }), (Throwable)e);
            }
            catch (Throwable e2) {
                ExceptionUtils.handleThrowable(e2);
                this.unavailable(null);
                if (StandardWrapper.log.isDebugEnabled()) {
                    StandardWrapper.log.debug((Object)StandardWrapper.sm.getString("standardWrapper.instantiate", new Object[] { this.servletClass }), e2);
                }
                throw new ServletException(StandardWrapper.sm.getString("standardWrapper.instantiate", new Object[] { this.servletClass }), e2);
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
            this.initServlet(servlet);
            this.singleThreadModel = (servlet instanceof SingleThreadModel);
            if (this.singleThreadModel && this.instancePool == null) {
                this.instancePool = new Stack<Servlet>();
            }
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
    
    public void servletSecurityAnnotationScan() throws ServletException {
        if (this.getServlet() == null) {
            Class<?> clazz = null;
            try {
                clazz = this.getParentClassLoader().loadClass(this.getServletClass());
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
        if (this.instanceInitialized) {
            return;
        }
        try {
            this.instanceSupport.fireInstanceEvent("beforeInit", servlet);
            if (Globals.IS_SECURITY_ENABLED) {
                Object[] args = { this.facade };
                SecurityUtil.doAsPrivilege("init", servlet, StandardWrapper.classType, args);
                args = null;
            }
            else {
                servlet.init((ServletConfig)this.facade);
            }
            this.instanceInitialized = true;
            this.instanceSupport.fireInstanceEvent("afterInit", servlet);
        }
        catch (UnavailableException f) {
            this.instanceSupport.fireInstanceEvent("afterInit", servlet, (Throwable)f);
            this.unavailable(f);
            throw f;
        }
        catch (ServletException f2) {
            this.instanceSupport.fireInstanceEvent("afterInit", servlet, (Throwable)f2);
            throw f2;
        }
        catch (Throwable f3) {
            ExceptionUtils.handleThrowable(f3);
            this.getServletContext().log("StandardWrapper.Throwable", f3);
            this.instanceSupport.fireInstanceEvent("afterInit", servlet, f3);
            throw new ServletException(StandardWrapper.sm.getString("standardWrapper.initException", new Object[] { this.getName() }), f3);
        }
    }
    
    public void removeInitParameter(final String name) {
        synchronized (this.parameters) {
            this.parameters.remove(name);
        }
        this.fireContainerEvent("removeInitParameter", name);
    }
    
    public void removeInstanceListener(final InstanceListener listener) {
        this.instanceSupport.removeInstanceListener(listener);
    }
    
    public void removeMapping(final String mapping) {
        synchronized (this.mappings) {
            this.mappings.remove(mapping);
        }
        if (this.parent.getState().equals(LifecycleState.STARTED)) {
            this.fireContainerEvent("removeMapping", mapping);
        }
    }
    
    public void removeSecurityReference(final String name) {
        synchronized (this.references) {
            this.references.remove(name);
        }
        this.fireContainerEvent("removeSecurityReference", name);
    }
    
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
    
    public void unavailable(final UnavailableException unavailable) {
        this.getServletContext().log(StandardWrapper.sm.getString("standardWrapper.unavailable", new Object[] { this.getName() }));
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
                    StandardWrapper.log.info((Object)StandardWrapper.sm.getString("standardWrapper.waiting", new Object[] { this.countAllocated.toString() }));
                }
                try {
                    Thread.sleep(delay);
                }
                catch (InterruptedException ex) {}
                ++nRetries;
            }
        }
        final PrintStream out = System.out;
        if (this.swallowOutput) {
            SystemLogHandler.startCapture();
        }
        try {
            this.instanceSupport.fireInstanceEvent("beforeDestroy", this.instance);
            if (Globals.IS_SECURITY_ENABLED) {
                SecurityUtil.doAsPrivilege("destroy", this.instance);
                SecurityUtil.remove(this.instance);
            }
            else {
                this.instance.destroy();
            }
            this.instanceSupport.fireInstanceEvent("afterDestroy", this.instance);
            if (!((Context)this.getParent()).getIgnoreAnnotations()) {
                ((StandardContext)this.getParent()).getInstanceManager().destroyInstance((Object)this.instance);
            }
        }
        catch (Throwable t) {
            ExceptionUtils.handleThrowable(t);
            this.instanceSupport.fireInstanceEvent("afterDestroy", this.instance, t);
            this.instance = null;
            this.instancePool = null;
            this.nInstances = 0;
            this.fireContainerEvent("unload", this);
            this.unloading = false;
            throw new ServletException(StandardWrapper.sm.getString("standardWrapper.destroyException", new Object[] { this.getName() }), t);
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
        this.instance = null;
        if (this.isJspServlet && this.jspMonitorON != null) {
            Registry.getRegistry((Object)null, (Object)null).unregisterComponent(this.jspMonitorON);
        }
        if (this.singleThreadModel && this.instancePool != null) {
            try {
                while (!this.instancePool.isEmpty()) {
                    final Servlet s = this.instancePool.pop();
                    if (Globals.IS_SECURITY_ENABLED) {
                        SecurityUtil.doAsPrivilege("destroy", s);
                        SecurityUtil.remove(this.instance);
                    }
                    else {
                        s.destroy();
                    }
                    if (!((Context)this.getParent()).getIgnoreAnnotations()) {
                        ((StandardContext)this.getParent()).getInstanceManager().destroyInstance((Object)s);
                    }
                }
            }
            catch (Throwable t) {
                ExceptionUtils.handleThrowable(t);
                this.instancePool = null;
                this.nInstances = 0;
                this.unloading = false;
                this.fireContainerEvent("unload", this);
                throw new ServletException(StandardWrapper.sm.getString("standardWrapper.destroyException", new Object[] { this.getName() }), t);
            }
            this.instancePool = null;
            this.nInstances = 0;
        }
        this.singleThreadModel = false;
        this.unloading = false;
        this.fireContainerEvent("unload", this);
    }
    
    public String getInitParameter(final String name) {
        return this.findInitParameter(name);
    }
    
    public Enumeration<String> getInitParameterNames() {
        synchronized (this.parameters) {
            return new Enumerator<String>(this.parameters.keySet());
        }
    }
    
    public ServletContext getServletContext() {
        if (this.parent == null) {
            return null;
        }
        if (!(this.parent instanceof Context)) {
            return null;
        }
        return ((Context)this.parent).getServletContext();
    }
    
    public String getServletName() {
        return this.getName();
    }
    
    public long getProcessingTime() {
        return this.swValve.getProcessingTime();
    }
    
    public void setProcessingTime(final long processingTime) {
        this.swValve.setProcessingTime(processingTime);
    }
    
    public long getMaxTime() {
        return this.swValve.getMaxTime();
    }
    
    public void setMaxTime(final long maxTime) {
        this.swValve.setMaxTime(maxTime);
    }
    
    public long getMinTime() {
        return this.swValve.getMinTime();
    }
    
    public void setMinTime(final long minTime) {
        this.swValve.setMinTime(minTime);
    }
    
    public int getRequestCount() {
        return this.swValve.getRequestCount();
    }
    
    public void setRequestCount(final int requestCount) {
        this.swValve.setRequestCount(requestCount);
    }
    
    public int getErrorCount() {
        return this.swValve.getErrorCount();
    }
    
    public void setErrorCount(final int errorCount) {
        this.swValve.setErrorCount(errorCount);
    }
    
    public void incrementErrorCount() {
        this.swValve.setErrorCount(this.swValve.getErrorCount() + 1);
    }
    
    public long getLoadTime() {
        return this.loadTime;
    }
    
    public void setLoadTime(final long loadTime) {
        this.loadTime = loadTime;
    }
    
    public int getClassLoadTime() {
        return this.classLoadTime;
    }
    
    public MultipartConfigElement getMultipartConfigElement() {
        return this.multipartConfigElement;
    }
    
    public void setMultipartConfigElement(final MultipartConfigElement multipartConfigElement) {
        this.multipartConfigElement = multipartConfigElement;
    }
    
    public boolean isAsyncSupported() {
        return this.asyncSupported;
    }
    
    public void setAsyncSupported(final boolean asyncSupported) {
        this.asyncSupported = asyncSupported;
    }
    
    public boolean isEnabled() {
        return this.enabled;
    }
    
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
        if (thisMethods == null) {
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
            this.getServletContext().log(StandardWrapper.sm.getString("standardWrapper.unloadException", new Object[] { this.getName() }), (Throwable)e);
        }
        super.stopInternal();
        if (this.getObjectName() != null) {
            final Notification notification = new Notification("j2ee.state.stopped", this.getObjectName(), this.sequenceNumber++);
            this.broadcaster.sendNotification(notification);
        }
        final Notification notification = new Notification("j2ee.object.deleted", this.getObjectName(), this.sequenceNumber++);
        this.broadcaster.sendNotification(notification);
    }
    
    protected String getObjectNameKeyProperties() {
        final StringBuilder keyProperties = new StringBuilder("j2eeType=Servlet,name=");
        keyProperties.append(this.getName());
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
    
    public void removeNotificationListener(final NotificationListener listener, final NotificationFilter filter, final Object object) throws ListenerNotFoundException {
        this.broadcaster.removeNotificationListener(listener, filter, object);
    }
    
    public MBeanNotificationInfo[] getNotificationInfo() {
        if (this.notificationInfo == null) {
            this.notificationInfo = new MBeanNotificationInfo[] { new MBeanNotificationInfo(new String[] { "j2ee.object.created" }, Notification.class.getName(), "servlet is created"), new MBeanNotificationInfo(new String[] { "j2ee.state.starting" }, Notification.class.getName(), "servlet is starting"), new MBeanNotificationInfo(new String[] { "j2ee.state.running" }, Notification.class.getName(), "servlet is running"), new MBeanNotificationInfo(new String[] { "j2ee.state.stopped" }, Notification.class.getName(), "servlet start to stopped"), new MBeanNotificationInfo(new String[] { "j2ee.object.stopped" }, Notification.class.getName(), "servlet is stopped"), new MBeanNotificationInfo(new String[] { "j2ee.object.deleted" }, Notification.class.getName(), "servlet is deleted") };
        }
        return this.notificationInfo;
    }
    
    public void addNotificationListener(final NotificationListener listener, final NotificationFilter filter, final Object object) throws IllegalArgumentException {
        this.broadcaster.addNotificationListener(listener, filter, object);
    }
    
    public void removeNotificationListener(final NotificationListener listener) throws ListenerNotFoundException {
        this.broadcaster.removeNotificationListener(listener);
    }
    
    public boolean isEventProvider() {
        return false;
    }
    
    public boolean isStateManageable() {
        return false;
    }
    
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
