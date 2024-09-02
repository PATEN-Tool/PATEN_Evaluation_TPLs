// 
// Decompiled by Procyon v0.5.36
// 

package com.sap.scimono.entity.schema.validation;

import javax.validation.ConstraintValidatorContext;

class ValidationUtil
{
    public static void interpolateErrorMessage(final ConstraintValidatorContext context, final String errorMessage) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();
    }
}
