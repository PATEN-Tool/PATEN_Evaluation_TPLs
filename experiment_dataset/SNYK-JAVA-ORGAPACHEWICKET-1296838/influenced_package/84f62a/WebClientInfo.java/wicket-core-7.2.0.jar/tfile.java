// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.wicket.protocol.http.request;

import org.slf4j.LoggerFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import java.net.UnknownHostException;
import java.net.InetAddress;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.protocol.http.ClientProperties;
import org.slf4j.Logger;
import org.apache.wicket.core.request.ClientInfo;

public class WebClientInfo extends ClientInfo
{
    private static final long serialVersionUID = 1L;
    private static final Logger log;
    private final String userAgent;
    private final ClientProperties properties;
    
    public WebClientInfo(final RequestCycle requestCycle) {
        this(requestCycle, ((ServletWebRequest)requestCycle.getRequest()).getContainerRequest().getHeader("User-Agent"));
    }
    
    public WebClientInfo(final RequestCycle requestCycle, final String userAgent) {
        this.properties = new ClientProperties();
        this.userAgent = userAgent;
        this.properties.setRemoteAddress(this.getRemoteAddr(requestCycle));
        this.init();
    }
    
    public final ClientProperties getProperties() {
        return this.properties;
    }
    
    public final String getUserAgent() {
        return this.userAgent;
    }
    
    private String getUserAgentStringLc() {
        return (this.getUserAgent() != null) ? this.getUserAgent().toLowerCase() : "";
    }
    
    protected String getRemoteAddr(final RequestCycle requestCycle) {
        final ServletWebRequest request = (ServletWebRequest)requestCycle.getRequest();
        final HttpServletRequest req = request.getContainerRequest();
        String remoteAddr = request.getHeader("X-Forwarded-For");
        if (remoteAddr != null) {
            if (remoteAddr.contains(",")) {
                remoteAddr = Strings.split(remoteAddr, ',')[0].trim();
            }
            try {
                InetAddress.getByName(remoteAddr);
            }
            catch (UnknownHostException e) {
                remoteAddr = req.getRemoteAddr();
            }
        }
        else {
            remoteAddr = req.getRemoteAddr();
        }
        return remoteAddr;
    }
    
    private void init() {
        this.setInternetExplorerProperties();
        this.setOperaProperties();
        this.setMozillaProperties();
        this.setKonquerorProperties();
        this.setChromeProperties();
        this.setSafariProperties();
        if (WebClientInfo.log.isDebugEnabled()) {
            WebClientInfo.log.debug("determined user agent: " + this.properties);
        }
    }
    
    private void setKonquerorProperties() {
        this.properties.setBrowserKonqueror(UserAgent.KONQUEROR.matches(this.getUserAgent()));
        if (this.properties.isBrowserKonqueror()) {
            this.setMajorMinorVersionByPattern("konqueror/(\\d+)\\.(\\d+)");
        }
    }
    
    private void setChromeProperties() {
        this.properties.setBrowserChrome(UserAgent.CHROME.matches(this.getUserAgent()));
        if (this.properties.isBrowserChrome()) {
            this.setMajorMinorVersionByPattern("chrome/(\\d+)\\.(\\d+)");
        }
    }
    
    private void setSafariProperties() {
        this.properties.setBrowserSafari(UserAgent.SAFARI.matches(this.getUserAgent()));
        if (this.properties.isBrowserSafari()) {
            final String userAgent = this.getUserAgentStringLc();
            if (userAgent.contains("version/")) {
                this.setMajorMinorVersionByPattern("version/(\\d+)\\.(\\d+)");
            }
        }
    }
    
    private void setMozillaProperties() {
        this.properties.setBrowserMozillaFirefox(UserAgent.FIREFOX.matches(this.getUserAgent()));
        this.properties.setBrowserMozilla(UserAgent.MOZILLA.matches(this.getUserAgent()));
        if (this.properties.isBrowserMozilla() && this.properties.isBrowserMozillaFirefox()) {
            this.setMajorMinorVersionByPattern("firefox/(\\d+)\\.(\\d+)");
        }
    }
    
    private void setOperaProperties() {
        this.properties.setBrowserOpera(UserAgent.OPERA.matches(this.getUserAgent()));
        if (this.properties.isBrowserOpera()) {
            final String userAgent = this.getUserAgentStringLc();
            if (userAgent.startsWith("opera/") && userAgent.contains("version/")) {
                this.setMajorMinorVersionByPattern("version/(\\d+)\\.(\\d+)");
            }
            else if (userAgent.startsWith("opera/") && !userAgent.contains("version/")) {
                this.setMajorMinorVersionByPattern("opera/(\\d+)\\.(\\d+)");
            }
            else {
                this.setMajorMinorVersionByPattern("opera (\\d+)\\.(\\d+)");
            }
        }
    }
    
    private void setInternetExplorerProperties() {
        this.properties.setBrowserInternetExplorer(UserAgent.INTERNET_EXPLORER.matches(this.getUserAgent()));
        if (this.properties.isBrowserInternetExplorer()) {
            if (this.getUserAgentStringLc().contains("like gecko")) {
                this.setMajorMinorVersionByPattern("rv:(\\d+)\\.(\\d+)");
            }
            else {
                this.setMajorMinorVersionByPattern("msie (\\d+)\\.(\\d+)");
            }
        }
    }
    
    private void setMajorMinorVersionByPattern(final String patternString) {
        final String userAgent = this.getUserAgentStringLc();
        final Matcher matcher = Pattern.compile(patternString).matcher(userAgent);
        if (matcher.find()) {
            this.properties.setBrowserVersionMajor(Integer.parseInt(matcher.group(1)));
            this.properties.setBrowserVersionMinor(Integer.parseInt(matcher.group(2)));
        }
    }
    
    static {
        log = LoggerFactory.getLogger((Class)WebClientInfo.class);
    }
}
