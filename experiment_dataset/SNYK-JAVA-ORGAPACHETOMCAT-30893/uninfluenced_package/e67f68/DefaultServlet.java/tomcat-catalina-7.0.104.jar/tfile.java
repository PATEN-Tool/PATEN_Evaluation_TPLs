// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.catalina.servlets;

import org.xml.sax.InputSource;
import org.xml.sax.ext.EntityResolver2;
import java.util.Iterator;
import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilder;
import org.xml.sax.SAXException;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Node;
import javax.xml.transform.dom.DOMSource;
import org.xml.sax.EntityResolver;
import java.util.Locale;
import org.apache.catalina.util.IOTools;
import java.io.InputStreamReader;
import java.io.StringWriter;
import javax.naming.directory.DirContext;
import org.apache.catalina.util.ServerInfo;
import javax.xml.transform.Transformer;
import javax.naming.NamingEnumeration;
import javax.xml.transform.TransformerException;
import java.io.ByteArrayInputStream;
import javax.xml.transform.Result;
import java.io.Writer;
import javax.xml.transform.stream.StreamResult;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.ByteArrayOutputStream;
import java.io.Reader;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import javax.xml.transform.TransformerFactory;
import org.apache.tomcat.util.security.PrivilegedSetTccl;
import java.security.PrivilegedAction;
import java.security.AccessController;
import org.apache.tomcat.util.security.PrivilegedGetTccl;
import org.apache.catalina.Globals;
import org.apache.catalina.util.RequestUtil;
import javax.naming.NameClassPair;
import javax.xml.transform.Source;
import java.util.StringTokenizer;
import javax.servlet.ServletResponse;
import java.io.PrintWriter;
import javax.servlet.ServletOutputStream;
import org.apache.naming.resources.CacheEntry;
import java.io.FileNotFoundException;
import org.apache.catalina.connector.ResponseFacade;
import javax.servlet.ServletResponseWrapper;
import org.apache.naming.resources.ResourceAttributes;
import java.io.BufferedInputStream;
import java.io.RandomAccessFile;
import java.io.File;
import java.io.InputStream;
import org.apache.naming.resources.Resource;
import java.io.FileInputStream;
import org.apache.catalina.connector.RequestFacade;
import java.io.IOException;
import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.UnavailableException;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.naming.InitialContext;
import org.apache.naming.resources.ProxyDirContext;
import org.apache.catalina.util.URLEncoder;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.tomcat.util.res.StringManager;
import javax.servlet.http.HttpServlet;

public class DefaultServlet extends HttpServlet
{
    private static final long serialVersionUID = 1L;
    protected static final StringManager sm;
    private static final DocumentBuilderFactory factory;
    private static final SecureEntityResolver secureEntityResolver;
    protected static final ArrayList<Range> FULL;
    protected static final String mimeSeparation = "CATALINA_MIME_BOUNDARY";
    protected static final String RESOURCES_JNDI_NAME = "java:/comp/Resources";
    protected static final int BUFFER_SIZE = 4096;
    protected static final URLEncoder urlEncoder;
    protected int debug;
    protected int input;
    protected boolean listings;
    protected boolean readOnly;
    protected int output;
    protected String localXsltFile;
    protected String contextXsltFile;
    protected String globalXsltFile;
    protected String readmeFile;
    protected transient ProxyDirContext resources;
    protected String fileEncoding;
    protected int sendfileSize;
    protected boolean useAcceptRanges;
    protected boolean showServerInfo;
    
    public DefaultServlet() {
        this.debug = 0;
        this.input = 2048;
        this.listings = false;
        this.readOnly = true;
        this.output = 2048;
        this.localXsltFile = null;
        this.contextXsltFile = null;
        this.globalXsltFile = null;
        this.readmeFile = null;
        this.resources = null;
        this.fileEncoding = null;
        this.sendfileSize = 49152;
        this.useAcceptRanges = true;
        this.showServerInfo = true;
    }
    
    public void destroy() {
    }
    
    public void init() throws ServletException {
        if (this.getServletConfig().getInitParameter("debug") != null) {
            this.debug = Integer.parseInt(this.getServletConfig().getInitParameter("debug"));
        }
        if (this.getServletConfig().getInitParameter("input") != null) {
            this.input = Integer.parseInt(this.getServletConfig().getInitParameter("input"));
        }
        if (this.getServletConfig().getInitParameter("output") != null) {
            this.output = Integer.parseInt(this.getServletConfig().getInitParameter("output"));
        }
        this.listings = Boolean.parseBoolean(this.getServletConfig().getInitParameter("listings"));
        if (this.getServletConfig().getInitParameter("readonly") != null) {
            this.readOnly = Boolean.parseBoolean(this.getServletConfig().getInitParameter("readonly"));
        }
        if (this.getServletConfig().getInitParameter("sendfileSize") != null) {
            this.sendfileSize = Integer.parseInt(this.getServletConfig().getInitParameter("sendfileSize")) * 1024;
        }
        this.fileEncoding = this.getServletConfig().getInitParameter("fileEncoding");
        this.globalXsltFile = this.getServletConfig().getInitParameter("globalXsltFile");
        this.contextXsltFile = this.getServletConfig().getInitParameter("contextXsltFile");
        this.localXsltFile = this.getServletConfig().getInitParameter("localXsltFile");
        this.readmeFile = this.getServletConfig().getInitParameter("readmeFile");
        if (this.getServletConfig().getInitParameter("useAcceptRanges") != null) {
            this.useAcceptRanges = Boolean.parseBoolean(this.getServletConfig().getInitParameter("useAcceptRanges"));
        }
        if (this.input < 256) {
            this.input = 256;
        }
        if (this.output < 256) {
            this.output = 256;
        }
        if (this.debug > 0) {
            this.log("DefaultServlet.init:  input buffer size=" + this.input + ", output buffer size=" + this.output);
        }
        this.resources = (ProxyDirContext)this.getServletContext().getAttribute("org.apache.catalina.resources");
        if (this.resources == null) {
            try {
                this.resources = (ProxyDirContext)new InitialContext().lookup("java:/comp/Resources");
            }
            catch (NamingException e) {
                throw new ServletException("No resources", (Throwable)e);
            }
        }
        if (this.resources == null) {
            throw new UnavailableException(DefaultServlet.sm.getString("defaultServlet.noResources"));
        }
        if (this.getServletConfig().getInitParameter("showServerInfo") != null) {
            this.showServerInfo = Boolean.parseBoolean(this.getServletConfig().getInitParameter("showServerInfo"));
        }
    }
    
