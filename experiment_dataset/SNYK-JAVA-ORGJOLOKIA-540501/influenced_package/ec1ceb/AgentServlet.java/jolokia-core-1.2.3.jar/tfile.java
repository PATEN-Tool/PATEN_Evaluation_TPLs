// 
// Decompiled by Procyon v0.5.36
// 

package org.jolokia.http;

import javax.servlet.ServletContext;
import java.io.PrintWriter;
import java.util.Collections;
import org.jolokia.config.ConfigExtractor;
import java.util.Enumeration;
import java.util.HashMap;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import javax.security.auth.Subject;
import org.json.simple.JSONAware;
import javax.management.RuntimeMBeanException;
import java.util.Iterator;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.jolokia.discovery.AgentDetailsHolder;
import javax.servlet.ServletException;
import org.jolokia.config.Configuration;
import org.jolokia.util.NetworkUtil;
import org.jolokia.util.ClassUtil;
import org.jolokia.config.ConfigKey;
import javax.servlet.ServletConfig;
import java.io.IOException;
import org.jolokia.restrictor.DenyAllRestrictor;
import org.jolokia.restrictor.AllowAllRestrictor;
import org.jolokia.restrictor.RestrictorFactory;
import org.jolokia.discovery.DiscoveryMulticastResponder;
import org.jolokia.restrictor.Restrictor;
import org.jolokia.util.LogHandler;
import org.jolokia.backend.BackendManager;
import javax.servlet.http.HttpServlet;

public class AgentServlet extends HttpServlet
{
    private static final long serialVersionUID = 42L;
    private ServletRequestHandler httpGetHandler;
    private ServletRequestHandler httpPostHandler;
    private BackendManager backendManager;
    private LogHandler logHandler;
    private HttpRequestHandler requestHandler;
    private Restrictor restrictor;
    private String configMimeType;
    private DiscoveryMulticastResponder discoveryMulticastResponder;
    private boolean initAgentUrlFromRequest;
    
    public AgentServlet() {
        this(null);
    }
    
    public AgentServlet(final Restrictor pRestrictor) {
        this.initAgentUrlFromRequest = false;
        this.restrictor = pRestrictor;
    }
    
    protected LogHandler getLogHandler() {
        return this.logHandler;
    }
    
    protected Restrictor createRestrictor(final String pLocation) {
        final LogHandler log = this.getLogHandler();
        try {
            final Restrictor newRestrictor = RestrictorFactory.lookupPolicyRestrictor(pLocation);
            if (newRestrictor != null) {
                log.info("Using access restrictor " + pLocation);
                return newRestrictor;
            }
            log.info("No access restrictor found at " + pLocation + ", access to all MBeans is allowed");
            return new AllowAllRestrictor();
        }
        catch (IOException e) {
            log.error("Error while accessing access restrictor at " + pLocation + ". Denying all access to MBeans for security reasons. Exception: " + e, e);
            return new DenyAllRestrictor();
        }
    }
    
    public void init(final ServletConfig pServletConfig) throws ServletException {
        super.init(pServletConfig);
        final Configuration config = this.initConfig(pServletConfig);
        final String logHandlerClass = config.get(ConfigKey.LOGHANDLER_CLASS);
        this.logHandler = ((logHandlerClass != null) ? ClassUtil.newInstance(logHandlerClass) : this.createLogHandler(pServletConfig, Boolean.valueOf(config.get(ConfigKey.DEBUG))));
        this.httpGetHandler = this.newGetHttpRequestHandler();
        this.httpPostHandler = this.newPostHttpRequestHandler();
        if (this.restrictor == null) {
            this.restrictor = this.createRestrictor(NetworkUtil.replaceExpression(config.get(ConfigKey.POLICY_LOCATION)));
        }
        else {
            this.logHandler.info("Using custom access restriction provided by " + this.restrictor);
        }
        this.configMimeType = config.get(ConfigKey.MIME_TYPE);
        this.backendManager = new BackendManager(config, this.logHandler, this.restrictor);
        this.requestHandler = new HttpRequestHandler(config, this.backendManager, this.logHandler);
        this.initDiscoveryMulticast(config);
    }
    
