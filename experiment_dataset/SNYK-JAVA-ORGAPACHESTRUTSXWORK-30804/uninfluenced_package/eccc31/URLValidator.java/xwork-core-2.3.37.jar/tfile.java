// 
// Decompiled by Procyon v0.5.36
// 

package com.opensymphony.xwork2.validator.validators;

import org.apache.commons.lang3.StringUtils;
import com.opensymphony.xwork2.validator.ValidationException;
import com.opensymphony.xwork2.util.URLUtil;

public class URLValidator extends FieldValidatorSupport
{
    private String urlRegex;
    private String urlRegexExpression;
    
    @Override
    public void validate(final Object object) throws ValidationException {
        final String fieldName = this.getFieldName();
        final Object value = this.getFieldValue(fieldName, object);
        if (value == null || value.toString().length() == 0) {
            return;
        }
        if (!value.getClass().equals(String.class) || !URLUtil.verifyUrl((String)value)) {
            this.addFieldError(fieldName, object);
        }
    }
    
    public String getUrlRegex() {
        if (StringUtils.isNotEmpty((CharSequence)this.urlRegexExpression)) {
            return (String)this.parse(this.urlRegexExpression, String.class);
        }
        if (StringUtils.isNotEmpty((CharSequence)this.urlRegex)) {
            return this.urlRegex;
        }
        return "^(?:https?|ftp):\\/\\/(?:(?:[a-z0-9$_.+!*'(),;?&=\\-]|%[0-9a-f]{2})+(?::(?:[a-z0-9$_.+!*'(),;?&=\\-]|%[0-9a-f]{2})+)?@)?#?(?:(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)*[a-z][a-z0-9-]*[a-z0-9]|(?:(?:[1-9]?\\d|1\\d{2}|2[0-4]\\d|25[0-5])\\.){3}(?:[1-9]?\\d|1\\d{2}|2[0-4]\\d|25[0-5]))(?::\\d+)?)(?:(?:\\/(?:[a-z0-9$_.+!*'(),;:@&=\\-]|%[0-9a-f]{2})*)*(?:\\?(?:[a-z0-9$_.+!*'(),;:@&=\\-\\/:]|%[0-9a-f]{2})*)?)?(?:#(?:[a-z0-9$_.+!*'(),;:@&=\\-]|%[0-9a-f]{2})*)?$";
    }
    
    public void setUrlRegex(final String urlRegex) {
        this.urlRegex = urlRegex;
    }
    
    public void setUrlRegexExpression(final String urlRegexExpression) {
        this.urlRegexExpression = urlRegexExpression;
    }
}
