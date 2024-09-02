// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.web.servlet;

import org.springframework.web.context.support.ServletContextResource;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletResponse;
import javax.servlet.ServletRequest;
import org.springframework.util.StringUtils;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

public class ResourceServlet extends HttpServletBean
{
    public static final String RESOURCE_URL_DELIMITERS = ",; \t\n";
    public static final String RESOURCE_PARAM_NAME = "resource";
    private String defaultUrl;
    private String allowedResources;
    private String contentType;
    private boolean applyLastModified;
    private PathMatcher pathMatcher;
    private long startupTime;
    
    public ResourceServlet() {
        this.applyLastModified = false;
    }
    
    public void setDefaultUrl(final String defaultUrl) {
        this.defaultUrl = defaultUrl;
    }
    
    public void setAllowedResources(final String allowedResources) {
        this.allowedResources = allowedResources;
    }
    
    public void setContentType(final String contentType) {
        this.contentType = contentType;
    }
    
    public void setApplyLastModified(final boolean applyLastModified) {
        this.applyLastModified = applyLastModified;
    }
    
    @Override
    protected void initServletBean() {
        this.pathMatcher = this.getPathMatcher();
        this.startupTime = System.currentTimeMillis();
    }
    
    protected PathMatcher getPathMatcher() {
        return (PathMatcher)new AntPathMatcher();
    }
    
    protected final void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        final String resourceUrl = this.determineResourceUrl(request);
        if (resourceUrl != null) {
            try {
                this.doInclude(request, response, resourceUrl);
                return;
            }
            catch (ServletException ex) {
                if (this.logger.isWarnEnabled()) {
                    this.logger.warn((Object)("Failed to include content of resource [" + resourceUrl + "]"), (Throwable)ex);
                }
                if (!this.includeDefaultUrl(request, response)) {
                    throw ex;
                }
                return;
            }
            catch (IOException ex2) {
                if (this.logger.isWarnEnabled()) {
                    this.logger.warn((Object)("Failed to include content of resource [" + resourceUrl + "]"), (Throwable)ex2);
                }
                if (!this.includeDefaultUrl(request, response)) {
                    throw ex2;
                }
                return;
            }
        }
        if (!this.includeDefaultUrl(request, response)) {
            throw new ServletException("No target resource URL found for request");
        }
    }
    
    protected String determineResourceUrl(final HttpServletRequest request) {
        return request.getParameter("resource");
    }
    
    private boolean includeDefaultUrl(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        if (this.defaultUrl == null) {
            return false;
        }
        this.doInclude(request, response, this.defaultUrl);
        return true;
    }
    
    private void doInclude(final HttpServletRequest request, final HttpServletResponse response, final String resourceUrl) throws ServletException, IOException {
        if (this.contentType != null) {
            response.setContentType(this.contentType);
        }
        final String[] resourceUrls = StringUtils.tokenizeToStringArray(resourceUrl, ",; \t\n");
        for (int i = 0; i < resourceUrls.length; ++i) {
            if (this.allowedResources != null && !this.pathMatcher.match(this.allowedResources, resourceUrls[i])) {
                throw new ServletException("Resource [" + resourceUrls[i] + "] does not match allowed pattern [" + this.allowedResources + "]");
            }
            if (this.logger.isDebugEnabled()) {
                this.logger.debug((Object)("Including resource [" + resourceUrls[i] + "]"));
            }
            final RequestDispatcher rd = request.getRequestDispatcher(resourceUrls[i]);
            rd.include((ServletRequest)request, (ServletResponse)response);
        }
    }
    
    protected final long getLastModified(final HttpServletRequest request) {
        if (this.applyLastModified) {
            String resourceUrl = this.determineResourceUrl(request);
            if (resourceUrl == null) {
                resourceUrl = this.defaultUrl;
            }
            if (resourceUrl != null) {
                final String[] resourceUrls = StringUtils.tokenizeToStringArray(resourceUrl, ",; \t\n");
                long latestTimestamp = -1L;
                for (int i = 0; i < resourceUrls.length; ++i) {
                    final long timestamp = this.getFileTimestamp(resourceUrls[i]);
                    if (timestamp > latestTimestamp) {
                        latestTimestamp = timestamp;
                    }
                }
                return (latestTimestamp > this.startupTime) ? latestTimestamp : this.startupTime;
            }
        }
        return -1L;
    }
    
    protected long getFileTimestamp(final String resourceUrl) {
        final ServletContextResource resource = new ServletContextResource(this.getServletContext(), resourceUrl);
        try {
            final long lastModifiedTime = resource.lastModified();
            if (this.logger.isDebugEnabled()) {
                this.logger.debug((Object)("Last-modified timestamp of " + resource + " is " + lastModifiedTime));
            }
            return lastModifiedTime;
        }
        catch (IOException ex) {
            this.logger.warn((Object)("Couldn't retrieve last-modified timestamp of [" + resource + "] - using ResourceServlet startup time"));
            return -1L;
        }
    }
}
