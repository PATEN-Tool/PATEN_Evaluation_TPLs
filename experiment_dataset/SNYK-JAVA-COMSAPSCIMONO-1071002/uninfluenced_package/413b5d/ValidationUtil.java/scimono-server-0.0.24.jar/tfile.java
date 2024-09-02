// 
// Decompiled by Procyon v0.5.36
// 

package com.sap.scimono.entity.schema.validation;

import javax.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

class ValidationUtil
{
    private static final Pattern EXPRESSION_LANGUAGE_CHARACTERS;
    
    public static void interpolateErrorMessage(final ConstraintValidatorContext context, final String errorMessage) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(escapeExpressionLanguage(errorMessage)).addConstraintViolation();
    }
    
    private static String escapeExpressionLanguage(final String text) {
        return ValidationUtil.EXPRESSION_LANGUAGE_CHARACTERS.matcher(text).replaceAll("\\\\$1");
    }
    
    static {
        EXPRESSION_LANGUAGE_CHARACTERS = Pattern.compile("([${}])");
    }
}
