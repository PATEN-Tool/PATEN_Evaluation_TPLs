// 
// Decompiled by Procyon v0.5.36
// 

package io.hawt.web.filters;

import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.FilterChain;
import javax.servlet.ServletResponse;
import javax.servlet.ServletRequest;
import javax.servlet.ServletException;
import javax.servlet.FilterConfig;
import io.hawt.system.ConfigManager;
import javax.servlet.Filter;

public abstract class HttpHeaderFilter implements Filter
{
    private ConfigManager configManager;
    
    public void init(final FilterConfig filterConfig) throws ServletException {
        this.configManager = (ConfigManager)filterConfig.getServletContext().getAttribute("ConfigManager");
    }
    
    public void destroy() {
    }
    
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            this.addHeaders((HttpServletRequest)request, (HttpServletResponse)response);
        }
        chain.doFilter(request, response);
    }
    
    protected abstract void addHeaders(final HttpServletRequest p0, final HttpServletResponse p1) throws IOException, ServletException;
    
    protected String getConfigParameter(final String key) {
        return this.configManager.get(key, null);
    }
}
