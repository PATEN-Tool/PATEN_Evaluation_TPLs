// 
// Decompiled by Procyon v0.5.36
// 

package com.walmartlabs.concord.server.boot.filters;

import javax.servlet.ServletException;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.FilterChain;
import javax.servlet.ServletResponse;
import javax.servlet.ServletRequest;
import javax.servlet.FilterConfig;
import javax.inject.Inject;
import org.slf4j.LoggerFactory;
import com.walmartlabs.concord.server.cfg.ServerConfiguration;
import org.slf4j.Logger;
import javax.servlet.annotation.WebFilter;
import javax.inject.Singleton;
import javax.inject.Named;
import javax.servlet.Filter;

@Named
@Singleton
@WebFilter({ "/api/*", "/logs/*", "/forms/*" })
public class CORSFilter implements Filter
{
    private static final Logger log;
    private final ServerConfiguration cfg;
    
    static {
        log = LoggerFactory.getLogger((Class)CORSFilter.class);
    }
    
    @Inject
    public CORSFilter(final ServerConfiguration cfg) {
        this.cfg = cfg;
    }
    
    public void init(final FilterConfig filterConfig) {
        CORSFilter.log.info("CORS filter enabled");
    }
    
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
        final HttpServletResponse httpResp = (HttpServletResponse)response;
        httpResp.setHeader("Access-Control-Allow-Origin", this.cfg.getCORSConfiguration().getAllowOrigin());
        httpResp.setHeader("Access-Control-Allow-Methods", "*");
        httpResp.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, Range, Cookie, Origin");
        httpResp.setHeader("Access-Control-Expose-Headers", "cache-control,content-language,expires,last-modified,content-range,content-length,accept-ranges");
        final HttpServletRequest httpReq = (HttpServletRequest)request;
        if ("OPTIONS".equalsIgnoreCase(httpReq.getMethod())) {
            httpResp.setHeader("Allow", "OPTIONS, GET, POST, PUT, DELETE");
            httpResp.setStatus(204);
            return;
        }
        chain.doFilter(request, response);
    }
    
    public void destroy() {
    }
}
