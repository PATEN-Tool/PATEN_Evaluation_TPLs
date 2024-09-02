// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.catalina.servlets;

import java.io.Reader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import org.apache.catalina.util.IOTools;
import java.io.BufferedOutputStream;
import java.util.Collection;
import java.util.Vector;
import java.util.Iterator;
import java.nio.file.Files;
import java.nio.file.CopyOption;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.StringTokenizer;
import java.io.File;
import javax.servlet.ServletContext;
import org.apache.juli.logging.LogFactory;
import java.io.InputStream;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Cookie;
import java.util.Enumeration;
import java.util.Date;
import java.util.Locale;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletException;
import java.util.ArrayList;
import java.util.Map;
import javax.servlet.ServletConfig;
import java.util.Hashtable;
import java.util.regex.Pattern;
import java.util.List;
import org.apache.tomcat.util.res.StringManager;
import org.apache.juli.logging.Log;
import javax.servlet.http.HttpServlet;

public final class CGIServlet extends HttpServlet
{
    private static final Log log;
    private static final StringManager sm;
    private static final long serialVersionUID = 1L;
    private String cgiPathPrefix;
    private String cgiExecutable;
    private List<String> cgiExecutableArgs;
    private String parameterEncoding;
    private long stderrTimeout;
    private Pattern envHttpHeadersPattern;
    private static final Object expandFileLock;
    private final Hashtable<String, String> shellEnv;
    
    public CGIServlet() {
        this.cgiPathPrefix = null;
        this.cgiExecutable = "perl";
        this.cgiExecutableArgs = null;
        this.parameterEncoding = System.getProperty("file.encoding", "UTF-8");
        this.stderrTimeout = 2000L;
        this.envHttpHeadersPattern = Pattern.compile("ACCEPT[-0-9A-Z]*|CACHE-CONTROL|COOKIE|HOST|IF-[-0-9A-Z]*|REFERER|USER-AGENT");
        this.shellEnv = new Hashtable<String, String>();
    }
    
    public void init(final ServletConfig config) throws ServletException {
        super.init(config);
        this.cgiPathPrefix = this.getServletConfig().getInitParameter("cgiPathPrefix");
        final boolean passShellEnvironment = Boolean.parseBoolean(this.getServletConfig().getInitParameter("passShellEnvironment"));
        if (passShellEnvironment) {
            this.shellEnv.putAll(System.getenv());
        }
        if (this.getServletConfig().getInitParameter("executable") != null) {
            this.cgiExecutable = this.getServletConfig().getInitParameter("executable");
        }
        if (this.getServletConfig().getInitParameter("executable-arg-1") != null) {
            final List<String> args = new ArrayList<String>();
            int i = 1;
            while (true) {
                final String arg = this.getServletConfig().getInitParameter("executable-arg-" + i);
                if (arg == null) {
                    break;
                }
                args.add(arg);
                ++i;
            }
            this.cgiExecutableArgs = args;
        }
        if (this.getServletConfig().getInitParameter("parameterEncoding") != null) {
            this.parameterEncoding = this.getServletConfig().getInitParameter("parameterEncoding");
        }
        if (this.getServletConfig().getInitParameter("stderrTimeout") != null) {
            this.stderrTimeout = Long.parseLong(this.getServletConfig().getInitParameter("stderrTimeout"));
        }
        if (this.getServletConfig().getInitParameter("envHttpHeaders") != null) {
            this.envHttpHeadersPattern = Pattern.compile(this.getServletConfig().getInitParameter("envHttpHeaders"));
        }
    }
    
    @Deprecated
    protected void printServletEnvironment(final ServletOutputStream out, final HttpServletRequest req, final HttpServletResponse res) throws IOException {
        this.printServletEnvironment(req);
    }
    
