// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.data.rest.webmvc.json.patch;

import java.util.List;
import org.springframework.data.mapping.PropertyPath;
import java.util.Collection;
import org.springframework.util.StringUtils;
import java.util.ArrayList;

class AddOperation extends PatchOperation
{
    public AddOperation(final String path, final Object value) {
        super("add", path, value);
    }
    
    @Override
     <T> void perform(final Object targetObject, final Class<T> type) {
        this.addValue(targetObject, this.evaluateValueFromTarget(targetObject, type));
    }
    
    @Override
    protected <T> Object evaluateValueFromTarget(final Object targetObject, final Class<T> entityType) {
        if (!this.path.endsWith("-")) {
            return super.evaluateValueFromTarget(targetObject, entityType);
        }
        final List<String> segments = new ArrayList<String>();
        for (final String segment : this.path.split("/")) {
            if (!segment.matches("\\d+") && !segment.equals("-") && !segment.isEmpty()) {
                segments.add(segment);
            }
        }
        final PropertyPath propertyPath = PropertyPath.from(StringUtils.collectionToDelimitedString((Collection)segments, "."), (Class)entityType);
        return (this.value instanceof LateObjectEvaluator) ? ((LateObjectEvaluator)this.value).evaluate((Class<Object>)propertyPath.getType()) : this.value;
    }
}
