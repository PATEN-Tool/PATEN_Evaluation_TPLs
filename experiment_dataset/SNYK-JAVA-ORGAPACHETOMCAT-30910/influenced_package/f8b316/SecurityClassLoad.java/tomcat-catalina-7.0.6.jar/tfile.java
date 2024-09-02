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
        loadCoyotePackage(loader);
        loadLoaderPackage(loader);
        loadSessionPackage(loader);
        loadUtilPackage(loader);
        loadJavaxPackage(loader);
        loadConnectorPackage(loader);
        loadTomcatPackage(loader);
    }
    
    private static final void loadCorePackage(final ClassLoader loader) throws Exception {
        final String basePackage = "org.apache.catalina.core.";
        loader.loadClass("org.apache.catalina.core.ApplicationContextFacade$1");
        loader.loadClass("org.apache.catalina.core.ApplicationDispatcher$PrivilegedForward");
        loader.loadClass("org.apache.catalina.core.ApplicationDispatcher$PrivilegedInclude");
        loader.loadClass("org.apache.catalina.core.AsyncContextImpl");
        loader.loadClass("org.apache.catalina.core.AsyncContextImpl$DebugException");
        loader.loadClass("org.apache.catalina.core.AsyncContextImpl$1");
        loader.loadClass("org.apache.catalina.core.AsyncListenerWrapper");
        loader.loadClass("org.apache.catalina.core.ContainerBase$PrivilegedAddChild");
        loader.loadClass("org.apache.catalina.core.DefaultInstanceManager$1");
        loader.loadClass("org.apache.catalina.core.DefaultInstanceManager$2");
        loader.loadClass("org.apache.catalina.core.DefaultInstanceManager$3");
        loader.loadClass("org.apache.catalina.core.DefaultInstanceManager$4");
        loader.loadClass("org.apache.catalina.core.DefaultInstanceManager$5");
        loader.loadClass("org.apache.catalina.core.ApplicationHttpRequest$AttributeNamesEnumerator");
    }
    
    private static final void loadLoaderPackage(final ClassLoader loader) throws Exception {
        final String basePackage = "org.apache.catalina.loader.";
        loader.loadClass("org.apache.catalina.loader.WebappClassLoader$PrivilegedFindResourceByName");
    }
    
    private static final void loadSessionPackage(final ClassLoader loader) throws Exception {
        final String basePackage = "org.apache.catalina.session.";
        loader.loadClass("org.apache.catalina.session.StandardSession");
        loader.loadClass("org.apache.catalina.session.StandardSession$PrivilegedSetTccl");
        loader.loadClass("org.apache.catalina.session.StandardSession$1");
        loader.loadClass("org.apache.catalina.session.StandardManager$PrivilegedDoUnload");
    }
    
    private static final void loadUtilPackage(final ClassLoader loader) throws Exception {
        final String basePackage = "org.apache.catalina.util.";
        loader.loadClass("org.apache.catalina.util.Enumerator");
        loader.loadClass("org.apache.catalina.util.ParameterMap");
    }
    
    private static final void loadCoyotePackage(final ClassLoader loader) throws Exception {
        final String basePackage = "org.apache.coyote.";
        loader.loadClass("org.apache.coyote.http11.AbstractOutputBuffer$1");
    }
    
    private static final void loadJavaxPackage(final ClassLoader loader) throws Exception {
        loader.loadClass("javax.servlet.http.Cookie");
    }
    
    private static final void loadConnectorPackage(final ClassLoader loader) throws Exception {
        final String basePackage = "org.apache.catalina.connector.";
        loader.loadClass("org.apache.catalina.connector.RequestFacade$GetAttributePrivilegedAction");
        loader.loadClass("org.apache.catalina.connector.RequestFacade$GetParameterMapPrivilegedAction");
        loader.loadClass("org.apache.catalina.connector.RequestFacade$GetRequestDispatcherPrivilegedAction");
        loader.loadClass("org.apache.catalina.connector.RequestFacade$GetParameterPrivilegedAction");
        loader.loadClass("org.apache.catalina.connector.RequestFacade$GetParameterNamesPrivilegedAction");
        loader.loadClass("org.apache.catalina.connector.RequestFacade$GetParameterValuePrivilegedAction");
        loader.loadClass("org.apache.catalina.connector.RequestFacade$GetCharacterEncodingPrivilegedAction");
        loader.loadClass("org.apache.catalina.connector.RequestFacade$GetHeadersPrivilegedAction");
        loader.loadClass("org.apache.catalina.connector.RequestFacade$GetHeaderNamesPrivilegedAction");
        loader.loadClass("org.apache.catalina.connector.RequestFacade$GetCookiesPrivilegedAction");
        loader.loadClass("org.apache.catalina.connector.RequestFacade$GetLocalePrivilegedAction");
        loader.loadClass("org.apache.catalina.connector.RequestFacade$GetLocalesPrivilegedAction");
        loader.loadClass("org.apache.catalina.connector.ResponseFacade$SetContentTypePrivilegedAction");
        loader.loadClass("org.apache.catalina.connector.ResponseFacade$DateHeaderPrivilegedAction");
        loader.loadClass("org.apache.catalina.connector.RequestFacade$GetSessionPrivilegedAction");
        loader.loadClass("org.apache.catalina.connector.ResponseFacade$1");
        loader.loadClass("org.apache.catalina.connector.OutputBuffer$1");
        loader.loadClass("org.apache.catalina.connector.CoyoteInputStream$1");
        loader.loadClass("org.apache.catalina.connector.CoyoteInputStream$2");
        loader.loadClass("org.apache.catalina.connector.CoyoteInputStream$3");
        loader.loadClass("org.apache.catalina.connector.CoyoteInputStream$4");
        loader.loadClass("org.apache.catalina.connector.CoyoteInputStream$5");
        loader.loadClass("org.apache.catalina.connector.InputBuffer$1");
        loader.loadClass("org.apache.catalina.connector.Response$1");
        loader.loadClass("org.apache.catalina.connector.Response$2");
        loader.loadClass("org.apache.catalina.connector.Response$3");
    }
    
    private static final void loadTomcatPackage(final ClassLoader loader) throws Exception {
        final String basePackage = "org.apache.tomcat.";
        final Class<?> clazz = loader.loadClass("org.apache.tomcat.util.http.FastHttpDateFormat");
        clazz.newInstance();
        loader.loadClass("org.apache.tomcat.util.http.HttpMessages");
        loader.loadClass("org.apache.tomcat.util.net.Constants");
        loader.loadClass("org.apache.tomcat.util.net.SSLSupport$CipherData");
        loader.loadClass("org.apache.tomcat.util.net.JIoEndpoint$PrivilegedSetTccl");
        loader.loadClass("org.apache.tomcat.util.net.AprEndpoint$PrivilegedSetTccl");
    }
}
