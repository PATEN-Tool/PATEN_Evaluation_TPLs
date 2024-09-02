// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.data.rest.webmvc.json.patch;

import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.util.StringUtils;
import java.util.ArrayList;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.expression.ExpressionException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.CollectionFactory;
import java.util.Collection;
import java.util.List;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.Expression;

public abstract class PatchOperation
{
    private static final String INVALID_PATH_REFERENCE = "Invalid path reference %s on type %s (from source %s)!";
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
            catch (NullPointerException o_O) {
                throw new PatchException("Path '" + removePath + "' is not nullable.", o_O);
            }
            catch (SpelEvaluationException o_O2) {
                throw new PatchException("Path '" + removePath + "' is not nullable.", (Exception)o_O2);
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
            final TypeDescriptor descriptor = parentExpression.getValueTypeDescriptor(target);
            if (descriptor.isCollection() && !Collection.class.isInstance(value)) {
                final Collection<Object> collection = (Collection<Object>)CollectionFactory.createCollection(descriptor.getType(), 1);
                collection.add(value);
                parentExpression.setValue(target, (Object)collection);
            }
            else {
                this.spelExpression.setValue(target, value);
            }
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
        this.verifyPath(entityType);
        return this.evaluate((Class<Object>)this.spelExpression.getValueType(targetObject));
    }
    
    protected final <T> Object evaluate(final Class<T> targetType) {
        return (this.value instanceof LateObjectEvaluator) ? ((LateObjectEvaluator)this.value).evaluate(targetType) : this.value;
    }
    
    final <T> void perform(final Object target, final Class<T> type) {
        this.verifyPath(type);
        this.doPerform(target, (Class<Object>)type);
    }
    
    abstract <T> void doPerform(final Object p0, final Class<T> p1);
    
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
    
    protected PropertyPath verifyPath(final Class<?> type) {
        final List<String> segments = new ArrayList<String>();
        for (final String segment : this.path.split("/")) {
            if (!segment.matches("\\d+") && !segment.equals("-") && !segment.equals("~") && !segment.isEmpty()) {
                segments.add(segment);
            }
        }
        if (segments.isEmpty()) {
            return null;
        }
        final String pathSource = StringUtils.collectionToDelimitedString((Collection)segments, ".");
        try {
            return PropertyPath.from(pathSource, (Class)type);
        }
        catch (PropertyReferenceException o_O) {
            throw new PatchException(String.format("Invalid path reference %s on type %s (from source %s)!", pathSource, type, this.path), (Exception)o_O);
        }
    }
}