    private void printServletEnvironment(final HttpServletRequest req) throws IOException {
        CGIServlet.log.trace((Object)"ServletRequest Properties");
        Enumeration<String> attrs = (Enumeration<String>)req.getAttributeNames();
        while (attrs.hasMoreElements()) {
            final String attr = attrs.nextElement();
            CGIServlet.log.trace((Object)("Request Attribute: " + attr + ": [ " + req.getAttribute(attr) + "]"));
        }
        CGIServlet.log.trace((Object)("Character Encoding: [" + req.getCharacterEncoding() + "]"));
        CGIServlet.log.trace((Object)("Content Length: [" + req.getContentLengthLong() + "]"));
        CGIServlet.log.trace((Object)("Content Type: [" + req.getContentType() + "]"));
        final Enumeration<Locale> locales = (Enumeration<Locale>)req.getLocales();
        while (locales.hasMoreElements()) {
            final Locale locale = locales.nextElement();
            CGIServlet.log.trace((Object)("Locale: [" + locale + "]"));
        }
        Enumeration<String> params = (Enumeration<String>)req.getParameterNames();
        while (params.hasMoreElements()) {
            final String param = params.nextElement();
            for (final String value : req.getParameterValues(param)) {
                CGIServlet.log.trace((Object)("Request Parameter: " + param + ":  [" + value + "]"));
            }
        }
        CGIServlet.log.trace((Object)("Protocol: [" + req.getProtocol() + "]"));
        CGIServlet.log.trace((Object)("Remote Address: [" + req.getRemoteAddr() + "]"));
        CGIServlet.log.trace((Object)("Remote Host: [" + req.getRemoteHost() + "]"));
        CGIServlet.log.trace((Object)("Scheme: [" + req.getScheme() + "]"));
        CGIServlet.log.trace((Object)("Secure: [" + req.isSecure() + "]"));
        CGIServlet.log.trace((Object)("Server Name: [" + req.getServerName() + "]"));
        CGIServlet.log.trace((Object)("Server Port: [" + req.getServerPort() + "]"));
        CGIServlet.log.trace((Object)"HttpServletRequest Properties");
        CGIServlet.log.trace((Object)("Auth Type: [" + req.getAuthType() + "]"));
        CGIServlet.log.trace((Object)("Context Path: [" + req.getContextPath() + "]"));
        final Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (final Cookie cookie : cookies) {
                CGIServlet.log.trace((Object)("Cookie: " + cookie.getName() + ": [" + cookie.getValue() + "]"));
            }
        }
        final Enumeration<String> headers = (Enumeration<String>)req.getHeaderNames();
        while (headers.hasMoreElements()) {
            final String header = headers.nextElement();
            CGIServlet.log.trace((Object)("HTTP Header: " + header + ": [" + req.getHeader(header) + "]"));
        }
        CGIServlet.log.trace((Object)("Method: [" + req.getMethod() + "]"));
        CGIServlet.log.trace((Object)("Path Info: [" + req.getPathInfo() + "]"));
        CGIServlet.log.trace((Object)("Path Translated: [" + req.getPathTranslated() + "]"));
        CGIServlet.log.trace((Object)("Query String: [" + req.getQueryString() + "]"));
        CGIServlet.log.trace((Object)("Remote User: [" + req.getRemoteUser() + "]"));
        CGIServlet.log.trace((Object)("Requested Session ID: [" + req.getRequestedSessionId() + "]"));
        CGIServlet.log.trace((Object)("Requested Session ID From Cookie: [" + req.isRequestedSessionIdFromCookie() + "]"));
        CGIServlet.log.trace((Object)("Requested Session ID From URL: [" + req.isRequestedSessionIdFromURL() + "]"));
        CGIServlet.log.trace((Object)("Requested Session ID Valid: [" + req.isRequestedSessionIdValid() + "]"));
        CGIServlet.log.trace((Object)("Request URI: [" + req.getRequestURI() + "]"));
        CGIServlet.log.trace((Object)("Servlet Path: [" + req.getServletPath() + "]"));
        CGIServlet.log.trace((Object)("User Principal: [" + req.getUserPrincipal() + "]"));
        final HttpSession session = req.getSession(false);
        if (session != null) {
            CGIServlet.log.trace((Object)"HttpSession Properties");
            CGIServlet.log.trace((Object)("ID: [" + session.getId() + "]"));
            CGIServlet.log.trace((Object)("Creation Time: [" + new Date(session.getCreationTime()) + "]"));
            CGIServlet.log.trace((Object)("Last Accessed Time: [" + new Date(session.getLastAccessedTime()) + "]"));
            CGIServlet.log.trace((Object)("Max Inactive Interval: [" + session.getMaxInactiveInterval() + "]"));
            attrs = (Enumeration<String>)session.getAttributeNames();
            while (attrs.hasMoreElements()) {
                final String attr2 = attrs.nextElement();
                CGIServlet.log.trace((Object)("Session Attribute: " + attr2 + ": [" + session.getAttribute(attr2) + "]"));
            }
        }
        CGIServlet.log.trace((Object)"ServletConfig Properties");
        CGIServlet.log.trace((Object)("Servlet Name: [" + this.getServletConfig().getServletName() + "]"));
        params = (Enumeration<String>)this.getServletConfig().getInitParameterNames();
        while (params.hasMoreElements()) {
            final String param2 = params.nextElement();
            final String value = this.getServletConfig().getInitParameter(param2);
            CGIServlet.log.trace((Object)("Servlet Init Param: " + param2 + ": [" + value + "]"));
        }
        CGIServlet.log.trace((Object)"ServletContext Properties");
        CGIServlet.log.trace((Object)("Major Version: [" + this.getServletContext().getMajorVersion() + "]"));
        CGIServlet.log.trace((Object)("Minor Version: [" + this.getServletContext().getMinorVersion() + "]"));
        CGIServlet.log.trace((Object)("Real Path for '/': [" + this.getServletContext().getRealPath("/") + "]"));
        CGIServlet.log.trace((Object)("Server Info: [" + this.getServletContext().getServerInfo() + "]"));
        CGIServlet.log.trace((Object)"ServletContext Initialization Parameters");
        params = (Enumeration<String>)this.getServletContext().getInitParameterNames();
        while (params.hasMoreElements()) {
            final String param2 = params.nextElement();
            final String value = this.getServletContext().getInitParameter(param2);
            CGIServlet.log.trace((Object)("Servlet Context Init Param: " + param2 + ": [" + value + "]"));
        }
        CGIServlet.log.trace((Object)"ServletContext Attributes");
        attrs = (Enumeration<String>)this.getServletContext().getAttributeNames();
        while (attrs.hasMoreElements()) {
            final String attr2 = attrs.nextElement();
            CGIServlet.log.trace((Object)("Servlet Context Attribute: " + attr2 + ": [" + this.getServletContext().getAttribute(attr2) + "]"));
        }
    }
    
    protected void doPost(final HttpServletRequest req, final HttpServletResponse res) throws IOException, ServletException {
        this.doGet(req, res);
    }
    
    protected void doGet(final HttpServletRequest req, final HttpServletResponse res) throws ServletException, IOException {
        final CGIEnvironment cgiEnv = new CGIEnvironment(req, this.getServletContext());
        if (cgiEnv.isValid()) {
            final CGIRunner cgi = new CGIRunner(cgiEnv.getCommand(), cgiEnv.getEnvironment(), cgiEnv.getWorkingDirectory(), cgiEnv.getParameters());
            if ("POST".equals(req.getMethod())) {
                cgi.setInput((InputStream)req.getInputStream());
            }
            cgi.setResponse(res);
            cgi.run();
        }
        else {
            res.sendError(404);
        }
        if (CGIServlet.log.isTraceEnabled()) {
            final String[] arr$;
            final String[] cgiEnvLines = arr$ = cgiEnv.toString().split(System.lineSeparator());
            for (final String cgiEnvLine : arr$) {
                CGIServlet.log.trace((Object)cgiEnvLine);
            }
            this.printServletEnvironment(req);
        }
    }
    
    private boolean setStatus(final HttpServletResponse response, final int status) throws IOException {
        if (status >= 400) {
            response.sendError(status);
            return true;
        }
        response.setStatus(status);
        return false;
    }
    
    static {
        log = LogFactory.getLog((Class)CGIServlet.class);
        sm = StringManager.getManager((Class)CGIServlet.class);
        expandFileLock = new Object();
    }
    
    protected class CGIEnvironment
    {
        private ServletContext context;
        private String contextPath;
        private String servletPath;
        private String pathInfo;
        private String webAppRootDir;
        private File tmpDir;
        private Hashtable<String, String> env;
        private String command;
        private final File workingDirectory;
        private final ArrayList<String> cmdLineParameters;
        private final boolean valid;
        
        protected CGIEnvironment(final HttpServletRequest req, final ServletContext context) throws IOException {
            this.context = null;
            this.contextPath = null;
            this.servletPath = null;
            this.pathInfo = null;
            this.webAppRootDir = null;
            this.tmpDir = null;
            this.env = null;
            this.command = null;
            this.cmdLineParameters = new ArrayList<String>();
            this.setupFromContext(context);
            this.setupFromRequest(req);
            this.valid = this.setCGIEnvironment(req);
            if (this.valid) {
                this.workingDirectory = new File(this.command.substring(0, this.command.lastIndexOf(File.separator)));
            }
            else {
                this.workingDirectory = null;
            }
        }
        
        protected void setupFromContext(final ServletContext context) {
            this.context = context;
            this.webAppRootDir = context.getRealPath("/");
            this.tmpDir = (File)context.getAttribute("javax.servlet.context.tempdir");
        }
        
        protected void setupFromRequest(final HttpServletRequest req) throws UnsupportedEncodingException {
            boolean isIncluded = false;
            if (req.getAttribute("javax.servlet.include.request_uri") != null) {
                isIncluded = true;
            }
            if (isIncluded) {
                this.contextPath = (String)req.getAttribute("javax.servlet.include.context_path");
                this.servletPath = (String)req.getAttribute("javax.servlet.include.servlet_path");
                this.pathInfo = (String)req.getAttribute("javax.servlet.include.path_info");
            }
            else {
                this.contextPath = req.getContextPath();
                this.servletPath = req.getServletPath();
                this.pathInfo = req.getPathInfo();
            }
            if (this.pathInfo == null) {
                this.pathInfo = this.servletPath;
            }
            if (req.getMethod().equals("GET") || req.getMethod().equals("POST") || req.getMethod().equals("HEAD")) {
                String qs;
                if (isIncluded) {
                    qs = (String)req.getAttribute("javax.servlet.include.query_string");
                }
                else {
                    qs = req.getQueryString();
                }
                if (qs != null && qs.indexOf(61) == -1) {
                    final StringTokenizer qsTokens = new StringTokenizer(qs, "+");
                    while (qsTokens.hasMoreTokens()) {
                        this.cmdLineParameters.add(URLDecoder.decode(qsTokens.nextToken(), CGIServlet.this.parameterEncoding));
                    }
                }
            }
        }
        
        protected String[] findCGI(final String pathInfo, String webAppRootDir, final String contextPath, final String servletPath, final String cgiPathPrefix) {
            String path = null;
            String name = null;
            String scriptname = null;
            if (webAppRootDir != null && webAppRootDir.lastIndexOf(File.separator) == webAppRootDir.length() - 1) {
                webAppRootDir = webAppRootDir.substring(0, webAppRootDir.length() - 1);
            }
            if (cgiPathPrefix != null) {
                webAppRootDir = webAppRootDir + File.separator + cgiPathPrefix;
            }
            if (CGIServlet.log.isDebugEnabled()) {
                CGIServlet.log.debug((Object)CGIServlet.sm.getString("cgiServlet.find.path", new Object[] { pathInfo, webAppRootDir }));
            }
            File currentLocation = new File(webAppRootDir);
            final StringTokenizer dirWalker = new StringTokenizer(pathInfo, "/");
            if (CGIServlet.log.isDebugEnabled()) {
                CGIServlet.log.debug((Object)CGIServlet.sm.getString("cgiServlet.find.location", new Object[] { currentLocation.getAbsolutePath() }));
            }
            final StringBuilder cginameBuilder = new StringBuilder();
            while (!currentLocation.isFile() && dirWalker.hasMoreElements()) {
                final String nextElement = (String)dirWalker.nextElement();
                currentLocation = new File(currentLocation, nextElement);
                cginameBuilder.append('/').append(nextElement);
                if (CGIServlet.log.isDebugEnabled()) {
                    CGIServlet.log.debug((Object)CGIServlet.sm.getString("cgiServlet.find.location", new Object[] { currentLocation.getAbsolutePath() }));
                }
            }
            final String cginame = cginameBuilder.toString();
            if (!currentLocation.isFile()) {
                return new String[] { null, null, null, null };
            }
            path = currentLocation.getAbsolutePath();
            name = currentLocation.getName();
            if (".".equals(contextPath)) {
                scriptname = servletPath;
            }
            else {
                scriptname = contextPath + servletPath;
            }
            if (!servletPath.equals(cginame)) {
                scriptname += cginame;
            }
            if (CGIServlet.log.isDebugEnabled()) {
                CGIServlet.log.debug((Object)CGIServlet.sm.getString("cgiServlet.find.found", new Object[] { name, path, scriptname, cginame }));
            }
            return new String[] { path, scriptname, cginame, name };
        }
        
        protected boolean setCGIEnvironment(final HttpServletRequest req) throws IOException {
            final Hashtable<String, String> envp = new Hashtable<String, String>();
            envp.putAll(CGIServlet.this.shellEnv);
            String sPathInfoOrig = null;
            String sPathInfoCGI = null;
            String sPathTranslatedCGI = null;
            String sCGIFullPath = null;
            String sCGIScriptName = null;
            String sCGIFullName = null;
            String sCGIName = null;
            sPathInfoOrig = this.pathInfo;
            sPathInfoOrig = ((sPathInfoOrig == null) ? "" : sPathInfoOrig);
            if (this.webAppRootDir == null) {
                this.webAppRootDir = this.tmpDir.toString();
                this.expandCGIScript();
            }
            final String[] sCGINames = this.findCGI(sPathInfoOrig, this.webAppRootDir, this.contextPath, this.servletPath, CGIServlet.this.cgiPathPrefix);
            sCGIFullPath = sCGINames[0];
            sCGIScriptName = sCGINames[1];
            sCGIFullName = sCGINames[2];
            sCGIName = sCGINames[3];
            if (sCGIFullPath == null || sCGIScriptName == null || sCGIFullName == null || sCGIName == null) {
                return false;
            }
            envp.put("SERVER_SOFTWARE", "TOMCAT");
            envp.put("SERVER_NAME", this.nullsToBlanks(req.getServerName()));
            envp.put("GATEWAY_INTERFACE", "CGI/1.1");
            envp.put("SERVER_PROTOCOL", this.nullsToBlanks(req.getProtocol()));
            final int port = req.getServerPort();
            final Integer iPort = (port == 0) ? Integer.valueOf(-1) : Integer.valueOf(port);
            envp.put("SERVER_PORT", iPort.toString());
            envp.put("REQUEST_METHOD", this.nullsToBlanks(req.getMethod()));
            envp.put("REQUEST_URI", this.nullsToBlanks(req.getRequestURI()));
            if (this.pathInfo == null || this.pathInfo.substring(sCGIFullName.length()).length() <= 0) {
                sPathInfoCGI = "";
            }
            else {
                sPathInfoCGI = this.pathInfo.substring(sCGIFullName.length());
            }
            envp.put("PATH_INFO", sPathInfoCGI);
            if (!"".equals(sPathInfoCGI)) {
                sPathTranslatedCGI = this.context.getRealPath(sPathInfoCGI);
            }
            if (sPathTranslatedCGI != null) {
                if (!"".equals(sPathTranslatedCGI)) {
                    envp.put("PATH_TRANSLATED", this.nullsToBlanks(sPathTranslatedCGI));
                }
            }
            envp.put("SCRIPT_NAME", this.nullsToBlanks(sCGIScriptName));
            envp.put("QUERY_STRING", this.nullsToBlanks(req.getQueryString()));
            envp.put("REMOTE_HOST", this.nullsToBlanks(req.getRemoteHost()));
            envp.put("REMOTE_ADDR", this.nullsToBlanks(req.getRemoteAddr()));
            envp.put("AUTH_TYPE", this.nullsToBlanks(req.getAuthType()));
            envp.put("REMOTE_USER", this.nullsToBlanks(req.getRemoteUser()));
            envp.put("REMOTE_IDENT", "");
            envp.put("CONTENT_TYPE", this.nullsToBlanks(req.getContentType()));
            final long contentLength = req.getContentLengthLong();
            final String sContentLength = (contentLength <= 0L) ? "" : Long.toString(contentLength);
            envp.put("CONTENT_LENGTH", sContentLength);
            final Enumeration<String> headers = (Enumeration<String>)req.getHeaderNames();
            String header = null;
            while (headers.hasMoreElements()) {
                header = null;
                header = headers.nextElement().toUpperCase(Locale.ENGLISH);
                if (CGIServlet.this.envHttpHeadersPattern.matcher(header).matches()) {
                    envp.put("HTTP_" + header.replace('-', '_'), req.getHeader(header));
                }
            }
            final File fCGIFullPath = new File(sCGIFullPath);
            envp.put("X_TOMCAT_SCRIPT_PATH", this.command = fCGIFullPath.getCanonicalPath());
            envp.put("SCRIPT_FILENAME", this.command);
            this.env = envp;
            return true;
        }
        
        protected void expandCGIScript() {
            final StringBuilder srcPath = new StringBuilder();
            final StringBuilder destPath = new StringBuilder();
            InputStream is = null;
            if (CGIServlet.this.cgiPathPrefix == null) {
                srcPath.append(this.pathInfo);
                is = this.context.getResourceAsStream(srcPath.toString());
                destPath.append(this.tmpDir);
                destPath.append(this.pathInfo);
            }
            else {
                srcPath.append(CGIServlet.this.cgiPathPrefix);
                for (StringTokenizer pathWalker = new StringTokenizer(this.pathInfo, "/"); pathWalker.hasMoreElements() && is == null; is = this.context.getResourceAsStream(srcPath.toString())) {
                    srcPath.append("/");
                    srcPath.append(pathWalker.nextElement());
                }
                destPath.append(this.tmpDir);
                destPath.append("/");
                destPath.append((CharSequence)srcPath);
            }
            if (is == null) {
                CGIServlet.log.warn((Object)CGIServlet.sm.getString("cgiServlet.expandNotFound", new Object[] { srcPath }));
                return;
            }
            final File f = new File(destPath.toString());
            if (f.exists()) {
                try {
                    is.close();
                }
                catch (IOException e) {
                    CGIServlet.log.warn((Object)CGIServlet.sm.getString("cgiServlet.expandCloseFail", new Object[] { srcPath }), (Throwable)e);
                }
                return;
            }
            final File dir = f.getParentFile();
            if (!dir.mkdirs() && !dir.isDirectory()) {
                CGIServlet.log.warn((Object)CGIServlet.sm.getString("cgiServlet.expandCreateDirFail", new Object[] { dir.getAbsolutePath() }));
                return;
            }
            try {
                synchronized (CGIServlet.expandFileLock) {
                    if (f.exists()) {
                        return;
                    }
                    if (!f.createNewFile()) {
                        return;
                    }
                    try {
                        Files.copy(is, f.toPath(), new CopyOption[0]);
                    }
                    finally {
                        is.close();
                    }
                    if (CGIServlet.log.isDebugEnabled()) {
                        CGIServlet.log.debug((Object)CGIServlet.sm.getString("cgiServlet.expandOk", new Object[] { srcPath, destPath }));
                    }
                }
            }
            catch (IOException ioe) {
                CGIServlet.log.warn((Object)CGIServlet.sm.getString("cgiServlet.expandFail", new Object[] { srcPath, destPath }), (Throwable)ioe);
                if (f.exists() && !f.delete()) {
                    CGIServlet.log.warn((Object)CGIServlet.sm.getString("cgiServlet.expandDeleteFail", new Object[] { f.getAbsolutePath() }));
                }
            }
        }
        
        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("CGIEnvironment Info:");
            sb.append(System.lineSeparator());
            if (this.isValid()) {
                sb.append("Validity: [true]");
                sb.append(System.lineSeparator());
                sb.append("Environment values:");
                sb.append(System.lineSeparator());
                for (final Map.Entry<String, String> entry : this.env.entrySet()) {
                    sb.append("  ");
                    sb.append(entry.getKey());
                    sb.append(": [");
                    sb.append(this.blanksToString(entry.getValue(), "will be set to blank"));
                    sb.append("]");
                    sb.append(System.lineSeparator());
                }
                sb.append("Derived Command :[");
                sb.append(this.nullsToBlanks(this.command));
                sb.append("]");
                sb.append(System.lineSeparator());
                sb.append("Working Directory: [");
                if (this.workingDirectory != null) {
                    sb.append(this.workingDirectory.toString());
                }
                sb.append("]");
                sb.append(System.lineSeparator());
                sb.append("Command Line Params:");
                sb.append(System.lineSeparator());
                for (final String param : this.cmdLineParameters) {
                    sb.append("  [");
                    sb.append(param);
                    sb.append("]");
                    sb.append(System.lineSeparator());
                }
            }
            else {
                sb.append("Validity: [false]");
                sb.append(System.lineSeparator());
                sb.append("CGI script not found or not specified.");
                sb.append(System.lineSeparator());
                sb.append("Check the HttpServletRequest pathInfo property to see if it is what ");
                sb.append(System.lineSeparator());
                sb.append("you meant it to be. You must specify an existant and executable file ");
                sb.append(System.lineSeparator());
                sb.append("as part of the path-info.");
                sb.append(System.lineSeparator());
            }
            return sb.toString();
        }
        
        protected String getCommand() {
            return this.command;
        }
        
        protected File getWorkingDirectory() {
            return this.workingDirectory;
        }
        
        protected Hashtable<String, String> getEnvironment() {
            return this.env;
        }
        
        protected ArrayList<String> getParameters() {
            return this.cmdLineParameters;
        }
        
        protected boolean isValid() {
            return this.valid;
        }
        
        protected String nullsToBlanks(final String s) {
            return this.nullsToString(s, "");
        }
        
        protected String nullsToString(final String couldBeNull, final String subForNulls) {
            return (couldBeNull == null) ? subForNulls : couldBeNull;
        }
        
        protected String blanksToString(final String couldBeBlank, final String subForBlanks) {
            return ("".equals(couldBeBlank) || couldBeBlank == null) ? subForBlanks : couldBeBlank;
        }
    }
    
    protected class CGIRunner
    {
        private final String command;
        private final Hashtable<String, String> env;
        private final File wd;
        private final ArrayList<String> params;
        private InputStream stdin;
        private HttpServletResponse response;
        private boolean readyToRun;
        
        protected CGIRunner(final String command, final Hashtable<String, String> env, final File wd, final ArrayList<String> params) {
            this.stdin = null;
            this.response = null;
            this.readyToRun = false;
            this.command = command;
            this.env = env;
            this.wd = wd;
            this.params = params;
            this.updateReadyStatus();
        }
        
        protected void updateReadyStatus() {
            if (this.command != null && this.env != null && this.wd != null && this.params != null && this.response != null) {
                this.readyToRun = true;
            }
            else {
                this.readyToRun = false;
            }
        }
        
        protected boolean isReady() {
            return this.readyToRun;
        }
        
        protected void setResponse(final HttpServletResponse response) {
            this.response = response;
            this.updateReadyStatus();
        }
        
        protected void setInput(final InputStream stdin) {
            this.stdin = stdin;
            this.updateReadyStatus();
        }
        
        protected String[] hashToStringArray(final Hashtable<String, ?> h) throws NullPointerException {
            final Vector<String> v = new Vector<String>();
            final Enumeration<String> e = h.keys();
            while (e.hasMoreElements()) {
                final String k = e.nextElement();
                v.add(k + "=" + h.get(k).toString());
            }
            final String[] strArr = new String[v.size()];
            v.copyInto(strArr);
            return strArr;
        }
        
        protected void run() throws IOException {
            if (!this.isReady()) {
                throw new IOException(this.getClass().getName() + ": not ready to run.");
            }
            if (CGIServlet.log.isDebugEnabled()) {
                CGIServlet.log.debug((Object)("envp: [" + this.env + "], command: [" + this.command + "]"));
            }
            if (this.command.indexOf(File.separator + "." + File.separator) >= 0 || this.command.indexOf(File.separator + "..") >= 0 || this.command.indexOf(".." + File.separator) >= 0) {
                throw new IOException(this.getClass().getName() + "Illegal Character in CGI command " + "path ('.' or '..') detected.  Not " + "running CGI [" + this.command + "].");
            }
            Runtime rt = null;
            BufferedReader cgiHeaderReader = null;
            InputStream cgiOutput = null;
            BufferedReader commandsStdErr = null;
            Thread errReaderThread = null;
            BufferedOutputStream commandsStdIn = null;
            Process proc = null;
            int bufRead = -1;
            final List<String> cmdAndArgs = new ArrayList<String>();
            if (CGIServlet.this.cgiExecutable.length() != 0) {
                cmdAndArgs.add(CGIServlet.this.cgiExecutable);
            }
            if (CGIServlet.this.cgiExecutableArgs != null) {
                cmdAndArgs.addAll(CGIServlet.this.cgiExecutableArgs);
            }
            cmdAndArgs.add(this.command);
            cmdAndArgs.addAll(this.params);
            try {
                rt = Runtime.getRuntime();
                proc = rt.exec(cmdAndArgs.toArray(new String[cmdAndArgs.size()]), this.hashToStringArray(this.env), this.wd);
                final String sContentLength = this.env.get("CONTENT_LENGTH");
                if (!"".equals(sContentLength)) {
                    commandsStdIn = new BufferedOutputStream(proc.getOutputStream());
                    IOTools.flow(this.stdin, commandsStdIn);
                    commandsStdIn.flush();
                    commandsStdIn.close();
                }
                boolean isRunning = true;
                final BufferedReader stdErrRdr;
                commandsStdErr = (stdErrRdr = new BufferedReader(new InputStreamReader(proc.getErrorStream())));
                errReaderThread = new Thread() {
                    @Override
                    public void run() {
                        CGIRunner.this.sendToLog(stdErrRdr);
                    }
                };
                errReaderThread.start();
                final InputStream cgiHeaderStream = new HTTPHeaderInputStream(proc.getInputStream());
                cgiHeaderReader = new BufferedReader(new InputStreamReader(cgiHeaderStream));
                boolean skipBody = false;
                while (isRunning) {
                    try {
                        String line = null;
                        while ((line = cgiHeaderReader.readLine()) != null && !"".equals(line)) {
                            if (CGIServlet.log.isTraceEnabled()) {
                                CGIServlet.log.trace((Object)("addHeader(\"" + line + "\")"));
                            }
                            if (line.startsWith("HTTP")) {
                                skipBody = CGIServlet.this.setStatus(this.response, this.getSCFromHttpStatusLine(line));
                            }
                            else if (line.indexOf(58) >= 0) {
                                final String header = line.substring(0, line.indexOf(58)).trim();
                                final String value = line.substring(line.indexOf(58) + 1).trim();
                                if (header.equalsIgnoreCase("status")) {
                                    skipBody = CGIServlet.this.setStatus(this.response, this.getSCFromCGIStatusHeader(value));
                                }
                                else {
                                    this.response.addHeader(header, value);
                                }
                            }
                            else {
                                CGIServlet.log.info((Object)CGIServlet.sm.getString("cgiServlet.runBadHeader", new Object[] { line }));
                            }
                        }
                        final byte[] bBuf = new byte[2048];
                        final OutputStream out = (OutputStream)this.response.getOutputStream();
                        cgiOutput = proc.getInputStream();
                        try {
                            while (!skipBody && (bufRead = cgiOutput.read(bBuf)) != -1) {
                                if (CGIServlet.log.isTraceEnabled()) {
                                    CGIServlet.log.trace((Object)("output " + bufRead + " bytes of data"));
                                }
                                out.write(bBuf, 0, bufRead);
                            }
                        }
                        finally {
                            if (bufRead != -1) {
                                while ((bufRead = cgiOutput.read(bBuf)) != -1) {}
                            }
                        }
                        proc.exitValue();
                        isRunning = false;
                    }
                    catch (IllegalThreadStateException e2) {
                        try {
                            Thread.sleep(500L);
                        }
                        catch (InterruptedException ex) {}
                    }
                }
            }
            catch (IOException e) {
                CGIServlet.log.warn((Object)CGIServlet.sm.getString("cgiServlet.runFail"), (Throwable)e);
                throw e;
            }
            finally {
                if (cgiHeaderReader != null) {
                    try {
                        cgiHeaderReader.close();
                    }
                    catch (IOException ioe) {
                        CGIServlet.log.warn((Object)CGIServlet.sm.getString("cgiServlet.runHeaderReaderFail"), (Throwable)ioe);
                    }
                }
                if (cgiOutput != null) {
                    try {
                        cgiOutput.close();
                    }
                    catch (IOException ioe) {
                        CGIServlet.log.warn((Object)CGIServlet.sm.getString("cgiServlet.runOutputStreamFail"), (Throwable)ioe);
                    }
                }
                if (errReaderThread != null) {
                    try {
                        errReaderThread.join(CGIServlet.this.stderrTimeout);
                    }
                    catch (InterruptedException e3) {
                        CGIServlet.log.warn((Object)CGIServlet.sm.getString("cgiServlet.runReaderInterupt"));
                    }
                }
                if (proc != null) {
                    proc.destroy();
                    proc = null;
                }
            }
        }
        
        private int getSCFromHttpStatusLine(final String line) {
            final int statusStart = line.indexOf(32) + 1;
            if (statusStart < 1 || line.length() < statusStart + 3) {
                CGIServlet.log.warn((Object)CGIServlet.sm.getString("cgiServlet.runInvalidStatus", new Object[] { line }));
                return 500;
            }
            final String status = line.substring(statusStart, statusStart + 3);
            int statusCode;
            try {
                statusCode = Integer.parseInt(status);
            }
            catch (NumberFormatException nfe) {
                CGIServlet.log.warn((Object)CGIServlet.sm.getString("cgiServlet.runInvalidStatus", new Object[] { status }));
                return 500;
            }
            return statusCode;
        }
        
        private int getSCFromCGIStatusHeader(final String value) {
            if (value.length() < 3) {
                CGIServlet.log.warn((Object)CGIServlet.sm.getString("cgiServlet.runInvalidStatus", new Object[] { value }));
                return 500;
            }
            final String status = value.substring(0, 3);
            int statusCode;
            try {
                statusCode = Integer.parseInt(status);
            }
            catch (NumberFormatException nfe) {
                CGIServlet.log.warn((Object)CGIServlet.sm.getString("cgiServlet.runInvalidStatus", new Object[] { status }));
                return 500;
            }
            return statusCode;
        }
        
        private void sendToLog(final BufferedReader rdr) {
            String line = null;
            int lineCount = 0;
            try {
                while ((line = rdr.readLine()) != null) {
                    CGIServlet.log.warn((Object)CGIServlet.sm.getString("cgiServlet.runStdErr", new Object[] { line }));
                    ++lineCount;
                }
            }
            catch (IOException e) {
                CGIServlet.log.warn((Object)CGIServlet.sm.getString("cgiServlet.runStdErrFail"), (Throwable)e);
                try {
                    rdr.close();
                }
                catch (IOException e) {
                    CGIServlet.log.warn((Object)CGIServlet.sm.getString("cgiServlet.runStdErrFail"), (Throwable)e);
                }
            }
            finally {
                try {
                    rdr.close();
                }
                catch (IOException e2) {
                    CGIServlet.log.warn((Object)CGIServlet.sm.getString("cgiServlet.runStdErrFail"), (Throwable)e2);
                }
            }
            if (lineCount > 0) {
                CGIServlet.log.warn((Object)CGIServlet.sm.getString("cgiServlet.runStdErrCount", new Object[] { lineCount }));
            }
        }
    }
    
    protected static class HTTPHeaderInputStream extends InputStream
    {
        private static final int STATE_CHARACTER = 0;
        private static final int STATE_FIRST_CR = 1;
        private static final int STATE_FIRST_LF = 2;
        private static final int STATE_SECOND_CR = 3;
        private static final int STATE_HEADER_END = 4;
        private final InputStream input;
        private int state;
        
        HTTPHeaderInputStream(final InputStream theInput) {
            this.input = theInput;
            this.state = 0;
        }
        
        @Override
        public int read() throws IOException {
            if (this.state == 4) {
                return -1;
            }
            final int i = this.input.read();
            if (i == 10) {
                switch (this.state) {
                    case 0: {
                        this.state = 2;
                        break;
                    }
                    case 1: {
                        this.state = 2;
                        break;
                    }
                    case 2:
                    case 3: {
                        this.state = 4;
                        break;
                    }
                }
            }
            else if (i == 13) {
                switch (this.state) {
                    case 0: {
                        this.state = 1;
                        break;
                    }
                    case 1: {
                        this.state = 4;
                        break;
                    }
                    case 2: {
                        this.state = 3;
                        break;
                    }
                }
            }
            else {
                this.state = 0;
            }
            return i;
        }
    }
}
