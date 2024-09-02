// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.unomi.web;

import org.slf4j.LoggerFactory;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletException;
import javax.servlet.ServletConfig;
import org.apache.unomi.api.services.ConfigSharingService;
import org.slf4j.Logger;
import javax.servlet.http.HttpServlet;

@Deprecated
public class ContextServlet extends HttpServlet
{
    private static final long serialVersionUID = 2928875830103325238L;
    private static final Logger logger;
    private static final int MAX_COOKIE_AGE_IN_SECONDS = 31536000;
    private String profileIdCookieName;
    private String profileIdCookieDomain;
    private int profileIdCookieMaxAgeInSeconds;
    private int publicPostRequestBytesLimit;
    private ConfigSharingService configSharingService;
    
    public ContextServlet() {
        this.profileIdCookieName = "context-profile-id";
        this.profileIdCookieMaxAgeInSeconds = 31536000;
        this.publicPostRequestBytesLimit = 200000;
    }
    
    public void init(final ServletConfig config) throws ServletException {
        super.init(config);
        this.configSharingService.setProperty("profileIdCookieName", (Object)this.profileIdCookieName);
        this.configSharingService.setProperty("profileIdCookieDomain", (Object)this.profileIdCookieDomain);
        this.configSharingService.setProperty("profileIdCookieMaxAgeInSeconds", (Object)this.profileIdCookieMaxAgeInSeconds);
        this.configSharingService.setProperty("publicPostRequestBytesLimit", (Object)this.publicPostRequestBytesLimit);
        ContextServlet.logger.info("ContextServlet initialized.");
    }
    
    public void service(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        HttpServletRequestForwardWrapper.forward(request, response);
    }
    
    public void destroy() {
        ContextServlet.logger.info("Context servlet shutdown.");
    }
    
    public void setProfileIdCookieDomain(final String profileIdCookieDomain) {
        this.profileIdCookieDomain = profileIdCookieDomain;
    }
    
    public void setProfileIdCookieName(final String profileIdCookieName) {
        this.profileIdCookieName = profileIdCookieName;
    }
    
    public void setProfileIdCookieMaxAgeInSeconds(final int profileIdCookieMaxAgeInSeconds) {
        this.profileIdCookieMaxAgeInSeconds = profileIdCookieMaxAgeInSeconds;
    }
    
    public void setPublicPostRequestBytesLimit(final int publicPostRequestBytesLimit) {
        this.publicPostRequestBytesLimit = publicPostRequestBytesLimit;
    }
    
    public void setConfigSharingService(final ConfigSharingService configSharingService) {
        this.configSharingService = configSharingService;
    }
    
    static {
        logger = LoggerFactory.getLogger(ContextServlet.class.getName());
    }
}