    protected String getRelativePath(final HttpServletRequest request) {
        return this.getRelativePath(request, false);
    }
    
    protected String getRelativePath(final HttpServletRequest request, final boolean allowEmptyPath) {
        String pathInfo;
        String servletPath;
        if (request.getAttribute("javax.servlet.include.request_uri") != null) {
            pathInfo = (String)request.getAttribute("javax.servlet.include.path_info");
            servletPath = (String)request.getAttribute("javax.servlet.include.servlet_path");
        }
        else {
            pathInfo = request.getPathInfo();
            servletPath = request.getServletPath();
        }
        final StringBuilder result = new StringBuilder();
        if (servletPath.length() > 0) {
            result.append(servletPath);
        }
        if (pathInfo != null) {
            result.append(pathInfo);
        }
        if (result.length() == 0 && !allowEmptyPath) {
            result.append('/');
        }
        return result.toString();
    }
    
    protected String getPathPrefix(final HttpServletRequest request) {
        return request.getContextPath();
    }
    
    protected void service(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        if (req.getDispatcherType() == DispatcherType.ERROR) {
            this.doGet(req, resp);
        }
        else {
            super.service(req, resp);
        }
    }
    
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {
        this.serveResource(request, response, true);
    }
    
    protected void doHead(final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {
        final boolean serveContent = DispatcherType.INCLUDE.equals((Object)request.getDispatcherType());
        this.serveResource(request, response, serveContent);
    }
    
    protected void doOptions(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Allow", this.determineMethodsAllowed(req));
    }
    
    protected String determineMethodsAllowed(final HttpServletRequest req) {
        final StringBuilder allow = new StringBuilder();
        allow.append("OPTIONS, GET, HEAD, POST");
        if (!this.readOnly) {
            allow.append(", PUT, DELETE");
        }
        if (req instanceof RequestFacade && ((RequestFacade)req).getAllowTrace()) {
            allow.append(", TRACE");
        }
        return allow.toString();
    }
    
    protected void sendNotAllowed(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        resp.addHeader("Allow", this.determineMethodsAllowed(req));
        resp.sendError(405);
    }
    
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {
        this.doGet(request, response);
    }
    
    protected void doPut(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        if (this.readOnly) {
            this.sendNotAllowed(req, resp);
            return;
        }
        final String path = this.getRelativePath(req);
        boolean exists = true;
        try {
            this.resources.lookup(path);
        }
        catch (NamingException e) {
            exists = false;
        }
        boolean result = true;
        final Range range = this.parseContentRange(req, resp);
        InputStream resourceInputStream = null;
        try {
            if (range != null) {
                final File contentFile = this.executePartialPut(req, range, path);
                resourceInputStream = new FileInputStream(contentFile);
            }
            else {
                resourceInputStream = (InputStream)req.getInputStream();
            }
            final Resource newResource = new Resource(resourceInputStream);
            if (exists) {
                this.resources.rebind(path, newResource);
            }
            else {
                this.resources.bind(path, newResource);
            }
        }
        catch (NamingException e2) {
            result = false;
        }
        if (result) {
            if (exists) {
                resp.setStatus(204);
            }
            else {
                resp.setStatus(201);
            }
        }
        else {
            resp.sendError(409);
        }
    }
    
    protected File executePartialPut(final HttpServletRequest req, final Range range, final String path) throws IOException {
        final File tempDir = (File)this.getServletContext().getAttribute("javax.servlet.context.tempdir");
        final String convertedResourcePath = path.replace('/', '.');
        final File contentFile = new File(tempDir, convertedResourcePath);
        if (contentFile.createNewFile()) {
            contentFile.deleteOnExit();
        }
        RandomAccessFile randAccessContentFile = null;
        try {
            randAccessContentFile = new RandomAccessFile(contentFile, "rw");
            Resource oldResource = null;
            try {
                final Object obj = this.resources.lookup(path);
                if (obj instanceof Resource) {
                    oldResource = (Resource)obj;
                }
            }
            catch (NamingException ex) {}
            if (oldResource != null) {
                BufferedInputStream bufOldRevStream = null;
                try {
                    bufOldRevStream = new BufferedInputStream(oldResource.streamContent(), 4096);
                    final byte[] copyBuffer = new byte[4096];
                    int numBytesRead;
                    while ((numBytesRead = bufOldRevStream.read(copyBuffer)) != -1) {
                        randAccessContentFile.write(copyBuffer, 0, numBytesRead);
                    }
                }
                finally {
                    if (bufOldRevStream != null) {
                        try {
                            bufOldRevStream.close();
                        }
                        catch (IOException ex2) {}
                    }
                }
            }
            randAccessContentFile.setLength(range.length);
            randAccessContentFile.seek(range.start);
            final byte[] transferBuffer = new byte[4096];
            BufferedInputStream requestBufInStream = null;
            try {
                requestBufInStream = new BufferedInputStream((InputStream)req.getInputStream(), 4096);
                int numBytesRead2;
                while ((numBytesRead2 = requestBufInStream.read(transferBuffer)) != -1) {
                    randAccessContentFile.write(transferBuffer, 0, numBytesRead2);
                }
            }
            finally {
                if (requestBufInStream != null) {
                    try {
                        requestBufInStream.close();
                    }
                    catch (IOException ex3) {}
                }
            }
        }
        finally {
            if (randAccessContentFile != null) {
                try {
                    randAccessContentFile.close();
                }
                catch (IOException ex4) {}
            }
        }
        return contentFile;
    }
    
    protected void doDelete(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        if (this.readOnly) {
            this.sendNotAllowed(req, resp);
            return;
        }
        final String path = this.getRelativePath(req);
        boolean exists = true;
        try {
            this.resources.lookup(path);
        }
        catch (NamingException e) {
            exists = false;
        }
        if (exists) {
            boolean result = true;
            try {
                this.resources.unbind(path);
            }
            catch (NamingException e2) {
                result = false;
            }
            if (result) {
                resp.setStatus(204);
            }
            else {
                resp.sendError(405);
            }
        }
        else {
            resp.sendError(404);
        }
    }
    
    protected boolean checkIfHeaders(final HttpServletRequest request, final HttpServletResponse response, final ResourceAttributes resourceAttributes) throws IOException {
        return this.checkIfMatch(request, response, resourceAttributes) && this.checkIfModifiedSince(request, response, resourceAttributes) && this.checkIfNoneMatch(request, response, resourceAttributes) && this.checkIfUnmodifiedSince(request, response, resourceAttributes);
    }
    
    protected String rewriteUrl(final String path) {
        return DefaultServlet.urlEncoder.encode(path, "UTF-8");
    }
    
    @Deprecated
    protected void displaySize(final StringBuilder buf, final int filesize) {
        final int leftside = filesize / 1024;
        int rightside = filesize % 1024 / 103;
        if (leftside == 0 && rightside == 0 && filesize != 0) {
            rightside = 1;
        }
        buf.append(leftside).append(".").append(rightside);
        buf.append(" KB");
    }
    
    protected void serveResource(final HttpServletRequest request, final HttpServletResponse response, final boolean content) throws IOException, ServletException {
        boolean serveContent = content;
        final String path = this.getRelativePath(request, true);
        if (this.debug > 0) {
            if (serveContent) {
                this.log("DefaultServlet.serveResource:  Serving resource '" + path + "' headers and data");
            }
            else {
                this.log("DefaultServlet.serveResource:  Serving resource '" + path + "' headers only");
            }
        }
        if (path.length() == 0) {
            this.doDirectoryRedirect(request, response);
            return;
        }
        final CacheEntry cacheEntry = this.resources.lookupCache(path);
        final boolean isError = DispatcherType.ERROR == request.getDispatcherType();
        if (cacheEntry.exists) {
            if (cacheEntry.context == null) {
                final boolean included = request.getAttribute("javax.servlet.include.context_path") != null;
                if (!included && !isError && !this.checkIfHeaders(request, response, cacheEntry.attributes)) {
                    return;
                }
            }
            String contentType = cacheEntry.attributes.getMimeType();
            if (contentType == null) {
                contentType = this.getServletContext().getMimeType(cacheEntry.name);
                cacheEntry.attributes.setMimeType(contentType);
            }
            ArrayList<Range> ranges = null;
            long contentLength = -1L;
            if (cacheEntry.context != null) {
                if (!path.endsWith("/")) {
                    this.doDirectoryRedirect(request, response);
                    return;
                }
                if (!this.listings) {
                    response.sendError(404, DefaultServlet.sm.getString("defaultServlet.missingResource", new Object[] { request.getRequestURI() }));
                    return;
                }
                contentType = "text/html;charset=UTF-8";
            }
            else {
                if (!isError) {
                    if (this.useAcceptRanges) {
                        response.setHeader("Accept-Ranges", "bytes");
                    }
                    ranges = this.parseRange(request, response, cacheEntry.attributes);
                    response.setHeader("ETag", cacheEntry.attributes.getETag());
                    response.setHeader("Last-Modified", cacheEntry.attributes.getLastModifiedHttp());
                }
                contentLength = cacheEntry.attributes.getContentLength();
                if (contentLength == 0L) {
                    serveContent = false;
                }
            }
            ServletOutputStream ostream = null;
            PrintWriter writer = null;
            if (serveContent) {
                try {
                    ostream = response.getOutputStream();
                }
                catch (IllegalStateException e) {
                    if (contentType != null && !contentType.startsWith("text") && !contentType.endsWith("xml") && !contentType.contains("/javascript")) {
                        throw e;
                    }
                    writer = response.getWriter();
                    ranges = DefaultServlet.FULL;
                }
            }
            ServletResponse r = (ServletResponse)response;
            long contentWritten = 0L;
            while (r instanceof ServletResponseWrapper) {
                r = ((ServletResponseWrapper)r).getResponse();
            }
            if (r instanceof ResponseFacade) {
                contentWritten = ((ResponseFacade)r).getContentWritten();
            }
            if (contentWritten > 0L) {
                ranges = DefaultServlet.FULL;
            }
            if (cacheEntry.context != null || isError || ((ranges == null || ranges.isEmpty()) && request.getHeader("Range") == null) || ranges == DefaultServlet.FULL) {
                if (contentType != null) {
                    if (this.debug > 0) {
                        this.log("DefaultServlet.serveFile:  contentType='" + contentType + "'");
                    }
                    response.setContentType(contentType);
                }
                if (cacheEntry.resource != null && contentLength >= 0L && (!serveContent || ostream != null)) {
                    if (this.debug > 0) {
                        this.log("DefaultServlet.serveFile:  contentLength=" + contentLength);
                    }
                    if (contentWritten == 0L) {
                        if (contentLength < 2147483647L) {
                            response.setContentLength((int)contentLength);
                        }
                        else {
                            response.setHeader("content-length", "" + contentLength);
                        }
                    }
                }
                InputStream renderResult = null;
                if (cacheEntry.context != null && serveContent) {
                    renderResult = this.render(this.getPathPrefix(request), cacheEntry);
                }
                if (serveContent) {
                    try {
                        response.setBufferSize(this.output);
                    }
                    catch (IllegalStateException ex) {}
                    if (ostream != null) {
                        if (!this.checkSendfile(request, response, cacheEntry, contentLength, null)) {
                            this.copy(cacheEntry, renderResult, ostream);
                        }
                    }
                    else {
                        this.copy(cacheEntry, renderResult, writer);
                    }
                }
            }
            else {
                if (ranges == null || ranges.isEmpty()) {
                    return;
                }
                response.setStatus(206);
                if (ranges.size() == 1) {
                    final Range range = ranges.get(0);
                    response.addHeader("Content-Range", "bytes " + range.start + "-" + range.end + "/" + range.length);
                    final long length = range.end - range.start + 1L;
                    if (length < 2147483647L) {
                        response.setContentLength((int)length);
                    }
                    else {
                        response.setHeader("content-length", "" + length);
                    }
                    if (contentType != null) {
                        if (this.debug > 0) {
                            this.log("DefaultServlet.serveFile:  contentType='" + contentType + "'");
                        }
                        response.setContentType(contentType);
                    }
                    if (serveContent) {
                        try {
                            response.setBufferSize(this.output);
                        }
                        catch (IllegalStateException ex2) {}
                        if (ostream == null) {
                            throw new IllegalStateException();
                        }
                        if (!this.checkSendfile(request, response, cacheEntry, range.end - range.start + 1L, range)) {
                            this.copy(cacheEntry, ostream, range);
                        }
                    }
                }
                else {
                    response.setContentType("multipart/byteranges; boundary=CATALINA_MIME_BOUNDARY");
                    if (serveContent) {
                        try {
                            response.setBufferSize(this.output);
                        }
                        catch (IllegalStateException ex3) {}
                        if (ostream == null) {
                            throw new IllegalStateException();
                        }
                        this.copy(cacheEntry, ostream, ranges.iterator(), contentType);
                    }
                }
            }
            return;
        }
        String requestUri = (String)request.getAttribute("javax.servlet.include.request_uri");
        if (requestUri == null) {
            requestUri = request.getRequestURI();
            if (isError) {
                response.sendError((int)request.getAttribute("javax.servlet.error.status_code"));
            }
            else {
                response.sendError(404, DefaultServlet.sm.getString("defaultServlet.missingResource", new Object[] { requestUri }));
            }
            return;
        }
        throw new FileNotFoundException(DefaultServlet.sm.getString("defaultServlet.missingResource", new Object[] { requestUri }));
    }
    
    private void doDirectoryRedirect(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        final StringBuilder location = new StringBuilder(request.getRequestURI());
        location.append('/');
        if (request.getQueryString() != null) {
            location.append('?');
            location.append(request.getQueryString());
        }
        while (location.length() > 1 && location.charAt(1) == '/') {
            location.deleteCharAt(0);
        }
        response.sendRedirect(response.encodeRedirectURL(location.toString()));
    }
    
    protected Range parseContentRange(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        String rangeHeader = request.getHeader("Content-Range");
        if (rangeHeader == null) {
            return null;
        }
        if (!rangeHeader.startsWith("bytes")) {
            response.sendError(400);
            return null;
        }
        rangeHeader = rangeHeader.substring(6).trim();
        final int dashPos = rangeHeader.indexOf(45);
        final int slashPos = rangeHeader.indexOf(47);
        if (dashPos == -1) {
            response.sendError(400);
            return null;
        }
        if (slashPos == -1) {
            response.sendError(400);
            return null;
        }
        final Range range = new Range();
        try {
            range.start = Long.parseLong(rangeHeader.substring(0, dashPos));
            range.end = Long.parseLong(rangeHeader.substring(dashPos + 1, slashPos));
            range.length = Long.parseLong(rangeHeader.substring(slashPos + 1, rangeHeader.length()));
        }
        catch (NumberFormatException e) {
            response.sendError(400);
            return null;
        }
        if (!range.validate()) {
            response.sendError(400);
            return null;
        }
        return range;
    }
    
    protected ArrayList<Range> parseRange(final HttpServletRequest request, final HttpServletResponse response, final ResourceAttributes resourceAttributes) throws IOException {
        final String headerValue = request.getHeader("If-Range");
        if (headerValue != null) {
            long headerValueTime = -1L;
            try {
                headerValueTime = request.getDateHeader("If-Range");
            }
            catch (IllegalArgumentException ex) {}
            final String eTag = resourceAttributes.getETag();
            final long lastModified = resourceAttributes.getLastModified();
            if (headerValueTime == -1L) {
                if (!eTag.equals(headerValue.trim())) {
                    return DefaultServlet.FULL;
                }
            }
            else if (lastModified > headerValueTime + 1000L) {
                return DefaultServlet.FULL;
            }
        }
        final long fileLength = resourceAttributes.getContentLength();
        if (fileLength == 0L) {
            return null;
        }
        String rangeHeader = request.getHeader("Range");
        if (rangeHeader == null) {
            return null;
        }
        if (!rangeHeader.startsWith("bytes")) {
            response.addHeader("Content-Range", "bytes */" + fileLength);
            response.sendError(416);
            return null;
        }
        rangeHeader = rangeHeader.substring(6);
        final ArrayList<Range> result = new ArrayList<Range>();
        final StringTokenizer commaTokenizer = new StringTokenizer(rangeHeader, ",");
        while (commaTokenizer.hasMoreTokens()) {
            final String rangeDefinition = commaTokenizer.nextToken().trim();
            final Range currentRange = new Range();
            currentRange.length = fileLength;
            final int dashPos = rangeDefinition.indexOf(45);
            if (dashPos == -1) {
                response.addHeader("Content-Range", "bytes */" + fileLength);
                response.sendError(416);
                return null;
            }
            Label_0470: {
                if (dashPos == 0) {
                    try {
                        final long offset = Long.parseLong(rangeDefinition);
                        currentRange.start = fileLength + offset;
                        currentRange.end = fileLength - 1L;
                        break Label_0470;
                    }
                    catch (NumberFormatException e) {
                        response.addHeader("Content-Range", "bytes */" + fileLength);
                        response.sendError(416);
                        return null;
                    }
                }
                try {
                    currentRange.start = Long.parseLong(rangeDefinition.substring(0, dashPos));
                    if (dashPos < rangeDefinition.length() - 1) {
                        currentRange.end = Long.parseLong(rangeDefinition.substring(dashPos + 1, rangeDefinition.length()));
                    }
                    else {
                        currentRange.end = fileLength - 1L;
                    }
                }
                catch (NumberFormatException e) {
                    response.addHeader("Content-Range", "bytes */" + fileLength);
                    response.sendError(416);
                    return null;
                }
            }
            if (!currentRange.validate()) {
                response.addHeader("Content-Range", "bytes */" + fileLength);
                response.sendError(416);
                return null;
            }
            result.add(currentRange);
        }
        return result;
    }
    
    protected InputStream render(final String contextPath, final CacheEntry cacheEntry) throws IOException, ServletException {
        final Source xsltSource = this.findXsltInputStream(cacheEntry.context);
        if (xsltSource == null) {
            return this.renderHtml(contextPath, cacheEntry);
        }
        return this.renderXml(contextPath, cacheEntry, xsltSource);
    }
    
    protected InputStream renderXml(final String contextPath, final CacheEntry cacheEntry, final Source xsltSource) throws IOException, ServletException {
        final StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\"?>");
        sb.append("<listing ");
        sb.append(" contextPath='");
        sb.append(contextPath);
        sb.append("'");
        sb.append(" directory='");
        sb.append(cacheEntry.name);
        sb.append("' ");
        sb.append(" hasParent='").append(!cacheEntry.name.equals("/"));
        sb.append("'>");
        sb.append("<entries>");
        try {
            final NamingEnumeration<NameClassPair> enumeration = this.resources.list(cacheEntry.name);
            final String rewrittenContextPath = this.rewriteUrl(contextPath);
            while (enumeration.hasMoreElements()) {
                final NameClassPair ncPair = enumeration.nextElement();
                final String trimmed;
                final String resourceName = trimmed = ncPair.getName();
                if (!trimmed.equalsIgnoreCase("WEB-INF") && !trimmed.equalsIgnoreCase("META-INF")) {
                    if (trimmed.equalsIgnoreCase(this.localXsltFile)) {
                        continue;
                    }
                    if ((cacheEntry.name + trimmed).equals(this.contextXsltFile)) {
                        continue;
                    }
                    final CacheEntry childCacheEntry = this.resources.lookupCache(cacheEntry.name + resourceName);
                    if (!childCacheEntry.exists) {
                        continue;
                    }
                    sb.append("<entry");
                    sb.append(" type='").append((childCacheEntry.context != null) ? "dir" : "file").append("'");
                    sb.append(" urlPath='").append(rewrittenContextPath).append(this.rewriteUrl(cacheEntry.name + resourceName)).append((childCacheEntry.context != null) ? "/" : "").append("'");
                    if (childCacheEntry.resource != null) {
                        sb.append(" size='").append(this.renderSize(childCacheEntry.attributes.getContentLength())).append("'");
                    }
                    sb.append(" date='").append(childCacheEntry.attributes.getLastModifiedHttp()).append("'");
                    sb.append(">");
                    sb.append(RequestUtil.filter(trimmed));
                    if (childCacheEntry.context != null) {
                        sb.append("/");
                    }
                    sb.append("</entry>");
                }
            }
        }
        catch (NamingException e) {
            throw new ServletException("Error accessing resource", (Throwable)e);
        }
        sb.append("</entries>");
        final String readme = this.getReadme(cacheEntry.context);
        if (readme != null) {
            sb.append("<readme><![CDATA[");
            sb.append(readme);
            sb.append("]]></readme>");
        }
        sb.append("</listing>");
        ClassLoader original;
        if (Globals.IS_SECURITY_ENABLED) {
            final PrivilegedGetTccl pa = new PrivilegedGetTccl();
            original = AccessController.doPrivileged((PrivilegedAction<ClassLoader>)pa);
        }
        else {
            original = Thread.currentThread().getContextClassLoader();
        }
        try {
            if (Globals.IS_SECURITY_ENABLED) {
                final PrivilegedSetTccl pa2 = new PrivilegedSetTccl(DefaultServlet.class.getClassLoader());
                AccessController.doPrivileged((PrivilegedAction<Object>)pa2);
            }
            else {
                Thread.currentThread().setContextClassLoader(DefaultServlet.class.getClassLoader());
            }
            final TransformerFactory tFactory = TransformerFactory.newInstance();
            final Source xmlSource = new StreamSource(new StringReader(sb.toString()));
            final Transformer transformer = tFactory.newTransformer(xsltSource);
            final ByteArrayOutputStream stream = new ByteArrayOutputStream();
            final OutputStreamWriter osWriter = new OutputStreamWriter(stream, "UTF-8");
            final StreamResult out = new StreamResult(osWriter);
            transformer.transform(xmlSource, out);
            osWriter.flush();
            return new ByteArrayInputStream(stream.toByteArray());
        }
        catch (TransformerException e2) {
            throw new ServletException(DefaultServlet.sm.getString("defaultServlet.xslError"), (Throwable)e2);
        }
        finally {
            if (Globals.IS_SECURITY_ENABLED) {
                final PrivilegedSetTccl pa3 = new PrivilegedSetTccl(original);
                AccessController.doPrivileged((PrivilegedAction<Object>)pa3);
            }
            else {
                Thread.currentThread().setContextClassLoader(original);
            }
        }
    }
    