    private void initDiscoveryMulticast(final Configuration pConfig) {
        final String url = this.findAgentUrl(pConfig);
        if (url != null || this.listenForDiscoveryMcRequests(pConfig)) {
            if (url == null) {
                this.initAgentUrlFromRequest = true;
            }
            else {
                this.initAgentUrlFromRequest = false;
                this.backendManager.getAgentDetails().updateAgentParameters(url, null);
            }
            try {
                (this.discoveryMulticastResponder = new DiscoveryMulticastResponder(this.backendManager, this.restrictor, this.logHandler)).start();
            }
            catch (IOException e) {
                this.logHandler.error("Cannot start discovery multicast handler: " + e, e);
            }
        }
    }
    
    private String findAgentUrl(final Configuration pConfig) {
        String url = System.getProperty("jolokia." + ConfigKey.DISCOVERY_AGENT_URL.getKeyValue());
        if (url == null) {
            url = System.getenv("JOLOKIA_DISCOVERY_AGENT_URL");
            if (url == null) {
                url = pConfig.get(ConfigKey.DISCOVERY_AGENT_URL);
            }
        }
        return NetworkUtil.replaceExpression(url);
    }
    
    private boolean listenForDiscoveryMcRequests(final Configuration pConfig) {
        final boolean sysProp = System.getProperty("jolokia." + ConfigKey.DISCOVERY_ENABLED.getKeyValue()) != null;
        final boolean env = System.getenv("JOLOKIA_DISCOVERY") != null;
        final boolean config = pConfig.getAsBoolean(ConfigKey.DISCOVERY_ENABLED);
        return sysProp || env || config;
    }
    
    protected LogHandler createLogHandler(final ServletConfig pServletConfig, final boolean pDebug) {
        return new LogHandler() {
            public void debug(final String message) {
                if (pDebug) {
                    AgentServlet.this.log(message);
                }
            }
            
            public void info(final String message) {
                AgentServlet.this.log(message);
            }
            
            public void error(final String message, final Throwable t) {
                AgentServlet.this.log(message, t);
            }
        };
    }
    
