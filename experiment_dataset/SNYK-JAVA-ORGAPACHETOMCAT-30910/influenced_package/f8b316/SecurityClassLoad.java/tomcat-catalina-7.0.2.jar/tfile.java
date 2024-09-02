// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.catalina.security;

public final class SecurityClassLoad
{
    public static void securityClassLoad(final ClassLoader loader) throws Exception {
        if (System.getSecurityManager() == null) {
            return;
        }
        loadCorePackage(loader);
        loadLoaderPackage(loader);
        loadSessionPackage(loader);
        loadUtilPackage(loader);
        loadJavaxPackage(loader);
        loadConnectorPackage(loader);
        loadTomcatPackage(loader);
    }
    
    private static final void loadCorePackage(final ClassLoader loader) throws Exception {
        final String basePackage = "org.apache.catalina.core.";
        loader.loadClass(basePackage + "ApplicationContextFacade$1");
        loader.loadClass(basePackage + "ApplicationDispatcher$PrivilegedForward");
        loader.loadClass(basePackage + "ApplicationDispatcher$PrivilegedInclude");
        loader.loadClass(basePackage + "AsyncContextImpl");
        loader.loadClass(basePackage + "AsyncContextImpl$AsyncState");
        loader.loadClass(basePackage + "AsyncContextImpl$DebugException");
        loader.loadClass(basePackage + "AsyncContextImpl$1");
        loader.loadClass(basePackage + "AsyncListenerWrapper");
        loader.loadClass(basePackage + "ContainerBase$PrivilegedAddChild");
        loader.loadClass(basePackage + "DefaultInstanceManager$1");
        loader.loadClass(basePackage + "DefaultInstanceManager$2");
        loader.loadClass(basePackage + "DefaultInstanceManager$3");
        loader.loadClass(basePackage + "DefaultInstanceManager$4");
        loader.loadClass(basePackage + "DefaultInstanceManager$5");
        loader.loadClass(basePackage + "ApplicationHttpRequest$AttributeNamesEnumerator");
    }
    
    private static final void loadLoaderPackage(final ClassLoader loader) throws Exception {
        final String basePackage = "org.apache.catalina.loader.";
        loader.loadClass(basePackage + "WebappClassLoader$PrivilegedFindResourceByName");
    }
    
    private static final void loadSessionPackage(final ClassLoader loader) throws Exception {
        final String basePackage = "org.apache.catalina.session.";
        loader.loadClass(basePackage + "StandardSession");
        loader.loadClass(basePackage + "StandardSession$PrivilegedSetTccl");
        loader.loadClass(basePackage + "StandardSession$1");
        loader.loadClass(basePackage + "StandardManager$PrivilegedDoUnload");
    }
    
    private static final void loadUtilPackage(final ClassLoader loader) throws Exception {
        final String basePackage = "org.apache.catalina.util.";
        loader.loadClass(basePackage + "Enumerator");
        loader.loadClass(basePackage + "ParameterMap");
    }
    
    private static final void loadJavaxPackage(final ClassLoader loader) throws Exception {
        loader.loadClass("javax.servlet.http.Cookie");
    }
    
    private static final void loadConnectorPackage(final ClassLoader loader) throws Exception {
        final String basePackage = "org.apache.catalina.connector.";
        loader.loadClass(basePackage + "RequestFacade$GetAttributePrivilegedAction");
        loader.loadClass(basePackage + "RequestFacade$GetParameterMapPrivilegedAction");
        loader.loadClass(basePackage + "RequestFacade$GetRequestDispatcherPrivilegedAction");
        loader.loadClass(basePackage + "RequestFacade$GetParameterPrivilegedAction");
        loader.loadClass(basePackage + "RequestFacade$GetParameterNamesPrivilegedAction");
        loader.loadClass(basePackage + "RequestFacade$GetParameterValuePrivilegedAction");
        loader.loadClass(basePackage + "RequestFacade$GetCharacterEncodingPrivilegedAction");
        loader.loadClass(basePackage + "RequestFacade$GetHeadersPrivilegedAction");
        loader.loadClass(basePackage + "RequestFacade$GetHeaderNamesPrivilegedAction");
        loader.loadClass(basePackage + "RequestFacade$GetCookiesPrivilegedAction");
        loader.loadClass(basePackage + "RequestFacade$GetLocalePrivilegedAction");
        loader.loadClass(basePackage + "RequestFacade$GetLocalesPrivilegedAction");
        loader.loadClass(basePackage + "ResponseFacade$SetContentTypePrivilegedAction");
        loader.loadClass(basePackage + "ResponseFacade$DateHeaderPrivilegedAction");
        loader.loadClass(basePackage + "RequestFacade$GetSessionPrivilegedAction");
        loader.loadClass(basePackage + "ResponseFacade$1");
        loader.loadClass(basePackage + "OutputBuffer$1");
        loader.loadClass(basePackage + "CoyoteInputStream$1");
        loader.loadClass(basePackage + "CoyoteInputStream$2");
        loader.loadClass(basePackage + "CoyoteInputStream$3");
        loader.loadClass(basePackage + "CoyoteInputStream$4");
        loader.loadClass(basePackage + "CoyoteInputStream$5");
        loader.loadClass(basePackage + "InputBuffer$1");
        loader.loadClass(basePackage + "Response$1");
        loader.loadClass(basePackage + "Response$2");
        loader.loadClass(basePackage + "Response$3");
    }
    
    private static final void loadTomcatPackage(final ClassLoader loader) throws Exception {
        final String basePackage = "org.apache.tomcat.";
        loader.loadClass(basePackage + "util.net.SSLSupport$CipherData");
        loader.loadClass(basePackage + "util.net.JIoEndpoint$PrivilegedSetTccl");
        final Class<?> clazz = loader.loadClass(basePackage + "util.http.FastHttpDateFormat");
        clazz.newInstance();
    }
}