    protected InputStream renderHtml(final String contextPath, final CacheEntry cacheEntry) throws IOException, ServletException {
        final String name = cacheEntry.name;
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        final OutputStreamWriter osWriter = new OutputStreamWriter(stream, "UTF-8");
        final PrintWriter writer = new PrintWriter(osWriter);
        final StringBuilder sb = new StringBuilder();
        final String rewrittenContextPath = this.rewriteUrl(contextPath);
        sb.append("<!doctype html><html>\r\n");
        sb.append("<head>\r\n");
        sb.append("<title>");
        sb.append(DefaultServlet.sm.getString("directory.title", new Object[] { name }));
        sb.append("</title>\r\n");
        sb.append("<style>");
        sb.append("body {font-family:Tahoma,Arial,sans-serif;} h1, h2, h3, b {color:white;background-color:#525D76;} h1 {font-size:22px;} h2 {font-size:16px;} h3 {font-size:14px;} p {font-size:12px;} a {color:black;} .line {height:1px;background-color:#525D76;border:none;}");
        sb.append("</style> ");
        sb.append("</head>\r\n");
        sb.append("<body>");
        sb.append("<h1>");
        sb.append(DefaultServlet.sm.getString("directory.title", new Object[] { name }));
        String parentDirectory = name;
        if (parentDirectory.endsWith("/")) {
            parentDirectory = parentDirectory.substring(0, parentDirectory.length() - 1);
        }
        final int slash = parentDirectory.lastIndexOf(47);
        if (slash >= 0) {
            String parent = name.substring(0, slash);
            sb.append(" - <a href=\"");
            sb.append(rewrittenContextPath);
            if (parent.equals("")) {
                parent = "/";
            }
            sb.append(this.rewriteUrl(parent));
            if (!parent.endsWith("/")) {
                sb.append("/");
            }
            sb.append("\">");
            sb.append("<b>");
            sb.append(DefaultServlet.sm.getString("directory.parent", new Object[] { parent }));
            sb.append("</b>");
            sb.append("</a>");
        }
        sb.append("</h1>");
        sb.append("<hr class=\"line\">");
        sb.append("<table width=\"100%\" cellspacing=\"0\" cellpadding=\"5\" align=\"center\">\r\n");
        sb.append("<tr>\r\n");
        sb.append("<td align=\"left\"><font size=\"+1\"><strong>");
        sb.append(DefaultServlet.sm.getString("directory.filename"));
        sb.append("</strong></font></td>\r\n");
        sb.append("<td align=\"center\"><font size=\"+1\"><strong>");
        sb.append(DefaultServlet.sm.getString("directory.size"));
        sb.append("</strong></font></td>\r\n");
        sb.append("<td align=\"right\"><font size=\"+1\"><strong>");
        sb.append(DefaultServlet.sm.getString("directory.lastModified"));
        sb.append("</strong></font></td>\r\n");
        sb.append("</tr>");
        try {
            final NamingEnumeration<NameClassPair> enumeration = this.resources.list(cacheEntry.name);
            boolean shade = false;
            while (enumeration.hasMoreElements()) {
                final NameClassPair ncPair = enumeration.nextElement();
                final String trimmed;
                String resourceName = trimmed = ncPair.getName();
                if (!trimmed.equalsIgnoreCase("WEB-INF")) {
                    if (trimmed.equalsIgnoreCase("META-INF")) {
                        continue;
                    }
                    final CacheEntry childCacheEntry = this.resources.lookupCache(cacheEntry.name + resourceName);
                    if (!childCacheEntry.exists) {
                        continue;
                    }
                    sb.append("<tr");
                    if (shade) {
                        sb.append(" bgcolor=\"#eeeeee\"");
                    }
                    sb.append(">\r\n");
                    shade = !shade;
                    sb.append("<td align=\"left\">&nbsp;&nbsp;\r\n");
                    sb.append("<a href=\"");
                    sb.append(rewrittenContextPath);
                    resourceName = this.rewriteUrl(name + resourceName);
                    sb.append(resourceName);
                    if (childCacheEntry.context != null) {
                        sb.append("/");
                    }
                    sb.append("\"><tt>");
                    sb.append(RequestUtil.filter(trimmed));
                    if (childCacheEntry.context != null) {
                        sb.append("/");
                    }
                    sb.append("</tt></a></td>\r\n");
                    sb.append("<td align=\"right\"><tt>");
                    if (childCacheEntry.context != null) {
                        sb.append("&nbsp;");
                    }
                    else {
                        sb.append(this.renderSize(childCacheEntry.attributes.getContentLength()));
                    }
                    sb.append("</tt></td>\r\n");
                    sb.append("<td align=\"right\"><tt>");
                    sb.append(childCacheEntry.attributes.getLastModifiedHttp());
                    sb.append("</tt></td>\r\n");
                    sb.append("</tr>\r\n");
                }
            }
        }
        catch (NamingException e) {
            throw new ServletException("Error accessing resource", (Throwable)e);
        }
        sb.append("</table>\r\n");
        sb.append("<hr class=\"line\">");
        final String readme = this.getReadme(cacheEntry.context);
        if (readme != null) {
            sb.append(readme);
            sb.append("<hr class=\"line\">");
        }
        if (this.showServerInfo) {
            sb.append("<h3>").append(ServerInfo.getServerInfo()).append("</h3>");
        }
        sb.append("</body>\r\n");
        sb.append("</html>\r\n");
        writer.write(sb.toString());
        writer.flush();
        return new ByteArrayInputStream(stream.toByteArray());
    }
    
