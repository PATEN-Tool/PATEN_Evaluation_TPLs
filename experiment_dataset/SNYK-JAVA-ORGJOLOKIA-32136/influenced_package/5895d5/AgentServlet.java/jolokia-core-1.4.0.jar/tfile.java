// 
// Decompiled by Procyon v0.5.36
// 

package org.jolokia.http;

import javax.servlet.ServletContext;
import java.io.Writer;
import org.jolokia.util.IoUtil;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import org.json.simple.JSONStreamAware;
import java.util.Collections;
import org.jolokia.config.ConfigExtractor;
import java.util.Enumeration;
import java.util.HashMap;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import org.jolokia.discovery.AgentDetails;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import javax.security.auth.Subject;
import org.json.simple.JSONAware;
import javax.management.RuntimeMBeanException;
import java.util.Iterator;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.jolokia.util.NetworkUtil;
import java.io.IOException;
import org.jolokia.discovery.AgentDetailsHolder;
import org.jolokia.restrictor.RestrictorFactory;
import javax.servlet.ServletException;
import org.jolokia.config.Configuration;
import org.jolokia.util.ClassUtil;
import org.jolokia.config.ConfigKey;
import javax.servlet.ServletConfig;
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
    private boolean allowDnsReverseLookup;
    private boolean streamingEnabled;
    
    public AgentServlet() {
        this(null);
    }
    
    public AgentServlet(final Restrictor pRestrictor) {
        this.restrictor = pRestrictor;
    }
    
    protected LogHandler getLogHandler() {
        return this.logHandler;
    }
    
    public void init(final ServletConfig pServletConfig) throws ServletException {
        super.init(pServletConfig);
        final Configuration config = this.initConfig(pServletConfig);
        final String logHandlerClass = config.get(ConfigKey.LOGHANDLER_CLASS);
        this.logHandler = ((logHandlerClass != null) ? ClassUtil.newInstance(logHandlerClass, new Object[0]) : this.createLogHandler(pServletConfig, Boolean.valueOf(config.get(ConfigKey.DEBUG))));
        this.httpGetHandler = this.newGetHttpRequestHandler();
        this.httpPostHandler = this.newPostHttpRequestHandler();
        if (this.restrictor == null) {
            this.restrictor = this.createRestrictor(config);
        }
        else {
            this.logHandler.info("Using custom access restriction provided by " + this.restrictor);
        }
        this.configMimeType = config.get(ConfigKey.MIME_TYPE);
        this.backendManager = new BackendManager(config, this.logHandler, this.restrictor);
        this.requestHandler = new HttpRequestHandler(config, this.backendManager, this.logHandler);
        this.allowDnsReverseLookup = config.getAsBoolean(ConfigKey.ALLOW_DNS_REVERSE_LOOKUP);
        this.streamingEnabled = config.getAsBoolean(ConfigKey.STREAMING);
        this.initDiscoveryMulticast(config);
    }
    
    protected Restrictor createRestrictor(final Configuration config) {
        return RestrictorFactory.createRestrictor(config, this.logHandler);
    }
    
    private void initDiscoveryMulticast(final Configuration pConfig) {
        final String url = this.findAgentUrl(pConfig);
        if (url != null || this.listenForDiscoveryMcRequests(pConfig)) {
            this.backendManager.getAgentDetails().setUrl(url);
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
            this.requestHandler.checkAccess(this.allowDnsReverseLookup ? pReq.getRemoteHost() : null, pReq.getRemoteAddr(), this.getOriginOrReferer(pReq));
            this.updateAgentDetailsIfNeeded(pReq);
            json = this.handleSecurely(pReqHandler, pReq, pResp);
        }
        catch (Throwable exp) {
            json = (JSONAware)this.requestHandler.handleThrowable((exp instanceof RuntimeMBeanException) ? ((RuntimeMBeanException)exp).getTargetException() : exp);
        }
        finally {
            this.setCorsHeader(pReq, pResp);
            if (json == null) {
                json = (JSONAware)this.requestHandler.handleThrowable(new Exception("Internal error while handling an exception"));
            }
            this.sendResponse(pResp, pReq, json);
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
    
    private void updateAgentDetailsIfNeeded(final HttpServletRequest pReq) {
        final AgentDetails details = this.backendManager.getAgentDetails();
        if (details.isInitRequired()) {
            synchronized (details) {
                if (details.isInitRequired()) {
                    if (details.isUrlMissing()) {
                        final String url = this.getBaseUrl(NetworkUtil.sanitizeLocalUrl(pReq.getRequestURL().toString()), this.extractServletPath(pReq));
                        details.setUrl(url);
                    }
                    if (details.isSecuredMissing()) {
                        details.setSecured(pReq.getAuthType() != null);
                    }
                    details.seal();
                }
            }
        }
    }
    
    private String extractServletPath(final HttpServletRequest pReq) {
        return pReq.getRequestURI().substring(0, pReq.getContextPath().length());
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
    
    private boolean isStreamingEnabled(final HttpServletRequest pReq) {
        final String streamingFromReq = pReq.getParameter(ConfigKey.STREAMING.getKeyValue());
        if (streamingFromReq != null) {
            return Boolean.parseBoolean(streamingFromReq);
        }
        return this.streamingEnabled;
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
    
    private void sendResponse(final HttpServletResponse pResp, final HttpServletRequest pReq, final JSONAware pJson) throws IOException {
        final String callback = pReq.getParameter(ConfigKey.CALLBACK.getKeyValue());
        this.setContentType(pResp, (callback != null) ? "text/javascript" : this.getMimeType(pReq));
        pResp.setStatus(200);
        this.setNoCacheHeaders(pResp);
        if (pJson == null) {
            pResp.setContentLength(-1);
        }
        else if (this.isStreamingEnabled(pReq)) {
            this.sendStreamingResponse(pResp, callback, (JSONStreamAware)pJson);
        }
        else {
            this.sendAllJSON(pResp, callback, pJson);
        }
    }
    
    private void sendStreamingResponse(final HttpServletResponse pResp, final String pCallback, final JSONStreamAware pJson) throws IOException {
        final Writer writer = new OutputStreamWriter((OutputStream)pResp.getOutputStream(), "UTF-8");
        IoUtil.streamResponseAndClose(writer, pJson, pCallback);
    }
    
    private void sendAllJSON(final HttpServletResponse pResp, final String callback, final JSONAware pJson) throws IOException {
        OutputStream out = null;
        try {
            final String json = pJson.toJSONString();
            final String content = (callback == null) ? json : (callback + "(" + json + ");");
            final byte[] response = content.getBytes("UTF8");
            pResp.setContentLength(response.length);
            out = (OutputStream)pResp.getOutputStream();
            out.write(response);
        }
        finally {
            if (out != null) {
                out.close();
            }
        }
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
        catch (NoSuchMethodError noSuchMethodError) {}
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
