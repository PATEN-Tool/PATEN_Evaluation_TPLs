// 
// Decompiled by Procyon v0.5.36
// 

package com.opensymphony.xwork2.validator.validators;

import org.apache.logging.log4j.LogManager;
import org.apache.commons.lang3.StringUtils;
import com.opensymphony.xwork2.validator.ValidationException;
import java.util.regex.Pattern;
import org.apache.logging.log4j.Logger;

public class URLValidator extends FieldValidatorSupport
{
    private static final Logger LOG;
    public static final String DEFAULT_URL_REGEX = "^(https?|ftp):\\/\\/(([a-z0-9$_\\.\\+!\\*\\'\\(\\),;\\?&=-]|%[0-9a-f]{2})+(:([a-z0-9$_\\.\\+!\\*\\'\\(\\),;\\?&=-]|%[0-9a-f]{2})+)?@)?(#?)((([a-z0-9]\\.|[a-z0-9][a-z0-9-]*[a-z0-9]\\.)*[a-z][a-z0-9-]*[a-z0-9]|((\\d|[1-9]\\d|1\\d{2}|2[0-4][0-9]|25[0-5])\\.){3}(\\d|[1-9]\\d|1\\d{2}|2[0-4][0-9]|25[0-5]))(:\\d+)?)(((\\/{0,1}([a-z0-9$_\\.\\+!\\*\\'\\(\\),;:@&=-]|%[0-9a-f]{2})*)*(\\?([a-z0-9$_\\.\\+!\\*\\'\\(\\),;:@&=-]|%[0-9a-f]{2})*)?)?)?(#([a-z0-9$_\\.\\+!\\*\\'\\(\\),;:@&=-]|%[0-9a-f]{2})*)?$";
    private String urlRegexExpression;
    private Pattern urlPattern;
    
    public URLValidator() {
        this.urlPattern = Pattern.compile("^(https?|ftp):\\/\\/(([a-z0-9$_\\.\\+!\\*\\'\\(\\),;\\?&=-]|%[0-9a-f]{2})+(:([a-z0-9$_\\.\\+!\\*\\'\\(\\),;\\?&=-]|%[0-9a-f]{2})+)?@)?(#?)((([a-z0-9]\\.|[a-z0-9][a-z0-9-]*[a-z0-9]\\.)*[a-z][a-z0-9-]*[a-z0-9]|((\\d|[1-9]\\d|1\\d{2}|2[0-4][0-9]|25[0-5])\\.){3}(\\d|[1-9]\\d|1\\d{2}|2[0-4][0-9]|25[0-5]))(:\\d+)?)(((\\/{0,1}([a-z0-9$_\\.\\+!\\*\\'\\(\\),;:@&=-]|%[0-9a-f]{2})*)*(\\?([a-z0-9$_\\.\\+!\\*\\'\\(\\),;:@&=-]|%[0-9a-f]{2})*)?)?)?(#([a-z0-9$_\\.\\+!\\*\\'\\(\\),;:@&=-]|%[0-9a-f]{2})*)?$", 2);
    }
    
    @Override
    public void validate(final Object object) throws ValidationException {
        final String fieldName = this.getFieldName();
        final Object value = this.getFieldValue(fieldName, object);
        if (value == null || value.toString().length() == 0) {
            return;
        }
        final String stringValue = String.valueOf(value).trim();
        if (!value.getClass().equals(String.class) || !this.getUrlPattern().matcher(stringValue).matches()) {
            this.addFieldError(fieldName, object);
        }
    }
    
    protected Pattern getUrlPattern() {
        if (StringUtils.isNotEmpty((CharSequence)this.urlRegexExpression)) {
            final String regex = (String)this.parse(this.urlRegexExpression, String.class);
            if (regex == null) {
                URLValidator.LOG.warn("Provided URL Regex expression [{}] was evaluated to null! Falling back to default!", (Object)this.urlRegexExpression);
                this.urlPattern = Pattern.compile("^(https?|ftp):\\/\\/(([a-z0-9$_\\.\\+!\\*\\'\\(\\),;\\?&=-]|%[0-9a-f]{2})+(:([a-z0-9$_\\.\\+!\\*\\'\\(\\),;\\?&=-]|%[0-9a-f]{2})+)?@)?(#?)((([a-z0-9]\\.|[a-z0-9][a-z0-9-]*[a-z0-9]\\.)*[a-z][a-z0-9-]*[a-z0-9]|((\\d|[1-9]\\d|1\\d{2}|2[0-4][0-9]|25[0-5])\\.){3}(\\d|[1-9]\\d|1\\d{2}|2[0-4][0-9]|25[0-5]))(:\\d+)?)(((\\/{0,1}([a-z0-9$_\\.\\+!\\*\\'\\(\\),;:@&=-]|%[0-9a-f]{2})*)*(\\?([a-z0-9$_\\.\\+!\\*\\'\\(\\),;:@&=-]|%[0-9a-f]{2})*)?)?)?(#([a-z0-9$_\\.\\+!\\*\\'\\(\\),;:@&=-]|%[0-9a-f]{2})*)?$", 2);
            }
            else {
                this.urlPattern = Pattern.compile(regex, 2);
            }
        }
        return this.urlPattern;
    }
    
    public String getUrlRegex() {
        return this.getUrlPattern().pattern();
    }
    
    public void setUrlRegex(final String urlRegex) {
        this.urlPattern = Pattern.compile(urlRegex, 2);
    }
    
    public void setUrlRegexExpression(final String urlRegexExpression) {
        this.urlRegexExpression = urlRegexExpression;
    }
    
    static {
        LOG = LogManager.getLogger((Class)URLValidator.class);
    }
}
