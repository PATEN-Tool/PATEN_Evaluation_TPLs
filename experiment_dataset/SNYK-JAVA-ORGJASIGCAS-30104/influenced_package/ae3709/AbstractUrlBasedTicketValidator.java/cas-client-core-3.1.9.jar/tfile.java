// 
// Decompiled by Procyon v0.5.36
// 

package org.jasig.cas.client.validation;

import java.net.MalformedURLException;
import java.net.URL;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.HashMap;
import org.jasig.cas.client.util.CommonUtils;
import org.apache.commons.logging.LogFactory;
import java.util.Map;
import org.apache.commons.logging.Log;

public abstract class AbstractUrlBasedTicketValidator implements TicketValidator
{
    protected final Log log;
    private final String casServerUrlPrefix;
    private boolean renew;
    private Map customParameters;
    
    protected AbstractUrlBasedTicketValidator(final String casServerUrlPrefix) {
        this.log = LogFactory.getLog((Class)this.getClass());
        CommonUtils.assertNotNull(this.casServerUrlPrefix = casServerUrlPrefix, "casServerUrlPrefix cannot be null.");
    }
    
    protected void populateUrlAttributeMap(final Map urlParameters) {
    }
    
    protected abstract String getUrlSuffix();
    
    protected final String constructValidationUrl(final String ticket, final String serviceUrl) {
        final Map urlParameters = new HashMap();
        this.log.debug((Object)"Placing URL parameters in map.");
        urlParameters.put("ticket", ticket);
        urlParameters.put("service", this.encodeUrl(serviceUrl));
        if (this.renew) {
            urlParameters.put("renew", "true");
        }
        this.log.debug((Object)"Calling template URL attribute map.");
        this.populateUrlAttributeMap(urlParameters);
        this.log.debug((Object)"Loading custom parameters from configuration.");
        if (this.customParameters != null) {
            urlParameters.putAll(this.customParameters);
        }
        final String suffix = this.getUrlSuffix();
        final StringBuffer buffer = new StringBuffer(urlParameters.size() * 10 + this.casServerUrlPrefix.length() + suffix.length() + 1);
        int i = 0;
        synchronized (buffer) {
            buffer.append(this.casServerUrlPrefix);
            if (!this.casServerUrlPrefix.endsWith("/")) {
                buffer.append("/");
            }
            buffer.append(suffix);
            final Iterator iter = urlParameters.entrySet().iterator();
            while (iter.hasNext()) {
                buffer.append((i++ == 0) ? "?" : "&");
                final Map.Entry entry = iter.next();
                final String key = entry.getKey();
                final String value = entry.getValue();
                if (value != null) {
                    buffer.append(key);
                    buffer.append("=");
                    buffer.append(value);
                }
            }
            return buffer.toString();
        }
    }
    
    protected final String encodeUrl(final String url) {
        if (url == null) {
            return null;
        }
        try {
            return URLEncoder.encode(url, "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            return url;
        }
    }
    
    protected abstract Assertion parseResponseFromServer(final String p0) throws TicketValidationException;
    
    protected abstract String retrieveResponseFromServer(final URL p0, final String p1);
    
    public Assertion validate(final String ticket, final String service) throws TicketValidationException {
        final String validationUrl = this.constructValidationUrl(ticket, service);
        if (this.log.isDebugEnabled()) {
            this.log.debug((Object)("Constructing validation url: " + validationUrl));
        }
        try {
            this.log.debug((Object)"Retrieving response from server.");
            final String serverResponse = this.retrieveResponseFromServer(new URL(validationUrl), ticket);
            if (serverResponse == null) {
                throw new TicketValidationException("The CAS server returned no response.");
            }
            if (this.log.isDebugEnabled()) {
                this.log.debug((Object)("Server response: " + serverResponse));
            }
            return this.parseResponseFromServer(serverResponse);
        }
        catch (MalformedURLException e) {
            throw new TicketValidationException(e);
        }
    }
    
    public void setRenew(final boolean renew) {
        this.renew = renew;
    }
    
    public void setCustomParameters(final Map customParameters) {
        this.customParameters = customParameters;
    }
}
