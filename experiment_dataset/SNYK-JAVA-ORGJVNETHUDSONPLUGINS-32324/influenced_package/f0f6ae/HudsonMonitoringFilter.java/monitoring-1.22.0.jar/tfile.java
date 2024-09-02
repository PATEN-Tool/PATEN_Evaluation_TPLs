// 
// Decompiled by Procyon v0.5.36
// 

package org.jvnet.hudson.plugins.monitoring;

import java.io.IOException;
import hudson.model.Hudson;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.FilterChain;
import javax.servlet.ServletResponse;
import javax.servlet.ServletRequest;
import javax.servlet.ServletException;
import javax.servlet.FilterConfig;
import net.bull.javamelody.PluginMonitoringFilter;

public class HudsonMonitoringFilter extends PluginMonitoringFilter
{
    private static final boolean PLUGIN_AUTHENTICATION_DISABLED;
    
    public void init(final FilterConfig config) throws ServletException {
        super.init(config);
    }
    
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest)) {
            super.doFilter(request, response, chain);
            return;
        }
        final HttpServletRequest httpRequest = (HttpServletRequest)request;
        if (!HudsonMonitoringFilter.PLUGIN_AUTHENTICATION_DISABLED && httpRequest.getRequestURI().equals(this.getMonitoringUrl(httpRequest))) {
            Hudson.getInstance().checkPermission(Hudson.ADMINISTER);
        }
        super.doFilter(request, response, chain);
    }
    
    static {
        PLUGIN_AUTHENTICATION_DISABLED = Boolean.parseBoolean(System.getProperty("javamelody.plugin-authentication-disabled"));
    }
}
