// 
// Decompiled by Procyon v0.5.36
// 

package com.opensymphony.xwork2.validator.validators;

import org.apache.logging.log4j.LogManager;
import org.apache.commons.lang3.StringUtils;
import com.opensymphony.xwork2.validator.ValidationException;
import java.util.Iterator;
import java.util.Collection;
import java.util.Objects;
import java.util.regex.Pattern;
import org.apache.logging.log4j.Logger;

public class URLValidator extends FieldValidatorSupport
{
    private static final Logger LOG;
    public static final String DEFAULT_URL_REGEX = "^(?:https?|ftp):\\/\\/(?:(?:[a-z0-9$_.+!*'(),;?&=\\-]|%[0-9a-f]{2})+(?::(?:[a-z0-9$_.+!*'(),;?&=\\-]|%[0-9a-f]{2})+)?@)?#?(?:(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)*[a-z][a-z0-9-]*[a-z0-9]|(?:(?:[1-9]?\\d|1\\d{2}|2[0-4]\\d|25[0-5])\\.){3}(?:[1-9]?\\d|1\\d{2}|2[0-4]\\d|25[0-5]))(?::\\d+)?)(?:(?:\\/(?:[a-z0-9$_.+!*'(),;:@&=\\-]|%[0-9a-f]{2})*)*(?:\\?(?:[a-z0-9$_.+!*'(),;:@&=\\-\\/:]|%[0-9a-f]{2})*)?)?(?:#(?:[a-z0-9$_.+!*'(),;:@&=\\-]|%[0-9a-f]{2})*)?$";
    private String urlRegexExpression;
    private Pattern urlPattern;
    
    public URLValidator() {
        this.urlPattern = Pattern.compile("^(?:https?|ftp):\\/\\/(?:(?:[a-z0-9$_.+!*'(),;?&=\\-]|%[0-9a-f]{2})+(?::(?:[a-z0-9$_.+!*'(),;?&=\\-]|%[0-9a-f]{2})+)?@)?#?(?:(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)*[a-z][a-z0-9-]*[a-z0-9]|(?:(?:[1-9]?\\d|1\\d{2}|2[0-4]\\d|25[0-5])\\.){3}(?:[1-9]?\\d|1\\d{2}|2[0-4]\\d|25[0-5]))(?::\\d+)?)(?:(?:\\/(?:[a-z0-9$_.+!*'(),;:@&=\\-]|%[0-9a-f]{2})*)*(?:\\?(?:[a-z0-9$_.+!*'(),;:@&=\\-\\/:]|%[0-9a-f]{2})*)?)?(?:#(?:[a-z0-9$_.+!*'(),;:@&=\\-]|%[0-9a-f]{2})*)?$", 2);
    }
    
    @Override
    public void validate(final Object object) throws ValidationException {
        final Object value = this.getFieldValue(this.fieldName, object);
        final String stringValue = Objects.toString(value, "").trim();
        if (stringValue.length() == 0) {
            URLValidator.LOG.debug("Value for field {} is empty, won't ba validated, please use a required validator", (Object)this.fieldName);
            return;
        }
        if (value.getClass().isArray()) {
            final Object[] arr$;
            final Object[] values = arr$ = (Object[])value;
            for (final Object objValue : arr$) {
                URLValidator.LOG.debug("Validating element of array: {}", objValue);
                this.validateValue(object, objValue);
            }
        }
        else if (Collection.class.isAssignableFrom(value.getClass())) {
            final Collection values2 = (Collection)value;
            for (final Object objValue2 : values2) {
                URLValidator.LOG.debug("Validating element of collection: {}", objValue2);
                this.validateValue(object, objValue2);
            }
        }
        else {
            URLValidator.LOG.debug("Validating field: {}", value);
            this.validateValue(object, value);
        }
    }
    
    protected void validateValue(final Object object, final Object value) {
        final String stringValue = Objects.toString(value, "").trim();
        if (stringValue.length() == 0) {
            URLValidator.LOG.debug("Value for field {} is empty, won't ba validated, please use a required validator", (Object)this.fieldName);
            return;
        }
        try {
            this.setCurrentValue(value);
            if (!value.getClass().equals(String.class) || !this.getUrlPattern().matcher(stringValue).matches()) {
                this.addFieldError(this.fieldName, object);
            }
        }
        finally {
            this.setCurrentValue(null);
        }
    }
    
    protected Pattern getUrlPattern() {
        if (StringUtils.isNotEmpty((CharSequence)this.urlRegexExpression)) {
            final String regex = (String)this.parse(this.urlRegexExpression, String.class);
            if (regex == null) {
                URLValidator.LOG.warn("Provided URL Regex expression [{}] was evaluated to null! Falling back to default!", (Object)this.urlRegexExpression);
                this.urlPattern = Pattern.compile("^(?:https?|ftp):\\/\\/(?:(?:[a-z0-9$_.+!*'(),;?&=\\-]|%[0-9a-f]{2})+(?::(?:[a-z0-9$_.+!*'(),;?&=\\-]|%[0-9a-f]{2})+)?@)?#?(?:(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)*[a-z][a-z0-9-]*[a-z0-9]|(?:(?:[1-9]?\\d|1\\d{2}|2[0-4]\\d|25[0-5])\\.){3}(?:[1-9]?\\d|1\\d{2}|2[0-4]\\d|25[0-5]))(?::\\d+)?)(?:(?:\\/(?:[a-z0-9$_.+!*'(),;:@&=\\-]|%[0-9a-f]{2})*)*(?:\\?(?:[a-z0-9$_.+!*'(),;:@&=\\-\\/:]|%[0-9a-f]{2})*)?)?(?:#(?:[a-z0-9$_.+!*'(),;:@&=\\-]|%[0-9a-f]{2})*)?$", 2);
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
