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
import java.util.Map;
import org.jasig.cas.client.util.CommonUtils;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

public abstract class AbstractUrlBasedTicketValidator implements TicketValidator
{
    protected final Log log;
    private final String casServerUrlPrefix;
    private boolean renew;
    
    protected AbstractUrlBasedTicketValidator(final String casServerUrlPrefix) {
        this.log = LogFactory.getLog((Class)this.getClass());
        CommonUtils.assertNotNull(this.casServerUrlPrefix = casServerUrlPrefix, "casServerUrlPrefix cannot be null.");
    }
    
    protected void populateUrlAttributeMap(final Map urlParameters) {
    }
    
    protected abstract String getUrlSuffix();
    
    protected final String constructValidationUrl(final String ticket, final String serviceUrl) {
        final Map urlParameters = new HashMap();
        urlParameters.put("ticket", ticket);
        urlParameters.put("service", this.encodeUrl(serviceUrl));
        if (this.renew) {
            urlParameters.put("renew", "true");
        }
        this.populateUrlAttributeMap(urlParameters);
        final String suffix = this.getUrlSuffix();
        final StringBuffer buffer = new StringBuffer(urlParameters.size() * 10 + this.casServerUrlPrefix.length() + suffix.length() + 1);
        int i = 0;
        synchronized (buffer) {
            buffer.append(this.casServerUrlPrefix);
            buffer.append("/");
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
        try {
            final String serverResponse = this.retrieveResponseFromServer(new URL(validationUrl), ticket);
            if (serverResponse == null) {
                throw new TicketValidationException("The CAS server returned no response.");
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
}
