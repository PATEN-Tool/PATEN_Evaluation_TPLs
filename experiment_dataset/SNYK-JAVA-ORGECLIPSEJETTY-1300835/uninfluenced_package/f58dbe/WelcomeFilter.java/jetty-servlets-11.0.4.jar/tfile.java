// 
// Decompiled by Procyon v0.5.36
// 

package org.eclipse.jetty.servlets;

import jakarta.servlet.ServletException;
import java.io.IOException;
import org.eclipse.jetty.util.URIUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.Filter;

@Deprecated
public class WelcomeFilter implements Filter
{
    private String welcome;
    
    public void init(final FilterConfig filterConfig) {
        this.welcome = filterConfig.getInitParameter("welcome");
        if (this.welcome == null) {
            this.welcome = "index.html";
        }
    }
    
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
        final String path = ((HttpServletRequest)request).getServletPath();
        if (this.welcome != null && path.endsWith("/")) {
            final String uriInContext = URIUtil.encodePath(URIUtil.addPaths(path, this.welcome));
            request.getRequestDispatcher(uriInContext).forward(request, response);
        }
        else {
            chain.doFilter(request, response);
        }
    }
    
    public void destroy() {
    }
}
