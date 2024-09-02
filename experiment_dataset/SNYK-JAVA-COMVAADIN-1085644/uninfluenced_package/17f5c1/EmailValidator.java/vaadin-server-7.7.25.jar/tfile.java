// 
// Decompiled by Procyon v0.5.36
// 

package com.vaadin.data.validator;

public class EmailValidator extends RegexpValidator
{
    private static final String PATTERN = "^([a-zA-Z0-9_\\.\\-+])+@[a-zA-Z0-9-.]+\\.[a-zA-Z0-9-]{2,}$";
    
    public EmailValidator(final String errorMessage) {
        super("^([a-zA-Z0-9_\\.\\-+])+@[a-zA-Z0-9-.]+\\.[a-zA-Z0-9-]{2,}$", true, errorMessage);
    }
}
