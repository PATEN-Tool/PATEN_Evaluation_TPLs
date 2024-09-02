// 
// Decompiled by Procyon v0.5.36
// 

package com.vaadin.data.validator;

public class EmailValidator extends RegexpValidator
{
    public EmailValidator(final String errorMessage) {
        super("^([a-zA-Z0-9_\\.\\-+])+@(([a-zA-Z0-9-])+\\.)+([a-zA-Z0-9]{2,4})+$", true, errorMessage);
    }
}
