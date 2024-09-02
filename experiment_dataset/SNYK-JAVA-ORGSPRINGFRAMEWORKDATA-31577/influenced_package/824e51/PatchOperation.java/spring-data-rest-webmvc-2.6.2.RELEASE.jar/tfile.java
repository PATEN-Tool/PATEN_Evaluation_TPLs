// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.data.rest.webmvc.json.patch;

import org.springframework.expression.ExpressionException;
import java.util.List;
import org.springframework.expression.Expression;

public abstract class PatchOperation
{
    protected final String op;
    protected final String path;
    protected final Object value;
    protected final Expression spelExpression;
    
    public PatchOperation(final String op, final String path) {
        this(op, path, null);
    }
    
    public PatchOperation(final String op, final String path, final Object value) {
        this.op = op;
        this.path = path;
        this.value = value;
        this.spelExpression = PathToSpEL.pathToExpression(path);
    }
    
    public String getOp() {
        return this.op;
    }
    
    public String getPath() {
        return this.path;
    }
    
    public Object getValue() {
        return this.value;
    }
    
    protected Object popValueAtPath(final Object target, final String removePath) {
        final Integer listIndex = this.targetListIndex(removePath);
        final Expression expression = PathToSpEL.pathToExpression(removePath);
        final Object value = expression.getValue(target);
        if (listIndex == null) {
            try {
                expression.setValue(target, (Object)null);
                return value;
            }
            catch (NullPointerException e) {
                throw new PatchException("Path '" + removePath + "' is not nullable.");
            }
        }
        final Expression parentExpression = PathToSpEL.pathToParentExpression(removePath);
        final List<?> list = (List<?>)parentExpression.getValue(target);
        list.remove((listIndex >= 0) ? ((int)listIndex) : (list.size() - 1));
        return value;
    }
    
    protected void addValue(final Object target, final Object value) {
        final Expression parentExpression = PathToSpEL.pathToParentExpression(this.path);
        final Object parent = (parentExpression != null) ? parentExpression.getValue(target) : null;
        final Integer listIndex = this.targetListIndex(this.path);
        if (parent == null || !(parent instanceof List) || listIndex == null) {
            this.spelExpression.setValue(target, value);
        }
        else {
            final List<Object> list = (List<Object>)parentExpression.getValue(target);
            list.add((listIndex >= 0) ? ((int)listIndex) : list.size(), value);
        }
    }
    
    protected void setValueOnTarget(final Object target, final Object value) {
        this.spelExpression.setValue(target, value);
    }
    
    protected Object getValueFromTarget(final Object target) {
        try {
            return this.spelExpression.getValue(target);
        }
        catch (ExpressionException e) {
            throw new PatchException("Unable to get value from target", (Exception)e);
        }
    }
    
    protected <T> Object evaluateValueFromTarget(final Object targetObject, final Class<T> entityType) {
        return (this.value instanceof LateObjectEvaluator) ? ((LateObjectEvaluator)this.value).evaluate((Class<Object>)this.spelExpression.getValueType(targetObject)) : this.value;
    }
    
    abstract <T> void perform(final Object p0, final Class<T> p1);
    
    private Integer targetListIndex(final String path) {
        final String[] pathNodes = path.split("\\/");
        final String lastNode = pathNodes[pathNodes.length - 1];
        if (PathToSpEL.APPEND_CHARACTERS.contains(lastNode)) {
            return -1;
        }
        try {
            return Integer.parseInt(lastNode);
        }
        catch (NumberFormatException e) {
            return null;
        }
    }
}
