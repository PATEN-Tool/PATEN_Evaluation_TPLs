// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.data.mapping;

import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.StringUtils;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.Stack;
import java.util.ArrayList;
import org.springframework.util.ObjectUtils;
import java.util.Iterator;
import java.beans.Introspector;
import org.springframework.util.Assert;
import java.util.List;
import java.util.Collections;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.data.util.TypeInformation;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.data.util.Streamable;

public class PropertyPath implements Streamable<PropertyPath>
{
    private static final String PARSE_DEPTH_EXCEEDED = "Trying to parse a path with depth greater than 1000! This has been disabled for security reasons to prevent parsing overflows.";
    private static final String DELIMITERS = "_\\.";
    private static final String ALL_UPPERCASE = "[A-Z0-9._$]+";
    private static final Pattern SPLITTER;
    private static final Pattern SPLITTER_FOR_QUOTED;
    private static final Map<Key, PropertyPath> cache;
    private final TypeInformation<?> owningType;
    private final String name;
    private final TypeInformation<?> typeInformation;
    private final TypeInformation<?> actualTypeInformation;
    private final boolean isCollection;
    @Nullable
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
        this.typeInformation = propertyType;
        this.isCollection = propertyType.isCollectionLike();
        this.name = propertyName;
        this.actualTypeInformation = ((propertyType.getActualType() == null) ? propertyType : propertyType.getRequiredActualType());
    }
    
    public TypeInformation<?> getOwningType() {
        return this.owningType;
    }
    
    public String getSegment() {
        return this.name;
    }
    
    public PropertyPath getLeafProperty() {
        PropertyPath result;
        for (result = this; result.hasNext(); result = result.requiredNext()) {}
        return result;
    }
    
    public Class<?> getLeafType() {
        return this.getLeafProperty().getType();
    }
    
    public Class<?> getType() {
        return this.actualTypeInformation.getType();
    }
    
    public TypeInformation<?> getTypeInformation() {
        return this.typeInformation;
    }
    
    @Nullable
    public PropertyPath next() {
        return this.next;
    }
    
    public boolean hasNext() {
        return this.next != null;
    }
    
    public String toDotPath() {
        if (this.hasNext()) {
            return this.getSegment() + "." + this.requiredNext().toDotPath();
        }
        return this.getSegment();
    }
    
    public boolean isCollection() {
        return this.isCollection;
    }
    
    public PropertyPath nested(final String path) {
        Assert.hasText(path, "Path must not be null or empty!");
        final String lookup = this.toDotPath().concat(".").concat(path);
        return from(lookup, this.owningType);
    }
    
    @Override
    public Iterator<PropertyPath> iterator() {
        return new Iterator<PropertyPath>() {
            @Nullable
            private PropertyPath current = PropertyPath.this;
            
            @Override
            public boolean hasNext() {
                return this.current != null;
            }
            
            @Nullable
            @Override
            public PropertyPath next() {
                final PropertyPath result = this.current;
                if (result == null) {
                    return null;
                }
                this.current = result.next();
                return result;
            }
            
            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
    
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PropertyPath)) {
            return false;
        }
        final PropertyPath that = (PropertyPath)o;
        return this.isCollection == that.isCollection && ObjectUtils.nullSafeEquals((Object)this.owningType, (Object)that.owningType) && ObjectUtils.nullSafeEquals((Object)this.name, (Object)that.name) && ObjectUtils.nullSafeEquals((Object)this.typeInformation, (Object)that.typeInformation) && ObjectUtils.nullSafeEquals((Object)this.actualTypeInformation, (Object)that.actualTypeInformation) && ObjectUtils.nullSafeEquals((Object)this.next, (Object)that.next);
    }
    
    @Override
    public int hashCode() {
        int result = ObjectUtils.nullSafeHashCode((Object)this.owningType);
        result = 31 * result + ObjectUtils.nullSafeHashCode((Object)this.name);
        result = 31 * result + ObjectUtils.nullSafeHashCode((Object)this.typeInformation);
        result = 31 * result + ObjectUtils.nullSafeHashCode((Object)this.actualTypeInformation);
        result = 31 * result + (this.isCollection ? 1 : 0);
        result = 31 * result + ObjectUtils.nullSafeHashCode((Object)this.next);
        return result;
    }
    
    private PropertyPath requiredNext() {
        final PropertyPath result = this.next;
        if (result == null) {
            throw new IllegalStateException("No next path available! Clients should call hasNext() before invoking this method!");
        }
        return result;
    }
    
    public static PropertyPath from(final String source, final Class<?> type) {
        return from(source, ClassTypeInformation.from(type));
    }
    
    public static PropertyPath from(final String source, final TypeInformation<?> type) {
        Assert.hasText(source, "Source must not be null or empty!");
        Assert.notNull((Object)type, "TypeInformation must not be null or empty!");
        final List<String> iteratorSource;
        Matcher matcher2;
        final Matcher matcher;
        final Iterator<String> parts;
        PropertyPath result;
        final Stack<PropertyPath> current;
        final IllegalStateException ex;
        return PropertyPath.cache.computeIfAbsent(Key.of(type, source), it -> {
            iteratorSource = new ArrayList<String>();
            if (isQuoted(it.path)) {
                matcher2 = PropertyPath.SPLITTER_FOR_QUOTED.matcher(it.path.replace("\\Q", "").replace("\\E", ""));
            }
            else {
                matcher2 = PropertyPath.SPLITTER.matcher("_" + it.path);
            }
            matcher = matcher2;
            while (matcher.find()) {
                iteratorSource.add(matcher.group(1));
            }
            parts = iteratorSource.iterator();
            result = null;
            current = new Stack<PropertyPath>();
            while (parts.hasNext()) {
                if (result == null) {
                    result = create(parts.next(), it.type, current);
                    current.push(result);
                }
                else {
                    current.push(create(parts.next(), current));
                }
            }
            if (result == null) {
                new IllegalStateException(String.format("Expected parsing to yield a PropertyPath from %s but got null!", source));
                throw ex;
            }
            else {
                return result;
            }
        });
    }
    
    private static boolean isQuoted(final String source) {
        return source.matches("^\\\\Q.*\\\\E$");
    }
    
    private static PropertyPath create(final String source, final Stack<PropertyPath> base) {
        final PropertyPath previous = base.peek();
        final PropertyPath propertyPath = create(source, previous.typeInformation.getRequiredActualType(), base);
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
                current.next = create(addTail, current.actualTypeInformation, newBase);
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
        SPLITTER_FOR_QUOTED = Pattern.compile("(?:[%s]?([%s]*?[^%s]+))".replaceAll("%s", "\\."));
        cache = (Map)new ConcurrentReferenceHashMap();
    }
    
    private static final class Key
    {
        private final TypeInformation<?> type;
        private final String path;
        
        private Key(final TypeInformation<?> type, final String path) {
            this.type = type;
            this.path = path;
        }
        
        public static Key of(final TypeInformation<?> type, final String path) {
            return new Key(type, path);
        }
        
        public TypeInformation<?> getType() {
            return this.type;
        }
        
        public String getPath() {
            return this.path;
        }
        
        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Key)) {
                return false;
            }
            final Key key = (Key)o;
            return ObjectUtils.nullSafeEquals((Object)this.type, (Object)key.type) && ObjectUtils.nullSafeEquals((Object)this.path, (Object)key.path);
        }
        
        @Override
        public int hashCode() {
            int result = ObjectUtils.nullSafeHashCode((Object)this.type);
            result = 31 * result + ObjectUtils.nullSafeHashCode((Object)this.path);
            return result;
        }
        
        @Override
        public String toString() {
            return "PropertyPath.Key(type=" + this.getType() + ", path=" + this.getPath() + ")";
        }
    }
}
