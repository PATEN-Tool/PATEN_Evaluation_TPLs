// 
// Decompiled by Procyon v0.5.36
// 

package org.jvnet.hudson.plugins.monitoring;

import javax.servlet.ServletException;
import java.io.IOException;
import hudson.model.Hudson;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.FilterChain;
import javax.servlet.ServletResponse;
import javax.servlet.ServletRequest;
import net.bull.javamelody.MonitoringFilter;

public class HudsonMonitoringFilter extends MonitoringFilter
{
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest)) {
            super.doFilter(request, response, chain);
            return;
        }
        final HttpServletRequest httpRequest = (HttpServletRequest)request;
        if (httpRequest.getRequestURI().equals(this.getMonitoringUrl(httpRequest))) {
            Hudson.getInstance().checkPermission(Hudson.ADMINISTER);
        }
        super.doFilter(request, response, chain);
    }
}
