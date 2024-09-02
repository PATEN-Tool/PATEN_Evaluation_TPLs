// 
// Decompiled by Procyon v0.5.36
// 

package org.jvnet.hudson.plugins.monitoring;

import net.bull.javamelody.NodesController;
import java.io.IOException;
import hudson.model.Hudson;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.FilterChain;
import javax.servlet.ServletResponse;
import javax.servlet.ServletRequest;
import javax.servlet.ServletException;
import net.bull.javamelody.MonitoringFilter;
import javax.servlet.FilterConfig;
import net.bull.javamelody.NodesCollector;
import net.bull.javamelody.PluginMonitoringFilter;

public class HudsonMonitoringFilter extends PluginMonitoringFilter
{
    private static final boolean PLUGIN_AUTHENTICATION_DISABLED;
    private NodesCollector nodesCollector;
    
    public void init(final FilterConfig config) throws ServletException {
        super.init(config);
        this.nodesCollector = new NodesCollector((MonitoringFilter)this);
    }
    
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse) || this.getNodesCollector().isMonitoringDisabled()) {
            super.doFilter(request, response, chain);
            return;
        }
        final HttpServletRequest httpRequest = (HttpServletRequest)request;
        final String requestURI = httpRequest.getRequestURI();
        final String monitoringUrl = this.getMonitoringUrl(httpRequest);
        final String monitoringSlavesUrl = monitoringUrl + "/nodes";
        if (!HudsonMonitoringFilter.PLUGIN_AUTHENTICATION_DISABLED && (requestURI.equals(monitoringUrl) || requestURI.equals(monitoringSlavesUrl))) {
            Hudson.getInstance().checkPermission(Hudson.ADMINISTER);
        }
        if (requestURI.equals(monitoringSlavesUrl)) {
            final HttpServletResponse httpResponse = (HttpServletResponse)response;
            this.doMonitoring(httpRequest, httpResponse);
            return;
        }
        super.doFilter(request, response, chain);
    }
    
    private void doMonitoring(final HttpServletRequest httpRequest, final HttpServletResponse httpResponse) throws IOException {
        if (NodesController.isJavaInformationsNeeded(httpRequest)) {
            this.getNodesCollector().collectWithoutErrors();
        }
        final NodesController nodesController = new NodesController(this.getNodesCollector());
        nodesController.doMonitoring(httpRequest, httpResponse);
    }
    
    NodesCollector getNodesCollector() {
        return this.nodesCollector;
    }
    
    static {
        PLUGIN_AUTHENTICATION_DISABLED = Boolean.parseBoolean(System.getProperty("javamelody.plugin-authentication-disabled"));
    }
}