    protected String renderSize(final long size) {
        final long leftSide = size / 1024L;
        long rightSide = size % 1024L / 103L;
        if (leftSide == 0L && rightSide == 0L && size > 0L) {
            rightSide = 1L;
        }
        return "" + leftSide + "." + rightSide + " kb";
    }
    
    protected String getReadme(final DirContext directory) throws IOException {
        if (this.readmeFile != null) {
            try {
                final Object obj = directory.lookup(this.readmeFile);
                if (obj != null && obj instanceof Resource) {
                    final StringWriter buffer = new StringWriter();
                    InputStream is = null;
                    Reader reader = null;
                    try {
                        is = ((Resource)obj).streamContent();
                        if (this.fileEncoding != null) {
                            reader = new InputStreamReader(is, this.fileEncoding);
                        }
                        else {
                            reader = new InputStreamReader(is);
                        }
                        this.copyRange(reader, new PrintWriter(buffer));
                    }
                    finally {
                        if (reader != null) {
                            try {
                                reader.close();
                            }
                            catch (IOException e) {
                                this.log("Could not close reader", (Throwable)e);
                            }
                        }
                        if (is != null) {
                            try {
                                is.close();
                            }
                            catch (IOException e) {
                                this.log("Could not close is", (Throwable)e);
                            }
                        }
                    }
                    return buffer.toString();
                }
            }
            catch (NamingException e2) {
                if (this.debug > 10) {
                    this.log("readme '" + this.readmeFile + "' not found", (Throwable)e2);
                }
                return null;
            }
        }
        return null;
    }
    
