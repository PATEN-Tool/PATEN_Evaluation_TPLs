// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.data.mapping;

import org.springframework.util.StringUtils;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.Stack;
import java.util.ArrayList;
import java.util.Iterator;
import org.springframework.util.ObjectUtils;
import java.beans.Introspector;
import org.springframework.util.Assert;
import java.util.List;
import java.util.Collections;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import java.util.regex.Pattern;

public class PropertyPath implements Iterable<PropertyPath>
{
    private static final String PARSE_DEPTH_EXCEEDED = "Trying to parse a path with depth greater than 1000! This has been disabled for security reasons to prevent parsing overflows.";
    private static final String DELIMITERS = "_\\.";
    private static final String ALL_UPPERCASE = "[A-Z0-9._$]+";
    private static final Pattern SPLITTER;
    private final TypeInformation<?> owningType;
    private final String name;
    private final TypeInformation<?> type;
    private final boolean isCollection;
    private PropertyPath next;
    
    PropertyPath(final String name, final Class<?> owningType) {
        this(name, ClassTypeInformation.from(owningType), Collections.emptyList());
    }
    
    PropertyPath(final String name, final TypeInformation<?> owningType, final List<PropertyPath> base) {
        Assert.hasText(name, "Name must not be null or empty!");
        Assert.notNull((Object)owningType, "Owning type must not be null!");
        Assert.notNull((Object)base, "Perviously found properties must not be null!");
        final String propertyName = Introspector.decapitalize(name);
        final TypeInformation<?> propertyType = owningType.getProperty(propertyName);
        if (propertyType == null) {
            throw new PropertyReferenceException(propertyName, owningType, base);
        }
        this.owningType = owningType;
        this.isCollection = propertyType.isCollectionLike();
        this.type = propertyType.getActualType();
        this.name = propertyName;
    }
    
    public TypeInformation<?> getOwningType() {
        return this.owningType;
    }
    
    public String getSegment() {
        return this.name;
    }
    
    public PropertyPath getLeafProperty() {
        PropertyPath result;
        for (result = this; result.hasNext(); result = result.next()) {}
        return result;
    }
    
    public Class<?> getType() {
        return this.type.getType();
    }
    
    public PropertyPath next() {
        return this.next;
    }
    
    public boolean hasNext() {
        return this.next != null;
    }
    
    public String toDotPath() {
        if (this.hasNext()) {
            return this.getSegment() + "." + this.next().toDotPath();
        }
        return this.getSegment();
    }
    
    public boolean isCollection() {
        return this.isCollection;
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !this.getClass().equals(obj.getClass())) {
            return false;
        }
        final PropertyPath that = (PropertyPath)obj;
        return this.name.equals(that.name) && this.type.equals(that.type) && ObjectUtils.nullSafeEquals((Object)this.next, (Object)that.next);
    }
    
    @Override
    public int hashCode() {
        int result = 17;
        result += 31 * this.name.hashCode();
        result += 31 * this.type.hashCode();
        result += 31 * ((this.next == null) ? 0 : this.next.hashCode());
        return result;
    }
    
    @Override
    public Iterator<PropertyPath> iterator() {
        return new Iterator<PropertyPath>() {
            private PropertyPath current = PropertyPath.this;
            
            @Override
            public boolean hasNext() {
                return this.current != null;
            }
            
            @Override
            public PropertyPath next() {
                final PropertyPath result = this.current;
                this.current = this.current.next();
                return result;
            }
            
            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
    
    public static PropertyPath from(final String source, final Class<?> type) {
        return from(source, ClassTypeInformation.from(type));
    }
    
    public static PropertyPath from(final String source, final TypeInformation<?> type) {
        Assert.hasText(source, "Source must not be null or empty!");
        Assert.notNull((Object)type, "TypeInformation must not be null or empty!");
        final List<String> iteratorSource = new ArrayList<String>();
        final Matcher matcher = PropertyPath.SPLITTER.matcher("_" + source);
        while (matcher.find()) {
            iteratorSource.add(matcher.group(1));
        }
        final Iterator<String> parts = iteratorSource.iterator();
        PropertyPath result = null;
        final Stack<PropertyPath> current = new Stack<PropertyPath>();
        while (parts.hasNext()) {
            if (result == null) {
                result = create(parts.next(), type, current);
                current.push(result);
            }
            else {
                current.push(create(parts.next(), current));
            }
        }
        return result;
    }
    
    private static PropertyPath create(final String source, final Stack<PropertyPath> base) {
        final PropertyPath previous = base.peek();
        final PropertyPath propertyPath = create(source, previous.type, base);
        return previous.next = propertyPath;
    }
    
    private static PropertyPath create(final String source, final TypeInformation<?> type, final List<PropertyPath> base) {
        return create(source, type, "", base);
    }
    
    private static PropertyPath create(final String source, final TypeInformation<?> type, final String addTail, final List<PropertyPath> base) {
        if (base.size() > 1000) {
            throw new IllegalArgumentException("Trying to parse a path with depth greater than 1000! This has been disabled for security reasons to prevent parsing overflows.");
        }
        PropertyReferenceException exception = null;
        PropertyPath current = null;
        try {
            current = new PropertyPath(source, type, base);
            if (!base.isEmpty()) {
                base.get(base.size() - 1).next = current;
            }
            final List<PropertyPath> newBase = new ArrayList<PropertyPath>(base);
            newBase.add(current);
            if (StringUtils.hasText(addTail)) {
                current.next = create(addTail, current.type, newBase);
            }
            return current;
        }
        catch (PropertyReferenceException e) {
            if (current != null) {
                throw e;
            }
            exception = e;
            final Pattern pattern = Pattern.compile("\\p{Lu}\\p{Ll}*$");
            final Matcher matcher = pattern.matcher(source);
            if (matcher.find() && matcher.start() != 0) {
                final int position = matcher.start();
                final String head = source.substring(0, position);
                final String tail = source.substring(position);
                try {
                    return create(head, type, tail + addTail, base);
                }
                catch (PropertyReferenceException e2) {
                    throw e2.hasDeeperResolutionDepthThan(exception) ? e2 : exception;
                }
            }
            throw exception;
        }
    }
    
    @Override
    public String toString() {
        return String.format("%s.%s", this.owningType.getType().getSimpleName(), this.toDotPath());
    }
    
    static {
        SPLITTER = Pattern.compile("(?:[%s]?([%s]*?[^%s]+))".replaceAll("%s", "_\\."));
    }
}
