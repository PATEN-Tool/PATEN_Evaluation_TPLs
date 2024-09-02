// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.data.rest.webmvc.json.patch;

import org.springframework.data.mapping.PropertyPath;

class AddOperation extends PatchOperation
{
    public AddOperation(final String path, final Object value) {
        super("add", path, value);
    }
    
    @Override
     <T> void doPerform(final Object targetObject, final Class<T> type) {
        this.addValue(targetObject, this.evaluateValueFromTarget(targetObject, type));
    }
    
    @Override
    protected <T> Object evaluateValueFromTarget(final Object targetObject, final Class<T> entityType) {
        if (!this.path.endsWith("-")) {
            return super.evaluateValueFromTarget(targetObject, entityType);
        }
        final PropertyPath path = this.verifyPath(entityType);
        return this.evaluate((path == null) ? entityType : path.getType());
    }
}