    protected Source findXsltInputStream(final DirContext directory) throws IOException {
        if (this.localXsltFile != null) {
            try {
                final Object obj = directory.lookup(this.localXsltFile);
                if (obj != null && obj instanceof Resource) {
                    final InputStream is = ((Resource)obj).streamContent();
                    if (is != null) {
                        if (Globals.IS_SECURITY_ENABLED) {
                            return this.secureXslt(is);
                        }
                        return new StreamSource(is);
                    }
                }
            }
            catch (NamingException e) {
                if (this.debug > 10) {
                    this.log("localXsltFile '" + this.localXsltFile + "' not found", (Throwable)e);
                }
            }
        }
        if (this.contextXsltFile != null) {
            final InputStream is2 = this.getServletContext().getResourceAsStream(this.contextXsltFile);
            if (is2 != null) {
                if (Globals.IS_SECURITY_ENABLED) {
                    return this.secureXslt(is2);
                }
                return new StreamSource(is2);
            }
            else if (this.debug > 10) {
                this.log("contextXsltFile '" + this.contextXsltFile + "' not found");
            }
        }
        if (this.globalXsltFile != null) {
            final File f = this.validateGlobalXsltFile();
            if (f != null) {
                final long globalXsltFileSize = f.length();
                if (globalXsltFileSize > 2147483647L) {
                    this.log("globalXsltFile [" + f.getAbsolutePath() + "] is too big to buffer");
                }
                else {
                    FileInputStream fis = null;
                    try {
                        fis = new FileInputStream(f);
                        final byte[] b = new byte[(int)f.length()];
                        IOTools.readFully(fis, b);
                        return new StreamSource(new ByteArrayInputStream(b));
                    }
                    finally {
                        if (fis != null) {
                            try {
                                fis.close();
                            }
                            catch (IOException ex) {}
                        }
                    }
                }
            }
        }
        return null;
    }
    
