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
        loadRealmPackage(loader);
        loadServletsPackage(loader);
        loadSessionPackage(loader);
        loadUtilPackage(loader);
        loadValvesPackage(loader);
        loadJavaxPackage(loader);
        loadConnectorPackage(loader);
        loadTomcatPackage(loader);
    }
    
    private static final void loadCorePackage(final ClassLoader loader) throws Exception {
        final String basePackage = "org.apache.catalina.core.";
        loader.loadClass("org.apache.catalina.core.AccessLogAdapter");
        loadAnonymousInnerClasses(loader, "org.apache.catalina.core.ApplicationContextFacade");
        loader.loadClass("org.apache.catalina.core.ApplicationDispatcher$PrivilegedForward");
        loader.loadClass("org.apache.catalina.core.ApplicationDispatcher$PrivilegedInclude");
        loader.loadClass("org.apache.catalina.core.AsyncContextImpl");
        loader.loadClass("org.apache.catalina.core.AsyncContextImpl$DebugException");
        loadAnonymousInnerClasses(loader, "org.apache.catalina.core.AsyncContextImpl");
        loader.loadClass("org.apache.catalina.core.AsyncListenerWrapper");
        loader.loadClass("org.apache.catalina.core.ContainerBase$PrivilegedAddChild");
        loadAnonymousInnerClasses(loader, "org.apache.catalina.core.DefaultInstanceManager");
        loader.loadClass("org.apache.catalina.core.DefaultInstanceManager$AnnotationCacheEntry");
        loader.loadClass("org.apache.catalina.core.DefaultInstanceManager$AnnotationCacheEntryType");
        loader.loadClass("org.apache.catalina.core.ApplicationHttpRequest$AttributeNamesEnumerator");
    }
    
    private static final void loadLoaderPackage(final ClassLoader loader) throws Exception {
        final String basePackage = "org.apache.catalina.loader.";
        loader.loadClass("org.apache.catalina.loader.ResourceEntry");
        loader.loadClass("org.apache.catalina.loader.WebappClassLoaderBase$PrivilegedFindResourceByName");
    }
    
    private static final void loadRealmPackage(final ClassLoader loader) throws Exception {
        final String basePackage = "org.apache.catalina.realm.";
        loader.loadClass("org.apache.catalina.realm.LockOutRealm$LockRecord");
    }
    
    private static final void loadServletsPackage(final ClassLoader loader) throws Exception {
        final String basePackage = "org.apache.catalina.servlets.";
        loader.loadClass("org.apache.catalina.servlets.DefaultServlet");
    }
    
    private static final void loadSessionPackage(final ClassLoader loader) throws Exception {
        final String basePackage = "org.apache.catalina.session.";
        loader.loadClass("org.apache.catalina.session.StandardSession");
        loadAnonymousInnerClasses(loader, "org.apache.catalina.session.StandardSession");
        loader.loadClass("org.apache.catalina.session.StandardManager$PrivilegedDoUnload");
    }
    
    private static final void loadUtilPackage(final ClassLoader loader) throws Exception {
        final String basePackage = "org.apache.catalina.util.";
        loader.loadClass("org.apache.catalina.util.Enumerator");
        loader.loadClass("org.apache.catalina.util.ParameterMap");
        loader.loadClass("org.apache.catalina.util.RequestUtil");
    }
    
    private static final void loadValvesPackage(final ClassLoader loader) throws Exception {
        final String basePackage = "org.apache.catalina.valves.";
        loader.loadClass("org.apache.catalina.valves.AccessLogValve$3");
    }
    
    private static final void loadCoyotePackage(final ClassLoader loader) throws Exception {
        final String basePackage = "org.apache.coyote.";
        loadAnonymousInnerClasses(loader, "org.apache.coyote.http11.AbstractHttp11Processor");
        loadAnonymousInnerClasses(loader, "org.apache.coyote.http11.Http11Processor");
        loadAnonymousInnerClasses(loader, "org.apache.coyote.http11.Http11NioProcessor");
        loadAnonymousInnerClasses(loader, "org.apache.coyote.http11.Http11AprProcessor");
        loadAnonymousInnerClasses(loader, "org.apache.coyote.http11.AbstractOutputBuffer");
        loader.loadClass("org.apache.coyote.http11.Constants");
        final Class<?> clazz = loader.loadClass("org.apache.coyote.Constants");
        clazz.newInstance();
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
        loadAnonymousInnerClasses(loader, "org.apache.catalina.connector.ResponseFacade");
        loadAnonymousInnerClasses(loader, "org.apache.catalina.connector.OutputBuffer");
        loadAnonymousInnerClasses(loader, "org.apache.catalina.connector.CoyoteInputStream");
        loadAnonymousInnerClasses(loader, "org.apache.catalina.connector.InputBuffer");
        loadAnonymousInnerClasses(loader, "org.apache.catalina.connector.Response");
    }
    
    private static final void loadTomcatPackage(final ClassLoader loader) throws Exception {
        final String basePackage = "org.apache.tomcat.";
        loader.loadClass("org.apache.tomcat.util.buf.B2CConverter");
        loader.loadClass("org.apache.tomcat.util.buf.C2BConverter");
        loader.loadClass("org.apache.tomcat.util.buf.HexUtils");
        loader.loadClass("org.apache.tomcat.util.buf.StringCache");
        loader.loadClass("org.apache.tomcat.util.buf.StringCache$ByteEntry");
        loader.loadClass("org.apache.tomcat.util.buf.StringCache$CharEntry");
        loader.loadClass("org.apache.tomcat.util.buf.UriUtil");
        loader.loadClass("org.apache.tomcat.util.http.HttpMessages");
        final Class<?> clazz = loader.loadClass("org.apache.tomcat.util.http.FastHttpDateFormat");
        clazz.newInstance();
        loader.loadClass("org.apache.tomcat.util.http.HttpMessages");
        loader.loadClass("org.apache.tomcat.util.http.parser.HttpParser");
        loader.loadClass("org.apache.tomcat.util.http.parser.HttpParser$DomainParseState");
        loader.loadClass("org.apache.tomcat.util.http.parser.HttpParser$SkipResult");
        loader.loadClass("org.apache.tomcat.util.http.parser.MediaType");
        loader.loadClass("org.apache.tomcat.util.http.parser.MediaTypeCache");
        loader.loadClass("org.apache.tomcat.jni.Status");
        loader.loadClass("org.apache.tomcat.util.net.Constants");
        loadAnonymousInnerClasses(loader, "org.apache.tomcat.util.net.NioBlockingSelector$BlockPoller");
        loader.loadClass("org.apache.tomcat.util.net.SendfileState");
        loader.loadClass("org.apache.tomcat.util.net.SSLSupport$CipherData");
        loader.loadClass("org.apache.tomcat.util.security.PrivilegedGetTccl");
        loader.loadClass("org.apache.tomcat.util.security.PrivilegedSetTccl");
    }
    
    private static final void loadAnonymousInnerClasses(final ClassLoader loader, final String enclosingClass) {
        try {
            int i = 1;
            while (true) {
                loader.loadClass(enclosingClass + '$' + i);
                ++i;
            }
        }
        catch (ClassNotFoundException ignored) {}
    }
}