    public void destroy() {
        this.backendManager.destroy();
        if (this.discoveryMulticastResponder != null) {
            this.discoveryMulticastResponder.stop();
            this.discoveryMulticastResponder = null;
        }
        super.destroy();
    }
    
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        this.handle(this.httpGetHandler, req, resp);
    }
    
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        this.handle(this.httpPostHandler, req, resp);
    }
    
    protected void doOptions(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        final Map<String, String> responseHeaders = this.requestHandler.handleCorsPreflightRequest(req.getHeader("Origin"), req.getHeader("Access-Control-Request-Headers"));
        for (final Map.Entry<String, String> entry : responseHeaders.entrySet()) {
            resp.setHeader((String)entry.getKey(), (String)entry.getValue());
        }
    }
    
    private void handle(final ServletRequestHandler pReqHandler, final HttpServletRequest pReq, final HttpServletResponse pResp) throws IOException {
        JSONAware json = null;
        try {
            this.requestHandler.checkAccess(pReq.getRemoteHost(), pReq.getRemoteAddr(), this.getOriginOrReferer(pReq));
            this.updateAgentUrlIfNeeded(pReq);
            json = this.handleSecurely(pReqHandler, pReq, pResp);
        }
        catch (Throwable exp) {
            json = (JSONAware)this.requestHandler.handleThrowable((exp instanceof RuntimeMBeanException) ? ((RuntimeMBeanException)exp).getTargetException() : exp);
        }
        finally {
            this.setCorsHeader(pReq, pResp);
            final String callback = pReq.getParameter(ConfigKey.CALLBACK.getKeyValue());
            final String answer = (json != null) ? json.toJSONString() : this.requestHandler.handleThrowable(new Exception("Internal error while handling an exception")).toJSONString();
            if (callback != null) {
                this.sendResponse(pResp, "text/javascript", callback + "(" + answer + ");");
            }
            else {
                this.sendResponse(pResp, this.getMimeType(pReq), answer);
            }
        }
    }
    
    private JSONAware handleSecurely(final ServletRequestHandler pReqHandler, final HttpServletRequest pReq, final HttpServletResponse pResp) throws IOException, PrivilegedActionException {
        final Subject subject = (Subject)pReq.getAttribute("org.jolokia.jaasSubject");
        if (subject != null) {
            return Subject.doAs(subject, (PrivilegedExceptionAction<JSONAware>)new PrivilegedExceptionAction<JSONAware>() {
                public JSONAware run() throws IOException {
                    return pReqHandler.handleRequest(pReq, pResp);
                }
            });
        }
        return pReqHandler.handleRequest(pReq, pResp);
    }
    
    private String getOriginOrReferer(final HttpServletRequest pReq) {
        String origin = pReq.getHeader("Origin");
        if (origin == null) {
            origin = pReq.getHeader("Referer");
        }
        return (origin != null) ? origin.replaceAll("[\\n\\r]*", "") : null;
    }
    
    private void updateAgentUrlIfNeeded(final HttpServletRequest pReq) {
        if (this.initAgentUrlFromRequest) {
            this.updateAgentUrl(NetworkUtil.sanitizeLocalUrl(pReq.getRequestURL().toString()), this.extractServletPath(pReq), pReq.getAuthType() != null);
            this.initAgentUrlFromRequest = false;
        }
    }
    
    private String extractServletPath(final HttpServletRequest pReq) {
        return pReq.getRequestURI().substring(0, pReq.getContextPath().length());
    }
    
    private void updateAgentUrl(final String pRequestUrl, final String pServletPath, final boolean pIsAuthenticated) {
        final String url = this.getBaseUrl(pRequestUrl, pServletPath);
        this.backendManager.getAgentDetails().updateAgentParameters(url, pIsAuthenticated);
    }
    
    private String getBaseUrl(final String pUrl, final String pServletPath) {
        String sUrl;
        try {
            final URL url = new URL(pUrl);
            final String host = this.getIpIfPossible(url.getHost());
            sUrl = new URL(url.getProtocol(), host, url.getPort(), pServletPath).toExternalForm();
        }
        catch (MalformedURLException exp) {
            sUrl = this.plainReplacement(pUrl, pServletPath);
        }
        return sUrl;
    }
    
    private String getIpIfPossible(final String pHost) {
        try {
            final InetAddress address = InetAddress.getByName(pHost);
            return address.getHostAddress();
        }
        catch (UnknownHostException e) {
            return pHost;
        }
    }
    
    private String plainReplacement(final String pUrl, final String pServletPath) {
        final int idx = pUrl.lastIndexOf(pServletPath);
        String url;
        if (idx != -1) {
            url = pUrl.substring(0, idx) + pServletPath;
        }
        else {
            url = pUrl;
        }
        return url;
    }
    
    private void setCorsHeader(final HttpServletRequest pReq, final HttpServletResponse pResp) {
        final String origin = this.requestHandler.extractCorsOrigin(pReq.getHeader("Origin"));
        if (origin != null) {
            pResp.setHeader("Access-Control-Allow-Origin", origin);
            pResp.setHeader("Access-Control-Allow-Credentials", "true");
        }
    }
    
    private String getMimeType(final HttpServletRequest pReq) {
        final String requestMimeType = pReq.getParameter(ConfigKey.MIME_TYPE.getKeyValue());
        if (requestMimeType != null) {
            return requestMimeType;
        }
        return this.configMimeType;
    }
    
    private ServletRequestHandler newPostHttpRequestHandler() {
        return new ServletRequestHandler() {
            public JSONAware handleRequest(final HttpServletRequest pReq, final HttpServletResponse pResp) throws IOException {
                final String encoding = pReq.getCharacterEncoding();
                final InputStream is = (InputStream)pReq.getInputStream();
                return AgentServlet.this.requestHandler.handlePostRequest(pReq.getRequestURI(), is, encoding, AgentServlet.this.getParameterMap(pReq));
            }
        };
    }
    
    private ServletRequestHandler newGetHttpRequestHandler() {
        return new ServletRequestHandler() {
            public JSONAware handleRequest(final HttpServletRequest pReq, final HttpServletResponse pResp) {
                return AgentServlet.this.requestHandler.handleGetRequest(pReq.getRequestURI(), pReq.getPathInfo(), AgentServlet.this.getParameterMap(pReq));
            }
        };
    }
    
    private Map<String, String[]> getParameterMap(final HttpServletRequest pReq) {
        try {
            return (Map<String, String[]>)pReq.getParameterMap();
        }
        catch (UnsupportedOperationException exp) {
            final Map<String, String[]> ret = new HashMap<String, String[]>();
            final Enumeration params = pReq.getParameterNames();
            while (params.hasMoreElements()) {
                final String param = params.nextElement();
                ret.put(param, pReq.getParameterValues(param));
            }
            return ret;
        }
    }
    
    Configuration initConfig(final ServletConfig pConfig) {
        final Configuration config = new Configuration(new Object[] { ConfigKey.AGENT_ID, NetworkUtil.getAgentId(this.hashCode(), "servlet") });
        config.updateGlobalConfiguration(new ServletConfigFacade(pConfig));
        config.updateGlobalConfiguration(new ServletContextFacade(this.getServletContext()));
        config.updateGlobalConfiguration(Collections.singletonMap(ConfigKey.AGENT_TYPE.getKeyValue(), "servlet"));
        return config;
    }
    
    private void sendResponse(final HttpServletResponse pResp, final String pContentType, final String pJsonTxt) throws IOException {
        this.setContentType(pResp, pContentType);
        pResp.setStatus(200);
        this.setNoCacheHeaders(pResp);
        final PrintWriter writer = pResp.getWriter();
        writer.write(pJsonTxt);
    }
    
    private void setNoCacheHeaders(final HttpServletResponse pResp) {
        pResp.setHeader("Cache-Control", "no-cache");
        pResp.setHeader("Pragma", "no-cache");
        final long now = System.currentTimeMillis();
        pResp.setDateHeader("Date", now);
        pResp.setDateHeader("Expires", now - 3600000L);
    }
    
    private void setContentType(final HttpServletResponse pResp, final String pContentType) {
        boolean encodingDone = false;
        try {
            pResp.setCharacterEncoding("utf-8");
            pResp.setContentType(pContentType);
            encodingDone = true;
        }
        catch (NoSuchMethodError error) {}
        catch (UnsupportedOperationException ex) {}
        if (!encodingDone) {
            pResp.setContentType(pContentType + "; charset=utf-8");
        }
    }
    
    private static final class ServletConfigFacade implements ConfigExtractor
    {
        private final ServletConfig config;
        
        private ServletConfigFacade(final ServletConfig pConfig) {
            this.config = pConfig;
        }
        
        public Enumeration getNames() {
            return this.config.getInitParameterNames();
        }
        
        public String getParameter(final String pName) {
            return this.config.getInitParameter(pName);
        }
    }
    
    private static final class ServletContextFacade implements ConfigExtractor
    {
        private final ServletContext servletContext;
        
        private ServletContextFacade(final ServletContext pServletContext) {
            this.servletContext = pServletContext;
        }
        
        public Enumeration getNames() {
            return this.servletContext.getInitParameterNames();
        }
        
        public String getParameter(final String pName) {
            return this.servletContext.getInitParameter(pName);
        }
    }
    
    private interface ServletRequestHandler
    {
        JSONAware handleRequest(final HttpServletRequest p0, final HttpServletResponse p1) throws IOException;
    }
}
