// 
// Decompiled by Procyon v0.5.36
// 

package org.keycloak.userprofile.validation;

import org.keycloak.userprofile.UserProfileContext;
import java.util.function.BiFunction;
import java.util.ArrayList;
import java.util.List;

public class AttributeValidatorBuilder
{
    ValidationChainBuilder validationChainBuilder;
    String attributeKey;
    List<Validator> validations;
    
    public AttributeValidatorBuilder(final ValidationChainBuilder validationChainBuilder) {
        this.validations = new ArrayList<Validator>();
        this.validationChainBuilder = validationChainBuilder;
    }
    
    public AttributeValidatorBuilder addSingleAttributeValueValidationFunction(final String messageKey, final BiFunction<String, UserProfileContext, Boolean> validationFunction) {
        final String singleValue;
        final BiFunction<List<String>, UserProfileContext, Boolean> wrappedValidationFunction = (BiFunction<List<String>, UserProfileContext, Boolean>)((attrValues, context) -> {
            singleValue = ((attrValues == null) ? null : attrValues.get(0));
            return Boolean.valueOf(validationFunction.apply(singleValue, context));
        });
        this.validations.add(new Validator(messageKey, wrappedValidationFunction));
        return this;
    }
    
    public AttributeValidatorBuilder addValidationFunction(final String messageKey, final BiFunction<List<String>, UserProfileContext, Boolean> validationFunction) {
        this.validations.add(new Validator(messageKey, validationFunction));
        return this;
    }
    
    public AttributeValidatorBuilder forAttribute(final String attributeKey) {
        this.attributeKey = attributeKey;
        return this;
    }
    
    public ValidationChainBuilder build() {
        this.validationChainBuilder.addValidatorConfig(new AttributeValidator(this.attributeKey, this.validations));
        return this.validationChainBuilder;
    }
}