    private File validateGlobalXsltFile() {
        File result = null;
        final String base = System.getProperty("catalina.base");
        if (base != null) {
            final File baseConf = new File(base, "conf");
            result = this.validateGlobalXsltFile(baseConf);
        }
        if (result == null) {
            final String home = System.getProperty("catalina.home");
            if (home != null && !home.equals(base)) {
                final File homeConf = new File(home, "conf");
                result = this.validateGlobalXsltFile(homeConf);
            }
        }
        return result;
    }
    
    private File validateGlobalXsltFile(final File base) {
        File candidate = new File(this.globalXsltFile);
        if (!candidate.isAbsolute()) {
            candidate = new File(base, this.globalXsltFile);
        }
        if (!candidate.isFile()) {
            return null;
        }
        try {
            if (!candidate.getCanonicalPath().startsWith(base.getCanonicalPath())) {
                return null;
            }
        }
        catch (IOException ioe) {
            return null;
        }
        final String nameLower = candidate.getName().toLowerCase(Locale.ENGLISH);
        if (!nameLower.endsWith(".xslt") && !nameLower.endsWith(".xsl")) {
            return null;
        }
        return candidate;
    }
    
    private Source secureXslt(final InputStream is) {
        Source result = null;
        try {
            final DocumentBuilder builder = DefaultServlet.factory.newDocumentBuilder();
            builder.setEntityResolver(DefaultServlet.secureEntityResolver);
            final Document document = builder.parse(is);
            result = new DOMSource(document);
        }
        catch (ParserConfigurationException e) {
            if (this.debug > 0) {
                this.log(e.getMessage(), (Throwable)e);
            }
        }
        catch (SAXException e2) {
            if (this.debug > 0) {
                this.log(e2.getMessage(), (Throwable)e2);
            }
        }
        catch (IOException e3) {
            if (this.debug > 0) {
                this.log(e3.getMessage(), (Throwable)e3);
            }
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                }
                catch (IOException ex) {}
            }
        }
        return result;
    }
    
    protected boolean checkSendfile(final HttpServletRequest request, final HttpServletResponse response, final CacheEntry entry, final long length, final Range range) {
        if (this.sendfileSize > 0 && entry.resource != null && (length > this.sendfileSize || entry.resource.getContent() == null) && entry.attributes.getCanonicalPath() != null && Boolean.TRUE.equals(request.getAttribute("org.apache.tomcat.sendfile.support")) && request.getClass().getName().equals("org.apache.catalina.connector.RequestFacade") && response.getClass().getName().equals("org.apache.catalina.connector.ResponseFacade")) {
            request.setAttribute("org.apache.tomcat.sendfile.filename", (Object)entry.attributes.getCanonicalPath());
            if (range == null) {
                request.setAttribute("org.apache.tomcat.sendfile.start", (Object)0L);
                request.setAttribute("org.apache.tomcat.sendfile.end", (Object)length);
            }
            else {
                request.setAttribute("org.apache.tomcat.sendfile.start", (Object)range.start);
                request.setAttribute("org.apache.tomcat.sendfile.end", (Object)(range.end + 1L));
            }
            return true;
        }
        return false;
    }
    
    protected boolean checkIfMatch(final HttpServletRequest request, final HttpServletResponse response, final ResourceAttributes resourceAttributes) throws IOException {
        String eTag = resourceAttributes.getETag();
        if (eTag.startsWith("W/")) {
            eTag = eTag.substring(2);
        }
        final String headerValue = request.getHeader("If-Match");
        if (headerValue != null && headerValue.indexOf(42) == -1) {
            StringTokenizer commaTokenizer;
            boolean conditionSatisfied;
            String currentToken;
            for (commaTokenizer = new StringTokenizer(headerValue, ","), conditionSatisfied = false; !conditionSatisfied && commaTokenizer.hasMoreTokens(); conditionSatisfied = true) {
                currentToken = commaTokenizer.nextToken();
                currentToken = currentToken.trim();
                if (currentToken.startsWith("W/")) {
                    currentToken = currentToken.substring(2);
                }
                if (currentToken.equals(eTag)) {}
            }
            if (!conditionSatisfied) {
                response.sendError(412);
                return false;
            }
        }
        return true;
    }
    
    protected boolean checkIfModifiedSince(final HttpServletRequest request, final HttpServletResponse response, final ResourceAttributes resourceAttributes) {
        try {
            final long headerValue = request.getDateHeader("If-Modified-Since");
            final long lastModified = resourceAttributes.getLastModified();
            if (headerValue != -1L && request.getHeader("If-None-Match") == null && lastModified < headerValue + 1000L) {
                response.setStatus(304);
                response.setHeader("ETag", resourceAttributes.getETag());
                return false;
            }
        }
        catch (IllegalArgumentException illegalArgument) {
            return true;
        }
        return true;
    }
    
    protected boolean checkIfNoneMatch(final HttpServletRequest request, final HttpServletResponse response, final ResourceAttributes resourceAttributes) throws IOException {
        final String eTag = resourceAttributes.getETag();
        final String headerValue = request.getHeader("If-None-Match");
        if (headerValue != null) {
            boolean conditionSatisfied = false;
            if (!headerValue.equals("*")) {
                for (StringTokenizer commaTokenizer = new StringTokenizer(headerValue, ","); !conditionSatisfied && commaTokenizer.hasMoreTokens(); conditionSatisfied = true) {
                    final String currentToken = commaTokenizer.nextToken();
                    if (currentToken.trim().equals(eTag)) {}
                }
            }
            else {
                conditionSatisfied = true;
            }
            if (conditionSatisfied) {
                if ("GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod())) {
                    response.setStatus(304);
                    response.setHeader("ETag", eTag);
                    return false;
                }
                response.sendError(412);
                return false;
            }
        }
        return true;
    }
    
    protected boolean checkIfUnmodifiedSince(final HttpServletRequest request, final HttpServletResponse response, final ResourceAttributes resourceAttributes) throws IOException {
        try {
            final long lastModified = resourceAttributes.getLastModified();
            final long headerValue = request.getDateHeader("If-Unmodified-Since");
            if (headerValue != -1L && lastModified >= headerValue + 1000L) {
                response.sendError(412);
                return false;
            }
        }
        catch (IllegalArgumentException illegalArgument) {
            return true;
        }
        return true;
    }
    
    protected void copy(final CacheEntry cacheEntry, final InputStream is, final ServletOutputStream ostream) throws IOException {
        IOException exception = null;
        InputStream resourceInputStream = null;
        if (cacheEntry.resource != null) {
            final byte[] buffer = cacheEntry.resource.getContent();
            if (buffer != null) {
                ostream.write(buffer, 0, buffer.length);
                return;
            }
            resourceInputStream = cacheEntry.resource.streamContent();
        }
        else {
            resourceInputStream = is;
        }
        final InputStream istream = new BufferedInputStream(resourceInputStream, this.input);
        exception = this.copyRange(istream, ostream);
        istream.close();
        if (exception != null) {
            throw exception;
        }
    }
    
    protected void copy(final CacheEntry cacheEntry, final InputStream is, final PrintWriter writer) throws IOException {
        IOException exception = null;
        InputStream resourceInputStream = null;
        if (cacheEntry.resource != null) {
            resourceInputStream = cacheEntry.resource.streamContent();
        }
        else {
            resourceInputStream = is;
        }
        Reader reader;
        if (this.fileEncoding == null) {
            reader = new InputStreamReader(resourceInputStream);
        }
        else {
            reader = new InputStreamReader(resourceInputStream, this.fileEncoding);
        }
        exception = this.copyRange(reader, writer);
        reader.close();
        if (exception != null) {
            throw exception;
        }
    }
    
    protected void copy(final CacheEntry cacheEntry, final ServletOutputStream ostream, final Range range) throws IOException {
        IOException exception = null;
        final InputStream resourceInputStream = cacheEntry.resource.streamContent();
        final InputStream istream = new BufferedInputStream(resourceInputStream, this.input);
        exception = this.copyRange(istream, ostream, range.start, range.end);
        istream.close();
        if (exception != null) {
            throw exception;
        }
    }
    
    protected void copy(final CacheEntry cacheEntry, final ServletOutputStream ostream, final Iterator<Range> ranges, final String contentType) throws IOException {
        IOException exception = null;
        while (exception == null && ranges.hasNext()) {
            final InputStream resourceInputStream = cacheEntry.resource.streamContent();
            InputStream istream = null;
            try {
                istream = new BufferedInputStream(resourceInputStream, this.input);
                final Range currentRange = ranges.next();
                ostream.println();
                ostream.println("--CATALINA_MIME_BOUNDARY");
                if (contentType != null) {
                    ostream.println("Content-Type: " + contentType);
                }
                ostream.println("Content-Range: bytes " + currentRange.start + "-" + currentRange.end + "/" + currentRange.length);
                ostream.println();
                exception = this.copyRange(istream, ostream, currentRange.start, currentRange.end);
            }
            finally {
                if (istream != null) {
                    try {
                        istream.close();
                    }
                    catch (IOException ex) {}
                }
            }
        }
        ostream.println();
        ostream.print("--CATALINA_MIME_BOUNDARY--");
        if (exception != null) {
            throw exception;
        }
    }
    
    protected IOException copyRange(final InputStream istream, final ServletOutputStream ostream) {
        IOException exception = null;
        final byte[] buffer = new byte[this.input];
        int len = buffer.length;
        try {
            while (true) {
                len = istream.read(buffer);
                if (len == -1) {
                    break;
                }
                ostream.write(buffer, 0, len);
            }
        }
        catch (IOException e) {
            exception = e;
            len = -1;
        }
        return exception;
    }
    
    protected IOException copyRange(final Reader reader, final PrintWriter writer) {
        IOException exception = null;
        final char[] buffer = new char[this.input];
        int len = buffer.length;
        try {
            while (true) {
                len = reader.read(buffer);
                if (len == -1) {
                    break;
                }
                writer.write(buffer, 0, len);
            }
        }
        catch (IOException e) {
            exception = e;
            len = -1;
        }
        return exception;
    }
    
    protected IOException copyRange(final InputStream istream, final ServletOutputStream ostream, final long start, final long end) {
        if (this.debug > 10) {
            this.log("Serving bytes:" + start + "-" + end);
        }
        long skipped = 0L;
        try {
            skipped = istream.skip(start);
        }
        catch (IOException e) {
            return e;
        }
        if (skipped < start) {
            return new IOException(DefaultServlet.sm.getString("defaultServlet.skipfail", new Object[] { skipped, start }));
        }
        IOException exception = null;
        long bytesToRead = end - start + 1L;
        final byte[] buffer = new byte[this.input];
        int len = buffer.length;
        while (bytesToRead > 0L && len >= buffer.length) {
            try {
                len = istream.read(buffer);
                if (bytesToRead >= len) {
                    ostream.write(buffer, 0, len);
                    bytesToRead -= len;
                }
                else {
                    ostream.write(buffer, 0, (int)bytesToRead);
                    bytesToRead = 0L;
                }
            }
            catch (IOException e2) {
                exception = e2;
                len = -1;
            }
            if (len < buffer.length) {
                break;
            }
        }
        return exception;
    }
    
    static {
        sm = StringManager.getManager("org.apache.catalina.servlets");
        FULL = new ArrayList<Range>();
        (urlEncoder = new URLEncoder()).addSafeCharacter('-');
        DefaultServlet.urlEncoder.addSafeCharacter('_');
        DefaultServlet.urlEncoder.addSafeCharacter('.');
        DefaultServlet.urlEncoder.addSafeCharacter('*');
        DefaultServlet.urlEncoder.addSafeCharacter('/');
        if (Globals.IS_SECURITY_ENABLED) {
            (factory = DocumentBuilderFactory.newInstance()).setNamespaceAware(true);
            DefaultServlet.factory.setValidating(false);
            secureEntityResolver = new SecureEntityResolver();
        }
        else {
            factory = null;
            secureEntityResolver = null;
        }
    }
    
    protected static class Range
    {
        public long start;
        public long end;
        public long length;
        
        public boolean validate() {
            if (this.end >= this.length) {
                this.end = this.length - 1L;
            }
            return this.start >= 0L && this.end >= 0L && this.start <= this.end && this.length > 0L;
        }
    }
    
    private static class SecureEntityResolver implements EntityResolver2
    {
        @Override
        public InputSource resolveEntity(final String publicId, final String systemId) throws SAXException, IOException {
            throw new SAXException(DefaultServlet.sm.getString("defaultServlet.blockExternalEntity", new Object[] { publicId, systemId }));
        }
        
        @Override
        public InputSource getExternalSubset(final String name, final String baseURI) throws SAXException, IOException {
            throw new SAXException(DefaultServlet.sm.getString("defaultServlet.blockExternalSubset", new Object[] { name, baseURI }));
        }
        
        @Override
        public InputSource resolveEntity(final String name, final String publicId, final String baseURI, final String systemId) throws SAXException, IOException {
            throw new SAXException(DefaultServlet.sm.getString("defaultServlet.blockExternalEntity2", new Object[] { name, publicId, baseURI, systemId }));
        }
    }
}
